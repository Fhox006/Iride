package com.metrolist.music.discovery

import com.metrolist.innertube.models.AlbumItem
import com.metrolist.innertube.models.ArtistItem
import com.metrolist.innertube.models.PlaylistItem
import com.metrolist.innertube.models.SongItem
import com.metrolist.music.data.remote.ChartTrack
import com.metrolist.music.data.remote.ChartTrackMatcher
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import java.text.Normalizer
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.Locale
import kotlin.random.Random

@Serializable
enum class NewReleaseKind { ALBUM, EP, SINGLE }

/** Catalog IDs are deliberately separate from YouTube IDs; resolution happens on interaction. */
@Serializable
data class NewRelease(
    val id: String,
    val title: String,
    val artistName: String,
    val artistId: String? = null,
    val artworkUrl: String,
    val releaseDate: String,
    val kind: NewReleaseKind,
    val genre: String? = null,
    val country: String,
    val followed: Boolean = false,
    val explicit: Boolean = false,
    val artistArtworkUrl: String? = null,
    val appleArtistId: String? = null,
    val catalogUrl: String? = null,
    val trackCount: Int? = null,
    val collectionId: String? = null,
    val isTrack: Boolean = false,
    val collectionTitle: String? = null,
    val sourceArtistNames: Set<String> = emptySet(),
    val playable: Boolean = false,
)

data class NewReleaseSection(val id: String, val subject: String, val releases: List<NewRelease>)

data class NewReleasePage(
    val hero: List<NewRelease> = emptyList(),
    val followedAlbums: List<NewRelease> = emptyList(),
    val followedSingles: List<NewRelease> = emptyList(),
    val albums: List<NewRelease> = emptyList(),
    val songs: List<NewRelease> = emptyList(),
    val sections: List<NewReleaseSection> = emptyList(),
    val playlists: List<PlaylistItem> = emptyList(),
    val artists: List<ArtistItem> = emptyList(),
    val newAlbums: List<AlbumItem> = emptyList(),
    val checkedAt: Long = 0,
) {
    val hasContent: Boolean get() = hero.isNotEmpty() || followedAlbums.isNotEmpty() ||
        followedSingles.isNotEmpty() || albums.isNotEmpty() || songs.isNotEmpty() ||
        sections.any { it.releases.isNotEmpty() } || playlists.isNotEmpty() || artists.isNotEmpty() || newAlbums.isNotEmpty()
}

internal data class NewReleaseProfile(
    val artistWeights: Map<String, Double> = emptyMap(),
    val genreWeights: Map<String, Double> = emptyMap(),
    val mood: MixMood? = null,
    val localChartArtists: Set<String> = emptySet(),
)

/** Public feeds and lookup responses share one strict date/artwork validation path. */
internal object NewReleaseCatalog {
    private val numericId = Regex("[0-9]+")
    private val releaseSuffix = Regex("(?i)\\s*[-–:]\\s*(single|ep)\\s*$")
    private val edition = Regex("(?i)\\s*[\\[(][^\\])]*\\b(?:deluxe|expanded|extended edition|bonus|edition|anniversary|explicit|clean)\\b[^\\])]*[\\])]")
    private val editionSuffix = Regex("(?i)\\s*[-–:]\\s*(?:deluxe|expanded|bonus|special|standard|clean|explicit)(?:\\s+(?:edition|version|tracks))?\\s*$")
    private val recordingVersion = Regex("(?i)\\b(?:live|acoustic|remix|remaster(?:ed)?|instrumental|demo|re-recorded|rerecorded)\\b")
    private val bareEditionSuffix = Regex("(?i)\\s+(?:deluxe|expanded)(?:\\s+(?:edition|version))?\\s*$")

    fun albumFamilyTitle(title: String): String = title.replace(releaseSuffix, "").replace(edition) { match ->
        if (recordingVersion.containsMatchIn(match.value)) match.value else ""
    }
        .replace(editionSuffix, "").replace(bareEditionSuffix, "").trim()

    fun isAlbumEdition(title: String): Boolean =
        albumFamilyTitle(title) != title.replace(releaseSuffix, "").trim()

