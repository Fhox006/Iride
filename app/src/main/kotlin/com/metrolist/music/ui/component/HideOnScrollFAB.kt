/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.component

import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.metrolist.music.LocalPlayerAwareWindowInsets
import com.metrolist.music.R
import com.metrolist.music.ui.utils.isScrollingUp
import com.metrolist.music.ui.utils.pressScale
import com.metrolist.music.ui.theme.strokeCard

/**
 * New Iride UI: flat, monochrome, borderless-fill replacement for the default Material
 * [FloatingActionButton] — white-alpha ring on a near-black disc, no elevation/color, matching
 * the flat design language used elsewhere in New Iride UI (see [IrideSwitch], [GridMenuItem]).
 * Opt-in per call site via `useIrideStyle` so unrelated FAB usages (library/history/stats
 * screens) keep their normal Material styling.
 */
@Composable
private fun IrideFlatFAB(
    @DrawableRes icon: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        modifier = modifier
            .size(56.dp)
            .pressScale(interactionSource, pressedScale = 0.92f)
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = 0.85f))
            .border(1.dp, MaterialTheme.colorScheme.strokeCard, CircleShape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.9f),
        )
    }
}

@Composable
fun BoxScope.HideOnScrollFAB(
    visible: Boolean = true,
    lazyListState: LazyListState,
    @DrawableRes icon: Int,
    label: String? = null,
    onClick: () -> Unit,
    onRecognitionClick: (() -> Unit)? = null,
    useIrideStyle: Boolean = false,
) {
    AnimatedVisibility(
        visible = visible && lazyListState.isScrollingUp(),
        enter = slideInVertically { it },
        exit = slideOutVertically { it },
        modifier =
        Modifier
            .align(Alignment.BottomEnd)
            .windowInsetsPadding(
                LocalPlayerAwareWindowInsets.current
                    .only(WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal),
            ),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(16.dp)
        ) {
            if (onRecognitionClick != null) {
                SmallFloatingActionButton(
                    onClick = onRecognitionClick,
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.mic),
                        contentDescription = stringResource(R.string.recognize_music),
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
            }
            if (label != null) {
                ExtendedFloatingActionButton(
                    text = { Text(text = label) },
                    icon = {
                        Icon(
                            painter = painterResource(icon),
                            contentDescription = null,
                        )
                    },
                    onClick = onClick,
                    expanded = lazyListState.firstVisibleItemIndex == 0 && lazyListState.firstVisibleItemScrollOffset == 0,
                )
            } else if (useIrideStyle) {
                IrideFlatFAB(icon = icon, onClick = onClick)
            } else {
                FloatingActionButton(
                    onClick = onClick,
                ) {
                    Icon(
                        painter = painterResource(icon),
                        contentDescription = null,
                    )
                }
            }
        }
    }
}

@Composable
fun BoxScope.HideOnScrollFAB(
    visible: Boolean = true,
    lazyListState: LazyGridState,
    @DrawableRes icon: Int,
    label: String? = null,
    onClick: () -> Unit,
    onRecognitionClick: (() -> Unit)? = null,
) {
    AnimatedVisibility(
        visible = visible && lazyListState.isScrollingUp(),
        enter = slideInVertically { it },
        exit = slideOutVertically { it },
        modifier =
        Modifier
            .align(Alignment.BottomEnd)
            .windowInsetsPadding(
                LocalPlayerAwareWindowInsets.current
                    .only(WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal),
            ),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(16.dp)
        ) {
            if (onRecognitionClick != null) {
                SmallFloatingActionButton(
                    onClick = onRecognitionClick,
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.mic),
                        contentDescription = stringResource(R.string.recognize_music),
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
            }
            if (label != null) {
                ExtendedFloatingActionButton(
                    text = { Text(text = label) },
                    icon = {
                        Icon(
                            painter = painterResource(icon),
                            contentDescription = null,
                        )
                    },
                    onClick = onClick,
                    expanded = lazyListState.firstVisibleItemIndex == 0 && lazyListState.firstVisibleItemScrollOffset == 0,
                )
            } else {
                FloatingActionButton(
                    onClick = onClick,
                ) {
                    Icon(
                        painter = painterResource(icon),
                        contentDescription = null,
                    )
                }
            }
        }
    }
}

@Composable
fun BoxScope.HideOnScrollFAB(
    visible: Boolean = true,
    scrollState: ScrollState,
    @DrawableRes icon: Int,
    label: String? = null,
    onClick: () -> Unit,
    onRecognitionClick: (() -> Unit)? = null,
) {
    AnimatedVisibility(
        visible = visible && scrollState.isScrollingUp(),
        enter = slideInVertically { it },
        exit = slideOutVertically { it },
        modifier =
        Modifier
            .align(Alignment.BottomEnd)
            .windowInsetsPadding(
                LocalPlayerAwareWindowInsets.current
                    .only(WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal),
            ),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(16.dp)
        ) {
            if (onRecognitionClick != null) {
                SmallFloatingActionButton(
                    onClick = onRecognitionClick,
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.mic),
                        contentDescription = stringResource(R.string.recognize_music),
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
            }
            if (label != null) {
                ExtendedFloatingActionButton(
                    text = { Text(text = label) },
                    icon = {
                        Icon(
                            painter = painterResource(icon),
                            contentDescription = null,
                        )
                    },
                    onClick = onClick,
                    expanded = scrollState.value == 0,
                )
            } else {
                FloatingActionButton(
                    onClick = onClick,
                ) {
                    Icon(
                        painter = painterResource(icon),
                        contentDescription = null,
                    )
                }
            }
        }
    }
}
