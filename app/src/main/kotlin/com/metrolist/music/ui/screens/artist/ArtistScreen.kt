/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.screens.artist

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import com.metrolist.music.ui.theme.SpaceMonoFontFamily
import com.metrolist.music.ui.theme.textPrimary
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastForEach
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import coil3.compose.AsyncImage
import coil3.decode.DataSource
import com.metrolist.innertube.YouTube
import com.metrolist.innertube.models.AlbumItem
import com.metrolist.innertube.models.ArtistItem
import com.metrolist.innertube.models.EpisodeItem
import com.metrolist.innertube.models.PlaylistItem
import com.metrolist.innertube.models.PodcastItem
import com.metrolist.innertube.models.SongItem
import com.metrolist.innertube.models.WatchEndpoint
import com.metrolist.music.LocalDatabase
import com.metrolist.music.LocalListenTogetherManager
import com.metrolist.music.LocalPlayerAwareWindowInsets
import com.metrolist.music.LocalPlayerConnection
import com.metrolist.music.R
import com.metrolist.music.constants.AppBarHeight
import com.metrolist.music.constants.HideExplicitKey
import com.metrolist.music.constants.ShowArtistDescriptionKey
import com.metrolist.music.constants.ShowArtistSubscriberCountKey
import com.metrolist.music.constants.ShowMonthlyListenersKey
import com.metrolist.music.db.entities.Album
import com.metrolist.music.db.entities.toAlbumEntity
import com.metrolist.music.extensions.toMediaItem
import com.metrolist.music.models.toMediaMetadata
import com.metrolist.music.playback.queues.ListQueue
import com.metrolist.music.playback.queues.YouTubeQueue
import com.metrolist.music.ui.component.AlbumGridItem
import com.metrolist.music.ui.component.ItemThumbnail
import com.metrolist.music.ui.component.ExpandableText
import com.metrolist.music.ui.component.IconButton
import com.metrolist.music.ui.component.IrideOutlineIconButton
import com.metrolist.music.ui.component.IrideSegmentedToggle
import com.metrolist.music.ui.component.LinkSegment
import com.metrolist.music.ui.component.LocalMenuState
import com.metrolist.music.ui.component.NavigationTitle
import com.metrolist.music.ui.component.NewAction
import com.metrolist.music.ui.component.NewActionGrid
import com.metrolist.music.ui.component.SongCarousel
import com.metrolist.music.ui.component.SongListItem
import com.metrolist.music.ui.component.YouTubeGridItem
import com.metrolist.music.ui.component.YouTubeListItem
import com.metrolist.music.ui.component.frostedTopBarBackground
import com.metrolist.music.ui.component.recordFrostBackdrop
import com.metrolist.music.ui.component.rememberFrostBackdrop
import com.metrolist.music.ui.component.shimmer.ListItemPlaceHolder
import com.metrolist.music.ui.component.shimmer.ShimmerHost
import com.metrolist.music.ui.component.shimmer.TextPlaceholder
import com.metrolist.music.ui.menu.AlbumMenu
import com.metrolist.music.ui.menu.SongMenu
import com.metrolist.music.ui.menu.YouTubeArtistMenu
import com.metrolist.music.ui.menu.YouTubePlaylistMenu
import com.metrolist.music.ui.menu.YouTubeSongMenu
import com.metrolist.music.ui.utils.backToMain
import com.metrolist.music.ui.utils.fadingEdge
import com.metrolist.music.ui.utils.resize
import com.metrolist.music.utils.rememberPreference
import com.metrolist.music.viewmodels.AlbumReleaseType
import com.metrolist.music.viewmodels.ArtistViewModel
import com.valentinilk.shimmer.shimmer
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.util.lerp
import coil3.compose.AsyncImagePainter
import com.metrolist.music.ui.component.IridePressEffect
import com.metrolist.music.ui.component.TypewriterText
import com.metrolist.music.ui.component.rememberRubberBandPull
import com.metrolist.music.ui.component.rubberBandOverscroll
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import com.metrolist.music.ui.utils.IrideMotion
import com.metrolist.music.ui.utils.grainOverlay
import com.metrolist.music.ui.utils.headerEnter
import com.metrolist.music.ui.utils.irideEnter
import com.metrolist.music.ui.utils.irideEnterScale
import com.metrolist.music.ui.utils.pressScale
import com.metrolist.music.ui.utils.rememberDiscreteProgress
import com.metrolist.music.ui.utils.rememberEnterProgress
import com.metrolist.music.ui.utils.rememberGrainBrush
import com.metrolist.music.ui.utils.rememberReducedMotion
import com.metrolist.music.ui.utils.rememberSectionEnter
import com.metrolist.music.ui.utils.revealMask
import sv.lib.squircleshape.SquircleShape

/**
 * Overflow sheet for the top bar's three-dot button: game / radio / shuffle / share, the four
 * buttons the bar used to hold shoulder to shoulder. Each `on*` param is null when that action
 * doesn't apply (offline branch, no endpoint, guest room) — same gating the inline buttons used to
 * do themselves, just read once here instead of duplicated per button.
 */
