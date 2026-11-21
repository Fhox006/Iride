package com.metrolist.music.discovery

import android.content.Context
import coil3.imageLoader
import coil3.request.ImageRequest
import coil3.request.CachePolicy
import coil3.request.SuccessResult
import coil3.request.allowHardware
import coil3.request.transformations
import coil3.toBitmap
import coil3.size.Size
import com.metrolist.innertube.models.SongItem
import com.metrolist.music.ui.component.BlackBorderCropTransformation
import com.metrolist.music.ui.component.ArtistPortraitDetector
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.ConcurrentHashMap

/** Numbered YouTube thumbnails are sampled frames, unlike the interchangeable cover sizes. */
internal class MusicVideoArtworkVerifier(private val context: Context) {
    internal data class Artwork(
        val width: Int,
        val height: Int,
        val fingerprint: VideoArtworkFingerprint,
        val faceQuality: Float,
    )
    private val cache = ConcurrentHashMap<String, Artwork>()

    internal data class VerifiedCover(
        val url: String,
        val fingerprint: VideoArtworkFingerprint,
        val faceQuality: Float,
    )

    suspend fun verify(song: SongItem): VerifiedCover? = coroutineScope {
        if (!song.id.matches(Regex("[A-Za-z0-9_-]{11}"))) return@coroutineScope null
        val frames = (1..3).map { frame ->
            async { inspect("https://i.ytimg.com/vi/${song.id}/$frame.jpg")?.fingerprint }
        }.awaitAll()
        // Missing frames are inconclusive: do not promote an unverified static cover.
        if (!hasChangingVideoFrames(frames.filterNotNull())) return@coroutineScope null
        // Prefer YouTube's full resolution video thumbnail, falling back to smaller variants.
        val candidates = listOf(
            "https://i.ytimg.com/vi/${song.id}/maxresdefault.jpg",
            "https://i.ytimg.com/vi/${song.id}/sddefault.jpg",
            "https://i.ytimg.com/vi/${song.id}/hqdefault.jpg",
            song.thumbnail,
        ).distinct()
        val cover = candidates.mapNotNull { url ->
            inspect(url)?.takeIf { artwork ->
                artwork.width >= 160 && artwork.height >= 90 && artwork.width.toFloat() / artwork.height in 1.35f..2.7f
            }?.let { url to it }
        }.maxByOrNull { (_, artwork) -> VideoArtworkQuality(artwork.width, artwork.height, artwork.faceQuality) }
            ?: return@coroutineScope null
        VerifiedCover(cover.first, cover.second.fingerprint, cover.second.faceQuality)
    }

    private suspend fun inspect(url: String): Artwork? {
        cache[url]?.let { return it }
        return withTimeoutOrNull(5_000) {
            val result = context.imageLoader.execute(
                // Compare actual source dimensions, not equally downsampled 320px previews.
                // Analysis must not seed the display cache with a small software bitmap.
                ImageRequest.Builder(context).data(url).size(Size.ORIGINAL).allowHardware(false)
                    .memoryCachePolicy(CachePolicy.DISABLED)
                    .transformations(BlackBorderCropTransformation).build()
            ) as? SuccessResult ?: return@withTimeoutOrNull null
            val bitmap = result.image.toBitmap()
            val fingerprint = VideoArtworkFingerprint.create(bitmap.width, bitmap.height, bitmap::getPixel)
                ?: return@withTimeoutOrNull null
            Artwork(bitmap.width, bitmap.height, fingerprint, ArtistPortraitDetector.quality(bitmap)).also {
                if (cache.size >= 256) cache.clear()
                cache[url] = it
            }
        }
    }
}
