package com.metrolist.music.ui.component

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import com.metrolist.music.R
import com.metrolist.music.constants.IrideBaseBorderWidth

/** One quiet, noninteractive playlist marker for every artwork size. */
@Composable
internal fun PlaylistArtworkBadge(modifier: Modifier = Modifier) {
    val iconPainter = painterResource(R.drawable.playlist_play)
    Canvas(modifier) {
        drawPlaylistArtworkBadgeForCover(Rect(Offset.Zero, size), iconPainter)
    }
}

internal fun Modifier.playlistArtworkBadge(iconPainter: Painter): Modifier = drawWithContent {
    drawContent()
    drawPlaylistArtworkBadgeForCover(Rect(Offset.Zero, size), iconPainter)
}

/** Artwork, borders and badges use the same cover bounds throughout collection transitions. */
internal fun DrawScope.drawPlaylistArtworkBadgeForCover(coverBounds: Rect, iconPainter: Painter?) {
    val painter = iconPainter ?: return
    if (coverBounds.isEmpty) return
    val coverSide = coverBounds.size.minDimension
    val iconSize = (coverSide * 0.12f)
        .coerceIn(IrideFolio.PlaylistBadgeMinSize.toPx(), IrideFolio.PlaylistBadgeMaxSize.toPx())
        .coerceAtMost(coverSide * 0.4f)
    val border = maxOf(IrideBaseBorderWidth.toPx(), collectionArtworkBorderWidthPx(coverBounds.size))
    val inset = (IrideFolio.PlaylistBadgeInset.toPx() + border).coerceAtMost(coverSide * 0.2f)
    val topLeft = Offset(
        x = coverBounds.right - inset - iconSize,
        y = coverBounds.bottom - inset - iconSize,
    )
    val shadowOffset = IrideFolio.PlaylistBadgeShadowOffset.toPx()

    drawBadgeIcon(
        painter = painter,
        topLeft = topLeft + Offset(shadowOffset, shadowOffset),
        size = iconSize,
        color = Color.Black.copy(alpha = IrideFolio.PlaylistBadgeShadowAlpha),
    )
    drawBadgeIcon(
        painter = painter,
        topLeft = topLeft,
        size = iconSize,
        color = Color.White.copy(alpha = IrideFolio.PlaylistBadgeWhiteAlpha),
    )
}

private fun DrawScope.drawBadgeIcon(
    painter: Painter,
    topLeft: Offset,
    size: Float,
    color: Color,
) {
    withTransform({ translate(topLeft.x, topLeft.y) }) {
        with(painter) {
            draw(
                size = Size(size, size),
                colorFilter = ColorFilter.tint(color),
            )
        }
    }
}
