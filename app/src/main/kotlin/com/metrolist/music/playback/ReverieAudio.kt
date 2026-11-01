@file:androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)

package com.metrolist.music.playback

import android.content.Context
import android.media.AudioFocusRequest
import android.media.AudioManager
import androidx.core.content.getSystemService
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.metrolist.music.models.MediaMetadata
import com.metrolist.music.models.REVERIE_DISSOLVE_MILLIS
import com.metrolist.music.models.ReverieChapter
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.IOException
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

class ReverieAudioFactory @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    fun create(onFocusLost: () -> Unit): ReverieAudio = ReverieAudio(context, onFocusLost)
}

/** Two mixing slots select retained game-style preview players without preparing their streams again. */
class ReverieAudio internal constructor(
    context: Context,
    private val onFocusLost: () -> Unit,
) {
    val dissolveMillis = REVERIE_DISSOLVE_MILLIS
    private val manager = context.getSystemService<AudioManager>()!!
    private var enabled = false
    private var muted = false
    private var ducked = false
    private var released = false
    private var masterVolume = 1f
    private val focus = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
        .setAudioAttributes(android.media.AudioAttributes.Builder()
            .setUsage(android.media.AudioAttributes.USAGE_MEDIA)
            .setContentType(android.media.AudioAttributes.CONTENT_TYPE_MUSIC).build())
        .setOnAudioFocusChangeListener { change ->
            ducked = change == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK
            if (change == AudioManager.AUDIOFOCUS_LOSS || change == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT) {
                setPlaying(false)
                onFocusLost()
            }
            applyVolumes()
        }.build()

    private val previews = PreparedPreviewAudio(context, capacity = ReverieChapter.entries.size,
        onAudioBecomingNoisy = { setPlaying(false); onFocusLost() })

    private fun previewStart(song: MediaMetadata): Long = if (song.duration >= 50) 30_000L else 0L

    private inner class PreviewDeck : ReveriePreviewDeck {
        var player: ExoPlayer? = null
            private set
        override val mediaId: String? get() = player?.currentMediaItem?.mediaId
        override val ready: Boolean get() = player?.playbackState == Player.STATE_READY
        override val failed: Boolean get() = player == null || player?.playerError != null ||
            player?.playbackState == Player.STATE_ENDED || player?.playbackState == Player.STATE_IDLE
        override val playing: Boolean get() = player?.isPlaying == true
        override fun setPlaying(value: Boolean) { player?.playWhenReady = value }
        override fun prepare(song: MediaMetadata) {
            player?.let { it.pause(); it.volume = 0f }
            player = previews.preparedPlayer(song.id)
            player?.let {
                it.volume = 0f
                if (it.currentPosition != previewStart(song)) it.seekTo(previewStart(song))
            }
        }
    }

    private val decks = List(2) { PreviewDeck() }
    private val gains = floatArrayOf(0f, 0f)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val mixer = ReverieMixer(decks, scope) { index, gain -> gains[index] = gain; applyVolumes() }

    /** Resolve in parallel, then keep each READY player alive exactly as the artist game does. */
    suspend fun prepareAll(songs: List<MediaMetadata>, onProgress: (Int, Int) -> Unit) = withContext(Dispatchers.Main.immediate) {
        check(!released)
        mixer.reset()
        val tracks = songs.distinctBy { it.id }
        require(tracks.isNotEmpty() && tracks.size <= ReverieChapter.entries.size)
        val permits = Semaphore(3)
        var completed = 0
        onProgress(0, tracks.size)
        coroutineScope {
            tracks.map { song -> async {
                permits.withPermit {
                    val existing = previews.preparedPlayer(song.id)
                    if (existing?.playbackState != Player.STATE_READY || existing.playerError != null) {
                        val prepared = withTimeoutOrNull(20_000) {
                            val uri = previews.resolveStreamUrl(song.id) ?: throw IOException("Reverie preview unavailable")
                            previews.prepareRound(song.id, uri, previewStart(song))
                            true
                        }
                        if (prepared != true) throw IOException("Reverie preview preparation timed out")
                    }
                    previews.preparedPlayer(song.id)?.apply {
                        volume = 0f
                        repeatMode = Player.REPEAT_MODE_ONE
                    }
                    onProgress(++completed, tracks.size)
                }
            } }.awaitAll()
        }
    }

    private fun applyVolumes() {
        if (released) return
        val master = masterVolume * if (muted) 0f else if (ducked) .2f else 1f
        decks.forEachIndexed { i, deck -> deck.player?.volume = gains[i] * master }
    }

    fun setMuted(value: Boolean) { muted = value; applyVolumes() }
    fun setMasterVolume(value: Float) { masterVolume = value.coerceIn(0f, 1f); applyVolumes() }

    fun setPlaying(value: Boolean) {
        if (released) return
        if (value && !enabled && manager.requestAudioFocus(focus) != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            onFocusLost()
            return
        }
        if (!value && enabled) manager.abandonAudioFocusRequest(focus)
        enabled = value
        mixer.setPlaying(value)
    }

    fun preload(song: MediaMetadata?) { if (!released) mixer.preload(song) }

    fun prime(first: MediaMetadata?, second: MediaMetadata?) { if (!released) mixer.prime(first, second) }

    suspend fun transitionTo(song: MediaMetadata?, durationMillis: Long): Boolean =
        !released && mixer.transitionTo(song, durationMillis)

    suspend fun finish(durationMillis: Long) {
        if (!released) mixer.transitionTo(null, durationMillis)
    }

    fun release() {
        if (released) return
        released = true
        scope.cancel()
        previews.release()
        manager.abandonAudioFocusRequest(focus)
    }
}
