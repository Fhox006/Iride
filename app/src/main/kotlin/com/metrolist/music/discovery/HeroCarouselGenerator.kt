/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.discovery

import com.metrolist.innertube.YouTube
import com.metrolist.innertube.models.ArtistItem
import com.metrolist.innertube.models.AlbumItem
import com.metrolist.innertube.models.SongItem
import com.metrolist.innertube.pages.HomePage
import com.metrolist.music.db.MusicDatabase
import com.metrolist.music.models.HeroCarouselItem
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.ConcurrentHashMap

class HeroCarouselGenerator(
    private val database: MusicDatabase,
) {
    private val resolvedCovers = ConcurrentHashMap<String, String>()

    suspend fun generate(
        homePage: HomePage?,
        seed: Long,
        previousArtistIds: Set<String> = emptySet(),
        previousFirstArtistId: String? = null,
    ): List<HeroCarouselItem.ArtistRadio> {
        val recentArtists = database.mostPlayedArtists(
            fromTimeStamp = System.currentTimeMillis() - 365L * 24 * 60 * 60 * 1000,
            limit = 120,
        ).first()
            .filter { it.id.startsWith("UC") && !it.artist.isPodcastChannel }
            .map { artist ->
                HeroCarouselItem.ArtistRadio(
                    artistId = artist.id,
                    artistName = artist.artist.name,
                    coverUrl = artist.artist.thumbnailUrl,
                )
            }
        val bookmarkedArtists = database.artistsBookmarkedByCreateDateAsc().first()
            .filter { it.id.startsWith("UC") && !it.artist.isPodcastChannel }
            .map { HeroCarouselItem.ArtistRadio(it.id, it.artist.name, it.artist.thumbnailUrl) }
        val feedItems = homePage?.sections.orEmpty()
            .flatMap { it.items }
        val discoveryArtists = feedItems
            .filterIsInstance<ArtistItem>()
            .map { artist ->
                HeroCarouselItem.ArtistRadio(
                    artistId = artist.id,
                    artistName = artist.title,
                    coverUrl = artist.thumbnail,
                )
            }
        val creditedArtists = feedItems.flatMap { item ->
            when (item) {
                is SongItem -> item.artists
                is AlbumItem -> item.artists.orEmpty()
                else -> emptyList()
            }
        }.mapNotNull { artist ->
            artist.id?.takeIf { it.startsWith("UC") }?.let {
                HeroCarouselItem.ArtistRadio(it, artist.name, null)
            }
        }
        return selectArtistRadios(
            (recentArtists + bookmarkedArtists + discoveryArtists + creditedArtists).map { radio ->
                radio.copy(coverUrl = resolvedCovers[radio.artistId] ?: radio.coverUrl)
            },
            previousArtistIds,
            seed,
            previousFirstArtistId,
        )
    }

    suspend fun resolveCover(artistId: String): String? = withTimeoutOrNull(4_000) {
        YouTube.artist(artistId).getOrNull()?.artist?.thumbnail?.takeIf { it.isNotBlank() }
    }?.also { resolvedCovers[artistId] = it }

    suspend fun resolveMissingCovers(
        radios: List<HeroCarouselItem.ArtistRadio>,
    ): List<HeroCarouselItem.ArtistRadio> = coroutineScope {
        val semaphore = Semaphore(3)
        radios.map { radio ->
            async {
                if (!radio.coverUrl.isNullOrBlank()) radio
                else semaphore.withPermit { radio.copy(coverUrl = resolveCover(radio.artistId)) }
            }
        }.awaitAll()
    }
}
