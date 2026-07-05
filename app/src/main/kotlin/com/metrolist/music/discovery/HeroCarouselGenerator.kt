/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.discovery

import com.metrolist.innertube.models.PlaylistItem
import com.metrolist.innertube.models.isMixtape
import com.metrolist.innertube.pages.ExplorePage
import com.metrolist.innertube.pages.HomePage
import com.metrolist.music.db.MusicDatabase
import com.metrolist.music.db.entities.PlaylistEntity
import com.metrolist.music.extensions.filterGenuineFavorites
import com.metrolist.music.models.HeroCarouselItem
import com.metrolist.music.models.MoodSnapshot
import kotlinx.coroutines.flow.first
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
        moodSnapshot: MoodSnapshot?,
        seed: Long,
        seenAsFirstIds: Set<String>,
    ): Result {
        val random = Random(seed)
        val oneYearAgo = System.currentTimeMillis() - 365L * 24 * 60 * 60 * 1000

        // Broader pool of artists the user genuinely listens to (not just a handful
        // of plays), used to keep "new releases" tied to their actual taste instead
        // of YouTube's generic global feed.
        val knownArtists = database.mostPlayedArtists(fromTimeStamp = oneYearAgo, limit = 25).first()
            .filter { it.artist.isYouTubeArtist }
            .filterGenuineFavorites()
        val knownArtistIds = knownArtists.map { it.id }.toSet()

        val newReleaseCandidates = explorePage?.newReleaseAlbums
            ?.distinctBy { it.browseId }
            ?.let { albums ->
                if (knownArtistIds.isEmpty()) {
                    // New user with no listening history yet: fall back to the
                    // generic feed rather than showing nothing.
                    albums
                } else {
                    // Only keep albums where a followed artist is the primary
                    // credit, not a mere featuring on someone else's release.
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

        val mixtapes = homePage?.sections
            ?.flatMap { it.items }
            ?.filterIsInstance<PlaylistItem>()
            ?.filter { it.isMixtape }
            ?.distinctBy { it.id }
            ?.shuffled(random)
            ?.take(2)
            ?.map { playlist ->
                HeroCarouselItem.ForYou(
                    playlistId = playlist.id,
                    title = playlist.title,
                    subtitle = playlist.author?.name ?: "Mix",
                    coverUrl = playlist.thumbnail,
                    isLocal = false,
                )
            }.orEmpty()

        val forYouCandidates = mixtapes.ifEmpty {
            val likedCount = database.likedSongsCount().first()
            if (likedCount > 0) {
                listOf(
                    HeroCarouselItem.ForYou(
                        playlistId = PlaylistEntity.LIKED_PLAYLIST_ID,
                        title = "Liked Songs",
                        subtitle = "$likedCount songs",
                        coverUrl = null,
                        isLocal = true,
                    )
                )
            } else emptyList()
        }

        val moodCandidates = moodSnapshot?.items
            ?.firstOrNull { it.type == "playlist" }
            ?.let { snapshotItem ->
                listOf(
                    HeroCarouselItem.Mood(
                        playlistId = snapshotItem.id,
                        moodName = moodSnapshot.chipTitle,
                        coverUrl = snapshotItem.thumbnailUrl,
                    )
                )
            }.orEmpty()

        val topArtists = knownArtists.take(8).shuffled(random)

        val moreFromCandidates = topArtists.take(3).map { artist ->
            HeroCarouselItem.MoreFromArtist(
                artistId = artist.id,
                artistName = artist.artist.name,
                coverUrl = artist.artist.thumbnailUrl,
            )
        }

        val radioCandidates = topArtists.drop(3).take(3)
            .ifEmpty { topArtists.take(3) }
            .map { artist ->
                HeroCarouselItem.ArtistRadio(
                    artistId = artist.id,
                    artistName = artist.artist.name,
                    coverUrl = artist.artist.thumbnailUrl,
                )
            }

        val pools = listOf(newReleaseCandidates, forYouCandidates, moodCandidates, moreFromCandidates, radioCandidates)
            .filter { it.isNotEmpty() }
            .map { it.shuffled(random) }

        if (pools.isEmpty()) return Result(emptyList(), seenAsFirstIds)

        val cursors = IntArray(pools.size)
        val result = mutableListOf<HeroCarouselItem>()
        var lastPoolIndex = -1

        repeat(10) { position ->
            val availableIndices = pools.indices.filter { it != lastPoolIndex }.ifEmpty { pools.indices.toList() }
            val poolIndex = availableIndices.random(random)
            var pool = pools[poolIndex]

            // The album shown as the very first card must rotate: never repeat one
            // already presented in that position.
            if (position == 0) {
                val filtered = pool.filterNot { it is HeroCarouselItem.NewRelease && it.albumId in seenAsFirstIds }
                if (filtered.isNotEmpty()) pool = filtered
            }

            val idx = cursors[poolIndex] % pool.size
            cursors[poolIndex] = cursors[poolIndex] + 1
            result.add(pool[idx])
            lastPoolIndex = poolIndex
        }

        val updatedSeenIds = (result.firstOrNull() as? HeroCarouselItem.NewRelease)
            ?.let { seenAsFirstIds + it.albumId }
            ?: seenAsFirstIds

        return Result(result, updatedSeenIds)
    }
}
