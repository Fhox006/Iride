/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.screens.library

import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.media3.exoplayer.offline.Download
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.metrolist.music.LocalDownloadUtil
import com.metrolist.music.LocalPlayerAwareWindowInsets
import com.metrolist.music.LocalPlayerConnection
import com.metrolist.music.R
import com.metrolist.music.constants.AlbumSortDescendingKey
import com.metrolist.music.constants.AlbumSortType
import com.metrolist.music.constants.AlbumSortTypeKey
import com.metrolist.music.constants.AlbumViewTypeKey
import com.metrolist.music.constants.CONTENT_TYPE_ALBUM
import com.metrolist.music.constants.CONTENT_TYPE_HEADER
import com.metrolist.music.constants.GridItemSize
import com.metrolist.music.constants.GridItemsSizeKey
import com.metrolist.music.constants.GridThumbnailHeight
import com.metrolist.music.constants.AlbumTopGradientKey
import com.metrolist.music.constants.HideExplicitKey
import com.metrolist.music.constants.LibraryViewType
import com.metrolist.music.constants.PlayerBackgroundStyleKey
import com.metrolist.music.constants.PureBlackKey
import com.metrolist.music.constants.YtmSyncKey
import com.metrolist.music.extensions.matchesNormalizedQuery
import com.metrolist.music.extensions.normalizeForSearch
import com.metrolist.music.ui.component.CollapsingScreenHeader
import com.metrolist.music.ui.component.DefaultDialog
import com.metrolist.music.ui.component.EmptyPlaceholder
import com.metrolist.music.ui.component.IrideCollapsibleSection
import com.metrolist.music.ui.component.LibraryAlbumGridItem
import com.metrolist.music.ui.component.LibraryAlbumListItem
import com.metrolist.music.ui.component.LibraryContinueListeningAlbumItem
import com.metrolist.music.ui.component.LibrarySearchEmptyPlaceholder
import com.metrolist.music.ui.component.LibrarySearchHeader
import com.metrolist.music.ui.component.LibrarySortRow
import com.metrolist.music.ui.component.LocalItemHorizontalPadding
import com.metrolist.music.ui.component.LocalMenuState
import com.metrolist.music.ui.component.NavigationTitle
import com.metrolist.music.ui.component.TopScreenGradientBackground
import com.metrolist.music.ui.component.currentGridThumbnailHeight
import com.metrolist.music.ui.component.frostedTopBarBackground
import com.metrolist.music.ui.component.recordFrostBackdrop
import com.metrolist.music.ui.component.rememberFrostBackdrop
import com.metrolist.music.ui.component.rubberBandOverscroll
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
    val isPlaying by playerConnection.isEffectivelyPlaying.collectAsState()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()

    var viewType by rememberEnumPreference(AlbumViewTypeKey, LibraryViewType.GRID)
    val (sortType, onSortTypeChange) =
        rememberEnumPreference(AlbumSortTypeKey, AlbumSortType.CREATE_DATE)
    val (sortDescending, onSortDescendingChange) = rememberPreference(AlbumSortDescendingKey, true)
    val gridItemSize by rememberEnumPreference(GridItemsSizeKey, GridItemSize.BIG)

    val (ytmSync) = rememberPreference(YtmSyncKey, true)
    val hideExplicit by rememberPreference(key = HideExplicitKey, defaultValue = false)
    val pureBlack by rememberPreference(PureBlackKey, defaultValue = false)
    val betterLibraryBeta by rememberPreference(com.metrolist.music.constants.BetterLibraryBetaKey, defaultValue = false)
    val currentGridHeight = currentGridThumbnailHeight()
    val albumTopGradientEnabled by rememberPreference(AlbumTopGradientKey, defaultValue = true)
    val playerBackgroundStyle by rememberEnumPreference(
        PlayerBackgroundStyleKey,
        defaultValue = com.metrolist.music.constants.PlayerBackgroundStyle.BETTER_ANIMATED_GRADIENT,
    )

    LaunchedEffect(Unit) {
        if (ytmSync) {
            withContext(Dispatchers.IO) { viewModel.sync() }
        }
    }

    val albums by (if (isOffline) viewModel.downloadedAlbums else viewModel.allAlbums).collectAsState()
    val continueListeningAlbums by viewModel.continueListeningAlbums.collectAsState()
    var isSearchActive by rememberSaveable { mutableStateOf(false) }
    val searchQuery by viewModel.searchQuery.collectAsState()
    val normalizedQuery = remember(searchQuery) { searchQuery.normalizeForSearch() }

    val collapsedSections = remember { mutableStateMapOf<String, Boolean>() }
    fun isSectionCollapsed(key: String) = collapsedSections[key] == true
    fun toggleSection(key: String) { collapsedSections[key] = !isSectionCollapsed(key) }

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

    val lazyListState = rememberLazyListState()
    val lazyGridState = rememberLazyGridState()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val scrollToTop =
        backStackEntry?.savedStateHandle?.getStateFlow("scrollToTop", false)?.collectAsState()

    LaunchedEffect(scrollToTop?.value) {
        if (scrollToTop?.value == true) {
            when (viewType) {
                LibraryViewType.LIST -> lazyListState.animateScrollToItem(0)
                else -> lazyGridState.animateScrollToItem(0)
            }
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

    val continueListeningSpacing = 12.dp

    val continueListeningTitle: @Composable () -> Unit = {
        NavigationTitle(
            title = stringResource(R.string.continue_listening),
            useIrideStyle = true,
            collapsed = isSectionCollapsed("continue_listening"),
            onCollapseToggle = { toggleSection("continue_listening") },
            topPadding = 8.dp,
            modifier = Modifier.offset(x = (-20).dp),
        )
    }
    val continueListeningRow: @Composable () -> Unit = {
        val content: @Composable () -> Unit = {
            val continueListeningState = rememberLazyListState()
            LazyRow(
                state = continueListeningState,
                contentPadding = PaddingValues(horizontal = 0.dp),
                horizontalArrangement = Arrangement.spacedBy(continueListeningSpacing),
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
                        size = currentGridHeight,
                        onDismiss = { viewModel.dismissContinueListeningAlbum(album.id) },
                    )
                }
            }
        }
        IrideCollapsibleSection(collapsed = isSectionCollapsed("continue_listening")) { content() }
    }

    val favoritesTitle: @Composable () -> Unit = {
        Spacer(modifier = Modifier.height(8.dp))
    }
    val favoritesCollapsed = isSectionCollapsed("favorite_albums")

    val frostBackdrop = rememberFrostBackdrop()
    var titleBottomPx by remember { mutableStateOf(Float.MAX_VALUE) }
    var topBarBottomPx by remember { mutableStateOf(0f) }
    val headerTitleCovered by remember {
        derivedStateOf {
            val scrolledPastHeader = if (viewType == LibraryViewType.LIST) {
                lazyListState.firstVisibleItemIndex > 0
            } else {
                lazyGridState.firstVisibleItemIndex > 0
            }
            scrolledPastHeader || titleBottomPx <= topBarBottomPx
        }
    }
    val topBarRevealProgress = rememberDiscreteProgress(headerTitleCovered)
    val screenProgress = rememberEnterProgress(play = true, durationMillis = IrideMotion.Short, easing = IrideMotion.EaseOutQuart)

    val heroHeader: @Composable () -> Unit = {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .irideEnter(screenProgress, 10.dp),
        ) {
            Spacer(modifier = Modifier.height(28.dp))
            Text(
                text = stringResource(R.string.albums),
                style = TextStyle(
                    fontFamily = SpaceMonoFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 40.sp,
                    letterSpacing = (-0.6).sp,
                ),
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier
                    .fillMaxWidth()
                    .onGloballyPositioned { titleBottomPx = it.boundsInWindow().bottom },
            )
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(if (pureBlack) Color.Black else MaterialTheme.colorScheme.background)
            .recordFrostBackdrop(frostBackdrop)
            .graphicsLayer { alpha = screenProgress },
    ) {
        if (albumTopGradientEnabled) {
            TopScreenGradientBackground(
                mediaMetadata = mediaMetadata,
                playerBackground = playerBackgroundStyle,
            )
        }
        CompositionLocalProvider(LocalItemHorizontalPadding provides false) {
            when (viewType) {
                LibraryViewType.LIST -> {
                    LazyColumn(
                        state = lazyListState,
                        contentPadding = PaddingValues(
                            start = 20.dp,
                            end = 20.dp,
                            top = LocalPlayerAwareWindowInsets.current.asPaddingValues().calculateTopPadding(),
                            bottom = LocalPlayerAwareWindowInsets.current
                                .asPaddingValues().calculateBottomPadding(),
                        ),
                        overscrollEffect = null,
                        modifier = Modifier.rubberBandOverscroll(Orientation.Vertical, lazyListState),
                    ) {
                        item(key = "hero_header") { heroHeader() }

                        if (continueListeningAlbums.isNotEmpty()) {
                            item(key = "continue_listening_title") { continueListeningTitle() }
                            item(key = "continue_listening_row") { continueListeningRow() }
                        }

                        item(key = "favorite_albums_title") { favoritesTitle() }

                        if (!favoritesCollapsed) {
                            item(key = "sort", contentType = CONTENT_TYPE_HEADER) {
                                LibrarySortRow(
                                    sortOptions = sortOptions,
                                    currentSort = sortType,
                                    onSortChange = onSortTypeChange,
                                    sortDescending = sortDescending,
                                    onSortDescendingChange = onSortDescendingChange,
                                    viewType = viewType,
                                    onViewTypeChange = { viewType = it },
                                    useIrideStyle = true,
                                )
                            }

                            filteredAlbums.let { albums ->
                                if (albums.isEmpty()) {
                                    item(key = "empty_placeholder") {
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
                                    LibraryAlbumListItem(
                                        navController = navController,
                                        menuState = menuState,
                                        album = album,
                                        isActive = album.id == mediaMetadata?.album?.id,
                                        isPlaying = isPlaying,
                                        modifier = Modifier.animateItem(),
                                    )
                                }
                            }
                            item(key = "footer") {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 16.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        text = itemCountText,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    }
                }

                LibraryViewType.GRID, LibraryViewType.GRID_WIDE -> {
                    LazyVerticalGrid(
                        state = lazyGridState,
                        columns = if (viewType == LibraryViewType.GRID_WIDE) {
                            GridCells.Fixed(3)
                        } else {
                            GridCells.Adaptive(
                                minSize = GridThumbnailHeight +
                                    if (gridItemSize == GridItemSize.BIG) 24.dp else (-24).dp,
                            )
                        },
                        contentPadding = PaddingValues(
                            start = 20.dp,
                            end = 20.dp,
                            top = LocalPlayerAwareWindowInsets.current.asPaddingValues().calculateTopPadding(),
                            bottom = LocalPlayerAwareWindowInsets.current
                                .asPaddingValues().calculateBottomPadding(),
                        ),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        overscrollEffect = null,
                        modifier = Modifier.rubberBandOverscroll(Orientation.Vertical, lazyGridState),
                    ) {
                        item(key = "hero_header", span = { GridItemSpan(maxLineSpan) }) { heroHeader() }

                        if (continueListeningAlbums.isNotEmpty()) {
                            item(key = "continue_listening_title", span = { GridItemSpan(maxLineSpan) }) { continueListeningTitle() }
                            item(key = "continue_listening_row", span = { GridItemSpan(maxLineSpan) }) { continueListeningRow() }
                        }

                        item(key = "favorite_albums_title", span = { GridItemSpan(maxLineSpan) }) { favoritesTitle() }

                        if (!favoritesCollapsed) {
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
                                    viewType = viewType,
                                    onViewTypeChange = { viewType = it },
                                    useIrideStyle = true,
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
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 16.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        text = itemCountText,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

        val backProgress = rememberEnterProgress(play = true, durationMillis = IrideMotion.Short)
        LibrarySearchHeader(
            isSearchActive = isSearchActive,
            searchQuery = searchQuery,
            onSearchQueryChange = viewModel::updateSearchQuery,
            onBack = {
                isSearchActive = false
                viewModel.updateSearchQuery("")
            },
            keyboardController = keyboardController,
            modifier = Modifier
                .fillMaxWidth()
                .onGloballyPositioned { topBarBottomPx = it.boundsInWindow().bottom }
                .frostedTopBarBackground(
                    progress = topBarRevealProgress,
                    barColor = MaterialTheme.colorScheme.background,
                    strokeColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f),
                    backdrop = frostBackdrop,
                )
                .statusBarsPadding()
                .height(56.dp)
                .padding(horizontal = 4.dp),
        ) {
            Box(modifier = Modifier.irideEnter(backProgress, 6.dp)) {
                IconButton(onClick = { navController.navigateUp() }) {
                    Icon(
                        painter = painterResource(R.drawable.arrow_back),
                        contentDescription = null,
                    )
                }
            }
            Text(
                text = stringResource(R.string.albums),
                style = TextStyle(
                    fontFamily = SpaceMonoFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    letterSpacing = (-0.1).sp,
                ),
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 4.dp)
                    .irideEnter(topBarRevealProgress, 6.dp)
                    .revealMask(topBarRevealProgress),
            )
            IconButton(onClick = onDownloadAllClick) {
                when (downloadState) {
                    Download.STATE_COMPLETED -> {
                        Icon(
                            painter = painterResource(R.drawable.offline),
                            contentDescription = stringResource(R.string.all_favorite_albums_downloaded),
                        )
                    }
                    Download.STATE_DOWNLOADING, Download.STATE_QUEUED -> {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = Color.White,
                        )
                    }
                    else -> {
                        Icon(
                            painter = painterResource(R.drawable.arrow_circle_down),
                            contentDescription = stringResource(R.string.download_all_favorite_albums),
                        )
                    }
                }
            }
            IconButton(onClick = { isSearchActive = true }) {
                Icon(
                    painter = painterResource(R.drawable.search),
                    contentDescription = stringResource(R.string.search),
                )
            }
        }
    }
}
