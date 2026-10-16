package com.metrolist.music.playback.vocals

import android.content.Context
import android.os.Build
import android.os.Process
import com.metrolist.music.R
import java.io.File
import java.security.MessageDigest
import java.util.zip.ZipFile
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.StateFlow
import okhttp3.OkHttpClient
import okhttp3.Request

/** Installs the pinned runtime for this device, yielding bandwidth during background preparation. */
internal class VocalNativeRuntime(private val context: Context) {
    fun isInstalled(): Boolean {
        val supportedAbis = if (Process.is64Bit()) Build.SUPPORTED_64_BIT_ABIS else Build.SUPPORTED_32_BIT_ABIS
        val abi = supportedAbis.firstOrNull { it in LIBRARY_HASHES } ?: return false
        val directory = File(context.filesDir, "vocal-runtime/1.13.8/$abi")
        return checkNotNull(LIBRARY_HASHES[abi]).keys.all { File(directory, it).length() > 0 }
    }

    suspend fun ensureInstalled(client: OkHttpClient, foreground: StateFlow<Boolean>, progress: (Float) -> Unit): File {
        val supportedAbis = if (Process.is64Bit()) Build.SUPPORTED_64_BIT_ABIS else Build.SUPPORTED_32_BIT_ABIS
        val abi = supportedAbis.firstOrNull { it in LIBRARY_HASHES }
            ?: throw VocalPreparationException(context.getString(R.string.vocal_engine_unavailable))
        val hashes = checkNotNull(LIBRARY_HASHES[abi])
        val directory = File(context.filesDir, "vocal-runtime/1.13.8/$abi")
        if (hashes.all { (name, hash) -> File(directory, name).let { it.isFile && digest(it) == hash } }) {
            return directory
        }
        progress(0f)
        check(directory.mkdirs() || directory.isDirectory)
        val archive = File.createTempFile("vocal-runtime-", ".aar", context.cacheDir)
        try {
            client.newCall(Request.Builder().url(RUNTIME_URL).build()).execute().use { response ->
                check(response.isSuccessful) { "Runtime download: HTTP ${response.code}" }
                val body = checkNotNull(response.body)
                body.byteStream().use { input ->
                    archive.outputStream().buffered().use { output ->
                        val bytes = ByteArray(65536)
                        var total = 0L
                        while (true) {
                            currentCoroutineContext().ensureActive()
                            val count = input.read(bytes)
                            if (count < 0) break
                            total += count
                            check(total <= 64L * 1024 * 1024)
                            output.write(bytes, 0, count)
                            VocalProcessingBudget.awaitDownloadTurn(count, foreground)
                            progress(if (body.contentLength() > 0) (total.toFloat() / body.contentLength()).coerceIn(0f, 1f) else 0f)
                        }
                    }
                }
            }
            installArchive(archive, abi, directory)
            currentCoroutineContext().ensureActive()
            return directory
        } finally { archive.delete() }
    }

    companion object {
        private const val RUNTIME_URL = "https://github.com/k2-fsa/sherpa-onnx/releases/download/v1.13.8/sherpa-onnx-1.13.8.aar"
        private const val ARCHIVE_HASH = "633c24321e06b1fe79feafa03ea16cbc0f8a286641e2da3559bac91bdb13bd96"
        private val LIBRARY_HASHES = mapOf(
            "arm64-v8a" to mapOf(
                "libonnxruntime.so" to "33847ad43bffe204699fd4a27f7f3603452a8cdaf2f9a44983a0bc31ffcf2da1",
                "libsherpa-onnx-c-api.so" to "c85e2382e2ca12b0513826be926eaccfea896b4cbf568182e287671ba526f80d",
            ),
            "armeabi-v7a" to mapOf(
                "libonnxruntime.so" to "7a8f17a707e52e0ac3a026be319272876d9eae61f4bb19585292772d785f7744",
                "libsherpa-onnx-c-api.so" to "2294b9cc929ca0d22c5da52864ec8176f8fd119a739c9d594384763f6101d33f",
            ),
            "x86" to mapOf(
                "libonnxruntime.so" to "fcb6cd80415f7951e58fdd8158a238946dce3710d5fee492b67174d5131f9eee",
                "libsherpa-onnx-c-api.so" to "fc08ee45be6eaafde43e128f22f130aec4ef2c5362e44d95f7ae54e9ad6e752e",
            ),
            "x86_64" to mapOf(
                "libonnxruntime.so" to "087e84297cad64acb823c3d901d6e0cc934e91add7cdb20da802cd1c0bea5c3e",
                "libsherpa-onnx-c-api.so" to "631ca2828e47b61d8afe4b3c75a157423cbafaba6f8c0db89c27d21f1b173d9a",
            ),
        )

        internal fun installArchive(archive: File, abi: String, directory: File) {
            check(digest(archive) == ARCHIVE_HASH) { "Runtime checksum mismatch" }
            val hashes = checkNotNull(LIBRARY_HASHES[abi])
            check(directory.mkdirs() || directory.isDirectory)
            ZipFile(archive).use { zip ->
                for ((name, hash) in hashes) {
                    val entry = checkNotNull(zip.getEntry("jni/$abi/$name"))
                    check(entry.size in 1..40L * 1024 * 1024)
                    val pending = File.createTempFile("$name-", ".part", directory)
                    try {
                        zip.getInputStream(entry).use { input -> pending.outputStream().buffered().use { input.copyTo(it) } }
                        check(digest(pending) == hash) { "Native library checksum mismatch" }
                        check(pending.setReadOnly())
                        val destination = File(directory, name)
                        check(!destination.exists() || destination.delete())
                        check(pending.renameTo(destination))
                    } finally { pending.delete() }
                }
            }
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
    }
}
