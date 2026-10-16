package com.metrolist.music.playback

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ManualDissolveGateTest {
    @Test
    fun repeatedSelectionsPlayDirectlyUntilTheBurstEnds() {
        val gate = ManualDissolveGate()
        assertTrue(gate.shouldFade(10_000, isPlaying = true, isTransitioning = false))
        assertFalse(gate.shouldFade(10_300, isPlaying = true, isTransitioning = true))
        assertFalse(gate.shouldFade(10_900, isPlaying = true, isTransitioning = false))
        assertFalse(gate.shouldFade(11_500, isPlaying = true, isTransitioning = false))
        assertTrue(gate.shouldFade(12_500, isPlaying = true, isTransitioning = false))
    }

    @Test
    fun bufferingTransitionsCannotBeStackedEvenAfterALongWait() {
        val gate = ManualDissolveGate()
        assertTrue(gate.shouldFade(1000, isPlaying = true, isTransitioning = false))
        assertFalse(gate.shouldFade(9000, isPlaying = false, isTransitioning = true))
        assertFalse(gate.shouldFade(19_000, isPlaying = true, isTransitioning = true))
    }

    @Test
    fun pausedOrBufferingPlaybackDoesNotStartAFade() {
        val gate = ManualDissolveGate()
        assertFalse(gate.shouldFade(0, isPlaying = false, isTransitioning = false))
        assertFalse(gate.shouldFade(5000, isPlaying = false, isTransitioning = false))
        assertTrue(gate.shouldFade(10_000, isPlaying = true, isTransitioning = false))
    }
}
