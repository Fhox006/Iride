/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.lyrics

import android.content.Context
import com.metrolist.paxsenix.Paxsenix
import com.metrolist.music.constants.EnablePaxsenixKey
import com.metrolist.music.utils.dataStore
import com.metrolist.music.utils.get
import timber.log.Timber

object PaxsenixLyricsProvider : LyricsProvider {
    private const val TAG = "PaxsenixProvider"

    @Volatile private var initialized = false
    private val initLock = Any()

    private fun ensureInit(context: Context) {
        if (!initialized) synchronized(initLock) {
            if (!initialized) {
                Paxsenix.init(context.applicationContext)
                initialized = true
            }
        }
    }

    override val name = "Paxsenix"

    override fun isEnabled(context: Context): Boolean = context.dataStore[EnablePaxsenixKey] ?: true

    override suspend fun getLyrics(
        context: Context,
        id: String,
        title: String,
        artist: String,
        duration: Int,
        album: String?,
    ): Result<String> {
        Timber.tag(TAG).d("getLyrics called: title='$title', artist='$artist', duration=$duration")
        if (duration <= 0) {
            Timber.tag(TAG).w("Skipping ideal match quality because invalid duration=$duration for title=$title artist=$artist")
        }

        try {
            ensureInit(context)
            val result = Paxsenix.getLyrics(title, artist, duration, album)
            
            result.onSuccess { lyrics ->
                Timber.tag(TAG).i("Success! Got ${lyrics.length} chars of lyrics")
            }.onFailure { e ->
                Timber.tag(TAG).e(e, "Failed to get lyrics")
            }
            
            return result
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Exception in getLyrics")
            return Result.failure(e)
        }
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
        Timber.tag(TAG).d("getAllLyrics called")
        try {
            ensureInit(context)
            Paxsenix.getAllLyrics(title, artist, duration, album, callback)
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Error fetching lyrics from Paxsenix")
            callback("")
        }
    }
}
