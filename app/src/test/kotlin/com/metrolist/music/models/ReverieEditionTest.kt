package com.metrolist.music.models

import org.junit.Assert.*
import org.junit.Test
import java.time.YearMonth

class ReverieEditionTest {
    @Test fun eachMonthHasAStableDistinctSeasonalPrimary() {
        val editions = (1..12).map { reverieEdition(YearMonth.of(2026, it)) }
        assertEquals(12, editions.map { it.primary }.distinct().size)
        editions.forEachIndexed { index, edition ->
            assertEquals(edition, reverieEdition(YearMonth.of(2026, index + 1)))
            assertNotEquals(REVERIE_PAPER, reveriePalette(edition.primary).background)
        }
    }

    @Test fun editorialOrderVariesAcrossMonthsButAlwaysEndsWithTheSongAndPoster() {
        val available = ReverieChapter.entries.toList()
        var previous: List<ReverieChapter>? = null
        for (month in 1..12) {
            val date = YearMonth.of(2026, month)
            val order = reverieChapterOrder(date, available)
            assertEquals(order, reverieChapterOrder(date, available))
            assertEquals(available.toSet(), order.toSet())
            assertEquals(9, order.size)
            assertEquals(ReverieChapter.OVERTURE, order.first())
            assertEquals(listOf(ReverieChapter.SONG, ReverieChapter.POSTER), order.takeLast(2))
            assertNotEquals(previous, order)
            previous = order
        }
    }

    @Test fun unsupportedChaptersAreOmittedWithoutInventingMetrics() {
        val available = listOf(ReverieChapter.OVERTURE, ReverieChapter.TIME, ReverieChapter.SONG, ReverieChapter.POSTER)
        assertEquals(available, reverieChapterOrder(YearMonth.of(2026, 10), available))
    }

    @Test fun eligibilityRequiresEnoughDistinctTracksTimeAndDays() {
        assertTrue(hasEnoughReverieData(9, 1_800_000, 3))
        assertFalse(hasEnoughReverieData(8, 9_000_000, 20))
        assertFalse(hasEnoughReverieData(90, 1_799_999, 20))
        assertFalse(hasEnoughReverieData(90, 9_000_000, 2))
        assertFalse(hasEnoughReverieData(0, 0, 0))
    }
}
