package com.metrolist.music.ui.player

import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.addOutline
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import com.metrolist.music.LocalPlayerConnection
import com.metrolist.music.R
import com.metrolist.music.constants.IrideSoftBorderWidth
import com.metrolist.music.constants.PlayerBackgroundStyle
import com.metrolist.music.extensions.metadata
import com.metrolist.music.models.MediaMetadata
import com.metrolist.music.ui.component.BottomSheetState
import com.metrolist.music.ui.component.FloatingBottomNavigationHeight
import com.metrolist.music.ui.component.IrideFolio
import com.metrolist.music.ui.component.IrideFolioPlayerPanel
import com.metrolist.music.ui.component.IrideMiniPlayerHeight
import com.metrolist.music.ui.component.LocalBottomSheetPageState
import com.metrolist.music.ui.component.LocalMenuState
import com.metrolist.music.ui.component.TopScreenGradientBackground
import com.metrolist.music.ui.component.irideBottomDockHorizontalInset
import com.metrolist.music.ui.component.irideFolioCombinedClickable
import com.metrolist.music.ui.component.irideFolioPlayerArtworkRequest
import com.metrolist.music.ui.component.iridePlayerExpansionPhase
import com.metrolist.music.ui.component.rememberDeviceMatchedBottomPanelShape
import com.metrolist.music.ui.menu.PlayerMenu
import com.metrolist.music.ui.screens.Screens
import com.metrolist.music.ui.theme.IBMPlexMonoFontFamily
import com.metrolist.music.ui.theme.strokeItemSoft
import com.metrolist.music.ui.utils.IrideMotion
import com.metrolist.music.ui.utils.ShowMediaInfo
import com.metrolist.music.ui.utils.irideArtworkOverlayBorder
import com.metrolist.music.ui.utils.rememberReducedMotion
import com.metrolist.music.utils.makeTimeString
import com.metrolist.music.viewmodels.LyricsViewModel
import kotlin.math.abs