    fun standardAlbums(edition: JsonObject, candidates: List<JsonObject>): List<JsonObject> {
        val title = edition.text("collectionName") ?: return emptyList()
        val artist = normalize(edition.text("artistName").orEmpty())
        val released = edition.text("releaseDate")?.take(10) ?: return emptyList()
        return candidates.filter { candidate ->
            val candidateTitle = candidate.text("collectionName") ?: return@filter false
            val date = candidate.text("releaseDate")?.take(10) ?: return@filter false
            candidate.text("wrapperType") == "collection" &&
                candidate.text("collectionId") != edition.text("collectionId") &&
                normalize(candidate.text("artistName").orEmpty()) == artist &&
                !isAlbumEdition(candidateTitle) &&
                normalize(albumFamilyTitle(candidateTitle)) == normalize(albumFamilyTitle(title)) && date <= released
        }.distinctBy { it.text("collectionId") }
    }

    /** Album packaging may change song IDs and dates, but must not turn existing recordings into new tracks. */
    fun reissuedTrackIds(editionTracks: List<JsonObject>, standardTracks: List<JsonObject>): Set<String> {
        fun key(track: JsonObject): String = normalize(track.text("artistName").orEmpty()) + "|" +
            normalize(albumFamilyTitle(track.text("trackName").orEmpty()))
        val originals = standardTracks.filter { it.text("kind") == "song" }.map(::key).toSet()
        return editionTracks.filter { it.text("kind") == "song" && key(it) in originals }
            .mapNotNull { it.text("trackId")?.let { id -> "apple:song:$id" } }.toSet()
    }

    fun familyKey(release: NewRelease): String = normalize(release.artistName) + "|" +
        normalize(if (release.isTrack) release.title else albumFamilyTitle(release.title)) + "|" + release.isTrack

    fun artistNames(release: NewRelease): Set<String> = (release.sourceArtistNames + release.artistName +
        release.artistName.split(Regex("\\s*(?:,|&| x )\\s*"))).map(::normalize).filter(String::isNotBlank).toSet()

    // Publish dated catalog entries immediately; prefer an edition already resolved on YouTube.
    fun bestEdition(variants: List<NewRelease>): NewRelease? = variants
        .sortedWith(compareByDescending<NewRelease> { it.playable }.thenByDescending { it.trackCount ?: 0 }
            .thenByDescending { it.title != albumFamilyTitle(it.title) }.thenByDescending { it.explicit }).firstOrNull()

    fun date(raw: String?, today: LocalDate): String? {
        if (raw.isNullOrBlank()) return null
        val parsed = runCatching {
            if (raw.length == 10) LocalDate.parse(raw)
            else Instant.parse(raw).atOffset(ZoneOffset.UTC).toLocalDate()
        }.getOrNull() ?: return null
        return parsed.takeIf { !it.isBefore(NewReleaseSchedule.weekStart(today)) && !it.isAfter(today) }?.toString()
    }

    fun isFresh(release: NewRelease, today: LocalDate): Boolean = date(release.releaseDate, today) != null

    /** A changing chart or partial catalog must not erase releases already found this week. */
    fun mergeCached(previous: List<NewRelease>, incoming: List<NewRelease>, today: LocalDate): List<NewRelease> =
        (incoming + previous).distinctBy { it.id }.filter { isFresh(it, today) }

    fun collectionsMissingTracks(
        collections: List<JsonObject>,
        tracks: List<JsonObject>,
        today: LocalDate,
    ): List<JsonObject> {
        val counts = tracks.filter { it.text("wrapperType") == "track" }
            .distinctBy { it.text("trackId") }.groupingBy { it.text("collectionId") }.eachCount()
        return collections.filter { collection ->
            val id = collection.text("collectionId")
            val expected = collection.text("trackCount")?.toIntOrNull()
            collection.text("wrapperType") == "collection" && id != null &&
                date(collection.text("releaseDate"), today) != null &&
                (expected == null || (counts[id] ?: 0) < expected)
        }.distinctBy { it.text("collectionId") }
    }

