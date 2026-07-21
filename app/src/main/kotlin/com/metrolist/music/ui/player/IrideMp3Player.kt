
package com.metrolist.music.ui.player

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
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
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp as lerpColor
import androidx.compose.ui.unit.lerp as lerpTextUnit
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.toSize
import androidx.compose.ui.util.lerp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.compose.ui.zIndex
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.common.Timeline
import androidx.media3.exoplayer.source.ShuffleOrder.DefaultShuffleOrder
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import coil3.imageLoader
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.toBitmap
import com.metrolist.music.LocalDatabase
import com.metrolist.music.LocalPlayerConnection
import com.metrolist.music.R
import com.metrolist.music.extensions.metadata
import com.metrolist.music.extensions.move
import com.metrolist.music.extensions.toMediaItem
import com.metrolist.music.models.MediaMetadata
import com.metrolist.music.playback.queues.ListQueue
import androidx.compose.foundation.layout.BoxWithConstraints
import com.metrolist.music.ui.component.BetterAnimatedGradientBackground
import com.metrolist.music.ui.component.BottomSheetState
import com.metrolist.music.ui.component.LocalBottomSheetPageState
import com.metrolist.music.ui.component.LocalMenuState
import com.metrolist.music.ui.component.Lyrics
import com.metrolist.music.ui.component.LyricsPillController
import com.metrolist.music.ui.component.UnderlinePill
import com.metrolist.music.ui.menu.PlayerMenu
import com.metrolist.music.ui.theme.InterFontFamily
import com.metrolist.music.ui.theme.SpaceMonoFontFamily
import com.metrolist.music.ui.utils.ShowMediaInfo
import com.metrolist.music.utils.makeTimeString
import kotlin.math.roundToInt
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import sv.lib.squircleshape.SquircleShape

internal val IrideMp3BackgroundColor = Color(0xFF0A0A0A)
private val IrideMp3SurfaceColor = Color(0xFF1C1C1F)
private val IrideMp3PanelBorderColor = Color.White.copy(alpha = 0.14f)
private val IrideMp3DimIconColor = Color.White.copy(alpha = 0.55f)

private val IrideMp3WheelCenterColor = Color(0xFF2B2B31)
private val IrideMp3WheelEdgeColor = Color(0xFF131316)
private val IrideMp3HoleCenterColor = Color(0xFF111113)
private val IrideMp3HoleEdgeColor = Color(0xFF060608)
private val IrideMp3WheelRimBrush = Brush.verticalGradient(
    listOf(
        Color.White.copy(alpha = 0.26f),
        Color.White.copy(alpha = 0.09f),
        Color.White.copy(alpha = 0.03f),
    ),
)
private val IrideMp3HoleLipBrush = Brush.verticalGradient(
    listOf(
        Color.White.copy(alpha = 0.02f),
        Color.White.copy(alpha = 0.05f),
        Color.White.copy(alpha = 0.14f),
    ),
)

private const val IrideMp3CoverWidthFraction = 0.82f

// Lifts the control wheel up off the bottom system-gesture strip (see call site) so its bottom
// "more" zone stays fully tappable on gesture-nav devices.
private val WheelBottomGestureClearance = 24.dp

private fun irideSquircle(radius: Dp) = SquircleShape(radius = radius, cornerSmoothing = 0.48f)

