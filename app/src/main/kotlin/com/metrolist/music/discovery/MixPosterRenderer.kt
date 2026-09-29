package com.metrolist.music.discovery

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import android.text.Layout
import android.text.StaticLayout
import android.text.TextDirectionHeuristics
import android.text.TextPaint
import android.graphics.Typeface
import androidx.core.content.res.ResourcesCompat
import com.metrolist.music.R
import java.util.Locale
import kotlin.coroutines.coroutineContext
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random
import kotlinx.coroutines.ensureActive

internal object MixPosterRenderer {
    const val VERSION = 8

    suspend fun render(
        context: Context,
        size: Int,
        title: String,
        mood: MixMood,
        photo: Bitmap?,
        seed: Long,
    ): Bitmap {
        val background = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        var output: Bitmap? = null
        try {
            if (photo != null) drawPhoto(Canvas(background), background, photo, size, mood.color)
            else drawGraphicBackground(Canvas(background), size, mood, seed)
            val rendered = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
            output = rendered
            val canvas = Canvas(rendered)
            canvas.drawBitmap(background, 0f, 0f, null)
            if (photo != null) {
                drawRosette(canvas, background, size, mood, Random(seed))
            }
            val typeface = ResourcesCompat.getFont(context, R.font.anton) ?: Typeface.DEFAULT_BOLD
            drawTitle(canvas, title, size, mood.color, typeface)
            applyGrain(rendered, seed)
            return rendered
        } catch (error: Throwable) {
            output?.recycle()
            throw error
        } finally {
            background.recycle()
        }
    }

    private suspend fun drawPhoto(canvas: Canvas, background: Bitmap, photo: Bitmap, size: Int, moodColor: Int) {
        val scale = max(size.toFloat() / photo.width, size.toFloat() / photo.height)
        val width = photo.width * scale
        val height = photo.height * scale
        // RGB tinting preserves opacity; PorterDuff MULTIPLY with low alpha erased the photograph.
        val red = 0.94f + 0.06f * Color.red(moodColor) / 255f
        val green = 0.94f + 0.06f * Color.green(moodColor) / 255f
        val blue = 0.94f + 0.06f * Color.blue(moodColor) / 255f
        canvas.drawColor(Color.rgb(242, 240, 235))
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
            colorFilter = ColorMatrixColorFilter(ColorMatrix(floatArrayOf(
                1.05f * red, 0f, 0f, 0f, -6f,
                0f, 1.05f * green, 0f, 0f, -6f,
                0f, 0f, 1.05f * blue, 0f, -6f,
                0f, 0f, 0f, 1f, 0f,
            )))
        }
        canvas.drawBitmap(photo, null, RectF(
            (size - width) / 2, (size - height) / 2,
            (size + width) / 2, (size + height) / 2,
        ), paint)

