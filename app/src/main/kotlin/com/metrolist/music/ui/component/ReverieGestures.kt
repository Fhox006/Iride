package com.metrolist.music.ui.component

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import com.metrolist.music.models.ReverieGestureAxis
import com.metrolist.music.models.reverieGestureAxis
import com.metrolist.music.models.reverieSwipeStep
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Lock horizontal swipes here; vertical movement stays available to the native bottom sheet. */
@Composable
internal fun Modifier.reverieGestures(
    key: Any,
    onHold: (Boolean) -> Unit,
    onTap: (Float) -> Unit,
    onStep: (Int) -> Unit,
): Modifier {
    val holdChanged by rememberUpdatedState(onHold)
    val tap by rememberUpdatedState(onTap)
    val step by rememberUpdatedState(onStep)
    return pointerInput(key) {
        coroutineScope {
            val gestureScope = this
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false)
                val tracker = VelocityTracker().apply { addPosition(down.uptimeMillis, down.position) }
                var total = Offset.Zero
                var axis = ReverieGestureAxis.UNDECIDED
                var released = false
                var longPressed = false
                val hold = gestureScope.launch {
                    delay(240)
                    longPressed = true
                    holdChanged(true)
                }
                try {
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                        tracker.addPosition(change.uptimeMillis, change.position)
                        total = change.position - down.position
                        if (axis == ReverieGestureAxis.UNDECIDED) {
                            axis = reverieGestureAxis(total.x, total.y, viewConfiguration.touchSlop)
                            if (axis != ReverieGestureAxis.UNDECIDED) {
                                hold.cancel()
                                holdChanged(false)
                            }
                        }
                        if (axis == ReverieGestureAxis.HORIZONTAL) change.consume()
                        if (!change.pressed) {
                            released = true
                            break
                        }
                    }
                } finally {
                    hold.cancel()
                    holdChanged(false)
                }
                val velocity = tracker.calculateVelocity()
                when (axis) {
                    ReverieGestureAxis.HORIZONTAL -> if (released) {
                        step(reverieSwipeStep(total.x / density, velocity.x / density, size.width / density))
                    }
                    ReverieGestureAxis.VERTICAL -> Unit
                    ReverieGestureAxis.UNDECIDED -> if (released && !longPressed) {
                        tap(down.position.x / size.width.coerceAtLeast(1))
                    }
                }
            }
        }
    }
}
