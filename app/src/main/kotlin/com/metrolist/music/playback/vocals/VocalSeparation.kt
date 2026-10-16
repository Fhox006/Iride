package com.metrolist.music.playback.vocals

import android.content.Context
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import java.io.File
import java.io.IOException
import java.security.MessageDigest
import java.util.concurrent.TimeUnit
import java.util.concurrent.Executors
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.OkHttpClient
import okhttp3.Request
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream

enum class VocalPreparation { Idle, DownloadingRuntime, DownloadingModel, Separating, Ready, Error }

internal class VocalPreparationException(message: String) : IOException(message)

data class VocalSeparationState(
    val mediaId: String? = null,
    val phase: VocalPreparation = VocalPreparation.Idle,
    val progress: Float = 0f,
    val voiceLevel: Float = 1f,
    val error: String? = null,
    val downloadRequired: Boolean = false,
    val audioReady: Boolean = false,
) {
    val downloading: Boolean get() = phase == VocalPreparation.DownloadingRuntime || phase == VocalPreparation.DownloadingModel
    val starting: Boolean get() = voiceLevel < 1f &&
        (phase == VocalPreparation.Separating || (phase == VocalPreparation.Ready && !audioReady))
    val active: Boolean get() = phase == VocalPreparation.Ready && voiceLevel < 1f && audioReady
}

