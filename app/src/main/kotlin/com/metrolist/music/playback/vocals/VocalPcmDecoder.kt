package com.metrolist.music.playback.vocals

import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import java.io.BufferedOutputStream
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlin.math.roundToInt

/** Streams decoded audio to disk, keeping memory independent of song duration. */
internal object VocalPcmDecoder {
    suspend fun decode(source: File, destination: File, progress: (Float) -> Unit) {
        val extractor = MediaExtractor()
        var codec: MediaCodec? = null
        try {
            extractor.setDataSource(source.absolutePath)
            val track = (0 until extractor.trackCount).firstOrNull {
                extractor.getTrackFormat(it).getString(MediaFormat.KEY_MIME)?.startsWith("audio/") == true
            } ?: error("No audio track")
            extractor.selectTrack(track)
            val format = extractor.getTrackFormat(track)
            val duration = if (format.containsKey(MediaFormat.KEY_DURATION)) {
                format.getLong(MediaFormat.KEY_DURATION)
            } else 0L
            check(duration <= 20 * 60 * 1_000_000L) { "Audio exceeds 20 minutes" }
            val decoder = MediaCodec.createDecoderByType(checkNotNull(format.getString(MediaFormat.KEY_MIME)))
            codec = decoder
            decoder.configure(format, null, null, 0)
            decoder.start()
            BufferedOutputStream(destination.outputStream(), 65536).use { output ->
                var resampler = StereoPcmResampler(format.getInteger(MediaFormat.KEY_SAMPLE_RATE))
                var channels = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
                var floatPcm = false
                var inputEnded = false
                var outputEnded = false
                val info = MediaCodec.BufferInfo()
                val bytes = ByteBuffer.allocate(65536).order(ByteOrder.LITTLE_ENDIAN)
                fun flushBytes() {
                    output.write(bytes.array(), 0, bytes.position())
                    bytes.clear()
                }
                fun emit(left: Float, right: Float) {
                    if (bytes.remaining() < 4) flushBytes()
                    bytes.putShort(toShort(left))
                    bytes.putShort(toShort(right))
                }
                var lastOutputNanos = System.nanoTime()
                while (!outputEnded) {
                    currentCoroutineContext().ensureActive()
                    check(System.nanoTime() - lastOutputNanos < 30_000_000_000L) { "Audio decoder stalled" }
                    if (!inputEnded) {
                        val index = decoder.dequeueInputBuffer(10_000)
                        if (index >= 0) {
                            val input = checkNotNull(decoder.getInputBuffer(index))
                            val size = extractor.readSampleData(input, 0)
                            if (size < 0) {
                                decoder.queueInputBuffer(index, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                                inputEnded = true
                            } else {
                                decoder.queueInputBuffer(index, 0, size, extractor.sampleTime, 0)
                                extractor.advance()
                            }
                        }
                    }
                    when (val index = decoder.dequeueOutputBuffer(info, 10_000)) {
                        MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                            val decodedFormat = decoder.outputFormat
                            channels = decodedFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
                            check(channels in 1..2) { "Only mono and stereo audio are supported" }
                            val rate = decodedFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE)
                            check(resampler.inputFrames == 0L || rate == resampler.sampleRate)
                            if (resampler.inputFrames == 0L) resampler = StereoPcmResampler(rate)
                            floatPcm = decodedFormat.containsKey(MediaFormat.KEY_PCM_ENCODING) &&
                                decodedFormat.getInteger(MediaFormat.KEY_PCM_ENCODING) == 4
                        }
                        else -> if (index >= 0) {
                            lastOutputNanos = System.nanoTime()
                            try {
                                val pcm = checkNotNull(decoder.getOutputBuffer(index)).order(ByteOrder.LITTLE_ENDIAN)
                                pcm.position(info.offset)
                                pcm.limit(info.offset + info.size)
                                val frameBytes = channels * if (floatPcm) 4 else 2
                                while (pcm.remaining() >= frameBytes) {
                                    val left = if (floatPcm) pcm.float else pcm.short / 32768f
                                    val right = if (channels == 1) left else if (floatPcm) pcm.float else pcm.short / 32768f
                                    check(left.isFinite() && right.isFinite()) { "Invalid decoded audio" }
                                    resampler.accept(left, right, ::emit)
                                }
                                check(resampler.inputFrames <= resampler.sampleRate * 20 * 60L)
                                if (duration > 0) progress((info.presentationTimeUs.toFloat() / duration).coerceIn(0f, 1f))
                                outputEnded = info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0
                            } finally { decoder.releaseOutputBuffer(index, false) }
                        }
                    }
                }
                resampler.finish(::emit)
                flushBytes()
            }
        } finally {
            codec?.release()
            extractor.release()
        }
    }

    private fun toShort(sample: Float): Short =
        (sample.coerceIn(-1f, 1f) * 32768f).roundToInt().coerceIn(-32768, 32767).toShort()
}
