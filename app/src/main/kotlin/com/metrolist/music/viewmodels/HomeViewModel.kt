/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.viewmodels

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.metrolist.music.R
import com.metrolist.innertube.YouTube
import com.metrolist.innertube.models.AlbumItem
import com.metrolist.innertube.models.Artist as YTArtist
import com.metrolist.innertube.models.ArtistItem
import com.metrolist.innertube.models.PlaylistItem
import com.metrolist.innertube.models.SongItem
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.Job
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit
import timber.log.Timber
import com.metrolist.innertube.models.WatchEndpoint
import com.metrolist.innertube.models.BrowseEndpoint
import com.metrolist.innertube.models.YTItem
import com.metrolist.innertube.models.filterExplicit
import com.metrolist.innertube.models.filterVideoSongs
import com.metrolist.innertube.models.filterYoutubeShorts
import com.metrolist.innertube.models.isMixtape
import com.metrolist.innertube.pages.ExplorePage
import com.metrolist.innertube.pages.HomePage
import com.metrolist.innertube.utils.completed
import com.metrolist.music.constants.HideExplicitKey
import com.metrolist.music.constants.RandomizeHomeOrderKey
import com.metrolist.music.constants.HideVideoSongsKey
import com.metrolist.music.constants.HideYoutubeShortsKey
import com.metrolist.music.constants.HomeCacheLastLoadedKey
import com.metrolist.music.constants.AccountNameKey
import com.metrolist.music.constants.ArtistSortType
import com.metrolist.music.constants.AlbumSortType
import com.metrolist.music.constants.PlaylistSortType
import com.metrolist.music.constants.SongSortType
import com.metrolist.music.constants.AccountPhotoUrlKey
import com.metrolist.music.constants.InnerTubeCookieKey
import com.metrolist.music.constants.VisitorDataKey
import com.metrolist.music.constants.SyncBannerLaunchCountKey
import com.metrolist.innertube.utils.parseCookieString
import com.metrolist.music.constants.LastMoodChipParamsKey
import com.metrolist.music.constants.LastMoodChipTitleKey
import com.metrolist.music.constants.MoodSnapshotKey
import com.metrolist.music.constants.QuickPicks
import com.metrolist.music.constants.QuickPicksKey
import com.metrolist.music.constants.HeroCarouselSnapshotKey
import com.metrolist.music.constants.LastDiscoveryWeeklySyncKey
import com.metrolist.music.constants.HomeCollapsedSectionsKey
import com.metrolist.music.constants.ShowWrappedCardKey
import com.metrolist.music.discovery.AlbumRecommendationsGenerator
import com.metrolist.music.discovery.DiscoveryWeeklyGenerator
import com.metrolist.music.discovery.MixMood
import com.metrolist.music.discovery.MoodMixes
import com.metrolist.music.discovery.MixPosterRepository
import com.metrolist.music.discovery.MoodLoadRetry
import com.metrolist.music.discovery.MoodPageCache
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.update
import com.metrolist.music.discovery.HeroCarouselGenerator
import com.metrolist.music.models.DischiPerTeItem
import com.metrolist.music.models.ForYouShelfItem
import com.metrolist.music.models.ForYouShelfTile
import com.metrolist.music.models.HeroCarouselItem
import com.metrolist.music.constants.SpeedDialSnapshotKey
import com.metrolist.music.constants.WrappedSeenKey
import com.metrolist.music.models.HomeSnapshotItem
import com.metrolist.music.models.MoodSnapshot
import com.metrolist.music.models.toMediaMetadata
import com.metrolist.music.models.toPlaylistItem
import com.metrolist.music.models.SpeedDialSnapshot
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import com.metrolist.music.ui.screens.HomeSection
import com.metrolist.music.db.MusicDatabase
import com.metrolist.music.db.entities.Album
import com.metrolist.music.db.entities.toAlbumEntity
import com.metrolist.music.db.entities.Artist
import com.metrolist.music.db.entities.ArtistEntity
import com.metrolist.music.db.entities.LocalItem
import com.metrolist.music.db.entities.PlaylistEntity
import com.metrolist.music.db.entities.Playlist
import com.metrolist.music.db.entities.PlaylistSongMap
import com.metrolist.music.db.entities.Song
import com.metrolist.music.db.entities.SongEntity
import com.metrolist.music.db.entities.SpeedDialItem
import com.metrolist.music.extensions.filterVideoSongs
import com.metrolist.music.extensions.toEnum
import com.metrolist.music.models.SimilarRecommendation
import com.metrolist.music.ui.screens.wrapped.WrappedAudioService
import com.metrolist.music.ui.screens.wrapped.WrappedManager
import com.metrolist.music.utils.HomeFeedSnapshotStore
import com.metrolist.music.utils.SyncUtils
import com.metrolist.music.utils.NetworkConnectivityObserver
import com.metrolist.music.utils.dataStore
import com.metrolist.music.utils.get
import com.metrolist.music.utils.reportException
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import javax.inject.Inject
import kotlin.random.Random

private const val QUICK_PICKS_TARGET_COUNT = 20
private const val HOME_MIX_CONTINUATION_LIMIT = 3
private const val MAX_SIMILAR_RECOMMENDATIONS = 14
private const val HOME_SIMILAR_CONTINUATION_LIMIT = 6

private val HomeMixTitleMatchers = listOf(
    listOf("new release", "nuove uscite", "nuova uscita", "novita", "fresh"),
    listOf("discover", "discovery", "scopri", "scoperta"),
    listOf("archive", "archivio"),
    listOf("replay", "riproduzione", "ripetizione"),
)

private fun HomePage?.freshFindOldFavoritePlaylists(): List<PlaylistItem> {
    val mixes = this?.sections.orEmpty()
        .flatMap { it.items }
        .filterIsInstance<PlaylistItem>()
        .distinctBy { it.id }

    return HomeMixTitleMatchers.mapNotNull { matchers ->
        mixes.firstOrNull { playlist ->
            val normalizedTitle = java.text.Normalizer.normalize(
                playlist.title.lowercase(),
                java.text.Normalizer.Form.NFD,
            ).replace(Regex("\\p{Mn}+"), "")
            matchers.any { matcher -> normalizedTitle.contains(matcher) }
        }
    }.distinctBy { it.id }
}

data class DailyDiscoverItem(
    val seed: Song,
    val recommendation: YTItem,
    val relatedEndpoint: BrowseEndpoint?
)

data class CommunityPlaylistItem(
    val playlist: PlaylistItem,
    val songs: List<SongItem>
)

