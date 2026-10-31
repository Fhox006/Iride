package com.metrolist.music.ui.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

/** The artist and square-artwork pages share the same panel, ink depth and controls. */
internal class IrideExpansionContentMotion(val source: Rect, val progress: State<Float>) {
    var frame: Rect = Rect.Zero
    var coordinates: LayoutCoordinates? = null
}

internal val LocalIrideExpansionContentMotion = staticCompositionLocalOf<IrideExpansionContentMotion?> { null }

/** Window bounds without clipping by a shelf or by the display's edge. */
internal fun LayoutCoordinates.irideUnclippedBoundsInWindow(): Rect = Rect(
    localToWindow(Offset.Zero),
    localToWindow(Offset(size.width.toFloat(), size.height.toFloat())),
)

private data class ExpansionPanelShape(val height: Float, val radius: Float) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline =
        Outline.Rounded(RoundRect(Rect(0f, 0f, size.width, height), CornerRadius(radius)))
}

@Composable
internal fun irideExpansionPanelModifier(
    source: Rect,
    sourceRadius: Float,
    progress: State<Float>,
    frame: () -> Rect,
): Modifier {
    val corners = rememberDeviceCornerInfo()
    val fallback = with(LocalDensity.current) { 24.dp.toPx() }
    val deviceRadius = maxOf(corners.bottomLeftRadiusPx, corners.bottomRightRadiusPx).takeIf { it > 0f } ?: fallback
    return Modifier.graphicsLayer {
        val frame = frame()
        if (frame.width <= 0f || frame.height <= 0f) {
            shape = RectangleShape
            clip = false
            return@graphicsLayer
        }
        val fraction = progress.value.coerceIn(0f, 1f)
        val aperture = IrideExpansionMotion.aperture(source, frame, fraction)
        val scale = (aperture.width / frame.width).coerceAtLeast(0.001f)
        transformOrigin = TransformOrigin(0f, 0f)
        scaleX = scale
        scaleY = scale
        translationX = aperture.left - frame.left
        translationY = aperture.top - frame.top
        val height = aperture.height / scale
        val radius = ((sourceRadius * (1f - fraction) + deviceRadius * fraction) / scale)
            .coerceAtMost(minOf(frame.width, height) / 2f)
        shape = ExpansionPanelShape(height, radius)
        clip = true
    }
}

internal fun irideExpansionContentModifier(motion: IrideExpansionContentMotion): Modifier =
    Modifier.onPlaced { motion.coordinates = it }.graphicsLayer {
        val p = motion.progress.value
        alpha = IrideExpansionMotion.contentAlpha(p)
        val frame = motion.frame
        if (frame.width > 0f && frame.height > 0f) {
            val aperture = IrideExpansionMotion.aperture(motion.source, frame, p)
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
