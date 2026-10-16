package com.metrolist.music.models

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneOffset

class MonthlyReverieTest {
    private val september = YearMonth.of(2026, 9)
    private val october = YearMonth.of(2026, 10)

    @Test
    fun installationMidMonthExcludesOnlyTheTruncatedMonth() {
        val november = YearMonth.of(2026, 11)
        val december = november.plusMonths(1)
        assertEquals(december, firstFullReverieMonth(LocalDate.of(2026, 11, 10)))
        assertEquals(emptyList<YearMonth>(), coveredReverieMonths(
            listOf(november), LocalDate.of(2026, 12, 1), LocalDate.of(2026, 11, 10),
        ))
        assertEquals(listOf(december), coveredReverieMonths(
            listOf(november, december), LocalDate.of(2027, 1, 1), LocalDate.of(2026, 11, 10),
        ))
    }

    @Test
    fun firstDayInstallationKeepsThatMonthAfterItEnds() {
        assertEquals(september, firstFullReverieMonth(LocalDate.of(2026, 9, 1)))
        assertEquals(listOf(september), coveredReverieMonths(
            listOf(september, october), LocalDate.of(2026, 10, 15), LocalDate.of(2026, 9, 1),
        ))
    }

    @Test
    fun sparseUseDoesNotExcludeMonthsAfterTrackingWasEstablished() {
        assertEquals(listOf(october, september), coveredReverieMonths(
            listOf(september, october), LocalDate.of(2026, 11, 1), LocalDate.of(2026, 6, 10),
        ))
    }

    @Test
    fun leapYearAndYearBoundaryKeepCurrentMonthLocked() {
        val february = YearMonth.of(2028, 2)
        assertEquals(emptyList<YearMonth>(), coveredReverieMonths(
            listOf(february), LocalDate.of(2028, 2, 29), LocalDate.of(2027, 12, 31),
        ))
        assertEquals(listOf(february), coveredReverieMonths(
            listOf(february), LocalDate.of(2028, 3, 1), LocalDate.of(2027, 12, 31),
        ))
    }

    @Test
    fun archiveExcludesCurrentAndFutureMonthsAndKeepsNewestFirst() {
        assertEquals(
            listOf(september, september.minusMonths(1)),
            completedReverieMonths(
                listOf(october.plusMonths(1), september.minusMonths(1), september, october, september),
                LocalDate.of(2026, 10, 1),
            ),
        )
    }

    @Test
    fun homePromotesPreviousMonthDuringFirstSevenDaysOnly() {
        for (day in 1..7) {
            assertEquals(september, homeReverieMonth(listOf(september), LocalDate.of(2026, 10, day), emptySet(), emptySet()))
        }
        assertNull(homeReverieMonth(listOf(september), LocalDate.of(2026, 10, 8), emptySet(), emptySet()))
    }

    @Test
    fun seenCardStaysInCurrentSessionButDoesNotReturnAfterClosing() {
        val seen = setOf(september.toString())
        val today = LocalDate.of(2026, 10, 1)
        assertEquals(september, homeReverieMonth(listOf(september), today, seen, seen))
        assertNull(homeReverieMonth(listOf(september), today, seen, emptySet()))
    }

    @Test
    fun absentPreviousMonthDoesNotPromoteOlderHistory() {
        assertNull(homeReverieMonth(listOf(september.minusMonths(1)), LocalDate.of(2026, 10, 1), emptySet(), emptySet()))
        assertNull(homeReverieMonth(emptyList(), LocalDate.of(2026, 10, 1), emptySet(), emptySet()))
    }

    @Test
    fun januaryPromotesDecemberRegardlessOfLastYearsSeenMonths() {
        val december = YearMonth.of(2026, 12)
        assertEquals(
            december,
            homeReverieMonth(listOf(december), LocalDate.of(2027, 1, 1), setOf(september.toString()), emptySet()),
        )
    }

    @Test
    fun calendarBoundsIncludeMidnightAndExcludeNextMonthForRankingQueries() {
        val month = YearMonth.of(2028, 2)
        val bounds = month.listeningBounds()
        val midnight = month.atDay(1).atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli()
        val lastMillisecond = month.plusMonths(1).atDay(1).atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli() - 1
        assertEquals(midnight - 1, bounds.first)
        assertEquals(lastMillisecond, bounds.last)
        assertTrue(midnight > bounds.first && midnight <= bounds.last)
        assertTrue(lastMillisecond > bounds.first && lastMillisecond <= bounds.last)
        assertEquals(29L * 24 * 60 * 60 * 1000, bounds.last - bounds.first)
    }
}
