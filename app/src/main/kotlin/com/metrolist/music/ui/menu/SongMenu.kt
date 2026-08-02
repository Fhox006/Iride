/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.menu

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
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
import com.metrolist.music.constants.ListItemHeight
import com.metrolist.music.constants.ListThumbnailSize
import com.metrolist.music.constants.TopNavigationBarKey
import com.metrolist.music.db.MusicDatabase
import com.metrolist.music.db.entities.ArtistEntity
import com.metrolist.music.db.entities.Event
import com.metrolist.music.db.entities.PlaylistSong
import com.metrolist.music.db.entities.PodcastEntity
import com.metrolist.music.db.entities.Song
import com.metrolist.music.extensions.toMediaItem
import com.metrolist.music.models.toMediaMetadata
import com.metrolist.music.playback.ExoDownloadService
import com.metrolist.music.ui.component.DefaultDialog
import com.metrolist.music.ui.component.ListDialog
import com.metrolist.music.ui.component.LocalBottomSheetPageState
import com.metrolist.music.ui.component.Material3MenuGroup
import com.metrolist.music.ui.component.NewAction
import com.metrolist.music.ui.component.NewActionGrid
import com.metrolist.music.ui.component.Material3MenuItemData
import com.metrolist.music.ui.component.SongListItem
import com.metrolist.music.ui.theme.SpaceMonoFontFamily
import com.metrolist.music.ui.utils.ShowMediaInfo
import com.metrolist.music.utils.SyncUtils
import com.metrolist.music.utils.rememberPreference
import com.metrolist.music.viewmodels.CachePlaylistViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.time.LocalDateTime

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
    val playerConnection = LocalPlayerConnection.current ?: return
    val (newIrideUi) = rememberPreference(TopNavigationBarKey, defaultValue = true)
    val songState = database.song(originalSong.id).collectAsState(initial = originalSong)
    val song = songState.value ?: originalSong
    val download by LocalDownloadUtil.current
        .getDownload(originalSong.id)
        .collectAsState(initial = null)
    val coroutineScope = rememberCoroutineScope()
    val syncUtils = LocalSyncUtils.current
    val listenTogetherManager = LocalListenTogetherManager.current
    val scope = rememberCoroutineScope()
    var refetchIconDegree by remember { mutableFloatStateOf(0f) }

    val cacheViewModel = hiltViewModel<CachePlaylistViewModel>()

    val rotationAnimation by animateFloatAsState(
        targetValue = refetchIconDegree,
        animationSpec = tween(durationMillis = 800),
        label = "",
    )

    val (advancedMode) = rememberPreference(AdvancedModeKey, defaultValue = false)

    // Podcast subscription state for episodes
    val podcastEntity by produceState<PodcastEntity?>(initialValue = null, song) {
        val podcastId = song.song.albumId
        if (song.song.isEpisode && podcastId != null) {
            database.podcast(podcastId).collect { value = it }
        }
    }
    val isPodcastSubscribed = podcastEntity?.bookmarkedAt != null

    val orderedArtists by produceState(initialValue = emptyList<ArtistEntity>(), song) {
        withContext(Dispatchers.IO) {
            val artistMaps = database.songArtistMap(song.id).sortedBy { it.position }
            val sorted =
                artistMaps.mapNotNull { map ->
                    song.artists.firstOrNull { it.id == map.artistId }
                }
            value = sorted
        }
    }

    var showChoosePlaylistDialog by rememberSaveable {
        mutableStateOf(false)
    }

    var showErrorPlaylistAddDialog by rememberSaveable {
        mutableStateOf(false)
    }

    AddToPlaylistDialog(
        isVisible = showChoosePlaylistDialog,
        onGetSong = { playlist ->
            coroutineScope.launch(Dispatchers.IO) {
                playlist.playlist.browseId?.let { browseId ->
                    YouTube.addToPlaylist(browseId, song.id)
                }
            }
            listOf(song.id)
        },
        onGetSongIds = { listOf(song.id) },
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
                TextButton(
                    onClick = { showDeleteUploadedDialog = false },
                    enabled = !isDeleting,
                ) {
                    Text(stringResource(R.string.cancel))
                }
                TextButton(
                    onClick = {
                        val entityId = song.song.uploadEntityId
                        if (entityId == null) {
                            Toast
                                .makeText(
                                    context,
                                    R.string.delete_uploaded_song_failed,
                                    Toast.LENGTH_SHORT,
                                ).show()
                            showDeleteUploadedDialog = false
                            return@TextButton
                        }
                        isDeleting = true
                        coroutineScope.launch(Dispatchers.IO) {
                            YouTube
                                .deleteUploadedSong(entityId)
                                .onSuccess {
                                    database.query {
                                        delete(song.song)
                                    }
                                    withContext(Dispatchers.Main) {
                                        Toast
                                            .makeText(
                                                context,
                                                R.string.delete_uploaded_song_success,
                                                Toast.LENGTH_SHORT,
                                            ).show()
                                        isDeleting = false
                                        showDeleteUploadedDialog = false
                                        onDismiss()
                                    }
                                }.onFailure {
                                    withContext(Dispatchers.Main) {
                                        Toast
                                            .makeText(
                                                context,
                                                R.string.delete_uploaded_song_failed,
                                                Toast.LENGTH_SHORT,
                                            ).show()
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
                        Text(
                            text = stringResource(R.string.delete),
                            color = MaterialTheme.colorScheme.error,
                        )
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
            onDismiss = { showSelectArtistDialog = false },
        ) {
            items(
                items = song.artists.distinctBy { it.id },
                key = { "menu_song_artist_${it.id}" },
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

    val bottomSheetPageState = LocalBottomSheetPageState.current
    val configuration = LocalConfiguration.current
    val isPortrait = configuration.orientation == Configuration.ORIENTATION_PORTRAIT
    val isGuest = listenTogetherManager?.isInRoom == true && !listenTogetherManager.isHost

    val primaryActions =
        listOf(
            NewAction(
                icon = {
                    Icon(
                        painter = painterResource(R.drawable.playlist_play),
                        contentDescription = null,
                        modifier = Modifier.size(28.dp),
                        tint = if (newIrideUi) Color.White.copy(alpha = 0.85f) else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
                text = stringResource(R.string.swipe_label_next).lowercase().replaceFirstChar { it.uppercase() },
                onClick = {
                    onDismiss()
                    playerConnection.playNext(song.toMediaItem())
                },
            ),
            NewAction(
                icon = {
                    Icon(
                        painter = painterResource(R.drawable.queue_music),
                        contentDescription = null,
                        modifier = Modifier.size(28.dp),
                        tint = if (newIrideUi) Color.White.copy(alpha = 0.85f) else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
                text = stringResource(R.string.swipe_label_queue).lowercase().replaceFirstChar { it.uppercase() },
                onClick = {
                    onDismiss()
                    playerConnection.addToQueue(song.toMediaItem())
                },
            ),
            NewAction(
                icon = {
                    Icon(
                        painter = painterResource(R.drawable.radio),
                        contentDescription = null,
                        modifier = Modifier.size(28.dp),
                        tint = if (newIrideUi) Color.White.copy(alpha = 0.85f) else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
                text = stringResource(R.string.radio),
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
                val durationMs = if (song.song.duration > 0) song.song.duration.toLong() * 1000 else 180000L
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

            Download.STATE_QUEUED, Download.STATE_DOWNLOADING -> {
                Material3MenuItemData(
                    title = { Text(text = stringResource(R.string.downloading)) },
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
            title = { Text(text = stringResource(R.string.view_artist)) },
            description = { Text(text = song.artists.joinToString { it.name }) },
            icon = {
                val artistThumbnail = song.artists.firstOrNull()?.thumbnailUrl
                if (artistThumbnail != null) {
                    AsyncImage(
                        model = artistThumbnail,
                        contentDescription = null,
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape),
                    )
                } else {
                    Icon(
                        painter = painterResource(R.drawable.artist),
                        contentDescription = null,
                    )
                }
            },
            onClick = {
                if (song.artists.size == 1) {
                    navController.navigate("artist/${song.artists[0].id}")
                    onDismiss()
                } else {
                    showSelectArtistDialog = true
                }
            },
        )

    val isPodcast = song.song.isEpisode

    val viewAlbumItem =
        Material3MenuItemData(
            title = { Text(text = stringResource(if (isPodcast) R.string.view_podcast else R.string.view_album)) },
            description = {
                song.song.albumName?.let {
                    Text(text = it)
                }
            },
            icon = {
                if (song.thumbnailUrl != null) {
                    AsyncImage(
                        model = song.thumbnailUrl,
                        contentDescription = null,
                        modifier = Modifier
                            .size(28.dp)
                            .clip(RoundedCornerShape(4.dp)),
                    )
                } else {
                    Icon(
                        painter = painterResource(if (isPodcast) R.drawable.mic else R.drawable.album),
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

    val subscribeRefetchItems =
        buildList {
            song.song.albumId?.takeIf { song.song.isEpisode }?.let { podcastId ->
                add(
                    Material3MenuItemData(
                        title = {
                            Text(
                                text =
                                    stringResource(
                                        if (isPodcastSubscribed) {
                                            R.string.subscribed
                                        } else {
                                            R.string.subscribe_to_podcast
                                        },
                                    ),
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
                                        },
                                    ),
                                contentDescription = null,
                            )
                        },
                        onClick = {
                            Timber.d("[PODCAST_LIB] Toggling podcast save for: $podcastId")
                            coroutineScope.launch(Dispatchers.IO) {
                                val existingPodcast = podcastEntity
                                val isCurrentlySaved = existingPodcast?.bookmarkedAt != null

                                YouTube
                                    .savePodcast(podcastId, !isCurrentlySaved)
                                    .onSuccess {
                                        Timber.d("[PODCAST_LIB] savePodcast API success!")
                                    }.onFailure { e ->
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
                                                title = song.song.albumName ?: "Unknown Podcast",
                                                author = song.artists.firstOrNull()?.name,
                                                thumbnailUrl = song.song.thumbnailUrl,
                                            ).toggleBookmark(),
                                        )
                                    }
                                }
                            }
                            onDismiss()
                        },
                    ),
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
                    ),
                )
            }
        }

    val detailsItem =
        Material3MenuItemData(
            title = { Text(text = stringResource(R.string.details)) },
            description = { Text(text = stringResource(R.string.details_desc)) },
            icon = {
                Icon(
                    painter = painterResource(R.drawable.info),
                    contentDescription = null,
                )
            },
            onClick = {
                onDismiss()
                bottomSheetPageState.show {
                    ShowMediaInfo(song.id)
                }
            },
        )

    val destructiveItems =
        buildList {
            // Episodes keep "Save for later" (separate Episodes-for-Later playlist, not a library dupe)
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
                                            },
                                        ),
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
                                            },
                                        ),
                                    contentDescription = null,
                                )
                            },
                            onClick = {
                                coroutineScope.launch(Dispatchers.IO) {
                                    val shouldBeSaved = !isEpisodeSaved

                                    // Update local database first (optimistic update)
                                    database.query {
                                        update(
                                            song.song.copy(
                                                inLibrary = if (shouldBeSaved) LocalDateTime.now() else null,
                                                isEpisode = true,
                                            ),
                                        )
                                    }

                                    // Sync with YouTube (handles login check internally)
                                    val setVideoId = if (isEpisodeSaved) database.getSetVideoId(song.id)?.setVideoId else null
                                    syncUtils.saveEpisode(song.id, shouldBeSaved, setVideoId)
                                }
                                onDismiss()
                            },
                        ),
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
                                ),
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
                                        playlistSong?.let { ps ->
                                            val capturedSetVideoId = ps.map.setVideoId
                                            database.transaction {
                                                move(
                                                    ps.map.playlistId,
                                                    ps.map.position,
                                                    Int.MAX_VALUE
                                                )
                                                delete(ps.map.copy(position = Int.MAX_VALUE))
                                            }
                                            playlistBrowseId?.let { browseId ->
                                                syncUtils.scheduleRemoveFromPlaylist(
                                                    browseId,
                                                    ps.map.songId,
                                                    ps.map.playlistId
                                                ) {
                                                    capturedSetVideoId
                                                }
                                            }
                                            onDismiss()
                                        }
                                    },
                                ),
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
                                ),
                            )
                        }
                        // Delete uploaded song option
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
                                ),
                    )
                }
            }

    if (newIrideUi) {
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
                    NewIrideSongMenuHeader(
                        song = song,
                        database = database,
                        syncUtils = syncUtils,
                        context = context,
                        onDismiss = onDismiss,
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    var isEditingNote by rememberSaveable(song.id) { mutableStateOf(false) }
                    val coverWeight by animateFloatAsState(
                        targetValue = if (isEditingNote) 0.0001f else 0.48f,
                        animationSpec = tween(280),
                        label = "coverWeight",
                    )
                    val noteWeight by animateFloatAsState(
                        targetValue = if (isEditingNote) 1f else 0.52f,
                        animationSpec = tween(280),
                        label = "noteWeight",
                    )
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
                                    Modifier
                                        .fillMaxWidth()
                                        .aspectRatio(1f)
                                        .clip(RoundedCornerShape(8.dp)),
                            )
                        }
                        AddNoteBox(
                            song = song,
                            database = database,
                            isEditing = isEditingNote,
                            onEditingChange = { isEditingNote = it },
                            modifier = Modifier.weight(noteWeight),
                        )
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(20.dp)) }

            if (!isGuest) {
                item {
                    NewActionGrid(
                        actions = primaryActions,
                        modifier = Modifier.padding(horizontal = 4.dp),
                    )
                }
                item { Spacer(modifier = Modifier.height(16.dp)) }
            }

            if (listenTogetherManager != null && listenTogetherManager.isInRoom && !listenTogetherManager.isHost) {
                item { Material3MenuGroup(items = listOf(suggestToHostItem)) }
                item { Spacer(modifier = Modifier.height(12.dp)) }
            }

            if (destructiveItems.isNotEmpty()) {
                item { Material3MenuGroup(items = destructiveItems) }
                item { Spacer(modifier = Modifier.height(12.dp)) }
            }

            item {
                ArtistAlbumSwitchRow(
                    song = song,
                    orderedArtists = orderedArtists,
                    onArtistClick = viewArtistItem.onClick ?: {},
                    onAlbumClick = viewAlbumItem.onClick ?: {},
                    modifier = Modifier.padding(horizontal = 4.dp),
                )
            }
            item { Spacer(modifier = Modifier.height(14.dp)) }

            item {
                ProminentActionRow(
                    icon = R.drawable.playlist_add,
                    label = stringResource(R.string.add_to_playlist),
                    onClick = { showChoosePlaylistDialog = true },
                    modifier = Modifier.padding(horizontal = 4.dp),
                )
            }
            item { Spacer(modifier = Modifier.height(10.dp)) }

            item { Material3MenuGroup(items = listOf(downloadItem)) }

            if (subscribeRefetchItems.isNotEmpty()) {
                item { Spacer(modifier = Modifier.height(12.dp)) }
                item { Material3MenuGroup(items = subscribeRefetchItems) }
            }

            item { Spacer(modifier = Modifier.height(12.dp)) }
            item { Material3MenuGroup(items = listOf(detailsItem)) }
        }
    } else {
        SongListItem(
            song = song,
            badges = {},
            showDivider = false,
            trailingContent = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    FavoriteIconButton(song = song, database = database, syncUtils = syncUtils, coroutineScope = coroutineScope, context = context)
                    IconButton(
                        onClick = {
                            onDismiss()
                            val intent =
                                Intent().apply {
                                    action = Intent.ACTION_SEND
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, "https://music.youtube.com/watch?v=${song.id}")
                                }
                            context.startActivity(Intent.createChooser(intent, null))
                        },
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.share),
                            contentDescription = null,
                        )
                    }
                }
            },
        )
        HorizontalDivider()
        Spacer(modifier = Modifier.height(12.dp))

        LazyColumn(
            contentPadding =
                PaddingValues(
                    start = 0.dp,
                    top = 0.dp,
                    end = 0.dp,
                    bottom = 8.dp + WindowInsets.systemBars.asPaddingValues().calculateBottomPadding(),
                ),
        ) {
            if (!isGuest) {
                item {
                    NewActionGrid(
                        actions = primaryActions,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 16.dp),
                    )
                }
            }
            if (listenTogetherManager != null && listenTogetherManager.isInRoom && !listenTogetherManager.isHost) {
                item { Material3MenuGroup(items = listOf(suggestToHostItem)) }
                item { Spacer(modifier = Modifier.height(12.dp)) }
            }

            if (destructiveItems.isNotEmpty()) {
                item { Material3MenuGroup(items = destructiveItems) }
                item { Spacer(modifier = Modifier.height(12.dp)) }
            }

            item { Material3MenuGroup(items = listOf(downloadItem, addToPlaylistItem)) }

            item { Spacer(modifier = Modifier.height(12.dp)) }

            item {
                Material3MenuGroup(
                    items =
                        buildList {
                            if (!song.song.isEpisode) add(viewArtistItem)
                            if (song.song.albumId != null) add(viewAlbumItem)
                            addAll(subscribeRefetchItems)
                        },
                )
            }

            item { Spacer(modifier = Modifier.height(12.dp)) }

            item { Material3MenuGroup(items = listOf(detailsItem)) }
        }
    }
}

