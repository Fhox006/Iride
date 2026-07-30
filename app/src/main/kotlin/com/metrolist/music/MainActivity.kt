/**
 * Iride Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music

import android.Manifest
import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
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
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastAny
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.zIndex
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.core.util.Consumer
import androidx.core.view.WindowCompat
import androidx.datastore.preferences.core.edit
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.coroutineScope
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
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
import com.metrolist.music.constants.AppPeekHeight
import com.metrolist.music.constants.CurtainCornerRevealHeight
import com.metrolist.music.constants.AppLanguageKey
import com.metrolist.music.constants.CheckForUpdatesKey
import com.metrolist.music.constants.DarkModeKey
import com.metrolist.music.constants.DefaultOpenTabKey
import com.metrolist.music.constants.DisableScreenshotKey
import com.metrolist.music.constants.DynamicThemeKey
import com.metrolist.music.constants.EnableHighRefreshRateKey
import com.metrolist.music.constants.ExperimentalLyricsKey
import com.metrolist.music.constants.DataSyncIdKey
import com.metrolist.music.constants.InnerTubeCookieKey
import com.metrolist.music.constants.OnboardingCompletedKey
import com.metrolist.music.constants.ListenTogetherInTopBarKey
import com.metrolist.music.constants.ShowNewsTabKey
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
import com.metrolist.music.constants.SlimNavBarKey
import com.metrolist.music.constants.PlayerAutoHideTopPanelKey
import com.metrolist.music.constants.TopNavigationBarKey
import com.metrolist.music.constants.StopMusicOnTaskClearKey
import com.metrolist.music.constants.UseNewMiniPlayerDesignKey
import com.metrolist.music.constants.VisitorDataKey
import com.metrolist.music.db.MusicDatabase
import com.metrolist.music.db.entities.SearchHistory
import com.metrolist.music.extensions.toEnum
import com.metrolist.music.lyrics.LyricsProviderRegistry
import com.metrolist.music.models.toMediaMetadata
import com.metrolist.music.playback.DownloadUtil
import com.metrolist.music.playback.MusicService
import com.metrolist.music.playback.MusicService.MusicBinder
import com.metrolist.music.playback.PlayerConnection
import com.metrolist.music.playback.queues.YouTubeQueue
import com.metrolist.music.models.MediaMetadata
import com.metrolist.music.ui.component.AppNavigationRail
import com.metrolist.music.ui.component.RubberBandNavGate
import com.metrolist.music.ui.component.TopNavigationBar
import com.metrolist.music.ui.component.TopScreenGradientBackground
import com.metrolist.music.ui.component.DebugBubble
import com.metrolist.music.ui.component.FloatingPill
import com.metrolist.music.ui.component.FloatingPillBottomSpacing
import com.metrolist.music.ui.component.FloatingPillHeight
import com.metrolist.music.ui.component.BottomSheetMenu
import com.metrolist.music.ui.component.BottomSheetPage
import com.metrolist.music.ui.component.LocalBottomSheetPageState
import com.metrolist.music.ui.component.LocalMenuState
import com.metrolist.music.ui.component.collapsedAnchor
import com.metrolist.music.ui.component.dismissedAnchor
import com.metrolist.music.ui.component.rememberBottomSheetState
import com.metrolist.music.ui.component.rememberDeviceCornerInfo
import com.metrolist.music.ui.component.shimmer.ShimmerTheme
import com.metrolist.music.ui.menu.YouTubeSongMenu
import com.metrolist.music.ui.player.BottomSheetPlayer
import com.metrolist.music.ui.player.IrideBridgeState
import com.metrolist.music.ui.player.IrideMiniPlayerBridgeOverlay
import com.metrolist.music.ui.screens.Screens
import com.metrolist.music.ui.screens.NavigationBuilder
import com.metrolist.music.ui.screens.settings.DarkMode
import com.metrolist.music.ui.screens.settings.NavigationTab
import com.metrolist.music.ui.theme.ColorSaver
import com.metrolist.music.ui.theme.DefaultThemeColor
import com.metrolist.music.ui.theme.IrideTheme
import com.metrolist.music.ui.theme.extractThemeColor
import com.metrolist.music.ui.utils.appBarScrollBehavior
import com.metrolist.music.ui.utils.resetHeightOffset
import com.metrolist.music.utils.SyncUtils
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

    // Keep PlayerConnection as regular property - NOT mutableStateOf to prevent UI recomposition
    // when it becomes null during onStop. Only update the snapshot for Compose when needed.
    private var playerConnection: PlayerConnection? = null
    
    // This is the snapshot we pass to Compose - changes here trigger recomposition
    private var playerConnectionSnapshot by mutableStateOf<PlayerConnection?>(null)
    
    private var isServiceBound = false

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
                        // Connect Listen Together manager to player
                        listenTogetherManager.setPlayerConnection(playerConnection)
                    } catch (e: Exception) {
                        Timber.tag("MainActivity").e(e, "Failed to create PlayerConnection")
                        // Retry after a delay of 500ms
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
                // Disconnect Listen Together manager
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

    override fun onStart() {
        super.onStart()
        // Request notification permission on Android 13+ only for users who already
        // completed onboarding (new users handle this in the onboarding flow)
        if (dataStore.get(OnboardingCompletedKey, false)) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1000)
                }
            }
        }

        // Only start service if not already running — redundant startForegroundService() on a
        // running service re-triggers onStartCommand(), which on Android 12+ can interrupt active
        // coroutine flows and cause library/lyrics collectors to drop and never re-subscribe.
        if (!MusicService.isRunning) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                ContextCompat.startForegroundService(this, Intent(this, MusicService::class.java))
            } else {
                startService(Intent(this, MusicService::class.java))
            }
        }
        
        // Bind to service - if already bound, this is a no-op but ensures we stay connected
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

            // Always re-inject auth state — guards against in-memory loss after Activity recreation
            // without a DataStore change (distinctUntilChanged would suppress re-emission).
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

    override fun onDestroy() {
        if (isFinishing) {
            listenTogetherManager.disconnect()
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
        super.onCreate(savedInstanceState)
        window.decorView.layoutDirection = View.LAYOUT_DIRECTION_LTR
        WindowCompat.setDecorFitsSystemWindows(window, false)

        // Initialize Listen Together manager
        listenTogetherManager.initialize()

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
                        // Guarantees the next traversal actually happens instead of waiting on an
                        // unrelated invalidation.
                        content.postInvalidateOnAnimation()
                    }
                    return ready
                }
            },
        )
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

        if (BuildConfig.UPDATER_AVAILABLE) {
            LaunchedEffect(checkForUpdates) {
                if (checkForUpdates) {
                    withContext(Dispatchers.IO) {
                        val updatesEnabled = dataStore.get(CheckForUpdatesKey, true)
                        if (!updatesEnabled) return@withContext

                        Updater.checkForUpdate().onSuccess { (releaseInfo, _) ->
                            if (releaseInfo != null) {
                                onLatestVersionNameChange(releaseInfo.versionName)
                            }
                        }
                    }
                } else {
                    onLatestVersionNameChange(BuildConfig.VERSION_NAME)
                }
            }
        }

        var pendingUpdateRelease by remember { mutableStateOf<com.metrolist.music.utils.ReleaseInfo?>(null) }
        var pendingUpdateDownloadUrl by remember { mutableStateOf<String?>(null) }
        var updateDialogDismissedThisSession by rememberSaveable { mutableStateOf(false) }

        if (BuildConfig.UPDATER_AVAILABLE) {
            LaunchedEffect(checkForUpdates) {
                if (!checkForUpdates) {
                    pendingUpdateRelease = null
                    pendingUpdateDownloadUrl = null
                    return@LaunchedEffect
                }

                withContext(Dispatchers.IO) {
                    Updater.checkForAnyUpdate(forceRefresh = true)
                        .onSuccess { (releaseInfo, hasUpdate) ->
                            if (hasUpdate && releaseInfo != null) {
                                pendingUpdateRelease = releaseInfo
                                pendingUpdateDownloadUrl = Updater.getDownloadUrlForCurrentVariant(releaseInfo)
                            } else {
                                pendingUpdateRelease = null
                                pendingUpdateDownloadUrl = null
                            }
                        }
                        .onFailure {
                            pendingUpdateRelease = null
                            pendingUpdateDownloadUrl = null
                        }
                }
            }
        }

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

        val darkTheme by rememberEnumPreference(DarkModeKey, defaultValue = DarkMode.ON)
        val isSystemInDarkTheme = isSystemInDarkTheme()
        val useDarkTheme =
            remember(darkTheme, isSystemInDarkTheme) {
                if (darkTheme == DarkMode.AUTO) isSystemInDarkTheme else darkTheme == DarkMode.ON
            }

        LaunchedEffect(useDarkTheme) {
            setSystemBarAppearance(useDarkTheme)
        }

        val pureBlackEnabled by rememberPreference(PureBlackKey, defaultValue = false)
        val pureBlack =
            remember(pureBlackEnabled, useDarkTheme) {
                pureBlackEnabled && useDarkTheme
            }

        val mainTopGradientEnabled by rememberPreference(MainTopGradientKey, defaultValue = true)
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
        // The seed color feeds rememberDynamicColorScheme, which regenerates the entire Material
        // palette on every distinct value — so tweening the seed means ~36 full palette builds, and
        // every composable reading MaterialTheme recomposes with each one. On a cold start that
        // lands exactly on top of first layout (the artwork color is extracted right as the service
        // binds). Snap to the first color instead; only later track changes are worth animating.
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
                val bottomInset = with(density) { windowsInsets.getBottom(density).toDp() }
                val bottomInsetDp = WindowInsets.systemBars.asPaddingValues().calculateBottomPadding()

                val navController = rememberNavController()

                LaunchedEffect(Unit) {
                    // SimpMusic Removal Migration
                    if (dataStore.data.first()[SimpMusicMigrationDoneKey] != true) {
                        dataStore.edit { settings ->
                            // Remove SimpMusic from serialized order string and append Paxsenix if missing
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

                            // Reset preferred provider if it was SimpMusic
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
                val (showNewsTab) = rememberPreference(ShowNewsTabKey, defaultValue = false)
                val (slimNav) = rememberPreference(SlimNavBarKey, defaultValue = false)
                // Seeded from App's process-start cache (read before setContent()) instead of a
                // hardcoded literal, so the very first frame can't briefly disagree with the
                // real stored value and flash the wrong UI variant — see App.topNavigationBarEnabledCache.
                val (topNavigationBarEnabled) = rememberPreference(TopNavigationBarKey, defaultValue = App.topNavigationBarEnabledCache)
                val navigationItems =
                    remember(listenTogetherInTopBar, showNewsTab, topNavigationBarEnabled) {
                        val filtered = Screens.MainScreens.filter {
                            (it != Screens.ListenTogether || !listenTogetherInTopBar) &&
                                (it != Screens.News || showNewsTab)
                        }
                        // New Iride UI: Home, Library, Search, then Account last.
                        if (topNavigationBarEnabled) {
                            filtered.sortedBy { screen ->
                                when (screen) {
                                    Screens.Home -> 0
                                    Screens.Library -> 1
                                    Screens.Search -> 2
                                    Screens.Account -> 4
                                    else -> 3
                                }
                            }
                        } else {
                            filtered
                        }
                    }
                val (useNewMiniPlayerDesign) = rememberPreference(UseNewMiniPlayerDesignKey, defaultValue = true)
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

                val inSearchScreen by remember {
                    derivedStateOf { currentRoute?.startsWith("search/") == true }
                }
                val isTopLevelRoute by remember {
                    derivedStateOf {
                        currentRoute == null ||
                            // New Iride UI: "settings" (the Account tab's real destination) is
                            // treated as top-level too, so it gets the same fade transition and
                            // its own scrollable TopNavigationBar copy as Home/Library/Search —
                            // classic mode keeps it as a pushed sub-screen (back arrow, slide-in).
                            (navigationItems.any { it.route == currentRoute } && (currentRoute != "settings" || topNavigationBarEnabled)) ||
                            currentRoute?.startsWith("search/") == true
                    }
                }
                val isLandscape = configuration.containerDpSize.width > configuration.containerDpSize.height

                val showRail = isLandscape && !inSearchScreen

                // New Iride UI: the player becomes a fixed curtain layer behind the whole app
                // (portrait/top-level only — the landscape rail's MiniPlayer peek is untouched).
                val curtainMode = topNavigationBarEnabled && !showRail

                // New Iride UI: the app layer's corner cut is styled after this device's own
                // screen bezel radius (android.view.RoundedCorner, API 31+) instead of a fixed
                // value, so it reads as a continuation of the phone's own curvature rather than
                // an arbitrary UI shape. Not every OEM reports this accurately, so 0/unavailable
                // falls back to a fixed default.
                val curtainCornerInfo = rememberDeviceCornerInfo()
                val curtainCornerRadiusStart = with(density) {
                    curtainCornerInfo.bottomLeftRadiusPx.takeIf { it > 0f }?.toDp() ?: 28.dp
                }
                val curtainCornerRadiusEnd = with(density) {
                    curtainCornerInfo.bottomRightRadiusPx.takeIf { it > 0f }?.toDp() ?: 28.dp
                }

                val showTopGradientTarget = mainTopGradientEnabled && !pureBlack &&
                    (currentRoute in setOf(
                        Screens.Home.route,
                        Screens.Library.route,
                        Screens.Search.route,
                        Screens.News.route,
                        Screens.Account.route,
                    ) || currentRoute?.startsWith("settings") == true)
                val topGradientAlpha by animateFloatAsState(
                    targetValue = if (showTopGradientTarget) 1f else 0f,
                    animationSpec = tween(400),
                    label = "topGradientVisibility",
                )

                val playerBottomSheetState =
                    rememberBottomSheetState(
                        dismissedBound = 0.dp,
                        // New Iride UI: the curtain is never dismissed to nothing — it always sits
                        // collapsed (showing a placeholder peek row when no track is loaded) from the
                        // very first frame, instead of starting at dismissedAnchor and briefly falling
                        // back to the classic FloatingPill until a track shows up.
                        initialAnchor = if (curtainMode) collapsedAnchor else dismissedAnchor,
                        collapsedBound = if (!showRail) {
                            bottomInset + (if (isTopLevelRoute && !topNavigationBarEnabled) FloatingPillHeight else MiniPlayerHeight) + FloatingPillBottomSpacing +
                                (if (curtainMode) CurtainCornerRevealHeight else 0.dp)
                        } else {
                            bottomInset + MiniPlayerHeight
                        },
                        // New Iride UI: the player "curtain" can never cover the whole screen — a
                        // sliver (AppPeekHeight) of app content stays visible at the top always.
                        // Only applies where the curtain mechanism is actually engaged (portrait) —
                        // landscape/rail mode keeps the old self-positioning full-expand behavior.
                        expandedBound = if (curtainMode) maxHeight - AppPeekHeight else maxHeight,
                        preventDismissDrag = curtainMode,
                    )

                // New Iride UI: the curtain is always mounted, whether or not a track is loaded — it
                // never falls back to the classic FloatingPill. With nothing playing it just sits
                // collapsed showing a placeholder peek row (see BottomSheetPlayer's collapsedContent),
                // so the very first frame after cold start already reads as "the mp3 mini player",
                // never the rounded pill.
                val curtainActive = curtainMode

                // New Iride UI: after 5s static in the fully expanded player, fade out the app-peek
                // strip (TopNavigationBar + drag handle) that's needed to return to the app, leaving
                // a clean player view. Starting a downward drag (progress dropping off 1f) cancels
                // the pending fade and brings it back immediately.
                val (autoHideTopPanel) = rememberPreference(PlayerAutoHideTopPanelKey, defaultValue = true)
                val isPlayerSettledExpanded by remember(playerBottomSheetState) {
                    derivedStateOf { playerBottomSheetState.progress >= 0.999f }
                }
                var topPanelVisible by remember { mutableStateOf(true) }
                LaunchedEffect(curtainActive, autoHideTopPanel, isPlayerSettledExpanded) {
                    if (curtainActive && autoHideTopPanel && isPlayerSettledExpanded) {
                        delay(5000)
                        topPanelVisible = false
                    } else {
                        topPanelVisible = true
                    }
                }
                val topPanelAlpha by animateFloatAsState(
                    targetValue = if (topPanelVisible) 1f else 0f,
                    animationSpec = tween(500),
                    label = "playerTopPanelAlpha",
                )

                // New Iride UI bridge: shared between BottomSheetPlayer (which reports the mini
                // and expanded rects of the cover art) and IrideMiniPlayerBridgeOverlay (which
                // draws a single moving cover, behind the app, between the two) — see
                // IrideMp3Player.kt for the full explanation.
                val irideBridgeState = remember { IrideBridgeState() }

                val playerAwareWindowInsets =
                    remember(bottomInset, showRail, isTopLevelRoute, topNavigationBarEnabled, curtainActive, playerBottomSheetState.isDismissed) {
                        var bottom = bottomInset
                        if (curtainActive) {
                            // The app layer's own box is already shortened by collapsedBound (see
                            // Scaffold's modifier below) — screens never overlap the player here,
                            // so no extra bottom padding is needed at all.
                            bottom = 0.dp
                        } else if (!showRail) {
                            // FloatingPill always occupies space at the bottom
                            bottom += (if (isTopLevelRoute && !topNavigationBarEnabled) FloatingPillHeight else MiniPlayerHeight) + FloatingPillBottomSpacing
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

                // Navigation tracking
                LaunchedEffect(navBackStackEntry) {
                    if (inSearchScreen) {
                        val searchQuery =
                            withContext(Dispatchers.IO) {
                                val rawQuery = navBackStackEntry?.arguments?.getString("query")!!
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

                    // Reset scroll behavior for main navigation items
                    if (navigationItems.fastAny { it.route == navBackStackEntry?.destination?.route }) {
                        if (navigationItems.fastAny { it.route == previousTab }) {
                            topAppBarScrollBehavior.state.resetHeightOffset()
                        }
                    }

                    topAppBarScrollBehavior.state.resetHeightOffset()

                    // Track previous tab for animations
                    navController.currentBackStackEntry?.destination?.route?.let {
                        setPreviousTab(it)
                    }
                }

                // Reacts continuously to the actual current-track state instead of checking it once
                // at connection time — the saved queue is restored asynchronously by MusicService
                // (it waits for playerInitialized, then calls playQueue), so currentMediaItem is
                // still null at the instant playerConnection binds on a cold start. A one-shot check
                // here used to latch the sheet as "user-dismissed" before the restore completed,
                // leaving the New Iride UI stuck on the classic layout until manually toggled.
                LaunchedEffect(playerConnection, curtainMode) {
                    val connection = playerConnection ?: return@LaunchedEffect
                    connection.mediaMetadata.collectLatest { metadata ->
                        if (curtainMode) {
                            // New Iride UI: never dismiss the curtain — bounce back to collapsed
                            // (placeholder peek) instead, regardless of whether a track is loaded.
                            if (playerBottomSheetState.isDismissed) {
                                playerBottomSheetState.collapseSoft()
                            }
                        } else if (metadata == null) {
                            if (!playerBottomSheetState.isDismissed) {
                                playerBottomSheetState.dismiss()
                            }
                        } else if (playerBottomSheetState.isDismissed) {
                            playerBottomSheetState.collapseSoft()
                        }
                    }
                }

                var shouldShowTopBar by rememberSaveable { mutableStateOf(false) }

                LaunchedEffect(navBackStackEntry, listenTogetherInTopBar) {
                    val currentRoute = navBackStackEntry?.destination?.route
                    val isListenTogetherScreen =
                        currentRoute == Screens.ListenTogether.route ||
                            currentRoute == "listen_together_from_topbar"
                    shouldShowTopBar = currentRoute in topLevelScreens &&
                        currentRoute != Screens.Library.route &&
                        currentRoute != "settings" &&
                        currentRoute != Screens.Home.route &&
                        !(isListenTogetherScreen && listenTogetherInTopBar)
                }

                val coroutineScope = rememberCoroutineScope()
                var sharedSong: SongItem? by remember {
                    mutableStateOf(null)
                }
                val snackbarHostState = remember { SnackbarHostState() }

                LaunchedEffect(Unit) {
                    if (pendingIntent != null) {
                        handleRecognitionIntent(pendingIntent!!, navController)
                        handleDeepLinkIntent(pendingIntent!!, navController)
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

                val currentTitleRes =
                    remember(navBackStackEntry) {
                        when (navBackStackEntry?.destination?.route) {
                            Screens.Search.route -> R.string.search
                            Screens.Library.route -> R.string.filter_library
                            Screens.ListenTogether.route -> R.string.together
                            else -> null
                        }
                    }

                val pauseListenHistory by rememberPreference(PauseListenHistoryKey, defaultValue = false)
                val eventCount by database.eventCount().collectAsState(initial = 0)
                val showHistoryButton =
                    remember(pauseListenHistory, eventCount) {
                        !(pauseListenHistory && eventCount == 0)
                    }

                val baseBg = if (pureBlack) Color.Black else MaterialTheme.colorScheme.surfaceContainer

                val onNavItemClick: (Screens, Boolean) -> Unit =
                    remember(navController, coroutineScope, topAppBarScrollBehavior, playerBottomSheetState, navBackStackEntry, topNavigationBarEnabled) {
                        nav@{ screen: Screens, isSelected: Boolean ->
                            // Refuse to switch tabs while a Home/Library/Search/Account rubber-band
                            // pull is still dragging or springing back — see RubberBandNavGate.
                            if (RubberBandNavGate.isActive) return@nav
                            if (playerBottomSheetState.isExpanded) {
                                playerBottomSheetState.collapseSoft()
                            }
                            if (isSelected) {
                                val targetEntry = try {
                                    val route = navController.currentBackStackEntry?.destination?.route
                                    if (route == "search/{query}" || route == "search_input") {
                                        navController.getBackStackEntry("search_input")
                                    } else {
                                        navController.currentBackStackEntry
                                    }
                                } catch (e: Exception) {
                                    null
                                }

                                if (screen == Screens.Search) {
                                    val current = targetEntry?.savedStateHandle?.get<Int>("scrollToTopCount") ?: 0
                                    targetEntry?.savedStateHandle?.set("scrollToTopCount", current + 1)
                                } else {
                                    targetEntry?.savedStateHandle?.set("scrollToTop", true)
                                }

                                coroutineScope.launch {
                                    topAppBarScrollBehavior.state.resetHeightOffset()
                                }
                            } else {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.startDestinationId) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }

                                // New Iride UI: each tab's title/tab-bar row lives as a plain
                                // scrollable item inside the tab's own list (see the "library"/
                                // "search" exclusions in this Scaffold's outer topBar condition),
                                // so its own restored scroll offset decides whether the title is
                                // visible after a switch. Without this, switching tabs could land
                                // mid-scroll on one tab and at the top on another, making the title
                                // row appear at a different height depending on which tab you came
                                // from. Force every tab to land scrolled-to-top so the title is
                                // always in the same spot right after switching.
                                if (topNavigationBarEnabled) {
                                    val newEntry = try {
                                        navController.currentBackStackEntry
                                    } catch (e: Exception) {
                                        null
                                    }
                                    newEntry?.savedStateHandle?.set("scrollToTop", true)
                                    coroutineScope.launch {
                                        topAppBarScrollBehavior.state.resetHeightOffset()
                                    }
                                }
                            }
                        }
                    }

                val onSearchLongClick: () -> Unit =
                    remember(navController) {
                        {
                            navController.navigate("recognition") {
                                launchSingleTop = true
                            }
                        }
                    }

                // Remembered rather than rebuilt inline: a fresh instance on every recomposition of
                // this function is a new value for the local, which recomposes every
                // TopNavigationBar in the app (each tab root renders its own) for changes that have
                // nothing to do with navigation.
                val topNavBarController = remember(navigationItems, currentRoute, onNavItemClick) {
                    TopNavBarController(
                        navigationItems = navigationItems,
                        currentRoute = currentRoute,
                        onItemClick = onNavItemClick,
                    )
                }

                CompositionLocalProvider(
                    LocalDatabase provides database,
                    LocalContentColor provides if (pureBlack) Color.White else contentColorFor(MaterialTheme.colorScheme.surface),
                    LocalPlayerConnection provides playerConnection,
                    LocalPlayerAwareWindowInsets provides playerAwareWindowInsets,
                    LocalDownloadUtil provides downloadUtil,
                    LocalShimmerTheme provides ShimmerTheme,
                    LocalSyncUtils provides syncUtils,
                    LocalListenTogetherManager provides listenTogetherManager,
                    LocalTopNavBarController provides topNavBarController,
                ) {
                    // New Iride UI: player "curtain" mounted first (behind everything) as a fixed,
                    // full-screen layer. The app content below (Scaffold) sits on top of it and
                    // translates up on drag to reveal it — the curtain itself never moves.
                    if (curtainActive && currentRoute != "wrapped") {
                        BottomSheetPlayer(
                            state = playerBottomSheetState,
                            navController = navController,
                            pureBlack = pureBlack,
                            showPeekContent = showRail,
                            bridgeState = if (curtainMode) irideBridgeState else null,
                        )

                        // New Iride UI: draws the single moving cover on top of the curtain but
                        // still *behind* the app (Scaffold, declared right below) — it morphs
                        // between the collapsed and expanded cover position/size without ever
                        // needing to draw over the app itself.
                        IrideMiniPlayerBridgeOverlay(
                            bridgeState = irideBridgeState,
                            sheetProgress = playerBottomSheetState.progress,
                            navController = navController,
                            playerBottomSheetState = playerBottomSheetState,
                        )
                    }

                    Scaffold(
                        snackbarHost = { SnackbarHost(snackbarHostState) },
                        topBar = {
                            Column {
                                // "library", "search_input" and "settings" are excluded here for the same
                                // reason as "home": each renders its own copy of TopNavigationBar inside
                                // its own Scaffold (see LibraryMixScreen, SearchScreen, SettingsScreen), so
                                // its paddingValues correctly reserve space for it and it scrolls away together with the
                                // rest of that screen's content instead of staying pinned on top of it.
                                // Rendering it a second time here — pinned in this outer Scaffold, whose
                                // content Row never applies this topBar's paddingValues — would draw a
                                // duplicate copy on top of that screen's own content.
                                if (topNavigationBarEnabled && !showRail && isTopLevelRoute && currentRoute != "wrapped" && currentRoute != "onboarding" && currentRoute != "home" && currentRoute != "library" && currentRoute != Screens.Search.route && currentRoute != "settings") {
                                    TopNavigationBar(
                                        navigationItems = navigationItems,
                                        currentRoute = currentRoute,
                                        onItemClick = onNavItemClick,
                                        containerColor = if (mainTopGradientEnabled) Color.Transparent else MaterialTheme.colorScheme.background,
                                    )
                                }
                                AnimatedVisibility(
                                    visible = shouldShowTopBar,
                                    enter = fadeIn(animationSpec = tween(durationMillis = 600, easing = EaseInOut)),
                                    exit = fadeOut(animationSpec = tween(durationMillis = 500, easing = EaseInOut)),
                                ) {
                                    Row {
                                        TopAppBar(
                                            title = {
                                                Text(
                                                    text = currentTitleRes?.let { stringResource(it) } ?: "",
                                                    style = MaterialTheme.typography.titleLarge,
                                                )
                                            },
                                            actions = {},
                                            scrollBehavior = topAppBarScrollBehavior,
                                            colors =
                                                TopAppBarDefaults.topAppBarColors(
                                                    containerColor = if (pureBlack) Color.Black else MaterialTheme.colorScheme.surfaceContainer,
                                                    scrolledContainerColor = if (pureBlack) Color.Black else MaterialTheme.colorScheme.surfaceContainer,
                                                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                                                    actionIconContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    navigationIconContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                                ),
                                            modifier =
                                                Modifier.windowInsetsPadding(
                                                    if (showRail) {
                                                        WindowInsets(left = NavigationBarHeight)
                                                            .add(cutoutInsets.only(WindowInsetsSides.Start))
                                                    } else {
                                                        cutoutInsets.only(WindowInsetsSides.Start + WindowInsetsSides.End)
                                                    },
                                                ),
                                        )
                                    }
                                }
                            }
                        },
                        bottomBar = {
                            if (!curtainMode && currentRoute != "wrapped" && !playerBottomSheetState.isDismissed) {
                                BottomSheetPlayer(
                                    state = playerBottomSheetState,
                                    navController = navController,
                                    pureBlack = pureBlack,
                                    showPeekContent = showRail,
                                )
                            }
                        },
                        modifier =
                            if (curtainActive) {
                                // App layer: fixed height (leaves a collapsedBound-tall gap at the
                                // bottom where the curtain peeks through), rounded bottom corners,
                                // translates up (never scales/shrinks) as the curtain is dragged —
                                // capped so AppPeekHeight always stays visible at the top.
                                // The app content itself dissolves to black as the curtain expands
                                // (drawn inside this same clipped graphicsLayer, so the rounded
                                // corners fade correctly too) instead of a separate black panel on
                                // top — there's no second layer to composite.
                                Modifier
                                    .align(Alignment.TopStart)
                                    .fillMaxWidth()
                                    // Extends past collapsedBound by CurtainCornerRevealHeight so
                                    // the app layer's bottom edge lands flush with the collapsed
                                    // curtain's drag handle instead of leaving a bare curtain-
                                    // colored strip above it (collapsedBound reserves that strip
                                    // for the corner curve, but the curve itself only eats into the
                                    // far left/right edges — the centered handle is never under it).
                                    .height(maxHeight - playerBottomSheetState.collapsedBound + CurtainCornerRevealHeight)
                                    .graphicsLayer {
                                        shape = RoundedCornerShape(bottomStart = curtainCornerRadiusStart, bottomEnd = curtainCornerRadiusEnd)
                                        clip = true
                                        translationY = -(playerBottomSheetState.value - playerBottomSheetState.collapsedBound)
                                            .coerceAtLeast(0.dp)
                                            .toPx()
                                        alpha = topPanelAlpha
                                    }
                                    // Dissolve the app content to black as the curtain expands. The
                                    // seam border itself is no longer drawn here — see the unclipped
                                    // overlay Box declared right after this Scaffold, which keeps the
                                    // border always visible (not just mid-drag) and fully outside the
                                    // clipped shape (not clipped away at the rounded corners).
                                    .drawWithContent {
                                        drawContent()
                                        val dissolve = playerBottomSheetState.progress.coerceIn(0f, 1f)
                                        if (dissolve > 0f) {
                                            drawRect(Color.Black, alpha = dissolve)
                                        }
                                    }
                                    .nestedScroll(topAppBarScrollBehavior.nestedScrollConnection)
                            } else {
                                Modifier
                                    .fillMaxSize()
                                    .nestedScroll(topAppBarScrollBehavior.nestedScrollConnection)
                            },
                    ) {
                        Row(Modifier.fillMaxSize()) {
                            val onRailItemClick: (Screens, Boolean) -> Unit =
                                remember(navController, coroutineScope, topAppBarScrollBehavior, playerBottomSheetState, topNavigationBarEnabled) {
                                    { screen: Screens, isSelected: Boolean ->
                                        if (playerBottomSheetState.isExpanded) {
                                            playerBottomSheetState.collapseSoft()
                                        }

                                        if (isSelected) {
                                            navController.currentBackStackEntry?.savedStateHandle?.set("scrollToTop", true)
                                            coroutineScope.launch {
                                                topAppBarScrollBehavior.state.resetHeightOffset()
                                            }
                                        } else {
                                            navController.navigate(screen.route) {
                                                popUpTo(navController.graph.startDestinationId) {
                                                    saveState = true
                                                }
                                                launchSingleTop = true
                                                restoreState = true
                                            }

                                            // Same title-sync fix as onNavItemClick above.
                                            if (topNavigationBarEnabled) {
                                                val newEntry = try {
                                                    navController.currentBackStackEntry
                                                } catch (e: Exception) {
                                                    null
                                                }
                                                newEntry?.savedStateHandle?.set("scrollToTop", true)
                                                coroutineScope.launch {
                                                    topAppBarScrollBehavior.state.resetHeightOffset()
                                                }
                                            }
                                        }
                                    }
                                }

                            val onRailSearchLongClick: () -> Unit =
                                remember(navController) {
                                    {
                                        navController.navigate("recognition") {
                                            launchSingleTop = true
                                        }
                                    }
                                }

                            if (showRail && currentRoute != "wrapped") {
                                AppNavigationRail(
                                    navigationItems = navigationItems,
                                    currentRoute = currentRoute,
                                    onItemClick = onRailItemClick,
                                    pureBlack = pureBlack,
                                    onSearchLongClick = onRailSearchLongClick,
                                    accountImageUrl = accountImageUrl,
                                )
                            }
                            Box(
                                Modifier
                                    .weight(1f),
                            ) {
                                // Mounted once, persistently, so its animation clocks never
                                // restart when switching tabs — only alpha changes.
                                TopScreenGradientBackground(
                                    mediaMetadata = topGradientMediaMetadata,
                                    playerBackground = playerBackgroundStyle,
                                    modifier = Modifier
                                        .align(Alignment.TopStart)
                                        .graphicsLayer { alpha = topGradientAlpha },
                                )

                                val onboardingCompleted = remember { dataStore[OnboardingCompletedKey] ?: false }

                                fun topLevelIndex(route: String?) = navigationItems.indexOfFirst { it.route == route }

                                // NavHost with animations (Material 3 Expressive style)
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
                                    // Enter Transition - instant between tabs, slide for sub-screens
                                    enterTransition = {
                                        val currentRouteIndex = topLevelIndex(targetState.destination.route)
                                        val previousRouteIndex = topLevelIndex(initialState.destination.route)

                                        if (currentRouteIndex != -1 && previousRouteIndex != -1) {
                                            EnterTransition.None
                                        } else if (currentRouteIndex == -1 || currentRouteIndex > previousRouteIndex) {
                                            slideInHorizontally { it / 8 } + fadeIn(tween(200))
                                        } else {
                                            slideInHorizontally { -it / 8 } + fadeIn(tween(200))
                                        }
                                    },
                                    // Exit Transition - instant between tabs, slide for sub-screens
                                    exitTransition = {
                                        val currentRouteIndex = topLevelIndex(initialState.destination.route)
                                        val targetRouteIndex = topLevelIndex(targetState.destination.route)

                                        if (currentRouteIndex != -1 && targetRouteIndex != -1) {
                                            ExitTransition.None
                                        } else if (targetRouteIndex == -1 || targetRouteIndex > currentRouteIndex) {
                                            slideOutHorizontally { -it / 8 } + fadeOut(tween(200))
                                        } else {
                                            slideOutHorizontally { it / 8 } + fadeOut(tween(200))
                                        }
                                    },
                                    // Pop Enter Transition - instant between tabs
                                    popEnterTransition = {
                                        val currentRouteIndex = topLevelIndex(targetState.destination.route)
                                        val previousRouteIndex = topLevelIndex(initialState.destination.route)

                                        if (currentRouteIndex != -1 && previousRouteIndex != -1) {
                                            EnterTransition.None
                                        } else if (previousRouteIndex != -1 && previousRouteIndex < currentRouteIndex) {
                                            slideInHorizontally { it / 8 } + fadeIn(tween(200))
                                        } else {
                                            slideInHorizontally { -it / 8 } + fadeIn(tween(200))
                                        }
                                    },
                                    // Pop Exit Transition - instant between tabs
                                    popExitTransition = {
                                        val currentRouteIndex = topLevelIndex(initialState.destination.route)
                                        val targetRouteIndex = topLevelIndex(targetState.destination.route)

                                        if (currentRouteIndex != -1 && targetRouteIndex != -1) {
                                            ExitTransition.None
                                        } else if (currentRouteIndex != -1 && currentRouteIndex < targetRouteIndex) {
                                            slideOutHorizontally { -it / 8 } + fadeOut(tween(200))
                                        } else {
                                            slideOutHorizontally { it / 8 } + fadeOut(tween(200))
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

                    // New Iride UI: the seam border between app and player, drawn as its own
                    // unclipped layer directly on top of the app layer instead of inside its
                    // clipped drawWithContent. Two problems that fixes: (1) a centered stroke drawn
                    // inside a clip=true graphicsLayer has its outer half clipped away, leaving only
                    // a faint interior sliver, and rounded corners eat into it further — pushing the
                    // path outward here (positive outset, larger radius) puts the whole stroke
                    // outside the app shape's true edge, so it survives the corner curve intact.
                    // (2) the border used to fade in/out with drag progress (invisible at rest); a
                    // constant alpha keeps it visible at all times for accessibility. Shares the
                    // exact same height/translationY expressions as the app layer's own modifier
                    // above so the seam it traces always lines up with the app layer's real edge.
                    if (curtainActive) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .fillMaxWidth()
                                .height(maxHeight - playerBottomSheetState.collapsedBound + CurtainCornerRevealHeight)
                                .graphicsLayer {
                                    translationY = -(playerBottomSheetState.value - playerBottomSheetState.collapsedBound)
                                        .coerceAtLeast(0.dp)
                                        .toPx()
                                    alpha = topPanelAlpha
                                }
                                .drawWithContent {
                                    val strokeWidthPx = 1.5.dp.toPx()
                                    val outset = strokeWidthPx / 2f
                                    val rStart = curtainCornerRadiusStart.toPx() + outset
                                    val rEnd = curtainCornerRadiusEnd.toPx() + outset
                                    val bottomEdge = Path().apply {
                                        moveTo(-outset, size.height + outset - rStart)
                                        arcTo(
                                            rect = Rect(-outset, size.height + outset - 2 * rStart, -outset + 2 * rStart, size.height + outset),
                                            startAngleDegrees = 180f,
                                            sweepAngleDegrees = -90f,
                                            forceMoveTo = false,
                                        )
                                        lineTo(size.width + outset - rEnd, size.height + outset)
                                        arcTo(
                                            rect = Rect(size.width + outset - 2 * rEnd, size.height + outset - 2 * rEnd, size.width + outset, size.height + outset),
                                            startAngleDegrees = 90f,
                                            sweepAngleDegrees = -90f,
                                            forceMoveTo = false,
                                        )
                                    }
                                    drawPath(
                                        path = bottomEdge,
                                        color = Color.White.copy(alpha = 0.18f),
                                        style = Stroke(width = strokeWidthPx),
                                    )
                                },
                        )
                    }

                    // New Iride UI: the app-layer sliver (AppPeekHeight) that stays visible at the
                    // top while the curtain player is expanded is not really usable as an app
                    // screen (it's just the translated bottom edge of the app content, already
                    // dissolving to black via the Scaffold's own drawWithContent above) — dragging
                    // already collapses it, but a plain tap there used to fall through to whatever
                    // app content happened to be underneath instead of collapsing the player. This
                    // catcher is purely a hit target (no drawing of its own) so a tap here reliably
                    // collapses the player instead of hitting whatever's underneath.
                    // Only mounted while actually expanded: a disabled Modifier.clickable still
                    // registers a pointer input node and swallows taps meant for whatever's
                    // beneath it (here, TopNavigationBar's nav buttons, which live inside this
                    // same top AppPeekHeight strip) even though its onClick never fires — so
                    // gating via the composable's presence, not just `enabled`, is required.
                    if (curtainActive && !playerBottomSheetState.isCollapsed && !irideBridgeState.lyricsFullScreenActive) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .fillMaxWidth()
                                .height(AppPeekHeight)
                                .zIndex(1f)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    onClick = { playerBottomSheetState.collapseSoft() },
                                ),
                        )
                    }

                    // New Iride UI: drag-handle indicator for the curtain player. Drawn on its own
                    // layer above the app content (zIndex) instead of inside the curtain's own
                    // collapsedContent, because that content sits *behind* the app layer and fades
                    // out by ~25% drag progress — the handle used to vanish mid-gesture right when
                    // the user most needed the "this is a drag handle" affordance. Staying mounted
                    // for the whole curtainActive lifetime and gliding linearly (matching the sheet's
                    // own 1:1 drag progress, no easing) from its collapsed spot up to just under the
                    // app-peek sliver means it reads as functional in both drag directions — pulling
                    // it down from the top closes the player exactly like pulling it up opens it.
                    if (curtainActive) {
                        val handleProgress = playerBottomSheetState.progress.coerceIn(0f, 1f)
                        // Flush with the top of the miniplayer strip (just below the app layer's
                        // own bottom edge, which lands at collapsedBound - CurtainCornerRevealHeight)
                        // instead of higher up inside the app layer's rounded-corner reveal zone —
                        // the handle belongs to the miniplayer/player section, not the app section.
                        val collapsedHandleY = maxHeight - playerBottomSheetState.collapsedBound + CurtainCornerRevealHeight + 6.dp
                        val expandedHandleY = AppPeekHeight + CurtainCornerRevealHeight + 6.dp
                        val handleY = collapsedHandleY + (expandedHandleY - collapsedHandleY) * handleProgress
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .offset(y = handleY)
                                .zIndex(1f)
                                .size(width = 36.dp, height = 4.dp)
                                .clip(RoundedCornerShape(50))
                                .background(Color.White.copy(alpha = 0.35f)),
                        )
                    }

                    BottomSheetMenu(
                        state = LocalMenuState.current,
                        modifier = Modifier.align(Alignment.BottomCenter),
                    )

                    BottomSheetPage(
                        state = LocalBottomSheetPageState.current,
                        modifier = Modifier.align(Alignment.BottomCenter),
                    )

                    if (!curtainActive && !showRail && currentRoute != "wrapped" && currentRoute != "onboarding") {
                        FloatingPill(
                            navigationItems = navigationItems,
                            currentRoute = currentRoute,
                            onNavItemClick = onNavItemClick,
                            playerBottomSheetState = playerBottomSheetState,
                            onSearchLongClick = onSearchLongClick,
                            accountImageUrl = accountImageUrl,
                            pureBlack = pureBlack,
                            slimNav = slimNav,
                            showNavRow = !topNavigationBarEnabled,
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .zIndex(1f)
                                .graphicsLayer {
                                    val progress = playerBottomSheetState.progress.coerceIn(0f, 1f)
                                    val pillHeightPx = (FloatingPillHeight + FloatingPillBottomSpacing + bottomInset).toPx()
                                    translationY = pillHeightPx * progress
                                },
                        )
                    }

                    sharedSong?.let { song ->
                        playerConnection?.let {
                            Dialog(
                                onDismissRequest = { sharedSong = null },
                                properties = DialogProperties(usePlatformDefaultWidth = false),
                            ) {
                                Surface(
                                    modifier = Modifier.padding(24.dp),
                                    shape = RoundedCornerShape(16.dp),
                                    color = AlertDialogDefaults.containerColor,
                                    tonalElevation = AlertDialogDefaults.TonalElevation,
                                ) {
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
                    }

                    if (
                        pendingUpdateRelease != null &&
                        !updateDialogDismissedThisSession &&
                        currentRoute != "onboarding"
                    ) {
                        com.metrolist.music.ui.component.AppUpdateDialog(
                            releaseInfo = pendingUpdateRelease!!,
                            downloadUrl = pendingUpdateDownloadUrl,
                            onDismiss = { updateDialogDismissedThisSession = true },
                            onInstall = {
                                updateDialogDismissedThisSession = true
                                pendingUpdateDownloadUrl?.let { url ->
                                    startActivity(
                                        Intent(Intent.ACTION_VIEW, url.toUri()).apply {
                                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                        }
                                    )
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
// Deliberately NOT static: this one is the only app-wide local whose value actually changes at
// runtime — it flips null -> instance the moment MusicService binds, a few hundred ms into a cold
// start. A staticCompositionLocalOf invalidates its entire subtree unconditionally on any change,
// so that single flip used to tear down and recompose the whole UI mid-launch (nav bar, feed and
// mini player all blinking out and back). A regular compositionLocalOf recomposes only the
// surfaces that actually read the connection.
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
)
val LocalTopNavBarController = compositionLocalOf<TopNavBarController?> { null }
