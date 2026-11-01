package com.metrolist.music.ui.component

import android.os.SystemClock
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector2D
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.TwoWayConverter
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

internal data class FolioContextFrame(val pressure: Float = 0f, val reveal: Float = 0f)

private val contextFrameConverter = TwoWayConverter<FolioContextFrame, AnimationVector2D>(
    convertToVector = { AnimationVector2D(it.pressure, it.reveal) },
    convertFromVector = { FolioContextFrame(it.v1, it.v2) },
)

/** One vector animation transfers the press into the source rebound and panel reveal. */
internal class IrideFolioContextMotion {
    val frame = Animatable(FolioContextFrame(), contextFrameConverter)
    var retainMenu by mutableStateOf(false)
        private set
    var panelReady by mutableStateOf(false)
        private set
    var sourceBounds: Rect? = null
    var touchPoint: Offset? = null
        private set
    var awaitingMenu = false
        private set
    private var downTime = 0L
    private var committed = false
    private var pressJob: Job? = null
    private var sourceScope: CoroutineScope? = null
    private var sourceReducedMotion = false

    fun recordDown(uptimeMillis: Long, position: Offset) {
        downTime = uptimeMillis
        touchPoint = sourceBounds?.topLeft?.plus(position)
    }

    fun press(scope: CoroutineScope, timeoutMillis: Long, reducedMotion: Boolean) {
        sourceScope = scope
        sourceReducedMotion = reducedMotion
        if (committed || reducedMotion) return
        pressJob?.cancel()
        // Scrollable clickables defer PressInteraction; finish at the real long-press deadline.
        val elapsed = if (downTime > 0L) SystemClock.uptimeMillis() - downTime else 0L
        val remaining = (timeoutMillis - elapsed).coerceIn(1L, Int.MAX_VALUE.toLong()).toInt()
        pressJob = scope.launch(start = CoroutineStart.UNDISPATCHED) {
            frame.animateTo(
                FolioContextFrame(pressure = 1f),
                tween(remaining, easing = IrideFolioContextMotionSpec.AnticipationEasing),
            )
        }
    }

    fun commit() {
        pressJob?.cancel()
        committed = true
    }

    fun reserveForMenu() {
        awaitingMenu = true
    }

    fun attachMenu() {
        commit()
        awaitingMenu = false
        panelReady = false
        retainMenu = true
    }

    fun markPanelReady() {
        panelReady = true
    }

    fun abandon() {
        retainMenu = false
        awaitingMenu = false
        sourceScope?.let { release(it, sourceReducedMotion, force = true) }
    }

    fun release(scope: CoroutineScope, reducedMotion: Boolean, force: Boolean = false) {
        // Popup focus cancels the pointer; presentation owns the animation after recognition.
        if (committed && !force) return
        committed = false
        awaitingMenu = false
        pressJob?.cancel()
        pressJob = scope.launch(start = CoroutineStart.UNDISPATCHED) {
            if (reducedMotion) frame.snapTo(FolioContextFrame())
            else frame.animateTo(FolioContextFrame(), IrideFolioContextMotionSpec.ReturnSpring)
        }
    }

    suspend fun present(visible: Boolean, reducedMotion: Boolean) {
        pressJob?.cancel()
        val target = FolioContextFrame(reveal = if (visible) 1f else 0f)
        if (reducedMotion) frame.snapTo(target)
        else frame.animateTo(
            target,
            if (visible) IrideFolioContextMotionSpec.OpenSpring else IrideFolioContextMotionSpec.CloseTween,
        )
        if (!visible) {
            committed = false
            awaitingMenu = false
            retainMenu = false
        }
    }
}

internal object IrideFolioContextMotionSpec {
    const val SourceCompression = 0.06f
    const val PanelInitialScale = 0.94f
    val PanelBlurRadius = 14.dp
    val PanelMaximumTravel = 8.dp
    val AnticipationEasing = CubicBezierEasing(0.35f, 0f, 0.65f, 1f)
    val OpenSpring = spring(
        dampingRatio = 0.84f,
        stiffness = 460f,
        visibilityThreshold = FolioContextFrame(0.001f, 0.001f),
    )
    val ReturnSpring = spring(
        dampingRatio = 1f,
        stiffness = 900f,
        visibilityThreshold = FolioContextFrame(0.001f, 0.001f),
    )
    // Defocus becomes visible before fading; the transparent endpoint has no hard cutoff.
    fun panelBlurProgress(reveal: Float): Float {
        val focus = reveal.coerceIn(0f, 1f)
        return 1f - focus * focus
    }

    fun panelOpacity(reveal: Float): Float {
        val dispersion = 1f - reveal.coerceIn(0f, 1f)
        // Keep the panel opaque until its outline and lettering have already dispersed.
        val fade = ((dispersion - 0.4f) / 0.6f).coerceIn(0f, 1f)
        return 1f - fade * fade * (3f - 2f * fade)
    }

    val CloseTween = tween<FolioContextFrame>(
        durationMillis = 300,
        easing = CubicBezierEasing(0.2f, 0f, 0.2f, 1f),
    )
}
