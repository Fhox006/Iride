/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.lyrics

import android.content.Context
import com.metrolist.music.db.MusicDatabase
import com.metrolist.music.db.entities.LyricsEntity
import com.metrolist.music.db.entities.SongEntity
import com.metrolist.music.api.DeepLService
import com.metrolist.music.api.MistralService
import com.metrolist.music.api.OpenRouterService
import com.metrolist.music.api.OpenRouterStreamingService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

/**
 * A helper class that provides AI-powered translation for lyrics.
 */
object LyricsTranslationHelper {
    private val _status = MutableStateFlow<TranslationStatus>(TranslationStatus.Idle)
    val status: StateFlow<TranslationStatus> = _status.asStateFlow()

    private val _hasActiveTranslations = MutableStateFlow(false)
    val hasActiveTranslations: StateFlow<Boolean> = _hasActiveTranslations.asStateFlow()

    private val _translationSaved = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val translationSaved: SharedFlow<Unit> = _translationSaved.asSharedFlow()

    private val _manualTrigger = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val manualTrigger: SharedFlow<Unit> = _manualTrigger.asSharedFlow()

    private val _clearTranslationsTrigger = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val clearTranslationsTrigger: SharedFlow<Unit> = _clearTranslationsTrigger.asSharedFlow()

    private var translationJob: kotlinx.coroutines.Job? = null
    private var isCompositionActive = true

    // Cache translations in memory to avoid redundant API calls during a session
    private val translationCache = ConcurrentHashMap<String, List<String>>()

    // Map of language codes to full names for better AI understanding
    private val LanguageCodeToName = mapOf(
        "en" to "English",
        "es" to "Spanish",
        "fr" to "French",
        "de" to "German",
        "it" to "Italian",
        "pt" to "Portuguese",
        "ru" to "Russian",
        "ja" to "Japanese",
        "ko" to "Korean",
        "zh" to "Chinese",
        "ar" to "Arabic",
        "hi" to "Hindi",
        "bn" to "Bengali",
        "pa" to "Punjabi",
        "tr" to "Turkish",
        "vi" to "Vietnamese",
        "th" to "Thai",
        "id" to "Indonesian",
        "pl" to "Polish",
        "nl" to "Dutch",
        "sv" to "Swedish",
        "uk" to "Ukrainian"
    )

    fun setCompositionActive(active: Boolean) {
        isCompositionActive = active
    }

    fun triggerManualTranslation() {
        _manualTrigger.tryEmit(Unit)
    }

    fun triggerClearTranslations() {
        _clearTranslationsTrigger.tryEmit(Unit)
        _hasActiveTranslations.value = false
    }

    fun clearTranslations(lyrics: LyricsEntity): LyricsEntity {
        return lyrics.copy(
            translatedLyrics = "",
            translationLanguage = "",
            translationMode = ""
        )
    }

    fun cancelTranslation() {
        translationJob?.cancel()
        if (_status.value is TranslationStatus.Translating) {
            _status.value = TranslationStatus.Idle
        }
    }

    fun clearErrorStatus() {
        if (_status.value is TranslationStatus.Error) {
            _status.value = TranslationStatus.Idle
        }
    }

    private fun getCacheKey(text: String, mode: String, targetLanguage: String): String {
        return "${text.hashCode()}_${mode}_${targetLanguage}"
    }

    /**
     * Attempts to parse partial translation content from the AI.
     * This allows updating the UI progressively during streaming.
     */
    private fun tryParsePartialTranslation(content: String, expectedLines: Int): List<String> {
        // AI usually returns lines separated by newlines or numbered lists
        val lines = content.lines()
            .map { it.trim() }
            .filter { it.isNotBlank() }
            // Remove common AI formatting like "1. ", "Line 1: ", etc.
            .map { line ->
                line.replace(Regex("^\\d+\\.\\s*"), "")
                    .replace(Regex("^Line\\s+\\d+:\\s*", RegexOption.IGNORE_CASE), "")
                    .replace(Regex("^-\\s*"), "")
            }
        
        return lines
    }

