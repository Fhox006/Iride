package com.metrolist.music.ui.menu

internal enum class SongMenuArtistLabel {
    Artist,
    Artists,
    Collaborators,
}

internal data class SongMenuNavigation(
    val artistIds: List<String>,
    val artistLabel: SongMenuArtistLabel,
    val showAlbum: Boolean,
)

internal fun songMenuNavigation(
    artistIds: List<String>,
    songAlbumId: String?,
    currentArtistId: String?,
    currentAlbumId: String?,
    albumArtistIds: Set<String>,
): SongMenuNavigation {
    val distinctArtists = artistIds.distinct()
    val knownArtists = buildSet {
        currentArtistId?.let(::add)
        if (currentAlbumId != null) {
            if (albumArtistIds.isNotEmpty()) addAll(albumArtistIds)
            else distinctArtists.firstOrNull()?.let(::add)
        }
    }
    val visibleArtists = distinctArtists.filterNot { it in knownArtists }
    return SongMenuNavigation(
        artistIds = visibleArtists,
        artistLabel = when {
            visibleArtists.size < distinctArtists.size -> SongMenuArtistLabel.Collaborators
            visibleArtists.size > 1 -> SongMenuArtistLabel.Artists
            else -> SongMenuArtistLabel.Artist
        },
        showAlbum = songAlbumId != null && currentAlbumId == null,
    )
}
