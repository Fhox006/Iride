package com.metrolist.music.ui.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.metrolist.music.R
import com.metrolist.music.ui.theme.SpaceMonoFontFamily
import com.metrolist.music.ui.theme.textPrimary
import com.metrolist.music.ui.theme.textSecondary
import com.metrolist.music.ui.utils.pressScale

/** Category, context label, card title and detail inherit the same Folio scale. */
@Composable
fun irideFolioShelfTypography(): Typography = MaterialTheme.typography.let { type ->
    type.copy(
        titleMedium = type.titleMedium.copy(
            fontFamily = SpaceMonoFontFamily, fontSize = IrideFolio.ShelfTitleFontSize,
            lineHeight = IrideFolio.ShelfTitleFontSize * 1.35f,
        ),
        bodyLarge = type.bodyLarge.copy(
            fontFamily = SpaceMonoFontFamily, fontSize = IrideFolio.ShelfItemFontSize,
            lineHeight = IrideFolio.ShelfItemFontSize * 1.4f,
        ),
        titleSmall = type.titleSmall.copy(
            fontFamily = SpaceMonoFontFamily, fontSize = IrideFolio.ShelfItemFontSize,
            lineHeight = IrideFolio.ShelfItemFontSize * 1.4f,
        ),
        bodyMedium = type.bodyMedium.copy(
            fontFamily = SpaceMonoFontFamily, fontSize = IrideFolio.ShelfDetailFontSize,
            lineHeight = IrideFolio.ShelfDetailFontSize * 1.4f,
        ),
        bodySmall = type.bodySmall.copy(
            fontFamily = SpaceMonoFontFamily, fontSize = IrideFolio.ShelfDetailFontSize,
            lineHeight = IrideFolio.ShelfDetailFontSize * 1.4f,
        ),
        labelLarge = type.labelLarge.copy(
            fontFamily = SpaceMonoFontFamily, fontSize = IrideFolio.ShelfLabelFontSize,
            lineHeight = IrideFolio.ShelfLabelFontSize * 1.4f,
        ),
    )
}

@Composable
fun IrideFolioShelfHeader(
    title: String,
    modifier: Modifier = Modifier,
    label: String? = null,
    supportingText: String? = null,
    thumbnail: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null,
    onPlayAllClick: (() -> Unit)? = null,
    onRefreshClick: (() -> Unit)? = null,
    isRefreshing: Boolean = false,
    showNavigationArrow: Boolean = false,
    titleMaxLines: Int = 2,
    trailingContent: (@Composable () -> Unit)? = null,
) {
    Column(modifier) {
        NavigationTitle(
            title = title,
            label = label,
            thumbnail = thumbnail,
            onClick = onClick,
            onPlayAllClick = onPlayAllClick,
            onRefreshClick = onRefreshClick,
            isRefreshing = isRefreshing,
            useIrideStyle = true,
            prominentTitle = true,
            showDivider = false,
            topPadding = IrideFolio.ShelfTopPadding,
            bottomPadding = if (supportingText == null) IrideFolio.ShelfBottomPadding else 4.dp,
            showNavigationArrow = showNavigationArrow,
            titleMaxLines = titleMaxLines,
            trailingContent = trailingContent,
        )
        if (supportingText != null) {
            Text(
                supportingText,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontFamily = SpaceMonoFontFamily,
                    fontSize = IrideFolio.ShelfDetailFontSize,
                    lineHeight = IrideFolio.ShelfDetailFontSize * 1.4f,
                ),
                color = MaterialTheme.colorScheme.textSecondary,
                modifier = Modifier.padding(horizontal = IrideFolio.HorizontalPadding)
                    .padding(bottom = IrideFolio.ShelfBottomPadding),
            )
        }
    }
}

@Composable
fun IrideFolioShelfSectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleLarge.copy(
            fontFamily = SpaceMonoFontFamily,
            fontSize = IrideFolio.ShelfSectionFontSize,
            lineHeight = IrideFolio.ShelfSectionFontSize * 1.3f,
            fontWeight = FontWeight.Bold,
        ),
        color = MaterialTheme.colorScheme.textPrimary,
        modifier = Modifier.padding(horizontal = IrideFolio.HorizontalPadding)
            .padding(top = IrideFolio.ShelfTopPadding),
    )
}

/** An anchored choice control that belongs to scrolling content, using the overflow popup. */
@Composable
fun IrideFolioInlineSelector(
    label: String,
    sections: List<List<IrideFolioMenuAction>>,
) {
    var expanded by remember { mutableStateOf(false) }
    val interactions = remember { MutableInteractionSource() }
    Box {
        IrideFolioCompactControls {
            Row(
                modifier = Modifier.height(IrideFolio.ControlSurfaceSize)
                    .widthIn(max = IrideFolio.ShelfSelectorMaxWidth)
                    .irideFolioControlSurface(1f, backdrop = null, forceArtworkSurface = false)
                    .pressScale(interactions)
                    .clickable(interactionSource = interactions, indication = null, role = Role.Button) { expanded = !expanded }
                    .padding(horizontal = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    label,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontFamily = SpaceMonoFontFamily,
                        fontSize = IrideFolio.ShelfItemFontSize,
                        lineHeight = IrideFolio.ShelfItemFontSize * 1.4f,
                    ),
                    modifier = Modifier.weight(1f, fill = false),
                )
                Icon(painterResource(R.drawable.expand_more), null, Modifier.size(IrideFolio.ShelfSelectorIconSize))
            }
        }
        IrideFolioChoiceMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            actions = emptyList(),
            sections = sections.map { actions ->
                actions.map { action -> action.copy(onClick = {
                    expanded = false
                    action.onClick()
                }) }
            },
        )
    }
}
