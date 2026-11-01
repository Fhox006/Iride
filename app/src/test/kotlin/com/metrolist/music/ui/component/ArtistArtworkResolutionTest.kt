package com.metrolist.music.ui.component

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ArtistArtworkResolutionTest {
    @Test
    fun allowsFourKWhenItsDecodedPhotoFitsTheBudget() {
        assertEquals(3840, artistArtworkDetailResolution(64 * 1024 * 1024))
    }

    @Test
    fun smallerMemoryBudgetsKeepPhotoAndFrostTexturesWithinTheirLimit() {
        for (budget in listOf(4, 8, 16, 32)) {
            val bytes = budget * 1024 * 1024
            val side = artistArtworkDetailResolution(bytes)
            assertTrue(side < 3840)
            assertTrue(side.toLong() * side * 4 + 512 * 1024 <= bytes)
        }
    }

    @Test
    fun neverEnlargesASmallNativePhotoToPretendItIsFourK() {
        assertEquals(640, artistArtworkSquareSide(640, 640, 3840))
        assertEquals(640, artistArtworkSquareSide(960, 640, 3840))
    }

    @Test
    fun preservesAvailableDetailsAndBoundsLargerImages() {
        assertEquals(3840, artistArtworkSquareSide(3840, 3840, 3840))
        assertEquals(3840, artistArtworkSquareSide(6000, 4000, 3840))
        assertEquals(1200, artistArtworkSquareSide(3840, 3840, 1200))
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsInvalidImageDimensions() {
        artistArtworkSquareSide(0, 640, 3840)
    }
}
