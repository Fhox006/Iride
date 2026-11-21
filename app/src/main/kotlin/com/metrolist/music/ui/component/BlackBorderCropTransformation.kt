package com.metrolist.music.ui.component

import android.graphics.Bitmap
import coil3.size.Size
import coil3.transform.Transformation

/** Removes only clear, contiguous black bars from thumbnail edges. */
internal object BlackBorderCropTransformation : Transformation() {
    override val cacheKey: String = "iride-black-border-crop-v1"

    override suspend fun transform(input: Bitmap, size: Size): Bitmap {
        if (input.width < 32 || input.height < 32) return input

        fun isBlackBorderRow(y: Int): Boolean {
            val step = (input.width / 96).coerceAtLeast(1)
            var samples = 0
            var dark = 0
            for (x in 0 until input.width step step) {
                val color = input.getPixel(x, y)
                val alpha = color ushr 24
                val red = color shr 16 and 0xFF
                val green = color shr 8 and 0xFF
                val blue = color and 0xFF
                if (alpha > 200 && red <= 18 && green <= 18 && blue <= 18) dark++
                samples++
            }
            return samples > 0 && dark.toFloat() / samples >= 0.97f
        }

        fun isBlackBorderColumn(x: Int): Boolean {
            val step = (input.height / 96).coerceAtLeast(1)
            var samples = 0
            var dark = 0
            for (y in 0 until input.height step step) {
                val color = input.getPixel(x, y)
                val alpha = color ushr 24
                val red = color shr 16 and 0xFF
                val green = color shr 8 and 0xFF
                val blue = color and 0xFF
                if (alpha > 200 && red <= 18 && green <= 18 && blue <= 18) dark++
                samples++
            }
            return samples > 0 && dark.toFloat() / samples >= 0.97f
        }

        val maxVerticalCrop = (input.height * 0.20f).toInt()
        val maxHorizontalCrop = (input.width * 0.20f).toInt()
        var top = 0
        var bottom = input.height
        var left = 0
        var right = input.width
        while (top < maxVerticalCrop && isBlackBorderRow(top)) top++
        while (input.height - bottom < maxVerticalCrop && isBlackBorderRow(bottom - 1)) bottom--
        while (left < maxHorizontalCrop && isBlackBorderColumn(left)) left++
        while (input.width - right < maxHorizontalCrop && isBlackBorderColumn(right - 1)) right--

        // Tiny dark outlines and mostly-black artwork should retain original framing.
        if (top + input.height - bottom < input.height * 0.02f) {
            top = 0
            bottom = input.height
        }
        if (left + input.width - right < input.width * 0.02f) {
            left = 0
            right = input.width
        }
        if (right - left < input.width * 0.60f || bottom - top < input.height * 0.60f) return input
        if (left == 0 && top == 0 && right == input.width && bottom == input.height) return input
        return Bitmap.createBitmap(input, left, top, right - left, bottom - top)
    }
}
