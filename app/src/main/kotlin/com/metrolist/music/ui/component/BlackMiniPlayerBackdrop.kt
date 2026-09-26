package com.metrolist.music.ui.component

import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.ui.graphics.Brush
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import coil3.imageLoader
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.toBitmap
import com.metrolist.music.models.MediaMetadata
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

@Composable
fun BlackMiniPlayerBackdrop(
    height: Dp,
    fadeHeight: Dp,
    mediaMetadata: MediaMetadata?,
    modifier: Modifier = Modifier,
    animated: Boolean = true,
) {
    val context = LocalContext.current
    var thumbnail by remember { mutableStateOf<Bitmap?>(null) }
    LaunchedEffect(mediaMetadata?.id, mediaMetadata?.thumbnailUrl) {
        val url = mediaMetadata?.thumbnailUrl
        val bitmap = if (url != null) {
            val request = ImageRequest.Builder(context)
                .data(url)
                .size(100, 100)
                .allowHardware(false)
                .memoryCacheKey("gradient_${mediaMetadata?.id}")
                .build()
            runCatching { context.imageLoader.execute(request).image?.toBitmap() }.getOrNull()
        } else null
        currentCoroutineContext().ensureActive()
        thumbnail = bitmap
    }
    val fadeEnd = (fadeHeight / height).coerceIn(0f, 1f)
    val fadeBrush = remember(fadeEnd) {
        Brush.verticalGradient(
            colorStops = arrayOf(
                0f to Color.Transparent,
                fadeEnd * 0.25f to Color.Black.copy(alpha = 0.156f),
                fadeEnd * 0.5f to Color.Black.copy(alpha = 0.5f),
                fadeEnd * 0.75f to Color.Black.copy(alpha = 0.844f),
                fadeEnd to Color.Black,
                1f to Color.Black,
            ),
        )
    }
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clipToBounds()
            .background(fadeBrush),
    ) {
        AnimatedAlbumGradientBackground(
            thumbnail = thumbnail,
            style = AlbumGradientStyle.AMBIENT,
            animated = animated && mediaMetadata != null,
            modifier = Modifier
                .matchParentSize()
                .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                .drawWithContent {
                    drawContent()
                    // Mask only the color layer; the black backdrop keeps its original fade.
                    drawRect(fadeBrush, blendMode = BlendMode.DstIn)
                },
        )
    }
}
