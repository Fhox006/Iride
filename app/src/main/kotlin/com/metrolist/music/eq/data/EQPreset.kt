package com.metrolist.music.eq.data

/**
 * Built-in editor defaults and simple presets for the parametric equalizer.
 */
object EQPreset {

    val DEFAULT_BAND_FREQUENCIES = doubleArrayOf(55.0, 110.0, 220.0, 440.0, 1320.0, 3300.0, 6600.0, 13200.0)

    const val MIN_FREQUENCY = 20.0
    const val MAX_FREQUENCY = 20000.0
    const val MAX_GAIN_DB = 6.0
    const val GAIN_STEP = 0.5
    const val DEFAULT_Q = 0.5
    const val MIN_Q = 0.2
    const val MAX_Q = 5.0

    const val BASS_SHELF_FREQUENCY = 90.0
    const val MAX_BASS_BOOST_DB = 8.0

    const val PRESET_STANDARD = "standard"
    const val PRESET_BALANCED = "balanced"
    const val PRESET_MORE_BASS = "more_bass"
    const val PRESET_MORE_TREBLE = "more_treble"
    const val PRESET_VOICE = "voice"
    /** Marks the manual editor mode: no canned curve, bands are whatever the user set */
    const val PRESET_CUSTOM = "custom"

    private val PRESET_STANDARD_GAINS =
        doubleArrayOf(0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0)
    private val PRESET_BALANCED_GAINS =
        doubleArrayOf(1.5, 1.0, 0.5, 0.0, -1.0, -1.0, -0.5, 1.5)
    private val PRESET_MORE_BASS_GAINS =
        doubleArrayOf(5.0, 4.5, 3.0, 1.0, -0.5, -1.5, -2.0, -1.5)
    private val PRESET_MORE_TREBLE_GAINS =
        doubleArrayOf(-2.0, -1.5, -1.0, 0.0, 1.0, 3.0, 4.5, 5.0)
    private val PRESET_VOICE_GAINS =
        doubleArrayOf(-3.0, -2.5, -1.0, 2.0, 3.5, 2.5, 0.0, -1.0)

    fun defaultBands(): List<ParametricEQBand> =
        DEFAULT_BAND_FREQUENCIES.map { ParametricEQBand(frequency = it, gain = 0.0, q = DEFAULT_Q) }

    /**
     * Editable frequency range of a band: it may slide between its neighbours,
     * keeping a small geometric margin so bands never overlap.
     */
    fun frequencyBounds(index: Int): ClosedFloatingPointRange<Double> {
        val low = if (index == 0) {
            MIN_FREQUENCY
        } else {
            (DEFAULT_BAND_FREQUENCIES[index - 1] * 1.1).coerceAtLeast(MIN_FREQUENCY)
        }
        val high = if (index == DEFAULT_BAND_FREQUENCIES.lastIndex) {
            MAX_FREQUENCY
        } else {
            (DEFAULT_BAND_FREQUENCIES[index + 1] * 0.9).coerceAtMost(MAX_FREQUENCY)
        }
        return low..high.coerceAtLeast(low * 1.05)
    }

    fun presetGains(presetId: String): DoubleArray? = when (presetId) {
        PRESET_STANDARD -> PRESET_STANDARD_GAINS
        PRESET_BALANCED -> PRESET_BALANCED_GAINS
        PRESET_MORE_BASS -> PRESET_MORE_BASS_GAINS
        PRESET_MORE_TREBLE -> PRESET_MORE_TREBLE_GAINS
        PRESET_VOICE -> PRESET_VOICE_GAINS
        else -> null
    }

    fun bandsForPreset(presetId: String): List<ParametricEQBand>? {
        val gains = presetGains(presetId) ?: return null
        return gains.mapIndexed { index, gain ->
            ParametricEQBand(
                frequency = DEFAULT_BAND_FREQUENCIES[index],
                gain = gain,
                q = DEFAULT_Q
            )
        }
    }

    fun matchPreset(bands: List<ParametricEQBand>): String? {
        if (bands.size != DEFAULT_BAND_FREQUENCIES.size) return null
        for (id in listOf(
            PRESET_STANDARD,
            PRESET_BALANCED,
            PRESET_MORE_BASS,
            PRESET_MORE_TREBLE,
            PRESET_VOICE
        )) {
            val gains = presetGains(id)!!
            val matches = bands.indices.all { i ->
                kotlin.math.abs(bands[i].gain - gains[i]) < 0.01 &&
                        kotlin.math.abs(bands[i].frequency - DEFAULT_BAND_FREQUENCIES[i]) < 0.01
            }
            if (matches) return id
        }
        return null
    }

    /**
     * Conservative preamp that keeps positive boosts from clipping.
     */
    fun autoPreampDb(bands: List<ParametricEQBand>, bassBoostDb: Double): Double {
        val maxPositive = maxOf(
            bands.maxOfOrNull { it.gain } ?: 0.0,
            bassBoostDb
        )
        if (maxPositive <= 0.0) return 0.0
        val preamp = -(maxPositive * 0.75)
        val snapped = kotlin.math.round(preamp * 2.0) / 2.0
        return snapped.coerceIn(-12.0, 0.0)
    }
}
