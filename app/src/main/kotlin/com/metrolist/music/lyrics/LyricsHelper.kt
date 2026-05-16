/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.lyrics

import android.content.Context
import android.util.LruCache
import com.metrolist.music.constants.LyricsProviderOrderKey
import com.metrolist.music.constants.PLAYER_THUMBNAIL_SIZE
import com.metrolist.music.constants.PreferredLyricsProvider
import com.metrolist.music.constants.PreferredLyricsProviderKey
import com.metrolist.music.db.entities.LyricsEntity.Companion.LYRICS_NOT_FOUND
import com.metrolist.music.extensions.toEnum
import com.metrolist.music.models.MediaMetadata
import com.metrolist.music.ui.utils.resize
import com.metrolist.music.utils.NetworkConnectivityObserver
import com.metrolist.music.utils.dataStore
import com.metrolist.music.utils.reportException
import coil3.imageLoader
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.size.Size
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import timber.log.Timber
import javax.inject.Inject

private const val MAX_LYRICS_FETCH_MS = 30000L
private const val PROVIDER_NONE = ""

object LyricsDebugLog {
    const val ENABLED = true

    data class Entry(val timeMs: Long, val message: String)

    private val _entries = MutableStateFlow<List<Entry>>(emptyList())
    val entries: StateFlow<List<Entry>> = _entries

    fun log(msg: String) {
        if (!ENABLED) return
        android.util.Log.d("LyricsDebug", msg)
        val now = System.currentTimeMillis()
        _entries.value = (_entries.value + Entry(now, msg)).takeLast(40)
    }

    fun clear() {
        _entries.value = emptyList()
    }
}

