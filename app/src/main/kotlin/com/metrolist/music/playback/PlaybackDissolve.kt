package com.metrolist.music.playback

import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlin.coroutines.coroutineContext

/** The skip-fade envelope used by both the music service and the monthly film. */
fun playbackFadeInGain(progress: Float): Float {
    val remaining = 1f - progress.coerceIn(0f, 1f)
    return 1f - remaining * remaining
}

/** Start the envelope only when the incoming deck is actually sounding, not merely buffered. */
suspend fun runPlaybackDissolve(durationMillis: Long, isPlaying: () -> Boolean, apply: (Float) -> Unit) {
    var elapsed = 0L
    var previous = System.nanoTime()
    apply(0f)
    while (elapsed < durationMillis.coerceAtLeast(1) * 1_000_000) {
        coroutineContext.ensureActive()
        delay(16)
        val now = System.nanoTime()
        if (isPlaying()) elapsed += now - previous
        previous = now
        apply((elapsed / (durationMillis.coerceAtLeast(1) * 1_000_000f)).coerceIn(0f, 1f))
    }
    apply(1f)
}

class ReveriePlaybackLease internal constructor(val volume: Float, private val restore: (Boolean) -> Unit) {
    private var released = false
    fun release(resume: Boolean) {
        if (released) return
        released = true
        restore(resume)
    }
}
