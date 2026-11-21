package com.metrolist.innertube.utils

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AlbumReleaseDateTest {
    @Test fun parsesOnlyValidCalendarDates() {
        assertEquals(LocalDate.of(2026, 7, 1), parseAlbumReleaseDate("2026-07-01"))
        assertEquals(LocalDate.of(2026, 7, 1), parseAlbumReleaseDate("2026-07"))
        assertEquals(LocalDate.of(2026, 1, 1), parseAlbumReleaseDate("2026"))
        assertEquals(LocalDate.of(2024, 2, 29), parseAlbumReleaseDate("2024-02-29"))
        for (date in listOf(null, "", "0000", "2026-02-29", "2026-02-30", "2026-13", "01/07/2026")) {
            assertNull(parseAlbumReleaseDate(date))
        }
    }

    @Test fun fallsBackToYearWithoutInventingAnotherYear() {
        assertEquals(LocalDate.of(2026, 1, 1), albumReleaseDate(null, 2026))
        assertEquals(LocalDate.of(2026, 1, 1), albumReleaseDate("invalid", 2026))
        assertNull(albumReleaseDate(null, 0))
        assertNull(albumReleaseDate(null, -1))
        assertNull(albumReleaseDate(null, 10000))
    }

    @Test fun acceptsMorePreciseDatesConsistentWithKnownMonth() {
        assertEquals("2026-07-10", morePreciseAlbumReleaseDate(null, "2026-07-10", 2026))
        assertEquals("2026-07-10", morePreciseAlbumReleaseDate("2026", "2026-07-10", 2026))
        assertEquals("2026-07-10", morePreciseAlbumReleaseDate("2026-07", "2026-07-10", 2026))
        assertNull(morePreciseAlbumReleaseDate("2026-06", "2026-07-10", 2026))
    }

    @Test fun rejectsAnotherYearEditionAndInvalidDates() {
        assertNull(morePreciseAlbumReleaseDate(null, "2025-07-01", 2026))
        assertNull(morePreciseAlbumReleaseDate("2026", "2025-07-01", null))
        assertNull(morePreciseAlbumReleaseDate(null, "2026-02-30", 2026))
        assertNull(morePreciseAlbumReleaseDate(null, null, 2026))
    }

    @Test fun neverDowngradesOrReplacesAnAlreadyCompleteDate() {
        assertNull(morePreciseAlbumReleaseDate("2026-07-10", "2026", 2026))
        assertNull(morePreciseAlbumReleaseDate("2026-07-10", "2026-07", 2026))
        assertNull(morePreciseAlbumReleaseDate("2026-07-10", "2026-07-11", 2026))
    }

    @Test fun repairsInvalidOrMismatchedExternalDates() {
        assertEquals("2026-07-10", morePreciseAlbumReleaseDate("invalid", "2026-07-10", null))
        assertEquals("2026-07-10", morePreciseAlbumReleaseDate("2026-02-30", "2026-07-10", 2026))
        assertEquals("2026-07-10", morePreciseAlbumReleaseDate("2025-07-10", "2026-07-10", 2026))
    }

    @Test fun enrichesOnlyMissingInvalidPartialOrConflictingDates() {
        assertFalse(needsPreciseAlbumReleaseDate("2026-07-10", 2026))
        assertFalse(needsPreciseAlbumReleaseDate("2026-07-10", null))
        assertTrue(needsPreciseAlbumReleaseDate(null, 2026))
        assertTrue(needsPreciseAlbumReleaseDate("2026", 2026))
        assertTrue(needsPreciseAlbumReleaseDate("2026-07", 2026))
        assertTrue(needsPreciseAlbumReleaseDate("2026-02-30", 2026))
        assertTrue(needsPreciseAlbumReleaseDate("2025-07-10", 2026))
    }
}
