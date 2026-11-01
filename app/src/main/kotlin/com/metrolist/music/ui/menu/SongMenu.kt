/** Metrolist Project (C) 2026 Licensed under GPL-3.0 | See git history for contributors */
package com.metrolist.music.ui.menu

import androidx.compose.foundation.shape.CircleShape

import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import com.metrolist.music.ui.theme.FolioShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.DownloadRequest
import androidx.media3.exoplayer.offline.DownloadService
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import com.metrolist.innertube.YouTube
import com.metrolist.music.LocalDatabase
import com.metrolist.music.LocalDownloadUtil
import com.metrolist.music.LocalListenTogetherManager
import com.metrolist.music.LocalPlayerConnection
import com.metrolist.music.LocalSyncUtils
import com.metrolist.music.R
import com.metrolist.music.constants.AdvancedModeKey
import com.metrolist.music.constants.AutoLinkFeaturedArtistsKey
import com.metrolist.music.constants.ListItemHeight
import com.metrolist.music.constants.ListThumbnailSize
import com.metrolist.music.db.MusicDatabase
import com.metrolist.music.db.entities.Album
import com.metrolist.music.db.entities.ArtistEntity
import com.metrolist.music.db.entities.Event
import com.metrolist.music.db.entities.PlaylistSong
import com.metrolist.music.db.entities.PodcastEntity
import com.metrolist.music.db.entities.Song
import com.metrolist.music.extensions.toMediaItem
import com.metrolist.music.models.toMediaMetadata
import com.metrolist.music.playback.ExoDownloadService
import com.metrolist.music.ui.component.DefaultDialog
import com.metrolist.music.ui.component.IrideAnchoredSongMenu
import com.metrolist.music.ui.component.IrideFolio
import com.metrolist.music.ui.component.IrideFolioMenuDivider
import com.metrolist.music.ui.component.IrideFolioMenuItem
import com.metrolist.music.ui.component.IrideFolioPrimaryButton
import com.metrolist.music.ui.component.IrideFolioScrollbar
import com.metrolist.music.ui.component.rememberRubberBandPull
import com.metrolist.music.ui.component.IrideFolioSecondaryButton
import com.metrolist.music.ui.component.ListDialog
import com.metrolist.music.ui.component.LocalSongMenuPopupState
import com.metrolist.music.ui.component.Material3MenuItemData
import com.metrolist.music.ui.component.SongListItem
import com.metrolist.music.ui.component.folioTypewriterFontFamily
import com.metrolist.music.ui.component.irideFolioGestures
import com.metrolist.music.ui.component.rememberMenuActionScope
import com.metrolist.music.ui.theme.fillSubtle
import com.metrolist.music.ui.theme.strokeHairline
import com.metrolist.music.ui.theme.textPrimary
import com.metrolist.music.ui.theme.textSecondary
import com.metrolist.music.ui.theme.textTertiary
import com.metrolist.music.utils.ArtistMetadataResolver
import com.metrolist.music.utils.SyncUtils
import com.metrolist.music.utils.rememberPreference
import com.metrolist.music.viewmodels.CachePlaylistViewModel
import java.time.LocalDateTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import sv.lib.squircleshape.SquircleShape
import timber.log.Timber

