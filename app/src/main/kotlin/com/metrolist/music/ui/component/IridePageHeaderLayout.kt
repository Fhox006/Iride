package com.metrolist.music.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.sp

/** The shared page title and bar, with a configurable initial reveal for existing scrolling content. */
@Composable
fun IridePageHeaderLayout(
    title: String,
    modifier: Modifier = Modifier,
    initiallyCollapsed: Boolean = false,
    navigationIcon: (@Composable () -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
    content: @Composable BoxScope.() -> Unit,
) {
    val density = LocalDensity.current
    val statusBarHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val barBottom = statusBarHeight + IridePageLayoutDefaults.BarHeight
    var titleHeightPx by remember { mutableIntStateOf(0) }
    var collapseFraction by rememberSaveable { mutableFloatStateOf(if (initiallyCollapsed) 1f else 0f) }
    val textHeightPx = if (titleHeightPx > 0) titleHeightPx.toFloat() else with(density) { 41.sp.toPx() }
    val topSpacingPx = with(density) { IridePageLayoutDefaults.ContentTopSpacing.toPx() }
    val revealHeightPx = textHeightPx + topSpacingPx
    val collapsedDistancePx = collapseFraction * revealHeightPx
    val progress = iridePageTitleProgress(topSpacingPx - collapsedDistancePx, textHeightPx, 0f, pastTitle = false)
    val expandedHeight = with(density) { (revealHeightPx - collapsedDistancePx).toDp() } +
        IridePageLayoutDefaults.TitleBottomSpacing
    val headerDrag = rememberDraggableState { delta ->
        collapseFraction = (collapseFraction - delta / revealHeightPx).coerceIn(0f, 1f)
    }
    val connection = remember(revealHeightPx) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (available.y >= 0f) return Offset.Zero
                val previous = collapseFraction
                collapseFraction = (previous - available.y / revealHeightPx).coerceIn(0f, 1f)
                return Offset(0f, -(collapseFraction - previous) * revealHeightPx)
            }

            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                // Reveal only after the child has returned to the beginning of its content.
                if (available.y <= 0f) return Offset.Zero
                val previous = collapseFraction
                collapseFraction = (previous - available.y / revealHeightPx).coerceIn(0f, 1f)
                return Offset(0f, (previous - collapseFraction) * revealHeightPx)
            }
        }
    }

    Box(
        modifier = modifier.fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .nestedScroll(connection),
    ) {
        Box(
            modifier = Modifier.fillMaxSize().padding(top = barBottom + expandedHeight),
            content = content,
        )
        Box(
            modifier = Modifier.fillMaxWidth().padding(top = barBottom)
                .height(expandedHeight).clipToBounds()
                .draggable(headerDrag, Orientation.Vertical),
        ) {
            IridePageTitle(
                title = title,
                maxLines = 2,
                modifier = Modifier
                    .graphicsLayer { translationY = -collapsedDistancePx }
                    .wrapContentHeight(align = Alignment.Top, unbounded = true)
                    .padding(
                        start = IridePageLayoutDefaults.HorizontalPadding,
                        end = IridePageLayoutDefaults.HorizontalPadding,
                        top = IridePageLayoutDefaults.ContentTopSpacing,
                        bottom = IridePageLayoutDefaults.TitleBottomSpacing,
                    )
                    .onSizeChanged { titleHeightPx = it.height },
            )
        }
        IridePageTopBar(
            title = title,
            progress = progress,
            modifier = Modifier.draggable(headerDrag, Orientation.Vertical),
            navigationIcon = navigationIcon,
            actions = actions,
        )
    }
}
