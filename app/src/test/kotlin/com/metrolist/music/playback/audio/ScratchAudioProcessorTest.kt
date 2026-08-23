package com.metrolist.music.playback.audio

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * The read head is the playback engine now, so if its arithmetic is wrong every song plays wrong,
 * not just a scratch. These pin the three properties the design rests on: 1x is untouched audio,
 * the output:input ratio is what steers the decoder, and reverse really replays the recorded frames
 * backwards.
 */
class ScratchAudioProcessorTest {

    private val sampleRate = 48000
    private val channels = 1
    private val framesPerBuffer = 1024

    private fun newProcessor(): ScratchAudioProcessor =
        ScratchAudioProcessor().apply {
            configure(AudioProcessor.AudioFormat(sampleRate, channels, C.ENCODING_PCM_16BIT))
            flush()
        }

    /** A ramp, so every frame is identifiable by its value. */
    private fun inputBuffer(startFrame: Int): ByteBuffer =
        ByteBuffer.allocateDirect(framesPerBuffer * 2 * channels)
            .order(ByteOrder.nativeOrder())
            .apply {
                repeat(framesPerBuffer) { putShort(((startFrame + it) % 30000).toShort()) }
                flip()
            }

    private fun ScratchAudioProcessor.pump(buffers: Int, fromFrame: Int = 0): List<Short> {
        val out = mutableListOf<Short>()
        repeat(buffers) { i ->
            queueInput(inputBuffer(fromFrame + i * framesPerBuffer))
            val produced = getOutput()
            while (produced.hasRemaining()) out += produced.getShort()
        }
        return out
    }

    @Test
    fun `normal playback is a bit-exact passthrough`() {
        val processor = newProcessor()
        val output = processor.pump(buffers = 20)
        assertTrue("expected audio out", output.size > framesPerBuffer * 15)

        val tail = output.takeLast(framesPerBuffer * 10)
        val first = tail.first().toInt()
        tail.forEachIndexed { i, sample ->
            assertEquals("frame $i altered", ((first + i) % 30000).toShort(), sample)
        }
    }

    @Test
    fun `output length tracks head speed so the decoder follows`() {
        val normal = newProcessor()
        normal.pump(buffers = 10)
        val normalSteady = normal.pump(buffers = 10).size
        assertEquals(framesPerBuffer * 10, normalSteady)

        val held = newProcessor()
        held.pump(buffers = 10)
        held.setVelocity(0.0)
        held.pump(buffers = 10)
        val heldSteady = held.pump(buffers = 5).size
        assertTrue("expected expansion, got $heldSteady", heldSteady > framesPerBuffer * 5)

        val fast = newProcessor()
        fast.pump(buffers = 10)
        fast.setVelocity(4.0)
        fast.pump(buffers = 10)
        val fastSteady = fast.pump(buffers = 5).size
        assertTrue("expected compression, got $fastSteady", fastSteady < framesPerBuffer * 5)
    }

    @Test
    fun `reverse replays recorded frames backwards and rewinds the reported position`() {
        val processor = newProcessor()
        processor.pump(buffers = 200)
        assertTrue("forward playback should not drift", Math.abs(processor.driftMs) < 100)

        processor.setVelocity(-1.0)
        val reversed = processor.pump(buffers = 10, fromFrame = 200 * framesPerBuffer)

        val settled = reversed.takeLast(2000).map { it.toInt() }
        var descending = 0
        settled.zipWithNext { a, b -> if (b < a) descending++ }
        assertTrue("expected a descending ramp, got $descending of ${settled.size - 1}",
            descending > (settled.size - 1) * 0.9)

        assertTrue("position should have moved backwards, drift=${processor.driftMs}",
            processor.driftMs < -200)
    }
}
