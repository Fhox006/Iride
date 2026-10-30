package com.metrolist.music.ui.component

import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import kotlinx.coroutines.CancellationException
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
    val navigation = LocalIrideExpansionBackdrop.current
    DisposableEffect(navigation) {
        onDispose { navigation?.gesturePreview = false }
    }
    PredictiveBackHandler(enabled = enabled) { events ->
        recovery.stop()
        recovery.snapTo(1f)
        amount = 0f
        verticalTravel = 0f
        var firstY: Float? = null
        try {
            events.collect { event ->
                navigation?.gesturePreview = true
                if (firstY == null) firstY = event.touchY
                direction = if (event.swipeEdge == 0) 1f else -1f
                amount = (1f - exp(-3f * event.progress.coerceIn(0f, 1f))) * 0.35f
                verticalTravel = (event.touchY - firstY) * 0.45f
            }
            // Navigation owns the final flight. Its progress removes the residual drag
            // continuously, so releasing never snaps the panel back to full screen.
            navigation?.gesturePreview = false
            navigation?.returnBlur = 1f
            if (!onDismiss()) {
                navigation?.returnBlur = 0f
                navigation?.gesturePreview = true
                recovery.animateTo(0f, spring(dampingRatio = 0.85f, stiffness = 450f))
                navigation?.gesturePreview = false
            }
        } catch (cancelled: CancellationException) {
            scope.launch {
                recovery.animateTo(0f, spring(dampingRatio = 0.85f, stiffness = 450f))
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
