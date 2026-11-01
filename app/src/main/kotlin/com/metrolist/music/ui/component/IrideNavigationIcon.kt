package com.metrolist.music.ui.component

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.withTransform
import com.metrolist.music.ui.screens.Screens
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.sqrt

/** Fixed glyph anchors; the shared indicator changes ink without moving or activating a glyph. */
@Composable
internal fun IrideNavigationIcon(
    screen: Screens,
    selected: Boolean,
    reducedMotion: Boolean,
    color: Color,
    maskStart: Float,
    maskEnd: Float,
    modifier: Modifier = Modifier,
) {
    val selection = animateFloatAsState(
        targetValue = if (selected) 1f else 0f,
        animationSpec = if (reducedMotion) snap() else tween(
            durationMillis = if (selected) {
                IrideFolio.NavigationSelectionDurationMillis
            } else {
                IrideFolio.NavigationReturnDurationMillis
            },
            easing = IrideFolio.NavigationMotionEasing,
        ),
        label = "irideNavigationGlyph:" + screen.route,
    )
    val turn = if (screen == Screens.Search) remember { Animatable(1f) } else null
    LaunchedEffect(selected, reducedMotion) {
        turn ?: return@LaunchedEffect
        if (reducedMotion) {
            turn.snapTo(1f)
        } else if (selected) {
            // Continue from an interrupted flip rather than snapping the lens to its first frame.
            turn.animateTo(
                ceil(turn.value.toDouble()).toFloat() + 1f,
                tween(IrideFolio.NavigationTurnDurationMillis, easing = IrideFolio.NavigationMotionEasing),
            )
            turn.snapTo(1f)
        } else if (turn.value % 1f != 0f) {
            turn.animateTo(
                ceil(turn.value.toDouble()).toFloat(),
                tween(IrideFolio.NavigationReturnDurationMillis, easing = IrideFolio.NavigationMotionEasing),
            )
            turn.snapTo(1f)
        }
    }
    Box(
        modifier.drawWithCache {
            val iconSize = IrideFolio.NavigationIconSize.toPx()
            val origin = Offset((size.width - iconSize) / 2f, (size.height - iconSize) / 2f)
            val scale = iconSize / 24f
            val stroke = IrideFolio.NavigationIconStroke
            val glyph = when (screen) {
                Screens.Home -> HomeGlyph(stroke)
                Screens.Library -> LibraryGlyph(stroke)
                Screens.Account -> AccountGlyph(stroke)
                Screens.Search -> SearchGlyph(stroke)
                else -> null
            }
            onDrawBehind {
                val progress = if (reducedMotion) {
                    if (selected) 1f else 0f
                } else {
                    selection.value.coerceIn(0f, 1f)
                }
                val phase = if (reducedMotion) 0f else (turn?.value ?: 1f) % 1f
                fun drawGlyph(ink: Color) {
                    withTransform({
                        translate(origin.x, origin.y)
                        scale(scale, scale, pivot = Offset.Zero)
                    }) {
                        glyph?.draw(this, ink, progress, phase)
                    }
                }
                if (screen == Screens.Search) {
                    drawGlyph(color.copy(alpha = color.alpha * (0.64f + 0.36f * progress)))
                } else {
                    val muted = color.copy(alpha = color.alpha * 0.64f)
                    if (maskEnd > maskStart) {
                        // Disjoint clips: a moving mask never paints the same antialiased edge twice.
                        if (maskStart > 0f) clipRect(right = maskStart) { drawGlyph(muted) }
                        if (maskEnd < size.width) clipRect(left = maskEnd) { drawGlyph(muted) }
                        clipRect(left = maskStart, right = maskEnd) { drawGlyph(color) }
                    } else {
                        drawGlyph(muted)
                    }
                }
            }
        },
    )
}

private interface NavigationGlyph {
    fun draw(scope: DrawScope, ink: Color, selection: Float, turn: Float)
}

private fun glyphPath(geometry: Path.() -> Unit) = Path().apply(geometry)

