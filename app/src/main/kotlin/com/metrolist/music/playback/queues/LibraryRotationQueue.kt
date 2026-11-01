package com.metrolist.music.playback.queues

import androidx.media3.common.MediaItem
import com.metrolist.music.db.MusicDatabase
import com.metrolist.music.constants.StatPeriod
import kotlinx.coroutines.flow.first
import kotlin.math.ln
import kotlin.random.Random

private const val RECENTLY_PLAYED_WINDOW_MILLIS = 24L * 60 * 60 * 1000

/** Orders a source queue using a rolling month of play time and avoids yesterday's played tracks. */
suspend fun MusicDatabase.buildRotationQueue(
    items: List<MediaItem>,
    now: Long = System.currentTimeMillis(),
): List<MediaItem> {
    val uniqueItems = items.distinctBy(MediaItem::mediaId)
    if (uniqueItems.size < 2) return uniqueItems

    val playTimes = rotationSongStats(StatPeriod.MONTH_1.toTimeMillis())
        .first()
        .associate { it.songId to it.playTime }
    val recentlyPlayed = recentlyPlayedSongIds(now - RECENTLY_PLAYED_WINDOW_MILLIS)
        .first()
        .toSet()
    val eligibleItems = uniqueItems.filterNot { it.mediaId in recentlyPlayed }
        .ifEmpty { uniqueItems }

    val playedThisMonth = eligibleItems.filter { (playTimes[it.mediaId] ?: 0L) > 0L }
    val notPlayedThisMonth = eligibleItems.filterNot { (playTimes[it.mediaId] ?: 0L) > 0L }

    val rankedPlayedItems = playedThisMonth
        .map { item ->
            val playTime = playTimes[item.mediaId] ?: 0L
            val weight = 1.0 + ln(1.0 + playTime / 30_000.0)
            val randomValue = Random.nextDouble().coerceAtLeast(Double.MIN_VALUE)
            item to (-ln(randomValue) / weight)
        }
        .sortedBy { it.second }
        .map { it.first }

    return rankedPlayedItems + notPlayedThisMonth.shuffled()
}

fun <T> weightedRandomOrNull(
    items: List<T>,
    weight: (T) -> Long,
): T? {
    if (items.isEmpty()) return null
    val weights = items.map { weight(it).coerceAtLeast(0L).toDouble() }
    val totalWeight = weights.sum()
    if (totalWeight <= 0.0) return items.randomOrNull()

    var selection = Random.nextDouble(totalWeight)
    items.forEachIndexed { index, item ->
        selection -= weights[index]
        if (selection < 0.0) return item
    }
    return items.last()
}
