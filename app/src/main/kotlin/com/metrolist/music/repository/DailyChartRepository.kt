package com.metrolist.music.repository

import android.content.Context
import android.util.AtomicFile
import com.metrolist.innertube.YouTube
import com.metrolist.innertube.models.SongItem
import com.metrolist.music.data.remote.ChartMarket
import com.metrolist.music.data.remote.ChartTrack
import com.metrolist.music.data.remote.ChartTrackMatcher
import com.metrolist.music.data.remote.DailyChart
import com.metrolist.music.data.remote.DailyChartParser
import com.metrolist.music.data.remote.isCurrentFor
import com.metrolist.music.data.remote.isUsableFor
import com.metrolist.music.data.remote.withRankMovements
import dagger.hilt.android.qualifiers.ApplicationContext
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import timber.log.Timber
import java.io.File
import java.time.Instant
import java.time.ZoneOffset
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

data class ChartState(
    val chart: DailyChart? = null,
    val refreshing: Boolean = false,
    val refreshFailed: Boolean = false,
    val sourceMissing: Boolean = false,
)

enum class ChartTrackStatus { LOADING, READY, UNAVAILABLE, FAILED }

data class ChartTrackState(
    val status: ChartTrackStatus,
    val song: SongItem? = null,
    val checkedAt: Long = System.currentTimeMillis(),
)

@Serializable
private data class CachedChartSong(val song: SongItem, val savedAt: Long)

