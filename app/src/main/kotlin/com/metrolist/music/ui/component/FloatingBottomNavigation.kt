package com.metrolist.music.ui.component

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionOnScreen
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.toSize
import com.metrolist.music.ui.screens.Screens
import com.metrolist.music.ui.theme.IBMPlexMonoFontFamily
import com.metrolist.music.ui.theme.FolioShape
import com.metrolist.music.ui.utils.rememberReducedMotion
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sqrt
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

val FloatingBottomNavigationHeight = IrideBottomDockTouchTargetSize + IrideMiniPlayerContentInset * 2
private val BottomDockSelectionInset = 2.dp

@Composable
fun FloatingBottomNavigation(
    navigationItems: List<Screens>,
    currentRoute: String?,
    onItemClick: (Screens, Boolean) -> Unit,
    visible: Boolean = true,
    modifier: Modifier = Modifier,
    contentColor: Color = Color.White,
) {
    if (navigationItems.isEmpty()) return
    val activeScreen = navigationItems.firstOrNull {
        isRouteSelected(currentRoute, it.route, navigationItems)
    }
    var rememberedScreenRoute by rememberSaveable { mutableStateOf(Screens.Home.route) }
    LaunchedEffect(activeScreen) {
        if (activeScreen != null) rememberedScreenRoute = activeScreen.route
    }
    val selectedRoute = activeScreen?.route ?: rememberedScreenRoute
    val selectedIndex = navigationItems.indexOfFirst { it.route == selectedRoute }.coerceAtLeast(0)
    var visualIndex by remember { mutableIntStateOf(selectedIndex) }
    var dragPosition by remember { mutableFloatStateOf(selectedIndex.toFloat()) }
    var isDragging by remember { mutableStateOf(false) }
    var skippedIndices by remember { mutableStateOf(IntRange.EMPTY) }
    val reducedMotion = rememberReducedMotion()
    val dockMotion = LocalIrideBottomDockMotion.current
    val layoutDirection = LocalLayoutDirection.current
    val isRtl = layoutDirection == LayoutDirection.Rtl
    val coroutineScope = rememberCoroutineScope()
    val currentOnItemClick by rememberUpdatedState(onItemClick)
    var navigationJob by remember { mutableStateOf<Job?>(null) }
    val selectScreen: (Screens, Boolean, Boolean) -> Unit = { screen, selected, fromDrag ->
        navigationJob?.cancel()
        val targetIndex = navigationItems.indexOf(screen)
        skippedIndices = if (!fromDrag && !selected) {
            (minOf(selectedIndex, targetIndex) + 1) until maxOf(selectedIndex, targetIndex)
        } else {
            IntRange.EMPTY
        }
        visualIndex = targetIndex
        navigationJob = coroutineScope.launch {
            if (!selected && !reducedMotion) {
                // Start the indicator before composing the destination.
                repeat(3) { withFrameNanos { } }
            }
            currentOnItemClick(screen, selected)
        }
    }
    LaunchedEffect(selectedIndex) {
        if (!isDragging) visualIndex = selectedIndex
    }
    LaunchedEffect(visible) {
        if (!visible) {
            navigationJob?.cancel()
            isDragging = false
            skippedIndices = IntRange.EMPTY
            visualIndex = selectedIndex
            dockMotion?.reset()
        }
    }
    val animatedPosition by animateFloatAsState(
        targetValue = if (isDragging) dragPosition else visualIndex.toFloat(),
        animationSpec = if (reducedMotion) snap() else spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow,
        ),
        label = "bottomNavigationIndicatorPosition",
        finishedListener = { skippedIndices = IntRange.EMPTY },
    )
    // Keep selection while hidden without leaving pointer targets above the expanded player.
    if (!visible) return
    var panelBounds by remember { mutableStateOf<Rect?>(null) }
    val cornerInfo = rememberDeviceCornerInfo()
    val panelShape = rememberDeviceMatchedBottomPanelShape(
        horizontalPadding = IrideFolio.HorizontalPadding,
        additionalBottomSpacing = 0.dp,
        topRadius = 0.dp,
        panelBoundsOnScreen = panelBounds,
        fallbackBottomRadius = IrideFolio.ControlsCornerRadius,
        cornerInfo = cornerInfo,
    )
    BoxWithConstraints(
        modifier = modifier
            .irideBottomDockBounds()
            .height(FloatingBottomNavigationHeight)
            .onGloballyPositioned {
                panelBounds = Rect(it.positionOnScreen(), it.size.toSize())
            }
            .clip(panelShape)
            .padding(IrideMiniPlayerContentInset)
            .selectableGroup()
            .pointerInput(selectedRoute, navigationItems, reducedMotion, dockMotion, layoutDirection) {
                val segmentWidthPx = size.width.toFloat() / navigationItems.size
                if (segmentWidthPx <= 0f) return@pointerInput
                val activationThresholdPx = 12.dp.toPx()
                val resistancePx = 120.dp.toPx()
                var horizontalNavigation: Boolean? = null
                var rawDragPosition = selectedIndex.toFloat()
                var travel = Offset.Zero
                try {
                    detectDragGestures(
                        onDragStart = {
                            navigationJob?.cancel()
                            skippedIndices = IntRange.EMPTY
                            dragPosition = selectedIndex.toFloat()
                            rawDragPosition = dragPosition
                            horizontalNavigation = null
                            travel = Offset.Zero
                            dockMotion?.reset()
                        },
                        onDragEnd = {
                            if (isDragging) {
                                val targetIndex = dragPosition.roundToInt()
                                visualIndex = targetIndex
                                isDragging = false
                                val targetScreen = navigationItems[targetIndex]
                                if (selectedRoute != targetScreen.route) selectScreen(targetScreen, false, true)
                            }
                            dockMotion?.reset()
                        },
                        onDragCancel = {
                            isDragging = false
                            visualIndex = selectedIndex
                            dockMotion?.reset()
                        },
                        onDrag = { change, amount ->
                            travel += amount
                            if (horizontalNavigation == null) {
                                horizontalNavigation = abs(travel.x) > abs(travel.y)
                                isDragging = horizontalNavigation == true
                            }
                            if (isDragging) {
                                rawDragPosition += (if (isRtl) -amount.x else amount.x) / segmentWidthPx
                                dragPosition = rawDragPosition.coerceIn(0f, navigationItems.lastIndex.toFloat())
                            }
                            if (!reducedMotion) {
                                // Tab selection follows horizontal travel; only excess travel pulls the box.
                                val resistedTravel = if (isDragging) {
                                    Offset((rawDragPosition - dragPosition) * segmentWidthPx, travel.y)
                                } else travel
                                val distance = resistedTravel.getDistance()
                                val pull = if (distance > activationThresholdPx) {
                                    resistedTravel * ((distance - activationThresholdPx) / (distance * resistancePx))
                                } else Offset.Zero
                                dockMotion?.updateDrag(pull / sqrt(1f + pull.getDistanceSquared()), true)
                            }
                            change.consume()
                        },
                    )
                } finally {
                    isDragging = false
                    dockMotion?.reset()
                }
            },
    ) {
        val contentWidthPx = with(LocalDensity.current) { maxWidth.roundToPx() }
        // Layout and drawing share integer edges so weighted rounding cannot shift the highlight.
        val tabEdges = remember(contentWidthPx, navigationItems.size) {
            IntArray(navigationItems.size + 1) { index ->
                (contentWidthPx.toLong() * index / navigationItems.size).toInt()
            }
        }
        val density = LocalDensity.current
        val indicatorInsetPx = with(density) { BottomDockSelectionInset.roundToPx().toFloat() }
        val contentInsetPx = with(density) { IrideMiniPlayerContentInset.roundToPx().toFloat() }
        val selectionRadius = IrideFolio.ControlsCornerRadius - IrideMiniPlayerContentInset - BottomDockSelectionInset
        val contentHeightPx = with(density) { maxHeight.roundToPx().toFloat() }
        val selectionClip = remember(panelShape, contentWidthPx, contentHeightPx, contentInsetPx, indicatorInsetPx, layoutDirection, density) {
            val outline = panelShape.createOutline(
                Size(contentWidthPx + contentInsetPx * 2, contentHeightPx + contentInsetPx * 2),
                layoutDirection,
                density,
            )
            val outlinePath = Path().apply {
                when (outline) {
                    is Outline.Generic -> addPath(outline.path)
                    is Outline.Rounded -> addRoundRect(outline.roundRect)
                    is Outline.Rectangle -> addRect(outline.rect)
                }
            }
            // Removing an inward half-stroke produces a parallel contour, including for the
            // display's noncircular corners. Reducing both ellipse radii would vary the gap.
            outlinePath.parallelInset(contentInsetPx + indicatorInsetPx).apply {
                translate(Offset(-contentInsetPx, -contentInsetPx))
            }
        }
        val position = animatedPosition.coerceIn(0f, navigationItems.lastIndex.toFloat())
        val fromIndex = position.toInt()
        val toIndex = (fromIndex + 1).coerceAtMost(navigationItems.lastIndex)
        val fraction = position - fromIndex
        val indicatorStart = tabEdges[fromIndex] +
            (tabEdges[toIndex] - tabEdges[fromIndex]) * fraction + indicatorInsetPx
        val indicatorEnd = tabEdges[fromIndex + 1] +
            (tabEdges[toIndex + 1] - tabEdges[fromIndex + 1]) * fraction - indicatorInsetPx
        val startCornerMatch = (1f - position).coerceIn(0f, 1f)
        val endCornerMatch = (position - (navigationItems.lastIndex - 1)).coerceIn(0f, 1f)
        val indicatorShape = FolioShape(
            topStart = selectionRadius,
            topEnd = selectionRadius,
            bottomStart = selectionRadius * (1f - startCornerMatch),
            bottomEnd = selectionRadius * (1f - endCornerMatch),
        )
        Canvas(Modifier.fillMaxSize()) {
            // The shared inset contour keeps a constant gap at either outer corner, including
            // during a tab slide. Inner corners retain the regular selection rounding.
            clipPath(selectionClip) {
                translate(if (isRtl) size.width - indicatorEnd else indicatorStart, indicatorInsetPx) {
                    drawOutline(
                        outline = indicatorShape.createOutline(
                            Size(indicatorEnd - indicatorStart, size.height - indicatorInsetPx * 2),
                            layoutDirection,
                            this,
                        ),
                        color = contentColor.copy(alpha = 0.10f),
                    )
                }
            }
        }
        Layout(
            modifier = Modifier.fillMaxSize(),
            content = {
                navigationItems.forEachIndexed { index, screen ->
                    val buttonLeft = tabEdges[index].toFloat()
                    val buttonWidth = (tabEdges[index + 1] - tabEdges[index]).toFloat()
                    val showActiveContent = index !in skippedIndices
                    val maskStart = (indicatorStart - buttonLeft).coerceIn(0f, buttonWidth)
                    val maskEnd = (indicatorEnd - buttonLeft).coerceIn(0f, buttonWidth)
                    NavigationButton(
                        screen = screen,
                        selected = selectedRoute == screen.route,
                        maskStart = if (showActiveContent) {
                            if (isRtl) buttonWidth - maskEnd else maskStart
                        } else 0f,
                        maskEnd = if (showActiveContent) {
                            if (isRtl) buttonWidth - maskStart else maskEnd
                        } else 0f,
                        onItemClick = { screen, selected -> selectScreen(screen, selected, false) },
                        contentColor = contentColor,
                    )
                }
            },
        ) { measurables, constraints ->
            val placeables = measurables.mapIndexed { index, measurable ->
                measurable.measure(
                    Constraints.fixed(tabEdges[index + 1] - tabEdges[index], constraints.maxHeight),
                )
            }
            layout(constraints.maxWidth, constraints.maxHeight) {
                placeables.forEachIndexed { index, placeable ->
                    placeable.placeRelative(tabEdges[index], 0)
                }
            }
        }
    }
}

@Composable
private fun NavigationButton(
    screen: Screens,
    selected: Boolean,
    onItemClick: (Screens, Boolean) -> Unit,
    contentColor: Color,
    maskStart: Float,
    maskEnd: Float,
    modifier: Modifier = Modifier,
) {
    val currentSelected by rememberUpdatedState(selected)
    val description = stringResource(screen.titleId)
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
            fontFamily = IBMPlexMonoFontFamily,
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
