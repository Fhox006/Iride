package com.metrolist.music.data.remote

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ArtistArtworkParserTest {
    private val spotifyUrl = "https://open.spotify.com/artist/6eUKZXaKkcviH0Ku9w2n3V"
    private val appleUrl = "https://music.apple.com/us/artist/ed-sheeran/183313439"
    private val portrait = "https://is1-ssl.mzstatic.com/image/thumb/AMCArtistImages211/portrait.png/486x486bb.png"

    @Test
    fun extractsSpotifyPortraitAndDecodesHtmlEntities() {
        val artwork = ArtistArtworkParser.parse(ArtistArtworkSource.SPOTIFY, "Simon & Garfunkel", spotifyUrl, """
            <meta content="Simon &amp; Garfunkel" property="og:title">
            <meta content="https://i.scdn.co/image/portrait" property="og:image">
        """)
        assertEquals("https://i.scdn.co/image/portrait", artwork?.imageUrl)
        assertEquals(ArtistArtworkSource.SPOTIFY, artwork?.source)
    }

    @Test
    fun prefersAppleSquarePortraitOverSocialBannerAndAlbumArtwork() {
        val artwork = ArtistArtworkParser.parse(ArtistArtworkSource.APPLE_MUSIC, "Ed Sheeran", appleUrl, """
            <meta property="og:image" content="https://is1-ssl.mzstatic.com/image/thumb/AMCArtistImages211/portrait.png/1200x630cw.png">
            <script type="application/ld+json">{
                "@graph":[
                    {"@type":"MusicAlbum","name":"Ed Sheeran","image":"https://is1-ssl.mzstatic.com/image/thumb/Music/album.jpg"},
                    {"@type":"MusicGroup","name":"Ed Sheeran","image":"$portrait"}
                ]
            }</script>
        """)
        assertEquals(portrait.replace("486x486", "1200x1200"), artwork?.imageUrl)
        assertEquals("https://music.apple.com/us/artist/183313439", artwork?.artistUrl)
    }

    @Test
    fun rejectsWrongArtistAndNonArtistPages() {
        assertNull(ArtistArtworkParser.parse(ArtistArtworkSource.SPOTIFY, "Ed Sheeran", spotifyUrl, """
            <meta property="og:title" content="Someone Else">
            <meta property="og:image" content="https://i.scdn.co/image/portrait">
        """))
        assertNull(ArtistArtworkParser.profileUrl(ArtistArtworkSource.SPOTIFY, spotifyUrl.replace("/artist/", "/album/")))
        assertNull(ArtistArtworkParser.profileUrl(ArtistArtworkSource.APPLE_MUSIC, appleUrl.replace("/artist/", "/album/")))
    }

    @Test
    fun rejectsRedirectToAnotherArtist() {
        assertNull(ArtistArtworkParser.parse(ArtistArtworkSource.SPOTIFY, "Ed Sheeran", spotifyUrl, """
            <meta property="og:url" content="https://open.spotify.com/artist/0123456789012345678901">
            <meta property="og:title" content="Ed Sheeran">
            <meta property="og:image" content="https://i.scdn.co/image/portrait">
        """))
    }

    @Test
    fun rejectsMissingImagesAlbumCoversAndUntrustedHosts() {
        assertNull(ArtistArtworkParser.parse(ArtistArtworkSource.SPOTIFY, "Ed Sheeran", spotifyUrl,
            """<meta property="og:title" content="Ed Sheeran">"""))
        assertNull(ArtistArtworkParser.parse(ArtistArtworkSource.SPOTIFY, "Ed Sheeran", spotifyUrl, """
            <meta property="og:title" content="Ed Sheeran">
            <meta property="og:image" content="https://i.scdn.co.evil.example/image/portrait">
        """))
        assertNull(ArtistArtworkParser.parse(ArtistArtworkSource.APPLE_MUSIC, "Ed Sheeran", appleUrl, """
            <script type="application/ld+json">{
                "@type":"MusicGroup","name":"Ed Sheeran",
                "image":"https://is1-ssl.mzstatic.com/image/thumb/Music/album.jpg"
            }</script>
        """))
    }

    @Test
    fun normalizesStorefrontLinksAndRejectsUnsafeUrls() {
        assertEquals("https://music.apple.com/us/artist/183313439",
            ArtistArtworkParser.profileUrl(ArtistArtworkSource.APPLE_MUSIC, "https://itunes.apple.com/gb/artist/id183313439?uo=4"))
        assertEquals(spotifyUrl,
            ArtistArtworkParser.profileUrl(ArtistArtworkSource.SPOTIFY, spotifyUrl.replace("/artist/", "/intl-it/artist/")))
        assertNull(ArtistArtworkParser.profileUrl(ArtistArtworkSource.SPOTIFY, spotifyUrl.replace("https:", "http:")))
        assertNull(ArtistArtworkParser.profileUrl(ArtistArtworkSource.SPOTIFY, spotifyUrl.replace("open.spotify.com", "open.spotify.com.evil.example")))
        assertNull(ArtistArtworkParser.profileUrl(ArtistArtworkSource.SPOTIFY, spotifyUrl.replace("open.spotify.com", "user@open.spotify.com")))
    }

    @Test
    fun toleratesMalformedStructuredDataWhenSpotifyMetadataIsAvailable() {
        val artwork = ArtistArtworkParser.parse(ArtistArtworkSource.SPOTIFY, "Ed Sheeran", spotifyUrl, """
            <script type="application/ld+json">{broken json}</script>
            <meta property="og:title" content="Ed Sheeran">
            <meta property="og:image" content="https://i.scdn.co/image/portrait">
        """)
        assertEquals("https://i.scdn.co/image/portrait", artwork?.imageUrl)
    }

    @Test
    fun requestsFourKArtistAssetWithoutKeepingLowQualitySuffix() {
        val compressed = portrait.replace("486x486bb.png", "486x486bb-60.jpg")
        assertEquals(compressed.replace("486x486bb-60.jpg", "3840x3840bb.jpg"),
            ArtistArtworkParser.imageUrl(ArtistArtworkSource.APPLE_MUSIC, compressed, 3840))
    }

    @Test
    fun upgradesWebpAndPreservesQueryParameters() {
        val webp = portrait.replace("486x486bb.png", "486x486bb.webp") + "?cache=artist"
        assertEquals(webp.replace("486x486", "3840x3840"),
            ArtistArtworkParser.imageUrl(ArtistArtworkSource.APPLE_MUSIC, webp, 3840))
    }

    @Test
    fun doesNotInventHighResolutionSpotifyUrlsOrRewriteUntrustedAppleUrls() {
        val spotify = "https://i.scdn.co/image/portrait"
        assertEquals(spotify, ArtistArtworkParser.imageUrl(ArtistArtworkSource.SPOTIFY, spotify, 3840))
        val untrusted = portrait.replace("is1-ssl.mzstatic.com", "example.com")
        assertEquals(untrusted, ArtistArtworkParser.imageUrl(ArtistArtworkSource.APPLE_MUSIC, untrusted, 3840))
    }
}
