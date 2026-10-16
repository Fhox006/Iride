package com.metrolist.music.playback

import com.metrolist.music.ui.player.IrideMp3QueueOrder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class IrideMp3QueueHistoryTest {
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
