/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.component

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.flow.distinctUntilChanged

/**
 * Invokes [onVisible] with the set of item keys (as [String]) currently laid out within
 * [listState]'s viewport, every time that set changes. Used to clear "unseen" dots only when a row
 * actually scrolls on screen, as opposed to merely being composed off-screen in a carousel.
 */
@Composable
fun rememberNewlyVisibleKeys(listState: LazyListState, onVisible: (Set<String>) -> Unit) {
    LaunchedEffect(listState) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo.mapNotNull { it.key as? String }.toSet() }
            .distinctUntilChanged()
            .collect(onVisible)
    }
}

/** [rememberNewlyVisibleKeys] for the [LazyGridState]-based carousels ([SongCarousel]). */
@Composable
fun rememberNewlyVisibleKeys(gridState: LazyGridState, onVisible: (Set<String>) -> Unit) {
    LaunchedEffect(gridState) {
        snapshotFlow { gridState.layoutInfo.visibleItemsInfo.mapNotNull { it.key as? String }.toSet() }
            .distinctUntilChanged()
            .collect(onVisible)
    }
}
