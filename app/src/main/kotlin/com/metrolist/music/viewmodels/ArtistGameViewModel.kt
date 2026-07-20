/**
 * Iride Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.viewmodels

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.metrolist.innertube.YouTube
import com.metrolist.innertube.models.AlbumItem
import com.metrolist.innertube.models.SongItem
import com.metrolist.innertube.models.filterVideoSongs
import com.metrolist.music.ui.screens.artist.ArtistGameAudioService
import com.metrolist.music.utils.dataStore
import com.metrolist.music.utils.get
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.random.Random

data class RoundResult(val selectedId: String, val correct: Boolean)

data class ArtistGameArtistInfo(val name: String, val thumbnailUrl: String?)

sealed interface GameUiState {
    data object Loading : GameUiState
    data class Ready(val bestScoreMs: Long?, val preparing: Boolean) : GameUiState
    data class Countdown(val value: Int) : GameUiState
    data class Playing(
        val roundIndex: Int,
        val totalRounds: Int,
        val options: List<SongItem>,
        val correctId: String,
        val result: RoundResult?,
    ) : GameUiState
    data class Finished(val totalMs: Long, val isNewBest: Boolean) : GameUiState
    data object NotEnoughSongs : GameUiState
}

private data class RoundData(val correct: SongItem, val distractors: List<SongItem>, val positionMs: Long)

/** Snippet start point: a random point 20-70% into the track, or a flat 30s fallback when duration is unknown. */
private fun randomSnippetPositionMs(durationSec: Int?): Long {
    if (durationSec == null || durationSec <= 4) return 30_000L
    val fromSec = (durationSec * 0.2).toInt().coerceAtLeast(1)
    val toSec = (durationSec * 0.7).toInt().coerceAtLeast(fromSec + 1)
    return Random.nextInt(fromSec, toSec) * 1000L
}

