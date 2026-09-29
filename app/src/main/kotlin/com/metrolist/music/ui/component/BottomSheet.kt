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
import androidx.compose.runtime.rememberUpdatedState
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sign

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
    collapsedClickEnabled: Boolean = true,
    // Kept for callers that position the sheet externally.
    selfPositions: Boolean = true,
    // Reserves space above the expanded content without changing its layout.
    contentTopPadding: Dp = 0.dp,
    // Legacy background behavior for sheets that draw their background outside the moving content.
    backgroundAlwaysOpaque: Boolean = false,
    // The player panel moves over the app, so its background must travel with its content.
    backgroundFollowsContent: Boolean = false,
    content: @Composable BoxScope.() -> Unit,
) {
    if (!backgroundFollowsContent) {
        Box(
            modifier = modifier
                .graphicsLayer {
                    alpha = if (backgroundAlwaysOpaque) {
                        1f
                    } else {
                        (1.4f * (state.progress.coerceAtLeast(0.1f) - 0.1f).pow(0.5f)).coerceIn(0f, 1f)
                    }
                }
                .fillMaxSize(),
            content = background,
        )
    }
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
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    // The sheet is translated inside this full-screen layer. Convert the down
                    // point back to window coordinates so the guard stays aligned with the
                    // system navigation handle while the sheet is collapsed.
                    val densityScale = density
                    val screenStartY =
                        (down.position.y + (state.expandedBound - state.value).toPx()) / densityScale
                    val sideDragSlop = if (backgroundFollowsContent && state.isCollapsed) {
                        viewConfiguration.touchSlop
                    } else {
                        32.dp.toPx()
                    }
                    val dragSlop = bottomSheetDragSlop(
                        startYDp = screenStartY,
                        screenHeightDp = size.height / densityScale,
                        sideDragSlopPx = sideDragSlop,
                        densityScale = densityScale,
                    )
                    val velocityTracker = VelocityTracker()
                    velocityTracker.addPointerInputChange(down)
                    var accumulatedY = 0f
                    var accumulatedX = 0f
                    var dragging = false
                    try {
                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == down.id } ?: break
                            velocityTracker.addPointerInputChange(change)
                            if (!dragging) {
                                if (change.isConsumed || !change.pressed) break
                                val delta = change.positionChange()
                                accumulatedY += delta.y
                                accumulatedX += delta.x
                                if (isVerticalDragPastSlop(accumulatedY, accumulatedX, dragSlop)) {
                                    dragging = true
                                    change.consume()
                                    state.dispatchRawDelta(accumulatedY - sign(accumulatedY) * dragSlop)
                                }
                            } else {
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
                    } finally {
                        if (dragging) state.performFling(0f, onDismiss)
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
        if (backgroundFollowsContent) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = (state.progress / 0.25f).coerceIn(0f, 1f) },
                content = background,
            )
        }
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
                        // Externally positioned sheets need their collapsed row at the bottom.
                        if (!selfPositions) Modifier.align(Alignment.BottomStart) else Modifier
                    ),
            ) {
                Box(
                    modifier = Modifier
                        .graphicsLayer {
                            // The moving panel fades its background in as the mini row fades out.
                            alpha = 1f - (state.progress / (if (backgroundFollowsContent) 0.25f else 0.15f))
                                .coerceIn(0f, 1f)
                        }
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            enabled = collapsedClickEnabled && isSettled.value,
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

    private var dragValue by mutableStateOf<Dp?>(null)
    private var animationJob: Job? = null

    val value: Dp
        get() = dragValue ?: animatable.value

    internal fun dragBy(delta: Dp, preventDismissDrag: Boolean) {
        val currentValue = value
        animationJob?.cancel()
        val lowerBound = if (preventDismissDrag) collapsedBound else dismissedBound
        dragValue = (currentValue - delta).coerceIn(lowerBound, expandedBound)
    }

    private suspend fun animateTo(target: Dp, animationSpec: AnimationSpec<Dp>) {
        // Transfer the last synchronous drag position before exposing animated values again.
        dragValue?.let { animatable.snapTo(it) }
        dragValue = null
        animatable.animateTo(target, animationSpec)
    }

    private fun launchAnimation(target: Dp, animationSpec: AnimationSpec<Dp>) {
        animationJob?.cancel()
        animationJob = coroutineScope.launch { animateTo(target, animationSpec) }
    }

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
        val range = expandedBound - collapsedBound
        if (range > 0.dp) (value - collapsedBound) / range else 0f
    }

    // Set whenever an expand is triggered. Lets callers tell "just opened, no time to interact
    // yet" (e.g. an accidental tap while backgrounding) apart from "user has been sitting in the
    // expanded player for a while and left it open on purpose".
    var lastExpandedAtMs: Long = 0L
        private set

    fun collapse(animationSpec: AnimationSpec<Dp>) {
        onAnchorChanged(collapsedAnchor)
        launchAnimation(collapsedBound, animationSpec)
    }

    fun expand(animationSpec: AnimationSpec<Dp>) {
        lastExpandedAtMs = android.os.SystemClock.elapsedRealtime()
        onAnchorChanged(expandedAnchor)
        launchAnimation(expandedBound, animationSpec)
    }

    private fun collapse() {
        collapse(spring(stiffness = PlayerSheetSettleStiffness))
    }

    private fun expand() {
        expand(spring(stiffness = PlayerSheetSettleStiffness))
    }

    fun collapseSoft() {
        collapse(spring(stiffness = PlayerSheetSettleStiffness))
    }

    fun expandSoft() {
        expand(spring(stiffness = PlayerSheetSettleStiffness))
    }

    fun dismiss() {
        onAnchorChanged(dismissedAnchor)
        launchAnimation(dismissedBound, spring())
    }

    suspend fun dismissAndWait() {
        onAnchorChanged(dismissedAnchor)
        coroutineScope {
            animationJob?.cancel()
            val job = launch { animateTo(dismissedBound, spring()) }
            animationJob = job
            job.join()
        }
    }

    fun snapTo(value: Dp) {
        animationJob?.cancel()
        animationJob = coroutineScope.launch {
            animatable.snapTo(value)
            dragValue = null
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
            val l1 = dismissedBound + (collapsedBound - dismissedBound) / 2
            val l2 = collapsedBound + (expandedBound - collapsedBound) / 2
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

private val PlayerSheetSettleStiffness = Spring.StiffnessMediumLow * 0.8f

@Composable
fun rememberBottomSheetState(
    dismissedBound: Dp,
    expandedBound: Dp,
    collapsedBound: Dp = dismissedBound,
    initialAnchor: Int = dismissedAnchor,
    // Fires on every transition between dismissed/collapsed/expanded so the caller can mirror the
    // new anchor into DataStore and survive a real process kill (rememberSaveable alone only
    // covers config-change recreation). Default no-op keeps this composable drop-in for callers
    // that don't need cross-process persistence.
    onAnchorPersist: (Int) -> Unit = {},
    // When true, an interactive drag can never pull the sheet below collapsedBound — no swipe-to-
    // dismiss-by-dragging. Used by the New Iride UI curtain player: dragging down past the collapsed
    // mini player used to shrink it away and silently stop playback, reading as "throwing the song
    // away" instead of just refusing to close further. Opening (drag up) is untouched, and dismiss()
    // can still be invoked programmatically (e.g. an explicit close action) regardless of this flag.
    preventDismissDrag: Boolean = false,
): BottomSheetState {
    val density = LocalDensity.current
    val coroutineScope = rememberCoroutineScope()
    var anchor by rememberSaveable { mutableIntStateOf(initialAnchor) }
    val currentOnAnchorPersist by rememberUpdatedState(onAnchorPersist)

    val initialValue = when (anchor) {
        expandedAnchor -> expandedBound
        collapsedAnchor -> collapsedBound
        dismissedAnchor -> dismissedBound
        else -> error("Unknown BottomSheet anchor")
    }

    // Persisting a new target must not recreate the animation at its destination.
    // Recreate only for new bounds, restoring the last requested anchor in that layout.
    val animatable = remember(dismissedBound, expandedBound, collapsedBound, density) {
        Animatable(initialValue, Dp.VectorConverter)
    }

    return remember(animatable, coroutineScope, density, preventDismissDrag) {
        animatable.updateBounds(dismissedBound.coerceAtMost(expandedBound), expandedBound)
        lateinit var state: BottomSheetState
        state = BottomSheetState(
            draggableState = DraggableState { delta ->
                state.dragBy(with(density) { delta.toDp() }, preventDismissDrag)
            },
            onAnchorChanged = {
                anchor = it
                currentOnAnchorPersist(it)
            },
            coroutineScope = coroutineScope,
            animatable = animatable,
            collapsedBound = collapsedBound,
        )
        state
    }
}

internal fun bottomSheetDragSlop(
    startYDp: Float,
    screenHeightDp: Float,
    sideDragSlopPx: Float,
    densityScale: Float,
): Float {
    val isBottomGuardStart = startYDp >= screenHeightDp - BottomCenterGuardHeight

    return if (isBottomGuardStart) BottomGuardDragSlop * densityScale else sideDragSlopPx
}

internal fun isVerticalDragPastSlop(accumulatedY: Float, accumulatedX: Float, dragSlop: Float): Boolean =
    abs(accumulatedY) > dragSlop && abs(accumulatedY) > abs(accumulatedX)

private const val BottomCenterGuardHeight = 80f
private const val BottomGuardDragSlop = 56f
