package com.metrolist.music.ui.screens.search

import com.metrolist.music.data.remote.GenreArtworkParser

private val genreArtworkNames = SearchGenrePersonalization.catalog.associate {
    GenreArtworkParser.normalize(it.name) to it.name
} + mapOf(
    "hip hop" to "Rap", "hiphop" to "Rap", "rap hip hop" to "Rap",
    "lofi" to "Lo-Fi", "r and b" to "R&B", "rhythm and blues" to "R&B",
    "dnb" to "Drum and Bass", "d b" to "Drum and Bass", "drum bass" to "Drum and Bass",
    "alternative rock" to "Alternative Rock", "musica classica" to "Classical",
    "classica" to "Classical", "elettronica" to "Electronic", "colonna sonora" to "Soundtrack",
)

/** Exact genre names keep an artist or song containing words like "rock" out of this pipeline. */
internal fun searchGenreArtworkName(query: String): String? = genreArtworkNames[GenreArtworkParser.normalize(query)]
