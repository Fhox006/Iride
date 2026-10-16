package com.metrolist.music.playback.vocals

import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.file.Files
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VocalStemWriterTest {
    @Test
    fun `window joins retain original samples and exact duration including a short tail`() = runBlocking {
        val input = Files.createTempFile("vocals-", ".pcm").toFile()
        val output = Files.createTempFile("vocals-", ".wav").toFile()
        try {
            val frames = 44100 * 22 + 17
            val pcm = ByteBuffer.allocate(frames * 4).order(ByteOrder.LITTLE_ENDIAN)
            repeat(frames) { frame ->
                pcm.putShort((frame % 30000).toShort())
                pcm.putShort((-(frame % 30000)).toShort())
            }
            input.writeBytes(pcm.array())
            var progress = 0f
            VocalStemWriter.write(input, output, { left, right ->
                arrayOf(FloatArray(left.size) { left[it] / 2 }, FloatArray(right.size) { right[it] / 2 })
            }) {
                assertTrue(it >= progress && it <= 1f)
                progress = it
            }
            assertEquals(1f, progress, 0f)
            assertEquals(44L + frames * 8, output.length())
            val wave = ByteBuffer.wrap(output.readBytes()).order(ByteOrder.LITTLE_ENDIAN)
            assertEquals(4, wave.getShort(22).toInt())
            assertEquals(44100, wave.getInt(24))
            assertEquals(frames * 8, wave.getInt(40))
            wave.position(44)
            repeat(frames) { frame ->
                val expected = frame % 30000
                assertEquals(expected, wave.short.toInt())
                assertEquals(-expected, wave.short.toInt())
                assertEquals(expected / 2f, wave.short.toFloat(), 1f)
                assertEquals(-expected / 2f, wave.short.toFloat(), 1f)
            }
        } finally {
            input.delete()
            output.delete()
        }
    }

    @Test
    fun `cancellation stops before writing the next window`() = runBlocking {
        val input = Files.createTempFile("vocals-", ".pcm").toFile()
        val output = Files.createTempFile("vocals-", ".wav").toFile()
        try {
            input.writeBytes(ByteArray(44100 * 22 * 4))
            var calls = 0
            try {
                VocalStemWriter.write(input, output, { left, right -> calls++; arrayOf(left, right) }) {
                    throw CancellationException("Cancelled by user")
                }
                throw AssertionError("Cancellation was swallowed")
            } catch (_: CancellationException) {
                assertEquals(1, calls)
                assertTrue(output.length() < 44 + input.length() * 2)
            }
        } finally {
            input.delete()
            output.delete()
        }
    }
}
