package com.metrolist.paxsenix.models

import kotlinx.serialization.Serializable

@Serializable
data class LyricsContent(
    val timestamp: Long,
    val endtime: Long,
    val duration: Long,
    val structure: String? = null,
    val text: List<LyricText> = emptyList(),
    val background: Boolean = false,
    val backgroundText: List<LyricText> = emptyList(),
    val oppositeTurn: Boolean = false
)

@Serializable
data class LyricText(
    val text: String,
    val timestamp: Long,
    val endtime: Long,
    val duration: Long,
    val part: Boolean = false
)

@Serializable
data class LyricsMetadata(
    val songwriters: List<String> = emptyList()
)

@Serializable
data class LyricsResponse(
    val type: String? = null,
    val metadata: LyricsMetadata? = null,
    val content: List<LyricsContent> = emptyList(),
    val elrc: String? = null,
    val elrcMultiPerson: String? = null,
    val ttmlContent: String? = null,
    val plain: String? = null
)
