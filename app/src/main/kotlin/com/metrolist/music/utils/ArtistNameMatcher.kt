package com.metrolist.music.utils

import java.text.Normalizer
import java.util.Locale

internal object ArtistNameMatcher {
    fun normalize(name: String): String =
        Normalizer.normalize(name, Normalizer.Form.NFKD)
            .replace(Regex("\\p{M}+"), "")
            .lowercase(Locale.ROOT)
            .removeSuffix(" - topic")
            .filter { it.isLetterOrDigit() }

    fun <T> bestMatch(name: String, candidates: List<T>, candidateName: (T) -> String): T? {
        val target = normalize(name)
        if (target.isEmpty()) return null
        val ranked = candidates.map { it to similarity(target, normalize(candidateName(it))) }
            .sortedByDescending { it.second }
        val best = ranked.firstOrNull() ?: return null
        if (best.second == 1.0) return best.first
        // Short names and near-ties are too easy to confuse with unrelated artists.
        if (target.length < 5 || best.second < 0.84) return null
        if (ranked.getOrNull(1)?.let { best.second - it.second < 0.05 } == true) return null
        return best.first
    }

    private fun similarity(left: String, right: String): Double {
        if (left == right) return 1.0
        if (left.isEmpty() || right.isEmpty()) return 0.0
        var previous = IntArray(right.length + 1) { it }
        for (i in left.indices) {
            val current = IntArray(right.length + 1)
            current[0] = i + 1
            for (j in right.indices) {
                current[j + 1] = minOf(
                    current[j] + 1,
                    previous[j + 1] + 1,
                    previous[j] + if (left[i] == right[j]) 0 else 1,
                )
            }
            previous = current
        }
        return 1.0 - previous.last().toDouble() / maxOf(left.length, right.length)
    }
}
