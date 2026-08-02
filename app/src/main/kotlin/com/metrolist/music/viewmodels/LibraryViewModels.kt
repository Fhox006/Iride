/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

@file:OptIn(ExperimentalCoroutinesApi::class, kotlinx.coroutines.FlowPreview::class)

package com.metrolist.music.viewmodels

import android.content.Context
import timber.log.Timber
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.metrolist.innertube.YouTube
import com.metrolist.innertube.models.ArtistItem
import com.metrolist.innertube.utils.completed
import com.metrolist.music.constants.AlbumSortDescendingKey
import com.metrolist.music.constants.AlbumSortType
import com.metrolist.music.constants.AlbumSortTypeKey
import com.metrolist.music.constants.ArtistFilter
import com.metrolist.music.constants.ArtistFilterKey
import com.metrolist.music.constants.ArtistSongSortDescendingKey
import com.metrolist.music.constants.ArtistSongSortType
import com.metrolist.music.constants.ArtistSongSortTypeKey
import com.metrolist.music.constants.ArtistSortDescendingKey
import com.metrolist.music.constants.ArtistSortType
import com.metrolist.music.constants.ArtistSortTypeKey
import com.metrolist.music.constants.HideExplicitKey
import com.metrolist.music.constants.HideVideoSongsKey
import com.metrolist.music.constants.HideYoutubeShortsKey
import com.metrolist.music.constants.LibraryFilter
import com.metrolist.music.constants.PlaylistSortDescendingKey
import com.metrolist.music.constants.PlaylistSortType
import com.metrolist.music.constants.PlaylistSortTypeKey
import com.metrolist.music.constants.SongFilter
import com.metrolist.music.constants.SongFilterKey
import com.metrolist.music.constants.SongSortDescendingKey
import com.metrolist.music.constants.SongSortType
import com.metrolist.music.constants.SongSortTypeKey
import com.metrolist.music.constants.DismissedListenedAlbumsKey
import com.metrolist.music.constants.DismissedContinueListeningAlbumsKey
import com.metrolist.music.constants.DismissedSuggestedFollowArtistsKey
import com.metrolist.music.constants.RecentlySuggestedAlbumsKey
import com.metrolist.music.constants.TopSize
import com.metrolist.music.db.MusicDatabase
import com.metrolist.music.db.entities.Album
import com.metrolist.music.db.entities.GlobalAlbumPlayEvent
import com.metrolist.music.discovery.AlbumRecommendationsGenerator
import com.metrolist.music.extensions.filterExplicit
import com.metrolist.music.extensions.filterExplicitAlbums
import com.metrolist.music.extensions.filterVideoSongs
import com.metrolist.music.extensions.filterYoutubeShorts
import com.metrolist.music.extensions.matchesNormalizedQuery
import com.metrolist.music.extensions.normalizeForSearch
import com.metrolist.music.extensions.toEnum
import com.metrolist.music.models.DischiPerTeItem
import com.metrolist.music.playback.DownloadUtil
import com.metrolist.music.utils.NewReleaseNotifier
import com.metrolist.music.utils.PodcastRefreshTrigger
import com.metrolist.music.utils.SyncUtils
import com.metrolist.music.utils.dataStore
import com.metrolist.music.utils.get
import com.metrolist.music.utils.reportException
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.LocalDateTime
import java.time.ZoneOffset
import javax.inject.Inject

