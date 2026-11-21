package com.metrolist.music.playback

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ManualDissolveGateTest {
    @Test
    fun bufferingDoesNotConsumeTheRapidSelectionWindow() {
        assertTrue(isRapidDissolveSelection(10_000, playbackStartedAtMillis = null))
    }

    @Test
    fun rapidSelectionWindowStartsWhenAudioStarts() {
        assertTrue(isRapidDissolveSelection(11_999, playbackStartedAtMillis = 10_000))
        assertFalse(isRapidDissolveSelection(12_000, playbackStartedAtMillis = 10_000))
    }
}
