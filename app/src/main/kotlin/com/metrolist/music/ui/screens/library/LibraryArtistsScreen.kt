/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.screens.library

import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.gestures.Orientation
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
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.material3.LocalContentColor
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.metrolist.music.constants.PureBlackKey
import com.metrolist.music.ui.component.CollapsingScreenHeader
import com.metrolist.music.ui.component.LocalLibraryTextColor
import com.metrolist.music.ui.theme.SpaceMonoFontFamily
import com.metrolist.music.ui.utils.IrideMotion
import com.metrolist.music.ui.utils.irideEnter
import com.metrolist.music.ui.utils.rememberDiscreteProgress
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.metrolist.music.LocalPlayerAwareWindowInsets
import com.metrolist.music.LocalPlayerConnection
import com.metrolist.music.LocalDatabase
import com.metrolist.innertube.YouTube
import com.metrolist.innertube.models.AlbumItem
import com.metrolist.innertube.models.SongItem
import com.metrolist.music.R
import com.metrolist.music.constants.ArtistSortDescendingKey
import com.metrolist.music.constants.ArtistSortType
import com.metrolist.music.constants.ArtistSongSortType
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
import com.metrolist.music.ui.component.LibrarySuggestedFollowArtistItem
import com.metrolist.music.ui.component.LibrarySuggestedFollowArtistListItem
import com.metrolist.music.ui.component.LocalItemHorizontalPadding
import com.metrolist.music.ui.component.LibrarySearchEmptyPlaceholder
import com.metrolist.music.ui.component.LibrarySortRow
import com.metrolist.music.ui.component.LocalMenuState
import com.metrolist.music.ui.component.LibraryHeroTitle
import com.metrolist.music.ui.component.LibraryFooterCount
import com.metrolist.music.ui.component.LibraryPageTopBar
import com.metrolist.music.ui.component.IrideSearchBox
import com.metrolist.music.ui.component.IridePageLayout
import com.metrolist.music.ui.component.LibraryGridPage
import com.metrolist.music.ui.component.LibraryPlaybackActions
import com.metrolist.music.extensions.toMediaItem
import com.metrolist.music.models.toMediaMetadata
import com.metrolist.music.playback.queues.ListQueue
import com.metrolist.music.playback.queues.buildRotationQueue
import com.metrolist.music.ui.component.LibraryListPage
import com.metrolist.music.ui.component.rubberBandOverscroll
import com.metrolist.music.ui.component.rememberLibraryPageRevealState
import com.metrolist.music.ui.component.rememberLibraryTopBarProgress
import com.metrolist.music.ui.component.SortHeader
import com.metrolist.music.utils.rememberEnumPreference
import com.metrolist.music.utils.rememberPreference
import com.metrolist.music.viewmodels.LibraryArtistsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun LibraryArtistsScreen(
    navController: NavController,
    isOffline: Boolean = false,
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
    val (sortDescending, onSortDescendingChange) = rememberPreference(ArtistSortDescendingKey, true)
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
                    modifier = Modifier.rubberBandOverscroll(Orientation.Vertical, lazyGridState),
                    state = lazyGridState,
                    columns = if (viewType == LibraryViewType.GRID_WIDE) GridCells.Fixed(3) else GridCells.Fixed(2),
                    onNavigateUp = { navController.navigateUp() },
                    bottomPadding = LocalPlayerAwareWindowInsets.current
                        .asPaddingValues().calculateBottomPadding(),
                    playbackActions = if (filteredArtists.isEmpty()) null else {
                        {
                            LibraryPlaybackActions(
                                onShuffle = {
                                    coroutineScope.launch {
                                        val artist = filteredArtists.randomOrNull() ?: return@launch
                                        val queue = loadArtistMediaItems(database, artist).distinctBy { it.mediaId }.shuffled()
                                        if (queue.isNotEmpty()) playerConnection.playQueue(ListQueue(queueTitle, queue, 0))
                                    }
                                },
                                onRotation = {
                                    coroutineScope.launch {
                                        val cutoff = System.currentTimeMillis() - 30L * 24 * 60 * 60 * 1000
                                        val ids = filteredArtists.map { it.id }.toSet()
                                        val ranked = database.mostPlayedArtists(cutoff, limit = 20).first()
                                            .filter { it.id in ids }
                                            .take(5)
                                            .ifEmpty { filteredArtists.shuffled().take(5) }
                                        val tracks = ranked.flatMap { loadArtistMediaItems(database, it) }
                                            .distinctBy { it.mediaId }
                                        val queue = database.buildRotationQueue(tracks)
                                        if (queue.isNotEmpty()) playerConnection.playQueue(ListQueue(queueTitle, queue, 0))
                                    }
                                },
                            )
                        }
                    },
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    item(
                        key = "search_box",
                        span = { GridItemSpan(maxLineSpan) },
                        contentType = CONTENT_TYPE_HEADER,
                    ) {
                        IrideSearchBox(
                            query = searchFieldValue,
                            onQueryChange = { value ->
                                searchFieldValue = value
                                viewModel.updateSearchQuery(value.text)
                            },
                            placeholderText = stringResource(R.string.search_artists),
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

private suspend fun loadArtistMediaItems(
    database: com.metrolist.music.db.MusicDatabase,
    artist: com.metrolist.music.db.entities.Artist,
): List<androidx.media3.common.MediaItem> {
    val stored = database.artistSongs(artist.id, ArtistSongSortType.CREATE_DATE, true)
        .first()
        .map { it.toMediaItem() }
    val page = YouTube.artist(artist.artist.channelId ?: artist.id).getOrNull()
        ?: return stored
    val sections = page.sections.flatMap { it.items }
    val tracks = sections.filterIsInstance<SongItem>().map { it.toMediaMetadata().toMediaItem() }
    val releases = sections.filterIsInstance<AlbumItem>().distinctBy { it.browseId }.take(15)
    val releaseTracks = releases.flatMap { release ->
        YouTube.album(release.browseId).getOrNull()?.songs.orEmpty()
            .map { it.toMediaMetadata().toMediaItem() }
    }
    return (stored + tracks + releaseTracks).distinctBy { it.mediaId }
}
