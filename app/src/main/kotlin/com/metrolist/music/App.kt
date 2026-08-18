/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.widget.Toast
import androidx.datastore.preferences.core.edit
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.disk.DiskCache
import coil3.disk.directory
import coil3.memory.MemoryCache
import coil3.request.CachePolicy
import coil3.request.allowHardware
import coil3.request.crossfade
import com.metrolist.innertube.YouTube
import com.metrolist.innertube.models.YouTubeLocale
import com.metrolist.kugou.KuGou
import com.metrolist.lastfm.LastFM
import com.metrolist.music.BuildConfig
import com.metrolist.music.constants.*
import com.metrolist.music.di.ApplicationScope
import com.metrolist.music.extensions.toEnum
import com.metrolist.music.extensions.toInetSocketAddress
import com.metrolist.music.utils.CrashHandler
import com.metrolist.music.utils.GenreProvider
import com.metrolist.music.utils.NetworkConnectivityObserver
import com.metrolist.music.utils.cipher.CipherDeobfuscator
import com.metrolist.music.utils.dataStore
import com.metrolist.music.utils.get
import com.metrolist.music.utils.keepPreferencesWarm
import com.metrolist.music.utils.reportException
import com.metrolist.music.ui.component.dismissedAnchor
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import okhttp3.Credentials
import timber.log.Timber
import java.net.Authenticator
import java.net.PasswordAuthentication
import java.net.Proxy
import java.util.Locale
import javax.inject.Inject

