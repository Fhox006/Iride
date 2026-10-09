package com.metrolist.music.discovery

import com.metrolist.innertube.models.AlbumItem
import com.metrolist.innertube.models.Artist
import com.metrolist.innertube.models.SongItem
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class NewReleasesTest {
    private val today = LocalDate.parse("2026-10-07")

    @Test fun rejectsUnknownFutureAndOldDatesIncludingInvalidCalendarDays() {
        assertEquals("2026-09-23", NewReleaseCatalog.date("2026-09-23T07:00:00Z", today))
        assertEquals("2026-10-07", NewReleaseCatalog.date("2026-10-07", today))
        listOf(null, "", "2026", "2026-10-08", "2026-09-22", "2026-02-30", "2026-10-07garbage")
            .forEach { assertNull(NewReleaseCatalog.date(it, today)) }
    }

    @Test fun distinguishesSinglesAndEpsFromAlbums() {
        assertEquals(NewReleaseKind.SINGLE, NewReleaseCatalog.kind("New Work - Single", 4))
        assertEquals(NewReleaseKind.EP, NewReleaseCatalog.kind("New Work - EP", 2))
        assertEquals(NewReleaseKind.SINGLE, NewReleaseCatalog.kind("New Work", 1))
        assertEquals(NewReleaseKind.EP, NewReleaseCatalog.kind("New Work", 5))
        assertEquals(NewReleaseKind.ALBUM, NewReleaseCatalog.kind("New Work", 12))
    }

    @Test fun parsesFeedGenreIdAndApplesExplicitRatingTypo() {
        val item = Json.parseToJsonElement("""{
            "id":"123", "kind":"albums", "name":"New Work - EP", "artistName":"Artist",
            "artistId":"42", "releaseDate":"2026-10-02", "contentAdvisoryRating":"Explict",
            "artworkUrl100":"https://is1-ssl.mzstatic.com/image/thumb/art/100x100bb.jpg",
            "genres":[{"genreId":"34","name":"Musica"},{"genreId":"18","name":"Hip-Hop/Rap"}]
        }""") as JsonObject
        val release = NewReleaseCatalog.parse(item, "it", today, feed = true)!!
        assertEquals("New Work", release.title)
        assertEquals(NewReleaseKind.EP, release.kind)
        assertEquals("Hip-Hop/Rap", release.genre)
        assertTrue(release.explicit)
        assertEquals("https://is1-ssl.mzstatic.com/image/thumb/art/1200x1200bb.jpg", release.artworkUrl)
    }

    @Test fun excludesOlderRecordingsRepackagedInRecentCollectionsWithoutExcludingNewRemixes() {
        fun track(id: Int, date: String, title: String = "Original") = Json.parseToJsonElement("""{
            "wrapperType":"track", "kind":"song", "trackId":$id, "trackName":"$title",
            "artistName":"Artist", "releaseDate":"${date}T07:00:00Z",
            "artworkUrl100":"https://is1-ssl.mzstatic.com/image/art/100x100bb.jpg"
        }""") as JsonObject
        val catalog = listOf(track(1, "2021-01-01"), track(2, "2026-10-02"),
            track(3, "2026-10-02", "Original - New Remix"))
        val releases = NewReleaseCatalog.parseCatalog(catalog, "it", today)
        assertEquals(listOf("Original - New Remix"), releases.map(NewRelease::title))
        val originals = NewReleaseCatalog.originalDates(catalog)
        assertEquals("2021-01-01", originals[NewReleaseCatalog.recordingKey(track(2, "2026-10-02"))])
    }

    private fun release(index: Int, track: Boolean = false, artist: String = "Artist $index",
        kind: NewReleaseKind = if (track) NewReleaseKind.SINGLE else NewReleaseKind.ALBUM,
        followed: Boolean = false, genre: String = "Pop", date: String = "2026-10-02",
        collectionId: String = index.toString(), explicit: Boolean = false) = NewRelease(
        id = "apple:${if (track) "song" else "album"}:$index", title = "Work $index", artistName = artist,
        artworkUrl = "https://example.com/$index.jpg", releaseDate = date, kind = kind, genre = genre,
        country = "it", followed = followed, collectionId = collectionId, isTrack = track, explicit = explicit, playable = true,
    )

    private fun page(items: List<NewRelease>, trending: Set<String> = emptySet(), hide: Boolean = false,
        visit: Long = 0, profile: NewReleaseProfile = NewReleaseProfile()) = NewReleaseSelection.page(
        items, trending, profile, "it", hide, today, visit, 1_791_370_000_000L, 1_791_370_000_000L,
    )

    @Test fun followedAlbumsIncludeEpsAndSinglesComeBeforeAlbumTracks() {
        val album = release(1, followed = true)
        val ep = release(2, kind = NewReleaseKind.EP, followed = true)
        val single = release(3, kind = NewReleaseKind.SINGLE, followed = true)
        val albumTrack = release(4, track = true, followed = true, collectionId = "1")
        val epTrack = release(5, track = true, followed = true, collectionId = "2")
        val singleTrack = release(6, track = true, followed = true, collectionId = "3")
        val page = page(listOf(album, ep, single, albumTrack, epTrack, singleTrack))
        assertEquals(setOf(album.id, ep.id), page.followedAlbums.map(NewRelease::id).toSet())
        assertEquals(singleTrack.id, page.followedSingles.first().id)
        assertEquals(listOf(singleTrack.id, epTrack.id, albumTrack.id), page.followedSingles.map(NewRelease::id))
        assertEquals(setOf(singleTrack.id, albumTrack.id, epTrack.id), page.followedSingles.map(NewRelease::id).toSet())
    }

    @Test fun followedTracksStayChronologicalWithinSinglesEpsAndAlbumsAcrossVisits() {
        val single = release(50, kind = NewReleaseKind.SINGLE, followed = true)
        val ep = release(51, kind = NewReleaseKind.EP, followed = true)
        val album = release(52, followed = true)
        val olderSingle = release(60, track = true, followed = true, collectionId = "50", date = "2026-10-01")
        val newerSingle = release(61, track = true, followed = true, collectionId = "50", date = "2026-10-06")
        val epTrack = release(62, track = true, followed = true, collectionId = "51", date = "2026-09-30")
        val albumTrack = release(63, track = true, followed = true, collectionId = "52", date = "2026-09-29")
        val items = listOf(albumTrack, newerSingle, epTrack, olderSingle, single, ep, album)
        val expected = listOf(olderSingle.id, newerSingle.id, epTrack.id, albumTrack.id)
        assertEquals(expected, page(items, visit = 1).followedSingles.map(NewRelease::id))
        assertEquals(expected, page(items.reversed(), visit = 9).followedSingles.map(NewRelease::id))
    }

    @Test fun trackParserUsesCollectionTypeInsteadOfTreatingEverySongAsASingle() {
        fun track(collection: String, count: Int) = Json.parseToJsonElement("""{
            "wrapperType":"track", "kind":"song", "trackId":123, "trackName":"Song",
            "artistName":"Artist", "releaseDate":"2026-10-02", "collectionName":"$collection",
            "trackCount":$count, "artworkUrl100":"https://is1-ssl.mzstatic.com/image/art/100x100bb.jpg"
        }""") as JsonObject
        assertEquals(NewReleaseKind.SINGLE, NewReleaseCatalog.parse(track("Collection", 2), "it", today)!!.kind)
        assertEquals(NewReleaseKind.EP, NewReleaseCatalog.parse(track("Collection", 5), "it", today)!!.kind)
        assertEquals(NewReleaseKind.ALBUM, NewReleaseCatalog.parse(track("Collection", 12), "it", today)!!.kind)
    }

    @Test fun singleCollectionsWithoutASuffixKeepPriorityEvenWithoutAnAlbumEntry() {
        val single = release(70, track = true, followed = true, kind = NewReleaseKind.SINGLE)
            .copy(collectionTitle = "Plain title", trackCount = 2)
        val ep = release(71, track = true, followed = true, kind = NewReleaseKind.EP)
            .copy(collectionTitle = "Another plain title")
        val album = release(72, track = true, followed = true, kind = NewReleaseKind.ALBUM)
            .copy(collectionTitle = "Album title")
        assertEquals(listOf(single.id, ep.id, album.id), page(listOf(album, ep, single)).followedSingles.map(NewRelease::id))
    }

    @Test fun filtersExpiredAndExplicitItemsFromEveryShelfEvenForFollowedArtists() {
        val items = listOf(release(1, followed = true), release(2, followed = true, date = "2026-09-22"),
            release(3, followed = true, date = "2026-10-08"), release(4, followed = true, explicit = true),
            release(5, track = true, explicit = true))
        val page = page(items, hide = true)
        val all = page.hero + page.albums + page.songs + page.followedAlbums + page.followedSingles +
            page.sections.flatMap(NewReleaseSection::releases)
        assertTrue(all.isNotEmpty())
        assertTrue(all.all { it.id == items.first().id })
    }

    @Test fun balancesPersonalizedAndTrendingFreshSongsWhenBothPoolsSuffice() {
        val rap = (1..12).map { release(it, track = true, genre = "Hip-Hop/Rap") }
        val pop = (13..24).map { release(it, track = true) }
        val page = page(rap + pop, trending = pop.map(NewRelease::id).toSet(),
            profile = NewReleaseProfile(genreWeights = mapOf("rap" to 10.0)))
        assertEquals(24, page.songs.size)
        assertEquals(12, page.songs.count { it.genre == "Hip-Hop/Rap" })
        assertEquals(12, page.songs.count { it.genre == "Pop" })
        assertEquals(24, page.songs.distinctBy(NewRelease::id).size)
    }

    @Test fun reopeningRotatesDiscoveryOrderWhileFollowedShelfStaysChronological() {
        val items = (1..30).map { release(it) } + listOf(release(100, followed = true, date = "2026-10-03"),
            release(101, followed = true, date = "2026-10-01"))
        val first = page(items, visit = 1)
        val second = page(items, visit = 2)
        assertNotEquals(first.albums.map(NewRelease::id), second.albums.map(NewRelease::id))
        assertEquals(first.followedAlbums, second.followedAlbums)
        assertEquals(setOf("apple:album:100", "apple:album:101"),
            (first.hero + first.followedAlbums).filter { it.followed }.map(NewRelease::id).toSet())
        assertEquals(first.hero, second.hero)
    }

    @Test fun validatesAlbumIdentityAndYearInsteadOfOpeningUnrelatedFirstSearchResult() {
        val release = release(1, artist = "Artist")
        val correct = AlbumItem("correct", "playlist", title = "Work 1", artists = listOf(Artist("Artist", "UC1")),
            year = 2026, thumbnail = "")
        assertEquals(correct, NewReleaseCatalog.albumMatch(release, listOf(correct.copy(title = "Other"), correct)))
        assertNull(NewReleaseCatalog.albumMatch(release, listOf(correct.copy(year = 2020))))
        assertNull(NewReleaseCatalog.albumMatch(release, listOf(correct.copy(artists = listOf(Artist("Unrelated", null))))))
        val song = SongItem("song", "Work 1", listOf(Artist("Artist", null)), thumbnail = "",
            musicVideoType = "MUSIC_VIDEO_TYPE_ATV")
        assertEquals(song, NewReleaseCatalog.songMatch(release.copy(isTrack = true), listOf(song)))
        assertNull(NewReleaseCatalog.songMatch(release.copy(isTrack = true), listOf(song.copy(title = "Work 1 - Live"))))
        assertFalse(NewReleaseCatalog.isFresh(release.copy(releaseDate = "2025-10-02"), today))
    }

    @Test fun spotlightIncludesDiscoveryAlongsideFollowedAndListenedArtists() {
        val unfamiliar = release(1, genre = "Hip-Hop/Rap")
        val starred = release(2, followed = true)
        val listened = release(3, artist = "Listened")
        val page = page(listOf(unfamiliar, starred, listened), profile = NewReleaseProfile(
            artistWeights = mapOf("listened" to 1.0), genreWeights = mapOf("rap" to 100.0)))
        assertEquals(setOf(unfamiliar.id, starred.id, listened.id), page.hero.map(NewRelease::id).toSet())
        assertEquals(listOf(starred.id), page.followedAlbums.map(NewRelease::id))
        assertTrue(page.albums.none { it.followed })
    }

    @Test fun spotlightFillsWithoutFollowsOrListeningHistory() {
        val page = page((1..20).map { release(it) })
        assertEquals(8, page.hero.size)
        assertEquals(8, page.hero.map(NewRelease::artistName).distinct().size)
    }

    @Test fun spotlightReservesDiscoverySpaceWhenManyArtistsAreFollowed() {
        val items = (1..30).map { release(it, followed = it <= 20) }
        val page = page(items)
        assertEquals(4, page.hero.count { it.followed })
        assertEquals(4, page.hero.count { !it.followed })
        assertEquals(items.filter { it.followed }.map(NewRelease::id).toSet(),
            page.followedAlbums.map(NewRelease::id).toSet())
    }

    private fun albumCards(page: NewReleasePage): List<NewRelease> =
        (page.hero + page.followedAlbums + page.albums + page.sections.flatMap { it.releases }).filterNot { it.isTrack }

    @Test fun publishesCatalogAlbumsImmediatelyAndPrefersAnAlreadyResolvedEdition() {
        val standard = release(1, artist = "Artist").copy(title = "Work", trackCount = 12)
        val deluxe = release(2, artist = "Artist").copy(title = "Work (Deluxe Edition)", trackCount = 18)
        val unavailable = release(3, artist = "Artist").copy(title = "Work (Expanded Edition)",
            trackCount = 24, playable = false)
        val page = page(listOf(standard, deluxe, unavailable))
        assertEquals(listOf(deluxe.id), albumCards(page).map(NewRelease::id))
        assertEquals("Work", NewReleaseCatalog.albumFamilyTitle(deluxe.title))
        assertEquals(unavailable.id, albumCards(page(listOf(unavailable))).single().id)
        assertEquals(standard.id, albumCards(page(listOf(standard, deluxe.copy(playable = false)))).single().id)
    }

    @Test fun preservesDistinctRecordingVersionsWhileCollapsingDeluxeAndCleanPackaging() {
        assertEquals("Work", NewReleaseCatalog.albumFamilyTitle("Work (Deluxe Edition)"))
        assertEquals("Work", NewReleaseCatalog.albumFamilyTitle("Work (Clean Edition)"))
        listOf("Live", "Acoustic", "Remix", "Remastered", "Instrumental").forEach { version ->
            val title = "Work ($version Edition)"
            assertEquals(title, NewReleaseCatalog.albumFamilyTitle(title))
            assertNotEquals(NewReleaseCatalog.familyKey(release(1).copy(title = "Work")),
                NewReleaseCatalog.familyKey(release(2).copy(title = title)))
        }
    }

    @Test fun shelvesDoNotRepeatRecordingFamiliesAndSpotlightDoesNotRemoveFollowedAlbums() {
        val albums = (1..80).map { release(it, followed = it <= 16, genre = if (it % 2 == 0) "Pop" else "Hip-Hop/Rap") }
        val tracks = (81..160).map { release(it, track = true, followed = it < 90,
            genre = if (it % 2 == 0) "Pop" else "Hip-Hop/Rap") }
        val page = page(albums + tracks, trending = tracks.map(NewRelease::id).toSet())
        val all = page.followedAlbums + page.followedSingles + page.albums + page.songs +
            page.sections.flatMap(NewReleaseSection::releases)
        assertEquals(all.size, all.map(NewReleaseCatalog::familyKey).distinct().size)
        assertEquals(albums.filter { it.followed }.map(NewRelease::id).toSet(),
            page.followedAlbums.map(NewRelease::id).toSet())
        assertTrue(page.sections.isNotEmpty())
        val discoveries = page.hero.filterNot { it.followed }.map(NewReleaseCatalog::familyKey).toSet()
        assertTrue(all.none { NewReleaseCatalog.familyKey(it) in discoveries })
    }

    @Test fun firstPageContainsFreshAlbumsBeforeYouTubeResolution() {
        val items = (1..40).map { release(it).copy(playable = false) }
        val page = page(items)
        assertTrue(page.hasContent)
        assertEquals(8, page.hero.size)
        assertEquals(16, page.albums.size)
        assertTrue(albumCards(page).all { !it.playable })
        assertEquals(albumCards(page).size, albumCards(page).map(NewReleaseCatalog::familyKey).distinct().size)
    }

    @Test fun recommendationOnlyPageIsUsableWhileCatalogRefreshContinues() {
        val album = AlbumItem("browse", "playlist", title = "New Album", artists = listOf(Artist("Artist", "UC1")),
            year = 2026, thumbnail = "cover")
        assertTrue(NewReleasePage(newAlbums = listOf(album)).hasContent)
        assertTrue(NewReleasePage(sections = listOf(NewReleaseSection("world", "", listOf(release(1))))).hasContent)
        assertFalse(NewReleasePage().hasContent)
    }

    @Test fun youtubeShelfRemovesCatalogAndEditionDuplicatesWithoutHidingDistinctRecordings() {
        fun album(id: String, title: String, explicit: Boolean = false) = AlbumItem(id, "playlist-$id", title = title,
            artists = listOf(Artist("Artist", "UC1")), year = 2026, thumbnail = "cover", explicit = explicit)
        val shown = release(1, artist = "Artist").copy(title = "Work (Deluxe Edition)")
        val candidates = listOf(album("standard", "Work"), album("deluxe", "Work (Deluxe Edition)"),
            album("live", "Work (Live Edition)"), album("other", "Other"), album("other-deluxe", "Other (Deluxe Edition)"),
            album("explicit", "Explicit", explicit = true))
        assertEquals(listOf("live", "other"), NewReleaseSelection.discoveryAlbums(candidates, listOf(shown), true).map { it.id })
        assertEquals(listOf("live", "other", "explicit"), NewReleaseSelection.discoveryAlbums(candidates, listOf(shown), false).map { it.id })
    }

    @Test fun localDiscoveryUsesChartArtistProvenanceInsteadOfStorefrontCountry() {
        val local = (1..20).map { release(it, artist = "Local Artist $it").copy(country = "us") }
        val other = (21..40).map { release(it, artist = "Other Artist $it").copy(country = "it") }
        val localArtists = local.map { NewReleaseCatalog.normalize(it.artistName) }.toSet()
        val page = page(local + other, profile = NewReleaseProfile(localChartArtists = localArtists))
        val shelf = page.sections.first { it.id == "local" }
        assertTrue(shelf.releases.all { NewReleaseCatalog.normalize(it.artistName) in localArtists })
        assertTrue(shelf.releases.all { it.country == "us" })
        assertTrue(shelf.releases.none { it in other })
    }

    @Test fun bulkLookupSeparatesRequestedArtistsWithoutDroppingCollaborationCollections() {
        fun record(text: String) = Json.parseToJsonElement(text) as JsonObject
        val first = record("""{"wrapperType":"artist","artistId":1}""")
        val collaboration = record("""{"wrapperType":"collection","artistId":9,"collectionId":11}""")
        val second = record("""{"wrapperType":"artist","artistId":2}""")
        val album = record("""{"wrapperType":"collection","artistId":2,"collectionId":22}""")
        val groups = NewReleaseCatalog.lookupGroups(listOf(first, collaboration, second, album), setOf("1", "2", "3"))
        assertEquals(listOf(first, collaboration), groups["1"])
        assertEquals(listOf(second, album), groups["2"])
        assertNull(groups["3"])
    }
}