@Stable
class IrideBridgeState {
    var miniArt by mutableStateOf<Rect?>(null)
    var playerArt by mutableStateOf<Rect?>(null)
    var miniInfo by mutableStateOf<Rect?>(null)
    var playerInfo by mutableStateOf<Rect?>(null)

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
    onRadioClick: () -> Unit = {},
    onFavoriteClick: () -> Unit = {},
    onSeek: (Float) -> Unit = {},
    onArtistClick: (String) -> Unit = {},
    isLyricsActive: Boolean = false,
    isQueueActive: Boolean = false,
    onLyricsClick: () -> Unit = {},
    onQueueClick: () -> Unit = {},
    isFullScreen: Boolean = false,
    onToggleFullScreen: () -> Unit = {},
    isListenTogetherGuest: Boolean = false,
    isMuted: Boolean = false,
    navController: NavController,
    playerBottomSheetState: BottomSheetState,
    modifier: Modifier = Modifier,
    bottomInset: Dp = 0.dp,
    cornerRevealHeight: Dp = 0.dp,
    bridgeState: IrideBridgeState? = null,
) {
    val menuState = LocalMenuState.current
    val bottomSheetPageState = LocalBottomSheetPageState.current
    var radioActive by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    // Overflow menu trigger, lives in the title row (a flat area outside the circular wheel and
    // outside the lyrics/queue panel) instead of as a wheel zone — the wheel's bottom "more" spot
    // sat right where the round wheel and the square center play/pause button's touch target
    // overlapped, so taps there were unreliable and only landed from the outer edge. A plain row
    // button has no competing hit-test geometry, so a tap always reaches it.
    val onMoreClick = {
        menuState.show {
            PlayerMenu(
                mediaMetadata = mediaMetadata,
                navController = navController,
                playerBottomSheetState = playerBottomSheetState,
                onShowDetailsDialog = {
                    mediaMetadata.id.let { id ->
                        bottomSheetPageState.show { ShowMediaInfo(id) }
                    }
                },
                onDismiss = menuState::dismiss,
            )
        }
    }

    var dragFraction by remember { mutableStateOf<Float?>(null) }

    val panelActive = isLyricsActive || isQueueActive
    // Fullscreen only ever applies to lyrics, never queue — queue always stays in its normal
    // card, and its own fullscreen affordance (button + expand) was removed entirely below.
    val fullScreenActive = isFullScreen && isLyricsActive

    SideEffect {
        bridgeState?.panelActive = panelActive
        bridgeState?.lyricsFullScreenActive = fullScreenActive
    }
    DisposableEffect(bridgeState) {
        onDispose { bridgeState?.lyricsFullScreenActive = false }
    }
    val lyricsPillController = remember { LyricsPillController() }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(IrideMp3BackgroundColor)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = { playerBottomSheetState.collapseSoft() },
            )
            .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Top)),
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
        ) {
            Spacer(Modifier.height(cornerRevealHeight))

            Box(
                modifier = Modifier
                    // True lyrics fullscreen is a separate edge-to-edge Dialog (see
                    // FullScreenLyricsDialog below), not an in-card expand — a box that merely
                    // grows within the player's own bounds reads as "still boxed in", not an
                    // actual fullscreen. This card therefore always stays the normal cover size.
                    .fillMaxWidth(IrideMp3CoverWidthFraction)
                    .padding(top = 8.dp, bottom = 8.dp)
                    .aspectRatio(1f)
                    .align(Alignment.CenterHorizontally)
                    .clip(irideSquircle(16.dp))
                    .border(1.dp, IrideMp3PanelBorderColor, irideSquircle(16.dp)),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .zIndex(if (isLyricsActive || isQueueActive) 0f else 1f)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {},
                        )
                        .then(
                            if (bridgeState != null) {
                                Modifier.irideReportRect { bridgeState.playerArt = it }.alpha(0f)
                            } else {
                                Modifier
                            },
                        ),
                ) {
                    AsyncImage(
                        model = mediaMetadata.thumbnailUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                }

                val lyricsAlpha by animateFloatAsState(
                    targetValue = if (isLyricsActive) 1f else 0f,
                    animationSpec = tween(280, easing = FastOutSlowInEasing),
                    label = "irideLyricsAlpha",
                )
                val currentDragFraction by rememberUpdatedState(dragFraction)
                val currentDuration by rememberUpdatedState(duration)
                val lyricsSliderPositionProvider = remember {
                    { currentDragFraction?.let { (currentDuration * it).toLong() } }
                }
                val lyricsBgContext = LocalContext.current
                var lyricsBgBitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
                LaunchedEffect(mediaMetadata.thumbnailUrl) {
                    val url = mediaMetadata.thumbnailUrl
                    lyricsBgBitmap = if (url != null) {
                        val request = ImageRequest.Builder(lyricsBgContext)
                            .data(url)
                            .size(100, 100)
                            .allowHardware(false)
                            .build()
                        lyricsBgContext.imageLoader.execute(request).image?.toBitmap()
                    } else {
                        null
                    }
                }
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .zIndex(if (isLyricsActive) 1f else 0f)
                        .alpha(lyricsAlpha)
                        .background(IrideMp3BackgroundColor),
                ) {
                    BetterAnimatedGradientBackground(
                        thumbnail = lyricsBgBitmap,
                        modifier = Modifier.fillMaxSize(),
                    )
                    Lyrics(
                        sliderPositionProvider = lyricsSliderPositionProvider,
                        showLyrics = true,
                        showPills = false,
                        // The card itself never expands anymore — real fullscreen is the
                        // separate FullScreenLyricsDialog below, so this inline copy always
                        // stays in its normal (non-fullscreen) layout.
                        isFullScreen = false,
                        pillsController = lyricsPillController,
                        // 15% larger than the default in-card size for a more comfortable read —
                        // scoped to this player only (the fullscreen dialog copy keeps its own size).
                        textScale = 1.15f,
                        // Sits a bit higher than the fullscreen copy (top = 34.dp there) —
                        // this card is small, so lyrics start closer to the top edge instead
                        // of leaving a big empty gap above the first line.
                        modifier = Modifier.fillMaxSize().padding(top = 22.dp, start = 12.dp),
                    )
                }

                if (isFullScreen && isLyricsActive) {
                    FullScreenLyricsDialog(
                        sliderPositionProvider = lyricsSliderPositionProvider,
                        lyricsBgBitmap = lyricsBgBitmap,
                        // Same on-screen rect already tracked for the mini-player <-> full-player
                        // art bridge — this card and the cover art share the same bounds, so it
                        // doubles as the "grow from here" origin for the expand animation below.
                        sourceRect = bridgeState?.playerArt,
                        onDismiss = onToggleFullScreen,
                    )
                }

                val queueAlpha by animateFloatAsState(
                    targetValue = if (isQueueActive) 1f else 0f,
                    animationSpec = tween(280, easing = FastOutSlowInEasing),
                    label = "irideQueueAlpha",
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .zIndex(if (isQueueActive) 1f else 0f)
                        .alpha(queueAlpha)
                        .background(IrideMp3BackgroundColor),
                ) {
                    IrideQueuePreview(
                        mediaMetadata = mediaMetadata,
                        modifier = Modifier.fillMaxSize(),
                        topClearance = 26.dp,
                    )
                }

                // Fullscreen is a lyrics-only affordance now — queue never offers it, so this
                // whole control fades/disables away outside of the lyrics panel instead of also
                // reacting to isQueueActive (which panelActive would).
                val fullscreenAlpha by animateFloatAsState(
                    targetValue = if (!isLyricsActive) {
                        0f
                    } else if (bridgeState == null) {
                        1f
                    } else {
                        ((playerBottomSheetState.progress - 0.85f) / 0.15f).coerceIn(0f, 1f)
                    },
                    animationSpec = tween(200, easing = FastOutSlowInEasing),
                    label = "irideFullscreenAlpha",
                )
                Row(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .zIndex(4f)
                        .graphicsLayer { alpha = fullscreenAlpha }
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // Bigger tap target (was 26.dp/14.dp icon — too small to reliably hit) and a
                    // semi-transparent chip instead of an opaque one, matching the rest of the
                    // panel's overlay buttons.
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.14f))
                            .border(1.dp, IrideMp3PanelBorderColor, CircleShape)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                enabled = isLyricsActive,
                                onClick = onToggleFullScreen,
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            painter = painterResource(if (isFullScreen) R.drawable.expand_less else R.drawable.fullscreen),
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.9f),
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth(IrideMp3CoverWidthFraction)
                    .align(Alignment.CenterHorizontally)
                    .padding(horizontal = 6.dp),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(
                            if (bridgeState != null) {
                                Modifier.irideReportRect { bridgeState.playerInfo = it }.alpha(0f)
                            } else {
                                Modifier
                            },
                        ),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = mediaMetadata.title,
                            color = Color.White,
                            fontFamily = InterFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 16.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .weight(1f, fill = false)
                                .basicMarquee(iterations = 1, initialDelayMillis = 2000),
                        )
                        Spacer(Modifier.width(10.dp))
                        val favoriteScale = remember { Animatable(1f) }
                        LaunchedEffect(isFavorite) {
                            favoriteScale.snapTo(0.7f)
                            favoriteScale.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium))
                        }
                        Icon(
                            painter = painterResource(if (isFavorite) R.drawable.favorite else R.drawable.favorite_border),
                            contentDescription = null,
                            tint = if (isFavorite) Color.White else Color.White.copy(alpha = 0.7f),
                            modifier = Modifier
                                .size(20.dp)
                                .graphicsLayer {
                                    scaleX = favoriteScale.value
                                    scaleY = favoriteScale.value
                                }
                                .clickable { onFavoriteClick() },
                        )
                    }
                    IrideArtistText(
                        mediaMetadata = mediaMetadata,
                        color = Color(0xFFB8B8B8),
                        fontSize = 12.sp,
                        onArtistClick = onArtistClick,
                    )
                }

                Spacer(Modifier.height(14.dp))

                val rawProgress = dragFraction
                    ?: if (duration > 0) (position.toFloat() / duration).coerceIn(0f, 1f) else 0f
                val progress by animateFloatAsState(
                    targetValue = rawProgress,
                    animationSpec = tween(if (dragFraction != null) 0 else 450, easing = FastOutSlowInEasing),
                    label = "irideProgress",
                )
                val seekPulse = remember { Animatable(1f) }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(5.dp)
                        .graphicsLayer {
                            scaleY = seekPulse.value
                            transformOrigin = TransformOrigin(0.5f, 0.5f)
                        }
                        .clip(RoundedCornerShape(50))
                        .background(Color.White.copy(alpha = 0.22f))
                        .pointerInput(duration, isListenTogetherGuest) {
                            if (duration <= 0 || isListenTogetherGuest) return@pointerInput
                            awaitEachGesture {
                                val down = awaitFirstDown()
                                dragFraction = (down.position.x / size.width).coerceIn(0f, 1f)
                                drag(down.id) { change ->
                                    change.consume()
                                    dragFraction = (change.position.x / size.width).coerceIn(0f, 1f)
                                }
                                dragFraction?.let {
                                    onSeek(it)
                                    coroutineScope.launch {
                                        seekPulse.snapTo(1f)
                                        seekPulse.animateTo(1.3f, tween(180, easing = FastOutSlowInEasing))
                                        seekPulse.animateTo(1f, spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow))
                                    }
                                }
                                dragFraction = null
                            }
                        },
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(progress)
                            .clip(RoundedCornerShape(50))
                            .background(Color.White),
                    )
                }

                Spacer(Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = makeTimeString(position),
                        color = Color.White.copy(alpha = 0.65f),
                        fontFamily = InterFontFamily,
                        fontSize = 11.sp,
                    )
                    Text(
                        text = makeTimeString(duration),
                        color = Color.White.copy(alpha = 0.65f),
                        fontFamily = InterFontFamily,
                        fontSize = 11.sp,
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            BoxWithConstraints(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                // Wheel used to be a fixed 260.dp regardless of how much room this weighted
                // box actually got. On shorter screens that overflowed the box (Box doesn't
                // clip), pushing the bottom "more" zone past the visible/tappable area — the
                // three-dot button looked present but taps never landed on it. Clamping to
                // whatever space is really available keeps every wheel zone reachable.
                val wheelSize = minOf(260.dp, maxHeight * 0.92f, maxWidth * 0.92f)
                val scale = wheelSize / 260.dp
                val buttonSize = 74.dp * scale
                val iconSize = 25.dp * scale
                val skipIconSize = 32.dp * scale
                val centerButtonSize = 96.dp * scale
                val centerIconSize = 34.dp * scale

                IrideClickWheel(
                    isPlaying = isPlaying,
                    isRadioActive = radioActive,
                    isMoreActive = menuState.isVisible,
                    isListenTogetherGuest = isListenTogetherGuest,
                    isMuted = isMuted,
                    onPlayPauseClick = onPlayPauseClick,
                    onPreviousClick = onPreviousClick,
                    onNextClick = onNextClick,
                    onRadioClick = {
                        radioActive = !radioActive
                        onRadioClick()
                    },
                    onMoreClick = onMoreClick,
                    wheelSize = wheelSize,
                    buttonSize = buttonSize,
                    iconSize = iconSize,
                    skipIconSize = skipIconSize,
                    centerButtonSize = centerButtonSize,
                    centerIconSize = centerIconSize,
                    modifier = Modifier.align(Alignment.Center),
                )
                Row(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth(IrideMp3CoverWidthFraction)
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    IridePanelLabel(
                        text = "LYRICS",
                        isActive = isLyricsActive,
                        onClick = onLyricsClick,
                    )
                    IridePanelLabel(
                        text = "QUEUE",
                        isActive = isQueueActive,
                        onClick = onQueueClick,
                    )
                }
            }
            // Extra clearance beyond the raw nav-bar inset: on gesture-nav devices the bottom
            // strip is a system gesture zone that eats taps landing in it, and the wheel's bottom
            // "more" zone sat low enough that only the top half of its icon cleared the strip —
            // taps on the lower half never reached the button. Lifting the whole wheel up by this
            // margin keeps the three-dot zone fully tappable.
            Spacer(Modifier.height(bottomInset + WheelBottomGestureClearance))
        }
    }
}

