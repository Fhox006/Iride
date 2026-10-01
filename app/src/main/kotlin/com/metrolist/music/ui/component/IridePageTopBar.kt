package com.metrolist.music.ui.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.material3.LocalContentColor
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.metrolist.music.ui.theme.SpaceMonoFontFamily
import com.metrolist.music.ui.utils.rememberReducedMotion

/** The fixed Home bar, with optional navigation and contextual controls. */
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
    titleContent: (@Composable () -> Unit)? = null,
    reducedMotion: Boolean = rememberReducedMotion(),
) {
    IridePageTopBar(
        progress = progress,
        modifier = modifier,
        backgroundColor = backgroundColor,
        strokeColor = strokeColor,
        horizontalPadding = if (navigationIcon == null) IridePageLayoutDefaults.HorizontalPadding else 12.dp,
    ) {
        navigationIcon?.invoke()
        Box(
            modifier = Modifier.weight(1f).height(IridePageLayoutDefaults.BarHeight).clipToBounds(),
            contentAlignment = Alignment.CenterStart,
        ) {
            if (titleContent != null) {
                titleContent()
            } else {
                Text(
                    text = title,
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = SpaceMonoFontFamily,
                        fontWeight = FontWeight.SemiBold,
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.graphicsLayer {
                        val reveal = titleProgress.coerceIn(0f, 1f)
                        alpha = if (reveal > 0f) 1f else 0f
                        translationY = if (reducedMotion) 0f else
                            (1f - reveal) * (IridePageLayoutDefaults.BarHeight.toPx() + size.height) / 2f
                    },
                )
            }
        }
        actions()
    }
}

fun iridePageTitleProgress(titleTopPx: Float, titleHeightPx: Float, barBottomPx: Float, pastTitle: Boolean): Float =
    when {
        pastTitle -> 1f
        titleHeightPx <= 0f -> 0f
        else -> ((barBottomPx - titleTopPx) / titleHeightPx).coerceIn(0f, 1f)
    }

fun Modifier.iridePageTitleClip(progress: () -> Float): Modifier = drawWithContent {
    clipRect(top = size.height * progress().coerceIn(0f, 1f)) {
        this@drawWithContent.drawContent()
    }
}

/** Shared geometry and opaque background for pages with selection, editing or search controls. */
@Composable
fun IridePageTopBar(
    progress: Float,
    modifier: Modifier = Modifier,
    backgroundColor: Color = IridePageLayoutDefaults.BarColor,
    strokeColor: Color = Color.White.copy(alpha = 0.08f),
    horizontalPadding: androidx.compose.ui.unit.Dp = 12.dp,
    content: @Composable RowScope.() -> Unit,
) {
    CompositionLocalProvider(LocalContentColor provides Color.White) {
        Row(
            modifier = modifier.fillMaxWidth()
                .scrolledTopBarBackground(progress, backgroundColor, strokeColor)
                .statusBarsPadding()
                .height(IridePageLayoutDefaults.BarHeight)
                .clipToBounds()
                .padding(horizontal = horizontalPadding),
            verticalAlignment = Alignment.CenterVertically,
            content = content,
        )
    }
}

@Composable
fun Modifier.irideCompactTitleReveal(progress: Float): Modifier {
    val reducedMotion = rememberReducedMotion()
    return graphicsLayer {
        val reveal = progress.coerceIn(0f, 1f)
        alpha = if (reveal > 0f) 1f else 0f
        translationY = if (reducedMotion) 0f else
            (1f - reveal) * (IridePageLayoutDefaults.BarHeight.toPx() + size.height) / 2f
    }
}
