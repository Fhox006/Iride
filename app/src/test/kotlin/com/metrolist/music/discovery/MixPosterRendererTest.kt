package com.metrolist.music.discovery

import kotlin.random.Random
import org.junit.Assert.assertTrue
import org.junit.Test

class MixPosterRendererTest {
    @Test
    fun rosetteStaysOutsidePortraitAndAboveTitle() {
        val random = Random(42)
        val size = 1_000f

        repeat(1_000) {
            val radius = size * (0.14f + random.nextFloat() * 0.08f)
            val center = MixPosterRenderer.rosetteCenter(size, radius, random)
            assertTrue(center.x in 0f..size)
            assertTrue(center.x + radius <= size * 0.28f + 0.001f ||
                center.x - radius >= size * 0.72f - 0.001f)
            assertTrue(center.y >= radius)
            assertTrue(center.y + radius <= size * 0.62f)
        }
    }
}
