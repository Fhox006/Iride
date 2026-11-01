package com.metrolist.music.discovery

import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.yield
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MoodLoadRetryTest {
    @Test
    fun transientFailuresRecoverWithoutUserAction() = runBlocking {
        var attempts = 0
        val pauses = mutableListOf<Long>()
        val value = MoodLoadRetry.load(pause = { pauses += it }) {
            if (++attempts < 3) throw IOException("Offline")
            "loaded"
        }
        assertEquals("loaded", value)
        assertEquals(listOf(1_500L, 4_000L), pauses)
    }

    @Test
    fun failedRequestsStopAfterFourAttempts() = runBlocking {
        var attempts = 0
        val error = runCatching {
            MoodLoadRetry.load(pause = {}) {
                attempts++
                throw IOException("Offline")
            }
        }.exceptionOrNull()
        assertEquals(4, attempts)
        assertTrue(error is IOException)
    }

    @Test
    fun requestTimeoutRetriesWhileParentIsActive() = runBlocking {
        var attempts = 0
        val value = MoodLoadRetry.load(pause = {}) {
            if (++attempts == 1) withTimeout(1) { delay(100) }
            "loaded"
        }
        assertEquals("loaded", value)
        assertEquals(2, attempts)
    }

    @Test
    fun changingFilterCancelsPendingRequestWithoutRetry() = runBlocking {
        var attempts = 0
        val job = launch {
            MoodLoadRetry.load(pause = {}) {
                attempts++
                awaitCancellation()
            }
        }
        yield()
        job.cancelAndJoin()
        assertEquals(1, attempts)
    }

    @Test
    fun explicitCancellationNeverRetries() = runBlocking {
        var attempts = 0
        val error = runCatching {
            MoodLoadRetry.load(pause = {}) {
                attempts++
                throw CancellationException("Cancelled")
            }
        }.exceptionOrNull()
        assertTrue(error is CancellationException)
        assertEquals(1, attempts)
    }
}
