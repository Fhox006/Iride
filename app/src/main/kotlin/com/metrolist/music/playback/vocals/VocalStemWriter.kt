package com.metrolist.music.playback.vocals

import java.io.File
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlin.math.roundToInt

/** Overlapping inference windows avoid joins while keeping the original samples and duration. */
internal object VocalStemWriter {
    suspend fun write(
        pcm: File,
        destination: File,
        separate: (FloatArray, FloatArray) -> Array<FloatArray>,
        progress: (Float) -> Unit,
    ) {
        val totalFrames = pcm.length() / 4
        require(totalFrames > 0 && pcm.length() % 4 == 0L)
        val chunkFrames = 44100 * 11
        val overlap = 44100
        val step = chunkFrames - overlap
        RandomAccessFile(pcm, "r").use { input ->
            RandomAccessFile(destination, "rw").use { output ->
                output.setLength(0)
                output.write(wavHeader(totalFrames))
                var start = 0L
                var previous: Array<FloatArray>? = null
                while (start < totalFrames) {
                    currentCoroutineContext().ensureActive()
                    val frames = minOf(chunkFrames.toLong(), totalFrames - start).toInt()
                    val raw = ByteArray(frames * 4)
                    input.seek(start * 4)
                    input.readFully(raw)
                    val decoded = ByteBuffer.wrap(raw).order(ByteOrder.LITTLE_ENDIAN)
                    val original = Array(2) { FloatArray(frames) }
                    repeat(frames) { frame ->
                        original[0][frame] = decoded.short / 32768f
                        original[1][frame] = decoded.short / 32768f
                    }
                    val base = separate(original[0], original[1])
                    require(base.size == 2 && base.all { it.size == frames })
                    currentCoroutineContext().ensureActive()
                    val prior = previous
                    if (prior != null) {
                        repeat(minOf(overlap, frames)) { frame ->
                            val weight = frame.toFloat() / overlap
                            repeat(2) { channel ->
                                base[channel][frame] = prior[channel][frame] * (1f - weight) + base[channel][frame] * weight
                            }
                        }
                    }
                    val last = start + frames >= totalFrames
                    val emitted = if (last) frames else step
                    val interleaved = ByteBuffer.allocate(emitted * 8).order(ByteOrder.LITTLE_ENDIAN)
                    repeat(emitted) { frame ->
                        repeat(2) { channel -> interleaved.putShort(sample(original[channel][frame])) }
                        repeat(2) { channel -> interleaved.putShort(sample(base[channel][frame])) }
                    }
                    output.write(interleaved.array())
                    previous = if (last) null else Array(2) { base[it].copyOfRange(step, frames) }
                    progress(((start + emitted).toFloat() / totalFrames).coerceIn(0f, 1f))
                    if (last) break
                    start += step
                }
                check(output.length() == 44 + totalFrames * 8)
            }
        }
    }

    private fun sample(value: Float): Short {
        check(value.isFinite()) { "Non-finite separated audio" }
        return (value.coerceIn(-1f, 1f) * 32768f).roundToInt().coerceIn(-32768, 32767).toShort()
    }

    private fun wavHeader(frames: Long): ByteArray = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN).apply {
        put("RIFF".toByteArray())
        putInt((36 + frames * 8).toInt())
        put("WAVEfmt ".toByteArray())
        putInt(16)
        putShort(1)
        putShort(4)
        putInt(44100)
        putInt(44100 * 8)
        putShort(8)
        putShort(16)
        put("data".toByteArray())
        putInt((frames * 8).toInt())
    }.array()
}
