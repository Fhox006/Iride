package com.metrolist.music.ui.component

import androidx.compose.animation.core.Easing
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.lerp
import kotlin.math.exp

/** Shared timing and geometry for Folio artwork-to-page transitions. */
internal object IrideExpansionMotion {
    const val Duration = 500
    const val Dissolve = 300

    val Easing = Easing { fraction ->
        val decay = 7f
        val end = 1f - (1f + decay) * exp(-decay)
        (1f - (1f + decay * fraction) * exp(-decay * fraction)) / end
    }

    fun aperture(source: Rect, destination: Rect, progress: Float): Rect =
        lerp(source, destination, progress.coerceIn(0f, 1f))

    fun contentAlpha(progress: Float): Float = ((progress - 0.35f) / 0.45f).coerceIn(0f, 1f)

    fun controlsAlpha(progress: Float): Float = contentAlpha(progress)

    fun restorationProgress(progress: Float): Float = ((progress - 0.30f) / 0.70f).coerceIn(0f, 1f)

    fun restoredCover(initial: Rect, scrolled: Rect, progress: Float): Rect =
        aperture(initial, scrolled, restorationProgress(progress))

    /** A single scale preserves the cover's aspect ratio throughout the flight. */
    fun cover(source: Rect, destination: Rect, progress: Float): Rect {
        val p = progress.coerceIn(0f, 1f)
        val center = source.center + (destination.center - source.center) * p
        val start = minOf(source.width, source.height)
        val end = minOf(destination.width, destination.height)
        val half = (start + (end - start) * p) / 2f
        return Rect(center - Offset(half, half), center + Offset(half, half))
    }
}
