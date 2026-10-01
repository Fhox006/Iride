package com.metrolist.music.ui.component

import androidx.annotation.StringRes
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex

object IridePageLayoutDefaults {
    val BarHeight = 40.dp
    val HorizontalPadding = 20.dp
    val ContentTopSpacing = 8.dp
    val TitleBottomSpacing = 4.dp
    val TitleFadeDistance = 40.dp
    val BarColor = Color(0xFF020202)
}

internal val LocalPageControlsProgress = staticCompositionLocalOf { 0f }
private const val PAGE_FADE_DURATION_MS = 180

private const val PAGE_TITLE_KEY = "iride_page_title"

/** Preserve the caller's side/bottom clearance and reserve space for the pinned page title. */
@Composable
fun iridePageContentInsets(contentInsets: WindowInsets, barHeight: Dp = IridePageLayoutDefaults.BarHeight): WindowInsets =
    contentInsets.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom)
        .add(WindowInsets.statusBars.only(WindowInsetsSides.Top))
        .add(WindowInsets(top = barHeight + IridePageLayoutDefaults.ContentTopSpacing))

/**
 * Shared page protocol: title stays pinned and fades with scroll; page-wide rubber-band belongs to
 * [PageFrame], so title and content move together at the scroll boundary. Keep system status bars
 * visible and preserve their top inset as layout spacing.
 * Bottom clearance belongs to the caller.
 */
