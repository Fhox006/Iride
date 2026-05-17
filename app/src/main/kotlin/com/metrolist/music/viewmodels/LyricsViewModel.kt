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

sealed class LyricsSearchStatus {
    object Idle : LyricsSearchStatus()
    object Loading : LyricsSearchStatus()            // shimmer — no results yet
    object FoundPlain : LyricsSearchStatus()         // plain text found, still searching
    object FoundLine : LyricsSearchStatus()          // synced line found, still waiting for WORD
    object FoundWord : LyricsSearchStatus()          // word-level found, done
    object NotFoundTemporary : LyricsSearchStatus()  // 3s elapsed, no results yet — still searching in bg
    object NotFoundFinal : LyricsSearchStatus()      // search fully ended with no results
}

@HiltViewModel
class LyricsViewModel @Inject constructor(
    private val lyricsHelper: LyricsHelper,
    private val database: MusicDatabase,
) : ViewModel() {
    private var processJob: kotlinx.coroutines.Job? = null
    private var progressiveJob: kotlinx.coroutines.Job? = null

    val lyricsSearchStatus = MutableStateFlow<LyricsSearchStatus>(LyricsSearchStatus.Idle)

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
                        // Fallback for unsynced or invalid LRC
                        val baseTime = 1000000L // Start at 1000s to avoid overlap with real start
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

            // Romanize in the background after the UI has been updated
            if (lyrics != null && lyrics != LYRICS_NOT_FOUND && enabledLanguages.isNotEmpty()) {
                launch(Dispatchers.Default) {
                    processedLines.forEach { entry ->
                        if (entry == LyricsEntry.HEAD_LYRICS_ENTRY) return@forEach
                        entry.romanizedTextFlow.value = LyricsUtils.romanize(
                            text = lyrics,
                            line = entry.text,
                            enabledLanguages = enabledLanguages,
                            romanizeCyrillicByLine = romanizeCyrillicByLine
                        )
                    }
                }
            }
        }
    }

    fun loadProgressiveLyrics(
        mediaMetadata: MediaMetadata,
        enabledLanguages: List<String>,
        romanizeCyrillicByLine: Boolean,
        showIntervalIndicator: Boolean,
    ) {
        progressiveJob?.cancel()
        processJob?.cancel()
        lyricsSearchStatus.value = LyricsSearchStatus.Loading

        progressiveJob = viewModelScope.launch {
            // --- Cache check ---
            val cached = withContext(Dispatchers.IO) {
                database.lyrics(mediaMetadata.id).first()
            }
            // If DB has LYRICS_NOT_FOUND from a previous failed search, delete it and search fresh
            if (cached != null && cached.lyrics == LYRICS_NOT_FOUND) {
                database.query { delete(cached) }
            }

            val cachedTier = if (cached != null && cached.lyrics != LYRICS_NOT_FOUND) {
                LyricsUtils.detectTier(cached.lyrics)
            } else LyricsTier.PLAIN

            if (cached != null && cached.lyrics != LYRICS_NOT_FOUND) {
                processLyrics(cached.lyrics, enabledLanguages, romanizeCyrillicByLine, showIntervalIndicator)
                lyricsSearchStatus.value = when (cachedTier) {
                    LyricsTier.SYNCED_WORD -> LyricsSearchStatus.FoundWord
                    LyricsTier.SYNCED_LINE -> LyricsSearchStatus.FoundLine
                    else -> LyricsSearchStatus.FoundPlain
                }
                if (cachedTier == LyricsTier.SYNCED_WORD) return@launch
                // Non-WORD cache: delete so providers can upsert freely with fresher/better result
                database.query { delete(cached) }
            }
            // getLyricsProgressive always runs unless we returned early on SYNCED_WORD above

            // --- "Not found" timer: after 3s with no result, show temporary message ---
            // Keep searching in background for up to MAX_LYRICS_FETCH_MS
            val notFoundJob = launch {
                delay(3000L)
                // Only show if we still have no lyrics displayed at all
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
                    // If we already have LINE, only accept WORD (block other LINE results)
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
                        upsert(LyricsEntity(mediaMetadata.id, result.lyrics, result.provider))
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

            // Search ended — finalize status
            notFoundJob.cancel()
            if (lyricsSearchStatus.value == LyricsSearchStatus.Loading ||
                lyricsSearchStatus.value == LyricsSearchStatus.NotFoundTemporary) {
                lyricsSearchStatus.value = LyricsSearchStatus.NotFoundFinal
            }
            // If we ended on FoundLine with no WORD upgrade, keep FoundLine (don't downgrade)
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