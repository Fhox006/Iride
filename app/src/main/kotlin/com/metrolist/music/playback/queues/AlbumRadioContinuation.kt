package com.metrolist.music.playback.queues

import androidx.media3.common.MediaItem
import com.metrolist.music.extensions.metadata
import com.metrolist.music.models.MediaMetadata
import java.text.Normalizer
import java.util.Locale
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal class AlbumRadioContinuation(albumTracks: List<MediaMetadata>) {
    private val radio = albumTracks.firstOrNull()?.let(YouTubeQueue::radio)
    private val filter = AlbumRecommendationFilter(albumTracks)
    private val mutex = Mutex()
    private var initialized = false

    fun hasNextPage(): Boolean = radio != null && (!initialized || radio.hasNextPage())

    suspend fun nextPage(): List<MediaItem> = mutex.withLock {
        val queue = radio ?: return@withLock emptyList()
        var pagesLoaded = 0
        do {
            if (!hasNextPage()) return@withLock emptyList()
            val items = if (!initialized) {
                queue.getInitialStatus().items.also { initialized = true }
            } else {
                queue.nextPage()
            }
            val recommendations = items.filter { item -> item.metadata?.let(filter::accept) == true }
            if (recommendations.isNotEmpty()) return@withLock recommendations
            pagesLoaded++
            // Skip album-only pages without allowing an unbounded background fetch.
        } while (pagesLoaded < 3 && queue.hasNextPage())
        emptyList()
    }
}

internal class AlbumRecommendationFilter(albumTracks: List<MediaMetadata>) {
    private val seenIds = albumTracks.mapTo(mutableSetOf()) { it.id }
    private val albumKeys = albumTracks.flatMap(::trackKeys).toSet()

    fun accept(track: MediaMetadata): Boolean {
        if (!seenIds.add(track.id)) return false
        return trackKeys(track).none { it in albumKeys }
    }

    private fun trackKeys(track: MediaMetadata): List<Pair<String, String>> {
        val artists = track.artists.map { normalize(it.name).removeSuffix(" topic") }
            .filter { it.isNotEmpty() }
        val normalizedTitle = normalize(track.title.replace(videoLabel, " "))
        val title = artists.fold(normalizedTitle) { title, artist -> title.removePrefix("$artist ") }
            .ifEmpty { normalizedTitle }
        return artists.map { title to it }
    }

    private fun normalize(value: String): String = Normalizer.normalize(value, Normalizer.Form.NFD)
        .replace(accents, "")
        .lowercase(Locale.ROOT)
        .replace(separators, " ")
        .trim()

    private companion object {
        val accents = Regex("\\p{M}+")
        val separators = Regex("[^\\p{L}\\p{N}]+")
        val videoLabel = Regex(
            "[\\[(](?:official\\s+)?(?:music\\s+)?(?:video|audio|lyrics?|visuali[sz]er)" +
                "(?:\\s+(?:video|hd|4k))*[\\])]|" +
                "\\s*[-–—|]\\s*(?:official\\s+)?(?:music\\s+)?(?:video|audio|lyrics?|visuali[sz]er)" +
                "(?:\\s+(?:video|hd|4k))*\\s*$",
            RegexOption.IGNORE_CASE,
        )
    }
}