@HiltViewModel
class ArtistGameViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    val artistId = savedStateHandle.get<String>("artistId")!!

    var artistInfo by mutableStateOf<ArtistGameArtistInfo?>(null)
        private set

    var uiState by mutableStateOf<GameUiState>(GameUiState.Loading)
        private set

    var elapsedMs by mutableStateOf(0L)
        private set

    private val audioService = ArtistGameAudioService(context)
    private val resolvedUrls = mutableMapOf<String, Uri>()
    private var rounds: List<RoundData> = emptyList()
    private var timerJob: Job? = null
    private var roundJob: Job? = null

    private fun bestScoreKey() = longPreferencesKey("artist_game_best_ms_$artistId")

    init {
        viewModelScope.launch {
            val bestScoreMs = context.dataStore.get(bestScoreKey())
            preload(bestScoreMs)
        }
    }

    private suspend fun preload(bestScoreMs: Long?) {
        val page = YouTube.artist(artistId).getOrNull()
        if (page == null) {
            uiState = GameUiState.NotEnoughSongs
            return
        }
        artistInfo = ArtistGameArtistInfo(page.artist.title, page.artist.thumbnail)
        uiState = GameUiState.Ready(bestScoreMs, preparing = true)

        val directSongs = page.sections.flatMap { it.items.filterIsInstance<SongItem>() }
        val albumIds = page.sections.flatMap { it.items.filterIsInstance<AlbumItem>() }.map { it.browseId }.distinct()
        val albumSongs = albumIds.map { browseId ->
            viewModelScope.async { YouTube.album(browseId).getOrNull()?.songs.orEmpty() }
        }.awaitAll().flatten()

        // Real tracks only — no music videos, no video-only uploads, no blank-title junk entries.
        val pool = (directSongs + albumSongs)
            .filterVideoSongs(disableVideos = true)
            .filter { song ->
                song.title.isNotBlank() &&
                    song.artists.any { it.id == artistId || it.name.equals(page.artist.title, ignoreCase = true) }
            }
            .distinctBy { it.id }

        if (pool.size < 3) {
            uiState = GameUiState.NotEnoughSongs
            return
        }

        val roundCount = minOf(10, pool.size)
        val shuffledPool = pool.shuffled()
        val answers = shuffledPool.take(roundCount)
        rounds = answers.map { correct ->
            val distractors = pool.filter { it.id != correct.id }.shuffled().take(2)
            RoundData(correct, distractors, randomSnippetPositionMs(correct.duration))
        }.filter { it.distractors.size == 2 }

        if (rounds.isEmpty()) {
            uiState = GameUiState.NotEnoughSongs
            return
        }

        // Resolve + fully buffer every round's audio now, so the game itself never waits on the network.
        val prepared = rounds.map { round ->
            viewModelScope.async {
                val uri = audioService.resolveStreamUrl(round.correct.id) ?: return@async null
                audioService.prepareRound(round.correct.id, uri, round.positionMs)
                round.correct.id to uri
            }
        }.awaitAll()
        prepared.filterNotNull().forEach { (id, uri) -> resolvedUrls[id] = uri }

        rounds = rounds.filter { resolvedUrls.containsKey(it.correct.id) }
        if (rounds.isEmpty()) {
            uiState = GameUiState.NotEnoughSongs
            return
        }

        uiState = GameUiState.Ready(bestScoreMs, preparing = false)
    }

    fun onPlayNowClicked() {
        val ready = uiState as? GameUiState.Ready ?: return
        if (ready.preparing) return
        viewModelScope.launch {
            val toneGenerator = ToneGenerator(AudioManager.STREAM_MUSIC, 80)
            for (value in 3 downTo 1) {
                uiState = GameUiState.Countdown(value)
                toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP, 150)
                delay(1000)
            }
            toneGenerator.release()
            startTimer()
            startRound(0)
        }
    }

    private fun startTimer() {
        val startTime = System.currentTimeMillis()
        elapsedMs = 0L
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (true) {
                elapsedMs = System.currentTimeMillis() - startTime
                delay(100)
            }
        }
    }

    private fun startRound(index: Int) {
        val round = rounds[index]
        val options = (round.distractors + round.correct).shuffled()
        uiState = GameUiState.Playing(
            roundIndex = index,
            totalRounds = rounds.size,
            options = options,
            correctId = round.correct.id,
            result = null,
        )
        audioService.playPrepared(round.correct.id)
    }

    fun onOptionSelected(songId: String) {
        val playing = uiState as? GameUiState.Playing ?: return
        if (playing.result != null) return
        audioService.stop()
        val correct = songId == playing.correctId
        playFeedbackTone(correct)
        uiState = playing.copy(result = RoundResult(songId, correct))
        roundJob?.cancel()
        roundJob = viewModelScope.launch {
            delay(if (correct) 700 else 3000)
            val nextIndex = playing.roundIndex + 1
            if (nextIndex < playing.totalRounds) {
                startRound(nextIndex)
            } else {
                finishGame()
            }
        }
    }

    private fun playFeedbackTone(correct: Boolean) {
        viewModelScope.launch(Dispatchers.Default) {
            val toneGenerator = ToneGenerator(AudioManager.STREAM_MUSIC, 90)
            toneGenerator.startTone(if (correct) ToneGenerator.TONE_PROP_ACK else ToneGenerator.TONE_PROP_NACK, 200)
            delay(250)
            toneGenerator.release()
        }
    }

    private suspend fun finishGame() {
        timerJob?.cancel()
        val totalMs = elapsedMs
        val previousBest = context.dataStore.get(bestScoreKey())
        val isNewBest = previousBest == null || totalMs < previousBest
        if (isNewBest) {
            context.dataStore.edit { it[bestScoreKey()] = totalMs }
        }
        uiState = GameUiState.Finished(totalMs, isNewBest)
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
        roundJob?.cancel()
        audioService.release()
    }
}
