/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.media3.exoplayer.offline.Download
import com.metrolist.music.R
import com.metrolist.music.ui.utils.pressScale

/**
 * New Iride UI: the playlist/liked-songs transport controls — shuffle, play, download — as one
 * pill-shaped console housing all three, shuffle and download hugging the play button on either
 * side rather than spread across the row.
 */
@Composable
fun IridePlaylistControlPanel(
    onShuffleClick: () -> Unit,
    onPlayClick: () -> Unit,
    onDownloadClick: () -> Unit,
    downloadState: Int,
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        val pillShape = RoundedCornerShape(percent = 50)
        Row(
            modifier = Modifier
                .clip(pillShape)
                .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.14f)), pillShape)
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            IrideOutlineIconButton(
                onClick = onShuffleClick,
                icon = R.drawable.shuffle,
                contentDescription = stringResource(R.string.shuffle),
                size = 44.dp,
                iconSize = 20.dp,
                pressEffect = IridePressEffect.Spin,
            )

            IridePlayButton(onClick = onPlayClick, isPlaying = isPlaying)

            IrideOutlineIconButton(
                onClick = onDownloadClick,
                icon = when (downloadState) {
                    Download.STATE_COMPLETED -> R.drawable.check
                    else -> R.drawable.arrow_downward
                },
                contentDescription = null,
                loading = downloadState == Download.STATE_DOWNLOADING || downloadState == Download.STATE_QUEUED,
                size = 44.dp,
                iconSize = 20.dp,
                pressEffect = IridePressEffect.Pulse,
            )
        }
    }
}

@Composable
private fun IridePlayButton(
    onClick: () -> Unit,
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        modifier = modifier
            .size(64.dp)
            .pressScale(interactionSource, pressedScale = 0.92f)
            .clip(CircleShape)
            .border(BorderStroke(1.5.dp, Color.White.copy(alpha = 0.22f)), CircleShape)
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(bounded = true, radius = 32.dp),
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(if (isPlaying) R.drawable.ic_iride_pause else R.drawable.ic_iride_play),
            contentDescription = stringResource(if (isPlaying) R.string.pause else R.string.play),
            tint = Color.White,
            // Optical nudge: the play glyph's mass sits left of true center, reads off-center in a
            // symmetric circle unless shifted right. The pause glyph is already symmetric.
            modifier = Modifier
                .size(26.dp)
                .offset(x = if (isPlaying) 0.dp else 1.5.dp),
        )
    }
}
