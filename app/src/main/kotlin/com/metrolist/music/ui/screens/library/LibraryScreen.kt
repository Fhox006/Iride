/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.screens.library

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import com.metrolist.music.constants.LibraryFilter

enum class LibraryView { LIBRARY, DOWNLOADS }

@Composable
fun LibraryScreen(navController: NavController) {
    var currentView by remember { mutableStateOf(LibraryView.LIBRARY) }
    var selectedCategory by remember { mutableStateOf<LibraryFilter?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        when {
            selectedCategory == LibraryFilter.PLAYLISTS -> LibraryPlaylistsScreen(
                navController = navController,
                onDeselect = { selectedCategory = null },
            )
            selectedCategory == LibraryFilter.SONGS -> LibrarySongsScreen(
                navController = navController,
                onDeselect = { selectedCategory = null },
            )
            selectedCategory == LibraryFilter.ALBUMS -> LibraryAlbumsScreen(
                navController = navController,
                onDeselect = { selectedCategory = null },
            )
            selectedCategory == LibraryFilter.ARTISTS -> LibraryArtistsScreen(
                navController = navController,
                onDeselect = { selectedCategory = null },
            )
            currentView == LibraryView.LIBRARY -> LibraryMixScreen(
                navController = navController,
                currentView = currentView,
                onViewChange = { currentView = it },
                onNavigateToCategory = { selectedCategory = it },
            )
            else -> LibraryDownloadsScreen(
                navController = navController,
                currentView = currentView,
                onViewChange = { currentView = it },
                onNavigateToCategory = { selectedCategory = it },
            )
        }
    }
}
