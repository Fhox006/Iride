/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyListScope
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
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import androidx.compose.ui.util.lerp as lerpFloat
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
import com.metrolist.music.BuildConfig
import com.metrolist.music.constants.BetaBannerDismissedVersionKey
import com.metrolist.music.constants.GridItemSize
import com.metrolist.music.constants.GridItemsSizeKey
import com.metrolist.music.constants.GridThumbnailHeight
import com.metrolist.music.constants.HideExplicitKey
import com.metrolist.music.constants.MainTopGradientKey
import com.metrolist.music.constants.HideVideoSongsKey
import com.metrolist.music.constants.HideYoutubeShortsKey
import com.metrolist.music.constants.AccountNameKey
import com.metrolist.music.constants.AccountPhotoUrlKey
import com.metrolist.music.constants.InnerTubeCookieKey
import com.metrolist.music.constants.ListItemHeight
import com.metrolist.music.constants.ListThumbnailSize
import com.metrolist.music.constants.SmallGridThumbnailHeight
import com.metrolist.music.constants.TopNavigationBarKey
import com.metrolist.music.db.entities.Album
import com.metrolist.music.db.entities.Artist
import com.metrolist.music.db.entities.LocalItem
import com.metrolist.music.db.entities.Playlist
import com.metrolist.music.db.entities.Song
import com.metrolist.music.extensions.toMediaItem
import com.metrolist.music.models.MediaMetadata
import com.metrolist.music.models.toMediaMetadata
import com.metrolist.music.playback.queues.ListQueue
import com.metrolist.music.playback.queues.YouTubeQueue
import com.metrolist.music.ui.component.AlbumGridItem
import com.metrolist.music.ui.component.VinylPeekFraction
import com.metrolist.music.ui.component.ArtistGridItem
import com.metrolist.music.ui.component.ChipsRow
import com.metrolist.music.ui.component.LocalMenuState
import com.metrolist.music.ui.component.NavigationTitle
import com.metrolist.music.ui.component.TopNavigationBar
import com.metrolist.music.LocalTopNavBarController
import com.metrolist.music.ui.component.RandomizeGridItem
import com.metrolist.music.ui.component.PlaylistGridItem
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.min
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.platform.LocalContext
import coil3.request.ImageRequest
import com.metrolist.innertube.YouTube
import com.metrolist.innertube.utils.completed
import com.metrolist.music.LocalDatabase
import com.metrolist.music.db.entities.PlaylistEntity
import com.metrolist.music.models.HeroCarouselItem
import com.metrolist.music.models.DischiPerTeItem
import com.metrolist.music.db.entities.PlaylistSongMap
import com.metrolist.music.utils.SyncState
import com.metrolist.music.viewmodels.CommunityPlaylistItem
import kotlin.random.Random

private val HomeLargeTitleHeightDp = 80.dp
private val HomeSmallTitleBarHeightDp = 56.dp

