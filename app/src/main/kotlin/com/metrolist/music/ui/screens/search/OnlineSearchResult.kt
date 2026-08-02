/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.screens.search

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.navigation.NavController
import com.metrolist.innertube.YouTube
import com.metrolist.innertube.YouTube.SearchFilter.Companion.FILTER_ALBUM
import com.metrolist.innertube.YouTube.SearchFilter.Companion.FILTER_ARTIST
import com.metrolist.innertube.YouTube.SearchFilter.Companion.FILTER_COMMUNITY_PLAYLIST
import com.metrolist.innertube.YouTube.SearchFilter.Companion.FILTER_EPISODE
import com.metrolist.innertube.YouTube.SearchFilter.Companion.FILTER_FEATURED_PLAYLIST
import com.metrolist.innertube.YouTube.SearchFilter.Companion.FILTER_PODCAST
import com.metrolist.innertube.YouTube.SearchFilter.Companion.FILTER_PROFILE
import com.metrolist.innertube.YouTube.SearchFilter.Companion.FILTER_SONG
import com.metrolist.innertube.YouTube.SearchFilter.Companion.FILTER_VIDEO
import com.metrolist.innertube.models.AlbumItem
import com.metrolist.innertube.models.ArtistItem
import com.metrolist.innertube.models.EpisodeItem
import com.metrolist.innertube.models.PlaylistItem
import com.metrolist.innertube.models.PodcastItem
import com.metrolist.innertube.models.SongItem
import com.metrolist.innertube.models.WatchEndpoint
import com.metrolist.innertube.models.YTItem
import com.metrolist.music.LocalDatabase
import com.metrolist.music.LocalPlayerConnection
import com.metrolist.music.R
import com.metrolist.music.constants.HideVideoSongsKey
import com.metrolist.music.constants.MainTopGradientKey
import com.metrolist.music.constants.MiniPlayerBottomSpacing
import com.metrolist.music.constants.MiniPlayerHeight
import com.metrolist.music.constants.NavigationBarHeight
import com.metrolist.music.constants.PauseSearchHistoryKey
import com.metrolist.music.constants.TopNavigationBarKey
import com.metrolist.music.db.entities.SearchHistory
import com.metrolist.music.models.toMediaMetadata
import com.metrolist.music.playback.queues.YouTubeQueue
import com.metrolist.music.utils.recordSearchHistoryOpen
import com.metrolist.music.ui.component.ChipsRow
import com.metrolist.music.ui.component.EmptyPlaceholder
import com.metrolist.music.ui.component.HideOnScrollFAB
import com.metrolist.music.ui.component.LocalMenuState
import com.metrolist.music.ui.component.NavigationTitle
import com.metrolist.music.ui.component.YouTubeGridItem
import com.metrolist.music.ui.component.YouTubeListItem
import com.metrolist.music.ui.component.shimmer.GridItemPlaceHolder
import com.metrolist.music.ui.component.shimmer.ListItemPlaceHolder
import com.metrolist.music.ui.component.shimmer.ShimmerHost
import com.metrolist.music.ui.menu.YouTubeAlbumMenu
import com.metrolist.music.ui.menu.YouTubeArtistMenu
import com.metrolist.music.ui.menu.YouTubePlaylistMenu
import com.metrolist.music.ui.menu.YouTubeSongMenu
import com.metrolist.music.utils.rememberPreference
import com.metrolist.music.viewmodels.OnlineSearchViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.net.URLDecoder
import java.net.URLEncoder

// Chip row identity: Smart Search lives alongside the filter pills but isn't itself a
// YouTube.SearchFilter, so the row's value type wraps both instead of overloading `null`.
private sealed interface SearchChipKey {
    data object Smart : SearchChipKey
    data class Filter(val value: YouTube.SearchFilter?) : SearchChipKey
}

