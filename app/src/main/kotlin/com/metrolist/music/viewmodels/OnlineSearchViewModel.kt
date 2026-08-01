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
    // Mutable (not just constructor-read) so the inline New Iride UI results view can reuse a
    // single instance across multiple submitted queries via search() instead of relying on a
    // fresh nav backstack entry (and fresh ViewModel) per query like the classic route does.
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

    // Only the top-result card (summaries.first(), YT always returns it first) is used —
    // as a pure query-intent signal for loadSmartSearch's ordering — so unlike the old "All"
    // tab this doesn't need every shelf enriched with extra network calls.
    private suspend fun loadSummaryPage() {
        if (query.isBlank()) return
        if (summaryPage == null) {
            YouTube
                .searchSummary(query)
                .onSuccess {
                    val hideExplicit = context.dataStore.get(HideExplicitKey, false)
                    val hideVideoSongs = context.dataStore.get(HideVideoSongsKey, false)
                    val hideYoutubeShorts = context.dataStore.get(HideYoutubeShortsKey, false)
                    summaryPage = it.filterExplicit(hideExplicit)
                        .filterVideoSongs(hideVideoSongs)
                        .filterYoutubeShorts(hideYoutubeShorts)
                }.onFailure {
                    reportException(it)
                }
        }
    }

    // "Top result" tells us what the query is really about (an artist, a song, an album, ...),
    // used to rank Smart Search's category order (see categoryPriorityOrder).
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

    // Ranks categories relative to what the query is actually about, used by loadSmartSearch
    // to order its sections (most relevant category first).
    private fun categoryPriorityOrder(topCategory: Category): List<Category> = when (topCategory) {
        // Artist query: their songs and albums matter more than the artist card itself.
        Category.ARTIST -> listOf(Category.SONG, Category.ALBUM, Category.PLAYLIST, Category.VIDEO, Category.PODCAST, Category.PROFILE, Category.ARTIST, Category.EPISODE)
        // Album query: the album, then its tracks/artist.
        Category.ALBUM -> listOf(Category.ALBUM, Category.SONG, Category.ARTIST, Category.PLAYLIST, Category.VIDEO, Category.PODCAST, Category.PROFILE, Category.EPISODE)
        // Playlist/podcast queries: keep collections and their episodes up front.
        Category.PLAYLIST -> listOf(Category.PLAYLIST, Category.PODCAST, Category.SONG, Category.ARTIST, Category.ALBUM, Category.VIDEO, Category.EPISODE, Category.PROFILE)
        Category.PODCAST -> listOf(Category.PODCAST, Category.EPISODE, Category.PLAYLIST, Category.ARTIST, Category.SONG, Category.ALBUM, Category.VIDEO, Category.PROFILE)
        Category.PROFILE -> listOf(Category.PROFILE, Category.PLAYLIST, Category.SONG, Category.VIDEO, Category.ARTIST, Category.ALBUM, Category.PODCAST, Category.EPISODE)
        // Song/video/unclassified query: default YT Music ordering.
        else -> listOf(Category.SONG, Category.VIDEO, Category.ARTIST, Category.ALBUM, Category.PLAYLIST, Category.PODCAST, Category.EPISODE, Category.PROFILE)
    }

    // Community vs featured playlists share one Category, but Smart Search needs both as
    // separate sections/filters, so a category can expand to more than one filter.
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

    // Episodes matched by content type, not by title text: the dedicated-filter branch
    // below already learned the hard way that YT's shelf titles are localized, so matching
    // a literal "Episodes" string silently breaks on any non-English account language.
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
                    // The FILTER_EPISODE API returns episodes in a format that differs from the
                    // summary search: playlistItemData is absent and the subtitle structure is
                    // different, making reliable isEpisode detection fail for many items.
                    // Reuse the "Episodes" section from the summary page instead — it is already
                    // parsed correctly by fromMusicResponsiveListItemRenderer.
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

    // Smart Search: instead of relying on YT's own truncated summary shelves, fetch every
    // category's dedicated filter endpoint directly (same one backing each filter pill), so
    // every section shows a full page of real results, not just the 3-5 items YT's summary
    // groups items into. Ordered by query-intent priority, computed once the summary's
    // top-result card (used purely as an intent signal) is available.
    var smartSearchOrder by mutableStateOf<List<YouTube.SearchFilter>>(emptyList())
        private set
    private var smartSearchStarted = false

    // In-flight fetches for the *current* query — cancelled wholesale on search(), so an old
    // query's slow network calls can no longer land after a newer query has already taken over
    // (they used to keep running and write into viewStateMap after the fact).
    private var searchJob: Job? = null

    // Bounded per-query cache: switching back to a query already loaded this session restores
    // instantly instead of re-fetching every shelf from network again. FIFO eviction once full —
    // no need for real LRU/recency tracking at this size.
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
            loadSummaryPage()
            val hideVideoSongs = context.dataStore.get(HideVideoSongsKey, false)
            val topCategory = categoryOf(summaryPage?.summaries?.firstOrNull()?.items?.firstOrNull())
            val order = categoryPriorityOrder(topCategory)
                .flatMap { it.toFilters() }
                .let { filters -> if (hideVideoSongs) filters.filter { it != YouTube.SearchFilter.FILTER_VIDEO } else filters }
                .filter { it != YouTube.SearchFilter.FILTER_PROFILE }
            smartSearchOrder = order

            order.forEach { sectionFilter ->
                if (sectionFilter == YouTube.SearchFilter.FILTER_EPISODE) {
                    if (viewStateMap[sectionFilter.value] == null) {
                        viewStateMap[sectionFilter.value] = ItemsPage(episodesFromSummary(), null)
                    }
                } else {
                    // Launched as a child of this coroutine (not viewModelScope directly) so
                    // cancelling searchJob on the next search() cancels these too.
                    launch { fetchAndStoreFilterResults(sectionFilter) }
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

    // Reuses this instance for a brand-new query — used by the inline New Iride UI results view,
    // which stays on the same "search_input" screen (and thus the same ViewModel) across many
    // submitted queries instead of getting a fresh nav backstack entry per query like the classic
    // route does.
    fun search(newQuery: String) {
        if (newQuery == query) return
        if (query.isNotBlank()) {
            queryCache[query] = QueryCache(summaryPage, viewStateMap.toMap(), smartSearchOrder, smartSearchStarted)
            while (queryCache.size > MAX_QUERY_CACHE) {
                queryCache.remove(queryCache.keys.first())
            }
        }
        searchJob?.cancel()
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
