package com.metrolist.innertube

import com.metrolist.innertube.models.AlbumItem
import com.metrolist.innertube.models.Artist
import com.metrolist.innertube.models.SongItem
import com.metrolist.innertube.pages.AlbumPage
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.text.Normalizer
import java.util.Locale
import kotlin.math.abs
import kotlin.math.max

/** Catalog metadata only; playable IDs always come from YouTube. */
internal object ExternalAlbumCatalog {
    private val client by lazy {
        HttpClient(OkHttp) {
            expectSuccess = true
            install(HttpTimeout) {
                requestTimeoutMillis = 5_000
                connectTimeoutMillis = 3_000
            }
        }
    }
    private data class Entry(val rows: List<JsonObject>, val expires: Long)
    private val cache = linkedMapOf<String, Entry>()
    private val locks = Array(16) { Mutex() }
    private val requests = Semaphore(2)
    private val resolutions = Semaphore(3)
    private data class ResolvedEntry(val page: AlbumPage, val expires: Long)
    private val resolvedPages = linkedMapOf<String, ResolvedEntry>()
    private val albumLocks = Array(16) { Mutex() }

    suspend fun search(query: String, country: String): List<AlbumItem> = optional {
        if (query.isBlank()) return@optional emptyList()
        rows("search", mapOf("term" to query, "entity" to "album", "country" to country, "limit" to "50"))
            .mapNotNull { album(it, country) }
            .distinctBy { it.id }
    }.orEmpty()

    suspend fun artistAlbums(name: String, country: String): List<AlbumItem> = optional {
        if (name.isBlank()) return@optional emptyList()
        val artists = rows("search", mapOf("term" to name, "entity" to "musicArtist", "country" to country, "limit" to "25"))
        val artistId = matchingArtistId(name, artists) ?: return@optional emptyList()
        rows("lookup", mapOf("id" to artistId, "entity" to "album", "country" to country, "limit" to "200"))
            .mapNotNull { album(it, country) }
            .filter { release -> release.artists.orEmpty().any { normalize(name) in credits(it.name) } }
            .distinctBy { it.id }
    }.orEmpty()

    suspend fun releaseDate(title: String, artist: String?, year: Int?, country: String): String? = optional {
        if (title.isBlank() || artist.isNullOrBlank()) return@optional null
        rows("search", mapOf(
            "term" to "$artist $title",
            "entity" to "album",
            "country" to country,
            "limit" to "50",
        )).asSequence()
            .filter { it.text("wrapperType") == "collection" && it.text("collectionType") == "Album" }
            .filter { releaseName(it.text("collectionName").orEmpty()) == releaseName(title) }
            .filter { row -> credits(row.text("artistName").orEmpty()).any { it == normalize(artist) } }
            .sortedBy { row -> if (year != null && row.text("releaseDate")?.take(4)?.toIntOrNull() == year) 0 else 1 }
            .mapNotNull { row -> row.text("releaseDate")?.take(10)?.takeIf { it.matches(Regex("\\d{4}-\\d{2}-\\d{2}")) } }
            .firstOrNull()
    }

    internal fun matchingArtistId(name: String, artists: List<JsonObject>): String? = artists
        .filter { it.text("wrapperType") == "artist" && it.text("artistName")?.trim()?.equals(name.trim(), ignoreCase = true) == true }
        .mapNotNull { it.text("artistId")?.takeIf { id -> id.isNotEmpty() && id.all(Char::isDigit) } }
        .distinct().singleOrNull()

    suspend fun album(id: String, withSongs: Boolean): AlbumPage {
        if (!withSongs) return fetchAlbum(id, false)
        return albumLocks[(id.hashCode() and Int.MAX_VALUE) % albumLocks.size].withLock {
            synchronized(resolvedPages) {
                resolvedPages[id]?.takeIf { it.expires > System.currentTimeMillis() }
            }?.let { return@withLock it.page }
            val page = fetchAlbum(id, true)
            synchronized(resolvedPages) {
                val lifetime = if (page.unavailableSongs.isEmpty()) 10 * 60_000L else 30_000L
                resolvedPages[id] = ResolvedEntry(page, System.currentTimeMillis() + lifetime)
                while (resolvedPages.size > 16) resolvedPages.remove(resolvedPages.keys.first())
            }
            page
        }
    }

