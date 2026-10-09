package com.metrolist.music.ui.screens.search

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchScenePlacementTest {
    @Test
    fun photoCannotReappearAtTheTopWhenLazyHeaderIsRecycled() {
        assertEquals(-399f, searchArtworkPageOffset(-399, 800f, 0), 0f)
        val offscreen = searchArtworkPageOffset(null, 800f, 1)
        assertTrue(offscreen + 400f <= 0f)
        assertEquals(offscreen, searchArtworkPageOffset(null, 800f, 20), 0f)
        assertEquals(-399f, searchArtworkPageOffset(-399, 800f, 0), 0f)
        assertEquals(0f, searchArtworkPageOffset(null, 800f, 0), 0f)
    }

    @Test
    fun blockCursorAlwaysFollowsFinalGlyphEvenAtTheViewportEdge() {
        assertEquals(101f, searchCursorLeft(100f, 0, 1f), 0f)
        assertEquals(81f, searchCursorLeft(400f, 320, 1f), 0f)
        assertEquals(1f, searchCursorLeft(10f, 20, 1f), 0f)
    }

    @Test
    fun keyboardArrivingLateCannotCoverTheFieldDuringAnyZoomFrame() {
        for (keyboard in listOf(0, 80, 240, 320, 480, 580)) {
            for (scale in listOf(0.965f, 1f, 1.05f)) {
                for (proposed in listOf(120f, 374f, 436f, 600f)) {
                    val top = searchSceneKeyboardSafeTop(proposed, true, 800f, 800f, keyboard, 52f, scale, 24f)
                    val bottom = top + 52f + 52f * (scale - 1f).coerceAtLeast(0f) * 0.5f
                    assertTrue("IME=$keyboard scale=$scale top=$top", bottom + 24f <= 800f - keyboard)
                }
            }
        }
    }

    @Test
    fun resizedSceneAndSuggestionRowKeepTheSameKeyboardClearance() {
        for (keyboard in listOf(320, 380, 440)) {
            val top = searchSceneKeyboardSafeTop(436f, true, 800f, 410f, keyboard, 52f, 1.05f, 24f)
            val bottom = top + 52f + 1.3f
            assertTrue(bottom + 24f <= minOf(410f, 800f - keyboard) + 0.001f)
        }
    }

    @Test
    fun keyboardSafetyDoesNotMoveAnAlreadySafeBarOrRestingResultTitle() {
        assertEquals(120f, searchSceneKeyboardSafeTop(120f, true, 800f, 800f, 320, 52f, 1.05f, 24f), 0f)
        for (keyboard in listOf(580, 440, 320, 0)) {
            assertEquals(374f, searchSceneKeyboardSafeTop(374f, false, 800f, 410f, keyboard, 52f, 1f, 24f), 0f)
        }
    }

    @Test
    fun recapShowsAtMostSixRowsAndAdaptsToSpaceAboveKeyboard() {
        assertEquals(6, searchRecallVisibleRows(600f, 52f, 30))
        assertEquals(3, searchRecallVisibleRows(160f, 52f, 30))
        assertEquals(2, searchRecallVisibleRows(600f, 52f, 2))
        assertEquals(0, searchRecallVisibleRows(40f, 52f, 30))
        assertEquals(0, searchRecallVisibleRows(600f, 52f, 0))
    }

    private fun placement(
        editing: Boolean = false,
        result: Boolean = true,
        keyboard: Int = 0,
        fallback: Boolean = false,
        initialTop: Float = 120f,
        height: Float = 800f,
        resultHeight: Float = 52f,
    ) = searchScenePlacement(height, initialTop, editing, result, keyboard, fallback, 52f, resultHeight, 24f)

    @Test
    fun submittedTitleStaysAtScreenCentreDuringKeyboardDismissal() {
        for (keyboard in listOf(320, 240, 160, 80, 0)) {
            assertEquals(400f, placement(keyboard = keyboard).fieldTop + 26f, 0f)
        }
    }

    @Test
    fun scrollingDoesNotRetargetResultTransition() {
        assertEquals(placement(initialTop = 120f), placement(initialTop = -800f))
    }

    @Test
    fun restingBarAndTitleFollowPageImmediatelyWithoutAnimationLag() {
        for (offset in listOf(0f, -1f, -80f, -300f, -80f, 0f)) {
            assertEquals(120f + offset, searchSceneForegroundTop(120f, offset, 0f, 0f), 0f)
            assertEquals(374f + offset, searchSceneForegroundTop(374f, offset, 0f, 0f), 0f)
        }
    }

    @Test
    fun pullDownMovesTheWholeRestingPanelButNeverTheDetachedEditor() {
        assertEquals(454f, searchSceneForegroundTop(374f, 0f, 80f, 0f), 0f)
        assertEquals(404f, searchSceneForegroundTop(404f, -120f, 80f, 1f), 0f)
    }

    @Test
    fun liftingGraduallyDetachesFromTheOriginalPagePosition() {
        assertEquals(180f, searchSceneForegroundTop(300f, -120f, 0f, 0f), 0f)
        assertEquals(240f, searchSceneForegroundTop(300f, -120f, 0f, 0.5f), 0f)
        assertEquals(300f, searchSceneForegroundTop(300f, -120f, 0f, 1f), 0f)
    }

    @Test
    fun backgroundSeparatesOnFirstEditFrameAndClearsAfterDismissal() {
        assertEquals(6f, searchSceneBlurRadius(true, 0f), 0f)
        assertEquals(28f, searchSceneBlurRadius(true, 1f), 0f)
        assertEquals(0f, searchSceneBlurRadius(false, 0f), 0f)
        var previous = 0f
        for (frame in 0..20) {
            val radius = searchSceneBlurRadius(true, frame / 20f)
            assertTrue(radius >= previous)
            previous = radius
        }
    }

    @Test
    fun editorWaitsForKeyboardDestinationWithoutMovingToAFallbackFirst() {
        assertEquals(120f, placement(editing = true, result = false).fieldTop, 0f)
        assertEquals(374f, placement(editing = true).fieldTop, 0f)
        assertEquals(404f, placement(editing = true, keyboard = 320).fieldTop, 0f)
    }

    @Test
    fun editorLeavesAGapAboveKeyboardAndResultsStartBelowTitle() {
        val position = placement(editing = true, keyboard = 320)
        assertEquals(480f, position.fieldTop + 52f + 24f, 0f)
        assertTrue(position.resultsTop > position.resultTop + 52f)
    }

    @Test
    fun emptySubmissionRestoresLandingPositionEvenWhileKeyboardCloses() {
        assertEquals(120f, placement(result = false, keyboard = 320).fieldTop, 0f)
    }

    @Test
    fun hardwareKeyboardFallbackAndLargeTitlesStayWithinViewport() {
        assertEquals(436f, placement(editing = true, fallback = true).fieldTop, 0f)
        assertEquals(400f, placement(resultHeight = 82f).fieldTop + 41f, 0f)
        assertEquals(0f, placement(editing = true, keyboard = 320, height = 340f).fieldTop, 0f)
    }
}
