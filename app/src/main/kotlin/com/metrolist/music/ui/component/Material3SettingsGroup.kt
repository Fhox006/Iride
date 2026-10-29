/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import com.metrolist.music.ui.theme.FolioShape
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import com.metrolist.music.ui.theme.IBMPlexMonoFontFamily
import com.metrolist.music.ui.theme.strokeHairline
import com.metrolist.music.ui.theme.fillSubtle
import com.metrolist.music.ui.theme.textPrimary
import com.metrolist.music.ui.theme.textSecondary
import com.metrolist.music.ui.theme.textTertiary
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.metrolist.music.R

/**
 * A Material 3 Expressive style settings group component
 * @param title The title of the settings group
 * @param items List of settings items to display
 */
@Composable
fun Material3SettingsGroup(
    title: String? = null,
    items: List<Material3SettingsItem>,
    useLowContrast: Boolean = false,
    modifier: Modifier = Modifier
) {
    IrideSettingsGroup(title = title, items = items, modifier = modifier)
}

/** Joined destination rows share one borderless container per group in the settings index. */
@Composable
fun IrideFolioSettingsNavigationGroup(
    items: List<Material3SettingsItem>,
    modifier: Modifier = Modifier,
) {
    IrideSettingsGroup(title = null, items = items, modifier = modifier)
}

/** Settings share the library shuffle action's fill, including custom option layouts. */
@Composable
fun IrideFolioSettingsPanel(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val shape = FolioShape(IrideFolio.SettingsCornerRadius)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(colors.fillSubtle),
        content = content,
    )
}

/** Section labels stay outside the shared panel; joined rows retain individual touch targets. */
@Composable
private fun IrideSettingsGroup(
    title: String?,
    items: List<Material3SettingsItem>,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        title?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontFamily = IBMPlexMonoFontFamily,
                    letterSpacing = (-0.1).sp,
                ),
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.textSecondary,
                modifier = Modifier.padding(bottom = IrideFolio.SettingsTitleBottomSpacing, top = 4.dp)
            )
        }

        if (items.isEmpty()) return@Column
        IrideFolioSettingsPanel {
            items.forEachIndexed { index, item ->
                IrideSettingsItemRow(item = item)
                if (index != items.lastIndex) {
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = IrideFolio.SettingsRowHorizontalPadding),
                        color = MaterialTheme.colorScheme.strokeHairline,
                        thickness = IrideFolio.SettingsBorderWidth,
                    )
                }
            }
        }
    }
}

@Composable
private fun IrideSettingsItemRow(
    item: Material3SettingsItem,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(
        horizontal = IrideFolio.SettingsRowHorizontalPadding,
        vertical = IrideFolio.SettingsRowVerticalPadding,
    ),
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(
                enabled = item.enabled && item.onClick != null,
                onClick = { item.onClick?.invoke() }
            )
            .alpha(if (item.enabled) 1f else 0.4f)
            .padding(contentPadding),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (item.leadingContent != null) {
            item.leadingContent.invoke()
            Spacer(modifier = Modifier.width(16.dp))
        } else if (item.icon != null) {
            val iconTint = MaterialTheme.colorScheme.textPrimary.copy(alpha = if (item.isHighlighted) 1f else 0.85f)
            val icon: @Composable () -> Unit = {
                Icon(
                    painter = item.icon,
                    contentDescription = null,
                    tint = if (item.iconContainerColor != null) Color.White else iconTint,
                    modifier = Modifier.size(22.dp)
                )
            }
            if (item.iconContainerColor != null) {
                IrideIconTile(color = item.iconContainerColor) {
                    if (item.showBadge) {
                        BadgedBox(badge = { Badge(containerColor = MaterialTheme.colorScheme.textPrimary) }) {
                            icon()
                        }
                    } else {
                        icon()
                    }
                }
            } else if (item.showBadge) {
                BadgedBox(
                    badge = { Badge(containerColor = MaterialTheme.colorScheme.error) }
                ) {
                    Icon(
                        painter = item.icon,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(22.dp)
                    )
                }
            } else {
                Icon(
                    painter = item.icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
        }

        Column(modifier = Modifier.weight(1f)) {
            ProvideTextStyle(
                MaterialTheme.typography.bodyLarge.copy(
                    fontFamily = IBMPlexMonoFontFamily,
                    fontSize = 15.sp,
                    letterSpacing = (-0.1).sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.textPrimary,
                )
            ) {
                item.title()
            }

            item.description?.let { desc ->
                Spacer(modifier = Modifier.height(2.dp))
                ProvideTextStyle(
                    MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.textSecondary)
                ) {
                    desc()
                }
            }
        }

        item.trailingContent?.let { trailing ->
            Spacer(modifier = Modifier.width(8.dp))
            trailing()
        }
    }
}

/**
 * A collapsible section within a settings screen.
 * Renders a styled header row that toggles visibility of [content].
 * @param title Label shown in the header row
 * @param defaultExpanded Whether the section starts expanded (default: false)
 */
@Composable
fun ExpandableSettingsSection(
    title: String,
    modifier: Modifier = Modifier,
    defaultExpanded: Boolean = false,
    icon: Painter? = null,
    iconTint: Color? = null,
    description: String = "",
    content: @Composable () -> Unit
) {
    var expanded by remember { mutableStateOf(defaultExpanded) }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
                .padding(vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) {
                Icon(
                    painter = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.textPrimary.copy(alpha = 0.85f),
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontFamily = IBMPlexMonoFontFamily,
                        fontSize = 15.sp,
                        letterSpacing = (-0.1).sp,
                    ),
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.textPrimary
                )
                if (description.isNotEmpty()) {
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.textSecondary
                    )
                }
            }
            Icon(
                painter = painterResource(
                    if (expanded) R.drawable.expand_less else R.drawable.expand_more
                ),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.textTertiary,
                modifier = Modifier.size(18.dp)
            )
        }

        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                content()
            }
        }
    }
}

/**
 * Data class for Material 3 settings item
 */
data class Material3SettingsItem(
    val icon: Painter? = null,
    val leadingContent: (@Composable () -> Unit)? = null,
    val title: @Composable () -> Unit,
    val description: (@Composable () -> Unit)? = null,
    val trailingContent: (@Composable () -> Unit)? = null,
    val showBadge: Boolean = false,
    val isHighlighted: Boolean = false,
    val enabled: Boolean = true,
    val onClick: (() -> Unit)? = null,
    val iconContainerColor: Color? = null,
)
