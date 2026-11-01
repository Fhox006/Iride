package com.metrolist.music.ui.component

import android.graphics.BlendMode
import android.graphics.ColorMatrixColorFilter
import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import androidx.annotation.RequiresApi
import kotlin.math.min

/** Interpolate adjacent Gaussian kernels, rather than revealing sharp content through a blur. */
@RequiresApi(Build.VERSION_CODES.S)
internal fun progressiveFrostEffect(
    radiusPx: Float,
    fadeHeightPx: Float,
    topPaddingPx: Float,
    widthPx: Float,
): RenderEffect {
    // Three kernels keep the native fallback compact; the high-radius base remains opaque.
    val radii = floatArrayOf(0f, 8f, 64f)
        .map { it * radiusPx / 64f }
    val kernels = radii.mapIndexed { level, radius ->
        val mask = RenderEffect.createShaderEffect(
            miniPlayerFrostProfileShader(widthPx, fadeHeightPx, topPaddingPx) { progress ->
                val target = radiusPx * progress
                val rising = if (level == 0) 1f else {
                    (target - radii[level - 1]) / (radius - radii[level - 1])
                }
                val falling = if (level == radii.lastIndex) 1f else {
                    (radii[level + 1] - target) / (radii[level + 1] - radius)
                }
                min(rising, falling).coerceIn(0f, 1f)
            },
        )
        val kernel = if (radius == 0f) {
            RenderEffect.createOffsetEffect(0f, 0f)
        } else {
            RenderEffect.createBlurEffect(radius, radius, Shader.TileMode.CLAMP)
        }
        RenderEffect.createBlendModeEffect(kernel, mask, BlendMode.DST_IN)
    }
    // The kernel weights sum to one. PLUS combines them into one image, not translucent layers
    // over the page; the sharp kernel has zero weight once the target exceeds the smallest blur.
    val combined = kernels.reduce { result, kernel ->
        RenderEffect.createBlendModeEffect(result, kernel, BlendMode.PLUS)
    }
    // Profile masks use 8-bit alpha. Force coverage to one despite their rounding errors.
    return RenderEffect.createColorFilterEffect(
        ColorMatrixColorFilter(
            floatArrayOf(
                1f, 0f, 0f, 0f, 0f,
                0f, 1f, 0f, 0f, 0f,
                0f, 0f, 1f, 0f, 0f,
                0f, 0f, 0f, 0f, 255f,
            ),
        ),
        combined,
    )
}
