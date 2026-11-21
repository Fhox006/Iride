package com.metrolist.music.discovery

/** Keep usable hero resolution ahead of face preference; rank native pixels, not previews. */
internal data class VideoArtworkQuality(val width: Int, val height: Int, val faceQuality: Float) :
    Comparable<VideoArtworkQuality> {
    override fun compareTo(other: VideoArtworkQuality): Int = compareValuesBy(
        this, other,
        { it.width >= 480 && it.height >= 270 },
        { it.faceQuality > 0f },
        { it.width.toLong() * it.height },
        { it.faceQuality },
    )
}
