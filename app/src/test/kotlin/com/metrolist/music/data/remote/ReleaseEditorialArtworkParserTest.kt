package com.metrolist.music.data.remote

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReleaseEditorialArtworkParserTest {
    private val profile = "https://music.apple.com/us/artist/ed-sheeran/183313439"
    private val still = "https://is1-ssl.mzstatic.com/image/thumb/Features116/editorial.png/{w}x{h}{c}.{f}"
    private val motion = "https://is1-ssl.mzstatic.com/image/thumb/Video221/preview.png/{w}x{h}{c}.{f}"

    @Test
    fun prefersDedicatedLandscapePhotographyAndKeepsOriginalAspectRatio() {
        val images = ReleaseEditorialArtworkParser.editorialImages("Ed Sheeran", profile, page())
        assertEquals(
            listOf(
                still.replace("{w}x{h}{c}.{f}", "1600x800bb.jpg"),
                motion.replace("{w}x{h}{c}.{f}", "1600x900bb.jpg"),
            ),
            images,
        )
    }

    @Test
    fun neverUsesAlbumCoverAsEditorialPhotography() {
        val html = page(stillUrl = still.replace("Features116", "Music221"), motionUrl = motion.replace("Video221", "Music221"))
        assertTrue(ReleaseEditorialArtworkParser.editorialImages("Ed Sheeran", profile, html).isEmpty())
    }

    @Test
    fun neverUsesRecommendationOrOtherArtistArtwork() {
        val recommendation = page().replace("artistDetailHeader", "similarArtists")
        assertTrue(ReleaseEditorialArtworkParser.editorialImages("Ed Sheeran", profile, recommendation).isEmpty())
        val wrongHeader = page().replace("\"id\":\"183313439\"", "\"id\":\"159260351\"")
        assertTrue(ReleaseEditorialArtworkParser.editorialImages("Ed Sheeran", profile, wrongHeader).isEmpty())
    }

    @Test
    fun requiresCanonicalStructuredArtistAndMatchingPageIntent() {
        assertTrue(ReleaseEditorialArtworkParser.editorialImages("Another Artist", profile, page()).isEmpty())
        val redirected = page().replace(
            "<meta property=\"og:url\" content=\"$profile\">",
            "<meta property=\"og:url\" content=\"https://music.apple.com/us/artist/159260351\">",
        )
        assertTrue(ReleaseEditorialArtworkParser.editorialImages("Ed Sheeran", profile, redirected).isEmpty())
        val wrongIntent = page().replace("ArtistDetailPageIntent", "AlbumDetailPageIntent")
        assertTrue(ReleaseEditorialArtworkParser.editorialImages("Ed Sheeran", profile, wrongIntent).isEmpty())
        val wrongArtistSchema = page().replace("\"@type\":\"MusicGroup\"", "\"@type\":\"MusicAlbum\"")
        assertTrue(ReleaseEditorialArtworkParser.editorialImages("Ed Sheeran", profile, wrongArtistSchema).isEmpty())
    }

    @Test
    fun rejectsUntrustedCdnHostAndUnsafeImageUrls() {
        listOf(
            still.replace("is1-ssl.mzstatic.com", "is1-ssl.mzstatic.com.evil.example"),
            still.replace("https:", "http:"),
            still.replace("is1-ssl.mzstatic.com", "user@is1-ssl.mzstatic.com"),
            still.replace("is1-ssl.mzstatic.com", "is1-ssl.mzstatic.com:7443"),
        ).forEach { url ->
            val html = page(stillUrl = url, motionUrl = url)
            assertTrue(ReleaseEditorialArtworkParser.editorialImages("Ed Sheeran", profile, html).isEmpty())
        }
        assertFalse(ReleaseEditorialArtworkParser.safeImageUrl("file:///tmp/cover.jpg"))
        assertFalse(ReleaseEditorialArtworkParser.safeImageUrl("https://user:password@example.com/cover.jpg"))
    }

    @Test
    fun doesNotLabelSquarePortraitOrSmallImageAsEditorialLandscape() {
        val square = page().replace("\"width\":4320", "\"width\":2160")
            .replace("\"width\":3840", "\"width\":2160")
        assertTrue(ReleaseEditorialArtworkParser.editorialImages("Ed Sheeran", profile, square).isEmpty())
        val tiny = page().replace("\"width\":4320", "\"width\":800")
            .replace("\"width\":3840", "\"width\":800")
        assertTrue(ReleaseEditorialArtworkParser.editorialImages("Ed Sheeran", profile, tiny).isEmpty())
    }

    @Test
    fun malformedOrMissingPageStateAllowsFallback() {
        assertTrue(ReleaseEditorialArtworkParser.editorialImages("Ed Sheeran", profile, "<html></html>").isEmpty())
        val malformed = page().replace("\"data\":[", "\"data\":][")
        assertTrue(ReleaseEditorialArtworkParser.editorialImages("Ed Sheeran", profile, malformed).isEmpty())
    }

    @Test
    fun releaseIdentityAllowsEpAndSingleSuffixButKeepsEditionDistinct() {
        assertTrue(ReleaseEditorialArtworkParser.sameReleaseTitle("Camera", "Camera - Single"))
        assertTrue(ReleaseEditorialArtworkParser.sameReleaseTitle("Play (The Remixes)", "Play (The Remixes) - EP"))
        assertFalse(ReleaseEditorialArtworkParser.sameReleaseTitle("Play", "Play (Extended Edition)"))
        assertFalse(ReleaseEditorialArtworkParser.sameReleaseTitle("", " - Single"))
    }

    private fun page(stillUrl: String = still, motionUrl: String = motion): String = """
        <meta property="og:url" content="$profile">
        <script type="application/ld+json">{
            "@type":"MusicGroup","name":"Ed Sheeran","url":"$profile"
        }</script>
        <script id="serialized-server-data" type="application/json">{
            "data":[{
                "intent":{
                    "${'$'}kind":"ArtistDetailPageIntent",
                    "contentDescriptor":{"kind":"artist","identifiers":{"storeAdamID":"183313439"}}
                },
                "data":{"sections":[{
                    "itemKind":"artistDetailHeader",
                    "items":[{
                        "id":"183313439","title":"Ed Sheeran",
                        "contentDescriptor":{"kind":"artist","identifiers":{"storeAdamID":"183313439"}},
                        "wideArtwork":{"dictionary":{"url":"$stillUrl","width":4320,"height":2160}},
                        "wideVideoArtwork":{"dictionary":{
                            "motionArtistWide16x9":{"previewFrame":{"url":"$motionUrl","width":3840,"height":2160}}
                        }}
                    }]
                }]}
            }]
        }</script>
    """.trimIndent()
}
