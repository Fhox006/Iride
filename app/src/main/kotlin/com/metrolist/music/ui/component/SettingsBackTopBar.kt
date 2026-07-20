/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import com.metrolist.music.ui.theme.SpaceMonoFontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.metrolist.music.R
import com.metrolist.music.constants.PureBlackKey
import com.metrolist.music.constants.TopNavigationBarKey
import com.metrolist.music.ui.utils.backToMain
import com.metrolist.music.utils.rememberPreference

/**
 * Shared back-navigation top bar for settings sub-screens (Player, About, Appearance, etc).
 * Classic [TopAppBar] when New Iride UI is off; a bar matching [TopNavigationBar]'s bold
 * monospace look (and pure-black/background awareness) when it's on, so every settings
 * page reads as part of the same New Iride UI theme as Home/Library/Search/Account.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsBackTopBar(
    title: String,
    navController: NavController,
    actions: @Composable RowScope.() -> Unit = {},
) {
    val (topNavigationBarEnabled) = rememberPreference(TopNavigationBarKey, defaultValue = true)
    val (pureBlack) = rememberPreference(PureBlackKey, defaultValue = false)

    if (topNavigationBarEnabled) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(if (pureBlack) Color.Black else Color.Transparent)
                .statusBarsPadding()
                .height(56.dp)
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(
                onClick = navController::navigateUp,
                onLongClick = navController::backToMain,
            ) {
                Icon(
                    painterResource(R.drawable.arrow_back),
                    contentDescription = null,
                )
            }
            Text(
                text = title,
                style = TextStyle(
                    fontFamily = SpaceMonoFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    letterSpacing = (-0.1).sp,
                ),
                color = if (pureBlack) Color.White else MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 4.dp),
            )
            actions()
        }
    } else {
        TopAppBar(
            title = { Text(title) },
            navigationIcon = {
                IconButton(
                    onClick = navController::navigateUp,
                    onLongClick = navController::backToMain,
                ) {
                    Icon(
                        painterResource(R.drawable.arrow_back),
                        contentDescription = null,
                    )
                }
            },
            actions = actions,
        )
    }
}

/**
 * Slot-based variant of [SettingsBackTopBar] for screens whose top bar carries dynamic content
 * (selection counters, inline search fields, contextual actions). Renders a classic [TopAppBar]
 * when New Iride UI is off; the same flat 56dp monospace bar as [SettingsBackTopBar] when it's on.
 * Inside the Iride bar the [title] slot inherits the bold monospace style via [ProvideTextStyle],
 * so plain `Text(...)`/`TextField(textStyle = LocalTextStyle.current)` content matches the theme
 * without per-screen styling.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IrideAdaptiveTopBar(
    title: @Composable () -> Unit,
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
    // For screens that draw their own full-bleed background (gradient art, hero images):
    // keeps the bar container transparent in both modes so the artwork shows through.
    transparent: Boolean = false,
    // Only applies to the classic TopAppBar branch — the Iride bar is fixed and flat.
    scrollBehavior: TopAppBarScrollBehavior? = null,
) {
    val (topNavigationBarEnabled) = rememberPreference(TopNavigationBarKey, defaultValue = true)
    val (pureBlack) = rememberPreference(PureBlackKey, defaultValue = false)

    if (topNavigationBarEnabled) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    when {
                        transparent -> Color.Transparent
                        pureBlack -> Color.Black
                        else -> MaterialTheme.colorScheme.background
                    },
                )
                .statusBarsPadding()
                .height(56.dp)
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            navigationIcon()
            Box(modifier = Modifier.weight(1f).padding(start = 4.dp)) {
                ProvideTextStyle(
                    TextStyle(
                        fontFamily = SpaceMonoFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        letterSpacing = (-0.1).sp,
                        color = if (pureBlack) Color.White else MaterialTheme.colorScheme.onBackground,
                    ),
                ) {
                    title()
                }
            }
            actions()
        }
    } else {
        TopAppBar(
            title = title,
            navigationIcon = navigationIcon,
            actions = actions,
            colors = if (transparent) {
                TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            } else {
                TopAppBarDefaults.topAppBarColors()
            },
            scrollBehavior = scrollBehavior,
        )
    }
}
