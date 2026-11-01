/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.Painter

/** Integration options share Folio settings panels and monochrome icons. */
@Composable
fun IntegrationCard(
    title: String? = null,
    items: List<IntegrationCardItem>,
) {
    Material3SettingsGroup(
        title = title,
        items = items.map { item ->
            Material3SettingsItem(
                icon = item.icon,
                title = item.title,
                description = item.description,
                trailingContent = item.trailingContent,
                showBadge = item.showBadge,
                isHighlighted = item.isHighlighted,
                onClick = item.onClick,
            )
        },
    )
}

data class IntegrationCardItem(
    val icon: Painter? = null,
    val title: @Composable () -> Unit,
    val description: (@Composable () -> Unit)? = null,
    val trailingContent: (@Composable () -> Unit)? = null,
    val showBadge: Boolean = false,
    val isHighlighted: Boolean = false,
    val onClick: (() -> Unit)? = null,
)
