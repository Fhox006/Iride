package com.metrolist.paxsenix

import android.content.Context
import com.metrolist.music.betterlyrics.TTMLParser
import com.metrolist.paxsenix.models.LyricsResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import timber.log.Timber
import java.util.Locale
import kotlin.math.abs

@Serializable
data class SpotifySearchResult(
    val trackId: String? = null,
    val id: String? = null,
    val name: String? = null,
    val duration: String? = null,
    val duration_ms: Int? = null,
    val artistName: String? = null
)

object Paxsenix {
    @Volatile
    private var client: HttpClient? = null
    private var appVersion: String = "Unknown"

    fun init(context: Context) {
        if (client != null) return
        synchronized(this) {
            if (client != null) return
            appVersion = try {
                context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "Unknown"
            } catch (e: Exception) { "Unknown" }

            client = HttpClient(CIO) {
                install(HttpTimeout) {
                    requestTimeoutMillis = 15000
                    connectTimeoutMillis = 10000
                }
                install(ContentNegotiation) {
                    json(Json { isLenient = true; ignoreUnknownKeys = true })
                }
                defaultRequest {
                    url("https://lyrics.paxsenix.org")
                    header("User-Agent", "Iride/$appVersion (Android; lyrics-client)")
                }
                expectSuccess = false
            }
        }
    }

    private val httpClient: HttpClient
        get() = client ?: throw IllegalStateException("Paxsenix.init() must be called first")

    private val titleCleanupPatterns = listOf(
        Regex("""\s*\(.*?(official|video|audio|lyrics|lyric|visualizer|hd|hq|4k|remaster|remix|live|acoustic|version|edit|extended|radio|clean|explicit).*?\)""", RegexOption.IGNORE_CASE),
        Regex("""\s*\[.*?(official|video|audio|lyrics|lyric|visualizer|hd|hq|4k|remaster|remix|live|acoustic|version|edit|extended|radio|clean|explicit).*?\]""", RegexOption.IGNORE_CASE),
        Regex("""\s*\(feat\..*?\)""", RegexOption.IGNORE_CASE),
        Regex("""\s*\(ft\..*?\)""", RegexOption.IGNORE_CASE),
        Regex("""\s*feat\..*$""", RegexOption.IGNORE_CASE),
        Regex("""\s*ft\..*$""", RegexOption.IGNORE_CASE),
    )

    private val artistSeparators = listOf(" & ", " and ", ", ", " x ", " X ", " feat. ", " feat ", " ft. ", " ft ", " featuring ", " with ")

    private fun cleanTitle(title: String): String {
        var cleaned = title.trim()
        for (pattern in titleCleanupPatterns) cleaned = cleaned.replace(pattern, "")
        return cleaned.trim()
    }

    private fun cleanArtist(artist: String): String {
        var cleaned = artist.trim()
        for (sep in artistSeparators) {
            if (cleaned.contains(sep, ignoreCase = true)) {
                cleaned = cleaned.split(sep, ignoreCase = true, limit = 2)[0]
                break
            }
        }
        return cleaned.trim()
    }

    private suspend fun searchSpotify(query: String): List<SpotifySearchResult> = runCatching {
        val response = httpClient.get("/spotify/search") { parameter("q", query) }
        if (response.status != HttpStatusCode.OK) {
            Timber.w("Paxsenix Spotify search HTTP ${response.status.value} for q=$query")
            return@runCatching emptyList()
        }
        val json = Json { ignoreUnknownKeys = true; isLenient = true }
        val array = json.parseToJsonElement(response.body<String>()).jsonArray
        array.mapNotNull { el ->
            val obj = el.jsonObject
            val id = obj["trackId"]?.jsonPrimitive?.content ?: obj["id"]?.jsonPrimitive?.content ?: return@mapNotNull null
            val name = obj["name"]?.jsonPrimitive?.content ?: ""
            val durationMs = obj["duration_ms"]?.jsonPrimitive?.content?.toIntOrNull()
            val artistName = obj["artistName"]?.jsonPrimitive?.content
                ?: (obj["artists"] as? JsonArray)?.firstOrNull()?.jsonObject?.get("name")?.jsonPrimitive?.content
                ?: ""
            SpotifySearchResult(trackId = id, name = name, duration_ms = durationMs, artistName = artistName)
        }
    }.getOrElse {
        Timber.w("Paxsenix Spotify search error: ${it.message}")
        emptyList()
    }

