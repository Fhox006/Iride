/**
 * Iride Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.discovery

import com.metrolist.innertube.YouTube
import com.metrolist.innertube.models.SongItem
import com.metrolist.innertube.models.WatchEndpoint
import com.metrolist.music.db.MusicDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import timber.log.Timber
import java.util.Collections
import kotlin.random.Random

/**
 * Builds the weekly "Discovery Weekly" mix: songs the user hasn't heard yet, picked for
 * compatibility with their taste. There's no audio-feature data (tempo/energy/production) in
 * this app, so the similarity signal is YouTube's own per-song radio/related mix — it already
 * encodes style/mood/tempo/production far better than a hand-rolled heuristic would.
 */
class DiscoveryWeeklyGenerator(
    private val database: MusicDatabase,
) {
    private val targetSize = 30
    private val maxPerArtist = 2
    private val seedCount = 15
    private val perSeedCandidates = 4
    private val seedTimeoutMillis = 12_000L

    private val viralPattern = Regex(
        """(sped up|slowed( down)?|nightcore|8d audio|tiktok|\bmashup\b|\bedit\b)""",
        RegexOption.IGNORE_CASE,
    )
    private val viralToleranceShare = 0.15

    suspend fun generate(
        hideExplicit: Boolean,
        hideVideoSongs: Boolean,
        seed: Long,
    ): List<SongItem> = coroutineScope {
        val likedSongs = database.likedSongsByCreateDateAsc().first()
        val recentlyPlayed = database.mostPlayedSongs(fromTimeStamp = 0L, limit = 60).first()
        val historyPool = (likedSongs + recentlyPlayed).distinctBy { it.id }

        val excludedIds = database.allPlayedSongIds().toSet() + historyPool.map { it.id }.toSet()

        val viralShare = historyPool.count { viralPattern.containsMatchIn(it.title) }
            .toDouble() / historyPool.size
        val toleratesViral = historyPool.isEmpty() || viralShare >= viralToleranceShare

        val candidates = Collections.synchronizedList(mutableListOf<SongItem>())
        Timber.tag("DiscoveryWeekly").i("generate(): historyPool=%d excluded=%d", historyPool.size, excludedIds.size)

        var homePool = emptyList<SongItem>()
        var homeFetched = false
        suspend fun homeSongs(): List<SongItem> {
            if (!homeFetched) {
                homePool = fetchHomeSongs()
                homeFetched = true
                Timber.tag("DiscoveryWeekly").i("generate(): homePool=%d", homePool.size)
            }
            return homePool
        }

        val historySeedIds = historyPool.shuffled().take(seedCount).map { it.id }
        collectCandidates(historySeedIds, excludedIds, hideExplicit, hideVideoSongs, toleratesViral, candidates)
        Timber.tag("DiscoveryWeekly").i("generate(): candidates after history seeds=%d", candidates.size)

        if (candidates.size < targetSize) {
            val triedSeedIds = historySeedIds.toSet()
            collectCandidates(
                homeSongs().map { it.id }.filterNot { it in triedSeedIds }.shuffled().take(seedCount),
                excludedIds + candidates.map { it.id }.toSet(),
                hideExplicit,
                hideVideoSongs,
                toleratesViral,
                candidates,
            )
            Timber.tag("DiscoveryWeekly").i("generate(): candidates after home seeds=%d", candidates.size)
        }

        if (candidates.size < targetSize) {
            val candidateIdSet = candidates.map { it.id }.toSet()
            val filled = homeSongs()
                .filter { it.id !in excludedIds && it.id !in candidateIdSet }
                .filter { !hideExplicit || !it.explicit }
                .filter { !hideVideoSongs || !it.isVideoSong }
                .filter { toleratesViral || !viralPattern.containsMatchIn(it.title) }
                .take(targetSize - candidates.size)
            candidates.addAll(filled)
            Timber.tag("DiscoveryWeekly").i("generate(): filled=%d from home", filled.size)
        }

        val random = Random(seed)
        val perArtistCount = mutableMapOf<String, Int>()
        val result = candidates
            .distinctBy { it.id }
            .shuffled(random)
            .filter { song ->
                val artistKey = song.artists.firstOrNull()?.id ?: song.artists.firstOrNull()?.name ?: song.id
                val count = perArtistCount.getOrDefault(artistKey, 0)
                (count < maxPerArtist).also { keep -> if (keep) perArtistCount[artistKey] = count + 1 }
            }
            .take(targetSize)
        Timber.tag("DiscoveryWeekly").i("generate(): final=%d", result.size)
        result
    }

    private suspend fun fetchHomeSongs(): List<SongItem> =
        YouTube.home().getOrNull()
            ?.sections.orEmpty()
            .flatMap { it.items }
            .filterIsInstance<SongItem>()
            .distinctBy { it.id }

    private suspend fun collectCandidates(
        seedIds: List<String>,
        excludedIds: Set<String>,
        hideExplicit: Boolean,
        hideVideoSongs: Boolean,
        toleratesViral: Boolean,
        candidates: MutableList<SongItem>,
    ) = coroutineScope {
        seedIds.map { videoId ->
            launch(Dispatchers.IO) {
                val nextResult = withTimeoutOrNull(seedTimeoutMillis) {
                    YouTube.next(WatchEndpoint(videoId = videoId))
                }
                val endpoint = nextResult?.getOrNull()?.relatedEndpoint
                    ?: run {
                        Timber.tag("DiscoveryWeekly").w("next() failed/timeout for seed %s", videoId)
                        return@launch
                    }
                val relatedResult = withTimeoutOrNull(seedTimeoutMillis) {
                    YouTube.related(endpoint)
                }
                val related = relatedResult?.getOrNull()?.songs
                    ?: run {
                        Timber.tag("DiscoveryWeekly").w("related() failed/timeout for seed %s", videoId)
                        return@launch
                    }
                related
                    .filter { it.id !in excludedIds }
                    .filter { !hideExplicit || !it.explicit }
                    .filter { !hideVideoSongs || !it.isVideoSong }
                    .filter { toleratesViral || !viralPattern.containsMatchIn(it.title) }
                    .shuffled()
                    .take(perSeedCandidates)
                    .let { candidates.addAll(it) }
            }
        }.forEach { it.join() }
    }
}
