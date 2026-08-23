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

// Height of the large expanded area (below the small toolbar row)
val CollapsingHeaderLargeTitleHeight = 80.dp

// Height of the small collapsed toolbar row
val CollapsingHeaderSmallBarHeight = 56.dp

// New Iride UI (hideTitle): compact bar height used when there's no title to make room for, just a
// small trailing toggle — tight enough to not leave a dead band under TopNavigationBar.
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
    // Optional "+N" pill shown right after the title (e.g. new songs from followed artists).
    titleBadge: Int? = null,
    // New Iride UI: the animated large-title area is redundant with the persistent
    // TopNavigationBar tabs bar above it, so it collapses away to just the small,
    // fixed toolbar row (trailingContent stays visible there, never fades).
    hideTitle: Boolean = false,
) {
    val density = LocalDensity.current
    // New Iride UI: pushed screens keep the collapsing large title, but it renders in the same
    // bold monospace type as TopNavigationBar/SettingsBackTopBar so every header reads as one system.
    val largeTitleHeightPx = if (hideTitle) 0f else with(density) { CollapsingHeaderLargeTitleHeight.toPx() }

    // Tell the scroll behavior how much height it can collapse
    SideEffect {
        if (scrollBehavior.state.heightOffsetLimit != -largeTitleHeightPx) {
            scrollBehavior.state.heightOffsetLimit = -largeTitleHeightPx
        }
    }

    // fraction: 0f = fully expanded, 1f = fully collapsed
    val fraction = if (hideTitle) 0f else scrollBehavior.state.collapsedFraction
    // New Iride UI (hideTitle): this bar carries nothing but the small trailing toggle — the large
    // title never shows and TopNavigationBar above already fills the "tab bar" role — so it uses a
    // tighter height than the classic small toolbar row instead of leaving the toggle floating in a
    // mostly-empty 56dp band right below TopNavigationBar.
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
            // Navigation icon — same translation as title so it animates together,
            // but in its own Box so it centers in the 56dp bar independent of text height.
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

            // Title row — translates upward from the large position to the small bar
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
                    // Large title — scales down as the header collapses. Hidden entirely in the
                    // New Iride UI, where the TopNavigationBar tabs bar already shows above.
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
                                // fill = false so a short title doesn't stretch the badge to the far
                                // edge — it sits right beside the title. The trailing Spacer below
                                // still pushes any trailingContent to the row's end.
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
                        if (titleBadge != null && titleBadge > 0) {
                            androidx.compose.foundation.layout.Spacer(modifier = Modifier.width(8.dp))
                            NewReleaseBadge(
                                count = titleBadge,
                                modifier = Modifier.graphicsLayer {
                                    // Fade out with the title as the header collapses.
                                    alpha = lerpFloat(1f, 0f, (fraction * 2f).coerceIn(0f, 1f))
                                },
                            )
                        }
                        androidx.compose.foundation.layout.Spacer(modifier = Modifier.weight(1f))
                    }

                    // Optional trailing slot — fades out during collapse, always fully visible
                    // (fixed position) when the title is hidden.
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
