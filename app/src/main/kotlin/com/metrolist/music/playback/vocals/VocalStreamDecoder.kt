package com.metrolist.music.playback.vocals

import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import java.io.Closeable
import java.nio.ByteOrder
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlin.math.roundToLong

/** Seeks and decodes only the next inference window, retaining the source's media timestamps. */
internal class VocalStreamDecoder(private val source: VocalMediaDataSource, fallbackDurationMs: Long) : Closeable {
    private val extractor = MediaExtractor()
    private val format: MediaFormat
    val durationFrames: Long

    init {
        try {
            extractor.setDataSource(source)
            val track = (0 until extractor.trackCount).firstOrNull {
                extractor.getTrackFormat(it).getString(MediaFormat.KEY_MIME)?.startsWith("audio/") == true
            } ?: error("No audio track")
            extractor.selectTrack(track)
            format = extractor.getTrackFormat(track)
            val durationUs = if (format.containsKey(MediaFormat.KEY_DURATION)) {
                format.getLong(MediaFormat.KEY_DURATION).takeIf { it > 0 } ?: fallbackDurationMs * 1000
            } else fallbackDurationMs * 1000
            check(durationUs in 1..20 * 60 * 1_000_000L) { "Unsupported audio duration" }
            durationFrames = durationUs * VocalTimeline.SAMPLE_RATE / 1_000_000
        } catch (error: Throwable) {
            extractor.release()
            source.close()
            throw error
        }
    }

    suspend fun decode(startFrame: Long, requestedFrames: Int): Array<FloatArray> {
        val frames = minOf(requestedFrames.toLong(), durationFrames - startFrame).toInt()
        require(startFrame >= 0 && frames > 0)
        val samples = Array(2) { FloatArray(frames) }
        val startUs = startFrame * 1_000_000 / VocalTimeline.SAMPLE_RATE
        // Resetting codecs such as Opus needs preroll before the requested sample position.
        extractor.seekTo((startUs - 250_000).coerceAtLeast(0), MediaExtractor.SEEK_TO_PREVIOUS_SYNC)
        val decoder = MediaCodec.createDecoderByType(checkNotNull(format.getString(MediaFormat.KEY_MIME)))
        try {
            decoder.configure(format, null, null, 0)
            decoder.start()
            var rate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE)
            var channels = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
            var floatPcm = false
            var resampler = StereoPcmResampler(rate)
            var anchor = Long.MIN_VALUE
            var outputFrame = 0L
            var inputEnded = false
            var ended = false
            var covered = 0
            val info = MediaCodec.BufferInfo()
            var lastOutputNanos = System.nanoTime()
            fun emit(left: Float, right: Float) {
                val index = anchor + outputFrame++ - startFrame
                if (index in 0 until frames.toLong()) {
                    samples[0][index.toInt()] = left
                    samples[1][index.toInt()] = right
                    covered = maxOf(covered, index.toInt() + 1)
                }
            }
            while (!ended && covered < frames) {
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
                        val decoded = decoder.outputFormat
                        rate = decoded.getInteger(MediaFormat.KEY_SAMPLE_RATE)
                        channels = decoded.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
                        check(channels in 1..2)
                        check(resampler.inputFrames == 0L || rate == resampler.sampleRate)
                        if (resampler.inputFrames == 0L) resampler = StereoPcmResampler(rate)
                        floatPcm = decoded.containsKey(MediaFormat.KEY_PCM_ENCODING) && decoded.getInteger(MediaFormat.KEY_PCM_ENCODING) == 4
                    }
                    else -> if (index >= 0) {
                        lastOutputNanos = System.nanoTime()
                        try {
                            if (info.size > 0) {
                                if (anchor == Long.MIN_VALUE) anchor =
                                    (info.presentationTimeUs * VocalTimeline.SAMPLE_RATE / 1_000_000.0).roundToLong()
                                val pcm = checkNotNull(decoder.getOutputBuffer(index)).order(ByteOrder.LITTLE_ENDIAN)
                                pcm.position(info.offset)
                                pcm.limit(info.offset + info.size)
                                val frameBytes = channels * if (floatPcm) 4 else 2
                                while (pcm.remaining() >= frameBytes && covered < frames) {
                                    val left = if (floatPcm) pcm.float else pcm.short / 32768f
                                    val right = if (channels == 1) left else if (floatPcm) pcm.float else pcm.short / 32768f
                                    check(left.isFinite() && right.isFinite())
                                    resampler.accept(left, right, ::emit)
                                }
                            }
                            ended = info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0
                        } finally { decoder.releaseOutputBuffer(index, false) }
                    }
                }
            }
            if (ended && anchor != Long.MIN_VALUE) resampler.finish(::emit)
            check(covered > 0) { "No decoded samples" }
            return if (covered == frames) samples else Array(2) { samples[it].copyOf(covered) }
        } finally { decoder.release() }
    }

    override fun close() {
        try { extractor.release() } finally { source.close() }
    }
}
