package com.metrolist.music.viewmodels

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.metrolist.music.data.remote.ChartMarket
import com.metrolist.music.data.remote.ChartTrack
import com.metrolist.music.playback.queues.DailyChartQueue
import com.metrolist.music.playback.queues.Queue
import com.metrolist.music.repository.DailyChartRepository
import com.metrolist.music.repository.ChartDirectoryRepository
import com.metrolist.music.repository.SavedChartPlaylistRepository
import com.metrolist.music.data.remote.ChartMarkets
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DailyChartsHomeViewModel @Inject constructor(
    private val repository: DailyChartRepository,
    private val directory: ChartDirectoryRepository,
) : ViewModel() {
    val charts = repository.charts
    val markets = directory.state.map { if (it.hasSourceCatalog) it.markets else ChartMarkets.bootstrap }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ChartMarkets.bootstrap)
    private var refreshJob: Job? = null

    suspend fun refreshMarket(market: ChartMarket) = repository.refresh(market)

    suspend fun refreshCatalog() = directory.refresh()

    fun refresh() {
        if (refreshJob?.isActive == true) return
        refreshJob = viewModelScope.launch {
            coroutineScope { ChartMarket.home.forEach { market -> launch { repository.refresh(market) } } }
        }
    }
}

enum class ChartPlaybackError { UNAVAILABLE, NETWORK }

@HiltViewModel
class DailyChartViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: DailyChartRepository,
    private val savedPlaylists: SavedChartPlaylistRepository,
) : ViewModel() {
    val market = ChartMarket(checkNotNull(savedStateHandle.get<String>("market")))
    val charts = repository.charts
    val tracks = repository.tracks
    val savedPlaylist = savedPlaylists.playlist(market)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    private val mutableSaving = MutableStateFlow(false)
    val saving = mutableSaving.asStateFlow()
    private var refreshJob: Job? = null
    private var playJob: Job? = null
    private var playbackRequest = 0L
    private val mutableStartingRank = MutableStateFlow<Int?>(null)
    val startingRank = mutableStartingRank.asStateFlow()
    private val mutablePlaybackError = MutableStateFlow<ChartPlaybackError?>(null)
    val playbackError = mutablePlaybackError.asStateFlow()

    fun refresh(force: Boolean = false) {
        if (refreshJob?.isActive == true) return
        refreshJob = viewModelScope.launch { repository.refresh(market, force) }
    }

    fun toggleBookmark() {
        if (mutableSaving.value) return
        val chart = repository.charts.value[market.code]?.chart ?: return
        mutableSaving.value = true
        viewModelScope.launch {
            try {
                savedPlaylists.toggleBookmark(chart,
                    repository.tracks.value.mapNotNull { (id, state) -> state.song?.let { id to it } }.toMap())
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                timber.log.Timber.w(error, "Could not bookmark chart")
            } finally { mutableSaving.value = false }
        }
    }

    suspend fun resolveDownloadSongs(): List<com.metrolist.innertube.models.SongItem> {
        val chart = repository.charts.value[market.code]?.chart ?: return emptyList()
        return chart.tracks.mapNotNull { track ->
            try { repository.resolve(track, retry = true) }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { null }
        }.distinctBy { it.id }
    }

    suspend fun loadTrack(track: ChartTrack) {
        try {
            repository.resolve(track)
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            // The repository publishes a retryable row state; scrolling must not cancel siblings.
        }
    }

    fun play(rank: Int?, title: String, hideExplicit: Boolean, shuffle: Boolean = false, onReady: (Queue) -> Unit) {
        val chart = repository.charts.value[market.code]?.chart ?: return
        val request = ++playbackRequest
        playJob?.cancel()
        playJob = viewModelScope.launch {
            mutablePlaybackError.value = null
            mutableStartingRank.value = rank ?: 1
            try {
                val orderedTracks = if (shuffle && rank == null) chart.tracks.shuffled() else chart.tracks
                val candidates = if (rank == null) orderedTracks else orderedTracks.filter { it.rank == rank }
                for (track in candidates) {
                    val song = repository.resolve(track, retry = true)
                        ?.takeUnless { hideExplicit && it.explicit } ?: continue
                    onReady(DailyChartQueue(
                        title = title,
                        tracks = orderedTracks.dropWhile { it.spotifyId != track.spotifyId }.drop(1),
                        firstSong = song,
                        hideExplicit = hideExplicit,
                        resolve = { repository.resolve(it, retry = true) },
                    ))
                    return@launch
                }
                mutablePlaybackError.value = ChartPlaybackError.UNAVAILABLE
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                mutablePlaybackError.value = ChartPlaybackError.NETWORK
            } finally {
                if (request == playbackRequest) mutableStartingRank.value = null
            }
        }
    }
}
