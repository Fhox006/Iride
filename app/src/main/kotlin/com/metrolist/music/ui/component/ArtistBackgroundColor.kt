package com.metrolist.music.ui.component

import kotlin.math.roundToInt

private fun isArtistBackgroundPigment(pixel: Int): Boolean =
    pixel ushr 24 >= 128

private fun artistColorGroup(pixel: Int): Int {
    val red = (pixel ushr 16) and 255
    val green = (pixel ushr 8) and 255
    val blue = pixel and 255
    val maximum = maxOf(red, green, blue)
    val minimum = minOf(red, green, blue)
    val delta = maximum - minimum
    val saturation = if (maximum == 0) 0f else delta.toFloat() / maximum
    if (maximum < 8 || saturation < 0.18f) return maximum / 64
    val hue = when (maximum) {
        red -> (green - blue).toFloat() / delta
        green -> (blue - red).toFloat() / delta + 2f
        else -> (red - green).toFloat() / delta + 4f
    }.let { if (it < 0f) it + 6f else it }
    return 4 + (hue * 4f).roundToInt() % 24 * 3 + (saturation * 3f).toInt().coerceAtMost(2)
}

private fun artistRgbBucket(pixel: Int): Int =
    ((pixel ushr 12) and 0xF00) or ((pixel ushr 8) and 0xF0) or ((pixel ushr 4) and 0xF)

/** Find an edge-supported background pigment without turning black clothing into the backdrop. */
internal fun artistBackgroundColor(pixels: IntArray, width: Int, height: Int): Int {
    require(width > 0 && height > 0 && pixels.size == width * height)
    val population = IntArray(76)
    val edgePopulation = IntArray(76)
    val edgeWeight = DoubleArray(76)
    var visiblePixels = 0
    var edgePixels = 0
    fun edge(index: Int): Boolean {
        val x = index % width
        return x < width * 0.12f || x >= width * 0.88f
    }
    fun verticalWeight(index: Int): Double = 0.25 + 0.75 * (index / width).toDouble() / (height - 1).coerceAtLeast(1)

    pixels.forEachIndexed { index, pixel ->
        if (pixel ushr 24 < 128) return@forEachIndexed
        visiblePixels++
        if (edge(index)) edgePixels++
        if (!isArtistBackgroundPigment(pixel)) return@forEachIndexed
        val group = artistColorGroup(pixel)
        population[group]++
        if (edge(index)) {
            edgePopulation[group]++
            edgeWeight[group] += verticalWeight(index)
        }
    }
    if (visiblePixels == 0) return 0xFF000000.toInt()
    // Require meaningful coverage, rather than letting a bright detail or skin in the
    // center win. Lower side edges count more, but black clothing does not veto a backdrop.
    val candidates = population.indices.filter {
        population[it] >= visiblePixels * 0.06 && edgePopulation[it] >= edgePixels * 0.15
    }
    // Detailed scenery still supplies a pigment: never discard a photo's color for being a midtone.
    val selectedGroup = (candidates.ifEmpty { population.indices.filter { population[it] > 0 } })
        .maxBy { edgeWeight[it] * 4.0 + population[it] * 0.1 }
    val bucketWeight = DoubleArray(4096)
    val redSum = DoubleArray(4096)
    val greenSum = DoubleArray(4096)
    val blueSum = DoubleArray(4096)
    pixels.forEachIndexed { index, pixel ->
        if (!isArtistBackgroundPigment(pixel) || artistColorGroup(pixel) != selectedGroup) return@forEachIndexed
        val bucket = artistRgbBucket(pixel)
        val weight = 1.0 + if (edge(index)) 4.0 * verticalWeight(index) else 0.0
        bucketWeight[bucket] += weight
        redSum[bucket] += ((pixel ushr 16) and 255) * weight
        greenSum[bucket] += ((pixel ushr 8) and 255) * weight
        blueSum[bucket] += (pixel and 255) * weight
    }
    val selectedBucket = bucketWeight.indices.maxBy { bucketWeight[it] }
    val red = redSum[selectedBucket] / bucketWeight[selectedBucket]
    val green = greenSum[selectedBucket] / bucketWeight[selectedBucket]
    val blue = blueSum[selectedBucket] / bucketWeight[selectedBucket]
    var result = 0xFF000000.toInt()
    var nearest = Double.POSITIVE_INFINITY
    pixels.forEach { pixel ->
        if (!isArtistBackgroundPigment(pixel) || artistColorGroup(pixel) != selectedGroup ||
            artistRgbBucket(pixel) != selectedBucket) return@forEach
        val dr = ((pixel ushr 16) and 255) - red
        val dg = ((pixel ushr 8) and 255) - green
        val db = (pixel and 255) - blue
        val distance = dr * dr + dg * dg + db * db
        if (distance < nearest) {
            nearest = distance
            result = pixel or 0xFF000000.toInt()
        }
    }
    return result
}
