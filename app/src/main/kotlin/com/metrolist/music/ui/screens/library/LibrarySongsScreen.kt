/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.screens.library

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.metrolist.music.LocalPlayerAwareWindowInsets
import com.metrolist.music.LocalPlayerConnection
import com.metrolist.music.R
import com.metrolist.music.constants.AlbumTopGradientKey
import com.metrolist.music.constants.CONTENT_TYPE_HEADER
import com.metrolist.music.constants.CONTENT_TYPE_SONG
import com.metrolist.music.constants.HideExplicitKey
import com.metrolist.music.constants.PlayerBackgroundStyleKey
import com.metrolist.music.constants.PureBlackKey
import com.metrolist.music.constants.SongSortDescendingKey
import com.metrolist.music.constants.SongSortType
import com.metrolist.music.constants.SongSortTypeKey
import com.metrolist.music.constants.YtmSyncKey
import com.metrolist.music.extensions.matchesNormalizedQuery
import com.metrolist.music.extensions.normalizeForSearch
import com.metrolist.music.extensions.toMediaItem
import com.metrolist.music.playback.queues.ListQueue
import com.metrolist.music.ui.component.CollapsingScreenHeader
import com.metrolist.music.ui.component.HideOnScrollFAB
import com.metrolist.music.ui.component.LibrarySearchEmptyPlaceholder
import com.metrolist.music.ui.component.LibrarySortRow
import com.metrolist.music.ui.component.LocalMenuState
import com.metrolist.music.ui.component.SongListItem
import com.metrolist.music.ui.component.LibraryHeroTitle
import com.metrolist.music.ui.component.LibraryFooterCount
import com.metrolist.music.ui.component.LibraryPageTopBar
import com.metrolist.music.ui.component.LocalItemHorizontalPadding
import com.metrolist.music.ui.component.rememberLibraryPageRevealState
import com.metrolist.music.ui.component.rememberLibraryTopBarProgress
import com.metrolist.music.ui.component.TopScreenGradientBackground
import com.metrolist.music.ui.component.frostedTopBarBackground
import com.metrolist.music.ui.component.recordFrostBackdrop
import com.metrolist.music.ui.component.rememberFrostBackdrop
import com.metrolist.music.ui.menu.SongMenu
import com.metrolist.music.ui.theme.SpaceMonoFontFamily
import com.metrolist.music.ui.theme.textPrimary
import com.metrolist.music.ui.utils.IrideMotion
import com.metrolist.music.ui.utils.irideEnter
import com.metrolist.music.ui.utils.rememberDiscreteProgress
import com.metrolist.music.ui.utils.rememberEnterProgress
import com.metrolist.music.ui.utils.revealMask
import com.metrolist.music.utils.rememberEnumPreference
import com.metrolist.music.utils.rememberPreference
import com.metrolist.music.viewmodels.LibrarySongsViewModel

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun LibrarySongsScreen(
    navController: NavController,
    isOffline: Boolean = false,
    viewModel: LibrarySongsViewModel = hiltViewModel(),
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val menuState = LocalMenuState.current
    val queueAllSongsStr = stringResource(R.string.queue_all_songs)
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
    val albumTopGradientEnabled by rememberPreference(AlbumTopGradientKey, defaultValue = true)
    val playerBackgroundStyle by rememberEnumPreference(
        PlayerBackgroundStyleKey,
        defaultValue = com.metrolist.music.constants.PlayerBackgroundStyle.BETTER_ANIMATED_GRADIENT,
    )

    val songs by (if (isOffline) viewModel.downloadedSongs else viewModel.allSongs).collectAsState()
    var isSearchActive by rememberSaveable { mutableStateOf(false) }
    val searchQuery by viewModel.searchQuery.collectAsState()
    val debouncedSearchQuery by viewModel.debouncedSearchQuery.collectAsState()
    val normalizedQuery = remember(debouncedSearchQuery) { debouncedSearchQuery.normalizeForSearch() }

    LaunchedEffect(Unit) {
        if (ytmSync) {
            viewModel.syncLibrarySongs()
        }
    }

    val lazyListState = rememberLazyListState()

    val backStackEntry by navController.currentBackStackEntryAsState()
    val scrollToTop =
        backStackEntry?.savedStateHandle?.getStateFlow("scrollToTop", false)?.collectAsState()

    LaunchedEffect(scrollToTop?.value) {
        if (scrollToTop?.value == true) {
            lazyListState.animateScrollToItem(0)
            backStackEntry?.savedStateHandle?.set("scrollToTop", false)
        }
    }

    val filteredSongs by remember(songs, hideExplicit, normalizedQuery) {
        androidx.compose.runtime.derivedStateOf {
            (if (hideExplicit) songs.filter { !it.song.explicit } else songs).filter { song ->
                val artistNames = song.artists.map { it.name }.toTypedArray()
                matchesNormalizedQuery(normalizedQuery, song.song.title, song.album?.title, *artistNames)
            }
        }
    }

    val sortOptions = listOf(
        SongSortType.CREATE_DATE to stringResource(R.string.sort_by_create_date),
        SongSortType.NAME        to stringResource(R.string.sort_by_name),
        SongSortType.ARTIST      to stringResource(R.string.sort_by_artist),
        SongSortType.PLAY_TIME   to stringResource(R.string.sort_by_play_time),
    )

    val itemCountText = pluralStringResource(R.plurals.n_song, filteredSongs.size, filteredSongs.size)

    val frostBackdrop = rememberFrostBackdrop()
    val revealState = rememberLibraryPageRevealState()
    val topBarRevealProgress = rememberLibraryTopBarProgress(
        state = revealState,
        scrolledPastHeader = lazyListState.firstVisibleItemIndex > 0,
    )
    val screenProgress = rememberEnterProgress(play = true, durationMillis = IrideMotion.Short, easing = IrideMotion.EaseOutQuart)

    val scrollBehavior = if (betterLibraryBeta) {
        TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    } else {
        TopAppBarDefaults.exitUntilCollapsedScrollBehavior(
            snapAnimationSpec = tween(durationMillis = 200),
        )
    }

    val songListContent: LazyListScope.() -> Unit = {
        item(key = "page_header", contentType = CONTENT_TYPE_HEADER) {
            LibraryHeroTitle(
                title = stringResource(R.string.all_tracks),
                entranceAlpha = screenProgress,
                revealState = revealState,
            )
            LibrarySortRow(
                sortOptions = sortOptions,
                currentSort = sortType,
                onSortChange = onSortTypeChange,
                sortDescending = sortDescending,
                onSortDescendingChange = onSortDescendingChange,
            )
        }

        if (filteredSongs.isEmpty() && searchQuery.isNotBlank()) {
            item(key = "empty_search_result", contentType = CONTENT_TYPE_HEADER) {
                LibrarySearchEmptyPlaceholder(modifier = Modifier.animateItem())
            }
        }

        itemsIndexed(
            items = filteredSongs,
            key = { _, item -> item.song.id },
            contentType = { _, _ -> CONTENT_TYPE_SONG },
        ) { index, song ->
            SongListItem(
                song = song,
                isActive = song.id == mediaMetadata?.id,
                isPlaying = isPlaying,
                showLikedIcon = false,
                trailingContent = {
                    IconButton(
                        onClick = {
                            menuState.show {
                                SongMenu(
                                    originalSong = song,
                                    navController = navController,
                                    onDismiss = menuState::dismiss,
                                )
                            }
                        },
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.more_vert),
                            contentDescription = if (betterLibraryBeta)
                                stringResource(R.string.more_options)
                            else null,
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        if (song.id == mediaMetadata?.id) {
                            playerConnection.togglePlayPause()
                        } else {
                            playerConnection.playQueue(
                                ListQueue(
                                    title = queueAllSongsStr,
                                    items = filteredSongs.map { it.toMediaItem() },
                                    startIndex = index,
                                ),
                            )
                        }
                    }
                    .animateItem(),
            )
        }

        item(key = "footer") {
            LibraryFooterCount(text = itemCountText)
        }
    }

    val songsFab: @Composable BoxScope.() -> Unit = {
        HideOnScrollFAB(
            visible = filteredSongs.isNotEmpty(),
            lazyListState = lazyListState,
            icon = R.drawable.shuffle,
            label = if (betterLibraryBeta) stringResource(R.string.shuffle) else null,
            onClick = {
                playerConnection.playQueue(
                    ListQueue(
                        title = queueAllSongsStr,
                        items = filteredSongs.shuffled().map { it.toMediaItem() },
                    ),
                )
            },
        )
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
                songListContent()
            }
        }

        songsFab()
    }

        LibraryPageTopBar(
            title = stringResource(R.string.all_tracks),
            revealProgress = topBarRevealProgress,
            revealState = revealState,
            backdrop = frostBackdrop,
            isSearchActive = isSearchActive,
            searchQuery = searchQuery,
            onSearchQueryChange = viewModel::updateSearchQuery,
            onNavigateUp = { navController.navigateUp() },
            onSearchClick = { isSearchActive = true },
            onCloseSearch = {
                isSearchActive = false
                viewModel.updateSearchQuery("")
            },
            keyboardController = keyboardController,
        )
    }
}
