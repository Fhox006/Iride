/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import com.metrolist.innertube.models.AlbumItem
import com.metrolist.innertube.models.ArtistItem
import com.metrolist.innertube.models.EpisodeItem
import com.metrolist.innertube.models.PlaylistItem
import com.metrolist.innertube.models.PodcastItem
import com.metrolist.innertube.models.SongItem
import com.metrolist.innertube.models.isMixtape
import com.metrolist.music.LocalPlayerAwareWindowInsets
import com.metrolist.music.LocalPlayerConnection
import com.metrolist.music.R
import com.metrolist.music.constants.GridItemSize
import com.metrolist.music.constants.GridItemsSizeKey
import com.metrolist.music.constants.GridThumbnailHeight
import com.metrolist.music.constants.MainTopGradientKey
import com.metrolist.music.constants.PlayerBackgroundStyle
import com.metrolist.music.constants.PlayerBackgroundStyleKey
import com.metrolist.music.models.toMediaMetadata
import com.metrolist.music.ui.component.IconButton
import com.metrolist.music.ui.component.LocalMenuState
import com.metrolist.music.ui.component.NavigationTitle
import com.metrolist.music.ui.component.SettingsBackTopBar
import com.metrolist.music.ui.component.TopScreenGradientBackground
import com.metrolist.music.ui.component.YouTubeGridItem
import com.metrolist.music.ui.component.rememberFrostBackdrop
import com.metrolist.music.ui.component.recordFrostBackdrop
import com.metrolist.music.ui.component.shimmer.GridItemPlaceHolder
import com.metrolist.music.ui.component.shimmer.ShimmerHost
import com.metrolist.music.ui.menu.YouTubeAlbumMenu
import com.metrolist.music.ui.menu.YouTubeArtistMenu
import com.metrolist.music.ui.menu.YouTubePlaylistMenu
import com.metrolist.music.ui.menu.YouTubeSongMenu
import com.metrolist.music.ui.utils.backToMain
import com.metrolist.music.ui.utils.rememberDiscreteProgress
import com.metrolist.music.utils.rememberEnumPreference
import com.metrolist.music.utils.rememberPreference
import com.metrolist.music.viewmodels.YouTubeBrowseViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun YouTubeBrowseScreen(
    navController: NavController,
    viewModel: YouTubeBrowseViewModel = hiltViewModel(),
) {
    val menuState = LocalMenuState.current
    val haptic = LocalHapticFeedback.current
    val playerConnection = LocalPlayerConnection.current ?: return
    val isPlaying by playerConnection.isEffectivelyPlaying.collectAsState()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()

    val browseResult by viewModel.result.collectAsState()

    val coroutineScope = rememberCoroutineScope()
    val gridItemSize by rememberEnumPreference(GridItemsSizeKey, GridItemSize.BIG)

    val sortedSections = remember(browseResult) {
        val raw = browseResult?.items ?: return@remember emptyList()
        val seenIds = mutableSetOf<String>()

        val mixtapeSections = raw.filter { s -> s.items.any { it.isMixtape } }
        val songArtistSections = raw.filter { s ->
            !s.items.any { it.isMixtape } && s.items.any { it is SongItem || it is ArtistItem }
        }
        val restSections = raw.filter { s ->
            !s.items.any { it.isMixtape } && !s.items.any { it is SongItem || it is ArtistItem }
        }

        (mixtapeSections + songArtistSections + restSections).mapNotNull { section ->
            val uniqueItems = section.items.filter { seenIds.add(it.id) }
            if (uniqueItems.isEmpty()) null else section.copy(items = uniqueItems)
        }
    }

    val mainTopGradient by rememberPreference(MainTopGradientKey, defaultValue = true)
    val playerBackgroundStyle by rememberEnumPreference(PlayerBackgroundStyleKey, defaultValue = PlayerBackgroundStyle.BETTER_ANIMATED_GRADIENT)

    val gridState = rememberLazyGridState()
    val headerScrolled by remember {
        derivedStateOf {
            gridState.firstVisibleItemIndex > 0 || gridState.firstVisibleItemScrollOffset > 8
        }
    }
    val frostBackdrop = rememberFrostBackdrop()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .recordFrostBackdrop(frostBackdrop),
    ) {
        if (mainTopGradient) {
            TopScreenGradientBackground(
                mediaMetadata = mediaMetadata,
                playerBackground = playerBackgroundStyle,
            )
        } else {
            Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background))
        }
        LazyVerticalGrid(
            state = gridState,
            columns = GridCells.Adaptive(minSize = GridThumbnailHeight + if (gridItemSize == GridItemSize.BIG) 24.dp else (-24).dp),
            contentPadding = LocalPlayerAwareWindowInsets.current.asPaddingValues(),
        ) {
        if (browseResult == null) {
            items(8) {
                ShimmerHost {
                    GridItemPlaceHolder(fillMaxWidth = true)
                }
            }
        }

        sortedSections.forEach { section ->
            val sectionTitle = section.title
            if (!sectionTitle.isNullOrBlank()) {
                item(span = { GridItemSpan(maxCurrentLineSpan) }) {
                    NavigationTitle(title = sectionTitle)
                }
            }

            items(
                items = section.items,
                key = { "yt_browse_${it.id}" },
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
                        Modifier
                            .combinedClickable(
                                onClick = {
                                    when (item) {
                                        is SongItem -> {
                                            if (item.id == mediaMetadata?.id) {
                                                playerConnection.togglePlayPause()
                                            } else {
                                                playerConnection.startRadioForSong(item.toMediaMetadata())
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
                                                playerConnection.startRadioForSong(item.toMediaMetadata())
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
                            ).animateItem(),
                )
            }
        }
        }
    }

    SettingsBackTopBar(
        title = browseResult?.title.orEmpty(),
        navController = navController,
        backdrop = frostBackdrop,
        revealProgress = rememberDiscreteProgress(headerScrolled),
    )
}
