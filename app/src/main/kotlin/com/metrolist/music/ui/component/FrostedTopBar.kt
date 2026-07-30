/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.component

import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp

// "Frosted glass" background for the tall Iride top bars (album/artist/playlist).
//
// The screen's scrolling content is snapshotted into [FrostBackdrop.content] ([recordFrostBackdrop]),
// which draws it exactly as-is. The bar then re-records that layer into [FrostBackdrop.blurred] — a
// second layer that carries the gaussian [BlurEffect] — and draws it clipped to its own bounds, with
// a scrim on top. Two layers, not one: a RenderNode's renderEffect applies to every draw of that
// node, so a single shared layer would blur the whole screen, not only the strip behind the bar.
//
// The blur needs RenderEffect (API 31+). Below that [rememberFrostBackdrop] returns null and the bar
// falls back to the plain translucent scrim, at a higher alpha so text stays readable.
//
// `progress` 0f = fully transparent (bar sitting over the header at the top of the screen),
// 1f = frosted (scrolled, or always-on for the playlist bars).

// Scrim alpha is the contrast floor for the bar's own icons/title (onBackground) when bright content
// — album art, a light thumbnail — scrolls under it, so it stays high enough to keep them readable.
private const val FROST_SCRIM_ALPHA = 0.45f
private const val FROST_SCRIM_ALPHA_NO_BLUR = 0.58f
private val FROST_BLUR_RADIUS = 20.dp

class FrostBackdrop internal constructor(
    internal val content: GraphicsLayer,
    internal val blurred: GraphicsLayer,
)

/** Layers backing the frosted bar. Null when the device has no RenderEffect blur (API < 31). */
@Composable
fun rememberFrostBackdrop(): FrostBackdrop? {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return null
    val content = rememberGraphicsLayer()
    val blurred = rememberGraphicsLayer()
    val radiusPx = with(LocalDensity.current) { FROST_BLUR_RADIUS.toPx() }
    blurred.renderEffect = remember(radiusPx) { BlurEffect(radiusPx, radiusPx, TileMode.Clamp) }
    return FrostBackdrop(content, blurred)
}

/**
 * Put on the screen's scrolling content (must be a sibling drawn *before* the top bar, sharing its
 * top-left origin, so the blurred copy lands pixel-aligned under the bar). Draws the content
 * unchanged — the layer is only a snapshot for the bar to sample.
 */
fun Modifier.recordFrostBackdrop(backdrop: FrostBackdrop?): Modifier {
    val layer = backdrop?.content ?: return this
    return drawWithContent {
        layer.record { this@drawWithContent.drawContent() }
        drawLayer(layer)
    }
}

fun Modifier.frostedTopBarBackground(
    progress: Float,
    barColor: Color,
    strokeColor: Color,
    backdrop: FrostBackdrop? = null,
): Modifier = drawBehind {
    val p = progress.coerceIn(0f, 1f)
    if (p <= 0f) return@drawBehind
    val contentSize = backdrop?.content?.size
    if (backdrop != null && contentSize != null && contentSize.width > 0 && contentSize.height > 0) {
        val blurred = backdrop.blurred
        // Opaque base first: the screen already painted itself sharply under this bar, and the
        // blurred copy is transparent wherever the content was, so without this the sharp original
        // reads straight through the "glass" and the blur looks like it never happened.
        drawRect(barColor, alpha = p)
        // Record at the content's full size so the blur can pull in pixels from just below the bar,
        // then clip to the bar — otherwise the bottom edge samples nothing and washes out.
        blurred.alpha = p
        blurred.record(size = contentSize) { drawLayer(backdrop.content) }
        clipRect { drawLayer(blurred) }
    } else {
        drawRect(barColor, alpha = p)
    }
    drawRect(barColor, alpha = p * if (backdrop != null) FROST_SCRIM_ALPHA else FROST_SCRIM_ALPHA_NO_BLUR)
    val sw = 1.dp.toPx()
    val y = size.height - sw / 2f
    drawLine(
        color = strokeColor,
        start = Offset(0f, y),
        end = Offset(size.width, y),
        strokeWidth = sw,
        alpha = p,
    )
}
