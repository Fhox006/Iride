/** Metrolist Project (C) 2026 Licensed under GPL-3.0 | See git history for contributors */
package com.metrolist.music.ui.screens.playlist


import com.metrolist.music.ui.component.IrideFolioPrimaryButton
import com.metrolist.music.ui.component.IrideFolioSecondaryButton

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import com.metrolist.music.ui.component.irideFolioCombinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
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
import com.metrolist.music.ui.component.gestureNavigationEnabled
import com.metrolist.music.constants.AlbumTopGradientKey
import com.metrolist.music.constants.HideExplicitKey
import com.metrolist.music.constants.IrideBaseBorderWidth
import com.metrolist.music.constants.PlayerBackgroundStyle
import com.metrolist.music.constants.PlayerBackgroundStyleKey
import com.metrolist.music.db.entities.Playlist
import com.metrolist.music.db.entities.PlaylistEntity
import com.metrolist.music.db.entities.PlaylistSongMap
import com.metrolist.music.discovery.MixMood
import com.metrolist.music.discovery.MixPosterPhotoSource
import com.metrolist.music.discovery.MixPosterRepository
import com.metrolist.music.discovery.MoodMixes
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
import com.metrolist.music.ui.component.IrideCollectionTitle
import com.metrolist.music.ui.component.IrideCollectionPageLayout
import com.metrolist.music.ui.component.collectionExpansionTarget
import com.metrolist.music.ui.component.LocalCollectionExpansionProgress
import com.metrolist.music.ui.component.IridePlaylistControlPanel
import com.metrolist.music.ui.component.IridePressEffect
import com.metrolist.music.ui.component.IrideFolioCollectionSearchBox
import com.metrolist.music.ui.component.SongMenuIconButton
import com.metrolist.music.ui.component.LocalMenuState
import com.metrolist.music.ui.component.MixPosterPlaceholder
import com.metrolist.music.ui.component.TopScreenGradientBackground
import com.metrolist.music.ui.component.YouTubeListItem
import com.metrolist.music.ui.component.rememberGenreFilter
import com.metrolist.music.ui.menu.YouTubePlaylistMenu
import com.metrolist.music.ui.menu.YouTubeSelectionSongMenu
import com.metrolist.music.ui.menu.YouTubeSongMenu
import com.metrolist.music.ui.theme.IBMPlexMonoFontFamily
import com.metrolist.music.ui.theme.strokeCard
import com.metrolist.music.ui.utils.IrideMotion
import com.metrolist.music.ui.utils.backToMain
import com.metrolist.music.ui.utils.headerEnter
import com.metrolist.music.ui.utils.irideArtworkOverlayBorder
import com.metrolist.music.ui.utils.irideEnter
import com.metrolist.music.ui.utils.irideEnterScale
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
import kotlinx.coroutines.withContext
import sv.lib.squircleshape.SquircleShape

