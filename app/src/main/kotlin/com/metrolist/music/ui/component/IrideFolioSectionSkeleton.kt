package com.metrolist.music.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import com.metrolist.music.ui.theme.FolioShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.metrolist.music.ui.component.shimmer.ListItemPlaceHolder
import com.metrolist.music.ui.component.shimmer.ShimmerHost

/** A loading section reserves its heading and final content format as one indivisible block. */
@Composable
fun IrideFolioSectionSkeleton(
    thumbnailSize: Dp,
    songs: Boolean = false,
    mosaic: Boolean = false,
    controls: Boolean = false,
    hero: Boolean = false,
) {
    Column(Modifier.fillMaxWidth()) {
        ShimmerHost(showGradient = false) {
            Box(
                Modifier.padding(
                        start = IrideFolio.HorizontalPadding,
                        top = IrideFolio.ShelfTopPadding,
                        bottom = IrideFolio.ShelfBottomPadding,
                    )
                    .width(190.dp)
                    .background(
                        MaterialTheme.colorScheme.surfaceContainerHighest,
                        FolioShape(3.dp),
                    )
            ) {
                Text(
                    text = " ",
                    style =
                        LocalFolioDisplayHeadings.current?.shelf
                            ?: MaterialTheme.typography.titleLarge.copy(
                                fontFamily = folioTypewriterFontFamily(),
                                fontSize = IrideFolio.ShelfTitleFontSize,
                                lineHeight = IrideFolio.ShelfTitleFontSize * 1.35f,
                            ),
                    modifier = Modifier.alpha(0f),
                )
            }
            if (controls) Spacer(Modifier.height(40.dp))
            if (songs) {
                repeat(4) {
                    ListItemPlaceHolder(
                        Modifier.padding(horizontal = IrideFolio.HorizontalPadding - 4.dp)
                    )
                }
                Spacer(Modifier.height(IrideFolio.ShelfPagerFooterHeight))
            } else if (hero) {
                Box(
                    Modifier.fillMaxWidth()
                        .padding(horizontal = IrideFolio.HorizontalPadding)
                        .height(148.dp)
                        .background(
                            MaterialTheme.colorScheme.surfaceContainerHighest,
                            FolioShape(12.dp),
                        )
                )
                Spacer(Modifier.height(IrideFolio.ShelfPagerFooterHeight))
            }
        }
        if (!songs && !hero) {
            if (controls) {
                Box(Modifier.fillMaxWidth().height(thumbnailSize + 64.dp)) {
                    IrideFolioShelfSkeleton(thumbnailSize)
                }
            } else {
                IrideFolioShelfSkeleton(thumbnailSize, tilesPerSide = if (mosaic) 2 else 1)
                if (!mosaic) {
                    Text(
                        text = " ",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.alpha(0f).padding(vertical = 4.dp),
                    )
                }
            }
        }
    }
}
