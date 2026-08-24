package com.metrolist.music.utils

import android.content.Context
import com.metrolist.innertube.pages.HomePage
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

object HomeFeedSnapshotStore {
    private const val FILE_NAME = "home_feed_snapshot.json"
    private const val MAX_AGE_MS = 12 * 60 * 60 * 1000L

    private val json = Json { ignoreUnknownKeys = true }

    @Serializable
    data class Snapshot(
        val savedAt: Long,
        val page: HomePage,
    )

    fun load(context: Context): Snapshot? =
        runCatching {
            val file = File(context.filesDir, FILE_NAME)
            if (!file.exists()) return@runCatching null
            if (System.currentTimeMillis() - file.lastModified() > MAX_AGE_MS) {
                file.delete()
                return@runCatching null
            }
            json.decodeFromString<Snapshot>(file.readText()).takeIf { it.page.sections.isNotEmpty() }
        }.getOrNull()

    fun save(context: Context, page: HomePage) {
        runCatching {
            val tmp = File(context.filesDir, "$FILE_NAME.tmp")
            tmp.writeText(json.encodeToString(Snapshot(savedAt = System.currentTimeMillis(), page = page)))
            val dst = File(context.filesDir, FILE_NAME)
            if (dst.exists()) dst.delete()
            if (!tmp.renameTo(dst)) tmp.delete()
        }
    }

    fun clear(context: Context) {
        runCatching { File(context.filesDir, FILE_NAME).delete() }
    }
}
