package com.metrolist.innertube.utils

import com.metrolist.innertube.YouTube
import com.metrolist.innertube.pages.LibraryPage
import com.metrolist.innertube.pages.PlaylistPage
import java.security.MessageDigest

@JvmName("completedLibrary")
suspend fun Result<PlaylistPage>.completed(requireComplete: Boolean = false): Result<PlaylistPage> = runCatching {
    val page = getOrThrow()
    val songs = page.songs.toMutableList()
    var continuation = page.songsContinuation ?: page.continuation
    val seenContinuations = mutableSetOf<String>()
    var requestCount = 0
    val maxRequests = 50
    var consecutiveEmptyResponses = 0
    while (continuation != null && requestCount < maxRequests) {
        if (continuation in seenContinuations) {
            break
        }
        seenContinuations.add(continuation)
        requestCount++
        val result = YouTube.playlistContinuation(continuation)
        val continuationPage = (if (requireComplete) result.getOrThrow() else result.getOrNull()) ?: break
        continuation = continuationPage.continuation
        if (continuationPage.songs.isEmpty()) {
            consecutiveEmptyResponses++
            if (consecutiveEmptyResponses >= 2) break
        } else {
            consecutiveEmptyResponses = 0
            songs += continuationPage.songs
        }
    }
    check(!requireComplete || continuation == null) { "Incomplete playlist response" }
    PlaylistPage(
        playlist = page.playlist,
        songs = songs,
        songsContinuation = null,
        continuation = page.continuation
    )
}

@JvmName("completedPlaylist")
suspend fun Result<LibraryPage>.completed(requireComplete: Boolean = false): Result<LibraryPage> = runCatching {
    val page = getOrThrow()
    val items = page.items.toMutableList()
    var continuation = page.continuation
    val seenContinuations = mutableSetOf<String>()
    var requestCount = 0
    val maxRequests = 50
    var consecutiveEmptyResponses = 0
    while (continuation != null && requestCount < maxRequests) {
        if (continuation in seenContinuations) {
            break
        }
        seenContinuations.add(continuation)
        requestCount++
        val result = YouTube.libraryContinuation(continuation)
        val continuationPage = (if (requireComplete) result.getOrThrow() else result.getOrNull()) ?: break
        continuation = continuationPage.continuation
        if (continuationPage.items.isEmpty()) {
            consecutiveEmptyResponses++
            if (consecutiveEmptyResponses >= 2) break
        } else {
            consecutiveEmptyResponses = 0
            items += continuationPage.items
        }
    }
    check(!requireComplete || continuation == null) { "Incomplete library response" }
    LibraryPage(
        items = items,
        continuation = null
    )
}

fun ByteArray.toHex(): String = joinToString(separator = "") { eachByte -> "%02x".format(eachByte) }

fun sha1(str: String): String = MessageDigest.getInstance("SHA-1").digest(str.toByteArray()).toHex()

fun parseCookieString(cookie: String): Map<String, String> =
    cookie.split("; ")
        .filter { it.isNotEmpty() }
        .mapNotNull { part ->
            val splitIndex = part.indexOf('=')
            if (splitIndex == -1) null
            else part.substring(0, splitIndex) to part.substring(splitIndex + 1)
        }
        .toMap()

fun String.parseTime(): Int? {
    try {
        val parts = split(":").map { it.toInt() }
        if (parts.size == 2) {
            return parts[0] * 60 + parts[1]
        }
        if (parts.size == 3) {
            return parts[0] * 3600 + parts[1] * 60 + parts[2]
        }
    } catch (e: Exception) {
        return null
    }
    return null
}

fun isPrivateId(browseId: String): Boolean {
    return browseId.contains("privately")
}
