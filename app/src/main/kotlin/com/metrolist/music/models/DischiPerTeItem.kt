/**
 * Iride Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.models

import com.metrolist.innertube.models.AlbumItem
import com.metrolist.music.db.entities.Album

sealed class DischiPerTeItem {
    abstract val id: String

    data class Local(val album: Album) : DischiPerTeItem() {
        override val id: String get() = album.id
    }

    data class Remote(val item: AlbumItem) : DischiPerTeItem() {
        override val id: String get() = item.id
    }
}
