/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.metrolist.innertube.models.AlbumItem
import com.metrolist.innertube.models.ArtistItem
import com.metrolist.innertube.models.SongItem
import com.metrolist.innertube.models.WatchEndpoint
import com.metrolist.music.LocalPlayerAwareWindowInsets
import com.metrolist.music.LocalPlayerConnection
import com.metrolist.music.R
import com.metrolist.music.constants.ListItemHeight
import com.metrolist.music.constants.MainTopGradientKey
import com.metrolist.music.constants.NewsCollapsedZonesKey
import com.metrolist.music.extensions.toMediaItem
import com.metrolist.music.models.toMediaMetadata
import com.metrolist.music.playback.PlayerConnection
import com.metrolist.music.playback.queues.ListQueue
import com.metrolist.music.playback.queues.YouTubeQueue
import com.metrolist.music.ui.component.IrideCollapsibleSection
import com.metrolist.music.ui.component.IconButton
import com.metrolist.music.ui.component.LocalMenuState
import com.metrolist.music.ui.component.MenuState
import com.metrolist.music.ui.component.NavigationTitle
import com.metrolist.music.ui.component.TopNavigationBar
import com.metrolist.music.ui.component.YouTubeGridItem
import com.metrolist.music.ui.component.YouTubeListItem
import com.metrolist.music.ui.component.frostedTopBarBackground
import com.metrolist.music.ui.component.rememberFrostBackdrop
import com.metrolist.music.ui.component.recordFrostBackdrop
import com.metrolist.music.ui.component.rubberBandOverscroll
import com.metrolist.music.ui.component.shimmer.ShimmerHost
import com.metrolist.music.ui.menu.YouTubeAlbumMenu
import com.metrolist.music.ui.menu.YouTubeSongMenu
import com.metrolist.music.ui.theme.SpaceMonoFontFamily
import com.metrolist.music.ui.theme.textSecondary
import com.metrolist.music.ui.utils.SnapLayoutInfoProvider
import com.metrolist.music.ui.utils.rememberDiscreteProgress
import com.metrolist.music.utils.rememberPreference
import com.metrolist.music.viewmodels.NewsRelease
import com.metrolist.music.viewmodels.NewsViewModel

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NewsScreen(
    navController: NavController,
    viewModel: NewsViewModel = hiltViewModel(),
) {
    val menuState = LocalMenuState.current
    val haptic = LocalHapticFeedback.current
    val playerConnection = LocalPlayerConnection.current ?: return
    val isPlaying by playerConnection.isEffectivelyPlaying.collectAsState()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()

    val personalReleases by viewModel.personalReleases.collectAsState()
    val generalReleases by viewModel.generalReleases.collectAsState()
    val chartSongs by viewModel.chartSongs.collectAsState()
    val discoverArtists by viewModel.discoverArtists.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    val mainTopGradient by rememberPreference(MainTopGradientKey, defaultValue = true)
    var collapsedZones by rememberPreference(NewsCollapsedZonesKey, setOf<String>())

    fun isZoneCollapsed(zone: String) = zone in collapsedZones

    fun toggleZone(zone: String) {
        collapsedZones =
            if (zone in collapsedZones) collapsedZones - zone else collapsedZones + zone
    }

    val lazyListState = rememberLazyListState()
    val topNavBarController = com.metrolist.music.LocalTopNavBarController.current
    val newsCellSize = 140.dp

    val headerScrolled by remember {
        derivedStateOf {
            lazyListState.firstVisibleItemIndex > 0 || lazyListState.firstVisibleItemScrollOffset > 8
        }
    }
    val topBarRevealProgress = rememberDiscreteProgress(headerScrolled)
    val frostBackdrop = rememberFrostBackdrop()

    val anyContentLoaded = personalReleases.isNotEmpty() || generalReleases.isNotEmpty() ||
        chartSongs.isNotEmpty() || discoverArtists.isNotEmpty()

    Scaffold(
        modifier = Modifier,
        containerColor = if (mainTopGradient) Color.Transparent else MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0),
    ) { paddingValues ->
        Box(Modifier.fillMaxSize()) {
            LazyColumn(
                state = lazyListState,
                overscrollEffect = null,
                contentPadding = PaddingValues(
                    top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 64.dp,
                    bottom = LocalPlayerAwareWindowInsets.current.asPaddingValues().calculateBottomPadding(),
                ),
                modifier = Modifier
                    .fillMaxSize()
                    .recordFrostBackdrop(frostBackdrop)
                    .rubberBandOverscroll(Orientation.Vertical, lazyListState),
            ) {
            if (isLoading && !anyContentLoaded) {
                item(key = "loading_indicator") {
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp)
                            .height(2.dp)
                            .clip(RoundedCornerShape(1.dp)),
                        color = MaterialTheme.colorScheme.textSecondary,
                    )
                }
            }

            item(key = "zone_personal") {
                ZoneTitle(
                    title = stringResource(R.string.news_from_your_artists),
                    collapsed = isZoneCollapsed("personal"),
                    onToggle = { toggleZone("personal") },
                )
                IrideCollapsibleSection(collapsed = isZoneCollapsed("personal")) {
                    ReleasesRow(
                        releases = personalReleases,
                        isLoading = isLoading,
                        gridHeight = newsCellSize,
                        activeAlbumId = mediaMetadata?.album?.id,
                        isPlaying = isPlaying,
                        navController = navController,
                        menuState = menuState,
                        hapticFeedback = haptic,
                    ) {
                        SeeAllReleases(
                            releases = personalReleases,
                            activeAlbumId = mediaMetadata?.album?.id,
                            isPlaying = isPlaying,
                            navController = navController,
                            menuState = menuState,
                            hapticFeedback = haptic,
                        )
                    }
                }
            }

            item(key = "zone_general") {
                ZoneTitle(
                    title = stringResource(R.string.news_just_out),
                    collapsed = isZoneCollapsed("general"),
                    onToggle = { toggleZone("general") },
                )
                IrideCollapsibleSection(collapsed = isZoneCollapsed("general")) {
                    FeaturedRelease(
                        release = generalReleases.firstOrNull(),
                        onAlbumClick = { navController.navigate("album/${it.browseId}") },
                        onAlbumLongClick = { album ->
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            menuState.show {
                                YouTubeAlbumMenu(
                                    albumItem = album,
                                    navController = navController,
                                    onDismiss = menuState::dismiss,
                                )
                            }
                        },
                    )
                    ReleasesRow(
                        releases = generalReleases.drop(1),
                        isLoading = isLoading,
                        gridHeight = newsCellSize,
                        activeAlbumId = mediaMetadata?.album?.id,
                        isPlaying = isPlaying,
                        navController = navController,
                        menuState = menuState,
                        hapticFeedback = haptic,
                    )
                }
            }

            if (isLoading || chartSongs.isNotEmpty()) {
                item(key = "zone_charts") {
                    val chartsTitle = stringResource(R.string.news_hot_right_now)
                    ZoneTitle(
                        title = chartsTitle,
                        collapsed = isZoneCollapsed("charts"),
                        onToggle = { toggleZone("charts") },
                        onPlayAllClick = chartSongs.takeIf { it.isNotEmpty() }?.let { songs ->
                            {
                                playerConnection.playQueue(
                                    ListQueue(
                                        title = chartsTitle,
                                        items = songs.map { it.toMediaItem() },
                                    ),
                                )
                            }
                        },
                    )
                    IrideCollapsibleSection(collapsed = isZoneCollapsed("charts")) {
                        ChartsZone(
                            songs = chartSongs,
                            isLoading = isLoading && chartSongs.isEmpty(),
                            activeSongId = mediaMetadata?.id,
                            isPlaying = isPlaying,
                            navController = navController,
                            menuState = menuState,
                            hapticFeedback = haptic,
                            playerConnection = playerConnection,
                        )
                    }
                }
            }

            item(key = "zone_discover") {
                ZoneTitle(
                    title = stringResource(R.string.news_discover),
                    collapsed = isZoneCollapsed("discover"),
                    onToggle = { toggleZone("discover") },
                )
                IrideCollapsibleSection(collapsed = isZoneCollapsed("discover")) {
                    DiscoverZone(
                        artists = discoverArtists,
                        isLoading = isLoading && discoverArtists.isEmpty(),
                        gridHeight = newsCellSize,
                        navController = navController,
                    )
                }
            }
        }

            TopNavigationBar(
                navigationItems = topNavBarController?.navigationItems ?: emptyList(),
                currentRoute = topNavBarController?.currentRoute,
                onItemClick = topNavBarController?.onItemClick ?: { _, _ -> },
                compact = topNavBarController?.compact ?: false,
                accountImageUrl = topNavBarController?.accountImageUrl,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .frostedTopBarBackground(
                        progress = topBarRevealProgress,
                        barColor = MaterialTheme.colorScheme.background,
                        strokeColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f),
                        backdrop = frostBackdrop,
                    ),
                containerColor = Color.Transparent,
            )
        }
    }
}

