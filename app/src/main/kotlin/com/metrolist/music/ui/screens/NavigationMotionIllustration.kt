package com.metrolist.music.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.metrolist.music.R
import com.metrolist.music.ui.component.iridePageTitleTextStyle
import com.metrolist.music.ui.theme.textPrimary
import com.metrolist.music.ui.theme.FolioShape
import com.metrolist.music.ui.theme.drawFolioRoundRect
import com.metrolist.music.ui.utils.rememberReducedMotion
import kotlinx.coroutines.isActive
import kotlin.math.PI
import kotlin.math.sin
import kotlin.math.cos

internal enum class NavigationDemo { Welcome, Account, Settings, Back, Compatibility }

internal val LocalExperienceVisible = staticCompositionLocalOf { true }

/** One gesture per composition; motion pauses when the screen is no longer visible. */
@Composable
internal fun NavigationMotionIllustration(demo: NavigationDemo) {
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val reducedMotion = rememberReducedMotion()
    val visible = LocalExperienceVisible.current
    val phase = remember(demo) { Animatable(0f) }
    LaunchedEffect(demo, reducedMotion, lifecycle, visible) {
        if (reducedMotion || !visible) phase.snapTo(0.52f)
        else lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (isActive) {
                phase.snapTo(0f)
                phase.animateTo(1f, tween(5200, easing = LinearEasing))
            }
        }
    }
    val description = stringResource(when (demo) {
        NavigationDemo.Welcome -> R.string.experience_diagram_welcome
        NavigationDemo.Account -> R.string.experience_diagram_account
        NavigationDemo.Back -> R.string.experience_diagram_back
        NavigationDemo.Settings -> R.string.experience_diagram_settings
        NavigationDemo.Compatibility -> R.string.experience_diagram_compatibility
    })
    Box(
        Modifier.fillMaxSize().clipToBounds()
            .clearAndSetSemantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        when (demo) {
            NavigationDemo.Compatibility -> Text(
                "14", style = iridePageTitleTextStyle().copy(fontSize = 150.sp, lineHeight = 160.sp),
                color = MaterialTheme.colorScheme.textPrimary.copy(alpha = 0.16f),
            )
            NavigationDemo.Welcome -> WelcomeWordmark(phase)
            NavigationDemo.Account -> IdentityMotion(phase)
            else -> GestureCutaway(demo, phase)
        }
    }
}

private fun motionProgress(phase: Float, start: Float, end: Float): Float =
    FastOutSlowInEasing.transform(((phase - start) / (end - start)).coerceIn(0f, 1f))

@Composable
private fun WelcomeWordmark(phase: Animatable<Float, *>) {
    Icon(
        painter = painterResource(R.drawable.ic_logo),
        contentDescription = null,
        tint = MaterialTheme.colorScheme.textPrimary,
        modifier = Modifier.size(88.dp).graphicsLayer {
            val breath = sin(phase.value * 2f * PI.toFloat())
            scaleX = 0.98f + breath * 0.012f
            scaleY = scaleX
            translationY = breath * 3.dp.toPx()
        },
    )
}

