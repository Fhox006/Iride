/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.animation.core.tween
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import com.metrolist.music.LocalPlayerAwareWindowInsets
import com.metrolist.music.LocalTopNavBarController
import com.metrolist.music.R
import com.metrolist.music.constants.GridItemSize
import com.metrolist.music.constants.GridItemsSizeKey
import com.metrolist.music.constants.GridThumbnailHeight
import com.metrolist.music.constants.MainTopGradientKey
import com.metrolist.music.constants.PureBlackKey
import com.metrolist.music.utils.rememberPreference
import com.metrolist.music.db.entities.PodcastEntity
import com.metrolist.music.ui.component.ChipsRow
import com.metrolist.music.ui.component.CollapsingScreenHeader
import com.metrolist.music.ui.component.FloatingPillBottomSpacing
import com.metrolist.music.ui.component.FloatingPillHeight
import com.metrolist.music.ui.component.IconButton
import com.metrolist.music.ui.component.LocalMenuState
import com.metrolist.music.ui.component.TopNavigationBar
import com.metrolist.music.ui.component.frostedTopBarBackground
import com.metrolist.music.ui.component.YouTubeGridItem
import com.metrolist.music.ui.component.shimmer.GridItemPlaceHolder
import com.metrolist.music.ui.component.shimmer.ListItemPlaceHolder
import com.metrolist.music.ui.component.shimmer.ShimmerHost
import com.metrolist.music.ui.menu.YouTubeAlbumMenu
import com.metrolist.music.ui.menu.YouTubeArtistMenu
import com.metrolist.music.ui.menu.YouTubePlaylistMenu
import com.metrolist.music.ui.theme.textSecondary
import com.metrolist.music.ui.utils.backToMain
import com.metrolist.music.utils.rememberEnumPreference
import com.metrolist.music.viewmodels.AccountContentType
import com.metrolist.music.viewmodels.AccountViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun AccountScreen(
    navController: NavController,
    viewModel: AccountViewModel = hiltViewModel(),
) {
    val menuState = LocalMenuState.current
    val haptic = LocalHapticFeedback.current

    val coroutineScope = rememberCoroutineScope()

    val playlists by viewModel.playlists.collectAsState()
    val albums by viewModel.albums.collectAsState()
    val artists by viewModel.artists.collectAsState()
    val sePlaylist by viewModel.sePlaylist.collectAsState()
    val rdpnPlaylist by viewModel.rdpnPlaylist.collectAsState()
    val podcastPlaylists by viewModel.podcastPlaylists.collectAsState()
    val podcastChannels by viewModel.podcastChannels.collectAsState()
    val selectedContentType by viewModel.selectedContentType.collectAsState()
    val gridItemSize by rememberEnumPreference(GridItemsSizeKey, GridItemSize.BIG)
    val pureBlack by rememberPreference(PureBlackKey, defaultValue = false)
    val mainTopGradient by rememberPreference(MainTopGradientKey, defaultValue = true)
    val topNavBarController = LocalTopNavBarController.current

    LaunchedEffect(Unit) {
        if (playlists == null || albums == null || artists == null) {
            viewModel.refresh()
        }
    }

    val scrollState = androidx.compose.foundation.lazy.grid.rememberLazyGridState()
    val accountHeaderScrolled by androidx.compose.runtime.remember {
        androidx.compose.runtime.derivedStateOf {
            scrollState.firstVisibleItemIndex > 0 || scrollState.firstVisibleItemScrollOffset > 8
        }
    }
    val topBarRevealProgress = com.metrolist.music.ui.utils.rememberDiscreteProgress(accountHeaderScrolled)

    Scaffold(
        topBar = {
            if (topNavBarController != null) {
                TopNavigationBar(
                    navigationItems = topNavBarController.navigationItems,
                    currentRoute = topNavBarController.currentRoute,
                    onItemClick = topNavBarController.onItemClick,
                    containerColor = Color.Transparent,
                    compact = topNavBarController.compact,
                    accountImageUrl = topNavBarController.accountImageUrl,
                    modifier = Modifier.frostedTopBarBackground(
                        progress = topBarRevealProgress,
                        barColor = MaterialTheme.colorScheme.background,
                        strokeColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f),
                        backdrop = com.metrolist.music.ui.component.LocalScreenFrostBackdrop.current,
                    ),
                )
            }
        },
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets(0),
    ) { paddingValues ->
        LazyVerticalGrid(
            state = scrollState,
            columns = GridCells.Adaptive(minSize = GridThumbnailHeight + if (gridItemSize == GridItemSize.BIG) 24.dp else (-24).dp),
            contentPadding = PaddingValues(
                bottom = LocalPlayerAwareWindowInsets.current.asPaddingValues().calculateBottomPadding(),
            ),
            modifier = Modifier
                .padding(paddingValues)
                .background(
                    when {
                        pureBlack -> Color.Black
                        mainTopGradient -> Color.Transparent
                        else -> MaterialTheme.colorScheme.background
                    },
                ),
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                ChipsRow(
                    chips =
                        listOf(
                            AccountContentType.PLAYLISTS to stringResource(R.string.filter_playlists),
                            AccountContentType.ALBUMS to stringResource(R.string.filter_albums),
                            AccountContentType.ARTISTS to stringResource(R.string.filter_artists),
                            AccountContentType.PODCASTS to stringResource(R.string.filter_podcasts),
                        ),
                    currentValue = selectedContentType,
                    onValueUpdate = { viewModel.setSelectedContentType(it) },
                    useIrideStyle = true,
                )
            }

            AccountContentGridItems(
                navController = navController,
                menuState = menuState,
                haptic = haptic,
                coroutineScope = coroutineScope,
                selectedContentType = selectedContentType,
                playlists = playlists,
                albums = albums,
                artists = artists,
                rdpnPlaylist = rdpnPlaylist,
                sePlaylist = sePlaylist,
                podcastPlaylists = podcastPlaylists,
                podcastChannels = podcastChannels,
            )
        }
    }
}

