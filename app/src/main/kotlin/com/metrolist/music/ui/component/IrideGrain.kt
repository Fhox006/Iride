package com.metrolist.music.ui.component

import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.ComposeShader
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.PorterDuff
import android.graphics.Shader
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionOnScreen
import java.util.Random
import kotlin.math.roundToInt

private const val GrainTileSize = 512

// One immutable, seamless noise tile for the whole app; never regenerate noise during animation.
private val grainTile: Bitmap by lazy {
    val random = Random(0x4952494445L)
    val pixels = IntArray(GrainTileSize * GrainTileSize) {
        val sample = random.nextFloat()
        val squared = sample * sample
        // Sparse black specks: mean coverage is ~20%, with no grey wash or white highlights.
        (squared * squared * 255f).roundToInt() shl 24
    }
    Bitmap.createBitmap(pixels, GrainTileSize, GrainTileSize, Bitmap.Config.ARGB_8888)
}

private val softGrainTile: Bitmap by lazy {
    val pixels = IntArray(GrainTileSize * GrainTileSize)
    grainTile.getPixels(pixels, 0, GrainTileSize, 0, 0, GrainTileSize, GrainTileSize)
    val horizontal = IntArray(pixels.size)
    // A separable, symmetric blur rounds the specks before scaling. Wrap both passes so tiling
    // introduces neither seams nor rows of hard-edged pixels at fractional display densities.
    for (y in 0 until GrainTileSize) {
        val row = y * GrainTileSize
        for (x in 0 until GrainTileSize) {
            val left = (x + GrainTileSize - 1) % GrainTileSize
            val right = (x + 1) % GrainTileSize
            horizontal[row + x] = (pixels[row + left] ushr 24) +
                2 * (pixels[row + x] ushr 24) + (pixels[row + right] ushr 24)
        }
    }
    for (y in 0 until GrainTileSize) {
        val above = ((y + GrainTileSize - 1) % GrainTileSize) * GrainTileSize
        val row = y * GrainTileSize
        val below = ((y + 1) % GrainTileSize) * GrainTileSize
        for (x in 0 until GrainTileSize) {
            val alpha = (horizontal[above + x] + 2 * horizontal[row + x] +
                horizontal[below + x] + 8) / 16
            pixels[row + x] = alpha shl 24
        }
    }
    Bitmap.createBitmap(pixels, GrainTileSize, GrainTileSize, Bitmap.Config.ARGB_8888)
}

internal fun irideGrainBrush(
    density: Float,
    fadeHeight: Float? = null,
    soft: Boolean = true,
): ShaderBrush {
    val scale = (IrideFolio.GrainSize.value * density).coerceAtLeast(1f)
    val noise = BitmapShader(if (soft) softGrainTile else grainTile, Shader.TileMode.REPEAT, Shader.TileMode.REPEAT).apply {
        setLocalMatrix(Matrix().apply { setScale(scale, scale) })
        if (soft && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            setFilterMode(BitmapShader.FILTER_MODE_LINEAR)
        }
    }
    if (fadeHeight == null || fadeHeight <= 0f) return ShaderBrush(noise)

    val stops = FloatArray(33) { it / 32f }
    val colors = IntArray(stops.size) { index ->
        val t = stops[index]
        val fade = t * t * t * (t * (t * 6f - 15f) + 10f)
        ((1f - fade).coerceIn(0f, 1f) * 255f).roundToInt() shl 24
    }
    val mask = LinearGradient(0f, 0f, 0f, fadeHeight, colors, stops, Shader.TileMode.CLAMP)
    // Mask the texture itself, avoiding a full-size offscreen layer just to feather the noise.
    return ShaderBrush(ComposeShader(noise, mask, PorterDuff.Mode.DST_IN))
}

/** Background-only overlay: apply to gradient renderers, never to a container with text/artwork. */
internal fun Modifier.irideBackgroundGrain(
    opacity: Float = IrideFolio.BackgroundGrainOpacity,
    fadeOut: Boolean = false,
): Modifier = if (opacity <= 0f) this else drawWithCache {
    val grain = irideGrainBrush(density, if (fadeOut) size.height else null)
    onDrawWithContent {
        drawContent()
        drawRect(grain, alpha = opacity.coerceIn(0f, 1f))
    }
}

/** The surface clips a field anchored to the backdrop instead of carrying its own noise patch. */
internal fun DrawScope.drawIrideSurfaceGrain(grain: Brush, opacity: Float, sampleOffset: Offset) {
    val surfaceSize = size
    translate(-sampleOffset.x, -sampleOffset.y) {
        drawRect(grain, topLeft = sampleOffset, size = surfaceSize, alpha = opacity.coerceIn(0f, 1f))
    }
}

/** Draw above a surface fill but below its content, inside the owner's existing shape clip. */
@Composable
internal fun Modifier.irideSurfaceGrain(opacity: Float, soft: Boolean = true): Modifier {
    if (opacity <= 0f) return this
    val backdrop = LocalScreenFrostBackdrop.current
    var position by remember { mutableStateOf(Offset.Zero) }
    return onGloballyPositioned { position = it.positionOnScreen() }.drawWithCache {
        val grain = irideGrainBrush(density, soft = soft)
        onDrawBehind {
            drawIrideSurfaceGrain(grain, opacity, position - (backdrop?.positionOnScreen ?: Offset.Zero))
        }
    }
}