// ============================================================================
// New Iride UI song menu building blocks
// ============================================================================

@Composable
private fun FavoriteIconButton(
    song: Song,
    database: MusicDatabase,
    syncUtils: SyncUtils,
    coroutineScope: CoroutineScope,
    context: Context,
    tint: Color = LocalContentColor.current,
    selectedTint: Color = Color(0xFFE53E45),
) {
    val isEpisode = song.song.isEpisode
    val isFavorite = if (isEpisode) song.song.inLibrary != null else song.song.liked
    var optimisticFavorite by remember(isFavorite) { mutableStateOf(isFavorite) }

    IconButton(
        onClick = {
            if (isEpisode) {
                val isCurrentlySaved = song.song.inLibrary != null
                optimisticFavorite = !isCurrentlySaved
                database.query {
                    update(
                        song.song.copy(
                            inLibrary = if (isCurrentlySaved) null else LocalDateTime.now(),
                            isEpisode = true,
                        ),
                    )
                }
                coroutineScope.launch(Dispatchers.IO) {
                    if (isCurrentlySaved) {
                        val setVideoIdEntity = database.getSetVideoId(song.id)
                        val setVideoId = setVideoIdEntity?.setVideoId
                        if (setVideoId != null) {
                            YouTube
                                .removeEpisodeFromSavedEpisodes(song.id, setVideoId)
                                .onSuccess {
                                    Timber.d("[EPISODE_SAVE] Removed episode from Episodes for Later: ${song.id}")
                                }.onFailure { e ->
                                    Timber.e(e, "[EPISODE_SAVE] Failed to remove episode: ${song.id}")
                                    withContext(Dispatchers.Main) {
                                        Toast.makeText(context, R.string.error_episode_remove, Toast.LENGTH_SHORT).show()
                                    }
                                }
                        }
                    } else {
                        YouTube
                            .addEpisodeToSavedEpisodes(song.id)
                            .onSuccess {
                                Timber.d("[EPISODE_SAVE] Saved episode to Episodes for Later: ${song.id}")
                            }.onFailure { e ->
                                Timber.e(e, "[EPISODE_SAVE] Failed to save episode: ${song.id}")
                                withContext(Dispatchers.Main) {
                                    Toast.makeText(context, R.string.error_episode_save, Toast.LENGTH_SHORT).show()
                                }
                            }
                    }
                }
            } else {
                optimisticFavorite = !optimisticFavorite
                val s = song.song.toggleLike()
                database.query { update(s) }
                syncUtils.likeSong(s)
            }
        },
    ) {
        Icon(
            painter = painterResource(if (optimisticFavorite) R.drawable.favorite else R.drawable.favorite_border),
            tint = if (optimisticFavorite) selectedTint else tint,
            contentDescription = null,
        )
    }
}

