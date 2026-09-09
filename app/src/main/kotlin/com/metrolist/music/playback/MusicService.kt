/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

@file:Suppress("DEPRECATION")

package com.metrolist.music.playback

import android.app.ForegroundServiceStartNotAllowedException
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.database.SQLException
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.audiofx.AudioEffect
import android.media.audiofx.LoudnessEnhancer
import android.net.ConnectivityManager
import android.os.Binder
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.content.getSystemService
import androidx.core.net.toUri
import androidx.datastore.preferences.core.edit
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.Player.EVENT_POSITION_DISCONTINUITY
import androidx.media3.common.Player.EVENT_TIMELINE_CHANGED
import androidx.media3.common.Player.REPEAT_MODE_ALL
import androidx.media3.common.Player.REPEAT_MODE_OFF
import androidx.media3.common.Player.REPEAT_MODE_ONE
import androidx.media3.common.Player.STATE_IDLE
import androidx.media3.common.Timeline
import androidx.media3.common.audio.SonicAudioProcessor
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.HttpDataSource
import androidx.media3.datasource.ResolvingDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.analytics.AnalyticsListener
import androidx.media3.exoplayer.analytics.PlaybackStats
import androidx.media3.exoplayer.analytics.PlaybackStatsListener
import androidx.media3.exoplayer.audio.DefaultAudioSink
import androidx.media3.exoplayer.audio.DefaultAudioTrackBufferSizeProvider
import androidx.media3.exoplayer.audio.SilenceSkippingAudioProcessor
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.source.ShuffleOrder.DefaultShuffleOrder
import androidx.media3.extractor.ExtractorsFactory
import androidx.media3.extractor.mkv.MatroskaExtractor
import androidx.media3.extractor.mp4.FragmentedMp4Extractor
import androidx.media3.session.CommandButton
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaController
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaSession
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.MoreExecutors
import com.metrolist.innertube.YouTube
import com.metrolist.innertube.models.SongItem
import com.metrolist.innertube.models.WatchEndpoint
import com.metrolist.lastfm.LastFM
import com.metrolist.music.MainActivity
import com.metrolist.music.R
import com.metrolist.music.constants.AndroidAutoTargetPlaylistKey
import com.metrolist.music.constants.AudioNormalizationKey
import com.metrolist.music.constants.AudioOffload
import com.metrolist.music.constants.ScratchBufferKey
import com.metrolist.music.constants.ScratchBufferSize
import com.metrolist.music.constants.AudioQualityKey
import com.metrolist.music.constants.AutoDownloadOnLikeKey
import com.metrolist.music.constants.AutoLoadMoreKey
import com.metrolist.music.constants.AutoSkipNextOnErrorKey
import com.metrolist.music.constants.CrossfadeDurationKey
import com.metrolist.music.constants.CrossfadeEnabledKey
import com.metrolist.music.constants.CrossfadeGaplessKey
import com.metrolist.music.constants.DisableLoadMoreWhenRepeatAllKey
import com.metrolist.music.constants.DiscordActivityNameKey
import com.metrolist.music.constants.DiscordActivityTypeKey
import com.metrolist.music.constants.DiscordAdvancedModeKey
import com.metrolist.music.constants.DiscordAvatarKey
import com.metrolist.music.constants.DiscordButton1TextKey
import com.metrolist.music.constants.DiscordButton1VisibleKey
import com.metrolist.music.constants.DiscordButton2TextKey
import com.metrolist.music.constants.DiscordButton2VisibleKey
import com.metrolist.music.constants.DiscordStatusKey
import com.metrolist.music.constants.DiscordTokenKey
import com.metrolist.music.constants.DiscordUseDetailsKey
import com.metrolist.music.constants.EnableDiscordRPCKey
import com.metrolist.music.constants.EnableLastFMScrobblingKey
import com.metrolist.music.constants.EnableSongCacheKey
import com.metrolist.music.constants.MuzzaPlayerLogicKey
import com.metrolist.music.constants.HideExplicitKey
import com.metrolist.music.constants.HideVideoSongsKey
import com.metrolist.music.constants.HistoryDuration
import com.metrolist.music.constants.LastFMUseNowPlaying
import com.metrolist.music.constants.MediaSessionConstants
import com.metrolist.music.constants.MediaSessionConstants.CommandAddToTargetPlaylist
import com.metrolist.music.constants.MediaSessionConstants.CommandToggleLike
import com.metrolist.music.constants.MediaSessionConstants.CommandToggleRepeatMode
import com.metrolist.music.constants.MediaSessionConstants.CommandToggleShuffle
import com.metrolist.music.constants.MediaSessionConstants.CommandToggleStartRadio
import com.metrolist.music.constants.PauseListenHistoryKey
import com.metrolist.music.constants.PauseOnMute
import com.metrolist.music.constants.PersistentQueueKey
import com.metrolist.music.constants.PersistentShuffleAcrossQueuesKey
import com.metrolist.music.constants.PlayerVolumeKey
import com.metrolist.music.constants.PreventDuplicateTracksInQueueKey
import com.metrolist.music.constants.RememberShuffleAndRepeatKey
import com.metrolist.music.constants.RepeatModeKey
import com.metrolist.music.constants.ResumeOnBluetoothConnectKey
import com.metrolist.music.constants.ScrobbleDelayPercentKey
import com.metrolist.music.constants.ScrobbleDelaySecondsKey
import com.metrolist.music.constants.ScrobbleMinSongDurationKey
import com.metrolist.music.constants.ShowLyricsKey
import com.metrolist.music.constants.ShuffleModeKey
import com.metrolist.music.constants.ShufflePlaylistFirstKey
import com.metrolist.music.constants.SkipFadeDurationKey
import com.metrolist.music.constants.SkipFadeKey
import com.metrolist.music.constants.SkipSilenceInstantKey
import com.metrolist.music.constants.SkipSilenceKey
import com.metrolist.music.constants.StopMusicOnTaskClearKey
import com.metrolist.music.db.MusicDatabase
import com.metrolist.music.db.entities.Event
import com.metrolist.music.db.entities.FormatEntity
import com.metrolist.music.db.entities.LyricsEntity
import com.metrolist.music.db.entities.LyricsEntity.Companion.LYRICS_NOT_FOUND
import com.metrolist.music.db.entities.PlaylistEntity
import com.metrolist.music.db.entities.RelatedSongMap
import com.metrolist.music.db.entities.Song
import com.metrolist.music.di.DownloadCache
import com.metrolist.music.di.PlayerCache
import com.metrolist.music.eq.EqualizerService
import com.metrolist.music.eq.EqDeviceManager
import com.metrolist.music.eq.audio.CustomEqualizerAudioProcessor
import com.metrolist.music.eq.data.EQProfileRepository
import com.metrolist.music.extensions.SilentHandler
import com.metrolist.music.extensions.collect
import com.metrolist.music.extensions.collectLatest
import com.metrolist.music.extensions.currentMetadata
import com.metrolist.music.extensions.findNextMediaItemById
import com.metrolist.music.extensions.mediaItems
import com.metrolist.music.extensions.metadata
import com.metrolist.music.extensions.setOffloadEnabled
import com.metrolist.music.extensions.toEnum
import com.metrolist.music.extensions.toMediaItem
import com.metrolist.music.extensions.toPersistQueue
import com.metrolist.music.extensions.toQueue
import com.metrolist.music.lyrics.LyricsHelper
import com.metrolist.music.models.PersistPlayerState
import com.metrolist.music.models.MediaMetadata
import com.metrolist.music.models.PersistQueue
import com.metrolist.music.models.toMediaMetadata
import com.metrolist.music.playback.alarm.MusicAlarmScheduler
import com.metrolist.music.playback.alarm.MusicAlarmStore
import com.metrolist.music.playback.audio.ScratchAudioProcessor
import com.metrolist.music.playback.audio.SilenceDetectorAudioProcessor
import com.metrolist.music.playback.queues.EmptyQueue
import com.metrolist.music.playback.queues.ListQueue
import com.metrolist.music.playback.queues.Queue
import com.metrolist.music.playback.queues.YouTubeQueue
import com.metrolist.music.playback.queues.filterExplicit
import com.metrolist.music.playback.queues.filterVideoSongs
import com.metrolist.music.ui.utils.resize
import com.metrolist.music.utils.CoilBitmapLoader
import com.metrolist.music.utils.DiscordRPC
import com.metrolist.music.utils.NetworkConnectivityObserver
import com.metrolist.music.utils.ScrobbleManager
import com.metrolist.music.utils.SyncUtils
import com.metrolist.music.utils.YTPlayerUtils
import com.metrolist.music.utils.dataStore
import com.metrolist.music.utils.get
import com.metrolist.music.utils.reportException
import com.metrolist.music.widget.MetrolistWidgetManager
import com.metrolist.music.widget.MusicWidgetReceiver
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import timber.log.Timber
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.time.LocalDateTime
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import kotlin.coroutines.coroutineContext
import kotlin.random.Random
import kotlin.time.Duration.Companion.seconds

