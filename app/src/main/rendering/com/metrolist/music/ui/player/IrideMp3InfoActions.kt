package com.metrolist.music.ui.player

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.metrolist.music.R
import com.metrolist.music.ui.component.IrideFolio
import com.metrolist.music.ui.component.IrideFolioActionReveal
import com.metrolist.music.ui.component.SongMenuIconButton
import com.metrolist.music.ui.utils.IrideMotion
import com.metrolist.music.ui.utils.pressScale

@Composable
internal fun IrideMp3InfoActions(
    isFavorite: Boolean,
    onFavoriteClick: () -> Unit,
    onMoreClick: () -> Unit,
    reducedMotion: Boolean,
    expansion: Float = 0f,
    fullscreenAction: Boolean = false,
    contextualIcon: Int? = null,
    contextualDescription: String = "",
    onContextualClick: () -> Unit = {},
) {
    Row(
        Modifier.padding(start = 10.dp)
            .clip(IrideFolio.ControlsShape)
            .background(IrideMp3WheelCenterColor)
            .border(1.dp, IrideMp3PanelBorderColor, IrideFolio.ControlsShape),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val favoriteInteraction = remember { MutableInteractionSource() }
        Box(
            Modifier.size(IrideFolio.ControlsTouchTargetSize)
                .pressScale(favoriteInteraction, pressedScale = 0.88f)
                .clip(CircleShape)
                .clickable(
                    interactionSource = favoriteInteraction,
                    indication = null,
                    onClick = onFavoriteClick,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Crossfade(
                targetState = isFavorite,
                animationSpec =
                    tween(
                        if (reducedMotion) 0 else IrideMotion.Quick,
                        easing = IrideMotion.EaseOutQuart,
                    ),
                label = "mp3Favorite",
                modifier = Modifier.size(22.dp),
            ) { favorite ->
                Icon(
                    painterResource(
                        if (favorite) R.drawable.ic_widget_star_nav
                        else R.drawable.ic_widget_star_outline_nav
                    ),
                    stringResource(
                        if (favorite) R.string.mp3_remove_favorite else R.string.mp3_add_favorite
                    ),
                    tint = if (favorite) Color.White else Color.White.copy(alpha = 0.7f),
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
        // All three actions retain their size and order through the display morph.
        Box(
            Modifier.width(IrideFolio.ControlsTouchTargetSize)
                .height(IrideFolio.ControlsTouchTargetSize)
                .clipToBounds()
        ) {
            SongMenuIconButton(
                onClick = onMoreClick,
                modifier =
                    Modifier.requiredSize(IrideFolio.ControlsTouchTargetSize).graphicsLayer {
                        alpha = 1f
                    },
            ) {
                Icon(
                    painterResource(R.drawable.more_vert),
                    stringResource(R.string.mp3_more),
                    tint = Color.White.copy(alpha = .85f),
                    modifier = Modifier.size(22.dp),
                )
            }
        }
        IrideFolioActionReveal(visible = contextualIcon != null) {
            val interaction = remember { MutableInteractionSource() }
            Box(
                Modifier.size(IrideFolio.ControlsTouchTargetSize)
                    .pressScale(interaction, pressedScale = .92f)
                    .clip(CircleShape)
                    .clickable(
                        interactionSource = interaction,
                        indication = null,
                        onClick = onContextualClick,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                if (fullscreenAction)
                    Box(
                        Modifier.size(22.dp).semantics {
                            contentDescription = contextualDescription
                        }
                    ) {
                        Icon(
                            painterResource(R.drawable.fullscreen),
                            null,
                            tint = Color.White.copy(alpha = .85f),
                            modifier =
                                Modifier.fillMaxSize().graphicsLayer { alpha = 1f - expansion },
                        )
                        Icon(
                            painterResource(R.drawable.ic_iride_fullscreen_exit),
                            null,
                            tint = Color.White.copy(alpha = .85f),
                            modifier = Modifier.fillMaxSize().graphicsLayer { alpha = expansion },
                        )
                    }
                else
                    Icon(
                        painterResource(contextualIcon ?: R.drawable.history),
                        contextualDescription,
                        tint = Color.White.copy(alpha = .85f),
                        modifier = Modifier.size(22.dp),
                    )
            }
        }
    }
}
