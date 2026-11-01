/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.db.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "playlist_category_song_map",
    foreignKeys = [
        ForeignKey(
            entity = PlaylistCategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = SongEntity::class,
            parentColumns = ["id"],
            childColumns = ["songId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["categoryId", "songId"], unique = true),
        Index(value = ["songId"]),
    ],
)
data class PlaylistCategorySongMap(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val categoryId: String,
    val songId: String,
)
