package com.metrolist.music.ui.screens.playlist

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.DownloadRequest
import androidx.media3.exoplayer.offline.DownloadService
import androidx.navigation.NavController
import com.metrolist.music.LocalDatabase
import com.metrolist.music.LocalDownloadUtil
import com.metrolist.music.LocalListenTogetherManager
import com.metrolist.music.LocalPlayerAwareWindowInsets
import com.metrolist.music.LocalPlayerConnection
import com.metrolist.music.R
import com.metrolist.music.constants.HideExplicitKey
import com.metrolist.music.constants.ListThumbnailSize
import com.metrolist.music.constants.ThumbnailCornerRadius
import com.metrolist.music.data.remote.ChartMarket
import com.metrolist.music.db.entities.ArtistEntity
import com.metrolist.music.db.entities.Song
import com.metrolist.music.models.toMediaMetadata
import com.metrolist.music.playback.ExoDownloadService
import com.metrolist.music.repository.ChartTrackStatus
import com.metrolist.music.ui.component.IrideChartPoster
import com.metrolist.music.ui.component.IrideFolio
import com.metrolist.music.ui.component.IrideFolioCollectionSearchBox
import com.metrolist.music.ui.component.IrideFolioRankedSongRow
import com.metrolist.music.ui.component.IrideGeneratedPlaylistHeader
import com.metrolist.music.ui.component.IrideOutlineIconButton
import com.metrolist.music.ui.component.IridePageBackButton
import com.metrolist.music.ui.component.IridePageLayout
import com.metrolist.music.ui.component.IridePlaylistControlPanel
import com.metrolist.music.ui.component.IridePressEffect
import com.metrolist.music.ui.component.ItemThumbnail
import com.metrolist.music.ui.component.ListItem
import com.metrolist.music.ui.component.LocalMenuState
import com.metrolist.music.ui.component.SongListItem
import com.metrolist.music.ui.component.SongMenuIconButton
import com.metrolist.music.ui.component.chartCountryName
import com.metrolist.music.ui.menu.YouTubeSongMenu
import com.metrolist.music.utils.rememberPreference
import com.metrolist.music.viewmodels.DailyChartViewModel
import com.metrolist.music.viewmodels.DailyChartsHomeViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import sv.lib.squircleshape.SquircleShape

@Composable
internal fun chartTitle(market: ChartMarket, count: Int = 100, englishTitle: Boolean = false): String =
    stringResource(R.string.new_charts_playlist_title, chartCountryName(market.code))

internal fun chartDate(date: String) = LocalDate.parse(date).format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))