@HiltViewModel
class LibrarySongsViewModel
@Inject
constructor(
    @ApplicationContext context: Context,
    database: MusicDatabase,
    downloadUtil: DownloadUtil,
    private val syncUtils: SyncUtils,
) : ViewModel() {
    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()
    val debouncedSearchQuery = _searchQuery
        .debounce(300)
        .stateIn(viewModelScope, SharingStarted.Lazily, "")

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    val allSongs =
        context.dataStore.data
            .map {
                Triple(
                    Triple(
                        it[SongFilterKey].toEnum(SongFilter.LIKED),
                        it[SongSortTypeKey].toEnum(SongSortType.CREATE_DATE),
                        (it[SongSortDescendingKey] ?: true),
                    ),
                    it[HideExplicitKey] ?: false,
                    it[HideVideoSongsKey] ?: false
                )
            }.distinctUntilChanged()
            .flatMapLatest { (filterSort, hideExplicit, hideVideoSongs) ->
                val (filter, sortType, descending) = filterSort
                when (filter) {
                    SongFilter.LIBRARY -> database.songs(sortType, descending).map { it.filterExplicit(hideExplicit).filterVideoSongs(hideVideoSongs) }
                    SongFilter.LIKED -> database.likedSongs(sortType, descending).map { it.filterExplicit(hideExplicit).filterVideoSongs(hideVideoSongs) }
                    SongFilter.DOWNLOADED -> database.downloadedSongs(sortType, descending).map { it.filterExplicit(hideExplicit).filterVideoSongs(hideVideoSongs) }
                    SongFilter.UPLOADED -> database.uploadedSongs(sortType, descending).map { it.filterExplicit(hideExplicit).filterVideoSongs(hideVideoSongs) }
                }
            }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val downloadedSongs =
        context.dataStore.data
            .map {
                Triple(
                    it[SongSortTypeKey].toEnum(SongSortType.CREATE_DATE),
                    it[SongSortDescendingKey] ?: true,
                    it[HideExplicitKey] ?: false,
                )
            }.distinctUntilChanged()
            .flatMapLatest { (sortType, descending, hideExplicit) ->
                database.downloadedSongs(sortType, descending).map { it.filterExplicit(hideExplicit) }
            }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun syncLikedSongs() {
        viewModelScope.launch(Dispatchers.IO) { syncUtils.syncLikedSongs() }
    }

    fun syncLibrarySongs() {
        viewModelScope.launch(Dispatchers.IO) { syncUtils.syncLibrarySongs() }
    }

    fun syncUploadedSongs() {
        viewModelScope.launch(Dispatchers.IO) { syncUtils.syncUploadedSongs() }
    }
}

private const val SUGGESTED_FOLLOW_MIN_PLAYS = 10

