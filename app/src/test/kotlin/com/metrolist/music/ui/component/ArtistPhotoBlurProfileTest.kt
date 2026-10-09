package com.metrolist.music.ui.component

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ArtistPhotoBlurProfileTest {
    private fun progress(top: Float, initial: Float = 356f) =
        artistPhotoBlurProgress(top, initial, controlsBottom = 76f, lead = 160f, finishGap = 12f)

    @Test
    fun portraitStaysClearUntilItsTitleReachesUpperPartOfPage() {
        assertEquals(0f, progress(356f), 0f)
        assertEquals(0f, progress(270f), 0f)
        assertEquals(0f, progress(236f), 0f)
        assertTrue(progress(200f) > 0f)
    }

    @Test
    fun blurBecomesStrongBeforeTitleTouchesControls() {
        assertTrue(progress(140f) > 0.7f)
        assertEquals(1f, progress(88f), 0f)
        assertEquals(1f, progress(76f), 0f)
    }

    @Test
    fun radiusProgressIsBoundedAndMonotonic() {
        val values = (0..400).map { progress(400f - it) }
        assertTrue(values.all { it in 0f..1f })
        assertTrue(values.zipWithNext().all { (left, right) -> right >= left })
        assertEquals(0f, values.first(), 0f)
        assertEquals(1f, values.last(), 0f)
    }

    @Test
    fun smallViewportDoesNotStartWithBlurAlreadyApplied() {
        assertEquals(0f, progress(150f, initial = 150f), 0f)
        assertTrue(progress(130f, initial = 150f) in 0f..1f)
        assertEquals(1f, progress(88f, initial = 150f), 0f)
    }

    @Test
    fun missingLayoutDoesNotBlurAndRemovedHeaderReachesFullBlur() {
        assertEquals(0f, progress(Float.POSITIVE_INFINITY), 0f)
        assertEquals(1f, progress(Float.NEGATIVE_INFINITY), 0f)
    }

    @Test
    fun onsetAndCompletionHaveNoAbruptRadiusJump() {
        assertTrue(progress(235.9f) < 0.001f)
        assertTrue(1f - progress(88.1f) < 0.001f)
    }
}
