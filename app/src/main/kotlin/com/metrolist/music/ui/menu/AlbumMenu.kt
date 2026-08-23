/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.menu

import android.annotation.SuppressLint
import android.content.Intent
import android.content.res.Configuration
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
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
import com.metrolist.innertube.models.AlbumItem
import com.metrolist.music.LocalDatabase
import com.metrolist.music.LocalDownloadUtil
import com.metrolist.music.LocalListenTogetherManager
import com.metrolist.music.LocalPlayerConnection
import com.metrolist.music.R
import com.metrolist.music.constants.AdvancedModeKey
import com.metrolist.music.constants.ListItemHeight
import com.metrolist.music.constants.ListThumbnailSize
import com.metrolist.music.db.entities.Album
import com.metrolist.music.db.entities.ArtistEntity
import com.metrolist.music.db.entities.Song
import com.metrolist.music.extensions.toMediaItem
import com.metrolist.music.models.toMediaMetadata
import com.metrolist.music.playback.ExoDownloadService
import com.metrolist.music.playback.queues.ListQueue
import com.metrolist.music.ui.component.AlbumListItem
import com.metrolist.music.ui.component.ListDialog
import com.metrolist.music.ui.component.ListItem
import com.metrolist.music.ui.component.Material3MenuGroup
import com.metrolist.music.ui.component.Material3MenuItemData
import com.metrolist.music.ui.component.NewAction
import com.metrolist.music.ui.component.NewActionGrid
import com.metrolist.music.ui.component.SongListItem
import com.metrolist.music.ui.theme.SpaceMonoFontFamily
import com.metrolist.music.utils.PlaylistExporter
import com.metrolist.music.utils.getExportFileUri
import com.metrolist.music.utils.rememberPreference
import com.metrolist.music.utils.saveToPublicDocuments
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import sv.lib.squircleshape.SquircleShape

