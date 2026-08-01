/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.utils

/**
 * Detects "feat./featuring/ft." collaborator credits embedded in a song title
 * (English markers only) and strips them out, returning the credited name.
 */
object TitleFeaturingParser {
    private val quickCheck = Regex("""\bfeat\b|\bft\b|\bfeaturing\b""", RegexOption.IGNORE_CASE)

    private val parenPattern =
        Regex("""[(\[]\s*(?:feat\.?|featuring|ft\.?)\s+([^)\]]+?)\s*[)\]]""", RegexOption.IGNORE_CASE)
    private val trailingPattern =
        Regex("""\s+(?:feat\.?|featuring|ft\.?)\s+(.+)$""", RegexOption.IGNORE_CASE)

    // Splits a raw captured credit into individual names: "Frezza, G.Mineiro" or "Frezza & G.Mineiro"
    // both credit two separate artists, not one combined name.
    private val nameSeparator = Regex("""\s*(?:,|&|/)\s*""")

    private fun splitNames(raw: String): List<String> =
        nameSeparator.split(raw).map { it.trim() }.filter { it.isNotEmpty() }

    /** Cheap pre-check to avoid running the full regex on every title. */
    fun looksFeatured(title: String): Boolean = quickCheck.containsMatchIn(title)

    /** Returns (cleanTitle, collaboratorNames) if a featuring credit was found, else null. */
    fun extract(title: String): Pair<String, List<String>>? {
        parenPattern.find(title)?.let { match ->
            val artists = splitNames(match.groupValues[1])
            if (artists.isEmpty()) return null
            val clean = title.replace(match.value, "").trim().replace(Regex("""\s{2,}"""), " ")
            return clean to artists
        }
        trailingPattern.find(title)?.let { match ->
            val artists = splitNames(match.groupValues[1])
            if (artists.isEmpty()) return null
            val clean = title.substring(0, match.range.first).trim()
            return clean to artists
        }
        return null
    }
}
