package com.metrolist.music.data.remote

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.os.Build
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.OutputStream
import java.security.MessageDigest
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import okhttp3.OkHttpClient
import okhttp3.Request
import timber.log.Timber

@Singleton
internal class ArtistVideoFrameLoader @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val decoder = Mutex()
    private val client = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.SECONDS)
        .callTimeout(8, TimeUnit.SECONDS)
        .build()

    suspend fun load(url: String, resolution: Int): Bitmap? = withContext(Dispatchers.IO) {
        decoder.withLock {
            try {
                withTimeout(20_000) {
                    val directory = File(context.cacheDir, "artist_video_frames").apply { mkdirs() }
                    val digest = MessageDigest.getInstance("SHA-256").digest(url.toByteArray())
                        .joinToString("") { "%02x".format(it) }
                    val cached = File(directory, "${digest}_$resolution.png")
                    if (cached.isFile) {
                        BitmapFactory.decodeFile(cached.path)?.let { return@withTimeout it }
                        cached.delete()
                    }
                    val segment = firstSegment(url, resolution) ?: return@withTimeout null
                    val video = File.createTempFile("artist_frame_", ".mp4", directory)
                    val frame = try {
                        video.outputStream().use { output ->
                            segment.initialization?.let { download(it, output) }
                            download(segment.media, output)
                        }
                        currentCoroutineContext().ensureActive()
                        decode(video, resolution)
                    } finally {
                        video.delete()
                    } ?: return@withTimeout null
                    // Cache failure must not discard a successfully decoded portrait.
                    try {
                        val temporary = File.createTempFile("artist_still_", ".png", directory)
                        try {
                            temporary.outputStream().use { check(frame.compress(Bitmap.CompressFormat.PNG, 100, it)) }
                            check(temporary.renameTo(cached))
                        } finally {
                            temporary.delete()
                        }
                        directory.listFiles()?.filter { it.extension == "png" }
                            ?.sortedByDescending(File::lastModified)?.drop(16)?.forEach(File::delete)
                    } catch (error: Exception) {
                        Timber.d(error, "Unable to cache artist video frame")
                    }
                    frame
                }
            } catch (cancelled: CancellationException) {
                // A local timeout falls back to the supplied preview; screen cancellation still propagates.
                currentCoroutineContext().ensureActive()
                null
            } catch (error: Exception) {
                Timber.d(error, "Unable to decode artist video frame")
                null
            } catch (error: LinkageError) {
                Timber.d(error, "Artist video decoder unavailable")
                null
            }
        }
    }

    private suspend fun firstSegment(url: String, resolution: Int): ArtistVideoSegment? {
        var playlistUrl = resolveArtistVideoUrl(url, url) ?: return null
        repeat(3) {
            val bytes = java.io.ByteArrayOutputStream()
            download(ArtistVideoResource(playlistUrl), bytes, maximumBytes = 512 * 1024)
            val playlist = bytes.toString(Charsets.UTF_8.name())
            if ("#EXT-X-STREAM-INF:" !in playlist) return firstArtistVideoSegment(playlist, playlistUrl)
            // Avoid HEVC Main 10 and large native decoder allocations for a decorative still.
            val rendition = parseAMHlsVariants(playlist, playlistUrl)
                .filter { it.isH264 && it.width in 1..1920 && it.height in 1..1920 }
                .pickAMRendition(resolution) ?: return null
            playlistUrl = resolveArtistVideoUrl(playlistUrl, rendition.uri) ?: return null
        }
        return null
    }

    private suspend fun download(resource: ArtistVideoResource, output: OutputStream, maximumBytes: Int = 12 * 1024 * 1024) {
        currentCoroutineContext().ensureActive()
        val request = Request.Builder().url(resource.url).apply {
            resource.range?.let {
                require(it.last - it.first < maximumBytes)
                header("Range", "bytes=${it.first}-${it.last}")
            }
        }.build()
        client.newCall(request).execute().use { response ->
            check(response.isSuccessful)
            if (resource.range != null) check(response.code == 206)
            val body = checkNotNull(response.body)
            check(body.contentLength() <= maximumBytes)
            body.byteStream().use { input ->
                val buffer = ByteArray(16 * 1024)
                var total = 0
                while (true) {
                    currentCoroutineContext().ensureActive()
                    val count = input.read(buffer)
                    if (count < 0) break
                    total += count
                    check(total <= maximumBytes)
                    output.write(buffer, 0, count)
                }
                resource.range?.let { check(total.toLong() == it.last - it.first + 1) }
            }
        }
    }

    private fun decode(file: File, resolution: Int): Bitmap? {
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(file.path)
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                retriever.getScaledFrameAtTime(0, MediaMetadataRetriever.OPTION_CLOSEST, resolution, resolution)
            } else {
                val width = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull() ?: return null
                val height = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull() ?: return null
                if (width !in 1..1920 || height !in 1..1920) return null
                retriever.getFrameAtTime(0, MediaMetadataRetriever.OPTION_CLOSEST)?.let { frame ->
                    val scale = minOf(1f, resolution.toFloat() / maxOf(frame.width, frame.height))
                    Bitmap.createScaledBitmap(frame, (frame.width * scale).toInt().coerceAtLeast(1),
                        (frame.height * scale).toInt().coerceAtLeast(1), true).also {
                        if (it !== frame) frame.recycle()
                    }
                }
            }
        } finally {
            retriever.release()
        }
    }
}
