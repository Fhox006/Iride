package com.metrolist.music.data.remote

import com.metrolist.innertube.YouTube
import com.metrolist.innertube.utils.morePreciseAlbumReleaseDate
import com.metrolist.innertube.utils.needsPreciseAlbumReleaseDate
import com.metrolist.innertube.utils.parseAlbumReleaseDate
import com.metrolist.music.db.MusicDatabase
import com.metrolist.music.db.entities.Album
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AlbumReleaseDateRepository @Inject constructor(
    private val database: MusicDatabase,
    private val musicBrainzRepository: MusicBrainzRepository,
) {
    suspend fun enrich(album: Album) {
        val previousDate = album.album.releaseDate
        val year = album.album.year?.takeIf { it in 1..9999 }
        if (!needsPreciseAlbumReleaseDate(previousDate, year)) return
        val artist = album.artists.firstOrNull()?.name?.takeIf { it.isNotBlank() } ?: return
        val lookupYear = year ?: parseAlbumReleaseDate(previousDate)?.year
        val candidate = morePreciseAlbumReleaseDate(
            previousDate,
            YouTube.appleMusicAlbumReleaseDate(album.title, artist, lookupYear),
            lookupYear,
        ) ?: morePreciseAlbumReleaseDate(
            previousDate,
            musicBrainzRepository.getAlbumReleaseDate(album.title, artist, lookupYear),
            lookupYear,
        ) ?: return

        val current = database.album(album.id).first() ?: return
        // Metadata may have changed while the catalog request was in flight.
        if (current.title != album.title || current.album.year != album.album.year ||
            current.artists.firstOrNull()?.name != artist
        ) return
        val date = morePreciseAlbumReleaseDate(current.album.releaseDate, candidate, lookupYear) ?: return
        database.query {
            updateAlbumReleaseDate(current.id, date, current.album.releaseDate, current.album.year)
        }
    }
}
