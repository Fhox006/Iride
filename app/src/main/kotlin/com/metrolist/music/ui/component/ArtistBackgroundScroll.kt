package com.metrolist.music.ui.component

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow

internal class ArtistBackgroundScrollState(val distance: State<Float>, val albumsStart: State<Float>)

@Composable
internal fun rememberArtistBackgroundScroll(
    state: LazyListState,
    artistId: String,
    albumKeys: Set<String>,
): ArtistBackgroundScrollState {
    val distance = remember(state, artistId) { mutableFloatStateOf(0f) }
    val albumsStart = remember(state, artistId, albumKeys) { mutableFloatStateOf(Float.NaN) }
    val tracker = remember(state, artistId) { ArtistBackgroundScrollTracker() }
    LaunchedEffect(state, artistId, albumKeys) {
        snapshotFlow {
            Triple(
                state.firstVisibleItemIndex,
                state.firstVisibleItemScrollOffset,
                state.layoutInfo.visibleItemsInfo.map { ArtistScrollAnchor(it.offset, it.size, it.key) },
            )
        }.collect { (index, offset, visible) ->
            distance.floatValue = tracker.update(index, offset, visible)
            val album = visible.firstOrNull { it.key is String && it.key in albumKeys }
            if (album != null) {
                val start = distance.floatValue + album.offset
                albumsStart.floatValue = if (albumsStart.floatValue.isNaN()) start else minOf(albumsStart.floatValue, start)
            }
        }
    }
    return remember(distance, albumsStart) { ArtistBackgroundScrollState(distance, albumsStart) }
}
