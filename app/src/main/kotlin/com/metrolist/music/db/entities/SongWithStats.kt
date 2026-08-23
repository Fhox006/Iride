/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.db.entities

import androidx.compose.runtime.Immutable
import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation

@Immutable
data class SongWithStats(
    val id: String,
    val title: String,
    @Relation(
        entity = ArtistEntity::class,
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            value = SortedSongArtistMap::class,
            parentColumn = "songId",
            entityColumn = "artistId"
        )
    )
    val artists: List<ArtistEntity>,
    val thumbnailUrl: String,
    val artistName: String?,
    val songCountListened: Int,
    val timeListened: Long?,
    val isVideo: Boolean = false,
)
