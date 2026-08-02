/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.component

import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.metrolist.music.R

/**
 * Monospace/New-Iride-UI selection dot used in multi-select mode: a bare outline circle when
 * unselected, a solid white fill with a black checkmark when selected. Replaces the stock
 * Material [androidx.compose.material3.Checkbox] to match the app's flat monochrome language.
 * [minimumInteractiveComponentSize] keeps the visible dot small while still giving it its own
 * 48dp accessible/focusable tap target (mirrors what the stock Checkbox did internally) — tapping
 * elsewhere on the row still works too via the row's own click handler.
 */
@Composable
fun SelectionIndicator(
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: androidx.compose.ui.unit.Dp = 22.dp,
) {
    val fillProgress by animateFloatAsState(
        targetValue = if (selected) 1f else 0f,
        animationSpec = spring(stiffness = 700f, dampingRatio = 0.8f),
        label = "selectionIndicatorFill",
    )
    val checkAlpha by animateFloatAsState(
        targetValue = if (selected) 1f else 0f,
        animationSpec = tween(durationMillis = if (selected) 120 else 60),
        label = "selectionIndicatorCheck",
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .minimumInteractiveComponentSize()
            .selectable(
                selected = selected,
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                role = Role.Checkbox,
                onClick = onClick,
            )
            .size(size),
    ) {
        Box(
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = fillProgress))
                .border(
                    BorderStroke(1.5.dp, Color.White.copy(alpha = 0.6f - 0.6f * fillProgress)),
                    CircleShape,
                ),
        )
        Icon(
            painter = painterResource(R.drawable.check),
            contentDescription = null,
            tint = Color.Black,
            modifier = Modifier
                .size(size * 0.6f)
                .graphicsLayer {
                    alpha = checkAlpha
                    scaleX = 0.7f + 0.3f * checkAlpha
                    scaleY = 0.7f + 0.3f * checkAlpha
                },
        )
    }
}
