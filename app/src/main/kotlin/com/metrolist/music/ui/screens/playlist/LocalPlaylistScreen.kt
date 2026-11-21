/** Metrolist Project (C) 2026 Licensed under GPL-3.0 | See git history for contributors */
package com.metrolist.music.ui.screens.playlist

import com.metrolist.music.ui.component.PlaylistArtwork
import com.metrolist.music.ui.component.SongMenuIconButton

import com.metrolist.music.ui.component.IrideFolioPrimaryButton
import com.metrolist.music.ui.component.IrideFolioSecondaryButton

import android.annotation.SuppressLint
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import com.metrolist.music.ui.component.irideFolioCombinedClickable
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
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastAny
import androidx.compose.ui.util.fastForEachReversed
import androidx.compose.ui.util.fastSumBy
import androidx.core.net.toUri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.DownloadRequest
import androidx.media3.exoplayer.offline.DownloadService
import androidx.navigation.NavController
import com.metrolist.innertube.YouTube
import com.metrolist.music.LocalDatabase
import com.metrolist.music.LocalDownloadUtil
import com.metrolist.music.LocalPlayerAwareWindowInsets
import com.metrolist.music.LocalPlayerConnection
import com.metrolist.music.LocalSyncUtils
import com.metrolist.music.R
import com.metrolist.music.ui.component.gestureNavigationEnabled
import com.metrolist.music.constants.AlbumTopGradientKey
import com.metrolist.music.constants.PlayerBackgroundStyle
import com.metrolist.music.constants.PlayerBackgroundStyleKey
import com.metrolist.music.constants.PlaylistSongSortDescendingKey
import com.metrolist.music.constants.PlaylistSongSortType
import com.metrolist.music.constants.PlaylistSongSortTypeKey
import com.metrolist.music.constants.SwipeToRemoveSongKey
import com.metrolist.music.db.entities.Playlist
import com.metrolist.music.db.entities.PlaylistSong
import com.metrolist.music.extensions.move
import com.metrolist.music.extensions.toMediaItem
import com.metrolist.music.models.MediaMetadata
import com.metrolist.music.playback.ExoDownloadService
import com.metrolist.music.playback.queues.ListQueue
import com.metrolist.music.ui.component.CategoryFilterState
import com.metrolist.music.ui.component.CategoryPillsRow
import com.metrolist.music.ui.component.DefaultDialog
import com.metrolist.music.ui.component.CreatePlaylistDialog
import com.metrolist.music.ui.component.GenreSongInfo
import com.metrolist.music.ui.component.IconButton
import com.metrolist.music.ui.component.IrideOutlineIconButton
import com.metrolist.music.ui.component.IrideCollectionTitle
import com.metrolist.music.ui.component.EditableCollectionTitle
import com.metrolist.music.ui.component.IrideCollectionPageLayout
import com.metrolist.music.ui.component.LocalCollectionExpansionProgress
import com.metrolist.music.ui.component.IridePlaylistControlPanel
import com.metrolist.music.ui.component.PlaylistPageCapabilities
import com.metrolist.music.ui.component.playlistPageSections
import com.metrolist.music.ui.component.IridePressEffect
import com.metrolist.music.ui.component.IrideFolioCollectionSearchBox
import com.metrolist.music.ui.component.LocalMenuState
import com.metrolist.music.ui.component.SelectionIndicator
import com.metrolist.music.ui.component.SongListItem
import com.metrolist.music.ui.component.SongRowReorderButton
import com.metrolist.music.ui.theme.FolioShape
import com.metrolist.music.ui.component.rememberGenreFilter
import com.metrolist.music.ui.menu.LocalPlaylistMenu
import com.metrolist.music.ui.menu.SelectionSongMenu
import com.metrolist.music.ui.menu.SongMenu
import com.metrolist.music.ui.theme.IBMPlexMonoFontFamily
import com.metrolist.music.ui.utils.IrideMotion
import com.metrolist.music.ui.utils.backToMain
import com.metrolist.music.ui.utils.headerEnter
import com.metrolist.music.ui.utils.irideEnter
import com.metrolist.music.ui.utils.irideEnterScale
import com.metrolist.music.ui.utils.prefetchThumbnails
import com.metrolist.music.ui.utils.rememberEnterProgress
import com.metrolist.music.ui.utils.rememberSectionEnter
import com.metrolist.music.ui.utils.revealMask
import com.metrolist.music.utils.makeTimeString
import com.metrolist.music.utils.rememberEnumPreference
import com.metrolist.music.utils.rememberPreference
import com.metrolist.music.viewmodels.LocalPlaylistViewModel
import java.time.LocalDateTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

