/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.screens.library

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import com.metrolist.music.constants.LibraryView
import com.metrolist.music.viewmodels.LibraryViewModel

@Composable
fun LibraryScreen(
    navController: NavController,
    viewModel: LibraryViewModel = hiltViewModel(),
) {
    var currentView by viewModel.currentView

    Box(modifier = Modifier.fillMaxSize()) {
        if (currentView == LibraryView.LIBRARY) {
            LibraryMixScreen(
                navController = navController,
                currentView = currentView,
                onViewChange = { currentView = it },
            )
        } else {
            LibraryDownloadsScreen(
                navController = navController,
                currentView = currentView,
                onViewChange = { currentView = it },
            )
        }
    }
}
