package com.metrolist.music.ui.screens

import android.graphics.Bitmap
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.res.painterResource
import androidx.navigation.NavController
import androidx.palette.graphics.Palette
import com.metrolist.music.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.sqrt

private val IrideFallbackColors = listOf(
    Color(0xFF6A0DAD),
    Color(0xFF4B0082),
    Color(0xFF8B00FF),
    Color(0xFF9400D3),
    Color(0xFF7B2FBE)
)

private data class IrideExtractedSwatch(
    val color: Color,
    val population: Int
)

private fun irideColorToHSL(c: Color): Triple<Float, Float, Float> {
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
        max == r -> { var sector = (g - b) / d; if (sector < 0f) sector += 6f; sector / 6f }
        max == g -> ((b - r) / d + 2f) / 6f
        else -> ((r - g) / d + 4f) / 6f
    }
    return Triple(h.coerceIn(0f, 1f), s.coerceIn(0f, 1f), l.coerceIn(0f, 1f))
}

private fun irideHslToColor(h: Float, s: Float, l: Float): Color {
    if (s < 0.001f) return Color(l, l, l, 1f)
    val h6 = (h * 360f) / 60f
    val c = (1f - abs(2f * l - 1f)) * s
    val x = c * (1f - abs(h6 % 2f - 1f))
    val m = l - c / 2f
    val (r1, g1, b1) = when {
        h6 < 1f -> Triple(c, x, 0f); h6 < 2f -> Triple(x, c, 0f)
        h6 < 3f -> Triple(0f, c, x); h6 < 4f -> Triple(0f, x, c)
        h6 < 5f -> Triple(x, 0f, c); else -> Triple(c, 0f, x)
    }
    return Color((r1 + m).coerceIn(0f, 1f), (g1 + m).coerceIn(0f, 1f), (b1 + m).coerceIn(0f, 1f), 1f)
}

private fun irideHueTargetL(h: Float, s: Float): ClosedFloatingPointRange<Float> {
    if (s < 0.15f) return 0.22f..0.29f
    val hueDeg = h * 360f
    return when {
        hueDeg in 200f..280f -> 0.21f..0.27f
        hueDeg < 40f || hueDeg > 320f -> 0.24f..0.30f
        hueDeg in 80f..200f -> 0.23f..0.29f
        else -> 0.23f..0.30f
    }
}

private fun irideNormalizeColor(c: Color): Color {
    val (h, s, l) = irideColorToHSL(c)
    if (s < 0.12f && l > 0.65f) return irideHslToColor(h, s, 0.25f)
    if (l < 0.20f) return irideHslToColor(h, s, 0.23f)
    val range = irideHueTargetL(h, s)
    var targetL = when {
        l > 0.55f -> range.start + (l - range.endInclusive) * 0.38f
        l > range.endInclusive -> range.endInclusive - 0.04f
        else -> l - 0.045f
    }.coerceIn(range.start - 0.05f, range.endInclusive - 0.02f)
    val result = irideHslToColor(h, s.coerceAtLeast(0.28f), targetL)
    val (_, rs, _) = irideColorToHSL(result)
    return if (s > 0.30f && rs < 0.20f) {
        val factor = (targetL / l.coerceAtLeast(0.15f)).coerceAtMost(0.93f)
        Color((c.red * factor).coerceIn(0.05f, 0.95f), (c.green * factor).coerceIn(0.05f, 0.95f), (c.blue * factor).coerceIn(0.05f, 0.95f), 1f)
    } else result
}

private fun irideCalculateDistance(c1: Color, c2: Color): Double {
    val r1 = c1.red * 255; val g1 = c1.green * 255; val b1 = c1.blue * 255
    val r2 = c2.red * 255; val g2 = c2.green * 255; val b2 = c2.blue * 255
    return sqrt((r1 - r2).toDouble().pow(2) + (g1 - g2).toDouble().pow(2) + (b1 - b2).toDouble().pow(2))
}

private fun Color.irideIsVibrant(): Boolean {
    val (_, s, l) = irideColorToHSL(this)
    return s > 0.65f && l in 0.25f..0.75f
}

