/**
 * Iride Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.toSize
import androidx.compose.ui.util.lerp
import androidx.compose.ui.zIndex
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
import com.metrolist.music.ui.component.BetterAnimatedGradientBackground
import com.metrolist.music.ui.component.BottomSheetState
import com.metrolist.music.ui.component.GenreFilterState
import com.metrolist.music.ui.component.GenrePillsRow
import com.metrolist.music.ui.component.GenreSongInfo
import com.metrolist.music.ui.component.LocalBottomSheetPageState
import com.metrolist.music.ui.component.LocalMenuState
import com.metrolist.music.ui.component.Lyrics
import com.metrolist.music.ui.component.UnderlinePill
import com.metrolist.music.ui.component.rememberGenreFilter
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
// Lightened from near-black (0A0A0C/000000) — under direct sunlight or a bright room the wheel
// used to read as one flat black disc with no visible dial. These keep the player's background
// very close to black (per design intent) while giving the wheel itself, its border, and low-emphasis
// icons/text a contrast floor that survives a bright screen.
private val IrideMp3WheelTopColor = Color(0xFF232327)
private val IrideMp3WheelBottomColor = Color(0xFF0D0D0F)
private val IrideMp3PanelBorderColor = Color.White.copy(alpha = 0.14f)
private val IrideMp3DimIconColor = Color.White.copy(alpha = 0.55f)

// Cover width as a fraction of the player's width — title/artist/progress below share this same
// width so they line up with the cover's edges instead of using their own independent margin.
private const val IrideMp3CoverWidthFraction = 0.82f

// Squircle corner treatment shared by the cover/schermino everywhere it appears, matching the
// squircle used across the rest of the app (grid items, mini player, etc.) when New Iride UI is off.
private fun irideSquircle(radius: Dp) = SquircleShape(radius = radius, cornerSmoothing = 0.48f)

/**
 * Shared geometry bridge between the New Iride UI's collapsed miniplayer (curtain peek strip)
 * and the expanded [IrideMp3PlayerContent]. Both sides report the on-screen (window-space) rect
 * of their cover art and title/artist block here — each side hides its own copy
 * ([irideReportRect] callers pair a non-null lambda with `alpha(0f)`) and
 * [IrideMiniPlayerBridgeOverlay] draws a single moving instance of each that interpolates between
 * the two reported rects as the bottom sheet drags/animates. The title crossfades between its
 * mini (Inter) and expanded (Monospace) fonts as it moves; the progress indicator and the
 * play/skip/favorite buttons are left as real duplicates in each layout and just cross-fade.
 */
@Stable
class IrideBridgeState {
    var miniArt by mutableStateOf<Rect?>(null)
    var playerArt by mutableStateOf<Rect?>(null)
    var miniInfo by mutableStateOf<Rect?>(null)
    var playerInfo by mutableStateOf<Rect?>(null)

    // True while the expanded player's "schermino" is showing a lyrics/queue preview instead of
    // the cover — the bridge overlay must not draw its own moving cover copy on top of that
    // preview, since there's nothing on the mini side to morph it from/to in that state.
    var panelActive by mutableStateOf(false)
}

internal fun Modifier.irideReportRect(target: (Rect) -> Unit): Modifier =
    this.onGloballyPositioned { target(Rect(it.positionInWindow(), it.size.toSize())) }

// Deliberately no easing curve here: the cover must track the drag 1:1 with the manual sheet
// progress. An eased curve (previously a slow-fast-slow CubicBezier) looked smooth on a
// programmatic expand/collapse animation, but under a real drag it desynced from the finger — the
// cover raced ahead of the touch point around the midpoint, then sat still waiting near the end.

