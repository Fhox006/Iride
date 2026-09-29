package com.metrolist.music.ui.component

import androidx.annotation.StringRes
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.metrolist.music.R
import com.metrolist.music.ui.theme.strokeCard

/** Shared visual shell for Album and Playlist library grids. */
@Composable
fun LibraryGridPage(
    @StringRes titleRes: Int,
    state: LazyGridState,
    columns: GridCells,
    bottomPadding: androidx.compose.ui.unit.Dp,
    onNavigateUp: () -> Unit,
    modifier: Modifier = Modifier,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.spacedBy(12.dp),
    verticalArrangement: Arrangement.Vertical = Arrangement.spacedBy(8.dp),
    controls: (@Composable () -> Unit)? = null,
    playbackActions: (@Composable () -> Unit)? = null,
    menuActions: List<LibraryPageMenuAction> = emptyList(),
    content: LazyGridScope.() -> Unit,
) {
    val backdrop = rememberFrostBackdrop()
    val controlsProgress by animateFloatAsState(
        targetValue = if (state.firstVisibleItemIndex > 0) 1f else 0f,
        animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing),
        label = "libraryControlsReveal",
    )
    Box(modifier = Modifier.fillMaxSize()) {
        IridePageLayout(
            titleRes = titleRes,
            modifier = modifier.recordFrostBackdrop(backdrop),
            columns = columns,
            state = state,
            titleTopSpacing = IridePageLayoutDefaults.BarHeight + IridePageLayoutDefaults.ContentTopSpacing + 16.dp,
            bottomPadding = bottomPadding,
            horizontalArrangement = horizontalArrangement,
            verticalArrangement = verticalArrangement,
            content = {
                if (playbackActions != null) {
                    item(key = "library_playback_actions", span = { GridItemSpan(maxLineSpan) }) {
                        playbackActions()
                    }
                }
                content()
            },
        )
        LibraryNavigationPill(backdrop, controlsProgress, onNavigateUp, controls, menuActions)
    }
}

@Composable
fun LibraryListPage(
    @StringRes titleRes: Int,
    state: LazyListState,
    bottomPadding: androidx.compose.ui.unit.Dp,
    onNavigateUp: () -> Unit,
    modifier: Modifier = Modifier,
    contentHorizontalPadding: androidx.compose.ui.unit.Dp = 20.dp,
    titleTopSpacing: androidx.compose.ui.unit.Dp =
        IridePageLayoutDefaults.BarHeight + IridePageLayoutDefaults.ContentTopSpacing + 16.dp,
    showNavigationPill: Boolean = true,
    playbackActions: (@Composable () -> Unit)? = null,
    overlay: @Composable BoxScope.() -> Unit = {},
    content: LazyListScope.() -> Unit,
) {
    val backdrop = rememberFrostBackdrop()
    val controlsProgress by animateFloatAsState(
        targetValue = if (state.firstVisibleItemIndex > 0) 1f else 0f,
        animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing),
        label = "libraryControlsReveal",
    )
    Box(modifier = Modifier.fillMaxSize()) {
        IridePageLayout(
            titleRes = titleRes,
            modifier = modifier.recordFrostBackdrop(backdrop),
            state = state,
            bottomPadding = bottomPadding,
            contentHorizontalPadding = contentHorizontalPadding,
            titleTopSpacing = titleTopSpacing,
            overlay = overlay,
            content = {
                if (playbackActions != null) {
                    item(key = "library_playback_actions") { playbackActions() }
                }
                content()
            },
        )
        if (showNavigationPill) {
            LibraryNavigationPill(backdrop, controlsProgress, onNavigateUp)
        }
    }
}

@Composable
private fun LibraryNavigationPill(
    backdrop: FrostBackdrop?,
    progress: Float,
    onNavigateUp: () -> Unit,
    controls: (@Composable () -> Unit)? = null,
    menuActions: List<LibraryPageMenuAction> = emptyList(),
) {
    val statusBarPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val shape = androidx.compose.foundation.shape.RoundedCornerShape(18.dp)
    val baseStroke = MaterialTheme.colorScheme.strokeCard
    val stroke = baseStroke.copy(alpha = baseStroke.alpha * progress)
    Box(Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(top = statusBarPadding + 4.dp, start = IridePageLayoutDefaults.HorizontalPadding)
                .frostedNavigationSurface(backdrop, MaterialTheme.colorScheme.surface, shape, progress)
                .border(1.dp, stroke, shape)
                .padding(horizontal = 2.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onNavigateUp) {
                Icon(painterResource(R.drawable.arrow_back), stringResource(R.string.back), tint = Color.White)
            }
            if (controls != null) {
                Box(Modifier.width(1.dp).height(20.dp).background(stroke))
                controls()
            }
        }
        if (menuActions.isNotEmpty()) {
            var expanded by remember { mutableStateOf(false) }
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = statusBarPadding + 4.dp, end = IridePageLayoutDefaults.HorizontalPadding)
                    .frostedNavigationSurface(backdrop, MaterialTheme.colorScheme.surface, shape, progress)
                    .border(1.dp, stroke, shape)
                    .padding(horizontal = 2.dp, vertical = 2.dp),
            ) {
                IconButton(onClick = { expanded = true }, modifier = Modifier.size(44.dp)) {
                    Icon(painterResource(R.drawable.more_vert), stringResource(R.string.more_options), tint = Color.White)
                }
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    menuActions.forEach { action ->
                        DropdownMenuItem(
                            text = { androidx.compose.material3.Text(action.label) },
                            leadingIcon = { Icon(painterResource(action.icon), contentDescription = null) },
                            onClick = {
                                expanded = false
                                action.onClick()
                            },
                        )
                    }
                }
            }
        }
    }
}

data class LibraryPageMenuAction(
    val label: String,
    val icon: Int,
    val onClick: () -> Unit,
)
