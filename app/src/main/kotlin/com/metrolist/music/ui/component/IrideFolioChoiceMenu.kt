package com.metrolist.music.ui.component

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.res.painterResource

private class FolioChoiceAnchor {
    var bounds: Rect? = null
}

/**
 * Selectors share the contextual menu's geometry, safe viewport and constrained scroll indicator.
 */
@Composable
internal fun IrideFolioChoiceMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    actions: List<IrideFolioMenuAction>,
    sections: List<List<IrideFolioMenuAction>> = listOf(actions),
    modifier: Modifier = Modifier,
    focusable: Boolean = true,
) {
    val anchor = remember { FolioChoiceAnchor() }
    val popup = remember { MenuState() }
    val expandedAnchor = remember(expanded) { mutableStateOf(anchor.bounds) }
    LaunchedEffect(expanded, popup) {
        if (expanded) popup.show(anchored = true) {} else popup.dismiss()
    }
    Box(
        modifier.onGloballyPositioned {
            val bounds = it.parentLayoutCoordinates?.boundsInWindow() ?: it.boundsInWindow()
            anchor.bounds = bounds
            if (expanded) expandedAnchor.value = bounds
        }
    )
    IrideFolioContextMenu(
        items =
            sections.flatMapIndexed { section, entries ->
                entries.map { action ->
                    Material3MenuItemData(
                        title = { Text(action.label) },
                        icon = action.icon?.let { icon -> { Icon(painterResource(icon), null) } },
                        selected = action.selected,
                        onClick = action.onClick,
                        section = section,
                    )
                }
            },
        visible = expanded || popup.popupMotion?.retainMenu == true,
        onDismiss = onDismissRequest,
        popupOverride = popup,
        anchorOverride = expandedAnchor.value,
        focusable = focusable,
    )
}
