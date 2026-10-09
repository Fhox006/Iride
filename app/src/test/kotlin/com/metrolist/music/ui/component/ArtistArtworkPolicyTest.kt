package com.metrolist.music.ui.component

import com.metrolist.music.data.remote.ArtistArtworkSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.assertNull
import org.junit.Test

class ArtistArtworkPolicyTest {
    private val side = 64
    private fun solid(color: Int) = IntArray(side * side) { color }
    private fun accepts(pixels: IntArray) = artistArtworkHasUniformCorners(pixels, side, side)

    @Test
    fun acceptsSolidLightDarkAndColoredBackgrounds() {
        listOf(0xFF000000, 0xFFFFFFFF, 0xFF242A35, 0xFF9341C1).forEach {
            assertTrue(accepts(solid(it.toInt())))
        }
    }

    @Test
    fun acceptsSubtleLightingGradient() {
        val pixels = solid(0xFF242A35.toInt())
        for (y in 0 until side) for (x in 0 until side) {
            val shade = 42 + x / 6
            pixels[y * side + x] = 0xFF000000.toInt() or (shade shl 16) or (shade shl 8) or shade
        }
        assertTrue(accepts(pixels))
    }

    @Test
    fun rejectsStrongLightDarkAndHueDifferences() {
        listOf(0xFFFFFFFF.toInt(), 0xFFFF0000.toInt()).forEach { color ->
            val pixels = solid(0xFF002288.toInt())
            for (y in 0 until side) for (x in side / 2 until side) pixels[y * side + x] = color
            assertFalse(accepts(pixels))
        }
    }

    @Test
    fun rejectsOneContrastingCorner() {
        val pixels = solid(0xFF222222.toInt())
        for (y in 0 until 8) for (x in 0 until 8) pixels[y * side + x] = 0xFFFFFFFF.toInt()
        assertFalse(accepts(pixels))
    }

    @Test
    fun ignoresArtistInCenterAndIsolatedCornerNoise() {
        val pixels = solid(0xFF222222.toInt())
        for (y in 12 until 52) for (x in 12 until 52) pixels[y * side + x] = 0xFFFFFFFF.toInt()
        pixels[0] = 0xFFFFFFFF.toInt()
        assertTrue(accepts(pixels))
    }

    @Test
    fun rejectsTransparentBackground() {
        assertFalse(accepts(solid(0)))
    }

    private data class Candidate(val source: ArtistArtworkSource, val uniform: Boolean)
    private fun select(vararg candidates: Candidate) = preferredArtistArtwork(candidates.toList(), { it.source }, { it.uniform })?.source

    @Test
    fun prefersAppleRegardlessOfCompletionOrder() {
        assertEquals(ArtistArtworkSource.APPLE_MUSIC, select(
            Candidate(ArtistArtworkSource.SPOTIFY, true), Candidate(ArtistArtworkSource.YOUTUBE_MUSIC, true),
            Candidate(ArtistArtworkSource.APPLE_MUSIC, true)))
    }

    @Test
    fun skipsRejectedOrMissingAppleAndSpotify() {
        assertEquals(ArtistArtworkSource.SPOTIFY, select(
            Candidate(ArtistArtworkSource.APPLE_MUSIC, false), Candidate(ArtistArtworkSource.SPOTIFY, true),
            Candidate(ArtistArtworkSource.YOUTUBE_MUSIC, true)))
        assertEquals(ArtistArtworkSource.SPOTIFY, select(Candidate(ArtistArtworkSource.SPOTIFY, true)))
        assertEquals(ArtistArtworkSource.YOUTUBE_MUSIC, select(
            Candidate(ArtistArtworkSource.APPLE_MUSIC, false), Candidate(ArtistArtworkSource.SPOTIFY, false),
            Candidate(ArtistArtworkSource.YOUTUBE_MUSIC, false)))
    }

    @Test
    fun returnsNoArtworkWhenAllSourcesAreMissingOrRejected() {
        assertNull(select())
        assertNull(select(Candidate(ArtistArtworkSource.APPLE_MUSIC, false), Candidate(ArtistArtworkSource.SPOTIFY, false)))
    }
}
