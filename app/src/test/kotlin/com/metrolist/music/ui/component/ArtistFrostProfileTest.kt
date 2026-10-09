package com.metrolist.music.ui.component

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ArtistFrostProfileTest {
    @Test
    fun repeatedBlurPreservesSolidColorAndOpaqueEdgesEvenOnSmallImages() {
        val original = IntArray(7 * 5) { 0xFF2468AC.toInt() }
        var pixels = original
        repeat(3) {
            pixels = artistBoxBlur(pixels, 7, 5, horizontal = true, radius = 12)
            pixels = artistBoxBlur(pixels, 7, 5, horizontal = false, radius = 12)
        }
        assertArrayEquals(original, pixels)
    }

    @Test
    fun bothAxesProduceEquivalentResultsOnRotatedInputs() {
        val pixels = intArrayOf(0xFF000000.toInt(), 0xFFFFFFFF.toInt(), 0xFF333333.toInt())
        assertArrayEquals(
            artistBoxBlur(pixels, 3, 1, horizontal = true, radius = 2),
            artistBoxBlur(pixels, 1, 3, horizontal = false, radius = 2),
        )
    }

    @Test
    fun blurSoftensDetailsWithoutCreatingTransparentOrBlackBorders() {
        val pixels = IntArray(9 * 9) { 0xFFFFFFFF.toInt() }
        pixels[4 * 9 + 4] = 0xFF000000.toInt()
        val horizontal = artistBoxBlur(pixels, 9, 9, horizontal = true, radius = 2)
        val result = artistBoxBlur(horizontal, 9, 9, horizontal = false, radius = 2)
        assertTrue(result.all { it ushr 24 == 255 })
        assertEquals(0xFFFFFFFF.toInt(), result.first())
        assertTrue((result[4 * 9 + 4] and 255) in 1..254)
        assertTrue(result[4 * 9 + 4] == result[4 * 9 + 3])
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsInvalidBitmapShape() {
        artistBoxBlur(IntArray(5), 3, 3, horizontal = true, radius = 2)
    }
}
