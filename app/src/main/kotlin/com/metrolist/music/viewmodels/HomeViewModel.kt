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
import com.metrolist.innertube.models.Artist
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
import com.metrolist.music.constants.InnerTubeCookieKey
import com.metrolist.music.constants.LastMoodChipParamsKey
import com.metrolist.music.constants.LastMoodChipTitleKey
import com.metrolist.music.constants.MoodSnapshotKey
import com.metrolist.music.constants.QuickPicks
import com.metrolist.music.constants.QuickPicksKey
import com.metrolist.music.constants.RandomizeHomeOrderKey
import com.metrolist.music.constants.ShowWrappedCardKey
import com.metrolist.music.constants.SpeedDialSnapshotKey
import com.metrolist.music.constants.WrappedSeenKey
import com.metrolist.music.models.HomeSnapshotItem
import com.metrolist.music.models.MoodSnapshot
import com.metrolist.music.models.SpeedDialSnapshot
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import com.metrolist.music.ui.screens.HomeSection
import com.metrolist.music.db.MusicDatabase
import com.metrolist.music.db.entities.Album
import com.metrolist.music.db.entities.LocalItem
import com.metrolist.music.db.entities.Song
import com.metrolist.music.db.entities.SpeedDialItem
import com.metrolist.music.extensions.filterVideoSongs
import com.metrolist.music.extensions.toEnum
import com.metrolist.music.models.SimilarRecommendation
import com.metrolist.music.ui.screens.wrapped.WrappedAudioService
import com.metrolist.music.ui.screens.wrapped.WrappedManager
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
) : ViewModel() {
    val isRefreshing = MutableStateFlow(false)
    val isLoading = MutableStateFlow(false)
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
        .map { it[RandomizeHomeOrderKey] ?: true }
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val moodPage = MutableStateFlow<HomePage?>(null)
    private var lastMoodChipParams: String? = null

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
        val json = runCatching { snapshotJson.encodeToString(snapshot) }.getOrNull() ?: return
        context.dataStore.edit { it[MoodSnapshotKey] = json }
    }

    fun loadMoodPage(params: String?, chipTitle: String? = null, hideExplicit: Boolean, hideVideoSongs: Boolean, hideYoutubeShorts: Boolean) {
        if (params == lastMoodChipParams && moodPage.value != null) return
        lastMoodChipParams = params
        viewModelScope.launch(Dispatchers.IO) {
            if (params != null) {
                YouTube.home(params = params).onSuccess { nextSections ->
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
                    if (chipTitle != null) {
                        context.dataStore.edit { prefs ->
                            prefs[LastMoodChipTitleKey] = chipTitle
                            prefs[LastMoodChipParamsKey] = params
                        }
                        saveMoodSnapshotAfterLoad(chipTitle, params, filteredPage)
                    }
                }
            } else {
                moodPage.value = null
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
            isPhase1Complete
        ) { pinned, keepListening, quick, phase1Done ->
            val pinnedItems = pinned.map { it.toYTItem() }
            if (!phase1Done) return@combine pinnedItems
            val filled = pinnedItems.toMutableList()
            val targetSize = 27
            val kl = keepListening ?: emptyList()
            val qp = quick ?: emptyList()

            if (filled.size < targetSize) {
                val needed = targetSize - filled.size
                val available = kl.filter { item ->
                    filled.none { p -> p.id == item.id }
                }.mapNotNull { item ->
                    when (item) {
                        is Song -> SongItem(
                            id = item.id,
                            title = item.title,
                            artists = item.artists.map { Artist(name = it.name, id = it.id) },
                            thumbnail = item.thumbnailUrl ?: "",
                            explicit = false
                        )
                        is Album -> AlbumItem(
                            browseId = item.id,
                            playlistId = item.album.playlistId ?: "",
                            title = item.title,
                            artists = item.artists.map { Artist(name = it.name, id = it.id) },
                            year = item.album.year,
                            thumbnail = item.thumbnailUrl ?: ""
                        )
                        else -> null
                    }
                }
                filled.addAll(available.take(needed))
            }

            if (filled.size < targetSize) {
                val needed = targetSize - filled.size
                val available = qp.filter { song ->
                    filled.none { p -> p.id == song.id }
                }.map { song ->
                    SongItem(
                        id = song.id,
                        title = song.title,
                        artists = song.artists.map { Artist(name = it.name, id = it.id) },
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

        finalItems.addAll(sortedList.filter { section ->
            !(hasCachedSpeedDial && section == HomeSection.SpeedDial)
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
                        artists = song.artists.map { Artist(name = it.name, id = it.id) },
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
                            artists = item.artists.map { Artist(name = it.name, id = it.id) },
                            thumbnail = item.thumbnailUrl ?: "",
                            explicit = false
                        ))
                        is Album -> otherSources.add(AlbumItem(
                            browseId = item.id,
                            playlistId = item.album.playlistId ?: "",
                            title = item.title,
                            artists = item.artists.map { Artist(name = it.name, id = it.id) },
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
    private var phase2DailyDiscoverDone = false
    private var phase2CommunityDone = false
    private var phase2SimilarDone = false

    private fun checkPhase2Complete() {
        if (phase2DailyDiscoverDone && phase2CommunityDone && phase2SimilarDone) {
            phase2Complete.value = true
        }
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
                database.song(ytSong.id).first()?.let { localSong ->
                    if (!hideVideoSongs || !localSong.song.isVideo) ytSimilarSongs.add(localSong)
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
        dailyDiscoverLaunchJob?.cancel(); dailyDiscoverLaunchJob = null
        communityPlaylistsLaunchJob?.cancel(); communityPlaylistsLaunchJob = null
        similarRecommendationsLaunchJob?.cancel(); similarRecommendationsLaunchJob = null
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
            forgottenFavorites.value = database.forgottenFavorites().first()
                .filterVideoSongs(hideVideoSongs).shuffled().take(20)
            HomeCache.forgottenFavorites = forgottenFavorites.value
        }

        // Phase 2b: Rete — ritardata per lasciare thread IO liberi a Coil
        viewModelScope.launch(Dispatchers.IO) {
            kotlinx.coroutines.delay(2500)
            YouTube.explore().onSuccess { page ->
                explorePage.value = page.copy(newReleaseAlbums = page.newReleaseAlbums.filterExplicit(hideExplicit))
                HomeCache.explorePage = explorePage.value
            }.onFailure { reportException(it) }
        }

        viewModelScope.launch(Dispatchers.IO) {
            kotlinx.coroutines.delay(2500)
            YouTube.home().onSuccess { page ->
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
            }.onFailure { reportException(it) }
        }

        if (YouTube.cookie != null) {
            viewModelScope.launch(Dispatchers.IO) {
                kotlinx.coroutines.delay(2500)
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
                .filterNot { it.id == "SE" }
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
        // Read snapshots once from DataStore for fast first paint
        viewModelScope.launch(Dispatchers.IO) {
            val prefs = context.dataStore.data.first()
            prefs[SpeedDialSnapshotKey]?.let { json ->
                runCatching { snapshotJson.decodeFromString<SpeedDialSnapshot>(json) }
                    .getOrNull()?.let { cachedSpeedDialSnapshot.value = it }
            }
            prefs[MoodSnapshotKey]?.let { json ->
                runCatching { snapshotJson.decodeFromString<MoodSnapshot>(json) }
                    .getOrNull()?.let { cachedMoodSnapshot.value = it }
            }
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
                isPhase1Complete.value = true
                phase1Complete.value = true
                phase2Complete.value = true
                phase2DailyDiscoverDone = true
                phase2CommunityDone = true
                phase2SimilarDone = true
                isLoading.value = false
            } else {
                load()
            }
        }

        viewModelScope.launch(Dispatchers.IO) {
            kotlinx.coroutines.delay(8000)
            syncUtils.tryAutoSync()
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
                    lastProcessedCookie = cookie
                    isProcessingAccountData = true
                    try {
                        if (!cookie.isNullOrEmpty()) {
                            YouTube.cookie = cookie
                            YouTube.accountInfo().onSuccess { info ->
                                accountName.value = info.name
                                accountImageUrl.value = info.thumbnailUrl
                                    ?.replace(Regex("w\\d+-h\\d+(-[a-zA-Z0-9]+)?"), "w256-h256-c")
                                    ?: info.thumbnailUrl
                            }.onFailure { reportException(it) }
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
