package com.metrolist.music.ui.player

import android.graphics.drawable.ColorDrawable
import android.view.WindowManager
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalAccessibilityManager
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.util.lerp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.metrolist.music.ui.utils.IrideMotion
import com.metrolist.music.ui.utils.rememberReducedMotion
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlinx.coroutines.delay

@Stable
internal class IrideMp3DisplayState {
    var fullscreen by mutableStateOf(false)
        private set

    var inFullscreenWindow by mutableStateOf(false)
        internal set

    var controlsVisible by mutableStateOf(true)
        private set

    var keepControlsVisible by mutableStateOf(false)
    internal var touching by mutableStateOf(false)
    internal var interaction by mutableIntStateOf(0)
    internal var inlineBounds by mutableStateOf<Rect?>(null)
    internal var transitionBounds: Rect? = null

    fun requestFullscreen(expand: Boolean) {
        if (expand && !inFullscreenWindow) {
            transitionBounds = inlineBounds
            inFullscreenWindow = true
        }
        fullscreen = expand
        revealControls()
    }

    fun revealControls() {
        controlsVisible = true
        interaction++
    }

    fun toggleControls() {
        controlsVisible = !controlsVisible
        interaction++
    }

    internal fun hideControls() {
        controlsVisible = false
    }
}

@Immutable
internal data class IrideMp3DisplayPresentation(val expansion: Float, val controlsOpacity: Float)

