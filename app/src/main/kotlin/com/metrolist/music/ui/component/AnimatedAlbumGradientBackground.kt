package com.metrolist.music.ui.component

import android.graphics.Bitmap
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.palette.graphics.Palette
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.sqrt

private val FallbackColors = listOf(
    Color(0xFF2E2E2E),
    Color(0xFF424242),
    Color(0xFF1C1C1C),
    Color(0xFF383838)
)

private fun colorToHSL(c: Color): Triple<Float, Float, Float> {
    val r = c.red.coerceIn(0f, 1f)
    val g = c.green.coerceIn(0f, 1f)
    val b = c.blue.coerceIn(0f, 1f)
    val max = maxOf(r, g, b)
    val min = minOf(r, g, b)
    val l = (max + min) / 2f
    val d = max - min
    if (d < 0.001f) return Triple(0f, 0f, l)
    val s = if (l > 0.5f) d / (2f - max - min) else d / (max + min)
    val h = when {
        max == r -> {
            var sector = (g - b) / d
            if (sector < 0f) sector += 6f
            sector / 6f
        }
        max == g -> ((b - r) / d + 2f) / 6f
        else -> ((r - g) / d + 4f) / 6f
    }
    return Triple(h.coerceIn(0f, 1f), s.coerceIn(0f, 1f), l.coerceIn(0f, 1f))
}

private fun hslToColor(h: Float, s: Float, l: Float): Color {
    if (s < 0.001f) return Color(l, l, l, 1f)
    val h6 = (h * 360f) / 60f
    val c = (1f - abs(2f * l - 1f)) * s
    val x = c * (1f - abs(h6 % 2f - 1f))
    val m = l - c / 2f
    val (r1, g1, b1) = when {
        h6 < 1f -> Triple(c, x, 0f)
        h6 < 2f -> Triple(x, c, 0f)
        h6 < 3f -> Triple(0f, c, x)
        h6 < 4f -> Triple(0f, x, c)
        h6 < 5f -> Triple(x, 0f, c)
        else    -> Triple(c, 0f, x)
    }
    return Color(
        red   = (r1 + m).coerceIn(0f, 1f),
        green = (g1 + m).coerceIn(0f, 1f),
        blue  = (b1 + m).coerceIn(0f, 1f),
        alpha = 1f
    )
}

private fun hueTargetL(h: Float, s: Float): ClosedFloatingPointRange<Float> {
    if (s < 0.15f) return 0.22f..0.30f
    val hueDeg = h * 360f
    return when {
        hueDeg in 200f..280f -> 0.22f..0.28f
        hueDeg < 40f || hueDeg > 320f -> 0.26f..0.32f
        hueDeg in 80f..200f -> 0.24f..0.30f
        else -> 0.26f..0.32f
    }
}

private fun normalizeToReadable(c: Color): Color {
    val (h, s, l) = colorToHSL(c)
    // whites
    if (s < 0.15f && l > 0.70f) return hslToColor(h, s, 0.28f)
    // near-blacks floor
    if (l < 0.18f) return hslToColor(h, s, 0.22f)

    val range = hueTargetL(h, s)
    val targetL = when {
        l > range.endInclusive + 0.08f -> range.start + (l - range.endInclusive) * 0.10f
        l < range.start -> range.start
        else -> l
    }.coerceIn(range.start, range.endInclusive)

    val result = hslToColor(h, s, targetL)

    // SATURATION GUARD: if input was saturated but output lost its color, recompute directly
    val (_, rs, _) = colorToHSL(result)
    return if (s > 0.25f && rs < 0.10f) {
        // hslToColor broke - fallback: manually darken original RGB proportionally
        val factor = targetL / l.coerceAtLeast(0.01f)
        Color(
            red   = (c.red * factor).coerceIn(0f, 1f),
            green = (c.green * factor).coerceIn(0f, 1f),
            blue  = (c.blue * factor).coerceIn(0f, 1f),
            alpha = 1f
        )
    } else result
}

private data class ExtractedSwatch(
    val color: Color,
    val population: Int
)

