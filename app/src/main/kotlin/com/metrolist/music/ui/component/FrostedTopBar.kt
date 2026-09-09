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


private const val FROST_SCRIM_ALPHA = 0.45f
private const val FROST_SCRIM_ALPHA_NO_BLUR = 0.58f
private val FROST_BLUR_RADIUS = 20.dp

class FrostBackdrop internal constructor(
    internal val content: GraphicsLayer,
    internal val blurred: GraphicsLayer,
)

/**
 * Screen-wide frost snapshot recorded once around the tab surface (gradient included), so every
 * main tab's top bar can blur exactly what sits behind it — same result as screens that own their
 * gradient (Album/Artist), without each screen recording its own copy.
 */
val LocalScreenFrostBackdrop = androidx.compose.runtime.staticCompositionLocalOf<FrostBackdrop?> { null }

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
 *
 * [enabled] gates the recording: while false the layer is never touched, so an idle screen at the
 * top of its feed pays zero recording cost and the GPU never sees a stale layer.
 */
fun Modifier.recordFrostBackdrop(backdrop: FrostBackdrop?, enabled: Boolean = true): Modifier {
    val layer = backdrop?.content ?: return this
    if (!enabled) return this
    return drawWithContent {
        layer.record { this@drawWithContent.drawContent() }
        drawLayer(layer)
    }
}

/**
 * Plain scroll-reveal bar background: fades the bar color in with [progress] and draws the
 * hairline. No backdrop sampling, so whatever sits behind the screen (the shared top gradient)
 * stays visible until the bar itself covers it — used by screens that don't own their gradient.
 */
fun Modifier.scrolledTopBarBackground(
    progress: Float,
    barColor: Color,
    strokeColor: Color,
): Modifier = drawBehind {
    val p = progress.coerceIn(0f, 1f)
    if (p <= 0f) return@drawBehind
    drawRect(barColor, alpha = p)
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

fun Modifier.frostedTopBarBackground(
    progress: Float,
    barColor: Color,
    strokeColor: Color,
    backdrop: FrostBackdrop? = null,
): Modifier = drawBehind {
    val p = progress.coerceIn(0f, 1f)
    if (p <= 0f) return@drawBehind
    val contentSize = backdrop?.content?.size
    val backdropReady = backdrop != null && contentSize != null && contentSize.width > 0 && contentSize.height > 0
    if (backdropReady) {
        val blurred = backdrop!!.blurred
        blurred.alpha = p
        blurred.record(size = contentSize!!) { drawLayer(backdrop.content) }
        clipRect { drawLayer(blurred) }
        drawRect(barColor, alpha = p * FROST_SCRIM_ALPHA)
    } else {
        // Fallback when blur is unavailable (API < 31) or the layer is still warming
        // up: draw a solid scrim so the scrolled state never looks transparent.
        // On blur-capable devices this fallback is only visible for the very first
        // reveal frame until the GraphicsLayer gets a non-zero size.
        val fallbackAlpha = if (backdrop == null) FROST_SCRIM_ALPHA_NO_BLUR else FROST_SCRIM_ALPHA
        drawRect(barColor, alpha = p * fallbackAlpha)
    }
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
