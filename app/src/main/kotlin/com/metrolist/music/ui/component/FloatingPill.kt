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
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
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
import com.metrolist.music.constants.CropAlbumArtKey
import com.metrolist.music.constants.DarkModeKey
import com.metrolist.music.constants.HidePlayerThumbnailKey
import com.metrolist.music.constants.MiniPlayerBackgroundStyle
import com.metrolist.music.constants.MiniPlayerBackgroundStyleKey
import com.metrolist.music.constants.MiniPlayerHeight
import com.metrolist.music.constants.PlayerButtonsStyle
import com.metrolist.music.constants.PlayerButtonsStyleKey
import com.metrolist.music.constants.PureBlackKey
import com.metrolist.music.constants.SliderStyle
import com.metrolist.music.constants.SliderStyleKey
import com.metrolist.music.constants.SquigglySliderKey
import com.metrolist.music.constants.UseNewPlayerDesignKey
import com.metrolist.music.listentogether.ListenTogetherManager
import com.metrolist.music.listentogether.RoomRole
import com.metrolist.music.models.MediaMetadata
import com.metrolist.music.playback.CastConnectionHandler
import com.metrolist.music.playback.PlayerConnection
import com.metrolist.music.playback.SongWithArtists
import com.metrolist.music.ui.player.PlayerControlsPanel
import com.metrolist.music.ui.screens.Screens
import com.metrolist.music.ui.screens.settings.DarkMode
import com.metrolist.music.ui.theme.PlayerColorExtractor
import com.metrolist.music.ui.utils.resize
import com.metrolist.music.utils.makeTimeString
import com.metrolist.music.utils.rememberEnumPreference
import com.metrolist.music.utils.rememberPreference
import com.valentinilk.shimmer.shimmer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext

