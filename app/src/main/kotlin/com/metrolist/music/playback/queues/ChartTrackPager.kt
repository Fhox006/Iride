package com.metrolist.music.playback.queues

import com.metrolist.innertube.models.SongItem
import com.metrolist.music.data.remote.ChartTrack
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class ChartTrackPager(
    private val tracks: List<ChartTrack>,
    private val hideExplicit: Boolean,
    private val resolve: suspend (ChartTrack) -> SongItem?,
) {
    @Volatile private var cursor = 0
    private val mutex = Mutex()

    fun hasNextPage(): Boolean = cursor < tracks.size

    suspend fun nextPage(): List<SongItem> = mutex.withLock {
        while (cursor < tracks.size) {
            val batch = tracks.drop(cursor).take(8)
            val songs = coroutineScope {
                batch.map { track -> async {
                    var song: SongItem? = null
                    for (attempt in 0..2) {
                        try {
                            song = resolve(track)
                            break
                        } catch (error: CancellationException) {
                            throw error
                        } catch (error: Exception) {
                            if (attempt == 2) throw error
                            delay(1_000)
                        }
                    }
                    song?.takeUnless { hideExplicit && it.explicit }
                } }.mapNotNull { it.await() }
            }
            // Advance only after the whole batch succeeds, so network errors cannot lose ranks.
            cursor += batch.size
            if (songs.isNotEmpty()) return@withLock songs
        }
        emptyList()
    }
}
