package com.metrolist.music.ui.component

import org.junit.Assert.assertEquals
import org.junit.Test

class ArtistBackgroundScrollTrackerTest {
    @Test
    fun continuesTravelingAfterPhotoAndHeaderLeaveScreen() {
        val tracker = ArtistBackgroundScrollTracker()
        tracker.update(0, 500, listOf(ArtistScrollAnchor(-500, 800, "header"), ArtistScrollAnchor(300, 400, "songs")))
        assertEquals(1000f, tracker.update(1, 200, listOf(ArtistScrollAnchor(-200, 400, "songs"))), 0f)
        assertEquals(1150f, tracker.update(1, 350, listOf(ArtistScrollAnchor(-350, 400, "songs"))), 0f)
    }

    @Test
    fun reverseScrollAndReturnToTopRestoreOriginalBackgroundPosition() {
        val tracker = ArtistBackgroundScrollTracker()
        tracker.update(0, 500, listOf(ArtistScrollAnchor(300, 400, "songs")))
        tracker.update(1, 200, listOf(ArtistScrollAnchor(-200, 400, "songs")))
        assertEquals(850f, tracker.update(1, 50, listOf(ArtistScrollAnchor(-50, 400, "songs"))), 0f)
        assertEquals(0f, tracker.update(0, 0, listOf(ArtistScrollAnchor(0, 800, "header"))), 0f)
    }

    @Test
    fun contentReindexingUsesStableKeyRatherThanAssumingItemHeights() {
        val tracker = ArtistBackgroundScrollTracker()
        tracker.update(0, 500, listOf(ArtistScrollAnchor(300, 400, "songs")))
        assertEquals(1000f, tracker.update(2, 200, listOf(ArtistScrollAnchor(-200, 400, "songs"))), 0f)
    }

    @Test
    fun stationaryLayoutUpdatesDoNotAnimateBackground() {
        val tracker = ArtistBackgroundScrollTracker()
        tracker.update(0, 500, listOf(ArtistScrollAnchor(300, 400, "songs")))
        tracker.update(1, 200, listOf(ArtistScrollAnchor(-200, 400, "songs")))
        assertEquals(1000f, tracker.update(1, 200, listOf(ArtistScrollAnchor(-200, 600, "songs"))), 0f)
    }
}
