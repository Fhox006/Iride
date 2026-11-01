package com.metrolist.music.models

import kotlin.math.abs

enum class ReverieGestureAxis { UNDECIDED, HORIZONTAL, VERTICAL }

fun reverieGestureAxis(x: Float, y: Float, slop: Float): ReverieGestureAxis = when {
    maxOf(abs(x), abs(y)) <= slop -> ReverieGestureAxis.UNDECIDED
    abs(x) > abs(y) -> ReverieGestureAxis.HORIZONTAL
    else -> ReverieGestureAxis.VERTICAL
}

fun reverieSwipeStep(distance: Float, velocity: Float, width: Float): Int = when {
    abs(distance) < width * .14f && abs(velocity) < 700f -> 0
    distance < 0 -> 1
    distance > 0 -> -1
    velocity < 0 -> 1
    else -> -1
}

/** Explicit measured height leaves a top gap even in short windows. Insets belong to the content. */
fun reveriePanelHeight(availableHeight: Float): Float {
    val height = availableHeight.coerceAtLeast(0f)
    return minOf(height * .89f, (height - 48f).coerceAtLeast(0f))
}

data class ReverieCursor(val chapter: Int = 0, val revision: Int = 0, val instant: Boolean = false)

/** Synchronous selection; media preparation cannot delay the visible chapter or its timer reset. */
fun seekReverieChapter(cursor: ReverieCursor, target: Int, count: Int): ReverieCursor =
    if (target !in 0 until count) cursor else ReverieCursor(target, cursor.revision + 1, instant = true)
