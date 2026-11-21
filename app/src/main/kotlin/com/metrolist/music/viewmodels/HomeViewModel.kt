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
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
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
import com.metrolist.music.constants.HeroCarouselPendingSnapshotKey
import com.metrolist.music.constants.HomeRecommendationSnapshotKey
import com.metrolist.music.constants.HomeRecommendationPendingSnapshotKey
import com.metrolist.music.constants.LastDiscoveryWeeklySyncKey
import com.metrolist.music.constants.HomeCollapsedSectionsKey
import com.metrolist.music.constants.ShowWrappedCardKey
import com.metrolist.music.discovery.AlbumRecommendationsGenerator
import com.metrolist.music.discovery.DiscoveryWeeklyGenerator
import com.metrolist.music.discovery.MixMood
import com.metrolist.music.discovery.MoodMixes
import com.metrolist.music.discovery.MoodLoadRetry
import com.metrolist.music.discovery.MoodPageCache
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.update
import com.metrolist.music.discovery.HeroCarouselGenerator
import com.metrolist.music.discovery.HERO_CAROUSEL_TARGET_SIZE
import com.metrolist.music.models.DischiPerTeItem
import com.metrolist.music.models.ForYouShelfItem
import com.metrolist.music.models.ForYouShelfTile
import com.metrolist.music.models.HeroCarouselItem
import com.metrolist.music.constants.SpeedDialSnapshotKey
import com.metrolist.music.constants.WrappedSeenKey
import com.metrolist.music.models.HomeSnapshotItem
import com.metrolist.music.models.HomeDailyDiscoverSnapshotItem
import com.metrolist.music.models.HomeForYouShelfSnapshot
import com.metrolist.music.models.HomeForYouTileSnapshot
import com.metrolist.music.models.HomeSimilarShelfSnapshot
import com.metrolist.music.models.HomeRecommendationSnapshot
import com.metrolist.music.models.MoodSnapshot
import com.metrolist.music.models.toMediaMetadata
import com.metrolist.music.models.toPlaylistItem
import com.metrolist.music.models.toYTItem
import com.metrolist.music.models.SpeedDialSnapshot
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import com.metrolist.music.ui.screens.HomeSection
import com.metrolist.music.db.MusicDatabase
import com.metrolist.music.db.entities.Album
import com.metrolist.music.db.entities.AlbumEntity
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
private const val QUICK_PICKS_ALGORITHM_VERSION = 1
private const val FOR_YOU_ALGORITHM_VERSION = 3
private val FOR_YOU_THUMBNAIL_SIZE_SUFFIX =
    Regex("=(?:w\\d+|h\\d+|s\\d+)(?:-[a-z0-9]+)*$", RegexOption.IGNORE_CASE)

private fun normalizeForYouAlbumTitle(title: String): String =
    title.trim().replace(Regex("\\s+"), " ").lowercase()

private fun normalizeForYouArtwork(url: String?): String? =
    url?.trim()?.replace(FOR_YOU_THUMBNAIL_SIZE_SUFFIX, "")?.takeIf { it.isNotEmpty() }
private const val INITIAL_FOR_YOU_SHELF_COUNT = 8
private const val INITIAL_FOR_YOU_ARTIST_SCAN_LIMIT = 24
private const val HOME_MIX_CONTINUATION_LIMIT = 3
internal const val HOME_MOOD_SECTION_ENABLED = false
internal const val HOME_FAVORITES_DISCOVERIES_SECTION_ENABLED = false
private const val MAX_SIMILAR_RECOMMENDATIONS = 14
private const val HOME_SIMILAR_CONTINUATION_LIMIT = 6
private const val HOME_RECOMMENDATION_REFRESH_INTERVAL_MS = 24L * 60 * 60 * 1000

private fun <T> List<T>.randomSample(limit: Int): List<T> {
    if (limit <= 0 || isEmpty()) return emptyList()
    val sampleSize = minOf(limit, size)
    val selectedIndices = HashSet<Int>(sampleSize)
    val sample = ArrayList<T>(sampleSize)
    while (sample.size < sampleSize) {
        val index = Random.nextInt(size)
        if (selectedIndices.add(index)) sample += this[index]
    }
    return sample
}

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

