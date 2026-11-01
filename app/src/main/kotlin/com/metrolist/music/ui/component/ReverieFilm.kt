package com.metrolist.music.ui.component

import android.content.Context
import android.graphics.Canvas
import android.graphics.Bitmap
import android.graphics.RectF
import android.graphics.Paint
import android.graphics.Typeface
import android.text.Layout as TextLayout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.core.content.res.ResourcesCompat
import com.metrolist.music.R
import com.metrolist.music.models.ReverieChapter
import com.metrolist.music.models.ReverieRecap
import com.metrolist.music.models.ReveriePalette
import com.metrolist.music.models.reverieEdition
import com.metrolist.music.models.reveriePalette
import java.text.NumberFormat
import java.time.format.DateTimeFormatter
import java.util.Locale

fun reverieChapterTitle(chapter: ReverieChapter): Int = when (chapter) {
    ReverieChapter.OVERTURE -> R.string.reverie_overture
    ReverieChapter.TIME -> R.string.reverie_time_title
    ReverieChapter.RHYTHM -> R.string.reverie_rhythm_title
    ReverieChapter.ARTIST -> R.string.reverie_artist_title
    ReverieChapter.SONG -> R.string.reverie_song_title
    ReverieChapter.GENRES -> R.string.reverie_genres_title
    ReverieChapter.COLOURS -> R.string.reverie_colours_title
    ReverieChapter.ALBUM -> R.string.reverie_album_title
    ReverieChapter.POSTER -> R.string.reverie_poster_title
}

/** The exported image is rendered separately from the responsive, animated film. */
internal class ReverieFilmPainter(private val context: Context, private val recap: ReverieRecap) {
    private val titleFont = ResourcesCompat.getFont(context, R.font.anton) ?: Typeface.DEFAULT_BOLD
    private val monoFont = ResourcesCompat.getFont(context, R.font.dm_sans_variable) ?: Typeface.MONOSPACE
    private val boldMonoFont = Typeface.create(monoFont, Typeface.BOLD)
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG)
    private data class TextKey(val value: String, val size: Float, val color: Int, val title: Boolean, val width: Int, val maxLines: Int, val bold: Boolean)
    private val textLayouts = android.util.LruCache<TextKey, StaticLayout>(128)
    private val format = NumberFormat.getIntegerInstance()
    private val month = recap.month.format(DateTimeFormatter.ofPattern("MMMM", Locale.getDefault()))
    private var paper = 0xFFF2F0EB.toInt()
    private val ink = 0xFF24242A.toInt()
    private fun label(id: Int) = context.getString(id)
    private fun number(value: Long) = format.format(value)
    private fun line(canvas: Canvas, y: Float, color: Int) = rect(canvas, 24f, y, 336f, y + .7f, color)

    private fun rect(canvas: Canvas, left: Float, top: Float, right: Float, bottom: Float, color: Int) {
        paint.color = color; paint.style = Paint.Style.FILL
        canvas.drawRect(left, top, right, bottom, paint)
    }

    private fun text(
        canvas: Canvas, value: String, x: Float, y: Float, size: Float, color: Int,
        title: Boolean = false, width: Float = 312f, maxLines: Int = 3, bold: Boolean = false,
    ): Float {
        val key = TextKey(value, size, color, title, width.toInt().coerceAtLeast(1), maxLines, bold)
        val layout = textLayouts.get(key) ?: run {
            val style = TextPaint(textPaint).apply {
                typeface = when { title -> titleFont; bold -> boldMonoFont; else -> monoFont }
                this.color = color; textSize = size
            }
            StaticLayout.Builder.obtain(value, 0, value.length, style, key.width)
                .setIncludePad(false).setMaxLines(maxLines).setEllipsize(android.text.TextUtils.TruncateAt.END)
                .setLineSpacing(0f, if (title) .97f else 1.12f).setAlignment(TextLayout.Alignment.ALIGN_NORMAL).build()
                .also { textLayouts.put(key, it) }
        }
        canvas.save(); canvas.translate(x, y); layout.draw(canvas); canvas.restore()
        return layout.height.toFloat()
    }

    /** Album-led print layout mirrors the film's visual hierarchy. */
    fun drawPoster(canvas: Canvas, images: Map<String, Bitmap>, palette: ReveriePalette? = null) {
        val colors = palette ?: reveriePalette(reverieEdition(recap.month).primary)
        paper = colors.background
        rect(canvas, 0f, 0f, 360f, 560f, paper)
        text(canvas, "IRIDE / " + label(R.string.reverie_name).uppercase(Locale.ROOT), 24f, 16f, 10f, ink, maxLines = 1)
        text(canvas, recap.month.year.toString(), 295f, 16f, 10f, ink, width = 41f, maxLines = 1)
        text(canvas, month.uppercase(Locale.getDefault()), 24f, 35f, 38f, ink, true, maxLines = 1)
        val hero = recap.albums.firstOrNull() ?: recap.songs.firstOrNull()
        hero?.let { item ->
            images[item.id]?.let { bitmap ->
                val bounds = RectF(60f, 88f, 300f, 328f)
                val scale = maxOf(bounds.width() / bitmap.width, bounds.height() / bitmap.height)
                val width = bitmap.width * scale; val height = bitmap.height * scale
                val destination = RectF(bounds.centerX() - width / 2, bounds.centerY() - height / 2,
                    bounds.centerX() + width / 2, bounds.centerY() + height / 2)
                canvas.save(); canvas.clipRect(bounds)
                paint.color = android.graphics.Color.WHITE
                canvas.drawBitmap(bitmap, null, destination, paint)
                canvas.restore()
            }
            text(canvas, label(if (recap.albums.isNotEmpty()) R.string.reverie_top_album else R.string.reverie_top_song),
                24f, 338f, 8f, ink, maxLines = 1)
            text(canvas, item.title, 24f, 351f, 22f, ink, true, maxLines = 1)
            text(canvas, item.subtitle, 24f, 378f, 10f, ink, maxLines = 1)
        }
        line(canvas, 397f, ink)
        text(canvas, label(R.string.reverie_your_artists), 24f, 405f, 8f, ink, maxLines = 1)
        recap.artists.take(5).forEachIndexed { index, artist ->
            val y = 421f + index * 15f
            text(canvas, "0${index + 1}", 24f, y, 9f, ink, width = 22f, maxLines = 1)
            text(canvas, artist.title, 53f, y, 11f, ink, width = 283f, maxLines = 1)
        }
        line(canvas, 499f, ink)
        listOf(
            number(recap.minutes) to label(R.string.reverie_minutes),
            number(recap.songs.size.toLong()) to label(R.string.reverie_tracks),
            number(recap.activeDays.toLong()) to label(R.string.reverie_active_days),
        ).forEachIndexed { index, (value, caption) ->
            val x = 24f + index * 108f
            text(canvas, value, x, 505f, 24f, ink, true, width = 102f, maxLines = 1)
            text(canvas, caption, x, 535f, 7f, ink, width = 102f, maxLines = 2)
        }
    }
}
