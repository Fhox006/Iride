package com.metrolist.music.playback.queues

import androidx.media3.common.MediaItem
import com.metrolist.music.db.MusicDatabase
import kotlinx.coroutines.flow.first
import kotlin.math.ln
import kotlin.random.Random

private const val ROTATION_WINDOW_MILLIS = 30L * 24 * 60 * 60 * 1000
private const val RECENTLY_PLAYED_WINDOW_MILLIS = 24L * 60 * 60 * 1000

/** Orders a source queue using a rolling month of play time and avoids yesterday's played tracks. */
suspend fun MusicDatabase.buildRotationQueue(
    items: List<MediaItem>,
    now: Long = System.currentTimeMillis(),
): List<MediaItem> {
    val uniqueItems = items.distinctBy(MediaItem::mediaId)
    if (uniqueItems.size < 2) return uniqueItems

    val playTimes = rotationSongStats(now - ROTATION_WINDOW_MILLIS)
        .first()
        .associate { it.songId to it.playTime }
    val recentlyPlayed = recentlyPlayedSongIds(now - RECENTLY_PLAYED_WINDOW_MILLIS)
        .first()
        .toSet()
    val eligibleItems = uniqueItems.filterNot { it.mediaId in recentlyPlayed }
        .ifEmpty { uniqueItems }

    return eligibleItems
        .map { item ->
            val playTime = playTimes[item.mediaId] ?: 0L
            val weight = 1.0 + ln(1.0 + playTime / 30_000.0)
            val randomValue = Random.nextDouble().coerceAtLeast(Double.MIN_VALUE)
            item to (-ln(randomValue) / weight)
        }
        .sortedBy { it.second }
        .map { it.first }
}
