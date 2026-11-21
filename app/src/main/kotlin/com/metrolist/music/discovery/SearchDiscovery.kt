package com.metrolist.music.discovery

import com.metrolist.innertube.models.ArtistItem
import com.metrolist.innertube.models.PlaylistItem
import com.metrolist.innertube.models.Artist
import com.metrolist.music.utils.TitleFeaturingParser
import com.metrolist.innertube.models.SongItem
import com.metrolist.innertube.models.YTItem
import java.text.Normalizer
import java.util.Locale

internal object SearchDiscovery {
    const val MAX_SONGS = 40
    const val MAX_RECOMMENDATIONS = 20
    const val MAX_ARTISTS = 12
    private val combiningMarks = Regex("\\p{M}+")
    private val separators = Regex("[^\\p{L}\\p{N}]+")

    private fun normalize(text: String): String =
        Normalizer.normalize(text, Normalizer.Form.NFKD)
            .replace(combiningMarks, "")
            .lowercase(Locale.ROOT)
            .replace(separators, " ")
            .trim()

    fun cleanTitle(title: String): String = TitleFeaturingParser.extract(title)?.first ?: title

    fun songKey(song: SongItem): String =
        "${normalize(cleanTitle(song.title))}:${normalize(song.artists.firstOrNull()?.name.orEmpty())}"

    fun credits(seed: SongItem, matches: List<SongItem>, known: List<ArtistItem>): List<Artist> {
        val names = TitleFeaturingParser.extract(seed.title)?.second.orEmpty()
        val matching = matches.filter { songKey(it) == songKey(seed) }.flatMap { it.artists }
        val familiar = known.filter { artist -> names.any { normalize(it) == normalize(artist.title) } }
            .map { Artist(it.title, it.id) }
        return (seed.artists + matching + familiar).filter { it.id?.startsWith("UC") == true }
            .distinctBy { it.id }
    }

    fun missingCredits(seed: SongItem, credits: List<Artist>): List<String> =
        TitleFeaturingParser.extract(seed.title)?.second.orEmpty().filter { name ->
            credits.none { normalize(it.name) == normalize(name) }
        }.distinct()

    fun recognizeSong(query: String, songs: List<SongItem>, topItem: YTItem?): SongItem? {
        val normalizedQuery = normalize(query)
        if (normalizedQuery.isBlank()) return null
        // An artist query such as "Anna" must not become a song seed when a track
        // happens to share the artist's name.
        if (topItem is ArtistItem && normalize(topItem.title) == normalizedQuery) return null
        val tracks = songs.filter { !it.isVideoSong && !it.isEpisode }.distinctBy { it.id }.take(4)
        tracks.firstOrNull { song ->
            normalize(song.title) == normalizedQuery || song.artists.any { artist ->
                normalize("${song.title} ${artist.name}") == normalizedQuery ||
                    normalize("${artist.name} ${song.title}") == normalizedQuery
            }
        }?.let { return it }
        if (topItem is SongItem && normalizedQuery.length >= 5 && normalizedQuery.contains(' ')) {
            tracks.singleOrNull { song ->
                normalize(song.title).startsWith("$normalizedQuery ") &&
                    normalize(song.title) == normalize(topItem.title)
            }?.let { return it }
        }
        // A broad search's first result alone does not establish song intent.
        return null
    }

    fun songs(results: List<SongItem>, seed: SongItem?, recommendations: List<SongItem>): List<SongItem> {
        val matches = (listOfNotNull(seed) + results)
            .filter { !it.isVideoSong && !it.isEpisode }
            .distinctBy { it.id }
        return (matches.take(4) + recommendations + matches.drop(4))
            .filter { !it.isVideoSong && !it.isEpisode }
            .distinctBy { it.id }
            .take(MAX_SONGS)
    }

    fun duplicatesFirstSongs(topItem: YTItem?, songs: List<SongItem>): Boolean =
        topItem is SongItem && songs.take(4).any { it.id == topItem.id }

    fun similarArtists(artists: List<ArtistItem>, creditedIds: Set<String>): List<ArtistItem> =
        artists.filter { !it.isProfile && it.id !in creditedIds }
            .distinctBy { it.id }
            .take(MAX_ARTISTS)

    fun artists(credited: List<ArtistItem>, related: List<ArtistItem>, results: List<ArtistItem>): List<ArtistItem> =
        (credited + related + results).filter { !it.isProfile }.distinctBy { it.id }.take(MAX_ARTISTS)

    fun playlists(related: List<PlaylistItem>, results: List<PlaylistItem>): List<PlaylistItem> =
        (related + results).distinctBy { it.id }.take(20)
}