/**
 * True fullscreen for lyrics: a separate, edge-to-edge Android [Dialog] window instead of the
 * player card merely growing within its own bounds. A Dialog gets its own Window, so it is
 * guaranteed to draw above everything else in the activity (status bar, nav bar, any other
 * composable) — the previous in-card "expand" approach still shared the activity's single window
 * with everything else, which is why some UI (title/date texts) could end up drawn above it: draw
 * order there depends on composition order, not on being "the fullscreen one".
 */
@Composable
private fun FullScreenLyricsDialog(
    sliderPositionProvider: () -> Long?,
    lyricsBgBitmap: android.graphics.Bitmap?,
    // On-screen rect (window coordinates) of the small in-card lyrics view this dialog is
    // expanding from — null falls back to a plain fade-in, no grow animation.
    sourceRect: Rect?,
    onDismiss: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val progress = remember { Animatable(0f) }
    var closing by remember { mutableStateOf(false) }

    // Routes every close path (back press, close button, tap-to-exit inside Lyrics) through the
    // same shrink-back-down animation before actually tearing the dialog down, so closing mirrors
    // the expand-in instead of just vanishing.
    fun requestClose() {
        if (closing) return
        closing = true
        scope.launch {
            progress.animateTo(0f, tween(220, easing = FastOutSlowInEasing))
            onDismiss()
        }
    }

    LaunchedEffect(Unit) {
        progress.animateTo(1f, tween(320, easing = FastOutSlowInEasing))
    }

    Dialog(
        onDismissRequest = ::requestClose,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
            // Content always fills the whole screen, so there is no "outside" area a real tap
            // could land on — leaving this on only risks a stray dismiss-on-open if a click gets
            // delivered before the first layout pass sizes the content to the full window.
            dismissOnClickOutside = false,
        ),
    ) {
        val view = LocalView.current
        DisposableEffect(Unit) {
            val dialogWindow = (view.parent as? DialogWindowProvider)?.window
            if (dialogWindow != null) {
                WindowCompat.setDecorFitsSystemWindows(dialogWindow, false)
                val controller = WindowInsetsControllerCompat(dialogWindow, view)
                controller.hide(WindowInsetsCompat.Type.systemBars())
                controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
            onDispose {
                if (dialogWindow != null) {
                    WindowCompat.setDecorFitsSystemWindows(dialogWindow, true)
                    WindowInsetsControllerCompat(dialogWindow, view).show(WindowInsetsCompat.Type.systemBars())
                }
            }
        }

        val pillController = remember { LyricsPillController() }
        val density = LocalDensity.current

        // BoxWithConstraints resolves maxWidth/maxHeight on the first measure pass — unlike
        // onGloballyPositioned, no one-frame flash at a wrong (zero) size before the real
        // fullscreen bounds are known.
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val endRect = remember(maxWidth, maxHeight) {
                with(density) { Rect(0f, 0f, maxWidth.toPx(), maxHeight.toPx()) }
            }
            val startRect = sourceRect ?: endRect
            val p = progress.value
            val scaleX = if (endRect.width > 0f) lerp(startRect.width / endRect.width, 1f, p) else 1f
            val scaleY = if (endRect.height > 0f) lerp(startRect.height / endRect.height, 1f, p) else 1f
            val translateX = lerp(startRect.left, endRect.left, p) - endRect.left
            val translateY = lerp(startRect.top, endRect.top, p) - endRect.top

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        this.scaleX = scaleX
                        this.scaleY = scaleY
                        transformOrigin = TransformOrigin(0f, 0f)
                        translationX = translateX
                        translationY = translateY
                        alpha = lerp(0.4f, 1f, p)
                    }
                    .background(IrideMp3BackgroundColor),
            ) {
                BetterAnimatedGradientBackground(
                    thumbnail = lyricsBgBitmap,
                    modifier = Modifier.fillMaxSize(),
                )
                Lyrics(
                    sliderPositionProvider = sliderPositionProvider,
                    showLyrics = true,
                    showPills = false,
                    isFullScreen = true,
                    onExitFullScreen = ::requestClose,
                    pillsController = pillController,
                    modifier = Modifier.fillMaxSize().padding(top = 34.dp, start = 12.dp),
                )

                Row(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .windowInsetsPadding(WindowInsets.systemBars)
                        // Lowered a bit from the top edge (was a flat 16.dp) so it doesn't sit
                        // flush against the status bar strip.
                        .padding(top = 26.dp, start = 16.dp, end = 16.dp, bottom = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (pillController.hasTranslations) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.9f))
                                .border(1.dp, IrideMp3PanelBorderColor, CircleShape)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    onClick = { pillController.translateAction() },
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.translate),
                                contentDescription = null,
                                tint = Color.Black,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.14f))
                            .border(1.dp, IrideMp3PanelBorderColor, CircleShape)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = { requestClose() },
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.expand_less),
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.9f),
                            modifier = Modifier.size(22.dp),
                        )
                    }
                }
            }
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
) {
    val annotated = remember(mediaMetadata.artists) {
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
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        textDecoration = if (isPressed) TextDecoration.Underline else null,
        onTextLayout = { layoutResult = it },
        modifier = modifier
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
                    annotated.getStringAnnotations("artist", charOffset, charOffset)
                        .firstOrNull()
                        ?.let { ann -> if (ann.item.isNotBlank()) onArtistClick(ann.item) }
                },
            ),
    )
}

