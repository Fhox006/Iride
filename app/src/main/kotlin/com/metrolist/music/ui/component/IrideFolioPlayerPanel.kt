package com.metrolist.music.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import com.metrolist.music.R

/** The inline footer itself grows to the screen edges; its content never changes renderer. */
@Composable
internal fun IrideFolioPlayerPanel(
    expansion: Float,
    opacity: Float,
    bottomInset: Dp,
    onReveal: () -> Unit,
    transportHeight: Dp,
    info: @Composable () -> Unit,
    transport: @Composable () -> Unit,
) {
    val radius = IrideFolio.PlayerPanelCornerRadius * expansion
    val radiusPx = with(LocalDensity.current) { radius.toPx() }
    val panelShape =
        remember(radiusPx) {
            GenericShape { size, _ ->
                val r = radiusPx.coerceAtMost(minOf(size.width / 2f, size.height / 2f))
                moveTo(0f, 0f)
                cubicTo(0f, r * .552285f, r * .447715f, r, r, r)
                lineTo(size.width - r, r)
                cubicTo(size.width - r * .447715f, r, size.width, r * .552285f, size.width, 0f)
                lineTo(size.width, size.height)
                lineTo(0f, size.height)
                close()
            }
        }
    Box(Modifier.fillMaxWidth()) {
        Column(
            Modifier.fillMaxWidth()
                .then(if (opacity < .5f) Modifier.clearAndSetSemantics {} else Modifier)
                .graphicsLayer { alpha = opacity }
                .clip(panelShape)
                .background(Color.Black.copy(alpha = expansion))
                .padding(horizontal = IrideFolio.PlayerDisplayInset * expansion)
                .padding(bottom = bottomInset * expansion)
        ) {
            Spacer(Modifier.height(IrideFolio.PlayerViewportFooterGap + radius))
            info()
            Box(
                Modifier.fillMaxWidth()
                    .height((transportHeight + IrideFolio.PlayerTransportSpacing) * expansion)
                    .clipToBounds()
                    .graphicsLayer { alpha = expansion },
                contentAlignment = Alignment.BottomCenter,
            ) {
                transport()
            }
        }
        val controlsLabel = stringResource(R.string.mp3_playback_controls)
        if (opacity < 0.5f)
            Box(
                Modifier.matchParentSize()
                    .semantics { contentDescription = controlsLabel }
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onReveal,
                    )
            )
    }
}
