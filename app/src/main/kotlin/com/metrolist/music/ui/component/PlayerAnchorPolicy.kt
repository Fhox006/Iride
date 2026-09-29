package com.metrolist.music.ui.component

internal fun persistablePlayerAnchor(anchor: Int): Int =
    if (anchor == expandedAnchor) collapsedAnchor else anchor
