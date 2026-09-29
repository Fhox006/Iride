package com.metrolist.music.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.metrolist.innertube.YouTube
import com.metrolist.innertube.models.PlaylistItem
import com.metrolist.innertube.models.SongItem
import com.metrolist.music.constants.HideExplicitKey
import com.metrolist.music.utils.dataStore
import com.metrolist.music.utils.get
import com.metrolist.music.utils.reportException
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RockModuleState(
    val songs: List<SongItem> = emptyList(),
    val playlists: List<PlaylistItem> = emptyList(),
    val songsLoading: Boolean = true,
    val playlistsLoading: Boolean = true,
    val songsFailed: Boolean = false,
    val playlistsFailed: Boolean = false,
)

@HiltViewModel
class RockModuleViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
) : ViewModel() {
    private val _state = MutableStateFlow(RockModuleState())
    val state: StateFlow<RockModuleState> = _state.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        _state.value = RockModuleState()
        viewModelScope.launch {
            val hideExplicit = context.dataStore.get(HideExplicitKey, false)
            val (songResult, playlistResult) = coroutineScope {
                val songs = async(Dispatchers.IO) {
                    YouTube.search("rock viral", YouTube.SearchFilter.FILTER_SONG)
                }
                val playlists = async(Dispatchers.IO) {
                    YouTube.search("rock", YouTube.SearchFilter.FILTER_COMMUNITY_PLAYLIST)
                }
                songs.await() to playlists.await()
            }

            val songs = songResult.getOrNull()?.items.orEmpty()
                .filterIsInstance<SongItem>()
                .filter { !it.isVideoSong && !it.isEpisode && (!hideExplicit || !it.explicit) }
                .distinctBy { it.id }
            val playlists = playlistResult.getOrNull()?.items.orEmpty()
                .filterIsInstance<PlaylistItem>()
                .filterNot { it.id.startsWith("RD") || it.id.startsWith("OLAK") }
                .distinctBy { it.id }

            songResult.exceptionOrNull()?.let(::reportException)
            playlistResult.exceptionOrNull()?.let(::reportException)
            _state.value = RockModuleState(
                songs = songs,
                playlists = playlists,
                songsLoading = false,
                playlistsLoading = false,
                songsFailed = songResult.isFailure,
                playlistsFailed = playlistResult.isFailure,
            )
        }
    }
}
