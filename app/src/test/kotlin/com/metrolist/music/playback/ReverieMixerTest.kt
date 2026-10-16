package com.metrolist.music.playback

import com.metrolist.music.models.MediaMetadata
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.*
import org.junit.Test

class ReverieMixerTest {
    private class Deck : ReveriePreviewDeck {
        override var mediaId: String? = null
        override var ready = false
        override var failed = false
        var started = false
        var canPlay = true
        var preparations = 0
        override val playing get() = ready && started && canPlay && !failed
        override fun prepare(song: MediaMetadata) { mediaId = song.id; ready = true; preparations++ }
        override fun setPlaying(value: Boolean) { started = value }
    }
    private fun song(id: String) = MediaMetadata(id, id, emptyList(), 200)
    private suspend fun until(condition: () -> Boolean) = withTimeout(2_000) { while (!condition()) delay(2) }

    @Test fun pairedOpeningSharesOnePreparedDeckAndDoesNotRestartItsSong() = runBlocking {
        val decks = List(2) { Deck() }
        val mixer = ReverieMixer(decks, this) { _, _ -> }
        mixer.prime(song("pair"), song("pair"))
        assertEquals("pair", decks[1].mediaId)
        assertNull(decks[0].mediaId)
        mixer.setPlaying(true)
        assertTrue(mixer.transitionTo(song("pair"), 1))
        mixer.preload(song("pair"))
        assertTrue(mixer.transitionTo(song("pair"), 1))
        assertEquals(1, decks.sumOf { it.preparations })
        mixer.preload(song("next"))
        assertTrue(decks[1].playing)
        assertTrue(mixer.transitionTo(song("next"), 1))
    }

    @Test fun retryDoesNotReuseThePreviousFailure() = runBlocking {
        val decks = List(2) { Deck() }
        val mixer = ReverieMixer(decks, this) { _, _ -> }
        mixer.setPlaying(true)
        decks[1].failed = true
        assertFalse(mixer.transitionTo(song("first"), 1))
        decks[1].failed = false
        assertTrue(mixer.transitionTo(song("first"), 1))
        assertTrue(decks[1].playing)
    }

    @Test fun resetStopsBothDecksAndAllowsReprimingAfterFailure() = runBlocking {
        val decks = List(2) { Deck() }
        val gains = floatArrayOf(0f, 0f)
        val mixer = ReverieMixer(decks, this) { index, gain -> gains[index] = gain }
        mixer.setPlaying(true)
        assertTrue(mixer.transitionTo(song("old"), 1))
        mixer.reset()
        assertTrue(decks.none { it.started })
        assertTrue(gains.all { it == 0f })
        mixer.prime(song("first"), song("second"))
        assertEquals("first", decks[1].mediaId)
        assertEquals("second", decks[0].mediaId)
        assertTrue(mixer.transitionTo(song("first"), 1))
    }

    @Test fun anEndedTrackCannotReuseItsEarlierSuccessfulTransition() = runBlocking {
        val decks = List(2) { Deck() }
        val mixer = ReverieMixer(decks, this) { _, _ -> }
        mixer.setPlaying(true)
        assertTrue(mixer.transitionTo(song("first"), 1))
        decks[1].failed = true
        assertFalse(mixer.transitionTo(song("first"), 1))
    }

    @Test fun openingBuffersBothFirstAndSecondSongsBeforeStartingTheFilm() = runBlocking {
        val decks = List(2) { Deck() }
        val mixer = ReverieMixer(decks, this) { _, _ -> }
        mixer.prime(song("first"), song("second"))
        assertEquals("first", decks[1].mediaId)
        assertEquals("second", decks[0].mediaId)
        mixer.setPlaying(true)
        assertTrue(mixer.transitionTo(song("first"), 20))
        mixer.preload(song("second"))
        assertTrue(mixer.transitionTo(song("second"), 20))
        assertTrue(decks.all { it.preparations == 1 })
    }

