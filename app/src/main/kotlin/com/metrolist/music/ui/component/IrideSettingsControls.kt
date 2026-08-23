/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.component

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.metrolist.music.utils.rememberPreference

/**
 * Drop-in replacement for Material 3's [Switch]. Falls through to the stock [Switch] unless New
 * Iride UI is on, in which case it renders a flat monochrome pill (white fill/border on black,
 * no Material color) matching the rest of the New Iride UI settings styling.
 */
@Composable
fun IrideSwitch(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    thumbContent: (@Composable () -> Unit)? = null,
) {
    val trackWidth = 42.dp
    val trackHeight = 24.dp
    val thumbSize = 18.dp
    val inset = 3.dp
    val thumbOffset by animateDpAsState(
        targetValue = if (checked) trackWidth - thumbSize - inset else inset,
        label = "irideSwitchThumbOffset",
    )

    Box(
        modifier = modifier
            .size(width = trackWidth, height = trackHeight)
            .alpha(if (enabled) 1f else 0.35f)
            .clip(RoundedCornerShape(50))
            .background(if (checked) Color.White else Color.Transparent)
            .border(
                width = 1.dp,
                color = Color.White.copy(alpha = if (checked) 0f else 0.4f),
                shape = RoundedCornerShape(50),
            )
            .clickable(
                enabled = enabled && onCheckedChange != null,
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
            ) { onCheckedChange?.invoke(!checked) },
        contentAlignment = Alignment.CenterStart,
    ) {
        Box(
            modifier = Modifier
                .offset(x = thumbOffset)
                .size(thumbSize)
                .clip(CircleShape)
                .background(if (checked) Color.Black else Color.White.copy(alpha = 0.85f)),
        )
    }
}

/**
 * Drop-in replacement for Material 3's [Slider]. Falls through to the stock [Slider] unless New
 * Iride UI is on, in which case it renders a hairline white track with a small square-cut thumb
 * instead of the Material pill track, matching the rest of the New Iride UI settings styling.
 */
@Composable
fun IrideSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    steps: Int = 0,
    onValueChangeFinished: (() -> Unit)? = null,
) {
    Slider(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.alpha(if (enabled) 1f else 0.35f),
        enabled = enabled,
        valueRange = valueRange,
        steps = steps,
        onValueChangeFinished = onValueChangeFinished,
        thumb = {
            Box(
                modifier = Modifier
                    .size(14.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(Color.White),
            )
        },
        track = { sliderState ->
            val fraction = ((sliderState.value - sliderState.valueRange.start) /
                (sliderState.valueRange.endInclusive - sliderState.valueRange.start))
                .coerceIn(0f, 1f)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .clip(RoundedCornerShape(1.dp))
                    .background(Color.White.copy(alpha = 0.2f)),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(fraction)
                        .fillMaxHeight()
                        .background(Color.White.copy(alpha = 0.9f)),
                )
            }
        },
    )
}
