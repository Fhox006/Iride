package com.metrolist.music.ui.screens

import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.metrolist.music.R
import com.metrolist.music.ui.theme.FolioShape
import com.metrolist.music.ui.theme.textPrimary
import com.metrolist.music.ui.utils.rememberReducedMotion
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.exp

/** A local preview of the real page; it never changes the navigation stack or its backdrop. */
@Composable
internal fun GestureWelcomePeek(content: @Composable () -> Unit) {
    val reducedMotion = rememberReducedMotion()
    val visible = LocalExperienceVisible.current
    val scope = rememberCoroutineScope()
    val recovery = remember { Animatable(1f) }
    var settling by remember { mutableStateOf<Job?>(null) }
    var amount by remember { mutableFloatStateOf(0f) }
    var verticalTravel by remember { mutableFloatStateOf(0f) }
    var direction by remember { mutableFloatStateOf(1f) }

    fun settle(hold: Boolean) {
        settling = scope.launch {
            if (hold) delay(700)
            recovery.animateTo(0f, spring(dampingRatio = 0.85f, stiffness = 450f))
        }
    }

    PredictiveBackHandler(enabled = visible) { events ->
        settling?.cancel()
        recovery.stop()
        // Retain a partially recovered preview when another swipe begins immediately.
        val initialAmount = amount * recovery.value
        recovery.snapTo(1f)
        amount = initialAmount
        verticalTravel = 0f
        var firstY: Float? = null
        try {
            events.collect { event ->
                if (firstY == null) firstY = event.touchY
                direction = if (event.swipeEdge == 0) 1f else -1f
                val pull = (1f - exp(-3f * event.progress.coerceIn(0f, 1f))) * 0.35f
                amount = initialAmount + (0.35f - initialAmount) * (pull / 0.35f)
                verticalTravel = (event.touchY - firstY) * 0.45f
            }
            // Non-predictive Android back callbacks still get a readable, contained response.
            if (amount < 0.12f) amount = 0.24f
            settle(hold = true)
        } catch (cancelled: CancellationException) {
            settle(hold = false)
            throw cancelled
        }
    }

    val outlineColor = MaterialTheme.colorScheme.outline
    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        val message: @Composable () -> Unit = {
            Text(
                text = stringResource(R.string.experience_gesture_easter_egg),
                style = MaterialTheme.typography.titleLarge.copy(fontSize = 24.sp, lineHeight = 28.sp),
                color = MaterialTheme.colorScheme.textPrimary,
                modifier = Modifier.align(Alignment.TopCenter).safeDrawingPadding()
                    .padding(top = if (reducedMotion) 0.dp else 18.dp)
                    .graphicsLayer {
                        val hint = (amount * recovery.value / 0.20f).coerceIn(0f, 1f)
                        alpha = hint
                        translationY = if (reducedMotion) 0f else (1f - hint) * 8.dp.toPx()
                    }.clearAndSetSemantics {},
            )
        }
        if (!reducedMotion) message()
        Box(
            Modifier.fillMaxSize().graphicsLayer {
                val shrink = if (reducedMotion) 0f else amount * recovery.value
                scaleX = 1f - shrink
                scaleY = 1f - shrink
                translationX = direction * size.width * shrink * 0.65f
                translationY = verticalTravel.coerceIn(-size.height * 0.18f, size.height * 0.18f) *
                    recovery.value * if (reducedMotion) 0f else 1f
                shape = FolioShape(32.dp * (shrink / 0.12f).coerceIn(0f, 1f))
                clip = true
            }.drawWithContent {
                drawContent()
                val shrink = if (reducedMotion) 0f else amount * recovery.value
                val visibility = (shrink / 0.12f).coerceIn(0f, 1f)
                if (visibility > 0f) {
                    drawOutline(
                        outline = FolioShape(32.dp * visibility).createOutline(size, layoutDirection, this),
                        color = outlineColor.copy(alpha = 0.55f * visibility),
                        style = Stroke(2.dp.toPx()),
                    )
                }
            },
        ) {
            content()
        }
        if (reducedMotion) message()
    }
}
