/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.api

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import timber.log.Timber
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.concurrent.TimeUnit

object OpenRouterStreamingService {
    private val client =
        OkHttpClient
            .Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    private val JSON = "application/json; charset=utf-8".toMediaType()
    private val json = Json { ignoreUnknownKeys = true }

    private suspend fun validateAndRefreshKey(
        apiKey: String,
        readKeyFromPrefs: suspend () -> String,
    ): String {
        if (apiKey.isNotBlank()) return apiKey
        val fresh = readKeyFromPrefs()
        if (fresh.isBlank()) throw IllegalStateException("API key not configured")
        return fresh
    }

    suspend fun isKeyValid(apiKey: String, baseUrl: String = "https://openrouter.ai/api/v1/chat/completions"): Boolean =
        withContext(Dispatchers.IO) {
            if (apiKey.isBlank()) return@withContext false
            try {
                val modelsUrl = baseUrl.replace("/chat/completions", "/models")
                val request =
                    Request
                        .Builder()
                        .url(modelsUrl)
                        .addHeader("Authorization", "Bearer ${apiKey.trim()}")
                        .build()
                val response = client.newCall(request).execute()
                val code = response.code
                response.close()
                code in 200..299
            } catch (e: Exception) {
                false
            }
        }

    fun streamTranslation(
        text: String,
        targetLanguage: String,
        apiKey: String,
        baseUrl: String,
        model: String,
        mode: String,
        customSystemPrompt: String = "",
        readKeyFromPrefs: (suspend () -> String)? = null,
    ): Flow<StreamChunk> =
        flow {
            if (text.isBlank()) {
                emit(StreamChunk.Error("Input text is empty"))
                return@flow
            }

            val effectiveKey =
                try {
                    validateAndRefreshKey(apiKey, readKeyFromPrefs ?: { apiKey })
                } catch (e: IllegalStateException) {
                    emit(StreamChunk.Error(e.message ?: "API key not configured"))
                    return@flow
                }
            var activeKey = effectiveKey
            var keyRefreshed = false

            val lines = text.lines()
            val lineCount = lines.size

            Timber.d("Starting streaming translation for $lineCount lines")

            try {
                val systemPrompt =
                    if (customSystemPrompt.isNotBlank()) {
                        customSystemPrompt.replace("{lineCount}", lineCount.toString())
                    } else {
                        """You are a precise lyrics translation assistant. Your output must ALWAYS be a valid JSON array of strings.

CRITICAL RULES:
1. Output ONLY a JSON array: ["line1", "line2", "line3"]
2. NO explanations, NO questions, NO additional text
3. Each input line maps to exactly one output line
4. Preserve empty lines as empty strings ""
5. Return EXACTLY $lineCount items in the array
6. If uncertain, provide best approximation but maintain line count"""
                    }

                val userPrompt =
                    when (mode) {
                        "Transcribed" -> {
                            """Transcribe/transliterate the following $lineCount lines phonetically into $targetLanguage script.

CRITICAL REQUIREMENTS:
- Convert the SOUND/PRONUNCIATION of the original text into $targetLanguage script
- DO NOT translate the meaning - only represent how the original words SOUND
- Use the native script of $targetLanguage
- Preserve the original pronunciation as closely as possible
- Keep punctuation and formatting

Input ($lineCount lines):
$text

Output MUST be a JSON array with EXACTLY $lineCount strings."""
                        }

                        else -> {
                            """Translate the following $lineCount lines to $targetLanguage.

IMPORTANT:
- Provide natural, accurate translation
- Maintain poetic flow and meaning
- Keep punctuation appropriate for target language
- Preserve line-by-line structure exactly

Input ($lineCount lines):
$text

Output MUST be a JSON array with EXACTLY $lineCount strings."""
                        }
                    }

                val messages =
                    JSONArray().apply {
                        put(
                            JSONObject().apply {
                                put("role", "system")
                                put("content", systemPrompt)
                            },
                        )
                        put(
                            JSONObject().apply {
                                put("role", "user")
                                put("content", userPrompt)
                            },
                        )
                    }

                val jsonBody =
                    JSONObject().apply {
                        if (model.isNotBlank()) {
                            put("model", model)
                        }
                        put("messages", messages)
                        put("stream", true)
                        put("temperature", 0.3)
                        put("max_tokens", lineCount * 100)
                    }

                fun buildRequest(key: String): Request =
                    Request
                        .Builder()
                        .url(baseUrl.ifBlank { "https://openrouter.ai/api/v1/chat/completions" })
                        .apply {
                            if (key.isNotBlank()) {
                                addHeader("Authorization", "Bearer ${key.trim()}")
                            }
                        }.addHeader("Content-Type", "application/json")
                        .addHeader("HTTP-Referer", "https://github.com/MetrolistGroup/Metrolist")
                        .addHeader("X-Title", "Metrolist")
                        .post(jsonBody.toString().toRequestBody(JSON))
                        .build()

                // Execute first request; retry once with fresh key on 401/403
                var firstResponse = client.newCall(buildRequest(activeKey)).execute()
                if ((firstResponse.code == 401 || firstResponse.code == 403) && !keyRefreshed && readKeyFromPrefs != null) {
                    firstResponse.close()
                    val freshKey = readKeyFromPrefs()
                    if (freshKey.isNotBlank() && freshKey != activeKey) {
                        activeKey = freshKey
                        keyRefreshed = true
                        firstResponse = client.newCall(buildRequest(activeKey)).execute()
                    } else {
                        emit(StreamChunk.Error("API key invalid or expired"))
                        return@flow
                    }
                }

                firstResponse.use { response ->
                    Timber.d("Got streaming response: ${response.code}")

                    if (!response.isSuccessful) {
                        val rawBody = response.body?.string() ?: "(empty body)"
                        Timber.e("API error [${response.code}] raw body: $rawBody")
                        if (response.code == 401 || response.code == 403) {
                            emit(StreamChunk.Error("API key invalid or expired"))
                        } else {
                            emit(StreamChunk.Error("HTTP ${response.code}: $rawBody"))
                        }
                        return@flow
                    }

                    val reader = BufferedReader(InputStreamReader(response.body?.byteStream()))
                    var line: String?
                    val contentBuilder = StringBuilder()
                    var chunkCount = 0

                    while (reader.readLine().also { line = it } != null) {
                        line?.let { currentLine ->
                            if (currentLine.startsWith("data: ")) {
                                val data = currentLine.substring(6)
                                if (data == "[DONE]") {
                                    Timber.d("Streaming complete, received $chunkCount chunks")
                                    val fullContent = contentBuilder.toString()
                                    Timber.d("Full content length: ${fullContent.length}")
                                    val result = parseTranslationContent(fullContent, lineCount)
                                    result
                                        .onSuccess { translatedLines ->
                                            Timber.d("Successfully parsed ${translatedLines.size} lines")
                                            emit(StreamChunk.Complete(translatedLines))
                                        }.onFailure { error ->
                                            Timber.e(error, "Failed to parse translation")
                                            emit(StreamChunk.Error(error.message ?: "Parsing failed"))
                                        }
                                    return@flow
                                }

                                try {
                                    val jsonObject = json.parseToJsonElement(data).jsonObject
                                    val choices = jsonObject["choices"]?.jsonArray
                                    val delta =
                                        choices
                                            ?.get(0)
                                            ?.jsonObject
                                            ?.get("delta")
                                            ?.jsonObject
                                    val content = delta?.get("content")?.jsonPrimitive?.content

                                    content?.let { chunk ->
                                        contentBuilder.append(chunk)
                                        chunkCount++
                                        emit(StreamChunk.Content(chunk))
                                    }
                                } catch (e: Exception) {
                                    Timber.v("Ignored malformed chunk: ${e.message}")
                                }
                            }
                        }
                    }

                    if (contentBuilder.isNotEmpty()) {
                        Timber.w("Stream ended without [DONE] marker, attempting to parse content")
                        val fullContent = contentBuilder.toString()
                        val result = parseTranslationContent(fullContent, lineCount)
                        result
                            .onSuccess { translatedLines ->
                                emit(StreamChunk.Complete(translatedLines))
                            }.onFailure { error ->
                                emit(StreamChunk.Error(error.message ?: "Parsing failed"))
                            }
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Streaming error")
                emit(StreamChunk.Error(e.message ?: "Unknown error"))
            }
        }.flowOn(Dispatchers.IO)

    private fun parseTranslationContent(
        content: String,
        expectedLineCount: Int,
    ): Result<List<String>> {
        var translatedLines: List<String>? = null

        try {
            val jsonArray = JSONArray(content.trim())
            translatedLines = (0 until jsonArray.length()).map { jsonArray.optString(it) }
        } catch (e: Exception) {
            var cleanedContent = content.replace("```json", "").replace("```", "").trim()

            try {
                val jsonArray = JSONArray(cleanedContent)
                translatedLines = (0 until jsonArray.length()).map { jsonArray.optString(it) }
            } catch (e2: Exception) {
                val startIdx = cleanedContent.indexOf('[')
                val endIdx = cleanedContent.lastIndexOf(']')

                if (startIdx != -1 && endIdx != -1 && endIdx > startIdx) {
                    val jsonString = cleanedContent.substring(startIdx, endIdx + 1)
                    try {
                        val jsonArray = JSONArray(jsonString)
                        translatedLines = (0 until jsonArray.length()).map { jsonArray.optString(it) }
                    } catch (e3: Exception) {
                        translatedLines =
                            cleanedContent
                                .lines()
                                .filter { it.trim().isNotEmpty() }
                                .map { it.trim().removeSurrounding("\"").removeSurrounding("'") }
                    }
                }
            }
        }

        if (translatedLines == null) {
            return Result.failure(Exception("Failed to parse translation"))
        }

        return when {
            translatedLines.size == expectedLineCount -> {
                Result.success(translatedLines)
            }

            translatedLines.size > expectedLineCount -> {
                Result.success(translatedLines.take(expectedLineCount))
            }

            else -> {
                val paddedLines = translatedLines.toMutableList()
                while (paddedLines.size < expectedLineCount) {
                    paddedLines.add("")
                }
                Result.success(paddedLines)
            }
        }
    }

    sealed class StreamChunk {
        data class Content(
            val text: String,
        ) : StreamChunk()

        data class Complete(
            val translatedLines: List<String>,
        ) : StreamChunk()

        data class Error(
            val message: String,
        ) : StreamChunk()
    }
}
