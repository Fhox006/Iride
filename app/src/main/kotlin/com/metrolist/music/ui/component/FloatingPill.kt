/**
 * Iride Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import sv.lib.squircleshape.SquircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableLongState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import kotlinx.coroutines.isActive
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.Player
import androidx.palette.graphics.Palette
import coil3.compose.AsyncImage
import coil3.imageLoader
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.toBitmap
import com.metrolist.music.LocalDatabase
import com.metrolist.music.LocalListenTogetherManager
import com.metrolist.music.LocalPlayerConnection
import com.metrolist.music.R
import com.metrolist.music.constants.DarkModeKey
import com.metrolist.music.constants.MiniPlayerBackgroundStyle
import com.metrolist.music.constants.MiniPlayerBackgroundStyleKey
import com.metrolist.music.constants.MiniPlayerHeight
import com.metrolist.music.constants.TopNavigationBarKey
import com.metrolist.music.listentogether.ListenTogetherManager
import com.metrolist.music.models.MediaMetadata
import com.metrolist.music.playback.CastConnectionHandler
import com.metrolist.music.playback.PlayerConnection
import com.metrolist.music.ui.player.irideReportRect
import com.metrolist.music.ui.screens.Screens
import com.metrolist.music.ui.screens.settings.DarkMode
import com.metrolist.music.ui.theme.PlayerColorExtractor
import com.metrolist.music.ui.utils.resize
import com.metrolist.music.utils.rememberEnumPreference
import com.metrolist.music.utils.rememberPreference
import com.valentinilk.shimmer.shimmer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.withContext

private val PlaceholderMediaMetadata = MediaMetadata(
    id = "",
    title = "Tap a track to start listening",
    artists = emptyList(),
    duration = 0,
)

private val NavRowHeight = 56.dp
val FloatingPillHeight = MiniPlayerHeight + NavRowHeight  // 64 + 56 = 120dp
val FloatingPillBottomSpacing = 12.dp

@Stable
class PillProgressState(
    private val positionState: MutableLongState,
    private val durationState: MutableLongState,
) {
    val progress: Float
        get() {
            val duration = durationState.longValue
            return if (duration > 0) (positionState.longValue.toFloat() / duration).coerceIn(0f, 1f) else 0f
        }
}

private class PillProgressDrawCache {
    private var cachedSize = Size.Zero
    private var cachedInset = 0f
    private val trackPath = Path()
    private val progressPath = Path()
    private val pm = PathMeasure()
    private var total = 0f
    private var startOffset = 0f
    private val shape = SquircleShape(radius = 14.dp, cornerSmoothing = 0.48f)

    fun draw(scope: DrawScope, progress: Float, primaryColor: Color, trackColor: Color, strokeWidth: Float) {
        val inset = with(scope) { 2.dp.toPx() }

        if (scope.size != cachedSize || inset != cachedInset) {
            cachedSize = scope.size
            cachedInset = inset
            val pathSize = Size(scope.size.width - 2 * inset, scope.size.height - 2 * inset)
            val outline = shape.createOutline(pathSize, LayoutDirection.Ltr, scope)
            trackPath.reset()
            when (outline) {
                is Outline.Generic -> trackPath.addPath(outline.path, Offset(inset, inset))
                else -> {
                    val r = with(scope) { 14.dp.toPx() }
                    trackPath.addRoundRect(RoundRect(inset, inset, scope.size.width - inset, scope.size.height - inset, r, r))
                }
            }
            pm.setPath(trackPath, false)
            total = pm.length
            startOffset = findTopCenter(trackPath, scope.size.width / 2f, inset)
        }

        val stroke = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        scope.drawPath(trackPath, color = trackColor, style = stroke)

        if (progress > 0f && total > 0f) {
            val progressLength = total * progress
            progressPath.reset()
            val end = startOffset + progressLength
            if (end <= total) {
                pm.getSegment(startOffset, end, progressPath, true)
            } else {
                pm.getSegment(startOffset, total, progressPath, true)
                val overflow = Path()
                pm.getSegment(0f, end - total, overflow, true)
                progressPath.addPath(overflow)
            }
            scope.drawPath(progressPath, color = primaryColor, style = stroke)
        }
    }

    private fun findTopCenter(path: Path, targetX: Float, topY: Float): Float {
        val nativePM = android.graphics.PathMeasure(path.asAndroidPath(), false)
        val nativeTotal = nativePM.length
        val pos = FloatArray(2)
        val tan = FloatArray(2)
        var bestOffset = 0f
        var bestDist = Float.MAX_VALUE
        repeat(120) { i ->
            val t = nativeTotal * i / 120f
            nativePM.getPosTan(t, pos, tan)
            val dx = pos[0] - targetX
            val dy = pos[1] - topY
            val dist = dx * dx + dy * dy * 9f
            if (dist < bestDist) {
                bestDist = dist
                bestOffset = t
            }
        }
        return bestOffset
    }
}

@Composable
fun FloatingPill(
    navigationItems: List<Screens>,
    currentRoute: String?,
    onNavItemClick: (Screens, Boolean) -> Unit,
    playerBottomSheetState: BottomSheetState,
    onSearchLongClick: () -> Unit,
    accountImageUrl: String?,
    pureBlack: Boolean = false,
    slimNav: Boolean = false,
    showNavRow: Boolean = true,
    onPlayerExpand: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val playerConnection = LocalPlayerConnection.current

    val isTopLevelRoute = remember(currentRoute, navigationItems) {
        currentRoute == null ||
                (navigationItems.any { it.route == currentRoute } && currentRoute != "settings") ||
                currentRoute.startsWith("search/")
    }

    val targetPillHeight = if (showNavRow && isTopLevelRoute) FloatingPillHeight else MiniPlayerHeight
    var hasInitialized by remember { mutableStateOf(false) }
    val animatedPillHeight by animateDpAsState(
        targetValue = targetPillHeight,
        animationSpec = if (!hasInitialized) snap() else tween(durationMillis = 280, easing = FastOutSlowInEasing),
        label = "pillHeight",
        finishedListener = { hasInitialized = true },
    )
    LaunchedEffect(Unit) { hasInitialized = true }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Bottom))
            .padding(bottom = FloatingPillBottomSpacing)
            .padding(horizontal = 12.dp)
            .height(animatedPillHeight)
            .clip(SquircleShape(radius = 24.dp, cornerSmoothing = 0.48f)),
    ) {
        if (playerConnection == null) {
            PillShimmerSkeleton(isTopLevelRoute = isTopLevelRoute && showNavRow)
        } else {
            PillContent(
                navigationItems = navigationItems,
                currentRoute = currentRoute,
                onNavItemClick = onNavItemClick,
                playerBottomSheetState = playerBottomSheetState,
                onSearchLongClick = onSearchLongClick,
                accountImageUrl = accountImageUrl,
                isTopLevelRoute = isTopLevelRoute,
                showNavRow = showNavRow,
                animatedHeight = animatedPillHeight,
                pureBlack = pureBlack,
                slimNav = slimNav,
                playerConnection = playerConnection,
                onPlayerExpand = onPlayerExpand,
            )
        }
    }
}

@Composable
private fun PillShimmerSkeleton(isTopLevelRoute: Boolean) {
    val pillHeight = if (isTopLevelRoute) FloatingPillHeight else MiniPlayerHeight
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(pillHeight)
            .shimmer()
            .clip(SquircleShape(radius = 24.dp, cornerSmoothing = 0.48f))
            .background(MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.6f)),
    )
}

@Composable
private fun PillContent(
    navigationItems: List<Screens>,
    currentRoute: String?,
    onNavItemClick: (Screens, Boolean) -> Unit,
    playerBottomSheetState: BottomSheetState,
    onSearchLongClick: () -> Unit,
    accountImageUrl: String?,
    isTopLevelRoute: Boolean,
    showNavRow: Boolean,
    animatedHeight: Dp,
    pureBlack: Boolean,
    slimNav: Boolean,
    playerConnection: PlayerConnection,
    onPlayerExpand: (() -> Unit)? = null,
) {
    val miniPlayerBackground by rememberEnumPreference(
        MiniPlayerBackgroundStyleKey,
        defaultValue = MiniPlayerBackgroundStyle.DEFAULT,
    )
    val (newIrideUi, _) = rememberPreference(TopNavigationBarKey, defaultValue = true)
    val context = LocalContext.current
    var gradientColors by remember { mutableStateOf<List<Color>>(emptyList()) }
    val isSystemInDarkTheme = isSystemInDarkTheme()
    val darkTheme by rememberEnumPreference(DarkModeKey, defaultValue = DarkMode.ON)
    val useDarkTheme = remember(darkTheme, isSystemInDarkTheme) {
        if (darkTheme == DarkMode.AUTO) isSystemInDarkTheme else darkTheme == DarkMode.ON
    }

    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()
    val effectiveMetadata = mediaMetadata ?: PlaceholderMediaMetadata
    val playbackState by playerConnection.playbackState.collectAsState()
    val canSkipNext by playerConnection.canSkipNext.collectAsState()

    val castHandler = remember(playerConnection) {
        try { playerConnection.service.castConnectionHandler } catch (e: Exception) { null }
    }
    val isCasting by castHandler?.isCasting?.collectAsState() ?: remember { mutableStateOf(false) }

    val positionState = remember { mutableLongStateOf(0L) }
    val durationState = remember { mutableLongStateOf(0L) }
    val progressState = remember { PillProgressState(positionState, durationState) }
    val isPlaying by playerConnection.isPlaying.collectAsState()
    val listenTogetherManager = LocalListenTogetherManager.current

    LaunchedEffect(isPlaying, isCasting) {
        if (!isCasting && isPlaying) {
            while (isActive) {
                delay(200)
                positionState.longValue = playerConnection.player.currentPosition
                durationState.longValue = playerConnection.player.duration
            }
        }
    }
    LaunchedEffect(playbackState, mediaMetadata?.id) {
        if (!isCasting) {
            positionState.longValue = playerConnection.player.currentPosition
            durationState.longValue = playerConnection.player.duration
        }
    }

    LaunchedEffect(mediaMetadata?.id, miniPlayerBackground) {
        gradientColors = emptyList()
        if (miniPlayerBackground == MiniPlayerBackgroundStyle.GRADIENT) {
            val url = mediaMetadata?.thumbnailUrl
            if (url != null) {
                withContext(Dispatchers.IO) {
                    val request = ImageRequest.Builder(context)
                        .data(url).size(100, 100).allowHardware(false).build()
                    val result = runCatching { context.imageLoader.execute(request) }.getOrNull()
                    val bitmap = result?.image?.toBitmap()
                    if (bitmap != null) {
                        val palette = withContext(Dispatchers.Default) {
                            Palette.from(bitmap).maximumColorCount(8).resizeBitmapArea(100 * 100).generate()
                        }
                        val extracted = PlayerColorExtractor.extractGradientColors(
                            palette = palette,
                            fallbackColor = 0xFF000000.toInt(),
                        )
                        withContext(Dispatchers.Main) { gradientColors = extracted }
                    }
                }
            }
        }
    }

    val effectiveBackground = if (pureBlack && miniPlayerBackground == MiniPlayerBackgroundStyle.DEFAULT) {
        MiniPlayerBackgroundStyle.PURE_BLACK
    } else {
        miniPlayerBackground
    }

    val irideDefaultActive = newIrideUi && effectiveBackground == MiniPlayerBackgroundStyle.DEFAULT
    val backgroundColor = when {
        irideDefaultActive -> MaterialTheme.colorScheme.primaryContainer
        else -> when (effectiveBackground) {
            MiniPlayerBackgroundStyle.DEFAULT     -> MaterialTheme.colorScheme.surfaceContainer
            MiniPlayerBackgroundStyle.TRANSPARENT -> Color.Black.copy(alpha = 0.25f)
            MiniPlayerBackgroundStyle.GRADIENT    -> MaterialTheme.colorScheme.surfaceContainer
            MiniPlayerBackgroundStyle.PURE_BLACK  -> Color.Black
        }
    }
    val forceLightColors = !useDarkTheme && (
            effectiveBackground == MiniPlayerBackgroundStyle.PURE_BLACK ||
                    effectiveBackground == MiniPlayerBackgroundStyle.GRADIENT
            )
    val primaryColor   = if (forceLightColors) Color.White else MaterialTheme.colorScheme.primary
    val outlineColor   = when {
        forceLightColors     -> Color.White
        irideDefaultActive   -> MaterialTheme.colorScheme.onPrimaryContainer
        else                 -> MaterialTheme.colorScheme.outline
    }
    val onSurfaceColor = when {
        forceLightColors     -> Color.White
        irideDefaultActive   -> MaterialTheme.colorScheme.onPrimaryContainer
        else                 -> MaterialTheme.colorScheme.onSurface
    }
    val errorColor     = if (forceLightColors) Color(0xFFFF6B6B) else MaterialTheme.colorScheme.error

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(animatedHeight)
            .clip(SquircleShape(radius = 24.dp, cornerSmoothing = 0.48f))
            .background(backgroundColor)
            .border(1.dp, outlineColor.copy(alpha = 0.3f), SquircleShape(radius = 24.dp, cornerSmoothing = 0.48f)),
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Gradient overlay
            when (effectiveBackground) {
                MiniPlayerBackgroundStyle.GRADIENT -> {
                    val colors = if (gradientColors.isNotEmpty()) gradientColors
                    else listOf(MaterialTheme.colorScheme.surfaceContainer, MaterialTheme.colorScheme.surfaceContainer)
                    Box(
                        Modifier
                            .fillMaxSize()
                            .background(Brush.horizontalGradient(colors))
                            .background(Color.Black.copy(alpha = 0.15f)),
                    )
                }
                else -> {}
            }

            Column(modifier = Modifier.fillMaxWidth()) {
                // ── TOP ROW: player (height = MiniPlayerHeight, fully clickable → open player) ──
                PillPlayerRow(
                    progressState = progressState,
                    displayMetadata = effectiveMetadata,
                    favoriteSongId = mediaMetadata?.id,
                    playbackState = playbackState,
                    canSkipNext = canSkipNext,
                    isCasting = isCasting,
                    castHandler = castHandler,
                    playerConnection = playerConnection,
                    listenTogetherManager = listenTogetherManager,
                    primaryColor = primaryColor,
                    outlineColor = outlineColor,
                    onSurfaceColor = onSurfaceColor,
                    errorColor = errorColor,
                    onExpandClick = {
                        playerBottomSheetState.expandSoft()
                        onPlayerExpand?.invoke()
                    },
                )

                // ── BOTTOM ROW: nav buttons, visible only on top-level routes ──
                AnimatedVisibility(
                    visible = showNavRow && isTopLevelRoute,
                    enter = fadeIn(tween(300)) + slideInVertically(tween(320), initialOffsetY = { it }),
                    exit = fadeOut(tween(250)) + slideOutVertically(tween(280), targetOffsetY = { it }),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(NavRowHeight)
                            .padding(horizontal = 8.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        navigationItems.filter { it != Screens.Account }.forEach { screen ->
                            PillNavItem(
                                screen = screen,
                                currentRoute = currentRoute,
                                navigationItems = navigationItems,
                                onNavItemClick = onNavItemClick,
                                onSearchLongClick = onSearchLongClick,
                                accountImageUrl = accountImageUrl,
                                tintSelected = primaryColor,
                                tintUnselected = onSurfaceColor.copy(alpha = 0.6f),
                                showLabel = !slimNav,
                                modifier = Modifier.weight(1f).fillMaxHeight(),
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * The pill's top row (thumbnail/progress ring, title/artist, cast icon, favorite, play/pause,
 * skip-next) extracted so it can be reused both by the floating [FloatingPill] (old UI) and by the
 * New Iride UI's player-curtain peek slot, which pins this row to the bottom of the screen instead
 * of floating it — the drag-to-expand gesture in that case is attached by the caller, not here;
 * this composable only handles tap-to-expand via [onExpandClick].
 */
