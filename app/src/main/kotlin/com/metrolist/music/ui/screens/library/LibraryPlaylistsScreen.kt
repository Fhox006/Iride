/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.screens.library

import androidx.compose.foundation.layout.statusBars
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import com.metrolist.music.LocalDatabase
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.metrolist.innertube.utils.parseCookieString
import com.metrolist.innertube.YouTube
import com.metrolist.innertube.utils.completed
import com.metrolist.music.LocalPlayerAwareWindowInsets
import com.metrolist.music.LocalPlayerConnection
import com.metrolist.music.extensions.toMediaItem
import com.metrolist.music.models.toMediaMetadata
import com.metrolist.music.playback.queues.ListQueue
import com.metrolist.music.playback.queues.buildRotationQueue
import com.metrolist.music.R
import com.metrolist.music.constants.CONTENT_TYPE_HEADER
import com.metrolist.music.constants.CONTENT_TYPE_PLAYLIST
import com.metrolist.music.constants.GridItemSize
import com.metrolist.music.constants.GridItemsSizeKey
import com.metrolist.music.constants.GridThumbnailHeight
import com.metrolist.music.constants.InnerTubeCookieKey
import com.metrolist.music.constants.LibraryViewType
import com.metrolist.music.constants.PlaylistSortDescendingKey
import com.metrolist.music.constants.PlaylistSortType
import com.metrolist.music.constants.PlaylistSortTypeKey
import com.metrolist.music.constants.PlaylistViewTypeKey
import com.metrolist.music.constants.PureBlackKey
import com.metrolist.music.constants.YtmSyncKey
import com.metrolist.music.db.entities.Playlist
import com.metrolist.music.db.entities.PlaylistEntity
import com.metrolist.music.extensions.matchesNormalizedQuery
import com.metrolist.music.extensions.normalizeForSearch
import com.metrolist.music.ui.component.CollapsingScreenHeader
import com.metrolist.music.ui.component.CreatePlaylistDialog
import com.metrolist.music.ui.component.HideOnScrollFAB
import com.metrolist.music.ui.component.LibraryPlaylistGridItem
import com.metrolist.music.ui.component.LibraryPlaylistListItem
import com.metrolist.music.ui.component.LibrarySearchEmptyPlaceholder
import com.metrolist.music.ui.component.LibrarySortRow
import com.metrolist.music.ui.component.LocalItemHorizontalPadding
import com.metrolist.music.ui.component.LocalMenuState
import com.metrolist.music.ui.component.PlaylistGridItem
import com.metrolist.music.ui.component.PlaylistListItem
import com.metrolist.music.ui.component.LocalLibraryTextColor
import com.metrolist.music.ui.component.LibraryHeroTitle
import com.metrolist.music.ui.component.LibraryFooterCount
import com.metrolist.music.ui.component.LibraryPageTopBar
import com.metrolist.music.ui.component.IrideSearchBox
import com.metrolist.music.ui.component.LibraryGridPage
import com.metrolist.music.ui.component.LibraryPageMenuAction
import com.metrolist.music.ui.component.LibraryPlaybackActions
import com.metrolist.music.ui.component.IridePageLayout
import com.metrolist.music.ui.component.rubberBandOverscroll
import com.metrolist.music.ui.component.rememberLibraryPageRevealState
import com.metrolist.music.ui.component.rememberLibraryTopBarProgress
import com.metrolist.music.ui.theme.SpaceMonoFontFamily
import com.metrolist.music.ui.utils.IrideMotion
import com.metrolist.music.ui.utils.irideEnter
import com.metrolist.music.ui.utils.rememberDiscreteProgress
import com.metrolist.music.ui.utils.rememberEnterProgress
import com.metrolist.music.ui.utils.revealMask
import com.metrolist.music.utils.rememberEnumPreference
import com.metrolist.music.utils.rememberPreference
import com.metrolist.music.viewmodels.LibraryPlaylistsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch

