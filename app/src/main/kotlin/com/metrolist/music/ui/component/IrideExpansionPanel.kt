package com.metrolist.music.ui.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.metrolist.music.ui.theme.FolioShape

/** The artist and square-artwork pages share the same panel, ink depth and controls. */
internal class IrideExpansionContentMotion(val source: Rect, val progress: State<Float>) {
    var frame: Rect = Rect.Zero
    var coordinates: LayoutCoordinates? = null
    var gesture: IridePanelGestureMotion? = null
    var keepContentVisible = false
}

internal val LocalIrideExpansionContentMotion = staticCompositionLocalOf<IrideExpansionContentMotion?> { null }

/** Window bounds without clipping by a shelf or by the display's edge. */
internal fun LayoutCoordinates.irideUnclippedBoundsInWindow(): Rect = Rect(
    localToWindow(Offset.Zero),
    localToWindow(Offset(size.width.toFloat(), size.height.toFloat())),
)

private data class ExpansionPanelShape(
    val width: Float,
    val height: Float,
    val radius: Float,
    val continuousCorners: Boolean,
) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val panelSize = Size(width, height)
        if (!continuousCorners || radius >= panelSize.minDimension / 2f) {
            return Outline.Rounded(RoundRect(Rect(Offset.Zero, panelSize), CornerRadius(radius)))
        }
        return FolioShape(with(density) { radius.toDp() }).createOutline(panelSize, layoutDirection, density)
    }
}

/** The photo has its own native round-rect clip; the page's pigment cannot enter its circle. */
internal data class ArtistReturnPhotoShape(val roundness: Float) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline =
        Outline.Rounded(RoundRect(Rect(Offset.Zero, size), CornerRadius(size.minDimension * roundness)))
}

@Composable
internal fun irideExpansionPanelModifier(
    source: Rect,
    sourceRadius: Float,
    progress: State<Float>,
    gesture: IridePanelGestureMotion? = null,
    continuousCorners: Boolean = true,
    frame: () -> Rect,
): Modifier {
    val corners = rememberDeviceCornerInfo()
    val fallback = with(LocalDensity.current) { 24.dp.toPx() }
    val deviceRadius = maxOf(corners.bottomLeftRadiusPx, corners.bottomRightRadiusPx).takeIf { it > 0f } ?: fallback
    return Modifier.graphicsLayer {
        val frame = frame()
        if (frame.width <= 0f || frame.height <= 0f) {
            alpha = 0f
            shape = RectangleShape
            clip = false
            return@graphicsLayer
        }
        val fraction = progress.value.coerceIn(0f, 1f)
        alpha = if (fraction > 0f) 1f else 0f
        val aperture = IrideExpansionMotion.aperture(source, gesture?.frame(frame) ?: frame, fraction)
        val scale = (aperture.width / frame.width).coerceAtLeast(0.001f)
        transformOrigin = TransformOrigin(0f, 0f)
        scaleX = scale
        scaleY = scale
        translationX = aperture.left - frame.left
        translationY = aperture.top - frame.top
        val width = aperture.width / scale
        val height = aperture.height / scale
        val radius = (IrideExpansionMotion.cornerRadius(source, frame, aperture, sourceRadius, deviceRadius, fraction) / scale)
            .coerceAtMost(minOf(width, height) / 2f)
        shape = ExpansionPanelShape(width, height, radius, continuousCorners)
        clip = true
    }
}

internal fun irideExpansionContentModifier(motion: IrideExpansionContentMotion): Modifier =
    Modifier.onPlaced { motion.coordinates = it }.graphicsLayer {
        val p = motion.progress.value
        alpha = if (motion.keepContentVisible) 1f else IrideExpansionMotion.contentAlpha(p)
        val frame = motion.frame
        if (frame.width > 0f && frame.height > 0f) {
            val aperture = IrideExpansionMotion.aperture(motion.source, motion.gesture?.frame(frame) ?: frame, p)
            val panelScale = (aperture.width / frame.width).coerceAtLeast(0.001f)
            val contentScale = minOf(panelScale, aperture.height / frame.height, 1f)
            scaleX = contentScale / panelScale
            scaleY = contentScale / panelScale
            transformOrigin = TransformOrigin(0f, 0f)
            translationX = (aperture.width - frame.width * contentScale) / (2f * panelScale)
            translationY = ((aperture.height - frame.height * contentScale) / 2f +
                frame.height * 0.35f * (1f - p)) / panelScale
        }
    }

internal fun irideExpansionControlsModifier(progress: State<Float>): Modifier = Modifier.graphicsLayer {
    alpha = IrideExpansionMotion.controlsAlpha(progress.value)
}

/** Draw inside the panel's own clip, in its unscaled coordinates. */
internal fun Modifier.irideExpansionReturnArtwork(
    source: Rect,
    progress: State<Float>,
    gesture: IridePanelGestureMotion,
    frame: () -> Rect,
    drawArtwork: DrawScope.(bounds: Rect, panelScale: Float) -> Unit,
): Modifier {
    val opacity = Paint()
    return drawBehind {
        val p = progress.value
        val alpha = IrideExpansionMotion.returnArtworkAlpha(p)
        val restingFrame = frame()
        if (alpha <= 0f || restingFrame.isEmpty) return@drawBehind
        val panel = IrideExpansionMotion.aperture(source, gesture.frame(restingFrame), p)
        val panelScale = (panel.width / restingFrame.width).coerceAtLeast(0.001f)
        val bounds = Rect(Offset.Zero, Size(panel.width / panelScale, panel.height / panelScale))
        val canvas = drawContext.canvas
        opacity.alpha = alpha
        canvas.saveLayer(bounds, opacity)
        try {
            drawArtwork(bounds, panelScale)
        } finally {
            canvas.restore()
        }
    }
}

internal fun DrawScope.drawIrideReturnArtwork(image: ImageBitmap, bounds: Rect) {
    val coverScale = maxOf(bounds.width / image.width, bounds.height / image.height)
    val x = bounds.center.x - image.width * coverScale / 2f
    val y = bounds.center.y - image.height * coverScale / 2f
    translate(x, y) { scale(coverScale, pivot = Offset.Zero) { drawImage(image) } }
}
