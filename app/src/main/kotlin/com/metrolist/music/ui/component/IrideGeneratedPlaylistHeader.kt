package com.metrolist.music.ui.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import com.metrolist.music.ui.theme.IBMPlexMonoFontFamily
import com.metrolist.music.ui.theme.CollectionArtworkShape

/** Shared artwork, title and metadata layout for automatic playlists and daily charts. */
@Composable
fun IrideGeneratedPlaylistHeader(
    title: String,
    metadataLine: String,
    modifier: Modifier = Modifier,
    playlistId: String? = null,
    artwork: (@Composable (Modifier) -> Unit)? = null,
) {
    val coverShape = CollectionArtworkShape
    Column(
        modifier = modifier.fillMaxWidth().padding(horizontal = IrideFolio.HorizontalPadding)
            .padding(top = IrideFolio.GeneratedPlaylistTopPadding, bottom = IrideFolio.GeneratedPlaylistBottomPadding),
    ) {
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            val coverModifier = Modifier.size(IrideFolio.GeneratedPlaylistArtworkSize).shadow(
                IrideFolio.GeneratedPlaylistArtworkShadow,
                coverShape,
                spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
            )
            if (artwork != null) {
                artwork(coverModifier)
            } else {
                PlaylistArtwork(
                    title = title,
                    playlistId = playlistId,
                    modifier = coverModifier,
                    expansionTarget = true,
                )
            }
        }
        Spacer(Modifier.height(IrideFolio.GeneratedPlaylistTitleGap))
        IrideCollectionTitle(title = title)
        Spacer(Modifier.height(IrideFolio.GeneratedPlaylistMetadataGap))
        Text(
            text = metadataLine,
            style = TextStyle(fontFamily = IBMPlexMonoFontFamily, fontSize = IrideFolio.GeneratedPlaylistMetadataFontSize),
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f),
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
