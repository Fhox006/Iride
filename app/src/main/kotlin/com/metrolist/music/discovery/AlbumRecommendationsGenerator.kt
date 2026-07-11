/**
 * Iride Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.discovery

import com.metrolist.innertube.YouTube
import com.metrolist.innertube.models.AlbumItem
import com.metrolist.innertube.models.ArtistItem
import com.metrolist.innertube.models.filterExplicit
import com.metrolist.innertube.pages.ExplorePage
import com.metrolist.music.db.MusicDatabase
import com.metrolist.music.extensions.filterGenuineFavorites
import com.metrolist.music.models.DischiPerTeItem
import kotlinx.coroutines.flow.first
import kotlin.random.Random

/**
 * Builds the "Dischi scelti per te" mix: a handful of albums the user already
 * plays, some they haven't touched in a while, a big serving of new albums
 * from artists similar to the ones they like, some globally popular new
 * albums outside their usual taste, and the odd unheard album from an
 * artist they already know.
 */
class AlbumRecommendationsGenerator(
    private val database: MusicDatabase,
) {
    // Target list size and the rough mix of each category within it — the
    // percentages the feature was specced with don't sum to 100 (105), so we
    // round each share and then trim the overshoot off the largest (similar
    // artists) bucket rather than chase an exact split that was never exact.
    private val targetSize = 20
    private val categoryPercents = listOf(10, 15, 50, 20, 10)

    suspend fun generate(
        explorePage: ExplorePage?,
        hideExplicit: Boolean,
        seed: Long,
    ): List<DischiPerTeItem> {
        val random = Random(seed)
        val now = System.currentTimeMillis()
        val recentFrom = now - 14L * 24 * 60 * 60 * 1000
        val staleFrom = now - 365L * 24 * 60 * 60 * 1000
        val staleTo = now - 30L * 24 * 60 * 60 * 1000

        val recentAlbums = database.mostPlayedAlbums(fromTimeStamp = recentFrom, limit = 15).first()
            .filter { it.album.thumbnailUrl != null }
        val recentAlbumIds = recentAlbums.map { it.id }.toSet()

        val staleAlbums = database.mostPlayedAlbums(fromTimeStamp = staleFrom, toTimeStamp = staleTo, limit = 15).first()
            .filter { it.album.thumbnailUrl != null && it.id !in recentAlbumIds }

        val knownArtists = database.mostPlayedArtists(fromTimeStamp = staleFrom, limit = 20).first()
            .filter { it.artist.isYouTubeArtist }
            .filterGenuineFavorites()
        val knownArtistIds = knownArtists.map { it.id }.toSet()

        val allKnownAlbumIds = database.mostPlayedAlbums(fromTimeStamp = 0L, limit = 200).first()
            .map { it.id }.toSet()

        val similarArtistAlbums = knownArtists.shuffled(random).take(5).flatMap { artist ->
            val relatedArtists = YouTube.artist(artist.id).getOrNull()
                ?.sections?.flatMap { it.items }
                ?.filterIsInstance<ArtistItem>()
                ?.filterNot { it.id in knownArtistIds }
                ?.distinctBy { it.id }
                ?.shuffled(random)?.take(2)
                .orEmpty()
            relatedArtists.mapNotNull { relatedArtist ->
                YouTube.artist(relatedArtist.id).getOrNull()
                    ?.sections?.flatMap { it.items }
                    ?.filterIsInstance<AlbumItem>()
                    ?.maxByOrNull { it.year ?: 0 }
            }
        }.distinctBy { it.id }.filterExplicit(hideExplicit)

        val unheardKnownArtistAlbums = knownArtists.shuffled(random).take(6).mapNotNull { artist ->
            YouTube.artist(artist.id).getOrNull()
                ?.sections?.flatMap { it.items }
                ?.filterIsInstance<AlbumItem>()
                ?.filterNot { it.id in allKnownAlbumIds }
                ?.shuffled(random)?.firstOrNull()
        }.distinctBy { it.id }.filterExplicit(hideExplicit)

        val distantNewReleases = explorePage?.newReleaseAlbums
            ?.distinctBy { it.id }
            ?.filter { it.artists?.firstOrNull()?.id !in knownArtistIds }
            ?.filterExplicit(hideExplicit)
            .orEmpty()

        val rawCounts = categoryPercents.map { pct -> targetSize * pct / 100 }.toMutableList()
        val overshoot = rawCounts.sum() - targetSize
        rawCounts[2] -= overshoot

        val result = mutableListOf<DischiPerTeItem>()
        result += recentAlbums.shuffled(random).take(rawCounts[0]).map { DischiPerTeItem.Local(it) }
        result += staleAlbums.shuffled(random).take(rawCounts[1]).map { DischiPerTeItem.Local(it) }
        result += similarArtistAlbums.shuffled(random).take(rawCounts[2]).map { DischiPerTeItem.Remote(it) }
        result += distantNewReleases.shuffled(random).take(rawCounts[3]).map { DischiPerTeItem.Remote(it) }
        result += unheardKnownArtistAlbums.shuffled(random).take(rawCounts[4]).map { DischiPerTeItem.Remote(it) }

        return result.distinctBy { it.id }.shuffled(random)
    }
}