private const val INSTANT_SILENCE_SKIP_STEP_MS = 15_000L
private const val INSTANT_SILENCE_SKIP_SETTLE_MS = 350L

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
@androidx.annotation.OptIn(UnstableApi::class)
@AndroidEntryPoint
class MusicService :
    MediaLibraryService(),
    Player.Listener,
    PlaybackStatsListener.Callback {
    @Inject
    lateinit var database: MusicDatabase

    @Inject
    lateinit var lyricsHelper: LyricsHelper

    @Inject
    lateinit var syncUtils: SyncUtils

    @Inject
    lateinit var mediaLibrarySessionCallback: MediaLibrarySessionCallback

    @Inject
    lateinit var equalizerService: EqualizerService

    @Inject
    lateinit var eqProfileRepository: EQProfileRepository

    @Inject
    lateinit var eqDeviceManager: EqDeviceManager

    @Inject
    lateinit var widgetManager: MetrolistWidgetManager

    @Inject
    lateinit var listenTogetherManager: com.metrolist.music.listentogether.ListenTogetherManager

    private lateinit var audioManager: AudioManager
    private var audioFocusRequest: AudioFocusRequest? = null
    private var lastAudioFocusState = AudioManager.AUDIOFOCUS_NONE
    private var wasPlayingBeforeAudioFocusLoss = false
    private var hasAudioFocus = false
    private var reentrantFocusGain = false
    private var wasPlayingBeforeVolumeMute = false
    private var isPausedByVolumeMute = false

    private var crossfadeEnabled = false
    private var crossfadeDuration = 5000f
    private var crossfadeGapless = true
    private var crossfadeTriggerJob: Job? = null
    private var skipFadeEnabled = false
    private var skipFadeDuration = 3500f
    private var persistentQueueEnabled = true
    private var shufflePlaylistFirst = false
    private var rememberShuffleAndRepeat = true
    private var preloadJob: Job? = null
    private var muzzaPlayerLogicEnabled = false

    private val secondaryPlayerListener =
        object : Player.Listener {
            override fun onPlayerError(error: PlaybackException) {
                Timber.tag(TAG).e(error, "Secondary player error")
                secondaryPlayer?.stop()
                secondaryPlayer?.clearMediaItems()
                secondaryPlayer = null
            }
        }

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private val binder = MusicBinder()

    inner class MusicBinder : Binder() {
        val service: MusicService
            get() = this@MusicService
    }

    private lateinit var connectivityManager: ConnectivityManager
    lateinit var connectivityObserver: NetworkConnectivityObserver
    val waitingForNetworkConnection = MutableStateFlow(false)
    private val isNetworkConnected = MutableStateFlow(false)

    private lateinit var audioQuality: com.metrolist.music.constants.AudioQuality

    private var currentQueue: Queue = EmptyQueue
    var queueTitle: String? = null

    val currentMediaMetadata = MutableStateFlow<com.metrolist.music.models.MediaMetadata?>(null)
    private val currentSong =
        currentMediaMetadata
            .flatMapLatest { mediaMetadata ->
                database.song(mediaMetadata?.id)
            }.stateIn(scope, SharingStarted.Lazily, null)
    private val currentFormat =
        currentMediaMetadata.flatMapLatest { mediaMetadata ->
            database.format(mediaMetadata?.id)
        }

    lateinit var playerVolume: MutableStateFlow<Float>
    val isMuted = MutableStateFlow(false)
    private val sleepTimerVolumeMultiplier = MutableStateFlow(1f)
    private val audioFocusVolumeMultiplier = MutableStateFlow(1f)

    fun toggleMute() {
        val newMutedState = !isMuted.value
        isMuted.value = newMutedState
        applyEffectiveVolume()
    }

    fun setMuted(muted: Boolean) {
        isMuted.value = muted
        applyEffectiveVolume()
    }

    private fun calculateEffectiveVolume(
        volume: Float = playerVolume.value,
        muted: Boolean = isMuted.value,
        sleepTimerMultiplier: Float = sleepTimerVolumeMultiplier.value,
        focusMultiplier: Float = audioFocusVolumeMultiplier.value,
    ): Float {
        if (muted) return 0f
        return (volume * sleepTimerMultiplier * focusMultiplier).coerceIn(0f, 1f)
    }

    private fun applyEffectiveVolume() {
        if (!::player.isInitialized || isCrossfading) return
        player.volume = calculateEffectiveVolume()
    }

    lateinit var sleepTimer: SleepTimer

    @Inject
    @PlayerCache
    lateinit var playerCache: SimpleCache

    @Inject
    @DownloadCache
    lateinit var downloadCache: SimpleCache

    lateinit var player: ExoPlayer
        private set
    lateinit var scratchProcessor: ScratchAudioProcessor
        private set
    private var scratchBufferSize = ScratchBufferSize.MEDIUM

    /**
     * Sizes the turntable's scrubbable window. [ScratchBufferSize.FULL] wants the whole track, so
     * it can only be applied once a duration is known — hence this runs again on every media item
     * transition, not just when the setting changes. Allocation happens off the playback thread;
     * the processor swaps the new buffer in at its next flush.
     */
    private fun applyScratchBufferSize() {
        if (!::scratchProcessor.isInitialized) return
        val size = scratchBufferSize
        val durationMs = player.duration
        val seconds = when {
            size != ScratchBufferSize.FULL -> size.seconds
            durationMs > 0 -> (durationMs / 1000).toInt() + 5
            else -> ScratchBufferSize.LONG.seconds
        }
        scope.launch(Dispatchers.Default) { scratchProcessor.requestHistorySeconds(seconds) }
    }
    private var secondaryPlayer: ExoPlayer? = null
    private var fadingPlayer: ExoPlayer? = null
    private var isCrossfading = false
    private var crossfadeJob: Job? = null

    private lateinit var mediaSession: MediaLibrarySession

    private val playerInitialized = MutableStateFlow(false)
    val isPlayerReady: kotlinx.coroutines.flow.StateFlow<Boolean> = playerInitialized.asStateFlow()

    private val pendingQueueRestore = MutableStateFlow(false)
    val hasPendingQueueRestoreFlow: kotlinx.coroutines.flow.StateFlow<Boolean> = pendingQueueRestore.asStateFlow()

    private val _playerFlow = MutableStateFlow<ExoPlayer?>(null)
    val playerFlow = _playerFlow.asStateFlow()

    private val playerSilenceProcessors = HashMap<Player, SilenceDetectorAudioProcessor>()

    private val audioBandLevelsFlow = MutableStateFlow(com.metrolist.music.playback.audio.AudioBandLevels())
    val audioBandLevels: kotlinx.coroutines.flow.StateFlow<com.metrolist.music.playback.audio.AudioBandLevels> =
        audioBandLevelsFlow.asStateFlow()

    private val instantSilenceSkipEnabled = MutableStateFlow(false)

    private var isAudioEffectSessionOpened = false
    private var loudnessEnhancer: LoudnessEnhancer? = null

    private var discordRpc: DiscordRPC? = null
    private var lastPlaybackSpeed = 1.0f
    private var discordUpdateJob: kotlinx.coroutines.Job? = null

    private var scrobbleManager: ScrobbleManager? = null

    val automixItems = MutableStateFlow<List<MediaItem>>(emptyList())

    // Radio tracks already promoted into the real player timeline (sliced from
    // [appendedRadioMediaIds] in playback order). The UI merges this with [automixItems]
    // so the radio section is the single source of truth for "what plays next", with no
    // duplicate real-timeline row sitting between the current track and the radio header.
    val promotedRadioItems = MutableStateFlow<List<MediaItem>>(emptyList())

    val isAutoMixQueueActive = MutableStateFlow(false)

    // Bumped whenever the radio state is reset so that in-flight automix fetches
    // (async network calls) are discarded instead of resurrecting a cleared radio.
    private val automixRequestId = AtomicInteger(0)

    // Timeline position the radio "up next" was last ensured for; prevents repeated
    // promotions from a single playback position.
    private var automixEnsuredAtIndex: Int = C.INDEX_UNSET

    // Periodic keeper that promotes the next radio track while the radio is armed.
    private var automixWatchJob: kotlinx.coroutines.Job? = null

    // Consecutive extension attempts that yielded nothing new; after a few of them with an
    // empty reserve the radio disarms itself so the toggle never lies about being on.
    private var automixEmptyExtends: Int = 0

    // Serializes every mutation of the radio reserve list: promotions run on service/UI
    // threads while extension batches land on background ones, and an unsynchronized
    // read-modify-write used to resurrect already-promoted tracks as duplicates.
    private val automixLock = Any()

    // True while an extension batch is being fetched for the radio reserve list.
    private var automixExtending: Boolean = false

    // Fixed size of the upcoming radio as seen by the user: 1 promoted track in the
    // timeline + (limit - 1) visible virtual rows.
    val automixUpcomingLimit = 10

    // When the reserve list drops below this, an extra batch is fetched in the background.
    private val AUTOMIX_REFILL_THRESHOLD = 14

    // Media IDs of radio songs auto-appended to the real timeline while the radio was
    // active; they must leave the queue again when the radio is turned off.
    private val appendedRadioMediaIds = LinkedHashSet<String>()

    // Single-radio controller: the seed the running radio was started from, plus the
    // timestamp of the last toggle. Rapid double taps on the wheel used to
    // start-then-immediately-stop (or start twice), leaving the radio seemingly
    // dead or with a doubled upcoming queue.
    private var radioSeedId: String? = null
    private var lastRadioToggleAt: Long = 0L
    private val RADIO_TOGGLE_DEBOUNCE_MS = 600L

    // Snapshot of the upcoming timeline taken when the stable radio is armed from
    // the player, so toggling the radio back off restores exactly what was there
    // before (same songs, same order, same continuation). Null means there is
    // nothing to restore (radio started fresh from a menu, or a new queue has
    // been played since).
    private var preRadioUpcoming: List<MediaItem>? = null
    private var preRadioQueue: Queue = EmptyQueue
    private var preRadioTitle: String? = null

    private var originalQueueSize: Int = 0

    private var consecutivePlaybackErr = 0
    private var retryJob: Job? = null
    private var retryCount = 0
    private var silenceSkipJob: Job? = null

    private val songUrlCache = HashMap<String, Pair<String, Long>>()

    private val bypassCacheForQualityChange = mutableSetOf<String>()

    private var currentMediaIdRetryCount = mutableMapOf<String, Int>()
    private val MAX_RETRY_PER_SONG = 3
    private val RETRY_DELAY_MS = 1000L

    private val recentlyFailedSongs = mutableSetOf<String>()
    private var failedSongsClearJob: Job? = null

    var castConnectionHandler: CastConnectionHandler? = null
        private set

    private val screenStateReceiver =
        object : BroadcastReceiver() {
            override fun onReceive(
                context: Context,
                intent: Intent,
            ) {
                when (intent.action) {
                    Intent.ACTION_SCREEN_OFF -> {
                        if (!player.isPlaying) {
                            scope.launch(Dispatchers.IO) {
                                discordRpc?.closeRPC()
                            }
                        }
                    }

                    Intent.ACTION_SCREEN_ON -> {
                        if (player.isPlaying) {
                            scope.launch {
                                currentSong.value?.let { song ->
                                    updateDiscordRPC(song)
                                }
                            }
                        }
                    }
                }
            }
        }

    private val audioDeviceCallback =
        object : AudioDeviceCallback() {
            override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>?) {
                super.onAudioDevicesAdded(addedDevices)
                val hasBluetooth =
                    addedDevices?.any {
                        it.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP ||
                                it.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO
                    } == true

                if (hasBluetooth) {
                    if (dataStore.get(ResumeOnBluetoothConnectKey, false)) {
                        if (player.playbackState == Player.STATE_READY && !player.isPlaying) {
                            player.play()
                        }
                    }
                }
            }
        }

    override fun onCreate() {
        super.onCreate()
        isRunning = true

        playerInitialized.value = false


        try {
            val nm = getSystemService(NotificationManager::class.java)
            nm?.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID,
                    getString(R.string.music_player),
                    NotificationManager.IMPORTANCE_LOW,
                ),
            )
            val pending =
                PendingIntent.getActivity(
                    this,
                    0,
                    Intent(this, MainActivity::class.java),
                    PendingIntent.FLAG_IMMUTABLE,
                )
            val notification: Notification =
                NotificationCompat
                    .Builder(this, CHANNEL_ID)
                    .setContentTitle(getString(R.string.music_player))
                    .setContentText("")
                    .setSmallIcon(R.drawable.small_icon)
                    .setContentIntent(pending)
                    .setOngoing(true)
                    .build()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(
                    NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK,
                )
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
        } catch (e: Exception) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                e is ForegroundServiceStartNotAllowedException
            ) {
                Timber.tag(TAG).w("Foreground service start not allowed (likely app in background)")
            } else {
                Timber.tag(TAG).e(e, "Failed to create foreground notification")
                reportException(e)
            }
        }

        setMediaNotificationProvider(
            DefaultMediaNotificationProvider(
                this,
                { NOTIFICATION_ID },
                CHANNEL_ID,
                R.string.music_player,
            ).apply {
                setSmallIcon(R.drawable.small_icon)
            },
        )
        player = createExoPlayer()
        _playerFlow.value = player
        player.addListener(this@MusicService)
        sleepTimer =
            SleepTimer(scope, player) { multiplier ->
                sleepTimerVolumeMultiplier.value = multiplier
            }
        player.addListener(sleepTimer)

        playerInitialized.value = true
        Timber.tag(TAG).d("Player successfully initialized")

        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        setupAudioFocusRequest()

        mediaLibrarySessionCallback.apply {
            service = this@MusicService
            toggleLike = ::toggleLike
            toggleStartRadio = ::toggleStartRadio
            toggleLibrary = ::toggleLibrary
            addToTargetPlaylist = ::addToTargetPlaylist
        }
        mediaSession =
            MediaLibrarySession
                .Builder(this, player, mediaLibrarySessionCallback)
                .setSessionActivity(
                    PendingIntent.getActivity(
                        this,
                        0,
                        Intent(this, MainActivity::class.java),
                        PendingIntent.FLAG_IMMUTABLE,
                    ),
                ).setBitmapLoader(CoilBitmapLoader(this, scope))
                .build()
        player.repeatMode = dataStore.get(RepeatModeKey, REPEAT_MODE_OFF)

        if (rememberShuffleAndRepeat) {
            player.shuffleModeEnabled = dataStore.get(ShuffleModeKey, false)
        }

        val sessionToken = SessionToken(this, ComponentName(this, MusicService::class.java))
        val controllerFuture = MediaController.Builder(this, sessionToken).buildAsync()
        controllerFuture.addListener({ controllerFuture.get() }, MoreExecutors.directExecutor())

        connectivityManager = getSystemService()!!
        connectivityObserver = NetworkConnectivityObserver(this)

        val screenStateFilter =
            IntentFilter().apply {
                addAction(Intent.ACTION_SCREEN_ON)
                addAction(Intent.ACTION_SCREEN_OFF)
            }
        registerReceiver(screenStateReceiver, screenStateFilter)

        audioManager.registerAudioDeviceCallback(audioDeviceCallback, null)

        audioQuality = dataStore.get(AudioQualityKey).toEnum(com.metrolist.music.constants.AudioQuality.AUTO)
        playerVolume = MutableStateFlow(dataStore.get(PlayerVolumeKey, 1f).coerceIn(0f, 1f))

        initializeCast()

        lyricsHelper.preferred.collectLatest(scope) {}

        scope.launch {
            eqProfileRepository.effectiveProfile.collect { profile ->
                // Coefficients are applied in place by the audio processor; seeking here
                // would stutter playback on every live EQ tweak.
                if (profile != null) {
                    equalizerService.applyProfile(profile)
                } else {
                    equalizerService.disable()
                }
            }
        }

        scope.launch {
            connectivityObserver.networkStatus.collect { isConnected ->
                isNetworkConnected.value = isConnected
                if (isConnected && waitingForNetworkConnection.value) {
                    triggerRetry()
                }
                if (isConnected && discordRpc != null && player.isPlaying) {
                    val mediaId = player.currentMetadata?.id
                    if (mediaId != null) {
                        database.song(mediaId).first()?.let { song ->
                            updateDiscordRPC(song)
                        }
                    }
                }
            }
        }

        var isFirstQualityEmit = true
        scope.launch {
            dataStore.data
                .map {
                    it[AudioQualityKey]?.let { value ->
                        com.metrolist.music.constants.AudioQuality.entries
                            .find { it.name == value }
                    } ?: com.metrolist.music.constants.AudioQuality.AUTO
                }.distinctUntilChanged()
                .collect { newQuality ->
                    val oldQuality = audioQuality
                    audioQuality = newQuality

                    if (isFirstQualityEmit) {
                        isFirstQualityEmit = false
                        Timber.tag("MusicService").i("QUALITY INIT: $newQuality")
                        return@collect
                    }

                    Timber.tag("MusicService").i("QUALITY CHANGED: $oldQuality -> $newQuality")

                    val mediaId = player.currentMediaItem?.mediaId ?: return@collect
                    val currentPosition = player.currentPosition
                    val wasPlaying = player.isPlaying
                    val currentIndex = player.currentMediaItemIndex

                    Timber.tag("MusicService").i("RELOADING STREAM: $mediaId at position ${currentPosition}ms")

                    songUrlCache.remove(mediaId)

                    runBlocking(Dispatchers.IO) {
                        try {
                            playerCache.removeResource(mediaId)
                            downloadCache.removeResource(mediaId)
                            Timber.tag("MusicService").d("Cleared player and download cache for $mediaId")
                        } catch (e: Exception) {
                            Timber.tag("MusicService").e(e, "Failed to clear cache for $mediaId")
                        }
                    }

                    bypassCacheForQualityChange.add(mediaId)
                    Timber.tag("MusicService").d("Set bypass cache flag for $mediaId")

                    player.stop()
                    player.seekTo(currentIndex, currentPosition)
                    player.prepare()
                    if (wasPlaying) {
                        player.play()
                    }
                }
        }

        combine(
            playerVolume,
            isMuted,
            sleepTimerVolumeMultiplier,
            audioFocusVolumeMultiplier,
        ) { volume, muted, timerMultiplier, focusMultiplier ->
            calculateEffectiveVolume(
                volume = volume,
                muted = muted,
                sleepTimerMultiplier = timerMultiplier,
                focusMultiplier = focusMultiplier,
            )
        }.collectLatest(scope) {
            if (!isCrossfading) {
                player.volume = it
            }
        }

        playerVolume.debounce(1000).collect(scope) { volume ->
            dataStore.edit { settings ->
                settings[PlayerVolumeKey] = volume
            }
        }

        currentSong.debounce(1000).collect(scope) { song ->
            updateNotification()
            updateWidgetUI(player.isPlaying)
        }

        combine(
            currentMediaMetadata.distinctUntilChangedBy { it?.id },
            dataStore.data.map { it[ShowLyricsKey] ?: false }.distinctUntilChanged(),
        ) { mediaMetadata, showLyrics ->
            mediaMetadata to showLyrics
        }.collectLatest(scope) { (mediaMetadata, showLyrics) ->
            if (showLyrics && mediaMetadata != null && database
                    .lyrics(mediaMetadata.id)
                    .first() == null
            ) {
                val lyricsWithProvider = lyricsHelper.getLyrics(mediaMetadata)
                if (lyricsWithProvider.lyrics != LYRICS_NOT_FOUND) {
                    if (database.lyrics(mediaMetadata.id).first() == null) {
                        database.query {
                            upsert(
                                LyricsEntity(
                                    id = mediaMetadata.id,
                                    lyrics = lyricsWithProvider.lyrics,
                                    provider = lyricsWithProvider.provider,
                                ),
                            )
                        }
                    }
                }
            }
        }

        dataStore.data
            .map { (it[SkipSilenceKey] ?: false) to (it[SkipSilenceInstantKey] ?: false) }
            .distinctUntilChanged()
            .collectLatest(scope) { (skipSilence, instantSkip) ->
                player.skipSilenceEnabled = skipSilence
                secondaryPlayer?.skipSilenceEnabled = skipSilence

                val enableInstant = skipSilence && instantSkip
                instantSilenceSkipEnabled.value = enableInstant

                playerSilenceProcessors.values.forEach { processor ->
                    processor.instantModeEnabled = enableInstant
                    if (!enableInstant) {
                        processor.resetTracking()
                    }
                }

                if (!enableInstant) {
                    silenceSkipJob?.cancel()
                }
            }

        dataStore.data
            .map { it[ScratchBufferKey].toEnum(ScratchBufferSize.MEDIUM) }
            .distinctUntilChanged()
            .collectLatest(scope) { size ->
                scratchBufferSize = size
                applyScratchBufferSize()
            }

        combine(
            currentFormat,
            dataStore.data
                .map { it[AudioNormalizationKey] ?: true }
                .distinctUntilChanged(),
        ) { format, normalizeAudio ->
            format to normalizeAudio
        }.collectLatest(scope) { (format, normalizeAudio) -> setupLoudnessEnhancer() }

        combine(
            dataStore.data.map { it[AudioOffload] ?: false },
            dataStore.data.map { it[CrossfadeEnabledKey] ?: false },
        ) { offloadPref, crossfadeEnabled ->
            if (crossfadeEnabled) false else offloadPref
        }.distinctUntilChanged()
            .collectLatest(scope) { useOffload ->
                player.setOffloadEnabled(useOffload)
                secondaryPlayer?.setOffloadEnabled(useOffload)
            }

        dataStore.data
            .map { it[DiscordTokenKey] to (it[EnableDiscordRPCKey] ?: true) }
            .debounce(300)
            .distinctUntilChanged()
            .collect(scope) { (key, enabled) ->
                if (discordRpc?.isRpcRunning() == true) {
                    discordRpc?.closeRPC()
                }
                discordRpc = null
                if (key != null && enabled) {
                    discordRpc = DiscordRPC(this, key)
                    if (player.playbackState == Player.STATE_READY && player.playWhenReady) {
                        currentSong.value?.let {
                            updateDiscordRPC(it, true)
                        }
                    }
                }
            }

        dataStore.data
            .map {
                listOf(
                    it[DiscordUseDetailsKey],
                    it[DiscordAdvancedModeKey],
                    it[DiscordStatusKey],
                    it[DiscordButton1TextKey],
                    it[DiscordButton1VisibleKey],
                    it[DiscordButton2TextKey],
                    it[DiscordButton2VisibleKey],
                    it[DiscordActivityTypeKey],
                    it[DiscordActivityNameKey],
                )
            }.debounce(300)
            .distinctUntilChanged()
            .collect(scope) {
                if (player.playbackState == Player.STATE_READY) {
                    currentSong.value?.let { song ->
                        updateDiscordRPC(song, true)
                    }
                }
            }

        dataStore.data
            .map { it[EnableLastFMScrobblingKey] ?: false }
            .debounce(300)
            .distinctUntilChanged()
            .collect(scope) { enabled ->
                if (enabled && scrobbleManager == null) {
                    val delayPercent = dataStore.get(ScrobbleDelayPercentKey, LastFM.DEFAULT_SCROBBLE_DELAY_PERCENT)
                    val minSongDuration =
                        dataStore.get(ScrobbleMinSongDurationKey, LastFM.DEFAULT_SCROBBLE_MIN_SONG_DURATION)
                    val delaySeconds = dataStore.get(ScrobbleDelaySecondsKey, LastFM.DEFAULT_SCROBBLE_DELAY_SECONDS)
                    scrobbleManager =
                        ScrobbleManager(
                            scope,
                            minSongDuration = minSongDuration,
                            scrobbleDelayPercent = delayPercent,
                            scrobbleDelaySeconds = delaySeconds,
                        )
                    scrobbleManager?.useNowPlaying = dataStore.get(LastFMUseNowPlaying, false)
                } else if (!enabled && scrobbleManager != null) {
                    scrobbleManager?.destroy()
                    scrobbleManager = null
                }
            }

        dataStore.data
            .map { it[LastFMUseNowPlaying] ?: false }
            .distinctUntilChanged()
            .collectLatest(scope) {
                scrobbleManager?.useNowPlaying = it
            }

        dataStore.data
            .map { prefs ->
                Triple(
                    prefs[ScrobbleDelayPercentKey] ?: LastFM.DEFAULT_SCROBBLE_DELAY_PERCENT,
                    prefs[ScrobbleMinSongDurationKey] ?: LastFM.DEFAULT_SCROBBLE_MIN_SONG_DURATION,
                    prefs[ScrobbleDelaySecondsKey] ?: LastFM.DEFAULT_SCROBBLE_DELAY_SECONDS,
                )
            }.distinctUntilChanged()
            .collect(scope) { (delayPercent, minSongDuration, delaySeconds) ->
                scrobbleManager?.let {
                    it.scrobbleDelayPercent = delayPercent
                    it.minSongDuration = minSongDuration
                    it.scrobbleDelaySeconds = delaySeconds
                }
            }

        combine(
            dataStore.data.map { prefs ->
                Triple(
                    prefs[CrossfadeEnabledKey] ?: false,
                    prefs[CrossfadeDurationKey] ?: 5f,
                    prefs[CrossfadeGaplessKey] ?: true,
                )
            },
            listenTogetherManager.roomState,
        ) { (enabled, duration, gapless), roomState ->
            Triple(enabled && roomState == null, duration, gapless)
        }.distinctUntilChanged()
            .collect(scope) { (enabled, duration, gapless) ->
                crossfadeEnabled = enabled
                crossfadeDuration = duration * 1000f
                crossfadeGapless = gapless
            }

        dataStore.data
            .map { prefs -> prefs[SkipFadeKey] ?: false }
            .distinctUntilChanged()
            .collect(scope) { enabled ->
                skipFadeEnabled = enabled
            }

        dataStore.data
            .map { prefs -> prefs[SkipFadeDurationKey] ?: 3.5f }
            .distinctUntilChanged()
            .collect(scope) { duration ->
                skipFadeDuration = duration * 1000f
            }

        dataStore.data
            .map { prefs -> prefs[PersistentQueueKey] ?: true }
            .distinctUntilChanged()
            .collect(scope) { persistentQueueEnabled = it }

        dataStore.data
            .map { prefs -> prefs[ShufflePlaylistFirstKey] ?: false }
            .distinctUntilChanged()
            .collect(scope) { shufflePlaylistFirst = it }

        dataStore.data
            .map { prefs -> prefs[RememberShuffleAndRepeatKey] ?: true }
            .distinctUntilChanged()
            .collect(scope) { rememberShuffleAndRepeat = it }

        dataStore.data
            .map { prefs -> prefs[MuzzaPlayerLogicKey] ?: true }
            .distinctUntilChanged()
            .collect(scope) { muzzaPlayerLogicEnabled = it }

        if (persistentQueueEnabled) {
            val queueFile = filesDir.resolve(PERSISTENT_QUEUE_FILE)
            if (queueFile.exists()) {
                pendingQueueRestore.value = true
                runCatching {
                    queueFile.inputStream().use { fis ->
                        ObjectInputStream(fis).use { oos ->
                            oos.readObject() as PersistQueue
                        }
                    }
                }.onSuccess { queue ->
                    runCatching {
                        val restoredQueue = queue.toQueue()
                        scope.launch {
                            try {
                                playerInitialized.first { it }
                                if (isActive) {
                                    playQueue(
                                        queue = restoredQueue,
                                        playWhenReady = false,
                                    )
                                }
                            } finally {
                                pendingQueueRestore.value = false
                            }
                        }
                    }.onFailure { error ->
                        Timber.tag(TAG).w(error, "Failed to restore persisted queue, clearing data")
                        clearPersistedQueueFiles()
                        pendingQueueRestore.value = false
                    }
                }.onFailure { error ->
                    Timber.tag(TAG).w(error, "Failed to read persisted queue, clearing data")
                    clearPersistedQueueFiles()
                    pendingQueueRestore.value = false
                }
            }


            val playerStateFile = filesDir.resolve(PERSISTENT_PLAYER_STATE_FILE)
            if (playerStateFile.exists()) {
                runCatching {
                    playerStateFile.inputStream().use { fis ->
                        ObjectInputStream(fis).use { oos ->
                            oos.readObject() as PersistPlayerState
                        }
                    }
                }.onSuccess { playerState ->
                    scope.launch {
                        delay(1000)
                        playerVolume.value = playerState.volume

                        if (playerState.currentMediaItemIndex < player.mediaItemCount) {
                            player.seekTo(playerState.currentMediaItemIndex, playerState.currentPosition)
                        }
                    }
                }.onFailure { error ->
                    Timber.tag(TAG).w(error, "Failed to read player state, clearing data")
                    clearPersistedQueueFiles()
                }
            }
        }

        scope.launch {
            while (isActive) {
                delay(30.seconds)
                if (persistentQueueEnabled) {
                    saveQueueToDisk()
                }
                val currentMetadata = player.currentMediaItem?.metadata
                if (currentMetadata?.isEpisode == true && player.isPlaying && player.currentPosition > 0) {
                    previousEpisodePosition = player.currentPosition
                    saveEpisodePosition(currentMetadata.id, player.currentPosition)
                }
            }
        }
    }

    private fun createExoPlayer(): ExoPlayer {
        val eqProcessor = CustomEqualizerAudioProcessor()
        equalizerService.addAudioProcessor(eqProcessor)

        val silenceProcessor = SilenceDetectorAudioProcessor { handleLongSilenceDetected() }
        scratchProcessor = ScratchAudioProcessor()
        val visualizerTap = com.metrolist.music.playback.audio.VisualizerTapAudioProcessor(audioBandLevelsFlow)

        runBlocking {
            val skipSilence = dataStore.get(SkipSilenceKey, false)
            val instantSkip = dataStore.get(SkipSilenceInstantKey, false)
            silenceProcessor.instantModeEnabled = skipSilence && instantSkip
        }

        val player =
            ExoPlayer
                .Builder(this)
                .setMediaSourceFactory(createMediaSourceFactory())
                .setRenderersFactory(createRenderersFactory(eqProcessor, silenceProcessor, scratchProcessor, visualizerTap))
                .setHandleAudioBecomingNoisy(true)
                .setWakeMode(C.WAKE_MODE_NETWORK)
                .setAudioAttributes(
                    AudioAttributes
                        .Builder()
                        .setUsage(C.USAGE_MEDIA)
                        .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                        .build(),
                    false,
                ).setSeekBackIncrementMs(5000)
                .setSeekForwardIncrementMs(5000)
                .setDeviceVolumeControlEnabled(true)
                .build()

        playerSilenceProcessors[player] = silenceProcessor

        player.apply {
            runBlocking {
                val offload = dataStore.get(AudioOffload, false)
                val crossfade = dataStore.get(CrossfadeEnabledKey, false)
                setOffloadEnabled(if (crossfade) false else offload)
                skipSilenceEnabled = dataStore.get(SkipSilenceKey, false)
            }
            addAnalyticsListener(PlaybackStatsListener(false, this@MusicService))

        }
        return player
    }

    private fun setupAudioFocusRequest() {
        audioFocusRequest =
            AudioFocusRequest
                .Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setAudioAttributes(
                    android.media.AudioAttributes
                        .Builder()
                        .setUsage(android.media.AudioAttributes.USAGE_MEDIA)
                        .setContentType(android.media.AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build(),
                ).setOnAudioFocusChangeListener { focusChange ->
                    handleAudioFocusChange(focusChange)
                }.setAcceptsDelayedFocusGain(true)
                .build()
    }

    private fun handleAudioFocusChange(focusChange: Int) {
        when (focusChange) {
            AudioManager.AUDIOFOCUS_GAIN,
            AudioManager.AUDIOFOCUS_GAIN_TRANSIENT,
                -> {
                hasAudioFocus = true
                audioFocusVolumeMultiplier.value = 1f

                if (wasPlayingBeforeAudioFocusLoss && !player.isPlaying && !reentrantFocusGain) {
                    reentrantFocusGain = true
                    scope.launch {
                        delay(300)
                        if (hasAudioFocus && wasPlayingBeforeAudioFocusLoss && !player.isPlaying) {
                            if (castConnectionHandler?.isCasting?.value != true) {
                                player.play()
                            }
                            wasPlayingBeforeAudioFocusLoss = false
                        }
                        reentrantFocusGain = false
                    }
                }

                applyEffectiveVolume()
                lastAudioFocusState = focusChange
            }

            AudioManager.AUDIOFOCUS_LOSS -> {
                hasAudioFocus = false
                audioFocusVolumeMultiplier.value = 1f
                wasPlayingBeforeAudioFocusLoss = player.isPlaying
                if (player.isPlaying) {
                    player.pause()
                }
                abandonAudioFocus()
                lastAudioFocusState = focusChange
            }

            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                hasAudioFocus = false
                audioFocusVolumeMultiplier.value = 1f
                wasPlayingBeforeAudioFocusLoss = player.isPlaying
                if (player.isPlaying) {
                    player.pause()
                }
                lastAudioFocusState = focusChange
            }

            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                hasAudioFocus = false
                audioFocusVolumeMultiplier.value = 0.2f
                wasPlayingBeforeAudioFocusLoss = player.isPlaying
                if (player.isPlaying) {
                    applyEffectiveVolume()
                }
                lastAudioFocusState = focusChange
            }

            AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK -> {
                hasAudioFocus = true
                audioFocusVolumeMultiplier.value = 1f
                applyEffectiveVolume()
                lastAudioFocusState = focusChange
            }
        }
    }

    private fun requestAudioFocus(): Boolean {
        if (hasAudioFocus) return true

        audioFocusRequest?.let { request ->
            val result = audioManager.requestAudioFocus(request)
            hasAudioFocus = result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
            return hasAudioFocus
        }
        return false
    }

    private fun abandonAudioFocus() {
        if (hasAudioFocus) {
            audioFocusRequest?.let { request ->
                audioManager.abandonAudioFocusRequest(request)
                hasAudioFocus = false
            }
        }
    }

    private fun clearPersistedQueueFiles() {
        runCatching { filesDir.resolve(PERSISTENT_QUEUE_FILE).delete() }
        runCatching { filesDir.resolve(PERSISTENT_AUTOMIX_FILE).delete() }
        runCatching { filesDir.resolve(PERSISTENT_PLAYER_STATE_FILE).delete() }
    }

    fun hasAudioFocusForPlayback(): Boolean = hasAudioFocus

    private fun waitOnNetworkError() {
        if (waitingForNetworkConnection.value) return

        if (retryCount >= MAX_RETRY_COUNT) {
            Timber.tag(TAG).w("Max retry count ($MAX_RETRY_COUNT) reached, stopping playback")
            stopOnError()
            retryCount = 0
            return
        }

        waitingForNetworkConnection.value = true

        retryJob?.cancel()
        retryJob =
            scope.launch {
                val delayMs = minOf(3000L * (1 shl retryCount), 30000L)
                Timber.tag(TAG).d("Waiting ${delayMs}ms before retry attempt ${retryCount + 1}/$MAX_RETRY_COUNT")
                delay(delayMs)

                if (isNetworkConnected.value && waitingForNetworkConnection.value) {
                    retryCount++
                    triggerRetry()
                }
            }
    }

    private fun triggerRetry() {
        waitingForNetworkConnection.value = false
        retryJob?.cancel()

        if (player.currentMediaItem != null) {
            if (retryCount > 3) {
                Timber.tag(TAG).d("Retry count > 3, attempting to refresh stream URL")
                val currentPosition = player.currentPosition
                player.seekTo(player.currentMediaItemIndex, currentPosition)
            }
            player.prepare()
        }
    }

    private fun skipOnError() {
        /**
         * Auto skip to the next media item on error.
         *
         * To prevent a "runaway diesel engine" scenario, force the user to take action after
         * too many errors come up too quickly. Pause to show player "stopped" state
         */
        consecutivePlaybackErr += 2
        val nextWindowIndex = player.nextMediaItemIndex

        if (consecutivePlaybackErr <= MAX_CONSECUTIVE_ERR && nextWindowIndex != C.INDEX_UNSET) {
            player.seekTo(nextWindowIndex, C.TIME_UNSET)
            player.prepare()
            if (castConnectionHandler?.isCasting?.value != true) {
                player.play()
            }
            return
        }

        player.pause()
        consecutivePlaybackErr = 0
    }

    private fun stopOnError() {
        player.pause()
    }

    private fun updateNotification() {
        mediaSession.setCustomLayout(
            listOf(
                CommandButton.Builder()
                    .setDisplayName(
                        getString(
                            if (currentSong.value?.song?.liked == true) {
                                R.string.action_remove_like
                            } else {
                                R.string.action_like
                            },
                        ),
                    )
                    .setIconResId(if (currentSong.value?.song?.liked == true) R.drawable.ic_widget_star_nav else R.drawable.ic_widget_star_outline_nav)
                    .setSessionCommand(CommandToggleLike)
                    .setEnabled(currentSong.value != null)
                    .build(),
                CommandButton.Builder()
                    .setDisplayName(getString(if (player.shuffleModeEnabled) R.string.action_shuffle_off else R.string.action_shuffle_on))
                    .setIconResId(if (player.shuffleModeEnabled) R.drawable.shuffle_on else R.drawable.shuffle)
                    .setSessionCommand(CommandToggleShuffle)
                    .build(),
                CommandButton.Builder()
                    .setDisplayName(getString(R.string.start_radio))
                    .setIconResId(R.drawable.radio)
                    .setSessionCommand(CommandToggleStartRadio)
                    .setEnabled(currentSong.value != null)
                    .build(),
                CommandButton.Builder()
                    .setDisplayName(getString(R.string.android_auto_target_playlist))
                    .setIconResId(R.drawable.playlist_add)
                    .setSessionCommand(CommandAddToTargetPlaylist)
                    .setEnabled(currentSong.value != null)
                    .build(),
            ),
        )
    }

    private suspend fun recoverSong(
        mediaId: String,
        playbackData: YTPlayerUtils.PlaybackData? = null,
    ) {
        val song = database.song(mediaId).first()
        val mediaMetadata =
            withContext(Dispatchers.Main) {
                player.findNextMediaItemById(mediaId)?.metadata
            } ?: return
        val duration =
            song?.song?.duration?.takeIf { it != -1 }
                ?: mediaMetadata.duration.takeIf { it != -1 }
                ?: (
                        playbackData?.videoDetails ?: YTPlayerUtils
                            .playerResponseForMetadata(mediaId)
                            .getOrNull()
                            ?.videoDetails
                        )?.lengthSeconds?.toInt()
                ?: -1
        database.query {
            if (song == null) {
                insert(mediaMetadata.copy(duration = duration))
            } else {
                var updatedSong = song.song
                if (song.song.duration == -1) {
                    updatedSong = updatedSong.copy(duration = duration)
                }
                if (song.song.isVideo != mediaMetadata.isVideoSong) {
                    updatedSong = updatedSong.copy(isVideo = mediaMetadata.isVideoSong)
                }
                if (updatedSong != song.song) {
                    update(updatedSong)
                }
            }
        }
        if (!database.hasRelatedSongs(mediaId)) {
            val relatedEndpoint =
                YouTube.next(WatchEndpoint(videoId = mediaId)).getOrNull()?.relatedEndpoint
                    ?: return
            val relatedPage = YouTube.related(relatedEndpoint).getOrNull() ?: return
            database.query {
                relatedPage.songs
                    .map(SongItem::toMediaMetadata)
                    .onEach(::insert)
                    .map {
                        RelatedSongMap(
                            songId = mediaId,
                            relatedSongId = it.id,
                        )
                    }.forEach(::insert)
            }
        }
    }

    fun playQueue(
        queue: Queue,
        playWhenReady: Boolean = true,
    ) {
        if (!playerInitialized.value) {
            Timber.tag(TAG).w("playQueue called before player initialization, queuing request")
            scope.launch {
                playerInitialized.first { it }
                playQueue(queue, playWhenReady)
            }
            return
        }

        currentQueue = queue
        queueTitle = null
        clearRadioState()
        // A brand-new queue replaces everything: any pre-radio snapshot is stale.
        preRadioUpcoming = null
        preRadioQueue = EmptyQueue
        preRadioTitle = null
        val persistShuffleAcrossQueues = dataStore.get(PersistentShuffleAcrossQueuesKey, false)
        val previousShuffleEnabled = player.shuffleModeEnabled
        if (!persistShuffleAcrossQueues) {
            player.shuffleModeEnabled = false
        }
        originalQueueSize = 0
        queue.preloadItem?.let { preloadItem ->
            player.setMediaItem(preloadItem.toMediaItem())
            player.prepare()
            player.playWhenReady = playWhenReady
        }
        scope.launch(SilentHandler) {
            val initialStatus =
                withContext(Dispatchers.IO) {
                    queue
                        .getInitialStatus()
                        .filterExplicit(dataStore.get(HideExplicitKey, false))
                        .filterVideoSongs(dataStore.get(HideVideoSongsKey, false))
                }
            if (queue.preloadItem != null && player.playbackState == STATE_IDLE) return@launch
            if (initialStatus.title != null) {
                queueTitle = initialStatus.title
            }
            if (initialStatus.items.isEmpty()) return@launch
            originalQueueSize = initialStatus.items.size
            if (queue.preloadItem != null) {
                player.addMediaItems(
                    0,
                    initialStatus.items.subList(0, initialStatus.mediaItemIndex),
                )
                player.addMediaItems(
                    initialStatus.items.subList(
                        initialStatus.mediaItemIndex + 1,
                        initialStatus.items.size,
                    ),
                )
            } else {
                player.setMediaItems(
                    initialStatus.items,
                    if (initialStatus.mediaItemIndex >
                        0
                    ) {
                        initialStatus.mediaItemIndex
                    } else {
                        0
                    },
                    initialStatus.position,
                )
                player.prepare()
                player.playWhenReady = playWhenReady
            }

            if (player.shuffleModeEnabled) {
                val shufflePlaylistFirst = shufflePlaylistFirst
                applyShuffleOrder(player.currentMediaItemIndex, player.mediaItemCount, shufflePlaylistFirst)
            }
        }
    }

    fun startRadioSeamlessly() {
        if (!playerInitialized.value) {
            Timber.tag(TAG).w("startRadioSeamlessly called before player initialization")
            return
        }

        // Unified stable radio: same RDAMVM backend as the 3-dots menu.
        // Keeps the current song, replaces everything after it.
        // Snapshot once: if the radio is already on, the current upcoming list
        // is already radio content, so keep the original snapshot for restore.
        val currentMediaMetadata = player.currentMetadata ?: return

        val currentIndex = player.currentMediaItemIndex
        val currentMediaId = currentMediaMetadata.id
        if (!isAutoMixQueueActive.value) {
            val upcoming = mutableListOf<MediaItem>()
            val count = player.mediaItemCount
            var i = currentIndex + 1
            while (i < count) {
                upcoming.add(player.getMediaItemAt(i))
                i++
            }
            preRadioUpcoming = upcoming
            preRadioQueue = currentQueue
            preRadioTitle = queueTitle
        }

        clearRadioState()

        synchronized(automixLock) { radioSeedId = currentMediaId }
        isAutoMixQueueActive.value = true

        scope.launch(SilentHandler) {
            val radioQueue =
                YouTubeQueue(
                    endpoint =
                        WatchEndpoint(
                            videoId = currentMediaId,
                        ),
                )

            try {
                val initialStatus =
                    withContext(Dispatchers.IO) {
                        radioQueue
                            .getInitialStatus()
                            .filterExplicit(dataStore.get(HideExplicitKey, false))
                            .filterVideoSongs(dataStore.get(HideVideoSongsKey, false))
                    }

                // The server title for a mix is a generic word like "Radio":
                // qualify it with the seed song so the queue header can never
                // be mistaken for a track called "Radio".
                val radioLabel = getString(R.string.radio)
                val serverTitle = initialStatus.title
                queueTitle =
                    if (serverTitle.isNullOrBlank() || serverTitle.equals(radioLabel, ignoreCase = true)) {
                        "$radioLabel • ${currentMediaMetadata.title}"
                    } else {
                        serverTitle
                    }

                val radioItems =
                    initialStatus.items.filter { item ->
                        item.mediaId != currentMediaId
                    }

                if (radioItems.isNotEmpty()) {
                    val itemCount = player.mediaItemCount

                    if (itemCount > currentIndex + 1) {
                        player.removeMediaItems(currentIndex + 1, itemCount)
                    }

                    player.addMediaItems(currentIndex + 1, radioItems)
                    if (player.shuffleModeEnabled) {
                        val shufflePlaylistFirst = shufflePlaylistFirst
                        applyShuffleOrder(player.currentMediaItemIndex, player.mediaItemCount, shufflePlaylistFirst)
                    }
                }

                currentQueue = radioQueue
                isAutoMixQueueActive.value = true
            } catch (e: Exception) {
                try {
                    val nextResult =
                        withContext(Dispatchers.IO) {
                            YouTube.next(WatchEndpoint(videoId = currentMediaId)).getOrNull()
                        }
                    nextResult?.relatedEndpoint?.let { relatedEndpoint ->
                        val relatedPage =
                            withContext(Dispatchers.IO) {
                                YouTube.related(relatedEndpoint).getOrNull()
                            }
                        relatedPage?.songs?.let { songs ->
                            val radioItems =
                                songs
                                    .filter { it.id != currentMediaId }
                                    .map { it.toMediaItem() }
                                    .filterExplicit(dataStore.get(HideExplicitKey, false))
                                    .filterVideoSongs(dataStore.get(HideVideoSongsKey, false))

                            if (radioItems.isNotEmpty()) {
                                val itemCount = player.mediaItemCount
                                if (itemCount > currentIndex + 1) {
                                    player.removeMediaItems(currentIndex + 1, itemCount)
                                }
                                player.addMediaItems(currentIndex + 1, radioItems)
                                if (player.shuffleModeEnabled) {
                                    val shufflePlaylistFirst = shufflePlaylistFirst
                                    applyShuffleOrder(
                                        player.currentMediaItemIndex,
                                        player.mediaItemCount,
                                        shufflePlaylistFirst,
                                    )
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    Timber.tag(TAG).w(e, "Failed to extend queue with radio songs")
                }
            }
        }
    }

    fun startRadioForSong(mediaMetadata: MediaMetadata) {
        if (!playerInitialized.value) {
            Timber.tag(TAG).w("startRadioForSong called before player initialization")
            return
        }

        playQueue(YouTubeQueue.radio(mediaMetadata))
        // playQueue() clears the flag synchronously, re-arm it for the new radio queue.
        synchronized(automixLock) { radioSeedId = mediaMetadata.id }
        isAutoMixQueueActive.value = true
    }

    suspend fun regenerateAutomix(mediaMetadata: MediaMetadata) {
        // Legacy virtual branch removed: player radio now uses the stable
        // startRadioSeamlessly() backend. Kept as no-op for compatibility.
    }

    fun commitAutomixAsQueue() {
        // Legacy virtual branch removed: nothing to commit, the stable radio
        // already lives in the real timeline. Kept as no-op for compatibility.
    }

    fun clearRadioState() {
        automixRequestId.incrementAndGet()
        synchronized(automixLock) { radioSeedId = null }
        isAutoMixQueueActive.value = false
        automixItems.value = emptyList()
        automixEnsuredAtIndex = C.INDEX_UNSET
        automixWatchJob?.cancel()
        automixWatchJob = null
        removeAppendedRadioItems()
    }

    /**
     * Removes the radio tracks that were auto-appended to the timeline while the radio was
     * active. The currently playing item is always kept so playback is never interrupted.
     */
    private fun removeAppendedRadioItems() {
        if (!playerInitialized.value || appendedRadioMediaIds.isEmpty()) return
        val currentIndex = player.currentMediaItemIndex
        var removedAny = false
        for (i in player.mediaItemCount - 1 downTo currentIndex + 1) {
            if (player.getMediaItemAt(i).mediaId in appendedRadioMediaIds) {
                player.removeMediaItem(i)
                removedAny = true
            }
        }
        appendedRadioMediaIds.clear()
        rebuildPromotedRadioItems()
        if (removedAny && player.shuffleModeEnabled) {
            applyShuffleOrder(player.currentMediaItemIndex, player.mediaItemCount, shufflePlaylistFirst)
        }
    }

    /**
     * Re-derives [promotedRadioItems] from the live timeline in playback order, scoped to
     * tracks after the currently playing one. Called whenever [appendedRadioMediaIds]
     * changes (promotion, removal, clear) so the UI radio section stays in lockstep with
     * what the player is about to actually play.
     */
    private fun rebuildPromotedRadioItems() {
        if (!playerInitialized.value || appendedRadioMediaIds.isEmpty()) {
            promotedRadioItems.value = emptyList()
            return
        }
        val currentIndex = player.currentMediaItemIndex
        val out = mutableListOf<MediaItem>()
        for (i in (currentIndex + 1) until player.mediaItemCount) {
            val item = player.getMediaItemAt(i)
            if (item.mediaId in appendedRadioMediaIds) out.add(item)
        }
        promotedRadioItems.value = out
    }

    /**
     * Explicitly starts the (non-persistent) radio for the given song: the generated
     * songs stay in [automixItems] as a virtual branch — displayed right under the current
     * track — and are never committed into the player timeline. Calling [clearRadioState]
     * (toggle off, new content, …) stops it.
     */
    fun startAutoMixRadio(mediaMetadata: MediaMetadata) {
        // Legacy virtual branch removed: unified on the stable RDAMVM backend.
        // Keep the entry point for compatibility, behave like the player toggle.
        if (!playerInitialized.value) return
        startRadioSeamlessly()
    }

    /**
     * Single debounced entry point for the player radio toggle (MP3 wheel, queue
     * pill, …). Unified on the stable RDAMVM backend: ON = seamless radio from
     * the current song (same list as the 3-dots menu), OFF = stop infinite
     * continuation but keep what is already queued.
     *
     * @return true when the radio is ON after this call.
     */
    fun toggleRadio(mediaMetadata: MediaMetadata): Boolean {
        if (!playerInitialized.value) return false
        synchronized(automixLock) {
            val now = System.currentTimeMillis()
            if (now - lastRadioToggleAt < RADIO_TOGGLE_DEBOUNCE_MS) {
                return isAutoMixQueueActive.value
            }
            lastRadioToggleAt = now
        }
        return if (isAutoMixQueueActive.value && radioSeedId == mediaMetadata.id) {
            stopRadioAndRestore()
            false
        } else {
            startRadioSeamlessly()
            true
        }
    }

    /**
     * Turns the stable radio off and puts back exactly the upcoming queue that
     * was there before the radio was armed (same songs, same order, same
     * continuation and title). The currently playing song is never touched.
     * If there is nothing to restore (radio started fresh from a menu), it
     * only stops the infinite continuation and keeps the queued songs.
     */
    fun stopRadioAndRestore() {
        val snapshot = preRadioUpcoming
        val resumeQueue = preRadioQueue
        val resumeTitle = preRadioTitle
        val currentIndex =
            if (playerInitialized.value) player.currentMediaItemIndex else C.INDEX_UNSET
        clearRadioState()
        currentQueue = EmptyQueue
        if (snapshot != null && currentIndex != C.INDEX_UNSET) {
            val itemCount = player.mediaItemCount
            if (itemCount > currentIndex + 1) {
                player.removeMediaItems(currentIndex + 1, itemCount)
            }
            if (snapshot.isNotEmpty()) {
                player.addMediaItems(currentIndex + 1, snapshot)
            }
            if (player.shuffleModeEnabled) {
                applyShuffleOrder(player.currentMediaItemIndex, player.mediaItemCount, shufflePlaylistFirst)
            }
            currentQueue = resumeQueue
            queueTitle = resumeTitle
        }
    }

    fun getAutomixAlbum(albumId: String) {
        // Legacy virtual branch removed: album play must not resurrect radio.
    }

    fun getAutomix(playlistId: String) {
        // Legacy virtual branch removed: this used to refill automixItems AFTER
        // playQueue() cleared them, resurrecting radio on album change. No-op now.
    }

    fun addToQueueAutomix(
        item: MediaItem,
        position: Int,
    ) {
        // Legacy virtual branch removed: behave like a normal queue append.
        addToQueue(listOf(item))
    }

    fun playNextAutomix(
        item: MediaItem,
        position: Int,
    ) {
        // Legacy virtual branch removed: behave like a normal play-next.
        playNext(listOf(item))
    }

    /**
     * Legacy virtual promotion removed: stable radio lives directly in the
     * timeline via startRadioSeamlessly(). Kept as no-op for compatibility.
     */
    fun ensureAutomixUpNext() {
    }

    /**
     * Pins a timeline index right after the current item in the shuffle order: the
     * current item stays first, [headIndex] becomes second (i.e. what plays next),
     * everything else is shuffled. Used for radio promotions so the audio always
     * follows the Up Next radio section, even with shuffle on.
     */
    private fun pinRadioHeadNext(headIndex: Int) {
        val totalCount = player.mediaItemCount
        val currentIndex = player.currentMediaItemIndex
        if (totalCount == 0 || headIndex !in 0 until totalCount ||
            currentIndex !in 0 until totalCount || headIndex == currentIndex
        ) return
        val rest = (0 until totalCount).filter { it != currentIndex && it != headIndex }.shuffled()
        val order = IntArray(totalCount)
        var pos = 0
        order[pos++] = currentIndex
        order[pos++] = headIndex
        rest.forEach { order[pos++] = it }
        player.setShuffleOrder(DefaultShuffleOrder(order, System.currentTimeMillis()))
    }

    /**
     * Legacy virtual refill removed: stable radio continues via currentQueue
     * pagination in onMediaItemTransition. Kept as no-op for compatibility.
     */
    private fun extendAutomix() {
    }

    fun reorderAutomix(fromIndex: Int, toIndex: Int) {
    }

    fun insertIntoAutomix(item: MediaItem, index: Int) {
    }

    fun removeFromAutomix(mediaId: String) {
    }

    /**
     * Legacy virtual reserve removed: stable radio lives in the timeline.
     * Kept as no-op for compatibility.
     */
    fun applyAutomixOrder(items: List<MediaItem>) {
    }

    /**
     * Legacy virtual reserve removed: no promotion to notify.
     * Kept as no-op for compatibility.
     */
    fun notifyAutomixChanged() {
    }

    /** Stable radio lives in the timeline: nothing is virtually promoted. */
    fun isRadioPromoted(mediaId: String): Boolean = false

    fun promoteAutomixToQueue(item: MediaItem, insertionIndex: Int) {
        if (!playerInitialized.value) return
        player.addMediaItem(insertionIndex.coerceAtMost(player.mediaItemCount), item)
    }

    fun clearAutomix() {
        automixItems.value = emptyList()
    }

    fun playNext(items: List<MediaItem>) {
        if (player.mediaItemCount == 0 || player.playbackState == STATE_IDLE) {
            player.setMediaItems(items)
            player.prepare()
            if (castConnectionHandler?.isCasting?.value != true) {
                player.play()
            }
            return
        }

        if (dataStore.get(PreventDuplicateTracksInQueueKey, false)) {
            val itemIds = items.map { it.mediaId }.toSet()
            val indicesToRemove = mutableListOf<Int>()
            val currentIndex = player.currentMediaItemIndex

            for (i in 0 until player.mediaItemCount) {
                if (i != currentIndex && player.getMediaItemAt(i).mediaId in itemIds) {
                    indicesToRemove.add(i)
                }
            }

            indicesToRemove.sortedDescending().forEach { index ->
                player.removeMediaItem(index)
            }
        }

        val insertIndex = player.currentMediaItemIndex + 1
        val shuffleEnabled = player.shuffleModeEnabled

        player.addMediaItems(insertIndex, items)
        player.prepare()

        if (shuffleEnabled) {
            val timeline = player.currentTimeline
            if (!timeline.isEmpty) {
                val size = timeline.windowCount
                val currentIndex = player.currentMediaItemIndex

                val newIndices = (insertIndex until (insertIndex + items.size)).toSet()

                val orderAfter = mutableListOf<Int>()
                var idx = currentIndex
                while (true) {
                    idx = timeline.getNextWindowIndex(idx, Player.REPEAT_MODE_OFF, true)
                    if (idx == C.INDEX_UNSET) break
                    if (idx != currentIndex) orderAfter.add(idx)
                }

                val prevList = mutableListOf<Int>()
                var pIdx = currentIndex
                while (true) {
                    pIdx = timeline.getPreviousWindowIndex(pIdx, Player.REPEAT_MODE_OFF, true)
                    if (pIdx == C.INDEX_UNSET) break
                    if (pIdx != currentIndex) prevList.add(pIdx)
                }
                prevList.reverse()

                val existingOrder = (prevList + orderAfter).filter { it != currentIndex && it !in newIndices }

                val nextBlock = (insertIndex until (insertIndex + items.size)).toList()
                val finalOrder = IntArray(size)
                var pos = 0
                finalOrder[pos++] = currentIndex
                nextBlock.forEach { if (it in 0 until size) finalOrder[pos++] = it }
                existingOrder.forEach { if (pos < size) finalOrder[pos++] = it }

                if (pos < size) {
                    for (i in 0 until size) {
                        if (!finalOrder.contains(i)) {
                            finalOrder[pos++] = i
                            if (pos == size) break
                        }
                    }
                }

                player.setShuffleOrder(DefaultShuffleOrder(finalOrder, System.currentTimeMillis()))
            }
        }
    }

    fun addToQueue(items: List<MediaItem>) {
        if (dataStore.get(PreventDuplicateTracksInQueueKey, false)) {
            val itemIds = items.map { it.mediaId }.toSet()
            val indicesToRemove = mutableListOf<Int>()
            val currentIndex = player.currentMediaItemIndex

            for (i in 0 until player.mediaItemCount) {
                if (i != currentIndex && player.getMediaItemAt(i).mediaId in itemIds) {
                    indicesToRemove.add(i)
                }
            }

            indicesToRemove.sortedDescending().forEach { index ->
                player.removeMediaItem(index)
            }
        }

        player.addMediaItems(items)
        if (player.shuffleModeEnabled) {
            val shufflePlaylistFirst = shufflePlaylistFirst
            applyShuffleOrder(player.currentMediaItemIndex, player.mediaItemCount, shufflePlaylistFirst)
        }
        player.prepare()
    }

    fun toggleLibrary() {
        scope.launch {
            val songToToggle = currentSong.first()
            songToToggle?.let {
                val isInLibrary = it.song.inLibrary != null
                val token = if (isInLibrary) it.song.libraryRemoveToken else it.song.libraryAddToken

                token?.let { feedbackToken ->
                    YouTube.feedback(listOf(feedbackToken))
                }

                database.query {
                    update(it.song.toggleLibrary())
                }
                currentMediaMetadata.value = player.currentMetadata
            }
        }
    }

    fun toggleLike() {
        scope.launch {
            val songToToggle = currentSong.first()
            songToToggle?.let { librarySong ->
                val songEntity = librarySong.song

                if (songEntity.isEpisode) {
                    toggleEpisodeSaveForLater(songEntity)
                    return@let
                }

                val song = songEntity.toggleLike()
                database.query {
                    update(song)

                    if (dataStore.get(AutoDownloadOnLikeKey, false) && song.liked) {
                        val downloadRequest =
                            androidx.media3.exoplayer.offline.DownloadRequest
                                .Builder(song.id, song.id.toUri())
                                .setCustomCacheKey(song.id)
                                .setData(song.title.toByteArray())
                                .build()
                        androidx.media3.exoplayer.offline.DownloadService.sendAddDownload(
                            this@MusicService,
                            ExoDownloadService::class.java,
                            downloadRequest,
                            false,
                        )
                    }
                }
                updateWidgetUI(player.isPlaying)
                syncUtils.likeSong(song)
                currentMediaMetadata.value = player.currentMetadata
            }
        }
    }

    fun addToTargetPlaylist() {
        scope.launch {
            val currentSong = currentSong.first() ?: return@launch
            val targetPlaylistId = dataStore.get(AndroidAutoTargetPlaylistKey, MediaSessionConstants.TARGET_PLAYLIST_AUTO)

            if (targetPlaylistId == MediaSessionConstants.TARGET_PLAYLIST_AUTO) {
                Handler(Looper.getMainLooper()).post {
                    Toast
                        .makeText(
                            this@MusicService,
                            getString(R.string.android_auto_target_playlist_not_set),
                            Toast.LENGTH_SHORT,
                        ).show()
                }
                return@launch
            }

            database.query {
                insert(
                    com.metrolist.music.db.entities.PlaylistSongMap(
                        playlistId = targetPlaylistId,
                        songId = currentSong.id,
                        position = Int.MAX_VALUE,
                    ),
                )
            }
        }
    }

    private suspend fun toggleEpisodeSaveForLater(songEntity: com.metrolist.music.db.entities.SongEntity) {
        val isCurrentlySaved = songEntity.inLibrary != null
        val shouldBeSaved = !isCurrentlySaved

        database.query {
            update(
                songEntity.copy(
                    inLibrary = if (isCurrentlySaved) null else java.time.LocalDateTime.now(),
                    isEpisode = true,
                ),
            )
        }
        currentMediaMetadata.value = player.currentMetadata

        val setVideoId = if (isCurrentlySaved) database.getSetVideoId(songEntity.id)?.setVideoId else null
        syncUtils.saveEpisode(songEntity.id, shouldBeSaved, setVideoId)
    }

    fun toggleStartRadio() {
        startRadioSeamlessly()
    }

    private fun setupLoudnessEnhancer() {
        val audioSessionId = player.audioSessionId

        if (audioSessionId == C.AUDIO_SESSION_ID_UNSET || audioSessionId <= 0) {
            Timber
                .tag(TAG)
                .w("setupLoudnessEnhancer: invalid audioSessionId ($audioSessionId), cannot create effect yet")
            return
        }

        if (loudnessEnhancer == null) {
            try {
                loudnessEnhancer = LoudnessEnhancer(audioSessionId)
                Timber.tag(TAG).d("LoudnessEnhancer created for sessionId=$audioSessionId")
            } catch (e: Exception) {
                reportException(e)
                loudnessEnhancer = null
                return
            }
        }

        scope.launch {
            try {
                val currentMediaId =
                    withContext(Dispatchers.Main) {
                        player.currentMediaItem?.mediaId
                    }

                val normalizeAudio =
                    withContext(Dispatchers.IO) {
                        dataStore.data.map { it[AudioNormalizationKey] ?: true }.first()
                    }

                if (normalizeAudio && currentMediaId != null) {
                    val format =
                        withContext(Dispatchers.IO) {
                            database.format(currentMediaId).first()
                        }

                    Timber.tag(TAG).d("Audio normalization enabled: $normalizeAudio")
                    Timber
                        .tag(TAG)
                        .d("Format loudnessDb: ${format?.loudnessDb}, perceptualLoudnessDb: ${format?.perceptualLoudnessDb}")

                    val loudness = format?.loudnessDb ?: format?.perceptualLoudnessDb

                    withContext(Dispatchers.Main) {
                        if (loudness != null) {
                            val loudnessDb = loudness.toFloat()
                            val targetGain = (-loudnessDb * 100).toInt()
                            val clampedGain = targetGain.coerceIn(MIN_GAIN_MB, MAX_GAIN_MB)

                            Timber
                                .tag(TAG)
                                .d("Calculated raw normalization gain: $targetGain mB (from loudness: $loudnessDb)")

                            try {
                                loudnessEnhancer?.setTargetGain(clampedGain)
                                loudnessEnhancer?.enabled = true
                                Timber.tag(TAG).i("LoudnessEnhancer gain applied: $clampedGain mB")
                            } catch (e: Exception) {
                                Timber.tag(TAG).e(e, "Failed to apply loudness enhancement")
                                reportException(e)
                                releaseLoudnessEnhancer()
                            }
                        } else {
                            loudnessEnhancer?.enabled = false
                            Timber
                                .tag(TAG)
                                .w("Normalization enabled but no loudness data available - no normalization applied")
                        }
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        loudnessEnhancer?.enabled = false
                        Timber.tag(TAG).d("setupLoudnessEnhancer: normalization disabled or mediaId unavailable")
                    }
                }
            } catch (e: Exception) {
                reportException(e)
                releaseLoudnessEnhancer()
            }
        }
    }

    private fun releaseLoudnessEnhancer() {
        try {
            loudnessEnhancer?.release()
            Timber.tag(TAG).d("LoudnessEnhancer released")
        } catch (e: Exception) {
            reportException(e)
            Timber.tag(TAG).e(e, "Error releasing LoudnessEnhancer: ${e.message}")
        } finally {
            loudnessEnhancer = null
        }
    }

    private fun openAudioEffectSession() {
        if (isAudioEffectSessionOpened) return
        isAudioEffectSessionOpened = true
        setupLoudnessEnhancer()
        sendBroadcast(
            Intent(AudioEffect.ACTION_OPEN_AUDIO_EFFECT_CONTROL_SESSION).apply {
                putExtra(AudioEffect.EXTRA_AUDIO_SESSION, player.audioSessionId)
                putExtra(AudioEffect.EXTRA_PACKAGE_NAME, packageName)
                putExtra(AudioEffect.EXTRA_CONTENT_TYPE, AudioEffect.CONTENT_TYPE_MUSIC)
            },
        )
    }

    private fun closeAudioEffectSession() {
        if (!isAudioEffectSessionOpened) return
        isAudioEffectSessionOpened = false
        releaseLoudnessEnhancer()
        sendBroadcast(
            Intent(AudioEffect.ACTION_CLOSE_AUDIO_EFFECT_CONTROL_SESSION).apply {
                putExtra(AudioEffect.EXTRA_AUDIO_SESSION, player.audioSessionId)
                putExtra(AudioEffect.EXTRA_PACKAGE_NAME, packageName)
            },
        )
    }

    private var previousMediaItemIndex = C.INDEX_UNSET
    private var previousEpisodeId: String? = null
    private var previousEpisodePosition: Long = 0L

    /**
     * Save podcast episode playback position to database.
     * Only saves if the item is an episode and position is meaningful (> 3 seconds).
     */
    private fun saveEpisodePosition(
        episodeId: String,
        positionMs: Long,
    ) {
        if (positionMs < 3000) return
        scope.launch(Dispatchers.IO + SilentHandler) {
            database.updatePlaybackPosition(episodeId, positionMs)
            Timber.tag(TAG).d("Saved episode position: $episodeId at ${positionMs}ms")
        }
    }

    /**
     * Restore podcast episode playback position from database.
     * Seeks to saved position if available.
     */
    private fun restoreEpisodePosition(episodeId: String) {
        scope.launch(Dispatchers.IO + SilentHandler) {
            val savedPosition = database.getPlaybackPosition(episodeId)
            if (savedPosition != null && savedPosition > 0) {
                withContext(Dispatchers.Main) {
                    if (player.currentMediaItem?.mediaId == episodeId) {
                        player.seekTo(savedPosition)
                        Timber.tag(TAG).d("Restored episode position: $episodeId to ${savedPosition}ms")
                    }
                }
            }
        }
    }

    override fun onMediaItemTransition(
        mediaItem: MediaItem?,
        reason: Int,
    ) {
        previousEpisodeId?.let { episodeId ->
            if (previousEpisodePosition > 0) {
                saveEpisodePosition(episodeId, previousEpisodePosition)
            }
        }
        previousEpisodeId = null
        previousEpisodePosition = 0L

        if (::scratchProcessor.isInitialized) scratchProcessor.resetDrift()

        audioBandLevelsFlow.value = com.metrolist.music.playback.audio.AudioBandLevels()

        val newMetadata = mediaItem?.metadata
        if (newMetadata?.isEpisode == true) {
            previousEpisodeId = newMetadata.id
            scope.launch {
                delay(100)
                restoreEpisodePosition(newMetadata.id)
            }
        }

        if (reason == Player.MEDIA_ITEM_TRANSITION_REASON_AUTO) {
            val repeatMode = player.repeatMode
            if (repeatMode == REPEAT_MODE_ONE &&
                previousMediaItemIndex != C.INDEX_UNSET &&
                previousMediaItemIndex != player.currentMediaItemIndex
            ) {
                player.seekTo(previousMediaItemIndex, 0)
            }
        }
        previousMediaItemIndex = player.currentMediaItemIndex

        lastPlaybackSpeed = -1.0f

        setupLoudnessEnhancer()

        discordUpdateJob?.cancel()

        scrobbleManager?.onSongStop()
        if (player.playWhenReady && player.playbackState == Player.STATE_READY) {
            scrobbleManager?.onSongStart(player.currentMetadata, duration = player.duration)
        }

        if (castConnectionHandler?.isCasting?.value == true &&
            castConnectionHandler?.isSyncingFromCast != true &&
            mediaItem != null
        ) {
            val metadata = mediaItem.metadata
            if (metadata != null) {
                val navigated = castConnectionHandler?.navigateToMediaIfInQueue(metadata.id) ?: false
                if (!navigated) {
                    castConnectionHandler?.loadMedia(metadata)
                }
            }
        }

        if (dataStore.get(AutoLoadMoreKey, true) &&
            reason != Player.MEDIA_ITEM_TRANSITION_REASON_REPEAT &&
            player.mediaItemCount - player.currentMediaItemIndex <= 5 &&
            currentQueue.hasNextPage() &&
            !(dataStore.get(DisableLoadMoreWhenRepeatAllKey, false) && player.repeatMode == REPEAT_MODE_ALL)
        ) {
            scope.launch(SilentHandler) {
                val mediaItems =
                    withContext(Dispatchers.IO) {
                        currentQueue
                            .nextPage()
                            .filterExplicit(dataStore.get(HideExplicitKey, false))
                            .filterVideoSongs(dataStore.get(HideVideoSongsKey, false))
                    }
                if (player.playbackState != STATE_IDLE && mediaItems.isNotEmpty()) {
                    player.addMediaItems(mediaItems)
                    if (player.shuffleModeEnabled) {
                        val shufflePlaylistFirst = shufflePlaylistFirst
                        applyShuffleOrder(player.currentMediaItemIndex, player.mediaItemCount, shufflePlaylistFirst)
                    }
                }
            }
        }

        // Stable radio continues via currentQueue pagination above; the legacy
        // virtual automix branch is removed so there is nothing to consume here.

        if (persistentQueueEnabled) {
            saveQueueToDisk()
        }

        schedulePreload()
    }

    override fun onPlaybackStateChanged(
        @Player.State playbackState: Int,
    ) {
        if (playbackState == Player.STATE_READY) applyScratchBufferSize()

        if (playbackState == Player.STATE_ENDED) {
            val repeatMode = player.repeatMode
            if (repeatMode == REPEAT_MODE_ALL && player.mediaItemCount > 0) {
                player.seekTo(0, 0)
                player.prepare()
                player.play()
            }
        }

        if (persistentQueueEnabled && !isSilenceSkipping) {
            saveQueueToDisk()
        }

        if (playbackState == Player.STATE_READY) {
            consecutivePlaybackErr = 0
            retryCount = 0
            waitingForNetworkConnection.value = false
            retryJob?.cancel()

            player.currentMediaItem?.mediaId?.let { mediaId ->
                resetRetryCount(mediaId)
                Timber.tag(TAG).d("Playback successful for $mediaId, reset retry count")
            }
            scheduleCrossfade()
        }

        if (playbackState == Player.STATE_IDLE || playbackState == Player.STATE_ENDED) {
            scrobbleManager?.onSongStop()
        }
    }

    override fun onPlayWhenReadyChanged(
        playWhenReady: Boolean,
        reason: Int,
    ) {
        if (playWhenReady && castConnectionHandler?.isCasting?.value == true) {
            player.pause()
            return
        }

        if (reason == Player.PLAY_WHEN_READY_CHANGE_REASON_USER_REQUEST) {
            if (playWhenReady) {
                isPausedByVolumeMute = false
            }

            if (!playWhenReady && !isPausedByVolumeMute) {
                wasPlayingBeforeVolumeMute = false
            }
        }

        if (!playWhenReady) {
            val currentMetadata = player.currentMediaItem?.metadata
            if (currentMetadata?.isEpisode == true && player.currentPosition > 0) {
                saveEpisodePosition(currentMetadata.id, player.currentPosition)
                previousEpisodePosition = player.currentPosition
            }
        }

        if (playWhenReady) {
            setupLoudnessEnhancer()
        }
    }

    override fun onEvents(
        player: Player,
        events: Player.Events,
    ) {
        if (events.containsAny(
                Player.EVENT_PLAYBACK_STATE_CHANGED,
                Player.EVENT_PLAY_WHEN_READY_CHANGED,
            )
        ) {
            scheduleCrossfade()
            val isBufferingOrReady =
                player.playbackState == Player.STATE_BUFFERING || player.playbackState == Player.STATE_READY
            if (isBufferingOrReady && player.playWhenReady) {
                val focusGranted = requestAudioFocus()
                if (focusGranted) {
                    openAudioEffectSession()
                }
            } else {
                closeAudioEffectSession()
            }
        }
        if (events.containsAny(EVENT_TIMELINE_CHANGED, EVENT_POSITION_DISCONTINUITY)) {
            currentMediaMetadata.value = player.currentMetadata
        }

        if (events.containsAny(Player.EVENT_IS_PLAYING_CHANGED)) {
            updateWidgetUI(player.isPlaying)
            if (player.isPlaying) {
                startWidgetUpdates()
            } else {
                stopWidgetUpdates()
            }
            if (!player.isPlaying &&
                !events.containsAny(
                    Player.EVENT_POSITION_DISCONTINUITY,
                    Player.EVENT_MEDIA_ITEM_TRANSITION,
                )
            ) {
                scope.launch {
                    discordRpc?.close()
                }
            }
        }

        if (events.containsAny(
                Player.EVENT_MEDIA_ITEM_TRANSITION,
                Player.EVENT_IS_PLAYING_CHANGED,
            ) && player.isPlaying
        ) {
            val mediaId = player.currentMetadata?.id
            if (mediaId != null) {
                scope.launch {
                    database.song(mediaId).first()?.let { song ->
                        updateDiscordRPC(song)
                    }
                }
            }
        }

        if (events.containsAny(Player.EVENT_IS_PLAYING_CHANGED)) {
            scrobbleManager?.onPlayerStateChanged(player.isPlaying, player.currentMetadata, duration = player.duration)
        }
    }

    override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
        updateNotification()
        if (shuffleModeEnabled) {
            if (player.mediaItemCount == 0) return

            val currentIndex = player.currentMediaItemIndex
            val totalCount = player.mediaItemCount

            applyShuffleOrder(currentIndex, totalCount, shufflePlaylistFirst)
        }

        if (rememberShuffleAndRepeat) {
            scope.launch {
                dataStore.edit { settings ->
                    settings[ShuffleModeKey] = shuffleModeEnabled
                }
            }
        }

        if (persistentQueueEnabled) {
            saveQueueToDisk()
        }
    }

    override fun onRepeatModeChanged(repeatMode: Int) {
        updateNotification()
        scope.launch {
            dataStore.edit { settings ->
                settings[RepeatModeKey] = repeatMode
            }
        }

        if (persistentQueueEnabled) {
            saveQueueToDisk()
        }
    }

    /**
     * Applies a new shuffle order to the player, maintaining the current item's position.
     * If `shufflePlaylistFirst` is true, it attempts to shuffle original items separately from added items.
     */
    private fun applyShuffleOrder(
        currentIndex: Int,
        totalCount: Int,
        shufflePlaylistFirst: Boolean,
    ) {
        if (totalCount == 0) return

        if (shufflePlaylistFirst && originalQueueSize > 0 && originalQueueSize < totalCount) {
            val originalIndices = (0 until originalQueueSize).filter { it != currentIndex }.toMutableList()
            val addedIndices = (originalQueueSize until totalCount).filter { it != currentIndex }.toMutableList()

            originalIndices.shuffle()
            addedIndices.shuffle()

            val shuffledIndices = IntArray(totalCount)
            var pos = 0
            shuffledIndices[pos++] = currentIndex

            if (currentIndex < originalQueueSize) {
                originalIndices.forEach { shuffledIndices[pos++] = it }
                addedIndices.forEach { shuffledIndices[pos++] = it }
            } else {
                (0 until originalQueueSize).shuffled().forEach { shuffledIndices[pos++] = it }
                addedIndices.forEach { shuffledIndices[pos++] = it }
            }
            player.setShuffleOrder(DefaultShuffleOrder(shuffledIndices, System.currentTimeMillis()))
        } else {
            val shuffledIndices = IntArray(totalCount) { it }
            shuffledIndices.shuffle()
            val currentItemIndexInShuffled = shuffledIndices.indexOf(currentIndex)
            if (currentItemIndexInShuffled != -1) {
                val temp = shuffledIndices[0]
                shuffledIndices[0] = shuffledIndices[currentItemIndexInShuffled]
                shuffledIndices[currentItemIndexInShuffled] = temp
            }
            player.setShuffleOrder(DefaultShuffleOrder(shuffledIndices, System.currentTimeMillis()))
        }
    }

    override fun onPlaybackParametersChanged(playbackParameters: PlaybackParameters) {
        super.onPlaybackParametersChanged(playbackParameters)
        if (playbackParameters.speed != lastPlaybackSpeed) {
            lastPlaybackSpeed = playbackParameters.speed
            discordUpdateJob?.cancel()

            discordUpdateJob =
                scope.launch {
                    delay(1000)
                    if (player.playWhenReady && player.playbackState == Player.STATE_READY) {
                        currentSong.value?.let { song ->
                            updateDiscordRPC(song)
                        }
                    }
                }
        }
    }

    /**
     * Extracts the HTTP response code from an error's cause chain.
     * Returns null if no HTTP response code is found.
     */
    private fun getHttpResponseCode(error: PlaybackException): Int? {
        var cause: Throwable? = error.cause
        while (cause != null) {
            if (cause is HttpDataSource.InvalidResponseCodeException) {
                return cause.responseCode
            }
            cause = cause.cause
        }
        return null
    }

    /**
     * Checks if the error is caused by an expired/forbidden URL (HTTP 403).
     * This typically happens when a YouTube stream URL expires.
     */
    private fun isExpiredUrlError(error: PlaybackException): Boolean {
        val responseCode = getHttpResponseCode(error)
        return responseCode == 403
    }

    /**
     * Checks if the error is a Range Not Satisfiable error (HTTP 416).
     * This happens when cached data doesn't match the actual stream size.
     */
    private fun isRangeNotSatisfiableError(error: PlaybackException): Boolean {
        val responseCode = getHttpResponseCode(error)
        return responseCode == 416
    }

    /**
     * Checks if the error is a "page needs to be reloaded" error.
     * This is a YouTube-specific error that requires refreshing the stream.
     */
    private fun isPageReloadError(error: PlaybackException): Boolean {
        val errorMessage = error.message?.lowercase() ?: ""
        val causeMessage = error.cause?.message?.lowercase() ?: ""
        val innerCauseMessage =
            error.cause
                ?.cause
                ?.message
                ?.lowercase() ?: ""

        val reloadKeywords =
            listOf(
                "page needs to be reloaded",
                "pagina deve essere ricaricata",
                "la pagina deve essere ricaricata",
                "page must be reloaded",
                "reload",
                "ricaricata",
            )

        return reloadKeywords.any { keyword ->
            errorMessage.contains(keyword) ||
                    causeMessage.contains(keyword) ||
                    innerCauseMessage.contains(keyword)
        }
    }

    private fun isNetworkRelatedError(error: PlaybackException): Boolean {
        if (isExpiredUrlError(error) || isRangeNotSatisfiableError(error) || isPageReloadError(error)) {
            return false
        }
        return error.errorCode == PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED ||
                error.errorCode == PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT ||
                error.errorCode == PlaybackException.ERROR_CODE_IO_INVALID_HTTP_CONTENT_TYPE ||
                error.cause is java.net.ConnectException ||
                error.cause is java.net.UnknownHostException ||
                (error.cause as? PlaybackException)?.errorCode == PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED
    }

    /**
     * Checks if the error is caused by AudioTrack write or initialization failures.
     * These errors indicate the audio renderer is in a corrupted/invalid state.
     */
    private fun isAudioRendererError(error: PlaybackException): Boolean =
        error.errorCode == PlaybackException.ERROR_CODE_AUDIO_TRACK_WRITE_FAILED ||
                error.errorCode == PlaybackException.ERROR_CODE_AUDIO_TRACK_INIT_FAILED ||
                (error.cause as? PlaybackException)?.errorCode == PlaybackException.ERROR_CODE_AUDIO_TRACK_WRITE_FAILED ||
                (error.cause as? PlaybackException)?.errorCode == PlaybackException.ERROR_CODE_AUDIO_TRACK_INIT_FAILED

    override fun onPlayerError(error: PlaybackException) {
        super.onPlayerError(error)

        if (!playerInitialized.value) {
            Timber.tag(TAG).e(error, "Player error occurred but player not initialized")
            return
        }

        val mediaId = player.currentMediaItem?.mediaId
        Timber
            .tag(TAG)
            .w(error, "Player error occurred for $mediaId: errorCode=${error.errorCode}, message=${error.message}")
        reportException(error)

        if (mediaId != null && hasExceededRetryLimit(mediaId)) {
            Timber.tag(TAG).w("Song $mediaId has exceeded retry limit, skipping")
            markSongAsFailed(mediaId)
            handleFinalFailure()
            return
        }

        if (mediaId != null) {
            performAggressiveCacheClear(mediaId)
        }

        when {
            isAudioRendererError(error) -> {
                Timber.tag(TAG).d("AudioTrack error detected (${error.errorCode}), performing safe recovery")
                handleAudioRendererError(mediaId)
                return
            }

            isRangeNotSatisfiableError(error) -> {
                Timber.tag(TAG).d("Range Not Satisfiable (416) detected, performing strict recovery")
                handleRangeNotSatisfiableError(mediaId)
                return
            }

            isPageReloadError(error) -> {
                Timber.tag(TAG).d("Page reload error detected, performing strict recovery")
                handlePageReloadError(mediaId)
                return
            }

            isExpiredUrlError(error) -> {
                Timber.tag(TAG).d("Expired URL (403) detected, refreshing stream URL")
                handleExpiredUrlError(mediaId)
                return
            }

            !isNetworkConnected.value || isNetworkRelatedError(error) -> {
                Timber.tag(TAG).d("Network-related error detected, waiting for connection")
                waitOnNetworkError()
                return
            }
        }

        if (error.errorCode == PlaybackException.ERROR_CODE_IO_UNSPECIFIED ||
            error.errorCode == PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS
        ) {
            Timber.tag(TAG).d("IO error detected (${error.errorCode}), attempting recovery")
            handleGenericIOError(mediaId)
            return
        }

        if (mediaId != null && !hasExceededRetryLimit(mediaId)) {
            Timber.tag(TAG).d("Unknown error (${error.errorCode}), auto-retrying $mediaId before giving up")
            handleGenericIOError(mediaId)
            return
        }

        if (dataStore.get(AutoSkipNextOnErrorKey, false)) {
            Timber.tag(TAG).d("Auto-skipping to next track due to unrecoverable error")
            skipOnError()
        } else {
            Timber.tag(TAG).d("Stopping playback due to unrecoverable error")
            stopOnError()
        }
    }

    /**
     * Performs aggressive cache clearing for a media item.
     * Clears both player cache and download cache, plus URL cache.
     */
    private fun performAggressiveCacheClear(mediaId: String) {
        Timber.tag(TAG).d("Performing aggressive cache clear for $mediaId")

        songUrlCache.remove(mediaId)

        try {
            playerCache.removeResource(mediaId)
            Timber.tag(TAG).d("Cleared player cache for $mediaId")
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Failed to clear player cache for $mediaId")
        }

        try {
            YTPlayerUtils.forceRefreshForVideo(mediaId)
            Timber.tag(TAG).d("Cleared decryption caches for $mediaId")
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Failed to clear decryption caches for $mediaId")
        }
    }

    /**
     * Checks if a song has exceeded the retry limit.
     */
    private fun hasExceededRetryLimit(mediaId: String): Boolean {
        val currentRetries = currentMediaIdRetryCount[mediaId] ?: 0
        return currentRetries >= MAX_RETRY_PER_SONG
    }

    /**
     * Increments the retry count for a song.
     */
    private fun incrementRetryCount(mediaId: String) {
        val currentRetries = currentMediaIdRetryCount[mediaId] ?: 0
        currentMediaIdRetryCount[mediaId] = currentRetries + 1
        Timber.tag(TAG).d("Retry count for $mediaId: ${currentRetries + 1}/$MAX_RETRY_PER_SONG")
    }

    /**
     * Resets the retry count for a song (called on successful playback).
     */
    private fun resetRetryCount(mediaId: String) {
        currentMediaIdRetryCount.remove(mediaId)
        recentlyFailedSongs.remove(mediaId)
    }

    /**
     * Marks a song as failed to prevent further retry attempts.
     */
    private fun markSongAsFailed(mediaId: String) {
        recentlyFailedSongs.add(mediaId)
        currentMediaIdRetryCount.remove(mediaId)

        failedSongsClearJob?.cancel()
        failedSongsClearJob =
            scope.launch {
                delay(5 * 60 * 1000L)
                recentlyFailedSongs.clear()
                Timber.tag(TAG).d("Cleared recently failed songs list")
            }
    }

    /**
     * Handles AudioTrack errors (write failed, init failed) with safe recovery.
     * These errors indicate the audio renderer is corrupted and needs careful reset.
     */
    private fun handleAudioRendererError(mediaId: String?) {
        if (mediaId == null) {
            handleFinalFailure()
            return
        }

        incrementRetryCount(mediaId)

        retryJob?.cancel()
        retryJob =
            scope.launch {
                try {
                    player.pause()
                    Timber.tag(TAG).d("Paused playback due to AudioTrack error")

                    delay(RETRY_DELAY_MS)

                    if (!playerInitialized.value) {
                        Timber.tag(TAG).w("Player no longer initialized, aborting AudioTrack recovery")
                        return@launch
                    }

                    val currentIndex = player.currentMediaItemIndex
                    if (currentIndex != C.INDEX_UNSET) {
                        val currentPosition = player.currentPosition
                        player.seekTo(currentIndex, currentPosition)
                        player.prepare()

                        Timber.tag(TAG).d("Retrying playback for $mediaId after AudioTrack error")

                        if (wasPlayingBeforeAudioFocusLoss) {
                            delay(200)
                            if (hasAudioFocus && playerInitialized.value) {
                                if (castConnectionHandler?.isCasting?.value != true) {
                                    player.play()
                                }
                            }
                        }
                    } else {
                        Timber.tag(TAG).w("Invalid media item index during AudioTrack recovery")
                        handleFinalFailure()
                    }
                } catch (e: Exception) {
                    Timber.tag(TAG).e(e, "Error during AudioTrack error recovery")
                    handleFinalFailure()
                }
            }
    }

    /**
     * Handles Range Not Satisfiable (416) errors with strict recovery.
     * This error occurs when cached data doesn't match the actual stream size.
     */
    private fun handleRangeNotSatisfiableError(mediaId: String?) {
        if (mediaId == null) {
            handleFinalFailure()
            return
        }

        incrementRetryCount(mediaId)

        retryJob?.cancel()
        retryJob =
            scope.launch {
                performAggressiveCacheClear(mediaId)

                val currentPosition = player.currentPosition
                val currentIndex = player.currentMediaItemIndex

                delay(RETRY_DELAY_MS)

                player.seekTo(currentIndex, currentPosition)
                player.prepare()

                Timber.tag(TAG).d("Retrying playback for $mediaId after 416 error (from position ${currentPosition}ms)")
            }
    }

    /**
     * Handles "page needs to be reloaded" errors with strict recovery.
     * This requires clearing decryption caches and getting fresh stream URLs.
     */
    private fun handlePageReloadError(mediaId: String?) {
        if (mediaId == null) {
            handleFinalFailure()
            return
        }

        incrementRetryCount(mediaId)

        retryJob?.cancel()
        retryJob =
            scope.launch {
                Timber.tag(TAG).d("Handling page reload error for $mediaId")

                performAggressiveCacheClear(mediaId)

                delay(RETRY_DELAY_MS * 2)

                val currentPosition = player.currentPosition
                val currentIndex = player.currentMediaItemIndex
                player.seekTo(currentIndex, currentPosition)
                player.prepare()

                Timber.tag(TAG).d("Retrying playback for $mediaId after page reload error")
            }
    }

    /**
     * Handles expired URL (403) errors by clearing caches and retrying.
     */
    private fun handleExpiredUrlError(mediaId: String?) {
        if (mediaId == null) {
            handleFinalFailure()
            return
        }

        incrementRetryCount(mediaId)

        songUrlCache.remove(mediaId)
        Timber.tag(TAG).d("Cleared cached URL for $mediaId")

        try {
            YTPlayerUtils.forceRefreshForVideo(mediaId)
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Failed to clear decryption caches")
        }

        retryJob?.cancel()
        retryJob =
            scope.launch {
                delay(RETRY_DELAY_MS)

                val currentPosition = player.currentPosition
                val currentIndex = player.currentMediaItemIndex
                player.seekTo(currentIndex, currentPosition)
                player.prepare()

                Timber.tag(TAG).d("Retrying playback for $mediaId after 403 error")
            }
    }

    /**
     * Handles generic IO errors with recovery attempt.
     */
    private fun handleGenericIOError(mediaId: String?) {
        if (mediaId == null) {
            handleFinalFailure()
            return
        }

        incrementRetryCount(mediaId)

        retryJob?.cancel()
        retryJob =
            scope.launch {
                performAggressiveCacheClear(mediaId)

                val currentPosition = player.currentPosition
                val currentIndex = player.currentMediaItemIndex

                delay(RETRY_DELAY_MS)

                player.stop()
                player.seekTo(currentIndex, currentPosition)
                player.prepare()

                Timber.tag(TAG).d("Retrying playback for $mediaId after generic IO error (from position ${currentPosition}ms)")
            }
    }

    /**
     * Handles final failure when all recovery attempts have been exhausted.
     */
    private fun handleFinalFailure() {
        if (dataStore.get(AutoSkipNextOnErrorKey, false)) {
            Timber.tag(TAG).d("All recovery attempts exhausted, auto-skipping to next track")
            skipOnError()
        } else {
            Timber.tag(TAG).d("All recovery attempts exhausted, stopping playback")
            stopOnError()
        }
    }

    override fun onDeviceVolumeChanged(
        volume: Int,
        muted: Boolean,
    ) {
        super.onDeviceVolumeChanged(volume, muted)
        val pauseOnMute = dataStore.get(PauseOnMute, false)

        if ((volume == 0 || muted) && pauseOnMute) {
            if (player.isPlaying) {
                wasPlayingBeforeVolumeMute = true
                isPausedByVolumeMute = true
                player.pause()
            }
        } else if (volume > 0 && !muted && pauseOnMute) {
            if (wasPlayingBeforeVolumeMute && !player.isPlaying && castConnectionHandler?.isCasting?.value != true) {
                wasPlayingBeforeVolumeMute = false
                isPausedByVolumeMute = false
                player.play()
            }
        }
    }

    private fun createCacheDataSource(): CacheDataSource.Factory =
        CacheDataSource
            .Factory()
            .setCache(downloadCache)
            .setUpstreamDataSourceFactory(
                CacheDataSource
                    .Factory()
                    .setCache(playerCache)
                    .setUpstreamDataSourceFactory(
                        DefaultDataSource.Factory(
                            this,
                            OkHttpDataSource.Factory(
                                OkHttpClient
                                    .Builder()
                                    .proxy(YouTube.proxy)
                                    .proxyAuthenticator { _, response ->
                                        YouTube.proxyAuth?.let { auth ->
                                            response.request
                                                .newBuilder()
                                                .header("Proxy-Authorization", auth)
                                                .build()
                                        } ?: response.request
                                    }.build(),
                            ),
                        ),
                    ),
            ).setCacheWriteDataSinkFactory(null)
            .setFlags(FLAG_IGNORE_CACHE_ON_ERROR)

    private var isSilenceSkipping = false

    private fun handleLongSilenceDetected() {
        if (!instantSilenceSkipEnabled.value) return
        if (silenceSkipJob?.isActive == true) return

        silenceSkipJob =
            scope.launch {
                delay(200)
                performInstantSilenceSkip()
            }
    }

    private suspend fun performInstantSilenceSkip() {
        val duration = player.duration.takeIf { it != C.TIME_UNSET && it > 0 } ?: return
        if (duration <= INSTANT_SILENCE_SKIP_STEP_MS) return

        isSilenceSkipping = true
        try {
            var hops = 0
            val silenceProcessor = playerSilenceProcessors[player] ?: return
            while (coroutineContext.isActive && instantSilenceSkipEnabled.value && silenceProcessor.isCurrentlySilent()) {
                val current = player.currentPosition
                val target = (current + INSTANT_SILENCE_SKIP_STEP_MS).coerceAtMost(duration - 500)

                if (target <= current) break

                silenceProcessor.resetTracking()
                player.seekTo(target)
                hops++

                if (hops >= 80 || target >= duration - 500) break

                delay(INSTANT_SILENCE_SKIP_SETTLE_MS)
            }
            if (hops > 0) {
                Timber.tag(TAG).d("Silence skip: jumped $hops times")
            }
        } finally {
            isSilenceSkipping = false
        }
    }

    private fun updateDiscordRPC(
        song: Song,
        showFeedback: Boolean = false,
    ) {
        val useDetails = dataStore.get(DiscordUseDetailsKey, false)
        val advancedMode = dataStore.get(DiscordAdvancedModeKey, false)

        val status = if (advancedMode) dataStore.get(DiscordStatusKey, "online") else "online"
        val b1Text = if (advancedMode) dataStore.get(DiscordButton1TextKey, "") else ""
        val b1Visible = if (advancedMode) dataStore.get(DiscordButton1VisibleKey, true) else true
        val b2Text = if (advancedMode) dataStore.get(DiscordButton2TextKey, "") else ""
        val b2Visible = if (advancedMode) dataStore.get(DiscordButton2VisibleKey, true) else true
        val activityType = if (advancedMode) dataStore.get(DiscordActivityTypeKey, "listening") else "listening"
        val activityName = if (advancedMode) dataStore.get(DiscordActivityNameKey, "") else ""

        discordUpdateJob?.cancel()
        discordUpdateJob =
            scope.launch {
                discordRpc
                    ?.updateSong(
                        song,
                        player.currentPosition,
                        player.playbackParameters.speed,
                        useDetails,
                        status,
                        b1Text,
                        b1Visible,
                        b2Text,
                        b2Visible,
                        activityType,
                        activityName,
                    )?.onFailure {
                        if (showFeedback) {
                            Handler(Looper.getMainLooper()).post {
                                Toast
                                    .makeText(
                                        this@MusicService,
                                        "Discord RPC update failed: ${it.message}",
                                        Toast.LENGTH_SHORT,
                                    ).show()
                            }
                        }
                    }
            }
    }

    private suspend fun resolvePlayback(
        mediaId: String,
        playlistId: String? = null,
    ): Result<YTPlayerUtils.PlaybackData> =
        if (muzzaPlayerLogicEnabled) {
            YTPlayerUtils.playerResponseForPlaybackMuzza(
                mediaId,
                playlistId,
                audioQuality = audioQuality,
                connectivityManager = connectivityManager,
            )
        } else {
            YTPlayerUtils.playerResponseForPlayback(
                mediaId,
                playlistId,
                audioQuality = audioQuality,
                connectivityManager = connectivityManager,
            )
        }

    private fun createDataSourceFactory(): DataSource.Factory {
        return ResolvingDataSource.Factory(createCacheDataSource()) { dataSpec ->
            val mediaId = dataSpec.key ?: error("No media id")

            val shouldBypassCache = bypassCacheForQualityChange.contains(mediaId)

            if (!shouldBypassCache) {
                val usePlayerCache = dataStore.get(EnableSongCacheKey, true)
                if (downloadCache.isCached(
                        mediaId,
                        dataSpec.position,
                        if (dataSpec.length >= 0) dataSpec.length else 1,
                    ) ||
                    (usePlayerCache && playerCache.isCached(mediaId, dataSpec.position, CHUNK_LENGTH))
                ) {
                    scope.launch(Dispatchers.IO) { recoverSong(mediaId) }
                    return@Factory dataSpec
                }

                songUrlCache[mediaId]?.takeIf { it.second > System.currentTimeMillis() }?.let {
                    scope.launch(Dispatchers.IO) { recoverSong(mediaId) }
                    return@Factory dataSpec.withUri(it.first.toUri())
                }
            } else {
                Timber.tag("MusicService").i("BYPASSING CACHE for $mediaId due to quality change")
            }

            Timber.tag("MusicService").i("FETCHING STREAM: $mediaId | quality=$audioQuality")
            val playbackData =
                runBlocking(Dispatchers.IO) {
                    resolvePlayback(mediaId)
                }.getOrElse { throwable ->
                    when (throwable) {
                        is PlaybackException -> {
                            throw throwable
                        }

                        is java.net.ConnectException, is java.net.UnknownHostException -> {
                            throw PlaybackException(
                                getString(R.string.error_no_internet),
                                throwable,
                                PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED,
                            )
                        }

                        is java.net.SocketTimeoutException -> {
                            throw PlaybackException(
                                getString(R.string.error_timeout),
                                throwable,
                                PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT,
                            )
                        }

                        else -> {
                            throw PlaybackException(
                                getString(R.string.error_unknown),
                                throwable,
                                PlaybackException.ERROR_CODE_REMOTE_ERROR,
                            )
                        }
                    }
                }

            val nonNullPlayback =
                requireNotNull(playbackData) {
                    getString(R.string.error_unknown)
                }
            run {
                val format = nonNullPlayback.format
                val loudnessDb = nonNullPlayback.audioConfig?.loudnessDb
                val perceptualLoudnessDb = nonNullPlayback.audioConfig?.perceptualLoudnessDb

                Timber
                    .tag(TAG)
                    .d("Storing format for $mediaId with loudnessDb: $loudnessDb, perceptualLoudnessDb: $perceptualLoudnessDb")
                if (loudnessDb == null && perceptualLoudnessDb == null) {
                    Timber.tag(TAG).w("No loudness data available from YouTube for video: $mediaId")
                }

                database.query {
                    upsert(
                        FormatEntity(
                            id = mediaId,
                            itag = format.itag,
                            mimeType = format.mimeType.split(";")[0],
                            codecs = format.mimeType.split("codecs=").getOrNull(1)?.removeSurrounding("\"").orEmpty(),
                            bitrate = format.bitrate,
                            sampleRate = format.audioSampleRate,
                            contentLength = format.contentLength ?: 0L,
                            loudnessDb = loudnessDb,
                            perceptualLoudnessDb = perceptualLoudnessDb,
                            playbackUrl = nonNullPlayback.playbackTracking?.videostatsPlaybackUrl?.baseUrl,
                        ),
                    )
                }
                scope.launch(Dispatchers.IO) { recoverSong(mediaId, nonNullPlayback) }

                if (bypassCacheForQualityChange.remove(mediaId)) {
                    Timber.tag("MusicService").d("Cleared bypass cache flag for $mediaId after fresh fetch")
                }

                val streamUrl = nonNullPlayback.streamUrl

                songUrlCache[mediaId] =
                    streamUrl to System.currentTimeMillis() + (nonNullPlayback.streamExpiresInSeconds * 1000L)
                return@Factory dataSpec.withUri(streamUrl.toUri()).subrange(dataSpec.uriPositionOffset, CHUNK_LENGTH)
            }
        }
    }

    private fun createMediaSourceFactory() =
        DefaultMediaSourceFactory(
            createDataSourceFactory(),
            ExtractorsFactory {
                arrayOf(MatroskaExtractor(), FragmentedMp4Extractor())
            },
        )

    private fun createRenderersFactory(
        eqProcessor: CustomEqualizerAudioProcessor,
        silenceProcessor: SilenceDetectorAudioProcessor,
        scratchProcessor: ScratchAudioProcessor,
        visualizerTap: com.metrolist.music.playback.audio.VisualizerTapAudioProcessor,
    ) = object : DefaultRenderersFactory(this) {
        override fun buildAudioSink(
            context: Context,
            enableFloatOutput: Boolean,
            enableAudioTrackPlaybackParams: Boolean,
        ) = DefaultAudioSink
            .Builder(this@MusicService)
            .setEnableFloatOutput(enableFloatOutput)
            .setEnableAudioTrackPlaybackParams(enableAudioTrackPlaybackParams)
            .setAudioTrackBufferSizeProvider(
                DefaultAudioTrackBufferSizeProvider.Builder()
                    .setMinPcmBufferDurationUs(60_000)
                    .setMaxPcmBufferDurationUs(400_000)
                    .build(),
            )
            .setAudioProcessorChain(
                DefaultAudioSink.DefaultAudioProcessorChain(
                    arrayOf(
                        eqProcessor,
                        visualizerTap,
                        silenceProcessor,
                        scratchProcessor,
                    ),
                    SilenceSkippingAudioProcessor(2_000_000, 20_000, 256),
                    SonicAudioProcessor(),
                ),
            ).build()
    }

    override fun onPlaybackStatsReady(
        eventTime: AnalyticsListener.EventTime,
        playbackStats: PlaybackStats,
    ) {
        val mediaItem = eventTime.timeline.getWindow(eventTime.windowIndex, Timeline.Window()).mediaItem
        val historyDurationMs = dataStore[HistoryDuration]?.times(1000f) ?: 30000f

        if (playbackStats.totalPlayTimeMs >= historyDurationMs &&
            !dataStore.get(PauseListenHistoryKey, false)
        ) {
            database.query {
                incrementTotalPlayTime(mediaItem.mediaId, playbackStats.totalPlayTimeMs)
                try {
                    insert(
                        Event(
                            songId = mediaItem.mediaId,
                            timestamp = LocalDateTime.now(),
                            playTime = playbackStats.totalPlayTimeMs,
                        ),
                    )
                } catch (e: SQLException) {
                    Timber.tag(TAG).w(e, "Failed to insert listen history event")
                }
            }
        }

        if (playbackStats.totalPlayTimeMs >= historyDurationMs) {
            CoroutineScope(Dispatchers.IO).launch {
                val playbackUrl =
                    database.format(mediaItem.mediaId).first()?.playbackUrl
                        ?: YTPlayerUtils
                            .playerResponseForMetadata(mediaItem.mediaId, null)
                            .getOrNull()
                            ?.playbackTracking
                            ?.videostatsPlaybackUrl
                            ?.baseUrl
                playbackUrl?.let {
                    YouTube
                        .registerPlayback(null, playbackUrl)
                        .onFailure {
                            reportException(it)
                        }
                }
            }
        }
    }

    private fun saveQueueToDisk(sync: Boolean = false) {
        if (player.mediaItemCount == 0) {
            Timber.tag(TAG).d("Skipping queue save - no media items")
            return
        }

        val persistQueue =
            currentQueue.toPersistQueue(
                title = queueTitle,
                items = player.mediaItems.mapNotNull { it.metadata },
                mediaItemIndex = player.currentMediaItemIndex,
                position = player.currentPosition,
            )

        val persistPlayerState =
            PersistPlayerState(
                playWhenReady = player.playWhenReady,
                repeatMode = player.repeatMode,
                shuffleModeEnabled = player.shuffleModeEnabled,
                volume = playerVolume.value,
                currentPosition = player.currentPosition,
                currentMediaItemIndex = player.currentMediaItemIndex,
                playbackState = player.playbackState,
            )

        val writeToDisk = writeToDisk@{
            try {
                writeQueueFiles(persistQueue, persistPlayerState)
            } catch (e: Exception) {
                Timber.tag(TAG).e(e, "Error during queue save operation")
                reportException(e)
            }
        }

        if (sync) {
            writeToDisk()
        } else {
            scope.launch(Dispatchers.IO) { writeToDisk() }
        }
    }

    private fun writeQueueFiles(
        persistQueue: PersistQueue,
        persistPlayerState: PersistPlayerState,
    ) {
        runCatching {
            filesDir.resolve(PERSISTENT_QUEUE_FILE).outputStream().use { fos ->
                ObjectOutputStream(fos).use { oos ->
                    oos.writeObject(persistQueue)
                }
            }
            Timber.tag(TAG).d("Queue saved successfully")
        }.onFailure {
            Timber.tag(TAG).e(it, "Failed to save queue")
            reportException(it)
        }

        runCatching { filesDir.resolve(PERSISTENT_AUTOMIX_FILE).delete() }

        runCatching {
            filesDir.resolve(PERSISTENT_PLAYER_STATE_FILE).outputStream().use { fos ->
                ObjectOutputStream(fos).use { oos ->
                    oos.writeObject(persistPlayerState)
                }
            }
            Timber.tag(TAG).d("Player state saved successfully")
        }.onFailure {
            Timber.tag(TAG).e(it, "Failed to save player state")
            reportException(it)
        }
    }

    override fun onDestroy() {
        isRunning = false

        val currentMetadata = player.currentMediaItem?.metadata
        if (currentMetadata?.isEpisode == true && player.currentPosition > 0) {
            runBlocking(Dispatchers.IO) {
                database.updatePlaybackPosition(currentMetadata.id, player.currentPosition)
            }
        }

        try {
            unregisterReceiver(screenStateReceiver)
        } catch (e: Exception) {
        }
        audioManager.unregisterAudioDeviceCallback(audioDeviceCallback)
        castConnectionHandler?.release()
        if (persistentQueueEnabled) {
            saveQueueToDisk(sync = true)
        }
        if (discordRpc?.isRpcRunning() == true) {
            discordRpc?.closeRPC()
        }
        discordRpc = null
        connectivityObserver.unregister()
        abandonAudioFocus()
        releaseLoudnessEnhancer()
        cancelPreload()
        mediaLibrarySessionCallback.release()
        mediaSession.release()
        player.removeListener(this)
        player.removeListener(sleepTimer)
        playerSilenceProcessors.remove(player)
        player.release()
        discordUpdateJob?.cancel()
        scope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?) = super.onBind(intent) ?: binder

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        if (dataStore.get(StopMusicOnTaskClearKey, false)) {
            player.stop()
            stopSelf()
        }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo) = if (::mediaSession.isInitialized) mediaSession else null

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int,
    ): Int {
        when (intent?.action) {
            ACTION_ALARM_TRIGGER -> {
                handleAlarmTrigger(intent)
            }

            MusicWidgetReceiver.ACTION_PLAY_PAUSE -> {
                if (player.isPlaying) player.pause() else player.play()
                updateWidgetUI(player.isPlaying)
            }

            MusicWidgetReceiver.ACTION_LIKE -> {
                val currentLiked = currentSong.value?.song?.liked == true
                widgetManager.updateLikeButtonOptimistic(!currentLiked)

                scope.launch {
                    val songData = currentSong.value
                    val song = songData?.song
                    val songTitle = song?.title ?: getString(R.string.no_song_playing)
                    val artistName = songData?.artists?.joinToString(", ") { it.name } ?: getString(R.string.tap_to_open)
                    widgetManager.updateWidgets(
                        title = songTitle,
                        artist = artistName,
                        artworkUri = song?.thumbnailUrl?.resize(512, 512),
                        isPlaying = player.isPlaying,
                        isLiked = !currentLiked,
                        duration = if (player.duration != C.TIME_UNSET) player.duration else 0,
                        currentPosition = player.currentPosition,
                    )
                }
                toggleLike()
            }

            MusicWidgetReceiver.ACTION_NEXT -> {
                player.seekToNext()
                updateWidgetUI(player.isPlaying)
            }

            MusicWidgetReceiver.ACTION_PREVIOUS -> {
                player.seekToPrevious()
                updateWidgetUI(player.isPlaying)
            }

            MusicWidgetReceiver.ACTION_UPDATE_WIDGET -> {
                updateWidgetUI(player.isPlaying)
            }
        }

        return super.onStartCommand(intent, flags, startId)
    }

    private fun handleAlarmTrigger(intent: Intent) {
        scope.launch(Dispatchers.IO) {
            try {
                MusicAlarmScheduler.scheduleFromPreferences(this@MusicService)
            } catch (t: Throwable) {
                Timber.tag(TAG).e(t, "Failed to reschedule alarms after trigger")
            }
        }
        val playlistId = intent.getStringExtra(EXTRA_ALARM_PLAYLIST_ID).orEmpty()
        val alarmId = intent.getStringExtra(EXTRA_ALARM_ID).orEmpty()
        if (playlistId.isBlank()) {
            if (alarmId.isNotBlank()) {
                scope.launch(Dispatchers.IO) {
                    try {
                        val alarms = MusicAlarmStore.load(this@MusicService)
                        val updated =
                            alarms.map { alarm ->
                                if (alarm.id == alarmId) alarm.copy(enabled = false, nextTriggerAt = -1L) else alarm
                            }
                        MusicAlarmScheduler.scheduleAll(this@MusicService, updated)
                    } catch (t: Throwable) {
                        Timber.tag(TAG).e(t, "Failed to disable alarm with invalid playlist")
                    }
                }
            }
            return
        }
        val randomSong = intent.getBooleanExtra(EXTRA_ALARM_RANDOM_SONG, false)
        scope.launch {
            try {
                val playlistSongs =
                    withContext(Dispatchers.IO) {
                        database.playlistSongs(playlistId).first()
                    }
                if (playlistSongs.isEmpty()) {
                    if (alarmId.isNotBlank()) {
                        withContext(Dispatchers.IO) {
                            val alarms = MusicAlarmStore.load(this@MusicService)
                            val updated =
                                alarms.map { alarm ->
                                    if (alarm.id == alarmId) alarm.copy(enabled = false, nextTriggerAt = -1L) else alarm
                                }
                            MusicAlarmScheduler.scheduleAll(this@MusicService, updated)
                        }
                    }
                    return@launch
                }
                val items = playlistSongs.map { it.song.toMediaItem() }
                val playlistName =
                    withContext(Dispatchers.IO) {
                        database
                            .playlist(playlistId)
                            .first()
                            ?.playlist
                            ?.name
                    }
                withContext(Dispatchers.IO) {
                    MusicAlarmScheduler.scheduleFromPreferences(this@MusicService)
                }

                val alarmItems =
                    if (randomSong) {
                        val firstIndex = Random.nextInt(items.size)
                        buildList(items.size) {
                            add(items[firstIndex])
                            items.forEachIndexed { index, item ->
                                if (index != firstIndex) add(item)
                            }
                        }
                    } else {
                        items
                    }

                player.stop()
                player.clearMediaItems()
                playQueue(
                    ListQueue(
                        title = playlistName,
                        items = alarmItems,
                        startIndex = 0,
                        position = 0L,
                    ),
                    playWhenReady = true,
                )
            } catch (t: Throwable) {
                Timber.tag(TAG).e(t, "Failed to start alarm playback")
            }
        }
    }

    /**
     * Updates all app widgets with current playback state
     */
    private fun updateWidgetUI(isPlaying: Boolean) {
        scope.launch {
            try {
                if (widgetManager.getActiveWidgetIds().isEmpty()) return@launch
                val songData = currentSong.value
                val song = songData?.song
                val songTitle = song?.title ?: getString(R.string.no_song_playing)
                val artistName = songData?.artists?.joinToString(", ") { it.name } ?: getString(R.string.tap_to_open)
                val isLiked = songData?.song?.liked == true

                widgetManager.updateWidgets(
                    title = songTitle,
                    artist = artistName,
                    artworkUri = song?.thumbnailUrl?.resize(512, 512),
                    isPlaying = isPlaying,
                    isLiked = isLiked,
                    duration = if (player.duration != C.TIME_UNSET) player.duration else 0,
                    currentPosition = player.currentPosition,
                )
            } catch (e: Exception) {
                Timber.tag(TAG).w(e, "Failed to update widgets")
            }
        }
    }

    private var widgetUpdateJob: Job? = null

    private fun startWidgetUpdates() {
        widgetUpdateJob?.cancel()
        widgetUpdateJob =
            scope.launch {
                while (isActive) {
                    if (player.isPlaying) {
                        updateWidgetUI(true)
                    }
                    delay(1000)
                }
            }
    }

    private fun stopWidgetUpdates() {
        widgetUpdateJob?.cancel()
        widgetUpdateJob = null
    }

    private fun shareSong() {
        val songData = currentSong.value
        val songId = songData?.song?.id ?: return

        val shareIntent =
            Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, "https://music.youtube.com/watch?v=$songId")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        startActivity(
            Intent.createChooser(shareIntent, null).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            },
        )
    }

    /**
     * Get the stream URL for a given media ID.
     * This is used for Google Cast to send the audio URL to Chromecast.
     */
    suspend fun getStreamUrl(mediaId: String): String? =
        withContext(Dispatchers.IO) {
            try {
                val playbackData = resolvePlayback(mediaId).getOrNull()
                playbackData?.streamUrl
            } catch (e: Exception) {
                timber.log.Timber.e(e, "Failed to get stream URL for Cast")
                null
            }
        }

    /**
     * Initialize Google Cast support
     */
    private fun initializeCast() {
        if (dataStore.get(com.metrolist.music.constants.EnableGoogleCastKey, true)) {
            try {
                castConnectionHandler = CastConnectionHandler(this, scope, this)
                castConnectionHandler?.initialize()
                timber.log.Timber.d("Google Cast initialized")
            } catch (e: Exception) {
                timber.log.Timber.e(e, "Failed to initialize Google Cast")
            }
        }
    }

    override fun onPositionDiscontinuity(
        oldPosition: Player.PositionInfo,
        newPosition: Player.PositionInfo,
        reason: Int,
    ) {
        if (reason == Player.DISCONTINUITY_REASON_SEEK) {
            scheduleCrossfade()
        }
    }

    fun skipWithFade(
        forward: Boolean = true,
        restartCurrent: Boolean = false,
        onComplete: (() -> Unit)? = null,
    ): Boolean {
        if (!skipFadeEnabled) return false
        if (!::player.isInitialized) return false

        crossfadeTriggerJob?.cancel()
        crossfadeJob?.cancel()
        if (isCrossfading) cleanupCrossfade()
        cancelPreload()

        val savedRepeatMode = player.repeatMode
        val savedShuffleEnabled = player.shuffleModeEnabled

        val targetIndex: Int = when {
            restartCurrent -> player.currentMediaItemIndex
            forward -> {
                if (savedRepeatMode == REPEAT_MODE_ONE) player.currentMediaItemIndex
                else player.nextMediaItemIndex.takeIf { it != C.INDEX_UNSET } ?: return false
            }
            else -> player.previousMediaItemIndex.takeIf { it != C.INDEX_UNSET } ?: return false
        }

        secondaryPlayer = createExoPlayer()
        val secPlayer = secondaryPlayer ?: return false
        secPlayer.addListener(secondaryPlayerListener)

        val items = (0 until player.mediaItemCount).map { player.getMediaItemAt(it) }
        secPlayer.setMediaItems(items)
        secPlayer.seekTo(targetIndex, 0)
        secPlayer.volume = 0f
        secPlayer.repeatMode = savedRepeatMode
        secPlayer.shuffleModeEnabled = savedShuffleEnabled
        secPlayer.prepare()
        secPlayer.playWhenReady = true

        performCrossfadeSwap(fadeDuration = skipFadeDuration, onComplete = onComplete)

        if (savedShuffleEnabled) {
            val shufflePlaylistFirst = shufflePlaylistFirst
            applyShuffleOrder(player.currentMediaItemIndex, player.mediaItemCount, shufflePlaylistFirst)
        }

        return true
    }

    private fun cleanupSkipFade() {
        applyEffectiveVolume()
    }

    private fun scheduleCrossfade() {
        crossfadeTriggerJob?.cancel()
        crossfadeTriggerJob = null
        if (!crossfadeEnabled || player.duration == C.TIME_UNSET || player.duration <= crossfadeDuration) return
        if (crossfadeGapless && isNextItemGapless()) return
        if (!player.hasNextMediaItem() && player.repeatMode != REPEAT_MODE_ONE) return

        val triggerTime = player.duration - crossfadeDuration.toLong()
        val delayMs = triggerTime - player.currentPosition
        if (delayMs <= 0) return

        val targetMediaId = player.currentMediaItem?.mediaId

        crossfadeTriggerJob =
            scope.launch {
                delay(delayMs)
                if (isActive && player.isPlaying && player.currentMediaItem?.mediaId == targetMediaId && !sleepTimer.pauseWhenSongEnd) {
                    startCrossfade()
                }
            }
    }

    private fun isNextItemGapless(): Boolean {
        val current = player.currentMediaItem?.mediaMetadata ?: return false
        val nextIndex = player.nextMediaItemIndex
        if (nextIndex == C.INDEX_UNSET) return false
        val next = player.getMediaItemAt(nextIndex).mediaMetadata
        return current.albumTitle != null && current.albumTitle == next.albumTitle
    }

    private fun startCrossfade() {
        if (isCrossfading || secondaryPlayer != null) return

        val savedRepeatMode = player.repeatMode
        val savedShuffleEnabled = player.shuffleModeEnabled

        val targetIndex =
            if (savedRepeatMode == REPEAT_MODE_ONE) {
                player.currentMediaItemIndex
            } else {
                player.nextMediaItemIndex
            }
        if (targetIndex == C.INDEX_UNSET) return

        secondaryPlayer = createExoPlayer()
        val secPlayer = secondaryPlayer ?: return
        secPlayer.addListener(secondaryPlayerListener)

        val itemCount = player.mediaItemCount
        val items = mutableListOf<MediaItem>()
        for (i in 0 until itemCount) {
            items.add(player.getMediaItemAt(i))
        }

        secPlayer.setMediaItems(items)
        secPlayer.seekTo(targetIndex, 0)
        secPlayer.volume = 0f

        secPlayer.repeatMode = savedRepeatMode
        secPlayer.shuffleModeEnabled = savedShuffleEnabled

        secPlayer.prepare()
        secPlayer.playWhenReady = true

        performCrossfadeSwap()

        if (savedShuffleEnabled) {
            val shufflePlaylistFirst = shufflePlaylistFirst
            applyShuffleOrder(player.currentMediaItemIndex, player.mediaItemCount, shufflePlaylistFirst)
        }
    }

    private fun performCrossfadeSwap(
        fadeDuration: Float = crossfadeDuration,
        onComplete: (() -> Unit)? = null,
    ) {
        isCrossfading = true
        val nextPlayer = secondaryPlayer ?: return
        val currentPlayer = player

        fadingPlayer = currentPlayer
        player = nextPlayer
        _playerFlow.value = player
        secondaryPlayer = null

        fadingPlayer?.removeListener(this)
        fadingPlayer?.removeListener(sleepTimer)

        player.addListener(
            object : Player.Listener {
                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    if (isCrossfading && fadingPlayer != null) {
                        if (isPlaying) {
                            fadingPlayer?.play()
                        } else {
                            fadingPlayer?.pause()
                        }
                    } else {
                        player.removeListener(this)
                    }
                }
            },
        )

        nextPlayer.removeListener(secondaryPlayerListener)
        nextPlayer.addListener(this)
        nextPlayer.addListener(sleepTimer)

        sleepTimer.player = player

        try {
            (mediaSession as MediaSession).player = player
        } catch (e: Exception) {
            timber.log.Timber.e(e, "Failed to swap player in MediaSession")
        }

        crossfadeJob =
            scope.launch {
                val duration = fadeDuration.toLong()
                val steps = 20
                val stepTime = (duration / steps).coerceAtLeast(1L)
                val startVolume =
                    try {
                        fadingPlayer?.volume ?: 1f
                    } catch (e: Exception) {
                        1f
                    }

                for (i in 0..steps) {
                    if (!isActive) break
                    while (!player.isPlaying && isActive) {
                        delay(100)
                    }

                    val progress = i / steps.toFloat()
                    val fadeIn = 1.0f - (1.0f - progress) * (1.0f - progress)
                    val fadeOut = (1.0f - progress) * (1.0f - progress)

                    try {
                        player.volume = startVolume * fadeIn
                        fadingPlayer?.volume = startVolume * fadeOut
                    } catch (e: Exception) {
                        break
                    }

                    delay(stepTime)
                }

                try {
                    fadingPlayer?.volume = 0f
                    player.volume = startVolume
                    cleanupCrossfade()
                    onComplete?.invoke()
                } catch (e: Exception) {
                    Timber.tag(TAG).w(e, "Crossfade completion failed")
                }
            }
    }

    private fun cleanupCrossfade() {
        val dying = fadingPlayer
        dying?.stop()
        dying?.clearMediaItems()
        dying?.release()
        dying?.let { playerSilenceProcessors.remove(it) }
        fadingPlayer = null
        isCrossfading = false
        applyEffectiveVolume()
        sleepTimer.notifySongTransition()
    }

    private fun schedulePreload() {
        cancelPreload()
        if (!::player.isInitialized) return
        val nextIndex = player.nextMediaItemIndex
        if (nextIndex == C.INDEX_UNSET) return
        val nextItem = try { player.getMediaItemAt(nextIndex) } catch (_: Exception) { return }
        val mediaId = nextItem.mediaId

        preloadJob = scope.launch {
            delay(3000L)
            if (!isActive) return@launch
            if (songUrlCache[mediaId]?.second?.let { it > System.currentTimeMillis() } == true) return@launch
            try {
                withContext(Dispatchers.IO) {
                    resolvePlayback(mediaId).getOrNull()?.let { playbackData ->
                        songUrlCache[mediaId] = playbackData.streamUrl to
                            System.currentTimeMillis() + (playbackData.streamExpiresInSeconds * 1000L)
                    }
                }
            } catch (e: Exception) {
                Timber.tag(TAG).w(e, "Preload failed for $mediaId")
            }
        }
    }

    private fun cancelPreload() {
        preloadJob?.cancel()
        preloadJob = null
    }

    fun prefetchStreamUrls(mediaIds: List<String>) {
        mediaIds.forEach { mediaId ->
            if (songUrlCache[mediaId]?.second?.let { it > System.currentTimeMillis() } == true) return@forEach
            scope.launch(Dispatchers.IO) {
                try {
                    resolvePlayback(mediaId).getOrNull()?.let { playbackData ->
                        songUrlCache[mediaId] = playbackData.streamUrl to
                            System.currentTimeMillis() + (playbackData.streamExpiresInSeconds * 1000L)
                    }
                } catch (e: Exception) {
                    Timber.tag(TAG).w(e, "Stream prefetch failed for $mediaId")
                }
            }
        }
    }

    companion object {
        const val ACTION_ALARM_TRIGGER = "com.metrolist.music.action.ALARM_TRIGGER"
        const val EXTRA_ALARM_ID = "extra_alarm_id"
        const val EXTRA_ALARM_PLAYLIST_ID = "extra_alarm_playlist_id"
        const val EXTRA_ALARM_RANDOM_SONG = "extra_alarm_random_song"

        const val ROOT = "root"
        const val SONG = "song"
        const val ARTIST = "artist"
        const val ALBUM = "album"
        const val PLAYLIST = "playlist"
        const val YOUTUBE_PLAYLIST = "youtube_playlist"
        const val SEARCH = "search"
        const val SHUFFLE_ACTION = "__shuffle__"

        const val CHANNEL_ID = "music_channel_01"
        const val NOTIFICATION_ID = 888
        const val ERROR_CODE_NO_STREAM = 1000001
        const val CHUNK_LENGTH = 512 * 1024L
        const val PERSISTENT_QUEUE_FILE = "persistent_queue.data"
        const val PERSISTENT_AUTOMIX_FILE = "persistent_automix.data"
        const val PERSISTENT_PLAYER_STATE_FILE = "persistent_player_state.data"
        const val MAX_CONSECUTIVE_ERR = 5
        const val MAX_RETRY_COUNT = 10

        private const val MAX_GAIN_MB = 300
        private const val MIN_GAIN_MB = -1500

        private const val TAG = "MusicService"

        @Volatile
        var isRunning = false
            private set
    }
}