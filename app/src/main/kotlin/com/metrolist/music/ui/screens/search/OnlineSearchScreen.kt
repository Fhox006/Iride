/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.screens.search

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import com.metrolist.innertube.models.AlbumItem
import com.metrolist.innertube.models.ArtistItem
import com.metrolist.innertube.models.EpisodeItem
import com.metrolist.innertube.models.PlaylistItem
import com.metrolist.innertube.models.PodcastItem
import com.metrolist.innertube.models.SongItem
import com.metrolist.innertube.models.WatchEndpoint
import com.metrolist.music.LocalDatabase
import com.metrolist.music.LocalPlayerConnection
import com.metrolist.music.R
import com.metrolist.music.constants.ListThumbnailSize
import com.metrolist.music.constants.MainTopGradientKey
import com.metrolist.music.constants.PauseSearchHistoryKey
import com.metrolist.music.constants.SuggestionItemHeight
import com.metrolist.music.models.toMediaMetadata
import com.metrolist.music.ui.component.LocalMenuState
import com.metrolist.music.ui.component.rubberBandOverscroll
import com.metrolist.music.ui.component.YouTubeListItem
import com.metrolist.music.ui.menu.YouTubeAlbumMenu
import com.metrolist.music.ui.menu.YouTubeArtistMenu
import com.metrolist.music.ui.menu.YouTubePlaylistMenu
import com.metrolist.music.ui.menu.YouTubeSongMenu
import com.metrolist.music.ui.theme.SpaceMonoFontFamily
import com.metrolist.music.ui.utils.resize
import com.metrolist.music.playback.queues.YouTubeQueue
import com.metrolist.music.utils.recordSearchHistoryOpen
import com.metrolist.music.utils.rememberPreference
import com.metrolist.music.viewmodels.HomeViewModel
import com.metrolist.music.viewmodels.OnlineSearchSuggestionViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.drop