@HiltViewModel
@OptIn(FlowPreview::class)
class HomeViewModel @Inject constructor(
    @ApplicationContext val context: Context,
    val database: MusicDatabase,
    val syncUtils: SyncUtils,
    val wrappedManager: WrappedManager,
    private val wrappedAudioService: WrappedAudioService,
    private val networkConnectivity: NetworkConnectivityObserver,
) : ViewModel() {
    val contentSnapshot = HomeContentSnapshot(viewModelScope)
    val syncState = syncUtils.syncState

    val syncBannerLaunchCount: StateFlow<Int> = context.dataStore.data
        .map { it[SyncBannerLaunchCountKey] ?: 0 }
        .stateIn(viewModelScope, SharingStarted.Eagerly, 0)

    val isRefreshing = MutableStateFlow(false)
    val isLoading = MutableStateFlow(false)
    val isHomeFeedLoading = MutableStateFlow(true)
    val isForYouLoading = MutableStateFlow(true)
    val isAccountPlaylistsLoading = MutableStateFlow(true)
    val regeneratingSections = MutableStateFlow<Set<String>>(emptySet())
    val isRandomizing = MutableStateFlow(false)
    val isPhase1Complete = MutableStateFlow(false)
    val phase1Complete = MutableStateFlow(false)
    val phase2Complete = MutableStateFlow(false)
    val visibleSections: MutableStateFlow<Set<String>> = MutableStateFlow(setOf("speed_dial", "mood_and_genres", "discovery"))

    // Snapshot-backed read: after App.onCreate warms the store this never blocks the main
    // thread, unlike the runBlocking data.first() it replaces (this runs during the first
    // composition of the app).
    private val smartBootEnabled: Boolean = true

    private var homeFeedJob: Job? = null

    private val quickPicksEnum = context.dataStore.data.map {
        it[QuickPicksKey].toEnum(QuickPicks.QUICK_PICKS)
    }.distinctUntilChanged()

    val quickPicks = MutableStateFlow<List<Song>?>(null)
    val dailyDiscover = MutableStateFlow<List<DailyDiscoverItem>?>(null)
    val forgottenFavorites = MutableStateFlow<List<Song>?>(null)
    val keepListening = MutableStateFlow<List<LocalItem>?>(null)
    val similarRecommendations = MutableStateFlow<List<SimilarRecommendation>?>(null)
    val accountPlaylists = MutableStateFlow<List<PlaylistItem>?>(null)
    val homePage = MutableStateFlow<HomePage?>(null)
    val homePersonalMixes = MutableStateFlow<List<PlaylistItem>>(emptyList())
    val starredPlaylistThumbnails = database.lastLikedSongThumbnails()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    private var homeMixContinuationJob: Job? = null
    val explorePage = MutableStateFlow<ExplorePage?>(null)
    val communityPlaylists = MutableStateFlow<List<CommunityPlaylistItem>?>(null)
    val selectedChip = MutableStateFlow<HomePage.Chip?>(null)
    private val previousHomePage = MutableStateFlow<HomePage?>(null)

    private fun transformChips(chips: List<HomePage.Chip>?): List<HomePage.Chip>? {
        return chips?.filter { chip ->
            val title = chip.title.lowercase()
            !title.contains("home") && !title.contains("podcast")
        }?.shuffled()
    }

    val savedPodcastShows = MutableStateFlow<List<com.metrolist.innertube.models.PodcastItem>>(emptyList())
    val episodesForLater = MutableStateFlow<List<SongItem>>(emptyList())

    val allLocalItems = MutableStateFlow<List<LocalItem>>(emptyList())
    val allYtItems = MutableStateFlow<List<YTItem>>(emptyList())

    val randomSeed = MutableStateFlow(System.currentTimeMillis())

    private val randomizeHomeOrder: StateFlow<Boolean> = context.dataStore.data
        .map { it[RandomizeHomeOrderKey] ?: false }
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val moodPage = MutableStateFlow<HomePage?>(null)

    val isHeroCarouselEnabled = MutableStateFlow(true)
    val isHeroCarouselLoading = MutableStateFlow(true)

    val heroCarouselItems = MutableStateFlow<List<HeroCarouselItem>>(emptyList())

    private val heroCarouselGenerator = HeroCarouselGenerator(database)

    val dischiPerTe = MutableStateFlow<List<DischiPerTeItem>?>(null)
    val forYouShelves = MutableStateFlow<List<ForYouShelfItem>>(emptyList())
    private var forYouArtistPool: List<Artist> = emptyList()
    private var forYouPoolCursor = 0
    private var forYouEnrichmentJob: Job? = null
    private val _isLoadingMoreForYou = MutableStateFlow(false)

    private val albumRecommendationsGenerator = AlbumRecommendationsGenerator(database)

    private val discoveryWeeklyGenerator = DiscoveryWeeklyGenerator(database)
    private val discoveryWeeklySyncInFlight = java.util.concurrent.atomic.AtomicBoolean(false)

    val discoveryWeeklyPlaylist = database
        .playlist(PlaylistEntity.DISCOVER_WEEKLY_PLAYLIST_ID)
        .stateIn(viewModelScope, SharingStarted.Lazily, null)

    fun syncDiscoveryWeeklyIfNeeded(force: Boolean = false) {
        viewModelScope.launch(Dispatchers.IO) {
            if (!discoveryWeeklySyncInFlight.compareAndSet(false, true)) return@launch
            try {
                val prefs = context.dataStore.data.first()
                val lastSyncMillis = prefs[LastDiscoveryWeeklySyncKey]
                val existingRow = database.playlist(PlaylistEntity.DISCOVER_WEEKLY_PLAYLIST_ID).first()
                val playlistReady = existingRow != null && existingRow.songCount > 0
                val due = lastSyncMillis == null ||
                    java.time.Instant.ofEpochMilli(lastSyncMillis).plus(java.time.Duration.ofDays(7))
                        .isBefore(java.time.Instant.now())
                if (!force && !due && playlistReady) return@launch

                val hideExplicit = context.dataStore.get(HideExplicitKey, false)
                val hideVideoSongs = context.dataStore.get(HideVideoSongsKey, false)
                val songs = discoveryWeeklyGenerator.generate(
                    hideExplicit = hideExplicit,
                    hideVideoSongs = hideVideoSongs,
                    seed = java.time.LocalDate.now().toEpochDay() / 7,
                )
                if (songs.isEmpty()) {
                    Timber.tag("DiscoveryWeekly").w("generate() returned 0 songs, historyPool/network issue")
                    context.dataStore.edit { it[LastDiscoveryWeeklySyncKey] = System.currentTimeMillis() }
                    return@launch
                }

                val playlistId = PlaylistEntity.DISCOVER_WEEKLY_PLAYLIST_ID
                val existingPlaylist = existingRow?.playlist
                val now = java.time.LocalDateTime.now()
                val playlistEntity = existingPlaylist?.copy(lastUpdateTime = now)
                    ?: PlaylistEntity(
                        id = playlistId,
                        name = context.getString(R.string.discovery_weekly),
                        isEditable = true,
                        bookmarkedAt = now,
                        lastUpdateTime = now,
                    )
                if (existingPlaylist == null) database.insert(playlistEntity) else database.update(playlistEntity)

                database.clearPlaylist(playlistId)
                songs.forEachIndexed { position, song ->
                    database.insert(song.toMediaMetadata())
                    database.insert(PlaylistSongMap(songId = song.id, playlistId = playlistId, position = position))
                }

                context.dataStore.edit { it[LastDiscoveryWeeklySyncKey] = System.currentTimeMillis() }
            } catch (e: Exception) {
                reportException(e)
            } finally {
                discoveryWeeklySyncInFlight.set(false)
            }
        }
    }

    private fun defaultHeroCarouselSeed() = System.currentTimeMillis()

    private var lastHeroCarouselSeed = defaultHeroCarouselSeed()

    private var heroCarouselJob: Job? = null
    private var previousHeroArtistIds: Set<String> = emptySet()
    private var previousFirstHeroArtistId: String? = null
    private val repairedHeroCovers = java.util.concurrent.ConcurrentHashMap.newKeySet<String>()

    fun refreshHeroCarousel(seed: Long = lastHeroCarouselSeed, force: Boolean = false) {
        if (!force && heroCarouselItems.value.isNotEmpty()) return
        if (force) {
            previousFirstHeroArtistId = heroCarouselItems.value
                .filterIsInstance<HeroCarouselItem.ArtistRadio>().firstOrNull()?.artistId
            previousHeroArtistIds = heroCarouselItems.value
                .filterIsInstance<HeroCarouselItem.ArtistRadio>().map { it.artistId }.toSet()
            repairedHeroCovers.clear()
        }
        lastHeroCarouselSeed = seed
        heroCarouselJob?.cancel()
        isHeroCarouselLoading.value = true
        heroCarouselJob = viewModelScope.launch(Dispatchers.IO, start = kotlinx.coroutines.CoroutineStart.LAZY) {
            val myJob = coroutineContext[Job]
            try {
                val radios = heroCarouselGenerator.generate(
                    homePage = homePage.value,
                    seed = seed,
                    previousArtistIds = previousHeroArtistIds,
                    previousFirstArtistId = previousFirstHeroArtistId,
                )
                coroutineContext.ensureActive()
                if (radios.isEmpty()) return@launch
                heroCarouselItems.value = radios
                if (smartBootEnabled) persistHeroCarouselSnapshot(seed, radios)
                val withCovers = heroCarouselGenerator.resolveMissingCovers(radios)
                coroutineContext.ensureActive()
                if (withCovers != radios) {
                    val coversById = withCovers.associateBy { it.artistId }
                    heroCarouselItems.update { current ->
                        current.map { item ->
                            if (item is HeroCarouselItem.ArtistRadio && item.coverUrl.isNullOrBlank()) {
                                coversById[item.artistId] ?: item
                            } else item
                        }
                    }
                    if (smartBootEnabled) persistHeroCarouselSnapshot(seed, heroCarouselItems.value)
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                reportException(error)
            } finally {
                if (heroCarouselJob === myJob) isHeroCarouselLoading.value = false
            }
        }
        heroCarouselJob?.start()
    }

    fun repairHeroRadioCover(artistId: String) {
        if (!repairedHeroCovers.add(artistId)) return
        val seed = lastHeroCarouselSeed
        viewModelScope.launch(Dispatchers.IO) {
            val cover = heroCarouselGenerator.resolveCover(artistId) ?: return@launch
            if (seed != lastHeroCarouselSeed) return@launch
            heroCarouselItems.update { current ->
                current.map { item ->
                    if (item is HeroCarouselItem.ArtistRadio && item.artistId == artistId) item.copy(coverUrl = cover)
                    else item
                }
            }
            if (smartBootEnabled) persistHeroCarouselSnapshot(seed, heroCarouselItems.value)
        }
    }

    @kotlinx.serialization.Serializable
    private data class HeroCarouselSnapshot(
        val seed: Long,
        val items: List<HeroCarouselItem>,
    )

    private suspend fun persistHeroCarouselSnapshot(seed: Long, items: List<HeroCarouselItem>) {
        if (items.isEmpty()) return
        val json = runCatching { snapshotJson.encodeToString(HeroCarouselSnapshot(seed, items)) }.getOrNull() ?: return
        context.dataStore.edit { it[HeroCarouselSnapshotKey] = json }
    }

    suspend fun fetchArtistRadioEndpoint(artistId: String): WatchEndpoint? =
        YouTube.artist(artistId).getOrNull()?.artist?.radioEndpoint

    val cachedSpeedDialSnapshot = MutableStateFlow<SpeedDialSnapshot?>(null)
    val cachedMoodSnapshot = MutableStateFlow<MoodSnapshot?>(null)

    private val snapshotJson = Json { ignoreUnknownKeys = true }
    private var lastSavedSpeedDialIds: List<String> = emptyList()

    private fun mapYTItemToSnapshot(item: YTItem): HomeSnapshotItem? = when (item) {
        is SongItem -> HomeSnapshotItem(
            id = item.id,
            title = item.title,
            subtitle = item.artists.joinToString(", ") { it.name }.takeIf { it.isNotEmpty() },
            thumbnailUrl = item.thumbnail.takeIf { it.isNotEmpty() },
            type = "song",
        )
        is AlbumItem -> HomeSnapshotItem(
            id = item.browseId,
            title = item.title,
            subtitle = item.artists?.joinToString(", ") { it.name }?.takeIf { it.isNotEmpty() },
            thumbnailUrl = item.thumbnail.takeIf { it.isNotEmpty() },
            type = "album",
            browseId = item.browseId,
            playlistId = item.playlistId,
        )
        is ArtistItem -> HomeSnapshotItem(
            id = item.id,
            title = item.title,
            thumbnailUrl = item.thumbnail?.takeIf { it.isNotEmpty() },
            type = "artist",
        )
        is PlaylistItem -> HomeSnapshotItem(
            id = item.id,
            title = item.title,
            subtitle = item.author?.name,
            thumbnailUrl = item.thumbnail?.takeIf { it.isNotEmpty() },
            type = "playlist",
        )
        else -> null
    }

    private suspend fun saveMoodSnapshotAfterLoad(chipTitle: String, chipParams: String?, page: HomePage) {
        if (!MoodMixes.isAllowedCategory(chipTitle)) return
        val label = MixMood.fromChip(chipTitle)?.label ?: chipTitle
        val mixItems = MoodMixes.select(page.sections.flatMap { it.items })
        val snapshotItems = mixItems.mapNotNull { mapYTItemToSnapshot(it) }
        val existingIds = cachedMoodSnapshot.value
            ?.takeIf { it.chipParams == chipParams }
            ?.items?.map { it.id }
        if (existingIds == snapshotItems.map { it.id } &&
            cachedMoodSnapshot.value?.chipTitle == label &&
            cachedMoodSnapshot.value?.chipParams == chipParams) return
        val snapshot = MoodSnapshot(System.currentTimeMillis(), label, chipParams, snapshotItems)
        cachedMoodSnapshot.value = snapshot
        refreshHeroCarousel()
        val json = runCatching { snapshotJson.encodeToString(snapshot) }.getOrNull() ?: return
        context.dataStore.edit { it[MoodSnapshotKey] = json }
    }

    val moodMixItems = MutableStateFlow<List<PlaylistItem>?>(null)
    val isMoodLoading = MutableStateFlow(false)
    val isMoodError = MutableStateFlow(false)
    val preferredMoodParams = MutableStateFlow<String?>(null)
    val isMoodPreferenceReady = MutableStateFlow(false)
    private var loadedMoodFilters: Triple<Boolean, Boolean, Boolean>? = null
    val loadedMoodParams = MutableStateFlow<String?>(null)
    private var moodPageJob: kotlinx.coroutines.Job? = null

    private val moodPostersLazy = lazy { MixPosterRepository(context) }
    private val moodPosters by moodPostersLazy
    val moodPosterImageLoader get() = moodPosters.imageLoader
    val moodPosterUrls = MutableStateFlow<Map<String, String>>(emptyMap())
    data class HomeMoodContent(
        val params: String?,
        val items: List<PlaylistItem>?,
        val posters: Map<String, String>,
    )
    val homeMoodContent = combine(loadedMoodParams, moodMixItems, moodPosterUrls) { params, items, posters ->
        HomeMoodContent(params, items, posters)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, HomeMoodContent(null, null, emptyMap()))
    private var moodPosterJob: Job? = null
    private var moodPosterSize = 448
    private var requestedMoodParams: String? = null
    val testPlaylistTrackThumbnails = MutableStateFlow<List<String>>(emptyList())
    private var testPlaylistArtworkJob: Job? = null
    private var testPlaylistArtworkId: String? = null

    fun loadTestPlaylistArtwork(playlistId: String) {
        if (testPlaylistArtworkId == playlistId) return
        testPlaylistArtworkId = playlistId
        testPlaylistTrackThumbnails.value = emptyList()
        testPlaylistArtworkJob?.cancel()
        testPlaylistArtworkJob = viewModelScope.launch(Dispatchers.IO) {
            val thumbnails = YouTube.playlist(playlistId).getOrNull()
                ?.songs
                ?.take(4)
                ?.mapNotNull { it.thumbnail.takeIf(String::isNotBlank) }
                .orEmpty()
            if (testPlaylistArtworkId == playlistId) {
                testPlaylistTrackThumbnails.value = thumbnails
            }
        }
    }
    private var requestedMoodFilters: Triple<Boolean, Boolean, Boolean>? = null
    private val moodPageCache = MoodPageCache(context)

    private fun publishMoodMixes(items: List<PlaylistItem>, mood: MixMood?, revalidate: Boolean) {
        moodPosterJob?.cancel()
        val ids = items.map { it.id }.toSet()
        moodPosterUrls.update { urls -> urls.filterKeys { it in ids } }
        if (items.isEmpty()) {
            moodMixItems.value = items
            return
        }
        val size = moodPosterSize
        moodPosterJob = viewModelScope.launch(Dispatchers.IO) {
            moodPosters.prepare(items, mood ?: MixMood.CHILL, size, revalidate = false) { id, path ->
                coroutineContext.ensureActive()
                moodPosterUrls.update { if (path == null) it - id else it + (id to path) }
            }
            coroutineContext.ensureActive()
            moodMixItems.value = items
            if (!revalidate) return@launch
            var pending = items
            val delays = longArrayOf(0, 1_500, 4_000, 10_000)
            for (attempt in delays.indices) {
                if (delays[attempt] > 0) kotlinx.coroutines.delay(delays[attempt])
                val missing = moodPosters.prepare(pending, mood ?: MixMood.CHILL, size, revalidate) { id, path ->
                    coroutineContext.ensureActive()
                    moodPosterUrls.update { if (path == null) it - id else it + (id to path) }
                }
                pending = items.filter { it.id in missing }
                if (pending.isEmpty() || !revalidate) break
            }
        }
    }

    fun loadMoodPage(
        params: String?,
        chipTitle: String? = null,
        hideExplicit: Boolean,
        hideVideoSongs: Boolean,
        hideYoutubeShorts: Boolean,
        posterSize: Int = moodPosterSize,
        forceReload: Boolean = false,
    ) {
        val mood = MixMood.fromChip(chipTitle)
        val sizeChanged = moodPosterSize != posterSize.coerceIn(128, 768)
        moodPosterSize = posterSize.coerceIn(128, 768)
        val filters = Triple(hideExplicit, hideVideoSongs, hideYoutubeShorts)
        if (!forceReload && isMoodLoading.value && params == requestedMoodParams &&
            filters == requestedMoodFilters) return
        if (!forceReload && !isMoodError.value && params == loadedMoodParams.value &&
            loadedMoodFilters == filters && moodPage.value != null && !isMoodLoading.value) {
            if (sizeChanged) publishMoodMixes(moodMixItems.value.orEmpty(), mood, revalidate = false)
            return
        }
        requestedMoodParams = params
        requestedMoodFilters = filters
        isMoodError.value = false
        if (params == null || !MoodMixes.isAllowedCategory(chipTitle)) {
            moodPageJob?.cancel()
            moodPosterJob?.cancel()
            moodPosterUrls.value = emptyMap()
            moodPage.value = null
            moodMixItems.value = null
            isMoodLoading.value = false
            loadedMoodParams.value = null
            loadedMoodFilters = null
            return
        }
        moodPageJob?.cancel()
        moodPosterJob?.cancel()
        if (params != loadedMoodParams.value ||
            (loadedMoodFilters != null && filters != loadedMoodFilters)) {
            moodPage.value = null
            moodMixItems.value = null
            moodPosterUrls.value = emptyMap()
            loadedMoodParams.value = null
            loadedMoodFilters = null
        }
        isMoodLoading.value = true
        val cacheFile = moodPageCache.file(chipTitle.orEmpty(), filters)
        moodPageJob = viewModelScope.launch(Dispatchers.IO, start = kotlinx.coroutines.CoroutineStart.LAZY) {
            val myJob = coroutineContext[kotlinx.coroutines.Job]
            try {
                moodPageCache.read(cacheFile)?.let { cached ->
                    coroutineContext.ensureActive()
                    if (moodPageJob !== myJob) return@launch
                    moodPage.value = cached
                    loadedMoodParams.value = params
                    loadedMoodFilters = filters
                    publishMoodMixes(MoodMixes.select(cached.sections.flatMap { it.items }), mood, revalidate = false)
                }
                val result = Result.success(MoodLoadRetry.load {
                    withTimeout(25_000L) {
                        // Chip parameters belong to the current YouTube session. Resolve them
                        // again so waking the phone cannot reuse an expired filter endpoint.
                        val freshHome = YouTube.home().getOrThrow()
                        val chip = MoodMixes.chips(freshHome.chips).firstOrNull {
                            it.title.equals(chipTitle, ignoreCase = true)
                        } ?: error("Requested mood is missing from YouTube")
                        val page = YouTube.home(params = chip.endpoint?.params).getOrThrow()
                        val selected = page.chips?.firstOrNull { it.isSelected }
                        check(selected == null ||
                            (MixMood.fromChip(selected.title)?.label ?: selected.title)
                                .equals(chipTitle, ignoreCase = true)) { "YouTube returned a different mood" }
                        check(MoodMixes.select(page.sections.flatMap { it.items }).isNotEmpty()) {
                            "YouTube returned no mood mixes"
                        }
                        page
                    }
                })
                result.onSuccess { nextSections ->
                    coroutineContext.ensureActive()
                    if (moodPageJob !== myJob || params != requestedMoodParams ||
                        filters != requestedMoodFilters) return@onSuccess
                    val filteredPage = nextSections.copy(
                        sections = nextSections.sections.mapNotNull { section ->
                            val filteredItems = section.items
                                .filterExplicit(hideExplicit)
                                .filterVideoSongs(hideVideoSongs)
                                .filterYoutubeShorts(hideYoutubeShorts)
                            if (filteredItems.isEmpty()) null else section.copy(items = filteredItems)
                        }
                    )
                    moodPage.value = filteredPage
                    val mixItems = MoodMixes.select(filteredPage.sections.flatMap { it.items })
                    loadedMoodParams.value = params
                    loadedMoodFilters = filters
                    publishMoodMixes(mixItems, mood, revalidate = true)
                    preferredMoodParams.value = params
                    moodPageCache.write(cacheFile, filteredPage)
                    if (chipTitle != null) {
                        context.dataStore.edit { prefs ->
                            prefs[LastMoodChipTitleKey] = chipTitle
                            prefs[LastMoodChipParamsKey] = params
                        }
                        saveMoodSnapshotAfterLoad(chipTitle, params, filteredPage)
                    }
                }
            } catch (error: Exception) {
                coroutineContext.ensureActive()
                if (moodPageJob === myJob) isMoodError.value = true
                Timber.w(error, "Mood playlists will retry automatically")
            } finally {
                if (moodPageJob === myJob) isMoodLoading.value = false
            }
        }
        moodPageJob?.start()
    }

    val pinnedSpeedDialItems: StateFlow<List<SpeedDialItem>> =
        database.speedDialDao.getAll()
            .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val speedDialItems: StateFlow<List<YTItem>> =
        combine(
            database.speedDialDao.getAll(),
            keepListening,
            quickPicks,
            isPhase1Complete,
            homePage
        ) { pinned: List<SpeedDialItem>, keepListeningItems: List<LocalItem>?, quickPickItems: List<Song>?, phase1Done: Boolean, home: HomePage? ->
            val pinnedItems = pinned.map { it.toYTItem() }
            if (!phase1Done) return@combine pinnedItems
            val filled = pinnedItems.toMutableList()
            val targetSize = 27
            val kl = keepListeningItems ?: emptyList()
            val qp = quickPickItems ?: emptyList()

            if (filled.size < targetSize) {
                val needed = targetSize - filled.size
                val available = kl.filter { item ->
                    filled.none { p -> p.id == item.id }
                }.mapNotNull { item ->
                    when (item) {
                        is Song -> SongItem(
                            id = item.id,
                            title = item.title,
                            artists = item.artists.map { YTArtist(name = it.name, id = it.id) },
                            thumbnail = item.thumbnailUrl ?: "",
                            explicit = false
                        )
                        is Album -> AlbumItem(
                            browseId = item.id,
                            playlistId = item.album.playlistId ?: "",
                            title = item.title,
                            artists = item.artists.map { YTArtist(name = it.name, id = it.id) },
                            year = item.album.year,
                            thumbnail = item.thumbnailUrl ?: ""
                        )
                        else -> null
                    }
                }
                filled.addAll(available.take(needed))
            }

            if (filled.size < targetSize && home != null) {
                val needed = targetSize - filled.size
                val homeSongs = home.sections.flatMap { it.items }
                    .filterIsInstance<SongItem>()
                    .filter { item -> filled.none { p -> p.id == item.id } }
                filled.addAll(homeSongs.take(needed))
            }

            if (filled.size < targetSize) {
                val needed = targetSize - filled.size
                val available = qp.filter { song ->
                    filled.none { p -> p.id == song.id }
                }.map { song ->
                    SongItem(
                        id = song.id,
                        title = song.title,
                        artists = song.artists.map { YTArtist(name = it.name, id = it.id) },
                        thumbnail = song.thumbnailUrl ?: "",
                        explicit = false
                    )
                }
                filled.addAll(available.take(needed))
            }

            val albumIdMap = mutableMapOf<String, String?>()
            kl.forEach { item ->
                when (item) {
                    is Song -> albumIdMap[item.id] = item.album?.id
                    is Album -> albumIdMap[item.id] = item.id
                    else -> {}
                }
            }
            qp.forEach { song -> albumIdMap[song.id] = song.album?.id }

            diversifyByAlbum(filled.take(targetSize), albumIdMap, 6)
        }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    @Suppress("UNCHECKED_CAST")
    val homeSections: StateFlow<List<HomeSection>> = combine(
        quickPicks, keepListening, forgottenFavorites, similarRecommendations,
        homePage, communityPlaylists, dailyDiscover, accountPlaylists,
        selectedChip, randomizeHomeOrder, randomSeed, cachedSpeedDialSnapshot
    ) { args ->
        val quickPicks = args[0] as List<Song>?
        val keepListening = args[1] as List<LocalItem>?
        val forgottenFavorites = args[2] as List<Song>?
        val similarRecommendations = args[3] as List<SimilarRecommendation>?
        val homePage = args[4] as HomePage?
        val communityPlaylists = args[5] as List<CommunityPlaylistItem>?
        val dailyDiscover = args[6] as List<DailyDiscoverItem>?
        val accountPlaylists = args[7] as List<PlaylistItem>?
        val selectedChip = args[8] as HomePage.Chip?
        val randomizeHomeOrder = args[9] as Boolean
        val randomSeed = args[10] as Long
        val cachedSpeedDialSnap = args[11] as SpeedDialSnapshot?
        val hasCachedSpeedDial = cachedSpeedDialSnap?.items?.isNotEmpty() == true

        val list = mutableListOf<HomeSection>()
        val chipActive = selectedChip != null

        list.add(HomeSection.SpeedDial)

        if (quickPicks?.isNotEmpty() == true) list.add(HomeSection.QuickPicks)
        if (communityPlaylists?.isNotEmpty() == true) list.add(HomeSection.FromTheCommunity)
        if (dailyDiscover?.isNotEmpty() == true) list.add(HomeSection.DailyDiscover)
        if (keepListening?.isNotEmpty() == true) list.add(HomeSection.KeepListening)
        if (accountPlaylists?.isNotEmpty() == true) list.add(HomeSection.AccountPlaylists)
        if (forgottenFavorites?.isNotEmpty() == true) list.add(HomeSection.ForgottenFavorites)

        similarRecommendations?.indices?.forEach { i ->
            list.add(HomeSection.SimilarRecommendation(i))
        }

        val homePageSections = homePage?.sections.orEmpty()
        homePageSections.indices.filter { i ->
            !homePageSections[i].items.any { it.isMixtape }
        }.forEach { i ->
            list.add(HomeSection.HomePageSection(i))
        }

        val sortedList =
            if (randomizeHomeOrder) {
                list.sortedByDescending { section ->
                    val sectionRandom = Random(randomSeed + section.id.hashCode())
                    val base =
                        when (section) {
                            HomeSection.SpeedDial,
                            HomeSection.QuickPicks,
                            HomeSection.DailyDiscover,
                            -> 500
                            HomeSection.KeepListening,
                            HomeSection.AccountPlaylists,
                            HomeSection.ForgottenFavorites,
                            HomeSection.FromTheCommunity,
                            -> 300
                            is HomeSection.HomePageSection -> {
                                if (chipActive && homePageSections.getOrNull(section.index)?.items?.any { it.isMixtape } == true) 1000 else 100
                            }
                            else -> 100
                        }
                    val modifier =
                        when (section) {
                            HomeSection.SpeedDial,
                            HomeSection.QuickPicks,
                            HomeSection.DailyDiscover,
                            -> sectionRandom.nextInt(-200, 400)
                            HomeSection.KeepListening,
                            HomeSection.AccountPlaylists,
                            HomeSection.ForgottenFavorites,
                            HomeSection.FromTheCommunity,
                            -> sectionRandom.nextInt(-100, 400)
                            is HomeSection.HomePageSection -> sectionRandom.nextInt(-50, 50)
                            else -> sectionRandom.nextInt(-50, 50)
                        }
                    base + modifier
                }
            } else {
                val defaultOrder =
                    mapOf(
                        HomeSection.SpeedDial to 100,
                        HomeSection.QuickPicks to 90,
                        HomeSection.FromTheCommunity to 80,
                        HomeSection.DailyDiscover to 70,
                        HomeSection.KeepListening to 60,
                        HomeSection.AccountPlaylists to 50,
                        HomeSection.ForgottenFavorites to 40,
                    )
                list.sortedByDescending { section ->
                    when (section) {
                        is HomeSection.SimilarRecommendation -> 30 - section.index
                        is HomeSection.HomePageSection -> {
                            if (chipActive && homePageSections.getOrNull(section.index)?.items?.any { it.isMixtape } == true)
                                1000 - section.index
                            else
                                20 - section.index
                        }
                        else -> defaultOrder[section] ?: 0
                    }
                }
            }

        val finalItems = mutableListOf<HomeSection>()
        if (hasCachedSpeedDial && list.contains(HomeSection.SpeedDial)) finalItems.add(HomeSection.SpeedDial)
        if (list.contains(HomeSection.QuickPicks)) finalItems.add(HomeSection.QuickPicks)

        finalItems.addAll(sortedList.filter { section ->
            !(hasCachedSpeedDial && section == HomeSection.SpeedDial) &&
            section != HomeSection.QuickPicks
        })
        finalItems
    }.distinctUntilChanged().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private fun diversifyByAlbum(items: List<YTItem>, albumIdMap: Map<String, String?>, pageSize: Int): List<YTItem> {
        val result = mutableListOf<YTItem>()
        val remaining = items.toMutableList()
        while (remaining.isNotEmpty()) {
            val page = mutableListOf<YTItem>()
            val usedAlbumIds = mutableSetOf<String>()
            val deferred = mutableListOf<YTItem>()
            for (item in remaining) {
                if (page.size >= pageSize) break
                val albumId = albumIdMap[item.id]
                if (albumId == null || albumId !in usedAlbumIds) {
                    page.add(item)
                    if (albumId != null) usedAlbumIds.add(albumId)
                } else {
                    deferred.add(item)
                }
            }
            val needed = pageSize - page.size
            page.addAll(deferred.take(needed))
            val usedIds = page.map { it.id }.toSet()
            remaining.removeAll { it.id in usedIds }
            result.addAll(page)
        }
        return result
    }

    suspend fun getRandomItem(): YTItem? {
        try {
            isRandomizing.value = true

            val userSongs = mutableListOf<YTItem>()
            val otherSources = mutableListOf<YTItem>()

            quickPicks.value?.let { songs ->
                userSongs.addAll(songs.map { song ->
                    SongItem(
                        id = song.id,
                        title = song.title,
                        artists = song.artists.map { YTArtist(name = it.name, id = it.id) },
                        thumbnail = song.thumbnailUrl ?: "",
                        explicit = false
                    )
                })
            }

            keepListening.value?.let { items ->
                items.forEach { item ->
                    when (item) {
                        is Song -> userSongs.add(SongItem(
                            id = item.id,
                            title = item.title,
                            artists = item.artists.map { YTArtist(name = it.name, id = it.id) },
                            thumbnail = item.thumbnailUrl ?: "",
                            explicit = false
                        ))
                        is Album -> otherSources.add(AlbumItem(
                            browseId = item.id,
                            playlistId = item.album.playlistId ?: "",
                            title = item.title,
                            artists = item.artists.map { YTArtist(name = it.name, id = it.id) },
                            year = item.album.year,
                            thumbnail = item.thumbnailUrl ?: ""
                        ))
                        else -> {}
                    }
                }
            }

            otherSources.addAll(allYtItems.value)

            val item = if (userSongs.isNotEmpty() && (otherSources.isEmpty() || Random.nextFloat() < 0.8f)) {
                userSongs.distinctBy { it.id }.shuffled().firstOrNull()
            } else {
                otherSources.distinctBy { it.id }.shuffled().firstOrNull()
            } ?: userSongs.firstOrNull() ?: otherSources.firstOrNull()

            return item
        } finally {
            isRandomizing.value = false
        }
    }

    val accountName = MutableStateFlow("Guest")
    val accountImageUrl = MutableStateFlow<String?>(null)

	val showWrappedCard: StateFlow<Boolean> = context.dataStore.data.map { prefs ->
        val showWrappedPref = prefs[ShowWrappedCardKey] ?: false
        val seen = prefs[WrappedSeenKey] ?: false
        val isBeforeDate = LocalDate.now().isBefore(LocalDate.of(2026, 2, 1))

        isBeforeDate && (!seen || showWrappedPref)
    }.stateIn(viewModelScope, SharingStarted.Lazily, false)

    val wrappedSeen: StateFlow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[WrappedSeenKey] ?: false
    }.stateIn(viewModelScope, SharingStarted.Lazily, false)

    fun togglePin(item: YTItem) {
        viewModelScope.launch(Dispatchers.IO) {
            val speedDialItem = SpeedDialItem.fromYTItem(item)
            val isPinned = database.speedDialDao.isPinned(speedDialItem.id).first()
            if (isPinned) {
                database.speedDialDao.delete(speedDialItem.id)
            } else {
                database.speedDialDao.insert(speedDialItem)
            }
        }
    }

    fun markWrappedAsSeen() {
        viewModelScope.launch(Dispatchers.IO) {
            context.dataStore.edit {
                it[WrappedSeenKey] = true
            }
        }
    }
    private var lastProcessedCookie: String? = null
    private var isProcessingAccountData = false

    private var dailyDiscoverLaunchJob: kotlinx.coroutines.Job? = null
    private var communityPlaylistsLaunchJob: kotlinx.coroutines.Job? = null
    private var similarRecommendationsLaunchJob: kotlinx.coroutines.Job? = null
    private var dischiPerTeLaunchJob: kotlinx.coroutines.Job? = null

    val phase2DailyDiscoverDone = MutableStateFlow(false)
    val phase2CommunityDone = MutableStateFlow(false)
    val phase2SimilarDone = MutableStateFlow(false)
    val phase2DischiPerTeDone = MutableStateFlow(false)

    private fun checkPhase2Complete() {
        if (phase2DailyDiscoverDone.value && phase2CommunityDone.value &&
            phase2SimilarDone.value && phase2DischiPerTeDone.value
        ) {
            phase2Complete.value = true
        }
    }

    /**
     * One mini 2x2 block per artist (most-listened, last 30 days, or followed via the star):
     * artist photo + 3 album tiles, never songs. An artist's most-listened albums come first;
     * if listening history doesn't cover 3 albums, the rest of that artist's credited discography
     * fills the remaining tiles — their own albums plus albums they're featured on, not
     * necessarily listened to. Artists who still can't fill all 3 tiles are dropped rather than
     * shown as an incomplete box. Followed artists are oversampled so starred artists show up
     * often even when they're not in the top listening history.
     */
    private suspend fun buildForYouShelf(
        artist: Artist,
        fromTimeStamp: Long,
        toTimeStamp: Long,
        usedAlbumIds: Set<String>,
    ): ForYouShelfItem? {
        val tiles = mutableListOf<ForYouShelfTile>()
        val hideExplicit = context.dataStore.get(HideExplicitKey, false)

        val candidateSongs = database.mostPlayedSongsByArtist(artist.id, fromTimeStamp, toTimeStamp)
            .first()
        val artistAlbums = linkedMapOf<String, Album>()
        for (song in candidateSongs) {
            val albumId = song.album?.id ?: continue
            val album = database.album(albumId).first() ?: continue
            if (album.album.songCount > 1 && (!album.album.explicit || !hideExplicit)) {
                artistAlbums.putIfAbsent(albumId, album)
            }
        }

        database.artistCreditedAlbumsPreview(artist.id, previewSize = 30).first()
            .shuffled()
            .filter { it.album.songCount > 1 && (!it.album.explicit || !hideExplicit) }
            .forEach { artistAlbums.putIfAbsent(it.id, it) }

        artistAlbums.values.filterNot { it.id in usedAlbumIds }.shuffled().take(2).forEach { album ->
            tiles += ForYouShelfTile.Local(album)
        }

        artistAlbums.values.shuffled()
            .filterNot { album -> album.id in usedAlbumIds || tiles.any { it.id == album.id } }
            .take(3 - tiles.size)
            .forEach { tiles += ForYouShelfTile.Local(it) }

        return if (tiles.size < 3) null else ForYouShelfItem(artist, tiles)
    }

    private suspend fun buildForYouArtistPool(): List<Artist> {
        val fromTimeStamp = System.currentTimeMillis() - 86400000L * 30
        val toTimeStamp = System.currentTimeMillis()
        val listenedArtists = database.mostPlayedArtists(fromTimeStamp, limit = 25, toTimeStamp = toTimeStamp).first()
        val followedArtists = database.artistsBookmarked(ArtistSortType.CREATE_DATE, true).first()
        val libraryArtists = database.artists(ArtistSortType.PLAY_TIME, true).first()
        return (listenedArtists + followedArtists + followedArtists + libraryArtists)
            .filter { it.artist.thumbnailUrl != null }
            .distinctBy { it.id }
            .shuffled()
    }

    private suspend fun getForYouShelves() {
        forYouArtistPool = buildForYouArtistPool()
        forYouPoolCursor = 0
        val fromTimeStamp = System.currentTimeMillis() - 86400000L * 30
        val toTimeStamp = System.currentTimeMillis()

        val shelves = mutableListOf<ForYouShelfItem>()
        val usedArtistIds = mutableSetOf<String>()
        val usedAlbumIds = mutableSetOf<String>()
        while (shelves.size < 20 && forYouPoolCursor < forYouArtistPool.size) {
            val artist = forYouArtistPool[forYouPoolCursor]
            forYouPoolCursor++
            if (usedArtistIds.add(artist.id)) {
                buildForYouShelf(artist, fromTimeStamp, toTimeStamp, usedAlbumIds)?.let { shelf ->
                    usedAlbumIds += shelf.tiles.map { it.id }
                    shelves += shelf
                    // Present the first viewport without waiting for the entire twenty-artist pool.
                    if (shelves.size % 4 == 0) forYouShelves.value = shelves.toList()
                }
            }
        }
        forYouShelves.value = shelves
        HomeCache.forYouShelves = shelves
        scheduleForYouRecommendations()
    }

    private fun scheduleForYouRecommendations() {
        forYouEnrichmentJob?.cancel()
        forYouEnrichmentJob = viewModelScope.launch(Dispatchers.IO) {
            val albumIds = forYouShelves.value.flatMap { it.tiles }.mapTo(mutableSetOf()) { it.id }
            // Serialized enrichment: shares the InnerTube pool (5) with home/account
            // reloads, so a burst here must never starve them after a network switch.
            val semaphore = Semaphore(1)
            forYouShelves.value.forEach { shelf ->
                launch {
                    val alreadyUsed = synchronized(albumIds) { albumIds.toSet() }
                    val recommended = semaphore.withPermit {
                        findRelatedAlbum(shelf.artist.id, alreadyUsed)
                    } ?: return@launch

                    synchronized(albumIds) {
                        val shelves = forYouShelves.value
                        val index = shelves.indexOfFirst { it.artist.id == shelf.artist.id }
                        if (index < 0 || recommended.id in albumIds) return@synchronized
                        val current = shelves[index]
                        val replaceIndex = current.tiles.indexOfLast { it is ForYouShelfTile.Local }
                        if (replaceIndex < 0) return@synchronized
                        val updatedTiles = current.tiles.toMutableList()
                        albumIds.remove(updatedTiles[replaceIndex].id)
                        albumIds.add(recommended.id)
                        updatedTiles[replaceIndex] = ForYouShelfTile.RecommendedAlbum(recommended)
                        forYouShelves.value = shelves.toMutableList().also {
                            it[index] = current.copy(tiles = updatedTiles)
                        }
                        HomeCache.forYouShelves = forYouShelves.value
                    }
                }
            }
        }
    }

    private suspend fun findRelatedAlbum(artistId: String, usedAlbumIds: Set<String>): AlbumItem? {
        val hideExplicit = context.dataStore.get(HideExplicitKey, false)
        val relatedArtists = YouTube.artist(artistId).getOrNull()
            ?.sections?.flatMap { it.items }
            ?.filterIsInstance<ArtistItem>()
            ?.filterNot { it.id == artistId }
            ?.distinctBy { it.id }
            ?.shuffled()
            .orEmpty()

        for (artist in relatedArtists.take(1)) {
            val albums = YouTube.artist(artist.id).getOrNull()
                ?.sections?.flatMap { it.items }
                ?.filterIsInstance<AlbumItem>()
                ?.filter { it.id !in usedAlbumIds && (!it.explicit || !hideExplicit) }
                ?.filterNot { it.albumType?.trim()?.lowercase() in setOf("single", "singolo") }
                ?.shuffled()
                .orEmpty()
            if (albums.isNotEmpty()) return albums.first()
        }
        return null
    }

    fun loadMoreForYouShelves() {
        if (isForYouLoading.value || _isLoadingMoreForYou.value || forYouArtistPool.isEmpty()) return
        viewModelScope.launch(Dispatchers.IO) {
            _isLoadingMoreForYou.value = true
            val fromTimeStamp = System.currentTimeMillis() - 86400000L * 30
            val toTimeStamp = System.currentTimeMillis()
            val newShelves = mutableListOf<ForYouShelfItem>()
            val usedArtistIds = forYouShelves.value.mapTo(mutableSetOf()) { it.artist.id }
            val usedAlbumIds = forYouShelves.value.flatMap { it.tiles }.mapTo(mutableSetOf()) { it.id }
            var scanned = 0
            while (newShelves.size < 10 && scanned < forYouArtistPool.size) {
                if (forYouPoolCursor >= forYouArtistPool.size) {
                    forYouArtistPool = forYouArtistPool.shuffled()
                    forYouPoolCursor = 0
                }
                val artist = forYouArtistPool[forYouPoolCursor]
                forYouPoolCursor++
                scanned++
                if (usedArtistIds.add(artist.id)) {
                    buildForYouShelf(artist, fromTimeStamp, toTimeStamp, usedAlbumIds)?.let { shelf ->
                        usedAlbumIds += shelf.tiles.map { it.id }
                        newShelves += shelf
                    }
                }
            }
            if (newShelves.isNotEmpty()) {
                val combined = forYouShelves.value + newShelves
                forYouShelves.value = combined
                HomeCache.forYouShelves = combined
                scheduleForYouRecommendations()
            }
            _isLoadingMoreForYou.value = false
        }
    }

    fun regenerateForYouShelves() {
        viewModelScope.launch(Dispatchers.IO) {
            regenerateSection(
                key = "for_you_shelf",
                currentIds = {
                    forYouShelves.value.orEmpty()
                        .flatMap { listOf(it.artist.id) + it.tiles.map { tile -> tile.id } }
                        .toSet()
                },
                generate = { getForYouShelves() },
            )
        }
    }

    private suspend fun getDischiPerTe() {
        val hideExplicit = context.dataStore.get(HideExplicitKey, false)
        dischiPerTe.value = albumRecommendationsGenerator.generate(
            explorePage = explorePage.value,
            hideExplicit = hideExplicit,
            seed = System.currentTimeMillis(),
        )
        HomeCache.dischiPerTe = dischiPerTe.value
        refreshHeroCarousel()
    }

    private suspend fun getDailyDiscover() {
        val likedSongs = database.likedSongsByCreateDateAsc().first()
        if (likedSongs.isEmpty()) return

        val seeds = likedSongs.shuffled().distinctBy { it.id }.take(5)
        val items = java.util.Collections.synchronizedList(mutableListOf<DailyDiscoverItem>())

        kotlinx.coroutines.coroutineScope {
            seeds.map { seed ->
                launch(Dispatchers.IO) {
                    val endpoint = YouTube.next(WatchEndpoint(videoId = seed.id)).getOrNull()?.relatedEndpoint
                    if (endpoint != null) {
                        YouTube.related(endpoint).onSuccess { page ->
                            val recommendations = page.songs
                                .filter { item ->
                                    if (item.isVideoSong) return@filter false
                                    if (item.explicit) return@filter false
                                    true
                                }
                                .shuffled()

                            val recommendation = recommendations.firstOrNull { rec ->
                                rec.id != seed.id
                            }

                            if (recommendation != null) {
                                items.add(
                                    DailyDiscoverItem(
                                        seed = seed,
                                        recommendation = recommendation,
                                        relatedEndpoint = endpoint
                                    )
                                )
                                synchronized(items) {
                                    dailyDiscover.value = items.toList().distinctBy { it.recommendation.id }
                                }
                            }
                        }
                    }
                }
            }.forEach { it.join() }
        }
        dailyDiscover.value = items.toList().distinctBy { it.recommendation.id }
    }

    private fun eligibleQuickPicks(songs: List<Song>, hideExplicit: Boolean): List<Song> =
        songs.filter { !it.song.isVideo && !it.song.isEpisode && (!hideExplicit || !it.song.explicit) }
            .distinctBy { it.id }

    private suspend fun quickPickSong(item: SongItem): Song =
        database.song(item.id).first() ?: Song(
            song = item.toMediaMetadata().toSongEntity(),
            artists = item.artists.map { artist ->
                ArtistEntity(id = artist.id ?: ArtistEntity.generateArtistId(), name = artist.name)
            },
        )

    private suspend fun fillQuickPicksFromHome(page: HomePage, hideExplicit: Boolean) {
        if (quickPicks.value.orEmpty().size >= QUICK_PICKS_TARGET_COUNT) return
        val homeSongs = page.sections.flatMap { it.items }
            .filterIsInstance<SongItem>()
            .filter { !it.isVideoSong && !it.isEpisode && (!hideExplicit || !it.explicit) }
            .distinctBy { it.id }
            .shuffled()
        val songs = eligibleQuickPicks(quickPicks.value.orEmpty(), hideExplicit).toMutableList()
        val usedIds = songs.mapTo(mutableSetOf()) { it.id }
        for (item in homeSongs) {
            if (songs.size >= QUICK_PICKS_TARGET_COUNT) break
            if (item.id in usedIds) continue
            val song = quickPickSong(item)
            if (eligibleQuickPicks(listOf(song), hideExplicit).isNotEmpty() && usedIds.add(song.id)) {
                songs += song
            }
        }
        quickPicks.update { current ->
            eligibleQuickPicks(current.orEmpty() + songs, hideExplicit).take(QUICK_PICKS_TARGET_COUNT)
        }
    }

    private suspend fun getQuickPicks() {
        val mode = quickPicksEnum.first()
        val hideExplicit = context.dataStore.get(HideExplicitKey, false)
        val previous = eligibleQuickPicks(quickPicks.value.orEmpty(), hideExplicit)
        val primary = when (mode) {
            QuickPicks.QUICK_PICKS -> database.quickPicks().first()
            QuickPicks.LAST_LISTEN -> database.latestEvent().first()?.song?.let { song ->
                database.getRelatedSongs(song.id).first()
            }.orEmpty()
        }
        val songs = eligibleQuickPicks(primary, hideExplicit).shuffled().toMutableList()
        if (mode == QuickPicks.QUICK_PICKS && songs.size < QUICK_PICKS_TARGET_COUNT) {
            songs += eligibleQuickPicks(database.forgottenFavorites().first(), hideExplicit).shuffled()
            if (songs.distinctBy { it.id }.size < QUICK_PICKS_TARGET_COUNT) {
                songs += eligibleQuickPicks(
                    database.mostPlayedSongs(0L, limit = QUICK_PICKS_TARGET_COUNT * 3).first(),
                    hideExplicit,
                ).shuffled()
            }
        }
        quickPicks.value = songs.distinctBy { it.id }.take(QUICK_PICKS_TARGET_COUNT)
        if (mode == QuickPicks.QUICK_PICKS) {
            homePage.value?.let { fillQuickPicksFromHome(it, hideExplicit) }
            quickPicks.update { current ->
                (current.orEmpty() + previous.shuffled()).distinctBy { it.id }.take(QUICK_PICKS_TARGET_COUNT)
            }
        }
    }

    private suspend fun enrichQuickPicksFromNetwork() {
        if (quickPicks.value.orEmpty().size >= QUICK_PICKS_TARGET_COUNT) return
        val mode = quickPicksEnum.first()
        val hideExplicit = context.dataStore.get(HideExplicitKey, false)
        val recentSong = database.latestEvent().first()?.song
        val seeds = (if (mode == QuickPicks.LAST_LISTEN) {
            listOfNotNull(recentSong)
        } else {
            listOfNotNull(recentSong) + database.mostPlayedSongs(0L, limit = 12).first().shuffled()
        }).filter { !it.song.isVideo && !it.song.isEpisode }.distinctBy { it.id }.take(3)
        for (seed in seeds) {
            currentCoroutineContext().ensureActive()
            if (quickPicks.value.orEmpty().size >= QUICK_PICKS_TARGET_COUNT) break
            // Fetch sequentially and bound each seed so enrichment cannot flood the shared network pool.
            val related = withTimeoutOrNull(12_000L) {
                val endpoint = YouTube.next(WatchEndpoint(videoId = seed.id)).getOrNull()?.relatedEndpoint
                    ?: return@withTimeoutOrNull null
                YouTube.related(endpoint).getOrNull()
            }
            currentCoroutineContext().ensureActive()
            if (related == null) continue
            val existingIds = quickPicks.value.orEmpty().mapTo(mutableSetOf()) { it.id }
            val candidates = related.songs
                .filter { it.id != seed.id && !it.isVideoSong && !it.isEpisode && (!hideExplicit || !it.explicit) }
                .distinctBy { it.id }
                .filterNot { it.id in existingIds }
                .shuffled()
                .take(QUICK_PICKS_TARGET_COUNT - existingIds.size)
                .map { quickPickSong(it) }
            quickPicks.update { current ->
                eligibleQuickPicks(current.orEmpty() + candidates, hideExplicit)
                    .take(QUICK_PICKS_TARGET_COUNT)
            }
        }
    }

    private suspend fun getCommunityPlaylists() {
        val fromTimeStamp = System.currentTimeMillis() - 86400000L * 7 * 4
        val artistSeeds = database.mostPlayedArtists(fromTimeStamp, limit = 10).first()
            .filter { it.artist.isYouTubeArtist }
            .shuffled().take(3)
        val songSeeds = database.mostPlayedSongs(fromTimeStamp, limit = 5).first()
            .shuffled().take(2)

        val candidatePlaylists = java.util.Collections.synchronizedList(mutableListOf<PlaylistItem>())

        kotlinx.coroutines.coroutineScope {
            artistSeeds.map { seed ->
                launch(Dispatchers.IO) {
                    YouTube.artist(seed.id).onSuccess { page ->
                        page.sections.forEach { section ->
                            section.items.filterIsInstance<PlaylistItem>().forEach { playlist ->
                                if (playlist.author?.name != "YouTube Music" && 
                                    playlist.author?.name != "YouTube" && 
                                    playlist.author?.name != "Playlist" &&
                                    playlist.author?.name != seed.artist.name &&
                                    !playlist.id.startsWith("RD") &&
                                    !playlist.id.startsWith("OLAK")
                                ) {
                                    candidatePlaylists.add(playlist)
                                }
                            }
                        }
                    }
                }
            }
            songSeeds.map { seed ->
                launch(Dispatchers.IO) {
                    val endpoint = YouTube.next(WatchEndpoint(videoId = seed.id)).getOrNull()?.relatedEndpoint
                    if (endpoint != null) {
                        YouTube.related(endpoint).onSuccess { page ->
                            page.playlists.forEach { playlist ->
                                if (playlist.author?.name != "YouTube Music" && 
                                    playlist.author?.name != "YouTube" && 
                                    playlist.author?.name != "Playlist" &&
                                    !playlist.id.startsWith("RD") &&
                                    !playlist.id.startsWith("OLAK")
                                ) {
                                    candidatePlaylists.add(playlist)
                                }
                            }
                        }
                    }
                }
            }
        }

        val uniqueCandidates = candidatePlaylists.distinctBy { it.id }.shuffled().take(5)

        val playlists = java.util.Collections.synchronizedList(mutableListOf<CommunityPlaylistItem>())

        kotlinx.coroutines.coroutineScope {
            uniqueCandidates.map { playlist ->
                launch(Dispatchers.IO) {
                    YouTube.playlist(playlist.id).onSuccess { page ->
                        val songs = page.songs.take(10)
                        if (songs.isNotEmpty()) {
                            val songCountText = page.playlist.songCountText ?: playlist.songCountText
                            val updatedPlaylist = playlist.copy(songCountText = songCountText)
                            playlists.add(CommunityPlaylistItem(updatedPlaylist, songs))
                        }
                    }
                }
            }.forEach { it.join() }
        }

        communityPlaylists.value = playlists.shuffled()
    }

    private suspend fun getSimilarRecommendations(onlySeedId: String? = null) = coroutineScope {
        val hideExplicit = context.dataStore.get(HideExplicitKey, false)
        val fromTimeStamp = System.currentTimeMillis() - 86400000L * 14
        val artists = (
            database.mostPlayedArtists(fromTimeStamp, limit = 40).first() +
                database.artistsBookmarked(ArtistSortType.CREATE_DATE, descending = true).first() +
                database.artists(ArtistSortType.PLAY_TIME, descending = true).first()
            ).distinctBy { it.id }.filter { it.artist.isYouTubeArtist && !it.thumbnailUrl.isNullOrBlank() }
            .shuffled().take(20)
        val albums = (
            database.mostPlayedAlbums(fromTimeStamp, limit = 40).first() +
                database.albums(AlbumSortType.PLAY_TIME, descending = true).first()
            ).distinctBy { it.id }.filter { !it.thumbnailUrl.isNullOrBlank() && it.artists.isNotEmpty() }
            .shuffled().take(20)
        val playlists = database.playlists(PlaylistSortType.LAST_UPDATED, descending = true).first()
            .filter { !it.playlist.isLocal && !it.playlist.browseId.isNullOrBlank() && it.thumbnails.isNotEmpty() }
            .distinctBy { it.playlist.browseId }.shuffled().take(20)

        val localSeeds = buildList<LocalItem> {
            repeat(maxOf(artists.size, albums.size, playlists.size)) { index ->
                artists.getOrNull(index)?.let(::add)
                albums.getOrNull(index)?.let(::add)
                playlists.getOrNull(index)?.let(::add)
            }
        }
        val generated: MutableList<SimilarRecommendation> = similarRecommendations.value.orEmpty()
            .filter { it.title is Artist || it.title is Album || it.title is Playlist }
            .filter { recommendation ->
                !recommendation.thumbnailUrl.isNullOrBlank() &&
                    recommendation.items.distinctBy { it.id }.count { item ->
                        item is SongItem && !item.isVideoSong || item is AlbumItem ||
                            item is ArtistItem || item is PlaylistItem
                    } >= 2
            }
                .distinctBy { it.title.id }.take(MAX_SIMILAR_RECOMMENDATIONS).toMutableList()
        val refreshSeed = onlySeedId?.let { id ->
            similarRecommendations.value.orEmpty().firstOrNull { it.title.id == id }?.title
        }
        val seedsToGenerate = if (refreshSeed != null) listOf(refreshSeed) else localSeeds
        val semaphore = Semaphore(4)
        val publishLock = kotlinx.coroutines.sync.Mutex()
        val shelfFallbacks = java.util.concurrent.ConcurrentHashMap<String, List<YTItem>>()

        suspend fun recommendationFor(seed: LocalItem): SimilarRecommendation? {
            val candidates: List<YTItem> = when (seed) {
                is Artist -> {
                    val page = YouTube.artist(seed.id).getOrNull() ?: return null
                    val related = page.sections.flatMap { it.items }.filterIsInstance<ArtistItem>()
                        .filterNot { it.id == seed.id }.distinctBy { it.id }
                    val extendedRelated = related.take(4).flatMap { artist ->
                        YouTube.artist(artist.id).getOrNull()?.sections.orEmpty()
                            .flatMap { it.items }.filterIsInstance<ArtistItem>()
                    }
                    (related + extendedRelated).distinctBy { it.id }
                }
                is Album -> {
                    val artist = seed.artists.first()
                    val page = YouTube.artist(artist.id).getOrNull() ?: return null
                    val related = page.sections.flatMap { it.items }.filterIsInstance<ArtistItem>()
                        .filterNot { it.id == artist.id }.distinctBy { it.id }
                    val relatedAlbums = related.firstOrNull()?.let { item ->
                        YouTube.artist(item.id).getOrNull()?.sections.orEmpty()
                            .flatMap { it.items }.filterIsInstance<AlbumItem>()
                    }.orEmpty()
                    relatedAlbums + related + page.sections.flatMap { it.items }.filterIsInstance<AlbumItem>()
                        .filterNot { it.id == seed.id }
                }
                is Playlist -> {
                    val page = YouTube.playlist(seed.playlist.browseId!!.removePrefix("VL")).getOrNull()
                        ?: return null
                    val anchor = page.songs.firstOrNull() ?: return null
                    val endpoint = YouTube.next(WatchEndpoint(videoId = anchor.id))
                        .getOrNull()?.relatedEndpoint ?: return null
                    val related = YouTube.related(endpoint).getOrNull() ?: return null
                    related.albums + related.artists + related.playlists + related.songs
                }
                else -> return null
            }
            val existingIds = generated.asSequence()
                .filterNot { it.title.id == seed.id }
                .flatMap { it.items.asSequence() }
                .map { it.id }
                .toSet()
            val items = candidates.distinctBy { it.id }
                .filterExplicit(hideExplicit)
                .filter { it is SongItem && !it.isVideoSong || it is AlbumItem || it is ArtistItem || it is PlaylistItem }
                .filterNot { it.id == seed.id }
                .filterNot { it.id in existingIds }
                .shuffled()
                .take(12)
            if (items.size < 2) return null
            return SimilarRecommendation(
                title = seed,
                thumbnailUrl = if (seed is Playlist) seed.thumbnails.firstOrNull() else seed.thumbnailUrl,
                items = items,
            )
        }

        suspend fun publishSeed(seed: LocalItem) {
            semaphore.withPermit {
                if (publishLock.withLock {
                        generated.size >= MAX_SIMILAR_RECOMMENDATIONS &&
                            generated.none { it.title.id == seed.id }
                    }) return@withPermit
                val recommendation = withTimeoutOrNull(if (shelfFallbacks.containsKey(seed.id)) 5_000 else 12_000) {
                    recommendationFor(seed)
                } ?: shelfFallbacks[seed.id]?.let { shelfItems ->
                    val items = shelfItems.distinctBy { it.id }
                        .filterExplicit(hideExplicit)
                        .filter { it is SongItem && !it.isVideoSong || it is AlbumItem || it is ArtistItem || it is PlaylistItem }
                        .filterNot { it.id == seed.id }
                        .take(12)
                    if (items.size < 2) null else SimilarRecommendation(
                        title = seed,
                        thumbnailUrl = if (seed is Playlist) seed.thumbnails.firstOrNull() else seed.thumbnailUrl,
                        items = items,
                    )
                } ?: return@withPermit
                publishLock.withLock {
                    val index = generated.indexOfFirst { it.title.id == seed.id }
                    if (index >= 0) generated[index] = recommendation
                    else if (generated.size < MAX_SIMILAR_RECOMMENDATIONS) generated += recommendation
                    val byType = listOf(
                        generated.filter { it.title is Artist },
                        generated.filter { it.title is Album },
                        generated.filter { it.title is Playlist },
                    )
                    val ordered = buildList {
                        repeat(byType.maxOf { it.size }) { position ->
                            byType.forEach { group -> group.getOrNull(position)?.let(::add) }
                        }
                    }.take(MAX_SIMILAR_RECOMMENDATIONS)
                    similarRecommendations.value = ordered
                    HomeCache.similarRecommendations = ordered
                }
            }
        }

        fun YTItem.toSimilarSeed(): LocalItem? = when (this) {
            is ArtistItem -> takeIf { id.startsWith("UC") && !thumbnail.isNullOrBlank() }?.let {
                Artist(ArtistEntity(id = id, name = title, thumbnailUrl = thumbnail), songCount = 0)
            }
            is AlbumItem -> takeIf { thumbnail.isNotBlank() }?.toAlbumEntity()
                ?.takeIf { it.artists.isNotEmpty() }
            is PlaylistItem -> takeIf { !isMixtape && !thumbnail.isNullOrBlank() }?.let {
                val browseId = id.removePrefix("VL")
                Playlist(
                    playlist = PlaylistEntity(
                        id = browseId,
                        name = title,
                        browseId = browseId,
                        thumbnailUrl = thumbnail,
                        isEditable = false,
                    ),
                    songCount = 0,
                    songThumbnails = emptyList(),
                )
            }
            else -> null
        }

        coroutineScope {
            seedsToGenerate.forEach { seed -> launch(Dispatchers.IO) { publishSeed(seed) } }
            if (onlySeedId == null) {
            launch(Dispatchers.IO) {
                val remoteIds = localSeeds.mapTo(mutableSetOf()) { it.id }
                val pageFromState = withTimeoutOrNull(5_000) {
                    homePage.filter { it?.sections?.isNotEmpty() == true }.first()
                }
                var page = pageFromState ?: withTimeoutOrNull(8_000) {
                    YouTube.home().getOrNull()
                } ?: return@launch
                val seenContinuations = mutableSetOf<String>()
                var requestedFreshPage = false
                repeat(HOME_SIMILAR_CONTINUATION_LIMIT + 1) {
                    if (publishLock.withLock { generated.size >= MAX_SIMILAR_RECOMMENDATIONS }) return@launch
                    val seeds = page.sections.flatMap { section ->
                        val peers = section.items.distinctBy { it.id }
                        section.items.mapNotNull { item ->
                            item.toSimilarSeed()?.also { seed ->
                                shelfFallbacks.putIfAbsent(seed.id, peers)
                            }
                        }
                    }.filter { remoteIds.add(it.id) }
                    seeds.forEach { seed -> launch(Dispatchers.IO) { publishSeed(seed) } }
                    if (remoteIds.size - localSeeds.size >= 50) return@launch
                    val continuation = page.continuation
                    page = when {
                        !continuation.isNullOrEmpty() && seenContinuations.add(continuation) ->
                            withTimeoutOrNull(8_000) { YouTube.home(continuation = continuation).getOrNull() }
                        !requestedFreshPage && pageFromState != null -> {
                            requestedFreshPage = true
                            withTimeoutOrNull(8_000) { YouTube.home().getOrNull() }
                        }
                        else -> null
                    } ?: return@launch
                }
            }
            }
        }
        allYtItems.value = similarRecommendations.value?.flatMap { it.items }.orEmpty() +
            homePage.value?.sections?.flatMap { it.items }.orEmpty()
        Timber.tag("HomeSimilar").d(
            "Published %d of %d panels from %d local seeds",
            similarRecommendations.value.orEmpty().size,
            MAX_SIMILAR_RECOMMENDATIONS,
            seedsToGenerate.size,
        )
    }

    /**
     * Re-runs a section's generator a few times, retrying while the result is empty or
     * identical (by id) to what was already on screen — so tapping refresh never leaves
     * the user staring at the same cards, and never clears a section down to nothing.
     */
    private suspend fun regenerateSection(
        key: String,
        currentIds: () -> Set<String>,
        generate: suspend () -> Unit,
    ) {
        if (key in regeneratingSections.value) return
        regeneratingSections.value += key
        try {
            val previousIds = currentIds()
            var attempt = 0
            while (attempt < 3) {
                generate()
                val newIds = currentIds()
                if (newIds.isNotEmpty() && newIds != previousIds) break
                attempt++
            }
        } finally {
            regeneratingSections.value -= key
        }
    }

    /**
     * Only surfaces the section when there are at least 8 eligible tracks (otherwise the Home
     * screen's `takeIf { it.isNotEmpty() }` gate would show it with as few as 1), and pulls from
     * a wider shuffled pool so repeated regenerations actually vary.
     */
    private suspend fun getForgottenFavorites() {
        val hideVideoSongs = context.dataStore.get(HideVideoSongsKey, false)
        val eligible = database.forgottenFavorites().first().filterVideoSongs(hideVideoSongs)
        forgottenFavorites.value = if (eligible.size >= 8) eligible.shuffled().take(30) else null
    }

    fun regenerateForgottenFavorites() {
        viewModelScope.launch(Dispatchers.IO) {
            regenerateSection(
                key = "forgotten_favorites",
                currentIds = { forgottenFavorites.value?.map { it.id }?.toSet().orEmpty() },
                generate = { getForgottenFavorites() },
            )
            HomeCache.forgottenFavorites = forgottenFavorites.value
        }
    }

    fun regenerateQuickPicks() {
        viewModelScope.launch(Dispatchers.IO) {
            regenerateSection(
                key = "quick_picks",
                currentIds = { quickPicks.value?.map { it.id }?.toSet().orEmpty() },
                generate = { getQuickPicks() },
            )
            enrichQuickPicksFromNetwork()
            HomeCache.quickPicks = quickPicks.value
        }
    }

    fun regenerateDischiPerTe() {
        viewModelScope.launch(Dispatchers.IO) {
            regenerateSection(
                key = "dischi_per_te",
                currentIds = { dischiPerTe.value?.map { it.id }?.toSet().orEmpty() },
                generate = { getDischiPerTe() },
            )
        }
    }

    fun regenerateCommunityPlaylists() {
        viewModelScope.launch(Dispatchers.IO) {
            regenerateSection(
                key = "community_playlists",
                currentIds = { communityPlaylists.value?.map { it.playlist.id }?.toSet().orEmpty() },
                generate = { getCommunityPlaylists() },
            )
            HomeCache.communityPlaylists = communityPlaylists.value
        }
    }

    fun regenerateDailyDiscover() {
        viewModelScope.launch(Dispatchers.IO) {
            regenerateSection(
                key = "daily_discover",
                currentIds = { dailyDiscover.value?.map { it.recommendation.id }?.toSet().orEmpty() },
                generate = { getDailyDiscover() },
            )
            HomeCache.dailyDiscover = dailyDiscover.value
        }
    }

    fun regenerateSimilarRecommendation(seedId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            regenerateSection(
                key = "similar_recommendation_$seedId",
                currentIds = { similarRecommendations.value.orEmpty().firstOrNull { it.title.id == seedId }?.items?.map { it.id }?.toSet().orEmpty() },
                generate = { getSimilarRecommendations(onlySeedId = seedId) },
            )
        }
    }

    fun onSectionBecameVisible(sectionId: String) {
        val current = visibleSections.value
        if (sectionId in current) return
        visibleSections.value = current + sectionId

        when {
            sectionId == "daily_discover" && dailyDiscoverLaunchJob == null -> {
                dailyDiscoverLaunchJob = viewModelScope.launch(Dispatchers.IO) {
                    try {
                        phase1Complete.filter { it }.first()
                        withTimeoutOrNull(20_000L) { getDailyDiscover() }
                        HomeCache.dailyDiscover = dailyDiscover.value
                    } finally {
                        currentCoroutineContext().ensureActive()
                        phase2DailyDiscoverDone.value = true
                        checkPhase2Complete()
                    }
                }
            }
                sectionId == "from_the_community" && communityPlaylistsLaunchJob == null -> {
                communityPlaylistsLaunchJob = viewModelScope.launch(Dispatchers.IO) {
                    try {
                        phase1Complete.filter { it }.first()
                        withTimeoutOrNull(20_000L) { getCommunityPlaylists() }
                        HomeCache.communityPlaylists = communityPlaylists.value
                    } finally {
                        currentCoroutineContext().ensureActive()
                        phase2CommunityDone.value = true
                        checkPhase2Complete()
                    }
                }
            }
            sectionId.startsWith("similar_recommendation_") && similarRecommendationsLaunchJob == null -> {
                similarRecommendationsLaunchJob = viewModelScope.launch(Dispatchers.IO) {
                    try {
                        // Gate behind phase 1 like the other sections: the similar fan-out
                        // must not race (and starve) the home/account load on reconnect.
                        phase1Complete.filter { it }.first()
                        withTimeoutOrNull(30_000L) { getSimilarRecommendations() }
                    } finally {
                        currentCoroutineContext().ensureActive()
                        phase2SimilarDone.value = true
                        checkPhase2Complete()
                    }
                }
            }
            sectionId == "dischi_per_te" && dischiPerTeLaunchJob == null -> {
                dischiPerTeLaunchJob = viewModelScope.launch(Dispatchers.IO) {
                    try {
                        phase1Complete.filter { it }.first()
                        getDischiPerTe()
                    } finally {
                        currentCoroutineContext().ensureActive()
                        phase2DischiPerTeDone.value = true
                        checkPhase2Complete()
                    }
                }
            }
        }
    }

    fun loadHomeData() = viewModelScope.launch { load() }

    private suspend fun load() {
        refreshHomeFeed()
        refreshHeroCarousel(defaultHeroCarouselSeed(), force = true)
        isLoading.value = true
        phase1Complete.value = false
        phase2Complete.value = false
        phase2DailyDiscoverDone.value = false
        phase2CommunityDone.value = false
        phase2SimilarDone.value = false
        phase2DischiPerTeDone.value = true
        dailyDiscoverLaunchJob?.cancel(); dailyDiscoverLaunchJob = null
        communityPlaylistsLaunchJob?.cancel(); communityPlaylistsLaunchJob = null
        similarRecommendationsLaunchJob?.cancel(); similarRecommendationsLaunchJob = null
        dischiPerTeLaunchJob?.cancel(); dischiPerTeLaunchJob = null
        val hideExplicit = context.dataStore.get(HideExplicitKey, false)
        val hideVideoSongs = context.dataStore.get(HideVideoSongsKey, false)
        val fromTimeStamp = System.currentTimeMillis() - 86400000L * 7 * 2

        isForYouLoading.value = true
        viewModelScope.launch(Dispatchers.IO) {
            try {
                getForYouShelves()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                reportException(e)
            } finally {
                isForYouLoading.value = false
            }
        }

        try {
            coroutineScope {
                launch(Dispatchers.IO) {
                    runCatching { getQuickPicks() }.onFailure { reportException(it) }
                }
                launch(Dispatchers.IO) {
                    runCatching {
                        val songs = database.mostPlayedSongs(fromTimeStamp, limit = 15, offset = 5).first()
                            .filterVideoSongs(hideVideoSongs).shuffled().take(10)
                        val albums = database.mostPlayedAlbums(fromTimeStamp, limit = 8, offset = 2).first()
                            .filter { it.album.thumbnailUrl != null }.shuffled().take(5)
                        val artists = database.mostPlayedArtists(fromTimeStamp).first()
                            .filter { it.artist.isYouTubeArtist && it.artist.thumbnailUrl != null }.shuffled().take(5)
                        keepListening.value = (songs + albums + artists).shuffled()
                    }.onFailure { reportException(it) }
                }
            }
            allLocalItems.value = (quickPicks.value.orEmpty() + keepListening.value.orEmpty())
                .filter { it is Song || it is Album }
            HomeCache.quickPicks = quickPicks.value
            HomeCache.keepListening = keepListening.value
            isPhase1Complete.value = true
        } finally {
            isLoading.value = false
            phase1Complete.value = true
            // Reload cancels pending section jobs; restart those already requested by the screen.
            val sectionsToRestart = setOf(
                "daily_discover", "from_the_community", "similar_recommendation_0",
            ).intersect(visibleSections.value)
            visibleSections.value = visibleSections.value - sectionsToRestart
            sectionsToRestart.forEach(::onSectionBecameVisible)
        }

        viewModelScope.launch(Dispatchers.IO) {
            YouTube.explore().onSuccess { page ->
                explorePage.value = page.copy(newReleaseAlbums = page.newReleaseAlbums.filterExplicit(hideExplicit))
                HomeCache.explorePage = explorePage.value
                refreshHeroCarousel()
            }.onFailure { reportException(it) }
        }

        if (YouTube.cookie != null) {
            viewModelScope.launch(Dispatchers.IO) {
                loadAccountPlaylists()
            }
        } else {
            isAccountPlaylistsLoading.value = false
        }

        viewModelScope.launch(Dispatchers.IO) {
            enrichQuickPicksFromNetwork()
            HomeCache.quickPicks = quickPicks.value
        }
    }

    @Synchronized
    private fun refreshHomeFeed() {
        if (homeFeedJob?.isActive == true) return
        isHomeFeedLoading.value = true
        homeFeedJob = viewModelScope.launch(Dispatchers.IO) {
            try {
                if (!networkConnectivity.networkStatus.value) return@launch
                withTimeout(20_000L) { fetchHomeFeed() }
            } catch (_: TimeoutCancellationException) {
                // Keep the last successful feed when the connection stalls.
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                reportException(e)
            } finally {
                isHomeFeedLoading.value = false
            }
        }
    }

    private suspend fun fetchHomeFeed() {
        val hideExplicit = context.dataStore.get(HideExplicitKey, false)
        val hideVideoSongs = context.dataStore.get(HideVideoSongsKey, false)
        val hideYoutubeShorts = context.dataStore.get(HideYoutubeShortsKey, false)
        var homeResult = YouTube.home()

        if (homeResult.isFailure) {
            if (YouTube.cookie != null) {
                val recovered = syncUtils.reInjectCredentials()
                if (recovered) homeResult = YouTube.home()
            } else {
                YouTube.visitorData().getOrNull()?.let { fresh ->
                    YouTube.visitorData = fresh
                    context.dataStore.edit { it[VisitorDataKey] = fresh }
                }
                homeResult = YouTube.home()
            }
        }
        if (homeResult.isSuccess && YouTube.cookie == null &&
            homeResult.getOrNull()?.sections?.isEmpty() == true) {
            YouTube.visitorData().getOrNull()?.let { fresh ->
                YouTube.visitorData = fresh
                context.dataStore.edit { it[VisitorDataKey] = fresh }
            }
            homeResult = YouTube.home()
        }

        homeResult.onSuccess { page ->
            val transformedChips = transformChips(page.chips)
            val transformedPage = page.copy(
                chips = transformedChips,
                sections = page.sections.mapNotNull { section ->
                    val filtered = section.items
                        .filterExplicit(hideExplicit)
                        .filterVideoSongs(hideVideoSongs)
                        .filterYoutubeShorts(hideYoutubeShorts)
                    if (filtered.isEmpty()) null else section.copy(items = filtered)
                }
            )
            if (transformedPage.sections.isEmpty()) return@onSuccess
            homePage.value = transformedPage
            updateHomePersonalMixes(transformedPage)

            fillQuickPicksFromHome(transformedPage, hideExplicit)
            HomeCache.quickPicks = quickPicks.value

            val savedParamsForChip = cachedMoodSnapshot.value?.chipParams
            val allowedMoodChips = MoodMixes.chips(transformedChips)
            val preferredChip = if (!savedParamsForChip.isNullOrEmpty())
                transformedChips?.firstOrNull { it.endpoint?.params == savedParamsForChip }
            else null

            if (smartBootEnabled) {
                (allowedMoodChips.firstOrNull { it.endpoint?.params == savedParamsForChip }
                    ?: allowedMoodChips.firstOrNull() ?: preferredChip ?: transformedChips?.firstOrNull())
                    ?.takeIf { !it.endpoint?.params.isNullOrEmpty() }
                    ?.let { chip ->
                        applyChipWithSingleFetch(chip, hideExplicit, hideVideoSongs, hideYoutubeShorts)
                    }
            } else {
                if (selectedChip.value == null) {
                    (preferredChip ?: transformedChips?.firstOrNull())?.let { chip -> toggleChip(chip) }
                }
            }
            HomeCache.homePage = homePage.value
            HomeCache.lastLoadedAt = System.currentTimeMillis()
            context.dataStore.edit { it[HomeCacheLastLoadedKey] = HomeCache.lastLoadedAt }
            HomeFeedSnapshotStore.save(context, transformedPage)
            refreshHeroCarousel()
        }.onFailure { reportException(it) }
    }

    fun toggleChip(chip: HomePage.Chip?) {
        if (chip == null || chip == selectedChip.value && previousHomePage.value != null) {
            homePage.value = previousHomePage.value
            previousHomePage.value = null
            selectedChip.value = null
            return
        }

        if (selectedChip.value == null) {
            previousHomePage.value = homePage.value
        }

        viewModelScope.launch(Dispatchers.IO) {
            val hideExplicit = context.dataStore.get(HideExplicitKey, false)
            val hideVideoSongs = context.dataStore.get(HideVideoSongsKey, false)
            val hideYoutubeShorts = context.dataStore.get(HideYoutubeShortsKey, false)
            val nextSections = YouTube.home(params = chip.endpoint?.params).getOrNull() ?: return@launch

            homePage.value = nextSections.copy(
                chips = homePage.value?.chips,
                sections = nextSections.sections.mapNotNull { section ->
                    val filteredItems = section.items
                        .filterExplicit(hideExplicit)
                        .filterVideoSongs(hideVideoSongs)
                        .filterYoutubeShorts(hideYoutubeShorts)
                    if (filteredItems.isEmpty()) null else section.copy(items = filteredItems)
                }
            )
            selectedChip.value = chip

            if (chip.title.contains("Podcast", ignoreCase = true)) {
                fetchPodcastData()
            }
        }
    }

    /**
     * Applies the preferred chip to the main feed during Smart Boot.
     */
    private fun applyChipWithSingleFetch(
        chip: HomePage.Chip,
        hideExplicit: Boolean,
        hideVideoSongs: Boolean,
        hideYoutubeShorts: Boolean,
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val nextSections = try {
                withTimeout(15_000L) { YouTube.home(params = chip.endpoint?.params).getOrNull() }
            } catch (e: TimeoutCancellationException) {
                null
            } ?: return@launch

            val filteredSections = nextSections.sections.mapNotNull { section ->
                val filteredItems = section.items
                    .filterExplicit(hideExplicit)
                    .filterVideoSongs(hideVideoSongs)
                    .filterYoutubeShorts(hideYoutubeShorts)
                if (filteredItems.isEmpty()) null else section.copy(items = filteredItems)
            }
            if (filteredSections.isEmpty()) return@launch

            previousHomePage.value = homePage.value
            homePage.value = nextSections.copy(chips = homePage.value?.chips, sections = filteredSections)
            selectedChip.value = chip
            if (chip.title.contains("Podcast", ignoreCase = true)) {
                fetchPodcastData()
            }

        }
    }

    private suspend fun fetchPodcastData() {
        YouTube.savedPodcastShows().onSuccess { shows ->
            savedPodcastShows.value = shows
        }.onFailure {
            reportException(it)
        }

        YouTube.episodesForLater().onSuccess { episodes ->
            episodesForLater.value = episodes
        }.onFailure {
            reportException(it)
        }
    }

    private suspend fun loadAccountPlaylists() {
        isAccountPlaylistsLoading.value = true
        try {
            val hideYoutubeShorts = context.dataStore.get(HideYoutubeShortsKey, false)
            val result = withTimeoutOrNull(20_000L) {
                YouTube.library("FEmusic_liked_playlists").completed()
            } ?: return
            result.onSuccess {
                accountPlaylists.value = it.items.filterIsInstance<PlaylistItem>()
                    .filterNot { it.id == "LM" || it.id == "SE" }
                    .filterNot { playlist ->
                        playlist.songCountText?.let { text -> Regex("""\d+""").find(text)?.value?.toIntOrNull() } == 0
                    }
                    .filterYoutubeShorts(hideYoutubeShorts)
            }.onFailure {
                reportException(it)
            }
        } finally {
            isAccountPlaylistsLoading.value = false
        }
    }

    private fun updateHomePersonalMixes(page: HomePage?) {
        val initialPage = page ?: return
        val initialMixes = initialPage.freshFindOldFavoritePlaylists()
        homePersonalMixes.value = initialMixes
        homeMixContinuationJob?.cancel()

        if (initialMixes.size >= HomeMixTitleMatchers.size || initialPage.continuation.isNullOrEmpty()) {
            return
        }

        homeMixContinuationJob = viewModelScope.launch(Dispatchers.IO) {
            var currentPage = initialPage
            var sections = initialPage.sections
            val seenContinuations = mutableSetOf<String>()

            repeat(HOME_MIX_CONTINUATION_LIMIT) {
                val continuation = currentPage.continuation ?: return@launch
                if (!seenContinuations.add(continuation)) return@launch

                val result = YouTube.home(continuation = continuation)
                val nextPage = result.getOrNull() ?: run {
                    result.exceptionOrNull()?.let(::reportException)
                    return@launch
                }

                sections = sections + nextPage.sections
                val mixes = initialPage.copy(sections = sections).freshFindOldFavoritePlaylists()
                if (mixes.isNotEmpty()) homePersonalMixes.value = mixes
                if (mixes.size >= HomeMixTitleMatchers.size) return@launch
                currentPage = nextPage
            }
        }
    }

    fun refresh() {
        if (isRefreshing.value) return
        isRefreshing.value = true
        randomSeed.value = System.currentTimeMillis()
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val currentChip = selectedChip.value
                if (currentChip != null) {
                    refreshHeroCarousel(defaultHeroCarouselSeed(), force = true)
                    val hideExplicit = context.dataStore.get(HideExplicitKey, false)
                    val hideVideoSongs = context.dataStore.get(HideVideoSongsKey, false)
                    val hideYoutubeShorts = context.dataStore.get(HideYoutubeShortsKey, false)
                    val nextSections = YouTube.home(params = currentChip.endpoint?.params).getOrNull()
                    if (nextSections != null) {
                        homePage.value = nextSections.copy(
                            chips = homePage.value?.chips,
                            sections = nextSections.sections.mapNotNull { section ->
                                val filteredItems = section.items
                                    .filterExplicit(hideExplicit)
                                    .filterVideoSongs(hideVideoSongs)
                                    .filterYoutubeShorts(hideYoutubeShorts)
                                if (filteredItems.isEmpty()) null else section.copy(items = filteredItems)
                            }
                        )
                    }
                } else {
                    load()
                }
                refreshHeroCarousel()
            } finally {
                isRefreshing.value = false
            }
        }
        viewModelScope.launch(Dispatchers.IO) {
            syncUtils.tryAutoSync()
        }
    }

    fun refreshIfStale() {
        if (HomeCache.isStale() || HomeCache.homePage == null) {
            viewModelScope.launch(Dispatchers.IO) {
                load()
            }
        }
    }

    init {
        viewModelScope.launch(Dispatchers.IO) {
            val prefs = context.dataStore.data.first()
            prefs[SpeedDialSnapshotKey]?.let { json ->
                runCatching { snapshotJson.decodeFromString<SpeedDialSnapshot>(json) }
                    .getOrNull()?.let { cachedSpeedDialSnapshot.value = it }
            }
            prefs[MoodSnapshotKey]?.let { json ->
                runCatching { snapshotJson.decodeFromString<MoodSnapshot>(json) }
                    .getOrNull()?.let { snapshot ->
                        val restored = snapshot.items.mapNotNull { it.toPlaylistItem() }
                        val mood = MixMood.fromChip(snapshot.chipTitle)
                        val allowedCategory = MoodMixes.isAllowedCategory(snapshot.chipTitle)
                        val mixes = if (allowedCategory) MoodMixes.select(restored) else emptyList()
                        val clean = snapshot.copy(chipTitle = mood?.label ?: snapshot.chipTitle,
                            items = snapshot.items.filter { item -> mixes.any { it.id == item.id } }.distinctBy { it.id })
                        cachedMoodSnapshot.value = clean.takeIf { allowedCategory }
                        if (allowedCategory && moodPage.value == null && moodPageJob?.isActive != true) {
                            publishMoodMixes(mixes, mood, revalidate = false)
                            loadedMoodParams.value = snapshot.chipParams
                        }
                        if (clean != snapshot) context.dataStore.edit { it[MoodSnapshotKey] = snapshotJson.encodeToString(clean) }
                    }
            }
            preferredMoodParams.value = cachedMoodSnapshot.value?.chipParams
                ?: prefs[LastMoodChipParamsKey]
            isMoodPreferenceReady.value = true
            prefs[AccountNameKey]?.takeIf { it.isNotEmpty() }?.let { accountName.value = it }
            prefs[AccountPhotoUrlKey]?.takeIf { it.isNotEmpty() }?.let { accountImageUrl.value = it }
        }

        viewModelScope.launch(Dispatchers.IO) {
            speedDialItems
                .filter { it.isNotEmpty() }
                .collect { items ->
                    val snapshot18 = items.take(18)
                    val newIds = snapshot18.map { it.id }
                    if (newIds == lastSavedSpeedDialIds) return@collect
                    lastSavedSpeedDialIds = newIds
                    val snapshotItems = snapshot18.mapNotNull { mapYTItemToSnapshot(it) }
                    val snapshot = SpeedDialSnapshot(System.currentTimeMillis(), snapshotItems)
                    cachedSpeedDialSnapshot.value = snapshot
                    val json = runCatching { snapshotJson.encodeToString(snapshot) }.getOrNull() ?: return@collect
                    context.dataStore.edit { it[SpeedDialSnapshotKey] = json }
                }
        }

        viewModelScope.launch(Dispatchers.IO) {
            if (HomeCache.lastLoadedAt == 0L) {
                HomeCache.lastLoadedAt = context.dataStore.get(HomeCacheLastLoadedKey, 0L)
            }
            (HomeCache.homePage ?: HomeFeedSnapshotStore.load(context)?.page)?.let { restoredPage ->
                homePage.value = restoredPage
                updateHomePersonalMixes(restoredPage)
            }
            if (smartBootEnabled && isHeroCarouselEnabled.value) {
                context.dataStore.get(HeroCarouselSnapshotKey, "")?.takeIf { it.isNotEmpty() }?.let { json ->
                    runCatching { snapshotJson.decodeFromString<HeroCarouselSnapshot>(json) }.getOrNull()
                        ?.takeIf { it.items.isNotEmpty() }?.let { snapshot ->
                            heroCarouselItems.value = snapshot.items.filterIsInstance<HeroCarouselItem.ArtistRadio>()
                            previousHeroArtistIds = heroCarouselItems.value
                                .filterIsInstance<HeroCarouselItem.ArtistRadio>().map { it.artistId }.toSet()
                            previousFirstHeroArtistId = heroCarouselItems.value
                                .filterIsInstance<HeroCarouselItem.ArtistRadio>().firstOrNull()?.artistId
                        }
                }
            }
            quickPicks.value = HomeCache.quickPicks?.take(QUICK_PICKS_TARGET_COUNT)
            HomeCache.quickPicks = quickPicks.value
            keepListening.value = HomeCache.keepListening
            forgottenFavorites.value = HomeCache.forgottenFavorites
            explorePage.value = HomeCache.explorePage
            similarRecommendations.value = HomeCache.similarRecommendations.orEmpty()
                .distinctBy { it.title.id }
                .take(MAX_SIMILAR_RECOMMENDATIONS)
            HomeCache.similarRecommendations = similarRecommendations.value
            dailyDiscover.value = HomeCache.dailyDiscover
            communityPlaylists.value = HomeCache.communityPlaylists
            dischiPerTe.value = HomeCache.dischiPerTe
            forYouShelves.value = HomeCache.forYouShelves.orEmpty()
            if (!HomeCache.isStale() && HomeCache.homePage != null) {
                homePage.value = HomeCache.homePage
                if (homePersonalMixes.value.isEmpty()) {
                    HomeCache.homePage?.freshFindOldFavoritePlaylists()
                        ?.takeIf { it.isNotEmpty() }
                        ?.let { homePersonalMixes.value = it }
                }
                isForYouLoading.value = false
                isHomeFeedLoading.value = false
                if (YouTube.cookie != null) {
                    viewModelScope.launch(Dispatchers.IO) { loadAccountPlaylists() }
                } else {
                    isAccountPlaylistsLoading.value = false
                }
                refreshHeroCarousel(force = true)
                isPhase1Complete.value = true
                phase2DischiPerTeDone.value = true
                phase1Complete.value = true
                isLoading.value = false
                refreshHomeFeed()
                if (quickPicks.value.orEmpty().size < QUICK_PICKS_TARGET_COUNT) {
                    getQuickPicks()
                    enrichQuickPicksFromNetwork()
                    HomeCache.quickPicks = quickPicks.value
                }
            } else {
                load()
            }
        }

        viewModelScope.launch(Dispatchers.IO) {
            val cookie = context.dataStore.get(InnerTubeCookieKey, "")
            if ("SAPISID" in parseCookieString(cookie)) {
                val current = context.dataStore.get(SyncBannerLaunchCountKey, 0)
                if (current < 10) {
                    context.dataStore.edit { it[SyncBannerLaunchCountKey] = current + 1 }
                }
            }
        }

        viewModelScope.launch(Dispatchers.IO) {
            kotlinx.coroutines.delay(8000)
            syncUtils.tryAutoSync()
        }

        viewModelScope.launch(Dispatchers.IO) {
            kotlinx.coroutines.delay(12000)
            syncDiscoveryWeeklyIfNeeded()
        }

        viewModelScope.launch(Dispatchers.IO) {
            kotlinx.coroutines.delay(10000)
            showWrappedCard.collect { shouldShow ->
                if (shouldShow && !wrappedManager.state.value.isDataReady) {
                    try {
                        wrappedManager.prepare()
                        val state = wrappedManager.state.first { it.isDataReady }
                        val trackMap = state.trackMap
                        if (trackMap.isNotEmpty()) {
                            val firstTrackId = trackMap.entries.first().value
                            wrappedAudioService.prepareTrack(firstTrackId)
                        }
                    } catch (e: Exception) {
                        reportException(e)
                    }
                }
            }
        }

        viewModelScope.launch(Dispatchers.IO) {
            kotlinx.coroutines.delay(5000)
            context.dataStore.data
                .map { it[InnerTubeCookieKey] }
                .distinctUntilChanged()
                .collect { cookie ->
                    if (isProcessingAccountData) return@collect
                    val previousCookie = lastProcessedCookie
                    lastProcessedCookie = cookie
                    isProcessingAccountData = true
                    try {
                        if (!cookie.isNullOrEmpty()) {
                            YouTube.cookie = cookie
                            YouTube.accountInfo().onSuccess { info ->
                                val photoUrl = info.thumbnailUrl
                                    ?.replace(Regex("w\\d+-h\\d+(-[a-zA-Z0-9]+)?"), "w256-h256-c")
                                    ?: info.thumbnailUrl
                                accountName.value = info.name
                                accountImageUrl.value = photoUrl
                                context.dataStore.edit { prefs ->
                                    prefs[AccountNameKey] = info.name
                                    if (photoUrl != null) prefs[AccountPhotoUrlKey] = photoUrl
                                }
                            }.onFailure { reportException(it) }
                            if (previousCookie != null && previousCookie != cookie) {
                                syncUtils.performFullSync()
                            }
                        } else {
                            accountName.value = "Guest"
                            accountImageUrl.value = null
                            accountPlaylists.value = null
                        }
                    } finally {
                        isProcessingAccountData = false
                    }
                }
        }


        viewModelScope.launch(Dispatchers.IO) {
            context.dataStore.data
                .map { it[HideYoutubeShortsKey] ?: false }
                .distinctUntilChanged()
                .collect {
                    if (YouTube.cookie != null && accountPlaylists.value != null) {
                        loadAccountPlaylists()
                    }
                }
        }

        viewModelScope.launch(Dispatchers.IO) {
            networkConnectivity.networkChanged.debounce(1_000L).collect {
                if (networkConnectivity.networkStatus.value) {
                    refreshHomeFeed()
                    if (syncUtils.reInjectCredentials()) loadAccountPlaylists()
                }
            }
        }
    }

    override fun onCleared() {
        if (moodPostersLazy.isInitialized()) moodPosters.imageLoader.shutdown()
        super.onCleared()
    }

}
