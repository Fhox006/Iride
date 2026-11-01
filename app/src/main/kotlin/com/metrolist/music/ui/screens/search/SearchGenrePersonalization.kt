/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.screens.search

import androidx.datastore.preferences.core.stringPreferencesKey
import java.util.Locale

internal data class SearchGenre(
    val name: String,
    val family: String,
)

internal data class SearchGenreListening(
    val tags: List<String>,
    val playTimeMs: Long,
)

internal object SearchGenrePersonalization {
    const val MAX_LISTENING_SEEDS = 40
    const val TAP_POINTS = 10f

    val listeningScoresKey = stringPreferencesKey("search_genre_listening_scores")
    val tapScoresKey = stringPreferencesKey("search_genre_tap_scores")

    val catalog = listOf(
        "Pop" to "pop", "Rap" to "rap", "Rock" to "rock", "R&B" to "soul",
        "Electronic" to "electronic", "Dance" to "electronic", "EDM" to "electronic",
        "Alternative" to "rock", "Indie" to "indie", "Metal" to "metal", "Punk" to "punk",
        "Jazz" to "jazz", "Blues" to "blues", "Soul" to "soul", "Funk" to "funk",
        "Reggae" to "reggae", "Reggaeton" to "latin", "Latin" to "latin", "Country" to "country",
        "Folk" to "folk", "Classical" to "classical", "Ambient" to "ambient", "Soundtrack" to "classical",
        "Acoustic" to "folk", "Afrobeat" to "world", "Afrobeats" to "world", "Amapiano" to "world",
        "Bachata" to "latin", "Balearic" to "electronic", "Big Band" to "jazz", "Bossa Nova" to "latin",
        "Cumbia" to "latin", "Salsa" to "latin", "Samba" to "latin", "Tango" to "latin",
        "Bluegrass" to "country", "Gospel" to "soul", "Opera" to "classical", "New Age" to "ambient",
        "Experimental" to "alternative", "Avant-garde" to "alternative", "Spoken Word" to "alternative",
        "Acid House" to "house", "Ambient Techno" to "techno", "Bass House" to "house",
        "Breakbeat" to "electronic", "Breakcore" to "electronic", "Chillout" to "ambient",
        "Chillwave" to "electronic", "Dance-Pop" to "pop", "Dark Ambient" to "ambient",
        "Deep House" to "house", "Disco" to "dance", "Drum and Bass" to "electronic",
        "Dub" to "reggae", "Dubstep" to "electronic", "Electro" to "electronic",
        "Electroclash" to "electronic", "Electronica" to "electronic", "Eurodance" to "dance",
        "Folktronica" to "electronic", "Freestyle" to "dance", "Future Bass" to "electronic",
        "Gabber" to "electronic", "Garage" to "electronic", "Hardcore" to "electronic",
        "Hardstyle" to "electronic", "House" to "house", "IDM" to "electronic", "Italo Disco" to "dance",
        "Jersey Club" to "electronic", "Lo-Fi" to "electronic", "Minimal Techno" to "techno",
        "Progressive House" to "house", "Psytrance" to "electronic", "Tech House" to "house",
        "Techno" to "techno", "Trance" to "electronic", "Vaporwave" to "electronic",
        "Alternative Metal" to "metal", "Black Metal" to "metal", "Death Metal" to "metal",
        "Deathcore" to "metal", "Doom Metal" to "metal", "Grindcore" to "metal",
        "Hard Rock" to "rock", "Heavy Metal" to "metal", "Melodic Death Metal" to "metal",
        "Metalcore" to "metal", "Nu Metal" to "metal", "Power Metal" to "metal",
        "Progressive Metal" to "metal", "Speed Metal" to "metal", "Thrash Metal" to "metal",
        "Anarcho Punk" to "punk", "Emo" to "punk", "Gothic Rock" to "rock", "Grunge" to "rock",
        "Hardcore Punk" to "punk", "Math Rock" to "rock", "Pop Punk" to "punk", "Post-Punk" to "punk",
        "Post-Rock" to "rock", "Progressive Rock" to "rock", "Psychedelic Rock" to "rock",
        "Punk Rock" to "punk", "Rock and Roll" to "rock", "Rockabilly" to "rock", "Shoegaze" to "indie",
        "Ska" to "punk", "Surf Rock" to "rock", "Art Rock" to "rock", "Blues Rock" to "rock",
        "Country Rock" to "rock", "Dream Pop" to "indie", "Folk Rock" to "folk", "Funk Rock" to "funk",
        "Glam Rock" to "rock", "Garage Rock" to "rock", "Indie Folk" to "folk", "Indie Pop" to "indie",
        "Indie Rock" to "indie", "Pop Rock" to "rock", "Southern Rock" to "rock", "Synth-Pop" to "electronic",
        "Britpop" to "indie", "New Wave" to "alternative", "Darkwave" to "alternative", "Post-Punk Revival" to "punk",
        "Cloud Rap" to "rap", "Conscious Rap" to "rap", "Crunk" to "rap", "Drill" to "rap",
        "Emo Rap" to "rap", "Gangsta Rap" to "rap", "Grime" to "rap", "Horrorcore" to "rap",
        "Jazz Rap" to "rap", "Phonk" to "rap", "Plugg" to "rap", "Trap" to "rap",
        "Trap Metal" to "rap", "Boom Bap" to "rap", "Mumble Rap" to "rap", "Rap Rock" to "rap",
        "Sigilkore" to "rap", "Alternative Rap" to "rap", "Abstract Hip Hop" to "rap",
        "Alternative Hip Hop" to "rap", "Chopper" to "rap", "Conscious Hip Hop" to "rap",
        "Experimental Hip Hop" to "rap", "G-Funk" to "rap", "Nerdcore Hip Hop" to "rap",
        "Old School Hip Hop" to "rap", "Pop Rap" to "rap", "Rage" to "rap", "Rage Rap" to "rap",
        "Trap Soul" to "rap", "Underground Hip Hop" to "rap", "Drift Phonk" to "rap",
        "House Phonk" to "rap", "Memphis Phonk" to "rap", "Vapor Phonk" to "rap", "Rare Phonk" to "rap",
        "Jungle" to "electronic", "Liquid Funk" to "electronic",
        "Acid Techno" to "techno", "Big Beat" to "electronic", "Chillstep" to "electronic",
        "Dark Electro" to "electronic", "Digital Hardcore" to "electronic", "Dub Techno" to "techno",
        "Electro House" to "house", "Future Garage" to "electronic", "Glitch" to "electronic",
        "Hard Trance" to "electronic", "Melodic Techno" to "techno", "Organic House" to "house",
        "Psybient" to "ambient", "Tech Trance" to "electronic",
        "Emo Pop" to "punk", "Post-Hardcore" to "punk", "Post-Metal" to "metal",
        "Sludge Metal" to "metal", "Stoner Rock" to "rock", "Gothic Metal" to "metal",
        "Power Pop" to "pop", "Art Pop" to "pop", "Baroque Pop" to "pop",
        "Trip Hop" to "electronic", "Downtempo" to "electronic", "Progressive Rock" to "rock",
        "Contemporary R&B" to "soul", "Neo-Soul" to "soul", "Jazz Fusion" to "jazz", "Acid Jazz" to "jazz",
        "Fusion" to "jazz", "Swing" to "jazz", "Boogie" to "funk", "Zydeco" to "folk",
        "Chamber Music" to "classical", "Neoclassical" to "classical", "Orchestral" to "classical",
        "Western Classical" to "classical", "Dungeon Synth" to "ambient", "Drone" to "ambient",
        "Noise" to "alternative", "Industrial" to "alternative", "Industrial Metal" to "metal",
        "Hyperpop" to "pop", "Art Pop" to "pop", "Baroque Pop" to "pop", "Power Pop" to "pop",
        "A cappella" to "vocal", "Chanson" to "vocal", "Schlager" to "pop",
    ).distinctBy { it.first.lowercase(Locale.ROOT) }.map { SearchGenre(it.first, it.second) }

