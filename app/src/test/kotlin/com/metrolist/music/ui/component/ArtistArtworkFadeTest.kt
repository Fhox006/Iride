package com.metrolist.music.ui.component

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ArtistArtworkFadeTest {
    @Test
    fun portraitRemainsOpaqueAboveTheDissolveAndTitleHasOnlyPagePigment() {
        assertEquals(1f, artistArtworkPhotoOpacity(0f), 0f)
        assertEquals(1f, artistArtworkPhotoOpacity(0.48f), 0f)
        assertEquals(0f, artistArtworkPhotoOpacity(0.86f), 0.000001f)
        assertEquals(0f, artistArtworkPhotoOpacity(1f), 0f)
    }

    @Test
    fun fadeIsMonotonicWithoutAnAbruptVisibleStep() {
        var previous = artistArtworkPhotoOpacity(0f)
        for (row in 1..1000) {
            val opacity = artistArtworkPhotoOpacity(row / 1000f)
            assertTrue(opacity in 0f..1f)
            assertTrue(opacity <= previous + 0.000001f)
            assertTrue(previous - opacity < 0.005f)
            previous = opacity
        }
    }
}
