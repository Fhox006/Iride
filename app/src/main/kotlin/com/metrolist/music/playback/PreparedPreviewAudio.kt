@file:androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)

package com.metrolist.music.playback

import android.content.Context
import android.net.ConnectivityManager
import android.net.Uri
import androidx.core.content.getSystemService
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import com.metrolist.music.constants.AudioQuality
import com.metrolist.music.constants.AudioQualityKey
import com.metrolist.music.constants.MuzzaPlayerLogicKey
import com.metrolist.music.extensions.toEnum
import com.metrolist.music.utils.YTPlayerUtils
import com.metrolist.music.utils.dataStore
import com.metrolist.music.utils.get
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/** Game and recap previews retain the exact player that reached READY, including its buffered audio. */
internal class PreparedPreviewAudio(
    private val context: Context,
    private val capacity: Int = 3,
    private val onAudioBecomingNoisy: (() -> Unit)? = null,
) {
    private val players = linkedMapOf<String, ExoPlayer>()
    private var currentId: String? = null
    private var released = false

    suspend fun resolveStreamUrl(songId: String): Uri? = withContext(Dispatchers.IO) {
        try {
            val quality = context.dataStore.get(AudioQualityKey).toEnum(AudioQuality.AUTO)
            val connectivity = requireNotNull(context.getSystemService<ConnectivityManager>())
            val result = if (context.dataStore.get(MuzzaPlayerLogicKey, true)) {
                YTPlayerUtils.playerResponseForPlaybackMuzza(songId, audioQuality = quality, connectivityManager = connectivity)
            } else {
                YTPlayerUtils.playerResponseForPlayback(songId, audioQuality = quality, connectivityManager = connectivity)
            }
            result.getOrNull()?.streamUrl?.takeIf(String::isNotBlank)?.toUri()
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            null
        }
    }

    fun preparedPlayer(songId: String): ExoPlayer? = players[songId]

    suspend fun prepareRound(songId: String, uri: Uri, positionMs: Long) = withContext(Dispatchers.Main.immediate) {
        check(!released)
        players[songId]?.let { existing ->
            if (existing.playbackState == Player.STATE_READY && existing.playerError == null) {
                existing.pause()
                if (existing.currentPosition != positionMs) existing.seekTo(positionMs)
                awaitReady(existing)
                return@withContext
            }
            players.remove(songId)
            existing.release()
        }
        if (players.size >= capacity) {
            val oldest = players.entries.first()
            players.remove(oldest.key)
            oldest.value.release()
        }
        val player = ExoPlayer.Builder(context)
            .setLoadControl(DefaultLoadControl.Builder().setBackBuffer(30_000, false).build())
            .build()
        players[songId] = player
        try {
            player.playWhenReady = false
            if (onAudioBecomingNoisy != null) {
                player.setHandleAudioBecomingNoisy(true)
                player.addListener(object : Player.Listener {
                    override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
                        if (reason == Player.PLAY_WHEN_READY_CHANGE_REASON_AUDIO_BECOMING_NOISY) onAudioBecomingNoisy.invoke()
                    }
                })
            }
            player.setMediaItem(MediaItem.Builder().setUri(uri).setMediaId(songId).build(), positionMs)
            player.prepare()
            awaitReady(player)
        } catch (error: Exception) {
            if (players[songId] === player) {
                players.remove(songId)
                player.release()
            }
            throw error
        }
    }

    private suspend fun awaitReady(player: ExoPlayer) {
        if (player.playbackState == Player.STATE_READY && player.playerError == null) return
        val ready = withTimeoutOrNull(15_000) {
            awaitPlayer(player) { playbackState == Player.STATE_READY }
            true
        }
        if (ready != true) throw IOException("Preview preparation timed out")
    }

    private suspend fun awaitPlayer(player: ExoPlayer, ready: ExoPlayer.() -> Boolean) {
        var listener: Player.Listener? = null
        try {
            suspendCancellableCoroutine { continuation ->
                fun complete() {
                    if (!continuation.isActive) return
                    val error = player.playerError
                    when {
                        error != null -> continuation.resumeWithException(error)
                        player.playbackState == Player.STATE_ENDED -> continuation.resumeWithException(IOException("Preview ended"))
                        player.ready() -> continuation.resume(Unit)
                    }
                }
                listener = object : Player.Listener {
                    override fun onPlaybackStateChanged(playbackState: Int) = complete()
                    override fun onIsPlayingChanged(isPlaying: Boolean) = complete()
                    override fun onPlayerError(error: PlaybackException) = complete()
                }.also(player::addListener)
                complete()
            }
        } finally {
            listener?.let(player::removeListener)
        }
    }

    fun playPrepared(songId: String) {
        currentId = songId
        players[songId]?.play()
    }

    suspend fun playPreparedAwaitStart(songId: String) = withContext(Dispatchers.Main.immediate) {
        val player = players[songId] ?: throw IOException("Preview was not prepared")
        currentId = songId
        player.play()
        val started = withTimeoutOrNull(5_000) { awaitPlayer(player) { isPlaying }; true }
        if (started != true) throw IOException("Preview did not start")
    }

    fun stop() { currentId?.let { players[it]?.pause() } }

    fun release() {
        released = true
        players.values.forEach(ExoPlayer::release)
        players.clear()
        currentId = null
    }
}
