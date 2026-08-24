/**
 * Iride Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.theme

import android.graphics.Bitmap
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.SaverScope
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.palette.graphics.Palette
import com.materialkolor.PaletteStyle
import com.materialkolor.dynamiccolor.ColorSpec
import com.materialkolor.rememberDynamicColorScheme
import com.materialkolor.score.Score
import com.metrolist.music.utils.rememberPreference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

val DefaultThemeColor = Color(0xFFED5564)

/**
 * Kill-switch for the light theme: implementation is complete but hidden from
 * settings until polished. While true, every theme resolver locks to dark
 * regardless of the stored DarkMode preference.
 */
const val ForceDarkTheme = true

@Composable
fun IrideTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    pureBlack: Boolean = false,
    themeColor: Color = DefaultThemeColor,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val dark = darkTheme || ForceDarkTheme
    val useSystemDynamicColor = (themeColor == DefaultThemeColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)

    val baseColorScheme = if (useSystemDynamicColor) {
        if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    } else {
        rememberDynamicColorScheme(
            seedColor = themeColor,
            isDark = dark,
            specVersion = ColorSpec.SpecVersion.SPEC_2025,
            style = PaletteStyle.TonalSpot
        )
    }

    // Neutral surfaces mirror the app's monochrome identity; accent roles
    // (primary/secondary/tertiary) are kept from the generated base scheme.
    val colorScheme = remember(baseColorScheme, dark) {
        if (dark) {
            baseColorScheme.copy(
                background = Color.Black,
                onBackground = Color.White,
                surface = Color.Black,
                onSurface = Color.White,
                surfaceVariant = Color(0xFF1A1A1A),
                onSurfaceVariant = Color(0xFFCCCCCC),
                surfaceContainerLowest = Color.Black,
                surfaceContainerLow = Color(0xFF0D0D0D),
                surfaceContainer = Color(0xFF121212),
                surfaceContainerHigh = Color(0xFF1E1E1E),
                surfaceContainerHighest = Color(0xFF282828),
                inverseSurface = Color.White,
                inverseOnSurface = Color.Black,
                outline = Color(0xFF8A8A8A),
                outlineVariant = Color(0xFF3A3A3A),
                scrim = Color.Black,
                surfaceBright = Color(0xFF2C2C2C),
                surfaceDim = Color.Black,
            )
        } else {
            baseColorScheme.copy(
                background = Color.White,
                onBackground = Color(0xFF1A1A1A),
                surface = Color.White,
                onSurface = Color(0xFF1A1A1A),
                surfaceVariant = Color(0xFFF0F0F0),
                onSurfaceVariant = Color(0xFF444444),
                surfaceContainerLowest = Color.White,
                surfaceContainerLow = Color(0xFFFAFAFA),
                surfaceContainer = Color(0xFFF5F5F5),
                surfaceContainerHigh = Color(0xFFEFEFEF),
                surfaceContainerHighest = Color(0xFFE5E5E5),
                inverseSurface = Color(0xFF1A1A1A),
                inverseOnSurface = Color.White,
                outline = Color(0xFF757575),
                outlineVariant = Color(0xFFDDDDDD),
                scrim = Color.Black,
                surfaceBright = Color.White,
                surfaceDim = Color(0xFFE8E8E8),
            )
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        content = content
    )
}

suspend fun Bitmap.extractThemeColor(): Color = withContext(Dispatchers.Default) {
    val colorsToPopulation = Palette.from(this@extractThemeColor)
        .maximumColorCount(8)
        .generate()
        .swatches
        .associate { it.rgb to it.population }
    val rankedColors = Score.score(colorsToPopulation)
    Color(rankedColors.first())
}

suspend fun Bitmap.extractGradientColors(): List<Color> = withContext(Dispatchers.Default) {
    val extractedColors = Palette.from(this@extractGradientColors)
        .maximumColorCount(64)
        .generate()
        .swatches
        .associate { it.rgb to it.population }
    val orderedColors = Score.score(extractedColors, 2, 0xff4285f4.toInt(), true)
        .sortedByDescending { Color(it).luminance() }
    if (orderedColors.size >= 2)
        listOf(Color(orderedColors[0]), Color(orderedColors[1]))
    else
        listOf(Color(0xFF595959), Color(0xFF0D0D0D))
}

fun ColorScheme.pureBlack(apply: Boolean) =
    if (apply) copy(
        surface = Color.Black,
        background = Color.Black
    ) else this

val ColorSaver = object : Saver<Color, Int> {
    override fun restore(value: Int): Color = Color(value)
    override fun SaverScope.save(value: Color): Int = value.toArgb()
}
