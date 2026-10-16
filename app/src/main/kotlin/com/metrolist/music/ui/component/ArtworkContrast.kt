package com.metrolist.music.ui.component

import kotlin.math.pow
import kotlin.math.roundToInt

private fun linearArtworkChannel(value: Double): Double =
    if (value <= 0.04045) value / 12.92 else ((value + 0.055) / 1.055).pow(2.4)

internal fun artworkLuminance(pixel: Int, attenuation: Double = 1.0): Double =
    0.2126 * linearArtworkChannel(((pixel ushr 16) and 255) / 255.0 * attenuation) +
        0.7152 * linearArtworkChannel(((pixel ushr 8) and 255) / 255.0 * attenuation) +
        0.0722 * linearArtworkChannel((pixel and 255) / 255.0 * attenuation)

internal fun isDarkArtworkPigment(pixel: Int): Boolean =
    pixel ushr 24 >= 128 && artworkLuminance(pixel) <= 0.10

/** Reserve headroom for black grain and subtle decoration; do not round a failing ratio up. */
internal fun artworkUsesDarkInk(pixel: Int): Boolean =
    pixel ushr 24 >= 128 && (artworkLuminance(pixel, attenuation = 0.90) + 0.05) / 0.05 >= 7.0

/** Choose the stronger WCAG contrast without discarding the artist's original pigment. */
internal fun artistArtworkUsesDarkInk(pixel: Int): Boolean {
    val luminance = artworkLuminance(pixel)
    return (luminance + 0.05) / 0.05 >= 1.05 / (luminance + 0.05)
}

/** Keep the pigment, adjusting brightness only when secondary ink would be hard to read. */
internal fun readableArtistArtworkPigment(pixel: Int): Int {
    val darkInk = artistArtworkUsesDarkInk(pixel)
    val target = if (darkInk) 255 else 0
    fun blend(fraction: Double): Int = (0..2).fold(0xFF000000.toInt()) { color, channel ->
        val shift = channel * 8
        val value = (pixel ushr shift) and 255
        color or ((value + (target - value) * fraction).roundToInt() shl shift)
    }
    fun readable(background: Int): Boolean {
        val luminance = artworkLuminance(background)
        val inkLuminance = artworkLuminance((0..2).fold(0xFF000000.toInt()) { color, channel ->
            val shift = channel * 8
            val value = (background ushr shift) and 255
            val ink = if (darkInk) 0 else 255
            color or ((value * 0.30 + ink * 0.70).roundToInt() shl shift)
        })
        val contrast = (maxOf(luminance, inkLuminance) + 0.05) / (minOf(luminance, inkLuminance) + 0.05)
        // Leave a little headroom for GPU channel rounding during alpha compositing.
        return contrast >= 4.6
    }
    val opaque = pixel or 0xFF000000.toInt()
    if (readable(opaque)) return opaque
    var lower = 0.0
    var upper = 1.0
    repeat(12) {
        val middle = (lower + upper) / 2
        if (readable(blend(middle))) upper = middle else lower = middle
    }
    return blend(upper)
}

internal fun readableArtworkPigment(pixel: Int): Int =
    if (isDarkArtworkPigment(pixel) || artworkUsesDarkInk(pixel)) pixel or 0xFF000000.toInt()
    else 0xFF000000.toInt()
