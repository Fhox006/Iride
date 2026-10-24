/** Metrolist Project (C) 2026 Licensed under GPL-3.0 | See git history for contributors */
package com.metrolist.music.ui.screens.artist

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import com.metrolist.music.ui.component.irideFolioCombinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import com.metrolist.innertube.models.AlbumItem
import com.metrolist.innertube.models.ArtistItem
import com.metrolist.innertube.models.EpisodeItem
import com.metrolist.innertube.models.PlaylistItem
import com.metrolist.innertube.models.PodcastItem
import com.metrolist.innertube.models.SongItem
import com.metrolist.innertube.models.WatchEndpoint
import com.metrolist.music.LocalPlayerAwareWindowInsets
import com.metrolist.music.LocalPlayerConnection
import com.metrolist.music.R
import com.metrolist.music.constants.GridItemSize
import com.metrolist.music.constants.GridItemsSizeKey
import com.metrolist.music.constants.GridThumbnailHeight
import com.metrolist.music.models.toMediaMetadata
import com.metrolist.music.playback.queues.YouTubeQueue
import com.metrolist.music.ui.component.IconButton
import com.metrolist.music.ui.component.IridePageBackButton
import com.metrolist.music.ui.component.IridePageLayout
import com.metrolist.music.ui.component.LibrarySortRow
import com.metrolist.music.ui.component.SongMenuIconButton
import com.metrolist.music.ui.component.LocalMenuState
import com.metrolist.music.ui.component.IrideFolio
import com.metrolist.music.ui.component.YouTubeGridItem
import com.metrolist.music.ui.component.YouTubeListItem
import com.metrolist.music.ui.component.shimmer.GridItemPlaceHolder
import com.metrolist.music.ui.component.shimmer.ListItemPlaceHolder
import com.metrolist.music.ui.component.shimmer.ShimmerHost
import com.metrolist.music.ui.menu.YouTubeAlbumMenu
import com.metrolist.music.ui.menu.YouTubeArtistMenu
import com.metrolist.music.ui.menu.YouTubePlaylistMenu
import com.metrolist.music.ui.menu.YouTubeSongMenu
import com.metrolist.music.utils.rememberEnumPreference
import com.metrolist.music.utils.rememberPreference
import com.metrolist.music.viewmodels.ArtistItemsViewModel

