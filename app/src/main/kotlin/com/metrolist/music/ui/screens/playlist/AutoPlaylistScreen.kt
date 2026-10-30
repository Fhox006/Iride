/** Metrolist Project (C) 2026 Licensed under GPL-3.0 | See git history for contributors */
package com.metrolist.music.ui.screens.playlist


import com.metrolist.music.ui.component.IrideFolioPrimaryButton
import com.metrolist.music.ui.component.IrideFolioSecondaryButton

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import com.metrolist.music.ui.component.irideFolioCombinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults.Indicator
import androidx.compose.material3.pulltorefresh.pullToRefresh
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastForEachReversed
import androidx.compose.ui.util.fastSumBy
import androidx.core.net.toUri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.DownloadRequest
import androidx.media3.exoplayer.offline.DownloadService
import androidx.navigation.NavController
import com.metrolist.innertube.YouTube
import com.metrolist.music.LocalDownloadUtil
import com.metrolist.music.LocalListenTogetherManager
import com.metrolist.music.LocalPlayerAwareWindowInsets
import com.metrolist.music.LocalPlayerConnection
import com.metrolist.music.R
import com.metrolist.music.constants.AlbumTopGradientKey
import com.metrolist.music.constants.AutoPlaylistSongSortDescendingKey
import com.metrolist.music.constants.AutoPlaylistSongSortType
import com.metrolist.music.constants.AutoPlaylistSongSortTypeKey
import com.metrolist.music.constants.HideExplicitKey
import com.metrolist.music.constants.LibraryViewType
import com.metrolist.music.constants.PlayerBackgroundStyle
import com.metrolist.music.constants.PlayerBackgroundStyleKey
import com.metrolist.music.constants.YtmSyncKey
import com.metrolist.music.db.entities.Song
import com.metrolist.music.extensions.toMediaItem
import com.metrolist.music.models.MediaMetadata
import com.metrolist.music.playback.ExoDownloadService
import com.metrolist.music.playback.queues.ListQueue
import com.metrolist.music.ui.component.DefaultDialog
import com.metrolist.music.ui.component.EmptyPlaceholder
import com.metrolist.music.ui.component.GenrePillsRow
import com.metrolist.music.ui.component.GenreSongInfo
import com.metrolist.music.ui.component.IrideGeneratedPlaylistHeader
import com.metrolist.music.ui.component.IconButton
import com.metrolist.music.ui.component.IrideCollectionTitle
import com.metrolist.music.ui.component.IrideFolioMenuAction
import com.metrolist.music.ui.component.IrideCollectionPageLayout
import com.metrolist.music.ui.component.IridePlaylistControlPanel
import com.metrolist.music.ui.component.IrideFolioCollectionSearchBox
import com.metrolist.music.ui.component.LibrarySortRow
import com.metrolist.music.ui.component.SongMenuIconButton
import com.metrolist.music.ui.component.LocalMenuState
import com.metrolist.music.ui.component.SelectionIndicator
import com.metrolist.music.ui.component.SongListItem
import com.metrolist.music.ui.component.TopScreenGradientBackground
import com.metrolist.music.ui.component.rememberGenreFilter
import com.metrolist.music.ui.menu.AutoPlaylistMenu
import com.metrolist.music.ui.menu.SelectionSongMenu
import com.metrolist.music.ui.menu.SongMenu
import com.metrolist.music.ui.theme.strokeCard
import com.metrolist.music.ui.utils.IrideMotion
import com.metrolist.music.ui.utils.backToMain
import com.metrolist.music.ui.utils.irideEnterScale
import com.metrolist.music.ui.utils.isScrollingUp
import com.metrolist.music.ui.utils.prefetchThumbnails
import com.metrolist.music.ui.utils.rememberEnterProgress
import com.metrolist.music.utils.makeTimeString
import com.metrolist.music.utils.rememberEnumPreference
import com.metrolist.music.utils.rememberPreference
import com.metrolist.music.viewmodels.AutoPlaylistViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun AutoPlaylistScreen(
    navController: NavController,
    viewModel: AutoPlaylistViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val menuState = LocalMenuState.current
    val listenTogetherManager = LocalListenTogetherManager.current
    val isListenTogetherGuest = listenTogetherManager?.isInRoom == true && !listenTogetherManager.isHost
    val uploadUnsupportedFormatStr = stringResource(R.string.upload_unsupported_format)
    val uploadFileTooLargeStr = stringResource(R.string.upload_file_too_large)
    val uploadFailedStr = stringResource(R.string.upload_failed)
    val uploadCompleteStr = stringResource(R.string.upload_complete)
    val focusManager = LocalFocusManager.current
    val playerConnection = LocalPlayerConnection.current ?: return
    val isPlaying by playerConnection.isEffectivelyPlaying.collectAsState()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()
    val queueTitle by playerConnection.queueTitle.collectAsState()
    val playlist =
        when (viewModel.playlist) {
            "liked" -> stringResource(R.string.starred)
            "uploaded" -> stringResource(R.string.uploaded_playlist)
            "starred" -> stringResource(R.string.starred)
            else -> stringResource(R.string.offline)
        }

    val songs by viewModel.likedSongs.collectAsState(null)
    val albumTopGradientEnabled by rememberPreference(AlbumTopGradientKey, defaultValue = true)
    val playerBackgroundStyle by
        rememberEnumPreference(
            PlayerBackgroundStyleKey,
            defaultValue = PlayerBackgroundStyle.BETTER_ANIMATED_GRADIENT,
        )
    val playlistGradientMediaMetadata =
        remember(viewModel.playlist, songs?.firstOrNull()?.song?.thumbnailUrl) {
            songs?.firstOrNull()?.let {
                MediaMetadata(
                    id = viewModel.playlist,
                    title = it.song.title,
                    artists = emptyList(),
                    duration = 0,
                    thumbnailUrl = it.song.thumbnailUrl,
                )
            }
        }

    var isSearching by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf(TextFieldValue()) }
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    val hideExplicit by rememberPreference(key = HideExplicitKey, defaultValue = false)

    val (ytmSync) = rememberPreference(YtmSyncKey, true)

    val likeLength =
        remember(songs) {
            songs?.fastSumBy { it.song.duration } ?: 0
        }

    val playlistId = viewModel.playlist
    val playlistType =
        when (playlistId) {
            "liked" -> PlaylistType.LIKE
            "downloaded" -> PlaylistType.DOWNLOAD
            "uploaded" -> PlaylistType.UPLOADED
            "starred" -> PlaylistType.STARRED
            else -> PlaylistType.OTHER
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

    if (isSearching) {
        BackHandler {
            isSearching = false
            query = TextFieldValue()
            focusManager.clearFocus()
            keyboardController?.hide()
        }
    } else if (inSelectMode) {
        BackHandler(onBack = onExitSelectionMode)
    }

    val (sortType, onSortTypeChange) =
        rememberEnumPreference(
            AutoPlaylistSongSortTypeKey,
            AutoPlaylistSongSortType.CREATE_DATE,
        )
    val (sortDescending, onSortDescendingChange) =
        rememberPreference(AutoPlaylistSongSortDescendingKey, true)

    val downloadUtil = LocalDownloadUtil.current
    var downloadState by remember {
        mutableIntStateOf(Download.STATE_STOPPED)
    }

    val scope = rememberCoroutineScope()

    var showUploadDialog by remember { mutableStateOf(false) }
    var uploadProgress by remember { mutableFloatStateOf(0f) }
    var currentUploadIndex by remember { mutableIntStateOf(0) }
    var totalUploads by remember { mutableIntStateOf(0) }
    var currentFileName by remember { mutableStateOf("") }
    var isUploading by remember { mutableStateOf(false) }
    var uploadJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }

    val filePickerLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.OpenMultipleDocuments()
        ) { uris: List<Uri> ->
            if (uris.isNotEmpty()) {
                uploadJob = scope.launch {
                    isUploading = true
                    showUploadDialog = true
                    totalUploads = uris.size
                    var successCount = 0

                    uris.forEachIndexed { index, uri ->
                        currentUploadIndex = index + 1
                        uploadProgress = 0f

                        try {
                            val fileName = uri.lastPathSegment?.substringAfterLast('/') ?: "unknown"
                            currentFileName = fileName
                            val extension = fileName.substringAfterLast('.', "").lowercase()

                            if (extension !in YouTube.SUPPORTED_UPLOAD_TYPES) {
                                withContext(Dispatchers.Main) {
                                    Toast.makeText(
                                            context,
                                            uploadUnsupportedFormatStr,
                                            Toast.LENGTH_SHORT,
                                        )
                                        .show()
                                }
                                return@forEachIndexed
                            }

                            val inputStream = context.contentResolver.openInputStream(uri)
                            val data = inputStream?.readBytes()
                            inputStream?.close()

                            if (data == null) return@forEachIndexed

                            if (data.size > YouTube.MAX_UPLOAD_SIZE) {
                                withContext(Dispatchers.Main) {
                                    Toast.makeText(
                                            context,
                                            uploadFileTooLargeStr,
                                            Toast.LENGTH_SHORT,
                                        )
                                        .show()
                                }
                                return@forEachIndexed
                            }

                            val result =
                                YouTube.uploadSong(
                                    filename = fileName,
                                    data = data,
                                    onProgress = { progress ->
                                        uploadProgress = progress
                                    },
                                )

                            if (result.isSuccess && result.getOrDefault(false)) {
                                successCount++
                            }
                        } catch (e: Exception) {
                            withContext(Dispatchers.Main) {
                                Toast.makeText(
                                        context,
                                        uploadFailedStr + ": ${e.message}",
                                        Toast.LENGTH_SHORT,
                                    )
                                    .show()
                            }
                        }
                    }

                    isUploading = false

                    if (successCount > 0) {
                        uploadProgress = 1f
                        currentFileName = uploadCompleteStr
                        kotlinx.coroutines.delay(1000)

                        withContext(Dispatchers.Main) {
                            Toast.makeText(
                                    context,
                                    uploadCompleteStr,
                                    Toast.LENGTH_SHORT,
                                )
                                .show()
                        }

                        showUploadDialog = false

                        viewModel.syncUploadedSongs()
                    } else {
                        showUploadDialog = false
                    }
                }
            }
        }

    LaunchedEffect(Unit) {
        if (ytmSync) {
            viewModel.refresh()
        }
    }

    LaunchedEffect(songs) {
        if (songs?.isEmpty() == true) return@LaunchedEffect
        downloadUtil.downloads.collect { downloads ->
            downloadState =
                if (
                    songs?.all { downloads[it.song.id]?.state == Download.STATE_COMPLETED } == true
                ) {
                    Download.STATE_COMPLETED
                } else if (
                    songs?.all {
                        downloads[it.song.id]?.state == Download.STATE_QUEUED ||
                            downloads[it.song.id]?.state == Download.STATE_DOWNLOADING ||
                            downloads[it.song.id]?.state == Download.STATE_COMPLETED
                    } == true
                ) {
                    Download.STATE_DOWNLOADING
                } else {
                    Download.STATE_STOPPED
                }
        }
    }

    var showRemoveDownloadDialog by remember {
        mutableStateOf(false)
    }

    if (showRemoveDownloadDialog) {
        DefaultDialog(
            onDismiss = { showRemoveDownloadDialog = false },
            content = {
                Text(
                    text = stringResource(R.string.remove_download_playlist_confirm, playlist),
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
                        songs!!.forEach { song ->
                            DownloadService.sendRemoveDownload(
                                context,
                                ExoDownloadService::class.java,
                                song.song.id,
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

    if (showUploadDialog) {
        DefaultDialog(
            onDismiss = {
                if (isUploading) {
                    uploadJob?.cancel()
                    isUploading = false
                }
                showUploadDialog = false
            },
            icon = {
                Icon(
                    painter = painterResource(R.drawable.upload),
                    contentDescription = null,
                )
            },
            title = { Text(stringResource(R.string.uploading)) },
            buttons = {
                IrideFolioSecondaryButton(
                    onClick = {
                        if (isUploading) {
                            uploadJob?.cancel()
                            isUploading = false
                        }
                        showUploadDialog = false
                    }
                ) {
                    Text(stringResource(R.string.cancel))
                }
            },
        ) {
            Text(
                text = stringResource(R.string.upload_progress, currentUploadIndex, totalUploads),
                style = MaterialTheme.typography.bodyMedium,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = currentFileName,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(16.dp))
            LinearProgressIndicator(
                progress = { uploadProgress },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }

    val genreFilter =
        rememberGenreFilter(
            remember(songs) {
                songs?.map { GenreSongInfo(it.id, it.song.title, it.artists.firstOrNull()?.name) }
                    ?: emptyList()
            },
            cacheKey = "auto_${viewModel.playlist}",
        )

    val filteredSongs =
        remember(songs, query, genreFilter.selectedGenre, genreFilter.genreBySongId) {
            val base =
                if (query.text.isEmpty()) {
                    songs ?: emptyList()
                } else {
                    songs?.filter { song ->
                        song.song.title.contains(query.text, true) ||
                            song.artists.any { it.name.contains(query.text, true) }
                    } ?: emptyList()
                }
            base.filter { genreFilter.matches(it.id) }
        }

    LaunchedEffect(filteredSongs) {
        selection.fastForEachReversed { songId ->
            if (filteredSongs.find { it.id == songId } == null) {
                selection.remove(songId)
            }
        }

        if (
            selectionAnchorSongId != null && filteredSongs.none { it.id == selectionAnchorSongId }
        ) {
            selectionAnchorSongId = filteredSongs.firstOrNull { it.id in selection }?.id
        }
    }

    LaunchedEffect(filteredSongs) {
        prefetchThumbnails(context, filteredSongs.map { it.song.thumbnailUrl })
    }

    val state = rememberLazyListState()

    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val pullRefreshState = rememberPullToRefreshState()
    val canRefresh = playlistType == PlaylistType.LIKE || playlistType == PlaylistType.UPLOADED

    val onTopBarShuffleClick: () -> Unit = {
        playerConnection.playQueue(
            ListQueue(
                sourceRoute = "auto_playlist/$playlistId",
                title = playlist,
                items = songs.orEmpty().shuffled().map { it.toMediaItem() },
            )
        )
    }
    val isThisPlaylistQueueLoaded = queueTitle == playlist
    val isThisPlaylistPlaying = isPlaying && isThisPlaylistQueueLoaded
    val onTopBarPlaylistPlayClick: () -> Unit = {
        if (isThisPlaylistQueueLoaded) {
            playerConnection.togglePlayPause()
        } else {
            playerConnection.playQueue(
                ListQueue(
                    sourceRoute = "auto_playlist/$playlistId",
                    title = playlist,
                    items = songs.orEmpty().map { it.toMediaItem() },
                )
            )
        }
    }
    val onTopBarPlaylistDownloadClick: () -> Unit = {
        when (downloadState) {
            Download.STATE_COMPLETED -> showRemoveDownloadDialog = true
            Download.STATE_DOWNLOADING,
            Download.STATE_QUEUED -> {
                songs?.forEach { song ->
                    DownloadService.sendRemoveDownload(
                        context,
                        ExoDownloadService::class.java,
                        song.song.id,
                        false,
                    )
                }
            }
            else -> {
                songs?.forEach { song ->
                    val downloadRequest =
                        DownloadRequest.Builder(song.song.id, song.song.id.toUri())
                            .setCustomCacheKey(song.song.id)
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
    val controlPanelProgress =
        rememberEnterProgress(play = true, durationMillis = IrideMotion.Medium)

    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier =
                Modifier.fillMaxSize()
                    .then(
                        if (canRefresh) {
                            Modifier.pullToRefresh(
                                state = pullRefreshState,
                                isRefreshing = isRefreshing,
                                onRefresh = viewModel::refresh,
                            )
                        } else {
                            Modifier
                        }
                    )
        ) {

            val topBarNavigationIcon: @Composable () -> Unit = {
                IconButton(
                    onClick = {
                        when {
                            isSearching -> {
                                isSearching = false
                                query = TextFieldValue()
                                focusManager.clearFocus()
                                keyboardController?.hide()
                            }

                            inSelectMode -> {
                                onExitSelectionMode()
                            }

                            else -> {
                                navController.navigateUp()
                            }
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
                    SongMenuIconButton(
                        enabled = selection.isNotEmpty(),
                        onClick = {
                            menuState.show(anchored = true) {
                                SelectionSongMenu(
                                    songSelection = filteredSongs.filter { it.id in selection },
                                    onDismiss = menuState::dismiss,
                                    clearAction = onExitSelectionMode,
                                    isUploadedPlaylist = playlistType == PlaylistType.UPLOADED,
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
            val onDownloadPlaylist: () -> Unit = {
                when (downloadState) {
                    Download.STATE_COMPLETED -> showRemoveDownloadDialog = true
                    Download.STATE_DOWNLOADING, Download.STATE_QUEUED -> {
                        songs?.forEach { song ->
                            DownloadService.sendRemoveDownload(
                                context,
                                ExoDownloadService::class.java,
                                song.song.id,
                                false,
                            )
                        }
                    }
                    else -> {
                        songs?.forEach { song ->
                            val downloadRequest =
                                DownloadRequest.Builder(song.song.id, song.song.id.toUri())
                                    .setCustomCacheKey(song.song.id)
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
            val playlistMenuSections =
                if (isSearching || inSelectMode) emptyList()
                else {
                    listOf(
                        listOfNotNull(
                            if (isListenTogetherGuest) null
                            else IrideFolioMenuAction(
                                label = stringResource(R.string.add_to_queue),
                                icon = R.drawable.queue_music,
                                onClick = {
                                    playerConnection.addToQueue(songs.orEmpty().map { it.toMediaItem() })
                                },
                            ),
                            if (songs.isNullOrEmpty()) null
                            else IrideFolioMenuAction(
                                label = stringResource(R.string.export_playlist),
                                icon = R.drawable.share,
                                onClick = {
                                    menuState.show(anchored = true) {
                                        AutoPlaylistMenu(
                                            downloadState = downloadState,
                                            onQueue = {},
                                            onDownload = {},
                                            onDismiss = menuState::dismiss,
                                            songs = songs.orEmpty(),
                                            playlistName = playlist,
                                            exportOnly = true,
                                        )
                                    }
                                },
                            ),
                        ),
                        emptyList(),
                    )
                }
            IrideCollectionPageLayout(
                mediaMetadata = playlistGradientMediaMetadata,
                playerBackground = playerBackgroundStyle,
                gradientEnabled = albumTopGradientEnabled,
                title = playlist,
                state = state,
                showTopBar = true,
                showPageTitle = false,
                bottomPadding =
                    LocalPlayerAwareWindowInsets.current.asPaddingValues().calculateBottomPadding(),
                navigationIcon = {
                    topBarNavigationIcon()
                },
                actions = {
                    topBarActions()
                },
                menuSections = playlistMenuSections,
                topBarContent =
                    if (inSelectMode)
                        ({
                            Text(
                                when {
                                    inSelectMode ->
                                        pluralStringResource(
                                            R.plurals.n_song,
                                            selection.size,
                                            selection.size,
                                        )
                                    else -> ""
                                }
                            )
                        })
                    else null,
            ) {
                if (songs != null) {
                    if (songs!!.isEmpty()) {
                        item(key = "empty_playlist_title") {
                            AutoPlaylistHeader(
                                name = playlist,
                                songs = songs.orEmpty(),
                                likeLength = likeLength,
                                playlistType = playlistType,
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
                                onClear = { query = TextFieldValue("") },
                                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                            )
                        }
                        item(key = "empty_placeholder") {
                            EmptyPlaceholder(
                                icon = R.drawable.music_note,
                                text = stringResource(R.string.playlist_is_empty),
                            )
                        }
                    } else {
                        if (!isSearching) {
                            item(key = "playlist_header") {
                                AutoPlaylistHeader(
                                    name = playlist,
                                    songs = songs!!,
                                    likeLength = likeLength,
                                    playlistType = playlistType,
                                )
                            }
                        }

                        if (!isSearching) {
                            item(key = "control_panel") {
                                IridePlaylistControlPanel(
                                    onShuffleClick = onTopBarShuffleClick,
                                    onPlayClick = onTopBarPlaylistPlayClick,
                                    onDownloadClick = onTopBarPlaylistDownloadClick,
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
                                onClear = { query = TextFieldValue("") },
                                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                            )
                        }

                        item(key = "songs_header") {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier =
                                    Modifier.padding(
                                        start = 12.dp,
                                        end = 12.dp,
                                    ),
                            ) {
                                LibrarySortRow(
                                    sortOptions =
                                        listOf(
                                            AutoPlaylistSongSortType.CREATE_DATE to
                                                stringResource(R.string.sort_by_create_date),
                                            AutoPlaylistSongSortType.NAME to
                                                stringResource(R.string.sort_by_name),
                                            AutoPlaylistSongSortType.ARTIST to
                                                stringResource(R.string.sort_by_artist),
                                            AutoPlaylistSongSortType.PLAY_TIME to
                                                stringResource(R.string.sort_by_play_time),
                                        ),
                                    currentSort = sortType,
                                    onSortChange = onSortTypeChange,
                                    sortDescending = sortDescending,
                                    onSortDescendingChange = onSortDescendingChange,
                                    availableViewTypes =
                                        listOf(LibraryViewType.GRID, LibraryViewType.GRID_WIDE),
                                    modifier = Modifier.weight(1f),
                                )
                            }
                        }

                        item(key = "genre_pills") {
                            GenrePillsRow(state = genreFilter)
                        }
                    }

                    if (filteredSongs.isNotEmpty()) {
                        itemsIndexed(
                            items = filteredSongs,
                            key = { _, song -> song.id },
                        ) { index, song ->
                            val onCheckedChange: (Boolean) -> Unit = {
                                if (it) {
                                    selection.add(song.id)
                                } else {
                                    selection.remove(song.id)
                                }
                            }

                            SongListItem(
                                song = song,
                                subtitleColor = Color.Unspecified,
                                isActive = song.song.id == mediaMetadata?.id,
                                isPlaying = isPlaying,
                                trailingContent = {
                                    if (inSelectMode) {
                                        SelectionIndicator(
                                            selected = song.id in selection,
                                            onClick = { onCheckedChange(song.id !in selection) },
                                        )
                                    } else {
                                        SongMenuIconButton(
                                            onClick = {
                                                menuState.show(anchored = true) {
                                                    SongMenu(
                                                        originalSong = song,
                                                        navController = navController,
                                                        onDismiss = menuState::dismiss,
                                                    )
                                                }
                                            }
                                        ) {
                                            Icon(
                                                painter = painterResource(R.drawable.more_vert),
                                                contentDescription = stringResource(R.string.menu),
                                            )
                                        }
                                    }
                                },
                                modifier =
                                    Modifier.fillMaxWidth()
                                        .irideFolioCombinedClickable(
                                            onClick = {
                                                if (inSelectMode) {
                                                    onCheckedChange(song.id !in selection)
                                                } else if (song.song.id == mediaMetadata?.id) {
                                                    playerConnection.togglePlayPause()
                                                } else {
                                                    playerConnection.playQueue(
                                                        ListQueue(
                                                            sourceRoute = "auto_playlist/$playlistId",
                                                            title = playlist,
                                                            items =
                                                                songs!!.map { it.toMediaItem() },
                                                            startIndex =
                                                                songs!!.indexOfFirst {
                                                                    it.id == song.id
                                                                },
                                                        )
                                                    )
                                                }
                                            },
                                            onLongClick = {
                                                if (inSelectMode) {
                                                    val anchorIndex =
                                                        selectionAnchorSongId?.let { anchorSongId ->
                                                            filteredSongs.indexOfFirst {
                                                                it.id == anchorSongId
                                                            }
                                                        } ?: -1

                                                    if (anchorIndex == -1) {
                                                        onCheckedChange(true)
                                                        selectionAnchorSongId = song.id
                                                    } else {
                                                        val range =
                                                            if (anchorIndex <= index)
                                                                anchorIndex..index
                                                            else index..anchorIndex
                                                        for (rangeIndex in range) {
                                                            val rangeSongId =
                                                                filteredSongs[rangeIndex].id
                                                            if (rangeSongId !in selection) {
                                                                selection.add(rangeSongId)
                                                            }
                                                        }
                                                    }
                                                } else {
                                                    menuState.show(anchored = true) {
                                                        SongMenu(
                                                            originalSong = song,
                                                            navController = navController,
                                                            onDismiss = menuState::dismiss,
                                                        )
                                                    }
                                                }
                                            },
                                        )
                                        .animateItem(),
                            )
                        }
                    }
                } else {
                    item(key = "search_bar") {
                        IrideFolioCollectionSearchBox(
                            query = query,
                            onQueryChange = { query = it },
                            placeholderText = stringResource(R.string.search_playlists),
                            focusRequester = focusRequester,
                            onFocusChanged = { if (it.isFocused) isSearching = true },
                            onSearch = {},
                            onClear = { query = TextFieldValue("") },
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                        )
                    }
                }
            }

            if (canRefresh && playlistType != PlaylistType.LIKE) {
                Indicator(
                    isRefreshing = isRefreshing,
                    state = pullRefreshState,
                    modifier =
                        Modifier.align(Alignment.TopCenter)
                            .padding(LocalPlayerAwareWindowInsets.current.asPaddingValues()),
                )
            }

            if (playlistType == PlaylistType.UPLOADED) {
                androidx.compose.animation.AnimatedVisibility(
                    visible = state.isScrollingUp(),
                    enter = androidx.compose.animation.slideInVertically { it },
                    exit = androidx.compose.animation.slideOutVertically { it },
                    modifier =
                        Modifier.align(Alignment.BottomEnd)
                            .windowInsetsPadding(
                                LocalPlayerAwareWindowInsets.current.only(
                                    WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal
                                )
                            )
                            .padding(16.dp),
                ) {
                    FloatingActionButton(
                        onClick = {
                            filePickerLauncher.launch(
                                arrayOf(
                                    "audio/mpeg",
                                    "audio/mp4",
                                    "audio/x-m4a",
                                    "audio/flac",
                                    "audio/ogg",
                                    "audio/x-ms-wma",
                                )
                            )
                        }
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.upload),
                            contentDescription = stringResource(R.string.upload_songs),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AutoPlaylistHeader(
    name: String,
    songs: List<Song>,
    likeLength: Int,
    playlistType: PlaylistType,
    modifier: Modifier = Modifier,
) {
    val metadataLine = buildString {
        append(pluralStringResource(R.plurals.n_song, songs.size, songs.size))
        if (likeLength > 0) {
            append(" \u2022 ")
            append(makeTimeString(likeLength * 1000L))
        }
    }
    IrideGeneratedPlaylistHeader(
        title = name,
        thumbnails = remember(songs) { songs.mapNotNull { it.song.thumbnailUrl } },
        metadataLine = metadataLine,
        badgeIcon = when (playlistType) {
            PlaylistType.LIKE -> R.drawable.star
            PlaylistType.STARRED -> R.drawable.bookmark_filled
            PlaylistType.DOWNLOAD -> R.drawable.offline
            PlaylistType.UPLOADED -> R.drawable.backup
            else -> null
        },
        modifier = modifier,
    )
}

enum class PlaylistType {
    LIKE,
    DOWNLOAD,
    UPLOADED,
    STARRED,
    OTHER,
}
