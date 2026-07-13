/**
 * Iride Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.player

import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp as lerpColor
import androidx.compose.ui.unit.lerp as lerpTextUnit
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.toSize
import androidx.compose.ui.util.lerp
import coil3.compose.AsyncImage
import com.metrolist.music.LocalPlayerConnection
import com.metrolist.music.R
import com.metrolist.music.models.MediaMetadata
import com.metrolist.music.ui.theme.InterFontFamily
import com.metrolist.music.utils.makeTimeString
import kotlin.math.roundToInt

internal val IrideMp3BackgroundColor = Color(0xFF0D0D0F)
private val IrideMp3WheelTopColor = Color(0xFF17171A)
private val IrideMp3WheelBottomColor = Color(0xFF09090A)
private val IrideMp3BorderColor = Color.White.copy(alpha = 0.08f)

// Cover width as a fraction of the player's width — title/artist/progress below share this same
// width so they line up with the cover's edges instead of using their own independent margin.
private const val IrideMp3CoverWidthFraction = 0.82f

/**
 * Shared geometry bridge between the New Iride UI's collapsed miniplayer (curtain peek strip)
 * and the expanded [IrideMp3PlayerContent]. Both sides report the on-screen (window-space) rect
 * of their cover art and title/artist block here — each side hides its own copy
 * ([irideReportRect] callers pair a non-null lambda with `alpha(0f)`) and
 * [IrideMiniPlayerBridgeOverlay] draws a single moving instance of each that interpolates between
 * the two reported rects as the bottom sheet drags/animates. The title crossfades between its
 * mini (Inter) and expanded (Monospace) fonts as it moves; the progress indicator and the
 * play/skip/favorite buttons are left as real duplicates in each layout and just cross-fade.
 */
@Stable
class IrideBridgeState {
    var miniArt by mutableStateOf<Rect?>(null)
    var playerArt by mutableStateOf<Rect?>(null)
    var miniInfo by mutableStateOf<Rect?>(null)
    var playerInfo by mutableStateOf<Rect?>(null)
}

internal fun Modifier.irideReportRect(target: (Rect) -> Unit): Modifier =
    this.onGloballyPositioned { target(Rect(it.positionInWindow(), it.size.toSize())) }

// Deliberately no easing curve here: the cover must track the drag 1:1 with the manual sheet
// progress. An eased curve (previously a slow-fast-slow CubicBezier) looked smooth on a
// programmatic expand/collapse animation, but under a real drag it desynced from the finger — the
// cover raced ahead of the touch point around the midpoint, then sat still waiting near the end.

/**
 * The "New Iride UI" expanded player: an old-school MP3-player-styled layout with a square
 * cover, typewriter title/artist, a thick progress bar, and an iPod-style click wheel. The wheel
 * sits high below the cover, and a bottom row holds lyrics/more
 * (⋮)/favorite as a remote-control strip — all three share one height, none stacked above another.
 * Only play/pause is wired to real playback — the other wheel zones are visual/no-op for now.
 */