// Section header label for a Smart Search category — mirrors the same string used for that
// filter's own pill, so the two stay visually consistent.
private fun filterSectionTitleRes(filter: YouTube.SearchFilter): Int = when (filter) {
    FILTER_SONG -> R.string.filter_songs
    FILTER_VIDEO -> R.string.filter_videos
    FILTER_ALBUM -> R.string.filter_albums
    FILTER_ARTIST -> R.string.filter_artists
    FILTER_COMMUNITY_PLAYLIST -> R.string.filter_community_playlists
    FILTER_FEATURED_PLAYLIST -> R.string.filter_featured_playlists
    FILTER_PODCAST -> R.string.filter_podcasts
    FILTER_EPISODE -> R.string.filter_episodes
    FILTER_PROFILE -> R.string.filter_profiles
    else -> R.string.filter_all
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun OnlineSearchResult(
    navController: NavController,
    viewModel: OnlineSearchViewModel = hiltViewModel(),
    pureBlack: Boolean = false,
    savedStateHandle: SavedStateHandle? = null,
) {
    val database = LocalDatabase.current
    val playerConnection = LocalPlayerConnection.current ?: return

    val coroutineScope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val scrollToTopCount by savedStateHandle
        ?.getStateFlow("scrollToTopCount", 0)
        ?.collectAsState(initial = 0) ?: remember { mutableIntStateOf(0) }

    var lastHandledCount by rememberSaveable { mutableIntStateOf(0) }
    var isSearchFocused by remember { mutableStateOf(false) }

    LaunchedEffect(scrollToTopCount) {
        if (scrollToTopCount > lastHandledCount) {
            lastHandledCount = scrollToTopCount
            kotlinx.coroutines.delay(100)
            try {
                focusRequester.requestFocus()
                keyboardController?.show()
            } catch (e: Exception) {}
            isSearchFocused = true
        }
    }

    val pauseSearchHistory by rememberPreference(PauseSearchHistoryKey, defaultValue = false)
    // Restyles this route to match New Iride UI when reached from an entry point other than the
    // Search tab itself (voice search, genre taps, ...) — the tab's own submit flow no longer
    // navigates here when this is enabled, see SearchScreen's inline results.
    val topNavigationBarEnabled by rememberPreference(TopNavigationBarKey, defaultValue = true)
    val mainTopGradient by rememberPreference(MainTopGradientKey, defaultValue = true)

    BackHandler(enabled = isSearchFocused) {
        isSearchFocused = false
        focusManager.clearFocus()
    }

    val encodedQuery = navController.currentBackStackEntry?.arguments?.getString("query") ?: ""
    val decodedQuery = remember(encodedQuery) {
        try { URLDecoder.decode(encodedQuery, "UTF-8") } catch (e: Exception) { encodedQuery }
    }

    var query by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue(decodedQuery, TextRange(decodedQuery.length)))
    }

    val onSearch: (String) -> Unit = remember {
        { searchQuery ->
            if (searchQuery.isNotEmpty()) {
                isSearchFocused = false
                focusManager.clearFocus()
                if (!pauseSearchHistory) {
                    coroutineScope.launch(Dispatchers.IO) {
                        database.query { insert(SearchHistory(query = searchQuery)) }
                    }
                }
                navController.navigate("search/${URLEncoder.encode(searchQuery, "UTF-8")}") {
                    popUpTo("search/${URLEncoder.encode(decodedQuery, "UTF-8")}") { inclusive = true }
                }
            }
        }
    }

    LaunchedEffect(decodedQuery) {
        query = TextFieldValue(decodedQuery, TextRange(decodedQuery.length))
    }

    if (topNavigationBarEnabled) {
        // New Iride UI: the search bar scrolls away together with the chips/results — no pinned
        // chrome — so the background must be able to go transparent/gradient like Home/Library,
        // instead of the classic UI's always-opaque background below.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    when {
                        pureBlack -> Color.Black
                        mainTopGradient -> Color.Transparent
                        else -> MaterialTheme.colorScheme.background
                    },
                )
                .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Top)),
        ) {
            OnlineSearchResultsBody(
                navController = navController,
                viewModel = viewModel,
                pureBlack = pureBlack,
                useIrideStyle = true,
                isSearchFocused = isSearchFocused,
                queryText = query.text,
                onQueryChange = { query = it },
                onSearch = onSearch,
                onDismissSuggestions = {
                    isSearchFocused = false
                    focusManager.clearFocus()
                },
                header = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 8.dp),
                    ) {
                        IconButton(onClick = { navController.navigateUp() }) {
                            Icon(
                                painter = painterResource(R.drawable.arrow_back),
                                contentDescription = stringResource(R.string.dismiss),
                                tint = Color.White.copy(alpha = 0.6f),
                            )
                        }
                        IrideSearchBox(
                            query = query,
                            onQueryChange = { query = it },
                            placeholderText = stringResource(R.string.search_yt_music),
                            focusRequester = focusRequester,
                            onFocusChanged = { if (it.isFocused) isSearchFocused = true },
                            onSearch = { onSearch(query.text) },
                            onClear = { query = TextFieldValue("") },
                            modifier = Modifier.weight(1f),
                        )
                    }
                },
            )
        }
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(if (pureBlack) Color.Black else MaterialTheme.colorScheme.background)
                .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Top)),
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = {
                    Text(
                        text = stringResource(R.string.search_yt_music),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
                leadingIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(
                            painter = painterResource(R.drawable.arrow_back),
                            contentDescription = stringResource(R.string.dismiss),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                trailingIcon = {
                    if (query.text.isNotEmpty()) {
                        IconButton(onClick = { query = TextFieldValue("") }) {
                            Icon(
                                painter = painterResource(R.drawable.close),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { onSearch(query.text) }),
                singleLine = true,
                shape = RoundedCornerShape(28.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = if (pureBlack) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceContainerHigh,
                    unfocusedContainerColor = if (pureBlack) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceContainerHigh,
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .focusRequester(focusRequester)
                    .onFocusChanged { if (it.isFocused) isSearchFocused = true },
            )

            OnlineSearchResultsBody(
                modifier = Modifier.weight(1f),
                navController = navController,
                viewModel = viewModel,
                pureBlack = pureBlack,
                useIrideStyle = false,
                isSearchFocused = isSearchFocused,
                queryText = query.text,
                onQueryChange = { query = it },
                onSearch = onSearch,
                onDismissSuggestions = {
                    isSearchFocused = false
                    focusManager.clearFocus()
                },
            )
        }
    }
}

/**
 * Chips row + Smart Search/filtered results list + focus suggestion overlay + mic FAB — shared by
 * [OnlineSearchResult] (the classic separate-route screen) and [SearchScreen]'s inline New Iride
 * UI results view, so both stay in sync instead of maintaining two copies of this logic.
 */
@Composable
fun OnlineSearchResultsBody(
    navController: NavController,
    viewModel: OnlineSearchViewModel,
    pureBlack: Boolean,
    useIrideStyle: Boolean,
    isSearchFocused: Boolean,
    queryText: String,
    onQueryChange: (TextFieldValue) -> Unit,
    onSearch: (String) -> Unit,
    onDismissSuggestions: () -> Unit,
    modifier: Modifier = Modifier,
    // New Iride UI: leading scrollable item (search bar/nav) — when non-null, it and the chips
    // row share one LazyColumn with the results, so everything scrolls away together instead of
    // staying pinned, exactly like LocalSearchScreen/OnlineSearchScreen.
    header: (@Composable () -> Unit)? = null,
) {
    val menuState = LocalMenuState.current
    val playerConnection = LocalPlayerConnection.current ?: return
    val database = LocalDatabase.current
    val haptic = LocalHapticFeedback.current
    val isPlaying by playerConnection.isEffectivelyPlaying.collectAsState()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()
    val coroutineScope = rememberCoroutineScope()
    val lazyListState = rememberLazyListState()
    val pauseSearchHistory by rememberPreference(PauseSearchHistoryKey, defaultValue = false)

    var smartSelected by rememberSaveable { mutableStateOf(true) }
    LaunchedEffect(smartSelected, viewModel.query) {
        if (smartSelected) viewModel.loadSmartSearch()
    }

    val hideVideoSongs by rememberPreference(HideVideoSongsKey, defaultValue = false)
    LaunchedEffect(hideVideoSongs) {
        if (hideVideoSongs && viewModel.filter.value == FILTER_VIDEO) {
            viewModel.filter.value = null
        }
    }

    val searchFilter by viewModel.filter.collectAsState()
    val searchSummary = viewModel.summaryPage
    val itemsPage by remember(searchFilter) {
        derivedStateOf {
            searchFilter?.value?.let { viewModel.viewStateMap[it] }
        }
    }

    LaunchedEffect(lazyListState) {
        snapshotFlow {
            lazyListState.layoutInfo.visibleItemsInfo.any { it.key == "loading" }
        }.collect { shouldLoadMore ->
            if (!shouldLoadMore) return@collect
            viewModel.loadMore()
        }
    }

    // Vertical list item renderer (top result section + filtered tabs)
    val ytItemContent: @Composable LazyItemScope.(YTItem) -> Unit = { item ->
        val longClick = {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            menuState.show {
                when (item) {
                    is SongItem -> YouTubeSongMenu(song = item, navController = navController, onDismiss = menuState::dismiss)
                    is AlbumItem -> YouTubeAlbumMenu(albumItem = item, navController = navController, onDismiss = menuState::dismiss)
                    is ArtistItem -> YouTubeArtistMenu(artist = item, onDismiss = menuState::dismiss)
                    is PlaylistItem -> YouTubePlaylistMenu(playlist = item, coroutineScope = coroutineScope, onDismiss = menuState::dismiss)
                    is PodcastItem -> YouTubePlaylistMenu(playlist = item.asPlaylistItem(), coroutineScope = coroutineScope, onDismiss = menuState::dismiss)
                    is EpisodeItem -> YouTubeSongMenu(song = item.asSongItem(), navController = navController, onDismiss = menuState::dismiss)
                }
            }
        }
        YouTubeListItem(
            item = item,
            isActive = when (item) {
                is SongItem -> mediaMetadata?.id == item.id
                is AlbumItem -> mediaMetadata?.album?.id == item.id
                is EpisodeItem -> mediaMetadata?.id == item.id
                else -> false
            },
            isPlaying = isPlaying,
            trailingContent = {
                if (item is ArtistItem) {
                    Icon(
                        painter = painterResource(R.drawable.arrow_forward),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    IconButton(onClick = longClick) {
                        Icon(painter = painterResource(R.drawable.more_vert), contentDescription = null)
                    }
                }
            },
            modifier = Modifier
                .padding(horizontal = if (useIrideStyle) 20.dp else 16.dp)
                .combinedClickable(
                    onClick = {
                        if (!pauseSearchHistory) database.recordSearchHistoryOpen(viewModel.query, item)
                        when (item) {
                            is SongItem -> {
                                if (item.id == mediaMetadata?.id) playerConnection.togglePlayPause()
                                else playerConnection.playQueue(YouTubeQueue(WatchEndpoint(videoId = item.id), item.toMediaMetadata()))
                            }
                            is AlbumItem -> navController.navigate("album/${item.id}")
                            is ArtistItem -> navController.navigate("artist/${item.id}")
                            is PlaylistItem -> navController.navigate("online_playlist/${item.id}")
                            is PodcastItem -> navController.navigate("online_podcast/${item.id}")
                            is EpisodeItem -> {
                                if (item.id == mediaMetadata?.id) playerConnection.togglePlayPause()
                                else playerConnection.playQueue(YouTubeQueue(WatchEndpoint(videoId = item.id), item.toMediaMetadata()))
                            }
                        }
                    },
                    onLongClick = longClick,
                )
                .animateItem(),
        )
    }

    // Horizontal carousel shared by the "All" tab's category shelves and Smart Search's
    // per-filter sections — same card sizing/click/long-press behavior either way.
    val searchResultRow: @Composable (List<YTItem>) -> Unit = { rowItems ->
        LazyRow(contentPadding = PaddingValues(start = 16.dp, end = 8.dp)) {
            items(
                items = rowItems,
                key = { it.id },
            ) { rowItem ->
                val isAlbum = rowItem is AlbumItem
                val isVideo = rowItem is SongItem && rowItem.isVideoSong
                YouTubeGridItem(
                    item = rowItem,
                    isActive = when (rowItem) {
                        is SongItem -> mediaMetadata?.id == rowItem.id
                        is AlbumItem -> mediaMetadata?.album?.id == rowItem.id
                        is EpisodeItem -> mediaMetadata?.id == rowItem.id
                        else -> false
                    },
                    isPlaying = isPlaying,
                    coroutineScope = coroutineScope,
                    thumbnailRatio = if (isVideo) 16f / 9f else 1f,
                    thumbnailCornerRadius = if (isVideo) 8.dp else 3.dp,
                    size = when {
                        isAlbum -> 180.dp
                        isVideo -> 110.dp
                        else -> 148.dp
                    },
                    modifier = Modifier
                        .combinedClickable(
                            onClick = {
                                if (!pauseSearchHistory) database.recordSearchHistoryOpen(viewModel.query, rowItem)
                                when (rowItem) {
                                    is SongItem -> {
                                        if (rowItem.id == mediaMetadata?.id) playerConnection.togglePlayPause()
                                        else playerConnection.playQueue(YouTubeQueue(WatchEndpoint(videoId = rowItem.id), rowItem.toMediaMetadata()))
                                    }
                                    is AlbumItem -> navController.navigate("album/${rowItem.id}")
                                    is ArtistItem -> navController.navigate("artist/${rowItem.id}")
                                    is PlaylistItem -> navController.navigate("online_playlist/${rowItem.id}")
                                    is PodcastItem -> navController.navigate("online_podcast/${rowItem.id}")
                                    is EpisodeItem -> {
                                        if (rowItem.id == mediaMetadata?.id) playerConnection.togglePlayPause()
                                        else playerConnection.playQueue(YouTubeQueue(WatchEndpoint(videoId = rowItem.id), rowItem.toMediaMetadata()))
                                    }
                                }
                            },
                            onLongClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                menuState.show {
                                    when (rowItem) {
                                        is SongItem -> YouTubeSongMenu(song = rowItem, navController = navController, onDismiss = menuState::dismiss)
                                        is AlbumItem -> YouTubeAlbumMenu(albumItem = rowItem, navController = navController, onDismiss = menuState::dismiss)
                                        is ArtistItem -> YouTubeArtistMenu(artist = rowItem, onDismiss = menuState::dismiss)
                                        is PlaylistItem -> YouTubePlaylistMenu(playlist = rowItem, coroutineScope = coroutineScope, onDismiss = menuState::dismiss)
                                        is PodcastItem -> YouTubePlaylistMenu(playlist = rowItem.asPlaylistItem(), coroutineScope = coroutineScope, onDismiss = menuState::dismiss)
                                        is EpisodeItem -> YouTubeSongMenu(song = rowItem.asSongItem(), navController = navController, onDismiss = menuState::dismiss)
                                    }
                                }
                            },
                        )
                        .animateItem(),
                )
            }
        }
    }

    // Shimmer placeholder for a Smart Search section still awaiting its dedicated search call.
    val searchResultRowPlaceholder: @Composable () -> Unit = {
        Row(modifier = Modifier.padding(start = 4.dp)) {
            repeat(3) { GridItemPlaceHolder() }
        }
    }

    // Two-column vertical grid shared by the Album and Playlist filter tabs — same card sizing
    // language as Smart Search's carousels, but laid out as fillMaxWidth pairs (via lazy items()
    // so paginated results keep proper recycling) instead of a fixed-size horizontal scroll, so
    // each cover fills its half of the row instead of leaving dead space.
    val searchResultGrid2Col: LazyListScope.(List<YTItem>) -> Unit = { rowItems ->
        items(
            items = rowItems.chunked(2),
            key = { row -> "filtered_row_${row.first().id}" },
        ) { row ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = if (useIrideStyle) 20.dp else 12.dp,
                        vertical = 6.dp,
                    )
                    .animateItem(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                row.forEach { item ->
                    YouTubeGridItem(
                        item = item,
                        isActive = when (item) {
                            is SongItem -> mediaMetadata?.id == item.id
                            is AlbumItem -> mediaMetadata?.album?.id == item.id
                            is EpisodeItem -> mediaMetadata?.id == item.id
                            else -> false
                        },
                        isPlaying = isPlaying,
                        coroutineScope = coroutineScope,
                        thumbnailRatio = 1f,
                        thumbnailCornerRadius = 6.dp,
                        fillMaxWidth = true,
                        modifier = Modifier
                            .weight(1f)
                            .combinedClickable(
                                onClick = {
                                    if (!pauseSearchHistory) database.recordSearchHistoryOpen(viewModel.query, item)
                                    when (item) {
                                        is SongItem -> {
                                            if (item.id == mediaMetadata?.id) playerConnection.togglePlayPause()
                                            else playerConnection.playQueue(YouTubeQueue(WatchEndpoint(videoId = item.id), item.toMediaMetadata()))
                                        }
                                        is AlbumItem -> navController.navigate("album/${item.id}")
                                        is ArtistItem -> navController.navigate("artist/${item.id}")
                                        is PlaylistItem -> navController.navigate("online_playlist/${item.id}")
                                        is PodcastItem -> navController.navigate("online_podcast/${item.id}")
                                        is EpisodeItem -> {
                                            if (item.id == mediaMetadata?.id) playerConnection.togglePlayPause()
                                            else playerConnection.playQueue(YouTubeQueue(WatchEndpoint(videoId = item.id), item.toMediaMetadata()))
                                        }
                                    }
                                },
                                onLongClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    menuState.show {
                                        when (item) {
                                            is SongItem -> YouTubeSongMenu(song = item, navController = navController, onDismiss = menuState::dismiss)
                                            is AlbumItem -> YouTubeAlbumMenu(albumItem = item, navController = navController, onDismiss = menuState::dismiss)
                                            is ArtistItem -> YouTubeArtistMenu(artist = item, onDismiss = menuState::dismiss)
                                            is PlaylistItem -> YouTubePlaylistMenu(playlist = item, coroutineScope = coroutineScope, onDismiss = menuState::dismiss)
                                            is PodcastItem -> YouTubePlaylistMenu(playlist = item.asPlaylistItem(), coroutineScope = coroutineScope, onDismiss = menuState::dismiss)
                                            is EpisodeItem -> YouTubeSongMenu(song = item.asSongItem(), navController = navController, onDismiss = menuState::dismiss)
                                        }
                                    }
                                },
                            ),
                    )
                }
                if (row.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }

    // Filter pills
    val visibleChips = buildList {
        add(SearchChipKey.Smart to stringResource(R.string.filter_smart_search))
        add(SearchChipKey.Filter(FILTER_SONG) to stringResource(R.string.filter_songs))
        if (!hideVideoSongs) add(SearchChipKey.Filter(FILTER_VIDEO) to stringResource(R.string.filter_videos))
        add(SearchChipKey.Filter(FILTER_ALBUM) to stringResource(R.string.filter_albums))
        add(SearchChipKey.Filter(FILTER_ARTIST) to stringResource(R.string.filter_artists))
        add(SearchChipKey.Filter(FILTER_COMMUNITY_PLAYLIST) to stringResource(R.string.filter_community_playlists))
        add(SearchChipKey.Filter(FILTER_FEATURED_PLAYLIST) to stringResource(R.string.filter_featured_playlists))
        add(SearchChipKey.Filter(FILTER_PODCAST) to stringResource(R.string.filter_podcasts))
        add(SearchChipKey.Filter(FILTER_EPISODE) to stringResource(R.string.filter_episodes))
    }

    val chipsRow: @Composable () -> Unit = {
        ChipsRow(
            chips = visibleChips,
            currentValue = if (smartSelected) SearchChipKey.Smart else SearchChipKey.Filter(searchFilter),
            onValueUpdate = { key ->
                when (key) {
                    is SearchChipKey.Smart -> smartSelected = true
                    is SearchChipKey.Filter -> {
                        smartSelected = false
                        if (viewModel.filter.value != key.value) viewModel.filter.value = key.value
                    }
                }
                coroutineScope.launch { lazyListState.animateScrollToItem(0) }
            },
            modifier = Modifier.fillMaxWidth(),
            containerColor = Color.Transparent,
            horizontalPadding = if (useIrideStyle) 20.dp else 16.dp,
            useIrideStyle = useIrideStyle,
        )
    }

    // Smart Search + filtered results list content, shared by both the pinned-chrome (classic UI)
    // and scroll-away-chrome (New Iride UI, see `header`) layouts below.
    val resultsListContent: LazyListScope.() -> Unit = {
            if (smartSelected) {
                // Smart Search: full per-category carousels ordered by query intent, each
                // backed by that category's own dedicated search (not YT's truncated
                // summary shelves), so nothing is capped to a handful of items.
                val order = viewModel.smartSearchOrder
                val topResult = searchSummary?.summaries?.firstOrNull()

                if (order.isNotEmpty()) {
                    topResult?.let { top ->
                        item(key = "smart_top_title") {
                            NavigationTitle(top.title, useIrideStyle = useIrideStyle)
                        }
                        items(
                            items = top.items,
                            key = { "smart_top_${it.id}" },
                            itemContent = ytItemContent,
                        )
                    }

                    order.forEach { sectionFilter ->
                        val page = viewModel.viewStateMap[sectionFilter.value]
                        if (page != null && page.items.isEmpty()) return@forEach

                        item(key = "smart_title_${sectionFilter.value}") {
                            NavigationTitle(
                                title = stringResource(filterSectionTitleRes(sectionFilter)),
                                useIrideStyle = useIrideStyle,
                                onClick = {
                                    smartSelected = false
                                    if (viewModel.filter.value != sectionFilter) viewModel.filter.value = sectionFilter
                                    coroutineScope.launch { lazyListState.animateScrollToItem(0) }
                                },
                            )
                        }

                        item(key = "smart_row_${sectionFilter.value}") {
                            if (page == null) {
                                searchResultRowPlaceholder()
                            } else {
                                searchResultRow(page.items)
                            }
                        }
                    }

                    val allResolved = order.all { viewModel.viewStateMap[it.value] != null }
                    val allEmpty = allResolved && topResult == null &&
                        order.all { viewModel.viewStateMap[it.value]?.items.isNullOrEmpty() }
                    if (allEmpty) {
                        item {
                            EmptyPlaceholder(
                                icon = R.drawable.search,
                                text = stringResource(R.string.no_results_found),
                            )
                        }
                    }
                }
            } else {
                val filteredItems = itemsPage?.items.orEmpty().distinctBy { it.id }
                val isGridFilter = searchFilter == FILTER_COMMUNITY_PLAYLIST ||
                    searchFilter == FILTER_FEATURED_PLAYLIST ||
                    searchFilter == FILTER_ALBUM

                if (isGridFilter) {
                    searchResultGrid2Col(filteredItems)
                } else {
                    items(
                        items = filteredItems,
                        key = { "filtered_${it.id}" },
                        itemContent = ytItemContent,
                    )
                }

                // Pagination shimmer
                if (itemsPage?.continuation != null) {
                    item(key = "loading") {
                        ShimmerHost {
                            repeat(3) { ListItemPlaceHolder() }
                        }
                    }
                }

                // Empty state
                if (itemsPage?.items?.isEmpty() == true) {
                    item {
                        EmptyPlaceholder(
                            icon = R.drawable.search,
                            text = stringResource(R.string.no_results_found),
                        )
                    }
                }
            }

            // Initial loading shimmer
            if ((smartSelected && viewModel.smartSearchOrder.isEmpty()) ||
                (!smartSelected && itemsPage == null)
            ) {
                item {
                    ShimmerHost {
                        repeat(8) { ListItemPlaceHolder() }
                    }
                }
            }

            item(key = "bottom_spacer") {
                Spacer(modifier = Modifier.height(MiniPlayerHeight + MiniPlayerBottomSpacing + NavigationBarHeight))
            }
    }

    if (header != null) {
        // New Iride UI: the header is now a fixed, non-lazy sibling of the results/suggestions
        // area, instead of a LazyColumn item that got moved between two structurally different
        // parents (a LazyColumn item vs. a plain Column child) depending on focus.
        //
        // That move was the actual root cause of the long-standing "tap the search bar again to
        // re-edit -> history flashes on screen and instantly collapses, field becomes
        // uninteractable" bug: moving a *focused* BasicTextField's underlying node to a different
        // parent in the composition makes the platform briefly detach/reattach its window focus,
        // which fires a synthetic onFocusChanged(false) callback. That callback fed straight back
        // into isSearchFocused/isFocused — the very state whose flip *caused* the move in the
        // first place — closing the just-opened suggestions panel in the same frame it opened,
        // and leaving focus cleared so the field could no longer be typed into. Two "independent"
        // booleans weren't racing here; it was one state driving a structural move that echoed
        // back into itself.
        //
        // Pinning the header removes the feedback loop entirely: isSearchFocused now only ever
        // switches the content *below* the header (results list vs. suggestions list) — the
        // header's own position in the tree never changes, so its focus/IME state is never
        // disturbed by that toggle. (This does mean the header no longer scrolls away together
        // with browsed results the way it does on LocalSearchScreen/OnlineSearchScreen — a small
        // trade-off for the search box always staying visible and reliably re-editable, which
        // matches the classic non-Iride layout's behavior below.)
        LaunchedEffect(isSearchFocused) {
            if (!isSearchFocused) lazyListState.scrollToItem(0)
        }

        Box(modifier = modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize()) {
                header()
                if (isSearchFocused) {
                    // No background here: OnlineSearchScreen already paints its own
                    // (transparent/gradient when mainTopGradient is on, matching the very first
                    // search). Painting a solid MaterialTheme.colorScheme.background behind it
                    // ignored mainTopGradient and showed through as flat black on every re-search
                    // after the first, instead of staying transparent like the initial screen.
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                    ) {
                        OnlineSearchScreen(
                            query = queryText,
                            onQueryChange = onQueryChange,
                            navController = navController,
                            onSearch = onSearch,
                            onDismiss = onDismissSuggestions,
                            pureBlack = pureBlack,
                        )
                    }
                } else {
                    LazyColumn(
                        state = lazyListState,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                    ) {
                        item(key = "chips_row") { chipsRow() }
                        resultsListContent()
                    }
                }
            }

            HideOnScrollFAB(
                lazyListState = lazyListState,
                icon = R.drawable.mic,
                onClick = { navController.navigate("recognition") },
                useIrideStyle = useIrideStyle,
            )
        }
    } else {
        Column(modifier = modifier.fillMaxWidth()) {
            chipsRow()

            Box(modifier = Modifier.weight(1f)) {
                LazyColumn(
                    state = lazyListState,
                    modifier = Modifier.fillMaxWidth(),
                    content = resultsListContent,
                )

                if (isSearchFocused) {
                    OnlineSearchScreen(
                        query = queryText,
                        onQueryChange = onQueryChange,
                        navController = navController,
                        onSearch = onSearch,
                        onDismiss = onDismissSuggestions,
                        pureBlack = pureBlack,
                    )
                }

                HideOnScrollFAB(
                    lazyListState = lazyListState,
                    icon = R.drawable.mic,
                    onClick = { navController.navigate("recognition") },
                    useIrideStyle = useIrideStyle,
                )
            }
        }
    }
}
