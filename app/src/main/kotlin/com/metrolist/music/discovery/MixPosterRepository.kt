package com.metrolist.music.discovery

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.AtomicFile
import coil3.ImageLoader
import coil3.imageLoader
import coil3.memory.MemoryCache
import coil3.request.ImageRequest
import coil3.request.CachePolicy
import coil3.request.allowHardware
import coil3.toBitmap
import com.metrolist.innertube.YouTube
import com.metrolist.innertube.models.PlaylistItem
import com.metrolist.music.ui.utils.resize
import java.io.File
import java.io.IOException
import java.security.MessageDigest
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import timber.log.Timber

private typealias Entry = MixPosterCacheEntry

internal data class CachedMixPoster(val imagePath: String, val photoSource: MixPosterPhotoSource)

internal class MixPosterRepository(private val context: Context) {
    companion object {
        fun cachedPoster(context: Context, mixId: String, excludedThumbnail: String? = null): CachedMixPoster? = try {
            val directory = File(context.cacheDir, "mood_mix_posters")
            val key = MessageDigest.getInstance("SHA-256")
                .digest(mixId.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) }
            val json = Json { ignoreUnknownKeys = true }
            val entry = json.decodeFromString<Entry>(
                AtomicFile(File(directory, "$key.json")).openRead().bufferedReader().use { it.readText() },
            )
            val usableSource = entry.photoSource == MixPosterPhotoSource.GRAPHIC ||
                MixPosterPolicy.isArtistSource(entry.photoSource, entry.photoUrl, excludedThumbnail)
            if (entry.mixId == mixId && usableSource && File(entry.imageName).name == entry.imageName) {
                File(directory, entry.imageName).takeIf { it.isFile }?.let {
                    CachedMixPoster(it.absolutePath, entry.photoSource)
                }
            } else null
        } catch (_: Exception) {
            null
        }

    }

    val imageLoader: ImageLoader = context.imageLoader.newBuilder()
        .memoryCache { MemoryCache.Builder().maxSizeBytes(32L * 1024 * 1024).build() }
        .diskCache(null)
        .build()

    private val directory = File(context.cacheDir, "mood_mix_posters")
    private val json = Json { ignoreUnknownKeys = true }
    private val mutex = Mutex()

    suspend fun prepare(
        mixes: List<PlaylistItem>,
        mood: MixMood,
        size: Int,
        revalidate: Boolean,
        onPoster: (String, String?) -> Unit,
    ): Set<String> = withContext(Dispatchers.IO) {
        mutex.withLock {
            if (!directory.exists() && !directory.mkdirs()) return@withLock mixes.map { it.id }.toSet()
            val entries = mixes.associate { it.id to read(it.id) }.toMutableMap()
            for (mix in mixes) {
                currentCoroutineContext().ensureActive()
                val old = entries[mix.id]
                val obsoleteSource = old?.photoSource == MixPosterPhotoSource.TRACK ||
                    old?.photoSource == MixPosterPhotoSource.PLAYLIST ||
                    (!mix.thumbnail.isNullOrBlank() && old?.photoUrl == mix.thumbnail)
                if (old != null && image(old).isFile && !obsoleteSource) {
                    image(old).setLastModified(System.currentTimeMillis())
                    onPoster(mix.id, image(old).absolutePath)
                } else if (old == null || obsoleteSource) {
                    val fallback = renderAndSave(mix, MixMood.fromMixTitle(mix.title) ?: mood,
                        size, null, null, mix.id.hashCode().toLong(),
                        0, null, photoSource = MixPosterPhotoSource.GRAPHIC)
                    if (fallback != null) {
                        entries[mix.id] = fallback
                        onPoster(mix.id, image(fallback).absolutePath)
                    } else onPoster(mix.id, null)
                } else onPoster(mix.id, null)
            }
            val now = System.currentTimeMillis()
            val changing = mixes.filter { mix ->
                val old = entries[mix.id]
                old == null || MixPosterPolicy.needsPhotoRetry(old.photoSource) ||
                    MixPosterPolicy.needsRefresh(
                        old.photoFetchedAt.takeIf { it > 0 } ?: old.generatedAt,
                        now,
                    )
            }.map { it.id }.toSet()

            // Cached posters remain usable while only outdated photos fetch playlist tracks.
            val pages = if (revalidate) {
                coroutineScope {
                    val requests = Semaphore(3)
                    mixes.filter { it.id in changing }.map { mix ->
                        async {
                            requests.withPermit {
                                mix.id to recover {
                                    withTimeoutOrNull(10_000) { YouTube.playlist(mix.id).getOrNull() }
                                }
                            }
                        }
                    }.awaitAll().toMap()
                }
            } else emptyMap()

            val assigned = cachedArtists().filterKeys { it !in changing }.toMutableMap()

            for (mix in mixes) {
                currentCoroutineContext().ensureActive()
                val mixMood = MixMood.fromMixTitle(mix.title) ?: mood
                val old = entries[mix.id]
                val page = pages[mix.id]
                val firstTrack = page?.songs?.firstOrNull()?.id ?: old?.firstTrackId
                val firstArtist = firstArtist(page?.songs?.firstOrNull()?.artists) ?: old?.firstArtistId
                val refresh = revalidate && (mix.id in changing || old?.rendererVersion != MixPosterRenderer.VERSION)
                val rerender = old == null || old.size != size || old.title != mix.title ||
                    old.mood != mixMood.name || old.rendererVersion != MixPosterRenderer.VERSION || !image(old).isFile
                val sourceFile = old?.takeIf {
                    MixPosterPolicy.isArtistSource(it.photoSource, it.photoUrl, mix.thumbnail)
                }
                    ?.sourceImageName?.let { File(directory, it) }
                val needsSourceCache = old != null &&
                    MixPosterPolicy.isArtistSource(old.photoSource, old.photoUrl, mix.thumbnail) && sourceFile?.isFile != true
                if (!refresh && !rerender && !needsSourceCache) continue

                val candidates = page?.songs.orEmpty().flatMap { it.artists }
                    .mapNotNull { artist -> artist.id?.takeIf { id -> id.isNotBlank() && !id.startsWith("name:") } }
                    .distinct()
                    .ifEmpty { listOfNotNull(old?.artistId?.takeUnless { it.startsWith("name:") }) }
                val artistAttempts = MixPosterPolicy.artistAttempts(candidates, assigned.values.toSet(),
                    old?.artistId, old?.artistId != null && old.photoSource == MixPosterPhotoSource.ARTIST)
                var bestArtistPhoto: LoadedPhoto? = null
                var selected: LoadedPhoto? = null
                if (refresh) {
                    withTimeoutOrNull(30_000) {
                        for (artist in artistAttempts) {
                            val url = resolvePhoto(artist) ?: continue
                            if (!mix.thumbnail.isNullOrBlank() && url == mix.thumbnail) continue
                            val photo = loadPhoto(url, bypassCache = true) as? PhotoLoadResult.Ready ?: continue
                            val candidate = LoadedPhoto(photo.bitmap, url, artist, MixPosterPhotoSource.ARTIST,
                                fetchedAt = now, luminance = photo.luminance)
                            if (MixPosterPolicy.isPhotoBrightEnough(photo.luminance)) {
                                selected = candidate
                                break
                            }
                            if (MixPosterPolicy.isBrighterThan(photo.luminance, bestArtistPhoto?.luminance)) {
                                bestArtistPhoto = candidate
                            }
                        }
                    }
                    if (selected == null) selected = bestArtistPhoto
                }

                if (selected?.luminance?.let { it >= MixPosterPolicy.MIN_AVERAGE_LUMINANCE } != true &&
                    old != null && MixPosterPolicy.isArtistSource(old.photoSource, old.photoUrl, mix.thumbnail) &&
                    sourceFile?.isFile == true) {
                    val bitmap = recover { BitmapFactory.decodeFile(sourceFile.absolutePath) }
                    val luminance = bitmap?.let(::averageLuminance)
                    if (bitmap != null && luminance != null) {
                        val candidate = LoadedPhoto(bitmap, old.photoUrl, old.artistId, old.photoSource,
                            sourceImageName = old.sourceImageName,
                            fetchedAt = old.photoFetchedAt.takeIf { it > 0 } ?: old.generatedAt,
                            luminance = luminance, ownsBitmap = true)
                        if (MixPosterPolicy.isBrighterThan(luminance, selected?.luminance)) {
                            selected?.takeIf { it.ownsBitmap }?.bitmap?.recycle()
                            selected = candidate
                        } else bitmap.recycle()
                    } else bitmap?.recycle()
                }
                if (selected?.luminance?.let { it >= MixPosterPolicy.MIN_AVERAGE_LUMINANCE } != true &&
                    old != null && MixPosterPolicy.isArtistSource(old.photoSource, old.photoUrl, mix.thumbnail) &&
                    !old.photoUrl.isNullOrBlank()) {
                    when (val photo = loadPhoto(old.photoUrl)) {
                        is PhotoLoadResult.Ready -> {
                            val candidate = LoadedPhoto(photo.bitmap, old.photoUrl, old.artistId, old.photoSource,
                                fetchedAt = old.photoFetchedAt.takeIf { it > 0 } ?: old.generatedAt,
                                luminance = photo.luminance)
                            if (MixPosterPolicy.isBrighterThan(photo.luminance, selected?.luminance)) {
                                selected?.takeIf { it.ownsBitmap }?.bitmap?.recycle()
                                selected = candidate
                            }
                        }
                        PhotoLoadResult.Unavailable -> Unit
                    }
                }
                if (selected == null && old != null && image(old).isFile && !rerender) {
                    old.artistId?.let { assigned[mix.id] = it }
                    continue
                }
                val changedPhoto = selected != null &&
                    (old == null || selected.url != old.photoUrl || selected.source != old.photoSource)
                val weeklyRefreshSucceeded = refresh && selected?.fetchedAt == now
                val seed = if (weeklyRefreshSucceeded || changedPhoto) mix.id.hashCode().toLong() xor now
                    else old?.seed ?: mix.id.hashCode().toLong()
                val saved = try {
                    renderAndSave(mix, mixMood, size, selected?.artist, selected?.url,
                        seed, now, firstTrack, selected?.bitmap,
                        selected?.source ?: MixPosterPhotoSource.GRAPHIC,
                        firstArtistId = if (refresh && selected?.sourceImageName != null) old?.firstArtistId else firstArtist,
                        sourceImageName = selected?.sourceImageName,
                        photoFetchedAt = selected?.fetchedAt ?: 0)
                } finally {
                    if (selected?.ownsBitmap == true) selected.bitmap.recycle()
                }
                if (saved != null) {
                    entries[mix.id] = saved
                    selected?.artist?.let { assigned[mix.id] = it }
                    currentCoroutineContext().ensureActive()
                    onPoster(mix.id, image(saved).absolutePath)
                }
            }
            trim(entries.values.filterNotNull().flatMap { listOfNotNull(it.imageName, it.sourceImageName) }.toSet())
            mixes.filter { entries[it.id]?.photoSource == null ||
                entries[it.id]?.photoSource == MixPosterPhotoSource.GRAPHIC }.map { it.id }.toSet()
        }
    }

    private data class LoadedPhoto(
        val bitmap: Bitmap,
        val url: String?,
        val artist: String?,
        val source: MixPosterPhotoSource,
        val sourceImageName: String? = null,
        val fetchedAt: Long,
        val luminance: Float? = null,
        val ownsBitmap: Boolean = false,
    )

    private sealed interface PhotoLoadResult {
        data class Ready(val bitmap: Bitmap, val luminance: Float) : PhotoLoadResult
        data object Unavailable : PhotoLoadResult
    }

    private fun firstArtist(artists: List<com.metrolist.innertube.models.Artist>?): String? =
        artists?.firstOrNull()?.let { it.id ?: "name:${it.name}" }

    private suspend fun resolvePhoto(artist: String): String? = recover {
        withTimeoutOrNull(5_000) {
            YouTube.artist(artist).getOrNull()?.artist?.thumbnail?.takeIf { it.isNotBlank() }
        }
    }

    private suspend fun loadPhoto(url: String, bypassCache: Boolean = false): PhotoLoadResult = recover {
        withTimeoutOrNull(8_000) {
            val bitmap = imageLoader.execute(ImageRequest.Builder(context).data(url.resize(768, 768)).size(768, 768)
                .memoryCachePolicy(if (bypassCache) CachePolicy.DISABLED else CachePolicy.ENABLED)
                .allowHardware(false).build()).image?.toBitmap() ?: return@withTimeoutOrNull PhotoLoadResult.Unavailable
            val luminance = averageLuminance(bitmap)
                ?: return@withTimeoutOrNull PhotoLoadResult.Unavailable
            PhotoLoadResult.Ready(bitmap, luminance)
        }
    } ?: PhotoLoadResult.Unavailable

    private fun averageLuminance(bitmap: Bitmap): Float? {
        if (bitmap.width <= 0 || bitmap.height <= 0) return null
        val pixels = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        return MixPosterPolicy.averageLuminance(pixels)
    }

    private suspend fun renderAndSave(
        mix: PlaylistItem,
        mood: MixMood,
        size: Int,
        artist: String?,
        photoUrl: String?,
        seed: Long,
        generatedAt: Long,
        firstTrack: String?,
        photo: Bitmap? = null,
        photoSource: MixPosterPhotoSource = MixPosterPhotoSource.ARTIST,
        firstArtistId: String? = null,
        sourceImageName: String? = null,
        photoFetchedAt: Long = generatedAt,
    ): Entry? = recover {
        withContext(Dispatchers.Default) {
            val bitmap = MixPosterRenderer.render(context, size, mix.title, mood, photo, seed)
            try {
                withContext(Dispatchers.IO) {
                    currentCoroutineContext().ensureActive()
                    val original = if (photo != null && sourceImageName == null) {
                        val name = "${key(mix.id)}-${UUID.randomUUID()}.source.png"
                        atomicWrite(File(directory, name)) { stream ->
                            if (!photo.compress(Bitmap.CompressFormat.PNG, 100, stream)) {
                                throw IOException("Source photo encoding failed")
                            }
                        }
                        name
                    } else sourceImageName
                    val entry = Entry(mix.id, mix.title, mood.name, firstTrack, artist, photoUrl, generatedAt,
                        seed, "${key(mix.id)}-${UUID.randomUUID()}.png", size,
                        rendererVersion = MixPosterRenderer.VERSION, photoSource = photoSource,
                        firstArtistId = firstArtistId, sourceImageName = original, photoFetchedAt = photoFetchedAt)
                    atomicWrite(image(entry)) { stream ->
                        if (!bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)) {
                            throw IOException("Poster encoding failed")
                        }
                    }
                    atomicWrite(metadata(mix.id)) { it.write(json.encodeToString(entry).toByteArray(Charsets.UTF_8)) }
                    entry
                }
            } finally {
                bitmap.recycle()
            }
        }
    }

    private fun read(id: String): Entry? = try {
        val file = AtomicFile(metadata(id))
        json.decodeFromString<Entry>(file.openRead().bufferedReader().use { it.readText() })
            .takeIf { it.mixId == id && File(it.imageName).name == it.imageName &&
                (it.sourceImageName == null || File(it.sourceImageName).name == it.sourceImageName) }
    } catch (_: IOException) {
        null
    } catch (_: IllegalArgumentException) {
        null
    }

    private fun image(entry: Entry) = File(directory, entry.imageName)
    private fun metadata(id: String) = File(directory, "${key(id)}.json")
    private fun key(id: String) = MessageDigest.getInstance("SHA-256")
        .digest(id.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) }

    private fun cachedArtists(): Map<String, String> = directory.listFiles().orEmpty()
        .filter { it.extension == "json" }
        .mapNotNull { file ->
            val entry = try { json.decodeFromString<Entry>(file.readText()) } catch (_: Exception) { null }
            entry?.takeIf { File(it.imageName).name == it.imageName && it.generatedAt > 0 }
                ?.artistId?.let { entry.mixId to it }
        }.toMap()

    private fun atomicWrite(file: File, write: (java.io.FileOutputStream) -> Unit) {
        val atomic = AtomicFile(file)
        val stream = atomic.startWrite()
        try {
            write(stream)
            atomic.finishWrite(stream)
        } catch (error: Throwable) {
            atomic.failWrite(stream)
            throw error
        }
    }

    private fun trim(protectedNames: Set<String>) {
        val files = directory.listFiles().orEmpty()
        val now = System.currentTimeMillis()
        val freshNames = files.filter { it.extension == "json" }.flatMap { file ->
            val entry = try { json.decodeFromString<Entry>(file.readText()) } catch (_: Exception) { null }
            if (entry != null && now - entry.photoFetchedAt < MixPosterPolicy.WEEK_MS) {
                listOfNotNull(entry.imageName, entry.sourceImageName)
            } else emptyList()
        }.toSet()
        var bytes = files.sumOf { it.length() }
        val limit = 100L * 1024 * 1024
        for (file in files.filter { it.extension == "png" && it.name !in protectedNames && it.name !in freshNames }
            .sortedBy { it.lastModified() }) {
            if (bytes <= limit) break
            val length = file.length()
            if (file.delete()) bytes -= length
        }
        for (file in files.filter { it.extension == "json" }) {
            val entry = try { json.decodeFromString<Entry>(file.readText()) } catch (_: Exception) { null }
            if (entry == null || (!image(entry).exists() && entry.sourceImageName?.let { File(directory, it).isFile } != true && entry.photoUrl.isNullOrBlank())) file.delete()
        }
    }

    private suspend fun <T> recover(block: suspend () -> T): T? = try {
        block().also { currentCoroutineContext().ensureActive() }
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (error: Exception) {
        Timber.w(error, "Unable to load mix poster")
        null
    }
}