    fun recordingKey(item: JsonObject): String = normalize(item.text("artistName").orEmpty()) + "|" +
        normalize((item.text("trackName") ?: item.text("collectionName").orEmpty()).replace(releaseSuffix, "")) +
        "|" + (item.text("kind") == "song")

    fun recordingKey(release: NewRelease): String = normalize(release.artistName) + "|" +
        normalize(release.title) + "|" + release.isTrack

    fun originalDates(items: List<JsonObject>): Map<String, String> = items
        .filter { it.text("wrapperType") == "collection" || it.text("kind") == "song" }
        .groupBy(::recordingKey).mapNotNull { (key, editions) ->
            editions.mapNotNull { item ->
                val raw = item.text("releaseDate") ?: return@mapNotNull null
                runCatching {
                    if (raw.length == 10) LocalDate.parse(raw)
                    else Instant.parse(raw).atOffset(ZoneOffset.UTC).toLocalDate()
                }.getOrNull()
            }.minOrNull()?.let { key to it.toString() }
        }.toMap()

    fun parseCatalog(items: List<JsonObject>, country: String, today: LocalDate): List<NewRelease> {
        val originals = originalDates(items)
        return items.mapNotNull { item ->
            val release = parse(item, country, today) ?: return@mapNotNull null
            if (date(originals[recordingKey(item)], today) == null) null else release
        }
    }

    /** Lookup returns each requested artist followed by their catalog, including collaborators. */
    fun lookupGroups(items: List<JsonObject>, requestedIds: Set<String>): Map<String, List<JsonObject>> {
        val groups = mutableMapOf<String, MutableList<JsonObject>>()
        var current: String? = null
        items.forEach { item ->
            if (item.text("wrapperType") == "artist") {
                current = item.text("artistId")?.takeIf { it in requestedIds }
            }
            current?.let { groups.getOrPut(it) { mutableListOf() }.add(item) }
        }
        return groups
    }

    fun kind(title: String, trackCount: Int? = null): NewReleaseKind = when {
        releaseSuffix.find(title)?.groupValues?.get(1).equals("single", true) -> NewReleaseKind.SINGLE
        releaseSuffix.find(title)?.groupValues?.get(1).equals("ep", true) -> NewReleaseKind.EP
        trackCount != null && trackCount <= 3 -> NewReleaseKind.SINGLE
        trackCount != null && trackCount <= 6 -> NewReleaseKind.EP
        else -> NewReleaseKind.ALBUM
    }

    fun parse(item: JsonObject, country: String, today: LocalDate, feed: Boolean = false): NewRelease? {
        if (feed && item.text("kind") !in setOf("songs", "albums")) return null
        val track = item.text("kind") in setOf("song", "songs")
        if (!feed && item.text("wrapperType") != "collection" && !track) return null
        val rawId = item.text(if (feed) "id" else if (track) "trackId" else "collectionId")
            ?.takeIf { numericId.matches(it) } ?: return null
        val title = item.text(if (feed) "name" else if (track) "trackName" else "collectionName")
            ?.takeIf(String::isNotBlank) ?: return null
        val artist = item.text("artistName")?.takeIf(String::isNotBlank) ?: return null
        val date = date(item.text("releaseDate"), today) ?: return null
        val artwork = item.text("artworkUrl100")?.takeIf { it.startsWith("https://") } ?: return null
        val count = item.text("trackCount")?.toIntOrNull()
        val genre = if (feed) item.objects("genres").firstNotNullOfOrNull {
            genreNames[it.text("genreId")]
        } else genreNames[item.text("primaryGenreId")] ?: item.text("primaryGenreName")
        val rating = listOf("collectionExplicitness", "trackExplicitness", "contentAdvisoryRating")
            .mapNotNull { item.text(it)?.lowercase(Locale.ROOT) }
        return NewRelease(
            id = "apple:${if (track) "song" else "album"}:$rawId",
            title = if (track) title else title.replace(releaseSuffix, "").trim(),
            artistName = artist,
            artworkUrl = artwork.replace(Regex("/\\d+x\\d+bb\\."), "/1200x1200bb."),
            releaseDate = date,
            kind = if (track) item.text("collectionName")?.let { kind(it, count) } ?: NewReleaseKind.SINGLE
                else kind(title, count),
            genre = genre,
            country = country.lowercase(Locale.ROOT),
            explicit = rating.any { it == "explicit" || it == "explict" },
            appleArtistId = item.text("artistId")?.takeIf { numericId.matches(it) },
            catalogUrl = item.text(if (feed) "url" else if (track) "trackViewUrl" else "collectionViewUrl"),
            trackCount = count,
            collectionId = if (track) item.text("collectionId") else rawId,
            isTrack = track,
            collectionTitle = if (track) item.text("collectionName") else title,
        )
    }

