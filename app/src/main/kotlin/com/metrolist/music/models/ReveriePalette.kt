package com.metrolist.music.models

import java.util.Locale
import java.time.YearMonth
import kotlin.math.pow

data class ReveriePalette(
    val background: Int,
    val accent: Int = 0xFF383039.toInt(),
    val secondary: Int = 0xFFBA3167.toInt(),
)

const val REVERIE_PAPER = 0xFFF2F0EB.toInt()

private fun reverieLuminance(color: Int): Double {
    fun linear(shift: Int): Double {
        val value = (color shr shift and 255) / 255.0
        return if (value <= .04045) value / 12.92 else ((value + .055) / 1.055).pow(2.4)
    }
    return .2126 * linear(16) + .7152 * linear(8) + .0722 * linear(0)
}

fun reverieColorContrast(first: Int, second: Int): Double {
    val a = reverieLuminance(first) + .05
    val b = reverieLuminance(second) + .05
    return maxOf(a, b) / minOf(a, b)
}

fun readableReverieBackground(source: Int): Int {
    var color = source
    while (reverieColorContrast(color, 0xFF24242A.toInt()) < 4.8) {
        color = blendReverieColor(color, REVERIE_PAPER, .05f)
    }
    return color
}

/** Keep chromatic type chromatic when it needs more contrast, instead of replacing it with black. */
fun readableReverieColor(source: Int, background: Int, minimumContrast: Double = 4.5): Int {
    var color = source
    while (reverieColorContrast(color, background) < minimumContrast) {
        val darker = blendReverieColor(color, 0xFF101015.toInt(), .08f)
        if (darker == color) break
        color = darker
    }
    return color
}

/** Light paper retains a restrained tint from the artwork, with dark, softly tinted type. */
fun reveriePalette(source: Int, soft: Boolean = false): ReveriePalette {
    val channels = listOf(16, 8, 0).map { (source shr it and 255) / 255f }
    val darkest = channels.min()
    val chroma = channels.max() - darkest
    val pigmentChroma = if (chroma < .08f) chroma else chroma.coerceIn(.24f, .50f)
    val amount = if (soft) .48f else .74f
    fun channel(shift: Int, index: Int): Int {
        val hue = if (chroma == 0f) 0f else (channels[index] - darkest) / chroma
        val pigment = .96f - pigmentChroma + hue * pigmentChroma
        val paper = (REVERIE_PAPER shr shift and 255) / 255f
        return ((paper * (1 - amount) + pigment * amount) * 255).toInt().coerceIn(0, 255)
    }
    var background = (255 shl 24) or (channel(16, 0) shl 16) or (channel(8, 1) shl 8) or channel(0, 2)
    // Lift only as far as small dark text needs, retaining the cover's hue and chroma.
    background = readableReverieBackground(background)
    fun ink(shift: Int): Int = (((background shr shift and 255) * .22f) + 18).toInt()
    val accent = (255 shl 24) or (ink(16) shl 16) or (ink(8) shl 8) or ink(0)
    return ReveriePalette(background, accent, accent)
}

fun reverieMonthPalette(month: YearMonth): ReveriePalette = reveriePalette(reverieEdition(month).primary)

/** Displayed artwork always owns the paper tint; the month's colour is the fallback. */
fun reverieChapterPalettes(
    month: YearMonth,
    chapters: List<ReverieChapter>,
    artworkColors: Map<ReverieChapter, Int>,
): Map<ReverieChapter, ReveriePalette> {
    val primary = reverieEdition(month).primary
    val used = mutableListOf<Int>()
    return chapters.mapIndexed { index, chapter ->
        val hasImage = chapter in setOf(ReverieChapter.OVERTURE, ReverieChapter.ARTIST,
            ReverieChapter.ALBUM, ReverieChapter.SONG, ReverieChapter.POSTER) && chapter in artworkColors
        val source = artworkColors[chapter] ?: primary
        var palette = reveriePalette(source, soft = !hasImage && index % 3 == 1)
        var variation = 0
        while (!hasImage && palette.background in used && variation < 24) {
            variation++
            palette = palette.copy(background = blendReverieColor(palette.background, REVERIE_PAPER, .08f))
        }
        used.add(palette.background)
        chapter to palette
    }.toMap()
}

/** Locale-independent, version-stable pigments require no network or stored per-genre state. */
fun reverieGenreColor(name: String): Int {
    val pigments = intArrayOf(
        0xFFAD4936.toInt(), 0xFF3866A0.toInt(), 0xFF397A65.toInt(), 0xFF91518D.toInt(),
        0xFFAE6A28.toInt(), 0xFF387987.toInt(), 0xFFB34669.toInt(), 0xFF747329.toInt(),
        0xFF6652A3.toInt(), 0xFF9B543D.toInt(), 0xFF327C83.toInt(), 0xFF9C3971.toInt(),
    )
    val key = name.trim().lowercase(Locale.ROOT).replace(Regex("\\s+"), " ")
    return pigments[Math.floorMod(key.hashCode(), pigments.size)]
}

fun blendReverieColor(base: Int, tint: Int, amount: Float): Int {
    fun channel(shift: Int): Int = ((base shr shift and 255) * (1 - amount) +
        (tint shr shift and 255) * amount).toInt().coerceIn(0, 255)
    return (255 shl 24) or (channel(16) shl 16) or (channel(8) shl 8) or channel(0)
}
