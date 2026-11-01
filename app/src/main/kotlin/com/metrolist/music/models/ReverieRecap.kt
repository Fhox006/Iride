package com.metrolist.music.models

import com.metrolist.music.db.entities.CategoryStats
import com.metrolist.music.db.entities.EventWithSong
import java.time.YearMonth
import java.util.Locale

data class ReverieGenre(val name: String, val milliseconds: Long, val songCount: Int, val soundtrack: MediaMetadata)

data class ReverieRank(
    val id: String,
    val title: String,
    val subtitle: String,
    val artwork: String?,
    val milliseconds: Long,
    val plays: Int,
    val soundtrack: MediaMetadata,
)

data class ReverieRecap(
    val month: YearMonth,
    val milliseconds: Long,
    val plays: Int,
    val songs: List<ReverieRank>,
    val artists: List<ReverieRank>,
    val albums: List<ReverieRank>,
    val categories: List<CategoryStats>,
    val dailyMilliseconds: List<Long>,
    val genres: List<ReverieGenre> = emptyList(),
) {
    val minutes: Long get() = milliseconds / 60_000
    val activeDays: Int get() = dailyMilliseconds.count { it > 0 }
    val peakDay: Int get() = dailyMilliseconds.indices.maxByOrNull { dailyMilliseconds[it] }?.plus(1) ?: 1
    val eligible: Boolean get() = hasEnoughReverieData(songs.size, milliseconds, activeDays)
    val chapters: List<ReverieChapter> get() = reverieChapterOrder(month, buildList {
        add(ReverieChapter.OVERTURE)
        add(ReverieChapter.TIME)
        add(ReverieChapter.RHYTHM)
        if (artists.isNotEmpty()) add(ReverieChapter.ARTIST)
        add(ReverieChapter.SONG)
        if (genres.isNotEmpty()) add(ReverieChapter.GENRES)
        if (categories.isNotEmpty()) add(ReverieChapter.COLOURS)
        if (albums.isNotEmpty()) add(ReverieChapter.ALBUM)
        add(ReverieChapter.POSTER)
    })

    private val soundtracks: Map<ReverieChapter, MediaMetadata> by lazy {
        val available = songs.map { it.soundtrack }.distinctBy { it.id }
        val spotlights = setOf(ReverieChapter.SONG, ReverieChapter.ARTIST, ReverieChapter.ALBUM)
        val used = mutableSetOf<String>()
        val artistUses = mutableMapOf<String, Int>()
        fun MediaMetadata.artistKeys(): Set<String> = artists.map {
            it.id?.takeIf(String::isNotBlank) ?: it.name.trim().lowercase(Locale.ROOT)
        }.filter(String::isNotBlank).toSet()
        fun preferred(chapter: ReverieChapter): List<MediaMetadata> = when (chapter) {
            ReverieChapter.SONG -> available.take(1)
            ReverieChapter.ARTIST -> available.filter { track -> track.artists.any { it.id == artists.firstOrNull()?.id } }
            ReverieChapter.ALBUM -> available.filter { it.album?.id == albums.firstOrNull()?.id }
            ReverieChapter.GENRES -> listOfNotNull(genres.firstOrNull()?.soundtrack)
            else -> emptyList()
        }
        buildMap {
            fun choose(chapter: ReverieChapter): MediaMetadata? {
                val candidates = available.filter { it.id !in used }
                val preferred = preferred(chapter)
                val previous = chapters.getOrNull(chapters.indexOf(chapter) - 1)?.let { get(it)?.artistKeys() }.orEmpty()
                val selection = preferred.firstOrNull { it.id !in used }
                    ?: preferred.firstOrNull().takeIf { chapter in spotlights }
                    ?: candidates.minWithOrNull(compareBy<MediaMetadata> { track -> track.artistKeys().count { it in previous } }
                        .thenBy { track -> track.artistKeys().sumOf { artistUses[it] ?: 0 } })
                    ?: available.firstOrNull()
                selection?.let {
                    used.add(it.id)
                    it.artistKeys().forEach { key -> artistUses[key] = (artistUses[key] ?: 0) + 1 }
                }
                return selection
            }
            if (available.size >= chapters.size) {
                listOf(ReverieChapter.SONG, ReverieChapter.ARTIST, ReverieChapter.ALBUM).filter { it in chapters }.forEach { chapter ->
                    choose(chapter)?.let { put(chapter, it) }
                }
            }
            var pairSong: MediaMetadata? = null
            var pairSize = 0
            chapters.forEach { chapter ->
                if (chapter in spotlights) {
                    if (!containsKey(chapter)) choose(chapter)?.let { put(chapter, it) }
                    pairSong = null
                    pairSize = 0
                } else {
                    if (pairSize == 0) pairSong = choose(chapter)
                    pairSong?.let { put(chapter, it) }
                    pairSize = (pairSize + 1) % 2
                }
            }
        }
    }

    fun soundtrack(chapter: ReverieChapter): MediaMetadata? = soundtracks[chapter]

    fun backgroundArtworkId(chapter: ReverieChapter): String? = when (chapter) {
        ReverieChapter.OVERTURE -> songs.firstOrNull()?.id
        ReverieChapter.POSTER -> albums.firstOrNull()?.id ?: songs.firstOrNull()?.id
        ReverieChapter.ARTIST -> artists.firstOrNull()?.id
        ReverieChapter.ALBUM -> albums.firstOrNull()?.id
        ReverieChapter.SONG -> songs.firstOrNull()?.id
        else -> soundtrack(chapter)?.id ?: songs.firstOrNull()?.id
    }
}

