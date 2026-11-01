package com.metrolist.music.ui.menu

import android.util.LruCache
import com.metrolist.innertube.YouTube
import com.metrolist.innertube.models.AlbumItem

/** Keeps album variants available immediately when the album menu is opened again. */
internal object AlbumOtherVersionsCache {
    private val cache = LruCache<String, List<AlbumItem>>(128)

    fun get(albumId: String): List<AlbumItem>? = cache.get(albumId)

    suspend fun fetch(albumId: String): List<AlbumItem>? {
        cache.get(albumId)?.let { return it }
        return YouTube.album(albumId).getOrNull()?.otherVersions?.also { cache.put(albumId, it) }
    }
}
