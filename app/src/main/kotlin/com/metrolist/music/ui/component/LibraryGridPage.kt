package com.metrolist.music.ui.component

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.unit.dp
import com.metrolist.music.R
import com.metrolist.music.constants.LibraryViewType
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
        pageControls = {
            LibraryNavigationPill(LocalScreenFrostBackdrop.current, LocalPageControlsProgress.current, onNavigateUp, controls, menuActions, menuSections)
        },
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
    titleTopSpacing: androidx.compose.ui.unit.Dp =
        IridePageLayoutDefaults.ContentTopSpacing,
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
        pageControls = if (showNavigationPill) {
            {
                LibraryNavigationPill(LocalScreenFrostBackdrop.current, LocalPageControlsProgress.current, onNavigateUp, menuSections = menuSections)
            }
        } else {
            null
        },
        content = {
            content()
        },
    )
}

@Composable
private fun LibraryNavigationPill(
    backdrop: FrostBackdrop?,
    progress: Float,
    onNavigateUp: () -> Unit,
    controls: (@Composable () -> Unit)? = null,
    menuActions: List<LibraryPageMenuAction> = emptyList(),
    menuSections: List<List<LibraryPageMenuAction>> = emptyList(),
) {
    val sections = (listOf(menuActions) + menuSections).filter { it.isNotEmpty() }
    val statusBarPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val shape = androidx.compose.foundation.shape.RoundedCornerShape(18.dp)
    val surface = MaterialTheme.colorScheme.surface
    val borderColor = MaterialTheme.colorScheme.strokeCard
    val revealedBorder = borderColor.copy(alpha = borderColor.alpha * progress)
    var expanded by remember { mutableStateOf(false) }
    Box(Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(top = statusBarPadding + 4.dp, start = IridePageLayoutDefaults.HorizontalPadding)
                .frostedNavigationSurface(backdrop, surface, shape, progress)
                .border(1.dp, revealedBorder, shape)
                .padding(horizontal = 2.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onNavigateUp) {
                Icon(painterResource(R.drawable.arrow_back), stringResource(R.string.back), tint = Color.White)
            }
            if (controls != null) {
                Box(Modifier.width(1.dp).height(20.dp).background(revealedBorder))
                controls()
            }
        }
        if (sections.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = statusBarPadding + 4.dp, end = IridePageLayoutDefaults.HorizontalPadding)
                    .frostedNavigationSurface(backdrop, surface, shape, progress)
                    .border(1.dp, revealedBorder, shape)
                    .padding(horizontal = 2.dp, vertical = 2.dp),
            ) {
                IconButton(onClick = { expanded = true }, modifier = Modifier.size(48.dp)) {
                    Icon(painterResource(R.drawable.more_vert), stringResource(R.string.more_options), tint = Color.White)
                }
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    modifier = Modifier.width(224.dp)
                        .frostedNavigationSurface(backdrop, surface, shape)
                        .border(1.dp, borderColor, shape),
                    offset = androidx.compose.ui.unit.DpOffset(0.dp, 8.dp),
                    shape = shape,
                    containerColor = Color.Transparent,
                    tonalElevation = 0.dp,
                    shadowElevation = 0.dp,
                ) {
                    androidx.compose.foundation.layout.Column(Modifier.padding(2.dp)) {
                        sections.forEachIndexed { index, actions ->
                            if (index > 0) {
                                HorizontalDivider(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 2.dp),
                                    color = borderColor,
                                )
                            }
                            actions.forEach { action ->
                                DropdownMenuItem(
                                    text = { androidx.compose.material3.Text(action.label, color = Color.White) },
                                    leadingIcon = if (action.icon != null) ({
                                        Icon(painterResource(action.icon), contentDescription = null, tint = Color.White)
                                    }) else null,
                                    trailingIcon = if (action.selected) ({
                                        Icon(painterResource(R.drawable.check), contentDescription = null, tint = Color.White)
                                    }) else null,
                                    onClick = {
                                        expanded = false
                                        action.onClick()
                                    },
                                    modifier = Modifier.height(42.dp).semantics { selected = action.selected },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

data class LibraryPageMenuAction(
    val label: String,
    val icon: Int? = null,
    val onClick: () -> Unit,
    val selected: Boolean = false,
)

@Composable
fun libraryGridMenuActions(
    viewType: LibraryViewType,
    onViewTypeChange: (LibraryViewType) -> Unit,
): List<LibraryPageMenuAction> = listOf(
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
