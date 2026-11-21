package com.metrolist.music.discovery

import com.metrolist.innertube.models.Album
import com.metrolist.innertube.models.AlbumItem
import com.metrolist.innertube.models.Artist
import com.metrolist.innertube.models.ArtistItem
import com.metrolist.innertube.models.SongItem
import com.metrolist.innertube.models.WatchEndpoint
import com.metrolist.innertube.models.YTItem
import com.metrolist.music.db.MusicDatabase
import com.metrolist.music.db.entities.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn

/** Following an artist is a search signal even without a track in the recent-song window. */
internal fun MusicDatabase.searchPersonalCatalog(): Flow<List<YTItem>> = searchPersonalCatalog(
    playlistDraftSongs(limit = 240), quickPicks(), artistsBookmarkedByCreateDateAsc(),
)

internal fun searchPersonalCatalog(
    familiar: Flow<List<Song>>,
    picks: Flow<List<Song>>,
    followed: Flow<List<com.metrolist.music.db.entities.Artist>>,
): Flow<List<YTItem>> = combine(familiar, picks, followed,
) { familiar, picks, followed ->
    val songs = (familiar + picks).distinctBy { it.id }
        .filter { !it.song.isLocal && !it.song.isVideo && !it.song.isEpisode }
    val artists = (followed.map { it.artist } + songs.flatMap { it.orderedArtists })
        .distinctBy { it.id }.filter { it.id.startsWith("UC") && !it.isPodcastChannel }
        .map { ArtistItem(it.id, it.name, it.thumbnailUrl, channelId = it.channelId, shuffleEndpoint = null, radioEndpoint = null) }
    val albums = songs.mapNotNull { song ->
        song.album?.takeIf { !it.isLocal }?.let { album ->
            AlbumItem(
                browseId = album.id, playlistId = album.playlistId.orEmpty(), title = album.title,
                artists = song.orderedArtists.map { Artist(it.name, it.id) }, year = album.year,
                thumbnail = album.thumbnailUrl ?: song.thumbnailUrl.orEmpty(), explicit = album.explicit,
            )
        }
    }.distinctBy { it.id }
    songs.map { it.toSearchSong() } + artists + albums
}.flowOn(Dispatchers.Default)

private fun Song.toSearchSong() = SongItem(
    id = id, title = title, artists = orderedArtists.map { Artist(it.name, it.id) },
    album = song.albumId?.let { Album(song.albumName.orEmpty(), it) },
    duration = song.duration.takeIf { it > 0 }, thumbnail = thumbnailUrl.orEmpty(),
    explicit = song.explicit, endpoint = WatchEndpoint(videoId = id),
)
