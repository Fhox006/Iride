package com.metrolist.music.ui.screens.search

import com.metrolist.innertube.models.AlbumItem
import com.metrolist.innertube.models.ArtistItem
import com.metrolist.innertube.models.EpisodeItem
import com.metrolist.innertube.models.PodcastItem
import com.metrolist.innertube.models.SongItem
import com.metrolist.innertube.models.YTItem
import com.metrolist.music.discovery.SearchDiscovery

internal fun YTItem.isSearchMusic(): Boolean = when (this) {
    is PodcastItem, is EpisodeItem -> false
    is SongItem -> !isVideoSong && !isEpisode
    is ArtistItem -> !isProfile
    else -> true
}

/** Personal matches lead; server corrections retain their original order as the fallback. */
internal fun rankSearchSuggestions(query: String, personal: List<YTItem>, remote: List<YTItem>): List<YTItem> {
    val candidates = (personal + remote).filter { it.isSearchMusic() }.distinctBy { it.id }
    if (query.isBlank()) return emptyList()
    val familiarIds = personal.mapTo(hashSetOf()) { it.id }
    val matches = candidates.mapNotNull { item ->
        val credits = when (item) {
            is SongItem -> item.artists.joinToString(" ") { it.name }
            is AlbumItem -> item.artists.orEmpty().joinToString(" ") { it.name }
            else -> ""
        }
        searchPredictionScore(query, item.title, credits)?.let { score ->
            // A recognized artist (including a typo) belongs before tracks matching its credits.
            val priority = if (item is ArtistItem) {
                if (item.id in familiarIds) {
                    if (score == 0) -2 else if (score == 1) -1 else 1
                } else if (score <= 1) 1 else 4
            } else if (item is AlbumItem && score > 0) score + 2 else score
            Triple(item, priority, if (item.id in familiarIds) 0 else 1)
        }
    }.sortedWith(compareBy({ it.second }, { it.third }, { if (it.first is ArtistItem) 0 else 1 }))
        .map { it.first }
    val artistIntent = matches.filterIsInstance<ArtistItem>().firstOrNull { it.id in familiarIds }
    val focusedMatches = matches.filter { item ->
        // Do not combine "Kid" from one guest with a fuzzy "Yugi" from another credit.
        if (item is ArtistItem && artistIntent != null) item.id == artistIntent.id
        else item !is SongItem || artistIntent == null || searchPredictionScore(query, item.title) != null ||
            item.artists.any { it.id == artistIntent.id || it.name.equals(artistIntent.title, ignoreCase = true) }
    }
    val ranked = (if (focusedMatches.any { it.id in familiarIds }) focusedMatches + remote else remote)
        .filter { it.isSearchMusic() }.distinctBy { it.id }
    val firstSong = ranked.firstOrNull() as? SongItem
    val credit = firstSong?.artists?.firstOrNull { it.id?.startsWith("UC") == true }
    val artist = credit?.let { credited ->
        candidates.filterIsInstance<ArtistItem>().firstOrNull { it.id == credited.id }
            ?: ArtistItem(requireNotNull(credited.id), credited.name, null, shuffleEndpoint = null, radioEndpoint = null)
    }
    // Offer the author for a clear title match, without forcing an artist into every query.
    return if (artist != null && firstSong?.title?.trim()?.equals(query.trim(), ignoreCase = true) == true) {
        (listOfNotNull(firstSong, artist) + ranked).distinctBy { it.id }.take(5)
    } else ranked.take(5)
}

/** One vertical group only; selected playback stays first. */
internal fun searchTopItems(pinned: List<YTItem>, promoted: List<YTItem>, results: List<YTItem>): List<YTItem> =
    (pinned + promoted + results)
        .filter { it.isSearchMusic() }
        .distinctBy { it.id }
        .take(5)

/** Playback establishes intent, even when the draft was an ambiguous fragment of the title. */
internal fun searchSongContextItems(
    seed: SongItem,
    artists: List<ArtistItem>,
    familiar: List<SongItem>,
    matches: List<SongItem>,
    recommendations: List<SongItem>,
): List<YTItem> {
    fun sharesArtist(song: SongItem) = song.artists.any { credit ->
        seed.artists.any { author ->
            (author.id != null && author.id == credit.id) || author.name.equals(credit.name, ignoreCase = true)
        }
    }
    val authors = artists.filter { artist -> seed.artists.any { it.id == artist.id || it.name.equals(artist.title, ignoreCase = true) } }
    return (listOf(seed) + authors.take(1) + familiar.filter(::sharesArtist) + matches.filter(::sharesArtist) + recommendations)
        .filter { it.isSearchMusic() }
        .distinctBy { item ->
            if (item is SongItem) {
                "song:${SearchDiscovery.songKey(item)}"
            } else item.id
        }.take(5)
}
