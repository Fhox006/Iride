/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.screens.library

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import com.metrolist.music.constants.PureBlackKey
import com.metrolist.music.ui.component.CollapsingScreenHeader
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
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
import com.metrolist.music.ui.component.ChipsRow
import com.metrolist.music.ui.component.LibraryArtistGridItem
import com.metrolist.music.ui.component.LibraryArtistListItem
import com.metrolist.music.ui.component.LocalItemHorizontalPadding
import com.metrolist.music.ui.component.LibrarySearchEmptyPlaceholder
import com.metrolist.music.ui.component.LibrarySearchHeader
import com.metrolist.music.ui.component.LocalMenuState
import com.metrolist.music.ui.component.SortHeader
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
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(
                            painter = painterResource(R.drawable.arrow_back),
                            contentDescription = null,
                        )
                    }
                },
                trailingContent = {
                    IconButton(
                        onClick = { isSearchActive = true },
                        modifier = Modifier.size(40.dp),
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.search),
                            contentDescription = stringResource(R.string.search),
                        )
                    }
                    IconButton(
                        onClick = {
                            viewType = if (viewType == LibraryViewType.LIST)
                                LibraryViewType.GRID
                            else
                                LibraryViewType.LIST
                        },
                        modifier = Modifier.size(40.dp),
                    ) {
                        Icon(
                            painter = painterResource(
                                when (viewType) {
                                    LibraryViewType.LIST -> R.drawable.list
                                    else -> R.drawable.grid_view
                                }
                            ),
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
                            item(key = "filter", contentType = CONTENT_TYPE_HEADER) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(0.dp)
                                        .clipToBounds(),
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp),
                                    ) {
                                        ChipsRow(
                                            chips = listOf(
                                                ArtistFilter.LIKED to stringResource(R.string.filter_liked),
                                                ArtistFilter.LIBRARY to stringResource(R.string.filter_library),
                                            ),
                                            currentValue = filter,
                                            onValueUpdate = { filter = it },
                                            modifier = Modifier.weight(1f),
                                        )
                                    }
                                }
                            }

                            item(key = "sort", contentType = CONTENT_TYPE_HEADER) {
                                SortHeader(
                                    sortType = sortType,
                                    sortDescending = sortDescending,
                                    onSortTypeChange = onSortTypeChange,
                                    onSortDescendingChange = onSortDescendingChange,
                                    sortTypeText = { type ->
                                        when (type) {
                                            ArtistSortType.CREATE_DATE -> R.string.sort_by_create_date
                                            ArtistSortType.NAME -> R.string.sort_by_name
                                            ArtistSortType.SONG_COUNT -> R.string.sort_by_song_count
                                            ArtistSortType.PLAY_TIME -> R.string.sort_by_play_time
                                        }
                                    },
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
                                        modifier = Modifier.animateItem(),
                                        artist = artist
                                    )
                                }
                            }
                        }

                    LibraryViewType.GRID, LibraryViewType.GRID_WIDE ->
                        LazyVerticalGrid(
                            state = lazyGridState,
                            columns = GridCells.Adaptive(
                                minSize = GridThumbnailHeight +
                                    if (gridItemSize == GridItemSize.BIG) 24.dp else (-24).dp,
                            ),
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
                                key = "filter",
                                span = { GridItemSpan(maxLineSpan) },
                                contentType = CONTENT_TYPE_HEADER,
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(0.dp)
                                        .clipToBounds(),
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp),
                                    ) {
                                        ChipsRow(
                                            chips = listOf(
                                                ArtistFilter.LIKED to stringResource(R.string.filter_liked),
                                                ArtistFilter.LIBRARY to stringResource(R.string.filter_library),
                                            ),
                                            currentValue = filter,
                                            onValueUpdate = { filter = it },
                                            modifier = Modifier.weight(1f),
                                        )
                                    }
                                }
                            }

                            item(
                                key = "sort",
                                span = { GridItemSpan(maxLineSpan) },
                                contentType = CONTENT_TYPE_HEADER,
                            ) {
                                SortHeader(
                                    sortType = sortType,
                                    sortDescending = sortDescending,
                                    onSortTypeChange = onSortTypeChange,
                                    onSortDescendingChange = onSortDescendingChange,
                                    sortTypeText = { type ->
                                        when (type) {
                                            ArtistSortType.CREATE_DATE -> R.string.sort_by_create_date
                                            ArtistSortType.NAME -> R.string.sort_by_name
                                            ArtistSortType.SONG_COUNT -> R.string.sort_by_song_count
                                            ArtistSortType.PLAY_TIME -> R.string.sort_by_play_time
                                        }
                                    },
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