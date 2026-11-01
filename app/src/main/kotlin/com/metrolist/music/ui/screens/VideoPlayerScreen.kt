package com.metrolist.music.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.NavController
import com.metrolist.innertube.YouTube
import com.metrolist.music.LocalPlayerConnection
import com.metrolist.music.models.MediaMetadata
import com.metrolist.music.models.toMediaMetadata
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Compatibility route: all video entry points use the service player. */
@Composable
fun VideoPlayerScreen(navController: NavController, videoId: String, initialTitle: String?) {
    val connection = LocalPlayerConnection.current ?: return
    LaunchedEffect(connection, videoId) {
        val metadata = withContext(Dispatchers.IO) {
            YouTube.queue(listOf(videoId)).getOrNull()?.firstOrNull()?.toMediaMetadata()
        } ?: MediaMetadata(videoId, initialTitle.orEmpty(), emptyList(), -1)
        connection.videoPlayback.playVideo(metadata)
        navController.navigateUp()
    }
}
