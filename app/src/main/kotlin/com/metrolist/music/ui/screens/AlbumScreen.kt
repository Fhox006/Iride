/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.screens

/*import android.graphics.Bitmap*/
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import com.metrolist.music.ui.component.frostedTopBarBackground
import com.metrolist.music.ui.component.recordFrostBackdrop
import com.metrolist.music.ui.component.rememberFrostBackdrop
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
/*import androidx.compose.ui.draw.alpha*/
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import com.metrolist.music.ui.theme.SpaceMonoFontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withLink
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastForEachIndexed
import androidx.compose.ui.util.fastForEachReversed
import androidx.compose.ui.util.lerp
import sv.lib.squircleshape.SquircleShape
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.TextButton
import androidx.core.net.toUri
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.DownloadRequest
import androidx.media3.exoplayer.offline.DownloadService
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import coil3.compose.AsyncImagePainter
import coil3.decode.DataSource
import coil3.imageLoader
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.toBitmap
import com.metrolist.music.LocalDatabase
import com.metrolist.music.LocalDownloadUtil
import com.metrolist.music.LocalListenTogetherManager
import com.metrolist.music.LocalPlayerAwareWindowInsets
import com.metrolist.music.LocalPlayerConnection
import com.metrolist.music.R
import com.metrolist.music.constants.AlbumTopGradientKey
import com.metrolist.music.constants.HideDurationForStandardSongsKey
import com.metrolist.music.constants.HideExplicitKey
import com.metrolist.music.constants.HideVideoSongsKey
import com.metrolist.music.constants.IrideBaseBorderWidth
import com.metrolist.music.constants.PlayerBackgroundStyle
import com.metrolist.music.constants.PlayerBackgroundStyleKey
import com.metrolist.music.db.entities.Album
import com.metrolist.music.db.entities.AlbumPlayEvent
import com.metrolist.music.extensions.toMediaItem
import com.metrolist.music.models.MediaMetadata
import com.metrolist.music.playback.ExoDownloadService
import com.metrolist.music.playback.queues.ListQueue
import com.metrolist.music.playback.queues.LocalAlbumRadio
/*import com.metrolist.music.ui.component.AnimatedAlbumGradientBackground*/
import com.metrolist.music.ui.component.DefaultDialog
import com.metrolist.music.ui.component.IconButton
import com.metrolist.music.ui.component.IridePlaylistControlPanel
import com.metrolist.music.ui.component.IridePressEffect
import com.metrolist.music.ui.component.IrideLoadingIndicator
import com.metrolist.music.ui.component.IrideOutlineIconButton
import com.metrolist.music.ui.component.LocalMenuState
import com.metrolist.music.ui.component.NavigationTitle
import com.metrolist.music.ui.component.SongListItem
import com.metrolist.music.ui.component.TopScreenGradientBackground
import com.metrolist.music.ui.component.NeedleDropLeadInMs
import com.metrolist.music.ui.component.TypewriterText
import com.metrolist.music.ui.component.VinylPeekDisc
import com.metrolist.music.ui.component.YouTubeGridItem
import com.metrolist.music.ui.component.rememberRubberBandPull
import com.metrolist.music.ui.component.rubberBandOverscroll
import com.metrolist.music.utils.rememberEnumPreference
import com.metrolist.music.ui.menu.AlbumMenu
import com.metrolist.music.ui.menu.SelectionSongMenu
import com.metrolist.music.ui.menu.SongMenu
import com.metrolist.music.ui.menu.YouTubeAlbumMenu
import com.metrolist.music.ui.utils.IrideMotion
import com.metrolist.music.ui.utils.backToMain
import com.metrolist.music.ui.utils.headerEnter
import com.metrolist.music.ui.utils.irideEnter
import com.metrolist.music.ui.utils.irideEnterScale
import com.metrolist.music.ui.utils.rememberDiscreteProgress
import com.metrolist.music.ui.utils.rememberEnterProgress
import com.metrolist.music.ui.utils.rememberSectionEnter
import com.metrolist.music.ui.utils.revealMask
import com.metrolist.music.utils.GenreProvider
import com.metrolist.music.utils.TurntableSfx
import com.metrolist.music.utils.joinByBullet
import com.metrolist.music.utils.makeReadableTimeString
import com.metrolist.music.utils.makeTimeString
import com.metrolist.music.utils.rememberPreference
import com.metrolist.music.viewmodels.AlbumViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.exp

// Turntable feel for the cover-peeking disc: how far it slides out from behind the cover, how much
// the cover itself steps aside to make room, how fast it spins, and how much track time one full
// revolution represents while scratching (see the drag handler below). The two speeds are one
// setting in two units and must stay in step — 360 / DegreesPerSecond has to equal
// MsPerRevolution / 1000, otherwise a free-spinning disc and a dragged disc disagree about how
// much music a turn is worth, which is exactly what makes a scratch feel fake. Half real 33⅓ RPM:
// a literal 1.8s/turn read as frantic on a phone-sized disc.
private const val AlbumDiscPeekFraction = 0.30f
private const val AlbumCoverShiftFraction = 0.12f
private const val AlbumDiscDegreesPerSecond = 100f
private const val AlbumDiscMsPerRevolution = 3600L
private const val AlbumDiscFrictionPerSecond = 3.2f
private const val AlbumDiscCoastSettleEpsilon = 0.02
// Touch events can land only a few ms apart (batched input), and dividing a normal angle delta
// by a near-zero dtMs spikes the instantaneous velocity estimate absurdly high. Clamping caps how
// far off a single noisy sample can seed the release coast — without it, one bad sample right
// before release could make the coast (and so the audio settling back to 1x) take seconds.
private const val AlbumDiscMaxScratchVelocity = 12.0
// A finger resting on the platter reports no drag events at all, so silence for this long means
// "held", not "still moving at the last speed I heard about" — without it the music kept running
// under a stopped finger.
private const val AlbumDiscHoldTimeoutMs = 70L
// Slow, unhurried glide — the disc easing out from behind the cover is a small ceremony, not a
// snap; EaseOutExpo's long soft landing reads as calm at this length where it'd read as sluggish
// at the shared IrideMotion.Short/Medium durations used for on-screen movement elsewhere.
private const val AlbumDiscRevealMs = 900

