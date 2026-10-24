package com.metrolist.music.ui.component

import androidx.annotation.StringRes
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.LocalOverscrollFactory
import androidx.compose.foundation.background
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.metrolist.music.ui.utils.rememberReducedMotion
import com.metrolist.music.LocalPlayerAwareWindowInsets

object IridePageLayoutDefaults {
    val HorizontalPadding = IrideFolio.HorizontalPadding
    val ContentTopSpacing = IrideFolio.TitleTopSpacing
    val TitleBottomSpacing = IrideFolio.TitleBottomSpacing
    val BarColor = Color.Transparent
}

internal val LocalPageControlsProgress = staticCompositionLocalOf { 0f }

/** A floating header can own the title while the page retains its usual content spacing. */
val LocalIridePageTitleVisible = staticCompositionLocalOf { true }

/** A detached editor shares its page's pull when it returns to rest. */
val LocalIridePageOverscrollPull = staticCompositionLocalOf<RubberBandPull?> { null }

private const val PAGE_TITLE_KEY = "iride_page_title"

/**
 * Iride Folio adapter: title scrolls with the content; page-wide rubber-band belongs to
 * [PageFrame], so title and content move together at the scroll boundary. Keep system status bars
 * visible and preserve their top inset as layout spacing. Bottom clearance belongs to the caller.
 * Artwork headers can own their scrolling title with [showPageTitle] false and override
 * [contentTopPadding]. [overscrollPull] exposes the same outer spring to their image transform;
 * [translateOverscroll] can leave the page anchored when artwork alone handles the pull.
 */