@Composable
fun SongMenu(
    originalSong: Song,
    event: Event? = null,
    navController: NavController,
    playlistSong: PlaylistSong? = null,
    playlistBrowseId: String? = null,
    onDismiss: () -> Unit,
    isFromCache: Boolean = false,
    onHistoryRemoved: () -> Unit = {},
) {
    val context = LocalContext.current
    val database = LocalDatabase.current
    val popupState = LocalSongMenuPopupState.current
    val playerConnection = LocalPlayerConnection.current ?: return
    val songState = database.song(originalSong.id).collectAsState(initial = originalSong)
    val sourceSong = songState.value ?: originalSong
    val autoLinkFeaturedArtists by
        rememberPreference(AutoLinkFeaturedArtistsKey, defaultValue = true)
    val song by
        produceState(initialValue = sourceSong, sourceSong, autoLinkFeaturedArtists) {
            value = sourceSong
            if (autoLinkFeaturedArtists)
                value = ArtistMetadataResolver.withFeaturedArtists(database, sourceSong)
        }
    val download by
        LocalDownloadUtil.current.getDownload(originalSong.id).collectAsState(initial = null)
    val coroutineScope = rememberMenuActionScope()
    val syncUtils = LocalSyncUtils.current
    val listenTogetherManager = LocalListenTogetherManager.current
    val scope = rememberMenuActionScope()
    var refetchIconDegree by remember { mutableFloatStateOf(0f) }

    val cacheViewModel = hiltViewModel<CachePlaylistViewModel>()

    val rotationAnimation by
        animateFloatAsState(
            targetValue = refetchIconDegree,
            animationSpec = tween(durationMillis = 800),
            label = "",
        )

    val (advancedMode) = rememberPreference(AdvancedModeKey, defaultValue = false)

    val podcastEntity by
        produceState<PodcastEntity?>(initialValue = null, song) {
            val podcastId = song.song.albumId
            if (song.song.isEpisode && podcastId != null) {
                database.podcast(podcastId).collect { value = it }
            }
        }
    val isPodcastSubscribed = podcastEntity?.bookmarkedAt != null

    val sourceArtists = song.orderedArtists
    val orderedArtists by
        produceState(initialValue = sourceArtists, sourceArtists) {
            value = sourceArtists
            coroutineScope {
                sourceArtists.forEach { artist ->
                    launch {
                        val loaded = ArtistMetadataResolver.withArtwork(database, artist)
                        value = value.map { if (it.id == loaded.id) loaded else it }
                    }
                }
            }
        }
    val menuArtists = orderedArtists.filterIndexed { index, artist ->
        index == 0 || artist.isYouTubeArtist || artist.isLocal || song.song.isLocal
    }

    val currentEntry = navController.currentBackStackEntry
    val currentRoute = currentEntry?.destination?.route.orEmpty()
    val currentArtistId =
        if (currentRoute.startsWith("artist/")) {
            currentEntry?.arguments?.getString("artistId")
        } else null
    val currentAlbumId =
        if (currentRoute.startsWith("album/")) {
            currentEntry?.arguments?.getString("albumId")
        } else null
    val contextAlbum by
        produceState<Album?>(initialValue = null, currentAlbumId) {
            currentAlbumId?.let { id -> database.album(id).collect { value = it } }
        }
    val albumArtists = contextAlbum?.artists.orEmpty()
    val albumArtistIds = buildSet {
        addAll(albumArtists.map { it.id })
        // Local and online metadata can give the same credited artist different IDs.
        addAll(
            menuArtists
                .filter { artist ->
                    albumArtists.any { it.name.equals(artist.name, ignoreCase = true) }
                }
                .map { it.id }
        )
    }
    val navigation =
        songMenuNavigation(
            artistIds = menuArtists.map { it.id },
            songAlbumId = song.song.albumId,
            currentArtistId = currentArtistId,
            currentAlbumId = currentAlbumId,
            albumArtistIds = albumArtistIds,
        )
    val navigationArtists =
        menuArtists.distinctBy { it.id }.filter { it.id in navigation.artistIds }

    var showChoosePlaylistDialog by rememberSaveable {
        mutableStateOf(false)
    }

    var showErrorPlaylistAddDialog by rememberSaveable {
        mutableStateOf(false)
    }

    AddToPlaylistDialog(
        isVisible = showChoosePlaylistDialog,
        allowSyncing = !song.song.isLocal,
        onGetSong = { playlist ->
            if (!song.song.isLocal) {
                coroutineScope.launch(Dispatchers.IO) {
                    playlist.playlist.browseId?.let { browseId ->
                        YouTube.addToPlaylist(browseId, song.id)
                    }
                }
            }
            listOf(song.id)
        },
        onGetSongIds = { listOf(song.id) },
        onDismiss = {
            showChoosePlaylistDialog = false
            if (popupState != null) onDismiss()
        },
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

            items(listOf(song)) { song ->
                SongListItem(song = song)
            }
        }
    }

    var showSelectArtistDialog by rememberSaveable {
        mutableStateOf(false)
    }

    var showDeleteUploadedDialog by rememberSaveable {
        mutableStateOf(false)
    }
    var isDeleting by remember { mutableStateOf(false) }

    if (showDeleteUploadedDialog) {
        DefaultDialog(
            onDismiss = { if (!isDeleting) showDeleteUploadedDialog = false },
            icon = {
                Icon(
                    painter = painterResource(R.drawable.delete),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                )
            },
            title = { Text(stringResource(R.string.delete_uploaded_song)) },
            buttons = {
                IrideFolioSecondaryButton(
                    onClick = { showDeleteUploadedDialog = false },
                    enabled = !isDeleting,
                ) {
                    Text(stringResource(R.string.cancel))
                }
                IrideFolioPrimaryButton(
                    onClick = {
                        val entityId = song.song.uploadEntityId
                        if (entityId == null) {
                            Toast.makeText(
                                    context,
                                    R.string.delete_uploaded_song_failed,
                                    Toast.LENGTH_SHORT,
                                )
                                .show()
                            showDeleteUploadedDialog = false
                            return@IrideFolioPrimaryButton
                        }
                        isDeleting = true
                        coroutineScope.launch(Dispatchers.IO) {
                            YouTube.deleteUploadedSong(entityId)
                                .onSuccess {
                                    database.query {
                                        delete(song.song)
                                    }
                                    withContext(Dispatchers.Main) {
                                        Toast.makeText(
                                                context,
                                                R.string.delete_uploaded_song_success,
                                                Toast.LENGTH_SHORT,
                                            )
                                            .show()
                                        isDeleting = false
                                        showDeleteUploadedDialog = false
                                        onDismiss()
                                    }
                                }
                                .onFailure {
                                    withContext(Dispatchers.Main) {
                                        Toast.makeText(
                                                context,
                                                R.string.delete_uploaded_song_failed,
                                                Toast.LENGTH_SHORT,
                                            )
                                            .show()
                                        isDeleting = false
                                        showDeleteUploadedDialog = false
                                    }
                                }
                        }
                    },
                    enabled = !isDeleting,
                ) {
                    if (isDeleting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                        )
                    } else {
                        Text(text = stringResource(R.string.delete))
                    }
                }
            },
        ) {
            Text(
                text = stringResource(R.string.delete_uploaded_song_confirm),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }

    if (showSelectArtistDialog) {
        ListDialog(
            onDismiss = {
                showSelectArtistDialog = false
                if (popupState != null) onDismiss()
            }
        ) {
            items(
                items = navigationArtists,
                key = { "menu_song_artist_${it.id}" },
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
                        modifier = Modifier.padding(8.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        AsyncImage(
                            model = artist.thumbnailUrl,
                            placeholder = painterResource(R.drawable.artist),
                            error = painterResource(R.drawable.artist),
                            fallback = painterResource(R.drawable.artist),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
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

    val isGuest = listenTogetherManager?.isInRoom == true && !listenTogetherManager.isHost

    val primaryActions =
        listOf(
            Material3MenuItemData(
                title = { Text(stringResource(R.string.iride_menu_next)) },
                icon = { Icon(painterResource(R.drawable.playlist_play), null) },
                onClick = {
                    onDismiss()
                    playerConnection.playNext(song.toMediaItem())
                },
            ),
            Material3MenuItemData(
                title = { Text(stringResource(R.string.iride_menu_queue)) },
                icon = { Icon(painterResource(R.drawable.queue_music), null) },
                onClick = {
                    onDismiss()
                    playerConnection.addToQueue(song.toMediaItem())
                },
            ),
            Material3MenuItemData(
                title = { Text(stringResource(R.string.radio)) },
                icon = { Icon(painterResource(R.drawable.radio), null) },
                onClick = {
                    onDismiss()
                    playerConnection.startRadioForSong(song.toMediaMetadata())
                },
            ),
        )

    val suggestToHostItem =
        Material3MenuItemData(
            title = { Text(text = stringResource(R.string.suggest_to_host)) },
            icon = {
                Icon(
                    painter = painterResource(R.drawable.queue_music),
                    contentDescription = null,
                )
            },
            onClick = {
                val durationMs =
                    if (song.song.duration > 0) song.song.duration.toLong() * 1000 else 180000L
                val trackInfo =
                    com.metrolist.music.listentogether.TrackInfo(
                        id = song.id,
                        title = song.song.title,
                        artist = orderedArtists.joinToString(", ") { it.name },
                        album = song.song.albumName,
                        duration = durationMs,
                        thumbnail = song.thumbnailUrl,
                    )
                listenTogetherManager?.suggestTrack(trackInfo)
                onDismiss()
            },
        )

    val downloadItem =
        when (download?.state) {
            Download.STATE_COMPLETED -> {
                Material3MenuItemData(
                    title = { Text(text = stringResource(R.string.remove_download)) },
                    icon = {
                        Icon(
                            painter = painterResource(R.drawable.offline),
                            contentDescription = null,
                        )
                    },
                    onClick = {
                        DownloadService.sendRemoveDownload(
                            context,
                            ExoDownloadService::class.java,
                            song.id,
                            false,
                        )
                    },
                )
            }

            Download.STATE_QUEUED,
            Download.STATE_DOWNLOADING -> {
                Material3MenuItemData(
                    title = { Text(text = stringResource(R.string.folio_cancel_download)) },
                    icon = {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp,
                        )
                    },
                    onClick = {
                        DownloadService.sendRemoveDownload(
                            context,
                            ExoDownloadService::class.java,
                            song.id,
                            false,
                        )
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
                    },
                )
            }
        }

    val addToPlaylistItem =
        Material3MenuItemData(
            title = { Text(text = stringResource(R.string.add_to_playlist)) },
            icon = {
                Icon(
                    painter = painterResource(R.drawable.playlist_add),
                    contentDescription = null,
                )
            },
            onClick = { showChoosePlaylistDialog = true },
        )

    val viewArtistItem =
        Material3MenuItemData(
            title = {
                Text(
                    stringResource(
                        when (navigation.artistLabel) {
                            SongMenuArtistLabel.Artist -> R.string.view_artist
                            SongMenuArtistLabel.Artists -> R.string.song_menu_view_artists
                            SongMenuArtistLabel.Collaborators ->
                                R.string.song_menu_view_collaborators
                        }
                    )
                )
            },
            description = { Text(text = navigationArtists.joinToString { it.name }) },
            icon = {
                val artistThumbnail = navigationArtists.firstOrNull()?.thumbnailUrl
                if (artistThumbnail != null) {
                    AsyncImage(
                        model = artistThumbnail,
                        contentScale = ContentScale.Crop,
                        contentDescription = null,
                        modifier = Modifier.size(28.dp).clip(CircleShape),
                    )
                } else {
                    Icon(
                        painter = painterResource(R.drawable.artist),
                        contentDescription = null,
                    )
                }
            },
            onClick = {
                if (navigationArtists.size == 1) {
                    navController.navigate("artist/${navigationArtists[0].id}")
                    onDismiss()
                } else {
                    showSelectArtistDialog = true
                }
            },
        )

    val isPodcast = song.song.isEpisode

    val viewAlbumItem =
        Material3MenuItemData(
            title = {
                Text(
                    text =
                        stringResource(
                            when {
                                isPodcast -> R.string.view_podcast
                                song.album?.songCount == 1 -> R.string.view_single
                                song.album?.songCount?.let { it in 2..6 } == true ->
                                    R.string.view_ep
                                else -> R.string.view_album
                            }
                        )
                )
            },
            description = {
                song.song.albumName?.let {
                    Text(text = it)
                }
            },
            icon = {
                if ((song.album?.thumbnailUrl ?: song.thumbnailUrl) != null) {
                    AsyncImage(
                        model = song.album?.thumbnailUrl ?: song.thumbnailUrl,
                        contentScale = ContentScale.Crop,
                        contentDescription = null,
                        modifier = Modifier.size(28.dp).clip(FolioShape(4.dp)),
                    )
                } else {
                    Icon(
                        painter =
                            painterResource(if (isPodcast) R.drawable.mic else R.drawable.album),
                        contentDescription = null,
                    )
                }
            },
            onClick = {
                onDismiss()
                if (isPodcast) {
                    navController.navigate("online_podcast/${song.song.albumId}")
                } else {
                    navController.navigate("album/${song.song.albumId}")
                }
            },
        )

    val subscribeRefetchItems = buildList {
        song.song.albumId
            ?.takeIf { song.song.isEpisode }
            ?.let { podcastId ->
                add(
                    Material3MenuItemData(
                        title = {
                            Text(
                                text =
                                    stringResource(
                                        if (isPodcastSubscribed) {
                                            R.string.iride_remove_star
                                        } else {
                                            R.string.iride_star
                                        }
                                    )
                            )
                        },
                        description = {
                            song.song.albumName?.let {
                                Text(text = it)
                            }
                        },
                        icon = {
                            Icon(
                                painter =
                                    painterResource(
                                        if (isPodcastSubscribed) {
                                            R.drawable.library_add_check
                                        } else {
                                            R.drawable.library_add
                                        }
                                    ),
                                contentDescription = null,
                            )
                        },
                        onClick = {
                            Timber.d("[PODCAST_LIB] Toggling podcast save for: $podcastId")
                            coroutineScope.launch(Dispatchers.IO) {
                                val existingPodcast = podcastEntity
                                val isCurrentlySaved = existingPodcast?.bookmarkedAt != null

                                YouTube.savePodcast(podcastId, !isCurrentlySaved)
                                    .onSuccess {
                                        Timber.d("[PODCAST_LIB] savePodcast API success!")
                                    }
                                    .onFailure { e ->
                                        Timber.e(e, "[PODCAST_LIB] savePodcast API failed")
                                    }

                                if (existingPodcast != null) {
                                    Timber.d("[PODCAST_LIB] Updating existing podcast")
                                    database.query {
                                        update(existingPodcast.toggleBookmark())
                                    }
                                } else {
                                    Timber.d("[PODCAST_LIB] Creating new podcast entry")
                                    database.query {
                                        insert(
                                            PodcastEntity(
                                                    id = podcastId,
                                                    title =
                                                        song.song.albumName ?: "Unknown Podcast",
                                                    author = song.artists.firstOrNull()?.name,
                                                    thumbnailUrl = song.song.thumbnailUrl,
                                                )
                                                .toggleBookmark()
                                        )
                                    }
                                }
                            }
                            onDismiss()
                        },
                    )
                )
            }
        if (advancedMode) {
            add(
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
                            YouTube.queue(listOf(song.id)).onSuccess {
                                val newSong = it.firstOrNull()
                                if (newSong != null) {
                                    database.transaction {
                                        update(song, newSong.toMediaMetadata())
                                    }
                                }
                            }
                        }
                    },
                )
            )
        }
    }

    val destructiveItems = buildList {
        if (song.song.isEpisode) {
            val isEpisodeSaved = song.song.inLibrary != null
            add(
                Material3MenuItemData(
                    title = {
                        Text(
                            text =
                                stringResource(
                                    if (isEpisodeSaved) {
                                        R.string.remove_episode_from_saved
                                    } else {
                                        R.string.save_episode_for_later
                                    }
                                )
                        )
                    },
                    description = { Text(text = stringResource(R.string.episodes_for_later)) },
                    icon = {
                        Icon(
                            painter =
                                painterResource(
                                    if (isEpisodeSaved) {
                                        R.drawable.library_add_check
                                    } else {
                                        R.drawable.library_add
                                    }
                                ),
                            contentDescription = null,
                        )
                    },
                    onClick = {
                        coroutineScope.launch(Dispatchers.IO) {
                            val shouldBeSaved = !isEpisodeSaved

                            database.query {
                                update(
                                    song.song.copy(
                                        inLibrary =
                                            if (shouldBeSaved) LocalDateTime.now() else null,
                                        isEpisode = true,
                                    )
                                )
                            }

                            val setVideoId =
                                if (isEpisodeSaved) database.getSetVideoId(song.id)?.setVideoId
                                else null
                            syncUtils.saveEpisode(song.id, shouldBeSaved, setVideoId)
                        }
                        onDismiss()
                    },
                )
            )
        }
        if (event != null) {
            add(
                Material3MenuItemData(
                    title = { Text(text = stringResource(R.string.remove_from_history)) },
                    icon = {
                        Icon(
                            painter = painterResource(R.drawable.delete),
                            contentDescription = null,
                        )
                    },
                    onClick = {
                        onDismiss()
                        database.query {
                            delete(event)
                        }
                        onHistoryRemoved()
                    },
                )
            )
        }
        if (playlistSong != null) {
            add(
                Material3MenuItemData(
                    title = { Text(text = stringResource(R.string.remove_from_playlist)) },
                    icon = {
                        Icon(
                            painter = painterResource(R.drawable.delete),
                            contentDescription = null,
                        )
                    },
                    onClick = {
                        playlistSong.let { ps ->
                            val capturedSetVideoId = ps.map.setVideoId
                            database.transaction {
                                move(
                                    ps.map.playlistId,
                                    ps.map.position,
                                    Int.MAX_VALUE,
                                )
                                delete(ps.map.copy(position = Int.MAX_VALUE))
                            }
                            playlistBrowseId?.let { browseId ->
                                syncUtils.scheduleRemoveFromPlaylist(
                                    browseId,
                                    ps.map.songId,
                                    ps.map.playlistId,
                                ) {
                                    capturedSetVideoId
                                }
                            }
                            onDismiss()
                        }
                    },
                )
            )
        }
        if (isFromCache) {
            add(
                Material3MenuItemData(
                    title = { Text(text = stringResource(R.string.remove_from_cache)) },
                    icon = {
                        Icon(
                            painter = painterResource(R.drawable.delete),
                            contentDescription = null,
                        )
                    },
                    onClick = {
                        onDismiss()
                        cacheViewModel.removeSongFromCache(song.id)
                    },
                )
            )
        }
        if (song.song.isUploaded) {
            add(
                Material3MenuItemData(
                    title = { Text(text = stringResource(R.string.delete_uploaded_song)) },
                    icon = {
                        Icon(
                            painter = painterResource(R.drawable.delete),
                            contentDescription = null,
                        )
                    },
                    onClick = {
                        showDeleteUploadedDialog = true
                    },
                )
            )
        }
    }

    val favoriteItem = favoriteMenuItem(song, database, syncUtils, onDismiss)
    val shareItem =
        Material3MenuItemData(
            title = { Text(stringResource(R.string.share)) },
            icon = { Icon(painterResource(R.drawable.share), null) },
            onClick = {
                onDismiss()
                val intent =
                    Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, "https://music.youtube.com/watch?v=${song.id}")
                    }
                context.startActivity(Intent.createChooser(intent, null))
            },
        )
    val sections = buildList {
        add(if (isGuest) listOf(suggestToHostItem) else primaryActions)
        add(
            buildList {
                if (!song.song.isEpisode) add(favoriteItem)
                add(addToPlaylistItem)
                if (!song.song.isLocal)
                    add(
                        downloadItem.copy(
                            onClick = {
                                downloadItem.onClick?.invoke()
                                onDismiss()
                            }
                        )
                    )
            }
        )
        add(
            buildList {
                if (!song.song.isEpisode && navigationArtists.isNotEmpty()) add(viewArtistItem)
                if (navigation.showAlbum) add(viewAlbumItem)
            }
        )
        add(listOf(shareItem))
        if (subscribeRefetchItems.isNotEmpty()) add(subscribeRefetchItems)
        if (destructiveItems.isNotEmpty()) add(destructiveItems)
    }
        .filter { it.isNotEmpty() }

    val menuScrollState = rememberLazyListState()
    val menuOverscrollPull = rememberRubberBandPull()
    val density = LocalDensity.current
    val menuContentHeight =
        with(density) {
            sections.sumOf { it.size } * IrideFolio.MenuItemHeight.roundToPx() +
                (sections.size - 1).coerceAtLeast(0) *
                    (IrideFolio.MenuSectionVerticalPadding.roundToPx() * 2 +
                        IrideFolio.MenuDividerThickness.roundToPx()) +
                (IrideFolio.MenuVerticalPadding.roundToPx() +
                    IrideFolio.MenuBorderWidth.roundToPx()) * 2
        }
    val menuContent: @Composable (Boolean) -> Unit = { constrained ->
        Box {
            LazyColumn(
                state = menuScrollState,
                overscrollEffect = null,
                modifier =
                    Modifier.padding(
                            end = if (constrained) IrideFolio.ScrollbarTouchTargetWidth else 0.dp
                        )
                        .irideFolioGestures(menuScrollState, menuOverscrollPull),
                contentPadding =
                    PaddingValues(
                        vertical = IrideFolio.MenuVerticalPadding,
                        horizontal = IrideFolio.MenuContentPadding,
                    ),
            ) {
                sections.forEachIndexed { index, section ->
                    if (index > 0) item { IrideFolioMenuDivider() }
                    items(section) { action ->
                        IrideFolioMenuItem(
                            text = action.title,
                            selected = action.selected,
                            leadingIcon =
                                action.icon?.let { icon ->
                                    {
                                        Box(
                                            Modifier.size(24.dp),
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            icon()
                                        }
                                    }
                                },
                            onClick = { action.onClick?.invoke() },
                        )
                    }
                }
            }
            if (constrained) {
                Box(Modifier.matchParentSize(), contentAlignment = Alignment.CenterEnd) {
                    IrideFolioScrollbar(
                        menuScrollState,
                        Modifier.fillMaxHeight().padding(
                            top = IrideFolio.MenuScrollbarTopInset,
                            bottom = IrideFolio.MenuScrollbarBottomInset,
                        ),
                        menuOverscrollPull,
                    )
                }
            }
        }
    }
    // Dialogs live outside the popup so closing it does not dispose their state or coroutines.
    if (popupState != null) {
        if (
            !showChoosePlaylistDialog &&
                !showErrorPlaylistAddDialog &&
                !showSelectArtistDialog &&
                !showDeleteUploadedDialog
        ) {
            IrideAnchoredSongMenu(popupState, menuContentHeight) { constrained ->
                menuContent(constrained)
            }
        }
    } else {
        menuContent(false)
    }
}

