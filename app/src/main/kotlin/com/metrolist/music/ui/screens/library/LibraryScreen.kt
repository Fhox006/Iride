/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.screens.library

import androidx.compose.runtime.Composable
import androidx.navigation.NavController

@Composable
fun LibraryScreen(
    navController: NavController,
) {
    LibraryMixScreen(navController = navController)
}
