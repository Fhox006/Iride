package com.metrolist.music.ui.component

import com.metrolist.music.data.remote.ArtistArtworkSource
import kotlin.math.pow
import kotlin.math.sqrt

internal const val ArtistArtworkResolution = 1200

/** Compare median colors in four perimeter patches, rather than isolated noisy corner pixels. */
internal fun artistArtworkHasUniformCorners(pixels: IntArray, width: Int, height: Int): Boolean =
    artistArtworkCornerDistance(pixels, width, height) <= 0.17

internal fun artistArtworkCornerDistance(pixels: IntArray, width: Int, height: Int): Double {
    require(width > 0 && height > 0 && pixels.size == width * height)
    val patchWidth = (width / 10).coerceAtLeast(1)
    val patchHeight = (height / 10).coerceAtLeast(1)
    val corners = listOf(0 to 0, width - patchWidth to 0, 0 to height - patchHeight,
        width - patchWidth to height - patchHeight).map { (left, top) ->
        val samples = (top until top + patchHeight).flatMap { y ->
            (left until left + patchWidth).map { x -> pixels[y * width + x] }
        }
        val opaque = samples.filter { it ushr 24 >= 240 }
        if (opaque.size < samples.size * 0.8) return 1.0
        fun median(shift: Int): Double = opaque.map { (it ushr shift) and 255 }.sorted()[opaque.size / 2] / 255.0
        fun linear(value: Double): Double = if (value <= 0.04045) value / 12.92 else ((value + 0.055) / 1.055).pow(2.4)
        val r = linear(median(16))
        val g = linear(median(8))
        val b = linear(median(0))
        val l = Math.cbrt(0.4122214708 * r + 0.5363325363 * g + 0.0514459929 * b)
        val m = Math.cbrt(0.2119034982 * r + 0.6806995451 * g + 0.1073969566 * b)
        val s = Math.cbrt(0.0883024619 * r + 0.2817188376 * g + 0.6299787005 * b)
        doubleArrayOf(0.2104542553 * l + 0.7936177850 * m - 0.0040720468 * s,
            1.9779984951 * l - 2.4285922050 * m + 0.4505937099 * s,
            0.0259040371 * l + 0.7827717662 * m - 0.8086757660 * s)
    }
    // Oklab keeps the threshold perceptual across both dark and bright backdrops.
    return corners.indices.maxOf { i -> (i + 1 until corners.size).maxOfOrNull { j ->
        sqrt((0..2).sumOf { (corners[i][it] - corners[j][it]).pow(2) })
    } ?: 0.0 }
}

internal data class ArtistArtworkQuality(
    val portrait: Float,
    val borders: Float,
    val definition: Float,
    val blend: Float = 1f,
    val photographic: Float = 1f,
) {
    val portraitScore: Float get() = portrait.coerceIn(0f, 1f) * photographic.coerceIn(0f, 1f)
    val borderScore: Float get() = minOf(borders, blend).coerceIn(0f, 1f)
    val definitionScore: Float get() = definition.coerceIn(0f, 1f) * (0.5f + 0.5f * photographic.coerceIn(0f, 1f))
    val score: Float get() = portraitScore * 0.20f + borderScore * 0.40f + definitionScore * 0.40f

    val technicalScore: Float get() = (borderScore + definitionScore) * 0.5f
    val isGoodPortrait: Boolean get() = portraitScore >= 0.55f && borderScore >= 0.65f &&
        definitionScore >= 0.55f && photographic >= 0.55f
}

private fun artistArtworkSelectionScore(source: ArtistArtworkSource, quality: ArtistArtworkQuality): Float {
    val preference = when (source) {
        // Apple contributes up to half the score; a missing face reduces, but never removes, its preference.
        ArtistArtworkSource.APPLE_MUSIC -> 0.30f + 0.20f * quality.portraitScore
        ArtistArtworkSource.SPOTIFY -> 0.35f
        ArtistArtworkSource.YOUTUBE_MUSIC -> 0.25f
    }
    return preference + quality.score * 0.5f
}

private fun canReplaceAppleArtwork(apple: ArtistArtworkQuality, alternative: ArtistArtworkQuality): Boolean {
    if (alternative.borderScore < 0.65f || alternative.definitionScore < 0.55f || alternative.photographic < 0.55f) return false
    // A better score alone is insufficient: Apple must have a substantial visible defect.
    val hasTechnicalDefect = apple.borderScore < 0.40f || apple.definitionScore < 0.40f
    val technicallyBetter = hasTechnicalDefect && alternative.technicalScore > apple.technicalScore + 0.20f
    val replacesWeakNonPortrait = apple.portraitScore < 0.25f &&
        (apple.definition < 0.45f || apple.photographic < 0.45f || apple.blend < 0.5f) &&
        alternative.isGoodPortrait && alternative.definitionScore >= 0.60f
    return technicallyBetter || replacesWeakNonPortrait
}

