package com.metrolist.music.ui.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun FrostedMiniPlayerBackdrop(
    collapsedHeight: Dp,
    backdrop: FrostBackdrop?,
    contentColor: Color,
    backgroundColor: Color? = null,
    modifier: Modifier = Modifier,
    surfaceAlpha: Float = IrideFolio.floatingPillSurfaceAlpha(backdrop, backgroundColor),
) {
    val height = (collapsedHeight - IrideFolio.MiniPlayerBackdropTopOffset).coerceAtLeast(0.dp)
    val surface = IrideFolio.frostedSurfaceColor(contentColor, backgroundColor)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .frostedNavigationSurface(
                backdrop = backdrop,
                surface = surface,
                shape = RectangleShape,
                surfaceAlpha = surfaceAlpha,
                observeBackdrop = true,
            ),
    )
}
