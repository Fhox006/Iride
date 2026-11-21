package com.metrolist.music.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp as lerpColor
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.util.lerp
import com.metrolist.music.ui.theme.drawFolioRoundRect
import com.metrolist.music.ui.utils.IrideMotion
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay

internal val LocalOnboardingLaunchReveal = staticCompositionLocalOf<OnboardingLaunchReveal?> { null }

/** Lives above the navigation host so the expanded button can uncover the real Home. */
internal class OnboardingLaunchReveal {
    var origin by mutableStateOf<Rect?>(null)
        private set
    val expansion = Animatable(0f)
    val opacity = Animatable(1f)

    suspend fun expandFrom(button: Rect) {
        if (button.isEmpty) return
        expansion.snapTo(0f)
        opacity.snapTo(1f)
        origin = button
        try {
            expansion.animateTo(1f, tween(460, easing = IrideMotion.EaseOutQuart))
        } catch (cancelled: CancellationException) {
            origin = null
            throw cancelled
        }
    }

    suspend fun uncover() {
        delay(120)
        opacity.animateTo(0f, tween(430, easing = IrideMotion.EaseOutQuart))
        origin = null
    }
}

@Composable
internal fun OnboardingLaunchOverlay(reveal: OnboardingLaunchReveal, currentRoute: String?) {
    val button = reveal.origin ?: return
    var windowPosition by remember { mutableStateOf(Offset.Zero) }
    LaunchedEffect(reveal, currentRoute) {
        if (currentRoute != null && currentRoute != "onboarding") reveal.uncover()
    }
    BackHandler {}
    Canvas(
        Modifier.fillMaxSize()
            .onGloballyPositioned { windowPosition = it.positionInWindow() }
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) awaitPointerEvent(PointerEventPass.Initial).changes.forEach { it.consume() }
                }
            },
    ) {
        val progress = reveal.expansion.value
        val left = lerp(button.left - windowPosition.x, 0f, progress)
        val top = lerp(button.top - windowPosition.y, 0f, progress)
        val right = lerp(button.right - windowPosition.x, size.width, progress)
        val bottom = lerp(button.bottom - windowPosition.y, size.height, progress)
        drawFolioRoundRect(
            color = lerpColor(Color.White, Color.Black, progress),
            topLeft = Offset(left, top),
            size = Size(right - left, bottom - top),
            cornerRadius = CornerRadius(button.height * 0.5f * (1f - progress)),
            alpha = reveal.opacity.value,
        )
    }
}
