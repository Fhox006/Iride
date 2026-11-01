package com.metrolist.music.eq.audio

import com.metrolist.music.eq.data.EqFilterMath
import com.metrolist.music.eq.data.FilterType
import kotlin.math.abs
import kotlin.math.exp

/**
 * Time-varying low-shelf "transient shaper" behind the advanced "Duration" parameter.
 *
 * A fast/slow envelope pair tracks the energy below ~120 Hz. While sustained low-frequency
 * content is detected the extra shelf boost fades out, so only short percussive transients
 * get the full gain. This yields punchy bass on low-quality speakers without a constant rumble.
 *
 * Higher [strength] means shorter tolerated sustain (faster decay of the boost).
 */
class DynamicBassStage(
    private val sampleRate: Int,
    private val shelfFrequencyHz: Double
) {
    private var shelf = BiquadFilter(
        sampleRate = sampleRate,
        frequency = shelfFrequencyHz,
        gain = 0.0,
        filterType = FilterType.LSC
    )

    /** 0..1, how aggressively sustained bass is tamed */
    @Volatile
    var strength = 0.0
        set(value) {
            field = value.coerceIn(0.0, 1.0)
            slowCoef = timeConstant(sustainWindowMs(field))
        }

    /** Static shelf boost in dB that gets shaped over time */
    @Volatile
    var shelfGainDb = 0.0

    private var currentDb = 0.0
    private var sustain = 0.0

    private var lpState = 0.0
    private var fastEnv = 0.0
    private var slowEnv = 0.0
    private var refPeak = 0.0

    private var lpCoef = 0.0
    private var fastCoef = 0.0
    private var slowCoef = 0.0
    private var refDecay = 0.0
    private var sustainSmoothing = 0.0
    private var gainSmoothing = 0.0
    private var frameCounter = 0

    init {
        lpCoef = 1.0 - exp(-TAU * ENVELOPE_LP_HZ / sampleRate)
        fastCoef = timeConstant(FAST_ENV_MS)
        slowCoef = timeConstant(sustainWindowMs(strength))
        refDecay = exp(-1.0 / (REF_PEAK_S * sampleRate))
        sustainSmoothing = timeConstant(SUSTAIN_SMOOTHING_MS)
        gainSmoothing = timeConstant(GAIN_SMOOTHING_MS)
    }

    private fun sustainWindowMs(strengthValue: Double): Double =
        SUSTAIN_WINDOW_MAX_MS -
                (SUSTAIN_WINDOW_MAX_MS - SUSTAIN_WINDOW_MIN_MS) * strengthValue

    private fun timeConstant(ms: Double): Double = 1.0 - exp(-1.0 / (ms / 1000.0 * sampleRate))

    /**
     * Process one stereo frame; returns the shaped samples.
     */
    fun process(inputLeft: Double, inputRight: Double): Pair<Double, Double> {
        val mono = (inputLeft + inputRight) * 0.5
        lpState += lpCoef * (mono - lpState)

        val energy = abs(lpState)
        fastEnv += fastCoef * (energy - fastEnv)
        slowEnv += slowCoef * (energy - slowEnv)
        if (energy > refPeak) {
            refPeak = energy
        } else {
            refPeak *= refDecay
        }

        val reference = refPeak + 1e-4
        val presence = (slowEnv / reference).coerceIn(0.0, 1.0)
        val onset = ((fastEnv - slowEnv) / reference).coerceIn(0.0, 1.5)
        val targetSustain = (presence * PRESENCE_WEIGHT - onset * ONSET_PENALTY).coerceIn(0.0, 1.0)
        sustain += sustainSmoothing * (targetSustain - sustain)

        frameCounter++
        if (frameCounter >= CONTROL_BLOCK_FRAMES) {
            frameCounter = 0
            val targetDb = shelfGainDb * (1.0 - strength * sustain)
            currentDb += gainSmoothing * (targetDb - currentDb)
            if (abs(targetDb - currentDb) < 0.02) {
                currentDb = targetDb
            }
            shelf.recalculate(currentDb)
        }

        return shelf.processStereo(inputLeft, inputRight)
    }

    fun reset() {
        lpState = 0.0
        fastEnv = 0.0
        slowEnv = 0.0
        refPeak = 0.0
        sustain = 0.0
        currentDb = 0.0
        frameCounter = 0
        shelf.reset()
    }

    companion object {
        const val ENVELOPE_LP_HZ = 120.0
        const val FAST_ENV_MS = 10.0
        const val SUSTAIN_WINDOW_MIN_MS = 60.0
        const val SUSTAIN_WINDOW_MAX_MS = 600.0
        const val REF_PEAK_S = 3.0
        const val SUSTAIN_SMOOTHING_MS = 80.0
        const val GAIN_SMOOTHING_MS = 40.0
        const val CONTROL_BLOCK_FRAMES = 32
        const val PRESENCE_WEIGHT = 1.4
        const val ONSET_PENALTY = 1.2
        const val TAU = 2.0 * kotlin.math.PI

        /**
         * Magnitude response of the shaped shelf at full boost, for curve preview purposes.
         */
        fun staticResponseDb(frequency: Double, shelfFrequencyHz: Double, gainDb: Double): Double =
            EqFilterMath.magnitudeDb(
                EqFilterMath.lowShelf(48000.0, shelfFrequencyHz, gainDb),
                frequency,
                48000.0
            )
    }
}
