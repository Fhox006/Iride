/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.menu

import android.annotation.SuppressLint
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.navigation.NavController
import com.metrolist.innertube.YouTube
import com.metrolist.innertube.models.SongItem
import com.metrolist.music.LocalDatabase
import com.metrolist.music.db.entities.ArtistEntity
import com.metrolist.music.db.entities.Song
import com.metrolist.music.models.toMediaMetadata
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber

@SuppressLint("MutableCollectionMutableState")
@Composable
fun YouTubeSongMenu(
    song: SongItem,
    navController: NavController,
    onDismiss: () -> Unit,
    onHistoryRemoved: () -> Unit = {},
    showStarButton: Boolean = true,
) {
    val database = LocalDatabase.current
    val librarySong by database.song(song.id).collectAsState(initial = null)
    val coroutineScope = rememberCoroutineScope()

    val songForMenu = librarySong ?: remember(song.id) {
        Song(
            song = song.toMediaMetadata().toSongEntity(),
            artists = song.artists.mapNotNull { a ->
                a.id?.let { ArtistEntity(id = it, name = a.name) }
            },
            album = null,
        )
    }

    val onYtHistoryRemoved: (() -> Unit)? = song.historyRemoveToken?.let { token ->
        {
            coroutineScope.launch {
                Timber.d("[HISTORY_REMOVE] Removing song ${song.id} from YTM history")
                YouTube.feedback(listOf(token))
                    .onSuccess { Timber.d("[HISTORY_REMOVE] Successfully removed from YTM history") }
                    .onFailure { e -> Timber.e(e, "[HISTORY_REMOVE] Failed to remove from YTM history") }
                delay(500)
                onHistoryRemoved()
                onDismiss()
            }
        }
    }

    SongMenu(
        originalSong = songForMenu,
        navController = navController,
        onDismiss = onDismiss,
        showStarButton = showStarButton,
        onHistoryRemoved = onYtHistoryRemoved,
    )
}
