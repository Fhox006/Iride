package com.metrolist.music.ui.component

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.scale

/** Stable, artwork-dependent variation that enters from above the top edge. */
internal fun DrawScope.drawArtworkAmbientHalos(
    pigments: List<AlbumArtworkPigment>,
    height: Float,
) {
    if (height <= 0f || size.width <= 0f || pigments.isEmpty()) return
    repeat(2) { index ->
        val pigment = pigments.getOrElse(index) { pigments.first() }
        val center = Offset(
            size.width * ((if (index == 0) 0.22f else 0.76f) +
                (pigment.centerX - 0.5f) * 0.10f),
            -height * (0.02f + pigment.centerY * 0.04f),
        )
        val radius = size.width * ((if (index == 0) 0.68f else 0.60f) + pigment.centerX * 0.06f)
        // The album stays dark even for white accents, while hue and saturation remain intact.
        val color = Color(pigment.color).limitArtworkLuminance(IrideFolio.ArtworkMaximumLuminance)
        val opacity = if (index == 0) 0.40f else 0.65f
        val verticalRadius = minOf(radius, height * (0.78f + pigment.centerY * 0.06f))
        scale(1f, verticalRadius / radius, center) {
            drawCircle(
                Brush.radialGradient(
                    colorStops = Array(33) { stop ->
                        val t = stop / 32f
                        val fade = t * t * t * (t * (t * 6f - 15f) + 10f)
                        t to color.copy(alpha = opacity * (1f - fade).coerceIn(0f, 1f))
                    },
                    center = center, radius = radius,
                ),
                radius = radius, center = center,
            )
        }
    }
}
