package com.metrolist.music.ui.component

import android.graphics.Bitmap
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.palette.graphics.Palette
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sqrt

private val WhiteMappedGray = Color(0xFF8A8C8E)
private val DarkGrayBlack   = Color(0xFF1C1C1E)

private val FallbackColors = listOf(
    Color(0xFF1C1C1E),
    Color(0xFF2A2A2E),
    Color(0xFF3A3A3F),
    Color(0xFF555555)
)

private data class HslColor(val h: Float, val s: Float, val l: Float)
private data class ExtractedColor(val color: Color, val population: Int)
private data class GradientBlob(
    val color: Color,
    val center: Offset,
    val radius: Float,
    val alpha: Float
)
private data class BackgroundSpec(
    val baseBackground: Color,
    val palette: List<ExtractedColor>
)

private fun colorToHsl(color: Color): HslColor {
    val r = color.red.coerceIn(0f, 1f)
    val g = color.green.coerceIn(0f, 1f)
    val b = color.blue.coerceIn(0f, 1f)
    val maxC = maxOf(r, g, b)
    val minC = minOf(r, g, b)
    val l = (maxC + minC) / 2f
    val delta = maxC - minC
    if (delta < 0.0001f) return HslColor(0f, 0f, l)
    val s = if (l > 0.5f) delta / (2f - maxC - minC) else delta / (maxC + minC)
    val h = when (maxC) {
        r    -> (((g - b) / delta) + if (g < b) 6f else 0f) / 6f
        g    -> (((b - r) / delta) + 2f) / 6f
        else -> (((r - g) / delta) + 4f) / 6f
    }
    return HslColor(h.coerceIn(0f, 1f), s.coerceIn(0f, 1f), l.coerceIn(0f, 1f))
}

private fun hslToColor(hsl: HslColor): Color {
    val h = hsl.h.coerceIn(0f, 1f)
    val s = hsl.s.coerceIn(0f, 1f)
    val l = hsl.l.coerceIn(0f, 1f)
    if (s < 0.0001f) return Color(l, l, l, 1f)
    val q = if (l < 0.5f) l * (1 + s) else l + s - l * s
    val p = 2f * l - q
    fun hueToRgb(tIn: Float): Float {
        var t = tIn
        if (t < 0f) t += 1f
        if (t > 1f) t -= 1f
        return when {
            t < 1f / 6f -> p + (q - p) * 6f * t
            t < 1f / 2f -> q
            t < 2f / 3f -> p + (q - p) * (2f / 3f - t) * 6f
            else         -> p
        }
    }
    return Color(
        red   = hueToRgb(h + 1f / 3f),
        green = hueToRgb(h),
        blue  = hueToRgb(h - 1f / 3f),
        alpha = 1f
    )
}

private fun hueDistance(a: Float, b: Float): Float {
    val d = abs(a - b)
    return min(d, 1f - d)
}

private fun colorDistanceHsl(a: Color, b: Color): Float {
    val h1 = colorToHsl(a)
    val h2 = colorToHsl(b)
    val dh = hueDistance(h1.h, h2.h) * 100f
    val ds = abs(h1.s - h2.s) * 100f
    val dl = abs(h1.l - h2.l) * 100f
    return sqrt(dh * dh + ds * ds + dl * dl)
}

private fun blend(a: Color, b: Color, t: Float): Color {
    val p = t.coerceIn(0f, 1f)
    return Color(
        red   = a.red   * (1f - p) + b.red   * p,
        green = a.green * (1f - p) + b.green * p,
        blue  = a.blue  * (1f - p) + b.blue  * p,
        alpha = 1f
    )
}

private fun downscaleBitmap(bitmap: Bitmap, maxSide: Int = 96): Bitmap {
    val w = bitmap.width
    val h = bitmap.height
    val largest = max(w, h)
    if (largest <= maxSide) return bitmap
    val scale   = maxSide.toFloat() / largest
    val targetW = max(1, (w * scale).roundToInt())
    val targetH = max(1, (h * scale).roundToInt())
    return Bitmap.createScaledBitmap(bitmap, targetW, targetH, true)
}

