package com.metrolist.music.discovery

import org.junit.Assert.assertTrue
import org.junit.Test

class VideoArtworkQualityTest {
    @Test fun nativeResolutionWinsWhenPreviewsWouldHaveEqualDimensions() {
        assertTrue(VideoArtworkQuality(1280, 720, 0f) > VideoArtworkQuality(640, 360, 0f))
        assertTrue(VideoArtworkQuality(640, 360, 0.5f) > VideoArtworkQuality(480, 360, 0.9f))
    }

    @Test fun faceWinsOverLandscapeAtUsableResolution() {
        assertTrue(VideoArtworkQuality(640, 360, 0.5f) > VideoArtworkQuality(1280, 720, 0f))
    }

    @Test fun tinyFaceThumbnailCannotDisplaceUsableCover() {
        assertTrue(VideoArtworkQuality(1280, 720, 0f) > VideoArtworkQuality(320, 180, 0.9f))
        assertTrue(VideoArtworkQuality(480, 270, 0f) > VideoArtworkQuality(120, 90, 0.9f))
    }
}
