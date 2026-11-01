package com.metrolist.music.models

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.pow

class ReveriePaletteTest {
    @Test fun homeAndImageFreeOpeningShareTheSameLightMonthlyPalette() {
        for (month in 1..12) {
            val date = java.time.YearMonth.of(2026, month)
            val primary = reverieEdition(date).primary
            val channels = listOf(16, 8, 0).map { primary shr it and 255 }
            assertTrue(channels.max() - channels.min() < 160)
            val opening = reverieChapterPalettes(date, ReverieChapter.entries, emptyMap()).getValue(ReverieChapter.OVERTURE)
            assertEquals(reverieMonthPalette(date), opening)
            assertTrue(reverieColorContrast(opening.accent, opening.background) >= 3.0)
            assertTrue(luminance(opening.background) >= .45)
            assertEquals(opening.accent, opening.secondary)
        }
    }

    @Test fun artworkPagesKeepTheirImageTintWhileEditorialPagesVaryGently() {
        for (month in 1..12) for (artwork in listOf(0xFFF87912.toInt(), 0xFF888888.toInt())) {
            val date = java.time.YearMonth.of(2026, month)
            val sources = ReverieChapter.entries.associateWith { artwork }
            val palettes = reverieChapterPalettes(date, ReverieChapter.entries, sources)
            listOf(ReverieChapter.OVERTURE, ReverieChapter.ARTIST, ReverieChapter.ALBUM,
                ReverieChapter.SONG, ReverieChapter.POSTER).forEach { chapter ->
                assertEquals(reveriePalette(artwork), palettes.getValue(chapter))
            }
            val editorial = listOf(ReverieChapter.TIME, ReverieChapter.RHYTHM, ReverieChapter.GENRES, ReverieChapter.COLOURS)
            assertEquals(4, editorial.map { palettes.getValue(it).background }.distinct().size)
            assertEquals(palettes, reverieChapterPalettes(date, ReverieChapter.entries, sources))
            assertTrue(palettes.values.all { reverieColorContrast(it.background, 0xFF24242A.toInt()) >= 4.5 })
        }
    }

    @Test fun openingAndFinalPosterUseTheirCoversInsteadOfTheMonthlyColour() {
        val date = java.time.YearMonth.of(2026, 9)
        val cover = 0xFF2A65DA.toInt()
        val album = 0xFFE77B20.toInt()
        val palettes = reverieChapterPalettes(date, ReverieChapter.entries,
            mapOf(ReverieChapter.OVERTURE to cover, ReverieChapter.POSTER to album))
        assertEquals(reveriePalette(cover), palettes.getValue(ReverieChapter.OVERTURE))
        assertEquals(reveriePalette(album), palettes.getValue(ReverieChapter.POSTER))
    }

    @Test fun coloredTypeRemainsChromaticAfterContrastCorrection() {
        val paper = reverieMonthPalette(java.time.YearMonth.of(2026, 1)).background
        val red = readableReverieColor(0xFFEA3422.toInt(), paper)
        assertTrue(reverieColorContrast(red, paper) >= 4.5)
        assertTrue((red shr 16 and 255) > (red and 255) + 30)
    }
    @Test fun genrePigmentsIgnoreLocaleCaseAndExtraWhitespace() {
        assertEquals(reverieGenreColor("indie rock"), reverieGenreColor("  INDIE   Rock  "))
        val previous = java.util.Locale.getDefault()
        try {
            java.util.Locale.setDefault(java.util.Locale.forLanguageTag("tr-TR"))
            assertEquals(reverieGenreColor("INDIE"), reverieGenreColor("indie"))
        } finally { java.util.Locale.setDefault(previous) }
        assertTrue(listOf("rock", "pop", "jazz", "hip hop").map(::reverieGenreColor).distinct().size >= 3)
    }

    @Test fun orangeArtworkKeepsAnOrangeBackgroundAndNeutralArtworkStaysNeutral() {
        val orange = reveriePalette(0xFFF87912.toInt()).background
        assertTrue((orange shr 16 and 255) > (orange shr 8 and 255))
        assertTrue((orange shr 8 and 255) > (orange and 255) + 35)
        val neutral = reveriePalette(0xFF888888.toInt()).background
        assertTrue(kotlin.math.abs((neutral shr 16 and 255) - (neutral and 255)) < 10)
    }
    @Test fun albumHuesProduceDifferentLightBackgrounds() {
        val warm = reveriePalette(0xFFCC3222.toInt())
        val cool = reveriePalette(0xFF2244DD.toInt())
        assertTrue(warm != cool)
        assertTrue((warm.background shr 16 and 255) > (warm.background and 255) + 80)
        assertTrue((cool.background and 255) > (cool.background shr 16 and 255) + 80)
        assertTrue(reveriePalette(0xFFCC3222.toInt(), soft = true) != warm)
        assertTrue(REVERIE_PAPER != reveriePalette(0xFF111111.toInt()).background)
    }

    @Test fun darkAndBrightArtworkKeepSmallDarkTypeReadable() {
        val ink = luminance(0xFF24242A.toInt())
        for (red in 0..255 step 51) for (green in 0..255 step 51) for (blue in 0..255 step 51) {
            val source = (255 shl 24) or (red shl 16) or (green shl 8) or blue
            for (soft in listOf(false, true)) {
                val color = reveriePalette(source, soft).background
                assertEquals(255, color ushr 24)
                assertTrue((luminance(color) + .05) / (ink + .05) >= 4.5)
            }
        }
    }

    private fun luminance(color: Int): Double {
        fun channel(shift: Int): Double {
            val value = (color shr shift and 255) / 255.0
            return if (value <= .04045) value / 12.92 else ((value + .055) / 1.055).pow(2.4)
        }
        return .2126 * channel(16) + .7152 * channel(8) + .0722 * channel(0)
    }
}
