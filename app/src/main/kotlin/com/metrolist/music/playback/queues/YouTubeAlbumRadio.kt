/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.playback.queues

import androidx.media3.common.MediaItem
import com.metrolist.innertube.YouTube
import com.metrolist.music.extensions.toMediaItem
import com.metrolist.music.models.MediaMetadata
import com.metrolist.music.models.toMediaMetadata
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.withContext

class YouTubeAlbumRadio(
    private val playlistId: String,
) : Queue {
    override val preloadItem: MediaMetadata? = null
    override var sourceRoute: String? = null
        private set

    private var recommendations: AlbumRadioContinuation? = null

    override suspend fun getInitialStatus(): Queue.Status = withContext(IO) {
        val albumSongs = YouTube.albumSongs(playlistId).getOrThrow()
        sourceRoute = albumSongs.firstOrNull()?.album?.id?.let { "album/$it" }
        recommendations = AlbumRadioContinuation(albumSongs.map { it.toMediaMetadata() })
        Queue.Status(
            title = albumSongs.firstOrNull()?.album?.name.orEmpty(),
            items = albumSongs.map { it.toMediaItem() },
            mediaItemIndex = 0
        )
    }

    override fun hasNextPage(): Boolean = recommendations?.hasNextPage() == true

    override suspend fun nextPage(): List<MediaItem> = withContext(IO) {
        recommendations?.nextPage().orEmpty()
    }
}
