package com.metrolist.music.ui.screens.search

import com.metrolist.innertube.models.Artist
import com.metrolist.innertube.models.ArtistItem
import com.metrolist.innertube.models.SongItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchSuggestionRankingTest {
    @Test
    fun correctedExactFollowedArtistBeatsFuzzyArtistAndPreviousSearchResults() {
        val nayt = ArtistItem("UC_nayt", "Nayt", null, shuffleEndpoint = null, radioEndpoint = null)
        val daytona = ArtistItem("UC_daytona", "Daytona KK", null, shuffleEndpoint = null, radioEndpoint = null)
        val tracks = (1..4).map { song("nayt$it", "Song $it", "Nayt").copy(artists = listOf(Artist("Nayt", nayt.id))) }
        val previous = listOf(daytona, song("wrong", "Ferro Sporco", "Daytona KK"))
        rankSearchSuggestions("night", listOf(nayt, daytona) + tracks, previous)
        assertEquals(listOf(nayt) + tracks, rankSearchSuggestions("Nayt", listOf(daytona, nayt) + tracks, previous))
    }

    private fun song(id: String, title: String, artist: String = "Radiohead") =
        SongItem(id = id, title = title, artists = listOf(Artist(artist, "UC_artist")), thumbnail = "")

    @Test
    fun familiarTracksWinMatchingPrefixesWithoutAddingUnrelatedListening() {
        val familiar = song("known", "Creep")
        val remote = song("unknown", "Creep live")
        val unrelated = song("unrelated", "Everything in its right place")
        assertEquals(listOf(familiar, remote), rankSearchSuggestions("cre", listOf(familiar, unrelated), listOf(remote)))
    }

    @Test
    fun artistCreditsAndTyposStillFindFamiliarTracks() {
        val familiar = song("known", "Creep")
        assertEquals(listOf(familiar), rankSearchSuggestions("crep radio", listOf(familiar), emptyList()))
        assertTrue(rankSearchSuggestions("unknown artist", listOf(familiar), emptyList()).isEmpty())
    }

    @Test
    fun blankDraftDoesNotPromotePersonalTracks() {
        val personal = (1..12).map { song("$it", "Track $it") }
        assertTrue(rankSearchSuggestions("", personal, listOf(song("remote", "Remote"))).isEmpty())
    }

    @Test
    fun selectedTrackStaysFirstAndTopResultsHaveFiveDistinctEntries() {
        val selected = song("selected", "Creep")
        val artist = ArtistItem("UC_artist", "Radiohead", null, shuffleEndpoint = null, radioEndpoint = null)
        val results = (1..12).map { song("$it", "Result $it") }
        val top = searchTopItems(listOf(selected, artist), listOf(selected), results)
        assertEquals(5, top.size)
        assertEquals(selected, top.first())
        assertEquals(artist, top[1])
        assertEquals(5, top.map { it.id }.distinct().size)
    }

    @Test
    fun familiarArtistWinsTypoBeforeTracksMatchingOnlyCredits() {
        val artist = ArtistItem("UC_yugi", "Kid Yugi", null, shuffleEndpoint = null, radioEndpoint = null)
        val tracks = (1..6).map { song("$it", "Track $it", "Kid Yugi") }
        val result = rankSearchSuggestions("kid yugo", tracks + artist, emptyList())
        assertEquals(artist, result.first())
        assertEquals(5, result.size)
        assertEquals(artist, rankSearchSuggestions("kid yu", tracks + artist, emptyList()).first())
    }

    @Test
    fun artistTypoDoesNotCombineWordsFromUnrelatedFeaturedArtists() {
        val artist = ArtistItem("UC_yugi", "Kid Yugi", null, shuffleEndpoint = null, radioEndpoint = null)
        val unrelated = song("wrong", "ELA ME CHAMANDO").copy(
            artists = listOf(Artist("Ugovhb", "UC_ugo"), Artist("Uxie Kid", "UC_kid")),
        )
        val tracks = (1..5).map { song("$it", "Track $it").copy(artists = listOf(Artist("Kid Yugi", artist.id))) }
        val impostor = artist.copy(id = "UC_other", title = "Kid Yugo")
        val result = rankSearchSuggestions("kid yugo", listOf(unrelated) + tracks + artist, listOf(impostor))
        assertEquals(listOf(artist) + tracks.take(4), result)
    }

    @Test
    fun correctedServerRecommendationsKeepOrderWhenNothingFamiliarMatches() {
        val remote = (1..5).map { song("$it", "Server choice $it", "Kid Yugi") }
        assertEquals(remote, rankSearchSuggestions("kid yugo", listOf(song("local", "Creep")), remote))
    }

    @Test
    fun selectedSongReplacesAmbiguousQueryWithItsAuthorAndRelatedMusic() {
        val seed = song("seed", "Il Ferro di Čechov", "Kid Yugi")
        val author = ArtistItem("UC_artist", "Kid Yugi", null, shuffleEndpoint = null, radioEndpoint = null)
        val wrongArtist = song("wrong", "Il ferro", "Tiziano Ferro").copy(artists = listOf(Artist("Tiziano Ferro", "UC_ferro")))
        val known = song("known", "GRAMMELOT", "Kid Yugi")
        val radio = (1..6).map { song("radio$it", "Radio $it", "Guest") }
        val result = searchSongContextItems(seed, listOf(author), listOf(known), listOf(wrongArtist), radio)
        assertEquals(listOf(seed, author, known, radio[0], radio[1]), result)
    }

    @Test
    fun radioFillsSparseSongContextWithoutDuplicateOrEpisode() {
        val seed = song("seed", "Il Ferro di Čechov", "Kid Yugi")
        val radio = listOf(seed, song("episode", "Episode").copy(isEpisode = true)) + (1..8).map { song("$it", "Radio $it") }
        assertEquals(listOf(seed) + radio.drop(2).take(4), searchSongContextItems(seed, emptyList(), emptyList(), emptyList(), radio))
    }

    @Test
    fun differentVideoIdsForTheSameRecordingDoNotConsumeRecommendationSlots() {
        val seed = song("seed", "God's Plan", "Drake")
        val alternate = seed.copy(id = "clean-version")
        val radio = (1..6).map { song("$it", "Radio $it", "Drake") }
        assertEquals(listOf(seed) + radio.take(4), searchSongContextItems(seed, emptyList(), emptyList(), listOf(alternate), radio))
    }
}
