/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.screens.search

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.metrolist.music.LocalPlayerConnection
import com.metrolist.music.R
import com.metrolist.music.constants.CONTENT_TYPE_LIST
import com.metrolist.music.constants.ListItemHeight
import com.metrolist.music.constants.MainTopGradientKey
import com.metrolist.music.constants.TopNavigationBarKey
import com.metrolist.music.db.entities.Album
import com.metrolist.music.db.entities.Artist
import com.metrolist.music.db.entities.Playlist
import com.metrolist.music.db.entities.Song
import com.metrolist.music.extensions.toMediaItem
import com.metrolist.music.playback.queues.ListQueue
import com.metrolist.music.ui.component.AlbumListItem
import com.metrolist.music.ui.component.ArtistListItem
import com.metrolist.music.ui.component.ChipsRow
import com.metrolist.music.ui.component.EmptyPlaceholder
import com.metrolist.music.ui.component.LocalMenuState
import com.metrolist.music.ui.component.PlaylistListItem
import com.metrolist.music.ui.component.SongListItem
import com.metrolist.music.ui.menu.SongMenu
import com.metrolist.music.utils.rememberPreference
import com.metrolist.music.viewmodels.LocalFilter
import com.metrolist.music.viewmodels.LocalSearchViewModel
import kotlinx.coroutines.flow.drop

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LocalSearchScreen(
    query: String,
    navController: NavController,
    onDismiss: () -> Unit,
    isFromCache: Boolean = false,
    pureBlack: Boolean,
    // New Iride UI: leading scrollable item (TopNavigationBar + search box) — see SearchScreen,
    // which renders this in place of its own pinned header so it scrolls away with the rest of the
    // list instead of staying fixed on top, exactly like HomeScreen.
    header: (@Composable () -> Unit)? = null,
    viewModel: LocalSearchViewModel = hiltViewModel(),
) {
    val queueSearchedSongsStr = stringResource(R.string.queue_searched_songs)
    val keyboardController = LocalSoftwareKeyboardController.current
    val menuState = LocalMenuState.current
    val playerConnection = LocalPlayerConnection.current ?: return

    val isPlaying by playerConnection.isEffectivelyPlaying.collectAsState()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()

    val searchFilter by viewModel.filter.collectAsState()
    val result by viewModel.result.collectAsState()

    val topNavigationBarEnabled by rememberPreference(TopNavigationBarKey, defaultValue = false)
    val mainTopGradient by rememberPreference(MainTopGradientKey, defaultValue = false)

    val lazyListState = rememberLazyListState()

    LaunchedEffect(Unit) {
        snapshotFlow { lazyListState.firstVisibleItemScrollOffset }
            .drop(1)
            .collect {
                keyboardController?.hide()
            }
    }

    LaunchedEffect(query) {
        viewModel.query.value = query
    }

    val configuration = LocalWindowInfo.current
    val isLandscape = configuration.containerSize.width > configuration.containerSize.height

    val chipsRow: @Composable () -> Unit = {
        ChipsRow(
            chips =
                listOf(
                    LocalFilter.ALL to stringResource(R.string.filter_all),
                    LocalFilter.SONG to stringResource(R.string.filter_songs),
                    LocalFilter.ALBUM to stringResource(R.string.filter_albums),
                    LocalFilter.ARTIST to stringResource(R.string.filter_artists),
                    LocalFilter.PLAYLIST to stringResource(R.string.filter_playlists),
                ),
            currentValue = searchFilter,
            onValueUpdate = { viewModel.filter.value = it },
            horizontalPadding = if (topNavigationBarEnabled) 20.dp else 12.dp,
            useIrideStyle = topNavigationBarEnabled,
        )
    }

    if (header != null) {
        // New Iride UI: no pinned chrome at all — the header and the filter chips scroll away
        // together with the results, exactly like HomeScreen, instead of the classic UI's
        // pinned ChipsRow above a separately-scrolling result list.
        LazyColumn(
            state = lazyListState,
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(
                        when {
                            pureBlack -> Color.Black
                            mainTopGradient -> Color.Transparent
                            else -> MaterialTheme.colorScheme.background
                        },
                    )
                    .let { base ->
                        if (isLandscape) {
                            base.windowInsetsPadding(
                                WindowInsets.systemBars.only(WindowInsetsSides.Horizontal),
                            )
                        } else {
                            base
                        }
                    },
            contentPadding =
                WindowInsets.systemBars
                    .only(WindowInsetsSides.Bottom)
                    .asPaddingValues(),
        ) {
            item(key = "search_header") { header() }
            item(key = "local_search_chips") { chipsRow() }
            localSearchResultItems(
                result = result,
                navController = navController,
                menuState = menuState,
                onDismiss = onDismiss,
                isFromCache = isFromCache,
                isPlaying = isPlaying,
                mediaMetadata = mediaMetadata,
                queueSearchedSongsStr = queueSearchedSongsStr,
                onFilterChange = { viewModel.filter.value = it },
                playerConnection = playerConnection,
                useIrideStyle = topNavigationBarEnabled,
            )
        }
        return
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
                )
                .let { base ->
                    if (isLandscape) {
                        base.windowInsetsPadding(
                            WindowInsets.systemBars.only(WindowInsetsSides.Horizontal),
                        )
                    } else {
                        base
                    }
                },
    ) {
        chipsRow()

        LazyColumn(
            state = lazyListState,
            modifier = Modifier.weight(1f),
            contentPadding =
                WindowInsets.systemBars
                    .only(WindowInsetsSides.Bottom)
                    .asPaddingValues(),
        ) {
            localSearchResultItems(
                result = result,
                navController = navController,
                menuState = menuState,
                onDismiss = onDismiss,
                isFromCache = isFromCache,
                isPlaying = isPlaying,
                mediaMetadata = mediaMetadata,
                queueSearchedSongsStr = queueSearchedSongsStr,
                onFilterChange = { viewModel.filter.value = it },
                playerConnection = playerConnection,
                useIrideStyle = topNavigationBarEnabled,
            )
        }
    }
}

