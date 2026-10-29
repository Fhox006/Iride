package com.metrolist.music.data.remote

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ArtistVideoSegmentTest {
    private val playlistUrl = "https://video.example.com/artist/h264/playlist.m3u8?token=value"

    @Test
    fun takesFirstSegmentWithInitializationAndResolvesRelativeUrls() {
        val segment = firstArtistVideoSegment("""
            #EXTM3U
            #EXT-X-MAP:URI="../init.mp4?token=init"
            #EXTINF:6,
            first.m4s?token=first
            #EXTINF:6,
            second.m4s
        """.trimIndent(), playlistUrl)
        assertEquals("https://video.example.com/artist/init.mp4?token=init", segment?.initialization?.url)
        assertEquals("https://video.example.com/artist/h264/first.m4s?token=first", segment?.media?.url)
    }

    @Test
    fun supportsInitializationAndFirstFrameInDifferentRangesOfSameFile() {
        val segment = firstArtistVideoSegment("""
            #EXTM3U
            #EXT-X-MAP:URI="video.mp4",BYTERANGE="900@0"
            #EXTINF:6,
            #EXT-X-BYTERANGE:4096@900
            video.mp4
        """.trimIndent(), playlistUrl)
        assertEquals(0L..899L, segment?.initialization?.range)
        assertEquals(900L..4995L, segment?.media?.range)
    }

    @Test
    fun acceptsTransportStreamWithoutInitializationAndCrlfLines() {
        val segment = firstArtistVideoSegment("#EXTM3U\r\n#EXTINF:5,\r\n/first.ts\r\n", playlistUrl)
        assertNull(segment?.initialization)
        assertEquals("https://video.example.com/first.ts", segment?.media?.url)
    }

    @Test
    fun rejectsEncryptedMediaAndEncryptedInitializationEvenAfterKeyChanges() {
        assertNull(firstArtistVideoSegment("""
            #EXTM3U
            #EXT-X-KEY:METHOD=AES-128,URI="key"
            #EXTINF:6,
            first.m4s
        """.trimIndent(), playlistUrl))
        assertNull(firstArtistVideoSegment("""
            #EXTM3U
            #EXT-X-KEY:METHOD=AES-128,URI="key"
            #EXT-X-MAP:URI="init.mp4"
            #EXT-X-KEY:METHOD=NONE
            #EXTINF:6,
            first.m4s
        """.trimIndent(), playlistUrl))
    }

    @Test
    fun rejectsMissingSegmentsInvalidPlaylistsAndUnselectedMasterPlaylists() {
        assertNull(firstArtistVideoSegment("<html>Unavailable</html>", playlistUrl))
        assertNull(firstArtistVideoSegment("#EXTM3U\n#EXT-X-ENDLIST", playlistUrl))
        assertNull(firstArtistVideoSegment("#EXTM3U\n#EXT-X-STREAM-INF:BANDWIDTH=100\nvariant.m3u8", playlistUrl))
        assertNull(firstArtistVideoSegment("#EXTM3U\n#EXT-X-I-FRAMES-ONLY\nframe.mp4", playlistUrl))
    }

    @Test
    fun rejectsInvalidByteRangesWithoutThrowingOrReadingEntireVideo() {
        for (range in listOf("500", "0@0", "-1@0", "500@-1", "500@0@1", "9223372036854775807@50", "invalid")) {
            assertNull(range, firstArtistVideoSegment("#EXTM3U\n#EXT-X-BYTERANGE:$range\nvideo.mp4", playlistUrl))
        }
    }

    @Test
    fun rejectsInvalidMediaAndInitializationUrls() {
        for (url in listOf("http://video.example.com/frame.mp4", "file:///frame.mp4", "https://user@video.example.com/frame.mp4", "https://[invalid")) {
            assertNull(firstArtistVideoSegment("#EXTM3U\n$url", playlistUrl))
            assertNull(firstArtistVideoSegment("#EXTM3U\n#EXT-X-MAP:URI=\"$url\"\nfirst.m4s", playlistUrl))
        }
    }

    @Test
    fun selectsBoundedH264RenditionInsteadOfHevcForPortraitExtraction() {
        val variants = parseAMHlsVariants("""
            #EXTM3U
            #EXT-X-STREAM-INF:BANDWIDTH=100000,RESOLUTION=1200x1200,CODECS="hvc1.2.4"
            hevc.m3u8
            #EXT-X-STREAM-INF:BANDWIDTH=400000,RESOLUTION=640x640,CODECS="avc1.64001f"
            small.m3u8
            #EXT-X-STREAM-INF:BANDWIDTH=900000,RESOLUTION=1200x1200,CODECS="avc1.640028"
            portrait.m3u8
            #EXT-X-STREAM-INF:BANDWIDTH=3000000,RESOLUTION=3840x3840,CODECS="avc1.640033"
            large.m3u8
        """.trimIndent(), playlistUrl)
        val selected = variants.filter { it.isH264 && it.width in 1..1920 && it.height in 1..1920 }.pickAMRendition(1200)
        assertEquals("https://video.example.com/artist/h264/portrait.m3u8", selected?.uri)
    }
}
