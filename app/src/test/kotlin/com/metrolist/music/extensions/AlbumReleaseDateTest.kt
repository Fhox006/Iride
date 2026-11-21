/** Metrolist Project (C) 2026 Licensed under GPL-3.0 | See git history for contributors */
package com.metrolist.music.extensions

import com.metrolist.music.db.entities.Album
import com.metrolist.music.db.entities.AlbumEntity
import com.metrolist.music.db.entities.toAlbumEntity
import com.metrolist.innertube.models.AlbumItem
import java.time.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AlbumReleaseDateTest {
    private fun album(id: String, year: Int? = null, date: String? = null, title: String = id) = Album(
        AlbumEntity(id = id, title = title, year = year, releaseDate = date, songCount = 1, duration = 60),
    )

    @Test
    fun julyComesBeforeJuneForNewestFirstRegardlessOfTitle() {
        val albums = listOf(
            album("june", 2026, "2026-06-30", "A June album"),
            album("july", 2026, "2026-07-01", "Z July album"),
        )
        assertEquals(listOf("july", "june"), albums.sortedByReleaseDate(true).map { it.id })
        assertEquals(listOf("june", "july"), albums.sortedByReleaseDate(false).map { it.id })
    }

    @Test
    fun ordersByYearThenNumericMonthThenDay() {
        val albums = listOf(
            album("october2", 2026, "2026-10-02"),
            album("previousYear", 2025, "2025-12-31"),
            album("september", 2026, "2026-09-30"),
            album("october14", 2026, "2026-10-14"),
        )
        assertEquals(listOf("october14", "october2", "september", "previousYear"), albums.sortedByReleaseDate(true).map { it.id })
        assertEquals(listOf("previousYear", "september", "october2", "october14"), albums.sortedByReleaseDate(false).map { it.id })
    }

    @Test
    fun incompleteNewYearStillComesBeforeOlderFullDates() {
        val albums = listOf(album("old", 2025, "2025-12-31"), album("new", 2026), album("unknown"))
        assertEquals(listOf("new", "old", "unknown"), albums.sortedByReleaseDate(true).map { it.id })
        assertEquals(listOf("old", "new", "unknown"), albums.sortedByReleaseDate(false).map { it.id })
    }

    @Test
    fun albumsWithoutReleaseDatesUseMostRecentLibraryAdditionFirst() {
        val addedEarlier = album("addedEarlier", 2026).copy(
            album = album("addedEarlier", 2026).album.copy(
                bookmarkedAt = LocalDateTime.parse("2026-07-01T10:00:00"),
                lastUpdateTime = LocalDateTime.parse("2026-07-01T10:00:00"),
            ),
        )
        val addedLater = album("addedLater", 2026).copy(
            album = album("addedLater", 2026).album.copy(
                bookmarkedAt = LocalDateTime.parse("2026-07-02T10:00:00"),
                lastUpdateTime = LocalDateTime.parse("2026-07-02T10:00:00"),
            ),
        )
        val albums = listOf(addedEarlier, addedLater)
        assertEquals(listOf("addedLater", "addedEarlier"), albums.sortedByReleaseDate(true).map { it.id })
        assertEquals(listOf("addedEarlier", "addedLater"), albums.sortedByReleaseDate(false).map { it.id })
    }

    @Test
    fun undatedAlbumWithKnownYearStaysAheadOfOlderYears() {
        val albums = listOf(
            album("old", 2025, "2025-12-31"),
            album("currentYear", 2026).copy(album = album("currentYear", 2026).album.copy(
                bookmarkedAt = LocalDateTime.parse("2026-07-02T10:00:00"),
            )),
        )
        assertEquals(listOf("currentYear", "old"), albums.sortedByReleaseDate(true).map { it.id })
    }

    @Test
    fun usesPartialDatesAndReordersWhenMissingDayIsResolved() {
        val albums = listOf(
            album("unknown"), album("yearOnly", 2026), album("invalid", 2025, "2025-02-30"),
            album("monthOnly", 2026, "2026-08"), album("fullDate", date = "2026-08-02"),
        )
        assertEquals(listOf("fullDate", "monthOnly", "yearOnly", "invalid", "unknown"), albums.sortedByReleaseDate(true).map { it.id })
        assertEquals(listOf("invalid", "yearOnly", "monthOnly", "fullDate", "unknown"), albums.sortedByReleaseDate(false).map { it.id })
        val resolved = albums.map { if (it.id == "yearOnly") it.copy(album = it.album.copy(releaseDate = "2026-11-02")) else it }
        assertEquals("yearOnly", resolved.sortedByReleaseDate(true).first().id)
        assertNull(parseAlbumReleaseDate("2026-02-30"))
        assertNull(parseAlbumReleaseDate("2026-13"))
    }

    @Test
    fun sortUsesTheDisplayedDateEvenIfYearMetadataIsStale() {
        val albums = listOf(album("new", 2024, "2026-07-01"), album("old", 2026, "2026-06-30"))
        assertEquals(listOf("new", "old"), albums.sortedByReleaseDate(true).map { it.id })
    }

    @Test
    fun unknownDatesStayLastInBothDirectionsAndTiesAreDeterministic() {
        val albums = listOf(
            album("b", 2026, "2026-07-01", "Same"), album("invalid", 0, "garbage"),
            album("a", 2026, "2026-07-01", "same"), album("unknown"),
        )
        for (descending in listOf(true, false)) {
            val sorted = albums.sortedByReleaseDate(descending)
            assertEquals(listOf("a", "b", "invalid", "unknown"), sorted.map { it.id })
            assertEquals(sorted, albums.reversed().sortedByReleaseDate(descending))
        }
        assertEquals(emptyList<Album>(), emptyList<Album>().sortedByReleaseDate(true))
    }

    @Test
    fun previewLimitIsAppliedAfterChronologicalSorting() {
        val albums = (1..20).map { album("old$it", 2025, "2025-12-31") } + album("new", 2026, "2026-07-01")
        assertEquals("new", albums.sortedByReleaseDate(true).take(6).first().id)
    }

    @Test
    fun preservesFullDateWhenConvertingCatalogAlbumToLocalAlbum() {
        val item = AlbumItem(
            browseId = "album", playlistId = "", title = "Album", artists = null,
            year = 2026, thumbnail = "", releaseDate = "2026-07-10",
        )
        assertEquals("2026-07-10", item.toAlbumEntity().album.releaseDate)
    }
}
