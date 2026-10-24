package com.metrolist.music.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import com.metrolist.music.ui.theme.FolioShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.metrolist.music.ui.component.shimmer.ShimmerHost
import com.metrolist.music.ui.theme.IBMPlexMonoFontFamily

/** Folio loading uses the final artwork footprint and a font-scaled caption, never a spinner. */
@Composable
fun IrideFolioShelfSkeleton(
    thumbnailSize: Dp,
    modifier: Modifier = Modifier,
    contentPadding: Dp = IrideFolio.HorizontalPadding,
    tilesPerSide: Int = 1,
) {
    require(tilesPerSide in 1..2)
    val color = MaterialTheme.colorScheme.surfaceContainerHighest
    ShimmerHost(modifier = modifier.fillMaxWidth(), showGradient = false) {
        Row(
            Modifier.fillMaxWidth().clipToBounds().padding(start = contentPadding),
            horizontalArrangement = Arrangement.spacedBy(IrideFolio.ShelfItemSpacing),
        ) {
            repeat(4) {
                Column(Modifier.width(thumbnailSize)) {
                    val gap = if (tilesPerSide == 2) IrideFolio.ShelfMosaicGap else 0.dp
                    val side = (thumbnailSize - gap * (tilesPerSide - 1)) / tilesPerSide
                    Column(verticalArrangement = Arrangement.spacedBy(gap)) {
                        repeat(tilesPerSide) {
                            Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                                repeat(tilesPerSide) {
                                    Box(Modifier.size(side).background(color, FolioShape(6.dp)))
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    Box(Modifier.fillMaxWidth(0.65f).background(color, FolioShape(3.dp))) {
                        Text(
                            text = " ",
                            style = MaterialTheme.typography.bodyLarge.copy(fontFamily = IBMPlexMonoFontFamily),
                            maxLines = 1,
                            modifier = Modifier.alpha(0f),
                        )
                    }
                }
            }
        }
    }
}