/**
 * The "New Iride UI" expanded player: an old-school MP3-player-styled layout with a square cover,
 * typewriter title/artist (favorite toggle at the end of the title line), a thick progress bar,
 * and a control pad of same-size round buttons (radio / prev / play / next / more) sitting low,
 * taking up most of the bottom of the screen. LYRICS and QUEUE are plain text toggles flanking the
 * pad at its top edge; activating either swaps the cover ("schermino") for a live preview.
 */
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
    // Fraction (0f-1f) of the tapped position along the progress bar's width.
    onSeek: (Float) -> Unit = {},
    onArtistClick: (String) -> Unit = {},
    // LYRICS/QUEUE are driven by the same state the classic player uses (showInlineLyrics /
    // showQueue) so activating one here stays in sync with the rest of the player.
    isLyricsActive: Boolean = false,
    isQueueActive: Boolean = false,
    onLyricsClick: () -> Unit = {},
    onQueueClick: () -> Unit = {},
    // Needed to open the real PlayerMenu (kebab / "..." wheel button) — same menu the classic
    // player's more button opens.
    navController: NavController,
    playerBottomSheetState: BottomSheetState,
    modifier: Modifier = Modifier,
    bottomInset: Dp = 0.dp,
    // Extra height this composable's own background reaches above its normal top edge (the caller
    // shifts the whole content up by this much via BottomSheet's contentTopPadding) — covers the
    // strip the app layer's rounded corner cuts into when fully expanded, which otherwise has
    // nothing curtain-colored behind it. The inner column and the top-start corner button are
    // padded back down by the same amount so the visible layout doesn't shift.
    cornerRevealHeight: Dp = 0.dp,
    // New Iride UI bridge: when set, this player's own cover/title-artist/top-bar are hidden
    // (alpha 0, still laid out so their rects can be reported) and drawn instead by
    // IrideMiniPlayerBridgeOverlay, which morphs them in from the collapsed miniplayer instead of
    // cross-fading a duplicate copy in.
    bridgeState: IrideBridgeState? = null,
) {
    val menuState = LocalMenuState.current
    val bottomSheetPageState = LocalBottomSheetPageState.current
    var radioActive by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    // Fraction (0f-1f) of the progress bar currently being dragged, or null when the user's
    // finger isn't on it. Declared here (rather than next to the bar itself, further down) so the
    // lyrics panel above can also read it as a live seek preview.
    var dragFraction by remember { mutableStateOf<Float?>(null) }

    SideEffect {
        bridgeState?.panelActive = isLyricsActive || isQueueActive
    }

    // Own the full bottom-sheet area — the old player's background must never peek through.
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

            // The "schermino" — cover art by default, swapped for a live lyrics/queue preview
            // when one of those panels is active. A fullscreen affordance always sits in its
            // top-right corner (not wired up yet).
            Box(
                modifier = Modifier
                    .fillMaxWidth(IrideMp3CoverWidthFraction)
                    .padding(top = 8.dp, bottom = 8.dp)
                    .aspectRatio(1f)
                    .align(Alignment.CenterHorizontally)
                    .clip(irideSquircle(16.dp))
                    // The panel's outline must read at every state (cover/lyrics/queue), not just
                    // rely on the background color contrast — otherwise its edge disappears
                    // against a dark surrounding UI.
                    .border(1.dp, IrideMp3PanelBorderColor, irideSquircle(16.dp)),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .zIndex(if (isLyricsActive || isQueueActive) 0f else 1f)
                        // Lyrics/queue stay mounted (and interactive) underneath at alpha 0 when
                        // inactive — see the comment below — so the cover needs its own
                        // touch-consuming no-op here, otherwise a tap on the resting cover would
                        // fall through to the hidden panel's own click/drag handlers.
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

                // LYRICS/QUEUE previews stay composed at all times (never removed from the tree by
                // an `if`) and only cross-fade via alpha/zIndex — an `if (isLyricsActive) Lyrics(...)`
                // here used to tear down and rebuild Lyrics' hiltViewModel() on every single toggle,
                // which reloaded the lyrics from scratch and reset its scroll/click gesture state
                // each time. Keeping it mounted in the background keeps it synced and interactive
                // the instant the panel becomes visible, same as the classic player's
                // InlineLyricsView crossfade.
                val lyricsAlpha by animateFloatAsState(
                    targetValue = if (isLyricsActive) 1f else 0f,
                    animationSpec = tween(280, easing = FastOutSlowInEasing),
                    label = "irideLyricsAlpha",
                )
                // `position` is a plain composable parameter, not a State — a lambda that closes
                // over it directly would freeze at whatever value it held when Lyrics' own internal
                // LaunchedEffect(lyricsText, lines) last (re)started, since that effect doesn't
                // restart just because this lambda instance changes. Routing it through
                // rememberUpdatedState (same pattern the classic player uses for its own
                // sliderPositionProvider, see Player.kt) keeps a single stable lambda whose captured
                // state always reads the latest value, so the highlighted line keeps advancing
                // instead of getting stuck on the first line it lands on. Reusing `dragFraction`
                // (already tracked for the progress bar below) also makes the lyrics preview-scroll
                // live while the user is scrubbing, instead of only jumping once the drag ends.
                val currentDragFraction by rememberUpdatedState(dragFraction)
                val currentDuration by rememberUpdatedState(duration)
                val lyricsSliderPositionProvider = remember {
                    { currentDragFraction?.let { (currentDuration * it).toLong() } }
                }
                // Same 4-sprite animated gradient the classic player draws behind its own lyrics
                // (Player.kt, PlayerBackgroundStyle.BETTER_ANIMATED_GRADIENT) — this panel used to
                // sit on flat black instead, losing that treatment just because it's inside the
                // New Iride UI schermino. No crossfade here (unlike the classic player's version):
                // this panel is small and the bitmap swap on track change is barely noticeable at
                // this size, so the extra Animatable/incoming-bitmap machinery isn't worth it.
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
                        // Reserves room at the top for the fullscreen button that sits over this
                        // panel so its first line/row never lands underneath it. Left padding gives
                        // the lyrics text breathing room from the panel's own edge/border instead of
                        // starting flush against it.
                        modifier = Modifier.fillMaxSize().padding(top = 34.dp, start = 12.dp),
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

                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .zIndex(2f)
                        .padding(8.dp)
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.45f))
                        .border(1.dp, Color.White.copy(alpha = 0.2f), CircleShape)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            // Full-screen mode is not implemented yet.
                            onClick = {},
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = painterResource(R.drawable.fullscreen),
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.9f),
                        modifier = Modifier.size(14.dp),
                    )
                }
            }

            // Same width as the cover above, so title/artist/progress line up with its edges
            // instead of using their own independent side margin.
            Column(
                modifier = Modifier
                    .fillMaxWidth(IrideMp3CoverWidthFraction)
                    .align(Alignment.CenterHorizontally)
                    // Nudges title/artist in from the cover's own edges, toward screen center.
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
                    // Title reads NAME______(star): the favorite toggle sits at the far end of the
                    // title's own line (not the old dedicated bottom-row button) so it's associated
                    // with "this track", not with a remote-control action.
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
                            // Small ergonomic detail: a quick spring pop on toggle instead of an
                            // instant swap — subtle enough not to draw attention on its own.
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
                // Animated rather than snapped straight to the new value during normal playback —
                // the target only ticks forward a little each frame so this tracks it closely, and
                // it also smooths out the jump a tap-to-seek causes. While the user's finger is on
                // the bar this is skipped (duration 0) so the fill tracks the drag 1:1 instead of
                // lagging a beat behind it, like a native iOS-style scrubber.
                val progress by animateFloatAsState(
                    targetValue = rawProgress,
                    animationSpec = tween(if (dragFraction != null) 0 else 220, easing = FastOutSlowInEasing),
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
                        .pointerInput(duration) {
                            if (duration <= 0) return@pointerInput
                            // Starts the scrub the instant the finger touches down (like tapping
                            // anywhere on an iOS scrubber jumps the thumb there), follows it for as
                            // long as it stays down, and only commits the seek — and lets playback
                            // continue from there — once it lifts.
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
                                        seekPulse.animateTo(1.6f, tween(90, easing = FastOutSlowInEasing))
                                        seekPulse.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium))
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

            // Control pad: centered in the remaining space between the time row above and the
            // bottom of the screen below (was previously bottom-anchored, sitting flush against
            // the bottom edge with no breathing room under it).
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                val wheelSize = 260.dp
                val buttonSize = 74.dp
                val iconSize = 25.dp
                val skipIconSize = 32.dp
                // Bigger than the four side zones and its own zone — the play/pause hole was
                // previously the same size as radio/next/more/prev, easy to miss and visually
                // indistinguishable in proportion from a plain side button.
                val centerButtonSize = 96.dp
                val centerIconSize = 34.dp

                IrideClickWheel(
                    isPlaying = isPlaying,
                    isRadioActive = radioActive,
                    isMoreActive = menuState.isVisible,
                    onPlayPauseClick = onPlayPauseClick,
                    onPreviousClick = onPreviousClick,
                    onNextClick = onNextClick,
                    onRadioClick = {
                        radioActive = !radioActive
                        onRadioClick()
                    },
                    onMoreClick = {
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
                    },
                    wheelSize = wheelSize,
                    buttonSize = buttonSize,
                    iconSize = iconSize,
                    skipIconSize = skipIconSize,
                    centerButtonSize = centerButtonSize,
                    centerIconSize = centerIconSize,
                    modifier = Modifier.align(Alignment.Center),
                )
                // Same width fraction + centering as the cover/schermino above, so LYRICS starts
                // exactly on the cover's left edge and QUEUE ends exactly on its right edge instead
                // of using their own independent fixed padding from the full-width wheel Box.
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
            Spacer(Modifier.height(bottomInset))
        }
    }
}