@Composable
private fun favoriteMenuItem(
    song: Song,
    database: MusicDatabase,
    syncUtils: SyncUtils,
    onDismiss: () -> Unit,
): Material3MenuItemData {
    var optimisticFavorite by remember(song.id, song.song.liked) { mutableStateOf(song.song.liked) }
    return Material3MenuItemData(
        title = {
            Text(
                stringResource(
                    if (optimisticFavorite) R.string.iride_remove_star
                    else R.string.iride_star
                )
            )
        },
        icon = {
            Icon(
                painterResource(
                    if (optimisticFavorite) R.drawable.favorite else R.drawable.favorite_border
                ),
                contentDescription = null,
            )
        },
        onClick = {
            optimisticFavorite = !optimisticFavorite
            val updatedSong = song.song.toggleLike(syncToYouTube = false)
            database.query { upsert(updatedSong) }
            if (!song.song.isLocal) syncUtils.likeSong(updatedSong)
            onDismiss()
        },
    )
}

@Composable
internal fun StackedArtistAvatars(
    artists: List<ArtistEntity>,
    modifier: Modifier = Modifier,
    size: Dp = 30.dp,
) {
    val visible = artists.take(3)
    if (visible.isEmpty()) {
        Icon(
            painter = painterResource(R.drawable.artist),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.textSecondary,
            modifier = modifier.size(size),
        )
        return
    }
    val step = size * 0.5f
    Box(
        modifier = modifier.width(size + step * (visible.size - 1)),
        contentAlignment = Alignment.CenterStart,
    ) {
        for (i in visible.indices.reversed()) {
            AsyncImage(
                model = visible[i].thumbnailUrl,
                placeholder = painterResource(R.drawable.artist),
                error = painterResource(R.drawable.artist),
                fallback = painterResource(R.drawable.artist),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier =
                    Modifier.offset(x = step * i)
                        .size(size)
                        .clip(CircleShape)
                        .border(1.5.dp, Color.Black.copy(alpha = 0.5f), CircleShape),
            )
        }
    }
}