private enum class IrideQueueLevel { BASE, RADIO, ARTIST }
private enum class IrideRadioSubMode { STANDARD, CLOSE, DISCOVER }
private enum class IrideArtistSubMode { TOP, UNHEARD, DEEP_CUTS }

@Composable
private fun IrideQueuePreview(
    mediaMetadata: MediaMetadata,
    modifier: Modifier = Modifier,
    topClearance: Dp = 0.dp,
) {
    val playerConnection = LocalPlayerConnection.current ?: return
    val database = LocalDatabase.current
    val coroutineScope = rememberCoroutineScope()
    val queueWindows by playerConnection.queueWindows.collectAsState()
    val currentWindowIndex by playerConnection.currentWindowIndex.collectAsState()

    val mutableQueueWindows = remember { mutableStateListOf<Timeline.Window>() }
    LaunchedEffect(queueWindows) {
        mutableQueueWindows.apply {
            clear()
            addAll(queueWindows)
        }
    }

    val currentPlayingUid = remember(currentWindowIndex, queueWindows) {
        if (currentWindowIndex in queueWindows.indices) queueWindows[currentWindowIndex].uid else null
    }

    var queueLevel by remember { mutableStateOf(IrideQueueLevel.BASE) }
    var activeRadioSubMode by remember { mutableStateOf<IrideRadioSubMode?>(null) }
    var activeArtistSubMode by remember { mutableStateOf<IrideArtistSubMode?>(null) }

    fun applyUpcomingReorder(desiredUpcomingOrder: List<Int>) {
        val upcomingStart = currentWindowIndex + 1
        if (upcomingStart >= queueWindows.size || desiredUpcomingOrder.isEmpty()) return
        if (playerConnection.player.shuffleModeEnabled) {
            val newOrder = queueWindows.indices.map { i ->
                if (i < upcomingStart) {
                    queueWindows[i].firstPeriodIndex
                } else {
                    queueWindows[desiredUpcomingOrder[i - upcomingStart]].firstPeriodIndex
                }
            }
            playerConnection.player.setShuffleOrder(
                DefaultShuffleOrder(newOrder.toIntArray(), System.currentTimeMillis()),
            )
        } else {
            val working = queueWindows.indices.toMutableList()
            for (targetPos in upcomingStart until queueWindows.size) {
                val desiredOriginalIndex = desiredUpcomingOrder[targetPos - upcomingStart]
                val currentPos = working.indexOf(desiredOriginalIndex)
                if (currentPos != -1 && currentPos != targetPos) {
                    playerConnection.player.moveMediaItem(currentPos, targetPos)
                    working.add(targetPos, working.removeAt(currentPos))
                }
            }
        }
    }

    fun selectRadioMode(mode: IrideRadioSubMode) {
        activeRadioSubMode = mode
        playerConnection.startRadioForSong(mediaMetadata)
        when (mode) {
            IrideRadioSubMode.STANDARD -> Unit
            IrideRadioSubMode.CLOSE -> {
                val currentArtistIds = mediaMetadata.artists.mapNotNull { it.id }.toSet()
                val currentArtistNames = mediaMetadata.artists.map { it.name.lowercase() }.toSet()
                val upcomingStart = currentWindowIndex + 1
                val desiredOrder = queueWindows
                    .drop(upcomingStart)
                    .mapIndexed { offset, window -> (upcomingStart + offset) to window }
                    .sortedByDescending { (_, window) ->
                        window.mediaItem.metadata?.artists?.any { a ->
                            (a.id != null && a.id in currentArtistIds) || a.name.lowercase() in currentArtistNames
                        } == true
                    }
                    .map { it.first }
                applyUpcomingReorder(desiredOrder)
            }
            IrideRadioSubMode.DISCOVER -> {
                coroutineScope.launch {
                    val upcomingStart = currentWindowIndex + 1
                    val upcoming = queueWindows
                        .drop(upcomingStart)
                        .mapIndexed { offset, window -> (upcomingStart + offset) to window }
                    val playTimes = upcoming.associate { (idx, window) ->
                        val id = window.mediaItem.metadata?.id
                        idx to (id?.let { database.song(it).first()?.song?.totalPlayTime } ?: 0L)
                    }
                    val desiredOrder = upcoming.map { it.first }.sortedBy { playTimes[it] ?: 0L }
                    applyUpcomingReorder(desiredOrder)
                }
            }
        }
    }

    fun selectArtistMode(mode: IrideArtistSubMode) {
        activeArtistSubMode = mode
        // "That artist" means every artist credited on the currently playing song (the
        // primary artist and any featured/collab singers) — not just the first one, so a
        // collab track pulls from both artists' catalogs instead of arbitrarily picking one.
        val artistIds = mediaMetadata.artists.mapNotNull { it.id }
        if (artistIds.isEmpty()) return
        coroutineScope.launch {
            val songs = artistIds
                .flatMap { database.artistSongsByPlayTimeAsc(it).first() }
                .distinctBy { it.song.id }
            val picked = when (mode) {
                IrideArtistSubMode.TOP -> songs.asReversed().take(30)
                IrideArtistSubMode.UNHEARD -> songs.filter { it.song.totalPlayTime == 0L }
                IrideArtistSubMode.DEEP_CUTS -> songs.filter { it.song.totalPlayTime > 0L }.take(30)
            }
            if (picked.isNotEmpty()) {
                playerConnection.playQueue(
                    ListQueue(
                        title = mediaMetadata.artists.firstOrNull()?.name,
                        items = picked.map { it.toMediaItem() },
                    ),
                )
            }
        }
    }

    Column(modifier = modifier.padding(top = topClearance)) {
        IrideQueueModePanel(
            queueLevel = queueLevel,
            activeRadioSubMode = activeRadioSubMode,
            activeArtistSubMode = activeArtistSubMode,
            onKeepQueue = {
                queueLevel = IrideQueueLevel.BASE
                activeRadioSubMode = null
                activeArtistSubMode = null
            },
            onRadioToggle = {
                queueLevel = if (queueLevel == IrideQueueLevel.RADIO) IrideQueueLevel.BASE else IrideQueueLevel.RADIO
                activeArtistSubMode = null
            },
            onArtistToggle = {
                queueLevel = if (queueLevel == IrideQueueLevel.ARTIST) IrideQueueLevel.BASE else IrideQueueLevel.ARTIST
                activeRadioSubMode = null
            },
            onRadioSubModeSelect = ::selectRadioMode,
            onArtistSubModeSelect = ::selectArtistMode,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
        )

        if (mutableQueueWindows.isEmpty()) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text(
                    text = "QUEUE EMPTY",
                    color = Color.White.copy(alpha = 0.6f),
                    fontFamily = InterFontFamily,
                    fontSize = 12.sp,
                    letterSpacing = 0.5.sp,
                )
            }
            return@Column
        }

        val lazyListState = rememberLazyListState()
        var dragInfo by remember { mutableStateOf<Pair<Int, Int>?>(null) }
        val reorderableState = rememberReorderableLazyListState(lazyListState = lazyListState) { from, to ->
            val currentDragInfo = dragInfo
            dragInfo = if (currentDragInfo == null) from.index to to.index else currentDragInfo.first to to.index
            mutableQueueWindows.move(from.index, to.index)
        }

        LaunchedEffect(reorderableState.isAnyItemDragging) {
            if (!reorderableState.isAnyItemDragging) {
                dragInfo?.let { (from, to) ->
                    val safeFrom = from.coerceIn(0, queueWindows.lastIndex)
                    val safeTo = to.coerceIn(0, queueWindows.lastIndex)
                    if (!playerConnection.player.shuffleModeEnabled) {
                        playerConnection.player.moveMediaItem(safeFrom, safeTo)
                    } else {
                        playerConnection.player.setShuffleOrder(
                            DefaultShuffleOrder(
                                queueWindows
                                    .map { it.firstPeriodIndex }
                                    .toMutableList()
                                    .move(safeFrom, safeTo)
                                    .toIntArray(),
                                System.currentTimeMillis(),
                            ),
                        )
                    }
                    dragInfo = null
                }
            }
        }

        LazyColumn(
            state = lazyListState,
            modifier = Modifier.weight(1f).padding(start = 10.dp, end = 10.dp, top = 2.dp, bottom = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            itemsIndexed(mutableQueueWindows, key = { _, window -> window.uid.hashCode() }) { _, window ->
                ReorderableItem(state = reorderableState, key = window.uid.hashCode()) {
                    val metadata = window.mediaItem.metadata
                    val isActive = window.uid == currentPlayingUid
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .animateItem()
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                            ) { playerConnection.player.seekToDefaultPosition(window.firstPeriodIndex) },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        AsyncImage(
                            model = metadata?.thumbnailUrl,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(34.dp)
                                .clip(irideSquircle(8.dp)),
                        )
                        Spacer(Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = metadata?.title.orEmpty(),
                                color = if (isActive) Color.White else Color.White.copy(alpha = 0.75f),
                                fontFamily = InterFontFamily,
                                fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal,
                                fontSize = 12.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            if (metadata != null && metadata.artists.any { it.name.isNotBlank() }) {
                                Text(
                                    text = metadata.artists.joinToString(", ") { it.name },
                                    color = Color(0xFFB8B8B8),
                                    fontFamily = InterFontFamily,
                                    fontSize = 10.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                        Icon(
                            painter = painterResource(R.drawable.drag_handle),
                            contentDescription = null,
                            tint = IrideMp3DimIconColor,
                            modifier = Modifier
                                .size(18.dp)
                                .draggableHandle(),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun IrideQueueModePanel(
    queueLevel: IrideQueueLevel,
    activeRadioSubMode: IrideRadioSubMode?,
    activeArtistSubMode: IrideArtistSubMode?,
    onKeepQueue: () -> Unit,
    onRadioToggle: () -> Unit,
    onArtistToggle: () -> Unit,
    onRadioSubModeSelect: (IrideRadioSubMode) -> Unit,
    onArtistSubModeSelect: (IrideArtistSubMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Text(
            text = "QUEUE MODE",
            color = Color.White.copy(alpha = 0.6f),
            fontFamily = SpaceMonoFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 10.sp,
            letterSpacing = 1.sp,
        )
        Spacer(Modifier.height(4.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            UnderlinePill(
                text = "KEEP QUEUE",
                selected = queueLevel == IrideQueueLevel.BASE,
                onClick = onKeepQueue,
            )
            UnderlinePill(
                text = "RADIO",
                selected = queueLevel == IrideQueueLevel.RADIO,
                onClick = onRadioToggle,
            )
            UnderlinePill(
                text = "ARTIST",
                selected = queueLevel == IrideQueueLevel.ARTIST,
                onClick = onArtistToggle,
            )
        }
        if (queueLevel == IrideQueueLevel.RADIO) {
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                UnderlinePill(
                    text = "STANDARD",
                    selected = activeRadioSubMode == IrideRadioSubMode.STANDARD,
                    onClick = { onRadioSubModeSelect(IrideRadioSubMode.STANDARD) },
                )
                UnderlinePill(
                    text = "CLOSE",
                    selected = activeRadioSubMode == IrideRadioSubMode.CLOSE,
                    onClick = { onRadioSubModeSelect(IrideRadioSubMode.CLOSE) },
                )
                UnderlinePill(
                    text = "DISCOVER",
                    selected = activeRadioSubMode == IrideRadioSubMode.DISCOVER,
                    onClick = { onRadioSubModeSelect(IrideRadioSubMode.DISCOVER) },
                )
            }
        }
        if (queueLevel == IrideQueueLevel.ARTIST) {
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                UnderlinePill(
                    text = "TOP",
                    selected = activeArtistSubMode == IrideArtistSubMode.TOP,
                    onClick = { onArtistSubModeSelect(IrideArtistSubMode.TOP) },
                )
                UnderlinePill(
                    text = "UNHEARD",
                    selected = activeArtistSubMode == IrideArtistSubMode.UNHEARD,
                    onClick = { onArtistSubModeSelect(IrideArtistSubMode.UNHEARD) },
                )
                UnderlinePill(
                    text = "DEEP CUTS",
                    selected = activeArtistSubMode == IrideArtistSubMode.DEEP_CUTS,
                    onClick = { onArtistSubModeSelect(IrideArtistSubMode.DEEP_CUTS) },
                )
            }
        }
    }
}

@Composable
private fun IrideClickWheel(
    isPlaying: Boolean,
    isRadioActive: Boolean,
    isMoreActive: Boolean,
    isListenTogetherGuest: Boolean,
    isMuted: Boolean,
    onPlayPauseClick: () -> Unit,
    onPreviousClick: () -> Unit,
    onNextClick: () -> Unit,
    onRadioClick: () -> Unit,
    onMoreClick: () -> Unit,
    wheelSize: Dp,
    buttonSize: Dp,
    iconSize: Dp,
    skipIconSize: Dp,
    centerButtonSize: Dp,
    centerIconSize: Dp,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.requiredSize(wheelSize),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .requiredSize(wheelSize)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        listOf(IrideMp3WheelCenterColor, IrideMp3WheelEdgeColor),
                    ),
                )
                .border(
                    width = 1.dp,
                    brush = IrideMp3WheelRimBrush,
                    shape = CircleShape,
                ),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(5.dp)
                    .border(1.dp, Color.White.copy(alpha = 0.05f), CircleShape),
            )
        }

        val playPauseInteraction = remember { MutableInteractionSource() }
        val playPausePressed by playPauseInteraction.collectIsPressedAsState()
        val playPauseScale by animateFloatAsState(
            targetValue = if (playPausePressed) 0.9f else 1f,
            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessHigh),
            label = "irideWheelPlayPauseScale",
        )
        Box(
            modifier = Modifier
                .size(centerButtonSize)
                .graphicsLayer {
                    scaleX = playPauseScale
                    scaleY = playPauseScale
                }
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        listOf(IrideMp3HoleCenterColor, IrideMp3HoleEdgeColor),
                    ),
                )
                .border(1.dp, IrideMp3HoleLipBrush, CircleShape)
                .clickable(
                    interactionSource = playPauseInteraction,
                    indication = null,
                    onClick = onPlayPauseClick,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(
                    if (isListenTogetherGuest) {
                        if (isMuted) R.drawable.volume_off else R.drawable.volume_up
                    } else if (isPlaying) {
                        R.drawable.ic_iride_pause
                    } else {
                        R.drawable.ic_iride_play
                    },
                ),
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(centerIconSize),
            )
        }

        // Drawn last (on top of the center play/pause knob) so these zones win the hit test
        // in the sliver where the knob's square touch target creeps past its round edge into
        // the bottom "more" zone — without this, taps near the wheel's center on that zone
        // were swallowed by the play/pause button and only the outer edge of the button worked.
        Box(modifier = Modifier.requiredSize(wheelSize)) {
            WheelZone(alignment = Alignment.TopCenter, size = buttonSize, onClick = onRadioClick) {
                Icon(
                    painter = painterResource(R.drawable.radio),
                    contentDescription = null,
                    tint = if (isRadioActive) Color.White else IrideMp3DimIconColor,
                    modifier = Modifier.size(iconSize),
                )
            }
            WheelZone(
                alignment = Alignment.CenterEnd,
                size = buttonSize,
                onClick = onNextClick,
                enabled = !isListenTogetherGuest,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_iride_skip_next),
                    contentDescription = null,
                    tint = if (isListenTogetherGuest) IrideMp3DimIconColor else Color.White,
                    modifier = Modifier.size(skipIconSize),
                )
            }
            WheelZone(
                alignment = Alignment.BottomCenter,
                size = buttonSize,
                onClick = onMoreClick,
                // Wide bottom strip: the empty bottom-left/right of the wheel (prev/next sit at
                // mid-height, not down here) becomes part of the "more" target so a near-miss no
                // longer collapses the player.
                hitWidth = buttonSize * 2.2f,
            ) {
                Icon(
                    painter = painterResource(R.drawable.more_vert),
                    contentDescription = null,
                    tint = if (isMoreActive) Color.White else IrideMp3DimIconColor,
                    modifier = Modifier.size(iconSize),
                )
            }
            WheelZone(
                alignment = Alignment.CenterStart,
                size = buttonSize,
                onClick = onPreviousClick,
                enabled = !isListenTogetherGuest,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_iride_skip_previous),
                    contentDescription = null,
                    tint = if (isListenTogetherGuest) IrideMp3DimIconColor else Color.White,
                    modifier = Modifier.size(skipIconSize),
                )
            }
        }
    }
}

