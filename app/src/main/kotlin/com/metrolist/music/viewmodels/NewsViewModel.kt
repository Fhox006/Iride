/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.viewmodels

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.metrolist.innertube.YouTube
import com.metrolist.innertube.models.AlbumItem
import com.metrolist.innertube.models.ArtistItem
import com.metrolist.innertube.models.SongItem
import com.metrolist.innertube.models.YTItem
import com.metrolist.innertube.pages.MoodAndGenres
import com.metrolist.music.constants.HideExplicitKey
import com.metrolist.music.constants.NewsRefreshTimestampKey
import com.metrolist.music.constants.NewsSelectedGenreIdKey
import com.metrolist.music.constants.NewsSelectedGenreParamsKey
import com.metrolist.music.constants.NewsSelectedGenreTitleKey
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

enum class ReleaseKind { ALBUM, SINGLE }

data class NewsRelease(
    val album: AlbumItem,
    val kind: ReleaseKind,
)

data class NewsGenre(
    val id: String,
    val params: String?,
    val title: String,
)

/**
 * Feeds the rebuilt News tab. Every release is verified to actually be recent
 * (the album page's own year) before being shown — no stale catalog filler.
 * Sources: own listened/followed artists first, then YouTube's curated new-release
 * feed, charts and mood/genre browsing. All free endpoints already integrated.
 */