@Composable
fun IrideMp3PlayerContent(
    mediaMetadata: MediaMetadata,
    position: Long,
    duration: Long,
    isPlaying: Boolean,
    isFavorite: Boolean,
    onPlayPauseClick: () -> Unit,
    onPreviousClick: () -> Unit = {},
    onNextClick: () -> Unit = {},
    onQueueClick: () -> Unit = {},
    onRadioClick: () -> Unit = {},
    onFavoriteClick: () -> Unit = {},
    onMoreClick: () -> Unit = {},
    onLyricsClick: () -> Unit = {},
    // Fraction (0f-1f) of the tapped position along the progress bar's width.
    onSeek: (Float) -> Unit = {},
    onArtistClick: (String) -> Unit = {},
    modifier: Modifier = Modifier,
    bottomInset: Dp = 0.dp,
    // Extra height this composable's own background reaches above its normal top edge (the caller
    // shifts the whole content up by this much via BottomSheet's contentTopPadding) — covers the
    // strip the app layer's rounded corner cuts into when fully expanded, which otherwise has
    // nothing curtain-colored behind it. The inner column and the top-start corner button are
    // padded back down by the same amount so the visible layout doesn't shift.
    cornerRevealHeight: Dp = 0.dp,
    // New Iride UI bridge: when set, this player's own cover/title-artist/top-bar are hidden
    // (alpha 0, still laid out so their rects can be reported) and drawn instead by
    // IrideMiniPlayerBridgeOverlay, which morphs them in from the collapsed miniplayer instead of
    // cross-fading a duplicate copy in.
    bridgeState: IrideBridgeState? = null,
) {
    // Own the full bottom-sheet area — the old player's background must never peek through.
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(IrideMp3BackgroundColor)
            .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Top)),
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
        ) {
            Spacer(Modifier.height(cornerRevealHeight))

            Box(
                modifier = Modifier
                    .fillMaxWidth(IrideMp3CoverWidthFraction)
                    .padding(top = 8.dp, bottom = 8.dp)
                    .aspectRatio(1f)
                    .align(Alignment.CenterHorizontally)
                    .then(
                        if (bridgeState != null) {
                            Modifier.irideReportRect { bridgeState.playerArt = it }.alpha(0f)
                        } else {
                            Modifier
                        },
                    )
                    .clip(RoundedCornerShape(16.dp)),
            ) {
                AsyncImage(
                    model = mediaMetadata.thumbnailUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            }

            // Same width as the cover above, so title/artist/progress line up with its edges
            // instead of using their own independent side margin.
            Column(
                modifier = Modifier
                    .fillMaxWidth(IrideMp3CoverWidthFraction)
                    .align(Alignment.CenterHorizontally),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(
                            if (bridgeState != null) {
                                Modifier.irideReportRect { bridgeState.playerInfo = it }.alpha(0f)
                            } else {
                                Modifier
                            },
                        ),
                ) {
                    Text(
                        text = mediaMetadata.title,
                        color = Color.White,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.basicMarquee(iterations = 1, initialDelayMillis = 2000),
                    )
                    IrideArtistText(
                        mediaMetadata = mediaMetadata,
                        color = Color.Gray,
                        fontSize = 12.sp,
                        onArtistClick = onArtistClick,
                    )
                }

                Spacer(Modifier.height(14.dp))

                val progress = if (duration > 0) (position.toFloat() / duration).coerceIn(0f, 1f) else 0f
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(5.dp)
                        .clip(RoundedCornerShape(50))
                        .background(Color.White.copy(alpha = 0.15f))
                        .pointerInput(duration) {
                            detectTapGestures { offset ->
                                if (duration > 0) onSeek((offset.x / size.width).coerceIn(0f, 1f))
                            }
                        },
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(progress)
                            .clip(RoundedCornerShape(50))
                            .background(Color.White),
                    )
                }

                Spacer(Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = makeTimeString(position),
                        color = Color.White.copy(alpha = 0.5f),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                    )
                    Text(
                        text = makeTimeString(duration),
                        color = Color.White.copy(alpha = 0.5f),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                IrideClickWheel(
                    isPlaying = isPlaying,
                    onPlayPauseClick = onPlayPauseClick,
                    onPreviousClick = onPreviousClick,
                    onNextClick = onNextClick,
                    onQueueClick = onQueueClick,
                    onRadioClick = onRadioClick,
                    wheelSize = 224.dp,
                )
            }

            // Fills remaining space so the wheel sits high and the remote-control row below lands
            // at the true screen bottom.
            Spacer(Modifier.weight(1f))

            // Remote-control row: lyrics, more (⋮) and favorite all share one height — none
            // stacked above another.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 8.dp + bottomInset),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SideButton(icon = R.drawable.lyrics, onClick = onLyricsClick)
                SideButton(icon = R.drawable.more_vert, onClick = onMoreClick)
                SideButton(
                    icon = if (isFavorite) R.drawable.favorite else R.drawable.favorite_border,
                    onClick = onFavoriteClick,
                )
            }
        }
    }
}

/**
 * Artist line for the New Iride UI: same look as a plain Text, but each artist name is tappable
 * and navigates to that artist's screen — mirrors the tap-position-to-annotation approach the
 * classic player already uses for its own artist line.
 */
@Composable
internal fun IrideArtistText(
    mediaMetadata: MediaMetadata,
    color: Color,
    fontSize: TextUnit,
    onArtistClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    fontFamily: FontFamily = InterFontFamily,
) {
    val annotated = remember(mediaMetadata.artists) {
        buildAnnotatedString {
            mediaMetadata.artists.forEachIndexed { index, artist ->
                pushStringAnnotation(tag = "artist", annotation = artist.id.orEmpty())
                append(artist.name)
                pop()
                if (index != mediaMetadata.artists.lastIndex) append(", ")
            }
        }
    }
    var layoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }
    var tapPosition by remember { mutableStateOf<Offset?>(null) }

    Text(
        text = annotated,
        color = color,
        fontFamily = fontFamily,
        fontSize = fontSize,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        onTextLayout = { layoutResult = it },
        modifier = modifier
            .pointerInput(annotated) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        event.changes.firstOrNull()?.position?.let { tapPosition = it }
                    }
                }
            }
            .combinedClickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick = {
                    val layout = layoutResult ?: return@combinedClickable
                    val pos = tapPosition ?: return@combinedClickable
                    val charOffset = layout.getOffsetForPosition(pos)
                    annotated.getStringAnnotations("artist", charOffset, charOffset)
                        .firstOrNull()
                        ?.let { ann -> if (ann.item.isNotBlank()) onArtistClick(ann.item) }
                },
            ),
    )
}