private fun normalizeColorForBackground(color: Color): Color {
    val hsl = colorToHsl(color)

    // CASO 1: nero con artefatto JPEG (tinta scurissima) → nero pulito
    if (hsl.l < 0.16f && hsl.s > 0.12f) {
        return hslToColor(HslColor(hsl.h, 0f, 0.12f))
    }

    // CASO 2: nero / molto scuro → mantieni scuro
    if (hsl.l < 0.16f) {
        return hslToColor(HslColor(hsl.h, hsl.s.coerceAtMost(0.10f), 0.13f))
    }

    // CASO 3: bianco / quasi-bianco / pastello slavato → WhiteMappedGray
    if (hsl.l > 0.72f && hsl.s < 0.40f) {
        return WhiteMappedGray
    }

    // CASO 4: neutro (grigio) → Tieni la tinta originale, normalizza solo L in range ristretto
    if (hsl.s < 0.09f) {
        // Massimo 2 grigi: uno scuro (0b0b0b) e uno medio (818483)
        // Mantieni la tinta originale degli altri grigi
        val targetL = hsl.l.coerceIn(0.16f, 0.51f)  // range medio tra i 2 grigi estremi
        return hslToColor(HslColor(hsl.h, hsl.s, targetL))
    }

    // CASO 5: colore già intenso (s >= 0.60) → preserva tonalità, abbassa solo L
    if (hsl.s >= 0.60f) {
        val targetL = when {
            hsl.l > 0.70f -> (hsl.l - 0.25f).coerceIn(0.28f, 0.50f)
            hsl.l > 0.50f -> (hsl.l - 0.12f).coerceIn(0.28f, 0.50f)
            else           -> hsl.l.coerceIn(0.20f, 0.50f)
        }
        return hslToColor(HslColor(hsl.h, hsl.s, targetL))
    }

    // CASO 6: colore normale (0.09 <= s < 0.60)
    val targetL = when {
        hsl.l > 0.72f -> (hsl.l - 0.28f).coerceIn(0.22f, 0.48f)
        hsl.l > 0.55f -> (hsl.l - 0.18f).coerceIn(0.22f, 0.48f)
        else           -> hsl.l.coerceIn(0.16f, 0.48f)
    }
    val compensation = (hsl.l - targetL).coerceAtLeast(0f)
    val satBoost     = compensation * 0.20f
    val targetS      = (hsl.s + satBoost).coerceAtMost(0.88f).coerceAtLeast(hsl.s)
    return hslToColor(HslColor(hsl.h, targetS, targetL))
}

// ─────────────────────────────────────────────────────────────
// Rimossa postProcessColor - ora i grigi mantengono la loro tinta
// La deduplicazione tiene MAX 2 grigi (scuro + medio)
// ─────────────────────────────────────────────────────────────

private fun deduplicateGrays(colors: List<ExtractedColor>): List<ExtractedColor> {
    var darkGrayCount = 0
    var midGrayCount = 0
    val result = mutableListOf<ExtractedColor>()

    colors.forEach { ec ->
        val hsl = colorToHsl(ec.color)
        if (hsl.s < 0.09f) {  // è un grigio
            when {
                hsl.l < 0.16f && darkGrayCount < 1 -> {
                    result += ec
                    darkGrayCount++
                }
                hsl.l >= 0.16f && hsl.l <= 0.55f && midGrayCount < 1 -> {
                    result += ec
                    midGrayCount++
                }
                hsl.l > 0.55f -> {
                    // grigio chiaro → mappalo a WhiteMappedGray se non già presente
                    if (result.none { colorDistanceHsl(it.color, WhiteMappedGray) < 10f }) {
                        result += ec.copy(color = WhiteMappedGray)
                    }
                }
            }
        } else {
            // colore saturo → tieni sempre
            result += ec
        }
    }

    return result
}

