package com.metrolist.music.discovery

import android.content.Context
import com.metrolist.innertube.YouTube
import com.metrolist.innertube.models.AlbumItem
import com.metrolist.innertube.models.Artist
import com.metrolist.innertube.models.ArtistItem
import com.metrolist.innertube.models.WatchEndpoint
import com.metrolist.innertube.pages.AlbumPage
import com.metrolist.music.db.MusicDatabase
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File
import java.security.MessageDigest
import java.text.Normalizer
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random

/** Keeps playable album recommendations and their recent history cached between visits. */
@Singleton
class AlbumDiscoveryRepository @Inject constructor(
    @ApplicationContext context: Context,
    private val database: MusicDatabase,
) {
    private val cacheDirectory = File(context.cacheDir, "album-discovery-v1")
    private val cacheLock = Mutex()
    private val historyLock = Mutex()
    private val history = context.getSharedPreferences("album_discovery_history", Context.MODE_PRIVATE)

    internal fun usableAlbums(page: AlbumPage, albums: List<AlbumItem>, hideExplicit: Boolean): List<AlbumItem> {
        val excluded = page.otherVersions.map { it.id }.toSet() + page.album.id
        return albums.distinctBy { it.id }.filter {
            it.id !in excluded && !isSingle(it.albumType) && (!hideExplicit || !it.explicit)
        }
    }

    /** Retain catalog and cached recommendations when the network is unavailable. */
    internal suspend fun immediateAlbums(page: AlbumPage, hideExplicit: Boolean): List<AlbumItem> {
        val key = feedKey(page)
        val saved = cached(key, allowStale = true)?.let { text ->
            runCatching { kotlinx.serialization.json.Json.decodeFromString<List<AlbumItem>>(text) }.getOrNull()
        }.orEmpty()
        val contextual = usableAlbums(page, saved + page.similarAlbums, hideExplicit)
        if (contextual.isNotEmpty()) return contextual.take(20)
        val local = database.mostPlayedAlbums(fromTimeStamp = 0L, limit = 100).first()
            .filter { !it.album.isLocal && it.album.thumbnailUrl != null }
            .sortedByDescending { album -> album.artists.any { artist -> page.album.artists.orEmpty().any { it.name.equals(artist.name, true) } } }
            .map { album -> AlbumItem(
                browseId = album.id, playlistId = album.album.playlistId.orEmpty(), title = album.title,
                artists = album.artists.map { Artist(it.name, it.id) }, year = album.album.year,
                thumbnail = album.album.thumbnailUrl.orEmpty(), explicit = album.album.explicit, albumType = album.album.albumType,
            ) }
        val shared = cached("youtube-feed:recent", allowStale = true)?.let { text ->
            runCatching { kotlinx.serialization.json.Json.decodeFromString<List<AlbumItem>>(text) }.getOrNull()
        }.orEmpty()
        val sameArtists = local.filter { candidate -> candidate.artists.orEmpty().any { artist ->
            page.album.artists.orEmpty().any { it.name.equals(artist.name, true) }
        } }
        val familiar = usableAlbums(page, sameArtists, hideExplicit)
        if (familiar.isNotEmpty()) return familiar.take(20)
        return usableAlbums(page, shared + local, hideExplicit).take(20)
    }

    internal suspend fun youtubeMix(
        page: AlbumPage,
        hideExplicit: Boolean,
        rotation: Long,
        onProgress: (List<AlbumItem>) -> Unit,
    ): List<AlbumItem> = coroutineScope {
        fun artistAlbums(artist: com.metrolist.innertube.pages.ArtistPage): List<AlbumItem> = artist.sections
            .filterNot { section ->
                val title = discoveryName(section.title)
                listOf("featured", "appear", "appare", "collabor").any { it in title }
            }
            .flatMap { it.items }.filterIsInstance<AlbumItem>().map { album ->
                if (album.artists.isNullOrEmpty()) album.copy(artists = listOf(Artist(artist.artist.title, artist.artist.id))) else album
            }
        val known = database.mostPlayedArtists(fromTimeStamp = 0L, limit = 50).first()
            .map { discoveryName(it.artist.name) }.toSet()
        val pool = usableAlbums(page, immediateAlbums(page, hideExplicit), hideExplicit).toMutableList()
        val recent = readHistory("youtube-mix:${page.album.id}")
        val artistNames = page.album.artists.orEmpty().map { discoveryName(it.name) }.toSet()
        fun select(): List<AlbumItem> {
            val ordered = pool.distinctBy { it.id }.shuffled(Random(rotation)).sortedBy {
                discoveryName(it.title) + "|" + discoveryName(it.artists?.firstOrNull()?.name.orEmpty()) in recent
            }
            val (familiar, fresh) = ordered.partition { it.artists.orEmpty().any { discoveryName(it.name) in known } }
            val balanced = familiar.take(8) + fresh.take(8)
            val selectedIds = balanced.map { it.id }.toSet()
            return (balanced + ordered.filterNot { it.id in selectedIds }.take(16 - balanced.size))
                .shuffled(Random(rotation))
        }
        fun append(albums: List<AlbumItem>) {
            pool += usableAlbums(page, albums, hideExplicit)
            select().takeIf { it.isNotEmpty() }?.let(onProgress)
        }
        // Finish selection and caching even when slower catalog requests exhaust the budget.
        withTimeoutOrNull(12_000) {
            val artists = page.album.artists.orEmpty().mapNotNull { it.id }.distinct().take(2).map { id -> async {
                withTimeoutOrNull(5_000) { YouTube.artist(id).getOrNull() }
            } }.awaitAll().filterNotNull()
            artists.forEach { append(artistAlbums(it)) }
            val related = artists.flatMap { it.sections }.flatMap { it.items }.filterIsInstance<ArtistItem>()
                .distinctBy { it.id }.filterNot { discoveryName(it.title) in artistNames }
                .shuffled(Random(rotation)).sortedByDescending { discoveryName(it.title) in known }.take(3)
            related.map { artist -> async {
                withTimeoutOrNull(5_000) { YouTube.artist(artist.id).getOrNull()?.let(::artistAlbums) }.orEmpty()
            } }.awaitAll().forEach(::append)
            if (pool.size < 6) {
                val song = page.songs.getOrNull(page.songs.size / 2)
                val relatedAlbums = withTimeoutOrNull(5_000) {
                    song?.let { YouTube.next(WatchEndpoint(videoId = it.id)).getOrNull()?.relatedEndpoint }
                        ?.let { YouTube.related(it).getOrNull()?.albums }
                }.orEmpty()
                append(relatedAlbums)
            }
            if (pool.isEmpty()) {
                append(withTimeoutOrNull(5_000) {
                    YouTube.search(page.album.artists.orEmpty().joinToString(" ") { it.name }, YouTube.SearchFilter.FILTER_ALBUM)
                        .getOrNull()?.items?.filterIsInstance<AlbumItem>()
                }.orEmpty())
            }
        }
        val result = select().ifEmpty { immediateAlbums(page, hideExplicit) }
        saveFeed(page, result)
        rememberHistory("youtube-mix:${page.album.id}", result)
        result
    }

    private fun feedKey(page: AlbumPage) = "youtube-feed:${page.album.id}:mix"

    private suspend fun saveFeed(page: AlbumPage, albums: List<AlbumItem>) {
        if (albums.isEmpty()) return
        val text = kotlinx.serialization.json.Json.encodeToString(albums)
        putCache(feedKey(page), text)
        putCache("youtube-feed:recent", text)
    }

    private suspend fun readHistory(key: String): Set<String> = withContext(Dispatchers.IO) {
        historyLock.withLock { history.getStringSet(key, emptySet()).orEmpty().toSet() }
    }

    private suspend fun rememberHistory(key: String, albums: List<AlbumItem>) = withContext(Dispatchers.IO) {
        if (albums.isEmpty()) return@withContext
        historyLock.withLock {
            val editor = history.edit().putStringSet(key, albums.map { album ->
                discoveryName(album.title) + "|" + discoveryName(album.artists?.firstOrNull()?.name.orEmpty())
            }.toSet())
            history.all.keys.filterNot { it == key }.drop(127).forEach(editor::remove)
            editor.apply()
        }
    }

    private fun cacheFile(key: String): File {
        val digest = MessageDigest.getInstance("SHA-256").digest(key.toByteArray()).joinToString("") { "%02x".format(it) }
        return File(cacheDirectory, "$digest.json")
    }

    private suspend fun cached(key: String, allowStale: Boolean = false): String? = withContext(Dispatchers.IO) {
        cacheLock.withLock {
            val file = cacheFile(key)
            if (!file.exists() || (!allowStale && System.currentTimeMillis() - file.lastModified() !in 0..CACHE_TTL)) null
            else runCatching { file.readText() }.getOrNull()
        }
    }

    private suspend fun putCache(key: String, value: String) = withContext(Dispatchers.IO) {
        cacheLock.withLock {
            runCatching {
                cacheDirectory.mkdirs()
                cacheFile(key).writeText(value)
                cacheDirectory.listFiles()?.sortedByDescending { it.lastModified() }?.drop(200)?.forEach { it.delete() }
            }
        }
        Unit
    }

    private companion object {
        const val CACHE_TTL = 7L * 24 * 60 * 60 * 1000
        fun isSingle(type: String?) = type?.lowercase(Locale.ROOT) in setOf("single", "singolo")
    }
}

internal fun discoveryName(value: String): String = Normalizer.normalize(value, Normalizer.Form.NFKD)
    .replace(Regex("\\p{M}+"), "").lowercase(Locale.ROOT).replace(Regex("[^\\p{L}\\p{N}]+"), " ").trim()
