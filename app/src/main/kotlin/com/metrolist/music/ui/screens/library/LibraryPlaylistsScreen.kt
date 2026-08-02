/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.screens.library

import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.metrolist.innertube.utils.parseCookieString
import com.metrolist.music.LocalPlayerAwareWindowInsets
import com.metrolist.music.LocalPlayerConnection
import com.metrolist.music.R
import com.metrolist.music.constants.AlbumTopGradientKey
import com.metrolist.music.constants.CONTENT_TYPE_HEADER
import com.metrolist.music.constants.CONTENT_TYPE_PLAYLIST
import com.metrolist.music.constants.GridItemSize
import com.metrolist.music.constants.GridItemsSizeKey
import com.metrolist.music.constants.GridThumbnailHeight
import com.metrolist.music.constants.InnerTubeCookieKey
import com.metrolist.music.constants.LibraryViewType
import com.metrolist.music.constants.PlayerBackgroundStyleKey
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
import com.metrolist.music.ui.component.LibrarySearchHeader
import com.metrolist.music.ui.component.LibrarySortRow
import com.metrolist.music.ui.component.LocalItemHorizontalPadding
import com.metrolist.music.ui.component.LocalMenuState
import com.metrolist.music.ui.component.PlaylistGridItem
import com.metrolist.music.ui.component.PlaylistListItem
import com.metrolist.music.ui.component.TopScreenGradientBackground
import com.metrolist.music.ui.component.frostedTopBarBackground
import com.metrolist.music.ui.component.recordFrostBackdrop
import com.metrolist.music.ui.component.rememberFrostBackdrop
import com.metrolist.music.ui.theme.SpaceMonoFontFamily
import com.metrolist.music.ui.utils.IrideMotion
import com.metrolist.music.ui.utils.irideEnter
import com.metrolist.music.ui.utils.rememberEnterProgress
import com.metrolist.music.ui.utils.revealMask
import com.metrolist.music.utils.rememberEnumPreference
import com.metrolist.music.utils.rememberPreference
import com.metrolist.music.viewmodels.LibraryPlaylistsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

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
    val (topNavigationBarEnabled) = rememberPreference(com.metrolist.music.constants.TopNavigationBarKey, defaultValue = true)
    val albumTopGradientEnabled by rememberPreference(AlbumTopGradientKey, defaultValue = true)
    val playerBackgroundStyle by rememberEnumPreference(
        PlayerBackgroundStyleKey,
        defaultValue = com.metrolist.music.constants.PlayerBackgroundStyle.BETTER_ANIMATED_GRADIENT,
    )

    val playlists by viewModel.allPlaylists.collectAsState()
    val downloadedPlaylistIds by viewModel.downloadedPlaylistIds.collectAsState()

    // New Iride UI only: "Liked Songs" reads as "Starred" here. R.string.liked is shared with the
    // legacy UI (and other screens), so it is left untouched and only this pinned entry's display
    // text is swapped.
    val likedName = if (topNavigationBarEnabled) stringResource(R.string.starred) else stringResource(R.string.liked)
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

    var isSearchActive by rememberSaveable { mutableStateOf(false) }
    val searchQuery by viewModel.searchQuery.collectAsState()
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
            when (viewType) {
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

    if (topNavigationBarEnabled) {
        // New Iride UI hero pattern — see LibraryAlbumsScreen.kt for the canonical version this
        // was copied from, including the crash note below.
        val density = LocalDensity.current
        val frostBackdrop = rememberFrostBackdrop()
        var titleBottomPx by remember { mutableStateOf(Float.MAX_VALUE) }
        var topBarBottomPx by remember { mutableStateOf(0f) }
        val titleCoverRangePx = with(density) { 24.dp.toPx() }
        val topBarRevealProgress by remember {
            derivedStateOf {
                val scrolledPastHeader = if (viewType == LibraryViewType.LIST) {
                    lazyListState.firstVisibleItemIndex > 0
                } else {
                    lazyGridState.firstVisibleItemIndex > 0
                }
                if (scrolledPastHeader) {
                    1f
                } else {
                    ((topBarBottomPx + titleCoverRangePx - titleBottomPx) / titleCoverRangePx).coerceIn(0f, 1f)
                }
            }
        }
        val screenProgress = rememberEnterProgress(play = true, durationMillis = IrideMotion.Short, easing = IrideMotion.EaseOutQuart)

        val heroHeader: @Composable () -> Unit = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .irideEnter(screenProgress, 10.dp),
            ) {
                Spacer(modifier = Modifier.height(28.dp))
                Text(
                    text = stringResource(R.string.playlists),
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

        // The frosted bar below must be a sibling of this Box, never a child: nesting the bar's
        // frostedTopBarBackground draw inside the still-recording recordFrostBackdrop Box re-enters
        // the same RenderNode mid-record and crashes.
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
                        ) {
                            item(key = "hero_header") { heroHeader() }

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

                            item(key = "liked_pinned", contentType = CONTENT_TYPE_PLAYLIST) {
                                PlaylistListItem(
                                    playlist = likedPlaylistPinned,
                                    autoPlaylist = true,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { navController.navigate("auto_playlist/liked") }
                                        .animateItem(),
                                )
                            }

                            if (visibleResults.isEmpty()) {
                                item(key = "empty_placeholder") {
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
                                    PlaylistListItem(
                                        playlist = item.playlist,
                                        autoPlaylist = true,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                item.route?.let(navController::navigate)
                                            }
                                            .animateItem(),
                                    )
                                } else {
                                    LibraryPlaylistListItem(
                                        navController = navController,
                                        menuState = menuState,
                                        coroutineScope = coroutineScope,
                                        playlist = item.playlist,
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

                        HideOnScrollFAB(
                            lazyListState = lazyListState,
                            icon = R.drawable.add,
                            label = if (betterLibraryBeta) stringResource(R.string.new_playlist) else null,
                            onClick = { showCreatePlaylistDialog = true },
                        )
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
                        ) {
                            item(key = "hero_header", span = { GridItemSpan(maxLineSpan) }) { heroHeader() }

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

                        HideOnScrollFAB(
                            lazyListState = lazyGridState,
                            icon = R.drawable.add,
                            label = if (betterLibraryBeta) stringResource(R.string.new_playlist) else null,
                            onClick = { showCreatePlaylistDialog = true },
                        )
                    }
                }
            }
        } // close inner recording Box

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
                    text = stringResource(R.string.playlists),
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
                IconButton(onClick = { isSearchActive = true }) {
                    Icon(
                        painter = painterResource(R.drawable.search),
                        contentDescription = stringResource(R.string.search),
                    )
                }
            }
        } // close outer plain Box
    } else {
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            CollapsingScreenHeader(
                title = stringResource(R.string.playlists),
                scrollBehavior = scrollBehavior,
                pureBlack = pureBlack,
                isSearchActive = isSearchActive,
                onSearchActiveChange = { active ->
                    isSearchActive = active
                    if (!active) viewModel.updateSearchQuery("")
                },
                searchQuery = searchQuery,
                onSearchQueryChange = viewModel::updateSearchQuery,
                keyboardController = keyboardController,
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(
                            painter = painterResource(R.drawable.arrow_back),
                            contentDescription = if (betterLibraryBeta)
                                stringResource(R.string.navigate_back)
                            else null,
                        )
                    }
                },
            )
        },
        containerColor = if (betterLibraryBeta) {
            if (pureBlack) Color.Black else MaterialTheme.colorScheme.background
        } else {
            Color.Transparent
        },
        contentWindowInsets = WindowInsets(0),
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (!betterLibraryBeta) {
                        Modifier.background(if (pureBlack) Color.Black else MaterialTheme.colorScheme.background)
                    } else {
                        Modifier
                    }
                )
                .padding(paddingValues),
        ) {
            CompositionLocalProvider(LocalItemHorizontalPadding provides false) {
                when (viewType) {
                    LibraryViewType.LIST -> {
                        LazyColumn(
                            state = lazyListState,
                            contentPadding = PaddingValues(
                                start = 12.dp,
                                end = 12.dp,
                                top = 0.dp,
                                bottom = LocalPlayerAwareWindowInsets.current
                                    .asPaddingValues().calculateBottomPadding(),
                            ),
                        ) {
                            item(key = "sort", contentType = CONTENT_TYPE_HEADER) {
                                LibrarySortRow(
                                    sortOptions = sortOptions,
                                    currentSort = sortType,
                                    onSortChange = onSortTypeChange,
                                    sortDescending = sortDescending,
                                    onSortDescendingChange = onSortDescendingChange,
                                    viewType = viewType,
                                    onViewTypeChange = { viewType = it },
                                    useIrideStyle = topNavigationBarEnabled,
                                )
                            }

                            item(key = "liked_pinned", contentType = CONTENT_TYPE_PLAYLIST) {
                                PlaylistListItem(
                                    playlist = likedPlaylistPinned,
                                    autoPlaylist = true,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { navController.navigate("auto_playlist/liked") }
                                        .animateItem(),
                                )
                            }

                            if (visibleResults.isEmpty()) {
                                item(key = "empty_placeholder") {
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
                                    PlaylistListItem(
                                        playlist = item.playlist,
                                        autoPlaylist = true,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                item.route?.let(navController::navigate)
                                            }
                                            .animateItem(),
                                    )
                                } else {
                                    LibraryPlaylistListItem(
                                        navController = navController,
                                        menuState = menuState,
                                        coroutineScope = coroutineScope,
                                        playlist = item.playlist,
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

                        HideOnScrollFAB(
                            lazyListState = lazyListState,
                            icon = R.drawable.add,
                            label = if (betterLibraryBeta) stringResource(R.string.new_playlist) else null,
                            onClick = { showCreatePlaylistDialog = true },
                        )
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
                                start = 12.dp,
                                end = 12.dp,
                                top = 0.dp,
                                bottom = LocalPlayerAwareWindowInsets.current
                                    .asPaddingValues().calculateBottomPadding(),
                            ),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
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
                                    useIrideStyle = topNavigationBarEnabled,
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

                        HideOnScrollFAB(
                            lazyListState = lazyGridState,
                            icon = R.drawable.add,
                            label = if (betterLibraryBeta) stringResource(R.string.new_playlist) else null,
                            onClick = { showCreatePlaylistDialog = true },
                        )
                    }
                }
            }
        }
    }
    }
}
