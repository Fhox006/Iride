package com.metrolist.music.ui.player

import com.metrolist.music.ui.component.SongMenuIconButton

import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.innerShadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.graphics.lerp as lerpColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.toSize
import androidx.compose.ui.util.lerp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.media3.common.Timeline
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.size.Size as CoilSize
import com.metrolist.music.LocalDatabase
import com.metrolist.music.LocalPlayerConnection
import com.metrolist.music.R
import com.metrolist.music.constants.PlayerBackgroundStyle
import com.metrolist.music.models.MediaMetadata
import com.metrolist.music.extensions.metadata
import com.metrolist.music.ui.screens.Screens
import com.metrolist.music.ui.component.BottomSheetState
import com.metrolist.music.ui.component.IrideFolio
import com.metrolist.music.ui.component.LocalBottomSheetPageState
import com.metrolist.music.ui.component.LocalMenuState
import com.metrolist.music.ui.component.PillCoverRadius
import com.metrolist.music.ui.component.TopScreenGradientBackground
import com.metrolist.music.ui.menu.PlayerMenu
import com.metrolist.music.ui.theme.InterFontFamily
import com.metrolist.music.ui.theme.SpaceMonoFontFamily
import com.metrolist.music.ui.utils.IrideMotion
import com.metrolist.music.ui.utils.ShowMediaInfo
import com.metrolist.music.ui.utils.pressScale
import com.metrolist.music.ui.utils.rememberReducedMotion
import com.metrolist.music.ui.utils.resize
import com.metrolist.music.utils.makeTimeString
import com.metrolist.music.viewmodels.LyricsViewModel
import kotlin.math.roundToInt
import kotlin.math.abs
import sv.lib.squircleshape.SquircleShape

internal val IrideMp3BackgroundColor = Color(0xFF080809)
private val IrideMp3SurfaceColor = Color.White.copy(alpha = 0.06f)
private val IrideMp3PanelBorderColor = Color.White.copy(alpha = 0.12f)
private val IrideMp3FrameInset = 8.dp
private val IrideMp3FrameHorizontalPadding = 14.dp
private val IrideMp3FrameShape = irideSquircle(18.dp)
private val IrideMp3FrameBorderColor = Color(0xFF414141)
private val IrideMp3InfoActionSize = 48.dp
private val IrideMp3InfoActionIconSize = 28.dp
private val IrideMp3InfoActionColor = Color.White.copy(alpha = 0.9f)
private val IrideMp3DimIconColor = Color.White.copy(alpha = 0.75f)

// Shared artist-line grey — single source of truth for both the static info block and the
// bridge overlay, so the expanded state renders identically no matter which path draws it.
private val IrideArtistTextColor = Color(0xFFB8B8B8)

// Neutral, opaque surfaces keep the controls consistent across album artwork.
private val IrideMp3WheelSurfaceColor = Color(0xFF1C1C1C)
private val IrideMp3WheelCenterColor = Color(0xFF0C0C0C)
private val IrideMp3WheelBorderColor = Color(0xFF454545)
private val IrideMp3WheelCenterBorderColor = Color(0xFF666666)

// Matches AlbumScreen's cover squircle radius exactly (see item 5: one shared cover border
// language across the app) — also the END radius the bridge overlay's clip lerps toward.
private val IrideCoverBorderRadius = 12.dp

// How much of the expand/collapse `eased` progress the bridge overlay's ring gets before it's
// gone — short on purpose, "quasi immediatamente": the ring's job is just to bridge the mini
// pill's own ring into the move, not to survive the whole morph.
private const val BridgeRingFadeOutProgress = 0.12f
private val BridgeRingStrokeWidth = 2.5.dp

/**
 * Same technique as PillProgressDrawCache, minus its size/shape cache — this only ever draws for
 * [BridgeRingFadeOutProgress] worth of a single expand/collapse, so recomputing the traced outline
 * every frame is cheaper than the bookkeeping needed to cache it correctly.
 */
private fun DrawScope.drawBridgeProgressRing(
    cornerRadiusPx: Float,
    strokeWidthPx: Float,
    progress: Float,
) {
    val inset = strokeWidthPx / 2f
    val path =
        Path().apply {
            addRoundRect(
                RoundRect(
                    left = inset,
                    top = inset,
                    right = size.width - inset,
                    bottom = size.height - inset,
                    cornerRadius = CornerRadius(cornerRadiusPx),
                )
            )
        }
    val stroke = Stroke(width = strokeWidthPx, cap = StrokeCap.Round)
    drawPath(path, color = Color.White.copy(alpha = 0.2f), style = stroke)
    if (progress > 0f) {
        val measure = PathMeasure().apply { setPath(path, false) }
        val segment = Path()
        measure.getSegment(0f, measure.length * progress.coerceIn(0f, 1f), segment, true)
        drawPath(segment, color = Color.White, style = stroke)
    }
}

private fun irideSquircle(radius: Dp) = SquircleShape(radius = radius, cornerSmoothing = 0.48f)

@Stable
class IrideBridgeState {
    var miniArt by mutableStateOf<Rect?>(null)
    var playerArt by mutableStateOf<Rect?>(null)
    var miniInfo by mutableStateOf<Rect?>(null)
    var playerInfo by mutableStateOf<Rect?>(null)

    // Live playback fraction, reported every frame by the mini pill's own ring (PillPlayButton) —
    // lets the bridge overlay's ring (drawn on the moving cover, see IrideMiniPlayerBridgeOverlay)
    // start already in sync instead of snapping from 0 the instant the expand begins.
    var progress by mutableStateOf(0f)

    var panelActive by mutableStateOf(false)
    // True while the lyrics FullScreenLyricsDialog (a separate Android Window drawn on top of
    // everything) is showing. MainActivity's app-peek-height "tap to collapse" catcher sits in
    // the same Activity window underneath that dialog and has no other way to know a fullscreen
    // overlay is up — without this flag a tap landing in that strip during the dialog's
    // open/close transition collapses the whole player and drops the lyrics with it.
    var lyricsFullScreenActive by mutableStateOf(false)
}

internal fun Modifier.irideReportRect(target: (Rect) -> Unit): Modifier =
    this.onGloballyPositioned { target(Rect(it.positionInWindow(), it.size.toSize())) }

private enum class IrideMp3Page { NowPlaying, Menu, Queue, Lyrics, FindSimilar }

