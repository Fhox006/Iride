package com.metrolist.music.ui.component

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AlbumSleeveMotionTest {
    @Test
    fun quickAndSlowFlightsFinishOnTheFrontBeforeHandover() {
        for (duration in listOf(1, 70, 150, 500, 1000)) {
            for (angle in listOf(30f, 90f, 180f)) {
                var previous = angle
                for (step in 0..duration) {
                    val progress = step.toFloat() / duration
                    val rotation = AlbumSleeveMotion.rotation(angle, AlbumSleeveMotion.closeProgress(0f, 1f - progress))
                    assertTrue(rotation <= previous + .001f)
                    if (progress >= .65f) assertEquals(0f, rotation, .001f)
                    previous = rotation
                }
                assertEquals(0f, previous, 0f)
            }
        }
    }

    @Test
    fun gestureCommitHasNoJumpAndCancellationReversesExactly() {
        for (amount in listOf(0f, .01f, .2f, .6f, 1f)) {
            val preview = AlbumSleeveMotion.closeProgress(amount, 1f)
            assertEquals(amount, preview, .00001f)
            assertEquals(1f, AlbumSleeveMotion.closeProgress(amount, 0f), 0f)
            val samples = (0..100).map {
                AlbumSleeveMotion.rotation(180f, AlbumSleeveMotion.closeProgress(amount * it / 100f, 1f))
            }
            assertEquals(180f, samples.first(), .001f)
            assertTrue(samples.zipWithNext().all { (a, b) -> b <= a + .001f })
            var last = samples.last()
            for (step in 100 downTo 0) {
                val restoring = AlbumSleeveMotion.rotation(180f,
                    AlbumSleeveMotion.closeProgress(amount * step / 100f, 1f))
                assertEquals(samples[step], restoring, 0f)
                assertTrue(restoring >= last - .001f)
                last = restoring
            }
            assertEquals(180f, AlbumSleeveMotion.rotation(180f, AlbumSleeveMotion.closeProgress(0f, 1f)), 0f)
        }
    }
}