private fun rectangle(left: Float, top: Float, right: Float, bottom: Float) = glyphPath {
    moveTo(left, top)
    lineTo(right, top)
    lineTo(right, bottom)
    lineTo(left, bottom)
    close()
}

/** Filled contours, rather than adjoining stroked segments, give joins one continuous boundary. */
private class GlyphSurface(val filled: Path, interior: Path) {
    private val outline = Path.combine(PathOperation.Difference, filled, interior)
    private val blendPaint = Paint()

    fun draw(scope: DrawScope, ink: Color, fill: Float) = with(scope) {
        when {
            fill <= 0f -> drawPath(outline, ink)
            fill >= 1f -> drawPath(filled, ink)
            else -> {
                // Add premultiplied colors in an isolated layer: shared edges retain their opacity
                // throughout the outline-to-fill transition, without a seam or a double contour.
                drawContext.canvas.saveLayer(Rect(0f, 0f, 24f, 24f), blendPaint)
                drawPath(outline, ink, alpha = 1f - fill)
                drawPath(filled, ink, alpha = fill, blendMode = BlendMode.Plus)
                drawContext.canvas.restore()
            }
        }
    }
}

private class HomeGlyph(stroke: Float) : NavigationGlyph {
    private val outer = glyphPath {
        moveTo(3f, 12f)
        lineTo(12f, 3f)
        lineTo(21f, 12f)
        lineTo(21f, 21f)
        lineTo(3f, 21f)
        close()
    }
    private val roofInset = stroke * sqrt(2f)
    private val inner = glyphPath {
        moveTo(3f + stroke, 12f + roofInset - stroke)
        lineTo(12f, 3f + roofInset)
        lineTo(21f - stroke, 12f + roofInset - stroke)
        lineTo(21f - stroke, 21f - stroke)
        lineTo(3f + stroke, 21f - stroke)
        close()
    }
    private val doorway = rectangle(10f, 14f, 14f, 22f)
    private val outline = Path.combine(PathOperation.Difference, outer, inner)
    private val filled = Path.combine(PathOperation.Difference, outer, doorway)
    private val blendPaint = Paint()

    override fun draw(scope: DrawScope, ink: Color, selection: Float, turn: Float) = with(scope) {
        when {
            selection <= 0f -> drawPath(outline, ink)
            selection >= 1f -> drawPath(filled, ink)
            else -> {
                drawContext.canvas.saveLayer(Rect(0f, 0f, 24f, 24f), blendPaint)
                drawPath(outline, ink, alpha = 1f - selection)
                drawPath(filled, ink, alpha = selection, blendMode = BlendMode.Plus)
                drawContext.canvas.restore()
            }
        }
    }
}

private fun bookContour(left: Float, top: Float, right: Float, bottom: Float, radius: Float) = glyphPath {
    moveTo(left, bottom)
    lineTo(left, top + radius)
    quadraticTo(left, top, left + radius, top)
    lineTo(right - radius, top)
    quadraticTo(right, top, right, top + radius)
    lineTo(right, bottom)
    close()
}

private fun bookSurface(left: Float, top: Float, right: Float, bottom: Float, stroke: Float) = GlyphSurface(
    filled = bookContour(left, top, right, bottom, radius = 1f),
    interior = bookContour(left + stroke, top + stroke, right - stroke, bottom - stroke, radius = 0.25f),
)

private class LibraryGlyph(private val stroke: Float) : NavigationGlyph {
    private val first = bookSurface(3.9f, 3f, 7.9f, 21f, stroke)
    private val last = bookSurface(17.1f, 4f, 21.1f, 20f, stroke)
    private var middle = bookSurface(9.9f, 4f, 13.9f, 20f, stroke)
    private var previousSelection = 0f

