package com.metrolist.music.data.remote

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import org.jsoup.Jsoup
import java.net.URI
import java.text.Normalizer
import java.util.Locale

@Serializable
internal data class GenreArtworkCandidate(
    val title: String,
    val imageUrl: String,
    val pageUrl: String,
    val nativeSide: Int,
    val editorial: Boolean = false,
) {
    val identity: String get() = GenreArtworkParser.imageIdentity(imageUrl).orEmpty()

    fun imageAt(side: Int): String = imageUrl.replace("{w}", side.toString())
        .replace("{h}", side.toString()).replace("{f}", "jpg")
        .replace("1000x1000-", "${minOf(side, 1000)}x${minOf(side, 1000)}-")
}

internal object GenreArtworkParser {
    const val MINIMUM_SIDE = 1000
    private val marks = Regex("\\p{M}+")
    private val separators = Regex("[^\\p{L}\\p{N}]+")
    private val artistCollections = Regex(
        "\\b(?:inspired by|influences|tribute|discography|artist|concert set list|presents|feat|featuring)\\b",
    )

    fun normalize(value: String): String = Normalizer.normalize(value, Normalizer.Form.NFKD)
        .replace(marks, "").lowercase(Locale.ROOT).replace(separators, " ").trim()

    fun matchesTitle(genre: String, title: String): Boolean {
        val name = normalize(genre)
        val text = normalize(title)
        return name.isNotBlank() && genreNames(name).any { " $it " in " $text " } &&
            !artistCollections.containsMatchIn(text) && !title.contains(Regex("['’]s\\b"))
    }

    private fun genreNames(name: String): List<String> = when (name) {
        "rap" -> listOf("rap", "hip hop", "hiphop")
        "lo fi" -> listOf("lo fi", "lofi")
        "r b" -> listOf("r b", "r and b", "rhythm and blues")
        "drum and bass" -> listOf("drum and bass", "drum bass", "dnb", "d b")
        else -> listOf(name)
    }

    fun referencesArtist(title: String, artists: List<String>, genre: String): Boolean {
        val text = " ${normalize(title)} "
        val genreNames = genreNames(normalize(genre))
        return artists.any {
            val name = normalize(it)
            // Some catalog artists are literally named "Deep", "House" or "Hip Hop".
            // Their names inside the genre itself do not make a generic playlist artist-specific.
            name.isNotBlank() && genreNames.none { " $name " in " $it " } && " $name " in text
        }
    }

    fun imageIdentity(url: String): String? {
        val uri = runCatching { URI(url.replace("{w}", "1000").replace("{h}", "1000").replace("{f}", "jpg")) }
            .getOrNull() ?: return null
        if (uri.scheme != "https" || uri.userInfo != null || (uri.port != -1 && uri.port != 443)) return null
        val host = uri.host.orEmpty().lowercase(Locale.ROOT)
        if (host.endsWith(".mzstatic.com") && uri.path.startsWith("/image/thumb/")) {
            val asset = uri.path.removePrefix("/image/thumb/").substringBeforeLast('/')
            if (Regex("(?:^|/)(?:Music|AMCArtistImages|ArtistImages)[0-9]*(?:/|$)").containsMatchIn(asset) ||
                uri.path.contains("SC.FP")) return null
            return "apple:$asset"
        }
        if ((host == "dzcdn.net" || host.endsWith(".dzcdn.net")) && uri.path.startsWith("/images/playlist/")) {
            val hash = uri.path.removePrefix("/images/playlist/").substringBefore('/')
            return hash.takeIf { it.matches(Regex("[a-f0-9]{32}")) }?.let { "deezer:$it" }
        }
        return null
    }

    fun appleSearch(genre: String, html: String): List<GenreArtworkCandidate> {
        val page = applePage(html, "SearchResultsPageIntent") ?: return emptyList()
        val items = page.objects("sections").flatMap { it.objects("items") }
        val artists = items.filter { it.obj("contentDescriptor")?.text("kind") == "artist" }
            .mapNotNull { it.title() }
        return items.mapNotNull { item ->
            val descriptor = item.obj("contentDescriptor") ?: return@mapNotNull null
            if (descriptor.text("kind") != "playlist") return@mapNotNull null
            val title = item.title() ?: return@mapNotNull null
            if (!matchesTitle(genre, title) || referencesArtist(title, artists, genre)) return@mapNotNull null
            val artwork = item.obj("artwork")?.obj("dictionary") ?: return@mapNotNull null
            val side = minOf(artwork.int("width") ?: 0, artwork.int("height") ?: 0)
            val image = artwork.text("url") ?: return@mapNotNull null
            val url = descriptor.text("url") ?: return@mapNotNull null
            if (side < MINIMUM_SIDE || imageIdentity(image) == null || !isPlaylistPage(url)) return@mapNotNull null
            val curator = item.text("subtitle") ?: item.objects("subtitleLinks").firstOrNull()?.text("title").orEmpty()
            GenreArtworkCandidate(title, image, url, side, "Apple Music" in curator)
        }.distinctBy { it.identity }
    }

