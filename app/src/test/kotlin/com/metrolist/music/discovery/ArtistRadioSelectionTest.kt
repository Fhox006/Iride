package com.metrolist.music.discovery

import com.metrolist.music.models.HeroCarouselItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ArtistRadioSelectionTest {
    private fun radio(index: Int, cover: String? = null) =
        HeroCarouselItem.ArtistRadio("UC$index", "Artist $index", cover)

    @Test
    fun prefersUnseenArtistsWhenEnoughAreAvailable() {
        val previous = (0 until 10).map { radio(it).artistId }.toSet()
        val result = selectArtistRadios((0 until 30).map { radio(it) }, previous, 42)
        assertEquals(10, result.size)
        assertTrue(result.none { it.artistId in previous })
    }

    @Test
    fun fillsSmallPoolsWithoutDuplicatesAndKeepsFreshArtistsFirst() {
        val result = selectArtistRadios(listOf(radio(0), radio(1), radio(1), radio(2)), setOf("UC0"), 42)
        assertEquals(3, result.size)
        assertEquals("UC0", result.last().artistId)
        assertEquals(3, result.map { it.artistId }.toSet().size)
    }

    @Test
    fun keepsAvailablePhotoWhenAnEarlierDuplicateHasNoPhoto() {
        val result = selectArtistRadios(listOf(radio(0, " "), radio(0, "https://example.com/photo")), emptySet(), 42)
        assertEquals("https://example.com/photo", result.single().coverUrl)
    }

    @Test
    fun excludesInvalidArtistsAndHandlesEmptyPools() {
        val invalid = listOf(radio(0).copy(artistId = "local"), radio(1).copy(artistName = " "))
        assertTrue(selectArtistRadios(invalid, emptySet(), 42).isEmpty())
        assertTrue(selectArtistRadios(emptyList(), emptySet(), 42).isEmpty())
    }

    @Test
    fun changesTheLeadingArtistEvenWhenTheSmallPoolIsUnchanged() {
        val candidates = (0 until 3).map { radio(it) }
        val previous = selectArtistRadios(candidates, emptySet(), 42)
        val result = selectArtistRadios(candidates, previous.map { it.artistId }.toSet(), 42, previous.first().artistId)
        assertFalse(previous.first().artistId == result.first().artistId)
        assertEquals(previous.toSet(), result.toSet())
    }

    @Test
    fun keepsTheOnlyAvailableArtist() {
        assertEquals(listOf(radio(0)), selectArtistRadios(listOf(radio(0)), setOf("UC0"), 42, "UC0"))
    }

    @Test
    fun seedIsStableWithinASessionAndChangesOrderBetweenSessions() {
        val candidates = (0 until 30).map { radio(it) }
        assertEquals(selectArtistRadios(candidates, emptySet(), 42), selectArtistRadios(candidates, emptySet(), 42))
        assertFalse(selectArtistRadios(candidates, emptySet(), 42) == selectArtistRadios(candidates, emptySet(), 43))
    }
}