@SuppressLint("RememberReturnType")
@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun LocalPlaylistScreen(
    navController: NavController,
    isOffline: Boolean = false,
    viewModel: LocalPlaylistViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val menuState = LocalMenuState.current
    val database = LocalDatabase.current
    val playerConnection = LocalPlayerConnection.current ?: return
    val isPlaying by playerConnection.isEffectivelyPlaying.collectAsState()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()
    val queueTitle by playerConnection.queueTitle.collectAsState()

    val playlist by viewModel.playlist.collectAsState()
    val songs by viewModel.playlistSongs.collectAsState()
    val isThisPlaylistQueueLoaded = playlist != null && queueTitle == playlist?.playlist?.name
    val isThisPlaylistPlaying = isPlaying && isThisPlaylistQueueLoaded
    val mutableSongs = remember { mutableStateListOf<PlaylistSong>() }
    val playlistLength =
        remember(songs) {
            songs.fastSumBy { it.song.song.duration }
        }
    val (sortType, onSortTypeChange) =
        rememberEnumPreference(
            PlaylistSongSortTypeKey,
            PlaylistSongSortType.CUSTOM,
        )
    val (sortDescending, onSortDescendingChange) =
        rememberPreference(
            PlaylistSongSortDescendingKey,
            true,
        )
    var locked by rememberSaveable { mutableStateOf(true) }
    val swipeRemoveEnabled by rememberPreference(SwipeToRemoveSongKey, defaultValue = true)

    var headerRevealed by rememberSaveable { mutableStateOf(false) }
    val revealedSections = remember { mutableSetOf<String>() }
    val playlistTitle = playlist?.playlist?.name
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
    val genrePillsProgress = rememberSectionEnter("playlist_genre_pills", revealedSections)
    val controlPanelProgress = rememberSectionEnter("playlist_control_panel", revealedSections)
    val songsSectionProgress = rememberSectionEnter("playlist_songs", revealedSections)
    val albumTopGradientEnabled by rememberPreference(AlbumTopGradientKey, defaultValue = true)
    val playerBackgroundStyle by
        rememberEnumPreference(
            PlayerBackgroundStyleKey,
            defaultValue = PlayerBackgroundStyle.BETTER_ANIMATED_GRADIENT,
        )
    val playlistGradientMediaMetadata =
        remember(playlist?.playlist?.id, playlist?.thumbnails?.firstOrNull()) {
            playlist?.let {
                MediaMetadata(
                    id = it.playlist.id,
                    title = it.playlist.name,
                    artists = emptyList(),
                    duration = 0,
                    thumbnailUrl = it.thumbnails.firstOrNull(),
                )
            }
        }

    val syncUtils = LocalSyncUtils.current
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(playlist?.playlist?.browseId, syncUtils) {
        if (playlist?.playlist?.browseId != null) {
            syncUtils.tryAutoSync()
        }
    }

    var isSearching by rememberSaveable { mutableStateOf(false) }

    var query by
        rememberSaveable(stateSaver = TextFieldValue.Saver) {
            mutableStateOf(TextFieldValue())
        }

    val filteredSongs =
        remember(songs, query) {
            if (query.text.isEmpty()) {
                songs
            } else {
                songs.filter { song ->
                    song.song.song.title.contains(query.text, ignoreCase = true) ||
                        song.song.artists.fastAny {
                            it.name.contains(query.text, ignoreCase = true)
                        }
                }
            }
        }

    val genreFilter =
        rememberGenreFilter(
            remember(songs) {
                songs.map {
                    GenreSongInfo(
                        it.song.id,
                        it.song.song.title,
                        it.song.artists.firstOrNull()?.name,
                    )
                }
            },
            cacheKey = viewModel.playlistId,
        )

    val categories by viewModel.categories.collectAsState()
    val songCategoryIds by viewModel.songCategoryIds.collectAsState()
    var selectedCategoryId by rememberSaveable { mutableStateOf<String?>(null) }
    val categoryFilter =
        CategoryFilterState(
            categories = categories,
            songCategoryIds = songCategoryIds,
            selectedCategoryId = selectedCategoryId,
            onSelect = { id -> selectedCategoryId = if (selectedCategoryId == id) null else id },
        )

    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    var inSelectMode by rememberSaveable { mutableStateOf(false) }
    val selection =
        rememberSaveable(
            saver =
                listSaver<MutableList<Int>, Int>(
                    save = { it.toList() },
                    restore = { it.toMutableStateList() },
                )
        ) {
            mutableStateListOf()
        }
    var selectionAnchorMapId by rememberSaveable { mutableStateOf<Int?>(null) }
    val onExitSelectionMode = {
        inSelectMode = false
        selection.clear()
        selectionAnchorMapId = null
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
    } else if (!locked) {
        BackHandler { locked = true }
    }

    val downloadUtil = LocalDownloadUtil.current
    var downloadState by remember {
        mutableIntStateOf(Download.STATE_STOPPED)
    }

    val editable: Boolean = playlist?.playlist?.isEditable == true

    LaunchedEffect(songs) {
        selection.fastForEachReversed { mapId ->
            if (songs.find { it.map.id == mapId } == null) {
                selection.remove(Integer.valueOf(mapId))
            }
        }

        if (selectionAnchorMapId != null && songs.none { it.map.id == selectionAnchorMapId }) {
            selectionAnchorMapId = songs.firstOrNull { it.map.id in selection }?.map?.id
        }
    }

    LaunchedEffect(songs) {
        mutableSongs.apply {
            clear()
            addAll(songs)
        }
        if (songs.isEmpty()) return@LaunchedEffect
        downloadUtil.downloads.collect { downloads ->
            downloadState =
                if (songs.all { downloads[it.song.id]?.state == Download.STATE_COMPLETED }) {
                    Download.STATE_COMPLETED
                } else if (
                    songs.all {
                        downloads[it.song.id]?.state == Download.STATE_QUEUED ||
                            downloads[it.song.id]?.state == Download.STATE_DOWNLOADING ||
                            downloads[it.song.id]?.state == Download.STATE_COMPLETED
                    }
                ) {
                    Download.STATE_DOWNLOADING
                } else {
                    Download.STATE_STOPPED
                }
        }
    }

    var showAddMusic by rememberSaveable { mutableStateOf(false) }
    if (showAddMusic) {
        playlist?.playlist?.let { target ->
            CreatePlaylistDialog(
                onDismiss = { showAddMusic = false },
                targetPlaylist = target,
            )
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
                    text =
                        stringResource(
                            R.string.remove_download_playlist_confirm,
                            playlist?.playlist!!.name,
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
                        if (!editable) {
                            database.transaction {
                                playlist?.id?.let { clearPlaylist(it) }
                            }
                        }
                        songs.forEach { song ->
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

    var showDeletePlaylistDialog by remember {
        mutableStateOf(false)
    }
    if (showDeletePlaylistDialog) {
        DefaultDialog(
            onDismiss = { showDeletePlaylistDialog = false },
            content = {
                Text(
                    text =
                        stringResource(
                            R.string.delete_playlist_confirm,
                            playlist?.playlist!!.name,
                        ),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(horizontal = 18.dp),
                )
            },
            buttons = {
                IrideFolioSecondaryButton(
                    onClick = {
                        showDeletePlaylistDialog = false
                    }
                ) {
                    Text(text = stringResource(android.R.string.cancel))
                }
                IrideFolioPrimaryButton(
                    onClick = {
                        showDeletePlaylistDialog = false
                        database.query {
                            playlist?.let { delete(it.playlist) }
                        }
                        viewModel.viewModelScope.launch(Dispatchers.IO) {
                            playlist?.playlist?.browseId?.let { YouTube.deletePlaylist(it) }
                        }
                        navController.popBackStack()
                    }
                ) {
                    Text(text = stringResource(android.R.string.ok))
                }
            },
        )
    }

    val headerItems =
        when {
            playlist?.let { it.songCount == 0 && it.playlist.remoteSongCount == 0 } == true -> 3
            isSearching -> 3
            else -> 5
        }
    val lazyListState = rememberLazyListState()

    var dragInfo by remember {
        mutableStateOf<Pair<Int, Int>?>(null)
    }
    val reorderableState =
        rememberReorderableLazyListState(
            lazyListState = lazyListState,
            scrollThresholdPadding = LocalPlayerAwareWindowInsets.current.asPaddingValues(),
        ) { from, to ->
            if (to.index >= headerItems && from.index >= headerItems) {
                val currentDragInfo = dragInfo
                dragInfo =
                    if (currentDragInfo == null) {
                        (from.index - headerItems) to (to.index - headerItems)
                    } else {
                        currentDragInfo.first to (to.index - headerItems)
                    }

                mutableSongs.move(from.index - headerItems, to.index - headerItems)
            }
        }

    LaunchedEffect(reorderableState.isAnyItemDragging) {
        if (!reorderableState.isAnyItemDragging) {
            dragInfo?.let { (from, to) ->
                database.transaction {
                    move(viewModel.playlistId, from, to)
                }

                if (viewModel.playlist.value?.playlist?.browseId != null) {
                    viewModel.viewModelScope.launch(Dispatchers.IO) {
                        val playlistSongMap = database.playlistSongMaps(viewModel.playlistId, 0)
                        val successorIndex = if (from > to) to else to + 1
                        val successorSetVideoId =
                            playlistSongMap.getOrNull(successorIndex)?.setVideoId

                        playlistSongMap.getOrNull(from)?.setVideoId?.let { setVideoId ->
                            YouTube.moveSongPlaylist(
                                viewModel.playlist.value?.playlist?.browseId!!,
                                setVideoId,
                                successorSetVideoId,
                            )
                        }
                    }
                }

                dragInfo = null
            }
        }
    }

    val showTopBarTitle by remember {
        derivedStateOf {
            lazyListState.firstVisibleItemIndex > 0
        }
    }

    val displayedSongs =
        (if (isSearching) filteredSongs else mutableSongs).filter {
            categoryFilter.matches(it.song.id)
        }

    LaunchedEffect(displayedSongs) {
        prefetchThumbnails(context, displayedSongs.map { it.song.thumbnailUrl })
    }

    val onTopBarShuffleClick: () -> Unit = {
        playlist?.let { current ->
            playerConnection.playQueue(
                ListQueue(
                    sourceRoute = "local_playlist/${current.playlist.id}",
                    title = current.playlist.name,
                    items = songs.shuffled().map { it.song.toMediaItem() },
                )
            )
        }
    }
    val onTopBarPlaylistPlayClick: () -> Unit = {
        if (isThisPlaylistQueueLoaded) {
            playerConnection.togglePlayPause()
        } else {
            playlist?.let { current ->
                playerConnection.playQueue(
                    ListQueue(
                        sourceRoute = "local_playlist/${current.playlist.id}",
                        title = current.playlist.name,
                        items = songs.map { it.song.toMediaItem() },
                    )
                )
            }
        }
    }
    val onTopBarLikeClick: () -> Unit = {
        playlist?.let { current ->
            database.query {
                update(current.playlist.toggleLike())
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxSize().graphicsLayer { alpha = if (collectionExpansion != null) 1f else screenProgress }) {

            val topBarNavigationIcon: @Composable () -> Unit = {
                if (inSelectMode) {
                    IconButton(onClick = onExitSelectionMode) {
                        Icon(
                            painter = painterResource(R.drawable.close),
                            contentDescription = null,
                        )
                    }
                } else if (!locked) {
                    IconButton(onClick = { locked = true }) {
                        Icon(
                            painter = painterResource(R.drawable.close),
                            contentDescription = stringResource(R.string.reorder),
                        )
                    }
                } else {
                    IconButton(
                        onClick = {
                            if (isSearching) {
                                isSearching = false
                                query = TextFieldValue()
                                focusManager.clearFocus()
                                keyboardController?.hide()
                            } else {
                                navController.navigateUp()
                            }
                        },
                        onLongClick = {
                            if (!isSearching) {
                                navController.backToMain()
                            }
                        },
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
                    SongMenuIconButton(
                        enabled = selection.isNotEmpty(),
                        onClick = {
                            menuState.show(anchored = true) {
                                SelectionSongMenu(
                                    songSelection =
                                        displayedSongs
                                            .filter { it.map.id in selection }
                                            .map { it.song },
                                    onDismiss = menuState::dismiss,
                                    clearAction = onExitSelectionMode,
                                    songPosition =
                                        displayedSongs
                                            .filter { it.map.id in selection }
                                            .map { it.map },
                                )
                            }
                        },
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.more_vert),
                            contentDescription = null,
                        )
                    }
                } else if (!isSearching && locked) {
                    playlist?.let { currentPlaylist ->
                        SongMenuIconButton(
                            onClick = {
                                menuState.show(anchored = true) {
                                    LocalPlaylistMenu(
                                        playlist = currentPlaylist,
                                        songs = songs,
                                        context = context,
                                        downloadState = downloadState,
                                        onDelete = { showDeletePlaylistDialog = true },
                                        onReorder =
                                            if (editable) {
                                                {
                                                    locked = false
                                                    onSortTypeChange(PlaylistSongSortType.CUSTOM)
                                                }
                                            } else {
                                                null
                                            },
                                        sortType = sortType,
                                        onSortTypeChange = onSortTypeChange,
                                        sortDescending = sortDescending,
                                        onSortDescendingChange = onSortDescendingChange,
                                        showSortDescending = sortType != PlaylistSongSortType.CUSTOM,
                                        onDownload = {
                                            when (downloadState) {
                                                Download.STATE_COMPLETED ->
                                                    showRemoveDownloadDialog = true
                                                Download.STATE_DOWNLOADING,
                                                Download.STATE_QUEUED -> {
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
                                                            DownloadRequest.Builder(
                                                                    song.song.id,
                                                                    song.song.id.toUri(),
                                                                )
                                                                .setCustomCacheKey(song.song.id)
                                                                .setData(
                                                                    song.song.song.title
                                                                        .toByteArray()
                                                                )
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
                                        onQueue = {
                                            playerConnection.addToQueue(songs.map { it.song.toMediaItem() })
                                        },
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
                title = playlistTitle ?: playlist?.playlist?.name.orEmpty(),
                state = lazyListState,
                showTopBar = true,
                showPageTitle = false,
                bottomPadding =
                    LocalPlayerAwareWindowInsets.current
                        .union(WindowInsets.ime)
                        .asPaddingValues()
                        .calculateBottomPadding(),
                navigationIcon = if (gestureNavigationEnabled() && (!locked || !(inSelectMode || isSearching))) null else topBarNavigationIcon,
                detachedAction = if (
                    !inSelectMode && locked && !isSearching && playlist?.playlist?.isEditable == true
                ) {
                    {
                        IrideOutlineIconButton(
                            onClick = { showAddMusic = true },
                            icon = R.drawable.add_bold,
                            contentDescription = stringResource(R.string.playlist_draft_add_music),
                            size = 40.dp,
                            iconSize = 18.dp,
                        )
                    }
                } else null,
                actions = {
                    if (!inSelectMode && locked && !isSearching) {
                        playlist?.let { current ->
                            IrideOutlineIconButton(
                                onClick = onTopBarLikeClick,
                                icon =
                                    if (current.playlist.bookmarkedAt != null) R.drawable.favorite
                                    else R.drawable.favorite_border,
                                contentDescription =
                                    stringResource(
                                        if (current.playlist.bookmarkedAt != null)
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
                    if (inSelectMode || (!locked && !gestureNavigationEnabled()))
                        ({
                            Text(
                                when {
                                    inSelectMode ->
                                        pluralStringResource(
                                            R.plurals.n_selected,
                                            selection.size,
                                            selection.size,
                                        )
                                    !locked -> stringResource(R.string.reorder)
                                    else -> ""
                                }
                            )
                        })
                    else null,
            ) {
                playlist?.let { playlist ->
                    if (playlist.songCount == 0 && (playlist.playlist.remoteSongCount ?: 0) == 0) {
                        item(key = "empty_playlist_title") {
                            EditableCollectionTitle(
                                title = playlistTitle ?: playlist.playlist.name,
                                onSave = { name ->
                                    val entity = playlist.playlist
                                    database.query { update(entity.copy(name = name, lastUpdateTime = LocalDateTime.now())) }
                                    viewModel.viewModelScope.launch(Dispatchers.IO) {
                                        entity.browseId?.let { YouTube.renamePlaylist(it, name) }
                                    }
                                },
                                modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                            )
                        }

                        if (playlist.playlist.isEditable) {
                            item(key = "empty_playlist_add") {
                                IrideFolioPrimaryButton(
                                    onClick = { showAddMusic = true },
                                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
                                ) {
                                    Icon(painterResource(R.drawable.add), null, Modifier.size(20.dp))
                                    Spacer(Modifier.size(8.dp))
                                    Text(stringResource(R.string.playlist_draft_add_music))
                                }
                            }
                        }
                    } else {
                        playlistPageSections(
                            capabilities = PlaylistPageCapabilities(
                                showHeader = !isSearching,
                                showPlaybackControls = !isSearching,
                                showSearch = true,
                                showFilters = true,
                            ),
                            header = {
                                LocalPlaylistHeader(
                                    playlist = playlist,
                                    songs = songs,
                                    isOffline = isOffline,
                                    onRename = { name ->
                                        val entity = playlist.playlist
                                        database.query { update(entity.copy(name = name, lastUpdateTime = LocalDateTime.now())) }
                                        viewModel.viewModelScope.launch(Dispatchers.IO) {
                                            entity.browseId?.let { YouTube.renamePlaylist(it, name) }
                                        }
                                    },
                                    onShowRemoveDownloadDialog = {
                                        showRemoveDownloadDialog = true
                                    },
                                    onshowDeletePlaylistDialog = {
                                        showDeletePlaylistDialog = true
                                    },
                                    onStartSearch = { isSearching = true },
                                    snackbarHostState = snackbarHostState,
                                    headerRevealed = headerRevealed,
                                    titleTypingMs = titleTypingMs,
                                )
                            },
                            playbackControls = {
                                IridePlaylistControlPanel(
                                    onShuffleClick = onTopBarShuffleClick,
                                    onPlayClick = onTopBarPlaylistPlayClick,
                                    isPlaying = isThisPlaylistPlaying,
                                    modifier =
                                        Modifier.padding(bottom = 12.dp)
                                            .irideEnterScale(controlPanelProgress),
                                )
                            },
                            search = {
                                IrideFolioCollectionSearchBox(
                                    query = query,
                                    onQueryChange = { query = it },
                                    placeholderText = stringResource(R.string.search_playlists),
                                    focusRequester = focusRequester,
                                    onFocusChanged = { if (it.isFocused) isSearching = true },
                                    onSearch = {},
                                    onClear = { query = TextFieldValue() },
                                )
                            },
                            filters = {
                                CategoryPillsRow(
                                    state = categoryFilter,
                                    modifier = Modifier.irideEnter(genrePillsProgress, 6.dp),
                                )
                            },
                            filterKey = "category_pills",
                        )
                    }
                }
                    ?: run {
                        item(key = "search_bar") {
                            IrideFolioCollectionSearchBox(
                                query = query,
                                onQueryChange = { query = it },
                                placeholderText = stringResource(R.string.search_playlists),
                                focusRequester = focusRequester,
                                onFocusChanged = { if (it.isFocused) isSearching = true },
                                onSearch = {},
                                onClear = { query = TextFieldValue() },
                            )
                        }
                    }

                itemsIndexed(
                    items = displayedSongs,
                    key = { _, song -> song.map.id },
                ) { index, song ->
                    ReorderableItem(
                        state = reorderableState,
                        key = song.map.id,
                    ) { isDragging ->
                        val currentItem by rememberUpdatedState(song)

                        fun deleteFromPlaylist() {
                            val browseId = playlist?.playlist?.browseId
                            val setVideoId = currentItem.map.setVideoId
                            val songId = currentItem.map.songId
                            val playlistId = currentItem.map.playlistId

                            database.transaction {
                                move(playlistId, currentItem.map.position, Int.MAX_VALUE)
                                delete(currentItem.map.copy(position = Int.MAX_VALUE))
                            }

                            if (browseId != null) {
                                syncUtils.scheduleRemoveFromPlaylist(
                                    browseId,
                                    songId,
                                    playlistId,
                                ) {
                                    var setVideoId: String? = setVideoId
                                    if (setVideoId == null) {
                                        for (attempt in 0 until 10) {
                                            setVideoId = database.getSetVideoId(songId)?.setVideoId
                                            if (setVideoId != null) break
                                            delay(3_000L)
                                        }
                                    }
                                    setVideoId
                                }
                            }
                        }

                        val dismissBoxState =
                            rememberSwipeToDismissBoxState(
                                positionalThreshold = { totalDistance -> totalDistance }
                            )
                        var processedDismiss by remember { mutableStateOf(false) }
                        LaunchedEffect(dismissBoxState.currentValue) {
                            val dv = dismissBoxState.currentValue
                            if (
                                swipeRemoveEnabled &&
                                    !processedDismiss &&
                                    (dv == SwipeToDismissBoxValue.StartToEnd ||
                                        dv == SwipeToDismissBoxValue.EndToStart)
                            ) {
                                processedDismiss = true
                                deleteFromPlaylist()
                            }
                            if (dv == SwipeToDismissBoxValue.Settled) {
                                processedDismiss = false
                            }
                        }

                        val onCheckedChange: (Boolean) -> Unit = {
                            if (it) {
                                selection.add(song.map.id)
                            } else {
                                selection.remove(Integer.valueOf(song.map.id))
                            }
                        }

                        val content: @Composable () -> Unit = {
                            SongListItem(
                                song = song.song,
                                subtitleColor = Color.Unspecified,
                                isActive = song.song.id == mediaMetadata?.id,
                                isPlaying = isPlaying,
                                trailingContent = {
                                    if (inSelectMode) {
                                        SelectionIndicator(
                                            selected = selection.contains(song.map.id),
                                            onClick = {
                                                onCheckedChange(!selection.contains(song.map.id))
                                            },
                                        )
                                    } else {
                                        val reordering =
                                            sortType == PlaylistSongSortType.CUSTOM &&
                                                !locked &&
                                                !inSelectMode &&
                                                !isSearching &&
                                                editable
                                        SongRowReorderButton(
                                            reordering = reordering,
                                            onMenuClick = {
                                                menuState.show(anchored = true) {
                                                    SongMenu(
                                                        originalSong = song.song,
                                                        playlistSong = song,
                                                        playlistBrowseId =
                                                            playlist?.playlist?.browseId,
                                                        navController = navController,
                                                        onDismiss = menuState::dismiss,
                                                    )
                                                }
                                            },
                                        )
                                    }
                                },
                                modifier =
                                    Modifier.fillMaxWidth()
                                        .irideFolioCombinedClickable(
                                            onClick = {
                                                if (inSelectMode) {
                                                    onCheckedChange(
                                                        !selection.contains(song.map.id)
                                                    )
                                                } else if (song.song.id == mediaMetadata?.id) {
                                                    playerConnection.togglePlayPause()
                                                } else {
                                                    playerConnection.playQueue(
                                                        ListQueue(
                                                            sourceRoute = "local_playlist/${viewModel.playlistId}?offline=$isOffline",
                                                            title = playlist!!.playlist.name,
                                                            items =
                                                                songs.map { it.song.toMediaItem() },
                                                            startIndex =
                                                                songs.indexOfFirst {
                                                                    it.map.id == song.map.id
                                                                },
                                                        )
                                                    )
                                                }
                                            },
                                            onLongClick = {
                                                if (inSelectMode && selection.contains(song.map.id)) {
                                                    if (selection.size == displayedSongs.size) {
                                                        selection.clear()
                                                    } else {
                                                        selection.clear()
                                                        selection.addAll(
                                                            displayedSongs.map { it.map.id }
                                                        )
                                                    }
                                                } else if (inSelectMode) {
                                                    val anchorIndex =
                                                        selectionAnchorMapId?.let { anchorMapId ->
                                                            displayedSongs.indexOfFirst {
                                                                it.map.id == anchorMapId
                                                            }
                                                        } ?: -1

                                                    if (anchorIndex == -1) {
                                                        onCheckedChange(true)
                                                        selectionAnchorMapId = song.map.id
                                                    } else {
                                                        val range =
                                                            if (anchorIndex <= index)
                                                                anchorIndex..index
                                                            else index..anchorIndex
                                                        for (rangeIndex in range) {
                                                            val rangeMapId =
                                                                displayedSongs[rangeIndex].map.id
                                                            if (rangeMapId !in selection) {
                                                                selection.add(rangeMapId)
                                                            }
                                                        }
                                                    }
                                                } else {
                                                    menuState.show(anchored = true) {
                                                        SongMenu(
                                                            originalSong = song.song,
                                                            playlistSong = song,
                                                            playlistBrowseId = playlist?.playlist?.browseId,
                                                            navController = navController,
                                                            onDismiss = menuState::dismiss,
                                                        )
                                                    }
                                                }
                                            },
                                        ),
                            )
                        }

                        val rowMotionModifier =
                            Modifier.animateItem(placementSpec = IrideMotion.PlacementSpec)
                                .irideEnter(songsSectionProgress, 6.dp)

                        if (locked || inSelectMode || !swipeRemoveEnabled) {
                            Box(modifier = rowMotionModifier) {
                                content()
                            }
                        } else {
                            SwipeToDismissBox(
                                state = dismissBoxState,
                                backgroundContent = {},
                                modifier = rowMotionModifier,
                            ) {
                                content()
                            }
                        }
                    }
                }
            }

        }

        val topBarTitle: @Composable () -> Unit = {
            if (inSelectMode) {
                Text(pluralStringResource(R.plurals.n_selected, selection.size, selection.size))
            } else if (!locked) {
                Text(stringResource(R.string.reorder))
            } else if (showTopBarTitle) {
                Text(playlist?.playlist?.name.orEmpty())
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier =
                Modifier.windowInsetsPadding(
                        LocalPlayerAwareWindowInsets.current.union(WindowInsets.ime)
                    )
                    .align(Alignment.BottomCenter),
        )
    }
}

@Composable
fun LocalPlaylistHeader(
    playlist: Playlist,
    songs: List<PlaylistSong>,
    isOffline: Boolean = false,
    onRename: (String) -> Unit,
    onShowRemoveDownloadDialog: () -> Unit,
    onshowDeletePlaylistDialog: () -> Unit,
    onStartSearch: () -> Unit,
    snackbarHostState: SnackbarHostState,
    headerRevealed: Boolean,
    titleTypingMs: Int,
    modifier: Modifier = Modifier,
) {
    val playerConnection = LocalPlayerConnection.current ?: return
    val context = LocalContext.current
    val database = LocalDatabase.current
    val menuState = LocalMenuState.current
    val syncUtils = LocalSyncUtils.current
    val scope = rememberCoroutineScope()
    val editPlaylistCoverStr = stringResource(R.string.edit_playlist_cover)
    val playlistSyncedStr = stringResource(R.string.playlist_synced)

    val playlistLength =
        remember(songs) {
            songs.fastSumBy { it.song.song.duration }
        }

    val downloadUtil = LocalDownloadUtil.current
    var downloadState by remember {
        mutableIntStateOf(Download.STATE_STOPPED)
    }

    val liked = playlist.playlist.bookmarkedAt != null
    val editable: Boolean = playlist.playlist.isEditable

    LaunchedEffect(songs) {
        if (songs.isEmpty()) return@LaunchedEffect
        downloadUtil.downloads.collect { downloads ->
            downloadState =
                if (songs.all { downloads[it.song.id]?.state == Download.STATE_COMPLETED }) {
                    Download.STATE_COMPLETED
                } else if (
                    songs.all {
                        downloads[it.song.id]?.state == Download.STATE_QUEUED ||
                            downloads[it.song.id]?.state == Download.STATE_DOWNLOADING ||
                            downloads[it.song.id]?.state == Download.STATE_COMPLETED
                    }
                ) {
                    Download.STATE_DOWNLOADING
                } else {
                    Download.STATE_STOPPED
                }
        }
    }

    Column(
        modifier = modifier.fillMaxWidth().padding(top = 8.dp, bottom = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        val songCount =
            if (playlist.songCount == 0 && playlist.playlist.remoteSongCount != null) {
                playlist.playlist.remoteSongCount
            } else {
                playlist.songCount
            }
        val metadataLine = buildString {
            append(pluralStringResource(R.plurals.n_song, songCount, songCount))
            if (playlistLength > 0) {
                append(" • ")
                append(makeTimeString(playlistLength * 1000L))
            }
        }

        val onShuffleClick: () -> Unit = {
            playerConnection.playQueue(
                ListQueue(
                    sourceRoute = "local_playlist/${playlist.playlist.id}",
                    title = playlist.playlist.name,
                    items = songs.shuffled().map { it.song.toMediaItem() },
                )
            )
        }
        val onPlaylistPlayClick: () -> Unit = {
            playerConnection.playQueue(
                ListQueue(
                    sourceRoute = "local_playlist/${playlist.playlist.id}",
                    title = playlist.playlist.name,
                    items = songs.map { it.song.toMediaItem() },
                )
            )
        }
        val onPlaylistDownloadClick: () -> Unit = {
            when (downloadState) {
                Download.STATE_COMPLETED -> onShowRemoveDownloadDialog()
                Download.STATE_DOWNLOADING,
                Download.STATE_QUEUED -> {
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
                            DownloadRequest.Builder(song.song.id, song.song.id.toUri())
                                .setCustomCacheKey(song.song.id)
                                .setData(song.song.song.title.toByteArray())
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

        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                PlaylistArtwork(
                    title = playlist.title,
                    thumbnailUrl = playlist.playlist.thumbnailUrl,
                    playlistId = playlist.playlist.browseId ?: playlist.id,
                    modifier = Modifier.size(240.dp),
                    expansionTarget = true,
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            EditableCollectionTitle(
                title = playlist.playlist.name,
                onSave = onRename,
            )

            Spacer(modifier = Modifier.height(8.dp))

            val metadataProgress =
                headerEnter(
                    revealed = headerRevealed,
                    play = true,
                    delayMillis = titleTypingMs + 40,
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
}

@Composable
private fun MetadataChip(
    icon: Int,
    text: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = FolioShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = painterResource(icon),
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
        }
    }
}
