/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.media3.exoplayer.offline.Download
import com.metrolist.music.R

/** Playlist and album actions use Folio's shared collection control panel. */
@Composable
fun IridePlaylistControlPanel(
    onShuffleClick: () -> Unit,
    onPlayClick: () -> Unit,
    onDownloadClick: () -> Unit,
    downloadState: Int,
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
) {
    val downloading = downloadState == Download.STATE_DOWNLOADING || downloadState == Download.STATE_QUEUED
    IrideFolioCollectionControlPanel(
        leadingAction = IrideFolioCollectionAction(
            icon = R.drawable.shuffle,
            contentDescription = stringResource(R.string.shuffle),
            onClick = onShuffleClick,
            pressEffect = IridePressEffect.Spin,
        ),
        onPlayClick = onPlayClick,
        isPlaying = isPlaying,
        trailingAction = IrideFolioCollectionAction(
            icon = if (downloadState == Download.STATE_COMPLETED) R.drawable.check else R.drawable.arrow_downward,
            contentDescription = stringResource(
                when {
                    downloadState == Download.STATE_COMPLETED -> R.string.remove_download
                    downloading -> R.string.downloading
                    else -> R.string.action_download
                },
            ),
            onClick = onDownloadClick,
            loading = downloading,
            pressEffect = IridePressEffect.Pulse,
        ),
        modifier = modifier,
    )
}
