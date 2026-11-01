package com.metrolist.music.viewmodels

internal const val CONTINUE_LISTENING_VISIT_INTERVAL_MS = 5 * 60 * 1000L
internal const val CONTINUE_LISTENING_IGNORED_VISITS_TO_HIDE = 2

internal data class ContinueListeningVisibilityState(
    val ignoredVisits: Int = 0,
    val lastIgnoredVisitAtMs: Long? = null,
    val hidden: Boolean = false,
)

internal fun ContinueListeningVisibilityState.recordVisit(
    selectedAlbum: Boolean,
    nowMs: Long,
): ContinueListeningVisibilityState {
    if (hidden || selectedAlbum) return this
    if (lastIgnoredVisitAtMs != null && nowMs - lastIgnoredVisitAtMs < CONTINUE_LISTENING_VISIT_INTERVAL_MS) {
        return this
    }

    val nextCount = (ignoredVisits + 1).coerceAtMost(CONTINUE_LISTENING_IGNORED_VISITS_TO_HIDE)
    return copy(
        ignoredVisits = nextCount,
        lastIgnoredVisitAtMs = nowMs,
        hidden = nextCount >= CONTINUE_LISTENING_IGNORED_VISITS_TO_HIDE,
    )
}
