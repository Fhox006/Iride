package com.metrolist.music.ui.component

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LyricsScrollPauseTest {
    private var now = 10_000L
    private val pause = LyricsScrollPause { now }

    @Test
    fun holdingTheFingerNeverResumesEvenAcrossSeveralVerses() {
        pause.touchStarted()
        pause.manualScroll()
        now += 30_000L
        assertFalse(pause.canResume())
    }

    @Test
    fun gracePeriodStartsAtReleaseRatherThanAtTheLastMovement() {
        pause.touchStarted()
        pause.manualScroll()
        now += 5_000L
        pause.touchReleased()
        now += 1_499L
        assertFalse(pause.canResume())
        now += 1L
        assertTrue(pause.canResume())
    }

    @Test
    fun touchingAgainRestartsTheFullGracePeriod() {
        pause.touchStarted()
        pause.touchReleased()
        now += 1_400L
        pause.touchStarted()
        now += 1_000L
        assertFalse(pause.canResume())
        pause.touchReleased()
        now += 1_499L
        assertFalse(pause.canResume())
        now += 1L
        assertTrue(pause.canResume())
    }

    @Test
    fun wheelAndAccessibilityScrollingUseTheSameGracePeriod() {
        pause.manualScroll()
        now += 1_000L
        pause.manualScroll()
        now += 500L
        assertFalse(pause.canResume())
        now += 1_000L
        assertTrue(pause.canResume())
    }
}
