package com.metrolist.music.playback.vocals

import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull

/** Limit speculative inference to a small CPU duty cycle; user interaction wakes it immediately. */
internal object VocalProcessingBudget {
    fun backgroundDelayMs(processingMs: Long): Long =
        (processingMs.coerceIn(0, 7_500) * 4).coerceIn(1_500, 30_000)

    suspend fun awaitTurn(processingMs: Long, foreground: StateFlow<Boolean>) {
        if (!foreground.value) {
            withTimeoutOrNull(backgroundDelayMs(processingMs)) { foreground.first { it } }
        }
    }

    suspend fun awaitDownloadTurn(bytes: Int, foreground: StateFlow<Boolean>) {
        if (!foreground.value && bytes > 0) {
            // At most 512 KiB/s in the background; karaoke interaction removes the limit.
            val pauseMs = (bytes * 1_000L / (512 * 1024)).coerceAtLeast(1)
            withTimeoutOrNull(pauseMs) { foreground.first { it } }
        }
    }
}
