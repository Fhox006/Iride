package com.metrolist.music.playback

import com.metrolist.music.ui.player.VideoRotationPolicy
import org.junit.Assert.assertEquals
import org.junit.Test

class VideoRotationPolicyTest {
    @Test
    fun sensorRotatesSurfaceInBothLandscapeDirections() {
        assertEquals(-90, VideoRotationPolicy().onOrientation(90))
        assertEquals(90, VideoRotationPolicy().onOrientation(270))
    }

    @Test
    fun intermediateAnglesKeepTheLastSurfaceOrientation() {
        val policy = VideoRotationPolicy()
        assertEquals(-90, policy.onOrientation(90))
        assertEquals(-90, policy.onOrientation(130))
        assertEquals(-90, policy.onOrientation(-1))
    }

    @Test
    fun returningUprightRestoresOnlyTheSurfaceRotation() {
        val policy = VideoRotationPolicy()
        policy.onOrientation(90)
        assertEquals(0, policy.onOrientation(0))
    }

    @Test
    fun aCompleteTurnDoesNotJumpAcrossTheZeroDegreeBoundary() {
        val policy = VideoRotationPolicy()
        assertEquals(-90, policy.onOrientation(90))
        assertEquals(-180, policy.onOrientation(180))
        assertEquals(-270, policy.onOrientation(270))
        assertEquals(-360, policy.onOrientation(0))
        assertEquals(-450, policy.onOrientation(90))
    }

    @Test
    fun initiallyUnknownOrDiagonalSensorDoesNotRotateTheFilm() {
        val policy = VideoRotationPolicy()
        assertEquals(0, policy.onOrientation(-1))
        assertEquals(0, policy.onOrientation(45))
    }
}
