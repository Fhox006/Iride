/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.playback.audio

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.util.UnstableApi
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.floor

/**
 * The playback engine's variable-speed read head — a turntable platter sitting between the decoder
 * and the speaker.
 *
 * There is exactly one audio stream and one read head. Every decoded frame lands in [history]
 * (indexed by absolute frame since the last flush) and the head reads back out of it at
 * [velocity]. Normal playback is not a special case: it is the head running at exactly 1.0, where
 * `pos` advances by exactly one frame per output frame, the interpolation fraction is exactly zero
 * and the output is bit-identical to the input. Scratching only changes the number [setVelocity]
 * feeds in. Nothing is layered on top of playback, nothing is synthesized, nothing gets resynced
 * afterwards — 0.5 really is the song at half speed, -1.0 really is the song backwards, and on
 * release the head simply keeps going from wherever it ended up. No seek, ever.
 *
 * ## Why the decoder follows the head instead of the other way round
 *
 * The trick that makes the single-stream illusion hold is that we choose how many output frames to
 * emit per input buffer. The audio sink drains output in real time, so the output:input ratio *is*
 * the decoder's speed:
 *
 * - head at 1x  -> emit one frame per input frame; decoder runs at 1x. Passthrough.
 * - head at 3x  -> emit a third; the sink drains it in a third of the time and asks for input three
 *   times as fast, so the decoder sprints and the head always has fresh audio ahead of it. This is
 *   what a forward scratch needs, and it is why forward no longer collides with a player that
 *   "keeps advancing on its own" — there is only one thing advancing.
 * - head at 0 or in reverse -> emit [MaxExpansion] frames per input frame; the decoder crawls at
 *   1/[MaxExpansion] speed instead of racing forward and trampling the history the head is
 *   currently scrubbing over.
 *
 * ## Position
 *
 * ExoPlayer's clock counts frames handed to the speaker, so after a gesture that netted, say, two
 * seconds backwards it is off by exactly how far the head moved differently from wall clock. That
 * running difference is published as [driftMs] and the UI adds it to `player.currentPosition`; the
 * on-screen cursor therefore moves with the disc, forwards or backwards, live, and nothing needs to
 * be "confirmed" when the finger lifts.
 *
 * ponytail: the drift is a display correction, not a correction of the engine's own clock, so a
 * scratch that nets several seconds shifts where ExoPlayer thinks the track ends by that much.
 * Fixing that for real means a seek, which is exactly the jump this design exists to remove.
 */
@UnstableApi
@Suppress("DEPRECATION")
class ScratchAudioProcessor : AudioProcessor {

    companion object {
        /** Fallback history window when no size has been requested yet. */
        const val DefaultHistorySeconds = 60

        /** Hard ceiling for "whole track" mode; ~10 min stereo 44.1k is already ~100 MB. */
        const val MaxHistorySeconds = 600
        const val MinHistorySeconds = 5

        /**
         * Most output frames we will emit per input frame, i.e. how far the decoder is throttled
         * when the head is held or reversed. Higher keeps the decoder further out of the way but
         * buffers more audio ahead of the speaker, which shows up as lag between finger and sound.
         */
        private const val MaxExpansion = 4.0
        private const val MinRatio = 1.0 / MaxExpansion

        /** Ceiling on the head speed a gesture can ask for. */
        private const val MaxVelocity = 16.0

        /** One-pole time constant for [velocity] chasing [targetVelocity] — platter inertia. */
        private const val VelocityTimeConstantSeconds = 0.02

        /** Below this speed the head fades out: a stopped platter is silent, not a DC offset. */
        private const val SilenceVelocity = 0.04

        /**
         * How far the head sits behind the newest decoded frame in steady state. Pure headroom for
         * a forward scratch to accelerate into before the decoder has caught up; costs the same
         * amount of one-off delay when playback starts or after a seek.
         */
        private const val HeadLagSeconds = 0.04

        /** Frames drained per call once the decoder has hit end of stream. */
        private const val DrainChunkSeconds = 0.25

        private val EMPTY_BUFFER: ByteBuffer = ByteBuffer.allocateDirect(0).order(ByteOrder.nativeOrder())
    }

    private var sampleRate = 0
    private var channelCount = 0

    /** Ring of decoded PCM, indexed by absolute frame since the last [flush]. */
    private var history = ShortArray(0)
    private var historyFrames = 0L

    /** Absolute frame index of the next input frame to append. */
    private var writeFrame = 0L

    /** The read head, in the same absolute frame space as [writeFrame]. */
    private var pos = 0.0

