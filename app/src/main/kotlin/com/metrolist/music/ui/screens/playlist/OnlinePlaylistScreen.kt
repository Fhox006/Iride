/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.screens.playlist

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import sv.lib.squircleshape.SquircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import com.metrolist.music.ui.theme.SpaceMonoFontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastAny
import androidx.compose.ui.util.fastForEachReversed
import androidx.compose.ui.util.lerp
import androidx.core.net.toUri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.DownloadRequest
import androidx.media3.exoplayer.offline.DownloadService
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import coil3.compose.AsyncImagePainter
import coil3.decode.DataSource
import coil3.request.ImageRequest
import com.metrolist.innertube.models.PlaylistItem
import com.metrolist.innertube.models.SongItem
import com.metrolist.music.LocalDatabase
import com.metrolist.music.LocalDownloadUtil
import com.metrolist.music.LocalListenTogetherManager
import com.metrolist.music.LocalPlayerAwareWindowInsets
import com.metrolist.music.LocalPlayerConnection
import com.metrolist.music.LocalSyncUtils
import com.metrolist.music.R
import com.metrolist.music.constants.AlbumTopGradientKey
import com.metrolist.music.constants.HideExplicitKey
import com.metrolist.music.constants.IrideBaseBorderWidth
import com.metrolist.music.constants.PlayerBackgroundStyle
import com.metrolist.music.constants.PlayerBackgroundStyleKey
import com.metrolist.music.db.entities.Playlist
import com.metrolist.music.db.entities.PlaylistEntity
import com.metrolist.music.db.entities.PlaylistSongMap
import com.metrolist.music.extensions.toMediaItem
import com.metrolist.music.models.MediaMetadata
import com.metrolist.music.models.toMediaMetadata
import com.metrolist.music.playback.ExoDownloadService
import com.metrolist.music.playback.queues.ListQueue
import com.metrolist.music.playback.queues.YouTubePlaylistQueue
import com.metrolist.music.ui.component.DefaultDialog
import com.metrolist.music.ui.component.GenrePillsRow
import com.metrolist.music.ui.component.GenreSongInfo
import com.metrolist.music.ui.component.IconButton
import com.metrolist.music.ui.component.IrideLoadingIndicator
import com.metrolist.music.ui.component.IrideOutlineIconButton
import com.metrolist.music.ui.component.IridePlaylistControlPanel
import com.metrolist.music.ui.component.IridePressEffect
import com.metrolist.music.ui.component.LocalMenuState
import com.metrolist.music.ui.component.TopScreenGradientBackground
import com.metrolist.music.ui.component.TypewriterText
import com.metrolist.music.ui.component.YouTubeListItem
import com.metrolist.music.ui.component.frostedTopBarBackground
import com.metrolist.music.ui.component.recordFrostBackdrop
import com.metrolist.music.ui.component.rememberFrostBackdrop
import com.metrolist.music.ui.component.rememberGenreFilter
import com.metrolist.music.ui.component.rememberRubberBandPull
import com.metrolist.music.ui.component.rubberBandOverscroll
import com.metrolist.music.ui.menu.YouTubePlaylistMenu
import com.metrolist.music.ui.menu.YouTubeSelectionSongMenu
import com.metrolist.music.ui.menu.YouTubeSongMenu
import com.metrolist.music.ui.screens.search.IrideSearchBox
import com.metrolist.music.ui.utils.IrideMotion
import com.metrolist.music.ui.utils.backToMain
import com.metrolist.music.ui.utils.headerEnter
import com.metrolist.music.ui.utils.irideEnter
import com.metrolist.music.ui.utils.irideEnterScale
import com.metrolist.music.ui.utils.rememberDiscreteProgress
import com.metrolist.music.ui.utils.rememberEnterProgress
import com.metrolist.music.ui.utils.rememberSectionEnter
import com.metrolist.music.ui.utils.revealMask
import com.metrolist.music.utils.makeTimeString
import com.metrolist.music.utils.rememberEnumPreference
import com.metrolist.music.utils.rememberPreference
import com.metrolist.music.viewmodels.OnlinePlaylistViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun OnlinePlaylistScreen(
    navController: NavController,
    viewModel: OnlinePlaylistViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val menuState = LocalMenuState.current
    val database = LocalDatabase.current
    val haptic = LocalHapticFeedback.current
    val playerConnection = LocalPlayerConnection.current ?: return
    val listenTogetherManager = LocalListenTogetherManager.current
    val isListenTogetherGuest = listenTogetherManager?.let { it.isInRoom && !it.isHost } ?: false
    val coroutineScope = rememberCoroutineScope()

    val isPlaying by playerConnection.isEffectivelyPlaying.collectAsState()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()
    val queueTitle by playerConnection.queueTitle.collectAsState()

    val playlist by viewModel.playlist.collectAsState()
    val songs by viewModel.playlistSongs.collectAsState()
    val dbPlaylist by viewModel.dbPlaylist.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isLoadingMore by viewModel.isLoadingMore.collectAsState()
    val error by viewModel.error.collectAsState()
    val isPodcastPlaylist = viewModel.isPodcastPlaylist

    val hideExplicit by rememberPreference(key = HideExplicitKey, defaultValue = false)
    val albumTopGradientEnabled by rememberPreference(AlbumTopGradientKey, defaultValue = true)
    val playerBackgroundStyle by rememberEnumPreference(
        PlayerBackgroundStyleKey,
        defaultValue = PlayerBackgroundStyle.BETTER_ANIMATED_GRADIENT,
    )

    val isThisPlaylistQueueLoaded = playlist != null && queueTitle == playlist?.title
    val isThisPlaylistPlaying = isPlaying && isThisPlaylistQueueLoaded

    val lazyListState = rememberLazyListState()
    val snackbarHostState = remember { SnackbarHostState() }
    val frostBackdrop = rememberFrostBackdrop()
    val headerPull = rememberRubberBandPull()


    var isSearching by rememberSaveable { mutableStateOf(false) }
    var query by rememberSaveable(stateSaver = TextFieldValue.Saver) { mutableStateOf(TextFieldValue()) }
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val onExitSearch: () -> Unit = {
        isSearching = false
        query = TextFieldValue()
        focusManager.clearFocus()
        keyboardController?.hide()
    }

    // New Iride UI one-shot entrance: mirrors AlbumScreen's arrival choreography — title types out
    // once, then the rest of the header/sections fade in, never replaying on scroll-back.
    var headerRevealed by rememberSaveable { mutableStateOf(false) }
    val revealedSections = remember { mutableSetOf<String>() }
    val playlistTitle = playlist?.title
    val titleTypingMs = remember(playlistTitle) {
        val length = playlistTitle?.length ?: 0
        if (length == 0) 0 else minOf(26 * length, 700)
    }
    LaunchedEffect(playlistTitle) {
        if (playlistTitle != null && !headerRevealed) {
            delay(titleTypingMs + 60L + IrideMotion.Short)
            headerRevealed = true
        }
    }
    val screenProgress = rememberEnterProgress(play = true, durationMillis = IrideMotion.Short, easing = IrideMotion.EaseOutQuart)
    val genrePillsProgress = rememberSectionEnter("online_playlist_genre_pills", revealedSections)
    val songsSectionProgress = rememberSectionEnter("online_playlist_songs", revealedSections)

    // Window-space Y of the header title's bottom edge and of the top bar's bottom edge — the
    // overlay (glass + mirrored title) shows once the big title is behind the bar or scrolled past,
    // and tweens in/out at a fixed duration instead of following the scroll pixel-by-pixel.
    var nameBottomPx by remember { mutableStateOf(Float.MAX_VALUE) }
    var topBarBottomPx by remember { mutableStateOf(0f) }
    val headerTitleCovered by remember {
        derivedStateOf {
            headerRevealed && (
                lazyListState.firstVisibleItemIndex > 1 ||
                    nameBottomPx <= topBarBottomPx
                )
        }
    }
    val topBarRevealProgress = rememberDiscreteProgress(headerTitleCovered)

    val genreFilter =
        rememberGenreFilter(
            remember(songs) {
                songs.map { GenreSongInfo(it.id, it.title, it.artists.firstOrNull()?.name) }
            },
            cacheKey = playlist?.id,
        )

    val filteredSongs =
        remember(songs, query, genreFilter.selectedGenre, genreFilter.genreBySongId) {
            val base =
                if (query.text.isEmpty()) {
                    songs.mapIndexed { i, s -> i to s }
                } else {
                    songs.mapIndexed { i, s -> i to s }.filter {
                        it.second.title.contains(query.text, true) ||
                                it.second.artists.fastAny { a -> a.name.contains(query.text, true) }
                    }
                }
            base.filter { genreFilter.matches(it.second.id) }
        }

    var inSelectMode by rememberSaveable { mutableStateOf(false) }
    val selection =
        rememberSaveable(
            saver =
                listSaver<MutableList<String>, String>(
                    save = { it.toList() },
                    restore = { it.toMutableStateList() },
                ),
        ) { mutableStateListOf() }
    var selectionAnchorSongId by rememberSaveable { mutableStateOf<String?>(null) }
    val onExitSelectionMode = {
        inSelectMode = false
        selection.clear()
        selectionAnchorSongId = null
    }

    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(isSearching) { if (isSearching) focusRequester.requestFocus() }

    LaunchedEffect(filteredSongs) {
        selection.fastForEachReversed { songId ->
            if (filteredSongs.find { it.second.id == songId } == null) {
                selection.remove(songId)
            }
        }

        if (selectionAnchorSongId != null && filteredSongs.none { it.second.id == selectionAnchorSongId }) {
            selectionAnchorSongId = filteredSongs.firstOrNull { it.second.id in selection }?.second?.id
        }
    }

    if (isSearching) {
        BackHandler(onBack = onExitSearch)
    } else if (inSelectMode) {
        BackHandler(onBack = onExitSelectionMode)
    }

    // Top-bar mirrors of the header's play/download actions and the control panel pill (New Iride UI
    // only) — the header versions live in a separate composable closing over a non-null `playlist`
    // param, these close over the nullable top-level state so they're safe before the playlist loads.
    val downloadUtil = LocalDownloadUtil.current
    var downloadState by remember { mutableIntStateOf(Download.STATE_STOPPED) }
    LaunchedEffect(songs) {
        if (songs.isEmpty()) return@LaunchedEffect
        downloadUtil.downloads.collect { downloads ->
            downloadState =
                if (songs.all { downloads[it.id]?.state == Download.STATE_COMPLETED }) {
                    Download.STATE_COMPLETED
                } else if (songs.all {
                        downloads[it.id]?.state == Download.STATE_QUEUED ||
                            downloads[it.id]?.state == Download.STATE_DOWNLOADING ||
                            downloads[it.id]?.state == Download.STATE_COMPLETED
                    }
                ) {
                    Download.STATE_DOWNLOADING
                } else {
                    Download.STATE_STOPPED
                }
        }
    }
    var showRemoveDownloadDialog by remember { mutableStateOf(false) }

    val onControlPanelShuffleClick: () -> Unit = {
        playlist?.let { current ->
            playerConnection.playQueue(
                ListQueue(
                    title = current.title,
                    items = songs.shuffled().map { it.toMediaItem() },
                ),
            )
        }
    }
    val onControlPanelPlayClick: () -> Unit = {
        if (!isListenTogetherGuest) {
            if (isThisPlaylistQueueLoaded) {
                playerConnection.togglePlayPause()
            } else {
                playlist?.let { current ->
                    playerConnection.playQueue(
                        YouTubePlaylistQueue(
                            playlistId = current.id,
                            playlistTitle = current.title,
                            initialSongs = songs,
                            initialContinuation = viewModel.continuation,
                        ),
                    )
                }
            }
        }
    }
    val onControlPanelDownloadClick: () -> Unit = {
        when (downloadState) {
            Download.STATE_COMPLETED -> showRemoveDownloadDialog = true
            Download.STATE_DOWNLOADING, Download.STATE_QUEUED -> {
                songs.forEach { song ->
                    DownloadService.sendRemoveDownload(context, ExoDownloadService::class.java, song.id, false)
                }
            }
            else -> {
                songs.forEach { song ->
                    val downloadRequest =
                        DownloadRequest
                            .Builder(song.id, song.id.toUri())
                            .setCustomCacheKey(song.id)
                            .setData(song.title.toByteArray())
                            .build()
                    DownloadService.sendAddDownload(context, ExoDownloadService::class.java, downloadRequest, false)
                }
            }
        }
    }
    val onTopBarLikeClick: () -> Unit = {
        playlist?.let { current ->
            if (dbPlaylist != null) {
                database.transaction {
                    val currentPlaylist = dbPlaylist!!.playlist
                    update(currentPlaylist, current)
                    update(currentPlaylist.toggleLike())
                }
            } else {
                database.transaction {
                    val playlistEntity =
                        PlaylistEntity(
                            name = current.title,
                            browseId = current.id,
                            thumbnailUrl = current.thumbnail,
                            isEditable = current.isEditable,
                            remoteSongCount =
                                current.songCountText?.let {
                                    Regex("""\d+""").find(it)?.value?.toIntOrNull()
                                },
                            playEndpointParams = current.playEndpoint?.params,
                            shuffleEndpointParams = current.shuffleEndpoint?.params,
                            radioEndpointParams = current.radioEndpoint?.params,
                        ).toggleLike()
                    insert(playlistEntity)
                    coroutineScope.launch(Dispatchers.IO) {
                        songs
                            .map { it.toMediaMetadata() }
                            .onEach(::insert)
                            .mapIndexed { index, song ->
                                PlaylistSongMap(
                                    songId = song.id,
                                    playlistId = playlistEntity.id,
                                    position = index,
                                    setVideoId = song.setVideoId,
                                )
                            }.forEach(::insert)
                    }
                }
            }
        }
    }

    if (showRemoveDownloadDialog) {
        DefaultDialog(
            onDismiss = { showRemoveDownloadDialog = false },
            content = {
                Text(
                    text = stringResource(R.string.remove_download_playlist_confirm, playlist?.title ?: ""),
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
                        songs.forEach { song ->
                            DownloadService.sendRemoveDownload(context, ExoDownloadService::class.java, song.id, false)
                        }
                    },
                ) {
                    Text(text = stringResource(android.R.string.ok))
                }
            },
        )
    }

    val playlistGradientMediaMetadata = remember(playlist?.id, playlist?.thumbnail) {
        playlist?.let {
            MediaMetadata(id = it.id, title = it.title, artists = emptyList(), duration = 0, thumbnailUrl = it.thumbnail)
        }
    }

    // Two boxes, not one, exactly like AlbumScreen: the frosted top bar *samples* the backdrop
    // layer, so it must not be drawn inside the Box that records it. Nested, the bar's drawBehind
    // re-enters frostBackdrop.content (drawLayer of a RenderNode that is still mid-record) and the
    // platform throws — which is why the playlist screens crashed the moment the bar's glass turned
    // on (progress > 0) while the album screen never did.
    Box(modifier = Modifier.fillMaxSize()) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .recordFrostBackdrop(frostBackdrop)
            .graphicsLayer { alpha = screenProgress },
    ) {
        if (albumTopGradientEnabled) {
            TopScreenGradientBackground(
                mediaMetadata = playlistGradientMediaMetadata,
                playerBackground = playerBackgroundStyle,
            )
        }

        LazyColumn(
            state = lazyListState,
            modifier = Modifier
                .rubberBandOverscroll(Orientation.Vertical, lazyListState, headerPull),
            contentPadding = LocalPlayerAwareWindowInsets.current.union(WindowInsets.ime).asPaddingValues(),
        ) {
            item(key = "search_bar") {
                IrideSearchBox(
                    query = query,
                    onQueryChange = { query = it },
                    placeholderText = stringResource(R.string.search),
                    focusRequester = focusRequester,
                    onFocusChanged = { if (it.isFocused) isSearching = true },
                    onSearch = {},
                    onClear = { query = TextFieldValue() },
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                )
            }

            if (playlist == null || songs.isEmpty()) {
                if (isLoading) {
                    item(key = "loading_placeholder") {
                        Box(
                            modifier =
                                Modifier
                                    .fillParentMaxSize()
                                    .padding(32.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            IrideLoadingIndicator()
                        }
                    }
                } else if (error != null) {
                    item(key = "error_placeholder") {
                        Column(
                            modifier =
                                Modifier
                                    .fillParentMaxSize()
                                    .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                        ) {
                            Text(
                                text = error ?: stringResource(R.string.error_unknown),
                                style = MaterialTheme.typography.bodyLarge,
                                textAlign = TextAlign.Center,
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            androidx.compose.material3.TextButton(
                                onClick = { viewModel.retry() }
                            ) {
                                Text(stringResource(R.string.retry))
                            }
                        }
                    }
                } else if (!isLoading && songs.isEmpty()) {
                    item(key = "empty_placeholder") {
                        Box(
                            modifier =
                                Modifier
                                    .fillParentMaxSize()
                                    .padding(32.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = stringResource(R.string.playlist_is_empty),
                                style = MaterialTheme.typography.bodyLarge,
                            )
                        }
                    }
                }
            } else {
                playlist?.let { playlist ->
                    if (!isSearching) {
                        item(key = "playlist_header") {
                            OnlinePlaylistHeader(
                                playlist = playlist,
                                songs = songs,
                                dbPlaylist = dbPlaylist,
                                navController = navController,
                                coroutineScope = coroutineScope,
                                continuation = viewModel.continuation,
                                isPodcastPlaylist = isPodcastPlaylist,
                                headerRevealed = headerRevealed,
                                titleTypingMs = titleTypingMs,
                                onTitleBoundsChanged = { nameBottomPx = it },
                                modifier = Modifier.animateItem(),
                            )
                        }
                    }

                    if (!isSearching) {
                        item(key = "control_panel") {
                            val controlPanelProgress = headerEnter(
                                revealed = headerRevealed,
                                play = true,
                                delayMillis = titleTypingMs + 60,
                                durationMillis = IrideMotion.Short,
                            )
                            IridePlaylistControlPanel(
                                onShuffleClick = onControlPanelShuffleClick,
                                onPlayClick = onControlPanelPlayClick,
                                onDownloadClick = onControlPanelDownloadClick,
                                downloadState = downloadState,
                                isPlaying = isThisPlaylistPlaying,
                                modifier = Modifier
                                    .padding(bottom = 12.dp)
                                    .irideEnterScale(controlPanelProgress),
                            )
                        }
                    }

                    if (!isSearching) {
                        item(key = "genre_pills") {
                            GenrePillsRow(
                                state = genreFilter,
                                modifier = Modifier.irideEnter(genrePillsProgress, 6.dp),
                            )
                        }
                    }

                    itemsIndexed(filteredSongs) { index, (_, songItem) ->
                        val onCheckedChange: (Boolean) -> Unit = {
                            if (it) {
                                selection.add(songItem.id)
                            } else {
                                selection.remove(songItem.id)
                            }
                        }

                        YouTubeListItem(
                            item = songItem,
                            isActive = mediaMetadata?.id == songItem.id,
                            isPlaying = isPlaying,
                            isSelected = inSelectMode && songItem.id in selection,
                            modifier =
                                Modifier
                                    .combinedClickable(
                                        enabled = !hideExplicit || !songItem.explicit,
                                        onClick = {
                                            if (inSelectMode) {
                                                onCheckedChange(songItem.id !in selection)
                                            } else if (songItem.id == mediaMetadata?.id) {
                                                playerConnection.togglePlayPause()
                                            } else {
                                                playerConnection.playQueue(
                                                    YouTubePlaylistQueue(
                                                        playlistId = playlist.id,
                                                        playlistTitle = playlist.title,
                                                        initialSongs = filteredSongs.map { it.second },
                                                        initialContinuation = viewModel.continuation,
                                                        startIndex = index,
                                                    ),
                                                )
                                            }
                                        },
                                        onLongClick = {
                                            if (!inSelectMode) {
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                inSelectMode = true
                                                onCheckedChange(true)
                                                selectionAnchorSongId = songItem.id
                                            } else {
                                                val anchorIndex =
                                                    selectionAnchorSongId?.let { anchorSongId ->
                                                        filteredSongs.indexOfFirst { it.second.id == anchorSongId }
                                                    } ?: -1

                                                if (anchorIndex == -1) {
                                                    onCheckedChange(true)
                                                    selectionAnchorSongId = songItem.id
                                                } else {
                                                    val range = if (anchorIndex <= index) anchorIndex..index else index..anchorIndex
                                                    for (rangeIndex in range) {
                                                        val rangeSongId = filteredSongs[rangeIndex].second.id
                                                        if (rangeSongId !in selection) {
                                                            selection.add(rangeSongId)
                                                        }
                                                    }
                                                }
                                            }
                                        },
                                    ).animateItem(placementSpec = IrideMotion.PlacementSpec)
                                        .irideEnter(songsSectionProgress, 6.dp),
                            trailingContent = {
                                if (inSelectMode) {
                                    Checkbox(
                                        checked = songItem.id in selection,
                                        onCheckedChange = onCheckedChange,
                                    )
                                } else {
                                    IconButton(onClick = {
                                        menuState.show {
                                            YouTubeSongMenu(
                                                song = songItem,
                                                navController = navController,
                                                onDismiss = menuState::dismiss,
                                            )
                                        }
                                    }) {
                                        Icon(painterResource(R.drawable.more_vert), null)
                                    }
                                }
                            },
                        )
                    }

                    if (isLoadingMore) {
                        item(key = "loading_more") {
                            Box(
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                IrideLoadingIndicator()
                            }
                        }
                    }
                }
            }
        }

        }
        // --- everything below is a sibling of the recorded content, never inside it ---

        val topBarNavigationIcon: @Composable () -> Unit = {
            IconButton(
                onClick = {
                    if (isSearching) {
                        onExitSearch()
                    } else if (inSelectMode) {
                        onExitSelectionMode()
                    } else {
                        navController.navigateUp()
                    }
                },
                onLongClick = {
                    if (!isSearching && !inSelectMode) {
                        navController.backToMain()
                    }
                },
            ) {
                Icon(
                    painter =
                        painterResource(
                            if (inSelectMode) R.drawable.close else R.drawable.arrow_back,
                        ),
                    contentDescription = null,
                )
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
                            selection.addAll(filteredSongs.map { it.second.id })
                        }
                    },
                )
                IconButton(
                    enabled = selection.isNotEmpty(),
                    onClick = {
                        menuState.show {
                            YouTubeSelectionSongMenu(
                                songSelection =
                                    filteredSongs
                                        .filter { it.second.id in selection }
                                        .map { it.second },
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
            } else if (!isSearching) {
                playlist?.let { currentPlaylist ->
                    IconButton(
                        onClick = {
                            menuState.show {
                                YouTubePlaylistMenu(
                                    playlist = currentPlaylist,
                                    songs = songs,
                                    coroutineScope = coroutineScope,
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

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .onGloballyPositioned { topBarBottomPx = it.boundsInWindow().bottom }
                .frostedTopBarBackground(
                    progress = if (inSelectMode) 1f else topBarRevealProgress,
                    barColor = MaterialTheme.colorScheme.background,
                    strokeColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f),
                    backdrop = frostBackdrop,
                )
                .statusBarsPadding()
                .height(56.dp)
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            topBarNavigationIcon()
            Text(
                text = when {
                    inSelectMode -> if (isPodcastPlaylist) {
                        pluralStringResource(R.plurals.n_episode, selection.size, selection.size)
                    } else {
                        pluralStringResource(R.plurals.n_song, selection.size, selection.size)
                    }
                    isSearching -> ""
                    else -> playlist?.title.orEmpty()
                },
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
                    .padding(start = 4.dp)
                    .then(
                        if (inSelectMode || isSearching) {
                            Modifier
                        } else {
                            Modifier.irideEnter(topBarRevealProgress, 6.dp).revealMask(topBarRevealProgress)
                        },
                    ),
            )
            if (!inSelectMode && !isSearching) {
                playlist?.let { current ->
                    IrideOutlineIconButton(
                        onClick = onTopBarLikeClick,
                        icon = if (dbPlaylist?.playlist?.bookmarkedAt != null) R.drawable.favorite else R.drawable.favorite_border,
                        contentDescription = stringResource(
                            if (dbPlaylist?.playlist?.bookmarkedAt != null) R.string.remove_from_library else R.string.add_to_library,
                        ),
                        size = 40.dp,
                        iconSize = 20.dp,
                        pressEffect = IridePressEffect.Punch,
                        modifier = Modifier.irideEnterScale(
                            rememberEnterProgress(play = true, durationMillis = IrideMotion.Short),
                        ),
                    )
                }
            }
            topBarActions()
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

@Composable
private fun OnlinePlaylistHeader(
    playlist: PlaylistItem,
    songs: List<SongItem>,
    dbPlaylist: Playlist?,
    navController: NavController,
    coroutineScope: CoroutineScope,
    continuation: String?,
    isPodcastPlaylist: Boolean = false,
    headerRevealed: Boolean = false,
    titleTypingMs: Int = 0,
    modifier: Modifier = Modifier,
    onTitleBoundsChanged: (Float) -> Unit = {},
) {
    val playerConnection = LocalPlayerConnection.current ?: return
    val listenTogetherManager = LocalListenTogetherManager.current
    val isListenTogetherGuest = listenTogetherManager?.let { it.isInRoom && !it.isHost } ?: false
    val database = LocalDatabase.current
    val menuState = LocalMenuState.current
    val syncUtils = LocalSyncUtils.current
    val context = LocalContext.current
    val downloadUtil = LocalDownloadUtil.current
    var downloadState by remember { mutableIntStateOf(Download.STATE_STOPPED) }
    var showRemoveDownloadDialog by remember { mutableStateOf(false) }

    LaunchedEffect(songs) {
        if (songs.isEmpty()) return@LaunchedEffect
        downloadUtil.downloads.collect { downloads ->
            downloadState =
                if (songs.all { downloads[it.id]?.state == Download.STATE_COMPLETED }) {
                    Download.STATE_COMPLETED
                } else if (songs.all {
                        downloads[it.id]?.state == Download.STATE_QUEUED ||
                            downloads[it.id]?.state == Download.STATE_DOWNLOADING ||
                            downloads[it.id]?.state == Download.STATE_COMPLETED
                    }
                ) {
                    Download.STATE_DOWNLOADING
                } else {
                    Download.STATE_STOPPED
                }
        }
    }

    if (showRemoveDownloadDialog) {
        DefaultDialog(
            onDismiss = { showRemoveDownloadDialog = false },
            content = {
                Text(
                    text = stringResource(R.string.remove_download_playlist_confirm, playlist.title),
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
                        songs.forEach { song ->
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

    val playlistCoverSquircle = SquircleShape(radius = 12.dp, cornerSmoothing = 0.45f)

    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(
                    top = 12.dp,
                    bottom = 20.dp,
                )
                .padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // New Iride UI: fade the cover in once on a genuine new decode — a memory-cache hit
        // (revisiting a playlist already seen this session) resolves synchronously, so animating
        // that would replay the surfaceVariant placeholder every time.
        var coverLoaded by remember(playlist.id) { mutableStateOf(false) }
        var skipCoverEnterAnim by remember(playlist.id) { mutableStateOf(false) }
        val animatedCoverProgress = rememberEnterProgress(play = coverLoaded, durationMillis = 420, easing = IrideMotion.EaseOutQuart)
        val coverProgress = if (skipCoverEnterAnim) 1f else animatedCoverProgress

        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            AsyncImage(
                model = ImageRequest.Builder(context).data(playlist.thumbnail).build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                onState = { state ->
                    if (state is AsyncImagePainter.State.Success) {
                        if (state.result.dataSource == DataSource.MEMORY_CACHE) {
                            skipCoverEnterAnim = true
                        }
                        coverLoaded = true
                    }
                },
                modifier = Modifier
                    .size(240.dp)
                    .graphicsLayer {
                        alpha = coverProgress
                        val s = lerp(0.94f, 1f, coverProgress)
                        scaleX = s
                        scaleY = s
                    }
                    .shadow(
                        elevation = 20.dp,
                        shape = playlistCoverSquircle,
                        spotColor = Color.Black.copy(alpha = 0.5f),
                    )
                    .clip(playlistCoverSquircle)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .border(BorderStroke(IrideBaseBorderWidth, Color.White.copy(alpha = 0.22f)), playlistCoverSquircle),
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        TypewriterText(
            text = playlist.title,
            style = TextStyle(
                fontFamily = SpaceMonoFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                letterSpacing = (-0.2).sp,
            ),
            color = MaterialTheme.colorScheme.onBackground,
            // Keyed on the playlist, not the string: recomposing this screen must not retype it.
            resetKey = playlist.id,
            animate = !headerRevealed,
            maxLines = 3,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .onGloballyPositioned { onTitleBoundsChanged(it.boundsInWindow().bottom) },
        )

        Spacer(modifier = Modifier.height(8.dp))

        val totalDuration = songs.sumOf { it.duration ?: 0 }
        val metadataLine =
            buildString {
                append(
                    if (isPodcastPlaylist) {
                        pluralStringResource(R.plurals.n_episode, songs.size, songs.size)
                    } else {
                        pluralStringResource(R.plurals.n_song, songs.size, songs.size)
                    },
                )
                if (totalDuration > 0) {
                    append(" • ")
                    append(makeTimeString(totalDuration * 1000L))
                }
            }
        val metadataProgress = headerEnter(
            revealed = headerRevealed,
            play = true,
            delayMillis = titleTypingMs + 20,
            durationMillis = IrideMotion.Short,
        )
        Text(
            text = metadataLine,
            style = TextStyle(fontFamily = SpaceMonoFontFamily, fontSize = 13.sp),
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f),
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .revealMask(metadataProgress),
        )
        // Like/shuffle/play/download now live in the top bar + control panel pill.
    }
}
