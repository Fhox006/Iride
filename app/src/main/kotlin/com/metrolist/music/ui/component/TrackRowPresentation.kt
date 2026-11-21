package com.metrolist.music.ui.component

import androidx.media3.exoplayer.offline.Download
import com.metrolist.music.utils.TitleFeaturingParser

internal enum class TrackDownloadStatus {
    NOT_DOWNLOADED,
    QUEUED,
    DOWNLOADING,
    DOWNLOADED,
    FAILED,
    REMOVING,
}

internal fun trackDownloadStatus(state: Int?): TrackDownloadStatus =
    when (state) {
        Download.STATE_QUEUED -> TrackDownloadStatus.QUEUED
        Download.STATE_DOWNLOADING -> TrackDownloadStatus.DOWNLOADING
        Download.STATE_COMPLETED -> TrackDownloadStatus.DOWNLOADED
        Download.STATE_FAILED -> TrackDownloadStatus.FAILED
        Download.STATE_REMOVING -> TrackDownloadStatus.REMOVING
        else -> TrackDownloadStatus.NOT_DOWNLOADED
    }

internal fun cleanTrackTitle(title: String): String =
    TitleFeaturingParser.extract(title)?.first ?: title

internal fun formatTrackArtistCredits(artistNames: Iterable<String>, title: String? = null): String {
    val parsedFeatures = title?.let(TitleFeaturingParser::extract)?.second.orEmpty()
    val names =
        (artistNames + parsedFeatures)
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinctBy { it.lowercase() }
    if (names.isEmpty()) return ""

    val parsedFeatureKeys = parsedFeatures.map { it.trim().lowercase() }.toSet()
    val primaryArtist = names.firstOrNull { it.lowercase() !in parsedFeatureKeys }
    val features =
        if (primaryArtist == null) names
        else names.filterNot { it.equals(primaryArtist, ignoreCase = true) }
    val primary = primaryArtist.orEmpty()
    return when {
        features.isEmpty() -> primary
        primary.isEmpty() -> "feat. ${features.joinToString(", ")}"
        else -> "$primary feat. ${features.joinToString(", ")}"
    }
}

internal fun formatAlbumTrackArtistCredits(
    trackArtistNames: Iterable<String>,
    albumArtistNames: Iterable<String>,
    title: String? = null,
): String {
    val albumArtistKeys = albumArtistNames.map { it.trim().lowercase() }.filter(String::isNotEmpty).toSet()
    val parsedFeatures = title?.let(TitleFeaturingParser::extract)?.second.orEmpty()
    val featuredArtists =
        (trackArtistNames.drop(1) + parsedFeatures)
            .map { it.trim() }
            .filter { it.isNotEmpty() && it.lowercase() !in albumArtistKeys }
            .distinctBy { it.lowercase() }

    return if (featuredArtists.isEmpty()) "" else "feat. ${featuredArtists.joinToString(", ")}"
}
