package com.metrolist.music.viewmodels

import com.metrolist.music.models.DischiPerTeItem

object LibraryAlbumsCache {
    var recommendedAlbums: List<DischiPerTeItem>? = null

    fun clear() {
        recommendedAlbums = null
    }
}
