package com.metrolist.music.ui.player

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.metrolist.music.R
import com.metrolist.music.constants.IrideSoftBorderWidth
import com.metrolist.music.extensions.metadata
import com.metrolist.music.models.MediaMetadata
import com.metrolist.music.ui.component.irideFolioPlayerArtworkRequest
import com.metrolist.music.ui.theme.IBMPlexMonoFontFamily
import com.metrolist.music.ui.theme.drawFolioRoundRect
import com.metrolist.music.ui.theme.strokeItemSoft
import com.metrolist.music.ui.utils.irideArtworkOverlayBorder

@Composable
internal fun IrideMp3Progress(
    position: Long,
    duration: Long,
    dragFraction: Float?,
    enabled: Boolean,
    onDragFraction: (Float?) -> Unit,
    onSeek: (Float) -> Unit,
    touchTargetHeight: Dp = 20.dp,
) {
    val progress = (dragFraction ?: if (duration > 0) position.toFloat() / duration else 0f)
        .coerceIn(0f, 1f)
    Box(
        modifier = Modifier.fillMaxWidth().height(touchTargetHeight)
            .semantics {
                progressBarRangeInfo = ProgressBarRangeInfo(progress, 0f..1f)
                if (enabled && duration > 0) {
                    setProgress { value -> onSeek(value.coerceIn(0f, 1f)); true }
                }
            }
            .pointerInput(duration, enabled) {
                if (duration <= 0 || !enabled) return@pointerInput
                awaitEachGesture {
                    val down = awaitFirstDown()
                    down.consume()
                    var fraction = (down.position.x / size.width).coerceIn(0f, 1f)
                    onDragFraction(fraction)
                    try {
                        val released = drag(down.id) { change ->
                            change.consume()
                            fraction = (change.position.x / size.width).coerceIn(0f, 1f)
                            onDragFraction(fraction)
                        }
                        if (released) onSeek(fraction)
                    } finally {
                        onDragFraction(null)
                    }
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxWidth().height(3.dp)) {
            val radius = CornerRadius(size.height / 2f)
            drawFolioRoundRect(Color.White.copy(alpha = 0.18f), cornerRadius = radius)
            if (progress > 0f) {
                drawFolioRoundRect(
                    Color(0xFFE5E5E5),
                    size = Size(size.width * progress.coerceIn(0f, 1f), size.height),
                    cornerRadius = radius,
                )
            }
        }
    }
}

@Composable
internal fun IrideMp3CurrentTrack(
    metadata: MediaMetadata,
    remaining: Long,
    textScale: Float = 1f,
    selected: Boolean = true,
    onClick: () -> Unit,
) {
    val shape = IrideMp3MenuRowShape
    Row(
        Modifier.fillMaxWidth().clip(shape)
            .background(if (selected) IrideMp3SurfaceColor else Color.Transparent)
            .border(
                0.5.dp,
                if (selected) IrideMp3PanelBorderColor else Color.Transparent,
                shape,
            )
            .selectable(selected = selected, role = Role.Button, onClick = onClick)
            .padding(8.dp * textScale),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AsyncImage(
            model = irideFolioPlayerArtworkRequest(LocalContext.current, metadata.thumbnailUrl),
            contentDescription = null, contentScale = ContentScale.Crop,
            modifier = Modifier.size(52.dp * textScale)
                .irideArtworkOverlayBorder(IrideSoftBorderWidth, MaterialTheme.colorScheme.strokeItemSoft, IrideMp3ArtworkShape)
                .clip(IrideMp3ArtworkShape),
        )
        Column(Modifier.weight(1f).padding(start = 7.dp * textScale)) {
            Text(
                stringResource(R.string.mp3_now_playing), color = Color.White,
                fontFamily = IBMPlexMonoFontFamily, fontWeight = FontWeight.Bold,
                fontSize = (13 * textScale).sp, lineHeight = (18 * textScale).sp,
                maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
            Text(
                metadata.title, color = Color.White.copy(alpha = 0.55f),
                fontFamily = IBMPlexMonoFontFamily, fontWeight = FontWeight.SemiBold,
                fontSize = (11 * textScale).sp, lineHeight = (15 * textScale).sp,
                maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
            Text(
                stringResource(
                    R.string.mp3_time_left,
                    (remaining.coerceAtLeast(0) + 999) / 1000,
                ),
                color = Color.White.copy(alpha = 0.55f),
                fontFamily = IBMPlexMonoFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = (11 * textScale).sp,
                lineHeight = (15 * textScale).sp,
            )
        }
        Icon(
            painterResource(R.drawable.expand_more),
            contentDescription = null,
            tint = if (selected) Color.White else IrideMp3DimIconColor,
            modifier = Modifier.size(26.dp * textScale).graphicsLayer { rotationZ = -90f },
        )
    }
}
