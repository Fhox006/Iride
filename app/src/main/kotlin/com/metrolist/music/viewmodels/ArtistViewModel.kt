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
import com.metrolist.music.db.entities.toAlbumEntity
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
        viewModelScope.launch(Dispatchers.IO) { newReleaseNotifier.markSeen(artistId) }
    }

    val unseenAlbumIds = newReleaseNotifier.unseenAlbumIds(artistId)
        .stateIn(viewModelScope, SharingStarted.Lazily, emptySet())

    fun markAlbumSeen(albumId: String) {
        viewModelScope.launch(Dispatchers.IO) { newReleaseNotifier.markAlbumSeen(artistId, albumId) }
    }

    val unseenSongIds = newReleaseNotifier.unseenSongIds
        .stateIn(viewModelScope, SharingStarted.Lazily, emptySet())

    fun markSongSeen(songId: String) {
        viewModelScope.launch(Dispatchers.IO) { newReleaseNotifier.markSongSeen(songId) }
    }

    private data class FeaturingEntry(val song: com.metrolist.innertube.models.SongItem, val sortKey: Long)

    val featuringSongs = kotlinx.coroutines.flow.combine(
        newReleaseNotifier.featuredSongs(artistId),
        database.artistFeaturedSongs(artistId),
    ) { remote, local ->
        val remoteEntries = remote.map { info ->
            FeaturingEntry(
                song = com.metrolist.innertube.models.SongItem(
                    id = info.songId,
                    title = info.title,
                    artists = info.otherArtists.map { com.metrolist.innertube.models.Artist(it.name, it.id) },
                    album = if (info.albumId != null && info.albumTitle != null) {
                        com.metrolist.innertube.models.Album(info.albumTitle, info.albumId)
                    } else null,
                    thumbnail = info.thumbnailUrl,
                ),
                sortKey = info.year?.let { (it - 1970).toLong() * 365L * 86_400_000L } ?: info.firstSeenMs,
            )
        }
        val localEntries = local.filterNot { it.song.isVideo }.map { song ->
            FeaturingEntry(
                song = com.metrolist.innertube.models.SongItem(
                    id = song.id,
                    title = song.title,
                    artists = song.orderedArtists.filter { it.id != artistId }
                        .map { com.metrolist.innertube.models.Artist(it.name, it.id) },
                    album = song.album?.let { com.metrolist.innertube.models.Album(it.title, it.id) },
                    thumbnail = song.thumbnailUrl.orEmpty(),
                    duration = song.song.duration.takeIf { it > 0 },
                ),
                sortKey = song.song.inLibrary?.toInstant(java.time.ZoneOffset.UTC)?.toEpochMilli() ?: 0L,
            )
        }
        (remoteEntries + localEntries)
            .distinctBy { it.song.id }
            .sortedByDescending { it.sortKey }
            .map { it.song }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private val isPodcastChannel = savedStateHandle.get<Boolean>("isPodcastChannel") ?: false
    var artistPage by mutableStateOf<ArtistPage?>(null)

    private val _expandedTopSongs = MutableStateFlow<List<com.metrolist.innertube.models.SongItem>?>(null)
    val expandedTopSongs = _expandedTopSongs.asStateFlow()

    private val _apiSubscribed = MutableStateFlow<Boolean?>(null)

    val libraryArtist = database.artist(artistId)
        .stateIn(viewModelScope, SharingStarted.Lazily, null)

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

    private val _recentAlbumPreciseDate = MutableStateFlow<String?>(null)
    val recentAlbumPreciseDate = _recentAlbumPreciseDate.asStateFlow()

    val recentAlbum = kotlinx.coroutines.flow.combine(
        snapshotFlow { artistPage },
        libraryAlbums
    ) { page, localAlbums ->
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
            val type = when {
                localRecent.album.songCount <= 1 -> AlbumReleaseType.SINGLE
                localRecent.album.songCount in 2..6 -> AlbumReleaseType.EP
                else -> AlbumReleaseType.ALBUM
            }
            return@combine RecentAlbumInfo(localRecent, type)
        }

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

            RecentAlbumInfo(item.toAlbumEntity(), type)
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, null)

    init {
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

        _apiSubscribed.value = shouldBeSubscribed

        viewModelScope.launch(Dispatchers.IO) {
            Timber.d("[CHANNEL_TOGGLE] Inside coroutine, updating database...")
            val artist = libraryArtist.value?.artist
            Timber.d("[CHANNEL_TOGGLE] libraryArtist.value?.artist = $artist")
            if (artist != null) {
                val newBookmark = if (shouldBeSubscribed) {
                    artist.bookmarkedAt ?: java.time.LocalDateTime.now()
                } else {
                    null
                }
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
            syncUtils.subscribeChannel(channelId, shouldBeSubscribed)
        }
    }
}
