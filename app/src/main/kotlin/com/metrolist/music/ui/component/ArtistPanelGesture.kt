package com.metrolist.music.ui.component

import android.os.SystemClock
import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.exp

/** Keeps the guided drag attached to the outgoing page until its closing flight ends. */
@Composable
internal fun iridePanelGestureModifier(
    progress: State<Float>,
    enabled: Boolean,
    onDismiss: () -> Boolean,
): Modifier {
    var amount by remember { mutableFloatStateOf(0f) }
    var verticalTravel by remember { mutableFloatStateOf(0f) }
    var direction by remember { mutableFloatStateOf(1f) }
    val recovery = remember { Animatable(1f) }
    val scope = rememberCoroutineScope()
    var recoveryJob by remember { mutableStateOf<Job?>(null) }
    val navigation = LocalIrideExpansionBackdrop.current
    DisposableEffect(navigation) {
        onDispose { navigation?.gesturePreview = false }
    }
    PredictiveBackHandler(enabled = enabled) { events ->
        recoveryJob?.cancel()
        recovery.stop()
        recovery.snapTo(1f)
        amount = 0f
        verticalTravel = 0f
        var firstY = 0f
        var started = false
        var lastSampleTime = 0L
        var amountVelocity = 0f
        navigation?.gesturePreview = true
        try {
            events.collect { event ->
                if (!started) {
                    firstY = event.touchY
                    started = true
                }
                direction = if (event.swipeEdge == 0) 1f else -1f
                val nextAmount = (1f - exp(-3f * event.progress.coerceIn(0f, 1f))) * 0.35f
                val now = SystemClock.uptimeMillis()
                if (lastSampleTime > 0L && now > lastSampleTime) {
                    amountVelocity = (nextAmount - amount) * 1000f / (now - lastSampleTime)
                }
                lastSampleTime = now
                amount = nextAmount
                verticalTravel = (event.touchY - firstY) * 0.45f
            }
            // Navigation owns the final flight. Its progress removes the residual drag
            // continuously, so releasing never snaps the panel back to full screen.
            // Keep the preview visible until navigation has installed the closing flight.
            navigation?.returnBlur = 1f
            if (!onDismiss()) {
                navigation?.returnBlur = 0f
                navigation?.gesturePreview = true
                recovery.animateTo(
                    0f,
                    spring(dampingRatio = 1f, stiffness = 300f),
                    initialVelocity = if (amount > 0f && SystemClock.uptimeMillis() - lastSampleTime < 100L) {
                        amountVelocity / amount
                    } else 0f,
                )
                navigation?.gesturePreview = false
            }
        } catch (cancelled: CancellationException) {
            recoveryJob = scope.launch {
                recovery.animateTo(
                    0f,
                    spring(dampingRatio = 1f, stiffness = 300f),
                    initialVelocity = if (amount > 0f && SystemClock.uptimeMillis() - lastSampleTime < 100L) {
                        amountVelocity / amount
                    } else 0f,
                )
                navigation?.gesturePreview = false
            }
            throw cancelled
        }
    }
    return Modifier.graphicsLayer {
        val residual = progress.value.coerceIn(0f, 1f) * recovery.value
        val shrink = amount * residual
        scaleX = 1f - shrink
        scaleY = 1f - shrink
        translationX = direction * size.width * shrink * 0.65f
        translationY = verticalTravel.coerceIn(-size.height * 0.18f, size.height * 0.18f) * residual
    }
}
