package com.metrolist.music.ui.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.imageLoader
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.toBitmap
import com.metrolist.music.constants.PlayerBackgroundStyle
import com.metrolist.music.models.MediaMetadata
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

internal val LocalSharedTopGradient = staticCompositionLocalOf { false }

private object TopGradientPigments {
    val colors = android.util.LruCache<String, Color>(24)
    var last = Color(0xFF30323B)
}

/** One stationary pigment for ordinary pages; artwork headers retain their own backdrop. */
@Composable
fun TopScreenGradientBackground(
    mediaMetadata: MediaMetadata?,
    playerBackground: PlayerBackgroundStyle,
    modifier: Modifier = Modifier,
    onTopColorChanged: (Color) -> Unit = {},
    emphasizeArtwork: Boolean = false,
    backgroundColor: Color = MaterialTheme.colorScheme.background,
    artworkTint: Color? = null,
    artworkGradientHeight: Dp = IrideFolio.ArtworkGradientHeight,
    sharedBackground: Boolean = false,
) {
    if (emphasizeArtwork) {
        ArtworkHeaderGradientBackground(
            mediaMetadata = mediaMetadata,
            playerBackground = playerBackground,
            backgroundColor = backgroundColor,
            modifier = modifier,
            onTopColorChanged = onTopColorChanged,
            artworkTint = artworkTint,
            gradientHeight = artworkGradientHeight,
        )
        return
    }
    // Screens may still declare a gradient when used alone. Inside the app the root owns it.
    if (LocalSharedTopGradient.current && !sharedBackground) return

    val context = LocalContext.current
    val fallback = Color(0xFF30323B)
    var pigment by remember { mutableStateOf(TopGradientPigments.last) }
    val colors = TopGradientPigments.colors
    val artworkUrl = mediaMetadata?.thumbnailUrl?.takeIf { it.isNotBlank() }
    val videoUrl = mediaMetadata?.id?.takeIf { it.matches(Regex("[A-Za-z0-9_-]{11}")) }
        ?.let { "https://i.ytimg.com/vi/$it/hqdefault.jpg" }
    LaunchedEffect(artworkUrl, videoUrl) {
        val urls = listOfNotNull(artworkUrl, videoUrl).distinct()
        for (attempt in 0..2) {
            for (url in urls) {
                val cached = colors.get(url)
                if (cached != null) {
                    pigment = cached
                    TopGradientPigments.last = cached
                    return@LaunchedEffect
                }
                val extracted = withContext(Dispatchers.IO) {
                    val request = ImageRequest.Builder(context).data(url).size(100, 100)
                        .allowHardware(false).build()
                    val bitmap = runCatching { context.imageLoader.execute(request) }
                        .getOrNull()?.image?.toBitmap()
                    bitmap?.irideArtworkTint(fallback)?.limitArtworkLuminance(0.16f)
                }
                if (extracted != null) {
                    colors.put(url, extracted)
                    pigment = extracted
                    TopGradientPigments.last = extracted
                    return@LaunchedEffect
                }
            }
            if (urls.isEmpty()) return@LaunchedEffect
            if (attempt < 2) delay(1200)
        }
        // Retain the last resolved pigment through missing artwork or temporary network errors.
    }
    val tint by animateColorAsState(pigment, tween(700), label = "sharedTopPigment")
    val topColor = tint.copy(alpha = 0.55f).compositeOver(Color.Black)
    val currentOnTopColorChanged by rememberUpdatedState(onTopColorChanged)
    LaunchedEffect(topColor) { currentOnTopColorChanged(topColor) }
    Box(
        modifier.fillMaxWidth().height(380.dp)
            .invalidateFrostOnChanges { tint }
            .background(
                Brush.verticalGradient(
                    0f to topColor,
                    0.45f to topColor.irideDimmed(0.45f),
                    1f to Color.Black,
                ),
            )
            .irideBackgroundGrain(fadeOut = true),
    )
}
