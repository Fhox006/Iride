/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.db.entities

import androidx.room.Entity

/**
 * Caches a user-set custom song order for an auto playlist (liked/downloaded/uploaded/starred),
 * which has no backing playlist row of its own to store a position on. [playlistKey] is the
 * screen's own id ("liked", "downloaded", ...). Songs no longer in the live list are simply
 * skipped on read, not pruned here.
 */
@Entity(tableName = "auto_playlist_song_order", primaryKeys = ["playlistKey", "songId"])
data class AutoPlaylistSongOrderEntity(
    val playlistKey: String,
    val songId: String,
    val position: Int,
)
