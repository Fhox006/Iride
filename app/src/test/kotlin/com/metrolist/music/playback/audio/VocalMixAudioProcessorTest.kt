package com.metrolist.music.playback.audio

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import java.nio.ByteBuffer
import java.nio.ByteOrder
import org.junit.Assert.assertEquals
import org.junit.Test
import com.metrolist.music.playback.vocals.VocalTimeline

class VocalMixAudioProcessorTest {
    private fun process(level: Float, channels: Int, samples: ShortArray): ShortArray {
        val processor = VocalMixAudioProcessor { level }
        val outputFormat = processor.configure(AudioProcessor.AudioFormat(44100, channels, C.ENCODING_PCM_16BIT))
        if (channels == 4) assertEquals(2, outputFormat.channelCount)
        else assertEquals(channels, outputFormat.channelCount)
        processor.flush()
        val input = ByteBuffer.allocateDirect(samples.size * 2).order(ByteOrder.LITTLE_ENDIAN)
        samples.forEach { input.putShort(it) }
        input.flip()
        processor.queueInput(input)
        val output = processor.getOutput().order(ByteOrder.LITTLE_ENDIAN)
        return ShortArray(output.remaining() / 2) { output.short }
    }

    @Test
    fun `ordinary stereo remains bit exact even at zero voice level`() {
        val samples = shortArrayOf(-32768, 32767, 420, -1700)
        org.junit.Assert.assertArrayEquals(samples, process(0f, 2, samples))
    }

    @Test
    fun `ordinary mono and stereo keep their format for streaming karaoke`() {
        for (channels in listOf(1, 2)) {
            val processor = VocalMixAudioProcessor { 0f }
            assertEquals(AudioProcessor.AudioFormat(48000, channels, C.ENCODING_PCM_16BIT),
                processor.configure(AudioProcessor.AudioFormat(48000, channels, C.ENCODING_PCM_16BIT)))
            processor.flush()
            org.junit.Assert.assertTrue(processor.isActive)
        }
    }

    @Test
    fun `empty buffers are safe before the first sample and while draining`() {
        for (channels in listOf(2, 4)) {
            val processor = VocalMixAudioProcessor { 0f }
            processor.configure(AudioProcessor.AudioFormat(44100, channels, C.ENCODING_PCM_16BIT))
            processor.flush()
            processor.queueInput(AudioProcessor.EMPTY_BUFFER)
            org.junit.Assert.assertFalse(processor.getOutput().hasRemaining())
            processor.queueInput(AudioProcessor.EMPTY_BUFFER)
            processor.queueEndOfStream()
            org.junit.Assert.assertTrue(processor.isEnded)
        }
    }

    @Test
    fun `full voice reproduces original channels exactly`() {
        org.junit.Assert.assertArrayEquals(shortArrayOf(12000, -8000),
            process(1f, 4, shortArrayOf(12000, -8000, 2000, -4000)))
    }

    @Test
    fun `zero voice leaves accompaniment channels exactly`() {
        org.junit.Assert.assertArrayEquals(shortArrayOf(2000, -4000),
            process(0f, 4, shortArrayOf(12000, -8000, 2000, -4000)))
    }

    @Test
    fun `half voice blends original with accompaniment`() {
        org.junit.Assert.assertArrayEquals(shortArrayOf(7000, -6000),
            process(0.5f, 4, shortArrayOf(12000, -8000, 2000, -4000)))
    }

    @Test
    fun `voice changes are smoothed without a step in the waveform`() {
        var level = 1f
        val processor = VocalMixAudioProcessor { level }
        processor.configure(AudioProcessor.AudioFormat(44100, 4, C.ENCODING_PCM_16BIT))
        processor.flush()
        level = 0f
        val input = ByteBuffer.allocateDirect(11025 * 8).order(ByteOrder.LITTLE_ENDIAN)
        repeat(11025) { input.putShort(30000); input.putShort(30000); input.putShort(0); input.putShort(0) }
        input.flip()
        processor.queueInput(input)
        val output = processor.getOutput().order(ByteOrder.LITTLE_ENDIAN)
        var previous = 30000
        repeat(11025) {
            val left = output.short.toInt()
            assertEquals(left, output.short.toInt())
            org.junit.Assert.assertTrue(previous - left in 0..35)
            previous = left
        }
        org.junit.Assert.assertTrue(previous <= 1)
    }
    private fun preparedWindow(start: Long, frames: Int, base: Short = 8192, original: Short = 12000) =
        VocalTimeline.Chunk(start, Array(2) { FloatArray(frames) { base / 32768f } },
            Array(2) { FloatArray(frames) { original / 32768f } })