@SuppressLint("MutableCollectionMutableState")
@Composable
fun AlbumMenu(
    originalAlbum: Album,
    navController: NavController,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val database = LocalDatabase.current
    val downloadUtil = LocalDownloadUtil.current
    val playerConnection = LocalPlayerConnection.current ?: return
    val listenTogetherManager = LocalListenTogetherManager.current
    val isGuest = listenTogetherManager?.isInRoom == true && !listenTogetherManager.isHost
    val scope = rememberCoroutineScope()
    val libraryAlbum by database.album(originalAlbum.id).collectAsState(initial = originalAlbum)
    val album = libraryAlbum ?: originalAlbum
    var songs by remember {
        mutableStateOf(emptyList<Song>())
    }

    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        database.albumSongs(album.id).collect {
            songs = it
        }
    }

    // Fetched once per menu open (same call the "refetch" action already makes) so an
    // "Other version" tile can appear when YouTube lists an explicit/clean counterpart.
    var otherVersions by remember { mutableStateOf(emptyList<AlbumItem>()) }
    LaunchedEffect(album.id) {
        YouTube.album(album.id).onSuccess {
            otherVersions = it.otherVersions
        }
    }

    var downloadState by remember {
        mutableIntStateOf(STATE_STOPPED)
    }

    LaunchedEffect(songs) {
        if (songs.isEmpty()) return@LaunchedEffect
        downloadUtil.downloads.collect { downloads ->
            downloadState =
                if (songs.all { downloads[it.id]?.state == STATE_COMPLETED }) {
                    STATE_COMPLETED
                } else if (songs.all {
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

    val rotationAnimation by animateFloatAsState(
        targetValue = refetchIconDegree,
        animationSpec = tween(durationMillis = 800),
        label = "",
    )

    val (advancedMode) = rememberPreference(AdvancedModeKey, defaultValue = false)

    var showChoosePlaylistDialog by rememberSaveable {
        mutableStateOf(false)
    }

    var showSelectArtistDialog by rememberSaveable {
        mutableStateOf(false)
    }

    var showErrorPlaylistAddDialog by rememberSaveable {
        mutableStateOf(false)
    }

    val notAddedList by remember {
        mutableStateOf(mutableListOf<Song>())
    }

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
        onDismiss = {
            showChoosePlaylistDialog = false
        },
    )

    if (showErrorPlaylistAddDialog) {
        ListDialog(
            onDismiss = {
                showErrorPlaylistAddDialog = false
                onDismiss()
            },
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
                    modifier =
                        Modifier
                            .clickable { showErrorPlaylistAddDialog = false },
                )
            }

            items(notAddedList) { song ->
                SongListItem(song = song)
            }
        }
    }

    if (showSelectArtistDialog) {
        ListDialog(
            onDismiss = { showSelectArtistDialog = false },
        ) {
            items(
                items = album.artists.distinctBy { it.id },
                key = { "menu_album_artist_${it.id}" },
            ) { artist ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier =
                        Modifier
                            .height(ListItemHeight)
                            .clickable {
                                navController.navigate("artist/${artist.id}")
                                showSelectArtistDialog = false
                                onDismiss()
                            }.padding(horizontal = 12.dp),
                ) {
                    Box(
                        modifier = Modifier.padding(8.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        AsyncImage(
                            model = artist.thumbnailUrl,
                            contentDescription = null,
                            modifier =
                                Modifier
                                    .size(ListThumbnailSize)
                                    .clip(CircleShape),
                        )
                    }
                    Text(
                        text = artist.name,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier =
                            Modifier
                                .weight(1f)
                                .padding(horizontal = 8.dp),
                    )
                }
            }
        }
    }

    val trackCount = if (songs.isNotEmpty()) songs.size else album.album.songCount

    val addToPlaylistItem =
        Material3MenuItemData(
            title = { Text(text = stringResource(R.string.add_to_playlist)) },
            description = { Text(text = stringResource(R.string.add_to_playlist_desc)) },
            icon = {
                Icon(
                    painter = painterResource(R.drawable.playlist_add),
                    contentDescription = null,
                )
            },
            onClick = {
                showChoosePlaylistDialog = true
            },
        )

    val downloadItem =
        when (downloadState) {
            STATE_COMPLETED -> {
                Material3MenuItemData(
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

            STATE_QUEUED, STATE_DOWNLOADING -> {
                Material3MenuItemData(
                    title = { Text(text = stringResource(R.string.downloading)) },
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
                                DownloadRequest
                                    .Builder(song.id, song.id.toUri())
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
        if (album.artists.size == 1) {
            navController.navigate("artist/${album.artists[0].id}")
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

    LazyColumn(
        contentPadding =
            PaddingValues(
                start = 0.dp,
                top = 0.dp,
                end = 0.dp,
                bottom = 8.dp + WindowInsets.systemBars.asPaddingValues().calculateBottomPadding(),
            ),
    ) {
        item {
            Column(modifier = Modifier.padding(horizontal = 4.dp)) {
                NewIrideAlbumMenuHeader(
                    album = album,
                    trackCount = trackCount,
                    onDismiss = onDismiss,
                )
                Spacer(modifier = Modifier.height(20.dp))
                var isEditingNote by rememberSaveable(album.id) { mutableStateOf(false) }
                BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                    val density = LocalDensity.current
                    val textMeasurer = rememberTextMeasurer()
                    val standardCoverFraction = 0.48f
                    val minCoverFraction = standardCoverFraction * 0.6f
                    val noteTitle = album.album.noteTitle.orEmpty()
                    val totalWidthPx = with(density) { maxWidth.toPx() }
                    val titleMeasureStyle =
                        MaterialTheme.typography.bodyLarge.copy(
                            fontFamily = SpaceMonoFontFamily,
                            fontWeight = FontWeight.Bold,
                        )
                    val coverFraction =
                        remember(noteTitle, totalWidthPx, titleMeasureStyle) {
                            if (noteTitle.isBlank() || totalWidthPx <= 0f) {
                                standardCoverFraction
                            } else {
                                val titleWidthPx =
                                    textMeasurer
                                        .measure(
                                            text = AnnotatedString(noteTitle),
                                            style = titleMeasureStyle,
                                            maxLines = 1,
                                        ).size.width
                                        .toFloat()
                                val spacingPx = with(density) { 14.dp.toPx() }
                                val innerPaddingPx = with(density) { 32.dp.toPx() }
                                var fraction = standardCoverFraction
                                while (fraction > minCoverFraction) {
                                    val noteBoxWidthPx = totalWidthPx * (1f - fraction) - spacingPx - innerPaddingPx
                                    if (titleWidthPx <= noteBoxWidthPx) break
                                    fraction -= 0.04f
                                }
                                fraction.coerceAtLeast(minCoverFraction)
                            }
                        }
                    val coverWeight by animateFloatAsState(
                        targetValue = if (isEditingNote) 0.0001f else coverFraction,
                        animationSpec = tween(280),
                        label = "coverWeight",
                    )
                    val noteWeight by animateFloatAsState(
                        targetValue = if (isEditingNote) 1f else (1f - coverFraction),
                        animationSpec = tween(280),
                        label = "noteWeight",
                    )
                    val spacingPx = with(density) { 14.dp.toPx() }
                    val coverHeightDp =
                        with(density) { ((totalWidthPx - spacingPx) * coverFraction).toDp() }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        AnimatedVisibility(
                            visible = !isEditingNote,
                            enter = fadeIn(tween(280, delayMillis = 80)),
                            exit = fadeOut(tween(160)),
                            modifier = Modifier.weight(coverWeight),
                        ) {
                            AsyncImage(
                                model = album.album.thumbnailUrl,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .aspectRatio(1f)
                                        .clip(SquircleShape(radius = 8.dp, cornerSmoothing = 0.48f)),
                            )
                        }
                        AddNoteBox(
                            key = album.id,
                            initialTitle = album.album.noteTitle.orEmpty(),
                            initialRating = album.album.noteRating,
                            initialText = album.album.noteText.orEmpty(),
                            onPersist = { noteTitleValue, noteRatingValue, noteTextValue ->
                                database.query {
                                    update(
                                        album.album.copy(
                                            noteTitle = noteTitleValue.trim().ifBlank { null },
                                            noteRating = noteRatingValue,
                                            noteText = noteTextValue.trim().ifBlank { null },
                                        ),
                                    )
                                }
                            },
                            isEditing = isEditingNote,
                            onEditingChange = { isEditingNote = it },
                            modifier =
                                Modifier
                                    .weight(noteWeight)
                                    .then(if (isEditingNote) Modifier else Modifier.height(coverHeightDp)),
                        )
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(20.dp)) }

        if (!isGuest) {
            item {
                NewActionGrid(
                    actions =
                        listOf(
                            NewAction(
                                icon = {
                                    Icon(
                                        painter = painterResource(R.drawable.shuffle),
                                        contentDescription = null,
                                        modifier = Modifier.size(28.dp),
                                        tint = Color.White.copy(alpha = 0.85f),
                                    )
                                },
                                text = stringResource(R.string.shuffle),
                                onClick = {
                                    onDismiss()
                                    if (songs.isNotEmpty()) {
                                        album.album.playlistId?.let { playlistId ->
                                            playerConnection.service.getAutomix(playlistId)
                                        }
                                        playerConnection.playQueue(
                                            ListQueue(
                                                title = album.album.title,
                                                items = songs.shuffled().map(Song::toMediaItem),
                                            ),
                                        )
                                    }
                                },
                            ),
                            NewAction(
                                icon = {
                                    Icon(
                                        painter = painterResource(R.drawable.playlist_play),
                                        contentDescription = null,
                                        modifier = Modifier.size(28.dp),
                                        tint = Color.White.copy(alpha = 0.85f),
                                    )
                                },
                                text = stringResource(R.string.swipe_label_next).lowercase().replaceFirstChar { it.uppercase() },
                                onClick = {
                                    onDismiss()
                                    playerConnection.playNext(songs.map { it.toMediaItem() })
                                },
                            ),
                            NewAction(
                                icon = {
                                    Icon(
                                        painter = painterResource(R.drawable.radio),
                                        contentDescription = null,
                                        modifier = Modifier.size(28.dp),
                                        tint = Color.White.copy(alpha = 0.85f),
                                    )
                                },
                                text = stringResource(R.string.radio),
                                onClick = {
                                    onDismiss()
                                    songs.firstOrNull()?.let { seed ->
                                        playerConnection.startRadioForSong(seed.toMediaMetadata())
                                    }
                                },
                            ),
                        ),
                    modifier = Modifier.padding(horizontal = 4.dp),
                )
            }
            item { Spacer(modifier = Modifier.height(16.dp)) }

            item {
                Material3MenuGroup(
                    items =
                        listOf(
                            Material3MenuItemData(
                                title = { Text(text = stringResource(R.string.add_to_queue)) },
                                description = { Text(text = stringResource(R.string.add_to_queue_desc)) },
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
                            ),
                        ),
                )
            }
            item { Spacer(modifier = Modifier.height(12.dp)) }
        }

        item {
            Material3MenuGroup(items = listOf(downloadItem, addToPlaylistItem))
        }

        item { Spacer(modifier = Modifier.height(12.dp)) }

        item {
            ArtistOtherVersionSwitchRow(
                artists = album.artists,
                otherVersion = otherVersions.firstOrNull(),
                onArtistClick = onArtistClick,
                onOtherVersionClick = onOtherVersionClick,
                modifier = Modifier.padding(horizontal = 4.dp),
            )
        }

        if (advancedMode) {
            item { Spacer(modifier = Modifier.height(12.dp)) }

            item {
                var showExportDialog by remember { mutableStateOf(false) }
                Material3MenuGroup(
                    items =
                        listOf(
                            Material3MenuItemData(
                                title = { Text(text = stringResource(R.string.export_playlist)) },
                                icon = {
                                    Icon(
                                        painter = painterResource(R.drawable.share),
                                        contentDescription = null,
                                    )
                                },
                                onClick = { showExportDialog = true },
                            ),
                            Material3MenuItemData(
                                title = { Text(text = stringResource(R.string.refetch)) },
                                description = { Text(text = stringResource(R.string.refetch_desc)) },
                                icon = {
                                    Icon(
                                        painter = painterResource(R.drawable.sync),
                                        contentDescription = null,
                                        modifier = Modifier.graphicsLayer(rotationZ = rotationAnimation),
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
                        ),
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
                                    "csv" -> PlaylistExporter.exportPlaylistAsCSV(context, album.album.title, playlistSongs)
                                    "m3u" -> PlaylistExporter.exportPlaylistAsM3U(context, album.album.title, playlistSongs)
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
                                }.onFailure {
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
                                    "csv" -> PlaylistExporter.exportPlaylistAsCSV(context, album.album.title, playlistSongs)
                                    "m3u" -> PlaylistExporter.exportPlaylistAsM3U(context, album.album.title, playlistSongs)
                                    else -> Result.failure(IllegalArgumentException("Unknown format"))
                                }
                            export
                                .onSuccess { file ->
                                    val mimeType = if (format == "csv") "text/csv" else "audio/x-mpegurl"
                                    val save = saveToPublicDocuments(context, file, mimeType)
                                    save
                                        .onSuccess { Toast.makeText(context, R.string.export_success, Toast.LENGTH_SHORT).show() }
                                        .onFailure { Toast.makeText(context, R.string.export_failed, Toast.LENGTH_SHORT).show() }
                                }.onFailure {
                                    Toast.makeText(context, R.string.export_failed, Toast.LENGTH_SHORT).show()
                                }
                            showExportDialog = false
                        },
                    )
                }
            }
        }
    }

}

@Composable
private fun NewIrideAlbumMenuHeader(
    album: Album,
    trackCount: Int,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val database = LocalDatabase.current
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = album.album.title,
                style =
                    MaterialTheme.typography.titleMedium.copy(
                        fontFamily = SpaceMonoFontFamily,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.2).sp,
                    ),
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.basicMarquee(),
            )
            Spacer(modifier = Modifier.height(2.dp))
            val songCountText = pluralStringResource(R.plurals.n_song, trackCount, trackCount)
            val subtitle =
                if (album.artists.isNotEmpty()) {
                    "${album.artists.joinToString(", ") { it.name }} · $songCountText"
                } else {
                    songCountText
                }
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.6f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        IconButton(
            onClick = {
                database.query {
                    update(album.album.toggleLike())
                }
            },
        ) {
            Icon(
                painter = painterResource(if (album.album.bookmarkedAt != null) R.drawable.favorite else R.drawable.favorite_border),
                tint = if (album.album.bookmarkedAt != null) Color.White else Color.White.copy(alpha = 0.85f),
                contentDescription = null,
            )
        }
        IconButton(
            onClick = {
                onDismiss()
                val intent =
                    Intent().apply {
                        action = Intent.ACTION_SEND
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, "https://music.youtube.com/playlist?list=${album.album.playlistId}")
                    }
                context.startActivity(Intent.createChooser(intent, null))
            },
        ) {
            Icon(
                painter = painterResource(R.drawable.share),
                tint = Color.White.copy(alpha = 0.85f),
                contentDescription = null,
            )
        }
    }
}

@Composable
private fun ArtistOtherVersionSwitchRow(
    artists: List<ArtistEntity>,
    otherVersion: AlbumItem?,
    onArtistClick: () -> Unit,
    onOtherVersionClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        SwitchTile(
            label = stringResource(R.string.view_artist),
            onClick = onArtistClick,
            leading = { StackedArtistAvatars(artists) },
            modifier = Modifier.weight(1f),
        )
        if (otherVersion != null) {
            SwitchTile(
                label = stringResource(R.string.other_versions),
                onClick = onOtherVersionClick,
                leading = {
                    AsyncImage(
                        model = otherVersion.thumbnail,
                        contentDescription = null,
                        modifier = Modifier.size(30.dp).clip(SquircleShape(radius = 8.dp, cornerSmoothing = 0.48f)),
                    )
                },
                modifier = Modifier.weight(1f),
            )
        }
    }
}
