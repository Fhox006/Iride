/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.metrolist.music.db.MusicDatabase
import com.metrolist.music.db.entities.LyricsEntity
import com.metrolist.music.db.entities.LyricsEntity.Companion.LYRICS_NOT_FOUND
import com.metrolist.music.lyrics.LyricsEntry
import com.metrolist.music.lyrics.LyricsHelper
import com.metrolist.music.lyrics.LyricsTier
import com.metrolist.music.lyrics.LyricsUtils
import com.metrolist.music.models.MediaMetadata
import com.metrolist.music.ui.component.LyricsListItem
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

sealed class LyricsSearchStatus {
    object Idle : LyricsSearchStatus()
    object Loading : LyricsSearchStatus()
    object FoundPlain : LyricsSearchStatus()
    object FoundLine : LyricsSearchStatus()
    object FoundWord : LyricsSearchStatus()
    object NotFoundTemporary : LyricsSearchStatus()
    object NotFoundFinal : LyricsSearchStatus()
}

private const val DELAYED_RETRY_MS = 15_000L

@HiltViewModel
class LyricsViewModel @Inject constructor(
    private val lyricsHelper: LyricsHelper,
    private val database: MusicDatabase,
) : ViewModel() {
    private var processJob: kotlinx.coroutines.Job? = null
    private var progressiveJob: kotlinx.coroutines.Job? = null
    private var delayedRetryJob: kotlinx.coroutines.Job? = null

    private var loadedMediaId: String? = null
    private var delayedRetriedMediaId: String? = null

    val lyricsSearchStatus = MutableStateFlow<LyricsSearchStatus>(LyricsSearchStatus.Idle)

    private val _lyricsRevision = MutableStateFlow(0)
    val lyricsRevision: StateFlow<Int> = _lyricsRevision.asStateFlow()

    private val _displayedLyrics = MutableStateFlow<String?>(null)
    val displayedLyrics: StateFlow<String?> = _displayedLyrics.asStateFlow()

    private val _lines = MutableStateFlow<List<LyricsEntry>>(emptyList())
    val lines: StateFlow<List<LyricsEntry>> = _lines.asStateFlow()

    private val _mergedLyricsList = MutableStateFlow<List<LyricsListItem>>(emptyList())
    val mergedLyricsList: StateFlow<List<LyricsListItem>> = _mergedLyricsList.asStateFlow()

    fun processLyrics(
        lyrics: String?,
        enabledLanguages: List<String>,
        romanizeCyrillicByLine: Boolean,
        showIntervalIndicator: Boolean
    ) {
        _displayedLyrics.value = lyrics
        processJob?.cancel()
        processJob = viewModelScope.launch {
            val processedLines = withContext(Dispatchers.Default) {
                if (lyrics == null || lyrics == LYRICS_NOT_FOUND) {
                    emptyList()
                } else {
                    val timestampRegex = Regex("\\[\\d{1,2}:\\d{2}")
                    val isLrc = timestampRegex.containsMatchIn(lyrics)
                    val parsedLines = if (isLrc) LyricsUtils.parseLyrics(lyrics) else emptyList()

                    if (parsedLines.isNotEmpty()) {
                        listOf(LyricsEntry.HEAD_LYRICS_ENTRY) + parsedLines
                    } else {
                        val baseTime = 1000000L
                        lyrics.lines()
                            .filter { it.isNotBlank() && !timestampRegex.containsMatchIn(it) }
                            .mapIndexed { index, line ->
                                LyricsEntry(baseTime + index, line)
                            }
                    }
                }
            }

            _lines.value = processedLines
            updateMergedList(processedLines, showIntervalIndicator)

            if (lyrics != null && lyrics != LYRICS_NOT_FOUND && enabledLanguages.isNotEmpty()) {
                launch(Dispatchers.Default) {
                    // Detection over the full lyrics is expensive; with per-line romanization
                    // disabled the language is detected once instead of once per line.
                    val detectedLanguage =
                        if (romanizeCyrillicByLine) null else LyricsUtils.detectLanguage(lyrics, enabledLanguages)
                    processedLines.forEach { entry ->
                        if (entry == LyricsEntry.HEAD_LYRICS_ENTRY) return@forEach
                        entry.romanizedTextFlow.value =
                            if (romanizeCyrillicByLine) {
                                LyricsUtils.romanize(
                                    text = lyrics,
                                    line = entry.text,
                                    enabledLanguages = enabledLanguages,
                                    romanizeCyrillicByLine = true
                                )
                            } else {
                                LyricsUtils.romanizeDetected(detectedLanguage, entry.text)
                            }
                    }
                }
            }
        }
    }

    /** True while a progressive fetch for [mediaId] is still running. */
    fun isProgressiveLoading(mediaId: String): Boolean =
        loadedMediaId == mediaId && progressiveJob?.isActive == true

    fun loadProgressiveLyrics(
        mediaMetadata: MediaMetadata,
        enabledLanguages: List<String>,
        romanizeCyrillicByLine: Boolean,
        showIntervalIndicator: Boolean,
        force: Boolean = false,
        silent: Boolean = false,
    ) {
        // Idempotent per song: a still-running job or an already-reached SYNCED_WORD
        // must not be torn down by a panel remount. But a *finished* job stuck on a
        // poor tier (PLAIN/LINE/not-found) is NOT terminal: remounts may retry so a
        // slow word-level provider still gets its chance to upgrade the display.
        // Only a different song or an explicit force restarts the pipeline.
        val isJobActive = progressiveJob?.isActive == true
        val isTerminalWord = lyricsSearchStatus.value == LyricsSearchStatus.FoundWord
        val alreadyHandled = loadedMediaId == mediaMetadata.id && (isJobActive || isTerminalWord)
        if (alreadyHandled && !force) return

        progressiveJob?.cancel()
        processJob?.cancel()
        delayedRetryJob?.cancel()
        // A new pipeline supersedes any pending delayed retry. Re-arm it for a new
        // song, or for an explicit manual refetch (silent auto-retries don't re-arm,
        // so there is at most one delayed retry per arming).
        if (loadedMediaId != mediaMetadata.id || (force && !silent)) {
            delayedRetriedMediaId = null
        }
        // Silent retry (same song, lines already on screen): keep showing the current
        // lyrics while searching for an upgrade in the background instead of clearing
        // the UI back to shimmer.
        val keepDisplayed = silent && loadedMediaId == mediaMetadata.id && _lines.value.isNotEmpty()
        loadedMediaId = mediaMetadata.id
        if (!keepDisplayed) {
            lyricsSearchStatus.value = LyricsSearchStatus.Loading
            _displayedLyrics.value = null
            _lines.value = emptyList()
            _mergedLyricsList.value = emptyList()
        }

        progressiveJob = viewModelScope.launch {
            val cached = withContext(Dispatchers.IO) {
                database.lyrics(mediaMetadata.id).first()
            }
            if (cached != null && cached.lyrics == LYRICS_NOT_FOUND) {
                database.query { delete(cached) }
            }

            val cachedTier = if (cached != null && cached.lyrics != LYRICS_NOT_FOUND) {
                LyricsUtils.detectTier(cached.lyrics)
            } else LyricsTier.PLAIN

            val preservedTranslatedLyrics = cached?.translatedLyrics.orEmpty()
            val preservedTranslationLanguage = cached?.translationLanguage.orEmpty()
            val preservedTranslationMode = cached?.translationMode.orEmpty()

            if (cached != null && cached.lyrics != LYRICS_NOT_FOUND) {
                processLyrics(cached.lyrics, enabledLanguages, romanizeCyrillicByLine, showIntervalIndicator)
                lyricsSearchStatus.value = when (cachedTier) {
                    LyricsTier.SYNCED_WORD -> LyricsSearchStatus.FoundWord
                    LyricsTier.SYNCED_LINE -> LyricsSearchStatus.FoundLine
                    else -> LyricsSearchStatus.FoundPlain
                }
                if (cachedTier == LyricsTier.SYNCED_WORD) return@launch
                if (cached.translatedLyrics.isNullOrBlank()) {
                    database.query { delete(cached) }
                } else {
                    Timber.d("Skipping cached lyrics delete: row has saved translations (${cached.translationLanguage}/${cached.translationMode})")
                }
            }

            val notFoundJob = launch {
                delay(3000L)
                if (lyricsSearchStatus.value == LyricsSearchStatus.Loading) {
                    lyricsSearchStatus.value = LyricsSearchStatus.NotFoundTemporary
                }
            }

            var bestTierSaved = cachedTier
            var wordTierLocked = false

            lyricsHelper.getLyricsProgressive(mediaMetadata) { result, tier ->
                if (wordTierLocked) return@getLyricsProgressive

                val isUpgrade = tier.ordinal > bestTierSaved.ordinal
                val isFirstResult = bestTierSaved == LyricsTier.PLAIN && lyricsSearchStatus.value == LyricsSearchStatus.Loading
                    || lyricsSearchStatus.value == LyricsSearchStatus.NotFoundTemporary

                val shouldUpdate = when {
                    tier == LyricsTier.SYNCED_WORD -> true
                    bestTierSaved == LyricsTier.SYNCED_LINE && tier == LyricsTier.SYNCED_LINE -> false
                    isUpgrade -> true
                    isFirstResult -> true
                    else -> false
                }

                if (shouldUpdate) {
                    notFoundJob.cancel()
                    bestTierSaved = tier
                    processLyrics(result.lyrics, enabledLanguages, romanizeCyrillicByLine, showIntervalIndicator)
                    database.query {
                        upsert(
                            LyricsEntity(
                                id = mediaMetadata.id,
                                lyrics = result.lyrics,
                                provider = result.provider,
                                translatedLyrics = preservedTranslatedLyrics,
                                translationLanguage = preservedTranslationLanguage,
                                translationMode = preservedTranslationMode,
                            )
                        )
                    }
                    lyricsSearchStatus.value = when (tier) {
                        LyricsTier.SYNCED_WORD -> LyricsSearchStatus.FoundWord
                        LyricsTier.SYNCED_LINE -> LyricsSearchStatus.FoundLine
                        else -> LyricsSearchStatus.FoundPlain
                    }
                    if (tier == LyricsTier.SYNCED_WORD) {
                        wordTierLocked = true
                    }
                }
            }

            notFoundJob.cancel()
            if (lyricsSearchStatus.value == LyricsSearchStatus.Loading ||
                lyricsSearchStatus.value == LyricsSearchStatus.NotFoundTemporary) {
                lyricsSearchStatus.value = LyricsSearchStatus.NotFoundFinal
            }

            // One delayed background retry if we settled below word-level: transient
            // failures (rate-limit, slow network) get a second chance without any
            // user action. Silent (current text stays), once per song per arming.
            val settledPoor = lyricsSearchStatus.value == LyricsSearchStatus.FoundPlain ||
                lyricsSearchStatus.value == LyricsSearchStatus.FoundLine ||
                lyricsSearchStatus.value == LyricsSearchStatus.NotFoundFinal
            if (settledPoor && delayedRetriedMediaId != mediaMetadata.id) {
                delayedRetriedMediaId = mediaMetadata.id
                delayedRetryJob?.cancel()
                delayedRetryJob = viewModelScope.launch {
                    kotlinx.coroutines.delay(DELAYED_RETRY_MS)
                    val stillPoor = lyricsSearchStatus.value == LyricsSearchStatus.FoundPlain ||
                        lyricsSearchStatus.value == LyricsSearchStatus.FoundLine ||
                        lyricsSearchStatus.value == LyricsSearchStatus.NotFoundFinal ||
                        lyricsSearchStatus.value == LyricsSearchStatus.NotFoundTemporary
                    if (loadedMediaId == mediaMetadata.id &&
                        !isProgressiveLoading(mediaMetadata.id) && stillPoor
                    ) {
                        loadProgressiveLyrics(
                            mediaMetadata,
                            enabledLanguages,
                            romanizeCyrillicByLine,
                            showIntervalIndicator,
                            force = true,
                            silent = true,
                        )
                    }
                }
            }
        }
    }

    private fun updateMergedList(lines: List<LyricsEntry>, showIntervalIndicator: Boolean) {
        val result = mutableListOf<LyricsListItem>()
        if (lines.isEmpty()) {
            _mergedLyricsList.value = result
            return
        }
        lines.forEachIndexed { i, entry ->
            if (entry.text.isNotBlank()) {
                result.add(LyricsListItem.Line(i, entry))
            }
            if (showIntervalIndicator && i < lines.size - 1) {
                val nextStart = lines[i + 1].time
                val currentEnd = if (!entry.words.isNullOrEmpty()) {
                    (entry.words.last().endTime * 1000).toLong()
                } else if (entry.text.isBlank()) {
                    entry.time
                } else {
                    null
                }

                if (currentEnd != null && currentEnd < nextStart) {
                    val gap = nextStart - currentEnd
                    if (gap > 4000L) {
                        result.add(LyricsListItem.Indicator(i, gap, currentEnd, nextStart, lines[i + 1].agent))
                    }
                }
            }
        }
        _mergedLyricsList.value = result
    }
}