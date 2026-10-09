/** Metrolist Project (C) 2026 Licensed under GPL-3.0 | See git history for contributors */
package com.metrolist.music.ui.menu

import android.annotation.SuppressLint
import android.content.Intent
import android.content.res.Configuration
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.DownloadRequest
import androidx.media3.exoplayer.offline.DownloadService
import coil3.compose.AsyncImage
import com.metrolist.innertube.YouTube
import com.metrolist.innertube.models.PlaylistItem
import com.metrolist.innertube.models.SongItem
import com.metrolist.innertube.utils.completed
import com.metrolist.music.LocalDatabase
import com.metrolist.music.LocalDownloadUtil
import com.metrolist.music.LocalListenTogetherManager
import com.metrolist.music.LocalPlayerConnection
import com.metrolist.music.R
import com.metrolist.music.constants.ListThumbnailSize
import com.metrolist.music.constants.ThumbnailCornerRadius
import com.metrolist.music.db.entities.PlaylistEntity
import com.metrolist.music.db.entities.PlaylistSongMap
import com.metrolist.music.extensions.toMediaItem
import com.metrolist.music.models.MediaMetadata
import com.metrolist.music.models.toMediaMetadata
import com.metrolist.music.playback.ExoDownloadService
import com.metrolist.music.ui.component.DefaultDialog
import com.metrolist.music.ui.component.IrideFolioContextMenu
import com.metrolist.music.ui.component.IrideFolioPrimaryButton
import com.metrolist.music.ui.component.IrideFolioSecondaryButton
import com.metrolist.music.ui.component.ListDialog
import com.metrolist.music.ui.component.Material3MenuItemData
import com.metrolist.music.ui.component.TextFieldDialog
import com.metrolist.music.ui.component.folioLibraryAction
import com.metrolist.music.ui.component.folioMenuAction
import com.metrolist.music.ui.utils.resize
import com.metrolist.music.utils.exportYouTubePlaylistAsCSV
import com.metrolist.music.utils.exportYouTubePlaylistAsM3U
import com.metrolist.music.utils.getExportFileUri
import com.metrolist.music.utils.joinByBullet
import com.metrolist.music.utils.makeTimeString
import com.metrolist.music.utils.reportException
import com.metrolist.music.utils.saveToPublicDocuments
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("MutableCollectionMutableState")
@Composable
fun YouTubePlaylistMenu(
    playlist: PlaylistItem,
    songs: List<SongItem> = emptyList(),
    coroutineScope: CoroutineScope,
    onDismiss: () -> Unit,
    selectAction: () -> Unit = {},
    canSelect: Boolean = false,
    showPageControls: Boolean = true,
) {
    var menuSongs by remember(playlist.id, songs) { mutableStateOf(songs) }
    LaunchedEffect(playlist.id, songs) {
        if (songs.isEmpty()) {
            menuSongs = YouTube.playlist(playlist.id).completed().getOrNull()?.songs.orEmpty()
        }
    }
    val context = LocalContext.current
    val database = LocalDatabase.current
    val downloadUtil = LocalDownloadUtil.current
    val playerConnection = LocalPlayerConnection.current ?: return
    val listenTogetherManager = LocalListenTogetherManager.current
    val isGuest = listenTogetherManager?.isInRoom == true && !listenTogetherManager.isHost
    val dbPlaylist by database.playlistByBrowseId(playlist.id).collectAsState(initial = null)

    var showChoosePlaylistDialog by rememberSaveable { mutableStateOf(false) }
    var showImportPlaylistDialog by rememberSaveable { mutableStateOf(false) }
    var showErrorPlaylistAddDialog by rememberSaveable { mutableStateOf(false) }

    val notAddedList by remember { mutableStateOf(mutableListOf<MediaMetadata>()) }

    suspend fun sourceSongIds(): List<String> {
        val source =
            menuSongs.ifEmpty {
                YouTube.playlist(playlist.id).completed().getOrNull()?.songs.orEmpty()
            }
        database.withTransaction { source.map { it.toMediaMetadata() }.forEach(::insert) }
        return source.map { it.id }
    }

    AddToPlaylistDialog(
        isVisible = showChoosePlaylistDialog,
        onGetSong = { sourceSongIds() },
        onGetSongIds = { sourceSongIds() },
        onDismiss = { showChoosePlaylistDialog = false },
    )

    var downloadState by remember { mutableIntStateOf(Download.STATE_STOPPED) }
    LaunchedEffect(menuSongs) {
        if (menuSongs.isEmpty()) return@LaunchedEffect
        downloadUtil.downloads.collect { downloads ->
            downloadState =
                if (menuSongs.all { downloads[it.id]?.state == Download.STATE_COMPLETED }) {
                    Download.STATE_COMPLETED
                } else if (
                    menuSongs.all {
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
    var showExportDialog by remember { mutableStateOf(false) }
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
                        menuSongs.forEach { song ->
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

    ImportPlaylistDialog(
        isVisible = showImportPlaylistDialog,
        onGetSong = {
            val allSongs =
                menuSongs
                    .ifEmpty {
                        YouTube.playlist(playlist.id).completed().getOrNull()?.songs.orEmpty()
                    }
                    .map { it.toMediaMetadata() }
            database.withTransaction { allSongs.forEach(::insert) }
            allSongs.map { it.id }
        },
        playlistTitle = playlist.title,
        onDismiss = { showImportPlaylistDialog = false },
    )

    if (showErrorPlaylistAddDialog) {
        ListDialog(
            onDismiss = {
                showErrorPlaylistAddDialog = false
                onDismiss()
            }
        ) {
            item {
                ListItem(
                    headlineContent = { Text(text = stringResource(R.string.already_in_playlist)) },
                    leadingContent = {
                        Image(
                            painter = painterResource(R.drawable.close),
                            contentDescription = null,
                            colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onBackground),
                            modifier = Modifier.size(ListThumbnailSize),
                        )
                    },
                    modifier = Modifier.clickable { showErrorPlaylistAddDialog = false },
                )
            }

            items(notAddedList) { song ->
                ListItem(
                    headlineContent = { Text(text = song.title) },
                    leadingContent = {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.size(ListThumbnailSize),
                        ) {
                            AsyncImage(
                                model = song.thumbnailUrl,
                                contentDescription = null,
                                modifier =
                                    Modifier.fillMaxSize()
                                        .clip(RoundedCornerShape(ThumbnailCornerRadius)),
                            )
                        }
                    },
                    supportingContent = {
                        Text(
                            text =
                                joinByBullet(
                                    song.artists.joinToString { it.name },
                                    makeTimeString(song.duration * 1000L),
                                )
                        )
                    },
                )
            }
        }
    }

    val configuration = LocalConfiguration.current
    val isPortrait = configuration.orientation == Configuration.ORIENTATION_PORTRAIT

    var showRenameDialog by rememberSaveable { mutableStateOf(false) }
    var showDeleteDialog by rememberSaveable { mutableStateOf(false) }
    if (showRenameDialog && playlist.isEditable && !isGuest) {
        TextFieldDialog(
            title = { Text(stringResource(R.string.folio_rename)) },
            onDismiss = { showRenameDialog = false },
            initialTextFieldValue = androidx.compose.ui.text.input.TextFieldValue(playlist.title),
            onDone = { name ->
                coroutineScope.launch(Dispatchers.IO) {
                    YouTube.renamePlaylist(playlist.id, name)
                        .onSuccess {
                            database.query {
                                dbPlaylist?.playlist?.copy(name = name)?.let(::update)
                            }
                        }
                        .onFailure(::reportException)
                }
                onDismiss()
            },
        )
    }
    if (showDeleteDialog && playlist.isEditable && !isGuest) {
        DefaultDialog(
            onDismiss = { showDeleteDialog = false },
            title = { Text(stringResource(R.string.delete_playlist_confirm)) },
            content = { Text(playlist.title) },
            buttons = {
                IrideFolioSecondaryButton(onClick = { showDeleteDialog = false }) {
                    Text(stringResource(android.R.string.cancel))
                }
                IrideFolioPrimaryButton(
                    onClick = {
                        coroutineScope.launch(Dispatchers.IO) {
                            YouTube.deletePlaylist(playlist.id)
                                .onSuccess {
                                    database.transaction {
                                        dbPlaylist?.playlist?.let {
                                            deleteCategoriesForPlaylist(it.id)
                                            delete(it)
                                        }
                                    }
                                    database.speedDialDao.delete(playlist.id)
                                }
                                .onFailure(::reportException)
                        }
                        onDismiss()
                    }
                ) {
                    Text(stringResource(android.R.string.ok))
                }
            },
        )
    }

    IrideFolioContextMenu(
        items =
            buildList {
                    if (playlist.isEditable && !isGuest) {
                        add(
                            folioMenuAction(
                                R.string.edit,
                                R.drawable.edit,
                                { showRenameDialog = true },
                                text = stringResource(R.string.folio_rename),
                            )
                        )
                        add(
                            folioMenuAction(
                                R.string.delete,
                                R.drawable.delete,
                                { showDeleteDialog = true },
                            )
                        )
                    }

                    addAll(
                        listOfNotNull(
                            if (!isGuest) {
                                Material3MenuItemData(
                                    actionId = R.string.play_next,
                                    title = { Text(text = stringResource(R.string.play_next)) },
                                    description = {
                                        Text(text = stringResource(R.string.play_next_desc))
                                    },
                                    icon = {
                                        Icon(
                                            painter = painterResource(R.drawable.playlist_play),
                                            contentDescription = null,
                                        )
                                    },
                                    onClick = {
                                        coroutineScope.launch {
                                            menuSongs
                                                .ifEmpty {
                                                    withContext(Dispatchers.IO) {
                                                        YouTube.playlist(playlist.id)
                                                            .completed()
                                                            .getOrNull()
                                                            ?.songs
                                                            .orEmpty()
                                                    }
                                                }
                                                .let { menuSongs ->
                                                    playerConnection.playNext(
                                                        menuSongs.map {
                                                            it.copy(
                                                                    thumbnail =
                                                                        it.thumbnail.resize(
                                                                            544,
                                                                            544,
                                                                        )
                                                                )
                                                                .toMediaItem()
                                                        }
                                                    )
                                                }
                                        }
                                        onDismiss()
                                    },
                                )
                            } else {
                                null
                            },
                            if (!isGuest) {
                                Material3MenuItemData(
                                    actionId = R.string.add_to_queue,
                                    title = { Text(text = stringResource(R.string.add_to_queue)) },
                                    description = {
                                        Text(text = stringResource(R.string.add_to_queue_desc))
                                    },
                                    icon = {
                                        Icon(
                                            painter = painterResource(R.drawable.queue_music),
                                            contentDescription = null,
                                        )
                                    },
                                    onClick = {
                                        coroutineScope.launch {
                                            menuSongs
                                                .ifEmpty {
                                                    withContext(Dispatchers.IO) {
                                                        YouTube.playlist(playlist.id)
                                                            .completed()
                                                            .getOrNull()
                                                            ?.songs
                                                            .orEmpty()
                                                    }
                                                }
                                                .let { menuSongs ->
                                                    playerConnection.addToQueue(
                                                        menuSongs.map { it.toMediaItem() }
                                                    )
                                                }
                                        }
                                        onDismiss()
                                    },
                                )
                            } else {
                                null
                            },
                            Material3MenuItemData(
                                actionId = R.string.add_to_playlist,
                                title = { Text(text = stringResource(R.string.add_to_playlist)) },
                                description = {
                                    Text(text = stringResource(R.string.add_to_playlist_desc))
                                },
                                icon = {
                                    Icon(
                                        painter = painterResource(R.drawable.playlist_add),
                                        contentDescription = null,
                                    )
                                },
                                onClick = { showChoosePlaylistDialog = true },
                            ),
                        )
                    )
                    addAll(
                        buildList {
                            if (menuSongs.isNotEmpty()) {
                                add(
                                    when (downloadState) {
                                        Download.STATE_COMPLETED -> {
                                            Material3MenuItemData(
                                                actionId = R.string.remove_download,
                                                title = {
                                                    Text(
                                                        text =
                                                            stringResource(R.string.remove_download)
                                                    )
                                                },
                                                icon = {
                                                    Icon(
                                                        painter =
                                                            painterResource(R.drawable.offline),
                                                        contentDescription = null,
                                                    )
                                                },
                                                onClick = { showRemoveDownloadDialog = true },
                                            )
                                        }

                                        Download.STATE_QUEUED,
                                        Download.STATE_DOWNLOADING -> {
                                            Material3MenuItemData(
                                                actionId = R.string.downloading,
                                                title = {
                                                    Text(
                                                        text =
                                                            stringResource(
                                                                R.string.folio_cancel_download
                                                            )
                                                    )
                                                },
                                                icon = {
                                                    CircularProgressIndicator(
                                                        modifier = Modifier.size(24.dp),
                                                        strokeWidth = 2.dp,
                                                    )
                                                },
                                                onClick = { showRemoveDownloadDialog = true },
                                            )
                                        }

                                        else -> {
                                            Material3MenuItemData(
                                                actionId = R.string.action_download,
                                                title = {
                                                    Text(
                                                        text =
                                                            stringResource(R.string.action_download)
                                                    )
                                                },
                                                description = {
                                                    Text(
                                                        text =
                                                            stringResource(R.string.download_desc)
                                                    )
                                                },
                                                icon = {
                                                    Icon(
                                                        painter =
                                                            painterResource(R.drawable.download),
                                                        contentDescription = null,
                                                    )
                                                },
                                                onClick = {
                                                    menuSongs.forEach { song ->
                                                        val downloadRequest =
                                                            DownloadRequest.Builder(
                                                                    song.id,
                                                                    song.id.toUri(),
                                                                )
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
                                                },
                                            )
                                        }
                                    }
                                )
                            }
                            add(
                                Material3MenuItemData(
                                    actionId = R.string.export_playlist,
                                    title = {
                                        Text(text = stringResource(R.string.export_playlist))
                                    },
                                    icon = {
                                        Icon(
                                            painter = painterResource(R.drawable.share),
                                            contentDescription = null,
                                        )
                                    },
                                    onClick = { showExportDialog = true },
                                )
                            )
                            add(
                                Material3MenuItemData(
                                    actionId = R.string.share,
                                    title = { Text(text = stringResource(R.string.share)) },
                                    description = {
                                        Text(text = stringResource(R.string.share_desc))
                                    },
                                    icon = {
                                        Icon(
                                            painter = painterResource(R.drawable.share),
                                            contentDescription = null,
                                        )
                                    },
                                    onClick = {
                                        val intent =
                                            Intent().apply {
                                                action = Intent.ACTION_SEND
                                                type = "text/plain"
                                                putExtra(Intent.EXTRA_TEXT, playlist.shareLink)
                                            }
                                        context.startActivity(Intent.createChooser(intent, null))
                                        onDismiss()
                                    },
                                )
                            )
                            if (canSelect) {
                                add(
                                    Material3MenuItemData(
                                        actionId = R.string.select,
                                        title = { Text(text = stringResource(R.string.select)) },
                                        icon = {
                                            Icon(
                                                painter = painterResource(R.drawable.select_all),
                                                contentDescription = null,
                                            )
                                        },
                                        onClick = {
                                            onDismiss()
                                            selectAction()
                                        },
                                    )
                                )
                            }
                        }
                    )
                    add(
                        folioLibraryAction(
                            saved = dbPlaylist?.playlist?.bookmarkedAt != null,
                            onClick = {
                                val isCurrentlySaved = dbPlaylist?.playlist?.bookmarkedAt != null
                                if (dbPlaylist?.playlist == null) {
                                    database.transaction {
                                        val playlistEntity =
                                            PlaylistEntity(
                                                    name = playlist.title,
                                                    browseId = playlist.id,
                                                    thumbnailUrl = playlist.thumbnail,
                                                    isEditable = playlist.isEditable,
                                                    remoteSongCount =
                                                        playlist.songCountText?.let {
                                                            Regex("""\d+""")
                                                                .find(it)
                                                                ?.value
                                                                ?.toIntOrNull()
                                                        },
                                                    playEndpointParams =
                                                        playlist.playEndpoint?.params,
                                                    shuffleEndpointParams =
                                                        playlist.shuffleEndpoint?.params,
                                                    radioEndpointParams =
                                                        playlist.radioEndpoint?.params,
                                                )
                                                .toggleLike()
                                        insert(playlistEntity)
                                    }
                                } else {
                                    database.transaction {
                                        val currentPlaylist = dbPlaylist!!.playlist
                                        update(currentPlaylist, playlist)
                                        update(currentPlaylist.toggleLike())
                                    }
                                }
                                coroutineScope.launch(Dispatchers.IO) {
                                    if (!isCurrentlySaved) {
                                        val playlistEntity =
                                            database
                                                .playlistByBrowseId(playlist.id)
                                                .first()
                                                ?.playlist
                                        if (playlistEntity != null) {
                                            menuSongs
                                                .ifEmpty {
                                                    YouTube.playlist(playlist.id)
                                                        .completed()
                                                        .getOrNull()
                                                        ?.songs
                                                        .orEmpty()
                                                }
                                                .map { it.toMediaMetadata() }
                                                .onEach { database.transaction { insert(it) } }
                                                .mapIndexed { index, song ->
                                                    PlaylistSongMap(
                                                        songId = song.id,
                                                        playlistId = playlistEntity.id,
                                                        position = index,
                                                        setVideoId = song.setVideoId,
                                                    )
                                                }
                                                .forEach { database.transaction { insert(it) } }
                                        }
                                    }
                                    if (playlist.isPodcast) {
                                        YouTube.savePodcast(playlist.id, !isCurrentlySaved)
                                            .onSuccess {
                                                timber.log.Timber.d(
                                                    "[PODCAST_SAVE] savePodcast API success for ${playlist.id}"
                                                )
                                            }
                                            .onFailure { e ->
                                                timber.log.Timber.e(
                                                    e,
                                                    "[PODCAST_SAVE] savePodcast API failed for ${playlist.id}",
                                                )
                                                withContext(Dispatchers.Main) {
                                                    android.widget.Toast.makeText(
                                                            context,
                                                            if (isCurrentlySaved)
                                                                R.string.error_podcast_unsubscribe
                                                            else R.string.error_podcast_subscribe,
                                                            android.widget.Toast.LENGTH_SHORT,
                                                        )
                                                        .show()
                                                }
                                            }
                                    }
                                }
                            },
                        )
                    )
                }
                .filter {
                    showPageControls ||
                        it.actionId !in
                            setOf(
                                R.string.action_download,
                                R.string.remove_download,
                                R.string.downloading,
                                R.string.add_to_library,
                            )
                }
                .map { action ->
                    if (
                        menuSongs.isEmpty() &&
                            action.actionId in
                                setOf(
                                    R.string.play_next,
                                    R.string.add_to_queue,
                                    R.string.add_to_playlist,
                                    R.string.action_download,
                                )
                    )
                        action.copy(onClick = null)
                    else action
                },
        visible =
            !showRenameDialog &&
                !showDeleteDialog &&
                !showChoosePlaylistDialog &&
                !showImportPlaylistDialog &&
                !showErrorPlaylistAddDialog &&
                !showRemoveDownloadDialog &&
                !showExportDialog,
        onDismiss = onDismiss,
        retainOpenActions =
            setOf(
                R.string.edit,
                R.string.delete,
                R.string.add_to_playlist,
                R.string.remove_download,
                R.string.downloading,
                R.string.export_playlist,
            ),
    )

    if (showExportDialog) {
        ExportDialog(
            onDismiss = { showExportDialog = false },
            onShare = { format ->
                coroutineScope.launch {
                    val ytSongs =
                        if (menuSongs.isEmpty()) {
                            withContext(Dispatchers.IO) {
                                YouTube.playlist(playlist.id)
                                    .completed()
                                    .getOrNull()
                                    ?.songs
                                    .orEmpty()
                            }
                        } else {
                            menuSongs
                        }

                    val result =
                        when (format) {
                            "csv" -> exportYouTubePlaylistAsCSV(context, playlist.title, ytSongs)
                            "m3u" -> exportYouTubePlaylistAsM3U(context, playlist.title, ytSongs)
                            else -> Result.failure(IllegalArgumentException("Unknown format"))
                        }
                    result
                        .onSuccess { file ->
                            val uri = getExportFileUri(context, file)
                            val mime = if (format == "csv") "text/csv" else "audio/x-mpegurl"
                            shareFile(context, uri, mime)
                        }
                        .onFailure {
                            Toast.makeText(context, R.string.export_failed, Toast.LENGTH_SHORT)
                                .show()
                        }
                }
                onDismiss()
            },
            onSave = { format ->
                coroutineScope.launch {
                    val ytSongs =
                        if (menuSongs.isEmpty()) {
                            withContext(Dispatchers.IO) {
                                YouTube.playlist(playlist.id)
                                    .completed()
                                    .getOrNull()
                                    ?.songs
                                    .orEmpty()
                            }
                        } else {
                            menuSongs
                        }

                    val export =
                        when (format) {
                            "csv" -> exportYouTubePlaylistAsCSV(context, playlist.title, ytSongs)
                            "m3u" -> exportYouTubePlaylistAsM3U(context, playlist.title, ytSongs)
                            else -> Result.failure(IllegalArgumentException("Unknown format"))
                        }
                    export
                        .onSuccess { file ->
                            val mime = if (format == "csv") "text/csv" else "audio/x-mpegurl"
                            val save = saveToPublicDocuments(context, file, mime)
                            save
                                .onSuccess {
                                    Toast.makeText(
                                            context,
                                            R.string.export_success,
                                            Toast.LENGTH_SHORT,
                                        )
                                        .show()
                                }
                                .onFailure {
                                    Toast.makeText(
                                            context,
                                            R.string.export_failed,
                                            Toast.LENGTH_SHORT,
                                        )
                                        .show()
                                }
                        }
                        .onFailure {
                            Toast.makeText(context, R.string.export_failed, Toast.LENGTH_SHORT)
                                .show()
                        }
                }
                onDismiss()
            },
        )
    }
}

private fun shareFile(context: android.content.Context, uri: android.net.Uri, mimeType: String) {
    val shareIntent =
        Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    context.startActivity(
        Intent.createChooser(shareIntent, context.getString(R.string.export_playlist))
    )
}
