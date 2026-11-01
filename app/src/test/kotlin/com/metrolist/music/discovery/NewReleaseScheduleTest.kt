package com.metrolist.music.discovery

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

class NewReleaseScheduleTest {
    private val rome = ZoneId.of("Europe/Rome")
    private val hour = 3_600_000L

    private fun instant(value: String): Long = ZonedDateTime.parse(value).toInstant().toEpochMilli()

    @Test fun releaseWeekStartsOnFridayAndLastsThroughThursday() {
        val friday = LocalDate.parse("2026-10-09")
        (0L..6L).forEach { day ->
            assertEquals(friday, NewReleaseSchedule.weekStart(friday.plusDays(day)))
        }
        assertEquals(friday.plusWeeks(1), NewReleaseSchedule.weekStart(friday.plusWeeks(1)))
        assertEquals(LocalDate.parse("2025-12-26"), NewReleaseSchedule.weekStart(LocalDate.parse("2026-01-01")))
    }

    @Test fun localFridayInvalidatesThursdayCacheBeforeItsTtlExpires() {
        val checked = instant("2026-10-15T23:59:00+02:00[Europe/Rome]")
        val now = instant("2026-10-16T00:00:00+02:00[Europe/Rome]")
        assertTrue(NewReleaseSchedule.needsRefresh(checked, 24 * hour, now, rome))
        assertFalse(NewReleaseSchedule.needsRefresh(checked, 24 * hour, now, ZoneId.of("UTC")))
    }

    @Test fun fridayChecksHourlyWhileOtherDaysKeepTheirTtl() {
        val friday = instant("2026-10-16T09:00:00+02:00[Europe/Rome]")
        assertFalse(NewReleaseSchedule.needsRefresh(friday, 24 * hour, friday + hour - 1, rome))
        assertTrue(NewReleaseSchedule.needsRefresh(friday, 24 * hour, friday + hour, rome))
        assertTrue(NewReleaseSchedule.needsRefresh(friday, hour / 2, friday + hour / 2, rome))
        val saturday = friday + 24 * hour
        assertFalse(NewReleaseSchedule.needsRefresh(saturday, 24 * hour, saturday + hour, rome))
        assertTrue(NewReleaseSchedule.needsRefresh(saturday, 24 * hour, saturday + 24 * hour, rome))
        assertTrue(NewReleaseSchedule.needsRefresh(0, 24 * hour, friday, rome))
        assertTrue(NewReleaseSchedule.needsRefresh(friday + 1, 24 * hour, friday, rome))
    }

    @Test fun visiblePageChecksAtLocalMidnightAndAcrossDaylightSavingChanges() {
        assertEquals(60_000L, NewReleaseSchedule.nextCheckDelayMillis(
            ZonedDateTime.parse("2026-10-15T23:59:00+02:00[Europe/Rome]")))
        assertEquals(15 * 60_000L, NewReleaseSchedule.nextCheckDelayMillis(
            ZonedDateTime.parse("2026-10-25T02:55:00+02:00[Europe/Rome]")))
        assertEquals(1_000L, NewReleaseSchedule.nextCheckDelayMillis(
            ZonedDateTime.parse("2026-10-25T23:59:59+01:00[Europe/Rome]")))
    }
}
