package com.metrolist.music.models

import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneOffset

const val REVERIE_MIN_TRACKS = 9
const val REVERIE_MIN_MILLIS = 30 * 60_000L
const val REVERIE_MIN_DAYS = 3

fun hasEnoughReverieData(tracks: Int, milliseconds: Long, days: Int): Boolean =
    tracks >= REVERIE_MIN_TRACKS && milliseconds >= REVERIE_MIN_MILLIS && days >= REVERIE_MIN_DAYS

data class ReverieMonthActivity(val month: String, val tracks: Int, val milliseconds: Long, val days: Int) {
    val eligible: Boolean get() = hasEnoughReverieData(tracks, milliseconds, days)
}

/** Listening timestamps are stored as local calendar values encoded with UTC (see Converters). */
fun YearMonth.listeningBounds(): LongRange {
    val start = atDay(1).atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli()
    val end = plusMonths(1).atDay(1).atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli()
    // Existing ranking queries use an exclusive lower bound and an inclusive upper bound.
    return (start - 1)..(end - 1)
}

fun completedReverieMonths(months: List<YearMonth>, today: LocalDate): List<YearMonth> =
    months.filter { it < YearMonth.from(today) }.distinct().sortedDescending()

/** Installation or restored history proves coverage; quiet days are not missing data. */
fun firstFullReverieMonth(trackingStarted: LocalDate): YearMonth =
    YearMonth.from(trackingStarted).let { if (trackingStarted.dayOfMonth == 1) it else it.plusMonths(1) }

fun coveredReverieMonths(months: List<YearMonth>, today: LocalDate, trackingStarted: LocalDate): List<YearMonth> =
    completedReverieMonths(months, today).filter { it >= firstFullReverieMonth(trackingStarted) }

fun homeReverieMonth(
    months: List<YearMonth>,
    today: LocalDate,
    seenMonths: Set<String>,
    sessionMonths: Set<String>,
): YearMonth? {
    if (today.dayOfMonth > 7) return null
    val previousMonth = YearMonth.from(today).minusMonths(1)
    return previousMonth.takeIf {
        it in months && (it.toString() !in seenMonths || it.toString() in sessionMonths)
    }
}
