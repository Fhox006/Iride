/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */
package com.metrolist.music.ui.component

import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.metrolist.music.R
import com.metrolist.music.models.MediaMetadata
import com.metrolist.music.db.entities.PlaylistEntity
import com.metrolist.music.viewmodels.PlaylistCreationViewModel
import java.util.UUID

@Composable
fun CreatePlaylistDialog(
    onDismiss: () -> Unit,
    initialTextFieldValue: String? = null,
    allowSyncing: Boolean = true,
    onPlaylistCreated: ((String) -> Unit)? = null,
    loadInitialSongs: (suspend () -> List<MediaMetadata>)? = null,
    loadInitialSongIds: (suspend () -> List<String>)? = null,
    targetPlaylist: PlaylistEntity? = null,
) {
    val draftKey = rememberSaveable { UUID.randomUUID().toString() }
    val viewModel = hiltViewModel<PlaylistCreationViewModel>()
    val draft by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val defaultName = stringResource(R.string.playlist_draft_title)

    fun initialize() = viewModel.initialize(draftKey, initialTextFieldValue, targetPlaylist) {
        loadInitialSongs?.invoke() ?: loadInitialSongIds?.invoke()?.let { viewModel.resolveSongs(it) }.orEmpty()
    }

    fun dismiss() {
        viewModel.release()
        onDismiss()
    }

    LaunchedEffect(viewModel) { initialize() }
    LaunchedEffect(draft.created) {
        draft.created?.let { result ->
            viewModel.consumeCreated()
            if (result.savedLocally) {
                Toast.makeText(context, R.string.playlist_draft_saved_locally, Toast.LENGTH_SHORT).show()
            }
            onPlaylistCreated?.invoke(result.id)
            dismiss()
        }
    }

    IrideFolioEditorDialog(onDismiss = ::dismiss, dismissEnabled = !draft.saving) {
        IrideFolioPlaylistComposer(
            draft = draft,
            onDismiss = ::dismiss,
            onNameChange = viewModel::setName,
            onQueryChange = viewModel::setQuery,
            onSelectionChange = viewModel::showSelection,
            onToggleSong = viewModel::toggle,
            onRefresh = viewModel::refresh,
            onRetrySeeds = ::initialize,
            onCreate = { viewModel.create(allowSyncing, defaultName) },
        )
    }
}
