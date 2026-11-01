package com.metrolist.music.ui.component

import android.content.Context
import android.util.LruCache
import androidx.compose.animation.animateColor
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import coil3.imageLoader
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.toBitmap
import com.metrolist.music.constants.PlayerBackgroundStyle
import com.metrolist.music.models.MediaMetadata
import com.metrolist.music.ui.utils.rememberReducedMotion
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext

internal data class ArtworkHeaderPalette(
    val tint: Color,
    val pigments: List<AlbumArtworkPigment> = emptyList(),
) {
    fun halo(index: Int): AlbumArtworkPigment = pigments.getOrElse(index) {
        pigments.firstOrNull() ?: AlbumArtworkPigment(tint.toArgb(), 0, 0.5f, 0.5f)
    }
}

/** Visible collection tiles and their destination share the same already-resolved pigments. */
internal object ArtworkHeaderPalettes {
    private val colors = LruCache<String, ArtworkHeaderPalette>(96)
    private val decodes = Semaphore(2)

    fun cached(url: String?): ArtworkHeaderPalette? = url?.let(colors::get)

    suspend fun load(context: Context, url: String): ArtworkHeaderPalette? {
        cached(url)?.let { return it }
        return decodes.withPermit {
            cached(url) ?: withContext(Dispatchers.IO) {
                try {
                    val bitmap = context.imageLoader.execute(
                        ImageRequest.Builder(context).data(url).size(100, 100)
                            .allowHardware(false).build(),
                    ).image?.toBitmap() ?: return@withContext null
                    val pigments = bitmap.irideAlbumArtworkPigments()
                    val tint = pigments.firstOrNull()?.color?.let { Color(it) }
                        ?: return@withContext null
                    ArtworkHeaderPalette(tint, pigments).also { colors.put(url, it) }
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Exception) {
                    null
                }
            }
        }
    }
}

@Composable
internal fun ArtworkHeaderGradientBackground(
    mediaMetadata: MediaMetadata?,
    playerBackground: PlayerBackgroundStyle,
    backgroundColor: Color,
    modifier: Modifier = Modifier,
    onTopColorChanged: (Color) -> Unit,
    artworkTint: Color? = null,
    gradientHeight: Dp = IrideFolio.ArtworkGradientHeight,
) {
    val context = LocalContext.current
    val collection = LocalCollectionExpansionSource.current
    val url = collection?.artworkUrl ?: mediaMetadata?.thumbnailUrl
    var palette by remember(collection) {
        mutableStateOf(
            collection?.palette ?: ArtworkHeaderPalettes.cached(url)
                ?: ArtworkHeaderPalette(artworkTint ?: backgroundColor),
        )
    }
    val reducedMotion = rememberReducedMotion()
    val onTopColorChangedState by rememberUpdatedState(onTopColorChanged)

    LaunchedEffect(url, backgroundColor, collection) {
        // The source palette also covers the first frame before destination metadata exists.
        collection?.palette?.let {
            palette = it
            return@LaunchedEffect
        }
        if (url == null) return@LaunchedEffect
        for (attempt in 0..2) {
            val loaded = ArtworkHeaderPalettes.load(context, url)
            if (loaded != null) {
                collection?.palette = loaded
                palette = loaded
                return@LaunchedEffect
            }
            if (attempt < 2) delay(1200)
        }
    }
    val transition = updateTransition(palette, label = "artworkHeaderPalette")
    // A cached source palette is already the transition's initial value. If pigments
    // arrive later, blend them in instead of flashing from the black fallback.
    val fadeDuration = if (reducedMotion) 0 else 700
    val topTint by transition.animateColor(
        transitionSpec = { tween(fadeDuration) }, label = "artworkHeaderTint",
    ) { it.tint.limitArtworkLuminance(IrideFolio.ArtworkMaximumLuminance) }
    // Retarget from the visible colors and positions even during rapid skips.
    val pigments = List(2) { index ->
        val color by transition.animateColor(
            transitionSpec = { tween(fadeDuration) }, label = "artworkHaloColor$index",
        ) { Color(it.halo(index).color).limitArtworkLuminance(IrideFolio.ArtworkMaximumLuminance) }
        val centerX by transition.animateFloat(
            transitionSpec = { tween(fadeDuration) }, label = "artworkHaloX$index",
        ) { it.halo(index).centerX }
        val centerY by transition.animateFloat(
            transitionSpec = { tween(fadeDuration) }, label = "artworkHaloY$index",
        ) { it.halo(index).centerY }
        AlbumArtworkPigment(color.toArgb(), 0, centerX, centerY)
    }
    LaunchedEffect(topTint, backgroundColor) {
        onTopColorChangedState(topTint.copy(alpha = 0.85f).compositeOver(backgroundColor))
    }
    Canvas(
        modifier.fillMaxWidth().height(gradientHeight)
            .invalidateFrostOnChanges { topTint }
            .irideBackgroundGrain(fadeOut = true),
    ) {
        drawRect(backgroundColor)
        // Restore alpha26's full-width top wash; halos only add variation at its upper edge.
        drawRect(Brush.verticalGradient(
            0f to topTint.copy(alpha = 0.85f),
            0.45f to topTint.copy(alpha = 0.45f),
            1f to Color.Transparent,
            endY = size.height,
        ))
        if (playerBackground != PlayerBackgroundStyle.GRADIENT) {
            drawArtworkAmbientHalos(pigments = pigments, height = size.height)
        }
        drawRect(Brush.verticalGradient(
            0f to Color.Transparent,
            0.45f to Color.Transparent,
            0.85f to backgroundColor.copy(alpha = 0.85f),
            1f to backgroundColor,
            endY = size.height,
        ))
    }
}
