/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

@file:OptIn(ExperimentalCoroutinesApi::class, kotlinx.coroutines.FlowPreview::class)

package com.metrolist.music.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.metrolist.innertube.YouTube
import com.metrolist.innertube.models.AlbumItem
import com.metrolist.innertube.models.ArtistItem
import com.metrolist.innertube.models.PlaylistItem
import com.metrolist.innertube.models.SongItem
import com.metrolist.innertube.models.WatchEndpoint
import com.metrolist.innertube.models.YTItem
import com.metrolist.innertube.models.filterExplicit
import com.metrolist.innertube.models.filterVideoSongs
import com.metrolist.innertube.utils.YouTubeUrlParser
import com.metrolist.music.constants.HideExplicitKey
import com.metrolist.music.constants.HideVideoSongsKey
import com.metrolist.music.db.MusicDatabase
import com.metrolist.music.db.entities.SearchHistory
import com.metrolist.music.db.entities.Song
import com.metrolist.music.ui.screens.search.rankSearchSuggestions
import com.metrolist.innertube.models.Artist
import com.metrolist.innertube.models.Album
import com.metrolist.innertube.models.filterYoutubeShorts
import com.metrolist.music.constants.HideYoutubeShortsKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import com.metrolist.music.utils.dataStore
import com.metrolist.music.utils.get
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.delay
import com.metrolist.music.ui.screens.search.searchPredictionScore
import com.metrolist.music.discovery.searchPersonalCatalog
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class OnlineSearchSuggestionViewModel
    @Inject
    constructor(
        @ApplicationContext val context: Context,
        database: MusicDatabase,
    ) : ViewModel() {
        val query = MutableStateFlow("")
        var previewResultsEnabled = false
        private val _viewState = MutableStateFlow(SearchSuggestionViewState())
        val viewState = _viewState.asStateFlow()

        private val personalItems = database.searchPersonalCatalog()
            .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

        init {
            viewModelScope.launch {
                query.flatMapLatest { draft ->
                    val network = flow {
                        if (draft.isBlank()) {
                            emit(SearchSuggestionViewState(query = draft))
                            return@flow
                        }
                        val parsed = YouTubeUrlParser.parse(draft)
                        if (parsed != null) {
                            emit(SearchSuggestionViewState(query = draft, isUrlQuery = true))
                            val parsedItem = listOfNotNull(withTimeoutOrNull(8_000) { fetchParsedUrlItem(parsed) })
                            emit(SearchSuggestionViewState(query = draft, items = parsedItem, isUrlQuery = true))
                        } else {
                            // Clear previous-query previews while the current draft loads.
                            emit(SearchSuggestionViewState(query = draft))
                            delay(150)
                            val result = withTimeoutOrNull(5_000) { YouTube.searchSuggestions(draft).getOrNull() }
                            currentCoroutineContext().ensureActive()
                            emit(SearchSuggestionViewState(items = result?.recommendedItems.orEmpty(), suggestions = result?.queries.orEmpty()))
                            // Fetch a preview only when completions do not fill the five useful slots.
                            if (previewResultsEnabled && draft.trim().length >= 2 &&
                                rankSearchSuggestions(draft, personalItems.value, result?.recommendedItems.orEmpty()).size < 5) {
                                val recognizedArtist = rankSearchSuggestions(draft, personalItems.value, emptyList())
                                    .filterIsInstance<ArtistItem>().firstOrNull {
                                        searchPredictionScore(draft, it.title) != null
                                    }
                                val lookup = recognizedArtist?.title ?: draft
                                val summary = withTimeoutOrNull(5_000) { YouTube.searchSummary(lookup).getOrNull() }
                                currentCoroutineContext().ensureActive()
                                val items = result?.recommendedItems.orEmpty() + summary?.summaries.orEmpty().flatMap { it.items }
                                emit(SearchSuggestionViewState(items = items, suggestions = result?.queries.orEmpty()))
                                if (rankSearchSuggestions(draft, personalItems.value, items).size < 5) {
                                    val songs = withTimeoutOrNull(5_000) { YouTube.search(lookup, YouTube.SearchFilter.FILTER_SONG).getOrNull() }
                                    currentCoroutineContext().ensureActive()
                                    emit(SearchSuggestionViewState(items = items + songs?.items.orEmpty(), suggestions = result?.queries.orEmpty()))
                                }
                            }
                        }
                    }.flowOn(Dispatchers.IO)
                    combine(network, personalItems, database.searchHistory()) { remote, personal, history ->
                        withContext(Dispatchers.Default) {
                            val hideExplicit = context.dataStore.get(HideExplicitKey, false)
                            val hideVideoSongs = context.dataStore.get(HideVideoSongsKey, false)
                            val hideShorts = context.dataStore.get(HideYoutubeShortsKey, false)
                            val local = personal.filterExplicit(hideExplicit).filterVideoSongs(hideVideoSongs)
                                .filterYoutubeShorts(hideShorts)
                            val online = remote.items.filterNot { it is SongItem && (it.isVideoSong || it.isEpisode) }.filterExplicit(hideExplicit).filterVideoSongs(hideVideoSongs)
                                .filterYoutubeShorts(hideShorts)
                            val ranked = rankSearchSuggestions(draft, local, online)
                            val parsed = YouTubeUrlParser.parse(draft)
                            SearchSuggestionViewState(
                                query = draft,
                                history = history,
                                suggestions = remote.suggestions,
                                recommendedItems = ranked,
                                items = ranked,
                                parsedUrlItem = remote.items.firstOrNull().takeIf { parsed != null },
                                isUrlQuery = parsed != null,
                            )
                        }
                    }
                }.collect { state ->
                    if (state.query == query.value) _viewState.value = state
                }
            }
        }

        private fun Song.searchSong() = SongItem(
            id = id, title = title, artists = orderedArtists.map { Artist(it.name, it.id) },
            album = song.albumId?.let { Album(song.albumName.orEmpty(), it) },
            duration = song.duration.takeIf { it > 0 }, thumbnail = thumbnailUrl.orEmpty(),
            explicit = song.explicit, endpoint = WatchEndpoint(videoId = id),
        )

        private suspend fun fetchParsedUrlItem(parsedUrl: YouTubeUrlParser.ParsedUrl): YTItem? =
            when (parsedUrl) {
                is YouTubeUrlParser.ParsedUrl.Video -> {
                    YouTube
                        .next(WatchEndpoint(videoId = parsedUrl.id))
                        .getOrNull()
                        ?.items
                        ?.firstOrNull()
                }

                is YouTubeUrlParser.ParsedUrl.Playlist -> {
                    YouTube
                        .playlist(parsedUrl.id)
                        .getOrNull()
                        ?.playlist
                }

                is YouTubeUrlParser.ParsedUrl.Album -> {
                    val albumResult = YouTube.album("MPREb_${parsedUrl.id}")
                    if (albumResult.isSuccess) {
                        albumResult.getOrNull()?.album
                    } else {
                        YouTube
                            .playlist(parsedUrl.id)
                            .getOrNull()
                            ?.playlist
                    }
                }

                is YouTubeUrlParser.ParsedUrl.Artist -> {
                    if (parsedUrl.id.startsWith("MPRE")) {
                        YouTube
                            .artist(parsedUrl.id)
                            .getOrNull()
                            ?.artist
                    } else {
                        YouTube
                            .artist(parsedUrl.id)
                            .getOrNull()
                            ?.artist
                    }
                }
            }
    }

data class SearchSuggestionViewState(
    val query: String = "",
    val history: List<SearchHistory> = emptyList(),
    val suggestions: List<String> = emptyList(),
    val recommendedItems: List<YTItem> = emptyList(),
    val items: List<YTItem> = emptyList(),
    val parsedUrlItem: YTItem? = null,
    val isUrlQuery: Boolean = false,
)
