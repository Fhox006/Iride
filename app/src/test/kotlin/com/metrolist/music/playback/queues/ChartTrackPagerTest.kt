package com.metrolist.music.playback.queues

import com.metrolist.innertube.models.Artist
import com.metrolist.innertube.models.SongItem
import com.metrolist.music.data.remote.ChartTrack
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ChartTrackPagerTest {
    private fun tracks(count: Int = 20) = (1..count).map {
        ChartTrack(it, it.toString().padStart(22, '0'), "Song $it", listOf("Artist"), 100)
    }
    private fun song(track: ChartTrack, explicit: Boolean = false) = SongItem(
        track.rank.toString(), track.title, listOf(Artist("Artist", null)),
        thumbnail = "", musicVideoType = "MUSIC_VIDEO_TYPE_ATV", explicit = explicit,
    )

    @Test fun preservesRankDespiteOutOfOrderSearchCompletion() = runBlocking {
        val pager = ChartTrackPager(tracks(), false) { delay((21 - it.rank).toLong()); song(it) }
        val actual = mutableListOf<String>()
        while (pager.hasNextPage()) actual += pager.nextPage().map { it.id }
        assertEquals((1..20).map { it.toString() }, actual)
        assertTrue(pager.nextPage().isEmpty())
    }

    @Test fun skipsUnavailableAndFilteredTracksWithoutRenumberingOrStoppingEarly() = runBlocking {
        val pager = ChartTrackPager(tracks(), true) {
            if (it.rank <= 8 || it.rank == 11) null else song(it, explicit = it.rank == 9)
        }
        assertEquals(listOf("10", "12", "13", "14", "15", "16"), pager.nextPage().map { it.id })
        assertEquals(listOf("17", "18", "19", "20"), pager.nextPage().map { it.id })
        assertFalse(pager.hasNextPage())
    }

    @Test fun networkFailureDoesNotConsumePositions() = runBlocking {
        var fail = true
        val pager = ChartTrackPager(tracks(8), false) {
            if (fail && it.rank == 3) throw IllegalStateException("offline")
            song(it)
        }
        try {
            pager.nextPage()
            throw AssertionError("Expected failure")
        } catch (_: IllegalStateException) { }
        assertTrue(pager.hasNextPage())
        fail = false
        assertEquals((1..8).map { it.toString() }, pager.nextPage().map { it.id })
        assertFalse(pager.hasNextPage())
    }

    @Test fun concurrentPageRequestsHaveNoDuplicatesOrMissingPositions() = runBlocking {
        val pager = ChartTrackPager(tracks(16), false) { delay(5); song(it) }
        val first = async { pager.nextPage() }
        val second = async { pager.nextPage() }
        assertEquals((1..16).map { it.toString() }, (first.await() + second.await()).map { it.id })
    }

    @Test fun cancellationDoesNotConsumePositions() = runBlocking {
        var cancel = true
        val pager = ChartTrackPager(tracks(8), false) {
            if (cancel) throw CancellationException("cancelled")
            song(it)
        }
        try {
            pager.nextPage()
            throw AssertionError("Expected cancellation")
        } catch (_: CancellationException) { }
        cancel = false
        assertEquals((1..8).map { it.toString() }, pager.nextPage().map { it.id })
    }
}
