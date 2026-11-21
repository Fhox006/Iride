/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.metrolist.music.R

/** Playlist and album actions use Folio's shared collection control panel. */
@Composable
fun IridePlaylistControlPanel(
    onShuffleClick: () -> Unit,
    onPlayClick: () -> Unit,
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
) {
    IrideFolioCollectionControlPanel(
        leadingAction = IrideFolioCollectionAction(
            icon = R.drawable.shuffle,
            contentDescription = stringResource(R.string.shuffle),
            onClick = onShuffleClick,
            pressEffect = IridePressEffect.Spin,
        ),
        onPlayClick = onPlayClick,
        isPlaying = isPlaying,
        modifier = modifier,
    )
}
