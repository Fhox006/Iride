package com.metrolist.music.ui.component

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/** The gradient travels with the portrait in both directions. */
internal fun Modifier.artistPortraitFinish(pigment: Color, opacity: () -> Float = { 1f }): Modifier {
    val stops = Array(65) { index ->
        val fraction = index / 64f
        fraction to pigment.copy(alpha = 1f - artistArtworkPhotoOpacity(fraction))
    }
    return drawWithCache {
        val gradient = Brush.verticalGradient(
            colorStops = stops,
            endY = size.height,
        )
        onDrawWithContent {
            drawContent()
            // Keep the artwork pigment fixed inside the clipped panel during the gesture.
            drawRect(gradient, alpha = opacity().coerceIn(0f, 1f))
        }
    }
}
