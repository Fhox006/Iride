package com.metrolist.music.ui.screens.equalizer

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.metrolist.music.eq.data.EQPreset
import com.metrolist.music.ui.theme.SpaceMonoFontFamily
import com.metrolist.music.ui.theme.textPrimary
import com.metrolist.music.ui.theme.textSecondary
import kotlin.math.abs

/**
 * Minimal mono channel: a hairline vertical travel, the level segment from 0 dB to the
 * handle, and a small solid bar as handle. The numeric readout above is always visible
 * and is the only way to open the exact-value editor; touching the channel just selects.
 */
@Composable
fun VerticalGainSlider(
    value: Double,
    label: String,
    selected: Boolean,
    onSelect: () -> Unit,
    onValueChange: (Double) -> Unit,
    onValueClick: () -> Unit,
    modifier: Modifier = Modifier,
    accent: Color = MaterialTheme.colorScheme.textPrimary,
) {
    val density = LocalDensity.current
    val range = 2.0 * EQPreset.MAX_GAIN_DB
    val fraction = ((value + EQPreset.MAX_GAIN_DB) / range).coerceIn(0.0, 1.0)

    var dragging by remember { mutableStateOf(false) }
    val active = dragging || selected

    fun fractionAt(yPx: Float, canvasHeightPx: Float): Double {
        val pad = with(density) { TravelPad.toPx() }
        val usable = (canvasHeightPx - 2 * pad).coerceAtLeast(1f)
        return (1.0 - ((yPx - pad) / usable)).coerceIn(0.0, 1.0)
    }

    Column(modifier = modifier) {
        // Numeric readout: tap target for the exact-value editor
        Text(
            text = EqCurve.formatGain(value),
            style = TextStyle(
                fontFamily = SpaceMonoFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 10.sp
            ),
            color = accent.copy(alpha = if (active) 1f else 0.5f),
            textAlign = TextAlign.Center,
            maxLines = 1,
            modifier = Modifier
                .fillMaxWidth()
                .pointerInput(Unit) {
                    detectTapGestures(onTap = { onValueClick() })
                }
                .padding(top = 2.dp, bottom = 2.dp)
        )

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(FaderHeight)
                // Custom drag claim: the standard detector cancels on diagonal movement,
                // handing the gesture to the parent scroll and jumping the page mid-drag.
                .pointerInput(Unit) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        val slop = viewConfiguration.touchSlop
                        var totalDy = 0f
                        var lastY = down.position.y
                        var claimed = false
                        while (true) {
                            val event = awaitPointerEvent()
                            val change =
                                event.changes.firstOrNull { it.id == down.id } ?: break
                            if (!change.pressed) {
                                if (claimed) change.consume()
                                break
                            }
                            if (!claimed && change.isConsumed) break
                            totalDy += change.position.y - lastY
                            lastY = change.position.y
                            if (!claimed && abs(totalDy) > slop) {
                                claimed = true
                                dragging = true
                                onSelect()
                            }
                            if (claimed) {
                                change.consume()
                                onValueChange(
                                    fractionAt(lastY, size.height.toFloat()) * range -
                                            EQPreset.MAX_GAIN_DB
                                )
                            }
                        }
                        dragging = false
                    }
                }
                .pointerInput(Unit) {
                    detectTapGestures(onTap = { onSelect() })
                }
        ) {
            val height = size.height
            val centerX = size.width / 2f
            val pad = TravelPad.toPx()
            val usable = height - 2 * pad

            val zeroY = pad + usable / 2f
            val thumbY = pad + usable * (1.0 - fraction).toFloat()

            // Hairline travel
            drawLine(
                color = accent.copy(alpha = 0.22f),
                start = Offset(centerX, pad),
                end = Offset(centerX, height - pad),
                strokeWidth = TrackWidth.toPx(),
                cap = StrokeCap.Round
            )

            // Level from the 0 dB notch to the handle
            if (thumbY < zeroY - 1f || thumbY > zeroY + 1f) {
                drawLine(
                    color = accent.copy(alpha = if (active) 1f else 0.8f),
                    start = Offset(centerX, zeroY),
                    end = Offset(centerX, thumbY),
                    strokeWidth = TrackWidth.toPx(),
                    cap = StrokeCap.Round
                )
            }

            // Handle: small solid horizontal bar
            drawRoundRect(
                color = accent.copy(alpha = if (active) 1f else 0.75f),
                topLeft = Offset(centerX - HandleWidth.toPx() / 2f, thumbY - HandleHeight.toPx() / 2f),
                size = Size(HandleWidth.toPx(), HandleHeight.toPx()),
                cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
            )
        }

        Text(
            text = label,
            style = TextStyle(
                fontFamily = SpaceMonoFontFamily,
                fontSize = 9.sp
            ),
            color = MaterialTheme.colorScheme.textSecondary.copy(alpha = if (active) 1f else 0.7f),
            textAlign = TextAlign.Center,
            maxLines = 1,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp)
        )
    }
}

private val FaderHeight = 150.dp
private val TravelPad = 12.dp
private val TrackWidth = 2.dp
private val HandleWidth = 14.dp
private val HandleHeight = 4.dp
