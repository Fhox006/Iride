package com.metrolist.music.playback.audio

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.util.UnstableApi
import kotlinx.coroutines.flow.MutableStateFlow
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.exp
import kotlin.math.sqrt

data class AudioBandLevels(
    val bass: Float = 0f,
    val mid: Float = 0f,
    val treble: Float = 0f,
)

/**
 * Lightweight PCM pass-through: splits the stream into three bands via two cascaded one-pole
 * lowpass filters differenced against each other (the cheap textbook 3-band crossover) and
 * reports smoothed RMS energy per band into [levelsSink], for the now-playing equalizer bars.
 * Reads the same buffers [com.metrolist.music.eq.audio.CustomEqualizerAudioProcessor] already
 * reads, before they reach the output — no android.media.audiofx.Visualizer, so no RECORD_AUDIO
 * permission (Visualizer requires it even for the app's own audio session).
 */
@UnstableApi
@Suppress("DEPRECATION")
class VisualizerTapAudioProcessor(
    private val levelsSink: MutableStateFlow<AudioBandLevels>,
) : AudioProcessor {

    private var sampleRate = 0
    private var channelCount = 0

    private var outputBuffer: ByteBuffer = EMPTY_BUFFER
    private var inputEnded = false

    private var bassAlpha = 0.0
    private var midAlpha = 0.0
    private var lpBass = 0.0
    private var lpMid = 0.0

    private var sumBass = 0.0
    private var sumMid = 0.0
    private var sumTreble = 0.0
    private var windowSamples = 0
    private var lastEmitNanos = 0L

    // Per-band ceiling the bars normalize against, instead of a fixed constant. Rises fast to
    // catch a loud passage (so busy mixes spread across the bar instead of pinning at 1 on all
    // three bands) but decays very slowly, so it stays elevated through a quiet outro — a fade-out
    // reads as small, subtle movement rather than being re-amplified to fill the bar.
    private var ceilBass = RmsNormalizationFloor
    private var ceilMid = RmsNormalizationFloor
    private var ceilTreble = RmsNormalizationFloor

    override fun configure(inputAudioFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
        sampleRate = inputAudioFormat.sampleRate
        channelCount = inputAudioFormat.channelCount
        if (inputAudioFormat.encoding != C.ENCODING_PCM_16BIT) {
            throw AudioProcessor.UnhandledAudioFormatException(inputAudioFormat)
        }
        bassAlpha = 1.0 - exp(-2.0 * Math.PI * BassCutoffHz / sampleRate)
        midAlpha = 1.0 - exp(-2.0 * Math.PI * MidCutoffHz / sampleRate)
        lpBass = 0.0
        lpMid = 0.0
        return inputAudioFormat
    }

    override fun isActive(): Boolean = true

    override fun queueInput(inputBuffer: ByteBuffer) {
        if (!inputBuffer.hasRemaining()) {
            outputBuffer = EMPTY_BUFFER
            return
        }

        analyze(inputBuffer)

        val out = replaceOutputBuffer(inputBuffer.remaining())
        out.put(inputBuffer)
        out.flip()
    }

    private fun analyze(inputBuffer: ByteBuffer) {
        if (sampleRate <= 0 || channelCount <= 0) return
        inputBuffer.order(ByteOrder.LITTLE_ENDIAN)

        val frameCount = inputBuffer.remaining() / 2 / channelCount
        val basePosition = inputBuffer.position()

        repeat(frameCount) { frameIndex ->
            // Mono-mix all channels — good enough for a decorative meter.
            var sum = 0
            repeat(channelCount) { channelIndex ->
                val sampleIndex = basePosition + (frameIndex * channelCount + channelIndex) * 2
                sum += inputBuffer.getShort(sampleIndex)
            }
            val sample = (sum.toDouble() / channelCount) / 32768.0

            lpBass += bassAlpha * (sample - lpBass)
            lpMid += midAlpha * (sample - lpMid)
            val bass = lpBass
            val mid = lpMid - lpBass
            val treble = sample - lpMid

            sumBass += bass * bass
            sumMid += mid * mid
            sumTreble += treble * treble
            windowSamples++
        }

        val now = System.nanoTime()
        if (windowSamples > 0 && now - lastEmitNanos >= EmitIntervalNanos) {
            val rmsBass = sqrt(sumBass / windowSamples)
            val rmsMid = sqrt(sumMid / windowSamples)
            val rmsTreble = sqrt(sumTreble / windowSamples)

            ceilBass = adaptCeiling(ceilBass, rmsBass)
            ceilMid = adaptCeiling(ceilMid, rmsMid)
            ceilTreble = adaptCeiling(ceilTreble, rmsTreble)

            levelsSink.value = AudioBandLevels(
                bass = normalize(rmsBass, ceilBass),
                mid = normalize(rmsMid, ceilMid),
                treble = normalize(rmsTreble, ceilTreble),
            )
            sumBass = 0.0
            sumMid = 0.0
            sumTreble = 0.0
            windowSamples = 0
            lastEmitNanos = now
        }
    }

    private fun adaptCeiling(ceiling: Double, rms: Double): Double {
        val target = rms.coerceAtLeast(RmsNormalizationFloor)
        val alpha = if (target > ceiling) CeilingAttack else CeilingRelease
        return ceiling + alpha * (target - ceiling)
    }

    private fun normalize(rms: Double, ceiling: Double): Float =
        (rms / ceiling).toFloat().coerceIn(0f, 1f)

    override fun queueEndOfStream() {
        inputEnded = true
    }

    override fun getOutput(): ByteBuffer {
        val output = outputBuffer
        outputBuffer = EMPTY_BUFFER
        return output
    }

    override fun isEnded(): Boolean = inputEnded && outputBuffer === EMPTY_BUFFER

    @Deprecated("Deprecated in AudioProcessor")
    override fun flush() {
        outputBuffer = EMPTY_BUFFER
        inputEnded = false
        lpBass = 0.0
        lpMid = 0.0
        sumBass = 0.0
        sumMid = 0.0
        sumTreble = 0.0
        windowSamples = 0
        ceilBass = RmsNormalizationFloor
        ceilMid = RmsNormalizationFloor
        ceilTreble = RmsNormalizationFloor
    }

    @Deprecated("Deprecated in AudioProcessor")
    override fun reset() {
        flush()
        sampleRate = 0
        channelCount = 0
        levelsSink.value = AudioBandLevels()
    }

    private fun replaceOutputBuffer(size: Int): ByteBuffer {
        if (outputBuffer.capacity() < size) {
            outputBuffer = ByteBuffer.allocateDirect(size).order(ByteOrder.nativeOrder())
        } else {
            outputBuffer.clear()
        }
        return outputBuffer
    }

    private companion object {
        private const val BassCutoffHz = 250.0
        private const val MidCutoffHz = 4000.0
        private const val EmitIntervalNanos = 50_000_000L // ~20 fps, plenty for a smoothed meter

        // Floor (and starting point) for the adaptive per-band ceiling — full-scale sine peaks at
        // 0.707 RMS, real mastered music sits far below that. Retune if bars read too flat or too
        // pinned before the ceiling has had a chance to adapt.
        private const val RmsNormalizationFloor = 0.09

        // Ceiling rises fast enough to catch a loud passage within a few emits (~150-300ms), but
        // releases slowly enough (~15-20s half-life) that it stays elevated through a quiet outro.
        private const val CeilingAttack = 0.15
        private const val CeilingRelease = 0.002

        private val EMPTY_BUFFER: ByteBuffer = ByteBuffer.allocateDirect(0).order(ByteOrder.nativeOrder())
    }
}
