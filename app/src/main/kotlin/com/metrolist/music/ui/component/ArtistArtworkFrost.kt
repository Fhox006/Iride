package com.metrolist.music.ui.component

import android.graphics.Bitmap
import kotlin.math.roundToInt

/** Small blurred sample for evaluating the photo's transition into the page pigment. */
internal fun frostedArtistBitmap(bitmap: Bitmap): Bitmap {
    val scale = minOf(1f, 240f / maxOf(bitmap.width, bitmap.height))
    val width = (bitmap.width * scale).roundToInt().coerceAtLeast(1)
    val height = (bitmap.height * scale).roundToInt().coerceAtLeast(1)
    val sample = Bitmap.createScaledBitmap(bitmap, width, height, true)
    var pixels = IntArray(width * height)
    sample.getPixels(pixels, 0, width, 0, 0, width, height)
    if (sample !== bitmap) sample.recycle()
    // Three separable box passes approximate a Gaussian in linear time.
    repeat(3) {
        pixels = artistBoxBlur(pixels, width, height, horizontal = true, radius = 12)
        pixels = artistBoxBlur(pixels, width, height, horizontal = false, radius = 12)
    }
    return Bitmap.createBitmap(pixels, width, height, Bitmap.Config.ARGB_8888)
}
