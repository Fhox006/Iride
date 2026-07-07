package com.metrolist.lastfm.models

import kotlinx.serialization.Serializable

@Serializable
data class TopTagsResponse(
    val toptags: TopTags? = null,
)

@Serializable
data class TopTags(
    val tag: List<Tag> = emptyList(),
)

@Serializable
data class Tag(
    val name: String? = null,
    val count: Int = 0,
)
