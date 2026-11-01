package com.metrolist.innertube

import com.metrolist.innertube.models.AlbumItem
import com.metrolist.innertube.models.Artist
import com.metrolist.innertube.models.SongItem
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExternalAlbumCatalogTest {
    private fun album(id: String, title: String = "CASA GOSPEL", artist: String = "thasup", year: Int = 2024) = AlbumItem(
        browseId = id, playlistId = "", title = title, artists = listOf(Artist(artist, null)),
        year = year, thumbnail = "https://example.com/cover.jpg",
    )

    private fun song(title: String = "POSTO MIO", artist: String = "thasup", duration: Int? = 180) = SongItem(
        id = "video-id", title = title, artists = listOf(Artist(artist, null)), duration = duration, thumbnail = "",
    )

    @Test fun parsesCatalogMetadataAndShareLink() {
        val row = Json.parseToJsonElement("""{
            "wrapperType":"collection", "collectionType":"Album", "collectionId":1780348761,
            "collectionName":"CASA GOSPEL", "artistName":"thasup & Mara Sattei",
            "releaseDate":"2024-12-13T08:00:00Z", "collectionExplicitness":"explicit",
            "artworkUrl100":"https://example.com/100x100bb.jpg"
        }""").jsonObject
        val result = requireNotNull(ExternalAlbumCatalog.album(row, "it"))
        assertEquals("itunes:it:1780348761", result.id)
        assertEquals(2024, result.year)
        assertTrue(result.explicit)
        assertEquals("https://example.com/600x600bb.jpg", result.thumbnail)
        assertEquals("https://music.apple.com/it/album/1780348761", result.shareLink)
        assertTrue(result.playlistId.isEmpty())
    }

    @Test fun keepsNativeAlbumForMatchingCollaborativeRelease() {
        val native = album("MPRE123")
        val external = album("itunes:it:1780348761", title = "Casa Gospel", artist = "thasup & Mara Sattei")
        assertEquals(listOf(native), ExternalAlbumCatalog.merge(listOf(native), listOf(external)))
    }

    @Test fun preservesDifferentArtistYearAndEdition() {
        val native = album("MPRE123")
        val external = listOf(
            album("itunes:it:1", artist = "Other Artist"),
            album("itunes:it:2", year = 2025),
            album("itunes:it:3", title = "CASA GOSPEL (Deluxe)"),
        )
        assertEquals(listOf(native) + external, ExternalAlbumCatalog.merge(listOf(native), external))
    }

    @Test fun acceptsOfficialVideoWithCorrectArtistAndDuration() {
        val candidate = song(title = "thasup - POSTO MIO (Official Video)", artist = "thasup - Topic", duration = 183)
        assertEquals(candidate, ExternalAlbumCatalog.selectTrack("Posto mio", "thasup & Mara Sattei", 180, listOf(candidate)))
    }

    @Test fun acceptsOfficialVisualVideoWithoutDiscardingVersionQualifiers() {
        val candidate = song(title = "OCCHI MIEI (Official Visual Video)", duration = 155)
        assertEquals(candidate, ExternalAlbumCatalog.selectTrack("OCCHI MIEI", "thasup & Mara Sattei", 154, listOf(candidate)))
        assertNull(ExternalAlbumCatalog.selectTrack("OCCHI MIEI", "thasup", 154,
            listOf(candidate.copy(title = "OCCHI MIEI (Live) (Official Visual Video)"))))
    }

    @Test fun rejectsWrongArtistEditionDurationAndMissingDuration() {
        val candidates = listOf(
            song(artist = "Other Artist"), song(title = "POSTO MIO (Live)"),
            song(title = "POSTO MIO (Remix)"), song(duration = 230), song(duration = null),
        )
        assertNull(ExternalAlbumCatalog.selectTrack("POSTO MIO", "thasup", 180, candidates))
    }

    @Test fun choosesClosestDurationAndRejectsArbitraryTitlePrefixes() {
        val closest = song(duration = 181).copy(id = "closest")
        val candidates = listOf(song(duration = 189), closest, song(title = "Cover - POSTO MIO", duration = 180))
        assertEquals(closest, ExternalAlbumCatalog.selectTrack("POSTO MIO", "thasup", 180, candidates))
    }

    @Test fun ignoresNonAlbumRows() {
        assertNull(ExternalAlbumCatalog.album(Json.parseToJsonElement("""{"wrapperType":"track","kind":"song"}""").jsonObject, "it"))
    }

    @Test fun deduplicatesSingleSuffixWithoutRemovingEdition() {
        val native = album("MPRE123", title = "POSTO MIO")
        val external = album("itunes:it:1", title = "POSTO MIO - Single")
        assertEquals(listOf(native), ExternalAlbumCatalog.merge(listOf(native), listOf(external)))
    }
}
