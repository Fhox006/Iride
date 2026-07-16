/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import com.metrolist.music.ui.theme.SpaceMonoFontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.metrolist.music.ui.screens.Screens
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@Immutable
private data class NavItemState(
    val isSelected: Boolean,
    val iconRes: Int
)

@Stable
internal fun isRouteSelected(currentRoute: String?, screenRoute: String, navigationItems: List<Screens>): Boolean {
    if (currentRoute == null) return false
    if (currentRoute == screenRoute) return true
    if (navigationItems.any { it.route == screenRoute } &&
        currentRoute.startsWith("$screenRoute/")) return true

    // Fix: match the route template, not the resolved route
    if (screenRoute == "search_input" &&
        (currentRoute.startsWith("search/") || currentRoute == "search/{query}")) return true

    return false
}

@Composable
fun AppNavigationRail(
    navigationItems: List<Screens>,
    currentRoute: String?,
    onItemClick: (Screens, Boolean) -> Unit,
    modifier: Modifier = Modifier,
    pureBlack: Boolean = false,
    onSearchLongClick: (() -> Unit)? = null,
    accountImageUrl: String? = null
) {
    val containerColor = if (pureBlack) Color.Black else MaterialTheme.colorScheme.surfaceContainer
    val haptics = LocalHapticFeedback.current
    val viewConfiguration = LocalViewConfiguration.current

    NavigationRail(
        modifier = modifier,
        containerColor = containerColor
    ) {
        Spacer(modifier = Modifier.weight(1f))

        navigationItems.forEach { screen ->
            val isSelected = remember(currentRoute, screen.route) {
                isRouteSelected(currentRoute, screen.route, navigationItems)
            }
            val currentIsSelected by rememberUpdatedState(isSelected)
            val iconRes = remember(isSelected, screen) {
                if (isSelected) screen.iconIdActive else screen.iconIdInactive
            }

            val isSearchItem = screen == Screens.Search && onSearchLongClick != null
            val interactionSource = remember { MutableInteractionSource() }
            val scope = rememberCoroutineScope()

            var pressed by remember { mutableStateOf(false) }
            val iconScale by animateFloatAsState(
                targetValue = if (pressed) 0.75f else 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                ),
                label = "iconScale"
            )

            // Long press detection using InteractionSource
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
                                onSearchLongClick.invoke()
                            }
                            is PressInteraction.Release -> {
                                if (!isLongClick) {
                                    scope.launch {
                                        pressed = true
                                        delay(50)
                                        pressed = false
                                    }
                                    onItemClick(screen, currentIsSelected)
                                }
                            }
                            is PressInteraction.Cancel -> {
                                isLongClick = false
                            }
                        }
                    }
                }
            }

            NavigationRailItem(
                selected = isSelected,
                onClick = {
                    scope.launch {
                        pressed = true
                        delay(50)
                        pressed = false
                    }
                    if (!isSearchItem) {
                        onItemClick(screen, currentIsSelected)
                    }
                    // For search item, click is handled via InteractionSource
                },
                interactionSource = interactionSource,
                icon = {
                    Box(
                        modifier = Modifier.graphicsLayer {
                            scaleX = iconScale
                            scaleY = iconScale
                        }
                    ) {
                        if (screen == Screens.Account && accountImageUrl != null) {
                            AsyncImage(
                                model = accountImageUrl,
                                contentDescription = stringResource(screen.titleId),
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                            )
                        } else {
                            Icon(
                                painter = painterResource(id = iconRes),
                                contentDescription = stringResource(screen.titleId)
                            )
                        }
                    }
                }
            )
        }

        Spacer(modifier = Modifier.weight(1f))
    }
}