private const val HERO_CAROUSEL_REFRESH_INTERVAL_MS = 24L * 60 * 60 * 1000

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
    private val isFirstHomeViewModelInProcess = HomeCache.markViewModelCreated()
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

    private val heroCarouselGenerator = HeroCarouselGenerator(database, context)

    val dischiPerTe = MutableStateFlow<List<DischiPerTeItem>?>(null)
    val forYouShelves = MutableStateFlow<List<ForYouShelfItem>>(emptyList())
    private val forYouPreparationMutex = kotlinx.coroutines.sync.Mutex()
    private var pendingForYouShelves: List<ForYouShelfItem> = emptyList()
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
    private var lastHeroCarouselRefreshAt = 0L

    fun refreshHeroCarousel(seed: Long = lastHeroCarouselSeed, force: Boolean = false) {
        if (heroCarouselJob?.isActive == true) {
            if (!force) return
            heroCarouselJob?.cancel()
        }
        val now = System.currentTimeMillis()
        if (!force && lastHeroCarouselRefreshAt > 0L &&
            (now < lastHeroCarouselRefreshAt || now - lastHeroCarouselRefreshAt < HERO_CAROUSEL_REFRESH_INTERVAL_MS)) {
            isHeroCarouselLoading.value = heroCarouselItems.value.isEmpty()
            return
        }
        previousHeroArtistIds = heroCarouselItems.value
            .filterIsInstance<HeroCarouselItem.MusicVideo>().map { it.artistId }.toSet()
        previousFirstHeroArtistId = heroCarouselItems.value
            .filterIsInstance<HeroCarouselItem.MusicVideo>().firstOrNull()?.artistId
        lastHeroCarouselSeed = seed
        lastHeroCarouselRefreshAt = now
        isHeroCarouselLoading.value = true
        heroCarouselJob = viewModelScope.launch(Dispatchers.IO, start = kotlinx.coroutines.CoroutineStart.LAZY) {
            val myJob = coroutineContext[Job]
            try {
                val videos = heroCarouselGenerator.generate(
                    homePage = homePage.value,
                    seed = seed,
                    previousArtistIds = previousHeroArtistIds,
                    previousFirstArtistId = previousFirstHeroArtistId,
                    hideExplicit = context.dataStore.get(HideExplicitKey, false),
                    hideVideos = context.dataStore.get(HideVideoSongsKey, false),
                )
                coroutineContext.ensureActive()
                val generatedAt = System.currentTimeMillis()
                lastHeroCarouselRefreshAt = generatedAt
                if (isCompleteHeroCarousel(videos)) {
                    persistPendingHeroCarouselSnapshot(seed, generatedAt, videos)
                    if (heroCarouselItems.value.isEmpty()) heroCarouselItems.value = videos
                } else if (heroCarouselItems.value.isEmpty()) {
                    // First install has no complete snapshot to keep; show validated results once
                    // generation ends, but never save a partial replacement over a full snapshot.
                    heroCarouselItems.value = videos.take(HERO_CAROUSEL_TARGET_SIZE)
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

    private fun isCompleteHeroCarousel(items: List<HeroCarouselItem>): Boolean {
        val videos = items.filterIsInstance<HeroCarouselItem.MusicVideo>()
        return videos.size == HERO_CAROUSEL_TARGET_SIZE &&
            videos.map { it.artistId }.distinct().size == HERO_CAROUSEL_TARGET_SIZE &&
            videos.map { it.song.id }.distinct().size == HERO_CAROUSEL_TARGET_SIZE
    }

    fun removeHeroVideo(videoId: String) {
        heroCarouselItems.update { current ->
            current.filterNot { it is HeroCarouselItem.MusicVideo && it.song.id == videoId }
        }
        viewModelScope.launch(Dispatchers.IO) {
            persistCurrentHeroCarouselSnapshot(lastHeroCarouselSeed, lastHeroCarouselRefreshAt, heroCarouselItems.value)
        }
        if (heroCarouselJob?.isActive != true) refreshHeroCarousel(force = true)
    }

    @kotlinx.serialization.Serializable
    private data class HeroCarouselSnapshot(
        val seed: Long,
        val items: List<HeroCarouselItem>,
        val artworkVersion: Int = 0,
        val generatedAt: Long = 0L,
    )

    private fun decodeHeroCarouselSnapshot(json: String?): HeroCarouselSnapshot? =
        json?.takeIf { it.isNotBlank() }?.let {
            runCatching { snapshotJson.decodeFromString<HeroCarouselSnapshot>(it) }.getOrNull()
        }?.takeIf { it.artworkVersion == 1 && it.items.isNotEmpty() }

    private fun visibleHeroVideos(
        items: List<HeroCarouselItem>,
        hideVideos: Boolean,
        hideExplicit: Boolean,
    ): List<HeroCarouselItem.MusicVideo> {
        if (hideVideos) return emptyList()
        val artistIds = mutableSetOf<String>()
        val songIds = mutableSetOf<String>()
        return items.filterIsInstance<HeroCarouselItem.MusicVideo>()
            .filter { video ->
                (!hideExplicit || !video.song.explicit) &&
                    artistIds.add(video.artistId) && songIds.add(video.song.id)
            }
            .take(HERO_CAROUSEL_TARGET_SIZE)
    }

    private suspend fun restoreHeroCarouselSnapshot(prefs: androidx.datastore.preferences.core.Preferences) {
        val hideVideos = prefs[HideVideoSongsKey] ?: false
        val hideExplicit = prefs[HideExplicitKey] ?: false
        val pendingJson = prefs[HeroCarouselPendingSnapshotKey]
        val pending = decodeHeroCarouselSnapshot(pendingJson)
        val current = decodeHeroCarouselSnapshot(prefs[HeroCarouselSnapshotKey])
        val pendingVideos = pending?.let { visibleHeroVideos(it.items, hideVideos, hideExplicit) }.orEmpty()
        val currentVideos = current?.let { visibleHeroVideos(it.items, hideVideos, hideExplicit) }.orEmpty()
        val usePending = pending != null && isCompleteHeroCarousel(pendingVideos)
        val selectedSnapshot = if (usePending) pending else current
        val selectedVideos = if (usePending) pendingVideos else currentVideos

        heroCarouselItems.value = selectedVideos
        previousHeroArtistIds = selectedVideos.map { it.artistId }.toSet()
        previousFirstHeroArtistId = selectedVideos.firstOrNull()?.artistId
        lastHeroCarouselRefreshAt = selectedSnapshot?.generatedAt
            ?.takeIf { isCompleteHeroCarousel(selectedVideos) } ?: 0L
        isHeroCarouselLoading.value = selectedVideos.isEmpty()

        if (usePending && pendingJson != null) {
            context.dataStore.edit {
                it[HeroCarouselSnapshotKey] = pendingJson
                it.remove(HeroCarouselPendingSnapshotKey)
            }
        }
    }

    private suspend fun persistPendingHeroCarouselSnapshot(
        seed: Long,
        generatedAt: Long,
        items: List<HeroCarouselItem>,
    ) {
        if (!isCompleteHeroCarousel(items)) return
        val json = runCatching {
            snapshotJson.encodeToString(HeroCarouselSnapshot(seed, items, artworkVersion = 1, generatedAt = generatedAt))
        }.getOrNull() ?: return
        context.dataStore.edit { it[HeroCarouselPendingSnapshotKey] = json }
    }

    private suspend fun persistCurrentHeroCarouselSnapshot(
        seed: Long,
        generatedAt: Long,
        items: List<HeroCarouselItem>,
    ) {
        if (items.isEmpty()) {
            context.dataStore.edit { it.remove(HeroCarouselSnapshotKey) }
            return
        }
        val json = runCatching {
            snapshotJson.encodeToString(HeroCarouselSnapshot(seed, items, artworkVersion = 1, generatedAt = generatedAt))
        }.getOrNull() ?: return
        context.dataStore.edit { it[HeroCarouselSnapshotKey] = json }
    }

    suspend fun fetchArtistRadioEndpoint(artistId: String): WatchEndpoint? =
        YouTube.artist(artistId).getOrNull()?.artist?.radioEndpoint

    val cachedSpeedDialSnapshot = MutableStateFlow<SpeedDialSnapshot?>(null)
    val cachedMoodSnapshot = MutableStateFlow<MoodSnapshot?>(null)

    private val snapshotJson = Json { ignoreUnknownKeys = true }
    private var lastSavedSpeedDialIds: List<String> = emptyList()
    @Volatile private var currentRecommendationSnapshot = HomeRecommendationSnapshot()
    @Volatile private var pendingRecommendationSnapshot: HomeRecommendationSnapshot? = null
    private var lastSavedQuickPicksSnapshotIds: List<String> = emptyList()

    private fun isRecommendationSnapshotFresh(updatedAt: Long, now: Long = System.currentTimeMillis()): Boolean =
        updatedAt > 0L && now - updatedAt in 0..HOME_RECOMMENDATION_REFRESH_INTERVAL_MS

    private fun isSectionSnapshotFresh(currentUpdatedAt: Long, pendingUpdatedAt: Long): Boolean =
        isRecommendationSnapshotFresh(maxOf(currentUpdatedAt, pendingUpdatedAt))

    private fun isQuickPicksSnapshotFresh(): Boolean {
        val current = currentRecommendationSnapshot
        val pending = pendingRecommendationSnapshot
        return (current.quickPicksAlgorithmVersion == QUICK_PICKS_ALGORITHM_VERSION &&
            isRecommendationSnapshotFresh(current.quickPicksUpdatedAt)) ||
            (pending?.quickPicksAlgorithmVersion == QUICK_PICKS_ALGORITHM_VERSION &&
                isRecommendationSnapshotFresh(pending.quickPicksUpdatedAt))
    }

    private fun mapYTItemToSnapshot(item: YTItem): HomeSnapshotItem? = when (item) {
        is SongItem -> HomeSnapshotItem(
            id = item.id,
            title = item.title,
            subtitle = item.artists.joinToString(", ") { it.name }.takeIf { it.isNotEmpty() },
            thumbnailUrl = item.thumbnail.takeIf { it.isNotEmpty() },
            type = "song",
            explicit = item.explicit,
        )
        is AlbumItem -> HomeSnapshotItem(
            id = item.browseId,
            title = item.title,
            subtitle = item.artists?.joinToString(", ") { it.name }?.takeIf { it.isNotEmpty() },
            thumbnailUrl = item.thumbnail.takeIf { it.isNotEmpty() },
            type = "album",
            browseId = item.browseId,
            playlistId = item.playlistId,
            explicit = item.explicit,
            year = item.year,
            albumType = item.albumType,
            releaseDate = item.releaseDate,
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

    private fun Song.toHomeSnapshotItem() = HomeSnapshotItem(
        id = id,
        title = title,
        subtitle = artists.joinToString(", ") { it.name }.takeIf { it.isNotEmpty() },
        thumbnailUrl = thumbnailUrl,
        type = "song",
        explicit = song.explicit,
    )

    private fun ForYouShelfItem.toHomeForYouShelfSnapshot(): HomeForYouShelfSnapshot? {
        val snapshotTiles = tiles.mapNotNull { tile ->
            when (tile) {
                is ForYouShelfTile.Local -> {
                    val album = tile.item as? Album ?: return@mapNotNull null
                    HomeForYouTileSnapshot(
                        type = "local",
                        item = HomeSnapshotItem(
                            id = album.id,
                            title = album.title,
                            subtitle = album.artists.joinToString(", ") { it.name },
                            thumbnailUrl = album.thumbnailUrl,
                            type = "album",
                            browseId = album.album.id,
                            playlistId = album.album.playlistId,
                            explicit = album.album.explicit,
                            year = album.album.year,
                            albumType = album.album.albumType,
                            releaseDate = album.album.releaseDate,
                        ),
                    )
                }
                is ForYouShelfTile.RecommendedAlbum -> mapYTItemToSnapshot(tile.item)?.let {
                    HomeForYouTileSnapshot(type = "recommended", item = it)
                }
            }
        }
        if (snapshotTiles.size < 3) return null
        return HomeForYouShelfSnapshot(
            artistId = artist.id,
            artistName = artist.artist.name,
            artistThumbnailUrl = artist.thumbnailUrl,
            tiles = snapshotTiles,
        )
    }

    private fun HomeSnapshotItem.toSongFallback(): Song? {
        val item = toYTItem() as? SongItem ?: return null
        return Song(
            song = item.toMediaMetadata().toSongEntity(),
            artists = item.artists.map { artist ->
                ArtistEntity(id = artist.id ?: ArtistEntity.generateArtistId(), name = artist.name)
            },
        )
    }

    private fun SimilarRecommendation.toHomeSimilarShelfSnapshot(): HomeSimilarShelfSnapshot? {
        val (type, browseId) = when (val title = title) {
            is Artist -> "artist" to null
            is Album -> "album" to null
            is Playlist -> "playlist" to title.playlist.browseId
            else -> return null
        }
        val items = items.mapNotNull(::mapYTItemToSnapshot)
        if (items.size < 2) return null
        return HomeSimilarShelfSnapshot(
            titleType = type,
            titleId = title.id,
            title = title.title,
            thumbnailUrl = thumbnailUrl ?: title.thumbnailUrl,
            playlistBrowseId = browseId,
            items = items,
        )
    }

    private suspend fun persistPendingRecommendationSnapshot(
        update: (HomeRecommendationSnapshot) -> HomeRecommendationSnapshot,
    ) {
        context.dataStore.edit { prefs ->
            val base = prefs[HomeRecommendationPendingSnapshotKey]
                ?.let { runCatching { snapshotJson.decodeFromString<HomeRecommendationSnapshot>(it) }.getOrNull() }
                ?: prefs[HomeRecommendationSnapshotKey]
                    ?.let { runCatching { snapshotJson.decodeFromString<HomeRecommendationSnapshot>(it) }.getOrNull() }
                ?: HomeRecommendationSnapshot()
            val pending = update(base)
            runCatching { snapshotJson.encodeToString(pending) }.getOrNull()?.let {
                prefs[HomeRecommendationPendingSnapshotKey] = it
                pendingRecommendationSnapshot = pending
            }
        }
    }

    private suspend fun persistForYouSnapshot(shelves: List<ForYouShelfItem>) {
        val snapshots = shelves.take(24).mapNotNull { it.toHomeForYouShelfSnapshot() }
        if (snapshots.isEmpty()) return
        val updatedAt = System.currentTimeMillis()
        persistPendingRecommendationSnapshot { snapshot ->
            snapshot.copy(
                forYouUpdatedAt = updatedAt,
                forYouAlgorithmVersion = FOR_YOU_ALGORITHM_VERSION,
                forYouShelves = snapshots,
            )
        }
    }

    private fun trackQuickPicksSnapshot() {
        viewModelScope.launch(Dispatchers.IO) {
            val hideExplicit = context.dataStore.get(HideExplicitKey, false)
            quickPicks.collect { songs ->
                val eligible = eligibleQuickPicks(songs.orEmpty(), hideExplicit)
                    .take(QUICK_PICKS_TARGET_COUNT)
                if (eligible.size < QUICK_PICKS_TARGET_COUNT) return@collect
                val ids = eligible.map { it.id }
                if (ids == lastSavedQuickPicksSnapshotIds) return@collect
                val items = eligible.map { it.toHomeSnapshotItem() }
                if (items.size != QUICK_PICKS_TARGET_COUNT) return@collect
                val updatedAt = System.currentTimeMillis()
                persistPendingRecommendationSnapshot { snapshot ->
                    snapshot.copy(
                        quickPicksUpdatedAt = updatedAt,
                        quickPicks = items,
                        quickPicksAlgorithmVersion = QUICK_PICKS_ALGORITHM_VERSION,
                    )
                }
                lastSavedQuickPicksSnapshotIds = ids
            }
        }
    }

    private suspend fun restoreHomeRecommendationSnapshot(prefs: androidx.datastore.preferences.core.Preferences) {
        val current = prefs[HomeRecommendationSnapshotKey]
            ?.let { runCatching { snapshotJson.decodeFromString<HomeRecommendationSnapshot>(it) }.getOrNull() }
            ?: HomeRecommendationSnapshot()
        val pending = prefs[HomeRecommendationPendingSnapshotKey]
            ?.let { runCatching { snapshotJson.decodeFromString<HomeRecommendationSnapshot>(it) }.getOrNull() }
        val promotePending = isFirstHomeViewModelInProcess
        val now = System.currentTimeMillis()

        fun <T> choose(
            currentAt: Long,
            currentItems: List<T>,
            pendingAt: Long,
            pendingItems: List<T>,
        ): Pair<Long, List<T>> = when {
            promotePending && pendingItems.isNotEmpty() &&
                (isRecommendationSnapshotFresh(pendingAt, now) || currentItems.isEmpty()) -> pendingAt to pendingItems
            currentItems.isNotEmpty() -> currentAt to currentItems
            promotePending && pendingItems.isNotEmpty() -> pendingAt to pendingItems
            else -> 0L to emptyList()
        }

        val currentQuickPicksMatch = current.quickPicksAlgorithmVersion == QUICK_PICKS_ALGORITHM_VERSION
        val pendingQuickPicks = pending?.takeIf {
            it.quickPicksAlgorithmVersion == QUICK_PICKS_ALGORITHM_VERSION
        }
        val (quickPicksAt, quickPicksItems) = choose(
            if (currentQuickPicksMatch) current.quickPicksUpdatedAt else 0L,
            if (currentQuickPicksMatch) current.quickPicks else emptyList(),
            pendingQuickPicks?.quickPicksUpdatedAt ?: 0L,
            pendingQuickPicks?.quickPicks.orEmpty(),
        )
        val (dailyAt, dailyItems) = choose(
            current.dailyDiscoverUpdatedAt, current.dailyDiscover,
            pending?.dailyDiscoverUpdatedAt ?: 0L, pending?.dailyDiscover.orEmpty(),
        )
        val (communityAt, communityItems) = choose(
            current.communityUpdatedAt, current.communityPlaylists,
            pending?.communityUpdatedAt ?: 0L, pending?.communityPlaylists.orEmpty(),
        )
        val (similarAt, similarItems) = choose(
            current.similarUpdatedAt, current.similarShelves,
            pending?.similarUpdatedAt ?: 0L, pending?.similarShelves.orEmpty(),
        )
        val currentForYouMatches = current.forYouAlgorithmVersion == FOR_YOU_ALGORITHM_VERSION
        val pendingForYou = pending?.takeIf { it.forYouAlgorithmVersion == FOR_YOU_ALGORITHM_VERSION }
        val pendingForYouWithShelves = pendingForYou?.takeIf { it.forYouShelves.isNotEmpty() }
        val (forYouAt, forYouItems) = when {
            promotePending && pendingForYouWithShelves != null ->
                pendingForYouWithShelves.forYouUpdatedAt to pendingForYouWithShelves.forYouShelves
            currentForYouMatches && current.forYouShelves.isNotEmpty() ->
                current.forYouUpdatedAt to current.forYouShelves
            pendingForYouWithShelves != null ->
                pendingForYouWithShelves.forYouUpdatedAt to pendingForYouWithShelves.forYouShelves
            else -> 0L to emptyList()
        }
        val selected = HomeRecommendationSnapshot(
            quickPicksUpdatedAt = quickPicksAt,
            quickPicks = quickPicksItems,
            quickPicksAlgorithmVersion = if (quickPicksItems.isNotEmpty()) QUICK_PICKS_ALGORITHM_VERSION else 0,
            dailyDiscoverUpdatedAt = dailyAt,
            dailyDiscover = dailyItems,
            communityUpdatedAt = communityAt,
            communityPlaylists = communityItems,
            similarUpdatedAt = similarAt,
            similarShelves = similarItems,
            forYouUpdatedAt = forYouAt,
            forYouAlgorithmVersion = if (forYouItems.isNotEmpty()) FOR_YOU_ALGORITHM_VERSION else 0,
            forYouShelves = forYouItems,
        )
        currentRecommendationSnapshot = if (promotePending) selected else current
        pendingRecommendationSnapshot = if (promotePending) null else pending

        val songSnapshotItems = quickPicksItems + dailyItems.map { it.seed }
        val restoredSongs = if (songSnapshotItems.isEmpty()) emptyMap() else {
            runCatching { database.getSongsByIds(songSnapshotItems.map { it.id }.distinct()) }
                .getOrDefault(emptyList())
                .associateBy { it.id }
        }
        fun restoreSong(item: HomeSnapshotItem): Song? = restoredSongs[item.id] ?: item.toSongFallback()
        val hideExplicit = prefs[HideExplicitKey] ?: false

        if (quickPicksItems.isNotEmpty()) {
            val restored = quickPicksItems.mapNotNull(::restoreSong)
                .filter { !it.song.isVideo && !it.song.isEpisode && (!hideExplicit || !it.song.explicit) }
                .distinctBy { it.id }
                .take(QUICK_PICKS_TARGET_COUNT)
            quickPicks.value = restored
            HomeCache.quickPicks = restored
            lastSavedQuickPicksSnapshotIds = restored.map { it.id }
        }
        if (dailyItems.isNotEmpty()) {
            val restored = dailyItems.mapNotNull { item ->
                val seed = restoreSong(item.seed) ?: return@mapNotNull null
                val recommendation = item.recommendation.toYTItem() ?: return@mapNotNull null
                if (recommendation is SongItem && (recommendation.isVideoSong || recommendation.explicit)) {
                    return@mapNotNull null
                }
                DailyDiscoverItem(seed, recommendation, relatedEndpoint = null)
            }.distinctBy { it.recommendation.id }
            dailyDiscover.value = restored
            HomeCache.dailyDiscover = restored
        }
        if (communityItems.isNotEmpty()) {
            val restored = communityItems.mapNotNull { item ->
                item.toPlaylistItem()?.let { CommunityPlaylistItem(it, emptyList()) }
            }.distinctBy { it.playlist.id }
            communityPlaylists.value = restored
            HomeCache.communityPlaylists = restored
        }
        if (similarItems.isNotEmpty()) {
            val restored = similarItems.mapNotNull { shelf ->
                val title: LocalItem = when (shelf.titleType) {
                    "artist" -> Artist(
                        ArtistEntity(id = shelf.titleId, name = shelf.title, thumbnailUrl = shelf.thumbnailUrl),
                        songCount = 0,
                    )
                    "album" -> Album(
                        AlbumEntity(
                            id = shelf.titleId,
                            title = shelf.title,
                            thumbnailUrl = shelf.thumbnailUrl,
                            songCount = 0,
                            duration = 0,
                        ),
                    )
                    "playlist" -> Playlist(
                        PlaylistEntity(
                            id = shelf.titleId,
                            name = shelf.title,
                            browseId = shelf.playlistBrowseId ?: shelf.titleId,
                            thumbnailUrl = shelf.thumbnailUrl,
                            isEditable = false,
                        ),
                        songCount = 0,
                        songThumbnails = emptyList(),
                    )
                    else -> return@mapNotNull null
                }
                val items = shelf.items.mapNotNull { item -> item.toYTItem() }
                    .filterNot { it is SongItem && hideExplicit && it.explicit }
                if (items.size < 2) null else SimilarRecommendation(title, items, shelf.thumbnailUrl)
            }.distinctBy { it.title.id }.take(MAX_SIMILAR_RECOMMENDATIONS)
            similarRecommendations.value = restored
            HomeCache.similarRecommendations = restored
        }
        if (forYouItems.isNotEmpty() && (promotePending || HomeCache.forYouShelves.isNullOrEmpty())) {
            val initialForYouItems = forYouItems.take(INITIAL_FOR_YOU_SHELF_COUNT)
            val localAlbumIds = initialForYouItems.flatMap { shelf ->
                shelf.tiles.filter { it.type == "local" }.map { it.item.id }
            }.distinct()
            val localAlbums = if (localAlbumIds.isEmpty()) emptyMap<String, Album>() else {
                runCatching { database.albumsByIds(localAlbumIds).first() }
                    .getOrDefault(emptyList()).associateBy { it.id }
            }
            val restored = initialForYouItems.mapNotNull { shelf ->
                val artist = Artist(
                    ArtistEntity(
                        id = shelf.artistId,
                        name = shelf.artistName,
                        thumbnailUrl = shelf.artistThumbnailUrl,
                    ),
                    songCount = 0,
                )
                val tiles = shelf.tiles.mapNotNull { tile ->
                    when (tile.type) {
                        "local" -> localAlbums[tile.item.id]
                            ?.takeIf { !it.thumbnailUrl.isNullOrBlank() && (!hideExplicit || !it.album.explicit) }
                            ?.let { ForYouShelfTile.Local(it) }
                        "recommended" -> (tile.item.toYTItem() as? AlbumItem)
                            ?.takeIf { !it.thumbnail.isNullOrBlank() && (!hideExplicit || !it.explicit) }
                            ?.let { ForYouShelfTile.RecommendedAlbum(it) }
                        else -> null
                    }
                }
                ForYouShelfItem(artist, tiles).withArtworkOnly()
            }
            if (restored.isNotEmpty()) {
                forYouShelves.value = restored
                HomeCache.forYouShelves = restored
            }
        }

        if (promotePending) {
            context.dataStore.edit { stored ->
                val encoded = runCatching { snapshotJson.encodeToString(selected) }.getOrNull()
                if (encoded != null && selected != HomeRecommendationSnapshot()) {
                    stored[HomeRecommendationSnapshotKey] = encoded
                } else {
                    stored.remove(HomeRecommendationSnapshotKey)
                }
                stored.remove(HomeRecommendationPendingSnapshotKey)
            }
        }
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

    data class HomeMoodContent(
        val params: String?,
        val items: List<PlaylistItem>?,
    )
    val homeMoodContent = combine(loadedMoodParams, moodMixItems) { params, items ->
        HomeMoodContent(params, items)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, HomeMoodContent(null, null))
    private var requestedMoodParams: String? = null
    private var requestedMoodFilters: Triple<Boolean, Boolean, Boolean>? = null
    private val moodPageCache = MoodPageCache(context)

    fun loadMoodPage(
        params: String?,
        chipTitle: String? = null,
        hideExplicit: Boolean,
        hideVideoSongs: Boolean,
        hideYoutubeShorts: Boolean,
        forceReload: Boolean = false,
    ) {
        if (!HOME_MOOD_SECTION_ENABLED) return
        val filters = Triple(hideExplicit, hideVideoSongs, hideYoutubeShorts)
        if (!forceReload && isMoodLoading.value && params == requestedMoodParams &&
            filters == requestedMoodFilters) return
        if (!forceReload && !isMoodError.value && params == loadedMoodParams.value &&
            loadedMoodFilters == filters && moodPage.value != null && !isMoodLoading.value) {
            return
        }
        requestedMoodParams = params
        requestedMoodFilters = filters
        isMoodError.value = false
        if (params == null || !MoodMixes.isAllowedCategory(chipTitle)) {
            moodPageJob?.cancel()
            moodPage.value = null
            moodMixItems.value = null
            isMoodLoading.value = false
            loadedMoodParams.value = null
            loadedMoodFilters = null
            return
        }
        moodPageJob?.cancel()
        if (params != loadedMoodParams.value ||
            (loadedMoodFilters != null && filters != loadedMoodFilters)) {
            moodPage.value = null
            moodMixItems.value = null
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
                    moodMixItems.value = MoodMixes.select(cached.sections.flatMap { it.items })
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
                    moodMixItems.value = mixItems
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

    private fun ForYouShelfItem.withArtworkOnly(): ForYouShelfItem? {
        if (artist.thumbnailUrl.isNullOrBlank()) return null
        val tilesWithArtwork = tiles.filter { tile ->
            when (tile) {
                is ForYouShelfTile.Local -> !tile.item.thumbnailUrl.isNullOrBlank()
                is ForYouShelfTile.RecommendedAlbum -> !tile.item.thumbnail.isNullOrBlank()
            }
        }
        return if (tilesWithArtwork.size < 3) null else copy(tiles = tilesWithArtwork)
    }

    private suspend fun buildForYouShelfBatch(
        previous: List<ForYouShelfItem> = emptyList(),
        existing: List<ForYouShelfItem> = emptyList(),
        limit: Int = INITIAL_FOR_YOU_SHELF_COUNT,
    ): List<ForYouShelfItem> {
        val now = System.currentTimeMillis()
        val recent = database.mostPlayedArtists(now - 86400000L * 30, limit = 25, toTimeStamp = now).first()
        val followed = database.artistsBookmarked(ArtistSortType.CREATE_DATE, true).first()
        val library = database.artists(ArtistSortType.PLAY_TIME, true).first()
        val existingArtistIds = existing.mapTo(mutableSetOf()) { it.artist.id }
        val previousArtistIds = previous.map { it.artist.id }
        val artists = (recent + followed + keepListening.value.orEmpty().filterIsInstance<Artist>() + library)
            .filter { !it.thumbnailUrl.isNullOrBlank() && it.id !in existingArtistIds && !it.artist.isPodcastChannel }
            .distinctBy { it.id }
            .shuffled()
            .sortedBy { artist ->
                val previousIndex = previousArtistIds.indexOf(artist.id)
                if (previousIndex < 0) 0 else previousArtistIds.size - previousIndex
            }
            .take(INITIAL_FOR_YOU_ARTIST_SCAN_LIMIT)
        if (artists.isEmpty()) return emptyList()

        val hideExplicit = context.dataStore.get(HideExplicitKey, false)
        val localAlbums = database.artistAlbumsForArtists(artists.map { it.id }).first()
            .filter { it.album.songCount > 1 && !it.thumbnailUrl.isNullOrBlank() && (!hideExplicit || !it.album.explicit) }
        val usedAlbumIds = existing.flatMap { it.tiles }.mapTo(mutableSetOf()) { it.id }
        val previousAlbumIds = previous.flatMap { it.tiles }.mapTo(mutableSetOf()) { it.id }
        val shelves = mutableListOf<ForYouShelfItem>()
        for (group in artists.chunked(3)) {
            val remoteAlbums = coroutineScope {
                group.map { artist ->
                    async {
                        if (artist.artist.isYouTubeArtist && networkConnectivity.networkStatus.value) {
                            getForYouRemoteAlbums(artist.id, hideExplicit)
                        } else {
                            emptyList()
                        }
                    }
                }.awaitAll()
            }
            for ((artist, albums) in group.zip(remoteAlbums)) {
                val recommended = albums.map { ForYouShelfTile.RecommendedAlbum(it) }
                val local = localAlbums.filter { album -> album.artists.any { it.id == artist.id } }
                    .shuffled()
                    .map { ForYouShelfTile.Local(it) }
                val tiles = (recommended + local)
                    .filterNot { it.id in usedAlbumIds }
                    .sortedBy { it.id in previousAlbumIds }
                    .distinctBy { it.id }
                    .distinctBy { tile ->
                        normalizeForYouAlbumTitle(when (tile) {
                            is ForYouShelfTile.Local -> tile.item.title
                            is ForYouShelfTile.RecommendedAlbum -> tile.item.title
                        })
                    }
                    .distinctBy { tile ->
                        normalizeForYouArtwork(when (tile) {
                            is ForYouShelfTile.Local -> tile.item.thumbnailUrl
                            is ForYouShelfTile.RecommendedAlbum -> tile.item.thumbnail
                        })
                    }
                    .take(3)
                if (tiles.size == 3) {
                    shelves += ForYouShelfItem(artist, tiles)
                    usedAlbumIds += tiles.map { it.id }
                    if (shelves.size == limit) return shelves
                }
            }
        }
        return shelves
    }

    private suspend fun getForYouRemoteAlbums(artistId: String, hideExplicit: Boolean): List<AlbumItem> {
        fun List<YTItem>.albums() = filterIsInstance<AlbumItem>()
            .filter { it.thumbnail.isNotBlank() && (!hideExplicit || !it.explicit) }
            .filterNot { it.albumType?.trim()?.lowercase() in setOf("single", "singolo") }

        var seedAlbums = emptyList<AlbumItem>()
        val relatedAlbums = mutableListOf<AlbumItem>()
        withTimeoutOrNull(5_000L) {
            val items = YouTube.artist(artistId).getOrNull()?.sections.orEmpty().flatMap { it.items }
            seedAlbums = items.albums()
            val relatedArtists = items.filterIsInstance<ArtistItem>()
                .distinctBy { it.id }
                .filterNot { it.id == artistId }
                .shuffled()
                .take(2)
            for (related in relatedArtists) {
                relatedAlbums += YouTube.artist(related.id).getOrNull()
                    ?.sections.orEmpty().flatMap { it.items }.albums()
                if (relatedAlbums.distinctBy { it.id }.size >= 6) break
            }
        }
        return (relatedAlbums.shuffled() + seedAlbums.shuffled()).distinctBy { it.id }
            .sortedBy { album -> album.artists.orEmpty().any { it.id == artistId } }
    }

    private fun launchForYouLoad(rotate: Boolean = false) = viewModelScope.launch(Dispatchers.IO) {
        try {
            forYouPreparationMutex.withLock {
                val shown = forYouShelves.value
                if (shown.isEmpty() || rotate) {
                    val hideExplicit = context.dataStore.get(HideExplicitKey, false)
                    val prepared = pendingForYouShelves.filter { shelf ->
                        !hideExplicit || shelf.tiles.none { tile ->
                            when (tile) {
                                is ForYouShelfTile.Local -> (tile.item as? Album)?.album?.explicit == true
                                is ForYouShelfTile.RecommendedAlbum -> tile.item.explicit
                            }
                        }
                    }
                    val next = prepared.ifEmpty { buildForYouShelfBatch(previous = shown) }
                    if (next.isNotEmpty()) {
                        forYouShelves.value = next
                        HomeCache.forYouShelves = next
                        pendingForYouShelves = emptyList()
                        persistForYouSnapshot(next)
                    }
                }
                isForYouLoading.value = false
                val current = forYouShelves.value
                if (current.isNotEmpty() && pendingForYouShelves.isEmpty()) {
                    val next = buildForYouShelfBatch(previous = current)
                    if (next.isNotEmpty()) {
                        persistForYouSnapshot(next)
                        pendingForYouShelves = next
                    }
                }
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            reportException(error)
        } finally {
            isForYouLoading.value = false
        }
    }

    fun loadMoreForYouShelves() {
        if (isForYouLoading.value || !_isLoadingMoreForYou.compareAndSet(false, true)) return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                forYouPreparationMutex.withLock {
                    val existing = forYouShelves.value
                    val newShelves = buildForYouShelfBatch(existing = existing, limit = 10)
                    if (newShelves.isNotEmpty()) {
                        val combined = existing + newShelves
                        forYouShelves.value = combined
                        HomeCache.forYouShelves = combined
                    }
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                reportException(error)
            } finally {
                _isLoadingMoreForYou.value = false
            }
        }
    }

    fun regenerateForYouShelves() {
        if (!isForYouLoading.value || forYouShelves.value.isNotEmpty()) {
            launchForYouLoad(rotate = true)
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

        val seeds = likedSongs.randomSample(5).distinctBy { it.id }
        val items = java.util.Collections.synchronizedList(mutableListOf<DailyDiscoverItem>())
        val semaphore = Semaphore(2)

        kotlinx.coroutines.coroutineScope {
            seeds.map { seed ->
                launch(Dispatchers.IO) {
                    semaphore.withPermit {
                        val endpoint = YouTube.next(WatchEndpoint(videoId = seed.id)).getOrNull()?.relatedEndpoint
                        if (endpoint != null) {
                            YouTube.related(endpoint).onSuccess { page ->
                                val recommendation = page.songs
                                    .asSequence()
                                    .filter { !it.isVideoSong && !it.explicit && it.id != seed.id }
                                    .take(20)
                                    .toList()
                                    .randomSample(1)
                                    .firstOrNull()

                                if (recommendation != null) {
                                    items.add(
                                        DailyDiscoverItem(
                                            seed = seed,
                                            recommendation = recommendation,
                                            relatedEndpoint = endpoint,
                                        ),
                                    )
                                    synchronized(items) {
                                        dailyDiscover.value = items.toList().distinctBy { it.recommendation.id }
                                    }
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

    private fun quickPickFallbackSong(item: SongItem): Song = Song(
            song = item.toMediaMetadata().toSongEntity(),
            artists = item.artists.map { artist ->
                ArtistEntity(id = artist.id ?: ArtistEntity.generateArtistId(), name = artist.name)
            },
        )

    private suspend fun quickPickSongs(items: List<SongItem>): List<Song> {
        val uniqueItems = items.distinctBy { it.id }
        val stored = if (uniqueItems.isEmpty()) emptyMap() else {
            runCatching { database.getSongsByIds(uniqueItems.map { it.id }) }
                .getOrDefault(emptyList())
                .associateBy { it.id }
        }
        return uniqueItems.map { item -> stored[item.id] ?: quickPickFallbackSong(item) }
    }

    private suspend fun fillQuickPicksFromHome(page: HomePage, hideExplicit: Boolean) {
        if (quickPicks.value.orEmpty().size >= QUICK_PICKS_TARGET_COUNT) return
        val homeSongs = page.sections.flatMap { it.items }
            .filterIsInstance<SongItem>()
            .filter { !it.isVideoSong && !it.isEpisode && (!hideExplicit || !it.explicit) }
            .distinctBy { it.id }
            .shuffled()
        val songs = eligibleQuickPicks(quickPicks.value.orEmpty(), hideExplicit).toMutableList()
        val usedIds = songs.mapTo(mutableSetOf()) { it.id }
        val candidates = homeSongs.asSequence()
            .filter { it.id !in usedIds }
            .take(QUICK_PICKS_TARGET_COUNT - songs.size)
            .toList()
        for (song in eligibleQuickPicks(quickPickSongs(candidates), hideExplicit)) {
            if (usedIds.add(song.id)) songs += song
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
                .toList()
            val hydratedCandidates = quickPickSongs(candidates)
            quickPicks.update { current ->
                eligibleQuickPicks(current.orEmpty() + hydratedCandidates, hideExplicit)
                    .take(QUICK_PICKS_TARGET_COUNT)
            }
        }
    }

    private suspend fun getCommunityPlaylists() {
        val fromTimeStamp = System.currentTimeMillis() - 86400000L * 7 * 4
        val semaphore = Semaphore(2)
        val artistSeeds = database.mostPlayedArtists(fromTimeStamp, limit = 10).first()
            .filter { it.artist.isYouTubeArtist }
            .randomSample(3)
        val songSeeds = database.mostPlayedSongs(fromTimeStamp, limit = 5).first()
            .randomSample(2)

        val candidatePlaylists = java.util.Collections.synchronizedList(mutableListOf<PlaylistItem>())

        kotlinx.coroutines.coroutineScope {
            artistSeeds.map { seed ->
                launch(Dispatchers.IO) {
                    semaphore.withPermit {
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
            }
            songSeeds.map { seed ->
                launch(Dispatchers.IO) {
                    semaphore.withPermit {
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
        }

        val uniqueCandidates = candidatePlaylists.distinctBy { it.id }.shuffled().take(5)

        val playlists = java.util.Collections.synchronizedList(mutableListOf<CommunityPlaylistItem>())

        kotlinx.coroutines.coroutineScope {
            uniqueCandidates.map { playlist ->
                launch(Dispatchers.IO) {
                    semaphore.withPermit {
                        YouTube.playlist(playlist.id).onSuccess { page ->
                            val songs = page.songs.take(10)
                            if (songs.isNotEmpty()) {
                                val songCountText = page.playlist.songCountText ?: playlist.songCountText
                                val updatedPlaylist = playlist.copy(songCountText = songCountText)
                                playlists.add(CommunityPlaylistItem(updatedPlaylist, songs))
                            }
                        }
                    }
                }
            }.forEach { it.join() }
        }

        communityPlaylists.value = playlists.shuffled()
    }

    private suspend fun getSimilarRecommendations(
        onlySeedId: String? = null,
        replaceExisting: Boolean = false,
    ) = coroutineScope {
        val hideExplicit = context.dataStore.get(HideExplicitKey, false)
        val fromTimeStamp = System.currentTimeMillis() - 86400000L * 14
        val artists = (
            database.mostPlayedArtists(fromTimeStamp, limit = 40).first() +
                database.artistsBookmarked(ArtistSortType.CREATE_DATE, descending = true).first() +
                database.artists(ArtistSortType.PLAY_TIME, descending = true).first()
            ).distinctBy { it.id }.filter { it.artist.isYouTubeArtist && !it.thumbnailUrl.isNullOrBlank() }
            .randomSample(20)
        val albums = (
            database.mostPlayedAlbums(fromTimeStamp, limit = 40).first() +
                database.albums(AlbumSortType.PLAY_TIME, descending = true).first()
            ).distinctBy { it.id }.filter { !it.thumbnailUrl.isNullOrBlank() && it.artists.isNotEmpty() }
            .randomSample(20)
        val playlists = database.playlists(PlaylistSortType.LAST_UPDATED, descending = true).first()
            .filter { !it.playlist.isLocal && !it.playlist.browseId.isNullOrBlank() && it.thumbnails.isNotEmpty() }
            .distinctBy { it.playlist.browseId }.randomSample(20)

        val localSeeds = buildList<LocalItem> {
            repeat(maxOf(artists.size, albums.size, playlists.size)) { index ->
                artists.getOrNull(index)?.let(::add)
                albums.getOrNull(index)?.let(::add)
                playlists.getOrNull(index)?.let(::add)
            }
        }
        val generated: MutableList<SimilarRecommendation> = if (replaceExisting) {
            mutableListOf()
        } else {
            similarRecommendations.value.orEmpty()
                .filter { it.title is Artist || it.title is Album || it.title is Playlist }
                .filter { recommendation ->
                    !recommendation.thumbnailUrl.isNullOrBlank() &&
                        recommendation.items.distinctBy { it.id }.count { item ->
                            item is SongItem && !item.isVideoSong || item is AlbumItem ||
                                item is ArtistItem || item is PlaylistItem
                        } >= 2
                }
                .distinctBy { it.title.id }.take(MAX_SIMILAR_RECOMMENDATIONS).toMutableList()
        }
        val refreshSeed = onlySeedId?.let { id ->
            similarRecommendations.value.orEmpty().firstOrNull { it.title.id == id }?.title
        }
        val seedsToGenerate = if (refreshSeed != null) listOf(refreshSeed) else localSeeds
        val semaphore = Semaphore(2)
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
            val existingIds = publishLock.withLock {
                // Consume the lazy sequence while publication cannot mutate its backing list.
                generated.asSequence()
                    .filterNot { it.title.id == seed.id }
                    .flatMap { it.items.asSequence() }
                    .map { it.id }
                    .toSet()
            }
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
            val items = communityPlaylists.value.orEmpty()
                .mapNotNull { mapYTItemToSnapshot(it.playlist) }
                .distinctBy { it.id }
                .take(5)
            if (items.isNotEmpty()) {
                val updatedAt = System.currentTimeMillis()
                persistPendingRecommendationSnapshot { snapshot ->
                    snapshot.copy(communityUpdatedAt = updatedAt, communityPlaylists = items)
                }
            }
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
            val items = dailyDiscover.value.orEmpty().mapNotNull { item ->
                val recommendation = mapYTItemToSnapshot(item.recommendation) ?: return@mapNotNull null
                HomeDailyDiscoverSnapshotItem(item.seed.toHomeSnapshotItem(), recommendation)
            }.distinctBy { it.recommendation.id }.take(5)
            if (items.isNotEmpty()) {
                val updatedAt = System.currentTimeMillis()
                persistPendingRecommendationSnapshot { snapshot ->
                    snapshot.copy(dailyDiscoverUpdatedAt = updatedAt, dailyDiscover = items)
                }
            }
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
            val shelves = similarRecommendations.value.orEmpty()
                .mapNotNull { it.toHomeSimilarShelfSnapshot() }
                .distinctBy { it.titleId }
                .take(MAX_SIMILAR_RECOMMENDATIONS)
            if (shelves.size >= 2) {
                val updatedAt = System.currentTimeMillis()
                persistPendingRecommendationSnapshot { snapshot ->
                    snapshot.copy(similarUpdatedAt = updatedAt, similarShelves = shelves)
                }
            }
        }
    }

    fun onSectionBecameVisible(
        sectionId: String,
        forceRefresh: Boolean = isFirstHomeViewModelInProcess,
    ) {
        val current = visibleSections.value
        if (sectionId in current) return
        visibleSections.value = current + sectionId

        when {
            sectionId == "daily_discover" && dailyDiscoverLaunchJob == null -> {
                dailyDiscoverLaunchJob = viewModelScope.launch(Dispatchers.IO) {
                    try {
                        phase1Complete.filter { it }.first()
                        if (forceRefresh || !isSectionSnapshotFresh(
                                currentRecommendationSnapshot.dailyDiscoverUpdatedAt,
                                pendingRecommendationSnapshot?.dailyDiscoverUpdatedAt ?: 0L,
                            )
                        ) {
                            val completed = withTimeoutOrNull(20_000L) { getDailyDiscover(); true } == true
                            if (completed) {
                                val items = dailyDiscover.value.orEmpty().mapNotNull { item ->
                                    val seed = item.seed.toHomeSnapshotItem()
                                    val recommendation = mapYTItemToSnapshot(item.recommendation) ?: return@mapNotNull null
                                    HomeDailyDiscoverSnapshotItem(seed, recommendation)
                                }.distinctBy { it.recommendation.id }.take(5)
                                if (items.isNotEmpty()) {
                                    val updatedAt = System.currentTimeMillis()
                                    persistPendingRecommendationSnapshot { snapshot ->
                                        snapshot.copy(dailyDiscoverUpdatedAt = updatedAt, dailyDiscover = items)
                                    }
                                }
                            }
                        }
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
                        if (forceRefresh || !isSectionSnapshotFresh(
                                currentRecommendationSnapshot.communityUpdatedAt,
                                pendingRecommendationSnapshot?.communityUpdatedAt ?: 0L,
                            )
                        ) {
                            val completed = withTimeoutOrNull(20_000L) { getCommunityPlaylists(); true } == true
                            if (completed) {
                                val items = communityPlaylists.value.orEmpty()
                                    .mapNotNull { mapYTItemToSnapshot(it.playlist) }
                                    .distinctBy { it.id }
                                    .take(5)
                                if (items.isNotEmpty()) {
                                    val updatedAt = System.currentTimeMillis()
                                    persistPendingRecommendationSnapshot { snapshot ->
                                        snapshot.copy(communityUpdatedAt = updatedAt, communityPlaylists = items)
                                    }
                                }
                            }
                        }
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
                        if (forceRefresh || !isSectionSnapshotFresh(
                                currentRecommendationSnapshot.similarUpdatedAt,
                                pendingRecommendationSnapshot?.similarUpdatedAt ?: 0L,
                            )
                        ) {
                            val completed = withTimeoutOrNull(30_000L) {
                                getSimilarRecommendations(replaceExisting = forceRefresh)
                                true
                            } == true
                            if (completed) {
                                val shelves = similarRecommendations.value.orEmpty()
                                    .mapNotNull { it.toHomeSimilarShelfSnapshot() }
                                    .distinctBy { it.titleId }
                                    .take(MAX_SIMILAR_RECOMMENDATIONS)
                                if (shelves.size >= 2) {
                                    val updatedAt = System.currentTimeMillis()
                                    persistPendingRecommendationSnapshot { snapshot ->
                                        snapshot.copy(similarUpdatedAt = updatedAt, similarShelves = shelves)
                                    }
                                }
                            }
                        }
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

    private suspend fun load(forceHeroCarouselRefresh: Boolean = false) {
        refreshHomeFeed()
        refreshHeroCarousel(defaultHeroCarouselSeed(), force = forceHeroCarouselRefresh)
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
        val quickPicksNeedRefresh =
            forceHeroCarouselRefresh || isFirstHomeViewModelInProcess || !isQuickPicksSnapshotFresh()

        isForYouLoading.value = true

        try {
            coroutineScope {
                if (quickPicksNeedRefresh) {
                    launch(Dispatchers.IO) {
                        runCatching { getQuickPicks() }.onFailure { reportException(it) }
                    }
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
            sectionsToRestart.forEach {
                onSectionBecameVisible(
                    it,
                    forceRefresh = forceHeroCarouselRefresh || isFirstHomeViewModelInProcess,
                )
            }
        }

        launchForYouLoad(rotate = forceHeroCarouselRefresh)

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

        if (quickPicksNeedRefresh) {
            viewModelScope.launch(Dispatchers.IO) {
                enrichQuickPicksFromNetwork()
                HomeCache.quickPicks = quickPicks.value
            }
        }
    }

    @Synchronized
    private fun refreshHomeFeed() {
        if (homeFeedJob?.isActive == true) return
        isHomeFeedLoading.value = true
        homeFeedJob = viewModelScope.launch(Dispatchers.IO) {
            try {
                if (networkConnectivity.networkStatus.value) {
                    withTimeout(20_000L) { fetchHomeFeed() }
                }
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

            if (HOME_MOOD_SECTION_ENABLED) {
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
        if (!HOME_FAVORITES_DISCOVERIES_SECTION_ENABLED) return
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
                    load(forceHeroCarouselRefresh = true)
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
            if (HOME_MOOD_SECTION_ENABLED) prefs[MoodSnapshotKey]?.let { json ->
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
                            moodMixItems.value = mixes
                            loadedMoodParams.value = snapshot.chipParams
                        }
                        if (clean != snapshot) context.dataStore.edit { it[MoodSnapshotKey] = snapshotJson.encodeToString(clean) }
                    }
            }
            if (HOME_MOOD_SECTION_ENABLED) {
                preferredMoodParams.value = cachedMoodSnapshot.value?.chipParams
                    ?: prefs[LastMoodChipParamsKey]
            }
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
            val prefs = context.dataStore.data.first()
            if (HomeCache.lastLoadedAt == 0L) {
                HomeCache.lastLoadedAt = context.dataStore.get(HomeCacheLastLoadedKey, 0L)
            }
            (HomeCache.homePage ?: HomeFeedSnapshotStore.load(context)?.page)?.let { restoredPage ->
                homePage.value = restoredPage
                updateHomePersonalMixes(restoredPage)
            }
            if (isHeroCarouselEnabled.value) restoreHeroCarouselSnapshot(prefs)
            restoreHomeRecommendationSnapshot(prefs)
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
                .mapNotNull { it.withArtworkOnly() }
                .take(INITIAL_FOR_YOU_SHELF_COUNT)
            HomeCache.forYouShelves = forYouShelves.value
            trackQuickPicksSnapshot()
            if (!HomeCache.isStale() && HomeCache.homePage != null) {
                homePage.value = HomeCache.homePage
                if (HOME_FAVORITES_DISCOVERIES_SECTION_ENABLED && homePersonalMixes.value.isEmpty()) {
                    HomeCache.homePage?.freshFindOldFavoritePlaylists()
                        ?.takeIf { it.isNotEmpty() }
                        ?.let { homePersonalMixes.value = it }
                }
                launchForYouLoad()
                isHomeFeedLoading.value = false
                if (YouTube.cookie != null) {
                    viewModelScope.launch(Dispatchers.IO) { loadAccountPlaylists() }
                } else {
                    isAccountPlaylistsLoading.value = false
                }
                refreshHeroCarousel()
                isPhase1Complete.value = true
                phase2DischiPerTeDone.value = true
                phase1Complete.value = true
                isLoading.value = false
                refreshHomeFeed()
                if (!isQuickPicksSnapshotFresh()) {
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
        super.onCleared()
    }

}
