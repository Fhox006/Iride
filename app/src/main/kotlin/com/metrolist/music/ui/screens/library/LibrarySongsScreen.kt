/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.screens.library

import androidx.compose.foundation.layout.statusBars
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
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
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.toMutableStateList
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.metrolist.music.LocalPlayerAwareWindowInsets
import com.metrolist.music.LocalPlayerConnection
import com.metrolist.music.LocalDatabase
import com.metrolist.music.R
import com.metrolist.music.constants.CONTENT_TYPE_SONG
import com.metrolist.music.constants.CONTENT_TYPE_HEADER
import com.metrolist.music.constants.HideExplicitKey
import com.metrolist.music.constants.PureBlackKey
import com.metrolist.music.constants.SongSortDescendingKey
import com.metrolist.music.constants.SongSortType
import com.metrolist.music.constants.SongSortTypeKey
import com.metrolist.music.constants.YtmSyncKey
import com.metrolist.music.extensions.toMediaItem
import com.metrolist.music.extensions.matchesNormalizedQuery
import com.metrolist.music.extensions.normalizeForSearch
import com.metrolist.music.playback.queues.ListQueue
import com.metrolist.music.playback.queues.buildRotationQueue
import com.metrolist.music.ui.component.CollapsingScreenHeader
import com.metrolist.music.ui.component.LibrarySearchEmptyPlaceholder
import com.metrolist.music.ui.component.EmptyPlaceholder
import com.metrolist.music.ui.component.LibrarySortRow
import com.metrolist.music.ui.component.IrideAdaptiveTopBar
import com.metrolist.music.ui.component.IrideSearchBox
import com.metrolist.music.ui.component.LocalMenuState
import com.metrolist.music.ui.component.SongListItem
import com.metrolist.music.ui.component.LibraryHeroTitle
import com.metrolist.music.ui.component.LibraryFooterCount
import com.metrolist.music.ui.component.LibraryPageTopBar
import com.metrolist.music.ui.component.LibraryListPage
import com.metrolist.music.ui.component.LibraryPlaybackActions
import com.metrolist.music.db.entities.Song
import com.metrolist.music.ui.component.IridePageLayout
import com.metrolist.music.ui.component.rubberBandOverscroll
import com.metrolist.music.ui.component.LocalItemHorizontalPadding
import com.metrolist.music.ui.component.rememberLibraryPageRevealState
import com.metrolist.music.ui.component.rememberLibraryTopBarProgress
import com.metrolist.music.ui.component.LocalLibraryTextColor
import com.metrolist.music.ui.menu.SongMenu
import com.metrolist.music.ui.menu.SelectionSongMenu
import com.metrolist.music.ui.theme.SpaceMonoFontFamily
import com.metrolist.music.ui.utils.IrideMotion
import com.metrolist.music.ui.utils.rememberReducedMotion
import com.metrolist.music.ui.utils.irideEnter
import com.metrolist.music.ui.utils.rememberDiscreteProgress
import com.metrolist.music.ui.utils.rememberEnterProgress
import com.metrolist.music.ui.utils.revealMask
import com.metrolist.music.utils.rememberEnumPreference
import com.metrolist.music.utils.rememberPreference
import com.metrolist.music.viewmodels.LibrarySongsViewModel
import com.metrolist.music.viewmodels.CachePlaylistViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun LibrarySongsScreen(
    navController: NavController,
    isOffline: Boolean = false,
    isCache: Boolean = false,
    isLocal: Boolean = false,
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
    val (sortDescending, onSortDescendingChange) = rememberPreference(SongSortDescendingKey, true)

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
        sortDescending = sortDescending,
        onSortDescendingChange = onSortDescendingChange,
        emptyMessageRes = when {
            isCache -> R.string.playlist_is_empty
            isOffline -> R.string.no_downloads_yet
            else -> null
        },
        modifier = Modifier
            .fillMaxSize()
            .background(if (pureBlack) Color.Black else MaterialTheme.colorScheme.background)
            .rubberBandOverscroll(Orientation.Vertical, lazyListState),
        bottomPadding = LocalPlayerAwareWindowInsets.current.asPaddingValues().calculateBottomPadding(),
        onNavigateUp = { navController.navigateUp() },
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
    sortDescending: Boolean,
    onSortDescendingChange: (Boolean) -> Unit,
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
        SongSortType.ARTIST to stringResource(R.string.sort_by_artist),
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
            playbackActions = { LibraryPlaybackActions(onShuffle, onRotation) },
            overlay = overlay,
        ) {
            item(key = "search", contentType = CONTENT_TYPE_HEADER) {
                IrideSearchBox(
                    query = query,
                    onQueryChange = onQueryChange,
                    placeholderText = stringResource(searchPlaceholderRes),
                    focusRequester = searchFocusRequester,
                    onFocusChanged = {},
                    onSearch = { keyboardController?.hide() },
                    onClear = { onQueryChange(TextFieldValue()) },
                    modifier = Modifier.padding(bottom = 8.dp),
                )
            }
            item(key = "sort", contentType = CONTENT_TYPE_HEADER) {
                LibrarySortRow(
                    sortOptions = sortOptions,
                    currentSort = sortType,
                    onSortChange = onSortTypeChange,
                    sortDescending = sortDescending,
                    onSortDescendingChange = onSortDescendingChange,
                    verticalPadding = 4.dp,
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