class LyricsHelper
@Inject
constructor(
    @ApplicationContext private val context: Context,
    private val networkConnectivity: NetworkConnectivityObserver,
) {
    private var lyricsProviders =
        listOf(
            BetterLyricsProvider,
            PaxsenixLyricsProvider,
            LrcLibLyricsProvider,
            KuGouLyricsProvider,
            LyricsPlusProvider,
            YouTubeSubtitleLyricsProvider,
            YouTubeLyricsProvider
        )

    val preferred =
        context.dataStore.data
            .map { preferences ->
                val providerOrder = preferences[LyricsProviderOrderKey] ?: ""
                if (providerOrder.isNotBlank()) {
                    // Use the new provider order if available
                    LyricsProviderRegistry.getOrderedProviders(providerOrder)
                } else {
                    // Fall back to preferred provider logic for backward compatibility
                    val preferredProvider = preferences[PreferredLyricsProviderKey]
                        .toEnum(PreferredLyricsProvider.LRCLIB)
                    when (preferredProvider) {
                        PreferredLyricsProvider.LRCLIB -> listOf(
                            LrcLibLyricsProvider,
                            BetterLyricsProvider,
                            PaxsenixLyricsProvider,
                            KuGouLyricsProvider,
                            LyricsPlusProvider,
                            YouTubeSubtitleLyricsProvider,
                            YouTubeLyricsProvider
                        )
                        PreferredLyricsProvider.KUGOU -> listOf(
                            KuGouLyricsProvider,
                            BetterLyricsProvider,
                            PaxsenixLyricsProvider,
                            LrcLibLyricsProvider,
                            LyricsPlusProvider,
                            YouTubeSubtitleLyricsProvider,
                            YouTubeLyricsProvider
                        )
                        PreferredLyricsProvider.BETTER_LYRICS -> listOf(
                            BetterLyricsProvider,
                            PaxsenixLyricsProvider,
                            LrcLibLyricsProvider,
                            KuGouLyricsProvider,
                            LyricsPlusProvider,
                            YouTubeSubtitleLyricsProvider,
                            YouTubeLyricsProvider
                        )
                        PreferredLyricsProvider.PAXSENIX -> listOf(
                            PaxsenixLyricsProvider,
                            BetterLyricsProvider,
                            LrcLibLyricsProvider,
                            KuGouLyricsProvider,
                            LyricsPlusProvider,
                            YouTubeSubtitleLyricsProvider,
                            YouTubeLyricsProvider
                        )
                    }
                }
            }.distinctUntilChanged()
            .map { providers ->
                lyricsProviders = providers
            }

    private val helperScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    init {
        helperScope.launch {
            preferred.collect { /* lyricsProviders is already set inside the map {} */ }
        }
    }

    private val cache = LruCache<String, List<LyricsResult>>(MAX_CACHE_SIZE)
    private var currentLyricsJob: Job? = null

    private fun resolveWordLyricsDuration(mediaMetadata: MediaMetadata): Int {
        if (mediaMetadata.duration > 0) return mediaMetadata.duration
        // MediaMetadata has no textual duration field; no other source to parse from
        return -1
    }

    suspend fun getLyricsProgressive(
        mediaMetadata: MediaMetadata,
        onTierAvailable: suspend (LyricsWithProvider, LyricsTier) -> Unit
    ) {
        val cleanedTitle = LyricsUtils.cleanTitleForSearch(mediaMetadata.title)
        val artists = mediaMetadata.artists.joinToString { it.name }
        val wordDuration = resolveWordLyricsDuration(mediaMetadata)
        val enabledProviders = lyricsProviders.filter { it.isEnabled(context) }

        val fastSet = setOf(LrcLibLyricsProvider, KuGouLyricsProvider, YouTubeSubtitleLyricsProvider, YouTubeLyricsProvider)
        val wordProviderSet = setOf(BetterLyricsProvider, PaxsenixLyricsProvider)

        val tierMutex = Mutex()
        var bestTier = LyricsTier.PLAIN
        var anyEmitted = false

        LyricsDebugLog.clear()
        LyricsDebugLog.log("START song=${mediaMetadata.title} | providers=${enabledProviders.map { it.name }}")

        coroutineScope {
            val allJobs = enabledProviders.map { provider ->
                val isWordProvider = provider in wordProviderSet
                val timeout = when {
                    isWordProvider -> 12_000L
                    provider in fastSet -> 5_000L
                    else -> 10_000L
                }
                async(Dispatchers.IO) {
                    val startTime = System.currentTimeMillis()
                    LyricsDebugLog.log("REQUEST ${provider.name} | timeout=${timeout}ms")
                    try {
                        val result = withTimeoutOrNull(timeout) {
                            provider.getLyrics(
                                context,
                                mediaMetadata.id,
                                cleanedTitle,
                                artists,
                                if (isWordProvider) wordDuration else mediaMetadata.duration,
                                mediaMetadata.album?.title,
                            )
                        }
                        val elapsed = System.currentTimeMillis() - startTime
                        if (result == null) {
                            LyricsDebugLog.log("TIMEOUT ${provider.name} | after ${elapsed}ms")
                        } else if (result.isSuccess) {
                            val raw = result.getOrNull()!!
                            val filtered = LyricsUtils.filterLyricsCreditLines(raw)
                            val tier = LyricsUtils.detectTier(filtered)
                            LyricsDebugLog.log("SUCCESS ${provider.name} | ${elapsed}ms | tier=$tier | lines=${filtered.lines().size}")
                            val shouldEmit = tierMutex.withLock {
                                if (tier.ordinal > bestTier.ordinal || (!anyEmitted && tier.ordinal >= LyricsTier.PLAIN.ordinal)) {
                                    if (tier.ordinal > bestTier.ordinal) bestTier = tier
                                    anyEmitted = true
                                    true
                                } else false
                            }
                            if (shouldEmit) {
                                LyricsDebugLog.log("EMIT ${provider.name} | tier=$tier")
                                onTierAvailable(LyricsWithProvider(filtered, provider.name), tier)
                            } else {
                                LyricsDebugLog.log("SKIP ${provider.name} | tier=$tier not better than bestTier=$bestTier")
                            }
                        } else {
                            val err = result.exceptionOrNull()?.message ?: "unknown error"
                            LyricsDebugLog.log("FAIL ${provider.name} | ${elapsed}ms | $err")
                        }
                    } catch (_: CancellationException) {
                        LyricsDebugLog.log("CANCEL ${provider.name}")
                    } catch (e: Exception) {
                        val elapsed = System.currentTimeMillis() - startTime
                        LyricsDebugLog.log("EXCEPTION ${provider.name} | ${elapsed}ms | ${e.message}")
                    }
                }
            }
            allJobs.forEach { it.await() }
        }

        LyricsDebugLog.log("DONE | bestTier=$bestTier | anyEmitted=$anyEmitted")
    }

    suspend fun getLyrics(mediaMetadata: MediaMetadata): LyricsWithProvider {

        mediaMetadata.thumbnailUrl?.let { rawUrl ->
            val hdUrl = rawUrl.resize(PLAYER_THUMBNAIL_SIZE, PLAYER_THUMBNAIL_SIZE)
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val request = ImageRequest.Builder(context)
                        .data(hdUrl)
                        .size(Size.ORIGINAL)
                        .memoryCachePolicy(CachePolicy.ENABLED)
                        .diskCachePolicy(CachePolicy.ENABLED)
                        .build()
                    context.imageLoader.execute(request)
                } catch (_: Exception) {}
            }
        }

        currentLyricsJob?.cancel()

        val cached = cache.get(mediaMetadata.id)?.firstOrNull()
        if (cached != null) {
            return LyricsWithProvider(cached.lyrics, cached.providerName)
        }

        // Check network connectivity before making network requests
        // Use synchronous check as fallback if flow doesn't emit
        val isNetworkAvailable = try {
            networkConnectivity.isCurrentlyConnected()
        } catch (e: Exception) {
            // If network check fails, try to proceed anyway
            true
        }

        if (!isNetworkAvailable) {
            return LyricsWithProvider(LYRICS_NOT_FOUND, PROVIDER_NONE)
        }

        val result = withTimeoutOrNull(MAX_LYRICS_FETCH_MS) {
            val cleanedTitle = LyricsUtils.cleanTitleForSearch(mediaMetadata.title)
            val enabledProviders = lyricsProviders.filter { it.isEnabled(context) }
            val perProviderTimeout = MAX_LYRICS_FETCH_MS / enabledProviders.size.coerceAtLeast(1)

            for (provider in enabledProviders) {
                try {
                    Timber.tag("LyricsHelper")
                        .d("Trying provider: ${provider.name} for $cleanedTitle (timeout: ${perProviderTimeout}ms)")
                    val result = withTimeoutOrNull(perProviderTimeout) {
                        provider.getLyrics(
                            context,
                            mediaMetadata.id,
                            cleanedTitle,
                            mediaMetadata.artists.joinToString { it.name },
                            mediaMetadata.duration,
                            mediaMetadata.album?.title,
                        )
                    }
                    when {
                        result?.isSuccess == true -> {
                            Timber.tag("LyricsHelper").i("Successfully got lyrics from ${provider.name}")
                            val filteredLyrics = LyricsUtils.filterLyricsCreditLines(result.getOrNull()!!)
                            return@withTimeoutOrNull LyricsWithProvider(filteredLyrics, provider.name)
                        }
                        result == null -> {
                            Timber.tag("LyricsHelper").w("${provider.name} timed out after ${perProviderTimeout}ms")
                        }
                        else -> {
                            Timber.tag("LyricsHelper").w("${provider.name} failed: ${result.exceptionOrNull()?.message}")
                        }
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Timber.tag("LyricsHelper").w("${provider.name} threw exception: ${e.message}")
                }
            }
            Timber.tag("LyricsHelper").w("All providers failed for ${mediaMetadata.title}")
            return@withTimeoutOrNull LyricsWithProvider(LYRICS_NOT_FOUND, PROVIDER_NONE)
        }
        return result ?: LyricsWithProvider(LYRICS_NOT_FOUND, PROVIDER_NONE)
    }

    suspend fun getAllLyrics(
        mediaId: String,
        songTitle: String,
        songArtists: String,
        duration: Int,
        album: String? = null,
        callback: (LyricsResult) -> Unit,
    ) {
        currentLyricsJob?.cancel()

        val cacheKey = "$songArtists-$songTitle".replace(" ", "")
        cache.get(cacheKey)?.let { results ->
            results.forEach {
                callback(it)
            }
            return
        }

        // Check network connectivity before making network requests
        // Use synchronous check as fallback if flow doesn't emit
        val isNetworkAvailable = try {
            networkConnectivity.isCurrentlyConnected()
        } catch (e: Exception) {
            // If network check fails, try to proceed anyway
            true
        }

        if (!isNetworkAvailable) {
            // Still try to proceed in case of false negative
            return
        }

        val allResult = mutableListOf<LyricsResult>()
        currentLyricsJob = CoroutineScope(SupervisorJob()).launch {
            val cleanedTitle = LyricsUtils.cleanTitleForSearch(songTitle)
            lyricsProviders.forEach { provider ->
                if (provider.isEnabled(context)) {
                    try {
                        provider.getAllLyrics(context, mediaId, cleanedTitle, songArtists, duration, album) { lyrics ->
                            val filteredLyrics = LyricsUtils.filterLyricsCreditLines(lyrics)
                            val result = LyricsResult(provider.name, filteredLyrics)
                            allResult += result
                            callback(result)
                        }
                    } catch (e: Exception) {
                        // Catch network-related exceptions like UnresolvedAddressException
                        reportException(e)
                    }
                }
            }
            cache.put(cacheKey, allResult)
        }

        currentLyricsJob?.join()
    }

    fun cancelCurrentLyricsJob() {
        currentLyricsJob?.cancel()
        currentLyricsJob = null
    }

    companion object {
        private const val MAX_CACHE_SIZE = 3
    }
}

data class LyricsResult(
    val providerName: String,
    val lyrics: String,
)

data class LyricsWithProvider(
    val lyrics: String,
    val provider: String,
)