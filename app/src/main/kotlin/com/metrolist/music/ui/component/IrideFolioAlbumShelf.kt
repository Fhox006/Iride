package com.metrolist.music.ui.component

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.metrolist.innertube.models.AlbumItem
import kotlinx.coroutines.CoroutineScope

@Composable
fun IrideFolioAlbumShelf(
    albums: List<AlbumItem>,
    loading: Boolean,
    activeAlbumId: String?,
    isPlaying: Boolean,
    coroutineScope: CoroutineScope,
    onClick: (AlbumItem) -> Unit,
    onLongClick: (AlbumItem) -> Unit,
) {
    MaterialTheme(typography = irideFolioShelfTypography()) {
        if (albums.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxWidth().padding(horizontal = IrideFolio.HorizontalPadding, vertical = 20.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                IrideLoadingIndicator()
            }
        } else {
            SharedYouTubePlaylistRow(albums, key = { it.id }, contentPadding = IrideFolio.HorizontalPadding) { album ->
                YouTubeGridItem(
                    item = album,
                    isActive = activeAlbumId == album.id,
                    isPlaying = isPlaying,
                    coroutineScope = coroutineScope,
                    modifier = Modifier.combinedClickable(onClick = { onClick(album) }, onLongClick = { onLongClick(album) }),
                )
            }
        }
    }
}
