package com.metrolist.music.betterlyrics

import com.metrolist.music.betterlyrics.models.TTMLResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import timber.log.Timber

object BetterLyrics {
    private const val TAG = "BetterLyrics"
    private val client by lazy {
        HttpClient(CIO) {
            install(ContentNegotiation) {
                json(
                    Json {
                        isLenient = true
                        ignoreUnknownKeys = true
                    },
                )
            }

            install(HttpTimeout) {
                requestTimeoutMillis = 15000
                connectTimeoutMillis = 10000
                socketTimeoutMillis = 15000
            }

            defaultRequest {
                url("https://lyrics-api.boidu.dev")
                headers {
                    append("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    append("Accept", "application/json")
                }
            }

            expectSuccess = false
        }
    }

    private suspend fun fetchTTML(
        artist: String,
        title: String,
        duration: Int = -1,
        album: String? = null,
        videoId: String? = null,
        endpoint: String = "/getLyrics",
    ): String? = runCatching {
        Timber.tag(TAG).d("Fetching TTML for: $title by $artist (dur=$duration, album=$album, videoId=$videoId, endpoint=$endpoint)")
        val response = client.get(endpoint) {
            parameter("s", title)
            parameter("a", artist)
            if (duration > 0) parameter("d", duration)
            if (!album.isNullOrBlank()) parameter("al", album)
            if (!videoId.isNullOrBlank()) parameter("videoId", videoId)
        }
        if (response.status == HttpStatusCode.OK) {
            response.body<TTMLResponse>().ttml?.trim()?.takeIf { it.isNotEmpty() }
        } else {
            Timber.tag(TAG).w("API returned status: ${response.status} on $endpoint")
            null
        }
    }.getOrElse { e ->
        Timber.tag(TAG).e(e, "Exception during fetchTTML")
        null
    }

    suspend fun getLyrics(
        title: String,
        artist: String,
        duration: Int,
        album: String? = null,
        videoId: String? = null,
    ) = runCatching {
        val ttml = fetchTTML(artist, title, duration, album, videoId, "/getLyrics")
            ?: fetchTTML(artist, title, duration, album, videoId, "/ttml/getLyrics")
            ?: throw IllegalStateException("Lyrics unavailable")

        val parsedLines = TTMLParser.parseTTML(ttml)
        if (parsedLines.isEmpty()) throw IllegalStateException("Failed to parse lyrics")
        TTMLParser.toLRC(parsedLines)
    }

    suspend fun getAllLyrics(
        title: String,
        artist: String,
        duration: Int,
        album: String? = null,
        videoId: String? = null,
        callback: (String) -> Unit,
    ) {
        getLyrics(title, artist, duration, album, videoId)
            .onSuccess { lrcString -> callback(lrcString) }
    }
}
