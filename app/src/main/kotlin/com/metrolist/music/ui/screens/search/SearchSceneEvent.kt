package com.metrolist.music.ui.screens.search

internal enum class SearchSceneMode { Landing, Editing, Result }

/** Explicit user events own motion; focus notifications and network updates cannot replace it. */
internal data class SearchSceneEvent(
    val revision: Int = 0,
    val mode: SearchSceneMode = SearchSceneMode.Landing,
    val confirmsSearch: Boolean = false,
    val returnsToRoot: Boolean = false,
) {
    fun open(): SearchSceneEvent = if (mode == SearchSceneMode.Editing) this
        else SearchSceneEvent(revision + 1, SearchSceneMode.Editing)

    fun submit(hasQuery: Boolean): SearchSceneEvent = SearchSceneEvent(
        revision + 1, if (hasQuery) SearchSceneMode.Result else SearchSceneMode.Landing, hasQuery,
        returnsToRoot = !hasQuery,
    )

    fun dismiss(hasResult: Boolean): SearchSceneEvent = if (mode != SearchSceneMode.Editing) this
        else SearchSceneEvent(revision + 1, if (hasResult) SearchSceneMode.Result else SearchSceneMode.Landing)

    fun reset(): SearchSceneEvent = if (mode == SearchSceneMode.Landing) this
        else SearchSceneEvent(revision + 1, SearchSceneMode.Landing, returnsToRoot = true)
}

/** Data is released only by the transition which requested the reset, never a stale callback. */
internal fun searchRootReturnComplete(event: SearchSceneEvent, completedRevision: Int): Boolean =
    event.returnsToRoot && event.revision == completedRevision && event.mode == SearchSceneMode.Landing

internal fun searchSceneHasResult(event: SearchSceneEvent, submittedQuery: String): Boolean =
    event.mode == SearchSceneMode.Result || (event.mode == SearchSceneMode.Editing && submittedQuery.isNotBlank())

internal enum class SearchBackAction { None, DismissEditor, ReturnToRoot }

/** A cancelled gesture, or one overtaken by another action, cannot clear a newer search. */
internal fun searchBackAction(start: SearchSceneEvent, current: SearchSceneEvent): SearchBackAction = when {
    start.revision != current.revision || current.returnsToRoot -> SearchBackAction.None
    start.mode == SearchSceneMode.Editing -> SearchBackAction.DismissEditor
    start.mode == SearchSceneMode.Result -> SearchBackAction.ReturnToRoot
    else -> SearchBackAction.None
}
