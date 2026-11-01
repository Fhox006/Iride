package com.metrolist.music.ui.screens.search

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SearchGenreArtworkQueryTest {
    @Test
    fun recognizesEveryGenreWithoutCollapsingSubgenres() {
        SearchGenrePersonalization.catalog.forEach {
            assertEquals(it.name, searchGenreArtworkName(it.name))
        }
        assertEquals("Deep House", searchGenreArtworkName("  DEEP-HOUSE  "))
        assertEquals("Drift Phonk", searchGenreArtworkName("Drift Phonk"))
        assertEquals("Rap", searchGenreArtworkName("Hip Hop"))
        assertEquals("R&B", searchGenreArtworkName("Rhythm and Blues"))
    }

    @Test
    fun artistAndSongQueriesNeverBecomeGenreBackgrounds() {
        listOf("ASAP Rocky", "Rock Your Body", "Soulja Boy", "Travis Scott Trap", "Radiohead", "").forEach {
            assertNull(searchGenreArtworkName(it))
        }
    }
}
