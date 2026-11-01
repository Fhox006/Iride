package com.metrolist.music.ui.screens.search

import java.text.Normalizer
import java.util.Locale

private val predictionMarks = Regex("\\p{M}+")
private val predictionSeparators = Regex("[^\\p{L}\\p{N}]+")
private val predictionKinds = setOf("artista", "artist", "cantante", "singer", "album", "disco", "brano", "song", "canzone", "playlist")

private fun predictionWords(value: String): List<String> = predictionSeparators.split(
    predictionMarks.replace(Normalizer.normalize(value, Normalizer.Form.NFD), "")
        .lowercase(Locale.ROOT),
).filter { it.isNotEmpty() }

/** All typed words must match; a single typo is accepted only after four characters. */
internal fun searchPredictionScore(query: String, title: String, relatedText: String = ""): Int? {
    val typed = predictionWords(query)
    if (typed.isEmpty()) return null
    val titleWords = predictionWords(title)
    val relatedWords = predictionWords(relatedText)
    if (titleWords.joinToString(" ").startsWith(typed.joinToString(" "))) return 0
    if (typed.all { word -> titleWords.any { it.startsWith(word) } }) return 1
    fun candidatesFor(word: String) = titleWords + relatedWords.filter { it !in predictionKinds || it == word }
    if (typed.all { word -> candidatesFor(word).any { it.startsWith(word) } }) return 2
    return if (typed.all { word ->
        candidatesFor(word).any { candidate ->
            candidate.startsWith(word) || word.length >= 4 &&
                (word.length - 1..word.length + 1).any { length ->
                    candidate.length >= length && predictionOneEdit(word, candidate.take(length))
                }
        }
    }) 3 else null
}

private fun predictionOneEdit(a: String, b: String): Boolean {
    if (kotlin.math.abs(a.length - b.length) > 1) return false
    var i = 0
    var j = 0
    var edits = 0
    while (i < a.length && j < b.length) {
        if (a[i] == b[j]) { i++; j++; continue }
        if (++edits > 1) return false
        when {
            a.length == b.length && i + 1 < a.length && j + 1 < b.length &&
                a[i] == b[j + 1] && a[i + 1] == b[j] -> { i += 2; j += 2 }
            a.length > b.length -> i++
            b.length > a.length -> j++
            else -> { i++; j++ }
        }
    }
    return edits + (a.length - i) + (b.length - j) <= 1
}
