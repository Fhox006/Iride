package com.metrolist.music.ui.screens.search

/** Preserve the user's existing prefix, including case, when accepting a longer suggestion. */
internal fun searchCompletionText(typed: String, suggested: String): String {
    val prefix = searchCompletionPrefixLength(typed, suggested)
    return typed.take(prefix) + suggested.drop(prefix)
}

internal fun searchCompletionPrefixLength(typed: String, completed: String): Int {
    if (typed.isBlank()) return 0
    return typed.zip(completed).takeWhile { (left, right) -> left.equals(right, ignoreCase = true) }.size
}
