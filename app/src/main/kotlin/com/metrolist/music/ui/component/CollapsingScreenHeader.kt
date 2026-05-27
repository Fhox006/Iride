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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp as lerpFloat

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

// Height of the large expanded area (below the small toolbar row)
val CollapsingHeaderLargeTitleHeight = 80.dp

// Height of the small collapsed toolbar row
val CollapsingHeaderSmallBarHeight = 56.dp

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
    trailingContent: (@Composable () -> Unit)? = null,
) {
    val density = LocalDensity.current
    val largeTitleHeightPx = with(density) { CollapsingHeaderLargeTitleHeight.toPx() }

    // Tell the scroll behavior how much height it can collapse
    SideEffect {
        if (scrollBehavior.state.heightOffsetLimit != -largeTitleHeightPx) {
            scrollBehavior.state.heightOffsetLimit = -largeTitleHeightPx
        }
    }

    // fraction: 0f = fully expanded, 1f = fully collapsed
    val fraction = scrollBehavior.state.collapsedFraction
    val totalHeightDp = CollapsingHeaderSmallBarHeight + CollapsingHeaderLargeTitleHeight

    Surface(
        color = if (pureBlack) Color.Black else MaterialTheme.colorScheme.background,
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.statusBars)
            .height(totalHeightDp + with(density) { scrollBehavior.state.heightOffset.toDp() }),
    ) {
        Box {
            // Title row — translates upward from the large position to the small bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(CollapsingHeaderSmallBarHeight)
                    .padding(start = 12.dp, end = 12.dp)
                    .graphicsLayer {
                        translationY = lerpFloat(
                            with(density) { (CollapsingHeaderLargeTitleHeight - 12.dp).toPx() },
                            0f,
                            fraction,
                        )
                    },
                contentAlignment = Alignment.CenterStart,
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Bottom,
                ) {
                    // Large title — scales down as the header collapses
                    Text(
                        text = title,
                        style = MaterialTheme.typography.displaySmall.copy(
                            fontWeight = FontWeight.Bold,
                        ),
                        maxLines = 1,
                        modifier = Modifier
                            .weight(1f)
                            .graphicsLayer {
                                val targetScale = 0.61f
                                val scale = lerpFloat(1f, targetScale, fraction)
                                scaleX = scale
                                scaleY = scale
                                transformOrigin = TransformOrigin(0f, 0.5f)
                                alpha = lerpFloat(1f, 0.95f, fraction)
                            },
                    )

                    // Optional trailing slot — fades out during collapse
                    if (trailingContent != null) {
                        Box(
                            modifier = Modifier
                                .graphicsLayer {
                                    // Fade out in the first half of the collapse
                                    alpha = (1f - fraction * 2f).coerceIn(0f, 1f)
                                }
                                .padding(bottom = 4.dp),
                        ) {
                            trailingContent()
                        }
                    }
                }
            }

            // Search overlay — covers the entire header when active
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
