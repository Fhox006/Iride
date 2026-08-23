/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.db.entities

import androidx.compose.runtime.Immutable
import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation
import com.metrolist.innertube.models.AlbumItem

@Immutable
data class Album(
    @Embedded
    val album: AlbumEntity,
    @Relation(
        entity = ArtistEntity::class,
        entityColumn = "id",
        parentColumn = "id",
        associateBy =
        Junction(
            value = AlbumArtistMap::class,
            parentColumn = "albumId",
            entityColumn = "artistId",
        ),
    )
    val artists: List<ArtistEntity> = emptyList(),
    val songCountListened: Int? = 0,
    val timeListened: Long? = 0
) : LocalItem() {
    override val id: String
        get() = album.id
    override val title: String
        get() = album.title
    override val thumbnailUrl: String?
        get() = album.thumbnailUrl
}

fun AlbumItem.toAlbumEntity() = Album(
    album = AlbumEntity(
        id = browseId,
        playlistId = playlistId,
        title = title,
        year = year,
        thumbnailUrl = thumbnail,
        explicit = explicit,
        songCount = if (albumType?.contains("Single", ignoreCase = true) == true) 1 else 0,
        duration = 0,
    ),
    artists = artists?.mapNotNull { a -> a.id?.let { ArtistEntity(id = it, name = a.name) } } ?: emptyList(),
)
