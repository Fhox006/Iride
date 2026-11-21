package com.metrolist.innertube.utils

import java.time.LocalDate
import java.time.Year
import java.time.YearMonth

fun parseAlbumReleaseDate(value: String?): LocalDate? = runCatching {
    when (value?.length) {
        10 -> LocalDate.parse(value)
        7 -> YearMonth.parse(value).atDay(1)
        4 -> Year.parse(value).atDay(1)
        else -> null
    }?.takeIf { it.year in 1..9999 }
}.getOrNull()

fun albumReleaseDate(releaseDate: String?, year: Int?): LocalDate? =
    parseAlbumReleaseDate(releaseDate) ?: year?.takeIf { it in 1..9999 }?.let { Year.of(it).atDay(1) }

fun needsPreciseAlbumReleaseDate(current: String?, year: Int?): Boolean {
    val date = parseAlbumReleaseDate(current) ?: return true
    return current?.length != 10 || (year != null && year in 1..9999 && date.year != year)
}

/** Keep known date components and never replace an album's date with another year's edition. */
fun morePreciseAlbumReleaseDate(current: String?, candidate: String?, year: Int?): String? {
    val proposed = candidate ?: return null
    val date = parseAlbumReleaseDate(proposed) ?: return null
    val expectedYear = year?.takeIf { it in 1..9999 } ?: parseAlbumReleaseDate(current)?.year
    if (expectedYear != null && date.year != expectedYear) return null
    val previous = current?.takeIf { parseAlbumReleaseDate(it)?.year?.let { it == expectedYear } == true }
    if (previous != null && (proposed.length <= previous.length || !proposed.startsWith(previous))) return null
    return proposed
}
