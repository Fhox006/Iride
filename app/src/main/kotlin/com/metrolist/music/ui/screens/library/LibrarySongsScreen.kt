/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.screens.library

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.metrolist.innertube.YouTube
import com.metrolist.music.LocalPlayerAwareWindowInsets
import com.metrolist.music.LocalPlayerConnection
import com.metrolist.music.R
import com.metrolist.music.constants.CONTENT_TYPE_HEADER
import com.metrolist.music.constants.CONTENT_TYPE_SONG
import com.metrolist.music.constants.HideExplicitKey
import com.metrolist.music.constants.PureBlackKey
import com.metrolist.music.constants.SongFilter
import com.metrolist.music.constants.SongFilterKey
import com.metrolist.music.constants.SongSortDescendingKey
import com.metrolist.music.constants.SongSortType
import com.metrolist.music.constants.SongSortTypeKey
import com.metrolist.music.constants.YtmSyncKey
import com.metrolist.music.extensions.matchesNormalizedQuery
import com.metrolist.music.extensions.normalizeForSearch
import com.metrolist.music.extensions.toMediaItem
import com.metrolist.music.playback.queues.ListQueue
import com.metrolist.music.ui.component.ChipsRow
import com.metrolist.music.ui.component.CollapsingScreenHeader
import com.metrolist.music.ui.component.DefaultDialog
import com.metrolist.music.ui.component.HideOnScrollFAB
import com.metrolist.music.ui.component.LibrarySearchEmptyPlaceholder
import com.metrolist.music.ui.component.LibrarySortRow
import com.metrolist.music.ui.component.LocalMenuState
import com.metrolist.music.ui.component.SongListItem
import com.metrolist.music.ui.menu.SongMenu
import com.metrolist.music.utils.rememberEnumPreference
import com.metrolist.music.utils.rememberPreference
import com.metrolist.music.viewmodels.LibrarySongsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun LibrarySongsScreen(
    navController: NavController,
    isOffline: Boolean = false,
    viewModel: LibrarySongsViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val menuState = LocalMenuState.current
    val uploadUnsupportedFormatStr = stringResource(R.string.upload_unsupported_format)
    val uploadFileTooLargeStr = stringResource(R.string.upload_file_too_large)
    val uploadFailedStr = stringResource(R.string.upload_failed)
    val uploadCompleteStr = stringResource(R.string.upload_complete)
    val queueAllSongsStr = stringResource(R.string.queue_all_songs)
    val playerConnection = LocalPlayerConnection.current ?: return
    val isPlaying by playerConnection.isEffectivelyPlaying.collectAsState()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()
    val scope = rememberCoroutineScope()

    val (sortType, onSortTypeChange) =
        rememberEnumPreference(SongSortTypeKey, SongSortType.CREATE_DATE)
    val (sortDescending, onSortDescendingChange) = rememberPreference(SongSortDescendingKey, true)

    val (ytmSync) = rememberPreference(YtmSyncKey, true)
    val hideExplicit by rememberPreference(key = HideExplicitKey, defaultValue = false)
    val pureBlack by rememberPreference(PureBlackKey, defaultValue = false)
    val (topNavigationBarEnabled) = rememberPreference(com.metrolist.music.constants.TopNavigationBarKey, defaultValue = true)
    val betterLibraryBeta by rememberPreference(com.metrolist.music.constants.BetterLibraryBetaKey, defaultValue = false)

    val songs by (if (isOffline) viewModel.downloadedSongs else viewModel.allSongs).collectAsState()
    var isSearchActive by rememberSaveable { mutableStateOf(false) }
    val searchQuery by viewModel.searchQuery.collectAsState()
    val debouncedSearchQuery by viewModel.debouncedSearchQuery.collectAsState()
    val normalizedQuery = remember(debouncedSearchQuery) { debouncedSearchQuery.normalizeForSearch() }

    var filter by rememberEnumPreference(SongFilterKey, SongFilter.LIBRARY)

    LaunchedEffect(Unit) {
        filter = SongFilter.LIBRARY
    }

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
                                        Toast.makeText(context, uploadUnsupportedFormatStr, Toast.LENGTH_SHORT).show()
                                    }
                                    return@forEachIndexed
                                }

                                val inputStream = context.contentResolver.openInputStream(uri)
                                val data = inputStream?.readBytes()
                                inputStream?.close()

                                if (data == null) return@forEachIndexed

                                if (data.size > YouTube.MAX_UPLOAD_SIZE) {
                                    withContext(Dispatchers.Main) {
                                        Toast.makeText(context, uploadFileTooLargeStr, Toast.LENGTH_SHORT).show()
                                    }
                                    return@forEachIndexed
                                }

                                val result =
                                    YouTube.uploadSong(
                                        filename = fileName,
                                        data = data,
                                        onProgress = { progress -> uploadProgress = progress },
                                    )

                                if (result.isSuccess && result.getOrDefault(false)) {
                                    successCount++
                                }
                            } catch (e: Exception) {
                                withContext(Dispatchers.Main) {
                                    Toast.makeText(context, uploadFailedStr + ": ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }

                        isUploading = false

                        if (successCount > 0) {
                            uploadProgress = 1f
                            currentFileName = uploadCompleteStr
                            kotlinx.coroutines.delay(1000)
                            withContext(Dispatchers.Main) {
                                Toast.makeText(context, uploadCompleteStr, Toast.LENGTH_SHORT).show()
                            }
                            showUploadDialog = false
                            viewModel.syncUploadedSongs()
                        } else {
                            showUploadDialog = false
                        }
                    }
            }
        }

    LaunchedEffect(filter) {
        if (ytmSync) {
            when (filter) {
                SongFilter.LIKED -> viewModel.syncLikedSongs()
                SongFilter.LIBRARY -> viewModel.syncLibrarySongs()
                SongFilter.UPLOADED -> viewModel.syncUploadedSongs()
                else -> return@LaunchedEffect
            }
        }
    }

    val lazyListState = rememberLazyListState()

    val backStackEntry by navController.currentBackStackEntryAsState()
    val scrollToTop =
        backStackEntry?.savedStateHandle?.getStateFlow("scrollToTop", false)?.collectAsState()

    LaunchedEffect(scrollToTop?.value) {
        if (scrollToTop?.value == true) {
            lazyListState.animateScrollToItem(0)
            backStackEntry?.savedStateHandle?.set("scrollToTop", false)
        }
    }

    val filteredSongs =
        (if (hideExplicit) songs.filter { !it.song.explicit } else songs).filter { song ->
            val artistNames = song.artists.map { it.name }.toTypedArray()
            matchesNormalizedQuery(normalizedQuery, song.song.title, song.album?.title, *artistNames)
        }

    val sortOptions = listOf(
        SongSortType.CREATE_DATE to stringResource(R.string.sort_by_create_date),
        SongSortType.NAME        to stringResource(R.string.sort_by_name),
        SongSortType.ARTIST      to stringResource(R.string.sort_by_artist),
        SongSortType.PLAY_TIME   to stringResource(R.string.sort_by_play_time),
    )

    val itemCountText = pluralStringResource(R.plurals.n_song, filteredSongs.size, filteredSongs.size)

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

    val scrollBehavior = if (betterLibraryBeta) {
        TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    } else {
        TopAppBarDefaults.exitUntilCollapsedScrollBehavior(
            snapAnimationSpec = tween(durationMillis = 200),
        )
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            CollapsingScreenHeader(
                title = stringResource(R.string.all_tracks),
                scrollBehavior = scrollBehavior,
                pureBlack = pureBlack,
                isSearchActive = isSearchActive,
                onSearchActiveChange = { active ->
                    isSearchActive = active
                    if (!active) viewModel.updateSearchQuery("")
                },
                searchQuery = searchQuery,
                onSearchQueryChange = viewModel::updateSearchQuery,
                keyboardController = keyboardController,
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(
                            painter = painterResource(R.drawable.arrow_back),
                            contentDescription = if (betterLibraryBeta)
                                stringResource(R.string.navigate_back)
                            else null,
                        )
                    }
                },
            )
        },
        containerColor = if (betterLibraryBeta) {
            if (pureBlack) Color.Black else MaterialTheme.colorScheme.background
        } else {
            Color.Transparent
        },
        contentWindowInsets = WindowInsets(0),
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (!betterLibraryBeta) {
                        Modifier.background(if (pureBlack) Color.Black else MaterialTheme.colorScheme.background)
                    } else {
                        Modifier
                    }
                )
                .padding(paddingValues),
        ) {
            LazyColumn(
                state = lazyListState,
                contentPadding = PaddingValues(
                    start = 12.dp,
                    end = 12.dp,
                    top = 0.dp,
                    bottom = LocalPlayerAwareWindowInsets.current
                        .asPaddingValues().calculateBottomPadding(),
                ),
            ) {
                if (!isOffline) {
                    item(key = "filter", contentType = CONTENT_TYPE_HEADER) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .then(if (betterLibraryBeta) Modifier else Modifier.height(0.dp)),
                        ) {
                            ChipsRow(
                                chips = listOf(
                                    SongFilter.LIKED      to stringResource(R.string.filter_liked),
                                    SongFilter.LIBRARY    to stringResource(R.string.filter_library),
                                    SongFilter.UPLOADED   to stringResource(R.string.filter_uploaded),
                                    SongFilter.DOWNLOADED to stringResource(R.string.filter_downloaded),
                                ),
                                currentValue = filter,
                                onValueUpdate = { filter = it },
                                modifier = Modifier.weight(1f),
                            )
                            IconButton(onClick = { /* TODO: star action */ }) {
                                Icon(
                                    painter = painterResource(R.drawable.star),
                                    contentDescription = if (betterLibraryBeta) stringResource(R.string.starred) else null,
                                    tint = if (betterLibraryBeta) MaterialTheme.colorScheme.onSurfaceVariant else Color.White,
                                )
                            }
                        }
                    }
                }

                item(key = "sort", contentType = CONTENT_TYPE_HEADER) {
                    LibrarySortRow(
                        sortOptions = sortOptions,
                        currentSort = sortType,
                        onSortChange = onSortTypeChange,
                        sortDescending = sortDescending,
                        onSortDescendingChange = onSortDescendingChange,
                        useIrideStyle = topNavigationBarEnabled,
                    )
                }

                if (filteredSongs.isEmpty() && searchQuery.isNotBlank()) {
                    item(key = "empty_search_result", contentType = CONTENT_TYPE_HEADER) {
                        LibrarySearchEmptyPlaceholder(modifier = Modifier.animateItem())
                    }
                }

                itemsIndexed(
                    items = filteredSongs,
                    key = { _, item -> item.song.id },
                    contentType = { _, _ -> CONTENT_TYPE_SONG },
                ) { index, song ->
                    SongListItem(
                        song = song,
                        isActive = song.id == mediaMetadata?.id,
                        isPlaying = isPlaying,
                        showLikedIcon = false,
                        showDownloadIcon = filter != SongFilter.DOWNLOADED,
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
                                    contentDescription = if (betterLibraryBeta)
                                        stringResource(R.string.more_options)
                                    else null,
                                )
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (song.id == mediaMetadata?.id) {
                                    playerConnection.togglePlayPause()
                                } else {
                                    playerConnection.playQueue(
                                        ListQueue(
                                            title = queueAllSongsStr,
                                            items = filteredSongs.map { it.toMediaItem() },
                                            startIndex = index,
                                        ),
                                    )
                                }
                            }
                            .animateItem(),
                    )
                }

                item(key = "footer") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = itemCountText,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            HideOnScrollFAB(
                visible = if (filter == SongFilter.UPLOADED) true else filteredSongs.isNotEmpty(),
                lazyListState = lazyListState,
                icon = if (filter == SongFilter.UPLOADED) R.drawable.upload else R.drawable.shuffle,
                label = if (betterLibraryBeta) {
                    if (filter == SongFilter.UPLOADED)
                        stringResource(R.string.upload)
                    else
                        stringResource(R.string.shuffle)
                } else null,
                onClick = {
                    if (filter == SongFilter.UPLOADED) {
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
                    } else {
                        playerConnection.playQueue(
                            ListQueue(
                                title = queueAllSongsStr,
                                items = filteredSongs.shuffled().map { it.toMediaItem() },
                            ),
                        )
                    }
                },
            )
        }
    }
}
