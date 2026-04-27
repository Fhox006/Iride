/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.viewmodels

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.metrolist.innertube.YouTube
import com.metrolist.innertube.models.SongItem
import com.metrolist.innertube.models.filterExplicit
import com.metrolist.innertube.models.filterVideoSongs
import com.metrolist.innertube.models.filterYoutubeShorts
import com.metrolist.innertube.pages.BrowseResult
import com.metrolist.music.constants.HideExplicitKey
import com.metrolist.music.constants.HideVideoOnlyResultsKey
import com.metrolist.music.constants.HideVideoSongsKey
import com.metrolist.music.constants.HideYoutubeShortsKey
import com.metrolist.music.constants.MaxResolvedTrackCacheSizeKey
import com.metrolist.music.constants.ResolveVideoSongsKey
import com.metrolist.music.db.MusicDatabase
import com.metrolist.music.utils.dataStore
import com.metrolist.music.utils.get
import com.metrolist.music.utils.reportException
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class YouTubeBrowseViewModel
@Inject
constructor(
    @ApplicationContext val context: Context,
    savedStateHandle: SavedStateHandle,
    private val database: MusicDatabase
) : ViewModel() {
    private val browseId = savedStateHandle.get<String>("browseId")!!
    private val params = savedStateHandle.get<String>("params")

    val result = MutableStateFlow<BrowseResult?>(null)
    
    private var resolveVideoJob: Job? = null

    init {
        viewModelScope.launch {
            val hideExplicit = context.dataStore.get(HideExplicitKey, false)
            val hideVideoSongs = context.dataStore.get(HideVideoSongsKey, false)
            val hideYoutubeShorts = context.dataStore.get(HideYoutubeShortsKey, false)
            YouTube
                .browse(browseId, params)
                .onSuccess {
                    val filtered = it
                        .filterExplicit(hideExplicit)
                        .filterVideoSongs(hideVideoSongs)
                        .filterYoutubeShorts(hideYoutubeShorts)
                    result.value = filtered
                    resolveVideoSongsAsync(filtered)
                }.onFailure {
                    reportException(it)
                }
        }
    }

    private fun resolveVideoSongsAsync(browseResult: BrowseResult) {
        resolveVideoJob?.cancel()
        resolveVideoJob = viewModelScope.launch(Dispatchers.IO) {
            val resolveEnabled = context.dataStore.get(ResolveVideoSongsKey, true)
            if (!resolveEnabled) return@launch

            val hideVideoOnlyResults = context.dataStore.get(HideVideoOnlyResultsKey, false)
            val maxCacheSize = context.dataStore.get(MaxResolvedTrackCacheSizeKey, 1000)

            // Moods and Genres are typically accessed via BrowseResult
            // User requested to enable this for "Your Mood" section specifically.
            // Since Mood/Genres open this ViewModel, it applies.

            browseResult.items.filterIsInstance<SongItem>().forEach { song ->
                if (!isActive) return@launch
                if (!song.isVideoSong) return@forEach

                // Check cache
                val cached = database.getSetVideoId(song.id)
                val resolved = if (cached != null) {
                    cached.setVideoId?.let { id ->
                        song.copy(id = id, musicVideoType = null)
                    }
                } else {
                    val track = findAudioTrack(song)
                    database.insert(com.metrolist.music.db.entities.SetVideoIdEntity(song.id, track?.id))
                    if (maxCacheSize != -1) {
                        database.trimSetVideoIdCache(maxCacheSize)
                    }
                    track
                }

                if (resolved != null) {
                    updateSongInResult(song.id, resolved)
                } else if (hideVideoOnlyResults) {
                    removeSongFromResult(song.id)
                }
                
                if (cached == null) {
                    delay(400)
                }
            }
        }
    }

    private fun updateSongInResult(oldId: String, newSong: SongItem) {
        val current = result.value ?: return
        val newItems = current.items.map { item ->
            if (item is SongItem && item.id == oldId) newSong else item
        }
        result.value = current.copy(items = newItems as List<BrowseResult.Item>)
    }

    private fun removeSongFromResult(id: String) {
        val current = result.value ?: return
        val newItems = current.items.filterNot { item -> item is SongItem && item.id == id }
        result.value = current.copy(items = newItems as List<BrowseResult.Item>)
    }

    private suspend fun findAudioTrack(song: SongItem): SongItem? {
        val cleanTitle = song.title
            .replace(Regex("(?i)\\s*[\\[\\(](Official Video|Official Music Video|MV|Official Audio|Lyric Video|Audio)[\\]\\)]"), "")
            .replace(Regex("(?i)\\s*-\\s*(Visual Video|Official Video|Video|Music Video|Audio)"), "")
            .trim()
        val artistName = song.artists.firstOrNull()?.name ?: ""

        return YouTube.search("$cleanTitle $artistName", YouTube.SearchFilter.FILTER_SONG)
            .getOrNull()?.items?.filterIsInstance<SongItem>()?.firstOrNull { candidate ->
                if (candidate.isVideoSong) return@firstOrNull false

                val candidateCleanTitle = candidate.title
                    .replace(Regex("(?i)\\s*[\\[\\(](Official Video|Official Music Video|MV|Official Audio|Lyric Video|Audio)[\\]\\)]"), "")
                    .replace(Regex("(?i)\\s*-\\s*(Visual Video|Official Video|Video|Music Video|Audio)"), "")
                    .trim()

                val titleMatches = candidateCleanTitle.contains(cleanTitle, ignoreCase = true) ||
                        cleanTitle.contains(candidateCleanTitle, ignoreCase = true)
                if (!titleMatches) return@firstOrNull false

                val artistMatches = candidate.artists.any { it.name.equals(artistName, ignoreCase = true) }
                if (!artistMatches) return@firstOrNull false

                val songDuration = song.duration
                val candidateDuration = candidate.duration
                if (songDuration != null && candidateDuration != null) {
                    if (kotlin.math.abs(songDuration - candidateDuration) > 10) return@firstOrNull false
                }

                val blacklist = listOf("live", "instrumental", "acoustic", "cover", "karaoke", "remix", "performance", "session")
                if (blacklist.any { candidate.title.contains(it, ignoreCase = true) }) return@firstOrNull false

                true
            }
    }

    override fun onCleared() {
        resolveVideoJob?.cancel()
    }
}
