package com.metrolist.music.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ArtistNameMatcherTest {
    private fun match(name: String, vararg candidates: String) =
        ArtistNameMatcher.bestMatch(name, candidates.toList()) { it }

    @Test
    fun prefersExactNameOverSimilarSearchResult() {
        assertEquals("The Weeknd", match("The Weeknd", "The Weekend", "The Weeknd"))
    }

    @Test
    fun handlesAccentsPunctuationAndTopicSuffix() {
        assertEquals("Beyoncé - Topic", match("BEYONCE", "Beyoncé - Topic"))
        assertEquals("AC/DC", match("AC DC", "AC/DC"))
        assertEquals("Måneskin", match("Maneskin", "Måneskin"))
    }

    @Test
    fun acceptsSmallTypoWithClearWinner() {
        assertEquals("The Weeknd", match("The Weknd", "The Weeknd", "Ariana Grande"))
        assertEquals("Ariana Grande", match("Arianna Grande", "Ariana Grande"))
    }

    @Test
    fun rejectsUnrelatedArtistsAndSubstringMatches() {
        assertNull(match("Drake", "Nick Drake", "Kendrick Lamar"))
        assertNull(match("Coldplay", "Cold War Kids"))
        assertNull(match("Muse", "Mura"))
    }

    @Test
    fun rejectsAmbiguousFuzzyMatches() {
        assertNull(match("The Weknd", "The Weeknd", "The Wekand"))
    }

    @Test
    fun neverFuzzilyMatchesShortNames() {
        assertNull(match("SIA", "SZA"))
        assertEquals("Sia", match("SIA", "SZA", "Sia"))
    }

    @Test
    fun handlesMissingAndEmptyNames() {
        assertNull(match("Artist"))
        assertNull(match(" ", "Artist"))
        assertNull(match("!!!", "???"))
    }
}
