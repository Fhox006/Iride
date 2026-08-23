/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.metrolist.music.R
import com.metrolist.music.db.entities.PlaylistCategoryWithCount
import com.metrolist.music.ui.screens.search.IrideSearchBox
import com.metrolist.music.ui.theme.SpaceMonoFontFamily
import com.metrolist.music.ui.theme.fillSelected
import com.metrolist.music.ui.theme.textPrimary
import com.metrolist.music.ui.theme.textSecondary
import com.metrolist.music.ui.theme.textTertiary

/**
 * "Add to category" bottom sheet (New Iride UI selection-mode flow): search, pick one or more
 * existing categories (or create a new one inline), confirm with one sticky button at the bottom.
 * Roughly 70% of the screen height per spec, monospace/flat to match [BottomSheetMenu].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddToCategorySheet(
    isVisible: Boolean,
    onDismissRequest: () -> Unit,
    categories: List<PlaylistCategoryWithCount>,
    onCreateCategory: (name: String, colorHex: String?) -> String,
    onConfirm: (selectedCategoryIds: List<String>) -> Unit,
) {
    var query by rememberSaveable(stateSaver = TextFieldValue.Saver) { mutableStateOf(TextFieldValue()) }
    val selectedIds = remember { mutableStateOf(setOf<String>()) }
    var showCreateDialog by rememberSaveable { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }

    if (!isVisible && (query.text.isNotEmpty() || selectedIds.value.isNotEmpty())) {
        query = TextFieldValue()
        selectedIds.value = emptySet()
    }

    val filteredCategories = remember(categories, query.text) {
        if (query.text.isEmpty()) {
            categories
        } else {
            categories.filter { it.category.name.contains(query.text, ignoreCase = true) }
        }
    }

    fun toggle(categoryId: String) {
        selectedIds.value = if (categoryId in selectedIds.value) {
            selectedIds.value - categoryId
        } else {
            selectedIds.value + categoryId
        }
    }

    AnimatedBottomSheet(
        isVisible = isVisible,
        onDismissRequest = onDismissRequest,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        contentColor = MaterialTheme.colorScheme.textPrimary,
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .size(width = 40.dp, height = 4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)),
            )
        },
        modifier = Modifier.fillMaxHeight(fraction = 0.7f),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
        ) {
            Text(
                text = stringResource(R.string.add_to_category),
                style = MaterialTheme.typography.titleLarge.copy(
                    fontFamily = SpaceMonoFontFamily,
                    fontWeight = FontWeight.Bold,
                ),
                color = MaterialTheme.colorScheme.textPrimary,
            )
            Spacer(Modifier.height(12.dp))
            IrideSearchBox(
                query = query,
                onQueryChange = { query = it },
                placeholderText = stringResource(R.string.search_categories),
                focusRequester = focusRequester,
                onFocusChanged = {},
                onSearch = {},
                onClear = { query = TextFieldValue() },
            )
            Spacer(Modifier.height(8.dp))
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(filteredCategories, key = { it.category.id }) { entry ->
                    CategoryRow(
                        entry = entry,
                        selected = entry.category.id in selectedIds.value,
                        onToggle = { toggle(entry.category.id) },
                    )
                }
                item(key = "new_category_row") {
                    NewCategoryRow(onClick = { showCreateDialog = true })
                }
            }
            Spacer(Modifier.height(12.dp))
            val enabled = selectedIds.value.isNotEmpty()
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp)
                    .height(52.dp)
                    .clip(RoundedCornerShape(percent = 50))
                    .background(if (enabled) MaterialTheme.colorScheme.textPrimary else MaterialTheme.colorScheme.fillSelected)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        enabled = enabled,
                        role = Role.Button,
                    ) { onConfirm(selectedIds.value.toList()) },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(R.string.action_add),
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontFamily = SpaceMonoFontFamily,
                        fontWeight = FontWeight.Bold,
                    ),
                    color = if (enabled) MaterialTheme.colorScheme.background else MaterialTheme.colorScheme.textTertiary,
                )
            }
        }
    }

    if (showCreateDialog) {
        CreateCategoryDialog(
            onDismiss = { showCreateDialog = false },
            onCreate = { name, colorHex ->
                val newId = onCreateCategory(name, colorHex)
                selectedIds.value = selectedIds.value + newId
                showCreateDialog = false
            },
        )
    }
}

@Composable
private fun CategoryRow(
    entry: PlaylistCategoryWithCount,
    selected: Boolean,
    onToggle: () -> Unit,
) {
    val dotColor = remember(entry.category.colorHex) { parseCategoryColor(entry.category.colorHex) }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .selectable(
                selected = selected,
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                role = Role.Checkbox,
                onClick = onToggle,
            )
            .padding(vertical = 10.dp),
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(dotColor ?: MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f)),
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text = entry.category.name,
            style = MaterialTheme.typography.bodyLarge.copy(fontFamily = SpaceMonoFontFamily),
            color = MaterialTheme.colorScheme.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = pluralStringResource(R.plurals.n_song, entry.songCount, entry.songCount),
            style = MaterialTheme.typography.bodySmall.copy(fontFamily = SpaceMonoFontFamily),
            color = MaterialTheme.colorScheme.textSecondary,
        )
        Spacer(Modifier.width(12.dp))
        SelectionIndicator(selected = selected, onClick = onToggle, size = 20.dp)
    }
}

@Composable
private fun NewCategoryRow(onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                role = Role.Button,
                onClick = onClick,
            )
            .padding(vertical = 12.dp),
    ) {
        Icon(
            painter = painterResource(R.drawable.add),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.textPrimary,
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text = stringResource(R.string.new_category),
            style = MaterialTheme.typography.bodyLarge.copy(fontFamily = SpaceMonoFontFamily),
            color = MaterialTheme.colorScheme.textPrimary,
        )
    }
}

@Composable
private fun CreateCategoryDialog(
    onDismiss: () -> Unit,
    onCreate: (name: String, colorHex: String?) -> Unit,
) {
    var selectedColor by remember { mutableStateOf(CategoryColorPresets.first()) }

    TextFieldDialog(
        icon = { Icon(painter = painterResource(R.drawable.add), contentDescription = null) },
        title = { Text(text = stringResource(R.string.new_category)) },
        placeholder = { Text(text = stringResource(R.string.category_name_hint)) },
        onDismiss = onDismiss,
        onDone = { name -> onCreate(name, selectedColor) },
        extraContent = {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
            ) {
                CategoryColorPresets.forEach { hex ->
                    val color = parseCategoryColor(hex) ?: Color.White
                    val selected = hex == selectedColor
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .selectable(
                                selected = selected,
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                role = Role.RadioButton,
                                onClick = { selectedColor = hex },
                            )
                            .size(32.dp)
                            .border(
                                width = if (selected) 2.dp else 0.dp,
                                color = if (selected) MaterialTheme.colorScheme.textPrimary else Color.Transparent,
                                shape = CircleShape,
                            )
                            .padding(4.dp)
                            .clip(CircleShape)
                            .background(color),
                    ) {}
                }
            }
        },
    )
}
