package com.metrolist.music.ui.component

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class IrideFolioScrollbarGeometryTest {
    @Test
    fun hidesWhenContentFitsOrMeasurementsAreNotReady() {
        assertNull(folioScrollbarThumb(200f, 200, 200, 0, 20f))
        assertNull(folioScrollbarThumb(200f, 100, 200, 0, 20f))
        assertNull(folioScrollbarThumb(200f, Int.MAX_VALUE, 200, 0, 20f))
        assertNull(folioScrollbarThumb(0f, 1000, 200, 0, 20f))
    }

    @Test
    fun thumbCoversVisibleFractionAndReachesBothTrackEnds() {
        val start = folioScrollbarThumb(200f, 800, 200, 0, 20f)!!
        val end = folioScrollbarThumb(200f, 800, 200, 600, 20f)!!
        assertEquals(50f, start.height, 0f)
        assertEquals(0f, start.offset, 0f)
        assertEquals(200f, end.offset + end.height, 0f)
    }

    @Test
    fun longContentKeepsThumbUsableAndWithinShortTracks() {
        assertEquals(20f, folioScrollbarThumb(200f, 100000, 200, 50000, 20f)!!.height, 0f)
        val short = folioScrollbarThumb(12f, 100000, 200, 50000, 20f)!!
        assertTrue(short.height <= 12f)
        assertEquals(0f, short.travel, 0f)
    }

    @Test
    fun overscrollNeverMovesThumbOutsideTrack() {
        assertEquals(0f, folioScrollbarThumb(200f, 800, 200, -100, 20f)!!.offset, 0f)
        val beyondEnd = folioScrollbarThumb(200f, 800, 200, 1000, 20f)!!
        assertEquals(beyondEnd.travel, beyondEnd.offset, 0f)
    }
}