    private val affinity = mapOf(
        "Rap" to mapOf(
            "Drill" to 0.65f, "Trap" to 0.65f, "Cloud Rap" to 0.5f, "Boom Bap" to 0.4f,
            "Gangsta Rap" to 0.4f, "Grime" to 0.35f, "Phonk" to 0.35f, "Plugg" to 0.3f,
            "Jazz Rap" to 0.25f, "Emo Rap" to 0.25f,
        ),
        "Phonk" to mapOf(
            "Drift Phonk" to 0.5f, "House Phonk" to 0.4f, "Memphis Phonk" to 0.4f,
            "Vapor Phonk" to 0.3f, "Rare Phonk" to 0.25f,
        ),
        "Drill" to mapOf("Rap" to 0.45f, "Trap" to 0.35f, "Grime" to 0.3f),
        "Trap" to mapOf("Rap" to 0.45f, "Drill" to 0.35f, "Phonk" to 0.3f),
        "Rock" to mapOf(
            "Alternative Rock" to 0.5f, "Hard Rock" to 0.45f, "Indie Rock" to 0.35f,
            "Punk Rock" to 0.3f, "Metal" to 0.25f,
        ),
        "Pop" to mapOf("Dance-Pop" to 0.45f, "Indie Pop" to 0.35f, "R&B" to 0.2f),
        "Electronic" to mapOf("House" to 0.45f, "Techno" to 0.45f, "EDM" to 0.4f, "Trance" to 0.3f),
        "Metal" to mapOf("Metalcore" to 0.4f, "Death Metal" to 0.35f, "Hard Rock" to 0.2f),
        "R&B" to mapOf("Soul" to 0.45f, "Contemporary R&B" to 0.5f, "Neo-Soul" to 0.35f),
        "House" to mapOf("Deep House" to 0.45f, "Tech House" to 0.4f, "EDM" to 0.25f),
        "Techno" to mapOf("Minimal Techno" to 0.45f, "Ambient Techno" to 0.35f, "EDM" to 0.2f),
    )

