package com.metrolist.music.discovery

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive

internal object MoodLoadRetry {
    suspend fun <T> load(
        pause: suspend (Long) -> Unit = { delay(it) },
        request: suspend () -> T,
    ): T {
        val delays = longArrayOf(0, 1_500, 4_000, 10_000)
        for (attempt in delays.indices) {
            currentCoroutineContext().ensureActive()
            if (delays[attempt] > 0) pause(delays[attempt])
            try {
                return request()
            } catch (error: Exception) {
                currentCoroutineContext().ensureActive()
                if (error is CancellationException && error !is TimeoutCancellationException) throw error
                if (attempt == delays.lastIndex) throw error
            }
        }
        error("No mood load attempts")
    }
}
