/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.lyrics

import android.content.Context
import com.metrolist.music.betterlyrics.BetterLyrics
import com.metrolist.music.constants.EnableBetterLyricsKey
import com.metrolist.music.utils.dataStore
import com.metrolist.music.utils.get
import timber.log.Timber

object BetterLyricsProvider : LyricsProvider {
    private const val TAG = "BetterLyricsProvider"

    override val name = "BetterLyrics"

    override fun isEnabled(context: Context): Boolean = context.dataStore[EnableBetterLyricsKey] ?: true

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
        return BetterLyrics.getLyrics(title, artist, duration, album, videoId = id)
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
        BetterLyrics.getAllLyrics(title, artist, duration, album, videoId = id, callback = callback)
    }
}
