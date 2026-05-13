/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.api

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object DeepLService {
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(90, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()
    private val JSON = "application/json; charset=utf-8".toMediaType()

    private suspend fun validateAndRefreshKey(
        apiKey: String,
        readKeyFromPrefs: suspend () -> String,
    ): String {
        if (apiKey.isNotBlank()) return apiKey
        val fresh = readKeyFromPrefs()
        if (fresh.isBlank()) throw IllegalStateException("API key not configured")
        return fresh
    }

    suspend fun isKeyValid(apiKey: String): Boolean =
        withContext(Dispatchers.IO) {
            if (apiKey.isBlank()) return@withContext false
            try {
                val usageUrl = if (apiKey.endsWith(":fx")) {
                    "https://api-free.deepl.com/v2/usage"
                } else {
                    "https://api.deepl.com/v2/usage"
                }
                val request = Request.Builder()
                    .url(usageUrl)
                    .addHeader("Authorization", "DeepL-Auth-Key ${apiKey.trim()}")
                    .build()
                val response = client.newCall(request).execute()
                val code = response.code
                response.close()
                code in 200..299
            } catch (e: Exception) {
                false
            }
        }

    suspend fun translate(
        text: String,
        targetLanguage: String,
        apiKey: String,
        formality: String = "default",
        maxRetries: Int = 3,
        readKeyFromPrefs: (suspend () -> String)? = null,
    ): Result<List<String>> = withContext(Dispatchers.IO) {
        var currentAttempt = 0

        // Validate input
        if (text.isBlank()) {
            return@withContext Result.failure(Exception("Input text is empty"))
        }

        val effectiveKey =
            try {
                validateAndRefreshKey(apiKey, readKeyFromPrefs ?: { apiKey })
            } catch (e: IllegalStateException) {
                return@withContext Result.failure(e)
            }
        var activeKey = effectiveKey
        var keyRefreshed = false

        val lines = text.lines()
        val lineCount = lines.size

        // DeepL language codes (uppercase)
        val deeplLangCode = when (targetLanguage.lowercase()) {
            "zh", "zh-cn", "zh-hans" -> "ZH"
            "zh-tw", "zh-hant" -> "ZH"
            "en", "en-us" -> "EN-US"
            "en-gb" -> "EN-GB"
            "pt", "pt-pt" -> "PT-PT"
            "pt-br" -> "PT-BR"
            else -> targetLanguage.uppercase().take(2)
        }

        while (currentAttempt < maxRetries) {
            try {
                // Determine if using free or pro API based on active key
                val baseUrl = if (activeKey.endsWith(":fx")) {
                    "https://api-free.deepl.com/v2/translate"
                } else {
                    "https://api.deepl.com/v2/translate"
                }

                val jsonBody = JSONObject().apply {
                    put("text", JSONArray().apply {
                        lines.forEach { put(it) }
                    })
                    put("target_lang", deeplLangCode)
                    if (formality != "default") {
                        put("formality", formality)
                    }
                    put("preserve_formatting", true)
                }

                val request = Request.Builder()
                    .url(baseUrl)
                    .addHeader("Authorization", "DeepL-Auth-Key ${activeKey.trim()}")
                    .addHeader("Content-Type", "application/json")
                    .post(jsonBody.toString().toRequestBody(JSON))
                    .build()

                val response = client.newCall(request).execute()
                val responseBody = response.body?.string()

                if (!response.isSuccessful) {
                    // On 401/403, attempt a one-shot key refresh before failing
                    if ((response.code == 401 || response.code == 403) && !keyRefreshed && readKeyFromPrefs != null) {
                        val freshKey = readKeyFromPrefs()
                        if (freshKey.isNotBlank() && freshKey != activeKey) {
                            activeKey = freshKey
                            keyRefreshed = true
                            continue
                        }
                        return@withContext Result.failure(Exception("API key invalid or expired"))
                    }
                    if (response.code == 401 || response.code == 403) {
                        return@withContext Result.failure(Exception("API key invalid or expired"))
                    }

                    // Retry on server errors (5xx)
                    if (response.code >= 500) {
                        currentAttempt++
                        kotlinx.coroutines.delay(1000L * currentAttempt)
                        continue
                    }

                    val errorMsg = try {
                        JSONObject(responseBody ?: "").optString("message")
                            ?: "HTTP ${response.code}: ${response.message}"
                    } catch (e: Exception) {
                        "HTTP ${response.code}: ${response.message}"
                    }
                    return@withContext Result.failure(Exception("Translation failed: $errorMsg"))
                }

                if (responseBody == null) {
                    currentAttempt++
                    continue
                }

                val jsonResponse = JSONObject(responseBody)
                val translations = jsonResponse.optJSONArray("translations")
                if (translations != null && translations.length() > 0) {
                    val translatedLines = (0 until translations.length()).map { i ->
                        translations.getJSONObject(i).optString("text", "")
                    }

                    if (translatedLines.size == lineCount) {
                        return@withContext Result.success(translatedLines)
                    } else if (translatedLines.size > lineCount) {
                        return@withContext Result.success(translatedLines.take(lineCount))
                    } else {
                        val paddedLines = translatedLines.toMutableList()
                        while (paddedLines.size < lineCount) {
                            paddedLines.add("")
                        }
                        return@withContext Result.success(paddedLines)
                    }
                }
            } catch (e: Exception) {
                if (currentAttempt == maxRetries - 1) {
                    return@withContext Result.failure(e)
                }
            }
            currentAttempt++
            kotlinx.coroutines.delay(1000L * currentAttempt)
        }
        return@withContext Result.failure(Exception("Max retries exceeded"))
    }
}