@Composable
fun PillPlayerRow(
    progressState: PillProgressState,
    displayMetadata: MediaMetadata,
    favoriteSongId: String?,
    playbackState: Int,
    canSkipNext: Boolean,
    isCasting: Boolean,
    castHandler: CastConnectionHandler?,
    playerConnection: PlayerConnection,
    listenTogetherManager: ListenTogetherManager?,
    primaryColor: Color,
    outlineColor: Color,
    onSurfaceColor: Color,
    errorColor: Color,
    onExpandClick: () -> Unit,
    modifier: Modifier = Modifier,
    // New Iride UI bridge: when set, the cover art below reports its on-screen rect here and hides
    // itself instead of drawing — IrideMiniPlayerBridgeOverlay morphs a single moving copy between
    // this collapsed position and the expanded player's cover, rather than cross-fading two
    // duplicates. Play/pause, skip and favorite are left alone and keep cross-fading.
    onArtPositioned: ((Rect) -> Unit)? = null,
    // Same idea as [onArtPositioned] but for the title/artist block — lets the bridge overlay morph
    // the text (and cross-fade its font) between this collapsed position and the expanded player's.
    onInfoPositioned: ((Rect) -> Unit)? = null,
) {
    // Non-null only when the caller is the New Iride UI's curtain peek row (see the doc comment
    // above) — used to switch to the sharp icon set that matches the expanded player's wheel,
    // without touching the classic FloatingPill's own icons.
    val isIrideStyle = onArtPositioned != null

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(MiniPlayerHeight)
            .clickable { onExpandClick() },
        contentAlignment = Alignment.Center,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp, vertical = 8.dp),
        ) {
            PillPlayButton(
                progressState = progressState,
                mediaMetadata = displayMetadata,
                primaryColor = primaryColor,
                outlineColor = outlineColor,
                onArtPositioned = onArtPositioned,
            )

            Spacer(Modifier.width(16.dp))

            PillSongInfo(
                mediaMetadata = displayMetadata,
                onSurfaceColor = onSurfaceColor,
                errorColor = errorColor,
                onInfoPositioned = onInfoPositioned,
                modifier = Modifier.weight(1f),
            )

            Spacer(Modifier.width(8.dp))

            if (isCasting) {
                Icon(
                    painter = painterResource(R.drawable.cast_connected),
                    contentDescription = null,
                    tint = primaryColor,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(Modifier.width(8.dp))
            }

            favoriteSongId?.let {
                PillFavoriteButton(
                    songId = it,
                    onSurfaceColor = onSurfaceColor,
                    playerConnection = playerConnection,
                )
            }

            Spacer(Modifier.width(4.dp))

            PillPlayPauseButton(
                playbackState = playbackState,
                isCasting = isCasting,
                castHandler = castHandler,
                playerConnection = playerConnection,
                listenTogetherManager = listenTogetherManager,
                onSurfaceColor = onSurfaceColor,
                useSharpIcons = isIrideStyle,
            )

            PillSkipNextButton(
                canSkipNext = canSkipNext,
                playerConnection = playerConnection,
                listenTogetherManager = listenTogetherManager,
                onSurfaceColor = onSurfaceColor,
                useSharpIcons = isIrideStyle,
            )
        }
    }
}

