/**
 * Iride Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.screens.artist

import android.content.Context
import android.net.ConnectivityManager
import android.net.Uri
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.metrolist.music.constants.AudioQualityKey
import com.metrolist.music.constants.AudioQuality
import com.metrolist.music.constants.MuzzaPlayerLogicKey
import com.metrolist.music.utils.YTPlayerUtils
import com.metrolist.music.utils.dataStore
import com.metrolist.music.utils.get
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

/**
 * Isolated ExoPlayer for the "guess the song" game — never touches MusicService/PlayerConnection,
 * so the real mini player and queue stay untouched while a round is playing a snippet.
 *
 * Every round gets its own [ExoPlayer], prepared and buffered up front via [prepareRound] during
 * the loading screen, so [playPrepared] is just a `play()` call with zero network/buffering delay
 * once the game is actually running.
 */
class ArtistGameAudioService(
    private val context: Context,
) {
    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private val preparedPlayers = mutableMapOf<String, ExoPlayer>()
    private var currentId: String? = null

    /** Resolves the playable stream URL for [songId] without touching the player, so it can be cached ahead of time. */
    suspend fun resolveStreamUrl(songId: String): Uri? =
        try {
            val audioQuality = context.dataStore.get(AudioQualityKey).let {
                AudioQuality.valueOf(it ?: AudioQuality.AUTO.name)
            }
            val muzzaPlayerLogicEnabled = context.dataStore.get(MuzzaPlayerLogicKey, true)
            val playbackData = withContext(Dispatchers.IO) {
                if (muzzaPlayerLogicEnabled) {
                    YTPlayerUtils.playerResponseForPlaybackMuzza(
                        videoId = songId,
                        audioQuality = audioQuality,
                        connectivityManager = connectivityManager,
                    ).getOrNull()
                } else {
                    YTPlayerUtils.playerResponseForPlayback(
                        videoId = songId,
                        audioQuality = audioQuality,
                        connectivityManager = connectivityManager,
                    ).getOrNull()
                }
            }
            playbackData?.streamUrl?.takeIf { it.isNotBlank() }?.toUri()
        } catch (e: Exception) {
            null
        }

    /**
     * Prepares [songId] on its own [ExoPlayer], seeks to [positionMs] and suspends until the
     * player has buffered enough to play instantly. Call for every round up front.
     */
    suspend fun prepareRound(songId: String, uri: Uri, positionMs: Long) {
        suspendCancellableCoroutine { cont ->
            val exoPlayer = ExoPlayer.Builder(context).build()
            val listener = object : Player.Listener {
                override fun onPlaybackStateChanged(playbackState: Int) {
                    if (playbackState == Player.STATE_READY) {
                        exoPlayer.removeListener(this)
                        if (cont.isActive) cont.resume(Unit)
                    }
                }
            }
            exoPlayer.addListener(listener)
            exoPlayer.playWhenReady = false
            exoPlayer.setMediaItem(MediaItem.Builder().setUri(uri).setMediaId(songId).build())
            exoPlayer.seekTo(positionMs)
            exoPlayer.prepare()
            preparedPlayers[songId] = exoPlayer
            cont.invokeOnCancellation { exoPlayer.removeListener(listener) }
        }
    }

    /** Plays a round already buffered by [prepareRound] — instant, no network/prepare delay. */
    fun playPrepared(songId: String) {
        currentId = songId
        preparedPlayers[songId]?.play()
    }

    fun stop() {
        currentId?.let { preparedPlayers[it]?.pause() }
    }

    fun release() {
        preparedPlayers.values.forEach { it.release() }
        preparedPlayers.clear()
        currentId = null
    }
}
