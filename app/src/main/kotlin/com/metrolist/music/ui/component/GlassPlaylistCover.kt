/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.component

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.metrolist.music.constants.IrideBaseBorderWidth
import com.metrolist.music.ui.utils.rememberReducedMotion

/**
 * Frosted-glass playlist cover: a blurred mosaic of up to 4 of the playlist's own thumbnails
 * behind a translucent panel with a badge icon centered on top. Used for the Liked/Starred
 * cover both on its own screen (large) and wherever it's referenced from outside (Library
 * list/grid rows, small) so the two stay visually identical.
 */
@Composable
fun GlassPlaylistCover(
    thumbnails: List<String>,
    icon: Int,
    size: Dp,
    shape: Shape,
    iconSizeFraction: Float = 0.65f,
    modifier: Modifier = Modifier,
) {
    val mosaicThumbnails = thumbnails.distinct().take(4)
    val reducedMotion = rememberReducedMotion()
    Box(
        modifier =
            modifier
                .size(size)
                .clip(shape)
                .background(MaterialTheme.colorScheme.surfaceContainer),
    ) {
        if (mosaicThumbnails.isNotEmpty()) {
            // Dissolve between mosaics instead of a hard cut, since liking/unliking a song
            // reshuffles which 4 thumbnails land in the 4 tiles.
            Crossfade(
                targetState = mosaicThumbnails,
                modifier = Modifier.matchParentSize(),
                animationSpec = if (reducedMotion) snap() else tween(durationMillis = 300, easing = FastOutSlowInEasing),
                label = "glassPlaylistCoverMosaic",
            ) { tiles ->
                Box(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            // Zoomed well past the crop bounds so each quadrant's photo fills more of
                            // its quarter and the 4 tiles read as one blended scene instead of 4
                            // distinctly separated corners.
                            .graphicsLayer(scaleX = 1.6f, scaleY = 1.6f)
                            .blur(size * 0.1f),
                ) {
                    if (tiles.size == 1) {
                        AsyncImage(
                            model = tiles[0],
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                        )
                    } else {
                        Column(Modifier.fillMaxSize()) {
                            Row(Modifier.weight(1f).fillMaxWidth()) {
                                AsyncImage(
                                    model = tiles.getOrElse(0) { tiles[0] },
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.weight(1f).fillMaxHeight(),
                                )
                                AsyncImage(
                                    model = tiles.getOrElse(1) { tiles[0] },
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.weight(1f).fillMaxHeight(),
                                )
                            }
                            Row(Modifier.weight(1f).fillMaxWidth()) {
                                AsyncImage(
                                    model = tiles.getOrElse(2) { tiles[0] },
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.weight(1f).fillMaxHeight(),
                                )
                                AsyncImage(
                                    model = tiles.getOrElse(3) { tiles[0] },
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.weight(1f).fillMaxHeight(),
                                )
                            }
                        }
                    }
                }
            }
            Box(
                modifier =
                    Modifier
                        .matchParentSize()
                        .background(Color.Black.copy(alpha = 0.38f)),
            )
        }
        Box(
            modifier =
                Modifier
                    .matchParentSize()
                    .background(
                        MaterialTheme.colorScheme.surface.copy(
                            alpha = if (mosaicThumbnails.isEmpty()) 1f else 0.16f,
                        ),
                    ).background(
                        Brush.linearGradient(
                            colors = listOf(Color.White.copy(alpha = 0.22f), Color.Transparent, Color.Transparent),
                            start = Offset.Zero,
                            end = Offset(340f, 340f),
                        ),
                    ).border(IrideBaseBorderWidth, Color.White.copy(alpha = 0.22f), shape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(icon),
                contentDescription = null,
                tint =
                    if (mosaicThumbnails.isEmpty()) {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                    } else {
                        Color.White.copy(alpha = 0.95f)
                    },
                modifier = Modifier.size(size * iconSizeFraction),
            )
        }
    }
}
