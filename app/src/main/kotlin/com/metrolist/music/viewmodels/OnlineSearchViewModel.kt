/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.viewmodels

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import com.metrolist.innertube.models.WatchEndpoint
import com.metrolist.innertube.models.YTItem
import com.metrolist.innertube.models.filterExplicit
import com.metrolist.innertube.models.filterVideoSongs
import com.metrolist.innertube.models.filterYoutubeShorts
import com.metrolist.innertube.pages.SearchSummary
import com.metrolist.innertube.pages.SearchSummaryPage
import com.metrolist.music.constants.HideExplicitKey
import com.metrolist.music.constants.HideVideoSongsKey
import com.metrolist.music.constants.HideYoutubeShortsKey
import com.metrolist.music.discovery.SearchDiscovery
import com.metrolist.music.models.ItemsPage
import com.metrolist.music.utils.dataStore
import com.metrolist.music.utils.get
import com.metrolist.music.utils.reportException
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
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
    var searchRevision by mutableIntStateOf(0)
        private set

    data class SongDiscovery(
        val seed: SongItem,
        val recommendations: List<SongItem> = emptyList(),
        val artists: List<ArtistItem> = emptyList(),
        val similarArtists: List<ArtistItem> = emptyList(),
        val albums: List<AlbumItem> = emptyList(),
        val playlists: List<PlaylistItem> = emptyList(),
    )

    var songDiscovery by mutableStateOf<SongDiscovery?>(null)
        private set

    private val matchingSongs: List<SongItem>
        get() = (viewStateMap[YouTube.SearchFilter.FILTER_SONG.value]?.items
            ?: summaryPage?.summaries.orEmpty().flatMap { it.items })
            .filterIsInstance<SongItem>()
            .filter { !it.isVideoSong && !it.isEpisode }
            .distinctBy { it.id }

    val songs: List<SongItem>
        get() = SearchDiscovery.songs(matchingSongs, songDiscovery?.seed, songDiscovery?.recommendations.orEmpty())

    fun sectionItems(filter: YouTube.SearchFilter): List<YTItem> {
        val results = viewStateMap[filter.value]?.items.orEmpty()
        return when (filter) {
            YouTube.SearchFilter.FILTER_SONG -> songs
            YouTube.SearchFilter.FILTER_ALBUM -> YouTube.mergeAlbums(
                (songDiscovery?.albums.orEmpty() + results).filterIsInstance<AlbumItem>(),
            )
            YouTube.SearchFilter.FILTER_COMMUNITY_PLAYLIST -> (songDiscovery?.playlists.orEmpty() + results).distinctBy { it.id }
            else -> results
        }
    }

    val topResult: SearchSummary?
        get() {
            val top = summaryPage?.summaries?.firstOrNull() ?: return null
            val songs = (viewStateMap[YouTube.SearchFilter.FILTER_SONG.value]?.items.orEmpty() +
                summaryPage?.summaries.orEmpty().flatMap { it.items })
                .filterIsInstance<SongItem>()
                .filter { !it.isVideoSong && !it.isEpisode }
                .distinctBy { it.id }
            val exactMatches = songs.filter { it.title.trim().equals(query.trim(), ignoreCase = true) }
            val promotedSongs = if (exactMatches.isNotEmpty()) {
                exactMatches
            } else if (top.items.any { it is SongItem && it.isVideoSong }) {
                songs.take(1)
            } else {
                emptyList()
            }
            val items = (promotedSongs + top.items.filterNot { it is SongItem && it.isVideoSong })
                .distinctBy { it.id }
            if (SearchDiscovery.duplicatesFirstSongs(items.firstOrNull(), this.songs)) return null
            return top.copy(items = items).takeIf { items.isNotEmpty() }
        }

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
                    currentCoroutineContext().ensureActive()
                    if (requestedQuery == query) {
                        val hideExplicit = context.dataStore.get(HideExplicitKey, false)
                        val hideVideoSongs = context.dataStore.get(HideVideoSongsKey, false)
                        val hideYoutubeShorts = context.dataStore.get(HideYoutubeShortsKey, false)
                        summaryPage = it.filterExplicit(hideExplicit)
                            .filterVideoSongs(hideVideoSongs)
                            .filterYoutubeShorts(hideYoutubeShorts)
                    }
                }.onFailure {
                    currentCoroutineContext().ensureActive()
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
        else -> listOf(Category.PLAYLIST, Category.SONG, Category.VIDEO, Category.ARTIST, Category.ALBUM, Category.PODCAST, Category.EPISODE, Category.PROFILE)
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
        val requestedQuery = query
        YouTube
            .search(requestedQuery, filter, includeExternalAlbums = true)
            .onSuccess { result ->
                currentCoroutineContext().ensureActive()
                if (requestedQuery != query) return@onSuccess
                val hideExplicit = context.dataStore.get(HideExplicitKey, false)
                val hideVideoSongs = context.dataStore.get(HideVideoSongsKey, false)
                val hideYoutubeShorts = context.dataStore.get(HideYoutubeShortsKey, false)
                viewStateMap[filter.value] =
                    ItemsPage(
                        result.items
                            .distinctBy { it.id }
                            .filterExplicit(hideExplicit)
                            .filterVideoSongs(hideVideoSongs || filter == YouTube.SearchFilter.FILTER_SONG)
                            .filterYoutubeShorts(hideYoutubeShorts),
                        result.continuation,
                    )
            }.onFailure {
                currentCoroutineContext().ensureActive()
                if (requestedQuery != query) return@onFailure
                val category = Category.entries.firstOrNull { filter in it.toFilters() }
                val summaryItems = summaryPage?.summaries.orEmpty()
                    .flatMap { it.items }
                    .filter { categoryOf(it) == category }
                    .distinctBy { it.id }
                viewStateMap[filter.value] = ItemsPage(summaryItems, null)
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
        val songDiscovery: SongDiscovery?,
    )
    private val queryCache = linkedMapOf<String, QueryCache>()

    fun loadSmartSearch() {
        if (smartSearchStarted || query.isBlank()) return
        smartSearchStarted = true
        searchJob = viewModelScope.launch {
            loadSummaryPage()
            currentCoroutineContext().ensureActive()
            val topCategory = categoryOf(summaryPage?.summaries?.firstOrNull()?.items?.firstOrNull())
            val hideVideoSongs = context.dataStore.get(HideVideoSongsKey, false)
            val priorityOrder = (listOf(Category.SONG) + categoryPriorityOrder(topCategory)).distinct()
            val order = priorityOrder
                .flatMap { it.toFilters() }
                .let { filters -> if (hideVideoSongs) filters.filter { it != YouTube.SearchFilter.FILTER_VIDEO } else filters }
                .filter { it != YouTube.SearchFilter.FILTER_PROFILE }
                .let { filters -> filters.filter { it != YouTube.SearchFilter.FILTER_VIDEO } +
                    filters.filter { it == YouTube.SearchFilter.FILTER_VIDEO } }
            smartSearchOrder = order

            val requestedQuery = query
            order.forEach { sectionFilter ->
                if (sectionFilter != YouTube.SearchFilter.FILTER_EPISODE) {
                    launch {
                        fetchAndStoreFilterResults(sectionFilter)
                        if (sectionFilter == YouTube.SearchFilter.FILTER_SONG) {
                            loadSongDiscovery(requestedQuery)
                        }
                    }
                }
            }
            if (viewStateMap[YouTube.SearchFilter.FILTER_EPISODE.value] == null) {
                viewStateMap[YouTube.SearchFilter.FILTER_EPISODE.value] = ItemsPage(episodesFromSummary(), null)
            }
        }
    }

    private suspend fun loadSongDiscovery(requestedQuery: String) {
        currentCoroutineContext().ensureActive()
        if (requestedQuery != query) return
        val seed = SearchDiscovery.recognizeSong(
            requestedQuery,
            matchingSongs,
            summaryPage?.summaries?.firstOrNull()?.items?.firstOrNull(),
        ) ?: return
        val credits = (seed.artists + matchingSongs.take(4).flatMap { it.artists })
            .filter { it.id?.startsWith("UC") == true }
            .distinctBy { it.id }
            .take(3)
        val creditedIds = credits.mapNotNull { it.id }.toSet()
        songDiscovery = SongDiscovery(
            seed = seed,
            artists = credits.map { artist ->
                ArtistItem(
                    id = requireNotNull(artist.id),
                    title = artist.name,
                    thumbnail = null,
                    shuffleEndpoint = null,
                    radioEndpoint = null,
                )
            },
        )
        val contextualOrder = listOf(
            YouTube.SearchFilter.FILTER_SONG,
            YouTube.SearchFilter.FILTER_ARTIST,
            YouTube.SearchFilter.FILTER_ALBUM,
        )
        smartSearchOrder = (contextualOrder + smartSearchOrder).distinct()
        val hideExplicit = context.dataStore.get(HideExplicitKey, false)
        val hideYoutubeShorts = context.dataStore.get(HideYoutubeShortsKey, false)
        coroutineScope {
            launch {
                val related = withTimeoutOrNull(12_000L) {
                    val endpoint = YouTube.next(seed.endpoint ?: WatchEndpoint(videoId = seed.id))
                        .getOrNull()?.relatedEndpoint ?: return@withTimeoutOrNull null
                    YouTube.related(endpoint).getOrNull()
                }
                currentCoroutineContext().ensureActive()
                if (requestedQuery != query || related == null) return@launch
                val current = songDiscovery ?: return@launch
                songDiscovery = current.copy(
                    recommendations = recommendedSongs(
                        related.songs + current.recommendations,
                        seed.id,
                        hideExplicit,
                        hideYoutubeShorts,
                    ),
                    similarArtists = SearchDiscovery.similarArtists(current.similarArtists + related.artists, creditedIds),
                    albums = (current.albums + related.albums.filterExplicit(hideExplicit))
                        .distinctBy { it.id }.take(12),
                    playlists = related.playlists.distinctBy { it.id }.take(12),
                )
            }
            launch {
                for (credit in credits) {
                    val page = withTimeoutOrNull(10_000L) { YouTube.artist(requireNotNull(credit.id)).getOrNull() }
                    currentCoroutineContext().ensureActive()
                    if (requestedQuery != query) return@launch
                    if (page == null) continue
                    val current = songDiscovery ?: return@launch
                    val relatedItems = page.sections.flatMap { it.items }
                    songDiscovery = current.copy(
                        recommendations = recommendedSongs(
                            current.recommendations + relatedItems.filterIsInstance<SongItem>(),
                            seed.id,
                            hideExplicit,
                            hideYoutubeShorts,
                        ),
                        artists = current.artists.map { if (it.id == page.artist.id) page.artist else it },
                        similarArtists = SearchDiscovery.similarArtists(
                            current.similarArtists + relatedItems.filterIsInstance<ArtistItem>(),
                            creditedIds,
                        ),
                        albums = (current.albums + relatedItems.filterIsInstance<AlbumItem>().filterExplicit(hideExplicit))
                            .distinctBy { it.id }.take(12),
                    )
                }
            }
        }
    }

    private fun recommendedSongs(
        items: List<SongItem>,
        seedId: String,
        hideExplicit: Boolean,
        hideYoutubeShorts: Boolean,
    ): List<SongItem> = items
        .filter { it.id != seedId && !it.isVideoSong && !it.isEpisode }
        .filterExplicit(hideExplicit)
        .filterYoutubeShorts(hideYoutubeShorts)
        .distinctBy { it.id }
        .take(SearchDiscovery.MAX_RECOMMENDATIONS)

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
                .filterVideoSongs(hideVideoSongs || currentFilter == YouTube.SearchFilter.FILTER_SONG)
                .filterYoutubeShorts(hideYoutubeShorts)
            viewStateMap[filterValue] = ItemsPage(
                (viewState.items + newItems).distinctBy { it.id },
                searchResult.continuation
            )
        }
    }

    fun search(newQuery: String, resetCarousels: Boolean = false) {
        if (newQuery == query) {
            if (resetCarousels) searchRevision++
            return
        }
        if (query.isNotBlank()) {
            queryCache[query] = QueryCache(
                summaryPage,
                viewStateMap.toMap(),
                smartSearchOrder,
                smartSearchStarted && searchJob?.isActive != true,
                songDiscovery,
            )
            while (queryCache.size > MAX_QUERY_CACHE) {
                queryCache.remove(queryCache.keys.first())
            }
        }
        searchJob?.cancel()
        summaryJob?.cancel()
        query = newQuery
        searchRevision++
        filter.value = null
        viewStateMap.clear()
        val cached = queryCache[newQuery]
        if (cached != null) {
            summaryPage = cached.summaryPage
            viewStateMap.putAll(cached.viewStateMap)
            smartSearchOrder = cached.smartSearchOrder
            smartSearchStarted = cached.smartSearchStarted
            songDiscovery = cached.songDiscovery
        } else {
            summaryPage = null
            smartSearchOrder = emptyList()
            smartSearchStarted = false
            songDiscovery = null
        }
    }

    private companion object {
        const val MAX_QUERY_CACHE = 8
    }
}
