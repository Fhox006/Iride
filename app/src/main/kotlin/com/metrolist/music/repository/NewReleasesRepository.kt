package com.metrolist.music.repository

import android.content.Context
import android.util.AtomicFile
import com.metrolist.innertube.YouTube
import com.metrolist.innertube.models.AlbumItem
import com.metrolist.innertube.models.ArtistItem
import com.metrolist.innertube.models.PlaylistItem
import com.metrolist.innertube.models.SongItem
import com.metrolist.innertube.models.YTItem
import com.metrolist.innertube.pages.HomePage
import com.metrolist.music.constants.HideExplicitKey
import com.metrolist.music.constants.ContentCountryKey
import com.metrolist.music.constants.LastMoodChipTitleKey
import com.metrolist.music.data.remote.AppleMusicRequestThrottle
import com.metrolist.music.data.remote.ChartMarket
import com.metrolist.music.db.MusicDatabase
import com.metrolist.music.discovery.MixMood
import com.metrolist.music.discovery.NewRelease
import com.metrolist.music.discovery.NewReleaseCatalog
import com.metrolist.music.discovery.NewReleaseCatalog.objects
import com.metrolist.music.discovery.NewReleaseCatalog.text
import com.metrolist.music.discovery.NewReleasePage
import com.metrolist.music.discovery.NewReleaseProfile
import com.metrolist.music.discovery.NewReleaseSelection
import com.metrolist.music.discovery.NewReleaseSchedule
import com.metrolist.music.utils.dataStore
import com.metrolist.music.utils.get
import com.metrolist.music.utils.HomeFeedSnapshotStore
import com.metrolist.music.viewmodels.HomeCache
import dagger.hilt.android.qualifiers.ApplicationContext
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.URLBuilder
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import timber.log.Timber
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
private data class ArtistReleaseCache(
    val name: String,
    val appleId: String? = null,
    val checkedAt: Long = 0,
    val releases: List<NewRelease> = emptyList(),
    val genre: String? = null,
    val originalDates: Map<String, String> = emptyMap(),
    val nextAttemptAt: Long = 0,
    val failures: Int = 0,
    val editionComparisonVersion: Int = 0,
    val tracklistScanVersion: Int = 0,
    val reissuedTrackIds: Set<String> = emptySet(),
)

@Serializable
private data class ReleaseCache(
    val country: String,
    val checkedAt: Long = 0,
    val discovery: List<NewRelease> = emptyList(),
    val trendingIds: Set<String> = emptySet(),
    val artists: Map<String, ArtistReleaseCache> = emptyMap(),
    val artistIds: Map<String, String> = emptyMap(),
    val localChartArtists: Set<String> = emptySet(),
    val feedCheckedAt: Map<String, Long> = emptyMap(),
    val recommendationsCheckedAt: Long = 0,
    val playlists: List<PlaylistItem> = emptyList(),
    val recommendedArtists: List<ArtistItem> = emptyList(),
    val newAlbums: List<AlbumItem> = emptyList(),
    val albumsCheckedAt: Long = 0,
)

@Serializable
private data class CachedReleaseTarget(val release: NewRelease, val target: YTItem, val checkedAt: Long)

