package com.metrolist.music.discovery

import java.time.DayOfWeek
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.temporal.TemporalAdjusters

internal object NewReleaseSchedule {
    private const val HOUR = 3_600_000L

    fun weekStart(today: LocalDate): LocalDate =
        today.with(TemporalAdjusters.previousOrSame(DayOfWeek.FRIDAY))

    fun needsRefresh(
        checkedAt: Long,
        ttl: Long,
        now: Long,
        zone: ZoneId = ZoneId.systemDefault(),
    ): Boolean {
        if (checkedAt <= 0 || now < checkedAt) return true
        val today = Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
        val checkedDate = Instant.ofEpochMilli(checkedAt).atZone(zone).toLocalDate()
        val interval = if (today.dayOfWeek == DayOfWeek.FRIDAY) minOf(ttl, HOUR) else ttl
        return checkedDate < weekStart(today) || now - checkedAt >= interval
    }

    fun nextCheckDelayMillis(now: ZonedDateTime = ZonedDateTime.now()): Long =
        minOf(
            15 * 60_000L,
            Duration.between(now, now.toLocalDate().plusDays(1).atStartOfDay(now.zone)).toMillis(),
        ).coerceAtLeast(1L)
}
