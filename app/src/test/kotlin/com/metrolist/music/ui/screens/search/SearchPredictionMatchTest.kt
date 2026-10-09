package com.metrolist.music.ui.screens.search

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class SearchPredictionMatchTest {
    @Test
    fun completesPrefixesAcrossWordsAndIgnoresAccents() {
        assertEquals(0, searchPredictionScore("beyo", "Beyoncé"))
        assertNotNull(searchPredictionScore("taylor swi", "Taylor Swift"))
        assertNotNull(searchPredictionScore("swift tay", "Taylor Swift"))
    }

    @Test
    fun acceptsOneTypoAndTransposedLettersOnlyForLongEnoughInput() {
        assertNotNull(searchPredictionScore("adelw", "Adele"))
        assertNotNull(searchPredictionScore("taylor swft", "Taylor Swift"))
        assertNotNull(searchPredictionScore("adeel", "Adele"))
        assertNull(searchPredictionScore("adl", "Adele"))
    }

    @Test
    fun albumsAndSongsMayMatchTheirArtistButNeverUnrelatedWords() {
        assertNotNull(searchPredictionScore("adele album", "30", "Adele album disco"))
        assertNotNull(searchPredictionScore("adele hel", "Hello", "Adele brano song"))
        assertNull(searchPredictionScore("adele", "Enter Sandman", "Metallica song"))
        assertNull(searchPredictionScore("adele coldplay", "Adele", "artist"))
        assertNull(searchPredictionScore("", "Adele"))
        assertNull(searchPredictionScore("a", "Billie Eilish", "artista artist cantante"))
        assertNull(searchPredictionScore("a artist", "Billie Eilish", "artista artist cantante"))
        assertNull(searchPredictionScore("al", "30", "Adele album disco"))
    }

    @Test
    fun titleMatchesRankBeforeRelatedArtistsOrTypoMatches() {
        assertEquals(0, searchPredictionScore("adele", "Adele"))
        assertEquals(2, searchPredictionScore("adele", "Hello", "Adele"))
        assertEquals(3, searchPredictionScore("adelw", "Adele"))
    }
}
