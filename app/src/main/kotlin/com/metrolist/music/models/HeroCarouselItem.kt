/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.models

sealed class HeroCarouselItem {
    data class NewRelease(
        val albumId: String,
        val title: String,
        val artistName: String,
        val coverUrl: String?,
    ) : HeroCarouselItem()

    data class ForYou(
        val playlistId: String,
        val title: String,
        val subtitle: String,
        val coverUrl: String?,
        val isLocal: Boolean,
    ) : HeroCarouselItem()

    data class Mood(
        val playlistId: String,
        val moodName: String,
        val coverUrl: String?,
    ) : HeroCarouselItem()

    data class MoreFromArtist(
        val artistId: String,
        val artistName: String,
        val coverUrl: String?,
    ) : HeroCarouselItem()

    data class ArtistRadio(
        val artistId: String,
        val artistName: String,
        val coverUrl: String?,
    ) : HeroCarouselItem()
}

fun HeroCarouselItem.stableKey(): String = when (this) {
    is HeroCarouselItem.NewRelease -> "hero_new_$albumId"
    is HeroCarouselItem.ForYou -> "hero_foryou_$playlistId"
    is HeroCarouselItem.Mood -> "hero_mood_$playlistId"
    is HeroCarouselItem.MoreFromArtist -> "hero_more_$artistId"
    is HeroCarouselItem.ArtistRadio -> "hero_radio_$artistId"
}
