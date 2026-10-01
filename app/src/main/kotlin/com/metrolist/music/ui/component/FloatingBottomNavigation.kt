package com.metrolist.music.ui.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateValueAsState
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.withFrameNanos
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionOnScreen
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.metrolist.music.ui.screens.Screens
import com.metrolist.music.ui.theme.SpaceMonoFontFamily
import com.metrolist.music.ui.theme.strokeCard
import com.metrolist.music.ui.utils.rememberReducedMotion
import kotlin.math.roundToInt
import kotlin.math.abs
import kotlin.math.sqrt
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

val FloatingBottomNavigationHeight = 62.dp
internal val FloatingBottomNavigationPillHeight = 54.dp
internal val FloatingBottomNavigationOffset = 12.dp

@Composable
fun FloatingBottomNavigation(
    navigationItems: List<Screens>,
    currentRoute: String?,
    onItemClick: (Screens, Boolean) -> Unit,
    backdrop: FrostBackdrop?,
    visible: Boolean = true,
    modifier: Modifier = Modifier,
) {
    val surface = MaterialTheme.colorScheme.surface
    val onSurface = MaterialTheme.colorScheme.onSurface
    val groupShape = RoundedCornerShape(18.dp)
    val borderColor = MaterialTheme.colorScheme.strokeCard
    val groupedScreens = listOf(Screens.Home, Screens.Library, Screens.Account)
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
    val reducedMotion = rememberReducedMotion()
    val coroutineScope = rememberCoroutineScope()
    val currentOnItemClick by rememberUpdatedState(onItemClick)
    var navigationJob by remember { mutableStateOf<Job?>(null) }
    val selectScreen: (Screens, Boolean) -> Unit = { screen, selected ->
        navigationJob?.cancel()
        groupedScreens.indexOf(screen).takeIf { it >= 0 }?.let { visualGroupIndex = it }
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
    )
    val indicatorAlpha by animateFloatAsState(
        targetValue = if (selectedRoute == Screens.Search.route) 0f else 1f,
        animationSpec = tween(180),
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
    Row(
        modifier = modifier.widthIn(max = 320.dp).fillMaxWidth()
            .graphicsLayer {
                val offset = floatingOffset.value
                translationX = offset.x * 6.dp.toPx()
                translationY = offset.y * 6.dp.toPx()
            }
            .pointerInput(visible, selectedRoute, reducedMotion) {
                if (!visible) return@pointerInput
                val groupWidthPx = size.width - (FloatingBottomNavigationPillHeight + 8.dp).toPx()
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
                                    selectScreen(targetScreen, false)
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
                .height(FloatingBottomNavigationPillHeight)
                .frostedNavigationSurface(backdrop, surface, groupShape)
                .border(1.dp, borderColor, groupShape),
        ) {
            val segmentWidth = maxWidth / groupedScreens.size
            val segmentWidthPx = with(LocalDensity.current) { segmentWidth.toPx() }
            val indicatorInsetPx = with(LocalDensity.current) { 4.dp.toPx() }
            val indicatorHeightPx = with(LocalDensity.current) { 46.dp.toPx() }
            val indicatorWidthPx = segmentWidthPx - 2f * indicatorInsetPx
            Canvas(Modifier.fillMaxSize()) {
                val left = segmentWidthPx * animatedPosition + indicatorInsetPx
                val top = (size.height - indicatorHeightPx) / 2f
                drawRoundRect(
                    color = onSurface.copy(alpha = 0.10f),
                    topLeft = Offset(left, top),
                    size = Size(indicatorWidthPx, indicatorHeightPx),
                    cornerRadius = CornerRadius(14.dp.toPx()),
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
                    NavigationButton(
                        screen = screen,
                        selected = selectedRoute == screen.route,
                        maskStart = if (selectedRoute == Screens.Search.route) 0f else maskStart,
                        maskEnd = if (selectedRoute == Screens.Search.route) 0f else maskEnd,
                        onItemClick = selectScreen,
                        enabled = visible,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
        Spacer(Modifier.width(8.dp))
        val search = Screens.Search
        Box(
            modifier = Modifier
                .size(FloatingBottomNavigationPillHeight)
                .frostedNavigationSurface(backdrop, surface, groupShape)
                .border(1.dp, borderColor, groupShape),
            contentAlignment = Alignment.Center,
        ) {
            NavigationButton(
                screen = search,
                selected = selectedRoute == search.route,
                onItemClick = selectScreen,
                enabled = visible,
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
    enabled: Boolean,
    maskStart: Float = 0f,
    maskEnd: Float = 0f,
    modifier: Modifier = Modifier,
) {
    val currentSelected by rememberUpdatedState(selected)
    val onSurface = MaterialTheme.colorScheme.onSurface
    val searchColor by animateColorAsState(
        if (selected) onSurface else onSurface.copy(alpha = 0.72f),
        label = "bottomNavigationSearchIcon",
    )
    val searchBackground by animateColorAsState(
        if (screen == Screens.Search && selected) onSurface.copy(alpha = 0.10f) else Color.Transparent,
        label = "bottomNavigationSearchSelection",
    )
    Box(
        modifier = modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(14.dp))
            .semantics { this.selected = selected }
            .clickable(enabled = enabled, role = Role.Tab, interactionSource = remember { MutableInteractionSource() }, indication = null) {
                onItemClick(screen, currentSelected)
            },
        contentAlignment = Alignment.Center,
    ) {
        if (screen == Screens.Search) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(4.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(searchBackground),
            )
            Icon(
                painter = painterResource(if (selected) screen.iconIdActive else screen.iconIdInactive),
                contentDescription = stringResource(screen.titleId),
                tint = searchColor,
                modifier = Modifier.size(23.dp),
            )
        } else {
            NavigationButtonContent(screen, active = false, color = onSurface.copy(alpha = 0.72f))
            NavigationButtonContent(
                screen,
                active = true,
                color = onSurface,
                modifier = Modifier
                    .drawWithContent {
                        if (maskEnd > maskStart) {
                            clipRect(left = maskStart, right = maskEnd) {
                                this@drawWithContent.drawContent()
                            }
                        }
                    }
                    .clearAndSetSemantics { },
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
        verticalArrangement = Arrangement.spacedBy(2.dp),
        modifier = modifier.fillMaxWidth().padding(horizontal = 4.dp),
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
): Modifier {
    var position = Offset.Zero
    return this
        .onGloballyPositioned { position = it.positionOnScreen() }
        .clip(shape)
        .drawBehind {
            val reveal = progress.coerceIn(0f, 1f)
            if (reveal <= 0f) return@drawBehind
            // Cover the original sharp pixels before compositing a potentially transparent snapshot.
            drawRect(surface.copy(alpha = 1f), alpha = reveal)
            if (backdrop != null && backdrop.content.size.width > 0) {
                backdrop.blurred.alpha = reveal
                backdrop.blurred.record(size = backdrop.content.size) { drawLayer(backdrop.content) }
                translate(
                    backdrop.positionOnScreen.x - position.x,
                    backdrop.positionOnScreen.y - position.y,
                ) { drawLayer(backdrop.blurred) }
            }
            drawRect(surface, alpha = reveal * if (backdrop == null) 0.96f else 0.84f)
        }
}
