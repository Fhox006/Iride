package com.metrolist.innertube

import com.metrolist.innertube.models.AlbumItem
import com.metrolist.innertube.models.Artist
import com.metrolist.innertube.models.ArtistItem
import com.metrolist.innertube.models.BrowseEndpoint
import com.metrolist.innertube.pages.ArtistPage
import com.metrolist.innertube.pages.ArtistSection
import com.metrolist.innertube.pages.latestNativeRelease
import com.metrolist.innertube.pages.withCatalogAlbums
import com.metrolist.innertube.pages.sortedByReleaseDate
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ArtistAlbumCatalogTest {
    private val artist = ArtistItem("UCartist", "Example Artist", null, shuffleEndpoint = null, radioEndpoint = null)
    private fun album(id: String, title: String, type: String = "Album", year: Int = 2024) = AlbumItem(
        browseId = id, playlistId = "", title = title, artists = listOf(Artist(artist.title, artist.id)),
        albumType = type, year = year, thumbnail = "",
    )
    private fun page(vararg sections: ArtistSection) = ArtistPage(artist, sections.toList(), null, null)

    @Test fun catalogOrdersJulyBeforeJuneRegardlessOfTitle() {
        val june = album("june", "A June album", year = 2026).copy(releaseDate = "2026-06-30")
        val july = album("july", "Z July album", year = 2026).copy(releaseDate = "2026-07-01")
        assertEquals(listOf(july, june), listOf(june, july).sortedByReleaseDate())
    }

    @Test fun catalogFallsBackToYearForInvalidDatesAndKeepsUnknownDatesLast() {
        val invalid = album("invalid", "Invalid", year = 2026).copy(releaseDate = "2026-13-01")
        val older = album("older", "Older", year = 2025).copy(releaseDate = "2025-12-31")
        val unknown = album("unknown", "Unknown").copy(year = null, releaseDate = "invalid")
        assertEquals(listOf(invalid, older, unknown), listOf(unknown, older, invalid).sortedByReleaseDate())
    }

    @Test fun latestNativeReleaseUsesPreciseDateAndIgnoresCatalogOnlyAndAppearsOnRows() {
        val older = album("MPREolder", "Older", year = 2026).copy(releaseDate = "2026-04-10")
        val latest = album("MPRElatest", "Latest", type = "Single", year = 2026).copy(releaseDate = "2026-09-03")
        val unconfirmed = album("itunes:it:9", "Catalog-only", year = 2027)
            .copy(releaseDate = "2027-01-01")
        val featured = album("MPREfeatured", "Featured", year = 2028)

        val selected = page(
            ArtistSection("Albums", listOf(older, unconfirmed), null),
            ArtistSection("Singles", listOf(latest), null),
            ArtistSection("Appears on albums", listOf(featured), null),
        ).latestNativeRelease()

        assertEquals("MPRElatest", selected?.second?.id)
        assertEquals("Singles", selected?.first?.title)
    }

    @Test fun latestNativeReleaseReturnsNullWhenOnlyUnconfirmedCatalogRowsExist() {
        val catalogOnly = album("itunes:it:9", "Catalog-only", year = 2027)
        assertNull(page(ArtistSection("Albums", listOf(catalogOnly), null)).latestNativeRelease())
    }

    @Test fun insertsMissingAlbumAndDeduplicatesNativeCardsWithoutCredits() {
        val native = album("MPREnative", "First", year = 2022).copy(artists = null)
        val missing = album("itunes:it:2", "Missing")
        val endpoint = BrowseEndpoint("moreAlbums")
        val original = page(ArtistSection("Album", listOf(native), endpoint))
        val enriched = original.withCatalogAlbums(listOf(album("itunes:it:1", "First", year = 2022), missing), "Albums", "Singles & EPs")
        val creditedNative = native.copy(artists = listOf(Artist(artist.title, artist.id)))
        assertEquals(listOf(missing, creditedNative), enriched.sections.single().items)
        assertEquals(endpoint, enriched.sections.single().moreEndpoint)
    }

    @Test fun createsAbsentShelvesAndSeparatesAlbumsFromSinglesAndEps() {
        val album = album("itunes:it:1", "Album")
        val single = album("itunes:it:2", "Single", "Single")
        val ep = album("itunes:it:3", "EP", "EP")
        val enriched = page().withCatalogAlbums(listOf(album, single, ep), "Albums", "Singles & EPs")
        assertEquals(listOf(album), enriched.sections[0].items)
        assertEquals(listOf(ep, single), enriched.sections[1].items)
    }

    @Test fun leavesAppearsOnShelfIntactAndIsIdempotent() {
        val appearsOn = ArtistSection("Appears on albums", listOf(album("MPREother", "Other album")), null)
        val external = listOf(album("itunes:it:1", "Missing"))
        val enriched = page(appearsOn).withCatalogAlbums(external, "Albums", "Singles & EPs")
        assertEquals(appearsOn, enriched.sections.first())
        assertEquals(external, enriched.sections.last().items)
        assertEquals(enriched, enriched.withCatalogAlbums(external, "Albums", "Singles & EPs"))
    }

    @Test fun requiresExactUnambiguousArtistIdentity() {
        fun row(id: Int, name: String) = Json.parseToJsonElement(
            """{"wrapperType":"artist","artistId":$id,"artistName":"$name"}""",
        ).jsonObject
        assertEquals("1", ExternalAlbumCatalog.matchingArtistId("Example Artist", listOf(row(1, "Example Artist"), row(2, "Example Artist Tribute"))))
        assertNull(ExternalAlbumCatalog.matchingArtistId("Example Artist", listOf(row(1, "Example Artist"), row(2, "Example Artist"))))
        assertNull(ExternalAlbumCatalog.matchingArtistId("Example Artist", listOf(row(1, "Example Artist Tribute"))))
        assertEquals("1", ExternalAlbumCatalog.matchingArtistId("thasup", listOf(row(1, "thasup"), row(2, "Tha Sup"))))
    }
}
