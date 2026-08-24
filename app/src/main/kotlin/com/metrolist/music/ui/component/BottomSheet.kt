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
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
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
    selfPositions: Boolean = true,
    contentTopPadding: Dp = 0.dp,
    backgroundAlwaysOpaque: Boolean = false,
    content: @Composable BoxScope.() -> Unit,
) {
    val density = LocalDensity.current

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
        content = background
    )
    Box(
        modifier = modifier
            .fillMaxSize()
            .graphicsLayer {
                if (selfPositions) {
                    translationY = (state.expandedBound - state.value)
                        .toPx()
                        .coerceAtLeast(0f)
                }
            }
            .pointerInput(state, isExpandable) {
                if (!isExpandable) return@pointerInput
                handleBottomSheetDrag(
                    state = state,
                    dragSlopPx = 32.dp.toPx(),
                    dominanceRatio = 1f,
                    onDismiss = onDismiss,
                )
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
                        if (!selfPositions) Modifier.align(Alignment.BottomStart) else Modifier
                    ),
            ) {
                Box(
                    modifier = Modifier
                        .graphicsLayer {
                            val collapseFade = 1f - (state.progress / 0.15f).coerceIn(0f, 1f)
                            val dismissTravel = (state.collapsedBound - state.value).coerceAtLeast(0.dp)
                            val dismissRange = (state.collapsedBound - state.dismissedBound).coerceAtLeast(1.dp)
                            val dismissFade = 1f - (dismissTravel / dismissRange).coerceIn(0f, 1f)
                            alpha = collapseFade * dismissFade
                        }
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            enabled = isSettled.value,
                            onClick = { if (isExpandable) state.expandSoft() },
                        )
                        .fillMaxWidth()
                        .height(clickableHeight)
                        .pointerInput(state, isExpandable) {
                            if (!isExpandable) return@pointerInput
                            handleBottomSheetDrag(
                                state = state,
                                dragSlopPx = 12.dp.toPx(),
                                dominanceRatio = 0.6f,
                                onDismiss = onDismiss,
                            )
                        },
                    content = collapsedContent,
                )
            }
        }
    }
}

internal suspend fun PointerInputScope.handleBottomSheetDrag(
    state: BottomSheetState,
    dragSlopPx: Float,
    dominanceRatio: Float,
    onDismiss: (() -> Unit)?,
) {
    val commitPx = 28.dp.toPx()
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false)
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
                if (abs(accumulatedY) > dragSlopPx && abs(accumulatedY) >= abs(accumulatedX) * dominanceRatio) {
                    dragging = true
                    change.consume()
                    state.dispatchRawDelta(accumulatedY)
                }
                if (!change.pressed) break
            } else {
                // Fully-expanded is a hard ceiling: upward drags are swallowed so the panel
                // never grinds against its bound while an inner list scrolls.
                val delta = change.positionChange()
                if (!(state.isExpanded && delta.y < 0)) {
                    state.dispatchRawDelta(delta.y)
                }
                change.consume()
                if (!change.pressed) break
            }
        }
        // Closing needs more conviction than opening: from the expanded panel a stray
        // vertical drift (e.g. scrolling a list through its gaps) must not collapse it.
        val downCommitPx = if (state.isExpanded) commitPx * 2f else commitPx
        val direction = when {
            accumulatedY < 0f -> -1
            accumulatedY > downCommitPx -> 1
            else -> 0
        }
        if (dragging || abs(accumulatedY) > commitPx) {
            state.performFling(0f, onDismiss, if (abs(accumulatedY) > commitPx) direction else 0)
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

    fun performFling(velocity: Float, onDismiss: (() -> Unit)?, dragDirection: Int = 0) {
        when {
            dragDirection < 0 -> expand()
            dragDirection > 0 -> dismissOrCollapse(onDismiss)
            velocity > 250 -> expand()
            velocity < -250 -> dismissOrCollapse(onDismiss)
            else -> {
                val l0 = dismissedBound
                val l1 = (collapsedBound - dismissedBound) / 2
                val l2 = (expandedBound - collapsedBound) / 2
                val l3 = expandedBound

                when (value) {
                    in l0..l1 -> dismissOrCollapse(onDismiss)
                    in l1..l2 -> collapse()
                    in l2..l3 -> expand()
                    else -> Unit
                }
            }
        }
    }

    private fun dismissOrCollapse(onDismiss: (() -> Unit)?) {
        if (value < collapsedBound && onDismiss != null) {
            dismiss()
            onDismiss.invoke()
        } else {
            collapse()
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
    onAnchorPersist: (Int) -> Unit = {},
    preventDismissDrag: Boolean = false,
): BottomSheetState {
    val density = LocalDensity.current
    val coroutineScope = rememberCoroutineScope()

    val initialValue = when (initialAnchor) {
        expandedAnchor -> expandedBound
        collapsedAnchor -> collapsedBound
        else -> dismissedBound
    }

    val animatable = remember(initialAnchor, dismissedBound, expandedBound, collapsedBound) {
        Animatable(initialValue, Dp.VectorConverter)
    }

    return remember(initialAnchor, dismissedBound, expandedBound, collapsedBound, coroutineScope) {
        val targetValue = when (initialAnchor) {
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
            onAnchorChanged = onAnchorPersist,
            coroutineScope = coroutineScope,
            animatable = animatable,
            collapsedBound = collapsedBound
        )
    }
}