@Composable
private fun BoxScope.WheelZone(
    alignment: Alignment,
    size: Dp,
    onClick: () -> Unit,
    enabled: Boolean = true,
    // Touch target size; defaults to the icon size. The bottom "more" zone widens it so a tap
    // landing slightly left/right of the dots still opens the menu instead of falling through to
    // the player's background collapse-on-tap and dropping to the mini player.
    hitWidth: Dp = size,
    hitHeight: Dp = size,
    content: @Composable () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.92f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessHigh),
        label = "irideWheelZoneScale",
    )
    Box(
        modifier = Modifier
            .align(alignment)
            .padding(12.dp)
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
            modifier = Modifier
                .size(size)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }
                .clip(CircleShape),
            contentAlignment = Alignment.Center,
            content = { content() },
        )
    }
}

/**
 * Plain-text LYRICS/QUEUE toggle — gray at rest, white when active. No icon, no background:
 * pinned into the corners of the control pad's Box (see [IrideMp3PlayerContent]), not stacked
 * next to the wheel itself.
 */
@Composable
private fun IridePanelLabel(
    text: String,
    isActive: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val color by animateColorAsState(
        targetValue = if (isActive) Color.White else Color.White.copy(alpha = 0.55f),
        animationSpec = tween(200, easing = FastOutSlowInEasing),
        label = "iridePanelLabelColor",
    )
    Text(
        text = text,
        color = color,
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 12.sp,
        letterSpacing = 0.5.sp,
        modifier = modifier
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            ),
    )
}

