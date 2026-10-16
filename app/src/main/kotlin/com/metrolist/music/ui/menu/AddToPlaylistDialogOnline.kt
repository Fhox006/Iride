/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */
package com.metrolist.music.ui.menu

import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.metrolist.music.R
import com.metrolist.music.db.entities.Song
import com.metrolist.music.models.MediaMetadata
import com.metrolist.music.ui.component.CreatePlaylistDialog
import com.metrolist.music.ui.component.IrideFolioPlaylistPicker
import com.metrolist.music.viewmodels.PlaylistsViewModel

@Composable
fun AddToPlaylistDialogOnline(
    isVisible: Boolean,
    allowSyncing: Boolean = true,
    initialTextFieldValue: String? = null,
    songs: SnapshotStateList<Song>,
    onDismiss: () -> Unit,
    onProgressStart: (Boolean) -> Unit,
    onPercentageChange: (Int) -> Unit,
    onSongChange: (String) -> Unit = {},
    viewModel: PlaylistsViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val playlists by viewModel.allPlaylists.collectAsStateWithLifecycle()
    val adding by viewModel.addState.collectAsStateWithLifecycle()
    var showCreate by rememberSaveable { mutableStateOf(false) }
    var resolved by remember { mutableStateOf<List<MediaMetadata>?>(null) }
    var containingSong by remember { mutableStateOf<Set<String>>(emptySet()) }

    suspend fun sourceSongs(): List<MediaMetadata> = resolved ?: viewModel.importSongs(songs.toList()) { percent, title ->
        onPercentageChange(percent)
        onSongChange(title)
    }.also { resolved = it }

    LaunchedEffect(isVisible) {
        if (!isVisible) {
            resolved = null
            containingSong = emptySet()
        } else if (!adding.busy) viewModel.resetResult()
    }
    LaunchedEffect(isVisible, playlists, resolved) {
        if (isVisible) containingSong = viewModel.containingSelection(resolved?.map { it.id } ?: songs.map { it.id })
    }
    LaunchedEffect(isVisible, adding.completed, adding.failed) {
        if (!isVisible) return@LaunchedEffect
        if (adding.completed != null || adding.failed) onProgressStart(false)
        if (adding.completed != null) {
            if (adding.completed?.savedLocally == true) {
                Toast.makeText(context, R.string.playlist_draft_saved_locally, Toast.LENGTH_SHORT).show()
            }
            viewModel.resetResult()
            onDismiss()
        }
    }

    if (isVisible && !showCreate) {
        IrideFolioPlaylistPicker(
            playlists = playlists,
            containingSong = containingSong,
            busy = adding.busy,
            failed = adding.failed,
            onDismiss = onDismiss,
            onCreate = { showCreate = true },
            onSelect = { playlist ->
                onProgressStart(true)
                viewModel.add(playlist, allowSyncing) { sourceSongs() }
            },
        )
    }
    if (isVisible && showCreate) {
        CreatePlaylistDialog(
            onDismiss = { showCreate = false },
            initialTextFieldValue = initialTextFieldValue,
            allowSyncing = allowSyncing,
            loadInitialSongs = { sourceSongs() },
            onPlaylistCreated = { onDismiss() },
        )
    }
}
