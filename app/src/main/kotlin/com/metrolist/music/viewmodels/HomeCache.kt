package com.metrolist.music.viewmodels

import com.metrolist.innertube.pages.ExplorePage
import com.metrolist.innertube.pages.HomePage
import com.metrolist.music.db.entities.LocalItem
import com.metrolist.music.db.entities.Song
import com.metrolist.music.models.SimilarRecommendation

object HomeCache {
    var homePage: HomePage? = null
    var quickPicks: List<Song>? = null
    var keepListening: List<LocalItem>? = null
    var forgottenFavorites: List<Song>? = null
    var similarRecommendations: List<SimilarRecommendation>? = null
    var dailyDiscover: List<DailyDiscoverItem>? = null
    var communityPlaylists: List<CommunityPlaylistItem>? = null
    var explorePage: ExplorePage? = null
    var lastLoadedAt: Long = 0L
    private const val CACHE_TTL_MS = 15 * 60 * 1000L
    fun isStale() = System.currentTimeMillis() - lastLoadedAt > CACHE_TTL_MS
    fun clear() { 
        homePage = null
        quickPicks = null
        keepListening = null
        forgottenFavorites = null
        similarRecommendations = null
        dailyDiscover = null
        communityPlaylists = null
        explorePage = null
        lastLoadedAt = 0L 
    }
}
