package com.metrolist.music.playback.vocals

import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.yield
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class VocalProcessingBudgetTest {
    @Test fun backgroundWorkGetsAnIdleBudgetProportionalToItsCost() {
        assertEquals(1_500L, VocalProcessingBudget.backgroundDelayMs(0))
        assertEquals(8_000L, VocalProcessingBudget.backgroundDelayMs(2_000))
        assertEquals(30_000L, VocalProcessingBudget.backgroundDelayMs(Long.MAX_VALUE))
    }

    @Test fun openingKaraokeImmediatelyInterruptsTheBackgroundWait() = runBlocking {
        val foreground = MutableStateFlow(false)
        val waiting = async { VocalProcessingBudget.awaitTurn(7_500, foreground) }
        yield()
        assertFalse(waiting.isCompleted)
        foreground.value = true
        withTimeout(1_000) { waiting.await() }
    }

    @Test fun usingKaraokeRemovesTheBackgroundDownloadLimit() = runBlocking {
        val foreground = MutableStateFlow(false)
        val waiting = async { VocalProcessingBudget.awaitDownloadTurn(512 * 1024, foreground) }
        yield()
        assertFalse(waiting.isCompleted)
        foreground.value = true
        withTimeout(500) { waiting.await() }
        withTimeout(500) { VocalProcessingBudget.awaitDownloadTurn(512 * 1024, foreground) }
    }
}
