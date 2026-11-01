package com.metrolist.music.data.remote

import com.metrolist.innertube.models.SongItem
import com.metrolist.innertube.models.WatchEndpoint.WatchEndpointMusicSupportedConfigs.WatchEndpointMusicConfig.Companion.MUSIC_VIDEO_TYPE_ATV
import kotlinx.serialization.Serializable
import org.jsoup.Jsoup
import java.text.Normalizer
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.ResolverStyle
import java.util.Locale

/** Every market uses the same source and parser; new countries need only their ISO code. */
@JvmInline
value class ChartMarket(val code: String) {
    init {
        require(code == "global" || code.matches(Regex("[a-z]{2}")))
    }

    val url: String get() = "https://kworb.net/spotify/country/${code}_daily.html"

    fun countryName(locale: Locale = Locale.getDefault()): String =
        Locale.Builder().setRegion(code.uppercase(Locale.ROOT)).build().getDisplayCountry(locale)

    companion object {
        val Global = ChartMarket("global")
        val Italy = ChartMarket("it")
        val UnitedStates = ChartMarket("us")
        val home = listOf(Global, Italy, UnitedStates)
    }
}

@Serializable
enum class ChartRankMovement { UP, DOWN, NEW }

@Serializable
data class ChartTrack(
    val rank: Int,
    val spotifyId: String,
    val title: String,
    val artists: List<String>,
    val streams: Long,
    val movement: ChartRankMovement? = null,
)

@Serializable
data class DailyChart(
    val marketCode: String,
    val date: String,
    val tracks: List<ChartTrack>,
    val checkedAt: Long,
    val sourceTrackCount: Int = DailyChartParser.TRACK_COUNT,
)

fun DailyChart.withRankMovements(previous: DailyChart?): DailyChart {
    val comparablePrevious = previous?.takeIf { it.marketCode == marketCode && it.date <= date }
    val previousRanks = comparablePrevious?.tracks?.associate { it.spotifyId to it.rank }
    val previousMovements = comparablePrevious?.tracks?.associate { it.spotifyId to it.movement }
    return copy(tracks = tracks.map { track ->
        val previousRank = previousRanks?.get(track.spotifyId)
        val movement = when {
            previousRanks == null -> null
            date == comparablePrevious.date && previousRank == track.rank -> previousMovements?.get(track.spotifyId)
            previousRank == null -> ChartRankMovement.NEW
            track.rank < previousRank -> ChartRankMovement.UP
            track.rank > previousRank -> ChartRankMovement.DOWN
            else -> null
        }
        track.copy(movement = movement)
    })
}

fun DailyChart.isHistorical(today: LocalDate = LocalDate.now(java.time.ZoneOffset.UTC)): Boolean =
    LocalDate.parse(date).isBefore(today.minusDays(7))

fun DailyChart.isUsableFor(market: ChartMarket): Boolean = runCatching {
    marketCode == market.code && date == LocalDate.parse(date).toString() && checkedAt >= 0 &&
        sourceTrackCount > 0 && tracks.map { it.rank } == (1..minOf(sourceTrackCount, DailyChartParser.TRACK_COUNT)).toList() &&
        tracks.distinctBy { it.spotifyId }.size == tracks.size &&
        tracks.all {
            it.spotifyId.matches(Regex("[A-Za-z0-9]{22}")) && it.title.isNotBlank() &&
                it.artists.isNotEmpty() && it.artists.all(String::isNotBlank) && it.streams >= 0
        }
}.getOrDefault(false)

fun DailyChart.isCurrentFor(market: ChartMarket, today: LocalDate = LocalDate.now(java.time.ZoneOffset.UTC)): Boolean =
    isUsableFor(market) && !isHistorical(today) && !LocalDate.parse(date).isAfter(today.plusDays(1))

object DailyChartParser {
    const val TRACK_COUNT = 100
    private val datePattern = Regex("Spotify Daily Chart - .+? - (\\d{4}/\\d{2}/\\d{2})")
    private val trackIdPattern = Regex("(?:^|/)track/([A-Za-z0-9]{22})\\.html$")

    fun parse(html: String, market: ChartMarket, checkedAt: Long): DailyChart {
        val document = Jsoup.parse(html, market.url)
        val rawDate = datePattern.find(document.select(".pagetitle").text())?.groupValues?.get(1)
            ?: error("Missing chart date")
        val date = LocalDate.parse(rawDate, DateTimeFormatter.ofPattern("uuuu/MM/dd").withResolverStyle(ResolverStyle.STRICT))
        val rows = document.select("table#spotifydaily tbody tr")
        val ranks = rows.map { it.select("td").firstOrNull()?.text()?.toIntOrNull() }
        require(ranks.isNotEmpty() && ranks == (1..rows.size).toList()) { "Incomplete chart" }
        if (rows.size < TRACK_COUNT) {
            require(market != ChartMarket.Global) { "Incomplete global chart" }
            // Small markets can publish fewer songs. Require the complete document to
            // distinguish those charts from a response cut off midway through the table.
            require(html.trimEnd().endsWith("</html>", ignoreCase = true)) { "Incomplete chart document" }
        }
        val tracks = rows.mapNotNull { row ->
            val cells = row.select("td")
            val rank = cells.firstOrNull()?.text()?.toIntOrNull() ?: return@mapNotNull null
            if (rank !in 1..TRACK_COUNT) return@mapNotNull null
            val cell = cells.getOrNull(2) ?: error("Missing track cell")
            val titleLink = cell.select("a[href]").firstOrNull {
                trackIdPattern.containsMatchIn(it.attr("href"))
            } ?: error("Missing track link")
            val id = trackIdPattern.find(titleLink.attr("href"))!!.groupValues[1]
            val artists = cell.select("a[href*=/artist/]").map { it.text().trim() }
            val title = titleLink.text().trim()
            val streams = cells.getOrNull(6)?.text()?.replace(",", "")?.toLongOrNull()
                ?: error("Missing stream count")
            require(title.isNotBlank() && artists.isNotEmpty() && artists.all { it.isNotBlank() } && streams >= 0)
            ChartTrack(rank, id, title, artists, streams)
        }.sortedBy { it.rank }
        // Never overwrite a good cached chart with a changed layout or a truncated response.
        require(tracks.map { it.rank } == (1..minOf(rows.size, TRACK_COUNT)).toList()) { "Incomplete chart" }
        require(tracks.distinctBy { it.spotifyId }.size == tracks.size) { "Duplicate chart tracks" }
        return DailyChart(market.code, date.toString(), tracks, checkedAt, sourceTrackCount = rows.size)
    }
}