    fun listeningScores(listening: List<SearchGenreListening>): Map<String, Float> {
        val scores = mutableMapOf<String, Float>()
        listening.forEach { item ->
            val points = (item.playTimeMs.coerceAtLeast(0L) / 60_000f).coerceAtLeast(0.1f)
            val tags = item.tags.mapNotNull(::canonicalGenre).distinct()
            tags.forEach { genre ->
                scores[genre] = (scores[genre] ?: 0f) + points
                affinity[genre].orEmpty().forEach { (related, weight) ->
                    scores[related] = (scores[related] ?: 0f) + points * weight
                }
            }
        }
        return scores
    }

    fun rank(
        listeningScores: Map<String, Float>,
        tapScores: Map<String, Int>,
    ): List<SearchGenre> = catalog.sortedWith(
        compareByDescending<SearchGenre> {
            (listeningScores[it.name] ?: 0f) + (tapScores[it.name] ?: 0) * TAP_POINTS
        }.thenBy { catalog.indexOf(it) },
    )

    fun canonicalGenre(raw: String): String? {
        val normalized = normalize(raw)
        if (normalized.isEmpty()) return null
        if (normalized == "hip hop" || normalized == "hiphop" || normalized == "rap hip hop") return "Rap"

        val catalogMatch = catalog.asSequence()
            .map { it.name }
            .filter { name ->
                val candidate = normalize(name)
                normalized == candidate || normalized.endsWith(" $candidate")
            }
            .maxByOrNull { normalize(it).length }
        if (catalogMatch != null) return catalogMatch

        return when {
            "drill" in normalized -> "Drill"
            "phonk" in normalized -> "Phonk"
            "trap" in normalized -> "Trap"
            "rap" in normalized || "hip hop" in normalized || "hiphop" in normalized -> "Rap"
            normalized.endsWith(" pop") -> "Pop"
            "rock" in normalized -> "Rock"
            else -> null
        }
    }

    private fun normalize(value: String): String = value.lowercase(Locale.ROOT)
        .replace(Regex("[^a-z0-9]+"), " ")
        .trim()
}
