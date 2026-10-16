package com.metrolist.music.ui.player

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import android.view.OrientationEventListener
import android.view.TextureView
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import coil3.compose.AsyncImage
import com.metrolist.music.LocalPlayerConnection
import com.metrolist.music.R
import com.metrolist.music.models.MediaMetadata
import com.metrolist.music.ui.component.IrideFolio
import com.metrolist.music.ui.component.IrideFolioFrostedButton
import com.metrolist.music.ui.component.irideFolioPlayerArtworkRequest
import com.metrolist.music.ui.theme.SpaceMonoFontFamily
import com.metrolist.music.ui.utils.IrideMotion
import com.metrolist.music.ui.utils.rememberReducedMotion
import kotlinx.coroutines.delay

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
@Composable
internal fun IrideMp3Video(
    metadata: MediaMetadata,
    duration: Long,
    isPlaying: Boolean,
    isGuest: Boolean,
    onSeek: (Float) -> Unit,
    fullscreen: Boolean,
    onToggleControls: () -> Unit,
    morphing: Boolean,
) {
    val connection = LocalPlayerConnection.current ?: return
    val video = connection.videoPlayback
    val state by video.state.collectAsState()
    val player by connection.service.playerFlow.collectAsState()
    val active = player ?: return
    val playbackState by connection.playbackState.collectAsState()
    val lifecycleState by LocalLifecycleOwner.current.lifecycle.currentStateFlow.collectAsState()
    val surfaceVisible = lifecycleState.isAtLeast(Lifecycle.State.STARTED)
    val reducedMotion = rememberReducedMotion()
    val context = LocalContext.current
    val activity = remember(context) { context.videoActivity() }
    var enlarged by remember(metadata.id, active) { mutableStateOf(false) }
    val rotationPolicy = remember { VideoRotationPolicy() }
    var seekFeedback by remember { mutableStateOf<Int?>(null) }
    var seekFeedbackKey by remember { mutableIntStateOf(0) }
    var aspect by remember(active) { mutableFloatStateOf(16f / 9f) }
    val currentDuration by rememberUpdatedState(duration)
    val currentSeek by rememberUpdatedState(onSeek)
    val currentGuest by rememberUpdatedState(isGuest)
    val latestToggleControls by rememberUpdatedState(onToggleControls)
    var surfaceRotation by remember { mutableIntStateOf(0) }
    val canZoom = state.isVideo && surfaceVisible && !morphing &&
        VideoZoomPolicy.isAllowed(aspect, surfaceRotation)
    LaunchedEffect(surfaceRotation, fullscreen, canZoom) {
        enlarged = false
    }
    val animatedRotation by
        animateFloatAsState(
            surfaceRotation.toFloat(),
            if (reducedMotion) snap() else tween(IrideMotion.Medium),
            label = "videoSurfaceRotation",
        )
    DisposableEffect(activity) {
        val previousOrientation = activity?.requestedOrientation
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        onDispose { previousOrientation?.let { activity?.requestedOrientation = it } }
    }
    DisposableEffect(context, surfaceVisible) {
        val listener =
            object : OrientationEventListener(context) {
                override fun onOrientationChanged(orientation: Int) {
                    surfaceRotation = rotationPolicy.onOrientation(orientation)
                }
            }
        if (surfaceVisible && listener.canDetectOrientation()) listener.enable()
        onDispose { listener.disable() }
    }
    DisposableEffect(active) {
        val listener =
            object : Player.Listener {
                override fun onVideoSizeChanged(videoSize: VideoSize) {
                    if (videoSize.width > 0 && videoSize.height > 0) {
                        aspect =
                            videoSize.width * videoSize.pixelWidthHeightRatio / videoSize.height
                    }
                }
            }
        active.addListener(listener)
        listener.onVideoSizeChanged(active.videoSize)
        onDispose { active.removeListener(listener) }
    }
    LaunchedEffect(seekFeedbackKey) {
        delay(800)
        seekFeedback = null
    }
    fun seekBy(seconds: Int) {
        if (currentGuest || currentDuration <= 0 || !active.isCurrentMediaItemSeekable) return
        currentSeek(
            ((active.currentPosition + seconds * 1000L).toDouble() / currentDuration)
                .toFloat()
                .coerceIn(0f, 1f)
        )
        seekFeedback = seekFeedback?.takeIf { it * seconds > 0 }?.plus(seconds) ?: seconds
        seekFeedbackKey++
    }

    @Composable
    fun Action(icon: Int, label: Int, enabled: Boolean = true, onClick: () -> Unit) {
        IrideFolioFrostedButton(
            onClick = onClick,
            enabled = enabled,
            grouped = true,
            contentPadding = PaddingValues(0.dp),
            modifier = Modifier.size(IrideFolio.PlayerActionSize),
        ) {
            Icon(
                painterResource(icon),
                stringResource(label),
                tint = Color.White,
                modifier = Modifier.size(IrideFolio.PlayerActionIconSize),
            )
        }
    }
    BoxWithConstraints(
        Modifier.fillMaxSize().background(Color.Black).clipToBounds(),
        contentAlignment = Alignment.Center,
    ) {
        BoxWithConstraints(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            val radians = Math.toRadians(animatedRotation.toDouble())
            val cosine = kotlin.math.abs(kotlin.math.cos(radians)).toFloat()
            val sine = kotlin.math.abs(kotlin.math.sin(radians)).toFloat()
            val rotatedWidth = aspect * cosine + sine
            val rotatedHeight = aspect * sine + cosine
            val unit = minOf(maxWidth / rotatedWidth, maxHeight / rotatedHeight)
            val surfaceWidth = unit * aspect
            val surfaceHeight = unit
            val fitWidth = unit * rotatedWidth
            val fitHeight = unit * rotatedHeight
            val scale by
                animateFloatAsState(
                    if (enlarged && canZoom) VideoZoomPolicy.EnlargedScale else 1f,
                    if (reducedMotion) snap()
                    else tween(IrideMotion.Medium, easing = IrideMotion.EaseOutQuart),
                    label = "videoZoom",
                )
            // The crop follows the same animated scale as the film instead of jumping first.
            val frameWidth = minOf(maxWidth, fitWidth * scale)
            val frameHeight = minOf(maxHeight, fitHeight * scale)
            if (!state.isVideo)
                AsyncImage(
                    model = irideFolioPlayerArtworkRequest(context, metadata.thumbnailUrl),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            if (state.isVideo) {
                Box(
                    Modifier.size(frameWidth, frameHeight)
                        .clip(RoundedCornerShape(IrideFolio.PlayerPanelCornerRadius)),
                    contentAlignment = Alignment.Center,
                ) {
                    AndroidView(
                        factory = { TextureView(it) },
                        update = { texture ->
                            texture.keepScreenOn = isPlaying && surfaceVisible
                            if (surfaceVisible) video.attachSurface(active, texture)
                            else video.detachSurface(active, texture)
                        },
                        onReset = {},
                        onRelease = { texture ->
                            texture.keepScreenOn = false
                            video.detachSurface(active, texture)
                        },
                        modifier =
                            Modifier.requiredSize(surfaceWidth, surfaceHeight).graphicsLayer {
                                rotationZ = animatedRotation
                                scaleX = scale
                                scaleY = scale
                            },
                    )
                }
                Box(
                    Modifier.fillMaxSize()
                        .pointerInput(active, fullscreen) {
                            detectTapGestures(
                                onTap = { if (fullscreen) latestToggleControls() },
                                onDoubleTap = { seekBy(if (it.x < size.width / 2f) -10 else 10) },
                            )
                        }
                        .pointerInput(active, metadata.id, canZoom) {
                            if (!canZoom) return@pointerInput
                            awaitEachGesture {
                                awaitFirstDown(requireUnconsumed = false)
                                var factor = 1f
                                do {
                                    val event = awaitPointerEvent()
                                    if (event.changes.count { it.pressed } >= 2) {
                                        factor *= event.calculateZoom()
                                        enlarged = VideoZoomPolicy.afterPinch(enlarged, factor, canZoom)
                                        event.changes.forEach { it.consume() }
                                    }
                                } while (event.changes.any { it.pressed })
                            }
                        }
                )
            }
        }
        if (
            state.loading ||
                state.searching ||
                state.isVideo && playbackState == Player.STATE_BUFFERING
        ) {
            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(32.dp))
        }
        seekFeedback?.let { seconds ->
            key(seekFeedbackKey) {
                val motion = remember { Animatable(0f) }
                LaunchedEffect(Unit) { if (!reducedMotion) motion.animateTo(1f, tween(700)) }
                val direction = if (seconds < 0) -1f else 1f
                Row(
                    Modifier.align(if (seconds < 0) Alignment.CenterStart else Alignment.CenterEnd)
                        .windowInsetsPadding(
                            WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal)
                        )
                        .padding(horizontal = maxOf(48.dp, maxWidth * 0.16f))
                        .graphicsLayer {
                            alpha =
                                if (reducedMotion) 1f
                                else
                                    minOf(motion.value / 0.15f, (1f - motion.value) / 0.2f)
                                        .coerceIn(0f, 1f)
                            translationX =
                                if (reducedMotion) 0f else direction * motion.value * 18.dp.toPx()
                        }
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (seconds > 0)
                        Text(
                            stringResource(R.string.mp3_video_seek_seconds, seconds),
                            color = Color.White,
                            fontFamily = SpaceMonoFontFamily,
                            fontSize = 12.sp,
                        )
                    Row(Modifier.padding(horizontal = 4.dp).graphicsLayer { scaleX = direction }) {
                        repeat(3) { index ->
                            Icon(
                                painterResource(R.drawable.arrow_forward),
                                null,
                                tint = Color.White,
                                modifier =
                                    Modifier.size(14.dp).graphicsLayer {
                                        alpha =
                                            if (reducedMotion) 1f
                                            else
                                                ((motion.value - index * 0.14f) * 3f).coerceIn(
                                                    0f,
                                                    1f,
                                                )
                                        translationX =
                                            if (reducedMotion) 0f
                                            else (1f - motion.value) * -6.dp.toPx()
                                    },
                            )
                        }
                    }
                    if (seconds < 0)
                        Text(
                            stringResource(R.string.mp3_video_seek_seconds, seconds),
                            color = Color.White,
                            fontFamily = SpaceMonoFontFamily,
                            fontSize = 12.sp,
                        )
                }
            }
        }
        if (state.failed || !state.isVideo && !state.searching && !state.loading) {
            Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    stringResource(
                        if (state.failed) R.string.mp3_video_error
                        else R.string.mp3_video_no_results
                    ),
                    color = Color.White,
                    fontFamily = SpaceMonoFontFamily,
                    fontSize = 12.sp,
                )
                Action(R.drawable.refresh, R.string.retry, !isGuest) {
                    if (state.isVideo) video.retry()
                    else video.enterVideoMode(metadata, force = true)
                }
            }
        }
    }
}

private tailrec fun Context.videoActivity(): Activity? =
    when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.videoActivity()
        else -> null
    }
