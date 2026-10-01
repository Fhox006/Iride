package com.metrolist.music.viewmodels

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.metrolist.innertube.YouTube
import com.metrolist.innertube.models.SongItem
import com.metrolist.music.db.MusicDatabase
import com.metrolist.music.db.entities.PlaylistEntity
import com.metrolist.music.db.entities.PlaylistSongMap
import com.metrolist.music.models.toMediaMetadata
import com.metrolist.music.utils.spotify.SpotifyExportParser
import com.metrolist.music.utils.spotify.SpotifyExportPlaylist
import com.metrolist.music.utils.spotify.SpotifyExportTooLargeException
import com.metrolist.music.utils.spotify.SpotifyExportTrack
import com.metrolist.music.utils.spotify.SpotifyTrackMatcher
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream
import java.io.File
import java.io.InputStream
import java.time.LocalDateTime
import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject

enum class SpotifyImportPhase { PENDING, RESOLVING, READY, FAILED, SAVED }

enum class SpotifyImportError { INVALID_FILE, FILE_TOO_LARGE, READ_FAILED, SAVE_FAILED }

data class SpotifyImportPlaylist(
    val id: String,
    val name: String,
    val total: Int,
    val processed: Int = 0,
    val matched: Int = 0,
    val unmatched: Int = 0,
    val failed: Int = 0,
    val thumbnail: String? = null,
    val phase: SpotifyImportPhase = SpotifyImportPhase.PENDING,
    val savedPlaylistId: String? = null,
)

data class SpotifyImportUiState(
    val playlists: List<SpotifyImportPlaylist> = emptyList(),
    val reading: Boolean = false,
    val saving: Boolean = false,
    val error: SpotifyImportError? = null,
    val savedCount: Int = 0,
) {
    val canSave: Boolean
        get() = !reading && !saving && playlists.none {
            it.phase == SpotifyImportPhase.PENDING || it.phase == SpotifyImportPhase.RESOLVING
        } && playlists.any { it.phase == SpotifyImportPhase.READY }
}

