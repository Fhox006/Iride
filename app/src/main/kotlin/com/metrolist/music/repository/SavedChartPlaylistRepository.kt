package com.metrolist.music.repository

import android.content.Context
import android.util.AtomicFile
import com.metrolist.innertube.models.SongItem
import com.metrolist.music.R
import com.metrolist.music.data.remote.ChartMarket
import com.metrolist.music.data.remote.ChartTrack
import com.metrolist.music.data.remote.DailyChart
import com.metrolist.music.db.MusicDatabase
import com.metrolist.music.db.entities.PlaylistEntity
import com.metrolist.music.db.entities.PlaylistSongMap
import com.metrolist.music.models.toMediaMetadata
import com.metrolist.music.ui.component.chartCountryName
import com.metrolist.music.ui.component.hasChartArtwork
import com.metrolist.music.ui.component.renderChartPoster
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

@Singleton
class SavedChartPlaylistRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: MusicDatabase,
) {
    private val lock = Mutex()
    private val covers = File(context.filesDir, "chart_posters")

    fun playlist(market: ChartMarket) = database.playlist(ChartPlaylistIdentity.id(market))

    suspend fun toggleBookmark(chart: DailyChart, songs: Map<String, SongItem>) = withContext(Dispatchers.IO) {
        lock.withLock {
            val market = ChartMarket(chart.marketCode)
            val id = ChartPlaylistIdentity.id(market)
            val previous = database.playlistBlocking(id)?.playlist
            if (previous?.bookmarkedAt != null) {
                database.withTransaction {
                    playlistBlocking(id)?.playlist?.let { update(it.copy(bookmarkedAt = null)) }
                }
                return@withLock
            }
            if (!hasChartArtwork(market.code) || !shouldUpdateSavedChart(chart.date, null, LocalDate.now(ZoneOffset.UTC))) return@withLock
            val cover = posterPath(market, chart.tracks.size)
            val fingerprint = chartRankingFingerprint(chart.date, chart.tracks.map { it.spotifyId })
            val entity = (previous ?: PlaylistEntity(id = id, name = title(market))).copy(
                name = title(market), browseId = null, isLocal = true, isEditable = false,
                isAutoSync = false, bookmarkedAt = LocalDateTime.now(),
                lastUpdateTime = LocalDate.parse(chart.date).atStartOfDay(),
                remoteSongCount = chart.tracks.size, thumbnailUrl = cover,
                playEndpointParams = fingerprint,
            )
            database.withTransaction {
                val latest = playlistBlocking(id)?.playlist
                if (latest != null && !shouldUpdateSavedChart(chart.date, latest.lastUpdateTime?.toLocalDate(), LocalDate.now(ZoneOffset.UTC))) {
                    update(latest.copy(bookmarkedAt = latest.bookmarkedAt ?: LocalDateTime.now()))
                    return@withTransaction
                }
                val current = entity.copy(createdAt = latest?.createdAt ?: entity.createdAt,
                    bookmarkedAt = latest?.bookmarkedAt ?: entity.bookmarkedAt)
                if (latest == null) insert(current) else update(current)
                mergeResolvedSongs(entity.id, chart, songs,
                    clearPrevious = shouldReplaceSavedChart(latest?.playEndpointParams, fingerprint,
                        latest?.lastUpdateTime?.toLocalDate(), LocalDate.parse(chart.date)))
            }
        }
    }

    /** Called centrally after a source refresh, including chart discovery and Library opening. */
    suspend fun synchronize(chart: DailyChart, songs: Map<String, SongItem>) = withContext(Dispatchers.IO) {
        lock.withLock {
            val market = ChartMarket(chart.marketCode)
            val previous = database.playlistBlocking(ChartPlaylistIdentity.id(market))?.playlist ?: return@withLock
            if (previous.bookmarkedAt == null || !hasChartArtwork(market.code) ||
                !shouldUpdateSavedChart(chart.date, previous.lastUpdateTime?.toLocalDate(), LocalDate.now(ZoneOffset.UTC))) return@withLock
            val cover = posterPath(market, chart.tracks.size)
            val fingerprint = chartRankingFingerprint(chart.date, chart.tracks.map { it.spotifyId })
            database.withTransaction {
                val latest = playlistBlocking(previous.id)?.playlist ?: return@withTransaction
                if (latest.bookmarkedAt == null || !shouldUpdateSavedChart(chart.date,
                    latest.lastUpdateTime?.toLocalDate(), LocalDate.now(ZoneOffset.UTC))) return@withTransaction
                // Resolutions are saved separately; a minute-long foreground cache check needs no writes.
                if (latest.playEndpointParams == fingerprint && latest.remoteSongCount == chart.tracks.size &&
                    latest.thumbnailUrl == cover && latest.name == title(market)) return@withTransaction
                update(latest.copy(name = title(market), remoteSongCount = chart.tracks.size, thumbnailUrl = cover,
                    lastUpdateTime = LocalDate.parse(chart.date).atStartOfDay(), playEndpointParams = fingerprint))
                mergeResolvedSongs(previous.id, chart, songs,
                    clearPrevious = shouldReplaceSavedChart(latest.playEndpointParams, fingerprint,
                        latest.lastUpdateTime?.toLocalDate(), LocalDate.parse(chart.date)))
            }
        }
    }

    /** Fill resolved rows incrementally. A later chart date always clears the previous ranking first. */
    suspend fun resolved(track: ChartTrack, song: SongItem, charts: Collection<DailyChart>) = withContext(Dispatchers.IO) {
        lock.withLock {
            charts.forEach { chart ->
                val currentTrack = chart.tracks.find { it.spotifyId == track.spotifyId } ?: return@forEach
                val id = ChartPlaylistIdentity.id(ChartMarket(chart.marketCode))
                val playlist = database.playlistBlocking(id)?.playlist ?: return@forEach
                if (playlist.bookmarkedAt == null || playlist.lastUpdateTime?.toLocalDate() != LocalDate.parse(chart.date)) return@forEach
                database.withTransaction {
                    val latest = playlistBlocking(id)?.playlist ?: return@withTransaction
                    if (latest.bookmarkedAt == null || latest.lastUpdateTime?.toLocalDate() != LocalDate.parse(chart.date)) return@withTransaction
                    insert(song.toMediaMetadata())
                    val existing = playlistSongMaps(song.id).firstOrNull { it.playlistId == id }
                    if (existing == null) insert(PlaylistSongMap(playlistId = id, songId = song.id, position = currentTrack.rank - 1))
                    else if (existing.position != currentTrack.rank - 1) update(existing.copy(position = currentTrack.rank - 1))
                }
            }
        }
    }

    private fun MusicDatabase.mergeResolvedSongs(id: String, chart: DailyChart, songs: Map<String, SongItem>, clearPrevious: Boolean) {
        if (clearPrevious) clearPlaylist(id)
        val existing = playlistSongMaps(id, 0).associateBy { it.songId }
        chart.tracks.mapNotNull { track -> songs[track.spotifyId]?.let { track to it } }
            .distinctBy { it.second.id }.forEach { (track, song) ->
                insert(song.toMediaMetadata())
                val previous = existing[song.id]
                if (previous == null) insert(PlaylistSongMap(playlistId = id, songId = song.id, position = track.rank - 1))
                else if (previous.position != track.rank - 1) update(previous.copy(position = track.rank - 1))
            }
    }

    private fun title(market: ChartMarket) = context.getString(R.string.new_charts_playlist_title, chartCountryName(market.code))

    private fun posterPath(market: ChartMarket, count: Int): String {
        val file = File(covers, "v1_${market.code}_$count.png")
        if (!file.isFile || file.length() == 0L) {
            covers.mkdirs()
            val bitmap = renderChartPoster(context, market.code, count)
            try {
                val atomic = AtomicFile(file)
                val stream = atomic.startWrite()
                try {
                    check(bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, stream))
                    atomic.finishWrite(stream)
                } catch (error: Exception) {
                    atomic.failWrite(stream)
                    throw error
                }
            } finally { bitmap.recycle() }
        }
        return file.toURI().toString()
    }
}
