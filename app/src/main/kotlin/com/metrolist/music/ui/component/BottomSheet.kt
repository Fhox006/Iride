/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.component

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.DraggableState
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.input.pointer.util.addPointerInputChange
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import com.metrolist.music.constants.NavigationBarAnimationSpec
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.pow

/**
 * Bottom Sheet
 * Modified from [ViMusic](https://github.com/vfsfitvnm/ViMusic)
 */
@Composable
fun BottomSheet(
    state: BottomSheetState,
    modifier: Modifier = Modifier,
    background: @Composable (BoxScope.() -> Unit) = { },
    onDismiss: (() -> Unit)? = null,
    collapsedContent: @Composable BoxScope.() -> Unit,
    isExpandable: Boolean = true,
    clickableHeight: Dp = state.collapsedBound,
    // When false, this composable never moves/clips itself (used for the "curtain" player layer
    // that stays fixed behind the app content — the app layer does the moving instead) and does
    // not attach its own drag gesture (the caller attaches it to the visible peek content instead).
    selfPositions: Boolean = true,
    // Reserves space at the top of the expanded [content] — used in New Iride UI mode where the
    // player is a fixed full-screen layer but its content must start below AppPeekHeight (the
    // app-layer sliver that always stays visible at the top, since the player itself never moves).
    contentTopPadding: Dp = 0.dp,
    // When true, [background] is drawn fully opaque at every drag position instead of fading in
    // over the first ~10-61% of progress. Used by the New Iride UI curtain player: its collapsed
    // peek content already paints the same dark background, so the generic fade-in left a gap at
    // low progress where nothing was drawn yet — briefly showing raw black instead of the curtain's
    // own color and reading as a mismatched, separate background peeking through at the seam.
    backgroundAlwaysOpaque: Boolean = false,
    content: @Composable BoxScope.() -> Unit,
) {
    val density = LocalDensity.current

    Box(
        modifier = modifier
            .graphicsLayer {
                // background fades during about 10%-61% progress (unless backgroundAlwaysOpaque)
                alpha = if (backgroundAlwaysOpaque) {
                    1f
                } else {
                    (1.4f * (state.progress.coerceAtLeast(0.1f) - 0.1f).pow(0.5f)).coerceIn(0f, 1f)
                }
            }
            .fillMaxSize(),
        content = background
    )
    Box(
        modifier = modifier
            .fillMaxSize()
            // Use graphicsLayer for offset to ensure hardware acceleration and 120Hz support
            .graphicsLayer {
                if (selfPositions) {
                    translationY = (state.expandedBound - state.value)
                        .toPx()
                        .coerceAtLeast(0f)
                }
            }
            .pointerInput(state, isExpandable) {
                if (!isExpandable) return@pointerInput
                // Plain detectVerticalDragGestures claims the gesture at the platform's default
                // touch slop (~8dp) — a real finger's ordinary jitter during a tap on a button
                // anywhere in this full-surface Box crosses that easily, canceling the button's
                // own click mid-press (Compose cancels a pressed clickable the moment an ancestor
                // consumes a position change for that pointer) and, on release, can even read as a
                // downward fling into performFling(onDismiss) — closing the whole player from what
                // was meant to be a tap. Requiring a much larger, unambiguous vertical move before
                // this Box claims the pointer lets ordinary taps reach their target reliably, while
                // real swipes (expand/collapse/dismiss) still clear it well within a normal gesture.
                val dragSlop = 32.dp.toPx()
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val velocityTracker = VelocityTracker()
                    velocityTracker.addPointerInputChange(down)
                    var accumulatedY = 0f
                    var accumulatedX = 0f
                    var dragging = false
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                        if (!dragging) {
                            if (change.isConsumed) break
                            val delta = change.positionChange()
                            accumulatedY += delta.y
                            accumulatedX += delta.x
                            if (abs(accumulatedY) > dragSlop && abs(accumulatedY) > abs(accumulatedX)) {
                                dragging = true
                                change.consume()
                                velocityTracker.addPointerInputChange(change)
                                state.dispatchRawDelta(accumulatedY)
                            }
                            if (!change.pressed) break
                        } else {
                            velocityTracker.addPointerInputChange(change)
                            state.dispatchRawDelta(change.positionChange().y)
                            change.consume()
                            if (!change.pressed) {
                                val velocity = -velocityTracker.calculateVelocity().y
                                state.performFling(velocity, onDismiss)
                                dragging = false
                                break
                            }
                        }
                    }
                    if (dragging) {
                        // Pointer left the stream without a normal lift (e.g. another gesture
                        // took over) mid-drag — same as the old onDragCancel path.
                        state.performFling(0f, onDismiss)
                    }
                }
            }
            .graphicsLayer {
                if (selfPositions) {
                    val cornerRadius = if (!state.isExpanded) 16.dp.toPx() else 0f
                    shape = RoundedCornerShape(topStart = cornerRadius, topEnd = cornerRadius)
                    clip = true
                }
            }
    ) {
        if (!state.isCollapsed && !state.isDismissed) {
            BackHandler(onBack = state::collapseSoft)
        }

        // main content
        if (!state.isCollapsed) {
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = contentTopPadding)
                    .graphicsLayer {
                        alpha = ((state.progress - 0.15f) * 4).coerceIn(0f, 1f)
                    },
                content = content
            )
        }

        if (!state.isExpanded && (onDismiss == null || !state.isDismissed)) {
            val isSettled = remember { derivedStateOf { state.value == state.collapsedBound } }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(state.collapsedBound)
                    .then(
                        // When the sheet doesn't self-translate (curtain mode), this Box would
                        // otherwise sit at the top of the full-screen container (Box's default
                        // TopStart alignment) instead of at the bottom where the visible "gap" the
                        // app layer leaves actually is.
                        if (!selfPositions) Modifier.align(Alignment.BottomStart) else Modifier
                    ),
            ) {
                Box(
                    modifier = Modifier
                        .graphicsLayer {
                            // Matches the expanded content's own fade-in start (progress 0.15,
                            // just below) instead of the old faster 0.25 cutoff — that gap used to
                            // leave both this collapsed row AND the expanded content (including
                            // whatever queue/lyrics panel was open) partially visible and stacked
                            // on top of each other for a stretch of the drag, so shrinking the
                            // player with a panel open showed its dark background ghosting through
                            // behind the miniplayer row.
                            alpha = 1f - (state.progress / 0.15f).coerceIn(0f, 1f)
                        }
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            enabled = isSettled.value,
                            onClick = { if (isExpandable) state.expandSoft() },
                        )
                        .fillMaxWidth()
                        .height(clickableHeight),
                    content = collapsedContent,
                )
            }
        }
    }
}