@Composable
fun IrideMp3PlayerContent(
    mediaMetadata: MediaMetadata,
    position: Long,
    duration: Long,
    isPlaying: Boolean,
    isFavorite: Boolean,
    onPlayPauseClick: () -> Unit,
    onPreviousClick: () -> Unit = {},
    onNextClick: () -> Unit = {},
    onShuffleClick: () -> Unit = {},
    isShuffleActive: Boolean = false,
    onFavoriteClick: () -> Unit = {},
    onSeek: (Float) -> Unit = {},
    onArtistClick: (String) -> Unit = {},
    onLyricsOptions: () -> Unit = {},
    isListenTogetherGuest: Boolean = false,
    isMuted: Boolean = false,
    navController: NavController,
    playerBottomSheetState: BottomSheetState,
    playerBackground: PlayerBackgroundStyle = PlayerBackgroundStyle.GRADIENT,
    modifier: Modifier = Modifier,
    bottomInset: Dp = 0.dp,
    bridgeState: IrideBridgeState? = null,
) {
    val menuState = LocalMenuState.current
    val bottomSheetPageState = LocalBottomSheetPageState.current
    val onMoreClick = {
        menuState.show(anchored = true) {
            PlayerMenu(
                mediaMetadata = mediaMetadata,
                navController = navController,
                playerBottomSheetState = playerBottomSheetState,
                onShowDetailsDialog = {
                    bottomSheetPageState.show { ShowMediaInfo(mediaMetadata.id) }
                },
                onDismiss = menuState::dismiss,
            )
        }
    }
    var page by rememberSaveable { mutableStateOf(IrideMp3Page.NowPlaying) }
    var selectedItem by rememberSaveable { mutableIntStateOf(0) }
    var dragFraction by remember { mutableStateOf<Float?>(null) }
    val reducedMotion = rememberReducedMotion()
    val queueControls = remember { IrideMp3QueueControls() }
    val lyricsControls = remember { com.metrolist.music.ui.component.LyricsPillController() }
    val lyricsViewModel: LyricsViewModel = hiltViewModel()
    val unavailableLyricsMediaId by lyricsViewModel.unavailableLyricsMediaId.collectAsState()
    val showLyricsEntry = unavailableLyricsMediaId != mediaMetadata.id
    val context = LocalContext.current
    var lyricsFullscreen by remember { mutableStateOf(false) }
    val categories = listOf(
        IrideMp3Page.NowPlaying, IrideMp3Page.Queue, IrideMp3Page.Lyrics, IrideMp3Page.FindSimilar,
    )
    val availableCategoryIndices = categories.indices.filter { index ->
        showLyricsEntry || categories[index] != IrideMp3Page.Lyrics
    }
    LaunchedEffect(availableCategoryIndices) {
        if (selectedItem !in availableCategoryIndices) selectedItem = availableCategoryIndices.first()
    }
    fun moveMenuSelection(direction: Int) {
        val current = availableCategoryIndices.indexOf(selectedItem).coerceAtLeast(0)
        selectedItem = availableCategoryIndices[
            (current + direction + availableCategoryIndices.size) % availableCategoryIndices.size
        ]
    }
    val menuOpen = page == IrideMp3Page.Menu
    val openMenu = { page = IrideMp3Page.Menu }
    val previousClick by rememberUpdatedState(onPreviousClick)
    val nextClick by rememberUpdatedState(onNextClick)
    fun selectCategory(index: Int) {
        if (index !in availableCategoryIndices) return
        selectedItem = index
        if (categories[index] == IrideMp3Page.FindSimilar) {
            val query = (listOf(mediaMetadata.title) + mediaMetadata.artists.map { it.name }).joinToString(" ")
            page = IrideMp3Page.NowPlaying
            playerBottomSheetState.collapseSoft()
            navController.navigate(Screens.Search.route) { launchSingleTop = true }
            navController.currentBackStackEntry?.savedStateHandle?.set("playerSearchQuery", query)
        } else page = categories[index]
    }

    BackHandler(enabled = page != IrideMp3Page.NowPlaying) {
        page = if (menuOpen) IrideMp3Page.NowPlaying else IrideMp3Page.Menu
    }
    SideEffect {
        bridgeState?.panelActive = page != IrideMp3Page.NowPlaying
        bridgeState?.lyricsFullScreenActive = lyricsFullscreen
    }
    DisposableEffect(bridgeState) {
        onDispose {
            bridgeState?.panelActive = false
            bridgeState?.lyricsFullScreenActive = false
        }
    }

    BoxWithConstraints(modifier.fillMaxSize().clipToBounds().background(IrideMp3BackgroundColor)) {
        val panelWidth = maxWidth
        val panelHeight = maxHeight
        val textScale = (panelWidth / 360.dp).coerceIn(0.90f, 1.1f)
        val topSpacing = minOf(32.dp, panelHeight * 0.05f)
        val bottomSpacing = maxOf(bottomInset, 20.dp).coerceAtMost(panelHeight)
        val availableHeight = (panelHeight - topSpacing - bottomSpacing).coerceAtLeast(0.dp)
        val screenHeight = minOf(panelWidth * 1.12f + 8.dp, availableHeight * 0.68f)
        val coverShape = irideSquircle(IrideCoverBorderRadius)
        Column(Modifier.fillMaxSize().padding(top = topSpacing, bottom = bottomSpacing)) {
            Box(
                Modifier.fillMaxWidth().height(screenHeight)
                    .padding(horizontal = IrideMp3FrameHorizontalPadding)
                    .clip(IrideMp3FrameShape)
                    .background(Color.Black)
                    .border(1.dp, IrideMp3FrameBorderColor, IrideMp3FrameShape),
            ) {
                TopScreenGradientBackground(
                    mediaMetadata = mediaMetadata,
                    playerBackground = when (playerBackground) {
                        PlayerBackgroundStyle.DEFAULT, PlayerBackgroundStyle.BLUR -> PlayerBackgroundStyle.GRADIENT
                        else -> playerBackground
                    },
                    emphasizeArtwork = true,
                    backgroundColor = IrideMp3BackgroundColor,
                    artworkGradientHeight = screenHeight,
                )
                Column(Modifier.fillMaxSize().padding(IrideMp3FrameInset)) {
                    Box(
                        modifier = Modifier.weight(1f).fillMaxWidth()
                            .clip(coverShape)
                            .pointerInput(isListenTogetherGuest, page) {
                                if (isListenTogetherGuest || page != IrideMp3Page.NowPlaying) return@pointerInput
                                val threshold = maxOf(48.dp.toPx(), size.width * 0.20f)
                                awaitEachGesture {
                                    val down = awaitFirstDown(requireUnconsumed = false)
                                    var distance = Offset.Zero
                                    var horizontal = false
                                    while (true) {
                                        val change = awaitPointerEvent().changes.firstOrNull { it.id == down.id } ?: break
                                        if (change.isConsumed) break
                                        if (!change.pressed) {
                                            if (horizontal && distance.x <= -threshold) nextClick()
                                            else if (horizontal && distance.x >= threshold) previousClick()
                                            break
                                        }
                                        distance += change.positionChange()
                                        if (!horizontal && distance.getDistance() > viewConfiguration.touchSlop) {
                                            if (abs(distance.x) < abs(distance.y) * 1.7f) break
                                            horizontal = true
                                        }
                                        if (horizontal) change.consume()
                                    }
                                }
                            },
                    ) {
                        when (page) {
                            IrideMp3Page.NowPlaying -> Crossfade(
                                targetState = mediaMetadata.thumbnailUrl,
                                animationSpec = tween(
                                    if (reducedMotion) 0 else IrideMotion.Short,
                                    easing = IrideMotion.EaseOutQuart,
                                ),
                                label = "mp3Artwork",
                                modifier = Modifier.fillMaxSize(),
                            ) { thumbnailUrl ->
                                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    AsyncImage(
                                        model = remember(context, thumbnailUrl) {
                                            ImageRequest.Builder(context)
                                                .data(thumbnailUrl?.resize(1080, 1080))
                                                .size(CoilSize.ORIGINAL)
                                                .build()
                                        },
                                        contentDescription = stringResource(R.string.mp3_now_playing),
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                            .irideReportRect { bridgeState?.playerArt = it }
                                            .clip(coverShape)
                                            .combinedClickable(
                                                interactionSource = remember { MutableInteractionSource() },
                                                indication = null,
                                                onClick = {},
                                                onLongClick = onMoreClick,
                                            ),
                                    )
                                }
                            }
                            IrideMp3Page.Menu -> IrideMp3Menu(
                                mediaMetadata = mediaMetadata,
                                remaining = (duration - position).coerceAtLeast(0),
                                selectedItem = selectedItem,
                                textScale = textScale,
                                showLyrics = showLyricsEntry,
                                onSelect = ::selectCategory,
                            )
                            IrideMp3Page.Queue -> IrideMp3Queue(queueControls, duration - position,
                                isListenTogetherGuest, playerBottomSheetState,
                                onNowPlaying = { page = IrideMp3Page.NowPlaying }, textScale = textScale)
                            IrideMp3Page.Lyrics -> IrideMp3Lyrics(position, controller = lyricsControls,
                                onOptions = onLyricsOptions, onFullscreen = { lyricsFullscreen = it })
                            IrideMp3Page.FindSimilar -> Unit
                        }
                    }
                    Spacer(Modifier.height(IrideCoverBorderRadius))
                    Column(
                        Modifier.fillMaxWidth().padding(start = 6.dp, end = 6.dp, bottom = 6.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween,
                        ) {
                            val displayedPosition = dragFraction?.let { (duration * it).toLong() } ?: position
                            listOf(makeTimeString(displayedPosition), makeTimeString(duration)).forEach { time ->
                                Text(
                                    text = time,
                                    color = Color.White.copy(alpha = 0.75f),
                                    fontFamily = SpaceMonoFontFamily,
                                    fontSize = (10 * textScale).sp,
                                    lineHeight = (14 * textScale).sp,
                                )
                            }
                        }
                        IrideMp3Progress(
                            position = position,
                            duration = duration,
                            dragFraction = dragFraction,
                            enabled = !isListenTogetherGuest,
                            onDragFraction = { dragFraction = it },
                            onSeek = onSeek,
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(
                                modifier = Modifier.weight(1f)
                                    .irideReportRect { bridgeState?.playerInfo = it },
                            ) {
                                Text(
                                    text = mediaMetadata.title,
                                    color = Color.White,
                                    fontFamily = SpaceMonoFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = (16 * textScale).sp,
                                    lineHeight = (21 * textScale).sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.basicMarquee(iterations = 1, initialDelayMillis = 2000)
                                        .combinedClickable(
                                            interactionSource = remember { MutableInteractionSource() },
                                            indication = null,
                                            onClick = {},
                                            onLongClick = onMoreClick,
                                        ),
                                )
                                IrideArtistText(
                                    mediaMetadata = mediaMetadata,
                                    color = IrideArtistTextColor,
                                    fontSize = (12 * textScale).sp,
                                    lineHeight = (16 * textScale).sp,
                                    fontWeight = FontWeight.Normal,
                                    fontFamily = SpaceMonoFontFamily,
                                    onArtistClick = onArtistClick,
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            }
                            IrideMp3InfoActions(isFavorite, onFavoriteClick, onMoreClick, reducedMotion)
                        }
                    }
                }
            }
            BoxWithConstraints(
                modifier = Modifier.weight(1f).fillMaxWidth()
                    .padding(top = minOf(28.dp, availableHeight * 0.045f)),
                contentAlignment = Alignment.Center,
            ) {
                val wheelSize = minOf(216.dp, panelWidth * 0.56f, maxHeight * 0.94f)
                IrideClickWheel(
                    isPlaying = isPlaying,
                    isShuffleActive = isShuffleActive,
                    isMenuOpen = menuOpen,
                    onMenuClick = openMenu,
                    onMoveUp = { moveMenuSelection(-1) },
                    onMoveDown = { moveMenuSelection(1) },
                    isListenTogetherGuest = isListenTogetherGuest,
                    isMuted = isMuted,
                    onPlayPauseClick = onPlayPauseClick,
                    onPreviousClick = onPreviousClick,
                    onNextClick = onNextClick,
                    onRadioClick = onShuffleClick,
                    onCenterClick = {
                        if (menuOpen) selectCategory(selectedItem)
                        else onPlayPauseClick()
                    },
                    wheelSize = wheelSize,
                )
            }
        }
    }
}

