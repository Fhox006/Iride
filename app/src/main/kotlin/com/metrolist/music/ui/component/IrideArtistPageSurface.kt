package com.metrolist.music.ui.component

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.metrolist.music.LocalDatabase

/** Artist subpages inherit the same pinned portrait pigment and ink as the profile. */
@Composable
internal fun IrideArtistPageSurface(artistId: String, content: @Composable () -> Unit) {
    val database = LocalDatabase.current
    val artist by remember(database, artistId) { database.artist(artistId) }.collectAsState(null)
    val entity = artist?.artist
    val artwork by rememberIrideArtistArtwork(
        artistId = artistId,
        url = entity?.thumbnailUrl,
        artistName = entity?.name,
        channelId = entity?.channelId,
        externalEnabled = artistId.startsWith("UC") && entity?.isPodcastChannel != true &&
            entity?.isPrivatelyOwnedArtist != true && entity?.isLocal != true,
        highResolution = false,
    )
    val tint = artwork?.tint ?: Color.Black
    IrideArtworkSurface(
        backgroundColor = tint,
        contentColor = if (artistArtworkUsesDarkInk(tint.toArgb())) Color.Black else Color.White,
        modifier = Modifier.fillMaxSize(),
    ) { content() }
}
