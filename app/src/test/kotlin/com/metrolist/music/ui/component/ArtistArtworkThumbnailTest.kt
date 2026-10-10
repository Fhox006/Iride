package com.metrolist.music.ui.component

import org.junit.Assert.assertEquals
import org.junit.Test

class ArtistArtworkThumbnailTest {
    @Test
    fun youtubePreviewRequestsSmallPhotoWithoutChangingIdentity() {
        val url = "https://lh3.googleusercontent.com/artist=w1200-h1200-p-l90-rj"
        assertEquals("https://lh3.googleusercontent.com/artist=w448-h448-p-l90-rj",
            artistArtworkDownloadUrl(url, 448))
        assertEquals("https://lh3.googleusercontent.com/artist=w192-h192-p-l90-rj",
            artistArtworkDownloadUrl(url, 192))
    }

    @Test
    fun applePreviewPreservesAssetAndQuery() {
        val url = "https://is1-ssl.mzstatic.com/image/thumb/AMCArtistImages211/photo.png/1200x1200bb.png?token=abc"
        assertEquals(url.replace("1200x1200", "448x448"), artistArtworkDownloadUrl(url, 448))
        assertEquals(url.replace("1200x1200", "1920x1920"), artistArtworkDownloadUrl(url, 1920))
    }

    @Test
    fun spotifyAndLocalPhotosKeepOriginalUrl() {
        for (url in listOf("https://i.scdn.co/image/portrait", "content://media/artist/12")) {
            assertEquals(url, artistArtworkDownloadUrl(url, 448))
        }
    }

    @Test
    fun youtubeChannelThumbnailCanUpgradeSamePhotoToHd() {
        val url = "https://yt3.ggpht.com/portrait=s1200"
        val preview = artistArtworkDownloadUrl(url, 448)
        assertEquals("https://yt3.ggpht.com/portrait=s448", preview)
        assertEquals(url, artistArtworkDownloadUrl(preview, 1200))
    }
}
