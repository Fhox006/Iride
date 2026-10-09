package com.metrolist.music.ui.component

import kotlin.math.pow

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

internal fun readableArtworkPigment(pixel: Int): Int =
    if (isDarkArtworkPigment(pixel) || artworkUsesDarkInk(pixel)) pixel or 0xFF000000.toInt()
    else 0xFF000000.toInt()
