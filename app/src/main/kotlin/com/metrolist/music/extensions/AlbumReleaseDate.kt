/** Metrolist Project (C) 2026 Licensed under GPL-3.0 | See git history for contributors */
package com.metrolist.music.extensions

import com.metrolist.innertube.utils.albumReleaseDate
import com.metrolist.music.db.entities.Album
import java.time.LocalDate
import java.util.Locale
import com.metrolist.innertube.utils.parseAlbumReleaseDate as parseReleaseDate

internal fun parseAlbumReleaseDate(value: String?): LocalDate? = parseReleaseDate(value)

fun List<Album>.sortedByReleaseDate(descending: Boolean): List<Album> {
    val dates = associate { album ->
        album.id to albumReleaseDate(album.album.releaseDate, album.album.year)
    }
    val chronological = if (descending) {
        compareByDescending<Album> { dates[it.id] }
    } else {
        compareBy<Album> { dates[it.id] }
    }
    val addedAtOrder = if (descending) {
        compareByDescending<Album> { it.album.bookmarkedAt ?: it.album.inLibrary ?: it.album.lastUpdateTime }
    } else {
        compareBy<Album> { it.album.bookmarkedAt ?: it.album.inLibrary ?: it.album.lastUpdateTime }
    }
    return sortedWith(
        compareBy<Album> { dates[it.id] == null }
            .then(chronological)
            .then(addedAtOrder)
            .thenBy { it.album.title.lowercase(Locale.ROOT) }
            .thenBy { it.id },
    )
}
