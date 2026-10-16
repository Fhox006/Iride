package com.metrolist.music.ui.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties

/** Contextual controls anchored below their Folio header group, sharing its glass renderer. */
@Composable
internal fun IrideFolioControlPanel(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    preferAbove: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    if (!expanded) return
    val backdrop = LocalFolioButtonBackdrop.current ?: LocalScreenFrostBackdrop.current
    val gap = with(LocalDensity.current) { IrideFolio.MenuOffset.y.roundToPx() }
    val position = remember(gap, preferAbove) {
        object : PopupPositionProvider {
            override fun calculatePosition(
                anchorBounds: IntRect,
                windowSize: IntSize,
                layoutDirection: LayoutDirection,
                popupContentSize: IntSize,
            ): IntOffset {
                val left = if (layoutDirection == LayoutDirection.Ltr) {
                    anchorBounds.right - popupContentSize.width
                } else anchorBounds.left
                return IntOffset(
                    left.coerceIn(0, (windowSize.width - popupContentSize.width).coerceAtLeast(0)),
                    (if (preferAbove) anchorBounds.top - popupContentSize.height - gap else anchorBounds.bottom + gap)
                        .coerceIn(0, (windowSize.height - popupContentSize.height).coerceAtLeast(0)),
                )
            }
        }
    }
    Popup(
        popupPositionProvider = position,
        onDismissRequest = onDismissRequest,
        properties = PopupProperties(focusable = true),
    ) {
        Column(
            Modifier.width(IrideFolio.MenuWidth)
                .irideFolioGlassSurface(backdrop)
                .padding(horizontal = IrideFolio.MenuSectionHorizontalPadding, vertical = IrideFolio.MenuVerticalPadding),
            content = content,
        )
    }
}
