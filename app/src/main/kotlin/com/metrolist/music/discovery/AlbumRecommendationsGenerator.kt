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

        // Albums found on an artist's own page never carry `artists` (YouTube
        // doesn't repeat the artist name there), so we pair each album with the
        // artist page it came from and carry that name along as a fallback.
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
     * Builds the "Album consigliati" carousel for the Library Albums screen: unlike [generate],
     * this excludes every album the user has ever played or already saved — it's meant purely as
     * a discovery feed, not a "keep listening" mix. ~95% comes from artists related to ones the
     * user already listens to (the same artist-page expansion [generate] uses), the remaining
     * ~5% is a deliberate "outside your usual taste" pick from global new releases. Both slices
     * respect a 90%-newer-than-3-years / 10%-older split.
     */
    suspend fun generateForLibrary(
        explorePage: ExplorePage?,
        hideExplicit: Boolean,
        seed: Long,
        excludedAlbumIds: Set<String>,
    ): List<DischiPerTeItem> {
        val random = Random(seed)
        val currentYear = java.time.Year.now().value
        val targetSize = 20
        val unknownCount = (targetSize * 5 / 100).coerceAtLeast(1)
        val similarCount = targetSize - unknownCount

        val knownArtists = database.mostPlayedArtists(fromTimeStamp = 0L, limit = 25).first()
            .filter { it.artist.isYouTubeArtist }
            .filterGenuineFavorites()
        val knownArtistIds = knownArtists.map { it.id }.toSet()

        val playedAlbumIds = database.mostPlayedAlbums(fromTimeStamp = 0L, limit = 500).first()
            .map { it.id }.toSet()
        val fullExclusion = excludedAlbumIds + playedAlbumIds

        // Related artists (not already known) discovered off each known artist's page, paired
        // with a random non-single, not-yet-played album off *their* page. Track which related
        // artist ids got used so the "unknown artist" bucket below doesn't double-dip into them.
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

        val similarSelected = selectWithAgeSplit(similarArtistAlbums, similarCount, currentYear, random) { it.first.year }
        val unknownSelected = selectWithAgeSplit(unknownArtistAlbums, unknownCount, currentYear, random) { it.year }

        // If one bucket came up short (e.g. a new user with few known artists), top the list up
        // from whatever the other bucket has left over rather than returning a sparse carousel.
        val result = mutableListOf<DischiPerTeItem>()
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

// Splits `pool` into "recent" (year within 3 of `currentYear`) and "older" partitions and takes a
// 90/10 sample of `count` items, falling back to whichever partition has more when one is short.
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

// YouTube never flags singles structurally — the release type only shows up
// as a localized label ("Single", "Singolo", ...) next to the year. This is a
// best-effort check against the labels we know about; unrecognized labels are
// treated as albums rather than risk hiding real albums.
private val singleReleaseLabels = setOf("single", "singolo")

private fun AlbumItem.isSingleRelease(): Boolean =
    albumType?.trim()?.lowercase() in singleReleaseLabels
