package com.metrolist.music.ui.screens.search

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchGenrePersonalizationTest {
    @Test
    fun rapListeningBoostsDrillAndTrapAheadOfLessListenedRock() {
        val scores = SearchGenrePersonalization.listeningScores(
            listOf(
                SearchGenreListening(tags = listOf("Rap"), playTimeMs = 60 * 60_000L),
                SearchGenreListening(tags = listOf("Rock"), playTimeMs = 10 * 60_000L),
            ),
        )

        val firstGenres = SearchGenrePersonalization.rank(scores, emptyMap()).take(3).map { it.name }

        assertEquals(listOf("Rap", "Drill", "Trap"), firstGenres)
    }

    @Test
    fun tappingGenreCanRaiseItAboveUnselectedListeningGenre() {
        val scores = SearchGenrePersonalization.listeningScores(
            listOf(SearchGenreListening(tags = listOf("Rap"), playTimeMs = 60 * 60_000L)),
        )

        val ranked = SearchGenrePersonalization.rank(scores, mapOf("Rock" to 5)).map { it.name }

        assertTrue(ranked.indexOf("Rock") < ranked.indexOf("Drill"))
    }

    @Test
    fun regionalTagsMapToBroadGenresInsteadOfCatalogEntries() {
        assertEquals("Rap", SearchGenrePersonalization.canonicalGenre("French Rap"))
        assertEquals("Drill", SearchGenrePersonalization.canonicalGenre("UK Drill"))
        assertFalse(SearchGenrePersonalization.catalog.any { it.name == "French Rap" || it.name == "UK Drill" })
    }
}
