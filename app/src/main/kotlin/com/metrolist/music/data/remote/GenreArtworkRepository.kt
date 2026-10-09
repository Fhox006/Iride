package com.metrolist.music.data.remote

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
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class GenreArtworkRepository @Inject constructor() {
    private val requests = Semaphore(3)
    private val client = HttpClient(OkHttp) {
        install(HttpTimeout) {
            requestTimeoutMillis = 10_000
            connectTimeoutMillis = 5_000
        }
    }

    suspend fun appleCandidates(genre: String): List<GenreArtworkCandidate> = attempt {
        // This HTML search is independent of the throttled iTunes Search API.
        val url = URLBuilder("https://music.apple.com/us/search").apply {
            parameters.append("term", genre)
        }.buildString()
        get(url)?.let { GenreArtworkParser.appleSearch(genre, it) }
    }.orEmpty()

    suspend fun deezerCandidates(genre: String): List<GenreArtworkCandidate> = attempt {
        val url = URLBuilder("https://api.deezer.com/search/playlist").apply {
            parameters.append("q", genre)
            parameters.append("limit", "25")
        }.buildString()
        get(url)?.let { GenreArtworkParser.deezerSearch(genre, it) }
    }.orEmpty()

    suspend fun matchesGenre(genre: String, candidate: GenreArtworkCandidate): Boolean = attempt {
        if (!GenreArtworkParser.isPlaylistPage(candidate.pageUrl)) return@attempt false
        val html = get(candidate.pageUrl) ?: return@attempt false
        if (candidate.pageUrl.startsWith("https://music.apple.com/")) {
            GenreArtworkParser.applePlaylistMatches(genre, candidate, html)
        } else {
            GenreArtworkParser.deezerPlaylistMatches(genre, candidate, html)
        }
    } == true

    private suspend fun get(url: String): String? = requests.withPermit {
        val response = client.get(url) {
            header(HttpHeaders.UserAgent, "Iride/1.0 (https://github.com/Fhox006/Iride)")
            header(HttpHeaders.Accept, "application/json, text/html")
        }
        if (response.status != HttpStatusCode.OK ||
            (response.headers[HttpHeaders.ContentLength]?.toLongOrNull() ?: 0L) > 6_000_000) return@withPermit null
        response.bodyAsText().takeIf { it.length <= 6_000_000 }
    }

    private suspend fun <T> attempt(block: suspend () -> T): T? = try {
        block()
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (_: Exception) {
        null
    }
}
