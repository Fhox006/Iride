package com.metrolist.music.ui.component

import androidx.media3.exoplayer.offline.Download
import org.junit.Assert.assertEquals
import org.junit.Test

class TrackRowPresentationTest {
    @Test
    fun featuringInTitleMovesIntoArtistCredits() {
        assertEquals(
            "Last Train Artist feat. Sayf",
            formatTrackArtistCredits(listOf("Last Train Artist"), "Last Train 2 Shibuya feat. Sayf"),
        )
        assertEquals("Last Train 2 Shibuya", cleanTrackTitle("Last Train 2 Shibuya (feat. Sayf)"))
    }

    @Test
    fun linkedFeaturedArtistsRemainVisibleAfterTitleWasCleaned() {
        assertEquals("Artist feat. Guest", formatTrackArtistCredits(listOf("Artist", "Guest")))
    }

    @Test
    fun duplicateFeaturingCreditsAppearOnce() {
        assertEquals(
            "Artist feat. Guest",
            formatTrackArtistCredits(listOf("Artist", "Guest"), "Song feat. Guest"),
        )
    }

    @Test
    fun downloadStatesHaveExplicitRowPresentation() {
        assertEquals(TrackDownloadStatus.NOT_DOWNLOADED, trackDownloadStatus(null))
        assertEquals(TrackDownloadStatus.NOT_DOWNLOADED, trackDownloadStatus(Download.STATE_STOPPED))
        assertEquals(TrackDownloadStatus.QUEUED, trackDownloadStatus(Download.STATE_QUEUED))
        assertEquals(TrackDownloadStatus.DOWNLOADING, trackDownloadStatus(Download.STATE_DOWNLOADING))
        assertEquals(TrackDownloadStatus.DOWNLOADED, trackDownloadStatus(Download.STATE_COMPLETED))
        assertEquals(TrackDownloadStatus.FAILED, trackDownloadStatus(Download.STATE_FAILED))
        assertEquals(TrackDownloadStatus.REMOVING, trackDownloadStatus(Download.STATE_REMOVING))
    }
}
