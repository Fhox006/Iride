/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.component

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.metrolist.music.R
import com.metrolist.music.constants.LibraryViewType

@Composable
fun <T> LibrarySortRow(
    sortOptions: List<Pair<T, String>>,
    currentSort: T,
    onSortChange: (T) -> Unit,
    sortDescending: Boolean,
    onSortDescendingChange: (Boolean) -> Unit,
    viewType: LibraryViewType,
    onViewTypeChange: (LibraryViewType) -> Unit,
    modifier: Modifier = Modifier,
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val currentLabel = sortOptions.firstOrNull { it.first == currentSort }?.second ?: ""
    val defaultSort = sortOptions.firstOrNull()?.first
    val isDefault = defaultSort == null || currentSort == defaultSort

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 6.dp),
    ) {
        Box {
            SortMenuChip(
                label = currentLabel,
                expanded = menuExpanded,
                isDefault = isDefault,
                onOpen = { menuExpanded = true },
                onReset = {
                    defaultSort?.let { onSortChange(it) }
                    menuExpanded = false
                },
            )
            DropdownMenu(
                expanded = menuExpanded,
                onDismissRequest = { menuExpanded = false },
                shape = MaterialTheme.shapes.extraLarge,
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            ) {
                sortOptions.forEach { (type, label) ->
                    val isSelected = type == currentSort
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onSortChange(type)
                                menuExpanded = false
                            },
                        color = if (isSelected) MaterialTheme.colorScheme.secondaryContainer
                                else Color.Transparent,
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (isSelected) MaterialTheme.colorScheme.onSecondaryContainer
                                        else MaterialTheme.colorScheme.onSurface,
                            )
                            if (isSelected) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .background(
                                            color = MaterialTheme.colorScheme.primary,
                                            shape = CircleShape,
                                        )
                                )
                            }
                        }
                    }
                }
            }
        }

        SortDirectionButton(
            descending = sortDescending,
            onClick = { onSortDescendingChange(!sortDescending) },
        )

        Spacer(modifier = Modifier.weight(1f))

        AlbumViewTypeButton(
            viewType = viewType,
            onViewTypeChange = onViewTypeChange,
        )
    }
}

@Composable
private fun SortMenuChip(
    label: String,
    expanded: Boolean,
    isDefault: Boolean,
    onOpen: () -> Unit,
    onReset: () -> Unit,
) {
    val arrowRotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = tween(200),
        label = "sortMenuArrow",
    )

    val containerColor = if (isDefault) MaterialTheme.colorScheme.surfaceContainerHigh
                         else MaterialTheme.colorScheme.secondaryContainer
    val contentColor = if (isDefault) MaterialTheme.colorScheme.onSurfaceVariant
                       else MaterialTheme.colorScheme.onSecondaryContainer

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier
            .height(32.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(containerColor)
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick = onOpen,
            )
            .padding(horizontal = 12.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = contentColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (isDefault) {
            Icon(
                painter = painterResource(R.drawable.expand_more),
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier
                    .size(16.dp)
                    .graphicsLayer { rotationZ = arrowRotation },
            )
        } else {
            Box(
                modifier = Modifier
                    .size(18.dp)
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() },
                        onClick = onReset,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(R.drawable.close),
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(14.dp),
                )
            }
        }
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
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(R.drawable.arrow_downward),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .size(16.dp)
                .graphicsLayer { rotationZ = rotation },
        )
    }
}

@Composable
fun AlbumViewTypeButton(
    viewType: LibraryViewType,
    onViewTypeChange: (LibraryViewType) -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 32.dp,
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(size / 2))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
            ) { onViewTypeChange(viewType.toggle()) },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(
                when (viewType) {
                    LibraryViewType.LIST -> R.drawable.list
                    LibraryViewType.GRID -> R.drawable.grid_view
                    LibraryViewType.GRID_WIDE -> R.drawable.grid_view_3
                },
            ),
            contentDescription = stringResource(
                when (viewType) {
                    LibraryViewType.LIST -> R.string.switch_to_grid_view
                    LibraryViewType.GRID -> R.string.switch_to_wide_grid_view
                    LibraryViewType.GRID_WIDE -> R.string.switch_to_list_view
                },
            ),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(16.dp),
        )
    }
}
