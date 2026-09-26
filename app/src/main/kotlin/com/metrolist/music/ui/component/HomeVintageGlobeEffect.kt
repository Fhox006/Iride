/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.component

import android.graphics.Bitmap
import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import android.graphics.Shader
import android.os.Build
import android.os.SystemClock
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.ImageShader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs

/**
 * "Pallina" treatment used **only** by the Home tab, gated behind the
 * "Advanced animation" setting (default ON) and the system reduced-motion
 * flag.
 *
 * - [rememberHomeScrollVelocity] samples scroll velocity throttled (~30fps
 *   max) with plain timestamps: no frame callbacks, no per-frame allocation
 *   storms.
 * - [homeGlobeField] applies the radial globe lens (faint bulge) with a
 *   vertical-only smash (squash & stretch on a soft spring from the caller)
 *   and speed blur on flings (whisper-soft at rest). No tilt, no lateral
 *   squeeze, no zoom, no layer shrink: the layer rect always covers the
 *   screen, and edge pixels clamp instead of mirroring, so no boundary can
 *   ever show at the margins.
 * - [HomeFilmViewfinderOverlay] draws static scanlines over the
 *   Home list. Monochrome only, so hues are untouched.
 *
 */

  /** Speed blur starts at this |lean| (lower = activates sooner during normal scrolling). */
  private const val HOME_BLUR_THRESHOLD = 0.15f

  /** Max vertical blur radius in px on a hard fling (includes the base blur). */
  private const val HOME_MAX_BLUR_PX = 4f

  /** Soft base blur: gentle enough that grain stays visible at rest. */
  private const val HOME_BASE_BLUR_PX = 0.15f

  /** Quiet scanlines: static, subtle, nothing pulses or flickers. */
  private const val CRT_SCANLINE_ALPHA = 0.035f

  /** Faint barrel: center bulges barely, margins stay clean. */
  private const val HOME_GLOBE_STRENGTH = 0.06f

  /** Lateral bubble kept near full. */
  private const val HOME_GLOBE_X = 0.95f

  /** Vertical bubble slightly reduced. */
  private const val HOME_GLOBE_Y = 0.85f

  /** No vertical smash — CRT is flat. */
  private const val HOME_SMASH_Y = 0f

  /** No zoom: the layer is never scaled, so nothing can ever be cut at the margins. */
  private const val HOME_ZOOM = 1.0f

/**
 * Signed smoothed scroll velocity in px/s for [state], sampled throttled.
 *
 * Emissions closer than 32ms are folded into the next window instead of
 * recomputing, so this writes state at most ~30 times per second during a
 * fling. Index jumps are converted to px with the average visible item size
 * so a fling across many rows doesn't read as zero. When scrolling stops the
 * value snaps back to 0 (callers smooth the visual side, so no decay loop).
 */
@Composable
fun rememberHomeScrollVelocity(state: LazyListState): Float {
    var velocity by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(state) {
        var lastIndex = state.firstVisibleItemIndex
        var lastOffset = state.firstVisibleItemScrollOffset
        var lastTime = SystemClock.uptimeMillis()
        var smooth = 0f

        // Idle watchdog: without this the last fling sample would stick.
        launch {
            snapshotFlow { state.isScrollInProgress }.collect { scrolling ->
                if (!scrolling) {
                    delay(90)
                    if (!state.isScrollInProgress) {
                        smooth = 0f
                        velocity = 0f
                        lastIndex = state.firstVisibleItemIndex
                        lastOffset = state.firstVisibleItemScrollOffset
                        lastTime = SystemClock.uptimeMillis()
                    }
                }
            }
        }

        snapshotFlow { state.firstVisibleItemIndex to state.firstVisibleItemScrollOffset }
            .collect { (index, offset) ->
                val now = SystemClock.uptimeMillis()
                val dtSec = (now - lastTime) / 1000f
                when {
                    dtSec in 0.032f..0.5f -> {
                        val avgSize = state.layoutInfo.visibleItemsInfo
                            .map { it.size }
                            .average()
                            .takeIf { it.isFinite() && it > 0f }
                            ?.toFloat() ?: 600f
                        val dPx = (index - lastIndex) * avgSize + (offset - lastOffset)
                        val instant = (dPx / dtSec).coerceIn(-30000f, 30000f)
                        smooth = (smooth * 0.6f + instant * 0.4f).coerceIn(-30000f, 30000f)
                        if (abs(smooth) < 5f) smooth = 0f
                        velocity = smooth
                        lastIndex = index
                        lastOffset = offset
                        lastTime = now
                    }
                    dtSec >= 0.5f -> {
                        // Stale baseline (e.g. after a jank freeze): reset silently.
                        lastIndex = index
                        lastOffset = offset
                        lastTime = now
                    }
                    // Else: too soon, keep the old baseline so the next delta
                    // spans a real window instead of spiking on near-zero dt.
                }
            }
    }
    return velocity
}

