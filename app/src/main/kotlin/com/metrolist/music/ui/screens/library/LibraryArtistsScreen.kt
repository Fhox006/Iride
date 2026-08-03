/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.screens.library

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.metrolist.music.constants.AlbumTopGradientKey
import com.metrolist.music.constants.PlayerBackgroundStyleKey
import com.metrolist.music.constants.PureBlackKey
import com.metrolist.music.ui.component.CollapsingScreenHeader
import com.metrolist.music.ui.component.NewReleaseBadge
import com.metrolist.music.ui.component.TopScreenGradientBackground
import com.metrolist.music.ui.component.frostedTopBarBackground
import com.metrolist.music.ui.component.recordFrostBackdrop
import com.metrolist.music.ui.component.rememberFrostBackdrop
import com.metrolist.music.ui.theme.SpaceMonoFontFamily
import com.metrolist.music.ui.utils.IrideMotion
import com.metrolist.music.ui.utils.irideEnter
import com.metrolist.music.ui.utils.rememberEnterProgress
import com.metrolist.music.ui.utils.revealMask
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
import androidx.compose.ui.platform.LocalHapticFeedback
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
import com.metrolist.music.constants.ArtistFilter
import com.metrolist.music.constants.ArtistFilterKey
import com.metrolist.music.constants.ArtistSortDescendingKey
import com.metrolist.music.constants.ArtistSortType
import com.metrolist.music.constants.ArtistSortTypeKey
import com.metrolist.music.constants.ArtistViewTypeKey
import com.metrolist.music.constants.CONTENT_TYPE_ARTIST
import com.metrolist.music.constants.CONTENT_TYPE_HEADER
import com.metrolist.music.constants.GridItemSize
import com.metrolist.music.constants.GridItemsSizeKey
import com.metrolist.music.constants.GridThumbnailHeight
import com.metrolist.music.constants.LibraryViewType
import com.metrolist.music.constants.YtmSyncKey
import com.metrolist.music.ui.component.ArtistNewReleaseRingItem
import com.metrolist.music.ui.component.ChipsRow
import com.metrolist.music.ui.component.LibraryArtistGridItem
import com.metrolist.music.ui.component.LibraryArtistListItem
import com.metrolist.music.ui.component.LibrarySuggestedFollowArtistItem
import com.metrolist.music.ui.component.NavigationTitle
import com.metrolist.music.ui.component.LocalItemHorizontalPadding
import com.metrolist.music.ui.component.LibrarySearchEmptyPlaceholder
import com.metrolist.music.ui.component.LibrarySearchHeader
import com.metrolist.music.ui.component.LibrarySortRow
import com.metrolist.music.ui.component.LocalMenuState
import com.metrolist.music.ui.component.SortHeader
import com.metrolist.music.ui.component.currentGridThumbnailHeight
import com.metrolist.music.utils.rememberEnumPreference
import com.metrolist.music.utils.rememberPreference
import com.metrolist.music.viewmodels.LibraryArtistsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun LibraryArtistsScreen(
    navController: NavController,
    isOffline: Boolean = false,
    viewModel: LibraryArtistsViewModel = hiltViewModel(),
) {
    val menuState = LocalMenuState.current
    val haptic = LocalHapticFeedback.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val coroutineScope = rememberCoroutineScope()
    val playerConnection = LocalPlayerConnection.current ?: return
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()
    var viewType by rememberEnumPreference(ArtistViewTypeKey, LibraryViewType.GRID)

    var filter by rememberEnumPreference(ArtistFilterKey, ArtistFilter.LIKED)
    val (sortType, onSortTypeChange) = rememberEnumPreference(
        ArtistSortTypeKey,
        ArtistSortType.CREATE_DATE
    )
    val (sortDescending, onSortDescendingChange) = rememberPreference(ArtistSortDescendingKey, true)
    val gridItemSize by rememberEnumPreference(GridItemsSizeKey, GridItemSize.BIG)
    val (ytmSync) = rememberPreference(YtmSyncKey, true)
    val pureBlack by rememberPreference(PureBlackKey, defaultValue = false)
    val (topNavigationBarEnabled) = rememberPreference(com.metrolist.music.constants.TopNavigationBarKey, defaultValue = true)
    val albumTopGradientEnabled by rememberPreference(AlbumTopGradientKey, defaultValue = true)
    val playerBackgroundStyle by rememberEnumPreference(
        PlayerBackgroundStyleKey,
        defaultValue = com.metrolist.music.constants.PlayerBackgroundStyle.BETTER_ANIMATED_GRADIENT,
    )

    val sortOptions = listOf(
        ArtistSortType.CREATE_DATE to stringResource(R.string.sort_by_create_date),
        ArtistSortType.NAME        to stringResource(R.string.sort_by_name),
        ArtistSortType.SONG_COUNT  to stringResource(R.string.sort_by_song_count),
        ArtistSortType.PLAY_TIME   to stringResource(R.string.sort_by_play_time),
    )

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(
        snapAnimationSpec = tween(durationMillis = 200),
    )

    LaunchedEffect(Unit) {
        if (ytmSync) {
            withContext(Dispatchers.IO) {
                viewModel.sync()
            }
        }
    }

    var isSearchActive by rememberSaveable { mutableStateOf(false) }
    val searchQuery by viewModel.searchQuery.collectAsState()
    val filteredArtistsRaw by viewModel.filteredArtists.collectAsState()
    val filteredArtists = if (isOffline) emptyList() else filteredArtistsRaw

    val newSongCounts by viewModel.newSongCounts.collectAsState()
    val totalNewSongs by viewModel.totalNewSongs.collectAsState()
    val newReleaseArtists by viewModel.newReleaseArtists.collectAsState()
    val showNewReleases = !isOffline && searchQuery.isBlank() && newReleaseArtists.isNotEmpty()
    val suggestedFollowArtists by viewModel.suggestedFollowArtists.collectAsState()
    val showSuggestedFollow = !isOffline && searchQuery.isBlank() && suggestedFollowArtists.isNotEmpty()

    // Lead section: "what happened since you last looked" — followed artists with an unseen
    // release, ringed like a story tray. This is the reason to open the screen, so it sits above
    // suggestions and the roster, not buried as a badge inside the grid.
    val newReleaseSection: @Composable () -> Unit = {
        androidx.compose.foundation.layout.Column {
            NavigationTitle(
                title = stringResource(R.string.new_from_followed_artists),
                useIrideStyle = topNavigationBarEnabled,
            )
            LazyRow(
                contentPadding = PaddingValues(horizontal = if (topNavigationBarEnabled) 16.dp else 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(
                    items = newReleaseArtists,
                    key = { "new_release_${it.id}" },
                ) { artist ->
                    ArtistNewReleaseRingItem(
                        artist = artist,
                        newSongCount = newSongCounts[artist.id] ?: 0,
                        modifier = Modifier.clickable { navController.navigate("artist/${artist.id}") },
                    )
                }
            }
        }
    }

    // "Artists you play a lot but forgot to follow" — one horizontal row. The "+" follows right
    // away (no detour through the artist page); a visible trash icon dismisses the suggestion.
    val suggestedFollowSection: @Composable () -> Unit = {
        androidx.compose.foundation.layout.Column {
            NavigationTitle(
                title = stringResource(R.string.suggested_follow_artists),
                useIrideStyle = topNavigationBarEnabled,
            )
            val suggestedFollowSize = currentGridThumbnailHeight()
            LazyRow(
                contentPadding = PaddingValues(horizontal = if (topNavigationBarEnabled) 16.dp else 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(
                    items = suggestedFollowArtists,
                    key = { "suggested_follow_${it.id}" },
                ) { artist ->
                    LibrarySuggestedFollowArtistItem(
                        navController = navController,
                        artist = artist,
                        size = suggestedFollowSize,
                        onFollow = { viewModel.followSuggestedArtist(artist.id) },
                        onDismiss = { viewModel.dismissSuggestedFollowArtist(artist.id) },
                    )
                }
            }
        }
    }

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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .onGloballyPositioned { titleBottomPx = it.boundsInWindow().bottom },
                ) {
                    Text(
                        text = stringResource(R.string.artists),
                        style = TextStyle(
                            fontFamily = SpaceMonoFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 40.sp,
                            letterSpacing = (-0.6).sp,
                        ),
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    if (totalNewSongs > 0) {
                        Spacer(modifier = Modifier.width(10.dp))
                        NewReleaseBadge(count = totalNewSongs)
                    }
                }
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
                    LibraryViewType.LIST ->
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

                            if (showNewReleases) {
                                item(key = "new_releases") { newReleaseSection() }
                            }

                            if (showSuggestedFollow) {
                                item(key = "suggested_follow") { suggestedFollowSection() }
                            }

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

                            filteredArtists.let { artists ->
                                if (artists.isEmpty()) {
                                    item(key = "empty_placeholder") {
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
                                    items = artists,
                                    key = { it.id },
                                    contentType = { CONTENT_TYPE_ARTIST },
                                ) { artist ->
                                    LibraryArtistListItem(
                                        navController = navController,
                                        menuState = menuState,
                                        coroutineScope = coroutineScope,
                                        newSongCount = newSongCounts[artist.id] ?: 0,
                                        modifier = Modifier.animateItem(),
                                        artist = artist
                                    )
                                }
                            }
                        }

                    LibraryViewType.GRID, LibraryViewType.GRID_WIDE ->
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

                            if (showNewReleases) {
                                item(
                                    key = "new_releases",
                                    span = { GridItemSpan(maxLineSpan) },
                                ) { newReleaseSection() }
                            }

                            if (showSuggestedFollow) {
                                item(
                                    key = "suggested_follow",
                                    span = { GridItemSpan(maxLineSpan) },
                                ) { suggestedFollowSection() }
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
                                    viewType = viewType,
                                    onViewTypeChange = { viewType = it },
                                    useIrideStyle = true,
                                )
                            }

                            filteredArtists.let { artists ->
                                if (artists.isEmpty()) {
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
                                    items = artists,
                                    key = { it.id },
                                    contentType = { CONTENT_TYPE_ARTIST },
                                ) { artist ->
                                    LibraryArtistGridItem(
                                        navController = navController,
                                        menuState = menuState,
                                        coroutineScope = coroutineScope,
                                        newSongCount = newSongCounts[artist.id] ?: 0,
                                        modifier = Modifier.animateItem(),
                                        artist = artist
                                    )
                                }
                            }
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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 4.dp)
                        .irideEnter(topBarRevealProgress, 6.dp)
                        .revealMask(topBarRevealProgress),
                ) {
                    Text(
                        text = stringResource(R.string.artists),
                        style = TextStyle(
                            fontFamily = SpaceMonoFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            letterSpacing = (-0.1).sp,
                        ),
                        color = MaterialTheme.colorScheme.onBackground,
                        maxLines = 1,
                    )
                    if (totalNewSongs > 0) {
                        Spacer(modifier = Modifier.width(6.dp))
                        NewReleaseBadge(count = totalNewSongs)
                    }
                }
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
                title = stringResource(R.string.artists),
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
                titleBadge = totalNewSongs.takeIf { it > 0 },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(
                            painter = painterResource(R.drawable.arrow_back),
                            contentDescription = null,
                        )
                    }
                },
            )
        },
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets(0),
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(if (pureBlack) Color.Black else MaterialTheme.colorScheme.background)
                .padding(paddingValues),
        ) {
            CompositionLocalProvider(LocalItemHorizontalPadding provides false) {
                when (viewType) {
                    LibraryViewType.LIST ->
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
                            if (showNewReleases) {
                                item(key = "new_releases") { newReleaseSection() }
                            }

                            if (showSuggestedFollow) {
                                item(key = "suggested_follow") { suggestedFollowSection() }
                            }

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

                            filteredArtists.let { artists ->
                                if (artists.isEmpty()) {
                                    item(key = "empty_placeholder") {
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
                                    items = artists,
                                    key = { it.id },
                                    contentType = { CONTENT_TYPE_ARTIST },
                                ) { artist ->
                                    LibraryArtistListItem(
                                        navController = navController,
                                        menuState = menuState,
                                        coroutineScope = coroutineScope,
                                        newSongCount = newSongCounts[artist.id] ?: 0,
                                        modifier = Modifier.animateItem(),
                                        artist = artist
                                    )
                                }
                            }
                        }

                    LibraryViewType.GRID, LibraryViewType.GRID_WIDE ->
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
                            if (showNewReleases) {
                                item(
                                    key = "new_releases",
                                    span = { GridItemSpan(maxLineSpan) },
                                ) { newReleaseSection() }
                            }

                            if (showSuggestedFollow) {
                                item(
                                    key = "suggested_follow",
                                    span = { GridItemSpan(maxLineSpan) },
                                ) { suggestedFollowSection() }
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
                                    viewType = viewType,
                                    onViewTypeChange = { viewType = it },
                                    useIrideStyle = topNavigationBarEnabled,
                                )
                            }

                            filteredArtists.let { artists ->
                                if (artists.isEmpty()) {
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
                                    items = artists,
                                    key = { it.id },
                                    contentType = { CONTENT_TYPE_ARTIST },
                                ) { artist ->
                                    LibraryArtistGridItem(
                                        navController = navController,
                                        menuState = menuState,
                                        coroutineScope = coroutineScope,
                                        newSongCount = newSongCounts[artist.id] ?: 0,
                                        modifier = Modifier.animateItem(),
                                        artist = artist
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