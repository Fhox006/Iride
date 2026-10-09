package com.metrolist.music.ui.component

import kotlin.math.sqrt

/** Tonal detail separates flat, high-contrast graphics from photos without penalizing grayscale itself. */
internal fun artistPhotographicQuality(pixels: IntArray, width: Int, height: Int): Float {
    require(width > 0 && height > 0 && pixels.size == width * height)
    val tones = IntArray(32)
    var count = 0
    for (y in height / 6 until (height * 5 / 6).coerceAtLeast(1)) {
        for (x in width / 6 until (width * 5 / 6).coerceAtLeast(1)) {
            val pixel = pixels[y * width + x]
            if (pixel ushr 24 < 240) continue
            val luminance = ((pixel ushr 16) and 255) * 0.2126f +
                ((pixel ushr 8) and 255) * 0.7152f + (pixel and 255) * 0.0722f
            tones[(luminance.toInt() / 8).coerceIn(0, 31)]++
            count++
        }
    }
    if (count == 0) return 0f
    val significantTones = tones.count { it >= maxOf(1f, count * 0.01f) }
    val ordered = tones.sortedDescending()
    val dominantFraction = (ordered[0] + ordered[1]).toFloat() / count
    val richness = (significantTones / 12f).coerceIn(0f, 1f)
    val distribution = ((1f - dominantFraction) / 0.30f).coerceIn(0f, 1f)
    return richness * distribution
}

/** Assess the actual blurred lower band against its page tint across the full width, including the center. */
internal fun artistArtworkBlendQuality(blurredPixels: IntArray, width: Int, height: Int, tint: Int): Float {
    require(width > 0 && height > 0 && blurredPixels.size == width * height)
    val previous = FloatArray(width * 3)
    var edgeDistance = 0f
    var edgeCount = 0
    var largestStep = 0f
    val start = (height * 0.80f).toInt().coerceAtMost(height - 1)
    val tintRed = (tint ushr 16) and 255
    val tintGreen = (tint ushr 8) and 255
    val tintBlue = tint and 255
    for (y in start until height) {
        val t = (y - start).toFloat() / (height - 1 - start).coerceAtLeast(1)
        val remaining = 1f - t * t * t * (t * (t * 6f - 15f) + 10f)
        for (x in 0 until width) {
            val pixel = blurredPixels[y * width + x]
            if (pixel ushr 24 < 240) return 0f
            val red = (((pixel ushr 16) and 255) - tintRed) / 255f
            val green = (((pixel ushr 8) and 255) - tintGreen) / 255f
            val blue = ((pixel and 255) - tintBlue) / 255f
            val distance = sqrt(red * red * 0.2126f + green * green * 0.7152f + blue * blue * 0.0722f)
            if (x < maxOf(1, width / 8) || x >= width - maxOf(1, width / 8)) {
                edgeDistance += distance * remaining
                edgeCount++
            }
            // Evaluate the entire composited transition. Dark clothing may differ from the page
            // pigment while still fading smoothly; color equality is required mainly at the sides.
            val index = x * 3
            val compositedRed = red * remaining
            val compositedGreen = green * remaining
            val compositedBlue = blue * remaining
            if (y > start) {
                val dr = compositedRed - previous[index]
                val dg = compositedGreen - previous[index + 1]
                val db = compositedBlue - previous[index + 2]
                largestStep = maxOf(largestStep, sqrt(dr * dr * 0.2126f + dg * dg * 0.7152f + db * db * 0.0722f))
            }
            previous[index] = compositedRed
            previous[index + 1] = compositedGreen
            previous[index + 2] = compositedBlue
        }
    }
    val sideMismatch = edgeDistance / edgeCount.coerceAtLeast(1)
    return (1f - sideMismatch * 0.6f / 0.40f - largestStep * 0.4f / 0.30f).coerceIn(0f, 1f)
}
