package com.metrolist.music.playback.queues

import com.metrolist.music.models.MediaMetadata
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AlbumRecommendationFilterTest {
    private val song = MediaMetadata(
        id = "album-track",
        title = "Déjà vu",
        artists = listOf(MediaMetadata.Artist("artist", "Test Artist")),
        duration = 180,
    )

    @Test fun excludesEveryAlbumTrackByIdAcrossBatches() {
        val second = song.copy(id = "second-track", title = "Another song")
        val filter = AlbumRecommendationFilter(listOf(song, second))
        assertFalse(filter.accept(song))
        assertTrue(filter.accept(song.copy(id = "new", title = "New song")))
        assertFalse(filter.accept(second.copy(title = "Different metadata")))
        assertFalse(filter.accept(song.copy(id = "new", title = "New song")))
    }

    @Test fun excludesAudioAndVideoEditionsWithDifferentIds() {
        val filter = AlbumRecommendationFilter(listOf(song))
        assertFalse(filter.accept(song.copy(id = "other-audio")))
        assertFalse(filter.accept(song.copy(id = "video", title = "Deja vu (Official Music Video)")))
        assertFalse(filter.accept(song.copy(id = "prefixed-video", title = "Test Artist - Déjà vu - Official Video")))
        assertFalse(filter.accept(song.copy(id = "topic", artists = listOf(MediaMetadata.Artist(null, "Test Artist - Topic")))))
    }

    @Test fun keepsOtherSongsAndOtherArtistsWithTheSameTitle() {
        val filter = AlbumRecommendationFilter(listOf(song))
        assertTrue(filter.accept(song.copy(id = "other-song", title = "A different song")))
        assertTrue(filter.accept(song.copy(id = "other-artist", artists = listOf(MediaMetadata.Artist(null, "Other Artist")))))
        assertTrue(filter.accept(song.copy(id = "live", title = "Déjà vu (Live)")))
    }

    @Test fun matchesSharedArtistWithoutRequiringIdenticalCredits() {
        val filter = AlbumRecommendationFilter(listOf(song.copy(artists = song.artists + MediaMetadata.Artist(null, "Guest"))))
        assertFalse(filter.accept(song.copy(id = "video")))
    }

    @Test fun emptyAlbumAcceptsRecommendationsAndMissingCreditsDoNotMatchUnrelatedTracks() {
        assertTrue(AlbumRecommendationFilter(emptyList()).accept(song))
        val filter = AlbumRecommendationFilter(listOf(song.copy(artists = emptyList())))
        assertTrue(filter.accept(song.copy(id = "other")))
        assertFalse(filter.accept(song))
    }
}