/** Optional engine installation is persistent; inference and decoded windows stay bounded. */
@UnstableApi
internal class VocalSeparation(private val context: Context) {
    private val models = File(context.filesDir, "vocal-models/spleeter-fp16-v1")
    private val mutex = Mutex()
    private val client = OkHttpClient.Builder().connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS).build()

    private data class PendingWindow(
        val startFrame: Long,
        val revision: Long,
        val audio: Deferred<Result<Array<FloatArray>>>,
    )

    fun componentsInstalled(): Boolean = File(models, "vocals.fp16.onnx").length() > 0 &&
        File(models, "accompaniment.fp16.onnx").length() > 0 && VocalNativeRuntime(context).isInstalled()

    suspend fun prepare(
        timeline: VocalTimeline,
        durationMs: Long,
        positionMs: () -> Long,
        seekRevision: () -> Long,
        enabled: () -> Boolean,
        foreground: StateFlow<Boolean>,
        openSource: suspend () -> Pair<DataSource, DataSpec>,
        update: (VocalPreparation, Float) -> Unit,
    ): Unit = mutex.withLock {
        Executors.newSingleThreadExecutor { task ->
            Thread({
                android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND)
                task.run()
            }, "Iride-karaoke")
        }.asCoroutineDispatcher().use { worker ->
            withContext(worker) {
                val workerThread = android.os.Process.myTid()
                val priorityUpdates = launch(Dispatchers.Default) {
                    foreground.collect { active ->
                        android.os.Process.setThreadPriority(workerThread, if (active)
                            android.os.Process.THREAD_PRIORITY_DEFAULT else android.os.Process.THREAD_PRIORITY_BACKGROUND)
                    }
                }
                try {
                    val runtime = VocalNativeRuntime(context).ensureInstalled(client, foreground) { update(VocalPreparation.DownloadingRuntime, it) }
                    ensureModels(foreground) { update(VocalPreparation.DownloadingModel, it) }
                    currentCoroutineContext().ensureActive()
                    update(VocalPreparation.Separating, 0f)
                    val (source, spec) = openSource()
                    val mediaSource = VocalMediaDataSource(source, spec, checkNotNull(currentCoroutineContext()[Job]))
                    VocalStreamDecoder(mediaSource, durationMs).use { decoder ->
                        coroutineScope {
                            // Prepare the first usable windows at normal speed; idle only once audio is buffered.
                            SpleeterNative(models, runtime).use { engine ->
                                val rate = VocalTimeline.SAMPLE_RATE
                                val step = VocalWindowScheduler.STEP_FRAMES
                                val overlap = VocalWindowScheduler.OVERLAP_FRAMES
                                val scheduler = VocalWindowScheduler(decoder.durationFrames,
                                    positionMs().coerceAtLeast(0) * rate / 1000, seekRevision())
                                var previous: VocalTimeline.Chunk? = null
                                var previousEnd = -1L
                                var inferenceMs = 0L
                                var ready = false
                                var pending: PendingWindow? = null
                                try {
                                    while (true) {
                                        currentCoroutineContext().ensureActive()
                                        val position = positionMs().coerceAtLeast(0) * rate / 1000
                                        if (!enabled()) {
                                            VocalProcessingBudget.awaitTurn(0, foreground)
                                            continue
                                        }
                                        android.os.Process.setThreadPriority(if (foreground.value)
                                            android.os.Process.THREAD_PRIORITY_DEFAULT else android.os.Process.THREAD_PRIORITY_BACKGROUND)
                                        val next = scheduler.select(position, seekRevision(), inferenceMs)
                                        if (next == null) { delay(250); continue }
                                        if (previousEnd != next) previous = null
                                        val requestRevision = seekRevision()
                                        val started = System.nanoTime()
                                        val prefetched = pending
                                        pending = null
                                        val original = if (prefetched?.startFrame == next && prefetched.revision == requestRevision) {
                                            prefetched.audio.await().getOrThrow()
                                        } else {
                                            prefetched?.audio?.cancelAndJoin()
                                            decoder.decode(next, step + overlap)
                                        }
                                        currentCoroutineContext().ensureActive()
                                        if (requestRevision != seekRevision()) continue
                                        val following = next + step
                                        if (foreground.value && original[0].size == step + overlap && following < decoder.durationFrames &&
                                            following <= position + rate * 30) {
                                            // One decoded window ahead overlaps codec I/O with native inference.
                                            pending = PendingWindow(following, requestRevision, async(Dispatchers.IO) {
                                                try { Result.success(decoder.decode(following, step + overlap)) }
                                                catch (cancelled: CancellationException) { throw cancelled }
                                                catch (error: Exception) { Result.failure(error) }
                                            })
                                        }
                                        val base = engine.accompaniment(original[0], original[1])
                                        currentCoroutineContext().ensureActive()
                                        inferenceMs = (System.nanoTime() - started) / 1_000_000
                                        if (requestRevision != seekRevision()) continue
                                        require(base.size == 2 && base.all { it.size == original[0].size })
                                        check(base.all { channel -> channel.all { it.isFinite() } })
                                        val prior = previous?.takeIf { previousEnd == next }
                                        if (prior != null) repeat(minOf(prior.channels[0].size, base[0].size)) { frame ->
                                            val weight = frame.toFloat() / prior.channels[0].size
                                            repeat(2) { channel ->
                                                base[channel][frame] = prior.channels[channel][frame] * (1f - weight) + base[channel][frame] * weight
                                                original[channel][frame] = prior.original[channel][frame] * (1f - weight) + original[channel][frame] * weight
                                            }
                                        }
                                        val tail = original[0].size < step + overlap || next + original[0].size >= decoder.durationFrames
                                        val emitted = if (tail) original[0].size else minOf(step, original[0].size)
                                        timeline.publish(VocalTimeline.Chunk(next,
                                            Array(2) { base[it].copyOf(emitted) }, Array(2) { original[it].copyOf(emitted) }),
                                            positionMs().coerceAtLeast(0) * rate / 1000)
                                        previous = if (tail || emitted == base[0].size) null else VocalTimeline.Chunk(
                                            next + emitted,
                                            Array(2) { base[it].copyOfRange(emitted, base[it].size) },
                                            Array(2) { original[it].copyOfRange(emitted, original[it].size) },
                                        )
                                        scheduler.advance(next, emitted, tail)
                                        previousEnd = next + emitted
                                        if (!ready) { ready = true; update(VocalPreparation.Ready, 1f) }
                                        VocalProcessingBudget.awaitTurn(inferenceMs, foreground)
                                    }
                                } finally { pending?.audio?.cancel() }
                            }
                        }
                    }
                } finally { priorityUpdates.cancel() }
            }
        }
    }

    private suspend fun ensureModels(foreground: StateFlow<Boolean>, progress: (Float) -> Unit) {
        val expected = mapOf(
            "vocals.fp16.onnx" to "24cef84aedcd1fe87c0b743ef3370ad34dc1fabf6c9014d6128a75a538c7b668",
            "accompaniment.fp16.onnx" to "d14cea55793cc531a5875f5f4da08207d1c5ab9292e8e0099a104eecb014fcc0",
        )
        if (expected.all { (name, hash) -> File(models, name).let { it.isFile && digest(it) == hash } }) return
        progress(0f)
        models.mkdirs()
        val archive = File.createTempFile("spleeter-", ".tar.bz2", context.cacheDir)
        try {
            val request = Request.Builder().url(MODEL_URL).build()
            client.newCall(request).execute().use { response ->
                check(response.isSuccessful) { "Model download: HTTP ${response.code}" }
                val body = checkNotNull(response.body)
                body.byteStream().use { input ->
                    archive.outputStream().buffered().use { output ->
                        val buffer = ByteArray(65536)
                        var total = 0L
                        while (true) {
                            currentCoroutineContext().ensureActive()
                            val read = input.read(buffer)
                            if (read < 0) break
                            total += read
                            check(total < 64L * 1024 * 1024)
                            output.write(buffer, 0, read)
                            VocalProcessingBudget.awaitDownloadTurn(read, foreground)
                            progress(if (body.contentLength() > 0) (total.toFloat() / body.contentLength()).coerceIn(0f, 1f) else 0f)
                        }
                    }
                }
            }
            check(digest(archive) == "d54561979bd2e08a51e7dbd99ac36bb47564e089eefd403636dbca93e811bba2") {
                "Model checksum mismatch"
            }
            TarArchiveInputStream(BZip2CompressorInputStream(archive.inputStream().buffered())).use { tar ->
                while (true) {
                    currentCoroutineContext().ensureActive()
                    val entry = tar.nextEntry ?: break
                    val name = entry.name.substringAfterLast('/')
                    if (!entry.isFile || name !in expected) continue
                    val temporary = File(models, "$name.part")
                    try {
                        temporary.outputStream().buffered().use { tar.copyTo(it) }
                        check(digest(temporary) == expected[name])
                        val destination = File(models, name)
                        destination.delete()
                        check(temporary.renameTo(destination))
                    } finally { temporary.delete() }
                }
            }
            check(expected.keys.all { File(models, it).isFile })
        } finally { archive.delete() }
    }

    private fun digest(file: File): String {
        val hash = MessageDigest.getInstance("SHA-256")
        file.inputStream().buffered().use { input ->
            val bytes = ByteArray(65536)
            while (true) {
                val count = input.read(bytes)
                if (count < 0) break
                hash.update(bytes, 0, count)
            }
        }
        return hash.digest().joinToString("") { "%02x".format(it) }
    }

    companion object {
        private const val MODEL_URL = "https://github.com/k2-fsa/sherpa-onnx/releases/download/source-separation-models/sherpa-onnx-spleeter-2stems-fp16.tar.bz2"
        private fun digest(bytes: ByteArray) = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
    }
}