@HiltViewModel
class LibraryArtistsViewModel
@Inject
constructor(
    @ApplicationContext private val context: Context,
    private val database: MusicDatabase,
    private val syncUtils: SyncUtils,
    private val newReleaseNotifier: NewReleaseNotifier,
) : ViewModel() {
    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()
    val debouncedSearchQuery = _searchQuery
        .debounce(300)
        .stateIn(viewModelScope, SharingStarted.Lazily, "")

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    // New songs from followed artists: artistId -> count, and the library-title total.
    val newSongCounts = newReleaseNotifier.counts
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyMap())
    val totalNewSongs = newSongCounts
        .map { counts -> counts.values.sum() }
        .stateIn(viewModelScope, SharingStarted.Lazily, 0)

    // "You play them a lot but forgot to follow" — analog of the Albums screen's recently-listened
    // row. Frequently-played YouTube artists not yet bookmarked and not dismissed from this row.
    private val dismissedSuggestedFollowIds = context.dataStore.data
        .map { prefs ->
            prefs[DismissedSuggestedFollowArtistsKey]?.let { json ->
                runCatching { Json.decodeFromString<List<String>>(json) }.getOrNull()
            }?.toSet().orEmpty()
        }

    val suggestedFollowArtists = combine(
        database.mostPlayedArtists(fromTimeStamp = 0L, limit = 50),
        // Bookmarked-name/channelId lookup guards against a followed artist appearing here under
        // a second, duplicate db row (a different YTM channel/browse id for the same real-world
        // artist) — filtering by id alone (bookmarkedAt on THIS row) misses that case. channelId
        // is the stable YT identifier so it's checked first; name is a trimmed fallback for rows
        // without one (e.g. privately-owned artists).
        database.artistsBookmarked(ArtistSortType.CREATE_DATE, true)
            .map { bookmarked ->
                bookmarked.map { it.artist.name.trim().lowercase() }.toSet() to
                    bookmarked.mapNotNull { it.artist.channelId }.toSet()
            },
        dismissedSuggestedFollowIds,
    ) { played, (bookmarkedNames, bookmarkedChannelIds), dismissed ->
        played.filter {
            it.artist.bookmarkedAt == null &&
                !it.artist.isLocal &&
                it.artist.isYouTubeArtist &&
                it.songCount >= SUGGESTED_FOLLOW_MIN_PLAYS &&
                it.id !in dismissed &&
                it.artist.channelId !in bookmarkedChannelIds &&
                it.artist.name.trim().lowercase() !in bookmarkedNames
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun dismissSuggestedFollowArtist(artistId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            context.dataStore.edit { prefs ->
                val existing = prefs[DismissedSuggestedFollowArtistsKey]?.let { json ->
                    runCatching { Json.decodeFromString<List<String>>(json) }.getOrNull()
                }.orEmpty()
                prefs[DismissedSuggestedFollowArtistsKey] =
                    Json.encodeToString((existing.toSet() + artistId).toList())
            }
        }
    }

    // Quick-follow from the suggested row: bookmark right away, no detour through the artist page.
    fun followSuggestedArtist(artistId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            database.artist(artistId).first()?.let { artist ->
                database.query {
                    update(artist.artist.toggleLike())
                }
            }
        }
    }

    val allArtists =
        context.dataStore.data
            .map {
                Triple(
                    it[ArtistFilterKey].toEnum(ArtistFilter.LIKED),
                    it[ArtistSortTypeKey].toEnum(ArtistSortType.CREATE_DATE),
                    it[ArtistSortDescendingKey] ?: true,
                )
            }.distinctUntilChanged()
            .flatMapLatest { (filter, sortType, descending) ->
                when (filter) {
                    ArtistFilter.LIKED -> database.artistsBookmarked(sortType, descending)
                    ArtistFilter.LIBRARY -> database.artists(sortType, descending)
                }
            }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // Followed artists with an unseen release, newest activity first. The screen's lead section:
    // "what happened since you last looked", not another list to browse.
    val newReleaseArtists =
        combine(allArtists, newSongCounts) { artists, counts ->
            artists
                .filter { (counts[it.id] ?: 0) > 0 }
                .sortedByDescending { counts[it.id] ?: 0 }
        }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val filteredArtists =
        combine(allArtists, searchQuery) { artists, query ->
            val normalizedQuery = query.normalizeForSearch()
            artists
                .filter { artist ->
                    matchesNormalizedQuery(normalizedQuery, artist.artist.name)
                }
                .distinctBy { it.id }
        }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun sync() {
        viewModelScope.launch(Dispatchers.IO) { syncUtils.syncArtistsSubscriptions() }
    }

    init {
        // Check followed artists for new releases (throttled inside the notifier).
        viewModelScope.launch(Dispatchers.IO) {
            val followedIds = database.artistsBookmarked(ArtistSortType.CREATE_DATE, true)
                .first()
                .filter { it.artist.isYouTubeArtist && !it.artist.isPodcastChannel }
                .map { it.id }
            newReleaseNotifier.refresh(followedIds)
        }
        viewModelScope.launch(Dispatchers.IO) {
            allArtists.collect { artists ->
                artists
                    .map { it.artist }
                    .filter {
                        it.thumbnailUrl == null || Duration.between(
                            it.lastUpdateTime,
                            LocalDateTime.now()
                        ) > Duration.ofDays(10)
                    }.forEach { artist ->
                        YouTube.artist(artist.id).onSuccess { artistPage ->
                            database.query {
                                update(artist, artistPage)
                            }
                        }
                    }
            }
        }
    }
}

@Serializable
private data class TimestampedAlbumId(val id: String, val timestamp: Long)

private const val CONTINUE_LISTENING_MIN_STREAK = 3

// Walks `events` (newest first, one row per song play joined to its album) and returns, for each
// album with a run of >= CONTINUE_LISTENING_MIN_STREAK consecutive same-album plays, that run's
// most recent timestamp. Only the latest run per album is kept — older runs are superseded, which
// is also what lets a dismissed album quietly reappear: a fresh streak produces a newer timestamp
// than the dismiss time, an unrepeated old streak doesn't.
private fun deriveContinueListeningCandidates(events: List<GlobalAlbumPlayEvent>): Map<String, LocalDateTime> {
    val result = linkedMapOf<String, LocalDateTime>()
    var i = 0
    while (i < events.size) {
        val albumId = events[i].albumId
        var j = i
        while (j < events.size && events[j].albumId == albumId) j++
        if (j - i >= CONTINUE_LISTENING_MIN_STREAK && albumId !in result) {
            result[albumId] = events[i].timestamp
        }
        i = j
    }
    return result
}

