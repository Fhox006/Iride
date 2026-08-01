/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.db.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "search_history",
    indices = [
        Index(
            value = ["query"],
            unique = true,
        ),
    ],
)
data class SearchHistory(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val query: String,
    // Populated once the user opens a result for this query (Spotify-style history): the row then
    // displays that item's title/thumbnail instead of the raw query text, and tapping it jumps
    // straight back to the item instead of re-running the search.
    val itemId: String? = null,
    val itemType: String? = null,
    val title: String? = null,
    val thumbnailUrl: String? = null,
)
