/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.playback.queues

import androidx.media3.common.MediaItem
import com.metrolist.music.db.entities.AlbumWithSongs
import com.metrolist.music.extensions.toMediaItem
import com.metrolist.music.models.MediaMetadata
import com.metrolist.music.models.toMediaMetadata
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.withContext

class LocalAlbumRadio(
    private val albumWithSongs: AlbumWithSongs,
    private val startIndex: Int = 0,
) : Queue {
    override val preloadItem: MediaMetadata? = null
    override val sourceRoute: String = "album/${albumWithSongs.album.id}"

    private val recommendations = AlbumRadioContinuation(albumWithSongs.songs.map { it.toMediaMetadata() })

    override suspend fun getInitialStatus(): Queue.Status = withContext(IO) {
        Queue.Status(
            title = albumWithSongs.album.title,
            items = albumWithSongs.songs.map { it.toMediaItem() },
            mediaItemIndex = startIndex
        )
    }

    override fun hasNextPage(): Boolean = recommendations.hasNextPage()

    override suspend fun nextPage(): List<MediaItem> = withContext(IO) {
        recommendations.nextPage()
    }
}
