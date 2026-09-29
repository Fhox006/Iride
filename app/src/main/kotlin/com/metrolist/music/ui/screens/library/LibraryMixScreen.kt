/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.screens.library

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import android.content.Intent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.items as listItems
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import com.metrolist.music.constants.LibraryViewType
import com.metrolist.music.constants.MixSortDescendingKey
import com.metrolist.music.constants.MixSortType
import com.metrolist.music.constants.MixSortTypeKey
import com.metrolist.music.constants.MixViewTypeKey
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
import com.metrolist.music.ui.component.LibraryArtistListItem
import com.metrolist.music.ui.component.LibraryPlaylistListItem
import com.metrolist.music.ui.component.LibrarySearchEmptyPlaceholder
import com.metrolist.music.ui.component.IrideSegmentedToggle
import com.metrolist.music.ui.component.IridePageLayout
import com.metrolist.music.ui.component.IridePageLayoutDefaults
import com.metrolist.music.ui.component.IrideSearchBox
import com.metrolist.music.ui.component.LibrarySectionLabel
import com.metrolist.music.ui.component.LocalItemHorizontalPadding
import com.metrolist.music.ui.component.LocalMenuState
import com.metrolist.music.ui.component.PlaylistGridItem
import com.metrolist.music.ui.component.PlaylistListItem
import com.metrolist.music.ui.component.rubberBandOverscroll
import com.metrolist.music.ui.component.SongGridItem
import com.metrolist.music.ui.component.SongListItem
import com.metrolist.music.ui.theme.SpaceMonoFontFamily
import com.metrolist.music.ui.theme.strokeHairline
import com.metrolist.music.ui.theme.textPrimary
import com.metrolist.music.ui.theme.textSecondary
import com.metrolist.music.ui.theme.textTertiary
import com.metrolist.music.ui.menu.AlbumMenu
import com.metrolist.music.ui.menu.ArtistMenu
import com.metrolist.music.ui.menu.PlaylistMenu
import com.metrolist.music.ui.menu.SongMenu
import com.metrolist.music.ui.utils.IrideMotion
import com.metrolist.music.ui.utils.rememberReducedMotion
import com.metrolist.music.ui.utils.IrideTabEntrance
import com.metrolist.music.ui.utils.irideEnter
import com.metrolist.music.ui.utils.revealMask
import com.metrolist.music.ui.utils.rememberEnterProgress
import com.metrolist.music.ui.utils.rememberSectionEnter
import com.metrolist.music.utils.rememberEnumPreference
import com.metrolist.music.utils.rememberPreference
import com.metrolist.music.viewmodels.LibraryMixViewModel
import com.metrolist.music.viewmodels.CachePlaylistViewModel
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
        albums.value + artist.value + playlist.value + likedEntry
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
                .background(Color.Black)
                .rubberBandOverscroll(
                    Orientation.Vertical,
                    if (isListView) lazyListState else lazyGridState,
                ),
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
                                modifier = Modifier.fillMaxWidth().clickable { navController.navigate("auto_playlist/downloaded") },
                            )
                        }
                        if (showCachedPlaylists) item(key = "cachedPlaylist") {
                            PlaylistListItem(
                                playlist = cachedPlaylist,
                                autoPlaylist = true,
                                modifier = Modifier.fillMaxWidth().clickable { navController.navigate("cache_playlist/cached") },
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
                        listItems(filteredItems, key = { it.id }) { item ->
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
                                            .clickable { navController.navigate("auto_playlist/downloaded") }
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
                                                onClick = { navController.navigate("auto_playlist/downloaded") },
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
                                            .clickable { navController.navigate("cache_playlist/cached") }
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
                                                onClick = { navController.navigate("cache_playlist/cached") },
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
                        key = { it.id },
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
                    .clickable { navController.navigate(item.route) },
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
