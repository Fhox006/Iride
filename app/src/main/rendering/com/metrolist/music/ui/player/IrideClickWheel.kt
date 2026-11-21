package com.metrolist.music.ui.player

import androidx.compose.animation.Crossfade
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.innerShadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.metrolist.music.R
import com.metrolist.music.ui.component.irideFolioCombinedClickable
import com.metrolist.music.ui.theme.IBMPlexMonoFontFamily
import com.metrolist.music.ui.utils.IrideMotion
import com.metrolist.music.ui.utils.pressScale
import com.metrolist.music.ui.utils.rememberReducedMotion

@Composable
internal fun IrideClickWheel(
    isPlaying: Boolean,
    isShuffleActive: Boolean,
    isMenuOpen: Boolean,
    onMenuClick: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    isListenTogetherGuest: Boolean,
    isMuted: Boolean,
    onPlayPauseClick: () -> Unit,
    onPreviousClick: () -> Unit,
    onNextClick: () -> Unit,
    onRadioClick: () -> Unit,
    onCenterClick: () -> Unit,
    wheelSize: Dp,
    modifier: Modifier = Modifier,
) {
    val scale = wheelSize / 233.dp
    val reducedMotion = rememberReducedMotion()
    val shuffleDescription = stringResource(if (isShuffleActive) R.string.mp3_shuffle_on else R.string.mp3_shuffle_off)
    val geometry = IrideMp3WheelGeometry(wheelSize)
    val centerSize = geometry.centerSize
    // Each zone spans the ring band, putting its icon and selected halo on the same midpoint.
    val zoneSize = geometry.zoneSize
    val edgeInset = 0.dp
    val iconSize = geometry.iconSize
    val radioHighlight by animateColorAsState(
        if (isShuffleActive) Color.White.copy(alpha = 0.12f) else Color.Transparent,
        tween(if (reducedMotion) 0 else IrideMotion.Short), label = "mp3RadioSeed",
    )
    val radioOutline by animateColorAsState(
        if (isShuffleActive) Color.White.copy(alpha = 0.5f) else Color.Transparent,
        tween(if (reducedMotion) 0 else IrideMotion.Short), label = "mp3RadioOutline",
    )
    Box(modifier.size(wheelSize), contentAlignment = Alignment.Center) {
        Box(
            Modifier.fillMaxSize().clip(CircleShape)
                .mp3MatteSurface(IrideMp3WheelSurfaceColor)
                .innerShadow(
                    CircleShape,
                    Shadow(radius = 4.dp, color = Color.Black.copy(alpha = 0.35f)),
                )
                .border(
                    1.dp,
                    IrideMp3WheelBorderColor,
                    CircleShape,
                ),
        )
        WheelZone(
            alignment = Alignment.TopCenter,
            size = zoneSize,
            edgeInset = edgeInset,
            onClick = if (isMenuOpen) onMoveUp else onMenuClick,
        ) {
            AnimatedContent(
                targetState = isMenuOpen,
                transitionSpec = {
                    (fadeIn(tween(if (reducedMotion) 0 else IrideMotion.Short)) +
                        scaleIn(initialScale = 0.82f, animationSpec = tween(if (reducedMotion) 0 else IrideMotion.Short)))
                        .togetherWith(
                            fadeOut(tween(if (reducedMotion) 0 else IrideMotion.Quick)) +
                                scaleOut(targetScale = 0.82f, animationSpec = tween(if (reducedMotion) 0 else IrideMotion.Quick))
                        )
                },
                label = "mp3MenuControl",
                modifier = Modifier.fillMaxSize(),
            ) { menuOpen ->
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    if (menuOpen) {
                        Icon(
                            painterResource(R.drawable.expand_less), stringResource(R.string.mp3_move_up),
                            tint = Color.White, modifier = Modifier.size(28.dp),
                        )
                    } else {
                        Text(
                            stringResource(R.string.mp3_menu), color = Color.White,
                            fontFamily = IBMPlexMonoFontFamily, fontWeight = FontWeight.Medium,
                            fontSize = (12 * scale).sp, letterSpacing = 0.8.sp,
                        )
                    }
                }
            }
        }
        WheelZone(
            alignment = Alignment.CenterEnd,
            size = zoneSize,
            edgeInset = edgeInset,
            onClick = onNextClick,
            enabled = !isListenTogetherGuest,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_iride_fast_forward_filled),
                contentDescription = stringResource(R.string.mp3_next),
                tint = if (isListenTogetherGuest) IrideMp3DimIconColor else Color.White,
                modifier = Modifier.size(iconSize),
            )
        }
        WheelZone(
            alignment = Alignment.CenterStart,
            size = zoneSize,
            edgeInset = edgeInset,
            onClick = onPreviousClick,
            enabled = !isListenTogetherGuest,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_iride_fast_forward_filled),
                contentDescription = stringResource(R.string.mp3_previous),
                tint = if (isListenTogetherGuest) IrideMp3DimIconColor else Color.White,
                modifier = Modifier.size(iconSize).graphicsLayer { scaleX = -1f },
            )
        }
        WheelZone(
            alignment = Alignment.BottomCenter,
            size = zoneSize,
            edgeInset = edgeInset,
            onClick = if (isMenuOpen) onMoveDown else onRadioClick,
            enabled = isMenuOpen || !isListenTogetherGuest,
        ) {
            AnimatedContent(
                targetState = isMenuOpen,
                transitionSpec = {
                    (fadeIn(tween(if (reducedMotion) 0 else IrideMotion.Short)) +
                        scaleIn(initialScale = 0.72f, animationSpec = tween(if (reducedMotion) 0 else IrideMotion.Short)))
                        .togetherWith(
                            fadeOut(tween(if (reducedMotion) 0 else IrideMotion.Quick)) +
                                scaleOut(targetScale = 0.72f, animationSpec = tween(if (reducedMotion) 0 else IrideMotion.Quick))
                        )
                },
                label = "mp3RadioControl",
                modifier = Modifier.fillMaxSize(),
            ) { menuOpen ->
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    if (menuOpen) {
                        Icon(
                            painterResource(R.drawable.expand_more), stringResource(R.string.mp3_move_down),
                            tint = Color.White, modifier = Modifier.size(28.dp),
                        )
                    } else {
                        Box(
                            Modifier.size(zoneSize * 0.72f).clip(CircleShape).background(radioHighlight)
                                .border(1.dp, radioOutline, CircleShape)
                                .semantics { stateDescription = shuffleDescription },
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                painterResource(R.drawable.radio), shuffleDescription,
                                tint = if (isListenTogetherGuest) IrideMp3DimIconColor else Color.White,
                                modifier = Modifier.size(iconSize),
                            )
                        }
                    }
                }
            }
        }
        val centerInteraction = remember { MutableInteractionSource() }
        val centerDescription = stringResource(
            if (isMenuOpen) R.string.mp3_select
            else if (isListenTogetherGuest) R.string.mp3_mute
            else if (isPlaying) R.string.mp3_pause else R.string.mp3_play,
        )
        Box(
            modifier = Modifier.size(centerSize)
                .pressScale(centerInteraction, pressedScale = 0.88f)
                .clip(CircleShape)
                .mp3MatteSurface(IrideMp3WheelCenterColor)
                .innerShadow(
                    CircleShape,
                    Shadow(radius = 2.dp, color = Color.Black.copy(alpha = 0.3f)),
                )
                .border(
                    1.dp,
                    IrideMp3WheelCenterBorderColor,
                    CircleShape,
                )
                .semantics { contentDescription = centerDescription }
                .irideFolioCombinedClickable(
                    interactionSource = centerInteraction,
                    indication = null,
                    onClick = onCenterClick,
                    onLongClick = onPlayPauseClick,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Box {
                if (isListenTogetherGuest) {
                    Crossfade(
                        targetState = isMuted,
                        animationSpec = tween(if (reducedMotion) 0 else IrideMotion.Quick),
                        label = "mp3Mute",
                        modifier = Modifier.size(iconSize),
                    ) { muted ->
                        Icon(
                            painter = painterResource(if (muted) R.drawable.volume_off else R.drawable.volume_up),
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                } else {
                    IrideMp3PlaybackGlyph(isPlaying, reducedMotion, Modifier.size(iconSize))
                }
            }
        }
    }
}

@Composable
internal fun BoxScope.WheelZone(
    alignment: Alignment,
    size: Dp,
    // Distance from the wheel's outer edge to the zone's center — centers it in the outer half
    // of the ring band (between the rim and the center hole) instead of a flat margin that
    // ignored how big that band actually was.
    edgeInset: Dp,
    onClick: () -> Unit,
    enabled: Boolean = true,
    // Touch target size; defaults to the icon size. The bottom play/pause zone widens it so a tap
    // landing slightly left/right of the icon still hits instead of falling through to the
    // player's background collapse-on-tap and dropping to the mini player.
    hitWidth: Dp = size,
    hitHeight: Dp = size,
    // Nudges the zone off its raw edge-aligned spot, in sync with the other 3 zones, so the ring
    // of buttons reads as a slightly wider circle centered in the gray band.
    offsetX: Dp = 0.dp,
    offsetY: Dp = 0.dp,
    content: @Composable () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        modifier =
            Modifier.align(alignment)
                .padding(edgeInset)
                .offset(x = offsetX, y = offsetY)
                .size(width = hitWidth, height = hitHeight)
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    enabled = enabled,
                    onClick = onClick,
                ),
        // Anchor the icon to the zone's own edge so a widened hit box grows inward (toward the
        // wheel center) without shifting the icon off its spot.
        contentAlignment = alignment,
    ) {
        Box(
            modifier =
                Modifier.size(size)
                    .pressScale(interactionSource, pressedScale = 0.88f)
                    .clip(CircleShape),
            contentAlignment = Alignment.Center,
            content = { content() },
        )
    }
}
