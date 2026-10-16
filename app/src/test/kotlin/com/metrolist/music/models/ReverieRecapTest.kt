package com.metrolist.music.models

import com.metrolist.music.db.entities.AlbumEntity
import com.metrolist.music.db.entities.ArtistEntity
import com.metrolist.music.db.entities.CategoryStats
import com.metrolist.music.db.entities.Event
import com.metrolist.music.db.entities.EventWithSong
import com.metrolist.music.db.entities.Song
import com.metrolist.music.db.entities.SongEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.YearMonth

class ReverieRecapTest {
    @Test fun genresComeFromPerSongTagsAndAreWeightedByActualListening() {
        val songs = createReverieRecap(month, listOf(event("one", 1, 180_000), event("two", 1, 60_000))).songs
        val ranked = rankReverieGenres(songs, mapOf("one" to listOf("Rock", "rock", "Alternative"), "two" to listOf("Jazz", "Rock")))
        assertEquals(listOf("Rock", "Alternative", "Jazz"), ranked.map { it.name })
        assertEquals(240_000, ranked.first().milliseconds)
        assertEquals(2, ranked.first().songCount)
        assertEquals("one", ranked.first().soundtrack.id)
        assertTrue(rankReverieGenres(songs, emptyMap()).isEmpty())
    }
    private val month = YearMonth.of(2028, 2)
    private val artist = ArtistEntity("artist", "Artist")
    private val album = AlbumEntity(id = "album", title = "Album", songCount = 2, duration = 400)
    private fun event(id: String, day: Int, playTime: Long, video: Boolean = false, explicit: Boolean = false,
                      episode: Boolean = false, artists: List<ArtistEntity> = listOf(artist), inAlbum: Boolean = true) = EventWithSong(
        Event(songId = id, timestamp = month.atDay(day).atTime(12, 0), playTime = playTime),
        Song(SongEntity(id, id, duration = 200, isVideo = video, explicit = explicit, isEpisode = episode),
            artists, album = album.takeIf { inAlbum }),
    )

    @Test fun usesFullCalendarMonthIncludingLeapDayAndExcludesAdjacentMonths() {
        val first = event("first", 1, 60_000)
        val last = event("last", 29, 120_000)
        val recap = createReverieRecap(month, listOf(
            first.copy(event = first.event.copy(timestamp = month.atDay(1).atStartOfDay())), last,
            first.copy(event = first.event.copy(timestamp = month.minusMonths(1).atEndOfMonth().atTime(23, 59))),
            last.copy(event = last.event.copy(timestamp = month.plusMonths(1).atDay(1).atStartOfDay())),
        ))
        assertEquals(3, recap.minutes)
        assertEquals(2, recap.plays)
        assertEquals(29, recap.dailyMilliseconds.size)
        assertEquals(29, recap.peakDay)
        assertEquals(2, recap.activeDays)
    }

    @Test fun ranksByRealListeningTimeAndUsesDeterministicTies() {
        val recap = createReverieRecap(month, listOf(event("z", 1, 1), event("a", 1, 1), event("b", 2, 90_000), event("a", 2, 1)))
        assertEquals(listOf("b", "a", "z"), recap.songs.map { it.id })
        assertEquals(2, recap.songs[1].plays)
        assertEquals(90_003, recap.milliseconds)
    }

    @Test fun filtersPodcastVideoExplicitAndNonListeningEventsBeforeAllMetrics() {
        val recap = createReverieRecap(month, listOf(
            event("music", 1, 60_000), event("video", 2, 120_000, video = true),
            event("explicit", 3, 120_000, explicit = true), event("podcast", 4, 600_000, episode = true),
            event("zero", 5, 0), event("negative", 5, -100),
        ), hideVideoSongs = true, hideExplicit = true)
        assertEquals(listOf("music"), recap.songs.map { it.id })
        assertEquals(1, recap.minutes)
        assertEquals(1, recap.activeDays)
        assertEquals(1, recap.plays)
        assertEquals(60_000, recap.artists.single().milliseconds)
        assertEquals(60_000, recap.albums.single().milliseconds)
    }

