package com.metrolist.music.ui.component

import com.metrolist.music.db.entities.Playlist
import com.metrolist.music.db.entities.PlaylistEntity
import com.metrolist.music.utils.playlistCoverId
import org.junit.Assert.assertEquals
import org.junit.Test

class PlaylistArtworkAnalysisTest {
    @Test fun officialCoverWinsEvenWithFourDifferentSongCovers() {
        val playlist = Playlist(PlaylistEntity(id = "test", name = "Test", thumbnailUrl = "official"),
            songCount = 4, songThumbnails = listOf("a", "b", "c", "d"))
        assertEquals(listOf("official"), playlist.thumbnails)
    }

    @Test fun missingCoverPreservesSongOrderAndRepeatedArtwork() {
        val playlist = Playlist(PlaylistEntity(id = "test", name = "Test"),
            songCount = 4, songThumbnails = listOf("a", null, "a", "b"))
        assertEquals(listOf("a", "a", "b"), playlist.thumbnails)
    }

    @Test fun customPhotoUsesSameIdentityInCardHeaderAndMenu() {
        assertEquals(playlistCoverId("liked"), playlistCoverId("LP_LIKED"))
        assertEquals(playlistCoverId("downloaded"), playlistCoverId("LP_DOWNLOADED"))
        assertEquals(playlistCoverId("VLPL123"), playlistCoverId("PL123"))
        assertEquals(playlistCoverId("50"), playlistCoverId("top_playlist/50"))
        assertEquals("LP_local", playlistCoverId("LP_local"))
    }

}
