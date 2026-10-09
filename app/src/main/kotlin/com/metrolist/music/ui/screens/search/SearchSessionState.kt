package com.metrolist.music.ui.screens.search

/** The submitted page and its transition change atomically; the native field owns only the draft. */
internal data class SearchSessionState(
    val submittedQuery: String = "",
    val event: SearchSceneEvent = SearchSceneEvent(
        mode = if (submittedQuery.isBlank()) SearchSceneMode.Landing else SearchSceneMode.Result,
    ),
) {
    fun open(): SearchSessionState = copy(
        submittedQuery = if (event.returnsToRoot) "" else submittedQuery,
        event = event.open(),
    )

    fun submit(query: String): SearchSessionState {
        val submitted = query.trim()
        return if (submitted.isEmpty()) reset()
            else SearchSessionState(submitted, event.submit(true))
    }

    fun dismiss(): SearchSessionState = copy(event = event.dismiss(submittedQuery.isNotBlank()))

    fun reset(): SearchSessionState = if (event.returnsToRoot) this else copy(
        event = if (event.mode == SearchSceneMode.Landing) event.submit(false) else event.reset(),
    )

    fun finish(revision: Int): SearchSessionState = if (searchRootReturnComplete(event, revision)) {
        SearchSessionState("", event.copy(returnsToRoot = false))
    } else this

    // A process restart must not revive the result that the user has already asked to clear.
    val restoredQuery: String get() = if (event.returnsToRoot) "" else submittedQuery
}
