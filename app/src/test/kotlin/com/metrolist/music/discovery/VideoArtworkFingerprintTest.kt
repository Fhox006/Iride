package com.metrolist.music.discovery

import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class VideoArtworkFingerprintTest {
    private fun sample(width: Int = 320, height: Int = 180, shift: Int = 0, noise: Int = 0) =
        VideoArtworkFingerprint.create(width, height) { x, y ->
            val red = ((x * 255 / width + shift) % 256 + noise).coerceIn(0, 255)
            val green = (y * 255 / height + noise).coerceIn(0, 255)
            0xFF000000.toInt() or (red shl 16) or (green shl 8) or 64
        }!!

    @Test fun repeatedFramesRejectStaticArtwork() {
        assertFalse(hasChangingVideoFrames(listOf(sample(), sample(), sample())))
    }

    @Test fun smallCompressionDifferencesStillRejectStaticArtwork() {
        assertFalse(hasChangingVideoFrames(listOf(sample(), sample(noise = 3), sample(noise = 5))))
    }

    @Test fun differentSceneAcceptsVideoEvenWithTwoRepeatedFrames() {
        assertTrue(hasChangingVideoFrames(listOf(sample(), sample(), sample(shift = 100))))
    }

    @Test fun missingFramesDoNotPassVerification() {
        assertFalse(hasChangingVideoFrames(listOf(sample(), sample(shift = 100))))
        assertFalse(hasChangingVideoFrames(emptyList()))
    }

    @Test fun differentResolutionsOfSameImageAreDuplicates() {
        assertTrue(sample().resembles(sample(width = 640, height = 360)))
        assertFalse(sample().resembles(sample(shift = 100)))
    }

    @Test fun blankAndTinyImagesAreRejected() {
        assertNull(VideoArtworkFingerprint.create(120, 90) { _, _ -> 0xFF000000.toInt() })
        assertNull(VideoArtworkFingerprint.create(120, 90) { _, _ -> 0xFFFFFFFF.toInt() })
        assertNull(VideoArtworkFingerprint.create(1, 1) { _, _ -> 0 })
        assertNotNull(sample())
    }
}
