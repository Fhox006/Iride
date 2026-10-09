package com.metrolist.music.ui.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.navigation.NavBackStackEntry
import com.metrolist.music.ui.utils.IrideMotion
import com.metrolist.music.ui.utils.rememberReducedMotion

internal val LocalArtworkContentColor = staticCompositionLocalOf<Color?> { null }
internal val LocalArtworkBackgroundColor = staticCompositionLocalOf<Color?> { null }
// Navigation is a sibling of the page, so it cannot inherit the artwork's local theme.
internal val LocalPageNavigationAppearance = staticCompositionLocalOf<PageNavigationAppearance?> { null }
internal val LocalFloatingPanelAppearance = compositionLocalOf { FloatingPanelAppearance() }

@Immutable
internal data class FloatingPanelAppearance(
    val contentColor: Color = Color.White,
    val backgroundColor: Color = Color.Black,
    val surfaceAlpha: Float = IrideFolio.FloatingPillSurfaceOpacity,
)

/** Animate both floating surfaces together, retaining the current colors across page changes. */
@Composable
internal fun rememberFloatingPanelAppearance(
    pageAppearance: PageNavigationAppearance,
    backdrop: FrostBackdrop?,
): FloatingPanelAppearance {
    val reducedMotion = rememberReducedMotion()
    val targetContent = pageAppearance.contentColor ?: Color.White
    val targetBackground = pageAppearance.backgroundColor
    val content by animateColorAsState(
        targetContent,
        animationSpec = if (reducedMotion) snap() else tween(IrideMotion.Medium, easing = IrideMotion.EaseOutQuart),
        label = "floatingPanelContent",
    )
    val background by animateColorAsState(
        IrideFolio.frostedSurfaceColor(targetContent, targetBackground),
        animationSpec = if (reducedMotion) snap() else tween(IrideMotion.Medium, easing = IrideMotion.EaseOutQuart),
        label = "floatingPanelBackground",
    )
    val surfaceAlpha by animateFloatAsState(
        IrideFolio.floatingPillSurfaceAlpha(backdrop, targetBackground),
        animationSpec = if (reducedMotion) snap() else tween(IrideMotion.Medium, easing = IrideMotion.EaseOutQuart),
        label = "floatingPanelTintAlpha",
    )
    return FloatingPanelAppearance(content, background, surfaceAlpha)
}

private data class PagePanelColors(val background: Color, val content: Color)

internal class PageNavigationAppearance(private val entryId: String?) {
    private var colors by mutableStateOf<PagePanelColors?>(null)
    val contentColor: Color? get() = colors?.content
    val backgroundColor: Color? get() = colors?.background

    fun updateColors(pageEntryId: String?, background: Color, content: Color) {
        // Outgoing pages remain composed during transitions and must not recolor the new page.
        if (pageEntryId == entryId) colors = PagePanelColors(background, content)
    }
}

/** A pigment and its ink switch together, avoiding a frame of dark text on a dark background. */
@Composable
internal fun IrideArtworkSurface(
    backgroundColor: Color,
    modifier: Modifier = Modifier,
    paintBackground: Boolean = true,
    contentColor: Color? = null,
    content: @Composable BoxScope.() -> Unit,
) {
    val ink = contentColor ?: if (artworkUsesDarkInk(backgroundColor.toArgb())) Color.Black else Color.White
    val navigationAppearance = LocalPageNavigationAppearance.current
    val pageEntryId = (LocalViewModelStoreOwner.current as? NavBackStackEntry)?.id
    SideEffect { navigationAppearance?.updateColors(pageEntryId, backgroundColor, ink) }
    MaterialTheme(
        colorScheme = MaterialTheme.colorScheme.copy(
            primary = ink, secondary = ink, tertiary = ink,
            onPrimary = backgroundColor, onSecondary = backgroundColor, onTertiary = backgroundColor,
            background = backgroundColor, surface = backgroundColor,
            surfaceVariant = backgroundColor, surfaceContainer = backgroundColor,
            surfaceContainerHigh = backgroundColor, surfaceContainerHighest = backgroundColor,
            surfaceContainerLow = backgroundColor, surfaceContainerLowest = backgroundColor,
            onBackground = ink, onSurface = ink, onSurfaceVariant = ink,
        ),
    ) {
        CompositionLocalProvider(
            LocalContentColor provides ink,
            LocalLibraryTextColor provides ink,
            LocalArtworkContentColor provides ink,
            LocalArtworkBackgroundColor provides backgroundColor,
            LocalIrideFolioScrimColor provides backgroundColor,
        ) {
            Box(if (paintBackground) modifier.background(backgroundColor) else modifier, content = content)
        }
    }
}
