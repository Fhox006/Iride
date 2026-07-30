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
import com.metrolist.music.constants.ArtistNewReleasesCheckedKey
import com.metrolist.music.constants.ArtistNewReleasesKey
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

/**
 * Per-followed-artist state for the Library › Artists new-release badges.
 *
 * @param knownAlbumIds every album/single/EP id we've already accounted for (the baseline).
 * @param newSongIds song ids from releases that appeared *after* the baseline and are still unseen.
 * @param initialized false until the first sync recorded a baseline — before that we don't flag the
 *        whole back-catalog as "new".
 */
@Serializable
data class ArtistReleaseState(
    val knownAlbumIds: Set<String> = emptySet(),
    val newSongIds: List<String> = emptyList(),
    val initialized: Boolean = false,
)

/**
 * Detects new songs from followed artists and exposes a per-artist "+N" count for the library.
 *
 * - [counts] derives the badge live: stored unseen song ids minus any already played (from the
 *   `event` table), so listening to a new song anywhere — radio, search, playlist — drops it from
 *   the count without opening the artist.
 * - [refresh] fetches each followed artist's page, diffs album ids against the stored baseline, and
 *   for each genuinely new release fetches its tracklist to accumulate the new song ids.
 * - [markSeen] clears an artist's unseen list when its profile is opened.
 */
@Singleton
class NewReleaseNotifier @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: MusicDatabase,
) {
    private val json = Json { ignoreUnknownKeys = true }

    // ponytail: SQLite IN() caps at ~999 binds; new-release backlog realistically stays far below,
    // but clamp so a pathological store can never crash the query.
    private val maxReconcileIds = 900

    private fun parse(raw: String?): Map<String, ArtistReleaseState> =
        raw?.let { runCatching { json.decodeFromString<Map<String, ArtistReleaseState>>(it) }.getOrNull() }
            .orEmpty()

    /** artistId -> count of unseen, not-yet-played new songs. */
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

    /** Clears the unseen list for one artist (keeps its baseline). Call when the profile opens. */
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

    /**
     * Refreshes new-release state for the given followed artists. Throttled to once per [windowMs]
     * unless [force]. Drops state for artists no longer followed. Network-bound; run off the main
     * thread.
     */
    suspend fun refresh(
        followedArtistIds: List<String>,
        force: Boolean = false,
        windowMs: Long = 12 * 60 * 60 * 1000L,
    ) {
        if (followedArtistIds.isEmpty()) return
        val now = System.currentTimeMillis()
        val prefsSnapshot = context.dataStore.data.first()
        val lastChecked = prefsSnapshot[ArtistNewReleasesCheckedKey] ?: 0L
        if (!force && now - lastChecked < windowMs) return

        val followed = followedArtistIds.toSet()
        val store = parse(prefsSnapshot[ArtistNewReleasesKey]).toMutableMap()
        // Forget artists that were unfollowed.
        store.keys.retainAll(followed)

        for (artistId in followed) {
            val page = YouTube.artist(artistId).getOrNull() ?: continue
            // Only the artist's own releases — Album/Single/EP shelves, never "Featured on"/playlists.
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
                // First time we see this artist: record baseline, flag nothing.
                store[artistId] = state.copy(knownAlbumIds = currentAlbumIds, initialized = true)
                continue
            }

            val newAlbumIds = currentAlbumIds - state.knownAlbumIds
            if (newAlbumIds.isEmpty()) continue

            val newSongs = newAlbumIds.flatMap { albumId ->
                YouTube.album(albumId).getOrNull()?.songs?.map { it.id }.orEmpty()
            }
            store[artistId] = state.copy(
                knownAlbumIds = state.knownAlbumIds + currentAlbumIds,
                newSongIds = (state.newSongIds + newSongs).distinct(),
            )
        }

        context.dataStore.edit { prefs ->
            prefs[ArtistNewReleasesKey] = json.encodeToString<Map<String, ArtistReleaseState>>(store)
            prefs[ArtistNewReleasesCheckedKey] = now
        }
    }
}
