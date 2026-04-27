/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.metrolist.music.repository.SharedContentRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SharedContentViewModel
@Inject
constructor(
    private val repository: SharedContentRepository
) : ViewModel() {
    val moodAndGenres = repository.moodAndGenres

    init {
        viewModelScope.launch {
            repository.fetchMoodAndGenres()
        }
    }
}
