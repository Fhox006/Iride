package com.metrolist.music.ui.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.metrolist.music.R

internal fun folioContextActions(items: List<Material3MenuItemData>): List<Material3MenuItemData> =
    folioContextItems(items, actionId = { it.actionId }, section = { it.section })

@Composable
internal fun folioMenuAction(
    label: Int,
    icon: Int,
    onClick: () -> Unit,
    text: String? = null,
): Material3MenuItemData =
    Material3MenuItemData(
        actionId = label,
        title = {
            androidx.compose.material3.Text(text ?: androidx.compose.ui.res.stringResource(label))
        },
        icon = {
            androidx.compose.material3.Icon(androidx.compose.ui.res.painterResource(icon), null)
        },
        onClick = onClick,
    )

@Composable
internal fun folioLibraryAction(saved: Boolean, onClick: () -> Unit): Material3MenuItemData =
    folioMenuAction(
        label = R.string.add_to_library,
        icon = if (saved) R.drawable.favorite else R.drawable.favorite_border,
        onClick = onClick,
        text = stringResource(if (saved) R.string.remove_from_library else R.string.add_to_library),
    )

/**
 * Dialog state stays in the caller; hiding the popup never disposes the operation or its coroutine.
 */
@Composable
internal fun IrideFolioContextMenu(
    items: List<Material3MenuItemData>,
    visible: Boolean = true,
    onDismiss: () -> Unit,
    retainOpenActions: Set<Int> = emptySet(),
    popupOverride: MenuState? = null,
    anchorOverride: androidx.compose.ui.geometry.Rect? = null,
    focusable: Boolean = true,
) {
    val actions = folioContextActions(items)
    val sections =
        actions.groupBy { it.section ?: folioActionOrder(it.actionId).first }.values.toList()
    val scrollState = rememberLazyListState()
    val density = LocalDensity.current
    val height =
        with(density) {
            actions.size * IrideFolio.MenuItemHeight.roundToPx() +
                (sections.size - 1).coerceAtLeast(0) *
                    (IrideFolio.MenuSectionVerticalPadding.roundToPx() * 2 +
                        IrideFolio.MenuDividerThickness.roundToPx()) +
                (IrideFolio.MenuVerticalPadding.roundToPx() +
                    IrideFolio.MenuBorderWidth.roundToPx()) * 2
        }
    val content: @Composable (Boolean) -> Unit = { constrained ->
        Box {
            LazyColumn(
                state = scrollState,
                overscrollEffect = null,
                modifier =
                    Modifier.padding(
                        end = if (constrained) IrideFolio.ScrollbarTouchTargetWidth else 0.dp
                    ).irideFolioGestures(scrollState),
                contentPadding =
                    PaddingValues(
                        horizontal = IrideFolio.MenuContentPadding,
                        vertical = IrideFolio.MenuVerticalPadding,
                    ),
            ) {
                sections.forEachIndexed { index, section ->
                    if (index > 0) item { IrideFolioMenuDivider() }
                    items(section) { action ->
                        IrideFolioMenuItem(
                            text = action.title,
                            enabled = action.onClick != null,
                            selected = action.selected,
                            leadingIcon =
                                action.icon?.let { icon ->
                                    {
                                        Box(
                                            Modifier.size(24.dp),
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            icon()
                                        }
                                    }
                                },
                            onClick = {
                                action.onClick?.invoke()
                                if (action.actionId !in retainOpenActions) onDismiss()
                            },
                        )
                    }
                }
            }
            if (constrained)
                Box(Modifier.matchParentSize(), contentAlignment = Alignment.CenterEnd) {
                    IrideFolioScrollbar(scrollState, Modifier.fillMaxHeight())
                }
        }
    }
    if (visible && actions.isNotEmpty()) {
        val popup = popupOverride ?: LocalSongMenuPopupState.current
        if (popup != null)
            IrideAnchoredSongMenu(
                popup,
                height,
                anchorBounds = anchorOverride ?: popup.popupAnchor,
                onDismissRequest = onDismiss,
                focusable = focusable,
            ) {
                content(it)
            }
        else content(false)
    }
}

@Composable
internal fun folioMenuArtwork(thumbnailUrl: String?, portrait: Boolean, fallbackIcon: Int) {
    if (thumbnailUrl.isNullOrBlank()) {
        androidx.compose.material3.Icon(androidx.compose.ui.res.painterResource(fallbackIcon), null)
    } else {
        AsyncImage(
            model = thumbnailUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier =
                Modifier.size(24.dp).clip(if (portrait) CircleShape else RoundedCornerShape(4.dp)),
        )
    }
}
