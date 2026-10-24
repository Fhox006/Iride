package com.metrolist.music.ui.theme

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.DrawStyle
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import sv.lib.squircleshape.SquircleShape

private const val FolioCornerSmoothing = 0.6f

/** Shared continuous corners for surfaces, clipping, borders and Material defaults. */
fun FolioShape(size: Dp): SquircleShape =
    SquircleShape(radius = size, cornerSmoothing = FolioCornerSmoothing)

fun FolioShape(percent: Int): SquircleShape =
    // Leave space for smoothing: a half-height radius would turn back into a circle.
    SquircleShape(percent = percent.coerceAtMost(32), cornerSmoothing = FolioCornerSmoothing)

fun FolioShape(
    topStart: Dp = 0.dp,
    topEnd: Dp = 0.dp,
    bottomEnd: Dp = 0.dp,
    bottomStart: Dp = 0.dp,
): SquircleShape = SquircleShape(
    topStart = topStart,
    topEnd = topEnd,
    bottomEnd = bottomEnd,
    bottomStart = bottomStart,
    cornerSmoothing = FolioCornerSmoothing,
)

/** Canvas indicators use the same outline as composable surfaces. */
internal fun DrawScope.drawFolioRoundRect(
    color: Color,
    topLeft: Offset = Offset.Zero,
    size: Size = Size(this.size.width - topLeft.x, this.size.height - topLeft.y),
    cornerRadius: CornerRadius = CornerRadius.Zero,
    alpha: Float = 1f,
    style: DrawStyle = Fill,
) {
    if (size.width <= 0f || size.height <= 0f) return
    val radius = minOf(cornerRadius.x, cornerRadius.y, size.minDimension * 0.32f)
    val outline = FolioShape(radius.toDp()).createOutline(size, layoutDirection, this)
    translate(topLeft.x, topLeft.y) {
        drawOutline(outline, color, alpha, style)
    }
}
