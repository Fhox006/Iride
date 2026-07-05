/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.lyrics

import android.content.Context
import com.metrolist.music.betterlyrics.UnisonLyrics
import com.metrolist.music.constants.EnableBetterLyricsUnisonKey
import com.metrolist.music.utils.dataStore
import com.metrolist.music.utils.get
import timber.log.Timber

object BetterLyricsUnisonProvider : LyricsProvider {
    private const val TAG = "BetterLyricsUnisonProvider"

    override val name = "BetterLyricsUnison"

    override fun isEnabled(context: Context): Boolean = context.dataStore[EnableBetterLyricsUnisonKey] ?: true

    override suspend fun getLyrics(
        context: Context,
        id: String,
        title: String,
        artist: String,
        duration: Int,
        album: String?,
    ): Result<String> {
        if (duration <= 0) {
            Timber.tag(TAG).w("Skipping ideal match quality because invalid duration=$duration for title=$title artist=$artist")
        }
        return UnisonLyrics.getLyrics(title, artist, duration, album, videoId = id)
    }

    override suspend fun getAllLyrics(
        context: Context,
        id: String,
        title: String,
        artist: String,
        duration: Int,
        album: String?,
        callback: (String) -> Unit,
    ) {
        UnisonLyrics.getAllLyrics(title, artist, duration, album, videoId = id, callback = callback)
    }
}