@Composable
fun IridePageLayout(
    @StringRes titleRes: Int = 0,
    title: String =
        if (titleRes != 0) stringResource(titleRes).lowercase().replaceFirstChar { it.titlecase() }
        else "",
    modifier: Modifier = Modifier,
    state: LazyListState = rememberLazyListState(),
    listModifier: Modifier = Modifier,
    reducedMotion: Boolean = rememberReducedMotion(),
    titleTopSpacing: Dp = IridePageLayoutDefaults.ContentTopSpacing,
    bottomPadding: Dp = 0.dp,
    contentHorizontalPadding: Dp = 0.dp,
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    background: @Composable BoxScope.() -> Unit = {},
    navigationIcon: (@Composable () -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
    menuSections: List<List<IrideFolioMenuAction>> = emptyList(),
    topBarContent: (@Composable () -> Unit)? = null,
    showTopBar: Boolean = false,
    topBarStrokeColor: Color = Color.White.copy(alpha = 0.08f),
    overlay: @Composable BoxScope.() -> Unit = {},
    pageControls: (@Composable BoxScope.() -> Unit)? = null,
    showPageTitle: Boolean = LocalIridePageTitleVisible.current,
    showTopScrim: Boolean = true,
    topScrimEncounter: (() -> Float)? = null,
    topScrimColor: Color = LocalIrideFolioScrimColor.current,
    contentTopPadding: Dp? = null,
    animateTopScrimColor: Boolean = true,
    overscrollPull: RubberBandPull? = LocalIridePageOverscrollPull.current,
    translateOverscroll: Boolean = true,
    content: LazyListScope.() -> Unit,
) {
    val effectiveTitleTopSpacing =
        if (
            (showTopBar || menuSections.any { it.isNotEmpty() } || pageControls != null) &&
                titleTopSpacing == IridePageLayoutDefaults.ContentTopSpacing
        ) {
            IrideFolio.TitleAfterControls
        } else {
            titleTopSpacing
        }
    val header =
        rememberPageHeader(state, effectiveTitleTopSpacing) {
            TitlePosition(
                pastTitle = state.firstVisibleItemIndex > 0,
                scrollOffset = state.firstVisibleItemScrollOffset,
                // Lazy offsets exclude the leading content padding; page overlays include it.
                encounterTop = state.layoutInfo.visibleItemsInfo.firstOrNull()
                    ?.let { (it.offset + state.layoutInfo.beforeContentPadding).toFloat() }
                    ?: Float.POSITIVE_INFINITY,
            )
        }
    PageFrame(
        title = title,
        header = header,
        modifier = modifier,
        reducedMotion = reducedMotion,
        scrollState = state,
        overlay = overlay,
        background = background,
        navigationIcon = navigationIcon,
        actions = actions,
        menuSections = menuSections,
        topBarContent = topBarContent,
        showTopBar = showTopBar || menuSections.any { it.isNotEmpty() },
        pageControls = pageControls,
        topBarStrokeColor = topBarStrokeColor,
        showTopScrim = showTopScrim,
        topScrimEncounter = topScrimEncounter,
        topScrimColor = topScrimColor,
        animateTopScrimColor = animateTopScrimColor,
        overscrollPull = overscrollPull,
        translateOverscroll = translateOverscroll,
    ) {
        LazyColumn(
            state = state,
            verticalArrangement = verticalArrangement,
            overscrollEffect = null,
            contentPadding =
                PaddingValues(
                    start = contentHorizontalPadding,
                    end = contentHorizontalPadding,
                    top = contentTopPadding ?: header.contentTopPadding,
                    bottom = bottomPadding,
                ),
            modifier = Modifier.fillMaxSize().then(listModifier),
        ) {
            if (showPageTitle) {
                item(key = PAGE_TITLE_KEY) {
                    PageTitle(
                        title,
                        (IridePageLayoutDefaults.HorizontalPadding - contentHorizontalPadding)
                            .coerceAtLeast(0.dp),
                        visible = topBarContent == null,
                    )
                }
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
    reducedMotion: Boolean = rememberReducedMotion(),
    background: @Composable BoxScope.() -> Unit = {},
    navigationIcon: (@Composable () -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
    menuSections: List<List<IrideFolioMenuAction>> = emptyList(),
    content: @Composable BoxScope.() -> Unit,
) {
    val titleTopSpacing = IrideFolio.TitleAfterControls
    val density = LocalDensity.current
    val contentTop =
        iridePageStatusBarTopPadding() +
            titleTopSpacing +
            with(density) { IrideFolio.TitleLineHeight.toDp() }
    val titleTopPx = with(density) { contentTop.toPx() - IrideFolio.TitleLineHeight.toPx() }
    val header =
        rememberPageHeader(scrollState, titleTopSpacing) {
            TitlePosition(
                pastTitle = false,
                scrollOffset = scrollOffset(),
                // Observe the moving title even when a native document keeps its viewport below the controls.
                encounterTop = titleTopPx - scrollOffset(),
            )
        }
    val titleHeight = with(LocalDensity.current) { IrideFolio.TitleLineHeight.toDp() }
    PageFrame(
        title = title,
        header = header,
        modifier = modifier,
        reducedMotion = reducedMotion,
        scrollState = scrollState,
        background = background,
        navigationIcon = navigationIcon,
        actions = actions,
        menuSections = menuSections,
        showTopBar = true,
        topBarStrokeColor = Color.Transparent,
        titleScrollOffset = scrollOffset,
    ) {
        Box(
            modifier =
                Modifier.fillMaxSize()
                    .padding(
                        top =
                            header.contentTopPadding +
                                titleHeight +
                                IridePageLayoutDefaults.TitleBottomSpacing
                    ),
            content = content,
        )
    }
}

/** The same header for grids, with a full-width title and the standard horizontal page margins. */
@Composable
fun IridePageLayout(
    @StringRes titleRes: Int = 0,
    title: String =
        if (titleRes != 0) stringResource(titleRes).lowercase().replaceFirstChar { it.titlecase() }
        else "",
    columns: GridCells,
    modifier: Modifier = Modifier,
    state: LazyGridState = rememberLazyGridState(),
    gridModifier: Modifier = Modifier,
    reducedMotion: Boolean = rememberReducedMotion(),
    titleTopSpacing: Dp = IridePageLayoutDefaults.ContentTopSpacing,
    bottomPadding: Dp = 0.dp,
    background: @Composable BoxScope.() -> Unit = {},
    navigationIcon: (@Composable () -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
    menuSections: List<List<IrideFolioMenuAction>> = emptyList(),
    topBarContent: (@Composable () -> Unit)? = null,
    showTopBar: Boolean = false,
    topBarStrokeColor: Color = Color.White.copy(alpha = 0.08f),
    pageControls: (@Composable BoxScope.() -> Unit)? = null,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    content: LazyGridScope.() -> Unit,
) {
    val effectiveTitleTopSpacing =
        if (
            (showTopBar || menuSections.any { it.isNotEmpty() } || pageControls != null) &&
                titleTopSpacing == IridePageLayoutDefaults.ContentTopSpacing
        ) {
            IrideFolio.TitleAfterControls
        } else {
            titleTopSpacing
        }
    val header =
        rememberPageHeader(state, effectiveTitleTopSpacing) {
            TitlePosition(
                pastTitle = state.firstVisibleItemIndex > 0,
                scrollOffset = state.firstVisibleItemScrollOffset,
                encounterTop = state.layoutInfo.visibleItemsInfo.firstOrNull()
                    ?.let { (it.offset.y + state.layoutInfo.beforeContentPadding).toFloat() }
                    ?: Float.POSITIVE_INFINITY,
            )
        }
    PageFrame(
        title,
        header,
        modifier,
        reducedMotion,
        state,
        background = background,
        navigationIcon = navigationIcon,
        actions = actions,
        menuSections = menuSections,
        topBarContent = topBarContent,
        showTopBar = showTopBar || menuSections.any { it.isNotEmpty() },
        pageControls = pageControls,
        topBarStrokeColor = topBarStrokeColor,
    ) {
        LazyVerticalGrid(
            columns = columns,
            state = state,
            overscrollEffect = null,
            contentPadding =
                PaddingValues(
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
                PageTitle(title, horizontalPadding = 0.dp, visible = topBarContent == null)
            }
            content()
        }
    }
}

private data class TitlePosition(
    val pastTitle: Boolean,
    val scrollOffset: Int,
    val encounterTop: Float,
)

private class PageHeader(
    val contentTopPadding: Dp,
    val titleTopSpacing: Dp,
    titlePosition: () -> TitlePosition,
) {
    val encounterTop by derivedStateOf { titlePosition().encounterTop }
    val scrolling by derivedStateOf {
        titlePosition().pastTitle || titlePosition().scrollOffset > 0
    }
}

@Composable
private fun rememberPageHeader(
    scrollState: Any,
    titleTopSpacing: Dp,
    titlePosition: () -> TitlePosition,
): PageHeader {
    val statusBarHeight = iridePageStatusBarTopPadding()
    // The position callback only captures the list/grid state, which is also the remember key.
    return remember(scrollState, statusBarHeight, titleTopSpacing, titlePosition) {
        PageHeader(
            statusBarHeight + titleTopSpacing,
            titleTopSpacing,
            titlePosition,
        )
    }
}

@Composable
private fun PageTitle(title: String, horizontalPadding: Dp, visible: Boolean = true) {
    IridePageTitle(
        title = title,
        monospace = true,
        modifier =
            Modifier.then(
                    if (visible) Modifier else Modifier.alpha(0f).clearAndSetSemantics {}
                )
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
    background: @Composable BoxScope.() -> Unit = {},
    navigationIcon: (@Composable () -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
    menuSections: List<List<IrideFolioMenuAction>> = emptyList(),
    topBarContent: (@Composable () -> Unit)? = null,
    showTopBar: Boolean = false,
    pageControls: (@Composable BoxScope.() -> Unit)? = null,
    topBarStrokeColor: Color = Color.White.copy(alpha = 0.08f),
    titleScrollOffset: (() -> Int)? = null,
    showTopScrim: Boolean = true,
    topScrimEncounter: (() -> Float)? = null,
    topScrimColor: Color = LocalIrideFolioScrimColor.current,
    animateTopScrimColor: Boolean = true,
    overscrollPull: RubberBandPull? = null,
    translateOverscroll: Boolean = true,
    content: @Composable () -> Unit,
) {
    val pagePull = overscrollPull ?: rememberRubberBandPull()
    val ambientBackground = LocalScreenFrostBackground.current
    val pageBackground = rememberFrostBackdrop(
        background = ambientBackground,
        backgroundColor = MaterialTheme.colorScheme.surface,
    )
    val backdrop = rememberFrostBackdrop(
        background = ambientBackground,
        backgroundColor = MaterialTheme.colorScheme.surface,
        stableBlur = true,
    )
    val statusBarHeight = iridePageStatusBarTopPadding()
    val scrimColor = if (animateTopScrimColor) {
        val animatedColor by animateColorAsState(
            targetValue = topScrimColor,
            animationSpec = tween(if (reducedMotion) 0 else IrideFolio.FadeDurationMillis),
            label = "irideFolioScrimColor",
        )
        animatedColor
    } else {
        topScrimColor
    }
    val scrimBrush = remember(scrimColor) { IrideFolio.topScrimBrush(scrimColor) }
    val controlsBottomPx =
        with(LocalDensity.current) {
            (statusBarHeight + IrideFolio.ControlsTopSpacing + IrideFolio.ControlsHeight).toPx()
        }
    val cameraBoundaryPx = with(LocalDensity.current) {
        (statusBarHeight + IrideFolio.TopScrimCameraGap).toPx()
    }
    val encounterTop = topScrimEncounter?.invoke() ?: header.encounterTop
    val firstEncounter = header.scrolling && encounterTop <= controlsBottomPx
    val scrimEncounter = header.scrolling && encounterTop <= cameraBoundaryPx
    val scrimProgress = rememberIrideFolioFade(if (showTopScrim && scrimEncounter) 1f else 0f, reducedMotion)
    val controlsProgress =
        rememberIrideFolioFade(
            if (firstEncounter) 1f else 0f,
            reducedMotion,
        )
    CompositionLocalProvider(
        LocalOverscrollFactory provides null,
        LocalScreenFrostBackdrop provides backdrop,
        LocalScreenFrostBackground provides pageBackground,
        LocalPageControlsProgress provides controlsProgress,
    ) {
        Box(
            modifier =
                modifier.fillMaxSize()
                    .rubberBandOverscroll(
                        Orientation.Vertical,
                        scrollState,
                        pagePull,
                        translateContent = false,
                    )
        ) {
            Box(
                Modifier.fillMaxSize()
                    .recordFrostBackdrop(
                        backdrop,
                        enabled = controlsProgress > 0f || (backdrop?.recordingRequests ?: 0) > 0,
                        trackScroll = true,
                    )
            ) {
                // A sibling handles exposed background without competing with the list's fling.
                Box(Modifier.fillMaxSize().irideFolioScrollReceiver(scrollState))
                Box(Modifier.fillMaxSize().recordFrostBackdrop(
                    pageBackground, enabled = (pageBackground?.recordingRequests ?: 0) > 0,
                )) {
                    CompositionLocalProvider(LocalScreenFrostBackground provides ambientBackground) {
                        background()
                    }
                }
                Box(
                    Modifier.fillMaxSize().graphicsLayer {
                        translationY = if (translateOverscroll) pagePull.offset else 0f
                    }
                ) {
                    content()
                    if (scrimProgress > 0f) {
                        Box(
                            Modifier.fillMaxWidth()
                                .height(
                                    statusBarHeight +
                                        IrideFolio.ControlsTopSpacing +
                                        IrideFolio.ControlsHeight +
                                        IrideFolio.TopScrimFadeHeight
                                )
                                .align(Alignment.TopCenter)
                                .graphicsLayer { alpha = scrimProgress }
                                .background(scrimBrush)
                        )
                    }
                    if (titleScrollOffset != null) {
                        Box(Modifier.fillMaxSize().clipToBounds()) {
                            IridePageTitle(
                                title = title,
                                monospace = true,
                                modifier =
                                    Modifier.graphicsLayer {
                                            translationY = -titleScrollOffset().coerceAtLeast(0).toFloat()
                                        }
                                        .padding(
                                            start = IridePageLayoutDefaults.HorizontalPadding,
                                            end = IridePageLayoutDefaults.HorizontalPadding,
                                            top = statusBarHeight + header.titleTopSpacing,
                                        ),
                            )
                        }
                    }
                }
            }
            if (showTopBar) {
                IridePageTopBar(
                    title = title,
                    progress = controlsProgress,
                    modifier = Modifier.irideFolioScrollReceiver(scrollState),
                    navigationIcon = navigationIcon,
                    actions = actions,
                    menuSections = menuSections,
                    titleContent = topBarContent,
                    reducedMotion = reducedMotion,
                )
            }
            overlay()
            pageControls?.invoke(this)
        }
    }
}

/**
 * Folio owns verticalScroll here. Callers supply ordinary Columns, with no second verticalScroll.
 * The title scrolls with the content, without pinning or fading. A
 * viewport-sized minimum keeps short/empty pages draggable across their whole background.
 */
@Composable
fun IridePageFrame(
    title: String,
    scrollState: ScrollState,
    modifier: Modifier = Modifier,
    reducedMotion: Boolean = rememberReducedMotion(),
    background: @Composable BoxScope.() -> Unit = {},
    navigationIcon: (@Composable () -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
    menuSections: List<List<IrideFolioMenuAction>> = emptyList(),
    showTopBar: Boolean = navigationIcon != null,
    overlay: @Composable BoxScope.() -> Unit = {},
    content: @Composable BoxScope.() -> Unit,
) {
    val titleTopSpacing =
        if (showTopBar || menuSections.any { it.isNotEmpty() }) IrideFolio.TitleAfterControls
        else IridePageLayoutDefaults.ContentTopSpacing
    var titleHeightPx by remember(title) { mutableIntStateOf(0) }
    val density = LocalDensity.current
    val titleHeight =
        with(density) {
            if (titleHeightPx > 0) titleHeightPx.toDp() else IrideFolio.TitleLineHeight.toDp()
        }
    val contentTop =
        iridePageStatusBarTopPadding() +
            titleTopSpacing +
            titleHeight +
            IridePageLayoutDefaults.TitleBottomSpacing
    val titleTopPx = with(density) { (contentTop - titleHeight - IridePageLayoutDefaults.TitleBottomSpacing).toPx() }
    val header =
        rememberPageHeader(scrollState, titleTopSpacing) {
            TitlePosition(false, scrollState.value, titleTopPx - scrollState.value)
        }
    PageFrame(
        title = title,
        header = header,
        modifier = modifier,
        reducedMotion = reducedMotion,
        scrollState = scrollState,
        background = background,
        navigationIcon = navigationIcon,
        actions = actions,
        menuSections = menuSections,
        showTopBar = showTopBar || menuSections.any { it.isNotEmpty() },
        overlay = overlay,
    ) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val minimumContentHeight = (maxHeight - contentTop).coerceAtLeast(0.dp)
            var contentOverflows by remember(scrollState) { mutableStateOf(false) }
            Column(Modifier.fillMaxSize().verticalScroll(scrollState, overscrollEffect = null)) {
                // Measuring the same typography avoids fixed-height assumptions with font scaling.
                IridePageTitle(
                    title = title,
                    modifier =
                        Modifier.padding(
                                top = header.contentTopPadding,
                                start = IridePageLayoutDefaults.HorizontalPadding,
                                end = IridePageLayoutDefaults.HorizontalPadding,
                            )
                            .onSizeChanged {
                                titleHeightPx =
                                    it.height -
                                        with(density) { header.contentTopPadding.roundToPx() }
                            },
                )
                Box(
                    Modifier.fillMaxWidth()
                        .padding(top = IridePageLayoutDefaults.TitleBottomSpacing)
                        .heightIn(min = minimumContentHeight)
                        .onSizeChanged {
                            contentOverflows =
                                it.height >
                                    with(density) {
                                        (minimumContentHeight +
                                                IridePageLayoutDefaults.TitleBottomSpacing)
                                            .roundToPx()
                                    }
                        },
                    content = content,
                )
                if (contentOverflows) {
                    Spacer(
                        Modifier.height(
                            LocalPlayerAwareWindowInsets.current
                                .asPaddingValues()
                                .calculateBottomPadding()
                        )
                    )
                }
            }
        }
    }
}

@Composable
fun iridePageStatusBarTopPadding(): Dp {
    val resources = LocalContext.current.resources
    val density = LocalDensity.current
    val inset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val resourceId =
        remember(resources) {
            resources.getIdentifier("status_bar_height", "dimen", "android")
        }
    val systemHeight =
        if (resourceId != 0) with(density) { resources.getDimension(resourceId).toDp() } else 0.dp
    return maxOf(inset, systemHeight)
}
