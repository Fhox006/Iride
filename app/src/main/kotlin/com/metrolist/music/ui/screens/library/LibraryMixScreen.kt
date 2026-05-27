/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.screens.library

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.metrolist.music.LocalPlayerAwareWindowInsets
import com.metrolist.music.LocalPlayerConnection
import com.metrolist.music.R
import com.metrolist.music.constants.CONTENT_TYPE_HEADER
import com.metrolist.music.constants.CONTENT_TYPE_PLAYLIST
import com.metrolist.music.constants.GridItemSize
import com.metrolist.music.constants.GridItemsSizeKey
import com.metrolist.music.constants.GridThumbnailHeight
import com.metrolist.music.constants.LibraryView
import com.metrolist.music.constants.LibraryViewType
import com.metrolist.music.constants.MixSortDescendingKey
import com.metrolist.music.constants.MixSortType
import com.metrolist.music.constants.MixSortTypeKey
import com.metrolist.music.constants.LibraryOfflineModeKey
import com.metrolist.music.constants.MixViewTypeKey
import com.metrolist.music.constants.PureBlackKey
import com.metrolist.music.constants.ShowCachedPlaylistKey
import com.metrolist.music.constants.ShowDownloadedPlaylistKey
import com.metrolist.music.constants.ShowUploadedPlaylistKey
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
import com.metrolist.music.playback.queues.ListQueue
import com.metrolist.music.ui.component.AlbumGridItem
import com.metrolist.music.ui.component.ArtistGridItem
import com.metrolist.music.ui.component.LibraryAlbumListItem
import com.metrolist.music.ui.component.LibraryPlaylistListItem
import com.metrolist.music.ui.component.LibrarySearchEmptyPlaceholder
import com.metrolist.music.ui.component.CollapsingScreenHeader
import com.metrolist.music.ui.component.LocalItemHorizontalPadding
import com.metrolist.music.ui.component.LocalMenuState
import com.metrolist.music.ui.component.PlaylistGridItem
import com.metrolist.music.ui.component.PlaylistListItem
import com.metrolist.music.ui.component.SongGridItem
import com.metrolist.music.ui.component.SongListItem
import com.metrolist.music.ui.component.SortHeader
import com.metrolist.music.ui.menu.AlbumMenu
import com.metrolist.music.ui.menu.ArtistMenu
import com.metrolist.music.ui.menu.PlaylistMenu
import com.metrolist.music.ui.menu.SongMenu
import com.metrolist.music.utils.rememberEnumPreference
import com.metrolist.music.utils.rememberPreference
import com.metrolist.music.viewmodels.LibraryMixViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.Collator
import java.time.LocalDateTime
import java.util.UUID

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun LibraryMixScreen(
    navController: NavController,
    currentView: LibraryView = LibraryView.LIBRARY,
    onViewChange: (LibraryView) -> Unit = {},
    viewModel: LibraryMixViewModel = hiltViewModel(),
) {
    val menuState = LocalMenuState.current
    val haptic = LocalHapticFeedback.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val queueSearchedSongsStr = stringResource(R.string.queue_searched_songs)
    val playerConnection = LocalPlayerConnection.current ?: return
    val isPlaying by playerConnection.isEffectivelyPlaying.collectAsState()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()

    val pureBlack by rememberPreference(PureBlackKey, defaultValue = false)
    var viewType by rememberEnumPreference(MixViewTypeKey, LibraryViewType.GRID)
    val (sortType, onSortTypeChange) =
        rememberEnumPreference(
            MixSortTypeKey,
            MixSortType.CREATE_DATE,
        )
    val (sortDescending, onSortDescendingChange) = rememberPreference(MixSortDescendingKey, true)
    val gridItemSize by rememberEnumPreference(GridItemsSizeKey, GridItemSize.BIG)

    val (ytmSync) = rememberPreference(YtmSyncKey, true)

    val (isLibraryFilter, setLibraryFilter) = rememberPreference(LibraryOfflineModeKey, defaultValue = true)
    var isSearchActive by rememberSaveable { mutableStateOf(false) }
    val searchQuery by viewModel.searchQuery.collectAsState()
    val debouncedSearchQuery by viewModel.debouncedSearchQuery.collectAsState()
    val normalizedQuery = remember(isSearchActive, searchQuery, debouncedSearchQuery) {
        if (isSearchActive) {
            searchQuery.normalizeForSearch()
        } else {
            debouncedSearchQuery.normalizeForSearch()
        }
    }

    val topSize by viewModel.topValue.collectAsState(initial = 50)
    val lastLikedDate by viewModel.lastLikedDate.collectAsState()
    val likedPlaylistName = stringResource(R.string.liked)
    val likedPlaylist = remember(lastLikedDate, likedPlaylistName) {
        Playlist(
            playlist = PlaylistEntity(
                id = PlaylistEntity.LIKED_PLAYLIST_ID,
                name = likedPlaylistName,
                createdAt = lastLikedDate,
                lastUpdateTime = lastLikedDate,
            ),
            songCount = 0,
            songThumbnails = emptyList(),
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
                    name = stringResource(R.string.cached_playlist),
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

    val (showDownloaded) = rememberPreference(ShowDownloadedPlaylistKey, false)
    val (showCached) = rememberPreference(ShowCachedPlaylistKey, false)
    val (showUploaded) = rememberPreference(ShowUploadedPlaylistKey, false)

    val showDownloadedPlaylist =
        showDownloaded && matchesNormalizedQuery(normalizedQuery, downloadPlaylist.playlist.name)
    val showTopPlaylists = false
    val showUploadedPlaylists =
        showUploaded && matchesNormalizedQuery(normalizedQuery, uploadedPlaylist.playlist.name)
    val showCachedPlaylists = showCached && matchesNormalizedQuery(normalizedQuery, cachedPlaylist.playlist.name)


    val albums = viewModel.albums.collectAsState()
    val artist = viewModel.artists.collectAsState()
    val songs = viewModel.songs.collectAsState()
    val playlist = viewModel.playlists.collectAsState()
    val uploadedSongs by viewModel.uploadedSongs.collectAsState()
    val downloadedAlbums by viewModel.downloadedAlbums.collectAsState()
    val locale = LocalLocale.current.platformLocale
    val collator = remember(locale) {
        Collator.getInstance(locale).apply {
            strength = Collator.PRIMARY
        }
    }
    var allItems = if (!isLibraryFilter) {
        downloadedAlbums
    } else {
        val likedEntry = if (lastLikedDate != null) listOf(likedPlaylist) else emptyList()
        val base = albums.value + artist.value + playlist.value + likedEntry
        when (sortType) {
            MixSortType.CREATE_DATE -> {
                base.sortedBy { item ->
                    when (item) {
                        is Album -> item.album.bookmarkedAt
                        is Artist -> item.artist.bookmarkedAt
                        is Playlist -> item.playlist.createdAt
                        else -> LocalDateTime.now()
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
                            else -> ""
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
                        else -> LocalDateTime.now()
                    }
                }
            }
        }.reversed(sortDescending)
    }

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
                    else -> true
                }
            }

        if (normalizedQuery.isBlank()) {
            matchedItems.distinctBy { it.id }
        } else {
            matchedItems
                .sortedWith { first, second ->
                    val firstPriority =
                        when (first) {
                            is Playlist -> 0
                            is Song -> 1
                            is Artist -> 2
                            is Album -> 3
                            else -> 4
                        }
                    val secondPriority =
                        when (second) {
                            is Playlist -> 0
                            is Song -> 1
                            is Artist -> 2
                            is Album -> 3
                            else -> 4
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
                                else -> ""
                            }
                        val secondName =
                            when (second) {
                                is Playlist -> second.playlist.name
                                is Song -> second.song.title
                                is Artist -> second.artist.name
                                is Album -> second.album.title
                                else -> ""
                            }
                        collator.compare(firstName, secondName)
                    }
                }
                .distinctBy { it.id }
        }
    }

    val coroutineScope = rememberCoroutineScope()

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

    LaunchedEffect(Unit) {
        // Always reset to Library mode on fresh launch (do not persist Downloads selection)
        setLibraryFilter(true)
        if (ytmSync) {
            withContext(Dispatchers.IO) {
                viewModel.syncAllLibrary()
            }
        }
    }

    val contentAlpha = remember { Animatable(1f) }
    var isFirstComposition by remember { mutableStateOf(true) }
    var displayedFilter by remember { mutableStateOf(isLibraryFilter) }
    LaunchedEffect(isLibraryFilter) {
        if (isFirstComposition) {
            isFirstComposition = false
            displayedFilter = isLibraryFilter
            return@LaunchedEffect
        }
        contentAlpha.animateTo(0f, animationSpec = tween(100))
        displayedFilter = isLibraryFilter
        contentAlpha.animateTo(1f, animationSpec = tween(150))
    }
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(
        snapAnimationSpec = tween(durationMillis = 200),
    )
    val fraction = scrollBehavior.state.collapsedFraction
    val onFilterToggle = { setLibraryFilter(!isLibraryFilter) }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            CollapsingScreenHeader(
                title = if (isLibraryFilter)
                    stringResource(R.string.filter_library)
                else
                    "Offline Library",
                scrollBehavior = scrollBehavior,
                pureBlack = pureBlack,
                isSearchActive = isSearchActive,
                onSearchActiveChange = { isSearchActive = it },
                searchQuery = searchQuery,
                onSearchQueryChange = viewModel::updateSearchQuery,
                keyboardController = keyboardController,
                trailingContent = {
                    val btnSize = 40.dp
                    val iconSize = 20.dp
                    val indicatorSize = 36.dp
                    val indicatorOffset by animateDpAsState(
                        targetValue = if (isLibraryFilter) 2.dp else 42.dp,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMedium,
                        ),
                        label = "libraryFilterIndicator",
                    )
                    Box(
                        modifier = Modifier
                            .width(btnSize * 2)
                            .height(btnSize)
                            .clip(RoundedCornerShape(btnSize / 2))
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                    ) {
                        Box(
                            modifier = Modifier
                                .offset(x = indicatorOffset, y = 2.dp)
                                .size(indicatorSize)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.secondaryContainer),
                        )
                        Row(modifier = Modifier.fillMaxSize()) {
                            Box(
                                modifier = Modifier
                                    .size(btnSize)
                                    .clickable(
                                        enabled = fraction < 0.05f,
                                        indication = null,
                                        interactionSource = remember { MutableInteractionSource() },
                                    ) { if (!isLibraryFilter) onFilterToggle() },
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.bookmark_outlined),
                                    contentDescription = null,
                                    tint = if (isLibraryFilter)
                                        MaterialTheme.colorScheme.onSecondaryContainer
                                    else
                                        MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(iconSize),
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .size(btnSize)
                                    .clickable(
                                        enabled = fraction < 0.05f,
                                        indication = null,
                                        interactionSource = remember { MutableInteractionSource() },
                                    ) { if (isLibraryFilter) onFilterToggle() },
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.download),
                                    contentDescription = null,
                                    tint = if (!isLibraryFilter)
                                        MaterialTheme.colorScheme.onSecondaryContainer
                                    else
                                        MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(iconSize),
                                )
                            }
                        }
                    }
                },
            )
        },
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets(0),
    ) { paddingValues ->
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(if (pureBlack) Color.Black else MaterialTheme.colorScheme.background)
                    .padding(paddingValues)
                    .alpha(contentAlpha.value),
        ) {
            CompositionLocalProvider(LocalItemHorizontalPadding provides false) {
                key(viewType) {
                    when (viewType) {
                        LibraryViewType.LIST -> {
                            LazyColumn(
                                state = lazyListState,
                                contentPadding = PaddingValues(
                                    start = 12.dp,
                                    end = 12.dp,
                                    top = 0.dp,
                                    bottom = LocalPlayerAwareWindowInsets.current.asPaddingValues().calculateBottomPadding(),
                                ),
                            ) {
                                item(key = "categories") {
                                    CategoriesContent(
                                        navController = navController,
                                        showUploads = uploadedSongs.isNotEmpty(),
                                        showCache = showCached,
                                        isOffline = !displayedFilter,
                                    )
                                }

                                if (normalizedQuery.isBlank()) {
                                    item(key = "recently_added_label", contentType = CONTENT_TYPE_HEADER) {
                                        Text(
                                            text = if (displayedFilter) "Recently Added" else "Recently Downloaded",
                                            style = MaterialTheme.typography.headlineMedium,
                                            modifier = Modifier.padding(vertical = 12.dp),
                                        )
                                    }
                                    item(key = "sort_header", contentType = CONTENT_TYPE_HEADER) {
                                        SortHeader(
                                            sortType = sortType,
                                            sortDescending = sortDescending,
                                            onSortTypeChange = onSortTypeChange,
                                            onSortDescendingChange = onSortDescendingChange,
                                            sortTypeText = { sortType ->
                                                when (sortType) {
                                                    MixSortType.CREATE_DATE -> R.string.sort_by_create_date
                                                    MixSortType.LAST_UPDATED -> R.string.sort_by_last_updated
                                                    MixSortType.NAME -> R.string.sort_by_name
                                                }
                                            },
                                            trailingContent = {
                                                IconButton(
                                                    onClick = {
                                                        viewType = if (viewType == LibraryViewType.LIST) LibraryViewType.GRID else LibraryViewType.LIST
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
                                                        contentDescription = stringResource(
                                                            when (viewType) {
                                                                LibraryViewType.LIST -> R.string.switch_to_grid_view
                                                                else -> R.string.switch_to_list_view
                                                            }
                                                        ),
                                                    )
                                                }
                                            },
                                        )
                                    }
                                }

                                if (showDownloadedPlaylist) {
                                    item(key = "downloadedPlaylist", contentType = { CONTENT_TYPE_PLAYLIST }) {
                                        PlaylistListItem(
                                            playlist = downloadPlaylist,
                                            autoPlaylist = true,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable { navController.navigate("auto_playlist/downloaded") }
                                                .animateItem(),
                                        )
                                    }
                                }

                                if (showCachedPlaylists) {
                                    item(key = "cachedPlaylist", contentType = { CONTENT_TYPE_PLAYLIST }) {
                                        PlaylistListItem(
                                            playlist = cachedPlaylist,
                                            autoPlaylist = true,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable { navController.navigate("cache_playlist/cached") }
                                                .animateItem(),
                                        )
                                    }
                                }

                                if (showTopPlaylists) {
                                    item(key = "TopPlaylist", contentType = { CONTENT_TYPE_PLAYLIST }) {
                                        PlaylistListItem(
                                            playlist = topPlaylist,
                                            autoPlaylist = true,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable { navController.navigate("top_playlist/$topSize") }
                                                .animateItem(),
                                        )
                                    }
                                }

                                if (showUploadedPlaylists) {
                                    item(key = "uploadedPlaylist", contentType = { CONTENT_TYPE_PLAYLIST }) {
                                        PlaylistListItem(
                                            playlist = uploadedPlaylist,
                                            autoPlaylist = true,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable { navController.navigate("auto_playlist/uploaded") }
                                                .animateItem(),
                                        )
                                    }
                                }

                                items(
                                    items = filteredItems,
                                    key = { it.id },
                                    contentType = { CONTENT_TYPE_PLAYLIST },
                                ) { item ->
                                    when (item) {
                                        is Playlist -> {
                                            if (item.id == PlaylistEntity.LIKED_PLAYLIST_ID) {
                                                PlaylistListItem(
                                                    playlist = item,
                                                    autoPlaylist = true,
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .clickable { navController.navigate("auto_playlist/liked") }
                                                        .animateItem(),
                                                )
                                            } else {
                                                LibraryPlaylistListItem(
                                                    navController = navController,
                                                    menuState = menuState,
                                                    coroutineScope = coroutineScope,
                                                    playlist = item,
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .animateItem(),
                                                )
                                            }
                                        }

                                        is Song -> {
                                            SongListItem(
                                                song = item,
                                                isActive = item.id == mediaMetadata?.id,
                                                isPlaying = isPlaying,
                                                showLikedIcon = false,
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .combinedClickable(
                                                        onClick = {
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
                                                        },
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
                                                    )
                                                    .animateItem(),
                                            )
                                        }

                                        is Album -> {
                                            LibraryAlbumListItem(
                                                navController = navController,
                                                menuState = menuState,
                                                album = item,
                                                isActive = item.id == mediaMetadata?.album?.id,
                                                isPlaying = isPlaying,
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .animateItem(),
                                            )
                                        }

                                        else -> {}
                                    }
                                }
                            }
                        }

                        LibraryViewType.GRID, LibraryViewType.GRID_WIDE -> {
                            LazyVerticalGrid(
                                state = lazyGridState,
                                columns =
                                    GridCells.Adaptive(
                                        minSize = GridThumbnailHeight + if (gridItemSize == GridItemSize.BIG) 24.dp else (-24).dp,
                                    ),
                                contentPadding = PaddingValues(
                                    start = 12.dp,
                                    end = 12.dp,
                                    top = 0.dp,
                                    bottom = LocalPlayerAwareWindowInsets.current.asPaddingValues().calculateBottomPadding(),
                                ),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                item(
                                    key = "categories",
                                    span = { GridItemSpan(maxLineSpan) },
                                ) {
                                    CategoriesContent(
                                        navController = navController,
                                        showUploads = uploadedSongs.isNotEmpty(),
                                        showCache = showCached,
                                        isOffline = !displayedFilter,
                                    )
                                }

                                if (normalizedQuery.isBlank()) {
                                    item(
                                        key = "recently_added_label",
                                        span = { GridItemSpan(maxLineSpan) },
                                        contentType = CONTENT_TYPE_HEADER,
                                    ) {
                                        Text(
                                            text = if (displayedFilter) "Recently Added" else "Recently Downloaded",
                                            style = MaterialTheme.typography.headlineMedium,
                                            modifier = Modifier.padding(vertical = 12.dp),
                                        )
                                    }
                                    item(
                                        key = "sort_header",
                                        span = { GridItemSpan(maxLineSpan) },
                                        contentType = CONTENT_TYPE_HEADER,
                                    ) {
                                        SortHeader(
                                            sortType = sortType,
                                            sortDescending = sortDescending,
                                            onSortTypeChange = onSortTypeChange,
                                            onSortDescendingChange = onSortDescendingChange,
                                            sortTypeText = { sortType ->
                                                when (sortType) {
                                                    MixSortType.CREATE_DATE -> R.string.sort_by_create_date
                                                    MixSortType.LAST_UPDATED -> R.string.sort_by_last_updated
                                                    MixSortType.NAME -> R.string.sort_by_name
                                                }
                                            },
                                            trailingContent = {
                                                IconButton(
                                                    onClick = {
                                                        viewType = if (viewType == LibraryViewType.LIST) LibraryViewType.GRID else LibraryViewType.LIST
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
                                                        contentDescription = stringResource(
                                                            when (viewType) {
                                                                LibraryViewType.LIST -> R.string.switch_to_grid_view
                                                                else -> R.string.switch_to_list_view
                                                            }
                                                        ),
                                                    )
                                                }
                                            },
                                        )
                                    }
                                }

                                if (showDownloadedPlaylist) {
                                    item(
                                        key = "downloadedPlaylist",
                                        contentType = { CONTENT_TYPE_PLAYLIST },
                                    ) {
                                        PlaylistGridItem(
                                            playlist = downloadPlaylist,
                                            fillMaxWidth = true,
                                            autoPlaylist = true,
                                            modifier =
                                                Modifier
                                                    .fillMaxWidth()
                                                    .combinedClickable(
                                                        onClick = {
                                                            navController.navigate("auto_playlist/downloaded")
                                                        },
                                                    )
                                                    .animateItem(),
                                        )
                                    }
                                }

                                if (showCachedPlaylists) {
                                    item(
                                        key = "cachedPlaylist",
                                        contentType = { CONTENT_TYPE_PLAYLIST },
                                    ) {
                                        PlaylistGridItem(
                                            playlist = cachedPlaylist,
                                            fillMaxWidth = true,
                                            autoPlaylist = true,
                                            modifier =
                                            Modifier
                                                .fillMaxWidth()
                                                .combinedClickable(
                                                    onClick = {
                                                        navController.navigate("cache_playlist/cached")
                                                    },
                                                )
                                                .animateItem(),
                                        )
                                    }
                                }

                                if (showTopPlaylists) {
                                    item(
                                        key = "TopPlaylist",
                                        contentType = { CONTENT_TYPE_PLAYLIST },
                                    ) {
                                        PlaylistGridItem(
                                            playlist = topPlaylist,
                                            fillMaxWidth = true,
                                            autoPlaylist = true,
                                            modifier =
                                                Modifier
                                                    .fillMaxWidth()
                                                    .combinedClickable(
                                                        onClick = {
                                                            navController.navigate("top_playlist/$topSize")
                                                        },
                                                    ).animateItem(),
                                        )
                                    }
                                }

                                if (showUploadedPlaylists) {
                                    item(
                                        key = "uploadedPlaylist",
                                        contentType = { CONTENT_TYPE_PLAYLIST },
                                    ) {
                                        PlaylistGridItem(
                                            playlist = uploadedPlaylist,
                                            fillMaxWidth = true,
                                            autoPlaylist = true,
                                            modifier =
                                                Modifier
                                                    .fillMaxWidth()
                                                    .clickable {
                                                        navController.navigate("auto_playlist/uploaded")
                                                    }.animateItem(),
                                        )
                                    }
                                }

                                items(
                                    items = filteredItems,
                                    key = { it.id },
                                    contentType = { CONTENT_TYPE_PLAYLIST },
                                ) { item ->
                                    when (item) {
                                        is Playlist -> {
                                            PlaylistGridItem(
                                                playlist = item,
                                                fillMaxWidth = true,
                                                autoPlaylist = item.id == PlaylistEntity.LIKED_PLAYLIST_ID,
                                                modifier =
                                                    Modifier
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
                                                        ).animateItem(),
                                            )
                                        }

                                        is Song -> {
                                            SongGridItem(
                                                song = item,
                                                isActive = item.id == mediaMetadata?.id,
                                                isPlaying = isPlaying,
                                                showLikedIcon = false,
                                                fillMaxWidth = true,
                                                modifier =
                                                    Modifier
                                                        .fillMaxWidth()
                                                        .combinedClickable(
                                                            onClick = {
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
                                                            },
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
                                                        )
                                                        .animateItem(),
                                            )
                                        }

                                        is Artist -> {
                                            ArtistGridItem(
                                                artist = item,
                                                showLikedIcon = false,
                                                fillMaxWidth = true,
                                                modifier =
                                                    Modifier
                                                        .fillMaxWidth()
                                                        .combinedClickable(
                                                            onClick = {
                                                                navController.navigate("artist/${item.id}")
                                                            },
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
                                                        ).animateItem(),
                                            )
                                        }

                                        is Album -> {
                                            AlbumGridItem(
                                                album = item,
                                                showLikedIcon = false,
                                                isActive = item.id == mediaMetadata?.album?.id,
                                                isPlaying = isPlaying,
                                                coroutineScope = coroutineScope,
                                                fillMaxWidth = true,
                                                showPlayButton = false,
                                                modifier =
                                                    Modifier
                                                        .fillMaxWidth()
                                                        .combinedClickable(
                                                            onClick = {
                                                                navController.navigate("album/${item.id}")
                                                            },
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
                                                        ).animateItem(),
                                            )
                                        }

                                        else -> {}
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
                                        LibrarySearchEmptyPlaceholder(modifier = Modifier.animateItem())
                                    }
                                }
                            }
                        }

                        else -> {}
                    }
                }
            }

        }
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
    showCache: Boolean,
    isOffline: Boolean,
) {
    val albumsStr = stringResource(R.string.albums)
    val artistsStr = stringResource(R.string.artists)
    val playlistsStr = stringResource(R.string.playlists)
    val cacheStr = stringResource(R.string.cache)
    val uploadedStr = stringResource(R.string.filter_uploaded)

    val items = remember(isOffline, showUploads, showCache, albumsStr, artistsStr, playlistsStr, cacheStr, uploadedStr) {
        buildList {
            add(CategoryItem(playlistsStr, R.drawable.queue_music, if (isOffline) "library_playlists_offline" else "library_playlists"))
            add(CategoryItem(albumsStr, R.drawable.album, if (isOffline) "library_albums_offline" else "library_albums"))
            add(CategoryItem(artistsStr, R.drawable.artist, if (isOffline) "library_artists_offline" else "library_artists"))
            add(CategoryItem("All Tracks", R.drawable.library_music, if (isOffline) "library_songs_offline" else "library_songs"))
            if (isOffline && showCache) add(CategoryItem(cacheStr, R.drawable.cached, "cache_playlist/cached"))
            if (isOffline && showUploads) add(CategoryItem(uploadedStr, R.drawable.upload, "auto_playlist/uploaded"))
        }
    }

    Column {
        items.forEach { item ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .clickable { navController.navigate(item.route) },
            ) {
                Icon(
                    painter = painterResource(item.icon),
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    text = item.label,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f),
                )
                Icon(
                    painter = painterResource(R.drawable.navigate_next),
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
            )
        }
    }
}