/**
 * Artist line for the New Iride UI: same look as a plain Text, but each artist name is tappable
 * and navigates to that artist's screen — mirrors the tap-position-to-annotation approach the
 * classic player already uses for its own artist line.
 */
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

    Text(
        text = annotated,
        color = color,
        fontFamily = fontFamily,
        fontSize = fontSize,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
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
                interactionSource = remember { MutableInteractionSource() },
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

/**
 * Compact "up next" list drawn inside the schermino when QUEUE is active — a scannable preview
 * with cover art, not the full queue sheet's drag-scroll-select machinery, but reorderable and
 * tap-to-seek like the real thing (same sh.calvin.reorderable mechanics as Queue.kt's own
 * queue list — long-press the drag handle to reorder, tap a row to jump to it).
 *
 * A small "QUEUE MODE" pill panel sits above the list (see [IrideQueueModePanel]) — KEEP QUEUE /
 * RADIO / ARTIST, each RADIO/ARTIST opening a second row of sub-modes. Only the *upcoming* portion
 * of the queue (after the currently playing item) is ever touched — the current/past items are
 * left alone so switching modes never yanks playback mid-track.
 */
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

    // Reorders only queueWindows[currentWindowIndex+1 ..] into desiredUpcomingOrder (a permutation
    // of those same original indices) — never touches the currently playing item or anything
    // before it. Two paths mirror the manual drag-reorder below: shuffle mode has no bulk "set
    // order" API, so it's rebuilt in one setShuffleOrder call; plain mode has no bulk API either,
    // so it's driven into place via a selection-sort of moveMediaItem calls instead.
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
        // Every sub-mode is still this song's radio — CLOSE/DISCOVER only reshuffle what it hands
        // back, they don't replace it with an unrelated pool (that's the whole point: radio always
        // stays radio *of this track*, the sub-modes are just how far the upcoming picks drift).
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
                    // totalPlayTime == 0 (never played locally) sorts first — the only "have I
                    // heard this" signal actually available without a network round trip per track.
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
        val artistId = mediaMetadata.artists.firstOrNull { it.id != null }?.id ?: return
        coroutineScope.launch {
            // Library-only: there's no cheap way to pull this artist's *full* YTM catalog here, so
            // TOP/UNHEARD/DEEP CUTS all work off songs Iride already knows locally (downloaded,
            // liked, or previously queued/played). Fine for TOP/DEEP CUTS; UNHEARD undercounts
            // (misses tracks never added at all) but still surfaces real never-played library songs.
            val songs = database.artistSongsByPlayTimeAsc(artistId).first()
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

    // Genre pills here only dim non-matching rows, never remove them — hard-filtering would shift
    // this list's item positions out of sync with the player timeline indices that drag-reorder
    // and tap-to-seek below both rely on.
    val genreFilter = rememberGenreFilter(
        songs = remember(queueWindows) {
            queueWindows.mapNotNull { window ->
                val meta = window.mediaItem.metadata ?: return@mapNotNull null
                GenreSongInfo(id = meta.id, title = meta.title, artist = meta.artists.firstOrNull()?.name)
            }
        },
    )

    Column(modifier = modifier.padding(top = topClearance)) {
        IrideQueueModePanel(
            queueLevel = queueLevel,
            activeRadioSubMode = activeRadioSubMode,
            activeArtistSubMode = activeArtistSubMode,
            genreFilter = genreFilter,
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
                    val dimmed = queueLevel == IrideQueueLevel.RADIO &&
                        genreFilter.selectedGenre != null &&
                        metadata != null &&
                        !genreFilter.matches(metadata.id)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .animateItem()
                            .alpha(if (dimmed) 0.35f else 1f)
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

/**
 * "QUEUE MODE" pill panel — KEEP QUEUE always resets back to the plain manual queue; RADIO/ARTIST
 * each expand a second pill row of sub-modes. Same underline pill look as [GenrePillsRow]'s New
 * Iride UI variant (via the shared [UnderlinePill]), so this reads as the same filter-row language
 * used elsewhere (playlist genre pills) instead of inventing a new visual.
 */
@Composable
private fun IrideQueueModePanel(
    queueLevel: IrideQueueLevel,
    activeRadioSubMode: IrideRadioSubMode?,
    activeArtistSubMode: IrideArtistSubMode?,
    genreFilter: GenreFilterState,
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
            if (activeRadioSubMode != null) {
                GenrePillsRow(state = genreFilter, modifier = Modifier.padding(top = 2.dp))
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

/**
 * The New Iride UI's iPod-style click wheel — dial ring background with radio/next/more/prev
 * zones at N/E/S/W and a play/pause hole in the center. All zone sizes are driven by [buttonSize]
 * (computed from the wheel's own size by the caller) so the whole pad scales with the available
 * screen width instead of being pinned to a small fixed diameter.
 */
@Composable
private fun IrideClickWheel(
    isPlaying: Boolean,
    isRadioActive: Boolean,
    isMoreActive: Boolean,
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
        // requiredSize ignores incoming parent constraints, so the wheel is always a perfect
        // circle — plain size() would shrink into an ellipse if the parent's width got squeezed.
        modifier = modifier.requiredSize(wheelSize),
        contentAlignment = Alignment.Center,
    ) {
        // Outer ring — dark dial, rim-lit from above like light falling on it from overhead.
        Box(
            modifier = Modifier
                .requiredSize(wheelSize)
                .clip(CircleShape)
                .background(Brush.verticalGradient(listOf(IrideMp3WheelTopColor, IrideMp3WheelBottomColor)))
                .border(
                    width = 1.dp,
                    brush = Brush.verticalGradient(
                        listOf(
                            Color.White.copy(alpha = 0.22f),
                            Color.White.copy(alpha = 0.05f),
                            Color.White.copy(alpha = 0f),
                        ),
                    ),
                    shape = CircleShape,
                ),
        ) {
            WheelZone(alignment = Alignment.TopCenter, size = buttonSize, onClick = onRadioClick) {
                Icon(
                    painter = painterResource(R.drawable.radio),
                    contentDescription = null,
                    tint = if (isRadioActive) Color.White else IrideMp3DimIconColor,
                    modifier = Modifier.size(iconSize),
                )
            }
            WheelZone(alignment = Alignment.CenterEnd, size = buttonSize, onClick = onNextClick) {
                Icon(
                    painter = painterResource(R.drawable.ic_iride_skip_next),
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(skipIconSize),
                )
            }
            WheelZone(alignment = Alignment.BottomCenter, size = buttonSize, onClick = onMoreClick) {
                Icon(
                    painter = painterResource(R.drawable.more_vert),
                    contentDescription = null,
                    tint = if (isMoreActive) Color.White else IrideMp3DimIconColor,
                    modifier = Modifier.size(iconSize),
                )
            }
            WheelZone(alignment = Alignment.CenterStart, size = buttonSize, onClick = onPreviousClick) {
                Icon(
                    painter = painterResource(R.drawable.ic_iride_skip_previous),
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(skipIconSize),
                )
            }
        }

        // Center button — play/pause, the one control most likely to be pressed, so it's sized up
        // from the four side zones instead of matching them 1:1. Same wheel gradient + rim-light
        // border as the outer ring (not a flat black hole) so it reads as part of the same disc.
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
                .background(Brush.verticalGradient(listOf(IrideMp3WheelTopColor, IrideMp3WheelBottomColor)))
                .border(
                    width = 1.dp,
                    brush = Brush.verticalGradient(
                        listOf(
                            Color.White.copy(alpha = 0.22f),
                            Color.White.copy(alpha = 0.05f),
                            Color.White.copy(alpha = 0f),
                        ),
                    ),
                    shape = CircleShape,
                )
                .clickable(
                    interactionSource = playPauseInteraction,
                    indication = null,
                    onClick = onPlayPauseClick,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(if (isPlaying) R.drawable.ic_iride_pause else R.drawable.ic_iride_play),
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(centerIconSize),
            )
        }
    }
}

@Composable
private fun BoxScope.WheelZone(
    alignment: Alignment,
    size: Dp,
    onClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    // Ergonomic detail: a small press-in on tap, low enough not to draw the eye but enough to
    // register as physical feedback on a zone that has no other visual press state of its own.
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.92f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessHigh),
        label = "irideWheelZoneScale",
    )
    Box(
        modifier = Modifier
            .align(alignment)
            .padding(12.dp)
            .size(size)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(CircleShape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
        content = { content() },
    )
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

/**
 * Draws the cover art for the New Iride UI as a single moving instance, positioned every frame by
 * interpolating between the mini/player rects [IrideBridgeState] last reported. This is what makes
 * the cover *move* between the two layouts instead of cross-fading a duplicate copy in/out. Title,
 * artist, the progress indicator, and the play/skip/favorite buttons are all left as real
 * duplicates in each layout and keep cross-fading — only the cover gets the morph treatment. Skips
 * drawing entirely while the expanded side is showing a lyrics/queue preview instead of the cover.
 */
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

    // Text takes the second half of the drag (IrideCoverTextSplit -> 1), staying put at its mini
    // position until the cover has already landed. Linear (plain lerp, no easing). The cover itself
    // keeps the full 0 -> 1 range (see BridgedElement below) — only the text is held back, so it
    // never crosses paths with the cover mid-drag.
    val textProgress = ((eased - IrideCoverTextSplit) / (1f - IrideCoverTextSplit)).coerceIn(0f, 1f)

    Box(
        modifier = modifier
            .fillMaxSize()
            .onGloballyPositioned { rootOffset = it.positionInWindow() },
    ) {
        // Falls back to whichever side is known when the other hasn't been measured yet (e.g. cold
        // start at rest, before the expanded content has ever composed) — draws statically at the
        // one known rect instead of not drawing at all, which would otherwise blank out the art.
        val artStart = bridgeState.miniArt ?: bridgeState.playerArt
        val artEnd = bridgeState.playerArt ?: artStart
        if (artStart != null && artEnd != null && !bridgeState.panelActive) {
            BridgedElement(start = artStart, end = artEnd, rootOffset = rootOffset, progress = eased) { scale ->
                // The clip below is applied *inside* the graphicsLayer-scaled box, so a plain
                // constant dp radius would get crushed down by the same factor as the box itself —
                // at the mini end (scale ~0.15) a 14dp radius rendered at ~2dp on screen, reading
                // as a barely-rounded rectangle instead of a squircle. Dividing by the current
                // frame's scale keeps the on-screen radius correct for every frame of the morph,
                // not just the fully-expanded end.
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

// Fraction of the drag before the text starts moving — see the comment where this is used.
// Was 0.5f (title/artist held stationary until the drag was half done, reading as a very late,
// abrupt jump into place); lowered so it starts moving noticeably earlier while still trailing
// the cover enough to avoid crossing paths with it.
private const val IrideCoverTextSplit = 0.28f

/**
 * Moves the title/artist block between the collapsed miniplayer and expanded player positions —
 * position/width only (no graphicsLayer scale, which would blur text). The title is drawn as two
 * overlaid copies (mini's Inter, expanded's Monospace) cross-fading via alpha as they travel, since
 * a font family change can't be interpolated directly; the artist line keeps one Inter copy and
 * just lerps its color/size, since its font never changes.
 */
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
        // No crossfade: mini (Inter) font holds for the whole move, swapped for the expanded
        // (Monospace) font only on the last frame (progress == 1) — a blended crossfade between two
        // different font families reads as a garbled double-exposure mid-transition.
        if (progress < 1f) {
            Text(
                text = metadata.title,
                color = miniTitleColor,
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.Medium,
                fontSize = lerpTextUnit(14.sp, 16.sp, progress),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        } else {
            Text(
                text = metadata.title,
                color = Color.White,
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
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

/**
 * Lays out [content] pinned to [end]'s position/size (its natural, expanded-player size), then
 * scales+translates it toward [start] via graphicsLayer as [progress] goes 1 -> 0. Doing the morph
 * with graphicsLayer instead of re-measuring at a lerped size every frame keeps it to a draw-phase
 * transform (cheap, 120Hz-friendly) rather than a layout pass.
 */
@Composable
private fun BridgedElement(
    start: Rect,
    end: Rect,
    rootOffset: Offset,
    progress: Float,
    // Exposes the frame's current scaleX to [content] so it can counter-scale things that must
    // not shrink along with the box (e.g. a clip shape's corner radius — see the squircle comment
    // at the call site) instead of only being usable for the graphicsLayer transform below.
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
