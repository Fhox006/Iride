package com.metrolist.music.ui.component

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavController
import com.metrolist.innertube.models.WatchEndpoint
import com.metrolist.music.LocalListenTogetherManager
import com.metrolist.music.LocalPlayerConnection
import com.metrolist.music.R
import com.metrolist.music.discovery.NewRelease
import com.metrolist.music.models.toMediaMetadata
import com.metrolist.music.playback.queues.YouTubeQueue
import com.metrolist.music.ui.menu.YouTubeSongMenu
import com.metrolist.music.viewmodels.FollowedReleasesViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun HomeFollowedReleases(
    navController: NavController,
    snackbarHostState: SnackbarHostState,
    isVisible: Boolean,
    viewModel: FollowedReleasesViewModel = hiltViewModel(),
) {
    val tracks by viewModel.tracks.collectDeferredHomeState("followed_releases")
    val resolvingId by viewModel.resolvingId.collectAsStateWithLifecycle()
    val resolvedSongIds by viewModel.resolvedSongIds.collectAsStateWithLifecycle()
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val menuState = LocalMenuState.current
    val player = LocalPlayerConnection.current
    val listenTogether = LocalListenTogetherManager.current
    val isGuest = listenTogether?.isInRoom == true && !listenTogether.isHost
    val scope = rememberCoroutineScope()
    var openJob by remember { mutableStateOf<Job?>(null) }
    val unavailable = stringResource(R.string.new_releases_unavailable)
    val connectionError = stringResource(R.string.new_releases_connection_error)

    LaunchedEffect(lifecycle, viewModel, isVisible) {
        if (!isVisible) return@LaunchedEffect
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (true) {
                viewModel.refresh()
                delay(15 * 60_000L)
            }
        }
    }

    val open: (NewRelease, Boolean) -> Unit = { release, showMenu ->
        if (showMenu || !isGuest) {
            val previousJob = openJob
            previousJob?.cancel()
            openJob = scope.launch {
                previousJob?.join()
                try {
                    val song = viewModel.resolve(release)
                    when {
                        song == null -> snackbarHostState.showSnackbar(unavailable)
                        showMenu -> menuState.show(anchored = true) {
                            YouTubeSongMenu(song, navController, menuState::dismiss)
                        }
                        player == null -> snackbarHostState.showSnackbar(connectionError)
                        player.mediaMetadata.value?.id == song.id -> player.togglePlayPause()
                        else -> player.playQueue(YouTubeQueue(
                            endpoint = WatchEndpoint(videoId = song.id),
                            preloadItem = song.toMediaMetadata(),
                        ))
                    }
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Exception) {
                    snackbarHostState.showSnackbar(connectionError)
                }
            }
        }
    }

    if (tracks.isNotEmpty()) {
        SharedHomeSectionTitle(stringResource(R.string.home_new_tracks_for_you))
        IrideFolioReleaseShelf(
            releases = tracks,
            resolvingId = resolvingId,
            onClick = { open(it, false) },
            onMenuClick = { open(it, true) },
            resolvedSongIds = resolvedSongIds,
        )
    } else {
        // Keep an empty shelf measurable so visibility can trigger its first load.
        Spacer(Modifier.fillMaxWidth().height(1.dp))
    }
}
