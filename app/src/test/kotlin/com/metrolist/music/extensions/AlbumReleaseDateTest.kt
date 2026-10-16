/** Metrolist Project (C) 2026 Licensed under GPL-3.0 | See git history for contributors */
package com.metrolist.music.extensions

import com.metrolist.music.db.entities.Album
import com.metrolist.music.db.entities.AlbumEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AlbumReleaseDateTest {
    private fun album(id: String, year: Int? = null, date: String? = null) = Album(
        AlbumEntity(id = id, title = id, year = year, releaseDate = date, songCount = 1, duration = 60),
    )

    @Test
    fun ordersByYearThenMonthThenDayInBothDirections() {
        val albums = listOf(
            album("march", 2026, "2026-03-30"),
            album("october2", 2026, "2026-10-02"),
            album("previousYear", 2025, "2025-12-31"),
            album("october14", 2026, "2026-10-14"),
        )
        assertEquals(listOf("october14", "october2", "march", "previousYear"), albums.sortedByReleaseDate(true).map { it.id })
        assertEquals(listOf("previousYear", "march", "october2", "october14"), albums.sortedByReleaseDate(false).map { it.id })
    }

    @Test
    fun usesAvailablePrecisionAndKeepsUnknownDatesLast() {
        val albums = listOf(
            album("unknown"), album("yearOnly", 2026), album("invalid", 2025, "2025-02-30"),
            album("monthOnly", 2026, "2026-08"), album("fullDate", date = "2026-08-02"),
        )
        assertEquals(listOf("fullDate", "monthOnly", "yearOnly", "invalid", "unknown"), albums.sortedByReleaseDate(true).map { it.id })
        assertEquals(listOf("invalid", "yearOnly", "monthOnly", "fullDate", "unknown"), albums.sortedByReleaseDate(false).map { it.id })
        assertNull(parseAlbumReleaseDate("2026-02-30"))
        assertNull(parseAlbumReleaseDate("2026-13"))
        assertNull(parseAlbumReleaseDate(""))
    }

    @Test
    fun equalDatesHaveStableOrderRegardlessOfInputOrder() {
        val albums = listOf(album("b", date = "2026-10-14"), album("a", date = "2026-10-14"))
        for (descending in listOf(true, false)) {
            assertEquals(listOf("a", "b"), albums.sortedByReleaseDate(descending).map { it.id })
            assertEquals(albums.sortedByReleaseDate(descending), albums.reversed().sortedByReleaseDate(descending))
        }
    }
}
