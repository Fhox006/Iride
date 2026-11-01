/**
 * Iride Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import com.metrolist.music.ui.theme.FolioShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.rememberAsyncImagePainter
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.metrolist.music.R
import com.metrolist.music.models.MediaMetadata
import com.metrolist.music.ui.theme.SatoshiFontFamily
import com.metrolist.music.ui.theme.IBMPlexMonoFontFamily

@Composable
fun rememberAdjustedFontSize(
    text: String,
    maxWidth: Dp,
    maxHeight: Dp,
    density: Density,
    initialFontSize: TextUnit = 20.sp,
    minFontSize: TextUnit = 14.sp,
    style: TextStyle = TextStyle.Default,
    textMeasurer: androidx.compose.ui.text.TextMeasurer? = null
): TextUnit {
    val measurer = textMeasurer ?: rememberTextMeasurer()

    var calculatedFontSize by remember(text, maxWidth, maxHeight, style, density) {
        val initialSize = when {
            text.length < 50 -> initialFontSize
            text.length < 100 -> (initialFontSize.value * 0.8f).sp
            text.length < 200 -> (initialFontSize.value * 0.6f).sp
            else -> (initialFontSize.value * 0.5f).sp
        }
        mutableStateOf(initialSize)
    }

    LaunchedEffect(key1 = text, key2 = maxWidth, key3 = maxHeight) {
        val targetWidthPx = with(density) { maxWidth.toPx() * 0.92f }
        val targetHeightPx = with(density) { maxHeight.toPx() * 0.92f }
        if (text.isBlank()) {
            calculatedFontSize = minFontSize
            return@LaunchedEffect
        }

        if (text.length < 20) {
            val largerSize = (initialFontSize.value * 1.1f).sp
            val result = measurer.measure(
                text = AnnotatedString(text),
                style = style.copy(fontSize = largerSize)
            )
            if (result.size.width <= targetWidthPx && result.size.height <= targetHeightPx) {
                calculatedFontSize = largerSize
                return@LaunchedEffect
            }
        } else if (text.length < 30) {
            val largerSize = (initialFontSize.value * 0.9f).sp
            val result = measurer.measure(
                text = AnnotatedString(text),
                style = style.copy(fontSize = largerSize)
            )
            if (result.size.width <= targetWidthPx && result.size.height <= targetHeightPx) {
                calculatedFontSize = largerSize
                return@LaunchedEffect
            }
        }

        var minSize = minFontSize.value
        var maxSize = initialFontSize.value
        var bestFit = minSize
        var iterations = 0

        while (minSize <= maxSize && iterations < 20) {
            iterations++
            val midSize = (minSize + maxSize) / 2
            val midSizeSp = midSize.sp
            val result = measurer.measure(
                text = AnnotatedString(text),
                style = style.copy(fontSize = midSizeSp)
            )
            if (result.size.width <= targetWidthPx && result.size.height <= targetHeightPx) {
                bestFit = midSize
                minSize = midSize + 0.5f
            } else {
                maxSize = midSize - 0.5f
            }
        }

        calculatedFontSize = if (bestFit < minFontSize.value) minFontSize else bestFit.sp
    }

    return calculatedFontSize
}

@Composable
fun LyricsImageCard(
    lyricText: String,
    mediaMetadata: MediaMetadata,
    darkBackground: Boolean = true,
    backgroundColor: Color? = null,
    backgroundStyle: LyricsBackgroundStyle = LyricsBackgroundStyle.SOLID,
    textColor: Color? = null,
    secondaryTextColor: Color? = null,
    textAlign: TextAlign = TextAlign.Center,
) {
    val context = LocalContext.current
    val background = backgroundColor ?: if (darkBackground) Color(0xFF121212) else Color(0xFFF5F5F5)
    val ink = textColor ?: if (darkBackground) Color.White else Color.Black
    val secondary = secondaryTextColor ?: ink.copy(alpha = 0.7f)
    val painter = rememberAsyncImagePainter(
        ImageRequest.Builder(context).data(mediaMetadata.thumbnailUrl).crossfade(false).build(),
    )
    BoxWithConstraints(Modifier.fillMaxWidth().clip(FolioShape(20.dp))) {
        val scale = maxWidth.value / LyricsShareStyle.Width
        val density = LocalDensity.current
        val fontScale = density.fontScale
        val padding = (LyricsShareStyle.Padding * scale).dp
        val coverSize = (LyricsShareStyle.CoverSize * scale).dp
        val coverRadius = (LyricsShareStyle.CoverRadius * scale).dp
        val measurer = rememberTextMeasurer()
        val lyricSize = remember(lyricText, maxWidth, density, measurer) {
            val pixelsPerUnit = density.density * scale
            val availableWidth = ((LyricsShareStyle.Width - LyricsShareStyle.Padding * 2) * pixelsPerUnit).toInt()
                .coerceAtLeast(1)
            val fitted = LyricsShareStyle.fittedTextSize(
                lyricText, LyricsShareStyle.lyricsHeightLimit(LyricsShareStyle.CoverSize),
            ) { size ->
                val fontSize = (size * scale / fontScale).sp
                measurer.measure(
                    text = AnnotatedString(lyricText),
                    style = TextStyle(
                        fontFamily = SatoshiFontFamily,
                        fontWeight = FontWeight.Black,
                        fontSize = fontSize,
                        lineHeight = fontSize * LyricsShareStyle.LineSpacing,
                        platformStyle = PlatformTextStyle(includeFontPadding = false),
                    ),
                    constraints = Constraints(maxWidth = availableWidth),
                ).size.height / pixelsPerUnit
            }
            (fitted * scale / fontScale).sp
        }
        Box(Modifier.matchParentSize()) {
            if (backgroundStyle == LyricsBackgroundStyle.BLUR) {
                Image(
                    painter, null, contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize().blur(50.dp),
                )
                Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.3f)))
            } else {
                Box(Modifier.fillMaxSize().background(Brush.verticalGradient(
                    LyricsShareStyle.gradientColors(background.toArgb()).map { Color(it) },
                )))
            }
        }
        Column(
            modifier = Modifier.fillMaxWidth()
                .padding(padding),
        ) {
            Column(Modifier.fillMaxWidth().padding(bottom = (LyricsShareStyle.FooterGap * scale).dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = (LyricsShareStyle.HeaderGap * scale).dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Image(
                        painter, null, contentScale = ContentScale.Crop,
                        modifier = Modifier.size(coverSize).clip(FolioShape(coverRadius)),
                    )
                    Spacer(Modifier.width((LyricsShareStyle.CoverGap * scale).dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            mediaMetadata.title, color = ink, fontFamily = SatoshiFontFamily,
                            fontSize = (LyricsShareStyle.TitleSize * scale / fontScale).sp,
                            fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            mediaMetadata.artists.joinToString { it.name }, color = secondary,
                            fontFamily = IBMPlexMonoFontFamily,
                            fontSize = (LyricsShareStyle.ArtistSize * scale / fontScale).sp,
                            maxLines = 1, overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                Text(
                    lyricText, color = ink, fontFamily = SatoshiFontFamily,
                    fontSize = lyricSize, fontWeight = FontWeight.Black, textAlign = textAlign,
                    lineHeight = lyricSize * LyricsShareStyle.LineSpacing,
                    style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false)),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Image(
                    painterResource(R.drawable.lyrics_share_wordmark), null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.size(
                        width = (LyricsShareStyle.LogoWidth * scale).dp,
                        height = (LyricsShareStyle.LogoHeight * scale).dp,
                    ),
                    colorFilter = ColorFilter.tint(Color.White),
                )
            }
        }
    }
}
