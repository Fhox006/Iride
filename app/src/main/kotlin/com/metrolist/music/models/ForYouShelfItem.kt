/**
 * Iride Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.models

import com.metrolist.innertube.models.AlbumItem
import com.metrolist.music.db.entities.Artist
import com.metrolist.music.db.entities.LocalItem

data class ForYouShelfItem(
    val artist: Artist,
    val tiles: List<ForYouShelfTile>,
)

sealed interface ForYouShelfTile {
    val id: String

    data class Local(val item: LocalItem) : ForYouShelfTile {
        override val id: String get() = item.id
    }

    data class RecommendedAlbum(val item: AlbumItem) : ForYouShelfTile {
        override val id: String get() = item.id
    }
}
