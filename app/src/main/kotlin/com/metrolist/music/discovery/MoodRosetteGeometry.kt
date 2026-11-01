package com.metrolist.music.discovery

import android.graphics.Path
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

internal fun moodPhotoColorFilter(pigment: Int): android.graphics.ColorMatrixColorFilter {
    val red = .94f + .06f * android.graphics.Color.red(pigment) / 255f
    val green = .94f + .06f * android.graphics.Color.green(pigment) / 255f
    val blue = .94f + .06f * android.graphics.Color.blue(pigment) / 255f
    return android.graphics.ColorMatrixColorFilter(floatArrayOf(
        1.05f * red, 0f, 0f, 0f, -6f,
        0f, 1.05f * green, 0f, 0f, -6f,
        0f, 0f, 1.05f * blue, 0f, -6f,
        0f, 0f, 0f, 1f, 0f,
    ))
}

/** Shared print ornament: the static Mood posters and the film use the same petal geometry. */
internal fun moodRosettePath(
    x: Float, y: Float, radius: Float, petals: Int, sharpness: Float, rotation: Float,
    morphPetals: Int = petals, morph: Float = 0f, result: Path = Path(),
): Path {
    result.rewind()
    val sharp = sharpness > .6f && morph == 0f
    val samples = if (sharp) petals * 2 else 240
    repeat(samples) { i ->
        val angle = i * 2 * PI / samples
        val wave = cos(angle * petals) * (1 - morph) + cos(angle * morphPetals) * morph
        val r = if (sharp) radius * if (i % 2 == 0) 1f else .42f
            else radius * (.82f + .18f * wave.toFloat())
        val px = x + r * cos(angle + rotation).toFloat()
        val py = y + r * sin(angle + rotation).toFloat()
        if (i == 0) result.moveTo(px, py) else result.lineTo(px, py)
    }
    result.close()
    return result
}
