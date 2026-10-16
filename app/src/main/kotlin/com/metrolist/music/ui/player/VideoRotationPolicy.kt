package com.metrolist.music.ui.player

/**
 * Sensor rotation affects the video surface only; intermediate angles retain the last quarter turn.
 */
internal class VideoRotationPolicy {
    private var rotation = 0

    fun onOrientation(angle: Int): Int {
        val target =
            when (angle) {
                in 0..25,
                in 335..359 -> 0
                in 60..120 -> 270
                in 155..205 -> 180
                in 240..300 -> 90
                else -> return rotation
            }
        val delta = ((target - rotation) % 360 + 540) % 360 - 180
        rotation += delta
        return rotation
    }
}