    override fun draw(scope: DrawScope, ink: Color, selection: Float, turn: Float) {
        if (selection != previousSelection) {
            middle = bookSurface(9.9f, 4f - selection, 13.9f, 20f + selection, stroke)
            previousSelection = selection
        }
        first.draw(scope, ink, selection)
        middle.draw(scope, ink, selection)
        // The first book is fixed; the other books keep their own centers throughout the motion.
        with(scope) {
            withTransform({ rotate(16f * selection, pivot = Offset(19.1f, 12f)) }) {
                last.draw(this, ink, selection)
            }
        }
    }
}

private class AccountGlyph(stroke: Float) : NavigationGlyph {
    private val head = GlyphSurface(
        rectangle(9f, 3.5f, 15f, 9.5f),
        rectangle(9f + stroke, 3.5f + stroke, 15f - stroke, 9.5f - stroke),
    )
    private val body = GlyphSurface(
        filled = glyphPath {
            moveTo(3f, 21f)
            cubicTo(3f, 16.7f, 7f, 13.5f, 12f, 13.5f)
            cubicTo(17f, 13.5f, 21f, 16.7f, 21f, 21f)
            close()
        },
        interior = glyphPath {
            moveTo(3f + stroke, 21f - stroke)
            cubicTo(5.2f, 17f, 8.2f, 13.5f + stroke, 12f, 13.5f + stroke)
            cubicTo(15.8f, 13.5f + stroke, 18.8f, 17f, 21f - stroke, 21f - stroke)
            close()
        },
    )

    override fun draw(scope: DrawScope, ink: Color, selection: Float, turn: Float) {
        body.draw(scope, ink, selection)
        with(scope) {
            // At every angle the head remains above y = 10.75; the fixed shoulders start at 13.5.
            withTransform({ rotate(45f * selection, pivot = Offset(12f, 6.5f)) }) {
                head.draw(this, ink, selection)
            }
        }
    }
}

/** Orthographic rotation around the diagonal stem: the lens turns in place and the stem stays put. */
private fun lensContour(radius: Float, aspect: Float): Path {
    val center = 9.75f
    val control = radius * 0.55228475f
    fun x(u: Float, v: Float) = center + (u + v + aspect * (u - v)) / 2f
    fun y(u: Float, v: Float) = center + (u + v - aspect * (u - v)) / 2f
    return glyphPath {
        fun segment(u1: Float, v1: Float, u2: Float, v2: Float, u3: Float, v3: Float) {
            cubicTo(x(u1, v1), y(u1, v1), x(u2, v2), y(u2, v2), x(u3, v3), y(u3, v3))
        }
        moveTo(x(0f, -radius), y(0f, -radius))
        segment(control, -radius, radius, -control, radius, 0f)
        segment(radius, control, control, radius, 0f, radius)
        segment(-control, radius, -radius, control, -radius, 0f)
        segment(-radius, -control, -control, -radius, 0f, -radius)
        close()
    }
}

private class SearchGlyph(private val stroke: Float) : NavigationGlyph {
    private val halfWidth = stroke / (2f * sqrt(2f))
    private val stem = glyphPath {
        moveTo(14.2f - halfWidth, 14.2f + halfWidth)
        lineTo(20.7f - halfWidth, 20.7f + halfWidth)
        lineTo(20.7f + halfWidth, 20.7f - halfWidth)
        lineTo(14.2f + halfWidth, 14.2f - halfWidth)
        close()
    }
    private fun surface(aspect: Float) = GlyphSurface(
        Path.combine(PathOperation.Union, lensContour(7f, aspect), stem),
        lensContour(7f - stroke, aspect),
    )
    private var previousAspect = 1f
    private var lens = surface(previousAspect)

    override fun draw(scope: DrawScope, ink: Color, selection: Float, turn: Float) {
        // A very thin edge remains visible at quarter turns, like a physical lens seen edge-on.
        val aspect = abs(cos(2.0 * PI * turn)).toFloat().coerceAtLeast(0.04f)
        if (aspect != previousAspect) {
            lens = surface(aspect)
            previousAspect = aspect
        }
        lens.draw(scope, ink, selection)
    }
}