enum class ReverieChapter { OVERTURE, TIME, RHYTHM, ARTIST, SONG, GENRES, COLOURS, ALBUM, POSTER }

fun rankReverieGenres(songs: List<ReverieRank>, assignments: Map<String, List<String>>): List<ReverieGenre> =
    songs.flatMap { song ->
        assignments[song.id].orEmpty().map(String::trim).filter(String::isNotBlank)
            .distinctBy { it.lowercase(Locale.ROOT) }.map { it to song }
    }.groupBy { it.first.lowercase(Locale.ROOT) }.values.map { matches ->
        val representative = matches.maxBy { it.second.milliseconds }
        ReverieGenre(representative.first, matches.sumOf { it.second.milliseconds }, matches.size, representative.second.soundtrack)
    }.sortedWith(compareByDescending<ReverieGenre> { it.milliseconds }.thenBy { it.name }).take(4)

/** Snapshot only recorded listening. Preview playback never enters the history database. */
fun createReverieRecap(
    month: YearMonth,
    events: List<EventWithSong>,
    categories: List<CategoryStats> = emptyList(),
    hideVideoSongs: Boolean = false,
    hideExplicit: Boolean = false,
): ReverieRecap {
    val listening = events.filter {
        YearMonth.from(it.event.timestamp) == month && it.event.playTime > 0 &&
            !it.song.song.isEpisode && (!hideVideoSongs || !it.song.song.isVideo) &&
            (!hideExplicit || !it.song.song.explicit)
    }
    val ranking = compareByDescending<ReverieRank> { it.milliseconds }
        .thenByDescending { it.plays }.thenBy { it.id }
    fun rank(id: String, title: String, subtitle: String, artwork: String?, rows: List<EventWithSong>): ReverieRank {
        val representative = rows.groupBy { it.song.id }.values
            .sortedWith(compareByDescending<List<EventWithSong>> { group -> group.sumOf { it.event.playTime } }
                .thenBy { it.first().song.id }).first().first().song
        return ReverieRank(id, title, subtitle, artwork, rows.sumOf { it.event.playTime }, rows.size, representative.toMediaMetadata())
    }
    val songs = listening.groupBy { it.song.id }.map { (id, rows) ->
        val song = rows.first().song
        rank(id, song.title, song.orderedArtists.joinToString { it.name }, song.thumbnailUrl, rows)
    }.sortedWith(ranking)
    val artists = listening.flatMap { row -> row.song.orderedArtists.distinctBy { it.id }.map { it to row } }
        .groupBy { it.first.id }.map { (id, pairs) ->
            val artist = pairs.first().first
            rank(id, artist.name, "", artist.thumbnailUrl, pairs.map { it.second })
        }.sortedWith(ranking)
    val albums = listening.filter { it.song.album != null }.groupBy { it.song.album!!.id }.map { (id, rows) ->
        val song = rows.first().song
        val album = song.album!!
        val artwork = album.thumbnailUrl?.takeIf(String::isNotBlank)
            ?: rows.firstNotNullOfOrNull { it.song.thumbnailUrl?.takeIf(String::isNotBlank) }
        rank(id, album.title, song.orderedArtists.joinToString { it.name }, artwork, rows)
    }.sortedWith(ranking)
    val days = LongArray(month.lengthOfMonth())
    listening.forEach { days[it.event.timestamp.dayOfMonth - 1] += it.event.playTime }
    return ReverieRecap(
        month, listening.sumOf { it.event.playTime }, listening.size, songs, artists, albums,
        // Category membership comes from YouTube Music playlists; it is not an inferred audio mood.
        categories.filter { (it.timeListened ?: 0) > 0 }.sortedByDescending { it.timeListened }, days.toList(),
    )
}

const val REVERIE_CHAPTER_MILLIS = 10_000L
const val REVERIE_DISSOLVE_MILLIS = 800L

fun reverieChapterAt(positionMillis: Long, chapterCount: Int): Int =
    (positionMillis.coerceAtLeast(0) / REVERIE_CHAPTER_MILLIS)
        .coerceAtMost((chapterCount - 1).coerceAtLeast(0).toLong()).toInt()
