package com.metrolist.music.ui.component

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ArtistArtworkCompositionTest {
    private val side = 64
    private val backdrop = 0xFF303030.toInt()

    private fun blur(pixels: IntArray): IntArray {
        var result = pixels
        repeat(3) {
            result = artistBoxBlur(result, side, side, horizontal = true, radius = 3)
            result = artistBoxBlur(result, side, side, horizontal = false, radius = 3)
        }
        return result
    }

    @Test
    fun flatBlackAndWhiteGraphicDoesNotEarnPhotographicDetailBonus() {
        val graphic = IntArray(side * side) { if ((it / side + it % side) % 4 < 2) -1 else 0xFF000000.toInt() }
        assertTrue(artistPhotographicQuality(graphic, side, side) < 0.2f)
    }

    @Test
    fun grayscaleWithNaturalTonalGradationRemainsEligibleAsPhoto() {
        val grayscale = IntArray(side * side) {
            val shade = (it % side) * 4
            0xFF000000.toInt() or (shade shl 16) or (shade shl 8) or shade
        }
        assertTrue(artistPhotographicQuality(grayscale, side, side) > 0.8f)
    }

    @Test
    fun limitedAntialiasingDoesNotMakeBinaryGraphicsLookPhotographic() {
        val graphic = IntArray(side * side) {
            val shade = if (it % 20 == 0) (it % 255) else if (it % 3 == 0) 0 else 255
            0xFF000000.toInt() or (shade shl 16) or (shade shl 8) or shade
        }
        assertTrue(artistPhotographicQuality(graphic, side, side) < 0.4f)
    }

    @Test
    fun checksLowerCenterEvenWhenAllFourCornersMatch() {
        val clean = IntArray(side * side) { backdrop }
        val obstructed = clean.copyOf()
        for (y in 48 until side) for (x in 22 until 42) obstructed[y * side + x] = -1
        assertTrue(artistArtworkHasUniformCorners(obstructed, side, side))
        val good = artistArtworkBlendQuality(blur(clean), side, side, backdrop)
        val bad = artistArtworkBlendQuality(blur(obstructed), side, side, backdrop)
        assertTrue(good - bad > 0.1f)
    }

    @Test
    fun smoothDarkClothingDoesNotVetoPastelPhotoBackground() {
        val pink = 0xFFF2C9D8.toInt()
        val pixels = IntArray(side * side) {
            if (it % side in 12..51 && it / side >= 35) 0xFF101010.toInt() else pink
        }
        assertTrue(artistArtworkBlendQuality(blur(pixels), side, side, pink) >= 0.65f)
    }

    @Test
    fun upperLightingDoesNotVetoSmoothLowerTransition() {
        val pixels = IntArray(side * side) { if (it / side < 15 && it % side < 32) -1 else backdrop }
        assertTrue(artistArtworkBlendQuality(blur(pixels), side, side, backdrop) > 0.95f)
    }

    @Test
    fun transparentAndBlankContentDoesNotGainPhotoBonus() {
        assertEquals(0f, artistPhotographicQuality(IntArray(side * side), side, side), 0f)
        assertEquals(0f, artistPhotographicQuality(IntArray(side * side) { -1 }, side, side), 0f)
        assertEquals(0f, artistArtworkBlendQuality(IntArray(side * side), side, side, backdrop), 0f)
    }

    @Test
    fun blendingScoreIsBoundedAcrossDifferentBackgrounds() {
        for (color in listOf(0xFF000000.toInt(), -1, 0xFF2244AA.toInt(), backdrop)) {
            val score = artistArtworkBlendQuality(IntArray(side * side) { color }, side, side, backdrop)
            assertTrue(score in 0f..1f)
        }
    }
}
