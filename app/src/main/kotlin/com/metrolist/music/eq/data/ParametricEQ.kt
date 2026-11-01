package com.metrolist.music.eq.data

import kotlinx.serialization.Serializable

/**
 * Represents a single parametric EQ filter/band
 * Supports APO Parametric EQ filters
 */
@Serializable
data class ParametricEQBand(
    val frequency: Double,
    val gain: Double,
    val q: Double = 1.41,
    val filterType: FilterType = FilterType.PK,
    val enabled: Boolean = true
)

/**
 * Represents a complete parametric EQ configuration for a headphone
 * Parsed from AutoEQ preset files (...ParametricEQ.txt)
 */
@Serializable
data class ParametricEQ(
    val preamp: Double,
    val bands: List<ParametricEQBand>,
    val metadata: Map<String, String> = emptyMap()
) {
    companion object {
        const val MAX_BANDS = 20
    }
}