    fun normalize(value: String): String = Normalizer.normalize(value, Normalizer.Form.NFD)
        .replace(Regex("\\p{M}+"), "").lowercase(Locale.ROOT)
        .replace(Regex("[^\\p{L}\\p{N}]+"), " ").trim().replace(Regex("\\s+"), " ")

    fun genreFamily(value: String?): String {
        val genre = normalize(value.orEmpty())
        return when {
            listOf("rap", "hip hop", "hiphop", "trap", "drill", "phonk").any { it in genre } -> "rap"
            listOf("r b", "soul", "funk").any { it in genre } -> "soul"
            listOf("electronic", "dance", "house", "techno", "edm").any { it in genre } -> "electronic"
            listOf("ambient", "chill", "new age").any { it in genre } -> "ambient"
            listOf("rock", "alternative", "indie", "punk", "metal").any { it in genre } -> "rock"
            listOf("classical", "soundtrack", "orchestral").any { it in genre } -> "classical"
            "pop" in genre -> "pop"
            "jazz" in genre -> "jazz"
            "latin" in genre || "reggaeton" in genre -> "latin"
            "country" in genre || "folk" in genre || "acoustic" in genre -> "folk"
            else -> genre
        }
    }

    fun songMatch(release: NewRelease, candidates: List<SongItem>): SongItem? = ChartTrackMatcher.bestMatch(
        ChartTrack(1, "0000000000000000000000", release.title,
            release.artistName.split(Regex("\\s*(?:,|&| x )\\s*")).filter(String::isNotBlank), 0),
        candidates.filterNot { it.isEpisode },
    )

    fun albumMatch(release: NewRelease, candidates: List<AlbumItem>): AlbumItem? = candidates.firstOrNull { item ->
        normalize(item.title.replace(releaseSuffix, "")) == normalize(release.title) &&
            item.artists.orEmpty().any { artist -> normalize(artist.name) == normalize(release.artistName) ||
                release.artistName.split(Regex("\\s*(?:,|&| x )\\s*")).any { normalize(it) == normalize(artist.name) } ||
                release.artistId != null && artist.id == release.artistId } &&
            (item.year == null || item.year == release.releaseDate.take(4).toIntOrNull())
    }

    fun JsonObject.text(key: String): String? = (get(key) as? JsonPrimitive)?.contentOrNull
    fun JsonObject.objects(key: String): List<JsonObject> = (get(key) as? JsonArray)?.filterIsInstance<JsonObject>().orEmpty()

    private val genreNames = mapOf(
        "2" to "Blues", "3" to "Comedy", "4" to "Children's Music", "5" to "Classical",
        "6" to "Country", "7" to "Electronic", "8" to "Holiday", "9" to "Opera", "10" to "Singer/Songwriter",
        "11" to "Jazz", "12" to "Latin", "13" to "New Age", "14" to "Pop", "15" to "R&B/Soul",
        "16" to "Soundtrack", "17" to "Dance", "18" to "Hip-Hop/Rap", "19" to "World", "20" to "Alternative",
        "21" to "Rock", "22" to "Christian", "24" to "Reggae", "27" to "J-Pop", "51" to "K-Pop",
    )
}

