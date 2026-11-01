/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.lyrics

import android.content.Context
import com.atilika.kuromoji.dict.CharacterDefinitions
import com.atilika.kuromoji.dict.ConnectionCosts
import com.atilika.kuromoji.dict.InsertedDictionary
import com.atilika.kuromoji.dict.TokenInfoDictionary
import com.atilika.kuromoji.dict.UnknownDictionary
import com.atilika.kuromoji.ipadic.Token
import com.atilika.kuromoji.ipadic.Tokenizer
import com.atilika.kuromoji.trie.DoubleArrayTrie
import com.atilika.kuromoji.util.ResourceResolver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import timber.log.Timber
import java.io.File
import java.io.FilterInputStream
import java.io.IOException
import java.io.InputStream
import java.security.MessageDigest
import java.util.concurrent.atomic.AtomicBoolean
import java.util.zip.ZipEntry
import java.util.zip.ZipFile

/**
 * Manages on-demand download of the IPADIC dictionary used by kuromoji to romanize
 * Japanese lyrics.
 *
 * The dictionary binaries are stripped from the APK at build time (see the packaging
 * excludes in app/build.gradle.kts) because they account for more than half of the APK
 * size while being useful only for a niche feature. When needed, the official
 * kuromoji-ipadic jar is fetched once from Maven Central, checksum-verified, and kept in
 * internal storage. Dictionary entries are read directly from that zip, so no extraction
 * step is required.
 */
object JapaneseDictManager {
    private const val TAG = "JapaneseDictManager"

    private const val DICT_JAR_URL =
        "https://repo1.maven.org/maven2/com/atilika/kuromoji/kuromoji-ipadic/0.9.0/kuromoji-ipadic-0.9.0.jar"

    private const val DICT_JAR_SHA1 = "f0ce8fadfc1f1f9f77b25fd4e78a129cc0f76062"
    private const val DICT_ENTRY_PREFIX = "com/atilika/kuromoji/ipadic/"
    private const val DICT_JAR_NAME = "kuromoji_ipadic.jar"
    private const val TEMP_JAR_NAME = "kuromoji_ipadic.tmp"

    private const val KANJI_LENGTH_THRESHOLD = 2
    private const val KANJI_PENALTY = 3000
    private const val OTHER_LENGTH_THRESHOLD = 7
    private const val OTHER_PENALTY = 1700

    @Volatile
    private var appContext: Context? = null

    private val httpClient = OkHttpClient()

    private val availabilityCache = AtomicBoolean(false)
    private val availabilityChecked = AtomicBoolean(false)

    private val tokenizerMutex = Mutex()

    @Volatile
    private var tokenizer: Tokenizer? = null

    fun init(context: Context) {
        if (appContext == null) {
            appContext = context.applicationContext
        }
    }

    fun isDownloaded(): Boolean {
        if (availabilityChecked.get()) return availabilityCache.get()
        val context = appContext ?: return false
        val file = dictJarFile(context)
        val valid = file.exists() && sha1Hex(file).equals(DICT_JAR_SHA1, ignoreCase = true)
        if (file.exists() && !valid) {
            file.delete()
            Timber.tag(TAG).w("Discarded invalid Japanese dictionary at %s", file.absolutePath)
        }
        availabilityCache.set(valid)
        availabilityChecked.set(true)
        return valid
    }

