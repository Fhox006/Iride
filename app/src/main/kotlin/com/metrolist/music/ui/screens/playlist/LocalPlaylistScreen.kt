/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.screens.playlist

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import sv.lib.squircleshape.SquircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Checkbox
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
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
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import com.metrolist.music.ui.theme.SpaceMonoFontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastAny
import androidx.compose.ui.util.fastForEachIndexed
import androidx.compose.ui.util.fastForEachReversed
import androidx.compose.ui.util.fastSumBy
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.DownloadRequest
import androidx.media3.exoplayer.offline.DownloadService
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import com.metrolist.innertube.YouTube
import com.metrolist.innertube.models.SongItem
import com.metrolist.innertube.utils.completed
import com.metrolist.music.LocalDatabase
import com.metrolist.music.LocalDownloadUtil
import com.metrolist.music.LocalPlayerAwareWindowInsets
import com.metrolist.music.LocalPlayerConnection
import com.metrolist.music.LocalSyncUtils
import com.metrolist.music.R
import com.metrolist.music.constants.DarkModeKey
import com.metrolist.music.constants.PlaylistEditLockKey
import com.metrolist.music.constants.PlaylistSongSortDescendingKey
import com.metrolist.music.constants.PlaylistSongSortType
import com.metrolist.music.constants.PlaylistSongSortTypeKey
import com.metrolist.music.constants.SwipeToRemoveSongKey
import com.metrolist.music.constants.TopNavigationBarKey
import com.metrolist.music.db.entities.Playlist
import com.metrolist.music.db.entities.PlaylistSong
import com.metrolist.music.db.entities.PlaylistSongMap
import com.metrolist.music.extensions.move
import com.metrolist.music.extensions.toMediaItem
import com.metrolist.music.models.toMediaMetadata
import com.metrolist.music.playback.ExoDownloadService
import com.metrolist.music.playback.queues.ListQueue
import com.metrolist.music.ui.component.ActionPromptDialog
import com.metrolist.music.ui.component.DefaultDialog
import com.metrolist.music.ui.component.DraggableScrollbar
import com.metrolist.music.ui.component.EmptyPlaceholder
import com.metrolist.music.ui.component.GenrePillsRow
import com.metrolist.music.ui.component.GenreSongInfo
import com.metrolist.music.ui.component.IconButton
import com.metrolist.music.ui.component.IrideOutlineIconButton
import com.metrolist.music.ui.component.LibrarySortRow
import com.metrolist.music.ui.component.LocalMenuState
import com.metrolist.music.ui.component.OverlayEditButton
import com.metrolist.music.ui.component.SongListItem
import com.metrolist.music.ui.component.TextFieldDialog
import com.metrolist.music.ui.menu.CustomThumbnailMenu
import com.metrolist.music.ui.menu.LocalPlaylistMenu
import com.metrolist.music.ui.menu.SelectionSongMenu
import com.metrolist.music.ui.menu.SongMenu
import com.metrolist.music.ui.screens.settings.DarkMode
import com.metrolist.music.ui.component.rememberGenreFilter
import com.metrolist.music.ui.utils.backToMain
import com.metrolist.music.ui.utils.prefetchThumbnails
import com.metrolist.music.utils.makeTimeString
import com.metrolist.music.utils.rememberEnumPreference
import com.metrolist.music.utils.rememberPreference
import com.metrolist.music.utils.reportException
import com.metrolist.music.viewmodels.LocalPlaylistViewModel
import com.yalantis.ucrop.UCrop
import io.ktor.client.plugins.ClientRequestException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import java.time.LocalDateTime