@Composable
private fun IrideMp3InfoActions(
    isFavorite: Boolean,
    onFavoriteClick: () -> Unit,
    onMoreClick: () -> Unit,
    reducedMotion: Boolean,
) {
    Row(
        Modifier.padding(start = 10.dp)
            .clip(IrideFolio.ControlsShape)
            .background(IrideMp3WheelCenterColor)
            .border(1.dp, IrideMp3PanelBorderColor, IrideFolio.ControlsShape),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val favoriteInteraction = remember { MutableInteractionSource() }
        Box(
            Modifier.size(IrideFolio.ControlsTouchTargetSize)
                .pressScale(favoriteInteraction, pressedScale = 0.92f)
                .clip(CircleShape)
                .clickable(
                    interactionSource = favoriteInteraction,
                    indication = null,
                    onClick = onFavoriteClick,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Crossfade(
                targetState = isFavorite,
                animationSpec =
                    tween(
                        if (reducedMotion) 0 else IrideMotion.Quick,
                        easing = IrideMotion.EaseOutQuart,
                    ),
                label = "mp3Favorite",
                modifier = Modifier.size(22.dp),
            ) { favorite ->
                Icon(
                    painterResource(
                        if (favorite) R.drawable.ic_widget_star_nav
                        else R.drawable.ic_widget_star_outline_nav
                    ),
                    stringResource(
                        if (favorite) R.string.mp3_remove_favorite else R.string.mp3_add_favorite
                    ),
                    tint = if (favorite) Color.White else Color.White.copy(alpha = 0.7f),
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
        SongMenuIconButton(
            onClick = onMoreClick,
            modifier = Modifier.size(IrideFolio.ControlsTouchTargetSize),
        ) {
            Icon(
                painterResource(R.drawable.more_vert),
                stringResource(R.string.mp3_more),
                tint = Color.White.copy(alpha = 0.85f),
                modifier = Modifier.size(22.dp),
            )
        }
    }
}

@Composable
private fun IrideMp3Progress(
    position: Long,
    duration: Long,
    dragFraction: Float?,
    enabled: Boolean,
    onDragFraction: (Float?) -> Unit,
    onSeek: (Float) -> Unit,
) {
    val progress = (dragFraction ?: if (duration > 0) position.toFloat() / duration else 0f)
        .coerceIn(0f, 1f)
    Box(
        modifier = Modifier.fillMaxWidth().height(20.dp)
            .semantics {
                progressBarRangeInfo = ProgressBarRangeInfo(progress, 0f..1f)
                if (enabled && duration > 0) {
                    setProgress { value -> onSeek(value.coerceIn(0f, 1f)); true }
                }
            }
            .pointerInput(duration, enabled) {
                if (duration <= 0 || !enabled) return@pointerInput
                awaitEachGesture {
                    val down = awaitFirstDown()
                    down.consume()
                    var fraction = (down.position.x / size.width).coerceIn(0f, 1f)
                    onDragFraction(fraction)
                    try {
                        val released = drag(down.id) { change ->
                            change.consume()
                            fraction = (change.position.x / size.width).coerceIn(0f, 1f)
                            onDragFraction(fraction)
                        }
                        if (released) onSeek(fraction)
                    } finally {
                        onDragFraction(null)
                    }
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxWidth().height(3.dp)) {
            val radius = CornerRadius(size.height / 2f)
            drawRoundRect(Color.White.copy(alpha = 0.18f), cornerRadius = radius)
            if (progress > 0f) {
                drawRoundRect(
                    Color(0xFFE5E5E5),
                    size = Size(size.width * progress.coerceIn(0f, 1f), size.height),
                    cornerRadius = radius,
                )
            }
        }
    }
}

@Composable
internal fun IrideMp3CurrentTrack(
    metadata: MediaMetadata,
    remaining: Long,
    textScale: Float = 1f,
    selected: Boolean = true,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(14.dp)
    Row(
        Modifier.fillMaxWidth().clip(shape)
            .background(if (selected) IrideMp3SurfaceColor else Color.Transparent)
            .border(0.5.dp, if (selected) IrideMp3PanelBorderColor else Color.Transparent, shape)
            .selectable(selected = selected, role = Role.Button, onClick = onClick)
            .padding(8.dp * textScale),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AsyncImage(
            model = metadata.thumbnailUrl, contentDescription = null, contentScale = ContentScale.Crop,
            modifier = Modifier.size(52.dp * textScale).clip(RoundedCornerShape(6.dp)),
        )
        Column(Modifier.weight(1f).padding(start = 7.dp * textScale)) {
            Text(
                stringResource(R.string.mp3_now_playing), color = Color.White,
                fontFamily = SpaceMonoFontFamily, fontWeight = FontWeight.Bold,
                fontSize = (14 * textScale).sp, lineHeight = (20 * textScale).sp,
                maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
            Text(
                metadata.title, color = Color.White.copy(alpha = 0.55f),
                fontFamily = SpaceMonoFontFamily, fontWeight = FontWeight.SemiBold,
                fontSize = (11 * textScale).sp, lineHeight = (15 * textScale).sp,
                maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
            Text(
                stringResource(R.string.mp3_time_left, (remaining.coerceAtLeast(0) + 999) / 1000),
                color = Color.White.copy(alpha = 0.55f), fontFamily = SpaceMonoFontFamily,
                fontWeight = FontWeight.SemiBold, fontSize = (11 * textScale).sp,
                lineHeight = (15 * textScale).sp,
            )
        }
        Icon(
            painterResource(R.drawable.expand_more), null,
            tint = if (selected) Color.White else IrideMp3DimIconColor,
            modifier = Modifier.size(26.dp * textScale).graphicsLayer { rotationZ = -90f },
        )
    }
}

@Composable
private fun IrideMp3Menu(
    mediaMetadata: MediaMetadata,
    remaining: Long,
    selectedItem: Int,
    textScale: Float,
    showLyrics: Boolean,
    onSelect: (Int) -> Unit,
) {
    val playerConnection = LocalPlayerConnection.current
    val queueWindows by playerConnection?.queueWindows?.collectAsState() ?: remember { mutableStateOf(emptyList<Timeline.Window>()) }
    val queueIndex by playerConnection?.currentWindowIndex?.collectAsState() ?: remember { mutableIntStateOf(0) }
    val songsLeft = (queueWindows.size - queueIndex - 1).coerceAtLeast(0)
    val labels = listOf(R.string.mp3_now_playing, R.string.mp3_manage_queue, R.string.mp3_read_lyrics, R.string.mp3_song_info)
    val menuEntries = labels.indices.drop(1).filter { showLyrics || it != 2 }
    val listState = rememberLazyListState()
    val reducedMotion = rememberReducedMotion()
    LaunchedEffect(selectedItem, menuEntries) {
        val scrollIndex = menuEntries.indexOf(selectedItem)
        if (scrollIndex >= 0 && listState.layoutInfo.visibleItemsInfo.none { it.index == scrollIndex }) {
            if (reducedMotion) listState.scrollToItem(scrollIndex)
            else listState.animateScrollToItem(scrollIndex)
        }
    }
    @Composable fun MenuEntry(index: Int) {
        if (index == 0) {
            IrideMp3CurrentTrack(
                mediaMetadata, remaining, textScale, selectedItem == 0,
                onClick = { onSelect(0) },
            )
            Spacer(Modifier.height(4.dp))
            return
        }
        val selected = selectedItem == index
        val selectionColor by animateColorAsState(
            if (selected) IrideMp3SurfaceColor else Color.Transparent,
            tween(if (reducedMotion) 0 else IrideMotion.Quick),
            label = "mp3MenuSelection",
        )
        Row(
            modifier = Modifier.fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(selectionColor)
                .border(
                    0.5.dp,
                    if (selected) Color.White.copy(alpha = 0.14f) else Color.Transparent,
                    RoundedCornerShape(14.dp),
                )
                .selectable(selected = selected, role = Role.Button, onClick = { onSelect(index) })
                .padding(horizontal = 7.dp * textScale, vertical = (if (index == 1) 7.dp else 4.dp) * textScale),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val artworkSize = 52.dp * textScale
            val upcoming = queueWindows.drop((queueIndex + 1).coerceAtLeast(0)).take(3)
            Box(Modifier.size(width = artworkSize + if (index == 1) 12.dp else 0.dp, height = if (index == 1) artworkSize else 32.dp * textScale), contentAlignment = Alignment.CenterStart) {
                if (index == 1) {
                    upcoming.asReversed().forEachIndexed { reversed, window ->
                        val depth = upcoming.lastIndex - reversed
                        AsyncImage(model = window.mediaItem.metadata?.thumbnailUrl,
                            contentDescription = null, contentScale = ContentScale.Crop,
                            alpha = if (depth == 0) 1f else if (depth == 1) .6f else .3f,
                            modifier = Modifier.size(artworkSize - depth.dp * 2)
                                .offset(x = depth.dp * 6).clip(RoundedCornerShape(5.dp)))
                    }
                    if (upcoming.isEmpty()) Icon(painterResource(R.drawable.queue_music), null,
                        tint = Color.White.copy(alpha = .55f), modifier = Modifier.size(28.dp))
                } else {
                    Icon(painterResource(if (index == 2) R.drawable.ic_iride_quote else R.drawable.search), null,
                        tint = Color.White, modifier = Modifier.align(Alignment.Center).size(26.dp * textScale))
                }
            }
            Spacer(Modifier.width(7.dp * textScale))
            Column(Modifier.weight(1f)) {
                Text(
                    text = stringResource(labels[index]),
                    color = Color.White,
                    fontFamily = SpaceMonoFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = (14 * textScale).sp,
                    lineHeight = (20 * textScale).sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (index == 1) {
                    Text(
                        text = stringResource(R.string.mp3_queue_description),
                        color = Color.White.copy(alpha = 0.55f),
                        fontFamily = SpaceMonoFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = (11 * textScale).sp,
                        lineHeight = (15 * textScale).sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = stringResource(R.string.mp3_songs_left, songsLeft),
                        color = Color.White.copy(alpha = 0.55f),
                        fontFamily = SpaceMonoFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = (11 * textScale).sp,
                        lineHeight = (15 * textScale).sp,
                    )
                }
            }
            Icon(
                painter = painterResource(if (index == 3) R.drawable.ic_iride_arrow_up_right else R.drawable.expand_more),
                contentDescription = null,
                tint = if (selected) Color.White else Color(0xFF666666),
                modifier = Modifier.size((if (index == 3) 22.dp else 26.dp) * textScale)
                    .graphicsLayer { rotationZ = if (index == 3) 0f else -90f },
            )
        }
        Spacer(Modifier.height(4.dp))
    }
    Column(Modifier.fillMaxSize().padding(1.dp)) {
        MenuEntry(0)
        LazyColumn(Modifier.weight(1f).fillMaxWidth(), state = listState) {
            items(menuEntries, key = { it }) { index -> MenuEntry(index) }
        }
    }
}

@Composable
internal fun IrideArtistText(
    mediaMetadata: MediaMetadata,
    color: Color,
    fontSize: TextUnit,
    onArtistClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    fontFamily: FontFamily = InterFontFamily,
    lineHeight: TextUnit = TextUnit.Unspecified,
    fontWeight: FontWeight = FontWeight.Normal,
) {
    val annotated =
        remember(mediaMetadata.artists) {
            buildAnnotatedString {
                mediaMetadata.artists.forEachIndexed { index, artist ->
                    pushStringAnnotation(tag = "artist", annotation = artist.id.orEmpty())
                    append(artist.name)
                    pop()
                    if (index != mediaMetadata.artists.lastIndex) append(", ")
                }
            }
        }
    var layoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }
    var tapPosition by remember { mutableStateOf<Offset?>(null) }
    val interactionSource = remember { MutableInteractionSource() }
    // Underline while held so the press reads as "release to open artist screen".
    val isPressed by interactionSource.collectIsPressedAsState()

    Text(
        text = annotated,
        color = color,
        fontFamily = fontFamily,
        fontSize = fontSize,
        lineHeight = lineHeight,
        fontWeight = fontWeight,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        textDecoration = if (isPressed) TextDecoration.Underline else null,
        onTextLayout = { layoutResult = it },
        modifier =
            modifier
                .pointerInput(annotated) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent()
                            event.changes.firstOrNull()?.position?.let { tapPosition = it }
                        }
                    }
                }
                .combinedClickable(
                    indication = null,
                    interactionSource = interactionSource,
                    onClick = {
                        val layout = layoutResult ?: return@combinedClickable
                        val pos = tapPosition ?: return@combinedClickable
                        val charOffset = layout.getOffsetForPosition(pos)
                        annotated
                            .getStringAnnotations("artist", charOffset, charOffset)
                            .firstOrNull()
                            ?.let { ann -> if (ann.item.isNotBlank()) onArtistClick(ann.item) }
                    },
                ),
    )
}

@Composable
private fun IrideClickWheel(
    isPlaying: Boolean,
    isShuffleActive: Boolean,
    isMenuOpen: Boolean,
    onMenuClick: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    isListenTogetherGuest: Boolean,
    isMuted: Boolean,
    onPlayPauseClick: () -> Unit,
    onPreviousClick: () -> Unit,
    onNextClick: () -> Unit,
    onRadioClick: () -> Unit,
    onCenterClick: () -> Unit,
    wheelSize: Dp,
    modifier: Modifier = Modifier,
) {
    val scale = wheelSize / 233.dp
    val reducedMotion = rememberReducedMotion()
    val shuffleDescription = stringResource(if (isShuffleActive) R.string.mp3_shuffle_on else R.string.mp3_shuffle_off)
    val centerSize = wheelSize * 0.40f
    // Each zone spans the ring band, putting its icon and selected halo on the same midpoint.
    val zoneSize = (wheelSize - centerSize) / 2
    val edgeInset = 0.dp
    val iconSize = (wheelSize * 0.13f).coerceIn(20.dp, 28.dp)
    val radioHighlight by animateColorAsState(
        if (isShuffleActive) Color.White.copy(alpha = 0.12f) else Color.Transparent,
        tween(if (reducedMotion) 0 else IrideMotion.Short), label = "mp3RadioSeed",
    )
    val radioOutline by animateColorAsState(
        if (isShuffleActive) Color.White.copy(alpha = 0.5f) else Color.Transparent,
        tween(if (reducedMotion) 0 else IrideMotion.Short), label = "mp3RadioOutline",
    )
    Box(modifier.size(wheelSize), contentAlignment = Alignment.Center) {
        Box(
            Modifier.fillMaxSize().clip(CircleShape)
                .background(IrideMp3WheelSurfaceColor)
                .innerShadow(
                    CircleShape,
                    Shadow(
                        radius = 8.dp, spread = 1.dp,
                        color = Color.Black.copy(alpha = 0.65f),
                        offset = DpOffset.Zero,
                    ),
                )
                .border(
                    1.dp,
                    IrideMp3WheelBorderColor,
                    CircleShape,
                ),
        )
        WheelZone(
            alignment = Alignment.TopCenter,
            size = zoneSize,
            edgeInset = edgeInset,
            onClick = if (isMenuOpen) onMoveUp else onMenuClick,
        ) {
            Crossfade(
                targetState = isMenuOpen,
                animationSpec = tween(if (reducedMotion) 0 else IrideMotion.Quick),
                label = "mp3MenuControl",
                modifier = Modifier.fillMaxSize(),
            ) { menuOpen ->
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    if (menuOpen) {
                        Icon(
                            painterResource(R.drawable.expand_less), stringResource(R.string.mp3_move_up),
                            tint = Color.White, modifier = Modifier.size(28.dp),
                        )
                    } else {
                        Text(
                            stringResource(R.string.mp3_menu), color = Color.White,
                            fontFamily = SpaceMonoFontFamily, fontWeight = FontWeight.Medium,
                            fontSize = (12 * scale).sp, letterSpacing = 0.8.sp,
                        )
                    }
                }
            }
        }
        WheelZone(
            alignment = Alignment.CenterEnd,
            size = zoneSize,
            edgeInset = edgeInset,
            onClick = onNextClick,
            enabled = !isListenTogetherGuest,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_iride_fast_forward_filled),
                contentDescription = stringResource(R.string.mp3_next),
                tint = if (isListenTogetherGuest) IrideMp3DimIconColor else Color.White,
                modifier = Modifier.size(iconSize),
            )
        }
        WheelZone(
            alignment = Alignment.CenterStart,
            size = zoneSize,
            edgeInset = edgeInset,
            onClick = onPreviousClick,
            enabled = !isListenTogetherGuest,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_iride_fast_forward_filled),
                contentDescription = stringResource(R.string.mp3_previous),
                tint = if (isListenTogetherGuest) IrideMp3DimIconColor else Color.White,
                modifier = Modifier.size(iconSize).graphicsLayer { scaleX = -1f },
            )
        }
        WheelZone(
            alignment = Alignment.BottomCenter,
            size = zoneSize,
            edgeInset = edgeInset,
            onClick = if (isMenuOpen) onMoveDown else onRadioClick,
            enabled = isMenuOpen || !isListenTogetherGuest,
        ) {
            Crossfade(
                targetState = isMenuOpen,
                animationSpec = tween(if (reducedMotion) 0 else IrideMotion.Quick),
                label = "mp3RadioControl",
                modifier = Modifier.fillMaxSize(),
            ) { menuOpen ->
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    if (menuOpen) {
                        Icon(
                            painterResource(R.drawable.expand_more), stringResource(R.string.mp3_move_down),
                            tint = Color.White, modifier = Modifier.size(28.dp),
                        )
                    } else {
                        Box(
                            Modifier.size(zoneSize * 0.72f).clip(CircleShape).background(radioHighlight)
                                .border(1.dp, radioOutline, CircleShape)
                                .semantics { stateDescription = shuffleDescription },
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                painterResource(R.drawable.radio), shuffleDescription,
                                tint = if (isListenTogetherGuest) IrideMp3DimIconColor else Color.White,
                                modifier = Modifier.size(iconSize),
                            )
                        }
                    }
                }
            }
        }
        val centerInteraction = remember { MutableInteractionSource() }
        val centerDescription = stringResource(
            if (isMenuOpen) R.string.mp3_select
            else if (isListenTogetherGuest) R.string.mp3_mute
            else if (isPlaying) R.string.mp3_pause else R.string.mp3_play,
        )
        Box(
            modifier = Modifier.size(centerSize)
                .pressScale(centerInteraction, pressedScale = 0.96f)
                .clip(CircleShape).background(IrideMp3WheelCenterColor)
                .innerShadow(
                    CircleShape,
                    Shadow(
                        radius = 4.dp,
                        color = Color.Black.copy(alpha = 0.5f),
                        offset = DpOffset.Zero,
                    ),
                )
                .border(
                    1.dp,
                    IrideMp3WheelCenterBorderColor,
                    CircleShape,
                )
                .semantics { contentDescription = centerDescription }
                .combinedClickable(
                    interactionSource = centerInteraction,
                    indication = null,
                    onClick = onCenterClick,
                    onLongClick = onPlayPauseClick,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Box {
                if (isListenTogetherGuest) {
                    Crossfade(
                        targetState = isMuted,
                        animationSpec = tween(if (reducedMotion) 0 else IrideMotion.Quick),
                        label = "mp3Mute",
                        modifier = Modifier.size(iconSize),
                    ) { muted ->
                        Icon(
                            painter = painterResource(if (muted) R.drawable.volume_off else R.drawable.volume_up),
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                } else {
                    IrideMp3PlaybackGlyph(isPlaying, reducedMotion, Modifier.size(iconSize))
                }
            }
        }
    }
}

@Composable
private fun IrideMp3PlaybackGlyph(
    isPlaying: Boolean,
    reducedMotion: Boolean,
    modifier: Modifier = Modifier,
) {
    val pausedProgress by animateFloatAsState(
        targetValue = if (isPlaying) 0f else 1f,
        animationSpec = tween(
            if (reducedMotion) 0 else IrideMotion.Short,
            easing = IrideMotion.EaseOutQuart,
        ),
        label = "mp3PlaybackMorph",
    )
    Canvas(modifier) {
        val progress = pausedProgress
        val unitX = size.width / 960f
        val unitY = size.height / 960f
        val left = lerp(280f, 100f, progress) * unitX
        val triangle = Path().apply {
            moveTo(left, lerp(180f, 220f, progress) * unitY)
            lineTo(lerp(700f, 560f, progress) * unitX, 480f * unitY)
            lineTo(left, lerp(780f, 740f, progress) * unitY)
            close()
        }
        drawPath(triangle, Color.White)
        if (progress > 0f) {
            val barHeight = 520f * progress * unitY
            val barSize = Size(110f * progress * unitX, barHeight)
            val top = (size.height - barHeight) / 2f
            val color = Color.White.copy(alpha = progress)
            drawRect(color, Offset(lerp(740f, 580f, progress) * unitX, top), barSize)
            drawRect(color, Offset(lerp(850f, 770f, progress) * unitX, top), barSize)
        }
    }
}

@Composable
internal fun IrideMp3PlayerHandle(
    state: BottomSheetState,
    screenHeight: Dp,
    isExpandable: Boolean,
    contentColor: Color = Color.White,
    modifier: Modifier = Modifier,
) {
    if (state.isDismissed) return
    val description = stringResource(R.string.mp3_close)
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        modifier = modifier.fillMaxWidth().height(20.dp)
            .graphicsLayer {
                val progress = (state.progress / .25f).coerceIn(0f, 1f)
                val morph = progress * progress * (3f - 2f * progress)
                val centerOffset = lerp(IrideFolio.MiniPlayerDividerOffset.value, -12f, morph).dp
                // One mark follows the sheet: the divider becomes its upper closing handle.
                translationY = (screenHeight - state.value + centerOffset - 10.dp).toPx()
            }
            .then(
                if (isExpandable && !state.isCollapsed) {
                    Modifier.semantics { contentDescription = description }
                        .draggable(
                            state = state,
                            orientation = Orientation.Vertical,
                            onDragStopped = { velocity -> state.performFling(-velocity, null) },
                        )
                        .clickable(
                            interactionSource = interactionSource,
                            indication = null,
                            onClick = state::collapseSoft,
                        )
                } else Modifier,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val progress = (state.progress / .25f).coerceIn(0f, 1f)
            val morph = progress * progress * (3f - 2f * progress)
            val collapsedWidth = (size.width - 2f * IrideFolio.MiniPlayerDividerHorizontalInset.toPx())
                .coerceAtLeast(0f)
            val width = lerp(collapsedWidth, 96.dp.toPx().coerceAtMost(collapsedWidth), morph)
            val thickness = lerp(IrideFolio.MiniPlayerDividerStroke.toPx(), 4.dp.toPx(), morph)
            drawRoundRect(
                color = lerpColor(
                    contentColor.copy(alpha = IrideFolio.MiniPlayerDividerOpacity),
                    Color(0xFFC4C4C4),
                    morph,
                ),
                topLeft = Offset((size.width - width) / 2f, (size.height - thickness) / 2f),
                size = Size(width, thickness),
                cornerRadius = CornerRadius(thickness / 2f),
            )
        }
    }
}

