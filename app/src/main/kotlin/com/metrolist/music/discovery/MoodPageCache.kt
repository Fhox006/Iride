package com.metrolist.music.discovery

import android.content.Context
import android.util.AtomicFile
import com.metrolist.innertube.YouTube
import com.metrolist.innertube.pages.HomePage
import java.io.File
import java.security.MessageDigest
import java.util.Locale
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import timber.log.Timber

internal class MoodPageCache(context: Context) {
    private val directory = File(context.filesDir, "mood_pages")
    private val json = Json { ignoreUnknownKeys = true }
    private val mutex = Mutex()

    fun file(title: String, filters: Triple<Boolean, Boolean, Boolean>): AtomicFile {
        val category = (MixMood.fromChip(title)?.label ?: title).trim().lowercase(Locale.ROOT)
        val identity = "${YouTube.dataSyncId ?: YouTube.visitorData.orEmpty()}|$category|$filters"
        val key = MessageDigest.getInstance("SHA-256")
            .digest(identity.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) }
        return AtomicFile(File(directory, "$key.json"))
    }

    suspend fun read(file: AtomicFile): HomePage? = mutex.withLock {
        try {
            json.decodeFromString<HomePage>(file.openRead().bufferedReader().use { it.readText() })
                .takeIf { MoodMixes.select(it.sections.flatMap { section -> section.items }).isNotEmpty() }
        } catch (_: Exception) {
            null
        }
    }

    suspend fun write(file: AtomicFile, page: HomePage) = mutex.withLock {
        if (!directory.isDirectory && !directory.mkdirs()) return@withLock
        val bytes = json.encodeToString(page).toByteArray(Charsets.UTF_8)
        try {
            val stream = file.startWrite()
            try {
                stream.write(bytes)
                file.finishWrite(stream)
            } catch (error: Exception) {
                file.failWrite(stream)
                throw error
            }
        } catch (error: Exception) {
            Timber.w(error, "Unable to cache mood playlists")
        }
    }
}
