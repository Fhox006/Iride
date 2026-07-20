package com.metrolist.music.viewmodels

import com.metrolist.music.models.DischiPerTeItem

// Library Albums screen "Recommended Albums" carousel: process-scoped so leaving/re-entering the
// screen (which recreates LibraryAlbumsViewModel) keeps the same suggestions, while a full app
// restart (process death) clears it and lets a fresh batch generate. Mirrors HomeCache.
object LibraryAlbumsCache {
    var recommendedAlbums: List<DischiPerTeItem>? = null

    fun clear() {
        recommendedAlbums = null
    }
}
