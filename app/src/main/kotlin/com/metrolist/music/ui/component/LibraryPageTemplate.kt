/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.component

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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
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
import androidx.compose.ui.layout.boundsInWindow
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
import com.metrolist.music.ui.theme.textSecondary
import com.metrolist.music.ui.utils.IrideMotion
import com.metrolist.music.ui.utils.irideEnter
import com.metrolist.music.ui.utils.rememberDiscreteProgress
import com.metrolist.music.ui.utils.rememberEnterProgress
import com.metrolist.music.ui.utils.revealMask

object LibraryPageDefaults {
    val SidePadding = 20.dp
    val HeroTopSpace = 28.dp
    val HeroToControlsGap = 16.dp
    val GridSpacing = 12.dp
    val TopBarHeight = 40.dp
}

@Stable
class LibraryPageRevealState {
    var titleBottomPx by mutableFloatStateOf(Float.MAX_VALUE)
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
    val covered by remember(scrolledPastHeader) {
        derivedStateOf { scrolledPastHeader || state.titleBottomPx <= state.topBarBottomPx }
    }
    return rememberDiscreteProgress(covered)
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
            modifier = Modifier
                .fillMaxWidth()
                .onGloballyPositioned { revealState.titleBottomPx = it.boundsInWindow().bottom },
        ) {
            Text(
                text = title,
                style = TextStyle(
                    fontFamily = SpaceMonoFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 40.sp,
                    letterSpacing = (-0.6).sp,
                ),
                color = MaterialTheme.colorScheme.onBackground,
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
    trailing: (@Composable () -> Unit)? = null,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge.copy(
                fontFamily = SpaceMonoFontFamily,
                fontSize = 13.sp,
                letterSpacing = 0.2.sp,
            ),
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.textSecondary,
            modifier = Modifier.weight(1f),
        )
        trailing?.invoke()
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
                tint = MaterialTheme.colorScheme.textSecondary,
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
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
fun LibraryPageTopBar(
    title: String,
    revealProgress: Float,
    revealState: LibraryPageRevealState,
    backdrop: FrostBackdrop?,
    isSearchActive: Boolean,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onNavigateUp: () -> Unit,
    onSearchClick: () -> Unit,
    onCloseSearch: () -> Unit,
    keyboardController: SoftwareKeyboardController?,
    modifier: Modifier = Modifier,
    extraActions: @Composable RowScope.() -> Unit = {},
) {
    val focusRequester = remember { FocusRequester() }
    val backProgress = rememberEnterProgress(play = true, durationMillis = IrideMotion.Short)

    LaunchedEffect(isSearchActive) {
        if (isSearchActive) {
            focusRequester.requestFocus()
            keyboardController?.show()
        }
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .onGloballyPositioned { revealState.topBarBottomPx = it.boundsInWindow().bottom }
            .frostedTopBarBackground(
                progress = revealProgress,
                barColor = MaterialTheme.colorScheme.background,
                strokeColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f),
                backdrop = backdrop,
            )
            .statusBarsPadding()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {},
            )
            .height(LibraryPageDefaults.TopBarHeight)
            .padding(horizontal = 4.dp),
    ) {
        if (isSearchActive) {
            TextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                placeholder = {
                    Text(
                        text = stringResource(R.string.search_library),
                        style = MaterialTheme.typography.titleMedium,
                    )
                },
                singleLine = true,
                textStyle = MaterialTheme.typography.titleMedium,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { keyboardController?.hide() }),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent,
                ),
                modifier =
                    Modifier
                        .weight(1f)
                        .focusRequester(focusRequester),
            )

            IconButton(onClick = onCloseSearch) {
                Icon(
                    painter = painterResource(R.drawable.close),
                    contentDescription = stringResource(R.string.close),
                )
            }
        } else {
            Box(modifier = Modifier.irideEnter(backProgress, 6.dp)) {
                IconButton(onClick = onNavigateUp) {
                    Icon(
                        painter = painterResource(R.drawable.arrow_back),
                        contentDescription = null,
                    )
                }
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 4.dp)
                    .irideEnter(revealProgress, 6.dp)
                    .revealMask(revealProgress),
            ) {
                Text(
                    text = title,
                    style = TextStyle(
                        fontFamily = SpaceMonoFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        letterSpacing = (-0.1).sp,
                    ),
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                )
            }
            extraActions()
            IconButton(onClick = onSearchClick) {
                Icon(
                    painter = painterResource(R.drawable.search),
                    contentDescription = stringResource(R.string.search),
                )
            }
        }
    }
}