    fun applePlaylistMatches(genre: String, candidate: GenreArtworkCandidate, html: String): Boolean {
        val page = applePage(html, "PlaylistDetailPageIntent") ?: return false
        val sections = page.objects("sections")
        val header = sections.filter { it.text("itemKind") == "containerDetailHeaderLockup" }
            .flatMap { it.objects("items") }.singleOrNull() ?: return false
        if (header.obj("contentDescriptor")?.text("url") != candidate.pageUrl ||
            header.title() != candidate.title || !matchesTitle(genre, candidate.title)) return false
        val artists = sections.filter { it.text("itemKind") == "trackLockup" }
            .flatMap { it.objects("items") }.mapNotNull { it.text("artistName") }
        return isMultiArtistGenre(genre, candidate.title, artists)
    }

    fun deezerSearch(genre: String, body: String): List<GenreArtworkCandidate> {
        val json = Json.parseToJsonElement(body) as? JsonObject ?: return emptyList()
        return json.objects("data").mapNotNull { item ->
            val title = item.text("title") ?: return@mapNotNull null
            val id = item.text("id")?.takeIf { it.matches(Regex("[0-9]+")) } ?: return@mapNotNull null
            val image = item.text("picture_xl") ?: return@mapNotNull null
            if (!matchesTitle(genre, title) || imageIdentity(image) == null) return@mapNotNull null
            GenreArtworkCandidate(title, image, "https://api.deezer.com/playlist/$id", 1000,
                item.obj("user")?.text("name")?.contains("Deezer", ignoreCase = true) == true)
        }.distinctBy { it.identity }
    }

    fun deezerPlaylistMatches(genre: String, candidate: GenreArtworkCandidate, body: String): Boolean {
        val json = Json.parseToJsonElement(body) as? JsonObject ?: return false
        if (json.text("title") != candidate.title ||
            "https://api.deezer.com/playlist/${json.text("id")}" != candidate.pageUrl) return false
        val artists = json.obj("tracks")?.objects("data").orEmpty()
            .mapNotNull { it.obj("artist")?.text("name") }
        return isMultiArtistGenre(genre, candidate.title, artists)
    }

    private fun isMultiArtistGenre(genre: String, title: String, artists: List<String>): Boolean {
        val names = artists.map(::normalize).filter(String::isNotBlank)
        return names.distinct().size >= 3 &&
            (names.groupingBy { it }.eachCount().values.maxOrNull() ?: 0).toFloat() / names.size < 0.75f &&
            !referencesArtist(title, artists, genre)
    }

    fun isPlaylistPage(url: String): Boolean {
        val uri = runCatching { URI(url) }.getOrNull() ?: return false
        return uri.scheme == "https" && uri.userInfo == null && (uri.port == -1 || uri.port == 443) &&
            ((uri.host == "music.apple.com" &&
                uri.path.matches(Regex("/[a-z]{2}/playlist/[^/]+/pl\\.[a-zA-Z0-9]+"))) ||
                (uri.host == "api.deezer.com" &&
                    uri.path.matches(Regex("/playlist/[0-9]+"))))
    }

    private fun applePage(html: String, kind: String): JsonObject? {
        val server = Jsoup.parse(html).selectFirst("script#serialized-server-data")?.data() ?: return null
        val json = Json.parseToJsonElement(server) as? JsonObject ?: return null
        return json.objects("data").singleOrNull { it.obj("intent")?.text("\$kind") == kind }?.obj("data")
    }

    private fun JsonObject.text(field: String): String? = (this[field] as? JsonPrimitive)?.contentOrNull
    private fun JsonObject.int(field: String): Int? = (this[field] as? JsonPrimitive)?.intOrNull
    private fun JsonObject.obj(field: String): JsonObject? = this[field] as? JsonObject
    private fun JsonObject.objects(field: String): List<JsonObject> =
        (this[field] as? JsonArray)?.filterIsInstance<JsonObject>().orEmpty()
    private fun JsonObject.title(): String? = text("title") ?: objects("titleLinks").firstOrNull()?.text("title")
}

/** A small luminance fingerprint also catches the same photo uploaded under different URLs. */
internal fun genreArtworkFingerprint(pixels: IntArray): Long {
    require(pixels.size == 9 * 8)
    fun luminance(pixel: Int): Int = ((pixel ushr 16) and 255) * 299 +
        ((pixel ushr 8) and 255) * 587 + (pixel and 255) * 114
    var fingerprint = 0L
    for (y in 0 until 8) for (x in 0 until 8) {
        if (luminance(pixels[y * 9 + x]) > luminance(pixels[y * 9 + x + 1])) {
            fingerprint = fingerprint or (1L shl (y * 8 + x))
        }
    }
    return fingerprint
}

internal fun genreArtworkIsDuplicate(identity: String, fingerprint: Long, otherIdentity: String, otherFingerprint: Long): Boolean =
    identity == otherIdentity || java.lang.Long.bitCount(fingerprint xor otherFingerprint) <= 4
