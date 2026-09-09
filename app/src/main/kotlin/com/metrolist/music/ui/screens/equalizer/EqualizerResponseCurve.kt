package com.metrolist.music.ui.screens.equalizer

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import com.metrolist.music.eq.data.ParametricEQBand
import com.metrolist.music.ui.theme.textPrimary

/**
 * Display-only frequency response screen for the EQ console (no touch handling: the
 * fader channels below it are the controls). Hardware-style: thin line, hairline grid,
 * monospace tick labels fully inset. A small white dot marks each band's current
 * position on the curve so fader movements are visible on the screen.
 */
@Composable
fun EqualizerResponseCurve(
    bands: List<ParametricEQBand>,
    bassBoostDb: Double,
    preampDb: Double,
    selectedIndex: Int,
    modifier: Modifier = Modifier,
    height: androidx.compose.ui.unit.Dp = 128.dp,
    lineColor: Color = MaterialTheme.colorScheme.textPrimary
) {
    Canvas(modifier = modifier.fillMaxWidth().height(height)) {
        val width = size.width
        val heightPx = size.height
        val labelPx = 10.dp.toPx()
        val hPad = HPad.toPx()
        val bottomPad = labelPx + BottomGap.toPx()
        val topPad = TopPad.toPx()
        val usableWidth = width - 2 * hPad
        val usableHeight = heightPx - topPad - bottomPad

        fun xFor(fraction: Float): Float = hPad + fraction * usableWidth

        fun yFor(db: Double): Float {
            val clamped = db.coerceIn(-EqDbRange, EqDbRange)
            return (topPad + usableHeight * (1.0 - (clamped / EqDbRange + 1.0) / 2.0)).toFloat()
        }

        fun xForFrequency(frequency: Double): Float =
            xFor(EqCurve.logFraction(frequency))

        // Grid: vertical guides per grid frequency, dashed 0 dB axis
        val hairline = lineColor.copy(alpha = 0.16f)
        val paint = android.graphics.Paint().apply {
            isAntiAlias = true
            textSize = labelPx
            typeface = android.graphics.Typeface.MONOSPACE
            color = lineColor.copy(alpha = 0.55f).toArgb()
            textAlign = android.graphics.Paint.Align.CENTER
        }

        EqCurve.GRID_LINES.forEach { (frequency, _) ->
            drawLine(
                color = hairline,
                start = Offset(xForFrequency(frequency.toDouble()), topPad),
                end = Offset(xForFrequency(frequency.toDouble()), heightPx - bottomPad),
                strokeWidth = 1.dp.toPx()
            )
        }
        drawLine(
            color = lineColor.copy(alpha = 0.28f),
            start = Offset(hPad, yFor(0.0)),
            end = Offset(hPad + usableWidth, yFor(0.0)),
            strokeWidth = 1.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 6.dp.toPx()))
        )

        // Combined magnitude curve
        val samples = 128
        val path = Path()
        for (i in 0 until samples) {
            val fraction = i.toFloat() / (samples - 1)
            val db = EqCurve.responseDb(bands, bassBoostDb, preampDb, EqCurve.frequencyAt(fraction))
            val x = xFor(fraction)
            val y = yFor(db)
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(path, color = lineColor, style = Stroke(width = 1.8.dp.toPx()))

        // Band position markers: each node is draggable (frequency on x, gain on y),
        // so a faint guide ties every node to the 0 dB axis and the selected one
        // gets a column highlight under it.
        bands.forEachIndexed { index, band ->
            val clamped = band.gain.coerceIn(-EqDbRange, EqDbRange)
            val center = Offset(
                xForFrequency(band.frequency),
                yFor(clamped)
            )
            val active = index == selectedIndex
            if (active) {
                drawRect(
                    color = lineColor.copy(alpha = 0.05f),
                    topLeft = Offset(center.x - 9.dp.toPx(), topPad),
                    size = Size(18.dp.toPx(), heightPx - topPad - bottomPad)
                )
            }
            drawLine(
                color = lineColor.copy(alpha = if (active) 0.35f else 0.12f),
                start = Offset(center.x, yFor(0.0)),
                end = center,
                strokeWidth = 1.dp.toPx()
            )
            drawCircle(
                color = lineColor.copy(alpha = if (active) 1f else 0.7f),
                radius = if (active) 3.5.dp.toPx() else 2.5.dp.toPx(),
                center = center
            )
            if (active) {
                drawCircle(
                    color = lineColor,
                    radius = 6.dp.toPx(),
                    center = center,
                    style = Stroke(width = 1.dp.toPx())
                )
            }
        }

        // Labels last, baselines well inside the canvas
        EqCurve.GRID_LINES.forEach { (frequency, label) ->
            drawContext.canvas.nativeCanvas.drawText(
                label,
                xForFrequency(frequency.toDouble()),
                heightPx - 3.dp.toPx(),
                paint
            )
        }
    }
}

private const val EqDbRange = 15.0

private val TopPad = 10.dp
private val BottomGap = 8.dp
private val HPad = 18.dp
