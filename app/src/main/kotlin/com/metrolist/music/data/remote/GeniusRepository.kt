/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.data.remote

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import javax.inject.Inject
import javax.inject.Singleton

/** A Genius song credited to an artist as a featured (non-primary) contributor. */
data class GeniusFeaturedSong(
    val geniusId: Int,
    val title: String,
    val primaryArtistName: String,
)

/**
 * Genius indexes every song an artist is credited on, primary or featured — exactly what YTM's
 * own artist-page shelves don't reliably surface (a "feat." credit buried in someone else's album
 * often never shows up in the artist's own YTM page at all). Used by [com.metrolist.music.utils.NewReleaseNotifier]
 * to fill that gap: resolve the artist's Genius id by name, then list their songs and keep only
 * the ones where Genius' own primary_artist differs from them (their own releases are already
 * covered by the YTM shelf scan).
 *
 * Requires a user-supplied access token (Settings › AI & Lyrics › Genius API Token) — Genius issues
 * these per-app at genius.com/api-clients, there's no fixed key to ship in the app itself.
 */
@Singleton
class GeniusRepository @Inject constructor() {
    private val baseUrl = "https://api.genius.com"

    private fun requestJson(path: String, accessToken: String): JSONObject? {
        var connection: HttpURLConnection? = null
        return try {
            connection = URL("$baseUrl$path").openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.setRequestProperty("Authorization", "Bearer $accessToken")
            connection.setRequestProperty("Accept", "application/json")
            connection.connectTimeout = 8000
            connection.readTimeout = 8000
            if (connection.responseCode != HttpURLConnection.HTTP_OK) return null
            JSONObject(connection.inputStream.bufferedReader().use { it.readText() })
        } catch (e: Exception) {
            null
        } finally {
            connection?.disconnect()
        }
    }

    private fun resolveArtistId(artistName: String, accessToken: String): Int? {
        val encoded = URLEncoder.encode(artistName, "UTF-8")
        val hits = requestJson("/search?q=$encoded", accessToken)
            ?.optJSONObject("response")?.optJSONArray("hits") ?: return null

        for (i in 0 until hits.length()) {
            val primaryArtist = hits.optJSONObject(i)?.optJSONObject("result")?.optJSONObject("primary_artist")
                ?: continue
            if (primaryArtist.optString("name").equals(artistName, ignoreCase = true)) {
                return primaryArtist.optInt("id").takeIf { it != 0 }
            }
        }
        return null
    }

    /**
     * Songs where [artistName] is credited but not as Genius' own primary artist, newest-popularity
     * first. Returns empty on any failure (missing/invalid token, no network, no name match) —
     * this is a best-effort supplemental source, never a hard dependency for the caller.
     */
    suspend fun findFeaturedSongs(
        artistName: String,
        accessToken: String,
        maxPages: Int,
    ): List<GeniusFeaturedSong> = withContext(Dispatchers.IO) {
        if (accessToken.isBlank()) return@withContext emptyList()
        val artistId = resolveArtistId(artistName, accessToken) ?: return@withContext emptyList()

        val results = mutableListOf<GeniusFeaturedSong>()
        for (page in 1..maxPages) {
            val songs = requestJson(
                "/artists/$artistId/songs?per_page=50&page=$page&sort=popularity",
                accessToken,
            )?.optJSONObject("response")?.optJSONArray("songs") ?: break
            if (songs.length() == 0) break

            for (i in 0 until songs.length()) {
                val song = songs.optJSONObject(i) ?: continue
                val primaryArtist = song.optJSONObject("primary_artist") ?: continue
                if (primaryArtist.optInt("id") == artistId) continue
                results += GeniusFeaturedSong(
                    geniusId = song.optInt("id"),
                    title = song.optString("title"),
                    primaryArtistName = primaryArtist.optString("name"),
                )
            }
        }
        results
    }
}
