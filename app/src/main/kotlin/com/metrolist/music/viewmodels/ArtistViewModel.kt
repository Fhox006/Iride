/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.viewmodels

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.metrolist.innertube.YouTube
import com.metrolist.innertube.models.filterExplicit
import com.metrolist.innertube.models.filterVideoSongs
import com.metrolist.innertube.models.filterYoutubeShorts
import com.metrolist.innertube.pages.ArtistPage
import com.metrolist.music.constants.HideExplicitKey
import com.metrolist.music.constants.HideVideoSongsKey
import com.metrolist.music.constants.HideYoutubeShortsKey
import com.metrolist.music.data.remote.MusicBrainzRepository
import com.metrolist.music.db.MusicDatabase
import com.metrolist.music.db.entities.ArtistEntity
import com.metrolist.music.extensions.filterExplicit
import com.metrolist.music.extensions.filterExplicitAlbums
import com.metrolist.music.utils.NewReleaseNotifier
import com.metrolist.music.utils.SyncUtils
import com.metrolist.music.utils.dataStore
import com.metrolist.music.utils.get
import com.metrolist.music.utils.reportException
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import com.metrolist.music.extensions.filterVideoSongs as filterVideoSongsLocal

enum class AlbumReleaseType { ALBUM, EP, SINGLE }