@HiltAndroidApp
class App :
    Application(),
    SingletonImageLoader.Factory {
    @Inject
    @ApplicationScope
    lateinit var applicationScope: CoroutineScope

    @Inject
    lateinit var connectivityObserver: NetworkConnectivityObserver

    override fun onCreate() {
        super.onCreate()

        // Install crash handler first
        CrashHandler.install(this)

        // Initialize cipher deobfuscator for WEB_REMIX streaming
        CipherDeobfuscator.initialize(this)

        // Load the on-disk genre-tag cache used by playlist filter pills
        GenreProvider.init(this)

        // Mirror the preferences into memory so composition never blocks on a disk read.
        dataStore.keepPreferencesWarm(applicationScope)

        // Warm the New Iride UI preference before setContent() ever runs (see companion doc).
        topNavigationBarEnabledCache = dataStore.get(TopNavigationBarKey, true)

        // Warm the player-sheet anchor so a cold start with no saved value falls back to the
        // dismissed position (classic UI), and a cold start with a saved value restores it
        // before any Composable can flash the wrong layout.
        playerAnchorCache = dataStore.get(PlayerAnchorKey, dismissedAnchor)

        Timber.plant(Timber.DebugTree())

        // تهيئة إعدادات التطبيق عند الإقلاع
        applicationScope.launch {
            initializeSettings()
            observeSettingsChanges()
        }

        observeConnectivity()
    }

    /**
     * A network change leaves the OkHttp pool full of sockets bound to the old (now dead) network.
     * Every later request picks one up and stalls until the connect timeout, so the app looks
     * permanently offline until the process is killed. Dropping the pool on each transition makes
     * the next request open a fresh connection instead.
     */
    private fun observeConnectivity() {
        applicationScope.launch(Dispatchers.IO) {
            connectivityObserver.networkStatus
                .drop(1)
                .collect { connected ->
                    YouTube.evictConnections()
                    if (connected) {
                        val prefs = dataStore.data.first()
                        prefs[InnerTubeCookieKey]?.takeIf { it.isNotEmpty() }?.let { YouTube.cookie = it }
                        prefs[VisitorDataKey]?.takeIf { it.isNotEmpty() && it != "null" }?.let { YouTube.visitorData = it }
                    }
                }
        }

        // Wi-Fi <-> mobile handovers don't always flip networkStatus's boolean (see comment on
        // networkChanged), so they need their own eviction trigger. Debounced because a single
        // handover fires onLost+onAvailable (and often a couple of onCapabilitiesChanged) within
        // milliseconds of each other; evictAll() only needs to run once per transition.
        applicationScope.launch(Dispatchers.IO) {
            connectivityObserver.networkChanged
                .debounce(300)
                .collect {
                    YouTube.evictConnections()
                }
        }
    }

    internal suspend fun initializeSettings() {
        val settings = dataStore.data.first()
        val locale = Locale.getDefault()
        val languageTag = locale.language

        YouTube.locale =
            YouTubeLocale(
                gl =
                    settings[ContentCountryKey]?.takeIf { it != SYSTEM_DEFAULT }
                        ?: locale.country.takeIf { it in CountryCodeToName }
                        ?: "US",
                hl =
                    settings[ContentLanguageKey]?.takeIf { it != SYSTEM_DEFAULT }
                        ?: locale.language.takeIf { it in LanguageCodeToName }
                        ?: languageTag.takeIf { it in LanguageCodeToName }
                        ?: "en",
            )

        if (languageTag == "zh-TW") {
            KuGou.useTraditionalChinese = true
        }

        // Initialize LastFM with API keys from BuildConfig (GitHub Secrets)
        LastFM.initialize(
            apiKey = BuildConfig.LASTFM_API_KEY.takeIf { it.isNotEmpty() } ?: "",
            secret = BuildConfig.LASTFM_SECRET.takeIf { it.isNotEmpty() } ?: "",
        )

        if (settings[ProxyEnabledKey] == true) {
            val username = settings[ProxyUsernameKey].orEmpty()
            val password = settings[ProxyPasswordKey].orEmpty()
            val type = settings[ProxyTypeKey].toEnum(defaultValue = Proxy.Type.HTTP)

            if (username.isNotEmpty() || password.isNotEmpty()) {
                if (type == Proxy.Type.HTTP) {
                    YouTube.proxyAuth = Credentials.basic(username, password)
                } else {
                    Authenticator.setDefault(
                        object : Authenticator() {
                            override fun getPasswordAuthentication(): PasswordAuthentication =
                                PasswordAuthentication(username, password.toCharArray())
                        },
                    )
                }
            }
            try {
                settings[ProxyUrlKey]?.let {
                    YouTube.proxy = Proxy(type, it.toInetSocketAddress())
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@App, getString(R.string.failed_to_parse_proxy), Toast.LENGTH_SHORT).show()
                }
                reportException(e)
            }
        }

        YouTube.useLoginForBrowse = settings[UseLoginForBrowse] ?: true

        val channel =
            NotificationChannel(
                "updates",
                getString(R.string.update_channel_name),
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply {
                description = getString(R.string.update_channel_desc)
            }
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(channel)
    }

    private fun observeSettingsChanges() {
        applicationScope.launch(Dispatchers.IO) {
            dataStore.data
                .map { it[VisitorDataKey] }
                .distinctUntilChanged()
                .collect { visitorData ->
                    YouTube.visitorData = visitorData?.takeIf { it != "null" }
                        ?: YouTube.visitorData().getOrNull()?.also { newVisitorData ->
                            dataStore.edit { settings ->
                                settings[VisitorDataKey] = newVisitorData
                            }
                        }
                }
        }

        applicationScope.launch(Dispatchers.IO) {
            dataStore.data
                .map { it[DataSyncIdKey] }
                .distinctUntilChanged()
                .collect { dataSyncId ->
                    YouTube.dataSyncId =
                        dataSyncId?.let {
                            it.takeIf { !it.contains("||") }
                                ?: it.takeIf { it.endsWith("||") }?.substringBefore("||")
                                ?: it.substringAfter("||")
                        }
                }
        }

        applicationScope.launch(Dispatchers.IO) {
            dataStore.data
                .map { it[InnerTubeCookieKey] }
                .distinctUntilChanged()
                .collect { cookie ->
                    try {
                        YouTube.cookie = cookie
                    } catch (e: Exception) {
                        Timber.e(e, "Could not parse cookie. Clearing existing cookie.")
                        forgetAccount(this@App)
                    }
                }
        }

        applicationScope.launch(Dispatchers.IO) {
            dataStore.data
                .map { it[LastFMSessionKey] }
                .distinctUntilChanged()
                .collect { session ->
                    try {
                        LastFM.sessionKey = session
                    } catch (e: Exception) {
                        Timber.e("Error while loading last.fm session key. %s", e.message)
                    }
                }
        }

        applicationScope.launch(Dispatchers.IO) {
            dataStore.data
                .map { Triple(it[ContentCountryKey], it[ContentLanguageKey], it[AppLanguageKey]) }
                .distinctUntilChanged()
                .collect { (contentCountry, contentLanguage, appLanguage) ->
                    val systemLocale = Locale.getDefault()
                    val effectiveAppLocale =
                        appLanguage
                            ?.takeUnless { it == SYSTEM_DEFAULT }
                            ?.let { Locale.forLanguageTag(it) }
                            ?: systemLocale

                    YouTube.locale =
                        YouTubeLocale(
                            gl =
                                contentCountry?.takeIf { it != SYSTEM_DEFAULT }
                                    ?: effectiveAppLocale.country.takeIf { it in CountryCodeToName }
                                    ?: systemLocale.country.takeIf { it in CountryCodeToName }
                                    ?: "US",
                            hl =
                                contentLanguage?.takeIf { it != SYSTEM_DEFAULT }
                                    ?: effectiveAppLocale.toLanguageTag().takeIf { it in LanguageCodeToName }
                                    ?: effectiveAppLocale.language.takeIf { it in LanguageCodeToName }
                                    ?: "en",
                        )
                }
        }
    }

    override fun newImageLoader(context: PlatformContext): ImageLoader {
        val cacheSize =
            runBlocking {
                dataStore.data.map { it[MaxImageCacheSizeKey] ?: 512 }.first()
            }
        return ImageLoader
            .Builder(this)
            .apply {
                crossfade(true)
                allowHardware(Build.VERSION.SDK_INT >= Build.VERSION_CODES.P)
                // Memory cache for fast image loading (prevents network requests on recomposition)
                memoryCache {
                    MemoryCache
                        .Builder()
                        .maxSizePercent(context, 0.25)
                        .build()
                }
                if (cacheSize == 0) {
                    diskCachePolicy(CachePolicy.DISABLED)
                } else {
                    diskCache(
                        DiskCache
                            .Builder()
                            .directory(cacheDir.resolve("coil"))
                            .maxSizeBytes(cacheSize * 1024 * 1024L)
                            .build(),
                    )
                    // Allow reading from disk cache as fallback when network is unavailable
                    networkCachePolicy(CachePolicy.ENABLED)
                }
            }.build()
    }

    companion object {
        // New Iride UI: warmed synchronously in onCreate(), before any Activity/Compose code
        // runs, so the very first composed frame of MainActivity/HomeScreen already has the
        // real stored value to seed rememberPreference(TopNavigationBarKey, ...) with instead
        // of a hardcoded literal default. rememberPreference() itself does perform a blocking
        // DataStore read for its own first-composition value, but that read races with several
        // other blocking preference reads happening in the same first composition (theme color,
        // dark mode, dynamic theme, etc.) — on a slow cold start any jank there is exactly the
        // kind of window where a stale/default UI branch can end up on screen for a frame or
        // two before settling. Reading it here, at process start, removes that race entirely:
        // by the time any Composable asks for it, the true value is already sitting in memory.
        @Volatile
        var topNavigationBarEnabledCache: Boolean = true
            private set

        // Mirror of PlayerAnchorKey for synchronous first-frame reads (see
        // playerBottomSheetState's initialAnchor in MainActivity). Updated in lockstep with the
        // DataStore write that fires from BottomSheetState.onAnchorChanged so config changes pick
        // up the latest user choice without an extra disk round-trip.
        @Volatile
        var playerAnchorCache: Int = dismissedAnchor
            private set

        fun setPlayerAnchorCache(value: Int) {
            playerAnchorCache = value
        }

        suspend fun forgetAccount(context: Context) {
            Timber.d("forgetAccount: Starting logout process")

            // Clear DataStore preferences
            Timber.d("forgetAccount: Clearing DataStore preferences")
            context.dataStore.edit { settings ->
                settings.remove(InnerTubeCookieKey)
                settings.remove(VisitorDataKey)
                settings.remove(DataSyncIdKey)
                settings.remove(AccountNameKey)
                settings.remove(AccountEmailKey)
                settings.remove(AccountChannelHandleKey)
                settings.remove(AccountPhotoUrlKey)
            }
            Timber.d("forgetAccount: DataStore preferences cleared")

            // Immediately clear YouTube object's auth state
            Timber.d("forgetAccount: Clearing YouTube object auth state")
            Timber.d(
                "forgetAccount: Before - cookie=${YouTube.cookie?.take(
                    50,
                )}, visitorData=${YouTube.visitorData?.take(20)}, dataSyncId=${YouTube.dataSyncId?.take(20)}",
            )
            YouTube.cookie = null
            YouTube.visitorData = null
            YouTube.dataSyncId = null
            Timber.d(
                "forgetAccount: After - cookie=${YouTube.cookie}, visitorData=${YouTube.visitorData}, dataSyncId=${YouTube.dataSyncId}",
            )

            // Clear WebView cookies to prevent auto-relogin
            Timber.d("forgetAccount: Clearing WebView CookieManager")
            withContext(Dispatchers.Main) {
                android.webkit.CookieManager.getInstance().apply {
                    removeAllCookies { removed ->
                        Timber.d("forgetAccount: CookieManager.removeAllCookies callback: removed=$removed")
                    }
                    flush()
                }
            }
            Timber.d("forgetAccount: Logout process complete")
        }
    }
}
