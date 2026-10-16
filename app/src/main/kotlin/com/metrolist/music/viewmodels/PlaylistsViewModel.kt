/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */
package com.metrolist.music.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.metrolist.music.db.MusicDatabase
import com.metrolist.music.db.entities.Playlist
import com.metrolist.music.db.entities.Song
import com.metrolist.music.extensions.isSyncEnabled
import com.metrolist.music.models.MediaMetadata
import com.metrolist.music.repository.CreatedPlaylist
import com.metrolist.music.repository.PlaylistDraftRepository
import com.metrolist.music.utils.SyncUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject

data class PlaylistAddState(
    val busy: Boolean = false,
    val failed: Boolean = false,
    val completed: CreatedPlaylist? = null,
)

@HiltViewModel
class PlaylistsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: MusicDatabase,
    private val syncUtils: SyncUtils,
    private val draftRepository: PlaylistDraftRepository,
) : ViewModel() {
    val allPlaylists = database.editablePlaylistsByNameAsc()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    private val _addState = MutableStateFlow(PlaylistAddState())
    val addState = _addState.asStateFlow()

    suspend fun sync() { syncUtils.syncSavedPlaylists() }

    suspend fun resolveSongs(ids: List<String>): List<MediaMetadata> = draftRepository.songs(ids)

    suspend fun importSongs(songs: List<Song>, onProgress: (Int, String) -> Unit): List<MediaMetadata> =
        draftRepository.importSongs(songs, onProgress)

    suspend fun containingSelection(ids: List<String>): Set<String> = withContext(Dispatchers.IO) {
        if (ids.isEmpty()) return@withContext emptySet()
        allPlaylists.value.filter { database.playlistDuplicates(it.id, ids).isNotEmpty() }.map { it.id }.toSet()
    }

    fun add(playlist: Playlist, allowSyncing: Boolean, loadSongs: suspend () -> List<MediaMetadata>) {
        if (_addState.value.busy) return
        _addState.value = PlaylistAddState(busy = true)
        viewModelScope.launch {
            try {
                val songs = withContext(Dispatchers.IO) { loadSongs() }
                val result = draftRepository.append(playlist.playlist, songs, allowSyncing && context.isSyncEnabled())
                _addState.value = PlaylistAddState(completed = result)
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                Timber.w(error, "Could not add playlist tracks")
                _addState.value = PlaylistAddState(failed = true)
            }
        }
    }

    fun resetResult() {
        if (!_addState.value.busy) _addState.value = PlaylistAddState()
    }
}