@OptIn(ExperimentalFoundationApi::class, ExperimentalComposeUiApi::class, ExperimentalMaterial3Api::class, FlowPreview::class)
@Composable
fun OnlineSearchScreen(
    query: String,
    onQueryChange: (TextFieldValue) -> Unit,
    navController: NavController,
    onSearch: (String) -> Unit,
    onDismiss: () -> Unit,
    pureBlack: Boolean,
    isFocused: Boolean = false,
    header: (@Composable () -> Unit)? = null,
    viewModel: OnlineSearchSuggestionViewModel = hiltViewModel(),
    homeViewModel: HomeViewModel = hiltViewModel(),
) {
    val database = LocalDatabase.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val menuState = LocalMenuState.current
    val playerConnection = LocalPlayerConnection.current ?: return

    val coroutineScope = rememberCoroutineScope()

    val haptic = LocalHapticFeedback.current
    val isPlaying by playerConnection.isEffectivelyPlaying.collectAsState()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()
    val viewState by viewModel.viewState.collectAsState()
    val explorePage by homeViewModel.explorePage.collectAsState()
    val discoveryWeeklyPlaylist by homeViewModel.discoveryWeeklyPlaylist.collectAsState()

    val mainTopGradient by rememberPreference(MainTopGradientKey, defaultValue = true)
    val pauseSearchHistory by rememberPreference(PauseSearchHistoryKey, defaultValue = false)

    val lazyListState = rememberLazyListState()

    LaunchedEffect(Unit) {
        snapshotFlow { lazyListState.firstVisibleItemScrollOffset }
            .drop(1)
            .collect {
                keyboardController?.hide()
            }
    }

    LaunchedEffect(query) {
        snapshotFlow { query }.debounce(300L).collectLatest {
            viewModel.query.value = it
        }
    }

    LaunchedEffect(Unit) {
        homeViewModel.syncDiscoveryWeeklyIfNeeded()
    }

    LaunchedEffect(explorePage?.moodAndGenres?.isNotEmpty()) {
        if (explorePage?.moodAndGenres?.isNotEmpty() == true && query.isEmpty() && !isFocused) {
            lazyListState.scrollToItem(0)
        }
    }

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .background(
                    when {
                        pureBlack -> Color.Black
                        mainTopGradient -> Color.Transparent
                        else -> MaterialTheme.colorScheme.background
                    },
                ),
    ) {
    header?.invoke()
    LazyColumn(
        state = lazyListState,
        contentPadding = WindowInsets.systemBars.only(WindowInsetsSides.Bottom).asPaddingValues(),
        modifier = Modifier
            .weight(1f)
            .rubberBandOverscroll(Orientation.Vertical, lazyListState),
    ) {
        if (query.isEmpty() && !isFocused) {
            val discoveryWeeklyEntry: Any =
                discoveryWeeklyPlaylist?.takeIf { it.songCount > 0 } ?: DiscoveryWeeklyPending
            val moods = explorePage?.moodAndGenres.orEmpty()
            item(key = "moods_header") {
                Text(
                    text = stringResource(R.string.mood_and_genres),
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontFamily = SpaceMonoFontFamily,
                        fontSize = 13.sp,
                        letterSpacing = (-0.1).sp,
                    ),
                    fontWeight = FontWeight.Bold,
                    color = Color.White.copy(alpha = 0.55f),
                    modifier = Modifier
                        .padding(
                            start = 20.dp,
                            end = 20.dp,
                            top = 20.dp,
                            bottom = 6.dp,
                        )
                        .animateItem(),
                )
            }

            val gridEntries: List<Any> = listOf(discoveryWeeklyEntry) + moods
            val gridRows = gridEntries.chunked(2)
            itemsIndexed(
                items = gridRows,
                key = { index, _ -> "mood_row_$index" },
            ) { _, row ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            horizontal = 20.dp,
                            vertical = 4.dp,
                        )
                        .animateItem(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    row.forEach { entry ->
                        when (entry) {
                            is com.metrolist.music.db.entities.Playlist -> {
                                DiscoveryWeeklyCard(
                                    thumbnails = entry.thumbnails,
                                    onClick = { navController.navigate("local_playlist/${entry.id}") },
                                    useIrideStyle = true,
                                    modifier = Modifier.weight(1f),
                                )
                            }
                            is com.metrolist.innertube.pages.MoodAndGenres.Item -> {
                                SearchMoodCard(
                                    title = entry.title,
                                    onClick = {
                                        navController.navigate("youtube_browse/${entry.endpoint.browseId}?params=${entry.endpoint.params}")
                                    },
                                    useIrideStyle = true,
                                    modifier = Modifier.weight(1f),
                                )
                            }
                            is DiscoveryWeeklyPending -> {
                                DiscoveryWeeklyCard(
                                    thumbnails = emptyList(),
                                    onClick = {},
                                    useIrideStyle = true,
                                    pendingLabel = stringResource(R.string.discovery_weekly_creating),
                                    modifier = Modifier.weight(1f),
                                )
                            }
                        }
                    }
                    if (row.size < 2) {
                        Spacer(Modifier.weight(1f))
                    }
                }
            }

            if (false) item(key = "genres_card") {
                ElevatedCard(
                    onClick = { navController.navigate("genres_screen") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(96.dp)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    ),
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                        contentAlignment = Alignment.CenterStart,
                    ) {
                        Column {
                            Text(
                                text = "Find your genres",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                            )
                            Text(
                                text = "Explore all music genres",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f),
                            )
                        }
                        Icon(
                            painter = painterResource(R.drawable.music_note),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.3f),
                            modifier = Modifier
                                .size(64.dp)
                                .align(Alignment.CenterEnd),
                        )
                    }
                }
            }
        } else {

            if (viewState.isUrlQuery && viewState.parsedUrlItem != null) {
                item(key = "parsed_url_header") {
                    Column(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.parsed_from_link),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(bottom = 8.dp),
                        )
                    }
                }

                item(key = "parsed_url_item") {
                    val item = viewState.parsedUrlItem!!
                    YouTubeListItem(
                        item = item,
                        isActive =
                            when (item) {
                                is SongItem -> mediaMetadata?.id == item.id
                                is AlbumItem -> mediaMetadata?.album?.id == item.id
                                is EpisodeItem -> mediaMetadata?.id == item.id
                                else -> false
                            },
                        isPlaying = isPlaying,
                        trailingContent = {
                            IconButton(
                                onClick = {
                                    menuState.show {
                                        when (item) {
                                            is SongItem -> {
                                                YouTubeSongMenu(
                                                    song = item,
                                                    navController = navController,
                                                    onDismiss = {
                                                        menuState.dismiss()
                                                        onDismiss()
                                                    },
                                                )
                                            }

                                            is AlbumItem -> {
                                                YouTubeAlbumMenu(
                                                    albumItem = item,
                                                    navController = navController,
                                                    onDismiss = {
                                                        menuState.dismiss()
                                                        onDismiss()
                                                    },
                                                )
                                            }

                                            is ArtistItem -> {
                                                YouTubeArtistMenu(
                                                    artist = item,
                                                    onDismiss = {
                                                        menuState.dismiss()
                                                        onDismiss()
                                                    },
                                                )
                                            }

                                            is PlaylistItem -> {
                                                YouTubePlaylistMenu(
                                                    playlist = item,
                                                    coroutineScope = coroutineScope,
                                                    onDismiss = {
                                                        menuState.dismiss()
                                                        onDismiss()
                                                    },
                                                )
                                            }

                                            is PodcastItem -> {
                                                YouTubePlaylistMenu(
                                                    playlist = item.asPlaylistItem(),
                                                    coroutineScope = coroutineScope,
                                                    onDismiss = {
                                                        menuState.dismiss()
                                                        onDismiss()
                                                    },
                                                )
                                            }

                                            is EpisodeItem -> {
                                                YouTubeSongMenu(
                                                    song = item.asSongItem(),
                                                    navController = navController,
                                                    onDismiss = {
                                                        menuState.dismiss()
                                                        onDismiss()
                                                    },
                                                )
                                            }
                                        }
                                    }
                                },
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.more_vert),
                                    contentDescription = null,
                                )
                            }
                        },
                        modifier =
                            Modifier
                                .combinedClickable(
                                    onClick = {
                                        if (!pauseSearchHistory) database.recordSearchHistoryOpen(query, item)
                                        when (item) {
                                            is SongItem -> {
                                                if (item.id == mediaMetadata?.id) {
                                                    playerConnection.togglePlayPause()
                                                } else {
                                                    playerConnection.playQueue(YouTubeQueue.radio(item.toMediaMetadata()))
                                                    onDismiss()
                                                }
                                            }

                                            is AlbumItem -> {
                                                navController.navigate("album/${item.id}")
                                                onDismiss()
                                            }

                                            is ArtistItem -> {
                                                navController.navigate("artist/${item.id}")
                                                onDismiss()
                                            }

                                            is PlaylistItem -> {
                                                navController.navigate("online_playlist/${item.id}")
                                                onDismiss()
                                            }

                                            is PodcastItem -> {
                                                navController.navigate("online_podcast/${item.id}")
                                                onDismiss()
                                            }

                                            is EpisodeItem -> {
                                                if (item.id == mediaMetadata?.id) {
                                                    playerConnection.togglePlayPause()
                                                } else {
                                                    playerConnection.playQueue(YouTubeQueue.radio(item.toMediaMetadata()))
                                                    onDismiss()
                                                }
                                            }
                                        }
                                    },
                                    onLongClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        menuState.show {
                                            when (item) {
                                                is SongItem -> {
                                                    YouTubeSongMenu(
                                                        song = item,
                                                        navController = navController,
                                                        onDismiss = {
                                                            menuState.dismiss()
                                                            onDismiss()
                                                        },
                                                    )
                                                }

                                                is AlbumItem -> {
                                                    YouTubeAlbumMenu(
                                                        albumItem = item,
                                                        navController = navController,
                                                        onDismiss = {
                                                            menuState.dismiss()
                                                            onDismiss()
                                                        },
                                                    )
                                                }

                                                is ArtistItem -> {
                                                    YouTubeArtistMenu(
                                                        artist = item,
                                                        onDismiss = {
                                                            menuState.dismiss()
                                                            onDismiss()
                                                        },
                                                    )
                                                }

                                                is PlaylistItem -> {
                                                    YouTubePlaylistMenu(
                                                        playlist = item,
                                                        coroutineScope = coroutineScope,
                                                        onDismiss = {
                                                            menuState.dismiss()
                                                            onDismiss()
                                                        },
                                                    )
                                                }

                                                is PodcastItem -> {
                                                    YouTubePlaylistMenu(
                                                        playlist = item.asPlaylistItem(),
                                                        coroutineScope = coroutineScope,
                                                        onDismiss = {
                                                            menuState.dismiss()
                                                            onDismiss()
                                                        },
                                                    )
                                                }

                                                is EpisodeItem -> {
                                                    YouTubeSongMenu(
                                                        song = item.asSongItem(),
                                                        navController = navController,
                                                        onDismiss = {
                                                            menuState.dismiss()
                                                            onDismiss()
                                                        },
                                                    )
                                                }
                                            }
                                        }
                                    },
                                ).background(if (pureBlack) Color.Black else MaterialTheme.colorScheme.surface)
                                .animateItem(),
                    )
                }

                item(key = "parsed_url_divider") {
                    HorizontalDivider(
                        color = Color.White.copy(alpha = 0.08f),
                        modifier =
                            Modifier
                                .padding(vertical = 8.dp)
                                .animateItem(),
                    )
                }
            }

            items(viewState.history, key = { "history_${it.query}" }) { history ->
                SuggestionItem(
                    query = history.title ?: history.query,
                    online = false,
                    thumbnailUrl = history.thumbnailUrl,
                    isArtistThumbnail = history.itemType == "artist",
                    onClick = {
                        when (history.itemType) {
                            "song" -> {
                                if (history.itemId == mediaMetadata?.id) {
                                    playerConnection.togglePlayPause()
                                } else {
                                    playerConnection.playQueue(YouTubeQueue(WatchEndpoint(videoId = history.itemId!!)))
                                }
                                onDismiss()
                            }
                            "album" -> { navController.navigate("album/${history.itemId}"); onDismiss() }
                            "artist" -> { navController.navigate("artist/${history.itemId}"); onDismiss() }
                            "playlist" -> { navController.navigate("online_playlist/${history.itemId}"); onDismiss() }
                            else -> {
                                onSearch(history.query)
                                onDismiss()
                            }
                        }
                    },
                    onDelete = {
                        database.query {
                            delete(history)
                        }
                    },
                    onFillTextField = {
                        onQueryChange(TextFieldValue(history.query, TextRange(history.query.length)))
                    },
                    modifier = Modifier.animateItem(),
                    pureBlack = pureBlack,
                    useIrideStyle = true,
                )
            }

            items(viewState.suggestions, key = { "suggestion_$it" }) { query ->
                SuggestionItem(
                    query = query,
                    online = true,
                    onClick = {
                        onSearch(query)
                        onDismiss()
                    },
                    onFillTextField = {
                        onQueryChange(TextFieldValue(query, TextRange(query.length)))
                    },
                    modifier = Modifier.animateItem(),
                    pureBlack = pureBlack,
                    useIrideStyle = true,
                )
            }

            if (viewState.items.isNotEmpty() && viewState.history.size + viewState.suggestions.size > 0) {
                item(key = "search_divider") {
                    HorizontalDivider(
                        color = Color.White.copy(alpha = 0.08f),
                        modifier = Modifier.animateItem(),
                    )
                }
                item(key = "search_divider_spacer") {
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            items(viewState.items, key = { "item_${it.id}" }) { item ->
                YouTubeListItem(
                    item = item,
                    isActive =
                        when (item) {
                            is SongItem -> mediaMetadata?.id == item.id
                            is AlbumItem -> mediaMetadata?.album?.id == item.id
                            is EpisodeItem -> mediaMetadata?.id == item.id
                            else -> false
                        },
                    isPlaying = isPlaying,
                    trailingContent = {
                        IconButton(
                            onClick = {
                                menuState.show {
                                    when (item) {
                                        is SongItem -> {
                                            YouTubeSongMenu(
                                                song = item,
                                                navController = navController,
                                                onDismiss = {
                                                    menuState.dismiss()
                                                    onDismiss()
                                                },
                                            )
                                        }

                                        is AlbumItem -> {
                                            YouTubeAlbumMenu(
                                                albumItem = item,
                                                navController = navController,
                                                onDismiss = {
                                                    menuState.dismiss()
                                                    onDismiss()
                                                },
                                            )
                                        }

                                        is ArtistItem -> {
                                            YouTubeArtistMenu(
                                                artist = item,
                                                onDismiss = {
                                                    menuState.dismiss()
                                                    onDismiss()
                                                },
                                            )
                                        }

                                        is PlaylistItem -> {
                                            YouTubePlaylistMenu(
                                                playlist = item,
                                                coroutineScope = coroutineScope,
                                                onDismiss = {
                                                    menuState.dismiss()
                                                    onDismiss()
                                                },
                                            )
                                        }

                                        is PodcastItem -> {
                                            YouTubePlaylistMenu(
                                                playlist = item.asPlaylistItem(),
                                                coroutineScope = coroutineScope,
                                                onDismiss = {
                                                    menuState.dismiss()
                                                    onDismiss()
                                                },
                                            )
                                        }

                                        is EpisodeItem -> {
                                            YouTubeSongMenu(
                                                song = item.asSongItem(),
                                                navController = navController,
                                                onDismiss = {
                                                    menuState.dismiss()
                                                    onDismiss()
                                                },
                                            )
                                        }
                                    }
                                }
                            },
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.more_vert),
                                contentDescription = null,
                            )
                        }
                    },
                    modifier =
                        Modifier
                            .combinedClickable(
                                onClick = {
                                    if (!pauseSearchHistory) database.recordSearchHistoryOpen(query, item)
                                    when (item) {
                                        is SongItem -> {
                                            if (item.id == mediaMetadata?.id) {
                                                playerConnection.togglePlayPause()
                                            } else {
                                                playerConnection.playQueue(YouTubeQueue.radio(item.toMediaMetadata()))
                                                onDismiss()
                                            }
                                        }

                                        is AlbumItem -> {
                                            navController.navigate("album/${item.id}")
                                            onDismiss()
                                        }

                                        is ArtistItem -> {
                                            navController.navigate("artist/${item.id}")
                                            onDismiss()
                                        }

                                        is PlaylistItem -> {
                                            navController.navigate("online_playlist/${item.id}")
                                            onDismiss()
                                        }

                                        is PodcastItem -> {
                                            navController.navigate("online_podcast/${item.id}")
                                            onDismiss()
                                        }

                                        is EpisodeItem -> {
                                            if (item.id == mediaMetadata?.id) {
                                                playerConnection.togglePlayPause()
                                            } else {
                                                playerConnection.playQueue(YouTubeQueue.radio(item.toMediaMetadata()))
                                                onDismiss()
                                            }
                                        }
                                    }
                                },
                                onLongClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    menuState.show {
                                        when (item) {
                                            is SongItem -> {
                                                YouTubeSongMenu(
                                                    song = item,
                                                    navController = navController,
                                                    onDismiss = {
                                                        menuState.dismiss()
                                                        onDismiss()
                                                    },
                                                )
                                            }

                                            is AlbumItem -> {
                                                YouTubeAlbumMenu(
                                                    albumItem = item,
                                                    navController = navController,
                                                    onDismiss = {
                                                        menuState.dismiss()
                                                        onDismiss()
                                                    },
                                                )
                                            }

                                            is ArtistItem -> {
                                                YouTubeArtistMenu(
                                                    artist = item,
                                                    onDismiss = {
                                                        menuState.dismiss()
                                                        onDismiss()
                                                    },
                                                )
                                            }

                                            is PlaylistItem -> {
                                                YouTubePlaylistMenu(
                                                    playlist = item,
                                                    coroutineScope = coroutineScope,
                                                    onDismiss = {
                                                        menuState.dismiss()
                                                        onDismiss()
                                                    },
                                                )
                                            }

                                            is PodcastItem -> {
                                                YouTubePlaylistMenu(
                                                    playlist = item.asPlaylistItem(),
                                                    coroutineScope = coroutineScope,
                                                    onDismiss = {
                                                        menuState.dismiss()
                                                        onDismiss()
                                                    },
                                                )
                                            }

                                            is EpisodeItem -> {
                                                YouTubeSongMenu(
                                                    song = item.asSongItem(),
                                                    navController = navController,
                                                    onDismiss = {
                                                        menuState.dismiss()
                                                        onDismiss()
                                                    },
                                                )
                                            }
                                        }
                                    }
                                },
                            ).background(if (pureBlack) Color.Black else MaterialTheme.colorScheme.surface)
                            .animateItem(),
                )
            }
        }

        item(key = "pill_spacer") {
            Spacer(modifier = Modifier.height(136.dp))
        }
    }
    }
}