@Composable
fun IrideMiniPlayerBridgeOverlay(
    bridgeState: IrideBridgeState,
    sheetProgress: Float,
    modifier: Modifier = Modifier,
) {
    val playerConnection = LocalPlayerConnection.current ?: return
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()
    val metadata = mediaMetadata ?: return
    val eased = sheetProgress.coerceIn(0f, 1f)
    var rootOffset by remember { mutableStateOf(Offset.Zero) }

    val textProgress = ((eased - IrideCoverTextSplit) / (1f - IrideCoverTextSplit)).coerceIn(0f, 1f)

    Box(
        modifier = modifier
            .fillMaxSize()
            .onGloballyPositioned { rootOffset = it.positionInWindow() },
    ) {
        val artStart = bridgeState.miniArt ?: bridgeState.playerArt
        val artEnd = bridgeState.playerArt ?: artStart
        if (artStart != null && artEnd != null && !bridgeState.panelActive) {
            BridgedElement(start = artStart, end = artEnd, rootOffset = rootOffset, progress = eased) { scale ->
                val onScreenRadius = lerp(14f, 16f, eased)
                val compensatedRadius = (onScreenRadius / scale.coerceAtLeast(0.01f)).coerceAtMost(200f)
                AsyncImage(
                    model = metadata.thumbnailUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(irideSquircle(compensatedRadius.dp)),
                )
            }
        }

        val infoStart = bridgeState.miniInfo ?: bridgeState.playerInfo
        val infoEnd = bridgeState.playerInfo ?: infoStart
        if (infoStart != null && infoEnd != null) {
            BridgedInfoBlock(
                metadata = metadata,
                start = infoStart,
                end = infoEnd,
                rootOffset = rootOffset,
                progress = textProgress,
            )
        }
    }
}

