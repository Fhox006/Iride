package com.metrolist.music.data.remote

import android.content.Context
import com.metrolist.music.data.remote.ArtistArtworkParser.text
import dagger.hilt.android.qualifiers.ApplicationContext
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.URLBuilder
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.Serializable
import javax.inject.Inject
import javax.inject.Singleton

/** Public artist pages supply artwork without account access or embedded service credentials. */
@Singleton
class ArtistArtworkRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val client = HttpClient(OkHttp) {
        install(HttpTimeout) {
            requestTimeoutMillis = 8_000
            connectTimeoutMillis = 5_000
            socketTimeoutMillis = 8_000
        }
    }
    private data class CacheKey(val name: String, val channelId: String?) {
        val storageKey: String get() = Json.encodeToString(listOf(name, channelId.orEmpty()))
    }
    @Serializable
    private data class CacheEntry(val artworks: List<ArtistArtworkCandidate>, val expiresAt: Long)
    private val cache = LinkedHashMap<CacheKey, CacheEntry>(64, 0.75f, true)
    private val lookupMutex = Mutex()
    private val requests = Semaphore(2)
    private val lookupLocks = Array(32) { Mutex() }
    private val preferences by lazy { context.getSharedPreferences("artist_artwork_candidates_v1", Context.MODE_PRIVATE) }

    suspend fun findArtworks(
        name: String,
        youtubeChannelId: String? = null,
        onCandidate: suspend (ArtistArtworkCandidate) -> Unit = {},
    ): List<ArtistArtworkCandidate> =
        withContext(Dispatchers.IO) {
            if (name.isBlank()) return@withContext emptyList()
            val key = CacheKey(name.trim(), youtubeChannelId)
            lookupLocks[(key.hashCode() and Int.MAX_VALUE) % lookupLocks.size].withLock {
                val cached = lookupMutex.withLock { cache[key] }
                    ?: runCatching {
                        preferences.getString(key.storageKey, null)?.let { Json.decodeFromString<CacheEntry>(it) }
                    }.getOrNull()
                if (cached != null && cached.expiresAt > System.currentTimeMillis()) {
                    lookupMutex.withLock {
                        cache[key] = cached
                        while (cache.size > 64) cache.remove(cache.keys.first())
                    }
                    supervisorScope { cached.artworks.map { async { onCandidate(it) } }.forEach { it.await() } }
                    return@withLock cached.artworks
                }
                requests.withPermit {
                    val artworks = supervisorScope {
                        val links = async { attempt { musicBrainzLinks(name, youtubeChannelId) }.orEmpty() }
                        listOf(ArtistArtworkSource.APPLE_MUSIC, ArtistArtworkSource.SPOTIFY).map { source ->
                            async {
                                attempt {
                                    // Apple's exact-name lookup can finish while MusicBrainz resolves links.
                                    val profile = (if (source == ArtistArtworkSource.APPLE_MUSIC) attempt { appleSearch(name) } else null)
                                        ?: links.await().firstNotNullOfOrNull { ArtistArtworkParser.profileUrl(source, it) }
                                        ?: return@attempt null
                                    get(profile)?.let { ArtistArtworkParser.parse(source, name, profile, it) }
                                        ?.also { onCandidate(it) }
                                }
                            }
                        }.mapNotNull { it.await() }
                    }
                    val lifetime = when (artworks.size) {
                        2 -> 86_400_000L
                        1 -> 300_000L
                        else -> 60_000L
                    }
                    val entry = CacheEntry(artworks, System.currentTimeMillis() + lifetime)
                    lookupMutex.withLock {
                        cache[key] = entry
                        while (cache.size > 64) cache.remove(cache.keys.first())
                        val savedKeys = preferences.all.keys.filter { it != key.storageKey }
                        preferences.edit().apply {
                            putString(key.storageKey, Json.encodeToString(entry))
                            savedKeys.take((savedKeys.size - 63).coerceAtLeast(0)).forEach { remove(it) }
                        }.apply()
                    }
                    artworks
                }
            }
        }

    private suspend fun musicBrainzLinks(name: String, channelId: String?): List<String> {
        val linkedArtist = if (channelId?.matches(Regex("UC[A-Za-z0-9_-]+")) == true) {
            musicBrainzJson("url", mapOf(
                "resource" to "https://music.youtube.com/channel/$channelId",
                "inc" to "artist-rels",
            ))?.objects("relations")?.mapNotNull { it["artist"] as? JsonObject }
                ?.distinctBy { it.text("id") }?.singleOrNull()
        } else null
        val artist = linkedArtist ?: run {
            val escapedName = name.replace("\\", "\\\\").replace("\"", "\\\"")
            val matches = musicBrainzJson("artist", mapOf("query" to "artist:\"$escapedName\"", "limit" to "10"))
                ?.objects("artists").orEmpty()
                .filter { ArtistArtworkParser.sameName(name, it.text("name")) }
                .sortedByDescending { it.text("score")?.toIntOrNull() ?: 0 }
            val best = matches.firstOrNull() ?: return emptyList()
            val score = best.text("score")?.toIntOrNull() ?: 0
            if (score < 95 || matches.getOrNull(1)?.let {
                    score - (it.text("score")?.toIntOrNull() ?: 0) < 5
                } == true) return emptyList()
            best
        }
        val id = artist.text("id")?.takeIf { it.matches(Regex("[0-9a-f-]{36}")) } ?: return emptyList()
        return musicBrainzJson("artist/$id", mapOf("inc" to "url-rels"))?.objects("relations")
            ?.mapNotNull { (it["url"] as? JsonObject)?.text("resource") }.orEmpty()
    }

    private suspend fun appleSearch(name: String): String? {
        val json = getJson(url("https://itunes.apple.com/search", mapOf(
            "term" to name, "entity" to "musicArtist", "country" to "US", "limit" to "25",
        ))) ?: return null
        // Equal names can belong to different artists; ambiguous results are never guessed.
        return json.objects("results")
            .filter { ArtistArtworkParser.sameName(name, it.text("artistName")) }
            .distinctBy { it.text("artistId") }
            .singleOrNull()?.text("artistLinkUrl")
            ?.let { ArtistArtworkParser.profileUrl(ArtistArtworkSource.APPLE_MUSIC, it) }
    }

    private suspend fun musicBrainzJson(path: String, parameters: Map<String, String>): JsonObject? {
        MusicBrainzRequestThrottle.awaitTurn()
        return getJson(url("https://musicbrainz.org/ws/2/$path", parameters + ("fmt" to "json")))
    }

    private suspend fun getJson(url: String): JsonObject? = get(url)?.let { Json.parseToJsonElement(it) as? JsonObject }

    private suspend fun get(url: String): String? {
        val response = client.get(url) {
            header(HttpHeaders.UserAgent, "Iride/1.0 (https://github.com/Fhox006/Iride)")
            header(HttpHeaders.Accept, "application/json, text/html")
        }
        if (response.status != HttpStatusCode.OK) return null
        return response.bodyAsText()
    }

    private fun url(base: String, parameters: Map<String, String>): String = URLBuilder(base).apply {
        parameters.forEach { (key, value) -> this.parameters.append(key, value) }
    }.buildString()

    private fun JsonObject.objects(key: String): List<JsonObject> =
        (get(key) as? JsonArray)?.filterIsInstance<JsonObject>().orEmpty()

    private suspend fun <T> attempt(block: suspend () -> T): T? = try {
        block()
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (_: Exception) {
        null
    }
}

/** Shared with album metadata requests to respect MusicBrainz's one-request-per-second limit. */
internal object MusicBrainzRequestThrottle {
    private val mutex = Mutex()
    private var lastRequest = 0L

    suspend fun awaitTurn() = mutex.withLock {
        val remaining = 1_000_000_000L - (System.nanoTime() - lastRequest)
        if (remaining > 0) delay((remaining + 999_999) / 1_000_000)
        lastRequest = System.nanoTime()
    }
}
