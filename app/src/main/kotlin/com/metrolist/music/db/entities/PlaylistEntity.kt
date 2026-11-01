/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.db.entities

import androidx.compose.runtime.Immutable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.metrolist.innertube.YouTube
import kotlinx.coroutines.launch
import timber.log.Timber
import org.apache.commons.lang3.RandomStringUtils
import java.time.LocalDateTime

@Immutable
@Entity(tableName = "playlist")
data class PlaylistEntity(
    @PrimaryKey val id: String = generatePlaylistId(),
    val name: String,
    val browseId: String? = null,
    val createdAt: LocalDateTime? = LocalDateTime.now(),
    val lastUpdateTime: LocalDateTime? = LocalDateTime.now(),
    @ColumnInfo(name = "isEditable", defaultValue = true.toString())
    val isEditable: Boolean = true,
    val bookmarkedAt: LocalDateTime? = null,
    val remoteSongCount: Int? = null,
    val playEndpointParams: String? = null,
    val thumbnailUrl: String? = null,
    val shuffleEndpointParams: String? = null,
    val radioEndpointParams: String? = null,
    @ColumnInfo(name = "isLocal", defaultValue = false.toString())
    val isLocal: Boolean = false,
    @ColumnInfo(name = "isAutoSync", defaultValue = false.toString())
    val isAutoSync: Boolean = false
) {
    companion object {
        const val LIKED_PLAYLIST_ID = "LP_LIKED"
        const val DOWNLOADED_PLAYLIST_ID = "LP_DOWNLOADED"
        const val STARRED_PLAYLIST_ID = "LP_STARRED"
        const val WEEKLY_MOST_PLAYLIST_ID = "LP_WEEKLY_MOST"
        const val MONTHLY_MOST_PLAYLIST_ID = "LP_MONTHLY_MOST"
        const val DISCOVER_WEEKLY_PLAYLIST_ID = "LP_DISCOVER_WEEKLY"

        fun generatePlaylistId() = "LP" + RandomStringUtils.insecure().next(8, true, false)
    }

    val shareLink: String?
        get() {
            return if (browseId != null)
                "https://music.youtube.com/playlist?list=$browseId"
            else null
        }

    fun localToggleLike() = copy(
        bookmarkedAt = if (bookmarkedAt != null) null else LocalDateTime.now()
    )

    // Immediate sync via shared supervised scope (same package: SongEntity.kt).
    // runCatching: nothing ever reaches the uncaught handler.
    fun toggleLike(): PlaylistEntity = localToggleLike().also {
        browseId?.takeUnless { isLocal }?.let { id ->
            EntitySyncScope.launch {
                runCatching { YouTube.likePlaylist(id, bookmarkedAt == null).getOrThrow() }
                    .onFailure { error -> Timber.e(error, "Failed to sync playlist like: %s", id) }
            }
        }
    }
}
