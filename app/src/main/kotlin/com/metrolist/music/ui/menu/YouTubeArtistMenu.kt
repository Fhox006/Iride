/** Metrolist Project (C) 2026 Licensed under GPL-3.0 | See git history for contributors */
package com.metrolist.music.ui.menu

import android.content.Intent
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.metrolist.innertube.models.ArtistItem
import com.metrolist.music.R
import com.metrolist.music.ui.component.IrideFolioContextMenu
import com.metrolist.music.ui.component.folioMenuAction

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun YouTubeArtistMenu(artist: ArtistItem, onDismiss: () -> Unit) {
    val context = LocalContext.current

    IrideFolioContextMenu(
        items =
            buildList {
                add(
                    folioMenuAction(
                        R.string.share,
                        R.drawable.share,
                        onClick = {
                            context.startActivity(
                                Intent.createChooser(
                                    Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(Intent.EXTRA_TEXT, artist.shareLink)
                                    },
                                    null,
                                )
                            )
                        },
                    )
                )
            },
        visible = true,
        onDismiss = onDismiss,
        retainOpenActions = emptySet(),
    )
}
