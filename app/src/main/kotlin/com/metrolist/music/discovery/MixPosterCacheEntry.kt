package com.metrolist.music.discovery

import kotlinx.serialization.Serializable

@Serializable
internal data class MixPosterCacheEntry(
    val mixId: String,
    val title: String,
    val mood: String,
    val firstTrackId: String? = null,
    val artistId: String? = null,
    val photoUrl: String? = null,
    val generatedAt: Long = 0,
    val seed: Long,
    val imageName: String,
    val size: Int,
    // Missing versions belong to the old format; never default them to the current renderer.
    val rendererVersion: Int = 0,
    val photoSource: MixPosterPhotoSource = MixPosterPhotoSource.ARTIST,
    val firstArtistId: String? = null,
    val sourceImageName: String? = null,
    val photoFetchedAt: Long = 0,
)

@Serializable
internal enum class MixPosterPhotoSource { ARTIST, TRACK, PLAYLIST, GRAPHIC }
