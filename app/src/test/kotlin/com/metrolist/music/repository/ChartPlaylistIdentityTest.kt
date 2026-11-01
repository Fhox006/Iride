package com.metrolist.music.repository

import com.metrolist.music.data.remote.ChartMarket
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ChartPlaylistIdentityTest {
    @Test fun bookmarkIdPreservesCountryOrGlobalSource() {
        listOf(ChartMarket.Italy, ChartMarket.Global, ChartMarket("jp"), ChartMarket("gb")).forEach { market ->
            assertEquals(market, ChartPlaylistIdentity.market(ChartPlaylistIdentity.id(market)))
        }
        assertEquals("LP_CHART_it", ChartPlaylistIdentity.id(ChartMarket.Italy))
        assertEquals("LP_CHART_global", ChartPlaylistIdentity.id(ChartMarket.Global))
    }

    @Test fun unrelatedPlaylistsNeverBecomeChartRoutes() {
        listOf("", "LP_LIKED", "LPabc", "RDTMAabc", "LP_CHART_", "LP_CHART_IT", "LP_CHART_GLOBAL",
            "LP_CHART_zz", "LP_CHART_it/../global", "LP_CHART_global?market=it").forEach {
            assertNull(it, ChartPlaylistIdentity.market(it))
        }
    }

    @Test fun refreshAcceptsCurrentSourceWithoutRegressingSavedRanking() {
        val today = LocalDate.of(2026, 10, 7)
        assertTrue(shouldUpdateSavedChart("2026-10-05", null, today))
        assertTrue(shouldUpdateSavedChart("2026-10-05", LocalDate.of(2026, 10, 5), today))
        assertTrue(shouldUpdateSavedChart("2026-10-06", LocalDate.of(2026, 10, 5), today))
        assertFalse(shouldUpdateSavedChart("2026-10-04", LocalDate.of(2026, 10, 5), today))
    }

    @Test fun archivesAndInvalidDatesCannotReplaceLiveSavedCharts() {
        val today = LocalDate.of(2026, 10, 7)
        assertFalse(shouldUpdateSavedChart("2022-04-07", null, today))
        assertFalse(shouldUpdateSavedChart("2026-09-29", null, today))
        assertFalse(shouldUpdateSavedChart("2026-10-09", null, today))
        assertFalse(shouldUpdateSavedChart("broken", null, today))
        assertFalse(shouldUpdateSavedChart("2026-02-30", null, today))
        assertTrue(shouldUpdateSavedChart("2026-09-30", null, today))
    }

    @Test fun reopeningOrPartialResolutionRetainsPreviouslySavedSongs() {
        val date = LocalDate.of(2026, 10, 5)
        val fingerprint = chartRankingFingerprint(date.toString(), listOf("first", "second"))
        assertFalse(shouldReplaceSavedChart(fingerprint, fingerprint, date, date))
        // A bookmark created by the earlier implementation had no fingerprint; retain its rows.
        assertFalse(shouldReplaceSavedChart(null, fingerprint, date, date))
        assertTrue(shouldReplaceSavedChart(fingerprint,
            chartRankingFingerprint(date.toString(), listOf("second", "first")), date, date))
        assertTrue(shouldReplaceSavedChart(fingerprint,
            chartRankingFingerprint("2026-10-06", listOf("first", "second")), date, date.plusDays(1)))
    }
}
