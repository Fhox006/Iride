package com.metrolist.music.ui.component

import org.junit.Assert.assertEquals
import org.junit.Test

class ArtistBackgroundColorTest {
    private val orange = 0xFFE87824.toInt()
    private val black = 0xFF101010.toInt()
    private val blue = 0xFF547ACC.toInt()
    private val red = 0xFFD02624.toInt()

    private fun portrait(pixel: (Int, Int) -> Int): Int = artistBackgroundColor(
        IntArray(100 * 100) { index -> pixel(index % 100, index / 100) }, 100, 100,
    )

    @Test
    fun midtoneBackdropKeepsItsPigmentWithoutPromotingBlackClothing() {
        assertEquals(orange, portrait { x, y -> if (y > 20 && x in 12..87) black else orange })
    }

    @Test
    fun mixedColoredBackgroundsPreferTheLowerPerimeter() {
        assertEquals(red, portrait { _, y -> if (y < 30) blue else red })
    }

    @Test
    fun smallCentralColorDetailDoesNotReplaceBlackBackground() {
        assertEquals(black, portrait { x, y -> if (x in 45..54 && y in 40..49) orange else black })
    }

    @Test
    fun brightFaceDoesNotBecomeBackgroundOnAMidtoneBackdrop() {
        val skin = 0xFFF2B68C.toInt()
        assertEquals(orange, portrait { x, y -> if (x in 25..74 && y in 15..74) skin else orange })
    }

    @Test
    fun neutralBackgroundKeepsItsExactPigment() {
        val gray = 0xFFB2B2B2.toInt()
        assertEquals(gray, portrait { _, _ -> gray })
    }

    @Test
    fun translucentPixelsDoNotInventBackgroundColor() {
        assertEquals(black, portrait { x, _ -> if (x < 20) 0x00FF0000 else black })
    }

    @Test
    fun naturallyDarkColoredEdgesKeepTheExactPigment() {
        for (pigment in listOf(0xFF30251A.toInt(), 0xFF183321.toInt(), 0xFF400F14.toInt())) {
            assertEquals(pigment, portrait { x, y -> if (x in 25..74 && y < 70) orange else pigment })
        }
    }

    @Test
    fun readableDarkGraysAndSaturatedPigmentsAreNotDiscarded() {
        for (pigment in listOf(0xFF444444.toInt(), 0xFF555555.toInt(), 0xFF244ACC.toInt(), 0xFF702A38.toInt())) {
            assertEquals(pigment, portrait { _, _ -> pigment })
        }
    }

    @Test
    fun whiteBackdropStaysWhiteWithBlackClothing() {
        val white = 0xFFFFFFFF.toInt()
        assertEquals(white, portrait { x, y -> if (y > 20 && x in 12..87) black else white })
    }

    @Test
    fun brightPastelKeepsItsColorWithoutBecomingGray() {
        val pastel = 0xFFCEE7D4.toInt()
        assertEquals(pastel, portrait { _, _ -> pastel })
    }

    @Test
    fun pastelPinkSideBackgroundStaysPinkWithFaceAndDarkClothing() {
        val pink = 0xFFF2C9D8.toInt()
        val skin = 0xFFF2B68C.toInt()
        assertEquals(pink, portrait { x, y ->
            when {
                x in 30..69 && y in 10..44 -> skin
                x in 12..87 && y >= 45 -> black
                else -> pink
            }
        })
    }

    @Test
    fun smallDarkSideDetailsDoNotReplaceTheColoredBackdrop() {
        assertEquals(red, portrait { x, y -> if (x < 2 && y < 10) black else red })
    }

    @Test
    fun everyOpaqueSolidColorKeepsItsPigment() {
        for (r in 0..255 step 17) for (g in 0..255 step 17) for (b in 0..255 step 17) {
            val color = 0xFF000000.toInt() or (r shl 16) or (g shl 8) or b
            assertEquals(color, artistBackgroundColor(IntArray(16) { color }, 4, 4))
        }
    }

    @Test
    fun transparentPhotoFallsBackSafely() {
        assertEquals(0xFF000000.toInt(), artistBackgroundColor(IntArray(16), 4, 4))
    }
}
