package com.metrolist.music.ui.component

import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController

private const val PendingSearchSection = "pendingSimpleSearchSection"
private const val SearchSectionSource = "simpleSearchSectionSource"

/** Search previews disappear on navigation, so they cannot own a returning artwork flight. */
internal fun NavController.navigateSearchSection(route: String, simpleMotion: Boolean = true) {
    if (!simpleMotion) {
        navigate(route)
        return
    }
    val source = currentBackStackEntry ?: return
    source.savedStateHandle[PendingSearchSection] = true
    try {
        navigate(route)
        currentBackStackEntry?.takeIf { it.id != source.id }?.savedStateHandle
            ?.set(SearchSectionSource, source.id)
    } finally {
        source.savedStateHandle.remove<Boolean>(PendingSearchSection)
    }
}

internal fun usesSimpleSearchSectionMotion(from: NavBackStackEntry?, to: NavBackStackEntry?): Boolean {
    if (from == null || to == null) return false
    return from.savedStateHandle.get<Boolean>(PendingSearchSection) == true ||
        from.savedStateHandle.get<String>(SearchSectionSource) == to.id ||
        to.savedStateHandle.get<String>(SearchSectionSource) == from.id
}
