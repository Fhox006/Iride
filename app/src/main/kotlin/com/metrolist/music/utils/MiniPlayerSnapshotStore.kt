package com.metrolist.music.utils

import android.content.Context
import android.content.SharedPreferences
import com.metrolist.music.models.MediaMetadata
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

/** A small display-only snapshot; playback and queue restoration remain owned by MusicService. */
object MiniPlayerSnapshotStore {
    private lateinit var preferences: SharedPreferences
    private val snapshot = MutableStateFlow<MediaMetadata?>(null)
    val metadata = snapshot.asStateFlow()

    fun bottomInsetPx(layoutKey: String): Int = preferences.getInt("bottom_inset_$layoutKey", 0)

    fun updateBottomInset(layoutKey: String, insetPx: Int) {
        if (bottomInsetPx(layoutKey) != insetPx) {
            preferences.edit().putInt("bottom_inset_$layoutKey", insetPx).apply()
        }
    }

    fun initialize(context: Context) {
        preferences = context.getSharedPreferences("mini_player_snapshot", Context.MODE_PRIVATE)
        snapshot.value = runCatching {
            val raw = preferences.getString("metadata", null) ?: return@runCatching null
            val json = JSONObject(raw)
            val artists = json.getJSONArray("artists")
            MediaMetadata(
                id = json.getString("id"),
                title = json.getString("title"),
                artists = List(artists.length()) { index ->
                    val artist = artists.getJSONObject(index)
                    MediaMetadata.Artist(
                        id = artist.optString("id").takeIf { it.isNotEmpty() },
                        name = artist.getString("name"),
                    )
                },
                duration = json.getInt("duration"),
                thumbnailUrl = json.optString("thumbnail").takeIf { it.isNotEmpty() },
                liked = json.optBoolean("liked"),
            )
        }.getOrNull()
    }

    fun update(metadata: MediaMetadata?) {
        if (snapshot.value == metadata) return
        snapshot.value = metadata
        if (metadata == null) {
            preferences.edit().remove("metadata").apply()
            return
        }
        val json = JSONObject()
            .put("id", metadata.id)
            .put("title", metadata.title)
            .put("duration", metadata.duration)
            .put("thumbnail", metadata.thumbnailUrl.orEmpty())
            .put("liked", metadata.liked)
            .put("artists", JSONArray().apply {
                metadata.artists.forEach { artist ->
                    put(JSONObject().put("id", artist.id.orEmpty()).put("name", artist.name))
                }
            })
        preferences.edit().putString("metadata", json.toString()).apply()
    }
}
