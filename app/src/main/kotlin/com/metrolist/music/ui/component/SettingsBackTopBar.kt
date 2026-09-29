/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.component

import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.metrolist.music.ui.theme.SpaceMonoFontFamily
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.metrolist.music.R
import com.metrolist.music.ui.utils.backToMain

/** Compact shared bar for callers whose title is already collapsed. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsBackTopBar(
    title: String,
    navController: NavController,
    actions: @Composable RowScope.() -> Unit = {},
    backdrop: FrostBackdrop? = null,
    revealProgress: Float = 0f,
) {
    IridePageTopBar(
        title = title,
        progress = 1f,
        navigationIcon = { IridePageBackButton(navController) },
        actions = actions,
    )
}

@Composable
fun IridePageBackButton(navController: NavController) {
    IconButton(
        onClick = navController::navigateUp,
        onLongClick = navController::backToMain,
    ) {
        IrideIconTile(size = 36.dp) {
            Icon(
                painter = painterResource(R.drawable.arrow_back),
                contentDescription = stringResource(R.string.back),
                tint = Color.White,
                modifier = Modifier.size(22.dp),
            )
        }
    }
}

/** Contextual page bar for inline search, selection counters and actions. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IrideAdaptiveTopBar(
    title: @Composable () -> Unit,
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
    transparent: Boolean = false,
    scrollBehavior: TopAppBarScrollBehavior? = null,
    backdrop: FrostBackdrop? = null,
    revealProgress: Float = 0f,
) {
    IridePageTopBar(
        title = "",
        progress = if (transparent) revealProgress else 1f,
        navigationIcon = navigationIcon,
        actions = actions,
        titleContent = {
            ProvideTextStyle(
                MaterialTheme.typography.titleMedium.copy(
                    fontFamily = SpaceMonoFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                ),
            ) {
                title()
            }
        },
    )
}