@Composable
private fun IrideClickWheel(
    isPlaying: Boolean,
    onPlayPauseClick: () -> Unit,
    onPreviousClick: () -> Unit,
    onNextClick: () -> Unit,
    onQueueClick: () -> Unit,
    onRadioClick: () -> Unit,
    wheelSize: Dp = 184.dp,
) {
    Box(
        // requiredSize ignores incoming parent constraints, so the wheel is always a perfect
        // circle — plain size() would shrink into an ellipse if the parent's width got squeezed.
        modifier = Modifier.requiredSize(wheelSize),
        contentAlignment = Alignment.Center,
    ) {
        // Outer ring — dark dial, rim-lit from above like light falling on it from overhead.
        Box(
            modifier = Modifier
                .requiredSize(wheelSize)
                .clip(CircleShape)
                .background(Brush.verticalGradient(listOf(IrideMp3WheelTopColor, IrideMp3WheelBottomColor)))
                .border(
                    width = 1.dp,
                    brush = Brush.verticalGradient(
                        listOf(
                            Color.White.copy(alpha = 0.22f),
                            Color.White.copy(alpha = 0.05f),
                            Color.White.copy(alpha = 0f),
                        ),
                    ),
                    shape = CircleShape,
                ),
        ) {
            WheelZone(icon = R.drawable.radio, alignment = Alignment.TopCenter, onClick = onRadioClick)
            WheelZone(icon = R.drawable.skip_next, alignment = Alignment.CenterEnd, onClick = onNextClick)
            WheelZone(icon = R.drawable.queue_music, alignment = Alignment.BottomCenter, onClick = onQueueClick)
            WheelZone(icon = R.drawable.skip_previous, alignment = Alignment.CenterStart, onClick = onPreviousClick)
        }

        // Center hole — play/pause. The only wired control for now.
        Box(
            modifier = Modifier
                .size(88.dp)
                .clip(CircleShape)
                .background(IrideMp3BackgroundColor)
                .border(1.dp, IrideMp3BorderColor, CircleShape)
                .clickable { onPlayPauseClick() },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(if (isPlaying) R.drawable.pause else R.drawable.play),
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(34.dp),
            )
        }
    }
}

@Composable
private fun BoxScope.WheelZone(
    icon: Int,
    alignment: Alignment,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .align(alignment)
            .padding(15.dp)
            .size(44.dp)
            .clip(CircleShape)
            .clickable { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.7f),
            modifier = Modifier.size(22.dp),
        )
    }
}

@Composable
private fun SideButton(
    icon: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(46.dp)
            .clip(CircleShape)
            .background(IrideMp3BackgroundColor)
            .border(1.dp, IrideMp3BorderColor, CircleShape)
            .clickable { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.75f),
            modifier = Modifier.size(20.dp),
        )
    }
}

/**
 * Draws the cover art for the New Iride UI as a single moving instance, positioned every frame by
 * interpolating between the mini/player rects [IrideBridgeState] last reported. This is what makes
 * the cover *move* between the two layouts instead of cross-fading a duplicate copy in/out. Title,
 * artist, the progress indicator, and the play/skip/favorite buttons are all left as real
 * duplicates in each layout and keep cross-fading — only the cover gets the morph treatment.
 */
@Composable
fun IrideMiniPlayerBridgeOverlay(
    bridgeState: IrideBridgeState,
    sheetProgress: Float,
    modifier: Modifier = Modifier,
) {
    val playerConnection = LocalPlayerConnection.current ?: return
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()
    val metadata = mediaMetadata ?: return
    val eased = sheetProgress.coerceIn(0f, 1f)
    var rootOffset by remember { mutableStateOf(Offset.Zero) }

    // Text takes the second half of the drag (IrideCoverTextSplit -> 1), staying put at its mini
    // position until the cover has already landed. Linear (plain lerp, no easing). The cover itself
    // keeps the full 0 -> 1 range (see BridgedElement below) — only the text is held back, so it
    // never crosses paths with the cover mid-drag.
    val textProgress = ((eased - IrideCoverTextSplit) / (1f - IrideCoverTextSplit)).coerceIn(0f, 1f)

    Box(
        modifier = modifier
            .fillMaxSize()
            .onGloballyPositioned { rootOffset = it.positionInWindow() },
    ) {
        // Falls back to whichever side is known when the other hasn't been measured yet (e.g. cold
        // start at rest, before the expanded content has ever composed) — draws statically at the
        // one known rect instead of not drawing at all, which would otherwise blank out the art.
        val artStart = bridgeState.miniArt ?: bridgeState.playerArt
        val artEnd = bridgeState.playerArt ?: artStart
        if (artStart != null && artEnd != null) {
            BridgedElement(start = artStart, end = artEnd, rootOffset = rootOffset, progress = eased) {
                AsyncImage(
                    model = metadata.thumbnailUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(lerp(14f, 16f, eased).dp)),
                )
            }
        }

        val infoStart = bridgeState.miniInfo ?: bridgeState.playerInfo
        val infoEnd = bridgeState.playerInfo ?: infoStart
        if (infoStart != null && infoEnd != null) {
            BridgedInfoBlock(
                metadata = metadata,
                start = infoStart,
                end = infoEnd,
                rootOffset = rootOffset,
                progress = textProgress,
            )
        }
    }
}

