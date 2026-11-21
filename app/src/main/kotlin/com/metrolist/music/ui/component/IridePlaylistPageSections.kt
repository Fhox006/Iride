package com.metrolist.music.ui.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer

private const val PlaylistSearchRevealStartProgress = 0.88f

/** Controls which shared sections a playlist page renders. */
@Immutable
internal data class PlaylistPageCapabilities(
    val showHeader: Boolean = true,
    val showPlaybackControls: Boolean = true,
    val showSearch: Boolean = true,
    val showSecondaryControls: Boolean = false,
    val showFilters: Boolean = false,
)

/** Keeps common playlist sections in one order while pages supply source-specific content. */
internal fun LazyListScope.playlistPageSections(
    capabilities: PlaylistPageCapabilities,
    header: (@Composable LazyItemScope.() -> Unit)? = null,
    playbackControls: (@Composable LazyItemScope.() -> Unit)? = null,
    search: (@Composable LazyItemScope.() -> Unit)? = null,
    secondaryControls: (@Composable LazyItemScope.() -> Unit)? = null,
    filters: (@Composable LazyItemScope.() -> Unit)? = null,
    filterKey: String = "genre_pills",
    secondaryControlsKey: String = "playlist_secondary_controls",
) {
    if (capabilities.showHeader && header != null) {
        item(key = "playlist_header") { header() }
    }
    if (capabilities.showPlaybackControls && playbackControls != null) {
        item(key = "control_panel") { playbackControls() }
    }
    if (capabilities.showSearch && search != null) {
        item(key = "search_bar") {
            val expansionProgress = LocalCollectionExpansionProgress.current
            Box(
                modifier = Modifier.graphicsLayer {
                    val progress = expansionProgress?.value ?: 1f
                    alpha = ((progress - PlaylistSearchRevealStartProgress) /
                        (1f - PlaylistSearchRevealStartProgress)).coerceIn(0f, 1f)
                },
            ) {
                search()
            }
        }
    }
    if (capabilities.showSecondaryControls && secondaryControls != null) {
        item(key = secondaryControlsKey) { secondaryControls() }
    }
    if (capabilities.showFilters && filters != null) {
        item(key = filterKey) { filters() }
    }
}
