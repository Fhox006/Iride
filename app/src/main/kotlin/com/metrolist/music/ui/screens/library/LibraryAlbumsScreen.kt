/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.screens.library

import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.media3.common.MediaItem
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import com.metrolist.music.LocalDatabase
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.media3.exoplayer.offline.Download
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.metrolist.music.LocalDownloadUtil
import com.metrolist.music.LocalPlayerAwareWindowInsets
import com.metrolist.music.LocalPlayerConnection
import com.metrolist.innertube.YouTube
import com.metrolist.music.R
import com.metrolist.music.constants.AlbumSortDescendingKey
import com.metrolist.music.constants.AlbumSortType
import com.metrolist.music.constants.AlbumSortTypeKey
import com.metrolist.music.constants.AlbumViewTypeKey
import com.metrolist.music.constants.CONTENT_TYPE_ALBUM
import com.metrolist.music.constants.CONTENT_TYPE_HEADER
import com.metrolist.music.constants.HideExplicitKey
import com.metrolist.music.constants.LibraryViewType
import com.metrolist.music.constants.PureBlackKey
import com.metrolist.music.constants.YtmSyncKey
import com.metrolist.music.extensions.matchesNormalizedQuery
import com.metrolist.music.extensions.normalizeForSearch
import com.metrolist.music.ui.component.CollapsingScreenHeader
import com.metrolist.music.ui.component.DefaultDialog
import com.metrolist.music.ui.component.EmptyPlaceholder
import com.metrolist.music.ui.component.LibraryAlbumGridItem
import com.metrolist.music.ui.component.LibraryContinueListeningAlbumItem
import com.metrolist.music.ui.component.LibrarySearchEmptyPlaceholder
import com.metrolist.music.ui.component.LibrarySortRow
import com.metrolist.music.ui.component.LocalItemHorizontalPadding
import com.metrolist.music.ui.component.LocalMenuState
import com.metrolist.music.ui.component.rubberBandOverscroll
import com.metrolist.music.ui.component.LibrarySectionLabel
import com.metrolist.music.ui.component.LibraryFooterCount
import com.metrolist.music.ui.component.IrideSearchBox
import com.metrolist.music.ui.component.LibraryGridPage
import com.metrolist.music.ui.component.LibraryPageMenuAction
import com.metrolist.music.ui.component.LibraryPlaybackActions
import com.metrolist.music.extensions.toMediaItem
import com.metrolist.music.models.toMediaMetadata
import com.metrolist.music.playback.queues.ListQueue
import com.metrolist.music.playback.queues.buildRotationQueue
import com.metrolist.music.ui.component.LocalLibraryTextColor
import com.metrolist.music.ui.theme.SpaceMonoFontFamily
import com.metrolist.music.ui.utils.IrideMotion
import com.metrolist.music.ui.utils.irideEnter
import com.metrolist.music.ui.utils.rememberDiscreteProgress
import com.metrolist.music.ui.utils.rememberEnterProgress
import com.metrolist.music.ui.utils.revealMask
import com.metrolist.music.utils.rememberEnumPreference
import com.metrolist.music.utils.rememberPreference
import com.metrolist.music.viewmodels.LibraryAlbumsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun LibraryAlbumsScreen(
    navController: NavController,
    isOffline: Boolean = false,
    viewModel: LibraryAlbumsViewModel = hiltViewModel(),
) {
    val menuState = LocalMenuState.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val coroutineScope = rememberCoroutineScope()
    val playerConnection = LocalPlayerConnection.current ?: return
    val database = LocalDatabase.current
    val isPlaying by playerConnection.isEffectivelyPlaying.collectAsState()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()

    var viewType by rememberEnumPreference(AlbumViewTypeKey, LibraryViewType.GRID)
    val gridViewType = if (viewType == LibraryViewType.LIST) LibraryViewType.GRID else viewType
    LaunchedEffect(viewType) {
        if (viewType == LibraryViewType.LIST) viewType = LibraryViewType.GRID
    }
    val (sortType, onSortTypeChange) =
        rememberEnumPreference(AlbumSortTypeKey, AlbumSortType.CREATE_DATE)
    val (sortDescending, onSortDescendingChange) = rememberPreference(AlbumSortDescendingKey, true)

    val (ytmSync) = rememberPreference(YtmSyncKey, true)
    val hideExplicit by rememberPreference(key = HideExplicitKey, defaultValue = false)
    val pureBlack by rememberPreference(PureBlackKey, defaultValue = false)
    val betterLibraryBeta by rememberPreference(com.metrolist.music.constants.BetterLibraryBetaKey, defaultValue = false)

    LaunchedEffect(Unit) {
        if (ytmSync) {
            withContext(Dispatchers.IO) { viewModel.sync() }
        }
    }

    val albums by (if (isOffline) viewModel.downloadedAlbums else viewModel.allAlbums).collectAsState()
    val continueListeningAlbums by viewModel.continueListeningAlbums.collectAsState()
    val continueListeningHidden by viewModel.isContinueListeningHidden.collectAsState()
    val continueListeningAvailable = rememberUpdatedState(continueListeningAlbums.isNotEmpty())
    val continueListeningAlbumSelected = remember { mutableStateOf(false) }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        var visitActive = false
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    visitActive = true
                    continueListeningAlbumSelected.value = false
                }
                Lifecycle.Event.ON_PAUSE -> if (visitActive) {
                    visitActive = false
                    if (continueListeningAvailable.value) {
                        viewModel.recordContinueListeningVisit(continueListeningAlbumSelected.value)
                    }
                }
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            if (visitActive && continueListeningAvailable.value) {
                viewModel.recordContinueListeningVisit(continueListeningAlbumSelected.value)
            }
        }
    }

    LaunchedEffect(mediaMetadata?.album?.id, continueListeningAlbums) {
        val activeAlbumId = mediaMetadata?.album?.id
        if (activeAlbumId != null && continueListeningAlbums.any { it.id == activeAlbumId }) {
            viewModel.hideContinueListening()
        }
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

    val filteredAlbums = remember(albums, hideExplicit, normalizedQuery) {
        val visible = if (hideExplicit) albums.filter { !it.album.explicit } else albums
        visible.filter { album ->
            val artistNames = album.artists.map { it.name }.toTypedArray()
            matchesNormalizedQuery(normalizedQuery, album.album.title, *artistNames)
        }.distinctBy { it.id }
    }

    val downloadUtil = LocalDownloadUtil.current
    val likedAlbumsSongs by viewModel.likedAlbumsSongs.collectAsState()
    val isProcessingDownloads by viewModel.isProcessingDownloads.collectAsState()
    var downloadState by remember { mutableIntStateOf(Download.STATE_STOPPED) }
    var showRemoveDownloadDialog by remember { mutableStateOf(false) }

    LaunchedEffect(likedAlbumsSongs, isProcessingDownloads) {
        if (isProcessingDownloads) {
            downloadState = Download.STATE_DOWNLOADING
            return@LaunchedEffect
        }
        if (likedAlbumsSongs.isEmpty()) {
            downloadState = Download.STATE_STOPPED
            return@LaunchedEffect
        }
        downloadUtil.downloads.collect { downloads ->
            downloadState =
                if (likedAlbumsSongs.all { downloads[it.id]?.state == Download.STATE_COMPLETED }) {
                    Download.STATE_COMPLETED
                } else if (likedAlbumsSongs.any {
                        downloads[it.id]?.state == Download.STATE_QUEUED ||
                            downloads[it.id]?.state == Download.STATE_DOWNLOADING
                    }
                ) {
                    Download.STATE_DOWNLOADING
                } else {
                    Download.STATE_STOPPED
                }
        }
    }

    val onDownloadAllClick: () -> Unit = {
        when (downloadState) {
            Download.STATE_COMPLETED -> {
                showRemoveDownloadDialog = true
            }
            Download.STATE_DOWNLOADING, Download.STATE_QUEUED -> {
                viewModel.removeAllFavoriteAlbumsDownloads()
            }
            else -> {
                viewModel.downloadAllFavoriteAlbums()
            }
        }
    }

    if (showRemoveDownloadDialog) {
        DefaultDialog(
            onDismiss = { showRemoveDownloadDialog = false },
            content = {
                Text(
                    text = stringResource(R.string.remove_all_favorite_albums_confirm),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(horizontal = 18.dp),
                )
            },
            buttons = {
                TextButton(onClick = { showRemoveDownloadDialog = false }) {
                    Text(text = stringResource(R.string.cancel))
                }
                TextButton(
                    onClick = {
                        showRemoveDownloadDialog = false
                        viewModel.removeAllFavoriteAlbumsDownloads()
                    },
                ) {
                    Text(text = stringResource(R.string.remove_download))
                }
            },
        )
    }

    val sortOptions = listOf(
        AlbumSortType.CREATE_DATE to stringResource(R.string.sort_by_create_date),
        AlbumSortType.NAME       to stringResource(R.string.sort_by_name),
        AlbumSortType.ARTIST     to stringResource(R.string.sort_by_artist),
        AlbumSortType.YEAR       to stringResource(R.string.sort_by_year),
        AlbumSortType.SONG_COUNT to stringResource(R.string.sort_by_song_count),
        AlbumSortType.LENGTH     to stringResource(R.string.sort_by_length),
        AlbumSortType.PLAY_TIME  to stringResource(R.string.sort_by_play_time),
    )

    val itemCountText = pluralStringResource(R.plurals.n_album, filteredAlbums.size, filteredAlbums.size)
    val albumsQueueTitle = stringResource(R.string.albums)

    val lazyGridState = rememberLazyGridState()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val scrollToTop =
        backStackEntry?.savedStateHandle?.getStateFlow("scrollToTop", false)?.collectAsState()

    LaunchedEffect(scrollToTop?.value) {
        if (scrollToTop?.value == true) {
            lazyGridState.animateScrollToItem(0)
            backStackEntry?.savedStateHandle?.set("scrollToTop", false)
        }
    }

    val scrollBehavior = if (betterLibraryBeta) {
        TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    } else {
        TopAppBarDefaults.exitUntilCollapsedScrollBehavior(
            snapAnimationSpec = tween(durationMillis = 200),
        )
    }

    val continueListeningTitle: @Composable () -> Unit = {
        LibrarySectionLabel(
            text = stringResource(R.string.continue_listening),
            verticalPadding = 4.dp,
        )
    }
    val continueListeningRow: @Composable () -> Unit = {
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val albumTileSize = (maxWidth - 24.dp) / 3
            val continueListeningState = rememberLazyListState()
            LazyRow(
                state = continueListeningState,
                contentPadding = PaddingValues(horizontal = 0.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                overscrollEffect = null,
                modifier = Modifier.rubberBandOverscroll(Orientation.Horizontal, continueListeningState),
            ) {
                items(continueListeningAlbums, key = { "continue_listening_${it.id}" }) { album ->
                    LibraryContinueListeningAlbumItem(
                        navController = navController,
                        menuState = menuState,
                        coroutineScope = coroutineScope,
                        album = album,
                        isActive = album.id == mediaMetadata?.album?.id,
                        isPlaying = isPlaying,
                        size = albumTileSize,
                        onAlbumSelected = { continueListeningAlbumSelected.value = true },
                    )
                }
            }
        }
    }
    val albumSearchBar: @Composable () -> Unit = {
        IrideSearchBox(
            query = searchFieldValue,
            onQueryChange = { value ->
                searchFieldValue = value
                viewModel.updateSearchQuery(value.text)
            },
            placeholderText = stringResource(R.string.search_albums),
            focusRequester = searchFocusRequester,
            onFocusChanged = {},
            onSearch = { keyboardController?.hide() },
            onClear = {
                searchFieldValue = TextFieldValue()
                viewModel.updateSearchQuery("")
            },
            modifier = Modifier.padding(bottom = 12.dp),
        )
    }

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
                    LibraryGridPage(
                        titleRes = R.string.albums,
                        onNavigateUp = { navController.navigateUp() },
                        modifier = Modifier.rubberBandOverscroll(Orientation.Vertical, lazyGridState),
                        columns = if (gridViewType == LibraryViewType.GRID_WIDE) {
                            GridCells.Fixed(3)
                        } else {
                            GridCells.Fixed(2)
                        },
                        state = lazyGridState,
                        bottomPadding = LocalPlayerAwareWindowInsets.current
                            .asPaddingValues().calculateBottomPadding(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        menuActions = listOf(
                            LibraryPageMenuAction(
                                label = stringResource(
                                    if (downloadState == Download.STATE_STOPPED) R.string.download_all_albums else R.string.remove_download,
                                ),
                                icon = R.drawable.download,
                                onClick = onDownloadAllClick,
                            ),
                        ),
                        playbackActions = {
                            LibraryPlaybackActions(
                                onShuffle = {
                                    coroutineScope.launch {
                                        val album = filteredAlbums.randomOrNull() ?: return@launch
                                        val queue = loadAlbumMediaItems(database, album).shuffled()
                                        if (queue.isNotEmpty()) playerConnection.playQueue(ListQueue(album.album.title, queue, 0))
                                    }
                                },
                                onRotation = {
                                    coroutineScope.launch {
                                        val items = filteredAlbums.flatMap { loadAlbumMediaItems(database, it) }
                                        val queue = database.buildRotationQueue(items)
                                        if (queue.isNotEmpty()) playerConnection.playQueue(ListQueue(albumsQueueTitle, queue, 0))
                                    }
                                },
                            )
                        },
                    ) {
                        item(
                            key = "album_search",
                            span = { GridItemSpan(maxLineSpan) },
                            contentType = CONTENT_TYPE_HEADER,
                        ) {
                            albumSearchBar()
                        }

                        if (!continueListeningHidden && continueListeningAlbums.isNotEmpty()) {
                            item(key = "continue_listening_title", span = { GridItemSpan(maxLineSpan) }) { continueListeningTitle() }
                            item(key = "continue_listening_row", span = { GridItemSpan(maxLineSpan) }) { continueListeningRow() }
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
                                    viewType = gridViewType,
                                    onViewTypeChange = { viewType = it },
                                    availableViewTypes = listOf(LibraryViewType.GRID, LibraryViewType.GRID_WIDE),
                                    verticalPadding = 4.dp,
                                )
                            }

                            filteredAlbums.let { albums ->
                                if (albums.isEmpty()) {
                                    item(span = { GridItemSpan(maxLineSpan) }) {
                                        if (searchQuery.isNotBlank()) {
                                            LibrarySearchEmptyPlaceholder(modifier = Modifier.animateItem())
                                        } else {
                                            EmptyPlaceholder(
                                                icon = R.drawable.album,
                                                text = stringResource(R.string.library_album_empty),
                                                modifier = Modifier.animateItem(),
                                            )
                                        }
                                    }
                                }
                                items(
                                    items = albums,
                                    key = { it.id },
                                    contentType = { CONTENT_TYPE_ALBUM },
                                ) { album ->
                                    LibraryAlbumGridItem(
                                        navController = navController,
                                        menuState = menuState,
                                        coroutineScope = coroutineScope,
                                        album = album,
                                        isActive = album.id == mediaMetadata?.album?.id,
                                        isPlaying = isPlaying,
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

private suspend fun loadAlbumMediaItems(
    database: com.metrolist.music.db.MusicDatabase,
    album: com.metrolist.music.db.entities.Album,
): List<MediaItem> {
    val storedSongs = database.albumSongs(album.id).first()
    if (storedSongs.isNotEmpty()) return storedSongs.map { it.toMediaItem() }
    return YouTube.album(album.id)
        .getOrNull()
        ?.songs
        .orEmpty()
        .map { it.toMediaMetadata().toMediaItem() }
}
