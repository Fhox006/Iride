package com.metrolist.music.ui.player

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.Timeline
import coil3.compose.AsyncImage
import com.metrolist.music.LocalPlayerConnection
import com.metrolist.music.R
import com.metrolist.music.constants.IrideSoftBorderWidth
import com.metrolist.music.extensions.metadata
import com.metrolist.music.ui.component.irideFolioPlayerArtworkRequest
import com.metrolist.music.ui.component.irideFolioGestures
import com.metrolist.music.ui.theme.IBMPlexMonoFontFamily
import com.metrolist.music.ui.theme.strokeItemSoft
import com.metrolist.music.ui.utils.IrideMotion
import com.metrolist.music.ui.utils.irideArtworkOverlayBorder
import com.metrolist.music.ui.utils.rememberReducedMotion

@Composable
internal fun IrideMp3Menu(
    mediaMetadata: com.metrolist.music.models.MediaMetadata,
    remaining: Long,
    selectedItem: Int,
    textScale: Float,
    showLyrics: Boolean,
    onSelect: (Int) -> Unit,
) {
    val playerConnection = LocalPlayerConnection.current
    val queueWindows by playerConnection?.queueWindows?.collectAsState()
        ?: remember { mutableStateOf(emptyList<Timeline.Window>()) }
    val queueIndex by playerConnection?.currentWindowIndex?.collectAsState()
        ?: remember { mutableStateOf(0) }
    val songsLeft = (queueWindows.size - queueIndex - 1).coerceAtLeast(0)
    val labels = listOf(R.string.mp3_now_playing, R.string.mp3_manage_queue, R.string.mp3_watch_track_video, R.string.mp3_read_lyrics, R.string.mp3_song_info, R.string.comments, R.string.listen_together)
    val menuEntries = IrideMp3MenuOrder.drop(1).filter { showLyrics || it != 3 }
    val upcoming = remember(queueWindows, queueIndex) {
        queueWindows.drop((queueIndex + 1).coerceAtLeast(0)).take(3)
    }
    val videoState by playerConnection?.videoPlayback?.state?.collectAsState()
        ?: remember { mutableStateOf(com.metrolist.music.playback.VideoPlaybackState()) }
    val listState = rememberLazyListState()
    val reducedMotion = rememberReducedMotion()
    LaunchedEffect(selectedItem, menuEntries) {
        val scrollIndex = menuEntries.indexOf(selectedItem)
        if (scrollIndex >= 0 && listState.layoutInfo.visibleItemsInfo.none { it.index == scrollIndex }) {
            if (reducedMotion) listState.scrollToItem(scrollIndex)
            else listState.animateScrollToItem(scrollIndex)
        }
    }
    @Composable fun MenuEntry(index: Int) {
        if (index == 0) {
            IrideMp3CurrentTrack(
                videoState.audioMetadata ?: mediaMetadata,
                remaining,
                textScale,
                selectedItem == 0,
                onClick = { onSelect(0) },
            )
            Spacer(Modifier.height(4.dp))
            return
        }
        val selected = selectedItem == index
        val selectionColor by animateColorAsState(
            if (selected) IrideMp3SurfaceColor else Color.Transparent,
            tween(if (reducedMotion) 0 else IrideMotion.Quick),
            label = "mp3MenuSelection",
        )
        Row(
            modifier = Modifier.fillMaxWidth()
                .clip(IrideMp3MenuRowShape)
                .background(selectionColor)
                .border(
                    0.5.dp,
                    if (selected) Color.White.copy(alpha = 0.14f) else Color.Transparent,
                    IrideMp3MenuRowShape,
                )
                .selectable(selected = selected, role = Role.Button, onClick = { onSelect(index) })
                .padding(
                    horizontal = 8.dp * textScale,
                    vertical = (if (index == 1) 5.dp else 3.dp) * textScale,
                ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val artworkSize = 52.dp * textScale
            Box(
                Modifier.size(
                    width = artworkSize,
                    height = if (index == 1) artworkSize else 36.dp * textScale,
                ),
                contentAlignment = Alignment.CenterStart,
            ) {
                if (index == 1) {
                    IrideMp3QueueArtwork(upcoming, textScale)
                } else {
                    Icon(
                        painterResource(
                            when (index) {
                                2 -> R.drawable.ic_iride_play
                                3 -> R.drawable.ic_iride_quote
                                5 -> R.drawable.comment
                                6 -> R.drawable.group
                                else -> R.drawable.search
                            }
                        ),
                        null,
                        tint = Color.White,
                        modifier = Modifier.align(Alignment.Center).size(26.dp * textScale),
                    )
                }
            }
            Spacer(Modifier.width(7.dp * textScale))
            Column(Modifier.weight(1f)) {
                Text(
                    text = stringResource(labels[index]),
                    color = Color.White,
                    fontFamily = IBMPlexMonoFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = (14 * textScale).sp,
                    lineHeight = (20 * textScale).sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (index == 1) {
                    Text(
                        text = stringResource(R.string.mp3_queue_description),
                        color = Color.White.copy(alpha = 0.55f),
                        fontFamily = IBMPlexMonoFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = (11 * textScale).sp,
                        lineHeight = (15 * textScale).sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = stringResource(R.string.mp3_songs_left, songsLeft),
                        color = Color.White.copy(alpha = 0.55f),
                        fontFamily = IBMPlexMonoFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = (11 * textScale).sp,
                        lineHeight = (15 * textScale).sp,
                    )
                }
            }
            Icon(
                painter = painterResource(R.drawable.expand_more),
                contentDescription = null,
                tint = if (selected) Color.White else Color(0xFF666666),
                modifier = Modifier.size(26.dp * textScale)
                    .graphicsLayer { rotationZ = if (index == 1) -90f else -135f },
            )
        }
        Spacer(Modifier.height(4.dp))
    }
    Column(Modifier.fillMaxSize().padding(1.dp)) {
        MenuEntry(0)
        LazyColumn(Modifier.weight(1f).fillMaxWidth().clipToBounds().irideFolioGestures(listState), state = listState, overscrollEffect = null) {
            items(menuEntries, key = { it }) { index -> MenuEntry(index) }
        }
    }
}

@Composable
private fun IrideMp3QueueArtwork(upcoming: List<Timeline.Window>, textScale: Float) {
    val context = LocalContext.current
    Box(Modifier.size(52.dp * textScale), contentAlignment = Alignment.CenterStart) {
        if (upcoming.isEmpty()) {
            Icon(
                painterResource(R.drawable.queue_music),
                contentDescription = null,
                tint = IrideMp3DimIconColor,
                modifier = Modifier.align(Alignment.Center).size(28.dp * textScale),
            )
        }
        upcoming.asReversed().forEachIndexed { reversed, window ->
            val depth = upcoming.lastIndex - reversed
            AsyncImage(
                model = irideFolioPlayerArtworkRequest(context, window.mediaItem.metadata?.thumbnailUrl),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .offset(x = 6.dp * depth * textScale, y = (-2).dp * depth * textScale)
                    .size(46.dp * textScale)
                    .graphicsLayer {
                        val scale = 1f - depth * 0.10f
                        scaleX = scale
                        scaleY = scale
                        rotationY = -18f
                        cameraDistance = 10f * size.width
                        transformOrigin = TransformOrigin(0f, 0.5f)
                        shadowElevation = 2.dp.toPx()
                        shape = IrideMp3ArtworkShape
                    }
                    .clip(IrideMp3ArtworkShape)
                    .background(IrideMp3BackgroundColor)
                    .irideArtworkOverlayBorder(
                        IrideSoftBorderWidth,
                        MaterialTheme.colorScheme.strokeItemSoft,
                        IrideMp3ArtworkShape,
                    ),
            )
        }
    }
}
