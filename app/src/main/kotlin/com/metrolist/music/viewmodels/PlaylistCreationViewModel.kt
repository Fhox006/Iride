package com.metrolist.music.viewmodels

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.metrolist.music.db.entities.PlaylistEntity
import com.metrolist.music.extensions.isSyncEnabled
import com.metrolist.music.models.MediaMetadata
import com.metrolist.music.repository.CreatedPlaylist
import com.metrolist.music.repository.PlaylistDraftRepository
import com.metrolist.music.repository.playlistDraftRecommendations
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject

data class PlaylistDraftState(
    val name: String = "",
    val query: String = "",
    val songs: List<MediaMetadata> = emptyList(),
    val existingSongs: List<MediaMetadata> = emptyList(),
    val addingToPlaylist: Boolean = false,
    val suggestions: List<MediaMetadata> = emptyList(),
    val showingSelection: Boolean = false,
    val loading: Boolean = false,
    val preparing: Boolean = true,
    val saving: Boolean = false,
    val unavailable: Boolean = false,
    val error: PlaylistDraftError? = null,
    val created: CreatedPlaylist? = null,
)

enum class PlaylistDraftError { INITIAL_SONGS, SAVE }

@HiltViewModel
class PlaylistCreationViewModel @Inject constructor(
    private val repository: PlaylistDraftRepository,
    private val savedState: SavedStateHandle,
    @ApplicationContext private val context: Context,
) : ViewModel() {
    private var draftId = savedState.get<String>("draftId") ?: PlaylistEntity.generatePlaylistId().also {
        savedState["draftId"] = it
    }
    private val _state = MutableStateFlow(
        PlaylistDraftState(
            name = savedState["name"] ?: "",
            query = savedState["query"] ?: "",
            songs = savedState.get<ArrayList<MediaMetadata>>("songs").orEmpty(),
            showingSelection = savedState["selection"] ?: false,
        ),
    )
    val state = _state.asStateFlow()
    private var initialized = false
    private var preparationJob: Job? = null
    private var discoveryJob: Job? = null
    private var page = 0
    private var targetPlaylist: PlaylistEntity? = null

    fun initialize(
        session: String,
        initialName: String?,
        playlist: PlaylistEntity?,
        loadSongs: (suspend () -> List<MediaMetadata>)?,
    ) {
        if (savedState.get<String>("session") != session) {
            release()
            savedState["session"] = session
        }
        if (initialized) return
        initialized = true
        targetPlaylist = playlist
        _state.update { it.copy(preparing = true, addingToPlaylist = playlist != null) }
        preparationJob = viewModelScope.launch {
            try {
                val existing = playlist?.let { repository.playlistSongs(it.id) }.orEmpty()
                _state.update { it.copy(existingSongs = existing) }
                if (savedState.get<Boolean>("initialized") != true) {
                    val songs = withContext(Dispatchers.IO) { loadSongs?.invoke().orEmpty() }
                    _state.update { it.copy(name = playlist?.name ?: initialName.orEmpty(), songs = songs.distinctBy { it.id }) }
                    savedState["initialized"] = true
                    persist()
                }
                _state.update { it.copy(preparing = false, error = null) }
                discover()
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                Timber.w(error, "Could not prepare playlist seeds")
                initialized = false
                _state.update { it.copy(preparing = false, error = PlaylistDraftError.INITIAL_SONGS) }
            }
        }
    }

    private fun persist() {
        val draft = _state.value
        savedState["name"] = draft.name
        savedState["query"] = draft.query
        savedState["songs"] = ArrayList(draft.songs)
        savedState["selection"] = draft.showingSelection
    }

    fun setName(name: String) {
        if (_state.value.saving || _state.value.addingToPlaylist) return
        _state.update { it.copy(name = name.take(120)) }
        persist()
        page = 0
        if (_state.value.query.isBlank() && _state.value.songs.isEmpty()) discover(debounce = true)
    }

    fun setQuery(query: String) {
        if (_state.value.saving) return
        _state.update { it.copy(query = query, showingSelection = false, suggestions = emptyList()) }
        persist()
        page = 0
        discover(debounce = true)
    }

    fun showSelection(selected: Boolean) {
        _state.update { it.copy(showingSelection = selected) }
        persist()
    }

    fun toggle(song: MediaMetadata) {
        if (_state.value.saving || _state.value.preparing) return
        _state.update { draft ->
            draft.copy(songs = if (draft.songs.any { it.id == song.id }) {
                draft.songs.filterNot { it.id == song.id }
            } else draft.songs + song)
        }
        persist()
        page = 0
        if (_state.value.query.isBlank()) discover(debounce = true)
    }

    fun refresh() {
        page++
        discover()
    }

    private fun discover(debounce: Boolean = false) {
        discoveryJob?.cancel()
        discoveryJob = viewModelScope.launch {
            val draft = _state.value
            val allSongs = draft.existingSongs + draft.songs
            val existingIds = draft.existingSongs.map { it.id }.toSet()
            _state.update { it.copy(loading = true, unavailable = false) }
            try {
                if (debounce) delay(500)
                val query = draft.query.trim()
                val local = repository.localSongs(query).filterNot { it.id in existingIds }
                _state.update { it.copy(suggestions = playlistDraftRecommendations(local, allSongs, draft.name, page)) }
                var unavailable = false
                val remote = if (query.isNotEmpty()) {
                    repository.search(query).also { unavailable = it == null }.orEmpty()
                } else if (allSongs.isNotEmpty()) {
                    val seeds = allSongs.takeLast(3).reversed()
                    val results = coroutineScope {
                        seeds.map { seed -> async { repository.related(seed) } }.awaitAll()
                    }
                    unavailable = results.any { it == null }
                    val groups = results.map { it.orEmpty() }
                    // Interleave seed radios so one artist cannot occupy the entire first page.
                    (0 until (groups.maxOfOrNull { it.size } ?: 0)).flatMap { index ->
                        groups.mapNotNull { it.getOrNull(index) }
                    }
                } else if (draft.name.trim().length >= 3) {
                    repository.search(draft.name.trim()).also { unavailable = it == null }.orEmpty()
                } else emptyList()
                _state.update {
                    it.copy(
                        suggestions = if (query.isNotEmpty()) (local + remote).filterNot { song -> song.id in existingIds }.distinctBy { song -> song.id }
                        else playlistDraftRecommendations(remote + local, allSongs, draft.name, page),
                        unavailable = unavailable,
                        loading = false,
                    )
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                Timber.w(error, "Could not load playlist discovery")
                _state.update { it.copy(loading = false, unavailable = true) }
            }
        }
    }

    fun create(allowSyncing: Boolean, defaultName: String) {
        val draft = _state.value
        if (draft.saving || draft.preparing || draft.error == PlaylistDraftError.INITIAL_SONGS || draft.created != null) return
        if (targetPlaylist != null && draft.songs.isEmpty()) return
        _state.update { it.copy(saving = true, error = null) }
        discoveryJob?.cancel()
        viewModelScope.launch {
            try {
                val name = draft.name.trim().ifEmpty { repository.defaultName(defaultName) }
                val sync = allowSyncing && context.isSyncEnabled()
                val result = targetPlaylist?.let { repository.append(it, draft.songs, sync) }
                    ?: repository.create(draftId, name, draft.songs, sync)
                _state.update { it.copy(saving = false, created = result, loading = false) }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                Timber.e(error, "Could not save playlist")
                _state.update { it.copy(saving = false, loading = false, error = PlaylistDraftError.SAVE) }
            }
        }
    }

    fun consumeCreated() { _state.update { it.copy(created = null) } }

    fun release() {
        if (_state.value.saving) return
        preparationJob?.cancel()
        discoveryJob?.cancel()
        initialized = false
        page = 0
        targetPlaylist = null
        repository.clear()
        draftId = PlaylistEntity.generatePlaylistId()
        savedState["draftId"] = draftId
        savedState["initialized"] = false
        _state.value = PlaylistDraftState()
        persist()
    }

    suspend fun resolveSongs(ids: List<String>): List<MediaMetadata> = repository.songs(ids)
}
