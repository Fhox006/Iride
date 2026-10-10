package com.metrolist.music.discovery

import android.content.Context
import com.metrolist.innertube.YouTube
import com.metrolist.innertube.models.AlbumItem
import com.metrolist.innertube.models.Artist
import com.metrolist.innertube.models.ArtistItem
import com.metrolist.innertube.models.WatchEndpoint
import com.metrolist.innertube.pages.AlbumPage
import com.metrolist.music.data.remote.MusicBrainzRequestThrottle
import com.metrolist.music.db.MusicDatabase
import com.metrolist.music.utils.GenreProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.http.URLBuilder
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONObject
import java.io.File
import java.security.MessageDigest
import java.text.Normalizer
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.ln
import kotlin.random.Random

internal data class DiscoveryAlbum(
    val id: String,
    val title: String,
    val artists: List<String>,
    val artistIds: List<String>,
    val tags: Map<String, Double>,
    val releaseIds: List<String>,
    val year: Int?,
) {
    val key: String get() = discoveryName(title) + "|" + discoveryName(artists.firstOrNull().orEmpty())
}

internal data class AlbumDiscoveryContext(val seed: DiscoveryAlbum)

/**
 * YouTube supplies playable recommendations. Album and track tags refine regional searches;
 * sparse metadata never prevents the shelves from retaining catalog or cached results.
 */
