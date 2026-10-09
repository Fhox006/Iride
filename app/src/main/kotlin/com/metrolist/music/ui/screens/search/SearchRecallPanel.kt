package com.metrolist.music.ui.screens.search

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.material3.Text
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.metrolist.music.LocalDatabase
import com.metrolist.innertube.models.YTItem
import com.metrolist.innertube.models.AlbumItem
import com.metrolist.innertube.models.ArtistItem
import com.metrolist.innertube.models.SongItem
import com.metrolist.innertube.models.PlaylistItem
import com.metrolist.music.R
import com.metrolist.music.ui.component.IrideFolioScrollbar
import com.metrolist.music.ui.component.IrideFolioSecondaryButton
import com.metrolist.music.ui.component.rubberBandOverscroll
import com.metrolist.music.ui.utils.rememberReducedMotion

/** Artwork recap with the same edge resistance and scrollbar as Folio pages. */
@Composable
internal fun SearchRecallPanel(
    query: String,
    submittedQuery: String,
    suggestions: List<String>,
    predictionItems: List<YTItem>,
    availableHeight: Float,
    onComplete: (String, Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val database = LocalDatabase.current
    val historyFlow = remember(database) { database.searchHistory() }
    val history by historyFlow.collectAsState(initial = emptyList())
    val historyMode = query.isBlank() || query == submittedQuery
    val recentHistory = history.take(15)
    val matchingHistory = remember(query, recentHistory) {
        recentHistory.filter { searchPredictionScore(query, it.title ?: it.query, it.query) != null }.take(3)
    }
    val previews = remember(query, predictionItems) {
        predictionItems.distinctBy { it.id }.mapNotNull { item ->
            searchPredictionScore(query, item.title, predictionRelatedText(item))?.let { it to item }
        }.sortedWith(compareBy({ it.first }, { if (it.second is ArtistItem) 0 else 1 }))
            .take(6).map { it.second }
    }
    val completions = remember(query, suggestions, previews, matchingHistory) {
        suggestions.distinct().filter { candidate ->
            searchPredictionScore(query, candidate) != null &&
                previews.none { it.title.equals(candidate, ignoreCase = true) } &&
                matchingHistory.none { it.query.equals(candidate, ignoreCase = true) }
        }.take(6)
    }
    val entryCount = if (historyMode) recentHistory.size else matchingHistory.size + previews.size + completions.size
    val rowHeight = 52.dp
    val rowHeightPx = with(LocalDensity.current) { rowHeight.toPx() }
    val visibleRows = searchRecallVisibleRows(availableHeight, rowHeightPx, entryCount)
    if (visibleRows == 0) return
    val state = rememberLazyListState()
    val reducedMotion = rememberReducedMotion()

    Box(modifier.height(rowHeight * visibleRows).clipToBounds()) {
        LazyColumn(
            state = state,
            overscrollEffect = null,
            modifier = Modifier.fillMaxSize().then(
                if (reducedMotion) Modifier else Modifier.rubberBandOverscroll(Orientation.Vertical, state),
            ),
        ) {
            if (historyMode) {
                items(recentHistory, key = { "history_${it.query}" }) { entry ->
                    SuggestionItem(
                        query = entry.title ?: entry.query,
                        online = false,
                        showDelete = false,
                        thumbnailUrl = entry.thumbnailUrl,
                        isArtistThumbnail = entry.itemType == "artist",
                        onClick = { onComplete(entry.query, true) },
                        onFillTextField = {
                            onComplete(entry.query, false)
                        },
                        pureBlack = false,
                        useIrideStyle = true,
                        modifier = Modifier.height(rowHeight),
                    )
                }
                item(key = "clear_search_history") {
                    Box(Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
                        IrideFolioSecondaryButton(
                            onClick = { database.query { clearSearchHistory() } },
                            modifier = Modifier.fillMaxWidth().height(40.dp),
                        ) {
                            Text(stringResource(R.string.iride_clear_search_history))
                        }
                    }
                }
            } else {
                items(matchingHistory, key = { "matched_history_${it.id}" }) { entry ->
                    SuggestionItem(
                        query = entry.title ?: entry.query,
                        online = true,
                        thumbnailUrl = entry.thumbnailUrl,
                        isArtistThumbnail = entry.itemType == "artist",
                        onClick = { onComplete(entry.query, true) },
                        onFillTextField = { onComplete(entry.query, false) },
                        pureBlack = false,
                        useIrideStyle = true,
                        modifier = Modifier.height(rowHeight),
                    )
                }
                items(previews, key = { "preview_${it.id}" }) { item ->
                    val completedQuery = predictionQuery(item)
                    SuggestionItem(
                        query = if (item is ArtistItem) item.title else completedQuery,
                        online = true,
                        thumbnailUrl = item.thumbnail,
                        isArtistThumbnail = item is ArtistItem,
                        onClick = { onComplete(completedQuery, true) },
                        onFillTextField = { onComplete(completedQuery, false) },
                        pureBlack = false,
                        useIrideStyle = true,
                        modifier = Modifier.height(rowHeight),
                    )
                }
                items(completions, key = { "suggestion_$it" }) { entry ->
                    SuggestionItem(
                        query = entry,
                        online = true,
                        onClick = { onComplete(entry, true) },
                        onFillTextField = { onComplete(entry, false) },
                        pureBlack = false,
                        useIrideStyle = true,
                        modifier = Modifier.height(rowHeight),
                    )
                }
            }
        }
        IrideFolioScrollbar(state, Modifier.align(Alignment.CenterEnd).fillMaxHeight())
    }
}

private fun predictionRelatedText(item: YTItem): String = when (item) {
    is ArtistItem -> "artista artist cantante singer"
    is AlbumItem -> item.artists.orEmpty().joinToString(" ") { it.name } + " album disco"
    is SongItem -> item.artists.joinToString(" ") { it.name } + " brano song canzone"
    is PlaylistItem -> item.author?.name.orEmpty() + " playlist"
    else -> ""
}

private fun predictionQuery(item: YTItem): String {
    val artist = when (item) {
        is AlbumItem -> item.artists?.firstOrNull()?.name
        is SongItem -> item.artists.firstOrNull()?.name
        else -> null
    }
    return if (artist.isNullOrBlank() || item.title.contains(artist, ignoreCase = true)) item.title
    else "${item.title} $artist"
}
