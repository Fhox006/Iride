/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.screens.artist

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastForEach
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.compose.ui.layout.ContentScale
import coil3.compose.AsyncImage
import com.metrolist.innertube.YouTube
import com.metrolist.innertube.models.AlbumItem
import com.metrolist.innertube.models.ArtistItem
import com.metrolist.innertube.models.EpisodeItem
import com.metrolist.innertube.models.PlaylistItem
import com.metrolist.innertube.models.PodcastItem
import com.metrolist.innertube.models.SongItem
import com.metrolist.innertube.models.WatchEndpoint
import com.metrolist.music.LocalDatabase
import com.metrolist.music.LocalListenTogetherManager
import com.metrolist.music.LocalPlayerAwareWindowInsets
import com.metrolist.music.LocalPlayerConnection
import com.metrolist.music.R
import com.metrolist.music.constants.AppBarHeight
import com.metrolist.music.constants.HideExplicitKey
import com.metrolist.music.constants.ShowArtistDescriptionKey
import com.metrolist.music.constants.ShowArtistSubscriberCountKey
import com.metrolist.music.constants.ShowMonthlyListenersKey
import com.metrolist.music.db.entities.Album
import com.metrolist.music.extensions.toMediaItem
import com.metrolist.music.models.toMediaMetadata
import com.metrolist.music.playback.queues.ListQueue
import com.metrolist.music.playback.queues.YouTubeQueue
import com.metrolist.music.ui.component.AlbumGridItem
import com.metrolist.music.ui.component.ExpandableText
import com.metrolist.music.ui.component.HideOnScrollFAB
import com.metrolist.music.ui.component.IconButton
import com.metrolist.music.ui.component.LinkSegment
import com.metrolist.music.ui.component.LocalMenuState
import com.metrolist.music.ui.component.NavigationTitle
import com.metrolist.music.ui.component.SongListItem
import com.metrolist.music.ui.component.YouTubeGridItem
import com.metrolist.music.ui.component.YouTubeListItem
import com.metrolist.music.ui.component.shimmer.ListItemPlaceHolder
import com.metrolist.music.ui.component.shimmer.ShimmerHost
import com.metrolist.music.ui.component.shimmer.TextPlaceholder
import com.metrolist.music.ui.menu.AlbumMenu
import com.metrolist.music.ui.menu.SongMenu
import com.metrolist.music.ui.menu.YouTubeAlbumMenu
import com.metrolist.music.ui.menu.YouTubeArtistMenu
import com.metrolist.music.ui.menu.YouTubePlaylistMenu
import com.metrolist.music.ui.menu.YouTubeSongMenu
import com.metrolist.music.ui.utils.backToMain
import com.metrolist.music.ui.utils.fadingEdge
import com.metrolist.music.ui.utils.isScrollingUp
import com.metrolist.music.ui.utils.resize
import com.metrolist.music.utils.rememberPreference
import com.metrolist.music.viewmodels.ArtistViewModel
import com.valentinilk.shimmer.shimmer
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ArtistScreen(
    navController: NavController,
    viewModel: ArtistViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val database = LocalDatabase.current
    val menuState = LocalMenuState.current
    val haptic = LocalHapticFeedback.current
    val uriHandler = LocalUriHandler.current
    val coroutineScope = rememberCoroutineScope()
    val playerConnection = LocalPlayerConnection.current ?: return
    val listenTogetherManager = LocalListenTogetherManager.current
    val isGuest = listenTogetherManager?.isInRoom == true && !listenTogetherManager.isHost
    val isPlaying by playerConnection.isEffectivelyPlaying.collectAsState()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()
    val artistPage = viewModel.artistPage
    val libraryArtist by viewModel.libraryArtist.collectAsState()
    val librarySongs by viewModel.librarySongs.collectAsState()
    val libraryAlbums by viewModel.libraryAlbums.collectAsState()
    val isChannelSubscribed by viewModel.isChannelSubscribed.collectAsState()
    val recentAlbum by viewModel.recentAlbum.collectAsState()
    val hideExplicit by rememberPreference(key = HideExplicitKey, defaultValue = false)
    val showArtistDescription by rememberPreference(key = ShowArtistDescriptionKey, defaultValue = true)
    val showArtistSubscriberCount by rememberPreference(key = ShowArtistSubscriberCountKey, defaultValue = true)
    val showMonthlyListeners by rememberPreference(key = ShowMonthlyListenersKey, defaultValue = true)

    val albumsTitles = remember(artistPage) {
        artistPage?.sections
            ?.filter { it.title.contains("Album", ignoreCase = true) }
            ?.flatMap { it.items }
            ?.filterIsInstance<AlbumItem>()
            ?.map { it.title.lowercase().trim() }
            ?.toSet() ?: emptySet()
    }

    val lazyListState = rememberLazyListState()
    val snackbarHostState = remember { SnackbarHostState() }
    var showLocal by rememberSaveable { mutableStateOf(false) }
    val density = LocalDensity.current

    // Calculate the offset value outside of the offset lambda
    val systemBarsTopPadding = WindowInsets.systemBars.asPaddingValues().calculateTopPadding()
    val headerOffset =
        with(density) {
            -(systemBarsTopPadding + AppBarHeight).roundToPx()
        }

    val transparentAppBar by remember {
        derivedStateOf {
            lazyListState.firstVisibleItemIndex == 0 && lazyListState.firstVisibleItemScrollOffset < 100
        }
    }

    LaunchedEffect(libraryArtist) {
        // always show local page for local artists. Show local page remote artist when offline
        showLocal = libraryArtist?.artist?.isLocal == true
    }

    Box(
        modifier = Modifier.fillMaxSize(),
    ) {
        LazyColumn(
            state = lazyListState,
            contentPadding = LocalPlayerAwareWindowInsets.current.asPaddingValues(),
        ) {
            if (artistPage == null && !showLocal) {
                item(key = "shimmer") {
                    ShimmerHost(
                        modifier =
                            Modifier
                                .offset {
                                    IntOffset(x = 0, y = headerOffset)
                                },
                    ) {
                        // Artist Image Placeholder
                        Box(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(1.1f),
                        ) {
                            Spacer(
                                modifier =
                                    Modifier
                                        .fillMaxSize()
                                        .shimmer()
                                        .background(MaterialTheme.colorScheme.onSurface)
                                        .fadingEdge(
                                            top = systemBarsTopPadding + AppBarHeight,
                                        ),
                            )
                            val bgColor = MaterialTheme.colorScheme.background
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(160.dp)
                                    .align(Alignment.BottomCenter)
                                    .background(
                                        Brush.verticalGradient(
                                            colors = listOf(Color.Transparent, bgColor),
                                        )
                                    ),
                            )
                        }
                        // Artist Name and Controls Section
                        Column(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                        ) {
                            // Artist Name Placeholder
                            TextPlaceholder(
                                height = 36.dp,
                                modifier =
                                    Modifier
                                        .fillMaxWidth(0.7f)
                                        .padding(bottom = 16.dp),
                            )

                            // Buttons Row Placeholder
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                            ) {
                                // Subscribe (Like) Button Placeholder
                                Box(
                                    modifier =
                                        Modifier
                                            .size(40.dp)
                                            .shimmer()
                                            .background(
                                                MaterialTheme.colorScheme.onSurface,
                                                RoundedCornerShape(20.dp),
                                            ),
                                )

                                // Radio Button Placeholder
                                Box(
                                    modifier =
                                        Modifier
                                            .size(40.dp)
                                            .shimmer()
                                            .background(
                                                MaterialTheme.colorScheme.onSurface,
                                                RoundedCornerShape(20.dp),
                                            ),
                                )

                                // Shuffle Button Placeholder
                                Box(
                                    modifier =
                                        Modifier
                                            .size(40.dp)
                                            .shimmer()
                                            .background(
                                                MaterialTheme.colorScheme.onSurface,
                                                RoundedCornerShape(20.dp),
                                            ),
                                )

                                // Link Button Placeholder
                                Box(
                                    modifier =
                                        Modifier
                                            .size(40.dp)
                                            .shimmer()
                                            .background(
                                                MaterialTheme.colorScheme.onSurface,
                                                RoundedCornerShape(20.dp),
                                            ),
                                )
                            }
                        }
                        // Songs List Placeholder
                        repeat(6) {
                            ListItemPlaceHolder()
                        }
                    }
                }
            } else {
                item(key = "header") {
                    val thumbnail = artistPage?.artist?.thumbnail ?: libraryArtist?.artist?.thumbnailUrl
                    val artistName = artistPage?.artist?.title ?: libraryArtist?.artist?.name

                    Box {
                        // Artist Image with offset
                        if (thumbnail != null) {
                            Box(
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .aspectRatio(1f)
                                        .offset {
                                            IntOffset(x = 0, y = headerOffset)
                                        }
                                        .background(MaterialTheme.colorScheme.surfaceVariant)
                                        .fadingEdge(
                                            top = systemBarsTopPadding + AppBarHeight,
                                        ),
                            ) {
                                AsyncImage(
                                    model = thumbnail.resize(1200, 1200),
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize(),
                                )
                                // Full gradient overlay: 0% background at top → 100% at bottom
                                val bgColor = MaterialTheme.colorScheme.background
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(160.dp)
                                        .align(Alignment.BottomCenter)
                                        .background(
                                            Brush.verticalGradient(
                                                colors = listOf(Color.Transparent, bgColor),
                                            )
                                        ),
                                )
                            }
                        }

                        // Artist Name and Controls Section - positioned at bottom of image
                        Column(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .padding(
                                        top =
                                            if (thumbnail != null) {
                                                // Position content at the bottom part of the image
                                                // Using screen width to calculate aspect ratio height minus overlap
                                                LocalResources.current.displayMetrics.widthPixels.let { screenWidth ->
                                                    with(density) {
                                                        ((screenWidth / 1.2f) - 144).toDp()
                                                    }
                                                }
                                            } else {
                                                16.dp
                                            },
                                    ),
                        ) {
                            Column(
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp),
                            ) {
                                // Artist Name
                                Text(
                                    text = artistName ?: "Unknown",
                                    style = MaterialTheme.typography.headlineLarge,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    fontSize = 32.sp,
                                )

                                val monthlyListeners = artistPage?.monthlyListenerCount
                                if (showMonthlyListeners && !monthlyListeners.isNullOrEmpty()) {
                                    val cleanListeners = remember(monthlyListeners) {
                                        monthlyListeners
                                            .replace("monthly listeners", "", ignoreCase = true)
                                            .replace("ascoltatori mensili", "", ignoreCase = true)
                                            .trim()
                                    }
                                    Text(
                                        text = "$cleanListeners listeners this month",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }

                                // Recent Album Panel
                                if (recentAlbum != null) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    RecentAlbumPanel(
                                        album = recentAlbum!!,
                                        onClick = { navController.navigate("album/${recentAlbum!!.id}") },
                                        onLongClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            menuState.show {
                                                AlbumMenu(
                                                    originalAlbum = recentAlbum!!,
                                                    navController = navController,
                                                    onDismiss = menuState::dismiss,
                                                )
                                            }
                                        }
                                    )
                                }
                            }
                            if (recentAlbum != null) {
                                Spacer(modifier = Modifier.height(16.dp))
                            }
                        }
                    }
                }

                if (showLocal) {
                    if (librarySongs.isNotEmpty()) {
                        item(key = "local_songs_title") {
                            NavigationTitle(
                                title = stringResource(R.string.songs),
                                modifier = Modifier.animateItem(),
                                onClick = {
                                    navController.navigate("artist/${viewModel.artistId}/songs")
                                },
                            )
                        }

                        val filteredLibrarySongs =
                            if (hideExplicit) {
                                librarySongs.filter { !it.song.explicit }
                            } else {
                                librarySongs
                            }
                        itemsIndexed(
                            items = filteredLibrarySongs,
                            key = { index, item -> "local_song_${item.id}_$index" },
                        ) { index, song ->
                            SongListItem(
                                song = song,
                                isActive = song.id == mediaMetadata?.id,
                                isPlaying = isPlaying,
                                trailingContent = {
                                    IconButton(
                                        onClick = {
                                            menuState.show {
                                                SongMenu(
                                                    originalSong = song,
                                                    navController = navController,
                                                    onDismiss = menuState::dismiss,
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
                                        .fillMaxWidth()
                                        .combinedClickable(
                                            onClick = {
                                                if (!isGuest) {
                                                    if (song.id == mediaMetadata?.id) {
                                                        playerConnection.togglePlayPause()
                                                    } else {
                                                        playerConnection.playQueue(
                                                            ListQueue(
                                                                title = libraryArtist?.artist?.name ?: "Unknown Artist",
                                                                items = librarySongs.map { it.toMediaItem() },
                                                                startIndex = index,
                                                            ),
                                                        )
                                                    }
                                                }
                                            },
                                            onLongClick = {
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                menuState.show {
                                                    SongMenu(
                                                        originalSong = song,
                                                        navController = navController,
                                                        onDismiss = menuState::dismiss,
                                                    )
                                                }
                                            },
                                        ).animateItem(),
                            )
                        }
                    }

                    if (libraryAlbums.isNotEmpty()) {
                        val filteredLibraryAlbums = libraryAlbums.filter { album ->
                            !hideExplicit || !album.album.explicit
                        }

                        if (filteredLibraryAlbums.isNotEmpty()) {
                            item(key = "local_albums_title") {
                                NavigationTitle(
                                    title = stringResource(R.string.albums),
                                    modifier = Modifier.animateItem(),
                                    onClick = {
                                        navController.navigate("artist/${viewModel.artistId}/albums")
                                    },
                                )
                            }

                            item(key = "local_albums_list") {
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(0.dp),
                                    contentPadding = WindowInsets.systemBars.only(WindowInsetsSides.Horizontal).asPaddingValues(),
                                ) {
                                    items(
                                        items = filteredLibraryAlbums,
                                        key = { "local_album_${it.id}_${filteredLibraryAlbums.indexOf(it)}" },
                                    ) { album ->
                                        AlbumGridItem(
                                            album = album,
                                            isActive = mediaMetadata?.album?.id == album.id,
                                            isPlaying = isPlaying,
                                            coroutineScope = coroutineScope,
                                            showPlayButton = false,
                                            size = 180.dp,
                                            modifier =
                                                Modifier
                                                    .combinedClickable(
                                                        onClick = {
                                                            navController.navigate("album/${album.id}")
                                                        },
                                                        onLongClick = {
                                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                            menuState.show {
                                                                AlbumMenu(
                                                                    originalAlbum = album,
                                                                    navController = navController,
                                                                    onDismiss = menuState::dismiss,
                                                                )
                                                            }
                                                        },
                                                    ).animateItem(),
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else {
                    artistPage?.sections?.fastForEach { section ->
                        if (section.items.isNotEmpty()) {
                            val isSinglesSection = section.title.contains("Single", ignoreCase = true) || section.title.contains("EP", ignoreCase = true)
                            // Filter out recent album and duplicate Singles/EPs
                            val filteredItems = section.items.filter { item ->
                                val isDuplicate = isSinglesSection && item is AlbumItem && albumsTitles.contains(item.title.lowercase().trim())
                                !isDuplicate
                            }

                            if (filteredItems.isNotEmpty()) {
                                item(key = "section_${section.title}") {
                                    NavigationTitle(
                                        title = section.title,
                                        modifier = Modifier.animateItem(),
                                        onClick =
                                            section.moreEndpoint?.let {
                                                {
                                                    navController.navigate(
                                                        "artist/${viewModel.artistId}/items?browseId=${it.browseId}?params=${it.params}",
                                                    )
                                                }
                                            },
                                    )
                                }

                                if ((filteredItems.firstOrNull() as? SongItem)?.album != null) {
                                    items(
                                        items = filteredItems.distinctBy { it.id },
                                        key = { "youtube_song_${it.id}" },
                                    ) { song ->
                                        YouTubeListItem(
                                            item = song as SongItem,
                                            isActive = mediaMetadata?.id == song.id,
                                            isPlaying = isPlaying,
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
                                                ) {
                                                    Icon(
                                                        painter = painterResource(R.drawable.more_vert),
                                                        contentDescription = null,
                                                    )
                                                }
                                            },
                                            modifier =
                                                Modifier
                                                    .fillMaxWidth()
                                                    .combinedClickable(
                                                        onClick = {
                                                            if (!isGuest) {
                                                                if (song.id == mediaMetadata?.id) {
                                                                    playerConnection.togglePlayPause()
                                                                } else {
                                                                    playerConnection.playQueue(
                                                                        YouTubeQueue(
                                                                            WatchEndpoint(videoId = song.id),
                                                                            song.toMediaMetadata(),
                                                                        ),
                                                                    )
                                                                }
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
                                                    ).animateItem(),
                                        )
                                    }
                                } else {
                                    item(key = "section_list_${section.title}") {
                                        val isAlbumSection = section.title.contains("Album", ignoreCase = true)
                                        val isSingleEpSection = section.title.contains("Single", ignoreCase = true) ||
                                                section.title.contains("EP", ignoreCase = true)
                                        val hidePlayButton = isAlbumSection || isSingleEpSection
                                        val isVideoSection = section.title.contains("Video", ignoreCase = true) ||
                                                section.title.contains("Performance", ignoreCase = true)
                                        LazyRow(
                                            horizontalArrangement = Arrangement.spacedBy(0.dp),
                                            contentPadding = WindowInsets.systemBars.only(WindowInsetsSides.Horizontal).asPaddingValues(),
                                        ) {
                                            items(
                                                items = filteredItems.distinctBy { it.id },
                                                key = { "youtube_album_${it.id}" },
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
                                                    coroutineScope = coroutineScope,
                                                    thumbnailRatio = if (isVideoSection) 16f / 9f else 1f,
                                                    thumbnailCornerRadius = if (isVideoSection) 8.dp else 3.dp,
                                                    showPlayButton = !hidePlayButton,
                                                    size = when {
                                                        isAlbumSection -> 180.dp
                                                        isVideoSection -> 110.dp
                                                        else -> 148.dp
                                                    },
                                                    modifier =
                                                        Modifier
                                                            .combinedClickable(
                                                                onClick = {
                                                                    when (item) {
                                                                        is SongItem -> {
                                                                            if (!isGuest) {
                                                                                playerConnection.playQueue(
                                                                                    YouTubeQueue(
                                                                                        WatchEndpoint(videoId = item.id),
                                                                                        item.toMediaMetadata(),
                                                                                    ),
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
                                                                            if (!isGuest) {
                                                                                playerConnection.playQueue(
                                                                                    YouTubeQueue(
                                                                                        WatchEndpoint(videoId = item.id),
                                                                                        item.toMediaMetadata(),
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
                            }
                        }
                    }

                    // About Artist Section
                    if (!showLocal && (showArtistDescription || showArtistSubscriberCount)) {
                        val description = artistPage?.description
                        val descriptionRuns = artistPage?.descriptionRuns
                        val subscriberCount = artistPage?.subscriberCountText

                        if ((showArtistDescription && !description.isNullOrEmpty()) ||
                            (showArtistSubscriberCount && !subscriberCount.isNullOrEmpty())
                        ) {
                            item(key = "about_artist") {
                                Column(
                                    modifier =
                                        Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 16.dp)
                                            .padding(vertical = 16.dp)
                                            .animateItem(),
                                ) {
                                    Text(
                                        text = "Information",
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(bottom = 16.dp),
                                    )

                                    if (showArtistSubscriberCount && !subscriberCount.isNullOrEmpty()) {
                                        val formattedSubscribers = formatSubscriberCount(subscriberCount)
                                        Text(
                                            text = formattedSubscribers ?: subscriberCount,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.padding(bottom = 16.dp),
                                        )
                                    }

                                    if (showArtistDescription && (!description.isNullOrEmpty() || !descriptionRuns.isNullOrEmpty())) {
                                        Text(
                                            text = "Wikipedia",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(bottom = 8.dp),
                                        )

                                        val linkSegments = remember(descriptionRuns) {
                                            descriptionRuns?.map { run ->
                                                LinkSegment(
                                                    text = run.text,
                                                    url = run.navigationEndpoint?.urlEndpoint?.url
                                                )
                                            }
                                        }

                                        ExpandableText(
                                            text = description.orEmpty(),
                                            runs = linkSegments,
                                            collapsedMaxLines = 4,
                                        )

                                        val wikiLink = descriptionRuns?.find {
                                            it.text.contains("Wikipedia", ignoreCase = true) ||
                                                    it.navigationEndpoint?.urlEndpoint?.url?.contains("wikipedia.org") == true
                                        }?.navigationEndpoint?.urlEndpoint?.url

                                        if (wikiLink != null) {
                                            Text(
                                                text = "Read more on Wikipedia",
                                                style = MaterialTheme.typography.labelLarge,
                                                color = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier
                                                    .padding(top = 12.dp)
                                                    .clickable { uriHandler.openUri(wikiLink) }
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

        val isScrollingUp = lazyListState.isScrollingUp()
        val showLocalFab = librarySongs.isNotEmpty() && libraryArtist?.artist?.isLocal != true

        // Library/Local Toggle FAB
        HideOnScrollFAB(
            visible = showLocalFab,
            lazyListState = lazyListState,
            icon = if (showLocal) R.drawable.language else R.drawable.library_music,
            onClick = {
                showLocal = showLocal.not()
                if (!showLocal && artistPage == null) viewModel.fetchArtistsFromYTM()
            },
        )

        // Play All FAB (Stacked above Library/Local FAB if visible)
        val canPlayAll =
            !isGuest && (
                    (showLocal && librarySongs.isNotEmpty()) ||
                            (
                                    !showLocal && artistPage?.sections?.any {
                                        (it.items.firstOrNull() as? SongItem)?.album != null
                                    } == true
                                    )
                    )

        if (canPlayAll) {
            androidx.compose.animation.AnimatedVisibility(
                visible = isScrollingUp,
                enter = androidx.compose.animation.slideInVertically { it * 2 },
                exit = androidx.compose.animation.slideOutVertically { it * 2 },
                modifier =
                    Modifier
                        .align(Alignment.BottomEnd)
                        .windowInsetsPadding(
                            LocalPlayerAwareWindowInsets.current
                                .only(WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal),
                        )
                        // Add padding to position it above the other FAB (56dp height + 16dp padding + 8dp spacing)
                        // If the other FAB is visible.
                        .padding(bottom = if (showLocalFab) 64.dp else 0.dp),
            ) {
                val onPlayAllClick: () -> Unit = {
                    if (!isGuest) {
                        if (showLocal) {
                            if (librarySongs.isNotEmpty()) {
                                playerConnection.playQueue(
                                    ListQueue(
                                        title = libraryArtist?.artist?.name ?: "Unknown Artist",
                                        items = librarySongs.map { it.toMediaItem() },
                                    ),
                                )
                            }
                        } else if (artistPage != null) {
                            val songSection =
                                artistPage.sections.find { section ->
                                    (section.items.firstOrNull() as? SongItem)?.album != null
                                }

                            val moreEndpoint = songSection?.moreEndpoint
                            if (moreEndpoint != null) {
                                coroutineScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                                    val result = YouTube.artistItems(moreEndpoint).getOrNull()
                                    withContext(kotlinx.coroutines.Dispatchers.Main) {
                                        if (result != null && result.items.isNotEmpty()) {
                                            val songs = result.items.filterIsInstance<SongItem>().map { it.toMediaItem() }
                                            playerConnection.playQueue(
                                                ListQueue(
                                                    title = artistPage.artist.title,
                                                    items = songs,
                                                ),
                                            )
                                        } else {
                                            // Fallback to loaded items
                                            val songs = songSection.items.filterIsInstance<SongItem>().map { it.toMediaItem() }
                                            if (songs.isNotEmpty()) {
                                                playerConnection.playQueue(
                                                    ListQueue(
                                                        title = artistPage.artist.title,
                                                        items = songs,
                                                    ),
                                                )
                                            }
                                        }
                                    }
                                }
                            } else if (songSection != null) {
                                // Use loaded items if no more endpoint
                                val songs = songSection.items.filterIsInstance<SongItem>().map { it.toMediaItem() }
                                playerConnection.playQueue(
                                    ListQueue(
                                        title = artistPage.artist.title,
                                        items = songs,
                                    ),
                                )
                            } else {
                                // Fallback to shuffle endpoint (stripped) if no song section found
                                val shuffleEndpoint = artistPage.artist.shuffleEndpoint
                                if (shuffleEndpoint != null) {
                                    val endpoint =
                                        if (shuffleEndpoint.playlistId != null) {
                                            WatchEndpoint(
                                                playlistId = shuffleEndpoint.playlistId,
                                                params = null, // Remove shuffle params to play in order
                                                videoId = null, // Ensure videoId is null to start from beginning of playlist
                                            )
                                        } else {
                                            shuffleEndpoint
                                        }
                                    playerConnection.playQueue(YouTubeQueue(endpoint))
                                }
                            }
                        }
                    }
                }

                if (showLocalFab) {
                    androidx.compose.material3.SmallFloatingActionButton(
                        modifier = Modifier.padding(16.dp).offset(x = (-4).dp), // Align center with standard FAB (56dp vs 48dp)
                        onClick = onPlayAllClick,
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.play),
                            contentDescription = "Play All",
                        )
                    }
                } else {
                    androidx.compose.material3.FloatingActionButton(
                        modifier = Modifier.padding(16.dp),
                        onClick = onPlayAllClick,
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.play),
                            contentDescription = "Play All",
                            modifier = Modifier.size(32.dp),
                        )
                    }
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier =
                Modifier
                    .windowInsetsPadding(LocalPlayerAwareWindowInsets.current)
                    .align(Alignment.BottomCenter),
        )
    }

    TopAppBar(
        title = { if (!transparentAppBar) Text(artistPage?.artist?.title.orEmpty()) },
        navigationIcon = {
            IconButton(
                onClick = navController::navigateUp,
                onLongClick = navController::backToMain,
            ) {
                Icon(
                    painterResource(R.drawable.arrow_back),
                    contentDescription = null,
                )
            }
        },
        actions = {
            val shareLink = artistPage?.artist?.shareLink
            Row(
                modifier = Modifier
                    .padding(end = 8.dp)
                    .background(
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                        shape = RoundedCornerShape(20.dp)
                    )
                    .padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { viewModel.toggleChannelSubscription() }
                ) {
                    Icon(
                        painter = painterResource(if (isChannelSubscribed) R.drawable.favorite else R.drawable.favorite_border),
                        contentDescription = null,
                        tint = if (isChannelSubscribed) MaterialTheme.colorScheme.primary else LocalContentColor.current,
                        modifier = Modifier.size(20.dp),
                    )
                }

                if (!showLocal && !isGuest) {
                    artistPage?.artist?.radioEndpoint?.let { radioEndpoint ->
                        IconButton(
                            onClick = { playerConnection.playQueue(YouTubeQueue(radioEndpoint)) }
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.radio),
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }

                    artistPage?.artist?.shuffleEndpoint?.let { shuffleEndpoint ->
                        IconButton(
                            onClick = { playerConnection.playQueue(YouTubeQueue(shuffleEndpoint)) }
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.shuffle),
                                contentDescription = "Shuffle",
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }
                }

                IconButton(
                    onClick = {
                        shareLink?.let { link ->
                            val sendIntent = android.content.Intent().apply {
                                action = android.content.Intent.ACTION_SEND
                                putExtra(android.content.Intent.EXTRA_TEXT, link)
                                type = "text/plain"
                            }
                            val shareIntent = android.content.Intent.createChooser(sendIntent, null)
                            context.startActivity(shareIntent)
                        }
                    }
                ) {
                    Icon(
                        painterResource(R.drawable.share),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        },
        colors =
            if (transparentAppBar) {
                TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            } else {
                TopAppBarDefaults.topAppBarColors()
            },
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun RecentAlbumPanel(
    album: Album,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        AsyncImage(
            model = album.thumbnailUrl?.resize(544, 544),
            contentDescription = null,
            modifier = Modifier
                .size(96.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
        )

        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = album.album.title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Start
            )

            val extendedDate = remember(album.album.releaseDate) {
                formatExtendedDate(album.album.releaseDate)
            }
            // Fallback to year when full releaseDate is not available (e.g. from YTM API)
            val displayDate = extendedDate ?: album.album.year?.toString()

            if (displayDate != null) {
                Text(
                    text = displayDate,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Start
                )
            }

            if (album.album.songCount > 0) {
                Text(
                    text = pluralStringResource(R.plurals.n_song, album.album.songCount, album.album.songCount),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Start
                )
            }
        }
    }
}

fun formatExtendedDate(dateStr: String?): String? {
    if (dateStr == null) return null
    return try {
        val parts = dateStr.split("-")
        val date = when (parts.size) {
            3 -> java.time.LocalDate.of(parts[0].toInt(), parts[1].toInt(), parts[2].toInt())
            2 -> java.time.LocalDate.of(parts[0].toInt(), parts[1].toInt(), 1)
            else -> java.time.LocalDate.of(parts[0].toInt(), 1, 1)
        }

        val formatter = when (parts.size) {
            3 -> DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.US)
            2 -> DateTimeFormatter.ofPattern("MMMM yyyy", Locale.US)
            else -> DateTimeFormatter.ofPattern("yyyy", Locale.US)
        }

        date.format(formatter)
    } catch (e: Exception) {
        null
    }
}

fun formatSubscriberCount(subscriberCount: String?): String? {
    if (subscriberCount == null) return null
    val regex = """([\d.,]+)\s*([KMB]?)""".toRegex(RegexOption.IGNORE_CASE)
    val matchResult = regex.find(subscriberCount) ?: return null
    val (numStr, unit) = matchResult.destructured
    val num = numStr.replace(",", ".").toDoubleOrNull() ?: return null

    // Discard implausible values: no suffix and less than 1000
    if (unit.isEmpty() && num < 1000) return null

    val rounded = (num * 10.0).roundToInt() / 10.0
    val formattedNum = if (rounded % 1.0 == 0.0) rounded.toInt().toString() else rounded.toString()

    return when (unit.uppercase()) {
        "K" -> "${formattedNum}k subscribers on YouTube"
        "M" -> "$formattedNum million subscribers on YouTube"
        "B" -> "$formattedNum billion subscribers on YouTube"
        else -> "$formattedNum subscribers on YouTube"
    }
}