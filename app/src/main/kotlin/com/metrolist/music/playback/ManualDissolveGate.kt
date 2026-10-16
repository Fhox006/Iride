package com.metrolist.music.playback

/** A burst of selections previews tracks directly instead of stacking audio transitions. */
internal class ManualDissolveGate {
    private var lastSelectionMillis: Long? = null

    fun shouldFade(nowMillis: Long, isPlaying: Boolean, isTransitioning: Boolean): Boolean {
        val previous = lastSelectionMillis
        lastSelectionMillis = nowMillis
        return isPlaying && !isTransitioning &&
            (previous == null || nowMillis - previous >= 1000L)
    }
}
