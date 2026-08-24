/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package com.metrolist.music.utils

import android.content.Context
import androidx.datastore.preferences.core.edit
import com.metrolist.innertube.YouTube
import com.metrolist.innertube.models.AlbumItem
import com.metrolist.innertube.models.Artist
import com.metrolist.innertube.models.SongItem
import com.metrolist.music.constants.ArtistNewReleasesCheckedKey
import com.metrolist.music.constants.ArtistNewReleasesKey
import com.metrolist.music.constants.GeniusApiTokenKey
import com.metrolist.music.constants.UnseenSongDotsKey
import com.metrolist.music.data.remote.GeniusFeaturedSong
import com.metrolist.music.data.remote.GeniusRepository
import com.metrolist.music.db.MusicDatabase
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
data class NameId(val name: String, val id: String?)

/** Snapshot of a song where the followed artist appears as a featured (non-primary) credit. */
@Serializable
data class FeaturedSongInfo(
    val songId: String,
    val title: String,
    val thumbnailUrl: String,
    val albumId: String? = null,
    val albumTitle: String? = null,
    val otherArtists: List<NameId> = emptyList(),
    val year: Int? = null,
    val firstSeenMs: Long,
)

/**
 * Per-followed-artist state for the Library › Artists new-release badges and the artist page's
 * Featuring section.
 *
 * @param knownAlbumIds every album/single/EP id we've already accounted for (the baseline).
 * @param newSongIds song ids from releases that appeared *after* the baseline and are still unseen —
 *        drives the library "+N" badge. Fed by both owned releases and newly-discovered features.
 * @param unseenAlbumIds album/single/EP ids from those same releases, cleared one at a time when the
 *        user actually opens that release — unlike [newSongIds] this does NOT clear on [markSeen], so
 *        the per-item marker on the artist page survives opening the artist profile.
 * @param initialized false until the first sync recorded a baseline — before that we don't flag the
 *        whole back-catalog as "new".
 * @param knownFeaturedSongIds song ids already accounted for as features, so a feature isn't
 *        re-flagged "new" on every refresh.
 * @param featuredSongs permanent content list for the artist page's Featuring section — tracks where
 *        this artist is a featured (non-primary) credit, whether on a single or another artist's
 *        album. Capped to bound DataStore size (see [NewReleaseNotifier.maxFeaturedSongsPerArtist]).
 */
@Serializable
data class ArtistReleaseState(
    val knownAlbumIds: Set<String> = emptySet(),
    val newSongIds: List<String> = emptyList(),
    val unseenAlbumIds: Set<String> = emptySet(),
    val initialized: Boolean = false,
    val knownFeaturedSongIds: Set<String> = emptySet(),
    val featuredSongs: List<FeaturedSongInfo> = emptyList(),
    val knownGeniusSongIds: Set<Int> = emptySet(),
)

/**
 * Detects new songs and features from followed artists and exposes a per-artist "+N" count for the
 * library plus the artist page's Featuring section.
 *
 * - [counts] derives the badge live: stored unseen song ids minus any already played (from the
 *   `event` table), so listening to a new song anywhere — radio, search, playlist — drops it from
 *   the count without opening the artist.
 * - [refresh] fetches each followed artist's page, diffs album ids against the stored baseline for
 *   owned releases, and separately scans every shelf for tracks crediting the artist as a featured
 *   (non-primary) artist — whether a standalone single or a track buried in someone else's album.
 * - [markSeen] clears an artist's unseen song count (the library "+N") when its profile is opened.
 * - [markAlbumSeen] clears a single release's "new" marker when the user opens that release.
 * - [markSongSeen] clears one song's dot globally once its row actually scrolls into view — used by
 *   the Featuring section, Top Songs, and album song lists alike, independent of played state.
 */
