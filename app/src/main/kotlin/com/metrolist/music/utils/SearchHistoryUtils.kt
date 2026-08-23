/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.utils

import com.metrolist.innertube.models.AlbumItem
import com.metrolist.innertube.models.ArtistItem
import com.metrolist.innertube.models.EpisodeItem
import com.metrolist.innertube.models.PlaylistItem
import com.metrolist.innertube.models.PodcastItem
import com.metrolist.innertube.models.SongItem
import com.metrolist.innertube.models.YTItem
import com.metrolist.music.db.MusicDatabase
import com.metrolist.music.db.entities.SearchHistory

fun MusicDatabase.recordSearchHistoryOpen(searchQuery: String, item: YTItem) {
    if (searchQuery.isBlank()) return
    val type = when (item) {
        is SongItem, is EpisodeItem -> "song"
        is AlbumItem -> "album"
        is ArtistItem -> "artist"
        is PlaylistItem, is PodcastItem -> "playlist"
    }
    query {
        insert(
            SearchHistory(
                query = searchQuery,
                itemId = item.id,
                itemType = type,
                title = item.title,
                thumbnailUrl = item.thumbnail,
            ),
        )
    }
}
