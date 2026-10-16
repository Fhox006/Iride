package com.metrolist.music.utils

import android.content.Context
import android.util.AtomicFile
import com.metrolist.innertube.pages.HomePage
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

object HomeFeedSnapshotStore {
    private const val FILE_NAME = "home_feed_snapshot.json"

    private val json = Json { ignoreUnknownKeys = true }

    @Serializable
    data class Snapshot(
        val savedAt: Long,
        val page: HomePage,
    )

    @Synchronized
    fun load(context: Context): Snapshot? =
        runCatching {
            val file = AtomicFile(File(context.filesDir, FILE_NAME))
            json.decodeFromString<Snapshot>(file.readFully().toString(Charsets.UTF_8))
                .takeIf { it.page.sections.isNotEmpty() }
        }.getOrNull()

    @Synchronized
    fun save(context: Context, page: HomePage) {
        if (page.sections.isEmpty()) return
        runCatching {
            val data = json.encodeToString(Snapshot(savedAt = System.currentTimeMillis(), page = page))
            val file = AtomicFile(File(context.filesDir, FILE_NAME))
            val output = file.startWrite()
            try {
                output.write(data.toByteArray(Charsets.UTF_8))
                file.finishWrite(output)
            } catch (e: Exception) {
                file.failWrite(output)
                throw e
            }
        }
    }

    @Synchronized
    fun clear(context: Context) {
        AtomicFile(File(context.filesDir, FILE_NAME)).delete()
    }
}
