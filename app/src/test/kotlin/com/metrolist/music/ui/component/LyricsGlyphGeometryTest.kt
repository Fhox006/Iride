package com.metrolist.music.ui.component

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LyricsGlyphGeometryTest {
    @Test
    fun loneLetterIncludesInkOutsideItsAdvance() {
        val clips = lyricsGlyphClips(listOf(LyricsGlyphCell(0, 0f, 18f)), -32f, 50f)

        assertEquals(listOf(LyricsGlyphCell(0, -32f, 50f)), clips)
    }

    @Test
    fun trailingSpacesCannotRepaintTheLastLetter() {
        val clips = lyricsGlyphClips(
            listOf(LyricsGlyphCell(0, 0f, 18f), LyricsGlyphCell(1, 18f, 26f, isWhitespace = true)),
            -32f,
            58f,
        )

        assertEquals(listOf(LyricsGlyphCell(0, -32f, 58f)), clips)
    }

    @Test
    fun internalSpacesLeaveNoGapOrOverlapBetweenGlyphClips() {
        val clips = lyricsGlyphClips(
            listOf(
                LyricsGlyphCell(0, 0f, 18f),
                LyricsGlyphCell(1, 18f, 26f, isWhitespace = true),
                LyricsGlyphCell(2, 26f, 42f),
            ),
            -32f,
            74f,
        )

        assertEquals(2, clips.size)
        assertEquals(clips[0].right, clips[1].left, 0f)
        assertEquals(-32f, clips.first().left, 0f)
        assertEquals(74f, clips.last().right, 0f)
    }

    @Test
    fun rightToLeftTextAssignsOverhangByVisualOrder() {
        val clips = lyricsGlyphClips(
            listOf(LyricsGlyphCell(0, 18f, 36f), LyricsGlyphCell(1, 0f, 18f)),
            -32f,
            68f,
        )

        assertEquals(listOf(LyricsGlyphCell(1, -32f, 18f), LyricsGlyphCell(0, 18f, 68f)), clips)
    }

    @Test
    fun zeroWidthClustersCannotStealTheLastLettersOverhang() {
        val clips = lyricsGlyphClips(
            listOf(LyricsGlyphCell(0, 0f, 18f), LyricsGlyphCell(1, 18f, 18f)),
            -32f,
            50f,
        )

        assertEquals(listOf(LyricsGlyphCell(0, -32f, 50f)), clips)
    }

    @Test
    fun blankLineHasNoGlyphLayerToRepaint() {
        val clips = lyricsGlyphClips(listOf(LyricsGlyphCell(0, 0f, 8f, isWhitespace = true)), -32f, 40f)

        assertTrue(clips.isEmpty())
    }
}