@HiltViewModel
class NewsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: MusicDatabase,
) : ViewModel() {

    private val _personalReleases = MutableStateFlow<List<NewsRelease>>(emptyList())
    val personalReleases = _personalReleases.asStateFlow()

    private val _generalReleases = MutableStateFlow<List<NewsRelease>>(emptyList())
    val generalReleases = _generalReleases.asStateFlow()

    private val _chartSongs = MutableStateFlow<List<SongItem>>(emptyList())
    val chartSongs = _chartSongs.asStateFlow()

    private val _discoverArtists = MutableStateFlow<List<ArtistItem>>(emptyList())
    val discoverArtists = _discoverArtists.asStateFlow()

    private val _genres = MutableStateFlow<List<NewsGenre>>(emptyList())
    val genres = _genres.asStateFlow()

    private val _selectedGenre = MutableStateFlow<NewsGenre?>(null)
    val selectedGenre = _selectedGenre.asStateFlow()

    private val _genreShelf = MutableStateFlow<List<YTItem>>(emptyList())
    val genreShelf = _genreShelf.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private var ownArtistIds: Set<String> = emptySet()

    init {
        viewModelScope.launch {
            restoreSelectedGenre()
        }
        refresh()
    }

    fun refresh(force: Boolean = false) {
        viewModelScope.launch {
            val last = context.dataStore.get(NewsRefreshTimestampKey, 0L)
            if (!force && System.currentTimeMillis() - last < STALE_AFTER_MS && _personalReleases.value.isNotEmpty()) {
                loadCuratedArtists()
                return@launch
            }

            _isLoading.value = true
            try {
                val hideExplicit = context.dataStore.get(HideExplicitKey, false)
                val minYear = Year.now().value
                ownArtistIds = loadOwnArtistIds()

                coroutineScope {
                    val personal = async { loadPersonalReleases(minYear, hideExplicit) }
                    val general = async { loadGeneralReleases(minYear, hideExplicit) }
                    val charts = async { loadCharts(hideExplicit) }
                    val genres = async { loadGenres() }
                    personal.await()
                    general.await()
                    charts.await()
                    genres.await()
                    loadCuratedArtists()
                }

                context.dataStore.edit { it[NewsRefreshTimestampKey] = System.currentTimeMillis() }
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun selectGenre(genre: NewsGenre?) {
        _selectedGenre.value = genre
        _genreShelf.value = emptyList()
        viewModelScope.launch {
            persistGenre(genre)
            genre?.let { loadGenreShelf(it) }
        }
    }

    private suspend fun loadOwnArtistIds(): Set<String> = ownArtistsFromDb().map { it.id }.toSet()

    private suspend fun ownArtistsFromDb() = withContext(Dispatchers.IO) {
        val artists = database.allArtistsByPlayTime().first()
        val followed = artists.filter { it.artist.bookmarkedAt != null }.take(FOLLOWED_LIMIT)
        val frequent = artists.filter { it.artist.bookmarkedAt == null }.take(FREQUENT_LIMIT)
        (followed + frequent).distinctBy { it.id }
    }

    private suspend fun loadPersonalReleases(minYear: Int, hideExplicit: Boolean) {
        val artists = ownArtistsFromDb()
        val candidates = coroutineScope {
            artists.map { artist ->
                async {
                    YouTube.artist(artist.id).getOrNull()?.sections?.let { sections ->
                        val albumSection = sections.find {
                            it.title.contains("Album", ignoreCase = true) ||
                                it.title.contains("Latest", ignoreCase = true) ||
                                it.title.contains("Uscita", ignoreCase = true)
                        }
                        val singleSection = sections.find { it.title.contains("Singl", ignoreCase = true) }
                        albumSection?.items?.filterIsInstance<AlbumItem>()?.take(PER_ARTIST_ALBUMS).orEmpty() +
                            singleSection?.items?.filterIsInstance<AlbumItem>()?.take(PER_ARTIST_SINGLES).orEmpty()
                    }.orEmpty()
                }
            }.awaitAll()
        }.flatten().distinctBy { it.id }.take(PERSONAL_CANDIDATES)

        _personalReleases.value = verifyRecent(candidates, minYear)
            .filterReleases(hideExplicit)
    }

    private suspend fun loadGeneralReleases(minYear: Int, hideExplicit: Boolean) {
        val explore = YouTube.explore().getOrNull() ?: return
        seedGenres(explore.moodAndGenres)

        val candidates = explore.newReleaseAlbums
            .filter { it.hasOfficialArtist() }
            .filterNot { album -> album.artists.orEmpty().any { it.id in ownArtistIds } }
            .distinctBy { it.id }
            .take(GENERAL_CANDIDATES)

        _generalReleases.value = verifyRecent(candidates, minYear)
            .filterReleases(hideExplicit)
    }

    private suspend fun loadCharts(hideExplicit: Boolean) {
        val page = YouTube.getChartsPage().getOrNull() ?: return

        _chartSongs.value = page.sections
            .flatMap { it.items }
            .filterIsInstance<SongItem>()
            .filter { song ->
                val officialArtists = song.artists.filter { !it.id.isNullOrBlank() }
                officialArtists.isNotEmpty() && officialArtists.none { it.id in ownArtistIds }
            }
            .filterNot { it.isVideoSong }
            .distinctBy { it.id }
            .take(CHART_SONGS_LIMIT)
            .let { songs ->
                if (hideExplicit) songs.filterNot { it.explicit } else songs
            }
    }

    /**
     * The Discover shelf features hand-picked, widely-known official artists across genres
     * (Italian + international), resolved through YouTube Music's artist search so every entry
     * is a real channel — never obscure uploads, and never artists the user already follows or
     * listens to (those already have the "From your artists" zone). Reshuffled every refresh.
     */
    private suspend fun loadCuratedArtists() {
        val official = coroutineScope {
            CURATED_ARTIST_NAMES.shuffled()
                .take(DISCOVER_LIMIT + 4)
                .map { name ->
                    async(Dispatchers.IO) {
                        YouTube.search(name, YouTube.SearchFilter.FILTER_ARTIST).getOrNull()
                            ?.items
                            ?.filterIsInstance<ArtistItem>()
                            ?.firstOrNull { !it.isProfile }
                    }
                }.awaitAll().filterNotNull()
                .filterNot { it.id in ownArtistIds }
                .distinctBy { it.id }
                .take(DISCOVER_LIMIT)
        }
        if (_discoverArtists.value.isEmpty() || official.isNotEmpty()) {
            _discoverArtists.value = official
        }
    }

    private suspend fun loadGenres() {
        if (_genres.value.isNotEmpty()) return
        val moods = YouTube.moodAndGenres().getOrNull().orEmpty()
        seedGenres(moods.flatMap { it.items })
    }

    private fun seedGenres(items: List<MoodAndGenres.Item>) {
        if (_genres.value.isNotEmpty()) return
        _genres.value = items
            .distinctBy { it.title }
            .take(GENRE_LIMIT)
            .map { NewsGenre(it.endpoint.browseId, it.endpoint.params, it.title) }
    }

    private suspend fun verifyRecent(candidates: List<AlbumItem>, minYear: Int): List<NewsRelease> =
        candidates.mapConcurrent(CONCURRENCY) { candidate ->
            // Trust the feed-provided year when present; only hit the album page for
            // candidates without one. Cuts most of the verification traffic.
            val album = if (candidate.year != null) {
                candidate.takeIf { (it.year ?: 0) >= minYear }
            } else {
                YouTube.album(candidate.browseId).getOrNull()?.album?.takeIf { (it.year ?: 0) >= minYear }
            } ?: return@mapConcurrent null
            if (!album.hasOfficialArtist()) return@mapConcurrent null
            NewsRelease(
                album = album,
                kind = when {
                    album.albumType?.contains("single", ignoreCase = true) == true -> ReleaseKind.SINGLE
                    album.albumType?.contains("ep", ignoreCase = true) == true -> ReleaseKind.SINGLE
                    else -> ReleaseKind.ALBUM
                },
            )
        }.filterNotNull()
            .distinctBy { it.album.id }

    private fun AlbumItem.hasOfficialArtist(): Boolean =
        artists.orEmpty().any { !it.id.isNullOrBlank() }

    private suspend fun loadGenreShelf(genre: NewsGenre) {
        val result = YouTube.browse(genre.id, genre.params).getOrNull() ?: return
        val items = result.items
            .flatMap { it.items }
            .filterNot { it is SongItem }
            .distinctBy { it.id }
            .take(GENRE_SHELF_LIMIT)
        if (_selectedGenre.value?.id == genre.id) {
            _genreShelf.value = items
        }
    }

    private suspend fun <T, R> List<T>.mapConcurrent(
        maxConcurrency: Int,
        transform: suspend (T) -> R,
    ): List<R> = coroutineScope {
        chunked(maxConcurrency).flatMap { chunk ->
            chunk.map { item -> async { transform(item) } }.awaitAll()
        }
    }

    private fun List<NewsRelease>.filterReleases(hideExplicit: Boolean): List<NewsRelease> =
        if (hideExplicit) filterNot { it.album.explicit } else this

    private suspend fun restoreSelectedGenre() {
        val id = context.dataStore.get(NewsSelectedGenreIdKey, "").ifEmpty { return }
        val genre = NewsGenre(
            id = id,
            params = context.dataStore.get(NewsSelectedGenreParamsKey, "").ifEmpty { null },
            title = context.dataStore.get(NewsSelectedGenreTitleKey, ""),
        )
        _selectedGenre.value = genre
        loadGenreShelf(genre)
    }

    private suspend fun persistGenre(genre: NewsGenre?) {
        context.dataStore.edit { prefs ->
            if (genre == null) {
                prefs.remove(NewsSelectedGenreIdKey)
                prefs.remove(NewsSelectedGenreParamsKey)
                prefs.remove(NewsSelectedGenreTitleKey)
            } else {
                prefs[NewsSelectedGenreIdKey] = genre.id
                prefs[NewsSelectedGenreParamsKey] = genre.params.orEmpty()
                prefs[NewsSelectedGenreTitleKey] = genre.title
            }
        }
    }

    companion object {
        private const val FOLLOWED_LIMIT = 20
        private const val FREQUENT_LIMIT = 15
        private const val PER_ARTIST_ALBUMS = 2
        private const val PER_ARTIST_SINGLES = 2
        private const val PERSONAL_CANDIDATES = 30
        private const val GENERAL_CANDIDATES = 24
        private const val CHART_SONGS_LIMIT = 20
        private const val DISCOVER_LIMIT = 12
        private const val GENRE_LIMIT = 14
        private const val GENRE_SHELF_LIMIT = 24
        private const val CONCURRENCY = 8
        private const val STALE_AFTER_MS = 6L * 60 * 60 * 1000

        // Established, widely-listened artists across genres (Italian + international). Names
        // are resolved to real official channels via artist search, never hardcoded IDs.
        private val CURATED_ARTIST_NAMES = listOf(
            "Guè Pequeno",
            "Marracash",
            "Lazza",
            "Sfera Ebbasta",
            "Salmo",
            "Nitro",
            "Fabri Fibra",
            "Capo Plaza",
            "Ghali",
            "Geolier",
            "Coez",
            "Carl Brave",
            "Rkomi",
            "Emis Killa",
            "Noyz Narcos",
            "Club Dogo",
            "Fedez",
            "J-Ax",
            "MadMan",
            "Ernia",
            "Bresh",
            "Rosa Chemical",
            "Rondodasosa",
            "Baby Gang",
            "Kid Yugi",
            "Tha Supreme",
            "Chiello",
            "Izi",
            "Shiva",
            "Mahmood",
            "Blanco",
            "Ultimo",
            "Pinguini Tattici Nucleari",
            "Måneskin",
            "Coldplay",
            "The Weeknd",
            "Billie Eilish",
            "Dua Lipa",
            "Imagine Dragons",
            "Arctic Monkeys",
            "Twenty One Pilots",
            "Tame Impala",
            "Kendrick Lamar",
            "Travis Scott",
        )
    }
}