    /**
     * Downloads the dictionary jar from Maven Central, reporting progress as a percentage.
     * The file is only moved into place after its SHA-1 has been verified.
     */
    suspend fun download(onProgress: (Int) -> Unit): Result<Unit> =
        withContext(Dispatchers.IO) {
            val context =
                appContext ?: return@withContext Result.failure(
                    IllegalStateException("JapaneseDictManager.init() was never called"),
                )
            try {
                val request = Request.Builder().url(DICT_JAR_URL).build()
                httpClient.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        throw IOException("HTTP ${response.code} while downloading Japanese dictionary")
                    }
                    val body = response.body ?: throw IOException("Empty response body")
                    val contentLength = body.contentLength()
                    val tempFile = File(context.filesDir, TEMP_JAR_NAME)
                    val digest = MessageDigest.getInstance("SHA-1")

                    body.byteStream().use { input ->
                        tempFile.outputStream().use { output ->
                            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                            var totalRead = 0L
                            while (true) {
                                val read = input.read(buffer)
                                if (read == -1) break
                                output.write(buffer, 0, read)
                                digest.update(buffer, 0, read)
                                totalRead += read
                                if (contentLength > 0) {
                                    onProgress((totalRead * 100 / contentLength).toInt())
                                }
                            }
                        }
                    }

                    val actualSha1 = digest.digest().joinToString("") { "%02x".format(it) }
                    if (!actualSha1.equals(DICT_JAR_SHA1, ignoreCase = true)) {
                        tempFile.delete()
                        throw IOException("Japanese dictionary checksum mismatch ($actualSha1)")
                    }

                    val target = dictJarFile(context)
                    target.delete()
                    if (!tempFile.renameTo(target)) {
                        throw IOException("Could not move downloaded dictionary into place")
                    }

                    availabilityCache.set(true)
                    availabilityChecked.set(true)
                    Timber.tag(TAG).i("Japanese dictionary downloaded (%d bytes)", target.length())
                    Result.success(Unit)
                }
            } catch (e: Exception) {
                Timber.tag(TAG).e(e, "Failed to download Japanese dictionary")
                Result.failure(e)
            }
        }

    /**
     * Tokenizes [text] with the IPADIC model, loading it lazily on first use.
     * Returns null when the dictionary has not been downloaded yet so callers can fall
     * back to leaving the text unchanged.
     */
    suspend fun tokenize(text: String): List<Token>? =
        withContext(Dispatchers.Default) {
            getOrCreateTokenizer()?.tokenize(text)
        }

    private suspend fun getOrCreateTokenizer(): Tokenizer? {
        tokenizer?.let { return it }
        return tokenizerMutex.withLock {
            val existing = tokenizer
            if (existing != null) {
                existing
            } else {
                val context = appContext
                if (context == null || !isDownloaded()) {
                    null
                } else {
                    try {
                        OnDemandDictionaryBuilder(dictJarFile(context)).build().also {
                            tokenizer = it
                            Timber.tag(TAG).i("Japanese tokenizer ready")
                        }
                    } catch (e: Exception) {
                        Timber.tag(TAG).e(e, "Failed to build Japanese tokenizer")
                        null
                    }
                }
            }
        }
    }

    private fun dictJarFile(context: Context): File = File(context.filesDir, DICT_JAR_NAME)

    private fun sha1Hex(file: File): String {
        val digest = MessageDigest.getInstance("SHA-1")
        file.inputStream().use { input ->
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            while (true) {
                val read = input.read(buffer)
                if (read == -1) break
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    /**
     * Resolves kuromoji resource names against the entries stored inside the downloaded
     * jar instead of the classpath.
     */
    private class ZipResourceResolver(private val zipFilePath: String) : ResourceResolver {
        override fun resolve(resourceName: String): InputStream {
            val zip = ZipFile(zipFilePath)
            val entry: ZipEntry =
                zip.getEntry(DICT_ENTRY_PREFIX + resourceName)
                    ?: run {
                        zip.close()
                        throw IOException("$resourceName not found in $zipFilePath")
                    }
            return object : FilterInputStream(zip.getInputStream(entry)) {
                override fun close() {
                    super.close()
                    zip.close()
                }
            }.buffered(ZIP_STREAM_BUFFER_SIZE)
        }
    }

    /** Builds a tokenizer whose dictionaries come from the downloaded jar. */
    private class OnDemandDictionaryBuilder(dictZip: File) : Tokenizer.Builder() {
        init {
            resolver = ZipResourceResolver(dictZip.absolutePath)
        }

        override fun loadDictionaries() {
            penalties =
                arrayListOf(
                    KANJI_LENGTH_THRESHOLD,
                    KANJI_PENALTY,
                    OTHER_LENGTH_THRESHOLD,
                    OTHER_PENALTY,
                )
            try {
                doubleArrayTrie = DoubleArrayTrie.newInstance(resolver)
                connectionCosts = ConnectionCosts.newInstance(resolver)
                tokenInfoDictionary = TokenInfoDictionary.newInstance(resolver)
                characterDefinitions = CharacterDefinitions.newInstance(resolver)
                unknownDictionary =
                    UnknownDictionary.newInstance(resolver, characterDefinitions, totalFeatures)
                insertedDictionary = InsertedDictionary(totalFeatures)
            } catch (e: Exception) {
                throw RuntimeException("Could not load Japanese dictionaries.", e)
            }
        }
    }
}

private const val DEFAULT_BUFFER_SIZE = 128 * 1024
private const val ZIP_STREAM_BUFFER_SIZE = 64 * 1024