@Composable
fun DailyChartsHomeShelf(navController: NavController, viewModel: DailyChartsHomeViewModel = hiltViewModel()) {
    NewReleaseChartsShelf(navController)
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DailyChartScreen(navController: NavController, viewModel: DailyChartViewModel = hiltViewModel(), englishTitle: Boolean = false) {
    val charts by viewModel.charts.collectAsStateWithLifecycle()
    val trackStates by viewModel.tracks.collectAsStateWithLifecycle()
    val startingRank by viewModel.startingRank.collectAsStateWithLifecycle()
    val savedPlaylist by viewModel.savedPlaylist.collectAsStateWithLifecycle()
    val saving by viewModel.saving.collectAsStateWithLifecycle()
    val chart = charts[viewModel.market.code]?.chart
    val title = chartTitle(viewModel.market)
    val country = chartCountryName(viewModel.market.code)
    val playerConnection = LocalPlayerConnection.current ?: return
    val listenTogetherManager = LocalListenTogetherManager.current
    val isListenTogetherGuest = listenTogetherManager?.isInRoom == true && !listenTogetherManager.isHost
    val displayedCount = chart?.tracks?.size ?: savedPlaylist?.playlist?.remoteSongCount
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val downloadUtil = LocalDownloadUtil.current
    val downloads by downloadUtil.downloads.collectAsStateWithLifecycle()
    var preparingDownload by remember { mutableStateOf(false) }
    val hideExplicit by rememberPreference(HideExplicitKey, false)
    val mediaMetadata by playerConnection.mediaMetadata.collectAsStateWithLifecycle()
    val isPlaying by playerConnection.isEffectivelyPlaying.collectAsStateWithLifecycle()
    val queueTitle by playerConnection.queueTitle.collectAsStateWithLifecycle()
    val isThisPlaylistPlaying = queueTitle == title && isPlaying
    val bookmarked = savedPlaylist?.playlist?.bookmarkedAt != null
    val resolvedSongs = chart?.tracks.orEmpty().mapNotNull { trackStates[it.spotifyId]?.song }
    val allResolved = chart != null && resolvedSongs.size == chart.tracks.size
    val downloadState = when {
        preparingDownload -> Download.STATE_DOWNLOADING
        allResolved && resolvedSongs.all { downloads[it.id]?.state == Download.STATE_COMPLETED } -> Download.STATE_COMPLETED
        resolvedSongs.any { downloads[it.id]?.state == Download.STATE_DOWNLOADING || downloads[it.id]?.state == Download.STATE_QUEUED } -> Download.STATE_DOWNLOADING
        else -> Download.STATE_STOPPED
    }
    val menuState = LocalMenuState.current
    val database = LocalDatabase.current
    var query by remember { mutableStateOf(TextFieldValue()) }
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val visibleTracks = chart?.tracks.orEmpty().filter { track ->
        query.text.isBlank() || track.title.contains(query.text, true) || track.artists.any { it.contains(query.text, true) }
    }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(lifecycle, viewModel) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (true) { viewModel.refresh(); delay(60_000) }
        }
    }
    IridePageLayout(
        title = title, showPageTitle = false, showTopBar = true,
        bottomPadding = LocalPlayerAwareWindowInsets.current.asPaddingValues().calculateBottomPadding(),
        navigationIcon = { IridePageBackButton(navController) },
        actions = {
            IrideOutlineIconButton(onClick = viewModel::toggleBookmark,
                icon = if (bookmarked) R.drawable.favorite else R.drawable.favorite_border,
                contentDescription = stringResource(if (bookmarked) R.string.remove_from_library else R.string.add_to_library),
                enabled = chart != null && !saving, size = 40.dp, iconSize = 20.dp, pressEffect = IridePressEffect.Punch)
        },
    ) {
        item(key = "playlist_header") {
            IrideGeneratedPlaylistHeader(title, emptyList(),
                metadataLine = displayedCount?.let { pluralStringResource(R.plurals.n_song, it, it) }.orEmpty(),
                artwork = {
                    if (displayedCount != null) {
                        IrideChartPoster(viewModel.market.code, country,
                            stringResource(R.string.new_charts_top_count, displayedCount), Modifier.fillMaxSize())
                    } else Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceContainer))
                })
        }
        if (chart != null) {
            item(key = "control_panel") {
                IridePlaylistControlPanel(
                    onShuffleClick = {
                        if (!isListenTogetherGuest) viewModel.play(null, title, hideExplicit, shuffle = true) { playerConnection.playQueue(it) }
                    },
                    onPlayClick = {
                        if (!isListenTogetherGuest) {
                            if (queueTitle == title) playerConnection.togglePlayPause()
                            else viewModel.play(null, title, hideExplicit) { playerConnection.playQueue(it) }
                        }
                    },
                    onDownloadClick = {
                        if (!preparingDownload) scope.launch {
                            if (downloadState == Download.STATE_COMPLETED || downloadState == Download.STATE_DOWNLOADING) {
                                resolvedSongs.forEach { song -> DownloadService.sendRemoveDownload(context, ExoDownloadService::class.java, song.id, false) }
                            } else {
                                preparingDownload = true
                                try {
                                    viewModel.resolveDownloadSongs().filterNot { hideExplicit && it.explicit }.forEach { song ->
                                        val request = DownloadRequest.Builder(song.id, song.id.toUri()).setCustomCacheKey(song.id)
                                            .setData(song.title.toByteArray()).build()
                                        DownloadService.sendAddDownload(context, ExoDownloadService::class.java, request, false)
                                    }
                                } catch (cancelled: CancellationException) { throw cancelled }
                                catch (error: Exception) { timber.log.Timber.w(error, "Could not prepare chart download") }
                                finally { preparingDownload = false }
                            }
                        }
                    }, downloadState = downloadState, isPlaying = isThisPlaylistPlaying, modifier = Modifier.padding(bottom = 12.dp))
            }
            item(key = "search_bar") {
                IrideFolioCollectionSearchBox(query, onQueryChange = { query = it },
                    placeholderText = stringResource(R.string.search_playlists), focusRequester = focusRequester,
                    onFocusChanged = {}, onSearch = { focusManager.clearFocus() }, onClear = { query = TextFieldValue() },
                    modifier = Modifier.padding(horizontal = IrideFolio.HorizontalPadding, vertical = 8.dp))
            }
        }
        items(visibleTracks, key = { it.spotifyId }) { track ->
            val trackState = trackStates[track.spotifyId]
            val song = trackState?.song
            val hidden = hideExplicit && song?.explicit == true
            val onPlay = {
                if (!isListenTogetherGuest) {
                    if (song != null && song.id == mediaMetadata?.id) playerConnection.togglePlayPause()
                    else viewModel.play(track.rank, title, hideExplicit) { playerConnection.playQueue(it) }
                }
            }
            val onMenu = {
                if (song != null) menuState.show(anchored = true) {
                    YouTubeSongMenu(song, navController, onDismiss = menuState::dismiss)
                }
            }
            LaunchedEffect(track.spotifyId, lifecycle) {
                lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                    while (true) {
                        viewModel.loadTrack(track)
                        delay(60_000)
                    }
                }
            }
            IrideFolioRankedSongRow(rank = track.rank, movement = track.movement) {
                if (song != null) {
                    val librarySong by remember(song.id) { database.song(song.id) }.collectAsStateWithLifecycle(initialValue = null)
                    val displaySong = librarySong ?: remember(song) {
                        Song(
                            song = song.toMediaMetadata().toSongEntity(),
                            artists = song.artists.mapNotNull { artist -> artist.id?.let { ArtistEntity(id = it, name = artist.name) } },
                        )
                    }
                    SongListItem(
                        song = displaySong,
                        subtitleOverride = if (displaySong.artists.isEmpty()) song.artists.joinToString { it.name } else null,
                        subtitleColor = Color.Unspecified,
                        isActive = song.id == mediaMetadata?.id,
                        isPlaying = isPlaying,
                        isSwipeable = !hidden,
                        trailingContent = {
                            SongMenuIconButton(onClick = onMenu) {
                                Icon(painterResource(R.drawable.more_vert), stringResource(R.string.menu))
                            }
                        },
                        modifier = Modifier.fillMaxWidth().combinedClickable(
                            enabled = !hidden,
                            onClick = onPlay,
                            onLongClick = onMenu,
                        ),
                    )
                } else {
                    ListItem(
                        title = track.title,
                        subtitle = track.artists.joinToString(),
                        subtitleColor = Color.Unspecified,
                        thumbnailContent = {
                            ItemThumbnail(
                                thumbnailUrl = null,
                                isActive = false,
                                isPlaying = false,
                                shape = SquircleShape(radius = ThumbnailCornerRadius, cornerSmoothing = 0.5f),
                                modifier = Modifier.size(ListThumbnailSize),
                                softBorder = true,
                            )
                        },
                        trailingContent = {
                            if (trackState?.status == ChartTrackStatus.LOADING || startingRank == track.rank) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                            } else {
                                SongMenuIconButton(onClick = {}, enabled = false) {
                                    Icon(painterResource(R.drawable.more_vert), stringResource(R.string.menu))
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth().clickable(onClick = onPlay),
                    )
                }
            }
        }
    }
}
