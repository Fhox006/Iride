package com.metrolist.music.ui.component

/** A separable, edge-clamped box pass keeps opaque portraits opaque without dark borders. */
internal fun artistBoxBlur(pixels: IntArray, width: Int, height: Int, horizontal: Boolean, radius: Int): IntArray {
    require(width > 0 && height > 0 && pixels.size == width * height && radius >= 0)
    val count = radius * 2 + 1
    val length = if (horizontal) width else height
    val lines = if (horizontal) height else width
    val stride = if (horizontal) 1 else width
    val result = IntArray(pixels.size)
    repeat(lines) { line ->
        val start = if (horizontal) line * width else line
        var alpha = 0
        var red = 0
        var green = 0
        var blue = 0
        fun accumulate(position: Int, sign: Int) {
            val pixel = pixels[start + position.coerceIn(0, length - 1) * stride]
            alpha += (pixel ushr 24) * sign
            red += ((pixel ushr 16) and 255) * sign
            green += ((pixel ushr 8) and 255) * sign
            blue += (pixel and 255) * sign
        }
        for (position in -radius..radius) accumulate(position, 1)
        repeat(length) { position ->
            result[start + position * stride] =
                ((alpha / count) shl 24) or ((red / count) shl 16) or
                    ((green / count) shl 8) or (blue / count)
            accumulate(position - radius, -1)
            accumulate(position + radius + 1, 1)
        }
    }
    return result
}