private enum class ArtistItemsSortType {
    DEFAULT,
    NAME,
    ARTIST,
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ArtistItemsScreen(
    navController: NavController,
    viewModel: ArtistItemsViewModel = hiltViewModel(),
) {
    val menuState = LocalMenuState.current
    val playerConnection = LocalPlayerConnection.current ?: return
    val isPlaying by playerConnection.isEffectivelyPlaying.collectAsState()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()

    val lazyListState = rememberLazyListState()
    val lazyGridState = rememberLazyGridState()
    val coroutineScope = rememberCoroutineScope()
    val gridItemSize by rememberEnumPreference(GridItemsSizeKey, GridItemSize.BIG)

    val title by viewModel.title.collectAsState()
    val itemsPage by viewModel.itemsPage.collectAsState()

    val keyboardController = LocalSoftwareKeyboardController.current

    var sortType by rememberSaveable { mutableStateOf(ArtistItemsSortType.DEFAULT) }
    var sortDescending by rememberSaveable { mutableStateOf(true) }
    val sortedSongs =
        remember(itemsPage, sortType, sortDescending) {
            val songs = itemsPage?.items.orEmpty().distinctBy { it.id }
            when (sortType) {
                ArtistItemsSortType.DEFAULT -> songs
                ArtistItemsSortType.NAME -> {
                    val sorted = songs.sortedBy { it.title.lowercase() }
                    if (sortDescending) sorted.reversed() else sorted
                }
                ArtistItemsSortType.ARTIST -> {
                    val sorted = songs.sortedBy {
                        (it as? SongItem)?.artists?.firstOrNull()?.name?.lowercase() ?: ""
                    }
                    if (sortDescending) sorted.reversed() else sorted
                }
            }
        }

    LaunchedEffect(lazyListState) {
        snapshotFlow {
                lazyListState.layoutInfo.visibleItemsInfo.any { it.key == "loading" }
            }
            .collect { shouldLoadMore ->
                if (!shouldLoadMore) return@collect
                viewModel.loadMore()
            }
    }

    LaunchedEffect(lazyGridState) {
        snapshotFlow {
                lazyGridState.layoutInfo.visibleItemsInfo.any { it.key == "loading" }
            }
            .collect { shouldLoadMore ->
                if (!shouldLoadMore) return@collect
                viewModel.loadMore()
            }
    }

    Box(
        Modifier.fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        if (itemsPage == null) {
            ShimmerHost(modifier = Modifier) {
                repeat(8) {
                    ListItemPlaceHolder()
                }
            }
        }

        if (itemsPage?.items?.firstOrNull() is SongItem) {
            IridePageLayout(
                title = title,
                state = lazyListState,
                showTopBar = true,
                navigationIcon = { IridePageBackButton(navController) },
                bottomPadding =
                    LocalPlayerAwareWindowInsets.current.asPaddingValues().calculateBottomPadding(),
            ) {
                item(key = "sort") {
                    LibrarySortRow(
                        sortOptions =
                            listOf(
                                ArtistItemsSortType.DEFAULT to
                                    stringResource(R.string.sort_by_play_time),
                                ArtistItemsSortType.NAME to stringResource(R.string.sort_by_name),
                                ArtistItemsSortType.ARTIST to
                                    stringResource(R.string.sort_by_artist),
                            ),
                        currentSort = sortType,
                        onSortChange = { sortType = it },
                        sortDescending = sortDescending,
                        onSortDescendingChange = { sortDescending = it },
                        modifier = Modifier.padding(horizontal = 12.dp),
                    )
                }

                items(
                    items = sortedSongs,
                    key = { "artist_items_list_${it.id}" },
                ) { item ->
                    val videoItem = item is SongItem && (item.isVideoSong ||
                        title.contains("video", ignoreCase = true) || title.contains("performance", ignoreCase = true))
                    if (videoItem) {
                        YouTubeGridItem(
                            item = item,
                            thumbnailRatio = IrideFolio.VideoThumbnailRatio,
                            forceThumbnailRatio = true,
                            thumbnailCornerRadius = IrideFolio.VideoThumbnailRadius,
                            thumbnailUrlOverride = com.metrolist.music.playback.youtubeVideoThumbnail(item.id, item.thumbnail),
                            fillMaxWidth = true,
                            isActive = mediaMetadata?.id == item.id,
                            isPlaying = isPlaying,
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                                .irideFolioCombinedClickable(
                                    onClick = {
                                        playerConnection.videoPlayback.playVideo(item.toMediaMetadata())
                                    },
                                    onLongClick = {
                                        menuState.show(anchored = true) {
                                            YouTubeSongMenu(song = item, navController = navController, onDismiss = menuState::dismiss)
                                        }
                                    },
                                ),
                        )
                    } else YouTubeListItem(
                        item = item,
                        isActive =
                            when (item) {
                                is SongItem -> mediaMetadata?.id == item.id
                                is AlbumItem -> mediaMetadata?.album?.id == item.id
                                else -> false
                            },
                        isPlaying = isPlaying,
                        trailingContent = {
                            SongMenuIconButton(
                                onClick = {
                                    menuState.show(
                                        anchored = true,
                                    ) {
                                        when (item) {
                                            is SongItem -> {
                                                YouTubeSongMenu(
                                                    song = item,
                                                    navController = navController,
                                                    onDismiss = menuState::dismiss,
                                                )
                                            }

                                            is AlbumItem -> {
                                                YouTubeAlbumMenu(
                                                    albumItem = item,
                                                    navController = navController,
                                                    onDismiss = menuState::dismiss,
                                                )
                                            }

                                            is ArtistItem -> {
                                                YouTubeArtistMenu(
                                                    artist = item,
                                                    onDismiss = menuState::dismiss,
                                                )
                                            }

                                            is PlaylistItem -> {
                                                YouTubePlaylistMenu(
                                                    playlist = item,
                                                    coroutineScope = coroutineScope,
                                                    onDismiss = menuState::dismiss,
                                                )
                                            }

                                            is PodcastItem -> {
                                                YouTubePlaylistMenu(
                                                    playlist = item.asPlaylistItem(),
                                                    coroutineScope = coroutineScope,
                                                    onDismiss = menuState::dismiss,
                                                )
                                            }

                                            is EpisodeItem -> {
                                                YouTubeSongMenu(
                                                    song = item.asSongItem(),
                                                    navController = navController,
                                                    onDismiss = menuState::dismiss,
                                                )
                                            }
                                        }
                                    }
                                }
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.more_vert),
                                    contentDescription = null,
                                )
                            }
                        },
                        modifier =
                            Modifier.clickable {
                                when (item) {
                                    is SongItem -> {
                                        if (item.id == mediaMetadata?.id) {
                                            playerConnection.togglePlayPause()
                                        } else {
                                            playerConnection.playQueue(
                                                YouTubeQueue(
                                                    item.endpoint
                                                        ?: WatchEndpoint(videoId = item.id),
                                                    item.toMediaMetadata(),
                                                )
                                            )
                                        }
                                    }

                                    is AlbumItem -> {
                                        navController.navigate("album/${item.id}")
                                    }

                                    is ArtistItem -> {
                                        navController.navigate("artist/${item.id}")
                                    }

                                    is PlaylistItem -> {
                                        navController.navigate("online_playlist/${item.id}")
                                    }

                                    is PodcastItem -> {
                                        navController.navigate("online_podcast/${item.id}")
                                    }

                                    is EpisodeItem -> {
                                        if (item.id == mediaMetadata?.id) {
                                            playerConnection.togglePlayPause()
                                        } else {
                                            playerConnection.playQueue(
                                                YouTubeQueue(
                                                    item.endpoint
                                                        ?: WatchEndpoint(videoId = item.id),
                                                    item.toMediaMetadata(),
                                                )
                                            )
                                        }
                                    }
                                }
                            },
                    )
                }

                if (itemsPage?.continuation != null) {
                    item(key = "loading") {
                        ShimmerHost {
                            repeat(3) {
                                ListItemPlaceHolder()
                            }
                        }
                    }
                }
            }
        } else {
            IridePageLayout(
                title = title,
                state = lazyGridState,
                showTopBar = true,
                navigationIcon = { IridePageBackButton(navController) },
                bottomPadding =
                    LocalPlayerAwareWindowInsets.current.asPaddingValues().calculateBottomPadding(),
                columns =
                    GridCells.Adaptive(
                        minSize =
                            GridThumbnailHeight +
                                if (gridItemSize == GridItemSize.BIG) 24.dp else (-24).dp
                    ),
            ) {
                items(
                    items = itemsPage?.items.orEmpty().distinctBy { it.id },
                    key = { "artist_items_grid_${it.id}" },
                ) { item ->
                    YouTubeGridItem(
                        item = item,
                        isActive =
                            when (item) {
                                is SongItem -> mediaMetadata?.id == item.id
                                is AlbumItem -> mediaMetadata?.album?.id == item.id
                                else -> false
                            },
                        isPlaying = isPlaying,
                        fillMaxWidth = true,
                        coroutineScope = coroutineScope,
                        modifier =
                            Modifier.irideFolioCombinedClickable(
                                    onClick = {
                                        when (item) {
                                            is SongItem -> {
                                                if (item.isVideoSong || title.contains("video", ignoreCase = true) ||
                                                    title.contains("performance", ignoreCase = true)
                                                ) {
                                                    playerConnection.videoPlayback.playVideo(item.toMediaMetadata())
                                                } else playerConnection.playQueue(
                                                    YouTubeQueue(
                                                        item.endpoint
                                                            ?: WatchEndpoint(videoId = item.id),
                                                        item.toMediaMetadata(),
                                                    )
                                                )
                                            }

                                            is AlbumItem -> {
                                                navController.navigate("album/${item.id}")
                                            }

                                            is ArtistItem -> {
                                                navController.navigate("artist/${item.id}")
                                            }

                                            is PlaylistItem -> {
                                                navController.navigate("online_playlist/${item.id}")
                                            }

                                            is PodcastItem -> {
                                                navController.navigate("online_podcast/${item.id}")
                                            }

                                            is EpisodeItem -> {
                                                playerConnection.playQueue(
                                                    YouTubeQueue(
                                                        item.endpoint
                                                            ?: WatchEndpoint(videoId = item.id),
                                                        item.toMediaMetadata(),
                                                    )
                                                )
                                            }
                                        }
                                    },
                                    onLongClick = {
                                        menuState.show(
                                            anchored = true,
                                        ) {
                                            when (item) {
                                                is SongItem -> {
                                                    YouTubeSongMenu(
                                                        song = item,
                                                        navController = navController,
                                                        onDismiss = menuState::dismiss,
                                                    )
                                                }

                                                is AlbumItem -> {
                                                    YouTubeAlbumMenu(
                                                        albumItem = item,
                                                        navController = navController,
                                                        onDismiss = menuState::dismiss,
                                                    )
                                                }

                                                is ArtistItem -> {
                                                    YouTubeArtistMenu(
                                                        artist = item,
                                                        onDismiss = menuState::dismiss,
                                                    )
                                                }

                                                is PlaylistItem -> {
                                                    YouTubePlaylistMenu(
                                                        playlist = item,
                                                        coroutineScope = coroutineScope,
                                                        onDismiss = menuState::dismiss,
                                                    )
                                                }

                                                is PodcastItem -> {
                                                    YouTubePlaylistMenu(
                                                        playlist = item.asPlaylistItem(),
                                                        coroutineScope = coroutineScope,
                                                        onDismiss = menuState::dismiss,
                                                    )
                                                }

                                                is EpisodeItem -> {
                                                    YouTubeSongMenu(
                                                        song = item.asSongItem(),
                                                        navController = navController,
                                                        onDismiss = menuState::dismiss,
                                                    )
                                                }
                                            }
                                        }
                                    },
                                )
                                .animateItem(),
                    )
                }

                if (itemsPage?.continuation != null) {
                    item(key = "loading") {
                        ShimmerHost(Modifier.animateItem()) {
                            GridItemPlaceHolder(fillMaxWidth = true)
                        }
                    }
                }
            }
        }
    }
}