private fun extractColors(bitmap: Bitmap): List<ExtractedColor> {
    val scaled  = downscaleBitmap(bitmap, 96)
    val palette = Palette.from(scaled).maximumColorCount(12).generate()

    val swatches = listOfNotNull(
        palette.dominantSwatch,
        palette.darkVibrantSwatch,
        palette.vibrantSwatch,
        palette.lightVibrantSwatch,
        palette.darkMutedSwatch,
        palette.mutedSwatch,
        palette.lightMutedSwatch
    ).map { ExtractedColor(Color(it.rgb), it.population) }

    if (swatches.isEmpty()) return emptyList()

    val totalPop = swatches.sumOf { it.population }.coerceAtLeast(1)
    val filtered = swatches.filter { it.population.toFloat() / totalPop >= 0.008f }

    val distinct = mutableListOf<ExtractedColor>()
    filtered.sortedByDescending { it.population }.forEach { candidate ->
        val normalized = normalizeColorForBackground(candidate.color)
        if (distinct.none { colorDistanceHsl(it.color, normalized) < 18f }) {
            distinct += candidate.copy(color = normalized)
        }
    }

    while (distinct.size < 5) {
        val base = distinct.getOrElse(distinct.size % max(1, distinct.size)) {
            ExtractedColor(DarkGrayBlack, 1)
        }
        val hsl     = colorToHsl(base.color)
        val variant = hslToColor(
            HslColor(
                hsl.h,
                hsl.s * 0.85f,
                (hsl.l - 0.06f).coerceAtLeast(0.13f)
            )
        )
        distinct += ExtractedColor(variant, 1)
    }

    val deduplicated = deduplicateGrays(distinct)

    val result = deduplicated.toMutableList()
    var hasDarkGray = result.any {
        colorToHsl(it.color).let { hsl -> hsl.s < 0.09f && hsl.l < 0.16f }
    }
    var hasMidGray = result.any {
        colorToHsl(it.color).let { hsl -> hsl.s < 0.09f && hsl.l in 0.16f..0.55f }
    }

    while (result.size < 5) {
        if (!hasDarkGray) {
            result += ExtractedColor(Color(0xFF0B0B0B), 1)
            hasDarkGray = true
        } else if (!hasMidGray) {
            result += ExtractedColor(Color(0xFF818483), 1)
            hasMidGray = true
        } else {
            val saturated = result.firstOrNull { colorToHsl(it.color).s >= 0.09f }
            if (saturated != null) {
                val hsl = colorToHsl(saturated.color)
                val variant = hslToColor(
                    HslColor(
                        hsl.h,
                        hsl.s * 0.85f,
                        (hsl.l - 0.06f).coerceAtLeast(0.13f)
                    )
                )
                result += ExtractedColor(variant, 1)
            } else {
                result += ExtractedColor(DarkGrayBlack, 1)
            }
        }
    }

    return result.take(5)
}

private fun buildBackgroundSpec(bitmap: Bitmap): BackgroundSpec {
    val extracted      = extractColors(bitmap)
    val baseColor      = extracted.firstOrNull()?.color ?: DarkGrayBlack
    val baseBackground = blend(baseColor, Color.Black, 0.65f)
    return BackgroundSpec(baseBackground, extracted)
}

// ======================= COMPOSABLE PRINCIPALE =======================

