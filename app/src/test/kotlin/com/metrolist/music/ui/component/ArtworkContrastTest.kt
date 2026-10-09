package com.metrolist.music.ui.component

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ArtworkContrastTest {
    @Test
    fun artistInkMeetsNormalTextContrastForEverySampledPigment() {
        for (r in 0..255 step 17) for (g in 0..255 step 17) for (b in 0..255 step 17) {
            val color = 0xFF000000.toInt() or (r shl 16) or (g shl 8) or b
            val luminance = artworkLuminance(color)
            val contrast = if (artistArtworkUsesDarkInk(color)) (luminance + 0.05) / 0.05
                else 1.05 / (luminance + 0.05)
            assertTrue("Artist pigment $color: contrast $contrast", contrast >= 4.5)
        }
    }

    @Test
    fun whiteAndPastelsKeepTheirOriginalPigment() {
        for (color in listOf(0xFFFFFFFF.toInt(), 0xFFCEE7D4.toInt(), 0xFFFFF1D0.toInt())) {
            assertEquals(color, readableArtworkPigment(color))
            assertTrue(artworkUsesDarkInk(color))
        }
    }

    @Test
    fun midtonesAreRejectedRatherThanDarkenedOrConvertedToGray() {
        for (color in listOf(0xFFFF0000.toInt(), 0xFF008800.toInt(), 0xFF777777.toInt())) {
            assertEquals(0xFF000000.toInt(), readableArtworkPigment(color))
        }
    }

    @Test
    fun grainHeadroomRejectsGrayThatOnlyBarelyPassesWithoutDecoration() {
        val gray = 0xFF999999.toInt()
        assertTrue((artworkLuminance(gray) + 0.05) / 0.05 > 7.0)
        assertFalse(artworkUsesDarkInk(gray))
        assertEquals(0xFF000000.toInt(), readableArtworkPigment(gray))
    }

    @Test
    fun everyAcceptedGrayHasReadableInkAndAnUnchangedPigment() {
        for (value in 0..255) {
            val color = 0xFF000000.toInt() or (value shl 16) or (value shl 8) or value
            val selected = readableArtworkPigment(color)
            assertTrue(selected == color || selected == 0xFF000000.toInt())
            val contrast = if (artworkUsesDarkInk(selected)) {
                (artworkLuminance(selected, 0.90) + 0.05) / 0.05
            } else {
                1.05 / (artworkLuminance(selected) + 0.05)
            }
            assertTrue("Gray $value: contrast $contrast", contrast >= 7.0)
        }
    }
}
