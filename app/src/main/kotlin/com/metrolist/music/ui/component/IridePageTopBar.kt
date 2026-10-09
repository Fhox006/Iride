package com.metrolist.music.ui.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.metrolist.music.ui.utils.rememberReducedMotion

/**
 * Folio controls only. Legacy title/background arguments remain source-compatible, but never
 * recreate a compact page title or a full-width surface. titleContent is for search/selection.
 */
@Composable
fun IridePageTopBar(
    title: String,
    progress: Float,
    modifier: Modifier = Modifier,
    backgroundColor: Color = IridePageLayoutDefaults.BarColor,
    strokeColor: Color = Color.White.copy(alpha = 0.08f),
    titleProgress: Float = progress,
    navigationIcon: (@Composable () -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
    menuSections: List<List<IrideFolioMenuAction>> = emptyList(),
    titleContent: (@Composable () -> Unit)? = null,
    reducedMotion: Boolean = rememberReducedMotion(),
) {
    val backdrop = LocalScreenFrostBackdrop.current
    IridePageTopBar(progress = progress, modifier = modifier) {
        if (navigationIcon != null) {
            IrideFolioCompactControls {
                Box(
                    Modifier.size(IrideFolio.ControlSurfaceSize)
                        .irideFolioControlSurface(progress, backdrop)
                ) {
                    navigationIcon()
                }
            }
        }
        Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
            titleContent?.invoke()
        }
        Row(
            modifier = Modifier.irideFolioControlSurface(progress, backdrop),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IrideFolioCompactControls { Row(content = actions) }
            IrideFolioOverflowMenu(menuSections)
        }
    }
}

/**
 * Transparent geometry for contextual bars. The caller supplies control groups; page-wide scrims,
 * elevation and divider backgrounds are deliberately excluded by the Folio protocol.
 */
@Composable
fun IridePageTopBar(
    progress: Float,
    modifier: Modifier = Modifier,
    backgroundColor: Color = IridePageLayoutDefaults.BarColor,
    strokeColor: Color = Color.White.copy(alpha = 0.08f),
    horizontalPadding: androidx.compose.ui.unit.Dp = IridePageLayoutDefaults.HorizontalPadding,
    content: @Composable RowScope.() -> Unit,
) {
    CompositionLocalProvider(
        LocalContentColor provides (LocalArtworkContentColor.current ?: Color.White),
        LocalMinimumInteractiveComponentSize provides 0.dp,
        LocalPageControlsProgress provides progress,
    ) {
        Row(
            modifier =
                modifier
                    .fillMaxWidth()
                    .padding(top = iridePageStatusBarTopPadding() + IrideFolio.ControlsTopSpacing)
                    .height(IrideFolio.ControlsHeight)
                    .padding(horizontal = horizontalPadding),
            verticalAlignment = Alignment.CenterVertically,
            content = content,
        )
    }
}
