/**
 * Iride Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music

import com.metrolist.music.ui.component.IrideFolioDialog

import android.annotation.SuppressLint
import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.ServiceConnection
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.os.SystemClock
import android.view.View
import android.view.ViewTreeObserver
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.derivedStateOf
import kotlinx.coroutines.isActive
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.zIndex
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastAny
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.util.Consumer
import androidx.core.view.WindowCompat
import androidx.datastore.preferences.core.edit
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.coroutineScope
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.ComposeNavigator
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import coil3.imageLoader
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.request.crossfade
import coil3.toBitmap
import com.metrolist.innertube.YouTube
import com.metrolist.innertube.models.SongItem
import com.metrolist.innertube.models.WatchEndpoint
import com.metrolist.music.constants.AppBarHeight
import com.metrolist.music.constants.AppLanguageKey
import com.metrolist.music.constants.CheckForUpdatesKey
import com.metrolist.music.constants.DarkModeKey
import com.metrolist.music.constants.DefaultOpenTabKey
import com.metrolist.music.constants.DisableScreenshotKey
import com.metrolist.music.constants.DynamicThemeKey
import com.metrolist.music.constants.EnableHighRefreshRateKey
import com.metrolist.music.constants.DataSyncIdKey
import com.metrolist.music.constants.InnerTubeCookieKey
import com.metrolist.music.constants.OnboardingCompletedKey
import com.metrolist.music.constants.LastUpdateCheckKey
import com.metrolist.music.constants.   PendingUpdateNotesKey
import com.metrolist.music.constants.PendingUpdateTagKey
import com.metrolist.music.constants.PendingUpdateVersionNameKey
import com.metrolist.music.constants.PlayerAnchorKey
import com.metrolist.music.constants.ListenTogetherInTopBarKey
import com.metrolist.music.constants.ListenTogetherUsernameKey
import com.metrolist.music.constants.LyricsProviderOrderKey
import com.metrolist.music.constants.MiniPlayerHeight
import com.metrolist.music.constants.NavigationBarHeight
import com.metrolist.music.constants.PauseListenHistoryKey
import com.metrolist.music.constants.PauseSearchHistoryKey
import com.metrolist.music.constants.PreferredLyricsProvider
import com.metrolist.music.constants.PreferredLyricsProviderKey
import com.metrolist.music.constants.MainTopGradientKey
import com.metrolist.music.constants.PlayerBackgroundStyle
import com.metrolist.music.constants.PlayerBackgroundStyleKey
import com.metrolist.music.constants.PureBlackKey
import com.metrolist.music.constants.SYSTEM_DEFAULT
import com.metrolist.music.constants.SelectedThemeColorKey
import com.metrolist.music.constants.SimpMusicMigrationDoneKey
import com.metrolist.music.constants.CompactTopNavigationBarKey
import com.metrolist.music.constants.StopMusicOnTaskClearKey
import com.metrolist.music.constants.UpdateAnnouncementDismissedTagKey
import com.metrolist.music.constants.LastSessionEndedAtKey
import com.metrolist.music.constants.VisitorDataKey
import com.metrolist.music.db.MusicDatabase
import com.metrolist.music.db.entities.SearchHistory
import com.metrolist.music.extensions.toEnum
import com.metrolist.music.lyrics.LyricsProviderRegistry
import com.metrolist.music.models.toMediaMetadata
import com.metrolist.music.playback.DownloadUtil
import com.metrolist.music.playback.MusicService
import com.metrolist.music.utils.MiniPlayerSnapshotStore
import com.metrolist.music.playback.MusicService.MusicBinder
import com.metrolist.music.playback.PlayerConnection
import com.metrolist.music.playback.queues.YouTubeQueue
import com.metrolist.music.models.MediaMetadata
import com.metrolist.music.ui.component.AppNavigationRail
import com.metrolist.music.ui.component.TopNavigationBar
import com.metrolist.music.ui.component.TopScreenGradientBackground
import com.metrolist.music.ui.component.LocalIrideFolioScrimColor
import com.metrolist.music.ui.component.IrideFolio
import com.metrolist.music.ui.component.LocalPageNavigationAppearance
import com.metrolist.music.ui.component.LocalFloatingPanelAppearance
import com.metrolist.music.ui.component.rememberFloatingPanelAppearance
import com.metrolist.music.ui.component.PageNavigationAppearance
import com.metrolist.music.ui.component.DebugBubble
import com.metrolist.music.ui.component.FloatingPillBottomSpacing
import com.metrolist.music.ui.component.FloatingBottomNavigation
import com.metrolist.music.ui.component.FloatingBottomNavigationHeight
import com.metrolist.music.ui.component.FloatingBottomNavigationOffset
import com.metrolist.music.ui.component.FrostedMiniPlayerBackdrop
import com.metrolist.music.ui.component.rememberFrostBackdrop
import com.metrolist.music.ui.component.recordFrostBackdrop
import com.metrolist.music.ui.component.invalidateFrostOnChanges
import com.metrolist.music.ui.component.LocalScreenFrostBackground
import com.metrolist.music.ui.component.IrideMiniPlayerHeight
import com.metrolist.music.ui.component.UpdateInterstitialScreen
import com.metrolist.music.ui.component.BottomSheetMenu
import com.metrolist.music.ui.component.BottomSheetPage
import com.metrolist.music.ui.component.LocalBottomSheetPageState
import com.metrolist.music.ui.component.observeSongMenuAnchor
import com.metrolist.music.ui.component.LocalScreenFrostBackdrop
import com.metrolist.music.ui.component.LocalSongMenuFooterHeight
import com.metrolist.music.ui.component.LocalMenuState
import com.metrolist.music.ui.component.collapsedAnchor
import com.metrolist.music.ui.component.dismissedAnchor
import com.metrolist.music.ui.component.persistablePlayerAnchor
import com.metrolist.music.ui.component.rememberBottomSheetState
import com.metrolist.music.ui.component.shimmer.ShimmerTheme
import com.metrolist.music.ui.menu.YouTubeSongMenu
import com.metrolist.music.ui.player.BottomSheetPlayer
import com.metrolist.music.ui.player.IrideMp3PlayerHandle
import com.metrolist.music.ui.player.IrideBridgeState
import com.metrolist.music.ui.screens.Screens
import com.metrolist.music.ui.screens.NavigationBuilder
import com.metrolist.music.ui.screens.settings.DarkMode
import com.metrolist.music.ui.screens.settings.NavigationTab
import com.metrolist.music.ui.theme.ColorSaver
import com.metrolist.music.ui.theme.DefaultThemeColor
import com.metrolist.music.ui.theme.ForceDarkTheme
import com.metrolist.music.ui.theme.IrideTheme
import com.metrolist.music.ui.theme.extractThemeColor
import com.metrolist.music.ui.utils.appBarScrollBehavior
import com.metrolist.music.ui.utils.IrideMotion
import com.metrolist.music.ui.utils.rememberReducedMotion
import com.metrolist.music.ui.utils.resetHeightOffset
import com.metrolist.music.utils.SyncUtils
import com.metrolist.music.utils.UpdateDownloadState
import com.metrolist.music.utils.UpdateDownloader
import com.metrolist.music.utils.Updater
import com.metrolist.music.utils.dataStore
import com.metrolist.music.utils.get
import com.metrolist.music.utils.rememberEnumPreference
import com.metrolist.music.utils.rememberPreference
import com.metrolist.music.utils.reportException
import com.metrolist.music.utils.setAppLocale
import com.metrolist.music.viewmodels.HomeViewModel
import com.metrolist.music.viewmodels.SharedContentViewModel
import com.valentinilk.shimmer.LocalShimmerTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.net.URLDecoder
import java.net.URLEncoder
import java.util.Locale
import javax.inject.Inject

@Suppress("DEPRECATION", "ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE")
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    companion object {
        private const val ACTION_SEARCH = "com.metrolist.music.action.SEARCH"
        private const val ACTION_LIBRARY = "com.metrolist.music.action.LIBRARY"
        const val ACTION_RECOGNITION = "com.metrolist.music.action.RECOGNITION"
        const val EXTRA_AUTO_START_RECOGNITION = "auto_start_recognition"
        private const val FIRST_FRAME_HOLD_TIMEOUT_MS = 200L

        /** Reopening after at least this long counts as a fresh session, not a quick app switch. */
        private const val COLD_SESSION_GAP_MS = 8L * 60 * 60 * 1000

        /** Cap on the changelog excerpt persisted for the update announcement screen. */
        private const val MAX_PENDING_UPDATE_NOTES_LENGTH = 2500
    }

    @Inject
    lateinit var database: MusicDatabase

    @Inject
    lateinit var downloadUtil: DownloadUtil

    @Inject
    lateinit var syncUtils: SyncUtils

    @Inject
    lateinit var listenTogetherManager: com.metrolist.music.listentogether.ListenTogetherManager

    private lateinit var navController: NavHostController
    private var pendingIntent: Intent? = null
    private var latestVersionName by mutableStateOf(BuildConfig.VERSION_NAME)

    private var playerConnection: PlayerConnection? = null
    private var playerConnectionSnapshot by mutableStateOf<PlayerConnection?>(null)
    private var isServiceBound = false
    private var updateDownloadReceiver: BroadcastReceiver? = null

    private val serviceConnection =
        object : ServiceConnection {
            override fun onServiceConnected(
                name: ComponentName?,
                service: IBinder?,
            ) {
                if (service is MusicBinder) {
                    try {
                        playerConnection = PlayerConnection(this@MainActivity, service, database, lifecycleScope)
                        playerConnectionSnapshot = playerConnection
                        Timber.tag("MainActivity").d("PlayerConnection created successfully")
                        listenTogetherManager.setPlayerConnection(playerConnection)
                    } catch (e: Exception) {
                        Timber.tag("MainActivity").e(e, "Failed to create PlayerConnection")
                        lifecycleScope.launch {
                            delay(500)
                            try {
                                playerConnection = PlayerConnection(this@MainActivity, service, database, lifecycleScope)
                                playerConnectionSnapshot = playerConnection
                                listenTogetherManager.setPlayerConnection(playerConnection)
                            } catch (e2: Exception) {
                                Timber.tag("MainActivity").e(e2, "Failed to create PlayerConnection on retry")
                            }
                        }
                    }
                }
            }

            override fun onServiceDisconnected(name: ComponentName?) {
                listenTogetherManager.setPlayerConnection(null)
                playerConnection?.dispose()
                playerConnection = null
                playerConnectionSnapshot = null
            }
        }

    private fun safeUnbindService(source: String) {
        if (!isServiceBound) return
        try {
            unbindService(serviceConnection)
        } catch (e: IllegalArgumentException) {
            Timber.tag("MainActivity").w(e, "Service was not bound when attempting to unbind in $source")
        } finally {
            isServiceBound = false
            listenTogetherManager.setPlayerConnection(null)
            playerConnection?.dispose()
            playerConnection = null
            playerConnectionSnapshot = null
        }
    }

    private fun registerUpdateDownloadReceiver() {
        updateDownloadReceiver =
            object : BroadcastReceiver() {
                override fun onReceive(
                    context: Context?,
                    intent: Intent?,
                ) {
                    val downloadId = intent?.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1L) ?: return
                    if (UpdateDownloader.onDownloadCompleted(this@MainActivity, downloadId)) {
                        UpdateDownloader.promptInstall(this@MainActivity)
                    }
                }
            }
        ContextCompat.registerReceiver(
            this,
            updateDownloadReceiver,
            IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE),
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
    }

    override fun onStart() {
        super.onStart()
        // Optional notifications are disabled; do not request notification permission.
        // Re-asking here on every launch would only nag users who deliberately declined:
        // Android silences the prompt after two denials anyway, so the call is dead weight.

        if (!MusicService.isRunning) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                ContextCompat.startForegroundService(this, Intent(this, MusicService::class.java))
            } else {
                startService(Intent(this, MusicService::class.java))
            }
        }
        if (!isServiceBound) {
            bindService(
                Intent(this, MusicService::class.java),
                serviceConnection,
                BIND_AUTO_CREATE,
            )
            isServiceBound = true
        }
    }

    override fun onStop() {
        super.onStop()
    }

    override fun onResume() {
        super.onResume()
        lifecycleScope.launch {
            val prefs = dataStore.data.first()
            val cookie = prefs[InnerTubeCookieKey]
            val visitorData = prefs[VisitorDataKey]

            if (!cookie.isNullOrEmpty()) {
                YouTube.cookie = cookie
            }
            if (!visitorData.isNullOrEmpty() && visitorData != "null") {
                YouTube.visitorData = visitorData
            }
            prefs[DataSyncIdKey]?.takeIf { it.isNotEmpty() }?.let { raw ->
                YouTube.dataSyncId = if (raw.contains("||")) {
                    if (raw.endsWith("||")) raw.substringBefore("||") else raw.substringAfter("||")
                } else {
                    raw
                }
            }

            if ((cookie != null && YouTube.cookie == null) ||
                (visitorData != null && YouTube.visitorData == null)) {
                (application as App).initializeSettings()
            }
        }
    }

    private fun persistableAnchor(anchor: Int): Int = persistablePlayerAnchor(anchor)

    override fun onDestroy() {
        updateDownloadReceiver?.let {
            runCatching { unregisterReceiver(it) }
        }
        updateDownloadReceiver = null
        if (isFinishing) {
            listenTogetherManager.disconnect()
            val anchor = persistableAnchor(App.playerAnchorCache)
            App.setPlayerAnchorCache(anchor)
            CoroutineScope(Dispatchers.IO).launch {
                dataStore.edit { it[PlayerAnchorKey] = anchor }
            }
        }
        super.onDestroy()
        val stopServiceOnClear =
            dataStore.get(StopMusicOnTaskClearKey, false) &&
                playerConnection?.isPlaying?.value == true &&
                isFinishing
        if (isServiceBound) {
            safeUnbindService("onDestroy()")
        }
        if (stopServiceOnClear) {
            stopService(Intent(this, MusicService::class.java))
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (::navController.isInitialized) {
            handleDeepLinkIntent(intent, navController)
        } else {
            pendingIntent = intent
        }
    }

    @SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        // Theme choice before the first frame. These reads go through the snapshot-backed
        // DataStore operator: after App.onCreate has warmed the store they never touch disk,
        // so unlike the previous runBlocking round trip they cannot stall the main thread
        // even on a cold process.
        val darkModeRaw = dataStore[DarkModeKey]
        val pureBlackPreferred = dataStore[PureBlackKey] ?: false
        val systemDark =
            (resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) ==
                android.content.res.Configuration.UI_MODE_NIGHT_YES
        val useDark = when (darkModeRaw) {
            "ON" -> true
            "OFF" -> false
            else -> systemDark
        }
        // Light theme is hidden while ForceDarkTheme is on: always boot with the dark splash.
        if (!ForceDarkTheme && !useDark && !pureBlackPreferred) {
            setTheme(R.style.Theme_Metrolist_Splash_Light)
        } else {
            setTheme(R.style.Theme_Metrolist_Splash)
        }
        installSplashScreen()
        super.onCreate(savedInstanceState)
        requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        window.decorView.layoutDirection = View.LAYOUT_DIRECTION_LTR
        WindowCompat.setDecorFitsSystemWindows(window, false)

        listenTogetherManager.initialize()

        if (BuildConfig.UPDATER_AVAILABLE) {
            UpdateDownloader.cleanupStaleDownloads(this)
            registerUpdateDownloadReceiver()
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            val locale =
                dataStore[AppLanguageKey]
                    ?.takeUnless { it == SYSTEM_DEFAULT }
                    ?.let { Locale.forLanguageTag(it) }
                    ?: Locale.getDefault()
            setAppLocale(this, locale)
        }

        lifecycleScope.launch {
            dataStore.data
                .map { it[DisableScreenshotKey] ?: false }
                .distinctUntilChanged()
                .collectLatest {
                    if (it) {
                        window.setFlags(
                            WindowManager.LayoutParams.FLAG_SECURE,
                            WindowManager.LayoutParams.FLAG_SECURE,
                        )
                    } else {
                        window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
                    }
                }
        }

        holdFirstFrameUntilReady()

        setContent {
            IrideApp(
                latestVersionName = latestVersionName,
                onLatestVersionNameChange = { latestVersionName = it },
                playerConnection = playerConnectionSnapshot,
                database = database,
                downloadUtil = downloadUtil,
                syncUtils = syncUtils,
            )
        }
    }

    /**
     * Holds the very first frame back so the app is published with its layout already settled
     * against the real window insets, rather than publishing one laid out against a 0dp navigation
     * bar and visibly re-laying the mini player out when the insets land.
     *
     * Insets are dispatched before the first measure/layout, but the composition reading them
     * re-runs after it — so the second traversal is the first whose layout is settled. That is the
     * entire wait: roughly one frame, not a fixed delay. Composition, measure and layout keep
     * running throughout; only the publish waits, which is what the platform's splash handoff is
     * for. The deadline is a safety net for a device that never produces a second traversal.
     *
     * Deliberately NOT waiting on the MusicService binding: that used to matter only because
     * LocalPlayerConnection was a static CompositionLocal whose null -> instance flip recomposed
     * the world. With that fixed the bind is invisible, and waiting on it just held the launch —
     * and everything downstream of it, artwork included — for hundreds of milliseconds.
     */
    private fun holdFirstFrameUntilReady() {
        val content = findViewById<View>(android.R.id.content)
        val deadline = SystemClock.uptimeMillis() + FIRST_FRAME_HOLD_TIMEOUT_MS
        var traversals = 0
        content.viewTreeObserver.addOnPreDrawListener(
            object : ViewTreeObserver.OnPreDrawListener {
                override fun onPreDraw(): Boolean {
                    val ready = ++traversals >= 2 || SystemClock.uptimeMillis() >= deadline
                    if (ready) {
                        content.viewTreeObserver.removeOnPreDrawListener(this)
                    } else {
                        content.postInvalidateOnAnimation()
                    }
                    return ready
                }
            },
        )
    }

    private fun isColdSessionLaunch(): Boolean {
        val endedAt = dataStore[LastSessionEndedAtKey] ?: return true
        return System.currentTimeMillis() - endedAt >= COLD_SESSION_GAP_MS
    }

    @SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun IrideApp(
        latestVersionName: String,
        onLatestVersionNameChange: (String) -> Unit,
        playerConnection: PlayerConnection?,
        database: MusicDatabase,
        downloadUtil: DownloadUtil,
        syncUtils: SyncUtils,
    ) {
        val checkForUpdates by rememberPreference(CheckForUpdatesKey, defaultValue = true)

        // Single silent update check per launch (cache-aware). When a compatible update is
        // found the APK is downloaded in the background and its metadata persisted; nothing
        // is shown during this session — the announcement interstitial surfaces on the NEXT
        // launch, driven purely by the persisted state so it renders instantly, offline too.
        // The Settings badge keeps working off latestVersionName.
        if (BuildConfig.UPDATER_AVAILABLE) {
            LaunchedEffect(checkForUpdates) {
                if (!checkForUpdates) {
                    onLatestVersionNameChange(BuildConfig.VERSION_NAME)
                    return@LaunchedEffect
                }

                delay(20_000)

                val lastCheckAt = withContext(Dispatchers.IO) { dataStore.get(LastUpdateCheckKey, 0L) }
                if (System.currentTimeMillis() - lastCheckAt <= Updater.CHECK_INTERVAL_MILLIS) {
                    onLatestVersionNameChange(BuildConfig.VERSION_NAME)
                    return@LaunchedEffect
                }

                withContext(Dispatchers.IO) {
                    Updater.checkForAnyUpdate(forceRefresh = false)
                        .onSuccess { result ->
                            val (releaseInfo, hasUpdate) = result
                            dataStore.edit { prefs ->
                                prefs[LastUpdateCheckKey] = System.currentTimeMillis()
                            }
                            val info = releaseInfo
                            if (info == null) {
                                onLatestVersionNameChange(BuildConfig.VERSION_NAME)
                                return@onSuccess
                            }
                            onLatestVersionNameChange(info.versionName)

                            val tag = info.tagName
                            val forceTest = Updater.FORCE_UPDATE_ANNOUNCEMENT_FOR_TESTING
                            // The test flag runs BEFORE the version comparison: while it is on
                            // the announcement fires against the real latest release even when
                            // this build is already current (that comparison would otherwise
                            // bail first and nothing could ever be exercised).
                            if (!forceTest && Updater.compareVersions(BuildConfig.VERSION_NAME, tag) >= 0) {
                                // Genuinely up to date: clear any leftover announcement state
                                // from an install that already happened.
                                dataStore.edit { prefs ->
                                    prefs.remove(PendingUpdateTagKey)
                                    prefs.remove(PendingUpdateVersionNameKey)
                                    prefs.remove(PendingUpdateNotesKey)
                                }
                                return@onSuccess
                            }

                            val effectiveHasUpdate = hasUpdate || forceTest
                            if (!effectiveHasUpdate) return@onSuccess

                            val downloadUrl =
                                Updater.getDownloadUrlForCurrentVariant(info)
                            if (downloadUrl == null) {
                                Timber.tag("UpdateAnnouncement")
                                    .w("Release %s has no APK asset for arch/variant", tag)
                                return@onSuccess
                            }
                            if (!(dataStore[OnboardingCompletedKey] ?: false)) return@onSuccess

                            val pendingTag = dataStore[PendingUpdateTagKey]
                            if (pendingTag != tag) {
                                Timber.tag("UpdateAnnouncement")
                                    .i("Pending update %s (%s), starting silent download", info.versionName, tag)
                                dataStore.edit { prefs ->
                                    prefs[PendingUpdateTagKey] = tag
                                    prefs[PendingUpdateVersionNameKey] = info.versionName
                                    prefs[PendingUpdateNotesKey] = info.description.take(MAX_PENDING_UPDATE_NOTES_LENGTH)
                                }
                                UpdateDownloader.enqueueUpdate(
                                    applicationContext,
                                    downloadUrl,
                                    info.versionName,
                                )
                            } else when (UpdateDownloader.queryDownloadState(applicationContext)) {
                                // Same version already announced: only re-kick a download that
                                // failed on a previous launch; a completed or running one is left alone.
                                is UpdateDownloadState.Failed -> {
                                    Timber.tag("UpdateAnnouncement")
                                        .w("Previous download of %s failed, retrying", tag)
                                    UpdateDownloader.enqueueUpdate(
                                        applicationContext,
                                        downloadUrl,
                                        info.versionName,
                                    )
                                }
                                else -> Unit
                            }
                        }
                        .onFailure {
                            Timber.tag("UpdateAnnouncement").w(it, "Silent update check failed")
                        }
                }
            }
        }

        // Full-screen "update ready" gate for this launch. Computed once, synchronously, from
        // persisted state: no network involved, so the wall replaces the app immediately even
        // before the check above completes. It only fires when a previous launch already
        // detected and downloaded the update. The test flag bypasses BOTH the version
        // comparison and the dismissed-version memory, so the announcement can be exercised
        // repeatedly against the real latest release.
        val showUpdateGateInitially = remember {
            BuildConfig.UPDATER_AVAILABLE &&
                isColdSessionLaunch() &&
                (dataStore[OnboardingCompletedKey] ?: false) && run {
                    val tag = dataStore[PendingUpdateTagKey]
                    tag != null &&
                        (Updater.FORCE_UPDATE_ANNOUNCEMENT_FOR_TESTING ||
                            (Updater.compareVersions(BuildConfig.VERSION_NAME, tag) < 0 &&
                                tag != dataStore[UpdateAnnouncementDismissedTagKey]))
                }
        }
        var showUpdateInterstitial by rememberSaveable { mutableStateOf(showUpdateGateInitially) }

        val enableDynamicTheme by rememberPreference(DynamicThemeKey, defaultValue = true)
        val enableHighRefreshRate by rememberPreference(EnableHighRefreshRateKey, defaultValue = true)

        LaunchedEffect(enableHighRefreshRate) {
            val window = this@MainActivity.window
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val layoutParams = window.attributes
                if (enableHighRefreshRate) {
                    layoutParams.preferredDisplayModeId = 0
                } else {
                    val modes = window.windowManager.defaultDisplay.supportedModes
                    val mode60 =
                        modes.firstOrNull { kotlin.math.abs(it.refreshRate - 60f) < 1f }
                            ?: modes.minByOrNull { kotlin.math.abs(it.refreshRate - 60f) }

                    if (mode60 != null) {
                        layoutParams.preferredDisplayModeId = mode60.modeId
                    }
                }
                window.attributes = layoutParams
            } else {
                val params = window.attributes
                if (enableHighRefreshRate) {
                    params.preferredRefreshRate = 0f
                } else {
                    params.preferredRefreshRate = 60f
                }
                window.attributes = params
            }
        }

        val darkTheme by rememberEnumPreference(DarkModeKey, defaultValue = DarkMode.AUTO)
        val isSystemInDarkTheme = isSystemInDarkTheme()
        val useDarkTheme =
            remember(darkTheme, isSystemInDarkTheme) {
                ForceDarkTheme ||
                    if (darkTheme == DarkMode.AUTO) isSystemInDarkTheme else darkTheme == DarkMode.ON
            }

        val pureBlackEnabled by rememberPreference(PureBlackKey, defaultValue = false)
        val pureBlack =
            remember(pureBlackEnabled, useDarkTheme) {
                pureBlackEnabled && useDarkTheme
            }

        val mainTopGradientEnabled by rememberPreference(MainTopGradientKey, defaultValue = false)
        val playerBackgroundStyle by rememberEnumPreference(
            PlayerBackgroundStyleKey,
            defaultValue = PlayerBackgroundStyle.BETTER_ANIMATED_GRADIENT,
        )
        val topGradientMediaMetadata by remember(playerConnection) {
            playerConnection?.mediaMetadata ?: MutableStateFlow<MediaMetadata?>(null)
        }.collectAsStateWithLifecycle()

        val (selectedThemeColorInt) = rememberPreference(SelectedThemeColorKey, defaultValue = DefaultThemeColor.toArgb())
        val selectedThemeColor = Color(selectedThemeColorInt)

        var targetThemeColor by rememberSaveable(stateSaver = ColorSaver) {
            mutableStateOf(selectedThemeColor)
        }
        var animateThemeColor by remember { mutableStateOf(false) }
        LaunchedEffect(Unit) {
            delay(1200)
            animateThemeColor = true
        }
        val themeColor by animateColorAsState(
            targetValue = targetThemeColor,
            animationSpec = if (animateThemeColor) tween(durationMillis = 600) else snap(),
            label = "themeColor"
        )
        val themeColorCache = remember { LinkedHashMap<String, Color>(21, 0.75f, true) }

        LaunchedEffect(selectedThemeColor) {
            if (!enableDynamicTheme) {
                targetThemeColor = selectedThemeColor
            }
        }

        LaunchedEffect(playerConnection, enableDynamicTheme, selectedThemeColor) {
            val playerConnection = playerConnection
            if (!enableDynamicTheme || playerConnection == null) {
                targetThemeColor = selectedThemeColor
                return@LaunchedEffect
            }

            playerConnection.service.currentMediaMetadata.collectLatest { song ->
                val thumbnailUrl = song?.thumbnailUrl
                if (thumbnailUrl != null) {
                    val cached = themeColorCache[thumbnailUrl]
                    if (cached != null) {
                        targetThemeColor = cached
                        return@collectLatest
                    }

                    withContext(Dispatchers.IO) {
                        try {
                            val result =
                                imageLoader.execute(
                                    ImageRequest
                                        .Builder(this@MainActivity)
                                        .data(thumbnailUrl)
                                        .allowHardware(false)
                                        .memoryCachePolicy(CachePolicy.ENABLED)
                                        .diskCachePolicy(CachePolicy.ENABLED)
                                        .networkCachePolicy(CachePolicy.ENABLED)
                                        .crossfade(false)
                                        .build(),
                                )
                            val extractedColor = result.image?.toBitmap()?.extractThemeColor() ?: selectedThemeColor
                            targetThemeColor = extractedColor
                            themeColorCache[thumbnailUrl] = extractedColor
                            if (themeColorCache.size > 20) {
                                themeColorCache.entries.iterator().also {
                                    it.next()
                                    it.remove()
                                }
                            }
                        } catch (e: Exception) {
                            targetThemeColor = selectedThemeColor
                        }
                    }
                } else {
                    targetThemeColor = selectedThemeColor
                }
            }
        }

        IrideTheme(
            darkTheme = useDarkTheme,
            pureBlack = pureBlack,
            themeColor = themeColor,
        ) {
            BoxWithConstraints(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .background(if (pureBlack) Color.Black else MaterialTheme.colorScheme.surface),
            ) {
                val density = LocalDensity.current
                val configuration = LocalWindowInfo.current
                val cutoutInsets = WindowInsets.displayCutout
                val windowsInsets = WindowInsets.systemBars
                val insetLayoutKey = "${resources.configuration.orientation}_${density.density}"
                val liveBottomInsetPx = windowsInsets.getBottom(density)
                val rootInsets = androidx.core.view.ViewCompat.getRootWindowInsets(window.decorView)
                val bottomInsetPx = if (liveBottomInsetPx > 0) liveBottomInsetPx else {
                    rootInsets?.getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars())?.bottom
                        ?: MiniPlayerSnapshotStore.bottomInsetPx(insetLayoutKey)
                }
                val bottomInset = with(density) { bottomInsetPx.toDp() }
                LaunchedEffect(insetLayoutKey, bottomInsetPx) {
                    MiniPlayerSnapshotStore.updateBottomInset(insetLayoutKey, bottomInsetPx)
                }
                val bottomInsetDp = WindowInsets.systemBars.asPaddingValues().calculateBottomPadding()

                val navController = rememberNavController()

                LaunchedEffect(Unit) {
                    if (dataStore.data.first()[SimpMusicMigrationDoneKey] != true) {
                        dataStore.edit { settings ->
                            val currentOrder = settings[LyricsProviderOrderKey] ?: ""
                            if (currentOrder.contains("SimpMusic") || !currentOrder.contains("Paxsenix")) {
                                val orderList = currentOrder.split(",")
                                    .map { it.trim() }
                                    .filter { it.isNotBlank() && it != "SimpMusic" }
                                    .toMutableList()
                                if (!orderList.contains("Paxsenix")) {
                                    orderList.add("Paxsenix")
                                }
                                settings[LyricsProviderOrderKey] = orderList.joinToString(",")
                            }

                            if (settings[PreferredLyricsProviderKey] == "SIMPMUSIC") {
                                settings[PreferredLyricsProviderKey] = PreferredLyricsProvider.LRCLIB.name
                            }

                            settings[SimpMusicMigrationDoneKey] = true
                        }
                    }
                }

                val homeViewModel: HomeViewModel = hiltViewModel()
                val sharedContentViewModel: SharedContentViewModel = hiltViewModel()
                val accountImageUrl by homeViewModel.accountImageUrl.collectAsState()
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val (previousTab, setPreviousTab) = rememberSaveable { mutableStateOf("home") }

                val (listenTogetherInTopBar) = rememberPreference(ListenTogetherInTopBarKey, defaultValue = true)
                val (compactTopNavigationBar) = rememberPreference(CompactTopNavigationBarKey, defaultValue = true)
                val navigationItems =
                    remember(listenTogetherInTopBar) {
                        val filtered = Screens.MainScreens.filter {
                            it != Screens.ListenTogether || !listenTogetherInTopBar
                        }
                        filtered.sortedBy { screen ->
                            when (screen) {
                                Screens.Home -> 0
                                Screens.Library -> 1
                                Screens.Account -> 2
                                Screens.Search -> 3
                                else -> 4
                            }
                        }
                    }
                val composeNavigator = remember(navController) {
                    navController.navigatorProvider.getNavigator<ComposeNavigator>("composable")
                }
                // NavHost attaches its navigator later in this composition. Wait for its first
                // entry before accessing navigator state, including when restoring a saved stack.
                val navigationBackStack = if (navBackStackEntry != null && composeNavigator.isAttached) {
                    composeNavigator.backStack.collectAsState().value
                } else {
                    emptyList()
                }
                val activeNavigationRoute = navigationBackStack.lastOrNull { entry ->
                    navigationItems.any { it.route == entry.destination.route }
                }?.destination?.route
                val defaultOpenTab =
                    remember {
                        dataStore[DefaultOpenTabKey].toEnum(defaultValue = NavigationTab.HOME)
                    }
                val tabOpenedFromShortcut =
                    remember {
                        when (intent?.action) {
                            ACTION_SEARCH -> NavigationTab.LIBRARY
                            ACTION_LIBRARY -> NavigationTab.SEARCH
                            else -> null
                        }
                    }

                val topLevelScreens =
                    remember {
                        listOf(
                            Screens.Home.route,
                            Screens.Library.route,
                            Screens.ListenTogether.route,
                            "settings",
                        )
                    }

                val (query, onQueryChange) =
                    rememberSaveable(stateSaver = TextFieldValue.Saver) {
                        mutableStateOf(TextFieldValue())
                    }

                val onSearch: (String) -> Unit =
                    remember {
                        { searchQuery ->
                            if (searchQuery.isNotEmpty()) {
                                navController.navigate("search/${URLEncoder.encode(searchQuery, "UTF-8")}")

                                if (dataStore[PauseSearchHistoryKey] != true) {
                                    lifecycleScope.launch(Dispatchers.IO) {
                                        database.query {
                                            insert(SearchHistory(query = searchQuery))
                                        }
                                    }
                                }
                            }
                        }
                    }

                val currentRoute by remember {
                    derivedStateOf { navBackStackEntry?.destination?.route }
                }

                val isFullscreenStory = currentRoute == "wrapped" || currentRoute?.startsWith("reverie/") == true
                val inSearchScreen by remember {
                    derivedStateOf { currentRoute?.startsWith("search/") == true }
                }
                val isTopLevelRoute by remember {
                    derivedStateOf {
                        currentRoute == null ||
                            navigationItems.any { it.route == currentRoute } ||
                            currentRoute?.startsWith("search/") == true
                    }
                }
                val isLandscape = configuration.containerDpSize.width > configuration.containerDpSize.height

                val showRail = false

                val curtainMode = !showRail

                val showTopGradientTarget = mainTopGradientEnabled && !pureBlack &&
                    currentRoute?.startsWith("settings") == true && currentRoute != Screens.Account.route
                val topGradientAlpha by animateFloatAsState(
                    targetValue = if (showTopGradientTarget) 1f else 0f,
                    animationSpec = tween(400),
                    label = "topGradientVisibility",
                )

                val playerTopPeek = if (curtainMode) maxHeight * 0.14f else 0.dp

                val playerBottomSheetState =
                    rememberBottomSheetState(
                        dismissedBound = 0.dp,
                        initialAnchor = persistableAnchor(App.playerAnchorCache),
                        collapsedBound = if (!showRail) {
                            bottomInset + (if (curtainMode) IrideMiniPlayerHeight + IrideFolio.MiniPlayerTopSpacing else MiniPlayerHeight + FloatingPillBottomSpacing)
                        } else {
                            bottomInset + MiniPlayerHeight
                        },
                        expandedBound = maxHeight - playerTopPeek,
                        preventDismissDrag = curtainMode,
                        onAnchorPersist = { anchor ->
                            App.setPlayerAnchorCache(anchor)
                            lifecycleScope.launch(Dispatchers.IO) {
                                dataStore.edit { it[PlayerAnchorKey] = persistableAnchor(anchor) }
                            }
                        },
                    )

                val playerCoversSystemBars by remember(playerBottomSheetState) {
                    derivedStateOf { playerBottomSheetState.progress >= 0.95f }
                }
                LaunchedEffect(useDarkTheme, playerCoversSystemBars, isFullscreenStory) {
                    setSystemBarAppearance(useDarkTheme || playerCoversSystemBars || isFullscreenStory)
                }

                val lifecycleOwner = LocalLifecycleOwner.current
                DisposableEffect(playerBottomSheetState, lifecycleOwner) {
                    val observer = LifecycleEventObserver { _, event ->
                        if (event == Lifecycle.Event.ON_STOP) {
                            lifecycleScope.launch(Dispatchers.IO) {
                                dataStore.edit {
                                    it[PlayerAnchorKey] = persistableAnchor(App.playerAnchorCache)
                                    it[LastSessionEndedAtKey] = System.currentTimeMillis()
                                }
                            }
                        }
                    }
                    lifecycleOwner.lifecycle.addObserver(observer)
                    onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
                }

                val curtainActive = curtainMode

                val irideBridgeState = remember { IrideBridgeState() }

                val playerAwareWindowInsets =
                    remember(bottomInset, showRail, isTopLevelRoute, curtainActive, playerBottomSheetState.isDismissed) {
                        var bottom = bottomInset
                        if (curtainActive) {
                            bottom = playerBottomSheetState.collapsedBound + FloatingBottomNavigationHeight + 8.dp
                        } else if (!showRail) {
                            bottom += MiniPlayerHeight + FloatingPillBottomSpacing
                        } else {
                            if (!playerBottomSheetState.isDismissed) bottom += MiniPlayerHeight
                        }
                        windowsInsets
                            .only(WindowInsetsSides.Horizontal + WindowInsetsSides.Top)
                            .add(WindowInsets(top = AppBarHeight, bottom = bottom))
                    }
                appBarScrollBehavior(
                    canScroll = {
                        !inSearchScreen &&
                            (playerBottomSheetState.isCollapsed || playerBottomSheetState.isDismissed)
                    },
                )

                val topAppBarScrollBehavior =
                    appBarScrollBehavior(
                        canScroll = {
                            !inSearchScreen &&
                                (playerBottomSheetState.isCollapsed || playerBottomSheetState.isDismissed)
                        },
                    )

                LaunchedEffect(navBackStackEntry) {
                    if ((currentRoute == Screens.Search.route || inSearchScreen) && playerBottomSheetState.progress > 0f) {
                        playerBottomSheetState.collapseSoft()
                    }
                    if (inSearchScreen) {
                        val searchQuery =
                            withContext(Dispatchers.IO) {
                                val rawQuery = navBackStackEntry?.arguments?.getString("query") ?: ""
                                try {
                                    URLDecoder.decode(rawQuery, "UTF-8")
                                } catch (e: IllegalArgumentException) {
                                    rawQuery
                                }
                            }
                        onQueryChange(
                            TextFieldValue(
                                searchQuery,
                                TextRange(searchQuery.length),
                            ),
                        )
                    } else if (navigationItems.fastAny { it.route == navBackStackEntry?.destination?.route }) {
                        onQueryChange(TextFieldValue())
                    }

                    if (navigationItems.fastAny { it.route == navBackStackEntry?.destination?.route }) {
                        if (navigationItems.fastAny { it.route == previousTab }) {
                            topAppBarScrollBehavior.state.resetHeightOffset()
                        }
                    }

                    topAppBarScrollBehavior.state.resetHeightOffset()

                    navController.currentBackStackEntry?.destination?.route?.let {
                        setPreviousTab(it)
                    }
                }

                LaunchedEffect(playerConnection, curtainMode) {
                    val connection = playerConnection ?: return@LaunchedEffect
                    connection.mediaMetadata.collectLatest { metadata ->
                        val mediaId = metadata?.id
                        if (curtainMode && mediaId != null &&
                            playerBottomSheetState.isDismissed
                        ) {
                            playerBottomSheetState.collapseSoft()
                        } else if (!curtainMode && metadata == null) {
                            if (!playerBottomSheetState.isDismissed) {
                                playerBottomSheetState.dismiss()
                            }
                        }
                    }
                }

                val coroutineScope = rememberCoroutineScope()
                var sharedSong: SongItem? by remember {
                    mutableStateOf(null)
                }
                val snackbarHostState = remember { SnackbarHostState() }

                LaunchedEffect(Unit) {
                    val initialPendingIntent = pendingIntent
                    if (initialPendingIntent != null) {
                        handleRecognitionIntent(initialPendingIntent, navController)
                        handleDeepLinkIntent(initialPendingIntent, navController)
                        pendingIntent = null
                    } else {
                        handleRecognitionIntent(intent, navController)
                        handleDeepLinkIntent(intent, navController)
                    }
                }

                DisposableEffect(Unit) {
                    val listener =
                        Consumer<Intent> { intent ->
                            handleRecognitionIntent(intent, navController)
                            handleDeepLinkIntent(intent, navController)
                        }

                    addOnNewIntentListener(listener)
                    onDispose { removeOnNewIntentListener(listener) }
                }

                val baseBg = if (pureBlack) Color.Black else MaterialTheme.colorScheme.surfaceContainer

                val onNavItemClick: (Screens, Boolean) -> Unit =
                    remember(navController, composeNavigator, navigationItems, coroutineScope, topAppBarScrollBehavior, playerBottomSheetState) {
                        navigate@ { screen: Screens, _: Boolean ->
                            if (!composeNavigator.isAttached) return@navigate
                            if (playerBottomSheetState.isExpanded) {
                                playerBottomSheetState.collapseSoft()
                            }
                            val activeRoot = composeNavigator.backStack.value.lastOrNull { entry ->
                                navigationItems.any { it.route == entry.destination.route }
                            }?.destination?.route
                            if (activeRoot == screen.route) {
                                navController.popBackStack(screen.route, inclusive = false)
                                val targetEntry = navController.currentBackStackEntry
                                when (screen) {
                                    Screens.Search -> {
                                        val count = targetEntry?.savedStateHandle?.get<Int>("scrollToTopCount") ?: 0
                                        targetEntry?.savedStateHandle?.set("scrollToTopCount", count + 1)
                                    }
                                    Screens.Library -> targetEntry?.savedStateHandle?.set("resetLibrary", true)
                                    else -> targetEntry?.savedStateHandle?.set("scrollToTop", true)
                                }
                            } else {
                                navController.navigate(screen.route) {
                                    // Save the root too: Home can own an album/artist stack.
                                    if (activeRoot != null) {
                                        popUpTo(activeRoot) {
                                            inclusive = true
                                            saveState = true
                                        }
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                            coroutineScope.launch { topAppBarScrollBehavior.state.resetHeightOffset() }
                        }
                    }

                val topNavBarController = remember(navigationItems, activeNavigationRoute, currentRoute, onNavItemClick, compactTopNavigationBar, accountImageUrl) {
                    TopNavBarController(
                        navigationItems = navigationItems,
                        currentRoute = activeNavigationRoute ?: currentRoute,
                        onItemClick = onNavItemClick,
                        compact = compactTopNavigationBar,
                        accountImageUrl = accountImageUrl,
                    )
                }
                val navigationBackdrop = rememberFrostBackdrop(
                    blurredSampleHeight = playerBottomSheetState.collapsedBound +
                        FloatingBottomNavigationHeight + IrideFolio.NavigationFrostBlurRadius * 3f + 2.dp,
                    blurredScale = 0.5f,
                    rasterizeContent = true,
                    stableBlur = true,
                )
                val songMenuState = LocalMenuState.current
                val songMenuBackdrop = rememberFrostBackdrop(
                    blurredScale = 0.5f,
                    rasterizeContent = true,
                    stableBlur = true,
                )
                val pageNavigationAppearance = remember(navBackStackEntry) {
                    PageNavigationAppearance(navBackStackEntry?.id)
                }
                val floatingPanelAppearance = rememberFloatingPanelAppearance(pageNavigationAppearance, navigationBackdrop)
                val pageBackgroundBackdrop = rememberFrostBackdrop(
                    backgroundColor = MaterialTheme.colorScheme.surface,
                    captureOnly = true,
                )
                // Cache the small blur steps instead of allocating a native effect on each animation frame.
                val playerPageBlurEffects = remember(density.density) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        List(21) { step ->
                            if (step == 0) null else {
                                val radius = with(density) { 5.dp.toPx() } * step / 20f
                                androidx.compose.ui.graphics.BlurEffect(
                                    radius, radius, androidx.compose.ui.graphics.TileMode.Clamp,
                                )
                            }
                        }
                    } else emptyList()
                }
                val folioDragOverlay = remember { com.metrolist.music.ui.component.IrideFolioDragOverlayState() }

                var topGradientScrimColor by remember(topGradientMediaMetadata?.thumbnailUrl, playerBackgroundStyle) {
                    mutableStateOf(Color.Black)
                }
                CompositionLocalProvider(
                    androidx.compose.foundation.LocalOverscrollFactory provides null,
                    com.metrolist.music.ui.component.LocalFolioDragOverlay provides folioDragOverlay,
                    LocalDatabase provides database,
                    LocalContentColor provides if (pureBlack) Color.White else contentColorFor(MaterialTheme.colorScheme.surface),
                    LocalPlayerConnection provides playerConnection,
                    LocalPlayerAwareWindowInsets provides playerAwareWindowInsets,
                    LocalDownloadUtil provides downloadUtil,
                    LocalShimmerTheme provides ShimmerTheme,
                    LocalSyncUtils provides syncUtils,
                    LocalListenTogetherManager provides listenTogetherManager,
                    LocalTopNavBarController provides topNavBarController,
                    LocalIrideFolioScrimColor provides lerp(Color.Black, topGradientScrimColor, topGradientAlpha),
                    LocalScreenFrostBackground provides pageBackgroundBackdrop,
                    LocalPageNavigationAppearance provides pageNavigationAppearance,
                    LocalFloatingPanelAppearance provides floatingPanelAppearance,
                ) {
                    val playerScreenHeight = maxHeight
                    val floatingNavigationAlpha by remember(playerBottomSheetState) {
                        derivedStateOf { (1f - playerBottomSheetState.progress / 0.35f).coerceIn(0f, 1f) }
                    }
                    val floatingNavigationVisible = !isFullscreenStory &&
                        currentRoute != "onboarding" && floatingNavigationAlpha > 0f

                    // Capture the visible page, player and navigation before drawing any song menu.
                    Box(
                        Modifier.fillMaxSize()
                            .observeSongMenuAnchor(songMenuState)
                            .recordFrostBackdrop(
                                songMenuBackdrop,
                                enabled = songMenuState.isVisible && songMenuState.anchored,
                                trackScroll = true,
                            ),
                    ) {
                        // Blur the page itself so its rounded edges and every exposed area stay continuous.
                        Box(
                            Modifier.fillMaxSize()
                                .invalidateFrostOnChanges {
                                    playerBottomSheetState.progress
                                    folioDragOverlay.progress
                                }
                                .graphicsLayer {
                                    val playerProgress = if (curtainActive && !isFullscreenStory && currentRoute != "onboarding") {
                                        playerBottomSheetState.progress.coerceIn(0f, 1f)
                                    } else 0f
                                    val progress = maxOf(playerProgress, folioDragOverlay.progress)
                                    renderEffect = playerPageBlurEffects.getOrNull((progress * 20f).toInt())
                                    alpha = 1f - 0.14f * progress
                                }
                                .recordFrostBackdrop(navigationBackdrop, trackScroll = true)
                        ) {
                            Scaffold(
                                snackbarHost = { SnackbarHost(snackbarHostState) },
                                // Folio pages own their transparent title and controls.
                                topBar = {},
                                bottomBar = {
                                    if (!curtainMode && !isFullscreenStory && currentRoute != "onboarding" && !playerBottomSheetState.isDismissed) {
                                        BottomSheetPlayer(
                                            state = playerBottomSheetState,
                                            navController = navController,
                                            pureBlack = pureBlack,
                                            showPeekContent = showRail,
                                        )
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxSize(),
                            ) {
                                Row(Modifier.fillMaxSize()) {
                                    val onRailSearchLongClick: () -> Unit =
                                        remember(navController) {
                                            {
                                                navController.navigate("recognition") {
                                                    launchSingleTop = true
                                                }
                                            }
                                        }

                                    if (showRail && !isFullscreenStory && currentRoute != "onboarding") {
                                        AppNavigationRail(
                                            navigationItems = navigationItems,
                                            currentRoute = activeNavigationRoute ?: currentRoute,
                                            onItemClick = onNavItemClick,
                                            pureBlack = pureBlack,
                                            onSearchLongClick = onRailSearchLongClick,
                                            accountImageUrl = accountImageUrl,
                                        )
                                    }
                                    Box(
                                        Modifier
                                            .weight(1f),
                                    ) {
                                        val playerCoversScreen by remember(playerBottomSheetState) {
                                            derivedStateOf { playerBottomSheetState.progress >= 0.99f }
                                        }
                                        Box(
                                            Modifier.fillMaxSize()
                                                .background(MaterialTheme.colorScheme.surface)
                                                .recordFrostBackdrop(pageBackgroundBackdrop),
                                        ) {
                                            if (!playerCoversScreen) {
                                                TopScreenGradientBackground(
                                                    mediaMetadata = topGradientMediaMetadata,
                                                    playerBackground = playerBackgroundStyle,
                                                    onTopColorChanged = { topGradientScrimColor = it },
                                                    modifier = Modifier
                                                        .align(Alignment.TopStart)
                                                        .graphicsLayer { alpha = topGradientAlpha },
                                                )
                                            }
                                        }

                                        val onboardingCompleted = remember { dataStore[OnboardingCompletedKey] ?: false }

                                        fun topLevelIndex(route: String?) = navigationItems.indexOfFirst { it.route == route }

                                        val navAnimationsEnabled = !rememberReducedMotion()
                                        val navEnterDuration = if (navAnimationsEnabled) IrideMotion.Short else 0
                                        val navExitDuration = if (navAnimationsEnabled) IrideMotion.Quick else 0

                                        NavHost(
                                            navController = navController,
                                            startDestination =
                                                if (!onboardingCompleted) {
                                                    "onboarding"
                                                } else {
                                                    when (tabOpenedFromShortcut ?: defaultOpenTab) {
                                                        NavigationTab.HOME -> Screens.Home
                                                        NavigationTab.LIBRARY -> Screens.Library
                                                        else -> Screens.Home
                                                    }.route
                                                },
                                            enterTransition = {
                                                val currentRouteIndex = topLevelIndex(targetState.destination.route)
                                                val previousRouteIndex = topLevelIndex(initialState.destination.route)

                                                if (currentRouteIndex != -1 && previousRouteIndex != -1) {
                                                    fadeIn(tween(navEnterDuration, easing = IrideMotion.EaseOutExpo))
                                                } else if (currentRouteIndex == -1 || currentRouteIndex > previousRouteIndex) {
                                                    slideInHorizontally(tween(navEnterDuration, easing = IrideMotion.EaseOutExpo)) { it / 8 } +
                                                        fadeIn(tween(navEnterDuration, easing = IrideMotion.EaseOutExpo))
                                                } else {
                                                    slideInHorizontally(tween(navEnterDuration, easing = IrideMotion.EaseOutExpo)) { -it / 8 } +
                                                        fadeIn(tween(navEnterDuration, easing = IrideMotion.EaseOutExpo))
                                                }
                                            },
                                            exitTransition = {
                                                val currentRouteIndex = topLevelIndex(initialState.destination.route)
                                                val targetRouteIndex = topLevelIndex(targetState.destination.route)

                                                if (currentRouteIndex != -1 && targetRouteIndex != -1) {
                                                    fadeOut(tween(navExitDuration, easing = IrideMotion.EaseOutQuart))
                                                } else if (targetRouteIndex == -1 || targetRouteIndex > currentRouteIndex) {
                                                    slideOutHorizontally(tween(navExitDuration, easing = IrideMotion.EaseOutQuart)) { -it / 8 } +
                                                        fadeOut(tween(navExitDuration, easing = IrideMotion.EaseOutQuart))
                                                } else {
                                                    slideOutHorizontally(tween(navExitDuration, easing = IrideMotion.EaseOutQuart)) { it / 8 } +
                                                        fadeOut(tween(navExitDuration, easing = IrideMotion.EaseOutQuart))
                                                }
                                            },
                                            popEnterTransition = {
                                                val currentRouteIndex = topLevelIndex(targetState.destination.route)
                                                val previousRouteIndex = topLevelIndex(initialState.destination.route)

                                                if (currentRouteIndex != -1 && previousRouteIndex != -1) {
                                                    fadeIn(tween(navEnterDuration, easing = IrideMotion.EaseOutExpo))
                                                } else if (previousRouteIndex != -1 && previousRouteIndex < currentRouteIndex) {
                                                    slideInHorizontally(tween(navEnterDuration, easing = IrideMotion.EaseOutExpo)) { it / 8 } +
                                                        fadeIn(tween(navEnterDuration, easing = IrideMotion.EaseOutExpo))
                                                } else {
                                                    slideInHorizontally(tween(navEnterDuration, easing = IrideMotion.EaseOutExpo)) { -it / 8 } +
                                                        fadeIn(tween(navEnterDuration, easing = IrideMotion.EaseOutExpo))
                                                }
                                            },
                                            popExitTransition = {
                                                val currentRouteIndex = topLevelIndex(initialState.destination.route)
                                                val targetRouteIndex = topLevelIndex(targetState.destination.route)

                                                if (currentRouteIndex != -1 && targetRouteIndex != -1) {
                                                    fadeOut(tween(navExitDuration, easing = IrideMotion.EaseOutQuart))
                                                } else if (currentRouteIndex != -1 && currentRouteIndex < targetRouteIndex) {
                                                    slideOutHorizontally(tween(navExitDuration, easing = IrideMotion.EaseOutQuart)) { -it / 8 } +
                                                        fadeOut(tween(navExitDuration, easing = IrideMotion.EaseOutQuart))
                                                } else {
                                                    slideOutHorizontally(tween(navExitDuration, easing = IrideMotion.EaseOutQuart)) { it / 8 } +
                                                        fadeOut(tween(navExitDuration, easing = IrideMotion.EaseOutQuart))
                                                }
                                            },
                                            modifier = Modifier.nestedScroll(topAppBarScrollBehavior.nestedScrollConnection),
                                        ) {
                                            NavigationBuilder(
                                                navController = navController,
                                                scrollBehavior = topAppBarScrollBehavior,
                                                latestVersionName = latestVersionName,
                                                activity = this@MainActivity,
                                                snackbarHostState = snackbarHostState,
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Both surfaces sample the page before either panel is drawn and fade together.
                        if (curtainActive && floatingNavigationAlpha > 0f && !isFullscreenStory && currentRoute != "onboarding") {
                            FrostedMiniPlayerBackdrop(
                                collapsedHeight = playerBottomSheetState.collapsedBound,
                                backdrop = navigationBackdrop,
                                contentColor = floatingPanelAppearance.contentColor,
                                backgroundColor = floatingPanelAppearance.backgroundColor,
                                surfaceAlpha = floatingPanelAppearance.surfaceAlpha,
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .graphicsLayer {
                                        alpha = floatingNavigationAlpha
                                    },
                            )
                        }


                        if (curtainActive && !isFullscreenStory && currentRoute != "onboarding") {
                            if (playerBottomSheetState.progress > 0f) {
                                val closePlayerDescription = stringResource(R.string.mp3_close)
                                Box(
                                    modifier = Modifier.fillMaxWidth().height(playerTopPeek)
                                        .semantics { contentDescription = closePlayerDescription }
                                        .draggable(
                                            state = playerBottomSheetState,
                                            orientation = Orientation.Vertical,
                                            enabled = playerBottomSheetState.progress > 0f,
                                            onDragStopped = { velocity -> playerBottomSheetState.performFling(-velocity, null) },
                                        )
                                        .clickable(
                                            interactionSource = remember { MutableInteractionSource() },
                                            indication = null,
                                            enabled = playerBottomSheetState.progress > 0f,
                                            onClick = playerBottomSheetState::collapseSoft,
                                        ),
                                    contentAlignment = Alignment.BottomCenter,
                                ) {}
                            }
                            BottomSheetPlayer(
                                state = playerBottomSheetState,
                                navController = navController,
                                pureBlack = pureBlack,
                                showPeekContent = showRail,
                                bridgeState = irideBridgeState,
                                expandedTopInset = playerTopPeek,
                            )
                            IrideMp3PlayerHandle(
                                state = playerBottomSheetState,
                                screenHeight = playerScreenHeight,
                                isExpandable = topGradientMediaMetadata != null,
                                contentColor = floatingPanelAppearance.contentColor,
                            )
                        }

                        // Navigation stays above the collapsed player's touch layer; menus stay above both.
                        if (currentRoute != "onboarding") {
                            FloatingBottomNavigation(
                                navigationItems = navigationItems,
                                currentRoute = activeNavigationRoute ?: currentRoute,
                                onItemClick = onNavItemClick,
                                backdrop = navigationBackdrop,
                                contentColor = floatingPanelAppearance.contentColor,
                                backgroundColor = floatingPanelAppearance.backgroundColor,
                                surfaceAlpha = floatingPanelAppearance.surfaceAlpha,
                                visible = floatingNavigationVisible,
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .padding(bottom = if (playerBottomSheetState.isDismissed) bottomInset else playerBottomSheetState.collapsedBound)
                                    .offset(y = FloatingBottomNavigationOffset)
                                    .height(FloatingBottomNavigationHeight)
                                    .graphicsLayer {
                                        alpha = floatingNavigationAlpha
                                    },
                            )
                        }
                    }

                    val playerFooterHeight = if (playerBottomSheetState.isDismissed) {
                        bottomInset
                    } else playerBottomSheetState.collapsedBound
                    val songMenuFooterHeight = if (floatingNavigationVisible) {
                        maxOf(
                            playerFooterHeight,
                            playerFooterHeight + FloatingBottomNavigationHeight - FloatingBottomNavigationOffset,
                        )
                    } else playerFooterHeight
                    CompositionLocalProvider(
                        LocalScreenFrostBackdrop provides songMenuBackdrop,
                        LocalSongMenuFooterHeight provides songMenuFooterHeight,
                    ) {
                        BottomSheetMenu(
                            state = LocalMenuState.current,
                            modifier = Modifier.align(Alignment.BottomCenter),
                        )
                    }

                    BottomSheetPage(
                        state = LocalBottomSheetPageState.current,
                        modifier = Modifier.align(Alignment.BottomCenter),
                    )

                    sharedSong?.let { song ->
                        playerConnection?.let {
                            IrideFolioDialog(onDismissRequest = { sharedSong = null }) {

                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                    ) {
                                        YouTubeSongMenu(
                                            song = song,
                                            navController = navController,
                                            onDismiss = { sharedSong = null },
                                        )
                                    }

                            }
                        }
                    }

                    if (showUpdateInterstitial) {
                        UpdateInterstitialScreen(
                            onDismiss = {
                                showUpdateInterstitial = false
                                lifecycleScope.launch(Dispatchers.IO) {
                                    val tag = dataStore[PendingUpdateTagKey] ?: return@launch
                                    dataStore.edit { it[UpdateAnnouncementDismissedTagKey] = tag }
                                }
                            },
                        )
                    }

                    DebugBubble()
                }
            }
        }
    }

    /**
     * Handles the ACTION_RECOGNITION intent sent from the Music Recognizer Widget.
     * Always navigates to the recognition screen to show the result.
     */
    private fun handleRecognitionIntent(
        intent: Intent,
        navController: NavHostController,
    ) {
        if (intent.action != ACTION_RECOGNITION) return
        val autoStart = intent.getBooleanExtra(EXTRA_AUTO_START_RECOGNITION, false)
        intent.action = null
        intent.removeExtra(EXTRA_AUTO_START_RECOGNITION)
        navController.navigate(if (autoStart) "recognition?autoStart=true" else "recognition") {
            launchSingleTop = true
        }
    }

    private fun handleDeepLinkIntent(
        intent: Intent,
        navController: NavHostController,
    ) {
        val uri = intent.data ?: intent.extras?.getString(Intent.EXTRA_TEXT)?.toUri() ?: return
        intent.data = null
        intent.removeExtra(Intent.EXTRA_TEXT)
        val coroutineScope = lifecycle.coroutineScope

        val listenCode =
            uri.getQueryParameter("code")
                ?: uri.getQueryParameter("room")
                ?: uri.pathSegments.getOrNull(1)
        val isListenLink = uri.pathSegments.firstOrNull() == "listen" || uri.host?.equals("listen", ignoreCase = true) == true
        if (!listenCode.isNullOrBlank() && isListenLink) {
            val username = dataStore.get(ListenTogetherUsernameKey, "").ifBlank { "Guest" }
            listenTogetherManager.joinRoom(listenCode, username)
            return
        }

        when (val path = uri.pathSegments.firstOrNull()) {
            "playlist" -> {
                uri.getQueryParameter("list")?.let { playlistId ->
                    if (playlistId.startsWith("OLAK5uy_")) {
                        coroutineScope.launch(Dispatchers.IO) {
                            YouTube
                                .albumSongs(playlistId)
                                .onSuccess { songs ->
                                    songs.firstOrNull()?.album?.id?.let { browseId ->
                                        withContext(Dispatchers.Main) {
                                            navController.navigate("album/$browseId")
                                        }
                                    }
                                }.onFailure { reportException(it) }
                        }
                    } else {
                        navController.navigate("online_playlist/$playlistId")
                    }
                }
            }

            "browse" -> {
                uri.lastPathSegment?.let { browseId ->
                    navController.navigate("album/$browseId")
                }
            }

            "channel", "c" -> {
                uri.lastPathSegment?.let { artistId ->
                    navController.navigate("artist/$artistId")
                }
            }

            "search" -> {
                uri.getQueryParameter("q")?.let {
                    navController.navigate("search/${URLEncoder.encode(it, "UTF-8")}")
                }
            }

            else -> {
                val videoId =
                    when {
                        path == "watch" -> uri.getQueryParameter("v")
                        uri.host == "youtu.be" -> uri.pathSegments.firstOrNull()
                        else -> null
                    }

                val playlistId = uri.getQueryParameter("list")

                if (videoId != null) {
                    coroutineScope.launch(Dispatchers.IO) {
                        YouTube
                            .queue(listOf(videoId), playlistId)
                            .onSuccess { queue ->
                                withContext(Dispatchers.Main) {
                                    playerConnection?.playQueue(
                                        YouTubeQueue(
                                            WatchEndpoint(videoId = queue.firstOrNull()?.id, playlistId = playlistId),
                                            queue.firstOrNull()?.toMediaMetadata(),
                                        ),
                                    )
                                }
                            }.onFailure {
                                reportException(it)
                            }
                    }
                } else if (playlistId != null) {
                    coroutineScope.launch(Dispatchers.IO) {
                        YouTube
                            .queue(null, playlistId)
                            .onSuccess { queue ->
                                val firstItem = queue.firstOrNull()
                                withContext(Dispatchers.Main) {
                                    playerConnection?.playQueue(
                                        YouTubeQueue(
                                            WatchEndpoint(videoId = firstItem?.id, playlistId = playlistId),
                                            firstItem?.toMediaMetadata(),
                                        ),
                                    )
                                }
                            }.onFailure {
                                reportException(it)
                            }
                    }
                }
            }
        }
    }

    @SuppressLint("ObsoleteSdkInt")
    private fun setSystemBarAppearance(isDark: Boolean) {
        WindowCompat.getInsetsController(window, window.decorView.rootView).apply {
            isAppearanceLightStatusBars = !isDark
            isAppearanceLightNavigationBars = !isDark
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            window.statusBarColor = (if (isDark) Color.Transparent else Color.Black.copy(alpha = 0.2f)).toArgb()
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            window.navigationBarColor = (if (isDark) Color.Transparent else Color.Black.copy(alpha = 0.2f)).toArgb()
        }
    }
}

val LocalDatabase = staticCompositionLocalOf<MusicDatabase> { error("No database provided") }
val LocalPlayerConnection = compositionLocalOf<PlayerConnection?> { error("No PlayerConnection provided") }
val LocalPlayerAwareWindowInsets = compositionLocalOf<WindowInsets> { error("No WindowInsets provided") }
val LocalDownloadUtil = staticCompositionLocalOf<DownloadUtil> { error("No DownloadUtil provided") }
val LocalSyncUtils = staticCompositionLocalOf<SyncUtils> { error("No SyncUtils provided") }
val LocalListenTogetherManager = staticCompositionLocalOf<com.metrolist.music.listentogether.ListenTogetherManager?> { null }
val LocalIsPlayerExpanded = compositionLocalOf { false }

data class TopNavBarController(
    val navigationItems: List<Screens>,
    val currentRoute: String?,
    val onItemClick: (Screens, Boolean) -> Unit,
    val compact: Boolean = false,
    val accountImageUrl: String? = null,
)
val LocalTopNavBarController = compositionLocalOf<TopNavBarController?> { null }
