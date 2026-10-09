package com.metrolist.music.ui.component

import kotlin.math.ceil
import kotlin.math.sqrt

internal data class AlbumArtworkPigment(
    val color: Int,
    val population: Int,
    val centerX: Float,
    val centerY: Float,
)

/** Rank visible accents before neutral paper/ink, keeping every pigment a real source pixel. */
internal fun albumArtworkPigments(
    pixels: IntArray,
    width: Int,
    height: Int,
    readableOnly: Boolean = false,
): List<AlbumArtworkPigment> {
    require(width > 0 && height > 0 && pixels.size == width * height)
    val population = IntArray(4096)
    val horizontal = LongArray(4096)
    val vertical = LongArray(4096)
    val colors = IntArray(4096)
    val distances = IntArray(4096) { Int.MAX_VALUE }
    var visible = 0
    pixels.forEachIndexed { index, pixel ->
        if (pixel ushr 24 < 128) return@forEachIndexed
        val red = (pixel ushr 16) and 255
        val green = (pixel ushr 8) and 255
        val blue = pixel and 255
        val bucket = (red shr 4 shl 8) or (green shr 4 shl 4) or (blue shr 4)
        population[bucket]++
        horizontal[bucket] += index % width
        vertical[bucket] += index / width
        visible++
        val dr = red - ((red and 0xF0) + 8)
        val dg = green - ((green and 0xF0) + 8)
        val db = blue - ((blue and 0xF0) + 8)
        val distance = dr * dr + dg * dg + db * db
        if (distance < distances[bucket]) {
            distances[bucket] = distance
            colors[bucket] = pixel or 0xFF000000.toInt()
        }
    }
    if (visible == 0) return emptyList()
    val minimumPopulation = ceil(visible * 0.008).toInt().coerceAtLeast(1)
    val candidates = population.indices.filter { population[it] >= minimumPopulation }.map { bucket ->
        AlbumArtworkPigment(
            colors[bucket], population[bucket],
            horizontal[bucket].toFloat() / population[bucket] / (width - 1).coerceAtLeast(1),
            vertical[bucket].toFloat() / population[bucket] / (height - 1).coerceAtLeast(1),
        )
    }.filter { !readableOnly || readableArtworkPigment(it.color) == it.color }
    fun channels(pigment: AlbumArtworkPigment): Pair<Int, Int> {
        val red = (pigment.color ushr 16) and 255
        val green = (pigment.color ushr 8) and 255
        val blue = pigment.color and 255
        return maxOf(red, green, blue) to minOf(red, green, blue)
    }
    val accents = candidates.filter {
        val (maximum, minimum) = channels(it)
        maximum >= 32 && maximum - minimum >= maximum * 0.18f
    }
    val eligible = accents.ifEmpty {
        candidates.filter { channels(it).first >= 32 }.ifEmpty { candidates }
    }
    return eligible.sortedByDescending {
        val (maximum, minimum) = channels(it)
        val saturation = if (maximum == 0) 0.0 else (maximum - minimum).toDouble() / maximum
        sqrt(it.population.toDouble()) * if (accents.isEmpty()) {
            (maximum / 255.0).coerceAtLeast(0.05)
        } else {
            0.65 + saturation * 0.35
        }
    }.take(4)
}
