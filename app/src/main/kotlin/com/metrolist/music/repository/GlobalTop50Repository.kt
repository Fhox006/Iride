/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.repository

import com.metrolist.innertube.YouTube
import com.metrolist.innertube.models.SongItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GlobalTop50Repository @Inject constructor() {

    private val KWORB_URL = "https://kworb.net/spotify/country/global_daily.html"

    suspend fun fetchGlobalTop50(): List<Pair<String, String>> = withContext(Dispatchers.IO) {
        try {
            Timber.d("GlobalTop50: Fetching Kworb HTML...")
            val doc = Jsoup.connect(KWORB_URL)
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                .timeout(10000)
                .get()
            val rows = doc.select("table tbody tr")
            Timber.d("GlobalTop50: Found ${rows.size} rows")
            rows.take(50).mapNotNull { row ->
                val textCell = row.select("td.text a").firstOrNull()?.text()
                if (textCell != null && textCell.contains(" - ")) {
                    val parts = textCell.split(" - ", limit = 2)
                    Timber.d("GlobalTop50: Found track ${parts[0]} - ${parts[1]}")
                    parts[0].trim() to parts[1].trim()
                } else null
            }
        } catch (e: Exception) {
            Timber.e(e, "GlobalTop50: Error fetching Kworb")
            emptyList()
        }
    }

    suspend fun searchOnYouTubeMusic(artist: String, title: String): SongItem? {
        val query = "$artist - $title"
        Timber.d("GlobalTop50: Searching YT for $query")
        return YouTube.search(query, YouTube.SearchFilter.FILTER_SONG)
            .getOrNull()?.items?.filterIsInstance<SongItem>()?.firstOrNull()
    }

    suspend fun fetchAndSearchGlobalTop50(): List<SongItem> = coroutineScope {
        val tracks = fetchGlobalTop50()
        tracks.map { (artist, title) ->
            async(Dispatchers.IO) {
                searchOnYouTubeMusic(artist, title)
            }
        }.mapNotNull { it.await() }
    }
}
