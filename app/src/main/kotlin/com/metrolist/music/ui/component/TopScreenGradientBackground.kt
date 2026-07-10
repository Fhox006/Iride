package com.metrolist.music.ui.component

import android.graphics.Bitmap
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.palette.graphics.Palette
import coil3.imageLoader
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.toBitmap
import com.metrolist.music.constants.PlayerBackgroundStyle
import com.metrolist.music.models.MediaMetadata
import com.metrolist.music.ui.theme.PlayerColorExtractor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Subtle, top-anchored version of the player's animated gradient, shown behind
 * the main tabs (Home/Library/Search/News). Mirrors whatever [playerBackground]
 * style the user picked for the player, fading to full transparency before the
 * midpoint of the screen so it reads as a tinted top edge, not a full repaint.
 *
 * Meant to be mounted once, persistently, above the tab NavHost — visibility is
 * controlled by the caller via alpha so the internal animation clocks never
 * restart when switching between the four tabs.
 */
private val TopGradientHeight = 380.dp
private const val TopGradientOpacity = 0.55f

@Composable
fun TopScreenGradientBackground(
    mediaMetadata: MediaMetadata?,
    playerBackground: PlayerBackgroundStyle,
    modifier: Modifier = Modifier,
) {
    if (playerBackground == PlayerBackgroundStyle.DEFAULT || playerBackground == PlayerBackgroundStyle.BLUR) {
        return
    }

    val context = LocalContext.current
    var thumbnail by remember { mutableStateOf<Bitmap?>(null) }
    var gradientColors by remember { mutableStateOf<List<Color>>(emptyList()) }
    val colorCache = remember { mutableMapOf<String, List<Color>>() }
    val fallbackColor = MaterialTheme.colorScheme.surface.toArgb()

    LaunchedEffect(mediaMetadata?.id, mediaMetadata?.thumbnailUrl) {
        val url = mediaMetadata?.thumbnailUrl
        if (url == null) {
            thumbnail = null
            return@LaunchedEffect
        }
        withContext(Dispatchers.IO) {
            val request = ImageRequest.Builder(context)
                .data(url)
                .size(100, 100)
                .allowHardware(false)
                .build()
            val bitmap = runCatching { context.imageLoader.execute(request) }
                .getOrNull()?.image?.toBitmap()
            withContext(Dispatchers.Main) { thumbnail = bitmap }

            val id = mediaMetadata?.id
            if (bitmap != null && id != null) {
                val cached = colorCache[id]
                val colors = cached ?: run {
                    val palette = Palette.from(bitmap).maximumColorCount(8).generate()
                    PlayerColorExtractor.extractGradientColors(palette, fallbackColor)
                        .also { colorCache[id] = it }
                }
                withContext(Dispatchers.Main) { gradientColors = colors }
            } else {
                withContext(Dispatchers.Main) { gradientColors = emptyList() }
            }
        }
    }

    val bgColor = MaterialTheme.colorScheme.background

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(TopGradientHeight),
    ) {
        when (playerBackground) {
            PlayerBackgroundStyle.GRADIENT -> {
                AnimatedContent(
                    targetState = gradientColors,
                    transitionSpec = { fadeIn(tween(900)).togetherWith(fadeOut(tween(900))) },
                    label = "topGradientBackground",
                ) { colors ->
                    if (colors.isNotEmpty()) {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .height(TopGradientHeight)
                                .background(
                                    Brush.verticalGradient(
                                        colorStops = arrayOf(
                                            0.0f to colors[0].copy(alpha = TopGradientOpacity),
                                            0.45f to colors.getOrElse(1) { colors[0] }.copy(alpha = TopGradientOpacity * 0.6f),
                                            1.0f to Color.Transparent,
                                        ),
                                    ),
                                ),
                        )
                    }
                }
            }

            PlayerBackgroundStyle.ANIMATED_GRADIENT -> {
                TopGradientBlobs(gradientColors, bgColor)
            }

            PlayerBackgroundStyle.BETTER_ANIMATED_GRADIENT -> {
                Box(Modifier.fillMaxWidth().height(TopGradientHeight)) {
                    BetterAnimatedGradientBackground(
                        thumbnail = thumbnail,
                        modifier = Modifier.fillMaxWidth().height(TopGradientHeight),
                    )
                    // Fade the blurred art into the ordinary page background before
                    // it reaches the middle of the screen, and keep it subtle overall.
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(TopGradientHeight)
                            .background(
                                Brush.verticalGradient(
                                    colorStops = arrayOf(
                                        0.0f to bgColor.copy(alpha = 1f - TopGradientOpacity),
                                        0.6f to bgColor.copy(alpha = 1f - TopGradientOpacity * 0.6f),
                                        1.0f to bgColor,
                                    ),
                                ),
                            ),
                    )
                }
            }

            else -> {}
        }
    }
}

@Composable
private fun TopGradientBlobs(colors: List<Color>, bgColor: Color) {
    val safeColors = colors.ifEmpty {
        listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary)
    }
    val c0 by androidx.compose.animation.animateColorAsState(safeColors[0], tween(1400), label = "tgb_c0")
    val c1 by androidx.compose.animation.animateColorAsState(
        safeColors.getOrElse(1) { safeColors[0] },
        tween(1400),
        label = "tgb_c1",
    )

    val infinite = rememberInfiniteTransition(label = "top_gradient_blobs")
    val shift by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(14000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "top_gradient_shift",
    )

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(TopGradientHeight),
    ) {
        val w = size.width
        val h = size.height

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(c0.copy(alpha = TopGradientOpacity), Color.Transparent),
                center = Offset(w * (0.22f + 0.10f * shift), h * 0.22f),
                radius = w * 0.62f,
            ),
            radius = w * 0.62f,
            center = Offset(w * (0.22f + 0.10f * shift), h * 0.22f),
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(c1.copy(alpha = TopGradientOpacity * 0.85f), Color.Transparent),
                center = Offset(w * (0.76f - 0.08f * shift), h * 0.14f),
                radius = w * 0.55f,
            ),
            radius = w * 0.55f,
            center = Offset(w * (0.76f - 0.08f * shift), h * 0.14f),
        )

        drawRect(
            brush = Brush.verticalGradient(
                colorStops = arrayOf(
                    0.00f to Color.Transparent,
                    0.45f to Color.Transparent,
                    0.85f to bgColor.copy(alpha = 0.85f),
                    1.00f to bgColor,
                ),
                startY = 0f,
                endY = h,
            ),
        )
    }
}
