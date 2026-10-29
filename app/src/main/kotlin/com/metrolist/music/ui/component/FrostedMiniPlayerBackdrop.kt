package com.metrolist.music.ui.component

import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionOnScreen
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize

@Composable
fun FrostedPlayerNavigationBackdrop(
    collapsedHeight: Dp,
    backdrop: FrostBackdrop?,
    contentColor: Color,
    backgroundColor: Color? = null,
    modifier: Modifier = Modifier,
    surfaceAlpha: Float = IrideFolio.floatingPillSurfaceAlpha(backdrop, backgroundColor),
) {
    val dockHeight = IrideMiniPlayerHeight + FloatingBottomNavigationHeight
    val bottomSpacing = (collapsedHeight - dockHeight).coerceAtLeast(0.dp)
    val surface = IrideFolio.frostedSurfaceColor(contentColor, backgroundColor)
    var panelBounds by remember { mutableStateOf<Rect?>(null) }
    val shape = rememberDeviceMatchedBottomPanelShape(
        horizontalPadding = IrideFolio.HorizontalPadding,
        additionalBottomSpacing = 0.dp,
        topRadius = IrideFolio.ControlsCornerRadius,
        bottomSpacing = bottomSpacing,
        panelBoundsOnScreen = panelBounds,
    )
    Box(
        modifier = modifier
            .irideBottomDockBounds()
            .padding(bottom = bottomSpacing)
            .height(dockHeight)
            .onGloballyPositioned {
                panelBounds = Rect(it.positionOnScreen(), it.size.toSize())
            }
            .frostedNavigationSurface(
                backdrop = backdrop,
                surface = surface,
                shape = shape,
                surfaceAlpha = surfaceAlpha,
                observeBackdrop = true,
            )
            .border(1.dp, contentColor.copy(alpha = 0.22f), shape)
            .drawWithContent {
                drawContent()
                val inset = ((IrideMiniPlayerHeight - IrideMiniPlayerCoverSize) / 2).toPx()
                val dividerY = IrideMiniPlayerHeight.toPx()
                drawLine(
                    color = contentColor.copy(alpha = IrideFolio.MiniPlayerDividerOpacity),
                    start = Offset(inset, dividerY),
                    end = Offset(size.width - inset, dividerY),
                    strokeWidth = IrideFolio.MiniPlayerDividerStroke.toPx(),
                )
            },
    )
}
