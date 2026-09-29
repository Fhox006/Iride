package com.metrolist.music.discovery

import kotlin.math.hypot
import kotlin.random.Random
import org.junit.Assert.assertTrue
import org.junit.Test

class MixPosterRendererTest {
    @Test
    fun rosetteIsUsuallyFullyVisibleAndAvoidsCenter() {
        val random = Random(42)
        val size = 1_000f
        val radius = 200f
        var clipped = 0

        repeat(1_000) {
            val center = MixPosterRenderer.rosetteCenter(size, radius, random)
            if (center.x < radius || center.x > size - radius ||
                center.y < radius || center.y > size - radius) {
                clipped++
            }
            assertTrue(hypot(center.x - size / 2f, center.y - size / 2f) >= size * 0.12f)
            assertTrue(center.y + radius <= size * 0.62f)
        }

        assertTrue("Expected occasional edge placement, got $clipped / 1000", clipped in 100..200)
    }
}
