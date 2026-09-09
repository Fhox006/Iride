/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.component

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.SoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp as lerpFloat
import com.metrolist.music.ui.theme.SpaceMonoFontFamily
import com.metrolist.music.utils.rememberPreference

/**
 * Reusable collapsing large-title header for library sub-screens.
 *
 * Replicates the expressive header of LibraryMixScreen:
 * - Large bold title at the bottom of the expanded area
 * - Animates upward and shrinks to ~0.61x scale as the user scrolls
 * - Optional [trailingContent] slot for pills, toggles, or action buttons
 *   shown only in the expanded state (fades out as the header collapses)
 * - Optional search overlay via [LibrarySearchHeader]
 *
 * Usage:
 * ```
 * val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(
 *     snapAnimationSpec = tween(durationMillis = 200)
 * )
 * Scaffold(
 *     modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
 *     topBar = {
 *         CollapsingScreenHeader(
 *             title = stringResource(R.string.albums),
 *             scrollBehavior = scrollBehavior,
 *             pureBlack = pureBlack,
 *             isSearchActive = isSearchActive,
 *             onSearchActiveChange = { isSearchActive = it },
 *             searchQuery = searchQuery,
 *             onSearchQueryChange = viewModel::updateSearchQuery,
 *             keyboardController = keyboardController,
 *         )
 *     },
 *     containerColor = Color.Transparent,
 *     contentWindowInsets = WindowInsets(0),
 * ) { paddingValues -> ... }
 * ```
 *
 * @param title          The large title string to display.
 * @param scrollBehavior Must be [TopAppBarDefaults.exitUntilCollapsedScrollBehavior].
 * @param pureBlack      When true, uses pure black background (AMOLED mode).
 * @param isSearchActive When true, the [LibrarySearchHeader] overlay is shown.
 * @param onSearchActiveChange Callback to toggle search mode.
 * @param searchQuery    Current search text.
 * @param onSearchQueryChange Callback for search text changes.
 * @param keyboardController Used to show/hide the software keyboard.
 * @param trailingContent Optional composable slot placed at the trailing end of the title row.
 *                        Only visible when the header is in its expanded state (fraction < 0.05).
 *                        Fades out during collapse. Use for filter pills, toggle buttons, etc.
 */

val CollapsingHeaderLargeTitleHeight = 80.dp

val CollapsingHeaderSmallBarHeight = 56.dp

val CollapsingHeaderCompactBarHeight = 40.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollapsingScreenHeader(
    title: String,
    scrollBehavior: TopAppBarScrollBehavior,
    pureBlack: Boolean,
    isSearchActive: Boolean,
    onSearchActiveChange: (Boolean) -> Unit,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    keyboardController: SoftwareKeyboardController?,
    navigationIcon: (@Composable () -> Unit)? = null,
    trailingContent: (@Composable () -> Unit)? = null,
    transparentBackground: Boolean = false,
    hideTitle: Boolean = false,
) {
    val density = LocalDensity.current
    val largeTitleHeightPx = if (hideTitle) 0f else with(density) { CollapsingHeaderLargeTitleHeight.toPx() }

    SideEffect {
        if (scrollBehavior.state.heightOffsetLimit != -largeTitleHeightPx) {
            scrollBehavior.state.heightOffsetLimit = -largeTitleHeightPx
        }
    }

    val fraction = if (hideTitle) 0f else scrollBehavior.state.collapsedFraction
    val barHeight = if (hideTitle) CollapsingHeaderCompactBarHeight else CollapsingHeaderSmallBarHeight
    val totalHeightDp = barHeight + (if (hideTitle) 0.dp else CollapsingHeaderLargeTitleHeight)

    Surface(
        color = when {
            transparentBackground -> Color.Transparent
            pureBlack -> Color.Black
            else -> MaterialTheme.colorScheme.background
        },
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.statusBars)
            .height(totalHeightDp + with(density) { scrollBehavior.state.heightOffset.toDp() }),
    ) {
        Box {
            if (navigationIcon != null) {
                Box(
                    modifier = Modifier
                        .width(56.dp)
                        .height(barHeight)
                        .padding(start = 4.dp)
                        .graphicsLayer {
                            translationY = if (hideTitle) 0f else lerpFloat(
                                with(density) { (CollapsingHeaderLargeTitleHeight - 12.dp).toPx() },
                                0f,
                                fraction,
                            )
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    navigationIcon()
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(CollapsingHeaderSmallBarHeight)
                    .padding(start = if (navigationIcon != null) 52.dp else 12.dp, end = 12.dp)
                    .graphicsLayer {
                        translationY = if (hideTitle) 0f else lerpFloat(
                            with(density) { (CollapsingHeaderLargeTitleHeight - 12.dp).toPx() },
                            0f,
                            fraction,
                        )
                    },
                contentAlignment = if (hideTitle) Alignment.CenterEnd else Alignment.CenterStart,
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Bottom,
                ) {
                    if (!hideTitle) {
                        Text(
                            text = title,
                            style = TextStyle(
                                fontFamily = SpaceMonoFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 28.sp,
                                letterSpacing = (-0.5).sp,
                            ),
                            maxLines = 1,
                            modifier = Modifier
                                .weight(1f, fill = false)
                                .graphicsLayer {
                                    val targetScale = 0.61f
                                    val scale = lerpFloat(1f, targetScale, fraction)
                                    scaleX = scale
                                    scaleY = scale
                                    transformOrigin = TransformOrigin(0f, 0.5f)
                                    alpha = lerpFloat(1f, 0.95f, fraction)
                                },
                        )
                        androidx.compose.foundation.layout.Spacer(modifier = Modifier.weight(1f))
                    }

                    if (trailingContent != null) {
                        Box(
                            modifier = Modifier
                                .graphicsLayer {
                                    alpha = if (hideTitle) 1f else (1f - fraction * 2f).coerceIn(0f, 1f)
                                }
                                .padding(bottom = 4.dp),
                        ) {
                            trailingContent()
                        }
                    }
                }
            }

            LibrarySearchHeader(
                isSearchActive = isSearchActive,
                searchQuery = searchQuery,
                onSearchQueryChange = onSearchQueryChange,
                onBack = {
                    onSearchActiveChange(false)
                    onSearchQueryChange("")
                },
                keyboardController = keyboardController,
                modifier = Modifier,
            ) {}
        }
    }
}
