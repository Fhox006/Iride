package com.metrolist.music.ui.screens.search

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.unit.IntOffset
import com.metrolist.music.ui.component.IrideArtistArtwork
import com.metrolist.music.ui.component.IrideArtistArtworkImage
import com.metrolist.music.ui.component.IrideArtistBackground
import com.metrolist.music.ui.component.artistArtworkPhotoOpacity
import com.metrolist.music.ui.component.RubberBandPull
import kotlin.math.roundToInt

/** Artist photo, pigment, parallax, and bottom fade use the artist screen's rendering pipeline. */
@Composable
internal fun SearchArtistBackdrop(
    artwork: IrideArtistArtwork,
    pageOffset: Float,
    blurProgress: Float,
    pull: RubberBandPull,
) {
    IrideArtistBackground(artwork.tint, Modifier.fillMaxSize())
    // Match the artist page: fill the strip exposed by a downward pull with the zoomed photo.
    IrideArtistArtworkImage(
        artwork = artwork,
        scrollOffset = { (-pageOffset).coerceAtLeast(0f) },
        blurProgress = { blurProgress },
        pullOffset = { pull.offset },
        modifier = Modifier.fillMaxWidth().aspectRatio(1f).drawWithContent {
            val exposed = pull.offset.coerceAtLeast(0f)
            if (exposed > 0f) clipRect(bottom = exposed.coerceAtMost(size.height)) {
                this@drawWithContent.drawContent()
            }
        },
    )
    Box(
        Modifier.fillMaxWidth().aspectRatio(1f)
            .offset { IntOffset(0, (pageOffset + pull.offset).roundToInt()) }
            .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen; clip = true }
            .drawWithCache {
                val fade = Brush.verticalGradient(
                    colorStops = Array(65) { index ->
                        val fraction = index / 64f
                        fraction to Color.White.copy(alpha = artistArtworkPhotoOpacity(fraction))
                    },
                    startY = 0f,
                    endY = size.height,
                )
                onDrawWithContent { drawContent(); drawRect(fade, blendMode = BlendMode.DstIn) }
            },
    ) {
        IrideArtistArtworkImage(
            artwork = artwork,
            scrollOffset = { (-pageOffset).coerceAtLeast(0f) },
            blurProgress = { blurProgress },
            pullOffset = { pull.offset },
            pagePullOffset = { pull.offset },
            modifier = Modifier.fillMaxSize(),
        )
    }
}
