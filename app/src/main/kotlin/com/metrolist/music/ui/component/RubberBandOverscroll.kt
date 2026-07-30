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
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.Velocity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.sign

// Apple's UIScrollView rubber-band curve: f(x) = x*d*c / (d + c*x). Lower c = less bite at
// the very first pixels of pull (easier to slip past the edge before the curve tightens).
private const val RUBBER_BAND_CONSTANT = 0.2f

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

// Shared across every vertical rubberBandOverscroll instance (Home/Library/Search/Account's own
// top-level scroll). Lets the bottom/top nav bar refuse to switch tabs while a drag-up/down pull
// is still in progress or springing back — a tab switch mid-pull used to cut the rubber-band
// animation off and yank the new screen in underneath the still-moving content.
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

    // Only vertical pulls gate tab switching — horizontal shelf swipes inside Home happen
    // constantly during normal browsing and were never meant to lock the tab bar.
    private var isPulling = false

    private fun setPulling(active: Boolean) {
        if (orientation != Orientation.Vertical || active == isPulling) return
        isPulling = active
        if (active) RubberBandNavGate.enter() else RubberBandNavGate.exit()
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

    // Fling that dies against the edge (list can't consume it) — same case UIScrollView
    // resolves by feeding leftover velocity into its bounce spring instead of a hard stop.
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
