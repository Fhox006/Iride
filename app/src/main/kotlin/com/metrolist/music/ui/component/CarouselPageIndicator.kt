package com.metrolist.music.ui.component

import com.metrolist.music.ui.theme.drawFolioRoundRect

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.metrolist.music.ui.theme.textPrimary
import com.metrolist.music.ui.utils.rememberReducedMotion
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.sign

@Composable
fun CarouselPageIndicator(
    pageCount: Int,
    currentPage: Int,
    modifier: Modifier = Modifier,
    overscrollOffset: Float = 0f,
) {
    if (pageCount <= 1) return

    val reducedMotion = rememberReducedMotion()
    val visibleCount = minOf(pageCount, 5)
    val centerSlot = visibleCount / 2
    val slotSize = 10.dp
    val selectedWidth = 9.dp
    val density = LocalDensity.current
    val slotPx = with(density) { slotSize.toPx() }
    val selectedWidthPx = with(density) { selectedWidth.toPx() }
    val initialPosition = currentPage.toFloat()
    val animatedPage = remember(pageCount) { Animatable(initialPosition) }
    val inactiveColor = MaterialTheme.colorScheme.onSurface
    val activeColor = MaterialTheme.colorScheme.textPrimary
    val endPull = with(density) { 4.dp.toPx() }

    LaunchedEffect(currentPage, pageCount, reducedMotion) {
        if (reducedMotion) animatedPage.snapTo(currentPage.toFloat())
        else animatedPage.animateTo(
            currentPage.toFloat(),
            spring(dampingRatio = 0.86f, stiffness = 540f),
        )
    }

    Box(
        modifier = modifier.fillMaxWidth().padding(top = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(
            modifier = Modifier.size(
                width = slotSize * visibleCount,
                height = 7.dp,
            ),
        ) {
            val centerY = size.height / 2f
            val firstVisiblePage =
                if (pageCount > visibleCount) {
                    (animatedPage.value - centerSlot).coerceIn(0f, (pageCount - visibleCount).toFloat())
                } else {
                    0f
                }
            val selectedX =
                (animatedPage.value - firstVisiblePage) * slotPx + slotPx / 2f
            val pullProgress =
                if (reducedMotion || overscrollOffset == 0f) 0f
                else (abs(overscrollOffset) / size.width).coerceIn(0f, 1f)
            val pullShift = sign(overscrollOffset) * endPull * pullProgress
            val pullScale = 1f - 0.08f * pullProgress

            withTransform({
                translate(left = pullShift)
                scale(
                    scaleX = pullScale,
                    scaleY = 1f,
                    pivot = Offset(if (overscrollOffset > 0f) 0f else size.width, centerY),
                )
            }) {
                if (pageCount > visibleCount) {
                    // Slide the five-dot window beneath the centered active pill. Only dots
                    // entering or leaving through either edge scale and fade.
                    val firstCandidate = floor(firstVisiblePage).toInt() - 1
                    val lastCandidate = firstCandidate + visibleCount + 1
                    val hasHiddenBefore = firstVisiblePage > 0f
                    val hasHiddenAfter = firstVisiblePage < pageCount - visibleCount
                    for (index in firstCandidate..lastCandidate) {
                        if (index !in 0 until pageCount) continue
                        val x = (index - firstVisiblePage + 0.5f) * slotPx
                        if (x !in 0f..size.width) continue
                        val leadingProgress =
                            if (hasHiddenBefore) ((slotPx - x) / slotPx).coerceIn(0f, 1f) else 0f
                        val trailingProgress =
                            if (hasHiddenAfter) ((x - (size.width - slotPx)) / slotPx).coerceIn(0f, 1f) else 0f
                        val edgeProgress = smoothstep(maxOf(leadingProgress, trailingProgress))
                        val radius = lerp(1.5.dp.toPx(), 0.75.dp.toPx(), edgeProgress)
                        val alpha = lerp(0.52f, 0.18f, edgeProgress)
                        drawCircle(inactiveColor.copy(alpha = alpha), radius, Offset(x, centerY))
                    }
                } else {
                    for (slot in 0 until visibleCount) {
                        val x = slot * slotPx + slotPx / 2f
                        drawCircle(inactiveColor.copy(alpha = 0.52f), 1.5.dp.toPx(), Offset(x, centerY))
                    }
                }

                val pillHeight = 4.dp.toPx()
                drawFolioRoundRect(
                    color = activeColor,
                    topLeft = Offset(selectedX - selectedWidthPx / 2f, centerY - pillHeight / 2f),
                    size = androidx.compose.ui.geometry.Size(selectedWidthPx, pillHeight),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(pillHeight / 2f),
                )
            }
        }
    }
}

private fun lerp(start: Float, end: Float, fraction: Float): Float =
    start + (end - start) * fraction

private fun smoothstep(fraction: Float): Float {
    val value = fraction.coerceIn(0f, 1f)
    return value * value * (3f - 2f * value)
}