private fun Color.irideIsDarkVibrant(): Boolean {
    val (_, s, l) = irideColorToHSL(this)
    return s > 0.60f && l < 0.45f
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IrideScreen(
    navController: NavController,
    thumbnail: Bitmap? = null
) {
    var swatches by remember(thumbnail) { mutableStateOf<List<IrideExtractedSwatch>>(emptyList()) }

    LaunchedEffect(thumbnail) {
        if (thumbnail == null) {
            swatches = emptyList()
            return@LaunchedEffect
        }
        val result = withContext(Dispatchers.IO) {
            val palette = Palette.from(thumbnail).generate()
            val candidates = listOfNotNull(
                palette.vibrantSwatch, palette.darkVibrantSwatch, palette.mutedSwatch,
                palette.darkMutedSwatch, palette.dominantSwatch, palette.lightVibrantSwatch
            )
            if (candidates.isEmpty()) return@withContext emptyList()
            val sorted = candidates
                .map { IrideExtractedSwatch(Color(it.rgb), it.population) }
                .sortedWith(compareByDescending { swatch ->
                    when {
                        swatch.color.irideIsVibrant() -> 1000 + swatch.population
                        swatch.color.irideIsDarkVibrant() -> 800 + swatch.population
                        else -> swatch.population
                    }
                })
            val totalPop = sorted.sumOf { it.population }.toFloat()
            val minThreshold = (totalPop * 0.025f).toInt()
            val filtered = sorted.filter { it.population >= minThreshold }.take(6)
            val distinct = mutableListOf<IrideExtractedSwatch>()
            filtered.forEach { candidate ->
                if (distinct.size >= 5) return@forEach
                if (distinct.none { irideCalculateDistance(candidate.color, it.color) < 55 }) {
                    distinct.add(candidate)
                }
            }
            distinct.map { it.copy(color = irideNormalizeColor(it.color)) }
        }
        swatches = result
    }

    val finalColors = if (swatches.isEmpty()) IrideFallbackColors else swatches.map { it.color }
    val populations = swatches.map { it.population }

    val c1 by animateColorAsState(finalColors[0 % finalColors.size], tween(1200), label = "c1")
    val c2 by animateColorAsState(finalColors[1 % finalColors.size], tween(1200), label = "c2")
    val c3 by animateColorAsState(finalColors[2 % finalColors.size], tween(1200), label = "c3")
    val c4 by animateColorAsState(finalColors[3 % finalColors.size], tween(1200), label = "c4")
    val c5 by animateColorAsState(finalColors[4 % finalColors.size], tween(1200), label = "c5")

    val inf = rememberInfiniteTransition(label = "iride_gradient")
    val o1 by inf.animateFloat(0f, 1f, infiniteRepeatable(tween(8000, easing = LinearEasing), RepeatMode.Reverse), label = "o1")
    val o2 by inf.animateFloat(0f, 1f, infiniteRepeatable(tween(13000, easing = LinearEasing), RepeatMode.Reverse), label = "o2")
    val o3 by inf.animateFloat(0f, 1f, infiniteRepeatable(tween(17000, easing = LinearEasing), RepeatMode.Reverse), label = "o3")

    Box(modifier = Modifier.fillMaxSize()) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width; val h = size.height; val md = max(w, h)
            val totalPop = populations.sum().toFloat()
            fun getRadius(i: Int): Float {
                if (populations.isEmpty()) return 0.95f * md
                val pop = populations.getOrNull(i)?.toFloat() ?: 0f
                val ratio = if (totalPop > 0) pop / totalPop else 0f
                return (md * (0.60f + 0.70f * ratio)).coerceIn(0.55f * md, 1.35f * md)
            }

            val animColors = listOf(c1, c2, c3, c4, c5)
            val positions = listOf(
                Offset(w * (0.15f + 0.18f * (o1 * 2 - 1)), h * (0.20f + 0.18f * (o2 * 2 - 1))),
                Offset(w * (0.85f + 0.18f * (o2 * 2 - 1)), h * (0.80f + 0.18f * (o3 * 2 - 1))),
                Offset(w * (0.80f + 0.18f * (o3 * 2 - 1)), h * (0.15f + 0.18f * (o1 * 2 - 1))),
                Offset(w * (0.20f + 0.18f * ((1f - o1) * 2 - 1)), h * (0.85f + 0.18f * ((1f - o2) * 2 - 1))),
                Offset(w * (0.50f + 0.18f * (o2 * 2 - 1)), h * (0.50f + 0.18f * (o3 * 2 - 1)))
            )
            positions.forEachIndexed { i, pos ->
                val r = getRadius(i)
                drawCircle(brush = Brush.radialGradient(listOf(animColors[i], Color.Transparent), pos, r), radius = r, center = pos)
            }
        }

        TopAppBar(
            title = { Text("Iride") },
            navigationIcon = {
                IconButton(onClick = { navController.navigateUp() }) {
                    Icon(painterResource(R.drawable.arrow_back), contentDescription = null)
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
        )
    }
}
