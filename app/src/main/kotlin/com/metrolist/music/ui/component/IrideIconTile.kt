package com.metrolist.music.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import com.metrolist.music.ui.theme.FolioShape
import androidx.compose.runtime.Composable
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.metrolist.music.ui.theme.strokeCard

/** Shared colored container for monochrome settings icons. */
@Composable
fun IrideIconTile(
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    color: Color,
    content: @Composable () -> Unit,
) {
    IrideIconFrame(
        modifier = modifier.size(size),
        shape = FolioShape(12.dp),
        backgroundColor = color,
        borderColor = MaterialTheme.colorScheme.strokeCard,
        content = content,
    )
}

@Composable
fun IrideIconFrame(
    modifier: Modifier = Modifier,
    shape: Shape,
    backgroundColor: Color,
    borderColor: Color,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .clip(shape)
            .background(backgroundColor)
            .border(1.dp, borderColor, shape),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}
