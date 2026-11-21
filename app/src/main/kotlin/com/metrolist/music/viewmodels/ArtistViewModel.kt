/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.viewmodels

import android.content.Context
import android.os.SystemClock
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
import com.metrolist.innertube.pages.latestNativeRelease
import com.metrolist.innertube.pages.withCatalogAlbums
import com.metrolist.innertube.utils.albumReleaseDate
import com.metrolist.music.R
import com.metrolist.music.constants.HideExplicitKey
import com.metrolist.music.constants.HideVideoSongsKey
import com.metrolist.music.constants.HideYoutubeShortsKey
import com.metrolist.music.data.remote.ArtistArtworkCandidate
import com.metrolist.music.data.remote.ArtistArtworkRepository
import com.metrolist.music.data.remote.AppleMusicArtistEditorial
import com.metrolist.music.data.remote.MusicBrainzRepository
import com.metrolist.music.db.MusicDatabase
import com.metrolist.music.db.entities.ArtistEntity
import com.metrolist.music.db.entities.toAlbumEntity
import com.metrolist.music.extensions.filterExplicit
import com.metrolist.music.extensions.filterExplicitAlbums
import com.metrolist.music.utils.SyncUtils
import com.metrolist.music.utils.dataStore
import com.metrolist.music.utils.get
import com.metrolist.music.utils.reportException
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import timber.log.Timber
import javax.inject.Inject
import com.metrolist.music.extensions.filterVideoSongs as filterVideoSongsLocal

private object ArtistPageCache {
    private data class CachedPage(val page: ArtistPage, val loadedAt: Long)

    private const val FreshWindowMs = 120_000L
    private val pages = object : LinkedHashMap<String, CachedPage>(16, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, CachedPage>?) = size > 20
    }
    private val requests = mutableMapOf<String, Deferred<Result<ArtistPage>>>()
    private val prefetchedAt = mutableMapOf<String, Long>()
    private val requestScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @Synchronized fun get(id: String): ArtistPage? = pages[id]?.page

    @Synchronized fun put(id: String, page: ArtistPage) {
        pages[id] = CachedPage(page, SystemClock.elapsedRealtime())
    }

    fun prefetch(id: String) {
        synchronized(this) {
            val cachedAt = pages[id]?.loadedAt
            if (cachedAt != null && SystemClock.elapsedRealtime() - cachedAt < FreshWindowMs) return
            prefetchedAt[id] = SystemClock.elapsedRealtime()
        }
        request(id)
    }

    suspend fun fetch(id: String): Result<ArtistPage> {
        val prefetched = synchronized(this) {
            val cached = pages[id]
            val requestedAt = prefetchedAt[id]
            if (
                cached != null && requestedAt != null && cached.loadedAt >= requestedAt &&
                SystemClock.elapsedRealtime() - requestedAt < FreshWindowMs
            ) {
                prefetchedAt.remove(id)
                cached.page
            } else null
        }
        if (prefetched != null) return Result.success(prefetched)
        return request(id).await()
    }

    private fun request(id: String): Deferred<Result<ArtistPage>> = synchronized(this) {
        requests[id]?.let { return@synchronized it }
        requestScope.async {
            try {
                YouTube.artist(id)
            } catch (cancelled: kotlinx.coroutines.CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                Result.failure(error)
            }
        }.also { deferred ->
            requests[id] = deferred
            requestScope.launch {
                try {
                    deferred.await().onSuccess { put(id, it) }
                } finally {
                    synchronized(this@ArtistPageCache) {
                        if (requests[id] === deferred) requests.remove(id)
                    }
                }
            }
        }
    }
}