@Composable
private fun BoxScope.WheelZone(
    alignment: Alignment,
    size: Dp,
    // Distance from the wheel's outer edge to the zone's center — centers it in the outer half
    // of the ring band (between the rim and the center hole) instead of a flat margin that
    // ignored how big that band actually was.
    edgeInset: Dp,
    onClick: () -> Unit,
    enabled: Boolean = true,
    // Touch target size; defaults to the icon size. The bottom play/pause zone widens it so a tap
    // landing slightly left/right of the icon still hits instead of falling through to the
    // player's background collapse-on-tap and dropping to the mini player.
    hitWidth: Dp = size,
    hitHeight: Dp = size,
    // Nudges the zone off its raw edge-aligned spot, in sync with the other 3 zones, so the ring
    // of buttons reads as a slightly wider circle centered in the gray band.
    offsetX: Dp = 0.dp,
    offsetY: Dp = 0.dp,
    content: @Composable () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        modifier =
            Modifier.align(alignment)
                .padding(edgeInset)
                .offset(x = offsetX, y = offsetY)
                .size(width = hitWidth, height = hitHeight)
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    enabled = enabled,
                    onClick = onClick,
                ),
        // Anchor the icon to the zone's own edge so a widened hit box grows inward (toward the
        // wheel center) without shifting the icon off its spot.
        contentAlignment = alignment,
    ) {
        Box(
            modifier =
                Modifier.size(size)
                    .pressScale(interactionSource, pressedScale = 0.92f)
                    .clip(CircleShape),
            contentAlignment = Alignment.Center,
            content = { content() },
        )
    }
}

