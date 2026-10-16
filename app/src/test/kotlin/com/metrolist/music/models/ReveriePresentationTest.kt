package com.metrolist.music.models

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReveriePresentationTest {
    @Test fun panelLeavesATopGapAndStaysInsideTheMeasuredViewport() {
        for (height in listOf(200f, 320f, 480f, 640f, 852f, 1024f)) {
            val panel = reveriePanelHeight(height)
            assertTrue(panel <= height * .89f + .001f)
            assertTrue(height - panel >= 48f - .001f)
            assertTrue(panel > 0)
        }
        assertEquals(0f, reveriePanelHeight(-1f), 0f)
        assertEquals(0f, reveriePanelHeight(0f), 0f)
    }

    @Test fun rapidManualSelectionChangesChapterAndTimerRevisionSynchronously() {
        var cursor = ReverieCursor()
        listOf(1, 2, 7, 3).forEachIndexed { index, target ->
            cursor = seekReverieChapter(cursor, target, 9)
            assertEquals(target, cursor.chapter)
            assertEquals(index + 1, cursor.revision)
            assertTrue(cursor.instant)
        }
    }

    @Test fun selectingTheSameChapterRestartsItWithoutWaitingForAudio() {
        val cursor = ReverieCursor(3, 8)
        assertEquals(ReverieCursor(3, 9, true), seekReverieChapter(cursor, 3, 9))
    }

    @Test fun invalidNavigationCannotLeaveTheChapterRange() {
        val cursor = ReverieCursor(0, 2)
        assertEquals(cursor, seekReverieChapter(cursor, -1, 9))
        assertEquals(cursor, seekReverieChapter(cursor, 9, 9))
        assertEquals(cursor, seekReverieChapter(cursor, 0, 0))
    }

    @Test fun swipesAdvanceLeftAndReturnRightWithoutTurningTapsIntoNavigation() {
        assertEquals(1, reverieSwipeStep(-90f, -40f, 360f))
        assertEquals(-1, reverieSwipeStep(90f, 40f, 360f))
        assertEquals(1, reverieSwipeStep(-20f, -900f, 360f))
        assertEquals(-1, reverieSwipeStep(20f, 900f, 360f))
        assertEquals(0, reverieSwipeStep(-20f, -100f, 360f))
        assertEquals(0, reverieSwipeStep(0f, 0f, 360f))
    }

    @Test fun sheetPullAndChapterSwipeChooseDifferentAxesAfterTouchSlop() {
        assertEquals(ReverieGestureAxis.UNDECIDED, reverieGestureAxis(5f, -4f, 8f))
        assertEquals(ReverieGestureAxis.HORIZONTAL, reverieGestureAxis(-60f, 12f, 8f))
        assertEquals(ReverieGestureAxis.HORIZONTAL, reverieGestureAxis(60f, -12f, 8f))
        assertEquals(ReverieGestureAxis.VERTICAL, reverieGestureAxis(12f, 60f, 8f))
        assertEquals(ReverieGestureAxis.VERTICAL, reverieGestureAxis(-12f, -60f, 8f))
    }
}