@HiltViewModel
class LibraryAlbumsViewModel
@Inject
constructor(
    @ApplicationContext private val context: Context,
    database: MusicDatabase,
    private val syncUtils: SyncUtils,
) : ViewModel() {
    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()
    val debouncedSearchQuery = _searchQuery
        .debounce(300)
        .stateIn(viewModelScope, SharingStarted.Lazily, "")

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    val allAlbums =
        context.dataStore.data
            .map {
                Triple(
                    it[AlbumSortTypeKey].toEnum(AlbumSortType.CREATE_DATE),
                    it[AlbumSortDescendingKey] ?: true,
                    it[HideExplicitKey] ?: false,
                )
            }.distinctUntilChanged()
            .flatMapLatest { (sortType, descending, hideExplicit) ->
                database.albumsLiked(sortType, descending).map { it.filterExplicitAlbums(hideExplicit) }
            }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val downloadedAlbums = database.albumsDownloadedByDateDesc()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun sync() {
        viewModelScope.launch(Dispatchers.IO) { syncUtils.syncLikedAlbums() }
    }

    // "Recommended Albums" — discovery carousel, generated lazily and cached until the user asks
    // for a fresh batch (mirrors HomeViewModel's dischi-per-te regenerate flow). Backed by
    // LibraryAlbumsCache (process-scoped) rather than just this ViewModel's own field, so
    // navigating away from and back to the Albums screen doesn't lose/regenerate the list — only
    // a full app restart does.
    private val albumRecommendationsGenerator = AlbumRecommendationsGenerator(database)
    val recommendedAlbums = MutableStateFlow(LibraryAlbumsCache.recommendedAlbums)
    val isRegeneratingRecommendedAlbums = MutableStateFlow(false)

    fun loadRecommendedAlbums() {
        if (recommendedAlbums.value != null) return
        viewModelScope.launch(Dispatchers.IO) {
            isRegeneratingRecommendedAlbums.value = true
            try {
                generateRecommendedAlbums()
            } finally {
                isRegeneratingRecommendedAlbums.value = false
            }
        }
    }

    fun regenerateRecommendedAlbums() {
        viewModelScope.launch(Dispatchers.IO) {
            isRegeneratingRecommendedAlbums.value = true
            try {
                generateRecommendedAlbums()
            } finally {
                isRegeneratingRecommendedAlbums.value = false
            }
        }
    }

    private val recommendationCooldownMs = Duration.ofDays(3).toMillis()

    private suspend fun generateRecommendedAlbums() {
        val hideExplicit = context.dataStore.get(HideExplicitKey, false)
        val explorePage = HomeCache.explorePage ?: YouTube.explore().getOrNull()?.also {
            HomeCache.explorePage = it
        }
        val cutoff = System.currentTimeMillis() - recommendationCooldownMs
        val recentlySuggestedIds = context.dataStore.data.first()[RecentlySuggestedAlbumsKey]?.let { json ->
            runCatching { Json.decodeFromString<List<TimestampedAlbumId>>(json) }.getOrNull()
        }.orEmpty().filter { it.timestamp >= cutoff }.map { it.id }.toSet()

        val generated = albumRecommendationsGenerator.generateForLibrary(
            explorePage = explorePage,
            hideExplicit = hideExplicit,
            seed = System.currentTimeMillis(),
            excludedAlbumIds = allAlbums.value.map { it.id }.toSet(),
            recentlySuggestedIds = recentlySuggestedIds,
        )
        recommendedAlbums.value = generated
        LibraryAlbumsCache.recommendedAlbums = generated
        recordSuggestedAlbums(generated.map { it.id })
    }

    private suspend fun recordSuggestedAlbums(ids: List<String>) {
        if (ids.isEmpty()) return
        val now = System.currentTimeMillis()
        // Prune entries well past the cooldown so this pref doesn't grow forever.
        val pruneCutoff = now - recommendationCooldownMs * 3
        context.dataStore.edit { prefs ->
            val existing = prefs[RecentlySuggestedAlbumsKey]?.let { json ->
                runCatching { Json.decodeFromString<List<TimestampedAlbumId>>(json) }.getOrNull()
            }.orEmpty().filter { it.timestamp >= pruneCutoff }
            val merged = (existing + ids.map { TimestampedAlbumId(it, now) })
                .groupBy { it.id }
                .map { (_, entries) -> entries.maxByOrNull { it.timestamp }!! }
            prefs[RecentlySuggestedAlbumsKey] = Json.encodeToString(merged)
        }
    }

    // "Continue Listening" — albums with >= 3 consecutive plays (anywhere in the library, not
    // just on the album's own page) that the user hasn't favorited yet. Removing one from this
    // list only hides it here until a newer streak forms; favorites and play history are
    // untouched.
    private val dismissedContinueListeningAlbums = context.dataStore.data
        .map { prefs ->
            prefs[DismissedContinueListeningAlbumsKey]?.let { json ->
                runCatching { Json.decodeFromString<List<TimestampedAlbumId>>(json) }.getOrNull()
            }.orEmpty().associate { it.id to it.timestamp }
        }

    val continueListeningAlbums: StateFlow<List<Album>> = combine(
        database.recentGlobalAlbumPlayEvents(300),
        allAlbums,
        dismissedContinueListeningAlbums,
    ) { events, saved, dismissed ->
        val savedIds = saved.map { it.id }.toSet()
        deriveContinueListeningCandidates(events)
            .filterKeys { it !in savedIds }
            .filter { (id, timestamp) ->
                val dismissedAt = dismissed[id] ?: return@filter true
                timestamp.toInstant(ZoneOffset.UTC).toEpochMilli() > dismissedAt
            }
    }.flatMapLatest { candidates ->
        if (candidates.isEmpty()) {
            flowOf(emptyList<Album>())
        } else {
            database.albumsByIds(candidates.keys.toList()).map { albums ->
                albums.sortedByDescending { candidates[it.id] }
            }
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun dismissContinueListeningAlbum(albumId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val now = System.currentTimeMillis()
            context.dataStore.edit { prefs ->
                val existing = prefs[DismissedContinueListeningAlbumsKey]?.let { json ->
                    runCatching { Json.decodeFromString<List<TimestampedAlbumId>>(json) }.getOrNull()
                }.orEmpty().filterNot { it.id == albumId }
                prefs[DismissedContinueListeningAlbumsKey] = Json.encodeToString(existing + TimestampedAlbumId(albumId, now))
            }
        }
    }

    // "Album ascoltati" — albums with >=2 songs played that aren't saved to the library yet,
    // i.e. candidates the user might want to add. "Clear" only hides ids from this list (stored
    // below), it never touches the real `event` play-history rows.
    private val dismissedListenedAlbumIds = context.dataStore.data
        .map { prefs ->
            prefs[DismissedListenedAlbumsKey]?.let { json ->
                runCatching { Json.decodeFromString<List<String>>(json) }.getOrNull()
            }?.toSet().orEmpty()
        }

    val recentlyListenedAlbums = combine(
        database.mostPlayedAlbums(fromTimeStamp = 0L, limit = 50),
        allAlbums,
        dismissedListenedAlbumIds,
    ) { played, saved, dismissed ->
        val savedIds = saved.map { it.id }.toSet()
        played.filter { (it.songCountListened ?: 0) >= 2 && it.id !in savedIds && it.id !in dismissed }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun clearRecentlyListened() {
        viewModelScope.launch(Dispatchers.IO) {
            val currentIds = recentlyListenedAlbums.value.map { it.id }.toSet()
            if (currentIds.isEmpty()) return@launch
            context.dataStore.edit { prefs ->
                val existing = prefs[DismissedListenedAlbumsKey]?.let { json ->
                    runCatching { Json.decodeFromString<List<String>>(json) }.getOrNull()
                }.orEmpty()
                prefs[DismissedListenedAlbumsKey] = Json.encodeToString((existing.toSet() + currentIds).toList())
            }
        }
    }

    init {
        viewModelScope.launch(Dispatchers.IO) {
            allAlbums.collect { albums ->
                albums
                    .filter {
                        it.album.songCount == 0
                    }.forEach { album ->
                        YouTube
                            .album(album.id)
                            .onSuccess { albumPage ->
                                database.query {
                                    update(album.album, albumPage, album.artists)
                                }
                            }.onFailure {
                                reportException(it)
                                if (it.message?.contains("NOT_FOUND") == true) {
                                    database.query {
                                        delete(album.album)
                                    }
                                }
                            }
                    }
            }
        }
    }
}

@HiltViewModel
class LibraryPlaylistsViewModel
@Inject
constructor(
    @ApplicationContext context: Context,
    database: MusicDatabase,
    private val syncUtils: SyncUtils,
) : ViewModel() {
    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()
    val debouncedSearchQuery = _searchQuery
        .debounce(300)
        .stateIn(viewModelScope, SharingStarted.Lazily, "")

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    val allPlaylists =
        context.dataStore.data
            .map {
                Triple(
                    it[PlaylistSortTypeKey].toEnum(PlaylistSortType.CREATE_DATE),
                    it[PlaylistSortDescendingKey] ?: true,
                    it[HideYoutubeShortsKey] ?: false
                )
            }.distinctUntilChanged()
            .flatMapLatest { (sortType, descending, hideYoutubeShorts) ->
                database.playlists(sortType, descending).map { it.filterYoutubeShorts(hideYoutubeShorts) }
            }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val downloadedPlaylistIds = database.playlistIdsWithDownloadedSongs()
        .map { it.toSet() }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptySet())

    val lastLikedThumbnails = database.lastLikedSongThumbnails()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun sync() {
        viewModelScope.launch(Dispatchers.IO) { syncUtils.syncSavedPlaylists() }
    }

    val topValue =
        context.dataStore.data
            .map { it[TopSize] ?: "50" }
            .distinctUntilChanged()
}

@HiltViewModel
class ArtistSongsViewModel
@Inject
constructor(
    @ApplicationContext context: Context,
    database: MusicDatabase,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val artistId = savedStateHandle.get<String>("artistId")!!
    val artist =
        database
            .artist(artistId)
            .stateIn(viewModelScope, SharingStarted.Lazily, null)

    val songs =
        context.dataStore.data
            .map {
                Triple(
                    it[ArtistSongSortTypeKey].toEnum(ArtistSongSortType.CREATE_DATE) to (it[ArtistSongSortDescendingKey]
                        ?: true),
                    it[HideExplicitKey] ?: false,
                    it[HideVideoSongsKey] ?: false
                )
            }.distinctUntilChanged()
            .flatMapLatest { (sortDesc, hideExplicit, hideVideoSongs) ->
                val (sortType, descending) = sortDesc
                database.artistSongs(artistId, sortType, descending).map { it.filterExplicit(hideExplicit).filterVideoSongs(hideVideoSongs) }
            }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
}

@HiltViewModel
class LibraryMixViewModel
@Inject
constructor(
    @ApplicationContext context: Context,
    database: MusicDatabase,
    private val syncUtils: SyncUtils,
) : ViewModel() {
    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing = _isRefreshing.asStateFlow()

    val isLibraryMode: MutableState<Boolean> = mutableStateOf(true)

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()
    val debouncedSearchQuery = _searchQuery
        .debounce(300)
        .stateIn(viewModelScope, SharingStarted.Lazily, "")

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    val syncAllLibrary = {
         viewModelScope.launch(Dispatchers.IO) {
             syncUtils.tryAutoSync()
         }
    }

    fun refresh() {
        viewModelScope.launch(Dispatchers.IO) {
            _isRefreshing.value = true
            try {
                YouTube.evictConnections()
                syncUtils.resetState()
                syncUtils.reInjectCredentials()
                syncUtils.performFullSyncSuspend()
                // Process-scoped caches that a real force-stop would wipe but a plain resync
                // doesn't touch — without this the button looked "refreshing" while every
                // screen kept serving the same stale in-memory data.
                HomeCache.clear()
                LibraryAlbumsCache.clear()
            } catch (e: Exception) {
                Timber.e(e, "Library refresh failed")
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    val topValue =
        context.dataStore.data
            .map { it[TopSize] ?: "50" }
            .distinctUntilChanged()
    var artists =
        database
            .artistsBookmarked(
                ArtistSortType.CREATE_DATE,
                true,
            ).stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    var albums = context.dataStore.data
        .map { it[HideExplicitKey] ?: false }
        .distinctUntilChanged()
        .flatMapLatest { hideExplicit ->
            database.albumsLiked(AlbumSortType.CREATE_DATE, true).map { it.filterExplicitAlbums(hideExplicit) }
        }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    var songs = context.dataStore.data
        .map { Triple(it[HideExplicitKey] ?: false, it[HideVideoSongsKey] ?: false, it[HideYoutubeShortsKey] ?: false) }
        .distinctUntilChanged()
        .flatMapLatest { (hideExplicit, hideVideoSongs, _) ->
            combine(
                database.songs(SongSortType.CREATE_DATE, true),
                database.songsInBookmarkedPlaylists()
            ) { librarySongs, playlistSongs ->
                (librarySongs + playlistSongs)
                    .distinctBy { it.id }
                    .filterExplicit(hideExplicit)
                    .filterVideoSongs(hideVideoSongs)
            }
        }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    var uploadedSongs = database
        .uploadedSongs(SongSortType.CREATE_DATE, true)
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    var downloadedAlbums = database
        .albumsDownloadedByDateDesc()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    var downloadedLooseSongs = database
        .downloadedSongsNotInFullAlbum()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    val downloadedPlaylistIds = database.playlistIdsWithDownloadedSongs()
        .map { it.toSet() }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptySet())
    var playlists = context.dataStore.data
        .map { it[HideYoutubeShortsKey] ?: false }
        .distinctUntilChanged()
        .flatMapLatest { hideYoutubeShorts ->
            database.playlists(PlaylistSortType.CREATE_DATE, true).map { it.filterYoutubeShorts(hideYoutubeShorts) }
        }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val lastLikedDate = database.lastLikedSongDate()
        .stateIn(viewModelScope, SharingStarted.Lazily, null)
    val lastLikedThumbnails = database.lastLikedSongThumbnails()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    init {
        viewModelScope.launch(Dispatchers.IO) {
            albums.collect { albums ->
                albums
                    .filter {
                        it.album.songCount == 0
                    }.forEach { album ->
                        YouTube
                            .album(album.id)
                            .onSuccess { albumPage ->
                                database.query {
                                    update(album.album, albumPage, album.artists)
                                }
                            }.onFailure {
                                reportException(it)
                                if (it.message?.contains("NOT_FOUND") == true) {
                                    database.query {
                                        delete(album.album)
                                    }
                                }
                            }
                    }
            }
        }
        viewModelScope.launch(Dispatchers.IO) {
            artists.collect { artists ->
                artists
                    .map { it.artist }
                    .filter {
                        it.thumbnailUrl == null ||
                                Duration.between(
                                    it.lastUpdateTime,
                                    LocalDateTime.now(),
                                ) > Duration.ofDays(10)
                    }.forEach { artist ->
                        YouTube.artist(artist.id).onSuccess { artistPage ->
                            database.query {
                                update(artist, artistPage)
                            }
                        }
                    }
            }
        }
    }
}

@HiltViewModel
class LibraryPodcastsViewModel
@Inject
constructor(
    @ApplicationContext context: Context,
    private val database: MusicDatabase,
    private val syncUtils: SyncUtils,
) : ViewModel() {
    // Subscribed podcast channels synced from YT Music
    val subscribedChannels = database.subscribedPodcasts()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    // SE "Episodes for Later" playlist fetched from YT Music (like AccountScreen)
    private val _sePlaylist = MutableStateFlow<com.metrolist.innertube.models.PlaylistItem?>(null)
    val sePlaylist = _sePlaylist.asStateFlow()

    // RDPN "New Episodes" playlist fetched from YouTube Music (real thumbnail + episode count)
    private val _rdpnPlaylist = MutableStateFlow<com.metrolist.innertube.models.PlaylistItem?>(null)
    val rdpnPlaylist = _rdpnPlaylist.asStateFlow()

    // Podcast host channels fetched from YT Music library/podcast_channels
    private val _apiPodcastChannels = MutableStateFlow<List<ArtistItem>>(emptyList())

    // Podcast channels: API subscriptions + locally bookmarked artists that have podcasts
    // Only shows channels explicitly subscribed to (not derived from saved podcasts)
    val podcastChannels = kotlinx.coroutines.flow.combine(
        _apiPodcastChannels,
        database.bookmarkedPodcastChannels()
    ) { apiChannels, localPodcastChannels ->
        // Convert locally bookmarked podcast channels to ArtistItem format
        val localAsArtistItems = localPodcastChannels.map { artist ->
            ArtistItem(
                id = artist.id,
                title = artist.artist.name,
                thumbnail = artist.artist.thumbnailUrl,
                shuffleEndpoint = null,
                radioEndpoint = null,
            )
        }

        // Combine and deduplicate by ID (prefer API version if exists)
        val apiIds = apiChannels.map { it.id }.toSet()
        val uniqueLocalChannels = localAsArtistItems.filter { it.id !in apiIds }
        apiChannels + uniqueLocalChannels
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    // Downloaded podcast episodes
    val downloadedEpisodes =
        context.dataStore.data
            .map {
                Pair(
                    it[SongSortTypeKey].toEnum(SongSortType.CREATE_DATE) to (it[SongSortDescendingKey] ?: true),
                    it[HideExplicitKey] ?: false
                )
            }.distinctUntilChanged()
            .flatMapLatest { (sortDesc, hideExplicit) ->
                val (sortType, descending) = sortDesc
                database.downloadedPodcastEpisodes(sortType, descending).map { it.filterExplicit(hideExplicit) }
            }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // Saved podcast episodes (in library, not necessarily downloaded)
    val savedEpisodes =
        context.dataStore.data
            .map {
                Pair(
                    it[SongSortTypeKey].toEnum(SongSortType.CREATE_DATE) to (it[SongSortDescendingKey] ?: true),
                    it[HideExplicitKey] ?: false
                )
            }.distinctUntilChanged()
            .flatMapLatest { (sortDesc, hideExplicit) ->
                val (sortType, descending) = sortDesc
                database.savedPodcastEpisodes(sortType, descending).map { it.filterExplicit(hideExplicit) }
            }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private suspend fun fetchSePlaylist() {
        YouTube.library("FEmusic_liked_playlists").completed().onSuccess {
            _sePlaylist.value = it.items
                .filterIsInstance<com.metrolist.innertube.models.PlaylistItem>()
                .find { it.id == "SE" }
        }.onFailure {
            timber.log.Timber.e(it, "[PODCAST] Failed to fetch SE playlist")
        }
    }

    private suspend fun fetchPodcastChannels() {
        YouTube.libraryPodcastChannels().onSuccess { page ->
            val channels = page.items.filterIsInstance<ArtistItem>()
            _apiPodcastChannels.value = channels
            timber.log.Timber.d("[PODCAST] Fetched ${channels.size} podcast channels from YT Music")
        }.onFailure {
            timber.log.Timber.e(it, "[PODCAST] Failed to fetch podcast channels")
        }
    }

    private suspend fun fetchRdpnPlaylist() {
        YouTube.newEpisodesPlaylistInfo().onSuccess { item ->
            _rdpnPlaylist.value = item
            timber.log.Timber.d("[PODCAST] RDPN playlist: ${item.title}, thumbnail: ${item.thumbnail}")
        }.onFailure {
            timber.log.Timber.e(it, "[PODCAST] Failed to fetch RDPN playlist info")
        }
    }

    init {
        viewModelScope.launch(Dispatchers.IO) {
            fetchSePlaylist()
        }
        viewModelScope.launch(Dispatchers.IO) {
            fetchPodcastChannels()
        }
        viewModelScope.launch(Dispatchers.IO) {
            fetchRdpnPlaylist()
        }
        viewModelScope.launch(Dispatchers.IO) {
            syncUtils.syncPodcastSubscriptionsSuspend()
        }
        // Observe refresh trigger for auto-refresh after subscribe/unsubscribe
        viewModelScope.launch(Dispatchers.IO) {
            PodcastRefreshTrigger.refreshFlow.collect {
                // Small delay to allow YouTube's backend to update
                kotlinx.coroutines.delay(1500)
                fetchPodcastChannels()
            }
        }
    }

    fun clearPodcastData() {
        viewModelScope.launch(Dispatchers.IO) {
            syncUtils.clearPodcastData()
        }
    }

    suspend fun refreshAll() {
        fetchSePlaylist()
        fetchPodcastChannels()
        fetchRdpnPlaylist()
        syncUtils.syncPodcastSubscriptionsSuspend()
        syncUtils.syncEpisodesForLaterSuspend()
    }

    /**
     * Force refresh podcast channels. Called when screen becomes visible.
     */
    fun refreshChannels() {
        viewModelScope.launch(Dispatchers.IO) {
            fetchPodcastChannels()
        }
    }
}

@HiltViewModel
class LibraryViewModel
@Inject
constructor() : ViewModel() {
    private val curScreen = mutableStateOf(LibraryFilter.LIBRARY)
    val filter: MutableState<LibraryFilter> = curScreen
}
