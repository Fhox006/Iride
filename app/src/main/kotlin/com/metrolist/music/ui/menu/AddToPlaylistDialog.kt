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
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.metrolist.music.R
import com.metrolist.music.db.entities.Playlist
import com.metrolist.music.db.entities.PlaylistEntity
import com.metrolist.music.ui.component.CreatePlaylistDialog
import com.metrolist.music.ui.component.IrideFolioPlaylistPicker
import com.metrolist.music.viewmodels.PlaylistsViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import timber.log.Timber

@Composable
fun AddToPlaylistDialog(
    isVisible: Boolean,
    allowSyncing: Boolean = true,
    initialTextFieldValue: String? = null,
    onGetSong: suspend (Playlist) -> List<String>,
    onGetSongIds: (suspend () -> List<String>)? = null,
    onDismiss: () -> Unit,
    viewModel: PlaylistsViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val playlists by viewModel.allPlaylists.collectAsStateWithLifecycle()
    val adding by viewModel.addState.collectAsStateWithLifecycle()
    var showCreate by rememberSaveable { mutableStateOf(false) }
    var songIds by remember { mutableStateOf<List<String>?>(null) }
    var containingSong by remember { mutableStateOf<Set<String>>(emptySet()) }
    val sourceMutex = remember { Mutex() }

    suspend fun sourceIds(): List<String> = sourceMutex.withLock {
        songIds ?: withContext(Dispatchers.IO) {
            // Legacy providers may sync as a side effect; a draft has no remote destination yet.
            onGetSongIds?.invoke() ?: onGetSong(Playlist(PlaylistEntity(name = ""), 0, emptyList()))
        }.also { songIds = it }
    }

    LaunchedEffect(isVisible) {
        if (!isVisible) {
            songIds = null
            containingSong = emptySet()
        } else if (!adding.busy) viewModel.resetResult()
    }
    LaunchedEffect(isVisible, playlists) {
        if (!isVisible) return@LaunchedEffect
        try {
            containingSong = viewModel.containingSelection(sourceIds())
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            Timber.d(error, "Playlist membership preview unavailable")
        }
    }
    LaunchedEffect(isVisible, adding.completed) {
        if (isVisible && adding.completed != null) {
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
                viewModel.add(playlist, allowSyncing) { viewModel.resolveSongs(sourceIds()) }
            },
        )
    }
    if (isVisible && showCreate) {
        CreatePlaylistDialog(
            onDismiss = { showCreate = false },
            initialTextFieldValue = initialTextFieldValue,
            allowSyncing = allowSyncing,
            loadInitialSongIds = { sourceIds() },
            onPlaylistCreated = { onDismiss() },
        )
    }
}
