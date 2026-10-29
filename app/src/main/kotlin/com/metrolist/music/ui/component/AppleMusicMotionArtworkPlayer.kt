/**
 * SPDX-License-Identifier: GPL-3.0-only
 * Loop and first-frame handoff adapted from SimpMusic/core MediaPlayerView.kt,
 * revision 2894e549763b8a68225b4456d3290f3227bdbb01 (SimpMusic contributors).
 * https://github.com/maxrave-dev/core/tree/2894e549763b8a68225b4456d3290f3227bdbb01/media/media3-ui
 * Iride uses a lifecycle-aware, silent TextureView so Folio's fades and blur apply to video.
 */
package com.metrolist.music.ui.component

import android.content.Context
import android.graphics.Matrix
import androidx.annotation.OptIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import android.view.TextureView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.Player
import androidx.media3.common.PlaybackException
import androidx.media3.common.VideoSize
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.common.C
import java.util.concurrent.atomic.AtomicBoolean
import timber.log.Timber

@OptIn(UnstableApi::class)
@Composable
internal fun AppleMusicMotionArtworkPlayer(
    url: String,
    modifier: Modifier = Modifier,
    onFirstFrame: () -> Unit = {},
    onError: () -> Unit = {},
    active: Boolean = true,
) {
    val context = LocalContext.current.applicationContext
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val latestOnFirstFrame = rememberUpdatedState(onFirstFrame)
    val latestOnError = rememberUpdatedState(onError)
    val latestActive = rememberUpdatedState(active)
    val player = remember(context, url, lifecycle) {
        try {
            ExoPlayer.Builder(context, DefaultRenderersFactory(context).setEnableDecoderFallback(true))
                .setLoadControl(DefaultLoadControl.Builder()
                    .setBufferDurationsMs(500, 2_500, 250, 500)
                    .setTargetBufferBytes(4 * 1024 * 1024)
                    .build())
                .build()
        } catch (error: RuntimeException) {
            Timber.w(error, "Unable to create artist motion player")
            null
        } catch (error: LinkageError) {
            Timber.w(error, "Artist motion renderer unavailable")
            null
        }
    }
    if (player == null) {
        LaunchedEffect(url) { latestOnError.value() }
        return
    }
    val released = remember(player) { AtomicBoolean(false) }
    var videoAspectRatio by remember(player) { mutableFloatStateOf(1f) }
    DisposableEffect(player, lifecycle) {
        fun updatePlayback() {
            if (!released.get()) {
                try {
                    player.playWhenReady = latestActive.value && lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)
                } catch (error: RuntimeException) {
                    Timber.d(error, "Unable to update artist motion playback")
                    latestOnError.value()
                }
            }
        }
        val listener = object : Player.Listener {
            override fun onRenderedFirstFrame() {
                if (!released.get()) latestOnFirstFrame.value()
            }
            override fun onPlayerError(error: PlaybackException) {
                Timber.d(error, "Artist motion playback failed")
                if (!released.get()) latestOnError.value()
            }
            override fun onVideoSizeChanged(videoSize: VideoSize) {
                if (videoSize.width > 0 && videoSize.height > 0) {
                    val ratio = videoSize.width * videoSize.pixelWidthHeightRatio / videoSize.height
                    if (ratio.isFinite() && ratio > 0f) videoAspectRatio = ratio
                }
            }
        }
        val observer = LifecycleEventObserver { _, _ -> updatePlayback() }
        player.addListener(listener)
        lifecycle.addObserver(observer)
        try {
            player.volume = 0f
            player.repeatMode = Player.REPEAT_MODE_ONE
            player.trackSelectionParameters = player.trackSelectionParameters.buildUpon()
                .setTrackTypeDisabled(C.TRACK_TYPE_AUDIO, true)
                .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
                .setMaxVideoSize(1280, 1280)
                .setPreferredVideoMimeType(MimeTypes.VIDEO_H264)
                .build()
            player.setMediaItem(MediaItem.Builder().setUri(url).setMimeType(MimeTypes.APPLICATION_M3U8).build())
            updatePlayback()
            player.prepare()
        } catch (error: RuntimeException) {
            Timber.w(error, "Unable to prepare artist motion playback")
            latestOnError.value()
        }
        onDispose {
            lifecycle.removeObserver(observer)
            if (released.compareAndSet(false, true)) {
                player.removeListener(listener)
                try {
                    player.clearVideoSurface()
                } catch (error: RuntimeException) {
                    Timber.d(error, "Unable to detach artist motion surface")
                } finally {
                    try {
                        player.release()
                    } catch (error: RuntimeException) {
                        Timber.d(error, "Unable to release artist motion player")
                    }
                }
            }
        }
    }
    LaunchedEffect(player, lifecycle, active) {
        if (!released.get()) {
            try {
                player.playWhenReady = active && lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)
            } catch (error: RuntimeException) {
                Timber.d(error, "Unable to change artist motion playback")
                latestOnError.value()
            }
        }
    }
    AndroidView(
        factory = { viewContext -> ArtistMotionTextureView(viewContext) },
        update = {
            it.videoAspectRatio = videoAspectRatio
            if (!released.get()) {
                try {
                    player.setVideoTextureView(it)
                } catch (error: RuntimeException) {
                    Timber.d(error, "Unable to attach artist motion surface")
                    latestOnError.value()
                }
            }
        },
        onRelease = {
            if (!released.get()) {
                try {
                    player.clearVideoTextureView(it)
                } catch (error: RuntimeException) {
                    Timber.d(error, "Unable to clear artist motion texture")
                }
            }
        },
        modifier = modifier,
    )
}

private class ArtistMotionTextureView(context: Context) : TextureView(context) {
    var videoAspectRatio = 1f
        set(value) {
            field = value
            updateCrop()
        }

    init {
        isOpaque = false
    }

    override fun onSizeChanged(width: Int, height: Int, oldWidth: Int, oldHeight: Int) {
        super.onSizeChanged(width, height, oldWidth, oldHeight)
        updateCrop()
    }

    private fun updateCrop() {
        if (width <= 0 || height <= 0 || !videoAspectRatio.isFinite() || videoAspectRatio <= 0f) return
        val relativeAspect = videoAspectRatio / (width.toFloat() / height)
        setTransform(Matrix().apply {
            setScale(maxOf(relativeAspect, 1f), maxOf(1f / relativeAspect, 1f), width / 2f, height / 2f)
        })
    }
}