internal fun prefetchArtistPage(artistId: String) {
    if (artistId.startsWith("UC")) ArtistPageCache.prefetch(artistId)
}

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
    private val artistArtworkRepository: ArtistArtworkRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    val artistId = savedStateHandle.get<String>("artistId")!!

    private val isPodcastChannel = savedStateHandle.get<Boolean>("isPodcastChannel") ?: false
    var artistPage by mutableStateOf<ArtistPage?>(null)
    var artistPageLoadFailed by mutableStateOf(false)
        private set
    private var artistFetchJob: Job? = null
    private val latestNativeArtistPage = MutableStateFlow<ArtistPage?>(null)
    private val _appleMusicEditorial = MutableStateFlow<AppleMusicArtistEditorial?>(null)
    val appleMusicEditorial = _appleMusicEditorial.asStateFlow()
    private var editorialArtistName: String? = null
    private var editorialJob: kotlinx.coroutines.Job? = null

    fun loadAppleMusicEditorial(name: String?) {
        if (name.isNullOrBlank() || isPodcastChannel || !artistId.startsWith("UC") || artistPage?.artist?.isProfile == true) {
            editorialJob?.cancel()
            editorialArtistName = null
            _appleMusicEditorial.value = null
            return
        }
        if (editorialArtistName == name) return
        editorialArtistName = name
        editorialJob?.cancel()
        _appleMusicEditorial.value = null
        editorialJob = viewModelScope.launch {
            val editorial = try {
                artistArtworkRepository.findAppleMusicEditorial(name)
            } catch (cancelled: kotlinx.coroutines.CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                Timber.w(error, "Unable to load artist editorial artwork")
                null
            }
            if (editorialArtistName != name) return@launch
            _appleMusicEditorial.value = editorial
            // A failed request must be retryable when the artist page becomes visible again.
            if (editorial == null) editorialArtistName = null
        }
    }

    private val _expandedTopSongs = MutableStateFlow<List<com.metrolist.innertube.models.SongItem>?>(null)
    val expandedTopSongs = _expandedTopSongs.asStateFlow()

    private val _apiSubscribed = MutableStateFlow<Boolean?>(null)

    val libraryArtist = database.artist(artistId)
        .stateIn(viewModelScope, SharingStarted.Lazily, null)

    private val libraryContentActive = MutableStateFlow(false)

    fun setLibraryContentActive(active: Boolean) {
        libraryContentActive.value = active
    }

    private fun librarySongsFlow() = context.dataStore.data
        .map { (it[HideExplicitKey] ?: false) to (it[HideVideoSongsKey] ?: false) }
        .distinctUntilChanged()
        .flatMapLatest { (hideExplicit, hideVideoSongs) ->
            database.artistLikedSongs(artistId).map { it.filterExplicit(hideExplicit).filterVideoSongsLocal(hideVideoSongs) }
        }
        .flowOn(Dispatchers.Default)

    val libraryHasSongs = database.artistHasLikedSongs(artistId)
        .stateIn(viewModelScope, SharingStarted.Lazily, false)

    val externalArtistArtworks = kotlinx.coroutines.flow.combine(
        snapshotFlow { artistPage?.artist },
        libraryArtist,
    ) { online, local ->
        (online?.title ?: local?.artist?.name) to (online?.channelId ?: local?.artist?.channelId ?: artistId)
    }.distinctUntilChanged().flatMapLatest { (name, channelId) ->
        channelFlow<List<ArtistArtworkCandidate>> {
            send(emptyList())
            if (name != null && !isPodcastChannel) {
                val candidates = mutableListOf<ArtistArtworkCandidate>()
                val candidateMutex = Mutex()
                artistArtworkRepository.findArtworks(name, channelId) { candidate ->
                    candidateMutex.withLock {
                        candidates.add(candidate)
                        send(candidates.toList())
                    }
                }
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val isChannelSubscribed = kotlinx.coroutines.flow.combine(
        _apiSubscribed,
        database.artist(artistId),
    ) { apiState, localArtist ->
        val locallyBookmarked = localArtist?.artist?.bookmarkedAt != null
        locallyBookmarked || (apiState == true)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, false)
    val librarySongs = libraryContentActive
        .flatMapLatest { active -> if (active) librarySongsFlow() else flowOf(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val libraryAlbums = libraryContentActive
        .map { active -> active || !artistId.startsWith("UC") }
        .distinctUntilChanged()
        .flatMapLatest { active ->
            if (!active) {
                flowOf(emptyList())
            } else {
                context.dataStore.data
                    .map { it[HideExplicitKey] ?: false }
                    .distinctUntilChanged()
                    .flatMapLatest { hideExplicit ->
                        database.artistAlbumsPreview(artistId, previewSize = 20).map { it.filterExplicitAlbums(hideExplicit) }
                    }
            }
        }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    suspend fun loadLibrarySongsForPlayback() = librarySongsFlow().first()

    private val _recentAlbumPreciseDate = MutableStateFlow<String?>(null)
    val recentAlbumPreciseDate = _recentAlbumPreciseDate.asStateFlow()

    val recentAlbum = kotlinx.coroutines.flow.combine(
        latestNativeArtistPage,
        libraryAlbums,
    ) { page, localAlbums ->
        if (page != null) {
            val (section, item) = page.latestNativeRelease() ?: return@combine null
            val type = when {
                item.albumType?.contains("single", ignoreCase = true) == true ||
                    section.title.contains("Single", ignoreCase = true) ||
                    section.title.contains("Singol", ignoreCase = true) -> AlbumReleaseType.SINGLE
                item.albumType?.contains("EP", ignoreCase = true) == true ||
                    section.title.contains("EP", ignoreCase = true) -> AlbumReleaseType.EP
                else -> AlbumReleaseType.ALBUM
            }
            return@combine RecentAlbumInfo(item.toAlbumEntity(), type)
        }

        // Remote pages must never flash a guessed local album while YouTube loads.
        if (artistId.startsWith("UC")) return@combine null

        val threeMonthsAgo = java.time.LocalDate.now().minusMonths(3)
        val localRecent = localAlbums.mapNotNull { album ->
            val date = albumReleaseDate(album.album.releaseDate, album.album.year)
            if (date != null && date.isAfter(threeMonthsAgo)) album to date else null
        }.maxByOrNull { it.second }?.first ?: return@combine null
        val type = when {
            localRecent.album.songCount <= 1 -> AlbumReleaseType.SINGLE
            localRecent.album.songCount in 2..6 -> AlbumReleaseType.EP
            else -> AlbumReleaseType.ALBUM
        }
        RecentAlbumInfo(localRecent, type)
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
        artistFetchJob?.cancel()
        artistPageLoadFailed = false
        latestNativeArtistPage.value = null
        artistFetchJob = viewModelScope.launch {
            val hideExplicit = context.dataStore.get(HideExplicitKey, false)
            val hideVideoSongs = context.dataStore.get(HideVideoSongsKey, false)
            val hideYoutubeShorts = context.dataStore.get(HideYoutubeShortsKey, false)
            fun showPage(page: ArtistPage) {
                val sections = page.sections.map { section ->
                    section.copy(items = section.items.filterExplicit(hideExplicit).filterVideoSongs(hideVideoSongs).filterYoutubeShorts(hideYoutubeShorts))
                }.filter { it.items.isNotEmpty() }
                artistPage = page.copy(sections = sections)
            }
            ArtistPageCache.get(artistId)?.let(::showPage)
            ArtistPageCache.fetch(artistId)
                .onSuccess { page ->
                    currentCoroutineContext().ensureActive()
                    val filteredSections = withContext(Dispatchers.Default) {
                        page.sections
                            .map { section ->
                                section.copy(items = section.items.filterExplicit(hideExplicit).filterVideoSongs(hideVideoSongs).filterYoutubeShorts(hideYoutubeShorts))
                            }
                            .filter { section -> section.items.isNotEmpty() }
                    }

                    ArtistPageCache.put(artistId, page)
                    val loadCatalog = !isPodcastChannel && !page.artist.isProfile && artistId.startsWith("UC")
                    latestNativeArtistPage.value = page.copy(sections = filteredSections)
                    artistPage = page.copy(sections = filteredSections)
                    _apiSubscribed.value = page.isSubscribed

                    if (loadCatalog) {
                        launch {
                            val shortSections = page.sections.filter { section ->
                                section.title.contains("singl", ignoreCase = true) ||
                                    section.title.contains("singol", ignoreCase = true) ||
                                    section.title.contains("EP", ignoreCase = true)
                            }
                            val catalog = async(Dispatchers.IO) {
                                YouTube.artistCatalogAlbums(page.artist.title).filterExplicit(hideExplicit)
                            }
                            val knownShortReleases = shortSections.flatMap { section ->
                                section.items.filterIsInstance<com.metrolist.innertube.models.AlbumItem>()
                            }.toMutableList()
                            val expandedShortReleases = kotlinx.coroutines.coroutineScope {
                                shortSections.mapNotNull { section ->
                                    section.moreEndpoint?.let { endpoint ->
                                        async(Dispatchers.IO) {
                                            YouTube.artistItems(endpoint).getOrNull()?.items
                                                ?.filterIsInstance<com.metrolist.innertube.models.AlbumItem>()
                                                .orEmpty()
                                        }
                                    }
                                }.awaitAll().flatten()
                            }
                            currentCoroutineContext().ensureActive()
                            knownShortReleases += expandedShortReleases
                            val releases = catalog.await()
                            val deduplicatedShortReleases = knownShortReleases.distinctBy { it.id }
                            val pageToMerge = artistPage
                            val mergedPage = withContext(Dispatchers.Default) {
                                pageToMerge?.withCatalogAlbums(
                                    releases,
                                    context.getString(R.string.albums),
                                    context.getString(R.string.external_artist_singles),
                                    deduplicatedShortReleases,
                                )
                            }
                            if (pageToMerge != null) artistPage = mergedPage
                        }
                    }

                    _expandedTopSongs.value = null
                    val topSongsMoreEndpoint = filteredSections.firstOrNull { section ->
                        (section.items.firstOrNull() as? com.metrolist.innertube.models.SongItem)?.album != null
                    }?.moreEndpoint
                    if (topSongsMoreEndpoint != null) {
                        launch {
                            withContext(Dispatchers.IO) { YouTube.artistItems(topSongsMoreEndpoint) }
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
                    currentCoroutineContext().ensureActive()
                    artistPageLoadFailed = artistPage == null
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
