package com.metrolist.music.ui.screens.search

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchSceneEventTest {
    @Test
    fun everyRepeatedSubmissionHasItsOwnConfirmation() {
        var event = SearchSceneEvent()
        repeat(20) {
            event = event.open()
            val previous = event.revision
            event = event.submit(hasQuery = true)
            assertTrue(event.revision > previous)
            assertTrue(event.confirmsSearch)
            assertEquals(SearchSceneMode.Result, event.mode)
        }
    }

    @Test
    fun pointerDownAndAccessibilityClickDoNotRestartAnOpenEditor() {
        val pressed = SearchSceneEvent().open()
        assertEquals(pressed, pressed.open())
        assertFalse(pressed.confirmsSearch)
    }

    @Test
    fun emptySubmitReturnsToTheRootWithANewMotionEvent() {
        val result = SearchSceneEvent().open().submit(true)
        val root = result.submit(false)
        assertTrue(root.revision > result.revision)
        assertEquals(SearchSceneMode.Landing, root.mode)
        assertFalse(root.confirmsSearch)
    }

    @Test
    fun dismissingDoesNotPlayAnotherSubmission() {
        val editing = SearchSceneEvent().submit(true).open()
        val dismissed = editing.dismiss(true)
        assertTrue(dismissed.revision > editing.revision)
        assertEquals(SearchSceneMode.Result, dismissed.mode)
        assertFalse(dismissed.confirmsSearch)
    }

    @Test
    fun keyboardBackAndActivityBackCannotRestartTheSameReturn() {
        val editing = SearchSceneEvent().submit(true).open()
        val returned = editing.dismiss(true)
        assertEquals(returned, returned.dismiss(true))
        val root = returned.submit(false)
        assertEquals(root, root.dismiss(false))
    }

    @Test
    fun lifecycleOrKeyboardNotificationsCannotReplaceASubmission() {
        val submitted = SearchSceneEvent().open().submit(true)
        assertEquals(submitted, submitted.dismiss(true))
        val emptySubmitted = submitted.open().submit(false)
        assertEquals(emptySubmitted, emptySubmitted.dismiss(false))
    }
}