    private suspend fun fetchAlbum(id: String, withSongs: Boolean): AlbumPage {
        val parts = id.split(':')
        require(parts.size == 3 && parts[0] == "itunes" && parts[1].matches(Regex("[a-z]{2}")) &&
            parts[2].isNotEmpty() && parts[2].all(Char::isDigit))
        val rows = rows("lookup", mapOf("id" to parts[2], "country" to parts[1], "entity" to "song", "limit" to "200"))
        val album = rows.firstNotNullOfOrNull { album(it, parts[1]) } ?: error("Album not found in external catalog")
        val releaseDate = rows.firstOrNull { it.text("wrapperType") == "collection" }?.text("releaseDate")?.take(10)
        if (!withSongs) return AlbumPage(album, emptyList(), emptyList(), releaseDate = releaseDate)
        val tracks = rows.filter { it.text("kind") == "song" && it.text("collectionId") == parts[2] }
            .sortedWith(compareBy({ it.number("discNumber") ?: 1 }, { it.number("trackNumber") ?: 0 }))
        require(tracks.isNotEmpty()) { "External album has no track list" }
        val matches = arrayOfNulls<SongItem>(tracks.size)
        // Keep long releases within the album screen's loading deadline, preserving
        // completed matches when the remaining lookups run out of time.
        withTimeoutOrNull(40_000) {
            coroutineScope {
                tracks.mapIndexed { index, track ->
                    async {
                        resolutions.withPermit {
                            matches[index] = withTimeoutOrNull(10_000) { resolve(track) }?.let {
                                it.copy(explicit = it.explicit || track.text("trackExplicitness") == "explicit")
                            }
                        }
                    }
                }.awaitAll()
            }
        }
        val usedIds = mutableSetOf<String>()
        matches.indices.forEach { index ->
            matches[index]?.let { if (!usedIds.add(it.id)) matches[index] = null }
        }
        return AlbumPage(
            album = album,
            songs = matches.filterNotNull(),
            otherVersions = emptyList(),
            releaseDate = releaseDate,
            unavailableSongs = tracks.mapIndexedNotNull { index, track ->
                track.text("trackName").takeIf { matches[index] == null }
            },
        )
    }

    private suspend fun resolve(track: JsonObject): SongItem? {
        val title = track.text("trackName") ?: return null
        val artist = track.text("artistName") ?: return null
        val duration = track.number("trackTimeMillis")?.div(1000) ?: return null
        val query = "$artist $title"
        val songs = YouTube.search(query, YouTube.SearchFilter.FILTER_SONG).getOrNull()?.items.orEmpty().filterIsInstance<SongItem>()
        currentCoroutineContext().ensureActive()
        selectTrack(title, artist, duration, songs)?.let { return it }
        val videos = YouTube.search(query, YouTube.SearchFilter.FILTER_VIDEO).getOrNull()?.items.orEmpty().filterIsInstance<SongItem>()
        currentCoroutineContext().ensureActive()
        return selectTrack(title, artist, duration, videos)
    }

    internal fun selectTrack(title: String, artist: String, duration: Int, candidates: List<SongItem>): SongItem? {
        val names = credits(artist)
        return candidates.filter { song ->
            val actualDuration = song.duration ?: return@filter false
            val credited = song.artists.any { candidate -> credits(candidate.name).any { it in names } }
            val cleanedTitle = song.title.replace(Regex("(?i)\\s*[\\[(](?:official (?:(?:music|visual|lyric) )?(?:video|audio)|official lyrics?|lyrics?|visuali[sz]er|audio)[\\])]"), "")
            val withoutArtist = cleanedTitle.substringAfter(" - ", cleanedTitle)
            // Keep edition qualifiers (live, remix, acoustic, sped up) significant.
            credited && (normalize(cleanedTitle) == normalize(title) ||
                (normalize(withoutArtist) == normalize(title) && names.any { normalize(cleanedTitle.substringBefore(" - ")) == it })) &&
                abs(actualDuration - duration) <= max(8, duration / 12)
        }.minByOrNull { abs(requireNotNull(it.duration) - duration) }
    }

    internal fun merge(native: List<AlbumItem>, external: List<AlbumItem>): List<AlbumItem> {
        val merged = native.toMutableList()
        for (album in external) {
            val existingIndex = merged.indexOfFirst { other ->
                other.id == album.id || (releaseName(other.title) == releaseName(album.title) &&
                    (other.year == null || album.year == null || other.year == album.year) &&
                    other.artists.orEmpty().any { a -> album.artists.orEmpty().any { b -> credits(a.name).intersect(credits(b.name)).isNotEmpty() } })
            }
            if (existingIndex < 0) {
                merged += album
            } else {
                val existing = merged[existingIndex]
                if (existing.releaseDate == null && album.releaseDate != null) {
                    merged[existingIndex] = existing.copy(releaseDate = album.releaseDate)
                }
            }
        }
        return merged
    }