    @Test fun collaborationsCreditArtistsWithoutDoubleCountingTotalListening() {
        val guest = ArtistEntity("guest", "Guest")
        val recap = createReverieRecap(month, listOf(event("duet", 1, 60_000, artists = listOf(artist, guest, guest))))
        assertEquals(60_000, recap.milliseconds)
        assertEquals(2, recap.artists.size)
        assertTrue(recap.artists.all { it.milliseconds == 60_000L && it.plays == 1 })
    }

    @Test fun shortHistoryKeepsTheOpeningPairAndNeverLeavesAChapterSilent() {
        val recap = createReverieRecap(month, listOf(
            event("third", 1, 10_000), event("second", 1, 20_000), event("winner", 2, 90_000),
        ))
        assertEquals("winner", recap.soundtrack(ReverieChapter.OVERTURE)?.id)
        assertEquals("winner", recap.soundtrack(recap.chapters[1])?.id)
        assertEquals("second", recap.soundtrack(recap.chapters[2])?.id)
        assertEquals("winner", recap.soundtrack(ReverieChapter.SONG)?.id)
        assertTrue(recap.chapters.all { recap.soundtrack(it) != null })
    }

    @Test fun soundtrackBalancesArtistsEvenWhenOneDominatesTheMonthlyRanking() {
        val others = listOf(ArtistEntity("b", "B"), ArtistEntity("c", "C"))
        val recap = createReverieRecap(month,
            (1..12).map { event("lead-$it", 1, 1_000_000L - it) } +
                others.flatMap { performer -> (1..5).map {
                    event("${performer.id}-$it", 1, 1_000L - it, artists = listOf(performer), inAlbum = false)
                } },
        )
        val selected = recap.chapters.mapNotNull { recap.soundtrack(it) }.distinctBy { it.id }
        val counts = selected.groupingBy { it.artists.first().id }.eachCount()
        assertTrue(counts.values.all { it <= 3 })
        assertTrue(recap.soundtrack(ReverieChapter.OVERTURE)?.artists?.first()?.id !=
            recap.soundtrack(recap.chapters[2])?.artists?.first()?.id)
        assertEquals("lead-1", recap.soundtrack(ReverieChapter.SONG)?.id)
        assertEquals("artist", recap.soundtrack(ReverieChapter.ARTIST)?.artists?.first()?.id)
        assertEquals("album", recap.soundtrack(ReverieChapter.ALBUM)?.album?.id)
    }

    @Test fun editorialSlidesSharePairsWhileArtistAlbumAndSongHaveDedicatedTracks() {
        val basic = createReverieRecap(month, (1..12).map { event("track-$it", 1, it * 60_000L) },
            listOf(CategoryStats("Mood", "#123456", 12, 100_000)))
        val recap = basic.copy(genres = rankReverieGenres(basic.songs, basic.songs.associate { it.id to listOf("Rock") }))
        val tracks = recap.chapters.mapNotNull { recap.soundtrack(it)?.id }
        assertEquals(9, tracks.size)
        assertEquals(7, tracks.distinct().size)
        assertEquals(tracks[0], tracks[1])
        assertEquals(tracks[2], tracks[3])
        assertFalse(tracks[1] == tracks[2])
        assertFalse(tracks[3] == tracks[4])
        assertEquals("track-12", recap.soundtrack(ReverieChapter.SONG)?.id)
        assertEquals("artist", recap.soundtrack(ReverieChapter.ARTIST)?.artists?.first()?.id)
        assertEquals("album", recap.soundtrack(ReverieChapter.ALBUM)?.album?.id)
        assertTrue(tracks.all { id -> recap.songs.any { it.id == id } })
        assertEquals(tracks, recap.chapters.mapNotNull { recap.soundtrack(it)?.id })
    }

    @Test fun aSingleSongKeepsEverySupportedChapterAudible() {
        val recap = createReverieRecap(month, listOf(event("only", 1, 90_000)))
        assertEquals(listOf("only"), recap.chapters.mapNotNull { recap.soundtrack(it)?.id }.distinct())
        assertTrue(recap.chapters.all { recap.soundtrack(it) != null })
        assertEquals("only", recap.soundtrack(ReverieChapter.OVERTURE)?.id)
    }

