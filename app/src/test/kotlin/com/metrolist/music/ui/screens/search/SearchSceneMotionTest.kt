package com.metrolist.music.ui.screens.search

import androidx.compose.animation.core.FloatSpringSpec
import kotlin.math.abs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchSceneMotionTest {
    private val landing = SearchScenePose(146f, 0f, 0f, 0f)
    private val editor = SearchScenePose(430f, 1f, 0f, 1f)
    private val result = SearchScenePose(400f, 0f, 1f, 1f)
    private val clock = FloatSpringSpec(dampingRatio = 1f, stiffness = SearchMotionStiffness, visibilityThreshold = 0.002f)
    private val durationMillis = (clock.getDurationNanos(0f, 1f, 0f) / 1_000_000L).toInt()
    private fun progressAt(millis: Int): Float = if (millis >= durationMillis) 1f
        else clock.getValueFromNanos(millis * 1_000_000L, 0f, 1f, 0f)

    @Test
    fun firstKeyboardOpeningCannotBeMistakenForBack() {
        val session = SearchKeyboardSession()
        assertFalse(session.dismissesEditor(0, 0))
        assertFalse(session.dismissesEditor(0, 400))
        assertFalse(session.dismissesEditor(100, 400))
        assertFalse(session.dismissesEditor(400, 400))
        assertFalse(session.dismissesEditor(400, 0))
        assertTrue(session.dismissesEditor(0, 0))
    }

    @Test
    fun delayedOrCancelledKeyboardOpenDoesNotCancelSearchMotion() {
        val session = SearchKeyboardSession()
        assertFalse(session.dismissesEditor(0, 400))
        assertFalse(session.dismissesEditor(0, 0))
        assertFalse(session.dismissesEditor(0, 400))
        assertFalse(session.dismissesEditor(400, 400))
        assertTrue(session.dismissesEditor(0, 0))
    }

    @Test
    fun transientTargetResetDuringOpeningDoesNotDismissTheDraft() {
        val session = SearchKeyboardSession()
        assertFalse(session.dismissesEditor(0, 400))
        assertFalse(session.dismissesEditor(100, 400))
        assertFalse(session.dismissesEditor(100, 0))
        assertFalse(session.dismissesEditor(220, 400))
        assertFalse(session.dismissesEditor(400, 400))
        assertFalse(session.dismissesEditor(400, 0))
        assertTrue(session.dismissesEditor(0, 0))
    }

    @Test
    fun cancelledPredictiveImeBackKeepsTheEditorOpen() {
        val session = SearchKeyboardSession()
        assertFalse(session.dismissesEditor(400, 400))
        assertFalse(session.dismissesEditor(400, 0))
        assertFalse(session.dismissesEditor(180, 0))
        assertFalse(session.dismissesEditor(250, 400))
        assertFalse(session.dismissesEditor(400, 400))
    }

    @Test
    fun backBeforeKeyboardFullyOpensDismissesWhenItCloses() {
        val session = SearchKeyboardSession()
        assertFalse(session.dismissesEditor(100, 400))
        assertFalse(session.dismissesEditor(80, 0))
        assertTrue(session.dismissesEditor(0, 0))
    }

    @Test
    fun attachmentRequiresGeometryAndKeyboardToFinishNotOnlyVisualProgress() {
        assertFalse(searchSceneCanAttach(1f, false, 400, 350f, 400f))
        assertFalse(searchSceneCanAttach(1f, true, 0, 399.9f, 400f))
        assertFalse(searchSceneCanAttach(1f, false, 0, 350f, 400f))
        assertFalse(searchSceneCanAttach(0.99f, false, 0, 400f, 400f))
        assertTrue(searchSceneCanAttach(1f, false, 0, 400f, 400f))
    }

    @Test
    fun everyInterruptedOpeningSubmissionAndReturnStartsAtTheVisiblePose() {
        for (initial in listOf(landing, result)) {
            for (interruption in listOf(0f, 0.05f, 0.25f, 0.65f, 0.99f, 1f)) {
                val visible = searchScenePose(initial, editor, interruption, true, false)
                for (destination in listOf(landing, result, editor)) {
                    val opens = destination == editor
                    for (confirms in listOf(false, true)) {
                        assertEquals(visible, searchScenePose(visible, destination, 0f, opens, confirms))
                        assertEquals(destination, searchScenePose(visible, destination, 1f, opens, confirms))
                    }
                }
            }
        }
    }

    @Test
    fun touchIsVisibleInTheFirstFrameWithoutAnInstantScaleJump() {
        assertEquals(landing, searchScenePose(landing, editor, 0f, true, false))
        val firstFrame = searchScenePose(landing, editor, progressAt(16), true, false)
        assertTrue(firstFrame.scale < 0.99f)
        assertTrue(firstFrame.centerY > landing.centerY)
    }

    @Test
    fun restoredPressStaysCollectedDuringTheLift() {
        for (millis in listOf(32, 64)) {
            val pose = searchScenePose(landing, editor, progressAt(millis), true, false)
            assertTrue("Press released too early at $millis ms", pose.scale < 0.96f)
            assertTrue(pose.scale >= 0.95f)
        }
    }

    @Test
    fun arrowFadesContinuouslyBothInAndOutWithTheEditor() {
        assertEquals(0f, searchSceneArrowAlpha(0f), 0f)
        assertEquals(1f, searchSceneArrowAlpha(1f), 0f)
        var previous = 0f
        for (frame in 0..100) {
            val alpha = searchSceneArrowAlpha(frame / 100f)
            assertTrue(alpha >= previous)
            previous = alpha
        }
        assertTrue(searchSceneArrowAlpha(0.5f) in 0.1f..0.9f)
    }

    @Test
    fun lateKeyboardTargetKeepsPositionAndVelocityEvenAfterTheInitialSpringFinished() {
        for (arrivalMillis in listOf(16, 140, durationMillis + 100)) {
            val time = arrivalMillis * 1_000_000L
            val position = if (arrivalMillis >= durationMillis) 430f else clock.getValueFromNanos(time, 146f, 430f, 0f)
            val velocity = if (arrivalMillis >= durationMillis) 0f else clock.getVelocityFromNanos(time, 146f, 430f, 0f)
            assertEquals(position, clock.getValueFromNanos(0L, position, 350f, velocity), 0.0001f)
            assertEquals(velocity, clock.getVelocityFromNanos(0L, position, 350f, velocity), 0.0001f)
            val duration = clock.getDurationNanos(position, 350f, velocity)
            assertEquals(350f, clock.getValueFromNanos(duration, position, 350f, velocity), 0.05f)
        }
    }

    @Test
    fun pressHasOneMinimumThenExpandsToFinalSizeWithoutOvershoot() {
        val scales = (0..1000).map { searchScenePose(landing, editor, it / 1000f, true, false).scale }
        val minimum = scales.indices.minBy { scales[it] }
        assertTrue(scales[minimum] < 0.975f)
        assertEquals(1f, scales.first(), 0f)
        assertEquals(1f, scales.last(), 0f)
        for (index in 1..minimum) assertTrue(scales[index] <= scales[index - 1] + 0.000001f)
        for (index in minimum + 1..scales.lastIndex) assertTrue(scales[index] >= scales[index - 1] - 0.000001f)
        assertTrue(scales.all { it <= 1f })
    }

    @Test
    fun staysCollectedDuringTravelAndExpandsOnArrival() {
        val halfway = searchScenePose(landing, editor, 0.5f, true, false)
        val arriving = searchScenePose(landing, editor, progressAt(220), true, false)
        assertTrue(halfway.scale < 0.975f)
        assertTrue(arriving.scale > halfway.scale)
        assertTrue(arriving.centerY > editor.centerY - 10f)
    }

    @Test
    fun centreNeverChangesDirectionForAnyOpeningOrReturn() {
        for ((from, to) in listOf(landing to editor, result to editor, editor to landing, editor to result)) {
            var previous = from.centerY
            for (frame in 1..1000) {
                val pose = searchScenePose(from, to, frame / 1000f, to == editor, false)
                assertTrue((pose.centerY - previous) * (to.centerY - from.centerY) >= -0.0001f)
                previous = pose.centerY
            }
            assertEquals(to.centerY, previous, 0.0001f)
        }
    }

    @Test
    fun titleFrostAndPositionAlwaysUseTheSameTravel() {
        for (frame in 0..100) {
            val pose = searchScenePose(landing, editor, frame / 100f, true, false)
            assertEquals(pose.lift, (pose.centerY - landing.centerY) / (editor.centerY - landing.centerY), 0.0001f)
            assertEquals(pose.lift, pose.funnel, 0f)
        }
    }

    @Test
    fun emptyReturnStartsAtLastRenderedPoseWithoutDestinationFlash() {
        assertEquals(editor, searchScenePose(editor, landing, 0f, false, false))
        val first = searchScenePose(editor, landing, progressAt(1), false, false)
        assertTrue(first.centerY > 429f)
        assertTrue(first.funnel > 0.999f)
        assertEquals(landing, searchScenePose(editor, landing, 1f, false, false))
    }

    @Test
    fun interruptedLiftReturnsWithoutReplayingThePress() {
        val interrupted = searchScenePose(result, editor, 0.45f, true, false)
        assertEquals(interrupted, searchScenePose(interrupted, landing, 0f, false, false))
        var previous = interrupted.scale
        for (frame in 1..100) {
            val pose = searchScenePose(interrupted, landing, frame / 100f, false, false)
            assertTrue(abs(pose.scale - 1f) <= abs(previous - 1f) + 0.0001f)
            previous = pose.scale
        }
    }

    @Test
    fun manualSubmissionHasFeedbackEvenForAnAlreadySubmittedQuery() {
        val pressed = searchScenePose(result, result, 0.2f, false, true)
        assertTrue(pressed.scale < 0.98f)
        assertEquals(result.centerY, pressed.centerY, 0f)
        assertEquals(result, searchScenePose(result, result, 1f, false, true))
    }

    @Test
    fun resultReturnArrivesWithTitleAndFieldTogether() {
        for (frame in 0..100) {
            val pose = searchScenePose(editor, result, frame / 100f, false, true)
            assertEquals(1f, pose.lift + pose.result, 0.0001f)
        }
        assertEquals(result, searchScenePose(editor, result, 1f, false, true))
    }

    @Test
    fun changingHeightAndVisibleKeyboardDoNotCreateASecondVerticalMovement() {
        val from = SearchScenePose(400f, 0f, 1f, 1f)
        val to = SearchScenePose(350f, 1f, 1f, 1f)
        var previous = from.centerY
        for (millis in 0..durationMillis) {
            val progress = progressAt(millis)
            val pose = searchScenePose(from, to, progress, true, false)
            val height = 52f + 48f * (1f - pose.lift)
            val imeTime = ((millis - 16f) / 220f).coerceIn(0f, 1f)
            val keyboard = (400f * (1f - (1f - imeTime) * (1f - imeTime) * (1f - imeTime))).toInt()
            val top = searchSceneKeyboardSafeTop(
                pose.centerY - height * 0.5f, true, 800f, 800f - keyboard,
                keyboard, height, pose.scale, 24f * pose.lift * pose.lift * pose.lift * pose.lift,
            )
            val centre = top + height * 0.5f
            assertTrue("Vertical reversal at $millis ms", centre <= previous + 0.0001f)
            assertTrue(top + height <= 800f - keyboard + 0.0001f)
            previous = centre
        }
        assertEquals(to.centerY, previous, 0.0001f)
    }

    @Test
    fun returnAndSubmitStayAboveImeUntilItsFinalFrameAcrossViewportSizes() {
        for (viewport in listOf(480f, 800f, 1200f)) {
            for (keyboardFraction in listOf(0.25f, 0.5f, 0.7f)) {
                val keyboardHeight = viewport * keyboardFraction
                val from = SearchScenePose(viewport - keyboardHeight - 50f, 1f, 1f, 1f)
                for (destination in listOf(SearchScenePose(120f, 0f, 0f, 0f), SearchScenePose(viewport / 2f, 0f, 1f, 1f))) {
                    for (imeDuration in listOf(120, 260, 700)) {
                        for (millis in 0..maxOf(durationMillis, imeDuration) step 8) {
                            val keyboard = (keyboardHeight * (1f - millis.toFloat() / imeDuration).coerceIn(0f, 1f)).toInt()
                            val pose = searchScenePose(from, destination, progressAt(millis), false, destination.result > 0f)
                            val fieldHeight = 52f + 48f * pose.result * (1f - pose.lift)
                            for (sceneHeight in listOf(viewport, viewport - keyboard)) {
                                val top = searchSceneKeyboardSafeTop(
                                    pose.centerY - fieldHeight / 2f, keyboard > 0,
                                    viewport, sceneHeight, keyboard, fieldHeight, pose.scale,
                                    24f * pose.lift * pose.lift * pose.lift * pose.lift,
                                )
                                if (keyboard > 0) assertTrue(top + fieldHeight <= viewport - keyboard + 0.001f)
                            }
                        }
                    }
                }
            }
        }
    }
}
