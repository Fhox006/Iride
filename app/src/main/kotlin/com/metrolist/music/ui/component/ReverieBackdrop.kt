package com.metrolist.music.ui.component

import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.rotateRad
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.metrolist.music.models.REVERIE_RING_COUNT
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.metrolist.music.models.ReveriePalette
import com.metrolist.music.models.reverieFigurePose
import com.metrolist.music.models.reverieMorphRadius
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
internal fun rememberReverieMotionTime(running: Boolean): State<Float> {
    val time = rememberSaveable { mutableFloatStateOf(0f) }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    var resumed by remember(lifecycle) { mutableStateOf(lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) }
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, _ -> resumed = lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED) }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }
    LaunchedEffect(running, resumed) {
        if (!running || !resumed) return@LaunchedEffect
        var previous = withFrameNanos { it }
        while (true) withFrameNanos { frame ->
            time.floatValue += (frame - previous) / 1_000_000f
            previous = frame
        }
    }
    return time
}

/** Fine nested contours morph over light paper, without filled shapes. */
@Composable
internal fun ReverieBackdrop(chapter: Int, palette: ReveriePalette, reducedMotion: Boolean, modifier: Modifier = Modifier, variant: Int = 0) {
    val transition = updateTransition(chapter, label = "reverieBackdrop")
    val duration = if (reducedMotion) 0 else 600
    val background = animateColorAsState(Color(palette.background), tween(duration), label = "paperColor")
    val x = transition.animateFloat({ tween(duration) }, label = "x") { reverieFigurePose(it, variant).x }
    val y = transition.animateFloat({ tween(duration) }, label = "y") { reverieFigurePose(it, variant).y }
    val radiusFraction = transition.animateFloat({ tween(duration) }, label = "radius") { reverieFigurePose(it, variant).radius }
    val rotation = transition.animateFloat({ tween(duration) }, label = "rotation") { reverieFigurePose(it, variant).rotation }
    val square = transition.animateFloat({ tween(duration) }, label = "square") { reverieFigurePose(it, variant).contour.square }
    val rosette = transition.animateFloat({ tween(duration) }, label = "rosette") { reverieFigurePose(it, variant).contour.rosette }
    val diamond = transition.animateFloat({ tween(duration) }, label = "diamond") { reverieFigurePose(it, variant).contour.diamond }
    val path = remember { Path() }
    val points = remember { FloatArray(160 * 2) }
    Canvas(modifier.fillMaxSize().clipToBounds()) {
        drawRect(background.value)
        val center = Offset(size.width * x.value, size.height * y.value)
        val radius = size.minDimension * radiusFraction.value
        repeat(160) { point ->
            val angle = point * (2 * PI / 160).toFloat()
            val distance = reverieMorphRadius(angle, square.value, rosette.value, diamond.value)
            points[point * 2] = cos(angle) * distance
            points[point * 2 + 1] = sin(angle) * distance
        }
        rotateRad(rotation.value, center) {
            for (ring in 1..REVERIE_RING_COUNT) {
                path.reset()
                val scale = radius * ring / REVERIE_RING_COUNT.toFloat()
                repeat(160) { point ->
                    val x = center.x + points[point * 2] * scale
                    val y = center.y + points[point * 2 + 1] * scale
                    if (point == 0) path.moveTo(x, y) else path.lineTo(x, y)
                }
                path.close()
                drawPath(path, Color.White.copy(alpha = .6f + .2f * ring / REVERIE_RING_COUNT), style = Stroke(1.dp.toPx()))
            }
        }
    }
}
