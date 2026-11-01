/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.utils

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.unit.Dp

/**
 * Draws an outline stroke ON TOP of the node's content, fully INSIDE the layout
 * bounds: the line hugs the artwork edge instead of straddling it, so nothing
 * bleeds outside the image.
 *
 * A border declared through `Modifier.border()` before the artwork is painted over by
 * full-bleed images (`ContentScale.Crop` + `fillMaxSize`) and half of the stroke is cut
 * by the preceding `.clip()`. Drawing the stroke after [drawContent] keeps it fully
 * visible regardless of what the artwork renders underneath.
 *
 * Place this modifier BEFORE any `.clip()` in the chain, or apply it to a topmost
 * overlay child of the thumbnail Box. When wrapped in animated `graphicsLayer`
 * scopes the stroke follows the artwork animation.
 */
fun Modifier.irideArtworkOverlayBorder(
    width: Dp,
    color: Color,
    shape: Shape,
): Modifier = drawWithContent {
    drawContent()
    val strokePx = width.toPx()
    if (strokePx <= 0f || size.width <= strokePx || size.height <= strokePx) return@drawWithContent

    // Shrink the outline by the stroke width and offset by half of it so the whole
    // stroke sits inside the bounds instead of straddling the edge.
    val insetOutline = shape.createOutline(
        size = Size(size.width - strokePx, size.height - strokePx),
        layoutDirection = layoutDirection,
        density = this,
    )
    translate(left = strokePx / 2f, top = strokePx / 2f) {
        when (insetOutline) {
            is Outline.Generic -> drawPath(insetOutline.path, color, style = Stroke(strokePx))
            is Outline.Rounded -> drawPath(
                Path().apply { addRoundRect(insetOutline.roundRect) },
                color,
                style = Stroke(strokePx),
            )
            is Outline.Rectangle -> drawRect(color, style = Stroke(strokePx))
        }
    }
}
