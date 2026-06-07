/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.discovery

import com.metrolist.music.db.MusicDatabase
import com.metrolist.music.db.entities.PlaylistEntity
import com.metrolist.music.models.DiscoveryItem
import com.metrolist.music.models.PlaylistType
import kotlinx.coroutines.flow.first
import kotlin.random.Random

class DiscoveryCarouselGenerator(
    private val database: MusicDatabase,
) {
    suspend fun generate(seed: Long = System.currentTimeMillis()): List<DiscoveryItem> {
        val random = Random(seed)
        val oneYearAgo = System.currentTimeMillis() - 365L * 24 * 60 * 60 * 1000

        val pool = mutableListOf<DiscoveryItem>()

        // Liked songs (always included when non-empty)
        val likedCount = database.likedSongsCount().first()
        if (likedCount > 0) {
            pool.add(
                DiscoveryItem.PlaylistCard(
                    id = PlaylistEntity.LIKED_PLAYLIST_ID,
                    title = "Liked Songs",
                    subtitle = "$likedCount songs",
                    type = PlaylistType.LIKED_SONGS,
                    coverUrl = null,
                )
            )
        }

        // Top artists (up to 3)
        val topArtists = database.mostPlayedArtists(fromTimeStamp = oneYearAgo, limit = 5).first()
        topArtists.shuffled(random).take(3).forEach { artist ->
            pool.add(
                DiscoveryItem.ArtistStation(
                    artistId = artist.id,
                    artistName = artist.artist.name,
                    coverUrl = artist.artist.thumbnailUrl,
                )
            )
        }

        // Personal playlists (1-2)
        val personalPlaylists = database.editablePlaylistsByNameAsc().first()
            .filter { it.id != PlaylistEntity.LIKED_PLAYLIST_ID }
            .shuffled(random)
            .take(2)
        personalPlaylists.forEach { playlist ->
            pool.add(
                DiscoveryItem.PlaylistCard(
                    id = playlist.id,
                    title = playlist.title,
                    subtitle = "${playlist.songCount} songs",
                    type = PlaylistType.PERSONAL,
                    coverUrl = playlist.thumbnails.firstOrNull(),
                )
            )
        }

        // Most played album
        val topAlbums = database.mostPlayedAlbums(fromTimeStamp = oneYearAgo, limit = 5).first()
        val topAlbumIds = topAlbums.map { it.id }.toSet()
        topAlbums.shuffled(random).firstOrNull()?.let { album ->
            pool.add(
                DiscoveryItem.AlbumCard(
                    albumId = album.id,
                    albumTitle = album.title,
                    artistName = album.artists.firstOrNull()?.name ?: "",
                    coverUrl = album.thumbnailUrl,
                    isPearl = false,
                )
            )
        }

        // Pearl: low play time, in library, not in top played albums
        database.albumsByPlayTimeAsc().first()
            .filter { it.album.inLibrary != null && it.id !in topAlbumIds }
            .firstOrNull()?.let { album ->
                pool.add(
                    DiscoveryItem.AlbumCard(
                        albumId = album.id,
                        albumTitle = album.title,
                        artistName = album.artists.firstOrNull()?.name ?: "",
                        coverUrl = album.thumbnailUrl,
                        isPearl = true,
                    )
                )
            }

        return interleave(pool.shuffled(random), random).take(10)
    }

    private fun interleave(items: List<DiscoveryItem>, @Suppress("UNUSED_PARAMETER") random: Random): List<DiscoveryItem> {
        if (items.size <= 1) return items
        val result = mutableListOf<DiscoveryItem>()
        val remaining = items.toMutableList()
        while (remaining.isNotEmpty()) {
            val lastType = result.lastOrNull()?.let { it::class }
            val candidate = remaining.firstOrNull { it::class != lastType } ?: remaining.first()
            remaining.remove(candidate)
            result.add(candidate)
        }
        return result
    }
}