    private var velocity = 1.0
    private var velocityAlpha = 0.0

    /** Fractional carry so output frame counts stay exact at 1x instead of rounding upward. */
    private var outputCarry = 0.0
    private var needsHeadLag = true

    @Volatile
    private var targetVelocity = 1.0

    @Volatile
    private var requestedHistorySeconds = DefaultHistorySeconds

    /** Pre-allocated off the audio thread by [requestHistorySeconds]; swapped in at [flush]. */
    @Volatile
    private var pendingHistory: ShortArray? = null

    /**
     * How far the head has moved differently from the frames handed to the speaker, in ms. Add it
     * to `player.currentPosition` to get the position actually being heard.
     */
    @Volatile
    var driftMs = 0L
        private set

    @Volatile
    private var driftFrames = 0.0

    /**
     * Clears the accumulated position correction. A gapless track change does not flush the sink,
     * so without this a scratch in one song would keep shifting the next song's cursor.
     */
    fun resetDrift() {
        driftFrames = 0.0
        driftMs = 0L
    }

    private var buffer: ByteBuffer = EMPTY_BUFFER
    private var outputBuffer: ByteBuffer = EMPTY_BUFFER
    private var inputEnded = false

    /**
     * Sets the platter speed: 1.0 is normal playback, 0.5 half speed, 0 stopped, negative plays the
     * track backwards. The head eases towards it over [VelocityTimeConstantSeconds] so however hard
     * a gesture yanks, the speed itself never jumps — which is the whole reason there are no clicks
     * at a reversal.
     */
    fun setVelocity(velocity: Double) {
        targetVelocity = velocity.coerceIn(-MaxVelocity, MaxVelocity)
    }

    /**
     * Resizes the scrubbable window. Allocates here, so call it off the audio thread; the new
     * buffer is swapped in at the next [flush] (track change or seek) to avoid tearing a stream in
     * progress.
     */
    fun requestHistorySeconds(seconds: Int) {
        val wanted = seconds.coerceIn(MinHistorySeconds, MaxHistorySeconds)
        requestedHistorySeconds = wanted
        val rate = sampleRate
        val channels = channelCount
        if (rate == 0 || channels == 0) return
        if (historyFrames == rate.toLong() * wanted) {
            pendingHistory = null
            return
        }
        pendingHistory = allocateHistory(rate, channels, wanted)
    }

    private fun allocateHistory(rate: Int, channels: Int, seconds: Int): ShortArray? =
        try {
            ShortArray((rate.toLong() * seconds * channels).toInt())
        } catch (error: OutOfMemoryError) {
            // "Whole track" on a long song can genuinely not fit; a short window still scratches.
            try {
                ShortArray(rate * MinHistorySeconds * channels)
            } catch (fallbackError: OutOfMemoryError) {
                null
            }
        }

    override fun configure(inputAudioFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
        if (inputAudioFormat.encoding != C.ENCODING_PCM_16BIT || inputAudioFormat.channelCount > 2) {
            throw AudioProcessor.UnhandledAudioFormatException(inputAudioFormat)
        }
        sampleRate = inputAudioFormat.sampleRate
        channelCount = inputAudioFormat.channelCount
        velocityAlpha = 1.0 - exp(-1.0 / (sampleRate * VelocityTimeConstantSeconds))
        history = pendingHistory
            ?: allocateHistory(sampleRate, channelCount, requestedHistorySeconds)
            ?: ShortArray(sampleRate * MinHistorySeconds * channelCount)
        pendingHistory = null
        historyFrames = (history.size / channelCount).toLong()
        resetStream()
        return inputAudioFormat
    }

    private fun resetStream() {
        writeFrame = 0L
        pos = 0.0
        velocity = 1.0
        targetVelocity = 1.0
        outputCarry = 0.0
        needsHeadLag = true
        driftFrames = 0.0
        driftMs = 0L
    }

    override fun isActive(): Boolean = sampleRate > 0 && historyFrames > 0

    override fun queueInput(inputBuffer: ByteBuffer) {
        if (!isActive) return
        val frameSize = 2 * channelCount
        val inputFrames = inputBuffer.remaining() / frameSize
        if (inputFrames == 0) {
            if (inputEnded) drainTail()
            return
        }

        appendToHistory(inputBuffer, inputFrames)
        inputBuffer.position(inputBuffer.limit())

        // The head's forward appetite sets the decoder's speed (see the class doc). Negative and
        // zero fall through to MinRatio, so reversing throttles the decoder rather than letting it
        // race ahead over the very history the head is reading.
        val ratio = velocity.coerceAtLeast(MinRatio)
        outputCarry += inputFrames / ratio
        if (needsHeadLag) {
            outputCarry -= sampleRate * HeadLagSeconds
            needsHeadLag = false
        }
        val outputFrames = floor(outputCarry).toInt()
        if (outputFrames <= 0) return
        outputCarry -= outputFrames
        render(outputFrames)
    }

