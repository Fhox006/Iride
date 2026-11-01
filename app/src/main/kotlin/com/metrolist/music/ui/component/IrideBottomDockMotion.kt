package com.metrolist.music.ui.component

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.animateValueAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.metrolist.music.ui.utils.rememberReducedMotion

@Stable
internal class IrideBottomDockMotion(
    val offset: State<Offset>,
    val updateDrag: (Offset, Boolean) -> Unit,
) {
    fun reset() = updateDrag(Offset.Zero, false)
}

internal val LocalIrideBottomDockMotion = staticCompositionLocalOf<IrideBottomDockMotion?> { null }

@Composable
internal fun rememberIrideBottomDockMotion(): IrideBottomDockMotion {
    val dragOffset = remember { mutableStateOf(Offset.Zero) }
    val dragging = remember { mutableStateOf(false) }
    val reducedMotion = rememberReducedMotion()
    val animatedOffset = animateValueAsState(
        targetValue = if (reducedMotion) Offset.Zero else dragOffset.value,
        typeConverter = Offset.VectorConverter,
        animationSpec = if (reducedMotion) snap() else spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = if (dragging.value) 180f else 55f,
            visibilityThreshold = Offset(0.002f, 0.002f),
        ),
        label = "bottomDockFloatingOffset",
    )
    return remember(animatedOffset) {
        IrideBottomDockMotion(animatedOffset) { offset, isDragging ->
            dragOffset.value = offset
            dragging.value = isDragging
        }
    }
}

// The surface and both rows read one animation in their draw layers, without relaying out the page.
internal fun Modifier.irideBottomDockMotion(): Modifier = composed {
    val motion = LocalIrideBottomDockMotion.current
    if (motion == null) this else graphicsLayer {
        val offset = motion.offset.value
        translationX = offset.x * 6.dp.toPx()
        translationY = offset.y * 6.dp.toPx()
    }
}
