package com.metrolist.music.utils.spotify

import java.text.Normalizer
import java.util.Locale
import kotlin.math.abs

/** Conservative matching keeps covers and alternate versions out of imported playlists. */
object SpotifyTrackMatcher {
    private val FEATURED_ARTISTS = Regex("(?i)\\s*[\\[(](?:feat\\.?|ft\\.?|with)\\s+[^\\])]+[\\])]")
    private val DIACRITICS = Regex("\\p{M}+")
    private val PUNCTUATION = Regex("[^\\p{L}\\p{N}]+")
    private val WHITESPACE = Regex("\\s+")
    private val VERSIONS = setOf("live", "acoustic", "instrumental", "karaoke", "remix", "sped", "slowed", "cover", "demo")

    fun cacheKey(track: SpotifyExportTrack) = listOf(
        normalized(track.title), track.artists.joinToString(";") { normalized(it) }, track.durationSeconds.toString(),
    ).joinToString("\u0000")

    fun score(
        source: SpotifyExportTrack,
        title: String,
        artists: List<String>,
        duration: Int?,
    ): Double? {
        if (source.artists.isEmpty()) return null
        val sourceTitle = normalizedTitle(source.title)
        val candidateTitle = normalizedTitle(title)
        val titleScore = similarity(sourceTitle, candidateTitle)
        if (titleScore < 0.8) return null
        val artistScore = artists.maxOfOrNull {
            similarity(normalized(source.artists.first()), normalized(it))
        } ?: return null
        if (artistScore < 0.8) return null
        if (sourceTitle.split(' ').filter { it in VERSIONS }.toSet() !=
            candidateTitle.split(' ').filter { it in VERSIONS }.toSet()) return null
        val expectedDuration = source.durationSeconds
        val durationDifference = expectedDuration?.let { expected -> duration?.let { abs(expected - it) } }
        if (durationDifference != null &&
            durationDifference > maxOf(12, minOf(20, (expectedDuration * 0.08).toInt()))) return null
        val durationScore = durationDifference?.let { 1.0 - it.coerceAtMost(20) / 20.0 } ?: 0.5
        return titleScore * 0.55 + artistScore * 0.35 + durationScore * 0.1
    }

    private fun normalizedTitle(value: String) = normalized(value.replace(FEATURED_ARTISTS, ""))

    private fun normalized(value: String) = Normalizer.normalize(value, Normalizer.Form.NFD)
        .replace(DIACRITICS, "").lowercase(Locale.ROOT).replace(PUNCTUATION, " ")
        .trim().replace(WHITESPACE, " ")

    private fun similarity(first: String, second: String): Double {
        if (first.isEmpty() || second.isEmpty()) return 0.0
        if (first == second) return 1.0
        val firstWords = first.split(' ').toSet()
        val secondWords = second.split(' ').toSet()
        return firstWords.intersect(secondWords).size.toDouble() / firstWords.union(secondWords).size
    }
}
