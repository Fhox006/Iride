package com.metrolist.music.ui.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** Loading accepts one intent; the latest callback runs once its required data arrives. */
@Composable
internal fun IrideFolioContextMenuAction(
    action: Material3MenuItemData,
    onDismiss: () -> Unit,
    retainOpen: Boolean,
) {
    var queued by remember(action.actionId) { mutableStateOf(false) }
    val latestAction by rememberUpdatedState(action)
    val invoke by rememberUpdatedState(newValue = {
        latestAction.onClick?.invoke()
        if (!retainOpen) onDismiss()
    })
    LaunchedEffect(queued, action.readiness) {
        if (queued) {
            when (action.readiness) {
                MenuActionReadiness.Ready -> {
                    queued = false
                    invoke()
                }
                MenuActionReadiness.Unavailable -> queued = false
                MenuActionReadiness.Pending -> Unit
            }
        }
    }
    IrideFolioMenuItem(
        text = action.title,
        enabled = action.onClick != null && action.readiness != MenuActionReadiness.Unavailable,
        selected = action.selected,
        leadingIcon = if (queued) {
            { CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp) }
        } else action.icon?.let { icon ->
            { Box(Modifier.size(24.dp), contentAlignment = Alignment.Center) { icon() } }
        },
        onClick = {
            if (!queued) {
                when (action.readiness) {
                    MenuActionReadiness.Ready -> invoke()
                    MenuActionReadiness.Pending -> queued = true
                    MenuActionReadiness.Unavailable -> Unit
                }
            }
        },
    )
}