@Composable
private fun ColumnScope.ArtistOverflowMenu(
    onGame: () -> Unit,
    onRadio: (() -> Unit)?,
    onShuffle: (() -> Unit)?,
    onShare: (() -> Unit)?,
    onDismiss: () -> Unit,
) {
    fun icon(@androidx.annotation.DrawableRes res: Int): @Composable () -> Unit = {
        Icon(
            painter = painterResource(res),
            contentDescription = null,
            modifier = Modifier.size(28.dp),
            tint = MaterialTheme.colorScheme.textPrimary,
        )
    }

    val actions = buildList {
        add(
            NewAction(
                icon = icon(R.drawable.game_controller),
                text = stringResource(R.string.guess_game),
                onClick = { onDismiss(); onGame() },
            ),
        )
        onRadio?.let { action ->
            add(
                NewAction(
                    icon = icon(R.drawable.radio),
                    text = stringResource(R.string.radio),
                    onClick = { onDismiss(); action() },
                ),
            )
        }
        onShuffle?.let { action ->
            add(
                NewAction(
                    icon = icon(R.drawable.shuffle),
                    text = stringResource(R.string.shuffle),
                    onClick = { onDismiss(); action() },
                ),
            )
        }
        onShare?.let { action ->
            add(
                NewAction(
                    icon = icon(R.drawable.share),
                    text = stringResource(R.string.share),
                    onClick = { onDismiss(); action() },
                ),
            )
        }
    }
    NewActionGrid(
        actions = actions,
        columns = 2,
        modifier = Modifier.padding(vertical = 16.dp),
    )
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ArtistScreen(
    navController: NavController,
    viewModel: ArtistViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val database = LocalDatabase.current
    val menuState = LocalMenuState.current
    val haptic = LocalHapticFeedback.current
    val uriHandler = LocalUriHandler.current
    val coroutineScope = rememberCoroutineScope()
    val playerConnection = LocalPlayerConnection.current ?: return
    val listenTogetherManager = LocalListenTogetherManager.current
    val isGuest = listenTogetherManager?.isInRoom == true && !listenTogetherManager.isHost
    val isPlaying by playerConnection.isEffectivelyPlaying.collectAsState()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()
    val artistPage = viewModel.artistPage
    val libraryArtist by viewModel.libraryArtist.collectAsState()
    val librarySongs by viewModel.librarySongs.collectAsState()
    val libraryAlbums by viewModel.libraryAlbums.collectAsState()
    val isChannelSubscribed by viewModel.isChannelSubscribed.collectAsState()
    val recentAlbum by viewModel.recentAlbum.collectAsState()
    val recentAlbumPreciseDate by viewModel.recentAlbumPreciseDate.collectAsState()
    val expandedTopSongs by viewModel.expandedTopSongs.collectAsState()
    val hideExplicit by rememberPreference(key = HideExplicitKey, defaultValue = false)
    val showArtistDescription by rememberPreference(key = ShowArtistDescriptionKey, defaultValue = true)
    val showArtistSubscriberCount by rememberPreference(key = ShowArtistSubscriberCountKey, defaultValue = true)
    val showMonthlyListeners by rememberPreference(key = ShowMonthlyListenersKey, defaultValue = true)
    val imageAspectRatio = 0.94f
    val irideHorizontalPadding = 20.dp

    val albumsTitles = remember(artistPage) {
        artistPage?.sections
            ?.filter { it.title.contains("Album", ignoreCase = true) }
            ?.flatMap { it.items }
            ?.filterIsInstance<AlbumItem>()
            ?.map { it.title.lowercase().trim() }
            ?.toSet() ?: emptySet()
    }

    val essentialAlbums = remember(artistPage, expandedTopSongs) {
        val albumSection = artistPage?.sections?.firstOrNull { section ->
            section.title.contains("Album", ignoreCase = true) &&
                !section.title.contains("Single", ignoreCase = true) &&
                !section.title.contains("EP", ignoreCase = true)
        }
        val albums = albumSection?.items?.filterIsInstance<AlbumItem>() ?: emptyList()
        val isFamous = hasLargeAudience(artistPage?.monthlyListenerCount) ||
            hasLargeAudience(artistPage?.subscriberCountText)
        if (albums.size < 8 || !isFamous) {
            emptyList()
        } else {
            val topSongsSection = artistPage?.sections?.firstOrNull { section ->
                (section.items.firstOrNull() as? SongItem)?.album != null
            }
            val shelfTopSongs = topSongsSection?.items?.filterIsInstance<SongItem>() ?: emptyList()
            val topSongs = expandedTopSongs?.takeIf { it.size > shelfTopSongs.size } ?: shelfTopSongs

            val albumsById = albums.associateBy { it.id }
            val albumsByTitle = albums.associateBy { it.title.lowercase().trim() }
            val ranked = LinkedHashSet<AlbumItem>()
            topSongs.forEach { song ->
                val album = song.album?.id?.let(albumsById::get)
                    ?: song.album?.name?.lowercase()?.trim()?.let(albumsByTitle::get)
                album?.let(ranked::add)
            }
            albums.forEach(ranked::add)
            ranked.take(4).sortedByDescending { it.year ?: Int.MIN_VALUE }
        }
    }

    val lazyListState = rememberLazyListState()
    val snackbarHostState = remember { SnackbarHostState() }
    var showLocal by rememberSaveable { mutableStateOf(false) }
    val librarySourceIndicatorOffset by animateDpAsState(
        targetValue = if (!showLocal) 2.dp else 42.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium,
        ),
        label = "artistSourceIndicator",
    )
    val density = LocalDensity.current

    val systemBarsTopPadding = WindowInsets.systemBars.asPaddingValues().calculateTopPadding()
    val headerOffset =
        with(density) {
            -(systemBarsTopPadding + AppBarHeight).roundToPx()
        }

    val transparentAppBar by remember {
        derivedStateOf {
            lazyListState.firstVisibleItemIndex == 0 && lazyListState.firstVisibleItemScrollOffset < 100
        }
    }
    var headerRevealed by rememberSaveable { mutableStateOf(false) }
    val frostBackdrop = rememberFrostBackdrop()

    val headerPull = rememberRubberBandPull()
    val grainBrush = rememberGrainBrush()

    val revealedSections = remember(showLocal) { mutableSetOf<String>() }

    val screenProgress = rememberEnterProgress(play = true, durationMillis = IrideMotion.Short, easing = IrideMotion.EaseOutQuart)

    val artistName = artistPage?.artist?.title ?: libraryArtist?.artist?.name
    val nameTypingMs = remember(artistName) {
        val length = artistName?.length ?: 0
        if (length == 0) 0 else minOf(26 * length, 700)
    }
    var nameBottomPx by remember { mutableStateOf(Float.MAX_VALUE) }
    var topBarBottomPx by remember { mutableStateOf(0f) }
    val headerTitleCovered by remember {
        derivedStateOf {
            if (!headerRevealed) return@derivedStateOf false
            // Fallback to scroll offset when header is off-screen (nameBottom disposed)
            // and require a bit more scroll (100px like transparentAppBar) to avoid
            // flicker after a few pixels; this keeps frost stable even after the
            // header item is recycled.
            headerRevealed && (
                lazyListState.firstVisibleItemIndex > 0 ||
                    lazyListState.firstVisibleItemScrollOffset > 100 ||
                    nameBottomPx <= topBarBottomPx
                )
        }
    }
    val topBarRevealProgress = rememberDiscreteProgress(headerTitleCovered)
    LaunchedEffect(artistName) {
        if (artistName != null && !headerRevealed) {
            delay(nameTypingMs + 140L + IrideMotion.Medium)
            headerRevealed = true
        }
    }

    LaunchedEffect(libraryArtist) {
        showLocal = libraryArtist?.artist?.isLocal == true
    }

    var showLocalInitialized by remember { mutableStateOf(false) }
    LaunchedEffect(showLocal) {
        if (showLocalInitialized) {
            lazyListState.animateScrollToItem(0)
        } else {
            showLocalInitialized = true
        }
    }

    val artistLoading = artistPage == null && !showLocal

    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer { alpha = screenProgress },
    ) {
        LazyColumn(
            modifier = Modifier
                .recordFrostBackdrop(frostBackdrop)
                .rubberBandOverscroll(Orientation.Vertical, lazyListState, headerPull),
            state = lazyListState,
            overscrollEffect = null,
            contentPadding = LocalPlayerAwareWindowInsets.current.asPaddingValues(),
        ) {
            item(key = "header") {
                    val thumbnail = artistPage?.artist?.thumbnail ?: libraryArtist?.artist?.thumbnailUrl
                    val reducedMotion = rememberReducedMotion()
                    val showRecentAlbumPanel = !showLocal && recentAlbum != null
                    val gradientHeightPx = with(density) { 160.dp.roundToPx() }

                    BoxWithConstraints(
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        val imageHeightPx = if (thumbnail != null) {
                            with(density) { (maxWidth.toPx() * imageAspectRatio).roundToInt() }
                        } else {
                            0
                        }
                        if (thumbnail != null) {
                            var imageLoaded by remember(thumbnail) { mutableStateOf(false) }
                            var skipImageEnterAnim by remember(thumbnail) { mutableStateOf(false) }
                            val animatedImageProgress = rememberEnterProgress(
                                play = imageLoaded,
                                durationMillis = 520,
                                easing = IrideMotion.EaseOutQuart,
                            )
                            val imageProgress = if (skipImageEnterAnim) 1f else animatedImageProgress
                            Box(
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .aspectRatio(imageAspectRatio)
                                        .offset {
                                            val parallax = if (
                                                lazyListState.firstVisibleItemIndex == 0
                                            ) {
                                                (lazyListState.firstVisibleItemScrollOffset * 0.35f).roundToInt()
                                            } else {
                                                0
                                            }
                                            IntOffset(x = 0, y = headerOffset + parallax)
                                        }
                                        .then(
                                            Modifier.graphicsLayer {
                                                    val stretch = if (size.height > 0f) {
                                                        (headerPull.offset / size.height)
                                                            .coerceIn(0f, 0.6f)
                                                    } else {
                                                        0f
                                                    }
                                                    val s = lerp(1.06f, 1f, imageProgress) + stretch
                                                    scaleX = s
                                                    scaleY = s
                                                    transformOrigin = TransformOrigin(0.5f, 1f)
                                                }
                                        )
                                        .background(MaterialTheme.colorScheme.surfaceVariant),
                            ) {
                                AsyncImage(
                                    model = thumbnail.resize(1200, 1200),
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    onState = { state ->
                                        if (state is AsyncImagePainter.State.Success) {
                                            if (state.result.dataSource == DataSource.MEMORY_CACHE) {
                                                skipImageEnterAnim = true
                                            }
                                            imageLoaded = true
                                        }
                                    },
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .graphicsLayer { alpha = imageProgress }
                                        .grainOverlay(grainBrush),
                                )
                                val bgColor = MaterialTheme.colorScheme.background
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(160.dp)
                                        .align(Alignment.BottomCenter)
                                        .background(
                                            Brush.verticalGradient(
                                                colors = listOf(Color.Transparent, bgColor),
                                            )
                                        ),
                                )
                            }
                        }

                        Column(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .then(
                                        if (thumbnail != null) {
                                            Modifier.padding(
                                                top = with(density) {
                                                    (imageHeightPx - gradientHeightPx).coerceAtLeast(0).toDp()
                                                },
                                            )
                                        } else {
                                            Modifier.padding(top = 16.dp)
                                        },
                                    )
                                    .then(
                                        if (reducedMotion) {
                                            Modifier
                                        } else {
                                            Modifier.animateContentSize(
                                                tween(IrideMotion.Medium, easing = IrideMotion.EaseOutQuart),
                                            )
                                        },
                                    ),
                        ) {
                            Column(
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = irideHorizontalPadding),
                            ) {
                                val irideNameStyle = TextStyle(
                                    fontFamily = SpaceMonoFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 28.sp,
                                    letterSpacing = (-0.3).sp,
                                )
                                if (artistName != null) {
                                    val shuffleEndpointForPlay = artistPage?.artist?.shuffleEndpoint
                                    val canShufflePlay = shuffleEndpointForPlay != null || librarySongs.isNotEmpty()
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 6.dp)
                                            .onGloballyPositioned { nameBottomPx = it.boundsInWindow().bottom },
                                    ) {
                                        TypewriterText(
                                            text = artistName,
                                            style = irideNameStyle,
                                            color = MaterialTheme.colorScheme.onBackground,
                                            resetKey = viewModel.artistId,
                                            animate = !headerRevealed,
                                            maxLines = 2,
                                            modifier = Modifier.weight(1f),
                                        )
                                        if (canShufflePlay) {
                                            val playProgress = headerEnter(
                                                revealed = headerRevealed,
                                                play = true,
                                                delayMillis = nameTypingMs + 20,
                                                durationMillis = IrideMotion.Short,
                                            )
                                            Box(
                                                modifier = Modifier
                                                    .padding(start = 10.dp)
                                                    .irideEnterScale(playProgress, from = 0.7f)
                                                    .clip(SquircleShape(radius = 10.dp, cornerSmoothing = 0.48f))
                                                    .background(Color.Black.copy(alpha = 0.28f)),
                                            ) {
                                                IrideOutlineIconButton(
                                                    onClick = {
                                                        if (!isGuest) {
                                                            val endpoint = shuffleEndpointForPlay
                                                            if (endpoint != null) {
                                                                playerConnection.playQueue(YouTubeQueue(endpoint))
                                                            } else if (librarySongs.isNotEmpty()) {
                                                                playerConnection.playQueue(
                                                                    ListQueue(
                                                                        title = artistName,
                                                                        items = librarySongs.shuffled().map { it.toMediaItem() },
                                                                        startIndex = 0,
                                                                    ),
                                                                )
                                                            }
                                                        }
                                                    },
                                                    icon = R.drawable.play,
                                                    contentDescription = stringResource(R.string.shuffle),
                                                    tint = Color.White,
                                                    size = 38.dp,
                                                    iconSize = 18.dp,
                                                    pressEffect = IridePressEffect.Punch,
                                                )
                                            }
                                        }
                                    }
                                } else {
                                    Spacer(
                                        modifier = Modifier.height(
                                            with(density) { (28f * 1.2f).sp.toDp() } + 6.dp,
                                        ),
                                    )
                                }

                                if (libraryAlbums.isNotEmpty() || librarySongs.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    val toggleProgress = headerEnter(
                                        revealed = headerRevealed,
                                        play = artistName != null,
                                        delayMillis = nameTypingMs + 40,
                                        durationMillis = IrideMotion.Short,
                                    )
                                    IrideSegmentedToggle(
                                        modifier = Modifier.irideEnter(toggleProgress, 6.dp),
                                        options = listOf(
                                            false to stringResource(R.string.online),
                                            true to stringResource(R.string.filter_library),
                                        ),
                                        selected = showLocal,
                                        onSelect = { value ->
                                            if (value != showLocal) {
                                                showLocal = value
                                                if (!value && artistPage == null) viewModel.fetchArtistsFromYTM()
                                            }
                                        },
                                    )
                                }

                                val monthlyListeners = artistPage?.monthlyListenerCount
                                if (showMonthlyListeners && !monthlyListeners.isNullOrEmpty()) {
                                    val cleanListeners = remember(monthlyListeners) {
                                        monthlyListeners
                                            .replace("monthly listeners", "", ignoreCase = true)
                                            .replace("ascoltatori mensili", "", ignoreCase = true)
                                            .trim()
                                    }
                                    val listenersProgress = headerEnter(
                                        revealed = headerRevealed,
                                        play = artistName != null,
                                        delayMillis = nameTypingMs + 80,
                                        durationMillis = IrideMotion.Short,
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text(
                                        text = "$cleanListeners listeners this month",
                                        style = TextStyle(fontFamily = SpaceMonoFontFamily, fontSize = 12.sp),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.revealMask(listenersProgress),
                                    )
                                }


                                if (showRecentAlbumPanel) {
                                    val panelProgress = headerEnter(
                                        revealed = headerRevealed,
                                        play = artistName != null,
                                        delayMillis = nameTypingMs + 140,
                                        durationMillis = IrideMotion.Medium,
                                    )
                                    Spacer(modifier = Modifier.height(14.dp))
                                    RecentAlbumPanel(
                                        album = recentAlbum!!.album,
                                        releaseType = recentAlbum!!.type,
                                        preciseDate = recentAlbumPreciseDate,
                                        useMonospace = true,
                                        enterProgress = panelProgress,
                                        isActive = mediaMetadata?.album?.id == recentAlbum!!.album.id,
                                        isPlaying = isPlaying,
                                        onClick = { navController.navigate("album/${recentAlbum!!.album.id}") },
                                        onLongClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            menuState.show {
                                                AlbumMenu(
                                                    originalAlbum = recentAlbum!!.album,
                                                    navController = navController,
                                                    onDismiss = menuState::dismiss,
                                                )
                                            }
                                        }
                                    )
                                }
                            }
                            if (showRecentAlbumPanel) {
                                Spacer(modifier = Modifier.height(4.dp))
                            }
                        }
                    }
                }

                if (showLocal) {
                    if (librarySongs.isNotEmpty()) {
                        item(key = "local_songs_title") {
                            NavigationTitle(
                                title = stringResource(R.string.songs),
                                modifier = Modifier
                                    .animateItem(placementSpec = IrideMotion.PlacementSpec)
                                    .revealMask(rememberSectionEnter("local_songs", revealedSections)),
                                useIrideStyle = true,
                                onClick = {
                                    navController.navigate("artist/${viewModel.artistId}/songs")
                                },
                            )
                        }

                        val filteredLibrarySongs =
                            if (hideExplicit) {
                                librarySongs.filter { !it.song.explicit }
                            } else {
                                librarySongs
                            }
                        item(key = "local_songs_carousel") {
                            SongCarousel(
                                items = filteredLibrarySongs,
                                key = { "local_song_${it.id}" },
                                modifier = Modifier.animateItem(placementSpec = IrideMotion.PlacementSpec),
                            ) { song, itemWidth ->
                                SongListItem(
                                    song = song,
                                    isActive = song.id == mediaMetadata?.id,
                                    isPlaying = isPlaying,
                                    isSwipeable = false,
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
                                                contentDescription = null,
                                            )
                                        }
                                    },
                                    modifier =
                                        Modifier
                                            .width(itemWidth)
                                            .padding(horizontal = 8.dp)
                                            .combinedClickable(
                                                onClick = {
                                                    if (!isGuest) {
                                                        if (song.id == mediaMetadata?.id) {
                                                            playerConnection.togglePlayPause()
                                                        } else {
                                                            playerConnection.playQueue(
                                                                ListQueue(
                                                                    title = libraryArtist?.artist?.name ?: "Unknown Artist",
                                                                    items = filteredLibrarySongs.map { it.toMediaItem() },
                                                                    startIndex = filteredLibrarySongs.indexOfFirst { it.id == song.id },
                                                                ),
                                                            )
                                                        }
                                                    }
                                                },
                                                onLongClick = {
                                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                    menuState.show {
                                                        SongMenu(
                                                            originalSong = song,
                                                            navController = navController,
                                                            onDismiss = menuState::dismiss,
                                                        )
                                                    }
                                                },
                                            ),
                                )
                            }
                        }
                    }

                    if (libraryAlbums.isNotEmpty()) {
                        val filteredLibraryAlbums = libraryAlbums.filter { album ->
                            !hideExplicit || !album.album.explicit
                        }

                        if (filteredLibraryAlbums.isNotEmpty()) {
                            item(key = "local_albums_title") {
                                NavigationTitle(
                                    title = stringResource(R.string.albums),
                                    modifier = Modifier
                                        .animateItem(placementSpec = IrideMotion.PlacementSpec)
                                        .revealMask(rememberSectionEnter("local_albums", revealedSections)),
                                    useIrideStyle = true,
                                    onClick = {
                                        navController.navigate("artist/${viewModel.artistId}/albums")
                                    },
                                )
                            }

                            item(key = "local_albums_list") {
                                val rowState = rememberLazyListState()
                                LazyRow(
                                    state = rowState,
                                    overscrollEffect = null,
                                    modifier = Modifier
                                        .irideEnter(
                                            rememberSectionEnter("local_albums_row", revealedSections),
                                            10.dp,
                                        )
                                        .rubberBandOverscroll(Orientation.Horizontal, rowState),
                                    horizontalArrangement = Arrangement.spacedBy(0.dp),
                                    contentPadding = PaddingValues(horizontal = irideHorizontalPadding),
                                ) {
                                    items(
                                        items = filteredLibraryAlbums,
                                        key = { "local_album_${it.id}_${filteredLibraryAlbums.indexOf(it)}" },
                                    ) { album ->
                                        AlbumGridItem(
                                            album = album,
                                            isActive = mediaMetadata?.album?.id == album.id,
                                            isPlaying = isPlaying,
                                            coroutineScope = coroutineScope,
                                            size = 180.dp,
                                            modifier =
                                                Modifier
                                                    .combinedClickable(
                                                        onClick = {
                                                            navController.navigate("album/${album.id}")
                                                        },
                                                        onLongClick = {
                                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                            menuState.show {
                                                                AlbumMenu(
                                                                    originalAlbum = album,
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
                    }
                } else {
                    var essentialAlbumsRendered = false
                    var discographyButtonRendered = false
                    artistPage?.sections?.fastForEach { section ->
                        val isFromYourLibrarySection = section.title.contains("your library", ignoreCase = true) ||
                                section.title.contains("tua libreria", ignoreCase = true)
                        if (section.items.isNotEmpty() && !isFromYourLibrarySection) {
                            val isSinglesSection = section.title.contains("Single", ignoreCase = true) || section.title.contains("EP", ignoreCase = true)
                            val filteredItemsUnsorted = section.items.filter { item ->
                                val isDuplicate = isSinglesSection && item is AlbumItem && albumsTitles.contains(item.title.lowercase().trim())
                                !isDuplicate
                            }

                            val isAlbumOrSingleEpSection = section.title.contains("Album", ignoreCase = true) ||
                                    section.title.contains("Single", ignoreCase = true) ||
                                    section.title.contains("EP", ignoreCase = true)
                            val filteredItems = if (isAlbumOrSingleEpSection) {
                                filteredItemsUnsorted.sortedByDescending { (it as? AlbumItem)?.year ?: Int.MIN_VALUE }
                            } else {
                                filteredItemsUnsorted
                            }

                            if (filteredItems.isNotEmpty()) {
                                val isTopSongsShelf = (filteredItems.firstOrNull() as? SongItem)?.album != null

                                item(key = "section_${section.title}") {
                                    NavigationTitle(
                                        title = section.title,
                                        modifier = Modifier
                                            .then(
                                                if (isTopSongsShelf) {
                                                    Modifier
                                                } else {
                                                    Modifier.animateItem(placementSpec = IrideMotion.PlacementSpec)
                                                },
                                            )
                                            .revealMask(rememberSectionEnter(section.title, revealedSections)),
                                        useIrideStyle = true,
                                        onClick =
                                            section.moreEndpoint?.let {
                                                {
                                                    navController.navigate(
                                                        "artist/${viewModel.artistId}/items?browseId=${it.browseId}?params=${it.params}",
                                                    )
                                                }
                                            },
                                    )
                                }

                                if (isTopSongsShelf) {
                                    val shelfTopSongs = filteredItems.distinctBy { it.id }.filterIsInstance<SongItem>()
                                    val topSongs = expandedTopSongs?.takeIf { it.size > shelfTopSongs.size } ?: shelfTopSongs
                                    item(key = "top_songs_carousel_${section.title}") {
                                        val topSongsGridState = rememberLazyGridState()
                                        SongCarousel(
                                            items = topSongs,
                                            key = { it.id },
                                            gridState = topSongsGridState,
                                        ) { song, itemWidth ->
                                            YouTubeListItem(
                                                item = song,
                                                isActive = mediaMetadata?.id == song.id,
                                                isPlaying = isPlaying,
                                                isSwipeable = false,
                                                trailingContent = {
                                                    IconButton(
                                                        onClick = {
                                                            menuState.show {
                                                                YouTubeSongMenu(
                                                                    song = song,
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
                                                },
                                                modifier =
                                                    Modifier
                                                        .width(itemWidth)
                                                        .padding(horizontal = 8.dp)
                                                        .combinedClickable(
                                                            onClick = {
                                                                if (!isGuest) {
                                                                    if (song.id == mediaMetadata?.id) {
                                                                        playerConnection.togglePlayPause()
                                                                    } else {
                                                                        playerConnection.playQueue(
                                                                            ListQueue(
                                                                                title = section.title,
                                                                                items = topSongs.map { it.toMediaItem() },
                                                                                startIndex = topSongs.indexOfFirst { it.id == song.id },
                                                                            ),
                                                                        )
                                                                    }
                                                                }
                                                            },
                                                            onLongClick = {
                                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                                menuState.show {
                                                                    YouTubeSongMenu(
                                                                        song = song,
                                                                        navController = navController,
                                                                        onDismiss = menuState::dismiss,
                                                                    )
                                                                }
                                                            },
                                                        ),
                                            )
                                        }
                                    }

                                    if (essentialAlbums.isNotEmpty() && !essentialAlbumsRendered) {
                                        essentialAlbumsRendered = true
                                        item(key = "essential_albums_title") {
                                            NavigationTitle(
                                                title = stringResource(R.string.essential_albums),
                                                modifier = Modifier
                                                    .animateItem(placementSpec = IrideMotion.PlacementSpec)
                                                    .revealMask(rememberSectionEnter("essential_albums", revealedSections)),
                                                useIrideStyle = true,
                                            )
                                        }
                                        item(key = "essential_albums_row") {
                                            val rowState = rememberLazyListState()
                                            LazyRow(
                                                state = rowState,
                                                overscrollEffect = null,
                                                modifier = Modifier
                                                    .irideEnter(
                                                        rememberSectionEnter("essential_albums_row", revealedSections),
                                                        10.dp,
                                                    )
                                                    .rubberBandOverscroll(Orientation.Horizontal, rowState),
                                                horizontalArrangement = Arrangement.spacedBy(0.dp),
                                                contentPadding = PaddingValues(horizontal = irideHorizontalPadding),
                                            ) {
                                                items(
                                                    items = essentialAlbums,
                                                    key = { "essential_album_${it.id}" },
                                                ) { album ->
                                                    YouTubeGridItem(
                                                        item = album,
                                                        isActive = mediaMetadata?.album?.id == album.id,
                                                        isPlaying = isPlaying,
                                                        coroutineScope = coroutineScope,
                                                        size = 270.dp,
                                                        thumbnailShape = RoundedCornerShape(270.dp * 0.06f),
                                                        hairlineBorder = true,
                                                        modifier = Modifier
                                                            .combinedClickable(
                                                                onClick = {
                                                                    navController.navigate("album/${album.id}")
                                                                },
                                                                onLongClick = {
                                                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                                    menuState.show {
                                                                        AlbumMenu(
                                                                            originalAlbum = album.toAlbumEntity(),
                                                                            navController = navController,
                                                                            onDismiss = menuState::dismiss,
                                                                        )
                                                                    }
                                                                },
                                                            )
                                                            .animateItem(placementSpec = IrideMotion.PlacementSpec),
                                                    )
                                                }
                                            }
                                        }
                                    }
                                } else {
                                    item(key = "section_list_${section.title}") {
                                        val isAlbumSection = section.title.contains("Album", ignoreCase = true)
                                        val isVideoSection = section.title.contains("Video", ignoreCase = true) ||
                                                section.title.contains("Performance", ignoreCase = true)
                                        val rowState = rememberLazyListState()
                                        LazyRow(
                                            state = rowState,
                                            overscrollEffect = null,
                                            modifier = Modifier
                                                .irideEnter(
                                                    rememberSectionEnter(
                                                        "row_${section.title}",
                                                        revealedSections,
                                                    ),
                                                    10.dp,
                                                )
                                                .rubberBandOverscroll(Orientation.Horizontal, rowState),
                                            horizontalArrangement = Arrangement.spacedBy(0.dp),
                                            contentPadding = PaddingValues(horizontal = irideHorizontalPadding),
                                        ) {
                                            items(
                                                items = filteredItems.distinctBy { it.id },
                                                key = { "youtube_album_${it.id}" },
                                            ) { item ->
                                                YouTubeGridItem(
                                                    item = item,
                                                    isActive =
                                                        when (item) {
                                                            is SongItem -> mediaMetadata?.id == item.id
                                                            is AlbumItem -> mediaMetadata?.album?.id == item.id
                                                            else -> false
                                                        },
                                                    isPlaying = isPlaying,
                                                    coroutineScope = coroutineScope,
                                                    thumbnailRatio = if (isVideoSection) 16f / 9f else 1f,
                                                    thumbnailCornerRadius = if (isVideoSection) 8.dp else 3.dp,
                                                    size = when {
                                                        isAlbumSection -> 180.dp
                                                        isVideoSection -> 110.dp
                                                        else -> 148.dp
                                                    },
                                                    modifier =
                                                        Modifier
                                                            .combinedClickable(
                                                                onClick = {
                                                                    when (item) {
                                                                        is SongItem -> {
                                                                            if (isVideoSection) {
                                                                                navController.navigate(
                                                                                    "video_player/${item.id}?title=${Uri.encode(item.title)}",
                                                                                )
                                                                            } else if (!isGuest) {
                                                                                playerConnection.playQueue(
                                                                                    YouTubeQueue(
                                                                                        WatchEndpoint(videoId = item.id),
                                                                                        item.toMediaMetadata(),
                                                                                    ),
                                                                                )
                                                                            }
                                                                        }

                                                                        is AlbumItem -> {
                                                                            navController.navigate("album/${item.id}")
                                                                        }

                                                                        is ArtistItem -> {
                                                                            navController.navigate("artist/${item.id}")
                                                                        }

                                                                        is PlaylistItem -> {
                                                                            navController.navigate("online_playlist/${item.id}")
                                                                        }

                                                                        is PodcastItem -> {
                                                                            navController.navigate("online_podcast/${item.id}")
                                                                        }

                                                                        is EpisodeItem -> {
                                                                            if (!isGuest) {
                                                                                playerConnection.playQueue(
                                                                                    YouTubeQueue(
                                                                                        WatchEndpoint(videoId = item.id),
                                                                                        item.toMediaMetadata(),
                                                                                    ),
                                                                                )
                                                                            }
                                                                        }
                                                                    }
                                                                },
                                                                onLongClick = {
                                                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                                    menuState.show {
                                                                        when (item) {
                                                                            is SongItem -> {
                                                                                YouTubeSongMenu(
                                                                                    song = item,
                                                                                    navController = navController,
                                                                                    onDismiss = menuState::dismiss,
                                                                                )
                                                                            }

                                                                            is AlbumItem -> {
                                                                                AlbumMenu(
                                                                                    originalAlbum = item.toAlbumEntity(),
                                                                                    navController = navController,
                                                                                    onDismiss = menuState::dismiss,
                                                                                )
                                                                            }

                                                                            is ArtistItem -> {
                                                                                YouTubeArtistMenu(
                                                                                    artist = item,
                                                                                    onDismiss = menuState::dismiss,
                                                                                )
                                                                            }

                                                                            is PlaylistItem -> {
                                                                                YouTubePlaylistMenu(
                                                                                    playlist = item,
                                                                                    coroutineScope = coroutineScope,
                                                                                    onDismiss = menuState::dismiss,
                                                                                )
                                                                            }

                                                                            is PodcastItem -> {
                                                                                YouTubePlaylistMenu(
                                                                                    playlist = item.asPlaylistItem(),
                                                                                    coroutineScope = coroutineScope,
                                                                                    onDismiss = menuState::dismiss,
                                                                                )
                                                                            }

                                                                            is EpisodeItem -> {
                                                                                YouTubeSongMenu(
                                                                                    song = item.asSongItem(),
                                                                                    navController = navController,
                                                                                    onDismiss = menuState::dismiss,
                                                                                )
                                                                            }
                                                                        }
                                                                    }
                                                                },
                                                            ).animateItem(placementSpec = IrideMotion.PlacementSpec),
                                                )
                                            }
                                        }
                                    }
                                }

                                if (isSinglesSection && !discographyButtonRendered) {
                                    discographyButtonRendered = true
                                    item(key = "discography_button") {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.Center,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = irideHorizontalPadding, vertical = 4.dp)
                                                .animateItem(placementSpec = IrideMotion.PlacementSpec)
                                                .clip(SquircleShape(radius = 14.dp, cornerSmoothing = 0.48f))
                                                .background(MaterialTheme.colorScheme.onBackground.copy(alpha = 0.06f))
                                                .clickable {
                                                    navController.navigate("artist/${viewModel.artistId}/discography")
                                                }
                                                .padding(vertical = 14.dp),
                                        ) {
                                            Text(
                                                text = stringResource(R.string.view_discography).uppercase(),
                                                style = TextStyle(
                                                    fontFamily = SpaceMonoFontFamily,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 12.sp,
                                                    letterSpacing = 1.sp,
                                                ),
                                                color = MaterialTheme.colorScheme.onBackground,
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Icon(
                                                painter = painterResource(R.drawable.arrow_forward),
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                                                modifier = Modifier.size(16.dp),
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (!showLocal && (showArtistDescription || showArtistSubscriberCount)) {
                        val description = artistPage?.description
                        val descriptionRuns = artistPage?.descriptionRuns
                        val subscriberCount = artistPage?.subscriberCountText

                        if ((showArtistDescription && !description.isNullOrEmpty()) ||
                            (showArtistSubscriberCount && !subscriberCount.isNullOrEmpty())
                        ) {
                            item(key = "about_artist") {
                                Column(
                                    modifier =
                                        Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = irideHorizontalPadding)
                                            .padding(vertical = 16.dp)
                                            .animateItem(placementSpec = IrideMotion.PlacementSpec),
                                ) {
                                    Text(
                                        text = stringResource(R.string.information),
                                        style = TextStyle(fontFamily = SpaceMonoFontFamily, fontSize = 13.sp, letterSpacing = (-0.1).sp),
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier
                                            .padding(bottom = 16.dp)
                                            .revealMask(rememberSectionEnter("about", revealedSections)),
                                    )

                                    if (showArtistSubscriberCount && !subscriberCount.isNullOrEmpty()) {
                                        val formattedSubscribers = formatSubscriberCount(subscriberCount)
                                        Text(
                                            text = formattedSubscribers ?: subscriberCount,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(bottom = 16.dp),
                                        )
                                    }

                                    if (showArtistDescription && (!description.isNullOrEmpty() || !descriptionRuns.isNullOrEmpty())) {
                                        Text(
                                            text = "Wikipedia",
                                            style = TextStyle(fontFamily = SpaceMonoFontFamily, fontSize = 12.sp, letterSpacing = (-0.1).sp),
                                            fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(bottom = 8.dp),
                                        )

                                        val linkSegments = remember(descriptionRuns) {
                                            descriptionRuns?.map { run ->
                                                LinkSegment(
                                                    text = run.text,
                                                    url = run.navigationEndpoint?.urlEndpoint?.url
                                                )
                                            }
                                        }

                                        ExpandableText(
                                            text = description.orEmpty(),
                                            runs = linkSegments,
                                            collapsedMaxLines = 4,
                                        )

                                        val wikiLink = descriptionRuns?.find {
                                            it.text.contains("Wikipedia", ignoreCase = true) ||
                                                    it.navigationEndpoint?.urlEndpoint?.url?.contains("wikipedia.org") == true
                                        }?.navigationEndpoint?.urlEndpoint?.url

                                        if (wikiLink != null) {
                                            Text(
                                                text = "Read more on Wikipedia",
                                                style = MaterialTheme.typography.labelLarge,
                                                color = MaterialTheme.colorScheme.onBackground,
                                                textDecoration = TextDecoration.Underline,
                                                modifier = Modifier
                                                    .padding(top = 12.dp)
                                                    .clickable { uriHandler.openUri(wikiLink) }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
        }

    SnackbarHost(
            hostState = snackbarHostState,
            modifier =
                Modifier
                    .windowInsetsPadding(LocalPlayerAwareWindowInsets.current)
                    .align(Alignment.BottomCenter),
        )
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
                .height(40.dp)
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val backProgress = rememberEnterProgress(play = true, durationMillis = IrideMotion.Short)
            Box(modifier = Modifier.irideEnterScale(backProgress, from = 0.8f)) {
                IconButton(
                    onClick = navController::navigateUp,
                    onLongClick = navController::backToMain,
                ) {
                    Icon(
                        painterResource(R.drawable.arrow_back),
                        contentDescription = stringResource(R.string.back),
                    )
                }
            }
            Text(
                text = artistPage?.artist?.title ?: libraryArtist?.artist?.name.orEmpty(),
                style = TextStyle(
                    fontFamily = SpaceMonoFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                ),
                color = MaterialTheme.colorScheme.onBackground,
                overflow = TextOverflow.Ellipsis,
                maxLines = 1,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 4.dp)
                    .irideEnter(topBarRevealProgress, 6.dp)
                    .revealMask(topBarRevealProgress),
            )
            val shareLinkAction = artistPage?.artist?.shareLink
            val radioEndpointAction = artistPage?.artist?.radioEndpoint
            val shuffleEndpointAction = artistPage?.artist?.shuffleEndpoint
            val actionsReady = artistPage != null || libraryArtist != null
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.animateContentSize(
                    animationSpec = tween(IrideMotion.Short, easing = IrideMotion.EaseOutQuart),
                ),
            ) {
                IrideOutlineIconButton(
                    onClick = { viewModel.toggleChannelSubscription() },
                    icon = if (isChannelSubscribed) R.drawable.favorite else R.drawable.favorite_border,
                    contentDescription = stringResource(R.string.subscribe),
                    tint = MaterialTheme.colorScheme.textPrimary,
                    size = 40.dp,
                    iconSize = 20.dp,
                    pressEffect = IridePressEffect.Punch,
                    modifier = Modifier.irideEnterScale(
                        rememberEnterProgress(play = actionsReady, durationMillis = IrideMotion.Short),
                    ),
                )
                IrideOutlineIconButton(
                    onClick = {
                        menuState.show {
                            ArtistOverflowMenu(
                                onGame = { navController.navigate("artist/${viewModel.artistId}/game") },
                                onRadio = if (!showLocal && !isGuest && radioEndpointAction != null) {
                                    { playerConnection.playQueue(YouTubeQueue(radioEndpointAction)) }
                                } else {
                                    null
                                },
                                onShuffle = if (!showLocal && !isGuest && shuffleEndpointAction != null) {
                                    { playerConnection.playQueue(YouTubeQueue(shuffleEndpointAction)) }
                                } else {
                                    null
                                },
                                onShare = shareLinkAction?.let { link -> { shareArtist(context, link) } },
                                onDismiss = menuState::dismiss,
                            )
                        }
                    },
                    icon = R.drawable.more_vert,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.textPrimary,
                    size = 40.dp,
                    iconSize = 20.dp,
                    modifier = Modifier.irideEnterScale(
                        rememberEnterProgress(
                            play = actionsReady,
                            delayMillis = IrideMotion.StaggerStep,
                            durationMillis = IrideMotion.Short,
                        ),
                    ),
                )
            }
        }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun RecentAlbumPanel(
    album: Album,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    useMonospace: Boolean = false,
    releaseType: AlbumReleaseType? = null,
    preciseDate: String? = null,
    /** 0f→1f entrance driven by the caller, so the panel lands after the artist name is typed. */
    enterProgress: Float = 1f,
    isActive: Boolean = false,
    isPlaying: Boolean = false,
) {
    val interactionSource = remember { MutableInteractionSource() }
    Column(modifier = Modifier.fillMaxWidth()) {
        val eyebrowText = stringResource(R.string.artist_latest_release).uppercase() +
            (releaseType?.let { " • ${it.name}" } ?: "")
        Text(
            text = eyebrowText,
            style = if (useMonospace) {
                TextStyle(fontFamily = SpaceMonoFontFamily, fontSize = 11.sp, letterSpacing = 1.sp)
            } else {
                MaterialTheme.typography.labelMedium
            },
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = if (useMonospace) 0.5f else 1f),
            modifier = Modifier
                .padding(bottom = 8.dp)
                .revealMask(enterProgress),
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .irideEnter(enterProgress, 6.dp)
                .pressScale(interactionSource)
                .combinedClickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onClick,
                    onLongClick = onLongClick
                ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ItemThumbnail(
                thumbnailUrl = album.thumbnailUrl?.resize(544, 544),
                isActive = isActive,
                isPlaying = isPlaying,
                shape = if (useMonospace) {
                    SquircleShape(radius = 9.dp, cornerSmoothing = 0.5f)
                } else {
                    SquircleShape(radius = 6.dp, cornerSmoothing = 0.48f)
                },
                modifier = Modifier.size(if (useMonospace) 108.dp else 96.dp),
                hairlineBorder = true,
            )

            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = album.album.title,
                    style = if (useMonospace) {
                        TextStyle(fontFamily = SpaceMonoFontFamily, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    } else {
                        MaterialTheme.typography.titleLarge
                    },
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Start
                )

                val extendedDate = remember(album.album.releaseDate, preciseDate) {
                    formatExtendedDate(album.album.releaseDate ?: preciseDate)
                }
                val displayDate = extendedDate ?: album.album.year?.toString()

                val metaText = listOfNotNull(
                    displayDate,
                    if (album.album.songCount > 0) {
                        pluralStringResource(R.plurals.n_song, album.album.songCount, album.album.songCount)
                    } else {
                        null
                    },
                ).joinToString(" • ")

                if (useMonospace) {
                    if (metaText.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = metaText,
                            style = TextStyle(fontFamily = SpaceMonoFontFamily, fontSize = 12.sp),
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                            textAlign = TextAlign.Start,
                        )
                    }
                } else {
                    if (displayDate != null) {
                        Text(
                            text = displayDate,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Start
                        )
                    }

                    if (album.album.songCount > 0) {
                        Text(
                            text = pluralStringResource(R.plurals.n_song, album.album.songCount, album.album.songCount),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Start
                        )
                    }
                }
            }
        }
    }
}

private fun shareArtist(context: Context, link: String) {
    val sendIntent = android.content.Intent().apply {
        action = android.content.Intent.ACTION_SEND
        putExtra(android.content.Intent.EXTRA_TEXT, link)
        type = "text/plain"
    }
    context.startActivity(android.content.Intent.createChooser(sendIntent, null))
}

fun formatExtendedDate(dateStr: String?): String? {
    if (dateStr == null) return null
    return try {
        val parts = dateStr.split("-")
        val date = when (parts.size) {
            3 -> java.time.LocalDate.of(parts[0].toInt(), parts[1].toInt(), parts[2].toInt())
            2 -> java.time.LocalDate.of(parts[0].toInt(), parts[1].toInt(), 1)
            else -> java.time.LocalDate.of(parts[0].toInt(), 1, 1)
        }

        val formatter = when (parts.size) {
            3 -> DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.US)
            2 -> DateTimeFormatter.ofPattern("MMMM yyyy", Locale.US)
            else -> DateTimeFormatter.ofPattern("yyyy", Locale.US)
        }

        date.format(formatter)
    } catch (e: Exception) {
        null
    }
}

/** True when a YTM count string ("2.1M subscribers", "850K monthly listeners"...) reaches million+ scale. */
private fun hasLargeAudience(countText: String?): Boolean {
    if (countText == null) return false
    val unit = """([\d.,]+)\s*([KMB])""".toRegex(RegexOption.IGNORE_CASE).find(countText)
        ?.groupValues?.get(2)?.uppercase()
    return unit == "M" || unit == "B"
}

fun formatSubscriberCount(subscriberCount: String?): String? {
    if (subscriberCount == null) return null
    val regex = """([\d.,]+)\s*([KMB]?)""".toRegex(RegexOption.IGNORE_CASE)
    val matchResult = regex.find(subscriberCount) ?: return null
    val (numStr, unit) = matchResult.destructured
    val num = numStr.replace(",", ".").toDoubleOrNull() ?: return null

    if (unit.isEmpty() && num < 1000) return null

    val rounded = (num * 10.0).roundToInt() / 10.0
    val formattedNum = if (rounded % 1.0 == 0.0) rounded.toInt().toString() else rounded.toString()

    return when (unit.uppercase()) {
        "K" -> "${formattedNum}k subscribers on YouTube"
        "M" -> "$formattedNum million subscribers on YouTube"
        "B" -> "$formattedNum billion subscribers on YouTube"
        else -> "$formattedNum subscribers on YouTube"
    }
}