@Composable
private fun PillNavItem(
    screen: Screens,
    currentRoute: String?,
    navigationItems: List<Screens>,
    onNavItemClick: (Screens, Boolean) -> Unit,
    onSearchLongClick: () -> Unit,
    accountImageUrl: String?,
    tintSelected: Color,
    tintUnselected: Color,
    showLabel: Boolean,
    modifier: Modifier = Modifier,
) {
    val isSelected = remember(currentRoute, screen.route) {
        isRouteSelected(currentRoute, screen.route, navigationItems)
    }
    val currentIsSelected by rememberUpdatedState(isSelected)
    val iconRes = remember(isSelected, screen) {
        if (isSelected) screen.iconIdActive else screen.iconIdInactive
    }
    val tint = if (isSelected) tintSelected else tintUnselected

    val isSearchItem = screen == Screens.Search
    val interactionSource = remember { MutableInteractionSource() }
    val haptics = LocalHapticFeedback.current
    val viewConfiguration = LocalViewConfiguration.current

    if (isSearchItem) {
        LaunchedEffect(interactionSource) {
            var isLongClick = false
            interactionSource.interactions.collectLatest { interaction ->
                when (interaction) {
                    is PressInteraction.Press -> {
                        isLongClick = false
                        delay(viewConfiguration.longPressTimeoutMillis)
                        isLongClick = true
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        onSearchLongClick()
                    }
                    is PressInteraction.Release -> {
                        if (!isLongClick) onNavItemClick(screen, currentIsSelected)
                    }
                    is PressInteraction.Cancel -> { isLongClick = false }
                }
            }
        }
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = { if (!isSearchItem) onNavItemClick(screen, currentIsSelected) },
            )
            .fillMaxHeight()
            .padding(horizontal = 12.dp, vertical = 4.dp),
    ) {
        if (screen == Screens.Account && accountImageUrl != null) {
            AsyncImage(
                model = accountImageUrl,
                contentDescription = stringResource(screen.titleId),
                modifier = Modifier.size(24.dp).clip(CircleShape),
            )
        } else {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = stringResource(screen.titleId),
                tint = tint,
                modifier = Modifier.size(24.dp),
            )
        }
        if (showLabel) {
            Text(
                text = stringResource(screen.titleId),
                color = tint,
                fontSize = 10.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun PillPlayButton(
    progressState: PillProgressState,
    mediaMetadata: MediaMetadata,
    primaryColor: Color,
    outlineColor: Color,
    onArtPositioned: ((Rect) -> Unit)? = null,
) {
    val trackColor = outlineColor.copy(alpha = 0.2f)
    val strokeWidth = 3.dp
    val pillDrawCache = remember { PillProgressDrawCache() }

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(48.dp)
            .drawWithContent {
                drawContent()
                pillDrawCache.draw(this, progressState.progress, primaryColor, trackColor, strokeWidth.toPx())
            },
    ) {
        val imageShape = SquircleShape(radius = 14.dp, cornerSmoothing = 0.48f)
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(44.dp)
                .then(
                    if (onArtPositioned != null) {
                        Modifier.irideReportRect(onArtPositioned).alpha(0f)
                    } else {
                        Modifier
                    },
                )
                .clip(imageShape)
                .border(1.dp, outlineColor.copy(alpha = 0.3f), imageShape),
        ) {
            val thumbnailUrl = remember(mediaMetadata.thumbnailUrl) { mediaMetadata.thumbnailUrl?.resize(120, 120) }
            AsyncImage(
                model = thumbnailUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize().clip(imageShape),
            )
        }
    }
}

