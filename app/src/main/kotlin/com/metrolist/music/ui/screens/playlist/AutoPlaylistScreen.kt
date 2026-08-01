/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.screens.playlist

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import sv.lib.squircleshape.SquircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults.Indicator
import androidx.compose.material3.pulltorefresh.pullToRefresh
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastForEachReversed
import androidx.compose.ui.util.fastSumBy
import androidx.compose.ui.util.lerp
import androidx.core.net.toUri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.DownloadRequest
import androidx.media3.exoplayer.offline.DownloadService
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import com.metrolist.innertube.YouTube
import com.metrolist.music.LocalDownloadUtil
import com.metrolist.music.LocalPlayerAwareWindowInsets
import com.metrolist.music.LocalPlayerConnection
import com.metrolist.music.R
import com.metrolist.music.constants.AlbumTopGradientKey
import com.metrolist.music.constants.AutoPlaylistSongSortDescendingKey
import com.metrolist.music.constants.AutoPlaylistSongSortType
import com.metrolist.music.constants.AutoPlaylistSongSortTypeKey
import com.metrolist.music.constants.HideExplicitKey
import com.metrolist.music.constants.IrideBaseBorderWidth
import com.metrolist.music.constants.PlayerBackgroundStyle
import com.metrolist.music.constants.PlayerBackgroundStyleKey
import com.metrolist.music.constants.TopNavigationBarKey
import com.metrolist.music.constants.YtmSyncKey
import com.metrolist.music.db.entities.Song
import com.metrolist.music.extensions.toMediaItem
import com.metrolist.music.models.MediaMetadata
import com.metrolist.music.playback.ExoDownloadService
import com.metrolist.music.playback.queues.ListQueue
import com.metrolist.music.ui.component.DefaultDialog
import com.metrolist.music.ui.component.DraggableScrollbar
import com.metrolist.music.ui.component.EmptyPlaceholder
import com.metrolist.music.ui.component.GenrePillsRow
import com.metrolist.music.ui.component.GenreSongInfo
import com.metrolist.music.ui.component.GlassPlaylistCover
import com.metrolist.music.ui.component.HideOnScrollFAB
import com.metrolist.music.ui.component.IconButton
import com.metrolist.music.ui.component.IridePlaylistControlPanel
import com.metrolist.music.ui.component.LibrarySortRow
import com.metrolist.music.ui.component.LocalMenuState
import com.metrolist.music.ui.component.frostedTopBarBackground
import com.metrolist.music.ui.component.recordFrostBackdrop
import com.metrolist.music.ui.component.rememberFrostBackdrop
import com.metrolist.music.ui.component.rememberRubberBandPull
import com.metrolist.music.ui.component.rubberBandOverscroll
import com.metrolist.music.ui.component.SongListItem
import com.metrolist.music.ui.component.TopScreenGradientBackground
import com.metrolist.music.ui.component.rememberGenreFilter
import com.metrolist.music.ui.menu.AutoPlaylistMenu
import com.metrolist.music.ui.menu.SelectionSongMenu
import com.metrolist.music.ui.menu.SongMenu
import com.metrolist.music.ui.screens.search.IrideSearchBox
import com.metrolist.music.ui.utils.IrideMotion
import com.metrolist.music.ui.utils.backToMain
import com.metrolist.music.ui.utils.irideEnter
import com.metrolist.music.ui.utils.irideEnterScale
import com.metrolist.music.ui.utils.isScrollingUp
import com.metrolist.music.ui.utils.prefetchThumbnails
import com.metrolist.music.ui.utils.rememberEnterProgress
import com.metrolist.music.ui.utils.revealMask
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
    val haptic = LocalHapticFeedback.current
    val uploadUnsupportedFormatStr = stringResource(R.string.upload_unsupported_format)
    val uploadFileTooLargeStr = stringResource(R.string.upload_file_too_large)
    val uploadFailedStr = stringResource(R.string.upload_failed)
    val uploadCompleteStr = stringResource(R.string.upload_complete)
    val focusManager = LocalFocusManager.current
    val playerConnection = LocalPlayerConnection.current ?: return
    val isPlaying by playerConnection.isEffectivelyPlaying.collectAsState()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()
    val queueTitle by playerConnection.queueTitle.collectAsState()
    val topNavigationBarEnabled by rememberPreference(TopNavigationBarKey, defaultValue = true)
    val playlist =
        when (viewModel.playlist) {
            // New Iride UI only: "Liked Songs" reads as "Starred" here. R.string.liked is shared
            // with the legacy UI (and other screens), so it is left untouched and only the display
            // text used by this New-Iride-UI-gated composable is swapped.
            "liked" -> if (topNavigationBarEnabled) stringResource(R.string.starred) else stringResource(R.string.liked)
            "uploaded" -> stringResource(R.string.uploaded_playlist)
            "starred" -> stringResource(R.string.starred)
            else -> stringResource(R.string.offline)
        }

    val songs by viewModel.likedSongs.collectAsState(null)
    val albumTopGradientEnabled by rememberPreference(AlbumTopGradientKey, defaultValue = true)
    val playerBackgroundStyle by rememberEnumPreference(
        PlayerBackgroundStyleKey,
        defaultValue = PlayerBackgroundStyle.BETTER_ANIMATED_GRADIENT,
    )
    val playlistGradientMediaMetadata = remember(viewModel.playlist, songs?.firstOrNull()?.song?.thumbnailUrl) {
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

    val density = LocalDensity.current
    var nameBottomPx by remember { mutableStateOf(Float.MAX_VALUE) }
    var topBarBottomPx by remember { mutableStateOf(0f) }
    val titleCoverRangePx = with(density) { 24.dp.toPx() }
    val headerPull = rememberRubberBandPull()

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
                ),
        ) { mutableStateListOf() }
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
    val (sortDescending, onSortDescendingChange) = rememberPreference(AutoPlaylistSongSortDescendingKey, true)

    val downloadUtil = LocalDownloadUtil.current
    var downloadState by remember {
        mutableIntStateOf(Download.STATE_STOPPED)
    }

    val scope = rememberCoroutineScope()

    // Upload state
    var showUploadDialog by remember { mutableStateOf(false) }
    var uploadProgress by remember { mutableFloatStateOf(0f) }
    var currentUploadIndex by remember { mutableIntStateOf(0) }
    var totalUploads by remember { mutableIntStateOf(0) }
    var currentFileName by remember { mutableStateOf("") }
    var isUploading by remember { mutableStateOf(false) }
    var uploadJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }

    val filePickerLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.OpenMultipleDocuments(),
        ) { uris: List<Uri> ->
            if (uris.isNotEmpty()) {
                uploadJob =
                    scope.launch {
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
                                        Toast
                                            .makeText(
                                                context,
                                                uploadUnsupportedFormatStr,
                                                Toast.LENGTH_SHORT,
                                            ).show()
                                    }
                                    return@forEachIndexed
                                }

                                val inputStream = context.contentResolver.openInputStream(uri)
                                val data = inputStream?.readBytes()
                                inputStream?.close()

                                if (data == null) return@forEachIndexed

                                if (data.size > YouTube.MAX_UPLOAD_SIZE) {
                                    withContext(Dispatchers.Main) {
                                        Toast
                                            .makeText(
                                                context,
                                                uploadFileTooLargeStr,
                                                Toast.LENGTH_SHORT,
                                            ).show()
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
                                    Toast
                                        .makeText(
                                            context,
                                            uploadFailedStr + ": ${e.message}",
                                            Toast.LENGTH_SHORT,
                                        ).show()
                                }
                            }
                        }

                        isUploading = false

                        if (successCount > 0) {
                            // Show completion briefly
                            uploadProgress = 1f
                            currentFileName = uploadCompleteStr
                            kotlinx.coroutines.delay(1000)

                            // Show toast on main thread
                            withContext(Dispatchers.Main) {
                                Toast
                                    .makeText(
                                        context,
                                        uploadCompleteStr,
                                        Toast.LENGTH_SHORT,
                                    ).show()
                            }

                            showUploadDialog = false

                            // Refresh uploaded songs
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
                if (songs?.all { downloads[it.song.id]?.state == Download.STATE_COMPLETED } == true) {
                    Download.STATE_COMPLETED
                } else if (songs?.all {
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
                TextButton(
                    onClick = { showRemoveDownloadDialog = false },
                ) {
                    Text(text = stringResource(android.R.string.cancel))
                }

                TextButton(
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
                    },
                ) {
                    Text(text = stringResource(android.R.string.ok))
                }
            },
        )
    }

    // Upload progress dialog
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
                TextButton(
                    onClick = {
                        if (isUploading) {
                            uploadJob?.cancel()
                            isUploading = false
                        }
                        showUploadDialog = false
                    },
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
                songs?.map { GenreSongInfo(it.id, it.song.title, it.artists.firstOrNull()?.name) } ?: emptyList()
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

        if (selectionAnchorSongId != null && filteredSongs.none { it.id == selectionAnchorSongId }) {
            selectionAnchorSongId = filteredSongs.firstOrNull { it.id in selection }?.id
        }
    }

    LaunchedEffect(filteredSongs) {
        prefetchThumbnails(context, filteredSongs.map { it.song.thumbnailUrl })
    }

    val state = rememberLazyListState()
    // Same crossing math as AlbumScreen/LocalPlaylistScreen: a continuous pixel-accurate ramp as
    // the header name goes behind the bar, instead of a flat item-index threshold. No typing/
    // reveal gate here — this header's name has no entrance animation to wait on.
    val topBarRevealProgress by remember {
        derivedStateOf {
            if (state.firstVisibleItemIndex > 1) {
                1f
            } else {
                ((topBarBottomPx + titleCoverRangePx - nameBottomPx) / titleCoverRangePx).coerceIn(0f, 1f)
            }
        }
    }

    // Every leading LazyColumn item ahead of the song rows themselves — search_bar (New Iride UI
    // only) + playlist_header + control_panel (both hidden while searching) + songs_header +
    // genre_pills. Consumed by DraggableScrollbar below.
    val headerItems = when {
        topNavigationBarEnabled && !isSearching -> 5
        topNavigationBarEnabled -> 3
        !isSearching -> 3
        else -> 2
    }

    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val pullRefreshState = rememberPullToRefreshState()
    val canRefresh = playlistType == PlaylistType.LIKE || playlistType == PlaylistType.UPLOADED
    val frostBackdrop = rememberFrostBackdrop()

    // Top-bar mirrors of the header's shuffle/play/download actions (New Iride UI only) — also
    // reused by the control panel just above the song list, so declared before the LazyColumn
    // that renders it.
    val onTopBarShuffleClick: () -> Unit = {
        playerConnection.playQueue(
            ListQueue(
                title = playlist,
                items = songs.orEmpty().shuffled().map { it.toMediaItem() },
            ),
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
                    title = playlist,
                    items = songs.orEmpty().map { it.toMediaItem() },
                ),
            )
        }
    }
    val onTopBarPlaylistDownloadClick: () -> Unit = {
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
                        DownloadRequest
                            .Builder(song.song.id, song.song.id.toUri())
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
    val controlPanelProgress = rememberEnterProgress(play = true, durationMillis = IrideMotion.Medium)

    // Two boxes, not one, exactly like AlbumScreen: the frosted top bar *samples* the backdrop
    // layer, so it must not be drawn inside the Box that records it. Nested, the bar's drawBehind
    // re-enters frostBackdrop.content (drawLayer of a RenderNode that is still mid-record) and the
    // platform throws — which is why the playlist screens crashed the moment the bar's glass turned
    // on (progress > 0) while the album screen never did.
    Box(modifier = Modifier.fillMaxSize()) {
    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .recordFrostBackdrop(frostBackdrop)
                .then(
                    if (canRefresh) {
                        Modifier.pullToRefresh(
                            state = pullRefreshState,
                            isRefreshing = isRefreshing,
                            onRefresh = viewModel::refresh,
                        )
                    } else {
                        Modifier
                    },
                ),
    ) {
        if (albumTopGradientEnabled) {
            TopScreenGradientBackground(
                mediaMetadata = playlistGradientMediaMetadata,
                playerBackground = playerBackgroundStyle,
            )
        }

        LazyColumn(
            modifier = Modifier
                .then(
                    if (topNavigationBarEnabled) {
                        Modifier.rubberBandOverscroll(Orientation.Vertical, state, headerPull)
                    } else {
                        Modifier
                    },
                ),
            state = state,
            contentPadding = LocalPlayerAwareWindowInsets.current.asPaddingValues(),
        ) {
            if (topNavigationBarEnabled) {
                item(key = "search_bar") {
                    IrideSearchBox(
                        query = query,
                        onQueryChange = { query = it },
                        placeholderText = stringResource(R.string.search),
                        focusRequester = focusRequester,
                        onFocusChanged = { if (it.isFocused) isSearching = true },
                        onSearch = {},
                        onClear = { query = TextFieldValue("") },
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                    )
                }
            }

            if (songs != null) {
                if (songs!!.isEmpty()) {
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
                                downloadState = downloadState,
                                playlistType = playlistType,
                                onShowRemoveDownloadDialog = { showRemoveDownloadDialog = true },
                                menuState = menuState,
                                onTitleBoundsChanged = { nameBottomPx = it },
                            )
                        }
                    }

                    if (topNavigationBarEnabled && !isSearching) {
                        item(key = "control_panel") {
                            IridePlaylistControlPanel(
                                onShuffleClick = onTopBarShuffleClick,
                                onPlayClick = onTopBarPlaylistPlayClick,
                                onDownloadClick = onTopBarPlaylistDownloadClick,
                                downloadState = downloadState,
                                isPlaying = isThisPlaylistPlaying,
                                modifier = Modifier
                                    .padding(bottom = 12.dp)
                                    .irideEnterScale(controlPanelProgress),
                            )
                        }
                    }

                    item(key = "songs_header") {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(
                                // Matches SongListItem's own 12dp horizontal inset so the sort
                                // row lines up with the song rows below it.
                                start = if (topNavigationBarEnabled) 12.dp else 8.dp,
                                end = if (topNavigationBarEnabled) 12.dp else 8.dp,
                            ),
                        ) {
                            LibrarySortRow(
                                sortOptions =
                                    listOf(
                                        AutoPlaylistSongSortType.CREATE_DATE to stringResource(R.string.sort_by_create_date),
                                        AutoPlaylistSongSortType.NAME to stringResource(R.string.sort_by_name),
                                        AutoPlaylistSongSortType.ARTIST to stringResource(R.string.sort_by_artist),
                                        AutoPlaylistSongSortType.PLAY_TIME to stringResource(R.string.sort_by_play_time),
                                    ),
                                currentSort = sortType,
                                onSortChange = onSortTypeChange,
                                sortDescending = sortDescending,
                                onSortDescendingChange = onSortDescendingChange,
                                useIrideStyle = topNavigationBarEnabled,
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
                            // New Iride UI: featured-artist subtitle text should match the rest of
                            // the row instead of the default muted secondary tone.
                            subtitleColor = if (topNavigationBarEnabled) Color.Unspecified else null,
                            isActive = song.song.id == mediaMetadata?.id,
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
                                            contentDescription = stringResource(R.string.menu),
                                        )
                                    }
                                }
                            },
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .combinedClickable(
                                        onClick = {
                                            if (inSelectMode) {
                                                onCheckedChange(song.id !in selection)
                                            } else if (song.song.id == mediaMetadata?.id) {
                                                playerConnection.togglePlayPause()
                                            } else {
                                                playerConnection.playQueue(
                                                    ListQueue(
                                                        title = playlist,
                                                        items = songs!!.map { it.toMediaItem() },
                                                        startIndex = songs!!.indexOfFirst { it.id == song.id },
                                                    ),
                                                )
                                            }
                                        },
                                        onLongClick = {
                                            if (!inSelectMode) {
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                inSelectMode = true
                                                onCheckedChange(true)
                                                selectionAnchorSongId = song.id
                                            } else {
                                                val anchorIndex =
                                                    selectionAnchorSongId?.let { anchorSongId ->
                                                        filteredSongs.indexOfFirst { it.id == anchorSongId }
                                                    } ?: -1

                                                if (anchorIndex == -1) {
                                                    onCheckedChange(true)
                                                    selectionAnchorSongId = song.id
                                                } else {
                                                    val range = if (anchorIndex <= index) anchorIndex..index else index..anchorIndex
                                                    for (rangeIndex in range) {
                                                        val rangeSongId = filteredSongs[rangeIndex].id
                                                        if (rangeSongId !in selection) {
                                                            selection.add(rangeSongId)
                                                        }
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

        DraggableScrollbar(
            modifier =
                Modifier
                    .padding(
                        LocalPlayerAwareWindowInsets.current
                            .union(WindowInsets.ime)
                            .asPaddingValues(),
                    ).align(Alignment.CenterEnd),
            scrollState = state,
            headerItems = headerItems,
        )

        if (canRefresh && playlistType != PlaylistType.LIKE) {
            Indicator(
                isRefreshing = isRefreshing,
                state = pullRefreshState,
                modifier =
                    Modifier
                        .align(Alignment.TopCenter)
                        .padding(LocalPlayerAwareWindowInsets.current.asPaddingValues()),
            )
        }

        // Upload FAB for uploaded playlist - positioned above mini player
        if (playlistType == PlaylistType.UPLOADED) {
            androidx.compose.animation.AnimatedVisibility(
                visible = state.isScrollingUp(),
                enter = androidx.compose.animation.slideInVertically { it },
                exit = androidx.compose.animation.slideOutVertically { it },
                modifier =
                    Modifier
                        .align(Alignment.BottomEnd)
                        .windowInsetsPadding(
                            LocalPlayerAwareWindowInsets.current
                                .only(WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal),
                        ).padding(16.dp),
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
                            ),
                        )
                    },
                ) {
                    Icon(
                        painter = painterResource(R.drawable.upload),
                        contentDescription = stringResource(R.string.upload_songs),
                    )
                }
            }
        }
        }
        // --- everything below is a sibling of the recorded content, never inside it ---

        val topBarTitle: @Composable () -> Unit = {
            when {
                inSelectMode -> {
                    Text(
                        text = pluralStringResource(R.plurals.n_song, selection.size, selection.size),
                        style = if (topNavigationBarEnabled) {
                            TextStyle(
                                fontFamily = SpaceMonoFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                letterSpacing = (-0.1).sp,
                            )
                        } else {
                            MaterialTheme.typography.titleLarge
                        },
                    )
                }

                isSearching && !topNavigationBarEnabled -> {
                    TextField(
                        value = query,
                        onValueChange = { query = it },
                        placeholder = {
                            Text(
                                text = stringResource(R.string.search),
                                style = MaterialTheme.typography.titleLarge,
                            )
                        },
                        singleLine = true,
                        textStyle = MaterialTheme.typography.titleLarge,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        colors =
                            TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                                disabledIndicatorColor = Color.Transparent,
                            ),
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .focusRequester(focusRequester),
                    )
                }

                else -> {
                    Text(
                        text = playlist,
                        style = if (topNavigationBarEnabled) {
                            TextStyle(
                                fontFamily = SpaceMonoFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                letterSpacing = (-0.1).sp,
                            )
                        } else {
                            MaterialTheme.typography.titleLarge
                        },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }

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
                                selection.addAll(filteredSongs.map { it.id })
                            }
                        },
                    )
                    IconButton(
                        enabled = selection.isNotEmpty(),
                        onClick = {
                            menuState.show {
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
                } else if (!isSearching) {
                    if (!topNavigationBarEnabled) {
                        IconButton(
                            onClick = { isSearching = true },
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.search),
                                contentDescription = null,
                            )
                        }
                    }
                    IconButton(
                        onClick = {
                            menuState.show {
                                AutoPlaylistMenu(
                                    downloadState = downloadState,
                                    onQueue = {
                                        playerConnection.playQueue(
                                            ListQueue(
                                                title = playlist,
                                                items = songs?.map { it.toMediaItem() } ?: emptyList(),
                                            ),
                                        )
                                    },
                                    onDownload = {
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
                                                    val downloadRequest = DownloadRequest
                                                        .Builder(song.song.id, song.song.id.toUri())
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
                                    },
                                    onDismiss = menuState::dismiss,
                                    songs = songs ?: emptyList(),
                                    playlistName = playlist,
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

        if (topNavigationBarEnabled) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .onGloballyPositioned { topBarBottomPx = it.boundsInWindow().bottom }
                    .frostedTopBarBackground(
                        progress = topBarRevealProgress,
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
                // Always composed and always holding its weight — fades in via topBarRevealProgress,
                // tracking the header name going behind this bar (same as AlbumScreen). Only the ⋯
                // overflow lives here otherwise; shuffle/play/download moved into the pill panel.
                Text(
                    text = when {
                        inSelectMode -> pluralStringResource(R.plurals.n_song, selection.size, selection.size)
                        isSearching -> ""
                        else -> playlist
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
                topBarActions()
            }
        } else {
            TopAppBar(
                title = topBarTitle,
                navigationIcon = topBarNavigationIcon,
                actions = topBarActions,
            )
        }
    }
}

@Composable
private fun AutoPlaylistHeader(
    name: String,
    songs: List<Song>,
    likeLength: Int,
    downloadState: Int,
    playlistType: PlaylistType,
    onShowRemoveDownloadDialog: () -> Unit,
    menuState: com.metrolist.music.ui.component.MenuState,
    modifier: Modifier = Modifier,
    onTitleBoundsChanged: (Float) -> Unit = {},
) {
    val playerConnection = LocalPlayerConnection.current ?: return
    val context = LocalContext.current
    val topNavigationBarEnabled by rememberPreference(TopNavigationBarKey, defaultValue = true)

    val metadataLine =
        buildString {
            append(pluralStringResource(R.plurals.n_song, songs.size, songs.size))
            if (likeLength > 0) {
                append(" • ")
                append(makeTimeString(likeLength * 1000L))
            }
        }

    val onShuffleClick: () -> Unit = {
        playerConnection.playQueue(
            ListQueue(
                title = name,
                items = songs.shuffled().map { it.toMediaItem() },
            ),
        )
    }
    val onPlaylistPlayClick: () -> Unit = {
        playerConnection.playQueue(
            ListQueue(
                title = name,
                items = songs.map { it.toMediaItem() },
            ),
        )
    }
    val onPlaylistDownloadClick: () -> Unit = {
        when (downloadState) {
            Download.STATE_COMPLETED -> onShowRemoveDownloadDialog()
            Download.STATE_DOWNLOADING, Download.STATE_QUEUED -> {
                songs.forEach { song ->
                    DownloadService.sendRemoveDownload(
                        context,
                        ExoDownloadService::class.java,
                        song.song.id,
                        false,
                    )
                }
            }
            else -> {
                songs.forEach { song ->
                    val downloadRequest =
                        DownloadRequest
                            .Builder(song.song.id, song.song.id.toUri())
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

    val coverContent: @Composable () -> Unit = {
        val coverSquircle = SquircleShape(radius = 12.dp, cornerSmoothing = 0.45f)
        val badgeIcon = when (playlistType) {
            PlaylistType.LIKE -> R.drawable.star
            PlaylistType.STARRED -> R.drawable.bookmark_filled
            else -> null
        }
        if (badgeIcon != null) {
            if (topNavigationBarEnabled) {
                // Frosted-glass cover: blurred mosaic of the playlist's own thumbnails behind a
                // translucent panel, replacing the old flat gray square + centered gray star
                // (felt anonymous). Shared with the Library list/grid rows via GlassPlaylistCover
                // so the same playlist looks identical on its own screen and from outside.
                val mosaicThumbnails =
                    remember(songs) {
                        songs.mapNotNull { it.song.thumbnailUrl }.distinct().take(4)
                    }
                GlassPlaylistCover(
                    thumbnails = mosaicThumbnails,
                    icon = badgeIcon,
                    size = 240.dp,
                    shape = coverSquircle,
                    iconSizeFraction = 0.65f,
                )
            } else {
                androidx.compose.material3.Surface(
                    modifier =
                        Modifier
                            .size(240.dp)
                            .shadow(
                                elevation = 24.dp,
                                shape = coverSquircle,
                                spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                            ),
                    shape = coverSquircle,
                    color = MaterialTheme.colorScheme.surfaceContainer,
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        Icon(
                            painter = painterResource(badgeIcon),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                            modifier = Modifier.size(240.dp * 0.65f),
                        )
                    }
                }
            }
        } else {
            androidx.compose.material3.Surface(
                modifier =
                    Modifier
                        .size(240.dp)
                        .shadow(
                            elevation = 24.dp,
                            shape = coverSquircle,
                            spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                        ),
                shape = coverSquircle,
                color = MaterialTheme.colorScheme.surface,
                border = if (topNavigationBarEnabled) {
                    BorderStroke(IrideBaseBorderWidth, Color.White.copy(alpha = 0.22f))
                } else {
                    null
                },
            ) {
                AsyncImage(
                    model = songs[0].song.thumbnailUrl,
                    contentDescription = null,
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }

    if (topNavigationBarEnabled) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(top = 12.dp, bottom = 20.dp),
        ) {
            // Cover entrance matches AlbumScreen/LocalPlaylistScreen — no per-image decode signal
            // to gate on here (GlassPlaylistCover has no onState hook, and mosaics arrive one
            // thumbnail at a time), so this plays once on composition instead.
            val coverProgress = rememberEnterProgress(play = true, durationMillis = 420, easing = IrideMotion.EaseOutQuart)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer {
                        alpha = coverProgress
                        val s = lerp(0.94f, 1f, coverProgress)
                        scaleX = s
                        scaleY = s
                    },
                contentAlignment = Alignment.Center,
            ) {
                coverContent()
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = name,
                style = TextStyle(
                    fontFamily = SpaceMonoFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    letterSpacing = (-0.2).sp,
                ),
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .fillMaxWidth()
                    .onGloballyPositioned { onTitleBoundsChanged(it.boundsInWindow().bottom) },
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = metadataLine,
                style = TextStyle(fontFamily = SpaceMonoFontFamily, fontSize = 13.sp),
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            // Action buttons (shuffle/play/download) live in the top bar now — see topBarActions.
        }
    } else {
        Column(
            modifier =
                modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Playlist Thumbnail - Large centered with shadow
            Box(
                modifier = Modifier.padding(top = 8.dp, bottom = 20.dp),
            ) {
                coverContent()
            }

            // Playlist Name
            Text(
                text = name,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 32.dp),
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Metadata - Song Count • Duration
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
                // Shuffle Button - Smaller secondary button
                androidx.compose.material3.Surface(
                    onClick = onShuffleClick,
                    shape = androidx.compose.foundation.shape.CircleShape,
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.size(48.dp),
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.shuffle),
                            contentDescription = stringResource(R.string.shuffle),
                            modifier = Modifier.size(24.dp),
                        )
                    }
                }

                // Play Button - Larger primary circular button
                Surface(
                    onClick = onPlaylistPlayClick,
                    color = MaterialTheme.colorScheme.primary,
                    shape = androidx.compose.foundation.shape.CircleShape,
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
                    onClick = onPlaylistDownloadClick,
                    shape = androidx.compose.foundation.shape.CircleShape,
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

enum class PlaylistType {
    LIKE,
    DOWNLOAD,
    UPLOADED,
    STARRED,
    OTHER,
}
