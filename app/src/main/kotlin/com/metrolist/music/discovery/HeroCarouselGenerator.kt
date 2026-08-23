/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.discovery

import com.metrolist.innertube.models.ArtistItem
import com.metrolist.innertube.pages.ExplorePage
import com.metrolist.innertube.pages.HomePage
import com.metrolist.music.db.MusicDatabase
import com.metrolist.music.extensions.filterGenuineFavorites
import com.metrolist.music.models.DischiPerTeItem
import com.metrolist.music.models.HeroCarouselItem
import com.metrolist.music.utils.GenreProvider
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlin.random.Random

class HeroCarouselGenerator(
    private val database: MusicDatabase,
) {
    data class Result(
        val items: List<HeroCarouselItem>,
        val seenAsFirstIds: Set<String>,
    )

    suspend fun generate(
        explorePage: ExplorePage?,
        homePage: HomePage?,
        dischiPerTe: List<DischiPerTeItem>,
        seed: Long,
        seenAsFirstIds: Set<String>,
        includeGenrePool: Boolean = true,
    ): Result {
        val random = Random(seed)
        val oneYearAgo = System.currentTimeMillis() - 365L * 24 * 60 * 60 * 1000

        val knownArtists = database.mostPlayedArtists(fromTimeStamp = oneYearAgo, limit = 25).first()
            .filter { it.artist.isYouTubeArtist }
            .filterGenuineFavorites()
        val knownArtistIds = knownArtists.map { it.id }.toSet()

        val newReleaseCandidates = explorePage?.newReleaseAlbums
            ?.distinctBy { it.browseId }
            ?.let { albums ->
                if (knownArtistIds.isEmpty()) {
                    albums
                } else {
                    albums.filter { it.artists?.firstOrNull()?.id in knownArtistIds }
                        .ifEmpty { albums }
                }
            }
            ?.shuffled(random)
            ?.take(3)
            ?.map { album ->
                HeroCarouselItem.NewRelease(
                    albumId = album.browseId,
                    title = album.title,
                    artistName = album.artists?.joinToString(", ") { it.name } ?: "",
                    coverUrl = album.thumbnail,
                )
            }.orEmpty()

        val inRotationCandidates = dischiPerTe.filterIsInstance<DischiPerTeItem.Local>()
            .shuffled(random)
            .take(3)
            .map { local ->
                HeroCarouselItem.InRotation(
                    albumId = local.album.id,
                    title = local.album.title,
                    artistName = local.album.artists.joinToString(", ") { it.name },
                    coverUrl = local.album.thumbnailUrl,
                )
            }

        val recommendedAlbumCandidates = dischiPerTe.filterIsInstance<DischiPerTeItem.Remote>()
            .shuffled(random)
            .take(3)
            .map { remote ->
                HeroCarouselItem.RecommendedAlbum(
                    albumId = remote.item.id,
                    title = remote.item.title,
                    artistName = remote.item.artists?.joinToString(", ") { it.name }
                        ?: remote.fallbackArtistName ?: "",
                    coverUrl = remote.item.thumbnail,
                )
            }

        val trendingArtistCandidates = homePage?.sections
            ?.flatMap { it.items }
            ?.filterIsInstance<ArtistItem>()
            ?.filterNot { it.id in knownArtistIds }
            ?.distinctBy { it.id }
            ?.shuffled(random)
            ?.take(3)
            ?.map { artist ->
                HeroCarouselItem.TrendingArtist(
                    artistId = artist.id,
                    artistName = artist.title,
                    coverUrl = artist.thumbnail,
                )
            }.orEmpty()

        val topArtists = knownArtists.take(8).shuffled(random)
        val radioCandidates = topArtists.take(3).map { artist ->
            HeroCarouselItem.ArtistRadio(
                artistId = artist.id,
                artistName = artist.artist.name,
                coverUrl = artist.artist.thumbnailUrl,
            )
        }

        val genreNewReleaseCandidates =
            if (includeGenrePool) buildGenreNewReleases(explorePage, random) else emptyList()

        val pools = listOf(
            newReleaseCandidates, inRotationCandidates, recommendedAlbumCandidates,
            trendingArtistCandidates, radioCandidates, genreNewReleaseCandidates,
        ).filter { it.isNotEmpty() }.map { it.shuffled(random).toMutableList() }

        if (pools.isEmpty()) return Result(emptyList(), seenAsFirstIds)

        val result = mutableListOf<HeroCarouselItem>()
        var lastPoolIndex = -1

        while (result.size < 10 && pools.any { it.isNotEmpty() }) {
            val availableIndices = pools.indices.filter { pools[it].isNotEmpty() && it != lastPoolIndex }
                .ifEmpty { pools.indices.filter { pools[it].isNotEmpty() } }
            val poolIndex = availableIndices.random(random)
            val pool = pools[poolIndex]

            val pickIndex = if (result.isEmpty()) {
                pool.indexOfFirst { it !is HeroCarouselItem.NewRelease || it.albumId !in seenAsFirstIds }
                    .takeIf { it != -1 } ?: 0
            } else 0

            result.add(pool.removeAt(pickIndex))
            lastPoolIndex = poolIndex
        }

        val updatedSeenIds = (result.firstOrNull() as? HeroCarouselItem.NewRelease)
            ?.let { seenAsFirstIds + it.albumId }
            ?: seenAsFirstIds

        return Result(result, updatedSeenIds)
    }

    private suspend fun buildGenreNewReleases(
        explorePage: ExplorePage?,
        random: Random,
    ): List<HeroCarouselItem.GenreNewRelease> = coroutineScope {
        val albums = explorePage?.newReleaseAlbums?.distinctBy { it.browseId }
            ?.shuffled(random)?.take(15) ?: return@coroutineScope emptyList()
        if (albums.isEmpty()) return@coroutineScope emptyList()

        val semaphore = Semaphore(4)
        val topSongs = database.mostPlayedSongs(fromTimeStamp = 0L, limit = 8).first()
        val favoriteGenre = topSongs.map { song ->
            async {
                semaphore.withPermit {
                    GenreProvider.getGenres(song.song.id, song.song.title, song.artists.firstOrNull()?.name)
                }
            }
        }.awaitAll().flatten()
            .groupingBy { it.lowercase() }
            .eachCount()
            .maxByOrNull { it.value }
            ?.key ?: return@coroutineScope emptyList()

        albums.map { album ->
            async {
                val artistName = album.artists?.joinToString(", ") { it.name } ?: ""
                val matched = semaphore.withPermit {
                    GenreProvider.getGenres(album.browseId, album.title, artistName.ifEmpty { null })
                }.firstOrNull { it.equals(favoriteGenre, ignoreCase = true) }
                matched?.let { Triple(album, artistName, it) }
            }
        }.awaitAll().filterNotNull().take(3).map { (album, artistName, genreLabel) ->
            HeroCarouselItem.GenreNewRelease(
                albumId = album.browseId,
                title = album.title,
                artistName = artistName,
                coverUrl = album.thumbnail,
                genreLabel = genreLabel,
            )
        }
    }
}
