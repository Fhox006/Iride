/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import com.metrolist.innertube.models.WatchEndpoint
import com.metrolist.music.LocalPlayerAwareWindowInsets
import com.metrolist.music.LocalPlayerConnection
import com.metrolist.music.extensions.toMediaItem
import com.metrolist.music.models.toMediaMetadata
import com.metrolist.music.playback.queues.ListQueue
import com.metrolist.music.playback.queues.YouTubeQueue
import com.metrolist.music.ui.component.LocalMenuState
import com.metrolist.music.ui.component.NavigationTitle
import com.metrolist.music.ui.component.YouTubeGridItem
import com.metrolist.music.ui.component.shimmer.ShimmerHost
import com.metrolist.music.ui.menu.YouTubeAlbumMenu
import com.metrolist.music.ui.menu.YouTubeSongMenu
import com.metrolist.music.viewmodels.GlobalTop50ViewModel
import com.metrolist.music.viewmodels.WhatNewViewModel

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun WhatNewScreen(
    navController: NavController,
    viewModel: WhatNewViewModel = hiltViewModel(),
    globalTop50ViewModel: GlobalTop50ViewModel = hiltViewModel(),
) {
    val menuState = LocalMenuState.current
    val haptic = LocalHapticFeedback.current
    val playerConnection = LocalPlayerConnection.current ?: return
    val isPlaying by playerConnection.isEffectivelyPlaying.collectAsState()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()

    val recentAlbums by viewModel.recentAlbums.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val globalTop50Songs by globalTop50ViewModel.songs.collectAsState()
    val isGlobalLoading by globalTop50ViewModel.isLoading.collectAsState()
    val globalError by globalTop50ViewModel.error.collectAsState()

    val coroutineScope = rememberCoroutineScope()
    val lazyListState = rememberLazyListState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("What's New") },
            )
        },
        contentWindowInsets = LocalPlayerAwareWindowInsets.current
    ) { paddingValues ->
        LazyColumn(
            state = lazyListState,
            contentPadding = paddingValues,
            modifier = Modifier.fillMaxSize()
        ) {
            item {
                NavigationTitle(
                    title = "Recent Releases",
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            item {
                if (isLoading && recentAlbums.isEmpty()) {
                    LazyRow(contentPadding = PaddingValues(horizontal = 16.dp)) {
                        items(5) {
                            ShimmerHost {
                                Box(modifier = Modifier.size(200.dp).padding(8.dp))
                            }
                        }
                    }
                } else if (recentAlbums.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = androidx.compose.ui.Alignment.Center
                    ) {
                        Text(
                            text = "No recent releases found.\nFollow or listen to artists to see their news.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                } else {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(recentAlbums) { album ->
                            YouTubeGridItem(
                                item = album,
                                isActive = mediaMetadata?.album?.id == album.id,
                                isPlaying = isPlaying,
                                coroutineScope = coroutineScope,
                                size = 200.dp,
                                modifier = Modifier.combinedClickable(
                                    onClick = { navController.navigate("album/${album.id}") },
                                    onLongClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        menuState.show {
                                            YouTubeAlbumMenu(
                                                albumItem = album,
                                                navController = navController,
                                                onDismiss = menuState::dismiss
                                            )
                                        }
                                    }
                                )
                            )
                        }
                    }
                }
            }

            item {
                NavigationTitle(
                    title = "Top 50 Global",
                    modifier = Modifier.padding(top = 16.dp),
                    onPlayAllClick = if (globalTop50Songs.isNotEmpty()) {
                        {
                            playerConnection.playQueue(
                                ListQueue(
                                    title = "Top 50 Global",
                                    items = globalTop50Songs.map { it.toMediaItem() }
                                )
                            )
                        }
                    } else null
                )
            }

            item {
                if (isGlobalLoading && globalTop50Songs.isEmpty()) {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(5) {
                            ShimmerHost {
                                Box(
                                    modifier = Modifier
                                        .size(180.dp)
                                        .padding(8.dp)
                                        .background(
                                            MaterialTheme.colorScheme.surfaceVariant,
                                            RoundedCornerShape(12.dp)
                                        )
                                )
                            }
                        }
                    }
                } else if (globalTop50Songs.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        contentAlignment = androidx.compose.ui.Alignment.Center
                    ) {
                        Text(
                            text = globalError ?: "No chart data available",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    }
                } else {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(globalTop50Songs) { song ->
                            YouTubeGridItem(
                                item = song,
                                isActive = mediaMetadata?.id == song.id,
                                isPlaying = isPlaying,
                                coroutineScope = coroutineScope,
                                size = 180.dp,
                                modifier = Modifier.combinedClickable(
                                    onClick = {
                                        playerConnection.playQueue(
                                            YouTubeQueue(
                                                song.endpoint ?: WatchEndpoint(videoId = song.id),
                                                song.toMediaMetadata()
                                            )
                                        )
                                    },
                                    onLongClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        menuState.show {
                                            YouTubeSongMenu(
                                                song = song,
                                                navController = navController,
                                                onDismiss = menuState::dismiss
                                            )
                                        }
                                    }
                                )
                            )
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }
}