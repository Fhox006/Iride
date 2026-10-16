package com.metrolist.music.playback.audio

import androidx.media3.common.util.UnstableApi
import androidx.media3.common.Format
import androidx.media3.exoplayer.audio.AudioSink
import androidx.media3.exoplayer.audio.ForwardingAudioSink
import java.nio.ByteBuffer

/** Decoder timestamps, rather than wall clock or UI position, anchor the vocal mix. */
@UnstableApi
internal class VocalTimelineAudioSink(sink: AudioSink, private val processor: VocalMixAudioProcessor) : ForwardingAudioSink(sink) {
    private var offsetUs = 0L
    private var currentBuffer: ByteBuffer? = null
    private var initialPosition = 0
    private var trimStartFrames = 0
    private var frameBytes = 4
    private var sampleRate = 44100

    override fun configure(inputFormat: Format, specifiedBufferSize: Int, outputChannels: IntArray?) {
        trimStartFrames = inputFormat.encoderDelay
        frameBytes = inputFormat.channelCount.coerceAtLeast(1) * 2
        sampleRate = inputFormat.sampleRate.coerceAtLeast(1)
        super.configure(inputFormat, specifiedBufferSize, outputChannels)
    }

    override fun setOutputStreamOffsetUs(outputStreamOffsetUs: Long) {
        if (offsetUs != outputStreamOffsetUs) processor.resetMediaTime()
        offsetUs = outputStreamOffsetUs
        super.setOutputStreamOffsetUs(outputStreamOffsetUs)
    }

    override fun handleBuffer(buffer: ByteBuffer, presentationTimeUs: Long, encodedAccessUnitCount: Int): Boolean {
        if (currentBuffer !== buffer) {
            currentBuffer = buffer
            initialPosition = buffer.position()
        }
        // The sink can retry a partly consumed buffer. Include its consumed PCM frames.
        val skipped = minOf(trimStartFrames, (buffer.limit() - initialPosition) / frameBytes)
        processor.setInputTimestamp(presentationTimeUs - offsetUs + skipped * 1_000_000L / sampleRate +
            processor.inputDurationUs(buffer.position() - initialPosition))
        val consumed = super.handleBuffer(buffer, presentationTimeUs, encodedAccessUnitCount)
        if (consumed) {
            trimStartFrames = (trimStartFrames - (buffer.limit() - initialPosition) / frameBytes).coerceAtLeast(0)
            currentBuffer = null
        }
        return consumed
    }

    override fun flush() { currentBuffer = null; super.flush() }
    override fun reset() { currentBuffer = null; super.reset() }
    override fun handleDiscontinuity() { processor.resetMediaTime(); super.handleDiscontinuity() }
}
