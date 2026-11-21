package com.metrolist.music.ui.component

import kotlin.math.sqrt

internal const val ArtistArtworkDetailResolution = 3840

/** Definition can improve during a flight, but the selected photograph cannot change. */
internal fun artistArtworkCanUpgrade(currentUrl: String?, candidateUrl: String?, currentWidth: Int, candidateWidth: Int): Boolean =
    currentUrl != null && currentUrl == candidateUrl && candidateWidth > currentWidth

/** Leave analysis headroom and keep a decoded ARGB photo inside its cache budget. */
internal fun artistArtworkDetailResolution(cacheBytes: Int): Int {
    require(cacheBytes > 0)
    val photoBytes = (cacheBytes - 512 * 1024).coerceAtLeast(4)
    return sqrt(photoBytes / 4.0).toInt().coerceIn(1, ArtistArtworkDetailResolution)
}

internal fun artistArtworkSquareSide(width: Int, height: Int, requestedSide: Int): Int {
    require(width > 0 && height > 0 && requestedSide > 0)
    return minOf(width, height, requestedSide)
}
