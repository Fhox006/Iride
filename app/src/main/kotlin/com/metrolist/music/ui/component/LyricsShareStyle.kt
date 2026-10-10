package com.metrolist.music.ui.component

import androidx.core.graphics.ColorUtils

/** Design units shared by the scaled preview and the exported image. */
internal object LyricsShareStyle {
    const val Width = 1440
    const val Padding = 80f
    const val CoverSize = 180f
    const val CoverRadius = 36f
    const val CoverGap = 40f
    const val HeaderGap = 64f
    const val TitleSize = 64f
    const val ArtistSize = 48f
    const val LineSpacing = 1.08f
    const val FooterGap = 56f
    const val LogoSize = 64f
    const val LogoGap = 24f
    const val AppNameSize = 48f

    fun textSize(text: String): Float = when {
        text.length < 80 -> 128f
        text.length < 180 -> 112f
        else -> 96f
    }

    fun lyricsHeightLimit(headerHeight: Float): Float =
        Width - Padding * 2 - headerHeight - HeaderGap - FooterGap - LogoSize

    /** Keep short excerpts content-sized and fit longer excerpts within a square. */
    fun fittedTextSize(text: String, heightLimit: Float, measureHeight: (Float) -> Float): Float {
        val preferred = textSize(text)
        if (measureHeight(preferred) <= heightLimit) return preferred
        var low = 1f
        var high = preferred
        repeat(12) {
            val candidate = (low + high) / 2
            if (measureHeight(candidate) <= heightLimit) low = candidate else high = candidate
        }
        return low
    }

    fun gradientColors(background: Int): IntArray = intArrayOf(
        ColorUtils.blendARGB(background, android.graphics.Color.WHITE, 0.06f),
        ColorUtils.blendARGB(background, android.graphics.Color.BLACK, 0.10f),
    )
}
