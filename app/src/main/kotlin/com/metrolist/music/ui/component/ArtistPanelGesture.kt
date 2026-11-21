package com.metrolist.music.ui.component

import android.os.SystemClock
import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Rect
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.exp

internal class IridePanelGestureMotion {
    var backActive by mutableStateOf(false)
    var backProgress by mutableFloatStateOf(0f)
    var amount by mutableFloatStateOf(0f)
    var horizontal by mutableFloatStateOf(0f)
    var verticalTravel by mutableFloatStateOf(0f)
    val recovery = Animatable(1f)

    val shrink: Float get() = (amount * recovery.value).coerceIn(0f, 0.35f)

    fun frame(frame: Rect): Rect {
        val residual = recovery.value.coerceIn(0f, 1f)
        return IrideExpansionMotion.gestureFrame(
            frame, shrink, horizontal * residual, verticalTravel * residual,
        )
    }
}

/** Gesture ownership is separate from rendering; panel and artwork use the same geometry. */
@Composable
internal fun rememberIridePanelGesture(
    enabled: Boolean,
    onDismiss: () -> Boolean,
): IridePanelGestureMotion {
    val motion = remember { IridePanelGestureMotion() }
    val recovery = motion.recovery
    val scope = rememberCoroutineScope()
    var recoveryJob by remember { mutableStateOf<Job?>(null) }
    val navigation = LocalIrideExpansionBackdrop.current
    DisposableEffect(navigation) {
        onDispose { navigation?.endGesturePreview(motion) }
    }
    val lightweightMode by rememberLightweightFolioMode()
    PredictiveBackHandler(enabled = enabled && !lightweightMode) { events ->
        recoveryJob?.cancel()
        var lastSampleTime = 0L
        var amountVelocity = 0f
        try {
            recovery.stop()
            val startAmount = motion.amount * recovery.value
            val startHorizontal = motion.horizontal * recovery.value
            val startVertical = motion.verticalTravel * recovery.value
            val startBackProgress = motion.backProgress * recovery.value
            recovery.snapTo(1f)
            motion.amount = startAmount
            motion.horizontal = startHorizontal
            motion.verticalTravel = startVertical
            motion.backProgress = startBackProgress
            motion.backActive = true
            var firstY = 0f
            var started = false
            navigation?.previewGesture = motion
            events.collect { event ->
                if (!started) {
                    firstY = event.touchY
                    started = true
                }
                val direction = if (event.swipeEdge == 0) 1f else -1f
                val fraction = event.progress.coerceIn(0f, 1f)
                motion.backProgress = startBackProgress + (1f - startBackProgress) * fraction
                val drag = (1f - exp(-3f * fraction)) * 0.35f
                val nextAmount = startAmount * (1f - fraction) + drag
                val now = SystemClock.uptimeMillis()
                if (lastSampleTime > 0L && now > lastSampleTime) {
                    amountVelocity = (nextAmount - motion.amount) * 1000f / (now - lastSampleTime)
                }
                lastSampleTime = now
                motion.amount = nextAmount
                motion.horizontal = startHorizontal * (1f - fraction) + direction * drag
                motion.verticalTravel = startVertical + (event.touchY - firstY) * 0.45f
            }
            // Navigation interpolates from this exact dragged frame to the source tile.
            // Keep the preview visible until navigation has installed the closing flight.
            navigation?.returnBlur = 1f
            if (!onDismiss()) {
                navigation?.returnBlur = 0f
                recovery.animateTo(
                    0f,
                    spring(dampingRatio = 1f, stiffness = 300f),
                    initialVelocity = if (motion.amount > 0f && SystemClock.uptimeMillis() - lastSampleTime < 100L) {
                        amountVelocity / motion.amount
                    } else 0f,
                )
                navigation?.endGesturePreview(motion)
                motion.backActive = false
                motion.backProgress = 0f
            }
        } catch (cancelled: CancellationException) {
            recoveryJob = scope.launch {
                recovery.animateTo(
                    0f,
                    spring(dampingRatio = 1f, stiffness = 300f),
                    initialVelocity = if (motion.amount > 0f && SystemClock.uptimeMillis() - lastSampleTime < 100L) {
                        amountVelocity / motion.amount
                    } else 0f,
                )
                navigation?.endGesturePreview(motion)
                motion.backActive = false
                motion.backProgress = 0f
            }
            throw cancelled
        }
    }
    return motion
}
