
package com.metrolist.music.ui.player

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
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
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp as lerpColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
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
import androidx.media3.common.MediaItem
import androidx.media3.common.Timeline
import androidx.media3.exoplayer.source.ShuffleOrder.DefaultShuffleOrder
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import coil3.imageLoader
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.size.Size
import coil3.toBitmap
import com.metrolist.music.LocalDatabase
import com.metrolist.music.LocalPlayerConnection
import com.metrolist.music.R
import com.metrolist.music.constants.IrideBaseBorderWidth
import com.metrolist.music.constants.LyricsRomanizeToggleKey
import com.metrolist.music.extensions.metadata
import com.metrolist.music.extensions.move
import com.metrolist.music.extensions.toMediaItem
import com.metrolist.music.models.MediaMetadata
import com.metrolist.music.models.toMediaMetadata
import androidx.compose.foundation.layout.BoxWithConstraints
import com.metrolist.music.ui.component.BetterAnimatedGradientBackground
import com.metrolist.music.ui.component.BottomSheetState
import com.metrolist.music.ui.component.GenreSongInfo
import com.metrolist.music.ui.component.LocalBottomSheetPageState
import com.metrolist.music.ui.component.LocalMenuState
import com.metrolist.music.ui.component.Lyrics
import com.metrolist.music.ui.component.LyricsPillController
import com.metrolist.music.ui.component.PillCoverRadius
import com.metrolist.music.ui.component.UnderlinePill
import com.metrolist.music.ui.component.rememberGenreFilter
import com.metrolist.music.ui.menu.PlayerMenu
import com.metrolist.music.ui.theme.InterFontFamily
import com.metrolist.music.ui.theme.SpaceMonoFontFamily
import com.metrolist.music.ui.utils.IrideMotion
import com.metrolist.music.ui.utils.ShowMediaInfo
import com.metrolist.music.ui.utils.pressScale
import com.metrolist.music.ui.utils.rememberReducedMotion
import com.metrolist.music.utils.makeTimeString
import com.metrolist.music.utils.rememberPreference
import kotlin.math.roundToInt
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import sv.lib.squircleshape.SquircleShape

internal val IrideMp3BackgroundColor = Color(0xFF0A0A0A)
private val IrideMp3SurfaceColor = Color(0xFF1C1C1F)
private val IrideMp3PanelBorderColor = Color.White.copy(alpha = 0.14f)
// Bumped from 0.55 — under direct sunlight/glare the low-alpha icons on the (also lightened)
// wheel background were washing out to near-invisible.
private val IrideMp3DimIconColor = Color.White.copy(alpha = 0.75f)

// Shared artist-line grey — single source of truth for both the static info block and the
// bridge overlay, so the expanded state renders identically no matter which path draws it.
private val IrideArtistTextColor = Color(0xFFB8B8B8)

// Flat grey surface, matching the rest of the New Iride UI (AlbumScreen panels/chips): a solid
// fill plus a single hairline border instead of a gradient disc.
private val IrideMp3WheelSurfaceColor = IrideMp3SurfaceColor

private const val IrideMp3CoverWidthFraction = 0.84f

// Matches AlbumScreen's cover squircle radius exactly (see item 5: one shared cover border
// language across the app) — also the END radius the bridge overlay's clip lerps toward.
private val IrideCoverBorderRadius = 12.dp

// How much of the expand/collapse `eased` progress the bridge overlay's ring gets before it's
// gone — short on purpose, "quasi immediatamente": the ring's job is just to bridge the mini
// pill's own ring into the move, not to survive the whole morph.
private const val BridgeRingFadeOutProgress = 0.12f
private val BridgeRingStrokeWidth = 2.5.dp

/** Same technique as PillProgressDrawCache, minus its size/shape cache — this only ever draws
 * for [BridgeRingFadeOutProgress] worth of a single expand/collapse, so recomputing the traced
 * outline every frame is cheaper than the bookkeeping needed to cache it correctly. */