private const val IrideCoverTextSplit = 0.28f

@Composable
private fun BridgedInfoBlock(
    metadata: MediaMetadata,
    start: Rect,
    end: Rect,
    rootOffset: Offset,
    progress: Float,
) {
    val density = LocalDensity.current
    val startLocal = remember(start, rootOffset) { start.translate(-rootOffset.x, -rootOffset.y) }
    val endLocal = remember(end, rootOffset) { end.translate(-rootOffset.x, -rootOffset.y) }
    val left = lerp(startLocal.left, endLocal.left, progress)
    val top = lerp(startLocal.top, endLocal.top, progress)
    val width = lerp(startLocal.width, endLocal.width, progress)

    val miniTitleColor = MaterialTheme.colorScheme.onSurface
    val miniArtistColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)

    Column(
        modifier = Modifier
            .offset { IntOffset(left.roundToInt(), top.roundToInt()) }
            .width(with(density) { width.toDp() }),
    ) {
        Text(
            text = metadata.title,
            color = lerpColor(miniTitleColor, Color.White, progress),
            fontFamily = InterFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = lerpTextUnit(14.sp, 16.sp, progress),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (metadata.artists.any { it.name.isNotBlank() }) {
            Text(
                text = metadata.artists.joinToString(", ") { it.name },
                color = lerpColor(miniArtistColor, Color.Gray, progress),
                fontFamily = InterFontFamily,
                fontSize = lerpTextUnit(12.sp, 12.sp, progress),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
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
        modifier = Modifier
            .offset { IntOffset(endLocal.left.roundToInt(), endLocal.top.roundToInt()) }
            .size(with(density) { DpSize(endLocal.width.toDp(), endLocal.height.toDp()) })
            .graphicsLayer {
                scaleX = frameScaleX
                scaleY = frameScaleY
                transformOrigin = TransformOrigin(0f, 0f)
                translationX = lerp(start.left, end.left, progress) - end.left
                translationY = lerp(start.top, end.top, progress) - end.top
            },
    ) {
        content(frameScaleX)
    }
}
