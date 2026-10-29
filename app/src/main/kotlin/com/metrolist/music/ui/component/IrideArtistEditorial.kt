/**
 * SPDX-License-Identifier: GPL-3.0-only
 * First-frame artwork and decoded-logo handoffs adapted from SimpMusic ArtistScreen.kt,
 * revision 068d74435f0cf0e8552af3da07879eafd328fea7, by SimpMusic contributors.
 * https://github.com/maxrave-dev/SimpMusic/blob/068d74435f0cf0e8552af3da07879eafd328fea7/composeApp/src/commonMain/kotlin/com/maxrave/simpmusic/ui/screen/other/ArtistScreen.kt
 * Iride changes: first-frame backgrounds, lifecycle-aware video, Folio typography and bounded logos.
 */
package com.metrolist.music.ui.component

import android.graphics.Bitmap
import android.os.Build
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.allowHardware
import com.metrolist.music.ui.utils.IrideMotion
import com.metrolist.music.ui.utils.rememberReducedMotion

@Composable
internal fun rememberIrideArtistEditorialArtwork(
    artistId: String,
    videoUrl: String?,
    previewUrl: String?,
): State<IrideArtistArtwork?> {
    val loader = rememberArtistArtworkLoader()
    val artwork = remember(artistId, videoUrl, previewUrl) {
        mutableStateOf(videoUrl?.let(loader::cachedEditorial))
    }
    LaunchedEffect(artistId, videoUrl, previewUrl) {
        artwork.value = videoUrl?.let { loader.loadEditorial(it, previewUrl) }
    }
    return artwork
}

@Composable
internal fun IrideArtistEditorialMotion(
    videoUrl: String,
    active: Boolean,
    scrollOffset: () -> Float,
    blurProgress: () -> Float,
    pullOffset: () -> Float,
    modifier: Modifier = Modifier,
) {
    var firstFrame by remember(videoUrl) { mutableStateOf(false) }
    var failed by remember(videoUrl) { mutableStateOf(false) }
    val videoAlpha by animateFloatAsState(
        targetValue = if (firstFrame && !failed) 1f else 0f,
        animationSpec = tween(IrideMotion.Short),
        label = "folioArtistMotion",
    )
    if (!failed) {
        Box(modifier) {
            Box(artistArtworkMotionModifier(scrollOffset, blurProgress, pullOffset, { 0f })) {
                AppleMusicMotionArtworkPlayer(
                    url = videoUrl,
                    active = active,
                    onFirstFrame = { firstFrame = true },
                    onError = { failed = true },
                    modifier = Modifier.fillMaxSize().graphicsLayer {
                        // Pre-Android 12 reveals the blurred first-frame photo beneath the video.
                        val blurVisibility = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) 1f
                            else 1f - blurProgress().coerceIn(0f, 1f)
                        alpha = videoAlpha * blurVisibility
                    },
                )
            }
        }
    }
}

internal fun Bitmap.cropBlackEditorialBars(): Bitmap {
    val pixels = IntArray(width * height)
    getPixels(pixels, 0, width, 0, 0, width, height)
    fun isBlack(color: Int): Boolean {
        val red = color shr 16 and 0xff
        val green = color shr 8 and 0xff
        val blue = color and 0xff
        return red <= 24 && green <= 24 && blue <= 24
    }
    fun rowIsBlack(y: Int): Boolean {
        var black = 0
        for (x in 0 until width) if (isBlack(pixels[y * width + x])) black++
        return black.toFloat() / width >= 0.98f
    }
    fun columnIsBlack(x: Int): Boolean {
        var black = 0
        for (y in 0 until height) if (isBlack(pixels[y * width + x])) black++
        return black.toFloat() / height >= 0.98f
    }

    val maxTop = (height * 0.25f).toInt()
    val maxSide = (width * 0.25f).toInt()
    var top = 0
    var bottom = height
    var left = 0
    var right = width
    while (top < maxTop && rowIsBlack(top)) top++
    while (height - bottom < maxTop && rowIsBlack(bottom - 1)) bottom--
    while (left < maxSide && columnIsBlack(left)) left++
    while (width - right < maxSide && columnIsBlack(right - 1)) right--
    if (top < height * 0.02f) top = 0
    if (height - bottom < height * 0.02f) bottom = height
    if (left < width * 0.02f) left = 0
    if (width - right < width * 0.02f) right = width
    return if (left == 0 && top == 0 && right == width && bottom == height) this
    else Bitmap.createBitmap(this, left, top, right - left, bottom - top)
}

@Composable
internal fun IrideArtistEditorialTitle(
    artistId: String,
    artistName: String,
    logoUrl: String?,
    style: TextStyle,
    color: Color,
    animateTitle: Boolean,
) {
    val context = LocalContext.current
    val reducedMotion = rememberReducedMotion()
    var logoLoaded by remember(artistId, logoUrl) { mutableStateOf(false) }
    val logoRequest = remember(context, logoUrl) {
        logoUrl?.let {
            ImageRequest.Builder(context).data(it).size(1200, 336).allowHardware(false).build()
        }
    }
    val logoAlpha by animateFloatAsState(
        if (logoUrl != null && logoLoaded) 1f else 0f,
        animationSpec = tween(if (reducedMotion) 0 else IrideMotion.Short),
        label = "folioArtistWordmark",
    )
    Box(Modifier.fillMaxWidth().heightIn(min = 44.dp), contentAlignment = Alignment.Center) {
        if (logoAlpha < 1f) {
            TypewriterText(
                text = artistName,
                style = style,
                color = color,
                resetKey = artistId,
                animate = animateTitle && !reducedMotion,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth().graphicsLayer { alpha = 1f - logoAlpha },
            )
        }
        if (logoUrl != null) {
            AsyncImage(
                model = logoRequest,
                contentDescription = artistName.takeIf { logoLoaded },
                contentScale = ContentScale.Fit,
                onSuccess = { logoLoaded = true },
                onError = { logoLoaded = false },
                modifier = Modifier.fillMaxWidth(0.7f).heightIn(max = 84.dp)
                    .graphicsLayer { alpha = logoAlpha },
            )
        }
    }
}
