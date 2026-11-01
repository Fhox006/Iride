package com.metrolist.music.ui.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.border
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionOnScreen
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

internal fun iridePlayerExpansionPhase(progress: Float): Float {
    val phase = ((progress - 0.5f) * 2f).coerceIn(0f, 1f)
    return phase * phase * (3f - 2f * phase)
}

@Composable
fun FrostedPlayerNavigationBackdrop(
    collapsedHeight: Dp,
    panelHeight: Dp = collapsedHeight,
    expansion: Float = 0f,
    backdrop: FrostBackdrop?,
    contentColor: Color,
    backgroundColor: Color? = null,
    modifier: Modifier = Modifier,
    surfaceAlpha: Float = IrideFolio.floatingPillSurfaceAlpha(backdrop, backgroundColor),
    dividerAlpha: Float = 1f - iridePlayerExpansionPhase(expansion),
    playerVisibility: Float = 1f,
) {
    val collapsedDockHeight = FloatingBottomNavigationHeight +
        (IrideMiniPlayerHeight + IrideMiniPlayerNavigationSpacing) * playerVisibility
    val progress = iridePlayerExpansionPhase(expansion)
    val collapsedBottomSpacing = (collapsedHeight - collapsedDockHeight).coerceAtLeast(0.dp)
    val bottomSpacing = collapsedBottomSpacing * (1f - progress)
    val dockHeight = (panelHeight - bottomSpacing).coerceAtLeast(collapsedDockHeight)
    val horizontalInset = irideBottomDockEdgeInset() * (1f - progress)
    val darkening = (expansion / 0.45f).coerceIn(0f, 1f)
    val finish = darkening * darkening * (3f - 2f * darkening)
    val surface = lerp(IrideFolio.frostedSurfaceColor(contentColor, backgroundColor), Color(0xFF080809), finish)
    val density = LocalDensity.current
    val insetPx = with(density) { horizontalInset.toPx() }
    val bottomSpacingPx = with(density) { bottomSpacing.toPx() }
    val dockHeightPx = with(density) { dockHeight.toPx() }
    var containerBounds by remember { mutableStateOf<Rect?>(null) }
    val panelBounds = containerBounds?.let { bounds ->
        val bottom = bounds.bottom - bottomSpacingPx
        Rect(bounds.left + insetPx, bottom - dockHeightPx, bounds.right - insetPx, bottom)
    }
    val shape = rememberDeviceMatchedBottomPanelShape(
        horizontalPadding = horizontalInset,
        additionalBottomSpacing = 0.dp,
        topRadius = IrideFolio.ControlsCornerRadius + (32.dp - IrideFolio.ControlsCornerRadius) * progress,
        bottomSpacing = bottomSpacing,
        panelBoundsOnScreen = panelBounds,
        simpleOutline = true,
    )
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = horizontalInset, end = horizontalInset, bottom = bottomSpacing)
            .height(dockHeight)
            .onGloballyPositioned { coordinates ->
                val position = coordinates.positionOnScreen()
                val bounds = Rect(
                    position.x - insetPx,
                    0f,
                    position.x + coordinates.size.width + insetPx,
                    position.y + coordinates.size.height + bottomSpacingPx,
                )
                val previous = containerBounds
                if (previous == null ||
                    kotlin.math.abs(previous.left - bounds.left) > 1f ||
                    kotlin.math.abs(previous.right - bounds.right) > 1f ||
                    kotlin.math.abs(previous.bottom - bounds.bottom) > 1f
                ) containerBounds = bounds
            }
            .frostedNavigationSurface(
                backdrop = backdrop,
                surface = surface,
                shape = shape,
                surfaceAlpha = surfaceAlpha + (1f - surfaceAlpha) * finish,
                backdropOpacity = 1f,
                observeBackdrop = true,
            )
            .border(1.dp, contentColor.copy(alpha = 0.22f * (1f - progress)), shape)
            .drawWithContent {
                drawContent()
                val inset = ((IrideMiniPlayerHeight - IrideMiniPlayerCoverSize) / 2).toPx() * playerVisibility
                val dividerY = (dockHeight - FloatingBottomNavigationHeight).toPx()
                drawLine(
                    color = contentColor.copy(alpha = IrideFolio.MiniPlayerDividerOpacity * dividerAlpha.coerceIn(0f, 1f)),
                    start = Offset(inset, dividerY),
                    end = Offset(size.width - inset, dividerY),
                    strokeWidth = IrideFolio.MiniPlayerDividerStroke.toPx(),
                )
            },
    )
}