private val NavRowHeight = 56.dp
val FloatingPillHeight = MiniPlayerHeight + NavRowHeight
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
    val miniPlayerBackground by rememberEnumPreference(MiniPlayerBackgroundStyleKey, defaultValue = MiniPlayerBackgroundStyle.DEFAULT)
    val context = LocalContext.current
    var gradientColors by remember { mutableStateOf<List<Color>>(emptyList()) }
    val isSystemInDarkTheme = isSystemInDarkTheme()
    val darkTheme by rememberEnumPreference(DarkModeKey, defaultValue = DarkMode.ON)
    val useDarkTheme = remember(darkTheme, isSystemInDarkTheme) { if (darkTheme == DarkMode.AUTO) isSystemInDarkTheme else darkTheme == DarkMode.ON }

    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()
    val playbackState by playerConnection.playbackState.collectAsState()
    val canSkipNext by playerConnection.canSkipNext.collectAsState()

    val castHandler = remember(playerConnection) { try { playerConnection.service.castConnectionHandler } catch (e: Exception) { null } }
    val isCasting by castHandler?.isCasting?.collectAsState() ?: remember { mutableStateOf(false) }

    val positionState = remember { mutableLongStateOf(0L) }
    val durationState = remember { mutableLongStateOf(0L) }
    val progressState = remember { PillProgressState(positionState, durationState) }
    val isPlaying by playerConnection.isPlaying.collectAsState()
    val listenTogetherManager = LocalListenTogetherManager.current

    LaunchedEffect(isPlaying, isCasting) { if (!isCasting && isPlaying) { while (isActive) { delay(100); positionState.longValue = playerConnection.player.currentPosition; durationState.longValue = playerConnection.player.duration } } }
    LaunchedEffect(playbackState, mediaMetadata?.id) { if (!isCasting) { positionState.longValue = playerConnection.player.currentPosition; durationState.longValue = playerConnection.player.duration } }

    LaunchedEffect(mediaMetadata?.id, miniPlayerBackground) {
        gradientColors = emptyList()
        if (miniPlayerBackground == MiniPlayerBackgroundStyle.GRADIENT) {
            val url = mediaMetadata?.thumbnailUrl
            if (url != null) {
                withContext(Dispatchers.IO) {
                    val result = runCatching { context.imageLoader.execute(ImageRequest.Builder(context).data(url).size(100, 100).allowHardware(false).build()) }.getOrNull()
                    val bitmap = result?.image?.toBitmap()
                    if (bitmap != null) {
                        val palette = withContext(Dispatchers.Default) { Palette.from(bitmap).maximumColorCount(8).resizeBitmapArea(100 * 100).generate() }
                        val extracted = PlayerColorExtractor.extractGradientColors(palette = palette, fallbackColor = 0xFF000000.toInt())
                        withContext(Dispatchers.Main) { gradientColors = extracted }
                    }
                }
            }
        }
    }

    val effectiveBackground = if (pureBlack && miniPlayerBackground == MiniPlayerBackgroundStyle.DEFAULT) MiniPlayerBackgroundStyle.PURE_BLACK else miniPlayerBackground
    val backgroundColor = when (effectiveBackground) {
        MiniPlayerBackgroundStyle.DEFAULT     -> MaterialTheme.colorScheme.surfaceContainer
        MiniPlayerBackgroundStyle.TRANSPARENT -> Color.Black.copy(alpha = 0.25f)
        MiniPlayerBackgroundStyle.BLUR        -> MaterialTheme.colorScheme.surfaceContainer
        MiniPlayerBackgroundStyle.GRADIENT    -> MaterialTheme.colorScheme.surfaceContainer
        MiniPlayerBackgroundStyle.PURE_BLACK  -> Color.Black
    }
    val forceLightColors = !useDarkTheme && (effectiveBackground == MiniPlayerBackgroundStyle.PURE_BLACK || effectiveBackground == MiniPlayerBackgroundStyle.BLUR || effectiveBackground == MiniPlayerBackgroundStyle.GRADIENT)
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
    val animatedHeight by animateDpAsState(targetValue = targetPillHeight, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow), label = "pillHeight")

    Column(modifier = Modifier.fillMaxWidth().height(animatedHeight).clip(RoundedCornerShape(28.dp)).background(backgroundColor).border(1.dp, outlineColor.copy(alpha = 0.3f), RoundedCornerShape(28.dp))) {
        Box(modifier = Modifier.fillMaxSize()) {
            when (effectiveBackground) {
                MiniPlayerBackgroundStyle.BLUR -> {
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                        mediaMetadata?.thumbnailUrl?.let { url ->
                            AsyncImage(model = url, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize().blur(60.dp))
                            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.45f)))
                        }
                    }
                }
                MiniPlayerBackgroundStyle.GRADIENT -> {
                    val colors = if (gradientColors.isNotEmpty()) gradientColors else listOf(MaterialTheme.colorScheme.surfaceContainer, MaterialTheme.colorScheme.surfaceContainer)
                    Box(Modifier.fillMaxSize().background(Brush.horizontalGradient(colors)).background(Color.Black.copy(alpha = 0.15f)))
                }
                else -> {}
            }

            Column(modifier = Modifier.fillMaxWidth()) {
                val showExpanded = pillProgress > 0.05f
                AnimatedContent(targetState = showExpanded, transitionSpec = { fadeIn(animationSpec = tween(200)) togetherWith fadeOut(animationSpec = tween(150)) }, label = "pillContentSwitch") { expanded ->
                    if (!expanded) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Box(modifier = Modifier.fillMaxWidth().height(MiniPlayerHeight).clickable { playerBottomSheetState.expandSoft() }, contentAlignment = Alignment.Center) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp, vertical = 8.dp)) {
                                    PillPlayButton(progressState = progressState, mediaMetadata = mediaMetadata, primaryColor = primaryColor, outlineColor = outlineColor)
                                    Spacer(Modifier.width(16.dp))
                                    PillSongInfo(mediaMetadata = mediaMetadata, onSurfaceColor = onSurfaceColor, errorColor = errorColor, modifier = Modifier.weight(1f))
                                    Spacer(Modifier.width(8.dp))
                                    if (isCasting) { Icon(painter = painterResource(R.drawable.cast_connected), contentDescription = null, tint = primaryColor, modifier = Modifier.size(20.dp)); Spacer(Modifier.width(8.dp)) }
                                    mediaMetadata?.let { PillFavoriteButton(songId = it.id, onSurfaceColor = onSurfaceColor, playerConnection = playerConnection) }
                                    Spacer(Modifier.width(4.dp))
                                    PillPlayPauseButton(playbackState = playbackState, isCasting = isCasting, castHandler = castHandler, playerConnection = playerConnection, listenTogetherManager = listenTogetherManager, onSurfaceColor = onSurfaceColor)
                                    PillSkipNextButton(canSkipNext = canSkipNext, playerConnection = playerConnection, listenTogetherManager = listenTogetherManager, onSurfaceColor = onSurfaceColor)
                                }
                            }
                            AnimatedVisibility(visible = isTopLevelRoute, enter = fadeIn(tween(300)) + slideInVertically(tween(320), initialOffsetY = { it }), exit = fadeOut(tween(250)) + slideOutVertically(tween(280), targetOffsetY = { it })) {
                                Row(modifier = Modifier.fillMaxWidth().height(NavRowHeight).padding(horizontal = 8.dp), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                                    navigationItems.forEach { screen -> PillNavItem(screen = screen, currentRoute = currentRoute, navigationItems = navigationItems, onNavItemClick = onNavItemClick, onSearchLongClick = onSearchLongClick, accountImageUrl = accountImageUrl, tintSelected = primaryColor, tintUnselected = onSurfaceColor.copy(alpha = 0.6f), showLabel = !slimNav) }
                                }
                            }
                        }
                    } else {
                        PillExpandedControls(navController = navController, playerBottomSheetState = playerBottomSheetState, playerConnection = playerConnection, onSurfaceColor = onSurfaceColor, primaryColor = primaryColor, backgroundColor = backgroundColor, modifier = Modifier.fillMaxSize())
                    }
                }
            }
        }
    }
}
