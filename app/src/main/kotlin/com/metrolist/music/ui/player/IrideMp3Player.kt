
package com.metrolist.music.ui.player

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
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
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.drag
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
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
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
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
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
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
import com.metrolist.music.LocalDatabase
import com.metrolist.music.LocalPlayerConnection
import com.metrolist.music.R
import com.metrolist.music.constants.IrideBaseBorderWidth
import com.metrolist.music.constants.LyricsRomanizeToggleKey
import com.metrolist.music.lyrics.LyricsTranslationHelper
import com.metrolist.music.utils.rememberPreference
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
import com.metrolist.music.viewmodels.LyricsViewModel
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
private val IrideMp3DimIconColor = Color.White.copy(alpha = 0.75f)

private val IrideArtistTextColor = Color(0xFFB8B8B8)

private val IrideMp3WheelSurfaceColor = IrideMp3SurfaceColor

private const val IrideMp3CoverWidthFraction = 0.84f

private val IrideCoverBorderRadius = 12.dp

private val WheelBottomGestureClearance = 24.dp

/** Vertical branch line color for songs belonging to the (virtual) radio section. */
private val IrideRadioBranchColor = Color.White.copy(alpha = 0.18f)

private fun irideSquircle(radius: Dp) = SquircleShape(radius = radius, cornerSmoothing = 0.48f)

/** A single draggable row of the inline MP3-player queue preview, tagged by section. */
private sealed class IrideQueueSlot(val key: Any) {
    class History(val metadata: MediaMetadata) : IrideQueueSlot("history_${metadata.id}")
    class QueueEntry(val window: Timeline.Window) : IrideQueueSlot(window.uid.hashCode())
    class Automix(val item: MediaItem) : IrideQueueSlot("automix_${item.mediaId}")
}

@Stable
class IrideBridgeState {
    var miniArt by mutableStateOf<Rect?>(null)
    var playerArt by mutableStateOf<Rect?>(null)
    var miniInfo by mutableStateOf<Rect?>(null)
    var playerInfo by mutableStateOf<Rect?>(null)

    var progress by mutableStateOf(0f)

    var panelActive by mutableStateOf(false)
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
    radioTrigger: Int = 0,
    queueOpenNonce: Int = 0,
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
    val radioActive by LocalPlayerConnection.current?.service?.isAutoMixQueueActive?.collectAsState() ?: remember { mutableStateOf(false) }

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

    // The thin frame belongs to the lyrics/queue panels that replace the artwork. While the
    // album art itself is on display the frame is never drawn â€” not settled, not mid-drag,
    // never â€” so nothing can ever peek out behind the photo.
    val coverBorderAlpha by animateFloatAsState(
        targetValue = if (isLyricsActive || isQueueActive) 1f else 0f,
        animationSpec = tween(if (reducedMotion) 0 else 150),
        label = "irideCoverBorderAlpha",
    )

    val panelActive = isLyricsActive || isQueueActive
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
                    .fillMaxWidth(IrideMp3CoverWidthFraction)
                    .padding(top = 8.dp, bottom = 8.dp)
                    .aspectRatio(1f)
                    .align(Alignment.CenterHorizontally)
                    .clip(irideSquircle(IrideCoverBorderRadius))
                    .border(
                        IrideBaseBorderWidth,
                        Color.White.copy(alpha = 0.22f * coverBorderAlpha),
                        irideSquircle(IrideCoverBorderRadius),
                    ),
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
                    if (!fullScreenActive && (isLyricsActive || lyricsAlpha > 0f)) {
                        BetterAnimatedGradientBackground(
                            thumbnail = lyricsBgBitmap,
                            modifier = Modifier.fillMaxSize(),
                        )
                        Lyrics(
                            sliderPositionProvider = lyricsSliderPositionProvider,
                            showLyrics = true,
                            showPills = false,
                            isFullScreen = false,
                            pillsController = lyricsPillController,
                            textScale = 1.15f,
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
                        openNonce = queueOpenNonce,
                    )
                }

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

