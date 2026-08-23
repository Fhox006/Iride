/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.metrolist.music.R
import com.metrolist.music.db.entities.PlaylistCategoryWithCount
import com.metrolist.music.ui.theme.SpaceMonoFontFamily
import com.metrolist.music.ui.theme.textPrimary
import com.metrolist.music.ui.theme.textTertiary

/** Filter state for the persisted, user-created playlist category pills (see [CategoryPillsRow]). */
data class CategoryFilterState(
    val categories: List<PlaylistCategoryWithCount>,
    val songCategoryIds: Map<String, List<String>>,
    val selectedCategoryId: String?,
    val onSelect: (String) -> Unit,
) {
    fun matches(songId: String): Boolean =
        selectedCategoryId == null || songCategoryIds[songId]?.contains(selectedCategoryId) == true
}

/**
 * Pill row for user-created playlist categories. Unlike [GenrePillsRow] this data is a persisted
 * Room table, not fetched from an external API, so there is no loading/skeleton state — the row
 * renders whatever [state] already holds the instant it's composed.
 */
@Composable
fun CategoryPillsRow(
    state: CategoryFilterState,
    onAddClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(20.dp),
        contentPadding = PaddingValues(start = 12.dp, end = 12.dp),
        modifier = modifier,
    ) {
        item(key = "add_category_pill") {
            AddCategoryPill(onClick = onAddClick)
        }
        items(state.categories, key = { it.category.id }) { entry ->
            CategoryPill(
                name = entry.category.name,
                colorHex = entry.category.colorHex,
                selected = state.selectedCategoryId == entry.category.id,
                onClick = { state.onSelect(entry.category.id) },
                modifier = Modifier.animateItem(),
            )
        }
    }
}

@Composable
private fun AddCategoryPill(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = modifier
            .heightIn(min = 48.dp)
            .widthIn(min = 48.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                role = Role.Button,
            ) { onClick() }
            .padding(horizontal = 8.dp),
    ) {
        Text(
            text = "+",
            style = MaterialTheme.typography.labelMedium.copy(
                fontFamily = SpaceMonoFontFamily,
                letterSpacing = 0.5.sp,
            ),
            color = MaterialTheme.colorScheme.textPrimary,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = stringResource(R.string.category_add),
            style = MaterialTheme.typography.labelMedium.copy(
                fontFamily = SpaceMonoFontFamily,
                letterSpacing = 0.5.sp,
            ),
            color = MaterialTheme.colorScheme.textPrimary,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun CategoryPill(
    name: String,
    colorHex: String?,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    var textWidthPx by remember(name) { mutableStateOf(0) }
    val textColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.textPrimary else MaterialTheme.colorScheme.textTertiary,
        animationSpec = spring(stiffness = 400f),
        label = "categoryPillTextColor",
    )
    val underlineAlpha by animateFloatAsState(
        targetValue = if (selected) 1f else 0f,
        animationSpec = spring(stiffness = 400f),
        label = "categoryPillUnderlineAlpha",
    )
    val dotColor = remember(colorHex) { parseCategoryColor(colorHex) }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier
            .heightIn(min = 48.dp)
            .widthIn(min = 48.dp)
            .selectable(
                selected = selected,
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                role = Role.Tab,
                onClick = onClick,
            )
            .padding(horizontal = 8.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            if (dotColor != null) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(dotColor),
                )
            }
            Text(
                text = name,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontFamily = SpaceMonoFontFamily,
                    letterSpacing = 0.5.sp,
                ),
                color = textColor,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                maxLines = 1,
                modifier = Modifier.onSizeChanged { textWidthPx = it.width },
            )
        }
        Spacer(Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .height(2.dp)
                .width(with(density) { textWidthPx.toDp() })
                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = underlineAlpha)),
        )
    }
}

/** Preset swatches offered when creating a category — no color-picker UI exists in the app yet. */
val CategoryColorPresets = listOf(
    "#FFFFFF", "#EF4444", "#F97316", "#EAB308", "#22C55E", "#14B8A6", "#3B82F6", "#A855F7", "#EC4899",
)

fun parseCategoryColor(colorHex: String?): Color? =
    colorHex?.let { runCatching { Color(android.graphics.Color.parseColor(it)) }.getOrNull() }
