package com.metrolist.music.ui.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.gestures.stopScroll
import androidx.compose.foundation.layout.width
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import kotlinx.coroutines.launch

private class FolioScrollbarBounds {
    var height = 0
}

/** A shared Folio scroll indicator and drag target, usable with lists or scrollable columns. */
@Composable
internal fun IrideFolioScrollbar(scrollState: ScrollableState, modifier: Modifier = Modifier) {
    val indicator = scrollState.scrollIndicatorState ?: return
    val bounds = remember { FolioScrollbarBounds() }
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val ink = LocalContentColor.current
    val padding = with(density) { IrideFolio.ScrollbarVerticalPadding.toPx() }
    val minimumThumbHeight = with(density) { IrideFolio.ScrollbarMinimumThumbHeight.toPx() }
    val thumb: () -> FolioScrollbarThumb? = {
        folioScrollbarThumb(
            trackHeight = bounds.height - padding * 2,
            contentHeight = indicator.contentSize,
            viewportHeight = indicator.viewportSize,
            scrollOffset = indicator.scrollOffset,
            minimumThumbHeight = minimumThumbHeight,
        )
    }
    val dragState = rememberDraggableState { delta ->
        thumb()?.let {
            if (it.travel > 0f) scrollState.dispatchRawDelta(delta * it.scrollRange / it.travel)
        }
    }
    Canvas(
        modifier.width(IrideFolio.ScrollbarTouchTargetWidth)
            .onSizeChanged { bounds.height = it.height }
            .draggable(
                state = dragState,
                orientation = Orientation.Vertical,
                onDragStarted = { scrollState.stopScroll() },
            )
            .semantics {
                val metrics = thumb()
                progressBarRangeInfo = ProgressBarRangeInfo(
                    if (metrics != null && metrics.travel > 0f) metrics.offset / metrics.travel else 0f,
                    0f..1f,
                )
                setProgress { fraction ->
                    val current = thumb() ?: return@setProgress false
                    scope.launch {
                        scrollState.scrollBy(fraction.coerceIn(0f, 1f) * current.scrollRange - indicator.scrollOffset)
                    }
                    true
                }
            },
    ) {
        val metrics = thumb() ?: return@Canvas
        val width = IrideFolio.ScrollbarWidth.toPx()
        val x = size.width - IrideFolio.ScrollbarEdgeInset.toPx() - width
        val radius = CornerRadius(width / 2)
        drawRoundRect(
            color = ink.copy(alpha = IrideFolio.ScrollbarTrackOpacity),
            topLeft = Offset(x, padding),
            size = Size(width, size.height - padding * 2),
            cornerRadius = radius,
        )
        drawRoundRect(
            color = ink.copy(alpha = IrideFolio.ScrollbarThumbOpacity),
            topLeft = Offset(x, padding + metrics.offset),
            size = Size(width, metrics.height),
            cornerRadius = radius,
        )
    }
}