@Composable
fun AppNavigationBar(
    navigationItems: List<Screens>,
    currentRoute: String?,
    onItemClick: (Screens, Boolean) -> Unit,
    modifier: Modifier = Modifier,
    pureBlack: Boolean = false,
    slimNav: Boolean = false,
    onSearchLongClick: (() -> Unit)? = null,
    accountImageUrl: String? = null
) {
    val containerColor = if (pureBlack) Color.Black else MaterialTheme.colorScheme.surfaceContainer
    val contentColor = if (pureBlack) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
    val haptics = LocalHapticFeedback.current
    val viewConfiguration = LocalViewConfiguration.current

    NavigationBar(
        modifier = modifier,
        containerColor = containerColor,
        contentColor = contentColor
    ) {
        navigationItems.forEach { screen ->
            val isSelected = remember(currentRoute, screen.route) {
                isRouteSelected(currentRoute, screen.route, navigationItems)
            }
            val currentIsSelected by rememberUpdatedState(isSelected)
            val iconRes = remember(isSelected, screen) {
                if (isSelected) screen.iconIdActive else screen.iconIdInactive
            }

            val isSearchItem = screen == Screens.Search && onSearchLongClick != null
            val interactionSource = remember { MutableInteractionSource() }
            val scope = rememberCoroutineScope()

            var pressed by remember { mutableStateOf(false) }
            val iconScale by animateFloatAsState(
                targetValue = if (pressed) 0.75f else 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                ),
                label = "iconScale"
            )

            // Long press detection using InteractionSource
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
                                onSearchLongClick.invoke()
                            }
                            is PressInteraction.Release -> {
                                if (!isLongClick) {
                                    scope.launch {
                                        pressed = true
                                        delay(50)
                                        pressed = false
                                    }
                                    onItemClick(screen, currentIsSelected)
                                }
                            }
                            is PressInteraction.Cancel -> {
                                isLongClick = false
                            }
                        }
                    }
                }
            }

            NavigationBarItem(
                selected = isSelected,
                onClick = {
                    scope.launch {
                        pressed = true
                        delay(50)
                        pressed = false
                    }
                    if (!isSearchItem) {
                        onItemClick(screen, currentIsSelected)
                    }
                    // For search item, click is handled via InteractionSource
                },
                interactionSource = interactionSource,
                icon = {
                    Box(
                        modifier = Modifier.graphicsLayer {
                            scaleX = iconScale
                            scaleY = iconScale
                        }
                    ) {
                        if (screen == Screens.Account && accountImageUrl != null) {
                            AsyncImage(
                                model = accountImageUrl,
                                contentDescription = stringResource(screen.titleId),
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                            )
                        } else {
                            Icon(
                                painter = painterResource(id = iconRes),
                                contentDescription = stringResource(screen.titleId)
                            )
                        }
                    }
                },
                label = if (!slimNav) {
                    {
                        Text(
                            text = stringResource(screen.titleId),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                } else null
            )
        }
    }
}

@Composable
fun TopNavigationBar(
    navigationItems: List<Screens>,
    currentRoute: String?,
    onItemClick: (Screens, Boolean) -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color = Color.Black,
    // Callers whose own scrollable container already reserves horizontal inset (e.g. Library's
    // LazyColumn contentPadding) must pass 0.dp here — otherwise this bar's own inset stacks on
    // top of the container's, pushing it further right than every other item in that list.
    horizontalPadding: Dp = 20.dp,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(containerColor)
            .statusBarsPadding()
            .padding(top = 10.dp)
            .height(64.dp)
            .horizontalScroll(rememberScrollState())
            .padding(start = horizontalPadding, end = horizontalPadding),
        verticalAlignment = Alignment.CenterVertically
    ) {
        navigationItems.forEach { screen ->
            val isSelected = remember(currentRoute, screen.route) {
                isRouteSelected(currentRoute, screen.route, navigationItems)
            }
            val currentIsSelected by rememberUpdatedState(isSelected)

            // Soft fade on the title text color when its tab becomes selected, so the switch
            // reads as a gentle settle instead of an instant color snap.
            val titleColor by animateColorAsState(
                targetValue = if (isSelected) Color.White else Color.White.copy(alpha = 0.55f),
                animationSpec = tween(220),
                label = "titleColor"
            )

            Text(
                text = stringResource(screen.titleId),
                style = TextStyle(
                    fontFamily = SpaceMonoFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    letterSpacing = (-0.1).sp,
                    color = titleColor
                ),
                maxLines = 1,
                softWrap = false,
                modifier = Modifier
                    .padding(end = 20.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        onItemClick(screen, currentIsSelected)
                    }
            )
        }
    }
}
