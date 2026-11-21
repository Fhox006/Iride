/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.viewmodels

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.derivedStateOf
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
import com.metrolist.music.discovery.searchPersonalCatalog
import com.metrolist.music.discovery.SearchDiscovery
import com.metrolist.music.ui.screens.search.searchTopItems
import com.metrolist.music.ui.screens.search.searchSongContextItems
import com.metrolist.music.ui.screens.search.isSearchMusic
import com.metrolist.music.ui.screens.search.rankSearchSuggestions
import com.metrolist.music.db.MusicDatabase
import com.metrolist.innertube.models.Artist
import com.metrolist.music.db.entities.Song
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import com.metrolist.music.models.ItemsPage
import com.metrolist.music.utils.dataStore
import com.metrolist.music.utils.get
import com.metrolist.music.utils.reportException
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.IOException
import kotlinx.coroutines.Job
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.first
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
    private val database: MusicDatabase,
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
    private var pinnedSuggestions: List<YTItem> by mutableStateOf(emptyList())
    private var pinnedSuggestionsQuery: String by mutableStateOf("")
    private var selectedSong: SongItem? by mutableStateOf(null)
    private var personalItems: List<YTItem> by mutableStateOf(emptyList())
    private val personalSongs: List<SongItem> get() = personalItems.filterIsInstance<SongItem>()
    private val networkRequests = Semaphore(3)
    private var discoveryRevision: Int? = null
    private var requestGeneration = 0
    private var remoteSuggestions: List<YTItem> by mutableStateOf(emptyList())

    data class SongDiscovery(
        val seed: SongItem,
        val recommendations: List<SongItem> = emptyList(),
        val artists: List<ArtistItem> = emptyList(),
        val similarArtists: List<ArtistItem> = emptyList(),
        val albums: List<AlbumItem> = emptyList(),
        val playlists: List<PlaylistItem> = emptyList(),
        val complete: Boolean = false,
    )

    var songDiscovery by mutableStateOf<SongDiscovery?>(null)
        private set

    private val matchingSongs: List<SongItem>
        get() = (viewStateMap[YouTube.SearchFilter.FILTER_SONG.value]?.items.orEmpty() +
            summaryPage?.summaries.orEmpty().flatMap { it.items })
            .filterIsInstance<SongItem>()
            .filter { !it.isVideoSong && !it.isEpisode }
            .distinctBy { it.id }

    val songs: List<SongItem>
        get() = SearchDiscovery.songs(matchingSongs, songDiscovery?.seed, songDiscovery?.recommendations.orEmpty())

    fun sectionItems(filter: YouTube.SearchFilter): List<YTItem> {
        val summaryItems = summaryPage?.summaries.orEmpty().flatMap { it.items }.filter { item ->
            when (filter) {
                YouTube.SearchFilter.FILTER_ALBUM -> item is AlbumItem
                YouTube.SearchFilter.FILTER_ARTIST -> item is ArtistItem
                YouTube.SearchFilter.FILTER_COMMUNITY_PLAYLIST -> item is PlaylistItem
                YouTube.SearchFilter.FILTER_VIDEO -> item is SongItem && item.isVideoSong && !item.isEpisode
                else -> false
            }
        }
        val results = (viewStateMap[filter.value]?.items.orEmpty() + summaryItems).distinctBy { it.id }
        val section = when (filter) {
            YouTube.SearchFilter.FILTER_SONG -> songs
            YouTube.SearchFilter.FILTER_ALBUM -> YouTube.mergeAlbums(
                (songDiscovery?.albums.orEmpty() + results).filterIsInstance<AlbumItem>(),
            )
            YouTube.SearchFilter.FILTER_COMMUNITY_PLAYLIST ->
                SearchDiscovery.playlists(songDiscovery?.playlists.orEmpty(), results.filterIsInstance<PlaylistItem>())
            YouTube.SearchFilter.FILTER_ARTIST -> SearchDiscovery.artists(
                songDiscovery?.artists.orEmpty(), songDiscovery?.similarArtists.orEmpty(), results.filterIsInstance<ArtistItem>(),
            )
            else -> results
        }
        val topResultIds = topResult?.items.orEmpty().asSequence().map { it.id }.toSet()
        return section.filter { it.isSearchMusic() || (filter == YouTube.SearchFilter.FILTER_VIDEO && it is SongItem && !it.isEpisode) }
            .filterNot { filter != YouTube.SearchFilter.FILTER_ARTIST && it.id in topResultIds }
    }

    val topResult: SearchSummary? by derivedStateOf {
            val pinned = pinnedSuggestions.takeIf {
                pinnedSuggestionsQuery.equals(query, ignoreCase = true)
            }.orEmpty().take(5)
            val top = summaryPage?.summaries?.firstOrNull()
            val discovery = songDiscovery
            val seed = discovery?.seed ?: selectedSong
            val all = remoteSuggestions + pinned + summaryPage?.summaries.orEmpty().flatMap { it.items } + matchingSongs
            val items = if (seed != null) {
                val artistIds = seed.artists.mapNotNull { it.id }.toSet()
                val familiar = personalSongs.filter { song -> song.artists.any { it.id in artistIds } }
                val relatedMatches = matchingSongs.filter { song -> song.artists.any { it.id in artistIds } }
                val artists = discovery?.artists.orEmpty().ifEmpty {
                    seed.artists.filter { it.id?.startsWith("UC") == true }.take(1).map {
                        ArtistItem(requireNotNull(it.id), it.name, null, shuffleEndpoint = null, radioEndpoint = null)
                    }
                }
                searchSongContextItems(seed, artists, familiar, relatedMatches, discovery?.recommendations.orEmpty())
            } else {
                rankSearchSuggestions(query, personalItems, all)
            }
            SearchSummary(top?.title ?: context.getString(com.metrolist.music.R.string.search_recommended), items)
                .takeIf { items.isNotEmpty() }
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
            val requestedRevision = requestGeneration
            request { YouTube.searchSummary(requestedQuery) }
                .onSuccess {
                    currentCoroutineContext().ensureActive()
                    if (requestedQuery == query && requestedRevision == requestGeneration) {
                        val hideExplicit = context.dataStore.get(HideExplicitKey, false)
                        val hideVideoSongs = context.dataStore.get(HideVideoSongsKey, false)
                        val hideYoutubeShorts = context.dataStore.get(HideYoutubeShortsKey, false)
                        summaryPage = it.copy(summaries = it.summaries.mapNotNull { section ->
                            section.copy(items = section.items.filter { item -> item.isSearchMusic() || item is SongItem && !item.isEpisode })
                                .takeIf { section -> section.items.isNotEmpty() }
                        }).filterExplicit(hideExplicit)
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
            database.searchPersonalCatalog().collect { catalog ->
                personalItems = catalog.filterExplicit(context.dataStore.get(HideExplicitKey, false))
                    .filterYoutubeShorts(context.dataStore.get(HideYoutubeShortsKey, false))
            }
        }

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
        if (query.isBlank() || filter == YouTube.SearchFilter.FILTER_PODCAST || filter == YouTube.SearchFilter.FILTER_EPISODE) return
        if (viewStateMap[filter.value] != null) return
        val requestedQuery = query
        val requestedRevision = requestGeneration
        val seed = selectedSong ?: songDiscovery?.seed
        val lookup = if (seed != null) {
            when (filter) {
                YouTube.SearchFilter.FILTER_ARTIST, YouTube.SearchFilter.FILTER_ALBUM -> seed.artists.firstOrNull()?.name ?: requestedQuery
                else -> "${seed.title} ${seed.artists.joinToString(" ") { it.name }}"
            }
        } else {
            personalItems.filterIsInstance<ArtistItem>().firstOrNull {
                com.metrolist.music.ui.screens.search.searchPredictionScore(requestedQuery, it.title)?.let { score -> score <= 1 } == true
            }?.title ?: requestedQuery
        }
        request { YouTube.search(lookup, filter) }
            .onSuccess { result ->
                currentCoroutineContext().ensureActive()
                if (requestedQuery != query || requestedRevision != requestGeneration) return@onSuccess
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
                if (requestedQuery != query || requestedRevision != requestGeneration) return@onFailure
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
        val pinnedSuggestions: List<YTItem>,
        val remoteSuggestions: List<YTItem>,
        val selectedSong: SongItem?,
    )
    private val queryCache = linkedMapOf<String, QueryCache>()

    fun loadSmartSearch() {
        if (smartSearchStarted || query.isBlank()) return
        smartSearchStarted = true
        val requestedQuery = query
        smartSearchOrder = listOf(
            YouTube.SearchFilter.FILTER_SONG, YouTube.SearchFilter.FILTER_ALBUM,
            YouTube.SearchFilter.FILTER_COMMUNITY_PLAYLIST, YouTube.SearchFilter.FILTER_ARTIST,
            YouTube.SearchFilter.FILTER_FEATURED_PLAYLIST,
        ) + if (context.dataStore.get(HideVideoSongsKey, false)) emptyList() else listOf(YouTube.SearchFilter.FILTER_VIDEO)
        searchJob = viewModelScope.launch {
            if (selectedSong != null) launch { loadSongDiscovery(requestedQuery) }
            launch {
                loadSummaryPage()
                loadSongDiscovery(requestedQuery)
            }
            fetchAndStoreFilterResults(YouTube.SearchFilter.FILTER_SONG)
            launch { loadSongDiscovery(requestedQuery) }
            if (pinnedSuggestions.isEmpty()) launch {
                val revision = requestGeneration
                val result = request { YouTube.searchSuggestions(requestedQuery) }.getOrNull()
                if (requestedQuery == query && revision == requestGeneration) {
                    remoteSuggestions = result?.recommendedItems.orEmpty().filter { it.isSearchMusic() }
                        .filterExplicit(context.dataStore.get(HideExplicitKey, false))
                }
            }
            smartSearchOrder.filter { it != YouTube.SearchFilter.FILTER_SONG }.forEach { sectionFilter ->
                launch { fetchAndStoreFilterResults(sectionFilter) }
            }
        }
    }

    private suspend fun <T> request(block: suspend () -> Result<T>): Result<T> = networkRequests.withPermit {
        withContext(Dispatchers.IO) {
            withTimeoutOrNull(10_000L) { block() } ?: Result.failure(IOException("Search request timed out"))
        }
    }

    private suspend fun loadSongDiscovery(requestedQuery: String) {
        currentCoroutineContext().ensureActive()
        if (requestedQuery != query) return
        val seed = selectedSong ?: SearchDiscovery.recognizeSong(
            requestedQuery,
            matchingSongs,
            summaryPage?.summaries?.firstOrNull()?.items?.firstOrNull(),
        ) ?: return
        val requestedRevision = requestGeneration
        if (discoveryRevision == requestedRevision || (songDiscovery?.seed?.id == seed.id && songDiscovery?.complete == true)) return
        discoveryRevision = requestedRevision
        val credits = SearchDiscovery.credits(seed, matchingSongs + personalSongs, personalItems.filterIsInstance<ArtistItem>())
        val creditedIds = credits.mapNotNull { it.id }.toSet()
        songDiscovery = SongDiscovery(
            seed = seed.copy(artists = credits.ifEmpty { seed.artists }),
            recommendations = personalSongs.filter { song -> song.id != seed.id && song.artists.any { it.id in creditedIds } }.take(20),
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
        val hideExplicit = context.dataStore.get(HideExplicitKey, false)
        val hideYoutubeShorts = context.dataStore.get(HideYoutubeShortsKey, false)
        try {
        coroutineScope {
            SearchDiscovery.missingCredits(seed, credits).forEach { name ->
                launch {
                    val artist = request { YouTube.search(name, YouTube.SearchFilter.FILTER_ARTIST) }.getOrNull()
                        ?.items?.filterIsInstance<ArtistItem>()
                        ?.firstOrNull { it.title.equals(name, ignoreCase = true) && !it.isProfile } ?: return@launch
                    if (requestedQuery != query || requestedRevision != requestGeneration) return@launch
                    songDiscovery?.let { current ->
                        songDiscovery = current.copy(
                            seed = current.seed.copy(artists = (current.seed.artists + Artist(artist.title, artist.id)).distinctBy { it.id }),
                            artists = (current.artists + artist).distinctBy { it.id },
                        )
                    }
                }
            }
            seed.album?.id?.takeIf { it.startsWith("MPRE") }?.let { albumId ->
                launch {
                    val album = request { YouTube.album(albumId) }.getOrNull()?.album ?: return@launch
                    if (requestedQuery == query && requestedRevision == requestGeneration) {
                        songDiscovery?.let { current -> songDiscovery = current.copy(albums = (listOf(album) + current.albums).distinctBy { it.id }) }
                    }
                }
            }
            launch {
                val radio = request { YouTube.next(WatchEndpoint(videoId = seed.id)) }.getOrNull()
                currentCoroutineContext().ensureActive()
                if (requestedQuery != query || requestedRevision != requestGeneration) return@launch
                songDiscovery?.let { current ->
                    songDiscovery = current.copy(recommendations = recommendedSongs(
                        current.recommendations + radio?.items.orEmpty(), seed.id, hideExplicit, hideYoutubeShorts,
                    ))
                }
                if (seed.album == null) {
                    radio?.items?.firstOrNull { it.id == seed.id }?.album?.id?.takeIf { it.startsWith("MPRE") }?.let { albumId ->
                        launch {
                            val album = request { YouTube.album(albumId) }.getOrNull()?.album ?: return@launch
                            if (requestedQuery == query && requestedRevision == requestGeneration) {
                                songDiscovery?.let { current -> songDiscovery = current.copy(albums = (listOf(album) + current.albums).distinctBy { it.id }) }
                            }
                        }
                    }
                }
                val related = radio?.relatedEndpoint?.let { endpoint -> request { YouTube.related(endpoint) }.getOrNull() }
                currentCoroutineContext().ensureActive()
                if (requestedQuery != query || requestedRevision != requestGeneration || related == null) return@launch
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
                for (credit in credits.take(2)) {
                    val page = request { YouTube.artist(requireNotNull(credit.id)) }.getOrNull()
                    currentCoroutineContext().ensureActive()
                    if (requestedQuery != query || requestedRevision != requestGeneration) return@launch
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
        if (requestedQuery == query && requestedRevision == requestGeneration) {
            songDiscovery = songDiscovery?.copy(complete = true)
        }
        } finally {
            if (discoveryRevision == requestedRevision) discoveryRevision = null
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

    fun search(
        newQuery: String,
        resetCarousels: Boolean = false,
        pinnedSuggestions: List<YTItem> = emptyList(),
        selectedSong: SongItem? = null,
    ) {
        if (newQuery == query && selectedSong?.id == this.selectedSong?.id) {
            if (pinnedSuggestions.isNotEmpty() || pinnedSuggestionsQuery != newQuery) {
                this.pinnedSuggestions = pinnedSuggestions.take(5)
                pinnedSuggestionsQuery = newQuery
            }
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
                this.pinnedSuggestions,
                remoteSuggestions,
                this.selectedSong,
            )
            while (queryCache.size > MAX_QUERY_CACHE) {
                queryCache.remove(queryCache.keys.first())
            }
        }
        searchJob?.cancel()
        summaryJob?.cancel()
        discoveryRevision = null
        requestGeneration++
        query = newQuery
        this.selectedSong = selectedSong
        this.pinnedSuggestions = pinnedSuggestions.take(5)
        pinnedSuggestionsQuery = newQuery
        searchRevision++
        filter.value = null
        viewStateMap.clear()
        val cached = queryCache[newQuery].takeIf { selectedSong == null }
        if (cached != null) {
            summaryPage = cached.summaryPage
            viewStateMap.putAll(cached.viewStateMap)
            smartSearchOrder = cached.smartSearchOrder
            smartSearchStarted = cached.smartSearchStarted
            songDiscovery = cached.songDiscovery
            remoteSuggestions = cached.remoteSuggestions
            this.selectedSong = cached.selectedSong
            if (pinnedSuggestions.isEmpty()) this.pinnedSuggestions = cached.pinnedSuggestions
            pinnedSuggestionsQuery = newQuery
        } else {
            summaryPage = null
            smartSearchOrder = emptyList()
            smartSearchStarted = false
            songDiscovery = null
            remoteSuggestions = emptyList()
        }
    }

    private companion object {
        const val MAX_QUERY_CACHE = 8
    }
}
