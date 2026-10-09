package com.metrolist.music.ui.screens

/** Bound server-provided headings without splitting a word. */
internal fun compactHomeTitle(title: String, maxWords: Int = 6): String {
    val words = title.trim().split(Regex("\\s+")).filter(String::isNotBlank)
    return words.take(maxWords).joinToString(" ") + if (words.size > maxWords) "…" else ""
}

/** Empty categories never count as separators between two four-row shelves. */
internal class HomeShelfOrder {
    private var previousHasFourRows = false

    fun canAdd(hasFourRows: Boolean): Boolean = !hasFourRows || !previousHasFourRows

    fun added(hasFourRows: Boolean) {
        previousHasFourRows = hasFourRows
    }
}