    private fun streamingProcessor(rate: Int, timeline: VocalTimeline, level: Float = 0f): VocalMixAudioProcessor =
        VocalMixAudioProcessor { level }.apply {
            this.timeline = { timeline }
            configure(AudioProcessor.AudioFormat(rate, 2, C.ENCODING_PCM_16BIT))
            flush()
        }

    private fun render(processor: VocalMixAudioProcessor, frames: Int, sample: Short = 12000): ShortArray {
        val input = ByteBuffer.allocateDirect(frames * 4).order(ByteOrder.LITTLE_ENDIAN)
        repeat(frames * 2) { input.putShort(sample) }
        input.flip()
        processor.queueInput(input)
        val output = processor.getOutput().order(ByteOrder.LITTLE_ENDIAN)
        return ShortArray(output.remaining() / 2) { output.short }
    }

    @Test
    fun `the first half second of ready audio is used without waiting for the entire track`() {
        val timeline = VocalTimeline()
        val processor = streamingProcessor(44100, timeline)
        processor.setInputTimestamp(100_000)
        org.junit.Assert.assertTrue(render(processor, 1000).all { it == 12000.toShort() })
        timeline.publish(preparedWindow(0, 44100 / 2), 0)
        val ready = render(processor, 2000)
        org.junit.Assert.assertTrue(ready.takeLast(100).all { it == 8192.toShort() })
        org.junit.Assert.assertFalse(timeline.covers(44100))
    }

    @Test
    fun `mid track activation uses media timestamps and never plays another position`() {
        val start = 44100L * 90
        val timeline = VocalTimeline()
        timeline.publish(preparedWindow(start, 44100), start)
        val processor = streamingProcessor(44100, timeline)
        processor.setInputTimestamp(0)
        org.junit.Assert.assertTrue(render(processor, 1000).all { it == 12000.toShort() })
        processor.flush()
        processor.setInputTimestamp(90_100_000)
        render(processor, 12000)
        org.junit.Assert.assertTrue(render(processor, 1000).all { it == 8192.toShort() })
    }

    @Test
    fun `streaming timebase stays continuous across input buffers and 48 kHz playback`() {
        val timeline = VocalTimeline()
        timeline.publish(preparedWindow(0, 44100 * 3), 0)
        val processor = streamingProcessor(48000, timeline)
        processor.setInputTimestamp(100_000)
        val onset = render(processor, 48000)
        org.junit.Assert.assertTrue(onset.takeLast(1000).all { it == 8192.toShort() })
        // A retried or rounded input timestamp must not restart the sample cursor.
        processor.setInputTimestamp(100_000)
        org.junit.Assert.assertTrue(render(processor, 48000).all { it == 8192.toShort() })
        val tail = render(processor, 60000)
        org.junit.Assert.assertEquals(12000.toShort(), tail.last())
    }

    @Test
    fun `seek flush discards old timebase and missing windows preserve original audio`() {
        val start = 44100L * 30
        val timeline = VocalTimeline()
        timeline.publish(preparedWindow(start, 44100), start)
        val processor = streamingProcessor(44100, timeline)
        processor.setInputTimestamp(30_100_000)
        render(processor, 12000)
        org.junit.Assert.assertEquals(8192.toShort(), render(processor, 1000).first())
        processor.flush()
        processor.setInputTimestamp(60_000_000)
        org.junit.Assert.assertTrue(render(processor, 1000).all { it == 12000.toShort() })
    }

    @Test
    fun `handoff is bounded to twenty milliseconds with no silent interval or gain overshoot`() {
        val timeline = VocalTimeline()
        timeline.publish(preparedWindow(0, 44100 * 2, base = -8000, original = 30000), 0)
        val processor = streamingProcessor(44100, timeline)
        processor.setInputTimestamp(100_000)
        val samples = render(processor, 14000, sample = 30000)
        val left = samples.filterIndexed { index, _ -> index % 2 == 0 }.map { it.toInt() }
        org.junit.Assert.assertTrue(left.all { it in -8000..30000 })
        left.zipWithNext().forEach { (before, after) -> org.junit.Assert.assertTrue(before - after in 0..44) }
        org.junit.Assert.assertTrue(left.drop(VocalTimeline.FADE_FRAMES).all { it == -8000 })
        org.junit.Assert.assertTrue(left.count { it == 0 } <= 1)
    }

