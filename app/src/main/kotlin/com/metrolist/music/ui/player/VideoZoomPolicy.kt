package com.metrolist.music.ui.player

internal object VideoZoomPolicy {
    const val EnlargedScale = 1.25f

    fun isAllowed(aspect: Float, rotation: Int): Boolean =
        aspect.isFinite() && aspect > 1f && kotlin.math.abs(rotation % 180) == 90

    fun afterPinch(enlarged: Boolean, factor: Float, allowed: Boolean): Boolean = when {
        !allowed -> false
        !factor.isFinite() || factor <= 0f -> enlarged
        factor > 1.08f -> true
        factor < 0.92f -> false
        else -> enlarged
    }
}
