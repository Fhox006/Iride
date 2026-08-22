package com.metrolist.music.betterlyrics

import com.metrolist.music.betterlyrics.models.UnisonApiResponse
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

/**
 * Community-sourced word-by-word ("Unison") lyrics API, distinct from the
 * generic BetterLyrics aggregator (lyrics-api.boidu.dev). Endpoint and
 * response contract taken from better-lyrics/better-lyrics `unisonApi.ts`.
 */
object UnisonLyrics {
    private const val TAG = "UnisonLyrics"

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
                requestTimeoutMillis = 12000
                connectTimeoutMillis = 10000
                socketTimeoutMillis = 12000
            }

            defaultRequest {
                url("https://unison.boidu.dev")
                headers.append("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                headers.append("Accept", "application/json")
            }

            expectSuccess = false
        }
    }

    suspend fun getLyrics(
        title: String,
        artist: String,
        duration: Int,
        album: String? = null,
        videoId: String? = null,
    ) = runCatching {
        Timber.tag(TAG).d("Fetching Unison lyrics for: $title by $artist (dur=$duration, album=$album, videoId=$videoId)")
        val response = client.get("/lyrics") {
            parameter("v", videoId.orEmpty())
            parameter("song", title)
            parameter("artist", artist)
            if (duration > 0) parameter("duration", duration)
            if (!album.isNullOrBlank()) parameter("album", album)
        }

        if (response.status != HttpStatusCode.OK) {
            throw IllegalStateException("Unison API returned status: ${response.status}")
        }

        val body = response.body<UnisonApiResponse>()
        val data = body.data
        if (!body.success || data?.lyrics.isNullOrBlank() || data?.format == null) {
            throw IllegalStateException(body.error ?: "Lyrics unavailable")
        }

        when (data.format) {
            "ttml" -> {
                val parsedLines = TTMLParser.parseTTML(data.lyrics)
                if (parsedLines.isEmpty()) throw IllegalStateException("Failed to parse Unison TTML lyrics")
                TTMLParser.toLRC(parsedLines)
            }
            "lrc" -> data.lyrics
            "plain" -> data.lyrics
            else -> throw IllegalStateException("Unknown Unison lyrics format: ${data.format}")
        }
    }.onFailure { e ->
        Timber.tag(TAG).w(e, "Failed to fetch Unison lyrics")
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
            .onSuccess { lyrics -> callback(lyrics) }
    }
}