@Composable
fun AnimatedAlbumGradientBackground(
    thumbnail: Bitmap?,
    modifier: Modifier = Modifier
) {
    var swatches by remember(thumbnail) { mutableStateOf<List<ExtractedSwatch>>(emptyList()) }

    LaunchedEffect(thumbnail) {
        if (thumbnail == null) {
            swatches = emptyList()
            return@LaunchedEffect
        }

        val result = withContext(Dispatchers.IO) {
            val palette = Palette.from(thumbnail).generate()
            val allSwatches = listOfNotNull(
                palette.vibrantSwatch,
                palette.darkVibrantSwatch,
                palette.lightVibrantSwatch,
                palette.mutedSwatch,
                palette.darkMutedSwatch,
                palette.lightMutedSwatch,
                palette.dominantSwatch
            ).map { ExtractedSwatch(Color(it.rgb), it.population) }

            if (allSwatches.isEmpty()) return@withContext emptyList()

            val totalPop = allSwatches.sumOf { it.population }
            val minPopThreshold = totalPop * 0.03f
            var filtered = allSwatches.filter { it.population >= minPopThreshold }
            if (filtered.size < 2) filtered = allSwatches.filter { it.population >= totalPop * 0.015f }
            if (filtered.isEmpty()) filtered = allSwatches

            val distinct = mutableListOf<ExtractedSwatch>()
            filtered.sortedByDescending { it.population }.forEach { candidate ->
                val tooClose = distinct.any { existing ->
                    calculateDistance(candidate.color, existing.color) < 60
                }
                if (!tooClose && distinct.size < 5) {
                    distinct.add(candidate)
                }
            }
            distinct.map { it.copy(color = normalizeToReadable(it.color)) }
        }
        swatches = result
    }

    val finalColors = if (swatches.isEmpty()) FallbackColors else swatches.map { it.color }
    val populations = swatches.map { it.population }

    val c1 by animateColorAsState(targetValue = finalColors[0 % finalColors.size], animationSpec = tween(1200), label = "c1")
    val c2 by animateColorAsState(targetValue = finalColors[1 % finalColors.size], animationSpec = tween(1200), label = "c2")
    val c3 by animateColorAsState(targetValue = finalColors[2 % finalColors.size], animationSpec = tween(1200), label = "c3")
    val c4 by animateColorAsState(targetValue = finalColors[3 % finalColors.size], animationSpec = tween(1200), label = "c4")
    val c5 by animateColorAsState(targetValue = finalColors[4 % finalColors.size], animationSpec = tween(1200), label = "c5")

    val infiniteTransition = rememberInfiniteTransition(label = "gradient")
    val offset1 by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(8000, easing = LinearEasing), RepeatMode.Reverse), label = "o1"
    )
    val offset2 by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(13000, easing = LinearEasing), RepeatMode.Reverse), label = "o2"
    )
    val offset3 by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(17000, easing = LinearEasing), RepeatMode.Reverse), label = "o3"
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height
        val maxDim = max(width, height)
        val totalPop = populations.sum().toFloat()

        fun getRadius(index: Int): Float {
            if (populations.isEmpty()) return 0.9f * maxDim
            val pop = populations.getOrNull(index)?.toFloat() ?: 0f
            val ratio = if (totalPop > 0) pop / totalPop else 0f
            return (maxDim * (0.55f + 0.65f * ratio)).coerceIn(0.5f * maxDim, 1.2f * maxDim)
        }

        val animatedColors = listOf(c1, c2, c3, c4, c5)
        val positions = listOf(
            Offset(width * (0.15f + 0.18f * (offset1 * 2 - 1)), height * (0.20f + 0.18f * (offset2 * 2 - 1))),
            Offset(width * (0.85f + 0.18f * (offset2 * 2 - 1)), height * (0.80f + 0.18f * (offset3 * 2 - 1))),
            Offset(width * (0.80f + 0.18f * (offset3 * 2 - 1)), height * (0.15f + 0.18f * (offset1 * 2 - 1))),
            Offset(width * (0.20f + 0.18f * ((1f - offset1) * 2 - 1)), height * (0.85f + 0.18f * ((1f - offset2) * 2 - 1))),
            Offset(width * (0.50f + 0.18f * (offset2 * 2 - 1)), height * (0.50f + 0.18f * (offset3 * 2 - 1)))
        )

        positions.forEachIndexed { i, pos ->
            val r = getRadius(i)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(animatedColors[i], Color.Transparent),
                    center = pos,
                    radius = r
                ),
                radius = r,
                center = pos
            )
        }
    }
}

private fun calculateDistance(c1: Color, c2: Color): Double {
    val r1 = c1.red * 255
    val g1 = c1.green * 255
    val b1 = c1.blue * 255
    val r2 = c2.red * 255
    val g2 = c2.green * 255
    val b2 = c2.blue * 255
    return sqrt((r1 - r2).toDouble().pow(2) + (g1 - g2).toDouble().pow(2) + (b1 - b2).toDouble().pow(2))
}
