package com.metrolist.music.playback

import com.metrolist.innertube.models.Artist
import com.metrolist.innertube.models.SongItem
import com.metrolist.music.models.MediaMetadata
import org.junit.Assert.assertNull
import org.junit.Assert.assertEquals
import org.junit.Test

class VideoEditionTest {
    @Test fun videoFavoritesUseTheAudioTrackAndRejectUnresolvedOrStaleMatches() {
        val audio = MediaMetadata("audio", "Song", emptyList(), 180)
        val video = audio.copy(id = "video", musicVideoType = "MUSIC_VIDEO_TYPE_OMV")
        assertEquals(audio, favoriteMetadataForEdition(video, video.id, audio))
        assertEquals(audio, favoriteMetadataForEdition(audio, video.id, null))
        assertNull(favoriteMetadataForEdition(video, video.id, null))
        assertNull(favoriteMetadataForEdition(video, "previous-video", audio))
        assertNull(favoriteMetadataForEdition(video, video.id, video))
        assertNull(favoriteMetadataForEdition(null, video.id, audio))
    }

    @Test fun mapsThePlaybackFractionBetweenDifferentEditions() {
        assertEquals(120_000L, proportionalVideoPosition(90_000, 180_000, 240_000))
        assertEquals(67_500L, proportionalVideoPosition(90_000, 240_000, 180_000))
    }

    @Test fun clampsUnknownDurationsAndOutOfRangePositions() {
        assertEquals(0L, proportionalVideoPosition(-100, 180_000, 240_000))
        assertEquals(239_999L, proportionalVideoPosition(999_999, 180_000, 240_000))
        assertEquals(60_000L, proportionalVideoPosition(60_000, -1, 240_000))
        assertEquals(60_000L, proportionalVideoPosition(60_000, 180_000, -1))
        assertEquals(0L, proportionalVideoPosition(1, 1, 1))
    }

    @Test fun matchingArtistWinsOverUnrelatedSameTitle() {
        val source = MediaMetadata("audio", "Déjà vu", listOf(MediaMetadata.Artist("artist", "Test Artist")), 180)
        val unrelated = SongItem("unrelated", "Deja vu", listOf(Artist("Someone Else", null)), duration = 180, thumbnail = "")
        val correct = SongItem("correct", "Déjà vu (Official Video)", listOf(Artist("Test Artist", "artist")), duration = 195, thumbnail = "")
        val correctVideo = correct.copy(musicVideoType = "MUSIC_VIDEO_TYPE_UGC")
        val unrelatedVideo = unrelated.copy(musicVideoType = "MUSIC_VIDEO_TYPE_UGC")
        assertEquals(listOf(correctVideo, unrelatedVideo), rankVideoEditions(source, listOf(unrelatedVideo, correctVideo, correctVideo)))
    }

    @Test fun similarDurationWinsBetweenSameArtistEditions() {
        val source = MediaMetadata("audio", "Song", listOf(MediaMetadata.Artist(null, "Artist")), 180)
        val studio = SongItem("studio", "Song", listOf(Artist("Artist", null)), duration = 181, thumbnail = "", musicVideoType = "MUSIC_VIDEO_TYPE_UGC")
        val extended = studio.copy(id = "extended", title = "Song (Extended)", duration = 400)
        assertEquals(studio, rankVideoEditions(source, listOf(extended, studio)).first())
    }

    @Test fun artistPrefixInVideoTitleDoesNotFavorACoverOverTheOriginalSong() {
        val source = MediaMetadata("video", "Test Artist - Song (Official Video)",
            listOf(MediaMetadata.Artist("artist", "Test Artist")), 180)
        val cover = SongItem("cover", "Test Artist - Song", listOf(Artist("Cover Channel", null)), duration = 180, thumbnail = "", musicVideoType = "MUSIC_VIDEO_TYPE_UGC")
        val original = SongItem("original", "Song", listOf(Artist("Test Artist", "artist")), duration = 180, thumbnail = "", musicVideoType = "MUSIC_VIDEO_TYPE_UGC")
        assertEquals(original, rankVideoEditions(source, listOf(cover, original)).first())
    }

    @Test fun videoRankingRejectsAudioOnlyResults() {
        val source = MediaMetadata("audio", "Song", emptyList(), 180)
        val audio = SongItem("audio", "Song", emptyList(), duration = 180, thumbnail = "", musicVideoType = "MUSIC_VIDEO_TYPE_ATV")
        val video = audio.copy(id = "video", musicVideoType = "MUSIC_VIDEO_TYPE_UGC")
        assertEquals(listOf(video), rankVideoEditions(source, listOf(audio, video)))
    }

    @Test fun usesOriginalVideoThumbnailWithoutSquareCropParameters() {
        assertEquals("https://i.ytimg.com/vi/video/maxresdefault.jpg",
            youtubeVideoThumbnail("video", "https://i.ytimg.com/vi/video/maxresdefault.jpg?sqp=square"))
        assertEquals("https://i.ytimg.com/vi/video/hqdefault.jpg",
            youtubeVideoThumbnail("video", "https://lh3.googleusercontent.com/album=w544-h544"))
        assertEquals("https://i.ytimg.com/vi/video/hqdefault.jpg",
            youtubeVideoThumbnail("video", "https://i.ytimg.com/vi/video/hqdefault.jpg=w512-h512-l90-rj"))
    }
    @Test fun audioMatchRejectsUnrelatedTitlesAndArtists() {
        val source = MediaMetadata("video", "Test Artist - Song (Official Video)",
            listOf(MediaMetadata.Artist(null, "Test Artist")), 180)
        val unrelated = SongItem("other", "Another title", listOf(Artist("Test Artist", null)), duration = 180, thumbnail = "")
        val sameTitle = unrelated.copy(id = "same-title", title = "Song", artists = listOf(Artist("Someone Else", null)))
        assertNull(matchingAudioEdition(source, listOf(unrelated, sameTitle)))
        assertNull(matchingAudioEdition(source, emptyList()))
    }

    @Test fun audioMatchRejectsVideoResultsAndVeryDifferentDurations() {
        val source = MediaMetadata("video", "Song", listOf(MediaMetadata.Artist(null, "Artist")), 180)
        val extended = SongItem("extended", "Song", listOf(Artist("Artist", null)), duration = 600, thumbnail = "")
        val video = extended.copy(id = "another-video", duration = 180, musicVideoType = "MUSIC_VIDEO_TYPE_OMV")
        assertNull(matchingAudioEdition(source, listOf(extended, video)))
    }

    @Test fun audioMatchFindsTrackBeforeVideoIsClosed() {
        val source = MediaMetadata("video", "Test Artist - Song (Official Video)",
            listOf(MediaMetadata.Artist(null, "Test Artist")), 195)
        val track = SongItem("audio", "Song", listOf(Artist("Test Artist", null)), duration = 180,
            thumbnail = "album-cover", musicVideoType = "MUSIC_VIDEO_TYPE_ATV")
        assertEquals(track, matchingAudioEdition(source, listOf(track)))
        assertEquals(track.copy(duration = null), matchingAudioEdition(source.copy(duration = -1), listOf(track.copy(duration = null))))
    }

}
