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
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import java.util.concurrent.ConcurrentHashMap
import java.util.Locale
import kotlinx.serialization.Serializable
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Artist editorial fields adapted from SimpMusic/core (GPL-3.0), AMModels.kt and
 * LyricsCanvasRepositoryImpl.getArtistEditorial, revision 2894e549763b8a68225b4456d3290f3227bdbb01.
 * https://github.com/maxrave-dev/core
 */
@Serializable
data class AppleMusicArtistEditorial(
    val logoUrl: String?,
    val logoBackgroundColor: String?,
    val squareVideoUrl: String?,
    val squarePreviewUrl: String?,
    val wideVideoUrl: String?,
    val widePreviewUrl: String?,
    val logoWidth: Int = 0,
    val logoHeight: Int = 0,
)

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
    private val appleEditorialMutex = Mutex()
    private val appleEditorialCache = ConcurrentHashMap<String, EditorialCacheEntry>()
    private var appleMusicToken: String? = null
    private val preferences by lazy { context.getSharedPreferences("artist_artwork_candidates_v1", Context.MODE_PRIVATE) }
    private val editorialPreferences by lazy { context.getSharedPreferences("artist_editorial_v1", Context.MODE_PRIVATE) }

    suspend fun findAppleMusicEditorial(artistName: String): AppleMusicArtistEditorial? =
        withContext(Dispatchers.IO) {
            if (artistName.isBlank()) return@withContext null
            val cacheKey = artistName.trim().lowercase(Locale.ROOT)
            appleEditorialCache[cacheKey]?.takeIf { it.expiresAt > System.currentTimeMillis() }
                ?.let { return@withContext it.editorial }
            appleEditorialMutex.withLock {
                val cached = appleEditorialCache[cacheKey] ?: runCatching {
                    editorialPreferences.getString(cacheKey, null)?.let { Json.decodeFromString<EditorialCacheEntry>(it) }
                }.getOrNull()
                cached?.takeIf { it.expiresAt > System.currentTimeMillis() }
                    ?.let { return@withLock it.editorial }
                // As in SimpMusic, only definitive catalog answers are cached; an offline visit
                // must not suppress artwork for the entire seven-day cache lifetime.
                val editorial = try {
                    fetchAppleMusicEditorial(artistName)
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Exception) {
                    return@withLock cached?.editorial
                }
                val entry = EditorialCacheEntry(
                    editorial = editorial,
                    expiresAt = System.currentTimeMillis() + EDITORIAL_CACHE_TTL_MS,
                )
                appleEditorialCache[cacheKey] = entry
                while (appleEditorialCache.size > 64) {
                    appleEditorialCache.entries.minByOrNull { it.value.expiresAt }?.key?.let(appleEditorialCache::remove)
                }
                editorialPreferences.edit().apply {
                    putString(cacheKey, Json.encodeToString(entry))
                    editorialPreferences.all.keys.filter { it != cacheKey }
                        .take((editorialPreferences.all.size - 63).coerceAtLeast(0)).forEach(::remove)
                }.apply()
                editorial
            }
        }

    /**
     * Catalog request/field mapping adapted from SimpMusic/core SimpMusicLyrics.kt and
     * LyricsCanvasRepositoryImpl.getArtistEditorial (GPL-3.0, revision 2894e549763b8a68225b4456d3290f3227bdbb01).
     * An exact, unambiguous name match preserves Iride's artist identity policy.
     */
    private suspend fun fetchAppleMusicEditorial(name: String): AppleMusicArtistEditorial? {
        val search = appleMusicApi("https://amp-api-edge.music.apple.com/v1/catalog/us/search", mapOf(
            "term" to name,
            "types" to "artists",
            "fields[artists]" to "name",
            "format[resources]" to "map",
            "extend" to "artistUrl",
            "l" to "en-US",
            "limit" to "25",
            "platform" to "web",
        ))
        val resources = search.obj("resources")?.objectMap("artists").orEmpty()
        val artistIds = search.obj("results")?.obj("artists")?.array("data")
            ?.mapNotNull { (it as? JsonObject)?.text("id") }
            ?: resources.keys.toList()
        val match = artistIds.mapNotNull { resources[it]?.let { value -> it to value } }
            .filter { (_, value) -> ArtistArtworkParser.sameName(name, value.obj("attributes")?.text("name")) }
            .distinctBy { it.first }.singleOrNull()
            ?: return null
        val artistId = match.second.text("id") ?: match.first
        val details = appleMusicApi(
            "https://amp-api.music.apple.com/v1/catalog/us/artists/$artistId",
            mapOf(
                "art[url]" to "c,f",
                "extend" to "editorialArtwork,editorialVideo,hero,keyColor",
                "format[resources]" to "map",
                "l" to "en-US",
                "platform" to "web",
            ),
        )
        // With format[resources]=map, data contains references, not artist attributes.
        // SimpMusic's getAMArtist dereferences resources.artists[id]; retain inline support
        // for catalog responses that do not use the map representation.
        val artistResource = details.obj("resources")?.objectMap("artists")?.get(artistId)
            ?: details.array("data")?.filterIsInstance<JsonObject>()?.firstOrNull { it.text("id") == artistId }
        val attributes = artistResource?.obj("attributes")
            ?: error("Apple Music artist response has no attributes")
        val logoArtwork = attributes.obj("editorialArtwork")?.obj("musicContentColorLogoTrimmed")
        val sourceWidth = logoArtwork?.int("width") ?: 0
        val sourceHeight = logoArtwork?.int("height") ?: 0
        val logoWidth = 1000
        val logoHeight = if (sourceWidth > 0 && sourceHeight > 0) {
            (sourceHeight.toLong() * logoWidth / sourceWidth).coerceIn(1, 1000).toInt()
        } else null
        val logoUrl = logoHeight?.let { logoArtwork?.imageUrl(logoWidth, it) }
        val motion = attributes.obj("editorialVideo")
        val square = motion?.obj("motionArtistSquare1x1")
        val wide = motion?.obj("motionArtistWide16x9") ?: motion?.obj("motionArtistFullscreen16x9")
        return AppleMusicArtistEditorial(
            logoUrl = logoUrl,
            logoBackgroundColor = logoArtwork?.text("bgColor"),
            squareVideoUrl = square?.text("video")?.takeIf { it.startsWith("https://") },
            squarePreviewUrl = square?.obj("previewFrame")?.imageUrl(640, 640),
            wideVideoUrl = wide?.text("video")?.takeIf { it.startsWith("https://") },
            widePreviewUrl = wide?.obj("previewFrame")?.imageUrl(1280, 720),
            logoWidth = sourceWidth,
            logoHeight = sourceHeight,
        ).takeIf { it.logoUrl != null || it.squareVideoUrl != null || it.wideVideoUrl != null }
    }

    private suspend fun appleMusicApi(url: String, parameters: Map<String, String>): JsonObject {
        suspend fun request(token: String) = client.get(url) {
            parameters.forEach { (key, value) -> this.url.parameters.append(key, value) }
            header(HttpHeaders.Authorization, "Bearer $token")
            header(HttpHeaders.Origin, "https://music.apple.com")
            header("Referer", "https://music.apple.com/")
            header(HttpHeaders.UserAgent, "Mozilla/5.0 (Macintosh; Intel Mac OS X 10.15; rv:152.0) Gecko/20100101 Firefox/152.0")
            header(HttpHeaders.Accept, "application/json")
        }
        var token = appleMusicToken ?: scrapeAppleMusicToken().also { appleMusicToken = it }
        var response = request(token)
        if (response.status == HttpStatusCode.Unauthorized) {
            appleMusicToken = null
            token = scrapeAppleMusicToken().also { appleMusicToken = it }
            response = request(token)
        }
        check(response.status == HttpStatusCode.OK) { "Apple Music catalog request failed: ${response.status.value}" }
        return Json.parseToJsonElement(response.bodyAsText()) as? JsonObject
            ?: error("Apple Music catalog response is not an object")
    }

    private suspend fun scrapeAppleMusicToken(): String {
        val homepage = get("https://music.apple.com") ?: error("Apple Music home unavailable")
        val bundlePath = Regex("""/assets/index~[^/]+\.js""").find(homepage)?.value
            ?: error("Apple Music web bundle not found")
        val bundle = get("https://music.apple.com$bundlePath") ?: error("Apple Music bundle unavailable")
        val tokens = Regex("""eyJ[A-Za-z0-9_-]+\.[A-Za-z0-9_-]+\.[A-Za-z0-9_-]+""")
            .findAll(bundle).map { it.value }.toList()
        return tokens.firstOrNull { token ->
            runCatching {
                val payload = token.split('.').getOrNull(1) ?: return@runCatching false
                val padded = payload + "=".repeat((4 - payload.length % 4) % 4)
                String(java.util.Base64.getUrlDecoder().decode(padded)).contains("\"iss\":\"AMPWebPlay\"")
            }.getOrDefault(false)
        } ?: tokens.firstOrNull() ?: error("Apple Music bearer not found")
    }

    @Serializable
    private data class EditorialCacheEntry(
        val editorial: AppleMusicArtistEditorial?,
        val expiresAt: Long,
    )

    private fun JsonObject.obj(key: String): JsonObject? = get(key) as? JsonObject
    private fun JsonObject.array(key: String): List<JsonElement>? = (get(key) as? JsonArray)?.toList()
    private fun JsonObject.text(key: String): String? = (get(key) as? JsonPrimitive)?.contentOrNull
    private fun JsonObject.int(key: String): Int? = (get(key) as? JsonPrimitive)?.contentOrNull?.toIntOrNull()
    private fun JsonObject.objectMap(key: String): Map<String, JsonObject> = obj(key)?.entries
        ?.mapNotNull { (key, value) -> (value as? JsonObject)?.let { key to it } }?.toMap().orEmpty()
    private fun JsonObject.imageUrl(width: Int, height: Int): String? = text("url")
        ?.replace("{w}", width.toString())?.replace("{h}", height.toString())
        ?.replace("{c}", "")?.replace("{f}", "png")
        ?.takeIf { it.startsWith("https://") && ".mzstatic.com/" in it }

    private companion object {
        const val EDITORIAL_CACHE_TTL_MS = 7L * 24 * 60 * 60 * 1000
    }

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
                if (cached != null && cached.expiresAt > System.currentTimeMillis() &&
                    cached.artworks.none { it.source == ArtistArtworkSource.SPOTIFY }) {
                    lookupMutex.withLock {
                        cache[key] = cached
                        while (cache.size > 64) cache.remove(cache.keys.first())
                    }
                    val appleArtworks = cached.artworks.filter { it.source == ArtistArtworkSource.APPLE_MUSIC }
                    supervisorScope { appleArtworks.map { async { onCandidate(it) } }.forEach { it.await() } }
                    return@withLock appleArtworks
                }
                requests.withPermit {
                    val artworks = supervisorScope {
                        val links = async { attempt { musicBrainzLinks(name, youtubeChannelId) }.orEmpty() }
                        listOf(ArtistArtworkSource.APPLE_MUSIC).map { source ->
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