@Composable
private fun PillSongInfo(
    mediaMetadata: MediaMetadata,
    onSurfaceColor: Color,
    errorColor: Color,
    modifier: Modifier = Modifier,
    onInfoPositioned: ((Rect) -> Unit)? = null,
) {
    val error by LocalPlayerConnection.current?.error?.collectAsState() ?: remember { mutableStateOf(null) }

    Column(
        modifier = modifier
            .then(
                if (onInfoPositioned != null) {
                    Modifier.irideReportRect(onInfoPositioned).alpha(0f)
                } else {
                    Modifier
                },
            ),
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = mediaMetadata.title,
            color = onSurfaceColor,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Clip,
            modifier = Modifier.basicMarquee(iterations = 1, initialDelayMillis = 3000, velocity = 30.dp),
        )
        if (mediaMetadata.artists.any { it.name.isNotBlank() }) {
            Text(
                text = mediaMetadata.artists.joinToString { it.name },
                color = onSurfaceColor.copy(alpha = 0.7f),
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Clip,
                modifier = Modifier.basicMarquee(iterations = 1, initialDelayMillis = 3000, velocity = 30.dp),
            )
        }
        AnimatedVisibility(visible = error != null, enter = fadeIn(), exit = fadeOut()) {
            Text(
                text = stringResource(R.string.error_playing),
                color = errorColor,
                fontSize = 10.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun PillFavoriteButton(
    songId: String,
    onSurfaceColor: Color,
    playerConnection: PlayerConnection,
) {
    val database = LocalDatabase.current
    val librarySong by database.song(songId).collectAsState(initial = null)
    val isEpisode = librarySong?.song?.isEpisode == true
    val isLiked = if (isEpisode) librarySong?.song?.inLibrary != null else librarySong?.song?.liked == true

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(36.dp)
            .clickable { playerConnection.service.toggleLike() },
    ) {
        Icon(
            painter = painterResource(if (isLiked) R.drawable.favorite else R.drawable.favorite_border),
            contentDescription = null,
            tint = onSurfaceColor,
            modifier = Modifier.size(20.dp),
        )
    }
}

@Composable
private fun PillPlayPauseButton(
    playbackState: Int,
    isCasting: Boolean,
    castHandler: CastConnectionHandler?,
    playerConnection: PlayerConnection,
    listenTogetherManager: ListenTogetherManager?,
    onSurfaceColor: Color,
    useSharpIcons: Boolean = false,
) {
    val isPlaying by playerConnection.isPlaying.collectAsState()
    val castIsPlaying by castHandler?.castIsPlaying?.collectAsState() ?: remember { mutableStateOf(false) }
    val effectiveIsPlaying = if (isCasting) castIsPlaying else isPlaying
    val isListenTogetherGuest = listenTogetherManager?.let { it.isInRoom && !it.isHost } ?: false

    IconButton(
        onClick = {
            if (isListenTogetherGuest) return@IconButton
            if (isCasting) {
                if (castIsPlaying) castHandler?.pause() else castHandler?.play()
            } else if (playbackState == Player.STATE_ENDED) {
                playerConnection.player.seekTo(0, 0)
                playerConnection.player.playWhenReady = true
            } else {
                playerConnection.togglePlayPause()
            }
        },
    ) {
        Icon(
            painter = painterResource(
                when {
                    playbackState == Player.STATE_ENDED -> R.drawable.replay
                    effectiveIsPlaying -> if (useSharpIcons) R.drawable.ic_iride_pause else R.drawable.pause
                    else -> if (useSharpIcons) R.drawable.ic_iride_play else R.drawable.play
                },
            ),
            contentDescription = null,
            tint = onSurfaceColor,
            modifier = Modifier.size(24.dp),
        )
    }
}

@Composable
private fun PillSkipNextButton(
    canSkipNext: Boolean,
    playerConnection: PlayerConnection,
    listenTogetherManager: ListenTogetherManager?,
    onSurfaceColor: Color,
    useSharpIcons: Boolean = false,
) {
    val isListenTogetherGuest = listenTogetherManager?.let { it.isInRoom && !it.isHost } ?: false
    IconButton(
        enabled = canSkipNext && !isListenTogetherGuest,
        onClick = { playerConnection.seekToNext() },
    ) {
        Icon(
            painter = painterResource(if (useSharpIcons) R.drawable.ic_iride_skip_next else R.drawable.skip_next),
            contentDescription = null,
            tint = if (canSkipNext && !isListenTogetherGuest) onSurfaceColor else onSurfaceColor.copy(alpha = 0.3f),
            modifier = Modifier.size(24.dp),
        )
    }
}

