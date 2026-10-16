package com.metrolist.music.viewmodels

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.metrolist.music.constants.HideExplicitKey
import com.metrolist.music.constants.HideVideoSongsKey
import com.metrolist.music.db.MusicDatabase
import com.metrolist.music.models.ReverieRecap
import com.metrolist.music.models.ReverieChapter
import com.metrolist.music.models.ReveriePalette
import com.metrolist.music.models.createReverieRecap
import com.metrolist.music.models.listeningBounds
import com.metrolist.music.models.rankReverieGenres
import com.metrolist.music.playback.ReverieAudioFactory
import com.metrolist.music.repository.MonthlyReverieRepository
import com.metrolist.music.utils.dataStore
import com.metrolist.music.utils.GenreProvider
import com.metrolist.music.utils.loadReverieImages
import com.metrolist.music.utils.loadReverieBackgrounds
import com.metrolist.music.utils.renderReverieImage
import com.metrolist.music.utils.writeReverieImage
import com.metrolist.music.utils.reportException
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.YearMonth
import javax.inject.Inject

@HiltViewModel
class MonthlyReverieViewModel @Inject constructor(
    private val repository: MonthlyReverieRepository,
    private val database: MusicDatabase,
    @ApplicationContext private val context: Context,
    val audioFactory: ReverieAudioFactory,
) : ViewModel() {
    private val _film = MutableStateFlow<ReverieFilmState>(ReverieFilmState.Loading)
    val film = _film.asStateFlow()
    private var loading: Job? = null
    private var loadedMonth: YearMonth? = null
    private var exportedImage: Uri? = null

    fun load(month: YearMonth, retry: Boolean = false) {
        if (!retry && loadedMonth == month) return
        loading?.cancel()
        loadedMonth = month
        exportedImage = null
        _film.value = ReverieFilmState.Loading
        loading = viewModelScope.launch {
            try {
                if (month !in repository.availableMonths.first()) {
                    _film.value = ReverieFilmState.Empty
                    return@launch
                }
                val recap = withContext(Dispatchers.IO) {
                    val bounds = month.listeningBounds()
                    val preferences = context.dataStore.data.first()
                    val hideVideo = preferences[HideVideoSongsKey] ?: false
                    val hideExplicit = preferences[HideExplicitKey] ?: false
                    createReverieRecap(
                        month, database.reverieEvents(bounds.first, bounds.last).first(),
                        categories = database.reverieCategories(bounds.first, bounds.last, hideVideo, hideExplicit).first(),
                        hideVideoSongs = hideVideo, hideExplicit = hideExplicit,
                    )
                }
                if (!recap.eligible) {
                    _film.value = ReverieFilmState.Empty
                } else {
                    val enriched = withContext(Dispatchers.IO) {
                        val assignments = ConcurrentHashMap<String, List<String>>()
                        recap.songs.forEach { song -> GenreProvider.cachedGenres(song.id)?.let { assignments[song.id] = it } }
                        // Cached assignments cover the whole month; bounded discovery fills the most relevant gaps.
                        withTimeoutOrNull(8_000) {
                            val requests = Semaphore(4)
                            coroutineScope {
                                recap.songs.filter { !assignments.containsKey(it.id) }.take(60).forEach { song ->
                                    launch { requests.withPermit {
                                        assignments[song.id] = GenreProvider.getGenres(song.id, song.title, song.subtitle)
                                    } }
                                }
                            }
                        }
                        recap.copy(genres = rankReverieGenres(recap.songs, assignments))
                    }
                    val images = loadReverieImages(context, enriched)
                    val backgrounds = loadReverieBackgrounds(enriched, images)
                    _film.value = ReverieFilmState.Ready(enriched, images, renderReverieImage(context, enriched, images, backgrounds), backgrounds)
                }
            } catch (error: CancellationException) { throw error
            } catch (error: Exception) {
                reportException(error)
                _film.value = ReverieFilmState.Error
            }
        }
    }

    suspend fun exportImage(): Uri {
        exportedImage?.let { return it }
        val ready = _film.value as? ReverieFilmState.Ready ?: error("Reverie is not ready")
        return writeReverieImage(context, ready.recap.month.toString(), ready.poster).also { exportedImage = it }
    }

    val homeMonth = repository.homeMonth.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5_000), null,
    )

    val archiveMonths = repository.completedMonths.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList(),
    )

    val firstFullMonth = repository.firstFullMonth.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5_000), null,
    )

    fun markHomeSeen(month: YearMonth) {
        viewModelScope.launch {
            try {
                repository.markHomeSeen(month)
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                reportException(error)
            }
        }
    }
}

sealed interface ReverieFilmState {
    data object Loading : ReverieFilmState
    data object Empty : ReverieFilmState
    data object Error : ReverieFilmState
    data class Ready(
        val recap: ReverieRecap,
        val images: Map<String, Bitmap>,
        val poster: Bitmap,
        val backgrounds: Map<ReverieChapter, ReveriePalette>,
    ) : ReverieFilmState
}
