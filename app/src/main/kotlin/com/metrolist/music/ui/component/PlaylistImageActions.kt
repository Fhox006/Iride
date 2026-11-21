package com.metrolist.music.ui.component

import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.datastore.preferences.core.edit
import com.metrolist.music.LocalDatabase
import com.metrolist.music.R
import com.metrolist.music.ui.screens.playlist.PlaylistCoverPickerActivity
import com.metrolist.music.utils.dataStore
import com.metrolist.music.utils.get
import com.metrolist.music.utils.playlistCoverKey
import com.metrolist.music.utils.playlistCoverId
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

@Composable
internal fun rememberPlaylistCustomCover(id: String?): String? {
    if (id == null) return null
    val context = LocalContext.current
    val key = remember(id) { playlistCoverKey(id) }
    val flow = remember(context, key) { context.dataStore.data.map { it[key] } }
    val cover by flow.collectAsState(initial = context.dataStore[key])
    return cover?.takeIf { it.isNotBlank() }
}

@Composable
internal fun playlistImageActions(id: String, legacyCustom: Boolean = false): List<Material3MenuItemData> {
    if (id == com.metrolist.music.db.entities.PlaylistEntity.LIKED_PLAYLIST_ID ||
        id == com.metrolist.music.db.entities.PlaylistEntity.STARRED_PLAYLIST_ID || id == "LM") return emptyList()
    val context = LocalContext.current
    val database = LocalDatabase.current
    val scope = rememberMenuActionScope()
    val custom = rememberPlaylistCustomCover(id)
    return buildList {
        add(folioMenuAction(
            if (custom != null || legacyCustom) R.string.playlist_change_image else R.string.playlist_set_image,
            R.drawable.insert_photo,
            onClick = {
                context.startActivity(Intent(context, PlaylistCoverPickerActivity::class.java)
                    .putExtra("playlistId", id))
            },
        ))
        if (custom != null || legacyCustom) add(folioMenuAction(
            R.string.remove_custom_image, R.drawable.delete,
            onClick = {
                scope.launch {
                    context.dataStore.edit { it.remove(playlistCoverKey(id)) }
                    val playlist = database.playlist(playlistCoverId(id)).first()
                        ?: database.playlistByBrowseId(id.removePrefix("VL")).first()
                    playlist?.let { database.query { update(it.playlist.copy(thumbnailUrl = null)) } }
                }
            },
        ))
    }
}

@Composable
internal fun playlistImageHeaderActions(id: String): List<IrideFolioMenuAction> =
    playlistImageActions(id).map { action ->
        IrideFolioMenuAction(
            label = stringResource(action.actionId!!),
            icon = if (action.actionId == R.string.remove_custom_image) R.drawable.delete else R.drawable.insert_photo,
            onClick = { action.onClick?.invoke() },
        )
    }