private object DiscoveryWeeklyPending

@Composable
private fun SearchMoodCard(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    useIrideStyle: Boolean = false,
) {
    if (useIrideStyle) {
        Box(
            contentAlignment = Alignment.BottomStart,
            modifier = modifier
                .height(72.dp)
                .clip(RoundedCornerShape(5.dp))
                .background(Color.White.copy(alpha = 0.06f))
                .clickable(onClick = onClick)
                .padding(12.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontFamily = SpaceMonoFontFamily,
                    fontSize = 13.sp,
                    letterSpacing = (-0.1).sp,
                ),
                fontWeight = FontWeight.SemiBold,
                color = Color.White.copy(alpha = 0.85f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        return
    }

    Box(
        contentAlignment = Alignment.BottomStart,
        modifier = modifier
            .height(80.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.secondaryContainer)
            .clickable(onClick = onClick)
            .padding(12.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun DiscoveryWeeklyCard(
    thumbnails: List<String>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    useIrideStyle: Boolean = false,
    pendingLabel: String? = null,
) {
    val mosaicThumbnails = thumbnails.distinct().take(4)
    val height = if (useIrideStyle) 72.dp else 80.dp

    Box(
        modifier = modifier
            .height(height)
            .clip(RoundedCornerShape(if (useIrideStyle) 5.dp else 18.dp))
            .background(if (useIrideStyle) Color.White.copy(alpha = 0.06f) else MaterialTheme.colorScheme.secondaryContainer)
            .then(if (pendingLabel == null) Modifier.clickable(onClick = onClick) else Modifier),
    ) {
        if (mosaicThumbnails.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .graphicsLayer(scaleX = 1.6f, scaleY = 1.6f)
                    .blur(height * 0.25f),
            ) {
                if (mosaicThumbnails.size == 1) {
                    AsyncImage(
                        model = mosaicThumbnails[0],
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    Column(Modifier.fillMaxSize()) {
                        Row(Modifier.weight(1f).fillMaxWidth()) {
                            AsyncImage(
                                model = mosaicThumbnails.getOrElse(0) { mosaicThumbnails[0] },
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.weight(1f).fillMaxHeight(),
                            )
                            AsyncImage(
                                model = mosaicThumbnails.getOrElse(1) { mosaicThumbnails[0] },
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.weight(1f).fillMaxHeight(),
                            )
                        }
                        Row(Modifier.weight(1f).fillMaxWidth()) {
                            AsyncImage(
                                model = mosaicThumbnails.getOrElse(2) { mosaicThumbnails[0] },
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.weight(1f).fillMaxHeight(),
                            )
                            AsyncImage(
                                model = mosaicThumbnails.getOrElse(3) { mosaicThumbnails[0] },
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.weight(1f).fillMaxHeight(),
                            )
                        }
                    }
                }
            }
            Box(modifier = Modifier.matchParentSize().background(Color.Black.copy(alpha = 0.45f)))
        }

        val onCardColor = if (mosaicThumbnails.isNotEmpty() || useIrideStyle) {
            Color.White
        } else {
            MaterialTheme.colorScheme.onSecondaryContainer
        }

        if (pendingLabel != null) {
            CircularProgressIndicator(
                color = onCardColor.copy(alpha = 0.7f),
                strokeWidth = 2.dp,
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(if (useIrideStyle) 18.dp else 22.dp),
            )
        } else {
            Icon(
                painter = painterResource(R.drawable.language),
                contentDescription = null,
                tint = onCardColor.copy(alpha = if (mosaicThumbnails.isNotEmpty() || useIrideStyle) 0.9f else 1f),
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset(x = 10.dp)
                    .size(if (useIrideStyle) 22.dp else 26.dp),
            )
        }

        Text(
            text = pendingLabel ?: stringResource(R.string.discovery_weekly),
            style = if (useIrideStyle) {
                MaterialTheme.typography.labelLarge.copy(
                    fontFamily = SpaceMonoFontFamily,
                    fontSize = 13.sp,
                    letterSpacing = (-0.1).sp,
                )
            } else {
                MaterialTheme.typography.titleSmall
            },
            fontWeight = FontWeight.Bold,
            color = onCardColor,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(12.dp),
        )
    }
}


@Composable
fun SuggestionItem(
    modifier: Modifier = Modifier,
    query: String,
    online: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit = {},
    onFillTextField: () -> Unit,
    pureBlack: Boolean,
    useIrideStyle: Boolean = false,
    thumbnailUrl: String? = null,
    isArtistThumbnail: Boolean = false,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier =
            modifier
                .fillMaxWidth()
                .height(SuggestionItemHeight)
                .background(
                    when {
                        pureBlack -> Color.Black
                        useIrideStyle -> Color.Transparent
                        else -> MaterialTheme.colorScheme.surface
                    },
                )
                .clickable(onClick = onClick)
                .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Horizontal)),
    ) {
        if (thumbnailUrl != null) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(thumbnailUrl.resize(192, 192))
                    .memoryCachePolicy(coil3.request.CachePolicy.ENABLED)
                    .diskCachePolicy(coil3.request.CachePolicy.ENABLED)
                    .networkCachePolicy(coil3.request.CachePolicy.ENABLED)
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .padding(start = if (useIrideStyle) 20.dp else 12.dp, end = 12.dp)
                    .size(ListThumbnailSize)
                    .clip(if (isArtistThumbnail) CircleShape else RoundedCornerShape(4.dp)),
            )
        } else if (useIrideStyle) {
            Spacer(modifier = Modifier.width(20.dp))
        } else {
            Icon(
                painterResource(if (online) R.drawable.search else R.drawable.history),
                contentDescription = null,
                tint = LocalContentColor.current,
                modifier = Modifier.padding(horizontal = 16.dp).alpha(0.5f),
            )
        }

        Text(
            text = query,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = if (useIrideStyle) {
                MaterialTheme.typography.bodyMedium.copy(fontFamily = SpaceMonoFontFamily, fontSize = 13.sp)
            } else {
                MaterialTheme.typography.bodyLarge
            },
            color = if (useIrideStyle) Color.White.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f).padding(end = if (useIrideStyle) 12.dp else 0.dp),
        )

        if (!online) {
            IconButton(
                onClick = onDelete,
                modifier = Modifier.alpha(if (useIrideStyle) 1f else 0.5f),
            ) {
                Icon(
                    painter = painterResource(R.drawable.close),
                    contentDescription = null,
                    tint = if (useIrideStyle) Color.White.copy(alpha = 0.35f) else LocalContentColor.current,
                )
            }
        }

        IconButton(
            onClick = onFillTextField,
            modifier = Modifier.alpha(if (useIrideStyle) 1f else 0.5f),
        ) {
            Icon(
                painter = painterResource(R.drawable.arrow_top_left),
                contentDescription = null,
                tint = if (useIrideStyle) Color.White.copy(alpha = 0.35f) else LocalContentColor.current,
            )
        }
    }
}
