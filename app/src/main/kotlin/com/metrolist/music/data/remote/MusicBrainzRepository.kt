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

@Singleton
class MusicBrainzRepository @Inject constructor() {
    private val userAgent = "Metrolist/1.0 (https://github.com/mostafaalagamy/Metrolist)"

    suspend fun getAlbumReleaseDate(
        albumTitle: String,
        artistName: String?,
        year: Int?
    ): String? = withContext(Dispatchers.IO) {
        try {
            // Search with exact phrase quotes.
            val query = buildString {
                append("release:\"${albumTitle.replace("\"", "\\\"")}\"")
                if (artistName != null) {
                    append(" AND artist:\"${artistName.replace("\"", "\\\"")}\"")
                }
            }
            
            val encodedQuery = URLEncoder.encode(query, "UTF-8")
            val url = URL("https://musicbrainz.org/ws/2/release-group?query=$encodedQuery&fmt=json")
            val response = makeGetRequest(url) ?: return@withContext null
            val releaseGroups = JSONObject(response).optJSONArray("release-groups") ?: return@withContext null

            // Strict filtering: find the first result that matches title AND artist AND year exactly
            var bestMbid: String? = null
            for (i in 0 until releaseGroups.length()) {
                val rg = releaseGroups.getJSONObject(i)
                val rgTitle = rg.optString("title")
                val rgDate = rg.optString("first-release-date")
                val rgMbid = rg.optString("id")
                
                // 1. Verify title matches exactly (ignoring case)
                if (!rgTitle.equals(albumTitle, ignoreCase = true)) continue
                
                // 2. Verify artist matches exactly
                val artistCredit = rg.optJSONArray("artist-credit")
                if (artistName != null && artistCredit != null && artistCredit.length() > 0) {
                    val firstArtist = artistCredit.getJSONObject(0).optJSONObject("artist")?.optString("name")
                    if (firstArtist != null && !firstArtist.equals(artistName, ignoreCase = true)) {
                        continue
                    }
                }

                // 3. Verify year matches
                if (year == null || rgDate.startsWith(year.toString())) {
                    bestMbid = rgMbid
                    break
                }
            }
            
            if (bestMbid == null) return@withContext null

            // Follow-up: get releases for this release-group to find the most complete date
            val releaseUrl = URL("https://musicbrainz.org/ws/2/release?release-group=$bestMbid&fmt=json")
            val releaseResponse = makeGetRequest(releaseUrl) ?: return@withContext null
            val releasesJson = JSONObject(releaseResponse)
            val releases = releasesJson.optJSONArray("releases") ?: return@withContext null

            var bestDate: String? = null

            for (i in 0 until releases.length()) {
                val release = releases.getJSONObject(i)
                val date = release.optString("date")
                if (date.isNotEmpty()) {
                    if (bestDate == null || isBetterDate(date, bestDate)) {
                        bestDate = date
                    }
                    if (date.length == 10) break
                }
            }

            bestDate
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun makeGetRequest(url: URL): String? {
        var connection: HttpURLConnection? = null
        return try {
            connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.setRequestProperty("User-Agent", userAgent)
            connection.setRequestProperty("Accept", "application/json")
            connection.connectTimeout = 5000
            connection.readTimeout = 5000

            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                connection.inputStream.bufferedReader().use { it.readText() }
            } else {
                null
            }
        } catch (e: Exception) {
            null
        } finally {
            connection?.disconnect()
        }
    }

    private fun isBetterDate(newDate: String, oldDate: String): Boolean {
        // Prefer YYYY-MM-DD > YYYY-MM > YYYY
        return newDate.length > oldDate.length
    }
}
