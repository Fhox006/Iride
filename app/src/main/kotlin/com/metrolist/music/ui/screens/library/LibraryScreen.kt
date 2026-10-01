/*
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.screens.library


import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.items as listItems
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.offline.Download
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.NavController
import com.metrolist.innertube.utils.completed
import com.metrolist.innertube.utils.parseCookieString
import com.metrolist.innertube.YouTube
import com.metrolist.music.constants.AlbumSortType
import com.metrolist.music.constants.AlbumSortTypeKey
import com.metrolist.music.constants.AlbumViewTypeKey
import com.metrolist.music.constants.ArtistSongSortType
import com.metrolist.music.constants.ArtistSortType
import com.metrolist.music.constants.ArtistSortTypeKey
import com.metrolist.music.constants.ArtistViewTypeKey
import com.metrolist.music.constants.CONTENT_TYPE_ALBUM
import com.metrolist.music.constants.CONTENT_TYPE_ARTIST
import com.metrolist.music.constants.CONTENT_TYPE_HEADER
import com.metrolist.music.constants.CONTENT_TYPE_PLAYLIST
import com.metrolist.music.constants.CONTENT_TYPE_SONG
import com.metrolist.music.constants.GridItemSize
import com.metrolist.music.constants.GridItemsSizeKey
import com.metrolist.music.constants.GridThumbnailHeight
import com.metrolist.music.constants.HideExplicitKey
import com.metrolist.music.constants.InnerTubeCookieKey
import com.metrolist.music.constants.LibraryViewType
import com.metrolist.music.constants.MixSortDescendingKey
import com.metrolist.music.constants.MixSortType
import com.metrolist.music.constants.MixSortTypeKey
import com.metrolist.music.constants.MixViewTypeKey
import com.metrolist.music.constants.PlaylistSortType
import com.metrolist.music.constants.PlaylistSortTypeKey
import com.metrolist.music.constants.PlaylistViewTypeKey
import com.metrolist.music.constants.PureBlackKey
import com.metrolist.music.constants.ShowCachedPlaylistKey
import com.metrolist.music.constants.ShowDownloadedPlaylistKey
import com.metrolist.music.constants.ShowUploadedPlaylistKey
import com.metrolist.music.constants.SongSortType
import com.metrolist.music.constants.SongSortTypeKey
import com.metrolist.music.constants.StatPeriod
import com.metrolist.music.constants.YtmSyncKey
import com.metrolist.music.db.entities.Album
import com.metrolist.music.db.entities.Artist
import com.metrolist.music.db.entities.Playlist
import com.metrolist.music.db.entities.PlaylistEntity
import com.metrolist.music.db.entities.Song
import com.metrolist.music.extensions.matchesNormalizedQuery
import com.metrolist.music.extensions.normalizeForSearch
import com.metrolist.music.extensions.reversed
import com.metrolist.music.extensions.toMediaItem
import com.metrolist.music.LocalDatabase
import com.metrolist.music.LocalDownloadUtil
import com.metrolist.music.LocalPlayerAwareWindowInsets
import com.metrolist.music.LocalPlayerConnection
import com.metrolist.music.models.toMediaMetadata
import com.metrolist.music.playback.queues.buildRotationQueue
import com.metrolist.music.playback.queues.ListQueue
import com.metrolist.music.playback.queues.weightedRandomOrNull
import com.metrolist.music.R
import com.metrolist.music.ui.component.AlbumGridItem
import com.metrolist.music.ui.component.ArtistGridItem
import com.metrolist.music.ui.component.ChipsRow
import com.metrolist.music.ui.component.CollapsingScreenHeader
import com.metrolist.music.ui.component.CreatePlaylistDialog
import com.metrolist.music.ui.component.DefaultDialog
import com.metrolist.music.ui.component.EmptyPlaceholder
import com.metrolist.music.ui.component.HideOnScrollFAB
import com.metrolist.music.ui.component.IrideAdaptiveTopBar
import com.metrolist.music.ui.component.IridePageLayout
import com.metrolist.music.ui.component.IridePageLayoutDefaults
import com.metrolist.music.ui.component.IrideSearchBox
import com.metrolist.music.ui.component.IrideSegmentedToggle
import com.metrolist.music.ui.component.LibraryAlbumGridItem
import com.metrolist.music.ui.component.LibraryAlbumListItem
import com.metrolist.music.ui.component.LibraryArtistGridItem
import com.metrolist.music.ui.component.LibraryArtistListItem
import com.metrolist.music.ui.component.LibraryContinueListeningAlbumItem
import com.metrolist.music.ui.component.LibraryFooterCount
import com.metrolist.music.ui.component.LibraryGridPage
import com.metrolist.music.ui.component.LibraryHeroTitle
import com.metrolist.music.ui.component.LibraryListPage
import com.metrolist.music.ui.component.LibraryPageMenuAction
import com.metrolist.music.ui.component.LibraryPageTopBar
import com.metrolist.music.ui.component.LibraryPlaybackActions
import com.metrolist.music.ui.component.LibraryPlaylistGridItem
import com.metrolist.music.ui.component.LibraryPlaylistListItem
import com.metrolist.music.ui.component.LibrarySearchEmptyPlaceholder
import com.metrolist.music.ui.component.LibrarySectionLabel
import com.metrolist.music.ui.component.libraryGridMenuActions
import com.metrolist.music.ui.component.librarySortMenuActions
import com.metrolist.music.ui.component.LibrarySuggestedFollowArtistItem
import com.metrolist.music.ui.component.LibrarySuggestedFollowArtistListItem
import com.metrolist.music.ui.component.LocalItemHorizontalPadding
import com.metrolist.music.ui.component.LocalLibraryTextColor
import com.metrolist.music.ui.component.LocalMenuState
import com.metrolist.music.ui.component.PlaylistGridItem
import com.metrolist.music.ui.component.PlaylistListItem
import com.metrolist.music.ui.component.rememberLibraryPageRevealState
import com.metrolist.music.ui.component.rememberLibraryTopBarProgress
import com.metrolist.music.ui.component.rubberBandOverscroll
import com.metrolist.music.ui.component.SongGridItem
import com.metrolist.music.ui.component.SongListItem
import com.metrolist.music.ui.menu.AlbumMenu
import com.metrolist.music.ui.menu.ArtistMenu
import com.metrolist.music.ui.menu.PlaylistMenu
import com.metrolist.music.ui.menu.SelectionSongMenu
import com.metrolist.music.ui.menu.SongMenu
import com.metrolist.music.ui.theme.SpaceMonoFontFamily
import com.metrolist.music.ui.theme.strokeHairline
import com.metrolist.music.ui.theme.textPrimary
import com.metrolist.music.ui.theme.textSecondary
import com.metrolist.music.ui.theme.textTertiary
import com.metrolist.music.ui.utils.irideEnter
import com.metrolist.music.ui.utils.IrideMotion
import com.metrolist.music.ui.utils.IrideTabEntrance
import com.metrolist.music.ui.utils.rememberDiscreteProgress
import com.metrolist.music.ui.utils.rememberEnterProgress
import com.metrolist.music.ui.utils.rememberReducedMotion
import com.metrolist.music.ui.utils.rememberSectionEnter
import com.metrolist.music.ui.utils.revealMask
import com.metrolist.music.utils.rememberEnumPreference
import com.metrolist.music.utils.rememberPreference
import com.metrolist.music.viewmodels.CachePlaylistViewModel
import com.metrolist.music.viewmodels.LibraryAlbumsViewModel
import com.metrolist.music.viewmodels.LibraryArtistsViewModel
import com.metrolist.music.viewmodels.LibraryMixViewModel
import com.metrolist.music.viewmodels.LibraryPlaylistsViewModel
import com.metrolist.music.viewmodels.LibrarySongsViewModel
import java.text.Collator
import java.time.LocalDateTime
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class LibraryCategory(@StringRes val titleRes: Int) {
    HOME(R.string.filter_library),
    ALBUMS(R.string.albums),
    PLAYLISTS(R.string.playlists),
    ARTISTS(R.string.artists),
    SONGS(R.string.all_tracks),
    LOCAL_SONGS(R.string.local_songs),
    CACHE(R.string.offline_ready_songs),
}

@Composable
private fun LibrarySearchAndActions(
    query: TextFieldValue,
    onQueryChange: (TextFieldValue) -> Unit,
    placeholderRes: Int,
    focusRequester: FocusRequester,
    onSearch: () -> Unit,
    onClear: () -> Unit,
    bottomContentSpacing: androidx.compose.ui.unit.Dp = 0.dp,
    showActions: Boolean = true,
    actions: (@Composable () -> Unit)?,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        IrideSearchBox(
            query = query,
            onQueryChange = onQueryChange,
            placeholderText = stringResource(placeholderRes),
            focusRequester = focusRequester,
            onFocusChanged = {},
            onSearch = onSearch,
            onClear = onClear,
            showBorder = false,
        )
        if (showActions) actions?.invoke()
        Spacer(Modifier.height(bottomContentSpacing))
    }
}

@Composable
fun LibraryScreen(
    navController: NavController,
    initialCategory: LibraryCategory = LibraryCategory.HOME,
    initialOffline: Boolean = false,
    initialCache: Boolean = false,
) {
    var category by rememberSaveable { mutableStateOf(if (initialCache) LibraryCategory.CACHE else initialCategory) }
    var isOffline by rememberSaveable { mutableStateOf(initialOffline || initialCache) }

    val onSelectCategory: (LibraryCategory, Boolean) -> Unit = { selected, offline ->
        category = selected
        isOffline = offline && selected != LibraryCategory.LOCAL_SONGS
    }

    BackHandler(enabled = category != LibraryCategory.HOME) {
        category = LibraryCategory.HOME
    }

    when (category) {
        LibraryCategory.HOME -> LibraryMixScreen(
            navController = navController,
            initialOffline = isOffline,
            onSelectCategory = onSelectCategory,
        )
        LibraryCategory.ALBUMS -> LibraryAlbumsScreen(
            navController = navController,
            isOffline = isOffline,
            onSelectCategory = onSelectCategory,
            onReturnToLibrary = { category = LibraryCategory.HOME },
        )
        LibraryCategory.PLAYLISTS -> LibraryPlaylistsScreen(
            navController = navController,
            isOffline = isOffline,
            onSelectCategory = onSelectCategory,
            onReturnToLibrary = { category = LibraryCategory.HOME },
        )
        LibraryCategory.ARTISTS -> LibraryArtistsScreen(
            navController = navController,
            isOffline = isOffline,
            onSelectCategory = onSelectCategory,
            onReturnToLibrary = { category = LibraryCategory.HOME },
        )
        LibraryCategory.SONGS -> LibrarySongsScreen(
            navController = navController,
            isOffline = isOffline,
            onSelectCategory = onSelectCategory,
            onReturnToLibrary = { category = LibraryCategory.HOME },
        )
        LibraryCategory.LOCAL_SONGS -> LibrarySongsScreen(
            navController = navController,
            isLocal = true,
            onSelectCategory = onSelectCategory,
            onReturnToLibrary = { category = LibraryCategory.HOME },
        )
        LibraryCategory.CACHE -> LibrarySongsScreen(
            navController = navController,
            isOffline = isOffline,
            isCache = true,
            onSelectCategory = onSelectCategory,
            onReturnToLibrary = { category = LibraryCategory.HOME },
        )
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun LibraryAlbumsScreen(
    navController: NavController,
    isOffline: Boolean = false,
    onSelectCategory: ((LibraryCategory, Boolean) -> Unit)? = null,
    onReturnToLibrary: (() -> Unit)? = null,
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
        AlbumSortType.YEAR        to stringResource(R.string.sort_by_year),
        AlbumSortType.NAME        to stringResource(R.string.sort_by_name),
        AlbumSortType.SONG_COUNT  to stringResource(R.string.sort_by_song_count),
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
                        onNavigateUp = { onReturnToLibrary?.invoke() ?: navController.navigateUp() },
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
                        menuSections = listOf(
                            libraryGridMenuActions(gridViewType) { viewType = it },
                            librarySortMenuActions(sortOptions, sortType, onSortTypeChange),
                        ),
                        menuActions = listOf(
                            LibraryPageMenuAction(
                                label = stringResource(
                                    if (downloadState == Download.STATE_STOPPED) R.string.download_all_albums else R.string.remove_download,
                                ),
                                icon = R.drawable.download,
                                onClick = onDownloadAllClick,
                            ),
                        ),
                    ) {
                        item(key = "library_search_actions", span = { GridItemSpan(maxLineSpan) }) {
                            LibrarySearchAndActions(
                                query = searchFieldValue,
                                onQueryChange = { value ->
                                    searchFieldValue = value
                                    viewModel.updateSearchQuery(value.text)
                                },
                                placeholderRes = R.string.search_albums,
                                focusRequester = searchFocusRequester,
                                onSearch = { keyboardController?.hide() },
                                onClear = {
                                    searchFieldValue = TextFieldValue()
                                    viewModel.updateSearchQuery("")
                                },
                                actions = {
                                    LibraryPlaybackActions(
                                onShuffle = {
                                    coroutineScope.launch {
                                        val monthlyStats = database.mostPlayedAlbums(
                                            fromTimeStamp = StatPeriod.MONTH_1.toTimeMillis(),
                                            limit = 100,
                                        ).first()
                                        val monthlyTimes = monthlyStats.associate { it.id to (it.timeListened ?: 0L) }
                                        val listenedAlbums = filteredAlbums.filter { monthlyTimes.containsKey(it.id) }
                                        val album = if (listenedAlbums.isNotEmpty() && kotlin.random.Random.nextFloat() < 0.85f) {
                                            weightedRandomOrNull(listenedAlbums) { monthlyTimes[it.id] ?: 0L }
                                        } else {
                                            filteredAlbums.randomOrNull()
                                        } ?: return@launch
                                        val queue = loadAlbumMediaItems(database, album).shuffled()
                                        if (queue.isNotEmpty()) playerConnection.playQueue(ListQueue(album.album.title, queue, 0))
                                    }
                                },
                                onRotation = {
                                    coroutineScope.launch {
                                        val playedAlbums = database.mostPlayedAlbums(
                                            fromTimeStamp = StatPeriod.MONTH_1.toTimeMillis(),
                                            limit = 100,
                                        ).first()
                                        val visibleAlbumIds = filteredAlbums.mapTo(mutableSetOf()) { it.id }
                                        val strongestListenTime = playedAlbums.firstOrNull()?.timeListened ?: 0L
                                        val rotationAlbums = playedAlbums
                                            .filter { it.id in visibleAlbumIds }
                                            .filter { (it.timeListened ?: 0L) >= maxOf(30_000L, strongestListenTime / 20) }
                                            .take(20)
                                        val items = rotationAlbums.flatMap { loadAlbumMediaItems(database, it) }
                                        val queue = database.buildRotationQueue(items)
                                        if (queue.isNotEmpty()) playerConnection.playQueue(ListQueue(albumsQueueTitle, queue, 0))
                                    }
                                },
                                    )
                                },
                            )
                        }

                        if (!continueListeningHidden && continueListeningAlbums.isNotEmpty()) {
                            item(key = "continue_listening_title", span = { GridItemSpan(maxLineSpan) }) { continueListeningTitle() }
                            item(key = "continue_listening_row", span = { GridItemSpan(maxLineSpan) }) { continueListeningRow() }
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

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun LibraryArtistsScreen(
    navController: NavController,
    isOffline: Boolean = false,
    onSelectCategory: ((LibraryCategory, Boolean) -> Unit)? = null,
    onReturnToLibrary: (() -> Unit)? = null,
    viewModel: LibraryArtistsViewModel = hiltViewModel(),
) {
    val menuState = LocalMenuState.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val coroutineScope = rememberCoroutineScope()
    val playerConnection = LocalPlayerConnection.current ?: return
    val database = LocalDatabase.current
    var viewType by rememberEnumPreference(ArtistViewTypeKey, LibraryViewType.GRID)

    val (sortType, onSortTypeChange) = rememberEnumPreference(
        ArtistSortTypeKey,
        ArtistSortType.CREATE_DATE
    )
    val (ytmSync) = rememberPreference(YtmSyncKey, true)
    val pureBlack by rememberPreference(PureBlackKey, defaultValue = false)

    val sortOptions = listOf(
        ArtistSortType.CREATE_DATE to stringResource(R.string.sort_by_create_date),
        ArtistSortType.NAME        to stringResource(R.string.sort_by_name),
        ArtistSortType.SONG_COUNT  to stringResource(R.string.sort_by_song_count),
        ArtistSortType.PLAY_TIME   to stringResource(R.string.sort_by_play_time),
    )

    LaunchedEffect(Unit) {
        if (ytmSync) {
            withContext(Dispatchers.IO) {
                viewModel.sync()
            }
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
    val filteredArtistsRaw by viewModel.filteredArtists.collectAsState()
    val filteredArtists = if (isOffline) emptyList() else filteredArtistsRaw
    val itemCountText = pluralStringResource(R.plurals.n_artist, filteredArtists.size, filteredArtists.size)
    val queueTitle = stringResource(R.string.queue_all_songs)

    val suggestedFollowArtists by viewModel.suggestedFollowArtists.collectAsState()
    val showSuggestedFollow = !isOffline && searchQuery.isBlank() && suggestedFollowArtists.isNotEmpty()

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

    LaunchedEffect(viewType) {
        if (viewType == LibraryViewType.LIST) viewType = LibraryViewType.GRID
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(if (pureBlack) Color.Black else MaterialTheme.colorScheme.background),
        ) {
            CompositionLocalProvider(
                LocalItemHorizontalPadding provides false,
                LocalContentColor provides Color.White,
                LocalLibraryTextColor provides Color.White,
            ) {
                LibraryGridPage(
                    titleRes = R.string.artists,
                    onNavigateUp = { onReturnToLibrary?.invoke() ?: navController.navigateUp() },
                    state = lazyGridState,
                    columns = if (viewType == LibraryViewType.GRID_WIDE) GridCells.Fixed(3) else GridCells.Fixed(2),
                    bottomPadding = LocalPlayerAwareWindowInsets.current
                        .asPaddingValues().calculateBottomPadding(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    menuSections = listOf(
                        libraryGridMenuActions(viewType) { viewType = it },
                        librarySortMenuActions(sortOptions, sortType, onSortTypeChange),
                    ),
                ) {
                    item(key = "library_search_actions", span = { GridItemSpan(maxLineSpan) }) {
                        LibrarySearchAndActions(
                            query = searchFieldValue,
                            onQueryChange = { value ->
                                searchFieldValue = value
                                viewModel.updateSearchQuery(value.text)
                            },
                            placeholderRes = R.string.search_artists,
                            focusRequester = searchFocusRequester,
                            onSearch = { keyboardController?.hide() },
                            onClear = {
                                searchFieldValue = TextFieldValue()
                                viewModel.updateSearchQuery("")
                            },
                            showActions = filteredArtists.isNotEmpty(),
                            actions = {
                                LibraryPlaybackActions(
                                onShuffle = {
                                    coroutineScope.launch {
                                        val monthlyStats = database.mostPlayedArtists(
                                            StatPeriod.MONTH_1.toTimeMillis(),
                                            limit = 100,
                                        ).first()
                                        val monthlyTimes = monthlyStats.associate { it.id to (it.timeListened ?: 0).toLong() }
                                        val listenedArtists = filteredArtists.filter { monthlyTimes.containsKey(it.id) }
                                        val artist = if (listenedArtists.isNotEmpty() && kotlin.random.Random.nextFloat() < 0.85f) {
                                            weightedRandomOrNull(listenedArtists) { monthlyTimes[it.id] ?: 0L }
                                        } else {
                                            filteredArtists.randomOrNull()
                                        } ?: return@launch
                                        val queue = database.artistSongs(artist.id, ArtistSongSortType.CREATE_DATE, true)
                                            .first().map { it.toMediaItem() }.distinctBy { it.mediaId }.shuffled()
                                        if (queue.isNotEmpty()) playerConnection.playQueue(ListQueue(artist.artist.name, queue, 0))
                                    }
                                },
                                onRotation = {
                                    coroutineScope.launch {
                                        val ids = filteredArtists.map { it.id }.toSet()
                                        val playedArtists = database.mostPlayedArtists(
                                            StatPeriod.MONTH_1.toTimeMillis(),
                                            limit = 100,
                                        ).first()
                                        val strongestListenTime = playedArtists.firstOrNull()?.timeListened?.toLong() ?: 0L
                                        val ranked = playedArtists
                                            .filter { it.id in ids }
                                            .filter { (it.timeListened ?: 0).toLong() >= maxOf(30_000L, strongestListenTime / 20) }
                                            .take(20)
                                        val tracks = ranked.flatMap { artist ->
                                            database.artistSongs(artist.id, ArtistSongSortType.CREATE_DATE, true)
                                                .first().map { it.toMediaItem() }
                                        }.distinctBy { it.mediaId }
                                        val queue = database.buildRotationQueue(tracks)
                                        if (queue.isNotEmpty()) playerConnection.playQueue(ListQueue(queueTitle, queue, 0))
                                    }
                                },
                                )
                            },
                        )
                    }

                    if (filteredArtists.isEmpty()) {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            if (searchQuery.isNotBlank()) {
                                LibrarySearchEmptyPlaceholder(
                                    icon = R.drawable.search,
                                    text = stringResource(R.string.no_results_found),
                                    modifier = Modifier.animateItem(),
                                )
                            } else {
                                LibrarySearchEmptyPlaceholder(
                                    icon = R.drawable.artist,
                                    text = stringResource(R.string.library_artist_empty),
                                    modifier = Modifier.animateItem(),
                                )
                            }
                        }
                    }

                    items(
                        items = filteredArtists,
                        key = { it.id },
                        contentType = { CONTENT_TYPE_ARTIST },
                    ) { artist ->
                        LibraryArtistGridItem(
                            navController = navController,
                            menuState = menuState,
                            coroutineScope = coroutineScope,
                            modifier = Modifier.animateItem(),
                            artist = artist,
                        )
                    }

                    if (showSuggestedFollow) {
                        items(
                            items = suggestedFollowArtists,
                            key = { "suggested_follow_${it.id}" },
                            contentType = { CONTENT_TYPE_ARTIST },
                        ) { artist ->
                            LibrarySuggestedFollowArtistItem(
                                navController = navController,
                                artist = artist,
                                onFollow = { viewModel.followSuggestedArtist(artist.id) },
                                onDismiss = { viewModel.dismissSuggestedFollowArtist(artist.id) },
                                modifier = Modifier.animateItem(),
                            )
                        }
                    }

                    item(key = "footer", span = { GridItemSpan(maxLineSpan) }) {
                        LibraryFooterCount(text = itemCountText)
                    }
                }
            }
        }
    }
}

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
    onSelectCategory: ((LibraryCategory, Boolean) -> Unit)? = null,
    onReturnToLibrary: (() -> Unit)? = null,
) {
    val menuState = LocalMenuState.current
    val haptic = LocalHapticFeedback.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val coroutineScope = rememberCoroutineScope()
    val playerConnection = LocalPlayerConnection.current ?: return
    val database = LocalDatabase.current
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()
    val activeQueueTitle by playerConnection.queueTitle.collectAsState()

    var viewType by rememberEnumPreference(PlaylistViewTypeKey, LibraryViewType.GRID)
    val (sortType, onSortTypeChange) = rememberEnumPreference(
        PlaylistSortTypeKey,
        PlaylistSortType.CREATE_DATE,
    )
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
                        onNavigateUp = { onReturnToLibrary?.invoke() ?: navController.navigateUp() },
                        state = lazyGridState,
                        columns = if (viewType == LibraryViewType.GRID_WIDE) GridCells.Fixed(3) else GridCells.Fixed(2),
                        bottomPadding = LocalPlayerAwareWindowInsets.current
                            .asPaddingValues().calculateBottomPadding(),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        menuSections = listOf(
                            libraryGridMenuActions(viewType) { viewType = it },
                            librarySortMenuActions(sortOptions, sortType, onSortTypeChange),
                        ),
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
                    ) {
                        item(key = "library_search_actions", span = { GridItemSpan(maxLineSpan) }) {
                            LibrarySearchAndActions(
                                query = searchFieldValue,
                                onQueryChange = { value ->
                                    searchFieldValue = value
                                    viewModel.updateSearchQuery(value.text)
                                },
                                placeholderRes = R.string.search_playlists,
                                focusRequester = searchFocusRequester,
                                onSearch = { keyboardController?.hide() },
                                onClear = {
                                    searchFieldValue = TextFieldValue()
                                    viewModel.updateSearchQuery("")
                                },
                                actions = {
                                    LibraryPlaybackActions(
                                onShuffle = {
                                    coroutineScope.launch {
                                        val chosen = filteredPlaylists.randomOrNull() ?: return@launch
                                        val queue = loadPlaylistMediaItems(database, chosen).shuffled()
                                        if (queue.isNotEmpty()) playerConnection.playQueue(ListQueue(chosen.playlist.name, queue, 0))
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
                                isActive = activeQueueTitle == item.playlist.playlist.name,
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

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun LibrarySongsScreen(
    navController: NavController,
    isOffline: Boolean = false,
    isCache: Boolean = false,
    isLocal: Boolean = false,
    onSelectCategory: ((LibraryCategory, Boolean) -> Unit)? = null,
    onReturnToLibrary: (() -> Unit)? = null,
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val menuState = LocalMenuState.current
    val haptic = LocalHapticFeedback.current
    val database = LocalDatabase.current
    val coroutineScope = rememberCoroutineScope()
    val queueAllSongsStr = stringResource(R.string.queue_all_songs)
    val cachedPlaylistStr = stringResource(R.string.offline_ready_songs)
    val playerConnection = LocalPlayerConnection.current ?: return
    val isPlaying by playerConnection.isEffectivelyPlaying.collectAsState()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()

    val (sortType, onSortTypeChange) =
        rememberEnumPreference(SongSortTypeKey, SongSortType.CREATE_DATE)

    val (ytmSync) = rememberPreference(YtmSyncKey, true)
    val hideExplicit by rememberPreference(key = HideExplicitKey, defaultValue = false)
    val pureBlack by rememberPreference(PureBlackKey, defaultValue = false)
    val betterLibraryBeta by rememberPreference(com.metrolist.music.constants.BetterLibraryBetaKey, defaultValue = false)

    val viewModel = if (isCache) null else hiltViewModel<LibrarySongsViewModel>()
    val cacheViewModel = if (isCache) hiltViewModel<CachePlaylistViewModel>() else null
    val emptySongs = remember { kotlinx.coroutines.flow.MutableStateFlow(emptyList<com.metrolist.music.db.entities.Song>()) }
    val cacheSongs by (cacheViewModel?.cachedSongs ?: emptySongs).collectAsState()
    val regularSongsFlow = when {
        isLocal -> viewModel?.localSongs
        isOffline -> viewModel?.downloadedSongs
        else -> viewModel?.allSongs
    } ?: emptySongs
    val regularSongs by regularSongsFlow.collectAsState()
    val localSongs by (viewModel?.localSongs ?: emptySongs).collectAsState()
    val songs = when {
        isCache -> cacheSongs
        isLocal -> regularSongs
        else -> (regularSongs + localSongs).distinctBy { it.id }
    }
    var searchFieldValue by rememberSaveable(stateSaver = TextFieldValue.Saver) { mutableStateOf(TextFieldValue()) }
    val searchFocusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        if (ytmSync && !isCache && !isLocal) {
            viewModel?.syncLibrarySongs()
        }
    }

    var inSelectMode by rememberSaveable(isCache) { mutableStateOf(false) }
    val selection = rememberSaveable(isCache, saver = listSaver<MutableList<String>, String>(
        save = { it.toList() },
        restore = { it.toMutableStateList() },
    )) { mutableStateListOf() }
    var selectionAnchorSongId by rememberSaveable(isCache) { mutableStateOf<String?>(null) }
    val onExitSelectionMode = {
        inSelectMode = false
        selection.clear()
        selectionAnchorSongId = null
    }
    BackHandler(enabled = isCache && inSelectMode, onBack = onExitSelectionMode)

    val filteredSongs by remember(songs, hideExplicit, searchFieldValue.text) {
        derivedStateOf {
            val visible = (if (hideExplicit) songs.filter { !it.song.explicit } else songs)
            if (searchFieldValue.text.isBlank()) visible else visible.filter { song ->
                song.song.title.contains(searchFieldValue.text, true) ||
                    song.album?.title?.contains(searchFieldValue.text, true) == true ||
                    song.artists.any { it.name.contains(searchFieldValue.text, true) }
            }
        }
    }

    LaunchedEffect(filteredSongs, isCache) {
        if (isCache) {
            selection.removeAll { selectedId -> filteredSongs.none { it.id == selectedId } }
            if (selectionAnchorSongId != null && filteredSongs.none { it.id == selectionAnchorSongId }) {
                selectionAnchorSongId = filteredSongs.firstOrNull { it.id in selection }?.id
            }
        }
    }

    val lazyListState = rememberLazyListState()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val scrollToTop = backStackEntry?.savedStateHandle?.getStateFlow("scrollToTop", false)?.collectAsState()

    LaunchedEffect(scrollToTop?.value) {
        if (scrollToTop?.value == true) {
            lazyListState.animateScrollToItem(0)
            backStackEntry?.savedStateHandle?.set("scrollToTop", false)
        }
    }

    val selectionTopBar: @Composable () -> Unit = {
        IrideAdaptiveTopBar(
            title = { Text(pluralStringResource(R.plurals.n_song, selection.size, selection.size)) },
            navigationIcon = {
                IconButton(onClick = onExitSelectionMode) {
                    Icon(painter = painterResource(R.drawable.close), contentDescription = null)
                }
            },
            actions = {
                androidx.compose.material3.Checkbox(
                    checked = selection.size == filteredSongs.size && selection.isNotEmpty(),
                    onCheckedChange = {
                        if (selection.size == filteredSongs.size) selection.clear()
                        else {
                            selection.clear()
                            selection.addAll(filteredSongs.map { it.id })
                        }
                    },
                )
                IconButton(
                    enabled = selection.isNotEmpty(),
                    onClick = {
                        menuState.show {
                            SelectionSongMenu(
                                songSelection = filteredSongs.filter { it.id in selection },
                                onDismiss = menuState::dismiss,
                                clearAction = onExitSelectionMode,
                            )
                        }
                    },
                ) {
                    Icon(painter = painterResource(R.drawable.more_vert), contentDescription = null)
                }
            },
        )
    }

    LibraryTracksPage(
        titleRes = when {
            isCache -> R.string.offline_ready_songs
            isLocal -> R.string.local_songs
            else -> R.string.all_tracks
        },
        songs = filteredSongs,
        state = lazyListState,
        query = searchFieldValue,
        onQueryChange = {
            searchFieldValue = it
            viewModel?.updateSearchQuery(it.text)
        },
        searchPlaceholderRes = if (isOffline || isCache) R.string.search_downloads else R.string.search_songs,
        searchFocusRequester = searchFocusRequester,
        sortType = sortType,
        onSortTypeChange = onSortTypeChange,
        emptyMessageRes = when {
            isCache -> R.string.playlist_is_empty
            isOffline -> R.string.no_downloads_yet
            else -> null
        },
        modifier = Modifier
            .fillMaxSize()
            .background(if (pureBlack) Color.Black else MaterialTheme.colorScheme.background),
        bottomPadding = LocalPlayerAwareWindowInsets.current.asPaddingValues().calculateBottomPadding(),
        onNavigateUp = { onReturnToLibrary?.invoke() ?: navController.navigateUp() },
        showNavigationPill = !isCache || !inSelectMode,
        onShuffle = {
            val queue = filteredSongs.shuffled().map { it.toMediaItem() }
            if (queue.isNotEmpty()) playerConnection.playQueue(ListQueue(queueAllSongsStr, queue, 0))
        },
        onRotation = {
            coroutineScope.launch {
                val queue = database.buildRotationQueue(filteredSongs.map { it.toMediaItem() })
                if (queue.isNotEmpty()) playerConnection.playQueue(ListQueue(queueAllSongsStr, queue, 0))
            }
        },
        overlay = { if (isCache && inSelectMode) selectionTopBar() },
    ) { song, index ->
        val onCheckedChange: (Boolean) -> Unit = { checked ->
            if (checked) selection.add(song.id) else selection.remove(song.id)
        }
        SongListItem(
            song = song,
            isActive = song.id == mediaMetadata?.id,
            isPlaying = isPlaying,
            showLikedIcon = false,
            trailingContent = {
                if (isCache && inSelectMode) {
                    androidx.compose.material3.Checkbox(
                        checked = song.id in selection,
                        onCheckedChange = onCheckedChange,
                    )
                } else {
                    IconButton(onClick = {
                        menuState.show {
                            SongMenu(
                                originalSong = song,
                                navController = navController,
                                onDismiss = menuState::dismiss,
                                isFromCache = isCache,
                            )
                        }
                    }) {
                        Icon(painter = painterResource(R.drawable.more_vert), contentDescription = stringResource(R.string.more_options))
                    }
                }
            },
            modifier = Modifier.fillMaxWidth().combinedClickable(
                onClick = {
                    if (isCache && inSelectMode) {
                        onCheckedChange(song.id !in selection)
                    } else if (song.id == mediaMetadata?.id) {
                        playerConnection.togglePlayPause()
                    } else {
                        playerConnection.playQueue(
                            ListQueue(
                                title = if (isCache) cachedPlaylistStr else queueAllSongsStr,
                                items = filteredSongs.map { it.toMediaItem() },
                                startIndex = index,
                            ),
                        )
                    }
                },
                onLongClick = if (isCache) {
                    {
                        if (!inSelectMode) {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            inSelectMode = true
                            onCheckedChange(true)
                            selectionAnchorSongId = song.id
                        } else {
                            val anchorIndex = selectionAnchorSongId?.let { anchor ->
                                filteredSongs.indexOfFirst { it.id == anchor }
                            } ?: -1
                            if (anchorIndex < 0) {
                                onCheckedChange(true)
                                selectionAnchorSongId = song.id
                            } else {
                                val range = if (anchorIndex <= index) anchorIndex..index else index..anchorIndex
                                range.forEach { rangeIndex -> selection.add(filteredSongs[rangeIndex].id) }
                            }
                        }
                    }
                } else null,
            ).animateItem(),
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LibraryTracksPage(
    titleRes: Int,
    songs: List<Song>,
    state: LazyListState,
    query: TextFieldValue,
    onQueryChange: (TextFieldValue) -> Unit,
    searchPlaceholderRes: Int,
    searchFocusRequester: FocusRequester,
    sortType: SongSortType,
    onSortTypeChange: (SongSortType) -> Unit,
    emptyMessageRes: Int?,
    modifier: Modifier = Modifier,
    bottomPadding: androidx.compose.ui.unit.Dp,
    onNavigateUp: () -> Unit,
    showNavigationPill: Boolean = true,
    onShuffle: () -> Unit,
    onRotation: () -> Unit,
    overlay: @Composable BoxScope.() -> Unit = {},
    rowContent: @Composable LazyItemScope.(Song, Int) -> Unit,
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val itemCountText = pluralStringResource(R.plurals.n_song, songs.size, songs.size)
    val sortOptions = listOf(
        SongSortType.CREATE_DATE to stringResource(R.string.sort_by_create_date),
        SongSortType.NAME to stringResource(R.string.sort_by_name),
        SongSortType.PLAY_TIME to stringResource(R.string.sort_by_play_time),
    )

    CompositionLocalProvider(
        LocalItemHorizontalPadding provides false,
        LocalContentColor provides Color.White,
        LocalLibraryTextColor provides Color.White,
    ) {
        LibraryListPage(
            titleRes = titleRes,
            state = state,
            modifier = modifier,
            bottomPadding = bottomPadding,
            onNavigateUp = onNavigateUp,
            showNavigationPill = showNavigationPill,
            menuSections = listOf(librarySortMenuActions(sortOptions, sortType, onSortTypeChange)),
            overlay = overlay,
        ) {
            item(key = "library_search_actions", contentType = CONTENT_TYPE_HEADER) {
                LibrarySearchAndActions(
                    query = query,
                    onQueryChange = onQueryChange,
                    placeholderRes = searchPlaceholderRes,
                    focusRequester = searchFocusRequester,
                    onSearch = { keyboardController?.hide() },
                    onClear = { onQueryChange(TextFieldValue()) },
                    bottomContentSpacing = 8.dp,
                    actions = { LibraryPlaybackActions(onShuffle, onRotation) },
                )
            }
            if (songs.isEmpty() && query.text.isBlank() && emptyMessageRes != null) {
                item(key = "empty_tracks") {
                    EmptyPlaceholder(
                        icon = if (emptyMessageRes == R.string.no_downloads_yet) R.drawable.check else R.drawable.music_note,
                        text = stringResource(emptyMessageRes),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
                    )
                }
            } else if (songs.isEmpty() && query.text.isNotBlank()) {
                item(key = "empty_search_result", contentType = CONTENT_TYPE_HEADER) {
                    LibrarySearchEmptyPlaceholder(modifier = Modifier.animateItem())
                }
            }
            itemsIndexed(songs, key = { _, song -> song.id }, contentType = { _, _ -> CONTENT_TYPE_SONG }) { index, song ->
                rowContent(song, index)
            }
            item(key = "footer") { LibraryFooterCount(text = itemCountText) }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun LibraryMixScreen(
    navController: NavController,
    initialOffline: Boolean = false,
    onSelectCategory: ((LibraryCategory, Boolean) -> Unit)? = null,
    viewModel: LibraryMixViewModel = hiltViewModel(),
) {
    val menuState = LocalMenuState.current
    val haptic = LocalHapticFeedback.current
    val queueSearchedSongsStr = stringResource(R.string.queue_searched_songs)
    val playerConnection = LocalPlayerConnection.current ?: return
    val isPlaying by playerConnection.isEffectivelyPlaying.collectAsState()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()

    var viewType by rememberEnumPreference(MixViewTypeKey, LibraryViewType.GRID)
    val isListView = viewType == LibraryViewType.LIST
    val contentGutter = if (isListView) 0.dp else 12.dp
    val (sortType, onSortTypeChange) =
        rememberEnumPreference(
            MixSortTypeKey,
            MixSortType.CREATE_DATE,
        )
    val (sortDescending, onSortDescendingChange) = rememberPreference(MixSortDescendingKey, true)
    val gridItemSize by rememberEnumPreference(GridItemsSizeKey, GridItemSize.BIG)

    val sortOptions = listOf(
        MixSortType.CREATE_DATE  to stringResource(R.string.sort_by_create_date),
        MixSortType.NAME         to stringResource(R.string.sort_by_name),
        MixSortType.LAST_UPDATED to stringResource(R.string.sort_by_last_updated),
    )

    val (ytmSync) = rememberPreference(YtmSyncKey, true)

    var isLibraryFilter by viewModel.isLibraryMode
    val isOffline = !isLibraryFilter
    LaunchedEffect(initialOffline) {
        isLibraryFilter = !initialOffline
    }
    val libraryTabKey = "library_$isLibraryFilter"
    val screenProgress = if (IrideTabEntrance.wasRevealed(libraryTabKey)) {
        1f
    } else {
        rememberEnterProgress(play = true, durationMillis = IrideMotion.Short, easing = IrideMotion.EaseOutQuart)
            .also { if (it >= 1f) IrideTabEntrance.markRevealed(libraryTabKey) }
    }
    val revealedSections = remember(isLibraryFilter) { IrideTabEntrance.sectionsFor(libraryTabKey) }
    val searchQuery by viewModel.searchQuery.collectAsState()
    val debouncedSearchQuery by viewModel.debouncedSearchQuery.collectAsState()
    val normalizedQuery = remember(debouncedSearchQuery) { debouncedSearchQuery.normalizeForSearch() }

    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val hardResetContext = LocalContext.current
    var hardResetArmed by remember { mutableStateOf(false) }
    LaunchedEffect(isRefreshing) {
        if (isRefreshing) {
            hardResetArmed = true
        } else if (hardResetArmed) {
            hardResetArmed = false
            val restartIntent = hardResetContext.packageManager
                .getLaunchIntentForPackage(hardResetContext.packageName)
                ?.let { Intent.makeRestartActivityTask(it.component) }
            if (restartIntent != null) {
                hardResetContext.startActivity(restartIntent)
                Runtime.getRuntime().exit(0)
            }
        }
    }
    val topSize by viewModel.topValue.collectAsState(initial = 50)
    val lastLikedDate by viewModel.lastLikedDate.collectAsState()
    val lastLikedThumbnails by viewModel.lastLikedThumbnails.collectAsState()
    val likedPlaylistName = stringResource(R.string.starred)
    val likedPlaylist = remember(lastLikedDate, likedPlaylistName, lastLikedThumbnails) {
        Playlist(
            playlist = PlaylistEntity(
                id = PlaylistEntity.LIKED_PLAYLIST_ID,
                name = likedPlaylistName,
                createdAt = lastLikedDate,
                lastUpdateTime = lastLikedDate,
            ),
            songCount = 0,
            songThumbnails = lastLikedThumbnails,
        )
    }

    val downloadPlaylist =
        Playlist(
            playlist =
                PlaylistEntity(
                    id = UUID.randomUUID().toString(),
                    name = stringResource(R.string.offline),
                ),
            songCount = 0,
            songThumbnails = emptyList(),
        )

    val topPlaylist =
        Playlist(
            playlist =
                PlaylistEntity(
                    id = UUID.randomUUID().toString(),
                    name = stringResource(R.string.my_top) + " $topSize",
                ),
            songCount = 0,
            songThumbnails = emptyList(),
        )

    val cachedPlaylist =
        Playlist(
            playlist =
                PlaylistEntity(
                    id = UUID.randomUUID().toString(),
                    name = stringResource(R.string.offline_ready_songs),
                ),
            songCount = 0,
            songThumbnails = emptyList(),
        )

    val uploadedPlaylist =
        Playlist(
            playlist =
                PlaylistEntity(
                    id = UUID.randomUUID().toString(),
                    name = stringResource(R.string.uploaded_playlist),
                ),
            songCount = 0,
            songThumbnails = emptyList(),
        )

    val downloadedSongs by viewModel.downloadedSongs.collectAsState()
    val downloadedPlaylistIds by viewModel.downloadedPlaylistIds.collectAsState()
    val cacheViewModel: CachePlaylistViewModel = hiltViewModel()
    val cachedSongs by cacheViewModel.cachedSongs.collectAsState()

    val (showDownloaded) = rememberPreference(ShowDownloadedPlaylistKey, false)
    val (showCached) = rememberPreference(ShowCachedPlaylistKey, false)
    val (showUploaded) = rememberPreference(ShowUploadedPlaylistKey, false)

    val showDownloadedPlaylist =
        showDownloaded && downloadedSongs.isNotEmpty() && matchesNormalizedQuery(normalizedQuery, downloadPlaylist.playlist.name)
    val showTopPlaylists = false
    val showUploadedPlaylists =
        showUploaded && matchesNormalizedQuery(normalizedQuery, uploadedPlaylist.playlist.name)
    val showCachedPlaylists = isOffline && !isLibraryFilter && showCached && cachedSongs.isNotEmpty() &&
        matchesNormalizedQuery(normalizedQuery, cachedPlaylist.playlist.name)


    val albums = viewModel.albums.collectAsState()
    val artist = viewModel.artists.collectAsState()
    val songs = viewModel.songs.collectAsState()
    val playlist = viewModel.playlists.collectAsState()
    val uploadedSongs by viewModel.uploadedSongs.collectAsState()
    val downloadedAlbums by viewModel.downloadedAlbums.collectAsState()
    val localSongs by viewModel.localSongs.collectAsState()
    val locale = LocalLocale.current.platformLocale
    val collator = remember(locale) {
        Collator.getInstance(locale).apply {
            strength = Collator.PRIMARY
        }
    }
    val base = if (!isLibraryFilter) {
        downloadedAlbums
    } else {
        val likedEntry = if (lastLikedDate != null) listOf(likedPlaylist) else emptyList()
        albums.value + artist.value + playlist.value.filterNot { playlist ->
            playlist.id == PlaylistEntity.WEEKLY_MOST_PLAYLIST_ID ||
                playlist.id == PlaylistEntity.MONTHLY_MOST_PLAYLIST_ID
        } + likedEntry
    }
    var allItems = when (sortType) {
        MixSortType.CREATE_DATE -> {
            base.sortedBy { item ->
                when (item) {
                    is Album -> item.album.bookmarkedAt
                    is Artist -> item.artist.bookmarkedAt
                    is Playlist -> item.playlist.createdAt
                    is Song -> item.song.dateDownload ?: item.song.inLibrary ?: LocalDateTime.now()
                }
            }
        }
        MixSortType.NAME -> {
            base.sortedWith(
                compareBy(collator) { item ->
                    when (item) {
                        is Album -> item.album.title
                        is Artist -> item.artist.name
                        is Playlist -> item.playlist.name
                        is Song -> item.song.title
                    }
                },
            )
        }
        MixSortType.LAST_UPDATED -> {
            base.sortedBy { item ->
                when (item) {
                    is Album -> item.album.lastUpdateTime
                    is Artist -> item.artist.lastUpdateTime
                    is Playlist -> item.playlist.lastUpdateTime
                    is Song -> item.song.dateDownload ?: item.song.inLibrary ?: LocalDateTime.now()
                }
            }
        }
    }.reversed(sortDescending)

    val searchableItems = if (normalizedQuery.isBlank()) allItems else allItems + songs.value

    val filteredItems = remember(searchableItems, normalizedQuery, collator) {
        val matchedItems =
            searchableItems.filter { item ->
                when (item) {
                    is Song -> {
                        val artistNames = item.orderedArtists.map { it.name }.toTypedArray()
                        matchesNormalizedQuery(normalizedQuery, item.song.title, item.song.albumName, *artistNames)
                    }

                    is Album -> {
                        val artistNames = item.artists.map { it.name }.toTypedArray()
                        matchesNormalizedQuery(normalizedQuery, item.album.title, *artistNames)
                    }

                    is Artist -> matchesNormalizedQuery(normalizedQuery, item.artist.name)
                    is Playlist -> matchesNormalizedQuery(normalizedQuery, item.playlist.name)
                }
            }

        if (normalizedQuery.isBlank()) {
            val distinct = matchedItems.distinctBy { it.id }
            val (pinned, rest) = distinct.partition { it is Playlist && it.playlist.id == PlaylistEntity.LIKED_PLAYLIST_ID }
            pinned + rest
        } else {
            matchedItems
                .sortedWith { first, second ->
                    val firstPriority =
                        when (first) {
                            is Playlist -> 0
                            is Song -> 1
                            is Artist -> 2
                            is Album -> 3
                        }
                    val secondPriority =
                        when (second) {
                            is Playlist -> 0
                            is Song -> 1
                            is Artist -> 2
                            is Album -> 3
                        }

                    if (firstPriority != secondPriority) {
                        firstPriority.compareTo(secondPriority)
                    } else {
                        val firstName =
                            when (first) {
                                is Playlist -> first.playlist.name
                                is Song -> first.song.title
                                is Artist -> first.artist.name
                                is Album -> first.album.title
                            }
                        val secondName =
                            when (second) {
                                is Playlist -> second.playlist.name
                                is Song -> second.song.title
                                is Artist -> second.artist.name
                                is Album -> second.album.title
                            }
                        collator.compare(firstName, secondName)
                    }
                }
                .distinctBy { it.id }
        }
    }

    val coroutineScope = rememberCoroutineScope()

    val lazyGridState = rememberLazyGridState()
    val lazyListState = rememberLazyListState()
    val reducedMotion = rememberReducedMotion()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val scrollToTop =
        backStackEntry?.savedStateHandle?.getStateFlow("scrollToTop", false)?.collectAsState()

    LaunchedEffect(scrollToTop?.value) {
        if (scrollToTop?.value == true) {
            if (isListView) lazyListState.animateScrollToItem(0) else lazyGridState.animateScrollToItem(0)
            backStackEntry?.savedStateHandle?.set("scrollToTop", false)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.updateSearchQuery("")
        if (ytmSync) {
            withContext(Dispatchers.IO) {
                viewModel.syncAllLibrary()
            }
        }
    }

    val onFilterToggle = { isLibraryFilter = !isLibraryFilter }

    MaterialTheme(
        colorScheme = MaterialTheme.colorScheme.copy(
            background = Color.Black,
            surface = Color.Black,
            surfaceContainer = Color.Black,
            surfaceContainerHigh = Color(0xFF202020),
            secondary = Color.White,
            tertiary = Color.White,
            onBackground = Color.White,
            onSurface = Color.White,
            onSurfaceVariant = Color.White,
            onPrimaryContainer = Color.White,
            onSecondaryContainer = Color.White,
        ),
    ) {
    CompositionLocalProvider(LocalContentColor provides Color.White) {
    Scaffold(
        modifier = Modifier,
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets(0),
    ) { paddingValues ->
        Box(
            modifier = Modifier.fillMaxSize()
                .background(Color.Black),
        ) {
            CompositionLocalProvider(LocalItemHorizontalPadding provides false) {
                if (isListView) {
                    IridePageLayout(
                        titleRes = R.string.filter_library,
                        state = lazyListState,
                        reducedMotion = reducedMotion,
                        listModifier = Modifier
                            .graphicsLayer { alpha = screenProgress },
                        contentHorizontalPadding = 20.dp,
                        bottomPadding = LocalPlayerAwareWindowInsets.current.asPaddingValues().calculateBottomPadding(),
                    ) {
                        item(key = "library_filter_toggle") {
                            IrideSegmentedToggle(
                                options = listOf(
                                    true to stringResource(R.string.filter_library),
                                    false to stringResource(R.string.filter_downloaded),
                                ),
                                selected = isLibraryFilter,
                                onSelect = { value -> if (value != isLibraryFilter) onFilterToggle() },
                                modifier = Modifier.padding(top = 4.dp, bottom = 12.dp),
                            )
                        }
                        item(key = "categories") {
                            CategoriesContent(
                                navController = navController,
                                showUploads = uploadedSongs.isNotEmpty(),
                                showLocalSongs = localSongs.isNotEmpty(),
                                showDownloadedPlaylists = downloadedPlaylistIds.isNotEmpty(),
                                showDownloadedAlbums = downloadedAlbums.isNotEmpty(),
                                showDownloadedSongs = downloadedSongs.isNotEmpty(),
                                showOfflineReady = cachedSongs.isNotEmpty(),
                                isOffline = !isLibraryFilter,
                                onSelectCategory = onSelectCategory,
                                useIrideStyle = true,
                            )
                        }
                        if (normalizedQuery.isBlank()) {
                            item(key = "recently_added_label", contentType = CONTENT_TYPE_HEADER) {
                                LibrarySectionLabel(
                                    text = if (isOffline && downloadedSongs.isEmpty() && localSongs.isEmpty()) {
                                        stringResource(R.string.download_songs_to_see_recent)
                                    } else {
                                        stringResource(if (isLibraryFilter) R.string.recently_added else R.string.recently_downloaded)
                                    },
                                    modifier = Modifier.padding(top = 20.dp),
                                )
                            }
                        }
                        if (showDownloadedPlaylist) item(key = "downloadedPlaylist") {
                            PlaylistListItem(
                                playlist = downloadPlaylist,
                                autoPlaylist = true,
                                modifier = Modifier.fillMaxWidth().clickable {
                                    if (onSelectCategory != null) onSelectCategory(LibraryCategory.SONGS, true)
                                    else navController.navigate("auto_playlist/downloaded")
                                },
                            )
                        }
                        if (showCachedPlaylists) item(key = "cachedPlaylist") {
                            PlaylistListItem(
                                playlist = cachedPlaylist,
                                autoPlaylist = true,
                                modifier = Modifier.fillMaxWidth().clickable {
                                    if (onSelectCategory != null) onSelectCategory(LibraryCategory.CACHE, true)
                                    else navController.navigate("cache_playlist/cached")
                                },
                            )
                        }
                        if (showTopPlaylists) item(key = "TopPlaylist") {
                            PlaylistListItem(
                                playlist = topPlaylist,
                                autoPlaylist = true,
                                modifier = Modifier.fillMaxWidth().clickable { navController.navigate("top_playlist/$topSize") },
                            )
                        }
                        if (showUploadedPlaylists) item(key = "uploadedPlaylist") {
                            PlaylistListItem(
                                playlist = uploadedPlaylist,
                                autoPlaylist = true,
                                modifier = Modifier.fillMaxWidth().clickable { navController.navigate("auto_playlist/uploaded") },
                            )
                        }
                        listItems(filteredItems) { item ->
                            when (item) {
                                is Playlist -> {
                                    if (item.id == PlaylistEntity.LIKED_PLAYLIST_ID) {
                                        PlaylistListItem(
                                            playlist = item,
                                            autoPlaylist = true,
                                            modifier = Modifier.fillMaxWidth().clickable { navController.navigate("auto_playlist/liked") },
                                        )
                                    } else {
                                        LibraryPlaylistListItem(navController, menuState, coroutineScope, item)
                                    }
                                }
                                is Album -> LibraryAlbumListItem(
                                    navController = navController,
                                    menuState = menuState,
                                    album = item,
                                    isActive = item.id == mediaMetadata?.album?.id,
                                    isPlaying = isPlaying,
                                )
                                is Artist -> LibraryArtistListItem(navController, menuState, coroutineScope, item)
                                is Song -> {
                                    val onClick = {
                                        if (item.id == mediaMetadata?.id) {
                                            playerConnection.togglePlayPause()
                                        } else {
                                            val filteredSongs = filteredItems.filterIsInstance<Song>()
                                            playerConnection.playQueue(
                                                ListQueue(
                                                    title = queueSearchedSongsStr,
                                                    items = filteredSongs.map { it.toMediaItem() },
                                                    startIndex = filteredSongs.indexOfFirst { it.id == item.id },
                                                ),
                                            )
                                        }
                                    }
                                    SongListItem(
                                        song = item,
                                        isActive = item.id == mediaMetadata?.id,
                                        isPlaying = isPlaying,
                                        modifier = Modifier.fillMaxWidth().combinedClickable(
                                            onClick = onClick,
                                            onLongClick = {
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                menuState.show {
                                                    SongMenu(
                                                        originalSong = item,
                                                        navController = navController,
                                                        onDismiss = menuState::dismiss,
                                                    )
                                                }
                                            },
                                        ),
                                    )
                                }
                            }
                        }
                        if (isLibraryFilter && normalizedQuery.isBlank()) item(key = "manual_refresh") {
                            LibraryRefreshButton(
                                isRefreshing = isRefreshing,
                                useIrideStyle = true,
                                onClick = { viewModel.refresh() },
                            )
                        }
                        if (filteredItems.isEmpty() && !showDownloadedPlaylist && !showCachedPlaylists &&
                            !showTopPlaylists && !showUploadedPlaylists && searchQuery.isNotBlank()
                        ) item(key = "empty_search_result") {
                            LibrarySearchEmptyPlaceholder()
                        }
                    }
                } else {
                IridePageLayout(
                    titleRes = R.string.filter_library,
                    state = lazyGridState,
                    reducedMotion = reducedMotion,
                    gridModifier = Modifier
                        .graphicsLayer { alpha = screenProgress },
                    columns = when (viewType) {
                        LibraryViewType.GRID_WIDE -> GridCells.Fixed(3)
                        else -> GridCells.Adaptive(
                            minSize = GridThumbnailHeight + if (gridItemSize == GridItemSize.BIG) 24.dp else (-24).dp,
                        )
                    },
                    bottomPadding = LocalPlayerAwareWindowInsets.current.asPaddingValues().calculateBottomPadding(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    item(key = "library_filter_toggle", span = { GridItemSpan(maxLineSpan) }) {
                        Row(
                            modifier = Modifier
                                .padding(top = 4.dp, bottom = 12.dp)
                                .animateItem(placementSpec = IrideMotion.PlacementSpec)
                                .irideEnter(rememberSectionEnter("library_filter_toggle", revealedSections), 8.dp),
                        ) {
                            IrideSegmentedToggle(
                                options = listOf(
                                    true to stringResource(R.string.filter_library),
                                    false to stringResource(R.string.filter_downloaded),
                                ),
                                selected = isLibraryFilter,
                                onSelect = { value -> if (value != isLibraryFilter) onFilterToggle() },
                            )
                        }
                    }

                    item(key = "categories", span = { GridItemSpan(maxLineSpan) }) {
                        CategoriesContent(
                            navController = navController,
                            showUploads = uploadedSongs.isNotEmpty(),
                            showLocalSongs = localSongs.isNotEmpty(),
                            showDownloadedPlaylists = downloadedPlaylistIds.isNotEmpty(),
                            showDownloadedAlbums = downloadedAlbums.isNotEmpty(),
                            showDownloadedSongs = downloadedSongs.isNotEmpty(),
                            showOfflineReady = cachedSongs.isNotEmpty(),
                            isOffline = !isLibraryFilter,
                            onSelectCategory = onSelectCategory,
                            useIrideStyle = true,
                            modifier = Modifier.animateItem(placementSpec = IrideMotion.PlacementSpec),
                        )
                    }

                    if (normalizedQuery.isBlank()) {
                        item(
                            key = "recently_added_label",
                            span = { GridItemSpan(maxLineSpan) },
                            contentType = CONTENT_TYPE_HEADER,
                        ) {
                            LibrarySectionLabel(
                                text = if (isOffline && downloadedSongs.isEmpty() && localSongs.isEmpty()) {
                                    stringResource(R.string.download_songs_to_see_recent)
                                } else if (isLibraryFilter) {
                                    stringResource(R.string.recently_added)
                                } else {
                                    stringResource(R.string.recently_downloaded)
                                },
                                modifier = Modifier
                                    .padding(top = 20.dp)
                                    .animateItem(placementSpec = IrideMotion.PlacementSpec)
                                    .revealMask(rememberSectionEnter("recently_added_label", revealedSections)),
                            )
                        }
                    }

                    if (showDownloadedPlaylist) {
                        item(key = "downloadedPlaylist", contentType = { CONTENT_TYPE_PLAYLIST }) {
                            Box(
                                modifier = Modifier
                                    .padding(bottom = contentGutter)
                                    .irideEnter(rememberSectionEnter("downloadedPlaylist", revealedSections)),
                            ) {
                                if (isListView) {
                                    PlaylistListItem(
                                        playlist = downloadPlaylist,
                                        autoPlaylist = true,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                if (onSelectCategory != null) onSelectCategory(LibraryCategory.SONGS, true)
                                                else navController.navigate("auto_playlist/downloaded")
                                            }
                                            .animateItem(placementSpec = IrideMotion.PlacementSpec),
                                    )
                                } else {
                                    PlaylistGridItem(
                                        playlist = downloadPlaylist,
                                        fillMaxWidth = true,
                                        autoPlaylist = true,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .combinedClickable(
                                                onClick = {
                                                    if (onSelectCategory != null) onSelectCategory(LibraryCategory.SONGS, true)
                                                    else navController.navigate("auto_playlist/downloaded")
                                                },
                                            )
                                            .animateItem(placementSpec = IrideMotion.PlacementSpec),
                                    )
                                }
                            }
                        }
                    }

                    if (showCachedPlaylists) {
                        item(key = "cachedPlaylist", contentType = { CONTENT_TYPE_PLAYLIST }) {
                            Box(
                                modifier = Modifier
                                    .padding(bottom = contentGutter)
                                    .irideEnter(rememberSectionEnter("cachedPlaylist", revealedSections)),
                            ) {
                                if (isListView) {
                                    PlaylistListItem(
                                        playlist = cachedPlaylist,
                                        autoPlaylist = true,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                if (onSelectCategory != null) onSelectCategory(LibraryCategory.CACHE, true)
                                                else navController.navigate("cache_playlist/cached")
                                            }
                                            .animateItem(placementSpec = IrideMotion.PlacementSpec),
                                    )
                                } else {
                                    PlaylistGridItem(
                                        playlist = cachedPlaylist,
                                        fillMaxWidth = true,
                                        autoPlaylist = true,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .combinedClickable(
                                                onClick = {
                                                    if (onSelectCategory != null) onSelectCategory(LibraryCategory.CACHE, true)
                                                    else navController.navigate("cache_playlist/cached")
                                                },
                                            )
                                            .animateItem(placementSpec = IrideMotion.PlacementSpec),
                                    )
                                }
                            }
                        }
                    }

                    if (showTopPlaylists) {
                        item(key = "TopPlaylist", contentType = { CONTENT_TYPE_PLAYLIST }) {
                            Box(
                                modifier = Modifier
                                    .padding(bottom = contentGutter)
                                    .irideEnter(rememberSectionEnter("TopPlaylist", revealedSections)),
                            ) {
                                if (isListView) {
                                    PlaylistListItem(
                                        playlist = topPlaylist,
                                        autoPlaylist = true,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { navController.navigate("top_playlist/$topSize") }
                                            .animateItem(placementSpec = IrideMotion.PlacementSpec),
                                    )
                                } else {
                                    PlaylistGridItem(
                                        playlist = topPlaylist,
                                        fillMaxWidth = true,
                                        autoPlaylist = true,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .combinedClickable(
                                                onClick = { navController.navigate("top_playlist/$topSize") },
                                            )
                                            .animateItem(placementSpec = IrideMotion.PlacementSpec),
                                    )
                                }
                            }
                        }
                    }

                    if (showUploadedPlaylists) {
                        item(key = "uploadedPlaylist", contentType = { CONTENT_TYPE_PLAYLIST }) {
                            Box(
                                modifier = Modifier
                                    .padding(bottom = contentGutter)
                                    .irideEnter(rememberSectionEnter("uploadedPlaylist", revealedSections)),
                            ) {
                                if (isListView) {
                                    PlaylistListItem(
                                        playlist = uploadedPlaylist,
                                        autoPlaylist = true,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { navController.navigate("auto_playlist/uploaded") }
                                            .animateItem(placementSpec = IrideMotion.PlacementSpec),
                                    )
                                } else {
                                    PlaylistGridItem(
                                        playlist = uploadedPlaylist,
                                        fillMaxWidth = true,
                                        autoPlaylist = true,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { navController.navigate("auto_playlist/uploaded") }
                                            .animateItem(placementSpec = IrideMotion.PlacementSpec),
                                    )
                                }
                            }
                        }
                    }

                    items(
                        items = filteredItems,
                        contentType = { CONTENT_TYPE_PLAYLIST },
                    ) { item ->
                        Box(modifier = Modifier.padding(bottom = contentGutter)) {
                            when (item) {
                                is Playlist -> {
                                    if (isListView) {
                                        if (item.id == PlaylistEntity.LIKED_PLAYLIST_ID) {
                                            PlaylistListItem(
                                                playlist = item,
                                                autoPlaylist = true,
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable { navController.navigate("auto_playlist/liked") }
                                                    .animateItem(placementSpec = IrideMotion.PlacementSpec),
                                            )
                                        } else {
                                            LibraryPlaylistListItem(
                                                navController = navController,
                                                menuState = menuState,
                                                coroutineScope = coroutineScope,
                                                playlist = item,
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .animateItem(placementSpec = IrideMotion.PlacementSpec),
                                            )
                                        }
                                    } else {
                                        PlaylistGridItem(
                                            playlist = item,
                                            fillMaxWidth = true,
                                            autoPlaylist = item.id == PlaylistEntity.LIKED_PLAYLIST_ID,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .combinedClickable(
                                                    onClick = {
                                                        if (item.id == PlaylistEntity.LIKED_PLAYLIST_ID) {
                                                            navController.navigate("auto_playlist/liked")
                                                        } else if (!item.playlist.isEditable && item.songCount == 0 &&
                                                            item.playlist.browseId != null
                                                        ) {
                                                            navController.navigate("online_playlist/${item.playlist.browseId}")
                                                        } else {
                                                            navController.navigate("local_playlist/${item.id}")
                                                        }
                                                    },
                                                    onLongClick = {
                                                        if (item.id != PlaylistEntity.LIKED_PLAYLIST_ID) {
                                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                            menuState.show {
                                                                PlaylistMenu(
                                                                    playlist = item,
                                                                    coroutineScope = coroutineScope,
                                                                    onDismiss = menuState::dismiss,
                                                                )
                                                            }
                                                        }
                                                    },
                                                )
                                                .animateItem(placementSpec = IrideMotion.PlacementSpec),
                                        )
                                    }
                                }

                                is Song -> {
                                    val onClick = {
                                        if (item.id == mediaMetadata?.id) {
                                            playerConnection.togglePlayPause()
                                        } else {
                                            val filteredSongs = filteredItems.filterIsInstance<Song>()
                                            playerConnection.playQueue(
                                                ListQueue(
                                                    title = queueSearchedSongsStr,
                                                    items = filteredSongs.map { it.toMediaItem() },
                                                    startIndex = filteredSongs.indexOfFirst { it.id == item.id },
                                                ),
                                            )
                                        }
                                    }
                                    val onLongClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        menuState.show {
                                            SongMenu(
                                                originalSong = item,
                                                navController = navController,
                                                onDismiss = menuState::dismiss,
                                            )
                                        }
                                    }
                                    if (isListView) {
                                        CompositionLocalProvider(LocalItemHorizontalPadding provides true) {
                                            SongListItem(
                                                song = item,
                                                isActive = item.id == mediaMetadata?.id,
                                                isPlaying = isPlaying,
                                                showLikedIcon = true,
                                                showDivider = true,
                                                hairlineBorder = false,
                                                softBorder = true,
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .combinedClickable(onClick = onClick, onLongClick = onLongClick)
                                                    .animateItem(placementSpec = IrideMotion.PlacementSpec),
                                            )
                                        }
                                    } else {
                                        SongGridItem(
                                            song = item,
                                            isActive = item.id == mediaMetadata?.id,
                                            isPlaying = isPlaying,
                                            showLikedIcon = false,
                                            fillMaxWidth = true,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .combinedClickable(onClick = onClick, onLongClick = onLongClick)
                                                .animateItem(placementSpec = IrideMotion.PlacementSpec),
                                        )
                                    }
                                }

                                is Album -> {
                                    if (isListView) {
                                        LibraryAlbumListItem(
                                            navController = navController,
                                            menuState = menuState,
                                            album = item,
                                            isActive = item.id == mediaMetadata?.album?.id,
                                            isPlaying = isPlaying,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .animateItem(placementSpec = IrideMotion.PlacementSpec),
                                        )
                                    } else {
                                        AlbumGridItem(
                                            album = item,
                                            showLikedIcon = false,
                                            isActive = item.id == mediaMetadata?.album?.id,
                                            isPlaying = isPlaying,
                                            coroutineScope = coroutineScope,
                                            fillMaxWidth = true,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .combinedClickable(
                                                    onClick = { navController.navigate("album/${item.id}") },
                                                    onLongClick = {
                                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                        menuState.show {
                                                            AlbumMenu(
                                                                originalAlbum = item,
                                                                navController = navController,
                                                                onDismiss = menuState::dismiss,
                                                            )
                                                        }
                                                    },
                                                )
                                                .animateItem(placementSpec = IrideMotion.PlacementSpec),
                                        )
                                    }
                                }

                                is Artist -> {
                                    if (!isListView) {
                                        ArtistGridItem(
                                            artist = item,
                                            showLikedIcon = false,
                                            fillMaxWidth = true,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .combinedClickable(
                                                    onClick = { navController.navigate("artist/${item.id}") },
                                                    onLongClick = {
                                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                        menuState.show {
                                                            ArtistMenu(
                                                                originalArtist = item,
                                                                coroutineScope = coroutineScope,
                                                                onDismiss = menuState::dismiss,
                                                            )
                                                        }
                                                    },
                                                )
                                                .animateItem(placementSpec = IrideMotion.PlacementSpec),
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (isLibraryFilter && normalizedQuery.isBlank()) {
                        item(key = "manual_refresh", span = { GridItemSpan(maxLineSpan) }) {
                            LibraryRefreshButton(
                                isRefreshing = isRefreshing,
                                useIrideStyle = true,
                                onClick = { viewModel.refresh() },
                                modifier = Modifier.animateItem(placementSpec = IrideMotion.PlacementSpec),
                            )
                        }
                    }

                    if (
                        filteredItems.isEmpty() &&
                        !showDownloadedPlaylist &&
                        !showCachedPlaylists &&
                        !showTopPlaylists &&
                        !showUploadedPlaylists &&
                        searchQuery.isNotBlank()
                    ) {
                        item(
                            key = "empty_search_result",
                            span = { GridItemSpan(maxLineSpan) },
                        ) {
                            LibrarySearchEmptyPlaceholder(
                                modifier = Modifier
                                    .animateItem(placementSpec = IrideMotion.PlacementSpec)
                                    .irideEnter(rememberEnterProgress(play = true, durationMillis = IrideMotion.Short)),
                            )
                        }
                    }
                }
                }
            }
        }
    }
    }
    }
}

@Composable
private fun LibraryRefreshButton(
    isRefreshing: Boolean,
    useIrideStyle: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val label = stringResource(if (isRefreshing) R.string.library_refreshing else R.string.library_refresh)
    val contentColor = if (useIrideStyle) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
    Row(
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 20.dp, bottom = 8.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(enabled = !isRefreshing, role = Role.Button, onClick = onClick)
            .heightIn(min = 48.dp)
            .padding(vertical = 14.dp)
            .semantics { contentDescription = label },
    ) {
        Icon(
            painter = painterResource(R.drawable.refresh),
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.width(10.dp))
        Text(
            text = label,
            style = if (useIrideStyle) {
                MaterialTheme.typography.labelLarge.copy(
                    fontFamily = SpaceMonoFontFamily,
                    fontSize = 13.sp,
                    letterSpacing = 0.2.sp,
                )
            } else {
                MaterialTheme.typography.labelLarge
            },
            color = contentColor,
        )
    }
}

private data class CategoryItem(
    val label: String,
    val icon: Int,
    val route: String,
)

@Composable
private fun CategoriesContent(
    navController: NavController,
    showUploads: Boolean,
    showLocalSongs: Boolean,
    showDownloadedPlaylists: Boolean,
    showDownloadedAlbums: Boolean,
    showDownloadedSongs: Boolean,
    showOfflineReady: Boolean,
    isOffline: Boolean,
    onSelectCategory: ((LibraryCategory, Boolean) -> Unit)? = null,
    useIrideStyle: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val albumsStr = stringResource(R.string.albums)
    val artistsStr = stringResource(R.string.artists)
    val playlistsStr = stringResource(R.string.playlists)
    val cacheStr = stringResource(R.string.offline_ready_songs)
    val allTracksStr = stringResource(R.string.all_tracks)
    val localSongsStr = stringResource(R.string.local_songs)
    val uploadedStr = stringResource(R.string.filter_uploaded)

    val items = remember(isOffline, showUploads, showLocalSongs, showDownloadedPlaylists, showDownloadedAlbums, showDownloadedSongs, showOfflineReady, albumsStr, artistsStr, playlistsStr, allTracksStr, cacheStr, localSongsStr, uploadedStr) {
        buildList {
            if (!isOffline || showDownloadedPlaylists) add(CategoryItem(playlistsStr, R.drawable.queue_music, if (isOffline) "library_playlists_offline" else "library_playlists"))
            if (!isOffline || showDownloadedAlbums) add(CategoryItem(albumsStr, R.drawable.album, if (isOffline) "library_albums_offline" else "library_albums"))
            if (!isOffline) add(CategoryItem(artistsStr, R.drawable.artist, "library_artists"))
            if (!isOffline || showDownloadedSongs || showLocalSongs) add(CategoryItem(allTracksStr, R.drawable.music_note, if (isOffline) "library_songs_offline" else "library_songs"))
            if (showLocalSongs) add(CategoryItem(localSongsStr, R.drawable.storage, "library_songs_local"))
            if (isOffline && showOfflineReady) add(CategoryItem(cacheStr, R.drawable.cached, "cache_playlist/cached"))
            if (isOffline && showUploads) add(CategoryItem(uploadedStr, R.drawable.upload, "auto_playlist/uploaded"))
        }
    }

    Column(modifier = modifier) {
        items.forEach { item ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .clickable {
                        val destination = when (item.route) {
                            "library_playlists", "library_playlists_offline" -> LibraryCategory.PLAYLISTS
                            "library_albums", "library_albums_offline" -> LibraryCategory.ALBUMS
                            "library_artists", "library_artists_offline" -> LibraryCategory.ARTISTS
                            "library_songs", "library_songs_offline" -> LibraryCategory.SONGS
                            "library_songs_local" -> LibraryCategory.LOCAL_SONGS
                            "cache_playlist/cached" -> LibraryCategory.CACHE
                            else -> null
                        }
                        if (destination != null && onSelectCategory != null) {
                            onSelectCategory(destination, isOffline && destination != LibraryCategory.LOCAL_SONGS)
                        } else {
                            navController.navigate(item.route)
                        }
                    },
            ) {
                Icon(
                    painter = painterResource(item.icon),
                    contentDescription = null,
                    modifier = Modifier.size(if (useIrideStyle) 20.dp else 24.dp),
                    tint = if (useIrideStyle) MaterialTheme.colorScheme.textSecondary else MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    text = item.label,
                    style = if (useIrideStyle) {
                        MaterialTheme.typography.labelLarge.copy(
                            fontFamily = SpaceMonoFontFamily,
                            fontSize = 14.sp,
                            letterSpacing = 0.2.sp,
                        )
                    } else {
                        MaterialTheme.typography.bodyLarge
                    },
                    fontWeight = if (useIrideStyle) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (useIrideStyle) MaterialTheme.colorScheme.textPrimary else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
                Icon(
                    painter = painterResource(R.drawable.navigate_next),
                    contentDescription = null,
                    modifier = Modifier.size(if (useIrideStyle) 18.dp else 24.dp),
                    tint = if (useIrideStyle) MaterialTheme.colorScheme.textSecondary else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            HorizontalDivider(
                color = if (useIrideStyle) {
                    MaterialTheme.colorScheme.strokeHairline
                } else {
                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                },
            )
        }
    }
}
