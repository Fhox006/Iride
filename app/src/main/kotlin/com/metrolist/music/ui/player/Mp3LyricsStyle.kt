package com.metrolist.music.ui.player

import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.palette.graphics.Palette
import com.metrolist.music.ui.component.BetterAnimatedGradientBackground
import com.metrolist.music.ui.component.IrideFolio

internal enum class Mp3LyricsStyle { GRADIENT, TV }

internal val Mp3LyricsStyleKey = stringPreferencesKey("mp3LyricsStyle")

@Composable
internal fun Mp3LyricsBackground(
    artwork: Bitmap?,
    lightweight: Boolean,
    incomingArtwork: Bitmap? = null,
    crossfadeProgress: Float = 0f,
    fallbackColor: Color = Color(0xFF39465D),
) {
    if (lightweight) {
        val gradient = remember(artwork, fallbackColor) {
            val artworkColor = artwork?.let {
                Palette.from(it).maximumColorCount(8).generate().dominantSwatch?.rgb?.let { rgb -> Color(rgb) }
            } ?: fallbackColor
            Brush.verticalGradient(
                listOf(
                    lerp(IrideMp3BackgroundColor, artworkColor, 0.48f),
                    lerp(IrideMp3BackgroundColor, artworkColor, 0.25f),
                    IrideMp3BackgroundColor,
                ),
            )
        }
        Box(Modifier.fillMaxSize().background(gradient))
    } else {
        BetterAnimatedGradientBackground(
            thumbnail = artwork,
            incomingThumbnail = incomingArtwork,
            crossfadeProgress = crossfadeProgress,
            grainOpacity = IrideFolio.LyricsGrainOpacity,
        )
    }
}
