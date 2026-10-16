package com.metrolist.music.playback

import android.net.Uri
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.source.MergingMediaSource
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.okhttp.OkHttpDataSource
import com.metrolist.innertube.YouTube
import com.metrolist.innertube.models.SongItem
import com.metrolist.music.extensions.metadata
import com.metrolist.music.extensions.toMediaItem
import com.metrolist.music.models.MediaMetadata
import com.metrolist.music.models.toMediaMetadata
import com.metrolist.music.playback.queues.ListQueue
import com.metrolist.music.utils.YTPlayerUtils
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient

data class VideoPlaybackState(
    val mediaId: String? = null,
    val isVideo: Boolean = false,
    val loading: Boolean = false,
    val searching: Boolean = false,
    val candidates: List<SongItem> = emptyList(),
    val failed: Boolean = false,
    val audioMetadata: MediaMetadata? = null,
)

/** Shares the service player, queue and MediaSession with audio playback. */
@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
class VideoPlayback(
    private val scope: CoroutineScope,
    private val player: () -> ExoPlayer,
    private val playQueue: (ListQueue) -> Unit,
    private val startTrackRadio: (MediaMetadata) -> Unit,
    private val preloadArtwork: (MediaMetadata) -> Unit,
    private val transitionEdition: suspend (MediaItem, Long, () -> Unit) -> Unit,
    private val canChangePlayback: () -> Boolean,
) {
    private val mutableState = MutableStateFlow(VideoPlaybackState())
    val state = mutableState.asStateFlow()
    private val mutableOpenPlayerRequests = MutableStateFlow(0L)
    val openPlayerRequests = mutableOpenPlayerRequests.asStateFlow()
    private var searchJob: Job? = null
    private var switchJob: Job? = null
    private val audioSources = android.util.LruCache<String, MediaMetadata>(24)
    private val videoSources = android.util.LruCache<String, MediaMetadata>(24)
    private var retryCount = 0
    private val streamMutex = Mutex()
    private val streams = android.util.LruCache<String, Pair<Long, YTPlayerUtils.VideoPlaybackData>>(6)
    private var videoSurface: android.view.TextureView? = null
    private var surfacePlayer: ExoPlayer? = null
    private val httpFactory by lazy {
        OkHttpDataSource.Factory(
            OkHttpClient.Builder().proxy(YouTube.proxy).proxyAuthenticator { _, response ->
                val auth = YouTube.proxyAuth ?: return@proxyAuthenticator null
                if (response.request.header("Proxy-Authorization") != null) null
                else response.request.newBuilder().header("Proxy-Authorization", auth).build()
            }.build(),
        )
    }
    private var maxVideoHeight = VideoPlaybackPolicy.DEFAULT_HEIGHT
    private var videoModeRequested = false
    private var requestedMediaId: String? = null
    private var activationJob: Job? = null
    private var qualityRecoveryJob: Job? = null
    private var networkRecoveryJob: Job? = null
    private var audioMatch: Deferred<MediaMetadata>? = null
    private var radioVideoId: String? = null

    fun isVideoItem(item: MediaItem?): Boolean = item?.localConfiguration?.uri?.scheme == "iride-video"

    fun isOpenPlayerReady(mediaId: String): Boolean = videoModeRequested && requestedMediaId == mediaId &&
        state.value.isVideo && state.value.mediaId == mediaId

    private fun videoItem(metadata: MediaMetadata): MediaItem = metadata.toMediaItem().buildUpon()
        .setUri(Uri.Builder().scheme("iride-video").authority(metadata.id)
            .appendQueryParameter("height", maxVideoHeight.toString()).build()).build()

    fun onMediaItemTransition(item: MediaItem?) {
        val current = mutableState.value
        if (current.mediaId == item?.mediaId && current.isVideo == isVideoItem(item)) {
            if (isVideoItem(item)) item?.metadata?.let(::prepareAudioEdition)
            return
        }
        searchJob?.cancel()
        switchJob?.cancel()
        activationJob?.cancel()
        qualityRecoveryJob?.cancel()
        networkRecoveryJob?.cancel()
        retryCount = 0
        maxVideoHeight = VideoPlaybackPolicy.DEFAULT_HEIGHT
        audioMatch?.cancel()
        audioMatch = null
        mutableState.value = VideoPlaybackState(item?.mediaId, isVideoItem(item),
            audioMetadata = item?.mediaId?.let(audioSources::get))
        item?.metadata?.let(preloadArtwork)
        if (isVideoItem(item)) {
            val active = player()
            active.trackSelectionParameters = active.trackSelectionParameters.buildUpon()
                .setTrackTypeDisabled(C.TRACK_TYPE_VIDEO, videoSurface == null).build()
            item?.metadata?.let(::prepareAudioEdition)
        }
    }

    private fun prepareAudioEdition(metadata: MediaMetadata, retryMissing: Boolean = false) {
        val cachedAudio = audioSources.get(metadata.id)
        cachedAudio?.takeUnless { retryMissing && it.isVideoSong }?.let { audio ->
            if (radioVideoId == metadata.id) {
                radioVideoId = null
                if (!audio.isVideoSong && canChangePlayback()) startTrackRadio(audio)
            }
            return
        }
        if (retryMissing && cachedAudio?.isVideoSong == true) audioSources.remove(metadata.id)
        if (audioMatch?.isActive == true) return
        audioMatch = scope.async {
            val original = try {
                withContext(Dispatchers.IO) {
                    val query = "${metadata.title} ${metadata.artists.joinToString(" ") { it.name }}"
                    matchingAudioEdition(metadata, YouTube.search(query, YouTube.SearchFilter.FILTER_SONG)
                        .getOrThrow().items.filterIsInstance<SongItem>())?.toMediaMetadata()
                }
            } catch (cancelled: CancellationException) { throw cancelled
            } catch (_: Exception) { null }
            ensureActive()
            val audio = original ?: metadata
            audioSources.put(metadata.id, audio)
            if (original != null) videoSources.put(original.id, metadata)
            preloadArtwork(audio)
            if (mutableState.value.mediaId == metadata.id) {
                mutableState.value = mutableState.value.copy(audioMetadata = audio)
            }
            if (radioVideoId == metadata.id) {
                radioVideoId = null
                if (original != null && canChangePlayback() && player().currentMediaItem?.mediaId == metadata.id) {
                    startTrackRadio(original)
                }
            }
            audio
        }
    }

    fun attachSurface(active: ExoPlayer, surface: android.view.TextureView) {
        if (videoSurface === surface && surfacePlayer === active) return
        videoSurface = surface
        surfacePlayer = active
        active.setVideoTextureView(surface)
        if (active.trackSelectionParameters.disabledTrackTypes.contains(C.TRACK_TYPE_VIDEO)) {
            active.trackSelectionParameters = active.trackSelectionParameters.buildUpon()
                .setTrackTypeDisabled(C.TRACK_TYPE_VIDEO, false).build()
        }
    }

    suspend fun favoriteMetadata(metadata: MediaMetadata): MediaMetadata? {
        val state = mutableState.value
        if (!metadata.isVideoSong && !(state.isVideo && state.mediaId == metadata.id)) return metadata
        prepareAudioEdition(metadata, retryMissing = true)
        val audio = audioSources.get(metadata.id) ?: audioMatch?.await()
        return favoriteMetadataForEdition(metadata, metadata.id, audio)
    }

    fun detachSurface(active: ExoPlayer, surface: android.view.TextureView) {
        // A recycled view may already belong to the incoming player after an edition swap.
        if (videoSurface === surface && surfacePlayer !== active) return
        active.clearVideoTextureView(surface)
        if (videoSurface === surface && surfacePlayer === active) {
            videoSurface = null
            surfacePlayer = null
            active.trackSelectionParameters = active.trackSelectionParameters.buildUpon()
                .setTrackTypeDisabled(C.TRACK_TYPE_VIDEO, true).build()
        }
    }

    suspend fun resolveStreams(id: String, height: Int = maxVideoHeight): YTPlayerUtils.VideoPlaybackData = streamMutex.withLock {
        val key = "$id:$height"
        streams.get(key)?.takeIf { it.first > System.currentTimeMillis() }?.let { return@withLock it.second }
        val result = withContext(Dispatchers.IO) { YTPlayerUtils.resolveVideoStreams(id, height).getOrThrow() }
        streams.put(key, System.currentTimeMillis() + (result.expiresInSeconds - 60).coerceAtLeast(1) * 1000L to result)
        result
    }

    fun mediaSourceFactory(audioFactory: MediaSource.Factory): MediaSource.Factory = object : MediaSource.Factory by audioFactory {
        override fun createMediaSource(mediaItem: MediaItem): MediaSource {
            if (!isVideoItem(mediaItem)) return audioFactory.createMediaSource(mediaItem)
            val height = mediaItem.localConfiguration?.uri?.getQueryParameter("height")?.toIntOrNull()
                ?: VideoPlaybackPolicy.DEFAULT_HEIGHT
            fun source(video: Boolean): MediaSource {
                val resolving = DataSource.Factory {
                    VideoRangeDataSource(httpFactory.createDataSource()) {
                        val data = runBlocking(Dispatchers.IO) { resolveStreams(mediaItem.mediaId, height) }
                        VideoRangeDataSource.Stream(if (video) data.videoUrl else data.audioUrl,
                            if (video) data.videoLength else data.audioLength, data.userAgent)
                    }
                }
                return ProgressiveMediaSource.Factory(resolving).createMediaSource(mediaItem)
            }
            // Align adaptive tracks and their ends; mismatched timestamps can stall a raw merge.
            return MergingMediaSource(true, true, source(true), source(false))
        }
    }

    fun findVideos(metadata: MediaMetadata, force: Boolean = false) {
        if (metadata.isEpisode) return
        if (!force && mutableState.value.mediaId == metadata.id &&
            (mutableState.value.searching || mutableState.value.candidates.isNotEmpty())) return
        searchJob?.cancel()
        searchJob = scope.launch {
            mutableState.value = mutableState.value.copy(searching = true, failed = false)
            try {
                val candidates = withContext(Dispatchers.IO) {
                    val query = "${metadata.title} ${metadata.artists.joinToString(" ") { it.name }}"
                    rankVideoEditions(metadata, YouTube.search(query, YouTube.SearchFilter.FILTER_VIDEO)
                        .getOrThrow().items.filterIsInstance<SongItem>()).take(12)
                }
                ensureActive()
                if (player().currentMediaItem?.mediaId == metadata.id) {
                    mutableState.value = mutableState.value.copy(candidates = candidates, searching = false)
                }
            } catch (cancelled: CancellationException) { throw cancelled
            } catch (_: Exception) {
                mutableState.value = mutableState.value.copy(searching = false, failed = true)
            }
        }
    }

    fun enterVideoMode(metadata: MediaMetadata, force: Boolean = false) {
        if (!canChangePlayback() || metadata.isEpisode) return
        if (!force && videoModeRequested && requestedMediaId == metadata.id) return
        videoModeRequested = true
        requestedMediaId = metadata.id
        activationJob?.cancel()
        switchJob?.cancel()
        if (state.value.isVideo) {
            prepareAudioEdition(metadata)
            mutableState.value = state.value.copy(loading = false)
            return
        }
        activationJob = scope.launch {
            mutableState.value = state.value.copy(searching = true, failed = false)
            try {
                videoSources.get(metadata.id)?.let { cached ->
                    mutableState.value = state.value.copy(searching = false)
                    switchEdition(cached, metadata)
                    return@launch
                }
                if (metadata.isVideoSong) {
                    mutableState.value = state.value.copy(searching = false)
                    showCurrentVideo()
                    return@launch
                }
                val candidates = state.value.candidates.takeIf { !force && it.isNotEmpty() }
                    ?: withContext(Dispatchers.IO) {
                        val query = "${metadata.title} ${metadata.artists.joinToString(" ") { it.name }}"
                        rankVideoEditions(metadata, YouTube.search(query, YouTube.SearchFilter.FILTER_VIDEO)
                            .getOrThrow().items.filterIsInstance<SongItem>()).take(12)
                    }
                ensureActive()
                if (videoModeRequested && player().currentMediaItem?.mediaId == metadata.id) {
                    mutableState.value = state.value.copy(searching = false, candidates = candidates)
                    candidates.firstOrNull()?.let { selectVideo(it) }
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                mutableState.value = state.value.copy(searching = false, failed = true)
            }
        }
    }

    fun leaveVideoMode() {
        if (!videoModeRequested) return
        videoModeRequested = false
        requestedMediaId = null
        activationJob?.cancel()
        searchJob?.cancel()
        switchJob?.cancel()
        qualityRecoveryJob?.cancel()
        networkRecoveryJob?.cancel()
        val active = player()
        // Future entries also return to audio when the video panel is closed.
        for (index in 0 until active.mediaItemCount) {
            if (index == active.currentMediaItemIndex) continue
            val item = active.getMediaItemAt(index)
            if (isVideoItem(item)) item.metadata?.let { active.replaceMediaItem(index, (audioSources.get(it.id) ?: it).toMediaItem()) }
        }
        if (state.value.isVideo) switchToAudio()
        else mutableState.value = state.value.copy(loading = false, searching = false)
    }

    fun onPlaybackStateChanged(playbackState: Int) {
        qualityRecoveryJob?.cancel()
        if (!state.value.isVideo) return
        val expectedPlayer = player()
        val expected = expectedPlayer.currentMediaItem
        if (playbackState == androidx.media3.common.Player.STATE_READY) {
            // A brief READY between failures must not give a broken stream unlimited retries.
            qualityRecoveryJob = scope.launch {
                kotlinx.coroutines.delay(15_000)
                if (player() === expectedPlayer && expectedPlayer.currentMediaItem == expected &&
                    expectedPlayer.playbackState == androidx.media3.common.Player.STATE_READY
                ) retryCount = 0
            }
            return
        }
        if (playbackState != androidx.media3.common.Player.STATE_BUFFERING) return
        qualityRecoveryJob = scope.launch {
            kotlinx.coroutines.delay(6_000)
            val active = player()
            if (!videoModeRequested || active !== expectedPlayer || active.currentMediaItem != expected || !active.playWhenReady ||
                active.playbackState != androidx.media3.common.Player.STATE_BUFFERING || state.value.loading
            ) return@launch
            val lower = VideoPlaybackPolicy.lowerHeight(maxVideoHeight) ?: return@launch
            maxVideoHeight = lower
            val current = active.currentMediaItem?.metadata ?: return@launch
            switchEdition(current, audioSources.get(current.id) ?: current)
        }
    }

    fun playVideo(metadata: MediaMetadata, sourceRoute: String? = null) {
        if (!canChangePlayback()) return
        videoModeRequested = true
        requestedMediaId = metadata.id
        radioVideoId = metadata.id
        val entry = metadata.copy(thumbnailUrl = youtubeVideoThumbnail(metadata.id, metadata.thumbnailUrl),
            musicVideoType = "MUSIC_VIDEO_TYPE_OMV")
        preloadArtwork(entry)
        playQueue(ListQueue(items = listOf(videoItem(entry)), sourceRoute = sourceRoute))
        mutableOpenPlayerRequests.value++
    }

    fun selectVideo(candidate: SongItem) {
        if (!canChangePlayback()) return
        val current = player().currentMediaItem?.metadata ?: return
        val original = audioSources.get(current.id) ?: current
        val metadata = candidate.toMediaMetadata().copy(thumbnailUrl = youtubeVideoThumbnail(candidate.id, candidate.thumbnail),
            musicVideoType = "MUSIC_VIDEO_TYPE_OMV")
        switchEdition(metadata, original = original)
    }

    fun showCurrentVideo() {
        val current = player().currentMediaItem?.metadata ?: return
        switchEdition(current.copy(thumbnailUrl = youtubeVideoThumbnail(current.id)), original = current)
    }

    fun switchToAudio() {
        if (!canChangePlayback()) return
        val current = player().currentMediaItem?.metadata ?: return
        switchJob?.cancel()
        switchJob = scope.launch {
            val id = current.id
            mutableState.value = mutableState.value.copy(loading = true, failed = false)
            try {
                prepareAudioEdition(current)
                val original = audioSources.get(id) ?: audioMatch?.await() ?: current
                ensureActive()
                if (canChangePlayback() && player().currentMediaItem?.mediaId == id) {
                    replaceCurrent(original.toMediaItem(), original.duration * 1000L)
                }
            } catch (cancelled: CancellationException) { throw cancelled
            } catch (_: Exception) {
                if (canChangePlayback() && player().currentMediaItem?.mediaId == id) {
                    mutableState.value = mutableState.value.copy(failed = true)
                }
            } finally {
                if (mutableState.value.mediaId == id) mutableState.value = mutableState.value.copy(loading = false)
            }
        }
    }

    private fun switchEdition(metadata: MediaMetadata, original: MediaMetadata) {
        if (!canChangePlayback()) return
        switchJob?.cancel()
        val expected = player().currentMediaItem
        switchJob = scope.launch {
            mutableState.value = mutableState.value.copy(loading = true, failed = false)
            try {
                val data = resolveStreams(metadata.id)
                ensureActive()
                if (!canChangePlayback() || player().currentMediaItem != expected) return@launch
                audioSources.put(metadata.id, original)
                videoSources.put(original.id, metadata)
                preloadArtwork(original)
                val resolvedMetadata = metadata.copy(
                    thumbnailUrl = data.thumbnailUrl ?: metadata.thumbnailUrl,
                    duration = data.durationSeconds.takeIf { it in 1L..Int.MAX_VALUE.toLong() }?.toInt() ?: metadata.duration,
                )
                replaceCurrent(videoItem(resolvedMetadata), data.durationSeconds * 1000L)
            } catch (cancelled: CancellationException) { throw cancelled
            } catch (_: Exception) {
                mutableState.value = mutableState.value.copy(failed = true)
            } finally {
                mutableState.value = mutableState.value.copy(loading = false)
            }
        }
    }

    private suspend fun replaceCurrent(item: MediaItem, targetDuration: Long) {
        val active = player()
        val index = active.currentMediaItemIndex
        if (index == C.INDEX_UNSET) return
        transitionEdition(item, targetDuration) {
            if (!canChangePlayback()) throw CancellationException("Playback is no longer available")
            searchJob?.cancel()
            retryCount = 0
            if (isVideoItem(item) && videoModeRequested) requestedMediaId = item.mediaId
            mutableState.value = VideoPlaybackState(item.mediaId, isVideoItem(item),
                audioMetadata = audioSources.get(item.mediaId))
        }
    }

    fun onPlayerError(error: PlaybackException) {
        qualityRecoveryJob?.cancel()
        networkRecoveryJob?.cancel()
        val lower = VideoPlaybackPolicy.lowerHeight(maxVideoHeight)
        if (videoModeRequested && canChangePlayback() && lower != null && error.errorCode in 4001..4005) {
            maxVideoHeight = lower
            val current = player().currentMediaItem?.metadata
            if (current != null) {
                switchEdition(current, audioSources.get(current.id) ?: current)
                return
            }
        }
        val retryDelay = VideoPlaybackPolicy.retryDelay(retryCount)
        if (videoModeRequested && canChangePlayback() && error.errorCode in 2000..2999 && retryDelay != null) {
            retryCount++
            val active = player()
            val expected = active.currentMediaItem
            mutableState.value = mutableState.value.copy(failed = false)
            networkRecoveryJob = scope.launch {
                kotlinx.coroutines.delay(retryDelay)
                if (!videoModeRequested || !canChangePlayback() || player() !== active || active.currentMediaItem != expected) return@launch
                streamMutex.withLock { streams.evictAll() }
                if (!videoModeRequested || !canChangePlayback() || player() !== active || active.currentMediaItem != expected) return@launch
                // prepare() preserves the user's pause state and the current media position.
                active.prepare()
            }
        } else {
            mutableState.value = mutableState.value.copy(failed = true)
        }
    }

    fun retry() {
        if (!canChangePlayback()) return
        networkRecoveryJob?.cancel()
        retryCount = 0
        val active = player()
        streams.evictAll()
        mutableState.value = mutableState.value.copy(failed = false)
        active.prepare()
        active.play()
    }
}
