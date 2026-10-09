package com.metrolist.music.ui.menu

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SongMenuNavigationTest {
    @Test
    fun freeSongKeepsArtistAndAlbumNavigation() {
        val navigation = navigation(listOf("main"))
        assertEquals(listOf("main"), navigation.artistIds)
        assertEquals(SongMenuArtistLabel.Artist, navigation.artistLabel)
        assertTrue(navigation.showAlbum)
    }

    @Test
    fun freeCollaborationUsesPluralArtistLabelWithoutDroppingAnyone() {
        val navigation = navigation(listOf("main", "guest", "main"))
        assertEquals(listOf("main", "guest"), navigation.artistIds)
        assertEquals(SongMenuArtistLabel.Artists, navigation.artistLabel)
    }

    @Test
    fun artistPageHidesCurrentArtistAndKeepsAlbum() {
        val navigation = navigation(listOf("main"), artist = "main")
        assertTrue(navigation.artistIds.isEmpty())
        assertTrue(navigation.showAlbum)
    }

    @Test
    fun artistPageShowsOnlyOtherCollaboratorsRegardlessOfCreditOrder() {
        val navigation = navigation(listOf("main", "guest", "other"), artist = "guest")
        assertEquals(listOf("main", "other"), navigation.artistIds)
        assertEquals(SongMenuArtistLabel.Collaborators, navigation.artistLabel)
    }

    @Test
    fun albumPageHidesAlbumAndCreditedAlbumArtistsButKeepsGuests() {
        val navigation = navigation(listOf("main", "joint", "guest"), album = "album", albumArtists = setOf("main", "joint"))
        assertFalse(navigation.showAlbum)
        assertEquals(listOf("guest"), navigation.artistIds)
        assertEquals(SongMenuArtistLabel.Collaborators, navigation.artistLabel)
    }

    @Test
    fun albumPageSuppressesPrimaryArtistWhileAlbumMetadataLoads() {
        val navigation = navigation(listOf("main", "guest"), album = "album")
        assertFalse(navigation.showAlbum)
        assertEquals(listOf("guest"), navigation.artistIds)
    }

    @Test
    fun singleArtistAlbumHasNoRedundantNavigation() {
        val navigation = navigation(listOf("main"), album = "album")
        assertTrue(navigation.artistIds.isEmpty())
        assertFalse(navigation.showAlbum)
    }

    @Test
    fun missingAlbumNeverCreatesDeadAlbumAction() {
        assertFalse(songMenuNavigation(listOf("main"), null, null, null, emptySet()).showAlbum)
    }

    private fun navigation(
        artists: List<String>,
        artist: String? = null,
        album: String? = null,
        albumArtists: Set<String> = emptySet(),
    ) = songMenuNavigation(artists, "album", artist, album, albumArtists)
}
