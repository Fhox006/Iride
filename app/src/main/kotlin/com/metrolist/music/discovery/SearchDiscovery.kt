package com.metrolist.music.discovery

import com.metrolist.innertube.models.ArtistItem
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
}
