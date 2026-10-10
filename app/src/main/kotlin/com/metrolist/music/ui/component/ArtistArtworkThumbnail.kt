package com.metrolist.music.ui.component

import com.metrolist.music.data.remote.ArtistArtworkParser
import com.metrolist.music.data.remote.ArtistArtworkSource
import com.metrolist.music.ui.utils.resize

/** Keep the selected photo's identity while requesting only the pixels needed on screen. */
internal fun artistArtworkDownloadUrl(url: String, resolution: Int): String =
    if (url.contains(".mzstatic.com/")) {
        ArtistArtworkParser.imageUrl(ArtistArtworkSource.APPLE_MUSIC, url, resolution)
    } else {
        url.resize(resolution, resolution)
    }
