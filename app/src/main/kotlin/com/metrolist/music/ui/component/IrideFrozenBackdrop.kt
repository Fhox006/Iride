package com.metrolist.music.ui.component

import android.os.Build
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.GraphicsContext
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.sqrt

/**
 * Immutable pixels and immutable blur kernels. HWUI can retain each filtered texture while
 * the flight changes only their opacity. The live page is never blurred on animation frames.
 */
internal class IrideFrozenBackdrop(
    private val image: ImageBitmap,
    private val graphicsContext: GraphicsContext,
    private val density: Density,
    private val layoutDirection: LayoutDirection,
    private val maximumRadiusPx: Float = with(density) { 3.dp.toPx() },
    private val levels: Int = 8,
    private val contentScale: ContentScale = ContentScale.FillBounds,
    private val backingColor: Color? = null,
) {
    private data class Kernel(val layer: GraphicsLayer, val scale: Float)
    private val kernels = linkedMapOf<Int, Kernel>()
    private var viewport = Size.Zero

    private fun DrawScope.drawSource() {
        clipRect(right = viewport.width, bottom = viewport.height) {
            backingColor?.let { drawRect(it, size = viewport) }
            val factor = contentScale.computeScaleFactor(Size(image.width.toFloat(), image.height.toFloat()), viewport)
            translate((viewport.width - image.width * factor.scaleX) / 2f,
                (viewport.height - image.height * factor.scaleY) / 2f) {
                scale(factor.scaleX, factor.scaleY, pivot = Offset.Zero) {
                    drawImage(image, filterQuality = FilterQuality.High)
                }
            }
        }
    }

    private fun kernel(level: Int): Kernel {
        kernels.remove(level)?.let {
            kernels[level] = it
            return it
        }
        // More kernels near sharp focus, where small radius differences are visible.
        val fraction = level.toFloat() / levels
        val screenRadius = maximumRadiusPx * fraction * fraction
        // Preserve fine text as the source returns to focus.
        val sampleScale = if (screenRadius < 1.5f) 1f else 0.5f
        val radius = screenRadius * sampleScale
        val layer = graphicsContext.createGraphicsLayer().apply {
            renderEffect = BlurEffect(radius, radius, TileMode.Clamp)
            record(density, layoutDirection, IntSize(
                ceil(viewport.width * sampleScale).toInt().coerceAtLeast(1),
                ceil(viewport.height * sampleScale).toInt().coerceAtLeast(1),
            )) {
                scale(sampleScale, pivot = Offset.Zero) { drawSource() }
            }
        }
        // Bound GPU memory independently of the number of focus steps and screen size.
        // A flight normally reuses the same adjacent pair for several display frames.
        while (kernels.size >= 4) {
            val oldest = kernels.keys.first()
            kernels.remove(oldest)?.layer?.let(graphicsContext::releaseGraphicsLayer)
        }
        return Kernel(layer, sampleScale).also { kernels[level] = it }
    }

    fun draw(scope: DrawScope, progress: Float) = with(scope) {
        if (size.width <= 0f || size.height <= 0f) return@with
        if (viewport != size) {
            dispose()
            viewport = size
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            drawSource()
            return@with
        }
        val position = sqrt(progress.coerceIn(0f, 1f)) * levels
        val lower = floor(position).toInt()
        val upper = ceil(position).toInt()
        if (lower == 0) drawSource() else {
            val kernel = kernel(lower)
            kernel.layer.alpha = 1f
            scale(1f / kernel.scale, pivot = Offset.Zero) { drawLayer(kernel.layer) }
        }
        if (upper > lower) {
            val kernel = kernel(upper)
            val lowerRadius = lower.toFloat() * lower / (levels * levels)
            val upperRadius = upper.toFloat() * upper / (levels * levels)
            kernel.layer.alpha = ((progress - lowerRadius) / (upperRadius - lowerRadius)).coerceIn(0f, 1f)
            scale(1f / kernel.scale, pivot = Offset.Zero) { drawLayer(kernel.layer) }
        }
    }

    fun dispose() {
        kernels.values.forEach { graphicsContext.releaseGraphicsLayer(it.layer) }
        kernels.clear()
    }
}
