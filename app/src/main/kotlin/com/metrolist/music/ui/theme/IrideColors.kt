/**
 * Iride Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color

/**
 * Semantic color hierarchy for the monochrome Iride identity.
 *
 * All tokens resolve against [ColorScheme.onSurface], which is pure white in
 * dark theme and near-black in light theme. Replacing a hard-coded Color.White
 * with a token therefore keeps dark mode pixel-identical while letting light
 * mode adapt automatically ("mirror" rule).
 *
 * Text hierarchy:
 *  - [textPrimary]   titles, main labels, active icons
 *  - [textSecondary] subtitles, descriptions, secondary icons
 *  - [textTertiary]  hints, weak metadata, disabled icons
 *
 * Surface hierarchy:
 *  - [strokeHairline] dividers and hairline borders
 *  - [strokeCard]     strokes around items, artwork outlines, cards
 *  - [fillSubtle]     subtle surface fills, banners
 *  - [fillSelected]   chips, selected segments, emphasized fills
 *
 * Content drawn over media (player gradients, artwork, Wrapped stories) keeps
 * pure white on purpose and must NOT use these tokens.
 */
val ColorScheme.textPrimary: Color get() = onSurface

val ColorScheme.textSecondary: Color get() = onSurfaceVariant

val ColorScheme.textTertiary: Color get() = onSurface.copy(alpha = 0.45f)

val ColorScheme.strokeHairline: Color get() = onSurface.copy(alpha = 0.10f)

val ColorScheme.strokeCard: Color get() = onSurface.copy(alpha = 0.22f)

val ColorScheme.fillSubtle: Color get() = onSurface.copy(alpha = 0.06f)

val ColorScheme.fillSelected: Color get() = onSurface.copy(alpha = 0.14f)
