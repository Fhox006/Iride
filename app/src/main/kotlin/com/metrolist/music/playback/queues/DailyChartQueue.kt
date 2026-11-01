package com.metrolist.music.playback.queues

import androidx.media3.common.MediaItem
import com.metrolist.innertube.models.SongItem
import com.metrolist.music.data.remote.ChartTrack
import com.metrolist.music.extensions.toMediaItem
import com.metrolist.music.models.MediaMetadata

/** Resolves only upcoming tracks, preserving chart order without delaying the first song. */
class DailyChartQueue(
    private val title: String,
    tracks: List<ChartTrack>,
    private val firstSong: SongItem,
    hideExplicit: Boolean,
    resolve: suspend (ChartTrack) -> SongItem?,
    override val sourceRoute: String? = null,
) : Queue {
    private val pager = ChartTrackPager(tracks, hideExplicit, resolve)
    override val preloadItem: MediaMetadata? = null
    override val requiresPagination: Boolean = true

    override suspend fun getInitialStatus() = Queue.Status(title, listOf(firstSong.toMediaItem()), 0)

    override fun hasNextPage(): Boolean = pager.hasNextPage()

    override suspend fun nextPage(): List<MediaItem> = pager.nextPage().map { it.toMediaItem() }
}
