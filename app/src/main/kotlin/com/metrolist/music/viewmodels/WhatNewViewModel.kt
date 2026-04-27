/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.metrolist.innertube.YouTube
import com.metrolist.innertube.models.AlbumItem
import com.metrolist.music.db.MusicDatabase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class WhatNewViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: MusicDatabase,
) : ViewModel() {

    private val _recentAlbums = MutableStateFlow<List<AlbumItem>>(emptyList())
    val recentAlbums = _recentAlbums.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _isLoading.value = true
            _recentAlbums.value = fetchRecentAlbums()
            _isLoading.value = false
        }
    }

    private suspend fun fetchRecentAlbums(): List<AlbumItem> {
        val artists = withContext(Dispatchers.IO) {
            database.allArtistsByPlayTime().first()
        }
        val followedArtists = artists.filter { it.artist.bookmarkedAt != null }.take(30)
        val frequentArtists = artists.filter { it.artist.bookmarkedAt == null }.take(20)
        val targetArtists = (followedArtists + frequentArtists).distinctBy { it.id }

        return coroutineScope {
            targetArtists.map { artist ->
                async {
                    YouTube.artist(artist.id).getOrNull()?.let { page ->
                        val albumSection = page.sections.find {
                            it.title.contains("Album", ignoreCase = true) ||
                            it.title.contains("Singl", ignoreCase = true) ||
                            it.title.contains("Latest", ignoreCase = true) ||
                            it.title.contains("Uscita", ignoreCase = true)
                        }
                        albumSection?.items?.filterIsInstance<AlbumItem>()?.firstOrNull()
                    }
                }
            }.awaitAll().filterNotNull().distinctBy { it.id }
        }
    }
}
