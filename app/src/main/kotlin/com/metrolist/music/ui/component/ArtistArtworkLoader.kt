package com.metrolist.music.ui.component

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.util.LruCache
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.Color
import coil3.asImage
import coil3.imageLoader
import coil3.memory.MemoryCache
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.request.bitmapConfig
import coil3.size.Precision
import coil3.toBitmap
import com.metrolist.music.data.remote.ArtistArtworkRepository
import com.metrolist.music.data.remote.ArtistArtworkSource
import com.metrolist.music.ui.utils.resize
import com.metrolist.music.utils.ArtistNameMatcher
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.floatOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.put
import javax.inject.Inject
import javax.inject.Singleton
import timber.log.Timber

private const val ArtistArtworkSelectionPolicyVersion = 5
private const val ArtistArtworkPreviewResolution = 448

internal data class ArtistArtworkRequest(
    val id: String,
    val name: String?,
    val youtubeUrl: String?,
    val channelId: String? = null,
    val externalEnabled: Boolean = true,
) {
    val key = "$id:${ArtistNameMatcher.normalize(name.orEmpty())}:$externalEnabled"
}

internal data class PreparedArtistArtwork(
    val source: ArtistArtworkSource,
    val url: String,
    val artwork: IrideArtistArtwork,
    val quality: ArtistArtworkQuality,
)

