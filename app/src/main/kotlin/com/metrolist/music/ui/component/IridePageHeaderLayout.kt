package com.metrolist.music.ui.component

import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Bounded Folio adapter for embedded views. The legacy synthetic collapse/drag header has been
 * removed: title motion, bars and overscroll must never run a second protocol beside Folio. Normal
 * scrolling Columns should use IridePageFrame; lazy content should use IridePageLayout.
 */
@Composable
fun IridePageHeaderLayout(
    title: String,
    modifier: Modifier = Modifier,
    scrollState: ScrollableState = rememberScrollState(),
    scrollOffset: () -> Int = { 0 },
    navigationIcon: (@Composable () -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
    menuSections: List<List<IrideFolioMenuAction>> = emptyList(),
    content: @Composable BoxScope.() -> Unit,
) {
    IridePageFrame(
        title = title,
        modifier = modifier,
        scrollState = scrollState,
        scrollOffset = scrollOffset,
        navigationIcon = navigationIcon,
        actions = actions,
        menuSections = menuSections,
        content = content,
    )
}
