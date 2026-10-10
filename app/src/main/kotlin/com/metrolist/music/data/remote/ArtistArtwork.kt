package com.metrolist.music.data.remote

import com.metrolist.music.utils.ArtistNameMatcher
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import org.jsoup.Jsoup
import java.net.URI

enum class ArtistArtworkSource { APPLE_MUSIC, SPOTIFY, YOUTUBE_MUSIC }

@Serializable
data class ArtistArtworkCandidate(
    val source: ArtistArtworkSource,
    val artistName: String,
    val artistUrl: String,
    val imageUrl: String,
)

internal object ArtistArtworkParser {
    fun profileUrl(source: ArtistArtworkSource, url: String): String? {
        val uri = runCatching { URI(url) }.getOrNull() ?: return null
        if (uri.scheme != "https" || uri.userInfo != null || uri.port != -1) return null
        val path = uri.path.orEmpty()
        return when (source) {
            ArtistArtworkSource.YOUTUBE_MUSIC -> null
            ArtistArtworkSource.SPOTIFY -> {
                if (uri.host != "open.spotify.com") return null
                val id = Regex("^/(?:intl-[a-z]{2}/)?artist/([A-Za-z0-9]{22})/?$")
                    .matchEntire(path)?.groupValues?.get(1) ?: return null
                "https://open.spotify.com/artist/$id"
            }
            ArtistArtworkSource.APPLE_MUSIC -> {
                if (uri.host !in setOf("music.apple.com", "itunes.apple.com")) return null
                val id = Regex("^/[a-z]{2}/artist/(?:[^/]+/)?(?:id)?([0-9]+)/?$")
                    .matchEntire(path)?.groupValues?.get(1) ?: return null
                "https://music.apple.com/us/artist/$id"
            }
        }
    }

    fun parse(source: ArtistArtworkSource, name: String, url: String, html: String): ArtistArtworkCandidate? {
        val profile = profileUrl(source, url) ?: return null
        val document = Jsoup.parse(html, profile)
        val canonical = document.selectFirst("meta[property=og:url]")?.attr("content")
            ?: document.selectFirst("link[rel=canonical]")?.attr("href")
        if (canonical != null && profileUrl(source, canonical) != profile) return null

        val artist = document.select("script[type=application/ld+json]")
            .flatMap { script ->
                val json = runCatching { Json.parseToJsonElement(script.data()) }.getOrNull()
                json?.let(::objects).orEmpty()
            }.firstOrNull {
                it.text("@type") in setOf("MusicGroup", "Person") && sameName(name, it.text("name"))
            }
        if (source == ArtistArtworkSource.APPLE_MUSIC && artist == null) return null
        if (source == ArtistArtworkSource.SPOTIFY && artist == null) {
            val title = document.selectFirst("meta[property=og:title]")?.attr("content")
                ?.removeSuffix(" | Spotify") ?: return null
            if (!sameName(name, title)) return null
        }

        val structuredImage = artist?.get("image")?.let {
            (it as? JsonPrimitive)?.contentOrNull ?: (it as? JsonObject)?.text("url")
        }
        val image = sequenceOf(
            structuredImage,
            document.selectFirst("meta[property=og:image:secure_url]")?.attr("content"),
            document.selectFirst("meta[property=og:image]")?.attr("content"),
        ).filterNotNull().firstOrNull { validImage(source, it) } ?: return null

        // Apple's structured artist portrait is square; social previews can be cropped banners.
        val imageUrl = imageUrl(source, image, 1200)
        return ArtistArtworkCandidate(source, name, profile, imageUrl)
    }

    internal fun imageUrl(source: ArtistArtworkSource, url: String, resolution: Int): String {
        require(resolution > 0)
        if (source != ArtistArtworkSource.APPLE_MUSIC || !validImage(source, url)) return url
        // Replace thumbnail dimensions and low-quality suffixes, keeping the artist asset and query intact.
        return url.replace(
            Regex("/\\d+x\\d+[^/?.]*\\.(jpg|jpeg|png|webp)(?=[?#]|$)", RegexOption.IGNORE_CASE),
            "/${resolution}x${resolution}bb.$1",
        )
    }

    private fun validImage(source: ArtistArtworkSource, url: String): Boolean {
        val uri = runCatching { URI(url) }.getOrNull() ?: return false
        if (uri.scheme != "https" || uri.userInfo != null || uri.port != -1) return false
        return when (source) {
            ArtistArtworkSource.YOUTUBE_MUSIC -> false
            ArtistArtworkSource.SPOTIFY -> uri.host == "i.scdn.co" && uri.path.startsWith("/image/")
            ArtistArtworkSource.APPLE_MUSIC -> uri.host?.endsWith(".mzstatic.com") == true &&
                uri.path.contains("ArtistImages")
        }
    }

    private fun objects(element: JsonElement): List<JsonObject> = when (element) {
        is JsonArray -> element.flatMap(::objects)
        is JsonObject -> listOf(element) + element["@graph"]?.let(::objects).orEmpty()
        else -> emptyList()
    }

    internal fun sameName(left: String, right: String?): Boolean {
        val normalized = ArtistNameMatcher.normalize(left)
        return normalized.isNotEmpty() && right != null && normalized == ArtistNameMatcher.normalize(right)
    }

    internal fun JsonObject.text(key: String): String? = (get(key) as? JsonPrimitive)?.contentOrNull
}
