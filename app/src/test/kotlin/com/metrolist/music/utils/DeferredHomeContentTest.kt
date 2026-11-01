package com.metrolist.music.utils

import org.junit.Assert.assertEquals
import org.junit.Test

class DeferredHomeContentTest {
    @Test fun keepsRefreshesAndRemovalsPendingUntilHomeIsHidden() {
        val content = DeferredHomeContent(listOf("first"), List<String>::isNotEmpty)
        assertEquals(listOf("first"), content.update(listOf("second")))
        assertEquals(listOf("first"), content.update(emptyList()))
        assertEquals(emptyList<String>(), content.setVisible(false))
        content.update(listOf("latest"))
        content.setVisible(true)
        assertEquals(listOf("latest"), content.update(listOf("pending")))
        assertEquals(listOf("pending"), content.setVisible(false))
    }

    @Test fun allowsInitialLoadingButFreezesOnceTheShelfCanBeRendered() {
        val content = DeferredHomeContent(emptyList<String>(), { it.size >= 4 })
        assertEquals(listOf("one"), content.update(listOf("one")))
        val complete = listOf("one", "two", "three", "four")
        assertEquals(complete, content.update(complete))
        assertEquals(complete, content.update(complete.reversed()))
    }

    @Test fun switchingMoodChangesContentButRefreshingTheSameMoodWaits() {
        val content = DeferredHomeContent("chill" to listOf("first"),
            { it.second.isNotEmpty() }, { it.first })
        assertEquals("chill" to listOf("first"), content.update("chill" to listOf("refreshed")))
        assertEquals("energy" to listOf("new"), content.update("energy" to listOf("new")))
        assertEquals("energy" to listOf("new"), content.update("energy" to listOf("pending")))
    }

    @Test fun publishesAdditionalSectionsWithoutMovingOrRefreshingVisibleSections() {
        val first = "artist-1" to "original"
        val second = "album-1" to "second"
        val third = "playlist-1" to "third"
        val content = DeferredHomeContent(
            emptyList<Pair<String, String>>(),
            { it.isNotEmpty() },
            mergeWhileVisible = { current, incoming ->
                appendLoadedHomeSections(current, incoming) { it.first }
            },
        )

        assertEquals(listOf(first), content.update(listOf(first)))
        val loaded = listOf(second, "artist-1" to "refreshed", third, second)
        assertEquals(listOf(first, second, third), content.update(loaded))
        assertEquals(listOf(first, second, third), content.update(loaded.reversed()))
        assertEquals(loaded.reversed(), content.setVisible(false))
    }

    @Test fun keepsReplacementBatchesAndRemovalsPendingEvenWhenGrowthIsEnabled() {
        val content = DeferredHomeContent(
            listOf("first", "second"),
            { it.isNotEmpty() },
            mergeWhileVisible = { current, incoming ->
                appendLoadedHomeSections(current, incoming) { it }
            },
        )

        assertEquals(listOf("first", "second"), content.update(listOf("replacement", "other", "third")))
        assertEquals(listOf("first", "second"), content.update(listOf("first")))
        assertEquals(listOf("first", "second"), content.update(emptyList()))
        assertEquals(emptyList<String>(), content.setVisible(false))
    }
}
