package com.metrolist.music.playback

internal object VideoPlaybackPolicy {
    const val DEFAULT_HEIGHT = 720

    fun lowerHeight(height: Int): Int? = when {
        height > 720 -> 720
        height > 480 -> 480
        height > 360 -> 360
        else -> null
    }

    fun retryDelay(attempt: Int): Long? = when (attempt) {
        0 -> 500L
        1 -> 1_500L
        2 -> 3_000L
        else -> null
    }
}
