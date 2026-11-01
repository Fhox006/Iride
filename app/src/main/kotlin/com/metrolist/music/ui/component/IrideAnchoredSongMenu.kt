package com.metrolist.music.ui.component

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import com.metrolist.music.LocalPlayerAwareWindowInsets
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import com.metrolist.music.ui.theme.textPrimary
import com.metrolist.music.ui.utils.rememberReducedMotion
import kotlin.math.roundToInt

internal val LocalSongMenuFooterHeight = staticCompositionLocalOf { 0.dp }

private class SongMenuAnchor {
    var bounds: Rect? = null
    var origin = Offset.Zero
}

@Composable
internal fun SongMenuIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable () -> Unit,
) {
    val state = LocalMenuState.current
    val anchor = remember { SongMenuAnchor() }
    IrideFolioMenuButton(
        expanded = state.isVisible && state.anchored && state.popupAnchor == anchor.bounds,
        onClick = {
            state.pointerAnchor = anchor.bounds
            onClick()
        },
        modifier = modifier.onGloballyPositioned { anchor.bounds = it.boundsInWindow() },
        enabled = enabled,
        content = content,
    )
}

/** Observe without consuming: list clicks, long presses and scrolling retain their gestures. */
internal fun Modifier.observeSongMenuAnchor(state: MenuState): Modifier = composed {
    val anchor = remember { SongMenuAnchor() }
    val radius = with(LocalDensity.current) { 24.dp.toPx() }
    onGloballyPositioned { anchor.origin = it.positionInWindow() }
        .pointerInput(state, radius) {
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                val point = anchor.origin + down.position
                state.pointerAnchor = Rect(
                    point.x - radius, point.y - radius,
                    point.x + radius, point.y + radius,
                )
            }
        }
}

