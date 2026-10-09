package com.metrolist.music.ui.component

import android.graphics.LinearGradient
import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import android.graphics.Shader
import android.os.Build
import androidx.annotation.RequiresApi
import kotlin.math.ceil
import kotlin.math.exp

private fun frostPassShader(pairCount: Int) = """
    uniform shader content;
    uniform shader strength;
    uniform float2 sampleAxis;
    uniform float2 inputSize;
    uniform float centerWeight;
    uniform float2 gaussianPairs[$pairCount];

    half4 sampleAt(float2 position) {
        return content.eval(clamp(position, float2(0.5), inputSize - float2(0.5)));
    }

    half4 main(float2 position) {
        float amount = strength.eval(position).a;
        if (amount <= 0.001) return sampleAt(position);
        half4 color = sampleAt(position) * centerWeight;
        for (int i = 0; i < $pairCount; i++) {
            float2 delta = sampleAxis * gaussianPairs[i].x * amount;
            color += (sampleAt(position - delta) + sampleAt(position + delta)) * gaussianPairs[i].y;
        }
        // Input outside the sampled/clipped layer is transparent. Restore its coverage before
        // making the glass opaque, otherwise premultiplied RGB creates a dark inner border.
        if (color.a <= 0.00001) return sampleAt(position);
        return half4(color.rgb / color.a, 1.0);
    }
""".trimIndent()

private data class FrostGaussianKernel(val centerWeight: Float, val pairs: FloatArray)

private fun frostSigma(radiusPx: Float): Float = (radiusPx / 4f).coerceAtLeast(0.5f)

private fun frostGaussianKernel(radiusPx: Float): FrostGaussianKernel {
    val sigma = frostSigma(radiusPx)
    val support = ceil(3f * sigma).toInt()
    val weights = FloatArray(support + 1) { offset ->
        exp(-offset.toDouble() * offset / (2.0 * sigma * sigma)).toFloat()
    }
    val normalization = weights[0] + 2f * weights.drop(1).sum()
    val pairs = FloatArray(((support + 1) / 2) * 2)
    for (pair in 0 until pairs.size / 2) {
        val first = pair * 2 + 1
        val second = first + 1
        val firstWeight = weights[first]
        val secondWeight = weights.getOrElse(second) { 0f }
        val totalWeight = firstWeight + secondWeight
        pairs[pair * 2] = (first * firstWeight + second * secondWeight) / totalWeight
        pairs[pair * 2 + 1] = totalWeight / normalization
    }
    return FrostGaussianKernel(weights[0] / normalization, pairs)
}

internal fun stableFrostSamplePadding(radiusPx: Float): Int =
    ceil(3f * frostSigma(radiusPx)).toInt() + 2

/**
 * Keep every pass at the page's resolution. Large native Gaussian blurs internally downsample;
 * thin moving glyphs can flicker as they cross that coarse grid. Dense Gaussian samples avoid
 * both the coarse grid and the faceted shapes caused by widely spaced five-tap passes. Adjacent
 * taps share a bilinear fetch. Only the sampling distance varies for the mini player's edge.
 */
@RequiresApi(Build.VERSION_CODES.TIRAMISU)
internal fun stableFrostEffect(
    radiusPx: Float,
    widthPx: Float,
    heightPx: Float,
    strength: Shader = LinearGradient(0f, 0f, 0f, 1f, -1, -1, Shader.TileMode.CLAMP),
): RenderEffect {
    val kernel = frostGaussianKernel(radiusPx)
    val program = frostPassShader(kernel.pairs.size / 2)
    var effect: RenderEffect? = null
    for (horizontal in listOf(true, false)) {
        val shader = RuntimeShader(program).apply {
            setInputShader("strength", strength)
            setFloatUniform("sampleAxis", if (horizontal) 1f else 0f, if (horizontal) 0f else 1f)
            setFloatUniform("inputSize", widthPx, heightPx)
            setFloatUniform("centerWeight", kernel.centerWeight)
            setFloatUniform("gaussianPairs", kernel.pairs)
        }
        val pass = RenderEffect.createRuntimeShaderEffect(shader, "content")
        effect = effect?.let { RenderEffect.createChainEffect(pass, it) } ?: pass
    }
    return checkNotNull(effect)
}
