package com.metrolist.music.playback

import com.metrolist.music.ui.player.VideoZoomPolicy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VideoZoomPolicyTest {
    @Test fun onlyLandscapeFilmsInBothLandscapeOrientationsAllowZoom() {
        listOf(-450, -270, -90, 90, 270, 450).forEach { rotation ->
            assertTrue(VideoZoomPolicy.isAllowed(16f / 9f, rotation))
            assertFalse(VideoZoomPolicy.isAllowed(9f / 16f, rotation))
            assertFalse(VideoZoomPolicy.isAllowed(1f, rotation))
        }
        listOf(-360, -180, 0, 45, 135, 180, 360).forEach { rotation ->
            assertFalse(VideoZoomPolicy.isAllowed(16f / 9f, rotation))
        }
    }

    @Test fun repeatedOutwardPinchesKeepOnlyOneEnlargement() {
        var enlarged = false
        repeat(5) { enlarged = VideoZoomPolicy.afterPinch(enlarged, 1.5f, allowed = true) }
        assertTrue(enlarged)
        assertEquals(1.25f, VideoZoomPolicy.EnlargedScale, 0f)
        assertFalse(VideoZoomPolicy.afterPinch(enlarged, 0.8f, allowed = true))
    }

    @Test fun smallFingerMovementsDoNotToggleZoom() {
        assertFalse(VideoZoomPolicy.afterPinch(false, 1.02f, allowed = true))
        assertTrue(VideoZoomPolicy.afterPinch(true, 0.98f, allowed = true))
    }

    @Test fun losingLandscapeRestoresFitAndRejectsFurtherPinches() {
        assertFalse(VideoZoomPolicy.afterPinch(true, 1f, allowed = false))
        assertFalse(VideoZoomPolicy.afterPinch(false, 2f, allowed = false))
    }

    @Test fun invalidVideoSizesAndGestureFactorsCannotEnableZoom() {
        listOf(Float.NaN, Float.POSITIVE_INFINITY, 0f, -1f).forEach { aspect ->
            assertFalse(VideoZoomPolicy.isAllowed(aspect, 90))
        }
        listOf(Float.NaN, Float.POSITIVE_INFINITY, 0f, -1f).forEach { factor ->
            assertFalse(VideoZoomPolicy.afterPinch(false, factor, allowed = true))
            assertTrue(VideoZoomPolicy.afterPinch(true, factor, allowed = true))
        }
    }
}
