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
import kotlin.math.exp
import kotlin.math.floor

/**
 * Turns the album-screen vinyl-scratch gesture into a real turntable scratch: it mirrors every
 * decoded PCM frame into a rolling ring buffer, and while [scratching] is true it stops passing
 * that audio straight through and instead re-reads the ring buffer at a scrub position driven by
 * [beginScratch]/[setVelocity]/[endScratch] (called from the drag handler in AlbumScreen). Linear
 * interpolation between ring samples gives the pitch-follows-speed, silence-when-held character a
 * physical needle has, for free, out of real recently-played song audio — no synthesized sample.
 *
 * The scrub is velocity-driven, not position-driven: the caller reports the gesture's
 * *instantaneous speed* (1.0 = forward at normal speed, 0 = held, negative = reverse) via
 * [setVelocity], and [currentVelocity] chases that target with a one-pole low-pass filter run at
 * sample rate ([VelocityTimeConstantSeconds]). Read position is then the running integral of the
 * filtered velocity. Feeding raw position deltas straight into playback (the previous design) let
 * a fast flick or reversal teleport the read head, which is audible as a click/glitch; smoothing
 * velocity instead means the read head's speed always changes continuously, however hard the
 * input yanks it, the same way a turntable platter has inertia and can't teleport speed.
 *
 * ponytail: ring buffer only covers the last [RingSeconds] of audio, so a scratch can't reach
 * further back than that — plenty for a finger gesture, raise RingSeconds if that ever isn't true.
 */
@UnstableApi
@Suppress("DEPRECATION")
class ScratchAudioProcessor : AudioProcessor {

    private companion object {
        // Real decode never pauses while scratching (it can't — that's what keeps queueInput
        // being called at all, which is what keeps the scratch audible), so writeFrame and
        // therefore minFrame keep climbing under a held/slow scratch. Past RingSeconds of
        // holding roughly the same spot, minFrame catches up to pos and pins it — audible as
        // clicking after playing with the gesture for a while. 30s covers any realistic single
        // scratch session; ~5MB of ShortArray for the headroom is a cheap trade.
        const val RingSeconds = 30

        /** One-pole low-pass time constant for [currentVelocity] chasing [targetVelocity]. */
        const val VelocityTimeConstantSeconds = 0.025
        val EMPTY_BUFFER: ByteBuffer = ByteBuffer.allocateDirect(0).order(ByteOrder.nativeOrder())
    }

    private var sampleRate = 0
    private var channelCount = 0
    private var ring = ShortArray(0)
    private var ringCapacityFrames = 0L
    private var writeFrame = 0L

    /** Per-output-frame smoothing factor for the velocity filter; set from sample rate. */
    private var velocityAlpha = 0.0

    private var outputBuffer: ByteBuffer = EMPTY_BUFFER
    private var inputEnded = false

    @Volatile
    var scratching = false
        private set

    // Audio-thread-owned; only touched off-thread once, at beginScratch, before scratching flips
    // true — safe because that write happens-before the scratching=true volatile write, which the
    // audio thread must observe before it starts reading either field.
    private var outputPos = 0.0
    private var currentVelocity = 1.0

    /** UI-thread-written, audio-thread-read; single scalar assignment needs no lock. */
    @Volatile
    private var targetVelocity = 1.0

    /**
     * [audiblePositionMs] is the player's reported currentPosition, not [writeFrame] — the decoder
     * runs ahead of what's actually reaching the speaker (sink buffering), so starting the scrub
     * from writeFrame left a gap of already-queued, un-scratched audio still playing out under the
     * user's finger. That gap was the "teleports instead of gliding" complaint: motion looked
     * responsive but the audible pitch-bend lagged, then jumped to catch up.
     */
    fun beginScratch(audiblePositionMs: Long) {
        if (sampleRate == 0) return
        val minFrame = (writeFrame - ringCapacityFrames).coerceAtLeast(0L)
        val audibleFrame = (audiblePositionMs * sampleRate / 1000.0).coerceIn(minFrame.toDouble(), writeFrame.toDouble())
        outputPos = audibleFrame
        // Start "still spinning at normal speed" so entering the scratch is itself a smooth
        // deceleration/acceleration into the gesture's speed, not a jump.
        currentVelocity = 1.0
        targetVelocity = 1.0
        scratching = true
    }

    /** Report the gesture's current speed (1.0 = forward normal speed, 0 = held, negative = reverse). */
    fun setVelocity(velocity: Double) {
        if (!scratching) return
        targetVelocity = velocity
    }

    /** Ends the scratch, returning the net offset (ms) the scrub left behind vs. live position. */
    fun endScratch(): Long {
        scratching = false
        if (sampleRate == 0) return 0L
        return ((outputPos - writeFrame) / sampleRate * 1000).toLong()
    }

