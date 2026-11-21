package com.metrolist.music.ui.component

/** Gesture and closing flight form one reversible timeline, not two competing animations. */
internal object AlbumSleeveMotion {
    fun closeProgress(gestureProgress: Float, flightProgress: Float): Float =
        1f - (1f - gestureProgress.coerceIn(0f, 1f)) * flightProgress.coerceIn(0f, 1f)

    fun rotation(angle: Float, closeProgress: Float): Float {
        // Finish early enough for the tile handover, regardless of the exit's duration.
        val p = (closeProgress / 0.65f).coerceIn(0f, 1f)
        val eased = p * p * (3f - 2f * p)
        return angle * (1f - eased)
    }
}