    @Test
    fun `publishing ready karaoke never ducks or doubles unchanged accompaniment`() {
        val timeline = VocalTimeline()
        val processor = streamingProcessor(44100, timeline)
        processor.setInputTimestamp(100_000)
        org.junit.Assert.assertTrue(render(processor, 1000, sample = 8192).all { it == 8192.toShort() })
        timeline.publish(preparedWindow(0, 44100, base = 8192), 0)
        // Split the transition across buffers, as happens when inference finishes during playback.
        repeat(10) {
            org.junit.Assert.assertTrue(render(processor, 100, sample = 8192).all { it == 8192.toShort() })
        }
        org.junit.Assert.assertTrue(render(processor, 1000, sample = -30000).all { it == 8192.toShort() })
    }

    @Test
    fun `one hundred percent restores the complete original after a bounded handoff`() {
        var level = 0f
        val timeline = VocalTimeline()
        timeline.publish(preparedWindow(0, 44100 * 3, base = 2000, original = 10000), 0)
        val processor = VocalMixAudioProcessor { level }.apply {
            this.timeline = { timeline }
            configure(AudioProcessor.AudioFormat(48000, 2, C.ENCODING_PCM_16BIT))
            flush()
        }
        processor.setInputTimestamp(100_000)
        render(processor, 2000, sample = 10000)
        org.junit.Assert.assertTrue(render(processor, 100).all { it == 2000.toShort() })
        level = 1f
        val restored = render(processor, 2000, sample = 10000)
        org.junit.Assert.assertTrue(restored.drop(960 * 2).all { it == 10000.toShort() })
        org.junit.Assert.assertTrue(render(processor, 1000, sample = -12000).all { it == (-12000).toShort() })
    }

    @Test
    fun `full karaoke discards live audio completely after the fade`() {
        val timeline = VocalTimeline()
        timeline.publish(preparedWindow(0, 44100 * 3, base = 2000, original = 10000), 0)
        val processor = streamingProcessor(44100, timeline)
        processor.setInputTimestamp(100_000)
        render(processor, 12000, sample = 30000)
        val positiveLive = render(processor, 1000, sample = 30000)
        val negativeLive = render(processor, 1000, sample = -30000)
        org.junit.Assert.assertTrue(positiveLive.all { it == 2000.toShort() })
        org.junit.Assert.assertArrayEquals(positiveLive, negativeLive)
    }

    @Test
    fun `intermediate voice level uses only stems from the same prepared decoder`() {
        val timeline = VocalTimeline()
        timeline.publish(preparedWindow(0, 44100 * 3, base = 2000, original = 10000), 0)
        val processor = streamingProcessor(44100, timeline, level = 0.5f)
        processor.setInputTimestamp(100_000)
        render(processor, 12000, sample = 30000)
        org.junit.Assert.assertTrue(render(processor, 1000, sample = -30000).all { it == 6000.toShort() })
    }

    @Test
    fun `adjoining windows do not fade or bring live audio back at a chunk boundary`() {
        val timeline = VocalTimeline()
        timeline.publish(preparedWindow(0, 44100), 0)
        timeline.publish(preparedWindow(44100, 44100), 0)
        val processor = streamingProcessor(44100, timeline)
        processor.setInputTimestamp(500_000)
        render(processor, 12000)
        org.junit.Assert.assertTrue(render(processor, 11025).all { it == 8192.toShort() })
    }

    @Test
    fun `prepared coverage gap returns smoothly to live audio before the gap`() {
        val timeline = VocalTimeline()
        timeline.publish(preparedWindow(0, 44100, base = -8000), 0)
        val processor = streamingProcessor(44100, timeline)
        processor.setInputTimestamp(100_000)
        render(processor, 12000, sample = 30000)
        val samples = render(processor, 35000, sample = 30000).filterIndexed { index, _ -> index % 2 == 0 }
        org.junit.Assert.assertTrue(samples.all { it.toInt() in -8000..30000 })
        samples.zipWithNext().forEach { (before, after) ->
            org.junit.Assert.assertTrue(after.toInt() - before.toInt() in 0..44)
        }
        org.junit.Assert.assertTrue(samples.count { it == 0.toShort() } <= 1)
        org.junit.Assert.assertEquals(30000.toShort(), samples.last())
    }
}
