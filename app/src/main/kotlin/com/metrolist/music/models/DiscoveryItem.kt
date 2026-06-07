/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.models

sealed class DiscoveryItem {
    data class PlaylistCard(
        val id: String,
        val title: String,
        val subtitle: String,
        val type: PlaylistType,
        val coverUrl: String?,
    ) : DiscoveryItem()

    data class ArtistStation(
        val artistId: String,
        val artistName: String,
        val coverUrl: String?,
    ) : DiscoveryItem()

    data class AlbumCard(
        val albumId: String,
        val albumTitle: String,
        val artistName: String,
        val coverUrl: String?,
        val isPearl: Boolean,
    ) : DiscoveryItem()
}

enum class PlaylistType {
    LIKED_SONGS,
    PERSONAL,
}