    private suspend fun fetchAppleMusicLyrics(spotifyId: String): Result<String> = runCatching {
        val response = httpClient.get("/apple-music/lyrics") { parameter("id", spotifyId) }
        if (response.status != HttpStatusCode.OK) {
            throw IllegalStateException("Apple Music lyrics HTTP ${response.status.value} for id=$spotifyId")
        }
        val lyricsResponse = response.body<LyricsResponse>()

        if (!lyricsResponse.ttmlContent.isNullOrBlank()) {
            val lrc = convertTTMLToAppFormat(lyricsResponse.ttmlContent)
            if (lrc.isNotEmpty()) {
                Timber.d("Paxsenix: TTML parsed for id=$spotifyId")
                return@runCatching lrc
            }
        }
        if (!lyricsResponse.elrcMultiPerson.isNullOrBlank()) return@runCatching lyricsResponse.elrcMultiPerson
        if (!lyricsResponse.elrc.isNullOrBlank()) return@runCatching lyricsResponse.elrc
        if (!lyricsResponse.plain.isNullOrBlank()) return@runCatching lyricsResponse.plain
        if (lyricsResponse.content.isNotEmpty()) {
            val hasWordLevel = lyricsResponse.type == "Syllable"
            if (!hasWordLevel) {
                return@runCatching lyricsResponse.content
                    .map { line -> line.text.joinToString(" ") { it.text } }
                    .filter { it.isNotBlank() }
                    .joinToString("\n")
            }
            return@runCatching buildString {
                lyricsResponse.content.forEach { line ->
                    val ms = line.timestamp
                    val min = ms / 1000 / 60; val sec = (ms / 1000) % 60; val cs = (ms % 1000) / 10
                    val agent = when { line.background -> "{bg}"; line.oppositeTurn -> "{agent:v2}"; else -> "{agent:v1}" }
                    val lineText = line.text.joinToString(" ") { it.text }
                    if (lineText.isNotBlank()) {
                        appendLine(String.format(Locale.US, "[%02d:%02d.%02d]%s%s", min, sec, cs, agent, lineText))
                        val words = line.text.joinToString("|") { "${it.text}:${it.timestamp.toDouble()/1000}:${it.endtime.toDouble()/1000}" }
                        if (words.isNotEmpty()) appendLine("<$words>")
                    }
                }
            }
        }
        throw IllegalStateException("No lyrics content in Apple Music response for id=$spotifyId")
    }

    private suspend fun fetchMusixmatchLyrics(title: String, artist: String, duration: Int): Result<String> = runCatching {
        val response = httpClient.get("/musixmatch/lyrics") {
            parameter("type", "word")
            parameter("t", cleanTitle(title))
            parameter("a", cleanArtist(artist))
            parameter("d", duration)
        }
        if (response.status != HttpStatusCode.OK) {
            throw IllegalStateException("Musixmatch HTTP ${response.status.value}")
        }
        val body = response.body<String>()
        if (body.contains("isError") || body.contains("\"error\"")) {
            throw IllegalStateException("Musixmatch error: ${body.take(100)}")
        }
        if (body.isBlank()) throw IllegalStateException("Musixmatch empty response")
        body
    }

