package com.metrolist.music.viewmodels

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.metrolist.innertube.YouTube
import com.metrolist.innertube.models.PlaylistItem
import com.metrolist.innertube.models.SongItem
import com.metrolist.music.constants.HideVideoOnlyResultsKey
import com.metrolist.music.constants.HideVideoSongsKey
import com.metrolist.music.constants.HideVideosInLibraryKey
import com.metrolist.music.constants.MaxResolvedTrackCacheSizeKey
import com.metrolist.music.constants.ResolveVideoSongsKey
import com.metrolist.music.db.MusicDatabase
import com.metrolist.music.db.entities.SetVideoIdEntity
import com.metrolist.music.utils.dataStore
import com.metrolist.music.utils.get
import com.metrolist.music.utils.reportException
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay // ✅ FIX
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import com.metrolist.music.constants.SongSortType
import com.metrolist.innertube.models.Artist
import javax.inject.Inject

@HiltViewModel
class OnlinePlaylistViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    savedStateHandle: SavedStateHandle,
    private val database: MusicDatabase
) : ViewModel() {

    private val playlistId = savedStateHandle.get<String>("playlistId")!!

    private val normalizedPlaylistId = playlistId.removePrefix("VL")
    val isPodcastPlaylist = normalizedPlaylistId == "RDPN" || normalizedPlaylistId == "SE"

    val playlist = MutableStateFlow<PlaylistItem?>(null)
    val playlistSongs = MutableStateFlow<List<SongItem>>(emptyList())

    private val _isLoading = MutableStateFlow(true)
    val isLoading = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    private val _isLoadingMore = MutableStateFlow(false)
    val isLoadingMore = _isLoadingMore.asStateFlow()

    val dbPlaylist = database.playlistByBrowseId(playlistId)
        .stateIn(viewModelScope, SharingStarted.Lazily, null)

    var continuation: String? = null
        private set

    private var proactiveLoadJob: Job? = null
    private var resolveVideoJob: Job? = null

    // Video songs buffered for background resolution when hide=true
    private val pendingVideoSongs = mutableListOf<SongItem>()

    init {
        fetchInitialPlaylistData()
    }

    private fun fetchInitialPlaylistData() {
        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.value = true
            _error.value = null
            continuation = null
            proactiveLoadJob?.cancel()
            pendingVideoSongs.clear()

            if (isPodcastPlaylist) {
                fetchPodcastPlaylist()
            } else {
                fetchRegularPlaylist()
            }
        }
    }

    private suspend fun fetchPodcastPlaylist() {
        when (normalizedPlaylistId) {
            "RDPN" -> {
                YouTube.newEpisodes()
                    .onSuccess { episodes ->
                        playlist.value = PlaylistItem(
                            id = playlistId,
                            title = "New Episodes",
                            author = null,
                            songCountText = "${episodes.size} episodes",
                            thumbnail = episodes.firstOrNull()?.thumbnail ?: "",
                            playEndpoint = null,
                            shuffleEndpoint = null,
                            radioEndpoint = null,
                        )
                        playlistSongs.value = applySongFilters(episodes)
                        resolveVideoSongsAsync()
                        _isLoading.value = false
                    }.onFailure {
                        _error.value = it.message
                        _isLoading.value = false
                    }
            }

            "SE" -> {
                val result = YouTube.episodesForLater()
                val episodes = result.getOrNull() ?: emptyList()

                if (result.isSuccess && episodes.isNotEmpty()) {
                    playlist.value = PlaylistItem(
                        id = playlistId,
                        title = "Episodes for Later",
                        author = null,
                        songCountText = "${episodes.size} episodes",
                        thumbnail = episodes.firstOrNull()?.thumbnail ?: "",
                        playEndpoint = null,
                        shuffleEndpoint = null,
                        radioEndpoint = null,
                    )
                    playlistSongs.value = applySongFilters(episodes)
                    _isLoading.value = false
                } else {
                    loadLocalSavedEpisodes()
                }
            }
        }
    }

    private suspend fun fetchRegularPlaylist() {
        YouTube.playlist(playlistId)
            .onSuccess { page ->
                playlist.value = page.playlist
                playlistSongs.value = applySongFilters(page.songs)
                resolveVideoSongsAsync()
                continuation = page.songsContinuation
                _isLoading.value = false

                if (continuation != null) {
                    startProactiveBackgroundLoading()
                }
            }
    }

    private suspend fun loadLocalSavedEpisodes() {
        val saved = database.savedPodcastEpisodes(SongSortType.CREATE_DATE, true)
            .firstOrNull() ?: emptyList()

        val songItems = saved.map {
            SongItem(
                id = it.song.id,
                title = it.song.title,
                artists = it.artists.map { a -> Artist(a.id, a.name) },
                album = it.album?.let { a -> com.metrolist.innertube.models.Album(a.id, a.title) },
                duration = it.song.duration,
                thumbnail = it.song.thumbnailUrl ?: "",
                explicit = it.song.explicit,
                endpoint = null
            )
        }

        playlist.value = PlaylistItem(
            id = playlistId,
            title = "Episodes for Later",
            author = null,
            songCountText = "${songItems.size} episodes",
            thumbnail = songItems.firstOrNull()?.thumbnail ?: "",
            playEndpoint = null,
            shuffleEndpoint = null,
            radioEndpoint = null,
        )

        playlistSongs.value = applySongFilters(songItems)
        _isLoading.value = false
    }

    private fun startProactiveBackgroundLoading() {
        proactiveLoadJob?.cancel()

        proactiveLoadJob = viewModelScope.launch(Dispatchers.IO) {
            var token = continuation

            while (token != null && isActive) {
                if (_isLoadingMore.value) break

                YouTube.playlistContinuation(token)
                    .onSuccess {
                        val updated = playlistSongs.value.toMutableList()
                        updated.addAll(it.songs)
                        playlistSongs.value = applySongFilters(updated)
                        resolveVideoSongsAsync()

                        token = it.continuation
                        continuation = token
                    }
                    .onFailure {
                        token = null
                    }
            }
        }
    }

    fun loadMoreSongs() {
        if (_isLoadingMore.value) return
        val token = continuation ?: return

        proactiveLoadJob?.cancel()
        _isLoadingMore.value = true

        viewModelScope.launch(Dispatchers.IO) {
            YouTube.playlistContinuation(token)
                .onSuccess {
                    val updated = playlistSongs.value.toMutableList()
                    updated.addAll(it.songs)
                    playlistSongs.value = applySongFilters(updated)
                    resolveVideoSongsAsync()
                    continuation = it.continuation
                }

            _isLoadingMore.value = false

            if (continuation != null) {
                startProactiveBackgroundLoading()
            }
        }
    }

    fun retry() {
        proactiveLoadJob?.cancel()
        fetchInitialPlaylistData()
    }

    private fun applySongFilters(songs: List<SongItem>): List<SongItem> {
        val hide = context.dataStore.get(HideVideoSongsKey, false)
        val deduped = songs.distinctBy { it.id }
        if (!hide) return deduped

        val (videos, nonVideos) = deduped.partition { it.isVideoSong }
        val existingPendingIds = pendingVideoSongs.map { it.id }.toHashSet()
        pendingVideoSongs.addAll(videos.filter { it.id !in existingPendingIds })
        return nonVideos
    }

    private fun resolveVideoSongsAsync() {
        resolveVideoJob?.cancel()

        resolveVideoJob = viewModelScope.launch(Dispatchers.IO) {
            val resolveEnabled = context.dataStore.get(ResolveVideoSongsKey, true)
            if (!resolveEnabled) return@launch

            val hideVideoOnlyResults = context.dataStore.get(HideVideoOnlyResultsKey, false)
            val hideVideosInLibrary = context.dataStore.get(HideVideosInLibraryKey, false)
            val maxCacheSize = context.dataStore.get(MaxResolvedTrackCacheSizeKey, 1000)
            val hideVideoSongs = context.dataStore.get(HideVideoSongsKey, false)

            val isUserPlaylistOrLibrary = playlistId.startsWith("VL") ||
                                          playlistId.startsWith("PL") ||
                                          playlistId == "LM"

            val shouldHideVideoOnly = if (isUserPlaylistOrLibrary) hideVideosInLibrary else hideVideoOnlyResults

            if (hideVideoSongs) {
                // Resolve buffered video songs and APPEND audio equivalents to the displayed list
                val toResolve = pendingVideoSongs.toList()
                toResolve.forEach { song ->
                    if (!isActive) return@launch

                    val cached = database.getSetVideoId(song.id)
                    val resolved = if (cached != null) {
                        cached.setVideoId?.let { id -> song.copy(id = id, musicVideoType = null) }
                    } else {
                        val track = findAudioTrack(song)
                        database.insert(com.metrolist.music.db.entities.SetVideoIdEntity(song.id, track?.id))
                        if (maxCacheSize != -1) database.trimSetVideoIdCache(maxCacheSize)
                        track
                    }

                    if (resolved != null) {
                        val live = playlistSongs.value.toMutableList()
                        if (live.none { it.id == resolved.id }) {
                            live.add(resolved)
                            playlistSongs.value = live.toList()
                        }
                    }
                    // Unresolvable + hide=true → simply don't add

                    if (cached == null) delay(400)
                }
            } else {
                // Replace video songs in-place with resolved audio equivalents
                playlistSongs.value.forEach { song ->
                    if (!isActive) return@launch
                    if (!song.isVideoSong) return@forEach

                    val live = playlistSongs.value.toMutableList()
                    val liveIndex = live.indexOfFirst { it.id == song.id }
                    if (liveIndex == -1) return@forEach

                    val cached = database.getSetVideoId(song.id)
                    val resolved = if (cached != null) {
                        cached.setVideoId?.let { id -> song.copy(id = id, musicVideoType = null) }
                    } else {
                        val track = findAudioTrack(song)
                        database.insert(com.metrolist.music.db.entities.SetVideoIdEntity(song.id, track?.id))
                        if (maxCacheSize != -1) database.trimSetVideoIdCache(maxCacheSize)
                        track
                    }

                    if (resolved != null) {
                        live[liveIndex] = resolved
                        playlistSongs.value = live.toList()
                    } else if (shouldHideVideoOnly) {
                        live.removeAt(liveIndex)
                        playlistSongs.value = live.toList()
                    }

                    if (cached == null) delay(400)
                }
            }
        }
    }

    private suspend fun findAudioTrack(song: SongItem): SongItem? {
        val cleanTitle = song.title
            .replace(Regex("(?i)\\s*[\\[\\(](Official Video|Official Music Video|MV|Official Audio|Lyric Video|Audio)[\\]\\)]"), "")
            .replace(Regex("(?i)\\s*-\\s*(Visual Video|Official Video|Video|Music Video|Audio)"), "")
            .trim()
        val artistName = song.artists.firstOrNull()?.name ?: ""

        return YouTube.search("$cleanTitle $artistName", YouTube.SearchFilter.FILTER_SONG)
            .getOrNull()?.items?.filterIsInstance<SongItem>()?.firstOrNull { candidate ->
                if (candidate.isVideoSong) return@firstOrNull false

                val candidateCleanTitle = candidate.title
                    .replace(Regex("(?i)\\s*[\\[\\(](Official Video|Official Music Video|MV|Official Audio|Lyric Video|Audio)[\\]\\)]"), "")
                    .replace(Regex("(?i)\\s*-\\s*(Visual Video|Official Video|Video|Music Video|Audio)"), "")
                    .trim()

                val titleMatches = candidateCleanTitle.contains(cleanTitle, ignoreCase = true) ||
                        cleanTitle.contains(candidateCleanTitle, ignoreCase = true)
                if (!titleMatches) return@firstOrNull false

                val artistMatches = candidate.artists.any { it.name.equals(artistName, ignoreCase = true) }
                if (!artistMatches) return@firstOrNull false

                val songDuration = song.duration
                val candidateDuration = candidate.duration
                if (songDuration != null && candidateDuration != null) {
                    if (kotlin.math.abs(songDuration - candidateDuration) > 10) return@firstOrNull false
                }

                val blacklist = listOf("live", "instrumental", "acoustic", "cover", "karaoke", "remix", "performance", "session")
                if (blacklist.any { candidate.title.contains(it, ignoreCase = true) }) return@firstOrNull false

                true
            }
    }

    override fun onCleared() {
        proactiveLoadJob?.cancel()
        resolveVideoJob?.cancel()
    }
}