@Composable
private fun IdentityMotion(phase: Animatable<Float, *>) {
    val ink = MaterialTheme.colorScheme.textPrimary
    val paper = MaterialTheme.colorScheme.background
    BoxWithConstraints(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        val spread = minOf(maxWidth * 0.23f, 86.dp)
        val tileSize = minOf(maxWidth * 0.31f, 112.dp)
        Canvas(Modifier.fillMaxSize()) {
            val cycle = phase.value * 2f * PI.toFloat()
            val center = Offset(size.width / 2, size.height / 2)
            val line = 1.dp.toPx()
            val start = center + Offset(-spread.toPx(), -30.dp.toPx())
            val end = center + Offset(spread.toPx(), 8.dp.toPx())
            val connection = Path().apply {
                moveTo(start.x, start.y)
                cubicTo(center.x, start.y, center.x, end.y, end.x, end.y)
            }
            drawPath(connection, ink.copy(alpha = 0.16f), style = Stroke(line))
            repeat(4) { index ->
                val t = (phase.value + index * 0.25f) % 1f
                val u = 1f - t
                val x = u * u * u * start.x + 3 * u * u * t * center.x +
                    3 * u * t * t * center.x + t * t * t * end.x
                val y = u * u * u * start.y + 3 * u * u * t * start.y +
                    3 * u * t * t * end.y + t * t * t * end.y
                drawCircle(ink.copy(alpha = 0.65f), 2.5.dp.toPx(), Offset(x, y))
            }
            repeat(3) { index ->
                val y = center.y + 108.dp.toPx() + index * 13.dp.toPx()
                val inset = index * 10.dp.toPx()
                drawLine(ink.copy(alpha = 0.10f + 0.04f * cos(cycle + index)),
                    Offset(center.x - 70.dp.toPx() + inset, y), Offset(center.x + 70.dp.toPx() - inset, y),
                    4.dp.toPx(), StrokeCap.Round)
            }
        }
        repeat(2) { index ->
            val youtube = index == 0
            Box(
                Modifier.size(tileSize).graphicsLayer {
                    val cycle = phase.value * 2f * PI.toFloat()
                    translationX = spread.toPx() * if (youtube) -1f else 1f
                    translationY = (if (youtube) (-30).dp else 8.dp).toPx() +
                        sin(cycle + index * PI.toFloat()) * 5.dp.toPx()
                    rotationZ = sin(cycle) * (if (youtube) -2f else 2f)
                }.clip(FolioShape(28.dp)).background(paper)
                    .background(ink.copy(alpha = 0.045f)).border(1.dp, ink.copy(alpha = 0.25f), FolioShape(28.dp)),
                contentAlignment = Alignment.Center,
            ) {
                if (youtube) {
                    Canvas(Modifier.size(52.dp, 36.dp)) {
                        drawRoundRect(ink, cornerRadius = CornerRadius(10.dp.toPx()))
                        val play = Path().apply {
                            moveTo(size.width * 0.43f, size.height * 0.29f)
                            lineTo(size.width * 0.43f, size.height * 0.71f)
                            lineTo(size.width * 0.67f, size.height * 0.5f)
                            close()
                        }
                        drawPath(play, paper)
                    }
                } else {
                    Icon(painterResource(R.drawable.ic_logo), contentDescription = null,
                        tint = ink, modifier = Modifier.size(76.dp))
                }
            }
        }
    }
}

