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
import com.metrolist.music.constants.HideVideoSongsKey
import com.metrolist.music.constants.HideYoutubeShortsKey
import com.metrolist.music.constants.HomeCacheLastLoadedKey
import com.metrolist.music.constants.AccountNameKey
import com.metrolist.music.constants.ArtistSortType
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
import com.metrolist.music.constants.HeroCarouselEnabledKey
import com.metrolist.music.constants.LastDiscoveryWeeklySyncKey
import com.metrolist.music.constants.SeenNewReleaseFirstIdsKey
import com.metrolist.music.constants.RandomizeHomeOrderKey
import com.metrolist.music.constants.ShowWrappedCardKey
import com.metrolist.music.discovery.AlbumRecommendationsGenerator
import com.metrolist.music.discovery.DiscoveryWeeklyGenerator
import com.metrolist.music.discovery.HeroCarouselGenerator
import com.metrolist.music.models.DischiPerTeItem
import com.metrolist.music.models.ForYouShelfItem
import com.metrolist.music.models.HeroCarouselItem
import com.metrolist.music.models.stableKey
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
import com.metrolist.music.db.entities.Artist
import com.metrolist.music.db.entities.ArtistEntity
import com.metrolist.music.db.entities.LocalItem
import com.metrolist.music.db.entities.PlaylistEntity
import com.metrolist.music.db.entities.PlaylistSongMap
import com.metrolist.music.db.entities.Song
import com.metrolist.music.db.entities.SongEntity
import com.metrolist.music.db.entities.SpeedDialItem
import com.metrolist.music.extensions.filterVideoSongs
import com.metrolist.music.extensions.toEnum
import com.metrolist.music.models.SimilarRecommendation
import com.metrolist.music.ui.screens.wrapped.WrappedAudioService
import com.metrolist.music.ui.screens.wrapped.WrappedManager
import com.metrolist.music.utils.NewReleaseNotifier
import com.metrolist.music.utils.SyncUtils
import com.metrolist.music.utils.dataStore
import com.metrolist.music.utils.get
import com.metrolist.music.utils.reportException
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.withLock
import java.time.LocalDate
import javax.inject.Inject
import kotlin.random.Random

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
class HomeViewModel @Inject constructor(
    @ApplicationContext val context: Context,
    val database: MusicDatabase,
    val syncUtils: SyncUtils,
    val wrappedManager: WrappedManager,
    private val wrappedAudioService: WrappedAudioService,
    private val newReleaseNotifier: NewReleaseNotifier,
) : ViewModel() {
    val syncState = syncUtils.syncState

    val syncBannerLaunchCount: StateFlow<Int> = context.dataStore.data
        .map { it[SyncBannerLaunchCountKey] ?: 0 }
        .stateIn(viewModelScope, SharingStarted.Eagerly, 0)

    val isRefreshing = MutableStateFlow(false)
    val isLoading = MutableStateFlow(false)
    // Section ids currently being manually regenerated via the section's own refresh button
    // (e.g. "quick_picks", "dischi_per_te") — drives the spinning refresh icon per section.
    val regeneratingSections = MutableStateFlow<Set<String>>(emptySet())
    val isRandomizing = MutableStateFlow(false)
    val isPhase1Complete = MutableStateFlow(false)
    val phase1Complete = MutableStateFlow(false)
    val phase2Complete = MutableStateFlow(false)
    val visibleSections: MutableStateFlow<Set<String>> = MutableStateFlow(setOf("speed_dial", "mood_and_genres", "discovery"))

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
    val explorePage = MutableStateFlow<ExplorePage?>(null)
    val communityPlaylists = MutableStateFlow<List<CommunityPlaylistItem>?>(null)
    val selectedChip = MutableStateFlow<HomePage.Chip?>(null)
    private val previousHomePage = MutableStateFlow<HomePage?>(null)

    private val moodMapping = mapOf(
        "Energize" to "Dopaminergic",
        "Workout" to "Dopaminergic",
        "Gym" to "Dopaminergic",
        "Relax" to "Rest",
        "Sleep" to "Rest",
        "Sad" to "Melancholic",
        "Focus" to "Wellness",
        "Feel Good" to "Wellness",
        "Party" to "Festive",
        "Commute" to "Travel",
        "Romance" to "Love"
    )

    private fun transformChips(chips: List<HomePage.Chip>?): List<HomePage.Chip>? {
        return chips?.filter { chip ->
            val title = chip.title.lowercase()
            !title.contains("home") && !title.contains("podcast")
        }?.shuffled()?.map { chip ->
            chip.copy(title = moodMapping[chip.title] ?: chip.title)
        }
    }

    // Official API data for podcast sections
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
    private var lastMoodChipParams: String? = null

    val isHeroCarouselEnabled: StateFlow<Boolean> = context.dataStore.data
        .map { it[HeroCarouselEnabledKey] ?: true }
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)

    val heroCarouselItems = MutableStateFlow<List<HeroCarouselItem>>(emptyList())

    private val heroCarouselGenerator = HeroCarouselGenerator(database)

    val dischiPerTe = MutableStateFlow<List<DischiPerTeItem>?>(null)
    val forYouShelves = MutableStateFlow<List<ForYouShelfItem>>(emptyList())
    private var forYouArtistPool: List<Artist> = emptyList()
    private var forYouPoolCursor = 0
    private val _isLoadingMoreForYou = MutableStateFlow(false)

    private val albumRecommendationsGenerator = AlbumRecommendationsGenerator(database)

    private val discoveryWeeklyGenerator = DiscoveryWeeklyGenerator(database)
    private val discoveryWeeklySyncMutex = kotlinx.coroutines.sync.Mutex()

    val discoveryWeeklyPlaylist = database
        .playlist(PlaylistEntity.DISCOVER_WEEKLY_PLAYLIST_ID)
        .stateIn(viewModelScope, SharingStarted.Lazily, null)

    // Regenerates the Discovery Weekly playlist once every 7 days (or immediately if it has
    // never been built), same cooldown mechanism as the Weekly/Monthly Most playlists in
    // StatsViewModel. Safe to call on every cold start — it's a no-op most of the time.
    fun syncDiscoveryWeeklyIfNeeded(force: Boolean = false) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                discoveryWeeklySyncMutex.withLock {
                    val prefs = context.dataStore.data.first()
                    val lastSyncMillis = prefs[LastDiscoveryWeeklySyncKey]
                    // songCount, not just row existence — a row can exist with 0 songs (e.g. a
                    // past generation that came up empty) and the 7-day cooldown would otherwise
                    // lock that empty state in for a week before trying again.
                    val existingRow = database.playlist(PlaylistEntity.DISCOVER_WEEKLY_PLAYLIST_ID).first()
                    val playlistReady = existingRow != null && existingRow.songCount > 0
                    val due = lastSyncMillis == null ||
                        java.time.Instant.ofEpochMilli(lastSyncMillis).plus(java.time.Duration.ofDays(7))
                            .isBefore(java.time.Instant.now())
                    if (!force && !due && playlistReady) return@withLock

                    val hideExplicit = context.dataStore.get(HideExplicitKey, false)
                    val hideVideoSongs = context.dataStore.get(HideVideoSongsKey, false)
                    val songs = discoveryWeeklyGenerator.generate(
                        hideExplicit = hideExplicit,
                        hideVideoSongs = hideVideoSongs,
                        seed = java.time.LocalDate.now().toEpochDay() / 7,
                    )
                    if (songs.isEmpty()) return@withLock

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
                }
            } catch (e: Exception) {
                reportException(e)
            }
        }
    }

    // Per-launch, not per-day: a date-derived seed reproduced the exact same shuffle order on
    // every cold start within the same day, so "Featured for you" looked frozen no matter how
    // many times the app was reopened.
    private fun defaultHeroCarouselSeed() = System.currentTimeMillis()

    private var lastHeroCarouselSeed = defaultHeroCarouselSeed()

    // Cold start fires several async arrivals (cache restore, explorePage, homePage,
    // dischiPerTe) that each used to trigger a full regenerate — same seed, but a
    // different pool mix each time reshuffles the whole list, silently swapping out
    // the card the user is already looking at. Only the first successful generation
    // (or an explicit force, e.g. pull-to-refresh) replaces the list; every later
    // arrival only appends newly available cards to the end via [appendHeroCarousel].
    private var heroCarouselFirstGenDone = false

    fun refreshHeroCarousel(seed: Long = lastHeroCarouselSeed, force: Boolean = false) {
        if (!force && heroCarouselFirstGenDone) {
            appendHeroCarousel()
            return
        }
        lastHeroCarouselSeed = seed
        viewModelScope.launch(Dispatchers.IO) {
            val seenAsFirstIds = context.dataStore.data.first()[SeenNewReleaseFirstIdsKey] ?: emptySet()
            val result = heroCarouselGenerator.generate(
                explorePage = explorePage.value,
                homePage = homePage.value,
                dischiPerTe = dischiPerTe.value.orEmpty(),
                seed = seed,
                seenAsFirstIds = seenAsFirstIds,
            )
            heroCarouselItems.value = result.items
            heroCarouselFirstGenDone = true
            if (result.seenAsFirstIds != seenAsFirstIds) {
                context.dataStore.edit { it[SeenNewReleaseFirstIdsKey] = result.seenAsFirstIds }
            }
        }
    }

    // Panels only ever get added on the right, never reordered or replaced — cards
    // already shown keep their position no matter what data arrives afterwards.
    private fun appendHeroCarousel() {
        viewModelScope.launch(Dispatchers.IO) {
            val seenAsFirstIds = context.dataStore.data.first()[SeenNewReleaseFirstIdsKey] ?: emptySet()
            val fresh = heroCarouselGenerator.generate(
                explorePage = explorePage.value,
                homePage = homePage.value,
                dischiPerTe = dischiPerTe.value.orEmpty(),
                seed = lastHeroCarouselSeed,
                seenAsFirstIds = seenAsFirstIds,
            )
            val existingKeys = heroCarouselItems.value.map { it.stableKey() }.toSet()
            val appended = fresh.items.filterNot { it.stableKey() in existingKeys }
            if (appended.isNotEmpty()) {
                heroCarouselItems.value = (heroCarouselItems.value + appended).take(10)
            }
        }
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
        val mixItems = page.sections
            .flatMap { it.items }
            .filterIsInstance<PlaylistItem>()
            .take(10)
        if (mixItems.isEmpty()) return
        val snapshotItems = mixItems.mapNotNull { mapYTItemToSnapshot(it) }
        val existingIds = cachedMoodSnapshot.value
            ?.takeIf { it.chipParams == chipParams }
            ?.items?.map { it.id }
        if (existingIds == snapshotItems.map { it.id }) return
        val snapshot = MoodSnapshot(System.currentTimeMillis(), chipTitle, chipParams, snapshotItems)
        cachedMoodSnapshot.value = snapshot
        refreshHeroCarousel()
        val json = runCatching { snapshotJson.encodeToString(snapshot) }.getOrNull() ?: return
        context.dataStore.edit { it[MoodSnapshotKey] = json }
    }

    // Mixes actually rendered by the "Your Mood" row. Kept separate from moodPage so a chip
    // switch can hydrate instantly from the last cached snapshot instead of showing a blank
    // loader every time, and so the row never has to swap between differently-sized content.
    val moodMixItems = MutableStateFlow<List<PlaylistItem>?>(null)
    val isMoodLoading = MutableStateFlow(false)
    private var moodPageJob: kotlinx.coroutines.Job? = null

    fun loadMoodPage(params: String?, chipTitle: String? = null, hideExplicit: Boolean, hideVideoSongs: Boolean, hideYoutubeShorts: Boolean) {
        if (params == lastMoodChipParams && moodPage.value != null) return
        lastMoodChipParams = params
        if (params == null) {
            moodPageJob?.cancel()
            moodPage.value = null
            moodMixItems.value = null
            isMoodLoading.value = false
            return
        }
        // Cancel any still-in-flight load for a previously selected chip first. Without this,
        // switching chips quickly could let an older/slower request finish after a newer one and
        // overwrite its result — or, worse, its `finally` block flips isMoodLoading back to false
        // while the newer request is still genuinely loading, making the row look "done" with
        // stale content instead of showing the spinner for the chip actually selected.
        moodPageJob?.cancel()
        moodPageJob = viewModelScope.launch(Dispatchers.IO) {
            val myJob = coroutineContext[kotlinx.coroutines.Job]
            isMoodLoading.value = true
            try {
                // YouTube.home() has no built-in timeout of its own. If the request stalls (dead
                // connection, slow/no network), isMoodLoading would otherwise never resolve and
                // the Mood row would spin forever — this is the "gets stuck in infinite loading"
                // bug: bounding it here guarantees the section always reaches a loaded, empty, or
                // (falling through to the catch below) still-showing-previous-content state
                // within a bounded time instead of hanging indefinitely.
                val result = withTimeout(15_000L) { YouTube.home(params = params) }
                result.onSuccess { nextSections ->
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
                    val mixItems = filteredPage.sections
                        .flatMap { it.items }
                        .filterIsInstance<PlaylistItem>()
                        .take(10)
                    // Only replace what's on screen once the new mixes are actually in hand —
                    // never swap to an empty list just because this particular chip came back thin.
                    if (mixItems.isNotEmpty()) moodMixItems.value = mixItems
                    if (chipTitle != null) {
                        context.dataStore.edit { prefs ->
                            prefs[LastMoodChipTitleKey] = chipTitle
                            prefs[LastMoodChipParamsKey] = params
                        }
                        saveMoodSnapshotAfterLoad(chipTitle, params, filteredPage)
                    }
                }.onFailure {
                    // Leave whatever was already on screen (cached snapshot or the previous
                    // chip's mixes) in place instead of clearing it — a failed refresh shouldn't
                    // blank out a section that had valid content a moment ago. lastMoodChipParams
                    // was already updated above, but moodPage.value stays null/stale here, so the
                    // early-return guard at the top of this function won't skip a retry later.
                    reportException(it)
                }
            } catch (e: TimeoutCancellationException) {
                reportException(e)
            } finally {
                // Guard against a just-cancelled/late job clobbering a newer one's loading state
                // (see comment above) — only the most recently launched load is allowed to flip
                // isMoodLoading back to false.
                if (moodPageJob === myJob) isMoodLoading.value = false
            }
        }
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

            // Fallback to YouTube home page songs when local DB has no data (e.g. fresh install).
            // Preferred over Quick Picks so the two sections don't cannibalize each other's pool.
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
        // Pin SpeedDial only when a cached snapshot gives immediate content
        if (hasCachedSpeedDial && list.contains(HomeSection.SpeedDial)) finalItems.add(HomeSection.SpeedDial)
        // Always pin QuickPicks second regardless of random order
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
            // Visual feedback for the animation
            kotlinx.coroutines.delay(1000)

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

            // Probability: 80% User Songs, 20% Other Sources
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
    // Track last processed cookie to avoid unnecessary updates
    private var lastProcessedCookie: String? = null
    // Track if we're currently processing account data
    private var isProcessingAccountData = false

    private var dailyDiscoverLaunchJob: kotlinx.coroutines.Job? = null
    private var communityPlaylistsLaunchJob: kotlinx.coroutines.Job? = null
    private var similarRecommendationsLaunchJob: kotlinx.coroutines.Job? = null
    private var dischiPerTeLaunchJob: kotlinx.coroutines.Job? = null
    private var phase2DailyDiscoverDone = false
    private var phase2CommunityDone = false
    private var phase2SimilarDone = false
    private var phase2DischiPerTeDone = false

    private fun checkPhase2Complete() {
        if (phase2DailyDiscoverDone && phase2CommunityDone && phase2SimilarDone && phase2DischiPerTeDone) {
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
    private suspend fun buildForYouShelf(artist: Artist, fromTimeStamp: Long, toTimeStamp: Long): ForYouShelfItem? {
        val usedAlbumIds = mutableSetOf<String>()
        val tiles = mutableListOf<LocalItem>()

        val candidateSongs = database.mostPlayedSongsByArtist(artist.id, fromTimeStamp, toTimeStamp)
            .first().shuffled()
        for (song in candidateSongs) {
            if (tiles.size >= 3) break
            val albumId = song.album?.id ?: continue
            if (albumId in usedAlbumIds) continue
            val album = database.album(albumId).first() ?: continue
            tiles += album
            usedAlbumIds += albumId
        }

        if (tiles.size < 3) {
            val moreAlbums = database.artistCreditedAlbumsPreview(artist.id, previewSize = 20).first().shuffled()
            for (album in moreAlbums) {
                if (tiles.size >= 3) break
                if (album.id in usedAlbumIds) continue
                tiles += album
                usedAlbumIds += album.id
            }
        }

        return if (tiles.size < 3) null else ForYouShelfItem(artist, tiles)
    }

    // Pool is the whole library (listened + followed + everything else with local plays),
    // not just the top 25 — so the "On repeat for you" carousel has real material to page
    // through. Once loadMoreForYouShelves() exhausts the pool it wraps back to the start
    // (reshuffled, new album tiles) so scrolling forward never hits a hard stop.
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
        while (shelves.size < 20 && forYouPoolCursor < forYouArtistPool.size) {
            val artist = forYouArtistPool[forYouPoolCursor]
            forYouPoolCursor++
            buildForYouShelf(artist, fromTimeStamp, toTimeStamp)?.let { shelves += it }
        }
        forYouShelves.value = shelves
        HomeCache.forYouShelves = shelves
    }

    fun loadMoreForYouShelves() {
        if (_isLoadingMoreForYou.value || forYouArtistPool.isEmpty()) return
        viewModelScope.launch(Dispatchers.IO) {
            _isLoadingMoreForYou.value = true
            val fromTimeStamp = System.currentTimeMillis() - 86400000L * 30
            val toTimeStamp = System.currentTimeMillis()
            val newShelves = mutableListOf<ForYouShelfItem>()
            var scanned = 0
            // Scan up to one full lap of the pool looking for 10 more valid shelves; wrap the
            // cursor (reshuffling) once exhausted so the carousel keeps producing content.
            while (newShelves.size < 10 && scanned < forYouArtistPool.size) {
                if (forYouPoolCursor >= forYouArtistPool.size) {
                    forYouArtistPool = forYouArtistPool.shuffled()
                    forYouPoolCursor = 0
                }
                val artist = forYouArtistPool[forYouPoolCursor]
                forYouPoolCursor++
                scanned++
                buildForYouShelf(artist, fromTimeStamp, toTimeStamp)?.let { newShelves += it }
            }
            if (newShelves.isNotEmpty()) {
                val combined = forYouShelves.value + newShelves
                forYouShelves.value = combined
                HomeCache.forYouShelves = combined
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
        val hideVideoSongs = context.dataStore.get(HideVideoSongsKey, false)
        val likedSongs = database.likedSongsByCreateDateAsc().first()
        if (likedSongs.isEmpty()) return

        val seeds = likedSongs.shuffled().distinctBy { it.id }.take(5)
        
        // Use a synchronized list to collect results safely from concurrent coroutines
        val items = java.util.Collections.synchronizedList(mutableListOf<DailyDiscoverItem>())

        kotlinx.coroutines.coroutineScope {
            seeds.map { seed ->
                launch(Dispatchers.IO) {
                    val endpoint = YouTube.next(WatchEndpoint(videoId = seed.id)).getOrNull()?.relatedEndpoint
                    if (endpoint != null) {
                        YouTube.related(endpoint).onSuccess { page ->
                            val recommendations = page.songs
                                .filter { item ->
                                    if (hideVideoSongs && item.isVideoSong) return@filter false
                                    if (item.explicit) return@filter false
                                    true
                                }
                                .shuffled()

                            // Simple check to avoid immediate duplicate of seed
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
                            }
                        }
                    }
                }
            }.forEach { it.join() }
        }
        
        // Final deduplication just in case multiple seeds recommended the same song
        dailyDiscover.value = items.toList().distinctBy { it.recommendation.id }.shuffled()
    }

    private suspend fun getQuickPicks() {
        val hideVideoSongs = context.dataStore.get(HideVideoSongsKey, false)
        when (quickPicksEnum.first()) {
            QuickPicks.QUICK_PICKS -> {
                val relatedSongs = database.quickPicks().first().filterVideoSongs(hideVideoSongs)
                val forgotten = database.forgottenFavorites().first().filterVideoSongs(hideVideoSongs).take(8)
                quickPicks.value = (relatedSongs + forgotten)
                    .distinctBy { it.id }
                    .shuffled()
                    .take(20)
                    .ifEmpty { relatedSongs.shuffled().take(20) }
            }
            QuickPicks.LAST_LISTEN -> {
                val song = database.events().first().firstOrNull()?.song
                if (song != null && database.hasRelatedSongs(song.id)) {
                    quickPicks.value = database.getRelatedSongs(song.id).first().filterVideoSongs(hideVideoSongs).shuffled().take(20)
                }
            }
        }
    }

    private suspend fun enrichQuickPicksFromNetwork() {
        if (quickPicksEnum.first() != QuickPicks.QUICK_PICKS) return
        val hideVideoSongs = context.dataStore.get(HideVideoSongsKey, false)
        val recentSong = database.events().first().firstOrNull()?.song ?: return
        val endpoint = YouTube.next(WatchEndpoint(videoId = recentSong.id)).getOrNull()?.relatedEndpoint ?: return
        val ytSimilarSongs = mutableListOf<Song>()
        YouTube.related(endpoint).onSuccess { page ->
            page.songs.take(10).forEach { ytSong ->
                if (hideVideoSongs && ytSong.isVideoSong) return@forEach
                val localSong = database.song(ytSong.id).first()
                if (localSong != null) {
                    ytSimilarSongs.add(localSong)
                } else {
                    ytSimilarSongs.add(
                        Song(
                            song = SongEntity(
                                id = ytSong.id,
                                title = ytSong.title,
                                thumbnailUrl = ytSong.thumbnail,
                                explicit = ytSong.explicit,
                                isVideo = ytSong.isVideoSong,
                                duration = ytSong.duration ?: -1
                            ),
                            artists = ytSong.artists.map { artist ->
                                ArtistEntity(
                                    id = artist.id ?: ArtistEntity.generateArtistId(),
                                    name = artist.name
                                )
                            }
                        )
                    )
                }
            }
        }
        if (ytSimilarSongs.isNotEmpty()) {
            quickPicks.value = (quickPicks.value.orEmpty() + ytSimilarSongs).distinctBy { it.id }.shuffled().take(20)
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
                            // Use song count from the playlist page if available, otherwise use original
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

    private suspend fun getSimilarRecommendations() {
        val hideExplicit = context.dataStore.get(HideExplicitKey, false)
        val hideVideoSongs = context.dataStore.get(HideVideoSongsKey, false)
        val fromTimeStamp = System.currentTimeMillis() - 86400000L * 7 * 2

        val artistRecommendations = database.mostPlayedArtists(fromTimeStamp, limit = 15).first()
            .filter { it.artist.isYouTubeArtist }
            .shuffled().take(4)
            .mapNotNull {
                val items = mutableListOf<YTItem>()
                YouTube.artist(it.id).onSuccess { page ->
                    page.sections.takeLast(3).forEach { section -> items += section.items }
                }
                SimilarRecommendation(
                    title = it,
                    items = items
                        .distinctBy { item -> item.id }
                        .filterExplicit(hideExplicit)
                        .filterVideoSongs(hideVideoSongs)
                        .shuffled().take(12)
                        .ifEmpty { return@mapNotNull null }
                )
            }

        val songRecommendations = database.mostPlayedSongs(fromTimeStamp, limit = 15).first()
            .filter { it.album != null }
            .shuffled().take(3)
            .mapNotNull { song ->
                val endpoint = YouTube.next(WatchEndpoint(videoId = song.id)).getOrNull()?.relatedEndpoint
                    ?: return@mapNotNull null
                val page = YouTube.related(endpoint).getOrNull() ?: return@mapNotNull null
                SimilarRecommendation(
                    title = song,
                    items = (page.songs.shuffled().take(10) +
                            page.albums.shuffled().take(5) +
                            page.artists.shuffled().take(3) +
                            page.playlists.shuffled().take(3))
                        .distinctBy { it.id }
                        .filterExplicit(hideExplicit)
                        .filterVideoSongs(hideVideoSongs)
                        .shuffled()
                        .ifEmpty { return@mapNotNull null }
                )
            }

        val albumRecommendations = database.mostPlayedAlbums(fromTimeStamp, limit = 10).first()
            .filter { it.album.thumbnailUrl != null }
            .shuffled().take(2)
            .mapNotNull { album ->
                val items = mutableListOf<YTItem>()
                YouTube.album(album.id).onSuccess { page ->
                    page.otherVersions.let { items += it }
                }
                album.artists.firstOrNull()?.id?.let { artistId ->
                    YouTube.artist(artistId).onSuccess { page ->
                        page.sections.lastOrNull()?.items?.let { items += it }
                    }
                }
                SimilarRecommendation(
                    title = album,
                    items = items
                        .distinctBy { item -> item.id }
                        .filterExplicit(hideExplicit)
                        .filterVideoSongs(hideVideoSongs)
                        .shuffled().take(10)
                        .ifEmpty { return@mapNotNull null }
                )
            }

        similarRecommendations.value = (artistRecommendations + songRecommendations + albumRecommendations).shuffled()
        allYtItems.value = similarRecommendations.value?.flatMap { it.items }.orEmpty() +
                homePage.value?.sections?.flatMap { it.items }.orEmpty()
        HomeCache.similarRecommendations = similarRecommendations.value
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

    fun regenerateSimilarRecommendations() {
        viewModelScope.launch(Dispatchers.IO) {
            regenerateSection(
                key = "similar_recommendations",
                currentIds = { similarRecommendations.value?.flatMap { it.items }?.map { it.id }?.toSet().orEmpty() },
                generate = { getSimilarRecommendations() },
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
                    phase1Complete.filter { it }.first()
                    kotlinx.coroutines.delay(1500L)
                    getDailyDiscover()
                    HomeCache.dailyDiscover = dailyDiscover.value
                    phase2DailyDiscoverDone = true
                    checkPhase2Complete()
                }
            }
            sectionId == "from_the_community" && communityPlaylistsLaunchJob == null -> {
                communityPlaylistsLaunchJob = viewModelScope.launch(Dispatchers.IO) {
                    phase1Complete.filter { it }.first()
                    kotlinx.coroutines.delay(1500L)
                    getCommunityPlaylists()
                    HomeCache.communityPlaylists = communityPlaylists.value
                    phase2CommunityDone = true
                    checkPhase2Complete()
                }
            }
            sectionId.startsWith("similar_recommendation_") && similarRecommendationsLaunchJob == null -> {
                similarRecommendationsLaunchJob = viewModelScope.launch(Dispatchers.IO) {
                    phase1Complete.filter { it }.first()
                    kotlinx.coroutines.delay(1500L)
                    getSimilarRecommendations()
                    phase2SimilarDone = true
                    checkPhase2Complete()
                }
            }
            sectionId == "dischi_per_te" && dischiPerTeLaunchJob == null -> {
                dischiPerTeLaunchJob = viewModelScope.launch(Dispatchers.IO) {
                    phase1Complete.filter { it }.first()
                    kotlinx.coroutines.delay(1500L)
                    getDischiPerTe()
                    phase2DischiPerTeDone = true
                    checkPhase2Complete()
                }
            }
        }
    }

    fun loadHomeData() = viewModelScope.launch { load() }

    private suspend fun load() {
        isLoading.value = true
        phase1Complete.value = false
        phase2Complete.value = false
        phase2DailyDiscoverDone = false
        phase2CommunityDone = false
        phase2SimilarDone = false
        phase2DischiPerTeDone = false
        dailyDiscoverLaunchJob?.cancel(); dailyDiscoverLaunchJob = null
        communityPlaylistsLaunchJob?.cancel(); communityPlaylistsLaunchJob = null
        similarRecommendationsLaunchJob?.cancel(); similarRecommendationsLaunchJob = null
        dischiPerTeLaunchJob?.cancel(); dischiPerTeLaunchJob = null
        val hideExplicit = context.dataStore.get(HideExplicitKey, false)
        val hideVideoSongs = context.dataStore.get(HideVideoSongsKey, false)
        val hideYoutubeShorts = context.dataStore.get(HideYoutubeShortsKey, false)
        val fromTimeStamp = System.currentTimeMillis() - 86400000L * 7 * 2

        // Phase 1: DB-only — unblocks UI as fast as possible
        try {
            coroutineScope {
                launch(Dispatchers.IO) { getQuickPicks() }
                launch(Dispatchers.IO) {
                    val songs = database.mostPlayedSongs(fromTimeStamp, limit = 15, offset = 5).first()
                        .filterVideoSongs(hideVideoSongs).shuffled().take(10)
                    val albums = database.mostPlayedAlbums(fromTimeStamp, limit = 8, offset = 2).first()
                        .filter { it.album.thumbnailUrl != null }.shuffled().take(5)
                    val artists = database.mostPlayedArtists(fromTimeStamp).first()
                        .filter { it.artist.isYouTubeArtist && it.artist.thumbnailUrl != null }.shuffled().take(5)
                    keepListening.value = (songs + albums + artists).shuffled()
                }
                launch(Dispatchers.IO) { getForYouShelves() }
            }
            allLocalItems.value = (quickPicks.value.orEmpty() + keepListening.value.orEmpty())
                .filter { it is Song || it is Album }
            HomeCache.quickPicks = quickPicks.value
            HomeCache.keepListening = keepListening.value
            HomeCache.lastLoadedAt = System.currentTimeMillis()
            context.dataStore.edit { it[HomeCacheLastLoadedKey] = HomeCache.lastLoadedAt }
            isPhase1Complete.value = true
        } finally {
            isLoading.value = false
            phase1Complete.value = true
        }

        // Mood: parte subito con cached params, senza aspettare YouTube.home()
        val cachedMoodParams = cachedMoodSnapshot.value?.chipParams
        val cachedMoodTitle = cachedMoodSnapshot.value?.chipTitle
        if (!cachedMoodParams.isNullOrEmpty()) {
            viewModelScope.launch(Dispatchers.IO) {
                loadMoodPage(cachedMoodParams, cachedMoodTitle, hideExplicit, hideVideoSongs, hideYoutubeShorts)
            }
        }

        // Phase 2a: DB secondario — nessuna rete, parte subito
        viewModelScope.launch(Dispatchers.IO) {
            getForgottenFavorites()
            HomeCache.forgottenFavorites = forgottenFavorites.value
        }

        // Phase 2b: Rete — parte subito, in parallelo col Mood, così non arriva
        // in coda dietro un ritardo artificiale
        viewModelScope.launch(Dispatchers.IO) {
            YouTube.explore().onSuccess { page ->
                explorePage.value = page.copy(newReleaseAlbums = page.newReleaseAlbums.filterExplicit(hideExplicit))
                HomeCache.explorePage = explorePage.value
                refreshHeroCarousel()
            }.onFailure { reportException(it) }
        }

        viewModelScope.launch(Dispatchers.IO) {
            var homeResult = YouTube.home()
            if (homeResult.isFailure) {
                if (YouTube.cookie != null) {
                    val recovered = syncUtils.reInjectCredentials()
                    if (recovered) homeResult = YouTube.home()
                } else {
                    // Anonymous: stale/expired visitorData can cause failures on reopen.
                    // Fetch fresh visitor data and retry once.
                    YouTube.visitorData().getOrNull()?.let { fresh ->
                        YouTube.visitorData = fresh
                        context.dataStore.edit { it[VisitorDataKey] = fresh }
                    }
                    homeResult = YouTube.home()
                }
            }
            // Anonymous users: if the response succeeded but returned no sections,
            // also try refreshing visitor data once (stale session can return empty content).
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
                homePage.value = transformedPage

                // Cold start fallback: fresh installs have no local history, so Quick Picks
                // would otherwise stay empty forever. Seed it from the home feed and let it
                // get replaced by real listening data over time.
                if (quickPicks.value.isNullOrEmpty()) {
                    val homeSongs = transformedPage.sections.flatMap { it.items }
                        .filterIsInstance<SongItem>()
                        .distinctBy { it.id }
                        .shuffled()
                        .take(20)
                    if (homeSongs.isNotEmpty()) {
                        quickPicks.value = homeSongs.map { ytSong ->
                            Song(
                                song = SongEntity(
                                    id = ytSong.id,
                                    title = ytSong.title,
                                    thumbnailUrl = ytSong.thumbnail,
                                    explicit = ytSong.explicit,
                                    isVideo = ytSong.isVideoSong,
                                    duration = ytSong.duration ?: -1
                                ),
                                artists = ytSong.artists.map { artist ->
                                    ArtistEntity(id = artist.id ?: ArtistEntity.generateArtistId(), name = artist.name)
                                }
                            )
                        }
                        HomeCache.quickPicks = quickPicks.value
                    }
                }

                if (selectedChip.value == null) {
                    val savedParams = cachedMoodSnapshot.value?.chipParams
                    val preferredChip = if (!savedParams.isNullOrEmpty())
                        transformedChips?.firstOrNull { it.endpoint?.params == savedParams }
                    else null
                    (preferredChip ?: transformedChips?.firstOrNull())?.let { chip -> toggleChip(chip) }
                }
                val savedParams = cachedMoodSnapshot.value?.chipParams
                val moodChip = if (!savedParams.isNullOrEmpty())
                    transformedChips?.firstOrNull { it.endpoint?.params == savedParams } ?: transformedChips?.firstOrNull()
                else transformedChips?.firstOrNull()
                moodChip?.let { chip ->
                    loadMoodPage(chip.endpoint?.params, chip.title, hideExplicit, hideVideoSongs, hideYoutubeShorts)
                }
                HomeCache.homePage = homePage.value
                refreshHeroCarousel()
            }.onFailure { reportException(it) }
        }

        if (YouTube.cookie != null) {
            viewModelScope.launch(Dispatchers.IO) {
                loadAccountPlaylists()
            }
        }

        // Phase 3: Heavy — scaglionato, ben lontano dal caricamento immagini
        viewModelScope.launch(Dispatchers.IO) {
            kotlinx.coroutines.delay(4500)
            enrichQuickPicksFromNetwork()
            HomeCache.quickPicks = quickPicks.value
        }
        // getDailyDiscover, getCommunityPlaylists, getSimilarRecommendations are now
        // lazy — started via onSectionBecameVisible() when the user scrolls to them.
    }

    private val _isLoadingMore = MutableStateFlow(false)
    fun loadMoreYouTubeItems(continuation: String?) {
        if (continuation == null || _isLoadingMore.value) return
        val hideExplicit = context.dataStore.get(HideExplicitKey, false)
        val hideVideoSongs = context.dataStore.get(HideVideoSongsKey, false)
        val hideYoutubeShorts = context.dataStore.get(HideYoutubeShortsKey, false)
        val currentChip = selectedChip.value

        viewModelScope.launch(Dispatchers.IO) {
            _isLoadingMore.value = true
            val nextSections = try {
                withTimeout(30_000L) { YouTube.home(continuation).getOrNull() }
            } catch (e: TimeoutCancellationException) {
                null
            } ?: run {
                _isLoadingMore.value = false
                return@launch
            }

            homePage.value = nextSections.copy(
                chips = homePage.value?.chips,
                sections = (homePage.value?.sections.orEmpty() + nextSections.sections).mapNotNull { section ->
                    val filteredItems = section.items
                        .filterExplicit(hideExplicit)
                        .filterVideoSongs(hideVideoSongs)
                        .filterYoutubeShorts(hideYoutubeShorts)
                    if (filteredItems.isEmpty()) null else section.copy(items = filteredItems)
                }
            )
            _isLoadingMore.value = false
        }
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

            // Fetch podcast-specific data when podcasts chip is selected
            if (chip.title.contains("Podcast", ignoreCase = true)) {
                fetchPodcastData()
            }
        }
    }

    private suspend fun fetchPodcastData() {
        // Fetch saved podcast shows from official API
        YouTube.savedPodcastShows().onSuccess { shows ->
            savedPodcastShows.value = shows
        }.onFailure {
            reportException(it)
        }

        // Fetch episodes for later from official API
        YouTube.episodesForLater().onSuccess { episodes ->
            episodesForLater.value = episodes
        }.onFailure {
            reportException(it)
        }
    }

    private suspend fun loadAccountPlaylists() {
        val hideYoutubeShorts = context.dataStore.get(HideYoutubeShortsKey, false)
        YouTube.library("FEmusic_liked_playlists").completed().onSuccess {
            accountPlaylists.value = it.items.filterIsInstance<PlaylistItem>()
                .filterNot { it.id == "LM" || it.id == "SE" }
                .filterNot { playlist ->
                    playlist.songCountText?.let { text -> Regex("""\d+""").find(text)?.value?.toIntOrNull() } == 0
                }
                .filterYoutubeShorts(hideYoutubeShorts)
        }.onFailure {
            reportException(it)
        }
    }

    fun refresh() {
        if (isRefreshing.value) return
        isRefreshing.value = true
        randomSeed.value = System.currentTimeMillis()
        viewModelScope.launch(Dispatchers.IO) {
            // If a chip is selected, reload the chip's content instead of the default home
            val currentChip = selectedChip.value
            if (currentChip != null) {
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
            refreshHeroCarousel(System.currentTimeMillis(), force = true)
            isRefreshing.value = false
        }
        // Run sync when user manually refreshes
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
        // New releases from followed artists are checked here too (throttled inside the notifier),
        // not just on Library>Artists — Home is the screen actually opened every launch, so this is
        // what makes the "+N" badge show up without a separate visit to the Artists tab.
        viewModelScope.launch(Dispatchers.IO) {
            val followedIds = database.artistsBookmarked(ArtistSortType.CREATE_DATE, true)
                .first()
                .filter { it.artist.isYouTubeArtist && !it.artist.isPodcastChannel }
                .map { it.id }
            newReleaseNotifier.refresh(followedIds)
        }

        // Read snapshots once from DataStore for fast first paint
        viewModelScope.launch(Dispatchers.IO) {
            val prefs = context.dataStore.data.first()
            prefs[SpeedDialSnapshotKey]?.let { json ->
                runCatching { snapshotJson.decodeFromString<SpeedDialSnapshot>(json) }
                    .getOrNull()?.let { cachedSpeedDialSnapshot.value = it }
            }
            prefs[MoodSnapshotKey]?.let { json ->
                runCatching { snapshotJson.decodeFromString<MoodSnapshot>(json) }
                    .getOrNull()?.let { snapshot ->
                        cachedMoodSnapshot.value = snapshot
                        moodMixItems.value = snapshot.items.mapNotNull { it.toPlaylistItem() }
                            .takeIf { it.isNotEmpty() }
                    }
            }
            prefs[AccountNameKey]?.takeIf { it.isNotEmpty() }?.let { accountName.value = it }
            prefs[AccountPhotoUrlKey]?.takeIf { it.isNotEmpty() }?.let { accountImageUrl.value = it }
        }

        // Save speed dial snapshot when live items change
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

        // Load home data
        viewModelScope.launch(Dispatchers.IO) {
            if (HomeCache.lastLoadedAt == 0L) {
                HomeCache.lastLoadedAt = context.dataStore.get(HomeCacheLastLoadedKey, 0L)
            }
            if (!HomeCache.isStale() && HomeCache.homePage != null) {
                homePage.value = HomeCache.homePage
                quickPicks.value = HomeCache.quickPicks
                keepListening.value = HomeCache.keepListening
                forgottenFavorites.value = HomeCache.forgottenFavorites
                explorePage.value = HomeCache.explorePage
                similarRecommendations.value = HomeCache.similarRecommendations
                dailyDiscover.value = HomeCache.dailyDiscover
                communityPlaylists.value = HomeCache.communityPlaylists
                dischiPerTe.value = HomeCache.dischiPerTe
                forYouShelves.value = HomeCache.forYouShelves.orEmpty()
                refreshHeroCarousel()
                isPhase1Complete.value = true
                phase1Complete.value = true
                phase2Complete.value = true
                phase2DailyDiscoverDone = true
                phase2CommunityDone = true
                phase2SimilarDone = true
                phase2DischiPerTeDone = true
                isLoading.value = false
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

        // Off the cold-start critical path (delayed like tryAutoSync above) — cheap no-op most
        // launches since it's due-checked against a 7-day cooldown in DataStore.
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


        // Listen for HideYoutubeShorts preference changes and reload account playlists instantly
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
    }
}
