/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.component

import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.CompositingStrategy
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionOnScreen
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.invalidateDraw
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import kotlin.math.roundToInt


private const val FROST_SCRIM_ALPHA = 0.45f
private const val FROST_SCRIM_ALPHA_NO_BLUR = 0.58f

class FrostBackdrop internal constructor(
    internal val content: GraphicsLayer,
    internal val blurred: GraphicsLayer,
    internal val background: FrostBackdrop?,
    internal val backgroundColor: Color,
    internal val blurredSampleHeightPx: Int?,
    internal val blurredScale: Float,
    internal val captureOnly: Boolean,
) {
    internal var positionOnScreen: Offset = Offset.Zero
    internal var blurredOffset: Offset = Offset.Zero
    internal var recordingRequests by mutableIntStateOf(0)
    internal var scrollRecordingVersion by mutableIntStateOf(0)
        private set
    internal val scrollConnection = object : NestedScrollConnection {
        override fun onPostScroll(
            consumed: Offset,
            available: Offset,
            source: NestedScrollSource,
        ): Offset {
            if (consumed != Offset.Zero || available != Offset.Zero) scrollRecordingVersion++
            return Offset.Zero
        }
    }
    private val recordingListeners = mutableSetOf<() -> Unit>()

    internal fun addRecordingListener(listener: () -> Unit) {
        recordingListeners.add(listener)
    }

    internal fun removeRecordingListener(listener: () -> Unit) {
        recordingListeners.remove(listener)
    }

    internal fun notifyRecorded() {
        recordingListeners.forEach { it() }
    }
}

/** Keep popup snapshots active and observe them from inside the layer that draws their blur. */
internal fun Modifier.observeFrostBackdrop(backdrop: FrostBackdrop?): Modifier =
    if (backdrop == null) this else then(FrostBackdropObserverElement(backdrop))

private data class FrostBackdropObserverElement(val backdrop: FrostBackdrop) :
    ModifierNodeElement<FrostBackdropObserverNode>() {
    override fun create() = FrostBackdropObserverNode(backdrop)

    override fun update(node: FrostBackdropObserverNode) = node.updateBackdrop(backdrop)

    override fun InspectorInfo.inspectableProperties() {
        name = "observeFrostBackdrop"
    }
}

private class FrostBackdropObserverNode(private var backdrop: FrostBackdrop) :
    Modifier.Node(), DrawModifierNode {
    private val onRecorded: () -> Unit = { invalidateDraw() }

    override fun onAttach() {
        backdrop.addRecordingListener(onRecorded)
        backdrop.recordingRequests++
    }

    override fun onDetach() {
        backdrop.removeRecordingListener(onRecorded)
        backdrop.recordingRequests--
    }

    fun updateBackdrop(newBackdrop: FrostBackdrop) {
        if (backdrop === newBackdrop) return
        if (isAttached) {
            backdrop.removeRecordingListener(onRecorded)
            backdrop.recordingRequests--
        }
        backdrop = newBackdrop
        if (isAttached) {
            backdrop.addRecordingListener(onRecorded)
            backdrop.recordingRequests++
            invalidateDraw()
        }
    }

    override fun ContentDrawScope.draw() {
        drawContent()
    }
}

/**
 * Screen-wide frost snapshot recorded once around the tab surface (gradient included), so every
 * main tab's top bar can blur exactly what sits behind it — same result as screens that own their
 * gradient (Album/Artist), without each screen recording its own copy.
 */
val LocalScreenFrostBackdrop = androidx.compose.runtime.staticCompositionLocalOf<FrostBackdrop?> { null }
// Only the background, never the navigation/controls that sample the page's snapshot.
val LocalScreenFrostBackground = androidx.compose.runtime.staticCompositionLocalOf<FrostBackdrop?> { null }

/** Layers backing the frosted bar. Null when the device has no RenderEffect blur (API < 31). */
@Composable
fun rememberFrostBackdrop(
    background: FrostBackdrop? = null,
    backgroundColor: Color = Color.Transparent,
    blurredSampleHeight: Dp? = null,
    blurredScale: Float = 1f,
    rasterizeContent: Boolean = false,
    stableBlur: Boolean = false,
    captureOnly: Boolean = false,
): FrostBackdrop? {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return null
    val content = rememberGraphicsLayer()
    val blurred = rememberGraphicsLayer()
    // Shared bottom panels sample one rasterized page, rather than replaying changing lazy-item
    // display lists independently for every blur kernel and navigation capsule.
    content.compositingStrategy = if (rasterizeContent) {
        CompositingStrategy.Offscreen
    } else {
        CompositingStrategy.Auto
    }
    val radiusPx = with(LocalDensity.current) {
        (if (stableBlur) IrideFolio.NavigationFrostBlurRadius else IrideFolio.FrostedBlurRadius).toPx()
    } * blurredScale
    val sampleHeightPx = with(LocalDensity.current) { blurredSampleHeight?.roundToPx() }
    val nativeEffect = remember(radiusPx) {
        BlurEffect(radiusPx, radiusPx, TileMode.Clamp)
    }
    blurred.renderEffect = if (captureOnly) null else nativeEffect
    return remember(content, blurred, background, backgroundColor, sampleHeightPx, blurredScale, captureOnly) {
        FrostBackdrop(content, blurred, background, backgroundColor, sampleHeightPx, blurredScale, captureOnly)
    }
}