    internal fun mergeArtistAlbums(
        native: List<AlbumItem>,
        nativeShortReleases: List<AlbumItem>,
        external: List<AlbumItem>,
    ): List<AlbumItem> {
        val enrichedNative = native.map { release ->
            val catalogDate = external.firstOrNull { candidate ->
                releaseName(candidate.title) == releaseName(release.title) &&
                    release.artists.orEmpty().any { releaseArtist ->
                        candidate.artists.orEmpty().any { candidateArtist ->
                            credits(releaseArtist.name).intersect(credits(candidateArtist.name)).isNotEmpty()
                        }
                    }
            }?.releaseDate
            if (release.releaseDate == null && catalogDate != null) release.copy(releaseDate = catalogDate) else release
        }
        val additions = external.filterNot { candidate ->
            nativeShortReleases.any { shortRelease ->
                releaseName(candidate.title) == releaseName(shortRelease.title) &&
                    candidate.artists.orEmpty().any { candidateArtist ->
                        shortRelease.artists.orEmpty().any { shortArtist ->
                            credits(candidateArtist.name).intersect(credits(shortArtist.name)).isNotEmpty()
                        }
                    }
            }
        }
        return merge(enrichedNative, additions)
    }

    internal fun album(row: JsonObject, country: String): AlbumItem? {
        if (row.text("wrapperType") != "collection" || row.text("collectionType") != "Album") return null
        val id = row.text("collectionId")?.takeIf { it.isNotEmpty() && it.all(Char::isDigit) } ?: return null
        return AlbumItem(
            browseId = "itunes:$country:$id",
            playlistId = "",
            title = row.text("collectionName") ?: return null,
            artists = splitCredits(row.text("artistName") ?: return null).map { Artist(it, null) },
            year = row.text("releaseDate")?.take(4)?.toIntOrNull(),
            thumbnail = row.text("artworkUrl100")?.replace("100x100bb", "600x600bb") ?: return null,
            explicit = row.text("collectionExplicitness") == "explicit",
            albumType = when {
                row.text("collectionName")?.endsWith(" - Single") == true -> "Single"
                row.text("collectionName")?.endsWith(" - EP") == true -> "EP"
                else -> "Album"
            },
            releaseDate = row.text("releaseDate")?.take(10),
        )
    }

    private suspend fun rows(path: String, parameters: Map<String, String>): List<JsonObject> {
        val key = path + parameters.entries.sortedBy { it.key }.joinToString()
        return locks[(key.hashCode() and Int.MAX_VALUE) % locks.size].withLock {
            synchronized(cache) { cache[key]?.takeIf { it.expires > System.currentTimeMillis() } }?.let { return@withLock it.rows }
            val result = requests.withPermit {
                val response = client.get("https://itunes.apple.com/$path") {
                    parameters.forEach { (key, value) -> parameter(key, value) }
                }
                Json.parseToJsonElement(response.bodyAsText()).jsonObject["results"]?.jsonArray
                    ?.map { it.jsonObject } ?: error("Invalid catalog response")
            }
            synchronized(cache) {
                cache[key] = Entry(result, System.currentTimeMillis() + 30 * 60_000)
                while (cache.size > 64) cache.remove(cache.keys.first())
            }
            result
        }
    }

    private suspend fun <T> optional(block: suspend () -> T): T? = try {
        block()
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (_: Exception) {
        null
    }

    private fun JsonObject.text(key: String): String? = get(key)?.jsonPrimitive?.contentOrNull
    private fun JsonObject.number(key: String): Int? = text(key)?.toIntOrNull()
    private fun credits(value: String): Set<String> = value.replace(Regex("(?i) - topic$"), "")
        .split(Regex("\\s*(?:&|,| feat\\.? | featuring )\\s*"))
        .map(::normalize).filter(String::isNotEmpty).toSet()

    private fun splitCredits(value: String): List<String> = value
        .split(Regex("\\s*(?:&|,| feat\\.? | featuring )\\s*", RegexOption.IGNORE_CASE))
        .map(String::trim)
        .filter(String::isNotEmpty)
        .distinctBy(::normalize)

    private fun releaseName(value: String): String = normalize(value.replace(Regex("(?i) - (?:Single|EP)$"), ""))

    private fun normalize(value: String): String = Normalizer.normalize(value, Normalizer.Form.NFKD)
        .replace(Regex("\\p{M}"), "").lowercase(Locale.ROOT).replace(Regex("[^\\p{L}\\p{N}]"), "")
}