@Composable
internal fun IrideAnchoredSongMenu(
    state: MenuState,
    contentHeight: Int,
    anchorBounds: Rect? = state.popupAnchor,
    onDismissRequest: () -> Unit = state::dismiss,
    focusable: Boolean = true,
    content: @Composable ColumnScope.(constrained: Boolean) -> Unit,
) {
    val density = LocalDensity.current
    val direction = LocalLayoutDirection.current
    val windowSize = LocalWindowInfo.current.containerSize
    val insets = WindowInsets.safeDrawing
    val margin = with(density) { IrideFolio.MenuViewportVerticalMargin.roundToPx() }
    val gap = with(density) { IrideFolio.MenuOffset.y.roundToPx() }
    val safeBounds = IntRect(
        left = insets.getLeft(density, direction),
        top = insets.getTop(density),
        right = windowSize.width - insets.getRight(density, direction),
        bottom = windowSize.height - insets.getBottom(density),
    )
    val controlsBottom = with(density) {
        (iridePageStatusBarTopPadding() + IrideFolio.ControlsTopSpacing + IrideFolio.ControlsHeight).roundToPx()
    }
    val pageFooter = LocalPlayerAwareWindowInsets.current.asPaddingValues().calculateBottomPadding()
    val footerHeight = with(density) { maxOf(LocalSongMenuFooterHeight.current, pageFooter).roundToPx() }
    val viewport = songMenuViewport(
        safeBounds = safeBounds,
        controlsBottom = controlsBottom,
        footerTop = windowSize.height - footerHeight,
        margin = margin,
        horizontalMargin = with(density) { IrideFolio.HorizontalPadding.roundToPx() },
    )
    if (viewport.width <= 0 || viewport.height <= 0) return
    val anchor = anchorBounds?.let {
        IntRect(it.left.roundToInt(), it.top.roundToInt(), it.right.roundToInt(), it.bottom.roundToInt())
    }
    val menuAnchor = anchor ?: IntRect(viewport.right, viewport.bottom, viewport.right, viewport.bottom)
    val menuWidth = with(density) { IrideFolio.MenuWidth.roundToPx() }.coerceAtMost(viewport.width)
    val placement = songMenuPlacement(menuAnchor, viewport, IntSize(menuWidth, contentHeight), gap, direction)
    val motion = state.popupMotion
    val reducedMotion = rememberReducedMotion()
    val blurRadius = with(density) { IrideFolioContextMotionSpec.PanelBlurRadius.toPx() }
    val blurEffects = remember(blurRadius) { ArtistPhotoBlurEffects(blurRadius, TileMode.Decal) }
    // Transparent space belongs to the filtered layer, so the silhouette can soften outward.
    val blurBleed = IrideFolioContextMotionSpec.PanelBlurRadius * 3f
    val blurBleedPx = with(density) { blurBleed.roundToPx() }
    val origin = motion?.touchPoint ?: Offset(menuAnchor.center.x.toFloat(), menuAnchor.center.y.toFloat())
    val maximumTravel = with(density) { IrideFolioContextMotionSpec.PanelMaximumTravel.toPx() }
    val popupBounds = remember(menuAnchor, viewport, gap) { mutableStateOf<IntRect?>(null) }
    val positionProvider = remember(menuAnchor, viewport, gap, popupBounds, blurBleedPx) {
        object : PopupPositionProvider {
            override fun calculatePosition(
                anchorBounds: IntRect,
                windowSize: IntSize,
                layoutDirection: LayoutDirection,
                popupContentSize: IntSize,
            ): IntOffset {
                val surfaceSize = IntSize(
                    (popupContentSize.width - blurBleedPx * 2).coerceAtLeast(1),
                    (popupContentSize.height - blurBleedPx * 2).coerceAtLeast(1),
                )
                val position = songMenuPosition(
                    anchor = menuAnchor,
                    viewport = viewport,
                    menuSize = surfaceSize,
                    gap = gap,
                    layoutDirection = layoutDirection,
                )
                popupBounds.value = IntRect(position, surfaceSize)
                return position - IntOffset(blurBleedPx, blurBleedPx)
            }
        }
    }
    Popup(
        popupPositionProvider = positionProvider,
        onDismissRequest = onDismissRequest,
        properties = PopupProperties(focusable = focusable),
    ) {
        CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.textPrimary) {
            Box(
                Modifier
                    .graphicsLayer {
                        val bounds = popupBounds.value
                            ?: IntRect(placement.position, IntSize(menuWidth, placement.height))
                        val left = bounds.left - blurBleedPx
                        val top = bounds.top - blurBleedPx
                        val width = bounds.width + blurBleedPx * 2
                        val height = bounds.height + blurBleedPx * 2
                        val pivotX = ((origin.x - left) / width.coerceAtLeast(1)).coerceIn(0f, 1f)
                        val pivotY = ((origin.y - top) / height.coerceAtLeast(1)).coerceIn(0f, 1f)
                        val travelX = (origin.x - left - width * pivotX).coerceIn(-maximumTravel, maximumTravel)
                        val travelY = (origin.y - top - height * pivotY).coerceIn(-maximumTravel, maximumTravel)
                        val reveal = motion?.frame?.value?.reveal ?: 1f
                        val startScale = IrideFolioContextMotionSpec.PanelInitialScale
                        val scale = if (reducedMotion) 1f else startScale + (1f - startScale) * reveal
                        scaleX = scale
                        scaleY = scale
                        transformOrigin = TransformOrigin(pivotX, pivotY)
                        translationX = if (reducedMotion) 0f else travelX * (1f - reveal)
                        translationY = if (reducedMotion) 0f else travelY * (1f - reveal)
                        // Filter the already composited surface, including its border and silhouette.
                        renderEffect = if (reducedMotion) null else blurEffects.at(
                            IrideFolioContextMotionSpec.panelBlurProgress(reveal),
                        )
                        clip = false
                        alpha = IrideFolioContextMotionSpec.panelOpacity(reveal)
                    }
                    .pointerInput(state.isVisible, motion) {
                        if (motion != null && !state.isVisible) {
                            awaitPointerEventScope {
                                while (true) {
                                    awaitPointerEvent(PointerEventPass.Initial).changes.forEach { it.consume() }
                                }
                            }
                        }
                    }
                    .padding(blurBleed),
            ) {
                Column(
                    Modifier
                        .width(with(density) { viewport.width.toDp() }.coerceAtMost(IrideFolio.MenuWidth))
                        .heightIn(max = with(density) { placement.height.toDp() })
                        .onGloballyPositioned { if (it.size.width > 0 && it.size.height > 0) motion?.markPanelReady() }
                        .irideFolioMenuSurface(),
                ) { content(placement.constrained) }
            }
        }
    }
}