@Composable
internal fun SwitchTile(
    label: String,
    onClick: () -> Unit,
    leading: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tileShape = SquircleShape(radius = 14.dp, cornerSmoothing = 0.48f)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier =
            modifier
                .clip(tileShape)
                .border(BorderStroke(1.dp, MaterialTheme.colorScheme.strokeHairline), tileShape)
                .clickable(onClick = onClick)
                .padding(horizontal = 10.dp, vertical = 10.dp),
    ) {
        leading()
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = label,
            style =
                MaterialTheme.typography.labelLarge.copy(
                    fontFamily = folioTypewriterFontFamily(),
                    fontWeight = FontWeight.Bold,
                ),
            color = MaterialTheme.colorScheme.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
internal fun ArtistAlbumSwitchRow(
    song: Song,
    orderedArtists: List<ArtistEntity>,
    onArtistClick: () -> Unit,
    onAlbumClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isPodcast = song.song.isEpisode
    val albumLabelRes =
        when {
            isPodcast -> R.string.view_podcast
            song.album?.songCount == 1 -> R.string.view_single
            song.album?.songCount?.let { it in 2..6 } == true -> R.string.view_ep
            else -> R.string.view_album
        }

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (!isPodcast && orderedArtists.isNotEmpty()) {
            SwitchTile(
                label = stringResource(R.string.view_artist),
                onClick = onArtistClick,
                leading = { StackedArtistAvatars(orderedArtists) },
                modifier = Modifier.weight(1f),
            )
        }
        if (song.song.albumId != null) {
            SwitchTile(
                label = stringResource(albumLabelRes),
                onClick = onAlbumClick,
                leading = {
                    AsyncImage(
                        model = song.thumbnailUrl,
                        contentDescription = null,
                        modifier =
                            Modifier.size(30.dp)
                                .clip(SquircleShape(radius = 8.dp, cornerSmoothing = 0.48f)),
                    )
                },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
internal fun ProminentActionRow(
    icon: Int,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier =
            modifier
                .fillMaxWidth()
                .clip(SquircleShape(radius = 16.dp, cornerSmoothing = 0.48f))
                .background(MaterialTheme.colorScheme.fillSubtle)
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 16.dp),
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.textPrimary,
            modifier = Modifier.size(24.dp),
        )
        Spacer(modifier = Modifier.width(14.dp))
        Text(
            text = label,
            style =
                MaterialTheme.typography.titleMedium.copy(
                    fontFamily = folioTypewriterFontFamily(),
                    fontWeight = FontWeight.Bold,
                ),
            color = MaterialTheme.colorScheme.textPrimary,
        )
    }
}

@Composable
internal fun NoteRatingStars(
    rating: Float?,
    modifier: Modifier = Modifier,
    interactive: Boolean = false,
    onRatingChange: (Float) -> Unit = {},
) {
    val iconSize = 16.dp
    Row(verticalAlignment = Alignment.CenterVertically, modifier = modifier) {
        for (star in 1..5) {
            val fillFraction = ((rating ?: 0f) - (star - 1)).coerceIn(0f, 1f)
            Box(
                contentAlignment = Alignment.Center,
                modifier =
                    Modifier.size(if (interactive) 28.dp else iconSize)
                        .then(
                            if (interactive) {
                                Modifier.pointerInput(star) {
                                    detectTapGestures { offset ->
                                        val newRating =
                                            (star - 1) +
                                                if (offset.x < size.width / 2f) 0.5f else 1f
                                        onRatingChange(newRating)
                                    }
                                }
                            } else {
                                Modifier
                            }
                        ),
            ) {
                Box(modifier = Modifier.size(iconSize)) {
                    Icon(
                        painter = painterResource(R.drawable.favorite_border),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.textTertiary,
                        modifier = Modifier.size(iconSize),
                    )
                    if (fillFraction > 0f) {
                        Box(
                            modifier =
                                Modifier.fillMaxHeight().fillMaxWidth(fillFraction).clipToBounds()
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.favorite),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.textPrimary,
                                modifier = Modifier.size(iconSize),
                            )
                        }
                    }
                }
            }
        }
    }
}

