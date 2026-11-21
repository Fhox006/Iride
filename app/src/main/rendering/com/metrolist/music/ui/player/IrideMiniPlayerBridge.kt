package com.metrolist.music.ui.player

import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp as lerpColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import com.metrolist.music.LocalDatabase
import com.metrolist.music.LocalPlayerConnection
import com.metrolist.music.R
import com.metrolist.music.extensions.metadata
import com.metrolist.music.models.MediaMetadata
import com.metrolist.music.ui.component.BottomSheetState
import com.metrolist.music.ui.component.LocalBottomSheetPageState
import com.metrolist.music.ui.component.LocalMenuState
import com.metrolist.music.ui.component.PillCoverRadius
import com.metrolist.music.ui.component.SongMenuIconButton
import com.metrolist.music.ui.component.irideFolioPlayerArtworkRequest
import com.metrolist.music.ui.menu.PlayerMenu
import com.metrolist.music.ui.theme.InterFontFamily
import com.metrolist.music.ui.utils.ShowMediaInfo
import com.metrolist.music.ui.utils.rememberReducedMotion
import kotlin.math.roundToInt

@Composable
fun IrideMiniPlayerBridgeOverlay(
    bridgeState: IrideBridgeState,
    sheetProgress: Float,
    navController: NavController,
    playerBottomSheetState: BottomSheetState,
    modifier: Modifier = Modifier,
) {
    val playerConnection = LocalPlayerConnection.current ?: return
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()
    val metadata = mediaMetadata ?: return
    // Reduced motion: the container-transform morph becomes an instant cut between the mini and
    // player layouts instead of tracking drag position — snapping `eased` to the nearer endpoint
    // collapses every lerp below to that endpoint's value with no interpolation drawn.
    val reducedMotion = rememberReducedMotion()
    val eased =
        if (reducedMotion) {
            if (sheetProgress >= 0.5f) 1f else 0f
        } else {
            sheetProgress.coerceIn(0f, 1f)
        }
    var rootOffset by remember { mutableStateOf(Offset.Zero) }

    val textProgress = ((eased - IrideCoverTextSplit) / (1f - IrideCoverTextSplit)).coerceIn(0f, 1f)

    Box(
        modifier =
            modifier.fillMaxSize().onGloballyPositioned { rootOffset = it.positionInWindow() }
    ) {
        val artStart = bridgeState.miniArt
        val artEnd = bridgeState.playerArt
        if (artStart != null && artEnd != null && !bridgeState.panelActive) {
            // Fades out fast right at the start of the move instead of hanging around for the
            // whole morph — the mini pill's own ring (PillPlayButton) takes back over the instant
            // this one is gone, so it must never be caught visible mid-air with no ring anywhere.
            val ringAlpha = (1f - eased / BridgeRingFadeOutProgress).coerceIn(0f, 1f)
            BridgedElement(
                start = artStart,
                end = artEnd,
                rootOffset = rootOffset,
                progress = eased,
            ) { scale ->
                val onScreenRadius =
                    lerp(PillCoverRadius.value, IrideCoverBorderRadius.value, eased)
                val compensatedRadius =
                    (onScreenRadius / scale.coerceAtLeast(0.01f)).coerceAtMost(200f)
                val context = LocalContext.current
                AsyncImage(
                    model = irideFolioPlayerArtworkRequest(context, metadata.thumbnailUrl),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize().clip(irideSquircle(compensatedRadius.dp)),
                )
                if (ringAlpha > 0f) {
                    val strokeWidthDp = BridgeRingStrokeWidth / scale.coerceAtLeast(0.01f)
                    Box(
                        modifier =
                            Modifier.matchParentSize()
                                .graphicsLayer { alpha = ringAlpha }
                                .drawWithContent {
                                    drawBridgeProgressRing(
                                        cornerRadiusPx = compensatedRadius.dp.toPx(),
                                        strokeWidthPx = strokeWidthDp.toPx(),
                                        progress = bridgeState.progress,
                                    )
                                }
                    )
                }
            }
        }

        val infoStart = bridgeState.miniInfo
        val infoEnd = bridgeState.playerInfo
        if (infoStart != null && infoEnd != null) {
            BridgedInfoBlock(
                metadata = metadata,
                start = infoStart,
                end = infoEnd,
                rootOffset = rootOffset,
                progress = textProgress,
                navController = navController,
                playerBottomSheetState = playerBottomSheetState,
            )
        }
    }
}

