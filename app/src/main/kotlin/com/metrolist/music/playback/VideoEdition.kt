package com.metrolist.music.playback

import com.metrolist.innertube.models.SongItem
import com.metrolist.music.models.MediaMetadata
import java.text.Normalizer
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToLong

/** A video's favorite belongs to its matched audio track, never a stale match from another item. */
internal fun favoriteMetadataForEdition(
    current: MediaMetadata?,
    videoMediaId: String?,
    audio: MediaMetadata?,
): MediaMetadata? {
    current ?: return null
    if (current.id != videoMediaId) return current.takeUnless { it.isVideoSong }
    return audio?.takeUnless { it.isVideoSong }
}

internal fun proportionalVideoPosition(position: Long, sourceDuration: Long, targetDuration: Long): Long {
    if (targetDuration <= 0) return position.coerceAtLeast(0)
    if (sourceDuration <= 0) return position.coerceIn(0, targetDuration - 1)
    return ((position.coerceAtLeast(0).toDouble() / sourceDuration) * targetDuration)
        .roundToLong().coerceIn(0, targetDuration - 1)
}

private val editionNoise = setOf("official", "video", "audio", "lyrics", "hd", "4k")
private val editionAccents = Regex("\\p{M}")
private val editionSuffix = Regex("\\([^)]*(official|video|audio|lyrics)[^)]*\\)|\\[[^]]*(official|video|audio|lyrics)[^]]*]")
private val editionSeparator = Regex("[^\\p{L}\\p{N}]+")

private fun editionWords(text: String): Set<String> = Normalizer.normalize(text, Normalizer.Form.NFD)
    .replace(editionAccents, "").lowercase(Locale.ROOT)
    .replace(editionSuffix, " ").split(editionSeparator)
    .filter { it.isNotBlank() && it !in editionNoise }.toSet()

internal fun rankVideoEditions(source: MediaMetadata, candidates: List<SongItem>): List<SongItem> =
    rankEditions(source, candidates.filter { it.isVideoSong })

private fun rankEditions(source: MediaMetadata, candidates: List<SongItem>): List<SongItem> {
    val artists = source.artists.flatMap { editionWords(it.name) }.toSet()
    val originalTitle = editionWords(source.title)
    val title = (originalTitle - artists).ifEmpty { originalTitle }
    fun score(item: SongItem): Double {
        val words = editionWords(item.title)
        val artistWords = item.artists.flatMap { editionWords(it.name) }.toSet()
        val titleMatch = title.intersect(words).size.toDouble() / title.size.coerceAtLeast(1)
        val creditedArtistMatch = artists.intersect(artistWords).size.toDouble() / artists.size.coerceAtLeast(1)
        val titleArtistMatch = artists.intersect(words).size.toDouble() / artists.size.coerceAtLeast(1)
        val artistMatch = maxOf(creditedArtistMatch, titleArtistMatch * .6)
        val matchingArtistChannel = source.artists.mapNotNull { it.id }.toSet()
            .intersect(item.artists.mapNotNull { it.id }.toSet()).isNotEmpty()
        val duration = item.duration ?: 0
        val durationPenalty = if (source.duration > 0 && duration > 0) {
            (abs(source.duration.toLong() - duration).toDouble() / source.duration).coerceAtMost(1.0)
        } else 0.0
        val officialVideoBonus = when (item.musicVideoType) {
            "MUSIC_VIDEO_TYPE_OMV" -> if (matchingArtistChannel) 100.0 else 40.0
            "MUSIC_VIDEO_TYPE_UGC" -> 0.0
            else -> -100.0
        }
        val nonVisualTitlePenalty = if (Regex("\\b(lyrics?|audio|visualizer|karaoke)\\b", RegexOption.IGNORE_CASE).containsMatchIn(item.title)) 8.0 else 0.0
        return officialVideoBonus + titleMatch * 4 + artistMatch * 3 - durationPenalty - nonVisualTitlePenalty
    }
    return candidates.asSequence()
        .distinctBy { it.id }
        .sortedByDescending(::score)
        .toList()
}

/** Reject unrelated search hits rather than changing the song or seeding their radio. */
internal fun matchingAudioEdition(source: MediaMetadata, candidates: List<SongItem>): SongItem? {
    val artists = source.artists.flatMap { editionWords(it.name) }.toSet()
    val originalTitle = editionWords(source.title)
    val title = (originalTitle - artists).ifEmpty { originalTitle }
    if (title.isEmpty()) return null
    return rankEditions(source, candidates.filterNot { it.isVideoSong }).firstOrNull { candidate ->
        val words = editionWords(candidate.title)
        val credits = candidate.artists.flatMap { editionWords(it.name) }.toSet()
        val titleOverlap = title.intersect(words).size.toDouble() / title.size
        val artistOverlap = artists.intersect(credits + words).size.toDouble() / artists.size.coerceAtLeast(1)
        val duration = candidate.duration ?: 0
        val durationMatches = source.duration <= 0 || duration <= 0 ||
            abs(source.duration.toLong() - duration) <= maxOf(45L, source.duration / 4L)
        !candidate.isVideoSong && titleOverlap >= .75 &&
            (artists.isEmpty() || artistOverlap >= .5) && durationMatches
    }
}

fun youtubeVideoThumbnail(videoId: String, original: String? = null): String = original
    ?.takeIf { it.startsWith("https://i.ytimg.com/vi/$videoId/") || it.startsWith("https://i.ytimg.com/vi_webp/$videoId/") }
    ?.substringBefore('?')?.substringBefore('=') ?: "https://i.ytimg.com/vi/$videoId/hqdefault.jpg"
