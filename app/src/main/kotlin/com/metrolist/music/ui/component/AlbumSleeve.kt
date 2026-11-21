package com.metrolist.music.ui.component

import android.graphics.Bitmap
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.EnterExitState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.input.pointer.pointerInput
import coil3.compose.AsyncImagePainter
import coil3.compose.rememberAsyncImagePainter
import coil3.toBitmap
import androidx.core.content.res.ResourcesCompat
import com.metrolist.music.R
import com.metrolist.music.ui.utils.rememberReducedMotion

// Temporarily disabled until the sleeve flip is reworked in a future update.
private const val AlbumSleeveFlipEnabled = false

/** Album-only physical sleeve. The whole cover blocks touches to the disc behind it. */
@Composable
fun AlbumSleeve(
    albumId: String,
    title: String,
    trackTitles: List<String>,
    thumbnailUrl: String?,
    accentColor: Color,
    isPlaying: Boolean,
    onArtworkState: (AsyncImagePainter.State) -> Unit,
    modifier: Modifier = Modifier,
    artwork: Bitmap? = null,
) {
    var backVisible by remember(albumId) { mutableStateOf(false) }
    val reducedMotion = rememberReducedMotion()
    val visibility = LocalAnimatedVisibilityScope.current
    val expansion = LocalCollectionExpansionProgress.current
    val gesture = LocalCollectionPanelGesture.current
    val exiting = visibility?.transition?.targetState == EnterExitState.PostExit
    val closing = exiting || gesture?.backActive == true
    val context = LocalContext.current
    val rotation = remember(albumId) { Animatable(0f) }
    val renderedRotation by remember(rotation, expansion, gesture, closing, exiting) {
        derivedStateOf {
            if (!closing || expansion == null) rotation.value else {
                val preview = (gesture?.backProgress ?: 0f) * (gesture?.recovery?.value ?: 1f)
                AlbumSleeveMotion.rotation(rotation.value,
                    AlbumSleeveMotion.closeProgress(preview, if (exiting) expansion.value else 1f))
            }
        }
    }
    val painter = rememberAsyncImagePainter(rememberCollectionArtworkRequest(thumbnailUrl))
    val artworkState by painter.state.collectAsState()
    LaunchedEffect(artworkState) { onArtworkState(artworkState) }
    val cover = artwork ?: remember(artworkState) {
        (artworkState as? AsyncImagePainter.State.Success)?.result?.image?.toBitmap()
    }
    val pigment = remember(cover, accentColor) {
        cover?.let { image ->
            val sample = Bitmap.createScaledBitmap(image, 32, 32, false)
            val readable = if (sample.config == Bitmap.Config.HARDWARE) {
                sample.copy(Bitmap.Config.ARGB_8888, false)
            } else sample
            val pixels = IntArray(32 * 32)
            readable.getPixels(pixels, 0, 32, 0, 0, 32, 32)
            val color = albumArtworkPigments(pixels, 32, 32).firstOrNull()?.color
            if (readable !== sample) readable.recycle()
            if (sample !== image) sample.recycle()
            color
        } ?: accentColor.toArgb()
    }
    var print by remember(albumId) { mutableStateOf<AlbumSleeveRenderer.PrintedSleeve?>(null) }
    LaunchedEffect(backVisible, albumId, title, trackTitles, pigment, context) {
        if (!AlbumSleeveFlipEnabled || !backVisible || print != null) return@LaunchedEffect
        val heading = ResourcesCompat.getFont(context, R.font.satoshi_variable)
        val body = ResourcesCompat.getFont(context, R.font.inter_variable)
        print = AlbumSleeveRenderer.printBack(albumId, title, trackTitles, pigment, heading, body)
    }
    LaunchedEffect(backVisible, isPlaying, closing, reducedMotion, print) {
        if (!AlbumSleeveFlipEnabled) return@LaunchedEffect
        if (closing && expansion != null) return@LaunchedEffect
        val duration = if (reducedMotion) 0 else if (closing) {
            ((visibility?.transition?.totalDurationNanos ?: 0L) / 1_000_000L).coerceIn(0L, 150L).toInt()
        } else 1050
        rotation.animateTo(
            if (backVisible && print != null && !closing) 180f else 0f,
            tween(duration, easing = FastOutSlowInEasing),
        )
    }
    val renderer = remember(albumId) { AlbumSleeveRenderer(albumId, albumSleeveContour()) }
    Canvas(
        modifier.pointerInput(albumId, isPlaying, closing) {
            // Keep the cover blocking touches to the disc, but disable its flip gesture.
            detectTapGestures(onTap = {
                if (AlbumSleeveFlipEnabled && !closing) backVisible = !backVisible
            })
        },
    ) {
        drawIntoCanvas {
            renderer.draw(
                it.nativeCanvas,
                size.width,
                size.height,
                cover,
                if (AlbumSleeveFlipEnabled) print?.bitmap else null,
                if (AlbumSleeveFlipEnabled) renderedRotation else 0f,
            )
        }
    }
}
