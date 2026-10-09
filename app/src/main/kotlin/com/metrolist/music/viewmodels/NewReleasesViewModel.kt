package com.metrolist.music.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.metrolist.innertube.models.YTItem
import com.metrolist.innertube.models.SongItem
import com.metrolist.music.data.remote.ReleaseEditorialArtworkRepository
import com.metrolist.music.discovery.NewRelease
import com.metrolist.music.repository.NewReleasesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NewReleasesViewModel @Inject constructor(
    private val repository: NewReleasesRepository,
    private val editorial: ReleaseEditorialArtworkRepository,
) : ViewModel() {
    val page = repository.page
    val isRefreshing = repository.refreshing
    val isLoading = combine(page, isRefreshing) { page, refreshing ->
        refreshing && !page.hasContent
    }.stateIn(viewModelScope, SharingStarted.Eagerly, true)
    private val mutableResolvingId = MutableStateFlow<String?>(null)
    val resolvingId = mutableResolvingId.asStateFlow()
    private val mutableResolvedSongIds = MutableStateFlow<Map<String, String>>(emptyMap())
    val resolvedSongIds = mutableResolvedSongIds.asStateFlow()
    private var refreshJob: Job? = null

    init {
        refresh()
    }

    fun refresh(force: Boolean = false): Unit {
        if (refreshJob?.isActive == true) return
        refreshJob = viewModelScope.launch(Dispatchers.IO) { repository.refresh(force) }
    }

    fun onPageOpened(): Unit {
        viewModelScope.launch(Dispatchers.IO) {
            repository.onPageOpened()
            refresh()
        }
    }

    suspend fun heroArtwork(release: NewRelease): String? = editorial.findImage(
        release.artistName, release.title, release.artworkUrl, release.artistId, release.appleArtistId,
    )

    suspend fun openRelease(release: NewRelease): YTItem? {
        if (mutableResolvingId.value != null) return null
        mutableResolvingId.value = release.id
        return try {
            repository.resolve(release).also { target ->
                if (target is SongItem) mutableResolvedSongIds.update { it + (release.id to target.id) }
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } finally {
            mutableResolvingId.value = null
        }
    }
}
