package com.metrolist.music.ui.player

import android.graphics.Bitmap
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageShader
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.addOutline
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.metrolist.music.ui.theme.FolioShape
import kotlin.random.Random
import sv.lib.squircleshape.SquircleShape

internal val IrideMp3BackgroundColor = Color(0xFF080809)
internal val IrideMp3SurfaceColor = Color.White.copy(alpha = 0.06f)
internal val IrideMp3PanelBorderColor = Color.White.copy(alpha = 0.12f)
internal val IrideMp3FrameInset = 8.dp
internal val IrideMp3FrameHorizontalPadding = 14.dp
internal val IrideMp3FrameShape = irideSquircle(18.dp)
internal val IrideMp3FrameBorderColor = Color(0xFF414141)
internal val IrideMp3InfoActionSize = 48.dp
internal val IrideMp3InfoActionIconSize = 28.dp
internal val IrideMp3InfoActionColor = Color.White.copy(alpha = 0.9f)
internal val IrideMp3DimIconColor = Color.White.copy(alpha = 0.75f)
internal val IrideMp3ArtworkShape = FolioShape(5.dp)
internal val IrideMp3MenuRowShape = FolioShape(8.dp)
internal val IrideMp3MenuOrder = listOf(0, 1, 3, 2, 5, 4)

// Shared artist-line grey — single source of truth for both the static info block and the
// bridge overlay, so the expanded state renders identically no matter which path draws it.
internal val IrideArtistTextColor = Color(0xFFB8B8B8)

internal val IrideMp3WheelSurfaceColor = Color(0xFF1C1C1C)
internal val IrideMp3WheelCenterColor = Color(0xFF0C0C0C)
internal val IrideMp3WheelBorderColor = Color(0xFF454545)
internal val IrideMp3WheelCenterBorderColor = Color(0xFF5C5C5C)

private val mp3MatteTiles by lazy {
    val tileSize = 256
    val random = Random(0x49524944)
    val grain = IntArray(tileSize * tileSize) { random.nextInt(5) - random.nextInt(5) }
    listOf(IrideMp3BackgroundColor, IrideMp3WheelSurfaceColor, IrideMp3WheelCenterColor).associateWith { color ->
        val base = color.toArgb()
        // One texel per pixel, with the same small variation in each RGB channel.
        val pixels = IntArray(grain.size) { base + grain[it] * 0x010101 }
        Bitmap.createBitmap(pixels, tileSize, tileSize, Bitmap.Config.ARGB_8888).asImageBitmap()
    }
}

internal fun irideMp3ShellAlpha(progress: Float): Float = ((progress - 0.7f) / 0.3f).coerceIn(0f, 1f)

internal fun Modifier.mp3MatteSurface(color: Color): Modifier = drawWithCache {
    val tile = mp3MatteTiles.getValue(color)
    val grain = ShaderBrush(ImageShader(tile, TileMode.Repeated, TileMode.Repeated))
    onDrawBehind {
        drawRect(grain)
    }
}

// Matches AlbumScreen's cover squircle radius exactly (see item 5: one shared cover border
// language across the app) — also the END radius the bridge overlay's clip lerps toward.
internal val IrideCoverBorderRadius = 12.dp

// How much of the expand/collapse `eased` progress the bridge overlay's ring gets before it's
// gone — short on purpose, "quasi immediatamente": the ring's job is just to bridge the mini
// pill's own ring into the move, not to survive the whole morph.
internal const val BridgeRingFadeOutProgress = 0.12f
internal val BridgeRingStrokeWidth = 2.5.dp

/**
 * Same technique as PillProgressDrawCache, minus its size/shape cache — this only ever draws for
 * [BridgeRingFadeOutProgress] worth of a single expand/collapse, so recomputing the traced outline
 * every frame is cheaper than the bookkeeping needed to cache it correctly.
 */
internal fun DrawScope.drawBridgeProgressRing(
    cornerRadiusPx: Float,
    strokeWidthPx: Float,
    progress: Float,
) {
    val inset = strokeWidthPx / 2f
    val path = Path().apply {
        addOutline(
            irideSquircle(cornerRadiusPx.toDp()).createOutline(
                Size((size.width - 2f * inset).coerceAtLeast(0f), (size.height - 2f * inset).coerceAtLeast(0f)),
                layoutDirection,
                this@drawBridgeProgressRing,
            ),
        )
        translate(Offset(inset, inset))
    }
    val stroke = Stroke(width = strokeWidthPx, cap = StrokeCap.Round)
    drawPath(path, color = Color.White.copy(alpha = 0.2f), style = stroke)
    if (progress > 0f) {
        val measure = PathMeasure().apply { setPath(path, false) }
        val segment = Path()
        measure.getSegment(0f, measure.length * progress.coerceIn(0f, 1f), segment, true)
        drawPath(segment, color = Color.White, style = stroke)
    }
}

internal fun irideSquircle(radius: Dp) = SquircleShape(radius = radius, cornerSmoothing = 0.48f)

/** Navigation uses one disclosure mark; playback symbols retain their media meaning. */
@androidx.compose.runtime.Composable
internal fun IrideMp3Disclosure(selected: Boolean = false, textScale: Float = 1f) {
    androidx.compose.material3.Text(
        text = "\u203A",
        color = if (selected) IrideMp3InfoActionColor else IrideMp3DimIconColor,
        fontSize = (24 * textScale).sp,
        modifier = androidx.compose.ui.Modifier.size(24.dp * textScale),
        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
    )
}