private data class VisiblePlaylistItem(
    val key: String,
    val playlist: Playlist,
    val autoPlaylist: Boolean,
    val route: String? = null,
)

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun LibraryPlaylistsScreen(
    navController: NavController,
    isOffline: Boolean = false,
    viewModel: LibraryPlaylistsViewModel = hiltViewModel(),
    initialTextFieldValue: String? = null,
    allowSyncing: Boolean = true,
) {
    val menuState = LocalMenuState.current
    val haptic = LocalHapticFeedback.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val coroutineScope = rememberCoroutineScope()
    val playerConnection = LocalPlayerConnection.current ?: return
    val database = LocalDatabase.current
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()

    var viewType by rememberEnumPreference(PlaylistViewTypeKey, LibraryViewType.GRID)
    val (sortType, onSortTypeChange) = rememberEnumPreference(
        PlaylistSortTypeKey,
        PlaylistSortType.CREATE_DATE,
    )
    val (sortDescending, onSortDescendingChange) = rememberPreference(PlaylistSortDescendingKey, true)
    val gridItemSize by rememberEnumPreference(GridItemsSizeKey, GridItemSize.BIG)
    val pureBlack by rememberPreference(PureBlackKey, defaultValue = false)
    val betterLibraryBeta by rememberPreference(com.metrolist.music.constants.BetterLibraryBetaKey, defaultValue = false)

    val playlists by viewModel.allPlaylists.collectAsState()
    val downloadedPlaylistIds by viewModel.downloadedPlaylistIds.collectAsState()

    val likedName = stringResource(R.string.starred)
    val lastLikedThumbnails by viewModel.lastLikedThumbnails.collectAsState()
    val likedPlaylistPinned = remember(likedName, lastLikedThumbnails) {
        Playlist(
            playlist = PlaylistEntity(
                id = PlaylistEntity.LIKED_PLAYLIST_ID,
                name = likedName,
            ),
            songCount = 0,
            songThumbnails = lastLikedThumbnails,
        )
    }

    val searchQuery by viewModel.searchQuery.collectAsState()
    var searchFieldValue by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue(searchQuery))
    }
    LaunchedEffect(searchQuery) {
        if (searchFieldValue.text != searchQuery) {
            searchFieldValue = TextFieldValue(searchQuery, TextRange(searchQuery.length))
        }
    }
    val searchFocusRequester = remember { FocusRequester() }
    val normalizedQuery = remember(searchQuery) { searchQuery.normalizeForSearch() }
    val filteredPlaylists = remember(playlists, normalizedQuery, isOffline, downloadedPlaylistIds) {
        val base = if (isOffline) playlists.filter { it.id in downloadedPlaylistIds } else playlists
        if (normalizedQuery.isBlank()) {
            base
        } else {
            base.filter { playlist ->
                matchesNormalizedQuery(normalizedQuery, playlist.playlist.name)
            }
        }
    }

    val visibleResults = remember(filteredPlaylists) {
        filteredPlaylists.distinctBy { it.id }.map { playlist ->
            VisiblePlaylistItem(
                key = playlist.id,
                playlist = playlist,
                autoPlaylist = false,
            )
        }
    }

    val sortOptions = listOf(
        PlaylistSortType.CREATE_DATE  to stringResource(R.string.sort_by_create_date),
        PlaylistSortType.NAME         to stringResource(R.string.sort_by_name),
        PlaylistSortType.SONG_COUNT   to stringResource(R.string.sort_by_song_count),
        PlaylistSortType.LAST_UPDATED to stringResource(R.string.sort_by_last_updated),
    )

    val itemCountText = pluralStringResource(R.plurals.n_playlist, visibleResults.size, visibleResults.size)
    val queueTitle = stringResource(R.string.queue_all_songs)

    val lazyListState = rememberLazyListState()
    val lazyGridState = rememberLazyGridState()

    val backStackEntry by navController.currentBackStackEntryAsState()
    val scrollToTop =
        backStackEntry?.savedStateHandle?.getStateFlow("scrollToTop", false)?.collectAsState()

    val (innerTubeCookie) = rememberPreference(InnerTubeCookieKey, "")
    val isLoggedIn = remember(innerTubeCookie) {
        "SAPISID" in parseCookieString(innerTubeCookie)
    }

    val (ytmSync) = rememberPreference(YtmSyncKey, true)

    LaunchedEffect(Unit) {
        if (ytmSync) {
            withContext(Dispatchers.IO) {
                viewModel.sync()
            }
        }
    }

    LaunchedEffect(scrollToTop?.value) {
        if (scrollToTop?.value == true) {
            when (if (viewType == LibraryViewType.LIST) LibraryViewType.GRID else viewType) {
                LibraryViewType.LIST -> lazyListState.animateScrollToItem(0)
                else -> lazyGridState.animateScrollToItem(0)
            }
            backStackEntry?.savedStateHandle?.set("scrollToTop", false)
        }
    }

    var showCreatePlaylistDialog by rememberSaveable { mutableStateOf(false) }

    if (showCreatePlaylistDialog) {
        CreatePlaylistDialog(
            onDismiss = { showCreatePlaylistDialog = false },
            initialTextFieldValue = initialTextFieldValue,
            allowSyncing = allowSyncing,
            onPlaylistCreated = { playlistId ->
                showCreatePlaylistDialog = false
                navController.navigate("local_playlist/$playlistId")
            },
        )
    }

    val scrollBehavior = if (betterLibraryBeta) {
        TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    } else {
        TopAppBarDefaults.exitUntilCollapsedScrollBehavior(
            snapAnimationSpec = tween(durationMillis = 200),
        )
    }

    val revealState = rememberLibraryPageRevealState()
    val topBarRevealProgress = rememberLibraryTopBarProgress(
        state = revealState,
        scrolledPastHeader = if (viewType == LibraryViewType.LIST) {
            lazyListState.firstVisibleItemIndex > 0
        } else {
            lazyGridState.firstVisibleItemIndex > 0
        },
    )
    val screenProgress = rememberEnterProgress(play = true, durationMillis = IrideMotion.Short, easing = IrideMotion.EaseOutQuart)

    Box(modifier = Modifier.fillMaxSize()) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(if (pureBlack) Color.Black else MaterialTheme.colorScheme.background)

            .graphicsLayer { alpha = screenProgress },
    ) {
        CompositionLocalProvider(
            LocalItemHorizontalPadding provides false,
            LocalContentColor provides Color.White,
            LocalLibraryTextColor provides Color.White,
        ) {
            when (if (viewType == LibraryViewType.LIST) LibraryViewType.GRID else viewType) {
                LibraryViewType.LIST -> Unit
                LibraryViewType.GRID, LibraryViewType.GRID_WIDE -> {
                    LibraryGridPage(
                        titleRes = R.string.playlists,
                        onNavigateUp = { navController.navigateUp() },
                        modifier = Modifier.rubberBandOverscroll(Orientation.Vertical, lazyGridState),
                        state = lazyGridState,
                        columns = if (viewType == LibraryViewType.GRID_WIDE) GridCells.Fixed(3) else GridCells.Fixed(2),
                        bottomPadding = LocalPlayerAwareWindowInsets.current
                            .asPaddingValues().calculateBottomPadding(),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        menuActions = buildList {
                            if (!isOffline) add(
                                LibraryPageMenuAction(
                                    label = stringResource(R.string.download_all_playlists),
                                    icon = R.drawable.download,
                                    onClick = viewModel::downloadAllPlaylists,
                                ),
                            )
                            add(
                                LibraryPageMenuAction(
                                    label = stringResource(R.string.create_playlist),
                                    icon = R.drawable.add,
                                    onClick = { showCreatePlaylistDialog = true },
                                ),
                            )
                        },
                        playbackActions = {
                            LibraryPlaybackActions(
                                onShuffle = {
                                    coroutineScope.launch {
                                        val chosen = filteredPlaylists.randomOrNull() ?: return@launch
                                        val queue = loadPlaylistMediaItems(database, chosen).shuffled()
                                        if (queue.isNotEmpty()) playerConnection.playQueue(ListQueue(queueTitle, queue, 0))
                                    }
                                },
                                onRotation = {
                                    coroutineScope.launch {
                                        val items = filteredPlaylists.flatMap { loadPlaylistMediaItems(database, it) }
                                        val queue = database.buildRotationQueue(items)
                                        if (queue.isNotEmpty()) playerConnection.playQueue(ListQueue(queueTitle, queue, 0))
                                    }
                                },
                            )
                        },
                    ) {
                        item(
                            key = "page_header",
                            span = { GridItemSpan(maxLineSpan) },
                            contentType = CONTENT_TYPE_HEADER,
                        ) {
                            IrideSearchBox(
                                query = searchFieldValue,
                                onQueryChange = { value ->
                                    searchFieldValue = value
                                    viewModel.updateSearchQuery(value.text)
                                },
                                placeholderText = stringResource(R.string.search_playlists),
                                focusRequester = searchFocusRequester,
                                onFocusChanged = {},
                                onSearch = { keyboardController?.hide() },
                                onClear = {
                                    searchFieldValue = TextFieldValue()
                                    viewModel.updateSearchQuery("")
                                },
                                modifier = Modifier.padding(bottom = 8.dp),
                            )
                        }

                        item(
                            key = "sort",
                            span = { GridItemSpan(maxLineSpan) },
                            contentType = CONTENT_TYPE_HEADER,
                        ) {
                            LibrarySortRow(
                                sortOptions = sortOptions,
                                currentSort = sortType,
                                onSortChange = onSortTypeChange,
                                sortDescending = sortDescending,
                                onSortDescendingChange = onSortDescendingChange,
                                viewType = if (viewType == LibraryViewType.GRID_WIDE) LibraryViewType.GRID_WIDE else LibraryViewType.GRID,
                                onViewTypeChange = { viewType = it },
                                availableViewTypes = listOf(LibraryViewType.GRID, LibraryViewType.GRID_WIDE),
                                verticalPadding = 4.dp,
                            )
                        }

                        item(key = "liked_pinned", contentType = { CONTENT_TYPE_PLAYLIST }) {
                            PlaylistGridItem(
                                playlist = likedPlaylistPinned,
                                fillMaxWidth = true,
                                autoPlaylist = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .combinedClickable(
                                        onClick = { navController.navigate("auto_playlist/liked") },
                                    )
                                    .animateItem(),
                            )
                        }

                        if (visibleResults.isEmpty()) {
                            item(span = { GridItemSpan(maxLineSpan) }) {
                                if (searchQuery.isNotBlank()) {
                                    LibrarySearchEmptyPlaceholder(modifier = Modifier.animateItem())
                                } else {
                                    LibrarySearchEmptyPlaceholder(
                                        modifier = Modifier.animateItem(),
                                        icon = R.drawable.playlist_play,
                                        text = stringResource(R.string.library_playlist_empty),
                                    )
                                }
                            }
                        }

                        items(
                            items = visibleResults,
                            key = { it.key },
                            contentType = { CONTENT_TYPE_PLAYLIST },
                        ) { item ->
                            if (item.autoPlaylist) {
                                PlaylistGridItem(
                                    playlist = item.playlist,
                                    fillMaxWidth = true,
                                    autoPlaylist = true,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .combinedClickable(
                                            onClick = {
                                                item.route?.let(navController::navigate)
                                            },
                                        )
                                        .animateItem(),
                                )
                            } else {
                                LibraryPlaylistGridItem(
                                    navController = navController,
                                    menuState = menuState,
                                    coroutineScope = coroutineScope,
                                    playlist = item.playlist,
                                    modifier = Modifier.animateItem(),
                                )
                            }
                        }

                        item(
                            key = "footer",
                            span = { GridItemSpan(maxLineSpan) },
                        ) {
                            LibraryFooterCount(text = itemCountText)
                        }
                    }

                }
            }
        }
    }

    }
}
private suspend fun loadPlaylistMediaItems(
    database: com.metrolist.music.db.MusicDatabase,
    playlist: Playlist,
): List<androidx.media3.common.MediaItem> {
    val storedSongs = database.playlistSongs(playlist.id).first()
    if (storedSongs.isNotEmpty()) return storedSongs.map { it.song.toMediaItem() }
    return YouTube.playlist(playlist.playlist.id)
        .completed()
        .getOrNull()
        ?.songs
        .orEmpty()
        .map { it.toMediaMetadata().toMediaItem() }
}
