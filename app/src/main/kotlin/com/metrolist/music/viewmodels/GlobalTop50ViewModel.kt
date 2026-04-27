/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.viewmodels

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.metrolist.innertube.models.Artist
import com.metrolist.innertube.models.SongItem
import com.metrolist.music.constants.GlobalTop50CacheKey
import com.metrolist.music.constants.GlobalTop50LastUpdateKey
import com.metrolist.music.repository.GlobalTop50Repository
import com.metrolist.music.utils.dataStore
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import timber.log.Timber
import javax.inject.Inject

@Serializable
data class CachedSong(
    val id: String,
    val title: String,
    val artists: List<String>,
    val thumbnail: String,
    val explicit: Boolean = false
) {
    fun toSongItem() = SongItem(
        id = id,
        title = title,
        artists = artists.map { Artist(name = it, id = null) },
        thumbnail = thumbnail,
        explicit = explicit
    )

    companion object {
        fun fromSongItem(item: SongItem) = CachedSong(
            id = item.id,
            title = item.title,
            artists = item.artists.map { it.name },
            thumbnail = item.thumbnail,
            explicit = item.explicit
        )
    }
}

@HiltViewModel
class GlobalTop50ViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: GlobalTop50Repository
) : ViewModel() {

    private val _songs = MutableStateFlow<List<SongItem>>(emptyList())
    val songs: StateFlow<List<SongItem>> = _songs

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    init {
        fetchTop50()
    }

    fun fetchTop50(forceRefresh: Boolean = false) {
        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.value = true
            _error.value = null
            Timber.d("GlobalTop50: fetchTop50 started (forceRefresh=$forceRefresh)")

            val lastUpdate = context.dataStore.data.map { it[GlobalTop50LastUpdateKey] ?: 0L }.first()
            val currentTime = System.currentTimeMillis()
            val cacheExpired = currentTime - lastUpdate > 24 * 60 * 60 * 1000L
            Timber.d("GlobalTop50: Last update: $lastUpdate, Expired: $cacheExpired")

            if (!forceRefresh && !cacheExpired) {
                val cachedJson = context.dataStore.data.map { it[GlobalTop50CacheKey] }.first()
                if (cachedJson != null) {
                    try {
                        val cachedSongs = Json.decodeFromString<List<CachedSong>>(cachedJson)
                        Timber.d("GlobalTop50: Loaded ${cachedSongs.size} songs from cache")
                        _songs.value = cachedSongs.map { it.toSongItem() }
                        _isLoading.value = false
                        return@launch
                    } catch (e: Exception) {
                        Timber.e(e, "GlobalTop50: Cache decoding failed")
                    }
                }
            }

            try {
                Timber.d("GlobalTop50: Fetching fresh data from repository...")
                val freshSongs = repository.fetchAndSearchGlobalTop50()
                Timber.d("GlobalTop50: Repository returned ${freshSongs.size} songs")
                if (freshSongs.isNotEmpty()) {
                    _songs.value = freshSongs
                    
                    val cachedSongs = freshSongs.map { CachedSong.fromSongItem(it) }
                    context.dataStore.edit { prefs ->
                        prefs[GlobalTop50CacheKey] = Json.encodeToString(cachedSongs)
                        prefs[GlobalTop50LastUpdateKey] = currentTime
                    }
                    Timber.d("GlobalTop50: Cache updated")
                } else if (_songs.value.isEmpty()) {
                    _error.value = "Failed to fetch top 50 global"
                }
            } catch (e: Exception) {
                Timber.e(e, "GlobalTop50: Fetch failed")
                _error.value = e.message ?: "Unknown error"
            } finally {
                _isLoading.value = false
            }
        }
    }
}
