/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.component

import androidx.compose.ui.graphics.SolidColor
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.Stable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.SoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.metrolist.music.R
import com.metrolist.music.ui.theme.SpaceMonoFontFamily
import com.metrolist.music.ui.utils.irideEnter

object LibraryPageDefaults {
    val SidePadding = 20.dp
    val HeroTopSpace = IridePageLayoutDefaults.BarHeight + IridePageLayoutDefaults.ContentTopSpacing
    val HeroToControlsGap = IridePageLayoutDefaults.TitleBottomSpacing
    val GridSpacing = 12.dp
    val TopBarHeight = 40.dp
}

@Stable
class LibraryPageRevealState {
    var titleTopPx by mutableFloatStateOf(Float.MAX_VALUE)
    var titleHeightPx by mutableFloatStateOf(0f)
    val progress: Float
        get() = iridePageTitleProgress(titleTopPx, titleHeightPx, topBarBottomPx, pastTitle = false)
    var topBarBottomPx by mutableFloatStateOf(0f)
}

@Composable
fun rememberLibraryPageRevealState(): LibraryPageRevealState {
    return remember { LibraryPageRevealState() }
}

@Composable
fun rememberLibraryTopBarProgress(
    state: LibraryPageRevealState,
    scrolledPastHeader: Boolean,
): Float {
    return if (scrolledPastHeader) 1f else state.progress
}

@Composable
fun LibraryHeroTitle(
    title: String,
    entranceAlpha: Float,
    revealState: LibraryPageRevealState,
    modifier: Modifier = Modifier,
    badge: (@Composable RowScope.() -> Unit)? = null,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .irideEnter(entranceAlpha, 10.dp),
    ) {
        Spacer(modifier = Modifier.height(LibraryPageDefaults.HeroTopSpace))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            IridePageTitle(
                title = title,
                color = Color.White,
                modifier = Modifier.weight(1f)
                    .onGloballyPositioned {
                        revealState.titleTopPx = it.positionInWindow().y
                        revealState.titleHeightPx = it.size.height.toFloat()
                    }
                    .drawWithContent {
                        clipRect(top = size.height * revealState.progress) {
                            this@drawWithContent.drawContent()
                        }
                    },
            )
            if (badge != null) {
                Spacer(modifier = Modifier.width(10.dp))
                badge()
            }
        }
        Spacer(modifier = Modifier.height(LibraryPageDefaults.HeroToControlsGap))
    }
}

@Composable
fun LibrarySectionLabel(
    text: String,
    modifier: Modifier = Modifier,
    verticalPadding: Dp = 12.dp,
    trailing: (@Composable () -> Unit)? = null,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = verticalPadding),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge.copy(
                fontFamily = SpaceMonoFontFamily,
                fontSize = 13.sp,
                letterSpacing = 0.2.sp,
            ),
            fontWeight = FontWeight.SemiBold,
            color = Color.White,
            modifier = Modifier.weight(1f),
        )
        trailing?.invoke()
    }
}

@Composable
fun LibraryPlaybackActions(
    onShuffle: () -> Unit,
    onRotation: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = LibraryPageDefaults.SidePadding, vertical = 8.dp),
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(10.dp),
    ) {
        LibraryPlaybackAction(
            label = stringResource(R.string.shuffle),
            icon = R.drawable.shuffle,
            onClick = onShuffle,
            modifier = Modifier.weight(1f),
        )
        LibraryPlaybackAction(
            label = stringResource(R.string.in_rotation),
            icon = R.drawable.graphic_eq,
            onClick = onRotation,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun LibraryPlaybackAction(
    label: String,
    icon: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(48.dp),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        contentColor = Color.White,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
        ) {
            Icon(painter = painterResource(icon), contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge.copy(fontFamily = SpaceMonoFontFamily),
                maxLines = 1,
            )
        }
    }
}

@Composable
fun LibraryCollapsibleSectionLabel(
    text: String,
    collapsed: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val rotation by animateFloatAsState(
        targetValue = if (collapsed) 180f else 0f,
        label = "librarySectionCollapse",
    )
    LibrarySectionLabel(
        text = text,
        modifier = modifier,
        trailing = { Icon(
                painter = painterResource(R.drawable.expand_more),
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier
                    .rotate(rotation)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onToggle,
                    ),
            )
        },
    )
}

@Composable
fun LibraryFooterCount(
    text: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = Color.White,
        )
    }
}

@Composable
fun LibraryPageTopBar(
    title: String,
    revealProgress: Float,
    revealState: LibraryPageRevealState,
    isSearchActive: Boolean = false,
    searchQuery: String = "",
    onSearchQueryChange: (String) -> Unit = {},
    onNavigateUp: () -> Unit,
    onSearchClick: () -> Unit = {},
    onCloseSearch: () -> Unit = {},
    keyboardController: SoftwareKeyboardController? = null,
    modifier: Modifier = Modifier,
    extraActions: @Composable RowScope.() -> Unit = {},
    showSearchAction: Boolean = true,
) {
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(isSearchActive) {
        if (isSearchActive) {
            focusRequester.requestFocus()
            keyboardController?.show()
        }
    }

    IridePageTopBar(
        title = title,
        progress = if (isSearchActive) 1f else revealProgress,
        modifier = modifier.onGloballyPositioned {
            revealState.topBarBottomPx = it.positionInWindow().y + it.size.height
        },
        navigationIcon = if (isSearchActive) null else ({
            IconButton(onClick = onNavigateUp) {
                IrideIconTile(size = 36.dp) {
                    Icon(
                        painter = painterResource(R.drawable.arrow_back),
                        contentDescription = stringResource(R.string.back),
                        tint = Color.White,
                        modifier = Modifier.size(22.dp),
                    )
                }
            }
        }),
        titleContent = if (isSearchActive) ({
            Row(verticalAlignment = Alignment.CenterVertically) {
                BasicTextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    singleLine = true,
                    textStyle = MaterialTheme.typography.titleMedium.copy(color = Color.White),
                    cursorBrush = SolidColor(Color.White),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { keyboardController?.hide() }),
                    modifier = Modifier.weight(1f).focusRequester(focusRequester),
                    decorationBox = { innerTextField ->
                        Box(contentAlignment = Alignment.CenterStart) {
                            if (searchQuery.isEmpty()) {
                                Text(
                                    text = stringResource(R.string.search_library),
                                    color = Color.White,
                                    style = MaterialTheme.typography.titleMedium,
                                )
                            }
                            innerTextField()
                        }
                    },
                )
                IconButton(onClick = onCloseSearch) {
                    Icon(
                        painter = painterResource(R.drawable.close),
                        contentDescription = stringResource(R.string.close),
                    )
                }
            }
        }) else null,
        actions = {
            if (!isSearchActive) {
                extraActions()
                if (showSearchAction) IconButton(onClick = onSearchClick) {
                    Icon(
                        painter = painterResource(R.drawable.search),
                        contentDescription = stringResource(R.string.search),
                    )
                }
            }
        },
    )
}
