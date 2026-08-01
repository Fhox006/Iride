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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
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
import com.metrolist.music.constants.HideExplicitKey
import com.metrolist.music.constants.LibraryViewType
import com.metrolist.music.constants.PureBlackKey
import com.metrolist.music.constants.YtmSyncKey
import com.metrolist.music.extensions.matchesNormalizedQuery
import com.metrolist.music.extensions.normalizeForSearch
import com.metrolist.music.ui.component.CollapsingScreenHeader
import com.metrolist.music.ui.component.EmptyPlaceholder
import com.metrolist.music.ui.component.IrideCollapsibleSection
import com.metrolist.music.ui.component.LibraryAlbumGridItem
import com.metrolist.music.ui.component.LibraryAlbumListItem
import com.metrolist.music.ui.component.LibraryContinueListeningAlbumItem
import com.metrolist.music.ui.component.LibrarySearchEmptyPlaceholder
import com.metrolist.music.ui.component.LibrarySortRow
import com.metrolist.music.ui.component.LocalItemHorizontalPadding
import com.metrolist.music.ui.component.LocalMenuState
import com.metrolist.music.ui.component.NavigationTitle
import com.metrolist.music.ui.component.currentGridThumbnailHeight
import com.metrolist.music.ui.component.rubberBandOverscroll
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
    val (topNavigationBarEnabled) = rememberPreference(com.metrolist.music.constants.TopNavigationBarKey, defaultValue = true)
    val currentGridHeight = currentGridThumbnailHeight()

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

    // Per-section collapse state (session-only, not persisted) — same convention as HomeScreen.
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

    val continueListeningSpacing = 8.dp

    val continueListeningTitle: @Composable () -> Unit = {
        NavigationTitle(
            title = stringResource(R.string.continue_listening),
            useIrideStyle = topNavigationBarEnabled,
            collapsed = isSectionCollapsed("continue_listening"),
            onCollapseToggle = if (topNavigationBarEnabled) { { toggleSection("continue_listening") } } else null,
        )
    }
    val continueListeningRow: @Composable () -> Unit = {
        val content: @Composable () -> Unit = {
            val continueListeningState = rememberLazyListState()
            LazyRow(
                state = continueListeningState,
                contentPadding = PaddingValues(horizontal = if (topNavigationBarEnabled) 16.dp else 12.dp),
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
        if (topNavigationBarEnabled) {
            IrideCollapsibleSection(collapsed = isSectionCollapsed("continue_listening")) { content() }
        } else {
            content()
        }
    }

    // New Iride UI: no "Favorite Albums" label — it sat as a redundant divider directly under
    // Continue Listening, the two sections now just flow into one grid. Classic UI keeps it.
    val favoritesTitle: @Composable () -> Unit = {
        if (!topNavigationBarEnabled) {
            NavigationTitle(
                title = stringResource(R.string.favorite_albums),
                useIrideStyle = false,
                collapsed = isSectionCollapsed("favorite_albums"),
                onCollapseToggle = null,
            )
        } else {
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
    val favoritesCollapsed = isSectionCollapsed("favorite_albums") && topNavigationBarEnabled

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            CollapsingScreenHeader(
                title = stringResource(R.string.albums),
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
                            overscrollEffect = null,
                            modifier = Modifier.rubberBandOverscroll(Orientation.Vertical, lazyListState),
                        ) {
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
                                        useIrideStyle = topNavigationBarEnabled,
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
                                start = 12.dp,
                                end = 12.dp,
                                top = 0.dp,
                                bottom = LocalPlayerAwareWindowInsets.current
                                    .asPaddingValues().calculateBottomPadding(),
                            ),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            overscrollEffect = null,
                            modifier = Modifier.rubberBandOverscroll(Orientation.Vertical, lazyGridState),
                        ) {
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
                                        useIrideStyle = topNavigationBarEnabled,
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
    }
}
