package com.metrolist.music.playback

import com.metrolist.innertube.models.*
import com.metrolist.innertube.pages.HomePage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeVideoMetadataTest {
    private fun parse(type: String): SongItem {
        val endpoint = WatchEndpoint(
            videoId = "video",
            watchEndpointMusicSupportedConfigs = WatchEndpoint.WatchEndpointMusicSupportedConfigs(
                WatchEndpoint.WatchEndpointMusicSupportedConfigs.WatchEndpointMusicConfig(type),
            ),
        )
        val item = MusicTwoRowItemRenderer(
            title = Runs(listOf(Run("Song", null))),
            subtitle = Runs(listOf(Run("Artist", NavigationEndpoint(browseEndpoint = BrowseEndpoint("UCartist"))))),
            subtitleBadges = null,
            menu = null,
            thumbnailRenderer = ThumbnailRenderer(
                musicThumbnailRenderer = ThumbnailRenderer.MusicThumbnailRenderer(
                    Thumbnails(listOf(Thumbnail("https://i.ytimg.com/vi/video/hqdefault.jpg", 480, 360))), null, null,
                ),
                musicAnimatedThumbnailRenderer = null,
                croppedSquareThumbnailRenderer = null,
            ),
            navigationEndpoint = NavigationEndpoint(watchEndpoint = endpoint),
            thumbnailOverlay = null,
        )
        val shelf = MusicCarouselShelfRenderer(
            header = MusicCarouselShelfRenderer.Header(
                MusicCarouselShelfRenderer.Header.MusicCarouselShelfBasicHeaderRenderer(
                    strapline = null, title = Runs(listOf(Run("Videos", null))), thumbnail = null, moreContentButton = null,
                ),
            ),
            contents = listOf(MusicCarouselShelfRenderer.Content(item, null, null, null)),
            itemSize = "MUSIC_TWO_ROW_ITEM_SIZE_MEDIUM",
            numItemsPerColumn = null,
        )
        return HomePage.Section.fromMusicCarouselShelfRenderer(shelf)!!.items.single() as SongItem
    }

    @Test fun homePreservesVideoTypeAndWatchEndpoint() {
        val item = parse("MUSIC_VIDEO_TYPE_OMV")
        assertTrue(item.isVideoSong)
        assertEquals("video", item.endpoint?.videoId)
    }

    @Test fun homeKeepsAudioTracksDistinctFromVideos() {
        assertFalse(parse("MUSIC_VIDEO_TYPE_ATV").isVideoSong)
    }
}
