package com.metrolist.music.data.remote

import java.net.URI

internal data class ArtistVideoResource(val url: String, val range: LongRange? = null)

internal data class ArtistVideoSegment(
    val initialization: ArtistVideoResource?,
    val media: ArtistVideoResource,
)

/** Only the first, unencrypted segment is needed to decode the artist's still portrait. */
internal fun firstArtistVideoSegment(playlist: String, playlistUrl: String): ArtistVideoSegment? {
    if (!playlist.trimStart().startsWith("#EXTM3U")) return null
    var initialization: ArtistVideoResource? = null
    var range: LongRange? = null
    var encrypted = false
    for (line in playlist.lineSequence().map(String::trim).filter(String::isNotEmpty)) {
        when {
            line.startsWith("#EXT-X-STREAM-INF:") || line == "#EXT-X-I-FRAMES-ONLY" -> return null
            line.startsWith("#EXT-X-KEY:") -> encrypted = !line.substringAfter(':').split(',').contains("METHOD=NONE")
            line.startsWith("#EXT-X-MAP:") -> {
                if (encrypted) return null
                val uri = Regex("URI=\"([^\"]+)\"").find(line)?.groupValues?.get(1) ?: return null
                val byteRange = Regex("BYTERANGE=\"([^\"]+)\"").find(line)?.groupValues?.get(1)
                val parsedRange = byteRange?.let { artistVideoByteRange(it) ?: return null }
                initialization = ArtistVideoResource(resolveArtistVideoUrl(playlistUrl, uri) ?: return null, parsedRange)
            }
            line.startsWith("#EXT-X-BYTERANGE:") -> range = artistVideoByteRange(line.substringAfter(':')) ?: return null
            !line.startsWith('#') -> {
                if (encrypted) return null
                return ArtistVideoSegment(initialization, ArtistVideoResource(resolveArtistVideoUrl(playlistUrl, line) ?: return null, range))
            }
        }
    }
    return null
}

private fun artistVideoByteRange(value: String): LongRange? {
    val parts = value.split('@')
    val length = parts.firstOrNull()?.toLongOrNull()?.takeIf { it > 0 } ?: return null
    // An implicit offset requires a previous segment, which a first-frame request does not have.
    val offset = parts.getOrNull(1)?.toLongOrNull()?.takeIf { it >= 0 } ?: return null
    if (parts.size != 2 || offset > Long.MAX_VALUE - length) return null
    return offset until offset + length
}

internal fun resolveArtistVideoUrl(baseUrl: String, reference: String): String? = runCatching {
    URI(baseUrl).resolve(reference).takeIf { it.scheme == "https" && it.host != null && it.userInfo == null }?.toString()
}.getOrNull()
