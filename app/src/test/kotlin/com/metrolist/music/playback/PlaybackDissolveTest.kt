package com.metrolist.music.playback

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Test

class PlaybackDissolveTest {
    @Test
    fun cancellingWhileLoadingCannotRestoreTheOldEnvelopeOnResume() = runBlocking {
        val started = CompletableDeferred<Unit>()
        var sounding = false
        var progress = -1f
        val dissolve = launch {
            runPlaybackDissolve(100, { sounding }) {
                progress = it
                started.complete(Unit)
            }
        }
        withTimeout(2000) { started.await() }
        assertEquals(0f, progress, 0f)
        dissolve.cancelAndJoin()
        progress = 1f
        sounding = true
        delay(50)
        assertEquals(1f, progress, 0f)
    }

    @Test
    fun cancellingAnAudibleFadeStopsAllFurtherVolumeWrites() = runBlocking {
        val audible = CompletableDeferred<Unit>()
        var writes = 0
        val dissolve = launch {
            runPlaybackDissolve(5000, { true }) {
                writes++
                if (it > 0f) audible.complete(Unit)
            }
        }
        withTimeout(2000) { audible.await() }
        dissolve.cancelAndJoin()
        val stoppedWrites = writes
        delay(50)
        assertEquals(stoppedWrites, writes)
    }
}
