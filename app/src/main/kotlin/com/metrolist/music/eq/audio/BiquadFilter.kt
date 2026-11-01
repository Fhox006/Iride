package com.metrolist.music.eq.audio

import com.metrolist.music.eq.data.EqFilterMath
import com.metrolist.music.eq.data.FilterType

/**
 * Biquad filter implementation for EQ
 * Supports peaking (PK), low-shelf (LSC), and high-shelf (HSC) filters
 * Coefficient math shared with the visualization via [EqFilterMath]
 */
class BiquadFilter(
    sampleRate: Int,
    frequency: Double,
    gain: Double,
    q: Double = 1.41,
    private val filterType: FilterType = FilterType.PK
) {
    private val sampleRate: Int = sampleRate
    private var frequency: Double = frequency
    private var gain: Double = gain
    private var q: Double = q
    private var b0 = 0.0
    private var b1 = 0.0
    private var b2 = 0.0
    private var a1 = 0.0
    private var a2 = 0.0

    private var x1L = 0.0
    private var x2L = 0.0
    private var y1L = 0.0
    private var y2L = 0.0

    private var x1R = 0.0
    private var x2R = 0.0
    private var y1R = 0.0
    private var y2R = 0.0

    init {
        recalculate()
    }

    /**
     * Recalculate coefficients, optionally overriding parameters (used for smooth
     * live updates: retuning in place keeps the delay-line state, avoiding clicks).
     */
    fun recalculate(
        gainOverride: Double = gain,
        frequencyOverride: Double = frequency,
        qOverride: Double = q
    ) {
        frequency = frequencyOverride
        q = qOverride
        val c = when (filterType) {
            FilterType.LSC -> EqFilterMath.lowShelf(sampleRate.toDouble(), frequency, gainOverride)
            FilterType.HSC -> EqFilterMath.highShelf(sampleRate.toDouble(), frequency, gainOverride)
            else -> EqFilterMath.peaking(sampleRate.toDouble(), frequency, gainOverride, q)
        }
        gain = gainOverride
        b0 = c[0]
        b1 = c[1]
        b2 = c[2]
        a1 = c[3]
        a2 = c[4]
    }

    /**
     * Process a single sample (mono)
     */
    fun processSample(input: Double): Double {
        val output = b0 * input + b1 * x1L + b2 * x2L - a1 * y1L - a2 * y2L

        x2L = x1L
        x1L = input
        y2L = y1L
        y1L = output

        return output
    }

    /**
     * Process stereo samples (left and right channels)
     */
    fun processStereo(inputLeft: Double, inputRight: Double): Pair<Double, Double> {
        val outputLeft = b0 * inputLeft + b1 * x1L + b2 * x2L - a1 * y1L - a2 * y2L
        x2L = x1L
        x1L = inputLeft
        y2L = y1L
        y1L = outputLeft

        val outputRight = b0 * inputRight + b1 * x1R + b2 * x2R - a1 * y1R - a2 * y2R
        x2R = x1R
        x1R = inputRight
        y2R = y1R
        y1R = outputRight

        return Pair(outputLeft, outputRight)
    }

    /**
     * Reset filter state (clears history)
     */
    fun reset() {
        x1L = 0.0
        x2L = 0.0
        y1L = 0.0
        y2L = 0.0
        x1R = 0.0
        x2R = 0.0
        y1R = 0.0
        y2R = 0.0
    }
}
