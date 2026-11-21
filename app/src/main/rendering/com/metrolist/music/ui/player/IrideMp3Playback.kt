package com.metrolist.music.ui.player

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.util.lerp
import com.metrolist.music.ui.component.BottomSheetState
import com.metrolist.music.ui.utils.IrideMotion

@Composable
internal fun IrideMp3PlaybackGlyph(
    isPlaying: Boolean,
    reducedMotion: Boolean,
    modifier: Modifier = Modifier,
) {
    val pausedProgress by animateFloatAsState(
        targetValue = if (isPlaying) 0f else 1f,
        animationSpec = tween(
            if (reducedMotion) 0 else IrideMotion.Short,
            easing = IrideMotion.EaseOutQuart,
        ),
        label = "mp3PlaybackMorph",
    )
    Canvas(modifier) {
        val progress = pausedProgress
        val unitX = size.width / 960f
        val unitY = size.height / 960f
        val left = lerp(280f, 100f, progress) * unitX
        val triangle = Path().apply {
            moveTo(left, lerp(180f, 220f, progress) * unitY)
            lineTo(lerp(700f, 560f, progress) * unitX, 480f * unitY)
            lineTo(left, lerp(780f, 740f, progress) * unitY)
            close()
        }
        drawPath(triangle, Color.White)
        if (progress > 0f) {
            val barHeight = 520f * progress * unitY
            val barSize = Size(110f * progress * unitX, barHeight)
            val top = (size.height - barHeight) / 2f
            val color = Color.White.copy(alpha = progress)
            drawRect(color, Offset(lerp(740f, 580f, progress) * unitX, top), barSize)
            drawRect(color, Offset(lerp(850f, 770f, progress) * unitX, top), barSize)
        }
    }
}

@Composable
internal fun IrideMp3PlayerHandle(
    state: BottomSheetState,
    screenHeight: Dp,
    isExpandable: Boolean,
    contentColor: Color = Color.White,
    modifier: Modifier = Modifier,
    handleAlpha: Float = 1f,
) = com.metrolist.music.ui.component.IrideFolioDragHandle(
    state, screenHeight, isExpandable, contentColor, modifier, handleAlpha = handleAlpha,
)
