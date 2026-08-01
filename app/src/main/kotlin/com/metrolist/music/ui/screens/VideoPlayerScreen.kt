/**
 * Iride Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */
package com.metrolist.music.ui.screens

import android.net.Uri
import android.view.SurfaceView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.MergingMediaSource
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.navigation.NavController
import com.metrolist.innertube.YouTube
import com.metrolist.music.R
import com.metrolist.music.ui.component.IconButton
import com.metrolist.music.ui.component.IrideAdaptiveTopBar
import com.metrolist.music.ui.utils.backToMain
import com.metrolist.music.LocalPlayerConnection
import com.metrolist.music.utils.YTPlayerUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient

private sealed interface VideoLoadState {
    data object Loading : VideoLoadState
    data class Error(val message: String) : VideoLoadState
    data object Ready : VideoLoadState
}

/**
 * Fullscreen video playback for the Artist screen "Video"/"Performance" shelf. Those items used
 * to just queue the videoId's audio track (thumbnail shown, nothing visually played) — this
 * resolves an actual video+audio stream pair via [YTPlayerUtils.resolveVideoStreams] (ported
 * from Flow's NewPipeExtractor approach, github.com/A-EDev/Flow) and renders it.
 *
 * Runs its own isolated [ExoPlayer] (same pattern as [com.metrolist.music.ui.screens.artist.ArtistGameAudioService])
 * rather than reusing [com.metrolist.music.playback.MusicService]'s queue player, since that
 * player is audio-only and deeply wired into the app's queue/download/precache pipeline.
 */
@androidx.annotation.OptIn(UnstableApi::class)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoPlayerScreen(
    navController: NavController,
    videoId: String,
    initialTitle: String?,
) {
    val context = LocalContext.current
    val view = LocalView.current
    val playerConnection = LocalPlayerConnection.current

    var loadState by remember(videoId) { mutableStateOf<VideoLoadState>(VideoLoadState.Loading) }
    var title by remember(videoId) { mutableStateOf(initialTitle) }
    var retryKey by remember(videoId) { mutableStateOf(0) }

    val exoPlayer = remember(videoId) { ExoPlayer.Builder(context).build() }

    DisposableEffect(exoPlayer) {
        view.keepScreenOn = true
        onDispose {
            view.keepScreenOn = false
            exoPlayer.release()
        }
    }

    // Two overlapping audio streams (main app queue + video) is jarring — pause the mini player.
    LaunchedEffect(videoId) {
        playerConnection?.player?.pause()
    }

    LaunchedEffect(videoId, retryKey) {
        loadState = VideoLoadState.Loading
        val result = YTPlayerUtils.resolveVideoStreams(videoId)
        val data = result.getOrNull()
        if (data == null) {
            loadState = VideoLoadState.Error(result.exceptionOrNull()?.message ?: "Unknown error")
            return@LaunchedEffect
        }
        data.title?.let { title = it }

        withContext(Dispatchers.Main) {
            val dataSourceFactory = DefaultDataSource.Factory(
                context,
                OkHttpDataSource.Factory(
                    OkHttpClient.Builder().proxy(YouTube.proxy).build(),
                ),
            )
            val mediaSourceFactory = ProgressiveMediaSource.Factory(dataSourceFactory)
            val videoSource = mediaSourceFactory.createMediaSource(
                MediaItem.fromUri(Uri.parse(data.videoUrl)),
            )
            val audioSource = mediaSourceFactory.createMediaSource(
                MediaItem.fromUri(Uri.parse(data.audioUrl)),
            )
            exoPlayer.setMediaSource(MergingMediaSource(videoSource, audioSource))
            exoPlayer.playWhenReady = true
            exoPlayer.prepare()
            loadState = VideoLoadState.Ready
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .align(Alignment.Center),
        ) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    SurfaceView(ctx).also { exoPlayer.setVideoSurfaceView(it) }
                },
            )
        }

        when (val state = loadState) {
            is VideoLoadState.Loading -> CircularProgressIndicator(
                color = Color.White,
                modifier = Modifier.align(Alignment.Center),
            )

            is VideoLoadState.Error -> Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(horizontal = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Icon(
                    painter = painterResource(R.drawable.error),
                    contentDescription = null,
                    tint = Color.White,
                )
                Text(
                    text = state.message,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                )
                IconButton(onClick = { title = initialTitle; retryKey++ }, onLongClick = {}) {
                    Icon(
                        painter = painterResource(R.drawable.refresh),
                        contentDescription = stringResource(R.string.retry),
                        tint = Color.White,
                    )
                }
            }

            is VideoLoadState.Ready -> {}
        }

        IrideAdaptiveTopBar(
            title = { Text(title ?: "") },
            navigationIcon = {
                IconButton(onClick = navController::navigateUp, onLongClick = navController::backToMain) {
                    Icon(
                        painter = painterResource(R.drawable.arrow_back),
                        contentDescription = null,
                        tint = Color.White,
                    )
                }
            },
            transparent = true,
        )
    }
}
