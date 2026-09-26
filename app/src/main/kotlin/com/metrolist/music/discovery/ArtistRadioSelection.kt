package com.metrolist.music.discovery

import com.metrolist.music.models.HeroCarouselItem
import kotlin.random.Random

internal fun selectArtistRadios(
    candidates: List<HeroCarouselItem.ArtistRadio>,
    previousArtistIds: Set<String>,
    seed: Long,
    previousFirstArtistId: String? = null,
): List<HeroCarouselItem.ArtistRadio> {
    val unique = candidates
        .filter { it.artistId.startsWith("UC") && it.artistName.isNotBlank() }
        .groupBy { it.artistId }
        .map { (_, duplicates) -> duplicates.firstOrNull { !it.coverUrl.isNullOrBlank() } ?: duplicates.first() }
        .shuffled(Random(seed))
    val (fresh, repeated) = unique.partition { it.artistId !in previousArtistIds }
    val selected = (fresh + repeated).take(10).toMutableList()
    if (selected.size > 1 && selected.first().artistId == previousFirstArtistId) {
        val first = selected[0]
        selected[0] = selected[1]
        selected[1] = first
    }
    return selected
}
