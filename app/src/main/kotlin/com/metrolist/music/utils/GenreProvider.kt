/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.utils

import android.content.Context
import com.metrolist.lastfm.LastFM
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import timber.log.Timber
import java.io.File
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

@Serializable
private data class ITunesSearchResponse(
    val results: List<ITunesTrack> = emptyList(),
)

@Serializable
private data class ITunesTrack(
    val primaryGenreName: String? = null,
)

/**
 * Resolves music genre/style tags per song. Iride has no genre data anywhere
 * else: not in the local DB, not in YouTube Music's song/album/artist
 * responses. Two free sources are combined:
 *  - Last.fm track.getTopTags: crowd-sourced folksonomy tags, the only place
 *    that has fine-grained/regional labels like "phonk", "russian rap" or
 *    "french hip hop". Uses the API key already bundled with the app.
 *  - iTunes Search: broad but reliable official genre, no key required, used
 *    as a fallback/addition when Last.fm has nothing useful.
 *
 * Results are cached both in memory and on disk (filesDir/genre_cache.json)
 * so re-opening a playlist doesn't re-query the network every time.
 */
object GenreProvider {
    private val found = ConcurrentHashMap<String, List<String>>()
    private val notFound = ConcurrentHashMap.newKeySet<String>()

    private var cacheFile: File? = null
    private val dirty = AtomicBoolean(false)
    private val ioScope = CoroutineScope(Dispatchers.IO)

    private val json = Json { isLenient = true; ignoreUnknownKeys = true }

    private val client by lazy {
        HttpClient(CIO) {
            install(HttpTimeout) {
                requestTimeoutMillis = 8000
            }
        }
    }

    // Folksonomy noise: tags people attach that describe their relationship
    // to the song, not the song itself.
    private val tagBlacklist =
        setOf(
            "seen live", "favorite", "favorites", "favourite", "favourites",
            "awesome", "amazing", "love", "loved", "cool", "beautiful",
            "best", "great", "good", "check out", "spotify", "youtube",
            "music", "song", "songs", "my music", "sexy", "party",
        )

    // "Hip-Hop", "Rap", "Rap/Hip-Hop", "Hip Hop Rap"... are all the same
    // broad genre with different punctuation — collapse them into one pill.
    // A tag with an extra qualifying word ("Italian Rap", "French Hip Hop",
    // "Phonk") is a genuinely different, more specific genre and stays as is.
    private val hipHopTokens = setOf("rap", "hip", "hop", "hiphop")

    private fun canonicalizeGenre(raw: String): String {
        val normalized =
            raw
                .lowercase(Locale.ROOT)
                .replace(Regex("[-/_,]"), " ")
                .replace(Regex("\\s+"), " ")
                .trim()
        if (normalized.isEmpty()) return raw

        val tokens = normalized.split(" ").toSet()
        if (tokens.all { it in hipHopTokens }) return "Rap/Hip Hop"

        return normalized.split(" ").joinToString(" ") { it.replaceFirstChar(Char::uppercase) }
    }

    /** Call once at app startup. Loads the on-disk cache and starts the periodic flush. */
    fun init(context: Context) {
        if (cacheFile != null) return
        cacheFile = File(context.filesDir, "genre_cache.json")
        loadCache()

        ioScope.launch {
            while (true) {
                delay(3000)
                if (dirty.getAndSet(false)) persistCache()
            }
        }
    }

    private fun loadCache() {
        val file = cacheFile ?: return
        if (!file.exists()) return
        try {
            val cached: Map<String, List<String>> = json.decodeFromString(file.readText())
            found.putAll(cached)
        } catch (e: Exception) {
            Timber.tag("GenreProvider").d(e, "Failed to load genre cache")
        }
    }

    private fun persistCache() {
        val file = cacheFile ?: return
        try {
            file.writeText(json.encodeToString(found.toMap()))
        } catch (e: Exception) {
            Timber.tag("GenreProvider").d(e, "Failed to persist genre cache")
        }
    }

    suspend fun getGenres(
        songId: String,
        title: String,
        artist: String?,
    ): List<String> {
        found[songId]?.let { return it }
        if (notFound.contains(songId)) return emptyList()

        val tags = mutableListOf<String>()

        if (LastFM.isInitialized() && artist != null) {
            tags += fetchLastFmTags(artist, title)
        }

        if (tags.isEmpty()) {
            fetchITunesGenre(title, artist)?.let { tags += it }
        }

        val result =
            tags
                .distinctBy { it.lowercase(Locale.ROOT) }
                .take(4)

        if (result.isNotEmpty()) {
            found[songId] = result
        } else {
            notFound.add(songId)
        }
        dirty.set(true)
        return result
    }

    private suspend fun fetchLastFmTags(
        artist: String,
        title: String,
    ): List<String> =
        LastFM
            .getTopTags(artist, title)
            .getOrNull()
            ?.toptags
            ?.tag
            .orEmpty()
            .sortedByDescending { it.count }
            .mapNotNull { it.name }
            .filter { name -> name.lowercase(Locale.ROOT) !in tagBlacklist }
            .map { canonicalizeGenre(it) }
            .take(3)

    private suspend fun fetchITunesGenre(
        title: String,
        artist: String?,
    ): String? =
        try {
            val term = listOfNotNull(artist, title).joinToString(" ")
            // iTunes replies with Content-Type: text/javascript, not application/json,
            // so ContentNegotiation would silently drop the body — parse the raw text instead.
            val body =
                client
                    .get("https://itunes.apple.com/search") {
                        parameter("term", term)
                        parameter("media", "music")
                        parameter("entity", "song")
                        parameter("limit", 1)
                    }.bodyAsText()
            val response = json.decodeFromString<ITunesSearchResponse>(body)
            response.results.firstOrNull()?.primaryGenreName?.let { canonicalizeGenre(it) }
        } catch (e: Exception) {
            Timber.tag("GenreProvider").d(e, "Failed to fetch iTunes genre for $title")
            null
        }
}
