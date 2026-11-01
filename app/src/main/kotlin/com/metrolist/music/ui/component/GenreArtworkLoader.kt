package com.metrolist.music.ui.component

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.util.LruCache
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import coil3.imageLoader
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.request.bitmapConfig
import coil3.size.Precision
import coil3.toBitmap
import com.metrolist.music.data.remote.GenreArtworkCandidate
import com.metrolist.music.data.remote.GenreArtworkParser
import com.metrolist.music.data.remote.GenreArtworkRepository
import com.metrolist.music.data.remote.genreArtworkFingerprint
import com.metrolist.music.data.remote.genreArtworkIsDuplicate
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton
import java.util.concurrent.ConcurrentHashMap

@Serializable
private data class GenreArtworkSelection(val candidate: GenreArtworkCandidate, val fingerprint: Long)

private data class GenreArtworkPreview(
    val candidate: GenreArtworkCandidate,
    val fingerprint: Long,
    val score: Float,
    val artwork: IrideArtistArtwork,
)

@Singleton
internal class GenreArtworkLoader @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: GenreArtworkRepository,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val genreLocks = Array(32) { Mutex() }
    private val downloads = Semaphore(3)
    private val pending = mutableMapOf<String, Job>()
    private val updates = MutableSharedFlow<String>(extraBufferCapacity = 32)
    val artworkUpdates = updates.asSharedFlow()
    // Only ownership commits share a lock; a slow genre never blocks every other search.
    private val mutex = Mutex()
    private val preferences by lazy { context.getSharedPreferences("genre_artwork_selection_v1", Context.MODE_PRIVATE) }
    private val selections by lazy {
        ConcurrentHashMap(preferences.all.mapNotNull { (name, value) ->
            runCatching { Json.decodeFromString<GenreArtworkSelection>(value as String) }.getOrNull()
                ?.takeIf {
                    GenreArtworkParser.matchesTitle(name, it.candidate.title) &&
                        it.candidate.identity.isNotEmpty() && it.candidate.nativeSide >= GenreArtworkParser.MINIMUM_SIDE &&
                        GenreArtworkParser.isPlaylistPage(it.candidate.pageUrl)
                }?.let { name to it }
        }.toMap())
    }
    private val failures = ConcurrentHashMap<String, Long>()
    private val cacheBytes = (Runtime.getRuntime().maxMemory() / 8)
        .coerceIn(12L * 1024 * 1024, 32L * 1024 * 1024).toInt()
    private val resolution = minOf(artistArtworkDetailResolution(cacheBytes),
        context.resources.displayMetrics.widthPixels.coerceIn(GenreArtworkParser.MINIMUM_SIDE, 1920))
    private val cache = object : LruCache<String, IrideArtistArtwork>(cacheBytes) {
        override fun sizeOf(key: String, value: IrideArtistArtwork): Int = value.bitmap.allocationByteCount
    }
    private val previews = object : LruCache<String, IrideArtistArtwork>(4 * 1024 * 1024) {
        override fun sizeOf(key: String, value: IrideArtistArtwork): Int = value.bitmap.allocationByteCount
    }

    init {
        scope.launch {
            // Restore small verified photos from Coil's disk cache without any network requests.
            selections.entries.toList().takeLast(6).forEach { (key, selection) ->
                loadPreview(selection.candidate, diskOnly = true)?.let { publishPreview(key, it) }
            }
        }
    }

    fun cached(genre: String): IrideArtistArtwork? {
        val key = GenreArtworkParser.normalize(genre)
        return cache.get(key) ?: previews.get(key)
    }

    fun prefetch(genre: String) {
        val key = GenreArtworkParser.normalize(genre)
        if (cached(genre) != null) return
        synchronized(pending) {
            if (key in pending || pending.size >= 2) return
            val job = scope.launch(start = CoroutineStart.LAZY) {
                try {
                    load(genre, highResolution = false)
                } finally {
                    synchronized(pending) { pending.remove(key) }
                }
            }
            pending[key] = job
            job.start()
        }
    }

    suspend fun load(genre: String, highResolution: Boolean = true): IrideArtistArtwork? = withContext(Dispatchers.IO) {
        val key = GenreArtworkParser.normalize(genre)
        genreLocks[(key.hashCode() and Int.MAX_VALUE) % genreLocks.size].withLock {
            cache.get(key)?.let { return@withLock it }
            if (!highResolution) previews.get(key)?.let { return@withLock it }
            val now = System.currentTimeMillis()
            if ((failures[key] ?: 0L) > now) return@withLock cached(genre)
            try {
                selections[key]?.let { saved ->
                    (previews.get(key) ?: loadPreview(saved.candidate))?.let { preview ->
                        publishPreview(key, preview)
                        val artwork = if (highResolution) detailed(saved.candidate) ?: preview else preview
                        if (highResolution && artwork !== preview) publishDetail(key, artwork)
                        return@withLock artwork
                    }
                }
                val selected = select(genre, key, repository.appleCandidates(genre))
                    ?: select(genre, key, repository.deezerCandidates(genre))
                if (selected != null) {
                    publishPreview(key, selected.artwork)
                    failures.remove(key)
                    val artwork = if (highResolution) detailed(selected.candidate) ?: selected.artwork else selected.artwork
                    if (highResolution && artwork !== selected.artwork) publishDetail(key, artwork)
                    artwork
                } else {
                    failures[key] = now + 5 * 60_000L
                    null
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                failures[key] = now + 5 * 60_000L
                cached(genre)
            }
        }
    }

    private fun publishPreview(key: String, artwork: IrideArtistArtwork) {
        previews.put(key, artwork)
        updates.tryEmit(key)
    }

    private fun publishDetail(key: String, artwork: IrideArtistArtwork) {
        cache.put(key, artwork)
        updates.tryEmit(key)
    }

    private suspend fun select(genre: String, key: String, candidates: List<GenreArtworkCandidate>): GenreArtworkPreview? {
        val eligible = candidates.filter { candidate ->
            selections.none { (name, selected) -> name != key && selected.candidate.identity == candidate.identity }
        }.sortedWith(compareByDescending<GenreArtworkCandidate> { minOf(it.nativeSide, resolution) }
            .thenByDescending { it.editorial }).take(6)
        // A valid first batch can be displayed without downloading every remaining candidate.
        for (batch in eligible.chunked(3)) {
            val previews = supervisorScope {
                batch.map { candidate -> async { preview(candidate) } }.awaitAll()
                    .filterNotNull().sortedByDescending { it.score }
            }
            for (preview in previews) {
                if (selections.any { (name, selected) ->
                        name != key && genreArtworkIsDuplicate(preview.candidate.identity, preview.fingerprint,
                            selected.candidate.identity, selected.fingerprint)
                    }) continue
                if (!repository.matchesGenre(genre, preview.candidate)) continue
                val selection = GenreArtworkSelection(preview.candidate, preview.fingerprint)
                val claimed = mutex.withLock {
                    if (selections.any { (name, selected) ->
                            name != key && genreArtworkIsDuplicate(selection.candidate.identity, selection.fingerprint,
                                selected.candidate.identity, selected.fingerprint)
                        }) false else {
                        selections[key] = selection
                        preferences.edit().putString(key, Json.encodeToString(selection)).apply()
                        true
                    }
                }
                if (claimed) return preview
            }
        }
        return null
    }

    private suspend fun preview(candidate: GenreArtworkCandidate): GenreArtworkPreview? {
        val bitmap = download(candidate, 384) ?: return null
        return withContext(Dispatchers.Default) {
            val square = squareGenreArtwork(bitmap, 384)
            var retained = false
            try {
                val sample = Bitmap.createScaledBitmap(square, 64, 64, true)
                val pixels = IntArray(64 * 64)
                sample.getPixels(pixels, 0, 64, 0, 0, 64, 64)
                if (sample !== square) sample.recycle()
                val photographic = artistPhotographicQuality(pixels, 64, 64)
                val definition = artistDefinitionQuality(pixels, 64, 64, candidate.nativeSide)
                // Genre backgrounds prefer photos of scenes and instruments over a featured singer.
                if (photographic < 0.45f || definition < 0.45f || ArtistPortraitDetector.quality(square) >= 0.25f) {
                    return@withContext null
                }
                val hashSample = Bitmap.createScaledBitmap(square, 9, 8, true)
                val hashPixels = IntArray(9 * 8)
                hashSample.getPixels(hashPixels, 0, 9, 0, 0, 9, 8)
                hashSample.recycle()
                GenreArtworkPreview(candidate, genreArtworkFingerprint(hashPixels),
                    photographic * 0.60f + definition * 0.25f +
                        (candidate.nativeSide.toFloat() / 2400).coerceAtMost(1f) * 0.10f +
                        (if (candidate.editorial) 0.05f else 0f),
                    IrideArtistArtwork(square, square.irideArtistArtworkTint(1f))).also { retained = true }
            } finally {
                if (!retained && square !== bitmap) square.recycle()
            }
        }
    }

    private suspend fun loadPreview(candidate: GenreArtworkCandidate, diskOnly: Boolean = false): IrideArtistArtwork? {
        val bitmap = download(candidate, 384, diskOnly) ?: return null
        return withContext(Dispatchers.Default) {
            val square = squareGenreArtwork(bitmap, 384)
            IrideArtistArtwork(square, square.irideArtistArtworkTint(1f))
        }
    }

    private suspend fun detailed(candidate: GenreArtworkCandidate): IrideArtistArtwork? {
        val target = minOf(candidate.nativeSide, resolution)
        val bitmap = download(candidate, target) ?: return null
        if (minOf(bitmap.width, bitmap.height) < GenreArtworkParser.MINIMUM_SIDE) return null
        return withContext(Dispatchers.Default) {
            val square = squareGenreArtwork(bitmap, target)
            IrideArtistArtwork(square, square.irideArtistArtworkTint(1f))
        }
    }

    private suspend fun download(candidate: GenreArtworkCandidate, side: Int, diskOnly: Boolean = false): Bitmap? = try {
        downloads.withPermit {
            context.imageLoader.execute(
                ImageRequest.Builder(context).data(candidate.imageAt(side)).size(side, side)
                    .precision(Precision.EXACT).allowHardware(false).bitmapConfig(Bitmap.Config.ARGB_8888)
                    .networkCachePolicy(if (diskOnly) CachePolicy.DISABLED else CachePolicy.ENABLED)
                    .memoryCacheKey("iride_genre_artwork_v1:$side:${candidate.identity}").build(),
            ).image?.toBitmap()
        }
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (_: Exception) {
        null
    }
}

private fun squareGenreArtwork(bitmap: Bitmap, requestedSide: Int): Bitmap {
    val side = minOf(bitmap.width, bitmap.height)
    val output = minOf(side, requestedSide)
    if (bitmap.width == output && bitmap.height == output) return bitmap
    val left = (bitmap.width - side) / 2
    val top = (bitmap.height - side) / 2
    return Bitmap.createBitmap(output, output, Bitmap.Config.ARGB_8888).also {
        Canvas(it).drawBitmap(bitmap, Rect(left, top, left + side, top + side),
            Rect(0, 0, output, output), Paint(Paint.FILTER_BITMAP_FLAG))
    }
}

@EntryPoint
@InstallIn(SingletonComponent::class)
internal interface GenreArtworkEntryPoint {
    fun genreArtworkLoader(): GenreArtworkLoader
}

@Composable
internal fun rememberGenreArtworkLoader(): GenreArtworkLoader {
    val context = LocalContext.current.applicationContext
    return remember(context) {
        EntryPointAccessors.fromApplication(context, GenreArtworkEntryPoint::class.java).genreArtworkLoader()
    }
}

@Composable
internal fun rememberGenreArtwork(genre: String): State<IrideArtistArtwork?> {
    val loader = rememberGenreArtworkLoader()
    val artwork = remember(genre) { mutableStateOf(loader.cached(genre)) }
    LaunchedEffect(genre, loader) {
        coroutineScope {
            launch(start = CoroutineStart.UNDISPATCHED) {
                val key = GenreArtworkParser.normalize(genre)
                loader.artworkUpdates.collect { updated ->
                    if (updated == key) loader.cached(genre)?.let { artwork.value = it }
                }
            }
            loader.load(genre)?.let { artwork.value = it }
        }
    }
    return artwork
}