    suspend fun getLyrics(
        title: String,
        artist: String,
        duration: Int,
        album: String? = null,
    ): Result<String> = runCatching {
        val cleanedTitle = cleanTitle(title)
        val cleanedArtist = cleanArtist(artist)
        val durationMs = duration * 1000

        Timber.d("Paxsenix getLyrics: title='$title' artist='$artist' duration=$duration")

        val queries = buildList {
            add("$cleanedTitle $cleanedArtist")
            if (cleanedTitle != title.trim()) add("$title $cleanedArtist")
        }

        for (query in queries) {
            val results = searchSpotify(query)
            Timber.d("Paxsenix Spotify search '$query': ${results.size} results")

            val scored = results.mapNotNull { r ->
                var score = 0.0
                r.duration_ms?.let { d ->
                    val diff = abs(d - durationMs)
                    score += when { diff <= 2000 -> 100.0; diff <= 5000 -> 50.0; diff <= 10000 -> 10.0; else -> -50.0 }
                }
                val rArtist = r.artistName?.lowercase() ?: ""
                val tArtist = cleanedArtist.lowercase()
                if (rArtist.contains(tArtist) || tArtist.contains(rArtist)) score += 50.0
                else {
                    val words = tArtist.split(Regex("\\s+")).filter { it.length > 2 }
                    if (words.any { rArtist.contains(it) }) score += 25.0
                }
                val rTitle = r.name?.lowercase() ?: ""
                val tTitle = cleanedTitle.lowercase()
                if (rTitle == tTitle) score += 80.0
                else if (rTitle.contains(tTitle) || tTitle.contains(rTitle)) score += 40.0
                if (score > 0) r to score else null
            }.sortedByDescending { it.second }

            for ((result, score) in scored.take(3)) {
                val id = result.trackId ?: continue
                Timber.d("Paxsenix trying Apple Music id=$id name=${result.name} score=$score")
                val lyricsResult = fetchAppleMusicLyrics(id)
                if (lyricsResult.isSuccess) {
                    val lrc = lyricsResult.getOrThrow()
                    val hasWordTimings = lrc.contains("<") && lrc.contains(">")
                    if (hasWordTimings) {
                        Timber.d("Paxsenix: word-level lyrics found via Apple Music")
                        return Result.success(lrc)
                    }
                }
            }
        }

        Timber.d("Paxsenix: Apple Music path failed, trying Musixmatch fallback")
        val mxmResult = fetchMusixmatchLyrics(title, artist, duration)
        if (mxmResult.isSuccess) {
            Timber.d("Paxsenix: Musixmatch word-level lyrics found")
            return Result.success(mxmResult.getOrThrow())
        }

        throw IllegalStateException("No tracks found on Paxsenix (tried Spotify->AppleMusic + Musixmatch)")
    }

    suspend fun getAllLyrics(
        title: String,
        artist: String,
        duration: Int,
        album: String? = null,
        callback: (String) -> Unit,
    ) {
        val cleanedTitle = cleanTitle(title)
        val cleanedArtist = cleanArtist(artist)
        val durationMs = duration * 1000

        val results = searchSpotify("$cleanedTitle $cleanedArtist")
        val scored = results.mapNotNull { r ->
            var score = 0.0
            r.duration_ms?.let { d ->
                val diff = abs(d - durationMs)
                score += when { diff <= 2000 -> 100.0; diff <= 5000 -> 50.0; else -> -50.0 }
            }
            val rArtist = r.artistName?.lowercase() ?: ""
            if (rArtist.contains(cleanedArtist.lowercase())) score += 50.0
            if (score > 0) r to score else null
        }.sortedByDescending { it.second }

        for ((result, _) in scored.take(3)) {
            val id = result.trackId ?: continue
            val lyricsResult = fetchAppleMusicLyrics(id)
            if (lyricsResult.isSuccess) {
                val lrc = lyricsResult.getOrThrow()
                if (lrc.isNotBlank()) { callback(lrc); return }
            }
        }

        val mxm = fetchMusixmatchLyrics(title, artist, duration)
        if (mxm.isSuccess) callback(mxm.getOrThrow())
    }

    private fun convertTTMLToAppFormat(ttml: String): String {
        return try {
            val lines = TTMLParser.parseTTML(ttml)
            if (lines.isEmpty()) return ""
            buildString {
                lines.forEach { line ->
                    if (line.words.isNotEmpty()) {
                        val min = line.startTime.toLong() / 60
                        val sec = line.startTime.toLong() % 60
                        val cs = ((line.startTime - line.startTime.toLong()) * 100).toLong()
                        val agent = when (line.agent) { "v2" -> "{agent:v2}"; else -> "{agent:v1}" }
                        val lineText = line.words.joinToString(" ") { it.text }
                        appendLine(String.format(Locale.US, "[%02d:%02d.%02d]%s%s", min, sec, cs, agent, lineText))
                        val words = line.words.joinToString("|") { "${it.text}:${it.startTime}:${it.endTime}" }
                        appendLine("<$words>")
                    } else if (line.text.isNotBlank()) {
                        val min = line.startTime.toLong() / 60
                        val sec = line.startTime.toLong() % 60
                        val cs = ((line.startTime - line.startTime.toLong()) * 100).toLong()
                        appendLine(String.format(Locale.US, "[%02d:%02d.%02d]%s", min, sec, cs, line.text))
                    }
                }
            }
        } catch (e: Exception) {
            Timber.w("Paxsenix: TTML conversion failed: ${e.message}")
            ""
        }
    }
}
