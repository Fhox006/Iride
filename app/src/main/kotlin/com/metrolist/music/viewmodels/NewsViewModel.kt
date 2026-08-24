/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.metrolist.innertube.YouTube
import com.metrolist.innertube.models.AlbumItem
import com.metrolist.innertube.models.SongItem
import com.metrolist.innertube.models.filterExplicit
import com.metrolist.innertube.pages.AlbumPage
import com.metrolist.music.constants.HideExplicitKey
import com.metrolist.music.db.MusicDatabase
import com.metrolist.music.utils.dataStore
import com.metrolist.music.utils.get
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Year
import javax.inject.Inject

/**
 * Feeds the News tab. Sources are picked so mainstream/known artists dominate, and
 * every album/song is verified to actually be recent (via [AlbumPage.album]'s year) before
 * it's shown — no all-time-popular-catalog filler.
 *
 * Priority: own listened/followed artists first, then YouTube's curated "new release albums"
 * feed (popularity-ranked by construction). Deliberately avoids YouTube's personalized
 * recommendation feed and the "Top" charts, which is what surfaces low-listener niche
 * tracks (e.g. phonk) or old catalog hits mixed in with real new hits.
 */
@HiltViewModel
class NewsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: MusicDatabase,
) : ViewModel() {

    private val _newAlbums = MutableStateFlow<List<AlbumItem>>(emptyList())
    val newAlbums = _newAlbums.asStateFlow()

    private val _quickPlaySongs = MutableStateFlow<List<SongItem>>(emptyList())
    val quickPlaySongs = _quickPlaySongs.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _isLoading.value = true
            val hideExplicit = context.dataStore.get(HideExplicitKey, false)
            val minYear = Year.now().value - 1

            val ownArtists = withContext(Dispatchers.IO) {
                val artists = database.allArtistsByPlayTime().first()
                val followed = artists.filter { it.artist.bookmarkedAt != null }.take(20)
                val frequent = artists.filter { it.artist.bookmarkedAt == null }.take(15)
                (followed + frequent).distinctBy { it.id }
            }
            val ownArtistIds = ownArtists.map { it.id }.toSet()

            val ownAlbumCandidates = coroutineScope {
                ownArtists.map { artist ->
                    async {
                        YouTube.artist(artist.id).getOrNull()?.sections?.find {
                            it.title.contains("Album", ignoreCase = true) ||
                                it.title.contains("Singl", ignoreCase = true) ||
                                it.title.contains("Latest", ignoreCase = true) ||
                                it.title.contains("Uscita", ignoreCase = true)
                        }?.items?.filterIsInstance<AlbumItem>()?.firstOrNull()
                    }
                }.awaitAll()
            }.filterNotNull().distinctBy { it.id }

            val exploreAlbums = YouTube.explore().getOrNull()?.newReleaseAlbums.orEmpty()
            val exploreCandidates = exploreAlbums.filterNot { album ->
                album.artists.orEmpty().any { it.id in ownArtistIds }
            }.take(MAINSTREAM_ALBUM_CANDIDATES)

            val ownResolved = resolveRecentAlbums(ownAlbumCandidates, minYear)
            val mainstreamResolved = resolveRecentAlbums(exploreCandidates, minYear)

            _newAlbums.value = (ownResolved.map { it.album } + mainstreamResolved.map { it.album })
                .distinctBy { it.id }
                .filterExplicit(hideExplicit)
                .take(ALBUM_LIMIT)

            _quickPlaySongs.value = (ownResolved.mapNotNull { it.songs.firstOrNull() } +
                mainstreamResolved.mapNotNull { it.songs.firstOrNull() })
                .distinctBy { it.id }
                .filterExplicit(hideExplicit)
                .take(SONG_LIMIT)

            _isLoading.value = false
        }
    }

    private suspend fun resolveRecentAlbums(candidates: List<AlbumItem>, minYear: Int): List<AlbumPage> = coroutineScope {
        candidates.map { candidate ->
            async { YouTube.album(candidate.browseId).getOrNull() }
        }.awaitAll()
    }.filterNotNull().filter { page -> (page.album.year ?: 0) >= minYear }

    companion object {
        private const val MAINSTREAM_ALBUM_CANDIDATES = 20
        private const val ALBUM_LIMIT = 24
        private const val SONG_LIMIT = 30
    }
}
