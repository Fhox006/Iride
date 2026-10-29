/**
 * SPDX-License-Identifier: GPL-3.0-only
 * Adapted from SimpMusic/core, AMHlsVariant.kt, by SimpMusic contributors.
 * Source: https://github.com/maxrave-dev/core/blob/2894e549763b8a68225b4456d3290f3227bdbb01/service/lyricsService/src/commonMain/kotlin/org/simpmusic/lyrics/am/AMHlsVariant.kt
 * Iride changes: package and internal visibility.
 */
package com.metrolist.music.data.remote

/**
 * One `#EXT-X-STREAM-INF` entry of an animated-artwork master playlist.
 */
internal data class AMHlsVariant(
    val uri: String,
    val width: Int,
    val height: Int,
    val bandwidth: Int,
    val codecs: String,
) {
    /** True for H.264. Every non-`avc1` rendition Apple ships here is HEVC **Main 10**, i.e. 10-bit. */
    val isH264: Boolean get() = codecs.startsWith("avc1")
}

private val STREAM_INF = Regex("""#EXT-X-STREAM-INF:([^\n]*)\r?\n(\S+)""")
private val RESOLUTION = Regex("""RESOLUTION=(\d+)x(\d+)""")
private val CODECS = Regex("CODECS=\"([^\"]+)\"")
private val AVERAGE_BANDWIDTH = Regex("""AVERAGE-BANDWIDTH=(\d+)""")

// Anchored so it cannot match the tail of AVERAGE-BANDWIDTH= or _AVG-BANDWIDTH=.
private val BANDWIDTH = Regex("""(?:^|,)BANDWIDTH=(\d+)""")

/**
 * Parse the variants out of a master playlist. `#EXT-X-I-FRAME-STREAM-INF` lines are ignored: they
 * are trick-play thumbnails, not playable renditions.
 */
internal fun parseAMHlsVariants(
    masterPlaylist: String,
    masterUrl: String,
): List<AMHlsVariant> =
    STREAM_INF
        .findAll(masterPlaylist)
        .mapNotNull { match ->
            val attributes = match.groupValues[1]
            val resolution = RESOLUTION.find(attributes) ?: return@mapNotNull null
            val codecs = CODECS.find(attributes)?.groupValues?.get(1) ?: return@mapNotNull null
            val bandwidth =
                (
                    AVERAGE_BANDWIDTH.find(attributes)
                        ?: BANDWIDTH.find(attributes)
                )?.groupValues?.last()?.toIntOrNull() ?: return@mapNotNull null
            AMHlsVariant(
                uri = resolveAgainst(masterUrl, match.groupValues[2]),
                width = resolution.groupValues[1].toInt(),
                height = resolution.groupValues[2].toInt(),
                bandwidth = bandwidth,
                codecs = codecs,
            )
        }.toList()

/**
 * Pick the rendition to actually play.
 *
 * Codec is decided before quality, and it is the decision that matters: the ladder interleaves
 * H.264 and HEVC **Main 10**, so choosing by bitrate alone lands on a 10-bit stream at some
 * thresholds and an 8-bit one at others, for the same number. Animated artwork is decoded in
 * software, where 10-bit HEVC is far more expensive than H.264 for a decorative loop.
 *
 * After that: the narrowest rendition that still covers [minWidth] — enough to look right, nothing
 * spent past it — and the cheapest of those when several share a width. Bitrate cannot lead here
 * either, because it does not track resolution: on one measured ladder `486x648` costs 1516 kbps
 * while the larger `664x886` costs 1494.
 */
internal fun List<AMHlsVariant>.pickAMRendition(minWidth: Int): AMHlsVariant? {
    if (isEmpty()) return null
    val pool = filter { it.isH264 }.ifEmpty { this }
    val wideEnough = pool.filter { it.width >= minWidth }
    val targetWidth =
        if (wideEnough.isNotEmpty()) {
            wideEnough.minOf { it.width }
        } else {
            // Nothing reaches the target, so the largest available is the closest we get.
            pool.maxOf { it.width }
        }
    return pool.filter { it.width == targetWidth }.minByOrNull { it.bandwidth }
}

/** Master playlists observed so far carry absolute URIs; relative ones are still resolved. */
private fun resolveAgainst(
    baseUrl: String,
    uri: String,
): String =
    when {
        uri.startsWith("http://") || uri.startsWith("https://") -> uri
        uri.startsWith("/") -> {
            val schemeEnd = baseUrl.indexOf("://").takeIf { it >= 0 }?.plus(3) ?: 0
            val root = baseUrl.indexOf('/', schemeEnd).takeIf { it >= 0 } ?: baseUrl.length
            baseUrl.substring(0, root) + uri
        }
        else -> baseUrl.substringBeforeLast('/', "") + "/" + uri
    }
