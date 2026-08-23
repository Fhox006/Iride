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
import com.metrolist.innertube.models.AlbumItem
import com.metrolist.innertube.models.ArtistItem
import com.metrolist.innertube.models.EpisodeItem
import com.metrolist.innertube.models.PlaylistItem
import com.metrolist.innertube.models.PodcastItem
import com.metrolist.innertube.models.SongItem
import com.metrolist.innertube.models.YTItem
import com.metrolist.innertube.models.filterExplicit
import com.metrolist.innertube.models.filterVideoSongs
import com.metrolist.innertube.models.filterYoutubeShorts
import com.metrolist.innertube.pages.SearchSummaryPage
import com.metrolist.music.constants.HideExplicitKey
import com.metrolist.music.constants.HideVideoSongsKey
import com.metrolist.music.constants.HideYoutubeShortsKey
import com.metrolist.music.models.ItemsPage
import com.metrolist.music.utils.dataStore
import com.metrolist.music.utils.get
import com.metrolist.music.utils.reportException
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
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
    var query: String by mutableStateOf(
        savedStateHandle.get<String>("query")?.let {
            try {
                URLDecoder.decode(it, "UTF-8")
            } catch (e: IllegalArgumentException) {
                it
            }
        } ?: "",
    )
        private set
    val filter = MutableStateFlow<YouTube.SearchFilter?>(null)
    var summaryPage by mutableStateOf<SearchSummaryPage?>(null)
    val viewStateMap = mutableStateMapOf<String, ItemsPage?>()

    private var summaryJob: Job? = null

    private suspend fun loadSummaryPage() {
        if (query.isBlank()) return
        if (summaryPage == null) {
            val existing = summaryJob
            if (existing?.isActive == true) {
                existing.join()
            } else {
                launchSummaryFetch().join()
            }
        }
    }

    private fun launchSummaryFetch(): Job {
        val job = viewModelScope.launch {
            val requestedQuery = query
            YouTube
                .searchSummary(requestedQuery)
                .onSuccess {
                    if (requestedQuery == query) {
                        val hideExplicit = context.dataStore.get(HideExplicitKey, false)
                        val hideVideoSongs = context.dataStore.get(HideVideoSongsKey, false)
                        val hideYoutubeShorts = context.dataStore.get(HideYoutubeShortsKey, false)
                        summaryPage = it.filterExplicit(hideExplicit)
                            .filterVideoSongs(hideVideoSongs)
                            .filterYoutubeShorts(hideYoutubeShorts)
                    }
                }.onFailure {
                    reportException(it)
                }
        }
        summaryJob = job
        job.invokeOnCompletion { if (summaryJob === job) summaryJob = null }
        return job
    }

    private enum class Category { SONG, VIDEO, ALBUM, ARTIST, PLAYLIST, PODCAST, EPISODE, PROFILE, OTHER }

    private fun categoryOf(item: YTItem?): Category = when (item) {
        is SongItem -> if (item.isVideoSong) Category.VIDEO else Category.SONG
        is AlbumItem -> Category.ALBUM
        is ArtistItem -> if (item.isProfile) Category.PROFILE else Category.ARTIST
        is PlaylistItem -> Category.PLAYLIST
        is PodcastItem -> Category.PODCAST
        is EpisodeItem -> Category.EPISODE
        null -> Category.OTHER
    }

    private fun categoryPriorityOrder(topCategory: Category): List<Category> = when (topCategory) {
        Category.ARTIST -> listOf(Category.SONG, Category.ALBUM, Category.PLAYLIST, Category.VIDEO, Category.PODCAST, Category.PROFILE, Category.ARTIST, Category.EPISODE)
        Category.ALBUM -> listOf(Category.ALBUM, Category.SONG, Category.ARTIST, Category.PLAYLIST, Category.VIDEO, Category.PODCAST, Category.PROFILE, Category.EPISODE)
        Category.PLAYLIST -> listOf(Category.PLAYLIST, Category.PODCAST, Category.SONG, Category.ARTIST, Category.ALBUM, Category.VIDEO, Category.EPISODE, Category.PROFILE)
        Category.PODCAST -> listOf(Category.PODCAST, Category.EPISODE, Category.PLAYLIST, Category.ARTIST, Category.SONG, Category.ALBUM, Category.VIDEO, Category.PROFILE)
        Category.PROFILE -> listOf(Category.PROFILE, Category.PLAYLIST, Category.SONG, Category.VIDEO, Category.ARTIST, Category.ALBUM, Category.PODCAST, Category.EPISODE)
        else -> listOf(Category.SONG, Category.VIDEO, Category.ARTIST, Category.ALBUM, Category.PLAYLIST, Category.PODCAST, Category.EPISODE, Category.PROFILE)
    }

    private fun Category.toFilters(): List<YouTube.SearchFilter> = when (this) {
        Category.SONG -> listOf(YouTube.SearchFilter.FILTER_SONG)
        Category.VIDEO -> listOf(YouTube.SearchFilter.FILTER_VIDEO)
        Category.ALBUM -> listOf(YouTube.SearchFilter.FILTER_ALBUM)
        Category.ARTIST -> listOf(YouTube.SearchFilter.FILTER_ARTIST)
        Category.PROFILE -> listOf(YouTube.SearchFilter.FILTER_PROFILE)
        Category.PODCAST -> listOf(YouTube.SearchFilter.FILTER_PODCAST)
        Category.PLAYLIST -> listOf(YouTube.SearchFilter.FILTER_COMMUNITY_PLAYLIST, YouTube.SearchFilter.FILTER_FEATURED_PLAYLIST)
        Category.EPISODE -> listOf(YouTube.SearchFilter.FILTER_EPISODE)
        Category.OTHER -> emptyList()
    }

    private fun episodesFromSummary(): List<YTItem> =
        summaryPage?.summaries
            ?.firstOrNull { categoryOf(it.items.firstOrNull()) == Category.EPISODE }
            ?.items
            .orEmpty()

    init {
        viewModelScope.launch {
            filter.collect { filter ->
                if (filter == null) {
                    loadSummaryPage()
                } else if (filter == YouTube.SearchFilter.FILTER_EPISODE) {
                    if (viewStateMap[filter.value] == null) {
                        loadSummaryPage()
                        viewStateMap[filter.value] = ItemsPage(episodesFromSummary(), null)
                    }
                } else {
                    fetchAndStoreFilterResults(filter)
                }
            }
        }
    }

    private suspend fun fetchAndStoreFilterResults(filter: YouTube.SearchFilter) {
        if (query.isBlank()) return
        if (viewStateMap[filter.value] != null) return
        YouTube
            .search(query, filter)
            .onSuccess { result ->
                val hideExplicit = context.dataStore.get(HideExplicitKey, false)
                val hideVideoSongs = context.dataStore.get(HideVideoSongsKey, false)
                val hideYoutubeShorts = context.dataStore.get(HideYoutubeShortsKey, false)
                viewStateMap[filter.value] =
                    ItemsPage(
                        result.items
                            .distinctBy { it.id }
                            .filterExplicit(hideExplicit)
                            .filterVideoSongs(hideVideoSongs)
                            .filterYoutubeShorts(hideYoutubeShorts),
                        result.continuation,
                    )
            }.onFailure {
                reportException(it)
            }
    }

    var smartSearchOrder by mutableStateOf<List<YouTube.SearchFilter>>(emptyList())
        private set
    private var smartSearchStarted = false

    private var searchJob: Job? = null

    private data class QueryCache(
        val summaryPage: SearchSummaryPage?,
        val viewStateMap: Map<String, ItemsPage?>,
        val smartSearchOrder: List<YouTube.SearchFilter>,
        val smartSearchStarted: Boolean,
    )
    private val queryCache = linkedMapOf<String, QueryCache>()

    fun loadSmartSearch() {
        if (smartSearchStarted || query.isBlank()) return
        smartSearchStarted = true
        searchJob = viewModelScope.launch {
            val hideVideoSongs = context.dataStore.get(HideVideoSongsKey, false)
            val order = categoryPriorityOrder(Category.SONG)
                .flatMap { it.toFilters() }
                .let { filters -> if (hideVideoSongs) filters.filter { it != YouTube.SearchFilter.FILTER_VIDEO } else filters }
                .filter { it != YouTube.SearchFilter.FILTER_PROFILE }
            smartSearchOrder = order

            order.forEach { sectionFilter ->
                if (sectionFilter != YouTube.SearchFilter.FILTER_EPISODE) {
                    launch { fetchAndStoreFilterResults(sectionFilter) }
                }
            }
            loadSummaryPage()
            if (viewStateMap[YouTube.SearchFilter.FILTER_EPISODE.value] == null) {
                viewStateMap[YouTube.SearchFilter.FILTER_EPISODE.value] = ItemsPage(episodesFromSummary(), null)
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

    fun search(newQuery: String) {
        if (newQuery == query) return
        if (query.isNotBlank()) {
            queryCache[query] = QueryCache(summaryPage, viewStateMap.toMap(), smartSearchOrder, smartSearchStarted)
            while (queryCache.size > MAX_QUERY_CACHE) {
                queryCache.remove(queryCache.keys.first())
            }
        }
        searchJob?.cancel()
        summaryJob?.cancel()
        query = newQuery
        filter.value = null
        viewStateMap.clear()
        val cached = queryCache[newQuery]
        if (cached != null) {
            summaryPage = cached.summaryPage
            viewStateMap.putAll(cached.viewStateMap)
            smartSearchOrder = cached.smartSearchOrder
            smartSearchStarted = cached.smartSearchStarted
        } else {
            summaryPage = null
            smartSearchOrder = emptyList()
            smartSearchStarted = false
        }
        searchJob = viewModelScope.launch { loadSummaryPage() }
    }

    private companion object {
        const val MAX_QUERY_CACHE = 8
    }
}
