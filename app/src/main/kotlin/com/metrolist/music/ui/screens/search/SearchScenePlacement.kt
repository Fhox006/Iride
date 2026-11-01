package com.metrolist.music.ui.screens.search

internal data class SearchScenePlacement(
    val fieldTop: Float,
    val resultTop: Float,
    val resultsTop: Float,
)

/** Page motion is immediate; only the detachment weight is animated. */
internal fun searchSceneForegroundTop(
    animatedTop: Float,
    pageOffset: Float,
    pullOffset: Float,
    liftProgress: Float,
): Float = animatedTop + (pageOffset + pullOffset) * (1f - liftProgress)

/** Keep the rendered field, including its zoom, inside both the scene and the visible IME edge. */
internal fun searchSceneKeyboardSafeTop(
    proposedTop: Float,
    editing: Boolean,
    viewportHeight: Float,
    sceneHeight: Float,
    keyboardBottom: Int,
    fieldHeight: Float,
    fieldScale: Float,
    gap: Float,
): Float {
    if (!editing) return proposedTop
    val sceneBottom = if (sceneHeight > 0f) minOf(viewportHeight, sceneHeight) else viewportHeight
    val keyboardTop = viewportHeight - keyboardBottom.coerceAtLeast(0)
    val zoomOverflow = fieldHeight * (fieldScale - 1f).coerceAtLeast(0f) * 0.5f
    val safeTop = (minOf(sceneBottom, keyboardTop) - fieldHeight - zoomOverflow - gap).coerceAtLeast(0f)
    return proposedTop.coerceAtMost(safeTop)
}

/** Separation is visible on the first edit frame, before the foreground starts moving. */
internal fun searchSceneBlurRadius(editing: Boolean, progress: Float): Float =
    if (editing) (28f * progress).coerceAtLeast(6f) else 28f * progress

internal fun searchRecallVisibleRows(availableHeight: Float, rowHeight: Float, entryCount: Int): Int {
    if (rowHeight <= 0f || availableHeight <= 0f) return 0
    return (availableHeight / rowHeight).toInt().coerceIn(0, entryCount.coerceIn(0, 6))
}

/** Once the header leaves the lazy viewport, its photo must leave with it, never reset to zero. */
internal fun searchArtworkPageOffset(headerOffset: Int?, headerHeight: Float, firstVisibleItemIndex: Int): Float =
    headerOffset?.toFloat() ?: if (firstVisibleItemIndex > 0) -headerHeight else 0f

/** Reserve the caret's natural position, including when text fills its viewport. */
internal fun searchCursorLeft(caretLeft: Float, scrollOffset: Int, gap: Float): Float =
    (caretLeft - scrollOffset).coerceAtLeast(0f) + gap

/** Resting titles ignore the IME; editing targets the space above its final inset. */
internal fun searchScenePlacement(
    viewportHeight: Float,
    initialFieldTop: Float,
    editing: Boolean,
    hasResult: Boolean,
    keyboardTargetBottom: Int,
    keyboardFallbackReady: Boolean,
    fieldHeight: Float,
    resultHeight: Float,
    gap: Float,
): SearchScenePlacement {
    val resultTop = ((viewportHeight - resultHeight) * 0.5f).coerceAtLeast(0f)
    val restingTop = if (hasResult) resultTop else initialFieldTop
    val fieldTop = if (editing && (keyboardTargetBottom > 0 || keyboardFallbackReady)) {
        val availableBottom = if (keyboardTargetBottom > 0) {
            viewportHeight - keyboardTargetBottom
        } else {
            viewportHeight * 0.64f
        }
        (availableBottom - fieldHeight - gap).coerceAtLeast(0f)
    } else {
        restingTop
    }
    return SearchScenePlacement(fieldTop, resultTop, resultTop + resultHeight + gap)
}
