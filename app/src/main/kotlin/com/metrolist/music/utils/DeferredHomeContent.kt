package com.metrolist.music.utils

/** Defer refreshes until Home is hidden; empty shelves can load and section lists can opt into growth. */
internal class DeferredHomeContent<T>(
    initial: T,
    private val hasContent: (T) -> Boolean,
    private val identity: (T) -> Any? = { Unit },
    private val mergeWhileVisible: (T, T) -> T = { current, _ -> current },
) {
    var displayed: T = initial
        private set
    private var pending: T = initial
    private var visible = true

    fun update(value: T): T {
        pending = value
        displayed = if (!visible || !hasContent(displayed) || identity(displayed) != identity(value)) {
            value
        } else {
            mergeWhileVisible(displayed, value)
        }
        return displayed
    }

    fun setVisible(value: Boolean): T {
        visible = value
        if (!visible) displayed = pending
        return displayed
    }
}

/** Append new shelves from an ongoing load without replacing or moving visible shelves. */
internal fun <T, K> appendLoadedHomeSections(
    displayed: List<T>,
    incoming: List<T>,
    key: (T) -> K,
): List<T> {
    val displayedIds = displayed.mapTo(mutableSetOf(), key)
    val incomingIds = incoming.mapTo(mutableSetOf(), key)
    // A replacement batch remains pending until Home is hidden, like other refreshes.
    if (!incomingIds.containsAll(displayedIds)) return displayed
    val additions = incoming.distinctBy(key).filterNot { key(it) in displayedIds }
    return if (additions.isEmpty()) displayed else displayed + additions
}