internal val noteFieldColors: @Composable () -> androidx.compose.material3.TextFieldColors = {
    TextFieldDefaults.colors(
        focusedContainerColor = Color.Transparent,
        unfocusedContainerColor = Color.Transparent,
        focusedIndicatorColor = Color.Transparent,
        unfocusedIndicatorColor = Color.Transparent,
        cursorColor = MaterialTheme.colorScheme.textPrimary,
        focusedTextColor = MaterialTheme.colorScheme.textPrimary,
        unfocusedTextColor = MaterialTheme.colorScheme.textPrimary,
    )
}

@Composable
internal fun CoverNoteRow(
    song: Song,
    database: MusicDatabase,
    modifier: Modifier = Modifier,
) {
    var isEditingNote by rememberSaveable(song.id) { mutableStateOf(false) }
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val density = LocalDensity.current
        val textMeasurer = rememberTextMeasurer()
        val standardCoverFraction = 0.48f
        val minCoverFraction = standardCoverFraction * 0.6f
        val noteTitle = song.song.noteTitle.orEmpty()
        val totalWidthPx = with(density) { maxWidth.toPx() }
        val titleMeasureStyle =
            MaterialTheme.typography.bodyLarge.copy(
                fontFamily = folioTypewriterFontFamily(),
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
                            )
                            .size
                            .width
                            .toFloat()
                    val spacingPx = with(density) { 14.dp.toPx() }
                    val innerPaddingPx = with(density) { 32.dp.toPx() }
                    var fraction = standardCoverFraction
                    while (fraction > minCoverFraction) {
                        val noteBoxWidthPx =
                            totalWidthPx * (1f - fraction) - spacingPx - innerPaddingPx
                        if (titleWidthPx <= noteBoxWidthPx) break
                        fraction -= 0.04f
                    }
                    fraction.coerceAtLeast(minCoverFraction)
                }
            }
        val coverWeight by
            animateFloatAsState(
                targetValue = if (isEditingNote) 0.0001f else coverFraction,
                animationSpec = tween(280),
                label = "coverWeight",
            )
        val noteWeight by
            animateFloatAsState(
                targetValue = if (isEditingNote) 1f else (1f - coverFraction),
                animationSpec = tween(280),
                label = "noteWeight",
            )
        val spacingPx = with(density) { 14.dp.toPx() }
        val coverHeightDp = with(density) { ((totalWidthPx - spacingPx) * coverFraction).toDp() }
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
                    model = song.thumbnailUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier =
                        Modifier.fillMaxWidth()
                            .aspectRatio(1f)
                            .clip(SquircleShape(radius = 8.dp, cornerSmoothing = 0.48f)),
                )
            }
            AddNoteBox(
                key = song.id,
                initialTitle = song.song.noteTitle.orEmpty(),
                initialRating = song.song.noteRating,
                initialText = song.song.noteText.orEmpty(),
                onPersist = { noteTitleValue, noteRatingValue, noteTextValue ->
                    database.query {
                        update(
                            song.song.copy(
                                noteTitle = noteTitleValue.trim().ifBlank { null },
                                noteRating = noteRatingValue,
                                noteText = noteTextValue.trim().ifBlank { null },
                            )
                        )
                    }
                },
                isEditing = isEditingNote,
                onEditingChange = { isEditingNote = it },
                modifier =
                    Modifier.weight(noteWeight)
                        .then(if (isEditingNote) Modifier else Modifier.height(coverHeightDp)),
            )
        }
    }
}