internal object NewReleaseSelection {
    fun orderedFollowedTracks(tracks: List<NewRelease>): List<NewRelease> = tracks.sortedWith(
        compareBy<NewRelease> {
            when (it.kind) {
                NewReleaseKind.SINGLE -> 0
                NewReleaseKind.EP -> 1
                NewReleaseKind.ALBUM -> 2
            }
        }.thenBy { it.releaseDate }.thenBy { it.id },
    )

    fun discoveryAlbums(candidates: List<AlbumItem>, shown: List<NewRelease>, hideExplicit: Boolean): List<AlbumItem> {
        val claimed = shown.filterNot { it.isTrack }.flatMap { release ->
            val title = NewReleaseCatalog.normalize(NewReleaseCatalog.albumFamilyTitle(release.title))
            NewReleaseCatalog.artistNames(release).map { "$it|$title" }
        }.toMutableSet()
        return candidates.distinctBy { it.id }.filter { album ->
            if (hideExplicit && album.explicit) return@filter false
            val title = NewReleaseCatalog.normalize(NewReleaseCatalog.albumFamilyTitle(album.title))
            val keys = album.artists.orEmpty().map { "${NewReleaseCatalog.normalize(it.name)}|$title" }
                .ifEmpty { listOf(album.id) }
            if (keys.any { it in claimed }) false else {
                claimed.addAll(keys)
                true
            }
        }
    }

