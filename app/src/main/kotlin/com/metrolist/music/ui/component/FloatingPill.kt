/**
 * Iride Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.animateDpAsState
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Slider
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
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.navigation.NavController
import com.metrolist.music.utils.makeTimeString
import androidx.media3.common.Player as Media3Player
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
import com.metrolist.music.listentogether.ListenTogetherManager
import com.metrolist.music.models.MediaMetadata
import com.metrolist.music.playback.CastConnectionHandler
import com.metrolist.music.playback.PlayerConnection
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

import com.metrolist.music.playback.SongWithArtists
import com.metrolist.music.ui.player.PlayerControlsPanel
import com.metrolist.music.constants.UseNewPlayerDesignKey
import com.metrolist.music.constants.PlayerButtonsStyleKey
import com.metrolist.music.constants.PlayerButtonsStyle
import com.metrolist.music.constants.HidePlayerThumbnailKey
import com.metrolist.music.constants.CropAlbumArtKey
import com.metrolist.music.constants.PureBlackKey
import com.metrolist.music.constants.SliderStyle
import com.metrolist.music.constants.SliderStyleKey
import com.metrolist.music.constants.SquigglySliderKey
import com.metrolist.music.listentogether.RoomRole
import androidx.compose.runtime.saveable.rememberSaveable

private val NavRowHeight = 56.dp
val FloatingPillHeight = MiniPlayerHeight + NavRowHeight  // 64 + 56 = 120dp
val FloatingPillBottomSpacing = 12.dp
val FloatingPillExpandedHeight = 280.dp

@Stable
private class PillProgressState(
    private val positionState: MutableLongState,
    private val durationState: MutableLongState,
) {
    val progress: Float
        get() {
            val duration = durationState.longValue
            return if (duration > 0) (positionState.longValue.toFloat() / duration).coerceIn(0f, 1f) else 0f
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
    navController: NavController,
    modifier: Modifier = Modifier,
) {
    val playerConnection = LocalPlayerConnection.current
    val mediaMetadata by (playerConnection?.mediaMetadata?.collectAsState() ?: remember { mutableStateOf(null) })

    val isTopLevelRoute = remember(currentRoute, navigationItems) {
        currentRoute == null ||
            navigationItems.any { it.route == currentRoute } ||
            currentRoute.startsWith("search/")
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Bottom))
            .padding(bottom = FloatingPillBottomSpacing)
            .padding(horizontal = 12.dp)
            .graphicsLayer {
                val p = playerBottomSheetState.progress.coerceIn(0f, 1f)
                // Lift the pill upward as it expands so bottom stays anchored
                val expandDeltaPx = (FloatingPillExpandedHeight - FloatingPillHeight).toPx()
                translationY = -expandDeltaPx * p
            },
    ) {
        if (playerConnection == null || mediaMetadata == null) {
            PillShimmerSkeleton(isTopLevelRoute = isTopLevelRoute)
        } else {
            PillContent(
                navigationItems = navigationItems,
                currentRoute = currentRoute,
                onNavItemClick = onNavItemClick,
                playerBottomSheetState = playerBottomSheetState,
                onSearchLongClick = onSearchLongClick,
                accountImageUrl = accountImageUrl,
                isTopLevelRoute = isTopLevelRoute,
                pureBlack = pureBlack,
                slimNav = slimNav,
                playerConnection = playerConnection,
                navController = navController,
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
            .clip(RoundedCornerShape(28.dp))
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
    pureBlack: Boolean,
    slimNav: Boolean,
    playerConnection: PlayerConnection,
    navController: NavController,
) {
    val miniPlayerBackground by rememberEnumPreference(
        MiniPlayerBackgroundStyleKey,
        defaultValue = MiniPlayerBackgroundStyle.DEFAULT,
    )
    val context = LocalContext.current
    var gradientColors by remember { mutableStateOf<List<Color>>(emptyList()) }
    val isSystemInDarkTheme = isSystemInDarkTheme()
    val darkTheme by rememberEnumPreference(DarkModeKey, defaultValue = DarkMode.ON)
    val useDarkTheme = remember(darkTheme, isSystemInDarkTheme) {
        if (darkTheme == DarkMode.AUTO) isSystemInDarkTheme else darkTheme == DarkMode.ON
    }

    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()
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
                delay(100)
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

    val backgroundColor = when (effectiveBackground) {
        MiniPlayerBackgroundStyle.DEFAULT     -> MaterialTheme.colorScheme.surfaceContainer
        MiniPlayerBackgroundStyle.TRANSPARENT -> Color.Black.copy(alpha = 0.25f)
        MiniPlayerBackgroundStyle.BLUR        -> MaterialTheme.colorScheme.surfaceContainer
        MiniPlayerBackgroundStyle.GRADIENT    -> MaterialTheme.colorScheme.surfaceContainer
        MiniPlayerBackgroundStyle.PURE_BLACK  -> Color.Black
    }
    val forceLightColors = !useDarkTheme && (
        effectiveBackground == MiniPlayerBackgroundStyle.PURE_BLACK ||
        effectiveBackground == MiniPlayerBackgroundStyle.BLUR ||
        effectiveBackground == MiniPlayerBackgroundStyle.GRADIENT
    )
    val primaryColor   = if (forceLightColors) Color.White else MaterialTheme.colorScheme.primary
    val outlineColor   = if (forceLightColors) Color.White else MaterialTheme.colorScheme.outline
    val onSurfaceColor = if (forceLightColors) Color.White else MaterialTheme.colorScheme.onSurface
    val errorColor     = if (forceLightColors) Color(0xFFFF6B6B) else MaterialTheme.colorScheme.error

    val pillProgress = playerBottomSheetState.progress.coerceIn(0f, 1f)
    val targetPillHeight = when {
        pillProgress > 0.02f -> FloatingPillExpandedHeight
        isTopLevelRoute -> FloatingPillHeight
        else -> MiniPlayerHeight
    }
    val animatedHeight by animateDpAsState(
        targetValue = targetPillHeight,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow),
        label = "pillHeight",
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(animatedHeight)
            .clip(RoundedCornerShape(28.dp))
            .background(backgroundColor)
            .border(1.dp, outlineColor.copy(alpha = 0.3f), RoundedCornerShape(28.dp)),
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Blur/gradient overlays
            when (effectiveBackground) {
                MiniPlayerBackgroundStyle.BLUR -> {
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                        mediaMetadata?.thumbnailUrl?.let { url ->
                            AsyncImage(
                                model = url,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .blur(60.dp),
                            )
                            Box(
                                Modifier
                                    .fillMaxSize()
                                    .background(Color.Black.copy(alpha = 0.45f)),
                            )
                        }
                    }
                }
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
                // Crossfade between collapsed pill content and expanded player controls
                val showExpanded = pillProgress > 0.05f
                androidx.compose.animation.AnimatedContent(
                    targetState = showExpanded,
                    transitionSpec = {
                        fadeIn(animationSpec = tween(200)) togetherWith fadeOut(animationSpec = tween(150))
                    },
                    label = "pillContentSwitch",
                ) { expanded ->
                    if (!expanded) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            // ── TOP ROW: player (height = MiniPlayerHeight, fully clickable → open player) ──
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(MiniPlayerHeight)
                                    .clickable { playerBottomSheetState.expandSoft() },
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
                                        mediaMetadata = mediaMetadata,
                                        primaryColor = primaryColor,
                                        outlineColor = outlineColor,
                                    )

                                    Spacer(Modifier.width(16.dp))

                                    PillSongInfo(
                                        mediaMetadata = mediaMetadata,
                                        onSurfaceColor = onSurfaceColor,
                                        errorColor = errorColor,
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

                                    mediaMetadata?.let {
                                        PillFavoriteButton(
                                            songId = it.id,
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
                                    )

                                    PillSkipNextButton(
                                        canSkipNext = canSkipNext,
                                        playerConnection = playerConnection,
                                        listenTogetherManager = listenTogetherManager,
                                        onSurfaceColor = onSurfaceColor,
                                    )
                                }
                            }

                            // ── BOTTOM ROW: nav buttons, visible only on top-level routes ──
                            AnimatedVisibility(
                                visible = isTopLevelRoute,
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
                                    navigationItems.forEach { screen ->
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
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        // EXPANDED: player controls inside the pill
                        PillPlayerControls(
                            playerConnection = playerConnection,
                            navController = navController,
                            onSurfaceColor = onSurfaceColor,
                            primaryColor = primaryColor,
                            outlineColor = outlineColor,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }
            }
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
        modifier = Modifier
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = { if (!isSearchItem) onNavItemClick(screen, currentIsSelected) },
            )
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
    mediaMetadata: MediaMetadata?,
    primaryColor: Color,
    outlineColor: Color,
) {
    val trackColor = outlineColor.copy(alpha = 0.2f)
    val strokeWidth = 3.dp

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(48.dp)
            .drawWithContent {
                drawContent()
                val progress = progressState.progress
                val stroke = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round)
                val diameter = size.minDimension
                val topLeft = Offset((size.width - diameter) / 2, (size.height - diameter) / 2)
                drawArc(
                    color = trackColor,
                    startAngle = 0f, sweepAngle = 360f, useCenter = false,
                    topLeft = topLeft, size = Size(diameter, diameter), style = stroke,
                )
                drawArc(
                    color = primaryColor,
                    startAngle = -90f, sweepAngle = 360f * progress, useCenter = false,
                    topLeft = topLeft, size = Size(diameter, diameter), style = stroke,
                )
            },
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .border(1.dp, outlineColor.copy(alpha = 0.3f), CircleShape),
        ) {
            mediaMetadata?.let { metadata ->
                val thumbnailUrl = remember(metadata.thumbnailUrl) { metadata.thumbnailUrl?.resize(120, 120) }
                AsyncImage(
                    model = thumbnailUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize().clip(CircleShape),
                )
            }
        }
    }
}

@Composable
private fun PillSongInfo(
    mediaMetadata: MediaMetadata?,
    onSurfaceColor: Color,
    errorColor: Color,
    modifier: Modifier = Modifier,
) {
    val error by LocalPlayerConnection.current?.error?.collectAsState() ?: remember { mutableStateOf(null) }

    Column(modifier = modifier, verticalArrangement = Arrangement.Center) {
        mediaMetadata?.let { metadata ->
            Text(
                text = metadata.title,
                color = onSurfaceColor,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Clip,
                modifier = Modifier.basicMarquee(iterations = 1, initialDelayMillis = 3000, velocity = 30.dp),
            )
            if (metadata.artists.any { it.name.isNotBlank() }) {
                Text(
                    text = metadata.artists.joinToString { it.name },
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
                    effectiveIsPlaying -> R.drawable.pause
                    else -> R.drawable.play
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
) {
    val isListenTogetherGuest = listenTogetherManager?.let { it.isInRoom && !it.isHost } ?: false
    IconButton(
        enabled = canSkipNext && !isListenTogetherGuest,
        onClick = { playerConnection.seekToNext() },
    ) {
        Icon(
            painter = painterResource(R.drawable.skip_next),
            contentDescription = null,
            tint = if (canSkipNext && !isListenTogetherGuest) onSurfaceColor else onSurfaceColor.copy(alpha = 0.3f),
            modifier = Modifier.size(24.dp),
        )
    }
}

@Composable
private fun PillPlayerControls(
    playerConnection: PlayerConnection,
    navController: NavController,
    onSurfaceColor: Color,
    primaryColor: Color,
    outlineColor: Color,
    modifier: Modifier = Modifier,
) {
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()
    val playbackState by playerConnection.playbackState.collectAsState()
    val isPlaying by playerConnection.isPlaying.collectAsState()
    val canSkipNext by playerConnection.canSkipNext.collectAsState()
    val canSkipPrevious by playerConnection.canSkipPrevious.collectAsState()

    var position by remember { mutableLongStateOf(0L) }
    var duration by remember { mutableLongStateOf(0L) }
    var sliderPosition by remember { mutableStateOf<Long?>(null) }

    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            while (isActive) {
                delay(100)
                position = playerConnection.player.currentPosition
                duration = playerConnection.player.duration
            }
        }
    }
    LaunchedEffect(playbackState, mediaMetadata?.id) {
        position = playerConnection.player.currentPosition
        duration = playerConnection.player.duration
    }

    val effectivePosition = sliderPosition ?: position

    val playPauseRoundness by animateDpAsState(
        targetValue = if (isPlaying) 24.dp else 36.dp,
        animationSpec = tween(durationMillis = 90, easing = androidx.compose.animation.core.LinearEasing),
        label = "ppRoundness",
    )

    Column(
        modifier = modifier.padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        // ROW 1: title + artist (marquee)
        mediaMetadata?.let { meta ->
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = meta.title,
                    color = onSurfaceColor,
                    fontSize = 14.sp,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.basicMarquee(iterations = 1, initialDelayMillis = 2000, velocity = 30.dp),
                )
                if (meta.artists.any { it.name.isNotBlank() }) {
                    Text(
                        text = meta.artists.joinToString { it.name },
                        color = onSurfaceColor.copy(alpha = 0.7f),
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.basicMarquee(iterations = 1, initialDelayMillis = 2000, velocity = 30.dp),
                    )
                }
            }
        }

        Spacer(Modifier.height(4.dp))

        // ROW 2: Slider + time labels
        Column(modifier = Modifier.fillMaxWidth()) {
            androidx.compose.material3.Slider(
                value = effectivePosition.toFloat(),
                valueRange = 0f..(if (duration == C.TIME_UNSET) 0f else duration.toFloat()),
                onValueChange = { sliderPosition = it.toLong() },
                onValueChangeFinished = {
                    sliderPosition?.let { playerConnection.player.seekTo(it); position = it }
                    sliderPosition = null
                },
                modifier = Modifier.fillMaxWidth().height(20.dp),
            )
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = makeTimeString(effectivePosition),
                    color = onSurfaceColor.copy(alpha = 0.6f),
                    fontSize = 10.sp,
                )
                Text(
                    text = if (duration != C.TIME_UNSET) makeTimeString(duration) else "",
                    color = onSurfaceColor.copy(alpha = 0.6f),
                    fontSize = 10.sp,
                )
            }
        }

        Spacer(Modifier.height(4.dp))

        // ROW 3: Transport controls
        Row(
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            // Skip previous
            IconButton(
                onClick = playerConnection::seekToPrevious,
                enabled = canSkipPrevious,
            ) {
                Icon(
                    painter = painterResource(R.drawable.skip_previous),
                    contentDescription = null,
                    tint = if (canSkipPrevious) onSurfaceColor else onSurfaceColor.copy(alpha = 0.3f),
                    modifier = Modifier.size(28.dp),
                )
            }

            // Play/Pause big button
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(playPauseRoundness))
                    .background(onSurfaceColor)
                    .clickable {
                        if (playbackState == androidx.media3.common.Player.STATE_ENDED) {
                            playerConnection.player.seekTo(0, 0)
                            playerConnection.player.playWhenReady = true
                        } else {
                            playerConnection.togglePlayPause()
                        }
                    },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(
                        when {
                            playbackState == androidx.media3.common.Player.STATE_ENDED -> R.drawable.replay
                            isPlaying -> R.drawable.pause
                            else -> R.drawable.play
                        }
                    ),
                    contentDescription = null,
                    tint = if (MaterialTheme.colorScheme.surface == Color.Black) Color.White else Color.Black,
                    modifier = Modifier.size(32.dp),
                )
            }

            // Skip next
            IconButton(
                onClick = playerConnection::seekToNext,
                enabled = canSkipNext,
            ) {
                Icon(
                    painter = painterResource(R.drawable.skip_next),
                    contentDescription = null,
                    tint = if (canSkipNext) onSurfaceColor else onSurfaceColor.copy(alpha = 0.3f),
                    modifier = Modifier.size(28.dp),
                )
            }

            // Favorite
            val database = LocalDatabase.current
            val librarySong by database.song(mediaMetadata?.id ?: "").collectAsState(initial = null)
            val isLiked = librarySong?.song?.liked == true
            IconButton(onClick = { playerConnection.service.toggleLike() }) {
                Icon(
                    painter = painterResource(if (isLiked) R.drawable.favorite else R.drawable.favorite_border),
                    contentDescription = null,
                    tint = onSurfaceColor,
                    modifier = Modifier.size(24.dp),
                )
            }
        }
    }
}
