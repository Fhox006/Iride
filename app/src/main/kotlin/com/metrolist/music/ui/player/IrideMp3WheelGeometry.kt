package com.metrolist.music.ui.player

import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Shared physical dimensions for the wheel and its fullscreen transport. */
@Immutable
internal data class IrideMp3WheelGeometry(val diameter: Dp) {
    val centerSize: Dp
        get() = diameter * .40f

    val zoneSize: Dp
        get() = (diameter - centerSize) / 2

    val iconSize: Dp
        get() = (diameter * .13f).coerceIn(20.dp, 28.dp)
}
