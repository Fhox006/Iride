package com.metrolist.music.ui.component

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import com.metrolist.innertube.models.SongItem
import com.metrolist.innertube.models.YTItem

/**
 * Audio always uses compact rows. Only collections, artists and real videos occupy artwork tiles.
 */
@Composable
fun IrideFolioMediaShelf(
    items: List<YTItem>,
    contentPadding: Dp = IrideFolio.HorizontalPadding,
    songContent: @Composable (SongItem, Dp) -> Unit,
    tileContent: @Composable (YTItem) -> Unit,
) {
    val songs = remember(items) { items.filterIsInstance<SongItem>().filterNot { it.isVideoSong } }
    val tiles = remember(items) { items.filterNot { it is SongItem && !it.isVideoSong } }
    Column(Modifier.fillMaxWidth()) {
        if (songs.isNotEmpty()) {
            SharedQuickPicksCarousel(
                items = songs,
                key = { it.id },
                loading = false,
                contentPadding = contentPadding,
                usePager = true,
                minimumRows = 1,
                itemContent = songContent,
            )
        }
        if (tiles.isNotEmpty()) {
            val state = rememberLazyListState()
            LazyRow(
                state = state,
                contentPadding = PaddingValues(horizontal = contentPadding),
                horizontalArrangement = Arrangement.spacedBy(IrideFolio.ShelfItemSpacing),
                overscrollEffect = null,
                modifier =
                    Modifier.fillMaxWidth().rubberBandOverscroll(Orientation.Horizontal, state),
            ) {
                items(tiles, key = { it.id }) { tileContent(it) }
            }
        }
    }
}