private enum class IrideMp3Page { NowPlaying, Menu, Queue, Video, Lyrics, FindSimilar, Comments, ListenTogether }

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
    onShuffleClick: () -> Unit = {},
    isShuffleActive: Boolean = false,
    onFavoriteClick: () -> Unit = {},
    onSeek: (Float) -> Unit = {},
    onArtistClick: (String) -> Unit = {},
    onLyricsOptions: () -> Unit = {},
    isListenTogetherGuest: Boolean = false,
    isMuted: Boolean = false,
    navController: NavController,
    playerBottomSheetState: BottomSheetState,
    playerBackground: PlayerBackgroundStyle = PlayerBackgroundStyle.GRADIENT,
    modifier: Modifier = Modifier,
    bottomInset: Dp = 0.dp,
    bridgeState: IrideBridgeState? = null,
) {
    val bottomSheetPageState = LocalBottomSheetPageState.current
    var page by rememberSaveable { mutableStateOf(IrideMp3Page.NowPlaying) }
    val showMoreMenu: (com.metrolist.music.ui.component.MenuState) -> Unit = { targetMenu ->
        targetMenu.show(anchored = true) {
            PlayerMenu(
                mediaMetadata = mediaMetadata,
                showDissolve = true,
                navController = navController,
                playerBottomSheetState = playerBottomSheetState,
                onShowDetailsDialog = {
                    bottomSheetPageState.show { ShowMediaInfo(mediaMetadata.id) }
                },
                onDismiss = targetMenu::dismiss,
                onShowComments = { page = IrideMp3Page.Comments },
                onShowListenTogether = { page = IrideMp3Page.ListenTogether },
            )
        }
    }
    var selectedItem by rememberSaveable { mutableIntStateOf(0) }
    var dragFraction by remember { mutableStateOf<Float?>(null) }
    val reducedMotion = rememberReducedMotion()
    val queueControls = remember { IrideMp3QueueControls() }
    val lyricsControls = remember { com.metrolist.music.ui.component.LyricsPillController() }
    val lyricsViewModel: LyricsViewModel = hiltViewModel()
    val unavailableLyricsMediaId by lyricsViewModel.unavailableLyricsMediaId.collectAsState()
    val showLyricsEntry = unavailableLyricsMediaId != mediaMetadata.id
    val context = LocalContext.current
    val displayState = remember { IrideMp3DisplayState() }
    val videoConnection = LocalPlayerConnection.current
    val videoState by
        videoConnection?.videoPlayback?.state?.collectAsState()
            ?: remember { mutableStateOf(com.metrolist.music.playback.VideoPlaybackState()) }
    val videoOpenRequest by
        videoConnection?.videoPlayback?.openPlayerRequests?.collectAsState()
            ?: remember { mutableStateOf(0L) }
    var handledVideoOpenRequest by rememberSaveable { mutableStateOf(0L) }
    val pendingVideoOpen = videoOpenRequest > handledVideoOpenRequest
    val videoPlayerExpanded = !playerBottomSheetState.isCollapsed && !playerBottomSheetState.isDismissed
    LaunchedEffect(videoOpenRequest, videoState.isVideo, videoState.mediaId, mediaMetadata.id, videoPlayerExpanded) {
        if (pendingVideoOpen && videoPlayerExpanded &&
            videoConnection?.videoPlayback?.isOpenPlayerReady(mediaMetadata.id) == true
        ) {
            handledVideoOpenRequest = videoOpenRequest
            selectedItem = 2
            page = IrideMp3Page.Video
            displayState.requestFullscreen(true)
        }
    }
    LaunchedEffect(videoState.isVideo) {
        if (videoState.isVideo && !pendingVideoOpen) {
            selectedItem = 2
            page = IrideMp3Page.Video
        }
    }
    val videoPanelVisible =
        page == IrideMp3Page.Video &&
            (!playerBottomSheetState.isCollapsed && !playerBottomSheetState.isDismissed ||
                displayState.inFullscreenWindow)
    LaunchedEffect(videoPanelVisible, mediaMetadata.id, videoOpenRequest) {
        if (videoPanelVisible) videoConnection?.videoPlayback?.enterVideoMode(mediaMetadata)
        else if (!pendingVideoOpen) {
            videoConnection?.videoPlayback?.leaveVideoMode()
        }
    }
    DisposableEffect(videoConnection) {
        onDispose { videoConnection?.videoPlayback?.leaveVideoMode() }
    }
    val categories =
        listOf(
            IrideMp3Page.NowPlaying,
            IrideMp3Page.Queue,
            IrideMp3Page.Video,
            IrideMp3Page.Lyrics,
            IrideMp3Page.FindSimilar,
            IrideMp3Page.Comments,
            IrideMp3Page.ListenTogether,
        )
    val availableCategoryIndices =
        IrideMp3MenuOrder.filter { index ->
            showLyricsEntry || categories[index] != IrideMp3Page.Lyrics
        }
    LaunchedEffect(availableCategoryIndices) {
        if (selectedItem !in availableCategoryIndices)
            selectedItem = availableCategoryIndices.first()
    }
    fun moveMenuSelection(direction: Int) {
        val current = availableCategoryIndices.indexOf(selectedItem).coerceAtLeast(0)
        selectedItem =
            availableCategoryIndices[
                (current + direction + availableCategoryIndices.size) %
                    availableCategoryIndices.size]
    }
    val menuOpen = page == IrideMp3Page.Menu
    val openMenu = {
        categories.indexOf(page).takeIf { it >= 0 }?.let { selectedItem = it }
        page = IrideMp3Page.Menu
    }
    val previousClick by rememberUpdatedState(onPreviousClick)
    val nextClick by rememberUpdatedState(onNextClick)
    fun selectCategory(index: Int) {
        if (index !in availableCategoryIndices) return
        selectedItem = index
        if (categories[index] == IrideMp3Page.FindSimilar) {
            val query =
                (listOf(mediaMetadata.title) + mediaMetadata.artists.map { it.name }).joinToString(
                    " "
                )
            page = IrideMp3Page.NowPlaying
            playerBottomSheetState.collapseSoft()
            navController.navigate(Screens.Search.route) { launchSingleTop = true }
            navController.currentBackStackEntry?.savedStateHandle?.set("playerSearchQuery", query)
        } else page = categories[index]
    }

    LaunchedEffect(playerBottomSheetState.isCollapsed, playerBottomSheetState.isDismissed) {
        if (
            displayState.inFullscreenWindow &&
                (playerBottomSheetState.isCollapsed || playerBottomSheetState.isDismissed)
        ) {
            displayState.requestFullscreen(false)
        }
    }
    BackHandler(enabled = page != IrideMp3Page.NowPlaying && !displayState.inFullscreenWindow) {
        if (menuOpen) page = IrideMp3Page.NowPlaying else openMenu()
    }
    SideEffect {
        bridgeState?.panelActive = page != IrideMp3Page.NowPlaying
        bridgeState?.lyricsFullScreenActive = displayState.inFullscreenWindow
    }
    DisposableEffect(bridgeState) {
        onDispose {
            bridgeState?.panelActive = false
            bridgeState?.lyricsFullScreenActive = false
        }
    }

    BoxWithConstraints(modifier.fillMaxSize().clipToBounds()) {
        val sheetProgress = playerBottomSheetState.progress.coerceIn(0f, 1f)
        val backgroundAlpha = irideMp3ShellAlpha(sheetProgress)
        Box(
            Modifier.fillMaxSize()
                .graphicsLayer { alpha = backgroundAlpha }
                .mp3MatteSurface(IrideMp3BackgroundColor),
        )
        val panelWidth = maxWidth
        val panelHeight = maxHeight
        val textScale = (panelWidth / 360.dp).coerceIn(0.90f, 1.1f)
        val topSpacing = minOf(32.dp, panelHeight * 0.05f)
        val bottomSpacing = maxOf(bottomInset, 20.dp).coerceAtMost(panelHeight)
        val availableHeight = (panelHeight - topSpacing - bottomSpacing).coerceAtLeast(0.dp)
        val preferredScreenHeight = panelWidth * 1.12f + 8.dp
        val screenHeight = minOf(preferredScreenHeight, availableHeight * 0.68f)
        val wheelTopSpacing = minOf(28.dp, availableHeight * .045f)
        val wheelHeight = (availableHeight - screenHeight - wheelTopSpacing).coerceAtLeast(0.dp)
        val wheelSize = minOf(216.dp, panelWidth * .56f, wheelHeight * .94f)
        val measuredWheel = IrideMp3WheelGeometry(wheelSize)
        var inlineWheel by remember { mutableStateOf(measuredWheel) }
        SideEffect { if (!displayState.inFullscreenWindow) inlineWheel = measuredWheel }
        val wheelGeometry = if (displayState.inFullscreenWindow) inlineWheel else measuredWheel
        val coverShape = irideSquircle(IrideCoverBorderRadius)
        val finish = ((sheetProgress - 0.5f) / 0.5f).coerceIn(0f, 1f)
        val dockBottomSpacing = (playerBottomSheetState.collapsedBound -
            IrideMiniPlayerHeight - FloatingBottomNavigationHeight).coerceAtLeast(0.dp)
        val dividerInSheet = playerBottomSheetState.value - dockBottomSpacing - FloatingBottomNavigationHeight
        val scale = 0.85f + 0.15f * finish
        val deviceTop = IrideMiniPlayerHeight * (1f - finish)
        val screenBottom = deviceTop + (topSpacing + screenHeight) * scale
        val wheelTop = deviceTop + (topSpacing + screenHeight + wheelTopSpacing +
            (wheelHeight - wheelSize) / 2) * scale
        // The display stays behind the divider; the wheel uses the pill's outer mask instead.
        val clipRelease = ((dividerInSheet - screenBottom) /
            (wheelTop - screenBottom).coerceAtLeast(1.dp)).coerceIn(0f, 1f)
        val dockPhase = iridePlayerExpansionPhase(sheetProgress)
        val pillBottomSpacing = dockBottomSpacing * (1f - dockPhase)
        val pillBottom = playerBottomSheetState.value - pillBottomSpacing
        // Finish fading the labels before the wheel reaches the pill's lower edge.
        val navigationFade = ((pillBottom - screenBottom) /
            (wheelTop - screenBottom).coerceAtLeast(1.dp)).coerceIn(0f, 1f)
        val miniPlayerAlpha = 1f - navigationFade
        val pillMask = rememberDeviceMatchedBottomPanelShape(
            horizontalPadding = irideBottomDockHorizontalInset() * (1f - dockPhase),
            additionalBottomSpacing = 0.dp,
            topRadius = IrideFolio.ControlsCornerRadius + (32.dp - IrideFolio.ControlsCornerRadius) * dockPhase,
            bottomSpacing = pillBottomSpacing,
            simpleOutline = true,
        )
        SideEffect { bridgeState?.miniPlayerAlpha = miniPlayerAlpha }
        Column(
            Modifier.fillMaxSize()
                .drawWithContent {
                    val outline = pillMask.createOutline(
                        Size(size.width, pillBottom.toPx().coerceIn(0f, size.height)),
                        layoutDirection,
                        this,
                    )
                    val mask = Path().apply { addOutline(outline) }
                    clipPath(mask) { this@drawWithContent.drawContent() }
                }
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    transformOrigin = TransformOrigin(0.5f, 0f)
                    translationY = IrideMiniPlayerHeight.toPx() * (1f - finish)
                }
                .padding(top = topSpacing, bottom = bottomSpacing)
        ) {
            IrideMp3DisplayHost(
                state = displayState,
                mediaId = mediaMetadata.id,
                modifier =
                    Modifier.fillMaxWidth()
                        .drawWithContent {
                            val divider = ((dividerInSheet - deviceTop) / scale - topSpacing).toPx()
                            clipRect(bottom = (divider + (size.height - divider) * clipRelease).coerceIn(0f, size.height)) {
                                this@drawWithContent.drawContent()
                            }
                        }
                        .height(screenHeight)
                        .padding(horizontal = IrideMp3FrameHorizontalPadding),
            ) { presentation ->
                val displayMenu = LocalMenuState.current
                val onMoreClick = { showMoreMenu(displayMenu) }
                val expansion = presentation.expansion
                val controlsOpacity = presentation.controlsOpacity
                val frameInset = IrideMp3FrameInset * (1f - expansion)
                val viewportShape = irideSquircle(IrideCoverBorderRadius * (1f - expansion))
                val density = LocalDensity.current
                val safeBottom = with(density) { WindowInsets.displayCutout.getBottom(this).toDp() }
                val displayTextScale =
                    if (displayState.inFullscreenWindow) {
                        val sourceWidth =
                            with(density) { displayState.transitionBounds?.width?.toDp() }
                        sourceWidth?.let {
                            ((it + IrideMp3FrameHorizontalPadding * 2) / 360.dp).coerceIn(
                                .90f,
                                1.1f,
                            )
                        } ?: textScale
                    } else textScale
                Box(
                    Modifier.fillMaxSize()
                        .clip(IrideMp3FrameShape)
                        .background(Color.Black)
                        .border(
                            1.dp,
                            IrideMp3FrameBorderColor.copy(alpha = 1f - expansion),
                            IrideMp3FrameShape,
                        )
                ) {
                    TopScreenGradientBackground(
                        mediaMetadata = mediaMetadata,
                        playerBackground =
                            when (playerBackground) {
                                PlayerBackgroundStyle.DEFAULT,
                                PlayerBackgroundStyle.BLUR -> PlayerBackgroundStyle.GRADIENT
                                else -> playerBackground
                            },
                        emphasizeArtwork = true,
                        backgroundColor = IrideMp3BackgroundColor,
                        artworkGradientHeight = screenHeight,
                    )
                    IrideMp3DisplayLayout(expansion, Modifier.fillMaxSize().padding(frameInset)) {
                        Box(
                            modifier =
                                Modifier.fillMaxSize().clip(viewportShape).pointerInput(
                                    isListenTogetherGuest,
                                    page,
                                ) {
                                    if (isListenTogetherGuest || page != IrideMp3Page.NowPlaying)
                                        return@pointerInput
                                    val threshold = maxOf(48.dp.toPx(), size.width * 0.20f)
                                    awaitEachGesture {
                                        val down = awaitFirstDown(requireUnconsumed = false)
                                        var distance = Offset.Zero
                                        var horizontal = false
                                        while (true) {
                                            val change =
                                                awaitPointerEvent().changes.firstOrNull {
                                                    it.id == down.id
                                                } ?: break
                                            if (change.isConsumed) break
                                            if (!change.pressed) {
                                                if (horizontal && distance.x <= -threshold)
                                                    nextClick()
                                                else if (horizontal && distance.x >= threshold)
                                                    previousClick()
                                                break
                                            }
                                            distance += change.positionChange()
                                            if (
                                                !horizontal &&
                                                    distance.getDistance() >
                                                        viewConfiguration.touchSlop
                                            ) {
                                                if (abs(distance.x) < abs(distance.y) * 1.7f) break
                                                horizontal = true
                                            }
                                            if (horizontal) change.consume()
                                        }
                                    }
                                }
                        ) {
                            when (page) {
                                IrideMp3Page.NowPlaying ->
                                    Crossfade(
                                        targetState = mediaMetadata.thumbnailUrl,
                                        animationSpec =
                                            tween(
                                                if (reducedMotion) 0 else IrideMotion.Short,
                                                easing = IrideMotion.EaseOutQuart,
                                            ),
                                        label = "mp3Artwork",
                                        modifier = Modifier.fillMaxSize(),
                                    ) { thumbnailUrl ->
                                        Box(
                                            Modifier.fillMaxSize(),
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            AsyncImage(
                                                model =
                                                    remember(context, thumbnailUrl) {
                                                        irideFolioPlayerArtworkRequest(
                                                            context,
                                                            thumbnailUrl,
                                                        )
                                                    },
                                                contentDescription =
                                                    stringResource(R.string.mp3_now_playing),
                                                contentScale = ContentScale.Crop,
                                                modifier =
                                                    Modifier.fillMaxSize()
                                                        .irideReportRect {
                                                            bridgeState?.playerArt = it
                                                        }
                                                        .irideArtworkOverlayBorder(
                                                            IrideSoftBorderWidth,
                                                            MaterialTheme.colorScheme
                                                                .strokeItemSoft,
                                                            coverShape,
                                                        )
                                                        .clip(coverShape)
                                                        .irideFolioCombinedClickable(
                                                            interactionSource =
                                                                remember {
                                                                    MutableInteractionSource()
                                                                },
                                                            indication = null,
                                                            onClick = {},
                                                            onLongClick = onMoreClick,
                                                        ),
                                            )
                                        }
                                    }
                                IrideMp3Page.Menu ->
                                    IrideMp3Menu(
                                        mediaMetadata = mediaMetadata,
                                        remaining = (duration - position).coerceAtLeast(0),
                                        selectedItem = selectedItem,
                                        textScale = textScale,
                                        showLyrics = showLyricsEntry,
                                        onSelect = ::selectCategory,
                                    )
                                IrideMp3Page.Comments ->
                                    CommentsPanel(
                                        mediaId = mediaMetadata.id,
                                        textScale = displayTextScale,
                                        onClose = openMenu,
                                        textButtonColor = Color.White,
                                        iconButtonColor = Color.White,
                                    )
                                IrideMp3Page.ListenTogether ->
                                    ListenTogetherPanel(
                                        onClose = openMenu,
                                        onSettings = {
                                            page = IrideMp3Page.NowPlaying
                                            playerBottomSheetState.collapseSoft()
                                            navController.navigate("settings/integrations/listen_together") {
                                                launchSingleTop = true
                                            }
                                        },
                                    )
                                IrideMp3Page.Queue ->
                                    IrideMp3Queue(
                                        queueControls,
                                        duration - position,
                                        isListenTogetherGuest,
                                        playerBottomSheetState,
                                        onNowPlaying = { page = IrideMp3Page.NowPlaying },
                                        textScale = textScale,
                                    )
                                IrideMp3Page.Lyrics ->
                                    IrideMp3Lyrics(
                                        position = position,
                                        controller = lyricsControls,
                                        expansion = expansion,
                                        controlsOpacity = controlsOpacity,
                                        onOptions = {
                                            displayState.requestFullscreen(false)
                                            onLyricsOptions()
                                        },
                                        onKeepControlsVisible = {
                                            displayState.keepControlsVisible = it
                                        },
                                        onInteraction = displayState::revealControls,
                                    )
                                IrideMp3Page.Video ->
                                    IrideMp3Video(
                                        metadata = mediaMetadata,
                                        duration = duration,
                                        isPlaying = isPlaying,
                                        isGuest = isListenTogetherGuest,
                                        onSeek = onSeek,
                                        fullscreen = displayState.fullscreen,
                                        onToggleControls = displayState::toggleControls,
                                        morphing = expansion > 0f && expansion < 1f,
                                    )
                                IrideMp3Page.FindSimilar -> Unit
                            }
                        }
                        IrideFolioPlayerPanel(
                            expansion = expansion,
                            opacity = controlsOpacity,
                            bottomInset = safeBottom,
                            onReveal = displayState::revealControls,
                            transportHeight = wheelGeometry.centerSize,
                            transport = {
                                IrideMp3Transport(
                                    geometry = wheelGeometry,
                                    isPlaying = isPlaying,
                                    isGuest = isListenTogetherGuest,
                                    isMuted = isMuted,
                                    canSkipNext =
                                        videoConnection?.canSkipNext?.collectAsState()?.value ==
                                            true,
                                    reducedMotion = reducedMotion,
                                    onPlayPause = onPlayPauseClick,
                                    onPrevious = onPreviousClick,
                                    onNext = onNextClick,
                                )
                            },
                            info = {
                                Column(
                                    Modifier.fillMaxWidth()
                                        .padding(start = 6.dp, end = 6.dp, bottom = 6.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement =
                                            androidx.compose.foundation.layout.Arrangement
                                                .SpaceBetween,
                                    ) {
                                        val displayedPosition =
                                            dragFraction?.let { (duration * it).toLong() }
                                                ?: position
                                        listOf(
                                                makeTimeString(displayedPosition),
                                                makeTimeString(duration),
                                            )
                                            .forEach { time ->
                                                Text(
                                                    text = time,
                                                    color = Color.White.copy(alpha = 0.75f),
                                                    fontFamily = IBMPlexMonoFontFamily,
                                                    fontSize = (10 * displayTextScale).sp,
                                                    lineHeight = (14 * displayTextScale).sp,
                                                )
                                            }
                                    }
                                    IrideMp3Progress(
                                        position = position,
                                        duration = duration,
                                        dragFraction = dragFraction,
                                        enabled = !isListenTogetherGuest,
                                        onDragFraction = { dragFraction = it },
                                        onSeek = onSeek,
                                    )
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Column(
                                            modifier =
                                                Modifier.weight(1f).irideReportRect {
                                                    bridgeState?.playerInfo = it
                                                }
                                        ) {
                                            Text(
                                                text = mediaMetadata.title,
                                                color = Color.White,
                                                fontFamily = IBMPlexMonoFontFamily,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = (16 * displayTextScale).sp,
                                                lineHeight = (21 * displayTextScale).sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier =
                                                    Modifier.basicMarquee(
                                                            iterations = 1,
                                                            initialDelayMillis = 2000,
                                                        )
                                                        .irideFolioCombinedClickable(
                                                            interactionSource =
                                                                remember {
                                                                    MutableInteractionSource()
                                                                },
                                                            indication = null,
                                                            onClick = {},
                                                            onLongClick = onMoreClick,
                                                        ),
                                            )
                                            IrideArtistText(
                                                mediaMetadata = mediaMetadata,
                                                color = IrideArtistTextColor,
                                                fontSize = (12 * displayTextScale).sp,
                                                lineHeight = (16 * displayTextScale).sp,
                                                fontWeight = FontWeight.Normal,
                                                fontFamily = IBMPlexMonoFontFamily,
                                                onArtistClick = onArtistClick,
                                                modifier = Modifier.fillMaxWidth(),
                                            )
                                        }
                                        IrideMp3InfoActions(
                                            isFavorite,
                                            onFavoriteClick,
                                            onMoreClick,
                                            reducedMotion,
                                            expansion = expansion,
                                            fullscreenAction =
                                                page == IrideMp3Page.Video ||
                                                    page == IrideMp3Page.Lyrics,
                                            contextualIcon =
                                                when (page) {
                                                    IrideMp3Page.Video,
                                                    IrideMp3Page.Lyrics -> R.drawable.fullscreen
                                                    else -> null
                                                },
                                            contextualDescription =
                                                stringResource(
                                                    if (displayState.fullscreen)
                                                        R.string.mp3_exit_fullscreen
                                                    else R.string.mp3_fullscreen
                                                ),
                                            onContextualClick = {
                                                when (page) {
                                                    IrideMp3Page.Lyrics,
                                                    IrideMp3Page.Video ->
                                                        displayState.requestFullscreen(
                                                            !displayState.fullscreen
                                                        )
                                                    else -> Unit
                                                }
                                            },
                                        )
                                    }
                                }
                            },
                        )
                    }
                }
            }
            BoxWithConstraints(
                modifier = Modifier.weight(1f).fillMaxWidth().padding(top = wheelTopSpacing),
                contentAlignment = Alignment.Center,
            ) {
                IrideClickWheel(
                    isPlaying = isPlaying,
                    isShuffleActive = isShuffleActive,
                    isMenuOpen = menuOpen,
                    onMenuClick = openMenu,
                    onMoveUp = { moveMenuSelection(-1) },
                    onMoveDown = { moveMenuSelection(1) },
                    isListenTogetherGuest = isListenTogetherGuest,
                    isMuted = isMuted,
                    onPlayPauseClick = onPlayPauseClick,
                    onPreviousClick = onPreviousClick,
                    onNextClick = onNextClick,
                    onRadioClick = onShuffleClick,
                    onCenterClick = {
                        if (menuOpen) selectCategory(selectedItem) else onPlayPauseClick()
                    },
                    wheelSize = wheelGeometry.diameter,
                )
            }
        }
    }
}
