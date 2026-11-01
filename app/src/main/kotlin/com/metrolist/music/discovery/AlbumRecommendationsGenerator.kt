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
            .filter { it.album.thumbnailUrl != null && it.album.songCount > 1 }
        val recentAlbumIds = recentAlbums.map { it.id }.toSet()

        val staleAlbums = database.mostPlayedAlbums(fromTimeStamp = staleFrom, toTimeStamp = staleTo, limit = 15).first()
            .filter { it.album.thumbnailUrl != null && it.album.songCount > 1 && it.id !in recentAlbumIds }

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
                    ?.filterNot { it.isSingleRelease() }
                    ?.maxByOrNull { it.year ?: 0 }
                    ?.let { album -> album to relatedArtist.title }
            }
        }.distinctBy { it.first.id }.filter { !it.first.explicit || !hideExplicit }

        val unheardKnownArtistAlbums = knownArtists.shuffled(random).take(6).mapNotNull { artist ->
            YouTube.artist(artist.id).getOrNull()
                ?.sections?.flatMap { it.items }
                ?.filterIsInstance<AlbumItem>()
                ?.filterNot { it.id in allKnownAlbumIds || it.isSingleRelease() }
                ?.shuffled(random)?.firstOrNull()
                ?.let { album -> album to artist.title }
        }.distinctBy { it.first.id }.filter { !it.first.explicit || !hideExplicit }

        val distantNewReleases = explorePage?.newReleaseAlbums
            ?.distinctBy { it.id }
            ?.filter { it.artists?.firstOrNull()?.id !in knownArtistIds }
            ?.filterNot { it.isSingleRelease() }
            ?.filterExplicit(hideExplicit)
            .orEmpty()

        val rawCounts = categoryPercents.map { pct -> targetSize * pct / 100 }.toMutableList()
        val overshoot = rawCounts.sum() - targetSize
        rawCounts[2] -= overshoot

        val result = mutableListOf<DischiPerTeItem>()
        result += recentAlbums.shuffled(random).take(rawCounts[0]).map { DischiPerTeItem.Local(it) }
        result += staleAlbums.shuffled(random).take(rawCounts[1]).map { DischiPerTeItem.Local(it) }
        result += similarArtistAlbums.shuffled(random).take(rawCounts[2])
            .map { (album, artistName) -> DischiPerTeItem.Remote(album, fallbackArtistName = artistName) }
        result += distantNewReleases.shuffled(random).take(rawCounts[3]).map { DischiPerTeItem.Remote(it) }
        result += unheardKnownArtistAlbums.shuffled(random).take(rawCounts[4])
            .map { (album, artistName) -> DischiPerTeItem.Remote(album, fallbackArtistName = artistName) }

        return result.distinctBy { it.id }.shuffled(random)
    }

    /**
     * Builds the "Recommended Albums" carousel for the Library Albums screen. Mostly discovery
     * (never-heard albums from known/related artists and recent releases), plus two small local
     * slices pulled from the user's own play history: albums barely touched (a nudge to finish
     * them) and — rarely, at low weight — albums already played a lot (so favorites aren't
     * excluded forever, just not shoved in the user's face). `recentlySuggestedIds` are ids
     * surfaced in a recent regeneration; each bucket prefers albums outside that set and only
     * falls back to them if its fresh pool runs short, so nothing is excluded permanently.
     */
    suspend fun generateForLibrary(
        explorePage: ExplorePage?,
        hideExplicit: Boolean,
        seed: Long,
        excludedAlbumIds: Set<String>,
        recentlySuggestedIds: Set<String> = emptySet(),
    ): List<DischiPerTeItem> {
        val random = Random(seed)
        val currentYear = java.time.Year.now().value
        val targetSize = 20
        val lightlyPlayedCount = 3
        val heavilyPlayedCount = if (random.nextInt(100) < 20) 1 else 0
        val localCount = lightlyPlayedCount + heavilyPlayedCount
        val unknownCount = ((targetSize - localCount) * 5 / 100).coerceAtLeast(1)
        val similarCount = targetSize - localCount - unknownCount

        val knownArtists = database.mostPlayedArtists(fromTimeStamp = 0L, limit = 25).first()
            .filter { it.artist.isYouTubeArtist }
            .filterGenuineFavorites()
        val knownArtistIds = knownArtists.map { it.id }.toSet()

        val playedAlbums = database.mostPlayedAlbums(fromTimeStamp = 0L, limit = 500).first()
        val playedAlbumIds = playedAlbums.map { it.id }.toSet()
        val fullExclusion = excludedAlbumIds + playedAlbumIds

        val lightlyPlayedPool = playedAlbums.filter {
            it.album.songCount > 1 && (it.songCountListened ?: 0) in 1..2 && it.id !in excludedAlbumIds
        }
        val heavilyPlayedPool = playedAlbums.filter {
            it.album.songCount > 1 && (it.songCountListened ?: 0) >= 8 && it.id !in excludedAlbumIds
        }
        val lightlyPlayedSelected = selectPreferringFresh(
            lightlyPlayedPool, lightlyPlayedCount, recentlySuggestedIds, random,
        ) { it.id }
        val heavilyPlayedSelected = selectPreferringFresh(
            heavilyPlayedPool, heavilyPlayedCount, recentlySuggestedIds, random,
        ) { it.id }

        val usedRelatedArtistIds = mutableSetOf<String>()
        val similarArtistAlbums = knownArtists.shuffled(random).take(10).flatMap { artist ->
            val relatedArtists = YouTube.artist(artist.id).getOrNull()
                ?.sections?.flatMap { it.items }
                ?.filterIsInstance<ArtistItem>()
                ?.filterNot { it.id in knownArtistIds }
                ?.distinctBy { it.id }
                ?.shuffled(random)?.take(3)
                .orEmpty()
            relatedArtists.mapNotNull { relatedArtist ->
                val album = YouTube.artist(relatedArtist.id).getOrNull()
                    ?.sections?.flatMap { it.items }
                    ?.filterIsInstance<AlbumItem>()
                    ?.filterNot { it.id in fullExclusion || it.isSingleRelease() }
                    ?.shuffled(random)?.firstOrNull()
                if (album != null) usedRelatedArtistIds += relatedArtist.id
                album?.let { it to relatedArtist.title }
            }
        }.distinctBy { it.first.id }.filter { !it.first.explicit || !hideExplicit }

        val unknownArtistAlbums = explorePage?.newReleaseAlbums
            ?.distinctBy { it.id }
            ?.filter { album ->
                val artistId = album.artists?.firstOrNull()?.id
                album.id !in fullExclusion && artistId !in knownArtistIds && artistId !in usedRelatedArtistIds
            }
            ?.filterNot { it.isSingleRelease() }
            ?.filterExplicit(hideExplicit)
            .orEmpty()

        val similarSelected = selectWithAgeSplitPreferringFresh(
            similarArtistAlbums, similarCount, currentYear, random, recentlySuggestedIds, { it.first.id },
        ) { it.first.year }
        val unknownSelected = selectWithAgeSplitPreferringFresh(
            unknownArtistAlbums, unknownCount, currentYear, random, recentlySuggestedIds, { it.id },
        ) { it.year }

        val result = mutableListOf<DischiPerTeItem>()
        result += lightlyPlayedSelected.map { DischiPerTeItem.Local(it) }
        result += heavilyPlayedSelected.map { DischiPerTeItem.Local(it) }
        result += similarSelected.map { (album, artistName) -> DischiPerTeItem.Remote(album, fallbackArtistName = artistName) }
        result += unknownSelected.map { DischiPerTeItem.Remote(it) }

        val shortBy = targetSize - result.size
        if (shortBy > 0) {
            val similarLeftover = similarArtistAlbums.filterNot { pair -> similarSelected.any { it.first.id == pair.first.id } }
            val unknownLeftover = unknownArtistAlbums.filterNot { album -> unknownSelected.any { it.id == album.id } }
            val topUp = (similarLeftover.map { (album, artistName) -> DischiPerTeItem.Remote(album, fallbackArtistName = artistName) } +
                unknownLeftover.map { DischiPerTeItem.Remote(it) }).shuffled(random).take(shortBy)
            result += topUp
        }

        return result.distinctBy { it.id }.shuffled(random)
    }
}

