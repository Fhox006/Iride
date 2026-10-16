package com.metrolist.music.ui.component

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateValueAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionOnScreen
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.metrolist.music.ui.screens.Screens
import com.metrolist.music.ui.theme.SpaceMonoFontFamily
import com.metrolist.music.ui.utils.rememberReducedMotion
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sqrt
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

val FloatingBottomNavigationHeight = IrideFolio.NavigationHeight
internal val FloatingBottomNavigationPillHeight = IrideFolio.NavigationSurfaceHeight
internal val FloatingBottomNavigationOffset = 6.dp

@Composable
fun FloatingBottomNavigation(
    navigationItems: List<Screens>,
    currentRoute: String?,
    onItemClick: (Screens, Boolean) -> Unit,
    backdrop: FrostBackdrop?,
    visible: Boolean = true,
    modifier: Modifier = Modifier,
    contentColor: Color = Color.White,
    backgroundColor: Color? = null,
    surfaceAlpha: Float? = null,
) {
    val onSurface = contentColor
    val surface = IrideFolio.frostedSurfaceColor(onSurface, backgroundColor)
    val navigationSurfaceAlpha = surfaceAlpha ?: IrideFolio.floatingPillSurfaceAlpha(backdrop, backgroundColor)
    val groupShape = IrideFolio.ControlsShape
    val borderColor = onSurface.copy(alpha = 0.22f)
    val groupedScreens = remember(navigationItems) { navigationItems.filter { it != Screens.Search } }
    val activeScreen = Screens.MainScreens.firstOrNull {
        isRouteSelected(currentRoute, it.route, navigationItems)
    }
    var rememberedScreenRoute by rememberSaveable { mutableStateOf(Screens.Home.route) }
    var rememberedGroupIndex by rememberSaveable { mutableIntStateOf(0) }
    LaunchedEffect(activeScreen) {
        if (activeScreen != null) {
            rememberedScreenRoute = activeScreen.route
            groupedScreens.indexOf(activeScreen).takeIf { it >= 0 }?.let { rememberedGroupIndex = it }
        }
    }
    val selectedRoute = activeScreen?.route ?: rememberedScreenRoute
    val selectedGroupIndex = groupedScreens.indexOfFirst { it.route == selectedRoute }
        .takeIf { it >= 0 } ?: rememberedGroupIndex
    var visualGroupIndex by remember { mutableIntStateOf(selectedGroupIndex) }
    var dragPosition by remember { mutableFloatStateOf(selectedGroupIndex.toFloat()) }
    var isDragging by remember { mutableStateOf(false) }
    var floatingDragOffset by remember { mutableStateOf(Offset.Zero) }
    var isFloatingDragged by remember { mutableStateOf(false) }
    var snapFromSearch by remember { mutableStateOf(false) }
    var skippedGroupIndices by remember { mutableStateOf(IntRange.EMPTY) }
    val reducedMotion = rememberReducedMotion()
    val coroutineScope = rememberCoroutineScope()
    val currentOnItemClick by rememberUpdatedState(onItemClick)
    var navigationJob by remember { mutableStateOf<Job?>(null) }
    val selectScreen: (Screens, Boolean, Boolean) -> Unit = { screen, selected, fromDrag ->
        navigationJob?.cancel()
        val targetIndex = groupedScreens.indexOf(screen)
        skippedGroupIndices = if (!fromDrag && !selected && targetIndex >= 0 &&
            selectedRoute != Screens.Search.route
        ) {
            (minOf(selectedGroupIndex, targetIndex) + 1) until maxOf(selectedGroupIndex, targetIndex)
        } else {
            IntRange.EMPTY
        }
        if (targetIndex >= 0) visualGroupIndex = targetIndex
        snapFromSearch = selectedRoute == Screens.Search.route
        navigationJob = coroutineScope.launch {
            if (!selected && !reducedMotion && screen != Screens.Search && selectedRoute != Screens.Search.route) {
                // Let the indicator draw its first movement before composing the destination.
                repeat(3) { withFrameNanos { } }
            }
            currentOnItemClick(screen, selected)
        }
    }
    LaunchedEffect(selectedGroupIndex) {
        if (!isDragging) visualGroupIndex = selectedGroupIndex
    }
    LaunchedEffect(visible) {
        if (!visible) {
            navigationJob?.cancel()
            isDragging = false
            skippedGroupIndices = IntRange.EMPTY
            visualGroupIndex = selectedGroupIndex
        }
    }
    LaunchedEffect(selectedRoute, snapFromSearch) {
        if (snapFromSearch && selectedRoute != Screens.Search.route) {
            withFrameNanos { }
            snapFromSearch = false
        }
    }
    val animatedPosition by animateFloatAsState(
        targetValue = if (isDragging) dragPosition else visualGroupIndex.toFloat(),
        animationSpec = if (reducedMotion || selectedRoute == Screens.Search.route ||
            rememberedScreenRoute == Screens.Search.route || snapFromSearch
        ) snap() else spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow,
        ),
        label = "bottomNavigationIndicatorPosition",
        finishedListener = { skippedGroupIndices = IntRange.EMPTY },
    )
    val indicatorAlpha by animateFloatAsState(
        targetValue = if (selectedRoute == Screens.Search.route) 0f else 1f,
        animationSpec = if (reducedMotion) snap() else tween(
            IrideFolio.NavigationReturnDurationMillis,
            easing = IrideFolio.NavigationMotionEasing,
        ),
        label = "bottomNavigationIndicatorAlpha",
    )
    val floatingOffset = animateValueAsState(
        targetValue = if (reducedMotion) Offset.Zero else floatingDragOffset,
        typeConverter = Offset.VectorConverter,
        animationSpec = if (reducedMotion) snap() else spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = if (isFloatingDragged) 180f else 55f,
            visibilityThreshold = Offset(0.002f, 0.002f),
        ),
        label = "bottomNavigationFloatingOffset",
    )
    // Keep selection state while hidden, without leaving invisible pointer targets above the player.
    if (!visible) return
    Row(
        modifier = modifier.widthIn(max = IrideFolio.NavigationMaxWidth).fillMaxWidth()
            .padding(horizontal = IrideFolio.HorizontalPadding)
            .selectableGroup()
            .graphicsLayer {
                val offset = floatingOffset.value
                translationX = offset.x * 6.dp.toPx()
                translationY = offset.y * 6.dp.toPx()
            }
            .pointerInput(visible, selectedRoute, reducedMotion, groupedScreens) {
                if (!visible) return@pointerInput
                val groupWidthPx = size.width - (IrideFolio.NavigationTouchTargetSize + IrideFolio.NavigationGap).toPx()
                val segmentWidthPx = groupWidthPx / groupedScreens.size
                if (segmentWidthPx <= 0f) return@pointerInput
                val activationThresholdPx = 24.dp.toPx()
                val resistancePx = 120.dp.toPx()
                var startedOnGroup = false
                var horizontalNavigation: Boolean? = null
                var rawDragPosition = 0f
                var travel = Offset.Zero
                try {
                    detectDragGestures(
                        onDragStart = { start ->
                            navigationJob?.cancel()
                            skippedGroupIndices = IntRange.EMPTY
                            startedOnGroup = start.x < groupWidthPx
                            horizontalNavigation = null
                            rawDragPosition = selectedGroupIndex.toFloat()
                            travel = Offset.Zero
                            floatingDragOffset = Offset.Zero
                            isFloatingDragged = false
                        },
                        onDragEnd = {
                            if (isDragging) {
                                val targetIndex = dragPosition.roundToInt()
                                visualGroupIndex = targetIndex
                                isDragging = false
                                val targetScreen = groupedScreens[targetIndex]
                                if (selectedRoute != targetScreen.route) {
                                    selectScreen(targetScreen, false, true)
                                }
                            }
                            isFloatingDragged = false
                            floatingDragOffset = Offset.Zero
                        },
                        onDragCancel = {
                            isDragging = false
                            visualGroupIndex = selectedGroupIndex
                            isFloatingDragged = false
                            floatingDragOffset = Offset.Zero
                        },
                        onDrag = { change, amount ->
                            travel += amount
                            if (horizontalNavigation == null) {
                                horizontalNavigation = startedOnGroup && abs(travel.x) > abs(travel.y)
                                isDragging = horizontalNavigation == true
                                if (isDragging) dragPosition = rawDragPosition
                            }
                            if (isDragging) {
                                rawDragPosition += amount.x / segmentWidthPx
                                dragPosition = rawDragPosition
                                    .coerceIn(0f, groupedScreens.lastIndex.toFloat())
                            }
                            if (!reducedMotion) {
                                // Horizontal navigation only pulls the pill beyond the first/last tab.
                                val resistedTravel = if (isDragging) {
                                    Offset((rawDragPosition - dragPosition) * segmentWidthPx, 0f)
                                } else {
                                    travel
                                }
                                val distance = resistedTravel.getDistance()
                                isFloatingDragged = distance > activationThresholdPx
                                floatingDragOffset = if (isFloatingDragged) {
                                    val pull = resistedTravel *
                                        ((distance - activationThresholdPx) / (distance * resistancePx))
                                    pull / sqrt(1f + pull.getDistanceSquared())
                                } else {
                                    Offset.Zero
                                }
                            }
                            change.consume()
                        },
                    )
                } finally {
                    isDragging = false
                    isFloatingDragged = false
                    floatingDragOffset = Offset.Zero
                }
            },
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .weight(1f)
                .height(IrideFolio.NavigationTouchTargetSize),
        ) {
            Box(
                Modifier.align(Alignment.Center)
                    .fillMaxWidth()
                    .height(FloatingBottomNavigationPillHeight)
                    .frostedNavigationSurface(
                        backdrop, surface, groupShape,
                        surfaceAlpha = navigationSurfaceAlpha,
                        observeBackdrop = true,
                    )
                    .border(1.dp, borderColor, groupShape),
            )
            val segmentWidth = maxWidth / groupedScreens.size
            val segmentWidthPx = with(LocalDensity.current) { segmentWidth.toPx() }
            val indicatorInsetPx = with(LocalDensity.current) { IrideFolio.NavigationSelectionInset.toPx() }
            val indicatorHeightPx = with(LocalDensity.current) {
                (FloatingBottomNavigationPillHeight - IrideFolio.NavigationSelectionInset * 2).toPx()
            }
            val indicatorWidthPx = segmentWidthPx - 2f * indicatorInsetPx
            Canvas(Modifier.fillMaxSize()) {
                val left = segmentWidthPx * animatedPosition + indicatorInsetPx
                val top = (size.height - indicatorHeightPx) / 2f
                drawRoundRect(
                    color = onSurface.copy(alpha = 0.10f),
                    topLeft = Offset(left, top),
                    size = Size(indicatorWidthPx, indicatorHeightPx),
                    cornerRadius = CornerRadius(
                        IrideFolio.NavigationSelectionCornerRadius.toPx(),
                    ),
                    alpha = indicatorAlpha,
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxSize(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                groupedScreens.forEachIndexed { index, screen ->
                    val indicatorStart = segmentWidthPx * animatedPosition + indicatorInsetPx
                    val buttonLeft = segmentWidthPx * index
                    val maskStart = (indicatorStart - buttonLeft).coerceIn(0f, segmentWidthPx)
                    val maskEnd = (indicatorStart + indicatorWidthPx - buttonLeft).coerceIn(0f, segmentWidthPx)
                    // A tap across tabs must not activate the icons it only passes over.
                    val showActiveContent = selectedRoute != Screens.Search.route && index !in skippedGroupIndices
                    NavigationButton(
                        screen = screen,
                        selected = selectedRoute == screen.route,
                        maskStart = if (showActiveContent) maskStart else 0f,
                        maskEnd = if (showActiveContent) maskEnd else 0f,
                        onItemClick = { screen, selected -> selectScreen(screen, selected, false) },
                        reducedMotion = reducedMotion,
                        contentColor = onSurface,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
        Spacer(Modifier.width(IrideFolio.NavigationGap))
        val search = Screens.Search
        Box(
            modifier = Modifier.size(IrideFolio.NavigationTouchTargetSize),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                Modifier.size(FloatingBottomNavigationPillHeight)
                    .frostedNavigationSurface(
                        backdrop, surface, groupShape,
                        surfaceAlpha = navigationSurfaceAlpha,
                        observeBackdrop = true,
                    )
                    .border(1.dp, borderColor, groupShape),
            )
            NavigationButton(
                screen = search,
                selected = selectedRoute == search.route,
                onItemClick = { screen, selected -> selectScreen(screen, selected, false) },
                reducedMotion = reducedMotion,
                contentColor = onSurface,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Composable
private fun NavigationButton(
    screen: Screens,
    selected: Boolean,
    onItemClick: (Screens, Boolean) -> Unit,
    reducedMotion: Boolean,
    contentColor: Color,
    maskStart: Float = 0f,
    maskEnd: Float = 0f,
    modifier: Modifier = Modifier,
) {
    val currentSelected by rememberUpdatedState(selected)
    val description = stringResource(screen.titleId)
    val searchSelection = animateFloatAsState(
        targetValue = if (screen == Screens.Search && selected) 1f else 0f,
        animationSpec = if (reducedMotion) snap() else tween(
            IrideFolio.NavigationSelectionDurationMillis,
            easing = IrideFolio.NavigationMotionEasing,
        ),
        label = "bottomNavigationSearchSelection",
    )
    Box(
        modifier = modifier
            .fillMaxHeight()
            .semantics { contentDescription = description }
            .selectable(
                selected = selected,
                role = Role.Tab,
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = { onItemClick(screen, currentSelected) },
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (screen == Screens.Search) {
            Box(
                modifier = Modifier
                    .size(FloatingBottomNavigationPillHeight - IrideFolio.NavigationSelectionInset * 2)
                    .drawBehind {
                        drawRoundRect(
                            color = contentColor,
                            alpha = searchSelection.value * 0.10f,
                            cornerRadius = CornerRadius(
                                IrideFolio.NavigationSelectionCornerRadius.toPx(),
                            ),
                        )
                    },
            )
        }
        if (screen == Screens.Search) {
            Icon(
                painter = painterResource(if (selected) screen.iconIdActive else screen.iconIdInactive),
                contentDescription = null,
                tint = contentColor.copy(alpha = contentColor.alpha * (0.72f + 0.28f * searchSelection.value)),
                modifier = Modifier.size(23.dp),
            )
        } else {
            NavigationButtonContent(
                screen = screen,
                active = false,
                color = contentColor.copy(alpha = contentColor.alpha * 0.72f),
                modifier = Modifier.drawWithContent {
                    if (maskEnd > maskStart) {
                        if (maskStart > 0f) clipRect(right = maskStart) { this@drawWithContent.drawContent() }
                        if (maskEnd < size.width) clipRect(left = maskEnd) { this@drawWithContent.drawContent() }
                    } else {
                        drawContent()
                    }
                },
            )
            NavigationButtonContent(
                screen = screen,
                active = true,
                color = contentColor,
                modifier = Modifier.drawWithContent {
                    if (maskEnd > maskStart) {
                        clipRect(left = maskStart, right = maskEnd) { this@drawWithContent.drawContent() }
                    }
                },
            )
        }
    }
}

@Composable
private fun NavigationButtonContent(
    screen: Screens,
    active: Boolean,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterVertically),
        modifier = modifier.fillMaxSize().padding(horizontal = 4.dp).clearAndSetSemantics { },
    ) {
        Icon(
            painter = painterResource(if (active) screen.iconIdActive else screen.iconIdInactive),
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(23.dp),
        )
        Text(
            text = stringResource(screen.titleId),
            fontFamily = SpaceMonoFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 9.sp,
            lineHeight = 12.sp,
            letterSpacing = (-0.2).sp,
            color = color,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

internal fun Modifier.frostedNavigationSurface(
    backdrop: FrostBackdrop?,
    surface: Color,
    shape: androidx.compose.ui.graphics.Shape,
    progress: Float = 1f,
    surfaceAlpha: Float? = null,
    observeBackdrop: Boolean = false,
    backdropOpacity: Float = 1f,
    opaqueBackground: Boolean = true,
): Modifier = composed {
    var position by remember { mutableStateOf(Offset.Zero) }
    val revealPaint = remember { Paint() }
    val density = LocalDensity.current.density
    val grain = remember(density) { irideGrainBrush(density) }
    this
        .onGloballyPositioned { position = it.positionOnScreen() }
        .clip(shape)
        // The observer must share the blur's clipped layer. Invalidating a node outside this
        // clip only replays the inner cached layer, which can still contain the unblurred fallback.
        .then(if (observeBackdrop) Modifier.observeFrostBackdrop(backdrop) else Modifier)
        .drawBehind {
            val reveal = progress.coerceIn(0f, 1f)
            if (reveal <= 0f) return@drawBehind
            val backdropReady = backdrop != null && backdrop.content.size.width > 0 &&
                backdrop.content.size.height > 0
            if (backdropReady) {
                // Fade each control locally; mutating the shared layer's alpha also changes menus
                // and other controls that reference it in the same frame.
                val sampleOpacity = backdropOpacity.coerceIn(0f, 1f)
                val sampleAlpha = reveal * sampleOpacity
                if (sampleAlpha < 1f) {
                    revealPaint.alpha = sampleAlpha
                    drawContext.canvas.saveLayer(Rect(Offset.Zero, size), revealPaint)
                }
                // Opaque panels replace the page; glass blends the blur over the visible content.
                if (opaqueBackground && sampleOpacity == 1f) drawRect(surface.copy(alpha = 1f))
                drawFrostedBackdrop(backdrop, position)
                if (sampleAlpha < 1f) drawContext.canvas.restore()
            }
            drawRect(
                surface,
                alpha = reveal * (surfaceAlpha ?: if (backdropReady) {
                    IrideFolio.FrostedSurfaceOpacity
                } else {
                    IrideFolio.FrostedFallbackOpacity
                }),
            )
            // Blur replaces the sharp backdrop grain. Restore it only on a ready blurred surface;
            // the unblurred fallback already inherits the background's texture.
            if (backdropReady) {
                // Sample in the same coordinates as the backdrop, not the moving control's local
                // origin. Scrolling/dragging reveals another part of the field beneath the clip.
                val sampleOffset = position - backdrop.positionOnScreen
                drawIrideSurfaceGrain(grain, reveal * IrideFolio.SurfaceGrainOpacity, sampleOffset)
            }
        }
}
