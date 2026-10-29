/** Metrolist Project (C) 2026 Licensed under GPL-3.0 | See git history for contributors */
package com.metrolist.music.ui.menu

import androidx.compose.foundation.shape.CircleShape


import android.annotation.SuppressLint
import android.content.Intent
import android.content.res.Configuration
import android.widget.Toast
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.media3.exoplayer.offline.Download.STATE_COMPLETED
import androidx.media3.exoplayer.offline.Download.STATE_DOWNLOADING
import androidx.media3.exoplayer.offline.Download.STATE_QUEUED
import androidx.media3.exoplayer.offline.Download.STATE_STOPPED
import androidx.media3.exoplayer.offline.DownloadRequest
import androidx.media3.exoplayer.offline.DownloadService
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import com.metrolist.innertube.YouTube
import com.metrolist.music.LocalDatabase
import com.metrolist.music.LocalDownloadUtil
import com.metrolist.music.LocalListenTogetherManager
import com.metrolist.music.LocalPlayerConnection
import com.metrolist.music.R
import com.metrolist.music.constants.AdvancedModeKey
import com.metrolist.music.constants.ListItemHeight
import com.metrolist.music.constants.ListThumbnailSize
import com.metrolist.music.db.entities.Album
import com.metrolist.music.db.entities.Song
import com.metrolist.music.extensions.toMediaItem
import com.metrolist.music.playback.ExoDownloadService
import com.metrolist.music.ui.component.IrideFolioContextMenu
import com.metrolist.music.ui.component.ListDialog
import com.metrolist.music.ui.component.ListItem
import com.metrolist.music.ui.component.Material3MenuItemData
import com.metrolist.music.ui.component.MenuActionReadiness
import com.metrolist.music.ui.component.SongListItem
import com.metrolist.music.ui.component.folioLibraryAction
import com.metrolist.music.ui.component.folioMenuAction
import com.metrolist.music.ui.component.folioMenuArtwork
import com.metrolist.music.ui.component.rememberMenuActionScope
import com.metrolist.music.utils.PlaylistExporter
import com.metrolist.music.utils.getExportFileUri
import com.metrolist.music.utils.rememberPreference
import com.metrolist.music.utils.saveToPublicDocuments
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException
import com.metrolist.music.utils.reportException

