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
import com.metrolist.innertube.pages.SearchSummary
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
    var summaryPage by mutableStateOf<SearchSummaryPage?>(null)
    val viewStateMap = mutableStateMapOf<String, ItemsPage?>()

    private suspend fun loadSummaryPage() {
        if (summaryPage == null) {
            YouTube
                .searchSummary(query)
                .onSuccess {
                    val hideExplicit = context.dataStore.get(HideExplicitKey, false)
                    val hideVideoSongs = context.dataStore.get(HideVideoSongsKey, false)
                    val hideYoutubeShorts = context.dataStore.get(HideYoutubeShortsKey, false)
                    val filtered = it.filterExplicit(hideExplicit)
                        .filterVideoSongs(hideVideoSongs)
                        .filterYoutubeShorts(hideYoutubeShorts)
                    summaryPage = filtered.copy(summaries = reorderByQueryType(filtered.summaries))
                    enrichSummaries()
                }.onFailure {
                    reportException(it)
                }
        }
    }

    // "Top result" tells us what the query is really about (an artist, a song, an album, ...).
    // Reorder the remaining sections so the ones most relevant to that intent come first,
    // instead of the fixed Songs/Videos/Albums/Artists order YT always returns.
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

    private fun reorderByQueryType(summaries: List<SearchSummary>): List<SearchSummary> {
        if (summaries.isEmpty()) return summaries
        // The top-result card's title comes from YT's own (possibly localized) header text,
        // not the "Top result" literal, so matching on title breaks in non-English locales.
        // YT always returns that card as the first section instead, which is a stable signal.
        val top = summaries.first()
        val rest = summaries.drop(1)
        val topCategory = categoryOf(top.items.firstOrNull())

        val priorityOrder = when (topCategory) {
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

        val ranked = rest.sortedBy { summary ->
            priorityOrder.indexOf(categoryOf(summary.items.firstOrNull())).let { if (it == -1) priorityOrder.size else it }
        }
        return listOf(top) + ranked
    }

    // The summary endpoint truncates every section to a handful of items. Once it loads,
    // fetch each section's dedicated filter in the background and merge in more results so
    // the "All" tab actually shows everything, not just 3-5 items per category.
    // The first section is always the top-result card (see reorderByQueryType) and is left
    // untouched — it's a distinct card layout, not a plain category shelf.
    private fun enrichSummaries() {
        val sections = summaryPage?.summaries.orEmpty()
        sections.drop(1).forEach { summary ->
            val filter = filterForSummary(summary) ?: return@forEach
            viewModelScope.launch {
                YouTube.search(query, filter)
                    .onSuccess { result ->
                        val hideExplicit = context.dataStore.get(HideExplicitKey, false)
                        val hideVideoSongs = context.dataStore.get(HideVideoSongsKey, false)
                        val hideYoutubeShorts = context.dataStore.get(HideYoutubeShortsKey, false)
                        val merged = (summary.items + result.items)
                            .distinctBy { it.id }
                            .filterExplicit(hideExplicit)
                            .filterVideoSongs(hideVideoSongs)
                            .filterYoutubeShorts(hideYoutubeShorts)
                        val current = summaryPage ?: return@onSuccess
                        summaryPage = current.copy(
                            summaries = current.summaries.map {
                                if (it === summary) it.copy(items = merged) else it
                            }
                        )
                    }.onFailure {
                        reportException(it)
                    }
            }
        }
    }

    // Episodes are deliberately excluded: the dedicated FILTER_EPISODE search returns a
    // shape that our parser can't reliably classify (see the filter.collect handling below),
    // so mixing it into the summary would reintroduce that inconsistency.
    private fun filterForSummary(summary: SearchSummary): YouTube.SearchFilter? =
        when (categoryOf(summary.items.firstOrNull())) {
            Category.SONG -> YouTube.SearchFilter.FILTER_SONG
            Category.VIDEO -> YouTube.SearchFilter.FILTER_VIDEO
            Category.ALBUM -> YouTube.SearchFilter.FILTER_ALBUM
            Category.ARTIST -> YouTube.SearchFilter.FILTER_ARTIST
            Category.PROFILE -> YouTube.SearchFilter.FILTER_PROFILE
            Category.PODCAST -> YouTube.SearchFilter.FILTER_PODCAST
            // Community vs featured playlists can't be told apart from the item shape alone;
            // community is the far more common bucket so it's the safe default to enrich.
            Category.PLAYLIST -> YouTube.SearchFilter.FILTER_COMMUNITY_PLAYLIST
            Category.EPISODE, Category.OTHER -> null
        }

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
                    // parsed correctly by fromMusicResponsiveListItemRenderer and guaranteed to
                    // show the same results as the episodes section in the "All" filter.
                    if (viewStateMap[filter.value] == null) {
                        loadSummaryPage()
                        summaryPage?.let { page ->
                            val episodes = page.summaries
                                .firstOrNull { it.title == "Episodes" }
                                ?.items
                                .orEmpty()
                            viewStateMap[filter.value] = ItemsPage(episodes, null)
                        }
                    }
                } else {
                    if (viewStateMap[filter.value] == null) {
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
