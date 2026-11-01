package com.metrolist.music.playback.audio

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.BaseAudioProcessor
import androidx.media3.common.util.UnstableApi
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.exp
import kotlin.math.roundToInt
import com.metrolist.music.playback.vocals.VocalTimeline

/** One PCM output hands off to prepared stems; live audio contributes only during the short transition. */
@UnstableApi
class VocalMixAudioProcessor(private val gain: () -> Float) : BaseAudioProcessor() {
    internal var timeline: () -> VocalTimeline? = { null }
    private var fourChannels = false
    private var currentGain = 1.0
    private var smoothing = 0.0
    private var sampleRate = 44100
    private var channels = 2
    private var mediaFrame = 0.0
    private var hasTimestamp = false
    private var pendingFrame = 0.0
    private var preparedWeight = 0.0
    private var fadeStep = 0.0

    fun setInputTimestamp(positionUs: Long) {
        pendingFrame = positionUs * VocalTimeline.SAMPLE_RATE / 1_000_000.0
    }

    fun inputDurationUs(bytes: Int): Long = bytes.toLong() * 1_000_000 / (channels * 2L * sampleRate)

    fun resetMediaTime() {
        hasTimestamp = false
        preparedWeight = 0.0
    }

    override fun onConfigure(inputAudioFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
        fourChannels = inputAudioFormat.channelCount == 4
        channels = inputAudioFormat.channelCount
        if (!fourChannels && channels !in 1..2) return AudioProcessor.AudioFormat.NOT_SET
        if (inputAudioFormat.encoding != C.ENCODING_PCM_16BIT) {
            if (fourChannels) throw AudioProcessor.UnhandledAudioFormatException(inputAudioFormat)
            return AudioProcessor.AudioFormat.NOT_SET
        }
        sampleRate = inputAudioFormat.sampleRate
        smoothing = 1.0 - exp(-1.0 / (inputAudioFormat.sampleRate * 0.02))
        fadeStep = VocalTimeline.SAMPLE_RATE.toDouble() / (sampleRate * VocalTimeline.FADE_FRAMES)
        return AudioProcessor.AudioFormat(
            inputAudioFormat.sampleRate,
            if (fourChannels) 2 else channels,
            inputAudioFormat.encoding,
        )
    }

    override fun queueInput(inputBuffer: ByteBuffer) {
        // Media3 also calls this with EMPTY_BUFFER while draining the pipeline.
        if (!inputBuffer.hasRemaining()) return
        if (!fourChannels) {
            mixStreaming(inputBuffer)
            return
        }
        inputBuffer.order(ByteOrder.LITTLE_ENDIAN)
        val output = replaceOutputBuffer(inputBuffer.remaining() / 2).order(ByteOrder.LITTLE_ENDIAN)
        val target = gain().coerceIn(0f, 1f).toDouble()
        while (inputBuffer.remaining() >= 8) {
            val left = inputBuffer.short.toInt()
            val right = inputBuffer.short.toInt()
            val baseLeft = inputBuffer.short.toInt()
            val baseRight = inputBuffer.short.toInt()
            currentGain += smoothing * (target - currentGain)
            output.putShort(mix(left, baseLeft))
            output.putShort(mix(right, baseRight))
        }
        output.flip()
    }

    private fun mixStreaming(input: ByteBuffer) {
        if (!hasTimestamp) { mediaFrame = pendingFrame; hasTimestamp = true }
        val output = replaceOutputBuffer(input.remaining()).order(ByteOrder.LITTLE_ENDIAN)
        val chunks = timeline()?.chunks.orEmpty()
        if (chunks.isEmpty() && preparedWeight == 0.0) {
            val frames = input.remaining() / (channels * 2)
            output.put(input).flip()
            mediaFrame += frames * VocalTimeline.SAMPLE_RATE.toDouble() / sampleRate
            return
        }
        input.order(ByteOrder.LITTLE_ENDIAN)
        val target = gain().coerceIn(0f, 1f).toDouble()
        val step = VocalTimeline.SAMPLE_RATE.toDouble() / sampleRate
        var chunkIndex = chunks.indexOfFirst { mediaFrame < it.endFrame }
        var coverageEnd = VocalTimeline.coverageEnd(chunks, chunkIndex)
        while (input.remaining() >= channels * 2) {
            while (chunkIndex in chunks.indices && mediaFrame >= chunks[chunkIndex].endFrame) {
                chunkIndex++
                coverageEnd = VocalTimeline.coverageEnd(chunks, chunkIndex)
            }
            val chunk = chunks.getOrNull(chunkIndex)?.takeIf { mediaFrame >= it.startFrame && mediaFrame < it.endFrame }
            val wantPrepared = chunk != null && target < 1.0 && mediaFrame < coverageEnd - VocalTimeline.FADE_FRAMES
            // Complementary weights avoid a forced silence or doubling the accompaniment gain.
            // Finish returning to live audio before a gap; adjacent prepared windows need no handoff.
            preparedWeight = if (chunk == null) 0.0 else if (wantPrepared) {
                (preparedWeight + fadeStep).coerceAtMost(1.0)
            } else (preparedWeight - fadeStep).coerceAtLeast(0.0)
            currentGain += smoothing * (target - currentGain)
            repeat(channels) { channel ->
                val live = input.short.toInt()
                val value = if (preparedWeight > 0.0 && chunk != null) {
                    val local = mediaFrame - chunk.startFrame
                    fun prepared(stemChannel: Int): Double {
                        val base = VocalTimeline.sample(chunk.channels[stemChannel], local).toDouble()
                        val original = VocalTimeline.sample(chunk.original[stemChannel], local).toDouble()
                        return (base + currentGain * (original - base)) * 32768.0
                    }
                    val selected = if (channels == 1) (prepared(0) + prepared(1)) * 0.5 else prepared(channel)
                    if (preparedWeight == 1.0) selected else live + preparedWeight * (selected - live)
                } else live.toDouble()
                output.putShort(value.roundToInt()
                    .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort())
            }
            mediaFrame += step
        }
        output.flip()
    }

    private fun mix(original: Int, backing: Int): Short =
        (backing + currentGain * (original - backing)).roundToInt()
            .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()

    override fun onFlush() {
        currentGain = gain().coerceIn(0f, 1f).toDouble()
        resetMediaTime()
    }
}