private fun angleOfTouch(position: Offset, center: Offset): Float {
    val dx = position.x - center.x
    val dy = position.y - center.y
    return Math.toDegrees(atan2(dy, dx).toDouble()).toFloat()
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AlbumScreen(
    navController: NavController,
    viewModel: AlbumViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val menuState = LocalMenuState.current
    val database = LocalDatabase.current
    val haptic = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()
    val playerConnection = LocalPlayerConnection.current ?: return
    val listenTogetherManager = LocalListenTogetherManager.current
    val isListenTogetherGuest = listenTogetherManager?.let { it.isInRoom && !it.isHost } ?: false

    val scope = rememberCoroutineScope()

    val isPlaying by playerConnection.isEffectivelyPlaying.collectAsStateWithLifecycle()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsStateWithLifecycle()

    val playlistId by viewModel.playlistId.collectAsStateWithLifecycle()
    val albumWithSongs by viewModel.albumWithSongs.collectAsStateWithLifecycle()
    val otherVersions by viewModel.otherVersions.collectAsStateWithLifecycle()
    val similarAlbums by viewModel.similarAlbums.collectAsStateWithLifecycle()
    val hasError by viewModel.hasError.collectAsStateWithLifecycle()
    val hideExplicit by rememberPreference(key = HideExplicitKey, defaultValue = false)
    val hideVideoSongs by rememberPreference(key = HideVideoSongsKey, defaultValue = false)
    val albumTopGradientEnabled by rememberPreference(AlbumTopGradientKey, defaultValue = true)
    val lazyListState = rememberLazyListState()
    val unseenSongIds by viewModel.unseenSongIds.collectAsState()
    // Songs render in one plain Column (not lazily virtualized, see below), so a song's own
    // composition can't be used as a "seen" proxy — this tracks the LazyColumn's actual on-screen
    // bounds so each row can check itself against it.
    var listViewportBounds by remember { mutableStateOf<Rect?>(null) }
    val frostBackdrop = rememberFrostBackdrop()
    // Stretch of the vertical rubber band, hoisted so the header art can answer the pull.
    val headerPull = rememberRubberBandPull()

    // Screen's own one-shot "have I landed" flag: the header title types out only once, and the top
    // bar's glass may only turn on after a real frame exists to blur (frostBackdrop's GraphicsLayer
    // isn't saveable, so navigating back to an already-scrolled album must wait for one real frame).
    var headerRevealed by rememberSaveable { mutableStateOf(false) }
    // Sections that have already played their entrance — LazyColumn disposes items scrolled far off
    // screen, so without this a shelf would replay its wipe-in every time it scrolled back into view.
    val revealedSections = remember { mutableSetOf<String>() }

    // Window-space Y of the header title's bottom edge and of the top bar's bottom edge — the bar's
    // glass and mirrored title fade in exactly when the big title goes behind the bar (same crossing
    // ArtistScreen uses), and tweens in/out at a fixed duration instead of following the scroll
    // pixel-by-pixel.
    var nameBottomPx by remember { mutableStateOf(Float.MAX_VALUE) }
    var topBarBottomPx by remember { mutableStateOf(0f) }
    val headerTitleCovered by remember {
        derivedStateOf {
            headerRevealed && (
                lazyListState.firstVisibleItemIndex > 0 ||
                    nameBottomPx <= topBarBottomPx
                )
        }
    }
    val topBarRevealProgress = rememberDiscreteProgress(headerTitleCovered)

    val albumTitle = albumWithSongs?.album?.title
    // Everything in the header waits for the title to finish typing, so the block reads as one
    // sentence being composed rather than several things appearing at once.
    val titleTypingMs = remember(albumTitle) {
        val length = albumTitle?.length ?: 0
        if (length == 0) 0 else minOf(26 * length, 700)
    }
    LaunchedEffect(albumTitle) {
        if (albumTitle != null && !headerRevealed) {
            delay(titleTypingMs + 60L + IrideMotion.Short)
            headerRevealed = true
        }
    }

    // The whole screen arrives rather than being slapped down: covers the gap between navigation and
    // first layout.
    val screenProgress = rememberEnterProgress(play = true, durationMillis = IrideMotion.Short, easing = IrideMotion.EaseOutQuart)
    val playerBackgroundStyle by rememberEnumPreference(
        PlayerBackgroundStyleKey,
        defaultValue = PlayerBackgroundStyle.BETTER_ANIMATED_GRADIENT,
    )

    val filteredSongs =
        remember(albumWithSongs, hideExplicit, hideVideoSongs) {
            var songs = albumWithSongs?.songs ?: emptyList()
            if (hideExplicit) {
                songs = songs.filter { !it.song.explicit }
            }
            if (hideVideoSongs) {
                songs = songs.filter { !it.song.isVideo }
            }
            songs
        }

    val recentAlbumPlayEvents by produceState<List<AlbumPlayEvent>>(initialValue = emptyList(), albumWithSongs?.album?.id) {
        val albumId = albumWithSongs?.album?.id
        if (albumId == null) {
            value = emptyList()
        } else {
            database.recentAlbumPlayEvents(albumId).collect { value = it }
        }
    }
    val resumeTrackIndex = remember(recentAlbumPlayEvents, albumWithSongs) {
        val songCount = albumWithSongs?.songs?.size ?: 0
        var inOrderStreak = 0
        for (i in recentAlbumPlayEvents.indices) {
            inOrderStreak = if (i == 0) {
                1
            } else if (recentAlbumPlayEvents[i - 1].songIndex - recentAlbumPlayEvents[i].songIndex == 1) {
                inOrderStreak + 1
            } else {
                break
            }
        }
        val lastPlayedIndex = recentAlbumPlayEvents.firstOrNull()?.songIndex
        if (inOrderStreak >= 3 && lastPlayedIndex != null) {
            (lastPlayedIndex + 1).takeIf { it in 0 until songCount }
        } else {
            null
        }
    }
    var resumeDismissed by rememberSaveable(albumWithSongs?.album?.id) { mutableStateOf(false) }
    val isThisAlbumQueueLoaded = mediaMetadata?.album?.id == albumWithSongs?.album?.id
    val isThisAlbumPlaying = isPlaying && isThisAlbumQueueLoaded

    var inSelectMode by rememberSaveable { mutableStateOf(false) }
    val selection =
        rememberSaveable(
            saver =
                listSaver<MutableList<String>, String>(
                    save = { it.toList() },
                    restore = { it.toMutableStateList() },
                ),
        ) { mutableStateListOf() }
    val onExitSelectionMode = {
        inSelectMode = false
        selection.clear()
    }
    if (inSelectMode) {
        BackHandler(onBack = onExitSelectionMode)
    }

    LaunchedEffect(filteredSongs) {
        selection.fastForEachReversed { songId ->
            if (filteredSongs.find { it.id == songId } == null) {
                selection.remove(songId)
            }
        }
    }

    val downloadUtil = LocalDownloadUtil.current
    var downloadState by remember {
        mutableIntStateOf(Download.STATE_STOPPED)
    }
    var showRemoveDownloadDialog by remember { mutableStateOf(false) }

    // Control-panel pill (shuffle/play/download) — New Iride UI body panel, mirrors
    // LocalPlaylistScreen's IridePlaylistControlPanel usage.
    val onControlPanelPlayClick: () -> Unit = {
        if (!isListenTogetherGuest) {
            if (isThisAlbumQueueLoaded) {
                playerConnection.togglePlayPause()
            } else {
                albumWithSongs?.let { current ->
                    TurntableSfx.play()
                    playerConnection.service.getAutomix(playlistId)
                    coroutineScope.launch {
                        delay(NeedleDropLeadInMs)
                        playerConnection.playQueue(LocalAlbumRadio(current))
                    }
                }
            }
        }
    }
    val onControlPanelShuffleClick: () -> Unit = {
        albumWithSongs?.let { current ->
            playerConnection.playQueue(
                ListQueue(
                    title = current.album.title,
                    items = current.songs.shuffled().map { it.toMediaItem() },
                ),
            )
        }
    }
    val onControlPanelDownloadClick: () -> Unit = {
        albumWithSongs?.let { current ->
            when (downloadState) {
                Download.STATE_COMPLETED -> showRemoveDownloadDialog = true
                Download.STATE_DOWNLOADING, Download.STATE_QUEUED -> {
                    current.songs.forEach { song ->
                        DownloadService.sendRemoveDownload(
                            context,
                            ExoDownloadService::class.java,
                            song.id,
                            false,
                        )
                    }
                }
                else -> {
                    current.songs.forEach { song ->
                        val downloadRequest = DownloadRequest
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
                }
            }
        }
    }

    LaunchedEffect(albumWithSongs) {
        val songs = albumWithSongs?.songs?.map { it.id }
        if (songs.isNullOrEmpty()) return@LaunchedEffect
        downloadUtil.downloads.collect { downloads ->
            downloadState =
                if (songs.all { downloads[it]?.state == Download.STATE_COMPLETED }) {
                    Download.STATE_COMPLETED
                } else if (songs.all {
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

    /*
    var albumThumbnailBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var gradientReady by remember { mutableStateOf(false) }
    val gradientAlpha by animateFloatAsState(
        targetValue = if (gradientReady) 1f else 0f,
        animationSpec = tween(durationMillis = 800),
        label = "gradientAlpha"
    )

    LaunchedEffect(albumWithSongs?.album?.thumbnailUrl) {
        gradientReady = false
        albumThumbnailBitmap = null
        val url = albumWithSongs?.album?.thumbnailUrl ?: return@LaunchedEffect
        val request = ImageRequest.Builder(context)
            .data(url)
            .size(100, 100)
            .allowHardware(false)
            .build()
        val result = context.imageLoader.execute(request)
        albumThumbnailBitmap = result.image?.toBitmap()
        gradientReady = true
    }
    */

    if (showRemoveDownloadDialog) {
        DefaultDialog(
            onDismiss = { showRemoveDownloadDialog = false },
            content = {
                Text(
                    text = stringResource(
                        R.string.remove_download_playlist_confirm,
                        albumWithSongs?.album?.title ?: "",
                    ),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(horizontal = 18.dp),
                )
            },
            buttons = {
                TextButton(onClick = { showRemoveDownloadDialog = false }) {
                    Text(text = stringResource(android.R.string.cancel))
                }
                TextButton(
                    onClick = {
                        showRemoveDownloadDialog = false
                        albumWithSongs?.songs?.forEach { song ->
                            DownloadService.sendRemoveDownload(
                                context,
                                ExoDownloadService::class.java,
                                song.id,
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

    val albumGradientMediaMetadata = remember(albumWithSongs?.album?.id, albumWithSongs?.album?.thumbnailUrl) {
        albumWithSongs?.album?.let {
            MediaMetadata(id = it.id, title = it.title, artists = emptyList(), duration = 0, thumbnailUrl = it.thumbnailUrl)
        }
    }

    // Everything the frosted top bar sees through its "glass" is captured here: the gradient belongs
    // inside the snapshot too, otherwise the bar blurs the song list over a hard-edged gradient.
    // Contents left at their original indentation — wrapping alone, no reflow.
    Box(
        modifier = Modifier
            .fillMaxSize()
            .recordFrostBackdrop(frostBackdrop)
            // Fade only, covering the gap between navigation and first layout — a scale-down here
            // would inset edge-to-edge content and show a hairline of window background at the edges.
            .graphicsLayer { alpha = screenProgress },
    ) {

    if (albumTopGradientEnabled) {
        TopScreenGradientBackground(
            mediaMetadata = albumGradientMediaMetadata,
            playerBackground = playerBackgroundStyle,
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .onGloballyPositioned { listViewportBounds = it.boundsInWindow() }
            .rubberBandOverscroll(Orientation.Vertical, lazyListState, headerPull),
        state = lazyListState,
        contentPadding = LocalPlayerAwareWindowInsets.current.asPaddingValues(),
    ) {
            val albumWithSongs = albumWithSongs
            if (albumWithSongs != null && albumWithSongs.songs.isNotEmpty()) {
                item(key = "album_header") {
                    val albumCoverSquircle = SquircleShape(radius = 12.dp, cornerSmoothing = 0.45f)

                    // Shared across both layouts below.
                    val totalDuration = albumWithSongs.songs.sumOf { it.song.duration }
                    val releaseDate = albumWithSongs.album.releaseDate
                    val displayDate = remember(releaseDate, albumWithSongs.album.year) {
                        if (releaseDate == null) {
                            albumWithSongs.album.year?.toString()
                        } else {
                            val parts = releaseDate.split("-")
                            when (parts.size) {
                                3 -> {
                                    val y = parts[0]
                                    val m = parts[1].toInt().toString()
                                    val d = parts[2].toInt().toString()
                                    "$d/$m/$y"
                                }
                                2 -> {
                                    val y = parts[0]
                                    val m = parts[1].toInt().toString()
                                    "$m/$y"
                                }
                                else -> parts[0]
                            }
                        }
                    }
                    val albumGenre by produceState<String?>(initialValue = null, albumWithSongs.album.id) {
                        val primaryArtistName = albumWithSongs.artists.firstOrNull()?.name
                        value = GenreProvider.getGenres(
                            albumWithSongs.album.id,
                            albumWithSongs.album.title,
                            primaryArtistName,
                        ).firstOrNull()
                    }
                    val metadataLine = joinByBullet(
                        albumGenre,
                        displayDate,
                        if (totalDuration > 0) makeReadableTimeString(totalDuration * 1000L) else null,
                    )
                    val onLikeClick: () -> Unit = {
                        database.query {
                            update(albumWithSongs.album.toggleLike())
                        }
                    }
                    val onPlayClick: () -> Unit = {
                        if (!isListenTogetherGuest) {
                            TurntableSfx.play()
                            playerConnection.service.getAutomix(playlistId)
                            coroutineScope.launch {
                                delay(NeedleDropLeadInMs)
                                playerConnection.playQueue(
                                    LocalAlbumRadio(albumWithSongs),
                                )
                            }
                        }
                    }
                    val onDownloadClick: () -> Unit = {
                        when (downloadState) {
                            Download.STATE_COMPLETED -> showRemoveDownloadDialog = true
                            Download.STATE_DOWNLOADING, Download.STATE_QUEUED -> {
                                albumWithSongs.songs.forEach { song ->
                                    DownloadService.sendRemoveDownload(
                                        context,
                                        ExoDownloadService::class.java,
                                        song.id,
                                        false,
                                    )
                                }
                            }
                            else -> {
                                albumWithSongs.songs.forEach { song ->
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
                            }
                        }
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp)
                            .padding(top = 12.dp, bottom = 20.dp),
                    ) {
                            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                // A memory-cache hit (revisiting an album already seen this session)
                                // resolves synchronously — animating that in over 420ms would replay
                                // the surfaceVariant placeholder every time. Only a genuinely new
                                // decode plays the entrance.
                                var coverLoaded by remember(albumWithSongs.album.id) { mutableStateOf(false) }
                                var skipCoverEnterAnim by remember(albumWithSongs.album.id) { mutableStateOf(false) }
                                val animatedCoverProgress = rememberEnterProgress(
                                    play = coverLoaded,
                                    durationMillis = 420,
                                    easing = IrideMotion.EaseOutQuart,
                                )
                                val coverProgress = if (skipCoverEnterAnim) 1f else animatedCoverProgress

                                // Disc sits fully hidden behind the cover until this album is the one
                                // playing (or being scratched): then it peeks out to the right and
                                // spins, and the cover steps aside to make room for it.
                                var discRotation by remember(albumWithSongs.album.id) { mutableFloatStateOf(0f) }
                                var isScratchingDisc by remember(albumWithSongs.album.id) { mutableStateOf(false) }
                                val discOut = isThisAlbumPlaying || isScratchingDisc

                                // Leaving the screen mid-gesture kills the coast coroutine with it, so the
                                // platter would be left stopped or reversed and the music with it. The
                                // engine's speed belongs to the engine, not to this composition.
                                DisposableEffect(Unit) {
                                    onDispose { playerConnection.service.scratchProcessor.setVelocity(1.0) }
                                }
                                val discRevealProgress by animateFloatAsState(
                                    targetValue = if (discOut) 1f else 0f,
                                    animationSpec = tween(AlbumDiscRevealMs, easing = IrideMotion.EaseOutExpo),
                                    label = "albumDiscReveal",
                                )

                                LaunchedEffect(albumWithSongs.album.id, isThisAlbumPlaying, isScratchingDisc) {
                                    if (!isThisAlbumPlaying || isScratchingDisc) return@LaunchedEffect
                                    var lastFrame = withFrameNanos { it }
                                    while (true) {
                                        withFrameNanos { now ->
                                            val deltaSec = (now - lastFrame) / 1_000_000_000f
                                            lastFrame = now
                                            discRotation = (discRotation + deltaSec * AlbumDiscDegreesPerSecond) % 360f
                                        }
                                    }
                                }

                                VinylPeekDisc(
                                    thumbnailUrl = albumWithSongs.album.thumbnailUrl,
                                    size = 240.dp,
                                    rotationDegrees = discRotation,
                                    modifier = Modifier
                                        .offset(x = 240.dp * AlbumDiscPeekFraction * discRevealProgress)
                                        .graphicsLayer { alpha = discRevealProgress }
                                        .pointerInput(albumWithSongs.album.id) {
                                            var center = Offset.Zero
                                            var lastAngle = 0f
                                            var lastEventUptimeMillis = 0L
                                            var lastVelocity = 1.0
                                            var coastJob: Job? = null
                                            var holdWatchJob: Job? = null

                                            // Release is not a hand-back: the platter is the playback engine, so all that
                                            // happens is the same friction curve the disc coasts on being fed to the engine
                                            // as its speed until it reaches 1x. Nothing is confirmed, no position is
                                            // recomputed, playback simply carries on from wherever the head ended up.
                                            fun startCoast() {
                                                holdWatchJob?.cancel()
                                                coastJob = coroutineScope.launch {
                                                    val scratch = playerConnection.service.scratchProcessor
                                                    var velocity = lastVelocity
                                                    var lastFrame = withFrameNanos { it }
                                                    while (abs(velocity - 1.0) > AlbumDiscCoastSettleEpsilon) {
                                                        withFrameNanos { now ->
                                                            val dtSec = (now - lastFrame) / 1_000_000_000.0
                                                            lastFrame = now
                                                            velocity = 1.0 + (velocity - 1.0) * exp(-AlbumDiscFrictionPerSecond * dtSec)
                                                            val deltaDegrees = (velocity * dtSec * 1000.0 / AlbumDiscMsPerRevolution * 360f).toFloat()
                                                            discRotation = (discRotation + deltaDegrees + 360f) % 360f
                                                            scratch.setVelocity(velocity)
                                                        }
                                                    }
                                                    scratch.setVelocity(1.0)
                                                    isScratchingDisc = false
                                                }
                                            }

                                            detectDragGestures(
                                                onDragStart = { startPosition ->
                                                    if (discRevealProgress < 0.4f) return@detectDragGestures
                                                    coastJob?.cancel()
                                                    holdWatchJob?.cancel()
                                                    center = Offset(size.width / 2f, size.height / 2f)
                                                    lastAngle = angleOfTouch(startPosition, center)
                                                    lastEventUptimeMillis = 0L
                                                    lastVelocity = 1.0
                                                    isScratchingDisc = true
                                                    // Touching the platter grabs it, like a hand landing on a record.
                                                    playerConnection.service.scratchProcessor.setVelocity(0.0)
                                                    holdWatchJob = coroutineScope.launch {
                                                        while (true) {
                                                            delay(AlbumDiscHoldTimeoutMs)
                                                            val idleMs = android.os.SystemClock.uptimeMillis() - lastEventUptimeMillis
                                                            if (lastEventUptimeMillis != 0L && idleMs >= AlbumDiscHoldTimeoutMs) {
                                                                lastVelocity = 0.0
                                                                playerConnection.service.scratchProcessor.setVelocity(0.0)
                                                            }
                                                        }
                                                    }
                                                },
                                                onDrag = { change, _ ->
                                                    if (!isScratchingDisc) return@detectDragGestures
                                                    val angle = angleOfTouch(change.position, center)
                                                    var delta = angle - lastAngle
                                                    if (delta > 180f) delta -= 360f
                                                    if (delta < -180f) delta += 360f
                                                    lastAngle = angle
                                                    discRotation = (discRotation + delta + 360f) % 360f
                                                    val scratch = playerConnection.service.scratchProcessor
                                                    val dtMs = change.uptimeMillis - lastEventUptimeMillis
                                                    if (lastEventUptimeMillis != 0L && dtMs > 0) {
                                                        val instVelocity = ((delta / 360f * AlbumDiscMsPerRevolution) / dtMs)
                                                            .toDouble()
                                                            .coerceIn(-AlbumDiscMaxScratchVelocity, AlbumDiscMaxScratchVelocity)
                                                        scratch.setVelocity(instVelocity)
                                                        lastVelocity = lastVelocity + (instVelocity - lastVelocity) * 0.35
                                                    }
                                                    lastEventUptimeMillis = change.uptimeMillis
                                                },
                                                onDragEnd = { startCoast() },
                                                onDragCancel = { startCoast() },
                                            )
                                        },
                                )

                                AsyncImage(
                                    model = albumWithSongs.album.thumbnailUrl,
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    onState = { state ->
                                        if (state is AsyncImagePainter.State.Success) {
                                            if (state.result.dataSource == DataSource.MEMORY_CACHE) {
                                                skipCoverEnterAnim = true
                                            }
                                            coverLoaded = true
                                        }
                                    },
                                    modifier = Modifier
                                        .size(240.dp)
                                        .graphicsLayer {
                                            alpha = coverProgress
                                            val s = lerp(0.94f, 1f, coverProgress)
                                            scaleX = s
                                            scaleY = s
                                            translationX = -240.dp.toPx() * AlbumCoverShiftFraction * discRevealProgress
                                        }
                                        .shadow(
                                            elevation = 20.dp,
                                            shape = albumCoverSquircle,
                                            spotColor = Color.Black.copy(alpha = 0.5f),
                                        )
                                        .clip(albumCoverSquircle)
                                        .background(MaterialTheme.colorScheme.surfaceVariant)
                                        .border(BorderStroke(IrideBaseBorderWidth, Color.White.copy(alpha = 0.22f)), albumCoverSquircle),
                                )
                            }

                            Spacer(modifier = Modifier.height(20.dp))

                            TypewriterText(
                                text = albumWithSongs.album.title,
                                style = TextStyle(
                                    fontFamily = SpaceMonoFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 20.sp,
                                    letterSpacing = (-0.2).sp,
                                ),
                                color = MaterialTheme.colorScheme.onBackground,
                                // Keyed on the album, not the string: recomposing this screen must
                                // not retype the title.
                                resetKey = albumWithSongs.album.id,
                                // Types on the first landing only — coming back to the top of the
                                // page (e.g. scrolling back up) is navigation, not an arrival.
                                animate = !headerRevealed,
                                maxLines = 3,
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .onGloballyPositioned { nameBottomPx = it.boundsInWindow().bottom },
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            val artistRowProgress = headerEnter(
                                revealed = headerRevealed,
                                play = true,
                                delayMillis = titleTypingMs + 20,
                                durationMillis = IrideMotion.Short,
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .revealMask(artistRowProgress),
                            ) {
                                val primaryArtist = albumWithSongs.artists.firstOrNull()
                                if (primaryArtist?.thumbnailUrl != null) {
                                    AsyncImage(
                                        model = primaryArtist.thumbnailUrl,
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .size(18.dp)
                                            .clip(CircleShape),
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                }
                                Text(
                                    buildAnnotatedString {
                                        albumWithSongs.artists.fastForEachIndexed { index, artist ->
                                            val link =
                                                LinkAnnotation.Clickable(
                                                    tag = artist.id,
                                                    // Underline only while held — "release to open artist screen".
                                                    styles = TextLinkStyles(
                                                        style = SpanStyle(textDecoration = TextDecoration.None),
                                                        pressedStyle = SpanStyle(textDecoration = TextDecoration.Underline),
                                                    ),
                                                ) {
                                                    navController.navigate("artist/${artist.id}")
                                                }
                                            withLink(link) {
                                                append(artist.name)
                                            }
                                            if (index != albumWithSongs.artists.lastIndex) {
                                                append(", ")
                                            }
                                        }
                                    },
                                    style = TextStyle(fontFamily = SpaceMonoFontFamily, fontSize = 13.sp),
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.75f),
                                )
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            val metadataProgress = headerEnter(
                                revealed = headerRevealed,
                                play = true,
                                delayMillis = titleTypingMs + 40,
                                durationMillis = IrideMotion.Short,
                            )
                            Text(
                                text = metadataLine,
                                style = TextStyle(fontFamily = SpaceMonoFontFamily, fontSize = 13.sp),
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f),
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .revealMask(metadataProgress),
                            )
                            // Like/play live in the top bar now; download lives behind its ⋯ overflow.
                        }
                }

                item(key = "control_panel") {
                    val controlPanelProgress = headerEnter(
                        revealed = headerRevealed,
                        play = true,
                        delayMillis = titleTypingMs + 60,
                        durationMillis = IrideMotion.Short,
                    )
                    IridePlaylistControlPanel(
                        onShuffleClick = onControlPanelShuffleClick,
                        onPlayClick = onControlPanelPlayClick,
                        onDownloadClick = onControlPanelDownloadClick,
                        downloadState = downloadState,
                        isPlaying = isThisAlbumPlaying,
                        modifier = Modifier
                            .padding(bottom = 12.dp)
                            .irideEnterScale(controlPanelProgress),
                    )
                }

                if (resumeTrackIndex != null) {
                    item(key = "resume_banner") {
                        AnimatedVisibility(
                            visible = !resumeDismissed && !isThisAlbumPlaying,
                            enter = fadeIn() + expandVertically(),
                            exit = fadeOut() + shrinkVertically(),
                        ) {
                            val resumeShape = SquircleShape(radius = 12.dp, cornerSmoothing = 0.45f)
                            // New Iride UI: hairline-bordered console panel, no filled surface —
                            // matches IntegrationCard/NewMenuComponents' flat monochrome vocabulary.
                            Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 8.dp)
                                        .clip(resumeShape)
                                        .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.14f)), resumeShape)
                                        .padding(horizontal = 16.dp, vertical = 14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = stringResource(R.string.resume_album),
                                            style = MaterialTheme.typography.labelLarge.copy(
                                                fontFamily = SpaceMonoFontFamily,
                                                letterSpacing = (-0.1).sp,
                                            ),
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                        )
                                        Text(
                                            text = stringResource(
                                                R.string.resume_album_track_progress,
                                                resumeTrackIndex + 1,
                                                albumWithSongs?.songs?.size ?: 0,
                                            ),
                                            style = TextStyle(fontFamily = SpaceMonoFontFamily, fontSize = 13.sp),
                                            color = Color.White.copy(alpha = 0.55f),
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    OutlinedButton(
                                        onClick = {
                                            val album = albumWithSongs
                                            resumeDismissed = true
                                            if (!isListenTogetherGuest && album != null) {
                                                playerConnection.service.getAutomix(playlistId)
                                                playerConnection.playQueue(
                                                    LocalAlbumRadio(album, startIndex = resumeTrackIndex),
                                                )
                                            }
                                        },
                                        shape = RoundedCornerShape(8.dp),
                                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.3f)),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                                    ) {
                                        Text(
                                            text = stringResource(R.string.resume),
                                            style = TextStyle(fontFamily = SpaceMonoFontFamily, fontSize = 13.sp),
                                            fontWeight = FontWeight.Bold,
                                        )
                                    }
                                }
                        }
                    }
                }

                if (filteredSongs.isNotEmpty()) {
                    item(key = "songs_container") {
                        val hideDurationForStandard by rememberPreference(HideDurationForStandardSongsKey, defaultValue = true)
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                                .animateItem(placementSpec = IrideMotion.PlacementSpec)
                                .revealMask(rememberSectionEnter("songs", revealedSections))
                        ) {
                                filteredSongs.fastForEachIndexed { index, song ->
                                    val onCheckedChange: (Boolean) -> Unit = {
                                        if (it) {
                                            selection.add(song.id)
                                        } else {
                                            selection.remove(song.id)
                                        }
                                    }

                                    val featuredNames = song.orderedArtists.drop(1).map { it.name }
                                    val hideDuration = hideDurationForStandard && song.song.duration in 60..300
                                    val subtitleText = buildString {
                                        if (featuredNames.isNotEmpty()) {
                                            append("feat. ")
                                            append(featuredNames.joinToString(", "))
                                        }
                                        if (!hideDuration) {
                                            if (isNotEmpty()) append(" • ")
                                            append(makeTimeString(song.song.duration * 1000L))
                                        }
                                    }

                                    SongListItem(
                                        song = song,
                                        albumIndex = index + 1,
                                        subtitleOverride = subtitleText,
                                        showNewMarker = song.id in unseenSongIds,
                                        // New Iride UI: featuring-artist credits ("feat. X") should
                                        // read in the same color as the rest of the row instead of
                                        // the default muted secondary tone.
                                        subtitleColor = Color.Unspecified,
                                        isActive = song.id == mediaMetadata?.id,
                                        isPlaying = isPlaying,
                                        trailingContent = {
                                            if (inSelectMode) {
                                                Checkbox(
                                                    checked = song.id in selection,
                                                    onCheckedChange = onCheckedChange,
                                                )
                                            } else {
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
                                                        contentDescription = null,
                                                    )
                                                }
                                            }
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .then(
                                                if (song.id in unseenSongIds) {
                                                    Modifier.onGloballyPositioned { coords ->
                                                        val viewport = listViewportBounds ?: return@onGloballyPositioned
                                                        if (coords.boundsInWindow().overlaps(viewport)) {
                                                            viewModel.markSongSeen(song.id)
                                                        }
                                                    }
                                                } else {
                                                    Modifier
                                                },
                                            )
                                            .combinedClickable(
                                                onClick = {
                                                    if (inSelectMode) {
                                                        onCheckedChange(song.id !in selection)
                                                    } else if (!isListenTogetherGuest) {
                                                        if (song.id == mediaMetadata?.id) {
                                                            playerConnection.togglePlayPause()
                                                        } else {
                                                            playerConnection.service.getAutomix(playlistId)
                                                            playerConnection.playQueue(
                                                                LocalAlbumRadio(albumWithSongs, startIndex = index),
                                                            )
                                                        }
                                                    }
                                                },
                                                onLongClick = {
                                                    if (!inSelectMode) {
                                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                        inSelectMode = true
                                                        onCheckedChange(true)
                                                    }
                                                },
                                            ),
                                    )
                                }
                        }
                    }
                }

                if (otherVersions.isNotEmpty()) {
                    item(key = "other_versions_title") {
                        NavigationTitle(
                            title = stringResource(R.string.other_versions),
                            modifier = Modifier
                                .animateItem(placementSpec = IrideMotion.PlacementSpec)
                                .revealMask(rememberSectionEnter("other_versions", revealedSections)),
                            useIrideStyle = true,
                        )
                    }
                    item(key = "other_versions_list") {
                        val rowState = rememberLazyListState()
                        LazyRow(
                            state = rowState,
                            overscrollEffect = null,
                            horizontalArrangement = Arrangement.spacedBy(0.dp),
                            contentPadding = PaddingValues(horizontal = 20.dp),
                            modifier = Modifier
                                .irideEnter(rememberSectionEnter("other_versions_row", revealedSections), 10.dp)
                                .rubberBandOverscroll(Orientation.Horizontal, rowState),
                        ) {
                            items(
                                items = otherVersions.distinctBy { it.id },
                                key = { "album_other_${it.id}" },
                            ) { item ->
                                YouTubeGridItem(
                                    item = item,
                                    isActive = mediaMetadata?.album?.id == item.id,
                                    isPlaying = isPlaying,
                                    coroutineScope = scope,
                                    modifier =
                                        Modifier
                                            .combinedClickable(
                                                onClick = { navController.navigate("album/${item.id}") },
                                                onLongClick = {
                                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                    menuState.show {
                                                        YouTubeAlbumMenu(
                                                            albumItem = item,
                                                            navController = navController,
                                                            onDismiss = menuState::dismiss,
                                                        )
                                                    }
                                                },
                                            ).animateItem(placementSpec = IrideMotion.PlacementSpec),
                                )
                            }
                        }
                    }
                }

                if (similarAlbums.isNotEmpty()) {
                    item(key = "similar_albums_title") {
                        NavigationTitle(
                            title = stringResource(R.string.similar_albums),
                            modifier = Modifier.animateItem(),
                            useIrideStyle = true,
                        )
                    }
                    item(key = "similar_albums_list") {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(0.dp),
                            contentPadding = PaddingValues(horizontal = 20.dp),
                        ) {
                            items(
                                items = similarAlbums.distinctBy { it.id },
                                key = { "album_similar_${it.id}" },
                            ) { item ->
                                YouTubeGridItem(
                                    item = item,
                                    isActive = mediaMetadata?.album?.id == item.id,
                                    isPlaying = isPlaying,
                                    coroutineScope = scope,
                                    modifier =
                                        Modifier
                                            .combinedClickable(
                                                onClick = { navController.navigate("album/${item.id}") },
                                                onLongClick = {
                                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                    menuState.show {
                                                        YouTubeAlbumMenu(
                                                            albumItem = item,
                                                            navController = navController,
                                                            onDismiss = menuState::dismiss,
                                                        )
                                                    }
                                                },
                                            ).animateItem(placementSpec = IrideMotion.PlacementSpec),
                                )
                            }
                        }
                    }
                }
            } else {
                if (hasError) {
                    item(key = "error") {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(32.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = stringResource(R.string.error_title),
                                    style = MaterialTheme.typography.bodyLarge,
                                )
                                Spacer(Modifier.height(12.dp))
                                Button(onClick = viewModel::retry) {
                                    Text(stringResource(R.string.retry))
                                }
                            }
                        }
                    }
                } else {
                    item(key = "loading") {
                        Box(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            IrideLoadingIndicator()
                        }
                    }
                }
            }
        }

    }

    // Top-bar mirrors of the header's like/play actions (New Iride UI only) — the header versions
    // close over a non-null local `albumWithSongs`, these close over the nullable top-level state so
    // they're safe to call before the album has loaded.
    val onTopBarLikeClick: () -> Unit = {
        albumWithSongs?.let { current ->
            database.query {
                update(current.album.toggleLike())
            }
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
                onClick = { navController.navigateUp() },
                onLongClick = { navController.backToMain() },
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
                checked = selection.size == filteredSongs.size && selection.isNotEmpty(),
                onCheckedChange = {
                    if (selection.size == filteredSongs.size) {
                        selection.clear()
                    } else {
                        selection.clear()
                        selection.addAll(filteredSongs.map { it.id })
                    }
                },
            )
            IconButton(
                enabled = selection.isNotEmpty(),
                onClick = {
                    menuState.show {
                        SelectionSongMenu(
                            songSelection =
                                selection.mapNotNull { songId ->
                                    filteredSongs.find { it.id == songId }
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
        } else {
            val currentAlbumWithSongs = albumWithSongs
            if (currentAlbumWithSongs != null) {
                val albumForMenu = Album(currentAlbumWithSongs.album, currentAlbumWithSongs.artists)
                IconButton(
                    onClick = {
                        menuState.show {
                            AlbumMenu(
                                originalAlbum = albumForMenu,
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
            }
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .onGloballyPositioned { topBarBottomPx = it.boundsInWindow().bottom }
                .frostedTopBarBackground(
                    progress = topBarRevealProgress,
                    barColor = MaterialTheme.colorScheme.background,
                    strokeColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f),
                    backdrop = frostBackdrop,
                )
                .statusBarsPadding()
                .height(56.dp)
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val backProgress = rememberEnterProgress(play = true, durationMillis = IrideMotion.Short)
            Box(modifier = Modifier.irideEnterScale(backProgress, from = 0.8f)) {
                topBarNavigationIcon()
            }
            // Always composed and always holding its weight — the title fades in via
            // topBarRevealProgress, tracking the big header title going behind this bar.
            Text(
                text = if (inSelectMode) {
                    pluralStringResource(R.plurals.n_selected, selection.size, selection.size)
                } else {
                    albumWithSongs?.album?.title.orEmpty()
                },
                style = TextStyle(
                    fontFamily = SpaceMonoFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    letterSpacing = (-0.1).sp,
                ),
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 4.dp)
                    .then(
                        if (inSelectMode) {
                            Modifier
                        } else {
                            Modifier.irideEnter(topBarRevealProgress, 6.dp).revealMask(topBarRevealProgress)
                        },
                    ),
            )
            if (!inSelectMode && albumWithSongs != null) {
                val actionsProgress = rememberEnterProgress(play = true, durationMillis = IrideMotion.Short)
                IrideOutlineIconButton(
                    onClick = onTopBarLikeClick,
                    icon = if (albumWithSongs?.album?.bookmarkedAt != null) R.drawable.favorite else R.drawable.favorite_border,
                    contentDescription = stringResource(
                        if (albumWithSongs?.album?.bookmarkedAt != null) R.string.remove_from_library else R.string.add_to_library,
                    ),
                    size = 40.dp,
                    iconSize = 20.dp,
                    pressEffect = IridePressEffect.Punch,
                    modifier = Modifier.irideEnterScale(actionsProgress),
                )
            }
            // Download now lives behind the ⋯ overflow (AlbumMenu already has its own download item)
            // — three buttons shoulder to shoulder was the same crowding ArtistScreen solved by
            // moving its secondary actions behind an overflow instead of shrinking targets further.
            topBarActions()
        }
}