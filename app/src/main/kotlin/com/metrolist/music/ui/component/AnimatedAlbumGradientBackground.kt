package com.metrolist.music.ui.component

import android.graphics.Bitmap
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.InfiniteTransition
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.palette.graphics.Palette
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.max

private val DefaultColors = listOf(
    Color(0xFF3A3A3A),
    Color(0xFF555555),
    Color(0xFF2A2A2A)
)

@Composable
fun AnimatedAlbumGradientBackground(
    thumbnail: Bitmap?,
    modifier: Modifier = Modifier
) {
    var extractedColors by remember(thumbnail) { mutableStateOf(DefaultColors) }

    LaunchedEffect(thumbnail) {
        if (thumbnail == null) {
            extractedColors = DefaultColors
            return@LaunchedEffect
        }

        val colors = withContext(Dispatchers.IO) {
            val palette = Palette.from(thumbnail).generate()
            val swatches = listOfNotNull(
                palette.vibrantSwatch,
                palette.mutedSwatch,
                palette.darkVibrantSwatch,
                palette.lightMutedSwatch
            ).map { Color(it.rgb) }

            if (swatches.isEmpty()) {
                null
            } else {
                // Ensure we have 3 colors by repeating available swatches
                List(3) { index -> swatches[index % swatches.size] }
            }
        }

        extractedColors = colors ?: DefaultColors
    }

    val color1 by animateColorAsState(
        targetValue = extractedColors.getOrElse(0) { DefaultColors[0] },
        animationSpec = tween(1200),
        label = "color1"
    )
    val color2 by animateColorAsState(
        targetValue = extractedColors.getOrElse(1) { DefaultColors[1] },
        animationSpec = tween(1200),
        label = "color2"
    )
    val color3 by animateColorAsState(
        targetValue = extractedColors.getOrElse(2) { extractedColors.getOrElse(0) { DefaultColors[2] } },
        animationSpec = tween(1200),
        label = "color3"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "gradient")
    
    val offset1 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "offset1"
    )
    
    val offset2 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(13000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "offset2"
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height
        val radius = 0.9f * max(width, height)

        // Circle 1 - Top Leftish
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(color1, Color.Transparent),
                center = Offset(
                    x = width * (0.2f + 0.3f * offset1),
                    y = height * (0.2f + 0.3f * offset2)
                ),
                radius = radius
            ),
            radius = radius,
            center = Offset(
                x = width * (0.2f + 0.3f * offset1),
                y = height * (0.2f + 0.3f * offset2)
            )
        )

        // Circle 2 - Bottom Rightish
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(color2, Color.Transparent),
                center = Offset(
                    x = width * (0.8f - 0.4f * offset2),
                    y = height * (0.8f - 0.4f * offset1)
                ),
                radius = radius
            ),
            radius = radius,
            center = Offset(
                x = width * (0.8f - 0.4f * offset2),
                y = height * (0.8f - 0.4f * offset1)
            )
        )

        // Circle 3 - Center / Dynamic
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(color3, Color.Transparent),
                center = Offset(
                    x = width * (0.5f + 0.2f * offset1 - 0.1f * offset2),
                    y = height * (0.5f - 0.2f * offset2 + 0.1f * offset1)
                ),
                radius = radius
            ),
            radius = radius,
            center = Offset(
                x = width * (0.5f + 0.2f * offset1 - 0.1f * offset2),
                y = height * (0.5f - 0.2f * offset2 + 0.1f * offset1)
            )
        )
    }
}
