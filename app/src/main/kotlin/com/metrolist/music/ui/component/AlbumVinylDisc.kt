/**
 * Iride Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.component

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import kotlinx.coroutines.isActive
import sv.lib.squircleshape.SquircleShape

private val VinylDiscBaseTop = Color(0xFF141416)
private val VinylDiscBaseBottom = Color(0xFF060607)

// Degrees per millisecond — one full rotation roughly every 4.5s, close to a real 33rpm record's
// visual speed without looking frantic in a compact UI.
private const val VinylRotationSpeedDegPerMs = 360f / 4500f

/**
 * A vinyl record sitting concentrically behind an album cover: the cover stays put on top (the
 * record label/spindle are always hidden under it) while the disc itself grows a little further
 * out from behind the cover's edges and starts spinning while [isPlaying] is true, then shrinks
 * back and freezes in place — not resets to zero — the moment playback of this album stops. Used
 * by the New Iride UI's [com.metrolist.music.ui.screens.AlbumScreen] header.
 */
@Composable
fun AlbumVinylDisc(
    thumbnailUrl: String?,
    coverSize: Dp,
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val discSize by animateDpAsState(
        targetValue = coverSize * (if (isPlaying) 1.35f else 1.10f),
        animationSpec = spring(dampingRatio = 0.75f, stiffness = 220f),
        label = "vinylDiscSize",
    )

    var angle by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(isPlaying) {
        if (!isPlaying) return@LaunchedEffect
        var lastFrameMillis = -1L
        while (isActive) {
            withFrameMillis { frameMillis ->
                if (lastFrameMillis >= 0) {
                    val deltaMillis = frameMillis - lastFrameMillis
                    angle = (angle + deltaMillis * VinylRotationSpeedDegPerMs) % 360f
                }
                lastFrameMillis = frameMillis
            }
        }
    }

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .size(discSize)
                .graphicsLayer { rotationZ = angle }
                .shadow(elevation = 10.dp, shape = CircleShape, clip = false)
                .clip(CircleShape)
                .background(Brush.verticalGradient(listOf(VinylDiscBaseTop, VinylDiscBaseBottom))),
        ) {
            // Concentric grooves.
            for (ring in 1..6) {
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(discSize * (1f - ring * 0.09f))
                        .border(width = 0.6.dp, color = Color.White.copy(alpha = 0.05f), shape = CircleShape),
                )
            }
        }

        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(thumbnailUrl)
                .crossfade(true)
                .build(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(coverSize)
                .shadow(elevation = 12.dp, shape = irideAlbumCoverShape(coverSize), spotColor = Color.Black.copy(alpha = 0.5f))
                .clip(irideAlbumCoverShape(coverSize)),
        )
    }
}

private fun irideAlbumCoverShape(coverSize: Dp) = SquircleShape(radius = coverSize * 0.05f, cornerSmoothing = 0.45f)
