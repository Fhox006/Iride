package com.metrolist.innertube.models

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive

@Serializable
data class ContinuationItemRenderer(
    val continuationEndpoint: ContinuationEndpoint? = null,
    val button: JsonElement? = null,
) {
    @Serializable
    data class ContinuationEndpoint(
        val continuationCommand: ContinuationCommand?,
    ) {
        @Serializable
        data class ContinuationCommand(
            val token: String?,
        )
    }
}

/** Reply continuations can be nested in a button instead of continuationEndpoint (SimpMusic, GPL-3.0). */
internal fun ContinuationItemRenderer.commentContinuationToken(): String? {
    fun JsonElement.findToken(): String? = when (this) {
        is JsonObject -> (get("token") as? JsonPrimitive)?.content
            ?: values.firstNotNullOfOrNull { it.findToken() }
        is JsonArray -> firstNotNullOfOrNull { it.findToken() }
        else -> null
    }
    return continuationEndpoint?.continuationCommand?.token ?: button?.findToken()
}
