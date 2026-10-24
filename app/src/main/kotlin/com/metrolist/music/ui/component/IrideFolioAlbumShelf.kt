package com.metrolist.music.ui.component

import com.metrolist.music.ui.component.irideFolioCombinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.metrolist.innertube.models.AlbumItem
import com.metrolist.music.R
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
    modifier: Modifier = Modifier,
) {
    MaterialTheme(typography = irideFolioShelfTypography()) {
        if (albums.isEmpty()) {
            Box(
                modifier = modifier.fillMaxWidth().padding(horizontal = IrideFolio.HorizontalPadding, vertical = 20.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                if (loading) {
                    IrideLoadingIndicator()
                } else {
                    Text(stringResource(R.string.no_results_found))
                }
            }
        } else {
            SharedYouTubePlaylistRow(albums, key = { it.id }, contentPadding = IrideFolio.HorizontalPadding, modifier = modifier) { album ->
                YouTubeGridItem(
                    item = album,
                    isActive = activeAlbumId == album.id,
                    isPlaying = isPlaying,
                    coroutineScope = coroutineScope,
                    modifier = Modifier.irideFolioCombinedClickable(onClick = { onClick(album) }, onLongClick = { onLongClick(album) }),
                )
            }
        }
    }
}