@SuppressLint("MutableCollectionMutableState")
@Composable
fun AlbumMenu(originalAlbum: Album, navController: NavController, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val database = LocalDatabase.current
    val downloadUtil = LocalDownloadUtil.current
    val playerConnection = LocalPlayerConnection.current ?: return
    val listenTogetherManager = LocalListenTogetherManager.current
    val isGuest = listenTogetherManager?.isInRoom == true && !listenTogetherManager.isHost
    val scope = rememberMenuActionScope()
    val libraryAlbum by database.album(originalAlbum.id).collectAsState(initial = originalAlbum)
    val album = libraryAlbum ?: originalAlbum
    val currentArtistId = navController.currentBackStackEntry?.arguments?.getString("artistId")
    val homeArtistId =
        if (navController.currentBackStackEntry?.destination?.route == "home")
            album.artists.firstOrNull()?.id
        else null
    val menuArtists = album.artists.filter { it.id != currentArtistId && it.id != homeArtistId }
    var songs by remember(album.id) { mutableStateOf(emptyList<Song>()) }
    var songsResolved by remember(album.id) { mutableStateOf(false) }

    val coroutineScope = rememberMenuActionScope()

    LaunchedEffect(album.id) {
        try {
            database.albumSongs(album.id).collect {
                songs = it
                songsResolved = true
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            songsResolved = true
            reportException(error)
        }
    }

    var otherVersions by remember(album.id) {
        mutableStateOf(AlbumOtherVersionsCache.get(album.id).orEmpty())
    }
    LaunchedEffect(album.id) {
        AlbumOtherVersionsCache.fetch(album.id)?.let { otherVersions = it }
    }

    var downloadState by remember { mutableIntStateOf(STATE_STOPPED) }

    LaunchedEffect(songs) {
        if (songs.isEmpty()) return@LaunchedEffect
        downloadUtil.downloads.collect { downloads ->
            downloadState =
                if (songs.all { downloads[it.id]?.state == STATE_COMPLETED }) {
                    STATE_COMPLETED
                } else if (
                    songs.all {
                        downloads[it.id]?.state == STATE_QUEUED ||
                            downloads[it.id]?.state == STATE_DOWNLOADING ||
                            downloads[it.id]?.state == STATE_COMPLETED
                    }
                ) {
                    STATE_DOWNLOADING
                } else {
                    STATE_STOPPED
                }
        }
    }

    var refetchIconDegree by remember { mutableFloatStateOf(0f) }

    val rotationAnimation by
        animateFloatAsState(
            targetValue = refetchIconDegree,
            animationSpec = tween(durationMillis = 800),
            label = "",
        )

    val (advancedMode) = rememberPreference(AdvancedModeKey, defaultValue = false)

    var showChoosePlaylistDialog by rememberSaveable { mutableStateOf(false) }

    var showSelectArtistDialog by rememberSaveable { mutableStateOf(false) }

    var showErrorPlaylistAddDialog by rememberSaveable { mutableStateOf(false) }

    val notAddedList by remember { mutableStateOf(mutableListOf<Song>()) }

    AddToPlaylistDialog(
        isVisible = showChoosePlaylistDialog,
        onGetSong = { playlist ->
            coroutineScope.launch(Dispatchers.IO) {
                playlist.playlist.browseId?.let { playlistId ->
                    album.album.playlistId?.let { addPlaylistId ->
                        YouTube.addPlaylistToPlaylist(playlistId, addPlaylistId)
                    }
                }
            }
            songs.map { it.id }
        },
        onGetSongIds = { songs.map { it.id } },
        onDismiss = { showChoosePlaylistDialog = false },
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
                    title = stringResource(R.string.already_in_playlist),
                    thumbnailContent = {
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

            items(notAddedList) { song -> SongListItem(song = song) }
        }
    }

    if (showSelectArtistDialog) {
        ListDialog(onDismiss = { showSelectArtistDialog = false }) {
            items(
                items = menuArtists.distinctBy { it.id },
                key = { "menu_album_artist_${it.id}" },
            ) { artist ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier =
                        Modifier.height(ListItemHeight)
                            .clickable {
                                navController.navigate("artist/${artist.id}")
                                showSelectArtistDialog = false
                                onDismiss()
                            }
                            .padding(horizontal = 12.dp),
                ) {
                    Box(modifier = Modifier.padding(8.dp), contentAlignment = Alignment.Center) {
                        AsyncImage(
                            model = artist.thumbnailUrl,
                            contentDescription = null,
                            modifier = Modifier.size(ListThumbnailSize).clip(CircleShape),
                        )
                    }
                    Text(
                        text = artist.name,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                    )
                }
            }
        }
    }

    val trackCount = if (songs.isNotEmpty()) songs.size else album.album.songCount

    val addToPlaylistItem =
        Material3MenuItemData(
            actionId = R.string.add_to_playlist,
            title = { Text(text = stringResource(R.string.add_to_playlist)) },
            description = { Text(text = stringResource(R.string.add_to_playlist_desc)) },
            icon = {
                Icon(painter = painterResource(R.drawable.playlist_add), contentDescription = null)
            },
            onClick = { showChoosePlaylistDialog = true },
        )

    val downloadItem =
        when (downloadState) {
            STATE_COMPLETED -> {
                Material3MenuItemData(
                    actionId = R.string.remove_download,
                    title = { Text(text = stringResource(R.string.remove_download)) },
                    icon = {
                        Icon(
                            painter = painterResource(R.drawable.offline),
                            contentDescription = null,
                        )
                    },
                    onClick = {
                        songs.forEach { song ->
                            DownloadService.sendRemoveDownload(
                                context,
                                ExoDownloadService::class.java,
                                song.id,
                                false,
                            )
                        }
                    },
                )
            }

            STATE_QUEUED,
            STATE_DOWNLOADING -> {
                Material3MenuItemData(
                    actionId = R.string.downloading,
                    title = { Text(text = stringResource(R.string.folio_cancel_download)) },
                    icon = {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp,
                        )
                    },
                    onClick = {
                        songs.forEach { song ->
                            DownloadService.sendRemoveDownload(
                                context,
                                ExoDownloadService::class.java,
                                song.id,
                                false,
                            )
                        }
                    },
                )
            }

            else -> {
                Material3MenuItemData(
                    actionId = R.string.action_download,
                    title = { Text(text = stringResource(R.string.action_download)) },
                    description = { Text(text = stringResource(R.string.download_desc)) },
                    icon = {
                        Icon(
                            painter = painterResource(R.drawable.download),
                            contentDescription = null,
                        )
                    },
                    onClick = {
                        songs.forEach { song ->
                            val downloadRequest =
                                DownloadRequest.Builder(song.id, song.id.toUri())
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
                    },
                )
            }
        }

    val onArtistClick: () -> Unit = {
        if (menuArtists.size == 1) {
            navController.navigate("artist/${menuArtists[0].id}")
            onDismiss()
        } else {
            showSelectArtistDialog = true
        }
    }
    val onOtherVersionClick: () -> Unit = {
        otherVersions.firstOrNull()?.let { navController.navigate("album/${it.id}") }
        onDismiss()
    }

    val configuration = LocalConfiguration.current
    val isPortrait = configuration.orientation == Configuration.ORIENTATION_PORTRAIT

    var showExportDialog by rememberSaveable { mutableStateOf(false) }
    IrideFolioContextMenu(
        items =
            buildList {
                    if (!isGuest) {
                        addAll(
                            listOf(
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
                                        onDismiss()
                                        playerConnection.addToQueue(songs.map { it.toMediaItem() })
                                    },
                                )
                            )
                        )
                        add(
                            folioMenuAction(
                                R.string.play_next,
                                R.drawable.playlist_play,
                                { playerConnection.playNext(songs.map { it.toMediaItem() }) },
                            )
                        )
                    }
                    addAll(listOf(downloadItem, addToPlaylistItem))
                    add(
                        folioLibraryAction(
                            saved = album.album.bookmarkedAt != null,
                            onClick = { database.query { update(album.album.toggleLike()) } },
                        )
                    )
                    if (menuArtists.isNotEmpty())
                        add(
                            folioMenuAction(
                                    R.string.view_artist,
                                    R.drawable.artist,
                                    onArtistClick,
                                    text =
                                        stringResource(
                                            if (currentArtistId != null || homeArtistId != null)
                                                R.string.song_menu_view_collaborators
                                            else if (menuArtists.size > 1)
                                                R.string.song_menu_view_artists
                                            else R.string.view_artist
                                        ),
                                )
                                .copy(
                                    icon = {
                                        folioMenuArtwork(
                                            menuArtists.firstOrNull()?.thumbnailUrl,
                                            true,
                                            R.drawable.artist,
                                        )
                                    }
                                )
                        )
                    if (otherVersions.isNotEmpty())
                        add(
                            folioMenuAction(
                                R.string.other_versions,
                                R.drawable.album,
                                onOtherVersionClick,
                            )
                        )
                    add(
                        folioMenuAction(
                            R.string.share,
                            R.drawable.share,
                            {
                                context.startActivity(
                                    Intent.createChooser(
                                        Intent(Intent.ACTION_SEND).apply {
                                            type = "text/plain"
                                            putExtra(
                                                Intent.EXTRA_TEXT,
                                                if (album.id.startsWith("itunes:")) {
                                                    val parts = album.id.split(':')
                                                    "https://music.apple.com/${parts[1]}/album/${parts[2]}"
                                                } else "https://music.youtube.com/browse/${album.id}",
                                            )
                                        },
                                        null,
                                    )
                                )
                            },
                        )
                    )
                    if (advancedMode)
                        addAll(
                            listOf(
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
                                ),
                                Material3MenuItemData(
                                    actionId = R.string.refetch,
                                    title = { Text(text = stringResource(R.string.refetch)) },
                                    description = {
                                        Text(text = stringResource(R.string.refetch_desc))
                                    },
                                    icon = {
                                        Icon(
                                            painter = painterResource(R.drawable.sync),
                                            contentDescription = null,
                                            modifier =
                                                Modifier.graphicsLayer(
                                                    rotationZ = rotationAnimation
                                                ),
                                        )
                                    },
                                    onClick = {
                                        refetchIconDegree -= 360
                                        scope.launch(Dispatchers.IO) {
                                            YouTube.album(album.id).onSuccess {
                                                database.transaction {
                                                    update(album.album, it, album.artists)
                                                }
                                            }
                                        }
                                    },
                                ),
                            )
                        )
                }
                .map { action ->
                    if (
                        action.actionId in
                                setOf(
                                    R.string.play_next,
                                    R.string.add_to_queue,
                                    R.string.add_to_playlist,
                                    R.string.action_download,
                                )
                    )
                        action.copy(readiness = when {
                            songs.isNotEmpty() -> MenuActionReadiness.Ready
                            songsResolved -> MenuActionReadiness.Unavailable
                            else -> MenuActionReadiness.Pending
                        })
                    else action
                },
        visible =
            !showChoosePlaylistDialog &&
                !showSelectArtistDialog &&
                !showErrorPlaylistAddDialog &&
                !showExportDialog,
        onDismiss = onDismiss,
        retainOpenActions =
            setOf(R.string.add_to_playlist, R.string.view_artist, R.string.export_playlist),
    )

    val exportPlaylistStr = stringResource(R.string.export_playlist)

    if (showExportDialog) {
        ExportDialog(
            onDismiss = { showExportDialog = false },
            onShare = { format ->
                val playlistSongs =
                    songs.map { s ->
                        com.metrolist.music.db.entities.PlaylistSong(
                            map =
                                com.metrolist.music.db.entities.PlaylistSongMap(
                                    songId = s.id,
                                    playlistId = album.id,
                                    position = 0,
                                ),
                            song = s,
                        )
                    }
                val result =
                    when (format) {
                        "csv" ->
                            PlaylistExporter.exportPlaylistAsCSV(
                                context,
                                album.album.title,
                                playlistSongs,
                            )
                        "m3u" ->
                            PlaylistExporter.exportPlaylistAsM3U(
                                context,
                                album.album.title,
                                playlistSongs,
                            )
                        else -> Result.failure(IllegalArgumentException("Unknown format"))
                    }
                result
                    .onSuccess { file ->
                        val uri = getExportFileUri(context, file)
                        val mimeType = if (format == "csv") "text/csv" else "audio/x-mpegurl"
                        val shareIntent =
                            Intent(Intent.ACTION_SEND).apply {
                                type = mimeType
                                putExtra(Intent.EXTRA_STREAM, uri)
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                        context.startActivity(Intent.createChooser(shareIntent, exportPlaylistStr))
                    }
                    .onFailure {
                        Toast.makeText(context, R.string.export_failed, Toast.LENGTH_SHORT).show()
                    }
                showExportDialog = false
            },
            onSave = { format ->
                val playlistSongs =
                    songs.map { s ->
                        com.metrolist.music.db.entities.PlaylistSong(
                            map =
                                com.metrolist.music.db.entities.PlaylistSongMap(
                                    songId = s.id,
                                    playlistId = album.id,
                                    position = 0,
                                ),
                            song = s,
                        )
                    }
                val export =
                    when (format) {
                        "csv" ->
                            PlaylistExporter.exportPlaylistAsCSV(
                                context,
                                album.album.title,
                                playlistSongs,
                            )
                        "m3u" ->
                            PlaylistExporter.exportPlaylistAsM3U(
                                context,
                                album.album.title,
                                playlistSongs,
                            )
                        else -> Result.failure(IllegalArgumentException("Unknown format"))
                    }
                export
                    .onSuccess { file ->
                        val mimeType = if (format == "csv") "text/csv" else "audio/x-mpegurl"
                        val save = saveToPublicDocuments(context, file, mimeType)
                        save
                            .onSuccess {
                                Toast.makeText(context, R.string.export_success, Toast.LENGTH_SHORT)
                                    .show()
                            }
                            .onFailure {
                                Toast.makeText(context, R.string.export_failed, Toast.LENGTH_SHORT)
                                    .show()
                            }
                    }
                    .onFailure {
                        Toast.makeText(context, R.string.export_failed, Toast.LENGTH_SHORT).show()
                    }
                showExportDialog = false
            },
        )
    }
}