private fun LazyListScope.localSearchResultItems(
    result: com.metrolist.music.viewmodels.LocalSearchResult,
    navController: NavController,
    menuState: com.metrolist.music.ui.component.MenuState,
    onDismiss: () -> Unit,
    isFromCache: Boolean,
    isPlaying: Boolean,
    mediaMetadata: com.metrolist.music.models.MediaMetadata?,
    queueSearchedSongsStr: String,
    onFilterChange: (LocalFilter) -> Unit,
    playerConnection: com.metrolist.music.playback.PlayerConnection,
    useIrideStyle: Boolean = false,
) {
            result.map.forEach { (filter, items) ->
                if (result.filter == LocalFilter.ALL) {
                    item(key = filter) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .height(ListItemHeight)
                                    .clickable { onFilterChange(filter) }
                                    .padding(
                                        start = if (useIrideStyle) 20.dp else 12.dp,
                                        end = if (useIrideStyle) 20.dp else 18.dp,
                                    ),
                        ) {
                            Text(
                                text =
                                    stringResource(
                                        when (filter) {
                                            LocalFilter.SONG -> R.string.filter_songs
                                            LocalFilter.ALBUM -> R.string.filter_albums
                                            LocalFilter.ARTIST -> R.string.filter_artists
                                            LocalFilter.PLAYLIST -> R.string.filter_playlists
                                            LocalFilter.ALL -> error("")
                                        },
                                    ),
                                style = if (useIrideStyle) {
                                    MaterialTheme.typography.labelLarge.copy(
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 13.sp,
                                        letterSpacing = (-0.1).sp,
                                    )
                                } else {
                                    MaterialTheme.typography.titleLarge
                                },
                                fontWeight = if (useIrideStyle) FontWeight.Bold else FontWeight.Normal,
                                color = if (useIrideStyle) Color.White.copy(alpha = 0.35f) else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f),
                            )

                            Icon(
                                painter = painterResource(R.drawable.navigate_next),
                                contentDescription = null,
                                tint = if (useIrideStyle) Color.White.copy(alpha = 0.35f) else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(if (useIrideStyle) 18.dp else 24.dp),
                            )
                        }
                    }
                }

                items(
                    items = items.distinctBy { it.id },
                    key = { "search_local_${it.id}" },
                    contentType = { CONTENT_TYPE_LIST },
                ) { item ->
                    when (item) {
                        is Song -> {
                            SongListItem(
                                song = item,
                                isActive = item.id == mediaMetadata?.id,
                                isPlaying = isPlaying,
                                trailingContent = {
                                    IconButton(
                                        onClick = {
                                            menuState.show {
                                                SongMenu(
                                                    originalSong = item,
                                                    navController = navController,
                                                    onDismiss = {
                                                        onDismiss()
                                                        menuState.dismiss()
                                                    },
                                                    isFromCache = isFromCache,
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
                                modifier =
                                    Modifier
                                        .combinedClickable(
                                            onClick = {
                                                if (item.id == mediaMetadata?.id) {
                                                    playerConnection.togglePlayPause()
                                                } else {
                                                    val songs =
                                                        result.map
                                                            .getOrDefault(LocalFilter.SONG, emptyList())
                                                            .filterIsInstance<Song>()
                                                            .map { it.toMediaItem() }
                                                    playerConnection.playQueue(
                                                        ListQueue(
                                                            title = queueSearchedSongsStr,
                                                            items = songs,
                                                            startIndex = songs.indexOfFirst { it.mediaId == item.id },
                                                        ),
                                                    )
                                                }
                                            },
                                            onLongClick = {
                                                menuState.show {
                                                    SongMenu(
                                                        originalSong = item,
                                                        navController = navController,
                                                        onDismiss = {
                                                            onDismiss()
                                                            menuState.dismiss()
                                                        },
                                                        isFromCache = isFromCache,
                                                    )
                                                }
                                            },
                                        ).animateItem(),
                            )
                        }

                        is Album -> {
                            AlbumListItem(
                                album = item,
                                isActive = item.id == mediaMetadata?.album?.id,
                                isPlaying = isPlaying,
                                modifier =
                                    Modifier
                                        .clickable {
                                            onDismiss()
                                            navController.navigate("album/${item.id}")
                                        }.animateItem(),
                            )
                        }

                        is Artist -> {
                            ArtistListItem(
                                artist = item,
                                modifier =
                                    Modifier
                                        .clickable {
                                            onDismiss()
                                            navController.navigate("artist/${item.id}")
                                        }.animateItem(),
                            )
                        }

                        is Playlist -> {
                            PlaylistListItem(
                                playlist = item,
                                modifier =
                                    Modifier
                                        .clickable {
                                            onDismiss()
                                            navController.navigate("local_playlist/${item.id}")
                                        }.animateItem(),
                            )
                        }
                    }
                }
            }

            if (result.query.isNotEmpty() && result.map.isEmpty()) {
                item(key = "no_result") {
                    EmptyPlaceholder(
                        icon = R.drawable.search,
                        text = stringResource(R.string.no_results_found),
                    )
                }
            }
}
