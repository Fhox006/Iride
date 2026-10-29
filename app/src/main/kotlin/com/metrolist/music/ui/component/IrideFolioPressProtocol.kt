package com.metrolist.music.ui.component

import androidx.compose.foundation.Indication
import androidx.compose.foundation.IndicationNodeFactory
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.indication
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.node.DelegatableNode
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import androidx.compose.ui.node.currentValueOf
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.semantics.Role
import com.metrolist.music.ui.utils.rememberReducedMotion
import kotlinx.coroutines.launch

// MaterialTheme replaces LocalIndication. This local survives nested shelf/page themes.
internal val LocalIrideFolioPressIndication = staticCompositionLocalOf<IndicationNodeFactory> {
    IrideFolioPressIndication(reducedMotion = false)
}

/**
 * Couples long-press anticipation to the menu that the callback actually opens.
 * Compression wraps the whole modifier chain: artwork, text, backgrounds, clipping and strokes
 * must shrink together, including decorations applied before this modifier.
 */
@Composable
fun Modifier.irideFolioCombinedClickable(
    enabled: Boolean = true,
    onClickLabel: String? = null,
    role: Role? = null,
    onLongClickLabel: String? = null,
    onLongClick: (() -> Unit)? = null,
    onDoubleClick: (() -> Unit)? = null,
    hapticFeedbackEnabled: Boolean = true,
    interactionSource: MutableInteractionSource? = null,
    indication: Indication? = if (onLongClick != null) {
        LocalIrideFolioPressIndication.current
    } else {
        LocalIndication.current
    },
    onClick: () -> Unit,
): Modifier {
    val protocol = indication as? IrideFolioPressIndication
    if (protocol == null || onLongClick == null) {
        return combinedClickable(
            interactionSource = interactionSource,
            indication = indication,
            enabled = enabled,
            onClickLabel = onClickLabel,
            role = role,
            onLongClickLabel = onLongClickLabel,
            onLongClick = onLongClick,
            onDoubleClick = onDoubleClick,
            hapticFeedbackEnabled = hapticFeedbackEnabled,
            onClick = onClick,
        )
    }
    val timeout = LocalViewConfiguration.current.longPressTimeoutMillis
    val scope = rememberCoroutineScope()
    val menu = LocalMenuState.current
    val motion = remember(protocol, timeout) { IrideFolioContextMotion() }
    val boundIndication = remember(protocol, motion, timeout) {
        FolioContextIndication(motion, timeout, protocol.reducedMotion)
    }
    val pressInteractions = interactionSource ?: remember { MutableInteractionSource() }
    val feedback =
        onGloballyPositioned { motion.sourceBounds = it.boundsInWindow() }
            .pointerInput(motion) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                    motion.recordDown(down.uptimeMillis, down.position)
                }
            }
    return Modifier.indication(pressInteractions, boundIndication).then(feedback).combinedClickable(
        interactionSource = pressInteractions,
        indication = null,
        enabled = enabled,
        onClickLabel = onClickLabel,
        role = role,
        onLongClickLabel = onLongClickLabel,
        onLongClick = {
            motion.commit()
            val previousMotion = menu.pendingPressMotion
            menu.pendingPressMotion = motion
            try {
                onLongClick()
            } finally {
                menu.pendingPressMotion = previousMotion
                // Selection and other long-press actions may not open a panel.
                if (menu.popupMotion !== motion && !motion.awaitingMenu) {
                    motion.release(scope, protocol.reducedMotion, force = true)
                }
            }
        },
        onDoubleClick = onDoubleClick,
        hapticFeedbackEnabled = hapticFeedbackEnabled,
        onClick = onClick,
    )
}

/** Folio touch protocol: compress toward the finger while held, then settle elastically. */
@Composable
internal fun rememberIrideFolioPressIndication(): IndicationNodeFactory {
    val reducedMotion = rememberReducedMotion()
    return remember(reducedMotion) { IrideFolioPressIndication(reducedMotion) }
}

private data class IrideFolioPressIndication(
    val reducedMotion: Boolean,
) : IndicationNodeFactory {
    override fun create(interactionSource: InteractionSource): DelegatableNode =
        FolioContextPressNode(interactionSource, IrideFolioContextMotion(), null, reducedMotion)
}

private data class FolioContextIndication(
    val motion: IrideFolioContextMotion,
    val timeoutMillis: Long,
    val reducedMotion: Boolean,
) : IndicationNodeFactory {
    override fun create(interactionSource: InteractionSource): DelegatableNode =
        FolioContextPressNode(interactionSource, motion, timeoutMillis, reducedMotion)
}

private class FolioContextPressNode(
    private val interactionSource: InteractionSource,
    private val motion: IrideFolioContextMotion,
    private val timeoutMillis: Long?,
    private val reducedMotion: Boolean,
) : Modifier.Node(), DrawModifierNode, CompositionLocalConsumerModifierNode {
    override fun onAttach() {
        val timeout = timeoutMillis ?: currentValueOf(LocalViewConfiguration).longPressTimeoutMillis
        motion.release(coroutineScope, reducedMotion)
        coroutineScope.launch {
            interactionSource.interactions.collect { interaction ->
                when (interaction) {
                    is PressInteraction.Press -> motion.press(coroutineScope, timeout, reducedMotion)
                    is PressInteraction.Release, is PressInteraction.Cancel -> motion.release(coroutineScope, reducedMotion)
                }
            }
        }
    }

    override fun ContentDrawScope.draw() {
        val compression = if (reducedMotion) 0f else IrideFolioContextMotionSpec.SourceCompression
        val scale = (1f - compression * motion.frame.value.pressure).coerceIn(0.9f, 1.015f)
        scale(scale, pivot = center) { this@draw.drawContent() }
    }
}