    override fun configure(inputAudioFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
        if (inputAudioFormat.encoding != C.ENCODING_PCM_16BIT || inputAudioFormat.channelCount > 2) {
            throw AudioProcessor.UnhandledAudioFormatException(inputAudioFormat)
        }
        sampleRate = inputAudioFormat.sampleRate
        channelCount = inputAudioFormat.channelCount
        ringCapacityFrames = sampleRate.toLong() * RingSeconds
        ring = ShortArray((ringCapacityFrames * channelCount).toInt())
        velocityAlpha = 1.0 - exp(-1.0 / (sampleRate * VelocityTimeConstantSeconds))
        writeFrame = 0L
        scratching = false
        return inputAudioFormat
    }

    override fun isActive(): Boolean = sampleRate > 0

    override fun queueInput(inputBuffer: ByteBuffer) {
        val remaining = inputBuffer.remaining()
        if (remaining == 0 || sampleRate == 0) return
        inputBuffer.order(ByteOrder.LITTLE_ENDIAN)

        val frameCount = remaining / (2 * channelCount)
        val basePosition = inputBuffer.position()

        repeat(frameCount) { frameIndex ->
            val ringFrame = ((writeFrame + frameIndex) % ringCapacityFrames).toInt()
            val ringBase = ringFrame * channelCount
            repeat(channelCount) { ch ->
                ring[ringBase + ch] = inputBuffer.getShort(basePosition + (frameIndex * channelCount + ch) * 2)
            }
        }
        writeFrame += frameCount

        val out = replaceOutputBuffer(remaining)
        if (scratching) {
            val target = targetVelocity
            var vel = currentVelocity
            var pos = outputPos
            // Ring bounds are fixed for this whole call (writeFrame only moves on the next
            // queueInput). Clamping pos here, not just at read time in writeInterpolatedFrame,
            // matters: without it pos free-runs past the wall while pinned (e.g. scratching
            // forward faster than the decoder can hand us "future" audio), and by the time
            // velocity turns back around it has to unwind however far it drifted before sound
            // resumes — audible as a silent stall. Clamping in place means it's always pinned
            // exactly at the wall, so playback responds the instant the wall moves or velocity
            // reverses.
            val minFrame = (writeFrame - ringCapacityFrames).coerceAtLeast(0L).toDouble()
            val maxFrame = (writeFrame - 1).coerceAtLeast(0L).toDouble()
            repeat(frameCount) {
                vel += (target - vel) * velocityAlpha
                pos = (pos + vel).coerceIn(minFrame, maxFrame)
                writeInterpolatedFrame(pos, maxFrame, out)
            }
            currentVelocity = vel
            outputPos = pos
        } else {
            currentVelocity = 1.0
            outputPos = writeFrame.toDouble()
            repeat(frameCount) { frameIndex ->
                repeat(channelCount) { ch ->
                    out.putShort(inputBuffer.getShort(basePosition + (frameIndex * channelCount + ch) * 2))
                }
            }
        }
        inputBuffer.position(inputBuffer.limit())
        out.flip()
    }

    /** [pos] must already be clamped to this call's valid ring range; [maxFrame] as a Long for the interpolation neighbor cap. */
    private fun writeInterpolatedFrame(pos: Double, maxFrame: Double, out: ByteBuffer) {
        val maxFrameLong = maxFrame.toLong()
        val lowFrame = floor(pos).toLong()
        val frac = pos - lowFrame
        val highFrame = (lowFrame + 1).coerceAtMost(maxFrameLong)
        val lowRing = (lowFrame % ringCapacityFrames).toInt() * channelCount
        val highRing = (highFrame % ringCapacityFrames).toInt() * channelCount
        repeat(channelCount) { ch ->
            val a = ring[lowRing + ch]
            val b = ring[highRing + ch]
            out.putShort((a + (b - a) * frac).toInt().toShort())
        }
    }

    private fun replaceOutputBuffer(size: Int): ByteBuffer {
        if (outputBuffer.capacity() < size) {
            outputBuffer = ByteBuffer.allocateDirect(size).order(ByteOrder.nativeOrder())
        } else {
            outputBuffer.clear()
        }
        return outputBuffer
    }

    override fun queueEndOfStream() {
        inputEnded = true
    }

    override fun getOutput(): ByteBuffer {
        val output = outputBuffer
        outputBuffer = EMPTY_BUFFER
        return output
    }

    override fun isEnded(): Boolean = inputEnded && outputBuffer === EMPTY_BUFFER

    override fun flush() {
        outputBuffer = EMPTY_BUFFER
        inputEnded = false
        scratching = false
    }

    override fun reset() {
        flush()
        sampleRate = 0
        channelCount = 0
        ring = ShortArray(0)
        ringCapacityFrames = 0L
        writeFrame = 0L
    }

}
