package com.metrolist.music.ui.component

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ArtistBackgroundTransitionTest {
    @Test
    fun returningToTheArtistRestoresTheOriginalTint() {
        val initial = artistBackgroundTintOpacity(200f, 0f, 1800f, 1000f)
        assertEquals(1f, initial, 0f)
        assertEquals(0f, artistBackgroundTintOpacity(200f, 2400f, 1800f, 1000f), 0f)
        assertEquals(initial, artistBackgroundTintOpacity(200f, 0f, 1800f, 1000f), 0f)
    }

    @Test
    fun theWholeInitialViewportStaysTintedEvenWithAnEarlyAlbumSection() {
        for (scroll in 0..350 step 10) {
            for (y in 0..1000 step 20) {
                assertEquals(1f, artistBackgroundTintOpacity(y.toFloat(), scroll.toFloat(), 600f, 1000f), 0f)
            }
        }
    }

    @Test
    fun baseEntersFromBelowOnlyAfterTheInitialScrollAndMovesWithParallax() {
        val bottom = artistBackgroundTintOpacity(1000f, 550f, 600f, 1000f)
        assertTrue(bottom > 0f && bottom < 1f)
        assertEquals(1f, artistBackgroundTintOpacity(500f, 550f, 600f, 1000f), 0f)
        assertEquals(0f, artistBackgroundTintOpacity(0f, 2400f, 600f, 1000f), 0f)
    }

    @Test
    fun tintRetreatsContinuouslyWithoutReappearingFurtherDownThePage() {
        var previous = 1f
        for (scroll in 0..2400 step 10) {
            val opacity = artistBackgroundTintOpacity(0f, scroll.toFloat(), 1800f, 1000f)
            assertTrue(opacity in 0f..previous)
            previous = opacity
        }
        assertEquals(0f, previous, 0f)
    }

    @Test
    fun downwardOverscrollDoesNotChangeTheBackgroundPigment() {
        assertEquals(
            artistBackgroundTintOpacity(300f, 0f, 1800f, 1000f),
            artistBackgroundTintOpacity(300f, -120f, 1800f, 1000f), 0f,
        )
    }
}