@Singleton
class DailyChartRepository @Inject constructor(
    @ApplicationContext context: Context,
    private val savedPlaylists: SavedChartPlaylistRepository,
) {
    private val directory = File(context.filesDir, "daily_charts")
    private val songDirectory = File(context.cacheDir, "daily_chart_tracks")
    private val json = Json { ignoreUnknownKeys = true }
    private val client = HttpClient(OkHttp) {
        install(HttpTimeout) {
            requestTimeoutMillis = 12_000
            connectTimeoutMillis = 5_000
            socketTimeoutMillis = 12_000
        }
    }
    private val chartLocks = ConcurrentHashMap<String, Mutex>()
    private val trackLocks = Array(64) { Mutex() }
    private fun trackLock(id: String): Mutex = trackLocks[(id.hashCode() and Int.MAX_VALUE) % trackLocks.size]
    private val requests = Semaphore(3)
    private val chartRequests = Semaphore(3)
    private val mutableCharts = MutableStateFlow<Map<String, ChartState>>(emptyMap())
    val charts: StateFlow<Map<String, ChartState>> = mutableCharts.asStateFlow()
    private val mutableTracks = MutableStateFlow<Map<String, ChartTrackState>>(emptyMap())
    val tracks: StateFlow<Map<String, ChartTrackState>> = mutableTracks.asStateFlow()
    private val nextChartAttempt = ConcurrentHashMap<String, Long>()
    private val cacheLoadLock = Mutex()
    private var loadedCache = false

    suspend fun loadCachedCharts(): Unit = withContext(Dispatchers.IO) {
        cacheLoadLock.withLock {
            if (loadedCache) return@withLock
            directory.listFiles().orEmpty().filter { it.name.matches(Regex("(?:global|[a-z]{2})\\.json")) }.forEach { file ->
                val market = ChartMarket(file.nameWithoutExtension)
                chartLocks.computeIfAbsent(market.code) { Mutex() }.withLock {
                    val cached = read<DailyChart>(file)?.takeIf { it.isUsableFor(market) }
                    if (cached != null) {
                        mutableCharts.update { states ->
                            if (states[market.code]?.chart != null) states else states + (market.code to ChartState(cached))
                        }
                    }
                }
            }
            loadedCache = true
        }
    }

    suspend fun refresh(market: ChartMarket, force: Boolean = false) = withContext(Dispatchers.IO) {
        loadCachedCharts()
        chartLocks.computeIfAbsent(market.code) { Mutex() }.withLock {
            val file = File(directory, "${market.code}.json")
            val oldState = mutableCharts.value[market.code]
            val cached = (oldState?.chart ?: read<DailyChart>(file))?.takeIf { it.isUsableFor(market) }
            val now = System.currentTimeMillis()
            val nextAttempt = nextChartAttempt[market.code] ?: 0L
            if (!force && cached != null && !needsRefresh(cached.checkedAt, now)) {
                mutableCharts.update { it + (market.code to ChartState(cached)) }
                synchronizeSaved(cached)
                return@withLock
            }
            if (!force && now < nextAttempt) return@withLock
            mutableCharts.update { it + (market.code to ChartState(cached, refreshing = true)) }
            try {
                val response = chartRequests.withPermit { client.get(market.url) }
                if (response.status.value == 404) {
                    nextChartAttempt[market.code] = now + 86_400_000
                    mutableCharts.update { it + (market.code to ChartState(cached, refreshFailed = true, sourceMissing = true)) }
                    return@withLock
                }
                check(response.status.value == 200) { "Chart HTTP ${response.status.value}" }
                val fresh = DailyChartParser.parse(response.bodyAsText(), market, now).withRankMovements(cached)
                require(fresh.isCurrentFor(market)) { "Chart has no current valid output" }
                require(cached == null || fresh.date >= cached.date) { "Chart date moved backwards" }
                try {
                    write(file, fresh)
                } catch (error: Exception) {
                    Timber.w(error, "Could not cache daily chart")
                }
                mutableCharts.update { it + (market.code to ChartState(fresh)) }
                synchronizeSaved(fresh)
                nextChartAttempt.remove(market.code)
            } catch (error: CancellationException) {
                mutableCharts.update { it + (market.code to ChartState(cached)) }
                throw error
            } catch (error: Exception) {
                Timber.w(error, "Daily chart refresh failed: %s", market.code)
                nextChartAttempt[market.code] = now + 5 * 60_000
                mutableCharts.update { it + (market.code to ChartState(cached, refreshFailed = true)) }
            }
        }
    }

    suspend fun resolve(track: ChartTrack, retry: Boolean = false): SongItem? = withContext(Dispatchers.IO) {
        require(track.spotifyId.matches(Regex("[A-Za-z0-9]{22}")))
        trackLock(track.spotifyId).withLock {
            val now = System.currentTimeMillis()
            val previous = mutableTracks.value[track.spotifyId]
            if (previous?.song != null && now - previous.checkedAt in 0..30L * 86_400_000) return@withLock previous.song
            if (!retry && previous != null && previous.status != ChartTrackStatus.LOADING &&
                now - previous.checkedAt < 60_000) {
                if (previous.status == ChartTrackStatus.FAILED) error("Track search temporarily unavailable")
                return@withLock null
            }
            val file = File(songDirectory, "${track.spotifyId}.json")
            val saved = read<CachedChartSong>(file)?.takeIf {
                now - it.savedAt in 0..30L * 86_400_000 &&
                    ChartTrackMatcher.bestMatch(track, listOf(it.song)) != null
            }
            if (saved != null) {
                publish(track, ChartTrackState(ChartTrackStatus.READY, saved.song))
                saveResolved(track, saved.song)
                return@withLock saved.song
            }
            publish(track, ChartTrackState(ChartTrackStatus.LOADING))
            try {
                val song = requests.withPermit {
                    var matched: SongItem? = null
                    val queries = listOf("${track.title} ${track.artists.first()}", "${track.artists.first()} ${track.title}")
                    for (query in queries) {
                        val candidates = withTimeout(15_000) {
                            YouTube.search(query, YouTube.SearchFilter.FILTER_SONG)
                                .getOrThrow().items.filterIsInstance<SongItem>()
                        }
                        // A small shared delay avoids a burst of searches when scrolling quickly.
                        delay(300)
                        matched = ChartTrackMatcher.bestMatch(track, candidates)
                        if (matched != null) break
                    }
                    matched
                }
                if (song != null) {
                    try {
                        write(file, CachedChartSong(song, now))
                    } catch (error: Exception) {
                        Timber.w(error, "Could not cache chart track")
                    }
                }
                publish(track, ChartTrackState(if (song == null) ChartTrackStatus.UNAVAILABLE else ChartTrackStatus.READY, song))
                if (song != null) saveResolved(track, song)
                song
            } catch (error: TimeoutCancellationException) {
                publish(track, ChartTrackState(ChartTrackStatus.FAILED))
                throw IllegalStateException("Chart track search timed out", error)
            } catch (error: CancellationException) {
                mutableTracks.update { it - track.spotifyId }
                throw error
            } catch (error: Exception) {
                publish(track, ChartTrackState(ChartTrackStatus.FAILED))
                throw error
            }
        }
    }

    private fun publish(track: ChartTrack, state: ChartTrackState) {
        mutableTracks.update {
            val updated = it + (track.spotifyId to state)
            if (updated.size <= 1_000) updated
            else updated.entries.sortedByDescending { entry -> entry.value.checkedAt }
                .take(1_000).associate { entry -> entry.toPair() }
        }
    }

    private suspend fun synchronizeSaved(chart: DailyChart) {
        try {
            savedPlaylists.synchronize(chart, mutableTracks.value.mapNotNull { (id, state) -> state.song?.let { id to it } }.toMap())
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            Timber.w(error, "Could not update saved chart playlist")
        }
    }

    private suspend fun saveResolved(track: ChartTrack, song: SongItem) {
        try {
            savedPlaylists.resolved(track, song, mutableCharts.value.values.mapNotNull { it.chart })
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            Timber.w(error, "Could not save resolved chart song")
        }
    }

    private inline fun <reified T> read(file: File): T? = try {
        AtomicFile(file).openRead().bufferedReader().use { json.decodeFromString<T>(it.readText()) }
    } catch (_: Exception) { null }

    private inline fun <reified T> write(file: File, value: T) {
        file.parentFile?.mkdirs()
        val atomic = AtomicFile(file)
        val stream = atomic.startWrite()
        try {
            stream.write(json.encodeToString(value).toByteArray(Charsets.UTF_8))
            atomic.finishWrite(stream)
        } catch (error: Exception) {
            atomic.failWrite(stream)
            throw error
        }
    }

    companion object {
        fun needsRefresh(checkedAt: Long, now: Long): Boolean {
            fun day(time: Long) = Instant.ofEpochMilli(time).atOffset(ZoneOffset.UTC).toLocalDate()
            return now < checkedAt || day(now) != day(checkedAt) || now - checkedAt >= 4 * 60 * 60 * 1000L
        }
    }
}