@HiltViewModel
class SpotifyImportViewModel @Inject constructor(
    private val database: MusicDatabase,
    @ApplicationContext private val context: Context,
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(SpotifyImportUiState())
    val uiState = mutableUiState.asStateFlow()

    private data class StagedPlaylist(
        val export: SpotifyExportPlaylist,
        val outcomes: MutableList<TrackOutcome?>,
    )

    private sealed interface TrackOutcome {
        data class Matched(val song: SongItem) : TrackOutcome
        data object Unmatched : TrackOutcome
        data object Failed : TrackOutcome
    }

    // Staging mutates only on the main dispatcher. Searches alone run on IO.
    private val staged = mutableMapOf<String, StagedPlaylist>()
    private val playlistJobs = mutableMapOf<String, Job>()
    private val searchCache = mutableMapOf<String, Deferred<TrackOutcome>>()
    private val cacheMutex = Mutex()
    private val searchPermits = Semaphore(3)
    private val consecutiveSearchFailures = AtomicInteger()
    private var activeReads = 0

    fun importUri(uri: Uri) {
        readExport {
            val name = context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
                ?.use { cursor ->
                    if (cursor.moveToFirst()) cursor.getString(0) else null
                } ?: uri.lastPathSegment.orEmpty()
            val input = context.contentResolver.openInputStream(uri) ?: throw IllegalStateException()
            input to name
        }
    }

    fun importFile(file: File, name: String = file.name) {
        readExport(open = {
            if (file.length() > SpotifyExportParser.MAX_BYTES) throw SpotifyExportTooLargeException()
            file.inputStream() to name
        }, cleanup = { cleanupOwnedExport(file) })
    }

    fun importBytes(bytes: ByteArray, fileName: String) {
        readExport {
            if (bytes.size > SpotifyExportParser.MAX_BYTES) throw SpotifyExportTooLargeException()
            ByteArrayInputStream(bytes) to fileName
        }
    }

    private fun readExport(cleanup: (() -> Unit)? = null, open: () -> Pair<InputStream, String>) {
        activeReads++
        mutableUiState.update { it.copy(reading = true, error = null) }
        viewModelScope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    val (input, name) = open()
                    input.use { SpotifyExportParser.parse(it, name) }
                }
                // An export completed during saving waits for that transaction before staging.
                uiState.first { !it.saving }
                val added = result.playlists.filter { export ->
                    staged.values.none { it.export.fingerprint == export.fingerprint }
                }.distinctBy { it.fingerprint }.map { export ->
                    val id = UUID.randomUUID().toString()
                    staged[id] = StagedPlaylist(export, MutableList(export.tracks.size) { null })
                    SpotifyImportPlaylist(
                        id = id,
                        name = export.name,
                        total = export.tracks.size,
                        phase = if (export.invalid) SpotifyImportPhase.FAILED else SpotifyImportPhase.PENDING,
                    )
                }
                // Publish the entire archive before starting any searches.
                mutableUiState.update {
                    it.copy(playlists = it.playlists + added)
                }
                added.filter { it.phase != SpotifyImportPhase.FAILED }.forEach { resolve(it.id) }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                mutableUiState.update {
                    it.copy(error = when (error) {
                        is SpotifyExportTooLargeException -> SpotifyImportError.FILE_TOO_LARGE
                        is IllegalArgumentException -> SpotifyImportError.INVALID_FILE
                        else -> SpotifyImportError.READ_FAILED
                    })
                }
            } finally {
                if (cleanup != null) withContext(NonCancellable + Dispatchers.IO) { cleanup() }
                activeReads--
                mutableUiState.update { it.copy(reading = activeReads > 0) }
            }
        }
    }

    private fun cleanupOwnedExport(file: File) {
        runCatching {
            val root = File(context.cacheDir, "exportify").canonicalFile
            val target = file.canonicalFile
            if (target.toPath().startsWith(root.toPath()) && target != root) {
                target.delete()
                target.parentFile?.takeUnless { it == root }?.delete()
            }
        }
    }

    private fun resolve(id: String) {
        val playlist = staged[id] ?: return
        playlistJobs[id]?.cancel()
        playlistJobs[id] = viewModelScope.launch {
            updateCard(id) { it.copy(phase = SpotifyImportPhase.RESOLVING) }
            playlist.export.tracks.forEachIndexed { index, track ->
                if (playlist.outcomes[index] == null) {
                    val outcome = resolveTrack(track)
                    playlist.outcomes[index] = outcome
                    updateCard(id) {
                        it.copy(
                            processed = it.processed + 1,
                            matched = it.matched + if (outcome is TrackOutcome.Matched) 1 else 0,
                            unmatched = it.unmatched + if (outcome == TrackOutcome.Unmatched) 1 else 0,
                            failed = it.failed + if (outcome == TrackOutcome.Failed) 1 else 0,
                            thumbnail = it.thumbnail ?: (outcome as? TrackOutcome.Matched)?.song?.thumbnail,
                        )
                    }
                }
            }
            updateCard(id) { it.copy(phase = SpotifyImportPhase.READY) }
        }
    }

    private fun publishProgress(id: String, playlist: StagedPlaylist) {
        val outcomes = playlist.outcomes
        updateCard(id) {
            it.copy(
                processed = outcomes.count { outcome -> outcome != null },
                matched = outcomes.count { outcome -> outcome is TrackOutcome.Matched },
                unmatched = outcomes.count { outcome -> outcome == TrackOutcome.Unmatched },
                failed = outcomes.count { outcome -> outcome == TrackOutcome.Failed },
                thumbnail = outcomes.firstNotNullOfOrNull { outcome -> (outcome as? TrackOutcome.Matched)?.song?.thumbnail },
            )
        }
    }

    private suspend fun resolveTrack(track: SpotifyExportTrack): TrackOutcome {
        if (track.title.isBlank() || track.artists.isEmpty()) return TrackOutcome.Unmatched
        val key = trackKey(track)
        val deferred = cacheMutex.withLock {
            searchCache.getOrPut(key) {
                viewModelScope.async(Dispatchers.IO) {
                    searchPermits.withPermit { search(track) }
                }
            }
        }
        return deferred.await()
    }

    private suspend fun search(track: SpotifyExportTrack): TrackOutcome {
        // Stop thousands of doomed requests when connectivity or the search service fails.
        if (consecutiveSearchFailures.get() >= 6) return TrackOutcome.Failed
        repeat(2) { attempt ->
            try {
                val candidates = YouTube.search(
                    "${track.title} ${track.artists.first()}",
                    YouTube.SearchFilter.FILTER_SONG,
                ).getOrThrow().items.filterIsInstance<SongItem>()
                val song = candidates.mapNotNull { candidate ->
                    if (candidate.isEpisode) null else SpotifyTrackMatcher.score(
                        track, candidate.title, candidate.artists.map { it.name }, candidate.duration,
                    )?.let { candidate to it }
                }.maxByOrNull { it.second }?.first
                consecutiveSearchFailures.set(0)
                delay(180)
                return song?.let { TrackOutcome.Matched(it) } ?: TrackOutcome.Unmatched
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                if (attempt == 0) delay(900)
            }
        }
        consecutiveSearchFailures.incrementAndGet()
        return TrackOutcome.Failed
    }

    fun retryFailures(id: String) {
        if (mutableUiState.value.saving) return
        val playlist = staged[id] ?: return
        val card = mutableUiState.value.playlists.firstOrNull { it.id == id } ?: return
        if (card.phase != SpotifyImportPhase.READY) return
        updateCard(id) { it.copy(phase = SpotifyImportPhase.PENDING) }
        viewModelScope.launch {
            consecutiveSearchFailures.set(0)
            cacheMutex.withLock {
                playlist.outcomes.forEachIndexed { index, outcome ->
                    if (outcome == TrackOutcome.Failed || outcome == TrackOutcome.Unmatched) {
                        searchCache.remove(trackKey(playlist.export.tracks[index]))
                        playlist.outcomes[index] = null
                    }
                }
            }
            publishProgress(id, playlist)
            resolve(id)
        }
    }

    fun remove(id: String) {
        if (mutableUiState.value.saving) return
        playlistJobs.remove(id)?.cancel()
        staged.remove(id)
        mutableUiState.update { it.copy(playlists = it.playlists.filterNot { card -> card.id == id }) }
        if (staged.isEmpty()) {
            viewModelScope.launch {
                cacheMutex.withLock {
                    searchCache.values.forEach { it.cancel() }
                    searchCache.clear()
                }
            }
        }
    }

    fun clearError() = mutableUiState.update { it.copy(error = null) }

    fun saveReadyPlaylists() {
        if (!mutableUiState.value.canSave) return
        val cards = mutableUiState.value.playlists.filter { it.phase == SpotifyImportPhase.READY }
        mutableUiState.update { it.copy(saving = true, error = null) }
        viewModelScope.launch {
            try {
                val saved = cards.associate { card -> card.id to PlaylistEntity(
                    name = card.name,
                    isLocal = true,
                    bookmarkedAt = LocalDateTime.now(),
                    thumbnailUrl = card.thumbnail,
                ) }
                database.withTransaction {
                    cards.forEach { card ->
                        val entity = saved.getValue(card.id)
                        insert(entity)
                        staged.getValue(card.id).outcomes.filterIsInstance<TrackOutcome.Matched>()
                            .forEachIndexed { index, outcome ->
                                insert(outcome.song.toMediaMetadata())
                                insert(PlaylistSongMap(playlistId = entity.id, songId = outcome.song.id, position = index))
                            }
                    }
                }
                // The save flag blocks edits/imports until the transaction and UI state agree.
                mutableUiState.update {
                    it.copy(
                        saving = false,
                        savedCount = it.savedCount + saved.size,
                        playlists = it.playlists.map { card -> saved[card.id]?.let { entity ->
                            card.copy(phase = SpotifyImportPhase.SAVED, savedPlaylistId = entity.id)
                        } ?: card },
                    )
                }
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                mutableUiState.update { it.copy(saving = false, error = SpotifyImportError.SAVE_FAILED) }
            }
        }
    }

    private fun updateCard(id: String, transform: (SpotifyImportPlaylist) -> SpotifyImportPlaylist) {
        mutableUiState.update { state ->
            state.copy(playlists = state.playlists.map { if (it.id == id) transform(it) else it })
        }
    }

    private fun trackKey(track: SpotifyExportTrack) = SpotifyTrackMatcher.cacheKey(track)
}
