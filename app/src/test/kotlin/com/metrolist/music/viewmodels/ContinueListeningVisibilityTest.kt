package com.metrolist.music.viewmodels

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ContinueListeningVisibilityTest {
    @Test
    fun hidesAfterTwoIgnoredVisitsAtLeastFiveMinutesApart() {
        val firstVisit = ContinueListeningVisibilityState().recordVisit(
            selectedAlbum = false,
            nowMs = 1_000L,
        )

        assertEquals(1, firstVisit.ignoredVisits)
        assertFalse(firstVisit.hidden)

        val secondVisit = firstVisit.recordVisit(
            selectedAlbum = false,
            nowMs = 1_000L + CONTINUE_LISTENING_VISIT_INTERVAL_MS,
        )

        assertEquals(2, secondVisit.ignoredVisits)
        assertTrue(secondVisit.hidden)
    }

    @Test
    fun ignoresVisitsTooCloseTogetherAndVisitsWithAnAlbumSelection() {
        val firstVisit = ContinueListeningVisibilityState().recordVisit(
            selectedAlbum = false,
            nowMs = 1_000L,
        )

        val tooSoon = firstVisit.recordVisit(
            selectedAlbum = false,
            nowMs = 1_000L + CONTINUE_LISTENING_VISIT_INTERVAL_MS - 1,
        )
        val selectedAlbum = firstVisit.recordVisit(
            selectedAlbum = true,
            nowMs = 1_000L + CONTINUE_LISTENING_VISIT_INTERVAL_MS,
        )

        assertEquals(firstVisit, tooSoon)
        assertEquals(firstVisit, selectedAlbum)
    }
}
