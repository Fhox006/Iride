package com.metrolist.music.data.remote

import com.metrolist.music.data.remote.ArtistArtworkParser.text
import com.metrolist.music.utils.ArtistNameMatcher
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.get
import io.ktor.client.request.head
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.URLBuilder
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.intOrNull
import org.jsoup.Jsoup
import java.net.URI
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

enum class ReleaseHeroArtworkSource { EDITORIAL, ARTIST_PORTRAIT, ALBUM }

data class ReleaseHeroArtwork(
    val imageUrl: String,
    val source: ReleaseHeroArtworkSource,
    val sourceUrl: String? = null,
)

/** Artist-header photography is separate from both artist portraits and album cover art. */
@Singleton
class ReleaseEditorialArtworkRepository @Inject constructor(
    private val artistArtworkRepository: ArtistArtworkRepository,
) {
    private val client = HttpClient(OkHttp) {
        install(HttpTimeout) {
            requestTimeoutMillis = 8_000
            connectTimeoutMillis = 4_000
            socketTimeoutMillis = 8_000
        }
    }
    private data class CacheKey(
        val artist: String,
        val album: String,
        val artwork: String,
        val channel: String?,
        val appleArtistId: String?,
    )
    private data class CacheEntry(val artwork: ReleaseHeroArtwork?, val expiresAt: Long)
    private val cache = LinkedHashMap<CacheKey, CacheEntry>(96, 0.75f, true)
    private val cacheMutex = Mutex()
    private val requests = Semaphore(2)

    suspend fun findImage(
        artistName: String,
        albumTitle: String,
        albumArtworkUrl: String,
        youtubeArtistId: String? = null,
        appleArtistId: String? = null,
    ): String? = findArtwork(artistName, albumTitle, albumArtworkUrl, youtubeArtistId, appleArtistId)?.imageUrl

    suspend fun findArtwork(
        artistName: String,
        albumTitle: String,
        albumArtworkUrl: String,
        youtubeArtistId: String? = null,
        appleArtistId: String? = null,
    ): ReleaseHeroArtwork? = withContext(Dispatchers.IO) {
        val fallback = albumArtworkUrl.takeIf(ReleaseEditorialArtworkParser::safeImageUrl)
            ?.let { ReleaseHeroArtwork(it, ReleaseHeroArtworkSource.ALBUM) }
        if (artistName.isBlank()) return@withContext fallback
        val key = CacheKey(
            ArtistNameMatcher.normalize(artistName),
            ReleaseEditorialArtworkParser.normalizedReleaseTitle(albumTitle),
            albumArtworkUrl,
            youtubeArtistId,
            appleArtistId,
        )
        cached(key)?.let { return@withContext it.artwork }
        requests.withPermit {
            cached(key)?.let { return@withPermit it.artwork }
            val artwork = withTimeoutOrNull(25_000) {
                resolve(artistName, albumTitle, youtubeArtistId, appleArtistId)
            } ?: fallback
            val lifetime = when (artwork?.source) {
                ReleaseHeroArtworkSource.EDITORIAL -> 43_200_000_000_000L
                ReleaseHeroArtworkSource.ARTIST_PORTRAIT -> 21_600_000_000_000L
                else -> 1_200_000_000_000L
            }
            cacheMutex.withLock {
                cache[key] = CacheEntry(artwork, System.nanoTime() + lifetime)
                while (cache.size > 96) cache.remove(cache.keys.first())
            }
            artwork
        }
    }

    private suspend fun cached(key: CacheKey): CacheEntry? =
        cacheMutex.withLock { cache[key]?.takeIf { it.expiresAt > System.nanoTime() } }

    private suspend fun resolve(name: String, album: String, channelId: String?, appleArtistId: String?): ReleaseHeroArtwork? {
        // Matching both release and artist disambiguates artists sharing the same display name.
        val releaseProfile = appleArtistId?.takeIf { it.matches(Regex("[0-9]+")) }
            ?.let { "https://music.apple.com/us/artist/$it" }
            ?: attempt { appleReleaseProfile(name, album) }
        if (releaseProfile != null) {
            val html = attempt { get(releaseProfile) }
            if (html != null) {
                editorial(name, releaseProfile, html)?.let { return it }
                ArtistArtworkParser.parse(ArtistArtworkSource.APPLE_MUSIC, name, releaseProfile, html)
                    ?.let { return ReleaseHeroArtwork(it.imageUrl, ReleaseHeroArtworkSource.ARTIST_PORTRAIT, it.artistUrl) }
            }
        }

        // The shared portrait repository may perform its own iTunes artist search.
        AppleMusicRequestThrottle.awaitTurn()
        val portraits = attempt { artistArtworkRepository.findArtworks(name, channelId) }.orEmpty()
        val apple = portraits.firstOrNull { it.source == ArtistArtworkSource.APPLE_MUSIC }
        if (apple != null && apple.artistUrl != releaseProfile) {
            attempt { get(apple.artistUrl) }?.let { html ->
                editorial(name, apple.artistUrl, html)?.let { return it }
            }
        }
        return (apple ?: portraits.firstOrNull())?.let {
            ReleaseHeroArtwork(it.imageUrl, ReleaseHeroArtworkSource.ARTIST_PORTRAIT, it.artistUrl)
        }
    }

    private suspend fun editorial(name: String, profile: String, html: String): ReleaseHeroArtwork? {
        return ReleaseEditorialArtworkParser.editorialImages(name, profile, html)
            .firstOrNull { image -> attempt { available(image) } == true }
            ?.let { ReleaseHeroArtwork(it, ReleaseHeroArtworkSource.EDITORIAL, profile) }
    }

    private suspend fun appleReleaseProfile(name: String, album: String): String? {
        if (album.isBlank()) return null
        AppleMusicRequestThrottle.awaitTurn()
        val country = Locale.getDefault().country.takeIf { it.matches(Regex("[A-Z]{2}")) } ?: "US"
        val url = URLBuilder("https://itunes.apple.com/search").apply {
            parameters.append("term", "$name $album")
            parameters.append("media", "music")
            parameters.append("entity", "album")
            parameters.append("country", country)
            parameters.append("limit", "25")
        }.buildString()
        val json = get(url)?.let { Json.parseToJsonElement(it) as? JsonObject } ?: return null
        val matches = (json["results"] as? JsonArray)?.filterIsInstance<JsonObject>().orEmpty()
            .filter {
                it.text("wrapperType") == "collection" && ArtistArtworkParser.sameName(name, it.text("artistName")) &&
                    ReleaseEditorialArtworkParser.sameReleaseTitle(album, it.text("collectionName"))
            }.mapNotNull { it.text("artistId") }
            .filter { it.matches(Regex("[0-9]+")) }
            .distinct()
        return matches.singleOrNull()?.let { "https://music.apple.com/us/artist/$it" }
    }

    private suspend fun get(url: String): String? {
        val response = client.get(url) {
            header(HttpHeaders.UserAgent, "Iride/1.0 (https://github.com/Fhox006/Iride)")
            header(HttpHeaders.Accept, "application/json, text/html")
        }
        if (response.status != HttpStatusCode.OK) return null
        if ((response.headers[HttpHeaders.ContentLength]?.toLongOrNull() ?: 0) > 6_000_000) return null
        return response.bodyAsText().takeIf { it.length <= 6_000_000 }
    }

    private suspend fun available(url: String): Boolean {
        val response = client.head(url)
        return response.status == HttpStatusCode.OK &&
            response.headers[HttpHeaders.ContentType]?.startsWith("image/", ignoreCase = true) == true
    }

    private suspend fun <T> attempt(block: suspend () -> T): T? = try {
        block()
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (_: Exception) {
        null
    }
}

internal object ReleaseEditorialArtworkParser {
    fun editorialImages(name: String, profileUrl: String, html: String): List<String> {
        val profile = ArtistArtworkParser.profileUrl(ArtistArtworkSource.APPLE_MUSIC, profileUrl) ?: return emptyList()
        val id = profile.substringAfterLast('/')
        val document = Jsoup.parse(html, profile)
        val canonical = document.selectFirst("meta[property=og:url]")?.attr("content")
            ?: document.selectFirst("link[rel=canonical]")?.attr("href")
        if (canonical != null && ArtistArtworkParser.profileUrl(ArtistArtworkSource.APPLE_MUSIC, canonical) != profile) {
            return emptyList()
        }
        val verifiedArtist = document.select("script[type=application/ld+json]").any { script ->
            val json = runCatching { Json.parseToJsonElement(script.data()) }.getOrNull()
            json?.let(::schemaObjects).orEmpty().any { artist ->
                artist.text("@type") in setOf("MusicGroup", "Person") &&
                    ArtistArtworkParser.sameName(name, artist.text("name")) &&
                    (artist.text("url") == null ||
                        ArtistArtworkParser.profileUrl(ArtistArtworkSource.APPLE_MUSIC, artist.text("url").orEmpty()) == profile)
            }
        }
        if (!verifiedArtist) return emptyList()

        val server = document.selectFirst("script#serialized-server-data")?.data()
            ?.let { runCatching { Json.parseToJsonElement(it) as? JsonObject }.getOrNull() } ?: return emptyList()
        val pages = (server["data"] as? JsonArray)?.filterIsInstance<JsonObject>().orEmpty()
        val page = pages.singleOrNull {
            val intent = it["intent"] as? JsonObject
            intent?.text("\$kind") == "ArtistDetailPageIntent" &&
                artistDescriptorMatches(intent["contentDescriptor"] as? JsonObject, id)
        }?.get("data") as? JsonObject ?: return emptyList()
        // Never traverse recommendations, playlists, music videos or other artists on the same page.
        val headers = (page["sections"] as? JsonArray)?.filterIsInstance<JsonObject>().orEmpty()
            .filter { it.text("itemKind") == "artistDetailHeader" }
            .flatMap { (it["items"] as? JsonArray)?.filterIsInstance<JsonObject>().orEmpty() }
        val header = headers.singleOrNull {
            it.text("id") == id && ArtistArtworkParser.sameName(name, it.text("title")) &&
                artistDescriptorMatches(it["contentDescriptor"] as? JsonObject, id)
        } ?: return emptyList()
        val still = (header["wideArtwork"] as? JsonObject)?.get("dictionary") as? JsonObject
        val motion = (header["wideVideoArtwork"] as? JsonObject)?.get("dictionary") as? JsonObject
        return listOfNotNull(
            still?.let(::landscapeUrl),
            (motion?.get("motionArtistWide16x9") as? JsonObject)?.get("previewFrame")
                ?.let { it as? JsonObject }?.let(::landscapeUrl),
            (motion?.get("motionArtistFullscreen16x9") as? JsonObject)?.get("previewFrame")
                ?.let { it as? JsonObject }?.let(::landscapeUrl),
        ).distinct()
    }

    fun normalizedReleaseTitle(title: String): String = ArtistNameMatcher.normalize(
        title.trim().replace(Regex("\\s+[-–—]\\s+(?:single|ep)$", RegexOption.IGNORE_CASE), ""),
    )

    fun sameReleaseTitle(left: String, right: String?): Boolean {
        val title = normalizedReleaseTitle(left)
        return title.isNotBlank() && right != null && title == normalizedReleaseTitle(right)
    }

    fun safeImageUrl(url: String): Boolean {
        val uri = runCatching { URI(url) }.getOrNull() ?: return false
        return uri.scheme == "https" && !uri.host.isNullOrBlank() && uri.userInfo == null &&
            (uri.port == -1 || uri.port == 443)
    }

    private fun artistDescriptorMatches(descriptor: JsonObject?, id: String): Boolean =
        descriptor?.text("kind") == "artist" &&
            (descriptor["identifiers"] as? JsonObject)?.text("storeAdamID") == id &&
            (descriptor.text("url") == null ||
                ArtistArtworkParser.profileUrl(ArtistArtworkSource.APPLE_MUSIC, descriptor.text("url").orEmpty()) ==
                "https://music.apple.com/us/artist/$id")

    private fun landscapeUrl(artwork: JsonObject): String? {
        val width = (artwork["width"] as? JsonPrimitive)?.intOrNull ?: return null
        val height = (artwork["height"] as? JsonPrimitive)?.intOrNull ?: return null
        if (width < 1_000 || height < 600 || width.toDouble() / height !in 1.4..2.5) return null
        val targetWidth = minOf(width, 1_600)
        val targetHeight = (height.toDouble() * targetWidth / width).toInt().coerceAtLeast(1)
        val url = artwork.text("url")?.replace("{w}", targetWidth.toString())
            ?.replace("{h}", targetHeight.toString())?.replace("{c}", "bb")?.replace("{f}", "jpg") ?: return null
        if (!safeImageUrl(url)) return null
        val uri = URI(url)
        if (uri.host?.endsWith(".mzstatic.com") != true) return null
        val assetFamily = uri.path.removePrefix("/image/thumb/").substringBefore('/')
        if (!uri.path.startsWith("/image/thumb/") ||
            !assetFamily.matches(Regex("(?:Features|Video|AMCArtistImages|ArtistImages)[0-9]*"))) return null
        return url
    }

    private fun schemaObjects(element: JsonElement): List<JsonObject> = when (element) {
        is JsonArray -> element.flatMap(::schemaObjects)
        is JsonObject -> listOf(element) + element["@graph"]?.let(::schemaObjects).orEmpty()
        else -> emptyList()
    }
}

/** Apple's public Search API is limited to approximately 20 requests per minute. */
internal object AppleMusicRequestThrottle {
    private val mutex = Mutex()
    private var lastRequest = 0L

    suspend fun awaitTurn() = mutex.withLock {
        val remaining = 3_100_000_000L - (System.nanoTime() - lastRequest)
        if (remaining > 0) delay((remaining + 999_999) / 1_000_000)
        lastRequest = System.nanoTime()
    }
}
