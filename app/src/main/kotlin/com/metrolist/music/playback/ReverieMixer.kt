package com.metrolist.music.playback

import com.metrolist.music.models.MediaMetadata
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlin.coroutines.coroutineContext
import kotlin.math.sqrt
import kotlin.math.PI
import kotlin.math.sin

interface ReveriePreviewDeck {
    val mediaId: String?
    val ready: Boolean
    val failed: Boolean
    val playing: Boolean
    fun prepare(song: MediaMetadata)
    fun setPlaying(value: Boolean)
}

data class ReverieGains(val incoming: Float, val outgoing: Float)

/** Preserve loudness across the overlap, including a seek that interrupts an earlier overlap. */
fun reverieCrossfadeGains(progress: Float, incomingStart: Float, outgoingStart: Float): ReverieGains {
    val sine = sin(playbackFadeInGain(progress) * (PI.toFloat() / 2))
    val p = sine * sine
    return ReverieGains(sqrt(incomingStart * incomingStart * (1 - p) + p), outgoingStart * sqrt(1 - p))
}

/** Audio owns its transition job: a chapter boundary must never cancel an audible overlap. */
class ReverieMixer(
    private val decks: List<ReveriePreviewDeck>,
    private val scope: CoroutineScope,
    private val applyGain: (Int, Float) -> Unit,
) {
    private val gains = floatArrayOf(0f, 0f)
    private val started = booleanArrayOf(false, false)
    private var enabled = false
    private val playingState = MutableStateFlow(false)
    private var active = 0
    private var activeId: String? = null
    private var requestedId: String? = null
    private var transition: Deferred<Boolean>? = null

    suspend fun reset() {
        transition?.cancelAndJoin()
        transition = null
        requestedId = null
        activeId = null
        active = 0
        decks.indices.forEach { index -> play(index, false); gain(index, 0f) }
    }

    fun prime(first: MediaMetadata?, second: MediaMetadata?) {
        if (activeId != null || transition != null) return
        listOf(first, second).forEachIndexed { position, song ->
            if (song != null && (position == 0 || song.id != first?.id)) decks[1 - position].prepare(song)
        }
    }

    fun setPlaying(value: Boolean) {
        enabled = value
        playingState.value = value
        decks.forEachIndexed { index, deck -> deck.setPlaying(value && started[index]) }
    }

    private fun gain(index: Int, value: Float) {
        gains[index] = value
        applyGain(index, value)
    }

    private fun play(index: Int, value: Boolean) {
        started[index] = value
        decks[index].setPlaying(enabled && value)
    }

    private suspend fun dissolve(duration: Long, sounding: () -> Boolean = { true }, apply: (Float) -> Unit) {
        val total = duration.coerceAtLeast(1) * 1_000_000L
        var elapsed = 0L
        apply(0f)
        while (elapsed < total) {
            playingState.first { it }
            val previous = System.nanoTime()
            delay(16)
            if (enabled && sounding()) elapsed += System.nanoTime() - previous
            apply((elapsed.toFloat() / total).coerceIn(0f, 1f))
        }
    }

    fun preload(song: MediaMetadata?) {
        if (song == null || song.id == activeId) return
        val index = 1 - active
        if (gains[index] > 0f) return
        val deck = decks[index]
        if (deck.mediaId == song.id && !deck.failed) return
        play(index, false)
        gain(index, 0f)
        deck.prepare(song)
    }

    suspend fun transitionTo(song: MediaMetadata?, duration: Long): Boolean {
        transition?.takeIf { requestedId == song?.id && !it.isCancelled }?.let { previous ->
            if (!previous.isCompleted) return previous.await()
            if (previous.await() && decks.any { it.mediaId == song?.id && it.ready && !it.failed }) return true
        }
        transition?.cancelAndJoin()
        requestedId = song?.id
        return scope.async { mixTo(song, duration) }.also { transition = it }.await()
    }

    private suspend fun silence(duration: Long) {
        val from = gains.copyOf()
        dissolve(duration) { progress ->
            from.indices.forEach { gain(it, from[it] * (1 - progress)) }
        }
        from.indices.forEach { play(it, false) }
        activeId = null
    }

    private suspend fun mixTo(song: MediaMetadata?, duration: Long): Boolean {
        if (song == null) { silence(duration); return false }
        val incoming = if (song.id == activeId) active else 1 - active
        val outgoing = 1 - incoming
        val player = decks[incoming]
        if (player.mediaId != song.id || player.failed) {
            if (gains[incoming] > 0f) {
                val from = gains[incoming]
                dissolve(120) { gain(incoming, from * (1 - it)) }
            }
            play(incoming, false)
            gain(incoming, 0f)
            player.prepare(song)
        }
        val ready = withTimeoutOrNull(6_000) {
            while (!player.ready && !player.failed) delay(16)
            player.ready && !player.failed
        } == true
        coroutineContext.ensureActive()
        if (!ready) { silence(duration); return false }
        play(incoming, true)
        // A pause/hold does not use up the incoming deck's startup allowance.
        var waiting = 0L
        while (!player.playing && !player.failed && waiting < 4_000) {
            delay(16)
            if (enabled) waiting += 16
        }
        if (!player.playing || player.failed) { silence(duration); return false }
        active = incoming
        activeId = song.id
        val incomingStart = gains[incoming]
        val outgoingStart = gains[outgoing]
        dissolve(duration, { player.playing || player.failed }) { progress ->
            val mix = reverieCrossfadeGains(progress, incomingStart, outgoingStart)
            gain(incoming, mix.incoming)
            gain(outgoing, mix.outgoing)
        }
        play(outgoing, false)
        return !player.failed
    }
}