    @Test fun chapterBoundaryDoesNotCancelTheOverlapOrJumpToFullVolume() = runBlocking {
        val decks = List(2) { Deck() }
        val gains = floatArrayOf(0f, 0f)
        val mixer = ReverieMixer(decks, this) { index, gain -> gains[index] = gain }
        mixer.setPlaying(true)
        assertTrue(mixer.transitionTo(song("first"), 30))
        mixer.preload(song("second"))
        val chapter = launch { mixer.transitionTo(song("second"), 220) }
        until { gains[0] > 0f && gains[0] < 1f }
        chapter.cancelAndJoin()
        assertTrue(gains[0] < 1f)
        assertTrue(decks.all { it.playing })
        assertTrue(mixer.transitionTo(song("second"), 220))
        assertEquals(1f, gains[0], 0f)
        assertEquals(0f, gains[1], 0f)
        assertEquals(1, decks[0].preparations)
    }

    @Test fun secondChapterStartsEvenIfPausedWhileItsIncomingVolumeIsStillZero() = runBlocking {
        val decks = List(2) { Deck() }
        val gains = floatArrayOf(0f, 0f)
        val mixer = ReverieMixer(decks, this) { index, gain -> gains[index] = gain }
        mixer.setPlaying(true)
        mixer.transitionTo(song("first"), 20)
        decks[0].canPlay = false
        val incoming = launch { mixer.transitionTo(song("second"), 40) }
        until { decks[0].started }
        assertEquals(0f, gains[0], 0f)
        mixer.setPlaying(false)
        assertFalse(decks[0].started)
        mixer.setPlaying(true)
        assertTrue(decks[0].started)
        decks[0].canPlay = true
        incoming.join()
        assertEquals("second", decks[0].mediaId)
        assertEquals(1f, gains[0], 0f)
        assertFalse(decks[1].started)
    }

    @Test fun manualSeekCrossfadesWithoutFirstMutingTheCurrentSong() = runBlocking {
        val decks = List(2) { Deck() }
        val gains = floatArrayOf(0f, 0f)
        val mixer = ReverieMixer(decks, this) { index, gain -> gains[index] = gain }
        mixer.setPlaying(true)
        mixer.transitionTo(song("first"), 20)
        decks[0].canPlay = false
        val jump = launch { mixer.transitionTo(song("third"), 50) }
        until { decks[0].started }
        assertEquals(1f, gains[1], 0f)
        assertTrue(decks[1].playing)
        decks[0].canPlay = true
        jump.join()
        assertEquals(1f, gains[0], 0f)
        assertEquals(0f, gains[1], 0f)
    }

    @Test fun interruptedOverlapKeepsItsActualGainsBeforeFadingToTheNewTrack() = runBlocking {
        val decks = List(2) { Deck() }
        val gains = floatArrayOf(0f, 0f)
        val mixer = ReverieMixer(decks, this) { index, gain -> gains[index] = gain }
        mixer.setPlaying(true)
        mixer.transitionTo(song("first"), 20)
        val second = launch { mixer.transitionTo(song("second"), 220) }
        until { gains[0] > 0f && gains[0] < 1f }
        val before = gains[0]
        val third = launch { mixer.transitionTo(song("third"), 80) }
        delay(5)
        assertTrue(gains[0] >= before)
        third.join(); second.join()
        assertEquals("third", decks[1].mediaId)
        assertEquals(1f, gains[1], 0f)
        assertEquals(0f, gains[0], 0f)
    }

    @Test fun overlapMaintainsConstantPowerAndContinuousEndpoints() {
        assertTrue(reverieCrossfadeGains(.02f, 0f, 1f).incoming < .1f)
        for (step in 0..100) {
            val gain = reverieCrossfadeGains(step / 100f, 0f, 1f)
            assertEquals(1f, gain.incoming * gain.incoming + gain.outgoing * gain.outgoing, .00001f)
        }
        assertEquals(ReverieGains(.4f, .8f), reverieCrossfadeGains(0f, .4f, .8f))
        assertEquals(ReverieGains(1f, 0f), reverieCrossfadeGains(1f, .4f, .8f))
    }
}
