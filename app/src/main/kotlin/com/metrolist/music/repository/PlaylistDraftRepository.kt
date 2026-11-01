package com.metrolist.music.repository

import com.metrolist.innertube.YouTube
import com.metrolist.innertube.models.SongItem
import com.metrolist.innertube.models.WatchEndpoint
import com.metrolist.innertube.utils.completed
import com.metrolist.music.db.MusicDatabase
import com.metrolist.music.db.entities.PlaylistEntity
import com.metrolist.music.db.entities.PlaylistSongMap
import com.metrolist.music.db.entities.Song
import com.metrolist.music.models.MediaMetadata
import com.metrolist.music.models.toMediaMetadata
import com.metrolist.music.utils.SyncUtils
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import timber.log.Timber
import java.time.LocalDateTime
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import javax.inject.Inject

/** Bounded, per-draft caches keep selecting and removing songs free of repeat requests. */
class PlaylistDraftRepository @Inject constructor(
    private val database: MusicDatabase,
    private val syncUtils: SyncUtils,
) {
    private val searchCache = LinkedHashMap<String, List<MediaMetadata>>()
    private val radioCache = LinkedHashMap<String, List<MediaMetadata>>()
    private val localIds = mutableSetOf<String>()

    fun clear() {
        searchCache.clear()
        radioCache.clear()
        localIds.clear()
    }

    suspend fun localSongs(query: String = ""): List<MediaMetadata> = withContext(Dispatchers.IO) {
        database.playlistDraftSongs(query).first().also { songs ->
            localIds.addAll(songs.filter { it.song.isLocal }.map { it.id })
        }.map { it.toMediaMetadata() }
    }

    suspend fun songs(ids: List<String>): List<MediaMetadata> = withContext(Dispatchers.IO) {
        val uniqueIds = ids.distinct()
        val known = uniqueIds.mapNotNull { id -> database.song(id).first() }.associateBy { it.id }
        localIds.addAll(known.values.filter { it.song.isLocal }.map { it.id })
        val missing = uniqueIds.filterNot { it in known }
        val fetched = missing.chunked(50).flatMap { chunk ->
            checkNotNull(withTimeoutOrNull(8_000) {
                YouTube.queue(videoIds = chunk).getOrThrow()
            }) { "Playlist seeds timed out" }
        }.associateBy { it.id }
        uniqueIds.map { id ->
            known[id]?.toMediaMetadata() ?: checkNotNull(fetched[id]) { "Missing playlist seed: $id" }.toMediaMetadata()
        }
    }

    suspend fun playlistSongs(id: String): List<MediaMetadata> = withContext(Dispatchers.IO) {
        database.playlistSongs(id).first().map { it.song }.also { songs ->
            localIds.addAll(songs.filter { it.song.isLocal }.map { it.id })
        }.map { it.toMediaMetadata() }
    }

    suspend fun importSongs(songs: List<Song>, onProgress: (Int, String) -> Unit): List<MediaMetadata> =
        withContext(Dispatchers.IO) {
            val semaphore = Semaphore(4)
            var completed = 0
            coroutineScope {
                songs.map { song ->
                    async {
                        semaphore.withPermit {
                            val metadata = database.song(song.id).first()?.toMediaMetadata()
                                ?: checkNotNull(withTimeoutOrNull(15_000) {
                                    if (song.id.matches(Regex("[A-Za-z0-9_-]{11}"))) {
                                        YouTube.queue(videoIds = listOf(song.id)).getOrThrow()
                                            .firstOrNull { it.id == song.id }?.toMediaMetadata()
                                    } else {
                                        val artists = song.artists.joinToString(" ") {
                                            if ('%' in it.name) runCatching {
                                                URLDecoder.decode(it.name, StandardCharsets.UTF_8.name())
                                            }.getOrDefault(it.name) else it.name
                                        }
                                        YouTube.search("${song.title} $artists", YouTube.SearchFilter.FILTER_SONG)
                                            .getOrThrow().items.filterIsInstance<SongItem>()
                                            .firstOrNull { !it.isEpisode }?.toMediaMetadata()
                                    }
                                }) { "No matching song for ${song.title}" }
                            withContext(Dispatchers.Main) {
                                completed++
                                onProgress(completed * 100 / songs.size, song.title)
                            }
                            metadata
                        }
                    }
                }.awaitAll()
            }
        }

    suspend fun search(query: String): List<MediaMetadata>? = cached(searchCache, query) {
        YouTube.search(query, YouTube.SearchFilter.FILTER_SONG).getOrThrow().items
            .filterIsInstance<SongItem>().filterNot { it.isEpisode }.map { it.toMediaMetadata() }
    }

    suspend fun related(seed: MediaMetadata): List<MediaMetadata>? {
        if (seed.id in localIds) return emptyList()
        return cached(radioCache, seed.id) {
            YouTube.next(WatchEndpoint(videoId = seed.id)).getOrThrow().items
                .filterNot { it.isEpisode }.map { it.toMediaMetadata() }
        }
    }

    private suspend fun cached(
        cache: LinkedHashMap<String, List<MediaMetadata>>,
        key: String,
        fetch: suspend () -> List<MediaMetadata>,
    ): List<MediaMetadata>? {
        cache[key]?.let { return it }
        return try {
            withTimeoutOrNull(8_000) { fetch() }?.also {
                if (cache.size >= 8) cache.remove(cache.keys.first())
                cache[key] = it
            }
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            Timber.d(error, "Playlist discovery unavailable")
            null
        }
    }

    /** Local commit precedes networking; a remote failure can never discard the draft. */
    suspend fun create(
        id: String,
        name: String,
        songs: List<MediaMetadata>,
        sync: Boolean,
    ): CreatedPlaylist = withContext(Dispatchers.IO) {
        val uniqueSongs = songs.distinctBy { it.id }
        val hasLocalSongs = uniqueSongs.any { database.song(it.id).first()?.song?.isLocal == true }
        val entity = PlaylistEntity(
            id = id,
            name = name,
            bookmarkedAt = LocalDateTime.now(),
            isLocal = !sync || hasLocalSongs,
        )
        database.withTransaction {
            insert(entity)
            uniqueSongs.forEachIndexed { index, song ->
                insert(song)
                insert(PlaylistSongMap(playlistId = id, songId = song.id, position = index))
            }
        }
        var savedLocally = false
        if (sync && !hasLocalSongs) {
            try {
                val browseId = withTimeoutOrNull(12_000) {
                    YouTube.createPlaylist(name, uniqueSongs.map { it.id })
                }
                if (browseId != null) {
                    database.update(entity.copy(browseId = browseId, isAutoSync = true, remoteSongCount = uniqueSongs.size))
                } else {
                    savedLocally = true
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                Timber.w(error, "Playlist saved locally; remote creation failed")
                savedLocally = true
            }
        }
        CreatedPlaylist(id, savedLocally)
    }

    /** Append in selection order, skipping duplicates against the current transaction snapshot. */
    suspend fun append(
        playlist: PlaylistEntity,
        songs: List<MediaMetadata>,
        sync: Boolean,
    ): CreatedPlaylist = withContext(Dispatchers.IO) {
        syncUtils.withPlaylistEdit(playlist.id) {
            val browseId = playlist.browseId.takeIf { sync && !playlist.isLocal }
            val pending = mutableListOf<MediaMetadata>()
            var committed = false
            var savedLocally = false
            try {
                database.withTransaction {
                    val maps = playlistSongMaps(playlist.id, 0)
                    val existingIds = maps.map { it.songId }.toSet()
                    var position = (maps.maxOfOrNull { it.position } ?: -1) + 1
                    songs.distinctBy { it.id }.filterNot { it.id in existingIds }.forEach { song ->
                        pending += song
                        insert(song)
                        insert(PlaylistSongMap(playlistId = playlist.id, songId = song.id, position = position++))
                    }
                    updatePlaylistLastUpdated(playlist.id)
                    if (playlist.browseId != null && pending.isNotEmpty()) {
                        // A process exit during upload must not let remote sync discard local additions.
                        pausePlaylistAutoSync(playlist.id)
                    }
                }
                committed = true
                if (browseId != null && pending.isNotEmpty()) {
                    val synced = withTimeoutOrNull(12_000) {
                        check(pending.none { database.song(it.id).first()?.song?.isLocal == true }) {
                            "Local track cannot be synced"
                        }
                        pending.map { it.id }.chunked(50).forEach { ids ->
                            YouTube.addToPlaylist(browseId, ids).getOrThrow()
                        }
                        val remoteIds = YouTube.playlist(browseId).completed().getOrThrow().songs.map { it.id }.toSet()
                        pending.all { it.id in remoteIds }
                    } ?: false
                    if (!synced) error("Playlist additions could not be synced")
                    if (playlist.isAutoSync) database.resumePlaylistAutoSync(playlist.id)
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                if (!committed) throw error
                Timber.w(error, "Playlist additions saved locally")
                // Preserve local additions if the next automatic sync sees only a partial remote edit.
                database.pausePlaylistAutoSync(playlist.id)
                savedLocally = true
            }
            CreatedPlaylist(playlist.id, savedLocally)
        }
    }

    suspend fun defaultName(base: String): String = withContext(Dispatchers.IO) {
        val names = database.playlistsByNameAsc().first().map { it.playlist.name.lowercase() }.toSet()
        if (base.lowercase() !in names) return@withContext base
        var suffix = 2
        while ("$base $suffix".lowercase() in names) suffix++
        "$base $suffix"
    }
}

data class CreatedPlaylist(val id: String, val savedLocally: Boolean)

internal fun playlistDraftRecommendations(
    candidates: List<MediaMetadata>,
    selected: List<MediaMetadata>,
    title: String,
    page: Int,
): List<MediaMetadata> {
    val selectedIds = selected.map { it.id }.toSet()
    val artists = selected.flatMap { it.artists }.map { it.name.lowercase() }.toSet()
    val words = title.lowercase().split(Regex("\\W+")).filter { it.length >= 3 }
    val ranked = candidates.asSequence()
        .filterNot { it.id in selectedIds || it.isEpisode }
        .distinctBy { it.id }
        .sortedByDescending { song ->
            val artistMatch = song.artists.count { it.name.lowercase() in artists }
            val text = "${song.title} ${song.artists.joinToString { it.name }}".lowercase()
            artistMatch * 2 + words.count { it in text }
        }.toList()
    if (ranked.isEmpty()) return emptyList()
    val start = ((page.toLong() * 8) % ranked.size).toInt()
    return (ranked.drop(start) + ranked.take(start)).take(12)
}
