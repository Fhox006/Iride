package com.metrolist.music.ui.component

import android.graphics.Bitmap
import android.os.Build
import android.util.LruCache
import androidx.compose.foundation.Image
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.RenderEffect
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalGraphicsContext
import androidx.compose.ui.platform.LocalLayoutDirection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

/** Reuse native kernels while changing their radius, instead of fading a blurred copy over a sharp photo. */
internal class ArtistPhotoBlurEffects(
    private val maximumRadius: Float,
    private val tileMode: TileMode = TileMode.Clamp,
) {
    private val effects = arrayOfNulls<RenderEffect>(97)

    fun at(progress: Float): RenderEffect? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return null
        val step = (progress.coerceIn(0f, 1f) * 96).roundToInt()
        if (step == 0) return null
        return effects[step] ?: BlurEffect(
            maximumRadius * step / 96f, maximumRadius * step / 96f, tileMode,
        ).also { effects[step] = it }
    }
}

/** Static portraits reuse filtered textures through both scrolling and the return flight. */
@Composable
internal fun ArtistCachedBlurredPhoto(
    bitmap: Bitmap,
    progress: () -> Float,
    contentScale: ContentScale,
    backingColor: Color?,
) {
    val context = LocalGraphicsContext.current
    val density = LocalDensity.current
    val direction = LocalLayoutDirection.current
    val radius = with(density) { IrideFolio.ArtistPhotoBlurRadius.toPx() }
    val renderer = remember(bitmap, context, density, direction, radius, contentScale, backingColor) {
        IrideFrozenBackdrop(bitmap.asImageBitmap(), context, density, direction,
            maximumRadiusPx = radius, levels = 16, contentScale = contentScale, backingColor = backingColor)
    }
    DisposableEffect(renderer) { onDispose { renderer.dispose() } }
    Canvas(Modifier.fillMaxSize()) { renderer.draw(this, progress()) }
}

/** Older Android versions replace one opaque photo with a computed kernel, never overlay the sharp image. */
@Composable
internal fun ArtistCompatBlurredPhoto(
    bitmap: Bitmap,
    progress: () -> Float,
    contentScale: ContentScale = ContentScale.Fit,
) {
    val original = remember(bitmap) { bitmap.asImageBitmap() }
    var image by remember(bitmap) { mutableStateOf(original) }
    LaunchedEffect(bitmap) {
        var kernels: ArtistPhotoBlurKernels? = null
        snapshotFlow { (progress().coerceIn(0f, 1f) * 48).roundToInt() }
            .distinctUntilChanged().collectLatest { step ->
                image = if (step == 0) original else withContext(Dispatchers.Default) {
                    val engine = kernels ?: ArtistPhotoBlurKernels(bitmap).also { kernels = it }
                    engine.at(step).asImageBitmap()
                }
            }
    }
    Image(image, null, contentScale = contentScale, filterQuality = FilterQuality.High, modifier = Modifier.fillMaxSize())
}

private class ArtistPhotoBlurKernels(bitmap: Bitmap) {
    private val side = minOf(bitmap.width, bitmap.height, 384)
    private val pixels = IntArray(side * side)
    // The six passes overwrite two scratch buffers. Bitmap.createBitmap copies the
    // final pixels, so cached frames never share these mutable working arrays.
    private val horizontalPixels = IntArray(side * side)
    private val verticalPixels = IntArray(side * side)
    private val cache = object : LruCache<Int, Bitmap>(6 * 1024 * 1024) {
        override fun sizeOf(key: Int, value: Bitmap): Int = value.allocationByteCount
    }

    init {
        val sample = Bitmap.createScaledBitmap(bitmap, side, side, true)
        sample.getPixels(pixels, 0, side, 0, 0, side, side)
        if (sample !== bitmap) sample.recycle()
    }

    fun at(step: Int): Bitmap {
        val radius = (side * 0.10f * step / 48f).roundToInt().coerceAtLeast(1)
        cache.get(radius)?.let { return it }
        var blurred = pixels
        repeat(3) {
            artistBoxBlur(blurred, side, side, horizontal = true, radius = radius, result = horizontalPixels)
            blurred = artistBoxBlur(horizontalPixels, side, side, horizontal = false, radius = radius, result = verticalPixels)
        }
        return Bitmap.createBitmap(blurred, side, side, Bitmap.Config.ARGB_8888).also { cache.put(radius, it) }
    }
}
