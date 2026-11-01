package com.metrolist.music.ui.utils

import org.junit.Assert.assertEquals
import org.junit.Test

class YouTubeUtilsTest {
    @Test
    fun keepsVideoArtworkUrlInsteadOfAssumingMaxResolutionExists() {
        listOf("hqdefault", "mqdefault", "sddefault").forEach { quality ->
            val url = "https://i.ytimg.com/vi/video/$quality.jpg"
            assertEquals(url, url.resize(1024, 1024))
        }
    }

    @Test
    fun preservesAspectRatioWhenDownsizingOneDimension() {
        val url = "https://lh3.googleusercontent.com/photo=w600-h300-p-l90-rj"
        assertEquals("https://lh3.googleusercontent.com/photo=w300-h150-p-l90-rj", url.resize(width = 300))
        assertEquals("https://lh3.googleusercontent.com/photo=w300-h150-p-l90-rj", url.resize(height = 150))
    }
}