private fun DrawScope.drawBridgeProgressRing(
    cornerRadiusPx: Float,
    strokeWidthPx: Float,
    progress: Float,
) {
    val inset = strokeWidthPx / 2f
    val path = Path().apply {
        addRoundRect(
            RoundRect(
                left = inset,
                top = inset,
                right = size.width - inset,
                bottom = size.height - inset,
                cornerRadius = CornerRadius(cornerRadiusPx),
            ),
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
    // Bumped by the wheel's Radio button to jump the (already open) UP NEXT panel straight
    // into Auto-Mix — see IrideQueuePreview's radioTrigger param.
    radioTrigger: Int = 0,
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
    // Reflects whether Auto-Mix has actually been committed as the live queue (see
    // MusicService.commitAutomixAsQueue) — not a local toggle, so the button only glows
    // when Radio genuinely did something.
    val radioActive by LocalPlayerConnection.current?.service?.isAutoMixQueueActive?.collectAsState() ?: remember { mutableStateOf(false) }

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
    val reducedMotion = rememberReducedMotion()

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
                    .clip(irideSquircle(IrideCoverBorderRadius))
                    // Same thin cover border as AlbumScreen (IrideBaseBorderWidth, white 0.22) —
                    // one shared visual language for every cover in the New Iride UI, not this
                    // screen's own weaker/thinner one.
                    .border(IrideBaseBorderWidth, Color.White.copy(alpha = 0.22f), irideSquircle(IrideCoverBorderRadius)),
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
                    animationSpec = tween(if (reducedMotion) 0 else IrideMotion.Short, easing = IrideMotion.EaseOutQuart),
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
                            .memoryCacheKey("gradient_${mediaMetadata.id}")
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
                    // Skipped entirely while the fullscreen dialog is up (see above) and, more
                    // importantly, unmounted whenever the panel is fully hidden: rememberInfiniteTransition
                    // keeps redrawing the Canvas (120.dp blur + 4 rotating full-size sprite draws)
                    // every frame for as long as it stays composed, alpha=0 does not stop that —
                    // leaving it always mounted meant this heaviest animation on screen ran
                    // continuously for the whole player session, not just while LYRICS was open.
                    if (!fullScreenActive && (isLyricsActive || lyricsAlpha > 0f)) {
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
                }

                if (isFullScreen && isLyricsActive) {
                    FullScreenLyricsDialog(
                        sliderPositionProvider = lyricsSliderPositionProvider,
                        lyricsBgBitmap = lyricsBgBitmap,
                        onDismiss = onToggleFullScreen,
                    )
                }

                val queueAlpha by animateFloatAsState(
                    targetValue = if (isQueueActive) 1f else 0f,
                    animationSpec = tween(if (reducedMotion) 0 else IrideMotion.Short, easing = IrideMotion.EaseOutQuart),
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
                        radioTrigger = radioTrigger,
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
                    animationSpec = tween(if (reducedMotion) 0 else IrideMotion.Quick, easing = IrideMotion.EaseOutQuart),
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
                                .weight(1f)
                                .basicMarquee(iterations = 1, initialDelayMillis = 2000),
                        )
                        Spacer(Modifier.width(6.dp))
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(36.dp)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                ) { onMoreClick() },
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.more_vert),
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.7f),
                                modifier = Modifier.size(20.dp),
                            )
                        }
                        val favoriteScale = remember { Animatable(1f) }
                        LaunchedEffect(isFavorite) {
                            if (reducedMotion) {
                                favoriteScale.snapTo(1f)
                            } else {
                                favoriteScale.snapTo(0.7f)
                                favoriteScale.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium))
                            }
                        }
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.size(36.dp),
                        ) {
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
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null,
                                    ) { onFavoriteClick() },
                            )
                        }
                    }
                    IrideArtistText(
                        mediaMetadata = mediaMetadata,
                        color = IrideArtistTextColor,
                        fontSize = 12.sp,
                        onArtistClick = onArtistClick,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                Spacer(Modifier.height(10.dp))

                val rawProgress = dragFraction
                    ?: if (duration > 0) (position.toFloat() / duration).coerceIn(0f, 1f) else 0f
                // Plain tween, no release bounce — the old scaleY pulse Animatable added a 3-step
                // spring sequence on every seek that ran concurrently with drag recomposition and
                // the animated background, and was the main source of visible jank while scrubbing.
                val progress by animateFloatAsState(
                    targetValue = rawProgress,
                    animationSpec = tween(if (dragFraction != null || reducedMotion) 0 else IrideMotion.Long, easing = IrideMotion.EaseOutQuart),
                    label = "irideProgress",
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(5.dp)
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
                                dragFraction?.let { onSeek(it) }
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

                Spacer(Modifier.height(4.dp))

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

            // This bottom chrome (LYRICS/UP NEXT toggle + click wheel) used to hard-hide the
            // instant the drag reversed (an `if`-removal, no dissolve at all) or, in a later pass,
            // fade out on a fixed short timer disconnected from the actual drag position — both
            // read as an abrupt cut instead of tracking the gesture. It's a plain child of this
            // content now, so it inherits the same scrub-driven alpha the whole card already gets
            // from BottomSheet's own `graphicsLayer { alpha = ((progress - 0.15f) * 4) }` — the
            // exact curve the collapsing miniplayer fades in against, so the two dissolve in lockstep
            // with the finger, not on any separate clock.
            Column(modifier = Modifier.weight(1f).fillMaxWidth()) {
                // Own flow row instead of an overlay pinned to the wheel's TopCenter — the overlay
                // approach only avoided the wheel's circle by relying on incidental slack between the
                // box top and the (size-clamped) circle. On shorter screens that slack shrank to
                // nothing and the labels sat on top of the wheel's radio zone. Giving this row its
                // own real height means the wheel below is sized against what's actually left,
                // guaranteeing no overlap regardless of screen height.
                Row(
                    modifier = Modifier
                        .fillMaxWidth(IrideMp3CoverWidthFraction)
                        .align(Alignment.CenterHorizontally)
                        .padding(horizontal = 6.dp)
                        .padding(top = 8.dp, bottom = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    IridePanelLabel(
                        text = "LYRICS",
                        isActive = isLyricsActive,
                        onClick = onLyricsClick,
                    )
                    IridePanelLabel(
                        text = "UP NEXT",
                        isActive = isQueueActive,
                        onClick = onQueueClick,
                    )
                }

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
                    // Base reference bumped 260 -> 352dp (+35%) per explicit ask: the click wheel
                    // (play/pause/skip/radio) needed to read as clearly bigger, not a marginal bump.
                    // Cover/info/label chrome above was shrunk (IrideMp3CoverWidthFraction 0.90 -> 0.78,
                    // tighter inter-row spacing) to free the vertical budget this needs; the 0.97 clamp
                    // factor still leaves a small margin so the circle doesn't touch the box edges.
                    val wheelSize = minOf(352.dp, maxHeight * 0.97f, maxWidth * 0.97f)
                    val scale = wheelSize / 260.dp
                    val buttonSize = 74.dp * scale
                    val iconSize = 25.dp * scale
                    val skipIconSize = 32.dp * scale
                    // Purely decorative now (center knob lost its click). Trimmed 150 -> 135dp so the
                    // hub reads smaller and leaves more ring band for the 4 zones to sit in.
                    val centerButtonSize = 135.dp * scale

                    IrideClickWheel(
                        isPlaying = isPlaying,
                        isRadioActive = radioActive,
                        isListenTogetherGuest = isListenTogetherGuest,
                        isMuted = isMuted,
                        onPlayPauseClick = onPlayPauseClick,
                        onPreviousClick = onPreviousClick,
                        onNextClick = onNextClick,
                        onRadioClick = onRadioClick,
                        wheelSize = wheelSize,
                        buttonSize = buttonSize,
                        iconSize = iconSize,
                        skipIconSize = skipIconSize,
                        centerButtonSize = centerButtonSize,
                        modifier = Modifier.align(Alignment.Center),
                    )
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
    onDismiss: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val reducedMotion = rememberReducedMotion()
    val progress = remember { Animatable(0f) }
    var closing by remember { mutableStateOf(false) }
    var romanizeEnabled by rememberPreference(LyricsRomanizeToggleKey, false)

    // Routes every close path (back press, close button, tap-to-exit inside Lyrics) through the
    // same shrink-back-down animation before actually tearing the dialog down, so closing mirrors
    // the expand-in instead of just vanishing.
    fun requestClose() {
        if (closing) return
        closing = true
        scope.launch {
            if (reducedMotion) {
                progress.snapTo(0f)
            } else {
                progress.animateTo(0f, tween(IrideMotion.Short, easing = IrideMotion.EaseOutExpo))
            }
            onDismiss()
        }
    }

    LaunchedEffect(Unit) {
        if (reducedMotion) {
            progress.snapTo(1f)
        } else {
            progress.animateTo(1f, tween(IrideMotion.Medium, easing = IrideMotion.EaseOutExpo))
        }
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

        // Plain fade + scale-from-center — the previous version morphed scaleX/scaleY/
        // translateX/translateY from the small in-card rect every frame, computed on top of
        // an already-heavy blurred background animation underneath (see the inline lyrics Box
        // above, now skipped while this dialog is open). That combination is what read as
        // laggy; a flat fade+scale is materially cheaper and still reads as an entrance.
        val p = progress.value
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = lerp(0.94f, 1f, p)
                    scaleY = lerp(0.94f, 1f, p)
                    alpha = p
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
                // Fullscreen-only boost rispetto alla size base (32.4sp -> ~52sp).
                // La copia inline nella card resta a textScale 1.15f.
                textScale = 1.6f,
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
                // Always visible (was gated behind pillController.hasTranslations, so it could
                // never be tapped to actually *request* a translation) — matches the old
                // classic player's translate pill, which is always present and just toggles
                // between "translate" and "revert" depending on hasTranslations.
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(if (pillController.hasTranslations) Color.White.copy(alpha = 0.9f) else Color.White.copy(alpha = 0.14f))
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
                        tint = if (pillController.hasTranslations) Color.Black else Color.White.copy(alpha = 0.9f),
                        modifier = Modifier.size(20.dp),
                    )
                }
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(if (romanizeEnabled) Color.White.copy(alpha = 0.9f) else Color.White.copy(alpha = 0.14f))
                        .border(1.dp, IrideMp3PanelBorderColor, CircleShape)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { romanizeEnabled = !romanizeEnabled },
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = painterResource(R.drawable.alphabet_cyrillic),
                        contentDescription = null,
                        tint = if (romanizeEnabled) Color.Black else Color.White.copy(alpha = 0.9f),
                        modifier = Modifier.size(20.dp),
                    )
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

@Composable
private fun IrideQueuePreview(
    mediaMetadata: MediaMetadata,
    modifier: Modifier = Modifier,
    topClearance: Dp = 0.dp,
    // Bumped by the wheel's Radio button: a one-shot signal to jump straight into Auto-Mix
    // (reset filter, regenerate, scroll down) without disturbing NOW or Continue Listening.
    radioTrigger: Int = 0,
) {
    val playerConnection = LocalPlayerConnection.current ?: return
    val database = LocalDatabase.current
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

    // History: one-shot load of the last 20 distinct songs played before this one — no
    // pagination needed here (unlike the old queue sheet), this panel is short-lived.
    var historyItems by remember { mutableStateOf<List<MediaMetadata>>(emptyList()) }
    LaunchedEffect(mediaMetadata.id) {
        val currentSongId = mediaMetadata.id
        val page = database.events().first()
            .distinctBy { it.song.id }
            .filter { it.song.id != currentSongId }
            .take(20)
        historyItems = page.reversed().map { it.song.toMediaMetadata() }
    }

    // Auto-Mix filter chips — same filters/logic as the old queue sheet (Queue.kt), reused
    // rather than duplicated, including real genre tags from GenreProvider.
    val automix by playerConnection.service.automixItems.collectAsState()
    // Live drag-reorder copy of automix, same pattern as mutableQueueWindows above — lets
    // dragging a song set its priority before Radio commits this order into the real queue.
    val mutableAutomixItems = remember { mutableStateListOf<MediaItem>() }
    LaunchedEffect(automix) {
        mutableAutomixItems.apply {
            clear()
            addAll(automix)
        }
    }
    val familiarArtistNames = remember(historyItems, queueWindows) {
        buildSet {
            historyItems.forEach { md -> md.artists.forEach { add(it.name) } }
            queueWindows.forEach { w -> w.mediaItem.metadata?.artists?.forEach { add(it.name) } }
        }
    }
    val automixGenreSongs = remember(mutableAutomixItems.toList()) {
        mutableAutomixItems.map {
            GenreSongInfo(id = it.mediaId, title = it.metadata?.title.orEmpty(), artist = it.metadata?.artists?.firstOrNull()?.name)
        }
    }
    val automixGenreFilter = rememberGenreFilter(automixGenreSongs)
    val dynamicGenreFilter = automixGenreFilter.sortedGenres.firstOrNull()?.uppercase()
    val automixFilters = remember(dynamicGenreFilter) {
        if (dynamicGenreFilter != null) AUTOMIX_STATIC_FILTERS + dynamicGenreFilter else AUTOMIX_STATIC_FILTERS
    }
    var selectedAutomixFilter by remember { mutableStateOf(AUTOMIX_FILTER_ALL) }
    // Only ALL preserves list order (see filterAutomix) — that's also the only mode dragging
    // is enabled in, so filteredAutomix and mutableAutomixItems always agree on order/positions
    // whenever a drag could be in flight.
    val filteredAutomix = remember(mutableAutomixItems.toList(), selectedAutomixFilter, familiarArtistNames, automixGenreFilter.genreBySongId) {
        filterAutomix(mutableAutomixItems, selectedAutomixFilter, familiarArtistNames, automixGenreFilter.genreBySongId)
    }

    Column(modifier = modifier.padding(top = topClearance)) {
        Text(
            text = "UP NEXT",
            color = Color.White.copy(alpha = 0.6f),
            fontFamily = SpaceMonoFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 10.sp,
            letterSpacing = 1.sp,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
        )

        val lazyListState = rememberLazyListState()

        // Radio wheel button: Player.kt already toggled the stable RDAMVM radio.
        // Here we only scroll the queue panel to the current position so the
        // user sees the new upcoming list.
        // Saveable so it stays in lockstep with the saveable radioNonce in Player.kt — if this
        // reset while the nonce survived, reopening the queue panel would replay a stale radio
        // request and silently re-arm the radio after the user had switched to another queue.
        var lastHandledRadioTrigger by rememberSaveable { mutableStateOf(0) }
        LaunchedEffect(radioTrigger) {
            if (radioTrigger == 0 || radioTrigger == lastHandledRadioTrigger) return@LaunchedEffect
            lastHandledRadioTrigger = radioTrigger
            delay(80)
            lazyListState.animateScrollToItem(historyItems.size)
        }

        if (mutableQueueWindows.isEmpty() && historyItems.isEmpty()) {
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

        var dragInfo by remember { mutableStateOf<Pair<Int, Int>?>(null) }
        var automixDragging by remember { mutableStateOf(false) }
        // Row indices in the LazyColumn are raw/global (history rows + the CONTINUE LISTENING
        // header sit before the queue section, and the Auto-Mix filter/header rows sit before
        // the Auto-Mix section) — the reorderable library hands back indices into the whole
        // column, not into mutableQueueWindows/mutableAutomixItems directly, so both need this
        // offset subtracted before touching either list. Missing this offset was the previous
        // bug: dragging with any history loaded moved the wrong queue item (or crashed).
        val queueOffset = historyItems.size + 1
        val automixOffset = queueOffset + mutableQueueWindows.size + 2
        val reorderableState = rememberReorderableLazyListState(lazyListState = lazyListState) { from, to ->
            val queueFrom = from.index - queueOffset
            val queueTo = to.index - queueOffset
            if (queueFrom in mutableQueueWindows.indices && queueTo in mutableQueueWindows.indices) {
                val currentDragInfo = dragInfo
                dragInfo = if (currentDragInfo == null) queueFrom to queueTo else currentDragInfo.first to queueTo
                mutableQueueWindows.move(queueFrom, queueTo)
                return@rememberReorderableLazyListState
            }
            // Auto-Mix priority reorder — only meaningful (and only enabled, see the drag
            // handle below) while showing the unfiltered ALL order Radio will actually commit.
            if (selectedAutomixFilter == AUTOMIX_FILTER_ALL) {
                val automixFrom = from.index - automixOffset
                val automixTo = to.index - automixOffset
                if (automixFrom in mutableAutomixItems.indices && automixTo in mutableAutomixItems.indices) {
                    automixDragging = true
                    mutableAutomixItems.move(automixFrom, automixTo)
                }
            }
        }

        // History hidden by default — scroll past it once it's loaded, so opening the
        // panel lands on CONTINUE LISTENING, not on the past.
        LaunchedEffect(historyItems) {
            if (historyItems.isNotEmpty()) lazyListState.scrollToItem(historyItems.size)
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
                if (automixDragging) {
                    // mutableAutomixItems already holds the final dragged order — persist it
                    // back to the service so Radio commits this order, not the fetched one.
                    playerConnection.service.automixItems.value = mutableAutomixItems.toList()
                    automixDragging = false
                }
            }
        }

        LazyColumn(
            state = lazyListState,
            modifier = Modifier.weight(1f).padding(start = 10.dp, end = 10.dp, top = 2.dp, bottom = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            items(historyItems, key = { "history_${it.id}" }) { item ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .animateItem()
                        // History → tap the row to play now (inserts next + skips into it,
                        // rest of the future queue stays intact); the icon only adds it ahead.
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                        ) {
                            playerConnection.playNext(item.toMediaItem())
                            playerConnection.player.seekToNextMediaItem()
                        },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    AsyncImage(
                        model = item.thumbnailUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.size(34.dp).clip(irideSquircle(8.dp)),
                    )
                    Spacer(Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = item.title,
                            color = Color.White.copy(alpha = 0.55f),
                            fontFamily = InterFontFamily,
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        if (item.artists.any { it.name.isNotBlank() }) {
                            Text(
                                text = item.artists.joinToString(", ") { it.name },
                                color = Color(0xFFB8B8B8).copy(alpha = 0.7f),
                                fontFamily = InterFontFamily,
                                fontSize = 10.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                    // History → "add to UP NEXT", never touches NOW.
                    Icon(
                        painter = painterResource(R.drawable.playlist_play),
                        contentDescription = null,
                        tint = IrideMp3DimIconColor,
                        modifier = Modifier
                            .size(18.dp)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                            ) { playerConnection.playNext(item.toMediaItem()) },
                    )
                }
            }

            item(key = "continue_listening_header") {
                Text(
                    text = stringResource(R.string.queue_continue_listening).uppercase(),
                    color = Color.White.copy(alpha = 0.6f),
                    fontFamily = SpaceMonoFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 10.sp,
                    letterSpacing = 1.sp,
                    modifier = Modifier.animateItem(),
                )
            }

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
                            ) {
                                playerConnection.player.seekToDefaultPosition(window.firstPeriodIndex)
                            },
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

            if (automix.isNotEmpty()) {
                item(key = "automix_filters") {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .animateItem(),
                    ) {
                        automixFilters.forEach { filter ->
                            UnderlinePill(
                                text = filter,
                                selected = selectedAutomixFilter == filter,
                                onClick = { selectedAutomixFilter = filter },
                            )
                        }
                    }
                }

                item(key = "automix_header") {
                    Text(
                        text = stringResource(R.string.queue_autoplay).uppercase(),
                        color = Color.White.copy(alpha = 0.6f),
                        fontFamily = SpaceMonoFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 10.sp,
                        letterSpacing = 1.sp,
                        modifier = Modifier.animateItem(),
                    )
                }

                itemsIndexed(filteredAutomix, key = { _, item -> "automix_${item.mediaId}" }) { _, item ->
                    ReorderableItem(state = reorderableState, key = "automix_${item.mediaId}") {
                        val metadata = item.metadata
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .animateItem()
                                // Auto-Mix → tap the row to play now (same play-now affordance as
                                // History/Queue rows above): inserts next + skips into it immediately.
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                ) {
                                    val realIndex = automix.indexOfFirst { it.mediaId == item.mediaId }
                                    if (realIndex != -1) {
                                        playerConnection.service.playNextAutomix(item, realIndex)
                                        playerConnection.player.seekToNextMediaItem()
                                    }
                                },
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            AsyncImage(
                                model = metadata?.thumbnailUrl,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.size(34.dp).clip(irideSquircle(8.dp)),
                            )
                            Spacer(Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = metadata?.title.orEmpty(),
                                    color = Color.White.copy(alpha = 0.75f),
                                    fontFamily = InterFontFamily,
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
                            // Drag priority only makes sense (and is only wired up) against the
                            // unfiltered ALL order — the same order Radio commits into the queue.
                            if (selectedAutomixFilter == AUTOMIX_FILTER_ALL) {
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
    }
}

@Composable
private fun IrideClickWheel(
    isPlaying: Boolean,
    isRadioActive: Boolean,
    isListenTogetherGuest: Boolean,
    isMuted: Boolean,
    onPlayPauseClick: () -> Unit,
    onPreviousClick: () -> Unit,
    onNextClick: () -> Unit,
    onRadioClick: () -> Unit,
    wheelSize: Dp,
    buttonSize: Dp,
    iconSize: Dp,
    skipIconSize: Dp,
    centerButtonSize: Dp,
    modifier: Modifier = Modifier,
) {
    // Ring band between the outer rim and the center hole: (wheelSize - centerButtonSize) / 2.
    // The 4 zones sit centered in that band's outer half, so they read as pushed out toward the
    // rim instead of hugging a flat edge margin that ignored how big the hole was.
    val ringWidth = (wheelSize - centerButtonSize) / 2
    val zoneEdgeInset = ringWidth / 6
    // Spread the 4 zones outward from center as a set, like widening the wheel's effective
    // diameter for just the buttons, so they land mid-band in the gray ring instead of hugging it.
    val zoneSpread = buttonSize * 0.14f

    Box(
        modifier = modifier.requiredSize(wheelSize),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .requiredSize(wheelSize)
                .clip(CircleShape)
                .background(IrideMp3WheelSurfaceColor)
                .border(1.dp, IrideMp3PanelBorderColor, CircleShape),
        )

        Box(
            modifier = Modifier
                .size(centerButtonSize)
                .clip(CircleShape)
                .background(IrideMp3BackgroundColor)
                .border(1.dp, IrideMp3PanelBorderColor, CircleShape),
        )

        Box(modifier = Modifier.requiredSize(wheelSize)) {
            WheelZone(alignment = Alignment.TopCenter, size = buttonSize, edgeInset = zoneEdgeInset, offsetY = -zoneSpread, onClick = onRadioClick) {
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
                edgeInset = zoneEdgeInset,
                offsetX = zoneSpread,
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
                edgeInset = zoneEdgeInset,
                offsetY = zoneSpread,
                onClick = onPlayPauseClick,
                // Wide bottom strip: the empty bottom-left/right of the wheel (prev/next sit at
                // mid-height, not down here) becomes part of the play/pause target so a near-miss
                // no longer collapses the player.
                hitWidth = buttonSize * 2.2f,
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
                    modifier = Modifier.size(skipIconSize),
                )
            }
            WheelZone(
                alignment = Alignment.CenterStart,
                size = buttonSize,
                edgeInset = zoneEdgeInset,
                offsetX = -zoneSpread,
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
        modifier = Modifier
            .align(alignment)
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
            modifier = Modifier
                .size(size)
                .pressScale(interactionSource, pressedScale = 0.92f)
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
    val reducedMotion = rememberReducedMotion()
    val color by animateColorAsState(
        targetValue = if (isActive) Color.White else Color.White.copy(alpha = 0.55f),
        animationSpec = tween(if (reducedMotion) 0 else IrideMotion.Quick, easing = IrideMotion.EaseOutQuart),
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
    val eased = if (reducedMotion) {
        if (sheetProgress >= 0.5f) 1f else 0f
    } else {
        sheetProgress.coerceIn(0f, 1f)
    }
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
            // Fades out fast right at the start of the move instead of hanging around for the
            // whole morph — the mini pill's own ring (PillPlayButton) takes back over the instant
            // this one is gone, so it must never be caught visible mid-air with no ring anywhere.
            val ringAlpha = (1f - eased / BridgeRingFadeOutProgress).coerceIn(0f, 1f)
            BridgedElement(start = artStart, end = artEnd, rootOffset = rootOffset, progress = eased) { scale ->
                val onScreenRadius = lerp(PillCoverRadius.value, IrideCoverBorderRadius.value, eased)
                val compensatedRadius = (onScreenRadius / scale.coerceAtLeast(0.01f)).coerceAtMost(200f)
                val context = LocalContext.current
                AsyncImage(
                    // Always requests the source at full quality regardless of this frame's box
                    // size — early in the transition (before the expanded rect is known) this box
                    // briefly measures at the mini pill's small size, and without an explicit
                    // Size.ORIGINAL request Coil decodes/caches a small bitmap for it that then
                    // visibly pops to sharp once the real size lands (the "grainy then upgrades"
                    // regression).
                    model = ImageRequest.Builder(context)
                        .data(metadata.thumbnailUrl)
                        .size(Size.ORIGINAL)
                        .build(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(irideSquircle(compensatedRadius.dp)),
                )
                if (ringAlpha > 0f) {
                    val strokeWidthDp = BridgeRingStrokeWidth / scale.coerceAtLeast(0.01f)
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .graphicsLayer { alpha = ringAlpha }
                            .drawWithContent {
                                drawBridgeProgressRing(
                                    cornerRadiusPx = compensatedRadius.dp.toPx(),
                                    strokeWidthPx = strokeWidthDp.toPx(),
                                    progress = bridgeState.progress,
                                )
                            },
                    )
                }
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
                navController = navController,
                playerBottomSheetState = playerBottomSheetState,
            )
        }
    }
}

private const val IrideCoverTextSplit = 0.28f

// Total horizontal space the bridge's more/favorite actions occupy once fully revealed
// (6.dp gap + two 36.dp targets). Reserved through an animated-width box so the weighted
// title never loses its width in a single frame mid-morph.
private val IrideBridgeInfoActionsWidth = 78.dp

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
    val isFavorite = if (isEpisode) librarySong?.song?.inLibrary != null else librarySong?.song?.liked == true

    val menuState = LocalMenuState.current
    val bottomSheetPageState = LocalBottomSheetPageState.current
    val onMoreClick = {
        menuState.show {
            PlayerMenu(
                mediaMetadata = metadata,
                navController = navController,
                playerBottomSheetState = playerBottomSheetState,
                onShowDetailsDialog = {
                    metadata.id.let { id ->
                        bottomSheetPageState.show { ShowMediaInfo(id) }
                    }
                },
                onDismiss = menuState::dismiss,
            )
        }
    }

    Column(
        modifier = Modifier
            .offset { IntOffset(left.roundToInt(), top.roundToInt()) }
            .width(with(density) { width.toDp() }),
    ) {
        val iconReveal = if (playerConnection != null) {
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
                    modifier = Modifier
                        .weight(1f)
                        .graphicsLayer {
                            val scale = lerp(14f / 16f, 1f, progress)
                            scaleX = scale
                            scaleY = scale
                            transformOrigin = TransformOrigin(0f, 0f)
                            translationY =
                                IrideBridgeInfoLineConvergence.toPx() / 2f * (1f - progress)
                        },
                )
                // Width-only reservation for the end actions. A Spacer is measured but never
                // inflates the row's height — actually composing the 36.dp icon boxes in this
                // row made it taller than the title line, which then got vertically centered in
                // the extra empty space and visually pushed away from the artist line below.
                Spacer(Modifier.width(IrideBridgeInfoActionsWidth * iconReveal))
            }
            if (playerConnection != null) {
                // Overlay so the fixed-size click targets never influence the row height; they
                // fade in centered on the title line, exactly where the player's static actions sit.
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .graphicsLayer { alpha = iconReveal },
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .padding(start = 6.dp)
                            .size(36.dp)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                enabled = iconReveal > 0f,
                            ) { onMoreClick() },
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.more_vert),
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.7f),
                            modifier = Modifier.size(20.dp),
                        )
                    }
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(36.dp)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                enabled = iconReveal > 0f,
                            ) { playerConnection.service.toggleLike() },
                    ) {
                        Icon(
                            painter = painterResource(if (isFavorite) R.drawable.favorite else R.drawable.favorite_border),
                            contentDescription = null,
                            tint = if (isFavorite) Color.White else Color.White.copy(alpha = 0.7f),
                            modifier = Modifier.size(20.dp),
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
                modifier = Modifier.graphicsLayer {
                    translationY =
                        -IrideBridgeInfoLineConvergence.toPx() / 2f * (1f - progress)
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
