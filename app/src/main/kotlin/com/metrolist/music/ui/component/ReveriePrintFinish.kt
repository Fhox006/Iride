package com.metrolist.music.ui.component

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache

/** Apply the print treatment once, above the complete composition, including text and controls. */
internal fun Modifier.reveriePrintFinish(): Modifier = drawWithCache {
    val grain = irideGrainBrush(density, soft = false)
    onDrawWithContent {
        drawContent()
        drawRect(grain, alpha = .13f)
    }
}
