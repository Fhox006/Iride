package com.metrolist.music.playback

import com.metrolist.music.ui.player.*
import org.junit.Assert.*
import org.junit.Test

class IrideMp3QueueMoodTest {
    private data class Track(
        val id: String,
        val artist: String?,
        val tags: List<String> = emptyList(),
    )

    private fun filter(items: List<Track>, mood: String, familiar: Set<String> = emptySet()) =
        matchingQueueMoodItems(
            items,
            mood,
            familiar,
            artists = { it.artist?.let(::listOf) },
            genres = { it.tags },
        )

    @Test fun unavailableFamiliarityFiltersAreNotOffered() {
        assertFalse(AUTOMIX_STATIC_FILTERS.contains(AUTOMIX_FILTER_DISCOVER))
        assertFalse(AUTOMIX_STATIC_FILTERS.contains(AUTOMIX_FILTER_FAMILIAR))
    }

    @Test
    fun unknownGenresDoNotPretendToMatchAMood() {
        assertTrue(filter(listOf(Track("a", "artist")), AUTOMIX_FILTER_PARTY).isEmpty())
        assertTrue(filter(listOf(Track("a", "artist")), AUTOMIX_FILTER_WORKOUT).isEmpty())
    }

    @Test
    fun resolvingTagsChangesTheActualFilterResult() {
        val unknown = listOf(Track("a", "artist"), Track("b", "other"))
        assertTrue(filter(unknown, AUTOMIX_FILTER_PARTY).isEmpty())
        val resolved =
            listOf(
                unknown[0].copy(tags = listOf("electronic dance")),
                unknown[1].copy(tags = listOf("jazz")),
            )
        assertEquals(listOf("a"), filter(resolved, AUTOMIX_FILTER_PARTY).map { it.id })
        assertEquals(listOf("b"), filter(resolved, "JAZZ").map { it.id })
    }

    @Test
    fun familiarAndDiscoverAreComplementaryForKnownArtists() {
        val items = listOf(Track("a", "known"), Track("b", "new"), Track("c", null))
        assertEquals(
            listOf("a"),
            filter(items, AUTOMIX_FILTER_FAMILIAR, setOf("known")).map { it.id },
        )
        assertEquals(
            listOf("b"),
            filter(items, AUTOMIX_FILTER_DISCOVER, setOf("known")).map { it.id },
        )
    }

    @Test
    fun clearingMoodRestoresOriginalOrderAndKeepsEveryEntry() {
        val order = IrideMp3QueueOrder<String>()
        val original = listOf("a", "b", "c", "d")
        order.synchronize(original)
        val prioritized = order.ordered(original, setOf("c", "d"))
        assertEquals(listOf("c", "d", "a", "b"), prioritized)
        order.synchronize(prioritized)
        assertEquals(original, order.ordered(prioritized))
        assertEquals(original, order.ordered(prioritized, emptySet()))
    }

    @Test
    fun manualReorderSurvivesMoodChanges() {
        val order = IrideMp3QueueOrder<String>()
        val original = listOf("a", "b", "c")
        order.synchronize(original)
        order.move("a", "c")
        val prioritized = order.ordered(original, setOf("a"))
        order.synchronize(prioritized)
        assertEquals(listOf("b", "c", "a"), order.ordered(prioritized))
    }

    @Test
    fun progressionRemovesPlayedEntriesAndAppendsNewRecommendations() {
        val order = IrideMp3QueueOrder<String>()
        order.synchronize(listOf("a", "b", "c"))
        assertFalse(order.synchronize(listOf("c", "b", "d")))
        assertEquals(listOf("b", "c", "d"), order.ordered(listOf("c", "b", "d")))
        assertTrue(order.synchronize(listOf("x", "y")))
        assertEquals(listOf("x", "y"), order.ordered(listOf("x", "y")))
    }

    @Test
    fun duplicateSongsRemainSeparateQueueEntries() {
        val order = IrideMp3QueueOrder<String>()
        val keys = listOf("song-first-occurrence", "other", "song-second-occurrence")
        order.synchronize(keys)
        assertEquals(3, order.ordered(keys).size)
        assertEquals(
            listOf(keys[0], keys[2], keys[1]),
            order.ordered(keys, setOf(keys[0], keys[2])),
        )
    }
}