private const val IrideCoverTextSplit = 0.28f

// Total horizontal space the bridge's more/favorite actions occupy once fully revealed
// (6.dp gap + two action targets). Reserved through an animated-width box so the weighted
// title never loses its width in a single frame mid-morph.
private val IrideBridgeInfoActionsWidth = 6.dp + IrideMp3InfoActionSize * 2

// Vertical convergence of the title/artist lines at the collapsed end of the morph. The fixed
// 16.sp title layout is taller than its scaled-down visual, which pushed the two lines apart
// in the mini state; each line translates toward their shared center by half of this amount
// at progress 0 and relaxes back to the laid-out positions by progress 1.
private val IrideBridgeInfoLineConvergence = 5.dp

@Composable
private fun BridgedInfoBlock(
    metadata: MediaMetadata,
    start: Rect,
    end: Rect,
    rootOffset: Offset,
    progress: Float,
    navController: NavController,
    playerBottomSheetState: BottomSheetState,
) {
    val density = LocalDensity.current
    val startLocal = remember(start, rootOffset) { start.translate(-rootOffset.x, -rootOffset.y) }
    val endLocal = remember(end, rootOffset) { end.translate(-rootOffset.x, -rootOffset.y) }
    val left = lerp(startLocal.left, endLocal.left, progress)
    val top = lerp(startLocal.top, endLocal.top, progress)
    val width = lerp(startLocal.width, endLocal.width, progress)

    val miniTitleColor = MaterialTheme.colorScheme.onSurface
    val miniArtistColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)

    val playerConnection = LocalPlayerConnection.current
    val database = LocalDatabase.current
    val librarySong by database.song(metadata.id).collectAsState(initial = null)
    val favoriteSong by playerConnection?.favoriteSong?.collectAsState(initial = null)
        ?: remember { mutableStateOf(null) }
    val isEpisode = librarySong?.song?.isEpisode == true
    val isFavorite =
        if (isEpisode) favoriteSong?.song?.inLibrary != null else favoriteSong?.song?.liked == true

    val menuState = LocalMenuState.current
    val bottomSheetPageState = LocalBottomSheetPageState.current
    val onMoreClick = {
        menuState.show(anchored = true) {
            PlayerMenu(
                mediaMetadata = metadata,
                showDissolve = true,
                navController = navController,
                playerBottomSheetState = playerBottomSheetState,
                onShowDetailsDialog = {
                    metadata.id.let { id -> bottomSheetPageState.show { ShowMediaInfo(id) } }
                },
                onDismiss = menuState::dismiss,
            )
        }
    }

    Column(
        modifier =
            Modifier.offset { IntOffset(left.roundToInt(), top.roundToInt()) }
                .width(with(density) { width.toDp() })
    ) {
        val iconReveal =
            if (playerConnection != null) {
                ((progress - 0.6f) / 0.4f).coerceIn(0f, 1f)
            } else {
                0f
            }
        Box(modifier = Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Fixed end-state size scaled via graphicsLayer instead of animating fontSize:
                // animating fontSize re-measures and re-rasterizes the text every frame (stuttery
                // glyphs, shifting ellipsis/letter spacing), while a GPU transform scales it
                // smoothly with zero relayout.
                Text(
                    text = metadata.title,
                    color = lerpColor(miniTitleColor, Color.White, progress),
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier =
                        Modifier.weight(1f).graphicsLayer {
                            // Mini end sits at 15.5/16 (was 14/16): bigger collapsed title,
                            // expanded end untouched at full 16.sp.
                            val scale = lerp(15.5f / 16f, 1f, progress)
                            scaleX = scale
                            scaleY = scale
                            transformOrigin = TransformOrigin(0f, 0f)
                            translationY =
                                IrideBridgeInfoLineConvergence.toPx() / 2f * (1f - progress)
                        },
                )
                // Width-only reservation for the end actions. A Spacer is measured but never
                // inflates the row's height ï¿½ actually composing the icon boxes in this
                // row made it taller than the title line, which then got vertically centered in
                // the extra empty space and visually pushed away from the artist line below.
                Spacer(Modifier.width(IrideBridgeInfoActionsWidth * iconReveal))
            }
            if (playerConnection != null) {
                // Overlay so the fixed-size click targets never influence the row height; they
                // fade in centered on the title line, exactly where the player's static actions
                // sit.
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier =
                        Modifier.align(Alignment.CenterEnd).graphicsLayer { alpha = iconReveal },
                ) {
                    SongMenuIconButton(
                        onClick = onMoreClick,
                        enabled = iconReveal > 0f,
                        modifier = Modifier.padding(start = 6.dp).size(IrideMp3InfoActionSize),
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.more_vert),
                            contentDescription = null,
                            tint = IrideMp3InfoActionColor,
                            modifier = Modifier.size(IrideMp3InfoActionIconSize),
                        )
                    }
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier =
                            Modifier.size(IrideMp3InfoActionSize).clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                enabled = iconReveal > 0f,
                            ) {
                                playerConnection.service.toggleLike()
                            },
                    ) {
                        Icon(
                            painter =
                                painterResource(
                                    if (isFavorite) R.drawable.favorite
                                    else R.drawable.favorite_border
                                ),
                            contentDescription = null,
                            tint = if (isFavorite) Color.White else IrideMp3InfoActionColor,
                            modifier = Modifier.size(IrideMp3InfoActionIconSize),
                        )
                    }
                }
            }
        }
        if (metadata.artists.any { it.name.isNotBlank() }) {
            IrideArtistText(
                mediaMetadata = metadata,
                color = lerpColor(miniArtistColor, IrideArtistTextColor, progress),
                fontSize = 12.sp,
                modifier =
                    Modifier.graphicsLayer {
                        translationY = -IrideBridgeInfoLineConvergence.toPx() / 2f * (1f - progress)
                    },
                onArtistClick = { artistId ->
                    if (progress < 1f) {
                        playerBottomSheetState.expandSoft()
                    } else if (artistId.isNotBlank()) {
                        navController.navigate("artist/$artistId")
                        playerBottomSheetState.collapseSoft()
                    }
                },
            )
        }
    }
}

@Composable
private fun BridgedElement(
    start: Rect,
    end: Rect,
    rootOffset: Offset,
    progress: Float,
    content: @Composable BoxScope.(scale: Float) -> Unit,
) {
    val density = LocalDensity.current
    val endLocal = remember(end, rootOffset) { end.translate(-rootOffset.x, -rootOffset.y) }
    val frameScaleX = if (end.width > 0f) lerp(start.width / end.width, 1f, progress) else 1f
    val frameScaleY = if (end.height > 0f) lerp(start.height / end.height, 1f, progress) else 1f
    Box(
        modifier =
            Modifier.offset { IntOffset(endLocal.left.roundToInt(), endLocal.top.roundToInt()) }
                .size(with(density) { DpSize(endLocal.width.toDp(), endLocal.height.toDp()) })
                .graphicsLayer {
                    scaleX = frameScaleX
                    scaleY = frameScaleY
                    transformOrigin = TransformOrigin(0f, 0f)
                    translationX = lerp(start.left, end.left, progress) - end.left
                    translationY = lerp(start.top, end.top, progress) - end.top
                }
    ) {
        content(frameScaleX)
    }
}