internal fun DrawScope.drawFrostedBackdrop(backdrop: FrostBackdrop, position: Offset) {
    val offset = backdrop.positionOnScreen + backdrop.blurredOffset - position
    translate(offset.x, offset.y) {
        scale(1f / backdrop.blurredScale, pivot = Offset.Zero) { drawLayer(backdrop.blurred) }
    }
}

/**
 * Put on the screen's scrolling content (must be a sibling drawn *before* the top bar, sharing its
 * top-left origin, so the blurred copy lands pixel-aligned under the bar). Draws the content
 * unchanged — the layer is only a snapshot for the bar to sample.
 *
 * [enabled] gates the recording: while false the layer is never touched, so an idle screen at the
 * top of its feed pays zero recording cost and the GPU never sees a stale layer.
 * [trackScroll] refreshes shared snapshots when descendant lazy lists move their retained layers.
 */
fun Modifier.recordFrostBackdrop(
    backdrop: FrostBackdrop?,
    enabled: Boolean = true,
    trackScroll: Boolean = false,
): Modifier {
    val layer = backdrop?.content ?: return this
    if (!enabled) return this
    return then(if (trackScroll) Modifier.nestedScroll(backdrop.scrollConnection) else Modifier)
        .onGloballyPositioned { backdrop.positionOnScreen = it.positionOnScreen() }
        .drawWithContent {
            // Lazy items move/recycle their own layers without necessarily redrawing this ancestor.
            // Read the scroll version in draw only: refresh the snapshot in the same frame as the
            // list, without recomposition, frame timers, or any additional work while stationary.
            if (trackScroll) backdrop.scrollRecordingVersion
            layer.record { this@drawWithContent.drawContent() }
            if (backdrop.captureOnly) {
                drawLayer(layer)
                backdrop.notifyRecorded()
                return@drawWithContent
            }
            // Flatten transparent text/images over the actual background before blurring. Otherwise
            // the sharp original pixels show through the transparent parts of the blurred glyphs.
            backdrop.blurred.alpha = 1f
            val sampleHeight = (backdrop.blurredSampleHeightPx ?: layer.size.height)
                .coerceIn(1, layer.size.height.coerceAtLeast(1))
            val sampleTop = layer.size.height - sampleHeight
            val sampleScale = backdrop.blurredScale
            backdrop.blurredOffset = Offset(0f, sampleTop.toFloat())
            val sampleSize = IntSize(
                (layer.size.width * sampleScale).roundToInt().coerceAtLeast(1),
                (sampleHeight * sampleScale).roundToInt().coerceAtLeast(1),
            )
            backdrop.blurred.record(size = sampleSize) {
                drawRect(backdrop.backgroundColor)
                scale(sampleScale, pivot = Offset.Zero) {
                    translate(top = -sampleTop.toFloat()) {
                        val background = backdrop.background
                        if (background != null && background.content.size.width > 0 && background.content.size.height > 0) {
                            translate(
                                background.positionOnScreen.x - backdrop.positionOnScreen.x,
                                background.positionOnScreen.y - backdrop.positionOnScreen.y,
                            ) { drawLayer(background.content) }
                        }
                        drawLayer(layer)
                    }
                }
            }
            drawLayer(layer)
            backdrop.notifyRecorded()
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

fun Modifier.alpha23FrostedTopBarBackground(
    progress: Float,
    barColor: Color,
    strokeColor: Color,
    backdrop: FrostBackdrop? = null,
): Modifier = drawBehind {
    val p = progress.coerceIn(0f, 1f)
    if (p <= 0f) return@drawBehind
    val contentSize = backdrop?.content?.size
    if (backdrop != null && contentSize != null && contentSize.width > 0 && contentSize.height > 0) {
        drawRect(barColor, alpha = p)
        val blurred = backdrop.blurred
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
