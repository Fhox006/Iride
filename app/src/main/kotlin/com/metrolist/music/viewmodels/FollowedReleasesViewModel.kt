package com.metrolist.music.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.metrolist.innertube.models.SongItem
import com.metrolist.music.discovery.NewRelease
import com.metrolist.music.repository.NewReleasesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class FollowedReleasesViewModel @Inject constructor(
    private val repository: NewReleasesRepository,
) : ViewModel() {
    val tracks = repository.page.map { it.followedSingles }
        .stateIn(viewModelScope, SharingStarted.Eagerly, repository.page.value.followedSingles)
    private val mutableResolvingId = MutableStateFlow<String?>(null)
    val resolvingId = mutableResolvingId.asStateFlow()
    private val mutableResolvedSongIds = MutableStateFlow<Map<String, String>>(emptyMap())
    val resolvedSongIds = mutableResolvedSongIds.asStateFlow()
    private var refreshJob: Job? = null
    val initialLoadComplete = MutableStateFlow(false)

    fun refresh(): Unit {
        if (refreshJob?.isActive == true) return
        refreshJob = viewModelScope.launch(Dispatchers.IO) {
            try {
                repository.onPageOpened(followedOnly = true)
                repository.refresh(followedOnly = true)
            } finally {
                initialLoadComplete.value = true
            }
        }
    }

    suspend fun resolve(release: NewRelease): SongItem? {
        if (mutableResolvingId.value != null) return null
        mutableResolvingId.value = release.id
        return try {
            (repository.resolve(release) as? SongItem)?.also { song ->
                mutableResolvedSongIds.update { it + (release.id to song.id) }
            }
        } finally {
            mutableResolvingId.value = null
        }
    }
}
