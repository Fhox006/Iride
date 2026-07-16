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
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.metrolist.music.R
import com.metrolist.music.constants.LibraryViewType
import com.metrolist.music.ui.theme.SpaceMonoFontFamily

@Composable
fun <T> LibrarySortRow(
    sortOptions: List<Pair<T, String>>,
    currentSort: T,
    onSortChange: (T) -> Unit,
    sortDescending: Boolean,
    onSortDescendingChange: (Boolean) -> Unit,
    viewType: LibraryViewType? = null,
    onViewTypeChange: (LibraryViewType) -> Unit = {},
    showDescending: Boolean = true,
    modifier: Modifier = Modifier,
    useIrideStyle: Boolean = false,
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val currentLabel = sortOptions.firstOrNull { it.first == currentSort }?.second ?: ""

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(if (useIrideStyle) 12.dp else 4.dp),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = if (useIrideStyle) 0.dp else 4.dp, vertical = 6.dp),
    ) {
        Box {
            SortMenuChip(
                label = currentLabel,
                expanded = menuExpanded,
                onClick = { menuExpanded = true },
                useIrideStyle = useIrideStyle,
            )
            DropdownMenu(
                expanded = menuExpanded,
                onDismissRequest = { menuExpanded = false },
                shape = RoundedCornerShape(16.dp),
                containerColor = if (useIrideStyle) {
                    androidx.compose.ui.graphics.Color(0xFF111111)
                } else {
                    MaterialTheme.colorScheme.surfaceContainerHigh
                },
            ) {
                sortOptions.forEach { (type, label) ->
                    val isSelected = type == currentSort
                    if (useIrideStyle) {
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelLarge.copy(
                                        fontFamily = SpaceMonoFontFamily,
                                    ),
                                    fontWeight = if (isSelected) androidx.compose.ui.text.font.FontWeight.SemiBold
                                                 else androidx.compose.ui.text.font.FontWeight.Normal,
                                    color = androidx.compose.ui.graphics.Color.White.copy(alpha = if (isSelected) 0.95f else 0.7f),
                                )
                            },
                            trailingIcon = if (isSelected) {
                                {
                                    Icon(
                                        painter = painterResource(R.drawable.check),
                                        contentDescription = null,
                                        tint = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.85f),
                                        modifier = Modifier.size(18.dp),
                                    )
                                }
                            } else null,
                            onClick = {
                                onSortChange(type)
                                menuExpanded = false
                            },
                        )
                    } else {
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.bodyMedium,
                                    textDecoration = if (isSelected) TextDecoration.Underline
                                                     else TextDecoration.None,
                                )
                            },
                            onClick = {
                                onSortChange(type)
                                menuExpanded = false
                            },
                        )
                    }
                }
            }
        }

        if (showDescending) {
            SortDirectionButton(
                descending = sortDescending,
                onClick = { onSortDescendingChange(!sortDescending) },
                useIrideStyle = useIrideStyle,
            )
        }

        if (viewType != null) {
            Spacer(modifier = Modifier.weight(1f))
            LibraryViewTypeButton(
                viewType = viewType,
                onViewTypeChange = onViewTypeChange,
                useIrideStyle = useIrideStyle,
            )
        }
    }
}

@Composable
private fun SortMenuChip(
    label: String,
    expanded: Boolean,
    onClick: () -> Unit,
    useIrideStyle: Boolean = false,
) {
    val arrowRotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = tween(200),
        label = "sortMenuArrow",
    )

    if (useIrideStyle) {
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
                    fontFamily = SpaceMonoFontFamily,
                ),
                fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.85f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Icon(
                painter = painterResource(R.drawable.expand_more),
                contentDescription = null,
                tint = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.5f),
                modifier = Modifier
                    .size(16.dp)
                    .graphicsLayer { rotationZ = arrowRotation },
            )
        }
        return
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier
            .height(32.dp)
            .clip(RoundedCornerShape(50))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick = onClick,
            )
            .padding(horizontal = 12.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Icon(
            painter = painterResource(R.drawable.expand_more),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
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
    useIrideStyle: Boolean = false,
) {
    val rotation by animateFloatAsState(
        targetValue = if (descending) 0f else 180f,
        animationSpec = tween(220),
        label = "sortDirection",
    )

    if (useIrideStyle) {
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
                tint = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.75f),
                modifier = Modifier
                    .size(18.dp)
                    .graphicsLayer { rotationZ = rotation },
            )
        }
        return
    }

    Box(
        modifier = modifier
            .size(32.dp)
            .clip(RoundedCornerShape(50))
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
            contentDescription = stringResource(if (descending) R.string.sort_descending else R.string.sort_ascending),
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .size(16.dp)
                .graphicsLayer { rotationZ = rotation },
        )
    }
}

@Composable
fun LibraryViewTypeButton(
    viewType: LibraryViewType,
    onViewTypeChange: (LibraryViewType) -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 32.dp,
    useIrideStyle: Boolean = false,
) {
    if (useIrideStyle) {
        Box(
            modifier = modifier
                .size(size)
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
                tint = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.75f),
                modifier = Modifier.size(18.dp),
            )
        }
        return
    }

    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(50))
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