@Composable
private fun ZoneTitle(
    title: String,
    collapsed: Boolean,
    onToggle: () -> Unit,
    onPlayAllClick: (() -> Unit)? = null,
) {
    NavigationTitle(
        title = title,
        useIrideStyle = true,
        collapsed = collapsed,
        onCollapseToggle = onToggle,
        onPlayAllClick = onPlayAllClick,
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ReleasesRow(
    releases: List<NewsRelease>,
    isLoading: Boolean,
    gridHeight: Dp,
    activeAlbumId: String?,
    isPlaying: Boolean,
    navController: NavController,
    menuState: MenuState,
    hapticFeedback: androidx.compose.ui.hapticfeedback.HapticFeedback,
    expandedContent: (@Composable () -> Unit)? = null,
) {
    var expanded by remember { mutableStateOf(false) }

    if (releases.isEmpty()) {
        ZoneStatusText(isLoading)
        return
    }

    if (expandedContent != null) {
        Text(
            text = stringResource(if (expanded) R.string.news_show_less else R.string.news_see_all),
            style = MaterialTheme.typography.labelMedium.copy(fontFamily = SpaceMonoFontFamily),
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .padding(start = 20.dp, top = 2.dp, bottom = 6.dp)
                .clickable { expanded = !expanded },
        )
    }

    if (expanded && expandedContent != null) {
        expandedContent()
    } else {
        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(items = releases, key = { it.album.id }) { release ->
                ReleaseGridCell(
                    release = release,
                    size = gridHeight,
                    isActive = activeAlbumId == release.album.id,
                    isPlaying = isPlaying,
                    navController = navController,
                    menuState = menuState,
                    hapticFeedback = hapticFeedback,
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SeeAllReleases(
    releases: List<NewsRelease>,
    activeAlbumId: String?,
    isPlaying: Boolean,
    navController: NavController,
    menuState: MenuState,
    hapticFeedback: androidx.compose.ui.hapticfeedback.HapticFeedback,
) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val widthFactor = if (maxWidth * 0.475f >= 320.dp) 0.475f else 0.9f
        val itemWidth = maxWidth * widthFactor

        val lazyGridState = rememberLazyGridState()
        val snapLayoutInfoProvider = remember(lazyGridState) {
            SnapLayoutInfoProvider(
                lazyGridState = lazyGridState,
                positionInLayout = { layoutSize, itemSize ->
                    (layoutSize * widthFactor / 2f - itemSize / 2f)
                },
            )
        }

        LazyHorizontalGrid(
            state = lazyGridState,
            rows = GridCells.Fixed(4),
            flingBehavior = rememberSnapFlingBehavior(snapLayoutInfoProvider),
            contentPadding = PaddingValues(horizontal = 20.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(ListItemHeight * ((releases.size + 3) / 4)),
        ) {
            items(items = releases, key = { "news_all_${it.album.id}" }) { release ->
                ReleaseListCell(
                    album = release.album,
                    isActive = activeAlbumId == release.album.id,
                    isPlaying = isPlaying,
                    width = itemWidth,
                    navController = navController,
                    menuState = menuState,
                    hapticFeedback = hapticFeedback,
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ReleaseListCell(
    album: AlbumItem,
    isActive: Boolean,
    isPlaying: Boolean,
    width: Dp,
    navController: NavController,
    menuState: MenuState,
    hapticFeedback: androidx.compose.ui.hapticfeedback.HapticFeedback,
) {
    YouTubeListItem(
        item = album,
        isActive = isActive,
        isPlaying = isPlaying,
        isSwipeable = false,
        modifier = Modifier
            .width(width)
            .combinedClickable(
                onClick = { navController.navigate("album/${album.browseId}") },
                onLongClick = {
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                    menuState.show {
                        YouTubeAlbumMenu(
                            albumItem = album,
                            navController = navController,
                            onDismiss = menuState::dismiss,
                        )
                    }
                },
            ),
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ReleaseGridCell(
    release: NewsRelease,
    size: Dp,
    isActive: Boolean,
    isPlaying: Boolean,
    navController: NavController,
    menuState: MenuState,
    hapticFeedback: androidx.compose.ui.hapticfeedback.HapticFeedback,
) {
    YouTubeGridItem(
        item = release.album,
        isActive = isActive,
        isPlaying = isPlaying,
        thumbnailRatio = 1f,
        size = size,
        modifier = Modifier.combinedClickable(
            onClick = { navController.navigate("album/${release.album.browseId}") },
            onLongClick = {
                hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                menuState.show {
                    YouTubeAlbumMenu(
                        albumItem = release.album,
                        navController = navController,
                        onDismiss = menuState::dismiss,
                    )
                }
            },
        ),
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ChartsZone(
    songs: List<SongItem>,
    isLoading: Boolean,
    activeSongId: String?,
    isPlaying: Boolean,
    navController: NavController,
    menuState: MenuState,
    hapticFeedback: androidx.compose.ui.hapticfeedback.HapticFeedback,
    playerConnection: PlayerConnection,
) {
    if (songs.isEmpty()) {
        ZoneStatusText(isLoading)
        return
    }

    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val widthFactor = if (maxWidth * 0.475f >= 320.dp) 0.475f else 0.9f
        val itemWidth = maxWidth * widthFactor

        val lazyGridState = rememberLazyGridState()
        val snapLayoutInfoProvider = remember(lazyGridState) {
            SnapLayoutInfoProvider(
                lazyGridState = lazyGridState,
                positionInLayout = { layoutSize, itemSize ->
                    (layoutSize * widthFactor / 2f - itemSize / 2f)
                },
            )
        }

        LazyHorizontalGrid(
            state = lazyGridState,
            rows = GridCells.Fixed(4),
            flingBehavior = rememberSnapFlingBehavior(snapLayoutInfoProvider),
            contentPadding = PaddingValues(horizontal = 20.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(ListItemHeight * ((songs.size + 3) / 4)),
        ) {
            itemsIndexed(items = songs, key = { _, song -> "news_chart_${song.id}" }) { index, song ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = (index + 1).toString().padStart(2, '0'),
                        style = MaterialTheme.typography.titleMedium.copy(fontFamily = SpaceMonoFontFamily),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.textSecondary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.width(34.dp),
                    )
                    ChartSongRow(
                        song = song,
                        width = itemWidth - 34.dp,
                        isActive = song.id == activeSongId,
                        isPlaying = isPlaying,
                        navController = navController,
                        menuState = menuState,
                        hapticFeedback = hapticFeedback,
                        playerConnection = playerConnection,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FeaturedRelease(
    release: NewsRelease?,
    onAlbumClick: (AlbumItem) -> Unit,
    onAlbumLongClick: (AlbumItem) -> Unit,
) {
    if (release == null) return
    val album = release.album
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp, bottom = 12.dp)
            .aspectRatio(1f)
            .clip(RoundedCornerShape(16.dp))
            .combinedClickable(
                onClick = { onAlbumClick(album) },
                onLongClick = { onAlbumLongClick(album) },
            ),
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(album.thumbnail)
                .crossfade(true)
                .build(),
            contentDescription = album.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to Color.Transparent,
                        0.55f to Color.Transparent,
                        1f to Color.Black.copy(alpha = 0.75f),
                    ),
                ),
        )

        val meta = listOfNotNull(
            album.artists?.joinToString { it.name },
            album.year?.toString(),
            album.albumType,
        ).joinToString("  ·  ")

        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            Text(
                text = album.title,
                style = MaterialTheme.typography.titleLarge.copy(fontFamily = SpaceMonoFontFamily),
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = meta,
                style = MaterialTheme.typography.bodyMedium.copy(fontFamily = SpaceMonoFontFamily),
                color = Color.White.copy(alpha = 0.85f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ChartSongRow(
    song: SongItem,
    width: Dp,
    isActive: Boolean,
    isPlaying: Boolean,
    navController: NavController,
    menuState: MenuState,
    hapticFeedback: androidx.compose.ui.hapticfeedback.HapticFeedback,
    playerConnection: PlayerConnection,
) {
    YouTubeListItem(
        item = song,
        isActive = isActive,
        isPlaying = isPlaying,
        isSwipeable = false,
        trailingContent = {
            IconButton(
                onClick = {
                    menuState.show {
                        YouTubeSongMenu(
                            song = song,
                            navController = navController,
                            onDismiss = menuState::dismiss,
                        )
                    }
                },
                onLongClick = {},
            ) {
                Icon(
                    painter = painterResource(R.drawable.more_vert),
                    contentDescription = null,
                )
            }
        },
        modifier = Modifier
            .width(width)
            .combinedClickable(
                onClick = {
                    if (song.id == playerConnection.mediaMetadata.value?.id) {
                        playerConnection.togglePlayPause()
                    } else {
                        playerConnection.playQueue(
                            YouTubeQueue(
                                endpoint = song.endpoint ?: WatchEndpoint(videoId = song.id),
                                preloadItem = song.toMediaMetadata(),
                            ),
                        )
                    }
                },
                onLongClick = {
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                    menuState.show {
                        YouTubeSongMenu(
                            song = song,
                            navController = navController,
                            onDismiss = menuState::dismiss,
                        )
                    }
                },
            ),
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DiscoverZone(
    artists: List<ArtistItem>,
    isLoading: Boolean,
    gridHeight: Dp,
    navController: NavController,
) {
    if (artists.isEmpty()) {
        ZoneStatusText(isLoading)
        return
    }
    LazyRow(
        contentPadding = PaddingValues(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(items = artists, key = { "news_discover_${it.id}" }) { artist ->
            YouTubeGridItem(
                item = artist,
                isActive = false,
                isPlaying = false,
                thumbnailRatio = 1f,
                size = gridHeight,
                modifier = Modifier.clickable {
                    artist.id?.let { navController.navigate("artist/$it") }
                },
            )
        }
    }
}

@Composable
private fun ZoneStatusText(isLoading: Boolean) {
    if (isLoading) {
        ShimmerHost(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                repeat(3) {
                    Box(
                        modifier = Modifier
                            .height(160.dp)
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                    )
                }
            }
        }
    } else {
        Text(
            text = stringResource(R.string.news_empty_general),
            style = MaterialTheme.typography.bodyMedium.copy(fontFamily = SpaceMonoFontFamily),
            color = MaterialTheme.colorScheme.textSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp),
        )
    }
}
