package com.metrolist.music.utils.spotify

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SpotifyExportParserTest {
    private fun parse(csv: String) = SpotifyExportParser.parse(
        csv.trimIndent().byteInputStream(), "My_Playlist.csv",
    ).playlists.single()

    @Test
    fun playlistedKeepsArtistNamesAndConvertsClockDurations() {
        val playlist = parse("""
            Title,Artist,Source Platform,Source URL,Duration
            Song,"Earth, Wind & Fire",Apple Music,https://music.apple.com/song,3:42
            Long Song,Artist,Apple Music,https://music.apple.com/long,1:02:03
        """)
        assertEquals("My Playlist", playlist.name)
        assertEquals(listOf("Earth, Wind & Fire"), playlist.tracks[0].artists)
        assertEquals(222, playlist.tracks[0].durationSeconds)
        assertEquals(3723, playlist.tracks[1].durationSeconds)
        assertEquals("https://music.apple.com/song", playlist.tracks[0].spotifyUri)
    }

    @Test
    fun invalidClockDurationsKeepTracksWithoutInventingTiming() {
        for (duration in listOf("", "3:60", "-1:20", "0:00", "2147483647:59:59", "1234")) {
            val track = parse("""
                Title,Artist,Source Platform,Source URL,Duration
                Song,Artist,Apple Music,https://music.apple.com/song,$duration
            """).tracks.single()
            assertEquals("Song", track.title)
            assertNull(duration, track.durationSeconds)
        }
    }

    @Test
    fun exportifyStillUsesMillisecondsAndSeparateArtists() {
        val track = parse("""
            Track Name,Artist Name(s),Duration (ms),Track URI
            Song,Artist One;Artist Two,222000,spotify:track:example
        """).tracks.single()
        assertEquals(222, track.durationSeconds)
        assertEquals(listOf("Artist One", "Artist Two"), track.artists)
        assertEquals("spotify:track:example", track.spotifyUri)
    }
}
