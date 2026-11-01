package com.metrolist.music.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.metrolist.music.R
import com.metrolist.music.ui.component.Material3SettingsGroup
import com.metrolist.music.ui.component.Material3SettingsItem

@Composable
internal fun PlaylistImportRow(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    busy: Boolean = false,
    saved: Boolean = false,
    progress: Float? = null,
    onOpen: (() -> Unit)? = null,
) {
    Column(modifier) {
        Material3SettingsGroup(items = listOf(Material3SettingsItem(
            icon = painterResource(R.drawable.playlist_play),
            title = { Text(title, maxLines = 2, overflow = TextOverflow.Ellipsis) },
            description = { Text(subtitle) },
            trailingContent = {
                when {
                    busy -> CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                    saved -> Icon(painterResource(R.drawable.check), null)
                }
            },
            onClick = onOpen,
        )))
        progress?.let {
            LinearProgressIndicator(
                progress = { it.coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            )
        }
    }
}
