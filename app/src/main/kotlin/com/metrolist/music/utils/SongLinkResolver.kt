/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.utils

import android.content.Context
import android.content.Intent
import android.widget.Toast
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject

enum class SharePlatform(val odesliKey: String) {
    YOUTUBE_MUSIC("youtubeMusic"),
    SPOTIFY("spotify"),
    APPLE_MUSIC("appleMusic"),
    SOUNDCLOUD("soundcloud"),
}

/**
 * Resolves a YouTube Music link to its equivalent on other streaming platforms
 * using the Odesli (song.link) public API.
 */
object SongLinkResolver {
    private val client = HttpClient()
    private const val API_URL = "https://api.song.link/v1-alpha.1/links"

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /**
     * Resolves the link and shares it, entirely decoupled from any Compose lifecycle.
     */
    fun shareLink(
        context: Context,
        youtubeMusicUrl: String,
        platform: SharePlatform,
        fallbackQuery: String?,
        notFoundMessage: String,
    ) {
        appScope.launch {
            val result = resolve(youtubeMusicUrl, platform, fallbackQuery)
            withContext(Dispatchers.Main) {
                result
                    .onSuccess { url ->
                        val intent =
                            Intent().apply {
                                action = Intent.ACTION_SEND
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, url)
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                        context.startActivity(Intent.createChooser(intent, null).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        })
                    }.onFailure {
                        Toast.makeText(context, notFoundMessage, Toast.LENGTH_SHORT).show()
                    }
            }
        }
    }

    suspend fun resolve(
        youtubeMusicUrl: String,
        platform: SharePlatform,
        fallbackQuery: String? = null,
    ): Result<String> =
        withContext(Dispatchers.IO) {
            if (platform == SharePlatform.YOUTUBE_MUSIC) {
                return@withContext Result.success(youtubeMusicUrl)
            }

            val exactMatch =
                runCatching {
                    val response =
                        client
                            .get(API_URL) {
                                parameter("url", youtubeMusicUrl)
                            }.bodyAsText()

                    JSONObject(response)
                        .optJSONObject("linksByPlatform")
                        ?.optJSONObject(platform.odesliKey)
                        ?.getString("url")
                        ?: throw IllegalStateException("Song not available on this platform")
                }

            if (exactMatch.isSuccess) return@withContext exactMatch

            fallbackQuery?.let { query ->
                Result.success(searchUrl(platform, query))
            } ?: exactMatch
        }

    private fun searchUrl(
        platform: SharePlatform,
        query: String,
    ): String {
        val encoded = java.net.URLEncoder.encode(query, "UTF-8")
        return when (platform) {
            SharePlatform.SPOTIFY -> "https://open.spotify.com/search/$encoded"
            SharePlatform.APPLE_MUSIC -> "https://music.apple.com/search?term=$encoded"
            SharePlatform.SOUNDCLOUD -> "https://soundcloud.com/search?q=$encoded"
            SharePlatform.YOUTUBE_MUSIC -> "https://music.youtube.com/search?q=$encoded"
        }
    }
}
