package com.metrolist.music.playback

import com.metrolist.music.ui.player.IrideMp3QueueOrder
import com.metrolist.music.ui.player.queueHistoryOldestFirst
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class IrideMp3QueueHistoryTest {
    @Test
    fun latestListenIsImmediatelyAboveTheQueue() {
        assertEquals(
            listOf("oldest", "middle", "latest"),
            queueHistoryOldestFirst(listOf("latest", "middle", "oldest"), emptyList()),
        )
    }

    @Test
    fun newlyFinishedSongsStayBesideQueueAfterManualReordering() {
        assertEquals(
            listOf("middle", "oldest", "latest", "just-finished"),
            queueHistoryOldestFirst(
                listOf("just-finished", "latest", "middle", "oldest"),
                listOf("middle", "oldest", "latest"),
            ),
        )
    }

    @Test
    fun historyRefreshDropsMissingSongsWithoutChangingManualOrder() {
        assertEquals(
            listOf("middle", "latest"),
            queueHistoryOldestFirst(
                listOf("latest", "middle"),
                listOf("middle", "removed", "latest"),
            ),
        )
        assertEquals(emptyList<String>(), queueHistoryOldestFirst(emptyList(), listOf("removed")))
    }

    @Test
    fun historyDropPositionSurvivesMoodChangesAndPlaybackProgression() {
        val order = IrideMp3QueueOrder<String>()
        order.synchronize(listOf("a", "b", "c"))
        val afterDrop = listOf("c", "history", "a", "b")
        order.setManualOrder(afterDrop)

        val filtered = order.ordered(afterDrop, setOf("a", "b"))
        assertEquals(listOf("a", "b", "c", "history"), filtered)
        assertFalse(order.synchronize(filtered))
        assertEquals(afterDrop, order.ordered(filtered))

        val advanced = listOf("a", "b", "recommendation")
        assertFalse(order.synchronize(advanced))
        assertEquals(advanced, order.ordered(advanced))
    }

    @Test
    fun requeuedHistorySongKeepsItsOwnPositionBesideAnotherOccurrence() {
        val order = IrideMp3QueueOrder<String>()
        val afterDrop = listOf("song-original", "other", "song-from-history")
        order.setManualOrder(afterDrop)
        order.move("song-from-history", "other")

        val updated = afterDrop + "recommendation"
        assertFalse(order.synchronize(updated))
        assertEquals(
            listOf("song-original", "song-from-history", "other", "recommendation"),
            order.ordered(updated),
        )
    }
}
