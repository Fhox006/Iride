/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults.Indicator
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import coil3.request.CachePolicy
import coil3.request.crossfade
import com.metrolist.innertube.models.AlbumItem
import com.metrolist.innertube.models.ArtistItem
import com.metrolist.innertube.models.EpisodeItem
import com.metrolist.innertube.models.PlaylistItem
import com.metrolist.innertube.models.PodcastItem
import com.metrolist.innertube.models.SongItem
import com.metrolist.innertube.models.WatchEndpoint
import com.metrolist.innertube.models.YTItem
import com.metrolist.innertube.models.isMixtape
import com.metrolist.innertube.utils.parseCookieString
import com.metrolist.music.LocalListenTogetherManager
import com.metrolist.music.LocalPlayerAwareWindowInsets
import com.metrolist.music.LocalPlayerConnection
import com.metrolist.music.R
import com.metrolist.music.constants.GridItemSize
import com.metrolist.music.constants.GridItemsSizeKey
import com.metrolist.music.constants.GridThumbnailHeight
import com.metrolist.music.constants.HideExplicitKey
import com.metrolist.music.constants.HideVideoSongsKey
import com.metrolist.music.constants.HideYoutubeShortsKey
import com.metrolist.music.constants.InnerTubeCookieKey
import com.metrolist.music.constants.ListItemHeight
import com.metrolist.music.constants.ListThumbnailSize
import com.metrolist.music.constants.SmallGridThumbnailHeight
import com.metrolist.music.db.entities.Album
import com.metrolist.music.db.entities.Artist
import com.metrolist.music.db.entities.LocalItem
import com.metrolist.music.db.entities.Playlist
import com.metrolist.music.db.entities.Song
import com.metrolist.music.extensions.toMediaItem
import com.metrolist.music.models.toMediaMetadata
import com.metrolist.music.playback.queues.ListQueue
import com.metrolist.music.playback.queues.YouTubeQueue
import com.metrolist.music.ui.component.AlbumGridItem
import com.metrolist.music.ui.component.ArtistGridItem
import com.metrolist.music.ui.component.ChipsRow
import com.metrolist.music.ui.component.LocalMenuState
import com.metrolist.music.ui.component.NavigationTitle
import com.metrolist.music.ui.component.RandomizeGridItem
import com.metrolist.music.ui.component.SongGridItem
import com.metrolist.music.ui.component.SongListItem
import com.metrolist.music.ui.component.SpeedDialGridItem
import com.metrolist.music.ui.component.YouTubeGridItem
import com.metrolist.music.ui.component.YouTubeListItem
import com.metrolist.music.ui.menu.AlbumMenu
import com.metrolist.music.ui.menu.ArtistMenu
import com.metrolist.music.ui.menu.SongMenu
import com.metrolist.music.ui.menu.YouTubeAlbumMenu
import com.metrolist.music.ui.menu.YouTubeArtistMenu
import com.metrolist.music.ui.menu.YouTubePlaylistMenu
import com.metrolist.music.ui.menu.YouTubeSongMenu
import com.metrolist.music.ui.utils.SnapLayoutInfoProvider
import com.metrolist.music.utils.SyncStatus
import com.metrolist.music.utils.rememberEnumPreference
import com.metrolist.music.utils.rememberPreference
import com.metrolist.music.viewmodels.HomeViewModel
import kotlinx.coroutines.launch
import kotlin.math.min

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class,
    androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun NewHomeScreen(
    navController: NavController,
    @Suppress("UNUSED_PARAMETER") snackbarHostState: SnackbarHostState,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val menuState = LocalMenuState.current
    val playerConnection = LocalPlayerConnection.current ?: return
    val haptic = LocalHapticFeedback.current
    val listenTogetherManager = LocalListenTogetherManager.current
    val isListenTogetherGuest = listenTogetherManager?.let { it.isInRoom && !it.isHost } ?: false

    val isPlaying by playerConnection.isEffectivelyPlaying.collectAsStateWithLifecycle()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsStateWithLifecycle()

    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
    var betaBannerDismissed by rememberSaveable { mutableStateOf(false) }

    val speedDialItems by viewModel.speedDialItems.collectAsStateWithLifecycle()
    val pinnedSpeedDialItems by viewModel.pinnedSpeedDialItems.collectAsStateWithLifecycle()
    val pinnedIds: Set<String> by remember(pinnedSpeedDialItems) {
        derivedStateOf { pinnedSpeedDialItems.map { it.id }.toSet() }
    }
    val isRandomizing by viewModel.isRandomizing.collectAsStateWithLifecycle()
    val speedDialSongIds: Set<String> by remember(speedDialItems) {
        derivedStateOf { speedDialItems.filterIsInstance<SongItem>().map { it.id }.toSet() }
    }

    val quickPicks by viewModel.quickPicks.collectAsStateWithLifecycle()
    val keepListening by viewModel.keepListening.collectAsStateWithLifecycle()
    val forgottenFavorites by viewModel.forgottenFavorites.collectAsStateWithLifecycle()
    val communityPlaylists by viewModel.communityPlaylists.collectAsStateWithLifecycle()
    val dailyDiscover by viewModel.dailyDiscover.collectAsStateWithLifecycle()
    val accountPlaylists by viewModel.accountPlaylists.collectAsStateWithLifecycle()
    val similarRecommendations by viewModel.similarRecommendations.collectAsStateWithLifecycle()
    val homePage by viewModel.homePage.collectAsStateWithLifecycle()
    val phase1Complete by viewModel.phase1Complete.collectAsStateWithLifecycle()

    val accountName by viewModel.accountName.collectAsStateWithLifecycle()
    val accountImageUrl by viewModel.accountImageUrl.collectAsStateWithLifecycle()
    val syncState by viewModel.syncState.collectAsStateWithLifecycle()
    val innerTubeCookie by rememberPreference(InnerTubeCookieKey, "")
    val isLoggedIn = remember(innerTubeCookie) { "SAPISID" in parseCookieString(innerTubeCookie) }
    val accountAvatarUrl = if (isLoggedIn) accountImageUrl else null

    val hideExplicit by rememberPreference(HideExplicitKey, defaultValue = false)
    val hideVideoSongs by rememberPreference(HideVideoSongsKey, defaultValue = false)
    val hideYoutubeShorts by rememberPreference(HideYoutubeShortsKey, defaultValue = false)
    val gridItemSize by rememberEnumPreference(GridItemsSizeKey, GridItemSize.BIG)
    val currentGridHeight = if (gridItemSize == GridItemSize.BIG) GridThumbnailHeight else SmallGridThumbnailHeight

    // Your Mood
    val moodPage by viewModel.moodPage.collectAsStateWithLifecycle()
    var selectedMoodCategory by remember { mutableStateOf<com.metrolist.innertube.pages.HomePage.Chip?>(null) }
    val moodChips = remember(homePage?.chips) {
        homePage?.chips?.map { it to it.title } ?: emptyList()
    }
    val moodMixesState = rememberLazyListState()

    LaunchedEffect(moodChips) {
        if (selectedMoodCategory == null && moodChips.isNotEmpty()) {
            selectedMoodCategory = moodChips.first().first
        }
    }
    LaunchedEffect(selectedMoodCategory) {
        moodMixesState.scrollToItem(0)
        if (selectedMoodCategory != null) {
            viewModel.loadMoodPage(
                selectedMoodCategory?.endpoint?.params,
                selectedMoodCategory?.title,
                hideExplicit, hideVideoSongs, hideYoutubeShorts,
            )
        }
    }

    val quickPicksLazyGridState = rememberLazyGridState()
    val forgottenFavoritesLazyGridState = rememberLazyGridState()
    LaunchedEffect(quickPicks) { quickPicksLazyGridState.scrollToItem(0) }
    LaunchedEffect(forgottenFavorites) { forgottenFavoritesLazyGridState.scrollToItem(0) }

    val scope = rememberCoroutineScope()
    var randomizeJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }
    val lazyListState = rememberLazyListState()

    LaunchedEffect(Unit) {
        snapshotFlow {
            lazyListState.layoutInfo.visibleItemsInfo.lastOrNull()?.index
        }.collect { lastVisibleIndex: Int? ->
            val len = lazyListState.layoutInfo.totalItemsCount
            if (lastVisibleIndex != null && lastVisibleIndex >= len - 3 && phase1Complete) {
                viewModel.loadMoreYouTubeItems(homePage?.continuation)
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.onSectionBecameVisible("daily_discover")
        viewModel.onSectionBecameVisible("from_the_community")
        viewModel.onSectionBecameVisible("similar_recommendation_0")
    }

    val pullRefreshState = rememberPullToRefreshState()

    PullToRefreshBox(
        state = pullRefreshState,
        isRefreshing = isRefreshing,
        onRefresh = viewModel::refresh,
        indicator = {
            Indicator(
                isRefreshing = isRefreshing,
                state = pullRefreshState,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(LocalPlayerAwareWindowInsets.current.asPaddingValues()),
            )
        },
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val containerWidthDp = maxWidth
            val horizontalLazyGridItemWidthFactor =
                if (containerWidthDp * 0.475f >= 320.dp) 0.475f else 0.9f
            val horizontalLazyGridItemWidth = containerWidthDp * horizontalLazyGridItemWidthFactor

            val quickPicksSnapLayoutInfoProvider = remember(quickPicksLazyGridState) {
                SnapLayoutInfoProvider(
                    lazyGridState = quickPicksLazyGridState,
                    positionInLayout = { layoutSize, itemSize ->
                        layoutSize * horizontalLazyGridItemWidthFactor / 2f - itemSize / 2f
                    },
                )
            }
            val forgottenFavoritesSnapLayoutInfoProvider = remember(forgottenFavoritesLazyGridState) {
                SnapLayoutInfoProvider(
                    lazyGridState = forgottenFavoritesLazyGridState,
                    positionInLayout = { layoutSize, itemSize ->
                        layoutSize * horizontalLazyGridItemWidthFactor / 2f - itemSize / 2f
                    },
                )
            }

            val localGridItem: @Composable (LocalItem) -> Unit = { item ->
                when (item) {
                    is Song -> SongGridItem(
                        song = item,
                        modifier = Modifier
                            .fillMaxWidth()
                            .combinedClickable(
                                onClick = {
                                    if (!isListenTogetherGuest) {
                                        if (item.id == mediaMetadata?.id) {
                                            playerConnection.togglePlayPause()
                                        } else {
                                            playerConnection.startRadioForSong(item.toMediaMetadata())
                                        }
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
                            ),
                        isActive = item.id == mediaMetadata?.id,
                        isPlaying = isPlaying,
                    )
                    is Album -> AlbumGridItem(
                        album = item,
                        isActive = item.id == mediaMetadata?.album?.id,
                        isPlaying = isPlaying,
                        coroutineScope = scope,
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
                            ),
                    )
                    is Artist -> ArtistGridItem(
                        artist = item,
                        modifier = Modifier
                            .fillMaxWidth()
                            .combinedClickable(
                                onClick = { navController.navigate("artist/${item.id}") },
                                onLongClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    menuState.show {
                                        ArtistMenu(
                                            originalArtist = item,
                                            coroutineScope = scope,
                                            onDismiss = menuState::dismiss,
                                        )
                                    }
                                },
                            ),
                    )
                    is Playlist -> {}
                }
            }

            val ytGridItem: @Composable (YTItem, androidx.compose.ui.unit.Dp?) -> Unit = { item, sizeOverride ->
                val size = sizeOverride ?: if (item.isMixtape) 180.dp else currentGridHeight
                YouTubeGridItem(
                    item = item,
                    isActive = item.id in listOf(mediaMetadata?.album?.id, mediaMetadata?.id),
                    isPlaying = isPlaying,
                    coroutineScope = scope,
                    thumbnailRatio = 1f,
                    size = size,
                    modifier = Modifier.combinedClickable(
                        onClick = {
                            when (item) {
                                is SongItem -> {
                                    if (!isListenTogetherGuest) {
                                        playerConnection.playQueue(
                                            YouTubeQueue(
                                                item.endpoint ?: WatchEndpoint(videoId = item.id),
                                                item.toMediaMetadata(),
                                            ),
                                        )
                                    }
                                }
                                is AlbumItem -> navController.navigate("album/${item.id}")
                                is ArtistItem -> navController.navigate("artist/${item.id}")
                                is PlaylistItem -> navController.navigate("online_playlist/${item.id}")
                                is PodcastItem -> navController.navigate("online_podcast/${item.id}")
                                is EpisodeItem -> {
                                    if (!isListenTogetherGuest) {
                                        playerConnection.playQueue(
                                            ListQueue(
                                                title = item.title,
                                                items = listOf(item.toMediaMetadata().toMediaItem()),
                                            ),
                                        )
                                    }
                                }
                            }
                        },
                        onLongClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            menuState.show {
                                when (item) {
                                    is SongItem -> YouTubeSongMenu(
                                        song = item,
                                        navController = navController,
                                        onDismiss = menuState::dismiss,
                                    )
                                    is AlbumItem -> YouTubeAlbumMenu(
                                        albumItem = item,
                                        navController = navController,
                                        onDismiss = menuState::dismiss,
                                    )
                                    is ArtistItem -> YouTubeArtistMenu(
                                        artist = item,
                                        onDismiss = menuState::dismiss,
                                    )
                                    is PlaylistItem -> YouTubePlaylistMenu(
                                        playlist = item,
                                        coroutineScope = scope,
                                        onDismiss = menuState::dismiss,
                                    )
                                    is PodcastItem -> YouTubePlaylistMenu(
                                        playlist = item.asPlaylistItem(),
                                        coroutineScope = scope,
                                        onDismiss = menuState::dismiss,
                                    )
                                    is EpisodeItem -> YouTubeSongMenu(
                                        song = item.asSongItem(),
                                        navController = navController,
                                        onDismiss = menuState::dismiss,
                                    )
                                }
                            }
                        },
                    ),
                )
            }

            LazyColumn(
                state = lazyListState,
                contentPadding = LocalPlayerAwareWindowInsets.current.asPaddingValues(),
                modifier = Modifier.fillMaxSize(),
            ) {
                item(key = "beta_banner") {
                    AnimatedVisibility(
                        visible = !betaBannerDismissed,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically(),
                    ) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.55f),
                            ),
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Beta Version",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onErrorContainer,
                                    )
                                    Text(
                                        text = "You're using an early beta build. Expect bugs, crashes, and missing features. Feedback is appreciated.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onErrorContainer,
                                    )
                                }
                                IconButton(onClick = { betaBannerDismissed = true }) {
                                    Icon(
                                        painter = painterResource(R.drawable.close),
                                        contentDescription = "Dismiss",
                                        tint = MaterialTheme.colorScheme.onErrorContainer,
                                    )
                                }
                            }
                        }
                    }
                }

                if (isLoading) {
                    item(key = "loading_indicator") {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    }
                }

                item(key = "sync_banner") {
                    AnimatedVisibility(
                        visible = isLoggedIn && syncState.overallStatus == SyncStatus.Syncing,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically(),
                    ) {
                        SyncBanner(syncState = syncState)
                    }
                }

                // ── Speed Dial ──────────────────────────────────────────────
                item(key = "speed_dial_title") {
                    NavigationTitle(
                        title = stringResource(R.string.speed_dial),
                        modifier = Modifier.animateItem(),
                    )
                }

                if (speedDialItems.isNotEmpty()) {
                    item(key = "speed_dial_list") {
                        val items = speedDialItems
                        val targetItemSize = 160.dp
                        val columns = (containerWidthDp / targetItemSize).toInt().coerceAtLeast(3)
                        val rows = if (columns >= 6) 1 else if (columns >= 4) 2 else 2
                        val itemsPerPage = columns * rows
                        val peekPadding = 12.dp
                        val itemWidth = (containerWidthDp - peekPadding * 2) / columns

                        val realPageCount = (items.size + 1 + itemsPerPage - 1) / itemsPerPage
                        val virtualPageCount = if (realPageCount > 1) realPageCount * 1000 else 1
                        val initialPage = if (realPageCount > 1) realPageCount * 500 else 0
                        val pagerState = rememberPagerState(
                            initialPage = initialPage,
                            pageCount = { virtualPageCount },
                        )

                        Column(modifier = Modifier.fillMaxWidth().animateItem()) {
                            HorizontalPager(
                                state = pagerState,
                                contentPadding = PaddingValues(horizontal = peekPadding),
                                pageSpacing = 0.dp,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(itemWidth * rows),
                            ) { page ->
                                val realPage = if (realPageCount > 1) page % realPageCount else 0
                                val isFirstPage = realPage == 0
                                val centerIndex = if (rows >= 2 && columns >= 2) columns * 2 - 1 else itemsPerPage - 1

                                val pageStartIndex = if (isFirstPage) 0 else realPage * itemsPerPage - 1
                                val pageItems = items.drop(pageStartIndex).take(if (isFirstPage) itemsPerPage - 1 else itemsPerPage)

                                Column(modifier = Modifier.fillMaxSize()) {
                                    for (row in 0 until rows) {
                                        Row(modifier = Modifier.fillMaxWidth()) {
                                            for (col in 0 until columns) {
                                                val itemIndex = row * columns + col
                                                val isRandomizeSlot = isFirstPage && itemIndex == centerIndex

                                                if (isRandomizeSlot) {
                                                    Box(
                                                        modifier = Modifier
                                                            .width(itemWidth)
                                                            .height(itemWidth)
                                                            .padding(4.dp),
                                                    ) {
                                                        RandomizeGridItem(
                                                            isLoading = isRandomizing,
                                                            onClick = {
                                                                if (isRandomizing) {
                                                                    randomizeJob?.cancel()
                                                                } else if (!isListenTogetherGuest) {
                                                                    randomizeJob = scope.launch {
                                                                        val randomItem = viewModel.getRandomItem()
                                                                        if (randomItem != null) {
                                                                            when (randomItem) {
                                                                                is SongItem -> playerConnection.playQueue(
                                                                                    YouTubeQueue(
                                                                                        randomItem.endpoint ?: WatchEndpoint(videoId = randomItem.id),
                                                                                        randomItem.toMediaMetadata(),
                                                                                    ),
                                                                                )
                                                                                is AlbumItem -> navController.navigate("album/${randomItem.id}")
                                                                                is ArtistItem -> navController.navigate("artist/${randomItem.id}")
                                                                                is PlaylistItem -> navController.navigate("online_playlist/${randomItem.id}")
                                                                                is PodcastItem -> navController.navigate("online_podcast/${randomItem.id}")
                                                                                is EpisodeItem -> playerConnection.playQueue(
                                                                                    ListQueue(
                                                                                        title = randomItem.title,
                                                                                        items = listOf(randomItem.toMediaMetadata().toMediaItem()),
                                                                                    ),
                                                                                )
                                                                            }
                                                                        }
                                                                    }
                                                                }
                                                            },
                                                        )
                                                    }
                                                } else {
                                                    val actualItemIndex = if (isFirstPage && itemIndex > centerIndex) itemIndex - 1 else itemIndex
                                                    if (actualItemIndex < pageItems.size) {
                                                        val sdItem = pageItems[actualItemIndex]
                                                        val isPinned = sdItem.id in pinnedIds
                                                        Box(
                                                            modifier = Modifier
                                                                .width(itemWidth)
                                                                .height(itemWidth)
                                                                .padding(4.dp),
                                                        ) {
                                                            SpeedDialGridItem(
                                                                item = sdItem,
                                                                isPinned = isPinned,
                                                                isActive = sdItem.id in listOf(mediaMetadata?.album?.id, mediaMetadata?.id),
                                                                isPlaying = isPlaying,
                                                                modifier = Modifier
                                                                    .fillMaxSize()
                                                                    .combinedClickable(
                                                                        onClick = {
                                                                            when (sdItem) {
                                                                                is SongItem -> {
                                                                                    if (!isListenTogetherGuest) {
                                                                                        playerConnection.playQueue(
                                                                                            YouTubeQueue(
                                                                                                sdItem.endpoint ?: WatchEndpoint(videoId = sdItem.id),
                                                                                                sdItem.toMediaMetadata(),
                                                                                            ),
                                                                                        )
                                                                                    }
                                                                                }
                                                                                is AlbumItem -> navController.navigate("album/${sdItem.id}")
                                                                                is ArtistItem -> navController.navigate("artist/${sdItem.id}")
                                                                                is PlaylistItem -> {
                                                                                    val rawType = pinnedSpeedDialItems.find { it.id == sdItem.id }?.type
                                                                                    if (rawType == "LOCAL_PLAYLIST") {
                                                                                        navController.navigate("local_playlist/${sdItem.id}")
                                                                                    } else {
                                                                                        navController.navigate("online_playlist/${sdItem.id}")
                                                                                    }
                                                                                }
                                                                                is PodcastItem -> navController.navigate("online_podcast/${sdItem.id}")
                                                                                is EpisodeItem -> {
                                                                                    if (!isListenTogetherGuest) {
                                                                                        playerConnection.playQueue(
                                                                                            ListQueue(
                                                                                                title = sdItem.title,
                                                                                                items = listOf(sdItem.toMediaMetadata().toMediaItem()),
                                                                                            ),
                                                                                        )
                                                                                    }
                                                                                }
                                                                            }
                                                                        },
                                                                        onLongClick = {
                                                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                                            menuState.show {
                                                                                when (sdItem) {
                                                                                    is SongItem -> YouTubeSongMenu(
                                                                                        song = sdItem,
                                                                                        navController = navController,
                                                                                        onDismiss = menuState::dismiss,
                                                                                    )
                                                                                    is AlbumItem -> YouTubeAlbumMenu(
                                                                                        albumItem = sdItem,
                                                                                        navController = navController,
                                                                                        onDismiss = menuState::dismiss,
                                                                                    )
                                                                                    is ArtistItem -> YouTubeArtistMenu(
                                                                                        artist = sdItem,
                                                                                        onDismiss = menuState::dismiss,
                                                                                    )
                                                                                    is PlaylistItem -> YouTubePlaylistMenu(
                                                                                        playlist = sdItem,
                                                                                        coroutineScope = scope,
                                                                                        onDismiss = menuState::dismiss,
                                                                                    )
                                                                                    is PodcastItem -> YouTubePlaylistMenu(
                                                                                        playlist = sdItem.asPlaylistItem(),
                                                                                        coroutineScope = scope,
                                                                                        onDismiss = menuState::dismiss,
                                                                                    )
                                                                                    is EpisodeItem -> YouTubeSongMenu(
                                                                                        song = sdItem.asSongItem(),
                                                                                        navController = navController,
                                                                                        onDismiss = menuState::dismiss,
                                                                                    )
                                                                                }
                                                                            }
                                                                        },
                                                                    ),
                                                            )
                                                        }
                                                    } else {
                                                        Spacer(modifier = Modifier.width(itemWidth))
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            if (realPageCount > 1) {
                                val currentRealPage by remember(realPageCount) {
                                    derivedStateOf { pagerState.currentPage % realPageCount }
                                }
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 8.dp),
                                    horizontalArrangement = Arrangement.Center,
                                ) {
                                    repeat(realPageCount) { index ->
                                        val isSelected = currentRealPage == index
                                        Box(
                                            modifier = Modifier
                                                .padding(horizontal = 3.dp)
                                                .size(if (isSelected) 5.dp else 4.dp)
                                                .clip(CircleShape)
                                                .background(
                                                    if (isSelected) MaterialTheme.colorScheme.primary
                                                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                                                ),
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                quickPicks?.let { qp ->
                    val filteredQp = qp.distinctBy { it.id }.filter { it.id !in speedDialSongIds }
                    if (filteredQp.isNotEmpty()) {
                        item(key = "quick_picks_title") {
                            val title = stringResource(R.string.quick_picks)
                            NavigationTitle(
                                title = title,
                                modifier = Modifier.animateItem(),
                                onPlayAllClick = if (!isListenTogetherGuest) {
                                    { playerConnection.playQueue(ListQueue(title = title, items = filteredQp.map { it.toMediaItem() })) }
                                } else null,
                            )
                        }
                        item(key = "quick_picks_list") {
                            LazyHorizontalGrid(
                                state = quickPicksLazyGridState,
                                rows = GridCells.Fixed(4),
                                flingBehavior = rememberSnapFlingBehavior(quickPicksSnapLayoutInfoProvider),
                                contentPadding = PaddingValues(horizontal = 12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(ListItemHeight * 4)
                                    .animateItem(),
                            ) {
                                items(items = filteredQp, key = { "home_quickpick_${it.id}" }) { song ->
                                    SongListItem(
                                        song = song,
                                        isActive = song.id == mediaMetadata?.id,
                                        isPlaying = isPlaying,
                                        isSwipeable = false,
                                        trailingContent = {
                                            IconButton(onClick = {
                                                menuState.show {
                                                    SongMenu(originalSong = song, navController = navController, onDismiss = menuState::dismiss)
                                                }
                                            }) { Icon(painter = painterResource(R.drawable.more_vert), contentDescription = null) }
                                        },
                                        modifier = Modifier
                                            .width(horizontalLazyGridItemWidth)
                                            .combinedClickable(
                                                onClick = {
                                                    if (!isListenTogetherGuest) {
                                                        if (song.id == mediaMetadata?.id) playerConnection.togglePlayPause()
                                                        else playerConnection.startRadioForSong(song.toMediaMetadata())
                                                    }
                                                },
                                                onLongClick = {
                                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                    menuState.show {
                                                        SongMenu(originalSong = song, navController = navController, onDismiss = menuState::dismiss)
                                                    }
                                                },
                                            ),
                                    )
                                }
                            }
                        }
                    }
                }

                // ── Your Mood (only shown when logged in) ────────────────────
                if (isLoggedIn) {
                    item(key = "your_mood_title") {
                        NavigationTitle(
                            title = "Your Mood",
                            modifier = Modifier.animateItem(),
                        )
                    }

                    item(key = "your_mood_section") {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .animateItem(),
                        ) {
                            if (moodChips.isNotEmpty()) {
                                ChipsRow(
                                    chips = moodChips,
                                    currentValue = selectedMoodCategory,
                                    onValueUpdate = { if (it != null) selectedMoodCategory = it },
                                    chipHeight = 40.dp,
                                    horizontalPadding = 12.dp,
                                    labelStyle = MaterialTheme.typography.labelLarge.copy(fontSize = 14.sp),
                                )
                            }

                            AnimatedContent(
                                targetState = moodPage,
                                transitionSpec = { fadeIn(tween(350)) togetherWith fadeOut(tween(200)) },
                                label = "moodContent",
                            ) { page ->
                                if (page == null) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(100.dp),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(24.dp),
                                            strokeWidth = 2.dp,
                                        )
                                    }
                                } else {
                                    val mixItems = page.sections
                                        .flatMap { it.items }
                                        .filterIsInstance<PlaylistItem>()
                                        .take(10)
                                    if (mixItems.isNotEmpty()) {
                                        LazyRow(
                                            state = moodMixesState,
                                            contentPadding = PaddingValues(horizontal = 12.dp),
                                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                                            modifier = Modifier.padding(top = 10.dp, bottom = 4.dp),
                                        ) {
                                            items(mixItems, key = { it.id }) { mix ->
                                                YouTubeGridItem(
                                                    item = mix,
                                                    isActive = mix.id == mediaMetadata?.album?.id,
                                                    isPlaying = isPlaying,
                                                    coroutineScope = scope,
                                                    thumbnailRatio = 1f,
                                                    size = 135.dp,
                                                    showTitle = true,
                                                    modifier = Modifier
                                                        .animateItem()
                                                        .combinedClickable(
                                                            onClick = {
                                                                navController.navigate("online_playlist/${mix.id}")
                                                            },
                                                            onLongClick = {
                                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                                menuState.show {
                                                                    YouTubePlaylistMenu(
                                                                        playlist = mix,
                                                                        coroutineScope = scope,
                                                                        onDismiss = menuState::dismiss,
                                                                    )
                                                                }
                                                            },
                                                        ),
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // ── Other sections (appear as data arrives, no stagger) ──────
                keepListening?.takeIf { it.isNotEmpty() }?.let { kl ->
                    item(key = "keep_listening_title") {
                        NavigationTitle(title = stringResource(R.string.keep_listening), modifier = Modifier.animateItem())
                    }
                    item(key = "keep_listening_list") {
                        val rows = if (kl.size > 6) 2 else 1
                        LazyHorizontalGrid(
                            state = rememberLazyGridState(),
                            rows = GridCells.Fixed(rows),
                            contentPadding = PaddingValues(horizontal = 12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(currentGridHeight * rows + 56.dp * rows)
                                .animateItem(),
                        ) {
                            items(kl) { localGridItem(it) }
                        }
                    }
                }

                forgottenFavorites?.takeIf { it.isNotEmpty() }?.let { ff ->
                    item(key = "forgotten_favorites_title") {
                        val title = stringResource(R.string.forgotten_favorites)
                        NavigationTitle(
                            title = title,
                            modifier = Modifier.animateItem(),
                            onPlayAllClick = if (!isListenTogetherGuest) {
                                { playerConnection.playQueue(ListQueue(title = title, items = ff.distinctBy { it.id }.map { it.toMediaItem() })) }
                            } else null,
                        )
                    }
                    item(key = "forgotten_favorites_list") {
                        val rows = min(4, ff.size)
                        LazyHorizontalGrid(
                            state = forgottenFavoritesLazyGridState,
                            rows = GridCells.Fixed(rows),
                            contentPadding = PaddingValues(horizontal = 12.dp),
                            flingBehavior = rememberSnapFlingBehavior(forgottenFavoritesSnapLayoutInfoProvider),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(ListItemHeight * rows)
                                .animateItem(),
                        ) {
                            items(items = ff.distinctBy { it.id }, key = { "home_forgotten_${it.id}" }) { song ->
                                SongListItem(
                                    song = song,
                                    isActive = song.id == mediaMetadata?.id,
                                    isPlaying = isPlaying,
                                    isSwipeable = false,
                                    trailingContent = {
                                        IconButton(onClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            menuState.show {
                                                SongMenu(originalSong = song, navController = navController, onDismiss = menuState::dismiss)
                                            }
                                        }) { Icon(painter = painterResource(R.drawable.more_vert), contentDescription = null) }
                                    },
                                    modifier = Modifier
                                        .width(horizontalLazyGridItemWidth)
                                        .combinedClickable(
                                            onClick = {
                                                if (!isListenTogetherGuest) {
                                                    if (song.id == mediaMetadata?.id) playerConnection.togglePlayPause()
                                                    else playerConnection.startRadioForSong(song.toMediaMetadata())
                                                }
                                            },
                                            onLongClick = {
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                menuState.show {
                                                    SongMenu(originalSong = song, navController = navController, onDismiss = menuState::dismiss)
                                                }
                                            },
                                        ),
                                )
                            }
                        }
                    }
                }

                accountPlaylists?.takeIf { it.isNotEmpty() }?.let { apl ->
                    item(key = "account_playlists_title") {
                        NavigationTitle(
                            label = stringResource(R.string.your_youtube_playlists),
                            title = accountName,
                            thumbnail = {
                                if (accountAvatarUrl != null) {
                                    AsyncImage(
                                        model = coil3.request.ImageRequest.Builder(androidx.compose.ui.platform.LocalContext.current)
                                            .data(accountAvatarUrl)
                                            .diskCachePolicy(CachePolicy.ENABLED)
                                            .diskCacheKey(accountAvatarUrl)
                                            .crossfade(false)
                                            .build(),
                                        placeholder = painterResource(R.drawable.person),
                                        error = painterResource(R.drawable.person),
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.size(ListThumbnailSize).clip(CircleShape),
                                    )
                                } else {
                                    Icon(
                                        painter = painterResource(R.drawable.person),
                                        contentDescription = null,
                                        modifier = Modifier.size(ListThumbnailSize),
                                    )
                                }
                            },
                            onClick = { navController.navigate("account") },
                            modifier = Modifier.animateItem(),
                        )
                    }
                    item(key = "account_playlists_list") {
                        LazyRow(contentPadding = PaddingValues(horizontal = 12.dp), modifier = Modifier.animateItem()) {
                            items(items = apl.distinctBy { it.id }, key = { "home_account_playlist_${it.id}" }) { ap ->
                                ytGridItem(ap, null)
                            }
                        }
                    }
                }

                dailyDiscover?.takeIf { it.isNotEmpty() }?.let { discoverList ->
                    item(key = "daily_discover_title") {
                        val title = stringResource(R.string.your_daily_discover)
                        NavigationTitle(
                            title = title,
                            onPlayAllClick = {
                                val items = discoverList.mapNotNull { (it.recommendation as? SongItem)?.toMediaMetadata() }
                                if (items.isNotEmpty()) playerConnection.playQueue(ListQueue(title = title, items = items.map { it.toMediaItem() }))
                            },
                        )
                    }
                    item(key = "daily_discover_content") {
                        Box(
                            modifier = Modifier.fillMaxWidth().height(340.dp).padding(horizontal = 12.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            val carouselState = androidx.compose.material3.carousel.rememberCarouselState { discoverList.size }
                            androidx.compose.material3.carousel.HorizontalMultiBrowseCarousel(
                                state = carouselState,
                                preferredItemWidth = 320.dp,
                                itemSpacing = 16.dp,
                                modifier = Modifier.fillMaxWidth().height(320.dp),
                            ) { i ->
                                val ddItem = discoverList[i]
                                DailyDiscoverCard(
                                    dailyDiscover = ddItem,
                                    onClick = {
                                        if (!isListenTogetherGuest) {
                                            val song = ddItem.recommendation as? SongItem
                                            val meta = song?.toMediaMetadata()
                                            if (meta != null) playerConnection.playQueue(
                                                YouTubeQueue(song.endpoint ?: WatchEndpoint(videoId = song.id), meta)
                                            )
                                        }
                                    },
                                    navController = navController,
                                    modifier = Modifier.maskClip(MaterialTheme.shapes.extraLarge),
                                )
                            }
                        }
                    }
                }

                communityPlaylists?.takeIf { it.isNotEmpty() }?.let { playlists ->
                    item(key = "community_playlists_title") {
                        NavigationTitle(title = stringResource(R.string.from_the_community), modifier = Modifier.animateItem())
                    }
                    item(key = "community_playlists_content") {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.animateItem(),
                        ) {
                            items(playlists) { cpItem ->
                                CommunityPlaylistCard(
                                    item = cpItem,
                                    onClick = { navController.navigate("online_playlist/${cpItem.playlist.id.removePrefix("VL")}") },
                                    onSongClick = { song ->
                                        if (!isListenTogetherGuest) {
                                            playerConnection.playQueue(
                                                YouTubeQueue(song.endpoint ?: WatchEndpoint(videoId = song.id), song.toMediaMetadata())
                                            )
                                        }
                                    },
                                )
                            }
                        }
                    }
                }

                similarRecommendations?.forEachIndexed { index, rec ->
                    item(key = "similar_to_title_$index") {
                        NavigationTitle(
                            label = stringResource(R.string.similar_to),
                            title = rec.title.title,
                            thumbnail = rec.title.thumbnailUrl?.let { thumbUrl ->
                                {
                                    val shape = if (rec.title is Artist) CircleShape else MaterialTheme.shapes.extraLarge
                                    AsyncImage(model = thumbUrl, contentDescription = null, modifier = Modifier.size(ListThumbnailSize).clip(shape))
                                }
                            },
                            onClick = {
                                when (rec.title) {
                                    is Song -> navController.navigate("album/${rec.title.album!!.id}")
                                    is Album -> navController.navigate("album/${rec.title.id}")
                                    is Artist -> navController.navigate("artist/${rec.title.id}")
                                    is Playlist -> {}
                                }
                            },
                            modifier = Modifier.animateItem(),
                        )
                    }
                    item(key = "similar_to_list_$index") {
                        LazyRow(contentPadding = PaddingValues(horizontal = 12.dp), modifier = Modifier.animateItem()) {
                            items(rec.items) { recItem -> ytGridItem(recItem, null) }
                        }
                    }
                }

                homePage?.sections?.forEachIndexed { index, sectionData ->
                    if (sectionData.items.none { it.isMixtape }) {
                        val sectionSongs = sectionData.items.filterIsInstance<SongItem>()
                        val hasPlayableSongs = sectionSongs.isNotEmpty()
                        val isSongsOnly = sectionData.items.isNotEmpty() && sectionData.items.all { it is SongItem }

                        item(key = "home_section_title_$index") {
                            NavigationTitle(
                                title = sectionData.title,
                                label = sectionData.label,
                                thumbnail = sectionData.thumbnail?.let { thumbUrl ->
                                    {
                                        val shape = if (sectionData.endpoint?.isArtistEndpoint == true) CircleShape
                                        else MaterialTheme.shapes.extraLarge
                                        AsyncImage(model = thumbUrl, contentDescription = null, modifier = Modifier.size(ListThumbnailSize).clip(shape))
                                    }
                                },
                                onClick = sectionData.endpoint?.let { ep ->
                                    {
                                        when {
                                            ep.browseId == "FEmusic_moods_and_genres" -> navController.navigate("mood_and_genres")
                                            ep.params != null -> navController.navigate("youtube_browse/${ep.browseId}?params=${ep.params}")
                                            else -> navController.navigate("browse/${ep.browseId}")
                                        }
                                    }
                                },
                                onPlayAllClick = if (hasPlayableSongs && !isListenTogetherGuest) {
                                    {
                                        playerConnection.playQueue(
                                            ListQueue(title = sectionData.title, items = sectionSongs.map { it.toMediaMetadata().toMediaItem() })
                                        )
                                    }
                                } else null,
                                modifier = Modifier.animateItem(),
                            )
                        }

                        if (isSongsOnly) {
                            item(key = "home_section_list_$index") {
                                LazyHorizontalGrid(
                                    state = rememberLazyGridState(),
                                    rows = GridCells.Fixed(4),
                                    contentPadding = PaddingValues(horizontal = 12.dp),
                                    modifier = Modifier.fillMaxWidth().height(ListItemHeight * 4).animateItem(),
                                ) {
                                    items(items = sectionSongs.distinctBy { it.id }, key = { "home_section_${index}_song_${it.id}" }) { song ->
                                        YouTubeListItem(
                                            item = song,
                                            isActive = song.id == mediaMetadata?.id,
                                            isPlaying = isPlaying,
                                            isSwipeable = false,
                                            trailingContent = {
                                                IconButton(onClick = {
                                                    menuState.show {
                                                        YouTubeSongMenu(song = song, navController = navController, onDismiss = menuState::dismiss)
                                                    }
                                                }) { Icon(painter = painterResource(R.drawable.more_vert), contentDescription = null) }
                                            },
                                            modifier = Modifier
                                                .width(horizontalLazyGridItemWidth)
                                                .combinedClickable(
                                                    onClick = {
                                                        if (!isListenTogetherGuest) playerConnection.playQueue(
                                                            YouTubeQueue(song.endpoint ?: WatchEndpoint(videoId = song.id), song.toMediaMetadata())
                                                        )
                                                    },
                                                    onLongClick = {
                                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                        menuState.show {
                                                            YouTubeSongMenu(song = song, navController = navController, onDismiss = menuState::dismiss)
                                                        }
                                                    },
                                                ),
                                        )
                                    }
                                }
                            }
                        } else {
                            item(key = "home_section_list_$index") {
                                LazyRow(contentPadding = PaddingValues(horizontal = 12.dp), modifier = Modifier.animateItem()) {
                                    items(items = sectionData.items.distinctBy { it.id }, key = { "home_section_${index}_item_${it.id}" }) { secItem ->
                                        ytGridItem(secItem, null)
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