        // Keep the photograph underneath the fine print texture instead of rebuilding it from dots.
        val spacing = (size * 0.006f).roundToInt().coerceAtLeast(2)
        val pixels = IntArray(size * size)
        background.getPixels(pixels, 0, size, 0, 0, size, size)
        val dot = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.argb(52, 16, 16, 22) }
        for (y in 0 until size step spacing) {
            coroutineContext.ensureActive()
            for (x in 0 until size step spacing) {
                val px = (x + spacing / 2).coerceAtMost(size - 1)
                val py = (y + spacing / 2).coerceAtMost(size - 1)
                val pixel = pixels[py * size + px]
                val luminance = (Color.red(pixel) * 0.299f + Color.green(pixel) * 0.587f +
                    Color.blue(pixel) * 0.114f) / 255f
                val radius = spacing * 0.23f * (1f - luminance)
                if (radius > 0.12f) canvas.drawCircle(px.toFloat(), py.toFloat(), radius, dot)
            }
        }
    }

    private fun drawRosette(canvas: Canvas, background: Bitmap, size: Int, mood: MixMood, random: Random) {
        val path = rosettePath(size, mood, random)
        val tint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            val r = Color.red(mood.color) / 255f
            val g = Color.green(mood.color) / 255f
            val b = Color.blue(mood.color) / 255f
            // Desaturate first, then map luminance to the mood ink without reversing it.
            colorFilter = ColorMatrixColorFilter(floatArrayOf(
                0.299f * r, 0.587f * r, 0.114f * r, 0f, 0f,
                0.299f * g, 0.587f * g, 0.114f * g, 0f, 0f,
                0.299f * b, 0.587f * b, 0.114f * b, 0f, 0f,
                0f, 0f, 0f, 1f, 0f,
            ))
        }
        canvas.save()
        canvas.clipPath(path)
        canvas.drawBitmap(background, 0f, 0f, tint)
        canvas.restore()
    }

    private fun rosettePath(size: Int, mood: MixMood, random: Random): Path {
        val radius = size * random.between(0.14f, 0.30f)
        val center = rosetteCenter(size.toFloat(), radius, random)
        val rotation = random.nextFloat() * (2 * PI).toFloat()
        val path = Path()
        val sharp = mood.sharpness > 0.6f
        val samples = if (sharp) mood.petals * 2 else 240
        repeat(samples) { index ->
            val angle = index * 2 * PI / samples
            val r = if (sharp) {
                radius * if (index % 2 == 0) 1f else 0.42f
            } else {
                radius * (0.82f + 0.18f * cos(angle * mood.petals).toFloat())
            }
            val x = center.x + r * cos(angle + rotation).toFloat()
            val y = center.y + r * sin(angle + rotation).toFloat()
            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.close()
        return path
    }

    internal data class RosetteCenter(val x: Float, val y: Float)

    internal fun rosetteCenter(size: Float, radius: Float, random: Random): RosetteCenter {
        val minimumY = radius
        val maximumY = (size * 0.62f - radius).coerceAtLeast(minimumY)
        if (random.nextFloat() < 0.15f) {
            val side = random.nextInt(2)
            return when (side) {
                0 -> RosetteCenter(random.between(radius * 0.4f, radius * 0.85f),
                    random.between(minimumY, maximumY))
                else -> RosetteCenter(random.between(size - radius * 0.85f, size - radius * 0.4f),
                    random.between(minimumY, maximumY))
            }
        }

        val minimum = radius
        val maximum = size - radius
        repeat(16) {
            val center = RosetteCenter(random.between(minimum, maximum), random.between(minimumY, maximumY))
            if (kotlin.math.hypot(center.x - size / 2f, center.y - size / 2f) >= size * 0.12f) return center
        }
        return RosetteCenter(minimum, random.between(minimumY, maximumY))
    }

    internal fun drawGraphicBackground(canvas: Canvas, size: Int, mood: MixMood, seed: Long) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb((Color.red(mood.color) * 0.4f).toInt(),
                (Color.green(mood.color) * 0.4f).toInt(), (Color.blue(mood.color) * 0.4f).toInt())
        }
        canvas.drawRect(0f, 0f, size.toFloat(), size.toFloat(), paint)
        canvas.drawPath(rosettePath(size, mood, Random(seed)), Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(255, 250, 242)
            alpha = 100
        })
    }

    private suspend fun applyGrain(bitmap: Bitmap, seed: Long) {
        val width = bitmap.width
        val pixels = IntArray(width * bitmap.height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, bitmap.height)
        val random = Random(seed xor 0x475241494EL)
        val cellSize = (width / 224).coerceAtLeast(1)
        val coarse = IntArray((width + cellSize - 1) / cellSize)
        for (y in 0 until bitmap.height) {
            coroutineContext.ensureActive()
            if (y % cellSize == 0) coarse.indices.forEach { coarse[it] = random.nextInt(-14, 15) }
            for (x in 0 until width) {
                val index = y * width + x
                val pixel = pixels[index]
                val noise = coarse[x / cellSize] + random.nextInt(-6, 7)
                pixels[index] = Color.argb(Color.alpha(pixel),
                    (Color.red(pixel) + noise).coerceIn(0, 255),
                    (Color.green(pixel) + noise).coerceIn(0, 255),
                    (Color.blue(pixel) + noise).coerceIn(0, 255))
            }
        }
        bitmap.setPixels(pixels, 0, width, 0, 0, width, bitmap.height)
    }

    internal fun drawTitle(canvas: Canvas, title: String, size: Int, color: Int, typeface: Typeface) {
        if (size <= 0) return
        val padding = size * 0.035f
        val width = size - 2 * padding
        val height = (size - padding - size * 0.62f) * 0.9f
        val top = size - padding - height
        val paint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            this.typeface = typeface
            textSize = size * 0.3f
            setShadowLayer(size * 0.006f, 0f, size * 0.003f, Color.argb(96, 0, 0, 0))
        }
        val words = title.trim().uppercase(Locale.ROOT).ifBlank { "MIX" }.split(Regex("[\\s\\p{Z}]+"))
        val lines = if (words.size <= 1) words else {
            val split = (1 until words.size).minBy { index ->
                val left = paint.measureText(words.take(index).joinToString(" "))
                val right = paint.measureText(words.drop(index).joinToString(" "))
                kotlin.math.abs(left - right)
            }
            listOf(words.take(split).joinToString(" "), words.drop(split).joinToString(" "))
        }
        val gap = size * 0.008f
        val rowHeight = (height - gap * (lines.size - 1)) / lines.size
        val bounds = Rect()
        lines.forEachIndexed { index, line ->
            paint.getTextBounds(line, 0, line.length, bounds)
            if (bounds.width() <= 0 || bounds.height() <= 0) return@forEachIndexed
            val layout = StaticLayout.Builder.obtain(line, 0, line.length, paint,
                kotlin.math.ceil(paint.measureText(line)).toInt().coerceAtLeast(1))
                .setTextDirection(TextDirectionHeuristics.FIRSTSTRONG_LTR)
                .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                .setIncludePad(false)
                .build()
            // StaticLayout supplies bidirectional shaping and system fallback glyphs for non-Latin titles.
            canvas.save()
            canvas.translate(padding, top + index * (rowHeight + gap))
            canvas.scale(width / bounds.width(), rowHeight / bounds.height())
            canvas.translate(-bounds.left.toFloat(), -layout.getLineBaseline(0) - bounds.top.toFloat())
            layout.draw(canvas)
            canvas.restore()
        }
    }

    private fun Random.between(from: Float, to: Float): Float = from + nextFloat() * (to - from)
}