data class RecentAlbumInfo(
    val album: com.metrolist.music.db.entities.Album,
    val type: AlbumReleaseType,
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ArtistViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: MusicDatabase,
    private val syncUtils: SyncUtils,
    private val musicBrainzRepository: MusicBrainzRepository,
    private val newReleaseNotifier: NewReleaseNotifier,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    val artistId = savedStateHandle.get<String>("artistId")!!

    init {
        // Opening the profile clears its new-release badge and it doesn't come back.
        viewModelScope.launch(Dispatchers.IO) { newReleaseNotifier.markSeen(artistId) }
    }

    private val isPodcastChannel = savedStateHandle.get<Boolean>("isPodcastChannel") ?: false
    var artistPage by mutableStateOf<ArtistPage?>(null)

    // YTM's own "Top Songs" shelf on the artist page only ever carries ~5 tracks — the full list
    // sits behind the shelf's own "more" browse endpoint, same one ArtistItemsScreen's "see all"
    // uses. Fetched quietly in the background so the carousel upgrades from 5 to the real count
    // without the user having to leave the page for it.
    private val _expandedTopSongs = MutableStateFlow<List<com.metrolist.innertube.models.SongItem>?>(null)
    val expandedTopSongs = _expandedTopSongs.asStateFlow()

    // Track API subscription state separately
    private val _apiSubscribed = MutableStateFlow<Boolean?>(null)

    val libraryArtist = database.artist(artistId)
        .stateIn(viewModelScope, SharingStarted.Lazily, null)

    // Combine API state with local database state - local takes precedence when not logged in
    val isChannelSubscribed = kotlinx.coroutines.flow.combine(
        _apiSubscribed,
        database.artist(artistId),
    ) { apiState, localArtist ->
        val locallyBookmarked = localArtist?.artist?.bookmarkedAt != null
        locallyBookmarked || (apiState == true)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, false)
    val librarySongs = context.dataStore.data
        .map { (it[HideExplicitKey] ?: false) to (it[HideVideoSongsKey] ?: false) }
        .distinctUntilChanged()
        .flatMapLatest { (hideExplicit, hideVideoSongs) ->
            database.artistSongsPreview(artistId).map { it.filterExplicit(hideExplicit).filterVideoSongsLocal(hideVideoSongs) }
        }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    val libraryAlbums = context.dataStore.data
        .map { it[HideExplicitKey] ?: false }
        .distinctUntilChanged()
        .flatMapLatest { hideExplicit ->
            database.artistAlbumsPreview(artistId, previewSize = 20).map { it.filterExplicitAlbums(hideExplicit) }
        }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // Precise release date for the currently computed [recentAlbum], resolved asynchronously via
    // MusicBrainz when the local DB / YTM shelf only gave us a bare year — mirrors what
    // AlbumViewModel already does when the user opens an album's own screen, but done proactively
    // here since a remote-sourced recent release usually isn't in the local DB yet to trigger that.
    private val _recentAlbumPreciseDate = MutableStateFlow<String?>(null)
    val recentAlbumPreciseDate = _recentAlbumPreciseDate.asStateFlow()

    val recentAlbum = kotlinx.coroutines.flow.combine(
        snapshotFlow { artistPage },
        libraryAlbums
    ) { page, localAlbums ->
        // First, try to find a recent album in the library (most accurate date)
        val threeMonthsAgo = java.time.LocalDate.now().minusMonths(3)
        val localRecent = localAlbums.filter { it.album.releaseDate != null }.mapNotNull { album ->
            val dateStr = album.album.releaseDate!!
            val date = try {
                val parts = dateStr.split("-")
                when (parts.size) {
                    3 -> java.time.LocalDate.of(parts[0].toInt(), parts[1].toInt(), parts[2].toInt())
                    2 -> java.time.LocalDate.of(parts[0].toInt(), parts[1].toInt(), 1)
                    else -> java.time.LocalDate.of(parts[0].toInt(), 1, 1)
                }
            } catch (e: Exception) {
                null
            }
            if (date != null && date.isAfter(threeMonthsAgo)) album to date else null
        }.maxByOrNull { it.second }?.first

        if (localRecent != null) {
            // Local library entries only ever carry a songCount, never the shelf they came
            // from — approximate the release type from track count (industry-standard-ish
            // thresholds: 1 track = single, 2-6 = EP, 7+ = album).
            val type = when {
                localRecent.album.songCount <= 1 -> AlbumReleaseType.SINGLE
                localRecent.album.songCount in 2..6 -> AlbumReleaseType.EP
                else -> AlbumReleaseType.ALBUM
            }
            return@combine RecentAlbumInfo(localRecent, type)
        }

        // If not in library, look at the artist page from YTM.
        // Consider every Albums/Singles/EPs shelf (not just the first match) and pick the
        // newest item across all of them, since YTM doesn't always order shelves consistently.
        val candidateSections = page?.sections
            ?.filter {
                it.title.contains("Album", ignoreCase = true) ||
                it.title.contains("Singol", ignoreCase = true) ||
                it.title.contains("Single", ignoreCase = true) ||
                it.title.contains("EP", ignoreCase = true) ||
                it.title.contains("Latest", ignoreCase = true) ||
                it.title.contains("Uscita", ignoreCase = true) ||
                it.title.contains("Release", ignoreCase = true)
            }

        val (albumSection, albumItem) = candidateSections
            ?.flatMap { section ->
                section.items.filterIsInstance<com.metrolist.innertube.models.AlbumItem>().map { section to it }
            }
            ?.maxByOrNull { (_, item) -> item.year ?: Int.MIN_VALUE }
            ?: (null to null)

        albumItem?.let { item ->
            val type = when {
                albumSection?.title?.contains("Album", ignoreCase = true) == true -> AlbumReleaseType.ALBUM
                albumSection?.title?.contains("EP", ignoreCase = true) == true -> AlbumReleaseType.EP
                albumSection?.title?.contains("Single", ignoreCase = true) == true ||
                    albumSection?.title?.contains("Singol", ignoreCase = true) == true -> AlbumReleaseType.SINGLE
                else -> AlbumReleaseType.ALBUM
            }

            RecentAlbumInfo(
                com.metrolist.music.db.entities.Album(
                    album = com.metrolist.music.db.entities.AlbumEntity(
                        id = item.browseId,
                        playlistId = item.playlistId,
                        title = item.title,
                        year = item.year,
                        thumbnailUrl = item.thumbnail,
                        explicit = item.explicit,
                        songCount = if (type == AlbumReleaseType.SINGLE) 1 else 0,
                        duration = 0
                    )
                ),
                type,
            )
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, null)

    init {
        // The recent-album panel otherwise only ever shows a bare year (YTM's artist-page shelves
        // don't carry a full date) — look up the exact date once we know which release is "recent"
        // instead of leaving it stuck at year-only precision.
        viewModelScope.launch {
            recentAlbum.collect { info ->
                _recentAlbumPreciseDate.value = null
                val album = info?.album?.album ?: return@collect
                val regex = Regex("""\d{4}-\d{2}(-\d{2})?""")
                if (album.releaseDate != null && regex.matches(album.releaseDate)) return@collect
                val artistName = artistPage?.artist?.title ?: libraryArtist.value?.artist?.name
                val date = musicBrainzRepository.getAlbumReleaseDate(
                    albumTitle = album.title,
                    artistName = artistName,
                    year = album.year,
                )
                if (date != null) _recentAlbumPreciseDate.value = date
            }
        }
    }

    init {
        // Load artist page and reload when hide explicit setting changes
        viewModelScope.launch {
            context.dataStore.data
                .map {
                    Triple(
                        it[HideExplicitKey] ?: false,
                        it[HideVideoSongsKey] ?: false,
                        it[HideYoutubeShortsKey] ?: false
                    )
                }
                .distinctUntilChanged()
                .collect {
                    fetchArtistsFromYTM()
                }
        }
    }

    fun fetchArtistsFromYTM() {
        viewModelScope.launch {
            val hideExplicit = context.dataStore.get(HideExplicitKey, false)
            val hideVideoSongs = context.dataStore.get(HideVideoSongsKey, false)
            val hideYoutubeShorts = context.dataStore.get(HideYoutubeShortsKey, false)
            YouTube.artist(artistId)
                .onSuccess { page ->
                    val filteredSections = page.sections
                        .map { section ->
                            section.copy(items = section.items.filterExplicit(hideExplicit).filterVideoSongs(hideVideoSongs).filterYoutubeShorts(hideYoutubeShorts))
                        }
                        .filter { section -> section.items.isNotEmpty() }

                    artistPage = page.copy(sections = filteredSections)
                    // Store API subscription state
                    _apiSubscribed.value = page.isSubscribed

                    _expandedTopSongs.value = null
                    val topSongsMoreEndpoint = filteredSections.firstOrNull { section ->
                        (section.items.firstOrNull() as? com.metrolist.innertube.models.SongItem)?.album != null
                    }?.moreEndpoint
                    if (topSongsMoreEndpoint != null) {
                        viewModelScope.launch {
                            YouTube.artistItems(topSongsMoreEndpoint)
                                .onSuccess { itemsPage ->
                                    _expandedTopSongs.value = itemsPage.items
                                        .filterIsInstance<com.metrolist.innertube.models.SongItem>()
                                        .filterExplicit(hideExplicit)
                                        .filterVideoSongs(hideVideoSongs)
                                        .filterYoutubeShorts(hideYoutubeShorts)
                                }
                        }
                    }
                }.onFailure {
                    reportException(it)
                }
        }
    }

    fun toggleChannelSubscription() {
        val channelId = artistPage?.artist?.channelId ?: artistId
        val isCurrentlySubscribed = isChannelSubscribed.value
        val shouldBeSubscribed = !isCurrentlySubscribed

        Timber.d("[CHANNEL_TOGGLE] toggleChannelSubscription called: artistId=$artistId, channelId=$channelId, isCurrentlySubscribed=$isCurrentlySubscribed, shouldBeSubscribed=$shouldBeSubscribed")

        // Optimistically update API state for immediate UI feedback
        _apiSubscribed.value = shouldBeSubscribed

        viewModelScope.launch(Dispatchers.IO) {
            Timber.d("[CHANNEL_TOGGLE] Inside coroutine, updating database...")
            // Update local database first (optimistic update)
            // Call DAO methods directly - they're synchronous on IO dispatcher
            val artist = libraryArtist.value?.artist
            Timber.d("[CHANNEL_TOGGLE] libraryArtist.value?.artist = $artist")
            if (artist != null) {
                val newBookmark = if (shouldBeSubscribed) {
                    artist.bookmarkedAt ?: java.time.LocalDateTime.now()
                } else {
                    null
                }
                // Also set isPodcastChannel if subscribing from podcast context
                val updatedArtist = artist.copy(
                    bookmarkedAt = newBookmark,
                    isPodcastChannel = if (shouldBeSubscribed && isPodcastChannel) true else artist.isPodcastChannel
                )
                Timber.d("[CHANNEL_TOGGLE] Updating existing artist: ${artist.id} -> bookmarkedAt=$newBookmark, isPodcastChannel=${updatedArtist.isPodcastChannel}")
                database.update(updatedArtist)
            } else if (shouldBeSubscribed) {
                Timber.d("[CHANNEL_TOGGLE] No existing artist, inserting new one")
                artistPage?.artist?.let {
                    database.insert(
                        ArtistEntity(
                            id = artistId,
                            name = it.title,
                            channelId = it.channelId,
                            thumbnailUrl = it.thumbnail,
                            bookmarkedAt = java.time.LocalDateTime.now(),
                            isPodcastChannel = isPodcastChannel,
                        )
                    )
                    Timber.d("[CHANNEL_TOGGLE] Inserted new artist: $artistId, isPodcastChannel=$isPodcastChannel")
                } ?: Timber.d("[CHANNEL_TOGGLE] artistPage?.artist is null, cannot insert")
            } else {
                Timber.d("[CHANNEL_TOGGLE] No artist and shouldBeSubscribed=false, nothing to do")
            }

            Timber.d("[CHANNEL_TOGGLE] Calling syncUtils.subscribeChannel($channelId, $shouldBeSubscribed)")
            // Sync with YouTube (handles login check internally)
            syncUtils.subscribeChannel(channelId, shouldBeSubscribed)
        }
    }
}
