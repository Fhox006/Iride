package com.metrolist.music.ui.component

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.metrolist.music.R
import com.metrolist.music.constants.LibraryViewType

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
    menuActions: List<LibraryPageMenuAction> = emptyList(),
    menuSections: List<List<LibraryPageMenuAction>> = emptyList(),
    content: LazyGridScope.() -> Unit,
) {
    val pageModifier = modifier
    IridePageLayout(
        titleRes = titleRes,
        modifier = pageModifier,
        columns = columns,
        state = state,
        bottomPadding = bottomPadding,
        horizontalArrangement = horizontalArrangement,
        verticalArrangement = verticalArrangement,
        showTopBar = true,
        navigationIcon = if (gestureNavigationEnabled() && controls == null) null else ({ LibraryNavigationControls(onNavigateUp, controls) }),
        menuSections = listOf(menuActions) + menuSections,
        content = {
            content()
        },
    )
}

@Composable
fun LibraryListPage(
    @StringRes titleRes: Int,
    state: LazyListState,
    bottomPadding: androidx.compose.ui.unit.Dp,
    onNavigateUp: () -> Unit,
    modifier: Modifier = Modifier,
    contentHorizontalPadding: androidx.compose.ui.unit.Dp = 20.dp,
    titleTopSpacing: androidx.compose.ui.unit.Dp = IridePageLayoutDefaults.ContentTopSpacing,
    showNavigationPill: Boolean = true,
    menuSections: List<List<LibraryPageMenuAction>> = emptyList(),
    overlay: @Composable BoxScope.() -> Unit = {},
    content: LazyListScope.() -> Unit,
) {
    val pageModifier = modifier
    IridePageLayout(
        titleRes = titleRes,
        modifier = pageModifier,
        state = state,
        bottomPadding = bottomPadding,
        contentHorizontalPadding = contentHorizontalPadding,
        titleTopSpacing = titleTopSpacing,
        overlay = overlay,
        showTopBar = showNavigationPill,
        navigationIcon =
            if (showNavigationPill && !gestureNavigationEnabled()) ({ LibraryNavigationControls(onNavigateUp) }) else null,
        menuSections = if (showNavigationPill) menuSections else emptyList(),
        content = {
            content()
        },
    )
}

@Composable
private fun LibraryNavigationControls(
    onNavigateUp: () -> Unit,
    controls: (@Composable () -> Unit)? = null,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (!gestureNavigationEnabled()) IconButton(onClick = onNavigateUp) {
            Icon(
                painterResource(R.drawable.arrow_back),
                stringResource(R.string.back),
                tint = Color.White,
            )
        }
        if (controls != null) {
            if (!gestureNavigationEnabled()) IrideFolioControlDivider()
            controls()
        }
    }
}

typealias LibraryPageMenuAction = IrideFolioMenuAction

@Composable
fun libraryGridMenuActions(
    viewType: LibraryViewType,
    onViewTypeChange: (LibraryViewType) -> Unit,
): List<LibraryPageMenuAction> =
    listOf(
        LibraryPageMenuAction(
            label = stringResource(R.string.library_simple_grid),
            icon = R.drawable.grid_view,
            onClick = { onViewTypeChange(LibraryViewType.GRID) },
            selected = viewType != LibraryViewType.GRID_WIDE,
        ),
        LibraryPageMenuAction(
            label = stringResource(R.string.library_dense_grid),
            icon = R.drawable.library_filter_lines,
            onClick = { onViewTypeChange(LibraryViewType.GRID_WIDE) },
            selected = viewType == LibraryViewType.GRID_WIDE,
        ),
    )

fun <T> librarySortMenuActions(
    sortOptions: List<Pair<T, String>>,
    currentSort: T,
    onSortChange: (T) -> Unit,
): List<LibraryPageMenuAction> = sortOptions.map { (type, label) ->
    LibraryPageMenuAction(
        label = label,
        onClick = { onSortChange(type) },
        selected = type == currentSort,
    )
}