@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalFoundationApi::class,
    ExperimentalMaterial3ExpressiveApi::class,
)
@Composable
fun OnlinePlaylistScreen(
    navController: NavController,
    viewModel: OnlinePlaylistViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val menuState = LocalMenuState.current
    val database = LocalDatabase.current
    val playerConnection = LocalPlayerConnection.current ?: return
    val listenTogetherManager = LocalListenTogetherManager.current
    val isListenTogetherGuest = listenTogetherManager?.let { it.isInRoom && !it.isHost } ?: false
    val coroutineScope = rememberCoroutineScope()

    val isPlaying by playerConnection.isEffectivelyPlaying.collectAsState()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()
    val queueTitle by playerConnection.queueTitle.collectAsState()

    val playlist by viewModel.playlist.collectAsState()
    val isMoodMixPlaylist =
        playlist?.let {
            it.id.startsWith("RDTMA") || MoodMixes.hasMixTitle(it.title)
        } == true
    var moodPosterPath by remember(playlist?.id) { mutableStateOf<String?>(null) }
    LaunchedEffect(playlist?.id, isMoodMixPlaylist) {
        val currentPlaylist = playlist ?: return@LaunchedEffect
        moodPosterPath = null
        if (isMoodMixPlaylist) {
            repeat(60) {
                val poster =
                    withContext(Dispatchers.IO) {
                        MixPosterRepository.cachedPoster(
                            context,
                            currentPlaylist.id,
                            currentPlaylist.thumbnail,
                        )
                    }
                moodPosterPath = poster?.imagePath
                if (poster?.photoSource == MixPosterPhotoSource.ARTIST) {
                    return@LaunchedEffect
                }
                delay(500)
            }
        }
    }
    val songs by viewModel.playlistSongs.collectAsState()
    val dbPlaylist by viewModel.dbPlaylist.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isLoadingMore by viewModel.isLoadingMore.collectAsState()
    val error by viewModel.error.collectAsState()
    val isPodcastPlaylist = viewModel.isPodcastPlaylist

    val hideExplicit by rememberPreference(key = HideExplicitKey, defaultValue = false)
    val albumTopGradientEnabled by rememberPreference(AlbumTopGradientKey, defaultValue = true)
    val playerBackgroundStyle by
        rememberEnumPreference(
            PlayerBackgroundStyleKey,
            defaultValue = PlayerBackgroundStyle.BETTER_ANIMATED_GRADIENT,
        )

    val isThisPlaylistQueueLoaded = playlist != null && queueTitle == playlist?.title
    val isThisPlaylistPlaying = isPlaying && isThisPlaylistQueueLoaded

    val lazyListState = rememberLazyListState()
    val snackbarHostState = remember { SnackbarHostState() }

    var isSearching by rememberSaveable { mutableStateOf(false) }
    var query by
        rememberSaveable(stateSaver = TextFieldValue.Saver) { mutableStateOf(TextFieldValue()) }
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val onExitSearch: () -> Unit = {
        isSearching = false
        query = TextFieldValue()
        focusManager.clearFocus()
        keyboardController?.hide()
    }

    var headerRevealed by rememberSaveable { mutableStateOf(false) }
    val revealedSections = remember { mutableSetOf<String>() }
    val playlistTitle = playlist?.title
    val titleTypingMs = 0
    LaunchedEffect(playlistTitle) {
        if (playlistTitle != null && !headerRevealed) {
            headerRevealed = true
        }
    }
    val collectionExpansion = LocalCollectionExpansionProgress.current
    val screenProgress =
        rememberEnterProgress(
            play = true,
            durationMillis = IrideMotion.Short,
            easing = IrideMotion.EaseOutQuart,
        )
    val genrePillsProgress = rememberSectionEnter("online_playlist_genre_pills", revealedSections)
    val songsSectionProgress = rememberSectionEnter("online_playlist_songs", revealedSections)

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
                    songs
                        .mapIndexed { i, s -> i to s }
                        .filter {
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
                )
        ) {
            mutableStateListOf()
        }
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

        if (
            selectionAnchorSongId != null &&
                filteredSongs.none { it.second.id == selectionAnchorSongId }
        ) {
            selectionAnchorSongId =
                filteredSongs.firstOrNull { it.second.id in selection }?.second?.id
        }
    }

    if (isSearching) {
        BackHandler(onBack = onExitSearch)
    } else if (inSelectMode) {
        BackHandler(onBack = onExitSelectionMode)
    }

    val downloadUtil = LocalDownloadUtil.current
    var downloadState by remember { mutableIntStateOf(Download.STATE_STOPPED) }
    LaunchedEffect(songs) {
        if (songs.isEmpty()) return@LaunchedEffect
        downloadUtil.downloads.collect { downloads ->
            downloadState =
                if (songs.all { downloads[it.id]?.state == Download.STATE_COMPLETED }) {
                    Download.STATE_COMPLETED
                } else if (
                    songs.all {
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
                    sourceRoute = "online_playlist/${current.id}",
                    title = current.title,
                    items = songs.shuffled().map { it.toMediaItem() },
                )
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
                        )
                    )
                }
            }
        }
    }
    val onControlPanelDownloadClick: () -> Unit = {
        when (downloadState) {
            Download.STATE_COMPLETED -> showRemoveDownloadDialog = true
            Download.STATE_DOWNLOADING,
            Download.STATE_QUEUED -> {
                songs.forEach { song ->
                    DownloadService.sendRemoveDownload(
                        context,
                        ExoDownloadService::class.java,
                        song.id,
                        false,
                    )
                }
            }
            else -> {
                songs.forEach { song ->
                    val downloadRequest =
                        DownloadRequest.Builder(song.id, song.id.toUri())
                            .setCustomCacheKey(song.id)
                            .setData(song.title.toByteArray())
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
                            )
                            .toggleLike()
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
                            }
                            .forEach(::insert)
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
                    text =
                        stringResource(
                            R.string.remove_download_playlist_confirm,
                            playlist?.title ?: "",
                        ),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(horizontal = 18.dp),
                )
            },
            buttons = {
                IrideFolioSecondaryButton(onClick = { showRemoveDownloadDialog = false }) {
                    Text(text = stringResource(android.R.string.cancel))
                }
                IrideFolioPrimaryButton(
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
                    }
                ) {
                    Text(text = stringResource(android.R.string.ok))
                }
            },
        )
    }

    val playlistGradientMediaMetadata =
        remember(playlist?.id, playlist?.thumbnail) {
            playlist?.let {
                MediaMetadata(
                    id = it.id,
                    title = it.title,
                    artists = emptyList(),
                    duration = 0,
                    thumbnailUrl = it.thumbnail,
                )
            }
        }

    Box(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxSize().graphicsLayer {
            alpha = if (collectionExpansion != null) 1f else screenProgress
        }) {

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
                                if (inSelectMode) R.drawable.close else R.drawable.arrow_back
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
                    SongMenuIconButton(
                        enabled = selection.isNotEmpty(),
                        onClick = {
                            menuState.show(anchored = true) {
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
                        SongMenuIconButton(
                            onClick = {
                                menuState.show(anchored = true) {
                                    YouTubePlaylistMenu(
                                        showPageControls = false,
                                        playlist = currentPlaylist,
                                        songs = songs,
                                        coroutineScope = coroutineScope,
                                        onDismiss = menuState::dismiss,
                                    )
                                }
                            }
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.more_vert),
                                contentDescription = null,
                            )
                        }
                    }
                }
            }
            IrideCollectionPageLayout(
                mediaMetadata = playlistGradientMediaMetadata,
                playerBackground = playerBackgroundStyle,
                gradientEnabled = albumTopGradientEnabled,
                title = playlist?.title.orEmpty(),
                state = lazyListState,
                showTopBar = true,
                showPageTitle = false,
                bottomPadding =
                    LocalPlayerAwareWindowInsets.current
                        .union(WindowInsets.ime)
                        .asPaddingValues()
                        .calculateBottomPadding(),
                navigationIcon = if (gestureNavigationEnabled() && !(inSelectMode || isSearching)) null else topBarNavigationIcon,
                actions = {
                    if (!inSelectMode && !isSearching) {
                        playlist?.let { current ->
                            IrideOutlineIconButton(
                                onClick = onTopBarLikeClick,
                                icon =
                                    if (dbPlaylist?.playlist?.bookmarkedAt != null)
                                        R.drawable.favorite
                                    else R.drawable.favorite_border,
                                contentDescription =
                                    stringResource(
                                        if (dbPlaylist?.playlist?.bookmarkedAt != null)
                                            R.string.iride_remove_star
                                        else R.string.iride_star
                                    ),
                                size = 40.dp,
                                iconSize = 20.dp,
                                pressEffect = IridePressEffect.Punch,
                                modifier =
                                    Modifier.irideEnterScale(
                                        rememberEnterProgress(
                                            play = true,
                                            durationMillis = IrideMotion.Short,
                                        )
                                    ),
                            )
                        }
                    }
                    topBarActions()
                },
                topBarContent =
                    if (inSelectMode)
                        ({
                            Text(
                                when {
                                    inSelectMode ->
                                        if (isPodcastPlaylist) {
                                            pluralStringResource(
                                                R.plurals.n_episode,
                                                selection.size,
                                                selection.size,
                                            )
                                        } else {
                                            pluralStringResource(
                                                R.plurals.n_song,
                                                selection.size,
                                                selection.size,
                                            )
                                        }
                                    else -> ""
                                }
                            )
                        })
                    else null,
            ) {
                if (playlist == null || songs.isEmpty()) {
                    playlist?.let { currentPlaylist ->
                        item(key = "empty_playlist_title") {
                            IrideCollectionTitle(
                                title = currentPlaylist.title,
                                modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                            )
                        }
                        item(key = "search_bar") {
                            IrideFolioCollectionSearchBox(
                                query = query,
                                onQueryChange = { query = it },
                                placeholderText = stringResource(R.string.search_playlists),
                                focusRequester = focusRequester,
                                onFocusChanged = { if (it.isFocused) isSearching = true },
                                onSearch = {},
                                onClear = { query = TextFieldValue() },
                                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                            )
                        }
                    }
                    if (isLoading) {
                        item(key = "loading_placeholder") {
                            Box(
                                modifier = Modifier.fillParentMaxSize().padding(32.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                IrideLoadingIndicator()
                            }
                        }
                    } else if (error != null) {
                        item(key = "error_placeholder") {
                            Column(
                                modifier = Modifier.fillParentMaxSize().padding(32.dp),
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
                                modifier = Modifier.fillParentMaxSize().padding(32.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = stringResource(R.string.playlist_is_empty),
                                    style = MaterialTheme.typography.bodyLarge,
                                )
                            }
                        }
                    }
                    if (playlist == null) {
                        item(key = "search_bar") {
                            IrideFolioCollectionSearchBox(
                                query = query,
                                onQueryChange = { query = it },
                                placeholderText = stringResource(R.string.search_playlists),
                                focusRequester = focusRequester,
                                onFocusChanged = { if (it.isFocused) isSearching = true },
                                onSearch = {},
                                onClear = { query = TextFieldValue() },
                                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                            )
                        }
                    }
                } else {
                    playlist?.let { playlist ->
                        if (!isSearching) {
                            item(key = "playlist_header") {
                                OnlinePlaylistHeader(
                                    playlist = playlist,
                                    coverThumbnail =
                                        if (isMoodMixPlaylist) moodPosterPath
                                        else playlist.thumbnail,
                                    isMoodMixPlaylist = isMoodMixPlaylist,
                                    songs = songs,
                                    dbPlaylist = dbPlaylist,
                                    navController = navController,
                                    coroutineScope = coroutineScope,
                                    continuation = viewModel.continuation,
                                    isPodcastPlaylist = isPodcastPlaylist,
                                    headerRevealed = headerRevealed,
                                    titleTypingMs = titleTypingMs,
                                    modifier = Modifier,
                                )
                            }
                        }

                        if (!isSearching) {
                            item(key = "control_panel") {
                                val controlPanelProgress =
                                    headerEnter(
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
                                    modifier =
                                        Modifier.padding(bottom = 12.dp)
                                            .irideEnterScale(controlPanelProgress),
                                )
                            }
                        }

                        item(key = "search_bar") {
                            IrideFolioCollectionSearchBox(
                                query = query,
                                onQueryChange = { query = it },
                                placeholderText = stringResource(R.string.search_playlists),
                                focusRequester = focusRequester,
                                onFocusChanged = { if (it.isFocused) isSearching = true },
                                onSearch = {},
                                onClear = { query = TextFieldValue() },
                                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                            )
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
                                    Modifier.irideFolioCombinedClickable(
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
                                                            initialSongs =
                                                                filteredSongs.map { it.second },
                                                            initialContinuation =
                                                                viewModel.continuation,
                                                            startIndex = index,
                                                        )
                                                    )
                                                }
                                            },
                                            onLongClick = {
                                                if (inSelectMode) {
                                                    val anchorIndex =
                                                        selectionAnchorSongId?.let { anchorSongId ->
                                                            filteredSongs.indexOfFirst {
                                                                it.second.id == anchorSongId
                                                            }
                                                        } ?: -1

                                                    if (anchorIndex == -1) {
                                                        onCheckedChange(true)
                                                        selectionAnchorSongId = songItem.id
                                                    } else {
                                                        val range =
                                                            if (anchorIndex <= index)
                                                                anchorIndex..index
                                                            else index..anchorIndex
                                                        for (rangeIndex in range) {
                                                            val rangeSongId =
                                                                filteredSongs[rangeIndex].second.id
                                                            if (rangeSongId !in selection) {
                                                                selection.add(rangeSongId)
                                                            }
                                                        }
                                                    }
                                                } else {
                                                    menuState.show(anchored = true) {
                                                        YouTubeSongMenu(
                                                            song = songItem,
                                                            navController = navController,
                                                            onDismiss = menuState::dismiss,
                                                        )
                                                    }
                                                }
                                            },
                                        )
                                        .animateItem(placementSpec = IrideMotion.PlacementSpec)
                                        .irideEnter(songsSectionProgress, 6.dp),
                                trailingContent = {
                                    if (inSelectMode) {
                                        Checkbox(
                                            checked = songItem.id in selection,
                                            onCheckedChange = onCheckedChange,
                                        )
                                    } else {
                                        SongMenuIconButton(
                                            onClick = {
                                                menuState.show(anchored = true) {
                                                    YouTubeSongMenu(
                                                        song = songItem,
                                                        navController = navController,
                                                        onDismiss = menuState::dismiss,
                                                    )
                                                }
                                            }
                                        ) {
                                            Icon(painterResource(R.drawable.more_vert), null)
                                        }
                                    }
                                },
                            )
                        }

                        if (isLoadingMore) {
                            item(key = "loading_more") {
                                Box(
                                    modifier = Modifier.fillMaxWidth().padding(16.dp),
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

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

@Composable
private fun OnlinePlaylistHeader(
    playlist: PlaylistItem,
    coverThumbnail: String?,
    isMoodMixPlaylist: Boolean,
    songs: List<SongItem>,
    dbPlaylist: Playlist?,
    navController: NavController,
    coroutineScope: CoroutineScope,
    continuation: String?,
    isPodcastPlaylist: Boolean = false,
    headerRevealed: Boolean = false,
    titleTypingMs: Int = 0,
    modifier: Modifier = Modifier,
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
                } else if (
                    songs.all {
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
                    text =
                        stringResource(R.string.remove_download_playlist_confirm, playlist.title),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(horizontal = 18.dp),
                )
            },
            buttons = {
                IrideFolioSecondaryButton(onClick = { showRemoveDownloadDialog = false }) {
                    Text(text = stringResource(android.R.string.cancel))
                }
                IrideFolioPrimaryButton(
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
                    }
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
        var coverLoaded by remember(playlist.id) { mutableStateOf(false) }
        var skipCoverEnterAnim by remember(playlist.id) { mutableStateOf(false) }
        LaunchedEffect(playlist.id, coverThumbnail, isMoodMixPlaylist) {
            if (isMoodMixPlaylist && coverThumbnail == null) coverLoaded = true
        }
        val animatedCoverProgress =
            rememberEnterProgress(
                play = coverLoaded,
                durationMillis = 420,
                easing = IrideMotion.EaseOutQuart,
            )
        val coverProgress = if (skipCoverEnterAnim || LocalCollectionExpansionProgress.current != null) 1f else animatedCoverProgress
        val coverModifier =
            Modifier.size(240.dp).collectionExpansionTarget(ready = coverLoaded)
                .graphicsLayer {
                    alpha = coverProgress
                    val s = lerp(0.94f, 1f, coverProgress)
                    scaleX = s
                    scaleY = s
                }
                .irideArtworkOverlayBorder(
                    IrideBaseBorderWidth,
                    MaterialTheme.colorScheme.strokeCard,
                    playlistCoverSquircle,
                )
                .shadow(
                    elevation = 20.dp,
                    shape = playlistCoverSquircle,
                    spotColor = Color.Black.copy(alpha = 0.5f),
                )
                .clip(playlistCoverSquircle)
                .background(MaterialTheme.colorScheme.surfaceVariant)

        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            if (isMoodMixPlaylist && coverThumbnail == null) {
                MixPosterPlaceholder(
                    title = playlist.title,
                    mood = MixMood.fromMixTitle(playlist.title) ?: MixMood.CHILL,
                    modifier = coverModifier,
                )
            } else {
                AsyncImage(
                    model = ImageRequest.Builder(context).data(coverThumbnail).build(),
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
                    modifier = coverModifier,
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        IrideCollectionTitle(title = playlist.title)

        Spacer(modifier = Modifier.height(8.dp))

        val totalDuration = songs.sumOf { it.duration ?: 0 }
        val metadataLine = buildString {
            append(
                if (isPodcastPlaylist) {
                    pluralStringResource(R.plurals.n_episode, songs.size, songs.size)
                } else {
                    pluralStringResource(R.plurals.n_song, songs.size, songs.size)
                }
            )
            if (totalDuration > 0) {
                append(" • ")
                append(makeTimeString(totalDuration * 1000L))
            }
        }
        val metadataProgress =
            headerEnter(
                revealed = headerRevealed,
                play = true,
                delayMillis = titleTypingMs + 20,
                durationMillis = IrideMotion.Short,
            )
        Text(
            text = metadataLine,
            style = TextStyle(fontFamily = IBMPlexMonoFontFamily, fontSize = 13.sp),
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f),
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().revealMask(metadataProgress),
        )
    }
}