// Fraction of the drag before the text starts moving — see the comment where this is used.
private const val IrideCoverTextSplit = 0.5f

/**
 * Moves the title/artist block between the collapsed miniplayer and expanded player positions —
 * position/width only (no graphicsLayer scale, which would blur text). The title is drawn as two
 * overlaid copies (mini's Inter, expanded's Monospace) cross-fading via alpha as they travel, since
 * a font family change can't be interpolated directly; the artist line keeps one Inter copy and
 * just lerps its color/size, since its font never changes.
 */
@Composable
private fun BridgedInfoBlock(
    metadata: MediaMetadata,
    start: Rect,
    end: Rect,
    rootOffset: Offset,
    progress: Float,
) {
    val density = LocalDensity.current
    val startLocal = remember(start, rootOffset) { start.translate(-rootOffset.x, -rootOffset.y) }
    val endLocal = remember(end, rootOffset) { end.translate(-rootOffset.x, -rootOffset.y) }
    val left = lerp(startLocal.left, endLocal.left, progress)
    val top = lerp(startLocal.top, endLocal.top, progress)
    val width = lerp(startLocal.width, endLocal.width, progress)

    val miniTitleColor = MaterialTheme.colorScheme.onSurface
    val miniArtistColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)

    Column(
        modifier = Modifier
            .offset { IntOffset(left.roundToInt(), top.roundToInt()) }
            .width(with(density) { width.toDp() }),
    ) {
        // No crossfade: mini (Inter) font holds for the whole move, swapped for the expanded
        // (Monospace) font only on the last frame (progress == 1) — a blended crossfade between two
        // different font families reads as a garbled double-exposure mid-transition.
        if (progress < 1f) {
            Text(
                text = metadata.title,
                color = miniTitleColor,
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.Medium,
                fontSize = lerpTextUnit(14.sp, 16.sp, progress),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        } else {
            Text(
                text = metadata.title,
                color = Color.White,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (metadata.artists.any { it.name.isNotBlank() }) {
            Text(
                text = metadata.artists.joinToString(", ") { it.name },
                color = lerpColor(miniArtistColor, Color.Gray, progress),
                fontFamily = InterFontFamily,
                fontSize = lerpTextUnit(12.sp, 12.sp, progress),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/**
 * Lays out [content] pinned to [end]'s position/size (its natural, expanded-player size), then
 * scales+translates it toward [start] via graphicsLayer as [progress] goes 1 -> 0. Doing the morph
 * with graphicsLayer instead of re-measuring at a lerped size every frame keeps it to a draw-phase
 * transform (cheap, 120Hz-friendly) rather than a layout pass.
 */
@Composable
private fun BridgedElement(
    start: Rect,
    end: Rect,
    rootOffset: Offset,
    progress: Float,
    content: @Composable BoxScope.() -> Unit,
) {
    val density = LocalDensity.current
    val endLocal = remember(end, rootOffset) { end.translate(-rootOffset.x, -rootOffset.y) }
    Box(
        modifier = Modifier
            .offset { IntOffset(endLocal.left.roundToInt(), endLocal.top.roundToInt()) }
            .size(with(density) { DpSize(endLocal.width.toDp(), endLocal.height.toDp()) })
            .graphicsLayer {
                scaleX = if (end.width > 0f) lerp(start.width / end.width, 1f, progress) else 1f
                scaleY = if (end.height > 0f) lerp(start.height / end.height, 1f, progress) else 1f
                transformOrigin = TransformOrigin(0f, 0f)
                translationX = lerp(start.left, end.left, progress) - end.left
                translationY = lerp(start.top, end.top, progress) - end.top
            },
        content = content,
    )
}
