package com.metrolist.music.repository

import com.metrolist.music.data.remote.ChartMarket
import java.time.LocalDate
import java.util.Locale
import java.security.MessageDigest

/** Stable local bookmarks retain their chart source instead of becoming a fixed song snapshot. */
object ChartPlaylistIdentity {
    const val PREFIX = "LP_CHART_"
    private val countryCodes = Locale.getISOCountries().map { it.lowercase(Locale.ROOT) }.toSet()

    fun id(market: ChartMarket): String = PREFIX + market.code

    fun market(playlistId: String): ChartMarket? {
        if (!playlistId.startsWith(PREFIX)) return null
        val code = playlistId.removePrefix(PREFIX)
        return code.takeIf { it == "global" || it in countryCodes }?.let(::ChartMarket)
    }
}

internal fun shouldUpdateSavedChart(sourceDate: String, savedDate: LocalDate?, today: LocalDate): Boolean =
    runCatching {
        val date = LocalDate.parse(sourceDate)
        date >= today.minusDays(7) && date <= today.plusDays(1) && (savedDate == null || date >= savedDate)
    }.getOrDefault(false)

internal fun chartRankingFingerprint(date: String, orderedTrackIds: List<String>): String {
    val source = "$date\n${orderedTrackIds.joinToString("\n")}".toByteArray(Charsets.UTF_8)
    return "IRIDE_CHART_V1:" + MessageDigest.getInstance("SHA-256").digest(source)
        .joinToString("") { "%02x".format(Locale.ROOT, it.toInt() and 0xff) }
}

internal fun shouldReplaceSavedChart(
    previousFingerprint: String?,
    currentFingerprint: String,
    savedDate: LocalDate?,
    sourceDate: LocalDate,
): Boolean = (savedDate != null && sourceDate > savedDate) ||
    (previousFingerprint != null && previousFingerprint != currentFingerprint)
