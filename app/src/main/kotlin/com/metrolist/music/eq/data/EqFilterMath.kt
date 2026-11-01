package com.metrolist.music.eq.data

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Shared RBJ Audio EQ Cookbook biquad coefficient math.
 * Returns normalized coefficients [b0, b1, b2, a1, a2] with a0 = 1.
 */
object EqFilterMath {

    private const val SQRT_TWO = 1.4142135623730951

    fun peaking(sampleRate: Double, frequency: Double, gainDb: Double, q: Double): DoubleArray {
        val a = 10.0.pow(gainDb / 40.0)
        val w = 2.0 * PI * frequency / sampleRate
        val sinW = sin(w)
        val cosW = cos(w)
        val alpha = sinW / (2.0 * q)

        val b0 = 1.0 + alpha * a
        val b1 = -2.0 * cosW
        val b2 = 1.0 - alpha * a
        val a0 = 1.0 + alpha / a
        val a1 = -2.0 * cosW
        val a2 = 1.0 - alpha / a

        // With gain = 0 dB the numerator and denominator coincide, so H is exactly 1.
        return doubleArrayOf(b0 / a0, b1 / a0, b2 / a0, a1 / a0, a2 / a0)
    }

    fun lowShelf(sampleRate: Double, frequency: Double, gainDb: Double): DoubleArray {
        val a = sqrt(10.0.pow(gainDb / 20.0))
        val w = 2.0 * PI * frequency / sampleRate
        val sinW = sin(w)
        val cosW = cos(w)
        val alpha = sinW / 2.0 * SQRT_TWO
        val sqrtA = sqrt(a)
        val aPlusOne = a + 1.0
        val aMinusOne = a - 1.0
        val twoSqrtAAlpha = 2.0 * sqrtA * alpha

        val b0 = a * (aPlusOne - aMinusOne * cosW + twoSqrtAAlpha)
        val b1 = 2.0 * a * (aMinusOne - aPlusOne * cosW)
        val b2 = a * (aPlusOne - aMinusOne * cosW - twoSqrtAAlpha)
        val a0 = aPlusOne + aMinusOne * cosW + twoSqrtAAlpha
        val a1 = -2.0 * (aMinusOne + aPlusOne * cosW)
        val a2 = aPlusOne + aMinusOne * cosW - twoSqrtAAlpha

        return doubleArrayOf(b0 / a0, b1 / a0, b2 / a0, a1 / a0, a2 / a0)
    }

    fun highShelf(sampleRate: Double, frequency: Double, gainDb: Double): DoubleArray {
        val a = sqrt(10.0.pow(gainDb / 20.0))
        val w = 2.0 * PI * frequency / sampleRate
        val sinW = sin(w)
        val cosW = cos(w)
        val alpha = sinW / 2.0 * SQRT_TWO
        val sqrtA = sqrt(a)
        val aPlusOne = a + 1.0
        val aMinusOne = a - 1.0
        val twoSqrtAAlpha = 2.0 * sqrtA * alpha

        val b0 = a * (aPlusOne + aMinusOne * cosW + twoSqrtAAlpha)
        val b1 = -2.0 * a * (aMinusOne - aPlusOne * cosW)
        val b2 = a * (aPlusOne + aMinusOne * cosW - twoSqrtAAlpha)
        val a0 = aPlusOne - aMinusOne * cosW + twoSqrtAAlpha
        val a1 = 2.0 * (aMinusOne - aPlusOne * cosW)
        val a2 = aPlusOne + aMinusOne * cosW - twoSqrtAAlpha

        return doubleArrayOf(b0 / a0, b1 / a0, b2 / a0, a1 / a0, a2 / a0)
    }

    /**
     * Magnitude response in dB at [frequency] for the given normalized coefficients.
     */
    fun magnitudeDb(coeffs: DoubleArray, frequency: Double, sampleRate: Double): Double {
        val b0 = coeffs[0]
        val b1 = coeffs[1]
        val b2 = coeffs[2]
        val a1 = coeffs[3]
        val a2 = coeffs[4]
        val w = 2.0 * PI * frequency / sampleRate
        val cw = kotlin.math.cos(w)
        val cw2 = kotlin.math.cos(2.0 * w)
        val num = b0 * b0 + b1 * b1 + b2 * b2 +
                2.0 * (b0 * b1 + b1 * b2) * cw + 2.0 * b0 * b2 * cw2
        val den = 1.0 + a1 * a1 + a2 * a2 +
                2.0 * a1 * (1.0 + a2) * cw + 2.0 * a2 * cw2
        return 10.0 * kotlin.math.log10(num / den + 1e-12)
    }
}
