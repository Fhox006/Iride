/** Metrolist Project (C) 2026 Licensed under GPL-3.0 | See git history for contributors */
package com.metrolist.music.extensions

import com.metrolist.music.db.entities.Album
import java.time.LocalDate
import java.time.Year
import java.time.YearMonth
import java.util.Locale

internal fun parseAlbumReleaseDate(value: String?): LocalDate? = runCatching {
    when (value?.length) {
        10 -> LocalDate.parse(value)
        7 -> YearMonth.parse(value).atDay(1)
        4 -> Year.parse(value).atDay(1)
        else -> null
    }
}.getOrNull()

fun List<Album>.sortedByReleaseDate(descending: Boolean): List<Album> {
    // Unknown dates stay last; partial dates use only the precision actually available.
    val dates = associate { album ->
        album.id to (parseAlbumReleaseDate(album.album.releaseDate)
            ?: album.album.year?.let { runCatching { Year.of(it).atDay(1) }.getOrNull() })
    }
    val chronological = if (descending) {
        compareByDescending<Album> { dates[it.id] }
    } else {
        compareBy<Album> { dates[it.id] }
    }
    return sortedWith(
        compareBy<Album> { dates[it.id] == null }
            .then(chronological)
            .thenBy { it.album.title.lowercase(Locale.ROOT) }
            .thenBy { it.id },
    )
}