@SuppressLint("RememberReturnType")
@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun LocalPlaylistScreen(
    navController: NavController,
    viewModel: LocalPlaylistViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val menuState = LocalMenuState.current
    val database = LocalDatabase.current
    val haptic = LocalHapticFeedback.current
    val playerConnection = LocalPlayerConnection.current ?: return
    val isPlaying by playerConnection.isEffectivelyPlaying.collectAsState()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()

    val playlist by viewModel.playlist.collectAsState()
    val songs by viewModel.playlistSongs.collectAsState()
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
    var locked by rememberPreference(PlaylistEditLockKey, defaultValue = true)
    val topNavigationBarEnabled by rememberPreference(TopNavigationBarKey, defaultValue = true)

    val coroutineScope = rememberCoroutineScope()
    val syncUtils = LocalSyncUtils.current
    val snackbarHostState = remember { SnackbarHostState() }

    var isSearching by rememberSaveable { mutableStateOf(false) }

    var query by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue())
    }

    val filteredSongs =
        remember(songs, query) {
            if (query.text.isEmpty()) {
                songs
            } else {
                songs.filter { song ->
                    song.song.song.title
                        .contains(query.text, ignoreCase = true) ||
                        song.song.artists
                            .fastAny { it.name.contains(query.text, ignoreCase = true) }
                }
            }
        }

    val genreFilter =
        rememberGenreFilter(
            remember(songs) {
                songs.map { GenreSongInfo(it.song.id, it.song.song.title, it.song.artists.firstOrNull()?.name) }
            },
        )

    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(isSearching) {
        if (isSearching) {
            focusRequester.requestFocus()
        }
    }

    var inSelectMode by rememberSaveable { mutableStateOf(false) }
    val selection =
        rememberSaveable(
            saver =
                listSaver<MutableList<Int>, Int>(
                    save = { it.toList() },
                    restore = { it.toMutableStateList() },
                ),
        ) { mutableStateListOf() }
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
        }
    } else if (inSelectMode) {
        BackHandler(onBack = onExitSelectionMode)
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
                } else if (songs.all {
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

    var showEditDialog by remember {
        mutableStateOf(false)
    }

    if (showEditDialog) {
        playlist?.playlist?.let { playlistEntity ->
            TextFieldDialog(
                icon = {
                    Icon(
                        painter = painterResource(R.drawable.edit),
                        contentDescription = null,
                    )
                },
                title = { Text(text = stringResource(R.string.edit_playlist)) },
                onDismiss = { showEditDialog = false },
                initialTextFieldValue =
                    TextFieldValue(
                        playlistEntity.name,
                        TextRange(playlistEntity.name.length),
                    ),
                onDone = { name ->
                    database.query {
                        update(
                            playlistEntity.copy(
                                name = name,
                                lastUpdateTime = LocalDateTime.now(),
                            ),
                        )
                    }
                    viewModel.viewModelScope.launch(Dispatchers.IO) {
                        playlistEntity.browseId?.let { YouTube.renamePlaylist(it, name) }
                    }
                },
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
                TextButton(
                    onClick = { showRemoveDownloadDialog = false },
                ) {
                    Text(text = stringResource(android.R.string.cancel))
                }

                TextButton(
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
                    },
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
                TextButton(
                    onClick = {
                        showDeletePlaylistDialog = false
                    },
                ) {
                    Text(text = stringResource(android.R.string.cancel))
                }
                TextButton(
                    onClick = {
                        showDeletePlaylistDialog = false
                        database.query {
                            playlist?.let { delete(it.playlist) }
                        }
                        viewModel.viewModelScope.launch(Dispatchers.IO) {
                            playlist?.playlist?.browseId?.let { YouTube.deletePlaylist(it) }
                        }
                        navController.popBackStack()
                    },
                ) {
                    Text(text = stringResource(android.R.string.ok))
                }
            },
        )
    }

    val headerItems = 2
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

                // Sync order with YT Music
                if (viewModel.playlist.value
                        ?.playlist
                        ?.browseId != null
                ) {
                    viewModel.viewModelScope.launch(Dispatchers.IO) {
                        val playlistSongMap = database.playlistSongMaps(viewModel.playlistId, 0)
                        val successorIndex = if (from > to) to else to + 1
                        val successorSetVideoId = playlistSongMap.getOrNull(successorIndex)?.setVideoId

                        playlistSongMap.getOrNull(from)?.setVideoId?.let { setVideoId ->
                            YouTube.moveSongPlaylist(
                                viewModel.playlist.value
                                    ?.playlist
                                    ?.browseId!!,
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
            genreFilter.matches(it.song.id)
        }

    LaunchedEffect(displayedSongs) {
        prefetchThumbnails(context, displayedSongs.map { it.song.thumbnailUrl })
    }

    Box(
        modifier = Modifier.fillMaxSize(),
    ) {
        LazyColumn(
            state = lazyListState,
            contentPadding = LocalPlayerAwareWindowInsets.current.union(WindowInsets.ime).asPaddingValues(),
        ) {
            playlist?.let { playlist ->
                if (playlist.songCount == 0 && playlist.playlist.remoteSongCount == 0) {
                    item(key = "empty_placeholder") {
                        EmptyPlaceholder(
                            icon = R.drawable.music_note,
                            text = stringResource(R.string.playlist_is_empty),
                            modifier = Modifier.animateItem(),
                        )
                    }
                } else {
                    if (!isSearching) {
                        item(key = "playlist_header") {
                            LocalPlaylistHeader(
                                playlist = playlist,
                                songs = songs,
                                onShowEditDialog = { showEditDialog = true },
                                onShowRemoveDownloadDialog = { showRemoveDownloadDialog = true },
                                onshowDeletePlaylistDialog = { showDeletePlaylistDialog = true },
                                onStartSearch = { isSearching = true },
                                snackbarHostState = snackbarHostState,
                                modifier = Modifier.animateItem(),
                            )
                        }
                    }

                    item(key = "controls_row") {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier =
                                Modifier
                                    .padding(
                                        // Matches SongListItem's own 12dp horizontal inset so the
                                        // sort row lines up with the song rows below it.
                                        start = if (topNavigationBarEnabled) 12.dp else 8.dp,
                                        end = if (topNavigationBarEnabled) 12.dp else 8.dp,
                                    )
                                    .animateItem(),
                        ) {
                            LibrarySortRow(
                                sortOptions =
                                    listOf(
                                        PlaylistSongSortType.CUSTOM to stringResource(R.string.sort_by_custom),
                                        PlaylistSongSortType.CREATE_DATE to stringResource(R.string.sort_by_create_date),
                                        PlaylistSongSortType.NAME to stringResource(R.string.sort_by_name),
                                        PlaylistSongSortType.ARTIST to stringResource(R.string.sort_by_artist),
                                        PlaylistSongSortType.PLAY_TIME to stringResource(R.string.sort_by_play_time),
                                    ),
                                currentSort = sortType,
                                onSortChange = onSortTypeChange,
                                sortDescending = sortDescending,
                                onSortDescendingChange = onSortDescendingChange,
                                showDescending = sortType != PlaylistSongSortType.CUSTOM,
                                useIrideStyle = topNavigationBarEnabled,
                                modifier = Modifier.weight(1f),
                            )
                            if (editable) {
                                IconButton(
                                    onClick = { locked = !locked },
                                    modifier = Modifier.padding(horizontal = 6.dp),
                                ) {
                                    Icon(
                                        painter = painterResource(if (locked) R.drawable.lock else R.drawable.lock_open),
                                        contentDescription = null,
                                    )
                                }
                            }
                        }
                    }

                    item(key = "genre_pills") {
                        GenrePillsRow(
                            state = genreFilter,
                            modifier = Modifier.animateItem(),
                        )
                    }
                }
            }

            itemsIndexed(
                items = displayedSongs,
                key = { _, song -> song.map.id },
            ) { index, song ->
                ReorderableItem(
                    state = reorderableState,
                    key = song.map.id,
                ) {
                    val currentItem by rememberUpdatedState(song)

                    fun deleteFromPlaylist() {
                        // Capture values before deletion — DB entry will be gone afterwards
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
                                playlistId
                            ) {
                                var setVideoId: String? = setVideoId  // already captured before deletion
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

                    val swipeRemoveEnabled by rememberPreference(SwipeToRemoveSongKey, defaultValue = true)
                    val dismissBoxState =
                        rememberSwipeToDismissBoxState(
                            positionalThreshold = { totalDistance -> totalDistance },
                        )
                    var processedDismiss by remember { mutableStateOf(false) }
                    LaunchedEffect(dismissBoxState.currentValue) {
                        val dv = dismissBoxState.currentValue
                        if (swipeRemoveEnabled && !processedDismiss && (
                                dv == SwipeToDismissBoxValue.StartToEnd ||
                                    dv == SwipeToDismissBoxValue.EndToStart
                            )
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
                            // New Iride UI: featured-artist subtitle text should match the rest of
                            // the row instead of the default muted secondary tone.
                            subtitleColor = if (topNavigationBarEnabled) Color.Unspecified else null,
                            isActive = song.song.id == mediaMetadata?.id,
                            isPlaying = isPlaying,
                            trailingContent = {
                                if (inSelectMode) {
                                    Checkbox(
                                        checked = selection.contains(song.map.id),
                                        onCheckedChange = onCheckedChange,
                                    )
                                } else {
                                    IconButton(
                                        onClick = {
                                            menuState.show {
                                                SongMenu(
                                                    originalSong = song.song,
                                                    playlistSong = song,
                                                    playlistBrowseId = playlist?.playlist?.browseId,
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

                                    if (sortType == PlaylistSongSortType.CUSTOM && !locked && !inSelectMode && !isSearching && editable) {
                                        IconButton(
                                            onClick = { },
                                            modifier = Modifier.draggableHandle(),
                                        ) {
                                            Icon(
                                                painter = painterResource(R.drawable.drag_handle),
                                                contentDescription = null,
                                            )
                                        }
                                    }
                                }
                            },
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .combinedClickable(
                                        onClick = {
                                            if (inSelectMode) {
                                                onCheckedChange(!selection.contains(song.map.id))
                                            } else if (song.song.id == mediaMetadata?.id) {
                                                playerConnection.togglePlayPause()
                                            } else {
                                                playerConnection.playQueue(
                                                    ListQueue(
                                                        title = playlist!!.playlist.name,
                                                        items = songs.map { it.song.toMediaItem() },
                                                        startIndex = songs.indexOfFirst { it.map.id == song.map.id },
                                                    ),
                                                )
                                            }
                                        },
                                        onLongClick = {
                                            if (!inSelectMode) {
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                inSelectMode = true
                                                onCheckedChange(true)
                                                selectionAnchorMapId = song.map.id
                                            } else {
                                                val anchorIndex =
                                                    selectionAnchorMapId?.let { anchorMapId ->
                                                        displayedSongs.indexOfFirst { it.map.id == anchorMapId }
                                                    } ?: -1

                                                if (anchorIndex == -1) {
                                                    onCheckedChange(true)
                                                    selectionAnchorMapId = song.map.id
                                                } else {
                                                    val range = if (anchorIndex <= index) anchorIndex..index else index..anchorIndex
                                                    for (rangeIndex in range) {
                                                        val rangeMapId = displayedSongs[rangeIndex].map.id
                                                        if (rangeMapId !in selection) {
                                                            selection.add(rangeMapId)
                                                        }
                                                    }
                                                }
                                            }
                                        },
                                    ),
                        )
                    }

                    if (locked || inSelectMode || !swipeRemoveEnabled) {
                        Box(modifier = Modifier.animateItem()) {
                            content()
                        }
                    } else {
                        SwipeToDismissBox(
                            state = dismissBoxState,
                            backgroundContent = {},
                            modifier = Modifier.animateItem(),
                        ) {
                            content()
                        }
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
            scrollState = lazyListState,
            headerItems = 2,
        )

        // Top-bar mirrors of the header's shuffle/play/download actions (New Iride UI only) — the
        // header versions live in a separate composable closing over a non-null `playlist` param.
        val onTopBarShuffleClick: () -> Unit = {
            playlist?.let { current ->
                playerConnection.playQueue(
                    ListQueue(
                        title = current.playlist.name,
                        items = songs.shuffled().map { it.song.toMediaItem() },
                    ),
                )
            }
        }
        val onTopBarPlaylistPlayClick: () -> Unit = {
            playlist?.let { current ->
                playerConnection.playQueue(
                    ListQueue(
                        title = current.playlist.name,
                        items = songs.map { it.song.toMediaItem() },
                    ),
                )
            }
        }
        val onTopBarPlaylistDownloadClick: () -> Unit = {
            when (downloadState) {
                Download.STATE_COMPLETED -> showRemoveDownloadDialog = true
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

        val topBarTitle: @Composable () -> Unit = {
            if (inSelectMode) {
                Text(pluralStringResource(R.plurals.n_selected, selection.size, selection.size))
            } else if (isSearching) {
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
            } else if (showTopBarTitle) {
                Text(playlist?.playlist?.name.orEmpty())
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
                    onClick = {
                        if (isSearching) {
                            isSearching = false
                            query = TextFieldValue()
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
                Checkbox(
                    checked = selection.size == songs.size && selection.isNotEmpty(),
                    onCheckedChange = {
                        if (selection.size == songs.size) {
                            selection.clear()
                        } else {
                            selection.clear()
                            selection.addAll(songs.map { it.map.id })
                        }
                    },
                )
                IconButton(
                    enabled = selection.isNotEmpty(),
                    onClick = {
                        menuState.show {
                            SelectionSongMenu(
                                songSelection =
                                    selection.mapNotNull { mapId ->
                                        songs.find { it.map.id == mapId }?.song
                                    },
                                songPosition =
                                    selection.mapNotNull { mapId ->
                                        songs.find { it.map.id == mapId }?.map
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
            } else if (!isSearching) {
                // Only search button remains in TopAppBar
                IconButton(
                    onClick = { isSearching = true },
                ) {
                    Icon(
                        painter = painterResource(R.drawable.search),
                        contentDescription = null,
                    )
                }
                playlist?.let { currentPlaylist ->
                    IconButton(
                        onClick = {
                            menuState.show {
                                LocalPlaylistMenu(
                                    playlist = currentPlaylist,
                                    songs = songs,
                                    context = context,
                                    downloadState = downloadState,
                                    onEdit = { showEditDialog = true },
                                    onSync = { syncUtils.syncSavedPlaylists() },
                                    onDelete = { showDeletePlaylistDialog = true },
                                    onDownload = {
                                        when (downloadState) {
                                            Download.STATE_COMPLETED -> showRemoveDownloadDialog = true
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
                                                    val downloadRequest = DownloadRequest
                                                        .Builder(song.song.id, song.song.id.toUri())
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
                                    },
                                    onQueue = {
                                        playerConnection.playQueue(
                                            ListQueue(
                                                title = currentPlaylist.playlist.name,
                                                items = songs.map { it.song.toMediaItem() },
                                            ),
                                        )
                                    },
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
                    .background(MaterialTheme.colorScheme.background)
                    .statusBarsPadding()
                    .height(56.dp)
                    .padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                topBarNavigationIcon()
                Box(modifier = Modifier.weight(1f).padding(start = 4.dp)) {
                    topBarTitle()
                }
                if (!inSelectMode && !isSearching && playlist != null) {
                    IrideOutlineIconButton(
                        onClick = onTopBarShuffleClick,
                        icon = R.drawable.shuffle,
                        contentDescription = stringResource(R.string.shuffle),
                        size = 40.dp,
                        iconSize = 20.dp,
                    )
                    IrideOutlineIconButton(
                        onClick = onTopBarPlaylistPlayClick,
                        icon = R.drawable.ic_iride_play,
                        contentDescription = stringResource(R.string.play),
                        size = 40.dp,
                        iconSize = 20.dp,
                    )
                    IrideOutlineIconButton(
                        onClick = onTopBarPlaylistDownloadClick,
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
                title = topBarTitle,
                navigationIcon = topBarNavigationIcon,
                actions = topBarActions,
            )
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier =
                Modifier
                    .windowInsetsPadding(LocalPlayerAwareWindowInsets.current.union(WindowInsets.ime))
                    .align(Alignment.BottomCenter),
        )
    }
}

@Composable
fun LocalPlaylistHeader(
    playlist: Playlist,
    songs: List<PlaylistSong>,
    onShowEditDialog: () -> Unit,
    onShowRemoveDownloadDialog: () -> Unit,
    onshowDeletePlaylistDialog: () -> Unit,
    onStartSearch: () -> Unit,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier,
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
    val topNavigationBarEnabled by rememberPreference(TopNavigationBarKey, defaultValue = true)

    val overrideThumbnail = remember { mutableStateOf<String?>(null) }
    var isCustomThumbnail: Boolean =
        playlist.thumbnails.firstOrNull()?.let {
            it.contains("studio_square_thumbnail") || it.contains("content://com.metrolist.music")
        } ?: false

    val result = remember { mutableStateOf<Uri?>(null) }
    var pendingCropDestUri by remember { mutableStateOf<Uri?>(null) }
    var showEditNoteDialog by remember { mutableStateOf(false) }

    val cropLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { res ->
            if (res.resultCode == android.app.Activity.RESULT_OK) {
                val output = res.data?.let { UCrop.getOutput(it) } ?: pendingCropDestUri
                if (output != null) result.value = output
            }
        }

    val (darkMode, _) =
        rememberEnumPreference(
            DarkModeKey,
            defaultValue = DarkMode.AUTO,
        )

    val cropColor = MaterialTheme.colorScheme
    val darkTheme = darkMode == DarkMode.ON || (darkMode == DarkMode.AUTO && isSystemInDarkTheme())

    val pickLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.PickVisualMedia(),
        ) { uri ->
            uri?.let { sourceUri ->
                val destFile = java.io.File(context.cacheDir, "playlist_cover_crop_${System.currentTimeMillis()}.jpg")
                val destUri = FileProvider.getUriForFile(context, "${context.packageName}.FileProvider", destFile)
                pendingCropDestUri = destUri

                val options =
                    UCrop.Options().apply {
                        setCompressionFormat(Bitmap.CompressFormat.JPEG)
                        setCompressionQuality(90)
                        setHideBottomControls(true)
                        setToolbarTitle(editPlaylistCoverStr)

                        setStatusBarLight(!darkTheme)

                        setToolbarColor(cropColor.surface.toArgb())
                        setToolbarWidgetColor(cropColor.inverseSurface.toArgb())
                        setRootViewBackgroundColor(cropColor.surface.toArgb())
                        setLogoColor(cropColor.surface.toArgb())
                    }

                val intent =
                    UCrop
                        .of(sourceUri, destUri)
                        .withAspectRatio(1f, 1f)
                        .withOptions(options)
                        .getIntent(context)
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                cropLauncher.launch(intent)
            }
        }

    LaunchedEffect(result.value) {
        val uri = result.value ?: return@LaunchedEffect
        withContext(Dispatchers.IO) {
            when {
                playlist.playlist.browseId == null -> {
                    overrideThumbnail.value = uri.toString()
                    isCustomThumbnail = true

                    // Update the database with the new thumbnail
                    database.query {
                        update(playlist.playlist.copy(thumbnailUrl = uri.toString()))
                    }
                }

                else -> {
                    val bytes = uriToByteArray(context, uri)
                    YouTube
                        .uploadCustomThumbnailLink(
                            playlist.playlist.browseId,
                            bytes!!,
                        ).onSuccess { newThumbnailUrl ->
                            overrideThumbnail.value = newThumbnailUrl
                            isCustomThumbnail = true

                            // Update the database with the new thumbnail URL
                            database.query {
                                update(playlist.playlist.copy(thumbnailUrl = newThumbnailUrl))
                            }
                        }.onFailure {
                            if (it is ClientRequestException) {
                                snackbarHostState.showSnackbar("${it.response.status.value} ${it.response.status.description}")
                            }
                            reportException(it)
                        }
                }
            }
        }
    }

    LaunchedEffect(songs) {
        if (songs.isEmpty()) return@LaunchedEffect
        downloadUtil.downloads.collect { downloads ->
            downloadState =
                if (songs.all { downloads[it.song.id]?.state == Download.STATE_COMPLETED }) {
                    Download.STATE_COMPLETED
                } else if (songs.all {
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
        modifier =
            modifier
                .fillMaxWidth()
                .padding(top = 8.dp, bottom = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (showEditNoteDialog) {
            ActionPromptDialog(
                title = stringResource(R.string.edit_playlist_cover),
                onDismiss = { showEditNoteDialog = false },
                onConfirm = {
                    showEditNoteDialog = false
                    pickLauncher.launch(
                        PickVisualMediaRequest(mediaType = ActivityResultContracts.PickVisualMedia.ImageOnly),
                    )
                },
                onCancel = { showEditNoteDialog = false },
            ) {
                if (playlist.playlist.browseId != null) {
                    Text(
                        text = stringResource(R.string.edit_playlist_cover_note),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Spacer(Modifier.height(8.dp))
                }
                Text(
                    text = stringResource(R.string.edit_playlist_cover_note_wait),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                )
            }
        }
        // Playlist Thumbnail(s) - shared between both layouts below, only the size differs.
        val playlistCoverSquircle = SquircleShape(radius = 12.dp, cornerSmoothing = 0.45f)
        val playlistCoverArt: @Composable (Dp) -> Unit = { coverSize ->
            Box {
                when (playlist.thumbnails.size) {
                    0 -> {
                        Surface(
                            modifier =
                                Modifier
                                    .size(coverSize)
                                    .shadow(
                                        elevation = 16.dp,
                                        shape = playlistCoverSquircle,
                                    ),
                            shape = playlistCoverSquircle,
                            color = MaterialTheme.colorScheme.surfaceVariant,
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.queue_music),
                                    contentDescription = null,
                                    modifier = Modifier.size(coverSize * 0.33f),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }

                    1 -> {
                        Surface(
                            modifier =
                                Modifier
                                    .size(coverSize)
                                    .shadow(
                                        elevation = 24.dp,
                                        shape = playlistCoverSquircle,
                                        spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                                    ),
                            shape = playlistCoverSquircle,
                        ) {
                            AsyncImage(
                                model = overrideThumbnail.value ?: playlist.thumbnails[0],
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize(),
                            )
                        }
                        if (editable) {
                            OverlayEditButton(
                                visible = true,
                                alignment = Alignment.BottomEnd,
                                onClick = {
                                    if (isCustomThumbnail) {
                                        menuState.show(
                                            {
                                                CustomThumbnailMenu(
                                                    onEdit = {
                                                        pickLauncher.launch(
                                                            PickVisualMediaRequest(
                                                                mediaType = ActivityResultContracts.PickVisualMedia.ImageOnly,
                                                            ),
                                                        )
                                                    },
                                                    onRemove = {
                                                        when {
                                                            playlist.playlist.browseId == null -> {
                                                                overrideThumbnail.value = null
                                                                database.query {
                                                                    update(playlist.playlist.copy(thumbnailUrl = null))
                                                                }
                                                            }

                                                            else -> {
                                                                scope.launch(Dispatchers.IO) {
                                                                    YouTube.removeThumbnailPlaylist(playlist.playlist.browseId).onSuccess { newThumbnailUrl ->
                                                                        overrideThumbnail.value = newThumbnailUrl
                                                                        database.query {
                                                                            update(playlist.playlist.copy(thumbnailUrl = newThumbnailUrl))
                                                                        }
                                                                    }
                                                                }
                                                            }
                                                        }
                                                        isCustomThumbnail = false
                                                    },
                                                    onDismiss = menuState::dismiss,
                                                )
                                            },
                                        )
                                    } else {
                                        showEditNoteDialog = true
                                    }
                                },
                            )
                        }
                    }

                    else -> {
                        Surface(
                            modifier =
                                Modifier
                                    .size(coverSize)
                                    .shadow(
                                        elevation = 24.dp,
                                        shape = playlistCoverSquircle,
                                        spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                                    ),
                            shape = playlistCoverSquircle,
                        ) {
                            Box(modifier = Modifier.fillMaxSize()) {
                                listOf(
                                    Alignment.TopStart,
                                    Alignment.TopEnd,
                                    Alignment.BottomStart,
                                    Alignment.BottomEnd,
                                ).fastForEachIndexed { index, alignment ->
                                    AsyncImage(
                                        model = playlist.thumbnails.getOrNull(index),
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier =
                                            Modifier
                                                .align(alignment)
                                                .size(coverSize / 2),
                                    )
                                }
                            }
                        }
                        if (editable) {
                            OverlayEditButton(
                                visible = true,
                                alignment = Alignment.BottomEnd,
                                onClick = {
                                    if (isCustomThumbnail) {
                                        menuState.show(
                                            {
                                                CustomThumbnailMenu(
                                                    onEdit = {
                                                        pickLauncher.launch(
                                                            PickVisualMediaRequest(
                                                                mediaType = ActivityResultContracts.PickVisualMedia.ImageOnly,
                                                            ),
                                                        )
                                                    },
                                                    onRemove = {
                                                        when {
                                                            playlist.playlist.browseId == null -> {
                                                                overrideThumbnail.value = null
                                                                database.query {
                                                                    update(playlist.playlist.copy(thumbnailUrl = null))
                                                                }
                                                            }

                                                            else -> {
                                                                scope.launch(Dispatchers.IO) {
                                                                    YouTube.removeThumbnailPlaylist(playlist.playlist.browseId).onSuccess { newThumbnailUrl ->
                                                                        overrideThumbnail.value = newThumbnailUrl
                                                                        database.query {
                                                                            update(playlist.playlist.copy(thumbnailUrl = newThumbnailUrl))
                                                                        }
                                                                    }
                                                                }
                                                            }
                                                        }
                                                        isCustomThumbnail = false
                                                    },
                                                    onDismiss = menuState::dismiss,
                                                )
                                            },
                                        )
                                    } else {
                                        showEditNoteDialog = true
                                    }
                                },
                            )
                        }
                    }
                }
            }
        }

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
                    title = playlist.playlist.name,
                    items = songs.shuffled().map { it.song.toMediaItem() },
                ),
            )
        }
        val onPlaylistPlayClick: () -> Unit = {
            playerConnection.playQueue(
                ListQueue(
                    title = playlist.playlist.name,
                    items = songs.map { it.song.toMediaItem() },
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

        if (topNavigationBarEnabled) {
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    playlistCoverArt(240.dp)
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = playlist.playlist.name,
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

                Text(
                    text = metadataLine,
                    style = TextStyle(fontFamily = SpaceMonoFontFamily, fontSize = 13.sp),
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
                // Action buttons (shuffle/play/download) live in the top bar now — see topBarActions.
            }
        } else {
        Box(
            modifier = Modifier.padding(top = 8.dp, bottom = 20.dp),
        ) {
            playlistCoverArt(240.dp)
        }

        // Playlist Name
        Text(
            text = playlist.playlist.name,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
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
            Surface(
                onClick = onShuffleClick,
                shape = CircleShape,
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
                onClick = onPlaylistDownloadClick,
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

@Composable
private fun MetadataChip(
    icon: Int,
    text: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
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

fun uriToByteArray(
    context: Context,
    uri: Uri,
): ByteArray? =
    try {
        context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
    } catch (_: SecurityException) {
        null
    }
