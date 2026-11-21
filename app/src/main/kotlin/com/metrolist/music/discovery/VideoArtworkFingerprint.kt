package com.metrolist.music.discovery

import kotlin.math.abs

/** A small RGB sample tolerates JPEG/resolution differences without equating different scenes. */
internal class VideoArtworkFingerprint private constructor(private val pixels: IntArray) {
    fun resembles(other: VideoArtworkFingerprint): Boolean {
        var difference = 0L
        var changed = 0
        for (i in pixels.indices) {
            val a = pixels[i]
            val b = other.pixels[i]
            val delta = abs((a shr 16 and 255) - (b shr 16 and 255)) +
                abs((a shr 8 and 255) - (b shr 8 and 255)) + abs((a and 255) - (b and 255))
            difference += delta
            if (delta > 60) changed++
        }
        return difference <= pixels.size * 3L * 9 && changed <= pixels.size / 12
    }

    companion object {
        fun create(width: Int, height: Int, pixel: (Int, Int) -> Int): VideoArtworkFingerprint? {
            if (width < 64 || height < 40) return null
            val pixels = IntArray(32 * 24) { index ->
                pixel(((index % 32 + 0.5) * width / 32).toInt().coerceAtMost(width - 1),
                    ((index / 32 + 0.5) * height / 24).toInt().coerceAtMost(height - 1))
            }
            val luminance = pixels.map { ((it shr 16 and 255) * 299 + (it shr 8 and 255) * 587 + (it and 255) * 114) / 1000 }
            val mean = luminance.average()
            // Reject blank/missing-thumbnail placeholders and near-solid frames.
            if (luminance.sumOf { (it - mean) * (it - mean) } / pixels.size < 36) return null
            return VideoArtworkFingerprint(pixels)
        }
    }
}

internal fun hasChangingVideoFrames(frames: List<VideoArtworkFingerprint>): Boolean =
    frames.size == 3 && frames.drop(1).any { !frames.first().resembles(it) }
