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
@Entity(tableName = "artist")
data class ArtistEntity(
    @PrimaryKey val id: String,
    val name: String,
    val thumbnailUrl: String? = null,
    val channelId: String? = null,
    val lastUpdateTime: LocalDateTime = LocalDateTime.now(),
    val bookmarkedAt: LocalDateTime? = null,
    @ColumnInfo(name = "isLocal", defaultValue = false.toString())
    val isLocal: Boolean = false,
    @ColumnInfo(name = "isPodcastChannel", defaultValue = false.toString())
    val isPodcastChannel: Boolean = false
) {
    val isYouTubeArtist: Boolean
        get() = id.startsWith("UC") || id.startsWith("FEmusic_library_privately_owned_artist")

    val isPrivatelyOwnedArtist: Boolean
        get() = id.startsWith("FEmusic_library_privately_owned_artist")

    fun localToggleLike() = copy(
        bookmarkedAt = if (bookmarkedAt != null) null else LocalDateTime.now(),
    )

    // Immediate sync via shared supervised scope (same package: SongEntity.kt).
    // runCatching: nothing ever reaches the uncaught handler.
    fun toggleLike(): ArtistEntity = localToggleLike().also {
        if (!isLocal && !isPrivatelyOwnedArtist) {
            EntitySyncScope.launch {
                runCatching {
                    val targetChannelId = channelId ?: YouTube.getChannelId(id)
                    if (targetChannelId.isNotEmpty()) {
                        YouTube.subscribeChannel(targetChannelId, bookmarkedAt == null).getOrThrow()
                    }
                }.onFailure { error -> Timber.e(error, "Failed to sync artist subscription: %s", id) }
            }
        }
    }

    companion object {
        fun generateArtistId() = "LA" + RandomStringUtils.insecure().next(8, true, false)
    }
}