    @Test fun theSecondSongStartsAfterTheOpeningPair() {
        val recap = createReverieRecap(month, listOf(event("one", 1, 90_000), event("two", 1, 60_000)))
        assertEquals("one", recap.soundtrack(ReverieChapter.OVERTURE)?.id)
        assertEquals("one", recap.soundtrack(recap.chapters[1])?.id)
        assertEquals("two", recap.soundtrack(recap.chapters[2])?.id)
        assertEquals(2, recap.chapters.mapNotNull { recap.soundtrack(it)?.id }.distinct().size)
    }

    @Test fun artworkSelectionUsesTheDisplayedArtistAndAlbumRatherThanTheirSoundtrackCover() {
        val recap = createReverieRecap(month, (1..9).map { event("track-$it", 1, it * 60_000L) })
        assertEquals("artist", recap.backgroundArtworkId(ReverieChapter.ARTIST))
        assertEquals("album", recap.backgroundArtworkId(ReverieChapter.ALBUM))
        assertEquals("track-9", recap.backgroundArtworkId(ReverieChapter.SONG))
        assertEquals(recap.soundtrack(ReverieChapter.TIME)?.id, recap.backgroundArtworkId(ReverieChapter.TIME))
        assertEquals("track-9", recap.backgroundArtworkId(ReverieChapter.OVERTURE))
        assertEquals("album", recap.backgroundArtworkId(ReverieChapter.POSTER))
    }

    @Test fun posterWithoutAnAlbumTakesTheDisplayedSongCover() {
        val recap = createReverieRecap(month, listOf(event("hero", 1, 90_000, inAlbum = false)))
        assertEquals("hero", recap.backgroundArtworkId(ReverieChapter.POSTER))
    }

    @Test fun albumWithoutItsOwnArtworkUsesAnAvailableTrackCover() {
        val noCover = event("missing", 1, 10_000)
        val covered = event("covered", 2, 90_000).let { row ->
            row.copy(song = row.song.copy(song = row.song.song.copy(thumbnailUrl = "https://example.com/cover.jpg")))
        }
        val recap = createReverieRecap(month, listOf(noCover, covered))
        assertEquals("https://example.com/cover.jpg", recap.albums.single().artwork)
        assertEquals("covered", recap.albums.single().soundtrack.id)
        val dedicated = covered.copy(song = covered.song.copy(album = album.copy(thumbnailUrl = "https://example.com/album.jpg")))
        assertEquals("https://example.com/album.jpg", createReverieRecap(month, listOf(dedicated)).albums.single().artwork)
    }

    @Test fun sparseHistoryOmitsUnsupportedChaptersAndKeepsRealCategoriesOnly() {
        val recap = createReverieRecap(month, listOf(event("song", 1, 1, artists = emptyList(), inAlbum = false)))
        assertFalse(ReverieChapter.ARTIST in recap.chapters)
        assertFalse(ReverieChapter.ALBUM in recap.chapters)
        assertFalse(ReverieChapter.COLOURS in recap.chapters)
        assertEquals(ReverieChapter.POSTER, recap.chapters.last())
        val categories = listOf(CategoryStats("Recorded", null, 1, 1), CategoryStats("Empty", null, 0, 0))
        assertEquals(listOf("Recorded"), createReverieRecap(month, emptyList(), categories).categories.map { it.name })
    }

    @Test fun timelineChaptersHaveEqualLengthAndClampAtBothEnds() {
        assertEquals(0, reverieChapterAt(-1, 8))
        assertEquals(0, reverieChapterAt(9_999, 8))
        assertEquals(1, reverieChapterAt(10_000, 8))
        assertEquals(7, reverieChapterAt(Long.MAX_VALUE, 8))
        assertEquals(7, reverieChapterAt((Int.MAX_VALUE.toLong() + 1) * REVERIE_CHAPTER_MILLIS, 8))
        assertEquals(0, reverieChapterAt(0, 0))
    }
}
