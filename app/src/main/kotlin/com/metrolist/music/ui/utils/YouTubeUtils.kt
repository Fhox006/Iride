/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.utils

fun String.resize(
    width: Int? = null,
    height: Int? = null,
): String {
    if (width == null && height == null) return this
    "https://lh3\\.googleusercontent\\.com/.*=w(\\d+)-h(\\d+).*".toRegex()
        .matchEntire(this)?.groupValues?.let { group ->
        val (W, H) = group.drop(1).map { it.toInt() }
        var w = width
        var h = height
        if (w != null && h == null) h = (w / W) * H
        if (w == null && h != null) w = (h / H) * W
        return "${split("=w")[0]}=w$w-h$h-p-l90-rj"
    }
    if (this.contains("lh3.googleusercontent.com") && !this.contains("=w")) {
        val w = width ?: height!!
        val h = height ?: width!!
        return "$this=w$w-h$h-p-l90-rj"
    }
    if (this.contains("yt3.ggpht.com")) {
        val size = width ?: height ?: return this
        return if (contains("=s")) {
            "${substringBefore("=s")}=s$size"
        } else {
            "$this=s$size"
        }
    }
    // Standard YouTube video thumbnails: upgrade to maxresdefault for large display sizes
    if (this.contains("i.ytimg.com")) {
        val maxSize = maxOf(width ?: 0, height ?: 0)
        if (maxSize >= 480) {
            return replace("hqdefault.jpg", "maxresdefault.jpg")
                .replace("mqdefault.jpg", "maxresdefault.jpg")
                .replace("sddefault.jpg", "maxresdefault.jpg")
        }
    }
    return this
}
