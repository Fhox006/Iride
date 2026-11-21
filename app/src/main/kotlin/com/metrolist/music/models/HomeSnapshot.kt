/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.models

import com.metrolist.innertube.models.AlbumItem
import com.metrolist.innertube.models.Artist
import com.metrolist.innertube.models.ArtistItem
import com.metrolist.innertube.models.PlaylistItem
import com.metrolist.innertube.models.SongItem
import com.metrolist.innertube.models.YTItem
import kotlinx.serialization.Serializable

@Serializable
data class HomeSnapshotItem(
    val id: String,
    val title: String,
    val subtitle: String? = null,
    val thumbnailUrl: String? = null,
    val type: String,
    val browseId: String? = null,
    val playlistId: String? = null,
    val explicit: Boolean = false,
    val year: Int? = null,
    val albumType: String? = null,
    val releaseDate: String? = null,
)

@Serializable
data class SpeedDialSnapshot(
    val updatedAt: Long,
    val items: List<HomeSnapshotItem>,
)

@Serializable
data class MoodSnapshot(
    val updatedAt: Long,
    val chipTitle: String,
    val chipParams: String? = null,
    val items: List<HomeSnapshotItem>,
)

@Serializable
data class HomeDailyDiscoverSnapshotItem(
    val seed: HomeSnapshotItem,
    val recommendation: HomeSnapshotItem,
)

@Serializable
data class HomeSimilarShelfSnapshot(
    val titleType: String,
    val titleId: String,
    val title: String,
    val thumbnailUrl: String? = null,
    val playlistBrowseId: String? = null,
    val items: List<HomeSnapshotItem>,
)

@Serializable
data class HomeForYouTileSnapshot(
    val type: String,
    val item: HomeSnapshotItem,
)

@Serializable
data class HomeForYouShelfSnapshot(
    val artistId: String,
    val artistName: String,
    val artistThumbnailUrl: String? = null,
    val tiles: List<HomeForYouTileSnapshot>,
)

@Serializable
data class HomeRecommendationSnapshot(
    val quickPicksUpdatedAt: Long = 0L,
    val quickPicks: List<HomeSnapshotItem> = emptyList(),
    val quickPicksAlgorithmVersion: Int = 0,
    val dailyDiscoverUpdatedAt: Long = 0L,
    val dailyDiscover: List<HomeDailyDiscoverSnapshotItem> = emptyList(),
    val communityUpdatedAt: Long = 0L,
    val communityPlaylists: List<HomeSnapshotItem> = emptyList(),
    val similarUpdatedAt: Long = 0L,
    val similarShelves: List<HomeSimilarShelfSnapshot> = emptyList(),
    val forYouUpdatedAt: Long = 0L,
    val forYouAlgorithmVersion: Int = 0,
    val forYouShelves: List<HomeForYouShelfSnapshot> = emptyList(),
)

fun HomeSnapshotItem.toYTItem(): YTItem? = when (type) {
    "song" -> SongItem(
        id = id,
        title = title,
        artists = subtitle?.split(", ")?.map { Artist(name = it, id = null) } ?: emptyList(),
        thumbnail = thumbnailUrl ?: "",
        explicit = explicit,
    )
    "album" -> AlbumItem(
        browseId = browseId ?: id,
        playlistId = playlistId ?: "",
        title = title,
        artists = subtitle?.split(", ")?.map { Artist(name = it, id = null) },
        thumbnail = thumbnailUrl ?: "",
        explicit = explicit,
        year = year,
        albumType = albumType,
        releaseDate = releaseDate,
    )
    "artist" -> ArtistItem(
        id = id,
        title = title,
        thumbnail = thumbnailUrl,
        shuffleEndpoint = null,
        radioEndpoint = null,
    )
    "playlist" -> PlaylistItem(
        id = id,
        title = title,
        author = subtitle?.let { Artist(name = it, id = null) },
        songCountText = null,
        thumbnail = thumbnailUrl,
        playEndpoint = null,
        shuffleEndpoint = null,
        radioEndpoint = null,
    )
    else -> null
}

fun HomeSnapshotItem.toPlaylistItem(): PlaylistItem? {
    if (type != "playlist") return null
    return PlaylistItem(
        id = id,
        title = title,
        author = subtitle?.let { Artist(name = it, id = null) },
        songCountText = null,
        thumbnail = thumbnailUrl,
        playEndpoint = null,
        shuffleEndpoint = null,
        radioEndpoint = null,
    )
}
