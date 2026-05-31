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
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

// 4-sprite Apple Music-style background.
// Structure: PixiJS LyricsScene reference (4 copies of album art, orbit + rotation).
// Blur: Modifier.blur (same API as LyricsLine). Saturation: graphicsLayer colorFilter.
// Periods: 2π / (speed * 30fps). Speeds: [0.003, 0.008, 0.006, 0.004] rad/frame.
@Composable
fun BetterAnimatedGradientBackground(
    thumbnail: Bitmap?,
    modifier: Modifier = Modifier,
) {
    val albumImage = remember(thumbnail) { thumbnail?.asImageBitmap() }

    val infinite = rememberInfiniteTransition(label = "better_anim_grad")

    // Periods 2.5× slower than PixiJS reference — more hypnotic on mobile.
    // RepeatMode.Reverse: no visible jump at loop boundary.
    // sprite 0: CW slow drift
    val angle0 by infinite.animateFloat(
        initialValue = 0f,
        targetValue = (PI * 2).toFloat(),
        animationSpec = infiniteRepeatable(tween(174_533, easing = LinearEasing), RepeatMode.Reverse),
        label = "bag_a0",
    )
    // sprite 1: CCW medium
    val angle1 by infinite.animateFloat(
        initialValue = 0f,
        targetValue = (PI * 2).toFloat(),
        animationSpec = infiniteRepeatable(tween(65_450, easing = LinearEasing), RepeatMode.Reverse),
        label = "bag_a1",
    )
    // sprite 2: CCW orbit
    val angle2 by infinite.animateFloat(
        initialValue = 0f,
        targetValue = (PI * 2).toFloat(),
        animationSpec = infiniteRepeatable(tween(87_268, easing = LinearEasing), RepeatMode.Reverse),
        label = "bag_a2",
    )
    // sprite 3: CW orbit
    val angle3 by infinite.animateFloat(
        initialValue = 0f,
        targetValue = (PI * 2).toFloat(),
        animationSpec = infiniteRepeatable(tween(130_900, easing = LinearEasing), RepeatMode.Reverse),
        label = "bag_a3",
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .graphicsLayer {
                colorFilter = ColorFilter.colorMatrix(
                    ColorMatrix().apply { setToSaturation(1.60f) }
                )
            },
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .blur(120.dp),  // heavier blur = more zoom, fewer distinct colors visible
        ) {
            val w = size.width
            val h = size.height
            val maxDim = max(w, h)

            // Dark base — ensures no pure-black bleed if blur clips at edge
            drawRect(Color(0xFF0D0D0D))
            val image = albumImage ?: return@Canvas

            // sprite 0 — centered, 3.0× maxDim.
            // GUARANTEES full coverage: at 3× the sprite extends 1.5× maxDim in all directions
            // from center — impossible to expose background regardless of position.
            val sz0 = (maxDim * 3.0f).toInt()
            withTransform({
                translate(w / 2f, h / 2f)
                rotate(Math.toDegrees(angle0.toDouble()).toFloat())
            }) {
                drawImage(image, dstOffset = IntOffset(-sz0 / 2, -sz0 / 2), dstSize = IntSize(sz0, sz0), filterQuality = FilterQuality.Low)
            }

            // sprite 1 — centered, 2.5× maxDim, faster CCW rotation
            val sz1 = (maxDim * 2.5f).toInt()
            withTransform({
                translate(w / 2f, h / 2f)
                rotate(-Math.toDegrees(angle1.toDouble()).toFloat())
            }) {
                drawImage(image, dstOffset = IntOffset(-sz1 / 2, -sz1 / 2), dstSize = IntSize(sz1, sz1), filterQuality = FilterQuality.Low)
            }

            // sprite 2 — orbiting CCW, 2.2× maxDim, tight orbit so it never leaves coverage zone
            val oa2 = -(angle2 * 0.75f).toDouble()
            val cx2 = w / 2f + (maxDim * 0.18f) * cos(oa2).toFloat()
            val cy2 = h / 2f + (maxDim * 0.18f) * sin(oa2).toFloat()
            val sz2 = (maxDim * 2.2f).toInt()
            withTransform({
                translate(cx2, cy2)
                rotate(-Math.toDegrees(angle2.toDouble()).toFloat())
            }) {
                drawImage(image, dstOffset = IntOffset(-sz2 / 2, -sz2 / 2), dstSize = IntSize(sz2, sz2), filterQuality = FilterQuality.Low)
            }

            // sprite 3 — orbiting CW, 2.0× maxDim
            val oa3 = (angle3 * 0.5f).toDouble()
            val cx3 = w / 2f + (maxDim * 0.22f) * cos(oa3).toFloat()
            val cy3 = h / 2f + (maxDim * 0.22f) * sin(oa3).toFloat()
            val sz3 = (maxDim * 2.0f).toInt()
            withTransform({
                translate(cx3, cy3)
                rotate(Math.toDegrees(angle3.toDouble()).toFloat())
            }) {
                drawImage(image, dstOffset = IntOffset(-sz3 / 2, -sz3 / 2), dstSize = IntSize(sz3, sz3), filterQuality = FilterQuality.Low)
            }

            // Darkness overlay for text legibility
            drawRect(Color.Black.copy(alpha = 0.30f))
        }
    }
}