    fun page(
        releases: List<NewRelease>,
        trendingIds: Set<String>,
        profile: NewReleaseProfile,
        country: String,
        hideExplicit: Boolean,
        today: LocalDate,
        visit: Long,
        now: Long,
        checkedAt: Long,
    ): NewReleasePage {
        val fresh = releases.filter { NewReleaseCatalog.isFresh(it, today) && (!hideExplicit || !it.explicit) }
        val albums = fresh.filter { !it.isTrack && it.kind != NewReleaseKind.SINGLE }
            .groupBy(NewReleaseCatalog::familyKey).mapNotNull { (_, variants) ->
                NewReleaseCatalog.bestEdition(variants)?.copy(followed = variants.any { it.followed },
                    sourceArtistNames = variants.flatMap { it.sourceArtistNames }.toSet())
            }
        val tracks = fresh.filter { it.isTrack }.sortedByDescending { it.followed }
            .distinctBy(NewReleaseCatalog::familyKey)
        fun score(item: NewRelease): Double {
            val family = NewReleaseCatalog.genreFamily(item.genre)
            val artist = NewReleaseCatalog.artistNames(item).maxOfOrNull { profile.artistWeights[it] ?: 0.0 } ?: 0.0
            val moodAffinity = when (profile.mood) {
                MixMood.CHILL, MixMood.SLEEP, MixMood.FOCUS -> family in setOf("ambient", "jazz", "classical", "folk", "soul")
                MixMood.ENERGY, MixMood.WORKOUT, MixMood.PARTY -> family in setOf("rap", "electronic", "rock", "latin")
                MixMood.ROMANTIC, MixMood.SAD -> family in setOf("soul", "pop", "folk")
                MixMood.FEEL_GOOD -> family in setOf("pop", "electronic", "latin")
                null -> false
            }
            return artist + (profile.genreWeights[family] ?: 0.0) + (if (item.followed) 20.0 else 0.0) +
                (if (moodAffinity) 2.0 else 0.0)
        }
        fun ranked(items: List<NewRelease>, seed: Long): List<NewRelease> =
            items.shuffled(Random(seed)).sortedByDescending(::score)
        fun diverse(items: List<NewRelease>, limit: Int): List<NewRelease> {
            val counts = mutableMapOf<String, Int>()
            return items.filter { item ->
                val artist = NewReleaseCatalog.normalize(item.artistName)
                val count = counts[artist] ?: 0
                if (count >= 3) false else { counts[artist] = count + 1; true }
            }.take(limit)
        }
        val heroSeed = now / (12 * 60 * 60 * 1_000L)
        val familiar = albums.filter { it.followed ||
            NewReleaseCatalog.artistNames(it).any { name -> (profile.artistWeights[name] ?: 0.0) > 0.0 } }
        val discovery = albums.filterNot { it in familiar }
        val familiarPicks = ranked(familiar, heroSeed).distinctBy { NewReleaseCatalog.normalize(it.artistName) }.take(4)
        val discoveryPicks = ranked(discovery, heroSeed).distinctBy { NewReleaseCatalog.normalize(it.artistName) }.take(4)
        val hero = (familiarPicks + discoveryPicks + ranked(albums, heroSeed))
            .distinctBy { NewReleaseCatalog.normalize(it.artistName) }.take(8).shuffled(Random(heroSeed))
        // Spotlight is a preview; followed albums must remain complete in their own shelf.
        val followedAlbums = albums.filter { it.followed }
            .sortedByDescending(NewRelease::releaseDate)
        val collectionKinds = fresh.filterNot { it.isTrack }.filter { it.collectionId != null }
            .associate { it.collectionId to it.kind }
        val followedSingles = orderedFollowedTracks(tracks.filter { it.followed }.map { track ->
            track.copy(kind = collectionKinds[track.collectionId]
                ?: track.kind.takeUnless { it == NewReleaseKind.SINGLE }
                ?: track.collectionTitle?.let { NewReleaseCatalog.kind(it, track.trackCount) }
                ?: track.kind)
        })
        val claimed = (hero + followedAlbums + followedSingles).map(NewReleaseCatalog::familyKey).toMutableSet()
        fun unclaimed(items: List<NewRelease>): List<NewRelease> = items.filter { NewReleaseCatalog.familyKey(it) !in claimed }
        fun claim(items: List<NewRelease>): List<NewRelease> {
            claimed += items.map(NewReleaseCatalog::familyKey)
            return items
        }
        val personalizedAlbums = claim(diverse(ranked(unclaimed(albums.filter { !it.followed && it.kind == NewReleaseKind.ALBUM }), visit), 16))
        val rankedTracks = ranked(unclaimed(tracks), visit)
        val personal = diverse(rankedTracks.filter { score(it) > 0.0 }, 12)
        val personalIds = personal.map(NewRelease::id).toSet()
        val trends = diverse(unclaimed(tracks).filter { it.id in trendingIds && it.id !in personalIds }.shuffled(Random(visit + 1)), 12)
        val songs = claim((0 until maxOf(personal.size, trends.size)).flatMap { index ->
            listOfNotNull(personal.getOrNull(index), trends.getOrNull(index))
        }.let { selected -> (selected + diverse(rankedTracks.filter { it.id !in selected.map(NewRelease::id) }, 24)).take(24) })
        val shelves = mutableListOf<NewReleaseSection>()
        fun shelf(id: String, subject: String, items: List<NewRelease>): Unit {
            val selected = diverse(ranked(unclaimed(items), visit + id.hashCode()), 14)
            if (selected.size >= 2) {
                shelves += NewReleaseSection(id, subject, claim(selected))
            }
        }
        val countryName = Locale.Builder().setRegion(country.uppercase(Locale.ROOT)).build().getDisplayCountry(Locale.ENGLISH)
        fun local(item: NewRelease): Boolean = NewReleaseCatalog.artistNames(item).any { it in profile.localChartArtists }
        shelf("eps", "", albums.filter { it.kind == NewReleaseKind.EP })
        shelf("local", countryName, albums.filter(::local))
        val genres = albums.filter { !it.genre.isNullOrBlank() }.groupBy { NewReleaseCatalog.genreFamily(it.genre) }
            .entries.sortedByDescending { profile.genreWeights[it.key] ?: 0.0 }
        genres.take(3).forEach { (family, items) ->
            shelf("genre_$family", items.first().genre.orEmpty(), items)
            shelf("songs_$family", items.first().genre.orEmpty(), tracks.filter { NewReleaseCatalog.genreFamily(it.genre) == family })
        }
        shelf("discovery", "", albums.filter { !it.followed && score(it) > 0 })
        shelf("world", "", albums.filterNot(::local))
        return NewReleasePage(
            hero = hero,
            followedAlbums = followedAlbums,
            followedSingles = followedSingles,
            albums = personalizedAlbums,
            songs = songs,
            sections = shelves.take(8),
            checkedAt = checkedAt,
        )
    }
}
