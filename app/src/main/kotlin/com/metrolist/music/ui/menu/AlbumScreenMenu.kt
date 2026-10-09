package com.metrolist.music.ui.menu

import android.content.Intent
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.metrolist.music.LocalListenTogetherManager
import com.metrolist.music.LocalPlayerConnection
import com.metrolist.music.R
import com.metrolist.music.constants.AdvancedModeKey
import com.metrolist.music.db.entities.AlbumWithSongs
import com.metrolist.music.db.entities.PlaylistSong
import com.metrolist.music.db.entities.PlaylistSongMap
import com.metrolist.music.extensions.toMediaItem
import com.metrolist.music.models.toMediaMetadata
import com.metrolist.music.ui.component.IrideFolioMenuAction
import com.metrolist.music.utils.PlaylistExporter
import com.metrolist.music.utils.getExportFileUri
import com.metrolist.music.utils.rememberPreference
import com.metrolist.music.utils.saveToPublicDocuments
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
internal fun albumScreenMenuSections(
    albumWithSongs: AlbumWithSongs?,
    onRefetch: () -> Unit,
): List<List<IrideFolioMenuAction>> {
    val album = albumWithSongs ?: return emptyList()
    val playerConnection = LocalPlayerConnection.current ?: return emptyList()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val listenTogetherManager = LocalListenTogetherManager.current
    val isGuest = listenTogetherManager?.isInRoom == true && !listenTogetherManager.isHost
    val advancedMode by rememberPreference(AdvancedModeKey, defaultValue = false)
    var showPlaylistDialog by rememberSaveable(album.album.id) { mutableStateOf(false) }
    var showExportDialog by rememberSaveable(album.album.id) { mutableStateOf(false) }

    if (showPlaylistDialog) {
        AddToPlaylistDialog(
            isVisible = true,
            onGetSong = { album.songs.map { it.id } },
            onGetSongIds = { album.songs.map { it.id } },
            onDismiss = { showPlaylistDialog = false },
        )
    }

    val exportTitle = stringResource(R.string.export_playlist)
    val onExport: (String, Boolean) -> Unit = { format, share ->
        showExportDialog = false
        scope.launch {
            val file = withContext(Dispatchers.IO) {
                val songs = album.songs.mapIndexed { index, song ->
                    PlaylistSong(
                        map = PlaylistSongMap(
                            songId = song.id,
                            playlistId = album.album.id,
                            position = index,
                        ),
                        song = song,
                    )
                }
                when (format) {
                    "csv" -> PlaylistExporter.exportPlaylistAsCSV(context, album.album.title, songs)
                    "m3u" -> PlaylistExporter.exportPlaylistAsM3U(context, album.album.title, songs)
                    else -> Result.failure(IllegalArgumentException("Unknown format"))
                }.getOrNull()
            }
            if (file == null) {
                Toast.makeText(context, R.string.export_failed, Toast.LENGTH_SHORT).show()
                return@launch
            }
            val mimeType = if (format == "csv") "text/csv" else "audio/x-mpegurl"
            if (share) {
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = mimeType
                    putExtra(Intent.EXTRA_STREAM, getExportFileUri(context, file))
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(Intent.createChooser(intent, exportTitle))
            } else {
                val saved = withContext(Dispatchers.IO) {
                    saveToPublicDocuments(context, file, mimeType).isSuccess
                }
                Toast.makeText(
                    context,
                    if (saved) R.string.export_success else R.string.export_failed,
                    Toast.LENGTH_SHORT,
                ).show()
            }
        }
    }
    if (showExportDialog) {
        ExportDialog(
            onDismiss = { showExportDialog = false },
            onShare = { format -> onExport(format, true) },
            onSave = { format -> onExport(format, false) },
        )
    }

    return buildList {
        if (!isGuest && album.songs.isNotEmpty()) {
            add(
                listOf(
                    IrideFolioMenuAction(
                        label = stringResource(R.string.play_next),
                        icon = R.drawable.playlist_play,
                        onClick = { playerConnection.playNext(album.songs.map { it.toMediaItem() }) },
                    ),
                    IrideFolioMenuAction(
                        label = stringResource(R.string.add_to_queue),
                        icon = R.drawable.queue_music,
                        onClick = { playerConnection.addToQueue(album.songs.map { it.toMediaItem() }) },
                    ),

                )
            )
        }
        add(
            buildList {
                if (album.songs.isNotEmpty()) {
                    add(
                        IrideFolioMenuAction(
                            label = stringResource(R.string.add_to_playlist),
                            icon = R.drawable.playlist_add,
                            onClick = { showPlaylistDialog = true },
                        )
                    )
                }
                add(
                    IrideFolioMenuAction(
                        label = stringResource(R.string.share),
                        icon = R.drawable.share,
                        onClick = {
                            val link = album.album.playlistId?.let {
                                "https://music.youtube.com/playlist?list=$it"
                            } ?: "https://music.youtube.com/browse/${album.album.id}"
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, link)
                            }
                            context.startActivity(Intent.createChooser(intent, null))
                        },
                    )
                )
            }
        )
        if (advancedMode) {
            add(
                buildList {
                    if (album.songs.isNotEmpty()) {
                        add(
                            IrideFolioMenuAction(
                                label = exportTitle,
                                icon = R.drawable.share,
                                onClick = { showExportDialog = true },
                            )
                        )
                    }
                    add(
                        IrideFolioMenuAction(
                            label = stringResource(R.string.refetch),
                            icon = R.drawable.sync,
                            onClick = onRefetch,
                        )
                    )
                }
            )
        }
    }
}
