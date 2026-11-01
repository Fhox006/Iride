/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.models

import kotlinx.serialization.Serializable

@Serializable
sealed class HeroCarouselItem {
    @Serializable
    data class NewRelease(
        val albumId: String,
        val title: String,
        val artistName: String,
        val coverUrl: String?,
    ) : HeroCarouselItem()

    @Serializable
    data class InRotation(
        val albumId: String,
        val title: String,
        val artistName: String,
        val coverUrl: String?,
    ) : HeroCarouselItem()

    @Serializable
    data class RecommendedAlbum(
        val albumId: String,
        val title: String,
        val artistName: String,
        val coverUrl: String?,
    ) : HeroCarouselItem()

    @Serializable
    data class TrendingArtist(
        val artistId: String,
        val artistName: String,
        val coverUrl: String?,
    ) : HeroCarouselItem()

    @Serializable
    data class ArtistRadio(
        val artistId: String,
        val artistName: String,
        val coverUrl: String?,
    ) : HeroCarouselItem()

    @Serializable
    data class GenreNewRelease(
        val albumId: String,
        val title: String,
        val artistName: String,
        val coverUrl: String?,
        val genreLabel: String,
    ) : HeroCarouselItem()
}

fun HeroCarouselItem.stableKey(): String = when (this) {
    is HeroCarouselItem.NewRelease -> "hero_new_$albumId"
    is HeroCarouselItem.InRotation -> "hero_rotation_$albumId"
    is HeroCarouselItem.RecommendedAlbum -> "hero_reco_$albumId"
    is HeroCarouselItem.TrendingArtist -> "hero_trending_$artistId"
    is HeroCarouselItem.ArtistRadio -> "hero_radio_$artistId"
    is HeroCarouselItem.GenreNewRelease -> "hero_genre_$albumId"
}