@Composable
internal fun AddNoteBox(
    key: Any,
    initialTitle: String,
    initialRating: Float?,
    initialText: String,
    onPersist: (title: String, rating: Float?, text: String) -> Unit,
    isEditing: Boolean,
    onEditingChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    var title by remember(key) { mutableStateOf(initialTitle) }
    var rating by remember(key) { mutableStateOf(initialRating) }
    var ratingText by remember(key) { mutableStateOf(initialRating?.toString().orEmpty()) }
    var description by remember(key) { mutableStateOf(initialText) }

    val titleMaxLength = 26
    var titleFlashActive by remember(key) { mutableStateOf(false) }
    var titleFlashToken by remember(key) { mutableIntStateOf(0) }
    LaunchedEffect(titleFlashToken) {
        if (titleFlashToken == 0) return@LaunchedEffect
        titleFlashActive = true
        delay(150)
        titleFlashActive = false
    }
    val titleColor by
        animateColorAsState(
            targetValue =
                if (titleFlashActive) Color.Red else MaterialTheme.colorScheme.textPrimary,
            animationSpec = tween(if (titleFlashActive) 60 else 300),
            label = "titleFlashColor",
        )

    fun persist() {
        onPersist(title, rating, description)
    }

    val isEmpty = title.isBlank() && rating == null && description.isBlank()
    val centerEmptyState = isEmpty && !isEditing

    val boxShape = SquircleShape(radius = 20.dp, cornerSmoothing = 0.48f)
    Column(
        modifier =
            modifier
                .clip(boxShape)
                .border(BorderStroke(1.dp, MaterialTheme.colorScheme.strokeHairline), boxShape)
                .then(if (!isEditing) Modifier.clickable { onEditingChange(true) } else Modifier)
                .animateContentSize()
                .padding(16.dp),
        verticalArrangement = if (centerEmptyState) Arrangement.Center else Arrangement.Top,
        horizontalAlignment =
            if (centerEmptyState) Alignment.CenterHorizontally else Alignment.Start,
    ) {
        when {
            isEditing -> {
                TextField(
                    value = title,
                    onValueChange = { input ->
                        if (input.length <= titleMaxLength) {
                            title = input
                            persist()
                        } else {
                            title = input.take(titleMaxLength)
                            persist()
                            titleFlashToken++
                        }
                    },
                    placeholder = {
                        Text(
                            stringResource(R.string.note_title_placeholder),
                            color = MaterialTheme.colorScheme.textTertiary,
                        )
                    },
                    singleLine = true,
                    colors =
                        TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            cursorColor = MaterialTheme.colorScheme.textPrimary,
                            focusedTextColor = titleColor,
                            unfocusedTextColor = titleColor,
                        ),
                    textStyle =
                        MaterialTheme.typography.bodyLarge.copy(
                            fontFamily = folioTypewriterFontFamily(),
                            fontWeight = FontWeight.Bold,
                        ),
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    NoteRatingStars(
                        rating = rating,
                        interactive = true,
                        onRatingChange = {
                            rating = it
                            ratingText = it.toString()
                            persist()
                        },
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    BasicTextField(
                        value = ratingText,
                        onValueChange = { input ->
                            val filtered = buildString {
                                var dotUsed = false
                                for (c in input) {
                                    when {
                                        c.isDigit() -> append(c)
                                        c == '.' && !dotUsed && isNotEmpty() -> {
                                            append(c)
                                            dotUsed = true
                                        }
                                    }
                                }
                            }
                                .take(4)
                            ratingText = filtered
                            filtered.toFloatOrNull()?.let {
                                rating = it.coerceIn(0f, 5f)
                                persist()
                            }
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        textStyle =
                            MaterialTheme.typography.bodyMedium.copy(
                                color = MaterialTheme.colorScheme.textPrimary,
                                fontFamily = folioTypewriterFontFamily(),
                            ),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.textPrimary),
                        modifier = Modifier.width(34.dp),
                    )
                    Text(
                        text = "/5",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.textSecondary,
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                TextField(
                    value = description,
                    onValueChange = {
                        description = it
                        persist()
                    },
                    placeholder = {
                        Text(
                            stringResource(R.string.note_description_placeholder),
                            color = MaterialTheme.colorScheme.textTertiary,
                        )
                    },
                    colors = noteFieldColors(),
                    textStyle =
                        MaterialTheme.typography.bodyMedium.copy(
                            color = MaterialTheme.colorScheme.textPrimary
                        ),
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(4.dp))
                IconButton(
                    onClick = { onEditingChange(false) },
                    modifier = Modifier.align(Alignment.End),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.expand_less),
                        tint = MaterialTheme.colorScheme.textSecondary,
                        contentDescription = null,
                    )
                }
            }

            isEmpty -> {
                Icon(
                    painter = painterResource(R.drawable.edit),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.textSecondary,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.add_note),
                    style =
                        MaterialTheme.typography.bodyMedium.copy(
                            fontFamily = folioTypewriterFontFamily(),
                            fontWeight = FontWeight.Bold,
                        ),
                    color = MaterialTheme.colorScheme.textSecondary,
                    textAlign = TextAlign.Center,
                )
            }

            else -> {
                BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                    val density = LocalDensity.current
                    val titleStyle =
                        MaterialTheme.typography.bodyLarge.copy(
                            fontFamily = folioTypewriterFontFamily(),
                            fontWeight = FontWeight.Bold,
                        )
                    val descriptionStyle = MaterialTheme.typography.bodyMedium
                    val availableHeightPx = with(density) { maxHeight.toPx() }
                    val titleMaxLines = if (title.isNotBlank()) 2 else 0
                    val titleBlockPx =
                        if (title.isNotBlank()) {
                            with(density) {
                                titleStyle.lineHeight.toPx() * titleMaxLines + 6.dp.toPx()
                            }
                        } else {
                            0f
                        }
                    val ratingBlockPx = if (rating != null) with(density) { 22.dp.toPx() } else 0f
                    val descriptionMaxLines =
                        with(density) {
                            ((availableHeightPx - titleBlockPx - ratingBlockPx) /
                                    descriptionStyle.lineHeight.toPx())
                                .toInt()
                                .coerceAtLeast(1)
                        }

                    Column {
                        if (title.isNotBlank()) {
                            Text(
                                text = title,
                                style = titleStyle,
                                color = MaterialTheme.colorScheme.textPrimary,
                                maxLines = titleMaxLines,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                        }
                        val ratingValue = rating
                        if (ratingValue != null) {
                            val ratingLabel =
                                if (ratingValue % 1f == 0f) {
                                    "${ratingValue.toInt()}/5"
                                } else {
                                    "${String.format(java.util.Locale.US, "%.1f", ratingValue)}/5"
                                }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                NoteRatingStars(rating = rating)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = ratingLabel,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.textSecondary,
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                        }
                        if (description.isNotBlank()) {
                            Text(
                                text = description,
                                style = descriptionStyle,
                                color = MaterialTheme.colorScheme.textSecondary,
                                maxLines = descriptionMaxLines,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }
        }
    }
}
