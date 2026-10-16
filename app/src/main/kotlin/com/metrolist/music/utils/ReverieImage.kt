package com.metrolist.music.utils

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.net.Uri
import androidx.compose.ui.geometry.Size
import androidx.core.content.FileProvider
import androidx.palette.graphics.Palette
import coil3.imageLoader
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.toBitmap
import com.metrolist.music.R
import com.metrolist.music.models.ReverieRecap
import com.metrolist.music.models.ReverieChapter
import com.metrolist.music.models.ReveriePalette
import com.metrolist.music.models.reverieEdition
import com.metrolist.music.ui.component.ArtistArtworkEntryPoint
import com.metrolist.music.ui.component.ArtistArtworkRequest
import com.metrolist.music.ui.component.ReverieFilmPainter
import com.metrolist.music.ui.component.irideGrainBrush
import com.metrolist.music.ui.utils.resize
import dagger.hilt.android.EntryPointAccessors
import java.io.File
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

internal suspend fun loadReverieImages(context: Context, recap: ReverieRecap): Map<String, Bitmap> = coroutineScope {
    val artistIds = recap.artists.take(1).map { it.id }.toSet()
    val soundtrackIds = recap.chapters.mapNotNull { recap.soundtrack(it)?.id }.toSet()
    val loader = EntryPointAccessors.fromApplication(context.applicationContext, ArtistArtworkEntryPoint::class.java).artistArtworkLoader()
    (recap.songs.take(3) + recap.songs.filter { it.id in soundtrackIds } + recap.artists.take(1) + recap.albums.take(1))
        .distinctBy { it.id }.map { item ->
        async {
            val bitmap = withTimeoutOrNull(8_000) image@ {
                if (item.id in artistIds) {
                    withTimeoutOrNull(2500) {
                        loader.loadForDisplay(ArtistArtworkRequest(item.id, item.title, item.artwork))?.artwork?.bitmap
                    }?.let { return@image it }
                }
                val imageSize = if (item.id == recap.albums.firstOrNull()?.id || item.id == recap.artists.firstOrNull()?.id) 960 else 640
                // Stored album artwork can be absent or stale; the representative song is a separate fallback.
                val candidates = listOf(item.artwork?.resize(imageSize, imageSize), item.artwork,
                    item.soundtrack.thumbnailUrl?.resize(imageSize, imageSize), item.soundtrack.thumbnailUrl)
                    .filterNotNull().filter(String::isNotBlank).distinct()
                for (url in candidates) {
                    val loaded = withTimeoutOrNull(2500) {
                        context.imageLoader.execute(ImageRequest.Builder(context).data(url)
                            .size(imageSize, imageSize).allowHardware(false).build()).image?.toBitmap()
                    }
                    if (loaded != null) return@image loaded
                }
                null
            } ?: reverieArtworkPlaceholder(item.title)
            item.id to bitmap
        }
    }.awaitAll().toMap()
}

internal suspend fun loadReverieBackgrounds(recap: ReverieRecap, images: Map<String, Bitmap>): Map<ReverieChapter, ReveriePalette> =
    withContext(Dispatchers.Default) {
        val edition = reverieEdition(recap.month)
        val palettes = images.mapValues { (_, bitmap) ->
            val palette = Palette.from(bitmap).clearFilters().resizeBitmapArea(4096).maximumColorCount(12).generate()
            palette.swatches.maxByOrNull { swatch ->
                swatch.population * (.65f + .35f * swatch.hsl[1])
            }?.rgb ?: edition.primary
        }
        val sources = recap.chapters.associateWith { chapter ->
            val category = recap.categories.firstOrNull()?.colorHex?.takeIf { chapter == ReverieChapter.COLOURS }
                ?.let { runCatching { android.graphics.Color.parseColor(it) }.getOrNull() }
            val genre = recap.genres.firstOrNull()?.let { com.metrolist.music.models.reverieGenreColor(it.name) }
                ?.takeIf { chapter == ReverieChapter.GENRES }
            category ?: genre ?: palettes[recap.backgroundArtworkId(chapter)] ?: edition.primary
        }
        com.metrolist.music.models.reverieChapterPalettes(recap.month, recap.chapters, sources)
    }

/** Missing network artwork is represented explicitly by an initial, never by an empty album frame. */
private fun reverieArtworkPlaceholder(title: String): Bitmap {
    val bitmap = Bitmap.createBitmap(480, 480, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    canvas.drawColor(0xFFF2F0EB.toInt())
    val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF3464C8.toInt(); style = android.graphics.Paint.Style.STROKE; strokeWidth = 2f
    }
    for (ring in 1..8) canvas.drawCircle(240f, 240f, ring * 26f, paint)
    paint.style = android.graphics.Paint.Style.FILL; paint.color = 0xFF24242A.toInt()
    paint.typeface = android.graphics.Typeface.MONOSPACE; paint.textSize = 180f
    paint.textAlign = android.graphics.Paint.Align.CENTER
    canvas.drawText(title.trim().take(1).uppercase(), 240f, 300f, paint)
    return bitmap
}

internal suspend fun renderReverieImage(context: Context, recap: ReverieRecap, images: Map<String, Bitmap>,
                                      backgrounds: Map<ReverieChapter, ReveriePalette>): Bitmap =
    withContext(Dispatchers.Default) {
        val bitmap = Bitmap.createBitmap(1080, 1680, Bitmap.Config.ARGB_8888)
        try {
            val canvas = Canvas(bitmap)
            canvas.scale(3f, 3f)
            ReverieFilmPainter(context, recap).drawPoster(canvas, images, backgrounds[ReverieChapter.POSTER])
            val grain = Paint().apply {
                shader = irideGrainBrush(1f, soft = false).createShader(Size(360f, 560f))
                alpha = 33
            }
            canvas.drawRect(0f, 0f, 360f, 560f, grain)
            bitmap
        } catch (error: Throwable) { bitmap.recycle(); throw error }
    }

internal suspend fun writeReverieImage(context: Context, month: String, image: Bitmap): Uri = withContext(Dispatchers.IO) {
    val folder = File(context.cacheDir, "reverie").apply { check(isDirectory || mkdirs()) }
    val file = File(folder, "Iride-$month-${UUID.randomUUID()}.png")
    try {
        file.outputStream().buffered().use { check(image.compress(Bitmap.CompressFormat.PNG, 100, it)) }
        FileProvider.getUriForFile(context, "${context.packageName}.FileProvider", file)
    } catch (error: Throwable) { file.delete(); throw error }
}

internal fun shareReverieImage(context: Context, uri: Uri) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "image/png"
        putExtra(Intent.EXTRA_STREAM, uri)
        clipData = ClipData.newUri(context.contentResolver, context.getString(R.string.reverie_name), uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, context.getString(R.string.reverie_share_image)))
}

internal fun copyReverieImage(context: Context, uri: Uri) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newUri(context.contentResolver, context.getString(R.string.reverie_name), uri))
}
