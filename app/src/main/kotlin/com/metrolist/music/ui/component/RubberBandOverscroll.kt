/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.component

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.Velocity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.math.abs
import kotlin.math.sign

private const val RUBBER_BAND_CONSTANT = 0.2f

/**
 * Upper bound for how long the nav gate may stay latched after the last overscroll event. The
 * settle spring is slower than this in the worst case only when the user keeps the finger down,
 * in which case new events keep renewing the watchdog anyway.
 */
private const val NAV_GATE_WATCHDOG_MILLIS = 2_500L

class RubberBandPull internal constructor(
    internal val raw: Animatable<Float, AnimationVector1D>,
) {
    internal var dimension: Float = 0f

    val offset: Float
        get() {
            val r = raw.value
            val d = dimension
            if (r == 0f || d <= 0f) return 0f
            val ar = abs(r)
            return sign(r) * (ar * d * RUBBER_BAND_CONSTANT) / (d + RUBBER_BAND_CONSTANT * ar)
        }
}

@Composable
fun rememberRubberBandPull(): RubberBandPull = remember { RubberBandPull(Animatable(0f)) }

object RubberBandNavGate {
    private var activeCount by mutableIntStateOf(0)
    val isActive: Boolean get() = activeCount > 0

    internal fun enter() { activeCount++ }
    internal fun exit() { if (activeCount > 0) activeCount-- }
}

@Composable
fun Modifier.rubberBandOverscroll(
    orientation: Orientation,
    state: ScrollableState? = null,
    pull: RubberBandPull = rememberRubberBandPull(),
): Modifier {
    val scope = rememberCoroutineScope()
    val connection = remember(pull, orientation, state, scope) {
        RubberBandConnection(pull.raw, scope, orientation, state)
    }
    DisposableEffect(connection) {
        onDispose { connection.releaseGate() }
    }
    return this
        .nestedScroll(connection)
        .graphicsLayer {
            pull.dimension = if (orientation == Orientation.Horizontal) size.width else size.height
            val offset = pull.offset
            if (orientation == Orientation.Horizontal) translationX = offset else translationY = offset
        }
}

private class RubberBandConnection(
    private val pull: Animatable<Float, AnimationVector1D>,
    private val scope: CoroutineScope,
    private val orientation: Orientation,
    private val state: ScrollableState?,
) : NestedScrollConnection {
    private val settle = spring<Float>(dampingRatio = 1f, stiffness = Spring.StiffnessLow)

    private var isPulling = false
    private var watchdogJob: Job? = null

    /**
     * Enters/exits the nav gate. The watchdog armed on entry is the safety net for gestures that
     * end without [onPreFling]/[onPostFling] running (cancelled pointer events, parent
     * interception, composition disposal mid-drag): without it the gate stays latched and every
     * top-bar click is silently swallowed until another overscroll cycle happens to release it.
     */
    private fun setPulling(active: Boolean) {
        if (orientation != Orientation.Vertical || active == isPulling) return
        isPulling = active
        if (active) {
            RubberBandNavGate.enter()
            armWatchdog()
        } else {
            disarmWatchdog()
            RubberBandNavGate.exit()
        }
    }

    private fun armWatchdog() {
        disarmWatchdog()
        watchdogJob = scope.launch {
            withTimeoutOrNull(NAV_GATE_WATCHDOG_MILLIS) {
                snapshotFlow { pull.value }.first { it == 0f }
            }
            setPulling(false)
        }
    }

    private fun disarmWatchdog() {
        watchdogJob?.cancel()
        watchdogJob = null
    }

    fun releaseGate() = setPulling(false)

    private val Offset.axis: Float
        get() = if (orientation == Orientation.Horizontal) x else y

    private val Velocity.axis: Float
        get() = if (orientation == Orientation.Horizontal) x else y

    private fun Float.toOffset(): Offset =
        if (orientation == Orientation.Horizontal) Offset(this, 0f) else Offset(0f, this)

    override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
        val cur = pull.value
        val delta = available.axis
        if (cur == 0f || delta == 0f || sign(delta) == sign(cur)) return Offset.Zero
        val next = if (cur > 0f) (cur + delta).coerceAtLeast(0f) else (cur + delta).coerceAtMost(0f)
        setPulling(next != 0f)
        scope.launch { pull.snapTo(next) }
        return (next - cur).toOffset()
    }

    override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
        val delta = available.axis
        if (delta == 0f || source != NestedScrollSource.UserInput) return Offset.Zero
        if (state != null && !state.canScrollForward && !state.canScrollBackward) return Offset.Zero
        val next = pull.value + delta
        setPulling(next != 0f)
        scope.launch { pull.snapTo(next) }
        return delta.toOffset()
    }

    override suspend fun onPreFling(available: Velocity): Velocity {
        if (pull.value == 0f) return Velocity.Zero
        try {
            pull.animateTo(0f, settle)
        } finally {
            setPulling(pull.value != 0f)
        }
        return available
    }

    override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
        val v = available.axis
        if (v == 0f) return Velocity.Zero
        if (state != null && !state.canScrollForward && !state.canScrollBackward) return Velocity.Zero
        try {
            pull.animateTo(0f, settle, initialVelocity = v)
        } finally {
            setPulling(pull.value != 0f)
        }
        return available
    }
}