@Composable
fun IridePageLayout(
    @StringRes titleRes: Int,
    modifier: Modifier = Modifier,
    state: LazyListState = rememberLazyListState(),
    listModifier: Modifier = Modifier,
    reducedMotion: Boolean = false,
    titleTopSpacing: Dp = IridePageLayoutDefaults.ContentTopSpacing,
    bottomPadding: Dp = 0.dp,
    contentHorizontalPadding: Dp = 0.dp,
    navigationIcon: (@Composable () -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
    showTopBar: Boolean = false,
    topBarStrokeColor: Color = Color.White.copy(alpha = 0.08f),
    overlay: @Composable BoxScope.() -> Unit = {},
    pageControls: (@Composable BoxScope.() -> Unit)? = null,
    content: LazyListScope.() -> Unit,
) {
    val effectiveTitleTopSpacing = if ((showTopBar || pageControls != null) && titleTopSpacing == IridePageLayoutDefaults.ContentTopSpacing) {
        IridePageLayoutDefaults.BarHeight + IridePageLayoutDefaults.ContentTopSpacing + 16.dp
    } else {
        titleTopSpacing
    }
    val header = rememberPageHeader(state, effectiveTitleTopSpacing) {
        TitlePosition(
            pastTitle = state.firstVisibleItemIndex > 0,
            scrollOffset = state.firstVisibleItemScrollOffset,
            // Lazy offsets exclude the leading content padding; page overlays include it.
            contentTop = state.layoutInfo.visibleItemsInfo.firstOrNull { it.key != PAGE_TITLE_KEY }
                ?.let { (it.offset + state.layoutInfo.beforeContentPadding).toFloat() }
                ?: Float.POSITIVE_INFINITY,
        )
    }
    val title = stringResource(titleRes)
    PageFrame(
        title = title.lowercase().replaceFirstChar { it.titlecase() },
        header = header,
        modifier = modifier,
        reducedMotion = reducedMotion,
        scrollState = state,
        overlay = overlay,
        navigationIcon = navigationIcon,
        actions = actions,
        showTopBar = showTopBar,
        pageControls = pageControls,
        topBarStrokeColor = topBarStrokeColor,
    ) {
        LazyColumn(
            state = state,
            overscrollEffect = null,
            contentPadding = PaddingValues(
                start = contentHorizontalPadding,
                end = contentHorizontalPadding,
                top = header.contentTopPadding,
                bottom = bottomPadding,
            ),
            modifier = Modifier.fillMaxSize().then(listModifier),
        ) {
            item(key = PAGE_TITLE_KEY) {
                PageTitle(titleRes, header, (IridePageLayoutDefaults.HorizontalPadding - contentHorizontalPadding).coerceAtLeast(0.dp))
            }
            content()
        }
    }
}

@Composable
fun IridePageFrame(
    title: String,
    scrollState: ScrollableState,
    scrollOffset: () -> Int = { (scrollState as? ScrollState)?.value ?: 0 },
    modifier: Modifier = Modifier,
    reducedMotion: Boolean = false,
    navigationIcon: (@Composable () -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
    content: @Composable BoxScope.() -> Unit,
) {
    val titleTopSpacing = IridePageLayoutDefaults.BarHeight + IridePageLayoutDefaults.ContentTopSpacing + 16.dp
    val header = rememberPageHeader(scrollState, titleTopSpacing) {
        TitlePosition(pastTitle = false, scrollOffset = scrollOffset(), contentTop = Float.POSITIVE_INFINITY)
    }
    val titleHeight = with(LocalDensity.current) { 41.sp.toDp() }
    PageFrame(
        title = title,
        header = header,
        modifier = modifier,
        reducedMotion = reducedMotion,
        scrollState = scrollState,
        navigationIcon = navigationIcon,
        actions = actions,
        showTopBar = true,
        topBarStrokeColor = Color.Transparent,
    ) {
        Box(
            modifier = Modifier.fillMaxSize().padding(
                top = header.contentTopPadding + titleHeight + IridePageLayoutDefaults.TitleBottomSpacing,
            ),
            content = content,
        )
    }
}

/** The same header for grids, with a full-width title and the standard horizontal page margins. */
@Composable
fun IridePageLayout(
    @StringRes titleRes: Int,
    columns: GridCells,
    modifier: Modifier = Modifier,
    state: LazyGridState = rememberLazyGridState(),
    gridModifier: Modifier = Modifier,
    reducedMotion: Boolean = false,
    titleTopSpacing: Dp = IridePageLayoutDefaults.ContentTopSpacing,
    bottomPadding: Dp = 0.dp,
    navigationIcon: (@Composable () -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
    showTopBar: Boolean = false,
    topBarStrokeColor: Color = Color.White.copy(alpha = 0.08f),
    pageControls: (@Composable BoxScope.() -> Unit)? = null,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    content: LazyGridScope.() -> Unit,
) {
    val effectiveTitleTopSpacing = if ((showTopBar || pageControls != null) && titleTopSpacing == IridePageLayoutDefaults.ContentTopSpacing) {
        IridePageLayoutDefaults.BarHeight + IridePageLayoutDefaults.ContentTopSpacing + 16.dp
    } else {
        titleTopSpacing
    }
    val header = rememberPageHeader(state, effectiveTitleTopSpacing) {
        TitlePosition(
            pastTitle = state.firstVisibleItemIndex > 0,
            scrollOffset = state.firstVisibleItemScrollOffset,
            contentTop = state.layoutInfo.visibleItemsInfo.firstOrNull { it.key != PAGE_TITLE_KEY }
                ?.let { (it.offset.y + state.layoutInfo.beforeContentPadding).toFloat() }
                ?: Float.POSITIVE_INFINITY,
        )
    }
    PageFrame(
        stringResource(titleRes).lowercase().replaceFirstChar { it.titlecase() },
        header,
        modifier,
        reducedMotion,
        state,
        navigationIcon = navigationIcon,
        actions = actions,
        showTopBar = showTopBar,
        pageControls = pageControls,
        topBarStrokeColor = topBarStrokeColor,
    ) {
        LazyVerticalGrid(
            columns = columns,
            state = state,
            overscrollEffect = null,
            contentPadding = PaddingValues(
                start = IridePageLayoutDefaults.HorizontalPadding,
                end = IridePageLayoutDefaults.HorizontalPadding,
                top = header.contentTopPadding,
                bottom = bottomPadding,
            ),
            horizontalArrangement = horizontalArrangement,
            verticalArrangement = verticalArrangement,
            modifier = Modifier.fillMaxSize().then(gridModifier),
        ) {
            item(key = PAGE_TITLE_KEY, span = { GridItemSpan(maxLineSpan) }) {
                PageTitle(titleRes, header, horizontalPadding = 0.dp)
            }
            content()
        }
    }
}

private data class TitlePosition(val pastTitle: Boolean, val scrollOffset: Int, val contentTop: Float)

private class PageHeader(
    val contentTopPadding: Dp,
    val titleTopSpacing: Dp,
    private val fadeDistancePx: Float,
    titlePosition: () -> TitlePosition,
) {
    val contentTop by derivedStateOf { titlePosition().contentTop }
    val scrolling by derivedStateOf { titlePosition().pastTitle || titlePosition().scrollOffset > 0 }
    val progress by derivedStateOf {
        val position = titlePosition()
        val scrollProgress = when {
            position.pastTitle -> 1f
            fadeDistancePx <= 0f -> 1f
            else -> (position.scrollOffset / fadeDistancePx).coerceIn(0f, 1f)
        }
        FastOutSlowInEasing.transform(scrollProgress)
    }
}

@Composable
private fun rememberPageHeader(
    scrollState: Any,
    titleTopSpacing: Dp,
    titlePosition: () -> TitlePosition,
): PageHeader {
    val statusBarHeight = iridePageStatusBarTopPadding()
    val fadeDistancePx = with(LocalDensity.current) { IridePageLayoutDefaults.TitleFadeDistance.toPx() }
    // The position callback only captures the list/grid state, which is also the remember key.
    return remember(scrollState, statusBarHeight, titleTopSpacing, fadeDistancePx, titlePosition) {
        PageHeader(statusBarHeight + titleTopSpacing, titleTopSpacing, fadeDistancePx, titlePosition)
    }
}

@Composable
private fun PageTitle(@StringRes titleRes: Int, header: PageHeader, horizontalPadding: Dp) {
    IridePageTitle(
        titleRes = titleRes,
        monospace = true,
        modifier = Modifier
            .alpha(0f)
            .padding(
                start = horizontalPadding,
                end = horizontalPadding,
                bottom = IridePageLayoutDefaults.TitleBottomSpacing,
            ),
    )
}

@Composable
private fun PageFrame(
    title: String,
    header: PageHeader,
    modifier: Modifier,
    reducedMotion: Boolean,
    scrollState: ScrollableState,
    overlay: @Composable BoxScope.() -> Unit = {},
    navigationIcon: (@Composable () -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
    showTopBar: Boolean = false,
    pageControls: (@Composable BoxScope.() -> Unit)? = null,
    topBarStrokeColor: Color = Color.White.copy(alpha = 0.08f),
    content: @Composable () -> Unit,
) {
    val backdrop = rememberFrostBackdrop()
    val statusBarHeight = iridePageStatusBarTopPadding()
    val density = LocalDensity.current
    val controlsBottomPx = with(density) { (statusBarHeight + 56.dp).toPx() }
    val titleAlpha by animateFloatAsState(
        targetValue = 1f - header.progress,
        animationSpec = tween(if (reducedMotion) 0 else PAGE_FADE_DURATION_MS),
        label = "pageTitleFade",
    )
    val controlsProgress by animateFloatAsState(
        targetValue = if (header.scrolling && header.contentTop <= controlsBottomPx) 1f else 0f,
        animationSpec = tween(if (reducedMotion) 0 else PAGE_FADE_DURATION_MS),
        label = "pageControlsFade",
    )
    Box(
        modifier = modifier
            .rubberBandOverscroll(Orientation.Vertical, scrollState)
            .fillMaxSize(),
    ) {
        Box(Modifier.fillMaxSize().recordFrostBackdrop(backdrop)) {
            content()
            overlay()

        }
        IridePageTitle(
                title = title,
                monospace = true,
                modifier = Modifier
                    .padding(
                        start = IridePageLayoutDefaults.HorizontalPadding,
                        end = IridePageLayoutDefaults.HorizontalPadding,
                        top = statusBarHeight + header.titleTopSpacing,
                    )
                    .alpha(titleAlpha)
                    .zIndex(1f),
            )

        if (showTopBar) {
            IridePageTopBar(
                title = title,
                progress = header.progress,
                strokeColor = topBarStrokeColor,
                navigationIcon = navigationIcon,
                actions = actions,
                reducedMotion = reducedMotion,
            )
        }
        CompositionLocalProvider(
            LocalScreenFrostBackdrop provides backdrop,
            LocalPageControlsProgress provides controlsProgress,
        ) {
            pageControls?.invoke(this@Box)
        }
    }
}

/** Settings-style frame that shares the same title fade and collapsing top bar as page lists. */
@Composable
fun IridePageFrame(
    title: String,
    scrollState: ScrollState,
    modifier: Modifier = Modifier,
    reducedMotion: Boolean = false,
    navigationIcon: (@Composable () -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
    content: @Composable BoxScope.() -> Unit,
) {
    val titleTopSpacing = IridePageLayoutDefaults.BarHeight + IridePageLayoutDefaults.ContentTopSpacing + 16.dp
    val header = rememberPageHeader(scrollState, titleTopSpacing) {
        TitlePosition(pastTitle = false, scrollOffset = scrollState.value, contentTop = Float.POSITIVE_INFINITY)
    }
    val titleHeight = with(LocalDensity.current) { 41.sp.toDp() }
    PageFrame(
        title = title,
        header = header,
        modifier = modifier,
        reducedMotion = reducedMotion,
        scrollState = scrollState,
        navigationIcon = navigationIcon,
        actions = actions,
        showTopBar = true,
        topBarStrokeColor = Color.Transparent,
    ) {
        Box(
            modifier = Modifier.fillMaxSize().padding(
                top = header.contentTopPadding + titleHeight + IridePageLayoutDefaults.TitleBottomSpacing,
            ),
            content = content,
        )
    }
}

@Composable
fun iridePageStatusBarTopPadding(): Dp {
    val resources = LocalContext.current.resources
    val density = LocalDensity.current
    val inset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val resourceId = remember(resources) {
        resources.getIdentifier("status_bar_height", "dimen", "android")
    }
    val systemHeight = if (resourceId != 0) with(density) { resources.getDimension(resourceId).toDp() } else 0.dp
    return maxOf(inset, systemHeight)
}




