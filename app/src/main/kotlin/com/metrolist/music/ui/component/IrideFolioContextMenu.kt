package com.metrolist.music.ui.component

import androidx.compose.foundation.shape.CircleShape

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import com.metrolist.music.ui.theme.FolioShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
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
        .map { action ->
            val title =
                when (action.actionId) {
                    R.string.play_next -> R.string.iride_menu_next
                    R.string.add_to_queue -> R.string.iride_menu_queue
                    else -> null
                }
            if (title == null) action else action.copy(title = { androidx.compose.material3.Text(stringResource(title)) })
        }

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
        text = stringResource(if (saved) R.string.iride_remove_star else R.string.iride_star),
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
    val scrollState = rememberScrollState()
    val overscrollPull = rememberRubberBandPull()
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
            // Measure this small menu in full: lazy estimates change as dividers enter the viewport.
            Column(
                modifier =
                    Modifier.padding(
                        end = if (constrained) IrideFolio.ScrollbarTouchTargetWidth else 0.dp
                    ).irideFolioGestures(scrollState, overscrollPull)
                    .verticalScroll(scrollState, overscrollEffect = null)
                    .padding(
                        horizontal = IrideFolio.MenuContentPadding,
                        vertical = IrideFolio.MenuVerticalPadding,
                    ),
            ) {
                sections.forEachIndexed { index, section ->
                    if (index > 0) IrideFolioMenuDivider()
                    section.forEach { action ->
                        key(action.actionId) {
                            IrideFolioContextMenuAction(
                                action = action,
                                onDismiss = onDismiss,
                                retainOpen = action.actionId in retainOpenActions,
                            )
                        }
                    }
                }
            }
            if (constrained)
                Box(Modifier.matchParentSize(), contentAlignment = Alignment.CenterEnd) {
                    IrideFolioScrollbar(
                        scrollState,
                        Modifier.fillMaxHeight().padding(
                            top = IrideFolio.MenuScrollbarTopInset,
                            bottom = IrideFolio.MenuScrollbarBottomInset,
                        ),
                        overscrollPull,
                    )
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
                Modifier.size(24.dp).clip(if (portrait) CircleShape else FolioShape(4.dp)),
        )
    }
}
