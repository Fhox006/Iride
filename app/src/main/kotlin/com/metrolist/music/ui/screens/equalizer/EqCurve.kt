package com.metrolist.music.ui.screens.equalizer

import com.metrolist.music.eq.data.EqFilterMath
import com.metrolist.music.eq.data.EQPreset
import com.metrolist.music.eq.data.FilterType
import com.metrolist.music.eq.data.ParametricEQBand
import kotlin.math.ln
import kotlin.math.pow

/**
 * Frequency-response evaluation for the editor's live curve preview.
 * Uses a nominal 48 kHz sample rate; visualization only.
 */
object EqCurve {

    const val NOMINAL_SAMPLE_RATE = 48000.0
    const val MIN_DISPLAY_FREQUENCY = 20.0
    const val MAX_DISPLAY_FREQUENCY = 20000.0
    const val DISPLAY_RANGE_DB = 15.0

    val GRID_LINES = listOf(
        20.0f to "20",
        50.0f to "50",
        100.0f to "100",
        500.0f to "500",
        1000.0f to "1k",
        5000.0f to "5k",
        10000.0f to "10k",
        20000.0f to "20k"
    )

    fun responseDb(
        bands: List<ParametricEQBand>,
        bassBoostDb: Double,
        preampDb: Double,
        frequency: Double
    ): Double {
        var total = preampDb

        for (band in bands) {
            if (!band.enabled || band.gain == 0.0) continue
            total += bandMagnitudeDb(band, frequency)
        }

        if (bassBoostDb != 0.0) {
            // When transient shaping is active the preview shows the unshaped shelf,
            // which matches what short transients actually receive.
            total += EqFilterMath.magnitudeDb(
                EqFilterMath.lowShelf(NOMINAL_SAMPLE_RATE, EQPreset.BASS_SHELF_FREQUENCY, bassBoostDb),
                frequency,
                NOMINAL_SAMPLE_RATE
            )
        }
        return total
    }

    private fun bandMagnitudeDb(band: ParametricEQBand, frequency: Double): Double {
        val coeffs = when (band.filterType) {
            FilterType.LSC ->
                EqFilterMath.lowShelf(NOMINAL_SAMPLE_RATE, band.frequency, band.gain)
            FilterType.HSC ->
                EqFilterMath.highShelf(NOMINAL_SAMPLE_RATE, band.frequency, band.gain)
            else ->
                EqFilterMath.peaking(NOMINAL_SAMPLE_RATE, band.frequency, band.gain, band.q)
        }
        return EqFilterMath.magnitudeDb(coeffs, frequency, NOMINAL_SAMPLE_RATE)
    }

    fun logFraction(frequency: Double): Float =
        (ln(frequency / MIN_DISPLAY_FREQUENCY) /
                ln(MAX_DISPLAY_FREQUENCY / MIN_DISPLAY_FREQUENCY)).toFloat().coerceIn(0f, 1f)

    fun frequencyAt(fraction: Float): Double =
        MIN_DISPLAY_FREQUENCY *
                (MAX_DISPLAY_FREQUENCY / MIN_DISPLAY_FREQUENCY).pow(fraction.toDouble())

    fun formatFrequency(frequency: Double): String = when {
        frequency >= 1000 -> {
            val khz = frequency / 1000.0
            if (khz % 1.0 < 0.05 || khz % 1.0 > 0.95) "${khz.toInt()} kHz"
            else "%.1f kHz".format(khz)
        }
        else -> "${frequency.toInt()} Hz"
    }

    fun formatGain(gain: Double): String = "%+.1f dB".format(gain)
}
