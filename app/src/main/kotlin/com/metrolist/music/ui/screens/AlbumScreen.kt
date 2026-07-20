/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.screens

/*import android.graphics.Bitmap*/
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
/*import androidx.compose.animation.core.animateFloatAsState*/
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
/*import androidx.compose.ui.draw.alpha*/
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import com.metrolist.music.ui.theme.SpaceMonoFontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withLink
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastForEachIndexed
import androidx.compose.ui.util.fastForEachReversed
import sv.lib.squircleshape.SquircleShape
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.TextButton
import androidx.core.net.toUri
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.DownloadRequest
import androidx.media3.exoplayer.offline.DownloadService
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import coil3.imageLoader
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.toBitmap
import com.metrolist.music.LocalDatabase
import com.metrolist.music.LocalDownloadUtil
import com.metrolist.music.LocalListenTogetherManager
import com.metrolist.music.LocalPlayerAwareWindowInsets
import com.metrolist.music.LocalPlayerConnection
import com.metrolist.music.R
import com.metrolist.music.constants.AlbumTopGradientKey
import com.metrolist.music.constants.HideDurationForStandardSongsKey
import com.metrolist.music.constants.HideExplicitKey
import com.metrolist.music.constants.HideVideoSongsKey
import com.metrolist.music.constants.PlayerBackgroundStyle
import com.metrolist.music.constants.PlayerBackgroundStyleKey
import com.metrolist.music.constants.TopNavigationBarKey
import com.metrolist.music.db.entities.Album
import com.metrolist.music.db.entities.AlbumPlayEvent
import com.metrolist.music.models.MediaMetadata
import com.metrolist.music.playback.ExoDownloadService
import com.metrolist.music.playback.queues.LocalAlbumRadio
/*import com.metrolist.music.ui.component.AnimatedAlbumGradientBackground*/
import com.metrolist.music.ui.component.AlbumVinylDisc
import com.metrolist.music.ui.component.DefaultDialog
import com.metrolist.music.ui.component.IconButton
import com.metrolist.music.ui.component.IrideLoadingIndicator
import com.metrolist.music.ui.component.IrideOutlineIconButton
import com.metrolist.music.ui.component.LocalMenuState
import com.metrolist.music.ui.component.NavigationTitle
import com.metrolist.music.ui.component.SongListItem
import com.metrolist.music.ui.component.TopScreenGradientBackground
import com.metrolist.music.ui.component.YouTubeGridItem
import com.metrolist.music.utils.rememberEnumPreference
import com.metrolist.music.ui.menu.AlbumMenu
import com.metrolist.music.ui.menu.SelectionSongMenu
import com.metrolist.music.ui.menu.SongMenu
import com.metrolist.music.ui.menu.YouTubeAlbumMenu
import com.metrolist.music.ui.utils.backToMain
import com.metrolist.music.utils.GenreProvider
import com.metrolist.music.utils.joinByBullet
import com.metrolist.music.utils.makeReadableTimeString
import com.metrolist.music.utils.makeTimeString
import com.metrolist.music.utils.rememberPreference
import com.metrolist.music.viewmodels.AlbumViewModel

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AlbumScreen(
    navController: NavController,
    viewModel: AlbumViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val menuState = LocalMenuState.current
    val database = LocalDatabase.current
    val haptic = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()
    val playerConnection = LocalPlayerConnection.current ?: return
    val listenTogetherManager = LocalListenTogetherManager.current
    val isListenTogetherGuest = listenTogetherManager?.let { it.isInRoom && !it.isHost } ?: false

    val scope = rememberCoroutineScope()

    val isPlaying by playerConnection.isEffectivelyPlaying.collectAsStateWithLifecycle()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsStateWithLifecycle()

    val playlistId by viewModel.playlistId.collectAsStateWithLifecycle()
    val albumWithSongs by viewModel.albumWithSongs.collectAsStateWithLifecycle()
    val otherVersions by viewModel.otherVersions.collectAsStateWithLifecycle()
    val similarAlbums by viewModel.similarAlbums.collectAsStateWithLifecycle()
    val hasError by viewModel.hasError.collectAsStateWithLifecycle()
    val hideExplicit by rememberPreference(key = HideExplicitKey, defaultValue = false)
    val hideVideoSongs by rememberPreference(key = HideVideoSongsKey, defaultValue = false)
    val topNavigationBarEnabled by rememberPreference(TopNavigationBarKey, defaultValue = true)
    val albumTopGradientEnabled by rememberPreference(AlbumTopGradientKey, defaultValue = true)
    val lazyListState = rememberLazyListState()
    val transparentAppBar by remember {
        derivedStateOf {
            lazyListState.firstVisibleItemIndex == 0 && lazyListState.firstVisibleItemScrollOffset < 100
        }
    }
    val topBarBackgroundColor by animateColorAsState(
        targetValue = if (transparentAppBar) Color.Transparent else MaterialTheme.colorScheme.background,
        animationSpec = tween(300),
        label = "albumTopBarBg",
    )
    val playerBackgroundStyle by rememberEnumPreference(
        PlayerBackgroundStyleKey,
        defaultValue = PlayerBackgroundStyle.BETTER_ANIMATED_GRADIENT,
    )

    val filteredSongs =
        remember(albumWithSongs, hideExplicit, hideVideoSongs) {
            var songs = albumWithSongs?.songs ?: emptyList()
            if (hideExplicit) {
                songs = songs.filter { !it.song.explicit }
            }
            if (hideVideoSongs) {
                songs = songs.filter { !it.song.isVideo }
            }
            songs
        }

    val recentAlbumPlayEvents by produceState<List<AlbumPlayEvent>>(initialValue = emptyList(), albumWithSongs?.album?.id) {
        val albumId = albumWithSongs?.album?.id
        if (albumId == null) {
            value = emptyList()
        } else {
            database.recentAlbumPlayEvents(albumId).collect { value = it }
        }
    }
    val resumeTrackIndex = remember(recentAlbumPlayEvents, albumWithSongs) {
        val songCount = albumWithSongs?.songs?.size ?: 0
        var inOrderStreak = 0
        for (i in recentAlbumPlayEvents.indices) {
            inOrderStreak = if (i == 0) {
                1
            } else if (recentAlbumPlayEvents[i - 1].songIndex - recentAlbumPlayEvents[i].songIndex == 1) {
                inOrderStreak + 1
            } else {
                break
            }
        }
        val lastPlayedIndex = recentAlbumPlayEvents.firstOrNull()?.songIndex
        if (inOrderStreak >= 3 && lastPlayedIndex != null) {
            (lastPlayedIndex + 1).takeIf { it in 0 until songCount }
        } else {
            null
        }
    }
    var resumeDismissed by rememberSaveable(albumWithSongs?.album?.id) { mutableStateOf(false) }
    val isThisAlbumPlaying = isPlaying && mediaMetadata?.album?.id == albumWithSongs?.album?.id

    var inSelectMode by rememberSaveable { mutableStateOf(false) }
    val selection =
        rememberSaveable(
            saver =
                listSaver<MutableList<String>, String>(
                    save = { it.toList() },
                    restore = { it.toMutableStateList() },
                ),
        ) { mutableStateListOf() }
    val onExitSelectionMode = {
        inSelectMode = false
        selection.clear()
    }
    if (inSelectMode) {
        BackHandler(onBack = onExitSelectionMode)
    }

    LaunchedEffect(filteredSongs) {
        selection.fastForEachReversed { songId ->
            if (filteredSongs.find { it.id == songId } == null) {
                selection.remove(songId)
            }
        }
    }

    val downloadUtil = LocalDownloadUtil.current
    var downloadState by remember {
        mutableIntStateOf(Download.STATE_STOPPED)
    }
    var showRemoveDownloadDialog by remember { mutableStateOf(false) }

    LaunchedEffect(albumWithSongs) {
        val songs = albumWithSongs?.songs?.map { it.id }
        if (songs.isNullOrEmpty()) return@LaunchedEffect
        downloadUtil.downloads.collect { downloads ->
            downloadState =
                if (songs.all { downloads[it]?.state == Download.STATE_COMPLETED }) {
                    Download.STATE_COMPLETED
                } else if (songs.all {
                        downloads[it]?.state == Download.STATE_QUEUED ||
                                downloads[it]?.state == Download.STATE_DOWNLOADING ||
                                downloads[it]?.state == Download.STATE_COMPLETED
                    }
                ) {
                    Download.STATE_DOWNLOADING
                } else {
                    Download.STATE_STOPPED
                }
        }
    }

    /*
    var albumThumbnailBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var gradientReady by remember { mutableStateOf(false) }
    val gradientAlpha by animateFloatAsState(
        targetValue = if (gradientReady) 1f else 0f,
        animationSpec = tween(durationMillis = 800),
        label = "gradientAlpha"
    )

    LaunchedEffect(albumWithSongs?.album?.thumbnailUrl) {
        gradientReady = false
        albumThumbnailBitmap = null
        val url = albumWithSongs?.album?.thumbnailUrl ?: return@LaunchedEffect
        val request = ImageRequest.Builder(context)
            .data(url)
            .size(100, 100)
            .allowHardware(false)
            .build()
        val result = context.imageLoader.execute(request)
        albumThumbnailBitmap = result.image?.toBitmap()
        gradientReady = true
    }
    */

    if (showRemoveDownloadDialog) {
        DefaultDialog(
            onDismiss = { showRemoveDownloadDialog = false },
            content = {
                Text(
                    text = stringResource(
                        R.string.remove_download_playlist_confirm,
                        albumWithSongs?.album?.title ?: "",
                    ),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(horizontal = 18.dp),
                )
            },
            buttons = {
                TextButton(onClick = { showRemoveDownloadDialog = false }) {
                    Text(text = stringResource(android.R.string.cancel))
                }
                TextButton(
                    onClick = {
                        showRemoveDownloadDialog = false
                        albumWithSongs?.songs?.forEach { song ->
                            DownloadService.sendRemoveDownload(
                                context,
                                ExoDownloadService::class.java,
                                song.id,
                                false,
                            )
                        }
                    },
                ) {
                    Text(text = stringResource(android.R.string.ok))
                }
            },
        )
    }

    val albumGradientMediaMetadata = remember(albumWithSongs?.album?.id, albumWithSongs?.album?.thumbnailUrl) {
        albumWithSongs?.album?.let {
            MediaMetadata(id = it.id, title = it.title, artists = emptyList(), duration = 0, thumbnailUrl = it.thumbnailUrl)
        }
    }

    if (albumTopGradientEnabled) {
        TopScreenGradientBackground(
            mediaMetadata = albumGradientMediaMetadata,
            playerBackground = playerBackgroundStyle,
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        state = lazyListState,
        contentPadding = LocalPlayerAwareWindowInsets.current.asPaddingValues(),
    ) {
            val albumWithSongs = albumWithSongs
            if (albumWithSongs != null && albumWithSongs.songs.isNotEmpty()) {
                item(key = "album_header") {
                    val albumCoverSquircle = SquircleShape(radius = 12.dp, cornerSmoothing = 0.45f)

                    // Shared across both layouts below.
                    val totalDuration = albumWithSongs.songs.sumOf { it.song.duration }
                    val releaseDate = albumWithSongs.album.releaseDate
                    val displayDate = remember(releaseDate, albumWithSongs.album.year) {
                        if (releaseDate == null) {
                            albumWithSongs.album.year?.toString()
                        } else {
                            val parts = releaseDate.split("-")
                            when (parts.size) {
                                3 -> {
                                    val y = parts[0]
                                    val m = parts[1].toInt().toString()
                                    val d = parts[2].toInt().toString()
                                    "$d/$m/$y"
                                }
                                2 -> {
                                    val y = parts[0]
                                    val m = parts[1].toInt().toString()
                                    "$m/$y"
                                }
                                else -> parts[0]
                            }
                        }
                    }
                    val albumGenre by produceState<String?>(initialValue = null, albumWithSongs.album.id) {
                        val primaryArtistName = albumWithSongs.artists.firstOrNull()?.name
                        value = GenreProvider.getGenres(
                            albumWithSongs.album.id,
                            albumWithSongs.album.title,
                            primaryArtistName,
                        ).firstOrNull()
                    }
                    val metadataLine = joinByBullet(
                        albumGenre,
                        displayDate,
                        if (totalDuration > 0) makeReadableTimeString(totalDuration * 1000L) else null,
                    )
                    val isThisAlbumPlaying = isPlaying && mediaMetadata?.album?.id == albumWithSongs.album.id

                    val onLikeClick: () -> Unit = {
                        database.query {
                            update(albumWithSongs.album.toggleLike())
                        }
                    }
                    val onPlayClick: () -> Unit = {
                        if (!isListenTogetherGuest) {
                            playerConnection.service.getAutomix(playlistId)
                            playerConnection.playQueue(
                                LocalAlbumRadio(albumWithSongs),
                            )
                        }
                    }
                    val onDownloadClick: () -> Unit = {
                        when (downloadState) {
                            Download.STATE_COMPLETED -> showRemoveDownloadDialog = true
                            Download.STATE_DOWNLOADING, Download.STATE_QUEUED -> {
                                albumWithSongs.songs.forEach { song ->
                                    DownloadService.sendRemoveDownload(
                                        context,
                                        ExoDownloadService::class.java,
                                        song.id,
                                        false,
                                    )
                                }
                            }
                            else -> {
                                albumWithSongs.songs.forEach { song ->
                                    val downloadRequest =
                                        DownloadRequest
                                            .Builder(song.id, song.id.toUri())
                                            .setCustomCacheKey(song.id)
                                            .setData(song.song.title.toByteArray())
                                            .build()
                                    DownloadService.sendAddDownload(
                                        context,
                                        ExoDownloadService::class.java,
                                        downloadRequest,
                                        false,
                                    )
                                }
                            }
                        }
                    }

                    if (topNavigationBarEnabled) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp)
                                .padding(top = 12.dp, bottom = 20.dp),
                        ) {
                            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                AlbumVinylDisc(
                                    thumbnailUrl = albumWithSongs.album.thumbnailUrl,
                                    coverSize = 240.dp,
                                    isPlaying = isThisAlbumPlaying,
                                )
                            }

                            Spacer(modifier = Modifier.height(20.dp))

                            Text(
                                text = albumWithSongs.album.title,
                                style = TextStyle(
                                    fontFamily = SpaceMonoFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 20.sp,
                                    letterSpacing = (-0.2).sp,
                                ),
                                color = MaterialTheme.colorScheme.onBackground,
                                textAlign = TextAlign.Center,
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.fillMaxWidth(),
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center,
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                val primaryArtist = albumWithSongs.artists.firstOrNull()
                                if (primaryArtist?.thumbnailUrl != null) {
                                    AsyncImage(
                                        model = primaryArtist.thumbnailUrl,
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .size(18.dp)
                                            .clip(CircleShape),
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                }
                                Text(
                                    buildAnnotatedString {
                                        albumWithSongs.artists.fastForEachIndexed { index, artist ->
                                            val link =
                                                LinkAnnotation.Clickable(
                                                    tag = artist.id,
                                                    // Underline only while held — "release to open artist screen".
                                                    styles = TextLinkStyles(
                                                        style = SpanStyle(textDecoration = TextDecoration.None),
                                                        pressedStyle = SpanStyle(textDecoration = TextDecoration.Underline),
                                                    ),
                                                ) {
                                                    navController.navigate("artist/${artist.id}")
                                                }
                                            withLink(link) {
                                                append(artist.name)
                                            }
                                            if (index != albumWithSongs.artists.lastIndex) {
                                                append(", ")
                                            }
                                        }
                                    },
                                    style = TextStyle(fontFamily = SpaceMonoFontFamily, fontSize = 13.sp),
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.75f),
                                )
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = metadataLine,
                                style = TextStyle(fontFamily = SpaceMonoFontFamily, fontSize = 13.sp),
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth(),
                            )
                            // Action buttons (like/play/download) live in the top bar now — see topBarActions.
                        }
                    } else {
                    Column(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp, bottom = 20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        // Album Thumbnail - Large centered with shadow
                        Surface(
                            modifier =
                                Modifier
                                    .size(240.dp)
                                    .shadow(
                                        elevation = 24.dp,
                                        shape = albumCoverSquircle,
                                        spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                                    ),
                            shape = albumCoverSquircle,
                        ) {
                            AsyncImage(
                                model = albumWithSongs.album.thumbnailUrl,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize(),
                            )
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // Album Name
                        Text(
                            text = albumWithSongs.album.title,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(horizontal = 32.dp),
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Artist - small avatar + plain name, no underline
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            val primaryArtist = albumWithSongs.artists.firstOrNull()
                            if (primaryArtist?.thumbnailUrl != null) {
                                AsyncImage(
                                    model = primaryArtist.thumbnailUrl,
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(20.dp)
                                        .clip(CircleShape),
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                            }
                            Text(
                                buildAnnotatedString {
                                    albumWithSongs.artists.fastForEachIndexed { index, artist ->
                                        val link =
                                            LinkAnnotation.Clickable(
                                                tag = artist.id,
                                                styles = TextLinkStyles(style = SpanStyle(textDecoration = TextDecoration.None)),
                                            ) {
                                                navController.navigate("artist/${artist.id}")
                                            }
                                        withLink(link) {
                                            append(artist.name)
                                        }
                                        if (index != albumWithSongs.artists.lastIndex) {
                                            append(", ")
                                        }
                                    }
                                },
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onBackground,
                                textAlign = TextAlign.Center,
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = metadataLine,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        // Action Buttons Row
                        Row(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 24.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            // Like Button - Smaller secondary button
                            Surface(
                                onClick = onLikeClick,
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.size(48.dp),
                            ) {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(
                                        painter =
                                            painterResource(
                                                if (albumWithSongs.album.bookmarkedAt !=
                                                    null
                                                ) {
                                                    R.drawable.favorite
                                                } else {
                                                    R.drawable.favorite_border
                                                },
                                            ),
                                        contentDescription = null,
                                        tint =
                                            if (albumWithSongs.album.bookmarkedAt != null) {
                                                MaterialTheme.colorScheme.error
                                            } else {
                                                MaterialTheme.colorScheme.onSurfaceVariant
                                            },
                                        modifier = Modifier.size(24.dp),
                                    )
                                }
                            }

                            // Play Button - Larger primary circular button
                            Surface(
                                onClick = onPlayClick,
                                color = MaterialTheme.colorScheme.primary,
                                shape = CircleShape,
                                modifier = Modifier.size(72.dp),
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.fillMaxSize(),
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.play),
                                        contentDescription = stringResource(R.string.play),
                                        tint = MaterialTheme.colorScheme.onPrimary,
                                        modifier = Modifier.size(32.dp),
                                    )
                                }
                            }

                            // Download Button - Smaller secondary button
                            Surface(
                                onClick = onDownloadClick,
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.size(48.dp),
                            ) {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    when (downloadState) {
                                        Download.STATE_COMPLETED -> Icon(
                                            painter = painterResource(R.drawable.check),
                                            contentDescription = null,
                                            modifier = Modifier.size(24.dp),
                                        )
                                        Download.STATE_DOWNLOADING, Download.STATE_QUEUED -> CircularProgressIndicator(
                                            strokeWidth = 2.dp,
                                            modifier = Modifier.size(24.dp),
                                        )
                                        else -> Icon(
                                            painter = painterResource(R.drawable.arrow_downward),
                                            contentDescription = null,
                                            modifier = Modifier.size(24.dp),
                                        )
                                    }
                                }
                            }
                        }
                    }
                    }
                }

                if (resumeTrackIndex != null) {
                    item(key = "resume_banner") {
                        AnimatedVisibility(
                            visible = !resumeDismissed && !isThisAlbumPlaying,
                            enter = fadeIn() + expandVertically(),
                            exit = fadeOut() + shrinkVertically(),
                        ) {
                            val resumeShape = SquircleShape(radius = 20.dp, cornerSmoothing = 0.45f)
                            // New Iride UI: flat monochrome card, matching BottomSheetMenu's dark panel styling.
                            val cardColor = if (topNavigationBarEnabled) Color(0xFF0A0A0A) else MaterialTheme.colorScheme.secondaryContainer
                            val onCardColor = if (topNavigationBarEnabled) Color.White.copy(alpha = 0.85f) else MaterialTheme.colorScheme.onSecondaryContainer
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                shape = resumeShape,
                                color = cardColor,
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 20.dp, vertical = 16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = stringResource(R.string.resume_album),
                                            style = MaterialTheme.typography.titleMedium,
                                            color = onCardColor,
                                        )
                                        Text(
                                            text = stringResource(
                                                R.string.resume_album_track_progress,
                                                resumeTrackIndex + 1,
                                                albumWithSongs?.songs?.size ?: 0,
                                            ),
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = onCardColor.copy(alpha = onCardColor.alpha * 0.7f),
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    TextButton(
                                        onClick = {
                                            val album = albumWithSongs
                                            resumeDismissed = true
                                            if (!isListenTogetherGuest && album != null) {
                                                playerConnection.service.getAutomix(playlistId)
                                                playerConnection.playQueue(
                                                    LocalAlbumRadio(album, startIndex = resumeTrackIndex),
                                                )
                                            }
                                        },
                                        colors = if (topNavigationBarEnabled) ButtonDefaults.textButtonColors(contentColor = Color.White) else ButtonDefaults.textButtonColors(),
                                    ) {
                                        Text(text = stringResource(R.string.resume))
                                    }
                                }
                            }
                        }
                    }
                }

                if (filteredSongs.isNotEmpty()) {
                    item(key = "songs_container") {
                        val hideDurationForStandard by rememberPreference(HideDurationForStandardSongsKey, defaultValue = true)
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                                filteredSongs.fastForEachIndexed { index, song ->
                                    val onCheckedChange: (Boolean) -> Unit = {
                                        if (it) {
                                            selection.add(song.id)
                                        } else {
                                            selection.remove(song.id)
                                        }
                                    }

                                    val featuredNames = song.orderedArtists.drop(1).map { it.name }
                                    val hideDuration = hideDurationForStandard && song.song.duration in 60..300
                                    val subtitleText = buildString {
                                        if (featuredNames.isNotEmpty()) {
                                            append("feat. ")
                                            append(featuredNames.joinToString(", "))
                                        }
                                        if (!hideDuration) {
                                            if (isNotEmpty()) append(" • ")
                                            append(makeTimeString(song.song.duration * 1000L))
                                        }
                                    }

                                    SongListItem(
                                        song = song,
                                        albumIndex = index + 1,
                                        subtitleOverride = subtitleText,
                                        // New Iride UI: featuring-artist credits ("feat. X") should
                                        // read in the same color as the rest of the row instead of
                                        // the default muted secondary tone.
                                        subtitleColor = if (topNavigationBarEnabled) Color.Unspecified else null,
                                        isActive = song.id == mediaMetadata?.id,
                                        isPlaying = isPlaying,
                                        trailingContent = {
                                            if (inSelectMode) {
                                                Checkbox(
                                                    checked = song.id in selection,
                                                    onCheckedChange = onCheckedChange,
                                                )
                                            } else {
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
                                            }
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .combinedClickable(
                                                onClick = {
                                                    if (inSelectMode) {
                                                        onCheckedChange(song.id !in selection)
                                                    } else if (!isListenTogetherGuest) {
                                                        if (song.id == mediaMetadata?.id) {
                                                            playerConnection.togglePlayPause()
                                                        } else {
                                                            playerConnection.service.getAutomix(playlistId)
                                                            playerConnection.playQueue(
                                                                LocalAlbumRadio(albumWithSongs, startIndex = index),
                                                            )
                                                        }
                                                    }
                                                },
                                                onLongClick = {
                                                    if (!inSelectMode) {
                                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                        inSelectMode = true
                                                        onCheckedChange(true)
                                                    }
                                                },
                                            ),
                                    )
                                }
                        }
                    }
                }

                if (otherVersions.isNotEmpty()) {
                    item(key = "other_versions_title") {
                        NavigationTitle(
                            title = stringResource(R.string.other_versions),
                            modifier = Modifier.animateItem(),
                        )
                    }
                    item(key = "other_versions_list") {
                        LazyRow(
                            contentPadding = WindowInsets.systemBars.only(WindowInsetsSides.Horizontal).asPaddingValues(),
                        ) {
                            items(
                                items = otherVersions.distinctBy { it.id },
                                key = { "album_other_${it.id}" },
                            ) { item ->
                                YouTubeGridItem(
                                    item = item,
                                    isActive = mediaMetadata?.album?.id == item.id,
                                    isPlaying = isPlaying,
                                    coroutineScope = scope,
                                    modifier =
                                        Modifier
                                            .combinedClickable(
                                                onClick = { navController.navigate("album/${item.id}") },
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
                                            ).animateItem(),
                                )
                            }
                        }
                    }
                }

                if (similarAlbums.isNotEmpty()) {
                    item(key = "similar_albums_title") {
                        NavigationTitle(
                            title = stringResource(R.string.similar_albums),
                            modifier = Modifier.animateItem(),
                        )
                    }
                    item(key = "similar_albums_list") {
                        LazyRow(
                            contentPadding = WindowInsets.systemBars.only(WindowInsetsSides.Horizontal).asPaddingValues(),
                        ) {
                            items(
                                items = similarAlbums.distinctBy { it.id },
                                key = { "album_similar_${it.id}" },
                            ) { item ->
                                YouTubeGridItem(
                                    item = item,
                                    isActive = mediaMetadata?.album?.id == item.id,
                                    isPlaying = isPlaying,
                                    coroutineScope = scope,
                                    modifier =
                                        Modifier
                                            .combinedClickable(
                                                onClick = { navController.navigate("album/${item.id}") },
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
                                            ).animateItem(),
                                )
                            }
                        }
                    }
                }
            } else {
                if (hasError) {
                    item(key = "error") {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(32.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = stringResource(R.string.error_title),
                                    style = MaterialTheme.typography.bodyLarge,
                                )
                                Spacer(Modifier.height(12.dp))
                                Button(onClick = viewModel::retry) {
                                    Text(stringResource(R.string.retry))
                                }
                            }
                        }
                    }
                } else {
                    item(key = "loading") {
                        Box(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            IrideLoadingIndicator()
                        }
                    }
                }
            }
        }

    // Top-bar mirrors of the header's like/play/download actions (New Iride UI only) — the header
    // versions close over a non-null local `albumWithSongs`, these close over the nullable top-level
    // state so they're safe to call before the album has loaded.
    val onTopBarLikeClick: () -> Unit = {
        albumWithSongs?.let { current ->
            database.query {
                update(current.album.toggleLike())
            }
        }
    }
    val onTopBarPlayClick: () -> Unit = {
        if (!isListenTogetherGuest) {
            albumWithSongs?.let { current ->
                playerConnection.service.getAutomix(playlistId)
                playerConnection.playQueue(LocalAlbumRadio(current))
            }
        }
    }
    val onTopBarDownloadClick: () -> Unit = {
        albumWithSongs?.let { current ->
            when (downloadState) {
                Download.STATE_COMPLETED -> showRemoveDownloadDialog = true
                Download.STATE_DOWNLOADING, Download.STATE_QUEUED -> {
                    current.songs.forEach { song ->
                        DownloadService.sendRemoveDownload(
                            context,
                            ExoDownloadService::class.java,
                            song.id,
                            false,
                        )
                    }
                }
                else -> {
                    current.songs.forEach { song ->
                        val downloadRequest =
                            DownloadRequest
                                .Builder(song.id, song.id.toUri())
                                .setCustomCacheKey(song.id)
                                .setData(song.song.title.toByteArray())
                                .build()
                        DownloadService.sendAddDownload(
                            context,
                            ExoDownloadService::class.java,
                            downloadRequest,
                            false,
                        )
                    }
                }
            }
        }
    }

    val topBarNavigationIcon: @Composable () -> Unit = {
        if (inSelectMode) {
            IconButton(onClick = onExitSelectionMode) {
                Icon(
                    painter = painterResource(R.drawable.close),
                    contentDescription = null,
                )
            }
        } else {
            IconButton(
                onClick = { navController.navigateUp() },
                onLongClick = { navController.backToMain() },
            ) {
                Icon(
                    painter = painterResource(R.drawable.arrow_back),
                    contentDescription = null,
                )
            }
        }
    }
    val topBarActions: @Composable RowScope.() -> Unit = {
        if (inSelectMode) {
            Checkbox(
                checked = selection.size == filteredSongs.size && selection.isNotEmpty(),
                onCheckedChange = {
                    if (selection.size == filteredSongs.size) {
                        selection.clear()
                    } else {
                        selection.clear()
                        selection.addAll(filteredSongs.map { it.id })
                    }
                },
            )
            IconButton(
                enabled = selection.isNotEmpty(),
                onClick = {
                    menuState.show {
                        SelectionSongMenu(
                            songSelection =
                                selection.mapNotNull { songId ->
                                    filteredSongs.find { it.id == songId }
                                },
                            onDismiss = menuState::dismiss,
                            clearAction = onExitSelectionMode,
                        )
                    }
                },
            ) {
                Icon(
                    painter = painterResource(R.drawable.more_vert),
                    contentDescription = null,
                )
            }
        } else {
            val currentAlbumWithSongs = albumWithSongs
            if (currentAlbumWithSongs != null) {
                val albumForMenu = Album(currentAlbumWithSongs.album, currentAlbumWithSongs.artists)
                IconButton(
                    onClick = {
                        menuState.show {
                            AlbumMenu(
                                originalAlbum = albumForMenu,
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
            }
        }
    }

    if (topNavigationBarEnabled) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(topBarBackgroundColor)
                .statusBarsPadding()
                .height(56.dp)
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            topBarNavigationIcon()
            Text(
                text = if (inSelectMode) pluralStringResource(R.plurals.n_selected, selection.size, selection.size) else "",
                style = TextStyle(
                    fontFamily = SpaceMonoFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    letterSpacing = (-0.1).sp,
                ),
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 4.dp),
            )
            if (!inSelectMode && albumWithSongs != null) {
                IrideOutlineIconButton(
                    onClick = onTopBarLikeClick,
                    icon = if (albumWithSongs?.album?.bookmarkedAt != null) R.drawable.favorite else R.drawable.favorite_border,
                    contentDescription = null,
                    size = 40.dp,
                    iconSize = 20.dp,
                )
                IrideOutlineIconButton(
                    onClick = onTopBarPlayClick,
                    icon = R.drawable.ic_iride_play,
                    contentDescription = stringResource(R.string.play),
                    size = 40.dp,
                    iconSize = 20.dp,
                )
                IrideOutlineIconButton(
                    onClick = onTopBarDownloadClick,
                    icon = when (downloadState) {
                        Download.STATE_COMPLETED -> R.drawable.check
                        else -> R.drawable.arrow_downward
                    },
                    contentDescription = null,
                    loading = downloadState == Download.STATE_DOWNLOADING || downloadState == Download.STATE_QUEUED,
                    size = 40.dp,
                    iconSize = 20.dp,
                )
            }
            topBarActions()
        }
    } else {
        TopAppBar(
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface,
                scrolledContainerColor = MaterialTheme.colorScheme.surface,
                navigationIconContentColor = MaterialTheme.colorScheme.onBackground,
                actionIconContentColor = MaterialTheme.colorScheme.onBackground,
                titleContentColor = MaterialTheme.colorScheme.onBackground,
            ),
            title = {
                if (inSelectMode) {
                    Text(pluralStringResource(R.plurals.n_selected, selection.size, selection.size))
                }
            },
            navigationIcon = topBarNavigationIcon,
            actions = topBarActions,
        )
    }
}