@Stable
class BottomSheetState(
    draggableState: DraggableState,
    private val coroutineScope: CoroutineScope,
    private val animatable: Animatable<Dp, AnimationVector1D>,
    private val onAnchorChanged: (Int) -> Unit,
    val collapsedBound: Dp,
) : DraggableState by draggableState {
    val dismissedBound: Dp
        get() = animatable.lowerBound!!

    val expandedBound: Dp
        get() = animatable.upperBound!!

    val value by animatable.asState()

    val isDismissed by derivedStateOf {
        value == animatable.lowerBound!!
    }

    val isCollapsed by derivedStateOf {
        value == collapsedBound
    }

    val isExpanded by derivedStateOf {
        value == animatable.upperBound
    }

    val progress by derivedStateOf {
        1f - (animatable.upperBound!! - animatable.value) / (animatable.upperBound!! - collapsedBound)
    }

    // Set whenever an expand is triggered. Lets callers tell "just opened, no time to interact
    // yet" (e.g. an accidental tap while backgrounding) apart from "user has been sitting in the
    // expanded player for a while and left it open on purpose".
    var lastExpandedAtMs: Long = 0L
        private set

    fun collapse(animationSpec: AnimationSpec<Dp>) {
        onAnchorChanged(collapsedAnchor)
        coroutineScope.launch {
            animatable.animateTo(collapsedBound, animationSpec)
        }
    }

    fun expand(animationSpec: AnimationSpec<Dp>) {
        lastExpandedAtMs = android.os.SystemClock.elapsedRealtime()
        onAnchorChanged(expandedAnchor)
        coroutineScope.launch {
            animatable.animateTo(animatable.upperBound!!, animationSpec)
        }
    }

    private fun collapse() {
        collapse(spring(stiffness = Spring.StiffnessMediumLow))
    }

    private fun expand() {
        expand(spring(stiffness = Spring.StiffnessMediumLow))
    }

    fun collapseSoft() {
        collapse(spring(stiffness = Spring.StiffnessMediumLow))
    }

    fun expandSoft() {
        expand(spring(stiffness = Spring.StiffnessMediumLow))
    }

    fun dismiss() {
        onAnchorChanged(dismissedAnchor)
        coroutineScope.launch {
            animatable.animateTo(animatable.lowerBound!!)
        }
    }
    
    suspend fun dismissAndWait() {
        onAnchorChanged(dismissedAnchor)
        animatable.animateTo(animatable.lowerBound!!)
    }

    fun snapTo(value: Dp) {
        coroutineScope.launch {
            animatable.snapTo(value)
        }
    }

    fun performFling(velocity: Float, onDismiss: (() -> Unit)?) {
        if (velocity > 250) {
            expand()
        } else if (velocity < -250) {
            if (value < collapsedBound && onDismiss != null) {
                dismiss()
                onDismiss.invoke()
            } else {
                collapse()
            }
        } else {
            val l0 = dismissedBound
            val l1 = (collapsedBound - dismissedBound) / 2
            val l2 = (expandedBound - collapsedBound) / 2
            val l3 = expandedBound

            when (value) {
                in l0..l1 -> {
                    if (onDismiss != null) {
                        dismiss()
                        onDismiss.invoke()
                    } else {
                        collapse()
                    }
                }

                in l1..l2 -> collapse()
                in l2..l3 -> expand()
                else -> Unit
            }
        }
    }

    val preUpPostDownNestedScrollConnection
        get() = object : NestedScrollConnection {
            var isTopReached = false

            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (isExpanded && available.y < 0) {
                    isTopReached = false
                }

                return if (isTopReached && available.y < 0 && source == NestedScrollSource.UserInput) {
                    dispatchRawDelta(available.y)
                    available
                } else {
                    Offset.Zero
                }
            }

            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource,
            ): Offset {
                if (!isTopReached) {
                    isTopReached = consumed.y == 0f && available.y > 0
                }

                return if (isTopReached && source == NestedScrollSource.UserInput) {
                    dispatchRawDelta(available.y)
                    available
                } else {
                    Offset.Zero
                }
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                return if (isTopReached) {
                    val velocity = -available.y
                    performFling(velocity, null)

                    available
                } else {
                    Velocity.Zero
                }
            }

            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                isTopReached = false
                return Velocity.Zero
            }
        }
}