@Composable
private fun NewIrideSongMenuHeader(
    song: Song,
    database: MusicDatabase,
    syncUtils: SyncUtils,
    context: Context,
    onDismiss: () -> Unit,
) {
    val coroutineScope = rememberCoroutineScope()
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = song.song.title,
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
            if (song.artists.isNotEmpty()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = song.artists.joinToString(", ") { it.name },
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.6f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        FavoriteIconButton(
            song = song,
            database = database,
            syncUtils = syncUtils,
            coroutineScope = coroutineScope,
            context = context,
            tint = Color.White.copy(alpha = 0.85f),
            selectedTint = Color.White,
        )
        IconButton(
            onClick = {
                onDismiss()
                val intent =
                    Intent().apply {
                        action = Intent.ACTION_SEND
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, "https://music.youtube.com/watch?v=${song.id}")
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
private fun StackedArtistAvatars(
    artists: List<ArtistEntity>,
    modifier: Modifier = Modifier,
    size: Dp = 30.dp,
) {
    val visible = artists.take(3)
    if (visible.isEmpty()) {
        Icon(
            painter = painterResource(R.drawable.artist),
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.7f),
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
                contentDescription = null,
                modifier =
                    Modifier
                        .offset(x = step * i)
                        .size(size)
                        .clip(CircleShape)
                        .border(1.5.dp, Color.Black.copy(alpha = 0.5f), CircleShape),
            )
        }
    }
}

@Composable
private fun SwitchTile(
    label: String,
    onClick: () -> Unit,
    leading: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier =
            modifier
                .clip(RoundedCornerShape(14.dp))
                .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)), RoundedCornerShape(14.dp))
                .clickable(onClick = onClick)
                .padding(horizontal = 10.dp, vertical = 10.dp),
    ) {
        leading()
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge.copy(fontFamily = SpaceMonoFontFamily, fontWeight = FontWeight.Bold),
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun ArtistAlbumSwitchRow(
    song: Song,
    orderedArtists: List<ArtistEntity>,
    onArtistClick: () -> Unit,
    onAlbumClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isPodcast = song.song.isEpisode
    // ponytail: no local YTM release-type field exists yet; songCount stands in for the
    // real Album/EP/Single classification. Upgrade to a persisted albumType once available.
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
        if (!isPodcast) {
            SwitchTile(
                label = stringResource(R.string.view_artist),
                onClick = onArtistClick,
                leading = { StackedArtistAvatars(orderedArtists.ifEmpty { song.artists }) },
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
                        modifier = Modifier.size(30.dp).clip(RoundedCornerShape(8.dp)),
                    )
                },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun ProminentActionRow(
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
                .clip(RoundedCornerShape(16.dp))
                .background(Color.White.copy(alpha = 0.1f))
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 16.dp),
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(24.dp),
        )
        Spacer(modifier = Modifier.width(14.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium.copy(fontFamily = SpaceMonoFontFamily, fontWeight = FontWeight.Bold),
            color = Color.White,
        )
    }
}

