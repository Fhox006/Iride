/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.component

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.metrolist.music.R
import com.metrolist.music.constants.LibraryViewType
import com.metrolist.music.ui.theme.IBMPlexMonoFontFamily

@Composable
fun <T> LibrarySortRow(
    sortOptions: List<Pair<T, String>>,
    currentSort: T,
    onSortChange: (T) -> Unit,
    sortDescending: Boolean,
    onSortDescendingChange: (Boolean) -> Unit,
    viewType: LibraryViewType? = null,
    onViewTypeChange: (LibraryViewType) -> Unit = {},
    availableViewTypes: List<LibraryViewType> = LibraryViewType.entries,
    showDescending: Boolean = true,
    verticalPadding: Dp = 6.dp,
    modifier: Modifier = Modifier,
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val currentLabel = sortOptions.firstOrNull { it.first == currentSort }?.second ?: ""

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 0.dp, vertical = verticalPadding),
    ) {
        Box {
            SortMenuChip(
                label = currentLabel,
                expanded = menuExpanded,
                onClick = { menuExpanded = true },
            )
            IrideFolioChoiceMenu(
                expanded = menuExpanded, onDismissRequest = { menuExpanded = false },
                actions = sortOptions.map { (type, label) ->
                    IrideFolioMenuAction(label = label, selected = type == currentSort,
                        onClick = { onSortChange(type) })
                },
            )
        }

        if (showDescending) {
            SortDirectionButton(
                descending = sortDescending,
                onClick = { onSortDescendingChange(!sortDescending) },
            )
        }

        if (viewType != null) {
            Spacer(modifier = Modifier.weight(1f))
            LibraryViewTypeButton(
                viewType = viewType,
                onViewTypeChange = onViewTypeChange,
                availableViewTypes = availableViewTypes,
            )
        }
    }
}

@Composable
private fun SortMenuChip(
    label: String,
    expanded: Boolean,
    onClick: () -> Unit,
) {
    val arrowRotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = tween(200),
        label = "sortMenuArrow",
    )

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier
            .height(32.dp)
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick = onClick,
            ),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium.copy(
                fontFamily = IBMPlexMonoFontFamily,
            ),
            fontWeight = FontWeight.SemiBold,
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Icon(
            painter = painterResource(R.drawable.expand_more),
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier
                .size(16.dp)
                .graphicsLayer { rotationZ = arrowRotation },
        )
    }
}

@Composable
private fun SortDirectionButton(
    descending: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val rotation by animateFloatAsState(
        targetValue = if (descending) 0f else 180f,
        animationSpec = tween(220),
        label = "sortDirection",
    )

    Box(
        modifier = modifier
            .size(32.dp)
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(R.drawable.arrow_downward),
            contentDescription = stringResource(if (descending) R.string.sort_descending else R.string.sort_ascending),
            tint = Color.White,
            modifier = Modifier
                .size(18.dp)
                .graphicsLayer { rotationZ = rotation },
        )
    }
}

@Composable
fun LibraryViewTypeButton(
    viewType: LibraryViewType,
    onViewTypeChange: (LibraryViewType) -> Unit,
    availableViewTypes: List<LibraryViewType> = LibraryViewType.entries,
    modifier: Modifier = Modifier,
    size: Dp = 32.dp,
) {
    val nextViewType = availableViewTypes[(availableViewTypes.indexOf(viewType) + 1) % availableViewTypes.size]
    Box(
        modifier = modifier
            .size(size)
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
            ) { onViewTypeChange(nextViewType) },
        contentAlignment = Alignment.Center,
    ) {
        AnimatedContent(
            targetState = viewType,
            transitionSpec = {
                (fadeIn() + scaleIn(initialScale = 0.7f)) togetherWith
                    (fadeOut() + scaleOut(targetScale = 0.7f))
            },
            label = "libraryViewTypeIcon",
        ) { currentViewType ->
            Icon(
                painter = painterResource(
                    when (currentViewType) {
                        LibraryViewType.LIST -> R.drawable.list
                        LibraryViewType.GRID -> R.drawable.grid_view
                        LibraryViewType.GRID_WIDE -> R.drawable.grid_view_3
                    },
                ),
                contentDescription = stringResource(
                    when (availableViewTypes[(availableViewTypes.indexOf(currentViewType) + 1) % availableViewTypes.size]) {
                        LibraryViewType.LIST -> R.string.switch_to_list_view
                        LibraryViewType.GRID -> R.string.switch_to_grid_view
                        LibraryViewType.GRID_WIDE -> R.string.switch_to_wide_grid_view
                    },
                ),
                tint = Color.White,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}
