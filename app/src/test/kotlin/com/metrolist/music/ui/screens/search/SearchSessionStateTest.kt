package com.metrolist.music.ui.screens.search

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchSessionStateTest {
    @Test
    fun firstAndRepeatedManualSearchAlwaysConfirm() {
        var state = SearchSessionState()
        repeat(20) {
            val before = state.open()
            state = before.submit("  Radiohead  ")
            assertEquals("Radiohead", state.submittedQuery)
            assertEquals(SearchSceneMode.Result, state.event.mode)
            assertEquals(before.event.revision + 1, state.event.revision)
            assertTrue(state.event.confirmsSearch)
        }
    }

    @Test
    fun cancellingAnEditRestoresTheSameSubmittedPage() {
        for (query in listOf("Radiohead", "album", "song", "no results", "Rock")) {
            val submitted = SearchSessionState().submit(query)
            val returned = submitted.open().dismiss()
            assertEquals(query, returned.submittedQuery)
            assertEquals(SearchSceneMode.Result, returned.event.mode)
            assertFalse(returned.event.confirmsSearch)
            assertEquals(returned, returned.dismiss())
        }
    }

    @Test
    fun cancellingTheFirstDraftReturnsToGenresWithoutCreatingAResult() {
        val state = SearchSessionState().open().dismiss()
        assertEquals("", state.submittedQuery)
        assertEquals(SearchSceneMode.Landing, state.event.mode)
        assertFalse(state.event.returnsToRoot)
    }

    @Test
    fun arrowAndroidBackAndEmptySubmitKeepOutgoingDataUntilReverseCompletes() {
        val submitted = SearchSessionState().submit("Radiohead")
        val returns = listOf(submitted.reset(), submitted.open().submit("  "), submitted.open().reset())
        for (returning in returns) {
            assertEquals("Radiohead", returning.submittedQuery)
            assertEquals(SearchSceneMode.Landing, returning.event.mode)
            assertTrue(returning.event.returnsToRoot)
            assertFalse(searchSceneHasResult(returning.event, returning.submittedQuery))
            val completed = returning.finish(returning.event.revision)
            assertEquals("", completed.submittedQuery)
            assertFalse(completed.event.returnsToRoot)
            assertEquals(returning.event.revision, completed.event.revision)
        }
    }

    @Test
    fun repeatedRootTapsDoNotRestartOrLoseThePendingReturn() {
        val returning = SearchSessionState().submit("Radiohead").reset()
        repeat(20) {
            assertEquals(returning, returning.reset())
            assertEquals(returning, returning.submit(""))
            assertEquals(returning, returning.dismiss())
        }
    }

    @Test
    fun staleCompletionCannotEraseANewerSearchOrItsEditor() {
        val returning = SearchSessionState().submit("Radiohead").reset()
        for (next in listOf(returning.open(), returning.submit("Muse"), returning.open().submit("Muse"))) {
            assertEquals(next, next.finish(returning.event.revision))
            assertFalse(next.event.returnsToRoot)
        }
    }

    @Test
    fun reopeningDuringRootReturnStartsANewEmptyDraft() {
        val returning = SearchSessionState().submit("Radiohead").reset()
        val reopened = returning.open()
        assertEquals("", reopened.submittedQuery)
        assertEquals(SearchSceneMode.Editing, reopened.event.mode)
        assertFalse(searchSceneHasResult(reopened.event, reopened.submittedQuery))
        assertEquals(reopened, reopened.finish(returning.event.revision))
    }

    @Test
    fun androidBackClosesEditorThenReturnsToRoot() {
        val editing = SearchSessionState().submit("Radiohead").open()
        assertEquals(SearchBackAction.DismissEditor, searchBackAction(editing.event, editing.event))
        val result = editing.dismiss()
        assertEquals(SearchBackAction.ReturnToRoot, searchBackAction(result.event, result.event))
        val root = result.reset()
        assertEquals(SearchBackAction.None, searchBackAction(root.event, root.event))
    }

    @Test
    fun imeAndActivityCallbacksFromOneGestureCannotCloseTwoLevels() {
        val editing = SearchSessionState().submit("Radiohead").open()
        val returnedByIme = editing.dismiss()
        assertEquals(SearchBackAction.None, searchBackAction(editing.event, returnedByIme.event))
        val submitted = editing.submit("Muse")
        assertEquals(SearchBackAction.None, searchBackAction(editing.event, submitted.event))
    }

    @Test
    fun processRestorationKeepsCommittedQueriesButDoesNotReviveClearedResults() {
        val result = SearchSessionState().submit("Radiohead")
        assertEquals(result.submittedQuery, SearchSessionState(result.open().restoredQuery).submittedQuery)
        val cleared = SearchSessionState(result.reset().restoredQuery)
        assertEquals("", cleared.submittedQuery)
        assertEquals(SearchSceneMode.Landing, cleared.event.mode)
    }

    @Test
    fun allActionSequencesKeepMotionAndSubmittedDataConsistent() {
        val actions: List<(SearchSessionState) -> SearchSessionState> = listOf(
            { it.open() }, { it.dismiss() }, { it.reset() }, { it.submit("Radiohead") },
            { it.submit("Muse") }, { it.submit("") }, { it.finish(it.event.revision) },
        )
        var states = listOf(SearchSessionState())
        repeat(5) {
            val next = mutableListOf<SearchSessionState>()
            for (before in states) for (action in actions) {
                val state = action(before)
                assertTrue(state.event.revision >= before.event.revision)
                if (state.event.mode == SearchSceneMode.Result) assertTrue(state.submittedQuery.isNotBlank())
                if (state.event.confirmsSearch) assertEquals(SearchSceneMode.Result, state.event.mode)
                if (state.event.returnsToRoot) {
                    assertEquals(SearchSceneMode.Landing, state.event.mode)
                    assertFalse(state.event.confirmsSearch)
                    assertEquals(state, state.reset())
                }
                if (state.event.revision > before.event.revision) {
                    assertEquals(state, state.finish(before.event.revision))
                }
                next += state
            }
            states = next
        }
    }
}
