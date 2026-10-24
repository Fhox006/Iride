package com.metrolist.music.ui.component

import com.metrolist.music.ui.theme.drawFolioRoundRect

import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp as lerpColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import com.metrolist.music.R
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@Stable
internal class IrideFolioDragOverlayState {
    var progress by mutableFloatStateOf(0f)
}

internal val LocalFolioDragOverlay = staticCompositionLocalOf<IrideFolioDragOverlayState?> { null }

/** Shared moving surface for the MP3 player and full-screen Folio stories. */
@Composable
internal fun IrideFolioDragPanel(
    state: BottomSheetState,
    modifier: Modifier = Modifier,
    curtainMode: Boolean = true,
    clickableHeight: Dp = state.collapsedBound,
    isExpandable: Boolean = true,
    expandedTopInset: Dp = 0.dp,
    background: @Composable BoxScope.() -> Unit,
    onDismiss: (() -> Unit)? = null,
    collapsedContent: @Composable BoxScope.() -> Unit = {},
    content: @Composable BoxScope.() -> Unit,
) {
    BottomSheet(
        state = state,
        modifier = modifier,
        clickableHeight = clickableHeight,
        collapsedClickEnabled = !curtainMode,
        isExpandable = isExpandable,
        expandedTopInset = expandedTopInset,
        backgroundFollowsContent = curtainMode,
        background = background,
        onDismiss = onDismiss,
        collapsedContent = collapsedContent,
        content = content,
    )
}

/** Curtain geometry, gestures and closing mark shared with the MP3 player. */
@Composable
internal fun IrideFolioDragOverlay(
    background: Color,
    onClosing: () -> Unit,
    onDismiss: () -> Unit,
    content: @Composable (() -> Unit) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val overlay = LocalFolioDragOverlay.current
    var dismissing by remember { mutableStateOf(false) }
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val peek = maxOf(maxHeight * .14f, 48.dp).coerceAtMost(maxHeight)
        val sheet = rememberBottomSheetState(dismissedBound = 0.dp, expandedBound = maxHeight - peek)
        fun close() {
            if (dismissing) return
            dismissing = true
            onClosing()
            scope.launch { sheet.dismissAndWait(); onDismiss() }
        }
        DisposableEffect(overlay) { onDispose { overlay?.progress = 0f } }
        LaunchedEffect(sheet, overlay) {
            snapshotFlow { sheet.progress }.collect { overlay?.progress = it.coerceIn(0f, 1f) }
        }
        LaunchedEffect(sheet) {
            sheet.expandSoft()
            snapshotFlow { sheet.value }.first { it > 0.dp }
            snapshotFlow { sheet.isDismissed }.first { it }
            if (!dismissing) { dismissing = true; onClosing(); onDismiss() }
        }
        Box(Modifier.fillMaxWidth().height(peek).clickable(onClick = ::close))
        IrideFolioDragPanel(
            state = sheet,
            expandedTopInset = peek,
            background = { Box(Modifier.fillMaxSize().background(background)) },
            collapsedContent = {},
            onDismiss = ::close,
        ) {
            content(::close)
            androidx.activity.compose.BackHandler { close() }
        }
        IrideFolioDragHandle(sheet, maxHeight, true, onClose = ::close)
    }
}

@Composable
internal fun IrideFolioDragHandle(
    state: BottomSheetState,
    screenHeight: Dp,
    isExpandable: Boolean,
    contentColor: Color = Color.White,
    modifier: Modifier = Modifier,
    onClose: (() -> Unit)? = null,
) {
    if (state.isDismissed) return
    val description = stringResource(R.string.mp3_close)
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        modifier = modifier.fillMaxWidth().height(20.dp)
            .graphicsLayer {
                val progress = (state.progress / .25f).coerceIn(0f, 1f)
                val morph = progress * progress * (3f - 2f * progress)
                val centerOffset = lerp(IrideFolio.MiniPlayerDividerOffset.value, -12f, morph).dp
                // One mark follows the sheet: the divider becomes its upper closing handle.
                translationY = (screenHeight - state.value + centerOffset - 10.dp).toPx()
            }
            .then(
                if (isExpandable && !state.isCollapsed) {
                    Modifier.semantics { contentDescription = description }
                        .draggable(
                            state = state,
                            orientation = Orientation.Vertical,
                            onDragStopped = { velocity -> state.performFling(-velocity, null) },
                        )
                        .clickable(
                            interactionSource = interactionSource,
                            indication = null,
                            onClick = { onClose?.invoke() ?: state.collapseSoft() },
                        )
                } else Modifier,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val progress = (state.progress / .25f).coerceIn(0f, 1f)
            val morph = progress * progress * (3f - 2f * progress)
            val collapsedWidth = (size.width - 2f * IrideFolio.MiniPlayerDividerHorizontalInset.toPx())
                .coerceAtLeast(0f)
            val width = lerp(collapsedWidth, 96.dp.toPx().coerceAtMost(collapsedWidth), morph)
            val thickness = lerp(IrideFolio.MiniPlayerDividerStroke.toPx(), 4.dp.toPx(), morph)
            drawFolioRoundRect(
                color = lerpColor(
                    contentColor.copy(alpha = IrideFolio.MiniPlayerDividerOpacity),
                    Color(0xFFC4C4C4),
                    morph,
                ),
                topLeft = Offset((size.width - width) / 2f, (size.height - thickness) / 2f),
                size = Size(width, thickness),
                cornerRadius = CornerRadius(thickness / 2f),
            )
        }
    }
}
