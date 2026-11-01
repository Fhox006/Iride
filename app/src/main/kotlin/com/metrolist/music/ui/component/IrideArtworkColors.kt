package com.metrolist.music.ui.component

import android.graphics.Bitmap
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import kotlin.math.roundToInt

internal fun Bitmap.irideAverageColor(fallback: Color): Color {
    val pixels = IntArray(width * height)
    getPixels(pixels, 0, width, 0, 0, width, height)
    var weight = 0.0
    var red = 0.0
    var green = 0.0
    var blue = 0.0
    pixels.forEach { pixel ->
        val alpha = (pixel ushr 24).toDouble()
        weight += alpha
        red += ((pixel ushr 16) and 255) * alpha
        green += ((pixel ushr 8) and 255) * alpha
        blue += (pixel and 255) * alpha
    }
    return if (weight == 0.0) fallback else Color(
        red = (red / weight / 255).toFloat(),
        green = (green / weight / 255).toFloat(),
        blue = (blue / weight / 255).toFloat(),
    )
}

internal fun Color.irideDimmed(factor: Float): Color = copy(
    red = red * factor,
    green = green * factor,
    blue = blue * factor,
)

internal fun Color.limitArtworkLuminance(maximum: Float): Color {
    if (luminance() <= maximum) return this
    var lower = 0f
    var upper = 1f
    // Scale RGB channels together: only brightness changes, never hue or saturation.
    repeat(12) {
        val fraction = (lower + upper) / 2f
        if (irideDimmed(fraction).luminance() <= maximum) {
            lower = fraction
        } else {
            upper = fraction
        }
    }
    return irideDimmed(lower)
}

internal fun Bitmap.irideArtworkTint(fallback: Color): Color {
    return irideAlbumArtworkPigments().firstOrNull()?.color?.let { Color(it) } ?: fallback
}

internal fun Bitmap.irideAlbumArtworkPigments(readableOnly: Boolean = false): List<AlbumArtworkPigment> {
    val scale = (128f / maxOf(width, height)).coerceAtMost(1f)
    val sample = Bitmap.createScaledBitmap(
        this, (width * scale).roundToInt().coerceAtLeast(1),
        (height * scale).roundToInt().coerceAtLeast(1), false,
    )
    return try {
        val pixels = IntArray(sample.width * sample.height)
        sample.getPixels(pixels, 0, sample.width, 0, 0, sample.width, sample.height)
        albumArtworkPigments(pixels, sample.width, sample.height, readableOnly)
    } finally {
        if (sample !== this) sample.recycle()
    }
}

internal fun Bitmap.irideArtistArtworkTint(imageAspectRatio: Float): Color {
    val scale = (128f / maxOf(width, height)).coerceAtMost(1f)
    val sample = Bitmap.createScaledBitmap(
        this,
        (width * scale).roundToInt().coerceAtLeast(1),
        (height * scale).roundToInt().coerceAtLeast(1),
        false,
    )
    val visibleWidth = minOf(sample.width, (sample.height * imageAspectRatio).roundToInt().coerceAtLeast(1))
    val visibleHeight = minOf(sample.height, (sample.width / imageAspectRatio).roundToInt().coerceAtLeast(1))
    // Match the centered crop, then look for background colors along both side edges.
    val visible = Bitmap.createBitmap(
        sample,
        (sample.width - visibleWidth) / 2,
        (sample.height - visibleHeight) / 2,
        visibleWidth,
        visibleHeight,
    )
    return try {
        val pixels = IntArray(visibleWidth * visibleHeight)
        visible.getPixels(pixels, 0, visibleWidth, 0, 0, visibleWidth, visibleHeight)
        Color(artistBackgroundColor(pixels, visibleWidth, visibleHeight))
    } finally {
        if (visible !== this && visible !== sample) visible.recycle()
        if (sample !== this) sample.recycle()
    }
}
