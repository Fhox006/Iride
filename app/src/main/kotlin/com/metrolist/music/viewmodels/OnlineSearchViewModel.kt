/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.viewmodels

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.metrolist.innertube.YouTube
import com.metrolist.innertube.models.YTItem
import com.metrolist.innertube.models.filterExplicit
import com.metrolist.innertube.models.filterVideoSongs
import com.metrolist.innertube.models.filterYoutubeShorts
import com.metrolist.music.constants.HideExplicitKey
import com.metrolist.music.constants.HideVideoSongsKey
import com.metrolist.music.constants.HideYoutubeShortsKey
import com.metrolist.music.models.ItemsPage
import com.metrolist.music.utils.dataStore
import com.metrolist.music.utils.get
import com.metrolist.music.utils.reportException
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import java.net.URLDecoder
import javax.inject.Inject

@HiltViewModel
class OnlineSearchViewModel
@Inject
constructor(
    @ApplicationContext val context: Context,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    val query = try {
        URLDecoder.decode(savedStateHandle.get<String>("query")!!, "UTF-8")
    } catch (e: IllegalArgumentException) {
        savedStateHandle.get<String>("query")!!
    }
    val filter = MutableStateFlow<YouTube.SearchFilter?>(null)
    val viewStateMap = mutableStateMapOf<String, ItemsPage?>()

    // Sentinel value for the "Best Results" tab — not a real YouTube filter
    private val FILTER_BEST_RESULTS_SENTINEL = "best_results_sentinel"

    // Each entry: section title (string) → list of YTItems
    var bestResultsSections by mutableStateOf<List<Pair<String, List<YTItem>>>>(emptyList())
        private set

    private var bestResultsLoaded = false

    fun loadBestResults() {
        if (bestResultsLoaded) return
        bestResultsLoaded = true
        viewModelScope.launch {
            val hideExplicit = context.dataStore.get(HideExplicitKey, false)
            val hideVideoSongs = context.dataStore.get(HideVideoSongsKey, false)
            val hideYoutubeShorts = context.dataStore.get(HideYoutubeShortsKey, false)

            val filtersToLoad = buildList {
                add(YouTube.SearchFilter.FILTER_ARTIST to "Artists")
                add(YouTube.SearchFilter.FILTER_SONG to "Songs")
                add(YouTube.SearchFilter.FILTER_ALBUM to "Albums")
                add(YouTube.SearchFilter.FILTER_COMMUNITY_PLAYLIST to "Community playlists")
                add(YouTube.SearchFilter.FILTER_FEATURED_PLAYLIST to "Featured playlists")
                if (!hideVideoSongs) add(YouTube.SearchFilter.FILTER_VIDEO to "Videos")
                add(YouTube.SearchFilter.FILTER_PODCAST to "Podcasts")
                add(YouTube.SearchFilter.FILTER_EPISODE to "Episodes")
                add(YouTube.SearchFilter.FILTER_PROFILE to "Profiles")
            }

            val sections = mutableListOf<Pair<String, List<YTItem>>>()

            for ((ytFilter, sectionTitle) in filtersToLoad) {
                YouTube.search(query, ytFilter)
                    .onSuccess { result ->
                        val items = result.items
                            .distinctBy { it.id }
                            .filterExplicit(hideExplicit)
                            .filterVideoSongs(hideVideoSongs)
                            .filterYoutubeShorts(hideYoutubeShorts)
                        if (items.isNotEmpty()) {
                            sections.add(sectionTitle to items)
                        }
                        // Cache in viewStateMap so individual filter tabs are free
                        viewStateMap[ytFilter.value] = ItemsPage(items, result.continuation)
                    }
                    .onFailure { reportException(it) }
            }

            bestResultsSections = sections
        }
    }

    init {
        viewModelScope.launch {
            filter.collect { filter ->
                when {
                    filter == null -> {
                        // "Best Results" tab
                        loadBestResults()
                    }
                    filter == YouTube.SearchFilter.FILTER_EPISODE -> {
                        if (viewStateMap[filter.value] == null) {
                            // Try to reuse cached data from bestResults load, else fetch directly
                            if (bestResultsSections.isNotEmpty()) {
                                val episodes = bestResultsSections
                                    .firstOrNull { it.first == "Episodes" }
                                    ?.second.orEmpty()
                                viewStateMap[filter.value] = ItemsPage(episodes, null)
                            } else {
                                YouTube.search(query, filter)
                                    .onSuccess { result ->
                                        val hideExplicit = context.dataStore.get(HideExplicitKey, false)
                                        val hideVideoSongs = context.dataStore.get(HideVideoSongsKey, false)
                                        val hideYoutubeShorts = context.dataStore.get(HideYoutubeShortsKey, false)
                                        viewStateMap[filter.value] = ItemsPage(
                                            result.items
                                                .distinctBy { it.id }
                                                .filterExplicit(hideExplicit)
                                                .filterVideoSongs(hideVideoSongs)
                                                .filterYoutubeShorts(hideYoutubeShorts),
                                            result.continuation
                                        )
                                    }.onFailure { reportException(it) }
                            }
                        }
                    }
                    else -> {
                        if (viewStateMap[filter.value] == null) {
                            YouTube.search(query, filter)
                                .onSuccess { result ->
                                    val hideExplicit = context.dataStore.get(HideExplicitKey, false)
                                    val hideVideoSongs = context.dataStore.get(HideVideoSongsKey, false)
                                    val hideYoutubeShorts = context.dataStore.get(HideYoutubeShortsKey, false)
                                    viewStateMap[filter.value] = ItemsPage(
                                        result.items
                                            .distinctBy { it.id }
                                            .filterExplicit(hideExplicit)
                                            .filterVideoSongs(hideVideoSongs)
                                            .filterYoutubeShorts(hideYoutubeShorts),
                                        result.continuation,
                                    )
                                }.onFailure { reportException(it) }
                        }
                    }
                }
            }
        }
    }

    fun loadMore() {
        val currentFilter = filter.value
        val filterValue = currentFilter?.value ?: return
        viewModelScope.launch {
            val viewState = viewStateMap[filterValue] ?: return@launch
            val continuation = viewState.continuation ?: return@launch
            val searchResult =
                YouTube.searchContinuation(continuation).getOrNull() ?: return@launch
            val hideExplicit = context.dataStore.get(HideExplicitKey, false)
            val hideVideoSongs = context.dataStore.get(HideVideoSongsKey, false)
            val hideYoutubeShorts = context.dataStore.get(HideYoutubeShortsKey, false)
            val newItems = searchResult.items
                .filterExplicit(hideExplicit)
                .filterVideoSongs(hideVideoSongs)
                .filterYoutubeShorts(hideYoutubeShorts)
            viewStateMap[filterValue] = ItemsPage(
                (viewState.items + newItems).distinctBy { it.id },
                searchResult.continuation
            )
        }
    }
}
