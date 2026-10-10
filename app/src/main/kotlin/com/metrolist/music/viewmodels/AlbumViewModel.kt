/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.viewmodels

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.metrolist.innertube.YouTube
import com.metrolist.innertube.models.AlbumItem
import com.metrolist.innertube.models.Artist
import com.metrolist.innertube.pages.AlbumPage
import com.metrolist.music.discovery.AlbumDiscoveryRepository
import com.metrolist.music.discovery.AlbumDiscoveryContext
import com.metrolist.music.discovery.DiscoveryLanguage
import com.metrolist.music.data.remote.MusicBrainzRepository
import com.metrolist.music.db.MusicDatabase
import com.metrolist.music.utils.NetworkConnectivityObserver
import com.metrolist.music.utils.reportException
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull
import timber.log.Timber
import javax.inject.Inject
import java.util.Locale

data class AlbumDiscoveryState(
    val albums: List<AlbumItem> = emptyList(),
    val styleAlbums: List<AlbumItem> = emptyList(),
    val albumsLoading: Boolean = true,
    val styleLoading: Boolean = true,
)

@HiltViewModel
class AlbumViewModel
@Inject
constructor(
    private val database: MusicDatabase,
    private val musicBrainzRepository: MusicBrainzRepository,
    private val networkConnectivity: NetworkConnectivityObserver,
    private val discoveryRepository: AlbumDiscoveryRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    val albumId = savedStateHandle.get<String>("albumId")!!
    val playlistId = MutableStateFlow("")

    val albumWithSongs =
        database
            .albumWithSongs(albumId)
            .stateIn(viewModelScope, SharingStarted.Eagerly, null)
    var otherVersions = MutableStateFlow<List<AlbumItem>>(emptyList())
    private val _discovery = MutableStateFlow(AlbumDiscoveryState())
    val discovery = _discovery.asStateFlow()
    private val _discoveryLanguage = MutableStateFlow(
        DiscoveryLanguage.all.firstOrNull { it.code == savedStateHandle.get<String>("discoveryLanguage") }
            ?: DiscoveryLanguage.defaultFor(Locale.getDefault()),
    )
    val discoveryLanguage = _discoveryLanguage.asStateFlow()
    private val stateHandle = savedStateHandle
    private var discoveryPage: AlbumPage? = null
    private var discoverySource: Deferred<AlbumDiscoveryContext?>? = null
    private var mixJob: Job? = null
    private var styleJob: Job? = null
    private var hideDiscoveryExplicit = false
    private var discoveryConfigured = false
    private var discoveryActive = false

    fun configureDiscovery(appLocale: Locale, hideExplicit: Boolean) {
        val changed = hideDiscoveryExplicit != hideExplicit
        hideDiscoveryExplicit = hideExplicit
        if (!discoveryConfigured) {
            discoveryConfigured = true
            if (!stateHandle.contains("discoveryLanguage")) _discoveryLanguage.value = DiscoveryLanguage.defaultFor(appLocale)
        }
        if (discoveryActive && discoveryPage != null && (changed || mixJob == null)) loadDiscovery(refreshMix = true)
    }

    fun activateDiscovery() {
        if (discoveryActive) return
        discoveryActive = true
        loadDiscovery(refreshMix = true)
    }

    fun selectDiscoveryLanguage(language: DiscoveryLanguage) {
        if (_discoveryLanguage.value == language) return
        _discoveryLanguage.value = language
        stateHandle["discoveryLanguage"] = language.code
        loadDiscovery(refreshMix = false)
    }

    fun refreshDiscovery() {
        if (discoveryPage == null) retry() else loadDiscovery(refreshMix = true)
    }

    private fun loadDiscovery(refreshMix: Boolean) {
        val page = discoveryPage ?: return
        val source = discoverySource ?: viewModelScope.async(Dispatchers.IO) {
            withTimeoutOrNull(10_000) { discoveryRepository.prepare(page) }
        }.also { discoverySource = it }
        val language = _discoveryLanguage.value
        val hideExplicit = hideDiscoveryExplicit
        val rotation = System.nanoTime()
        if (refreshMix) {
            mixJob?.cancel()
            _discovery.value = _discovery.value.copy(albumsLoading = true)
            mixJob = viewModelScope.launch {
                try {
                    val baseline = discoveryRepository.immediateAlbums(page, hideExplicit)
                    _discovery.value = _discovery.value.copy(albums = baseline)
                    withTimeoutOrNull(18_000) {
                        val albums = discoveryRepository.youtubeMix(page, hideExplicit, rotation) {
                            _discovery.value = _discovery.value.copy(albums = it)
                        }
                        _discovery.value = _discovery.value.copy(albums = albums)
                    }
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (error: Exception) {
                    Timber.w(error, "Album recommendations retain cached YouTube results")
                } finally {
                    if (currentCoroutineContext().isActive) _discovery.value = _discovery.value.copy(albumsLoading = false)
                }
            }
        }
        styleJob?.cancel()
        _discovery.value = _discovery.value.copy(styleLoading = true)
        styleJob = viewModelScope.launch {
            try {
                val baseline = discoveryRepository.immediateAlbums(page, hideExplicit, language)
                _discovery.value = _discovery.value.copy(styleAlbums = baseline)
                val context = withTimeoutOrNull(1_500) { source.await() }
                withTimeoutOrNull(18_000) {
                    val albums = discoveryRepository.youtubeStyle(page, language, hideExplicit, rotation, context) {
                        _discovery.value = _discovery.value.copy(styleAlbums = it)
                    }
                    _discovery.value = _discovery.value.copy(styleAlbums = albums)
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                Timber.w(error, "Album style recommendations retain cached YouTube results")
            } finally {
                if (currentCoroutineContext().isActive) _discovery.value = _discovery.value.copy(styleLoading = false)
            }
        }
    }

    private val _isLoading = MutableStateFlow(true)
    val isLoading = _isLoading.asStateFlow()

    private val _hasError = MutableStateFlow(false)
    val hasError = _hasError.asStateFlow()

    init {
        fetchFromYouTube()

        viewModelScope.launch {
            networkConnectivity.networkStatus
                .drop(1)
                .collect { connected ->
                    if (connected && _hasError.value) {
                        Timber.d("Network restored — auto-retrying album fetch for %s", albumId)
                        retry()
                    }
                }
        }
    }

    fun retry() {
        _hasError.value = false
        _isLoading.value = true
        fetchFromYouTube()
    }

    private fun restoreCachedDiscovery() {
        viewModelScope.launch {
            val cached = database.album(albumId).first() ?: return@launch
            val item = AlbumItem(
                browseId = cached.id, playlistId = cached.album.playlistId.orEmpty(), title = cached.title,
                artists = cached.artists.map { Artist(it.name, it.id) }, year = cached.album.year,
                thumbnail = cached.album.thumbnailUrl.orEmpty(), explicit = cached.album.explicit,
                albumType = cached.album.albumType,
            )
            val page = AlbumPage(item, emptyList(), otherVersions.value)
            discoveryPage = page
            val albums = discoveryRepository.immediateAlbums(page, hideDiscoveryExplicit, _discoveryLanguage.value)
            _discovery.value = AlbumDiscoveryState(albums, albums, albumsLoading = false, styleLoading = false)
            if (discoveryActive) loadDiscovery(refreshMix = true)
        }
    }

    private fun fetchFromYouTube() {
        viewModelScope.launch {
            try {
                withTimeout(60_000L) {
                    val album = database.album(albumId).first()
                    val ytResult = YouTube.album(albumId)

                    ytResult
                        .onSuccess { albumPage ->
                            playlistId.value = albumPage.album.playlistId
                            otherVersions.value = albumPage.otherVersions
                            database.transaction {
                                if (album == null) {
                                    insert(albumPage)
                                } else {
                                    update(album.album, albumPage, album.artists)
                                }
                            }
                            _isLoading.value = false
                            discoveryPage = albumPage
                            val versionIds = albumPage.otherVersions.map { it.id }.toSet()
                            val initial = albumPage.similarAlbums.distinctBy { it.id }
                                .filterNot { it.id == albumId || it.id in versionIds || (hideDiscoveryExplicit && it.explicit) }
                                .ifEmpty { listOf(albumPage.album) }
                            _discovery.value = AlbumDiscoveryState(albums = initial, styleAlbums = initial)
                            discoverySource?.cancel()
                            discoverySource = null
                            if (discoveryActive) loadDiscovery(refreshMix = true)
                        }.onFailure {
                            reportException(it)
                            if (it.message?.contains("NOT_FOUND") == true) {
                                database.query {
                                    album?.album?.let(::delete)
                                }
                            }
                            _hasError.value = true
                            _isLoading.value = false
                            _discovery.value = _discovery.value.copy(albumsLoading = false, styleLoading = false)
                            restoreCachedDiscovery()
                        }

                    val albumPage = ytResult.getOrNull() ?: return@withTimeout

                    val currentReleaseDate = album?.album?.releaseDate
                    val regex = Regex("""\d{4}-\d{2}(-\d{2})?""")

                    if (currentReleaseDate == null || !regex.matches(currentReleaseDate)) {
                        viewModelScope.launch(Dispatchers.IO) {
                            val releaseDate = musicBrainzRepository.getAlbumReleaseDate(
                                albumTitle = albumPage.album.title,
                                artistName = albumPage.album.artists?.firstOrNull()?.name,
                                year = albumPage.album.year
                            )
                            if (releaseDate != null) {
                                database.album(albumId).first()?.let { currentAlbum ->
                                    database.query {
                                        update(currentAlbum.album.copy(releaseDate = releaseDate))
                                    }
                                }
                            }
                        }
                    }
                }
            } catch (e: TimeoutCancellationException) {
                _hasError.value = true
                _isLoading.value = false
                _discovery.value = _discovery.value.copy(albumsLoading = false, styleLoading = false)
                restoreCachedDiscovery()
            } catch (e: Exception) {
                reportException(e)
                _hasError.value = true
                _isLoading.value = false
                _discovery.value = _discovery.value.copy(albumsLoading = false, styleLoading = false)
                restoreCachedDiscovery()
            }
        }
    }
}
