package com.metrolist.music.data.remote

import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GenreArtworkTest {
    private val image = "https://is1-ssl.mzstatic.com/image/thumb/Features211/v4/genre/photo.png/{w}x{h}bb.{f}"
    private val page = "https://music.apple.com/us/playlist/deep-house/pl.genre123"

    @Test
    fun acceptsGenreInsideLongTitleWithPunctuationAndAliases() {
        assertTrue(GenreArtworkParser.matchesTitle("Deep House", "Summer 2026: Deep-House & Ibiza"))
        assertTrue(GenreArtworkParser.matchesTitle("Rap", "Best Hip-Hop Beats"))
        assertTrue(GenreArtworkParser.matchesTitle("R&B", "R&B Essentials"))
        assertTrue(GenreArtworkParser.matchesTitle("Drum and Bass", "Liquid DnB"))
        assertFalse(GenreArtworkParser.matchesTitle("Rock", "Rockabilly Hits"))
        assertFalse(GenreArtworkParser.matchesTitle("Soul", "Soulja Boy Essentials"))
        assertFalse(GenreArtworkParser.matchesTitle("Deep House", "House Music"))
    }

    @Test
    fun excludesArtistCollectionsAndNamedArtistsEvenWithGenreInTitle() {
        assertFalse(GenreArtworkParser.matchesTitle("Jazz", "Jazz Inspired by Miles Davis"))
        assertFalse(GenreArtworkParser.matchesTitle("Deep House", "Disclosure's Deep House Mix"))
        assertTrue(GenreArtworkParser.referencesArtist("Miles Davis Jazz Essentials", listOf("Miles Davis"), "Jazz"))
        assertFalse(GenreArtworkParser.referencesArtist("Rap Essentials", listOf("Rap"), "Rap"))
        assertFalse(GenreArtworkParser.referencesArtist("Deep House Essentials", listOf("Deep", "House"), "Deep House"))
        assertFalse(GenreArtworkParser.referencesArtist("Hip Hop Beats", listOf("Hip Hop"), "Rap"))
    }

    @Test
    fun identityIgnoresCdnAndResolutionButRejectsArtistAndAlbumAssets() {
        val identity = GenreArtworkParser.imageIdentity(image)
        assertEquals(identity, GenreArtworkParser.imageIdentity(image.replace("is1-ssl", "is2-ssl")
            .replace("{w}x{h}bb.{f}", "2400x2400bb.jpg?l=it")))
        assertNull(GenreArtworkParser.imageIdentity(image.replace("Features211", "AMCArtistImages211")))
        assertNull(GenreArtworkParser.imageIdentity(image.replace("Features211", "Music211")))
        assertNull(GenreArtworkParser.imageIdentity(image.replace("bb.{f}", "SC.FPESS04.{f}")))
        assertNull(GenreArtworkParser.imageIdentity(image.replace("https:", "http:")))
        assertNull(GenreArtworkParser.imageIdentity(image.replace("mzstatic.com", "mzstatic.com.evil.example")))
        assertNull(GenreArtworkParser.imageIdentity(image.replace("is1-ssl", "user@is1-ssl")))
    }

    @Test
    fun readsOnlyPlaylistArtworkFromVerifiedSearchPageAndRequiresHd() {
        fun item(title: String, kind: String = "playlist", side: Int = 2400, url: String = image) = buildJsonObject {
            put("title", title)
            put("contentDescriptor", buildJsonObject { put("kind", kind); put("url", page) })
            put("artwork", buildJsonObject {
                put("dictionary", buildJsonObject { put("width", side); put("height", side); put("url", url) })
            })
        }
        val html = appleHtml("SearchResultsPageIntent", buildJsonArray {
            add(buildJsonObject {
                put("items", buildJsonArray {
                    add(item("Deep House Essentials"))
                    add(item("Deep House Essentials"))
                    add(item("Deep House Album", "album"))
                    add(item("Deep House", "artist"))
                    add(item("Deep House Small", side = 600))
                    add(item("House Essentials"))
                    add(item("Deep House Artist", url = image.replace("Features211", "AMCArtistImages211")))
                })
            })
        })
        val matches = GenreArtworkParser.appleSearch("Deep House", html)
        assertEquals(listOf("Deep House Essentials"), matches.map { it.title })
        assertTrue(GenreArtworkParser.appleSearch("Deep House", html.replace("SearchResultsPageIntent", "ArtistDetailPageIntent")).isEmpty())
    }

    @Test
    fun validatesDiversePlaylistAndRejectsOneArtistOrArtistNamedTitle() {
        val candidate = GenreArtworkCandidate("Deep House Essentials", image, page, 2400)
        fun playlist(title: String, artists: List<String>) = appleHtml("PlaylistDetailPageIntent", buildJsonArray {
            add(buildJsonObject {
                put("itemKind", "containerDetailHeaderLockup")
                put("items", buildJsonArray { add(buildJsonObject {
                    put("title", title)
                    put("contentDescriptor", buildJsonObject { put("url", page) })
                }) })
            })
            add(buildJsonObject {
                put("itemKind", "trackLockup")
                put("items", buildJsonArray { artists.forEach { add(buildJsonObject { put("artistName", it) }) } })
            })
        })
        assertTrue(GenreArtworkParser.applePlaylistMatches("Deep House", candidate,
            playlist(candidate.title, listOf("First", "Second", "Third"))))
        assertFalse(GenreArtworkParser.applePlaylistMatches("Deep House", candidate,
            playlist(candidate.title, List(10) { "First" } + listOf("Second", "Third"))))
        val dedicated = candidate.copy(title = "First Deep House Essentials")
        assertFalse(GenreArtworkParser.applePlaylistMatches("Deep House", dedicated,
            playlist(dedicated.title, listOf("First", "Second", "Third"))))
        assertFalse(GenreArtworkParser.applePlaylistMatches("Deep House", candidate,
            playlist("Another Deep House Playlist", listOf("First", "Second", "Third"))))
    }

    @Test
    fun deezerRequiresGenreTitlePlaylistAssetAndDiverseArtists() {
        val cover = "https://cdn-images.dzcdn.net/images/playlist/0123456789abcdef0123456789abcdef/1000x1000-000000-80-0-0.jpg"
        val body = """{"data":[{"id":123,"title":"Deep House Chill","picture_xl":"$cover"},
            {"id":456,"title":"House Chill","picture_xl":"$cover"},
            {"id":789,"title":"Deep House Chill","picture_xl":"${cover.replace("/playlist/", "/artist/")}"}]}"""
        val candidate = GenreArtworkParser.deezerSearch("Deep House", body).single()
        assertEquals("Deep House Chill", candidate.title)
        assertTrue(GenreArtworkParser.deezerPlaylistMatches("Deep House", candidate,
            """{"id":123,"title":"Deep House Chill","tracks":{"data":[{"artist":{"name":"One"}},
                {"artist":{"name":"Two"}},{"artist":{"name":"Three"}}]}}"""))
        assertEquals(cover.replace("1000x1000", "384x384"), candidate.imageAt(384))
        assertEquals(cover, candidate.imageAt(2400))
    }

    @Test
    fun fingerprintsDetectReuploadedPhotosButKeepDifferentPhotosSeparate() {
        fun pixels(descending: Boolean) = IntArray(72) { index ->
            val gray = if (descending) 240 - index % 9 * 20 else 40 + index % 9 * 20
            0xff000000.toInt() or (gray shl 16) or (gray shl 8) or gray
        }
        val first = genreArtworkFingerprint(pixels(true))
        val second = genreArtworkFingerprint(pixels(false))
        assertNotEquals(first, second)
        assertTrue(genreArtworkIsDuplicate("one", first, "one", second))
        assertTrue(genreArtworkIsDuplicate("one", first, "reuploaded", first xor 3L))
        assertFalse(genreArtworkIsDuplicate("one", first, "two", second))
    }

    private fun appleHtml(kind: String, sections: kotlinx.serialization.json.JsonArray): String {
        val server = buildJsonObject { put("data", buildJsonArray { add(buildJsonObject {
            put("intent", buildJsonObject { put("\$kind", kind) })
            put("data", buildJsonObject { put("sections", sections) })
        }) }) }
        return "<script id=\"serialized-server-data\" type=\"application/json\">$server</script>"
    }
}
