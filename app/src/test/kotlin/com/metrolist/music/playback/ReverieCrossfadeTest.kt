package com.metrolist.music.playback

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

class ReverieCrossfadeTest {
    @Test fun fadeEnvelopeStartsSilentAndReachesFullVolume() {
        assertEquals(0f, playbackFadeInGain(0f), 0f)
        assertEquals(1f, playbackFadeInGain(1f), 0f)
        assertEquals(0f, playbackFadeInGain(-1f), 0f)
        assertEquals(1f, playbackFadeInGain(2f), 0f)
    }

    @Test fun usesTheExistingIrideSkipEnvelope() {
        var previous = 0f
        for (step in 0..100) {
            val progress = step / 100f
            val incoming = playbackFadeInGain(progress)
            val outgoing = (1 - progress) * (1 - progress)
            assertEquals(1f, incoming + outgoing, .00001f)
            assertTrue(incoming >= previous)
            previous = incoming
        }
    }

    @Test fun doesNotConsumeTheFadeWhileTheIncomingDeckIsSilent() = runBlocking {
        var sounding = false
        var gain = -1f
        val fade = launch { runPlaybackDissolve(40, { sounding }) { gain = it } }
        delay(80)
        assertEquals(0f, gain, 0f)
        sounding = true
        fade.join()
        assertEquals(1f, gain, 0f)
    }
}
