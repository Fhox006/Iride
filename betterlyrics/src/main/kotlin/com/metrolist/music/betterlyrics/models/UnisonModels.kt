package com.metrolist.music.betterlyrics.models

import kotlinx.serialization.Serializable

@Serializable
data class UnisonApiResponse(
    val success: Boolean = false,
    val data: UnisonResponseData? = null,
    val error: String? = null,
    val code: String? = null,
)

@Serializable
data class UnisonResponseData(
    val lyrics: String? = null,
    val format: String? = null, // "ttml" | "lrc" | "plain"
    val syncType: String? = null, // "richsync" | "linesync" | "plain"
    val duration: Double? = null,
)
