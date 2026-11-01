package com.metrolist.music.discovery

internal object MixPosterPolicy {
    const val WEEK_MS = 7 * 24 * 60 * 60 * 1000L
    const val MIN_AVERAGE_LUMINANCE = 0.40f

    fun needsRefresh(fetchedAt: Long, now: Long): Boolean =
        fetchedAt <= 0 || now - fetchedAt >= WEEK_MS

    fun averageLuminance(pixels: IntArray): Float? {
        if (pixels.isEmpty()) return null
        val averageLuminance = pixels.sumOf { pixel ->
            val red = (pixel ushr 16) and 0xFF
            val green = (pixel ushr 8) and 0xFF
            val blue = pixel and 0xFF
            (red * 0.299 + green * 0.587 + blue * 0.114) / 255.0
        } / pixels.size
        return averageLuminance.toFloat()
    }

    fun isPhotoBrightEnough(pixels: IntArray): Boolean =
        averageLuminance(pixels)?.let { it >= MIN_AVERAGE_LUMINANCE } ?: false

    fun isPhotoBrightEnough(luminance: Float): Boolean = luminance >= MIN_AVERAGE_LUMINANCE

    fun isBrighterThan(candidate: Float, current: Float?): Boolean =
        current == null || candidate > current

    fun isArtistSource(source: MixPosterPhotoSource, photoUrl: String?, mixThumbnail: String?): Boolean =
        source == MixPosterPhotoSource.ARTIST &&
            (mixThumbnail.isNullOrBlank() || photoUrl != mixThumbnail)

    fun chooseArtist(candidates: List<String>, used: Set<String>, previous: String?, rotate: Boolean): String? {
        val distinct = candidates.distinct()
        val preferred = if (rotate) distinct.filter { it != previous }.ifEmpty { distinct } else distinct
        return preferred.firstOrNull { it !in used }
            ?: distinct.firstOrNull { it !in used }
            ?: preferred.firstOrNull()
    }

    fun artistAttempts(candidates: List<String>, used: Set<String>, previous: String?, rotate: Boolean): List<String> {
        val distinct = candidates.distinct()
        val preferred = if (rotate) distinct.filter { it != previous } + distinct.filter { it == previous }
            else distinct
        return preferred.filterNot { it in used } + preferred.filter { it in used }
    }

    fun needsPhotoRetry(source: MixPosterPhotoSource): Boolean = source != MixPosterPhotoSource.ARTIST
}
