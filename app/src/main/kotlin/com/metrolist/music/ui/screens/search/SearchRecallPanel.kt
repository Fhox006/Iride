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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.layout.size
import androidx.navigation.NavController
import com.metrolist.music.LocalPlayerConnection
import com.metrolist.music.ui.component.LocalMenuState
import com.metrolist.music.ui.component.LocalItemHorizontalPadding
import com.metrolist.music.ui.component.YouTubeListItem
import com.metrolist.music.ui.component.TrackOverflowIcon
import com.metrolist.music.ui.component.irideFolioCombinedClickable
import com.metrolist.music.ui.menu.YouTubeSongMenu
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.material3.Text
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import com.metrolist.music.LocalDatabase
import com.metrolist.innertube.models.YTItem
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
    navController: NavController,
    query: String,
    submittedQuery: String,
    suggestions: List<String>,
    recommendedItems: List<YTItem>,
    availableHeight: Float,
    onComplete: (String, Boolean) -> Unit,
    onOpenArtist: (String) -> Unit,
    onOpenAlbum: (String) -> Unit,
    onOpenItem: (YTItem) -> Unit,
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
    var clearHistoryRevealed by remember { androidx.compose.runtime.mutableStateOf(false) }
    LaunchedEffect(query, history.isEmpty()) {
        if (query.isNotBlank() || history.isEmpty()) clearHistoryRevealed = false
    }
    val showClearHistory = historyMode && recentHistory.isNotEmpty() && clearHistoryRevealed
    val previews = remember(recommendedItems) { recommendedItems.take(5) }
    val completions = remember(query, suggestions, previews, matchingHistory) {
        suggestions.distinct().filter { candidate ->
            searchPredictionScore(query, candidate) != null &&
                previews.none { it.title.equals(candidate, ignoreCase = true) } &&
                matchingHistory.none { it.query.equals(candidate, ignoreCase = true) }
        }.take(6)
    }
    val entryCount = when {
        historyMode -> recentHistory.size + if (showClearHistory) 1 else 0
        previews.isNotEmpty() -> previews.size
        else -> matchingHistory.size + completions.size
    }
    val rowHeight = com.metrolist.music.constants.ListItemHeight
    val rowHeightPx = with(LocalDensity.current) { rowHeight.toPx() }
    val revealThresholdPx = with(LocalDensity.current) { 56.dp.toPx() }
    val revealConnection = remember(query, historyMode, recentHistory.isNotEmpty(), revealThresholdPx) {
        object : NestedScrollConnection {
            var pullDistance = 0f

            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource,
            ): Offset {
                if (source != NestedScrollSource.UserInput) return Offset.Zero
                if (historyMode && recentHistory.isNotEmpty() && available.y > 0f) {
                    pullDistance += available.y
                    if (pullDistance >= revealThresholdPx) clearHistoryRevealed = true
                } else if (available.y < 0f) {
                    pullDistance = 0f
                }
                return Offset.Zero
            }

            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                pullDistance = 0f
                return Velocity.Zero
            }
        }
    }
    val visibleRows = searchRecallVisibleRows(availableHeight, rowHeightPx, entryCount)
    if (visibleRows == 0) return
    val state = rememberLazyListState()
    LaunchedEffect(showClearHistory) {
        if (showClearHistory) state.scrollToItem(0)
    }
    val playerConnection = LocalPlayerConnection.current ?: return
    val metadata by playerConnection.mediaMetadata.collectAsState()
    val playing by playerConnection.isEffectivelyPlaying.collectAsState()
    val menu = LocalMenuState.current
    val reducedMotion = rememberReducedMotion()

    SearchResultsStyle {
        Box(modifier.height(rowHeight * visibleRows).clipToBounds()) {
            LazyColumn(
                state = state,
                overscrollEffect = null,
                modifier = Modifier.fillMaxSize().then(
                    if (reducedMotion) Modifier else Modifier.rubberBandOverscroll(Orientation.Vertical, state),
                ).nestedScroll(revealConnection),
            ) {
                if (historyMode) {
                    if (showClearHistory) {
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
                    }
                    items(recentHistory, key = { "history_${it.query}" }) { entry ->
                        SuggestionItem(
                            query = entry.title ?: entry.query,
                            online = false,
                            showDelete = false,
                            thumbnailUrl = entry.thumbnailUrl,
                            isArtistThumbnail = entry.itemType == "artist",
                            historyItemId = entry.itemId,
                            historyItemType = entry.itemType,
                            isCollectionThumbnail = entry.itemType == "album" || entry.itemType == "playlist",
                            playlistId = entry.itemId.takeIf { entry.itemType == "playlist" },
                            onClick = {
                                val artistId = entry.itemId
                                when {
                                    entry.itemType == "artist" && !artistId.isNullOrBlank() -> onOpenArtist(artistId)
                                    entry.itemType == "album" && !artistId.isNullOrBlank() -> onOpenAlbum(artistId)
                                    entry.itemType == "playlist" && !artistId.isNullOrBlank() -> onOpenItem(
                                        PlaylistItem(artistId, entry.title ?: entry.query, null, null, entry.thumbnailUrl, null, null, null),
                                    )
                                    else -> onComplete(entry.query, true)
                                }
                            },
                            onFillTextField = {
                                onComplete(entry.query, false)
                            },
                            pureBlack = false,
                            useIrideStyle = true,
                            modifier = Modifier.height(rowHeight),
                        )
                    }
                } else {
                    if (previews.isEmpty()) items(matchingHistory, key = { "matched_history_${it.id}" }) { entry ->
                        SuggestionItem(
                            query = entry.title ?: entry.query,
                            online = true,
                            thumbnailUrl = entry.thumbnailUrl,
                            isArtistThumbnail = entry.itemType == "artist",
                            historyItemId = entry.itemId,
                            historyItemType = entry.itemType,
                            isCollectionThumbnail = entry.itemType == "album" || entry.itemType == "playlist",
                            playlistId = entry.itemId.takeIf { entry.itemType == "playlist" },
                            onClick = {
                                val artistId = entry.itemId
                                when {
                                    entry.itemType == "artist" && !artistId.isNullOrBlank() -> onOpenArtist(artistId)
                                    entry.itemType == "album" && !artistId.isNullOrBlank() -> onOpenAlbum(artistId)
                                    entry.itemType == "playlist" && !artistId.isNullOrBlank() -> onOpenItem(
                                        PlaylistItem(artistId, entry.title ?: entry.query, null, null, entry.thumbnailUrl, null, null, null),
                                    )
                                    else -> onComplete(entry.query, true)
                                }
                            },
                            onFillTextField = { onComplete(entry.query, false) },
                            pureBlack = false,
                            useIrideStyle = true,
                            modifier = Modifier.height(rowHeight),
                        )
                    }
                    items(previews, key = { "preview_${it.id}" }) { item ->
                        CompositionLocalProvider(LocalItemHorizontalPadding provides false) {
                            val showMenu = {
                                if (item is SongItem) menu.show(anchored = true) {
                                    YouTubeSongMenu(song = item, navController = navController, onDismiss = menu::dismiss)
                                }
                            }
                            YouTubeListItem(
                                item = item,
                                isActive = metadata?.id == item.id,
                                isPlaying = playing,
                                isSwipeable = item is SongItem,
                                showDivider = false,
                                modifier = Modifier.irideFolioCombinedClickable(
                                    onClick = { onOpenItem(item) }, onLongClick = showMenu,
                                ).padding(start = if (item is SongItem) 6.dp else 16.dp, end = 20.dp),
                                trailingContent = {
                                    if (item is SongItem) {
                                        IconButton(onClick = showMenu) { TrackOverflowIcon() }
                                    } else {
                                        IconButton(onClick = { onOpenItem(item) }) {
                                            Icon(painterResource(R.drawable.arrow_forward), null,
                                                modifier = Modifier.size(20.dp).rotate(-45f))
                                        }
                                    }
                                },
                            )
                        }
                    }
                    if (previews.isEmpty()) items(completions, key = { "suggestion_$it" }) { entry ->
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
}