    private fun appendToHistory(inputBuffer: ByteBuffer, inputFrames: Int) {
        inputBuffer.order(ByteOrder.LITTLE_ENDIAN)
        val base = inputBuffer.position()
        repeat(inputFrames) { frame ->
            val ringBase = ((writeFrame + frame) % historyFrames).toInt() * channelCount
            repeat(channelCount) { channel ->
                history[ringBase + channel] =
                    inputBuffer.getShort(base + (frame * channelCount + channel) * 2)
            }
        }
        writeFrame += inputFrames
    }

    private fun render(outputFrames: Int) {
        val out = replaceOutputBuffer(outputFrames * 2 * channelCount)
        // Fixed for the whole call: writeFrame only moves on the next queueInput.
        val minFrame = (writeFrame - historyFrames).coerceAtLeast(0L).toDouble()
        val maxFrame = (writeFrame - 1).coerceAtLeast(0L).toDouble()
        val maxFrameIndex = maxFrame.toLong()
        val target = targetVelocity
        var speed = velocity
        var head = pos
        val startHead = head
        repeat(outputFrames) {
            speed += (target - speed) * velocityAlpha
            if (abs(target - speed) < 1e-4) speed = target
            head = (head + speed).coerceIn(minFrame, maxFrame)
            writeHeadFrame(head, speed, maxFrameIndex, out)
        }
        velocity = speed
        pos = head
        // Everything the speaker consumed advanced wall-clock (and so ExoPlayer's clock) by
        // outputFrames, while the song really moved by however far the head went.
        driftFrames += (head - startHead) - outputFrames
        driftMs = (driftFrames / sampleRate * 1000.0).toLong()
        out.flip()
    }

    private fun writeHeadFrame(head: Double, speed: Double, maxFrameIndex: Long, out: ByteBuffer) {
        val gain = (abs(speed) / SilenceVelocity).coerceAtMost(1.0)
        val lowFrame = floor(head).toLong()
        val fraction = head - lowFrame
        val highFrame = (lowFrame + 1).coerceAtMost(maxFrameIndex)
        val lowRing = (lowFrame % historyFrames).toInt() * channelCount
        val highRing = (highFrame % historyFrames).toInt() * channelCount
        repeat(channelCount) { channel ->
            val a = history[lowRing + channel]
            val b = history[highRing + channel]
            out.putShort(((a + (b - a) * fraction) * gain).toInt().toShort())
        }
    }

    /**
     * The decoder can finish while the head is still behind it — that is exactly the state a
     * backwards scratch leaves. Hand the remainder out in chunks so the tail of the song is heard
     * instead of being cut off at end of stream.
     */
    private fun drainTail() {
        val remaining = (writeFrame - 1).coerceAtLeast(0L) - pos
        if (remaining < 1.0) return
        targetVelocity = 1.0
        val chunk = minOf(remaining, sampleRate * DrainChunkSeconds).toInt()
        if (chunk <= 0) return
        render(chunk)
    }

    private fun replaceOutputBuffer(size: Int): ByteBuffer {
        if (buffer.capacity() < size) {
            buffer = ByteBuffer.allocateDirect(size).order(ByteOrder.nativeOrder())
        } else {
            buffer.clear()
        }
        outputBuffer = buffer
        return buffer
    }

    override fun queueEndOfStream() {
        inputEnded = true
    }

    override fun getOutput(): ByteBuffer {
        val output = outputBuffer
        outputBuffer = EMPTY_BUFFER
        return output
    }

    override fun isEnded(): Boolean =
        inputEnded &&
            outputBuffer === EMPTY_BUFFER &&
            pos >= (writeFrame - 1).coerceAtLeast(0L).toDouble()

    override fun flush() {
        buffer = EMPTY_BUFFER
        outputBuffer = EMPTY_BUFFER
        inputEnded = false
        pendingHistory?.let { resized ->
            history = resized
            historyFrames = (resized.size / channelCount.coerceAtLeast(1)).toLong()
            pendingHistory = null
        }
        resetStream()
    }

    override fun reset() {
        flush()
        sampleRate = 0
        channelCount = 0
        history = ShortArray(0)
        historyFrames = 0L
    }
}
