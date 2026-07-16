/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.screens.playlist

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEachReversed
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import com.metrolist.music.LocalPlayerAwareWindowInsets
import com.metrolist.music.LocalPlayerConnection
import com.metrolist.music.R
import com.metrolist.music.constants.BetterLibraryBetaKey
import com.metrolist.music.constants.CONTENT_TYPE_HEADER
import com.metrolist.music.constants.CONTENT_TYPE_SONG
import com.metrolist.music.constants.PureBlackKey
import com.metrolist.music.constants.SongSortDescendingKey
import com.metrolist.music.constants.SongSortType
import com.metrolist.music.constants.SongSortTypeKey
import com.metrolist.music.extensions.toMediaItem
import com.metrolist.music.playback.queues.ListQueue
import com.metrolist.music.ui.component.CollapsingScreenHeader
import com.metrolist.music.ui.component.EmptyPlaceholder
import com.metrolist.music.ui.component.GenrePillsRow
import com.metrolist.music.ui.component.GenreSongInfo
import com.metrolist.music.ui.component.HideOnScrollFAB
import com.metrolist.music.ui.component.LibrarySearchEmptyPlaceholder
import com.metrolist.music.ui.component.LibrarySortRow
import com.metrolist.music.ui.component.LocalMenuState
import com.metrolist.music.ui.component.SongListItem
import com.metrolist.music.ui.component.rememberGenreFilter
import com.metrolist.music.ui.menu.SelectionSongMenu
import com.metrolist.music.ui.menu.SongMenu
import com.metrolist.music.utils.rememberEnumPreference
import com.metrolist.music.utils.rememberPreference
import com.metrolist.music.viewmodels.CachePlaylistViewModel
import java.time.LocalDateTime

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun CachePlaylistScreen(
    navController: NavController,
    viewModel: CachePlaylistViewModel = hiltViewModel(),
) {
    val menuState = LocalMenuState.current
    val playerConnection = LocalPlayerConnection.current ?: return
    val haptic = LocalHapticFeedback.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val cachedPlaylistStr = stringResource(R.string.cached_playlist)

    val isPlaying by playerConnection.isEffectivelyPlaying.collectAsState()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()
    val cachedSongs by viewModel.cachedSongs.collectAsState()

    val (sortType, onSortTypeChange) = rememberEnumPreference(
        SongSortTypeKey,
        SongSortType.CREATE_DATE,
    )
    val (sortDescending, onSortDescendingChange) = rememberPreference(SongSortDescendingKey, true)
    val pureBlack by rememberPreference(PureBlackKey, defaultValue = false)
    val betterLibraryBeta by rememberPreference(BetterLibraryBetaKey, defaultValue = false)

    val sortedSongs = remember(cachedSongs, sortType, sortDescending) {
        val sorted = when (sortType) {
            SongSortType.CREATE_DATE -> cachedSongs.sortedBy { it.song.dateDownload ?: LocalDateTime.MIN }
            SongSortType.NAME -> cachedSongs.sortedBy { it.song.title }
            SongSortType.ARTIST -> cachedSongs.sortedBy { song ->
                song.artists.joinToString(separator = "") { it.name }
            }
            SongSortType.PLAY_TIME -> cachedSongs.sortedBy { it.song.totalPlayTime }
        }
        if (sortDescending) sorted.reversed() else sorted
    }

    var inSelectMode by rememberSaveable { mutableStateOf(false) }
    val selection = rememberSaveable(
        saver = listSaver<MutableList<String>, String>(
            save = { it.toList() },
            restore = { it.toMutableStateList() },
        ),
    ) { mutableStateListOf() }
    var selectionAnchorSongId by rememberSaveable { mutableStateOf<String?>(null) }
    val onExitSelectionMode = {
        inSelectMode = false
        selection.clear()
        selectionAnchorSongId = null
    }

    var isSearchActive by rememberSaveable { mutableStateOf(false) }
    var searchQuery by rememberSaveable { mutableStateOf("") }

    BackHandler(enabled = inSelectMode, onBack = onExitSelectionMode)
    BackHandler(enabled = !inSelectMode && isSearchActive) {
        isSearchActive = false
        searchQuery = ""
    }

    val genreFilter =
        rememberGenreFilter(
            remember(sortedSongs) {
                sortedSongs.map { GenreSongInfo(it.id, it.title, it.artists.firstOrNull()?.name) }
            },
        )

    val filteredSongs = remember(sortedSongs, searchQuery, genreFilter.selectedGenre, genreFilter.genreBySongId) {
        val base =
            if (searchQuery.isBlank()) sortedSongs
            else sortedSongs.filter { song ->
                song.title.contains(searchQuery, true) ||
                    song.artists.any { it.name.contains(searchQuery, true) }
            }
        base.filter { genreFilter.matches(it.id) }
    }

    LaunchedEffect(filteredSongs) {
        selection.fastForEachReversed { songId ->
            if (filteredSongs.find { it.id == songId } == null) {
                selection.remove(songId)
            }
        }

        if (selectionAnchorSongId != null && filteredSongs.none { it.id == selectionAnchorSongId }) {
            selectionAnchorSongId = filteredSongs.firstOrNull { it.id in selection }?.id
        }
    }

    val lazyListState = rememberLazyListState()

    val scrollBehavior = if (betterLibraryBeta) {
        TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    } else {
        TopAppBarDefaults.exitUntilCollapsedScrollBehavior(
            snapAnimationSpec = tween(durationMillis = 200),
        )
    }

    val itemCountText = pluralStringResource(R.plurals.n_song, filteredSongs.size, filteredSongs.size)

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            if (inSelectMode) {
                TopAppBar(
                    title = {
                        Text(
                            text = pluralStringResource(R.plurals.n_song, selection.size, selection.size),
                            style = MaterialTheme.typography.titleLarge,
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onExitSelectionMode) {
                            Icon(
                                painter = painterResource(R.drawable.close),
                                contentDescription = null,
                            )
                        }
                    },
                    actions = {
                        Checkbox(
                            checked = selection.size == filteredSongs.size && selection.isNotEmpty(),
                            onCheckedChange = {
                                if (selection.size == filteredSongs.size) {
                                    selection.clear()
                                } else {
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
                            Icon(
                                painter = painterResource(R.drawable.more_vert),
                                contentDescription = null,
                            )
                        }
                    },
                )
            } else {
                CollapsingScreenHeader(
                    title = cachedPlaylistStr,
                    scrollBehavior = scrollBehavior,
                    pureBlack = pureBlack,
                    isSearchActive = isSearchActive,
                    onSearchActiveChange = { active ->
                        isSearchActive = active
                        if (!active) searchQuery = ""
                    },
                    searchQuery = searchQuery,
                    onSearchQueryChange = { searchQuery = it },
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
            }
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
                    },
                )
                .padding(paddingValues),
        ) {
            if (sortedSongs.isEmpty()) {
                EmptyPlaceholder(
                    icon = R.drawable.music_note,
                    text = stringResource(R.string.playlist_is_empty),
                )
            } else {
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
                            sortOptions = listOf(
                                SongSortType.CREATE_DATE to stringResource(R.string.sort_by_create_date),
                                SongSortType.NAME to stringResource(R.string.sort_by_name),
                                SongSortType.ARTIST to stringResource(R.string.sort_by_artist),
                                SongSortType.PLAY_TIME to stringResource(R.string.sort_by_play_time),
                            ),
                            currentSort = sortType,
                            onSortChange = onSortTypeChange,
                            sortDescending = sortDescending,
                            onSortDescendingChange = onSortDescendingChange,
                        )
                    }

                    item(key = "genre_pills", contentType = CONTENT_TYPE_HEADER) {
                        GenrePillsRow(state = genreFilter)
                    }

                    if (filteredSongs.isEmpty() && searchQuery.isNotBlank()) {
                        item(key = "empty_search_result", contentType = CONTENT_TYPE_HEADER) {
                            LibrarySearchEmptyPlaceholder(modifier = Modifier.animateItem())
                        }
                    }

                    itemsIndexed(
                        filteredSongs,
                        key = { _, song -> song.id },
                        contentType = { _, _ -> CONTENT_TYPE_SONG },
                    ) { index, song ->
                        val onCheckedChange: (Boolean) -> Unit = {
                            if (it) {
                                selection.add(song.id)
                            } else {
                                selection.remove(song.id)
                            }
                        }

                        SongListItem(
                            song = song,
                            isActive = song.id == mediaMetadata?.id,
                            isPlaying = isPlaying,
                            trailingContent = {
                                if (inSelectMode) {
                                    Checkbox(
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
                                                isFromCache = true,
                                            )
                                        }
                                    }) {
                                        Icon(
                                            painter = painterResource(R.drawable.more_vert),
                                            contentDescription = null,
                                        )
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .combinedClickable(
                                    onClick = {
                                        if (inSelectMode) {
                                            onCheckedChange(song.id !in selection)
                                        } else if (song.id == mediaMetadata?.id) {
                                            playerConnection.togglePlayPause()
                                        } else {
                                            playerConnection.playQueue(
                                                ListQueue(
                                                    title = cachedPlaylistStr,
                                                    items = filteredSongs.map { it.toMediaItem() },
                                                    startIndex = index,
                                                ),
                                            )
                                        }
                                    },
                                    onLongClick = {
                                        if (!inSelectMode) {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            inSelectMode = true
                                            onCheckedChange(true)
                                            selectionAnchorSongId = song.id
                                        } else {
                                            val anchorIndex = selectionAnchorSongId?.let { anchorSongId ->
                                                filteredSongs.indexOfFirst { it.id == anchorSongId }
                                            } ?: -1

                                            if (anchorIndex == -1) {
                                                onCheckedChange(true)
                                                selectionAnchorSongId = song.id
                                            } else {
                                                val range = if (anchorIndex <= index) anchorIndex..index else index..anchorIndex
                                                for (rangeIndex in range) {
                                                    val rangeSongId = filteredSongs[rangeIndex].id
                                                    if (rangeSongId !in selection) {
                                                        selection.add(rangeSongId)
                                                    }
                                                }
                                            }
                                        }
                                    },
                                )
                                .animateItem(),
                        )
                    }

                    item(key = "footer") {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp),
                            contentAlignment = androidx.compose.ui.Alignment.Center,
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

            HideOnScrollFAB(
                visible = filteredSongs.isNotEmpty() && !inSelectMode,
                lazyListState = lazyListState,
                icon = R.drawable.shuffle,
                label = if (betterLibraryBeta) stringResource(R.string.shuffle) else null,
                onClick = {
                    playerConnection.playQueue(
                        ListQueue(
                            title = cachedPlaylistStr,
                            items = filteredSongs.shuffled().map { it.toMediaItem() },
                        ),
                    )
                },
            )
        }
    }
}