@Singleton
internal class ArtistArtworkLoader @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: ArtistArtworkRepository,
) {
    private data class Selection(
        val source: ArtistArtworkSource,
        val url: String,
        val expiresAt: Long,
        val policyVersion: Int = ArtistArtworkSelectionPolicyVersion,
    )
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val requests = Semaphore(2)
    private val detailRequests = Semaphore(1)
    private data class BitmapRequestLock(val mutex: Mutex = Mutex(), var users: Int = 0)
    private val bitmapRequests = mutableMapOf<String, BitmapRequestLock>()
    private val pending = mutableMapOf<String, Job>()
    private val updates = MutableSharedFlow<String>(extraBufferCapacity = 32)
    val selectionUpdates = updates.asSharedFlow()
    private val selections = LinkedHashMap<String, Selection>(128, 0.75f, true)
    private val fallbackUrls = LinkedHashMap<String, String>(128, 0.75f, true)
    private val presentations = ArtistArtworkPresentation()
    private val qualityCache = LruCache<String, ArtistArtworkQuality>(128)
    private val portraitCache = LruCache<String, Float>(128)
    private val tintCache = LruCache<String, Int>(128)
    private val restored = CompletableDeferred<Unit>()
    private val preferences by lazy { context.getSharedPreferences("artist_artwork_selection_v2_portrait", Context.MODE_PRIVATE) }
    private val prepared = object : LruCache<String, PreparedArtistArtwork>(32 * 1024 * 1024) {
        override fun sizeOf(key: String, value: PreparedArtistArtwork): Int =
            value.artwork.bitmap.allocationByteCount
    }
    private val detailCacheBytes = (Runtime.getRuntime().maxMemory() / 4)
        .coerceIn(4L * 1024 * 1024, 64L * 1024 * 1024).toInt()
    private val detailResolution = minOf(
        artistArtworkDetailResolution(detailCacheBytes),
        context.resources.displayMetrics.widthPixels.coerceIn(ArtistArtworkResolution, 1920),
    )
    private val previews = object : LruCache<String, PreparedArtistArtwork>(8 * 1024 * 1024) {
        override fun sizeOf(key: String, value: PreparedArtistArtwork): Int = value.artwork.bitmap.allocationByteCount
    }
    private val detailed = object : LruCache<String, PreparedArtistArtwork>(detailCacheBytes) {
        override fun sizeOf(key: String, value: PreparedArtistArtwork): Int =
            value.artwork.bitmap.allocationByteCount
    }

    init {
        scope.launch {
            try {
                val saved = preferences.all.mapNotNull { (key, value) ->
                    runCatching {
                        val json = Json.parseToJsonElement(value as String) as JsonObject
                        fun text(field: String) = (json[field] as? JsonPrimitive)?.contentOrNull
                        val selection = Selection(ArtistArtworkSource.valueOf(requireNotNull(text("source"))),
                            requireNotNull(text("url")), (json["expiresAt"] as JsonPrimitive).longOrNull ?: 0L,
                            (json["policyVersion"] as? JsonPrimitive)?.intOrNull ?: 0)
                        val metrics = listOf("portrait", "borders", "definition", "blend", "photographic").map {
                            (json[it] as? JsonPrimitive)?.floatOrNull
                        }
                        if (selection.policyVersion == ArtistArtworkSelectionPolicyVersion) {
                            metrics[0]?.takeIf { it in 0f..1f }?.let { portraitCache.put(selection.url, it) }
                            (json["tint"] as? JsonPrimitive)?.intOrNull?.takeIf { it ushr 24 == 255 }
                                ?.let { tintCache.put(selection.url, it) }
                        }
                        if (selection.policyVersion == ArtistArtworkSelectionPolicyVersion &&
                            selection.expiresAt > System.currentTimeMillis() && metrics.all { it != null && it in 0f..1f }) {
                            qualityCache.put(selection.url, ArtistArtworkQuality(
                                requireNotNull(metrics[0]), requireNotNull(metrics[1]), requireNotNull(metrics[2]),
                                requireNotNull(metrics[3]), requireNotNull(metrics[4])))
                        }
                        key to selection
                    }.getOrNull()
                }.takeLast(128)
                synchronized(selections) { saved.forEach { (key, value) -> selections.putIfAbsent(key, value) } }
                // Restore recent previews while the app opens, using disk only and no provider lookup.
                // Foreground requests can read selections immediately, independently of warming.
                restored.complete(Unit)
                saved.takeLast(8).forEach { (_, selection) ->
                    load(selection.source, selection.url, diskOnly = true,
                        resolution = ArtistArtworkPreviewResolution, evaluateQuality = false)
                }
            } catch (error: Exception) {
                Timber.w(error, "Unable to restore artist artwork selections")
            } finally {
                restored.complete(Unit)
            }
        }
    }

    suspend fun restoreSelections() = restored.await()

    fun cached(request: ArtistArtworkRequest): PreparedArtistArtwork? {
        val selection = selectionFor(request)
        val url = selection?.url ?: synchronized(fallbackUrls) { fallbackUrls[request.id] } ?: return null
        return prepared.get(url)
    }

    fun cachedUrl(request: ArtistArtworkRequest): String? = selectionFor(request)?.url

    fun presentationUrl(request: ArtistArtworkRequest, suggested: String?): String? = presentations.choose(request.id, suggested)

    fun retainPresentation(request: ArtistArtworkRequest, url: String) = presentations.retain(request.id, url)

    fun releasePresentation(request: ArtistArtworkRequest, url: String) = presentations.release(request.id, url)

    fun cachedForDisplay(request: ArtistArtworkRequest): PreparedArtistArtwork? {
        val preview = presentations.current(request.id)
        val url = preview ?: selectionFor(request)?.url ?: synchronized(fallbackUrls) { fallbackUrls[request.id] }
            ?: request.youtubeUrl?.resize(ArtistArtworkResolution, ArtistArtworkResolution)
        return url?.let { detailed.get(it) ?: prepared.get(it) ?: previews.get(it) ?: cachedThumbnail(it) }
    }

    suspend fun loadHighResolution(artwork: PreparedArtistArtwork): PreparedArtistArtwork? {
        if (artwork.artwork.bitmap.width >= detailResolution) return artwork
        detailed.get(artwork.url)?.let { return it }
        return detailRequests.withPermit {
            detailed.get(artwork.url) ?: load(artwork.source, artwork.url, resolution = detailResolution, evaluateQuality = false)
        }
    }

    fun clearMemory() {
        prepared.evictAll()
        previews.evictAll()
        detailed.evictAll()
    }

    private fun selectionFor(request: ArtistArtworkRequest): Selection? = synchronized(selections) {
        selections[request.key] ?: selections.entries.lastOrNull {
            it.key.startsWith("${request.id}:") && it.key.endsWith(":${request.externalEnabled}")
        }?.value
    }

    fun prefetch(request: ArtistArtworkRequest) {
        request.youtubeUrl?.resize(ArtistArtworkResolution, ArtistArtworkResolution)?.let { url ->
            synchronized(fallbackUrls) {
                fallbackUrls[request.id] = url
                if (fallbackUrls.size > 128) fallbackUrls.remove(fallbackUrls.keys.first())
            }
        }
        val selection = selectionFor(request)
        if (selection?.policyVersion == ArtistArtworkSelectionPolicyVersion && cachedForDisplay(request) != null) return
        if (request.name.isNullOrBlank() && request.youtubeUrl == null && selection == null) return
        val job = synchronized(pending) {
            if (pending.containsKey(request.key) || pending.size >= 8) return
            scope.launch(start = CoroutineStart.LAZY) {
                try {
                    restored.await()
                    requests.withPermit { prepareSelection(request) }
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (error: Exception) {
                    Timber.w(error, "Unable to prepare artist artwork")
                } finally {
                    synchronized(pending) { pending.remove(request.key) }
                }
            }.also { pending[request.key] = it }
        }
        job.start()
    }

    private suspend fun prepareSelection(request: ArtistArtworkRequest) = supervisorScope {
        // Warming an existing profile never reranks providers or replaces a cached photo.
        loadForDisplay(request)?.let { return@supervisorScope }
        if (request.name.isNullOrBlank() || !request.externalEnabled) return@supervisorScope
        val saved = selectionFor(request)
        val previous = async { saved?.let { load(it.source, it.url) } }
        val fallback = async { loadFallback(request) }
        val alternatives = mutableListOf<PreparedArtistArtwork>()
        val previewSelection = Mutex()
        repository.findArtworks(requireNotNull(request.name), request.channelId ?: request.id) { candidate ->
            previewSelection.withLock {
                if (cachedForDisplay(request) == null) {
                    load(candidate.source, candidate.imageUrl, resolution = ArtistArtworkPreviewResolution,
                        evaluateQuality = false)?.let { save(request, it) }
                }
            }
            load(candidate.source, candidate.imageUrl)?.let { artwork ->
                synchronized(alternatives) { alternatives.add(artwork) }
            }
        }
        val candidates = synchronized(alternatives) { alternatives.toList() } + listOfNotNull(fallback.await())
        preferredArtistArtwork(candidates, { it.source }, { it.quality.borderScore >= 0.25f }, { it.quality }, previous.await())
            ?.let { save(request, it) }
    }

    suspend fun loadForDisplay(request: ArtistArtworkRequest): PreparedArtistArtwork? {
        restored.await()
        val saved = selectionFor(request)
        val pinned = presentations.current(request.id)
        val url = pinned ?: saved?.url ?: request.youtubeUrl?.resize(ArtistArtworkResolution, ArtistArtworkResolution)
            ?: synchronized(fallbackUrls) { fallbackUrls[request.id] } ?: return null
        val source = saved?.takeIf { it.url == url }?.source ?: sourceForUrl(url)
        val displayed = cachedForDisplay(request)
            ?: load(source, url, resolution = ArtistArtworkPreviewResolution, evaluateQuality = false)
            ?: return null
        // Persist the photo actually displayed, including YouTube and local-only profiles.
        if (saved?.url != url || saved.policyVersion != ArtistArtworkSelectionPolicyVersion) save(request, displayed)
        return displayed
    }

    private suspend fun loadFallback(request: ArtistArtworkRequest): PreparedArtistArtwork? =
        (request.youtubeUrl?.resize(ArtistArtworkResolution, ArtistArtworkResolution)
            ?: synchronized(fallbackUrls) { fallbackUrls[request.id] })?.let {
            load(ArtistArtworkSource.YOUTUBE_MUSIC, it)
        }

    private fun sourceForUrl(url: String): ArtistArtworkSource = when {
        url.contains(".mzstatic.com/") -> ArtistArtworkSource.APPLE_MUSIC
        url.startsWith("https://i.scdn.co/") -> ArtistArtworkSource.SPOTIFY
        else -> ArtistArtworkSource.YOUTUBE_MUSIC
    }

    private fun cachedThumbnail(url: String): PreparedArtistArtwork? {
        val memory = context.imageLoader.memoryCache ?: return null
        val bitmap = sequenceOf(448, 192).mapNotNull { resolution ->
            memory[MemoryCache.Key("iride_artist_thumbnail:$url:$resolution")]?.image?.toBitmap()
        }.firstOrNull() ?: return null
        val square = squareArtistBitmap(bitmap, ArtistArtworkPreviewResolution)
        val tint = tintCache.get(url)?.let { Color(it) } ?: square.irideArtistArtworkTint(1f)
            .also { tintCache.put(url, it.toArgb()) }
        return PreparedArtistArtwork(sourceForUrl(url), url, IrideArtistArtwork(square, tint),
            qualityCache.get(url) ?: ArtistArtworkQuality(0f, 0f, 0f)).also { previews.put(url, it) }
    }

    private fun cacheFor(resolution: Int): LruCache<String, PreparedArtistArtwork> = when (resolution) {
        ArtistArtworkPreviewResolution -> previews
        ArtistArtworkResolution -> prepared
        else -> detailed
    }

    private suspend fun load(
        source: ArtistArtworkSource,
        url: String,
        diskOnly: Boolean = false,
        resolution: Int = ArtistArtworkResolution,
        evaluateQuality: Boolean = true,
    ): PreparedArtistArtwork? {
        val cache = cacheFor(resolution)
        cache.get(url)?.let { return it }
        val requestKey = "$resolution:$url"
        val lock = synchronized(bitmapRequests) {
            bitmapRequests.getOrPut(requestKey) { BitmapRequestLock() }.also { it.users++ }
        }
        try {
            if (diskOnly) {
                if (!lock.mutex.tryLock()) return null
                return try {
                    cache.get(url) ?: loadUncached(source, url, diskOnly = true, resolution = resolution, evaluateQuality = evaluateQuality)
                } finally {
                    lock.mutex.unlock()
                }
            }
            return lock.mutex.withLock {
                cache.get(url) ?: loadUncached(source, url, diskOnly = false, resolution = resolution, evaluateQuality = evaluateQuality)
            }
        } finally {
            synchronized(bitmapRequests) {
                lock.users--
                if (lock.users == 0) bitmapRequests.remove(requestKey)
            }
        }
    }

    private suspend fun loadUncached(
        source: ArtistArtworkSource,
        url: String,
        diskOnly: Boolean,
        resolution: Int,
        evaluateQuality: Boolean,
    ): PreparedArtistArtwork? {
        return try {
            val downloadUrl = artistArtworkDownloadUrl(url, resolution)
            suspend fun decode(data: String, cacheOnly: Boolean): Bitmap? = context.imageLoader.execute(
                ImageRequest.Builder(context).data(data).size(resolution, resolution)
                    .precision(Precision.EXACT)
                    .memoryCacheKey(if (resolution == ArtistArtworkPreviewResolution)
                        "iride_artist_thumbnail:$url:$resolution" else "iride_artist_artwork_v3:$resolution:$url")
                    .diskCacheKey(data)
                    .bitmapConfig(Bitmap.Config.ARGB_8888)
                    .allowHardware(false).apply {
                        if (cacheOnly) networkCachePolicy(CachePolicy.DISABLED)
                    }.build(),
            ).image?.toBitmap()
            // Reuse old full-size disk entries before downloading a smaller preview.
            val bitmap = if (resolution == ArtistArtworkPreviewResolution && downloadUrl != url) {
                decode(downloadUrl, cacheOnly = true) ?: decode(url, cacheOnly = true)
                    ?: if (diskOnly) null else decode(downloadUrl, cacheOnly = false)
            } else decode(downloadUrl, cacheOnly = diskOnly)
            if (bitmap == null) return null
            val result = withContext(Dispatchers.Default) {
                val normalized = squareArtistBitmap(bitmap, resolution)
                cacheThumbnails(url, normalized)
                // One photo keeps its original page pigment when HD decoding changes the sampling grid.
                val tint = tintCache.get(url)?.let { Color(it) } ?: normalized.irideArtistArtworkTint(1f)
                    .also { tintCache.put(url, it.toArgb()) }
                val quality = qualityCache.get(url) ?: if (!evaluateQuality) ArtistArtworkQuality(0f, 0f, 0f) else run {
                    val sample = Bitmap.createScaledBitmap(normalized, 64, 64, true)
                    val pixels = IntArray(64 * 64)
                    sample.getPixels(pixels, 0, 64, 0, 0, 64, 64)
                    if (sample !== normalized) sample.recycle()
                    val frost = frostedArtistBitmap(normalized)
                    val frostSample = Bitmap.createScaledBitmap(frost, 64, 64, true)
                    val frostPixels = IntArray(64 * 64)
                    frostSample.getPixels(frostPixels, 0, 64, 0, 0, 64, 64)
                    if (frostSample !== frost) frostSample.recycle()
                    frost.recycle()
                    val blend = artistArtworkBlendQuality(frostPixels, 64, 64, tint.toArgb())
                    val corners = (1.0 - artistArtworkCornerDistance(pixels, 64, 64) / 0.40).toFloat().coerceIn(0f, 1f)
                    ArtistArtworkQuality(
                        portrait = portraitCache.get(url) ?: ArtistPortraitDetector.quality(normalized)
                            .also { portraitCache.put(url, it) },
                        borders = corners * 0.25f + blend * 0.75f,
                        definition = artistDefinitionQuality(pixels, 64, 64, minOf(bitmap.width, bitmap.height)),
                        blend = blend,
                        photographic = artistPhotographicQuality(pixels, 64, 64),
                    )
                }.also { if (evaluateQuality) qualityCache.put(url, it) }
                PreparedArtistArtwork(
                    source,
                    url,
                    IrideArtistArtwork(normalized, tint),
                    quality,
                )
            }
            cacheFor(resolution).put(url, result)
            result
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            null
        }
    }

    private fun cacheThumbnails(url: String, bitmap: Bitmap) {
        val memoryCache = context.imageLoader.memoryCache ?: return
        // Use the same keys and exact sizes as list/grid requests to avoid a second decode.
        for (resolution in intArrayOf(192, 448)) {
            val key = MemoryCache.Key("iride_artist_thumbnail:$url:$resolution")
            if (memoryCache[key] == null) {
                memoryCache[key] = MemoryCache.Value(
                    Bitmap.createScaledBitmap(bitmap, resolution, resolution, true).asImage(),
                )
            }
        }
    }

    private fun save(request: ArtistArtworkRequest, artwork: PreparedArtistArtwork) {
        // Candidate evaluation may have evicted the winner from the bounded bitmap cache.
        if (artwork.artwork.bitmap.width >= ArtistArtworkResolution) prepared.put(artwork.url, artwork)
        else previews.put(artwork.url, artwork)
        val selection = Selection(artwork.source, artwork.url, Long.MAX_VALUE)
        val removed = synchronized(selections) {
            selections[request.key] = selection
            if (selections.size > 128) selections.keys.first().also { selections.remove(it) } else null
        }
        val evaluatedQuality = qualityCache.get(artwork.url)
        preferences.edit().apply {
            if (removed != null) remove(removed)
            putString(request.key, buildJsonObject {
                put("source", selection.source.name)
                put("url", selection.url)
                put("expiresAt", selection.expiresAt)
                put("policyVersion", selection.policyVersion)
                evaluatedQuality?.let {
                    put("portrait", it.portrait)
                    put("borders", it.borders)
                    put("definition", it.definition)
                    put("blend", it.blend)
                    put("photographic", it.photographic)
                }
                put("tint", artwork.artwork.tint.toArgb())
            }.toString())
        }.apply()
        updates.tryEmit(request.key)
    }
}

private fun squareArtistBitmap(bitmap: Bitmap, resolution: Int): Bitmap {
    val outputSide = artistArtworkSquareSide(bitmap.width, bitmap.height, resolution)
    if (bitmap.width == outputSide && bitmap.height == outputSide) return bitmap
    val side = minOf(bitmap.width, bitmap.height)
    val left = (bitmap.width - side) / 2
    val top = (bitmap.height - side) / 2
    return Bitmap.createBitmap(outputSide, outputSide, Bitmap.Config.ARGB_8888).also {
        Canvas(it).drawBitmap(bitmap, Rect(left, top, left + side, top + side),
            Rect(0, 0, outputSide, outputSide), Paint(Paint.FILTER_BITMAP_FLAG))
    }
}

@EntryPoint
@InstallIn(SingletonComponent::class)
internal interface ArtistArtworkEntryPoint {
    fun artistArtworkLoader(): ArtistArtworkLoader
}

@Composable
internal fun rememberArtistArtworkLoader(): ArtistArtworkLoader {
    val context = LocalContext.current.applicationContext
    return remember(context) {
        EntryPointAccessors.fromApplication(context, ArtistArtworkEntryPoint::class.java).artistArtworkLoader()
    }
}

@Composable
internal fun rememberArtistThumbnail(
    id: String,
    name: String,
    youtubeUrl: String?,
    channelId: String? = null,
    externalEnabled: Boolean = true,
): String? {
    val loader = rememberArtistArtworkLoader()
    val request = remember(id, name, youtubeUrl, channelId, externalEnabled) {
        ArtistArtworkRequest(id, name, youtubeUrl, channelId, externalEnabled)
    }
    // A visible tile keeps its source. Its next appearance can use the newly prepared selection.
    val selectedUrl = remember(request.key) {
        mutableStateOf(loader.presentationUrl(request,
            loader.cachedUrl(request) ?: youtubeUrl?.resize(ArtistArtworkResolution, ArtistArtworkResolution)))
    }
    val url = selectedUrl.value
    DisposableEffect(request.id, url) {
        if (url != null) loader.retainPresentation(request, url)
        onDispose { if (url != null) loader.releasePresentation(request, url) }
    }
    LaunchedEffect(request) {
        loader.restoreSelections()
        if (selectedUrl.value == null) selectedUrl.value = loader.presentationUrl(request,
            loader.cachedUrl(request) ?: youtubeUrl?.resize(ArtistArtworkResolution, ArtistArtworkResolution))
        loader.prefetch(request)
        loader.selectionUpdates.collect { key ->
            if (key == request.key && selectedUrl.value == null) {
                selectedUrl.value = loader.presentationUrl(request, loader.cachedUrl(request))
            }
        }
    }
    return url ?: youtubeUrl?.resize(ArtistArtworkResolution, ArtistArtworkResolution)
}
