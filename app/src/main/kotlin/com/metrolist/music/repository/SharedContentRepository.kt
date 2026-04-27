/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.repository

import com.metrolist.innertube.YouTube
import com.metrolist.innertube.pages.MoodAndGenres
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SharedContentRepository @Inject constructor() {
    private val _moodAndGenres = MutableStateFlow<List<MoodAndGenres>?>(null)
    val moodAndGenres = _moodAndGenres.asStateFlow()

    suspend fun fetchMoodAndGenres() {
        if (_moodAndGenres.value != null) return
        YouTube.moodAndGenres().onSuccess {
            _moodAndGenres.value = it
        }
    }
}
