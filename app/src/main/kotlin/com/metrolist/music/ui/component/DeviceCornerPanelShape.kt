/**
 * Iride Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.component

import android.os.Build
import android.util.Log
import android.view.RoundedCorner
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.metrolist.music.BuildConfig

/** Detected device screen corner info needed for panel shape computation. */
data class DeviceCornerInfo(
    val bottomLeftRadiusPx: Float,
    val bottomRightRadiusPx: Float,
    val bottomInsetPx: Float,
)

/**
 * Reads bottom rounded-corner radii from the current window via [android.view.RoundedCorner]
 * (API 31+). Falls back to zero on older APIs or when corners are unavailable (emulator, split
 * window, etc.).
 *
 * [bottomInsetPx] is the navigation bar inset, read from [WindowInsets.systemBars].
 */
@Composable
fun rememberDeviceCornerInfo(): DeviceCornerInfo {
    val density = LocalDensity.current
    val view = LocalView.current
    // WindowInsets.systemBars triggers recomposition whenever the inset changes.
    val bottomInsetPx = WindowInsets.systemBars.getBottom(density).toFloat()

    val blPx: Float
    val brPx: Float
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val rootInsets = view.rootWindowInsets
        blPx = rootInsets?.getRoundedCorner(RoundedCorner.POSITION_BOTTOM_LEFT)?.radius?.toFloat() ?: 0f
        brPx = rootInsets?.getRoundedCorner(RoundedCorner.POSITION_BOTTOM_RIGHT)?.radius?.toFloat() ?: 0f
    } else {
        blPx = 0f
        brPx = 0f
    }

    return remember(blPx, brPx, bottomInsetPx) {
        DeviceCornerInfo(blPx, brPx, bottomInsetPx)
    }
}

/**
 * Returns a [Shape] whose top corners are fixed at [topRadius] and whose bottom corners are
 * derived from the device's physical screen rounded corners, reduced by the panel's distance from
 * the screen edges.
 *
 * Formula:
 *   edgeDistance = max(horizontalGapPx, totalBottomGapPx)
 *   effectiveRadius = max(0, deviceCornerRadius − edgeDistance)
 *
 * where:
 *   horizontalGapPx    = [horizontalPadding] converted to px
 *   totalBottomGapPx   = navBarInset + [additionalBottomSpacing] in px
 *
 * Both bottom corners are computed independently (supports asymmetric device corners).
 *
 * On API < 31 or when corner info is unavailable, falls back to [topRadius] on all four corners.
 *
 * NOTE: currently uses [RoundedCornerShape] as a placeholder. Replace with a cubic-bezier smooth
 * path once the geometry is validated on device.
 */
@Composable
fun rememberDeviceMatchedBottomPanelShape(
    horizontalPadding: Dp,
    additionalBottomSpacing: Dp,
    topRadius: Dp = 28.dp,
): Shape {
    val density = LocalDensity.current
    val cornerInfo = rememberDeviceCornerInfo()

    return remember(density, cornerInfo, horizontalPadding, additionalBottomSpacing, topRadius) {
        val hPx = with(density) { horizontalPadding.toPx() }
        val bPx = cornerInfo.bottomInsetPx + with(density) { additionalBottomSpacing.toPx() }
        val edgeDistance = maxOf(hPx, bPx)

        val blEffPx = maxOf(0f, cornerInfo.bottomLeftRadiusPx - edgeDistance)
        val brEffPx = maxOf(0f, cornerInfo.bottomRightRadiusPx - edgeDistance)

        if (BuildConfig.DEBUG) {
            Log.d(
                "DeviceCornerPanel",
                "device BL=${cornerInfo.bottomLeftRadiusPx}px  BR=${cornerInfo.bottomRightRadiusPx}px | " +
                    "navBar=${cornerInfo.bottomInsetPx}px  extraB=${with(density) { additionalBottomSpacing.toPx() }}px | " +
                    "H=${hPx}px  totalB=${bPx}px  edge=${edgeDistance}px | " +
                    "effective BL=${blEffPx}px  BR=${brEffPx}px",
            )
        }

        with(density) {
            // TODO: replace with smooth cubic-bezier GenericShape for squircle-style corners
            RoundedCornerShape(
                topStart = topRadius,
                topEnd = topRadius,
                bottomStart = blEffPx.toDp(),
                bottomEnd = brEffPx.toDp(),
            )
        }
    }
}

// ──────────────────────────────────────────────────────────────────────────────────────────────
// Debug / tuning composable — call from any Screen or @Preview to inspect live values on device
// ──────────────────────────────────────────────────────────────────────────────────────────────

@Composable
fun DeviceCornerDebugPanel(
    modifier: Modifier = Modifier,
) {
    var hPadSlider by remember { mutableFloatStateOf(12f) }
    var bSpaceSlider by remember { mutableFloatStateOf(12f) }

    val hPad = hPadSlider.dp
    val bSpace = bSpaceSlider.dp

    val cornerInfo = rememberDeviceCornerInfo()
    val shape = rememberDeviceMatchedBottomPanelShape(hPad, bSpace)
    val density = LocalDensity.current

    val hPx = with(density) { hPad.toPx() }
    val bPx = cornerInfo.bottomInsetPx + with(density) { bSpace.toPx() }
    val edge = maxOf(hPx, bPx)
    val blEff = maxOf(0f, cornerInfo.bottomLeftRadiusPx - edge)
    val brEff = maxOf(0f, cornerInfo.bottomRightRadiusPx - edge)

    Column(modifier = modifier) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp)
                .padding(horizontal = hPad, vertical = bSpace)
                .clip(shape)
                .background(Color(0xFF2A2A2A)),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "device BL: ${cornerInfo.bottomLeftRadiusPx.toInt()}px  BR: ${cornerInfo.bottomRightRadiusPx.toInt()}px",
                    color = Color.White,
                    style = MaterialTheme.typography.labelSmall,
                )
                Text(
                    "effective BL: ${blEff.toInt()}px  BR: ${brEff.toInt()}px",
                    color = Color.Green,
                    style = MaterialTheme.typography.labelSmall,
                )
            }
        }
        Text("H padding: ${hPadSlider.toInt()}dp", style = MaterialTheme.typography.labelSmall)
        Slider(value = hPadSlider, onValueChange = { hPadSlider = it }, valueRange = 0f..64f)
        Text("B spacing: ${bSpaceSlider.toInt()}dp", style = MaterialTheme.typography.labelSmall)
        Slider(value = bSpaceSlider, onValueChange = { bSpaceSlider = it }, valueRange = 0f..64f)
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun DeviceCornerDebugPanelPreview() {
    DeviceCornerDebugPanel()
}