    fun loadTranslationsFromDatabase(
        lyrics: List<LyricsEntry>,
        lyricsEntity: LyricsEntity?,
        targetLanguage: String,
        mode: String
    ) {
        if (lyricsEntity == null || lyricsEntity.translatedLyrics.isNullOrBlank()) {
            _hasActiveTranslations.value = false
            lyrics.forEach { it.translatedTextFlow.value = null }
            return
        }
        
        // Only load if language and mode match
        if (lyricsEntity.translationLanguage != targetLanguage || lyricsEntity.translationMode != mode) {
            _hasActiveTranslations.value = false
            lyrics.forEach { it.translatedTextFlow.value = null }
            return
        }
        
        val translatedLines = lyricsEntity.translatedLyrics.split("\n")
        val nonEmptyEntries = lyrics.filter { it.text.isNotBlank() }
        
        if (translatedLines.size >= nonEmptyEntries.size) {
            var transIndex = 0
            lyrics.forEach { entry ->
                if (entry.text.isNotBlank() && transIndex < translatedLines.size) {
                    entry.translatedTextFlow.value = translatedLines[transIndex]
                    transIndex++
                }
            }
            
            // Also cache them
            val fullText = nonEmptyEntries.joinToString("\n") { it.text }
            val cacheKey = getCacheKey(fullText, mode, targetLanguage)
            translationCache[cacheKey] = translatedLines
            _hasActiveTranslations.value = true
        }
    }

