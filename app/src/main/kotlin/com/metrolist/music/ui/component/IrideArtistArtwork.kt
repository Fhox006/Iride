package com.metrolist.music.ui.component

import android.graphics.Bitmap
import android.os.Build
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filterNotNull

internal data class IrideArtistArtwork(
    val bitmap: Bitmap,
    val tint: Color,
)

@Composable
internal fun rememberIrideArtistArtwork(
    artistId: String,
    url: String?,
    artistName: String?,
    channelId: String? = null,
    externalEnabled: Boolean = true,
    highResolution: Boolean = true,
): State<IrideArtistArtwork?> {
    val loader = rememberArtistArtworkLoader()
    val request = remember(artistId, artistName, url, channelId, externalEnabled) {
        ArtistArtworkRequest(artistId, artistName, url, channelId, externalEnabled)
    }
    val selected = remember(artistId) { mutableStateOf(loader.cachedForDisplay(request)) }
    val artwork = remember(artistId) { derivedStateOf { selected.value?.artwork } }
    val selectedUrl = selected.value?.url
    DisposableEffect(artistId, selectedUrl) {
        if (selectedUrl != null) loader.retainPresentation(request, selectedUrl)
        onDispose { if (selectedUrl != null) loader.releasePresentation(request, selectedUrl) }
    }
    LaunchedEffect(request, highResolution) {
        coroutineScope {
            launch {
                loader.selectionUpdates.collect { key ->
                    if (key == request.key && selected.value == null) selected.value = loader.cachedForDisplay(request)
                }
            }
            if (highResolution) launch {
                snapshotFlow { selected.value }.filterNotNull().collectLatest { displayed ->
                    val detailed = loader.loadHighResolution(displayed) ?: return@collectLatest
                    // Upgrade definition without replacing the photo, including late cache updates.
                    if (selected.value?.url == displayed.url && detailed.artwork.bitmap.width > displayed.artwork.bitmap.width) {
                        selected.value = detailed
                    }
                }
            }
            loader.prefetch(request)
            if (selected.value == null) {
                val prepared = loader.loadForDisplay(request)
                if (prepared != null) {
                    if (selected.value == null) selected.value = prepared
                }
            }
        }
    }
    return artwork
}

@Composable
internal fun IrideArtistBackground(
    tint: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier.fillMaxSize()) {
        drawRect(tint)
    }
}

@Composable
internal fun IrideArtistArtworkImage(
    artwork: IrideArtistArtwork,
    scrollOffset: () -> Float,
    blurProgress: () -> Float,
    pullOffset: () -> Float,
    modifier: Modifier = Modifier,
    pagePullOffset: () -> Float = { 0f },
) {
    val image = remember(artwork.bitmap) { artwork.bitmap.asImageBitmap() }
    val maximumBlur = with(LocalDensity.current) { IrideFolio.ArtistPhotoBlurRadius.toPx() }
    val effects = remember(maximumBlur) { ArtistPhotoBlurEffects(maximumBlur) }
    val motion = Modifier.fillMaxSize().graphicsLayer {
        // Keep the photo anchored while the page follows the same pull that drives its zoom.
        translationY = scrollOffset() * 0.6f - pagePullOffset().coerceAtLeast(0f)
        val stretch = (pullOffset() / size.height.coerceAtLeast(1f)).coerceIn(0f, 0.6f)
        scaleX = 1f + stretch
        scaleY = 1f + stretch
        transformOrigin = TransformOrigin(0.5f, 0f)
        renderEffect = effects.at(blurProgress())
        clip = true
    }
    Box(modifier) {
        Box(motion) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                Image(
                    image, null, contentScale = ContentScale.Fit,
                    filterQuality = FilterQuality.High,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                ArtistCompatBlurredPhoto(artwork.bitmap, blurProgress)
            }
        }
    }
}
