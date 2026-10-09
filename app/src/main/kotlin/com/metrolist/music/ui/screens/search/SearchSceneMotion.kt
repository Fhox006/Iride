package com.metrolist.music.ui.screens.search

import kotlin.math.pow
import kotlin.math.abs

internal const val SearchMotionStiffness = 650f

internal data class SearchSceneFrame(val centerY: Float, val progress: Float)

/** Last rendered geometry is sampled by the next user event, without invalidating composition. */
internal class SearchSceneSnapshot(var pose: SearchScenePose)

/** Arm only after actual visibility; an unopened or delayed IME must not dismiss the editor. */
internal class SearchKeyboardSession {
    private var becameVisible = false

    fun dismissesEditor(visibleBottom: Int, targetBottom: Int): Boolean {
        if (visibleBottom > 0 && targetBottom > 0) becameVisible = true
        // A predictive IME gesture can reverse its target before it commits. Waiting for actual
        // closure preserves the draft when that gesture is cancelled or the IME resizes itself.
        return becameVisible && visibleBottom == 0 && targetBottom == 0
    }
}

/** Never hand the field back to scrolling while the IME or a redirected spring is still moving. */
internal fun searchSceneCanAttach(
    progress: Float,
    running: Boolean,
    keyboardBottom: Int,
    center: Float,
    restingCenter: Float,
): Boolean = progress >= 1f && !running && keyboardBottom == 0 && abs(center - restingCenter) < 1f

internal data class SearchScenePose(
    val centerY: Float,
    val lift: Float,
    val result: Float,
    val funnel: Float,
    val scale: Float = 1f,
)

/** A single smooth trajectory, with one press envelope and no overshoot or settling bounce. */
internal fun searchScenePose(
    from: SearchScenePose,
    to: SearchScenePose,
    progress: Float,
    opening: Boolean,
    confirming: Boolean,
): SearchScenePose {
    val travel = searchSceneTravel(progress)
    fun blend(start: Float, end: Float) = start + (end - start) * travel
    val pressure = if (opening || confirming) searchScenePressure(progress) else 0f
    // Repeated presses during an interrupted return must not accumulate scale reductions.
    val pressedScale = minOf(from.scale, to.scale, 0.95f)
    val baseScale = blend(from.scale, to.scale)
    return SearchScenePose(
        blend(from.centerY, to.centerY), blend(from.lift, to.lift),
        blend(from.result, to.result), blend(from.funnel, to.funnel),
        baseScale + (pressedScale - baseScale) * pressure,
    )
}

/** Pressure rises quickly under the finger and releases as the shared path reaches its endpoint. */
internal fun searchScenePressure(progress: Float): Float {
    val p = progress.coerceIn(0f, 1f)
    val press = 1f - (1f - p).pow(14)
    val arrival = p.pow(8)
    return press * (1f - arrival)
}

internal fun searchSceneTravel(progress: Float): Float {
    // The clock already has physical acceleration and damping. A second easing here would
    // brake the ending twice and make the field appear to follow a programmed track.
    return progress.coerceIn(0f, 1f)
}

internal fun searchSceneArrowAlpha(lift: Float): Float {
    val p = ((lift - 0.12f) / 0.7f).coerceIn(0f, 1f)
    return p * p * (3f - 2f * p)
}
