package com.metrolist.music.db.entities

internal fun isCustomPlaylistImage(url: String?): Boolean =
    url != null && (url.startsWith("file:") || url.startsWith("content:") || url.contains("studio_square_thumbnail"))