@Composable
private fun NoteRatingStars(
    rating: Int?,
    modifier: Modifier = Modifier,
    interactive: Boolean = false,
    onRatingChange: (Int) -> Unit = {},
) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = modifier) {
        for (star in 1..5) {
            Box(
                contentAlignment = Alignment.Center,
                modifier =
                    Modifier
                        .size(if (interactive) 28.dp else 16.dp)
                        .then(if (interactive) Modifier.clickable { onRatingChange(star) } else Modifier),
            ) {
                Icon(
                    painter = painterResource(if ((rating ?: 0) >= star) R.drawable.favorite else R.drawable.favorite_border),
                    contentDescription = null,
                    tint = if ((rating ?: 0) >= star) Color.White else Color.White.copy(alpha = 0.25f),
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}

private val noteFieldColors: @Composable () -> androidx.compose.material3.TextFieldColors = {
    TextFieldDefaults.colors(
        focusedContainerColor = Color.Transparent,
        unfocusedContainerColor = Color.Transparent,
        focusedIndicatorColor = Color.Transparent,
        unfocusedIndicatorColor = Color.Transparent,
        cursorColor = Color.White,
        focusedTextColor = Color.White,
        unfocusedTextColor = Color.White,
    )
}

@Composable
private fun AddNoteBox(
    song: Song,
    database: MusicDatabase,
    isEditing: Boolean,
    onEditingChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    var title by remember(song.id) { mutableStateOf(song.song.noteTitle.orEmpty()) }
    var rating by remember(song.id) { mutableStateOf(song.song.noteRating) }
    var ratingText by remember(song.id) { mutableStateOf(song.song.noteRating?.toString().orEmpty()) }
    var description by remember(song.id) { mutableStateOf(song.song.noteText.orEmpty()) }

    // ponytail: writes on every keystroke (cheap local Room update, no debounce). Add a
    // debounce if this ever shows up as jank on a low-end device.
    fun persist() {
        database.query {
            update(
                song.song.copy(
                    noteTitle = title.trim().ifBlank { null },
                    noteRating = rating,
                    noteText = description.trim().ifBlank { null },
                ),
            )
        }
    }

    val isEmpty = title.isBlank() && rating == null && description.isBlank()

    Column(
        modifier =
            modifier
                .clip(RoundedCornerShape(20.dp))
                .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)), RoundedCornerShape(20.dp))
                .then(if (!isEditing) Modifier.clickable { onEditingChange(true) } else Modifier)
                .animateContentSize()
                .padding(16.dp),
    ) {
        when {
            isEditing -> {
                TextField(
                    value = title,
                    onValueChange = { title = it; persist() },
                    placeholder = {
                        Text(stringResource(R.string.note_title_placeholder), color = Color.White.copy(alpha = 0.35f))
                    },
                    singleLine = true,
                    colors = noteFieldColors(),
                    textStyle =
                        MaterialTheme.typography.bodyLarge.copy(
                            fontFamily = SpaceMonoFontFamily,
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
                            val digits = input.filter { it.isDigit() }.take(1)
                            ratingText = digits
                            rating = digits.toIntOrNull()?.coerceIn(1, 5)
                            persist()
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyMedium.copy(color = Color.White, fontFamily = SpaceMonoFontFamily),
                        cursorBrush = SolidColor(Color.White),
                        modifier = Modifier.width(20.dp),
                    )
                    Text(
                        text = "/5",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.6f),
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                TextField(
                    value = description,
                    onValueChange = { description = it; persist() },
                    placeholder = {
                        Text(stringResource(R.string.note_description_placeholder), color = Color.White.copy(alpha = 0.35f))
                    },
                    colors = noteFieldColors(),
                    textStyle = MaterialTheme.typography.bodyMedium.copy(color = Color.White),
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
                        tint = Color.White.copy(alpha = 0.7f),
                        contentDescription = null,
                    )
                }
            }

            isEmpty -> {
                Icon(
                    painter = painterResource(R.drawable.edit),
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.5f),
                    modifier = Modifier.size(20.dp),
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.add_note),
                    style = MaterialTheme.typography.bodyMedium.copy(fontFamily = SpaceMonoFontFamily, fontWeight = FontWeight.Bold),
                    color = Color.White.copy(alpha = 0.5f),
                )
            }

            else -> {
                if (title.isNotBlank()) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyLarge.copy(fontFamily = SpaceMonoFontFamily, fontWeight = FontWeight.Bold),
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                }
                if (rating != null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        NoteRatingStars(rating = rating)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "$rating/5",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.6f),
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                }
                if (description.isNotBlank()) {
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.7f),
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}
