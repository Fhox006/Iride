package com.metrolist.music.ui.component

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.DraggableState
import androidx.compose.runtime.MonotonicFrameClock
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BottomSheetStateTest {
    private val frameClock = TestFrameClock()
    private val scope = CoroutineScope(Job() + Dispatchers.Unconfined + frameClock)
    private val anchors = mutableListOf<Int>()

    @After
    fun tearDown() {
        scope.cancel()
    }

    @Test
    fun dragFollowsEveryDeltaImmediatelyAndCanReverse() {
        val state = createState()

        state.dispatchRawDelta(-20f)
        assertPosition(state, 120f)
        state.dispatchRawDelta(-30f)
        assertPosition(state, 150f)
        state.dispatchRawDelta(15f)
        assertPosition(state, 135f)
        assertEquals(35f / 800f, state.progress, 0.0001f)
    }

    @Test
    fun curtainStopsAtBothBoundsAndReversesWithoutOverscrollDebt() {
        val state = createState()

        state.dispatchRawDelta(500f)
        assertTrue(state.isCollapsed)
        state.dispatchRawDelta(-1000f)
        assertTrue(state.isExpanded)
        state.dispatchRawDelta(10f)
        assertPosition(state, 890f)
        state.dispatchRawDelta(1000f)
        state.dispatchRawDelta(-10f)
        assertPosition(state, 110f)
    }

    @Test
    fun dismissibleSheetsStillAllowDraggingBelowCollapsedBound() {
        val state = createState(preventDismissDrag = false)

        state.dispatchRawDelta(50f)
        assertPosition(state, 50f)
        state.dispatchRawDelta(100f)
        assertTrue(state.isDismissed)
        state.dispatchRawDelta(-10f)
        assertPosition(state, 10f)
    }

    @Test
    fun settlingStartsAtDraggedPositionAndRegrabCancelsIt() {
        val state = createState()
        state.dispatchRawDelta(-300f)

        state.collapse(tween(100, easing = LinearEasing))
        assertPosition(state, 400f)
        frameClock.sendFrame(0)
        frameClock.sendFrame(50)
        assertPosition(state, 250f)

        state.dispatchRawDelta(-20f)
        assertPosition(state, 270f)
        frameClock.sendFrame(100)
        assertPosition(state, 270f)
    }

    @Test
    fun programmaticSnapSupersedesSettlingAnimation() {
        val state = createState()
        state.dispatchRawDelta(-300f)
        state.collapse(tween(100, easing = LinearEasing))
        frameClock.sendFrame(0)

        state.snapTo(280.dp)
        assertPosition(state, 280f)
        frameClock.sendFrame(100)
        assertPosition(state, 280f)
    }

    @Test
    fun slowReleaseBelowActualMidpointChoosesCollapsedAnchor() {
        val state = createState(collapsed = 200.dp, expanded = 1000.dp)
        state.dispatchRawDelta(-350f)

        state.performFling(0f, null)

        assertEquals(listOf(collapsedAnchor), anchors)
        assertPosition(state, 550f)
        frameClock.sendFrame(0)
        frameClock.sendFrame(5000)
        assertTrue(state.isCollapsed)
    }

    @Test
    fun coincidentExpandedAndCollapsedBoundsHaveFiniteProgress() {
        val state = createState(collapsed = 100.dp, expanded = 100.dp)

        assertEquals(0f, state.progress, 0f)
        state.dispatchRawDelta(-10f)
        assertPosition(state, 100f)
    }

    @Test
    fun bottomGestureAcrossWidthUsesReducedSlop() {
        val slop = bottomSheetDragSlop(
            startYDp = 740f,
            screenHeightDp = 800f,
            sideDragSlopPx = 16f,
            densityScale = 2f,
        )

        assertEquals(112f, slop, 0f)
    }

    @Test
    fun bottomGuardSlopAppliesAcrossFullWidth() {
        val slop = bottomSheetDragSlop(
            startYDp = 720f,
            screenHeightDp = 800f,
            sideDragSlopPx = 16f,
            densityScale = 2f,
        )

        assertEquals(112f, slop, 0f)
    }

    @Test
    fun bottomCenterGestureMustExceedExtendedSlopBeforeMovingSheet() {
        assertTrue(!isVerticalDragPastSlop(accumulatedY = -111f, accumulatedX = 0f, dragSlop = 112f))
        assertTrue(!isVerticalDragPastSlop(accumulatedY = -112f, accumulatedX = 0f, dragSlop = 112f))
        assertTrue(isVerticalDragPastSlop(accumulatedY = -113f, accumulatedX = 0f, dragSlop = 112f))
        assertTrue(!isVerticalDragPastSlop(accumulatedY = -200f, accumulatedX = 201f, dragSlop = 112f))
    }

    @Test
    fun gesturesAboveBottomGuardKeepExistingSlop() {
        val bottomBandSlop = bottomSheetDragSlop(
            startYDp = 740f,
            screenHeightDp = 800f,
            sideDragSlopPx = 16f,
            densityScale = 2f,
        )
        val upperSlop = bottomSheetDragSlop(
            startYDp = 719f,
            screenHeightDp = 800f,
            sideDragSlopPx = 16f,
            densityScale = 2f,
        )

        assertEquals(112f, bottomBandSlop, 0f)
        assertEquals(16f, upperSlop, 0f)
    }

    @Test
    fun expandedPlayerIsPersistedClosedForNextAppLaunch() {
        assertEquals(collapsedAnchor, persistablePlayerAnchor(expandedAnchor))
        assertEquals(collapsedAnchor, persistablePlayerAnchor(collapsedAnchor))
    }

    private fun createState(
        collapsed: Dp = 100.dp,
        expanded: Dp = 900.dp,
        preventDismissDrag: Boolean = true,
    ): BottomSheetState {
        val animatable = Animatable(collapsed, Dp.VectorConverter)
        animatable.updateBounds(0.dp, expanded)
        lateinit var state: BottomSheetState
        state = BottomSheetState(
            draggableState = DraggableState { state.dragBy(it.dp, preventDismissDrag) },
            coroutineScope = scope,
            animatable = animatable,
            onAnchorChanged = { anchors.add(it) },
            collapsedBound = collapsed,
        )
        return state
    }

    private fun assertPosition(state: BottomSheetState, expected: Float) {
        assertEquals(expected, state.value.value, 0.001f)
    }

    private class TestFrameClock : MonotonicFrameClock {
        private val frames = Channel<Long>(Channel.UNLIMITED)

        override suspend fun <R> withFrameNanos(onFrame: (Long) -> R): R =
            onFrame(frames.receive())

        fun sendFrame(milliseconds: Long) {
            check(frames.trySend(milliseconds * 1_000_000L).isSuccess)
        }
    }

}
