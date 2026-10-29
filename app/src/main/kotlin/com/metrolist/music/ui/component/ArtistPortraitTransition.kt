package com.metrolist.music.ui.component

import androidx.compose.animation.EnterExitState
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.core.tween
import androidx.compose.runtime.DisposableEffect
import androidx.navigation.NavController
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import com.metrolist.music.ui.utils.IrideMotion
import com.metrolist.music.ui.utils.rememberReducedMotion

internal data class ArtistPortraitSeed(
    val sourceKey: String,
    val artistId: String,
    val name: String,
    val artwork: IrideArtistArtwork,
    val thumbnailUrl: String?,
)

internal class ArtistPortraitNavigation {
    var seed: ArtistPortraitSeed? by mutableStateOf(null)
        private set

    fun upgrade(artistId: String, artwork: IrideArtistArtwork) {
        val current = seed ?: return
        if (current.artistId == artistId && artwork.bitmap.width > current.artwork.bitmap.width) {
            seed = current.copy(artwork = artwork)
        }
    }

    fun select(sourceKey: String, artistId: String, name: String, artwork: IrideArtistArtwork, thumbnailUrl: String?) {
        seed = ArtistPortraitSeed(sourceKey, artistId, name, artwork, thumbnailUrl)
    }
}

internal val LocalArtistPortraitTransitionActive = staticCompositionLocalOf { false }

internal val LocalArtistPortraitNavigation =
    staticCompositionLocalOf<ArtistPortraitNavigation?> { null }

@Composable
internal fun IrideArtistNavigationTransitions(navController: NavController, content: @Composable () -> Unit) {
    val portraits = remember { ArtistPortraitNavigation() }
    var active by remember(navController) { mutableStateOf(false) }
    DisposableEffect(navController, portraits) {
        var previous = navController.currentBackStackEntry
        val listener = NavController.OnDestinationChangedListener { controller, _, _ ->
            val next = controller.currentBackStackEntry
            if (next?.id != previous?.id) {
                val from = previous
                val artistEntry = when {
                    from?.destination?.route == "home" -> next
                    next?.destination?.route == "home" -> from
                    else -> null
                }
                val seed = portraits.seed
                // The source token identifies this exact Home tile and artist back-stack entry.
                active = seed != null &&
                    artistEntry?.destination?.route?.startsWith("artist/{artistId}?") == true &&
                    artistEntry.arguments?.getString("artistId") == seed.artistId &&
                    artistEntry.arguments?.getString("portraitSource") == seed.sourceKey
                previous = next
            }
        }
        navController.addOnDestinationChangedListener(listener)
        onDispose { navController.removeOnDestinationChangedListener(listener) }
    }
    SharedTransitionLayout {
        CompositionLocalProvider(
            LocalSharedTransitionScope provides this,
            LocalArtistPortraitNavigation provides portraits,
            LocalArtistPortraitTransitionActive provides active,
        ) {
            content()
        }
    }
}

/** Only the tapped tile participates, even when the same artist occurs in several shelves. */
@Composable
internal fun artistPortraitSharedModifier(artistId: String, sourceKey: String? = null): Modifier {
    val seed = LocalArtistPortraitNavigation.current?.seed
    val sharedScope = LocalSharedTransitionScope.current
    val visibilityScope = LocalAnimatedVisibilityScope.current
    val reducedMotion = rememberReducedMotion()
    if (
        !LocalArtistPortraitTransitionActive.current ||
            reducedMotion ||
            seed?.artistId != artistId ||
            sharedScope == null ||
            visibilityScope == null ||
            (sourceKey != null && sourceKey != seed.sourceKey)
    )
        return Modifier

    return with(sharedScope) {
        Modifier.sharedElement(
            sharedContentState = rememberSharedContentState("artist-portrait-${seed.sourceKey}"),
            animatedVisibilityScope = visibilityScope,
            boundsTransform = { _, _ ->
                tween(ArtistPortraitMotion.Duration, easing = ArtistPortraitMotion.Easing)
            },
        )
    }
}

internal object ArtistPortraitMotion {
    const val Duration = 460
    const val ReturnLead = 100
    const val Dissolve = 300
    val Easing = androidx.compose.animation.core.CubicBezierEasing(0.42f, 0f, 0.58f, 1f)
}

/** Keep the page's ink and controls above the portrait while it moves in the shared overlay. */
@Composable
internal fun artistPortraitOverlayModifier(zIndex: Float): Modifier {
    val scope = LocalSharedTransitionScope.current
    if (!LocalArtistPortraitTransitionActive.current || scope == null || rememberReducedMotion()) return Modifier
    return with(scope) { Modifier.renderInSharedTransitionScopeOverlay(zIndexInOverlay = zIndex) }
}
