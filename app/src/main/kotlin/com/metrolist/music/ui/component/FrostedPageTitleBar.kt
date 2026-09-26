package com.metrolist.music.ui.component

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import com.metrolist.music.ui.theme.SpaceMonoFontFamily
import com.metrolist.music.ui.theme.textPrimary

@Composable
fun FrostedPageTitleBar(
    @StringRes titleRes: Int,
    progress: Float,
    backdrop: FrostBackdrop?,
    modifier: Modifier = Modifier,
    fullWidth: Boolean = false,
    titleProgress: Float = progress,
    barHeight: Dp = 56.dp,
    centerTitleVertically: Boolean = false,
    titleEnterOffset: Dp = 0.dp,
    titleEnterFromBottom: Boolean = false,
    shadeTopEdge: Boolean = false,
    solidBackgroundColor: Color? = null,
    fadeTitle: Boolean = true,
) {
    var titleHeightPx by remember { mutableStateOf(0) }
    val statusBarHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val barModifier = modifier.fillMaxWidth().height(statusBarHeight + barHeight)
    Box(
        modifier = if (fullWidth && solidBackgroundColor != null) {
            barModifier.drawBehind {
                val p = progress.coerceIn(0f, 1f)
                drawRect(solidBackgroundColor, alpha = p)
                val strokeWidth = 1.dp.toPx()
                val y = size.height - strokeWidth / 2f
                drawLine(
                    Color.White.copy(alpha = 0.22f),
                    Offset(0f, y),
                    Offset(size.width, y),
                    strokeWidth = strokeWidth,
                    alpha = p,
                )
            }
        } else if (fullWidth) barModifier
            .alpha23FrostedTopBarBackground(
                progress = progress,
                barColor = MaterialTheme.colorScheme.background,
                strokeColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f),
                backdrop = backdrop,
            )
            .drawBehind {
                if (shadeTopEdge) {
                    drawRect(
                        brush = Brush.verticalGradient(
                            colorStops = arrayOf(
                                0f to Color.Black,
                                1f to Color.Transparent,
                            ),
                            endY = 10.dp.toPx(),
                        ),
                        alpha = titleProgress.coerceIn(0f, 1f) * progress.coerceIn(0f, 1f),
                    )
                }
            } else barModifier,
        contentAlignment = if (fullWidth) Alignment.BottomStart else Alignment.TopEnd,
    ) {
        Box(
            modifier = if (fullWidth) {
                if (centerTitleVertically) {
                    Modifier.padding(start = 20.dp, end = 20.dp).height(barHeight)
                        .then(if (titleEnterFromBottom) Modifier.clipToBounds() else Modifier)
                } else {
                    Modifier.padding(start = 20.dp, bottom = 12.dp).height(32.dp)
                }
            } else {
                Modifier
                    .padding(top = statusBarHeight + 14.dp, end = 20.dp)
                    .defaultMinSize(minWidth = 112.dp)
                    .height(44.dp)
                    .clip(CircleShape)
                    .alpha23FrostedTopBarBackground(
                        progress = progress,
                        barColor = MaterialTheme.colorScheme.background,
                        strokeColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f),
                        backdrop = backdrop,
                    )
                    .padding(horizontal = 18.dp)
            },
            contentAlignment = if (fullWidth) Alignment.CenterStart else Alignment.Center,
        ) {
            Text(
                text = stringResource(titleRes),
                style = MaterialTheme.typography.titleMedium
                    .copy(fontWeight = FontWeight.SemiBold)
                    .let { if (fullWidth) it.copy(fontFamily = SpaceMonoFontFamily) else it },
                color = if (solidBackgroundColor != null) Color.White else MaterialTheme.colorScheme.textPrimary,
                modifier = Modifier.onSizeChanged { titleHeightPx = it.height }.graphicsLayer {
                    val reveal = titleProgress.coerceIn(0f, 1f)
                    alpha = if (fadeTitle) reveal else if (reveal > 0f) 1f else 0f
                    val enterOffset = if (titleEnterFromBottom) {
                        (barHeight.toPx() + titleHeightPx) / 2f
                    } else {
                        titleEnterOffset.toPx()
                    }
                    translationY = (1f - reveal) * enterOffset
                },
            )
        }
    }
}
