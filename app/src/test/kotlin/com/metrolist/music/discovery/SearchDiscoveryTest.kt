package com.metrolist.music.discovery

import com.metrolist.innertube.models.Artist
import com.metrolist.innertube.models.ArtistItem
import com.metrolist.innertube.models.SongItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchDiscoveryTest {
    private fun song(id: String, title: String = "Track $id") = SongItem(
        id = id,
        title = title,
        artists = listOf(Artist("Artist", "UCartist")),
        thumbnail = "",
    )

    private fun artist(id: String) = ArtistItem(
        id = id,
        title = id,
        thumbnail = null,
        shuffleEndpoint = null,
        radioEndpoint = null,
    )

    @Test
    fun recognizesTitlesDespiteCaseAccentsAndPunctuation() {
        val track = song("audio", "Déjà Vu!")
        assertEquals(track, SearchDiscovery.recognizeSong("  DEJA   VU  ", listOf(track), null))
    }

    @Test
    fun recognizesTitleWithArtistInEitherOrder() {
        val track = song("audio", "My Song")
        assertEquals(track, SearchDiscovery.recognizeSong("My Song Artist", listOf(track), null))
        assertEquals(track, SearchDiscovery.recognizeSong("Artist My Song", listOf(track), null))
    }

    @Test
    fun findsAudioCounterpartOfTopVideo() {
        val track = song("audio", "My Song")
        val video = track.copy(id = "video", musicVideoType = "MUSIC_VIDEO_TYPE_OMV")
        assertEquals(track, SearchDiscovery.recognizeSong("My Song official", listOf(track), video))
        assertNull(SearchDiscovery.recognizeSong("My Song", listOf(video), video))
    }

    @Test
    fun doesNotTreatBroadOrEmptyQueriesAsRecognizedSongs() {
        val tracks = listOf(song("audio", "Rock and Roll"))
        assertNull(SearchDiscovery.recognizeSong("rock", tracks, null))
        assertNull(SearchDiscovery.recognizeSong("", tracks, tracks.first()))
    }

    @Test
    fun hidesOnlyTopSongsDuplicatedInFirstFour() {
        val tracks = (0..5).map { song(it.toString()) }
        assertTrue(SearchDiscovery.duplicatesFirstSongs(tracks[3], tracks))
        assertFalse(SearchDiscovery.duplicatesFirstSongs(tracks[4], tracks))
        assertFalse(SearchDiscovery.duplicatesFirstSongs(artist("UCartist"), tracks))
    }

    @Test
    fun keepsMatchesFirstThenRecommendationsWithoutVideosEpisodesOrDuplicates() {
        val matches = (0..5).map { song(it.toString()) }
        val recommendations = listOf(
            matches.first(), song("related"), song("related"),
            song("video").copy(musicVideoType = "MUSIC_VIDEO_TYPE_OMV"),
            song("episode").copy(isEpisode = true),
        )
        val result = SearchDiscovery.songs(matches, matches[1], recommendations)
        assertEquals(listOf("1", "0", "2", "3", "related", "4", "5"), result.map { it.id })
    }

    @Test
    fun boundsRecommendationShelf() {
        val result = SearchDiscovery.songs(emptyList(), null, (0..99).map { song(it.toString()) })
        assertEquals(40, result.size)
    }

    @Test
    fun similarArtistsExcludeCreditedArtistsProfilesAndDuplicates() {
        val result = SearchDiscovery.similarArtists(
            listOf(artist("UCcredited"), artist("UCsimilar"), artist("UCsimilar"), artist("UCprofile").copy(isProfile = true)),
            setOf("UCcredited"),
        )
        assertEquals(listOf("UCsimilar"), result.map { it.id })
    }
}