    fun translateLyrics(
        lyrics: List<LyricsEntry>,
        targetLanguage: String,
        apiKey: String,
        baseUrl: String,
        model: String,
        mode: String,
        scope: CoroutineScope,
        context: Context,
        provider: String = "OpenRouter",
        deeplApiKey: String = "",
        deeplFormality: String = "default",
        useStreaming: Boolean = true,
        songId: String = "",
        database: MusicDatabase? = null,
        systemPrompt: String = "",
    ) {
        translationJob?.cancel()
        _status.value = TranslationStatus.Translating

        // Clear existing translations to indicate re-translation
        lyrics.forEach { it.translatedTextFlow.value = null }

        translationJob =
            scope.launch(Dispatchers.IO) {
                try {
                    // Validate inputs
                    val effectiveApiKey = if (provider == "DeepL") deeplApiKey else apiKey
                    if (effectiveApiKey.isBlank()) {
                        _status.value = TranslationStatus.Error(context.getString(com.metrolist.music.R.string.ai_error_api_key_required))
                        return@launch
                    }

                    if (lyrics.isEmpty()) {
                        _status.value = TranslationStatus.Error(context.getString(com.metrolist.music.R.string.ai_error_no_lyrics))
                        return@launch
                    }

                    // Filter out empty lines and keep track of their indices
                    val nonEmptyEntries =
                        lyrics.mapIndexedNotNull { index, entry ->
                            if (entry.text.isNotBlank()) index to entry else null
                        }

                    if (nonEmptyEntries.isEmpty()) {
                        _status.value = TranslationStatus.Error(context.getString(com.metrolist.music.R.string.ai_error_lyrics_empty))
                        return@launch
                    }

                    // Create text from non-empty lines only
                    val fullText = nonEmptyEntries.joinToString("\n") { it.second.text }

                    // Check cache first
                    val cacheKey = getCacheKey(fullText, mode, targetLanguage)
                    val cachedTranslations = translationCache[cacheKey]
                    if (cachedTranslations != null && cachedTranslations.size >= nonEmptyEntries.size) {
                        // Use cached translations
                        nonEmptyEntries.forEachIndexed { idx, (originalIndex, _) ->
                            if (idx < cachedTranslations.size) {
                                lyrics[originalIndex].translatedTextFlow.value = cachedTranslations[idx]
                            }
                        }
                        _hasActiveTranslations.value = true
                        _status.value = TranslationStatus.Success

                        // Persist cached translations to DB. Re-read the lyrics row right
                        // before upserting so a fresher lyrics upsert from the provider during
                        // the API call doesn't get clobbered by a stale snapshot.
                        if (songId.isNotBlank() && database != null) {
                            try {
                                val currentLyrics = database.lyrics(songId).first()
                                if (currentLyrics != null && currentLyrics.translatedLyrics.isNullOrBlank()) {
                                    database.query {
                                        upsert(
                                            currentLyrics.copy(
                                                translatedLyrics = cachedTranslations.joinToString("\n"),
                                                translationLanguage = targetLanguage,
                                                translationMode = mode,
                                            ),
                                        )
                                    }
                                    _translationSaved.tryEmit(Unit)
                                }
                            } catch (e: Exception) {
                                Timber.e(e, "Failed to persist cached translations to database")
                            }
                        }

                        delay(3000)
                        if (_status.value is TranslationStatus.Success && isCompositionActive) {
                            _status.value = TranslationStatus.Idle
                        }
                        return@launch
                    }

                    // Validate language for all modes
                    if (targetLanguage.isBlank()) {
                        _status.value = TranslationStatus.Error(context.getString(com.metrolist.music.R.string.ai_error_language_required))
                        return@launch
                    }

                    // Convert language code to full language name for better AI understanding
                    val fullLanguageName =
                        LanguageCodeToName[targetLanguage]
                            ?: try {
                                Locale.forLanguageTag(targetLanguage).displayLanguage.takeIf { it.isNotBlank() && it != targetLanguage }
                            } catch (e: Exception) {
                                null
                            }
                            ?: targetLanguage

                    val result =
                        if (provider == "DeepL") {
                            Timber.d("Using DeepL for translation")
                            // DeepL only supports translation mode
                            DeepLService.translate(
                                text = fullText,
                                targetLanguage = targetLanguage,
                                apiKey = deeplApiKey,
                                formality = deeplFormality,
                            )
                        } else if (provider == "Mistral") {
                            Timber.d("Using Mistral for translation")
                            // Use Mistral API directly
                            MistralService.translate(
                                text = fullText,
                                targetLanguage = fullLanguageName,
                                apiKey = apiKey,
                                model = model,
                                mode = mode,
                                customSystemPrompt = systemPrompt,
                            )
                        } else if (useStreaming && provider != "Custom") {
                            Timber.d("Using streaming for translation with provider: $provider")
                            // Use streaming for supported providers
                            var translatedLines: List<String>? = null
                            var hasError = false
                            var errorMessage = ""
                            val contentAccumulator = StringBuilder()
                            val streamStartTime = System.currentTimeMillis()

                            OpenRouterStreamingService
                                .streamTranslation(
                                    text = fullText,
                                    targetLanguage = fullLanguageName,
                                    apiKey = apiKey,
                                    baseUrl = baseUrl,
                                    model = model,
                                    mode = mode,
                                    customSystemPrompt = systemPrompt,
                                ).collect { chunk ->
                                    Timber.v("Received streaming chunk: $chunk")
                                    when (chunk) {
                                        is OpenRouterStreamingService.StreamChunk.Content -> {
                                            // Accumulate content for progressive parsing
                                            contentAccumulator.append(chunk.text)

                                            // Don't reveal partial lines for the first ~1.5s: early
                                            // chunks are mid-token (stray "[", numbering, markdown
                                            // fences) and read as a glitch if shown immediately.
                                            // Enough usually accumulates by then to parse clean lines.
                                            if (System.currentTimeMillis() - streamStartTime >= 1500L) {
                                                val partialContent = contentAccumulator.toString()
                                                val partialResult = tryParsePartialTranslation(partialContent, nonEmptyEntries.size)
                                                if (partialResult.isNotEmpty()) {
                                                    // Update lyrics with partial translations as they become available
                                                    partialResult.forEachIndexed { idx, translation ->
                                                        if (idx < nonEmptyEntries.size && translation.isNotBlank()) {
                                                            val originalIndex = nonEmptyEntries[idx].first
                                                            lyrics[originalIndex].translatedTextFlow.value = translation
                                                        }
                                                    }
                                                    _status.value = TranslationStatus.Translating
                                                }
                                            }
                                        }

                                        is OpenRouterStreamingService.StreamChunk.Complete -> {
                                            Timber.d("Streaming complete with ${chunk.translatedLines.size} lines")
                                            translatedLines = chunk.translatedLines
                                        }

                                        is OpenRouterStreamingService.StreamChunk.Error -> {
                                            Timber.e("Streaming error: ${chunk.message}")
                                            hasError = true
                                            errorMessage = chunk.message
                                            val maskedKey = if (apiKey.length > 4) apiKey.take(4) + "****" else "****"
                                            val debugText = buildString {
                                                appendLine("=== IRIDE AI DEBUG ===")
                                                appendLine("Provider: $provider")
                                                appendLine("URL: ${baseUrl.ifBlank { "https://openrouter.ai/api/v1/chat/completions" }}")
                                                appendLine("Model: $model")
                                                appendLine("Mode: $mode")
                                                appendLine("API Key: $maskedKey")
                                                appendLine("--- ERROR ---")
                                                appendLine(chunk.message)
                                            }
                                            withContext(Dispatchers.Main) {
                                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                                clipboard.setPrimaryClip(android.content.ClipData.newPlainText("Iride Debug", debugText))
                                            }
                                        }
                                    }
                                }

                            Timber.d("Streaming collection complete. hasError=$hasError, translatedLines=${translatedLines?.size}")
                            if (hasError) {
                                Result.failure(Exception(errorMessage))
                            } else if (translatedLines != null) {
                                Result.success(translatedLines)
                            } else {
                                Result.failure(Exception("No translation received"))
                            }
                        } else {
                            Timber.d("Using non-streaming for translation")
                            // Use non-streaming for Custom provider or when streaming is disabled
                            OpenRouterService.translate(
                                text = fullText,
                                targetLanguage = fullLanguageName,
                                apiKey = apiKey,
                                baseUrl = baseUrl,
                                model = model,
                                mode = mode,
                                customSystemPrompt = systemPrompt,
                            )
                        }

                    result
                        .onSuccess { translatedLines ->
                            // Save to database FIRST, before any UI-state checks. The user
                            // paid for an API call and may close the lyrics view before the
                            // success status flash fires; the translation must land in the DB
                            // regardless of composition lifetime.
                            //
                            // Re-read the lyrics row right before the upsert: the lyric provider
                            // may have already written a fresher entity while the API call was
                            // in flight, and copying from a stale snapshot would resurrect
                            // older lyrics+provider and re-wipe translations on the next fetch.
                            if (songId.isNotBlank() && database != null) {
                                try {
                                    val currentLyrics = database.lyrics(songId).first()
                                    if (currentLyrics != null) {
                                        database.query {
                                            upsert(
                                                currentLyrics.copy(
                                                    translatedLyrics = translatedLines.joinToString("\n"),
                                                    translationLanguage = targetLanguage,
                                                    translationMode = mode,
                                                ),
                                            )
                                        }
                                        _translationSaved.tryEmit(Unit)
                                    }
                                } catch (e: Exception) {
                                    Timber.e(e, "Failed to save translated lyrics to database")
                                }
                            }

                            // Composition torn down while the API call was running — DB write
                            // already done above, skip UI updates that would touch a dead view.
                            if (!isCompositionActive) {
                                return@onSuccess
                            }

                            // Cache the translations for this session
                            val cacheKey = getCacheKey(fullText, mode, targetLanguage)
                            translationCache[cacheKey] = translatedLines

                            // Map translations back to original non-empty entries only
                            val expectedCount = nonEmptyEntries.size

                            when {
                                translatedLines.size >= expectedCount -> {
                                    // Perfect match or more - map to non-empty entries
                                    nonEmptyEntries.forEachIndexed { idx, (originalIndex, _) ->
                                        lyrics[originalIndex].translatedTextFlow.value = translatedLines[idx]
                                    }
                                    _hasActiveTranslations.value = true
                                    _status.value = TranslationStatus.Success
                                }

                                translatedLines.size < expectedCount -> {
                                    // Fewer translations than expected - map what we have
                                    translatedLines.forEachIndexed { idx, translation ->
                                        if (idx < nonEmptyEntries.size) {
                                            val originalIndex = nonEmptyEntries[idx].first
                                            lyrics[originalIndex].translatedTextFlow.value = translation
                                        }
                                    }
                                    _hasActiveTranslations.value = true
                                    _status.value = TranslationStatus.Success
                                }
                            }

                            // Auto-hide success message after 3 seconds
                            delay(3000)
                            if (_status.value is TranslationStatus.Success && isCompositionActive) {
                                _status.value = TranslationStatus.Idle
                            }
                        }
                        .onFailure { error ->
                            if (!isCompositionActive) {
                                return@onFailure
                            }

                            val errorMessage = error.message ?: context.getString(com.metrolist.music.R.string.ai_error_unknown)

                            // Show error in UI
                            _status.value = TranslationStatus.Error(errorMessage)
                        }
                } catch (e: Exception) {
                    // Ignore cancellation exceptions or if composition is no longer active
                    if (e !is kotlinx.coroutines.CancellationException && isCompositionActive) {
                        val errorMessage = e.message ?: context.getString(com.metrolist.music.R.string.ai_error_translation_failed)
                        _status.value = TranslationStatus.Error(errorMessage)
                    }
                }
            }
    }

    sealed class TranslationStatus {
        data object Idle : TranslationStatus()
        data object Translating : TranslationStatus()
        data object Success : TranslationStatus()
        data class Error(val message: String) : TranslationStatus()
    }
}