internal fun artistPortraitQuality(confidence: Float, eyesFraction: Float, centerX: Float, centerY: Float): Float {
    if (!confidence.isFinite() || !eyesFraction.isFinite() || !centerX.isFinite() || !centerY.isFinite() ||
        confidence < 0.4f || eyesFraction !in 0.05f..0.35f || centerX !in 0f..1f || centerY !in 0f..1f) return 0f
    val left = centerX - eyesFraction * 1.3f
    val right = centerX + eyesFraction * 1.3f
    val top = centerY - eyesFraction
    val bottom = centerY + eyesFraction * 2f
    val coverage = ((minOf(right, 1f) - maxOf(left, 0f)).coerceAtLeast(0f) *
        (minOf(bottom, 1f) - maxOf(top, 0f)).coerceAtLeast(0f)) /
        ((right - left) * (bottom - top))
    if (coverage < 0.75f) return 0f
    // Ignore tiny faces in promotional artwork; prefer a clear, substantially visible head.
    val prominence = ((eyesFraction - 0.04f) / 0.08f).coerceIn(0f, 1f)
    return confidence.coerceIn(0f, 1f) * prominence * coverage
}

internal fun artistDefinitionQuality(pixels: IntArray, width: Int, height: Int, nativeSide: Int): Float {
    require(width > 0 && height > 0 && pixels.size == width * height)
    fun luminance(pixel: Int): Float = ((pixel ushr 16) and 255) * 0.2126f +
        ((pixel ushr 8) and 255) * 0.7152f + (pixel and 255) * 0.0722f
    var detail = 0f
    var count = 0
    for (y in height / 6 until height * 5 / 6) for (x in width / 6 until width * 5 / 6) {
        val current = luminance(pixels[y * width + x])
        if (x + 1 < width) { detail += kotlin.math.abs(current - luminance(pixels[y * width + x + 1])); count++ }
        if (y + 1 < height) { detail += kotlin.math.abs(current - luminance(pixels[(y + 1) * width + x])); count++ }
    }
    val detailQuality = (detail / count.coerceAtLeast(1) / 8f).coerceIn(0f, 1f)
    // Use the decoded input size before normalization so enlarging a small source earns no HD bonus.
    val resolution = sqrt((nativeSide.toFloat() / ArtistArtworkResolution).coerceIn(0f, 1f))
    return resolution * (0.35f + 0.65f * detailQuality)
}

internal fun <T> preferredArtistArtwork(
    candidates: List<T>,
    source: (T) -> ArtistArtworkSource,
    uniformCorners: (T) -> Boolean,
    quality: (T) -> ArtistArtworkQuality = { ArtistArtworkQuality(0f, if (uniformCorners(it)) 1f else 0f, 1f) },
    incumbent: T? = null,
): T? {
    // An established alternative has already used its one replacement opportunity.
    if (incumbent != null && source(incumbent) !in
        setOf(ArtistArtworkSource.APPLE_MUSIC, ArtistArtworkSource.SPOTIFY)) return incumbent
    val available = (candidates + listOfNotNull(incumbent))
        .distinct()
        .filter { source(it) != ArtistArtworkSource.SPOTIFY && uniformCorners(it) }
    // A well-defined portrait with a clean transition is a concrete reason to reject flat graphics.
    val acceptable = if (available.any { quality(it).isGoodPortrait }) {
        available.filter { quality(it).photographic >= 0.45f }
    } else available
    if (acceptable.isEmpty()) return (candidates + listOfNotNull(incumbent))
        .firstOrNull { source(it) == ArtistArtworkSource.YOUTUBE_MUSIC }
    val apple = acceptable.filter { source(it) == ArtistArtworkSource.APPLE_MUSIC }.maxByOrNull { quality(it).score }
    val appleQuality = apple?.let(quality)
    val appleScore = appleQuality?.let { artistArtworkSelectionScore(ArtistArtworkSource.APPLE_MUSIC, it) }
    // Keep Apple Music as the stable first choice whenever it has a valid artist portrait.
    // Other providers are fallbacks only; score-based switching made the profile photo change
    // between sources as candidates arrived or quality caches were restored.
    val eligible = if (apple != null) listOf(apple) else acceptable
    val scores = eligible.map { it to artistArtworkSelectionScore(source(it), quality(it)) }
    val bestScore = scores.maxOf { it.second }
    scores.firstOrNull { it.first == incumbent }?.let {
        // Keep a valid existing photo through small score differences or temporary provider failures.
        if (bestScore - it.second <= 0.06f) return it.first
    }
    // Keep service order for near-ties after requiring concrete reasons to replace Apple's photo.
    return scores.filter { bestScore - it.second <= 0.025f }.minByOrNull { source(it.first).ordinal }?.first
}
