package com.metrolist.music.ui.component

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ArtistArtworkPresentationTest {
    private var time = 0L
    private val presentation = ArtistArtworkPresentation { time }

    @Test
    fun tileAndDetailKeepSameSourceWhileBackgroundSelectionChanges() {
        assertEquals("youtube", presentation.choose("artist", "youtube"))
        presentation.retain("artist", "youtube")
        assertEquals("youtube", presentation.choose("artist", "apple"))
        presentation.retain("artist", "youtube")
        presentation.release("artist", "youtube")
        time = 60_000_000_000L
        assertEquals("youtube", presentation.current("artist"))
    }

    @Test
    fun navigationGapDoesNotChangePhotoButLaterVisitsCanUseApple() {
        presentation.choose("artist", "youtube")
        presentation.retain("artist", "youtube")
        presentation.release("artist", "youtube")
        time = 1_000_000_000L
        assertEquals("youtube", presentation.choose("artist", "apple"))
        time = 11_000_000_000L
        assertEquals("apple", presentation.choose("artist", "apple"))
    }

    @Test
    fun artistsDoNotSharePhotosAndMissingPhotoDoesNotPinEmptySelection() {
        assertNull(presentation.choose("a", null))
        assertEquals("apple-a", presentation.choose("a", "apple-a"))
        assertEquals("spotify-b", presentation.choose("b", "spotify-b"))
        assertEquals("apple-a", presentation.current("a"))
    }

    @Test
    fun finalSelectionPromotesVisibleFallbackAndIgnoresItsLateRelease() {
        presentation.choose("artist", "youtube")
        presentation.retain("artist", "youtube")
        presentation.select("artist", "apple")
        presentation.retain("artist", "apple")
        presentation.release("artist", "youtube")
        time = 60_000_000_000L
        assertEquals("apple", presentation.choose("artist", "spotify"))
    }
}
