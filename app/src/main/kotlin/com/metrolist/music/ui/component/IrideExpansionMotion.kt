package com.metrolist.music.ui.component

import androidx.compose.animation.core.Easing
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.lerp
import kotlin.math.exp

/** A late header measurement preserves the current position and remaining flight time. */
internal class CollectionArtworkPath(
    private val source: Rect,
    destination: Rect,
) {
    private var origin = source
    private var resolvedAt = 0f
    var destination = destination
        private set

    fun frame(progress: Float): Rect {
        if (progress >= 1f) return destination
        if (progress <= 0f) return source
        if (progress <= resolvedAt) return IrideExpansionMotion.cover(source, origin, progress / resolvedAt)
        val remaining = 1f - resolvedAt
        val fraction = if (remaining > 0f) (progress - resolvedAt) / remaining else 1f
        return IrideExpansionMotion.cover(origin, destination, fraction)
    }

    fun resolveDestination(measured: Rect, progress: Float) {
        origin = frame(progress)
        resolvedAt = progress.coerceIn(0f, 1f)
        destination = measured
    }
}

/** Shared timing and geometry for Folio artwork-to-page transitions. */
internal object IrideExpansionMotion {
    const val Duration = 500
    const val Dissolve = 300
    const val ArtworkCornerFraction = 0.05f
    const val ArtworkVisibleThreshold = 0.95f

    val Easing = Easing { fraction ->
        val decay = 7f
        val end = 1f - (1f + decay) * exp(-decay)
        (1f - (1f + decay * fraction) * exp(-decay * fraction)) / end
    }
    private const val ReturnArtworkStartProgress = 0.25f

    fun isArtworkMostlyVisible(bounds: Rect, clippedBounds: Rect): Boolean {
        val visible = bounds.intersect(clippedBounds)
        return !visible.isEmpty &&
            visible.width * visible.height >= bounds.width * bounds.height * ArtworkVisibleThreshold
    }

    fun returnArtworkAlpha(progress: Float): Float =
        1f - (progress / ReturnArtworkStartProgress).coerceIn(0f, 1f)

    fun aperture(source: Rect, destination: Rect, progress: Float): Rect =
        lerp(source, destination, progress.coerceIn(0f, 1f))

    /** Drag and flight share window coordinates; no second scale changes the trajectory. */
    fun gestureFrame(frame: Rect, shrink: Float, horizontal: Float, vertical: Float): Rect {
        val amount = shrink.coerceIn(0f, 0.35f)
        val inset = Offset(frame.width * amount / 2f, frame.height * amount / 2f)
        val travel = Offset(frame.width * horizontal * 0.65f,
            vertical.coerceIn(-frame.height * 0.18f, frame.height * 0.18f))
        return Rect(frame.topLeft + inset + travel, frame.bottomRight - inset + travel)
    }

    fun mapToPanel(rect: Rect, frame: Rect, panel: Rect): Rect {
        val scale = panel.width / frame.width
        return Rect(panel.topLeft + (rect.topLeft - frame.topLeft) * scale,
            panel.topLeft + (rect.bottomRight - frame.topLeft) * scale)
    }

    /** Apply drag to the in-flight cover while keeping the thumbnail endpoint fixed. */
    fun mapArtworkToDraggedPanel(artwork: Rect, source: Rect, frame: Rect, dragged: Rect, progress: Float): Rect {
        if (progress <= 0f) return source
        if (frame == dragged) return artwork
        val mappedSource = mapToPanel(source, frame, dragged)
        val anchor = lerp(source, mappedSource, progress)
        val mappedArtwork = mapToPanel(artwork, frame, dragged)
        return Rect(
            mappedArtwork.topLeft + (anchor.topLeft - mappedSource.topLeft),
            mappedArtwork.bottomRight + (anchor.bottomRight - mappedSource.bottomRight),
        )
    }

    /** Preserve relative roundness as the aperture changes aspect ratio. */
    fun cornerRadius(source: Rect, frame: Rect, aperture: Rect, sourceRadius: Float, deviceRadius: Float, progress: Float): Float {
        val p = progress.coerceIn(0f, 1f)
        val start = sourceRadius / minOf(source.width, source.height).coerceAtLeast(1f)
        val end = deviceRadius / minOf(frame.width, frame.height).coerceAtLeast(1f)
        return ((start + (end - start) * p) * minOf(aperture.width, aperture.height)).coerceIn(0f,
            minOf(aperture.width, aperture.height).coerceAtLeast(0f) / 2f)
    }

    fun contentAlpha(progress: Float): Float = ((progress - 0.35f) / 0.45f).coerceIn(0f, 1f)

    fun controlsAlpha(progress: Float): Float = contentAlpha(progress)

    fun artistReturnPhotoRoundness(progress: Float): Float = (1f - progress).coerceIn(0f, 1f) / 2f

    fun restorationProgress(progress: Float): Float = ((progress - 0.30f) / 0.70f).coerceIn(0f, 1f)

    fun restoredCover(initial: Rect, scrolled: Rect, progress: Float): Rect =
        aperture(initial, scrolled, restorationProgress(progress))

    /** Keep both endpoints exact, including artwork from a rectangular tile. */
    fun cover(source: Rect, destination: Rect, progress: Float): Rect {
        if (progress <= 0f) return source
        if (progress >= 1f) return destination
        val center = source.center + (destination.center - source.center) * progress
        val width = source.width + (destination.width - source.width) * progress
        val height = source.height + (destination.height - source.height) * progress
        val half = Offset(width / 2f, height / 2f)
        return Rect(center - half, center + half)
    }
}
