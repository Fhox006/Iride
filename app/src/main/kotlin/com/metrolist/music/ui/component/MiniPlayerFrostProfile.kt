package com.metrolist.music.ui.component

import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Matrix
import android.graphics.Shader
import android.os.Build
import kotlin.math.PI
import kotlin.math.roundToInt
import kotlin.math.sin

private const val ProfileWidth = 33
private const val ProfileHeight = 129

// A fixed, softly bowed field. Its variation vanishes at both ends, keeping the page seam-free.
private val profile: FloatArray by lazy {
    FloatArray(ProfileWidth * ProfileHeight) { index ->
        val x = (index % ProfileWidth) / (ProfileWidth - 1f)
        val y = (index / ProfileWidth) / (ProfileHeight - 1f)
        val wave = (sin(x * PI * 2.0 + 0.7) * 0.65 + sin(x * PI * 3.1) * 0.35).toFloat()
        val fraction = (y + wave * IrideFolio.MiniPlayerFrostWaviness *
            sin(y * PI).toFloat()).coerceIn(0f, 1f)
        fraction * fraction * fraction * (fraction * (fraction * 6f - 15f) + 10f)
    }
}

/** Tiny cached mask textures describe blur strength and tint, never a screenshot of the page. */
internal fun miniPlayerFrostProfileShader(
    widthPx: Float,
    fadeHeightPx: Float,
    topPx: Float,
    colorArgb: Int = -1,
    opacity: (Float) -> Float = { it },
): Shader {
    val colorAlpha = colorArgb ushr 24
    val rgb = colorArgb and 0x00FFFFFF
    val pixels = IntArray(profile.size) { index ->
        val alpha = (opacity(profile[index]).coerceIn(0f, 1f) * colorAlpha).roundToInt()
        (alpha shl 24) or rgb
    }
    val bitmap = Bitmap.createBitmap(pixels, ProfileWidth, ProfileHeight, Bitmap.Config.ARGB_8888)
    return BitmapShader(bitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP).apply {
        setLocalMatrix(Matrix().apply {
            setScale(widthPx / (ProfileWidth - 1), fadeHeightPx / (ProfileHeight - 1))
            postTranslate(0f, topPx)
        })
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            setFilterMode(BitmapShader.FILTER_MODE_LINEAR)
        }
    }
}
