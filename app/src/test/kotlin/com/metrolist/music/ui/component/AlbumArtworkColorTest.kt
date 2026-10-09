package com.metrolist.music.ui.component

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AlbumArtworkColorTest {
    private val white = 0xFFFFFFFF.toInt()
    private val black = 0xFF000000.toInt()
    private val blue = 0xFF244ACC.toInt()

    private fun cover(pixel: (Int, Int) -> Int) = albumArtworkPigments(
        IntArray(10000) { pixel(it % 100, it / 100) }, 100, 100,
    )

    @Test
    fun whiteMarkOnBlackCoverIsNotSuppressedByBlackPopulation() {
        assertEquals(white, cover { x, y -> if (x in 40..59 && y in 30..69) white else black }.first().color)
    }

    @Test
    fun blueAccentOnWhiteCoverWinsBeforeBackgroundNeutral() {
        assertEquals(blue, cover { x, y -> if (x in 40..59 && y in 30..69) blue else white }.first().color)
    }

    @Test
    fun fieldPositionComesFromTheColoredRegionOfTheCover() {
        val left = cover { x, _ -> if (x < 10) blue else white }.first()
        val right = cover { x, _ -> if (x > 89) blue else white }.first()
        assertTrue(left.centerX < 0.1f)
        assertTrue(right.centerX > 0.9f)
    }

    @Test
    fun tinyColoredNoiseAndTransparentPixelsDoNotBeatWhiteBackground() {
        assertEquals(white, cover { x, y -> if (x == 0 && y == 0) blue else white }.first().color)
        assertEquals(white, cover { x, _ -> if (x < 50) 0x0000FFFF else white }.first().color)
    }

    @Test
    fun extractedColorsBelongToSourcePixels() {
        val pixels = IntArray(10000) { index ->
            if (index % 3 == 0) 0xFF2145C8.toInt() else if (index % 3 == 1) 0xFF2246C9.toInt() else white
        }
        val source = pixels.toSet()
        assertTrue(albumArtworkPigments(pixels, 100, 100).all { it.color in source })
    }

    @Test
    fun whiteRemainsAvailableWhenColoredAccentFailsTextContrast() {
        val midtoneBlue = 0xFF547ACC.toInt()
        val pixels = IntArray(10000) { if (it % 100 < 10) midtoneBlue else white }
        assertEquals(midtoneBlue, albumArtworkPigments(pixels, 100, 100).first().color)
        assertEquals(white, albumArtworkPigments(pixels, 100, 100, readableOnly = true).first().color)
    }

    @Test
    fun readableColoredAccentWinsWithoutInventingShades() {
        val pastelBlue = 0xFFCADCFF.toInt()
        val pixels = IntArray(10000) { if (it % 100 < 10) pastelBlue else white }
        assertEquals(pastelBlue, albumArtworkPigments(pixels, 100, 100, readableOnly = true).first().color)
    }
}