// New Iride UI: collapses a section's body while keeping its NavigationTitle row visible.
@Composable
private fun IrideCollapsibleSection(
    collapsed: Boolean,
    content: @Composable () -> Unit,
) {
    AnimatedVisibility(
        visible = !collapsed,
        enter = expandVertically(animationSpec = tween(220)) + fadeIn(tween(180)),
        exit = shrinkVertically(animationSpec = tween(220)) + fadeOut(tween(140)),
    ) {
        content()
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class,
    androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun HomeScreen(
    navController: NavController,
    @Suppress("UNUSED_PARAMETER") snackbarHostState: SnackbarHostState,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val menuState = LocalMenuState.current
    val playerConnection = LocalPlayerConnection.current
    val database = LocalDatabase.current
    val haptic = LocalHapticFeedback.current
    val listenTogetherManager = LocalListenTogetherManager.current
    val isListenTogetherGuest = listenTogetherManager?.let { it.isInRoom && !it.isHost } ?: false

    val isPlaying by remember(playerConnection) {
        playerConnection?.isEffectivelyPlaying ?: MutableStateFlow(false)
    }.collectAsStateWithLifecycle()
    val mediaMetadata by remember(playerConnection) {
        playerConnection?.mediaMetadata ?: MutableStateFlow<MediaMetadata?>(null)
    }.collectAsStateWithLifecycle()

    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    var betaBannerDismissedVersion by rememberPreference(BetaBannerDismissedVersionKey, "")

    val speedDialItems by viewModel.speedDialItems.collectAsStateWithLifecycle()
    val pinnedSpeedDialItems by viewModel.pinnedSpeedDialItems.collectAsStateWithLifecycle()
    val pinnedIds: Set<String> by remember(pinnedSpeedDialItems) {
        derivedStateOf { pinnedSpeedDialItems.map { it.id }.toSet() }
    }
    val isRandomizing by viewModel.isRandomizing.collectAsStateWithLifecycle()
    val regeneratingSections by viewModel.regeneratingSections.collectAsStateWithLifecycle()

    val quickPicks by viewModel.quickPicks.collectAsStateWithLifecycle()
    val keepListening by viewModel.keepListening.collectAsStateWithLifecycle()
    val forgottenFavorites by viewModel.forgottenFavorites.collectAsStateWithLifecycle()
    val communityPlaylists by viewModel.communityPlaylists.collectAsStateWithLifecycle()
    val dailyDiscover by viewModel.dailyDiscover.collectAsStateWithLifecycle()
    val accountPlaylists by viewModel.accountPlaylists.collectAsStateWithLifecycle()
    val similarRecommendations by viewModel.similarRecommendations.collectAsStateWithLifecycle()
    val dischiPerTe by viewModel.dischiPerTe.collectAsStateWithLifecycle()
    val homePage by viewModel.homePage.collectAsStateWithLifecycle()
    val phase1Complete by viewModel.phase1Complete.collectAsStateWithLifecycle()
    val isHeroCarouselEnabled by viewModel.isHeroCarouselEnabled.collectAsStateWithLifecycle()
    val heroCarouselItems by viewModel.heroCarouselItems.collectAsStateWithLifecycle()

    val accountNameFlow by viewModel.accountName.collectAsStateWithLifecycle()
    val accountImageUrlFlow by viewModel.accountImageUrl.collectAsStateWithLifecycle()
    val syncState by viewModel.syncState.collectAsStateWithLifecycle()
    val syncBannerLaunchCount by viewModel.syncBannerLaunchCount.collectAsStateWithLifecycle()
    val innerTubeCookie by rememberPreference(InnerTubeCookieKey, "")
    val isLoggedIn = remember(innerTubeCookie) { "SAPISID" in parseCookieString(innerTubeCookie) }
    val accountNamePref by rememberPreference(AccountNameKey, "")
    val accountPhotoUrlPref by rememberPreference(AccountPhotoUrlKey, "")
    val accountName = if (accountNameFlow != "Guest") accountNameFlow else accountNamePref
    val accountImageUrl: String? = accountImageUrlFlow ?: accountPhotoUrlPref.takeIf { it.isNotEmpty() }
    val accountAvatarUrl = if (isLoggedIn) accountImageUrl else null

    val mainTopGradient by rememberPreference(MainTopGradientKey, defaultValue = false)
    val topNavigationBarEnabled by rememberPreference(TopNavigationBarKey, defaultValue = false)
    val topNavBarController = LocalTopNavBarController.current
    // New Iride UI: sections start flush with the "Home" label in TopNavigationBar (20dp),
    // instead of the classic UI's 12dp.
    val irideStart = if (topNavigationBarEnabled) 20.dp else 12.dp
    // The shared ListItem/GridItem components (SongListItem, YouTubeGridItem, etc.) each carry
    // their own built-in horizontal inset on top of whatever contentPadding their row/grid gets —
    // 12dp row padding + 4dp thumbnail box for ListItem-based items, and (in New Iride UI) 4dp
    // column padding for GridItem-based items — see GridItem's own `hPad` in Items.kt, which is
    // 4dp when the New Iride UI top nav is enabled, 8dp otherwise. That's on purpose (it's also
    // what creates the gap between adjacent items in these horizontal carousels) but it means
    // using irideStart as contentPadding here pushes the item's actual visual thumbnail further
    // right than the title text above it. Subtracting the item's own inset keeps the first
    // thumbnail flush with the title without touching those shared components (used everywhere,
    // not just this screen) or losing the item-to-item spacing. This previously subtracted the
    // classic UI's 8dp GridItem inset even in New Iride UI mode (where it's actually 4dp),
    // leaving grid covers sitting 4dp left of the title text above them.
    val irideListItemStart = if (topNavigationBarEnabled) 4.dp else irideStart
    val irideGridItemStart = if (topNavigationBarEnabled) 16.dp else irideStart
    // New Iride UI: thumbnails in NavigationTitle (e.g. "Similar to <artist>") shrink to sit
    // inline with the label/title text stack instead of towering over it.
    val irideTitleThumbSize = if (topNavigationBarEnabled) 22.dp else ListThumbnailSize
    val hideExplicit by rememberPreference(HideExplicitKey, defaultValue = false)
    val hideVideoSongs by rememberPreference(HideVideoSongsKey, defaultValue = false)
    val hideYoutubeShorts by rememberPreference(HideYoutubeShortsKey, defaultValue = false)
    val gridItemSize by rememberEnumPreference(GridItemsSizeKey, GridItemSize.BIG)
    val currentGridHeight = if (gridItemSize == GridItemSize.BIG) GridThumbnailHeight else SmallGridThumbnailHeight

    // Your Mood
    val moodMixItems by viewModel.moodMixItems.collectAsStateWithLifecycle()
    val isMoodLoading by viewModel.isMoodLoading.collectAsStateWithLifecycle()
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

    // New Iride UI: per-section collapse state (session-only, not persisted).
    val collapsedSections = remember { androidx.compose.runtime.mutableStateMapOf<String, Boolean>() }
    fun isSectionCollapsed(key: String) = collapsedSections[key] == true
    fun toggleSection(key: String) { collapsedSections[key] = !isSectionCollapsed(key) }

    val quickPicksLazyGridState = rememberLazyGridState()
    val forgottenFavoritesLazyGridState = rememberLazyGridState()
    LaunchedEffect(quickPicks) {
        if (quickPicksLazyGridState.firstVisibleItemIndex != 0) quickPicksLazyGridState.scrollToItem(0)
    }
    LaunchedEffect(forgottenFavorites) {
        if (forgottenFavoritesLazyGridState.firstVisibleItemIndex != 0) forgottenFavoritesLazyGridState.scrollToItem(0)
    }

    val scope = rememberCoroutineScope()
    var randomizeJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }
    val lazyListState = rememberLazyListState()

    // "Dischi scelti per te" doesn't always sit right below the mood pills — most of the
    // time it does, but every so often it lands further down the feed instead, so the
    // layout doesn't feel too predictable. Picked once per screen visit.
    val dischiPerTePosition = remember {
        if (Random.nextInt(100) < 55) {
            "after_mood"
        } else {
            listOf("after_keep_listening", "after_forgotten_favorites", "after_daily_discover", "after_community").random()
        }
    }

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
        viewModel.onSectionBecameVisible("dischi_per_te")
    }

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(
        snapAnimationSpec = tween(durationMillis = 200),
    )

    Scaffold(
        topBar = {
            if (!topNavigationBarEnabled) {
                HomeCollapsingHeader(
                    scrollBehavior = scrollBehavior,
                    accountImageUrl = accountAvatarUrl,
                    onAccountClick = { navController.navigate("settings") },
                    transparentBackground = mainTopGradient,
                )
            }
        },
        // Only wire up the collapsing-header nested scroll when the header is actually rendered.
        // When the New Iride UI top nav bar is enabled, HomeCollapsingHeader never composes, so its
        // SideEffect never bounds scrollBehavior.state.heightOffsetLimit — it stays at Compose's
        // default (-Float.MAX_VALUE), and exitUntilCollapsedScrollBehavior's onPreScroll then
        // consumes every upward scroll gesture trying to collapse a header that isn't there,
        // leaving nothing for the LazyColumn below. This is why the list appeared stuck/unscrollable
        // only in New UI mode, while LibraryMixScreen (whose header always renders) never had it.
        modifier = if (!topNavigationBarEnabled) {
            Modifier.nestedScroll(scrollBehavior.nestedScrollConnection)
        } else {
            Modifier
        },
        containerColor = if (mainTopGradient) Color.Transparent else MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0),
    ) { paddingValues ->
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
                                            playerConnection?.togglePlayPause()
                                        } else {
                                            playerConnection?.startRadioForSong(item.toMediaMetadata())
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

            val ytGridItem: @Composable (YTItem, androidx.compose.ui.unit.Dp?, Boolean) -> Unit = { item, sizeOverride, dischiPerTeStyle ->
                val size = sizeOverride ?: if (item.isMixtape) 180.dp else currentGridHeight
                YouTubeGridItem(
                    item = item,
                    isActive = item.id in listOf(mediaMetadata?.album?.id, mediaMetadata?.id),
                    isPlaying = isPlaying,
                    coroutineScope = scope,
                    thumbnailRatio = 1f,
                    showPlayButton = !dischiPerTeStyle,
                    showVinylEffect = dischiPerTeStyle,
                    size = size,
                    modifier = Modifier.combinedClickable(
                        onClick = {
                            when (item) {
                                is SongItem -> {
                                    if (!isListenTogetherGuest) {
                                        playerConnection?.playQueue(
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
                                        playerConnection?.playQueue(
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

            val dischiPerTeGridItem: @Composable (DischiPerTeItem) -> Unit = { discItem ->
                when (discItem) {
                    is DischiPerTeItem.Local -> {
                        val album = discItem.album
                        AlbumGridItem(
                            album = album,
                            isActive = album.id == mediaMetadata?.album?.id,
                            isPlaying = isPlaying,
                            coroutineScope = scope,
                            size = currentGridHeight,
                            showPlayButton = !topNavigationBarEnabled,
                            showVinylEffect = topNavigationBarEnabled,
                            modifier = Modifier.combinedClickable(
                                onClick = { navController.navigate("album/${album.id}") },
                                onLongClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    menuState.show {
                                        AlbumMenu(originalAlbum = album, navController = navController, onDismiss = menuState::dismiss)
                                    }
                                },
                            ),
                        )
                    }
                    is DischiPerTeItem.Remote -> ytGridItem(discItem.item, null, topNavigationBarEnabled)
                }
            }

            val dischiPerTeSection: LazyListScope.() -> Unit = {
                dischiPerTe?.takeIf { it.isNotEmpty() }?.let { discs ->
                    item(key = "dischi_per_te_title") {
                        NavigationTitle(
                            title = stringResource(R.string.discs_for_you),
                            modifier = Modifier.animateItem(),
                            onRefreshClick = { viewModel.regenerateDischiPerTe() },
                            isRefreshing = "dischi_per_te" in regeneratingSections,
                            useIrideStyle = topNavigationBarEnabled,
                            collapsed = isSectionCollapsed("dischi_per_te"),
                            onCollapseToggle = { toggleSection("dischi_per_te") },
                        )
                    }
                    item(key = "dischi_per_te_list") {
                        IrideCollapsibleSection(collapsed = isSectionCollapsed("dischi_per_te")) {
                            // Extra gap reserves room for the vinyl disc peeking out of each
                            // album cover (New Iride UI only) so it never overlaps the next card.
                            val dischiPerTeSpacing = if (topNavigationBarEnabled) currentGridHeight * VinylPeekFraction else 0.dp
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = irideGridItemStart),
                                horizontalArrangement = Arrangement.spacedBy(dischiPerTeSpacing),
                                modifier = Modifier.animateItem(),
                            ) {
                                items(discs, key = { "dischi_per_te_${it.id}" }) { discItem -> dischiPerTeGridItem(discItem) }
                            }
                        }
                    }
                }
            }

            LazyColumn(
                state = lazyListState,
                contentPadding = PaddingValues(
                    top = paddingValues.calculateTopPadding(),
                    bottom = LocalPlayerAwareWindowInsets.current.asPaddingValues().calculateBottomPadding(),
                ),
                modifier = Modifier.fillMaxSize(),
            ) {
                if (topNavigationBarEnabled && topNavBarController != null) {
                    item(key = "top_nav_bar") {
                        TopNavigationBar(
                            navigationItems = topNavBarController.navigationItems,
                            currentRoute = topNavBarController.currentRoute,
                            onItemClick = topNavBarController.onItemClick,
                            modifier = Modifier.animateItem(),
                            containerColor = if (mainTopGradient) Color.Transparent else MaterialTheme.colorScheme.background,
                        )
                    }
                }
                item(key = "beta_banner") {
                    AnimatedVisibility(
                        visible = betaBannerDismissedVersion != BuildConfig.VERSION_NAME,
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
                                IconButton(onClick = { betaBannerDismissedVersion = BuildConfig.VERSION_NAME }) {
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
                        visible = isLoggedIn && syncState.overallStatus == SyncStatus.Syncing && syncBannerLaunchCount < 3,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically(),
                    ) {
                        SyncBanner(syncState = syncState)
                    }
                }

                // ── Hero Carousel ───────────────────────────────────────────
                if (isHeroCarouselEnabled && heroCarouselItems.isNotEmpty()) {
                    item(key = "hero_carousel") {
                        HeroCarouselSection(
                            items = heroCarouselItems,
                            onNewReleaseClick = { albumId -> navController.navigate("album/$albumId") },
                            onForYouClick = { playlistId, isLocal ->
                                when {
                                    isLocal && playlistId == PlaylistEntity.LIKED_PLAYLIST_ID ->
                                        navController.navigate("auto_playlist/liked")
                                    isLocal -> navController.navigate("local_playlist/$playlistId")
                                    else -> navController.navigate("online_playlist/$playlistId")
                                }
                            },
                            onMoodClick = { playlistId -> navController.navigate("online_playlist/$playlistId") },
                            onArtistClick = { artistId -> navController.navigate("artist/$artistId") },
                            onArtistRadioClick = { artistId, _ ->
                                scope.launch(Dispatchers.IO) {
                                    val endpoint = viewModel.fetchArtistRadioEndpoint(artistId)
                                    withContext(Dispatchers.Main) {
                                        if (endpoint != null) {
                                            playerConnection?.playQueue(YouTubeQueue(endpoint))
                                        } else {
                                            navController.navigate("artist/$artistId")
                                        }
                                    }
                                }
                                Unit
                            },
                            modifier = Modifier.animateItem(),
                        )
                    }
                }

                // ── Speed Dial ──────────────────────────────────────────────
                // When the Hero Carousel leads the screen, push Speed Dial a few
                // blocks down instead of stacking two big carousels back to back.
                val deferSpeedDialToBottom = isHeroCarouselEnabled && heroCarouselItems.isNotEmpty()
                val speedDialContent: @Composable LazyItemScope.() -> Unit = {
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
                                                                                is SongItem -> playerConnection?.playQueue(
                                                                                    YouTubeQueue(
                                                                                        randomItem.endpoint ?: WatchEndpoint(videoId = randomItem.id),
                                                                                        randomItem.toMediaMetadata(),
                                                                                    ),
                                                                                )
                                                                                is AlbumItem -> navController.navigate("album/${randomItem.id}")
                                                                                is ArtistItem -> navController.navigate("artist/${randomItem.id}")
                                                                                is PlaylistItem -> navController.navigate("online_playlist/${randomItem.id}")
                                                                                is PodcastItem -> navController.navigate("online_podcast/${randomItem.id}")
                                                                                is EpisodeItem -> playerConnection?.playQueue(
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
                                                                                        playerConnection?.playQueue(
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
                                                                                        playerConnection?.playQueue(
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

                if (speedDialItems.isNotEmpty() && !deferSpeedDialToBottom && !topNavigationBarEnabled) {
                    item(key = "speed_dial_list") {
                        speedDialContent()
                    }
                }

                quickPicks?.let { qp ->
                    val filteredQp = qp.distinctBy { it.id }
                    if (filteredQp.isNotEmpty()) {
                        item(key = "quick_picks_title") {
                            LaunchedEffect(filteredQp) {
                                playerConnection?.prefetchStreamUrls(filteredQp.take(6).map { it.id })
                            }
                            val title = stringResource(R.string.quick_picks)
                            NavigationTitle(
                                title = title,
                                modifier = Modifier.animateItem(),
                                onPlayAllClick = if (!isListenTogetherGuest) {
                                    { playerConnection?.playQueue(ListQueue(title = title, items = filteredQp.map { it.toMediaItem() })) }
                                } else null,
                                onRefreshClick = { viewModel.regenerateQuickPicks() },
                                isRefreshing = "quick_picks" in regeneratingSections,
                                useIrideStyle = topNavigationBarEnabled,
                                collapsed = isSectionCollapsed("quick_picks"),
                                onCollapseToggle = { toggleSection("quick_picks") },
                            )
                        }
                        item(key = "quick_picks_list") {
                            IrideCollapsibleSection(collapsed = isSectionCollapsed("quick_picks")) {
                                LazyHorizontalGrid(
                                    state = quickPicksLazyGridState,
                                    rows = GridCells.Fixed(4),
                                    flingBehavior = rememberSnapFlingBehavior(quickPicksSnapLayoutInfoProvider),
                                    contentPadding = PaddingValues(horizontal = irideListItemStart),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(ListItemHeight * 4)
                                        .animateItem(),
                                ) {
                                    items(items = filteredQp, key = { "home_quickpick_${it.id}" }) { originalSong ->
                                        val song by database.song(originalSong.id).collectAsStateWithLifecycle(initialValue = originalSong)
                                        val currentSong = song ?: originalSong
                                        SongListItem(
                                            song = currentSong,
                                            isActive = currentSong.id == mediaMetadata?.id,
                                            isPlaying = isPlaying,
                                            isSwipeable = false,
                                            trailingContent = {
                                                IconButton(onClick = {
                                                    menuState.show {
                                                        SongMenu(originalSong = currentSong, navController = navController, onDismiss = menuState::dismiss)
                                                    }
                                                }) { Icon(painter = painterResource(R.drawable.more_vert), contentDescription = null) }
                                            },
                                            modifier = Modifier
                                                .width(horizontalLazyGridItemWidth)
                                                .combinedClickable(
                                                    onClick = {
                                                        if (!isListenTogetherGuest) {
                                                            if (currentSong.id == mediaMetadata?.id) playerConnection?.togglePlayPause()
                                                            else playerConnection?.playQueue(
                                                                YouTubeQueue(
                                                                    WatchEndpoint(videoId = currentSong.id),
                                                                    currentSong.toMediaMetadata(),
                                                                )
                                                            )
                                                        }
                                                    },
                                                    onLongClick = {
                                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                        menuState.show {
                                                            SongMenu(originalSong = currentSong, navController = navController, onDismiss = menuState::dismiss)
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

                // ── Your Mood (only shown when logged in) ────────────────────
                if (isLoggedIn) {
                    item(key = "your_mood_title") {
                        NavigationTitle(
                            title = "Mood & Playlists for You",
                            modifier = Modifier.animateItem(),
                            useIrideStyle = topNavigationBarEnabled,
                            collapsed = isSectionCollapsed("your_mood"),
                            onCollapseToggle = { toggleSection("your_mood") },
                        )
                    }

                    item(key = "your_mood_section") {
                      IrideCollapsibleSection(collapsed = isSectionCollapsed("your_mood")) {
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
                                    horizontalPadding = irideStart,
                                    labelStyle = MaterialTheme.typography.labelLarge.copy(fontSize = 14.sp),
                                    useIrideStyle = topNavigationBarEnabled,
                                )
                            }

                            // Fixed height so switching chips never resizes the row mid-fade —
                            // the old mixes stay put (dimmed) with a small corner spinner until
                            // the new ones are ready, instead of the whole thing flashing blank.
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(159.dp),
                            ) {
                                val mixItems = moodMixItems
                                if (mixItems.isNullOrEmpty()) {
                                    Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(24.dp),
                                            strokeWidth = 2.dp,
                                            color = if (topNavigationBarEnabled) Color.White.copy(alpha = 0.6f) else ProgressIndicatorDefaults.circularColor,
                                        )
                                    }
                                } else {
                                    LazyRow(
                                        state = moodMixesState,
                                        contentPadding = PaddingValues(horizontal = irideGridItemStart),
                                        horizontalArrangement = Arrangement.spacedBy(if (topNavigationBarEnabled) 4.dp else 12.dp),
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(top = 10.dp, bottom = 4.dp)
                                            .alpha(if (isMoodLoading) 0.4f else 1f),
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
                                                        enabled = !isMoodLoading,
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
                                    androidx.compose.animation.AnimatedVisibility(
                                        visible = isMoodLoading,
                                        modifier = Modifier.align(Alignment.Center),
                                        enter = fadeIn(),
                                        exit = fadeOut(),
                                    ) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(24.dp),
                                            strokeWidth = 2.dp,
                                            color = if (topNavigationBarEnabled) Color.White.copy(alpha = 0.6f) else ProgressIndicatorDefaults.circularColor,
                                        )
                                    }
                                }
                            }
                        }
                      }
                    }
                }

                if (dischiPerTePosition == "after_mood") dischiPerTeSection()

                // ── Other sections (appear as data arrives, no stagger) ──────
                keepListening?.takeIf { it.isNotEmpty() }?.let { kl ->
                    item(key = "keep_listening_title") {
                        NavigationTitle(
                            title = stringResource(R.string.keep_listening),
                            modifier = Modifier.animateItem(),
                            useIrideStyle = topNavigationBarEnabled,
                            collapsed = isSectionCollapsed("keep_listening"),
                            onCollapseToggle = { toggleSection("keep_listening") },
                        )
                    }
                    item(key = "keep_listening_list") {
                        IrideCollapsibleSection(collapsed = isSectionCollapsed("keep_listening")) {
                            val rows = if (kl.size > 6) 2 else 1
                            LazyHorizontalGrid(
                                state = rememberLazyGridState(),
                                rows = GridCells.Fixed(rows),
                                contentPadding = PaddingValues(horizontal = irideGridItemStart),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(currentGridHeight * rows + 56.dp * rows)
                                    .animateItem(),
                            ) {
                                items(kl) { localGridItem(it) }
                            }
                        }
                    }
                }

                if (dischiPerTePosition == "after_keep_listening") dischiPerTeSection()

                if (speedDialItems.isNotEmpty() && deferSpeedDialToBottom && !topNavigationBarEnabled) {
                    item(key = "speed_dial_list") {
                        speedDialContent()
                    }
                }

                forgottenFavorites?.takeIf { it.isNotEmpty() }?.let { ff ->
                    item(key = "forgotten_favorites_title") {
                        val title = stringResource(R.string.forgotten_favorites)
                        NavigationTitle(
                            title = title,
                            modifier = Modifier.animateItem(),
                            onPlayAllClick = if (!isListenTogetherGuest) {
                                { playerConnection?.playQueue(ListQueue(title = title, items = ff.distinctBy { it.id }.map { it.toMediaItem() })) }
                            } else null,
                            useIrideStyle = topNavigationBarEnabled,
                            collapsed = isSectionCollapsed("forgotten_favorites"),
                            onCollapseToggle = { toggleSection("forgotten_favorites") },
                        )
                    }
                    item(key = "forgotten_favorites_list") {
                        IrideCollapsibleSection(collapsed = isSectionCollapsed("forgotten_favorites")) {
                            val rows = min(4, ff.size)
                            LazyHorizontalGrid(
                                state = forgottenFavoritesLazyGridState,
                                rows = GridCells.Fixed(rows),
                                contentPadding = PaddingValues(horizontal = irideListItemStart),
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
                                                        if (song.id == mediaMetadata?.id) playerConnection?.togglePlayPause()
                                                        else playerConnection?.startRadioForSong(song.toMediaMetadata())
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

                if (dischiPerTePosition == "after_forgotten_favorites") dischiPerTeSection()

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
                                        modifier = Modifier.size(irideTitleThumbSize).clip(CircleShape),
                                    )
                                } else {
                                    Icon(
                                        painter = painterResource(R.drawable.person),
                                        contentDescription = null,
                                        modifier = Modifier.size(irideTitleThumbSize),
                                    )
                                }
                            },
                            onClick = { navController.navigate("account") },
                            modifier = Modifier.animateItem(),
                            useIrideStyle = topNavigationBarEnabled,
                            collapsed = isSectionCollapsed("account_playlists"),
                            onCollapseToggle = { toggleSection("account_playlists") },
                        )
                    }
                    item(key = "account_playlists_list") {
                        IrideCollapsibleSection(collapsed = isSectionCollapsed("account_playlists")) {
                            LazyRow(contentPadding = PaddingValues(horizontal = irideGridItemStart), modifier = Modifier.animateItem()) {
                                items(items = apl.distinctBy { it.id }, key = { "home_account_playlist_${it.id}" }) { ap ->
                                    ytGridItem(ap, null, false)
                                }
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
                                if (items.isNotEmpty()) playerConnection?.playQueue(ListQueue(title = title, items = items.map { it.toMediaItem() }))
                            },
                            onRefreshClick = { viewModel.regenerateDailyDiscover() },
                            isRefreshing = "daily_discover" in regeneratingSections,
                            useIrideStyle = topNavigationBarEnabled,
                            collapsed = isSectionCollapsed("daily_discover"),
                            onCollapseToggle = { toggleSection("daily_discover") },
                        )
                    }
                    item(key = "daily_discover_content") {
                        IrideCollapsibleSection(collapsed = isSectionCollapsed("daily_discover")) {
                            Box(
                                modifier = Modifier.fillMaxWidth().height(340.dp).padding(horizontal = irideStart),
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
                                                if (meta != null) playerConnection?.playQueue(
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
                }

                if (dischiPerTePosition == "after_daily_discover") dischiPerTeSection()

                communityPlaylists?.takeIf { it.isNotEmpty() }?.let { playlists ->
                    item(key = "community_playlists_title") {
                        NavigationTitle(
                            title = stringResource(R.string.from_the_community),
                            modifier = Modifier.animateItem(),
                            onRefreshClick = { viewModel.regenerateCommunityPlaylists() },
                            isRefreshing = "community_playlists" in regeneratingSections,
                            useIrideStyle = topNavigationBarEnabled,
                            collapsed = isSectionCollapsed("community_playlists"),
                            onCollapseToggle = { toggleSection("community_playlists") },
                        )
                    }
                    item(key = "community_playlists_content") {
                        IrideCollapsibleSection(collapsed = isSectionCollapsed("community_playlists")) {
                            val chunked = playlists.chunked(2)
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = irideGridItemStart)
                                    .animateItem(),
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                chunked.forEach { rowItems ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    ) {
                                        rowItems.forEach { cpItem ->
                                            PlaylistGridItem(
                                                playlist = Playlist(
                                                    playlist = PlaylistEntity(
                                                        id = cpItem.playlist.id.removePrefix("VL"),
                                                        name = cpItem.playlist.title,
                                                        browseId = cpItem.playlist.id.removePrefix("VL"),
                                                        thumbnailUrl = cpItem.playlist.thumbnail,
                                                    ),
                                                    songCount = 0,
                                                    songThumbnails = emptyList(),
                                                ),
                                                fillMaxWidth = true,
                                                autoPlaylist = false,
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .combinedClickable(
                                                        onClick = {
                                                            navController.navigate("online_playlist/${cpItem.playlist.id.removePrefix("VL")}")
                                                        },
                                                    ),
                                            )
                                        }
                                        if (rowItems.size == 1) {
                                            Spacer(modifier = Modifier.weight(1f))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                if (dischiPerTePosition == "after_community") dischiPerTeSection()

                similarRecommendations?.forEachIndexed { index, rec ->
                    item(key = "similar_to_title_$index") {
                        NavigationTitle(
                            label = stringResource(R.string.similar_to),
                            title = rec.title.title,
                            thumbnail = rec.title.thumbnailUrl?.let { thumbUrl ->
                                {
                                    val shape = if (rec.title is Artist) CircleShape else MaterialTheme.shapes.extraLarge
                                    AsyncImage(model = thumbUrl, contentDescription = null, modifier = Modifier.size(irideTitleThumbSize).clip(shape))
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
                            onRefreshClick = { viewModel.regenerateSimilarRecommendations() },
                            isRefreshing = "similar_recommendations" in regeneratingSections,
                            modifier = Modifier.animateItem(),
                            useIrideStyle = topNavigationBarEnabled,
                            collapsed = isSectionCollapsed("similar_to_$index"),
                            onCollapseToggle = { toggleSection("similar_to_$index") },
                        )
                    }
                    item(key = "similar_to_list_$index") {
                        IrideCollapsibleSection(collapsed = isSectionCollapsed("similar_to_$index")) {
                            LazyRow(contentPadding = PaddingValues(horizontal = irideGridItemStart), modifier = Modifier.animateItem()) {
                                items(rec.items) { recItem -> ytGridItem(recItem, null, false) }
                            }
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
                                        AsyncImage(model = thumbUrl, contentDescription = null, modifier = Modifier.size(irideTitleThumbSize).clip(shape))
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
                                        playerConnection?.playQueue(
                                            ListQueue(title = sectionData.title, items = sectionSongs.map { it.toMediaMetadata().toMediaItem() })
                                        )
                                    }
                                } else null,
                                modifier = Modifier.animateItem(),
                                useIrideStyle = topNavigationBarEnabled,
                                collapsed = isSectionCollapsed("home_section_$index"),
                                onCollapseToggle = { toggleSection("home_section_$index") },
                            )
                        }

                        if (isSongsOnly) {
                            item(key = "home_section_list_$index") {
                                IrideCollapsibleSection(collapsed = isSectionCollapsed("home_section_$index")) {
                                    LazyHorizontalGrid(
                                        state = rememberLazyGridState(),
                                        rows = GridCells.Fixed(4),
                                        contentPadding = PaddingValues(horizontal = irideListItemStart),
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
                                                            if (!isListenTogetherGuest) playerConnection?.playQueue(
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
                            }
                        } else {
                            item(key = "home_section_list_$index") {
                                IrideCollapsibleSection(collapsed = isSectionCollapsed("home_section_$index")) {
                                    LazyRow(contentPadding = PaddingValues(horizontal = irideGridItemStart), modifier = Modifier.animateItem()) {
                                        items(items = sectionData.items.distinctBy { it.id }, key = { "home_section_${index}_item_${it.id}" }) { secItem ->
                                            ytGridItem(secItem, null, false)
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
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeCollapsingHeader(
    scrollBehavior: TopAppBarScrollBehavior,
    accountImageUrl: String?,
    onAccountClick: () -> Unit,
    transparentBackground: Boolean = false,
) {
    val density = LocalDensity.current
    val largeTitleHeightPx = with(density) { HomeLargeTitleHeightDp.toPx() }

    SideEffect {
        if (scrollBehavior.state.heightOffsetLimit != -largeTitleHeightPx) {
            scrollBehavior.state.heightOffsetLimit = -largeTitleHeightPx
        }
    }

    val fraction = scrollBehavior.state.collapsedFraction
    val totalHeightDp = HomeSmallTitleBarHeightDp + HomeLargeTitleHeightDp
    val statusBarHeightDp = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

    Surface(
        color = if (transparentBackground) Color.Transparent else MaterialTheme.colorScheme.background,
        modifier = Modifier
            .fillMaxWidth()
            .height(totalHeightDp + statusBarHeightDp + with(density) { scrollBehavior.state.heightOffset.toDp() }),
    ) {
        Box(modifier = Modifier.windowInsetsPadding(WindowInsets.statusBars)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(HomeSmallTitleBarHeightDp)
                    .padding(horizontal = 12.dp)
                    .graphicsLayer {
                        translationY = lerpFloat(
                            with(density) { (HomeLargeTitleHeightDp - 12.dp).toPx() },
                            0f,
                            fraction,
                        )
                    },
                contentAlignment = Alignment.CenterStart,
            ) {
                val pillAlpha = (1f - fraction * 2f).coerceIn(0f, 1f)
                val pillEnabled = fraction < 0.05f
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    val homeStyle = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Bold)

                    Text(
                        text = stringResource(R.string.home),
                        style = homeStyle,
                        maxLines = 1,
                        modifier = Modifier.graphicsLayer {
                            val targetScale = 0.61f
                            val scale = lerpFloat(1f, targetScale, fraction)
                            scaleX = scale
                            scaleY = scale
                            transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0f, 0.5f)
                        },
                    )
                    Box(
                        modifier = Modifier
                            .alpha(pillAlpha)
                            .padding(bottom = 4.dp)
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .clickable(enabled = pillEnabled) { onAccountClick() },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.person),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SyncBanner(syncState: SyncState) {
    val ops = listOf(
        syncState.likedSongs, syncState.librarySongs, syncState.uploadedSongs,
        syncState.likedAlbums, syncState.uploadedAlbums, syncState.artists, syncState.playlists,
    )
    val completedCount = ops.count { it == SyncStatus.Completed }
    val progress = completedCount / ops.size.toFloat()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
        ),
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Syncing your library…",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                    Text(
                        text = "Network features may not work until sync completes.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f),
                    )
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(CircleShape),
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                trackColor = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.2f),
            )
            if (syncState.currentOperation.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = syncState.currentOperation,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.55f),
                )
            }
        }
    }
}

@Composable
fun CommunityPlaylistCard(
    item: CommunityPlaylistItem,
    onClick: () -> Unit,
    onSongClick: (SongItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    val database = LocalDatabase.current
    val playerConnection = LocalPlayerConnection.current
    val listenTogetherManager = LocalListenTogetherManager.current
    val isListenTogetherGuest = listenTogetherManager?.let { it.isInRoom && !it.isHost } ?: false
    val scope = rememberCoroutineScope()
    val isDark = isSystemInDarkTheme()

    val containerColor = if (isDark) {
        MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    }

    val dbPlaylist by database.playlistByBrowseId(item.playlist.id).collectAsState(initial = null)
    val isBookmarked = dbPlaylist?.playlist?.bookmarkedAt != null

    Card(
        modifier = modifier.width(320.dp).height(420.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        shape = RoundedCornerShape(28.dp),
        onClick = onClick,
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Box(modifier = Modifier.size(100.dp).clip(RoundedCornerShape(12.dp))) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        Row(modifier = Modifier.weight(1f)) {
                            AsyncImage(
                                model = item.songs.getOrNull(0)?.thumbnail?.replace(Regex("w\\d+-h\\d+"), "w120-h120"),
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.weight(1f).fillMaxSize(),
                            )
                            AsyncImage(
                                model = item.songs.getOrNull(1)?.thumbnail?.replace(Regex("w\\d+-h\\d+"), "w120-h120"),
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.weight(1f).fillMaxSize(),
                            )
                        }
                        Row(modifier = Modifier.weight(1f)) {
                            AsyncImage(
                                model = item.songs.getOrNull(2)?.thumbnail?.replace(Regex("w\\d+-h\\d+"), "w120-h120"),
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.weight(1f).fillMaxSize(),
                            )
                            AsyncImage(
                                model = item.songs.getOrNull(3)?.thumbnail?.replace(Regex("w\\d+-h\\d+"), "w120-h120"),
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.weight(1f).fillMaxSize(),
                            )
                        }
                    }
                }
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.Center) {
                    Text(
                        text = item.playlist.title,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 2,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = item.playlist.author?.name ?: "",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                        maxLines = 1,
                    )
                }
            }

            Column(modifier = Modifier.fillMaxWidth().weight(1f).padding(horizontal = 16.dp)) {
                item.songs.take(3).forEach { song ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .combinedClickable(onClick = { onSongClick(song) }),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        AsyncImage(
                            model = song.thumbnail.replace(Regex("w\\d+-h\\d+"), "w120-h120"),
                            contentDescription = null,
                            modifier = Modifier.size(56.dp).clip(RoundedCornerShape(12.dp)),
                            contentScale = ContentScale.Crop,
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = song.title,
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                            )
                            Text(
                                text = song.artists.joinToString(", ") { it.name },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
            ) {
                IconButton(
                    onClick = {
                        if (!isListenTogetherGuest) {
                            item.playlist.playEndpoint?.let { playerConnection?.playQueue(YouTubeQueue(it)) }
                        }
                    },
                    modifier = Modifier.size(48.dp).background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_widget_play),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(24.dp),
                    )
                }
                IconButton(
                    onClick = {
                        if (!isListenTogetherGuest) {
                            item.playlist.radioEndpoint?.let { playerConnection?.playQueue(YouTubeQueue(it)) }
                        }
                    },
                    modifier = Modifier.size(48.dp).background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f), CircleShape),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.radio),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.size(24.dp),
                    )
                }
                IconButton(
                    onClick = {
                        scope.launch(Dispatchers.IO) {
                            if (dbPlaylist?.playlist == null) {
                                val playlistEntity = PlaylistEntity(
                                    name = item.playlist.title,
                                    browseId = item.playlist.id,
                                    thumbnailUrl = item.playlist.thumbnail,
                                    remoteSongCount = item.playlist.songCountText?.split(" ")?.firstOrNull()?.toIntOrNull(),
                                    playEndpointParams = item.playlist.playEndpoint?.params,
                                    shuffleEndpointParams = item.playlist.shuffleEndpoint?.params,
                                    radioEndpointParams = item.playlist.radioEndpoint?.params,
                                ).toggleLike()
                                val songMetadata = item.songs.ifEmpty {
                                    YouTube.playlist(item.playlist.id).completed().getOrNull()?.songs.orEmpty()
                                }.map { it.toMediaMetadata() }
                                database.withTransaction {
                                    insert(playlistEntity)
                                    songMetadata.forEach { insert(it) }
                                    songMetadata.mapIndexed { index, song ->
                                        PlaylistSongMap(
                                            songId = song.id,
                                            playlistId = playlistEntity.id,
                                            position = index,
                                            setVideoId = song.setVideoId,
                                        )
                                    }.forEach { insert(it) }
                                }
                            } else {
                                database.transaction {
                                    update(dbPlaylist!!.playlist.toggleLike())
                                }
                            }
                        }
                    },
                    modifier = Modifier.size(48.dp).background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f), CircleShape),
                ) {
                    Icon(
                        painter = painterResource(if (isBookmarked) R.drawable.library_add_check else R.drawable.library_add),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.size(24.dp),
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun DailyDiscoverCard(
    dailyDiscover: com.metrolist.music.viewmodels.DailyDiscoverItem,
    onClick: () -> Unit,
    navController: NavController,
    modifier: Modifier = Modifier,
) {
    val database = LocalDatabase.current
    val playCount by database.getLifetimePlayCount(dailyDiscover.recommendation.id).collectAsState(initial = 0)
    val menuState = LocalMenuState.current
    val haptic = LocalHapticFeedback.current

    val song = dailyDiscover.recommendation as? SongItem
    val playsString = stringResource(R.string.plays)

    Card(
        modifier = modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(28.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    if (song != null) {
                        menuState.show {
                            YouTubeSongMenu(
                                song = song,
                                navController = navController,
                                onDismiss = { menuState.dismiss() },
                            )
                        }
                    }
                },
            ),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(28.dp),
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(dailyDiscover.recommendation.thumbnail?.replace(Regex("w\\d+-h\\d+"), "w544-h544"))
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )

            if (maxWidth > 200.dp) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color.Black.copy(alpha = 0.3f),
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.6f),
                                    Color.Black.copy(alpha = 0.9f),
                                ),
                            ),
                        ),
                )

                Column(
                    modifier = Modifier.fillMaxSize().padding(24.dp),
                    verticalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column {
                        Text(
                            text = dailyDiscover.recommendation.title,
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                        )
                        Text(
                            text = buildString {
                                append((dailyDiscover.recommendation as? SongItem)?.artists?.joinToString(", ") { it.name } ?: "")
                                if (playCount > 0) append(" • $playCount $playsString")
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.7f),
                        )
                    }

                    val messages = listOf(
                        R.string.daily_discover_sounds_like,
                        R.string.daily_discover_because_you_listen_to,
                        R.string.daily_discover_similar_to,
                        R.string.daily_discover_based_on,
                        R.string.daily_discover_for_fans_of,
                    )
                    val messageRes = remember(dailyDiscover.seed.id) {
                        messages[kotlin.math.abs(dailyDiscover.seed.id.hashCode()) % messages.size]
                    }
                    Text(
                        text = stringResource(
                            messageRes,
                            "${dailyDiscover.seed.title} • ${dailyDiscover.seed.artists.joinToString(", ") { it.name }}",
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = Color.White.copy(alpha = 0.6f),
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}
