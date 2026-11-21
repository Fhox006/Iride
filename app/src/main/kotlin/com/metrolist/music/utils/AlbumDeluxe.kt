package com.metrolist.music.utils

import com.metrolist.innertube.models.SongItem
import com.metrolist.innertube.pages.AlbumPage
import com.metrolist.innertube.utils.albumReleaseDate
import java.util.Locale
import kotlin.math.abs

internal const val AlbumDeluxeGroupingEnabled = false

data class AlbumDeluxeBlock(
    val sourceAlbumId: String,
    val sourceAlbumTitle: String,
    val songIds: List<String>,
)

/** Compare the complete track list, before any display filters are applied. */
internal fun sharedAlbumEdge(current: List<SongItem>, previous: List<SongItem>): List<String>? {
    fun String.normalized() = trim().lowercase(Locale.ROOT).replace(Regex("\\s+"), " ")
    fun sameRecording(song: SongItem, other: SongItem): Boolean {
        val duration = song.duration
        val otherDuration = other.duration
        return song.id == other.id ||
        (song.title.normalized() == other.title.normalized() &&
            song.artists.isNotEmpty() && other.artists.isNotEmpty() &&
            song.artists.map { it.name.normalized() }.toSet() == other.artists.map { it.name.normalized() }.toSet() &&
            duration != null && otherDuration != null && abs(duration - otherDuration) <= 2 &&
            song.explicit == other.explicit)
    }

    val shared = current.indices.filter { index -> previous.any { sameRecording(current[index], it) } }
    if (shared.size < 3 || shared.last() - shared.first() + 1 != shared.size) return null
    if (shared.first() != 0 && shared.last() != current.lastIndex) return null
    // Repeated copies of one recording must not satisfy the three-track threshold.
    val distinctRecordings = previous.filter { old -> shared.any { sameRecording(current[it], old) } }
        .distinctBy { it.title.normalized() to it.artists.map { artist -> artist.name.normalized() }.toSet() }
    if (distinctRecordings.size < 3) return null
    return shared.map { current[it].id }
}

internal fun detectAlbumDeluxe(current: AlbumPage, previous: AlbumPage): AlbumDeluxeBlock? {
    if (current.album.id == previous.album.id) return null
    val currentDate = albumReleaseDate(current.releaseDate ?: current.album.releaseDate, current.album.year) ?: return null
    val previousDate = albumReleaseDate(previous.releaseDate ?: previous.album.releaseDate, previous.album.year) ?: return null
    if (!previousDate.isBefore(currentDate)) return null
    // A year alone cannot establish which of two releases in that year came first.
    if (previousDate.year == currentDate.year &&
        ((current.releaseDate ?: current.album.releaseDate)?.length != 10 ||
            (previous.releaseDate ?: previous.album.releaseDate)?.length != 10)
    ) return null
    val shared = sharedAlbumEdge(current.songs, previous.songs) ?: return null
    return AlbumDeluxeBlock(previous.album.id, previous.album.title, shared)
}
