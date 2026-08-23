/**
 * Iride Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.utils

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.createBitmap
import androidx.core.graphics.drawable.toBitmap
import androidx.core.graphics.withClip
import androidx.core.graphics.withTranslation
import androidx.palette.graphics.Palette
import coil3.ImageLoader
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.toBitmap
import com.metrolist.music.R
import com.metrolist.music.ui.component.LyricsBackgroundStyle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import kotlin.math.abs
import kotlin.math.roundToInt

object ComposeToImage {
    suspend fun createLyricsImage(
        context: Context,
        coverArtUrl: String?,
        songTitle: String,
        artistName: String,
        lyrics: String,
        width: Int,
        height: Int,
        backgroundColor: Int? = null,
        backgroundStyle: LyricsBackgroundStyle = LyricsBackgroundStyle.SOLID,
        textColor: Int? = null,
        secondaryTextColor: Int? = null,
        lyricsAlignment: Layout.Alignment = Layout.Alignment.ALIGN_CENTER,
    ): Bitmap =
        withContext(Dispatchers.Default) {
            val imageWidth = 1440
            val padding = 80f
            val contentWidth = imageWidth - (padding * 2)

            val defaultBackgroundColor = 0xFF121212.toInt()
            val defaultTextColor = 0xFFFFFFFF.toInt()
            val defaultSecondaryTextColor = 0xB3FFFFFF.toInt()

            val bgColor = backgroundColor ?: defaultBackgroundColor
            val mainTextColor = textColor ?: defaultTextColor
            val secondaryTxtColor = secondaryTextColor ?: defaultSecondaryTextColor

            val interRegular = try {
                ResourcesCompat.getFont(context, R.font.inter_variable)
            } catch (_: Exception) {
                null
            } ?: Typeface.DEFAULT
            val interBold = Typeface.create(interRegular, Typeface.BOLD)

            val titlePaint = TextPaint().apply {
                color = mainTextColor
                textSize = 80f
                typeface = interBold
                isAntiAlias = true
            }

            val artistPaint = TextPaint().apply {
                color = secondaryTxtColor
                textSize = 60f
                typeface = interRegular
                isAntiAlias = true
            }

            val lyricsPaint = TextPaint().apply {
                color = mainTextColor
                typeface = interBold
                isAntiAlias = true
                textSize = when {
                    lyrics.length < 80 -> 110f
                    lyrics.length < 180 -> 90f
                    else -> 70f
                }
            }

            val appNamePaint = TextPaint().apply {
                color = secondaryTxtColor
                textSize = 54f
                typeface = interBold
                isAntiAlias = true
            }

            val headerTextMaxWidth = contentWidth - 300f

            val titleLayout = StaticLayout.Builder
                .obtain(songTitle, 0, songTitle.length, titlePaint, headerTextMaxWidth.toInt())
                .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                .setMaxLines(1)
                .setEllipsize(android.text.TextUtils.TruncateAt.END)
                .build()

            val artistLayout = StaticLayout.Builder
                .obtain(artistName, 0, artistName.length, artistPaint, headerTextMaxWidth.toInt())
                .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                .setMaxLines(1)
                .setEllipsize(android.text.TextUtils.TruncateAt.END)
                .build()

            val lyricsLayout = StaticLayout.Builder
                .obtain(lyrics, 0, lyrics.length, lyricsPaint, contentWidth.toInt())
                .setAlignment(lyricsAlignment)
                .setLineSpacing(0f, 1.3f)
                .setIncludePad(false)
                .build()

            val headerHeight = 260f
            val lyricsHeight = lyricsLayout.height.toFloat()
            val footerHeight = 90f
            val spacing = 80f

            val imageHeight = (padding * 2) + headerHeight + spacing + lyricsHeight + spacing + footerHeight + padding

            val bitmap = createBitmap(imageWidth, imageHeight.toInt())
            val canvas = Canvas(bitmap)

            var coverArtBitmap: Bitmap? = null
            if (coverArtUrl != null) {
                try {
                    val imageLoader = ImageLoader(context)
                    val request = ImageRequest.Builder(context).data(coverArtUrl).size(300).allowHardware(false).build()
                    val result = imageLoader.execute(request)
                    coverArtBitmap = result.image?.toBitmap()
                } catch (_: Exception) {}
            }

            val backgroundRect = RectF(0f, 0f, imageWidth.toFloat(), imageHeight)
            val backgroundPaint = Paint().apply { isAntiAlias = true }

            when (backgroundStyle) {
                LyricsBackgroundStyle.SOLID -> {
                    backgroundPaint.color = bgColor
                    canvas.drawRect(backgroundRect, backgroundPaint)
                }
                LyricsBackgroundStyle.BLUR -> {
                    backgroundPaint.color = 0xFF000000.toInt()
                    canvas.drawRect(backgroundRect, backgroundPaint)
                    if (coverArtBitmap != null) {
                        val scaledBitmap = Bitmap.createScaledBitmap(coverArtBitmap, imageWidth / 10, imageHeight.toInt() / 10, true)
                        val blurredBitmap = fastBlur(scaledBitmap, 1f, 20)
                        if (blurredBitmap != null) {
                            canvas.drawBitmap(blurredBitmap, null, backgroundRect, null)
                            canvas.drawRect(backgroundRect, Paint().apply { color = 0x4D000000 })
                        }
                    }
                }
                LyricsBackgroundStyle.GRADIENT -> {
                    if (coverArtBitmap != null) {
                        val palette = Palette.from(coverArtBitmap).generate()
                        val vibrant = palette.getVibrantColor(bgColor)
                        val darkVibrant = palette.getDarkVibrantColor(bgColor)
                        backgroundPaint.shader = LinearGradient(0f, 0f, 0f, imageHeight, intArrayOf(vibrant, darkVibrant), null, Shader.TileMode.CLAMP)
                        canvas.drawRect(backgroundRect, backgroundPaint)
                    } else {
                        backgroundPaint.color = bgColor
                        canvas.drawRect(backgroundRect, backgroundPaint)
                    }
                }
            }

            val coverArtSize = 260f
            coverArtBitmap?.let {
                val rect = RectF(padding, padding, padding + coverArtSize, padding + coverArtSize)
                val path = Path().apply { addRoundRect(rect, 20f, 20f, Path.Direction.CW) }
                canvas.save()
                canvas.clipPath(path)
                canvas.drawBitmap(it, null, rect, null)
                canvas.restore()
            }

            val headerTextX = padding + coverArtSize + 40f
            val headerCenterY = padding + coverArtSize / 2f
            val headerTextHeight = titleLayout.height + artistLayout.height + 10f
            val titleY = headerCenterY - headerTextHeight / 2f

            canvas.save()
            canvas.translate(headerTextX, titleY)
            titleLayout.draw(canvas)
            canvas.translate(0f, titleLayout.height.toFloat() + 10f)
            artistLayout.draw(canvas)
            canvas.restore()

            val lyricsY = padding + headerHeight + spacing
            canvas.save()
            canvas.translate(padding, lyricsY)
            lyricsLayout.draw(canvas)
            canvas.restore()

            val footerY = lyricsY + lyricsHeight + spacing
            val logoSize = 90f
            val logoRect = RectF(padding, footerY, padding + logoSize, footerY + logoSize)
            canvas.drawOval(logoRect, Paint().apply { color = secondaryTxtColor; isAntiAlias = true })
            ContextCompat.getDrawable(context, R.drawable.small_icon)?.toBitmap()?.let {
                val logoPaint = Paint().apply {
                    colorFilter = PorterDuffColorFilter(bgColor, PorterDuff.Mode.SRC_IN)
                    isAntiAlias = true
                }
                val iconPadding = 18f
                val iconRect = RectF(logoRect.left + iconPadding, logoRect.top + iconPadding, logoRect.right - iconPadding, logoRect.bottom - iconPadding)
                canvas.drawBitmap(it, null, iconRect, logoPaint)
            }

            val appNameX = padding + logoSize + 25f
            val appNameY = footerY + logoSize / 2f - (appNamePaint.descent() + appNamePaint.ascent()) / 2f
            canvas.drawText("Iride", appNameX, appNameY, appNamePaint)

            return@withContext bitmap
        }

    /** Renders the "guess the song" game result card — same monochrome/mono-font look as the New Iride UI. */
    suspend fun createArtistGameResultImage(
        context: Context,
        artistThumbnailUrl: String?,
        artistName: String,
        timeText: String,
        isNewBest: Boolean,
        correctCount: Int,
        totalRounds: Int,
    ): Bitmap =
        withContext(Dispatchers.Default) {
            val imageWidth = 1080
            val padding = 80f

            val monoFont = try {
                ResourcesCompat.getFont(context, R.font.space_mono_regular)
            } catch (_: Exception) {
                null
            } ?: Typeface.MONOSPACE

            val backgroundColor = 0xFF000000.toInt()
            val onBackgroundColor = 0xFFFFFFFF.toInt()
            val secondaryColor = 0xB3FFFFFF.toInt()

            var avatarBitmap: Bitmap? = null
            if (artistThumbnailUrl != null) {
                try {
                    val imageLoader = ImageLoader(context)
                    val request = ImageRequest.Builder(context).data(artistThumbnailUrl).size(400).allowHardware(false).build()
                    avatarBitmap = imageLoader.execute(request).image?.toBitmap()
                } catch (_: Exception) {}
            }

            val namePaint = TextPaint().apply {
                color = onBackgroundColor
                textSize = 64f
                typeface = Typeface.create(monoFont, Typeface.BOLD)
                isAntiAlias = true
                textAlign = Paint.Align.CENTER
            }
            val timePaint = TextPaint().apply {
                color = onBackgroundColor
                textSize = 150f
                typeface = Typeface.create(monoFont, Typeface.BOLD)
                isAntiAlias = true
                textAlign = Paint.Align.CENTER
            }
            val labelPaint = TextPaint().apply {
                color = secondaryColor
                textSize = 40f
                typeface = monoFont
                isAntiAlias = true
                textAlign = Paint.Align.CENTER
            }
            val badgePaint = TextPaint().apply {
                color = backgroundColor
                textSize = 34f
                typeface = Typeface.create(monoFont, Typeface.BOLD)
                isAntiAlias = true
                textAlign = Paint.Align.CENTER
            }
            val appNamePaint = TextPaint().apply {
                color = secondaryColor
                textSize = 36f
                typeface = monoFont
                isAntiAlias = true
                textAlign = Paint.Align.CENTER
            }
            val statValuePaint = TextPaint().apply {
                color = onBackgroundColor
                textSize = 44f
                typeface = Typeface.create(monoFont, Typeface.BOLD)
                isAntiAlias = true
                textAlign = Paint.Align.CENTER
            }
            val statLabelPaint = TextPaint().apply {
                color = secondaryColor
                textSize = 28f
                typeface = monoFont
                isAntiAlias = true
                textAlign = Paint.Align.CENTER
            }

            val avatarSize = 320f
            val badgeHeight = if (isNewBest) 90f else 0f
            val statsBoxHeight = 40f + (statValuePaint.descent() - statValuePaint.ascent()) +
                10f + (statLabelPaint.descent() - statLabelPaint.ascent()) + 40f
            val imageHeight = padding + avatarSize + 50f + (namePaint.descent() - namePaint.ascent()) + 40f +
                badgeHeight + 40f + (timePaint.descent() - timePaint.ascent()) + 20f +
                (labelPaint.descent() - labelPaint.ascent()) + 50f + statsBoxHeight + 60f + padding

            val bitmap = createBitmap(imageWidth, imageHeight.toInt())
            val canvas = Canvas(bitmap)
            canvas.drawColor(backgroundColor)

            val centerX = imageWidth / 2f
            var cursorY = padding

            val avatarRect = RectF(centerX - avatarSize / 2f, cursorY, centerX + avatarSize / 2f, cursorY + avatarSize)
            if (avatarBitmap != null) {
                canvas.save()
                canvas.clipPath(Path().apply { addOval(avatarRect, Path.Direction.CW) })
                canvas.drawBitmap(avatarBitmap, null, avatarRect, null)
                canvas.restore()
            } else {
                canvas.drawOval(avatarRect, Paint().apply { color = secondaryColor; isAntiAlias = true })
            }
            canvas.drawOval(avatarRect, Paint().apply { color = onBackgroundColor; style = Paint.Style.STROKE; strokeWidth = 4f; isAntiAlias = true })
            cursorY += avatarSize + 50f

            cursorY -= namePaint.ascent()
            canvas.drawText(artistName, centerX, cursorY, namePaint)
            cursorY += namePaint.descent() + 40f

            if (isNewBest) {
                val badgeText = context.getString(R.string.guess_game_new_best).uppercase()
                val badgeWidth = badgePaint.measureText(badgeText) + 80f
                val badgeRect = RectF(centerX - badgeWidth / 2f, cursorY, centerX + badgeWidth / 2f, cursorY + 70f)
                canvas.drawRoundRect(badgeRect, 35f, 35f, Paint().apply { color = onBackgroundColor; isAntiAlias = true })
                val badgeTextY = badgeRect.centerY() - (badgePaint.descent() + badgePaint.ascent()) / 2f
                canvas.drawText(badgeText, centerX, badgeTextY, badgePaint)
                cursorY += 90f + 40f
            }

            cursorY -= timePaint.ascent()
            canvas.drawText(timeText, centerX, cursorY, timePaint)
            cursorY += timePaint.descent() + 20f

            cursorY -= labelPaint.ascent()
            canvas.drawText(context.getString(R.string.guess_game), centerX, cursorY, labelPaint)
            cursorY += labelPaint.descent() + 50f

            val statsBoxRect = RectF(centerX - 260f, cursorY, centerX + 260f, cursorY + statsBoxHeight)
            canvas.drawRoundRect(statsBoxRect, 28f, 28f, Paint().apply { color = 0x0FFFFFFF; isAntiAlias = true })
            var statsCursorY = statsBoxRect.top + 40f
            statsCursorY -= statValuePaint.ascent()
            canvas.drawText(
                context.getString(R.string.guess_game_result_correct, correctCount, totalRounds),
                centerX,
                statsCursorY,
                statValuePaint,
            )
            statsCursorY += statValuePaint.descent() + 10f
            statsCursorY -= statLabelPaint.ascent()
            canvas.drawText(context.getString(R.string.guess_game_result_correct_label), centerX, statsCursorY, statLabelPaint)
            cursorY += statsBoxHeight + 60f

            canvas.drawText("IRIDE", centerX, imageHeight - padding + 10f, appNamePaint)

            return@withContext bitmap
        }

    private fun fastBlur(
        sentBitmap: Bitmap,
        scale: Float,
        radius: Int,
    ): Bitmap? {
        val width = (sentBitmap.width * scale).roundToInt()
        val height = (sentBitmap.height * scale).roundToInt()

        if (width <= 0 || height <= 0) return null

        val bitmap = Bitmap.createScaledBitmap(sentBitmap, width, height, false)
        val w = bitmap.width
        val h = bitmap.height
        val pix = IntArray(w * h)
        bitmap.getPixels(pix, 0, w, 0, 0, w, h)
        val wm = w - 1
        val hm = h - 1
        val wh = w * h
        val div = radius + radius + 1
        val r = IntArray(wh)
        val g = IntArray(wh)
        val b = IntArray(wh)
        var rsum: Int
        var gsum: Int
        var bsum: Int
        var x: Int
        var y: Int
        var i: Int
        var p: Int
        var yp: Int
        var yi: Int
        var yw: Int
        val vmin = IntArray(Math.max(w, h))
        var divsum = div + 1 shr 1
        divsum *= divsum
        val dv = IntArray(256 * divsum)
        i = 0
        while (i < 256 * divsum) {
            dv[i] = i / divsum
            i++
        }
        yw = 0
        yi = 0
        val stack = Array(div) { IntArray(3) }
        var stackpointer: Int
        var stackstart: Int
        var sir: IntArray
        var rbs: Int
        var r1 = radius + 1
        var routsum: Int
        var goutsum: Int
        var boutsum: Int
        var rinsum: Int
        var ginsum: Int
        var binsum: Int
        y = 0
        while (y < h) {
            bsum = 0
            gsum = 0
            rsum = 0
            boutsum = 0
            goutsum = 0
            routsum = 0
            binsum = 0
            ginsum = 0
            rinsum = 0
            i = -radius
            while (i <= radius) {
                p = pix[yi + Math.min(wm, Math.max(i, 0))]
                sir = stack[i + radius]
                sir[0] = p and 0xff0000 shr 16
                sir[1] = p and 0x00ff00 shr 8
                sir[2] = p and 0x0000ff
                rbs = r1 - Math.abs(i)
                rsum += sir[0] * rbs
                gsum += sir[1] * rbs
                bsum += sir[2] * rbs
                if (i > 0) {
                    rinsum += sir[0]
                    ginsum += sir[1]
                    binsum += sir[2]
                } else {
                    routsum += sir[0]
                    goutsum += sir[1]
                    boutsum += sir[2]
                }
                i++
            }
            stackpointer = radius
            x = 0
            while (x < w) {
                r[yi] = dv[rsum]
                g[yi] = dv[gsum]
                b[yi] = dv[bsum]
                rsum -= routsum
                gsum -= goutsum
                bsum -= boutsum
                stackstart = stackpointer - radius + div
                sir = stack[stackstart % div]
                routsum -= sir[0]
                goutsum -= sir[1]
                boutsum -= sir[2]
                if (y == 0) {
                    vmin[x] = Math.min(x + radius + 1, wm)
                }
                p = pix[yw + vmin[x]]
                sir[0] = p and 0xff0000 shr 16
                sir[1] = p and 0x00ff00 shr 8
                sir[2] = p and 0x0000ff
                rinsum += sir[0]
                ginsum += sir[1]
                binsum += sir[2]
                rsum += rinsum
                gsum += ginsum
                bsum += binsum
                stackpointer = (stackpointer + 1) % div
                sir = stack[stackpointer % div]
                routsum += sir[0]
                goutsum += sir[1]
                boutsum += sir[2]
                rinsum -= sir[0]
                ginsum -= sir[1]
                binsum -= sir[2]
                yi++
                x++
            }
            yw += w
            y++
        }
        x = 0
        while (x < w) {
            bsum = 0
            gsum = 0
            rsum = 0
            boutsum = 0
            goutsum = 0
            routsum = 0
            binsum = 0
            ginsum = 0
            rinsum = 0
            yp = -radius * w
            i = -radius
            while (i <= radius) {
                yi = Math.max(0, yp) + x
                sir = stack[i + radius]
                sir[0] = r[yi]
                sir[1] = g[yi]
                sir[2] = b[yi]
                rbs = r1 - Math.abs(i)
                rsum += sir[0] * rbs
                gsum += sir[1] * rbs
                bsum += sir[2] * rbs
                if (i > 0) {
                    rinsum += sir[0]
                    ginsum += sir[1]
                    binsum += sir[2]
                } else {
                    routsum += sir[0]
                    goutsum += sir[1]
                    boutsum += sir[2]
                }
                if (i < hm) {
                    yp += w
                }
                i++
            }
            yi = x
            stackpointer = radius
            y = 0
            while (y < h) {
                pix[yi] = -0x1000000 or (dv[rsum] shl 16) or (dv[gsum] shl 8) or dv[bsum]
                rsum -= routsum
                gsum -= goutsum
                bsum -= boutsum
                stackstart = stackpointer - radius + div
                sir = stack[stackstart % div]
                routsum -= sir[0]
                goutsum -= sir[1]
                boutsum -= sir[2]
                if (x == 0) {
                    vmin[y] = Math.min(y + r1, hm) * w
                }
                p = x + vmin[y]
                sir[0] = r[p]
                sir[1] = g[p]
                sir[2] = b[p]
                rinsum += sir[0]
                ginsum += sir[1]
                binsum += sir[2]
                rsum += rinsum
                gsum += ginsum
                bsum += binsum
                stackpointer = (stackpointer + 1) % div
                sir = stack[stackpointer % div]
                routsum += sir[0]
                goutsum += sir[1]
                boutsum += sir[2]
                rinsum -= sir[0]
                ginsum -= sir[1]
                binsum -= sir[2]
                yi += w
                y++
            }
            x++
        }
        bitmap.setPixels(pix, 0, w, 0, 0, w, h)
        return bitmap
    }

    fun saveBitmapAsFile(
        context: Context,
        bitmap: Bitmap,
        fileName: String,
    ): Uri =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val contentValues =
                ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, "$fileName.png")
                    put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/Iride")
                }
            val uri =
                context.contentResolver.insert(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    contentValues,
                ) ?: throw IllegalStateException("Failed to create new MediaStore record")

            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
            }
            uri
        } else {
            val cachePath = File(context.cacheDir, "images")
            cachePath.mkdirs()
            val imageFile = File(cachePath, "$fileName.png")
            FileOutputStream(imageFile).use { outputStream ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
            }
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.FileProvider",
                imageFile,
            )
        }
}