/** Moves the entire live display, including its footer, without replacing either renderer. */
@Composable
internal fun IrideMp3DisplayHost(
    state: IrideMp3DisplayState,
    mediaId: String,
    modifier: Modifier = Modifier,
    content: @Composable (IrideMp3DisplayPresentation) -> Unit,
) {
    val reducedMotion = rememberReducedMotion()
    val expansion = remember { Animatable(0f) }
    val inlineView = LocalView.current
    var restoreSystemBars by remember { mutableStateOf<(() -> Unit)?>(null) }
    LaunchedEffect(state.fullscreen, state.inFullscreenWindow) {
        val target = if (state.fullscreen) 1f else 0f
        if (!state.fullscreen && state.inFullscreenWindow) {
            restoreSystemBars?.invoke()
            withFrameNanos {}
        }
        expansion.animateTo(
            target,
            tween(
                if (reducedMotion) 0
                else (IrideMotion.Fullscreen * abs(target - expansion.value)).roundToInt(),
                easing = IrideMotion.EaseOutQuart,
            ),
        )
        if (!state.fullscreen && state.inFullscreenWindow) {
            withFrameNanos {}
            state.inFullscreenWindow = false
        }
    }
    val controlsOpacity by
        animateFloatAsState(
            if (!state.inFullscreenWindow || state.controlsVisible || expansion.value < 1f) 1f
            else 0f,
            tween(if (reducedMotion) 0 else IrideMotion.Short),
            label = "mp3DisplayControls",
        )
    val accessibility = LocalAccessibilityManager.current
    val timeout =
        accessibility?.calculateRecommendedTimeoutMillis(
            5_000L,
            containsIcons = true,
            containsText = true,
            containsControls = true,
        ) ?: 5_000L
    LaunchedEffect(mediaId, state.inFullscreenWindow) { state.revealControls() }
    LaunchedEffect(
        state.inFullscreenWindow,
        state.controlsVisible,
        state.touching,
        state.interaction,
        state.keepControlsVisible,
        expansion.value == 1f,
        timeout,
    ) {
        if (
            state.inFullscreenWindow &&
                expansion.value == 1f &&
                state.controlsVisible &&
                !state.touching &&
                !state.keepControlsVisible
        ) {
            delay(timeout)
            state.hideControls()
        }
    }
    val latestContent by rememberUpdatedState(content)
    val display = remember {
        movableContentOf<IrideMp3DisplayPresentation> { presentation ->
            latestContent(presentation)
        }
    }
    Box(modifier.onGloballyPositioned { state.inlineBounds = it.boundsInWindow() }) {
        if (!state.inFullscreenWindow) display(IrideMp3DisplayPresentation(0f, 1f))
    }
    if (state.inFullscreenWindow)
        Dialog(
            onDismissRequest = { state.requestFullscreen(false) },
            properties =
                DialogProperties(
                    usePlatformDefaultWidth = false,
                    decorFitsSystemWindows = false,
                    dismissOnClickOutside = false,
                ),
        ) {
            IrideMp3FullscreenMenuScope {
                val view = LocalView.current
                DisposableEffect(view) {
                    val window = (view.parent as? DialogWindowProvider)?.window
                    window?.apply {
                        WindowCompat.setDecorFitsSystemWindows(this, false)
                        setLayout(
                            WindowManager.LayoutParams.MATCH_PARENT,
                            WindowManager.LayoutParams.MATCH_PARENT,
                        )
                        setBackgroundDrawable(ColorDrawable(android.graphics.Color.TRANSPARENT))
                        clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
                        setWindowAnimations(0)
                    }
                    val sourceInsets = ViewCompat.getRootWindowInsets(inlineView)
                    val statusVisible =
                        sourceInsets?.isVisible(WindowInsetsCompat.Type.statusBars()) ?: true
                    val navigationVisible =
                        sourceInsets?.isVisible(WindowInsetsCompat.Type.navigationBars()) ?: true
                    val insets = window?.let { WindowCompat.getInsetsController(it, view) }
                    val restore = {
                        if (statusVisible) insets?.show(WindowInsetsCompat.Type.statusBars())
                        else insets?.hide(WindowInsetsCompat.Type.statusBars())
                        if (navigationVisible)
                            insets?.show(WindowInsetsCompat.Type.navigationBars())
                        else insets?.hide(WindowInsetsCompat.Type.navigationBars())
                        Unit
                    }
                    restoreSystemBars = restore
                    insets?.hide(WindowInsetsCompat.Type.systemBars())
                    insets?.systemBarsBehavior =
                        WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                    onDispose {
                        if (state.fullscreen) restore()
                        restoreSystemBars = null
                    }
                }
                var origin by remember { mutableStateOf(Offset.Zero) }
                val density = LocalDensity.current
                BoxWithConstraints(
                    Modifier.fillMaxSize()
                        .onGloballyPositioned { origin = it.positionInWindow() }
                        .pointerInput(state) {
                            awaitEachGesture {
                                awaitFirstDown(
                                    requireUnconsumed = false,
                                    pass = PointerEventPass.Initial,
                                )
                                state.touching = true
                                try {
                                    do {
                                        val event = awaitPointerEvent(PointerEventPass.Initial)
                                    } while (event.changes.any { it.pressed })
                                } finally {
                                    state.touching = false
                                    state.interaction++
                                }
                            }
                        }
                ) {
                    val source =
                        if (state.fullscreen) state.transitionBounds
                        else state.inlineBounds ?: state.transitionBounds
                    val progress = expansion.value
                    val width = with(density) { source?.width?.toDp() } ?: maxWidth
                    val height = with(density) { source?.height?.toDp() } ?: maxHeight
                    Box(
                        Modifier.offset {
                                IntOffset(
                                    lerp((source?.left ?: origin.x) - origin.x, 0f, progress)
                                        .roundToInt(),
                                    lerp((source?.top ?: origin.y) - origin.y, 0f, progress)
                                        .roundToInt(),
                                )
                            }
                            .requiredSize(
                                width + (maxWidth - width) * progress,
                                height + (maxHeight - height) * progress,
                            )
                    ) {
                        display(IrideMp3DisplayPresentation(progress, controlsOpacity))
                    }
                }
            }
        }
}

@Composable
private fun IrideMp3FullscreenMenuScope(content: @Composable () -> Unit) {
    val menu = remember { com.metrolist.music.ui.component.MenuState() }
    CompositionLocalProvider(com.metrolist.music.ui.component.LocalMenuState provides menu) {
        content()
        com.metrolist.music.ui.component.BottomSheetMenu(state = menu)
    }
}