const val expandedAnchor = 2
const val collapsedAnchor = 1
const val dismissedAnchor = 0

@Composable
fun rememberBottomSheetState(
    dismissedBound: Dp,
    expandedBound: Dp,
    collapsedBound: Dp = dismissedBound,
    initialAnchor: Int = dismissedAnchor,
    // When true, an interactive drag can never pull the sheet below collapsedBound — no swipe-to-
    // dismiss-by-dragging. Used by the New Iride UI curtain player: dragging down past the collapsed
    // mini player used to shrink it away and silently stop playback, reading as "throwing the song
    // away" instead of just refusing to close further. Opening (drag up) is untouched, and dismiss()
    // can still be invoked programmatically (e.g. an explicit close action) regardless of this flag.
    preventDismissDrag: Boolean = false,
): BottomSheetState {
    val density = LocalDensity.current
    val coroutineScope = rememberCoroutineScope()

    var previousAnchor by rememberSaveable {
        mutableIntStateOf(initialAnchor)
    }

    // Never restore to full-screen expanded on cold start — nav bar must be visible at startup.
    // One-shot (plain remember, not rememberSaveable): runs once per composition lifetime, so a
    // real process cold-start still corrects, but a legitimately-expanded sheet is never clobbered.
    // The old unconditional version ran on every recomposition: it flipped expandedAnchor ->
    // collapsedAnchor the instant the user opened the player, so previousAnchor never persisted as
    // expanded during a live session — then any bounds change (fullscreen lyrics dialog hiding
    // system bars) recomputed the remember() block against the stale collapsed anchor and snapped
    // the open player shut (which in turn closed lyrics + dismissed the fullscreen dialog).
    var startupCorrected by remember { mutableStateOf(false) }
    if (!startupCorrected) {
        if (previousAnchor == expandedAnchor) {
            previousAnchor = collapsedAnchor
        }
        startupCorrected = true
    }

    val initialValue = when (previousAnchor) {
        expandedAnchor -> expandedBound
        collapsedAnchor -> collapsedBound
        else -> dismissedBound
    }

    val animatable = remember {
        Animatable(initialValue, Dp.VectorConverter)
    }

    return remember(dismissedBound, expandedBound, collapsedBound, coroutineScope) {
        val targetValue = when (previousAnchor) {
            expandedAnchor -> expandedBound
            collapsedAnchor -> collapsedBound
            dismissedAnchor -> dismissedBound
            else -> error("Unknown BottomSheet anchor")
        }

        animatable.updateBounds(dismissedBound.coerceAtMost(expandedBound), expandedBound)
        coroutineScope.launch {
            if (animatable.value <= dismissedBound) {
                animatable.snapTo(dismissedBound)
            } else if (animatable.value != targetValue) {
                animatable.snapTo(targetValue)
            }
        }

        BottomSheetState(
            draggableState = DraggableState { delta ->
                coroutineScope.launch {
                    val target = animatable.value - with(density) { delta.toDp() }
                    animatable.snapTo(if (preventDismissDrag) target.coerceAtLeast(collapsedBound) else target)
                }
            },
            onAnchorChanged = { previousAnchor = it },
            coroutineScope = coroutineScope,
            animatable = animatable,
            collapsedBound = collapsedBound
        )
    }
}
