package com.metrolist.music.utils

import com.metrolist.innertube.YouTube
import com.metrolist.innertube.models.ArtistItem
import com.metrolist.innertube.models.SongItem
import com.metrolist.music.db.MusicDatabase
import com.metrolist.music.db.entities.ArtistEntity
import com.metrolist.music.db.entities.Song
import com.metrolist.music.ui.utils.resize
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout

/** Shared, bounded requests keep repeated song rows from flooding YouTube Music. */
object ArtistMetadataResolver {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val requests = Semaphore(3)
    private data class CacheEntry(val result: Result<ArtistEntity?>, val expiresAt: Long)
    private val cache = LinkedHashMap<String, CacheEntry>(128, 0.75f, true)
    private val pending = mutableMapOf<String, Deferred<Result<ArtistEntity?>>>()

    private suspend fun lookup(key: String, load: suspend () -> ArtistEntity?): Result<ArtistEntity?> {
        val request = synchronized(cache) {
            cache[key]?.takeIf { it.expiresAt > System.nanoTime() }?.let { return it.result }
            pending.getOrPut(key) {
                scope.async(start = CoroutineStart.LAZY) {
                    val result = try {
                        Result.success(requests.withPermit { withTimeout(20_000) { load() } })
                    } catch (error: TimeoutCancellationException) {
                        Result.failure(error)
                    } catch (error: CancellationException) {
                        throw error
                    } catch (error: Exception) {
                        Result.failure(error)
                    }
                    synchronized(cache) {
                        val lifetime = if (result.getOrNull() != null) 900_000_000_000L else 60_000_000_000L
                        cache[key] = CacheEntry(result, System.nanoTime() + lifetime)
                        while (cache.size > 128) cache.remove(cache.keys.first())
                    }
                    result
                }.also { deferred ->
                    deferred.invokeOnCompletion { synchronized(cache) { pending.remove(key) } }
                }
            }
        }
        return request.await()
    }

    suspend fun withArtwork(database: MusicDatabase, artist: ArtistEntity): ArtistEntity = withContext(Dispatchers.IO) {
        val saved = database.getArtistById(artist.id) ?: artist
        if (!saved.thumbnailUrl.isNullOrBlank() || !saved.isYouTubeArtist || saved.isPrivatelyOwnedArtist) {
            return@withContext saved
        }
        val fetched = lookup("id:${saved.id}") {
            val item = YouTube.artist(saved.id).getOrThrow().artist
            item.thumbnail?.takeIf { it.isNotBlank() }?.let {
                saved.copy(thumbnailUrl = it.resize(544, 544), channelId = item.channelId ?: saved.channelId)
            }
        }.getOrNull() ?: return@withContext saved
        if (fetched.thumbnailUrl.isNullOrBlank()) return@withContext saved
        var updated = fetched
        database.withTransaction {
            val current = getArtistById(saved.id)
            updated = (current ?: saved).copy(thumbnailUrl = fetched.thumbnailUrl, channelId = fetched.channelId)
            if (current == null) insert(updated) else update(updated)
        }
        updated
    }

    private suspend fun resolve(name: String, song: Song): Result<ArtistEntity?> {
        val direct = lookup("name:${ArtistNameMatcher.normalize(name)}") {
            val items = YouTube.search(name, YouTube.SearchFilter.FILTER_ARTIST).getOrThrow().items
                .filterIsInstance<ArtistItem>().filter { it.id.startsWith("UC") && !it.isProfile }.distinctBy { it.id }
            ArtistNameMatcher.bestMatch(name, items) { it.title }?.toEntity()
        }
        if (direct.isFailure) return direct
        if (direct.getOrNull()?.let { ArtistNameMatcher.normalize(it.name) == ArtistNameMatcher.normalize(name) } == true) {
            return direct
        }
        // Song credits can disambiguate artists absent from the artist search results.
        val contextual = lookup("credit:${song.id}:${ArtistNameMatcher.normalize(name)}") {
            val title = TitleFeaturingParser.extract(song.title)?.first ?: song.title
            val songs = YouTube.search("$title $name", YouTube.SearchFilter.FILTER_SONG).getOrThrow().items
                .filterIsInstance<SongItem>().filter { candidate ->
                    candidate.id == song.id || (
                        ArtistNameMatcher.normalize(TitleFeaturingParser.extract(candidate.title)?.first ?: candidate.title) ==
                            ArtistNameMatcher.normalize(title) &&
                            candidate.artists.any { credit -> song.artists.any { it.id == credit.id } }
                        )
                }
            val artists = songs.flatMap { it.artists }.filter { it.id?.startsWith("UC") == true }.distinctBy { it.id }
            ArtistNameMatcher.bestMatch(name, artists) { it.name }?.let {
                ArtistEntity(id = requireNotNull(it.id), name = it.name)
            }
        }
        return if (contextual.getOrNull() != null || direct.getOrNull() == null) contextual else direct
    }

    suspend fun linkFeaturedArtists(database: MusicDatabase, song: Song) {
        withFeaturedArtists(database, song)
    }

    suspend fun withFeaturedArtists(database: MusicDatabase, song: Song): Song = withContext(Dispatchers.IO) {
        if (song.song.isLocal || song.song.isEpisode) return@withContext song
        val extracted = TitleFeaturingParser.extract(song.title)
        val names = (extracted?.second.orEmpty() +
            song.orderedArtists.drop(1).filter { !it.isYouTubeArtist && !it.isLocal }.map { it.name }).distinct()
        if (names.isEmpty()) return@withContext song
        val resolved = mutableMapOf<String, ArtistEntity?>()
        for (name in names) {
            val credited = song.artists.firstOrNull { it.isYouTubeArtist && it.name.equals(name, ignoreCase = true) }
            val result = credited?.let { Result.success(it) } ?: resolve(name, song)
            // Leave the original title intact on network failures so a later render can retry.
            if (result.isFailure) return@withContext song
            resolved[name] = result.getOrNull()
        }
        database.withTransaction { linkFeaturedArtist(song, resolved) }
        // Unsaved online songs need the same verified credits without entering the library.
        val artists = song.orderedArtists.toMutableList()
        for ((name, artist) in resolved) {
            val placeholderIndex = artists.indexOfFirst {
                !it.isYouTubeArtist && !it.isLocal && it.name.equals(name, ignoreCase = true)
            }.takeIf { it > 0 }
            if (placeholderIndex != null) artists.removeAt(placeholderIndex)
            if (artist != null && artists.none { it.id == artist.id }) {
                if (placeholderIndex != null) artists.add(placeholderIndex, artist) else artists.add(artist)
            }
        }
        song.copy(song = song.song.copy(title = extracted?.first ?: song.title), artists = artists, artistMaps = emptyList())
    }

    private fun ArtistItem.toEntity() = ArtistEntity(
        id = id,
        name = title,
        thumbnailUrl = thumbnail?.resize(544, 544),
        channelId = channelId,
    )
}
