/** Metrolist Project (C) 2026 Licensed under GPL-3.0 | See git history for contributors */
package com.metrolist.music.ui.menu

import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.platform.LocalContext
import com.metrolist.music.LocalDatabase
import com.metrolist.music.R
import com.metrolist.music.db.entities.Artist
import com.metrolist.music.ui.component.IrideFolioContextMenu
import com.metrolist.music.ui.component.folioMenuAction
import kotlinx.coroutines.CoroutineScope

@Composable
fun ArtistMenu(
    originalArtist: Artist,
    coroutineScope: CoroutineScope,
    onDismiss: () -> Unit,
    onSelectLibraryView: ((Boolean) -> Unit)? = null,
    libraryViewActive: Boolean = false,
) {
    val context = LocalContext.current
    val database = LocalDatabase.current
    val artistState = database.artist(originalArtist.id).collectAsState(initial = originalArtist)
    val artist = artistState.value ?: originalArtist

    IrideFolioContextMenu(
        items =
            buildList {
                if (onSelectLibraryView != null) {
                    add(
                        folioMenuAction(
                            label = R.string.filter_library,
                            icon = if (libraryViewActive) R.drawable.bookmark_filled else R.drawable.bookmark_outlined,
                            onClick = { onSelectLibraryView(true) },
                            selected = libraryViewActive,
                            section = R.string.filter_library,
                        )
                    )
                    add(
                        folioMenuAction(
                            label = R.string.online,
                            icon = R.drawable.language,
                            onClick = { onSelectLibraryView(false) },
                            selected = !libraryViewActive,
                            section = R.string.filter_library,
                        )
                    )
                }

                if (artist.artist.isYouTubeArtist)
                    add(
                        folioMenuAction(
                            R.string.share,
                            R.drawable.share,
                            section = R.string.share,
                            onClick = {
                                context.startActivity(
                                    Intent.createChooser(
                                        Intent(Intent.ACTION_SEND).apply {
                                            type = "text/plain"
                                            putExtra(
                                                Intent.EXTRA_TEXT,
                                                "https://music.youtube.com/channel/${artist.id}",
                                            )
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
