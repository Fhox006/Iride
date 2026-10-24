/** Metrolist Project (C) 2026 Licensed under GPL-3.0 | See git history for contributors */
package com.metrolist.music.ui.menu

import android.annotation.SuppressLint
import android.content.res.Configuration
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.DownloadRequest
import androidx.media3.exoplayer.offline.DownloadService
import androidx.navigation.NavController
import com.metrolist.innertube.YouTube
import com.metrolist.innertube.models.AlbumItem
import com.metrolist.music.LocalDatabase
import com.metrolist.music.LocalDownloadUtil
import com.metrolist.music.LocalListenTogetherManager
import com.metrolist.music.LocalPlayerConnection
import com.metrolist.music.R
import com.metrolist.music.constants.ListItemHeight
import com.metrolist.music.constants.ListThumbnailSize
import com.metrolist.music.db.entities.Song
import com.metrolist.music.extensions.toMediaItem
import com.metrolist.music.playback.ExoDownloadService
import com.metrolist.music.ui.component.IrideFolioContextMenu
import com.metrolist.music.ui.component.ListDialog
import com.metrolist.music.ui.component.Material3MenuItemData
import com.metrolist.music.ui.component.SongListItem
import com.metrolist.music.ui.component.folioLibraryAction
import com.metrolist.music.ui.component.folioMenuArtwork
import com.metrolist.music.ui.component.rememberMenuActionScope
import com.metrolist.music.utils.reportException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import com.metrolist.music.ui.component.MenuActionReadiness

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("MutableCollectionMutableState")
@Composable
fun YouTubeAlbumMenu(albumItem: AlbumItem, navController: NavController, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val database = LocalDatabase.current
    val downloadUtil = LocalDownloadUtil.current
    val playerConnection = LocalPlayerConnection.current ?: return
    val listenTogetherManager = LocalListenTogetherManager.current
    val isGuest = listenTogetherManager?.isInRoom == true && !listenTogetherManager.isHost
    val album by database.albumWithSongs(albumItem.id).collectAsState(initial = null)
    val coroutineScope = rememberMenuActionScope()
    var albumLoadFailed by remember(albumItem.id) { mutableStateOf(false) }

    LaunchedEffect(albumItem.id) {
        try {
            val cached = database.albumWithSongs(albumItem.id).first()
            if (cached == null) {
                val page = YouTube.album(albumItem.id).getOrThrow()
                database.withTransaction { insert(page) }
                albumLoadFailed = page.songs.isEmpty()
            } else {
                albumLoadFailed = cached.songs.isEmpty()
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            albumLoadFailed = true
            reportException(error)
        }
    }

    var downloadState by remember { mutableIntStateOf(Download.STATE_STOPPED) }

    LaunchedEffect(album) {
        val songs = album?.songs?.map { it.id } ?: return@LaunchedEffect
        downloadUtil.downloads.collect { downloads ->
            downloadState =
                if (songs.all { downloads[it]?.state == Download.STATE_COMPLETED }) {
                    Download.STATE_COMPLETED
                } else if (
                    songs.all {
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

    var showChoosePlaylistDialog by rememberSaveable { mutableStateOf(false) }

    var showErrorPlaylistAddDialog by rememberSaveable { mutableStateOf(false) }

    val notAddedList by remember { mutableStateOf(mutableListOf<Song>()) }

    AddToPlaylistDialog(
        isVisible = showChoosePlaylistDialog,
        onGetSong = { playlist ->
            coroutineScope.launch(Dispatchers.IO) {
                playlist.playlist.browseId?.let { playlistId ->
                    album?.album?.playlistId?.let { addPlaylistId ->
                        YouTube.addPlaylistToPlaylist(playlistId, addPlaylistId)
                    }
                }
            }
            album?.songs?.map { it.id }.orEmpty()
        },
        onGetSongIds = { album?.songs?.map { it.id }.orEmpty() },
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

            items(notAddedList) { song -> SongListItem(song = song) }
        }
    }

    var showSelectArtistDialog by rememberSaveable { mutableStateOf(false) }

    if (showSelectArtistDialog) {
        ListDialog(onDismiss = { showSelectArtistDialog = false }) {
            items(
                items =
                    album
                        ?.artists
                        .orEmpty()
                        .filter {
                            it.id !=
                                navController.currentBackStackEntry
                                    ?.arguments
                                    ?.getString("artistId")
                        }
                        .distinctBy { it.id },
                key = { "menu_yt_album_artist_${it.id}" },
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
                    Box(
                        contentAlignment = Alignment.CenterStart,
                        modifier =
                            Modifier.fillParentMaxWidth()
                                .height(ListItemHeight)
                                .clickable {
                                    showSelectArtistDialog = false
                                    onDismiss()
                                    navController.navigate("artist/${artist.id}")
                                }
                                .padding(horizontal = 24.dp),
                    ) {
                        Text(
                            text = artist.name,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }

    Spacer(modifier = Modifier.height(12.dp))

    val configuration = LocalConfiguration.current
    val isPortrait = configuration.orientation == Configuration.ORIENTATION_PORTRAIT

    val currentArtistId = navController.currentBackStackEntry?.arguments?.getString("artistId")
    val homeArtistId =
        if (navController.currentBackStackEntry?.destination?.route == "home")
            albumItem.artists?.firstOrNull()?.id
        else null
    val artists =
        albumItem.artists.orEmpty().filter {
            it.id != null && it.id != currentArtistId && it.id != homeArtistId
        }
    IrideFolioContextMenu(
        items =
            buildList {
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
                                        album
                                            ?.songs
                                            ?.map { it.toMediaItem() }
                                            ?.let(playerConnection::playNext)
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
                                        album
                                            ?.songs
                                            ?.map { it.toMediaItem() }
                                            ?.let(playerConnection::addToQueue)
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
                        listOf(
                            when (downloadState) {
                                Download.STATE_COMPLETED -> {
                                    Material3MenuItemData(
                                        actionId = R.string.remove_download,
                                        title = {
                                            Text(text = stringResource(R.string.remove_download))
                                        },
                                        icon = {
                                            Icon(
                                                painter = painterResource(R.drawable.offline),
                                                contentDescription = null,
                                            )
                                        },
                                        onClick = {
                                            album?.songs?.forEach { song ->
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

                                Download.STATE_QUEUED,
                                Download.STATE_DOWNLOADING -> {
                                    Material3MenuItemData(
                                        actionId = R.string.downloading,
                                        title = {
                                            Text(
                                                text =
                                                    stringResource(R.string.folio_cancel_download)
                                            )
                                        },
                                        icon = {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(24.dp),
                                                strokeWidth = 2.dp,
                                            )
                                        },
                                        onClick = {
                                            album?.songs?.forEach { song ->
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
                                        title = {
                                            Text(text = stringResource(R.string.action_download))
                                        },
                                        description = {
                                            Text(text = stringResource(R.string.download_desc))
                                        },
                                        icon = {
                                            Icon(
                                                painter = painterResource(R.drawable.download),
                                                contentDescription = null,
                                            )
                                        },
                                        onClick = {
                                            album?.songs?.forEach { song ->
                                                val downloadRequest =
                                                    DownloadRequest.Builder(
                                                            song.id,
                                                            song.id.toUri(),
                                                        )
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
                        )
                    )
                    if (artists.isNotEmpty())
                        addAll(
                            listOf(
                                Material3MenuItemData(
                                    actionId = R.string.view_artist,
                                    title = {
                                        Text(
                                            stringResource(
                                                if (currentArtistId != null || homeArtistId != null)
                                                    R.string.song_menu_view_collaborators
                                                else if (artists.size > 1)
                                                    R.string.song_menu_view_artists
                                                else R.string.view_artist
                                            )
                                        )
                                    },
                                    description = { Text(text = artists.joinToString { it.name }) },
                                    icon = {
                                        folioMenuArtwork(
                                            album
                                                ?.artists
                                                ?.firstOrNull { it.id == artists.firstOrNull()?.id }
                                                ?.thumbnailUrl,
                                            true,
                                            R.drawable.artist,
                                        )
                                    },
                                    onClick = {
                                        if (artists.size == 1) {
                                            navController.navigate("artist/${artists[0].id}")
                                            onDismiss()
                                        } else {
                                            showSelectArtistDialog = true
                                        }
                                    },
                                )
                            )
                        )
                    add(
                        folioLibraryAction(
                            saved = album?.album?.bookmarkedAt != null,
                            onClick = {
                                database.query { album?.album?.toggleLike()?.let(::update) }
                            },
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
                                    R.string.add_to_library,
                                )
                    )
                        action.copy(readiness = when {
                            action.actionId == R.string.add_to_library && album != null -> MenuActionReadiness.Ready
                            !album?.songs.isNullOrEmpty() -> MenuActionReadiness.Ready
                            albumLoadFailed -> MenuActionReadiness.Unavailable
                            else -> MenuActionReadiness.Pending
                        })
                    else action
                },
        visible =
            !showChoosePlaylistDialog && !showSelectArtistDialog && !showErrorPlaylistAddDialog,
        onDismiss = onDismiss,
        retainOpenActions = setOf(R.string.add_to_playlist, R.string.view_artist),
    )
}