/**
 * The actual account-content grid items (playlists/albums/artists/podcasts), shared between the
 * New Iride UI branch and the classic push-navigation branch of [AccountScreen] above — only the
 * surrounding chrome (top bar style) differs between the two.
 */
private fun androidx.compose.foundation.lazy.grid.LazyGridScope.AccountContentGridItems(
    navController: NavController,
    menuState: com.metrolist.music.ui.component.MenuState,
    haptic: androidx.compose.ui.hapticfeedback.HapticFeedback,
    coroutineScope: kotlinx.coroutines.CoroutineScope,
    selectedContentType: AccountContentType,
    playlists: List<com.metrolist.innertube.models.PlaylistItem>?,
    albums: List<com.metrolist.innertube.models.AlbumItem>?,
    artists: List<com.metrolist.innertube.models.ArtistItem>?,
    rdpnPlaylist: com.metrolist.innertube.models.PlaylistItem?,
    sePlaylist: com.metrolist.innertube.models.PlaylistItem?,
    podcastPlaylists: List<PodcastEntity>,
    podcastChannels: List<com.metrolist.innertube.models.ArtistItem>,
) {
        when (selectedContentType) {
            AccountContentType.PLAYLISTS -> {
                items(
                    items = playlists.orEmpty().distinctBy { it.id },
                    key = { "account_playlist_${it.id}" },
                ) { item ->
                    YouTubeGridItem(
                        item = item,
                        fillMaxWidth = true,
                        modifier =
                            Modifier
                                .combinedClickable(
                                    onClick = {
                                        navController.navigate("online_playlist/${item.id}")
                                    },
                                    onLongClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        menuState.show {
                                            YouTubePlaylistMenu(
                                                playlist = item,
                                                coroutineScope = coroutineScope,
                                                onDismiss = menuState::dismiss,
                                            )
                                        }
                                    },
                                ),
                    )
                }

                if (playlists == null) {
                    items(8) {
                        ShimmerHost {
                            GridItemPlaceHolder(fillMaxWidth = true)
                        }
                    }
                }
            }

            AccountContentType.ALBUMS -> {
                items(
                    items = albums.orEmpty().distinctBy { it.id },
                    key = { "account_album_${it.id}" },
                ) { item ->
                    YouTubeGridItem(
                        item = item,
                        fillMaxWidth = true,
                        modifier =
                            Modifier
                                .combinedClickable(
                                    onClick = {
                                        navController.navigate("album/${item.id}")
                                    },
                                    onLongClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        menuState.show {
                                            YouTubeAlbumMenu(
                                                albumItem = item,
                                                navController = navController,
                                                onDismiss = menuState::dismiss,
                                            )
                                        }
                                    },
                                ),
                    )
                }

                if (albums == null) {
                    items(8) {
                        ShimmerHost {
                            GridItemPlaceHolder(fillMaxWidth = true)
                        }
                    }
                }
            }

            AccountContentType.ARTISTS -> {
                items(
                    items = artists.orEmpty().distinctBy { it.id },
                    key = { "account_artist_${it.id}" },
                ) { item ->
                    YouTubeGridItem(
                        item = item,
                        fillMaxWidth = true,
                        modifier =
                            Modifier
                                .combinedClickable(
                                    onClick = {
                                        navController.navigate("artist/${item.id}")
                                    },
                                    onLongClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        menuState.show {
                                            YouTubeArtistMenu(
                                                artist = item,
                                                onDismiss = menuState::dismiss,
                                            )
                                        }
                                    },
                                ),
                    )
                }

                if (artists == null) {
                    items(8) {
                        ShimmerHost {
                            GridItemPlaceHolder(fillMaxWidth = true)
                        }
                    }
                }
            }

            AccountContentType.PODCASTS -> {
                rdpnPlaylist?.let { rdpn ->
                    item(
                        key = "rdpn_playlist",
                        span = { GridItemSpan(maxLineSpan) },
                    ) {
                        SePlaylistAccountItem(
                            thumbnailUrl = rdpn.thumbnail,
                            title = stringResource(R.string.new_episodes),
                            subtitle = stringResource(R.string.auto_playlist),
                            onClick = { navController.navigate("online_playlist/RDPN") },
                        )
                    }
                }

                sePlaylist?.let { se ->
                    item(
                        key = "se_playlist",
                        span = { GridItemSpan(maxLineSpan) },
                    ) {
                        SePlaylistAccountItem(
                            thumbnailUrl = se.thumbnail,
                            title = stringResource(R.string.episodes_for_later),
                            subtitle = stringResource(R.string.auto_playlist),
                            onClick = { navController.navigate("online_playlist/SE") },
                        )
                    }
                }

                if (podcastPlaylists.isNotEmpty()) {
                    item(
                        key = "podcasts_header",
                        span = { GridItemSpan(maxLineSpan) },
                    ) {
                        Text(
                            text = stringResource(R.string.filter_podcasts),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.secondary,
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                        )
                    }
                    itemsIndexed(
                        items = podcastPlaylists,
                        key = { _, item -> "podcast_${item.id}" },
                        span = { _, _ -> GridItemSpan(maxLineSpan) },
                    ) { _, podcast ->
                        PodcastAccountItem(
                            thumbnailUrl = podcast.thumbnailUrl,
                            title = podcast.title,
                            subtitle = podcast.author,
                            onClick = { navController.navigate("online_podcast/${podcast.id}") },
                        )
                    }
                }

                if (podcastChannels.isNotEmpty()) {
                    item(
                        key = "channels_header",
                        span = { GridItemSpan(maxLineSpan) },
                    ) {
                        Text(
                            text = stringResource(R.string.filter_channels),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.secondary,
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                        )
                    }
                    itemsIndexed(
                        items = podcastChannels,
                        key = { _, item -> "channel_${item.id}" },
                        span = { _, _ -> GridItemSpan(maxLineSpan) },
                    ) { _, channel ->
                        PodcastChannelAccountItem(
                            thumbnailUrl = channel.thumbnail,
                            name = channel.title,
                            onClick = { navController.navigate("artist/${channel.id}") },
                        )
                    }
                }

                if (rdpnPlaylist == null && sePlaylist == null && podcastPlaylists.isEmpty() && podcastChannels.isEmpty()) {
                    items(4, span = { GridItemSpan(maxLineSpan) }) {
                        ShimmerHost {
                            ListItemPlaceHolder()
                        }
                    }
                }
            }
        }
}

@Composable
private fun SePlaylistAccountItem(
    thumbnailUrl: String?,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier =
            modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Box(
            modifier =
                Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            if (thumbnailUrl != null) {
                AsyncImage(
                    model = thumbnailUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier =
                        Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(8.dp)),
                )
            } else {
                Icon(
                    painter = painterResource(R.drawable.queue_music),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(28.dp),
                )
            }
        }

        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        Icon(
            painter = painterResource(R.drawable.navigate_next),
            contentDescription = null,
        )
    }
}

@Composable
private fun PodcastAccountItem(
    thumbnailUrl: String?,
    title: String,
    subtitle: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier =
            modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Box(
            modifier =
                Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            if (thumbnailUrl != null) {
                AsyncImage(
                    model = thumbnailUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier =
                        Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(8.dp)),
                )
            } else {
                Icon(
                    painter = painterResource(R.drawable.queue_music),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(28.dp),
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun PodcastChannelAccountItem(
    thumbnailUrl: String?,
    name: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier =
            modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        AsyncImage(
            model = thumbnailUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier =
                Modifier
                    .size(56.dp)
                    .clip(CircleShape),
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text = name,
            style = MaterialTheme.typography.bodyLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
    }
}
