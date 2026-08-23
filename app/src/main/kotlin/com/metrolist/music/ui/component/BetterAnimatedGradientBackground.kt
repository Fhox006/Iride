package com.metrolist.music.ui.component

import android.graphics.Bitmap
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.metrolist.music.ui.utils.rememberResumeFadeAlpha
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

@Composable
fun BetterAnimatedGradientBackground(
    thumbnail: Bitmap?,
    modifier: Modifier = Modifier,
    incomingThumbnail: Bitmap? = null,
    crossfadeProgress: Float = 0f,
) {
    val albumImage = remember(thumbnail) { thumbnail?.asImageBitmap() }
    val incomingAlbumImage = remember(incomingThumbnail) { incomingThumbnail?.asImageBitmap() }

    val infinite = rememberInfiniteTransition(label = "better_anim_grad")

    val angle0 by infinite.animateFloat(
        initialValue = 0f,
        targetValue = (PI * 2).toFloat(),
        animationSpec = infiniteRepeatable(tween(174_533, easing = LinearEasing), RepeatMode.Reverse),
        label = "bag_a0",
    )
    val angle1 by infinite.animateFloat(
        initialValue = 0f,
        targetValue = (PI * 2).toFloat(),
        animationSpec = infiniteRepeatable(tween(65_450, easing = LinearEasing), RepeatMode.Reverse),
        label = "bag_a1",
    )
    val angle2 by infinite.animateFloat(
        initialValue = 0f,
        targetValue = (PI * 2).toFloat(),
        animationSpec = infiniteRepeatable(tween(87_268, easing = LinearEasing), RepeatMode.Reverse),
        label = "bag_a2",
    )
    val angle3 by infinite.animateFloat(
        initialValue = 0f,
        targetValue = (PI * 2).toFloat(),
        animationSpec = infiniteRepeatable(tween(130_900, easing = LinearEasing), RepeatMode.Reverse),
        label = "bag_a3",
    )

    val resumeFadeAlpha = rememberResumeFadeAlpha()

    Box(
        modifier = modifier
            .fillMaxSize()
            .graphicsLayer {
                alpha = resumeFadeAlpha
                colorFilter = ColorFilter.colorMatrix(
                    ColorMatrix().apply { setToSaturation(1.60f) }
                )
            },
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .blur(120.dp),
        ) {
            val w = size.width
            val h = size.height
            val maxDim = max(w, h)

            drawRect(Color(0xFF0D0D0D))

            val isCrossfading = incomingAlbumImage != null && crossfadeProgress > 0f
            val currentAlpha = if (isCrossfading) 1f - crossfadeProgress else 1f

            albumImage?.let { image ->
                val sz0 = (maxDim * 3.0f).toInt()
                withTransform({
                    translate(w / 2f, h / 2f)
                    rotate(Math.toDegrees(angle0.toDouble()).toFloat())
                }) {
                    drawImage(image, dstOffset = IntOffset(-sz0 / 2, -sz0 / 2), dstSize = IntSize(sz0, sz0), filterQuality = FilterQuality.Low, alpha = currentAlpha)
                }

                val sz1 = (maxDim * 2.5f).toInt()
                withTransform({
                    translate(w / 2f, h / 2f)
                    rotate(-Math.toDegrees(angle1.toDouble()).toFloat())
                }) {
                    drawImage(image, dstOffset = IntOffset(-sz1 / 2, -sz1 / 2), dstSize = IntSize(sz1, sz1), filterQuality = FilterQuality.Low, alpha = currentAlpha)
                }

                val oa2 = -(angle2 * 0.75f).toDouble()
                val cx2 = w / 2f + (maxDim * 0.18f) * cos(oa2).toFloat()
                val cy2 = h / 2f + (maxDim * 0.18f) * sin(oa2).toFloat()
                val sz2 = (maxDim * 2.2f).toInt()
                withTransform({
                    translate(cx2, cy2)
                    rotate(-Math.toDegrees(angle2.toDouble()).toFloat())
                }) {
                    drawImage(image, dstOffset = IntOffset(-sz2 / 2, -sz2 / 2), dstSize = IntSize(sz2, sz2), filterQuality = FilterQuality.Low, alpha = currentAlpha)
                }

                val oa3 = (angle3 * 0.5f).toDouble()
                val cx3 = w / 2f + (maxDim * 0.22f) * cos(oa3).toFloat()
                val cy3 = h / 2f + (maxDim * 0.22f) * sin(oa3).toFloat()
                val sz3 = (maxDim * 2.0f).toInt()
                withTransform({
                    translate(cx3, cy3)
                    rotate(Math.toDegrees(angle3.toDouble()).toFloat())
                }) {
                    drawImage(image, dstOffset = IntOffset(-sz3 / 2, -sz3 / 2), dstSize = IntSize(sz3, sz3), filterQuality = FilterQuality.Low, alpha = currentAlpha)
                }
            }

            if (isCrossfading) {
                incomingAlbumImage.let { incoming ->
                    val sz0 = (maxDim * 3.0f).toInt()
                    withTransform({
                        translate(w / 2f, h / 2f)
                        rotate(Math.toDegrees(angle0.toDouble()).toFloat())
                    }) {
                        drawImage(incoming, dstOffset = IntOffset(-sz0 / 2, -sz0 / 2), dstSize = IntSize(sz0, sz0), filterQuality = FilterQuality.Low, alpha = crossfadeProgress)
                    }

                    val sz1 = (maxDim * 2.5f).toInt()
                    withTransform({
                        translate(w / 2f, h / 2f)
                        rotate(-Math.toDegrees(angle1.toDouble()).toFloat())
                    }) {
                        drawImage(incoming, dstOffset = IntOffset(-sz1 / 2, -sz1 / 2), dstSize = IntSize(sz1, sz1), filterQuality = FilterQuality.Low, alpha = crossfadeProgress)
                    }

                    val oa2 = -(angle2 * 0.75f).toDouble()
                    val cx2 = w / 2f + (maxDim * 0.18f) * cos(oa2).toFloat()
                    val cy2 = h / 2f + (maxDim * 0.18f) * sin(oa2).toFloat()
                    val sz2 = (maxDim * 2.2f).toInt()
                    withTransform({
                        translate(cx2, cy2)
                        rotate(-Math.toDegrees(angle2.toDouble()).toFloat())
                    }) {
                        drawImage(incoming, dstOffset = IntOffset(-sz2 / 2, -sz2 / 2), dstSize = IntSize(sz2, sz2), filterQuality = FilterQuality.Low, alpha = crossfadeProgress)
                    }

                    val oa3 = (angle3 * 0.5f).toDouble()
                    val cx3 = w / 2f + (maxDim * 0.22f) * cos(oa3).toFloat()
                    val cy3 = h / 2f + (maxDim * 0.22f) * sin(oa3).toFloat()
                    val sz3 = (maxDim * 2.0f).toInt()
                    withTransform({
                        translate(cx3, cy3)
                        rotate(Math.toDegrees(angle3.toDouble()).toFloat())
                    }) {
                        drawImage(incoming, dstOffset = IntOffset(-sz3 / 2, -sz3 / 2), dstSize = IntSize(sz3, sz3), filterQuality = FilterQuality.Low, alpha = crossfadeProgress)
                    }
                }
            }

            drawRect(Color.Black.copy(alpha = 0.30f))
        }
    }
}