@Singleton
class AlbumDiscoveryRepository @Inject constructor(
    @ApplicationContext context: Context,
    private val database: MusicDatabase,
) {
    private val cacheDirectory = File(context.cacheDir, "album-discovery-v1")
    private val cacheLock = Mutex()
    private val historyLock = Mutex()
    private val history = context.getSharedPreferences("album_discovery_history", Context.MODE_PRIVATE)
    private val cooldowns = ConcurrentHashMap<String, Long>()
    private val requests = Semaphore(3)
    private val client = HttpClient(OkHttp) {
        install(HttpTimeout) {
            requestTimeoutMillis = 8_000
            connectTimeoutMillis = 4_000
        }
    }

    private fun usableAlbums(page: AlbumPage, albums: List<AlbumItem>, hideExplicit: Boolean): List<AlbumItem> {
        val excluded = page.otherVersions.map { it.id }.toSet() + page.album.id
        return albums.distinctBy { it.id }.filter {
            it.id !in excluded && !isSingle(it.albumType) && (!hideExplicit || !it.explicit)
        }
    }

    /** Keep real cached/catalog albums available even when metadata services have no coverage. */
    internal suspend fun immediateAlbums(page: AlbumPage, hideExplicit: Boolean, language: DiscoveryLanguage? = null): List<AlbumItem> {
        val key = feedKey(page, language)
        val saved = cached(key, allowStale = true)?.let { text ->
            runCatching { kotlinx.serialization.json.Json.decodeFromString<List<AlbumItem>>(text) }.getOrNull()
        }.orEmpty()
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
        val contextual = usableAlbums(page, saved + page.similarAlbums, hideExplicit)
        if (contextual.isNotEmpty()) return contextual.take(20)
        val sameArtists = local.filter { candidate -> candidate.artists.orEmpty().any { artist ->
            page.album.artists.orEmpty().any { it.name.equals(artist.name, true) }
        } }
        val familiar = usableAlbums(page, sameArtists, hideExplicit)
        if (familiar.isNotEmpty()) return familiar.take(20)
        return usableAlbums(page, shared + local, hideExplicit).take(20)
            .ifEmpty { page.otherVersions.filter { !hideExplicit || !it.explicit }.ifEmpty { listOf(page.album) } }
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
            val count = minOf(familiar.size, fresh.size, 8)
            return if (count > 0) (familiar.take(count) + fresh.take(count)).shuffled(Random(rotation)) else ordered.take(16)
        }
        fun append(albums: List<AlbumItem>) {
            pool += usableAlbums(page, albums, hideExplicit)
            select().takeIf { it.isNotEmpty() }?.let(onProgress)
        }
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
        val result = select().ifEmpty { immediateAlbums(page, hideExplicit) }
        saveFeed(page, null, result)
        rememberHistory("youtube-mix:${page.album.id}", result)
        result
    }

    internal suspend fun youtubeStyle(
        page: AlbumPage,
        language: DiscoveryLanguage,
        hideExplicit: Boolean,
        rotation: Long,
        source: AlbumDiscoveryContext?,
        onProgress: (List<AlbumItem>) -> Unit,
    ): List<AlbumItem> = coroutineScope {
        val sampledSongs = page.songs.filter { it.title.isNotBlank() }.let { songs ->
            if (songs.isEmpty()) emptyList() else listOf(songs.first(), songs[songs.size / 2]).distinctBy { it.id }
        }
        val tags = sampledSongs.map { song -> async {
            withTimeoutOrNull(2_000) { GenreProvider.getGenres(song.id, song.title, song.artists.firstOrNull()?.name) }.orEmpty()
        } }.awaitAll().flatten()
        val styles = (source?.seed?.tags?.entries?.sortedByDescending { it.value }?.map { it.key }.orEmpty() + tags)
            .filterNot { discoveryName(it) in ignoredTags }.distinctBy(::discoveryName).take(2)
        val languageName = language.languageName(Locale.ENGLISH)
        val queries = styles.map { "$languageName $it" }.ifEmpty {
            listOf("$languageName ${page.album.artists?.firstOrNull()?.name.orEmpty()}")
        }
        val pool = mutableListOf<AlbumItem>()
        for (query in queries) {
            val albums = withTimeoutOrNull(5_000) {
                YouTube.search(query, YouTube.SearchFilter.FILTER_ALBUM).getOrNull()?.items?.filterIsInstance<AlbumItem>()
            }.orEmpty()
            pool += usableAlbums(page, albums, hideExplicit)
            if (pool.isNotEmpty()) onProgress(pool.distinctBy { it.id }.shuffled(Random(rotation)).take(16))
        }
        if (pool.isEmpty()) {
            val song = page.songs.getOrNull(page.songs.size / 2)
            pool += usableAlbums(page, withTimeoutOrNull(5_000) {
                song?.let { YouTube.next(WatchEndpoint(videoId = it.id)).getOrNull()?.relatedEndpoint }
                    ?.let { YouTube.related(it).getOrNull()?.albums }
            }.orEmpty(), hideExplicit)
        }
        val recent = readHistory("youtube-style:${page.album.id}:${language.code}")
        val result = pool.distinctBy { it.id }.shuffled(Random(rotation)).sortedBy {
            discoveryName(it.title) + "|" + discoveryName(it.artists?.firstOrNull()?.name.orEmpty()) in recent
        }.take(16)
            .ifEmpty { immediateAlbums(page, hideExplicit, language) }
        saveFeed(page, language, result)
        rememberHistory("youtube-style:${page.album.id}:${language.code}", result)
        result
    }

    private fun feedKey(page: AlbumPage, language: DiscoveryLanguage?) = "youtube-feed:${page.album.id}:${language?.code ?: "mix"}"

    private suspend fun saveFeed(page: AlbumPage, language: DiscoveryLanguage?, albums: List<AlbumItem>) {
        if (albums.isEmpty()) return
        val text = kotlinx.serialization.json.Json.encodeToString(albums)
        putCache(feedKey(page, language), text)
        putCache("youtube-feed:recent", text)
    }

    internal suspend fun prepare(page: AlbumPage): AlbumDiscoveryContext? = attempt {
        val title = cleanDiscoveryTitle(page.album.title)
        val artist = page.album.artists?.firstOrNull()?.name ?: return@attempt null
        val seed = searchGroups("releasegroup:${quote(title)} AND artistname:${quote(artist)}", 5)
            .firstOrNull { discoveryName(it.title) == discoveryName(title) &&
                it.artists.any { name -> discoveryName(name) == discoveryName(artist) } }
            ?: return@attempt null
        AlbumDiscoveryContext(seed)
    }

    private suspend fun searchGroups(query: String, limit: Int = 100): List<DiscoveryAlbum> {
        val response = getMusicBrainz("release-group", mapOf("query" to query, "limit" to limit.toString()))
            ?: return emptyList()
        return response.array("release-groups").mapNotNull(::parseAlbum)
    }

    private fun parseAlbum(json: JSONObject): DiscoveryAlbum? {
        if (!validMbid(json.optString("id")) || json.optString("primary-type") != "Album") return null
        if (json.arrayStrings("secondary-types").any { it in setOf("Compilation", "Live", "Remix", "DJ-mix") }) return null
        val credits = json.array("artist-credit")
        val artists = credits.mapNotNull { it.optJSONObject("artist")?.optString("name")?.takeIf(String::isNotBlank) }
        if (artists.isEmpty() || artists.any { it.equals("Various Artists", true) }) return null
        return DiscoveryAlbum(
            id = json.getString("id"), title = json.optString("title"), artists = artists,
            artistIds = credits.mapNotNull { it.optJSONObject("artist")?.optString("id") },
            tags = json.array("tags").filter { it.optInt("count") >= 0 }.mapNotNull {
                val tag = discoveryName(it.optString("name"))
                if (tag.isBlank() || tag in ignoredTags || tag.all(Char::isDigit)) null
                else tag to (1.0 + ln(1.0 + it.optInt("count").coerceAtLeast(0))) * if (tag in broadGenres) .2 else 1.0
            }.groupBy({ it.first }, { it.second }).mapValues { it.value.max() },
            releaseIds = json.array("releases").filter { it.optString("status") == "Official" }
                .mapNotNull { it.optString("id").takeIf(::validMbid) },
            year = json.optString("first-release-date").take(4).toIntOrNull(),
        )
    }

    private suspend fun getMusicBrainz(path: String, parameters: Map<String, String>): JSONObject? {
        val url = URLBuilder("https://musicbrainz.org/ws/2/$path").apply {
            parameters.forEach { (name, value) -> this.parameters.append(name, value) }
            this.parameters.append("fmt", "json")
        }.buildString()
        return get(url, musicBrainz = true)?.let(::JSONObject)
    }

    private suspend fun get(url: String, musicBrainz: Boolean = false): String? = attempt {
        cached(url)?.let { return@attempt it }
        val host = java.net.URI(url).host
        if ((cooldowns[host] ?: 0L) > System.currentTimeMillis()) return@attempt null
        requests.withPermit {
            if (musicBrainz) MusicBrainzRequestThrottle.awaitTurn()
            val response = client.get(url) {
                header("User-Agent", "Iride/1.0 (https://github.com/Fhox006/Iride)")
                header("Accept", "application/json")
            }
            if (response.status == HttpStatusCode.TooManyRequests || response.status == HttpStatusCode.ServiceUnavailable) {
                val seconds = response.headers["Retry-After"]?.toLongOrNull()?.coerceIn(1, 3600) ?: 60L
                cooldowns[host] = System.currentTimeMillis() + seconds * 1000
            }
            if (response.status != HttpStatusCode.OK || (response.headers["Content-Length"]?.toLongOrNull() ?: 0L) > 4_000_000) return@withPermit null
            response.bodyAsText().takeIf { it.length <= 4_000_000 }?.also { putCache(url, it) }
        }
    }

    private suspend fun readHistory(key: String): Set<String> = withContext(Dispatchers.IO) {
        historyLock.withLock { history.getStringSet(key, emptySet()).orEmpty().toSet() }
    }

    private suspend fun rememberHistory(key: String, albums: List<AlbumItem>) = withContext(Dispatchers.IO) {
        if (albums.isEmpty()) return@withContext
        historyLock.withLock {
            val editor = history.edit().putStringSet(key, albums.map { album ->
                discoveryName(cleanDiscoveryTitle(album.title)) + "|" + discoveryName(album.artists?.firstOrNull()?.name.orEmpty())
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

    private suspend fun <T> attempt(block: suspend () -> T): T? = try {
        block()
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (_: Exception) {
        null
    }

    private companion object {
        const val CACHE_TTL = 7L * 24 * 60 * 60 * 1000
        val broadGenres = setOf("pop", "rock", "electronic", "hip hop", "rap", "jazz", "r b", "folk", "metal", "classical")
        val ignoredTags = setOf("seen live", "favorites", "favourite", "owned", "albums i own", "british", "american", "italian", "english", "french", "german", "uk", "usa", "united states", "male vocalist", "female vocalist", "male vocalists", "female vocalists")
        val mbidPattern = Regex("[0-9a-f]{8}(-[0-9a-f]{4}){3}-[0-9a-f]{12}")
        fun validMbid(value: String) = value.matches(mbidPattern)
        fun quote(value: String) = "\"${value.replace("\\", "\\\\").replace("\"", "\\\"")}\""
        fun isSingle(type: String?) = type?.lowercase(Locale.ROOT) in setOf("single", "singolo")
    }
}

internal fun discoveryName(value: String): String = Normalizer.normalize(value, Normalizer.Form.NFKD)
    .replace(Regex("\\p{M}+"), "").lowercase(Locale.ROOT).replace(Regex("[^\\p{L}\\p{N}]+"), " ").trim()

private val discoveryEditionPattern = Regex(
    """\s*(?:\([^)]*(?:deluxe|expanded|remaster|anniversary|edition)[^)]*\)|\[[^\]]*(?:deluxe|expanded|remaster|anniversary|edition)[^\]]*\])""",
    RegexOption.IGNORE_CASE,
)

private fun cleanDiscoveryTitle(value: String): String = value.replace(discoveryEditionPattern, "").trim()

private fun JSONObject.array(name: String): List<JSONObject> = optJSONArray(name)?.let { array ->
    (0 until array.length()).mapNotNull(array::optJSONObject)
}.orEmpty()

private fun JSONObject.arrayStrings(name: String): List<String> = optJSONArray(name)?.let { array ->
    (0 until array.length()).mapNotNull { array.optString(it).takeIf(String::isNotBlank) }
}.orEmpty()
