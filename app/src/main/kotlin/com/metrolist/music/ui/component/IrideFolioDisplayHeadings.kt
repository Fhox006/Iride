package com.metrolist.music.ui.component

import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextGeometricTransform
import androidx.compose.ui.unit.sp
import com.metrolist.music.ui.theme.IBMPlexMonoFontFamily
import com.metrolist.music.ui.theme.SatoshiFontFamily
import java.util.Locale

internal data class FolioDisplayHeadings(val page: TextStyle, val shelf: TextStyle)

internal val LocalFolioDisplayHeadings = staticCompositionLocalOf<FolioDisplayHeadings?> { null }
internal val LocalFolioTypewriterFont = staticCompositionLocalOf<FontFamily?> { null }

@Composable
internal fun folioTypewriterFontFamily(): FontFamily =
    LocalFolioTypewriterFont.current ?: IBMPlexMonoFontFamily

/** Two coordinated roles, scoped to the Home experiment rather than the application theme. */
internal enum class FolioHomeTypeRole {
    Typewriter,
    Display,
}

internal fun String.homeSentenceCase(): String =
    lowercase(Locale.getDefault()).replaceFirstChar { it.titlecase(Locale.getDefault()) }

internal fun TextStyle.homeType(role: FolioHomeTypeRole): TextStyle =
    when (role) {
        FolioHomeTypeRole.Typewriter ->
            copy(
                fontFamily = IBMPlexMonoFontFamily,
                fontWeight = FontWeight.Normal,
                textGeometricTransform = TextGeometricTransform(),
            )
        FolioHomeTypeRole.Display ->
            copy(
                fontFamily = SatoshiFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = fontSize * IrideFolio.SatoshiTextHeightScale,
                textGeometricTransform = TextGeometricTransform(),
            )
    }

private fun Typography.homeTypewriter(): Typography =
    copy(
        displayLarge = displayLarge.homeType(FolioHomeTypeRole.Typewriter),
        displayMedium = displayMedium.homeType(FolioHomeTypeRole.Typewriter),
        displaySmall = displaySmall.homeType(FolioHomeTypeRole.Typewriter),
        headlineLarge = headlineLarge.homeType(FolioHomeTypeRole.Typewriter),
        headlineMedium = headlineMedium.homeType(FolioHomeTypeRole.Typewriter),
        headlineSmall = headlineSmall.homeType(FolioHomeTypeRole.Typewriter),
        titleLarge = titleLarge.homeType(FolioHomeTypeRole.Typewriter),
        titleMedium = titleMedium.homeType(FolioHomeTypeRole.Typewriter),
        titleSmall = titleSmall.homeType(FolioHomeTypeRole.Typewriter),
        bodyLarge = bodyLarge.homeType(FolioHomeTypeRole.Typewriter),
        bodyMedium = bodyMedium.homeType(FolioHomeTypeRole.Typewriter),
        bodySmall = bodySmall.homeType(FolioHomeTypeRole.Typewriter),
        labelLarge = labelLarge.homeType(FolioHomeTypeRole.Typewriter),
        labelMedium = labelMedium.homeType(FolioHomeTypeRole.Typewriter),
        labelSmall = labelSmall.homeType(FolioHomeTypeRole.Typewriter),
    )

@Composable
internal fun IrideFolioDisplayHeadings(menuState: MenuState, content: @Composable () -> Unit) {
    val typography = MaterialTheme.typography.homeTypewriter()
    val headings =
        FolioDisplayHeadings(
            page =
                typography.headlineLarge
                    .homeType(FolioHomeTypeRole.Display)
                    .copy(
                        fontSize = 34.sp * IrideFolio.SatoshiTextHeightScale,
                        lineHeight = 42.sp,
                        letterSpacing = (-0.3).sp,
                    ),
            shelf =
                typography.titleMedium
                    .homeType(FolioHomeTypeRole.Display)
                    .copy(
                        fontSize = 20.sp * IrideFolio.SatoshiTextHeightScale,
                        lineHeight = 27.sp,
                        letterSpacing = 0.sp,
                        textGeometricTransform = TextGeometricTransform(scaleX = 1.1f),
                    ),
        )
    val entranceOrder = remember { IrideFolioSectionEntranceOrder() }
    DisposableEffect(menuState, typography) {
        val previous = menuState.sourceTypography
        menuState.sourceTypography = typography
        onDispose { menuState.sourceTypography = previous }
    }
    MaterialTheme(typography = typography) {
        CompositionLocalProvider(
            LocalFolioDisplayHeadings provides headings,
            LocalFolioTypewriterFont provides IBMPlexMonoFontFamily,
            LocalFolioSectionEntranceOrder provides entranceOrder,
            LocalTextStyle provides typography.bodyLarge,
        ) {
            content()
        }
    }
}
