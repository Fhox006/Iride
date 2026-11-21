/** Metrolist Project (C) 2026 Licensed under GPL-3.0 | See git history for contributors */
package com.metrolist.music.discovery

import android.content.Context
import com.metrolist.innertube.YouTube
import com.metrolist.innertube.models.ArtistItem
import com.metrolist.innertube.models.SongItem
import com.metrolist.innertube.models.WatchEndpoint.WatchEndpointMusicSupportedConfigs.WatchEndpointMusicConfig.Companion.MUSIC_VIDEO_TYPE_OMV
import com.metrolist.innertube.pages.HomePage
import com.metrolist.music.db.MusicDatabase
import com.metrolist.music.models.HeroCarouselItem
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.random.Random

internal const val HERO_CAROUSEL_TARGET_SIZE = 8
private const val HERO_CAROUSEL_CANDIDATE_LIMIT = 20

class HeroCarouselGenerator(
    private val database: MusicDatabase,
    context: Context,
) {
    private val artworkVerifier = MusicVideoArtworkVerifier(context)
    private val starterArtistNames = listOf(
        "Adele",
        "Ariana Grande",
        "Bad Bunny",
        "Beyoncé",
        "Billie Eilish",
        "Bruno Mars",
        "Coldplay",
        "Dua Lipa",
        "Ed Sheeran",
        "Eros Ramazzotti",
        "Lady Gaga",
        "Mahmood",
        "Måneskin",
        "Michael Jackson",
        "Olivia Rodrigo",
        "Queen",
        "Taylor Swift",
        "The Weeknd",
    )

    suspend fun generate(
        homePage: HomePage?,
        seed: Long,
        previousArtistIds: Set<String> = emptySet(),
        previousFirstArtistId: String? = null,
        hideExplicit: Boolean = false,
        hideVideos: Boolean = false,
    ): List<HeroCarouselItem.MusicVideo> = coroutineScope {
        if (hideVideos) return@coroutineScope emptyList()
        val recent = database.mostPlayedArtists(
            fromTimeStamp = System.currentTimeMillis() - 365L * 24 * 60 * 60 * 1000,
            limit = 120,
        ).first()
        val bookmarked = database.artistsBookmarkedByCreateDateAsc().first()
        val library = database.artistsByCreateDateAsc().first()
        val personalSeeds = (recent + bookmarked + library)
            .filter { !it.artist.isPodcastChannel }
            .map { HeroCarouselItem.ArtistRadio(it.id, it.artist.name, it.artist.thumbnailUrl) }
            .distinctBy { it.artistId }
        val feedSeeds = homePage?.sections.orEmpty()
            .flatMap { section -> section.items.filterIsInstance<ArtistItem>() }
            .map { HeroCarouselItem.ArtistRadio(it.id, it.title, it.thumbnail) }
        val initialSeeds = if (personalSeeds.size >= HERO_CAROUSEL_TARGET_SIZE) personalSeeds else personalSeeds + feedSeeds
        val initialSeedCount = initialSeeds.distinctBy { it.artistId }.size
        val starterSeeds = if (initialSeedCount >= HERO_CAROUSEL_TARGET_SIZE) {
            emptyList()
        } else {
            searchStarterArtists(seed, HERO_CAROUSEL_TARGET_SIZE - initialSeedCount)
        }
        val seeds = selectArtistRadios(
            initialSeeds + starterSeeds,
            previousArtistIds,
            seed,
            previousFirstArtistId,
            candidateLimit = HERO_CAROUSEL_CANDIDATE_LIMIT,
        )
        val selected = mutableListOf<HeroCarouselItem.MusicVideo>()
        val covers = mutableListOf<VideoArtworkFingerprint>()
        // Fetch three artists at a time; publish only after the replacement set is complete.
        for (batch in seeds.chunked(3)) {
            if (selected.size >= HERO_CAROUSEL_TARGET_SIZE) break
            val candidates = batch.map { artist ->
                async {
                    try {
                        var fallback: Pair<HeroCarouselItem.MusicVideo, VideoArtworkFingerprint>? = null
                        withTimeoutOrNull(20_000) {
                            val page = YouTube.artist(artist.artistId).getOrNull() ?: return@withTimeoutOrNull null
                            val songs = page.sections.flatMap { it.items }.filterIsInstance<SongItem>()
                                .filter { it.musicVideoType == MUSIC_VIDEO_TYPE_OMV && !it.isEpisode && (!hideExplicit || !it.explicit) }
                                .distinctBy { it.id }
                                .shuffled(Random(seed xor artist.artistId.hashCode().toLong()))
                                .take(5)
                            for (song in songs) {
                                val cover = artworkVerifier.verify(song) ?: continue
                                val candidate = HeroCarouselItem.MusicVideo(
                                    artistId = artist.artistId,
                                    artistName = artist.artistName,
                                    artistThumbnail = artist.coverUrl,
                                    song = song.copy(thumbnail = cover.url),
                                ) to cover.fingerprint
                                if (cover.faceQuality > 0f) return@withTimeoutOrNull candidate
                                if (fallback == null) fallback = candidate
                            }
                            fallback
                        } ?: fallback
                    } catch (cancelled: CancellationException) {
                        throw cancelled
                    } catch (_: Exception) {
                        null
                    }
                }
            }.awaitAll()
            for ((video, fingerprint) in candidates.filterNotNull()) {
                if (selected.size >= HERO_CAROUSEL_TARGET_SIZE) break
                if (selected.any { it.song.id == video.song.id } || covers.any { it.resembles(fingerprint) }) continue
                selected += video
                covers += fingerprint
            }
        }
        selected.take(HERO_CAROUSEL_TARGET_SIZE)
    }

    private suspend fun searchStarterArtists(seed: Long, count: Int): List<HeroCarouselItem.ArtistRadio> = coroutineScope {
        starterArtistNames.shuffled(Random(seed)).take(count.coerceAtMost(8)).map { name ->
            async {
                try {
                    withTimeoutOrNull(8_000L) {
                        YouTube.search(name, YouTube.SearchFilter.FILTER_ARTIST)
                            .getOrNull()
                            ?.items
                            ?.filterIsInstance<ArtistItem>()
                            ?.firstOrNull { it.id.startsWith("UC") && it.title.isNotBlank() }
                            ?.let { HeroCarouselItem.ArtistRadio(it.id, it.title, it.thumbnail) }
                    }
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Exception) {
                    null
                }
            }
        }.awaitAll().filterNotNull()
    }
}
