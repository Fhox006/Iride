package com.metrolist.music.ui.player

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import kotlin.math.roundToInt

/** Inline content ends above the footer; expanded content continues behind its fading controls. */
@Composable
internal fun IrideMp3DisplayLayout(
    expansion: Float,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Layout(content = content, modifier = modifier) { measurables, constraints ->
        val footer = measurables[1].measure(constraints.copy(minHeight = 0))
        val reservedHeight = (footer.height * (1f - expansion)).roundToInt()
        val viewportHeight = (constraints.maxHeight - reservedHeight).coerceAtLeast(0)
        val viewport =
            measurables[0].measure(
                constraints.copy(minHeight = viewportHeight, maxHeight = viewportHeight)
            )
        layout(constraints.maxWidth, constraints.maxHeight) {
            viewport.placeRelative(0, 0)
            footer.placeRelative(0, constraints.maxHeight - footer.height)
        }
    }
}