@Composable
fun IrideMiniPlayerBridgeOverlay(
    bridgeState: IrideBridgeState,
    sheetProgress: Float,
    navController: NavController,
    playerBottomSheetState: BottomSheetState,
    modifier: Modifier = Modifier,
) {
    val playerConnection = LocalPlayerConnection.current ?: return
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()
    val metadata = mediaMetadata ?: return
    // Reduced motion: the container-transform morph becomes an instant cut between the mini and
    // player layouts instead of tracking drag position — snapping `eased` to the nearer endpoint
    // collapses every lerp below to that endpoint's value with no interpolation drawn.
    val reducedMotion = rememberReducedMotion()
    val eased =
        if (reducedMotion) {
            if (sheetProgress >= 0.5f) 1f else 0f
        } else {
            sheetProgress.coerceIn(0f, 1f)
        }
    var rootOffset by remember { mutableStateOf(Offset.Zero) }

    val textProgress = ((eased - IrideCoverTextSplit) / (1f - IrideCoverTextSplit)).coerceIn(0f, 1f)

    Box(
        modifier =
            modifier.fillMaxSize().onGloballyPositioned { rootOffset = it.positionInWindow() }
    ) {
        val artStart = bridgeState.miniArt
        val artEnd = bridgeState.playerArt
        if (artStart != null && artEnd != null && !bridgeState.panelActive) {
            // Fades out fast right at the start of the move instead of hanging around for the
            // whole morph — the mini pill's own ring (PillPlayButton) takes back over the instant
            // this one is gone, so it must never be caught visible mid-air with no ring anywhere.
            val ringAlpha = (1f - eased / BridgeRingFadeOutProgress).coerceIn(0f, 1f)
            BridgedElement(
                start = artStart,
                end = artEnd,
                rootOffset = rootOffset,
                progress = eased,
            ) { scale ->
                val onScreenRadius =
                    lerp(PillCoverRadius.value, IrideCoverBorderRadius.value, eased)
                val compensatedRadius =
                    (onScreenRadius / scale.coerceAtLeast(0.01f)).coerceAtMost(200f)
                val context = LocalContext.current
                AsyncImage(
                    // Always requests the source at full quality regardless of this frame's box
                    // size — early in the transition (before the expanded rect is known) this box
                    // briefly measures at the mini pill's small size, and without an explicit
                    // CoilSize.ORIGINAL request Coil decodes/caches a small bitmap for it that then
                    // visibly pops to sharp once the real size lands (the "grainy then upgrades"
                    // regression).
                    model =
                        ImageRequest.Builder(context)
                            .data(metadata.thumbnailUrl?.resize(1080, 1080))
                            .size(CoilSize.ORIGINAL)
                            .build(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize().clip(irideSquircle(compensatedRadius.dp)),
                )
                if (ringAlpha > 0f) {
                    val strokeWidthDp = BridgeRingStrokeWidth / scale.coerceAtLeast(0.01f)
                    Box(
                        modifier =
                            Modifier.matchParentSize()
                                .graphicsLayer { alpha = ringAlpha }
                                .drawWithContent {
                                    drawBridgeProgressRing(
                                        cornerRadiusPx = compensatedRadius.dp.toPx(),
                                        strokeWidthPx = strokeWidthDp.toPx(),
                                        progress = bridgeState.progress,
                                    )
                                }
                    )
                }
            }
        }

        val infoStart = bridgeState.miniInfo
        val infoEnd = bridgeState.playerInfo
        if (infoStart != null && infoEnd != null) {
            BridgedInfoBlock(
                metadata = metadata,
                start = infoStart,
                end = infoEnd,
                rootOffset = rootOffset,
                progress = textProgress,
                navController = navController,
                playerBottomSheetState = playerBottomSheetState,
            )
        }
    }
}

private const val IrideCoverTextSplit = 0.28f

// Total horizontal space the bridge's more/favorite actions occupy once fully revealed
// (6.dp gap + two action targets). Reserved through an animated-width box so the weighted
// title never loses its width in a single frame mid-morph.
private val IrideBridgeInfoActionsWidth = 6.dp + IrideMp3InfoActionSize * 2

// Vertical convergence of the title/artist lines at the collapsed end of the morph. The fixed
// 16.sp title layout is taller than its scaled-down visual, which pushed the two lines apart
// in the mini state; each line translates toward their shared center by half of this amount
// at progress 0 and relaxes back to the laid-out positions by progress 1.
private val IrideBridgeInfoLineConvergence = 5.dp

@Composable
private fun BridgedInfoBlock(
    metadata: MediaMetadata,
    start: Rect,
    end: Rect,
    rootOffset: Offset,
    progress: Float,
    navController: NavController,
    playerBottomSheetState: BottomSheetState,
) {
    val density = LocalDensity.current
    val startLocal = remember(start, rootOffset) { start.translate(-rootOffset.x, -rootOffset.y) }
    val endLocal = remember(end, rootOffset) { end.translate(-rootOffset.x, -rootOffset.y) }
    val left = lerp(startLocal.left, endLocal.left, progress)
    val top = lerp(startLocal.top, endLocal.top, progress)
    val width = lerp(startLocal.width, endLocal.width, progress)

    val miniTitleColor = MaterialTheme.colorScheme.onSurface
    val miniArtistColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)

    val playerConnection = LocalPlayerConnection.current
    val database = LocalDatabase.current
    val librarySong by database.song(metadata.id).collectAsState(initial = null)
    val isEpisode = librarySong?.song?.isEpisode == true
    val isFavorite =
        if (isEpisode) librarySong?.song?.inLibrary != null else librarySong?.song?.liked == true

    val menuState = LocalMenuState.current
    val bottomSheetPageState = LocalBottomSheetPageState.current
    val onMoreClick = {
        menuState.show(anchored = true) {
            PlayerMenu(
                mediaMetadata = metadata,
                navController = navController,
                playerBottomSheetState = playerBottomSheetState,
                onShowDetailsDialog = {
                    metadata.id.let { id -> bottomSheetPageState.show { ShowMediaInfo(id) } }
                },
                onDismiss = menuState::dismiss,
            )
        }
    }

    Column(
        modifier =
            Modifier.offset { IntOffset(left.roundToInt(), top.roundToInt()) }
                .width(with(density) { width.toDp() })
    ) {
        val iconReveal =
            if (playerConnection != null) {
                ((progress - 0.6f) / 0.4f).coerceIn(0f, 1f)
            } else {
                0f
            }
        Box(modifier = Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Fixed end-state size scaled via graphicsLayer instead of animating fontSize:
                // animating fontSize re-measures and re-rasterizes the text every frame (stuttery
                // glyphs, shifting ellipsis/letter spacing), while a GPU transform scales it
                // smoothly with zero relayout.
                Text(
                    text = metadata.title,
                    color = lerpColor(miniTitleColor, Color.White, progress),
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier =
                        Modifier.weight(1f).graphicsLayer {
                            // Mini end sits at 15.5/16 (was 14/16): bigger collapsed title,
                            // expanded end untouched at full 16.sp.
                            val scale = lerp(15.5f / 16f, 1f, progress)
                            scaleX = scale
                            scaleY = scale
                            transformOrigin = TransformOrigin(0f, 0f)
                            translationY =
                                IrideBridgeInfoLineConvergence.toPx() / 2f * (1f - progress)
                        },
                )
                // Width-only reservation for the end actions. A Spacer is measured but never
                // inflates the row's height ï¿½ actually composing the icon boxes in this
                // row made it taller than the title line, which then got vertically centered in
                // the extra empty space and visually pushed away from the artist line below.
                Spacer(Modifier.width(IrideBridgeInfoActionsWidth * iconReveal))
            }
            if (playerConnection != null) {
                // Overlay so the fixed-size click targets never influence the row height; they
                // fade in centered on the title line, exactly where the player's static actions
                // sit.
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier =
                        Modifier.align(Alignment.CenterEnd).graphicsLayer { alpha = iconReveal },
                ) {
                    SongMenuIconButton(
                        onClick = onMoreClick,
                        enabled = iconReveal > 0f,
                        modifier = Modifier.padding(start = 6.dp).size(IrideMp3InfoActionSize),
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.more_vert),
                            contentDescription = null,
                            tint = IrideMp3InfoActionColor,
                            modifier = Modifier.size(IrideMp3InfoActionIconSize),
                        )
                    }
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier =
                            Modifier.size(IrideMp3InfoActionSize).clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                enabled = iconReveal > 0f,
                            ) {
                                playerConnection.service.toggleLike()
                            },
                    ) {
                        Icon(
                            painter =
                                painterResource(
                                    if (isFavorite) R.drawable.favorite
                                    else R.drawable.favorite_border
                                ),
                            contentDescription = null,
                            tint = if (isFavorite) Color.White else IrideMp3InfoActionColor,
                            modifier = Modifier.size(IrideMp3InfoActionIconSize),
                        )
                    }
                }
            }
        }
        if (metadata.artists.any { it.name.isNotBlank() }) {
            IrideArtistText(
                mediaMetadata = metadata,
                color = lerpColor(miniArtistColor, IrideArtistTextColor, progress),
                fontSize = 12.sp,
                modifier =
                    Modifier.graphicsLayer {
                        translationY = -IrideBridgeInfoLineConvergence.toPx() / 2f * (1f - progress)
                    },
                onArtistClick = { artistId ->
                    if (progress < 1f) {
                        playerBottomSheetState.expandSoft()
                    } else if (artistId.isNotBlank()) {
                        navController.navigate("artist/$artistId")
                        playerBottomSheetState.collapseSoft()
                    }
                },
            )
        }
    }
}

@Composable
private fun BridgedElement(
    start: Rect,
    end: Rect,
    rootOffset: Offset,
    progress: Float,
    content: @Composable BoxScope.(scale: Float) -> Unit,
) {
    val density = LocalDensity.current
    val endLocal = remember(end, rootOffset) { end.translate(-rootOffset.x, -rootOffset.y) }
    val frameScaleX = if (end.width > 0f) lerp(start.width / end.width, 1f, progress) else 1f
    val frameScaleY = if (end.height > 0f) lerp(start.height / end.height, 1f, progress) else 1f
    Box(
        modifier =
            Modifier.offset { IntOffset(endLocal.left.roundToInt(), endLocal.top.roundToInt()) }
                .size(with(density) { DpSize(endLocal.width.toDp(), endLocal.height.toDp()) })
                .graphicsLayer {
                    scaleX = frameScaleX
                    scaleY = frameScaleY
                    transformOrigin = TransformOrigin(0f, 0f)
                    translationX = lerp(start.left, end.left, progress) - end.left
                    translationY = lerp(start.top, end.top, progress) - end.top
                }
    ) {
        content(frameScaleX)
    }
}
