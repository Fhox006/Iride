package com.metrolist.innertube.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TimedTextResponse(
    val events: List<TimedTextEvent>? = null
)

@Serializable
data class TimedTextEvent(
    @SerialName("tStartMs") val tStartMs: Long? = null,
    @SerialName("dDurationMs") val dDurationMs: Long? = null,
    @SerialName("segs") val segs: List<TimedTextSeg>? = null,
    @SerialName("aAppend") val aAppend: Int? = null
)

@Serializable
data class TimedTextSeg(
    @SerialName("utf8") val utf8: String? = null,
    @SerialName("tOffsetMs") val tOffsetMs: Long? = null
)