@Composable
private fun GestureCutaway(demo: NavigationDemo, phase: Animatable<Float, *>) {
    val ink = MaterialTheme.colorScheme.textPrimary
    val paper = MaterialTheme.colorScheme.background
    Canvas(Modifier.fillMaxWidth().heightIn(max = 340.dp).fillMaxHeight()) {
        val p = phase.value
        val opacity = 1f
        val move = motionProgress(p, 0.18f, 0.62f)
        val stroke = 1.2.dp.toPx()
        val radius = CornerRadius(28.dp.toPx())
        val top = 4.dp.toPx()
        val bottom = size.height - 4.dp.toPx()
        val width = minOf(size.width * 0.82f, (bottom - top) * 9f / 16f)
        val left = (size.width - width) / 2f
        val frameSize = Size(width, bottom - top)

        drawFolioRoundRect(ink.copy(alpha = 0.025f * opacity), Offset(left, top), frameSize, radius)
        drawFolioRoundRect(ink.copy(alpha = 0.32f * opacity), Offset(left, top), frameSize, radius, style = Stroke(stroke))

        if (demo == NavigationDemo.Settings) {
            val y = bottom - 22.dp.toPx()
            val center = size.width / 2f
            val spread = width * 0.27f
            val icon = 6.dp.toPx()
            val reset = motionProgress(p, 0.90f, 1f)
            val oldAlpha = 1f - motionProgress(p, 0.2f, 0.43f) + reset
            if (oldAlpha > 0f) {
                val triangle = Path().apply {
                    moveTo(center - spread + icon / 2, y - icon)
                    lineTo(center - spread - icon / 2, y)
                    lineTo(center - spread + icon / 2, y + icon)
                }
                drawPath(triangle, ink.copy(alpha = oldAlpha * 0.7f), style = Stroke(stroke, cap = StrokeCap.Round))
                drawCircle(ink.copy(alpha = oldAlpha * 0.7f), icon, Offset(center, y), style = Stroke(stroke))
                drawRoundRect(ink.copy(alpha = oldAlpha * 0.7f), Offset(center + spread - icon, y - icon),
                    Size(icon * 2, icon * 2), CornerRadius(2.dp.toPx()), style = Stroke(stroke))
            }
            val bar = motionProgress(p, 0.36f, 0.62f) * (1f - reset)
            if (bar > 0f) {
                val halfWidth = width * (0.08f + 0.12f * bar)
                val barY = bottom - 14.dp.toPx()
                drawLine(ink.copy(alpha = bar * opacity), Offset(center - halfWidth, barY),
                    Offset(center + halfWidth, barY), 3.dp.toPx(), StrokeCap.Round)
            }
        } else {
            val release = motionProgress(p, 0.64f, 0.79f)
            val travel = width * (0.28f * move + 0.76f * release)
            val pageLeft = left + 9.dp.toPx() + travel
            val pageTop = top + 9.dp.toPx() + size.height * 0.04f * move
            val pageWidth = width - 18.dp.toPx()
            val pageHeight = bottom - top - 18.dp.toPx() - size.height * 0.08f * move
            val pageAlpha = opacity * (1f - release)
            val phoneOutline = FolioShape(28.dp).createOutline(frameSize, layoutDirection, this)
            val screen = Path().apply {
                when (phoneOutline) {
                    is Outline.Generic -> addPath(phoneOutline.path, Offset(left, top))
                    is Outline.Rounded -> addPath(Path().apply { addRoundRect(phoneOutline.roundRect) }, Offset(left, top))
                    is Outline.Rectangle -> addRect(Rect(Offset(left, top), frameSize))
                }
            }
            clipPath(screen) {
                drawFolioRoundRect(paper, Offset(pageLeft, pageTop), Size(pageWidth, pageHeight), radius)
                drawFolioRoundRect(ink.copy(alpha = 0.07f * pageAlpha), Offset(pageLeft, pageTop), Size(pageWidth, pageHeight), radius)
                drawFolioRoundRect(ink.copy(alpha = 0.28f * pageAlpha), Offset(pageLeft, pageTop), Size(pageWidth, pageHeight), radius, style = Stroke(stroke))
                val restart = motionProgress(p, 0.88f, 1f)
                val start = Offset(left + 9.dp.toPx(), top + 9.dp.toPx())
                val startSize = Size(pageWidth, bottom - top - 18.dp.toPx())
                drawFolioRoundRect(paper.copy(alpha = restart), start, startSize, radius)
                drawFolioRoundRect(ink.copy(alpha = 0.07f * restart), start, startSize, radius)
                drawFolioRoundRect(ink.copy(alpha = 0.28f * restart), start, startSize, radius, style = Stroke(stroke))
            }
            val y = size.height * 0.38f
            val arrowAlpha = motionProgress(p, 0.16f, 0.25f) * (1f - motionProgress(p, 0.65f, 0.77f)) * opacity
            val arrowX = left + width * (0.04f + 0.24f * move)
            val arrowSize = 6.dp.toPx()
            drawCircle(ink.copy(alpha = arrowAlpha), 20.dp.toPx(), Offset(arrowX + arrowSize, y))
            val arrow = Path().apply {
                moveTo(arrowX + arrowSize, y - arrowSize)
                lineTo(arrowX, y)
                lineTo(arrowX + arrowSize, y + arrowSize)
                moveTo(arrowX, y)
                lineTo(arrowX + arrowSize * 2, y)
            }
            drawPath(arrow, paper.copy(alpha = arrowAlpha), style = Stroke(2.dp.toPx(), cap = StrokeCap.Round))
            // The trail begins at the physical edge, showing where the gesture must start.
            drawLine(ink.copy(alpha = arrowAlpha * 0.2f), Offset(left, y),
                Offset(arrowX - 6.dp.toPx(), y), stroke, StrokeCap.Round)
            drawLine(ink.copy(alpha = 0.55f * opacity), Offset(size.width * 0.42f, bottom - 10.dp.toPx()),
                Offset(size.width * 0.58f, bottom - 10.dp.toPx()), 2.dp.toPx(), StrokeCap.Round)
        }
    }
}
