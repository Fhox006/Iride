/** Metrolist Project (C) 2026 Licensed under GPL-3.0 | See git history for contributors */
package com.metrolist.music.ui.component

import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.navigation.NavController
import com.metrolist.music.R
import com.metrolist.music.ui.theme.IBMPlexMonoFontFamily
import com.metrolist.music.ui.utils.backToMain

@Composable
fun IridePageBackButton(navController: NavController) {
    IconButton(
        onClick = navController::navigateUp,
        onLongClick = navController::backToMain,
    ) {
        Icon(
            painter = painterResource(R.drawable.arrow_back),
            contentDescription = stringResource(R.string.back),
        )
    }
}

/** Contextual page bar for inline search, selection counters and actions. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IrideAdaptiveTopBar(
    title: @Composable () -> Unit,
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
    menuSections: List<List<IrideFolioMenuAction>> = emptyList(),
    transparent: Boolean = false,
    scrollBehavior: TopAppBarScrollBehavior? = null,
    backdrop: FrostBackdrop? = null,
    revealProgress: Float = 0f,
) {
    IridePageTopBar(
        title = "",
        progress = if (transparent) revealProgress else LocalPageControlsProgress.current,
        navigationIcon = navigationIcon,
        actions = actions,
        menuSections = menuSections,
        titleContent = {
            ProvideTextStyle(
                MaterialTheme.typography.titleMedium.copy(
                    fontFamily = IBMPlexMonoFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                )
            ) {
                title()
            }
        },
    )
}