@Singleton
class NewReleasesRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: MusicDatabase,
    private val charts: DailyChartRepository,
) {
    private val json = Json { ignoreUnknownKeys = true }
    private val directory = File(context.filesDir, "new_releases")
    private val client = HttpClient(OkHttp) {
        install(HttpTimeout) {
            requestTimeoutMillis = 12_000
            connectTimeoutMillis = 5_000
            socketTimeoutMillis = 12_000
        }
    }
    private val refreshLock = Mutex()
    private val cacheLock = Mutex()
    private val background = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val jobLock = Mutex()
    private var backgroundRefresh: Job? = null
    private var backgroundFollowedOnly = false
    private var backgroundForced = false
    private var retryJob: Job? = null
    private val unavailableReleases = ConcurrentHashMap<String, Long>()
    private var protectedArtists = emptySet<String>()
    private val resolveLocks = Array(32) { Mutex() }
    private val network = Semaphore(2)
    private val mutablePage = MutableStateFlow(NewReleasePage())
    val page = mutablePage.asStateFlow()
    private val mutableRefreshing = MutableStateFlow(false)
    val refreshing = mutableRefreshing.asStateFlow()
    private val mutableFailed = MutableStateFlow(false)
    val refreshFailed = mutableFailed.asStateFlow()
    private val mutableProgress = MutableStateFlow(0 to 0)
    val followedProgress = mutableProgress.asStateFlow()
    private var cache: ReleaseCache? = null
    private var profile = NewReleaseProfile()
    private var followed = emptyMap<String, String>()
    private var followedNames = emptyMap<String, String>()
    private var hideExplicit = false
    private var visit = System.nanoTime()
    private val resolved = ConcurrentHashMap<String, CachedReleaseTarget>()

    /** Incremental publication keeps the first screen usable during large followed-artist scans. */
    suspend fun refresh(force: Boolean = false, followedOnly: Boolean = false): Unit {
        while (true) {
            val (job, satisfiesRequest) = jobLock.withLock {
                val running = backgroundRefresh?.takeIf { it.isActive }
                if (running != null) {
                    running to ((followedOnly || !backgroundFollowedOnly) && (!force || backgroundForced))
                } else {
                    backgroundFollowedOnly = followedOnly
                    backgroundForced = force
                    val started = background.launch { refreshContent(force, followedOnly) }
                    backgroundRefresh = started
                    started to true
                }
            }
            // A followed-only scan must not swallow a request to refresh the entire releases page.
            job.join()
            if (satisfiesRequest) return
        }
    }

    private suspend fun refreshContent(force: Boolean, followedOnly: Boolean): Unit = withContext(Dispatchers.IO) {
        if (!refreshLock.tryLock()) return@withContext
        mutableRefreshing.value = true
        mutableFailed.value = false
        try {
            val country = storefront()
            val now = System.currentTimeMillis()
            val preferences = context.dataStore.data.first()
            hideExplicit = preferences[HideExplicitKey] ?: false
            cacheLock.withLock {
                restoreCache(country, includeRecommendations = !followedOnly)
                publish()
            }
            val favorites = database.artistsBookmarkedByNameAsc().first()
                .filterNot { it.artist.isPodcastChannel }.distinctBy { it.id }
            followed = favorites.associate { NewReleaseCatalog.normalize(it.artist.name) to it.id }
            followedNames = favorites.associate { NewReleaseCatalog.normalize(it.artist.name) to it.artist.name }
            if (followedOnly) {
                cacheLock.withLock { publish() }
                val pending = favorites.sortedBy { cache!!.artists[NewReleaseCatalog.normalize(it.artist.name)]?.checkedAt ?: 0L }
                    .map { it.artist.name }
                mutableProgress.value = 0 to pending.size
                scanArtists(pending, country, force, prioritizeFollowed = false)
                return@withContext
            }
            val recentArtists = database.mostPlayedArtists(now - 90 * DAY, limit = 12, toTimeStamp = now).first()
                .filterNot { it.artist.isPodcastChannel }
            val liked = database.likedSongsByPlayTimeAsc().first().takeLast(32)
            val listening = database.mostPlayedSongs(now - 90 * DAY, limit = 40, toTimeStamp = now).first()
            val tags = read<Map<String, List<String>>>(File(context.filesDir, "genre_cache.json")).orEmpty()
            val categories = database.mostPlayedCategories(now - 90 * DAY, limit = 8, toTimeStamp = now).first()
            val artistWeights = mutableMapOf<String, Double>()
            recentArtists.forEachIndexed { index, artist ->
                artistWeights[NewReleaseCatalog.normalize(artist.artist.name)] = (12 - index).toDouble()
            }
            liked.forEach { song -> song.artists.forEach { artist ->
                val name = NewReleaseCatalog.normalize(artist.name)
                artistWeights[name] = (artistWeights[name] ?: 0.0) + 0.5
            } }
            val genreWeights = mutableMapOf<String, Double>()
            listening.forEachIndexed { index, song -> tags[song.id].orEmpty().forEach { tag ->
                val family = NewReleaseCatalog.genreFamily(tag)
                genreWeights[family] = (genreWeights[family] ?: 0.0) + (40 - index) / 40.0
            } }
            categories.forEach { category ->
                val family = NewReleaseCatalog.genreFamily(category.name)
                if (family.isNotBlank()) genreWeights[family] = (genreWeights[family] ?: 0.0) + 2.0
            }
            profile = NewReleaseProfile(artistWeights, genreWeights, MixMood.fromChip(preferences[LastMoodChipTitleKey]))
            publish()

            val pending = favorites.sortedBy { cache!!.artists[NewReleaseCatalog.normalize(it.artist.name)]?.checkedAt ?: 0L }
                .map { it.artist.name }
            mutableProgress.value = 0 to pending.size
            supervisorScope {
                val newAlbums = async { refreshNewAlbums(country, force) }
                val recommendations = async {
                    refreshRecommendations(country, force, recentArtists.map { it.artist.name } + pending)
                }
                val feeds = async { refreshDiscovery(country, force) }
                val chartLoad = async {
                    supervisorScope {
                        listOf(ChartMarket(country), ChartMarket.Global).map { market -> async {
                            attempt { charts.refresh(market) }
                        } }.forEach { it.await() }
                    }
                    val local = charts.charts.value[country]?.chart?.tracks.orEmpty().flatMap { it.artists }
                    cacheLock.withLock {
                        cache = cache!!.copy(localChartArtists = local.map(NewReleaseCatalog::normalize).toSet())
                        publish()
                        save()
                    }
                    local
                }
                scanArtists(pending.take(8), country, force, prioritizeFollowed = false)
                val localNames = chartLoad.await()
                val globalNames = charts.charts.value["global"]?.chart?.tracks.orEmpty().flatMap { it.artists }
                val listeningNames = recentArtists.map { it.artist.name } + liked.flatMap { song -> song.artists.map { it.name } }
                protectedArtists = (pending + localNames + globalNames + listeningNames).map(NewReleaseCatalog::normalize).toSet()
                // Complete starred artists first. Chart catalogs never delay their daily scan.
                scanArtists(pending.drop(8), country, force, prioritizeFollowed = false)
                scanArtists((localNames + listeningNames + globalNames).distinctBy(NewReleaseCatalog::normalize), country, force)
                feeds.await()
                recommendations.await()
                newAlbums.await()
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            Timber.w(error, "New releases refresh failed")
            mutableFailed.value = true
        } finally {
            mutableRefreshing.value = false
            refreshLock.unlock()
            if (mutableFailed.value) scheduleRetry(followedOnly)
        }
    }

    private fun scheduleRetry(followedOnly: Boolean): Unit {
        retryJob?.cancel()
        val next = cache?.artists?.values?.filter { it.failures > 0 }?.minOfOrNull { it.nextAttemptAt }
        val wait = (next?.minus(System.currentTimeMillis()) ?: 30_000L).coerceIn(30_000L, 5 * 60_000L)
        retryJob = background.launch {
            delay(wait)
            refresh(followedOnly = followedOnly)
        }
    }

    private fun restoreCache(country: String, includeRecommendations: Boolean = true): Unit {
        if (cache?.country == country) return
        cache = read<ReleaseCache>(File(directory, "$country.json"))
            ?.takeIf { it.country == country } ?: ReleaseCache(country)
        if (!includeRecommendations) return
        val home = HomeCache.homePage ?: HomeFeedSnapshotStore.load(context)?.page
        val items = home?.sections.orEmpty().flatMap { it.items }
        cache = cache!!.copy(
            playlists = (cache!!.playlists + items.filterIsInstance<PlaylistItem>())
                .filter { !it.isPodcast && !it.thumbnail.isNullOrBlank() }.distinctBy { it.id }.take(36),
            recommendedArtists = (cache!!.recommendedArtists + items.filterIsInstance<ArtistItem>())
                .filter { !it.isProfile && !it.thumbnail.isNullOrBlank() }.distinctBy { it.id }.take(20),
        )
    }

    private suspend fun refreshNewAlbums(country: String, force: Boolean): Unit {
        val snapshot = cache ?: return
        if (!force && snapshot.newAlbums.isNotEmpty() && !NewReleaseSchedule.needsRefresh(snapshot.albumsCheckedAt, 6 * HOUR, System.currentTimeMillis())) return
        val albums = attempt { withTimeout(12_000) { YouTube.newReleaseAlbums().getOrThrow() } }
            ?.filter { !it.thumbnail.isNullOrBlank() }?.distinctBy { it.id }?.take(60).orEmpty()
        if (albums.isEmpty()) return
        cacheLock.withLock {
            if (cache?.country != country) return@withLock
            cache = cache!!.copy(newAlbums = albums, albumsCheckedAt = System.currentTimeMillis())
            publish()
            save()
        }
    }

    private suspend fun storeRecommendations(country: String, home: HomePage, complete: Boolean): Unit = cacheLock.withLock {
        if (cache?.country != country) return@withLock
        val items = home.sections.flatMap { it.items }
        val playlists = items.filterIsInstance<PlaylistItem>().filter { !it.isPodcast && !it.thumbnail.isNullOrBlank() }
        val artists = items.filterIsInstance<ArtistItem>().filter { !it.isProfile && !it.thumbnail.isNullOrBlank() }
        cache = cache!!.copy(
            playlists = (playlists + cache!!.playlists).distinctBy { it.id }.take(36),
            recommendedArtists = (artists + cache!!.recommendedArtists).distinctBy { it.id }.take(20),
            recommendationsCheckedAt = if (complete) System.currentTimeMillis() else cache!!.recommendationsCheckedAt,
        )
        publish()
        save()
    }

    private suspend fun refreshRecommendations(country: String, force: Boolean, artistNames: List<String>): Unit {
        val snapshot = cache ?: return
        if (!force && !NewReleaseSchedule.needsRefresh(snapshot.recommendationsCheckedAt, 6 * HOUR, System.currentTimeMillis())) return
        val home = attempt { withTimeout(18_000) { YouTube.home().getOrThrow() } } ?: return
        storeRecommendations(country, home, complete = false)
        val items = home.sections.flatMap { it.items }.toMutableList()
        var continuation = home.continuation
        for (index in 0 until 2) {
            if (continuation == null || (items.filterIsInstance<PlaylistItem>().distinctBy { it.id }.size >= 24 &&
                    items.filterIsInstance<ArtistItem>().distinctBy { it.id }.size >= 8)) break
            val next = attempt { withTimeout(12_000) { YouTube.home(continuation).getOrThrow() } } ?: break
            items.addAll(next.sections.flatMap { it.items })
            storeRecommendations(country, next, complete = false)
            continuation = next.continuation
        }
        val artists = items.filterIsInstance<ArtistItem>().filter { !it.isProfile && !it.thumbnail.isNullOrBlank() }
            .distinctBy { it.id }.toMutableList()
        if (artists.size < 4) {
            val names = (mutablePage.value.hero.map { it.artistName } +
                cache?.discovery.orEmpty().sortedByDescending { it.releaseDate }.map { it.artistName } + artistNames)
                .distinctBy(NewReleaseCatalog::normalize).take(4 - artists.size)
            val found = supervisorScope { names.map { name -> async {
                attempt { withTimeout(8_000) { YouTube.search(name, YouTube.SearchFilter.FILTER_ARTIST).getOrThrow() } }
                    ?.items?.filterIsInstance<ArtistItem>()?.firstOrNull {
                        !it.isProfile && !it.thumbnail.isNullOrBlank() &&
                            NewReleaseCatalog.normalize(it.title) == NewReleaseCatalog.normalize(name)
                    }
            } }.mapNotNull { it.await() } }
            artists.addAll(found)
        }
        val playlists = items.filterIsInstance<PlaylistItem>().filter { !it.isPodcast && !it.thumbnail.isNullOrBlank() }
            .distinctBy { it.id }.take(36)
        cacheLock.withLock {
            if (cache?.country != country) return@withLock
            cache = cache!!.copy(
                playlists = (playlists + cache!!.playlists).distinctBy { it.id }.take(36),
                recommendedArtists = (artists + cache!!.recommendedArtists).distinctBy { it.id }.take(20),
                recommendationsCheckedAt = System.currentTimeMillis(),
            )
            publish()
            save()
        }
    }

    private suspend fun scanArtists(names: List<String>, country: String, force: Boolean, prioritizeFollowed: Boolean = true): Unit {
        for (chunk in names.distinctBy(NewReleaseCatalog::normalize).chunked(8)) {
            reloadFollowed()
            if (prioritizeFollowed && chunk.any { NewReleaseCatalog.normalize(it) !in followed }) {
                val chunkKeys = chunk.map(NewReleaseCatalog::normalize).toSet()
                val dueFavorites = followedNames.filter { (key, _) ->
                    val saved = cache!!.artists[key]
                    key !in chunkKeys && (saved == null || NewReleaseSchedule.needsRefresh(saved.checkedAt, DAY, System.currentTimeMillis())) &&
                        (saved == null || System.currentTimeMillis() >= saved.nextAttemptAt)
                }.values.toList()
                if (dueFavorites.isNotEmpty()) scanArtists(dueFavorites, country, force, prioritizeFollowed = false)
            }
            val pending = chunk.filter { name ->
                val key = NewReleaseCatalog.normalize(name)
                val saved = cache!!.artists[key]
                val now = System.currentTimeMillis()
                val retryDue = saved == null || now >= saved.nextAttemptAt
                if (saved?.failures?.let { it > 0 } == true) mutableFailed.value = true
                retryDue && (force || saved == null || saved.failures > 0 || saved.editionComparisonVersion < EDITION_COMPARISON_VERSION ||
                    saved.tracklistScanVersion < TRACKLIST_SCAN_VERSION ||
                    NewReleaseSchedule.needsRefresh(saved.checkedAt, if (key in followed) DAY else 6 * HOUR, now))
            }
            val found = mutableMapOf<String, String>()
            for (name in pending) {
                val key = NewReleaseCatalog.normalize(name)
                val id = attempt {
                    cache!!.artists[key]?.appleId ?: cache!!.artistIds[key] ?: artistId(name, country)
                        ?: if (country != "us") artistId(name, "us") else null
                }
                if (id != null) found[name] = id else markArtistFailure(name)
            }
            if (found.isNotEmpty()) {
                val ids = found.values.toSet()
                val albumResults = attempt { lookup(ids.joinToString(","), "album", country) }
                val songResults = attempt { lookup(ids.joinToString(","), "song", country) }
                if (albumResults == null || songResults == null) {
                    found.keys.forEach { markArtistFailure(it) }
                } else {
                    val albums = NewReleaseCatalog.lookupGroups(albumResults, ids)
                    val songs = NewReleaseCatalog.lookupGroups(songResults, ids)
                    for ((name, appleId) in found) {
                        // A missing wrapper or empty group is retried individually, never marked checked.
                        val result = attempt {
                            val albumGroup = albums[appleId]?.takeIf { group -> group.any { it.text("wrapperType") == "collection" } }
                                ?.let { country to it } ?: lookupInStorefront(appleId, "album", country)
                            val songGroup = songs[appleId]?.takeIf { group -> group.any { it.text("kind") == "song" } }
                                ?.let { country to it } ?: lookupInStorefront(appleId, "song", country)
                            storeArtist(name, appleId, albumGroup, songGroup)
                        }
                        if (result == null) markArtistFailure(name)
                    }
                }
            }
            val checked = followed.keys.count { cache!!.artists[it]?.checkedAt?.let { time ->
                !NewReleaseSchedule.needsRefresh(time, DAY, System.currentTimeMillis()) } == true }
            mutableProgress.value = checked to followed.size
        }
    }

    private suspend fun markArtistFailure(name: String): Unit = cacheLock.withLock {
        val key = NewReleaseCatalog.normalize(name)
        val old = cache!!.artists[key] ?: ArtistReleaseCache(name)
        val failures = old.failures + 1
        val wait = when (failures) { 1 -> 30_000L; 2 -> 120_000L; else -> 300_000L }
        cache = cache!!.copy(artists = cache!!.artists + (key to old.copy(failures = failures,
            nextAttemptAt = System.currentTimeMillis() + wait)))
        mutableFailed.value = true
        save()
    }

    private suspend fun storeArtist(
        name: String,
        appleId: String,
        albums: Pair<String, List<JsonObject>>,
        songs: Pair<String, List<JsonObject>>,
    ): Unit {
        val key = NewReleaseCatalog.normalize(name)
        val catalog = albums.second + songs.second
        val editionRecords = catalog.filter { record ->
            record.text("collectionId") != null &&
                record.text("collectionName")?.let(NewReleaseCatalog::isAlbumEdition) == true &&
                NewReleaseCatalog.date(record.text("releaseDate"), today()) != null
        }.distinctBy { it.text("collectionId") }
        val comparisonTracks = mutableMapOf<String, MutableList<JsonObject>>()
        val reissuedTrackIds = mutableSetOf<String>()
        val tracklists = mutableMapOf<Pair<String, String>, List<JsonObject>>()
        suspend fun tracks(collection: JsonObject, country: String): List<JsonObject> {
            val id = requireNotNull(collection.text("collectionId"))
            return tracklists.getOrPut(country to id) {
                val response = lookup(id, "song", country)
                check(response.any { it.text("wrapperType") == "collection" && it.text("collectionId") == id }) {
                    "Missing album tracklist"
                }
                val entries = response.filter { it.text("wrapperType") == "track" && it.text("collectionId") == id }
                val expected = response.first { it.text("wrapperType") == "collection" }.text("trackCount")?.toIntOrNull()
                check(expected == null || entries.size >= expected) { "Incomplete album tracklist" }
                entries.filter { it.text("kind") == "song" }.also {
                    check(it.isNotEmpty()) { "Missing album tracks" }
                    comparisonTracks.getOrPut(country) { mutableListOf() }.addAll(it)
                }
            }
        }
        // Artist lookups are bounded; fetch each fresh collection when its tracks were cut off.
        for (collection in NewReleaseCatalog.collectionsMissingTracks(albums.second, songs.second, today())) {
            tracks(collection, albums.first)
        }
        for (edition in editionRecords) {
            val country = if (albums.second.any { it.text("collectionId") == edition.text("collectionId") }) albums.first else songs.first
            var originals = NewReleaseCatalog.standardAlbums(edition, catalog)
            if (originals.isEmpty()) {
                val title = NewReleaseCatalog.albumFamilyTitle(edition.text("collectionName").orEmpty())
                val results = apple("search", mapOf("term" to "$name $title", "entity" to "album",
                    "country" to country, "limit" to "200", "media" to "music")).objects("results")
                originals = NewReleaseCatalog.standardAlbums(edition, results)
            }
            if (originals.isEmpty()) continue
            val editionTracks = tracks(edition, country)
            val standardTracks = originals.flatMap { tracks(it, country) }
            reissuedTrackIds += NewReleaseCatalog.reissuedTrackIds(editionTracks, standardTracks)
        }
        val records = catalog + comparisonTracks.values.flatten()
        val collections = records.filter { it.text("wrapperType") == "collection" }
            .mapNotNull { record ->
                val id = record.text("collectionId") ?: return@mapNotNull null
                val title = record.text("collectionName") ?: return@mapNotNull null
                val count = record.text("trackCount")?.toIntOrNull()
                id to (NewReleaseCatalog.kind(title, count) to count)
            }.toMap()
        val releases = (NewReleaseCatalog.parseCatalog(albums.second, albums.first, today()) +
            NewReleaseCatalog.parseCatalog(songs.second, songs.first, today()) +
            comparisonTracks.flatMap { (country, tracks) -> NewReleaseCatalog.parseCatalog(tracks, country, today()) })
            .distinctBy { it.id }.map { release ->
                val collection = collections[release.collectionId].takeIf { release.isTrack }
                release.copy(kind = collection?.first ?: release.kind,
                    trackCount = collection?.second ?: release.trackCount,
                    artistId = followed[key].takeIf { NewReleaseCatalog.normalize(release.artistName) == key },
                    sourceArtistNames = release.sourceArtistNames + name)
            }
        val genre = records.firstOrNull { it.text("wrapperType") == "artist" && it.text("artistId") == appleId }
            ?.text("primaryGenreName")
        cacheLock.withLock {
            val previous = cache!!.artists[key]
            val originalDates = (previous?.originalDates.orEmpty().entries + NewReleaseCatalog.originalDates(records).entries)
                .groupBy { it.key }.mapValues { (_, dates) -> dates.minOf { it.value } }
            val cached = ArtistReleaseCache(name, appleId, System.currentTimeMillis(),
                NewReleaseCatalog.mergeCached(previous?.releases.orEmpty(), releases, today()),
                genre ?: previous?.genre, originalDates,
                editionComparisonVersion = EDITION_COMPARISON_VERSION,
                tracklistScanVersion = TRACKLIST_SCAN_VERSION,
                reissuedTrackIds = previous?.reissuedTrackIds.orEmpty() + reissuedTrackIds)
            cache = cache!!.copy(artists = cache!!.artists + (key to cached), artistIds = cache!!.artistIds + (key to appleId))
            publish()
            save()
        }
    }

    /** Revalidate dates whenever shown, including offline cache, then change discovery ordering. */
    suspend fun onPageOpened(followedOnly: Boolean = false): Unit = withContext(Dispatchers.IO) {
        val preferences = context.dataStore.data.first()
        hideExplicit = preferences[HideExplicitKey] ?: false
        reloadFollowed()
        cacheLock.withLock {
            visit++
            restoreCache(storefront(), includeRecommendations = !followedOnly)
            publish()
        }
    }

    private suspend fun reloadFollowed(): Unit {
        val artists = database.artistsBookmarkedByNameAsc().first().filterNot { it.artist.isPodcastChannel }
        followed = artists.associate { NewReleaseCatalog.normalize(it.artist.name) to it.id }
        followedNames = artists.associate { NewReleaseCatalog.normalize(it.artist.name) to it.artist.name }
    }

    private suspend fun refreshDiscovery(country: String, force: Boolean): Unit {
        val today = today()
        val responses = supervisorScope {
            listOf(country, "us", "gb", "fr", "br", "jp").distinct().flatMap { market ->
                listOf("albums", "songs").map { type -> async {
                    val feedKey = "$market/$type"
                    if (!force && !NewReleaseSchedule.needsRefresh(cache!!.feedCheckedAt[feedKey] ?: 0, 6 * HOUR, System.currentTimeMillis())) return@async null
                    val result = attempt {
                        val root = getJson("https://rss.marketingtools.apple.com/api/v2/$market/music/most-played/100/$type.json")
                        val feed = root["feed"] as? JsonObject ?: error("Missing music feed")
                        require(feed.text("country") == market)
                        val results = feed.objects("results")
                        require(results.isNotEmpty())
                        val releases = results.mapNotNull { NewReleaseCatalog.parse(it, market, today, feed = true) }
                        cacheLock.withLock {
                            val old = cache!!
                            val tracks = type == "songs"
                            val replacedIds = old.discovery.filter { it.country == market && it.isTrack == tracks }
                                .map(NewRelease::id).toSet()
                            cache = old.copy(
                                checkedAt = System.currentTimeMillis(),
                                discovery = NewReleaseCatalog.mergeCached(
                                    old.discovery.filter { it.country == market && it.isTrack == tracks }, releases, today,
                                ) + old.discovery.filterNot { it.country == market && it.isTrack == tracks },
                                trendingIds = if (tracks) (old.trendingIds - replacedIds) + releases.map(NewRelease::id)
                                    else old.trendingIds,
                                artistIds = old.artistIds + results.groupBy { NewReleaseCatalog.normalize(it.text("artistName").orEmpty()) }
                                    .mapNotNull { (name, entries) -> entries.mapNotNull { it.text("artistId") }
                                        .distinct().singleOrNull()?.let { name to it } }.toMap(),
                                feedCheckedAt = old.feedCheckedAt + (feedKey to System.currentTimeMillis()),
                            )
                            publish()
                        }
                        Triple(market, type, releases)
                    }
                    if (result == null) mutableFailed.value = true
                    result
                } }
            }.mapNotNull { it.await() }
        }
        if (responses.isNotEmpty()) cacheLock.withLock { save() }
    }

    private suspend fun artistId(name: String, country: String): String? {
        val cleaned = name.replace(Regex("(?i)\\s*-\\s*topic$"), "").trim()
        suspend fun search(artistName: String): String? {
            val response = apple("search", mapOf("term" to artistName, "entity" to "musicArtist", "country" to country,
                "limit" to "50", "media" to "music"))
            return response.objects("results").filter { NewReleaseCatalog.normalize(it.text("artistName").orEmpty()) ==
                NewReleaseCatalog.normalize(artistName) }.distinctBy { it.text("artistId") }.singleOrNull()?.text("artistId")
                ?.takeIf { it.matches(Regex("[0-9]+")) }
        }
        search(cleaned)?.let { return it }
        val youtubeId = followed[NewReleaseCatalog.normalize(name)]?.takeIf { it.startsWith("UC") } ?: return null
        val officialName = withTimeout(12_000) { YouTube.artist(youtubeId).getOrThrow().artist.title }
        return if (NewReleaseCatalog.normalize(officialName) != NewReleaseCatalog.normalize(cleaned)) search(officialName) else null
    }

    private suspend fun lookup(artistId: String, entity: String, country: String): List<JsonObject> = apple("lookup",
        mapOf("id" to artistId, "entity" to entity, "country" to country, "limit" to "200", "sort" to "recent"))
        .objects("results")

    private suspend fun lookupInStorefront(artistId: String, entity: String, country: String): Pair<String, List<JsonObject>> {
        val local = lookup(artistId, entity, country)
        val hasCatalog = local.any { it.text("wrapperType") == "collection" || it.text("kind") == "song" }
        return if (country != "us" && !hasCatalog) "us" to lookup(artistId, entity, "us") else country to local
    }

    private suspend fun apple(path: String, parameters: Map<String, String>): JsonObject {
        AppleMusicRequestThrottle.awaitTurn()
        return getJson(URLBuilder("https://itunes.apple.com/$path").apply {
            parameters.forEach { (key, value) -> this.parameters.append(key, value) }
        }.buildString())
    }

    private suspend fun getJson(url: String): JsonObject = network.withPermit {
        val response = client.get(url)
        check(response.status.value == 200) { "Music catalog HTTP ${response.status.value}" }
        json.parseToJsonElement(response.bodyAsText()) as? JsonObject ?: error("Invalid music catalog")
    }

    private fun publish(): Unit {
        val snapshot = cache ?: return
        val artists = snapshot.artists.values.flatMap { cached -> cached.releases.map { release ->
            val match = followed.entries.firstOrNull { (name, _) ->
                name == NewReleaseCatalog.normalize(release.artistName) || name == NewReleaseCatalog.normalize(cached.name) ||
                    release.artistName.split(Regex("\\s*(?:,|&)\\s*")).any { NewReleaseCatalog.normalize(it) == name }
            }
            release.copy(followed = match != null,
                artistId = match?.value.takeIf { match?.key == NewReleaseCatalog.normalize(release.artistName) })
        } }
        val knownOriginals = snapshot.artists.values.flatMap { it.originalDates.entries }.groupBy { it.key }
            .mapValues { (_, dates) -> dates.minOf { it.value } }
        val reissuedTrackIds = snapshot.artists.values.flatMap { it.reissuedTrackIds }.toMutableSet()
        // Apply cache migration to feed copies too, until their artist's tracklists have been compared.
        snapshot.artists.values.filter { it.editionComparisonVersion < EDITION_COMPARISON_VERSION }.forEach { cached ->
            reissuedTrackIds += cached.releases.filter { release ->
                release.isTrack && release.collectionTitle?.let(NewReleaseCatalog::isAlbumEdition) == true
            }.map { it.id }
        }
        val all = (artists + snapshot.discovery.map { release ->
            val match = followed[NewReleaseCatalog.normalize(release.artistName)]
            release.copy(followed = match != null, artistId = match)
        }).filter { release ->
            val original = knownOriginals[NewReleaseCatalog.recordingKey(release)]
            release.id !in reissuedTrackIds && (original == null || NewReleaseCatalog.date(original, today()) != null) &&
                (unavailableReleases[release.id] ?: 0L) <= System.currentTimeMillis()
        }.map { release ->
            if (release.isTrack || release.kind == com.metrolist.music.discovery.NewReleaseKind.SINGLE) release else {
                val saved = resolved[release.id]
                if (saved != null && !stale(saved.checkedAt, 14 * DAY, System.currentTimeMillis()) && matches(release, saved.target) &&
                    (!hideExplicit || !saved.target.explicit)) {
                    resolved[release.id] = saved
                    release.copy(playable = true, trackCount = saved.release.trackCount ?: release.trackCount)
                } else release.copy(playable = false)
            }
        }
        val inferredGenres = profile.genreWeights.toMutableMap()
        snapshot.artists.values.forEach { artist ->
            val weight = profile.artistWeights[NewReleaseCatalog.normalize(artist.name)] ?: 0.0
            if (weight > 0) {
                val family = NewReleaseCatalog.genreFamily(artist.genre ?: artist.releases.firstOrNull()?.genre)
                if (family.isNotBlank()) inferredGenres[family] = (inferredGenres[family] ?: 0.0) + weight / 10.0
            }
        }
        val selected = NewReleaseSelection.page(all, snapshot.trendingIds,
            profile.copy(genreWeights = inferredGenres, localChartArtists = snapshot.localChartArtists), snapshot.country, hideExplicit, today(), visit,
            System.currentTimeMillis(), snapshot.checkedAt).copy(
                playlists = snapshot.playlists.filter { !hideExplicit || !it.explicit },
                artists = snapshot.recommendedArtists.filter { !hideExplicit || !it.explicit },
            )
        val shown = selected.hero + selected.followedAlbums + selected.albums + selected.sections.flatMap { it.releases }
        val albumDate = Instant.ofEpochMilli(snapshot.albumsCheckedAt)
            .atZone(ZoneId.systemDefault()).toLocalDate()
        val currentAlbums = snapshot.newAlbums.takeIf { albumDate >= NewReleaseSchedule.weekStart(today()) }.orEmpty()
        mutablePage.value = selected.copy(newAlbums = NewReleaseSelection.discoveryAlbums(currentAlbums, shown, hideExplicit))
    }

    private fun targetFile(id: String): File = File(directory, "targets/${id.replace(':', '_')}.json")

    suspend fun resolve(release: NewRelease): YTItem? = withContext(Dispatchers.IO) {
        require(release.id.matches(Regex("apple:(song|album):[0-9]+")))
        if (!NewReleaseCatalog.isFresh(release, today())) return@withContext null
        val lock = resolveLocks[(release.id.hashCode() and Int.MAX_VALUE) % resolveLocks.size]
        lock.withLock {
            val now = System.currentTimeMillis()
            val file = targetFile(release.id)
            val saved = resolved[release.id] ?: read<CachedReleaseTarget>(file)
            val hide = context.dataStore.data.first()[HideExplicitKey] ?: false
            if (hide && release.explicit) return@withLock null
            if (saved != null && !stale(saved.checkedAt, 14 * DAY, now) && (!hide || !saved.target.explicit) && matches(release, saved.target)) {
                resolved[release.id] = saved
                return@withLock saved.target
            }
            try {
                val candidates = withTimeout(18_000) {
                    YouTube.search("${release.artistName} ${release.title}", if (release.isTrack)
                        YouTube.SearchFilter.FILTER_SONG else YouTube.SearchFilter.FILTER_ALBUM)
                        .getOrThrow().items.filter { !hide || !it.explicit }
                }
                val target = if (release.isTrack) NewReleaseCatalog.songMatch(release, candidates.filterIsInstance<SongItem>())
                    else NewReleaseCatalog.albumMatch(release, candidates.filterIsInstance<AlbumItem>())
                if (target != null) {
                    val cached = CachedReleaseTarget(release, target, now)
                    resolved[release.id] = cached
                    write(file, cached)
                    unavailableReleases.remove(release.id)
                } else markReleaseUnavailable(release, 2 * HOUR)
                target
            } catch (timeout: TimeoutCancellationException) {
                markReleaseUnavailable(release, 30_000)
                throw IllegalStateException("Release search timed out", timeout)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                markReleaseUnavailable(release, 30_000)
                throw error
            }
        }
    }

    private suspend fun markReleaseUnavailable(release: NewRelease, wait: Long): Unit {
        unavailableReleases[release.id] = System.currentTimeMillis() + wait
        resolved.remove(release.id)
        cacheLock.withLock { publish() }
        background.launch {
            delay(wait)
            if (!NewReleaseCatalog.isFresh(release, today())) return@launch
            val restored = attempt { resolve(release) }
            if (restored != null) {
                unavailableReleases.remove(release.id)
                cacheLock.withLock { publish() }
            }
        }
    }

    private fun matches(release: NewRelease, item: YTItem): Boolean = when (item) {
        is SongItem -> release.isTrack && NewReleaseCatalog.songMatch(release, listOf(item)) != null
        is AlbumItem -> !release.isTrack && NewReleaseCatalog.albumMatch(release, listOf(item)) != null
        else -> false
    }

    private fun storefront(): String {
        val preferred = context.dataStore.get(ContentCountryKey, "SYSTEM").lowercase(Locale.ROOT)
        if (preferred.matches(Regex("[a-z]{2}"))) return preferred
        val locales = context.resources.configuration.locales
        return (0 until locales.size()).firstNotNullOfOrNull { index -> locales[index].country
            .takeIf { it.matches(Regex("[A-Z]{2}")) } }?.lowercase(Locale.ROOT) ?: "us"
    }

    private fun save(): Unit {
        val snapshot = cache ?: return
        val cutoff = today()
        // Keep follows plus a bounded cache of discovery artists; do not evict favorites.
        val retained = snapshot.artists.entries.sortedByDescending { it.value.checkedAt }
            .filterIndexed { index, entry -> entry.key in followed || entry.key in protectedArtists || index < 120 }.associate { it.toPair() }
            .mapValues { (_, artist) -> artist.copy(releases = artist.releases.filter { NewReleaseCatalog.isFresh(it, cutoff) }) }
        cache = snapshot.copy(artists = retained, discovery = snapshot.discovery.filter { NewReleaseCatalog.isFresh(it, cutoff) })
        write(File(directory, "${snapshot.country}.json"), cache!!)
    }

    private inline fun <reified T> read(file: File): T? = try {
        AtomicFile(file).openRead().bufferedReader().use { json.decodeFromString<T>(it.readText()) }
    } catch (_: Exception) { null }

    private inline fun <reified T> write(file: File, value: T): Unit {
        try {
            file.parentFile?.mkdirs()
            val atomic = AtomicFile(file)
            val stream = atomic.startWrite()
            try {
                stream.write(json.encodeToString(value).toByteArray(Charsets.UTF_8))
                atomic.finishWrite(stream)
            } catch (error: Exception) { atomic.failWrite(stream); throw error }
        } catch (error: Exception) { Timber.w(error, "Could not cache new releases") }
    }

    private suspend fun <T> attempt(block: suspend () -> T): T? = try {
        block()
    } catch (_: TimeoutCancellationException) { null
    } catch (cancelled: CancellationException) { throw cancelled }
    catch (error: Exception) { Timber.w(error, "Music catalog source unavailable"); null }

    private fun today(): LocalDate = LocalDate.now()
    private fun stale(checkedAt: Long, ttl: Long, now: Long): Boolean = checkedAt <= 0 || now < checkedAt || now - checkedAt >= ttl

    companion object {
        private const val EDITION_COMPARISON_VERSION = 1
        private const val TRACKLIST_SCAN_VERSION = 1
        private const val HOUR = 3_600_000L
        private const val DAY = 86_400_000L
    }
}
