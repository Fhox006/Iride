/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.metrolist.innertube.YouTube
import com.metrolist.innertube.models.AlbumItem
import com.metrolist.innertube.models.ArtistItem
import com.metrolist.innertube.models.PlaylistItem
import com.metrolist.innertube.models.filterYoutubeShorts
import com.metrolist.innertube.utils.completed
import com.metrolist.music.constants.HideYoutubeShortsKey
import com.metrolist.music.db.MusicDatabase
import com.metrolist.music.db.entities.PodcastEntity
import com.metrolist.music.ui.utils.resize
import com.metrolist.music.utils.dataStore
import com.metrolist.music.utils.get
import com.metrolist.music.utils.reportException
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class AccountContentType {
    PLAYLISTS, ALBUMS, ARTISTS, PODCASTS
}

@HiltViewModel
class AccountViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    database: MusicDatabase,
) : ViewModel() {
    val playlists = MutableStateFlow<List<PlaylistItem>?>(null)
    val albums = MutableStateFlow<List<AlbumItem>?>(null)
    val artists = MutableStateFlow<List<ArtistItem>?>(null)
    // SE "Episodes for Later" playlist shown in Podcasts tab
    val sePlaylist = MutableStateFlow<PlaylistItem?>(null)
    // RDPN "New Episodes" playlist (real thumbnail + count from YouTube)
    val rdpnPlaylist = MutableStateFlow<PlaylistItem?>(null)
    // Subscribed podcast shows (from local DB, synced from YT Music)
    val podcastPlaylists = database.subscribedPodcasts()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    // Podcast host channels from YT Music library
    val podcastChannels = MutableStateFlow<List<ArtistItem>>(emptyList())

    // Selected content type for chips
    val selectedContentType = MutableStateFlow(AccountContentType.PLAYLISTS)

    private suspend fun <T> withRetry(maxRetries: Int = 3, block: suspend () -> T): T? {
        var retryDelay = 1000L
        var lastError: Exception? = null
        for (attempt in 0 until maxRetries) {
            try {
                return block()
            } catch (e: Exception) {
                lastError = e
                if (attempt < maxRetries - 1) {
                    delay(retryDelay)
                    retryDelay *= 2
                }
            }
        }
        lastError?.let { reportException(it) }
        return null
    }

    private suspend fun loadPlaylists() {
        val hideYoutubeShorts = context.dataStore.get(HideYoutubeShortsKey, false)
        withRetry {
            YouTube.library("FEmusic_liked_playlists").completed().getOrThrow()
        }?.let { page ->
            val all = page.items.filterIsInstance<PlaylistItem>()
            sePlaylist.value = all.find { it.id == "SE" }
            playlists.value = all
                .filterNot { it.id == "SE" }
                .filterYoutubeShorts(hideYoutubeShorts)
        }
    }

    fun refresh() {
        viewModelScope.launch {
            loadPlaylists()
            withRetry {
                YouTube.library("FEmusic_liked_albums").completed().getOrThrow()
            }?.let { albums.value = it.items.filterIsInstance<AlbumItem>() }
            withRetry {
                YouTube.library("FEmusic_library_corpus_artists").completed().getOrThrow()
            }?.let {
                artists.value = it.items.filterIsInstance<ArtistItem>().map { artist ->
                    artist.copy(thumbnail = artist.thumbnail?.resize(544, 544))
                }
            }
        }
        viewModelScope.launch {
            withRetry { YouTube.newEpisodesPlaylistInfo().getOrThrow() }
                ?.let { rdpnPlaylist.value = it }
        }
        viewModelScope.launch(Dispatchers.IO) {
            withRetry { YouTube.libraryPodcastChannels().getOrThrow() }
                ?.let { podcastChannels.value = it.items.filterIsInstance<ArtistItem>() }
        }
    }

    init {
        refresh()

        // Listen for HideYoutubeShorts preference changes and reload playlists instantly
        viewModelScope.launch(Dispatchers.IO) {
            context.dataStore.data
                .map { it[HideYoutubeShortsKey] ?: false }
                .distinctUntilChanged()
                .collect {
                    if (playlists.value != null) {
                        loadPlaylists()
                    }
                }
        }
    }

    fun setSelectedContentType(contentType: AccountContentType) {
        selectedContentType.value = contentType
    }
}
