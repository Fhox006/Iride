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

    // Generic viral/edit noise (sped up, nightcore, tiktok edits...) — excluded by default
    // unless the user's own history shows they genuinely listen to this.
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
        if (historyPool.isEmpty()) return@coroutineScope emptyList()

        val excludedIds = database.allPlayedSongIds().toSet() + historyPool.map { it.id }.toSet()

        val viralShare = historyPool.count { viralPattern.containsMatchIn(it.title) }
            .toDouble() / historyPool.size
        val toleratesViral = viralShare >= viralToleranceShare

        val seeds = historyPool.shuffled().take(seedCount)
        val candidates = Collections.synchronizedList(mutableListOf<SongItem>())

        seeds.map { seedSong ->
            launch(Dispatchers.IO) {
                val endpoint = YouTube.next(WatchEndpoint(videoId = seedSong.id)).getOrNull()?.relatedEndpoint
                    ?: return@launch
                val related = YouTube.related(endpoint).getOrNull()?.songs ?: return@launch
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

        val random = Random(seed)
        val perArtistCount = mutableMapOf<String, Int>()
        candidates
            .distinctBy { it.id }
            .shuffled(random)
            .filter { song ->
                val artistKey = song.artists.firstOrNull()?.id ?: song.artists.firstOrNull()?.name ?: song.id
                val count = perArtistCount.getOrDefault(artistKey, 0)
                (count < maxPerArtist).also { keep -> if (keep) perArtistCount[artistKey] = count + 1 }
            }
            .take(targetSize)
    }
}
