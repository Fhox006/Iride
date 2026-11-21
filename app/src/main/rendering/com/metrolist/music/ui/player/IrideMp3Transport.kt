package com.metrolist.music.ui.player

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.metrolist.music.R
import com.metrolist.music.ui.utils.pressScale

@Composable
internal fun IrideMp3Transport(
    geometry: IrideMp3WheelGeometry,
    isPlaying: Boolean,
    isGuest: Boolean,
    isMuted: Boolean,
    canSkipNext: Boolean,
    reducedMotion: Boolean,
    onPlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
) {
    Box(
        Modifier.requiredSize(geometry.diameter, geometry.centerSize),
        contentAlignment = Alignment.Center,
    ) {
        WheelZone(Alignment.CenterStart, geometry.zoneSize, 0.dp, onPrevious, enabled = !isGuest) {
            Icon(
                painterResource(R.drawable.ic_iride_fast_forward_filled),
                stringResource(R.string.mp3_previous),
                tint = if (isGuest) IrideMp3DimIconColor else Color.White,
                modifier = Modifier.size(geometry.iconSize).graphicsLayer { scaleX = -1f },
            )
        }
        WheelZone(
            Alignment.CenterEnd,
            geometry.zoneSize,
            0.dp,
            onNext,
            enabled = !isGuest && canSkipNext,
        ) {
            Icon(
                painterResource(R.drawable.ic_iride_fast_forward_filled),
                stringResource(R.string.mp3_next),
                tint = if (isGuest || !canSkipNext) IrideMp3DimIconColor else Color.White,
                modifier = Modifier.size(geometry.iconSize),
            )
        }
        val interaction = remember { MutableInteractionSource() }
        val label =
            stringResource(
                if (isGuest) R.string.mp3_mute
                else if (isPlaying) R.string.mp3_pause else R.string.mp3_play
            )
        Box(
            Modifier.size(geometry.centerSize)
                .pressScale(interaction, pressedScale = .88f)
                .clip(CircleShape)
                .clickable(
                    interactionSource = interaction,
                    indication = null,
                    onClickLabel = label,
                    onClick = onPlayPause,
                ),
            contentAlignment = Alignment.Center,
        ) {
            if (isGuest)
                Icon(
                    painterResource(if (isMuted) R.drawable.volume_off else R.drawable.volume_up),
                    label,
                    tint = Color.White,
                    modifier = Modifier.size(geometry.iconSize),
                )
            else
                IrideMp3PlaybackGlyph(
                    isPlaying,
                    reducedMotion,
                    Modifier.size(geometry.iconSize).semantics { contentDescription = label },
                )
        }
    }
}
