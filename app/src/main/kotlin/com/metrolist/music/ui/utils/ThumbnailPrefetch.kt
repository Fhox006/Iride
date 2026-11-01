/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.utils

import android.content.Context
import coil3.imageLoader
import coil3.request.CachePolicy
import coil3.request.ImageRequest

/**
 * Eagerly enqueues thumbnail loads for the first [count] items so artwork is already
 * in Coil's cache by the time the user scrolls to it, instead of waiting for each
 * AsyncImage to enter composition.
 */
fun prefetchThumbnails(
    context: Context,
    urls: List<String?>,
    count: Int = 20,
    size: Int = 300,
) {
    val loader = context.imageLoader
    urls.asSequence()
        .filterNotNull()
        .take(count)
        .forEach { url ->
            loader.enqueue(
                ImageRequest.Builder(context)
                    .data(url.resize(size, size))
                    .memoryCachePolicy(CachePolicy.ENABLED)
                    .diskCachePolicy(CachePolicy.ENABLED)
                    .build(),
            )
        }
}