private const val HOME_GLOBE_AGSL = """
uniform shader composable;
uniform vec2 uSize;
uniform float uGlobe;
uniform float uGlobeX;
uniform float uGlobeY;
uniform float uBlur;

vec2 globeUv(vec2 fragCoord) {
    vec2 uv = fragCoord / uSize;
    vec2 p = uv - vec2(0.5);
    float r2 = dot(p, p);
    float f = 1.0 + uGlobe * r2 + uGlobe * 0.45 * r2 * r2;
    // Split bubble: lateral almost full, vertical slightly reduced.
    vec2 ff = vec2(1.0 + (f - 1.0) * uGlobeX, 1.0 + (f - 1.0) * uGlobeY);
    vec2 src = vec2(0.5) + p * ff;
    // Clamped sampling: edge pixels stretch instead of mirroring, so the
    // margins never show a mirrored "glass" copy of the feed.
    return clamp(src, vec2(0.0), vec2(1.0)) * uSize;
}

vec4 blur3(vec2 fragCoord, float stepPx) {
    vec4 acc = composable.eval(globeUv(fragCoord)) * 0.5;
    acc += composable.eval(globeUv(fragCoord + vec2(0.0, stepPx))) * 0.25;
    acc += composable.eval(globeUv(fragCoord - vec2(0.0, stepPx))) * 0.25;
    return acc;
}

vec4 main(vec2 fragCoord) {
    if (uBlur < 0.5) {
        return composable.eval(globeUv(fragCoord));
    }
    if (uBlur < 1.5) {
        return blur3(fragCoord, uBlur);
    }
    vec4 acc = vec4(0.0);
    float wSum = 0.0;
    for (int i = -3; i <= 3; i++) {
        float fi = float(i);
        float w = exp(-fi * fi * 0.35);
        acc += composable.eval(globeUv(fragCoord + vec2(0.0, fi * uBlur * 0.55))) * w;
        wSum += w;
    }
    return acc / wSum;
}
"""

/**
 * Cached globe [RuntimeShader] (API 33+). Null on older APIs or if the
 * driver rejects the shader — callers fall back to tilt + smash + plain
 * Y-blur so the feed never blanks.
 */
@Composable
fun rememberHomeGlobeShader(): RuntimeShader? {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return null
    return remember {
        try {
            RuntimeShader(HOME_GLOBE_AGSL)
        } catch (_: Throwable) {
            null
        }
    }
}

private fun homeFallbackBlur(blurPx: Float): android.graphics.RenderEffect? {
    if (blurPx < 0.5f || Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return null
    return RenderEffect.createBlurEffect(0f, blurPx, Shader.TileMode.CLAMP)
}

/**
 * The whole feed with a subtle retro-glass treatment: faint barrel bulge
 * plus speed blur on flings, whisper-soft at rest. No glitch bands, no
 * vignette, no zoom, no tilt, no lateral squeeze, no layer shrink — the
 * layer rect always covers the screen, and clamped edge sampling means the
 * margins never mirror.
 *
 * Keeps [contentAlpha] (screen entrance). Visual no-op when [enabled] is
 * false.
 *
 * @param lean signed -1f..1f driver, already animated by the caller.
 */
@Composable
fun Modifier.homeGlobeField(
    lean: Float,
    contentAlpha: Float,
    enabled: Boolean,
): Modifier {
    val globeShader = rememberHomeGlobeShader()
    return graphicsLayer {
        alpha = contentAlpha
        if (!enabled) {
            renderEffect = null
            return@graphicsLayer
        }
        val n = abs(lean).coerceIn(0f, 1f)
        // Vertical-only smash on top of the base zoom: gentle stretch while
        // moving, never lateral.
        scaleX = HOME_ZOOM
        scaleY = HOME_ZOOM * (1f + HOME_SMASH_Y * n)

        // Speed blur: soft at rest (grain stays visible), opens on
        // faster scroll. Lower threshold = activates sooner.
        val blurPx = HOME_BASE_BLUR_PX + if (n < HOME_BLUR_THRESHOLD) {
            0f
        } else {
            ((n - HOME_BLUR_THRESHOLD) / (1f - HOME_BLUR_THRESHOLD) *
                (HOME_MAX_BLUR_PX - HOME_BASE_BLUR_PX))
                .coerceIn(0f, HOME_MAX_BLUR_PX - HOME_BASE_BLUR_PX)
        }

        if (globeShader != null && size.width > 1f && size.height > 1f) {
            try {
                globeShader.setFloatUniform("uSize", size.width, size.height)
                globeShader.setFloatUniform("uGlobe", HOME_GLOBE_STRENGTH)
                globeShader.setFloatUniform("uGlobeX", HOME_GLOBE_X)
                globeShader.setFloatUniform("uGlobeY", HOME_GLOBE_Y)
                globeShader.setFloatUniform("uBlur", blurPx)
                renderEffect =
                    RenderEffect.createRuntimeShaderEffect(globeShader, "composable")
                        .asComposeRenderEffect()
            } catch (_: Throwable) {
                renderEffect = homeFallbackBlur(blurPx)?.asComposeRenderEffect()
            }
        } else {
            renderEffect = homeFallbackBlur(blurPx)?.asComposeRenderEffect()
        }
    }
}

@Composable
fun HomeFilmViewfinderOverlay(
    modifier: Modifier = Modifier,
) {
    // Scanlines: 1 px black every 3 px, tiled from a 1×3 bitmap.
    val scanlineBrush = remember {
        val bmp = Bitmap.createBitmap(1, 3, Bitmap.Config.ARGB_8888)
        bmp.setPixel(0, 2, android.graphics.Color.BLACK)
        ShaderBrush(ImageShader(bmp.asImageBitmap(), TileMode.Repeated, TileMode.Repeated))
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        // Scanlines only — completely static, nothing pulses or flickers.
        drawRect(brush = scanlineBrush, alpha = CRT_SCANLINE_ALPHA)
    }
}