@Singleton
class NewReleaseNotifier @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: MusicDatabase,
    private val geniusRepository: GeniusRepository,
) {
    private val json = Json { ignoreUnknownKeys = true }

    private val maxReconcileIds = 900

    private val maxFeaturedSongsPerArtist = 200

    private val maxFeatureAlbumFetchesPerArtist = 20

    private val maxFeatureAlbumFetchesBaseline = 25

    private val maxGeniusPagesPerCycle = 2
    private val maxGeniusPagesBaseline = 6
    private val maxGeniusResolveAttemptsPerCycle = 10
    private val maxGeniusResolveAttemptsBaseline = 12

    private fun parse(raw: String?): Map<String, ArtistReleaseState> =
        raw?.let { runCatching { json.decodeFromString<Map<String, ArtistReleaseState>>(it) }.getOrNull() }
            .orEmpty()

    private fun parseIds(raw: String?): Set<String> =
        raw?.let { runCatching { json.decodeFromString<Set<String>>(it) }.getOrNull() }.orEmpty()

    /** artistId -> count of unseen, not-yet-played new songs (owned releases + newly found features). */
    val counts: Flow<Map<String, Int>> = context.dataStore.data
        .map { parse(it[ArtistNewReleasesKey]) }
        .distinctUntilChanged()
        .flatMapLatest { store ->
            val allIds = store.values.flatMap { it.newSongIds }.distinct().take(maxReconcileIds)
            if (allIds.isEmpty()) {
                flowOf(store.mapValues { 0 })
            } else {
                database.playedSongIds(allIds).map { played ->
                    val playedSet = played.toSet()
                    store.mapValues { (_, s) -> s.newSongIds.count { it !in playedSet } }
                }
            }
        }

    /** Unseen album/single/EP ids for one artist — drives the per-item "new" marker on its page. */
    fun unseenAlbumIds(artistId: String): Flow<Set<String>> = context.dataStore.data
        .map { parse(it[ArtistNewReleasesKey])[artistId]?.unseenAlbumIds.orEmpty() }
        .distinctUntilChanged()

    /** Tracks where this artist is a featured (non-primary) credit, newest first. */
    fun featuredSongs(artistId: String): Flow<List<FeaturedSongInfo>> = context.dataStore.data
        .map { parse(it[ArtistNewReleasesKey])[artistId]?.featuredSongs.orEmpty() }
        .distinctUntilChanged()

    /** Global set of song ids whose per-row dot hasn't been cleared yet (Top Songs, Featuring, albums). */
    val unseenSongIds: Flow<Set<String>> = context.dataStore.data
        .map { parseIds(it[UnseenSongDotsKey]) }
        .distinctUntilChanged()

    /** Clears the unseen song count for one artist (keeps its baseline). Call when the profile opens. */
    suspend fun markSeen(artistId: String) {
        context.dataStore.edit { prefs ->
            val store = parse(prefs[ArtistNewReleasesKey])
            val state = store[artistId] ?: return@edit
            if (state.newSongIds.isEmpty()) return@edit
            prefs[ArtistNewReleasesKey] = json.encodeToString(
                store + (artistId to state.copy(newSongIds = emptyList())),
            )
        }
    }

    /** Clears one release's "new" marker. Call when the user opens that specific album/single/EP. */
    suspend fun markAlbumSeen(artistId: String, albumId: String) {
        context.dataStore.edit { prefs ->
            val store = parse(prefs[ArtistNewReleasesKey])
            val state = store[artistId] ?: return@edit
            if (albumId !in state.unseenAlbumIds) return@edit
            prefs[ArtistNewReleasesKey] = json.encodeToString(
                store + (artistId to state.copy(unseenAlbumIds = state.unseenAlbumIds - albumId)),
            )
        }
    }

    /** Clears one song's dot globally. Call when its row actually scrolls into view — not on play. */
    suspend fun markSongSeen(songId: String) {
        context.dataStore.edit { prefs ->
            val ids = parseIds(prefs[UnseenSongDotsKey])
            if (songId !in ids) return@edit
            prefs[UnseenSongDotsKey] = json.encodeToString(ids - songId)
        }
    }

    /** True if `artists` includes `artistId` but not as the first (primary) credit. */
    private fun isFeaturedTrack(artists: List<Artist>, artistId: String): Boolean =
        artists.any { it.id == artistId } && artists.firstOrNull()?.id != artistId

    /**
     * Scans [page] for tracks crediting [artistId] as a featured (non-primary) artist — both bare
     * song shelves and other artists' albums not already known to be owned by [artistId]. Video
     * items are dropped: the Featuring section lists official tracks only, not music videos.
     */
    private suspend fun discoverFeatures(
        page: com.metrolist.innertube.pages.ArtistPage,
        artistId: String,
        currentAlbumIds: Set<String>,
        knownAlbumIds: Set<String>,
        now: Long,
        albumFetchCap: Int,
    ): List<FeaturedSongInfo> {
        val directFeatureSongs = page.sections
            .flatMap { it.items }
            .filterIsInstance<SongItem>()
            .filter { !it.isVideoSong && isFeaturedTrack(it.artists, artistId) }
            .map { song ->
                FeaturedSongInfo(
                    songId = song.id,
                    title = song.title,
                    thumbnailUrl = song.thumbnail,
                    albumId = song.album?.id,
                    albumTitle = song.album?.name,
                    otherArtists = song.artists.filter { it.id != artistId }
                        .map { NameId(it.name, it.id) },
                    firstSeenMs = now,
                )
            }

        val candidateAlbumIds = page.sections
            .flatMap { it.items }
            .filterIsInstance<AlbumItem>()
            .map { it.browseId }
            .filter { it !in currentAlbumIds && it !in knownAlbumIds }
            .distinct()
            .take(albumFetchCap)

        val albumFeatureSongs = candidateAlbumIds.flatMap { albumId ->
            val albumPage = YouTube.album(albumId).getOrNull() ?: return@flatMap emptyList()
            albumPage.songs
                .filter { !it.isVideoSong && isFeaturedTrack(it.artists, artistId) }
                .map { song ->
                    FeaturedSongInfo(
                        songId = song.id,
                        title = song.title,
                        thumbnailUrl = song.thumbnail,
                        albumId = albumId,
                        albumTitle = albumPage.album.title,
                        otherArtists = song.artists.filter { it.id != artistId }
                            .map { NameId(it.name, it.id) },
                        year = albumPage.album.year,
                        firstSeenMs = now,
                    )
                }
        }

        return (directFeatureSongs + albumFeatureSongs).distinctBy { it.songId }
    }

    private fun normalizeTitle(title: String): String = title.lowercase().replace(Regex("""[^a-z0-9]"""), "")

    /**
     * Genius indexes every song an artist is credited on, including "feat." credits YTM's own
     * shelves never surface (see [GeniusRepository]). Genius only gives a title + primary artist
     * name, not a playable YTM id, so each candidate is resolved via a YTM song search — best
     * effort, skipped if nothing close enough turns up. Returns the resolved features plus the
     * full set of Genius song ids considered (resolved or not), so callers can mark them known.
     */
    private suspend fun discoverGeniusFeatures(
        artistName: String,
        artistId: String,
        knownGeniusSongIds: Set<Int>,
        now: Long,
        geniusToken: String,
        maxPages: Int,
        maxResolveAttempts: Int,
    ): Pair<List<FeaturedSongInfo>, Set<Int>> {
        if (geniusToken.isBlank()) return emptyList<FeaturedSongInfo>() to knownGeniusSongIds

        val candidates = geniusRepository.findFeaturedSongs(artistName, geniusToken, maxPages)
            .filter { it.geniusId !in knownGeniusSongIds }
            .take(maxResolveAttempts)
        if (candidates.isEmpty()) return emptyList<FeaturedSongInfo>() to knownGeniusSongIds

        val resolved = candidates.mapNotNull { candidate ->
            val query = "${candidate.primaryArtistName} ${candidate.title}"
            val match = YouTube.search(query, YouTube.SearchFilter.FILTER_SONG).getOrNull()
                ?.items?.filterIsInstance<SongItem>()?.firstOrNull { result ->
                    val normalizedResult = normalizeTitle(result.title)
                    val normalizedCandidate = normalizeTitle(candidate.title)
                    !result.isVideoSong && normalizedCandidate.length >= 3 &&
                        (normalizedResult.contains(normalizedCandidate) || normalizedCandidate.contains(normalizedResult))
                } ?: return@mapNotNull null

            FeaturedSongInfo(
                songId = match.id,
                title = match.title,
                thumbnailUrl = match.thumbnail,
                albumId = match.album?.id,
                albumTitle = match.album?.name,
                otherArtists = match.artists.filter { it.id != artistId }.map { NameId(it.name, it.id) },
                firstSeenMs = now,
            )
        }

        return resolved to (knownGeniusSongIds + candidates.map { it.geniusId })
    }

    /**
     * Refreshes new-release and featuring state for the given followed artists. Throttled to once
     * per [windowMs] unless [force]. Drops state for artists no longer followed. Network-bound; run
     * off the main thread.
     */
    suspend fun refresh(
        followedArtistIds: List<String>,
        force: Boolean = false,
        windowMs: Long = 4 * 60 * 60 * 1000L,
    ) {
        if (followedArtistIds.isEmpty()) return
        val now = System.currentTimeMillis()
        val prefsSnapshot = context.dataStore.data.first()
        val lastChecked = prefsSnapshot[ArtistNewReleasesCheckedKey] ?: 0L
        if (!force && now - lastChecked < windowMs) return

        val followed = followedArtistIds.toSet()
        val store = parse(prefsSnapshot[ArtistNewReleasesKey]).toMutableMap()
        var unseenSongDots = parseIds(prefsSnapshot[UnseenSongDotsKey])
        val geniusToken = prefsSnapshot[GeniusApiTokenKey].orEmpty()
        store.keys.retainAll(followed)

        for (artistId in followed) {
            val page = YouTube.artist(artistId).getOrNull() ?: continue
            val artistName = page.artist.title

            val currentAlbumIds = page.sections
                .filter { section ->
                    val t = section.title
                    t.contains("Album", ignoreCase = true) ||
                        t.contains("Single", ignoreCase = true) ||
                        t.contains("Singol", ignoreCase = true) ||
                        t.contains("EP", ignoreCase = true)
                }
                .flatMap { it.items }
                .filterIsInstance<AlbumItem>()
                .map { it.browseId }
                .toSet()

            val state = store[artistId] ?: ArtistReleaseState()
            if (!state.initialized) {
                val baselineFeatures = discoverFeatures(
                    page, artistId, currentAlbumIds, knownAlbumIds = emptySet(), now,
                    albumFetchCap = maxFeatureAlbumFetchesBaseline,
                )
                val (baselineGeniusFeatures, geniusIdsAfterBaseline) = discoverGeniusFeatures(
                    artistName, artistId, knownGeniusSongIds = emptySet(), now, geniusToken,
                    maxPages = maxGeniusPagesBaseline, maxResolveAttempts = maxGeniusResolveAttemptsBaseline,
                )
                val allBaselineFeatures = (baselineFeatures + baselineGeniusFeatures).distinctBy { it.songId }
                store[artistId] = state.copy(
                    knownAlbumIds = currentAlbumIds,
                    initialized = true,
                    knownFeaturedSongIds = allBaselineFeatures.map { it.songId }.toSet(),
                    featuredSongs = allBaselineFeatures
                        .sortedByDescending { it.firstSeenMs }
                        .take(maxFeaturedSongsPerArtist),
                    knownGeniusSongIds = geniusIdsAfterBaseline,
                )
                continue
            }

            val newAlbumIds = currentAlbumIds - state.knownAlbumIds
            val newOwnedSongs = newAlbumIds.flatMap { albumId ->
                YouTube.album(albumId).getOrNull()?.songs?.map { it.id }.orEmpty()
            }

            val discoveredFeatures = discoverFeatures(
                page, artistId, currentAlbumIds, knownAlbumIds = state.knownAlbumIds, now,
                albumFetchCap = maxFeatureAlbumFetchesPerArtist,
            )
            val (geniusFeatures, geniusIdsAfterPass) = discoverGeniusFeatures(
                artistName, artistId, knownGeniusSongIds = state.knownGeniusSongIds, now, geniusToken,
                maxPages = maxGeniusPagesPerCycle, maxResolveAttempts = maxGeniusResolveAttemptsPerCycle,
            )
            val newFeatures = (discoveredFeatures + geniusFeatures)
                .distinctBy { it.songId }
                .filter { it.songId !in state.knownFeaturedSongIds }

            if (newAlbumIds.isEmpty() && newFeatures.isEmpty() && geniusIdsAfterPass == state.knownGeniusSongIds) continue

            val mergedFeaturedSongs = (state.featuredSongs + newFeatures)
                .distinctBy { it.songId }
                .sortedByDescending { it.firstSeenMs }
                .take(maxFeaturedSongsPerArtist)

            val newSongIdsThisPass = newOwnedSongs + newFeatures.map { it.songId }
            unseenSongDots = unseenSongDots + newSongIdsThisPass

            store[artistId] = state.copy(
                knownAlbumIds = state.knownAlbumIds + currentAlbumIds,
                newSongIds = (state.newSongIds + newSongIdsThisPass).distinct(),
                unseenAlbumIds = state.unseenAlbumIds + newAlbumIds,
                knownFeaturedSongIds = state.knownFeaturedSongIds + newFeatures.map { it.songId },
                featuredSongs = mergedFeaturedSongs,
                knownGeniusSongIds = geniusIdsAfterPass,
            )
        }

        context.dataStore.edit { prefs ->
            prefs[ArtistNewReleasesKey] = json.encodeToString<Map<String, ArtistReleaseState>>(store)
            prefs[ArtistNewReleasesCheckedKey] = now
            prefs[UnseenSongDotsKey] = json.encodeToString(unseenSongDots)
        }
    }
}
