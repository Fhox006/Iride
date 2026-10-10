package com.metrolist.music.ui.component

import android.os.SystemClock

/** Resume on a lyric change only after the finger has had time to leave the page. */
internal class LyricsScrollPause(private val clock: () -> Long = SystemClock::elapsedRealtime) {
    private var touching = false
    private var releasedAt = 0L

    fun touchStarted() {
        touching = true
    }

    fun touchReleased() {
        touching = false
        manualScroll()
    }

    fun manualScroll() {
        releasedAt = clock()
    }

    fun canResume(): Boolean =
        !touching && clock() - releasedAt >= 1_500L
}
