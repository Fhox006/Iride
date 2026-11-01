package com.metrolist.music.ui.screens.search

import org.junit.Assert.assertEquals
import org.junit.Test

class SearchCompletionTest {
    @Test
    fun acceptingSuggestionKeepsTypedPrefixAndOnlyTypesSuffix() {
        val completed = searchCompletionText("rad", "Radiohead")
        assertEquals("radiohead", completed)
        assertEquals(3, searchCompletionPrefixLength("rad", completed))
    }

    @Test
    fun recentWithEmptyFieldTypesEntireName() {
        val completed = searchCompletionText("", "Daft Punk")
        assertEquals("Daft Punk", completed)
        assertEquals(0, searchCompletionPrefixLength("", completed))
    }

    @Test
    fun correctingSuggestionOnlyRetypesFromFirstDifference() {
        val completed = searchCompletionText("Rado", "Radiohead")
        assertEquals("Radiohead", completed)
        assertEquals(3, searchCompletionPrefixLength("Rado", completed))
    }

    @Test
    fun alreadyCompleteNameDoesNotReplayTyping() {
        assertEquals(9, searchCompletionPrefixLength("Radiohead", "Radiohead"))
    }

    @Test
    fun correctingASuggestionKeepsTheCaseOfTheAlreadyTypedPrefix() {
        val completed = searchCompletionText("rado", "Radiohead")
        assertEquals("radiohead", completed)
        assertEquals(3, searchCompletionPrefixLength("rado", completed))
    }

    @Test
    fun unchangedSubmittedTextIsStillAUserPrefixWhenModifyingTheSearch() {
        val completed = searchCompletionText("Muse", "Muse live")
        assertEquals("Muse live", completed)
        assertEquals(4, searchCompletionPrefixLength("Muse", completed))
    }
}
