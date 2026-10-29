package com.metrolist.music.ui.component

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

/** The gradient and hairline travel with the portrait in both directions. */
internal fun Modifier.artistPortraitFinish(shape: Shape, pigment: Color, border: Color, gradientStrength: Float = 1f): Modifier =
    drawWithCache {
        val gradient = Brush.verticalGradient(
            colorStops = Array(65) { index ->
                val fraction = index / 64f
                fraction to pigment.copy(alpha = (1f - artistArtworkPhotoOpacity(fraction)) * gradientStrength.coerceIn(0f, 1f))
            },
            endY = size.height,
        )
        val outline = shape.createOutline(size, layoutDirection, this)
        onDrawWithContent {
            drawContent()
            drawRect(gradient)
            drawOutline(outline, border, style = Stroke(0.5.dp.toPx()))
        }
    }
