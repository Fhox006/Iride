/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.screens

import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.metrolist.innertube.models.WatchEndpoint
import com.metrolist.music.LocalPlayerAwareWindowInsets
import com.metrolist.music.LocalPlayerConnection
import com.metrolist.music.R
import com.metrolist.music.constants.ListItemHeight
import com.metrolist.music.constants.PureBlackKey
import com.metrolist.music.extensions.toMediaItem
import com.metrolist.music.models.toMediaMetadata
import com.metrolist.music.playback.queues.ListQueue
import com.metrolist.music.playback.queues.YouTubeQueue
import com.metrolist.music.ui.component.CollapsingScreenHeader
import com.metrolist.music.ui.component.IconButton
import com.metrolist.music.ui.component.LocalMenuState
import com.metrolist.music.ui.component.NavigationTitle
import com.metrolist.music.ui.component.YouTubeGridItem
import com.metrolist.music.ui.component.YouTubeListItem
import com.metrolist.music.ui.component.shimmer.GridItemPlaceHolder
import com.metrolist.music.ui.component.shimmer.ShimmerHost
import com.metrolist.music.ui.menu.YouTubeAlbumMenu
import com.metrolist.music.ui.menu.YouTubeSongMenu
import com.metrolist.music.ui.utils.SnapLayoutInfoProvider
import com.metrolist.music.utils.rememberPreference
import com.metrolist.music.viewmodels.NewsViewModel
import kotlinx.coroutines.launch

private const val ITEM_SONGS_TITLE = 3

private data class NewsCategory(
    val label: String,
    val thumbnail: String?,
    val onClick: () -> Unit,
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class, ExperimentalMaterial3ExpressiveApi::class)
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

    val newAlbums by viewModel.newAlbums.collectAsState()
    val quickPlaySongs by viewModel.quickPlaySongs.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    val pureBlack by rememberPreference(PureBlackKey, defaultValue = false)
    val coroutineScope = rememberCoroutineScope()
    val lazyListState = rememberLazyListState()

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(
        snapAnimationSpec = tween(durationMillis = 200),
    )

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            CollapsingScreenHeader(
                title = stringResource(R.string.news),
                scrollBehavior = scrollBehavior,
                pureBlack = pureBlack,
                isSearchActive = false,
                onSearchActiveChange = {},
                searchQuery = "",
                onSearchQueryChange = {},
                keyboardController = null,
            )
        },
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets(0),
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(if (pureBlack) Color.Black else MaterialTheme.colorScheme.background)
                .padding(paddingValues),
        ) {
            LazyColumn(
                state = lazyListState,
                contentPadding = LocalPlayerAwareWindowInsets.current
                    .only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom)
                    .asPaddingValues(),
                modifier = Modifier.fillMaxSize(),
            ) {
                item(key = "quick_access") {
                    val newAlbumLabel = stringResource(R.string.news_new_albums)
                    val newSongsLabel = stringResource(R.string.news_new_songs)
                    val chartsLabel = stringResource(R.string.news_charts)

                    val categories = remember(newAlbums, quickPlaySongs) {
                        listOf(
                            NewsCategory(newAlbumLabel, newAlbums.getOrNull(0)?.thumbnail) {
                                navController.navigate("new_release")
                            },
                            NewsCategory(newSongsLabel, quickPlaySongs.getOrNull(0)?.thumbnail) {
                                coroutineScope.launch { lazyListState.animateScrollToItem(ITEM_SONGS_TITLE) }
                            },
                            NewsCategory(
                                chartsLabel,
                                newAlbums.getOrNull(1)?.thumbnail ?: quickPlaySongs.getOrNull(1)?.thumbnail,
                            ) {
                                navController.navigate("charts_screen")
                            },
                        )
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp)
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        val carouselState = androidx.compose.material3.carousel.rememberCarouselState { categories.size }
                        androidx.compose.material3.carousel.HorizontalMultiBrowseCarousel(
                            state = carouselState,
                            preferredItemWidth = 220.dp,
                            itemSpacing = 12.dp,
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            modifier = Modifier.fillMaxWidth().height(150.dp),
                        ) { i ->
                            val category = categories[i]
                            NewsCategoryCard(
                                label = category.label,
                                thumbnail = category.thumbnail,
                                onClick = category.onClick,
                                modifier = Modifier.maskClip(MaterialTheme.shapes.extraLarge),
                            )
                        }
                    }
                }

                item(key = "albums_title") {
                    NavigationTitle(
                        title = stringResource(R.string.news_new_albums_title),
                    )
                }
                item(key = "albums_content") {
                    if (isLoading && newAlbums.isEmpty()) {
                        LazyRow(contentPadding = PaddingValues(horizontal = 16.dp)) {
                            items(5) {
                                ShimmerHost {
                                    Box(modifier = Modifier.size(160.dp).padding(8.dp))
                                }
                            }
                        }
                    } else if (newAlbums.isEmpty()) {
                        NewsEmptyText(stringResource(R.string.news_empty_albums))
                    } else {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            items(items = newAlbums, key = { "news_album_${it.id}" }) { album ->
                                YouTubeGridItem(
                                    item = album,
                                    isActive = mediaMetadata?.album?.id == album.id,
                                    isPlaying = isPlaying,
                                    coroutineScope = coroutineScope,
                                    size = 160.dp,
                                    modifier = Modifier.combinedClickable(
                                        onClick = { navController.navigate("album/${album.id}") },
                                        onLongClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
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
                        }
                    }
                }

                item(key = "songs_title") {
                    NavigationTitle(
                        title = stringResource(R.string.news_quick_play_title),
                        modifier = Modifier.padding(top = 8.dp),
                        onPlayAllClick = if (quickPlaySongs.isNotEmpty()) {
                            {
                                playerConnection.playQueue(
                                    ListQueue(
                                        title = "News",
                                        items = quickPlaySongs.map { it.toMediaItem() },
                                    ),
                                )
                            }
                        } else null,
                    )
                }
                item(key = "songs_content") {
                    if (isLoading && quickPlaySongs.isEmpty()) {
                        ShimmerHost {
                            repeat(2) { GridItemPlaceHolder() }
                        }
                    } else if (quickPlaySongs.isEmpty()) {
                        NewsEmptyText(stringResource(R.string.news_empty_songs))
                    } else {
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
                                contentPadding = WindowInsets.systemBars
                                    .only(WindowInsetsSides.Horizontal)
                                    .asPaddingValues(),
                                modifier = Modifier.fillMaxWidth().height(ListItemHeight * 4),
                            ) {
                                items(items = quickPlaySongs, key = { "news_song_${it.id}" }) { song ->
                                    YouTubeListItem(
                                        item = song,
                                        isActive = song.id == mediaMetadata?.id,
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
                                            .width(itemWidth)
                                            .combinedClickable(
                                                onClick = {
                                                    if (song.id == mediaMetadata?.id) {
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
                                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
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
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NewsCategoryCard(
    label: String,
    thumbnail: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(24.dp))
            .combinedClickable(onClick = onClick, onLongClick = {}),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(24.dp),
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (thumbnail != null) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(thumbnail)
                        .crossfade(true)
                        .build(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.55f),
                            ),
                        ),
                    ),
            )
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(16.dp),
            )
        }
    }
}

@Composable
private fun NewsEmptyText(text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}
