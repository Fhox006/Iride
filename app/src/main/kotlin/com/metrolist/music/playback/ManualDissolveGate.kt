package com.metrolist.music.playback

internal data class DissolveSettings(
    val automatic: Boolean,
    val manual: Boolean,
    val durationMillis: Float,
    val gapless: Boolean,
)

internal fun isRapidDissolveSelection(nowMillis: Long, playbackStartedAtMillis: Long?): Boolean =
    playbackStartedAtMillis == null || nowMillis - playbackStartedAtMillis in 0 until 2_000L