@Composable
fun AnimatedAlbumGradientBackground(
    thumbnail: Bitmap?,
    modifier: Modifier = Modifier
) {
    var spec by remember(thumbnail) { mutableStateOf<BackgroundSpec?>(null) }

    LaunchedEffect(thumbnail) {
        if (thumbnail == null) {
            spec = null
            return@LaunchedEffect
        }

        val builtSpec = withContext(Dispatchers.IO) {
            runCatching { buildBackgroundSpec(thumbnail) }.getOrNull()
        }
        spec = builtSpec
    }

    val palette = spec?.palette ?: FallbackColors.map { ExtractedColor(it, 1) }
    val finalColors = when {
        palette.size >= 5 -> palette.map { it.color }.take(5)
        else              -> palette.map { it.color } + List(5 - palette.size) { DarkGrayBlack }
    }

    val c1 by animateColorAsState(finalColors[0], tween(1400))
    val c2 by animateColorAsState(finalColors[1], tween(1400))
    val c3 by animateColorAsState(finalColors[2], tween(1400))
    val c4 by animateColorAsState(finalColors[3], tween(1400))
    val c5 by animateColorAsState(finalColors[4], tween(1400))

    val animatedBaseBackground by animateColorAsState(
        spec?.baseBackground ?: DarkGrayBlack,
        tween(1400)
    )

    val infinite = rememberInfiniteTransition()
    val t1 by infinite.animateFloat(0f, (Math.PI * 2).toFloat(), infiniteRepeatable(tween(24000, easing = LinearEasing)))
    val t2 by infinite.animateFloat(0f, (Math.PI * 2).toFloat(), infiniteRepeatable(tween(31000, easing = LinearEasing)))
    val t3 by infinite.animateFloat(0f, (Math.PI * 2).toFloat(), infiniteRepeatable(tween(39000, easing = LinearEasing)))

    Box(modifier = modifier.fillMaxSize()) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w      = size.width
            val h      = size.height
            val maxDim = max(w, h)

            drawRect(animatedBaseBackground)

            val blobs = listOf(
                GradientBlob(c1, Offset(w * 0.28f, h * 0.25f), maxDim * 0.58f, 0.75f),
                GradientBlob(c2, Offset(w * 0.75f, h * 0.32f), maxDim * 0.52f, 0.62f),
                GradientBlob(c3, Offset(w * 0.68f, h * 0.74f), maxDim * 0.47f, 0.48f),
                GradientBlob(c4, Offset(w * 0.22f, h * 0.78f), maxDim * 0.41f, 0.38f),
                GradientBlob(c5, Offset(w * 0.52f, h * 0.48f), maxDim * 0.35f, 0.28f)
            )

            blobs.forEach { blob ->
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            blob.color.copy(alpha = blob.alpha),
                            blob.color.copy(alpha = blob.alpha * 0.45f),
                            blob.color.copy(alpha = blob.alpha * 0.12f),
                            Color.Transparent
                        ),
                        center = blob.center,
                        radius = blob.radius
                    ),
                    radius    = blob.radius,
                    center    = blob.center,
                    blendMode = BlendMode.SrcOver
                )
            }

            drawRect(
                Brush.verticalGradient(
                    listOf(Color.Black.copy(0.12f), Color.Transparent, Color.Black.copy(0.22f))
                )
            )
        }
    }
}

/*
@Composable
private fun DebugColorPipelineDialog(
    rawColors        : List<ExtractedColor>,
    normalizedColors : List<ExtractedColor>,
    baseBackground   : Color,
    onDismiss        : () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFF111114))
                .padding(16.dp)
        ) {
            Text(
                "🎨  DEBUG — Color Pipeline",
                color      = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize   = 13.sp,
                fontFamily = FontFamily.Monospace
            )
            Spacer(Modifier.height(12.dp))

            Text("① Colori campionati (raw):",
                color = Color(0xFFFFD060), fontSize = 11.sp, fontFamily = FontFamily.Monospace)
            Spacer(Modifier.height(4.dp))
            rawColors.forEachIndexed { i, ec ->
                ColorSwatch(ec.color, "Raw[$i]", "pop:${ec.population}  ${ec.color.normalizeLabel()}")
            }

            Spacer(Modifier.height(12.dp))
            Text("② Dopo normalize + postProcess + dedup:",
                color = Color(0xFF60D0FF), fontSize = 11.sp, fontFamily = FontFamily.Monospace)
            Spacer(Modifier.height(4.dp))
            normalizedColors.forEachIndexed { i, ec ->
                ColorSwatch(ec.color, "Final[$i]", ec.color.normalizeLabel())
            }

            Spacer(Modifier.height(12.dp))
            Text("③ baseBackground (blend → Black 65%):",
                color = Color(0xFF90FF90), fontSize = 11.sp, fontFamily = FontFamily.Monospace)
            Spacer(Modifier.height(4.dp))
            ColorSwatch(baseBackground, "Base")

            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                Text(
                    "  Chiudi  ",
                    color      = Color(0xFFFFD060),
                    fontWeight = FontWeight.Bold,
                    fontSize   = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier   = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF2A2A2E))
                        .clickable { onDismiss() }
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
        }
    }
}
*/