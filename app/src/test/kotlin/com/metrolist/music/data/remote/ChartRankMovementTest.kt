package com.metrolist.music.data.remote

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ChartRankMovementTest {
    private fun chart(vararg ids: Int) = DailyChart(
        marketCode = "it",
        date = "2026-10-10",
        checkedAt = 123L,
        sourceTrackCount = ids.size,
        tracks = ids.mapIndexed { index, id ->
            ChartTrack(index + 1, id.toString().padStart(22, '0'), "Track $id", listOf("Artist"), 100L)
        },
    )

    @Test
    fun comparesTrackIdentityAcrossReorderingAndNewEntries() {
        val updated = chart(2, 1, 4, 3).withRankMovements(chart(1, 2, 3))
        assertEquals(
            listOf(ChartRankMovement.UP, ChartRankMovement.DOWN, ChartRankMovement.NEW, ChartRankMovement.DOWN),
            updated.tracks.map { it.movement },
        )
    }

    @Test
    fun unchangedRefreshClearsEveryPreviousIndicator() {
        val changed = chart(2, 1, 4).withRankMovements(chart(1, 2, 3))
        val unchanged = changed.withRankMovements(changed)
        assertEquals(listOf(null, null, null), unchanged.tracks.map { it.movement })
    }

    @Test
    fun firstSnapshotAndOtherMarketsHaveNoComparison() {
        val current = chart(1, 2)
        assertEquals(listOf(null, null), current.withRankMovements(null).tracks.map { it.movement })
        assertEquals(listOf(null, null), current.withRankMovements(chart(2, 1).copy(marketCode = "us")).tracks.map { it.movement })
        assertEquals(listOf(null, null), current.withRankMovements(chart(2, 1).copy(date = "2026-10-11")).tracks.map { it.movement })
    }

    @Test
    fun movementUsesCurrentRankOnBothSidesOfTop25() {
        val previous = chart(*(1..26).toList().toIntArray())
        val current = chart(*(1..24).toList().plus(listOf(26, 25)).toIntArray()).withRankMovements(previous)
        assertEquals(25, current.tracks[24].rank)
        assertEquals(ChartRankMovement.UP, current.tracks[24].movement)
        assertEquals(26, current.tracks[25].rank)
        assertEquals(ChartRankMovement.DOWN, current.tracks[25].movement)
        assertNull(current.tracks[23].movement)
    }

    @Test
    fun reentryIsNewEvenWhenItOccupiesDepartedTracksRank() {
        val previous = chart(1, 2, 3)
        val absent = chart(1, 2, 4).withRankMovements(previous)
        val reentered = previous.withRankMovements(absent)
        assertEquals(ChartRankMovement.NEW, reentered.tracks.last().movement)
        assertNull(reentered.tracks.first().movement)
    }

    @Test
    fun cachePreservesIndicatorsAndAcceptsSnapshotsWithoutThem() {
        val changed = chart(2, 1, 4).withRankMovements(chart(1, 2, 3))
        assertEquals(changed, Json.decodeFromString<DailyChart>(Json.encodeToString(changed)))
        val oldSnapshot = Json.encodeToString(chart(1))
        assertNull(Json.decodeFromString<DailyChart>(oldSnapshot).tracks.single().movement)
    }
}
