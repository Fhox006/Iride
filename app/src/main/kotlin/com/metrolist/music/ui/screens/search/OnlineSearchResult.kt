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
import com.metrolist.music.constants.MiniPlayerBottomSpacing
import com.metrolist.music.constants.MiniPlayerHeight
import com.metrolist.music.constants.NavigationBarHeight
import com.metrolist.music.constants.PauseSearchHistoryKey
import com.metrolist.music.db.entities.SearchHistory
import com.metrolist.music.models.toMediaMetadata
import com.metrolist.music.playback.queues.YouTubeQueue
import com.metrolist.music.ui.component.ChipsRow
import com.metrolist.music.ui.component.EmptyPlaceholder
import com.metrolist.music.ui.component.HideOnScrollFAB
import com.metrolist.music.ui.component.LocalMenuState
import com.metrolist.music.ui.component.NavigationTitle
import com.metrolist.music.ui.component.YouTubeGridItem
import com.metrolist.music.ui.component.YouTubeListItem
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

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun OnlineSearchResult(
    navController: NavController,
    viewModel: OnlineSearchViewModel = hiltViewModel(),
    pureBlack: Boolean = false,
    savedStateHandle: SavedStateHandle? = null,
) {
    val database = LocalDatabase.current
    val menuState = LocalMenuState.current
    val playerConnection = LocalPlayerConnection.current ?: return
    val haptic = LocalHapticFeedback.current
    val isPlaying by playerConnection.isEffectivelyPlaying.collectAsState()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()

    val coroutineScope = rememberCoroutineScope()
    val lazyListState = rememberLazyListState()
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
    val hideVideoSongs by rememberPreference(HideVideoSongsKey, defaultValue = false)

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

    LaunchedEffect(Unit) {
        viewModel.filter.value = null
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
                .padding(horizontal = 16.dp)
                .combinedClickable(
                    onClick = {
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(if (pureBlack) Color.Black else MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Top)),
    ) {
        // Search bar
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

        // Filter pills
        val visibleChips = buildList {
            add(null to stringResource(R.string.filter_all))
            add(FILTER_SONG to stringResource(R.string.filter_songs))
            if (!hideVideoSongs) add(FILTER_VIDEO to stringResource(R.string.filter_videos))
            add(FILTER_ALBUM to stringResource(R.string.filter_albums))
            add(FILTER_ARTIST to stringResource(R.string.filter_artists))
            add(FILTER_COMMUNITY_PLAYLIST to stringResource(R.string.filter_community_playlists))
            add(FILTER_FEATURED_PLAYLIST to stringResource(R.string.filter_featured_playlists))
            add(FILTER_PODCAST to stringResource(R.string.filter_podcasts))
            add(FILTER_EPISODE to stringResource(R.string.filter_episodes))
            add(FILTER_PROFILE to stringResource(R.string.filter_profiles))
        }

        ChipsRow(
            chips = visibleChips,
            currentValue = searchFilter,
            onValueUpdate = { newFilter ->
                if (viewModel.filter.value != newFilter) viewModel.filter.value = newFilter
                coroutineScope.launch { lazyListState.animateScrollToItem(0) }
            },
            modifier = Modifier.fillMaxWidth(),
            containerColor = Color.Transparent,
            horizontalPadding = 16.dp,
        )

        Box(modifier = Modifier.weight(1f)) {
            LazyColumn(
                state = lazyListState,
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (searchFilter == null) {
                    // "All" tab: grouped summary sections
                    val summaries = searchSummary?.summaries ?: emptyList()

                    summaries.forEachIndexed { index, summary ->
                        item(key = "title_${summary.title}") {
                            NavigationTitle(summary.title)
                        }

                        if (index == 0) {
                            // First section (top result): vertical list
                            items(
                                items = summary.items,
                                key = { it.id },
                                itemContent = ytItemContent,
                            )
                        } else {
                            // All other sections: horizontal scroll
                            item(key = "row_${summary.title}") {
                                LazyRow(contentPadding = PaddingValues(start = 16.dp, end = 8.dp)) {
                                    items(
                                        items = summary.items,
                                        key = { it.id },
                                    ) { rowItem ->
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
                                            thumbnailRatio = 1f,
                                            thumbnailCornerRadius = 6.dp,
                                            showPlayButton = false,
                                            modifier = Modifier
                                                .combinedClickable(
                                                    onClick = {
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
                        }
                    }

                    // Empty state after data loads with no results
                    if (searchSummary != null && summaries.isEmpty()) {
                        item {
                            EmptyPlaceholder(
                                icon = R.drawable.search,
                                text = stringResource(R.string.no_results_found),
                            )
                        }
                    }
                } else {
                    val filteredItems = itemsPage?.items.orEmpty().distinctBy { it.id }
                    val isPlaylistFilter = searchFilter == FILTER_COMMUNITY_PLAYLIST || searchFilter == FILTER_FEATURED_PLAYLIST

                    if (isPlaylistFilter) {
                        val chunked = filteredItems.chunked(2)
                        items(
                            items = chunked,
                            key = { row -> "filtered_row_${row.first().id}" },
                        ) { row ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                                    .animateItem(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                row.forEach { item ->
                                    YouTubeGridItem(
                                        item = item,
                                        isActive = false,
                                        isPlaying = isPlaying,
                                        coroutineScope = coroutineScope,
                                        thumbnailRatio = 1f,
                                        thumbnailCornerRadius = 6.dp,
                                        showPlayButton = false,
                                        modifier = Modifier
                                            .weight(1f)
                                            .combinedClickable(
                                                onClick = {
                                                    when (item) {
                                                        is PlaylistItem -> navController.navigate("online_playlist/${item.id}")
                                                        is PodcastItem -> navController.navigate("online_podcast/${item.id}")
                                                        else -> {}
                                                    }
                                                },
                                                onLongClick = {
                                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                    menuState.show {
                                                        when (item) {
                                                            is PlaylistItem -> YouTubePlaylistMenu(playlist = item, coroutineScope = coroutineScope, onDismiss = menuState::dismiss)
                                                            is PodcastItem -> YouTubePlaylistMenu(playlist = item.asPlaylistItem(), coroutineScope = coroutineScope, onDismiss = menuState::dismiss)
                                                            else -> {}
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
                if ((searchFilter == null && searchSummary == null) || (searchFilter != null && itemsPage == null)) {
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

            // Suggestion overlay when search bar is focused
            if (isSearchFocused) {
                OnlineSearchScreen(
                    query = query.text,
                    onQueryChange = { query = it },
                    navController = navController,
                    onSearch = onSearch,
                    onDismiss = {
                        isSearchFocused = false
                        focusManager.clearFocus()
                    },
                    pureBlack = pureBlack,
                )
            }

            HideOnScrollFAB(
                lazyListState = lazyListState,
                icon = R.drawable.mic,
                onClick = { navController.navigate("recognition") },
            )
        }
    }
}
