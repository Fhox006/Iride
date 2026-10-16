package com.metrolist.music.ui.component

import android.content.Context
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import com.metrolist.music.ui.utils.resize

/** One request identity lets the player, menu and video glass reuse preloaded covers. */
internal fun irideFolioPlayerArtworkRequest(context: Context, thumbnailUrl: String?): ImageRequest =
    ImageRequest.Builder(context)
        .data(thumbnailUrl?.resize(1080, 1080))
        .size(1080, 1080)
        .memoryCacheKey("iride_player_art:$thumbnailUrl")
        .placeholderMemoryCacheKey("iride_player_art:$thumbnailUrl")
        .memoryCachePolicy(CachePolicy.ENABLED)
        .diskCachePolicy(CachePolicy.ENABLED)
        .build()
