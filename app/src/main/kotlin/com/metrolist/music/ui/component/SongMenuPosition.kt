package com.metrolist.music.ui.component

import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection

internal fun songMenuViewport(
    safeBounds: IntRect,
    controlsBottom: Int,
    footerTop: Int,
    margin: Int,
    horizontalMargin: Int = margin,
): IntRect {
    val left = safeBounds.left + horizontalMargin
    val top = maxOf(safeBounds.top, controlsBottom) + margin
    return IntRect(
        left = left,
        top = top,
        right = (safeBounds.right - horizontalMargin).coerceAtLeast(left),
        bottom = (minOf(safeBounds.bottom, footerTop) - margin).coerceAtLeast(top),
    )
}

internal data class SongMenuPlacement(
    val position: IntOffset,
    val height: Int,
    val constrained: Boolean,
)

/** Prefer a complete menu above, then below; otherwise scroll on the roomier side. */
internal fun songMenuPlacement(
    anchor: IntRect,
    viewport: IntRect,
    menuSize: IntSize,
    gap: Int,
    layoutDirection: LayoutDirection,
): SongMenuPlacement {
    val aboveBottom = (anchor.top - gap).coerceIn(viewport.top, viewport.bottom)
    val belowTop = (anchor.bottom + gap).coerceIn(viewport.top, viewport.bottom)
    val aboveSpace = aboveBottom - viewport.top
    val belowSpace = viewport.bottom - belowTop
    val opensAbove = when {
        menuSize.height <= aboveSpace -> true
        menuSize.height <= belowSpace -> false
        else -> aboveSpace >= belowSpace
    }
    val available = if (opensAbove) aboveSpace else belowSpace
    // On exceptionally short windows the anchor itself can occupy the entire usable area.
    val height = minOf(menuSize.height, if (available > 0) available else viewport.height)
    val maxX = (viewport.right - menuSize.width).coerceAtLeast(viewport.left)
    val maxY = (viewport.bottom - height).coerceAtLeast(viewport.top)
    val y = if (opensAbove) aboveBottom - height else belowTop
    val x = if (layoutDirection == LayoutDirection.Ltr) anchor.right - menuSize.width else anchor.left
    return SongMenuPlacement(
        position = IntOffset(x.coerceIn(viewport.left, maxX), y.coerceIn(viewport.top, maxY)),
        height = height,
        constrained = height < menuSize.height,
    )
}

internal fun songMenuPosition(
    anchor: IntRect,
    viewport: IntRect,
    menuSize: IntSize,
    gap: Int,
    layoutDirection: LayoutDirection,
): IntOffset = songMenuPlacement(anchor, viewport, menuSize, gap, layoutDirection).position