object ChartTrackMatcher {
    private val diacritics = Regex("\\p{M}+")
    private val punctuation = Regex("[^\\p{L}\\p{N}]+")
    private val whitespace = Regex("\\s+")
    private val brackets = Regex("[\\[(]([^\\])]+)[\\])]")
    private val featured = Regex("(?i)\\s+(?:feat\\.?|ft\\.?|featuring)\\s+.*$")
    private val remaster = Regex("(?i)\\b(?:\\d{4}\\s+)?remaster(?:ed)?(?:\\s+\\d{4})?\\b")
    private val remixAlias = Regex("\\brmx\\b")
    private val repeatedRemix = Regex("\\b(remix)\\s+\\1\\b")
    private val bracketCredit = Regex("(?i)^(?:feat\\.?|ft\\.?|featuring|with|con|avec|mit|from)\\s")
    private val collaborationCredit = Regex("(?i)\\s+(?:con|with|feat\\.?|ft\\.?)\\s+(.+)$")
    private val versions = setOf(
        "live", "acoustic", "instrumental", "karaoke", "remix", "sped", "slowed", "cover", "demo",
        "unplugged", "version", "extended", "radio", "edit", "mix",
    )

    private fun normalized(text: String) = Normalizer.normalize(text, Normalizer.Form.NFD)
        .replace(diacritics, "").lowercase(Locale.ROOT)
        .replace(punctuation, " ").trim().replace(whitespace, " ")

    private fun canonicalTitle(text: String) = normalized(text).replace(remixAlias, "remix")
        .replace(repeatedRemix, "$1")

    private fun artistWords(name: String) = normalized(name).removePrefix("the ")
        .split(' ').filter { it.isNotEmpty() && it != "and" }.toSet()

    private fun titleParts(title: String, artists: List<String>): Pair<Set<String>, Set<String>> {
        var clean = title.replace(featured, "").replace(remaster, "")
        clean = clean.replace(brackets) { group ->
            if (bracketCredit.containsMatchIn(group.groupValues[1].trim())) ""
            else group.value
        }
        // Some catalogs append an unbracketed collaborator to the title. Strip it only
        // when the suffix actually names one of the credited artists.
        val collaboration = collaborationCredit.find(clean)
        if (collaboration != null && artists.any { artistWords(it) == artistWords(collaboration.groupValues[1]) }) {
            clean = clean.substring(0, collaboration.range.first)
        }
        val fullTitle = canonicalTitle(clean)
        val versionWords = fullTitle.split(' ').filter { it in versions }.toSet()
        clean = clean.replace(brackets) { group ->
            val text = group.groupValues[1].trim()
            if (normalized(text).split(' ').none { it in versions }) ""
            else group.value
        }
        // Preserve the full form as well: one catalog uses "Title - Subtitle", another
        // uses "Title (Subtitle)" for the same recording and soundtrack credit.
        return setOf(canonicalTitle(clean), fullTitle).filter(String::isNotEmpty).toSet() to versionWords
    }

    fun bestMatch(track: ChartTrack, candidates: List<SongItem>): SongItem? {
        val primaryArtist = track.artists.firstOrNull()?.let(::artistWords)?.takeIf { it.isNotEmpty() } ?: return null
        return candidates.mapNotNull { candidate ->
            // Unknown media types are deliberately excluded: these playlists promise audio tracks.
            if (candidate.isEpisode || candidate.musicVideoType != MUSIC_VIDEO_TYPE_ATV) null
            else {
                val creditedArtists = candidate.artists.map { it.name }
                // A joint Spotify artist credit may be split into separate YouTube artists.
                // Each participating name must match fully, excluding tribute-band names.
                val candidateArtists = creditedArtists.map(::artistWords)
                    .filter { it.isNotEmpty() && primaryArtist.containsAll(it) }.flatten().toSet()
                if (candidateArtists != primaryArtist) return@mapNotNull null
                val allArtists = track.artists + creditedArtists
                val (sourceTitles, sourceVersions) = titleParts(track.title, allArtists)
                val (candidateTitles, candidateVersions) = titleParts(candidate.title, allArtists)
                if (sourceTitles.intersect(candidateTitles).isEmpty() || sourceVersions != candidateVersions) null
                else candidate to (if (normalized(track.title) == normalized(candidate.title)) 2 else 1)
            }
        }.maxByOrNull { it.second }?.first
    }
}
