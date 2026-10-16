/**
 * Iride Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.screens.artist

import android.content.Context
import android.net.Uri
import com.metrolist.music.playback.PreparedPreviewAudio

/** Isolated game previews use the same retained players as monthly Reverie soundtracks. */
class ArtistGameAudioService(context: Context) {
    private val audio = PreparedPreviewAudio(context)

    suspend fun resolveStreamUrl(songId: String): Uri? = audio.resolveStreamUrl(songId)

    suspend fun prepareRound(songId: String, uri: Uri, positionMs: Long) = audio.prepareRound(songId, uri, positionMs)

    fun playPrepared(songId: String) = audio.playPrepared(songId)

    suspend fun playPreparedAwaitStart(songId: String) = audio.playPreparedAwaitStart(songId)

    fun stop() = audio.stop()

    fun release() = audio.release()
}
