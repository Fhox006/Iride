package com.metrolist.music.data.remote

import com.metrolist.innertube.models.Artist
import com.metrolist.innertube.models.SongItem
import com.metrolist.music.repository.DailyChartRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

class DailyChartTest {
    private fun html(count: Int = 100, date: String = "2026/10/05") = """
        <span class="pagetitle">Spotify Daily Chart - Italy - $date | Totals</span>
        <table id="spotifydaily"><tbody>${(1..count).joinToString("") { rank -> """
            <tr><td>$rank</td><td>=</td><td class="text mp"><div>
            <a href="../artist/artist.html">Beyoncé &amp; Artist</a> -
            <a href="../track/${rank.toString().padStart(22, '0')}.html">Title $rank - Remix</a>
            (w/ <a href="../artist/featured.html">Guest</a>)</div></td>
            <td>5</td><td>1</td><td>(x2)</td><td>123,456</td></tr>
        """ }}</tbody></table>
    """

    @Test fun parsesSeparateArtistAndTrackLinksWithFeaturedArtists() {
        val chart = DailyChartParser.parse(html(200), ChartMarket.Italy, 123L)
        assertEquals("it", chart.marketCode)
        assertEquals("2026-10-05", chart.date)
        assertEquals(123L, chart.checkedAt)
        assertEquals(100, chart.tracks.size)
        assertEquals("Title 1 - Remix", chart.tracks.first().title)
        assertEquals(listOf("Beyoncé & Artist", "Guest"), chart.tracks.first().artists)
        assertEquals(123456L, chart.tracks.first().streams)
        assertEquals((1..100).toList(), chart.tracks.map { it.rank })
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsTruncatedCharts() { DailyChartParser.parse(html(99), ChartMarket.Global, 0) }

    @Test fun acceptsCompleteSmallCountryChartsWithoutInventingSongs() {
        val chart = DailyChartParser.parse(html(31) + "</body></html>", ChartMarket("lu"), 123L)
        assertEquals(31, chart.tracks.size)
        assertEquals(31, chart.sourceTrackCount)
        assertTrue(chart.isUsableFor(ChartMarket("lu")))
        assertFalse(chart.copy(tracks = chart.tracks.dropLast(1)).isUsableFor(ChartMarket("lu")))
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsTruncatedSmallCountryCharts() { DailyChartParser.parse(html(31), ChartMarket("lu"), 0) }

    @Test fun distinguishesArchivesFromDelayedDailyPublication() {
        val chart = DailyChartParser.parse(html(), ChartMarket.Italy, 123L)
        val today = LocalDate.of(2026, 10, 7)
        assertFalse(chart.isHistorical(today))
        assertFalse(chart.copy(date = "2026-09-30").isHistorical(today))
        assertTrue(chart.copy(date = "2022-04-07").isHistorical(today))
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsDuplicatePositions() {
        DailyChartParser.parse(html().replace("<td>100</td>", "<td>99</td>"), ChartMarket.Global, 0)
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsDuplicateTrackIds() {
        DailyChartParser.parse(html().replace("0000000000000000000100.html", "0000000000000000000001.html"), ChartMarket.Global, 0)
    }

    @Test(expected = IllegalStateException::class)
    fun rejectsChangedLayout() {
        DailyChartParser.parse(html().replace("../track/", "../song/"), ChartMarket.Global, 0)
    }

    @Test(expected = java.time.format.DateTimeParseException::class)
    fun rejectsInvalidDates() { DailyChartParser.parse(html(date = "2026/02/30"), ChartMarket.Global, 0) }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsMarketPathInjection() { ChartMarket("../global") }

    @Test fun marketsUseOneEndpointPattern() {
        assertEquals("https://kworb.net/spotify/country/br_daily.html", ChartMarket("br").url)
        assertEquals(listOf("global", "it", "us"), ChartMarket.home.map { it.code })
    }

    @Test fun rejectsCorruptOrWrongMarketCacheSnapshots() {
        val chart = DailyChartParser.parse(html(), ChartMarket.Italy, 123L)
        assertTrue(chart.isUsableFor(ChartMarket.Italy))
        assertFalse(chart.isUsableFor(ChartMarket.Global))
        assertFalse(chart.copy(date = "broken").isUsableFor(ChartMarket.Italy))
        assertFalse(chart.copy(tracks = chart.tracks.drop(1)).isUsableFor(ChartMarket.Italy))
        assertFalse(chart.copy(tracks = chart.tracks.map { it.copy(spotifyId = "../bad") }).isUsableFor(ChartMarket.Italy))
    }

    @Test fun cacheKeepsOnlyNonemptyCurrentChartsForTheRequestedMarket() {
        val today = LocalDate.of(2026, 10, 7)
        val chart = DailyChartParser.parse(html(), ChartMarket.Italy, 123L)
        assertTrue(chart.isCurrentFor(ChartMarket.Italy, today))
        assertFalse(chart.isCurrentFor(ChartMarket.Global, today))
        assertFalse(chart.copy(tracks = emptyList()).isCurrentFor(ChartMarket.Italy, today))
        assertFalse(chart.copy(date = "2026-09-29").isCurrentFor(ChartMarket.Italy, today))
        assertFalse(chart.copy(date = "2026-10-09").isCurrentFor(ChartMarket.Italy, today))
        assertFalse(chart.copy(date = "invalid").isCurrentFor(ChartMarket.Italy, today))
    }

    @Test fun refreshesAtUtcDayBoundaryAndRetriesDuringPublicationDay() {
        val checkedAt = Instant.parse("2026-10-06T23:59:00Z").toEpochMilli()
        assertFalse(DailyChartRepository.needsRefresh(checkedAt, checkedAt + 30_000))
        assertTrue(DailyChartRepository.needsRefresh(checkedAt, checkedAt + 60_000))
        val morning = Instant.parse("2026-10-07T08:00:00Z").toEpochMilli()
        assertFalse(DailyChartRepository.needsRefresh(morning, morning + 3 * 60 * 60 * 1000))
        assertTrue(DailyChartRepository.needsRefresh(morning, morning + 4 * 60 * 60 * 1000))
        assertTrue(DailyChartRepository.needsRefresh(morning, morning - 1))
    }

    private val track = ChartTrack(1, "0000000000000000000001", "Halo", listOf("Beyoncé"), 100)
    private fun song(title: String = "Halo", artist: String = "Beyoncé", type: String? = "MUSIC_VIDEO_TYPE_ATV") =
        SongItem("youtube-id", title, listOf(Artist(artist, null)), thumbnail = "", musicVideoType = type)

    @Test fun matchesAudioWithAccentNormalization() {
        assertEquals("youtube-id", ChartTrackMatcher.bestMatch(track, listOf(song(artist = "Beyonce")))?.id)
    }

    @Test fun matchesLocalizedCreditsAndUnbracketedFeatures() {
        assertEquals("youtube-id", ChartTrackMatcher.bestMatch(track, listOf(song(title = "Halo (con Guest)")))?.id)
        assertEquals("youtube-id", ChartTrackMatcher.bestMatch(track.copy(title = "Halo feat. Guest"), listOf(song()))?.id)
        val collaboration = song().copy(artists = listOf(Artist("Beyoncé", null), Artist("Guest", null)))
        assertEquals("youtube-id", ChartTrackMatcher.bestMatch(track.copy(title = "Halo CON GUEST"), listOf(collaboration))?.id)
    }

    @Test fun acceptsRemasterLabelsAndArtistArticlesWithoutAcceptingDifferentRecordings() {
        assertEquals("youtube-id", ChartTrackMatcher.bestMatch(track.copy(title = "Halo - Remastered 2026"), listOf(song()))?.id)
        val dolls = track.copy(title = "Iris", artists = listOf("The Goo Goo Dolls"))
        assertEquals("youtube-id", ChartTrackMatcher.bestMatch(dolls, listOf(song(title = "Iris", artist = "Goo Goo Dolls")))?.id)
        assertNull(ChartTrackMatcher.bestMatch(track, listOf(song(title = "Halo (Radio Edit)"))))
        assertNull(ChartTrackMatcher.bestMatch(track, listOf(song(title = "Halo (Guest Version)"))))
        assertNull(ChartTrackMatcher.bestMatch(track, listOf(song(title = "Halo (Unplugged)"))))
    }

    @Test fun matchesEquivalentSubtitleAndRemixLabels() {
        assertEquals("youtube-id", ChartTrackMatcher.bestMatch(track.copy(title = "Halo - Inedito"), listOf(song(title = "Halo (Inedito) (con Guest)")))?.id)
        assertEquals("youtube-id", ChartTrackMatcher.bestMatch(track.copy(title = "Halo RMX - Remix"), listOf(song(title = "Halo (Remix)")))?.id)
        assertNull(ChartTrackMatcher.bestMatch(track, listOf(song(title = "Halo (Remix)"))))
    }

    @Test fun matchesSplitJointArtistCredits() {
        val duet = track.copy(artists = listOf("Beyoncé & Guest"))
        val split = song().copy(artists = listOf(Artist("Beyoncé", null), Artist("Guest", null)))
        assertEquals("youtube-id", ChartTrackMatcher.bestMatch(duet, listOf(split))?.id)
        assertNull(ChartTrackMatcher.bestMatch(duet, listOf(song())))
    }

    @Test fun rejectsVideosUnknownTypesCoversAndAlternateVersions() {
        assertNull(ChartTrackMatcher.bestMatch(track, listOf(song(type = "MUSIC_VIDEO_TYPE_OMV"))))
        assertNull(ChartTrackMatcher.bestMatch(track, listOf(song(type = null))))
        assertNull(ChartTrackMatcher.bestMatch(track, listOf(song(artist = "Cover Band"))))
        assertNull(ChartTrackMatcher.bestMatch(track, listOf(song(artist = "Beyoncé Tribute Band"))))
        assertNull(ChartTrackMatcher.bestMatch(track, listOf(song(title = "Halo - Live"))))
        assertNull(ChartTrackMatcher.bestMatch(track, listOf(song(title = "Halo - Remix"))))
        assertNull(ChartTrackMatcher.bestMatch(track, listOf(song().copy(isEpisode = true))))
    }
}