private fun <T> selectWithAgeSplit(
    pool: List<T>,
    count: Int,
    currentYear: Int,
    random: Random,
    yearOf: (T) -> Int?,
): List<T> {
    if (count <= 0 || pool.isEmpty()) return emptyList()
    val (recent, older) = pool.partition { (currentYear - (yearOf(it) ?: 0)) <= 3 }
    val recentCount = count * 90 / 100
    val olderCount = count - recentCount
    val chosenRecent = recent.shuffled(random).take(recentCount)
    val chosenOlder = older.shuffled(random).take(olderCount)
    val chosen = (chosenRecent + chosenOlder).toMutableList()
    if (chosen.size < count) {
        val remaining = (recent + older).filterNot { it in chosen }.shuffled(random)
        chosen += remaining.take(count - chosen.size)
    }
    return chosen
}

private fun <T> selectWithAgeSplitPreferringFresh(
    pool: List<T>,
    count: Int,
    currentYear: Int,
    random: Random,
    recentlySuggestedIds: Set<String>,
    idOf: (T) -> String,
    yearOf: (T) -> Int?,
): List<T> {
    if (count <= 0 || pool.isEmpty()) return emptyList()
    val (fresh, cooled) = pool.partition { idOf(it) !in recentlySuggestedIds }
    val chosen = selectWithAgeSplit(fresh, count, currentYear, random, yearOf).toMutableList()
    if (chosen.size < count) {
        chosen += selectWithAgeSplit(cooled, count - chosen.size, currentYear, random, yearOf)
    }
    return chosen
}

private fun <T> selectPreferringFresh(
    pool: List<T>,
    count: Int,
    recentlySuggestedIds: Set<String>,
    random: Random,
    idOf: (T) -> String,
): List<T> {
    if (count <= 0 || pool.isEmpty()) return emptyList()
    val (fresh, cooled) = pool.partition { idOf(it) !in recentlySuggestedIds }
    val chosen = fresh.shuffled(random).take(count).toMutableList()
    if (chosen.size < count) {
        chosen += cooled.shuffled(random).take(count - chosen.size)
    }
    return chosen
}

private val singleReleaseLabels = setOf("single", "singolo")

private fun AlbumItem.isSingleRelease(): Boolean =
    albumType?.trim()?.lowercase() in singleReleaseLabels
