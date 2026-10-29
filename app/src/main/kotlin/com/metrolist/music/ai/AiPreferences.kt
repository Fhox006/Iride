/*
 * ArchiveTune (2026)
 * © Rukamori — github.com/rukamori
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.metrolist.music.ai

import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.metrolist.music.constants.AiProviderKey
import com.metrolist.music.constants.OpenRouterApiKey
import com.metrolist.music.constants.OpenRouterBaseUrlKey
import com.metrolist.music.constants.OpenRouterDefaultBaseUrl
import com.metrolist.music.constants.OpenRouterDefaultModel
import com.metrolist.music.constants.OpenRouterModelKey

// Adapted from ArchiveTune's AiProvider and settings repository to retain Iride's stored settings.
enum class AiProvider {
    CHATGPT,
    GEMINI,
    OPENROUTER,
    CUSTOM,
    NONE,
}

val AiRequestParametersKey = stringPreferencesKey("ai_request_parameters")

fun archiveAiProvider(provider: String): AiProvider? =
    when (provider) {
        "OpenAI", "CHATGPT" -> AiProvider.CHATGPT
        "Gemini", "GEMINI" -> AiProvider.GEMINI
        "OpenRouter", "OPENROUTER" -> AiProvider.OPENROUTER
        "Custom", "CUSTOM" -> AiProvider.CUSTOM
        "None", "NONE" -> AiProvider.NONE
        else -> null
    }

/** Shared configuration for lyrics and future AI features, using the same settings page. */
fun Preferences.aiServiceConfig(): AiServiceConfig? {
    val provider = archiveAiProvider(this[AiProviderKey] ?: "Gemini") ?: return null
    return AiServiceConfig(
        provider = provider,
        apiKey = this[OpenRouterApiKey].orEmpty(),
        customEndpoint = this[OpenRouterBaseUrlKey] ?: OpenRouterDefaultBaseUrl,
        model = this[OpenRouterModelKey] ?: OpenRouterDefaultModel,
        requestParameters = this[AiRequestParametersKey].orEmpty(),
    )
}
