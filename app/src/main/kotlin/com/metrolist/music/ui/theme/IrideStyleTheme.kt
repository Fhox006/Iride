package com.metrolist.music.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.graphics.Color

// Iride Style BETA — Warm Dark Palette
// Background anchor: #25221B (warm dark sepia-ink)
// Seed hues: Red-rust #9E332F → primary, Magenta #943045 → secondary,
//            Violet #6F3782 → tertiary, Deep violet #533782 → containers,
//            Indigo #3B378D → fixed/dim roles
val IrideStyleColorScheme = darkColorScheme(
    primary                = Color(0xFFFFB3AE),
    onPrimary              = Color(0xFF561915),
    primaryContainer       = Color(0xFF7D1B18),
    onPrimaryContainer     = Color(0xFFFFDAD8),

    secondary              = Color(0xFFFFB1C2),
    onSecondary            = Color(0xFF5E1228),
    secondaryContainer     = Color(0xFF7A273E),
    onSecondaryContainer   = Color(0xFFFFD9E2),

    tertiary               = Color(0xFFDFB4FF),
    onTertiary             = Color(0xFF4A1070),
    tertiaryContainer      = Color(0xFF622888),
    onTertiaryContainer    = Color(0xFFF3DAFF),

    error                  = Color(0xFFFFB4AB),
    onError                = Color(0xFF690005),
    errorContainer         = Color(0xFF93000A),
    onErrorContainer       = Color(0xFFFFDAD6),

    background             = Color(0xFF25221B),
    onBackground           = Color(0xFFE8E1D8),

    surface                = Color(0xFF25221B),
    onSurface              = Color(0xFFE8E1D8),
    surfaceVariant         = Color(0xFF33302A),
    onSurfaceVariant       = Color(0xFFCEC4B8),

    surfaceContainerLowest = Color(0xFF1C1A14),
    surfaceContainerLow    = Color(0xFF2D2A23),
    surfaceContainer       = Color(0xFF322F27),
    surfaceContainerHigh   = Color(0xFF3D392F),
    surfaceContainerHighest= Color(0xFF48443A),

    inverseSurface         = Color(0xFFE8E1D8),
    inverseOnSurface       = Color(0xFF32302A),
    inversePrimary         = Color(0xFF9E332F),

    outline                = Color(0xFF998F84),
    outlineVariant         = Color(0xFF4D4840),

    scrim                  = Color(0xFF000000),
    surfaceBright          = Color(0xFF4A463C),
    surfaceDim             = Color(0xFF25221B),
)
