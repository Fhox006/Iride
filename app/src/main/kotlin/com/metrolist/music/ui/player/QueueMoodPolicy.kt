package com.metrolist.music.ui.player

const val AUTOMIX_FILTER_ALL = "ALL"
const val AUTOMIX_FILTER_POPULAR = "POPULAR"
const val AUTOMIX_FILTER_DISCOVER = "DISCOVER"
const val AUTOMIX_FILTER_FAMILIAR = "FAMILIAR"
const val AUTOMIX_FILTER_PARTY = "PARTY"
const val AUTOMIX_FILTER_WORKOUT = "WORKOUT"
const val AUTOMIX_FILTER_DEEP_CUTS = "DEEP CUTS"
val AUTOMIX_STATIC_FILTERS =
    listOf(
        AUTOMIX_FILTER_ALL,
        AUTOMIX_FILTER_POPULAR,
        AUTOMIX_FILTER_PARTY,
        AUTOMIX_FILTER_WORKOUT,
        AUTOMIX_FILTER_DEEP_CUTS,
    )
private val PARTY_TAG_KEYWORDS = listOf("dance", "electro", "edm", "house", "pop")
private val WORKOUT_TAG_KEYWORDS = listOf("rock", "metal", "hip", "rap", "edm")

/** Unknown tags do not match a mood. The caller decides whether an empty result falls back. */
internal fun <T> matchingQueueMoodItems(
    items: List<T>,
    filter: String,
    familiarArtists: Set<String>,
    artists: (T) -> List<String>?,
    genres: (T) -> List<String>,
): List<T> =
    when (filter) {
        AUTOMIX_FILTER_ALL,
        AUTOMIX_FILTER_POPULAR -> items
        AUTOMIX_FILTER_DEEP_CUTS -> items.drop(items.size / 2)
        AUTOMIX_FILTER_DISCOVER ->
            items.filter { item -> artists(item)?.none { it in familiarArtists } == true }
        AUTOMIX_FILTER_FAMILIAR ->
            items.filter { item -> artists(item)?.any { it in familiarArtists } == true }
        AUTOMIX_FILTER_PARTY ->
            items.filter { item ->
                genres(item).any { tag -> PARTY_TAG_KEYWORDS.any { tag.contains(it, true) } }
            }
        AUTOMIX_FILTER_WORKOUT ->
            items.filter { item ->
                genres(item).any { tag -> WORKOUT_TAG_KEYWORDS.any { tag.contains(it, true) } }
            }
        else -> items.filter { item -> genres(item).any { it.equals(filter, true) } }
    }
