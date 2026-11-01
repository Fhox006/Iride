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
        if (w != null && h == null) h = (w.toLong() * H / W.coerceAtLeast(1)).toInt().coerceAtLeast(1)
        if (w == null && h != null) w = (h.toLong() * W / H.coerceAtLeast(1)).toInt().coerceAtLeast(1)
        return "${split("=w")[0]}=w$w-h$h-p-l90-rj"
    }
    if (this.contains("lh3.googleusercontent.com") && !this.contains("=w")) {
        val w = width ?: height!!
        val h = height ?: width!!
        val base = if (contains("=s")) substringBefore("=s") else this
        return "$base=w$w-h$h-p-l90-rj"
    }
    if (this.contains("yt3.ggpht.com")) {
        val size = width ?: height ?: return this
        return if (contains("=s")) {
            "${substringBefore("=s")}=s$size"
        } else {
            "$this=s$size"
        }
    }
    // Videos do not always have maxresdefault artwork; preserve the URL supplied by YouTube.
    return this
}
