/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.extensions

import com.metrolist.music.db.entities.Album
import com.metrolist.music.db.entities.Artist
import com.metrolist.music.db.entities.Playlist
import com.metrolist.music.db.entities.Song

// Minimum plays for an artist to count as a real listening habit rather than
// a one-off (e.g. a couple of tracks heard once at a party).
private const val MIN_GENUINE_ARTIST_SONG_COUNT = 3

// ...unless the user genuinely binged them, even on an artist heard only once.
private const val MIN_GENUINE_ARTIST_TIME_LISTENED_MS = 20 * 60 * 1000L

// Filters out artists that only showed up because of a handful of plays
// (party playlists, one-off genres) rather than an actual listening habit.
fun List<Artist>.filterGenuineFavorites() =
    filter { it.songCount >= MIN_GENUINE_ARTIST_SONG_COUNT || (it.timeListened ?: 0) >= MIN_GENUINE_ARTIST_TIME_LISTENED_MS }

fun <T> List<T>.reversed(reversed: Boolean) = if (reversed) asReversed() else this

fun <T> MutableList<T>.move(
    fromIndex: Int,
    toIndex: Int,
): MutableList<T> {
    add(toIndex, removeAt(fromIndex))
    return this
}

fun <T : Any> List<T>.mergeNearbyElements(
    key: (T) -> Any = { it },
    merge: (first: T, second: T) -> T = { first, _ -> first },
): List<T> {
    if (isEmpty()) return emptyList()

    val mergedList = mutableListOf<T>()
    var currentItem = this[0]

    for (i in 1 until size) {
        val nextItem = this[i]
        if (key(currentItem) == key(nextItem)) {
            currentItem = merge(currentItem, nextItem)
        } else {
            mergedList.add(currentItem)
            currentItem = nextItem
        }
    }
    mergedList.add(currentItem)

    return mergedList
}

// Extension function to filter explicit content for local Song entities
fun List<Song>.filterExplicit(enabled: Boolean = true) =
    if (enabled) {
        filter { !it.song.explicit }
    } else {
        this
    }

// Extension function to filter video songs for local Song entities
fun List<Song>.filterVideoSongs(enabled: Boolean = true) =
    if (enabled) {
        filter { !it.song.isVideo }
    } else {
        this
    }

// Extension function to filter explicit content for local Album entities
fun List<Album>.filterExplicitAlbums(enabled: Boolean = true) =
    if (enabled) {
        filter { !it.album.explicit }
    } else {
        this
    }

// Extension function to filter YouTube Shorts playlist
fun List<Playlist>.filterYoutubeShorts(enabled: Boolean = false) =
    if (enabled) {
        filterNot { it.playlist.browseId?.startsWith("SS") == true }
    } else {
        this
    }