            val sheetProgress = playerBottomSheetState.progress
            val textEased = if (reducedMotion) {
                if (sheetProgress >= 0.5f) 1f else 0f
            } else {
                sheetProgress.coerceIn(0f, 1f)
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth(IrideMp3CoverWidthFraction)
                    .align(Alignment.CenterHorizontally)
                    .padding(horizontal = 6.dp),
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
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
                            fontSize = 18.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .weight(1f)
                                .graphicsLayer { alpha = textEased }
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
                        fontSize = 13.sp,
                        onArtistClick = onArtistClick,
                        modifier = Modifier
                            .fillMaxWidth()
                            .graphicsLayer { alpha = textEased },
                    )
                }

                Spacer(Modifier.height(10.dp))

                // Seek model: while dragging, dragFraction rules; after release the bar
                // HOLDS the committed value until the reported position catches up (or a
                // safety timeout passes), so it never snaps back to the old spot.
                var committedSeek by remember { mutableStateOf<Float?>(null) }
                LaunchedEffect(committedSeek) {
                    if (committedSeek != null) {
                        delay(1200)
                        committedSeek = null
                    }
                }
                LaunchedEffect(position, duration) {
                    val target = committedSeek ?: return@LaunchedEffect
                    if (duration > 0 &&
                        kotlin.math.abs(position.toFloat() / duration - target) < 0.01f
                    ) {
                        committedSeek = null
                    }
                }
                val shownFraction = dragFraction
                    ?: committedSeek
                    ?: if (duration > 0) (position.toFloat() / duration).coerceIn(0f, 1f) else 0f
                val interacting = dragFraction != null || committedSeek != null
                val progress by animateFloatAsState(
                    targetValue = shownFraction,
                    animationSpec = tween(if (interacting || reducedMotion) 0 else IrideMotion.Long, easing = IrideMotion.EaseOutQuart),
                    label = "irideProgress",
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 24.dp),
                    contentAlignment = Alignment.CenterStart,
                ) {
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
                                    dragFraction?.let { target ->
                                        committedSeek = target
                                        onSeek(target)
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
                }

                Spacer(Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = if (interacting && duration > 0) {
                            makeTimeString((shownFraction * duration).toLong())
                        } else {
                            makeTimeString(position)
                        },
                        color = Color.White.copy(alpha = if (interacting) 1f else 0.65f),
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

            Column(modifier = Modifier.weight(1f).fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth(IrideMp3CoverWidthFraction)
                        .align(Alignment.CenterHorizontally)
                        .padding(horizontal = 6.dp)
                        .padding(top = 8.dp, bottom = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    IridePanelLabel(
                        text = stringResource(R.string.mp3_panel_lyrics),
                        isActive = isLyricsActive,
                        onClick = onLyricsClick,
                    )
                    IridePanelLabel(
                        text = stringResource(R.string.mp3_panel_up_next),
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
                    val wheelSize = minOf(352.dp, maxHeight * 0.97f, maxWidth * 0.97f)
                    val scale = wheelSize / 260.dp
                    val buttonSize = 74.dp * scale
                    val iconSize = 25.dp * scale
                    val skipIconSize = 32.dp * scale
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
                Spacer(Modifier.height(bottomInset + WheelBottomGestureClearance))
            }
        }
    }
}

/**
 * True fullscreen for lyrics: a separate, edge-to-edge Android [Dialog] window instead of the
 * player card merely growing within its own bounds. A Dialog gets its own Window, so it is
 * guaranteed to draw above everything else in the activity (status bar, nav bar, any other
 * composable) â€” the previous in-card "expand" approach still shared the activity's single window
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
    val database = LocalDatabase.current
    val playerConnection = LocalPlayerConnection.current ?: return
    val currentLyricsEntity by playerConnection.currentLyrics.collectAsState(initial = null)
    val translationsActive by LyricsTranslationHelper.hasActiveTranslations.collectAsState()
    var romanizeEnabled by rememberPreference(LyricsRomanizeToggleKey, false)
    val lyricsViewModel: LyricsViewModel = hiltViewModel()
    val lyricLines by lyricsViewModel.lines.collectAsState()
    var hasRomanizedText by remember(lyricLines) { mutableStateOf(false) }
    LaunchedEffect(lyricLines) {
        lyricLines.forEach { entry ->
            launch {
                entry.romanizedTextFlow.collect { value ->
                    if (value != null) hasRomanizedText = true
                }
            }
        }
    }

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
                modifier = Modifier.fillMaxSize().padding(top = 34.dp, start = 12.dp),
            )

            Row(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .windowInsetsPadding(WindowInsets.systemBars)
                    .padding(top = 26.dp, start = 16.dp, end = 16.dp, bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(if (translationsActive) Color.White.copy(alpha = 0.9f) else Color.White.copy(alpha = 0.14f))
                        .border(1.dp, IrideMp3PanelBorderColor, CircleShape)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {
                                if (translationsActive) {
                                    currentLyricsEntity?.let { entity ->
                                        database.query { upsert(LyricsTranslationHelper.clearTranslations(entity)) }
                                        LyricsTranslationHelper.triggerClearTranslations()
                                    }
                                } else {
                                    LyricsTranslationHelper.triggerManualTranslation()
                                }
                            },
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = painterResource(R.drawable.translate),
                        contentDescription = null,
                        tint = if (translationsActive) Color.Black else Color.White.copy(alpha = 0.9f),
                        modifier = Modifier.size(20.dp),
                    )
                }
                AnimatedVisibility(
                    visible = hasRomanizedText,
                    enter = fadeIn(tween(IrideMotion.Medium)) +
                        scaleIn(
                            initialScale = 0.6f,
                            animationSpec = tween(IrideMotion.Medium, easing = IrideMotion.EaseOutExpo),
                        ),
                    exit = fadeOut(tween(IrideMotion.Short)) +
                        scaleOut(targetScale = 0.6f, animationSpec = tween(IrideMotion.Short)),
                ) {
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
    onTextLayout: ((TextLayoutResult) -> Unit)? = null,
    enabled: Boolean = true,
) {
    var layoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }
    var pressedArtistRange by remember { mutableStateOf<IntRange?>(null) }

    // The underline is applied only to the artist currently being pressed (never the whole
    // line), and the tap position is resolved inside the gesture handler itself so it can't
    // go stale like the previous separate pointer-input capture loop. When [enabled] is
    // false no gesture handler is attached at all, so taps fall through to whatever is
    // underneath (e.g. the mini player row).
    val annotated = remember(mediaMetadata.artists, pressedArtistRange) {
        buildAnnotatedString {
            mediaMetadata.artists.forEachIndexed { index, artist ->
                val start = length
                pushStringAnnotation(tag = "artist", annotation = artist.id.orEmpty())
                val range = pressedArtistRange
                if (range != null && start >= range.first && start <= range.last) {
                    pushStyle(SpanStyle(textDecoration = TextDecoration.Underline))
                }
                append(artist.name)
                if (range != null && start >= range.first && start <= range.last) {
                    pop()
                }
                pop()
                if (index != mediaMetadata.artists.lastIndex) append(", ")
            }
        }
    }

    Text(
        text = annotated,
        color = color,
        fontFamily = fontFamily,
        fontSize = fontSize,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        onTextLayout = {
            layoutResult = it
            onTextLayout?.invoke(it)
        },
        modifier = modifier.then(
            if (enabled) {
                Modifier.pointerInput(mediaMetadata.artists) {
                    detectTapGestures(
                        onPress = { pos ->
                            val charOffset = layoutResult?.getOffsetForPosition(pos)
                            pressedArtistRange = charOffset?.let { offset ->
                                annotated.getStringAnnotations("artist", offset, offset)
                                    .firstOrNull()
                                    ?.let { annotation -> annotation.start..annotation.end }
                            }
                            try {
                                awaitRelease()
                            } finally {
                                pressedArtistRange = null
                            }
                        },
                        onTap = { pos ->
                            val layout = layoutResult ?: return@detectTapGestures
                            val charOffset = layout.getOffsetForPosition(pos)
                            annotated.getStringAnnotations("artist", charOffset, charOffset)
                                .firstOrNull()
                                ?.let { ann -> if (ann.item.isNotBlank()) onArtistClick(ann.item) }
                        },
                    )
                }
            } else {
                Modifier
            },
        ),
    )
}

@Composable
private fun IrideQueuePreview(
    mediaMetadata: MediaMetadata,
    modifier: Modifier = Modifier,
    topClearance: Dp = 0.dp,
    radioTrigger: Int = 0,
    openNonce: Int = 0,
) {
    val playerConnection = LocalPlayerConnection.current ?: return
    val database = LocalDatabase.current
    val queueWindows by playerConnection.queueWindows.collectAsState()
    val currentWindowIndex by playerConnection.currentWindowIndex.collectAsState()

    // Only the current + upcoming portion of the timeline is listed here; tracks that have
    // already played surface in the history section above instead of lingering queued.
    // If the window index flow hasn't caught up yet, fall back to locating the current song
    // by id so the playing track is always the first queue row (and always highlighted).
    val visibleQueueWindows = remember(queueWindows, currentWindowIndex, mediaMetadata.id) {
        val startIndex = when {
            currentWindowIndex in queueWindows.indices -> currentWindowIndex
            else -> queueWindows.indexOfFirst { it.mediaItem.metadata?.id == mediaMetadata.id }
                .takeIf { it >= 0 } ?: 0
        }
        if (startIndex in queueWindows.indices) queueWindows.drop(startIndex) else queueWindows
    }

    val mutableQueueWindows = remember { mutableStateListOf<Timeline.Window>() }
    LaunchedEffect(visibleQueueWindows) {
        mutableQueueWindows.apply {
            clear()
            addAll(visibleQueueWindows)
        }
    }

    val currentPlayingUid = visibleQueueWindows.firstOrNull()?.uid

    var historyItems by remember { mutableStateOf<List<MediaMetadata>>(emptyList()) }
    var lastKnownMetadata by remember { mutableStateOf(mediaMetadata) }

    val lazyListState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    // LazyColumn row index of the currently playing track: [history header] + history rows +
    // [continue-listening header] all sit above it, so scrolling here pins the current song
    // to the top of the viewport while the history stays "covered" just above.
    fun currentRowIndex(): Int = (if (historyItems.isNotEmpty()) 1 else 0) + historyItems.size + 1

    // Live-append the track that just finished while the panel stays open (newest-first).
    LaunchedEffect(mediaMetadata.id) {
        val previous = lastKnownMetadata
        if (previous.id != mediaMetadata.id && historyItems.none { it.id == previous.id }) {
            historyItems = listOf(previous) + historyItems.take(19)
        }
        lastKnownMetadata = mediaMetadata
    }

    // Reload history every time the panel is opened, then land on the currently playing
    // track; the history section sits right above and is revealed by scrolling up.
    LaunchedEffect(openNonce) {
        val currentSongId = mediaMetadata.id
        val page = database.recentEventsPerSong(21).first()
            .filter { it.song.id != currentSongId }
            .take(20)
        historyItems = page.map { it.song.toMediaMetadata() }
        lastKnownMetadata = mediaMetadata
        if (historyItems.isNotEmpty() || mutableQueueWindows.isNotEmpty()) {
            lazyListState.scrollToItem(currentRowIndex())
        }
    }

    val automix by playerConnection.service.automixItems.collectAsState()
    val isRadioOn by playerConnection.service.isAutoMixQueueActive.collectAsState()
    // Seeded "similar content" only becomes a visible radio once the user actually arms the
    // radio â€” the wheel icon and this section always agree on what "on" means.
    val visibleAutomix = if (isRadioOn) automix else emptyList()
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
    val radioSourceItems = if (isRadioOn) mutableAutomixItems.toList() else emptyList()
    val automixGenreSongs = remember(radioSourceItems) {
        radioSourceItems.map {
            GenreSongInfo(id = it.mediaId, title = it.metadata?.title.orEmpty(), artist = it.metadata?.artists?.firstOrNull()?.name)
        }
    }
    val automixGenreFilter = rememberGenreFilter(automixGenreSongs)
    val dynamicGenreFilter = automixGenreFilter.sortedGenres.firstOrNull()?.uppercase()
    val automixFilters = remember(dynamicGenreFilter) {
        if (dynamicGenreFilter != null) AUTOMIX_STATIC_FILTERS + dynamicGenreFilter else AUTOMIX_STATIC_FILTERS
    }
    var selectedAutomixFilter by remember { mutableStateOf(AUTOMIX_FILTER_ALL) }
    val filteredAutomix =
        remember(radioSourceItems, selectedAutomixFilter, familiarArtistNames, automixGenreFilter.genreBySongId) {
            // Fixed upcoming batch: the promoted track in the timeline plus these virtual
            // rows always add up to the same constant.
            filterAutomix(radioSourceItems, selectedAutomixFilter, familiarArtistNames, automixGenreFilter.genreBySongId)
                .take(playerConnection.service.automixUpcomingLimit - 1)
        }

    // A filter is authoritative, not cosmetic: choosing one REPLACES the radio's real play
    // order with exactly the filtered list, so what you see here is what will sound.
    LaunchedEffect(selectedAutomixFilter, isRadioOn) {
        if (!isRadioOn || selectedAutomixFilter == AUTOMIX_FILTER_ALL) return@LaunchedEffect
        delay(80)
        val ordered = filterAutomix(
            mutableAutomixItems.toList(),
            selectedAutomixFilter,
            familiarArtistNames,
            automixGenreFilter.genreBySongId,
        )
        playerConnection.service.applyAutomixOrder(ordered)
    }

    // Flat slot list of the three sections. Rebuilt only while no drag is in progress so a
    // live reorder never fights the source refresh mid-gesture. The radio branch sits
    // DIRECTLY under the currently playing track (before the rest of the queue) so it is
    // immediately visible.
    val combinedList = remember { mutableStateListOf<IrideQueueSlot>() }
    var draggedSlotKey by remember { mutableStateOf<Any?>(null) }
    var dragFromOrdinal by remember { mutableStateOf(-1) }
    var dragFromAutomixOrdinal by remember { mutableStateOf(-1) }

    val reorderableState = rememberReorderableLazyListState(lazyListState = lazyListState) { from, to ->
        val fromIdx = combinedList.indexOfFirst { it.key == from.key }
        val toIdxRaw = combinedList.indexOfFirst { it.key == to.key }
        if (fromIdx != -1 && toIdxRaw != -1) {
            if (draggedSlotKey != from.key) {
                draggedSlotKey = from.key
                val moving = combinedList[fromIdx]
                dragFromOrdinal = if (moving is IrideQueueSlot.QueueEntry) {
                    combinedList.take(fromIdx).count { it is IrideQueueSlot.QueueEntry }
                } else {
                    -1
                }
                dragFromAutomixOrdinal = if (moving is IrideQueueSlot.Automix) {
                    combinedList.take(fromIdx).count { it is IrideQueueSlot.Automix }
                } else {
                    -1
                }
            }
            val moving = combinedList[fromIdx]
            val firstQueueIdx = combinedList.indexOfFirst { it is IrideQueueSlot.QueueEntry }
                .let { if (it == -1) combinedList.size else it }
            val firstAutomixIdx = combinedList.indexOfFirst { it is IrideQueueSlot.Automix }
                .let { if (it == -1) combinedList.size else it }
            // The coercion below IS the visual block: the reorder library only animates
            // swaps this callback accepts, so a forbidden direction freezes the row in
            // place instead of snapping back after the fact.
            val toIdx: Int? = when (moving) {
                // History may drop anywhere except on top of the playing row; landing in
                // the radio section inserts the track into the radio itself.
                is IrideQueueSlot.History -> {
                    val allowed = combinedList.indices.filter { it != firstQueueIdx }
                    allowed.minByOrNull { kotlin.math.abs(it - toIdxRaw) }
                }
                // Queue entries stay after the playing row (they can never become the
                // current track) and may land inside the radio section to join it.
                is IrideQueueSlot.QueueEntry -> {
                    val allowed = (firstQueueIdx + 1 until combinedList.size).toList()
                    allowed.minByOrNull { kotlin.math.abs(it - toIdxRaw) }
                }
                // Radio rows reorder freely inside their section and can be pulled out
                // into the queue right after the playing row.
                is IrideQueueSlot.Automix -> {
                    val allowed = (firstQueueIdx + 1 until combinedList.size).toList()
                    allowed.minByOrNull { kotlin.math.abs(it - toIdxRaw) }
                }
            }
            if (toIdx != null && fromIdx != toIdx) combinedList.move(fromIdx, toIdx)
        }
    }

    LaunchedEffect(historyItems, visibleQueueWindows, filteredAutomix, reorderableState.isAnyItemDragging) {
        if (reorderableState.isAnyItemDragging) return@LaunchedEffect
        // The promoted radio track is the REAL next window in the timeline; it is shown
        // right after the current row, followed by the virtual reserve, so what plays next
        // always matches what this list shows, top to bottom.
        val promotedUid = visibleQueueWindows.drop(1)
            .firstOrNull { playerConnection.service.isRadioPromoted(it.mediaItem.mediaId) }
            ?.uid
        // A song that is already a visible timeline row can never appear again as a radio
        // row — this belt makes duplicate rows impossible even if the reserve ever races.
        val shownWindowIds = visibleQueueWindows.mapTo(HashSet()) { it.mediaItem.mediaId }
        combinedList.apply {
            clear()
            addAll(historyItems.map { IrideQueueSlot.History(it) })
            visibleQueueWindows.firstOrNull()?.let { add(IrideQueueSlot.QueueEntry(it)) }
            visibleQueueWindows.drop(1)
                .firstOrNull { it.uid == promotedUid }
                ?.let { add(IrideQueueSlot.QueueEntry(it)) }
            addAll(filteredAutomix.filterNot { it.mediaId in shownWindowIds }.map { IrideQueueSlot.Automix(it) })
            visibleQueueWindows.drop(1)
                .filter { it.uid != promotedUid }
                .forEach { add(IrideQueueSlot.QueueEntry(it)) }
        }
    }

    Column(modifier = modifier.padding(top = topClearance)) {
        Text(
            text = stringResource(R.string.queue).uppercase(),
            color = Color.White.copy(alpha = 0.6f),
            fontFamily = SpaceMonoFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 10.sp,
            letterSpacing = 1.sp,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
        )

        var lastHandledRadioTrigger by rememberSaveable { mutableStateOf(0) }
        LaunchedEffect(radioTrigger) {
            if (radioTrigger == 0 || radioTrigger == lastHandledRadioTrigger) return@LaunchedEffect
            lastHandledRadioTrigger = radioTrigger
            val service = playerConnection.service
            if (service.isAutoMixQueueActive.value) {
                // Tapping the radio control again turns the radio off entirely.
                service.clearRadioState()
                return@LaunchedEffect
            }
            selectedAutomixFilter = AUTOMIX_FILTER_ALL
            // Arming the radio takes effect immediately: the service inserts the first
            // generated track right after the current one as soon as the fetch lands, so the
            // very next skip already follows the list shown here.
            service.startAutoMixRadio(mediaMetadata)
            delay(80)
            lazyListState.scrollToItem(currentRowIndex())
        }

        if (mutableQueueWindows.isEmpty() && historyItems.isEmpty() && visibleAutomix.isEmpty()) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text(
                    text = stringResource(R.string.queue_empty),
                    color = Color.White.copy(alpha = 0.6f),
                    fontFamily = InterFontFamily,
                    fontSize = 12.sp,
                    letterSpacing = 0.5.sp,
                )
            }
            return@Column
        }

        LaunchedEffect(reorderableState.isAnyItemDragging) {
            if (reorderableState.isAnyItemDragging) return@LaunchedEffect
            val key = draggedSlotKey
            val capturedQueueOrdinal = dragFromOrdinal
            val capturedAutomixOrdinal = dragFromAutomixOrdinal
            draggedSlotKey = null
            dragFromOrdinal = -1
            dragFromAutomixOrdinal = -1
            val idx = key?.let { k -> combinedList.indexOfFirst { it.key == k } } ?: -1
            if (idx == -1) return@LaunchedEffect

            val hiddenBefore = currentWindowIndex.coerceAtLeast(0)
            val firstQueueIdx = combinedList.indexOfFirst { it is IrideQueueSlot.QueueEntry }
                .let { if (it == -1) combinedList.size else it }
            val firstAutomixIdx = combinedList.indexOfFirst { it is IrideQueueSlot.Automix }
                .let { if (it == -1) combinedList.size else it }
            val automixCount = combinedList.count { it is IrideQueueSlot.Automix }
            fun inRadioRegion(i: Int) =
                automixCount > 0 && i >= firstAutomixIdx && i < firstAutomixIdx + automixCount

            when (val slot = combinedList[idx]) {
                is IrideQueueSlot.History -> {
                    when {
                        inRadioRegion(idx) -> {
                            // Dropped inside the radio section: the track joins the radio.
                            val radioOffset = combinedList.take(idx).count { it is IrideQueueSlot.Automix }
                            playerConnection.service.insertIntoAutomix(slot.metadata.toMediaItem(), radioOffset)
                            historyItems = historyItems.filter { it.id != slot.metadata.id }
                        }
                        idx < firstQueueIdx -> {
                            // Still inside its own section: persist the new order.
                            historyItems = combinedList.take(firstQueueIdx)
                                .filterIsInstance<IrideQueueSlot.History>()
                                .map { it.metadata }
                        }
                        else -> {
                            // Dropped into the queue at that spot â†’ real "play next" insertion.
                            val insertionIndex = combinedList.take(idx).count { it is IrideQueueSlot.QueueEntry } +
                                hiddenBefore
                            playerConnection.player.addMediaItem(insertionIndex, slot.metadata.toMediaItem())
                            historyItems = historyItems.filter { it.id != slot.metadata.id }
                        }
                    }
                }

                is IrideQueueSlot.QueueEntry -> {
                    if (inRadioRegion(idx)) {
                        // Moved into the radio section: leaves the real timeline and joins
                        // the radio at that position.
                        val window = slot.window
                        val radioOffset = combinedList.take(idx).count { it is IrideQueueSlot.Automix }
                        playerConnection.service.insertIntoAutomix(window.mediaItem, radioOffset)
                        val realIndex = window.firstPeriodIndex
                        if (realIndex in 0 until playerConnection.player.mediaItemCount &&
                            realIndex != playerConnection.player.currentMediaItemIndex
                        ) {
                            playerConnection.player.removeMediaItem(realIndex)
                        }
                    } else {
                        val fromOrdinal = capturedQueueOrdinal.coerceIn(0, queueWindows.lastIndex)
                        val toOrdinal = (
                            combinedList.take(idx).count { it is IrideQueueSlot.QueueEntry } - 1
                            ).coerceIn(0, queueWindows.lastIndex)
                        if (!playerConnection.player.shuffleModeEnabled) {
                            playerConnection.player.moveMediaItem(
                                fromOrdinal + hiddenBefore,
                                toOrdinal + hiddenBefore,
                            )
                        } else {
                            playerConnection.player.setShuffleOrder(
                                DefaultShuffleOrder(
                                    mutableQueueWindows
                                        .map { it.firstPeriodIndex }
                                        .toMutableList()
                                        .move(fromOrdinal, toOrdinal)
                                        .toIntArray(),
                                    System.currentTimeMillis(),
                                ),
                            )
                        }
                    }
                }

                is IrideQueueSlot.Automix -> {
                    if (!inRadioRegion(idx)) {
                        // Pulled out of the radio into the queue: promoted at that spot.
                        val insertionIndex = combinedList.take(idx).count { it is IrideQueueSlot.QueueEntry } +
                            hiddenBefore
                        playerConnection.service.promoteAutomixToQueue(slot.item, insertionIndex)
                    } else {
                        val toRadioOffset = combinedList.take(idx).count { it is IrideQueueSlot.Automix }
                        playerConnection.service.reorderAutomix(capturedAutomixOrdinal, toRadioOffset)
                    }
                }
            }
        }

        Box(modifier = Modifier.weight(1f)) {
        LazyColumn(
            state = lazyListState,
            modifier = Modifier.fillMaxSize().padding(start = 10.dp, end = 10.dp, top = 2.dp, bottom = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            val renderFirstQueueIdx = combinedList.indexOfFirst { it is IrideQueueSlot.QueueEntry }
                .let { if (it == -1) combinedList.size else it }
            // The radio group starts at the promoted track (the one that will actually play
            // next), so skipping always fires the FIRST row visible under the RADIO header.
            val renderAutomixStartIdx = combinedList.indexOfFirst { slot ->
                (slot is IrideQueueSlot.QueueEntry &&
                    playerConnection.service.isRadioPromoted(slot.window.mediaItem.mediaId)) ||
                    slot is IrideQueueSlot.Automix
            }.let { if (it == -1) combinedList.size else it }

            if (historyItems.isNotEmpty()) {
                item(key = "history_header") {
                    Text(
                        text = stringResource(R.string.queue_history_title).uppercase(),
                        color = Color.White.copy(alpha = 0.6f),
                        fontFamily = SpaceMonoFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 10.sp,
                        letterSpacing = 1.sp,
                        modifier = Modifier.animateItem(),
                    )
                }
            }

            combinedList.forEachIndexed { slotIdx, slot ->
                if (slotIdx == renderFirstQueueIdx) {
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
                }

                if (slotIdx == renderAutomixStartIdx && renderAutomixStartIdx < combinedList.size) {
                    item(key = "radio_section_header") {
                        Column(modifier = Modifier.animateItem()) {
                            Text(
                                text = stringResource(R.string.queue_radio_section_title).uppercase(),
                                color = Color.White.copy(alpha = 0.6f),
                                fontFamily = SpaceMonoFontFamily,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 10.sp,
                                letterSpacing = 1.sp,
                            )
                            Text(
                                text = stringResource(R.string.queue_radio_section_subtitle),
                                color = Color.White.copy(alpha = 0.4f),
                                fontFamily = InterFontFamily,
                                fontSize = 10.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }

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
                }

                item(key = slot.key) {
                    ReorderableItem(state = reorderableState, key = slot.key) {
                        when (slot) {
                            is IrideQueueSlot.History -> {
                                val item = slot.metadata
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .animateItem()
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
                                            color = Color.White.copy(alpha = 0.75f),
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

                            is IrideQueueSlot.QueueEntry -> {
                                val window = slot.window
                                val metadata = window.mediaItem.metadata
                                val isActive = window.uid == currentPlayingUid
                                // Promoted radio rows belong visually to the radio group.
                                val isRadioRow = playerConnection.service.isRadioPromoted(window.mediaItem.mediaId)
                                val dismissState = rememberSwipeToDismissBoxState(
                                    positionalThreshold = { totalDistance -> totalDistance },
                                )
                                var processedDismiss by remember { mutableStateOf(false) }

                                LaunchedEffect(dismissState.currentValue) {
                                    val dismissValue = dismissState.currentValue
                                    if (!processedDismiss && dismissValue != SwipeToDismissBoxValue.Settled) {
                                        processedDismiss = true
                                        val realIndex = window.firstPeriodIndex
                                        if (realIndex in 0 until playerConnection.player.mediaItemCount &&
                                            realIndex != playerConnection.player.currentMediaItemIndex
                                        ) {
                                            playerConnection.player.removeMediaItem(realIndex)
                                            if (isRadioRow) {
                                                // The next radio track must take its place
                                                // immediately, not at the next transition.
                                                playerConnection.service.notifyAutomixChanged()
                                            }
                                        } else {
                                            // Nothing was removed: settle the row back instead
                                            // of leaving it stranded half-swiped.
                                            coroutineScope.launch {
                                                dismissState.snapTo(SwipeToDismissBoxValue.Settled)
                                            }
                                        }
                                    }
                                    if (dismissValue == SwipeToDismissBoxValue.Settled) {
                                        processedDismiss = false
                                    }
                                }

                                SwipeToDismissBox(
                                    state = dismissState,
                                    // The playing track can never be swiped away.
                                    enableDismissFromStartToEnd = !isActive,
                                    enableDismissFromEndToStart = !isActive,
                                    backgroundContent = {
                                        SwipeTrashBadge(
                                            revealed = dismissState.targetValue != SwipeToDismissBoxValue.Settled,
                                        )
                                    },
                                ) {
                                    Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .animateItem()
                                        .then(
                                            if (isRadioRow) {
                                                Modifier
                                                    .drawBehind {
                                                        // Same vertical branch line as the rest
                                                        // of the radio group.
                                                        val x = 2.dp.toPx()
                                                        drawLine(
                                                            color = IrideRadioBranchColor,
                                                            start = Offset(x, -7.dp.toPx()),
                                                            end = Offset(x, size.height + 7.dp.toPx()),
                                                            strokeWidth = 2.dp.toPx(),
                                                            cap = StrokeCap.Round,
                                                        )
                                                    }
                                                    .padding(start = 12.dp)
                                            } else {
                                                Modifier
                                            },
                                        )
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
                                            color = Color.White,
                                            fontFamily = InterFontFamily,
                                            fontWeight = if (isActive || isRadioRow) FontWeight.SemiBold else FontWeight.Normal,
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

                            is IrideQueueSlot.Automix -> {
                                val item = slot.item
                                val metadata = item.metadata
                                val dismissState = rememberSwipeToDismissBoxState(
                                    positionalThreshold = { totalDistance -> totalDistance },
                                )
                                LaunchedEffect(dismissState.currentValue) {
                                    // Radio rows live outside the timeline: dropping them is
                                    // instantaneous and playback is untouched.
                                    if (dismissState.currentValue != SwipeToDismissBoxValue.Settled) {
                                        playerConnection.service.removeFromAutomix(item.mediaId)
                                    }
                                }
                                SwipeToDismissBox(
                                    state = dismissState,
                                    modifier = Modifier.animateItem(),
                                    backgroundContent = {
                                        SwipeTrashBadge(
                                            revealed = dismissState.targetValue != SwipeToDismissBoxValue.Settled,
                                        )
                                    },
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .drawBehind {
                                                // Vertical branch line tying the radio songs together.
                                                val x = 2.dp.toPx()
                                                drawLine(
                                                    color = IrideRadioBranchColor,
                                                    start = Offset(x, -7.dp.toPx()),
                                                    end = Offset(x, size.height + 7.dp.toPx()),
                                                    strokeWidth = 2.dp.toPx(),
                                                    cap = StrokeCap.Round,
                                                )
                                            }
                                            .padding(start = 12.dp)
                                            .clickable(
                                                interactionSource = remember { MutableInteractionSource() },
                                                indication = null,
                                            ) {
                                                val realIndex = visibleAutomix.indexOfFirst { it.mediaId == item.mediaId }
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
        }
    }
}

/** Trash badge shown behind a queue row, fading in only while the row is being swiped. */
@Composable
private fun SwipeTrashBadge(revealed: Boolean) {
    val alpha by animateFloatAsState(
        targetValue = if (revealed) 1f else 0f,
        animationSpec = tween(if (revealed) 120 else 150),
        label = "irideSwipeTrashAlpha",
    )
    Box(
        contentAlignment = Alignment.CenterEnd,
        modifier = Modifier
            .fillMaxSize()
            .padding(end = 14.dp),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(30.dp)
                .graphicsLayer { this.alpha = alpha }
                .clip(irideSquircle(10.dp))
                .background(Color.White.copy(alpha = 0.10f))
                .border(1.dp, IrideMp3PanelBorderColor, irideSquircle(10.dp)),
        ) {
            Icon(
                painter = painterResource(R.drawable.delete),
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.85f),
                modifier = Modifier.size(16.dp),
            )
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
    val ringWidth = (wheelSize - centerButtonSize) / 2
    val zoneEdgeInset = ringWidth / 6
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
                    // Deliberately dimmer than the other idle icons so the active (white)
                    // state is unmistakable.
                    tint = if (isRadioActive) Color.White else Color.White.copy(alpha = 0.32f),
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
    edgeInset: Dp,
    onClick: () -> Unit,
    enabled: Boolean = true,
    hitWidth: Dp = size,
    hitHeight: Dp = size,
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
 * Plain-text LYRICS/QUEUE toggle â€” gray at rest, white when active. No icon, no background:
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
    navController: NavController,
    playerBottomSheetState: BottomSheetState,
    modifier: Modifier = Modifier,
) {
    val playerConnection = LocalPlayerConnection.current ?: return
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()
    val metadata = mediaMetadata ?: return
    val reducedMotion = rememberReducedMotion()
    val sheetProgress = playerBottomSheetState.progress
    val eased = if (reducedMotion) {
        if (sheetProgress >= 0.5f) 1f else 0f
    } else {
        sheetProgress.coerceIn(0f, 1f)
    }
    var rootOffset by remember { mutableStateOf(Offset.Zero) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .onGloballyPositioned { rootOffset = it.positionInWindow() },
    ) {
        val artStart = bridgeState.miniArt ?: bridgeState.playerArt
        val artEnd = bridgeState.playerArt ?: artStart
        if (artStart != null && artEnd != null && !bridgeState.panelActive) {
            BridgedElement(start = artStart, end = artEnd, rootOffset = rootOffset, progress = eased) { scale ->
                val onScreenRadius = lerp(PillCoverRadius.value, IrideCoverBorderRadius.value, eased)
                val compensatedRadius = (onScreenRadius / scale.coerceAtLeast(0.01f)).coerceAtMost(200f)
                val context = LocalContext.current
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(metadata.thumbnailUrl)
                        .size(1024)
                        .build(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(irideSquircle(compensatedRadius.dp)),
                )
            }
        }

        // Title/artist use a pure crossfade now: the pill's own text fades out with the
        // sheet progress while the player's own text fades in, so nothing slides between
        // the two sizes anymore.
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
